package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;

/** 音序核心方块:右键播放旋律演示,由 ChimeEchoCoreBlockEntity 驱动。 */
public class ChimeEchoCoreBlock extends BaseEntityBlock {
    public static final MapCodec<ChimeEchoCoreBlock> CODEC = simpleCodec(ChimeEchoCoreBlock::new);
    public static final BooleanProperty ACTIVE = PuzzleCoreBlock.ACTIVE;

    public ChimeEchoCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChimeEchoCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHIME_ECHO_CORE.get(),
                (tickLevel, pos, tickState, core) -> core.serverTick(tickLevel));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.hasChunkAt(pos) || level.getBlockState(pos) != state) {
            return InteractionResult.CONSUME;
        }
        if (state.getValue(ACTIVE)) {
            PuzzleSolveEffects.recoverSeals(level, pos, state);
        } else if (level.getBlockEntity(pos) instanceof ChimeEchoCoreBlockEntity core) {
            if (!core.tryFinishPending()) {
                core.startDemo(player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            }
        }
        return InteractionResult.CONSUME;
    }
}
