package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public final class AerialMoveControl extends MoveControl {
    public AerialMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        this.mob.setNoGravity(true);
        Vec3 velocity = this.mob.getDeltaMovement();
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setDeltaMovement(velocity.scale(0.75D));
            return;
        }
        Vec3 delta = new Vec3(this.wantedX, this.wantedY, this.wantedZ).subtract(this.mob.position());
        if (delta.lengthSqr() < 0.09D) {
            this.operation = Operation.WAIT;
            this.mob.setDeltaMovement(velocity.scale(0.7D));
            return;
        }
        double speed = Math.min(0.65D, this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED));
        Vec3 desired = delta.normalize().scale(Math.min(speed, delta.length() * 0.3D));
        this.mob.setDeltaMovement(velocity.lerp(desired, 0.22D));
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * 180.0D / Math.PI) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 12.0F));
        this.mob.yBodyRot = this.mob.getYRot();
    }
}
