package cn.kurt6.unknown_echoes.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 判断一片水平范围的区块是否全部已加载。体积扫描、机关点位发现与场地事务开始前必须先问这里:
 * 未加载时 getBlockState 读到空气,会把"机关不存在"当成结论。
 */
public final class LoadedAreaCheck {

    private LoadedAreaCheck() {
    }

    /** 以 center 为中心、水平半径 radius 的区块是否全部已加载。垂直范围不参与判断。 */
    public static boolean horizontalRadiusLoaded(Level level, BlockPos center, int radius) {
        int safeRadius = Math.max(0, radius);
        return chunkRangeLoaded(level,
                center.getX() - safeRadius, center.getX() + safeRadius,
                center.getZ() - safeRadius, center.getZ() + safeRadius);
    }

    public static boolean boundsLoaded(Level level, BoundingBox bounds) {
        return chunkRangeLoaded(level, bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ());
    }

    private static boolean chunkRangeLoaded(Level level, int minX, int maxX, int minZ, int maxZ) {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }
}
