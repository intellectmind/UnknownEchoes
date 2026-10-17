package cn.kurt6.unknown_echoes.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EntityBodyCollision {
    private EntityBodyCollision() {
    }

    public static boolean isInWall(LivingEntity entity) {
        if (entity.noPhysics || entity.isSleeping()) {
            return false;
        }

        // 注册宽度包含身长；用它构造方形眼部区域会让窄体生物在侧墙旁误判窒息。
        AABB body = EntityHitboxProfiles.make(entity);
        AABB eyes = AABB.ofSize(entity.getEyePosition(),
                body.getXsize() * 0.8D, 1.0E-6, body.getZsize() * 0.8D);
        int minX = Mth.floor(eyes.minX);
        int minY = Mth.floor(eyes.minY);
        int minZ = Mth.floor(eyes.minZ);
        int maxX = Mth.floor(eyes.maxX);
        int maxY = Mth.floor(eyes.maxY);
        int maxZ = Mth.floor(eyes.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        VoxelShape eyeShape = null;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    pos.set(x, y, z);
                    BlockState state = entity.level().getBlockState(pos);
                    if (!state.isAir() && state.isSuffocating(entity.level(), pos)) {
                        if (eyeShape == null) {
                            eyeShape = Shapes.create(eyes);
                        }
                        if (Shapes.joinIsNotEmpty(state.getCollisionShape(entity.level(), pos)
                                .move(x, y, z), eyeShape, BooleanOp.AND)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
