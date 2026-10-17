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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** 深层回响的安全落点、入口锚记录与延迟跨维度传送。 */
public final class DeepEchoTeleporter {
    public static final ResourceKey<Level> DEEP_ECHOES = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, UnknownEchoes.id("deep_echoes"));
    public static final BlockPos DEEP_ORIGIN = BlockPos.ZERO;

    private static final String RETURN_ANCHOR_PREFIX = "abyss_return_anchor:";

    private DeepEchoTeleporter() {
    }

    public static void enter(ServerPlayer player, BlockPos sourceBeacon) {
        BlockPos source = sourceBeacon.immutable();
        ServerLevel sourceLevel = player.serverLevel();
        if (!isPlayerCurrent(player, sourceLevel) || player.isSpectator()) {
            return;
        }
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

        BooleanSupplier canContinue = () -> isFormalEntryCurrent(player, sourceLevel, source)
                && player.server.getLevel(DEEP_ECHOES) == destination;
        if (!canContinue.getAsBoolean()) {
            return;
        }
        player.getCooldowns().addCooldown(ModItems.ABYSS_BEACON.get(), 20);
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!prepareChunks(destination, DEEP_ORIGIN, 6, canContinue)) {
                return;
            }
            BlockPos arrival = resolveArrival(destination);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            boolean hallReady = ensureArrivalHall(destination, arrival);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!hallReady) {
                UnknownEchoes.LOGGER.warn("Deep Echoes arrival area is obstructed near {}", arrival);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.arrival_blocked"), true);
                return;
            }
            if (!prepareLandingChunks(destination, arrival, player, 0, canContinue)) {
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (!isEntryArrivalCurrent(destination, arrival, player, canContinue)) {
                    return;
                }
                if (!rememberReturnAnchor(player, source)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"), true);
                    return;
                }
                if (!isEntryArrivalCurrent(destination, arrival, player, canContinue)
                        || !source.equals(readReturnAnchor(player, sourceLevel))) {
                    return;
                }
                player.teleportTo(destination, arrival.getX() + 0.5, arrival.getY(),
                        arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
                if (!isAtArrival(player, destination, arrival)) {
                    return;
                }
                if (player.isSpectator()) {
                    return;
                }
                ModAdvancements.recordDeepEchoesEntry(player);
                if (!isAtArrival(player, destination, arrival) || player.isSpectator()) {
                    return;
                }
                destination.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL,
                        SoundSource.PLAYERS, 0.6F, 0.75F);
            }));
        }));
    }

    /** 管理员测试入口：只复用区块准备与安全落点，不写入任何正式进度。 */
    public static boolean teleportForTesting(ServerPlayer player) {
        ServerLevel sourceLevel = player.serverLevel();
        if (!isPlayerCurrent(player, sourceLevel)) {
            return false;
        }
        ServerLevel destination = player.server.getLevel(DEEP_ECHOES);
        if (destination == null) {
            UnknownEchoes.LOGGER.error("Deep Echoes dimension is missing: {}", DEEP_ECHOES.location());
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_echoes.unavailable"), true);
            return false;
        }

        BooleanSupplier canContinue = () -> isPlayerCurrent(player, sourceLevel)
                && player.server.getLevel(DEEP_ECHOES) == destination;
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!prepareChunks(destination, DEEP_ORIGIN, 6, canContinue)) {
                return;
            }
            BlockPos arrival = resolveArrival(destination);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            boolean hallReady = ensureArrivalHall(destination, arrival);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!hallReady) {
                UnknownEchoes.LOGGER.warn("Deep Echoes test arrival area is obstructed near {}", arrival);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.arrival_blocked"), true);
                return;
            }
            if (!prepareLandingChunks(destination, arrival, player, 0, canContinue)) {
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (!isEntryArrivalCurrent(destination, arrival, player, canContinue)) {
                    return;
                }
                player.teleportTo(destination, arrival.getX() + 0.5D, arrival.getY(),
                        arrival.getZ() + 0.5D, player.getYRot(), player.getXRot());
                if (!isAtArrival(player, destination, arrival)) {
                    return;
                }
                destination.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL,
                        SoundSource.PLAYERS, 0.6F, 0.75F);
            }));
        }));
        return true;
    }

    public static void returnToEchoRealm(ServerPlayer player, BlockPos sourceBeacon) {
        ServerLevel sourceLevel = player.serverLevel();
        BlockPos source = sourceBeacon.immutable();
        if (!isReturnSourceCurrent(player, sourceLevel, source)) {
            return;
        }
        ServerLevel echoRealm = player.server.getLevel(EchoBeaconBlock.ECHO_REALM);
        if (echoRealm == null) {
            UnknownEchoes.LOGGER.error("Echo Realm dimension is missing: {}", EchoBeaconBlock.ECHO_REALM.location());
            EchoBeaconBlock.teleportBackFromAbyssBeacon(player, source);
            return;
        }
        BlockPos anchor = readReturnAnchor(player, echoRealm);
        if (anchor == null) {
            EchoBeaconBlock.teleportBackFromAbyssBeacon(player, source);
            return;
        }

        BooleanSupplier canContinue = () -> isReturnSourceCurrent(player, sourceLevel, source)
                && player.server.getLevel(EchoBeaconBlock.ECHO_REALM) == echoRealm
                && anchor.equals(readReturnAnchor(player, echoRealm));
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!prepareLandingChunks(echoRealm, anchor, player, 5, canContinue)) {
                return;
            }
            BlockPos arrival = findSafeNear(echoRealm, anchor, player, canContinue);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (arrival == null) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.deep_echoes.return_blocked"), true);
                return;
            }
            player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                if (!canContinue.getAsBoolean()) {
                    return;
                }
                if (!isSafe(echoRealm, arrival, player)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.deep_echoes.return_blocked"), true);
                    return;
                }
                if (!canContinue.getAsBoolean()) {
                    return;
                }
                player.teleportTo(echoRealm, arrival.getX() + 0.5, arrival.getY(),
                        arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
            }));
        }));
    }

    private static boolean isReturnSourceCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                 BlockPos sourceBeacon) {
        return isPlayerCurrent(player, sourceLevel) && !player.isSpectator()
                && sourceLevel.dimension().equals(DEEP_ECHOES)
                && player.canInteractWithBlock(sourceBeacon, 1.0D)
                && AbyssBeaconBlock.isBeaconPresent(sourceLevel, sourceBeacon);
    }

    private static boolean isFormalEntryCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                BlockPos sourceBeacon) {
        return isPlayerCurrent(player, sourceLevel)
                && !player.isSpectator()
                && player.canInteractWithBlock(sourceBeacon, 1.0D)
                && CommonConfig.ENABLE_DEEP_ECHOES.get()
                && EchoPermission.canEnterDeepEchoes(player)
                && AbyssBeaconBlock.isCalibratedFor(player, sourceLevel, sourceBeacon);
    }

    private static boolean isPlayerCurrent(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.isAlive() && !player.hasDisconnected()
                && player.level() == level
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.server.getLevel(level.dimension()) == level;
    }

    private static boolean isAtArrival(ServerPlayer player, ServerLevel level, BlockPos arrival) {
        return isPlayerCurrent(player, level)
                && player.position().distanceToSqr(arrival.getX() + 0.5D, arrival.getY(),
                arrival.getZ() + 0.5D) <= 0.01D;
    }

    private static boolean isEntryArrivalCurrent(ServerLevel level, BlockPos arrival,
                                                  ServerPlayer player, BooleanSupplier canContinue) {
        return canContinue.getAsBoolean()
                && hasReadableChunks(level, DEEP_ORIGIN.getX() - 6, DEEP_ORIGIN.getZ() - 6,
                DEEP_ORIGIN.getX() + 6, DEEP_ORIGIN.getZ() + 6)
                && isArrivalHallReady(level, arrival, arrival.below())
                && isSafe(level, arrival, player)
                && canContinue.getAsBoolean();
    }

    private static BlockPos resolveArrival(ServerLevel level) {
        DeepArrivalData data = DeepArrivalData.get(level);
        if (data.arrivalY().isPresent()) {
            return new BlockPos(DEEP_ORIGIN.getX(), data.arrivalY().getAsInt(), DEEP_ORIGIN.getZ());
        }
        BlockPos existing = findExistingArrival(level);
        if (existing != null) {
            data.rememberArrival(existing.getY());
            return existing;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DEEP_ORIGIN.getX(), DEEP_ORIGIN.getZ());
        int minY = level.getMinBuildHeight() + 2;
        int maxY = level.getMaxBuildHeight() - 8;
        return new BlockPos(DEEP_ORIGIN.getX(), Math.clamp(surfaceY, minY, maxY), DEEP_ORIGIN.getZ());
    }

    private static BlockPos findExistingArrival(ServerLevel level) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 5;
        for (int floorY = minY; floorY <= maxY; floorY++) {
            BlockPos floorCenter = new BlockPos(DEEP_ORIGIN.getX(), floorY, DEEP_ORIGIN.getZ());
            int corners = 0;
            for (int x : new int[]{-6, 6}) {
                for (int z : new int[]{-6, 6}) {
                    if (level.getBlockState(floorCenter.offset(x, 0, z)).is(ModBlocks.ECHO_RUNE_BRICKS.get())) {
                        corners++;
                    }
                }
            }
            int fixtures = 0;
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}}) {
                if (level.getBlockState(floorCenter.offset(corner[0], 3, corner[1]))
                        .is(ModBlocks.ECHO_RUNE_BRICKS.get())) {
                    fixtures++;
                }
            }
            if (level.getBlockState(floorCenter.offset(5, 1, 5)).is(ModBlocks.ABYSS_BEACON.get())
                    || level.getBlockState(floorCenter.offset(3, 1, 0)).is(ModBlocks.ABYSS_BEACON.get())) {
                fixtures++;
            }
            if (isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.WIND, 0, -5)) fixtures++;
            if (isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.TIDE, -5, 0)) fixtures++;
            if (isRouteNodeReady(level, floorCenter, DeepRouteNodeBlock.Route.TRUE_SIGHT, 5, 0)) fixtures++;
            if (corners < 3 && fixtures < 2) {
                continue;
            }
            int floorBlocks = 0;
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    if (isHallFloor(level.getBlockState(floorCenter.offset(x, 0, z)))) floorBlocks++;
                }
            }
            if (floorBlocks >= 100) {
                return floorCenter.above();
            }
        }
        return null;
    }

    private static boolean ensureArrivalHall(ServerLevel level, BlockPos arrival) {
        if (!isArrivalAreaWithinBounds(level, arrival)) {
            return false;
        }
        DeepArrivalData data = DeepArrivalData.get(level);
        boolean initialized = data.arrivalY().isPresent();
        if (initialized && data.arrivalY().getAsInt() != arrival.getY()) {
            return false;
        }
        BlockPos floorCenter = arrival.below();
        if (isArrivalHallReady(level, arrival, floorCenter)) {
            data.rememberArrival(arrival.getY());
            return true;
        }
        Map<BlockPos, BlockState> layout = arrivalHallLayout(floorCenter);
        if (initialized) {
            for (var entry : layout.entrySet()) {
                if (!canRepairHallBlock(level.getBlockState(entry.getKey()), entry.getValue(),
                        entry.getKey(), floorCenter)) {
                    return false;
                }
            }
        }
        // 必须先固定位置，部分写入失败后仍在原址重试。
        data.rememberArrival(arrival.getY());
        for (var entry : layout.entrySet()) {
            if (!level.getBlockState(entry.getKey()).equals(entry.getValue())
                    && !level.setBlock(entry.getKey(), entry.getValue(), 3)) {
                return false;
            }
        }
        return isArrivalHallReady(level, arrival, floorCenter);
    }

    private static boolean isArrivalAreaWithinBounds(ServerLevel level, BlockPos arrival) {
        return arrival.getX() == DEEP_ORIGIN.getX() && arrival.getZ() == DEEP_ORIGIN.getZ()
                && arrival.getY() > level.getMinBuildHeight()
                && arrival.getY() <= level.getMaxBuildHeight() - 4
                && level.getWorldBorder().isWithinBounds(new AABB(
                arrival.getX() - 6, arrival.getY() - 1, arrival.getZ() - 6,
                arrival.getX() + 7, arrival.getY() + 4, arrival.getZ() + 7));
    }

    private static boolean canRepairHallBlock(BlockState state, BlockState expected,
                                              BlockPos pos, BlockPos floorCenter) {
        return state.equals(expected) || state.canBeReplaced()
                || pos.getY() == floorCenter.getY() && isHallFloor(state)
                || expected.is(ModBlocks.DEEP_ROUTE_NODE.get()) && state.is(ModBlocks.DEEP_ROUTE_NODE.get())
                || pos.equals(floorCenter.offset(3, 1, 0)) && state.is(ModBlocks.ABYSS_BEACON.get());
    }

    private static Map<BlockPos, BlockState> arrivalHallLayout(BlockPos floorCenter) {
        Map<BlockPos, BlockState> layout = new LinkedHashMap<>();
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                BlockPos floor = floorCenter.offset(x, 0, z);
                layout.put(floor, (x == 0 || z == 0 || (Math.abs(x) == 6 && Math.abs(z) == 6)
                        ? ModBlocks.ECHO_RUNE_BRICKS.get()
                        : ModBlocks.ECHO_STONE_BRICKS.get()).defaultBlockState());
                for (int y = 1; y <= 4; y++) {
                    layout.put(floorCenter.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        buildRouteArm(layout, floorCenter, DeepRouteNodeBlock.Route.WIND, 0, -5);
        buildRouteArm(layout, floorCenter, DeepRouteNodeBlock.Route.TIDE, -5, 0);
        buildRouteArm(layout, floorCenter, DeepRouteNodeBlock.Route.TRUE_SIGHT, 5, 0);
        for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}}) {
            for (int y = 1; y <= 3; y++) {
                layout.put(floorCenter.offset(corner[0], y, corner[1]),
                        (y == 3 ? ModBlocks.ECHO_RUNE_BRICKS.get() : ModBlocks.ECHO_CLIFF_STONE.get())
                                .defaultBlockState());
            }
        }
        layout.put(floorCenter.offset(5, 1, 5), ModBlocks.ABYSS_BEACON.get().defaultBlockState());
        return layout;
    }

    private static void buildRouteArm(Map<BlockPos, BlockState> layout, BlockPos floorCenter,
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
                layout.put(floorCenter.offset(x, 0, z), floor);
            }
        }
        layout.put(floorCenter.offset(nodeX, 1, nodeZ),
                ModBlocks.DEEP_ROUTE_NODE.get().defaultBlockState()
                        .setValue(DeepRouteNodeBlock.ROUTE, route));
    }

    private static boolean isArrivalHallReady(ServerLevel level, BlockPos arrival, BlockPos floorCenter) {
        if (!isArrivalAreaWithinBounds(level, arrival) || !isSafe(level, arrival, null)) {
            return false;
        }
        for (var entry : arrivalHallLayout(floorCenter).entrySet()) {
            if (!entry.getValue().isAir() && !level.getBlockState(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
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

    private static BlockPos findSafeNear(ServerLevel level, BlockPos beacon, ServerPlayer player,
                                          BooleanSupplier canContinue) {
        for (int radius = 2; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    if (!canContinue.getAsBoolean()) {
                        return null;
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
        return isFinite(box) && box.minY >= level.getMinBuildHeight() + 1
                && box.maxY <= level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(box)
                && hasReadableChunks(level, Mth.floor(box.minX - 1.0e-7D) - 1,
                Mth.floor(box.minZ - 1.0e-7D) - 1, Mth.floor(box.maxX + 1.0e-7D) + 1,
                Mth.floor(box.maxZ + 1.0e-7D) + 1)
                && level.noCollision(player, box)
                && Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    private static boolean prepareChunks(ServerLevel level, BlockPos center, int radius,
                                          BooleanSupplier canContinue) {
        return prepareChunkRange(level, center.getX() - radius, center.getZ() - radius,
                center.getX() + radius, center.getZ() + radius, canContinue);
    }

    private static boolean prepareLandingChunks(ServerLevel level, BlockPos center, ServerPlayer player,
                                                  int radius, BooleanSupplier canContinue) {
        AABB box = player.getBoundingBox().move(new Vec3(center.getX() + 0.5D, center.getY(),
                center.getZ() + 0.5D).subtract(player.position())).inflate(radius, 0.0D, radius);
        return isFinite(box) && prepareChunkRange(level, Mth.floor(box.minX - 1.0e-7D) - 1,
                Mth.floor(box.minZ - 1.0e-7D) - 1, Mth.floor(box.maxX + 1.0e-7D) + 1,
                Mth.floor(box.maxZ + 1.0e-7D) + 1, canContinue);
    }

    private static boolean prepareChunkRange(ServerLevel level, int minX, int minZ, int maxX, int maxZ,
                                               BooleanSupplier canContinue) {
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!canContinue.getAsBoolean()) {
                    return false;
                }
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    level.getChunk(chunkX, chunkZ);
                }
                if (!canContinue.getAsBoolean()) {
                    return false;
                }
            }
        }
        return canContinue.getAsBoolean() && hasReadableChunks(level, minX, minZ, maxX, maxZ);
    }

    private static boolean hasReadableChunks(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isFinite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    private static boolean rememberReturnAnchor(ServerPlayer player, BlockPos source) {
        String target = RETURN_ANCHOR_PREFIX + source.getX() + "," + source.getY() + "," + source.getZ();
        Map<String, String> replacements = new LinkedHashMap<>();
        for (String key : EchoAbilityManager.getData(player).getActivatedMechanisms()) {
            if (key.startsWith(RETURN_ANCHOR_PREFIX)) {
                replacements.put(key, target);
            }
        }
        return replacements.isEmpty() ? EchoAbilityManager.activateMechanism(player, target)
                : EchoAbilityManager.completeReservedMechanisms(player, replacements);
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
