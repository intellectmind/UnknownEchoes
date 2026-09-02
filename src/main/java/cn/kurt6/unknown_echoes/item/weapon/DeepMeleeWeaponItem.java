package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.item.tool.DeepAlloyTier;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 第二维度武器特效。所有位移、目标和冷却判定均由服务端执行。 */
import java.util.List;

public class DeepMeleeWeaponItem extends EchoMeleeWeaponItem implements WeaponSkill {
    private static final TagKey<Block> GRAVITY_HAMMER_MINEABLE = TagKey.create(
            Registries.BLOCK, UnknownEchoes.id("gravity_hammer_mineable"));
    private static final String GRAVITY_EFFECT_COOLDOWN = "ue_gravity_hammer_effect_until";
    private static final String RIFT_EFFECT_COOLDOWN = "ue_rift_dagger_effect_until";
    private static final String ANCHOR_EFFECT_COOLDOWN = "ue_anchor_blade_effect_until";
    private static final String GLAIVE_EFFECT_COOLDOWN = "ue_folded_glaive_effect_until";
    private static final int GRAVITY_COOLDOWN_TICKS = 60;
    private static final int RIFT_COOLDOWN_TICKS = 60;
    private static final int ANCHOR_COOLDOWN_TICKS = 60;
    private static final int GLAIVE_COOLDOWN_TICKS = 60;
    private static final int GRAVITY_HAMMER_PRIMARY_COST = 14;
    private static final int GRAVITY_HAMMER_SECONDARY_COST = 28;
    private static final int RIFT_DAGGER_PRIMARY_COST = 10;
    private static final int RIFT_DAGGER_SECONDARY_COST = 22;
    private static final int ANCHOR_BLADE_PRIMARY_COST = 12;
    private static final int ANCHOR_BLADE_SECONDARY_COST = 24;
    private static final int FOLDED_GLAIVE_PRIMARY_COST = 12;
    private static final int FOLDED_GLAIVE_SECONDARY_COST = 28;
    private static final int GRAVITY_HAMMER_PRIMARY_COOLDOWN = 160;
    private static final int GRAVITY_HAMMER_SECONDARY_COOLDOWN = 300;
    private static final int RIFT_DAGGER_PRIMARY_COOLDOWN = 100;
    private static final int RIFT_DAGGER_SECONDARY_COOLDOWN = 240;
    private static final int ANCHOR_BLADE_PRIMARY_COOLDOWN = 140;
    private static final int ANCHOR_BLADE_SECONDARY_COOLDOWN = 280;
    private static final int FOLDED_GLAIVE_PRIMARY_COOLDOWN = 120;
    private static final int FOLDED_GLAIVE_SECONDARY_COOLDOWN = 260;
    private static final int[] BACKSTEP_Y_OFFSETS = {0, 1, -1};

    private final Kind kind;

    public DeepMeleeWeaponItem(Kind kind, double damage, double speed,
                               String tooltipKey, Properties properties) {
        super(DeepAlloyTier.INSTANCE, damage, speed, tooltipKey, properties);
        this.kind = kind;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        boolean matchesThemedMaterial = switch (kind) {
            case GRAVITY_HAMMER -> repairCandidate.is(ModItems.GRAVITY_CORE_FRAGMENT.get());
            case RIFT_DAGGER -> repairCandidate.is(ModItems.VOID_RIFT_SHARD.get());
            case ANCHOR_BLADE -> repairCandidate.is(ModItems.SPACE_ANCHOR_ROD.get());
            case FOLDED_GLAIVE -> repairCandidate.is(ModItems.FOLDED_CARAPACE.get());
        };
        return matchesThemedMaterial || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        return activate(level, player, stack, hand, false);
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        return activate(level, player, stack, hand, true);
    }

