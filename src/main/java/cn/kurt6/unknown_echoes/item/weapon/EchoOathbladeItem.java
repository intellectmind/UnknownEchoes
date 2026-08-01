package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon.echo_oathblade.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** 主战技：前方扇形裁决斩。额外伤害走正常 hurt 流程，Boss 阶段保护仍会钳制。 */
    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        releaseJudgment(level, player);
        finishSkill(player, stack, hand, JUDGMENT_COOLDOWN_TICKS, 2);
        return true;
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    /** 副战技：用短时吸收与抗性换取生存窗口，与主战技共享物品冷却。 */
    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, true, false, true));
        spawnBulwarkEffect(level, player);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, 1.0F, 0.75F);
        finishSkill(player, stack, hand, BULWARK_COOLDOWN_TICKS, 1);
        return true;
    }

    /** 普通命中保留境域专精，并把满蓄力命中扩展为有内置冷却的清群横扫。 */
    @Override
    public float applyDamageEventEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        // 伤害事件发生在 Boss 自身的阶段钳制之后，禁止在这里给 Boss 追加伤害。
        float bonus = target.getType().is(ModEntityTags.ECHO_BOSSES)
                ? 0.0F : super.applyDamageEventEffect(level, player, target);
        if (bonus > 0.0F) {
            bonus += 1.0F;
        }

        if (!(target instanceof Enemy)
                || player.getPersistentData().getFloat(ATTACK_CHARGE_KEY) < 0.9F) {
            return bonus;
        }
        long now = level.getGameTime();
        if (player.getPersistentData().getLong(SWEEP_COOLDOWN_KEY) > now) {
            return bonus;
        }
        player.getPersistentData().putLong(SWEEP_COOLDOWN_KEY, now + PASSIVE_SWEEP_COOLDOWN_TICKS);
        releasePassiveSweep(level, player, target);
        return bonus;
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
            float damage = target.getType().is(ModEntityTags.ECHO_BOSSES)
                    ? JUDGMENT_BOSS_DAMAGE : JUDGMENT_DAMAGE;
            boolean[] hurt = {false};
            WeaponEffectGuard.run(player,
                    () -> hurt[0] = target.hurt(player.damageSources().playerAttack(player), damage));
            if (!hurt[0]) {
                continue;
            }
            if (!target.getType().is(ModEntityTags.ECHO_BOSSES)) {
                Vec3 push = target.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.65D))
                            .add(0.0D, 0.2D, 0.0D));
                    target.hurtMarked = true;
                }
            }
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.12D);
        }

        Vec3 effectCenter = origin.add(look.scale(2.6D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                effectCenter.x, effectCenter.y - 0.35D, effectCenter.z,
                5, 1.8D, 0.55D, 1.8D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD,
                effectCenter.x, effectCenter.y, effectCenter.z,
                22, 2.2D, 0.7D, 2.2D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.2F, 0.7F);
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
                                && !entity.getType().is(ModEntityTags.ECHO_BOSSES))
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
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    7, 0.3D, 0.3D, 0.3D, 0.08D);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                primary.getX(), primary.getY() + primary.getBbHeight() * 0.5D, primary.getZ(),
                3, 0.65D, 0.25D, 0.65D, 0.0D);
        level.playSound(null, primary.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 1.25F);
    }

    private void finishSkill(ServerPlayer player, ItemStack stack, InteractionHand hand,
                             int cooldownTicks, int durabilityCost) {
        player.getCooldowns().addCooldown(this, cooldownTicks);
        stack.hurtAndBreak(durabilityCost, player, player.getSlotForHand(hand));
        player.swing(hand, true);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static void spawnBulwarkEffect(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle) * 1.15D,
                    player.getY() + 0.2D + (i % 3) * 0.55D,
                    player.getZ() + Math.sin(angle) * 1.15D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
