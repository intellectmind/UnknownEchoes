package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.QuartPos;
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
        int half = footprint / 2;
        int max = footprint - 1;
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int centerX = minX + half;
        int centerZ = minZ + half;

        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX), QuartPos.fromBlock(seaLevel),
                QuartPos.fromBlock(centerZ), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return false;
        }

        return hasWaterColumnByHeightmap(context, centerX, centerZ, centerDepth)
                && hasWaterColumnByHeightmap(context, minX, minZ, edgeDepth)
                && hasWaterColumnByHeightmap(context, minX + max, minZ, edgeDepth)
                && hasWaterColumnByHeightmap(context, minX, minZ + max, edgeDepth)
                && hasWaterColumnByHeightmap(context, minX + max, minZ + max, edgeDepth);
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
        return seaLevel - floorY >= centerDepth;
    }

    private static boolean hasWaterColumnByHeightmap(Structure.GenerationContext context,
                                                     int x, int z, int requiredDepth) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int surfaceY = context.chunkGenerator().getBaseHeight(x, z,
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (surfaceY > seaLevel + 1) {
            return false;
        }
        int floorY = context.chunkGenerator().getBaseHeight(x, z,
                Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        return seaLevel - floorY >= requiredDepth;
    }

}
