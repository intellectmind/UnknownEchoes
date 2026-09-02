package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * Boss 在偏好交战距离上绕圈,让"必须站远才能放"的远程技能拿到窗口。
 * 只在目标进入近身贴脸距离时启动;近战 Goal 优先级更高,所以贴身回合仍然照常挥砍。
 */
public class CirclePreferredRangeGoal extends Goal {

    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_VERTICAL = 5;
    /** 一次绕圈的最长持续 tick,到点交还 MOVE 让近战 Goal 有机会重新贴上。 */
    private static final int MAX_DURATION_TICKS = 50;
    /** 冷却(tick):不退避会让 Boss 一直在环上滑步,永远不进近战。 */
    private static final int RETRY_TICKS = 90;
    /** 每次绕圈的角位移(弧度);太大等于直接横跳到对面。 */
    private static final double ARC_STEP = Math.PI * 0.45D;

    private final PathfinderMob mob;
    private final double engageDistance;
    private final double preferredDistance;
    private final double speedModifier;
    private final BooleanSupplier movementLocked;

    private int retryDelay;
    private int runTicks;
    private Vec3 circleTarget;

    /**
     * engageDistance:目标近于该距离(格)才开始绕圈。
     * preferredDistance:绕圈半径(格),取远程技能的射程下限之上一点。
     * movementLocked:抬手/施法期返回 true 的判据,期间本 Goal 既不启动也立刻让位。
     */
    public CirclePreferredRangeGoal(PathfinderMob mob, double engageDistance,
                                    double preferredDistance, double speedModifier,
                                    BooleanSupplier movementLocked) {
        this.mob = mob;
        this.engageDistance = engageDistance;
        this.preferredDistance = preferredDistance;
        this.speedModifier = speedModifier;
        this.movementLocked = movementLocked;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.retryDelay > 0) {
            --this.retryDelay;
            return false;
        }
        if (this.movementLocked.getAsBoolean()) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()
                || this.mob.distanceToSqr(target) > this.engageDistance * this.engageDistance) {
            return false;
        }
        this.retryDelay = RETRY_TICKS;
        this.circleTarget = this.findCircleTarget(target);
        return this.circleTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return this.runTicks > 0 && target != null && target.isAlive()
                && !this.movementLocked.getAsBoolean()
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.runTicks = MAX_DURATION_TICKS;
        this.mob.getNavigation().moveTo(this.circleTarget.x, this.circleTarget.y, this.circleTarget.z,
                this.speedModifier);
    }

    @Override
    public void stop() {
        this.circleTarget = null;
        this.runTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        --this.runTicks;
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /** 沿当前方位角推进一段弧,再按偏好半径取落点;落点仍走原版可站性校验。 */
    private Vec3 findCircleTarget(LivingEntity target) {
        Vec3 toMob = this.mob.position().subtract(target.position());
        double baseAngle = toMob.horizontalDistanceSqr() < 1.0E-4D
                ? this.mob.getRandom().nextDouble() * Math.PI * 2.0D
                : Math.atan2(toMob.z, toMob.x);
        double side = this.mob.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double angle = baseAngle + side * ARC_STEP;
        Vec3 desired = target.position().add(
                Math.cos(angle) * this.preferredDistance, 0.0D, Math.sin(angle) * this.preferredDistance);
        return LandRandomPos.getPosTowards(this.mob, SEARCH_RADIUS, SEARCH_VERTICAL, desired);
    }
}
