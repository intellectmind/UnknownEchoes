package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 水下符文座:潮汐灯塔礁的致盲机制方块( 潮汐执灯者)。 */
public class TideRuneSeatBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<TideRuneSeatBlock> CODEC = simpleCodec(TideRuneSeatBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** 致盲触发后的符文座冷却(tick)。 */
    public static final int COOLDOWN_TICKS = 400;
    /** 符文座生效半径:覆盖整个灯塔礁战场。 */
    private static final double KEEPER_RANGE = 16.0D;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 7, 15),
            Block.box(4, 7, 11, 12, 13, 16));

    public TideRuneSeatBlock(Properties properties) {
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

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), 3);
            MechanismFx.emit(level, ExplorationFx.tideSeat(Vec3.atBottomCenterOf(pos), true));
            level.playSound(null, pos, ModSounds.BLOCK_TIDE_SEAT_READY.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.blurred"), true);
            return InteractionResult.CONSUME;
        }
        if (!state.getValue(LIT)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.cooling"), true);
            return InteractionResult.CONSUME;
        }
        TideLanternKeeper keeper = findKeeper(serverLevel, pos);
        if (keeper == null || !keeper.isLanternLit()) {

            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.idle"), true);
            return InteractionResult.CONSUME;
        }
        keeper.applyRuneBlind(player);
        level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), 3);
        serverLevel.scheduleTick(pos, this, COOLDOWN_TICKS);
        serverLevel.playSound(null, pos, ModSounds.BLOCK_TIDE_SEAT_FLASH.get(), SoundSource.BLOCKS, 0.85F, 1.0F);
        MechanismFx.emit(serverLevel, ExplorationFx.tideSeat(Vec3.atBottomCenterOf(pos), false));
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.tide_rune_seat.flash"), true);
        return InteractionResult.CONSUME;
    }

    private static TideLanternKeeper findKeeper(ServerLevel level, BlockPos pos) {
        for (TideLanternKeeper keeper : level.getEntitiesOfClass(TideLanternKeeper.class,
                new AABB(pos).inflate(KEEPER_RANGE),
                keeper -> keeper.isAlive()
                        && keeper.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= KEEPER_RANGE * KEEPER_RANGE)) {
            return keeper;
        }
        return null;
    }
}
