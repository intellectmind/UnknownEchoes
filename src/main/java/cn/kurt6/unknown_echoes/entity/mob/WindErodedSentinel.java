package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
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
 * 风蚀哨兵:漂浮群岛的普通敌怪。
 * 风刃只造成普通伤害与击退,风之能力和结构权限仍由服务端进度系统判定。
 */
public class WindErodedSentinel extends Zombie implements GeoEntity {
    private static final int WIND_SLASH_COOLDOWN = 70;
    private static final double MIN_SLASH_DISTANCE = 3.0D;
    private static final double MAX_SLASH_DISTANCE = 14.0D;
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.wind_eroded_sentinel.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.wind_eroded_sentinel.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.wind_eroded_sentinel.attack", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int windSlashCooldown = WIND_SLASH_COOLDOWN;

    public WindErodedSentinel(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
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
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.windSlashCooldown > 0) {
            this.windSlashCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.hasLineOfSight(target)) {
            double distance = this.distanceTo(target);
            if (distance >= MIN_SLASH_DISTANCE && distance <= MAX_SLASH_DISTANCE) {
                performWindSlash(target);
                this.windSlashCooldown = WIND_SLASH_COOLDOWN + this.getRandom().nextInt(25);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            if (target instanceof LivingEntity living) {
                living.knockback(0.45D, this.getX() - living.getX(), this.getZ() - living.getZ());
            }
            this.playSound(SoundEvents.PHANTOM_FLAP, 0.8F,
                    0.75F + this.getRandom().nextFloat() * 0.2F);
            this.triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    private void performWindSlash(LivingEntity target) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        target.hurt(this.damageSources().mobAttack(this), 4.0F);
        target.knockback(0.70D, this.getX() - target.getX(), this.getZ() - target.getZ());
        ServerLevel serverLevel = (ServerLevel) this.level();
        double midX = (this.getX() + target.getX()) * 0.5D;
        double midY = target.getY() + target.getBbHeight() * 0.55D;
        double midZ = (this.getZ() + target.getZ()) * 0.5D;
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                midX, midY, midZ, 16,
                0.55D, 0.18D, 0.55D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                2, 0.25D, 0.12D, 0.25D, 0.0D);
        this.playSound(SoundEvents.PHANTOM_FLAP, 0.9F, 0.85F + this.getRandom().nextFloat() * 0.2F);
        this.triggerAnim("attack_controller", "attack");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DEEPSLATE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DEEPSLATE_BREAK;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.DEEPSLATE_STEP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
