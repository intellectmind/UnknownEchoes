package cn.kurt6.unknown_echoes.entity.ai;

import cn.kurt6.unknown_echoes.entity.mob.SporeCrownedWretch;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public final class SporeVolleyGoal extends Goal {
    private final SporeCrownedWretch mob;
    private LivingEntity target;
    private int windup;
    private int recovery;
    private int pathDelay;

    public SporeVolleyGoal(SporeCrownedWretch mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.validSporeTarget(this.mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return this.target == this.mob.getTarget() && this.mob.validSporeTarget(this.target);
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
                this.mob.releaseSpore(this.target);
            }
            return;
        }
        if (this.recovery > 0) {
            this.recovery--;
            this.mob.getNavigation().stop();
            return;
        }
        if (this.mob.distanceToSqr(this.target) <= 100.0D && this.mob.hasLineOfSight(this.target)) {
            this.windup = this.mob.attackWindupTicks();
            this.mob.getNavigation().stop();
            this.mob.signalAttack();
            return;
        }
        if (--this.pathDelay <= 0) {
            this.pathDelay = 10;
            var navigation = this.mob.getNavigation();
            if (navigation.isInProgress() && this.target.blockPosition().equals(navigation.getTargetPos())) {
                navigation.setSpeedModifier(1.0D);
            } else {
                navigation.moveTo(this.target, 1.0D);
            }
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.windup = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
