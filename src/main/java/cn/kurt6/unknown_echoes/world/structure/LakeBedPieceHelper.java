package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class LakeBedPieceHelper {

    /** 水下结构允许写入的最低海床高度;避免结构底座进入岩浆区或覆盖基岩层。 */
    public static final int MIN_SAFE_LAKE_FLOOR_Y = 8;
    /** 定位阶段最多采样 5x5 个高度列,兼顾跨区块地形与 /locate 性能。 */
    private static final int FLOOR_SAMPLE_GRID = 5;

    private LakeBedPieceHelper() {
    }

    /** 用生成器高度采样校验水深,不读取周边区块方块状态。 */
    public static boolean checkWaterDepthByBaseHeight(WorldGenLevel level, ChunkGenerator generator,
                                                      int minX, int minZ, int width, int depth,
                                                      int centerDepth, int edgeDepth) {
        int centerX = minX + width / 2;
        int centerZ = minZ + depth / 2;
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();

        return hasWaterColumnByBaseHeight(level, generator, randomState, seaLevel,
                        centerX, centerZ, centerDepth)
                && hasWaterColumnByBaseHeight(level, generator, randomState, seaLevel,
                        minX, minZ, edgeDepth)
                && hasWaterColumnByBaseHeight(level, generator, randomState, seaLevel,
                        maxX, minZ, edgeDepth)
                && hasWaterColumnByBaseHeight(level, generator, randomState, seaLevel,
                        minX, maxZ, edgeDepth)
                && hasWaterColumnByBaseHeight(level, generator, randomState, seaLevel,
                        maxX, maxZ, edgeDepth);
    }

    /** 均匀采样整座足印,得到跨区块稳定的统一海床基准。 */
    public static int averageOceanFloorByBaseHeight(Structure.GenerationContext context,
                                                     int minX, int minZ, int width, int depth) {
        return sampledOceanFloorAverage(minX, minZ, width, depth,
                (x, z) -> context.chunkGenerator().getBaseHeight(x, z,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
    }

    /** postProcess 兜底采样,同样不依赖当前区块交集。 */
    public static int averageOceanFloorByBaseHeight(WorldGenLevel level, ChunkGenerator generator,
                                                     int minX, int minZ, int width, int depth) {
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        return sampledOceanFloorAverage(minX, minZ, width, depth,
                (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        level, randomState));
    }

    static int sampledOceanFloorAverage(int minX, int minZ, int width, int depth,
                                                 java.util.function.IntBinaryOperator heightSampler) {
        int xSamples = Math.min(FLOOR_SAMPLE_GRID, Math.max(0, width));
        int zSamples = Math.min(FLOOR_SAMPLE_GRID, Math.max(0, depth));
        long total = 0L;
        int count = 0;
        for (int zIndex = 0; zIndex < zSamples; zIndex++) {
            int z = minZ + sampleOffset(depth, zIndex, zSamples);
            for (int xIndex = 0; xIndex < xSamples; xIndex++) {
                int x = minX + sampleOffset(width, xIndex, xSamples);
                total += heightSampler.applyAsInt(x, z);
                count++;
            }
        }
        return count == 0 ? Integer.MIN_VALUE : (int) Math.floorDiv(total, count);
    }

    private static int sampleOffset(int extent, int index, int sampleCount) {
        if (sampleCount <= 1 || extent <= 1) {
            return 0;
        }
        return (int) Math.floorDiv((long) (extent - 1) * index, sampleCount - 1L);
    }

    /** 水下结构不能落入世界底部的熔岩/基岩保护区。 */
    public static boolean isSafeLakeFloor(int floorY) {
        return floorY >= MIN_SAFE_LAKE_FLOOR_Y;
    }

    /** 结构移动后重新求当前区块交集;避免首次移动后继续使用旧的 chunkBox。 */
    public static BoundingBox intersection(BoundingBox requested, BoundingBox structure) {
        int minX = Math.max(requested.minX(), structure.minX());
        int minY = Math.max(requested.minY(), structure.minY());
        int minZ = Math.max(requested.minZ(), structure.minZ());
        int maxX = Math.min(requested.maxX(), structure.maxX());
        int maxY = Math.min(requested.maxY(), structure.maxY());
        int maxZ = Math.min(requested.maxZ(), structure.maxZ());
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return null;
        }
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean hasWaterColumnByBaseHeight(WorldGenLevel level, ChunkGenerator generator,
                                                      RandomState randomState, int seaLevel,
                                                      int x, int z, int requiredDepth) {
        int surfaceY = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                level, randomState);
        if (surfaceY > seaLevel + 1) {
            return false;
        }
        int floorY = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                level, randomState);
        return Math.min(seaLevel, surfaceY) - floorY >= requiredDepth;
    }
}
