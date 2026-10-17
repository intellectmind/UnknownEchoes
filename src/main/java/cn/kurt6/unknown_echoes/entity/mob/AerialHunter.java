package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.world.entity.MoverType;
import cn.kurt6.unknown_echoes.fx.MobAttackFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.EntityBodyCollision;
import net.minecraft.world.phys.AABB;
import cn.kurt6.unknown_echoes.entity.ai.AerialFlight;
import cn.kurt6.unknown_echoes.entity.ai.AerialMoveControl;
import cn.kurt6.unknown_echoes.entity.ai.AerialWanderGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import cn.kurt6.unknown_echoes.entity.ai.AerialSwoopGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public abstract class AerialHunter extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ATTACK_PRESENTATION =
            SynchedEntityData.defineId(AerialHunter.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation flyAnimation;
    private final RawAnimation attackAnimation;
    private AerialSwoopGoal swoopGoal;

    protected AerialHunter(EntityType<? extends Monster> type, Level level, String id) {
        super(type, level);
        this.flyAnimation = RawAnimation.begin().thenLoop("animation." + id + ".fly");
        this.attackAnimation = RawAnimation.begin().thenPlay("animation." + id + ".attack");
        this.moveControl = new AerialMoveControl(this);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.xpReward = 4;
    }

    public void signalSwoop() {
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
        if (!this.level().isClientSide && MobAttackFx.color(this) >= 0 && this.attackPresentation() != phase) {
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
    protected PathNavigation createNavigation(Level level) {
        return AerialFlight.navigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.swoopGoal = new AerialSwoopGoal(this);
        this.goalSelector.addGoal(1, this.swoopGoal);
        this.goalSelector.addGoal(4, new AerialWanderGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi()) {
            AerialFlight.travel(this);
            this.calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4,
                state -> state.setAndContinue(this.flyAnimation)));
        // 释放帧按服务端前摇计时，额外过渡会推迟整个攻击片段。
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> software.bernie.geckolib.animation.PlayState.STOP)
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

    /** 身体朝向可能由原版控制器直接更新，tick 结束后同步长方形判定。 */
    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive() || this.isNoAi()) {
            this.cancelAttackPresentation();
        }
        EntityHitboxProfiles.refresh(this);
    }

    /** 控制器可直接改变身体朝向，移动碰撞必须先读取当前长宽。 */
    @Override
    public void move(MoverType type, Vec3 movement) {
        EntityHitboxProfiles.refresh(this);
        AABB before = this.getBoundingBox();
        super.move(type, movement);
        if (type == MoverType.SELF && this.swoopGoal != null) {
            this.swoopGoal.onMovement(before, this.getBoundingBox());
        }
    }
}
