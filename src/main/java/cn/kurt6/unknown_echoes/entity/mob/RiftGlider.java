package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import javax.annotation.Nullable;

/** 层缝翔鳐:借深层回响的重力断层缓慢滑翔的被动生物。 */
public class RiftGlider extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.rift_glider.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.rift_glider.fly");
    private static final RawAnimation PULSE_ANIM =
            RawAnimation.begin().then("animation.rift_glider.pulse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pulseCooldown = 100;

    public RiftGlider(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.48D)
                .add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** 躲避排在 PanicGoal 之后、繁殖之前:被打到的反应优先,但要在敌怪贴上来之前就走开。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.45D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 11.0D, 1.1D, 1.4D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D,
                stack -> stack.is(Items.CHORUS_FRUIT) || stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.05D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0D) {
            this.setDeltaMovement(movement.multiply(1.0D, 0.55D, 1.0D));
        }
        if (this.level().isClientSide) {
            return;
        }
        if (--this.pulseCooldown > 0) {
            return;
        }
        this.pulseCooldown = 150 + this.random.nextInt(100);
        this.triggerAnim("pulse_controller", "pulse");
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                8, 0.8D, 0.25D, 0.8D, 0.015D);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CHORUS_FRUIT) || stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.RIFT_GLIDER.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RIFT_GLIDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RIFT_GLIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RIFT_GLIDER_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() || !this.onGround() ? FLY_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "pulse_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("pulse", PULSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
