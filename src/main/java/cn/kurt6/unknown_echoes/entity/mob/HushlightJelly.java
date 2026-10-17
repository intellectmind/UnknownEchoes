package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** 寂光浮囊:在静默深渊中以柔光脉冲交流的悬浮动物。 */
public class HushlightJelly extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.hushlight_jelly.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.hushlight_jelly.fly");
    private static final RawAnimation PULSE_ANIM =
            RawAnimation.begin().then("animation.hushlight_jelly.pulse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pulseCooldown = 60;

    public HushlightJelly(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 8, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FLYING_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 14.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** 躲避排在 PanicGoal 之后、繁殖之前:被打到的反应优先,但要在敌怪贴上来之前就走开。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new FleeThreatGoal(this, 9.0D, 0.95D, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.95D,
                stack -> stack.is(Items.GLOW_BERRIES) || stack.is(Items.AMETHYST_SHARD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 0.95D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.75D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);
        if (this.level().isClientSide || --this.pulseCooldown > 0) {
            return;
        }
        this.pulseCooldown = 120 + this.random.nextInt(100);
        this.triggerAnim("pulse_controller", "pulse");
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                10, 0.65D, 0.5D, 0.65D, 0.01D);
        this.playSound(ModSounds.HUSHLIGHT_JELLY_PULSE.get(), 0.45F, 1.0F);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES) || stack.is(Items.AMETHYST_SHARD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.HUSHLIGHT_JELLY.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HUSHLIGHT_JELLY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HUSHLIGHT_JELLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HUSHLIGHT_JELLY_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? FLY_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "pulse_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("pulse", PULSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
