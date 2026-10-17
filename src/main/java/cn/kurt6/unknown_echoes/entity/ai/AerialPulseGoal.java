package cn.kurt6.unknown_echoes.entity.ai;

import cn.kurt6.unknown_echoes.entity.mob.CrystalSpineUrchin;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AerialPulseGoal extends Goal {
    public static final int WINDUP_TICKS = 20;
    public static final int RECOVERY_TICKS = 50;
    private final CrystalSpineUrchin mob;
    private LivingEntity target;
    private Vec3 anchor;
    private Vec3 center;
    private int windup;
    private int cooldown;
    private int moveDelay;

    public AerialPulseGoal(CrystalSpineUrchin mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean validTarget(LivingEntity target) {
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isNoAi()
                && target != null && target.isAlive()
                && this.mob.canAttack(target) && !this.mob.isAlliedTo(target)
                && target.level() == this.mob.level() && this.mob.distanceToSqr(target) <= 24.0D * 24.0D
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    @Override
    public boolean canUse() {
        return this.validTarget(this.mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return this.target == this.mob.getTarget() && this.validTarget(this.target);
    }

    @Override
    public void start() {
        this.target = this.mob.getTarget();
        this.cooldown = 30;
        this.windup = 0;
        this.moveDelay = 0;
        this.mob.getNavigation().stop();
    }

    private void steer(Vec3 position, double speed) {
        this.mob.getMoveControl().setWantedPosition(position.x, position.y, position.z, speed);
    }

    @Override
    public void tick() {
        if (!this.canContinueToUse() || this.windup > 0 && this.mob.attackPresentation() == 0) {
            this.stop();
            return;
        }
        this.mob.getLookControl().setLookAt(this.target, 20.0F, 20.0F);
        if (this.windup > 0) {
            this.steer(this.anchor, 0.0D);
            if (this.mob.position().distanceToSqr(this.anchor) > 1.0D) {
                this.mob.cancelAttackPresentation();
                this.windup = 0;
                this.cooldown = RECOVERY_TICKS;
                return;
            }
            if (this.windup < WINDUP_TICKS && this.windup % 5 == 0) {
                this.mob.warnPulse(this.center);
            }
            if (--this.windup == 0) {
                this.cooldown = RECOVERY_TICKS;
                this.mob.attackPresentation(2);
                this.mob.firePulse(this.target, this.center);
            }
            return;
        }
        if (this.cooldown > 0) {
            if (this.mob.attackPresentation() != 0) {
                this.mob.attackPresentation(3);
            }
            this.cooldown--;
            if (this.cooldown == 0) {
                this.mob.attackPresentation(0);
            }
        }
        Vec3 bodyCenter = this.mob.position().add(0.0D, this.mob.getBbHeight() * 0.5D, 0.0D);
        if (this.cooldown == 0 && bodyCenter.distanceToSqr(this.target.getBoundingBox().getCenter())
                <= AerialPulseStrike.RADIUS * AerialPulseStrike.RADIUS
                && this.mob.getSensing().hasLineOfSight(this.target)) {
            this.anchor = this.mob.position();
            this.center = bodyCenter;
            this.windup = WINDUP_TICKS;
            this.steer(this.anchor, 0.0D);
            this.mob.signalSwoop();
            this.mob.warnPulse(this.center);
            return;
        }
        if (--this.moveDelay <= 0) {
            this.moveDelay = 12;
            Vec3 away = bodyCenter.subtract(this.target.getBoundingBox().getCenter());
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 point = this.target.getBoundingBox().getCenter().add(away.normalize().scale(2.1D))
                    .add(0.0D, -this.mob.getBbHeight() * 0.5D, 0.0D);
            if (AerialFlight.clearRoute(this.mob, point)) {
                this.steer(point, 0.9D);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.cancelAttackPresentation();
        this.target = null;
        this.windup = 0;
        this.center = null;
        this.anchor = null;
        this.mob.getNavigation().stop();
        this.steer(this.mob.position(), 0.0D);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
