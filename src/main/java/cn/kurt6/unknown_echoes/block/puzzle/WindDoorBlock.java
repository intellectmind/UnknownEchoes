package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 风门:能力权限系统的核心验证方块。
 * 只有拥有风之回响的玩家可以开关;开关时相邻的风门(上下左右连成一片)一起联动,
 * 所以多格大门只需点击任意一块。带朝向(放置时面向玩家),关闭为 4 格厚门板。
 */
public class WindDoorBlock extends Block {
    public static final MapCodec<WindDoorBlock> CODEC = simpleCodec(WindDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // 与相邻风门的连接状态(开启态据此省略中间的门框柱/顶梁,多格大门视觉上合并为一扇)
    public static final BooleanProperty CONN_NORTH = BooleanProperty.create("north");
    public static final BooleanProperty CONN_SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty CONN_WEST = BooleanProperty.create("west");
    public static final BooleanProperty CONN_EAST = BooleanProperty.create("east");
    public static final BooleanProperty CONN_UP = BooleanProperty.create("up");

    /** 联动开关的最大方块数,防止异常超大区域 */
    private static final int MAX_CONNECTED = 64;

    private static final VoxelShape CLOSED_NS = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape CLOSED_EW = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape TOP_NS = Block.box(0, 13, 6, 16, 16, 10);
    private static final VoxelShape TOP_EW = Block.box(6, 13, 0, 10, 16, 16);
    private static final VoxelShape POST_W = Block.box(0, 0, 6, 2, 16, 10);
    private static final VoxelShape POST_E = Block.box(14, 0, 6, 16, 16, 10);
    private static final VoxelShape POST_N = Block.box(6, 0, 0, 10, 16, 2);
    private static final VoxelShape POST_S = Block.box(6, 0, 14, 10, 16, 16);

    public WindDoorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(CONN_NORTH, Boolean.FALSE).setValue(CONN_SOUTH, Boolean.FALSE)
                .setValue(CONN_WEST, Boolean.FALSE).setValue(CONN_EAST, Boolean.FALSE)
                .setValue(CONN_UP, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
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
            BooleanProperty prop = connProperty(direction);
            if (prop != null) {
                state = state.setValue(prop, context.getLevel()
                        .getBlockState(context.getClickedPos().relative(direction))
                        .getBlock() instanceof WindDoorBlock);
            }
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = connProperty(direction);
        if (prop != null) {
            return state.setValue(prop, neighborState.getBlock() instanceof WindDoorBlock);
        }
        return state;
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
        Direction.Axis axis = state.getValue(FACING).getAxis();
        return axis == Direction.Axis.Z;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return isNorthSouth(state) ? CLOSED_NS : CLOSED_EW;
        }
        // 开启态:只在没有相邻风门的一侧保留门框柱/顶梁,多格大门合并为一个整体门洞
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return Shapes.empty();
        }
        return isNorthSouth(state) ? CLOSED_NS : CLOSED_EW;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (EchoPermission.canOpenWindDoor(player)) {
            boolean open = !state.getValue(OPEN);
            toggleConnected(level, pos, open);
            if (player instanceof ServerPlayer serverPlayer) {
                ArchiveTrialProgress.recordWind(serverPlayer, level, pos);
            }
            level.playSound(null, pos, open ? SoundEvents.BREEZE_IDLE_AIR : SoundEvents.BREEZE_LAND,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.6F);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.wind_door.locked"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** 洪泛遍历相邻风门,整片一起开/关。 */
    private void toggleConnected(Level level, BlockPos origin, boolean open) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty() && visited.size() <= MAX_CONNECTED) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!state.is(this)) {
                continue;
            }
            if (state.getValue(OPEN) != open) {
                level.setBlock(pos, state.setValue(OPEN, open), 3);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.add(next) && level.getBlockState(next).is(this)) {
                    queue.add(next);
                }
            }
        }
    }
}
