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

/**
 * 顺序符文柱:谜题元素。ORDER(0-7)是柱身记号(点数 1-8),LIT 表示已点亮。
 * 右键时把自己的 ORDER 报给附近的谜题核心,由核心按自己的乱序序列判定对错。
 * 墙上高处嵌着的常亮柱是顺序提示,不参与判定(核心只重置低处的柱)。
 */
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
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.sequence_rune.lit"), true);
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

    private static PuzzleCoreBlockEntity findCore(Level level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CORE_SEARCH_RADIUS, -4, -CORE_SEARCH_RADIUS),
                origin.offset(CORE_SEARCH_RADIUS, 4, CORE_SEARCH_RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.PUZZLE_CORE.get())
                    && level.getBlockEntity(pos) instanceof PuzzleCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }
}
