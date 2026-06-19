package cn.kurt6.unknown_echoes.block.wind;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 风流平台(V0.6E,5.2 权限目标落地):漂浮群岛机关方块。
 * 持风之回响右键激活(服务端校验 EchoPermission),产生限时上升气流柱——
 * 滑翔/坠落进入气流获得抬升;时长与柱高走 ServerConfig。
 * 边界(5.2/设计红线 #1):气流只是表现 + 运动效果,不做进度门槛;
 * 任何实体都能被已激活的气流抬升,但激活本身必须有风之权限。
 */
public class WindCurrentPlatformBlock extends BaseEntityBlock {
    public static final MapCodec<WindCurrentPlatformBlock> CODEC = simpleCodec(WindCurrentPlatformBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(2, 3, 2, 14, 6, 14));

    public WindCurrentPlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindCurrentPlatformBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WIND_CURRENT_PLATFORM.get(),
                WindCurrentPlatformBlockEntity::serverTick);
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
        if (!EchoPermission.canUseWindMechanism(serverPlayer)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.6F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.wind_platform.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.WIND_ECHO, "platform_locked");
            return InteractionResult.CONSUME;
        }
        // 重复激活 = 重置时长(稳定气流的回访交互,不另设冷却)
        if (level.getBlockEntity(pos) instanceof WindCurrentPlatformBlockEntity platform) {
            platform.activate(ServerConfig.WIND_PLATFORM_ACTIVE_TICKS.get());
            if (!state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, Boolean.TRUE), 3);
            }
            level.playSound(null, pos, SoundEvents.BREEZE_IDLE_AIR, SoundSource.BLOCKS, 1.2F, 0.9F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.wind_platform.activated"), true);
        }
        return InteractionResult.CONSUME;
    }
}
