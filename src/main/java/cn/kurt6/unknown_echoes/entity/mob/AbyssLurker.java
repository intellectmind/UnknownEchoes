package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.FlankTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/** 深渊潜伏者:静默深渊低光区域的低伏跳扑型敌怪。 */
public class AbyssLurker extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.abyss_lurker.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.abyss_lurker.walk");
    private static final RawAnimation POUNCE_ANIM =
            RawAnimation.begin().then("animation.abyss_lurker.pounce", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pounceCooldown;
    private boolean exposedToLight;

    public AbyssLurker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.FOLLOW_RANGE, 26.0D);
    }

    /** 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlankTargetGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 12.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return AbyssLurker.this.level().getMaxLocalRawBrightness(AbyssLurker.this.blockPosition()) == 0
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return AbyssLurker.this.level().getMaxLocalRawBrightness(AbyssLurker.this.blockPosition()) == 0
                        && super.canContinueToUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.isAlive() || this.isNoAi()) {
            return;
        }
        if (this.pounceCooldown > 0) {
            this.pounceCooldown--;
        }
        if (!this.level().isClientSide) {
            int brightness = this.level().getMaxLocalRawBrightness(this.blockPosition());
            boolean nowExposed = brightness > 0;
            if (nowExposed != this.exposedToLight) {
                this.exposedToLight = nowExposed;
                ServerLevel level = (ServerLevel) this.level();
                level.sendParticles(nowExposed ? ParticleTypes.SMOKE : ParticleTypes.SCULK_SOUL,
                        this.getX(), this.getY() + 0.35D, this.getZ(),
                        nowExposed ? 12 : 8, 0.28D, 0.18D, 0.28D, 0.02D);
                level.playSound(null, this.blockPosition(),
                        nowExposed ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, nowExposed ? 0.45F : 0.35F,
                        nowExposed ? 1.7F : 0.65F);
            }
            if (this.tickCount % 20 == 0 && nowExposed) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        30, 0, true, false, false));
            }
        }
        if (this.level().isClientSide || this.pounceCooldown > 0 || !this.onGround()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || target.level() != this.level()
                || !this.canAttack(target)) {
            return;
        }
        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr < 9.0D || distanceSqr > 64.0D
                || !this.getSensing().hasLineOfSight(target)) {
            return;
        }

        Vec3 direction = target.position().subtract(this.position()).normalize().scale(0.82D);
        this.setDeltaMovement(direction.x, 0.48D, direction.z);
        this.hasImpulse = true;
        this.pounceCooldown = 70 + this.random.nextInt(35);
        this.triggerAnim("pounce_controller", "pounce");
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                this.getX(), this.getY() + 0.2D, this.getZ(),
                10, 0.35D, 0.1D, 0.35D, 0.02D);
        level.playSound(null, this.blockPosition(), ModSounds.ABYSS_LURKER_POUNCE.get(),
                SoundSource.HOSTILE, 0.65F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ABYSS_LURKER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ABYSS_LURKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ABYSS_LURKER_DEATH.get();
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
