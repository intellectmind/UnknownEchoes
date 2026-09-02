package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.FlankTargetGoal;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 灰幕猎手:用灰尘遮蔽视野后低伏跳扑的深层掠食者。 */
public class AshveilStalker extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.ashveil_stalker.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.ashveil_stalker.walk");
    private static final RawAnimation POUNCE_ANIM =
            RawAnimation.begin().then("animation.ashveil_stalker.pounce", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pounceCooldown = 35;

    public AshveilStalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 38.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    /** 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlankTargetGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.82D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 11.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 14.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.pounceCooldown > 0) {
            --this.pounceCooldown;
        }
        if (this.level().isClientSide || this.pounceCooldown > 0 || !this.onGround()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr < 10.0D || distanceSqr > 72.0D
                || !this.getSensing().hasLineOfSight(target)) {
            return;
        }
        Vec3 direction = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(direction.x * 0.88D, 0.46D, direction.z * 0.88D);
        this.hasImpulse = true;
        this.pounceCooldown = 70 + this.random.nextInt(40);
        this.triggerAnim("pounce_controller", "pounce");
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.ASH,
                this.getX(), this.getY() + 0.25D, this.getZ(), 18, 0.55D, 0.2D, 0.55D, 0.03D);
        level.playSound(null, this.blockPosition(), ModSounds.ASHVEIL_STALKER_POUNCE.get(),
                SoundSource.HOSTILE, 0.65F, 1.0F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && !this.level().isClientSide) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 55, 0));
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ASHVEIL_STALKER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ASHVEIL_STALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ASHVEIL_STALKER_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "pounce_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("pounce", POUNCE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
