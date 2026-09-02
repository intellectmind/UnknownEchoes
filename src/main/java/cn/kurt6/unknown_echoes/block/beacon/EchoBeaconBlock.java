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
import net.minecraft.world.InteractionResult;
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

/** 残响信标: 核心信标(等级 RESONANCE)。 */
public class EchoBeaconBlock extends Block {
    public static final MapCodec<EchoBeaconBlock> CODEC = simpleCodec(EchoBeaconBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 5, 15),
            Block.box(5, 5, 5, 11, 16, 11));

    public static final ResourceKey<Level> ECHO_REALM = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, UnknownEchoes.id("echo_realm"));
    public static final BlockPos REALM_ARRIVAL = new BlockPos(0, 100, 0);
    /** 必须 >= 落点周边所有扫描的最大水平偏移(平台 3、回程信标 4)。 */
    private static final int ARRIVAL_PREPARE_RADIUS = 4;

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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
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
        if (serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }

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
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.beacon.need_key"), true);
            return InteractionResult.CONSUME;
        }

        if (!CommonConfig.ENABLE_ECHO_REALM.get()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.realm.disabled"), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateBeaconRoute(serverPlayer,
                EchoPermission.RESONANCE_BEACON_ID, EchoPermission.ECHO_REALM_ID)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }

        if (!serverPlayer.getAbilities().instabuild) {
            activationKey.shrink(1);
        }

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        }
        return InteractionResult.CONSUME;
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
        if (sourceBeacon != null && !isRealmEntryCurrent(player, sourceLevel, sourceBeacon)) {
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
        player.getCooldowns().addCooldown(ModItems.ECHO_KEY.get(), 20);
        // 落点在区块边界上(x=z=0),±4 的平台与回程信标横跨 4 个区块。
        // 只备中心区块会让另外 3 个在循环里逐个同步生成。
        prepareArrivalChunks(realm, REALM_ARRIVAL, ARRIVAL_PREPARE_RADIUS);
        BlockPos arrival = ensureArrivalPlatform(realm)
                ? REALM_ARRIVAL : findSafeArrival(realm);
        if (arrival == null || !ensureReturnBeaconNear(realm, arrival)) {
            UnknownEchoes.LOGGER.warn("Echo Realm arrival area is obstructed near {}", REALM_ARRIVAL);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm.arrival_blocked"), true);
            return;
        }

        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (player.isRemoved() || !player.isAlive() || player.level() != sourceLevel) {
                return;
            }
            if (!CommonConfig.ENABLE_ECHO_REALM.get()
                    || sourceBeacon != null && !isRealmEntryCurrent(player, sourceLevel, sourceBeacon)
                    || !isSafeArrival(realm, arrival, player)
                    || !hasReturnBeaconNear(realm, arrival)) {
                return;
            }
            player.teleportTo(realm, arrival.getX() + 0.5, arrival.getY(),
                    arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
            if (player.serverLevel() != realm) {
                return;
            }
            ModAdvancements.recordEchoRealmEntry(player);
            realm.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
        }));
    }

    private static boolean isRealmEntryCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                               BlockPos sourceBeacon) {
        return player.serverLevel() == sourceLevel
                && EchoPermission.canEnterEchoRealm(player)
                && sourceLevel.hasChunkAt(sourceBeacon)
                && sourceLevel.getBlockState(sourceBeacon).is(ModBlocks.RESONANCE_BEACON.get());
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
        boolean personalRespawn = hasOverworldRespawn(player);
        BlockPos sharedSpawn = overworld.getSharedSpawnPos();
        DimensionTransition target = resolveReturnTarget(player, overworld);
        BlockPos targetPos = BlockPos.containing(target.pos());

        target.newLevel().getChunkAt(targetPos);
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (!isReturnSourceCurrent(player, sourceLevel, sourceBeacon, sourceBlock)) {
                return;
            }
            DimensionTransition currentTarget;
            if (personalRespawn) {
                currentTarget = hasOverworldRespawn(player)
                        ? resolveReturnTarget(player, overworld) : null;
                if (!sameReturnTarget(target, currentTarget)) {
                    return;
                }
            } else {
                if (hasOverworldRespawn(player) || !sharedSpawn.equals(overworld.getSharedSpawnPos())) {
                    return;
                }
                currentTarget = target;
            }
            if (!isReturnTargetSafe(player, currentTarget)) {
                return;
            }
            player.teleportTo(currentTarget.newLevel(), currentTarget.pos().x, currentTarget.pos().y,
                    currentTarget.pos().z, currentTarget.yRot(), currentTarget.xRot());
            if (player.serverLevel() != currentTarget.newLevel()) {
                return;
            }
        }));
    }

    private static boolean isReturnSourceCurrent(ServerPlayer player, ServerLevel sourceLevel,
                                                 @Nullable BlockPos sourceBeacon,
                                                 @Nullable Block sourceBlock) {
        return !player.isRemoved() && player.isAlive() && player.level() == sourceLevel
                && (sourceBeacon == null || sourceBlock != null
                && sourceLevel.hasChunkAt(sourceBeacon)
                && sourceLevel.getBlockState(sourceBeacon).is(sourceBlock));
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
                && current.pos().equals(queued.pos());
    }

    private static boolean isReturnTargetSafe(ServerPlayer player, DimensionTransition target) {
        Vec3 offset = target.pos().subtract(player.position());
        AABB moved = player.getBoundingBox().move(offset);
        return target.newLevel().getWorldBorder().isWithinBounds(moved)
                && target.newLevel().noCollision(player, moved);
    }

    /** 一次备齐落点周围区块;逐格 getBlockState 会在未加载区块上同步生成。 */
    private static void prepareArrivalChunks(ServerLevel realm, BlockPos center, int radius) {
        for (int chunkX = (center.getX() - radius) >> 4; chunkX <= (center.getX() + radius) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - radius) >> 4; chunkZ <= (center.getZ() + radius) >> 4; chunkZ++) {
                realm.getChunk(chunkX, chunkZ);
            }
        }
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
                    if (realm.hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)
                            && isSafeArrival(realm, candidate)) {
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
                    if (!realm.hasChunk(worldX >> 4, worldZ >> 4)) {
                        continue;
                    }
                    int y = realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
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
        return realm.getWorldBorder().isWithinBounds(box)
                && realm.noCollision(player, box)
                && Block.canSupportCenter(realm, pos.below(), Direction.UP);
    }
}
