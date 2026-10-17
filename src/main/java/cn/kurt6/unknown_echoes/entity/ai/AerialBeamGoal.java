package cn.kurt6.unknown_echoes.entity.ai;

import java.util.EnumSet;
import cn.kurt6.unknown_echoes.entity.mob.ShardwingSkate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AerialBeamGoal extends Goal {
    public static final int WINDUP_TICKS = 14;
    public static final int RECOVERY_TICKS = 32;
    private final ShardwingSkate mob;
    private LivingEntity target;
    private int windup;
    private int cooldown;
    private int moveDelay;
    private Vec3 anchor;
    private Vec3 start;
    private Vec3 end;

    public AerialBeamGoal(ShardwingSkate mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean validTarget(LivingEntity target) {
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isNoAi()
                && target != null && target.isAlive() && target.level() == this.mob.level()
                && this.mob.canAttack(target) && !this.mob.isAlliedTo(target)
                && this.mob.distanceToSqr(target) <= 24.0D * 24.0D
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
        this.cooldown = 20;
        this.moveDelay = 0;
        this.windup = 0;
        this.mob.getNavigation().stop();
    }

    private void steer(Vec3 point, double speed) {
        this.mob.getMoveControl().setWantedPosition(point.x, point.y, point.z, speed);
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
            if (this.windup % 3 == 0) {
                this.mob.warnBeam(this.start, this.end);
            }
            if (--this.windup == 0) {
                this.cooldown = RECOVERY_TICKS;
                this.mob.attackPresentation(2);
                this.mob.fireBeam(this.target, this.start, this.end);
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
        if (this.cooldown == 0 && this.mob.distanceToSqr(this.target) <= 12.0D * 12.0D
                && this.mob.getSensing().hasLineOfSight(this.target)) {
            this.anchor = this.mob.position();
            this.start = this.anchor.add(0.0D, this.mob.getBbHeight() * 0.55D, 0.0D);
            Vec3 aim = this.target.position().add(0.0D, this.target.getBbHeight() * 0.5D, 0.0D);
            this.end = this.start.add(aim.subtract(this.start).normalize().scale(12.0D));
            this.windup = WINDUP_TICKS;
            this.steer(this.anchor, 0.0D);
            this.mob.signalSwoop();
            this.mob.warnBeam(this.start, this.end);
            return;
        }
        if (--this.moveDelay <= 0) {
            this.moveDelay = 12;
            Vec3 away = this.mob.position().subtract(this.target.position());
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 direction = away.normalize();
            Vec3 candidate = this.target.position().add(direction.scale(7.0D))
                    .add(-direction.z * 2.0D, 2.0D, direction.x * 2.0D);
            if (AerialFlight.clearRoute(this.mob, candidate)) {
                this.steer(candidate, 0.9D);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.cancelAttackPresentation();
        this.target = null;
        this.windup = 0;
        this.start = null;
        this.end = null;
        this.anchor = null;
        this.mob.getNavigation().stop();
        this.steer(this.mob.position(), 0.0D);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
