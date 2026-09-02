package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TideSensorDoorBlock extends BaseEntityBlock {
    public static final MapCodec<TideSensorDoorBlock> CODEC = simpleCodec(TideSensorDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONN_NORTH = BooleanProperty.create("north");
    public static final BooleanProperty CONN_SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty CONN_WEST = BooleanProperty.create("west");
    public static final BooleanProperty CONN_EAST = BooleanProperty.create("east");
    public static final BooleanProperty CONN_UP = BooleanProperty.create("up");

    /** 联动开关的最大方块数(同风门,防异常超大区域)。 */
    static final int MAX_CONNECTED = 32;

    private static final VoxelShape TOP_NS = Block.box(0, 14, 3, 16, 16, 13);
    private static final VoxelShape TOP_EW = Block.box(3, 14, 0, 13, 16, 16);
    private static final VoxelShape POST_W = Block.box(0, 0, 3, 3, 16, 13);
    private static final VoxelShape POST_E = Block.box(13, 0, 3, 16, 16, 13);
    private static final VoxelShape POST_N = Block.box(3, 0, 0, 13, 16, 3);
    private static final VoxelShape POST_S = Block.box(3, 0, 13, 13, 16, 16);

    public TideSensorDoorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(CONN_NORTH, Boolean.FALSE).setValue(CONN_SOUTH, Boolean.FALSE)
                .setValue(CONN_WEST, Boolean.FALSE).setValue(CONN_EAST, Boolean.FALSE)
                .setValue(CONN_UP, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, FACING, CONN_NORTH, CONN_SOUTH, CONN_WEST, CONN_EAST, CONN_UP);
    }

    private static BooleanProperty connProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> CONN_NORTH;
            case SOUTH -> CONN_SOUTH;
            case WEST -> CONN_WEST;
            case EAST -> CONN_EAST;
            case UP -> CONN_UP;
            default -> null;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        for (Direction direction : Direction.values()) {
            BooleanProperty property = connProperty(direction);
            if (property != null) {
                state = state.setValue(property, context.getLevel()
                        .getBlockState(context.getClickedPos().relative(direction)).is(this));
            }
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty property = connProperty(direction);
        return property == null ? state : state.setValue(property, neighborState.is(this));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static boolean isNorthSouth(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return Shapes.block();
        }
        VoxelShape shape = Shapes.empty();
        if (!state.getValue(CONN_UP)) {
            shape = Shapes.or(shape, isNorthSouth(state) ? TOP_NS : TOP_EW);
        }
        if (isNorthSouth(state)) {
            if (!state.getValue(CONN_WEST)) {
                shape = Shapes.or(shape, POST_W);
            }
            if (!state.getValue(CONN_EAST)) {
                shape = Shapes.or(shape, POST_E);
            }
        } else {
            if (!state.getValue(CONN_NORTH)) {
                shape = Shapes.or(shape, POST_N);
            }
            if (!state.getValue(CONN_SOUTH)) {
                shape = Shapes.or(shape, POST_S);
            }
        }
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TideSensorDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TIDE_SENSOR_DOOR.get(),
                TideSensorDoorBlockEntity::serverTick);
    }

    /** 右键 = 感应的主动形式:同样走服务端权限,开门时长同感应开启。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (state.getValue(OPEN)) {
            return InteractionResult.CONSUME;
        }
        if (cn.kurt6.unknown_echoes.ability.EchoPermission.canUseTideMechanism(serverPlayer)) {
            TideSensorDoorBlockEntity.openGroup(level, pos, serverPlayer);
        } else {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                    SoundSource.BLOCKS, 0.8F, 0.6F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_door.locked"), true);
            cn.kurt6.unknown_echoes.ability.EchoAbilityManager.recordFailure(serverPlayer,
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TIDE_ECHO, "door_locked");
        }
        return InteractionResult.CONSUME;
    }
}
