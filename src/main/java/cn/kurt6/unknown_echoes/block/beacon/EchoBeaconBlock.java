package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 残响信标:V0.1 核心信标(等级 RESONANCE)。
 * 未激活:需要玩家手持回声境域钥匙唤醒,然后写入玩家个人入口记录。
 * 已激活:再次右键传送进入回声境域;在回声境域中右键则返回主世界。
 * 激活状态记录在玩家数据中(个人进度),钥匙只是一次性入口材料。
 */
public class EchoBeaconBlock extends Block {
    public static final MapCodec<EchoBeaconBlock> CODEC = simpleCodec(EchoBeaconBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 5, 15),
            Block.box(5, 5, 5, 11, 16, 11));

    public static final ResourceKey<Level> ECHO_REALM = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, UnknownEchoes.id("echo_realm"));
    public static final BlockPos REALM_ARRIVAL = new BlockPos(0, 100, 0);

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
        // 在回声境域中:返回主世界
        if (level.dimension().equals(ECHO_REALM)) {
            teleportBack(serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasActivatedBeacon(serverPlayer, EchoPermission.RESONANCE_BEACON_ID)) {
            teleportToRealm(serverPlayer);
            return InteractionResult.CONSUME;
        }

        // 未激活:只能用回声境域钥匙唤醒,碎片/核心先在合成台中变成钥匙。
        if (!activationKey.is(ModItems.ECHO_KEY.get())) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.beacon.need_key"), true);
            return InteractionResult.CONSUME;
        }

        if (!serverPlayer.getAbilities().instabuild) {
            activationKey.shrink(1);
        }

        EchoAbilityManager.activateBeacon(serverPlayer, EchoPermission.RESONANCE_BEACON_ID);
        EchoAbilityManager.unlockDimension(serverPlayer, EchoPermission.ECHO_REALM_ID);

        serverPlayer.sendSystemMessage(Component.translatable("message.unknown_echoes.beacon.activated"));
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        }
        return InteractionResult.CONSUME;
    }

    public static void teleportToRealm(ServerPlayer player) {
        if (!CommonConfig.ENABLE_ECHO_REALM.get()) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.disabled"), true);
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
        realm.getChunkAt(REALM_ARRIVAL);
        BlockPos arrival = ensureArrivalPlatform(realm)
                ? REALM_ARRIVAL : findSafeArrival(realm);
        if (arrival == null || !ensureReturnBeaconNear(realm, arrival)) {
            UnknownEchoes.LOGGER.warn("Echo Realm arrival area is obstructed near {}", REALM_ARRIVAL);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm.arrival_blocked"), true);
            return;
        }
        // 同线程 server.execute 会立即执行,必须用 TickTask 排到下一 tick 才真正脱离交互 tick(gotcha #0)
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (player.isRemoved() || !player.isAlive() || player.level() == realm) {
                return;
            }
            player.teleportTo(realm, arrival.getX() + 0.5, arrival.getY(),
                    arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.entered"), true);
            realm.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
        }));
    }

    public static void teleportBack(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos target = player.getRespawnPosition() != null
                && player.getRespawnDimension().equals(Level.OVERWORLD)
                ? player.getRespawnPosition() : overworld.getSharedSpawnPos();
        // gotcha #0:预加载目标区块;同线程 server.execute 是立即执行,用 TickTask 排到下一 tick
        overworld.getChunkAt(target);
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            if (player.isRemoved() || !player.isAlive() || player.level() == overworld) {
                return;
            }
            player.teleportTo(overworld, target.getX() + 0.5, target.getY() + 1.0,
                    target.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.returned"), true);
        }));
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
        for (BlockPos pos : BlockPos.betweenClosed(arrival.offset(-4, -1, -4), arrival.offset(4, 1, 4))) {
            if (realm.getBlockState(pos).is(ModBlocks.RESONANCE_BEACON.get())) {
                return true;
            }
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

    private static boolean isSafeArrival(ServerLevel realm, BlockPos pos) {
        return realm.getBlockState(pos).getCollisionShape(realm, pos).isEmpty()
                && realm.getBlockState(pos.above()).getCollisionShape(realm, pos.above()).isEmpty()
                && Block.canSupportCenter(realm, pos.below(), Direction.UP);
    }
}
