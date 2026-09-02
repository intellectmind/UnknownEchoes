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
import net.minecraft.world.phys.BlockHitResult;

/**
 * 对页书板:配对谜题的翻页输入。右键把自己的位置报给附近的对页核心, 由核心判定同源与否;LIT 表示已归位。
 * 答案不写入方块状态。
 */
public class FoldpageTileBlock extends Block {
    public static final MapCodec<FoldpageTileBlock> CODEC = simpleCodec(FoldpageTileBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final int CORE_SEARCH_RADIUS = 10;

    public FoldpageTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        FoldpagePairCoreBlockEntity core = findCore(level, pos);
        if (core == null) {
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.7F, 0.5F);
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.foldpage.silent"), true);
            return InteractionResult.CONSUME;
        }
        core.flip(pos, player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        return InteractionResult.CONSUME;
    }

    private static FoldpagePairCoreBlockEntity findCore(Level level, BlockPos origin) {
        FoldpagePairCoreBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CORE_SEARCH_RADIUS, -4, -CORE_SEARCH_RADIUS),
                origin.offset(CORE_SEARCH_RADIUS, 4, CORE_SEARCH_RADIUS))) {
            if (level.hasChunkAt(pos)
                    && level.getBlockState(pos).is(ModBlocks.FOLDPAGE_PAIR_CORE.get())
                    && level.getBlockEntity(pos) instanceof FoldpagePairCoreBlockEntity core
                    && core.ownsTile(origin)) {
                double distance = pos.distSqr(origin);
                if (distance < nearestDistance) {
                    nearest = core;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }
}
