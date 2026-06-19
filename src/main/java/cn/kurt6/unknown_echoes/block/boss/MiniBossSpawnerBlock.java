package cn.kurt6.unknown_echoes.block.boss;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;

/**
 * Mini Boss 场地计时器(技术方块,V0.6A Mini Boss 公共框架):
 * 埋在守护者场地内,负责首次生成与击败后的场地重开(MINIBOSS_ARENA_REOPEN 配置)。
 * 不可见、不掉落、无 BlockItem(只由结构生成或管理员 /setblock 放置);强度 -1 防破坏。
 */
public class MiniBossSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<MiniBossSpawnerBlock> CODEC = simpleCodec(MiniBossSpawnerBlock::new);

    public MiniBossSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiniBossSpawnerBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MINIBOSS_SPAWNER.get(),
                MiniBossSpawnerBlockEntity::serverTick);
    }
}
