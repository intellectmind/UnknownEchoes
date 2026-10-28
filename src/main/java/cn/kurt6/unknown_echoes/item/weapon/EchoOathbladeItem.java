package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
        if (!isCurrentOathActor(level, player) || !WeaponSkillSupport.begin(player,
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
        if (!isCurrentOathActor(level, player) || !WeaponSkillSupport.begin(player,
                "ue_weapon_skill_echo_oathblade_secondary_until", BULWARK_COOLDOWN_TICKS, 0,
                () -> WeaponSkillSupport.needsAbsorptionRefresh(player, 100, 1, 8.0F)
                        || WeaponSkillSupport.needsEffectRefresh(
                        player, MobEffects.DAMAGE_RESISTANCE, 50, 0))) {
            return false;
        }
        castBulwark(level, player);
        finishSkill(player, stack, hand, 1);
        return true;
    }

    private void castBulwark(ServerLevel level, ServerPlayer player) {
        WeaponSkillSupport.applyAbsorption(player, 100, 1, 8.0F);
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, true, false, true));
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        spawnBulwarkEffect(level, player);
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_OATHBLADE_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
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
        if (!isCurrentSweepHit(level, player, target)
                || !(player.getPersistentData().getFloat(ATTACK_CHARGE_KEY) >= 0.9F)) {
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
        JudgmentAim aim = judgmentAim(player);
        Vec3 pivot = aim.feet().add(0.0D, player.getBbHeight() * 0.45D, 0.0D);
        for (LivingEntity target : judgmentTargets(level, player, aim)) {
            if (!isCurrentOathActor(level, player)) {
                return;
            }
            if (!isCurrentJudgmentTarget(level, player, target, aim)) {
                continue;
            }
            float damage = isBoss(target)
                    ? JUDGMENT_BOSS_DAMAGE : JUDGMENT_DAMAGE;
            boolean hurt = WeaponSkillSupport.hurt(player, target, damage);
            if (!isCurrentOathActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentJudgmentTarget(level, player, target, aim)) {
                continue;
            }
            if (!isBoss(target)) {
                Vec3 push = target.position().subtract(aim.feet()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.65D))
                            .add(0.0D, 0.2D, 0.0D));
                    if (!isCurrentJudgmentTarget(level, player, target, aim)) {
                        continue;
                    }
                    target.hurtMarked = true;
                }
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ECHO_OATHBLADE);
        }

        if (!isCurrentOathActor(level, player)) {
            return;
        }
        WeaponFx.slashArc(level, pivot, aim.direction(), WeaponFxTheme.ECHO_OATHBLADE,
                JUDGMENT_RANGE, WeaponFx.halfAngleOf(JUDGMENT_MIN_DOT));
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(aim.feet()), ModSounds.WEAPON_ECHO_OATHBLADE_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private record JudgmentAim(Vec3 feet, Vec3 origin, Vec3 direction, AABB bounds) {
    }

    private static JudgmentAim judgmentAim(ServerPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        return new JudgmentAim(player.position(), origin, look,
                new AABB(origin, origin.add(look.scale(JUDGMENT_RANGE))).inflate(JUDGMENT_RANGE * 0.7D));
    }

    private static List<LivingEntity> judgmentTargets(ServerLevel level, ServerPlayer player, JudgmentAim aim) {
        return level.getEntitiesOfClass(LivingEntity.class, aim.bounds(),
                        target -> isCurrentJudgmentTarget(level, player, target, aim))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(aim.feet())))
                .limit(JUDGMENT_MAX_TARGETS)
                .toList();
    }

    private static boolean isCurrentJudgmentTarget(ServerLevel level, ServerPlayer player,
                                                   LivingEntity target, JudgmentAim aim) {
        if (!(isCurrentOathActor(level, player) && target.level() == level
                && WeaponSkillSupport.isHostile(player, target) && target.getBoundingBox().intersects(aim.bounds()))) {
            return false;
        }
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(aim.origin());
        double distance = toTarget.length();
        return distance > 0.0D && distance <= JUDGMENT_RANGE
                && toTarget.scale(1.0D / distance).dot(aim.direction()) >= JUDGMENT_MIN_DOT
                && hasJudgmentSight(level, player, target, aim.origin());
    }

    private static boolean hasJudgmentSight(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 origin) {
        Vec3 end = target.getEyePosition();
        AABB box = target.getBoundingBox();
        if (end.distanceTo(origin) > 128.0D) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS && isCurrentOathActor(level, player)
                && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && end.equals(target.getEyePosition()) && box.equals(target.getBoundingBox());
    }

    private static void releasePassiveSweep(ServerLevel level, ServerPlayer player, LivingEntity primary) {
        Vec3 center = primary.position();
        AABB bounds = primary.getBoundingBox().inflate(PASSIVE_SWEEP_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, bounds,
                        target -> isCurrentSweepTarget(level, player, target, primary, bounds))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)))
                .limit(PASSIVE_SWEEP_MAX_TARGETS)
                .toList();
        if (nearby.isEmpty()) {
            return;
        }
        for (LivingEntity target : nearby) {
            if (!isCurrentOathActor(level, player)) {
                return;
            }
            if (!isCurrentSweepTarget(level, player, target, primary, bounds)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, PASSIVE_SWEEP_DAMAGE);
            if (!isCurrentOathActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentSweepTarget(level, player, target, primary, bounds)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ECHO_OATHBLADE);
        }
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        if (isCurrentSweepHit(level, player, primary) && primary.isAlive()) {
            WeaponFx.hitMark(level, primary, WeaponFxTheme.ECHO_OATHBLADE);
        }
        if (!isCurrentOathActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_ECHO_OATHBLADE_PRIMARY.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static boolean isCurrentOathActor(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    private static boolean isCurrentSweepHit(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return isCurrentOathActor(level, player) && target.level() == level && !target.isRemoved()
                && !target.isSpectator() && target != player && !target.isAlliedTo(player) && target instanceof Enemy;
    }

    private static boolean isCurrentSweepTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                LivingEntity primary, AABB bounds) {
        return isCurrentOathActor(level, player) && target.level() == level && target != primary
                && WeaponSkillSupport.isHostile(player, target) && !isBoss(target)
                && target.getBoundingBox().intersects(bounds);
    }

    private void finishSkill(ServerPlayer player, ItemStack stack, InteractionHand hand,
                             int durabilityCost) {
        WeaponSkillSupport.finish(player, stack, hand, durabilityCost);
    }

    private static void spawnBulwarkEffect(ServerLevel level, ServerPlayer player) {
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.ECHO_OATHBLADE, 1.15D, 3);
    }
}
