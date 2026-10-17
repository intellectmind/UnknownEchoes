package cn.kurt6.unknown_echoes.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoEntity;

public final class EcologyMeleePresentation<T extends Mob & GeoEntity> {
    private final T mob;
    private final int durationTicks;
    private LivingEntity attackTarget;
    private LivingEntity navigationTarget;
    private int startedAtTick;
    private boolean running;

    public EcologyMeleePresentation(T mob, int durationTicks) {
        this.mob = mob;
        this.durationTicks = durationTicks;
    }

    private boolean activeOwner() {
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isNoAi();
    }

    public boolean canAttack(LivingEntity target) {
        return this.activeOwner() && target != null && target.isAlive()
                && target.level() == this.mob.level() && this.mob.canAttack(target)
                && !this.mob.isAlliedTo(target)
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    public boolean begin(LivingEntity target) {
        if (!this.activeOwner() || target.level() != this.mob.level()) {
            return false;
        }
        this.cancel();
        this.attackTarget = target;
        this.navigationTarget = this.mob.getTarget();
        this.startedAtTick = this.mob.tickCount;
        this.running = true;
        this.mob.triggerAnim("attack_controller", "attack");
        return true;
    }

    public void tick() {
        if (this.mob.level().isClientSide || !this.running) {
            return;
        }
        if (this.mob.getTarget() != this.navigationTarget || !this.canAttack(this.attackTarget)) {
            this.cancel();
        } else if (this.mob.tickCount - this.startedAtTick >= this.durationTicks) {
            // 正常结束交给 PLAY_ONCE，服务端停止包只用于中断，避免截掉客户端最后一帧。
            this.attackTarget = null;
            this.navigationTarget = null;
            this.running = false;
        }
    }

    private void cancel() {
        if (!this.running) {
            return;
        }
        this.running = false;
        this.attackTarget = null;
        this.navigationTarget = null;
        this.mob.stopTriggeredAnim("attack_controller", "attack");
    }
}
