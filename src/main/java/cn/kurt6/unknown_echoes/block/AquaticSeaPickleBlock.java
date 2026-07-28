package cn.kurt6.unknown_echoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** 水下簇生植物：拒绝陆地放置，并禁用原版海泡菜的骨粉扩散。 */
public class AquaticSeaPickleBlock extends SeaPickleBlock {
    public AquaticSeaPickleBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var fluid = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluid.is(FluidTags.WATER) || fluid.getAmount() != 8) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var fluid = level.getFluidState(pos);
        return state.getValue(WATERLOGGED)
                && fluid.is(FluidTags.WATER)
                && fluid.getAmount() == 8
                && super.canSurvive(state, level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }
}
