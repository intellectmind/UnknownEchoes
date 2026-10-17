package cn.kurt6.unknown_echoes.block.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 对页核心方块:右键报告归档进度;配对判定见 FoldpagePairCoreBlockEntity。 */
public class FoldpagePairCoreBlock extends BaseEntityBlock {
    public static final MapCodec<FoldpagePairCoreBlock> CODEC = simpleCodec(FoldpagePairCoreBlock::new);
    public static final BooleanProperty ACTIVE = PuzzleCoreBlock.ACTIVE;

    public FoldpagePairCoreBlock(Properties properties) {
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
        return new FoldpagePairCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        } else if (level.getBlockEntity(pos) instanceof FoldpagePairCoreBlockEntity core) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.foldpage.progress",
                            core.matchedPairs(), core.totalPairs()), true);
        }
        return InteractionResult.CONSUME;
    }
}
