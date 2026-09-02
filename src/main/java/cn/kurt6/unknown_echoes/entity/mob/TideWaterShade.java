package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** 水影:潮汐执灯者的灯光召唤物。 */
public class TideWaterShade extends Monster implements GeoEntity {

    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.tide_water_shade.swim");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 寿命:15 秒后自行消散。 */
    private static final int LIFESPAN_TICKS = 300;
    /** 离执灯者多远算"脱离灯光范围"；Boss 侧用同一范围统计并清理所属水影。 */
    public static final double OWNER_TETHER_RANGE = 24.0D;

    private UUID ownerUuid = null;
    private int lifeTicks = LIFESPAN_TICKS;
    private int fadeTicks = -1;

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

    public void bindOwner(TideLanternKeeper owner) {
        this.ownerUuid = owner.getUUID();
    }

    public boolean isOwnedBy(TideLanternKeeper owner) {
        return owner != null && owner.getUUID().equals(this.ownerUuid);
    }

    /** 开始消散(执灯者死亡 / 寿命耗尽):短暂淡出后移除。 */
    public void beginFade() {
        if (this.fadeTicks < 0) {
            this.fadeTicks = 20;
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
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.fadeTicks >= 0) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.6, this.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
            if (--this.fadeTicks <= 0) {
                this.discard();
            }
            return;
        }
        if (--this.lifeTicks <= 0) {
            this.beginFade();
            return;
        }

        if (this.tickCount % 20 == 0) {
            TideLanternKeeper owner = this.findOwner(serverLevel);
            boolean ownerDefinitelyMissing = owner == null
                    && (this.ownerUuid == null || this.isOwnerResolutionAreaLoaded(serverLevel));
            if (ownerDefinitelyMissing || owner != null && (!owner.isAlive()
                    || this.distanceToSqr(owner) > OWNER_TETHER_RANGE * OWNER_TETHER_RANGE)) {
                this.beginFade();
            }
        }
        if (this.tickCount % 6 == 0) {

            serverLevel.sendParticles(
                    cn.kurt6.unknown_echoes.entity.boss.BossFx.waterBubbleOr(
                            serverLevel, this.position(), ParticleTypes.SPLASH),
                    this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.15, 0.25, 0.15, 0.0);
        }
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(SWIM_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
