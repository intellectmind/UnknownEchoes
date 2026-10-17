package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 风门:能力权限系统的核心验证方块。 */
public class WindDoorBlock extends Block {
    public static final MapCodec<WindDoorBlock> CODEC = simpleCodec(WindDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty CONN_NORTH = BooleanProperty.create("north");
    public static final BooleanProperty CONN_SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty CONN_WEST = BooleanProperty.create("west");
    public static final BooleanProperty CONN_EAST = BooleanProperty.create("east");
    public static final BooleanProperty CONN_UP = BooleanProperty.create("up");

    /** 联动开关的最大方块数,防止异常超大区域 */
    private static final int MAX_CONNECTED = 64;
    private static final ThreadLocal<Map<ServerLevel, Set<BlockPos>>> TOGGLING =
            ThreadLocal.withInitial(java.util.IdentityHashMap::new);

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
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (EchoPermission.canOpenWindDoor(serverPlayer)) {
            toggleConnected(serverLevel, pos, state, serverPlayer);
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.6F);
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.wind_door.locked"), true);
        }
        return InteractionResult.CONSUME;
    }

    private boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                         ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D) && isCurrentDoor(level, pos, state);
    }

    private boolean isCurrentDoor(ServerLevel level, BlockPos pos, BlockState state) {
        return level.hasChunkAt(pos) && state.is(this) && level.getBlockState(pos) == state;
    }

    /** 保存本次联动的原现场，写入失败或回调改变现场时停止剩余操作。 */
    private void toggleConnected(ServerLevel level, BlockPos origin, BlockState original,
                                 ServerPlayer player) {
        boolean open = !original.getValue(OPEN);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockState> found = new LinkedHashMap<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty() && found.size() < MAX_CONNECTED) {
            BlockPos current = queue.removeFirst();
            if (found.containsKey(current) || !level.hasChunkAt(current)) {
                continue;
            }
            BlockState door = level.getBlockState(current);
            if (!door.is(this)) {
                continue;
            }
            found.put(current, door);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (level.hasChunkAt(next)) {
                    queue.addLast(next.immutable());
                }
            }
        }
        if (found.isEmpty()) {
            return;
        }
        Map<ServerLevel, Set<BlockPos>> active = TOGGLING.get();
        Set<BlockPos> reserved = active.computeIfAbsent(level, ignored -> new HashSet<>());
        if (found.keySet().stream().anyMatch(reserved::contains)) {
            return;
        }
        reserved.addAll(found.keySet());
        try {
            BlockState expectedOrigin = original;
            for (var entry : found.entrySet()) {
                if (!isCurrentInteraction(level, origin, expectedOrigin, player)
                        || !EchoPermission.canOpenWindDoor(player)) {
                    return;
                }
                BlockPos doorPos = entry.getKey();
                BlockState door = entry.getValue();
                if (!isCurrentDoor(level, doorPos, door)) {
                    return;
                }
                BlockState target = door.setValue(OPEN, open);
                boolean applied = door == target || level.setBlock(doorPos, target, 3);
                if (!applied || !isCurrentDoor(level, doorPos, target)) {
                    return;
                }
                if (doorPos.equals(origin)) {
                    expectedOrigin = target;
                }
            }
            // 后面的写入也可能改变前面的门，整组确认后才登记个人试炼。
            if (!isCompletedBatch(level, origin, expectedOrigin, player, found, open)) {
                return;
            }
            ArchiveTrialProgress.recordWind(player, level, origin);
            if (isCompletedBatch(level, origin, expectedOrigin, player, found, open)) {
                level.playSound(null, origin, open ? SoundEvents.BREEZE_IDLE_AIR : SoundEvents.BREEZE_LAND,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } finally {
            reserved.removeAll(found.keySet());
            if (reserved.isEmpty()) {
                active.remove(level);
            }
            if (active.isEmpty()) {
                TOGGLING.remove();
            }
        }
    }

    private boolean isCompletedBatch(ServerLevel level, BlockPos origin, BlockState expectedOrigin,
                                      ServerPlayer player, Map<BlockPos, BlockState> found, boolean open) {
        if (!isCurrentInteraction(level, origin, expectedOrigin, player)
                || !EchoPermission.canOpenWindDoor(player)) {
            return false;
        }
        for (var entry : found.entrySet()) {
            if (!isCurrentDoor(level, entry.getKey(), entry.getValue().setValue(OPEN, open))) {
                return false;
            }
        }
        return true;
    }
}
