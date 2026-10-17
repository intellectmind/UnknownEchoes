package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * 目标贴到脸上时后撤重新拉开距离,让远程/扑击型敌怪不会被玩家贴身站死。
 * 优先级数必须小于近战 Goal,否则抢不到 MOVE,注册了也一次都不触发。
 * 有抬手期的生物必须传入 movementLocked,否则抬手动画会与实际落点分离。
 */
public class KeepDistanceGoal extends Goal {

    /** 后撤落点的搜索半径(格)与纵向范围(格)。 */
    private static final int RETREAT_RADIUS = 8;
    private static final int RETREAT_VERTICAL = 4;
    /** 两次尝试之间的冷却(tick):失败后不退避会每 2 tick 重新做一次随机落点搜索。 */
    private static final int RETRY_TICKS = 30;

    private final PathfinderMob mob;
    private final double minDistanceSqr;
    private final double speedModifier;
    private final BooleanSupplier movementLocked;

    private int retryDelay;
    private Vec3 retreatTarget;
    private LivingEntity maneuverTarget;

    /**
     * minDistance 单位为格:目标近于此距离时开始后撤。
     * movementLocked 在抬手/施法期返回 true,期间不启动也立刻让位。
     */
    public KeepDistanceGoal(PathfinderMob mob, double minDistance, double speedModifier,
                            BooleanSupplier movementLocked) {
        this.mob = mob;
        this.minDistanceSqr = minDistance * minDistance;
        this.speedModifier = speedModifier;
        this.movementLocked = movementLocked;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** 无抬手期的生物用这个构造器;有抬手期的必须显式传 movementLocked。 */
    public KeepDistanceGoal(PathfinderMob mob, double minDistance, double speedModifier) {
        this(mob, minDistance, speedModifier, () -> false);
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
        if (!this.mob.isAlive() || this.mob.isNoAi() || target == null || !target.isAlive()
                || target.level() != this.mob.level() || !this.mob.canAttack(target)
                || this.mob.isAlliedTo(target) || this.mob.distanceToSqr(target) > this.minDistanceSqr) {
            return false;
        }
        this.retryDelay = RETRY_TICKS;
        this.retreatTarget = DefaultRandomPos.getPosAway(this.mob, RETREAT_RADIUS, RETREAT_VERTICAL,
                target.position());
        if (!EscapePosition.isNotCloser(this.mob.position(), target.position(), this.retreatTarget)) {
            this.retreatTarget = null;
        }
        this.maneuverTarget = this.retreatTarget != null ? target : null;
        return this.retreatTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return this.isManeuverTargetValid()
                && this.mob.distanceToSqr(target) <= this.minDistanceSqr
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.retreatTarget == null || !this.isManeuverTargetValid()) {
            this.stop();
            return;
        }
        this.mob.getNavigation().moveTo(this.retreatTarget.x, this.retreatTarget.y, this.retreatTarget.z,
                this.speedModifier);
    }

    @Override
    public void stop() {
        this.retreatTarget = null;
        this.maneuverTarget = null;
        this.mob.getNavigation().stop();
    }

    private boolean isManeuverTargetValid() {
        LivingEntity target = this.mob.getTarget();
        return this.mob.isAlive() && !this.mob.isNoAi() && !this.movementLocked.getAsBoolean()
                && target != null && target == this.maneuverTarget && target.isAlive()
                && target.level() == this.mob.level() && this.mob.canAttack(target)
                && !this.mob.isAlliedTo(target);
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }
}
