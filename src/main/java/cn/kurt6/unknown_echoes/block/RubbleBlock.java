package cn.kurt6.unknown_echoes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 风蚀碎石(V0.6F,9.x 漂浮群岛地表装饰):被长年风蚀剥落的低矮碎石堆。
 * 纯装饰、低成本、无常驻 tick;非满格故 noOcclusion(gotcha #9),
 * 自定义扁平造型与碰撞箱匹配;放在任意有上表面的方块上,需下方有支撑面(canSurvive)。
 */
public class RubbleBlock extends Block {
    public static final MapCodec<RubbleBlock> CODEC = simpleCodec(RubbleBlock::new);

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D);

    public RubbleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RubbleBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
                                     BlockState neighborState, net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        // 支撑被挖掉时随邻块更新消失,canSurvive 只在放置时检查
        if (direction == net.minecraft.core.Direction.DOWN && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
