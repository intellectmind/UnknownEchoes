package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

public final class AerialFlight {
    private AerialFlight() {
    }

    public static PathNavigation navigation(Mob mob, Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(mob, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(false);
        return navigation;
    }

    public static boolean clearRoute(Mob mob, Vec3 destination) {
        Vec3 offset = destination.subtract(mob.position());
        if (!Double.isFinite(offset.lengthSqr()) || offset.lengthSqr() > 32.0D * 32.0D) {
            return false;
        }
        int samples = Math.max(1, Mth.ceil(offset.length() * 2.0D));
        int checkedMinChunkX = Integer.MIN_VALUE;
        int checkedMaxChunkX = Integer.MIN_VALUE;
        int checkedMinChunkZ = Integer.MIN_VALUE;
        int checkedMaxChunkZ = Integer.MIN_VALUE;
        for (int i = 1; i <= samples; i++) {
            Vec3 step = offset.scale((double) i / samples);
            Vec3 previous = offset.scale((double) (i - 1) / samples);
            // 覆盖相邻采样间的整段位移，窄体不能从两次检查之间漏过薄墙。
            AABB bounds = mob.getBoundingBox().move(previous).expandTowards(step.subtract(previous));
            BlockPos pos = BlockPos.containing(mob.position().add(step));
            int minY = Mth.floor(bounds.minY);
            int maxY = Mth.floor(bounds.maxY);
            if (pos.getY() < mob.level().getMinBuildHeight() + 1
                    || pos.getY() >= mob.level().getMaxBuildHeight() - 2
                    || maxY < mob.level().getMinBuildHeight() || minY >= mob.level().getMaxBuildHeight()) {
                return false;
            }
            // BlockCollisions 的读取范围比包围盒多一格，并在取整前外推 1e-7。
            int minX = Math.min(Mth.floor(bounds.minX - 1.0e-7D) - 1, pos.getX());
            int maxX = Math.max(Mth.floor(bounds.maxX + 1.0e-7D) + 1, pos.getX());
            int minZ = Math.min(Mth.floor(bounds.minZ - 1.0e-7D) - 1, pos.getZ());
            int maxZ = Math.max(Mth.floor(bounds.maxZ + 1.0e-7D) + 1, pos.getZ());
            int minChunkX = minX >> 4;
            int maxChunkX = maxX >> 4;
            int minChunkZ = minZ >> 4;
            int maxChunkZ = maxZ >> 4;
            if (minChunkX != checkedMinChunkX || maxChunkX != checkedMaxChunkX
                    || minChunkZ != checkedMinChunkZ || maxChunkZ != checkedMaxChunkZ) {
                if (!hasReadableChunks(mob.level(), new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ))) {
                    return false;
                }
                // 只复用本次同步检查中相同的区块矩形；各段仍独立执行液体和碰撞查询。
                checkedMinChunkX = minChunkX;
                checkedMaxChunkX = maxChunkX;
                checkedMinChunkZ = minChunkZ;
                checkedMaxChunkZ = maxChunkZ;
            }
            if (mob.level().containsAnyLiquid(bounds)
                    || !mob.level().noCollision(mob, bounds)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasReadableChunks(Level level, BlockPos low, BlockPos high) {
        if (!(level instanceof ServerLevel server)) {
            return level.hasChunksAt(low, high);
        }
        if (high.getY() < level.getMinBuildHeight() || low.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        for (int x = low.getX() >> 4; x <= high.getX() >> 4; x++) {
            for (int z = low.getZ() >> 4; z <= high.getZ() >> 4; z++) {
                if (server.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void travel(Mob mob) {
        mob.move(MoverType.SELF, mob.getDeltaMovement());
        mob.setDeltaMovement(mob.getDeltaMovement().scale(mob.isInWater() ? 0.75D : 0.91D));
        mob.fallDistance = 0.0F;
    }
}
