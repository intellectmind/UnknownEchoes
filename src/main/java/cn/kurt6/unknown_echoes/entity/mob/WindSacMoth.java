package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 风袋蛾:回声峭壁的被动飞虫,提供风囊膜普通材料。 */
public class WindSacMoth extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.wind_sac_moth.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.wind_sac_moth.fly");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public WindSacMoth(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.FLYING_SPEED, 0.48D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** 躲避排在 PanicGoal 之后:被打到的反应优先,但要在敌怪贴上来之前就走开。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.45D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 9.0D, 1.1D, 1.4D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D,
                stack -> stack.is(ModItems.DREAM_NECTAR.get()) || stack.is(ModItems.CRYSTAL_FEATHER.get()), false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0D) {
            this.setDeltaMovement(movement.multiply(1.0D, 0.55D, 1.0D));
        }
        if (!this.level().isClientSide && this.tickCount % 24 == 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.35D, this.getZ(),
                    1, 0.10D, 0.06D, 0.10D, 0.0D);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WIND_SAC_MOTH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WIND_SAC_MOTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WIND_SAC_MOTH_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(this.onGround() && !state.isMoving() ? IDLE_ANIM : FLY_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
