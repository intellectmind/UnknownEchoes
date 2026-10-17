package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
        if (!this.isValidTarget(target) || target == this.alertedFor) {
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
        if (!this.isValidTarget(target)) {
            return;
        }
        this.alertedFor = target;
        AABB bounds = this.mob.getBoundingBox()
                .inflate(this.alertRange, this.alertRange * 0.5D, this.alertRange);
        List<Mob> recipients = new ArrayList<>(MAX_ALERTED);
        // 完整资格与球形范围都放在查询内，找到四个接收者即可停止空间索引遍历。
        this.mob.level().getEntities(EntityTypeTest.forClass(Mob.class), bounds,
                other -> this.canAlert(other, target), recipients, MAX_ALERTED);
        for (Mob packmate : recipients) {
            if (this.mob.getTarget() != target || !this.isValidTarget(target)) {
                return;
            }
            if (this.canAlert(packmate, target)) {
                packmate.setTarget(target);
            }
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        return this.mob.isAlive() && !this.mob.isNoAi()
                && target != null && target.isAlive() && target.level() == this.mob.level()
                && this.mob.canAttack(target) && !this.mob.isAlliedTo(target);
    }

    private boolean canAlert(Mob other, LivingEntity target) {
        return other != this.mob && other.getClass() == this.mob.getClass()
                && other.isAlive() && !other.isNoAi() && other.level() == this.mob.level()
                && other.getTarget() == null && !other.isAlliedTo(target) && other.canAttack(target)
                && other.distanceToSqr(this.mob) <= this.alertRange * this.alertRange;
    }
}
