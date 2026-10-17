package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.EcologyMeleePresentation;
import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.FlankTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 镜尘假像:镜湖/镜尘回廊外围的普通幻象敌怪。 */
public class MirrorDustDecoy extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_decoy.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_decoy.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_decoy.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SHATTER_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_decoy.shatter", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final int SHATTER_FADE_START_TICKS = 4;
    private static final int SHATTER_FADE_END_TICKS = 19;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final EcologyMeleePresentation<MirrorDustDecoy> meleePresentation =
            new EcologyMeleePresentation<>(this, 16);

    public MirrorDustDecoy(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }

    /**
     * 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。
     * 1 点血的幻象不加残血脱离——它没有"残血"这一档,任何一击都是致死。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlankTargetGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 9.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 12.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isAlive() && this.tickCount % 16 == 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.WITCH,
                    this.getX(), this.getY() + 0.9D, this.getZ(),
                    1, 0.18D, 0.24D, 0.18D, 0.0D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !this.meleePresentation.canAttack(living)) {
            return false;
        }
        LivingEntity navigationTarget = this.getTarget();
        boolean hit = super.doHurtTarget(target);
        if (!hit || this.getTarget() != navigationTarget || !this.meleePresentation.canAttack(living)) {
            return hit;
        }
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35, 0), this);
        if (this.getTarget() == navigationTarget && this.meleePresentation.canAttack(living)
                && this.meleePresentation.begin(living)) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF,
                    target.getX(), target.getY() + 0.8D, target.getZ(),
                    6, 0.25D, 0.28D, 0.25D, 0.02D);
            this.playSound(ModSounds.MIRROR_DUST_DECOY_STRIKE.get(), 0.7F, 1.0F);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        this.meleePresentation.tick();
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (wasDead || !this.dead) {
            return;
        }
        if (!this.level().isClientSide) {
            this.meleePresentation.tick();
            this.triggerAnim("shatter_controller", "shatter");
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.WHITE_ASH,
                    this.getX(), this.getY() + 0.7D, this.getZ(),
                    18, 0.35D, 0.35D, 0.35D, 0.025D);
            this.playSound(SoundEvents.GLASS_BREAK, 0.8F,
                    1.25F + this.getRandom().nextFloat() * 0.2F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIRROR_DUST_DECOY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIRROR_DUST_DECOY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIRROR_DUST_DECOY_DEATH.get();
    }

    /** 碎裂先保持可见，再于原版第 20 tick 移除前淡尽；本体和发光层共用。 */
    public float shatterVisibility(float partialTick) {
        if (this.deathTime <= 0) {
            return 1.0F;
        }
        float progress = Mth.clamp((this.deathTime + Mth.clamp(partialTick, 0.0F, 1.0F)
                - SHATTER_FADE_START_TICKS) / (SHATTER_FADE_END_TICKS - SHATTER_FADE_START_TICKS),
                0.0F, 1.0F);
        return 1.0F - progress * progress * (3.0F - 2.0F * progress);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                this.isAlive() ? state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)
                        : PlayState.STOP));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
        controllers.add(new AnimationController<>(this, "shatter_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("shatter", SHATTER_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
