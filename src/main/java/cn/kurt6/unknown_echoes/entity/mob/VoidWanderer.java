package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.entity.ai.FlankTargetGoal;
import cn.kurt6.unknown_echoes.entity.ai.RetreatWhenLowGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 虚空漫游者:以经过完整碰撞检查的短距离相位移动切入战斗。 */
public class VoidWanderer extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.void_wanderer.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.void_wanderer.walk");
    private static final RawAnimation PHASE_ANIM =
            RawAnimation.begin().then("animation.void_wanderer.phase", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int phaseCooldown = 35;

    public VoidWanderer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    /** 战术 Goal 必须排在近战之前:MOVE 只能被更小的优先级数抢走,排在后面等于永不触发。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RetreatWhenLowGoal(this, 0.25F, 1.15D));
        this.goalSelector.addGoal(2, new FlankTargetGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AlertPackGoal(this, 14.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.tickCount % 18 == 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                    3, 0.42D, 0.65D, 0.42D, 0.02D);
        }
        if (this.phaseCooldown > 0) {
            this.phaseCooldown--;
        }
        if (this.level().isClientSide || this.phaseCooldown > 0) {
            return;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && this.phaseCooldown == 0
                && target instanceof LivingEntity living
                && this.getSensing().hasLineOfSight(living)) {
            this.tryPhase((ServerLevel) this.level(), living);
            this.phaseCooldown = 110 + this.random.nextInt(30);
        }
        return hit;
    }

    private void tryPhase(ServerLevel level, LivingEntity target) {
        Vec3 origin = this.position();
        Vec3 destination = this.findSafePhaseDestination(level, target);
        if (destination == null) {
            level.sendParticles(ParticleTypes.SMOKE,
                    origin.x, origin.y + this.getBbHeight() * 0.5D, origin.z,
                    8, 0.35D, 0.45D, 0.35D, 0.02D);
            return;
        }
        this.triggerAnim("phase_controller", "phase");
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + this.getBbHeight() * 0.5D, origin.z,
                24, 0.55D, 0.85D, 0.55D, 0.08D);
        this.teleportTo(destination.x, destination.y, destination.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true;
        level.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + this.getBbHeight() * 0.5D, destination.z,
                32, 0.6D, 0.9D, 0.6D, 0.12D);
        level.playSound(null, this.blockPosition(), ModSounds.VOID_WANDERER_PHASE.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private Vec3 findSafePhaseDestination(ServerLevel level, LivingEntity target) {
        double baseAngle = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX());
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = baseAngle + Math.PI
                    + (this.random.nextDouble() - 0.5D) * Math.PI * 1.5D;
            double distance = 2.8D + this.random.nextDouble() * 3.8D;
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            for (int dy = 2; dy >= -3; dy--) {
                double y = target.getY() + dy;
                BlockPos floor = BlockPos.containing(x, y - 0.1D, z);
                if (!level.isLoaded(floor)
                        || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    continue;
                }
                AABB movedBox = this.getBoundingBox().move(
                        x - this.getX(), y - this.getY(), z - this.getZ());
                if (level.getWorldBorder().isWithinBounds(movedBox)
                        && level.noCollision(this, movedBox)) {
                    return new Vec3(x, y, z);
                }
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.VOID_WANDERER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VOID_WANDERER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VOID_WANDERER_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "phase_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("phase", PHASE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
