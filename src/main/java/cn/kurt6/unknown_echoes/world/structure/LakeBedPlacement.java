package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.QuartPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 镜湖湖底结构的放置校验。
 *
 * 背景:镜湖群系按大陆性参数划片,但群系判定与地形噪声只在深水区严格对应——
 * 群系边缘存在大段高于海平面的"旱地镜湖"。"沉没"类结构(沉没圣殿/镜湖神殿/倒影回廊)
 * 原先只对齐 OCEAN_FLOOR_WG,不校验水体,落在旱地段就会整座干地生成,Boss 也跟着旱地出生。
 *
 * 校验方式:先用群系预判快速出局(气候采样远比噪声柱便宜,否则 /locate 全图扫描会把
 * 服务端主线程拖过看门狗 60 秒上限),再用噪声基准柱(getBaseColumn,纯噪声采样,
 * 不触发区块生成)从海平面向下数连续水深,要求结构足印的中心与四角都达到要求深度。
 */
final class LakeBedPlacement {

    private LakeBedPlacement() {
    }

    /**
     * 结构足印是否完全没入湖水。
     *
     * @param footprint   结构边长(方形足印,锚点为区块起点)
     * @param centerDepth 中心要求的最小水深(应 ≥ 结构高度 + 1,保证顶部不出水)
     * @param edgeDepth   四角要求的最小水深(容忍湖底坡度,可低于中心)
     */
    static boolean isSubmerged(Structure.GenerationContext context,
                               int footprint, int centerDepth, int edgeDepth) {
        int minX = context.chunkPos().getMinBlockX();
        int minZ = context.chunkPos().getMinBlockZ();
        int half = footprint / 2;
        int max = footprint - 1;
        int seaLevel = context.chunkGenerator().getSeaLevel();
        // 群系预判:中心不是本结构允许的群系直接出局,把昂贵的噪声柱采样留给镜湖候选点
        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(minX + half), QuartPos.fromBlock(seaLevel),
                QuartPos.fromBlock(minZ + half), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return false;
        }
        return waterDepth(context, minX + half, minZ + half) >= centerDepth
                && waterDepth(context, minX, minZ) >= edgeDepth
                && waterDepth(context, minX + max, minZ) >= edgeDepth
                && waterDepth(context, minX, minZ + max) >= edgeDepth
                && waterDepth(context, minX + max, minZ + max) >= edgeDepth;
    }

    /**
     * /locate 友好的足印预判:只采样中心群系与五个高度图点,不扫整列方块状态。
     * 严格的连续水方块校验仍在 Piece.postProcess 执行,这里用于过滤会生成空现场的候选点。
     */
    static boolean hasSubmergedFootprint(Structure.GenerationContext context,
                                         int footprint, int centerDepth, int edgeDepth) {
        int minX = context.chunkPos().getMinBlockX();
        int minZ = context.chunkPos().getMinBlockZ();
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

    /**
     * /locate 友好的中心点预判:只采样中心群系与两个高度图,不扫整列方块状态。
     * 保留给非主线小结构使用;主线水下结构优先用 hasSubmergedFootprint。
     */
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

    /** 从海平面向下数连续水方块数;遇到非水(湖底/旱地)即停。 */
    private static int waterDepth(Structure.GenerationContext context, int x, int z) {
        NoiseColumn column = context.chunkGenerator()
                .getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int minY = context.heightAccessor().getMinBuildHeight();
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
