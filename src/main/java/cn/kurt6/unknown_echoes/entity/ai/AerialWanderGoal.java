package cn.kurt6.unknown_echoes.entity.ai;

import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class AerialWanderGoal extends Goal {
    private final PathfinderMob mob;
    private final Supplier<Vec3> direction;
    private Vec3 destination;
    private int remaining;

    public AerialWanderGoal(PathfinderMob mob) {
        this(mob, () -> Vec3.ZERO);
    }

    public AerialWanderGoal(PathfinderMob mob, Supplier<Vec3> direction) {
        this.direction = direction;
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null || !this.mob.getNavigation().isDone()
                || this.mob.getRandom().nextInt(8) != 0) {
            return false;
        }
        Vec3 bias = this.direction.get().scale(4.0D);
        for (int attempt = 0; attempt < 10; attempt++) {
            double x = this.mob.getX() + bias.x + this.mob.getRandom().nextInt(17) - 8;
            double z = this.mob.getZ() + bias.z + this.mob.getRandom().nextInt(17) - 8;
            BlockPos column = BlockPos.containing(x, this.mob.level().getMinBuildHeight(), z);
            if (!AerialFlight.hasReadableChunks(this.mob.level(), column, column)) {
                continue;
            }
            int ground = this.mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ());
            double y = this.mob.getY() + this.mob.getRandom().nextInt(9) - 3;
            if (ground > this.mob.level().getMinBuildHeight() + 1 && Math.abs(ground - this.mob.getY()) < 20.0D) {
                y = Mth.clamp(y, ground + 2.0D, ground + 12.0D);
            }
            y = Mth.clamp(y, this.mob.level().getMinBuildHeight() + 2.0D,
                    this.mob.level().getMaxBuildHeight() - this.mob.getBbHeight() - 2.0D);
            Vec3 candidate = new Vec3(x, y, z);
            if (Math.abs(y - this.mob.getY()) <= 10.0D && AerialFlight.clearRoute(this.mob, candidate)) {
                this.destination = candidate;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.remaining = 80;
    }

    @Override
    public boolean canContinueToUse() {
        return this.remaining > 0 && this.mob.getTarget() == null && !this.mob.horizontalCollision
                && this.destination != null && this.mob.position().distanceToSqr(this.destination) > 0.25D;
    }

    @Override
    public void tick() {
        this.remaining--;
        this.mob.getMoveControl().setWantedPosition(this.destination.x, this.destination.y, this.destination.z, 1.0D);
    }

    @Override
    public void stop() {
        this.destination = null;
        this.mob.getMoveControl().setWantedPosition(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0D);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
