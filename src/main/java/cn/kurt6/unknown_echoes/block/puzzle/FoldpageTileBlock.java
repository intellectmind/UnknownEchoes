package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        if (!(level instanceof ServerLevel serverLevel)) return null;
        FoldpagePairCoreBlockEntity nearest = null;
        BlockPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int x = (origin.getX() - CORE_SEARCH_RADIUS) >> 4;
             x <= (origin.getX() + CORE_SEARCH_RADIUS) >> 4; x++) {
            for (int z = (origin.getZ() - CORE_SEARCH_RADIUS) >> 4;
                 z <= (origin.getZ() + CORE_SEARCH_RADIUS) >> 4; z++) {
                var chunk = serverLevel.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof FoldpagePairCoreBlockEntity core)) continue;
                    BlockPos pos = core.getBlockPos();
                    if (Math.abs(pos.getX() - origin.getX()) > CORE_SEARCH_RADIUS
                            || Math.abs(pos.getY() - origin.getY()) > 4
                            || Math.abs(pos.getZ() - origin.getZ()) > CORE_SEARCH_RADIUS
                            || !core.ownsTile(origin)
                            || !level.getBlockState(pos).is(ModBlocks.FOLDPAGE_PAIR_CORE.get())) continue;
                    double distance = pos.distSqr(origin);
                    // 与原体积遍历相同：等距时按 Z、Y、X 顺序选择，避免相邻核心串线。
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
