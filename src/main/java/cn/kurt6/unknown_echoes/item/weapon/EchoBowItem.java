package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Comparator;
import java.util.function.Predicate;

/** 第一维度弓类武器的轻量实现,保留原版弹道并补充玩法说明。 */
public class EchoBowItem extends BowItem implements WeaponSkill {
    private final String tooltipKey;
    private static final int ECHO_LONGBOW_PRIMARY_COOLDOWN = 140;
    private static final int ECHO_LONGBOW_SECONDARY_COOLDOWN = 320;
    private static final int DREAM_BLOOM_PRIMARY_COOLDOWN = 160;
    private static final int DREAM_BLOOM_SECONDARY_COOLDOWN = 360;

    public EchoBowItem(String tooltipKey, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    public String tooltipKey() {
        return tooltipKey;
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
        if (!WeaponSkillSupport.requireBowDraw(player, stack, hand, 20)) {
            return false;
        }
        int cooldown = tooltipKey.equals("echo_longbow")
                ? ECHO_LONGBOW_PRIMARY_COOLDOWN : DREAM_BLOOM_PRIMARY_COOLDOWN;
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), cooldown, 0)) {
            return false;
        }
        if (tooltipKey.equals("echo_longbow")) {
            castLongbowPrimary(level, player);
        } else {
            castDreamBloomPrimary(level, player);
        }
        WeaponSkillSupport.finish(player, stack, hand, 1);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        int cooldown = tooltipKey.equals("echo_longbow")
                ? ECHO_LONGBOW_SECONDARY_COOLDOWN : DREAM_BLOOM_SECONDARY_COOLDOWN;
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), cooldown, 0)) {
            return false;
        }
        if (tooltipKey.equals("echo_longbow")) {
            castLongbowSecondary(level, player);
        } else {
            castDreamBloomSecondary(level, player);
        }
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    private void castLongbowPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        SkillAim aim = skillAim(player, 22.0D, 1.0D);
        Predicate<LivingEntity> targetCheck = target -> isCurrentLineTarget(level, player, target, aim, 22.0D, 1.0D);
        for (LivingEntity target : aimedSkillTargets(level, aim, targetCheck, 3)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!targetCheck.test(target)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, 12.0F);
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
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(aim.feet()), ModSounds.WEAPON_ECHO_LONGBOW_PRIMARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        double beamLength = WeaponFx.clippedBeamLength(level, player, aim.origin(), aim.direction(), 22.0D);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.pierceBeam(level, player, fx, aim.origin(), aim.direction(), beamLength);
    }

    private void castDreamBloomPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        SkillAim aim = skillAim(player, 24.0D, 0.8D);
        Predicate<LivingEntity> targetCheck = target -> isCurrentLineTarget(level, player, target, aim, 24.0D, 0.8D);
        for (LivingEntity target : aimedSkillTargets(level, aim, targetCheck, 1)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!targetCheck.test(target)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, 22.0F);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !targetCheck.test(target)) {
                continue;
            }
            WeaponFx.heavyHitMark(level, target, fx);
            if (!targetCheck.test(target)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, true, false, true));
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(aim.feet()), ModSounds.WEAPON_DREAM_BLOOM_BOW_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        double beamLength = WeaponFx.clippedBeamLength(level, player, aim.origin(), aim.direction(), 24.0D);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.pierceBeam(level, player, fx, aim.origin(), aim.direction(), beamLength);
    }

    private void castLongbowSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        double radius = 7.0D;
        Vec3 center = player.position();
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : radiusSkillTargets(level, player, center, bounds, radius, 6)) {
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
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_ECHO_LONGBOW_SECONDARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void castDreamBloomSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        double radius = 7.0D;
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
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, true, false, true));
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_DREAM_BLOOM_BOW_SECONDARY.get(),
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
        return "ue_weapon_skill_" + tooltipKey + "_" + slot + "_until";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        boolean matchesMaterial = switch (tooltipKey) {
            case "echo_longbow" -> repairCandidate.is(ModItems.WINDWOVEN_THREAD.get());
            case "dream_bloom_bow" -> repairCandidate.is(ModItems.DREAM_NECTAR.get());
            default -> false;
        };
        return matchesMaterial || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".passive")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
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
