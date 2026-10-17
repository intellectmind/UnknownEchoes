package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.world.entity.MoverType;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.EntityBodyCollision;
import net.minecraft.world.phys.AABB;
import cn.kurt6.unknown_echoes.entity.ai.AerialFlight;
import cn.kurt6.unknown_echoes.entity.ai.AerialMoveControl;
import cn.kurt6.unknown_echoes.entity.ai.AerialWanderGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;

public abstract class AerialAnimal extends Animal implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idleAnimation;
    private final RawAnimation flyAnimation;

    protected AerialAnimal(EntityType<? extends Animal> type, Level level, String id) {
        super(type, level);
        this.idleAnimation = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        this.flyAnimation = RawAnimation.begin().thenLoop("animation." + id + ".fly");
        this.moveControl = new AerialMoveControl(this);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return AerialFlight.navigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.0D, this::isFood, false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new AerialWanderGoal(this, this::preferredFlightDirection));
    }

    protected Vec3 preferredFlightDirection() {
        return Vec3.ZERO;
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi()) {
            AerialFlight.travel(this);
            this.calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> state.setAndContinue(
                this.getDeltaMovement().lengthSqr() > 0.001D ? this.flyAnimation : this.idleAnimation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean isInWall() {
        return EntityBodyCollision.isInWall(this);
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityHitboxProfiles.make(this);
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        EntityHitboxProfiles.refresh(this);
    }

    /** 身体朝向可能由原版控制器直接更新，tick 结束后同步长方形判定。 */
    @Override
    public void tick() {
        super.tick();
        EntityHitboxProfiles.refresh(this);
    }

    /** 控制器可直接改变身体朝向，移动碰撞必须先读取当前长宽。 */
    @Override
    public void move(MoverType type, Vec3 movement) {
        EntityHitboxProfiles.refresh(this);
        super.move(type, movement);
    }
}
