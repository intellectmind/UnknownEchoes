package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * 镜湖湖底结构 Piece 的水深校验辅助方法。
 *
 * 背景:水深校验从 findGenerationPoint 推迟到 postProcess,避免 /locate 命令
 * 对每个候选区块都跑昂贵的 getBaseColumn 导致服务端看门狗超时。
 */
public final class LakeBedPieceHelper {

    private LakeBedPieceHelper() {
    }

    /**
     * 从海平面向下数连续水方块数;遇到非水即停。
     * 在 postProcess 时调用(已生成区块,用 level.getBlockState)。
     */
    public static int waterDepth(WorldGenLevel level, ChunkGenerator generator, int x, int z) {
        int seaLevel = generator.getSeaLevel();
        int minY = level.getMinBuildHeight();
        int depth = 0;
        for (int y = seaLevel - 1; y > minY; y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.getFluidState().is(FluidTags.WATER)) {
                break;
            }
            depth++;
        }
        return depth;
    }

    /**
     * 校验结构足印(方形)的中心与四角是否达到要求水深。
     *
     * @param minX        结构包围盒最小 X
     * @param minZ        结构包围盒最小 Z
     * @param width       结构宽度
     * @param depth       结构深度
     * @param centerDepth 中心要求的最小水深
     * @param edgeDepth   四角要求的最小水深
     */
    public static boolean checkWaterDepth(WorldGenLevel level, ChunkGenerator generator,
                                          int minX, int minZ, int width, int depth,
                                          int centerDepth, int edgeDepth) {
        int centerX = minX + width / 2;
        int centerZ = minZ + depth / 2;
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        return waterDepth(level, generator, centerX, centerZ) >= centerDepth
                && waterDepth(level, generator, minX, minZ) >= edgeDepth
                && waterDepth(level, generator, maxX, minZ) >= edgeDepth
                && waterDepth(level, generator, minX, maxZ) >= edgeDepth
                && waterDepth(level, generator, maxX, maxZ) >= edgeDepth;
    }

    /**
     * 用生成器噪声柱校验水深,不读取周边区块方块状态。
     * 大型结构跨区块时,postProcess 早期读取远端区块可能得到未完整填充的状态,
     * 导致 /locate 找得到但现场被误判为落旱地并整座取消。
     */
    public static boolean checkWaterDepthByNoise(WorldGenLevel level, ChunkGenerator generator,
                                                 int minX, int minZ, int width, int depth,
                                                 int centerDepth, int edgeDepth) {
        int centerX = minX + width / 2;
        int centerZ = minZ + depth / 2;
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        return noiseWaterDepth(level, generator, centerX, centerZ) >= centerDepth
                && noiseWaterDepth(level, generator, minX, minZ) >= edgeDepth
                && noiseWaterDepth(level, generator, maxX, minZ) >= edgeDepth
                && noiseWaterDepth(level, generator, minX, maxZ) >= edgeDepth
                && noiseWaterDepth(level, generator, maxX, maxZ) >= edgeDepth;
    }

    private static int noiseWaterDepth(WorldGenLevel level, ChunkGenerator generator, int x, int z) {
        NoiseColumn column = generator.getBaseColumn(
                x, z, level, level.getLevel().getChunkSource().randomState());
        int seaLevel = generator.getSeaLevel();
        int minY = level.getMinBuildHeight();
        int depth = 0;
        for (int y = seaLevel - 1; y > minY; y--) {
            BlockState state = column.getBlock(y);
            if (!state.getFluidState().is(FluidTags.WATER)) {
                break;
            }
            depth++;
        }
        return depth;
    }
}
