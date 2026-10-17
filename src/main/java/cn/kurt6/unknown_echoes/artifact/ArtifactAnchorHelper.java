package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.AbyssBeaconBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class ArtifactAnchorHelper {
    private static final int BIND_RANGE = 6;
    private static final int SAFE_SEARCH_RADIUS = 5;
    private static final double BOSS_LOCK_RANGE = 48.0D;
    private static final int MAX_BOUND_ANCHORS = 2;
    private static final int MAX_HORIZONTAL_COORDINATE = 30_000_000;
    private static final int MIN_PACKED_Y = -2048;
    private static final int MAX_PACKED_Y = 2047;

    private ArtifactAnchorHelper() {
    }

    static BindResult bindDimensionAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.DIMENSION_ANCHOR;
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        ServerLevel source = snapshot.source;
        if (!isBindingCurrent(player, snapshot)) {
            return new BindResult(Failure.INVALID_ANCHOR);
        }
        if (!isSupportedDimension(player.level().dimension())) {
            return new BindResult(Failure.UNSUPPORTED_DIMENSION);
        }
        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int minChunkX = (origin.getX() - BIND_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - BIND_RANGE) >> 4;
        int chunksZ = ((origin.getZ() + BIND_RANGE) >> 4) - minChunkZ + 1;
        int chunksX = ((origin.getX() + BIND_RANGE) >> 4) - minChunkX + 1;
        LevelChunk[] chunks = new LevelChunk[chunksX * chunksZ];
        boolean[] checked = new boolean[chunks.length];
        for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-BIND_RANGE, -3, -BIND_RANGE),
                origin.offset(BIND_RANGE, 3, BIND_RANGE))) {
            if (!isBindingCurrent(player, snapshot)) {
                return new BindResult(Failure.INVALID_ANCHOR);
            }
            double distance = cursor.distSqr(origin);
            if (source.isOutsideBuildHeight(cursor) || distance > BIND_RANGE * BIND_RANGE) {
                continue;
            }
            int index = ((cursor.getX() >> 4) - minChunkX) * chunksZ + (cursor.getZ() >> 4) - minChunkZ;
            if (!checked[index]) {
                chunks[index] = source.getChunkSource().getChunkNow(cursor.getX() >> 4, cursor.getZ() >> 4);
                checked[index] = true;
            }
            LevelChunk chunk = chunks[index];
            if (chunk == null) {
                continue;
            }
            BlockState state = chunk.getBlockState(cursor);
            if (!isBindingCurrent(player, snapshot)) {
                return new BindResult(Failure.INVALID_ANCHOR);
            }
            boolean authorized = isActivatedForPlayer(player, source, cursor, state);
            if (!authorized) {
                continue;
            }
            if (distance < nearestDistance) {
                nearest = cursor.immutable();
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            return new BindResult(Failure.NO_ANCHOR);
        }
        if (!isBindingCurrent(player, snapshot)
                || !isValidDimensionAnchor(player, source, nearest) || !isBindingCurrent(player, snapshot)) {
            return new BindResult(Failure.INVALID_ANCHOR);
        }
        if (hasBossNearby(source, nearest)) {
            return new BindResult(Failure.BOSS_NEARBY);
        }
        List<Anchor> anchors = boundAnchors(snapshot.binding, false);
        boolean dual = ArtifactManager.getData(player).getLevel(type) >= 2
                && "dual_anchor".equals(ArtifactManager.getData(player).getTuning(type));
        anchors = mergeAnchor(anchors, new Anchor(source.dimension(), nearest, 0L), dual);
        return new BindResult(writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors))
                ? Failure.NONE : Failure.INVALID_ANCHOR);
    }

    /** 记录当前坐标；身处目标维度不等于获得返回权限，传送时仍会重新校验进度。 */
    static BindResult bindRiftAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        BooleanSupplier canContinue = () -> isBindingCurrent(player, snapshot);
        if (!canContinue.getAsBoolean()) {
            return new BindResult(Failure.INVALID_ANCHOR);
        }
        BlockPos position = player.blockPosition();
        if (!isSupportedDimension(player.level().dimension())) {
            return new BindResult(Failure.UNSUPPORTED_DIMENSION);
        }
        if (hasBossNearby(player.serverLevel(), position)) {
            return new BindResult(Failure.BOSS_NEARBY);
        }
        if (safePosition(snapshot.source, player, player.getBoundingBox(), position, canContinue) == null) {
            return new BindResult(Failure.UNSAFE_POSITION);
        }
        List<Anchor> anchors = boundAnchors(snapshot.binding, true);
        long now = ArtifactManager.gameTime(player);
        normalizeRiftAnchors(player, anchors, now);
        boolean twin = ArtifactManager.getData(player).getLevel(type) >= 2
                && "twin_spike".equals(ArtifactManager.getData(player).getTuning(type));
        anchors = mergeAnchor(anchors, new Anchor(snapshot.source.dimension(), position.immutable(),
                maximumRiftExpiry(player, now)), twin);
        return new BindResult(writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors))
                ? Failure.NONE : Failure.INVALID_ANCHOR);
    }

    static boolean hasRiftBinding(ServerPlayer player) {
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), true);
        long now = ArtifactManager.gameTime(player);
        boolean changed = normalizeRiftAnchors(player, anchors, now);
        changed |= normalizeAnchorCount(player, type, anchors);
        if (changed) {
            persistBinding(player, type, anchors);
            // 同步中可能发生撤销或重绑；返回当前数据的判断，不把旧结果交给自动绑定入口。
            anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), true);
            normalizeRiftAnchors(player, anchors, ArtifactManager.gameTime(player));
            normalizeAnchorCount(player, type, anchors);
        }
        return !anchors.isEmpty();
    }

    /** 低频清理过期坐标，避免玩家长期不使用时数据残留。 */
    static void tickRiftAnchor(ServerPlayer player, ArtifactData data) {
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        String raw = data.getBinding(type);
        if (raw == null || raw.isBlank()) {
            return;
        }
        List<Anchor> anchors = boundAnchors(raw, true);
        long now = ArtifactManager.gameTime(player);
        boolean changed = normalizeRiftAnchors(player, anchors, now);
        changed |= normalizeAnchorCount(player, type, anchors);
        if (changed || !raw.equals(encodeBoundAnchors(anchors))) {
            persistBinding(player, type, anchors, data);
        }
    }

    static ResolveResult resolveDimensionAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.DIMENSION_ANCHOR;
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        if (!isBindingCurrent(player, snapshot)) {
            return new ResolveResult(null, Failure.INVALID_ANCHOR);
        }
        List<Anchor> anchors = boundAnchors(snapshot.binding, false);
        boolean changed = normalizeAnchorCount(player, type, anchors);
        Failure failure = Failure.INVALID_ANCHOR;
        for (int index = 0; index < anchors.size();) {
            Anchor anchor = anchors.get(index);
            if (!canEnterBoundDimension(player, anchor.dimension)) {
                failure = Failure.DIMENSION_LOCKED;
                index++;
                continue;
            }
            ServerLevel targetLevel = player.server.getLevel(anchor.dimension);
            if (targetLevel == null) {
                anchors.remove(index);
                changed = true;
                failure = Failure.INVALID_ANCHOR;
                continue;
            }
            BooleanSupplier canContinue = () -> isBindingCurrent(player, snapshot)
                    && player.server.getLevel(anchor.dimension) == targetLevel
                    && canEnterBoundDimension(player, anchor.dimension);
            if (!prepareChunks(targetLevel, player, anchor.pos, SAFE_SEARCH_RADIUS, canContinue)) {
                return new ResolveResult(null, Failure.INVALID_ANCHOR);
            }
            // 后续区块的准备可能处理卸载任务；暂不可读不能当作信标已拆除。
            if (!targetLevel.isOutsideBuildHeight(anchor.pos)
                    && targetLevel.getChunkSource().getChunkNow(anchor.pos.getX() >> 4, anchor.pos.getZ() >> 4) == null) {
                failure = Failure.UNSAFE_POSITION;
                index++;
                continue;
            }
            if (!isValidDimensionAnchor(player, targetLevel, anchor.pos)) {
                if (!canContinue.getAsBoolean()) {
                    return new ResolveResult(null, Failure.INVALID_ANCHOR);
                }
                anchors.remove(index);
                changed = true;
                failure = Failure.INVALID_ANCHOR;
                continue;
            }
            ResolveResult result = resolvePrepared(player, targetLevel, anchor, canContinue);
            if (!canContinue.getAsBoolean()) {
                return new ResolveResult(null, Failure.INVALID_ANCHOR);
            }
            if (result.destination != null) {
                if (changed && !writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors))) {
                    return new ResolveResult(null, Failure.INVALID_ANCHOR);
                }
                if (player.server.getLevel(anchor.dimension) != targetLevel
                        || !canEnterBoundDimension(player, anchor.dimension)) {
                    return new ResolveResult(null, Failure.INVALID_ANCHOR);
                }
                return result;
            }
            failure = result.failure;
            index++;
        }
        if (changed) {
            writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors));
        }
        return new ResolveResult(null, failure);
    }

    /** 调谐改变后立即收敛锚点数量与临时有效期，避免旧词条状态在下一次使用前继续生效。 */
    static void normalizeAfterTuning(ServerPlayer player, ArtifactType type) {
        if (type != ArtifactType.DIMENSION_ANCHOR && type != ArtifactType.RIFT_ANCHOR) {
            return;
        }
        ArtifactData data = ArtifactManager.getData(player);
        String raw = data.getBinding(type);
        if (raw == null || raw.isBlank()) {
            return;
        }
        boolean rift = type == ArtifactType.RIFT_ANCHOR;
        List<Anchor> anchors = boundAnchors(raw, rift);
        boolean changed = normalizeAnchorCount(player, type, anchors);
        if (rift) {
            changed |= normalizeRiftAnchors(player, anchors, ArtifactManager.gameTime(player));
        }
        String normalized = encodeBoundAnchors(anchors);
        if (changed || !raw.equals(normalized)) {
            data.setBinding(type, normalized);
        }
    }

    static ResolveResult resolveRiftAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        if (!isBindingCurrent(player, snapshot)) {
            return new ResolveResult(null, Failure.INVALID_ANCHOR);
        }
        List<Anchor> anchors = boundAnchors(snapshot.binding, true);
        long now = ArtifactManager.gameTime(player);
        int beforePrune = anchors.size();
        boolean changed = normalizeRiftAnchors(player, anchors, now);
        changed |= normalizeAnchorCount(player, type, anchors);
        Failure failure = beforePrune > 0 && anchors.isEmpty()
                ? Failure.EXPIRED : Failure.INVALID_ANCHOR;
        for (int index = 0; index < anchors.size();) {
            Anchor anchor = anchors.get(index);
            if (!canEnterBoundDimension(player, anchor.dimension)) {
                failure = Failure.DIMENSION_LOCKED;
                index++;
                continue;
            }
            ServerLevel targetLevel = player.server.getLevel(anchor.dimension);
            if (targetLevel == null) {
                anchors.remove(index);
                changed = true;
                failure = Failure.INVALID_ANCHOR;
                continue;
            }
            BooleanSupplier canContinue = () -> isBindingCurrent(player, snapshot)
                    && player.server.getLevel(anchor.dimension) == targetLevel
                    && canEnterBoundDimension(player, anchor.dimension) && isRiftExpiryCurrent(player, anchor.expiry);
            if (!prepareChunks(targetLevel, player, anchor.pos, SAFE_SEARCH_RADIUS, canContinue)) {
                return new ResolveResult(null, Failure.INVALID_ANCHOR);
            }
            ResolveResult result = resolvePrepared(player, targetLevel, anchor, canContinue);
            if (!canContinue.getAsBoolean()) {
                return new ResolveResult(null, Failure.INVALID_ANCHOR);
            }
            if (result.destination != null) {
                if (changed && !writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors))) {
                    return new ResolveResult(null, Failure.INVALID_ANCHOR);
                }
                if (player.server.getLevel(anchor.dimension) != targetLevel
                        || !canEnterBoundDimension(player, anchor.dimension) || !isRiftExpiryCurrent(player, anchor.expiry)) {
                    return new ResolveResult(null, Failure.INVALID_ANCHOR);
                }
                return result;
            }
            failure = result.failure;
            index++;
        }
        if (changed) {
            writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors));
        }
        return new ResolveResult(null, failure);
    }

    /** 异常存档不得把临时锚伪造成永久锚；上限随当前配置、等级和调谐动态计算。 */
    private static boolean normalizeRiftAnchors(ServerPlayer player, List<Anchor> anchors, long now) {
        long maximum = maximumRiftExpiry(player, now);
        boolean changed = false;
        for (int index = anchors.size() - 1; index >= 0; index--) {
            Anchor anchor = anchors.get(index);
            if (anchor.expiry <= now) {
                anchors.remove(index);
                changed = true;
            } else if (anchor.expiry > maximum) {
                anchors.set(index, new Anchor(anchor.dimension, anchor.pos, maximum));
                changed = true;
            }
        }
        return changed;
    }

    private static long maximumRiftExpiry(ServerPlayer player, long now) {
        long duration = ServerConfig.RIFT_ANCHOR_LIFETIME_TICKS.get()
                + Math.max(0, ArtifactManager.getData(player).getLevel(ArtifactType.RIFT_ANCHOR) - 1) * 2400L;
        if ("lingering".equals(ArtifactManager.getData(player).getTuning(ArtifactType.RIFT_ANCHOR))) {
            duration += duration / 2L;
        }
        return duration > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
    }

    static boolean teleport(ServerPlayer player, Destination destination) {
        Vec3 target = destination.target;
        player.teleportTo(destination.level, target.x, target.y, target.z,
                player.getYRot(), player.getXRot());
        if (player.serverLevel() != destination.level
                || player.position().distanceToSqr(target) > 0.01D) {
            return false;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        return true;
    }

    /** 只在传送成功后提交双锚轮换，避免失败请求改变下次目的地。 */
    static void commitUse(ServerPlayer player, ArtifactType type, Destination destination) {
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        if (!isBindingCurrent(player, snapshot) || !canEnterBoundDimension(player, destination.anchorDimension)
                || type == ArtifactType.RIFT_ANCHOR && !isRiftExpiryCurrent(player, destination.expiry)) {
            return;
        }
        List<Anchor> anchors = boundAnchors(snapshot.binding, type == ArtifactType.RIFT_ANCHOR);
        if (normalizeAnchorCount(player, type, anchors)) {
            String normalized = encodeBoundAnchors(anchors);
            if (!writeBindingIfCurrent(player, snapshot, normalized)) {
                return;
            }
            snapshot = snapshot.withBinding(normalized);
        }
        if (!supportsMultipleAnchors(player, type) || !canEnterBoundDimension(player, destination.anchorDimension)
                || type == ArtifactType.RIFT_ANCHOR && !isRiftExpiryCurrent(player, destination.expiry)) {
            return;
        }
        for (int index = 0; index < anchors.size(); index++) {
            Anchor anchor = anchors.get(index);
            if (anchor.dimension.equals(destination.anchorDimension)
                    && anchor.pos.equals(destination.anchorPos)
                    && anchor.expiry == destination.expiry) {
                rotateAfterUse(player, snapshot, anchors, index);
                return;
            }
        }
    }

    private static ResolveResult resolvePrepared(ServerPlayer player, ServerLevel targetLevel, Anchor anchor,
                                                 BooleanSupplier canContinue) {
        if (!canContinue.getAsBoolean()) {
            return new ResolveResult(null, Failure.INVALID_ANCHOR);
        }
        if (hasBossNearby(player.serverLevel(), player.blockPosition())) {
            return new ResolveResult(null, Failure.BOSS_NEARBY);
        }
        if (!canContinue.getAsBoolean()) {
            return new ResolveResult(null, Failure.INVALID_ANCHOR);
        }
        if (hasBossNearby(targetLevel, anchor.pos)) {
            return new ResolveResult(null, Failure.BOSS_NEARBY);
        }
        Vec3 safe = findSafeNear(targetLevel, player, anchor.pos, canContinue);
        if (safe == null) {
            return new ResolveResult(null, Failure.UNSAFE_POSITION);
        }
        if (hasBossNearby(targetLevel, BlockPos.containing(safe))) {
            return new ResolveResult(null, Failure.BOSS_NEARBY);
        }
        if (!canContinue.getAsBoolean()) {
            return new ResolveResult(null, Failure.INVALID_ANCHOR);
        }
        return new ResolveResult(new Destination(targetLevel, safe, anchor.expiry,
                anchor.dimension, anchor.pos), Failure.NONE);
    }

    private static boolean isValidDimensionAnchor(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && isActivatedForPlayer(player, level, pos, chunk.getBlockState(pos));
    }

    private static boolean isActivatedForPlayer(ServerPlayer player, ServerLevel level,
                                                BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.RESONANCE_BEACON.get())) {
            return EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID);
        }
        if (!state.is(ModBlocks.ABYSS_BEACON.get())) {
            return false;
        }
        return AbyssBeaconBlock.isCalibratedFor(player, level, pos);
    }

    private static boolean canEnterBoundDimension(ServerPlayer player, ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        if (Level.OVERWORLD.equals(dimension)) {
            return true;
        }
        if (id.equals(EchoPermission.ECHO_REALM_ID)) {
            return EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.ECHO_REALM_ID);
        }
        if (id.equals(EchoPermission.DEEP_ECHOES_ID)) {
            return EchoPermission.canEnterDeepEchoes(player);
        }
        return false;
    }

    private static boolean isSupportedDimension(ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        return Level.OVERWORLD.equals(dimension)
                || id.equals(EchoPermission.ECHO_REALM_ID)
                || id.equals(EchoPermission.DEEP_ECHOES_ID);
    }

    @Nullable
    private static Vec3 findSafeNear(ServerLevel level, ServerPlayer player, BlockPos anchor,
                                     BooleanSupplier canContinue) {
        AABB original = player.getBoundingBox();
        Vec3 exact = safePosition(level, player, original, anchor, canContinue);
        if (exact != null) {
            return exact;
        }
        for (int radius = 1; radius <= SAFE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int dy : new int[]{1, 0, 2, -1}) {
                        if (!canContinue.getAsBoolean()) {
                            return null;
                        }
                        BlockPos feet = anchor.offset(dx, dy, dz);
                        Vec3 target = safePosition(level, player, original, feet, canContinue);
                        if (target != null) {
                            return target;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Vec3 safePosition(ServerLevel level, ServerPlayer player, AABB original, BlockPos feet,
                                      BooleanSupplier canContinue) {
        Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
        AABB moved = original.move(target.subtract(player.position()));
        return canContinue.getAsBoolean() && level.getWorldBorder().isWithinBounds(moved)
                && hasReadableChunks(level, Mth.floor(moved.minX - 1.0e-7D) - 1,
                Mth.floor(moved.minZ - 1.0e-7D) - 1, Mth.floor(moved.maxX + 1.0e-7D) + 1,
                Mth.floor(moved.maxZ + 1.0e-7D) + 1)
                && canContinue.getAsBoolean() && level.noCollision(player, moved)
                && canContinue.getAsBoolean() && Block.canSupportCenter(level, feet.below(), Direction.UP)
                && canContinue.getAsBoolean() ? target : null;
    }

    private static boolean hasReadableChunks(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isDestinationCurrent(ServerPlayer player, ArtifactType type,
                                        Destination destination) {
        BindingSnapshot snapshot = bindingSnapshot(player, type);
        if (!isBindingCurrent(player, snapshot)) {
            return false;
        }
        if (!canEnterBoundDimension(player, destination.anchorDimension)
                || player.server.getLevel(destination.anchorDimension) != destination.level) {
            return false;
        }

        boolean rift = type == ArtifactType.RIFT_ANCHOR;
        List<Anchor> anchors = boundAnchors(
                snapshot.binding, rift);
        boolean changed = normalizeAnchorCount(player, type, anchors);
        if (rift) {
            long now = ArtifactManager.gameTime(player);
            changed |= normalizeRiftAnchors(player, anchors, now);
        }
        if (changed) {
            String normalized = encodeBoundAnchors(anchors);
            if (!writeBindingIfCurrent(player, snapshot, normalized)) {
                return false;
            }
            snapshot = snapshot.withBinding(normalized);
        }
        BindingSnapshot current = snapshot;
        BooleanSupplier canContinue = () -> isBindingCurrent(player, current)
                && canEnterBoundDimension(player, destination.anchorDimension)
                && player.server.getLevel(destination.anchorDimension) == destination.level
                && (!rift || isRiftExpiryCurrent(player, destination.expiry));
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        boolean stillBound = anchors.stream().anyMatch(anchor ->
                anchor.dimension.equals(destination.anchorDimension)
                        && anchor.pos.equals(destination.anchorPos)
                        && anchor.expiry == destination.expiry);
        if (!stillBound || !rift
                && !isValidDimensionAnchor(player, destination.level, destination.anchorPos)) {
            return false;
        }

        BlockPos feet = BlockPos.containing(destination.target);
        if (!canContinue.getAsBoolean() || hasBossNearby(player.serverLevel(), player.blockPosition())
                || !canContinue.getAsBoolean() || hasBossNearby(destination.level, destination.anchorPos)
                || !canContinue.getAsBoolean() || hasBossNearby(destination.level, feet)) {
            return false;
        }
        AABB original = player.getBoundingBox();
        return safePosition(destination.level, player, original, feet, canContinue) != null;
    }

    private static boolean isRiftExpiryCurrent(ServerPlayer player, long expiry) {
        long now = ArtifactManager.gameTime(player);
        return expiry > now && expiry <= maximumRiftExpiry(player, now);
    }

    private static boolean hasBossNearby(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BOSS_LOCK_RANGE);
        List<LivingEntity> bosses = new ArrayList<>(1);
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
                entity -> entity.isAlive() && (entity.getType().is(ModEntityTags.ECHO_BOSSES)
                        || entity.getType().is(Tags.EntityTypes.BOSSES))
                        && entity.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= BOSS_LOCK_RANGE * BOSS_LOCK_RANGE, bosses, 1);
        return !bosses.isEmpty();
    }

    private static boolean prepareChunks(ServerLevel level, ServerPlayer player, BlockPos center, int radiusBlocks,
                                          BooleanSupplier canContinue) {
        AABB footprint = player.getBoundingBox().move(new Vec3(center.getX() + 0.5D, center.getY(),
                center.getZ() + 0.5D).subtract(player.position())).inflate(radiusBlocks, 0.0D, radiusBlocks);
        // 原生 BlockCollisions 会读取身体边缘外一格；搜索半径还需加上玩家的实际体积。
        int minChunkX = (Mth.floor(footprint.minX - 1.0e-7D) - 1) >> 4;
        int maxChunkX = (Mth.floor(footprint.maxX + 1.0e-7D) + 1) >> 4;
        int minChunkZ = (Mth.floor(footprint.minZ - 1.0e-7D) - 1) >> 4;
        int maxChunkZ = (Mth.floor(footprint.maxZ + 1.0e-7D) + 1) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                level.getChunk(x, z);
                if (!canContinue.getAsBoolean()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String encode(ResourceKey<Level> dimension, BlockPos pos, long expiry) {
        return "v2|" + dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|"
                + pos.getZ() + "|" + Math.max(0L, expiry);
    }

    private static String encodeCoordinate(ResourceKey<Level> dimension, BlockPos pos, long expiry) {
        return "coord@" + dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|"
                + pos.getZ() + "|" + Math.max(0L, expiry);
    }

    private static List<Anchor> boundAnchors(String value, boolean coordinateOnly) {
        List<Anchor> anchors = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return anchors;
        }
        for (String part : value.split(";", -1)) {
            Anchor anchor = decode(part, coordinateOnly);
            if (anchor != null) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

    private static String encodeBoundAnchors(List<Anchor> anchors) {
        StringBuilder result = new StringBuilder();
        for (Anchor anchor : anchors) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(anchor.expiry > 0L
                    ? encodeCoordinate(anchor.dimension, anchor.pos, anchor.expiry)
                    : encode(anchor.dimension, anchor.pos, anchor.expiry));
            if (result.length() > 900) {
                break;
            }
        }
        return result.toString();
    }

    private static List<Anchor> mergeAnchor(List<Anchor> anchors, Anchor next, boolean allowTwo) {
        anchors.removeIf(anchor -> anchor.dimension.equals(next.dimension) && anchor.pos.equals(next.pos));
        if (!allowTwo) {
            anchors.clear();
        }
        anchors.add(next);
        if (anchors.size() > MAX_BOUND_ANCHORS) {
            anchors.subList(0, anchors.size() - MAX_BOUND_ANCHORS).clear();
        }
        return anchors;
    }

    /** 调谐关闭双点能力后，保留下一次将要使用的首个锚点，避免旧存档继续轮换。 */
    private static boolean normalizeAnchorCount(ServerPlayer player, ArtifactType type,
                                                List<Anchor> anchors) {
        int allowed = supportsMultipleAnchors(player, type) ? MAX_BOUND_ANCHORS : 1;
        if (anchors.size() <= allowed) {
            return false;
        }
        anchors.subList(allowed, anchors.size()).clear();
        return true;
    }

    private static boolean supportsMultipleAnchors(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case DIMENSION_ANCHOR -> ArtifactManager.getData(player).getLevel(type) >= 2
                    && "dual_anchor".equals(ArtifactManager.getData(player).getTuning(type));
            case RIFT_ANCHOR -> ArtifactManager.getData(player).getLevel(type) >= 2
                    && "twin_spike".equals(ArtifactManager.getData(player).getTuning(type));
            default -> false;
        };
    }

    private static void rotateAfterUse(ServerPlayer player, BindingSnapshot snapshot,
                                       List<Anchor> anchors, int index) {
        if (anchors.size() <= 1 || index < 0 || index >= anchors.size()) {
            return;
        }
        Anchor used = anchors.remove(index);
        anchors.add(used);
        writeBindingIfCurrent(player, snapshot, encodeBoundAnchors(anchors));
    }

    private static BindingSnapshot bindingSnapshot(ServerPlayer player, ArtifactType type) {
        ArtifactData data = ArtifactManager.getData(player);
        return new BindingSnapshot(type, data, data.getBinding(type), data.getSerial(type),
                data.getLevel(type), data.getTuning(type), player.serverLevel());
    }

    private static boolean isBindingCurrent(ServerPlayer player, BindingSnapshot snapshot) {
        ArtifactData data = snapshot.data;
        return player.isAlive() && !player.isRemoved() && !player.isSpectator() && !player.hasDisconnected()
                && player.level() == snapshot.source
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.server.getLevel(snapshot.source.dimension()) == snapshot.source
                && ArtifactManager.isEnabled(snapshot.type) && ArtifactManager.getData(player) == data
                && data.isClaimed(snapshot.type) && data.getSerial(snapshot.type) == snapshot.serial
                && data.getLevel(snapshot.type) == snapshot.level
                && data.getTuning(snapshot.type).equals(snapshot.tuning)
                && data.getBinding(snapshot.type).equals(snapshot.binding);
    }

    /** 区块等待和同步能执行其他任务；只提交仍属于当前会话和绑定的数据。 */
    private static boolean writeBindingIfCurrent(ServerPlayer player, BindingSnapshot snapshot, String binding) {
        if (!isBindingCurrent(player, snapshot)) {
            return false;
        }
        snapshot.data.setBinding(snapshot.type, binding);
        BindingSnapshot written = snapshot.withBinding(binding);
        if (!isBindingCurrent(player, written)) {
            return false;
        }
        ArtifactManager.syncToClient(player);
        return isBindingCurrent(player, written);
    }

    private static void persistBinding(ServerPlayer player, ArtifactType type, List<Anchor> anchors) {
        persistBinding(player, type, anchors, ArtifactManager.getData(player));
    }

    private static void persistBinding(ServerPlayer player, ArtifactType type,
                                       List<Anchor> anchors, ArtifactData data) {
        data.setBinding(type, encodeBoundAnchors(anchors));
        ArtifactManager.syncToClient(player);
    }

    @Nullable
    private static Anchor decode(String value, boolean coordinateOnly) {
        if (value == null || value.isBlank()) {
            return null;
        }
        boolean coordinate = value.startsWith("coord@");
        if (coordinateOnly && !coordinate) {
            return null;
        }
        if (coordinate) {
            value = value.substring("coord@".length());
        } else if (value.startsWith("v2|")) {
            value = value.substring(3);
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 5) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(parts[0]);
        if (id == null) {
            return null;
        }
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            if (Math.abs((long) x) > MAX_HORIZONTAL_COORDINATE
                    || Math.abs((long) z) > MAX_HORIZONTAL_COORDINATE
                    || y < MIN_PACKED_Y || y > MAX_PACKED_Y) {
                return null;
            }
            BlockPos pos = new BlockPos(x, y, z);
            return new Anchor(dimension, pos, Long.parseLong(parts[4]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    record Destination(ServerLevel level, Vec3 target, long expiry,
                       ResourceKey<Level> anchorDimension, BlockPos anchorPos) {
    }

    record BindResult(Failure failure) {
        boolean success() {
            return failure == Failure.NONE;
        }
    }

    record ResolveResult(@Nullable Destination destination, Failure failure) {
    }

    enum Failure {
        NONE,
        NO_ANCHOR,
        UNSUPPORTED_DIMENSION,
        DIMENSION_LOCKED,
        BOSS_NEARBY,
        UNSAFE_POSITION,
        INVALID_ANCHOR,
        EXPIRED
    }

    private record Anchor(ResourceKey<Level> dimension, BlockPos pos, long expiry) {
    }

    private record BindingSnapshot(ArtifactType type, ArtifactData data, String binding, int serial,
                                   int level, String tuning, ServerLevel source) {
        private BindingSnapshot withBinding(String value) {
            return new BindingSnapshot(type, data, value, serial, level, tuning, source);
        }
    }
}
