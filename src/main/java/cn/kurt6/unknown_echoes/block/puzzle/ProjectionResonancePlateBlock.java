package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 仅由回响投影维持的踏板，可临时开启附近投影共鸣门并输出红石信号。 */
public final class ProjectionResonancePlateBlock extends Block {
    public static final MapCodec<ProjectionResonancePlateBlock> CODEC =
            simpleCodec(ProjectionResonancePlateBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    // 未完成的开门必须随区块保存；完成后不再在每次占用复查中扫描周围方块。
    public static final BooleanProperty PENDING_OPEN = BooleanProperty.create("pending_open");

    public static final int DOOR_RADIUS = 8;
    public static final int RECHECK_TICKS = 10;
    private static final int DOOR_VERTICAL_RADIUS = 3;
    private static final Map<ServerLevel, Long> CONTROLLER_REVISIONS = new WeakHashMap<>();
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public ProjectionResonancePlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, Boolean.FALSE)
                .setValue(PENDING_OPEN, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, PENDING_OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof EchoProjectionEntity)
                || !entity.isAlive() || entity.level() != level || !(level instanceof ServerLevel serverLevel)
                || !isCurrentPlate(serverLevel, pos, state)) {
            return;
        }
        if (!state.getValue(ACTIVE)) {
            BlockState activeState = state.setValue(ACTIVE, Boolean.TRUE)
                    .setValue(PENDING_OPEN, Boolean.TRUE);
            boolean applied;
            try {
                applied = serverLevel.setBlock(pos, activeState, Block.UPDATE_ALL);
            } finally {
                // 方块通知、开门和声音均可能中断；已提交的激活仍须复查并恢复。
                if (isCurrentPlate(serverLevel, pos, activeState)) {
                    serverLevel.scheduleTick(pos, this, RECHECK_TICKS);
                }
            }
            if (!applied || !isCurrentPlate(serverLevel, pos, activeState)) {
                return;
            }
            if (!finishOpeningDoors(serverLevel, pos, activeState)) {
                return;
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                    18, 0.42D, 0.12D, 0.42D, 0.025D);
            serverLevel.playSound(null, pos, ModSounds.BLOCK_PROJECTION_PLATE_CONFIRM.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }

    private static boolean isCurrentPlate(ServerLevel level, BlockPos pos, BlockState expected) {
        return level.hasChunkAt(pos) && level.getBlockState(pos) == expected
                && expected.getBlock() instanceof ProjectionResonancePlateBlock;
    }

    private static boolean finishOpeningDoors(ServerLevel level, BlockPos pos, BlockState activeState) {
        if (!setNearbyResonanceDoors(level, pos, true) || !isCurrentPlate(level, pos, activeState)) {
            return false;
        }
        BlockState settledState = activeState.setValue(PENDING_OPEN, Boolean.FALSE);
        return level.setBlock(pos, settledState, Block.UPDATE_CLIENTS)
                && isCurrentPlate(level, pos, settledState);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isCurrentPlate(level, pos, state)) {
            return;
        }
        boolean wasActive = state.getValue(ACTIVE);
        if (wasActive) {
            boolean projectionPresent = !level.getEntitiesOfClass(EchoProjectionEntity.class,
                    new AABB(pos).inflate(0.25D, 0.5D, 0.25D), Entity::isAlive).isEmpty();
            if (projectionPresent) {
                level.scheduleTick(pos, this, RECHECK_TICKS);
                if (state.getValue(PENDING_OPEN)) {
                    finishOpeningDoors(level, pos, state);
                }
                return;
            }
        }

        BlockState idleState = state.setValue(ACTIVE, Boolean.FALSE)
                .setValue(PENDING_OPEN, Boolean.FALSE);
        boolean doorsSettled = false;
        try {
            if (wasActive) {
                boolean reset = level.setBlock(pos, idleState, Block.UPDATE_ALL);
                if (!reset || !isCurrentPlate(level, pos, idleState)) {
                    return;
                }
            }
            doorsSettled = setNearbyResonanceDoors(level, pos, false);
            if (!doorsSettled || !isCurrentPlate(level, pos, idleState) || !wasActive) {
                return;
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                    8, 0.28D, 0.08D, 0.28D, 0.015D);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.55F, 0.65F);
        } finally {
            // 复位可能已提交而关门尚未完成；闲置态的补偿 tick 继续处理未关闭的门。
            if (!doorsSettled && (isCurrentPlate(level, pos, state)
                    || isCurrentPlate(level, pos, idleState))) {
                level.scheduleTick(pos, this, RECHECK_TICKS);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.projection_resonance_plate.hint"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos,
                            Direction direction) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        if (state != oldState && level instanceof ServerLevel serverLevel) {
            CONTROLLER_REVISIONS.merge(serverLevel, 1L, Long::sum);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel) {
            CONTROLLER_REVISIONS.merge(serverLevel, 1L, Long::sum);
            ProjectionPlateCleanupData.release(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    static boolean finishRemovedPlateCleanup(ServerLevel level, BlockPos origin) {
        if (!level.hasChunkAt(origin)) {
            return false;
        }
        BlockState state = level.getBlockState(origin);
        if (state.getBlock() instanceof ProjectionResonancePlateBlock && state.getValue(ACTIVE)) {
            return true;
        }
        return setNearbyResonanceDoors(level, origin, false);
    }

    private static boolean setNearbyResonanceDoors(ServerLevel level, BlockPos origin, boolean open) {
        BlockState controllerState = level.getBlockState(origin);
        boolean complete = true;
        ControllerLookup otherControllers = open ? null : new ControllerLookup(level, origin);
        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-DOOR_RADIUS, -DOOR_VERTICAL_RADIUS, -DOOR_RADIUS),
                origin.offset(DOOR_RADIUS, DOOR_VERTICAL_RADIUS, DOOR_RADIUS))) {
            if (!level.hasChunkAt(cursor)) {
                complete = false;
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.PROJECTION_RESONANCE_DOOR.get())) {
                if (!open && !state.getValue(ProjectionResonanceDoorBlock.OPEN)) {
                    continue;
                }
                if (!open && isControlledByAnotherPlate(cursor, otherControllers)) {
                    continue;
                }
                complete &= setDoorState(level, cursor, open, origin, controllerState);
                if (!level.hasChunkAt(origin) || level.getBlockState(origin) != controllerState) {
                    return false;
                }
                continue;
            }
            if (!state.is(Blocks.IRON_DOOR)
                    || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    != DoubleBlockHalf.LOWER) {
                continue;
            }
            BlockPos upperPos = cursor.above();
            if (!open && isHeldOpen(level, cursor, upperPos, otherControllers)) {
                continue;
            }
            complete &= setDoorState(level, cursor, open, origin, controllerState);
            if (open || !isHeldOpen(level, cursor, upperPos, otherControllers)) {
                complete &= setDoorState(level, upperPos, open, origin, controllerState);
            }
            if (!level.hasChunkAt(origin) || level.getBlockState(origin) != controllerState) {
                return false;
            }
        }
        return complete;
    }

    private static boolean isHeldOpen(ServerLevel level, BlockPos cursor, BlockPos upperPos,
                                      ControllerLookup otherControllers) {
        return level.hasNeighborSignal(cursor)
                || upperPos != null && level.hasNeighborSignal(upperPos)
                || isControlledByAnotherPlate(cursor, otherControllers);
    }

    /** 只在控制状态变化后刷新；未遇到需要关门的位置时不扫描其他踏板。 */
    private static final class ControllerLookup {
        private final ServerLevel level;
        private final BlockPos origin;
        private long revision;
        private List<BlockPos> positions;

        private ControllerLookup(ServerLevel level, BlockPos origin) {
            this.level = level;
            this.origin = origin.immutable();
        }

        private List<BlockPos> positions() {
            long currentRevision = CONTROLLER_REVISIONS.getOrDefault(level, 0L);
            if (positions == null || revision != currentRevision) {
                positions = findOtherActiveControllers(level, origin);
                revision = currentRevision;
            }
            return positions;
        }
    }

    private static List<BlockPos> findOtherActiveControllers(ServerLevel level, BlockPos origin) {
        List<BlockPos> controllers = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-DOOR_RADIUS * 2, -DOOR_VERTICAL_RADIUS * 2, -DOOR_RADIUS * 2),
                origin.offset(DOOR_RADIUS * 2, DOOR_VERTICAL_RADIUS * 2, DOOR_RADIUS * 2))) {
            if (cursor.equals(origin)) {
                continue;
            }
            if (!level.hasChunkAt(cursor)) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() instanceof ProjectionResonancePlateBlock
                    && state.getValue(ACTIVE)) {
                controllers.add(cursor.immutable());
            }
        }
        return controllers;
    }

    private static boolean isControlledByAnotherPlate(BlockPos doorPos,
                                                       ControllerLookup controllers) {
        for (BlockPos controller : controllers.positions()) {
            if (Math.abs(controller.getX() - doorPos.getX()) <= DOOR_RADIUS
                    && Math.abs(controller.getY() - doorPos.getY()) <= DOOR_VERTICAL_RADIUS
                    && Math.abs(controller.getZ() - doorPos.getZ()) <= DOOR_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static boolean setDoorState(ServerLevel level, BlockPos pos, boolean open,
                                       BlockPos origin, BlockState controllerState) {
        if (!level.hasChunkAt(origin) || level.getBlockState(origin) != controllerState
                || !level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockState target = state;
        if (state.is(ModBlocks.PROJECTION_RESONANCE_DOOR.get())
                && state.getValue(ProjectionResonanceDoorBlock.OPEN) != open) {
            target = state.setValue(ProjectionResonanceDoorBlock.OPEN, open);
        } else if (state.is(Blocks.IRON_DOOR)
                && state.getValue(BlockStateProperties.OPEN) != open) {
            target = state.setValue(BlockStateProperties.OPEN, open);
        }
        if (target != state) {
            boolean applied = level.setBlock(pos, target, Block.UPDATE_ALL);
            if (!applied || !level.hasChunkAt(pos) || level.getBlockState(pos) != target) {
                return false;
            }
        }
        return level.hasChunkAt(origin) && level.getBlockState(origin) == controllerState;
    }
}
