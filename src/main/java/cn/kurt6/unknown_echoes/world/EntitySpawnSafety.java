package cn.kurt6.unknown_echoes.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 运行时实体生成前的完整碰撞盒安全检查，不触发区块加载。 */
public final class EntitySpawnSafety {
    private EntitySpawnSafety() {
    }

    /** 深层祭坛召唤 Boss 时的横向落点搜索半径(格)。 */
    private static final int ALTAR_SPAWN_OFFSET = 8;
    /** 落点上方需要的净空高度(格),覆盖两个深层 Boss 的站立高度。 */
    private static final int ALTAR_SPAWN_HEADROOM = 6;

    /**
     * 在祭坛四个正方向各偏移 8 格找一个可站立落点;返回 null 时调用方必须拒绝召唤。
     * 只读取已就绪区块；区块有票据但 FULL future 未完成时继续尝试下一方向。
     */
    @javax.annotation.Nullable
    public static BlockPos findAltarSpawnPos(ServerLevel level, BlockPos altarPos) {
        for (Direction direction : new Direction[]{
                Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
            BlockPos candidate = altarPos.relative(direction, ALTAR_SPAWN_OFFSET);
            BlockPos headroom = candidate.above(ALTAR_SPAWN_HEADROOM);
            BlockPos floor = candidate.below();
            if (!level.getWorldBorder().isWithinBounds(candidate)
                    || !level.isInWorldBounds(candidate)
                    || !level.isInWorldBounds(headroom) || !level.isInWorldBounds(floor)) {
                continue;
            }
            var chunk = level.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4);
            if (chunk != null && chunk.getBlockState(candidate).isAir()
                    && chunk.getBlockState(headroom).isAir()
                    && chunk.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isLoadedAndCollisionFree(ServerLevel level, Entity entity) {
        return isLoadedAndCollisionFree(level, entity, entity.position());
    }

    /** 按目标位置移动当前箱体进行检查，碰撞回调不能使原实体状态或目标现场失效。 */
    public static boolean isLoadedAndCollisionFree(ServerLevel level, Entity entity, Vec3 target) {
        AABB originalBounds = entity.getBoundingBox();
        Vec3 position = entity.position();
        AABB bounds = originalBounds.move(target.subtract(position));
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX - 1.0E-7D,
                bounds.maxY - 1.0E-7D, bounds.maxZ - 1.0E-7D);
        if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)
                || !level.getWorldBorder().isWithinBounds(bounds)) {
            return false;
        }
        if (!areCollisionChunksReady(level, bounds)) {
            return false;
        }
        return level.noCollision(entity, bounds)
                && originalBounds.equals(entity.getBoundingBox())
                && position.equals(entity.position())
                && level.getWorldBorder().isWithinBounds(bounds)
                && areCollisionChunksReady(level, bounds);
    }

    private static boolean areCollisionChunksReady(ServerLevel level, AABB bounds) {
        // BlockCollisions 的取整余量与一格外沿也会查询邻区块，不能只检查实体箱体。
        int minX = (net.minecraft.util.Mth.floor(bounds.minX - 1.0E-7D) - 1) >> 4;
        int maxX = (net.minecraft.util.Mth.floor(bounds.maxX + 1.0E-7D) + 1) >> 4;
        int minZ = (net.minecraft.util.Mth.floor(bounds.minZ - 1.0E-7D) - 1) >> 4;
        int maxZ = (net.minecraft.util.Mth.floor(bounds.maxZ + 1.0E-7D) + 1) >> 4;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }
}
