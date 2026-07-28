package cn.kurt6.unknown_echoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 仅允许在完整水源中存活，且不会被骨粉替换为原版高海草。 */
public class AquaticBiomePlantBlock extends SeagrassBlock {
    public AquaticBiomePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.getAmount() == 8
                && super.canSurvive(state, level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }
}
