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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** 重力折叠门：重力回响可短时稳定整组门体，物理绕行不授予任何进度。 */
public class GravityFoldDoorBlock extends Block {
    public static final MapCodec<GravityFoldDoorBlock> CODEC = simpleCodec(GravityFoldDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final int OPEN_TICKS = 140;
    private static final int MAX_CONNECTED = 32;
    private static final VoxelShape OPEN_SHAPE = Block.box(0, 14, 0, 16, 16, 16);

    public GravityFoldDoorBlock(Properties properties) {
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
        return state.getValue(OPEN) ? OPEN_SHAPE : Shapes.block();
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
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
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

        openConnected((ServerLevel) level, pos);
        serverPlayer.displayClientMessage(Component.translatable(
                "message.unknown_echoes.gravity_gate.opened"), true);
        return InteractionResult.CONSUME;
    }

    private static void openConnected(ServerLevel level, BlockPos start) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> found = new HashSet<>();
        queue.add(start.immutable());
        while (!queue.isEmpty() && found.size() < MAX_CONNECTED) {
            BlockPos current = queue.removeFirst();
            if (found.contains(current)
                    || !(level.getBlockState(current).getBlock() instanceof GravityFoldDoorBlock)) {
                continue;
            }
            found.add(current);
            for (Direction direction : Direction.values()) {
                queue.addLast(current.relative(direction).immutable());
            }
        }
        for (BlockPos doorPos : found) {
            BlockState door = level.getBlockState(doorPos);
            level.setBlock(doorPos, door.setValue(OPEN, Boolean.TRUE), 3);
            level.scheduleTick(doorPos, door.getBlock(), OPEN_TICKS);
        }
        level.playSound(null, start, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            level.setBlock(pos, state.setValue(OPEN, Boolean.FALSE), 3);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.45F, 0.65F);
        }
    }
}
