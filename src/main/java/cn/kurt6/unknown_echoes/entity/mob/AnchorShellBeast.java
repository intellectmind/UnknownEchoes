package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/** 锚壳兽:只在受击后反击,并会在空间锚与稳定器附近安静休眠。 */
public class AnchorShellBeast extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.anchor_shell_beast.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.anchor_shell_beast.walk");
    private static final RawAnimation PULSE_ANIM =
            RawAnimation.begin().then("animation.anchor_shell_beast.pulse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pulseCooldown;
    private boolean nearAnchor;

    public AnchorShellBeast(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AnchorRestGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.65D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.pulseCooldown > 0) {
            this.pulseCooldown--;
        }
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            this.nearAnchor = this.scanForAnchor();
        }
        if (!this.level().isClientSide && this.nearAnchor && this.getTarget() == null) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.2D, movement.y, movement.z * 0.2D);
        }
    }

    private boolean scanForAnchor() {
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 2, 5))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(ModBlocks.SPACE_ANCHOR.get()) || state.is(ModBlocks.VOID_STABILIZER.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.pulseCooldown == 0) {
            this.pulseCooldown = 100;
            this.releaseAnchorPulse((ServerLevel) this.level());
        }
        return hurt;
    }

    private void releaseAnchorPulse(ServerLevel level) {
        this.triggerAnim("pulse_controller", "pulse");
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 0.55D, this.getZ(),
                42, 1.5D, 0.45D, 1.5D, 0.08D);
        level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.NEUTRAL, 1.2F, 0.7F);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.5D), entity -> entity != this && entity.isAlive())) {
            Vec3 offset = entity.position().subtract(this.position());
            double distance = offset.length();
            if (distance < 0.1D) {
                continue;
            }
            double force = distance <= 2.5D ? 0.85D : -0.42D;
            Vec3 impulse = offset.normalize().scale(force);
            entity.push(impulse.x, 0.2D, impulse.z);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ARMADILLO_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMADILLO_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMADILLO_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "pulse_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("pulse", PULSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private class AnchorRestGoal extends Goal {
        private AnchorRestGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return AnchorShellBeast.this.nearAnchor && AnchorShellBeast.this.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            AnchorShellBeast.this.getNavigation().stop();
        }
    }
}
