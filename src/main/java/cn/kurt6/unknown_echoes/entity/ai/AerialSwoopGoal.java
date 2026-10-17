package cn.kurt6.unknown_echoes.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import cn.kurt6.unknown_echoes.entity.mob.AerialHunter;

public final class AerialSwoopGoal extends Goal {
    public static final int WINDUP_TICKS = 8;
    public static final int DIVE_TICKS = 12;
    public static final int RECOVERY_TICKS = 24;
    private static final double CONTACT_MARGIN = 0.35D;
    private static final double ORBIT_RADIUS = 3.0D;
    private static final double MAX_WINDUP_DISTANCE = 4.5D;
    private final AerialHunter mob;
    private LivingEntity target;
    private int phase;
    private int remaining;
    private int orbitMoveDelay;
    private boolean attemptedHit;
    private Vec3 destination;
    private double angle;

    public AerialSwoopGoal(AerialHunter mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean validTarget(LivingEntity target) {
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isNoAi()
                && target != null && target.isAlive() && target.level() == this.mob.level()
                && this.mob.canAttack(target) && !this.mob.isAlliedTo(target)
                && this.mob.distanceToSqr(target) <= 28.0D * 28.0D
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    @Override
    public boolean canUse() {
        return this.validTarget(this.mob.getTarget());
    }

    @Override
    public void start() {
        this.target = this.mob.getTarget();
        this.phase = 0;
        this.remaining = 35 + this.mob.getRandom().nextInt(20);
        this.angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0D;
        this.destination = null;
        this.orbitMoveDelay = 0;
        this.attemptedHit = false;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.target == this.mob.getTarget() && this.validTarget(this.target);
    }

    private void steer(Vec3 point, double speed) {
        this.mob.getMoveControl().setWantedPosition(point.x, point.y, point.z, speed);
    }

    private void recover() {
        if (this.phase == 1) {
            this.mob.cancelAttackPresentation();
        }
        this.phase = 3;
        this.mob.attackPresentation(3);
        this.remaining = RECOVERY_TICKS;
        Vec3 away = this.mob.position().subtract(this.target.position());
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 candidate = this.mob.position().add(away.normalize().scale(3.0D)).add(0.0D, 1.0D, 0.0D);
        this.destination = AerialFlight.clearRoute(this.mob, candidate) ? candidate : this.mob.position();
    }

    /** 仅由正常移动完成后的实际位置调用；传送不生成沿途攻击。 */
    public void onMovement(AABB before, AABB after) {
        this.tryHit(before, after);
    }

    private void tryHit(AABB before, AABB after) {
        if (this.mob.level().isClientSide || !this.mob.isAlive() || this.phase != 2
                || this.attemptedHit || !this.canContinueToUse()) {
            return;
        }
        AABB targetBox = this.target.getBoundingBox();
        boolean contact = after.inflate(CONTACT_MARGIN).intersects(targetBox);
        Vec3 contactPoint = after.getCenter();
        // 常规 move 只平移当前主体框；移动中改变体型时保留终点判定。
        if (!contact && Math.abs(before.getXsize() - after.getXsize()) < 1.0e-6D
                && Math.abs(before.getYsize() - after.getYsize()) < 1.0e-6D
                && Math.abs(before.getZsize() - after.getZsize()) < 1.0e-6D) {
            AABB expanded = targetBox.inflate(before.getXsize() * 0.5D + CONTACT_MARGIN,
                    before.getYsize() * 0.5D + CONTACT_MARGIN,
                    before.getZsize() * 0.5D + CONTACT_MARGIN).deflate(1.0e-7D);
            Vec3 start = before.getCenter();
            Vec3 end = after.getCenter();
            if (expanded.contains(start)) {
                contact = true;
                contactPoint = start;
            } else {
                var intersection = expanded.clip(start, end);
                if (intersection.isPresent()) {
                    contact = true;
                    contactPoint = intersection.get();
                }
            }
        }
        if (contact && this.mob.hasLineOfSight(this.target) && this.visibleAtContact(contactPoint)) {
            this.attemptedHit = true;
            this.mob.doHurtTarget(this.target);
        }
    }

    private boolean visibleAtContact(Vec3 contactPoint) {
        Vec3 center = this.mob.getBoundingBox().getCenter();
        if (contactPoint.distanceToSqr(center) < 1.0e-12D) {
            return true;
        }
        Vec3 eye = contactPoint.add(0.0D, this.mob.getEyeY() - center.y, 0.0D);
        Vec3 targetEye = this.target.getEyePosition();
        var hit = this.mob.level().clip(new ClipContext(eye, targetEye,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
        return hit.getLocation().distanceToSqr(targetEye) < 1.0e-6D;
    }

    @Override
    public void tick() {
        if (!this.canContinueToUse()) {
            this.stop();
            return;
        }
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        this.remaining--;
        if (this.phase == 0) {
            // 找不到可用落点也必须等待，避免掩体后每 tick 重扫整条路线。
            if (--this.orbitMoveDelay <= 0) {
                this.orbitMoveDelay = 10;
                this.angle += 0.65D;
                Vec3 candidate = this.target.position().add(Math.cos(this.angle) * ORBIT_RADIUS,
                        1.0D + this.target.getBbHeight(), Math.sin(this.angle) * ORBIT_RADIUS);
                if (AerialFlight.clearRoute(this.mob, candidate)) {
                    this.destination = candidate;
                }
            }
            if (this.destination != null) {
                this.steer(this.destination, 1.0D);
            }
            // 先接近再起手；过远时 12 tick 的俯冲会在到达目标前结束。
            if (this.remaining <= 0 && this.mob.getSensing().hasLineOfSight(this.target)
                    && this.mob.distanceToSqr(this.target) < MAX_WINDUP_DISTANCE * MAX_WINDUP_DISTANCE) {
                this.phase = 1;
                this.remaining = WINDUP_TICKS;
                this.steer(this.mob.position(), 0.0D);
                this.mob.signalSwoop();
            }
        } else if (this.phase == 1) {
            if (this.remaining <= 0) {
                this.destination = this.target.position().add(0.0D, this.target.getBbHeight() * 0.45D, 0.0D);
                if (!AerialFlight.clearRoute(this.mob, this.destination)) {
                    this.recover();
                    return;
                }
                this.phase = 2;
                this.mob.attackPresentation(2);
                this.remaining = DIVE_TICKS;
                this.attemptedHit = false;
            }
        } else if (this.phase == 2) {
            this.steer(this.destination, 1.8D);
            this.tryHit(this.mob.getBoundingBox(), this.mob.getBoundingBox());
            if (this.remaining <= 0 || this.mob.horizontalCollision
                    || this.mob.position().distanceToSqr(this.destination) < 0.25D) {
                this.recover();
            }
        } else {
            this.steer(this.destination, 0.65D);
            if (this.remaining <= 0) {
                this.phase = 0;
                this.mob.attackPresentation(0);
                this.remaining = 35 + this.mob.getRandom().nextInt(20);
                this.destination = null;
                this.orbitMoveDelay = 0;
            }
        }
    }

    @Override
    public void stop() {
        this.mob.cancelAttackPresentation();
        this.target = null;
        this.destination = null;
        this.mob.getNavigation().stop();
        this.steer(this.mob.position(), 0.0D);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
