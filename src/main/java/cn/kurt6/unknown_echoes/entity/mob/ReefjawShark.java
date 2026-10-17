package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.EcologyMeleePresentation;
import cn.kurt6.unknown_echoes.entity.EntityBodyCollision;
import net.minecraft.world.phys.AABB;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 礁颚鲨:只在水中追咬目标的礁区捕食者。 */
public class ReefjawShark extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.reefjaw_shark.idle");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.reefjaw_shark.swim");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin()
            .then("animation.reefjaw_shark.attack", Animation.LoopType.PLAY_ONCE);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final EcologyMeleePresentation<ReefjawShark> meleePresentation =
            new EcologyMeleePresentation<>(this, 13);

    public ReefjawShark(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.8F, 0.6F, false);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8D, 40));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        if (type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return this.isInWater() && target.isInWater() && super.canAttack(target);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.getTarget() != null && !this.canAttack(this.getTarget())) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !this.meleePresentation.canAttack(living)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && this.meleePresentation.begin(living)) {
            this.playSound(ModSounds.REEFJAW_SHARK_STRIKE.get(), 0.7F, 1.0F);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.REEFJAW_SHARK_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.REEFJAW_SHARK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.REEFJAW_SHARK_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state ->
                state.setAndContinue(this.isInWater() && state.isMoving() ? SWIM_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
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
        this.meleePresentation.tick();
        EntityHitboxProfiles.refresh(this);
    }

    /** 控制器可直接改变身体朝向，移动碰撞必须先读取当前长宽。 */
    @Override
    public void move(MoverType type, Vec3 movement) {
        EntityHitboxProfiles.refresh(this);
        super.move(type, movement);
    }
}
