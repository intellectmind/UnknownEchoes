package cn.kurt6.unknown_echoes.entity.ai;

import cn.kurt6.unknown_echoes.entity.mob.HumanoidRaider;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public final class GroundRaiderAttackGoal extends Goal {
    private final HumanoidRaider mob;
    private LivingEntity target;
    private int windup;
    private int recovery;
    private int pathDelay;

    public GroundRaiderAttackGoal(HumanoidRaider mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid(LivingEntity candidate) {
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isNoAi()
                && candidate != null && candidate.isAlive() && candidate.level() == this.mob.level()
                && !this.mob.isAlliedTo(candidate)
                && this.mob.canAttack(candidate) && this.mob.distanceToSqr(candidate) <= 24.0D * 24.0D
                && (!(candidate instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    @Override
    public boolean canUse() {
        return this.valid(this.mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return this.target == this.mob.getTarget() && this.valid(this.target);
    }

    @Override
    public void start() {
        this.target = this.mob.getTarget();
        this.windup = 0;
        this.recovery = 0;
        this.pathDelay = 0;
    }

    @Override
    public void tick() {
        if (!this.canContinueToUse()) {
            this.stop();
            return;
        }
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (this.windup > 0) {
            this.mob.getNavigation().stop();
            if (--this.windup == 0) {
                this.recovery = this.mob.attackRecoveryTicks();
                this.mob.attackPresentation(2);
                if (this.mob.isWithinMeleeAttackRange(this.target) && this.mob.hasLineOfSight(this.target)) {
                    this.mob.doHurtTarget(this.target);
                }
            }
            return;
        }
        if (this.recovery > 0) {
            this.mob.attackPresentation(3);
            this.recovery--;
            if (this.recovery == 0) {
                this.mob.attackPresentation(0);
            }
            this.mob.getNavigation().stop();
            return;
        }
        if (this.mob.isWithinMeleeAttackRange(this.target) && this.mob.hasLineOfSight(this.target)) {
            this.windup = this.mob.attackWindupTicks();
            this.mob.getNavigation().stop();
            this.mob.signalAttack();
            return;
        }
        // 追逐失败也遵守 10 tick 间隔，避免每 tick 重算导航。
        if (--this.pathDelay <= 0) {
            this.pathDelay = 10;
            var navigation = this.mob.getNavigation();
            // 重交同一路径会重置原版卡住检测；目标仍在原格时只维护速度。
            if (navigation.isInProgress() && this.target.blockPosition().equals(navigation.getTargetPos())) {
                navigation.setSpeedModifier(1.0D);
            } else {
                navigation.moveTo(this.target, 1.0D);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.cancelAttackPresentation();
        this.target = null;
        this.windup = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
