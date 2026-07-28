package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.network.EchoAbilityActionPayload;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 重力与虚空回响的瞬态体验能力。持久进度仍只存于 EchoAbilityData。 */
public final class DeepEchoAbilityHandler {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final String GRAVITY_COOLDOWN_KEY = "ue_gravity_echo_cooldown_until";
    private static final String VOID_COOLDOWN_KEY = "ue_void_echo_cooldown_until";

    private DeepEchoAbilityHandler() {
    }

    public static void handle(ServerPlayer player, byte action) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        switch (action) {
            case EchoAbilityActionPayload.ACTION_GRAVITY_ATTUNE -> useGravityAttunement(player);
            case EchoAbilityActionPayload.ACTION_VOID_STEP -> useVoidStep(player);
            default -> {
            }
        }
    }

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = globalGameTime(player);
        if (state.gravityEnd > now) {
            if (player.tickCount % 4 == 0) {
                suppressNearbyEntities(player, state.gravityRadius);
            }
            if (player.tickCount % 6 == 0 && player.level() instanceof ServerLevel level) {
                sendGravityRing(level, player.position(), state.gravityRadius, player.tickCount / 6);
            }
        }
        if (state.voidWardEnd > now) {
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (player.tickCount % 5 == 0 && player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                        7, 0.35D, 0.55D, 0.35D, 0.03D);
            }
        }
        if (state.gravityEnd <= now && state.gravityCooldownEnd <= now
                && state.voidWardEnd <= now && state.voidCooldownEnd <= now) {
            STATES.remove(player.getUUID());
        }
    }

    public static boolean isGravityAttuned(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.gravityEnd > globalGameTime(player);
    }

    public static boolean isVoidWardActive(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.voidWardEnd > globalGameTime(player);
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public static void copyCooldowns(Player source, Player target) {
        copyCooldown(source, target, GRAVITY_COOLDOWN_KEY);
        copyCooldown(source, target, VOID_COOLDOWN_KEY);
    }

    private static void useGravityAttunement(ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO)) {
            reject(player, EchoAbilityType.GRAVITY_ECHO, "locked",
                    "message.unknown_echoes.gravity_active.locked");
            return;
        }
        State state = state(player);
        long now = globalGameTime(player);
        long gravityCooldownEnd = cooldownEnd(player, GRAVITY_COOLDOWN_KEY, now);
        if (gravityCooldownEnd > now) {
            reject(player, EchoAbilityType.GRAVITY_ECHO, "cooldown",
                    "message.unknown_echoes.gravity_active.cooldown");
            return;
        }
        int cost = ServerConfig.GRAVITY_ACTIVE_ENERGY_COST.get();
        if (cost > 0 && !ArtifactManager.spendEnergy(player, cost)) {
            EchoAbilityManager.recordFailure(player, EchoAbilityType.GRAVITY_ECHO, "energy");
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.GRAVITY_ECHO);
        int duration = gravityDuration(research);
        if (EchoArmorSets.abyssWalkerPieces(player) >= 4) {
            duration = duration * 5 / 4;
        }
        double radius = gravityRadius(research);
        state.gravityEnd = now + duration;
        state.gravityRadius = radius;
        state.gravityCooldownEnd = now + scaledCooldown(
                ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS.get(), research);
        player.getPersistentData().putLong(GRAVITY_COOLDOWN_KEY, state.gravityCooldownEnd);
        if (player.level() instanceof ServerLevel level) {
            sendGravityCalibration(level, player.position(), radius);
            level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.PLAYERS, 0.9F, 0.75F);
        }
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.gravity_active.started",
                String.format(Locale.ROOT, "%.1f", radius),
                String.format(Locale.ROOT, "%.1f", duration / 20.0D)), true);
    }

    private static void useVoidStep(ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.VOID_ECHO)) {
            reject(player, EchoAbilityType.VOID_ECHO, "locked",
                    "message.unknown_echoes.void_step.locked");
            return;
        }
        State state = state(player);
        long now = globalGameTime(player);
        long voidCooldownEnd = cooldownEnd(player, VOID_COOLDOWN_KEY, now);
        if (voidCooldownEnd > now) {
            reject(player, EchoAbilityType.VOID_ECHO, "cooldown",
                    "message.unknown_echoes.void_step.cooldown");
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.VOID_ECHO);
        Vec3 destination = findSafeVoidStep(level, player, voidDistance(research));
        if (destination == null) {
            reject(player, EchoAbilityType.VOID_ECHO, "blocked",
                    "message.unknown_echoes.void_step.blocked");
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    10, 0.3D, 0.5D, 0.3D, 0.02D);
            return;
        }
        int cost = ServerConfig.VOID_STEP_ENERGY_COST.get();
        if (cost > 0 && !ArtifactManager.spendEnergy(player, cost)) {
            EchoAbilityManager.recordFailure(player, EchoAbilityType.VOID_ECHO, "energy");
            return;
        }
        Vec3 origin = player.position();
        sendVoidPath(level, origin, destination);
        player.teleportTo(level, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        int wardDuration = ServerConfig.VOID_WARD_DURATION_TICKS.get();
        if (EchoArmorSets.abyssWalkerPieces(player) >= 4) {
            wardDuration = wardDuration * 3 / 2;
        }
        state.voidWardEnd = now + wardDuration;
        state.voidCooldownEnd = now + scaledCooldown(
                ServerConfig.VOID_STEP_COOLDOWN_TICKS.get(), research);
        player.getPersistentData().putLong(VOID_COOLDOWN_KEY, state.voidCooldownEnd);
        level.sendParticles(ParticleTypes.END_ROD,
                destination.x, destination.y + player.getBbHeight() * 0.5D, destination.z,
                24, 0.4D, 0.7D, 0.4D, 0.08D);
        sendCalibrationBurst(level, destination, 18, 0.55D);
        level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.void_step.started",
                String.format(Locale.ROOT, "%.1f", origin.distanceTo(destination)),
                String.format(Locale.ROOT, "%.1f", wardDuration / 20.0D)), true);
    }

    private static Vec3 findSafeVoidStep(ServerLevel level, ServerPlayer player, double range) {
        Vec3 horizontal = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return null;
        }
        horizontal = horizontal.normalize();
        Vec3 eye = player.getEyePosition();
        Vec3 rayEnd = eye.add(horizontal.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, rayEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reachable = hit.getType() == HitResult.Type.MISS
                ? range : Math.max(0.0D, eye.distanceTo(hit.getLocation()) - 0.65D);
        AABB original = player.getBoundingBox();
        for (double distance = reachable; distance >= 1.25D; distance -= 0.25D) {
            double x = player.getX() + horizontal.x * distance;
            double z = player.getZ() + horizontal.z * distance;
            for (int dy : new int[]{0, 1, -1, -2}) {
                BlockPos feet = BlockPos.containing(x, player.getY() + dy, z);
                Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                AABB moved = original.move(target.subtract(player.position()));
                if (!level.getWorldBorder().isWithinBounds(feet)
                        || !level.hasChunkAt(feet)
                        || !level.noCollision(player, moved)
                        || !Block.canSupportCenter(level, feet.below(), Direction.UP)) {
                    continue;
                }
                Vec3 targetEye = target.add(0.0D, player.getEyeHeight(), 0.0D);
                BlockHitResult finalRay = level.clip(new ClipContext(eye, targetEye,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (finalRay.getType() == HitResult.Type.MISS) {
                    return target;
                }
            }
        }
        return null;
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static void suppressNearbyEntities(ServerPlayer player, double radius) {
        AABB area = player.getBoundingBox().inflate(radius, Math.max(2.5D, radius * 0.65D), radius);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && candidate != player
                        && !(candidate instanceof Player)
                        && !candidate.getType().is(ModEntityTags.ECHO_BOSSES))) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2,
                    true, false, true));
            Vec3 motion = entity.getDeltaMovement();
            double downward = Math.min(motion.y - 0.08D, -0.28D);
            entity.setDeltaMovement(motion.x * 0.72D, downward, motion.z * 0.72D);
            entity.hurtMarked = true;
        }
    }

    /** 重力场边界环：环半径与实际压制范围相同，持续旋转显示研究强化后的覆盖范围。 */
    private static void sendGravityRing(ServerLevel level, Vec3 center, double radius, int phase) {
        int points = 18;
        double offset = (phase % 24) * (Math.PI * 2.0D / 24.0D);
        for (int i = 0; i < points; i++) {
            double angle = offset + i * (Math.PI * 2.0D / points);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, center.y + 0.12D, z,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
            if ((i + phase) % 6 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, x, center.y + 0.28D, z,
                        1, 0.0D, 0.04D, 0.0D, 0.0D);
            }
        }
    }

    private static void sendGravityCalibration(ServerLevel level, Vec3 center, double radius) {
        sendGravityRing(level, center, radius, 0);
        sendGravityRing(level, center.add(0.0D, 0.65D, 0.0D), radius * 0.72D, 6);
        sendCalibrationBurst(level, center.add(0.0D, 0.2D, 0.0D), 28, 0.75D);
    }

    /** 虚空路径标定：只在安全落点已通过服务端校验后绘制。 */
    private static void sendVoidPath(ServerLevel level, Vec3 origin, Vec3 destination) {
        Vec3 delta = destination.subtract(origin);
        int points = Math.max(6, (int) Math.ceil(delta.length() * 2.0D));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Vec3 point = origin.add(delta.scale(t));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y + 0.55D, point.z, 1,
                    0.04D, 0.12D, 0.04D, 0.02D);
        }
    }

    private static void sendCalibrationBurst(ServerLevel level, Vec3 center, int count, double spread) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, count,
                spread, spread * 0.7D, spread, 0.04D);
    }

    private static int gravityDuration(int research) {
        return Math.min(120, ServerConfig.GRAVITY_ACTIVE_DURATION_TICKS.get() + research * 12);
    }

    private static double gravityRadius(int research) {
        return Math.min(32.0D, ServerConfig.GRAVITY_ACTIVE_RADIUS.get() + research * 1.5D);
    }

    private static double voidDistance(int research) {
        return Math.min(16.0D, ServerConfig.VOID_STEP_DISTANCE.get() + research * 1.25D);
    }

    private static long scaledCooldown(int baseTicks, int research) {
        double factor = switch (Math.max(0, Math.min(4, research))) {
            case 1 -> 0.85D;
            case 2 -> 0.70D;
            case 3 -> 0.55D;
            case 4 -> 0.40D;
            default -> 1.0D;
        };
        return Math.max(1L, Math.round(baseTicks * factor));
    }

    /** 主世界时间跨维度共享且随存档持久化，适合写入玩家 NBT 的截止时间。 */
    private static long globalGameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static long cooldownEnd(ServerPlayer player, String key, long now) {
        long end = player.getPersistentData().getLong(key);
        if (end <= now) {
            player.getPersistentData().remove(key);
            return 0L;
        }
        return end;
    }

    private static void copyCooldown(Player source, Player target, String key) {
        if (source.getPersistentData().contains(key)) {
            target.getPersistentData().putLong(key, source.getPersistentData().getLong(key));
        }
    }

    private static void reject(ServerPlayer player, EchoAbilityType ability,
                               String failure, String message) {
        EchoAbilityManager.recordFailure(player, ability, failure);
        player.displayClientMessage(Component.translatable(message), true);
    }

    private static final class State {
        private long gravityEnd;
        private long gravityCooldownEnd;
        private double gravityRadius;
        private long voidWardEnd;
        private long voidCooldownEnd;
    }
}
