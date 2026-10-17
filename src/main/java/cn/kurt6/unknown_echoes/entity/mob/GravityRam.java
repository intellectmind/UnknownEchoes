package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.KeepDistanceGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

/** 重力角兽:以锚角压缩距离并撞开目标的重装敌怪。 */
public class GravityRam extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.gravity_ram.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.gravity_ram.walk");
    private static final RawAnimation CHARGE_ANIM =
            RawAnimation.begin().then("animation.gravity_ram.charge", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int chargeCooldown = 50;

    public GravityRam(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 55.0D)
                .add(Attributes.ARMOR, 7.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 25.0D);
    }

    /**
     * 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。
     * 后撤距离取 4.5 格,低于 aiStep 冲撞的下限(距离平方 20),否则退开就立刻满足冲撞条件来回抽搐。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KeepDistanceGoal(this, 4.5D, 0.95D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.08D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.72D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 12.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.isAlive() || this.isNoAi()) {
            return;
        }
        if (this.chargeCooldown > 0) {
            --this.chargeCooldown;
        }
        if (this.level().isClientSide || this.chargeCooldown > 0 || !this.onGround()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || target.level() != this.level()
                || !this.canAttack(target)) {
            return;
        }
        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr < 20.0D || distanceSqr > 110.0D
                || !this.getSensing().hasLineOfSight(target)) {
            return;
        }
        Vec3 direction = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(direction.x * 0.95D, 0.16D, direction.z * 0.95D);
        this.hasImpulse = true;
        this.chargeCooldown = 95 + this.random.nextInt(45);
        this.triggerAnim("charge_controller", "charge");
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY() + 0.45D, this.getZ(), 18, 0.6D, 0.25D, 0.6D, 0.08D);
        level.playSound(null, this.blockPosition(), ModSounds.GRAVITY_RAM_CHARGE.get(),
                SoundSource.HOSTILE, 0.75F, 1.0F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 away = living.position().subtract(this.position());
            living.knockback(1.35D, -away.x, -away.z);
            if (!this.level().isClientSide) {
                this.triggerAnim("charge_controller", "charge");
            }
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GRAVITY_RAM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GRAVITY_RAM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GRAVITY_RAM_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "charge_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("charge", CHARGE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
