package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 维度锚与裂隙锚共享的个人绑定编码及安全传送校验。 */
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
        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-BIND_RANGE, -3, -BIND_RANGE),
                origin.offset(BIND_RANGE, 3, BIND_RANGE))) {
            BlockState state = player.level().getBlockState(cursor);
            boolean authorized = cursor.distSqr(origin) <= BIND_RANGE * BIND_RANGE
                    && isActivatedForPlayer(player, player.serverLevel(), cursor, state);
            if (!authorized) {
                continue;
            }
            double distance = cursor.distSqr(origin);
            if (distance < nearestDistance) {
                nearest = cursor.immutable();
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            return new BindResult(Failure.NO_ANCHOR);
        }
        if (hasBossNearby(player.serverLevel(), nearest)) {
            return new BindResult(Failure.BOSS_NEARBY);
        }
        ArtifactType type = ArtifactType.DIMENSION_ANCHOR;
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), false);
        boolean dual = ArtifactManager.getData(player).getLevel(type) >= 2
                && "dual_anchor".equals(ArtifactManager.getData(player).getTuning(type));
        anchors = mergeAnchor(anchors, new Anchor(player.level().dimension(), nearest, 0L), dual);
        ArtifactManager.getData(player).setBinding(type, encodeBoundAnchors(anchors));
        ArtifactManager.syncToClient(player);
        return new BindResult(Failure.NONE);
    }

    /** 记录当前坐标；身处目标维度不等于获得返回权限，传送时仍会重新校验进度。 */
    static BindResult bindRiftAnchor(ServerPlayer player, long expiry) {
        BlockPos position = player.blockPosition();
        if (!isSupportedDimension(player.level().dimension())) {
            return new BindResult(Failure.UNSUPPORTED_DIMENSION);
        }
        if (hasBossNearby(player.serverLevel(), position)) {
            return new BindResult(Failure.BOSS_NEARBY);
        }
        if (safePosition(player.serverLevel(), player, player.getBoundingBox(), position) == null) {
            return new BindResult(Failure.UNSAFE_POSITION);
        }
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), true);
        anchors.removeIf(anchor -> anchor.expiry <= ArtifactManager.gameTime(player));
        boolean twin = ArtifactManager.getData(player).getLevel(type) >= 2
                && "twin_spike".equals(ArtifactManager.getData(player).getTuning(type));
        anchors = mergeAnchor(anchors, new Anchor(player.level().dimension(), position.immutable(), expiry), twin);
        ArtifactManager.getData(player).setBinding(type, encodeBoundAnchors(anchors));
        ArtifactManager.syncToClient(player);
        return new BindResult(Failure.NONE);
    }

    static boolean hasRiftBinding(ServerPlayer player) {
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(ArtifactType.RIFT_ANCHOR), true);
        long now = ArtifactManager.gameTime(player);
        anchors.removeIf(anchor -> anchor.expiry <= now);
        return !anchors.isEmpty();
    }

    /** 低频清理过期坐标，避免玩家长期不使用时数据残留。 */
    static void tickRiftAnchor(ServerPlayer player) {
        if (player.tickCount % 40 != 0) {
            return;
        }
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        String raw = ArtifactManager.getData(player).getBinding(type);
        if (raw == null || raw.isBlank()) {
            return;
        }
        List<Anchor> anchors = boundAnchors(raw, true);
        long now = ArtifactManager.gameTime(player);
        anchors.removeIf(anchor -> anchor.expiry <= now);
        if (!raw.equals(encodeBoundAnchors(anchors))) {
            persistBinding(player, type, anchors);
        }
    }

    static ResolveResult resolveDimensionAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.DIMENSION_ANCHOR;
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), false);
        boolean changed = false;
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
            prepareChunks(targetLevel, anchor.pos, SAFE_SEARCH_RADIUS);
            if (!isValidDimensionAnchor(player, targetLevel, anchor.pos)) {
                anchors.remove(index);
                changed = true;
                failure = Failure.INVALID_ANCHOR;
                continue;
            }
            ResolveResult result = resolvePrepared(player, targetLevel, anchor);
            if (result.destination != null) {
                if (changed) {
                    persistBinding(player, type, anchors);
                }
                return result;
            }
            failure = result.failure;
            index++;
        }
        if (changed) {
            persistBinding(player, type, anchors);
        }
        return new ResolveResult(null, failure);
    }

    static ResolveResult resolveRiftAnchor(ServerPlayer player) {
        ArtifactType type = ArtifactType.RIFT_ANCHOR;
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), true);
        long now = ArtifactManager.gameTime(player);
        int beforePrune = anchors.size();
        anchors.removeIf(anchor -> anchor.expiry <= now);
        boolean changed = anchors.size() != beforePrune;
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
            prepareChunks(targetLevel, anchor.pos, SAFE_SEARCH_RADIUS);
            ResolveResult result = resolvePrepared(player, targetLevel, anchor);
            if (result.destination != null) {
                if (changed) {
                    persistBinding(player, type, anchors);
                }
                return result;
            }
            failure = result.failure;
            index++;
        }
        if (changed) {
            persistBinding(player, type, anchors);
        }
        return new ResolveResult(null, failure);
    }

    static void teleport(ServerPlayer player, Destination destination) {
        Vec3 target = destination.target;
        player.teleportTo(destination.level, target.x, target.y, target.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    /** 只在传送成功后提交双锚轮换，避免失败请求改变下次目的地。 */
    static void commitUse(ServerPlayer player, ArtifactType type, Destination destination) {
        List<Anchor> anchors = boundAnchors(ArtifactManager.getData(player).getBinding(type), type == ArtifactType.RIFT_ANCHOR);
        for (int index = 0; index < anchors.size(); index++) {
            Anchor anchor = anchors.get(index);
            if (anchor.dimension.equals(destination.anchorDimension)
                    && anchor.pos.equals(destination.anchorPos)
                    && anchor.expiry == destination.expiry) {
                rotateAfterUse(player, type, anchors, index);
                return;
            }
        }
    }

    private static ResolveResult resolvePrepared(ServerPlayer player, ServerLevel targetLevel, Anchor anchor) {
        if (hasBossNearby(player.serverLevel(), player.blockPosition())
                || hasBossNearby(targetLevel, anchor.pos)) {
            return new ResolveResult(null, Failure.BOSS_NEARBY);
        }
        Vec3 safe = findSafeNear(targetLevel, player, anchor.pos);
        if (safe == null) {
            return new ResolveResult(null, Failure.UNSAFE_POSITION);
        }
        return new ResolveResult(new Destination(targetLevel, safe, anchor.expiry,
                anchor.dimension, anchor.pos), Failure.NONE);
    }

    private static boolean isValidDimensionAnchor(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isActivatedForPlayer(player, level, pos, state);
    }

    private static boolean isActivatedForPlayer(ServerPlayer player, ServerLevel level,
                                                BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.RESONANCE_BEACON.get())) {
            return EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID);
        }
        if (!state.is(ModBlocks.ABYSS_BEACON.get())) {
            return false;
        }
        String prefix = "abyss_beacon:" + level.dimension().location() + ":"
                + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
        return EchoAbilityManager.hasActivatedMechanism(player, prefix + ":complete");
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
    private static Vec3 findSafeNear(ServerLevel level, ServerPlayer player, BlockPos anchor) {
        AABB original = player.getBoundingBox();
        Vec3 exact = safePosition(level, player, original, anchor);
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
                        BlockPos feet = anchor.offset(dx, dy, dz);
                        Vec3 target = safePosition(level, player, original, feet);
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
    private static Vec3 safePosition(ServerLevel level, ServerPlayer player, AABB original, BlockPos feet) {
        Vec3 target = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
        AABB moved = original.move(target.subtract(player.position()));
        return level.getWorldBorder().isWithinBounds(feet)
                && level.noCollision(player, moved)
                && Block.canSupportCenter(level, feet.below(), Direction.UP) ? target : null;
    }

    static boolean isDestinationSafe(ServerPlayer player, Destination destination) {
        if (hasBossNearby(player.serverLevel(), player.blockPosition())
                || hasBossNearby(destination.level, destination.anchorPos)) {
            return false;
        }
        AABB original = player.getBoundingBox();
        BlockPos feet = BlockPos.containing(destination.target);
        return safePosition(destination.level, player, original, feet) != null;
    }

    private static boolean hasBossNearby(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BOSS_LOCK_RANGE);
        return !level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity.getType().is(ModEntityTags.ECHO_BOSSES)).isEmpty();
    }

    private static void prepareChunks(ServerLevel level, BlockPos center, int radiusBlocks) {
        int minChunkX = (center.getX() - radiusBlocks) >> 4;
        int maxChunkX = (center.getX() + radiusBlocks) >> 4;
        int minChunkZ = (center.getZ() - radiusBlocks) >> 4;
        int maxChunkZ = (center.getZ() + radiusBlocks) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                level.getChunk(x, z);
            }
        }
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
            anchors.remove(0);
        }
        return anchors;
    }

    private static void rotateAfterUse(ServerPlayer player, ArtifactType type,
                                       List<Anchor> anchors, int index) {
        if (anchors.size() <= 1 || index < 0 || index >= anchors.size()) {
            return;
        }
        Anchor used = anchors.remove(index);
        anchors.add(used);
        ArtifactManager.getData(player).setBinding(type, encodeBoundAnchors(anchors));
        ArtifactManager.syncToClient(player);
    }

    private static void persistBinding(ServerPlayer player, ArtifactType type, List<Anchor> anchors) {
        ArtifactManager.getData(player).setBinding(type, encodeBoundAnchors(anchors));
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
}
