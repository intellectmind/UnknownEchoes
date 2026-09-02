package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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

/** 灰脊食苔兽:以背部矿化甲脊储存深层热量的被动群居兽。 */
public class AshbackGrazer extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.ashback_grazer.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.ashback_grazer.walk");
    private static final RawAnimation GRAZE_ANIM =
            RawAnimation.begin().then("animation.ashback_grazer.graze", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int grazeCooldown = 80;

    public AshbackGrazer(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    /** 躲避排在 PanicGoal 之后、繁殖之前:被打到的反应优先,但要在敌怪贴上来之前就走开。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 10.0D, 1.0D, 1.3D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D,
                stack -> stack.is(Items.MOSS_BLOCK) || stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || --this.grazeCooldown > 0) {
            return;
        }
        this.grazeCooldown = 160 + this.random.nextInt(140);
        if (this.getDeltaMovement().horizontalDistanceSqr() < 0.002D && this.onGround()) {
            this.triggerAnim("graze_controller", "graze");
            this.playSound(SoundEvents.ROOTED_DIRT_STEP, 0.55F, 0.72F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.MOSS_BLOCK) || stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.ASHBACK_GRAZER.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ASHBACK_GRAZER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ASHBACK_GRAZER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ASHBACK_GRAZER_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "graze_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("graze", GRAZE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
