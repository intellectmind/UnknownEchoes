package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Comparator;
import java.util.function.Predicate;

/** 潮汐弩；弹射物伤害以 ModItems 和 TideBoltEntity 的实际配置为准。 */
public class TideCrossbowItem extends CrossbowItem implements WeaponSkill {
    private static final String VIRTUAL_AMMO_KEY = "ue_tide_virtual";
    private static final int PRIMARY_COOLDOWN_TICKS = 160;
    private static final int SECONDARY_COOLDOWN_TICKS = 360;

    private final double boltBaseDamage;
    private final int quickLoadTicks;

    public TideCrossbowItem(double boltBaseDamage, int quickLoadTicks, Properties properties) {
        super(properties);
        this.boltBaseDamage = boltBaseDamage;
        this.quickLoadTicks = quickLoadTicks;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return cn.kurt6.unknown_echoes.particle.ParticleRules.data(stack).total() == 50 || super.isFoil(stack);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        if (!WeaponSkillSupport.requireCrossbowCharge(player, stack, hand,
                CrossbowItem.getChargeDuration(stack, player) - quickLoadTicks)) {
            return false;
        }
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }

        WeaponSkillSupport.consumeCrossbowCharge(stack);
        castPrimary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }

        castSecondary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private void castPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.TIDE_CROSSBOW;
        SkillAim aim = skillAim(player, 20.0D, 1.2D);
        Predicate<LivingEntity> targetCheck = target -> isCurrentLineTarget(level, player, target, aim, 20.0D, 1.2D);
        for (LivingEntity target : aimedSkillTargets(level, aim, targetCheck, 5)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!targetCheck.test(target)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, 15.0F);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !targetCheck.test(target)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!targetCheck.test(target)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0, true, false, true));
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        double beamLength = WeaponFx.clippedBeamLength(level, player, aim.origin(), aim.direction(), 20.0D);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.pierceBeam(level, player, fx, aim.origin(), aim.direction(), beamLength);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(aim.feet()), ModSounds.WEAPON_TIDE_CROSSBOW_PRIMARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void castSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.TIDE_CROSSBOW;
        double radius = 7.0D;
        Vec3 center = player.position();
        AABB bounds = player.getBoundingBox().inflate(radius);
        double wardHeight = player.getBbHeight() + 0.35D;
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 300, 0, true, false, true));
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        for (LivingEntity target : radiusSkillTargets(level, player, center, bounds, radius, 8)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, 10.0F);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, true, false, true));
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.wardSpiral(level, center, wardHeight, fx, 1.1D, 3);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_TIDE_CROSSBOW_SECONDARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private record SkillAim(Vec3 feet, Vec3 origin, Vec3 direction, AABB bounds) {
    }

    private static boolean isCurrentSkillActor(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    private static SkillAim skillAim(ServerPlayer player, double range, double padding) {
        Vec3 feet = player.position();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        return new SkillAim(feet, origin, direction,
                new AABB(origin, origin.add(direction.scale(range))).inflate(padding));
    }

    private static List<LivingEntity> aimedSkillTargets(ServerLevel level, SkillAim aim,
                                                        Predicate<LivingEntity> targetCheck, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class, aim.bounds(), targetCheck)
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(aim.feet())))
                .limit(limit)
                .toList();
    }

    private static boolean isCurrentAimTarget(ServerLevel level, ServerPlayer player, LivingEntity target, SkillAim aim) {
        return isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.getBoundingBox().intersects(aim.bounds());
    }

    private static boolean isCurrentLineTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                SkillAim aim, double range, double width) {
        if (!isCurrentAimTarget(level, player, target, aim)) {
            return false;
        }
        Vec3 offset = target.getBoundingBox().getCenter().subtract(aim.origin());
        double along = offset.dot(aim.direction());
        double targetRadius = target.getBbWidth() * 0.5D + width;
        return along >= 0.25D && along <= range
                && offset.subtract(aim.direction().scale(along)).lengthSqr() <= targetRadius * targetRadius
                && hasSightFrom(level, player, target, aim.origin());
    }

    private static boolean hasSightFrom(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 origin) {
        Vec3 end = target.getEyePosition();
        AABB box = target.getBoundingBox();
        if (end.distanceTo(origin) > 128.0D) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS && isCurrentSkillActor(level, player)
                && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && end.equals(target.getEyePosition()) && box.equals(target.getBoundingBox());
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

    private String skillKey(String slot) {
        return "ue_weapon_skill_tide_crossbow_" + slot + "_until";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.MIRROR_TIDE_ALLOY_PLATE.get())
                || repairCandidate.is(ModItems.WATCHER_EYE_SHARD.get())
                || repairCandidate.is(ModItems.BLACK_TIDE_THREAD.get())
                || repairCandidate.is(ModItems.TIDE_CORE.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.skill_effect")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary_effect")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary_buff")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.ammo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.passive")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft - quickLoadTicks);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon,
                                          ItemStack ammo, boolean isCrit) {
        if (!isVanillaTideAmmo(ammo)) {

            return super.createProjectile(level, shooter, weapon, ammo, isCrit);
        }
        TideBoltEntity bolt = new TideBoltEntity(level, shooter,
                ammo.copyWithCount(1), weapon.copyWithCount(1));
        if (ammo.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).contains(VIRTUAL_AMMO_KEY)) {
            bolt.setVirtualAmmo();
        }
        bolt.setBaseDamage(boltBaseDamage);
        if (isCrit) {
            bolt.setCritArrow(true);
        }
        bolt.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        return bolt;
    }

    private static boolean isVanillaTideAmmo(ItemStack ammo) {
        return ammo.is(Items.ARROW) || ammo.is(Items.TIPPED_ARROW)
                || ammo.is(Items.SPECTRAL_ARROW);
    }

    @Override
    protected void shoot(net.minecraft.server.level.ServerLevel level, LivingEntity shooter,
                         net.minecraft.world.InteractionHand hand, ItemStack weapon,
                         java.util.List<ItemStack> projectiles, float velocity, float inaccuracy,
                         boolean critical, LivingEntity target) {
        cn.kurt6.unknown_echoes.particle.ParticleAttack.shoot(shooter, weapon,
                () -> super.shoot(level, shooter, hand, weapon, projectiles, velocity, inaccuracy, critical, target));
    }
}
