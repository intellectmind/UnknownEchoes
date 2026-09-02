package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 被动生物主动躲开附近的敌对生物,不必先被打到。
 * 原版 AvoidEntityGoal 的逃跑点只认可站立方块,飞行生物在空中拿不到落点,所以这里按导航类型分派。
 */
public class FleeThreatGoal extends Goal {

    /** 两次实体查询之间的最小间隔(tick)。canUse 每 2 tick 就会被调用一次,不节流会按生物数量放大开销。 */
    private static final int SCAN_INTERVAL_TICKS = 10;
    /** 逃跑落点的搜索半径(格)与纵向范围(格)。 */
    private static final int ESCAPE_RADIUS = 14;
    private static final int ESCAPE_VERTICAL = 7;
    /** 威胁近于这个距离(格)时切到冲刺速度。 */
    private static final double SPRINT_DISTANCE_SQR = 49.0D;

    private final PathfinderMob mob;
    private final double detectRange;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;

    private int scanDelay;
    private LivingEntity threat;
    private Vec3 escapeTarget;

    /** detectRange 单位为格,按球形判定;两个速度倍率沿用原版 AvoidEntityGoal 的走/冲刺两档语义。 */
    public FleeThreatGoal(PathfinderMob mob, double detectRange,
                          double walkSpeedModifier, double sprintSpeedModifier) {
        this.mob = mob;
        this.detectRange = detectRange;
        this.walkSpeedModifier = walkSpeedModifier;
        this.sprintSpeedModifier = sprintSpeedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.scanDelay > 0) {
            --this.scanDelay;
            return false;
        }
        this.scanDelay = SCAN_INTERVAL_TICKS;
        this.threat = this.nearestThreat();
        if (this.threat == null) {
            return false;
        }
        this.escapeTarget = this.findEscapeTarget(this.threat);
        return this.escapeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.escapeTarget.x, this.escapeTarget.y, this.escapeTarget.z,
                this.walkSpeedModifier);
    }

    @Override
    public void stop() {
        this.threat = null;
        this.escapeTarget = null;
    }

    @Override
    public void tick() {
        if (this.threat == null) {
            return;
        }
        this.mob.getNavigation().setSpeedModifier(
                this.mob.distanceToSqr(this.threat) < SPRINT_DISTANCE_SQR
                        ? this.sprintSpeedModifier
                        : this.walkSpeedModifier);
    }

    /** 按 Enemy 接口判定威胁:本项目敌怪都是 Monster,但原版恶魂/幻翼/史莱姆只实现 Enemy。 */
    private LivingEntity nearestThreat() {
        AABB bounds = this.mob.getBoundingBox()
                .inflate(this.detectRange, this.detectRange * 0.5D, this.detectRange);
        double nearestSqr = this.detectRange * this.detectRange;
        LivingEntity nearest = null;
        for (Mob candidate : this.mob.level().getEntitiesOfClass(Mob.class, bounds,
                entity -> entity instanceof Enemy && entity.isAlive())) {
            double distanceSqr = candidate.distanceToSqr(this.mob);
            if (distanceSqr < nearestSqr) {
                nearestSqr = distanceSqr;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private Vec3 findEscapeTarget(LivingEntity from) {
        if (this.mob.getNavigation() instanceof FlyingPathNavigation) {
            Vec3 away = this.mob.position().subtract(from.position());
            Vec3 heading = away.horizontalDistanceSqr() < 1.0E-4D
                    ? new Vec3(ESCAPE_RADIUS, 0.0D, 0.0D)
                    : away.normalize().scale(ESCAPE_RADIUS);
            Vec3 airTarget = AirRandomPos.getPosTowards(this.mob, ESCAPE_RADIUS, ESCAPE_VERTICAL, 0,
                    this.mob.position().add(heading), Math.PI * 0.5D);
            if (airTarget != null) {
                return airTarget;
            }
        }
        return DefaultRandomPos.getPosAway(this.mob, ESCAPE_RADIUS, ESCAPE_VERTICAL, from.position());
    }
}
