package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** 回声境域毕业武器：双按键主动战技与满蓄力横扫均由服务端结算。 */
public class EchoOathbladeItem extends EchoMeleeWeaponItem implements WeaponSkill {
    private static final String SWEEP_COOLDOWN_KEY = "ue_oathblade_sweep_until";

    private static final int JUDGMENT_COOLDOWN_TICKS = 160;
    private static final int BULWARK_COOLDOWN_TICKS = 240;
    private static final int PASSIVE_SWEEP_COOLDOWN_TICKS = 30;
    private static final int JUDGMENT_MAX_TARGETS = 5;
    private static final int PASSIVE_SWEEP_MAX_TARGETS = 3;
    private static final double JUDGMENT_RANGE = 6.5D;
    private static final double PASSIVE_SWEEP_RADIUS = 2.75D;
    private static final double JUDGMENT_MIN_DOT = 0.70710678118D;
    private static final float JUDGMENT_DAMAGE = 12.0F;
    private static final float JUDGMENT_BOSS_DAMAGE = 8.0F;
    private static final float PASSIVE_SWEEP_DAMAGE = 4.0F;

    public EchoOathbladeItem(double damage, double speed, Properties properties) {
        super(damage, speed, "echo_oathblade", properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.skill_detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.secondary_detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.passive")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.detail")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 主战技：前方扇形裁决斩。额外伤害走正常 hurt 流程，Boss 阶段保护仍会钳制。 */
    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_echo_oathblade_primary_until", JUDGMENT_COOLDOWN_TICKS, 0)) {
            return false;
        }
        releaseJudgment(level, player);
        finishSkill(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    /** 副战技：用短时吸收与抗性换取生存窗口，并使用独立于主战技的冷却。 */
    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_echo_oathblade_secondary_until", BULWARK_COOLDOWN_TICKS, 0,
                () -> WeaponSkillSupport.needsAbsorptionRefresh(player, 100, 1, 8.0F)
                        || WeaponSkillSupport.needsEffectRefresh(
                        player, MobEffects.DAMAGE_RESISTANCE, 50, 0))) {
            return false;
        }
        WeaponSkillSupport.applyAbsorption(player, 100, 1, 8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, true, false, true));
        spawnBulwarkEffect(level, player);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_OATHBLADE_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        finishSkill(player, stack, hand, 1);
        return true;
    }

    /** 普通命中保留境域专精，并把满蓄力命中扩展为有内置冷却的清群横扫。 */
    @Override
    public float getDamageEventBonus(ServerLevel level, ServerPlayer player, LivingEntity target) {

        float bonus = isBoss(target)
                ? 0.0F : super.getDamageEventBonus(level, player, target);
        if (bonus > 0.0F) {
            bonus += 1.0F;
        }
        return bonus;
    }

    @Override
    public void applyPostDamageEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy)
                || player.getPersistentData().getFloat(ATTACK_CHARGE_KEY) < 0.9F) {
            return;
        }
        long now = level.getGameTime();
        if (WeaponSkillSupport.normalizeCooldownDeadline(
                player, SWEEP_COOLDOWN_KEY, now, PASSIVE_SWEEP_COOLDOWN_TICKS) > now) {
            return;
        }
        player.getPersistentData().putLong(SWEEP_COOLDOWN_KEY,
                WeaponSkillSupport.safeDeadline(now, PASSIVE_SWEEP_COOLDOWN_TICKS));
        releasePassiveSweep(level, player, target);
    }

    private void releaseJudgment(ServerLevel level, ServerPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 end = origin.add(look.scale(JUDGMENT_RANGE));
        AABB search = new AABB(origin, end).inflate(JUDGMENT_RANGE * 0.7D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                        entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                                && entity instanceof Enemy && !entity.isAlliedTo(player))
                .stream()
                .filter(entity -> isInsideJudgmentCone(player, origin, look, entity))
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(JUDGMENT_MAX_TARGETS)
                .toList();

        for (LivingEntity target : targets) {
            float damage = isBoss(target)
                    ? JUDGMENT_BOSS_DAMAGE : JUDGMENT_DAMAGE;
            boolean[] hurt = {false};
            WeaponEffectGuard.run(player,
                    () -> hurt[0] = target.hurt(player.damageSources().playerAttack(player), damage));
            if (!hurt[0]) {
                continue;
            }
            if (!isBoss(target)) {
                Vec3 push = target.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.65D))
                            .add(0.0D, 0.2D, 0.0D));
                    target.hurtMarked = true;
                }
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ECHO_OATHBLADE);
        }

        WeaponFx.slashArc(level, player, WeaponFxTheme.ECHO_OATHBLADE,
                JUDGMENT_RANGE, WeaponFx.halfAngleOf(JUDGMENT_MIN_DOT));
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_OATHBLADE_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean isInsideJudgmentCone(ServerPlayer player, Vec3 origin,
                                                Vec3 look, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
        double distance = toTarget.length();
        return distance > 0.0D && distance <= JUDGMENT_RANGE
                && toTarget.scale(1.0D / distance).dot(look) >= JUDGMENT_MIN_DOT
                && player.hasLineOfSight(target);
    }

    private static void releasePassiveSweep(ServerLevel level, ServerPlayer player, LivingEntity primary) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        primary.getBoundingBox().inflate(PASSIVE_SWEEP_RADIUS),
                        entity -> entity != player && entity != primary && entity.isAlive()
                                && entity instanceof Enemy && !entity.isAlliedTo(player)
                                && !isBoss(entity))
                .stream()
                .sorted(Comparator.comparingDouble(primary::distanceToSqr))
                .limit(PASSIVE_SWEEP_MAX_TARGETS)
                .toList();
        if (nearby.isEmpty()) {
            return;
        }
        for (LivingEntity target : nearby) {
            WeaponEffectGuard.run(player,
                    () -> target.hurt(player.damageSources().playerAttack(player), PASSIVE_SWEEP_DAMAGE));
            WeaponFx.hitMark(level, target, WeaponFxTheme.ECHO_OATHBLADE);
        }
        WeaponFx.hitMark(level, primary, WeaponFxTheme.ECHO_OATHBLADE);
        level.playSound(null, primary.blockPosition(), ModSounds.WEAPON_ECHO_OATHBLADE_PRIMARY.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private void finishSkill(ServerPlayer player, ItemStack stack, InteractionHand hand,
                             int durabilityCost) {
        WeaponSkillSupport.finish(player, stack, hand, durabilityCost);
    }

    private static void spawnBulwarkEffect(ServerLevel level, ServerPlayer player) {
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.ECHO_OATHBLADE, 1.15D, 3);
    }
}
