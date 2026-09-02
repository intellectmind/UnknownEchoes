package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WindSpearItem extends SwordItem implements WeaponSkill {

    private final int dashCooldownTicks;
    private final double dashStrength;
    private final float bladeDamage;
    /** 风刃判定:沿突刺路径的扫掠长度与半径。 */
    private static final double BLADE_LENGTH = 6.0;
    private static final double BLADE_RADIUS = 1.4;
    private static final int SECONDARY_COOLDOWN_TICKS = 280;
    public WindSpearItem(double damage, double speed, int cooldownTicks, double dashStrength,
                         float bladeDamage, Properties properties) {
        super(EchoMetalTier.INSTANCE, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.dashCooldownTicks = cooldownTicks;
        this.dashStrength = dashStrength;
        this.bladeDamage = bladeDamage;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.WINDWOVEN_THREAD.get())
                || repairCandidate.is(ModItems.COLOSSUS_MEMORY_CORE.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_spear.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_spear.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            this.activateSkill(serverLevel, serverPlayer, stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean activateSkill(ServerLevel level, net.minecraft.server.level.ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_wind_spear_primary_until", dashCooldownTicks, 0)) {
            return false;
        }
        Vec3 look = player.getLookAngle();

        Vec3 dash = new Vec3(look.x, Mth.clamp(look.y * 0.5 + 0.1, -0.1, 0.45), look.z)
                .normalize().scale(dashStrength);
        this.sweepWindBlade(level, player, dash.normalize());
        player.setDeltaMovement(dash);
        player.hurtMarked = true;
        level.playSound(null, player.blockPosition(),
                SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_wind_spear_secondary_until", SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        int affected = 0;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 6.0D, 8)) {
            if (!WeaponSkillSupport.hurt(player, target, 11.0F)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.WIND_SPEAR);
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                Vec3 push = target.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.9D))
                            .add(0.0D, 0.35D, 0.0D));
                    target.hurtMarked = true;
                }
            }
            affected++;
        }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0, true, false, true));
        WeaponFx.groundRing(level, player, WeaponFxTheme.WIND_SPEAR, 6.0D);
        level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 0.6D, player.getZ(),
                Math.max(4, affected), 2.2D, 0.6D, 2.2D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(),
                SoundSource.PLAYERS, 1.1F, 1.0F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    /** 风刃扫掠：突刺时沿冲刺方向切出风刃，击退路径上的敌对生物。 */
    private void sweepWindBlade(ServerLevel level, ServerPlayer player, Vec3 dir) {
        Vec3 start = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 requestedEnd = start.add(dir.scale(BLADE_LENGTH));
        BlockHitResult block = level.clip(new ClipContext(start, requestedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double bladeLength = block.getType() == HitResult.Type.MISS
                ? BLADE_LENGTH : start.distanceTo(block.getLocation());
        if (bladeLength <= 0.1D) {
            return;
        }
        Vec3 end = start.add(dir.scale(bladeLength));

        WeaponFx.pierceBeam(level, player, WeaponFxTheme.WIND_SPEAR, start, dir, bladeLength);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(start, end).inflate(BLADE_RADIUS),
                e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof Enemy
                        && !e.isAlliedTo(player))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(start);
            double along = toVictim.dot(dir);
            if (along < 0 || along > bladeLength) {
                continue;
            }
            if (toVictim.subtract(dir.scale(along)).length() > BLADE_RADIUS) {
                continue;
            }
            boolean[] hurt = {false};
            WeaponEffectGuard.run(player, () -> hurt[0] = victim.hurt(
                    player.damageSources().playerAttack(player), bladeDamage));
            if (!hurt[0]) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(victim)) {

                Vec3 side = toVictim.subtract(dir.scale(along));
                Vec3 push = (side.lengthSqr() > 1.0E-4 ? side.normalize() : new Vec3(-dir.z, 0, dir.x))
                        .scale(0.6).add(0, 0.35, 0);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push));
                victim.hurtMarked = true;
            }
            WeaponFx.hitMark(level, victim, WeaponFxTheme.WIND_SPEAR);
            level.sendParticles(ParticleTypes.GUST,
                    victim.getX(), victim.getY() + 0.3, victim.getZ(), 1, 0, 0, 0, 0);
        }
    }
}
