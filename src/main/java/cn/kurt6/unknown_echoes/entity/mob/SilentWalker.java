package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

/**
 * 沉默行者:失语沼泽常驻敌对生物。没有环境音,受击与死亡也只有被布料闷住般的轻响,
 * 所以只能靠看而不是靠听来发现它。
 */
public class SilentWalker extends Zombie implements GeoEntity {
    private static final int ATTACK_WINDUP_TICKS = 11;
    private static final int ATTACK_RECOVERY_TICKS = 12;
    private static final int STAGGER_COOLDOWN_TICKS = 10;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.silent_walker.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.silent_walker.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.silent_walker.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STAGGER_ANIM =
            RawAnimation.begin().then("animation.silent_walker.stagger", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pendingTargetId = -1;
    private int attackWindupTicks;
    private int attackRecoveryTicks;
    private int staggerCooldownTicks;

    public SilentWalker(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
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

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SILENT_WALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SILENT_WALKER_DEATH.get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.MUD_STEP;
    }

    /**
     * 覆写 Zombie 的行为组:去掉村庄穿行(本 Mod 无村民),补同族拉群。
     * 不加侧翼包夹:抬手期间自身会 stop 导航,再叠一个抢 MOVE 的 Goal 会打断啄击前摇。
     */
    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 14.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
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
            this.releaseBeakStrike(serverLevel);
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
        return true;
    }

    private void releaseBeakStrike(ServerLevel level) {
        Entity target = this.pendingTargetId >= 0 ? level.getEntity(this.pendingTargetId) : null;
        this.pendingTargetId = -1;
        this.attackRecoveryTicks = ATTACK_RECOVERY_TICKS;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        double reach = 2.35D + living.getBbWidth() * 0.5D;
        if (this.distanceToSqr(living) > reach * reach || !this.getSensing().hasLineOfSight(living)
                || !super.doHurtTarget(living)) {
            return;
        }
        int slownessTicks = ServerConfig.SILENT_WALKER_SLOWNESS_TICKS.get();
        if (slownessTicks > 0) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slownessTicks, 0), this);
        }
        double y = living.getY() + living.getBbHeight() * 0.55D;
        level.sendParticles(ParticleTypes.ASH,
                living.getX(), y, living.getZ(), 10, 0.32D, 0.28D, 0.32D, 0.015D);
        level.sendParticles(ParticleTypes.WHITE_ASH,
                living.getX(), y, living.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.01D);
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