    private boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack,
                             InteractionHand hand, boolean secondary) {
        Vec3 riftDestination = secondary && kind == Kind.RIFT_DAGGER
                ? findSafeBackstep(level, player, 6.0D) : null;
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_" + tooltipKey + (secondary ? "_secondary_until" : "_primary_until"),
                cooldownTicks(secondary), energyCost(secondary),
                () -> switch (kind) {
                    case RIFT_DAGGER -> !secondary
                            || riftDestination != null
                            || WeaponSkillSupport.needsEffectRefresh(
                            player, MobEffects.INVISIBILITY, 60, 0)
                            || WeaponSkillSupport.needsEffectRefresh(
                            player, MobEffects.MOVEMENT_SPEED, 120, 1);
                    case ANCHOR_BLADE -> !secondary
                            || WeaponSkillSupport.needsEffectRefresh(
                            player, MobEffects.DAMAGE_RESISTANCE, 160, 1)
                            || WeaponSkillSupport.needsAbsorptionRefresh(player, 160, 2, 12.0F);
                    default -> true;
                })) {
            return false;
        }
        if (secondary) {
            switch (kind) {
                case GRAVITY_HAMMER -> castGravityWell(level, player);
                case RIFT_DAGGER -> castRiftVeil(level, player, riftDestination);
                case ANCHOR_BLADE -> castAnchorBulwark(level, player);
                case FOLDED_GLAIVE -> castFoldedCollapse(level, player);
            }
        } else {
            switch (kind) {
                case GRAVITY_HAMMER -> castGravitySlam(level, player);
                case RIFT_DAGGER -> castRiftRush(level, player);
                case ANCHOR_BLADE -> castAnchorChains(level, player);
                case FOLDED_GLAIVE -> castUnfoldedArc(level, player);
            }
        }
        WeaponSkillSupport.finish(player, stack, hand, secondary ? 3 : 2);
        return true;
    }

    private int energyCost(boolean secondary) {
        return switch (kind) {
            case GRAVITY_HAMMER -> secondary
                    ? GRAVITY_HAMMER_SECONDARY_COST : GRAVITY_HAMMER_PRIMARY_COST;
            case RIFT_DAGGER -> secondary ? RIFT_DAGGER_SECONDARY_COST : RIFT_DAGGER_PRIMARY_COST;
            case ANCHOR_BLADE -> secondary ? ANCHOR_BLADE_SECONDARY_COST : ANCHOR_BLADE_PRIMARY_COST;
            case FOLDED_GLAIVE -> secondary
                    ? FOLDED_GLAIVE_SECONDARY_COST : FOLDED_GLAIVE_PRIMARY_COST;
        };
    }

    private int cooldownTicks(boolean secondary) {
        return switch (kind) {
            case GRAVITY_HAMMER -> secondary
                    ? GRAVITY_HAMMER_SECONDARY_COOLDOWN : GRAVITY_HAMMER_PRIMARY_COOLDOWN;
            case RIFT_DAGGER -> secondary
                    ? RIFT_DAGGER_SECONDARY_COOLDOWN : RIFT_DAGGER_PRIMARY_COOLDOWN;
            case ANCHOR_BLADE -> secondary
                    ? ANCHOR_BLADE_SECONDARY_COOLDOWN : ANCHOR_BLADE_PRIMARY_COOLDOWN;
            case FOLDED_GLAIVE -> secondary
                    ? FOLDED_GLAIVE_SECONDARY_COOLDOWN : FOLDED_GLAIVE_PRIMARY_COOLDOWN;
        };
    }

    private void castGravitySlam(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.GRAVITY_HAMMER;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 6.0D, 8)) {
            if (WeaponSkillSupport.hurt(player, target, 22.0F)) {
                WeaponFx.heavyHitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            100, 2, true, false, true));
                }
            }
        }
        WeaponFx.groundRing(level, player, fx, 6.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2D, player.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 1.2F, 0.45F);
    }

    private void castGravityWell(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.GRAVITY_HAMMER;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 9.0D, 10)) {
            if (!WeaponSkillSupport.hurt(player, target, 14.0F) || isBoss(target)) {
                continue;
            }
            WeaponFx.pullTether(level, player, target, fx);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, true, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2,
                    true, false, true));
        }
        WeaponFx.groundRing(level, player, fx, 9.0D);
    }

    private void castRiftRush(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.RIFT_DAGGER;
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player, 14.0D, 1.0D, 6)) {
            if (WeaponSkillSupport.hurt(player, target, 18.0F)) {
                WeaponFx.hitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
                }
            }
        }
        WeaponFx.pierceBeam(level, player, fx, origin, direction, 14.0D);
        player.setDeltaMovement(direction.x * 1.5D,
                Math.max(0.08D, direction.y * 0.8D + 0.08D), direction.z * 1.5D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private void castRiftVeil(ServerLevel level, ServerPlayer player, Vec3 destination) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.RIFT_DAGGER;
        // 折返轨迹必须在传送前用旧坐标铺:传送后玩家已经在终点,取不到出发点。
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        if (destination != null) {
            Vec3 span = destination.add(0.0D, player.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            WeaponFx.dashTrail(level, origin, span, span.length(), fx);
            player.teleportTo(level, destination.x, destination.y, destination.z,
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, true, false, true));
        WeaponFx.wardSpiral(level, player, fx, 0.7D, 2);
    }

    private void castAnchorChains(ServerLevel level, ServerPlayer player) {
        for (LivingEntity target : WeaponSkillSupport.coneTargets(level, player, 7.0D, 0.1D, 6)) {
            if (!WeaponSkillSupport.hurt(player, target, 20.0F) || isBoss(target)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ANCHOR_BLADE);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1, true, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2,
                    true, false, true));
        }
        WeaponFx.slashArc(level, player, WeaponFxTheme.ANCHOR_BLADE,
                7.0D, WeaponFx.halfAngleOf(0.1D));
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private void castAnchorBulwark(ServerLevel level, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, true, false, true));
        WeaponSkillSupport.applyAbsorption(player, 160, 2, 12.0F);
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.ANCHOR_BLADE, 1.25D, 3);
    }

    private void castUnfoldedArc(ServerLevel level, ServerPlayer player) {
        for (LivingEntity target : WeaponSkillSupport.coneTargets(level, player, 9.0D, -0.05D, 8)) {
            if (!WeaponSkillSupport.hurt(player, target, 18.0F) || isBoss(target)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.FOLDED_GLAIVE);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1,
                    true, false, true));
            target.knockback(0.8D, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
        WeaponFx.slashArc(level, player, WeaponFxTheme.FOLDED_GLAIVE,
                9.0D, WeaponFx.halfAngleOf(-0.05D));
    }

    private void castFoldedCollapse(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.FOLDED_GLAIVE;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 8.0D, 10)) {
            if (!WeaponSkillSupport.hurt(player, target, 24.0F) || isBoss(target)) {
                continue;
            }
            WeaponFx.pullTether(level, player, target, fx);
            Vec3 pull = player.position().add(0.0D, 0.7D, 0.0D).subtract(target.position());
            if (pull.lengthSqr() > 1.0E-4D) {
                target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.75D)));
                target.hurtMarked = true;
            }
        }
        WeaponFx.groundRing(level, player, fx, 8.0D);
    }

    @Override
    public float getDamageEventBonus(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return 0.0F;
    }

    @Override
    public boolean wouldExposeTarget(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (kind != Kind.RIFT_DAGGER || !(target instanceof Enemy)
                || isBoss(target) || !target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES)) {
            return false;
        }
        long now = player.server.overworld().getGameTime();
        return WeaponSkillSupport.normalizeCooldownDeadline(
                player, RIFT_EFFECT_COOLDOWN, now, RIFT_COOLDOWN_TICKS) <= now
                && findSafeBackstep(level, player, 4.0D) != null;
    }

    @Override
    public void applyPostDamageEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        switch (kind) {
            case GRAVITY_HAMMER -> applyGravityHammer(level, player, target);
            case RIFT_DAGGER -> applyRiftDagger(level, player, target);
            case ANCHOR_BLADE -> applyAnchorBlade(level, player, target);
            case FOLDED_GLAIVE -> applyFoldedGlaive(level, player, target);
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (kind == Kind.GRAVITY_HAMMER && state.is(GRAVITY_HAMMER_MINEABLE)) {
            return 14.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    private void applyGravityHammer(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBoss(target)) {
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (WeaponSkillSupport.normalizeCooldownDeadline(
                player, GRAVITY_EFFECT_COOLDOWN, now, GRAVITY_COOLDOWN_TICKS) > now) {
            return;
        }
        player.getPersistentData().putLong(GRAVITY_EFFECT_COOLDOWN,
                WeaponSkillSupport.safeDeadline(now, GRAVITY_COOLDOWN_TICKS));
        boolean deepTarget = target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                deepTarget ? 70 : 60, deepTarget ? 2 : 1, true, false, true));
        WeaponFx.heavyHitMark(level, target, WeaponFxTheme.GRAVITY_HAMMER);
        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 0.2D, target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.75F, 0.55F);
    }

    private void applyRiftDagger(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBoss(target)) {
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (WeaponSkillSupport.normalizeCooldownDeadline(
                player, RIFT_EFFECT_COOLDOWN, now, RIFT_COOLDOWN_TICKS) > now) {
            return;
        }
        Vec3 destination = findSafeBackstep(level, player, 4.0D);
        if (destination == null) {
            return;
        }
        player.getPersistentData().putLong(RIFT_EFFECT_COOLDOWN,
                WeaponSkillSupport.safeDeadline(now, RIFT_COOLDOWN_TICKS));
        WeaponFx.hitMark(level, target, WeaponFxTheme.RIFT_DAGGER);
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 span = destination.add(0.0D, player.getBbHeight() * 0.5D, 0.0D).subtract(origin);
        WeaponFx.dashTrail(level, origin, span, span.length(), WeaponFxTheme.RIFT_DAGGER);
        player.teleportTo(level, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                30, 1, true, false, true));
        if (target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    40, 0, true, false, true));
        }
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.RIFT_DAGGER, 0.6D, 2);
        level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.65F, 1.45F);
    }

    private void applyAnchorBlade(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBoss(target)) {
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (WeaponSkillSupport.normalizeCooldownDeadline(
                player, ANCHOR_EFFECT_COOLDOWN, now, ANCHOR_COOLDOWN_TICKS) > now) {
            return;
        }
        player.getPersistentData().putLong(ANCHOR_EFFECT_COOLDOWN,
                WeaponSkillSupport.safeDeadline(now, ANCHOR_COOLDOWN_TICKS));
        boolean deepTarget = target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                deepTarget ? 120 : 100, 1, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                deepTarget ? 60 : 50, 1,
                true, false, true));
        WeaponFx.hitMark(level, target, WeaponFxTheme.ANCHOR_BLADE);
        level.playSound(null, target.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 0.75F, 0.75F);
    }

    private void applyFoldedGlaive(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBoss(target)) {
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (WeaponSkillSupport.normalizeCooldownDeadline(
                player, GLAIVE_EFFECT_COOLDOWN, now, GLAIVE_COOLDOWN_TICKS) > now) {
            return;
        }
        player.getPersistentData().putLong(GLAIVE_EFFECT_COOLDOWN,
                WeaponSkillSupport.safeDeadline(now, GLAIVE_COOLDOWN_TICKS));
        AABB sweep = target.getBoundingBox().inflate(4.5D, 2.0D, 4.5D);
        int affected = 0;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweep,
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive()
                        && candidate.distanceToSqr(target) <= 20.25D
                        && !candidate.isAlliedTo(player)
                        && !isBoss(candidate))) {
            boolean deepTarget = nearby.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES);
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    deepTarget ? 70 : 60, deepTarget ? 1 : 0, true, false, true));
            nearby.knockback(0.8D,
                    player.getX() - nearby.getX(), player.getZ() - nearby.getZ());
            WeaponFx.hitMark(level, nearby, WeaponFxTheme.FOLDED_GLAIVE);
            if (++affected >= 6) {
                break;
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ(),
                Math.max(1, affected), 1.2D, 0.25D, 1.2D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.85F, 0.8F);
    }

    private static Vec3 findSafeBackstep(ServerLevel level, ServerPlayer player, double range) {
        Vec3 horizontal = player.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return null;
        }
        horizontal = horizontal.normalize();
        Vec3 eye = player.getEyePosition();
        AABB original = player.getBoundingBox();
        for (double distance = range; distance >= 0.75D; distance -= 0.25D) {
            double x = player.getX() + horizontal.x * distance;
            double z = player.getZ() + horizontal.z * distance;
            for (int dy : BACKSTEP_Y_OFFSETS) {
                BlockPos feet = BlockPos.containing(x, player.getY() + dy, z);
                Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                AABB moved = original.move(target.subtract(player.position()));
                if (!level.getWorldBorder().isWithinBounds(moved)
                        || !level.hasChunkAt(feet)
                        || !level.noCollision(player, moved)
                        || !Block.canSupportCenter(level, feet.below(), Direction.UP)) {
                    continue;
                }
                BlockHitResult ray = level.clip(new ClipContext(eye,
                        target.add(0.0D, player.getEyeHeight(), 0.0D),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (ray.getType() == HitResult.Type.MISS) {
                    return target;
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".primary",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".passive")
                .withStyle(ChatFormatting.GRAY));
    }

    public enum Kind {
        GRAVITY_HAMMER,
        RIFT_DAGGER,
        ANCHOR_BLADE,
        FOLDED_GLAIVE
    }
}
