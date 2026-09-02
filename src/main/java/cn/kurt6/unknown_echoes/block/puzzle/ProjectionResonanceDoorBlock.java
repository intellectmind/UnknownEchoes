package cn.kurt6.unknown_echoes.block.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 投影共鸣门：只能由投影共鸣踏板开关，生存模式下不可直接挖掘。 */
public final class ProjectionResonanceDoorBlock extends Block {
    public static final MapCodec<ProjectionResonanceDoorBlock> CODEC =
            simpleCodec(ProjectionResonanceDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape CLOSED_SHAPE = Shapes.block();
    private static final VoxelShape OPEN_SHAPE = Shapes.empty();

    public ProjectionResonanceDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPE : CLOSED_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPE : CLOSED_SHAPE;
    }
}
