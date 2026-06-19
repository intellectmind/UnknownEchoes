package cn.kurt6.unknown_echoes.block.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 共鸣烛:沉默祭坛四角的机制方块(设计文档 10.4.2 沉默祭司)。
 * 战斗中打碎可缩短静默领域持续时间——是少数"鼓励玩家破坏"的机关方块,
 * 因此低强度、可徒手快拆、不进 critical_blocks;场地重开时由 MiniBossSpawnerBlockEntity 补回。
 */
public class ResonanceCandleBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);

    public ResonanceCandleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 烛火与微弱声纹:提示它与"声音"有关
        level.addParticle(ParticleTypes.SMALL_FLAME,
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, 0.01, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.NOTE,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    random.nextDouble(), 0.0, 0.0);
        }
    }
}
