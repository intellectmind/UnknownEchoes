package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 顺序符文柱:谜题元素。ORDER(0-7)是柱身记号(点数 1-8),LIT 表示已点亮。 */
public class SequenceRuneBlock extends Block {
    public static final MapCodec<SequenceRuneBlock> CODEC = simpleCodec(SequenceRuneBlock::new);
    public static final IntegerProperty ORDER = IntegerProperty.create("order", 0, 7);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final int CORE_SEARCH_RADIUS = 10;

    public SequenceRuneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ORDER, 0).setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORDER, LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT)) {
            return InteractionResult.CONSUME;
        }

        PuzzleCoreBlockEntity core = findCore(level, pos);
        if (core == null) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
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
                    if (!state.is(ModBlocks.PUZZLE_CORE.get())
                            && !state.is(ModBlocks.CHIME_ECHO_CORE.get())) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    if (distance < nearestDistance) {
                        nearest = core;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }
}
