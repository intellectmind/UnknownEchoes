package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 重力折叠门：重力回响可短时稳定整组门体，物理绕行不授予任何进度。 */
public class GravityFoldDoorBlock extends Block {
    public static final MapCodec<GravityFoldDoorBlock> CODEC = simpleCodec(GravityFoldDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONN_NORTH = BooleanProperty.create("north");
    public static final BooleanProperty CONN_SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty CONN_WEST = BooleanProperty.create("west");
    public static final BooleanProperty CONN_EAST = BooleanProperty.create("east");
    public static final BooleanProperty CONN_UP = BooleanProperty.create("up");

    private static final int OPEN_TICKS = 140;
    private static final int MAX_CONNECTED = 32;
    private static final ThreadLocal<Map<ServerLevel, Set<BlockPos>>> OPENING =
            ThreadLocal.withInitial(HashMap::new);
    private static final VoxelShape TOP_NS = Block.box(0, 13, 5, 16, 16, 11);
    private static final VoxelShape TOP_EW = Block.box(5, 13, 0, 11, 16, 16);
    private static final VoxelShape POST_W = Block.box(0, 0, 5, 3, 16, 11);
    private static final VoxelShape POST_E = Block.box(13, 0, 5, 16, 16, 11);
    private static final VoxelShape POST_N = Block.box(5, 0, 0, 11, 16, 3);
    private static final VoxelShape POST_S = Block.box(5, 0, 13, 11, 16, 16);

    public GravityFoldDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
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
        if (!EchoPermission.canUseGravityMechanism(serverPlayer)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_gate.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.GRAVITY_ECHO,
                    "gravity_gate_locked");
            return InteractionResult.CONSUME;
        }

        openConnected(serverLevel, pos, state, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                                ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D) && isCurrentDoor(level, pos, state);
    }

    private static boolean isCurrentDoor(ServerLevel level, BlockPos pos, BlockState state) {
        return level.hasChunkAt(pos) && state.getBlock() instanceof GravityFoldDoorBlock
                && level.getBlockState(pos) == state;
    }

    private static void openConnected(ServerLevel level, BlockPos start, BlockState original,
                                      ServerPlayer player) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockState> found = new LinkedHashMap<>();
        queue.add(start.immutable());
        while (!queue.isEmpty() && found.size() < MAX_CONNECTED) {
            BlockPos current = queue.removeFirst();
            if (found.containsKey(current) || !level.hasChunkAt(current)) {
                continue;
            }
            BlockState door = level.getBlockState(current);
            if (!(door.getBlock() instanceof GravityFoldDoorBlock)) {
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
        Map<ServerLevel, Set<BlockPos>> active = OPENING.get();
        Set<BlockPos> reserved = active.computeIfAbsent(level, ignored -> new HashSet<>());
        if (found.keySet().stream().anyMatch(reserved::contains)) {
            return;
        }
        reserved.addAll(found.keySet());
        try {
            BlockState expectedStart = original;
            for (var entry : found.entrySet()) {
                if (!isCurrentInteraction(level, start, expectedStart, player)
                        || !EchoPermission.canUseGravityMechanism(player)) {
                    return;
                }
                BlockPos doorPos = entry.getKey();
                BlockState door = entry.getValue();
                if (!isCurrentDoor(level, doorPos, door)) {
                    return;
                }
                BlockState opened = door.setValue(OPEN, Boolean.TRUE);
                boolean applied;
                try {
                    applied = door == opened || level.setBlock(doorPos, opened, 3);
                } finally {
                    // 关闭任务属于已经打开的门，不能因玩家换世界或写入回调异常而遗漏。
                    if (isCurrentDoor(level, doorPos, opened)) {
                        level.scheduleTick(doorPos, opened.getBlock(), OPEN_TICKS);
                    }
                }
                if (!applied || !isCurrentDoor(level, doorPos, opened)) {
                    return;
                }
                if (doorPos.equals(start)) {
                    expectedStart = opened;
                }
            }
            if (isCurrentInteraction(level, start, expectedStart, player)
                    && EchoPermission.canUseGravityMechanism(player)) {
                level.playSound(null, start, SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        } finally {
            reserved.removeAll(found.keySet());
            if (reserved.isEmpty()) {
                active.remove(level);
            }
            if (active.isEmpty()) {
                OPENING.remove();
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isCurrentDoor(level, pos, state) || !state.getValue(OPEN)) {
            return;
        }
        BlockState closed = state.setValue(OPEN, Boolean.FALSE);
        try {
            if (level.setBlock(pos, closed, 3) && isCurrentDoor(level, pos, closed)) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.BLOCKS, 0.45F, 0.65F);
            }
        } finally {
            if (isCurrentDoor(level, pos, state)) {
                level.scheduleTick(pos, state.getBlock(), OPEN_TICKS);
            }
        }
    }
}
