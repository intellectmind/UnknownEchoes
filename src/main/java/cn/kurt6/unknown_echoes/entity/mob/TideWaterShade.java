package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import cn.kurt6.unknown_echoes.entity.EcologyMeleePresentation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** 水影:潮汐执灯者的灯光召唤物。 */
public class TideWaterShade extends Monster implements GeoEntity {

    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.tide_water_shade.swim");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.tide_water_shade.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FADE_ANIM =
            RawAnimation.begin().then("animation.tide_water_shade.fade", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final EntityDataAccessor<Integer> FADE_TICKS =
            SynchedEntityData.defineId(TideWaterShade.class, EntityDataSerializers.INT);
    private static final int FADE_DURATION_TICKS = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final EcologyMeleePresentation<TideWaterShade> meleePresentation =
            new EcologyMeleePresentation<>(this, 9);

    /** 寿命:15 秒后自行消散。 */
    private static final int LIFESPAN_TICKS = 300;
    /** 离执灯者多远算"脱离灯光范围"；Boss 侧用同一范围统计并清理所属水影。 */
    public static final double OWNER_TETHER_RANGE = 24.0D;

    private UUID ownerUuid = null;
    private int lifeTicks = LIFESPAN_TICKS;

    public TideWaterShade(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.7F, 0.6F, true);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FADE_TICKS, -1);
    }

    public boolean isFading() {
        return this.entityData.get(FADE_TICKS) >= 0;
    }

    public float fadeVisibility(float partialTick) {
        int remaining = this.entityData.get(FADE_TICKS);
        return remaining < 0 ? 1.0F : Mth.clamp((remaining - partialTick) / FADE_DURATION_TICKS, 0.0F, 1.0F);
    }

    public void bindOwner(TideLanternKeeper owner) {
        this.ownerUuid = owner.getUUID();
    }

    public boolean isOwnedBy(TideLanternKeeper owner) {
        return owner != null && owner.getUUID().equals(this.ownerUuid);
    }

    /** 开始消散(执灯者死亡 / 寿命耗尽):短暂淡出后移除。 */
    public void beginFade() {
        if (!this.level().isClientSide && this.isAlive() && !this.isFading()) {
            this.entityData.set(FADE_TICKS, FADE_DURATION_TICKS);
            this.lifeTicks = 0;
            this.setNoAi(true);
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            this.meleePresentation.tick();
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8D, 30));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        if (type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isFading()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return this.lifeTicks > 0 && !this.isFading() && super.canAttack(target);
    }

    @Override
    public void aiStep() {
        // 寿命和归属先于 Goal 调度结算，消散开始的这一 tick 就不能再追击或伤害。
        if (this.level() instanceof ServerLevel serverLevel && this.isAlive() && !this.isFading()) {
            if (--this.lifeTicks <= 0) {
                this.beginFade();
            } else if (this.tickCount % 20 == 0) {
                TideLanternKeeper owner = this.findOwner(serverLevel);
                boolean ownerDefinitelyMissing = owner == null
                        && (this.ownerUuid == null || this.isOwnerResolutionAreaLoaded(serverLevel));
                if (this.level() == serverLevel && this.isAlive()
                        && (ownerDefinitelyMissing || owner != null && (!owner.isAlive()
                        || this.distanceToSqr(owner) > OWNER_TETHER_RANGE * OWNER_TETHER_RANGE))) {
                    this.beginFade();
                }
            }
        }
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.isAlive()) {
            return;
        }

        if (this.isFading()) {
            if (this.tickCount % 4 == 0) {
                serverLevel.sendParticles(
                        cn.kurt6.unknown_echoes.entity.boss.BossFx.waterBubbleOr(
                                serverLevel, this.position(), ParticleTypes.SPLASH),
                        this.getX(), this.getY() + 0.6D, this.getZ(), 2, 0.12D, 0.25D, 0.12D, 0.01D);
            }
            int remaining = Math.max(0, this.entityData.get(FADE_TICKS) - 1);
            this.entityData.set(FADE_TICKS, remaining);
            if (remaining <= 0) {
                this.discard();
            }
            return;
        }
        if (this.getTarget() != null && !this.meleePresentation.canAttack(this.getTarget())) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
        if (this.tickCount % 6 == 0) {

            serverLevel.sendParticles(
                    cn.kurt6.unknown_echoes.entity.boss.BossFx.waterBubbleOr(
                            serverLevel, this.position(), ParticleTypes.SPLASH),
                    this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.15, 0.25, 0.15, 0.0);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !this.meleePresentation.canAttack(living)) {
            return false;
        }
        LivingEntity navigationTarget = this.getTarget();
        boolean hit = super.doHurtTarget(target);
        if (hit && this.getTarget() == navigationTarget && this.meleePresentation.canAttack(living)
                && this.meleePresentation.begin(living)) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            serverLevel.sendParticles(
                    cn.kurt6.unknown_echoes.entity.boss.BossFx.waterBubbleOr(
                            serverLevel, living.position(), ParticleTypes.SPLASH),
                    living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                    3, 0.16D, 0.20D, 0.16D, 0.02D);
            this.playSound(ModSounds.TIDE_WATER_SHADE_STRIKE.get(), 0.55F, 1.0F);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        this.meleePresentation.tick();
    }

    private TideLanternKeeper findOwner(ServerLevel level) {
        if (this.ownerUuid == null) {
            return null;
        }
        return level.getEntity(this.ownerUuid) instanceof TideLanternKeeper keeper ? keeper : null;
    }

    private boolean isOwnerResolutionAreaLoaded(ServerLevel level) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, this.blockPosition(),
                (int) Math.ceil(OWNER_TETHER_RANGE) + 2);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TIDE_WATER_SHADE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TIDE_WATER_SHADE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putInt("FadeTicks", this.entityData.get(FADE_TICKS));
        if (this.ownerUuid != null) {
            tag.putUUID("OwnerUuid", this.ownerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.contains("LifeTicks")
                ? Math.max(0, Math.min(tag.getInt("LifeTicks"), LIFESPAN_TICKS))
                : LIFESPAN_TICKS;
        this.ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        this.entityData.set(FADE_TICKS, tag.contains("FadeTicks")
                ? Mth.clamp(tag.getInt("FadeTicks"), -1, FADE_DURATION_TICKS) : -1);
        if (this.isFading()) {
            this.lifeTicks = 0;
            this.setNoAi(true);
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> this.isFading() ? PlayState.STOP : state.setAndContinue(SWIM_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
        controllers.add(new AnimationController<>(this, "state_controller", 0,
                state -> this.isFading() ? state.setAndContinue(FADE_ANIM) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
