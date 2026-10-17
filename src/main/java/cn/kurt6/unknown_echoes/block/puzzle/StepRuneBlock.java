package cn.kurt6.unknown_echoes.block.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 步纹符文:步径谜题的踩踏输入。薄板嵌在地面,玩家踩上去即把自己的 ORDER 报给附近的谜题核心;
 * 按壁画顺序走完全程解开封印,踩错整条路径熄灭重来。
 */
public class StepRuneBlock extends Block {
    public static final MapCodec<StepRuneBlock> CODEC = simpleCodec(StepRuneBlock::new);
    public static final IntegerProperty ORDER = SequenceRuneBlock.ORDER;
    public static final BooleanProperty LIT = SequenceRuneBlock.LIT;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private static final int RETRIGGER_TICKS = 20;

    public StepRuneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ORDER, 0).setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORDER, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.isSpectator()
                || !player.isAlive() || player.isRemoved() || player.hasDisconnected() || player.level() != level) {
            return;
        }

        if (level.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        level.scheduleTick(pos, this, RETRIGGER_TICKS);
        PuzzleCoreBlockEntity core = SequenceRuneBlock.findCore(level, pos);
        if (core == null || state.getValue(LIT) && !core.acceptsLitRuneInput()) {
            return;
        }
        core.tryActivate(state.getValue(ORDER), pos, player);
    }
}
