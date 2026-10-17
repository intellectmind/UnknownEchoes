package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.function.IntBinaryOperator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

final class LakeBedPlacement {

    private LakeBedPlacement() {
    }

    static boolean isSubmerged(Structure.GenerationContext context,
                               int footprint, int centerDepth, int edgeDepth) {
        return hasSubmergedFootprint(context, footprint, centerDepth, edgeDepth);
    }

    /** /locate 友好的足印预判:只采样中心群系与五个高度图点,不扫整列方块状态。 */
    static boolean hasSubmergedFootprint(Structure.GenerationContext context,
                                         int footprint, int centerDepth, int edgeDepth) {
        int minX = context.chunkPos().getMinBlockX();
        int minZ = context.chunkPos().getMinBlockZ();
        return hasSubmergedFootprint(context, minX, minZ, footprint, centerDepth, edgeDepth);
    }

    /** /locate 友好的足印预判,支持按旋转后的真实结构原点采样。 */
    static boolean hasSubmergedFootprint(Structure.GenerationContext context,
                                         int minX, int minZ, int footprint,
                                         int centerDepth, int edgeDepth) {
        if (!hasValidBiome(context, minX + footprint / 2, minZ + footprint / 2)) {
            return false;
        }
        return hasWaterAtFootprint(context, minX, minZ, footprint, footprint, centerDepth, edgeDepth,
                (x, z) -> sampleOceanFloor(context, x, z));
    }

    // 不合格候选返回 Integer.MIN_VALUE，最低湖床高度仍由调用方检查。
    static int findOceanFloor(Structure.GenerationContext context, int minX, int minZ,
                              int width, int depth, int centerDepth, int edgeDepth) {
        if (!hasValidBiome(context, minX + width / 2, minZ + depth / 2)) {
            return Integer.MIN_VALUE;
        }
        // 缓存仅属于本次候选，不能跨种子、维度或生成器复用。
        var heights = new Long2IntOpenHashMap(5);
        IntBinaryOperator sample = (x, z) -> {
            long key = ChunkPos.asLong(x, z);
            if (heights.containsKey(key)) {
                return heights.get(key);
            }
            int height = sampleOceanFloor(context, x, z);
            // 只缓存预判的五列，平均高度网格内的其他列只访问一次。
            if (heights.size() < 5) {
                heights.put(key, height);
            }
            return height;
        };
        if (!hasWaterAtFootprint(context, minX, minZ, width, depth, centerDepth, edgeDepth, sample)) {
            return Integer.MIN_VALUE;
        }
        return LakeBedPieceHelper.sampledOceanFloorAverage(minX, minZ, width, depth, sample);
    }

    private static boolean hasValidBiome(Structure.GenerationContext context, int x, int z) {
        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(context.chunkGenerator().getSeaLevel()),
                QuartPos.fromBlock(z), context.randomState().sampler());
        return context.validBiome().test(biome);
    }

    private static int sampleOceanFloor(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
    }

    private static boolean hasWaterAtFootprint(Structure.GenerationContext context,
                                              int minX, int minZ, int width, int depth,
                                              int centerDepth, int edgeDepth, IntBinaryOperator floorSampler) {
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;
        return hasWaterColumnByHeightmap(context, minX + width / 2, minZ + depth / 2, centerDepth, floorSampler)
                && hasWaterColumnByHeightmap(context, minX, minZ, edgeDepth, floorSampler)
                && hasWaterColumnByHeightmap(context, maxX, minZ, edgeDepth, floorSampler)
                && hasWaterColumnByHeightmap(context, minX, maxZ, edgeDepth, floorSampler)
                && hasWaterColumnByHeightmap(context, maxX, maxZ, edgeDepth, floorSampler);
    }

    /** /locate 友好的中心点预判:只采样中心群系与两个高度图,不扫整列方块状态。 */
    static boolean hasSubmergedCenter(Structure.GenerationContext context,
                                      int footprint, int centerDepth) {
        int minX = context.chunkPos().getMinBlockX();
        int minZ = context.chunkPos().getMinBlockZ();
        int half = footprint / 2;
        int centerX = minX + half;
        int centerZ = minZ + half;
        int seaLevel = context.chunkGenerator().getSeaLevel();

        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX), QuartPos.fromBlock(seaLevel),
                QuartPos.fromBlock(centerZ), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return false;
        }

        int surfaceY = context.chunkGenerator().getBaseHeight(centerX, centerZ,
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (surfaceY > seaLevel + 1) {
            return false;
        }
        int floorY = context.chunkGenerator().getBaseHeight(centerX, centerZ,
                Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        return Math.min(seaLevel, surfaceY) - floorY >= centerDepth;
    }

    private static boolean hasWaterColumnByHeightmap(Structure.GenerationContext context,
                                                     int x, int z, int requiredDepth, IntBinaryOperator floorSampler) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int surfaceY = context.chunkGenerator().getBaseHeight(x, z,
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (surfaceY > seaLevel + 1) {
            return false;
        }
        int floorY = floorSampler.applyAsInt(x, z);
        return Math.min(seaLevel, surfaceY) - floorY >= requiredDepth;
    }

}
