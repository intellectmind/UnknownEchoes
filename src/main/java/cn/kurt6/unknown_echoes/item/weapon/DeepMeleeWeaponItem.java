package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 第二维度武器特效。所有位移、目标和冷却判定均由服务端执行。 */
public class DeepMeleeWeaponItem extends EchoMeleeWeaponItem {
    private static final TagKey<Block> GRAVITY_HAMMER_MINEABLE = TagKey.create(
            Registries.BLOCK, UnknownEchoes.id("gravity_hammer_mineable"));
    private static final String GRAVITY_EFFECT_COOLDOWN = "ue_gravity_hammer_effect_until";
    private static final String RIFT_EFFECT_COOLDOWN = "ue_rift_dagger_effect_until";
    private static final int GRAVITY_COOLDOWN_TICKS = 120;
    private static final int RIFT_COOLDOWN_TICKS = 100;

    private final Kind kind;

    public DeepMeleeWeaponItem(Kind kind, double damage, double speed,
                               String tooltipKey, Properties properties) {
        super(damage, speed, tooltipKey, properties);
        this.kind = kind;
    }

    @Override
    public float applyDamageEventEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return switch (kind) {
            case GRAVITY_HAMMER -> applyGravityHammer(level, player, target);
            case RIFT_DAGGER -> applyRiftDagger(level, player, target);
        };
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (kind == Kind.GRAVITY_HAMMER && state.is(GRAVITY_HAMMER_MINEABLE)) {
            return 14.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    private float applyGravityHammer(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy)
                || !target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES)
                || target.getType().is(ModEntityTags.ECHO_BOSSES)) {
            return 0.0F;
        }
        long now = player.server.overworld().getGameTime();
        if (player.getPersistentData().getLong(GRAVITY_EFFECT_COOLDOWN) > now) {
            return 0.0F;
        }
        player.getPersistentData().putLong(GRAVITY_EFFECT_COOLDOWN, now + GRAVITY_COOLDOWN_TICKS);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                40, 1, true, false, true));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ(),
                14, 0.45D, 0.25D, 0.45D, 0.05D);
        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 0.2D, target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.75F, 0.55F);
        return 0.0F;
    }

    private float applyRiftDagger(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy)) {
            return 0.0F;
        }
        long now = player.server.overworld().getGameTime();
        if (player.getPersistentData().getLong(RIFT_EFFECT_COOLDOWN) > now) {
            return 0.0F;
        }
        Vec3 destination = findSafeBackstep(level, player, 3.0D);
        if (destination == null) {
            return 0.0F;
        }
        player.getPersistentData().putLong(RIFT_EFFECT_COOLDOWN, now + RIFT_COOLDOWN_TICKS);
        Vec3 origin = player.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + player.getBbHeight() * 0.5D, origin.z,
                18, 0.3D, 0.55D, 0.3D, 0.08D);
        player.teleportTo(level, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        if (target.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    40, 0, true, false, true));
        }
        level.sendParticles(ParticleTypes.END_ROD,
                destination.x, destination.y + player.getBbHeight() * 0.5D, destination.z,
                10, 0.25D, 0.45D, 0.25D, 0.03D);
        level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.65F, 1.45F);
        return 0.0F;
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
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos feet = BlockPos.containing(x, player.getY() + dy, z);
                Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                AABB moved = original.move(target.subtract(player.position()));
                if (!level.getWorldBorder().isWithinBounds(feet)
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

    public enum Kind {
        GRAVITY_HAMMER,
        RIFT_DAGGER
    }
}
