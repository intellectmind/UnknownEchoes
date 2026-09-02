package cn.kurt6.unknown_echoes.block.puzzle;

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

/** 仅由回响投影维持的踏板，可临时开启附近投影共鸣门并输出红石信号。 */
public final class ProjectionResonancePlateBlock extends Block {
    public static final MapCodec<ProjectionResonancePlateBlock> CODEC =
            simpleCodec(ProjectionResonancePlateBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final int DOOR_RADIUS = 8;
    public static final int RECHECK_TICKS = 10;
    private static final int DOOR_VERTICAL_RADIUS = 3;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public ProjectionResonancePlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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
        if (level.isClientSide || !(entity instanceof EchoProjectionEntity)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        if (!state.getValue(ACTIVE)) {
            serverLevel.setBlock(pos, state.setValue(ACTIVE, Boolean.TRUE), Block.UPDATE_ALL);
            setNearbyResonanceDoors(serverLevel, pos, true);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                    18, 0.42D, 0.12D, 0.42D, 0.025D);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.9F, 1.45F);
            serverLevel.scheduleTick(pos, this, RECHECK_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        boolean projectionPresent = !level.getEntitiesOfClass(EchoProjectionEntity.class,
                new AABB(pos).inflate(0.25D, 0.5D, 0.25D), Entity::isAlive).isEmpty();
        if (projectionPresent) {
            level.scheduleTick(pos, this, RECHECK_TICKS);
            return;
        }

        level.setBlock(pos, state.setValue(ACTIVE, Boolean.FALSE), Block.UPDATE_ALL);
        setNearbyResonanceDoors(level, pos, false);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                8, 0.28D, 0.08D, 0.28D, 0.015D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.55F, 0.65F);
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
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(ACTIVE)
                && level instanceof ServerLevel serverLevel) {
            setNearbyResonanceDoors(serverLevel, pos, false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void setNearbyResonanceDoors(ServerLevel level, BlockPos origin, boolean open) {
        List<BlockPos> otherControllers = open
                ? List.of()
                : findOtherActiveControllers(level, origin);
        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-DOOR_RADIUS, -DOOR_VERTICAL_RADIUS, -DOOR_RADIUS),
                origin.offset(DOOR_RADIUS, DOOR_VERTICAL_RADIUS, DOOR_RADIUS))) {
            if (!level.hasChunkAt(cursor)) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.PROJECTION_RESONANCE_DOOR.get())) {
                if (!open && (level.hasNeighborSignal(cursor)
                        || isControlledByAnotherPlate(cursor, otherControllers))) {
                    continue;
                }
                setDoorState(level, cursor, open);
                continue;
            }
            if (!state.is(Blocks.IRON_DOOR)
                    || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    != DoubleBlockHalf.LOWER) {
                continue;
            }
            BlockPos upperPos = cursor.above();
            if (!open && (level.hasNeighborSignal(cursor)
                    || level.hasNeighborSignal(upperPos)
                    || isControlledByAnotherPlate(cursor, otherControllers))) {
                continue;
            }
            setDoorState(level, cursor, open);
            setDoorState(level, upperPos, open);
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
                                                       List<BlockPos> controllers) {
        for (BlockPos controller : controllers) {
            if (Math.abs(controller.getX() - doorPos.getX()) <= DOOR_RADIUS
                    && Math.abs(controller.getY() - doorPos.getY()) <= DOOR_VERTICAL_RADIUS
                    && Math.abs(controller.getZ() - doorPos.getZ()) <= DOOR_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static void setDoorState(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.PROJECTION_RESONANCE_DOOR.get())
                && state.getValue(ProjectionResonanceDoorBlock.OPEN) != open) {
            level.setBlock(pos, state.setValue(ProjectionResonanceDoorBlock.OPEN, open), Block.UPDATE_ALL);
        } else if (state.is(Blocks.IRON_DOOR)
                && state.getValue(BlockStateProperties.OPEN) != open) {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_ALL);
        }
    }
}
