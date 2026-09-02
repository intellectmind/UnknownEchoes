package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * 自己锁定目标后把同族一起拉进战斗,让成群敌怪不再逐只被点掉。
 * 原版 HurtByTargetGoal.setAlertOthers 只在"被打到"时广播;这条覆盖"主动发现玩家"。
 */
public class AlertPackGoal extends Goal {

    /** 一次广播的最大同族数量:成群生成时不设上限会一次拉起十几只。 */
    private static final int MAX_ALERTED = 4;
    /** 同一目标只广播一次;换目标后重新允许。冷却(tick)防止目标反复丢失导致重复扫描。 */
    private static final int RESCAN_TICKS = 60;

    private final Mob mob;
    private final double alertRange;

    private int rescanDelay;
    private LivingEntity alertedFor;

    /** alertRange 单位为格,按 AABB 膨胀后再做球形距离过滤。 */
    public AlertPackGoal(Mob mob, double alertRange) {
        this.mob = mob;
        this.alertRange = alertRange;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.rescanDelay > 0) {
            --this.rescanDelay;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || target == this.alertedFor) {
            return false;
        }
        this.rescanDelay = RESCAN_TICKS;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.alertedFor = target;
        AABB bounds = this.mob.getBoundingBox()
                .inflate(this.alertRange, this.alertRange * 0.5D, this.alertRange);
        double rangeSqr = this.alertRange * this.alertRange;
        int alerted = 0;
        for (Mob packmate : this.mob.level().getEntitiesOfClass(Mob.class, bounds,
                other -> other != this.mob && other.getClass() == this.mob.getClass()
                        && other.isAlive() && other.getTarget() == null)) {
            if (packmate.distanceToSqr(this.mob) > rangeSqr) {
                continue;
            }
            packmate.setTarget(target);
            if (++alerted >= MAX_ALERTED) {
                return;
            }
        }
    }
}
