package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * 残血时脱离一次,给玩家一个"打退了它"的读数,而不是站在原地被平砍到死。
 * 一只生物整场只脱离一次(不随治疗重置),否则残血线附近会反复拉扯成风筝。
 * 有抬手期的生物必须传入 movementLocked,否则抬手动画会与实际落点分离。
 */
public class RetreatWhenLowGoal extends Goal {

    private static final int ESCAPE_RADIUS = 12;
    private static final int ESCAPE_VERTICAL = 6;
    /** 一次脱离的最长持续 tick:超时交还给近战 Goal。 */
    private static final int MAX_DURATION_TICKS = 70;
    private static final int SEARCH_RETRY_TICKS = 30;

    private final PathfinderMob mob;
    private final float healthFraction;
    private final double speedModifier;
    private final BooleanSupplier movementLocked;

    private boolean spent;
    private int nextSearchTick;
    private int runTicks;
    private Vec3 escapeTarget;

    /**
     * healthFraction 是最大生命的比例(0~1),低于它触发一次脱离。
     * movementLocked 在抬手/施法期返回 true,期间不启动也立刻让位。
     */
    public RetreatWhenLowGoal(PathfinderMob mob, float healthFraction, double speedModifier,
                              BooleanSupplier movementLocked) {
        this.mob = mob;
        this.healthFraction = healthFraction;
        this.speedModifier = speedModifier;
        this.movementLocked = movementLocked;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** 无抬手期的生物用这个构造器;有抬手期的必须显式传 movementLocked。 */
    public RetreatWhenLowGoal(PathfinderMob mob, float healthFraction, double speedModifier) {
        this(mob, healthFraction, speedModifier, () -> false);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextSearchTick) {
            return false;
        }
        if (this.spent || this.movementLocked.getAsBoolean()
                || this.mob.getHealth() > this.mob.getMaxHealth() * this.healthFraction) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // 搜索失败也退避；一次脱离机会只在导航成功启动后消耗。
        this.nextSearchTick = this.mob.tickCount + SEARCH_RETRY_TICKS;
        this.escapeTarget = this.findEscapeTarget(target);
        return this.escapeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.runTicks > 0 && !this.movementLocked.getAsBoolean()
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.escapeTarget == null) {
            return;
        }
        if (!this.mob.getNavigation().moveTo(this.escapeTarget.x, this.escapeTarget.y, this.escapeTarget.z,
                this.speedModifier)) {
            this.stop();
            return;
        }
        this.spent = true;
        this.runTicks = MAX_DURATION_TICKS;
    }

    @Override
    public void stop() {
        this.escapeTarget = null;
        this.runTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        --this.runTicks;
    }

    private Vec3 findEscapeTarget(LivingEntity from) {
        if (this.mob.getNavigation() instanceof FlyingPathNavigation) {
            Vec3 away = this.mob.position().subtract(from.position());
            Vec3 heading = away.horizontalDistanceSqr() < 1.0E-4D
                    ? new Vec3(ESCAPE_RADIUS, 0.0D, 0.0D)
                    : away.normalize().scale(ESCAPE_RADIUS);
            Vec3 airTarget = AirRandomPos.getPosTowards(this.mob, ESCAPE_RADIUS, ESCAPE_VERTICAL, 0,
                    this.mob.position().add(heading), Math.PI * 0.5D);
            if (EscapePosition.isNotCloser(this.mob.position(), from.position(), airTarget)) {
                return airTarget;
            }
        }
        Vec3 target = DefaultRandomPos.getPosAway(this.mob, ESCAPE_RADIUS, ESCAPE_VERTICAL, from.position());
        return EscapePosition.isNotCloser(this.mob.position(), from.position(), target) ? target : null;
    }
}
