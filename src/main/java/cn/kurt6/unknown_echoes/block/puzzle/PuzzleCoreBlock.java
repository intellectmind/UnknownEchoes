package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
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

/**
 * 谜题核心:保存符文顺序谜题的进度(BlockEntity)。
 * 全部四根顺序符文柱按正确顺序点亮后激活,清除周围的封印石。
 */
public class PuzzleCoreBlock extends BaseEntityBlock {
    public static final MapCodec<PuzzleCoreBlock> CODEC = simpleCodec(PuzzleCoreBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public PuzzleCoreBlock(Properties properties) {
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
        return new PuzzleCoreBlockEntity(pos, state);
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
        if (state.getValue(ACTIVE)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.puzzle_core.solved"), true);
        } else if (level.getBlockEntity(pos) instanceof PuzzleCoreBlockEntity core) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.puzzle_core.progress",
                            core.getProgress(), core.getSequenceLength()), true);
        }
        return InteractionResult.CONSUME;
    }
}
