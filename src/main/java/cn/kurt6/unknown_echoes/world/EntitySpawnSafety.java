package cn.kurt6.unknown_echoes.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

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
     * 不触发区块加载(hasChunkAt 为假即跳过该方向):同步加载会死锁。
     */
    @javax.annotation.Nullable
    public static BlockPos findAltarSpawnPos(ServerLevel level, BlockPos altarPos) {
        for (Direction direction : new Direction[]{
                Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
            BlockPos candidate = altarPos.relative(direction, ALTAR_SPAWN_OFFSET);
            BlockPos headroom = candidate.above(ALTAR_SPAWN_HEADROOM);
            if (!level.hasChunkAt(candidate)
                    || !level.getWorldBorder().isWithinBounds(candidate)
                    || !level.isInWorldBounds(candidate)
                    || !level.isInWorldBounds(headroom)) {
                continue;
            }
            BlockPos floor = candidate.below();
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(headroom).isAir()
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isLoadedAndCollisionFree(ServerLevel level, Entity entity) {
        AABB bounds = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX - 1.0E-7D,
                bounds.maxY - 1.0E-7D, bounds.maxZ - 1.0E-7D);
        if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)
                || !level.getWorldBorder().isWithinBounds(bounds)) {
            return false;
        }
        if (!LoadedAreaCheck.boundsLoaded(level,
                new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        min.getX(), min.getY(), min.getZ(),
                        max.getX(), max.getY(), max.getZ()))) {
            return false;
        }
        return level.noCollision(entity);
    }
}
