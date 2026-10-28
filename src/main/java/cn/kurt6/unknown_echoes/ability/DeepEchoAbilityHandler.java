package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.network.AbilityCooldownSyncPayload;
import cn.kurt6.unknown_echoes.network.EchoAbilityActionPayload;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 重力与虚空回响的瞬态体验能力。持久进度仍只存于 EchoAbilityData。 */
public final class DeepEchoAbilityHandler {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Set<UUID> TICKING = new HashSet<>();
    private static final Set<UUID> ACTIVATING = new HashSet<>();
    private static final String GRAVITY_COOLDOWN_KEY = "ue_gravity_echo_cooldown_until";
    private static final String GRAVITY_COOLDOWN_DURATION_KEY = "ue_gravity_echo_cooldown_duration";
    private static final String VOID_COOLDOWN_KEY = "ue_void_echo_cooldown_until";
    private static final String VOID_COOLDOWN_DURATION_KEY = "ue_void_echo_cooldown_duration";

    private DeepEchoAbilityHandler() {
    }

    public static void handle(ServerPlayer player, byte action) {
        if (!player.isAlive() || player.isSpectator() || player.isRemoved() || player.hasDisconnected()
                || !(player.level() instanceof ServerLevel) || !ACTIVATING.add(player.getUUID())) {
            return;
        }
        try {
            switch (action) {
                case EchoAbilityActionPayload.ACTION_GRAVITY_ATTUNE -> useGravityAttunement(player);
                case EchoAbilityActionPayload.ACTION_VOID_STEP -> useVoidStep(player);
                default -> {
                }
            }
        } finally {
            ACTIVATING.remove(player.getUUID());
        }
    }

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.isRemoved() || player.hasDisconnected()
                || !(player.level() instanceof ServerLevel level)) {
            STATES.remove(player.getUUID(), state);
            return;
        }
        if (!TICKING.add(player.getUUID())) {
            return;
        }
        try {
            Vec3 origin = player.position();
            long now = globalGameTime(player);
            long gravityEnd = state.gravityEnd;
            long wardEnd = state.voidWardEnd;
            int gravityRevision = state.gravityRevision;
            int wardRevision = state.voidRevision;
            if (gravityEnd > now) {
                if (player.tickCount % 4 == 0) {
                    suppressNearbyEntities(player, state.gravityRadius);
                }
                if (player.tickCount % 6 == 0
                        && isCurrentGravityField(player, level, state, origin, gravityEnd, gravityRevision)) {
                    sendGravityRing(level, origin, state.gravityRadius, player.tickCount / 6);
                }
            }
            if (wardEnd > now) {
                for (var effect : java.util.List.of(MobEffects.DARKNESS, MobEffects.WEAKNESS,
                        MobEffects.MOVEMENT_SLOWDOWN)) {
                    if (!isCurrentVoidWard(player, level, state, origin, wardEnd, wardRevision)) {
                        break;
                    }
                    player.removeEffect(effect);
                }
            }
            if (state.gravityEnd <= now && state.voidWardEnd <= now) {
                STATES.remove(player.getUUID(), state);
            }
        } finally {
            TICKING.remove(player.getUUID());
        }
    }

    public static boolean isGravityAttuned(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && state.gravityEnd > globalGameTime(player);
    }

    public static boolean isVoidWardActive(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && state.voidWardEnd > globalGameTime(player);
    }

    /** 撤销单项能力时停止其持续效果，保留另一项能力及已付出的冷却。 */
    public static void onAbilityRevoked(ServerPlayer player, EchoAbilityType ability) {
        if (ability != EchoAbilityType.GRAVITY_ECHO && ability != EchoAbilityType.VOID_ECHO) {
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (ability == EchoAbilityType.GRAVITY_ECHO) {
            state.gravityRevision++;
            state.gravityEnd = 0L;
        } else {
            state.voidRevision++;
            state.voidWardEnd = 0L;
        }
        long now = globalGameTime(player);
        if (state.gravityEnd <= now && state.voidWardEnd <= now) {
            STATES.remove(player.getUUID());
        }
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void resetTestCooldowns(ServerPlayer player) {
        player.getPersistentData().remove(GRAVITY_COOLDOWN_KEY);
        player.getPersistentData().remove(GRAVITY_COOLDOWN_DURATION_KEY);
        player.getPersistentData().remove(VOID_COOLDOWN_KEY);
        player.getPersistentData().remove(VOID_COOLDOWN_DURATION_KEY);
        syncCooldowns(player);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public static void copyCooldowns(Player source, Player target) {
        copyCooldown(source, target, GRAVITY_COOLDOWN_KEY);
        copyCooldownDuration(source, target, GRAVITY_COOLDOWN_DURATION_KEY);
        copyCooldown(source, target, VOID_COOLDOWN_KEY);
        copyCooldownDuration(source, target, VOID_COOLDOWN_DURATION_KEY);
    }

    public static void syncCooldowns(ServerPlayer player) {
        long now = globalGameTime(player);
        int maxGravityCooldown = maxCooldown(ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS.get());
        int maxVoidCooldown = maxCooldown(ServerConfig.VOID_STEP_COOLDOWN_TICKS.get());
        long gravityEnd = cooldownEnd(player, GRAVITY_COOLDOWN_KEY, now, maxGravityCooldown);
        long voidEnd = cooldownEnd(player, VOID_COOLDOWN_KEY, now, maxVoidCooldown);
        int gravityDuration = cooldownDuration(player, GRAVITY_COOLDOWN_DURATION_KEY,
                gravityEnd, now, maxGravityCooldown);
        int voidDuration = cooldownDuration(player, VOID_COOLDOWN_DURATION_KEY,
                voidEnd, now, maxVoidCooldown);
        PacketDistributor.sendToPlayer(player, new AbilityCooldownSyncPayload(
                gravityEnd, gravityDuration, voidEnd, voidDuration, now));
    }

    private static void useGravityAttunement(ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO)) {
            reject(player, EchoAbilityType.GRAVITY_ECHO, "locked",
                    "message.unknown_echoes.gravity_active.locked");
            return;
        }
        State state = state(player);
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        int revision = state.gravityRevision;
        long now = globalGameTime(player);
        long gravityCooldownEnd = cooldownEnd(player, GRAVITY_COOLDOWN_KEY, now,
                maxCooldown(ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS.get()));
        if (gravityCooldownEnd > now) {
            reject(player, EchoAbilityType.GRAVITY_ECHO, "cooldown",
                    "message.unknown_echoes.gravity_active.cooldown");
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.GRAVITY_ECHO);
        int duration = gravityDurationTicks(ServerConfig.GRAVITY_ACTIVE_DURATION_TICKS.get(), research);
        if (EchoArmorSets.abyssWalkerPieces(player) >= 4) {
            duration = duration * 5 / 4;
        }
        if (EchoArmorSets.hasResonatorAbilityBonus(player)) {
            duration = duration * 11 / 10;
        }
        double radius = gravityRadius(ServerConfig.GRAVITY_ACTIVE_RADIUS.get(), research);
        int cooldownDuration = (int) scaledCooldown(
                ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS.get(), research);
        if (EchoArmorSets.hasResonatorAbilityBonus(player)) {
            cooldownDuration = cooldownDuration * 9 / 10;
        }
        ArtifactManager.EnergyPayment payment = ArtifactManager.spendEnergyQuietly(player,
                ServerConfig.GRAVITY_ACTIVE_ENERGY_COST.get());
        if (payment == null) {
            if (isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.GRAVITY_ECHO, revision)) {
                EchoAbilityManager.recordFailure(player, EchoAbilityType.GRAVITY_ECHO, "energy");
            }
            return;
        }
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.GRAVITY_ECHO, revision)) {
            ArtifactManager.refundSpentEnergy(player, payment);
            ArtifactManager.syncToClient(player);
            return;
        }
        // 持续效果与冷却先共同提交；同步回调撤权后不得恢复已撤销状态。
        revision = ++state.gravityRevision;
        state.gravityEnd = safeDeadline(now, duration);
        state.gravityRadius = radius;
        gravityCooldownEnd = safeDeadline(now, cooldownDuration);
        player.getPersistentData().putLong(GRAVITY_COOLDOWN_KEY, gravityCooldownEnd);
        player.getPersistentData().putInt(GRAVITY_COOLDOWN_DURATION_KEY, cooldownDuration);
        if (payment.needsSync()) {
            ArtifactManager.syncToClient(player);
        }
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.GRAVITY_ECHO, revision)) {
            return;
        }
        syncCooldowns(player);
        if (isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.GRAVITY_ECHO, revision)) {
            sendGravityCalibration(level, origin, radius);
        }
        if (isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.GRAVITY_ECHO, revision)) {
            level.playSound(null, player.blockPosition(), ModSounds.ABILITY_GRAVITY_ECHO_ATTUNE.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
        }
    }

    private static void useVoidStep(ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.VOID_ECHO)) {
            reject(player, EchoAbilityType.VOID_ECHO, "locked",
                    "message.unknown_echoes.void_step.locked");
            return;
        }
        State state = state(player);
        long now = globalGameTime(player);
        long voidCooldownEnd = cooldownEnd(player, VOID_COOLDOWN_KEY, now,
                maxCooldown(ServerConfig.VOID_STEP_COOLDOWN_TICKS.get()));
        if (voidCooldownEnd > now) {
            reject(player, EchoAbilityType.VOID_ECHO, "cooldown",
                    "message.unknown_echoes.void_step.cooldown");
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 origin = player.position();
        int revision = state.voidRevision;
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.VOID_ECHO);
        Vec3 destination = findSafeVoidStep(level, player, voidDistance(research));
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)) {
            return;
        }
        if (destination == null) {
            reject(player, EchoAbilityType.VOID_ECHO, "blocked",
                    "message.unknown_echoes.void_step.blocked");
            if (isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        origin.x, origin.y + 0.9D, origin.z, 10, 0.3D, 0.5D, 0.3D, 0.02D);
            }
            return;
        }
        int wardDuration = ServerConfig.VOID_WARD_DURATION_TICKS.get();
        if (EchoArmorSets.abyssWalkerPieces(player) >= 4) {
            wardDuration = wardDuration * 3 / 2;
        }
        if (EchoArmorSets.hasResonatorAbilityBonus(player)) {
            wardDuration = wardDuration * 6 / 5;
        }
        int cooldownDuration = (int) scaledCooldown(
                ServerConfig.VOID_STEP_COOLDOWN_TICKS.get(), research);
        if (EchoArmorSets.hasResonatorAbilityBonus(player)) {
            cooldownDuration = cooldownDuration * 9 / 10;
        }
        ArtifactManager.EnergyPayment payment = ArtifactManager.spendEnergyQuietly(player,
                ServerConfig.VOID_STEP_ENERGY_COST.get());
        if (payment == null) {
            if (isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)) {
                EchoAbilityManager.recordFailure(player, EchoAbilityType.VOID_ECHO, "energy");
            }
            return;
        }
        voidCooldownEnd = safeDeadline(now, cooldownDuration);
        boolean teleported = false;
        try {
            player.getPersistentData().putLong(VOID_COOLDOWN_KEY, voidCooldownEnd);
            player.getPersistentData().putInt(VOID_COOLDOWN_DURATION_KEY, cooldownDuration);
            if (payment.needsSync()) {
                ArtifactManager.syncToClient(player);
            }
            if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)) {
                return;
            }
            syncCooldowns(player);
            if (!canVoidStepTo(player, level, state, origin, revision, destination)) {
                return;
            }
            try {
                player.teleportTo(level, destination.x, destination.y, destination.z,
                        player.getYRot(), player.getXRot());
            } finally {
                // 传送后的通知可能抛错；费用依据实际落点结算，不能把已完成移动当作失败退款。
                teleported = player.level() == level && player.position().distanceToSqr(destination) <= 0.01D;
            }
            if (!teleported || !isCurrentAbilityCast(player, level, state, destination, EchoAbilityType.VOID_ECHO, revision)) {
                return;
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            revision = ++state.voidRevision;
            state.voidWardEnd = safeDeadline(now, wardDuration);
            sendVoidPath(level, origin, destination);
            if (!isCurrentAbilityCast(player, level, state, destination, EchoAbilityType.VOID_ECHO, revision)) {
                return;
            }
            level.sendParticles(ParticleTypes.END_ROD,
                    destination.x, destination.y + player.getBbHeight() * 0.5D, destination.z,
                    6, 0.25D, 0.35D, 0.25D, 0.02D);
            if (!isCurrentAbilityCast(player, level, state, destination, EchoAbilityType.VOID_ECHO, revision)) {
                return;
            }
            sendCalibrationBurst(level, destination, 6, 0.4D);
            if (isCurrentAbilityCast(player, level, state, destination, EchoAbilityType.VOID_ECHO, revision)) {
                level.playSound(null, BlockPos.containing(destination), ModSounds.ABILITY_VOID_ECHO_STEP.get(),
                        SoundSource.PLAYERS, 0.9F, 1.0F);
            }
        } finally {
            if (!teleported) {
                ArtifactManager.refundSpentEnergy(player, payment);
                if (player.getPersistentData().getLong(VOID_COOLDOWN_KEY) == voidCooldownEnd
                        && player.getPersistentData().getInt(VOID_COOLDOWN_DURATION_KEY) == cooldownDuration) {
                    player.getPersistentData().remove(VOID_COOLDOWN_KEY);
                    player.getPersistentData().remove(VOID_COOLDOWN_DURATION_KEY);
                }
                if (!player.hasDisconnected()) {
                    ArtifactManager.syncToClient(player);
                    syncCooldowns(player);
                }
            }
        }
    }

    private static Vec3 findSafeVoidStep(ServerLevel level, ServerPlayer player, double range) {
        State state = STATES.get(player.getUUID());
        Vec3 origin = player.position();
        AABB original = player.getBoundingBox();
        if (state == null) {
            return null;
        }
        int revision = state.voidRevision;
        Vec3 horizontal = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return null;
        }
        horizontal = horizontal.normalize();
        Vec3 eye = player.getEyePosition();
        Vec3 rayEnd = eye.add(horizontal.scale(range));
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                || !hasVoidRayChunks(level, eye, rayEnd)) {
            return null;
        }
        BlockHitResult hit = level.clip(new ClipContext(eye, rayEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                || !original.equals(player.getBoundingBox())) {
            return null;
        }
        double reachable = hit.getType() == HitResult.Type.MISS
                ? range : Math.max(0.0D, eye.distanceTo(hit.getLocation()) - 0.65D);
        for (double distance = reachable; distance >= 1.25D; distance -= 0.25D) {
            double x = origin.x + horizontal.x * distance;
            double z = origin.z + horizontal.z * distance;
            for (int dy : new int[]{0, 1, -1, -2}) {
                if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                        || !original.equals(player.getBoundingBox())) {
                    return null;
                }
                BlockPos feet = BlockPos.containing(x, origin.y + dy, z);
                Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                if (canVoidStepTo(player, level, state, origin, revision, target)) {
                    return target;
                }
            }
        }
        return null;
    }

    private static boolean isCurrentAbilityCast(ServerPlayer player, ServerLevel level, State state,
                                                Vec3 origin, EchoAbilityType ability, int revision) {
        return isCurrentFieldOwner(player, level, state, origin) && EchoAbilityManager.hasAbility(player, ability)
                && (ability == EchoAbilityType.GRAVITY_ECHO ? state.gravityRevision : state.voidRevision) == revision;
    }

    private static boolean hasVoidRayChunks(ServerLevel level, Vec3 from, Vec3 to) {
        if (!Double.isFinite(from.x) || !Double.isFinite(from.y) || !Double.isFinite(from.z)
                || !Double.isFinite(to.x) || !Double.isFinite(to.y) || !Double.isFinite(to.z)) {
            return false;
        }
        return BlockGetter.traverseBlocks(from, to, level, (world, pos) ->
                world.isOutsideBuildHeight(pos)
                        || world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                        ? null : Boolean.FALSE, world -> Boolean.TRUE);
    }

    private static boolean canVoidStepTo(ServerPlayer player, ServerLevel level, State state,
                                         Vec3 origin, int revision, Vec3 target) {
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)) {
            return false;
        }
        AABB original = player.getBoundingBox();
        AABB moved = original.move(target.subtract(origin));
        BlockPos floor = BlockPos.containing(target).below();
        if (level.isOutsideBuildHeight(floor) || !level.getWorldBorder().isWithinBounds(moved)
                || !EntitySpawnSafety.areCollisionChunksReady(level, moved) || !level.noCollision(player, moved)) {
            return false;
        }
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                || !original.equals(player.getBoundingBox()) || !EntitySpawnSafety.areCollisionChunksReady(level, moved)
                || !Block.canSupportCenter(level, floor, Direction.UP)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 targetEye = target.add(0.0D, player.getEyeHeight(), 0.0D);
        if (!isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                || !original.equals(player.getBoundingBox()) || !hasVoidRayChunks(level, eye, targetEye)) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(eye, targetEye,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS
                && isCurrentAbilityCast(player, level, state, origin, EchoAbilityType.VOID_ECHO, revision)
                && original.equals(player.getBoundingBox()) && level.getWorldBorder().isWithinBounds(moved)
                && EntitySpawnSafety.areCollisionChunksReady(level, moved);
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static void suppressNearbyEntities(ServerPlayer player, double radius) {
        State state = STATES.get(player.getUUID());
        if (state == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position();
        long end = state.gravityEnd;
        int revision = state.gravityRevision;
        if (!isCurrentGravityField(player, level, state, center, end, revision)) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius, Math.max(2.5D, radius * 0.65D), radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> isGravityTarget(level, candidate, area, center, radius))) {
            if (!isCurrentGravityField(player, level, state, center, end, revision)) {
                return;
            }
            if (!isGravityTarget(level, entity, area, center, radius)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2,
                    true, false, true));
            if (!isCurrentGravityField(player, level, state, center, end, revision)) {
                return;
            }
            if (!isGravityTarget(level, entity, area, center, radius)) {
                continue;
            }
            Vec3 motion = entity.getDeltaMovement();
            double downward = Math.min(motion.y - 0.08D, -0.28D);
            entity.setDeltaMovement(motion.x * 0.72D, downward, motion.z * 0.72D);
            entity.hurtMarked = true;
        }
    }

    private static boolean isCurrentFieldOwner(ServerPlayer player, ServerLevel level, State state, Vec3 origin) {
        return STATES.get(player.getUUID()) == state && player.level() == level
                && player.isAlive() && !player.isSpectator() && !player.isRemoved() && !player.hasDisconnected()
                && player.position().equals(origin);
    }

    private static boolean isCurrentGravityField(ServerPlayer player, ServerLevel level, State state,
                                                Vec3 origin, long end, int revision) {
        return isCurrentFieldOwner(player, level, state, origin) && state.gravityEnd == end
                && state.gravityRevision == revision && end > globalGameTime(player);
    }

    private static boolean isCurrentVoidWard(ServerPlayer player, ServerLevel level, State state,
                                            Vec3 origin, long end, int revision) {
        return isCurrentFieldOwner(player, level, state, origin) && state.voidWardEnd == end
                && state.voidRevision == revision && end > globalGameTime(player);
    }

    private static boolean isGravityTarget(ServerLevel level, LivingEntity entity, AABB area,
                                            Vec3 center, double radius) {
        double dx = entity.getX() - center.x;
        double dz = entity.getZ() - center.z;
        return entity.level() == level && entity.isAlive() && !entity.isRemoved()
                && !(entity instanceof Player) && area.intersects(entity.getBoundingBox())
                && dx * dx + dz * dz <= radius * radius
                && !entity.getType().is(ModEntityTags.ECHO_BOSSES)
                && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** 重力场边界环：环半径与实际压制范围相同，持续旋转显示研究强化后的覆盖范围。 */
    private static void sendGravityRing(ServerLevel level, Vec3 center, double radius, int phase) {
        CombatFx.emit(level, Shape.SIGIL, center.add(0, 0.12, 0), Vec3.ZERO,
                radius, 0, 0, 0xA59CFF, 0x514CC8, 10, false);
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
        CombatFx.emit(level, Shape.WAKE, origin.add(0, 0.55, 0), delta,
                .16, delta.length(), 0, 0xDBB6FF, 0x6150C9, 12, false);
        CombatFx.emit(level, Shape.COLLAPSE, origin.add(0, 1.1, 0), Vec3.ZERO,
                0.85, 2.2, 0, 0xDBB6FF, 0x6150C9, 10, false);
        CombatFx.emit(level, Shape.ARRIVAL, destination.add(0, 1.1, 0), Vec3.ZERO,
                0.85, 2.2, 0, 0xDBB6FF, 0x6150C9, 16, false);
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
        CombatFx.emit(level, Shape.BURST, center, Vec3.ZERO,
                spread * 1.5, 0, 0, 0xC7CCFF, 0x616BD2, 10, false);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, count,
                spread, spread * 0.7D, spread, 0.04D);
    }

    public static int gravityDurationTicks(int baseTicks, int research) {
        return Math.min(1200, Math.max(20, baseTicks) + Math.max(0, research) * 12);
    }

    public static double gravityRadius(double baseRadius, int research) {
        return Math.min(32.0D, Math.max(1.0D, baseRadius) + Math.max(0, research) * 1.5D);
    }

    private static double voidDistance(int research) {
        return Math.min(16.0D, ServerConfig.VOID_STEP_DISTANCE.get() + research * 1.25D);
    }

    /** 研究等级冷却折减(0-4 级 → 100%/85%/70%/55%/40%);UI 提示与实际结算共用,避免文本写死脱节。 */
    public static long scaledCooldown(int baseTicks, int research) {
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

    private static int maxCooldown(int configuredTicks) {
        return Math.max(1, configuredTicks);
    }

    private static long safeDeadline(long now, long duration) {
        long boundedDuration = Math.max(0L, duration);
        return now > Long.MAX_VALUE - boundedDuration
                ? Long.MAX_VALUE : now + boundedDuration;
    }

    private static long cooldownEnd(ServerPlayer player, String key, long now, int maxDuration) {
        long end = player.getPersistentData().getLong(key);
        if (end <= now) {
            player.getPersistentData().remove(key);
            return 0L;
        }
        long latestValidEnd = safeDeadline(now, maxDuration);
        if (end > latestValidEnd) {
            player.getPersistentData().putLong(key, latestValidEnd);
            return latestValidEnd;
        }
        return end;
    }

    private static int cooldownDuration(ServerPlayer player, String key, long end, long now,
                                        int maxDuration) {
        if (end <= now) {
            player.getPersistentData().remove(key);
            return 0;
        }
        int remaining = (int) Math.min(maxDuration, end - now);
        int stored = player.getPersistentData().getInt(key);
        int normalized = Math.max(remaining, Math.min(maxDuration, Math.max(1, stored)));
        if (stored != normalized) {
            player.getPersistentData().putInt(key, normalized);
        }
        return normalized;
    }

    private static void copyCooldown(Player source, Player target, String key) {
        if (source.getPersistentData().contains(key)) {
            target.getPersistentData().putLong(key, source.getPersistentData().getLong(key));
        }
    }

    private static void copyCooldownDuration(Player source, Player target, String key) {
        if (source.getPersistentData().contains(key)) {
            target.getPersistentData().putInt(key, source.getPersistentData().getInt(key));
        }
    }

    private static void reject(ServerPlayer player, EchoAbilityType ability,
                               String failure, String message) {
        EchoAbilityManager.recordFailure(player, ability, failure);
        player.displayClientMessage(Component.translatable(message), true);
    }

    private static final class State {
        private long gravityEnd;
        private double gravityRadius;
        private long voidWardEnd;
        private int gravityRevision;
        private int voidRevision;
    }
}
