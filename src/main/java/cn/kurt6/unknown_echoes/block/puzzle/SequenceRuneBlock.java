package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 顺序符文柱:ORDER(0-7)是表盘记号(点数 1-8),LIT 表示已点亮。 */
public class SequenceRuneBlock extends Block {
    public static final MapCodec<SequenceRuneBlock> CODEC = simpleCodec(SequenceRuneBlock::new);
    public static final IntegerProperty ORDER = IntegerProperty.create("order", 0, 7);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** 模型原顶面的朝向；缺省向上以兼容既有输入柱。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public static final int CORE_SEARCH_RADIUS = 10;

    public SequenceRuneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ORDER, 0).setValue(LIT, Boolean.FALSE).setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORDER, LIT, FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        PuzzleCoreBlockEntity core = findCore(level, pos);
        if (state.getValue(LIT) && (core == null || !core.acceptsLitRuneInput())) {
            return InteractionResult.CONSUME;
        }
        if (core == null) {
            PlayerSoundFeedback.deny(player);
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.sequence_rune.silent"), true);
            return InteractionResult.CONSUME;
        }
        core.tryActivate(state.getValue(ORDER), pos,
                player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        return InteractionResult.CONSUME;
    }

    /** 步纹符文与顺序符文共用:在附近寻找谜题核心或音序核心。 */
    static PuzzleCoreBlockEntity findCore(Level level, BlockPos origin) {
        PuzzleCoreBlockEntity nearest = null;
        BlockPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;
        int minChunkX = (origin.getX() - CORE_SEARCH_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + CORE_SEARCH_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - CORE_SEARCH_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + CORE_SEARCH_RADIUS) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (var blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!(blockEntity instanceof PuzzleCoreBlockEntity core)) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    if (Math.abs((long) pos.getX() - origin.getX()) > CORE_SEARCH_RADIUS
                            || Math.abs((long) pos.getY() - origin.getY()) > 4
                            || Math.abs((long) pos.getZ() - origin.getZ()) > CORE_SEARCH_RADIUS) {
                        continue;
                    }
                    BlockState state = blockEntity.getBlockState();
                    if (core.isRemoved() || core.getLevel() != level
                            || level.getBlockState(pos) != state
                            || !state.is(ModBlocks.PUZZLE_CORE.get())
                            && !state.is(ModBlocks.CHIME_ECHO_CORE.get())) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    // 等距沿用原生坐标扫描的 Z、Y、X 顺序，不能依赖区块实体映射顺序。
                    if (distance < nearestDistance || distance == nearestDistance
                            && earlierCore(pos, nearestPos)) {
                        nearest = core;
                        nearestPos = pos;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    private static boolean earlierCore(BlockPos pos, BlockPos other) {
        return other == null || pos.getZ() < other.getZ()
                || pos.getZ() == other.getZ() && (pos.getY() < other.getY()
                || pos.getY() == other.getY() && pos.getX() < other.getX());
    }
}
