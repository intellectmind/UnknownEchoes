package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.RetreatWhenLowGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

/** 晶噪灵:晶歌林地敌对干扰生物。 */
public class CrystalNoiseWisp extends Monster implements GeoEntity {
    private static final int ATTACK_WINDUP_TICKS = 8;
    private static final int ATTACK_RECOVERY_TICKS = 8;
    private static final int STAGGER_COOLDOWN_TICKS = 10;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.crystal_noise_wisp.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.crystal_noise_wisp.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.crystal_noise_wisp.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STAGGER_ANIM =
            RawAnimation.begin().then("animation.crystal_noise_wisp.stagger", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pendingTargetId = -1;
    private int attackWindupTicks;
    private int attackRecoveryTicks;
    private int staggerCooldownTicks;

    public CrystalNoiseWisp(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }

    /** 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RetreatWhenLowGoal(this, 0.3F, 1.25D,
                () -> this.attackWindupTicks > 0));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 12.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 movement = this.getDeltaMovement();
        double drift = Math.sin((this.tickCount + this.getId()) * 0.11D) * 0.018D;
        this.setDeltaMovement(movement.x, drift, movement.z);
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.staggerCooldownTicks > 0) {
            this.staggerCooldownTicks--;
        }
        if (this.attackRecoveryTicks > 0) {
            this.attackRecoveryTicks--;
        }
        if (this.attackWindupTicks > 0 && --this.attackWindupTicks == 0) {
            this.releaseDiscordPulse(serverLevel);
        }
        if (this.tickCount % 18 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.55D, this.getZ(),
                    2, 0.22D, 0.16D, 0.22D, 0.01D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide || !target.isAlive()
                || this.attackWindupTicks > 0 || this.attackRecoveryTicks > 0) {
            return false;
        }
        this.pendingTargetId = target.getId();
        this.attackWindupTicks = ATTACK_WINDUP_TICKS;
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", "attack");
        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F,
                0.7F + this.getRandom().nextFloat() * 0.12F);
        return true;
    }

    private void releaseDiscordPulse(ServerLevel level) {
        Entity target = this.pendingTargetId >= 0 ? level.getEntity(this.pendingTargetId) : null;
        this.pendingTargetId = -1;
        this.attackRecoveryTicks = ATTACK_RECOVERY_TICKS;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        double reach = 1.85D + living.getBbWidth() * 0.5D;
        if (this.distanceToSqr(living) > reach * reach || !this.getSensing().hasLineOfSight(living)
                || !super.doHurtTarget(living)) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
        double y = living.getY() + living.getBbHeight() * 0.55D;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                living.getX(), y, living.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.06D);
        level.sendParticles(ParticleTypes.END_ROD,
                living.getX(), y, living.getZ(), 4, 0.22D, 0.22D, 0.22D, 0.02D);
        this.playSound(ModSounds.CRYSTAL_NOISE_WISP_JAM.get(), 0.9F,
                0.52F + this.getRandom().nextFloat() * 0.15F);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (amount > 0.0F && !this.level().isClientSide && this.isAlive()
                && this.attackWindupTicks <= 0 && this.staggerCooldownTicks <= 0) {
            this.staggerCooldownTicks = STAGGER_COOLDOWN_TICKS;
            this.triggerAnim("attack_controller", "stagger");
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRYSTAL_NOISE_WISP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRYSTAL_NOISE_WISP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRYSTAL_NOISE_WISP_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("stagger", STAGGER_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
