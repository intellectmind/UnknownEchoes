package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.EntityBodyCollision;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.ai.GroundRaiderAttackGoal;
import cn.kurt6.unknown_echoes.fx.MobAttackFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class HumanoidRaider extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ATTACK_PRESENTATION =
            SynchedEntityData.defineId(HumanoidRaider.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idleAnimation;
    private final RawAnimation walkAnimation;
    private final RawAnimation attackAnimation;
    private final int windupTicks;
    private final int recoveryTicks;

    protected HumanoidRaider(EntityType<? extends Monster> type, Level level, String id,
                             int windupTicks, int recoveryTicks) {
        super(type, level);
        this.idleAnimation = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        this.walkAnimation = RawAnimation.begin().thenLoop("animation." + id + ".walk");
        this.attackAnimation = RawAnimation.begin().thenPlay("animation." + id + ".attack");
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.xpReward = 5;
    }

    public int attackWindupTicks() {
        return this.windupTicks;
    }

    public int attackRecoveryTicks() {
        return this.recoveryTicks;
    }

    public void signalAttack() {
        this.attackPresentation(1);
        this.triggerAnim("attack_controller", "attack");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_PRESENTATION, 0);
    }

    public int attackPresentation() {
        return this.entityData.get(ATTACK_PRESENTATION);
    }

    public void attackPresentation(int phase) {
        if (!this.level().isClientSide && this.attackPresentation() != phase) {
            this.entityData.set(ATTACK_PRESENTATION, phase);
            MobAttackFx.present(this, phase);
        }
    }

    public void cancelAttackPresentation() {
        if (!this.level().isClientSide && this.attackPresentation() != 0) {
            this.attackPresentation(0);
            this.stopTriggeredAnim("attack_controller", "attack");
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GroundRaiderAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide || !this.isAlive() || this.isNoAi() || !(target instanceof LivingEntity living)
                || !living.isAlive() || living.level() != this.level() || !this.canAttack(living)
                || this.isAlliedTo(living)
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state ->
                state.setAndContinue(state.isMoving() ? this.walkAnimation : this.idleAnimation)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", this.attackAnimation));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean isInWall() {
        return EntityBodyCollision.isInWall(this);
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityHitboxProfiles.make(this);
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        EntityHitboxProfiles.refresh(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive() || this.isNoAi()) {
            this.cancelAttackPresentation();
        }
        EntityHitboxProfiles.refresh(this);
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        EntityHitboxProfiles.refresh(this);
        super.move(type, movement);
    }
}
