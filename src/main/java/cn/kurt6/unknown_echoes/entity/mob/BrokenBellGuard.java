package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
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

/** 残钟守卫:残钟荒原常驻敌对生物。 */
public class BrokenBellGuard extends Zombie implements GeoEntity {
    private static final int ATTACK_WINDUP_TICKS = 12;
    private static final int ATTACK_RECOVERY_TICKS = 10;
    private static final int STAGGER_COOLDOWN_TICKS = 12;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.broken_bell_guard.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.broken_bell_guard.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.broken_bell_guard.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STAGGER_ANIM =
            RawAnimation.begin().then("animation.broken_bell_guard.stagger", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pendingTargetId = -1;
    private int attackWindupTicks;
    private int attackRecoveryTicks;
    private int staggerCooldownTicks;

    public BrokenBellGuard(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean canBreakDoors() {
        return false;
    }

    /**
     * 覆写 Zombie 的行为组:去掉村庄穿行(本 Mod 无村民),补同族拉群。
     * 不加侧翼包夹:抬手期间自身会 stop 导航,再叠一个抢 MOVE 的 Goal 会打断重锤前摇。
     */
    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 16.0D));
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
        this.playSound(ModSounds.BROKEN_BELL_GUARD_TOLL.get(), 0.85F,
                0.48F + this.getRandom().nextFloat() * 0.08F);
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.staggerCooldownTicks > 0) {
            this.staggerCooldownTicks--;
        }
        if (this.attackRecoveryTicks > 0) {
            this.attackRecoveryTicks--;
        }
        if (this.level().isClientSide || this.attackWindupTicks <= 0
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 1.0D, 0.3D));
        if (--this.attackWindupTicks == 0) {
            this.releaseHammerBlow(serverLevel);
        }
    }

    private void releaseHammerBlow(ServerLevel level) {
        Entity target = this.pendingTargetId >= 0 ? level.getEntity(this.pendingTargetId) : null;
        this.pendingTargetId = -1;
        this.attackRecoveryTicks = ATTACK_RECOVERY_TICKS;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        double reach = 2.25D + living.getBbWidth() * 0.5D;
        if (this.distanceToSqr(living) > reach * reach || !this.getSensing().hasLineOfSight(living)) {
            return;
        }

        if (!super.doHurtTarget(living)) {
            return;
        }
        living.knockback(0.65D, this.getX() - living.getX(), this.getZ() - living.getZ());
        double impactY = living.getY() + living.getBbHeight() * 0.45D;
        level.sendParticles(ParticleTypes.CRIT,
                living.getX(), impactY, living.getZ(), 9, 0.35D, 0.25D, 0.35D, 0.08D);
        level.sendParticles(ParticleTypes.NOTE,
                living.getX(), impactY + 0.2D, living.getZ(), 3, 0.3D, 0.15D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                living.getX(), impactY, living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, living.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.9F, 0.72F + this.getRandom().nextFloat() * 0.08F);
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
    protected SoundEvent getAmbientSound() {
        return ModSounds.BROKEN_BELL_GUARD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BROKEN_BELL_GUARD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BROKEN_BELL_GUARD_DEATH.get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.CHAIN_STEP;
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
