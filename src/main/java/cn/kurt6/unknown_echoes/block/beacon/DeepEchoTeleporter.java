package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.config.CommonConfig;
import cn.kurt6.unknown_echoes.block.deep.DeepRouteNodeBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** 深层回响的安全落点、入口锚记录与延迟跨维度传送。 */
public final class DeepEchoTeleporter {
    public static final ResourceKey<Level> DEEP_ECHOES = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, UnknownEchoes.id("deep_echoes"));
    public static final BlockPos DEEP_ORIGIN = BlockPos.ZERO;

    private static final String RETURN_ANCHOR_PREFIX = "abyss_return_anchor:";

    private DeepEchoTeleporter() {
    }

    public static void enter(ServerPlayer player, BlockPos sourceBeacon) {
        if (!CommonConfig.ENABLE_DEEP_ECHOES.get()) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_echoes.disabled"), true);
            return;
        }
        if (!EchoPermission.canEnterDeepEchoes(player)) {
            return;
        }
        ServerLevel destination = player.server.getLevel(DEEP_ECHOES);
        if (destination == null) {
            UnknownEchoes.LOGGER.error("Deep Echoes dimension is missing: {}", DEEP_ECHOES.location());
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_echoes.unavailable"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.ABYSS_BEACON.get())) {
            return;
        }

        player.getCooldowns().addCooldown(ModItems.ABYSS_BEACON.get(), 20);
        BlockPos source = sourceBeacon.immutable();
        ServerLevel sourceLevel = player.serverLevel();
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (!isFormalEntryCurrent(player, sourceLevel, source)) {
                return;
            }
            prepareChunks(destination, DEEP_ORIGIN, 6);
            BlockPos arrival = resolveArrival(destination);
            if (!ensureArrivalHall(destination, arrival)) {
                UnknownEchoes.LOGGER.warn("Deep Echoes arrival area is obstructed near {}", arrival);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.arrival_blocked"), true);
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (!isFormalEntryCurrent(player, sourceLevel, source)
                        || !isArrivalHallReady(destination, arrival, arrival.below())
                        || !isSafe(destination, arrival, player)) {
                    return;
                }
                if (!rememberReturnAnchor(player, source)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"), true);
                    return;
                }
                player.teleportTo(destination, arrival.getX() + 0.5, arrival.getY(),
                        arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
                if (player.serverLevel() != destination) {
                    return;
                }
                ModAdvancements.recordDeepEchoesEntry(player);
                destination.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL,
                        SoundSource.PLAYERS, 0.6F, 0.75F);
            }));
        }));
    }

    /** 管理员测试入口：只复用区块准备与安全落点，不写入任何正式进度。 */
    public static boolean teleportForTesting(ServerPlayer player) {
        ServerLevel destination = player.server.getLevel(DEEP_ECHOES);
        if (destination == null) {
            UnknownEchoes.LOGGER.error("Deep Echoes dimension is missing: {}", DEEP_ECHOES.location());
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_echoes.unavailable"), true);
            return false;
        }

        ServerLevel sourceLevel = player.serverLevel();
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (player.isRemoved() || !player.isAlive() || player.level() != sourceLevel) {
                return;
            }
            prepareChunks(destination, DEEP_ORIGIN, 6);
            BlockPos arrival = resolveArrival(destination);
            if (!ensureArrivalHall(destination, arrival)) {
                UnknownEchoes.LOGGER.warn("Deep Echoes test arrival area is obstructed near {}", arrival);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.arrival_blocked"), true);
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (player.isRemoved() || !player.isAlive() || player.level() != sourceLevel
                        || !isSafe(destination, arrival, player)) {
                    return;
                }
                player.teleportTo(destination, arrival.getX() + 0.5D, arrival.getY(),
                        arrival.getZ() + 0.5D, player.getYRot(), player.getXRot());
                if (player.serverLevel() != destination) {
                    return;
                }
                destination.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL,
                        SoundSource.PLAYERS, 0.6F, 0.75F);
            }));
        }));
        return true;
    }

    public static void returnToEchoRealm(ServerPlayer player, BlockPos sourceBeacon) {
        ServerLevel echoRealm = player.server.getLevel(EchoBeaconBlock.ECHO_REALM);
        if (echoRealm == null) {
            UnknownEchoes.LOGGER.error("Echo Realm dimension is missing: {}", EchoBeaconBlock.ECHO_REALM.location());
            EchoBeaconBlock.teleportBackFromAbyssBeacon(player, sourceBeacon);
            return;
        }
        BlockPos anchor = readReturnAnchor(player, echoRealm);
        if (anchor == null) {
            EchoBeaconBlock.teleportBackFromAbyssBeacon(player, sourceBeacon);
            return;
        }

        ServerLevel sourceLevel = player.serverLevel();
        BlockPos source = sourceBeacon.immutable();
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (!isReturnSourceCurrent(player, sourceLevel, source)
                    || !anchor.equals(readReturnAnchor(player, echoRealm))) {
                return;
            }
            prepareChunks(echoRealm, anchor, 5);
            BlockPos arrival = findSafeNear(echoRealm, anchor, player);
            if (arrival == null) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.return_blocked"), true);
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (!isReturnSourceCurrent(player, sourceLevel, source)
                        || !anchor.equals(readReturnAnchor(player, echoRealm))) {
                    return;
                }
                if (!isSafe(echoRealm, arrival, player)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.deep_echoes.return_blocked"), true);
                    return;
                }
                player.teleportTo(echoRealm, arrival.getX() + 0.5, arrival.getY(),
                        arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
                if (player.serverLevel() != echoRealm) {
                    return;
                }
            }));
        }));
    }

    private static boolean isReturnSourceCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                 BlockPos sourceBeacon) {
        return !player.isRemoved() && player.isAlive() && player.level() == sourceLevel
                && sourceLevel.hasChunkAt(sourceBeacon)
                && sourceLevel.getBlockState(sourceBeacon).is(ModBlocks.ABYSS_BEACON.get());
    }

    private static boolean isFormalEntryCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                BlockPos sourceBeacon) {
        return !player.isRemoved()
                && player.isAlive()
                && player.level() == sourceLevel
                && CommonConfig.ENABLE_DEEP_ECHOES.get()
                && EchoPermission.canEnterDeepEchoes(player)
                && AbyssBeaconBlock.isCalibratedFor(player, sourceLevel, sourceBeacon);
    }

    private static BlockPos resolveArrival(ServerLevel level) {
        BlockPos existing = findExistingArrival(level);
        if (existing != null) {
            return existing;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DEEP_ORIGIN.getX(), DEEP_ORIGIN.getZ());
        int minY = level.getMinBuildHeight() + 2;
        int maxY = level.getMaxBuildHeight() - 8;
        return new BlockPos(DEEP_ORIGIN.getX(), Math.clamp(surfaceY, minY, maxY), DEEP_ORIGIN.getZ());
    }

    private static BlockPos findExistingArrival(ServerLevel level) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 9;
        for (int floorY = maxY; floorY >= minY; floorY--) {
            BlockPos floorCenter = new BlockPos(DEEP_ORIGIN.getX(), floorY, DEEP_ORIGIN.getZ());
            if (isHallFloor(level.getBlockState(floorCenter))
                    && level.getBlockState(floorCenter.offset(5, 1, 5))
                    .is(ModBlocks.ABYSS_BEACON.get())
                    && isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.WIND, 0, -5)
                    && isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.TIDE, -5, 0)
                    && isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.TRUE_SIGHT, 5, 0)) {
                return floorCenter.above();
            }
        }
        return null;
    }

    private static boolean ensureArrivalHall(ServerLevel level, BlockPos arrival) {
        BlockPos floorCenter = arrival.below();
        if (isArrivalHallReady(level, arrival, floorCenter)) {
            return true;
        }
        boolean initialized = isHallFloor(level.getBlockState(floorCenter));
        if (initialized) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockState floor = level.getBlockState(floorCenter.offset(x, 0, z));
                    if (!isHallFloor(floor) && !floor.canBeReplaced()) {
                        return false;
                    }
                    for (int y = 1; y <= 4; y++) {
                        BlockState state = level.getBlockState(floorCenter.offset(x, y, z));
                        if (!state.canBeReplaced() && !isHallFixture(state)) {
                            return false;
                        }
                    }
                }
            }
        }

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                BlockPos floor = floorCenter.offset(x, 0, z);
                level.setBlock(floor, (x == 0 || z == 0 || (Math.abs(x) == 6 && Math.abs(z) == 6)
                        ? ModBlocks.ECHO_RUNE_BRICKS.get()
                        : ModBlocks.ECHO_STONE_BRICKS.get()).defaultBlockState(), 3);
                for (int y = 1; y <= 4; y++) {
                    BlockPos clear = floorCenter.offset(x, y, z);
                    if (!level.getBlockState(clear).isAir() && !isHallFixture(level.getBlockState(clear))) {
                        level.setBlock(clear, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        buildRouteArm(level, floorCenter, DeepRouteNodeBlock.Route.WIND, 0, -5);
        buildRouteArm(level, floorCenter, DeepRouteNodeBlock.Route.TIDE, -5, 0);
        buildRouteArm(level, floorCenter, DeepRouteNodeBlock.Route.TRUE_SIGHT, 5, 0);
        for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}}) {
            for (int y = 1; y <= 3; y++) {
                level.setBlock(floorCenter.offset(corner[0], y, corner[1]),
                        (y == 3 ? ModBlocks.ECHO_RUNE_BRICKS.get() : ModBlocks.ECHO_CLIFF_STONE.get())
                                .defaultBlockState(), 3);
            }
        }
        BlockPos legacyBeacon = floorCenter.offset(3, 1, 0);
        if (level.getBlockState(legacyBeacon).is(ModBlocks.ABYSS_BEACON.get())) {
            level.setBlock(legacyBeacon, Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(floorCenter.offset(5, 1, 5),
                ModBlocks.ABYSS_BEACON.get().defaultBlockState(), 3);
        return isSafe(level, arrival, null);
    }

    private static void buildRouteArm(ServerLevel level, BlockPos floorCenter,
                                      DeepRouteNodeBlock.Route route, int nodeX, int nodeZ) {
        BlockState floor = routeFloor(route);
        int stepX = Integer.signum(nodeX);
        int stepZ = Integer.signum(nodeZ);
        for (int step = 1; step <= 5; step++) {
            int centerX = stepX * step;
            int centerZ = stepZ * step;
            for (int offset = -1; offset <= 1; offset++) {
                int x = centerX + (stepZ == 0 ? 0 : offset);
                int z = centerZ + (stepX == 0 ? 0 : offset);
                level.setBlock(floorCenter.offset(x, 0, z), floor, 3);
            }
        }
        level.setBlock(floorCenter.offset(nodeX, 1, nodeZ),
                ModBlocks.DEEP_ROUTE_NODE.get().defaultBlockState()
                        .setValue(DeepRouteNodeBlock.ROUTE, route), 3);
    }

    private static boolean isArrivalHallReady(ServerLevel level, BlockPos arrival, BlockPos floorCenter) {
        if (!isHallFloor(level.getBlockState(floorCenter)) || !isSafe(level, arrival, null)
                || !level.getBlockState(floorCenter.offset(5, 1, 5)).is(ModBlocks.ABYSS_BEACON.get())) {
            return false;
        }
        if (!isRouteArmReady(level, floorCenter, DeepRouteNodeBlock.Route.WIND, 0, -5)
                || !isRouteArmReady(level, floorCenter, DeepRouteNodeBlock.Route.TIDE, -5, 0)
                || !isRouteArmReady(level, floorCenter, DeepRouteNodeBlock.Route.TRUE_SIGHT, 5, 0)) {
            return false;
        }
        for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}}) {
            for (int y = 1; y <= 3; y++) {
                BlockState expected = (y == 3 ? ModBlocks.ECHO_RUNE_BRICKS.get()
                        : ModBlocks.ECHO_CLIFF_STONE.get()).defaultBlockState();
                if (!level.getBlockState(floorCenter.offset(corner[0], y, corner[1])).is(expected.getBlock())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isRouteArmReady(ServerLevel level, BlockPos floorCenter,
                                           DeepRouteNodeBlock.Route route, int nodeX, int nodeZ) {
        BlockState expectedFloor = routeFloor(route);
        int stepX = Integer.signum(nodeX);
        int stepZ = Integer.signum(nodeZ);
        for (int step = 1; step <= 5; step++) {
            int centerX = stepX * step;
            int centerZ = stepZ * step;
            for (int offset = -1; offset <= 1; offset++) {
                int x = centerX + (stepZ == 0 ? 0 : offset);
                int z = centerZ + (stepX == 0 ? 0 : offset);
                if (!level.getBlockState(floorCenter.offset(x, 0, z)).is(expectedFloor.getBlock())) {
                    return false;
                }
            }
        }
        return isRouteNodeReady(level, floorCenter, route, nodeX, nodeZ);
    }

    private static boolean isRouteNodeReady(ServerLevel level, BlockPos floorCenter,
                                            DeepRouteNodeBlock.Route route, int nodeX, int nodeZ) {
        BlockState node = level.getBlockState(floorCenter.offset(nodeX, 1, nodeZ));
        return node.is(ModBlocks.DEEP_ROUTE_NODE.get())
                && node.getValue(DeepRouteNodeBlock.ROUTE) == route;
    }

    private static BlockState routeFloor(DeepRouteNodeBlock.Route route) {
        return switch (route) {
            case WIND -> ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            case TIDE -> ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState();
            case TRUE_SIGHT -> ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
        };
    }

    private static boolean isHallFloor(BlockState state) {
        return state.is(ModBlocks.ECHO_STONE_BRICKS.get())
                || state.is(ModBlocks.ECHO_RUNE_BRICKS.get())
                || state.is(ModBlocks.WIND_ETCHED_STONE.get())
                || state.is(ModBlocks.TIDE_SMOOTH_STONE.get())
                || state.is(ModBlocks.MIRROR_STONE_BRICKS.get());
    }

    private static boolean isHallFixture(BlockState state) {
        return state.is(ModBlocks.ABYSS_BEACON.get())
                || state.is(ModBlocks.DEEP_ROUTE_NODE.get())
                || state.is(ModBlocks.ECHO_CLIFF_STONE.get())
                || state.is(ModBlocks.ECHO_RUNE_BRICKS.get());
    }

    private static BlockPos findSafeNear(ServerLevel level, BlockPos beacon, ServerPlayer player) {
        for (int radius = 2; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos candidate = beacon.offset(x, -1, z);
                    if (isSafe(level, candidate, player)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
        AABB box;
        if (player == null) {

            box = new AABB(pos.getX() + 0.2D, pos.getY(), pos.getZ() + 0.2D,
                    pos.getX() + 0.8D, pos.getY() + 1.8D, pos.getZ() + 0.8D);
        } else {
            Vec3 target = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            box = player.getBoundingBox().move(target.subtract(player.position()));
        }
        return level.getWorldBorder().isWithinBounds(box)
                && level.noCollision(player, box)
                && Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    private static void prepareChunks(ServerLevel level, BlockPos center, int radius) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static boolean rememberReturnAnchor(ServerPlayer player, BlockPos source) {
        var mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        mechanisms.removeIf(key -> key.startsWith(RETURN_ANCHOR_PREFIX));
        return EchoAbilityManager.activateMechanism(player, RETURN_ANCHOR_PREFIX
                + source.getX() + "," + source.getY() + "," + source.getZ());
    }

    private static BlockPos readReturnAnchor(ServerPlayer player, ServerLevel targetLevel) {
        for (String key : EchoAbilityManager.getData(player).getActivatedMechanisms()) {
            if (!key.startsWith(RETURN_ANCHOR_PREFIX)) {
                continue;
            }
            String[] parts = key.substring(RETURN_ANCHOR_PREFIX.length()).split(",", -1);
            if (parts.length != 3) {
                continue;
            }
            try {
                BlockPos anchor = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
                if (anchor.getY() >= targetLevel.getMinBuildHeight()
                        && anchor.getY() < targetLevel.getMaxBuildHeight()
                        && targetLevel.getWorldBorder().isWithinBounds(anchor)) {
                    return anchor;
                }
            } catch (NumberFormatException ignored) {

            }
        }
        return null;
    }
}
