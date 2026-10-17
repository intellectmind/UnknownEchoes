package cn.kurt6.unknown_echoes.block.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class WonderPuzzleDialBlock extends Block {
    public static final MapCodec<WonderPuzzleDialBlock> CODEC = simpleCodec(WonderPuzzleDialBlock::new);
    public static final IntegerProperty FORM = IntegerProperty.create("form", 0, 3);
    public static final IntegerProperty VALUE = IntegerProperty.create("value", 0, 8);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public WonderPuzzleDialBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORM, 0).setValue(VALUE, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORM, VALUE, LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        // 只遍历输入周围已加载区块的方块实体，避免强制加载相邻奇观。
        for (int x = (pos.getX() - 8) >> 4; x <= (pos.getX() + 8) >> 4; x++) {
            for (int z = (pos.getZ() - 8) >> 4; z <= (pos.getZ() + 8) >> 4; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (var entity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (entity instanceof WonderPuzzleCoreBlockEntity core && core.accepts(pos)) {
                        core.activate(pos, serverPlayer);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        return InteractionResult.CONSUME;
    }

}
