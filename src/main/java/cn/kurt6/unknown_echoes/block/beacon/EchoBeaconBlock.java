package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.config.CommonConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** 残响信标: 核心信标(等级 RESONANCE)。 */
public class EchoBeaconBlock extends Block {
    public static final MapCodec<EchoBeaconBlock> CODEC = simpleCodec(EchoBeaconBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 5, 15),
            Block.box(5, 5, 5, 11, 16, 11));

    public static final ResourceKey<Level> ECHO_REALM = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, UnknownEchoes.id("echo_realm"));
    public static final BlockPos REALM_ARRIVAL = new BlockPos(0, 100, 0);
    /** 覆盖备用落点搜索 8 格和其回程信标的 4 格范围。 */
    private static final int ARRIVAL_PREPARE_RADIUS = 12;
    private static final Set<ServerPlayer> INTERACTING = Collections.newSetFromMap(new WeakHashMap<>());

    public EchoBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (!stack.is(ModItems.ECHO_KEY.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            handleUse(level, pos, serverPlayer, stack);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        // 主手默认交互不能截断副手钥匙的正常使用。
        if (player.getOffhandItem().is(ModItems.ECHO_KEY.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        return handleUse(level, pos, serverPlayer, ItemStack.EMPTY);
    }

    public static InteractionResult activateWithKey(Level level, BlockPos pos, ServerPlayer serverPlayer,
                                                    ItemStack keyStack) {
        return handleUse(level, pos, serverPlayer, keyStack);
    }

    private static InteractionResult handleUse(Level level, BlockPos pos, ServerPlayer serverPlayer,
                                               ItemStack activationKey) {
        pos = pos.immutable();
        if (!(level instanceof ServerLevel sourceLevel) || !isInteractionCurrent(serverPlayer, sourceLevel, pos)
                || !INTERACTING.add(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        try {
            return useBeacon(sourceLevel, pos, serverPlayer, activationKey);
        } finally {
            INTERACTING.remove(serverPlayer);
        }
    }

    private static InteractionResult useBeacon(ServerLevel level, BlockPos pos, ServerPlayer serverPlayer,
                                               ItemStack activationKey) {
        if (level.dimension().equals(ECHO_REALM)) {
            teleportBack(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasActivatedBeacon(serverPlayer, EchoPermission.RESONANCE_BEACON_ID)) {
            teleportToRealm(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }

        // 未激活:只能用回声境域钥匙唤醒
        if (!activationKey.is(ModItems.ECHO_KEY.get())) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            if (isInteractionCurrent(serverPlayer, level, pos)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.beacon.need_key"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (activationKey != serverPlayer.getMainHandItem()
                && activationKey != serverPlayer.getOffhandItem()) {
            return InteractionResult.CONSUME;
        }

        if (!CommonConfig.ENABLE_ECHO_REALM.get()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.realm.disabled"), true);
            return InteractionResult.CONSUME;
        }

        boolean consumeKey = !serverPlayer.getAbilities().instabuild;
        if (!EchoAbilityManager.activateBeaconRoute(serverPlayer,
                EchoPermission.RESONANCE_BEACON_ID, EchoPermission.ECHO_REALM_ID, () -> {
                    if (consumeKey) {
                        activationKey.shrink(1);
                    }
                })) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }

        if (!isActivationCurrent(serverPlayer, level, pos)) {
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (isActivationCurrent(serverPlayer, level, pos)) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean isInteractionCurrent(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return isPlayerCurrent(player, level) && !player.isSpectator()
                && player.canInteractWithBlock(pos, 1.0D)
                && isBeaconPresent(level, pos, ModBlocks.RESONANCE_BEACON.get());
    }

    private static boolean isActivationCurrent(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return isInteractionCurrent(player, level, pos) && CommonConfig.ENABLE_ECHO_REALM.get()
                && EchoPermission.canEnterEchoRealm(player);
    }

    public static void teleportToRealm(ServerPlayer player) {
        queueTeleportToRealm(player, null);
    }

    private static void teleportToRealm(ServerPlayer player, BlockPos sourceBeacon) {
        queueTeleportToRealm(player, sourceBeacon.immutable());
    }

    private static void queueTeleportToRealm(ServerPlayer player, BlockPos sourceBeacon) {
        if (!CommonConfig.ENABLE_ECHO_REALM.get()) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.disabled"), true);
            return;
        }
        ServerLevel sourceLevel = player.serverLevel();
        if (!isRealmTransitCurrent(player, sourceLevel, sourceBeacon)) {
            return;
        }
        ServerLevel realm = player.server.getLevel(ECHO_REALM);
        if (realm == null) {
            UnknownEchoes.LOGGER.error("Echo Realm dimension is missing: {}", ECHO_REALM.location());
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.ECHO_KEY.get())) {
            return;
        }
        BooleanSupplier canContinue = () -> isRealmTransitCurrent(player, sourceLevel, sourceBeacon)
                && player.server.getLevel(ECHO_REALM) == realm;
        player.getCooldowns().addCooldown(ModItems.ECHO_KEY.get(), 20);
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            // 落点跨区块边界，准备与移动分别排队，准备前先复核请求。
            if (!prepareArrivalChunks(realm, REALM_ARRIVAL, ARRIVAL_PREPARE_RADIUS, canContinue)) {
                return;
            }
            boolean platformReady = ensureArrivalPlatform(realm);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            BlockPos arrival = platformReady ? REALM_ARRIVAL : findSafeArrival(realm);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            boolean returnReady = arrival != null && ensureReturnBeaconNear(realm, arrival);
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!returnReady) {
                UnknownEchoes.LOGGER.warn("Echo Realm arrival area is obstructed near {}", REALM_ARRIVAL);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm.arrival_blocked"), true);
                return;
            }
            if (!prepareLandingChunks(realm, Vec3.atBottomCenterOf(arrival), player, canContinue)) {
                return;
            }
            player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
                if (!canContinue.getAsBoolean()) {
                    return;
                }
                if (!isSafeArrival(realm, arrival, player) || !hasReturnBeaconNear(realm, arrival)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.realm.arrival_blocked"), true);
                    return;
                }
                if (!canContinue.getAsBoolean()) {
                    return;
                }
                player.teleportTo(realm, arrival.getX() + 0.5, arrival.getY(),
                        arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
                if (!isAtArrival(player, realm, arrival) || sourceBeacon != null && player.isSpectator()) {
                    return;
                }
                if (sourceBeacon != null) {
                    ModAdvancements.recordEchoRealmEntry(player);
                    if (!isAtArrival(player, realm, arrival) || player.isSpectator()) {
                        return;
                    }
                }
                realm.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
            }));
        }));
    }

    private static boolean isRealmTransitCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                 @Nullable BlockPos sourceBeacon) {
        return isPlayerCurrent(player, sourceLevel)
                && CommonConfig.ENABLE_ECHO_REALM.get()
                && (sourceBeacon == null || isRealmEntryCurrent(player, sourceLevel, sourceBeacon));
    }

    private static boolean isRealmEntryCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                               BlockPos sourceBeacon) {
        return player.serverLevel() == sourceLevel && !player.isSpectator()
                && EchoPermission.canEnterEchoRealm(player)
                && player.canInteractWithBlock(sourceBeacon, 1.0D)
                && isBeaconPresent(sourceLevel, sourceBeacon, ModBlocks.RESONANCE_BEACON.get());
    }

    public static void teleportBack(ServerPlayer player) {
        queueTeleportBack(player, null, null);
    }

    private static void teleportBack(ServerPlayer player, BlockPos sourceBeacon) {
        queueTeleportBack(player, sourceBeacon.immutable(), ModBlocks.RESONANCE_BEACON.get());
    }

    static void teleportBackFromAbyssBeacon(ServerPlayer player, BlockPos sourceBeacon) {
        queueTeleportBack(player, sourceBeacon.immutable(), ModBlocks.ABYSS_BEACON.get());
    }

    private static void queueTeleportBack(ServerPlayer player, @Nullable BlockPos sourceBeacon,
                                          @Nullable Block sourceBlock) {
        ServerLevel overworld = player.server.overworld();
        ServerLevel sourceLevel = player.serverLevel();
        if (!isReturnSourceCurrent(player, sourceLevel, sourceBeacon, sourceBlock)) {
            return;
        }
        boolean personalRespawn = hasOverworldRespawn(player);
        ReturnSpawn request = new ReturnSpawn(
                personalRespawn ? player.getRespawnPosition().immutable() : null,
                player.getRespawnAngle(), player.isRespawnForced(), overworld.getSharedSpawnPos().immutable());
        BooleanSupplier canContinue = () -> isReturnSourceCurrent(player, sourceLevel, sourceBeacon, sourceBlock)
                && player.server.overworld() == overworld;
        BooleanSupplier canPrepare = () -> canContinue.getAsBoolean() && request.isCurrent(player, overworld);

        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!request.isCurrent(player, overworld)) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm.return_target_changed"), true);
                return;
            }
            if (personalRespawn && !prepareArrivalChunks(overworld, request.personalPos(), 4, canPrepare)) {
                return;
            }
            DimensionTransition target = resolveReturnTarget(player, overworld);
            if (!canPrepare.getAsBoolean() || target.newLevel() != overworld
                    || !prepareLandingChunks(overworld, target.pos(), player, canPrepare)) {
                return;
            }
            player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
                if (!canContinue.getAsBoolean()) {
                    return;
                }
                if (!request.isCurrent(player, overworld)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.realm.return_target_changed"), true);
                    return;
                }
                // 公共出生点会随机调整位置；缺失床位时保留本次已准备的回退位置。
                boolean recheckRespawn = personalRespawn && !target.missingRespawnBlock();
                if (recheckRespawn && !hasReadableChunks(overworld, request.personalPos().getX() - 4,
                        request.personalPos().getZ() - 4, request.personalPos().getX() + 4,
                        request.personalPos().getZ() + 4)) {
                    return;
                }
                DimensionTransition currentTarget = recheckRespawn ? resolveReturnTarget(player, overworld) : target;
                if (!canPrepare.getAsBoolean()) {
                    return;
                }
                if (!sameReturnTarget(target, currentTarget)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.realm.return_target_changed"), true);
                    return;
                }
                if (!isReturnTargetSafe(player, currentTarget)) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.realm.return_blocked"), true);
                    return;
                }
                if (!canPrepare.getAsBoolean()) {
                    return;
                }
                player.teleportTo(currentTarget.newLevel(), currentTarget.pos().x, currentTarget.pos().y,
                        currentTarget.pos().z, currentTarget.yRot(), currentTarget.xRot());
            }));
        }));
    }

    private record ReturnSpawn(@Nullable BlockPos personalPos, float angle, boolean forced, BlockPos sharedSpawn) {
        private boolean isCurrent(ServerPlayer player, ServerLevel overworld) {
            if (!sharedSpawn.equals(overworld.getSharedSpawnPos())) {
                return false;
            }
            return personalPos == null ? !hasOverworldRespawn(player)
                    : hasOverworldRespawn(player) && personalPos.equals(player.getRespawnPosition())
                    && Float.compare(angle, player.getRespawnAngle()) == 0 && forced == player.isRespawnForced();
        }
    }

    private static boolean isReturnSourceCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                 @Nullable BlockPos sourceBeacon,
                                                 @Nullable Block sourceBlock) {
        return isPlayerCurrent(player, sourceLevel)
                && (sourceBeacon == null || !player.isSpectator() && sourceBlock != null
                && player.canInteractWithBlock(sourceBeacon, 1.0D)
                && isBeaconPresent(sourceLevel, sourceBeacon, sourceBlock));
    }

    private static boolean isPlayerCurrent(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.isAlive() && !player.hasDisconnected()
                && player.level() == level
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.server.getLevel(level.dimension()) == level;
    }

    private static boolean isBeaconPresent(ServerLevel level, BlockPos pos, Block block) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos).is(block);
    }

    private static boolean isAtArrival(ServerPlayer player, ServerLevel level, BlockPos arrival) {
        return isPlayerCurrent(player, level)
                && player.position().distanceToSqr(Vec3.atBottomCenterOf(arrival)) <= 0.01D;
    }

    private static boolean hasOverworldRespawn(ServerPlayer player) {
        return player.getRespawnDimension().equals(Level.OVERWORLD)
                && player.getRespawnPosition() != null;
    }

    private static DimensionTransition resolveReturnTarget(ServerPlayer player, ServerLevel overworld) {
        if (hasOverworldRespawn(player)) {
            return player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        }
        return new DimensionTransition(overworld, player, DimensionTransition.DO_NOTHING);
    }

    private static boolean sameReturnTarget(DimensionTransition queued, DimensionTransition current) {
        return current != null
                && current.newLevel() == queued.newLevel()
                && current.missingRespawnBlock() == queued.missingRespawnBlock()
                && current.pos().equals(queued.pos());
    }

    private static boolean isReturnTargetSafe(ServerPlayer player, DimensionTransition target) {
        Vec3 offset = target.pos().subtract(player.position());
        AABB moved = player.getBoundingBox().move(offset);
        return isFinite(moved) && moved.minY >= target.newLevel().getMinBuildHeight()
                && moved.maxY <= target.newLevel().getMaxBuildHeight()
                && target.newLevel().getWorldBorder().isWithinBounds(moved)
                && hasReadableCollisionChunks(target.newLevel(), moved)
                && target.newLevel().noCollision(player, moved);
    }

    /** 一次备齐落点周围区块;逐格 getBlockState 会在未加载区块上同步生成。 */
    private static boolean prepareArrivalChunks(ServerLevel realm, BlockPos center, int radius,
                                                 BooleanSupplier canContinue) {
        return prepareChunkRange(realm, center.getX() - radius, center.getZ() - radius,
                center.getX() + radius, center.getZ() + radius, canContinue);
    }

    private static boolean prepareLandingChunks(ServerLevel level, Vec3 target, ServerPlayer player,
                                                 BooleanSupplier canContinue) {
        AABB box = player.getBoundingBox().move(target.subtract(player.position()));
        return isFinite(box) && box.minY >= level.getMinBuildHeight()
                && box.maxY <= level.getMaxBuildHeight() && level.getWorldBorder().isWithinBounds(box)
                && prepareChunkRange(level, Mth.floor(box.minX - 1.0e-7D) - 1,
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

    private static boolean hasReadableCollisionChunks(ServerLevel level, AABB box) {
        return hasReadableChunks(level, Mth.floor(box.minX - 1.0e-7D) - 1,
                Mth.floor(box.minZ - 1.0e-7D) - 1, Mth.floor(box.maxX + 1.0e-7D) + 1,
                Mth.floor(box.maxZ + 1.0e-7D) + 1);
    }

    private static boolean isFinite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    private static boolean ensureArrivalPlatform(ServerLevel realm) {
        BlockPos center = REALM_ARRIVAL.below();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos base = center.offset(x, 0, z);
                BlockState baseState = realm.getBlockState(base);
                if (!baseState.is(ModBlocks.ECHO_STONE_BRICKS.get()) && !baseState.canBeReplaced()) {
                    return false;
                }
                for (int y = 1; y <= 3; y++) {
                    BlockPos clear = center.offset(x, y, z);
                    BlockState clearState = realm.getBlockState(clear);
                    boolean returnBeacon = clear.equals(center.offset(2, 1, 2))
                            && clearState.is(ModBlocks.RESONANCE_BEACON.get());
                    if (!returnBeacon && !clearState.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                realm.setBlock(center.offset(x, 0, z),
                        ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), 3);
                for (int y = 1; y <= 3; y++) {
                    BlockPos clear = center.offset(x, y, z);
                    if (!realm.getBlockState(clear).isAir()
                            && !realm.getBlockState(clear).is(ModBlocks.RESONANCE_BEACON.get())) {
                        realm.setBlock(clear, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        realm.setBlock(center.offset(2, 1, 2), ModBlocks.RESONANCE_BEACON.get().defaultBlockState(), 3);
        return isSafeArrival(realm, REALM_ARRIVAL);
    }

    private static BlockPos findSafeArrival(ServerLevel realm) {
        if (isSafeArrival(realm, REALM_ARRIVAL)) {
            return REALM_ARRIVAL;
        }
        for (int radius = 1; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos candidate = REALM_ARRIVAL.offset(x, 0, z);
                    if (isSafeArrival(realm, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        for (int radius = 0; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    int worldX = REALM_ARRIVAL.getX() + x;
                    int worldZ = REALM_ARRIVAL.getZ() + z;
                    var chunk = realm.getChunkSource().getChunkNow(worldX >> 4, worldZ >> 4);
                    if (chunk == null) {
                        continue;
                    }
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX & 15, worldZ & 15) + 1;
                    BlockPos candidate = new BlockPos(worldX, y, worldZ);
                    if (isSafeArrival(realm, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean ensureReturnBeaconNear(ServerLevel realm, BlockPos arrival) {
        if (!hasReadableChunks(realm, arrival.getX() - 4, arrival.getZ() - 4,
                arrival.getX() + 4, arrival.getZ() + 4)) {
            return false;
        }
        if (hasReturnBeaconNear(realm, arrival)) {
            return true;
        }
        for (int radius = 2; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((Math.abs(x) != radius && Math.abs(z) != radius) || (x == 0 && z == 0)) {
                        continue;
                    }
                    BlockPos beaconPos = arrival.offset(x, 0, z);
                    if (realm.getBlockState(beaconPos).isAir()
                            && Block.canSupportCenter(realm, beaconPos.below(), Direction.UP)) {
                        return realm.setBlock(beaconPos,
                                ModBlocks.RESONANCE_BEACON.get().defaultBlockState(), 3);
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasReturnBeaconNear(ServerLevel realm, BlockPos arrival) {
        if (!hasReadableChunks(realm, arrival.getX() - 4, arrival.getZ() - 4,
                arrival.getX() + 4, arrival.getZ() + 4)) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(arrival.offset(-4, -1, -4), arrival.offset(4, 1, 4))) {
            if (realm.getBlockState(pos).is(ModBlocks.RESONANCE_BEACON.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSafeArrival(ServerLevel realm, BlockPos pos) {
        return isSafeArrival(realm, pos, null);
    }

    private static boolean isSafeArrival(ServerLevel realm, BlockPos pos, @Nullable ServerPlayer player) {
        AABB box;
        if (player == null) {
            box = new AABB(pos.getX() + 0.2D, pos.getY(), pos.getZ() + 0.2D,
                    pos.getX() + 0.8D, pos.getY() + 1.8D, pos.getZ() + 0.8D);
        } else {
            Vec3 target = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            box = player.getBoundingBox().move(target.subtract(player.position()));
        }
        return isFinite(box) && box.minY >= realm.getMinBuildHeight() + 1
                && box.maxY <= realm.getMaxBuildHeight()
                && realm.getWorldBorder().isWithinBounds(box)
                && hasReadableCollisionChunks(realm, box)
                && realm.noCollision(player, box)
                && Block.canSupportCenter(realm, pos.below(), Direction.UP);
    }
}
