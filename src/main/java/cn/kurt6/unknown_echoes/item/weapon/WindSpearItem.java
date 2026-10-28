package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
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
    public boolean isFoil(ItemStack stack) {
        return cn.kurt6.unknown_echoes.particle.ParticleRules.data(stack).total() == 50 || super.isFoil(stack);
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
        // 技能耐久或损坏回调可能更换手持物，按当前栈完成原版使用收尾。
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean activateSkill(ServerLevel level, net.minecraft.server.level.ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player) || !WeaponSkillSupport.begin(player,
                "ue_weapon_skill_wind_spear_primary_until", dashCooldownTicks, 0)) {
            return false;
        }
        castPrimary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    private void castPrimary(ServerLevel level, ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 start = origin.add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, Mth.clamp(look.y * 0.5 + 0.1, -0.1, 0.45), look.z)
                .normalize().scale(dashStrength);
        this.sweepWindBlade(level, player, start, dash.normalize());
        // 伤害回调可能传送玩家；旧突刺速度只能提交到本次起点。
        if (!isAtSkillOrigin(level, player, origin)) {
            return;
        }
        player.setDeltaMovement(dash);
        if (!isAtSkillOrigin(level, player, origin)) {
            return;
        }
        player.hurtMarked = true;
        level.playSound(null, BlockPos.containing(origin),
                ModSounds.WEAPON_WIND_SPEAR_PRIMARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player) || !WeaponSkillSupport.begin(player,
                "ue_weapon_skill_wind_spear_secondary_until", SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        castSecondary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private void castSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.WIND_SPEAR;
        double radius = 6.0D;
        Vec3 center = player.position();
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : radiusSkillTargets(level, player, center, bounds, radius, 8)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, 11.0F);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.WIND_SPEAR);
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                Vec3 push = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.9D))
                            .add(0.0D, 0.35D, 0.0D));
                    if (isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                        target.hurtMarked = true;
                    }
                }
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0, true, false, true));
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.releaseBurst(level, center.add(0.0D, 0.2D, 0.0D),
                WeaponFxTheme.WIND_SPEAR, 1.0D);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_WIND_SPEAR_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 风刃扫掠：突刺时沿冲刺方向切出风刃，击退路径上的敌对生物。 */
    private void sweepWindBlade(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 dir) {
        Vec3 requestedEnd = start.add(dir.scale(BLADE_LENGTH));
        BlockHitResult block = level.clip(new ClipContext(start, requestedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double bladeLength = block.getType() == HitResult.Type.MISS
                ? BLADE_LENGTH : start.distanceTo(block.getLocation());
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        if (bladeLength <= 0.1D) {
            return;
        }
        Vec3 end = start.add(dir.scale(bladeLength));

        WeaponFx.pierceBeam(level, player, WeaponFxTheme.WIND_SPEAR, start, dir, bladeLength);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        AABB bounds = new AABB(start, end).inflate(BLADE_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> isCurrentBladeTarget(level, player, e, bounds, start, dir, bladeLength))) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentBladeTarget(level, player, victim, bounds, start, dir, bladeLength)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, victim, bladeDamage);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentBladeTarget(level, player, victim, bounds, start, dir, bladeLength)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(victim)) {
                Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(start);
                double along = toVictim.dot(dir);
                Vec3 side = toVictim.subtract(dir.scale(along));
                Vec3 push = (side.lengthSqr() > 1.0E-4 ? side.normalize() : new Vec3(-dir.z, 0, dir.x))
                        .scale(0.6).add(0, 0.35, 0);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push));
                if (!isCurrentBladeTarget(level, player, victim, bounds, start, dir, bladeLength)) {
                    continue;
                }
                victim.hurtMarked = true;
            }
            WeaponFx.hitMark(level, victim, WeaponFxTheme.WIND_SPEAR);
        }
    }

    private static boolean isCurrentSkillActor(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    private static boolean isAtSkillOrigin(ServerLevel level, ServerPlayer player, Vec3 origin) {
        return isCurrentSkillActor(level, player) && player.position().distanceToSqr(origin) <= 1.0E-6D;
    }

    private static boolean isCurrentBladeTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                 AABB bounds, Vec3 start, Vec3 dir, double length) {
        if (!(isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.getBoundingBox().intersects(bounds))) {
            return false;
        }
        Vec3 offset = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(start);
        double along = offset.dot(dir);
        return along >= 0.0D && along <= length && offset.subtract(dir.scale(along)).length() <= BLADE_RADIUS;
    }

    private static List<LivingEntity> radiusSkillTargets(ServerLevel level, ServerPlayer player,
                                                         Vec3 center, AABB bounds, double radius, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        target -> isCurrentRadiusTarget(level, player, target, center, bounds, radius))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)))
                .limit(limit)
                .toList();
    }

    private static boolean isCurrentRadiusTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                   Vec3 center, AABB bounds, double radius) {
        return isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.position().distanceToSqr(center) <= radius * radius
                && target.getBoundingBox().intersects(bounds);
    }
}
