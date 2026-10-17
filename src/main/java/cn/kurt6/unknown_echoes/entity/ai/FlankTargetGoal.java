package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 已有同族在正面缠住目标时,绕到目标背侧再进近战,而不是所有人挤在同一格互推。
 * 只负责移动到侧后方落点,伤害仍由近战 Goal 结算。
 * 有抬手期的生物必须传入 movementLocked,否则抬手动画会与实际落点分离。
 */
public class FlankTargetGoal extends Goal {

    /** 侧翼落点距目标的距离(格)。太近会立刻满足近战条件而白跑一趟。 */
    private static final double FLANK_DISTANCE = 4.5D;
    /** 落点搜索半径(格)与纵向范围(格)。 */
    private static final int SEARCH_RADIUS = 10;
    private static final int SEARCH_VERTICAL = 4;
    /** 一次侧翼移动的最长持续 tick;超时交还给近战 Goal,避免绕圈绕到底。 */
    private static final int MAX_DURATION_TICKS = 60;
    /** 冷却(tick):无论成功失败都要退避,否则每 2 tick 一次随机落点搜索。 */
    private static final int RETRY_TICKS = 50;
    /** 触发所需的同族数量下限:自己是唯一追击者时正面压上更合理。 */
    private static final int MIN_PACK_SIZE = 2;
    /** 目标已近于此距离(格)时不再绕后,直接打。 */
    private static final double ENGAGE_DISTANCE_SQR = 9.0D;

    private final PathfinderMob mob;
    private final double speedModifier;
    private final BooleanSupplier movementLocked;

    private int retryDelay;
    private int runTicks;
    private Vec3 flankTarget;
    private LivingEntity maneuverTarget;

    /** movementLocked 在抬手/施法期返回 true,期间不启动也立刻让位。 */
    public FlankTargetGoal(PathfinderMob mob, double speedModifier, BooleanSupplier movementLocked) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.movementLocked = movementLocked;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** 无抬手期的生物用这个构造器;有抬手期的必须显式传 movementLocked。 */
    public FlankTargetGoal(PathfinderMob mob, double speedModifier) {
        this(mob, speedModifier, () -> false);
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
                || this.mob.isAlliedTo(target)) {
            return false;
        }
        double distanceSqr = this.mob.distanceToSqr(target);
        if (distanceSqr <= ENGAGE_DISTANCE_SQR || distanceSqr > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        this.retryDelay = RETRY_TICKS;
        if (!this.hasPackmateEngaged(target)) {
            return false;
        }
        this.flankTarget = this.findFlankTarget(target);
        this.maneuverTarget = this.flankTarget != null ? target : null;
        return this.flankTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return this.runTicks > 0 && this.isManeuverTargetValid()
                && this.mob.distanceToSqr(target) > ENGAGE_DISTANCE_SQR
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.flankTarget == null || !this.isManeuverTargetValid()) {
            this.stop();
            return;
        }
        this.runTicks = MAX_DURATION_TICKS;
        this.mob.getNavigation().moveTo(this.flankTarget.x, this.flankTarget.y, this.flankTarget.z,
                this.speedModifier);
    }

    @Override
    public void stop() {
        this.flankTarget = null;
        this.maneuverTarget = null;
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

    /** 同族且更靠近目标的个体算"已在正面缠住";用类相等而不是 instanceof,避免不同种混算一队。 */
    private boolean hasPackmateEngaged(LivingEntity target) {
        double ownDistanceSqr = this.mob.distanceToSqr(target);
        List<PathfinderMob> engaged = new ArrayList<>(MIN_PACK_SIZE - 1);
        this.mob.level().getEntities(EntityTypeTest.forClass(PathfinderMob.class),
                this.mob.getBoundingBox().inflate(SEARCH_RADIUS),
                other -> other != this.mob && other.getClass() == this.mob.getClass()
                        && other.isAlive() && !other.isNoAi() && other.getTarget() == target
                        && other.canAttack(target) && !other.isAlliedTo(target)
                        && other.distanceToSqr(target) < ownDistanceSqr,
                engaged, MIN_PACK_SIZE - 1);
        return engaged.size() >= MIN_PACK_SIZE - 1;
    }

    private boolean isManeuverTargetValid() {
        LivingEntity target = this.mob.getTarget();
        return this.mob.isAlive() && !this.mob.isNoAi() && !this.movementLocked.getAsBoolean()
                && target != null && target == this.maneuverTarget && target.isAlive()
                && target.level() == this.mob.level() && this.mob.canAttack(target)
                && !this.mob.isAlliedTo(target);
    }

    /** 取目标背侧的落点:以"目标 → 自己"的反向为基准角,再让原版落点搜索在其扇区里挑一个可站点。 */
    private Vec3 findFlankTarget(LivingEntity target) {
        Vec3 toMob = this.mob.position().subtract(target.position());
        double baseAngle = Math.atan2(toMob.z, toMob.x);
        double side = this.mob.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double angle = baseAngle + side * Math.PI * 0.55D;
        Vec3 desired = target.position()
                .add(Math.cos(angle) * FLANK_DISTANCE, 0.0D, Math.sin(angle) * FLANK_DISTANCE);
        return LandRandomPos.getPosTowards(this.mob, SEARCH_RADIUS, SEARCH_VERTICAL, desired);
    }
}
