package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** 只检查已加载区块，并用区段调色板跳过不含可用栖鸣台的区段。 */
public final class RealmChimeLookup {
    private RealmChimeLookup() { }

    static BlockPos findNearest(ServerLevel level, BlockPos origin, RealmHabitatChimeBlock.Mode mode,
                                int horizontalRadius, int verticalRadius) {
        return findNearest(level, origin, mode, horizontalRadius, verticalRadius, false);
    }

    /** 观测镜查找已激活台，同距时保留 X、Z、Y 的选择顺序。 */
    public static BlockPos findActive(ServerLevel level, BlockPos origin, RealmHabitatChimeBlock.Mode mode,
                                      int horizontalRadius, int verticalRadius) {
        return findNearest(level, origin, mode, horizontalRadius, verticalRadius, true);
    }

    private static BlockPos findNearest(ServerLevel level, BlockPos origin, RealmHabitatChimeBlock.Mode mode,
                                        int horizontalRadius, int verticalRadius, boolean active) {
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - verticalRadius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + verticalRadius);
        if (minY > maxY) return null;
        int minX = origin.getX() - horizontalRadius, maxX = origin.getX() + horizontalRadius;
        int minZ = origin.getZ() - horizontalRadius, maxZ = origin.getZ() + horizontalRadius;
        int radiusSqr = horizontalRadius * horizontalRadius;
        BlockPos nearest = null;
        int nearestDistanceSqr = Integer.MAX_VALUE;
        java.util.function.Predicate<BlockState> available = active
                ? state -> state.is(ModBlocks.REALM_HABITAT_CHIME.get())
                        && state.getValue(RealmHabitatChimeBlock.MODE) == mode
                        && state.getValue(RealmHabitatChimeBlock.ACTIVE)
                : state -> isAvailable(state, mode);
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                int fromX = Math.max(minX, chunkX << 4), toX = Math.min(maxX, (chunkX << 4) + 15);
                int fromZ = Math.max(minZ, chunkZ << 4), toZ = Math.min(maxZ, (chunkZ << 4) + 15);
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    var section = chunk.getSection(chunk.getSectionIndex(sectionY << 4));
                    if (!section.maybeHas(available)) continue;
                    int fromY = Math.max(minY, sectionY << 4), toY = Math.min(maxY, (sectionY << 4) + 15);
                    for (int y = fromY; y <= toY; y++) {
                        int dy = y - origin.getY();
                        for (int x = fromX; x <= toX; x++) {
                            int dx = x - origin.getX();
                            for (int z = fromZ; z <= toZ; z++) {
                                int dz = z - origin.getZ();
                                int horizontalDistanceSqr = dx * dx + dz * dz;
                                int distanceSqr = horizontalDistanceSqr + dy * dy;
                                if (horizontalDistanceSqr > radiusSqr || distanceSqr > nearestDistanceSqr
                                        || !available.test(section.getBlockState(x & 15, y & 15, z & 15))) continue;
                                if (distanceSqr < nearestDistanceSqr
                                        || (active ? earlierActive(x, z, y, nearest) : earlier(y, x, z, nearest))) {
                                    nearestDistanceSqr = distanceSqr;
                                    nearest = new BlockPos(x, y, z);
                                }
                            }
                        }
                    }
                }
            }
        }
        return nearest;
    }

    static boolean isAvailable(BlockState state, RealmHabitatChimeBlock.Mode mode) {
        return state.getBlock() instanceof RealmHabitatChimeBlock
                && state.getValue(RealmHabitatChimeBlock.MODE) == mode
                && !state.getValue(RealmHabitatChimeBlock.ACTIVE);
    }

    private static boolean earlier(int y, int x, int z, BlockPos current) {
        return current == null || y < current.getY()
                || y == current.getY() && (x < current.getX() || x == current.getX() && z < current.getZ());
    }

    private static boolean earlierActive(int x, int z, int y, BlockPos current) {
        return current == null || x < current.getX()
                || x == current.getX() && (z < current.getZ() || z == current.getZ() && y < current.getY());
    }
}
