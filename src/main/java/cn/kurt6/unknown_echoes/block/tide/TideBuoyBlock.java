package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 潮汐浮标:潮汐灯塔礁的破灯机制方块( 潮汐执灯者)。 */
public class TideBuoyBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<TideBuoyBlock> CODEC = simpleCodec(TideBuoyBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** 撞灭后的复亮冷却(tick)。 */
    public static final int RELIGHT_TICKS = 400;

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public TideBuoyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, Boolean.TRUE)
                .setValue(WATERLOGGED, Boolean.TRUE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** 执灯者冲刺命中:亮灯浮标被撞灭,返回 true 表示这次撞击破灯成功。 */
    public static boolean tryExtinguish(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(LIT)) {
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), 3);
        level.scheduleTick(pos, state.getBlock(), RELIGHT_TICKS);
        level.playSound(null, pos, ModSounds.BLOCK_TIDE_BUOY_EXTINGUISH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        MechanismFx.emit(level, ExplorationFx.buoy(Vec3.atBottomCenterOf(pos), false));
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), 3);
            level.playSound(null, pos, ModSounds.BLOCK_TIDE_BUOY_RELIGHT.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            MechanismFx.emit(level, ExplorationFx.buoy(Vec3.atBottomCenterOf(pos), true));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Component.translatable(state.getValue(LIT)
                ? "message.unknown_echoes.tide_buoy.lit"
                : "message.unknown_echoes.tide_buoy.cooling"), true);
        return InteractionResult.CONSUME;
    }
}
