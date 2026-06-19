package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.CommonConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
        // 在回声境域中:返回主世界
        if (level.dimension() == ECHO_REALM) {
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
        // 先准备目标维度(生成平台、预加载区块),再在下一 tick 传送,避免同步阻塞
        ensureArrivalPlatform(realm);
        realm.getChunkAt(REALM_ARRIVAL); // 强制加载目标区块
        player.server.execute(() -> {
            if (player.isRemoved() || !player.isAlive()) {
                return; // 玩家在延迟期间离线/死亡,跳过传送
            }
            player.teleportTo(realm, REALM_ARRIVAL.getX() + 0.5, REALM_ARRIVAL.getY(),
                    REALM_ARRIVAL.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.entered"), true);
            realm.playSound(null, REALM_ARRIVAL, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
        });
    }

    public static void teleportBack(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos target = player.getRespawnPosition() != null
                && player.getRespawnDimension() == Level.OVERWORLD
                ? player.getRespawnPosition() : overworld.getSharedSpawnPos();
        player.teleportTo(overworld, target.getX() + 0.5, target.getY() + 1.0,
                target.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.translatable("message.unknown_echoes.realm.returned"), true);
    }

    /** 首次进入时在固定坐标生成安全平台和返回信标,避免玩家落入虚空或卡入地形。 */
    private static void ensureArrivalPlatform(ServerLevel realm) {
        BlockPos center = REALM_ARRIVAL.below();
        if (realm.getBlockState(center).is(ModBlocks.ECHO_STONE_BRICKS.get())) {
            return;
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                realm.setBlock(center.offset(x, 0, z),
                        ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), 3);
                for (int y = 1; y <= 3; y++) {
                    BlockPos clear = center.offset(x, y, z);
                    if (!realm.getBlockState(clear).isAir()) {
                        realm.setBlock(clear, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        realm.setBlock(center.offset(2, 1, 2), ModBlocks.RESONANCE_BEACON.get().defaultBlockState(), 3);
    }
}
