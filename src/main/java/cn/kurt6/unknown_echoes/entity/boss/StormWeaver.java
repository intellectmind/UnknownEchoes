package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StormWeaver extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("storm_weaver");

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.fly");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BLADES_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.blades", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CYCLONE_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.cyclone", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DIVE_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.dive", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.broken");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(StormWeaver.class, EntityDataSerializers.BOOLEAN);

    private static final int ARENA_RADIUS = 14;
    /** 悬空高度区间(相对场地锚点)。 */
    private static final int HOVER_MIN = 5;
    private static final int HOVER_MAX = 9;

    private static final int BLADE_TELEGRAPH_TICKS = 16;
    private static final double BLADE_MAX_DIST = 16.0D;
    /** 风刃的立方判定半宽。 */
    private static final double BLADE_HIT_HALF_WIDTH = 1.2D;

    private static final int DIVE_TELEGRAPH_TICKS = 24;
    private static final int BROKEN_WINDOW_TICKS = 120;
    /** 俯冲落地的结算半径。 */
    private static final double DIVE_IMPACT_RADIUS = 3.0D;

    private static final int CYCLONE_LIFETIME = 100;
    private static final double CYCLONE_RADIUS = 2.2D;

    private int bladeCooldown = 60;
    private int cycloneCooldown = 140;
    private int diveCooldown = 200;

    private int bladeTelegraphTicks = 0;
    private Vec3 bladeDir = Vec3.ZERO;
    private Vec3 bladeOrigin = Vec3.ZERO;
    /** 风刃浪涌推进距离,-1 = 未激活。 */
    private double bladeWaveDist = -1.0D;
    private int bladeWavesLeft = 0;
    private final Set<UUID> bladeHitVictims = new HashSet<>();

    private int diveTelegraphTicks = 0;
    private Vec3 diveMark = null;
    private boolean diving = false;
    private int brokenTicksRemaining = 0;

    /** 在场旋风:位置 + 剩余 tick。 */
    private final List<Cyclone> cyclones = new ArrayList<>();

    private Vec3 hoverWaypoint = null;
    private int waypointTicks = 0;

    private static class Cyclone {
        final Vec3 pos;
        int ticks;

        Cyclone(Vec3 pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    public StormWeaver(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        this.xpReward = 30;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BROKEN, false);
    }

    public boolean isBroken() {
        return this.entityData.get(DATA_BROKEN);
    }

    private void setBroken(boolean broken) {
        this.entityData.set(DATA_BROKEN, broken);
    }

    @Override
    protected ResourceLocation minibossId() {
        return MINIBOSS_ID;
    }

    @Override
    protected int arenaRadius() {
        return ARENA_RADIUS;
    }

    @Override
    protected boolean isDamageGuarded() {

        return !this.isBroken();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.storm_weaver.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        if (this.diveMark == null) {
            return;
        }
        // 俯冲前示意：地上画环 + 从天空到落点画一条连线。
        Vec3 marker = this.diveMark.add(0, 0.4, 0);
        MiniBossFx.objectiveMarker(level, marker, this.fxTheme(), level.getGameTime());
        MiniBossFx.objectiveLink(level, marker, this.position().add(0, 0.6, 0),
                this.fxTheme(), level.getGameTime());
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantStormWeaver(this, firstKill);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isSpawnerBindingPaused()) {
            return;
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.isBroken()) {
            this.tickBrokenWindow(serverLevel);
            return;
        }

        this.tickHover(serverLevel);
        this.tickCyclones(serverLevel);
        this.tickSpecialAttacks(serverLevel);
    }

    /** 风流追击:围绕场地锚点高空盘旋,周期性换悬空航点并与目标保持距离。 */
    private void tickHover(ServerLevel serverLevel) {
        if (--this.waypointTicks > 0 && this.hoverWaypoint != null
                && this.distanceToSqr(this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z) > 4.0) {
            this.getMoveControl().setWantedPosition(
                    this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z, 1.0);
            return;
        }
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        LivingEntity target = this.getTarget();

        double baseAngle = target != null
                ? Math.atan2(anchor.z - target.getZ(), anchor.x - target.getX())
                : this.random.nextDouble() * Math.PI * 2;
        double angle = baseAngle + (this.random.nextDouble() - 0.5) * Math.PI * 0.8;
        double dist = 4.0 + this.random.nextDouble() * (ARENA_RADIUS - 6);
        double height = HOVER_MIN + this.random.nextDouble() * (HOVER_MAX - HOVER_MIN);
        this.hoverWaypoint = new Vec3(
                anchor.x + Math.cos(angle) * dist,
                anchor.y + height,
                anchor.z + Math.sin(angle) * dist);
        this.waypointTicks = 40 + this.random.nextInt(30);
        this.getMoveControl().setWantedPosition(
                this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z, 1.0);
    }

    private boolean isTurbulentPhase() {
        return this.getHealth() < this.getMaxHealth() * 0.3F;
    }

    private int scaleCooldown(int base) {
        return this.isTurbulentPhase() ? (int) (base * 0.7F) : base;
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.bladeCooldown > 0) this.bladeCooldown--;
        if (this.cycloneCooldown > 0) this.cycloneCooldown--;
        if (this.diveCooldown > 0) this.diveCooldown--;

        if (this.bladeTelegraphTicks > 0) {
            this.tickBladeTelegraph(serverLevel);
            if (--this.bladeTelegraphTicks == 0) {
                this.bladeWaveDist = 1.0D;
                this.bladeHitVictims.clear();
            }
            return;
        }
        if (this.bladeWaveDist >= 0) {
            this.tickBladeWave(serverLevel);
            return;
        }

        if (this.diveTelegraphTicks > 0) {
            this.tickDiveTelegraph(serverLevel);
            if (--this.diveTelegraphTicks == 0) {
                this.diving = true;
                this.playSound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.7F);
            }
            return;
        }
        if (this.diving) {
            this.tickDive(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.diveCooldown <= 0) {
            this.startDive(serverLevel, target);
        } else if (this.bladeCooldown <= 0 && distSq <= BLADE_MAX_DIST * BLADE_MAX_DIST
                && this.hasLineOfSight(target)) {
            this.startBladeVolley(target);
        } else if (this.cycloneCooldown <= 0) {
            this.castCyclones(serverLevel, target);
        }
    }

    private void startBladeVolley(LivingEntity target) {
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position()).normalize();
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        this.bladeDir = dir;
        this.bladeOrigin = this.position().add(0, this.getBbHeight() * 0.5, 0);
        this.bladeTelegraphTicks = BLADE_TELEGRAPH_TICKS;
        this.bladeWavesLeft = 3;
        this.bladeCooldown = scaleCooldown(110);
        this.triggerAnim("attack_controller", "blades");
        this.playSound(SoundEvents.BREEZE_CHARGE, 1.6F, 0.8F);
    }

    private void tickBladeTelegraph(ServerLevel serverLevel) {
        // 预警画的就是刃波将要走的那条线，玩家照着它侧移才躲得开。
        MiniBossFx.pierceBeam(serverLevel, this.bladeOrigin, this.bladeDir,
                this.fxTheme(), BLADE_MAX_DIST);
    }

    private void tickBladeWave(ServerLevel serverLevel) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        if (this.bladeWaveDist <= 1.0D) {
            BossFx.cinematicImpact(serverLevel, this.bladeOrigin, 22.0D,
                    0.24F, 4, this.combatImpactColor());
        }
        for (int seg = 0; seg < 2 && this.bladeWaveDist >= 0; seg++) {
            Vec3 p = this.bladeOrigin.add(this.bladeDir.scale(this.bladeWaveDist));
            // 刃波是逐帧推进的立方判定，环画在半宽上让玩家读出"这一段现在危险"。
            MiniBossFx.impactZone(serverLevel, p, this.fxTheme(), BLADE_HIT_HALF_WIDTH,
                    this.bladeWaveDist / BLADE_MAX_DIST);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(p.x - BLADE_HIT_HALF_WIDTH, p.y - BLADE_HIT_HALF_WIDTH,
                            p.z - BLADE_HIT_HALF_WIDTH, p.x + BLADE_HIT_HALF_WIDTH,
                            p.y + BLADE_HIT_HALF_WIDTH, p.z + BLADE_HIT_HALF_WIDTH),
                    e -> e != this && !e.isSpectator() && !(e instanceof Enemy)
                            && !this.bladeHitVictims.contains(e.getUUID()))) {
                if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                    MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                }
                this.bladeHitVictims.add(victim.getUUID());
            }
            this.bladeWaveDist += 1.6D;
            if (this.bladeWaveDist > BLADE_MAX_DIST) {

                if (--this.bladeWavesLeft > 0) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive()) {
                        this.bladeDir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                                .subtract(this.position()).normalize();
                        this.bladeOrigin = this.position().add(0, this.getBbHeight() * 0.5, 0);
                    }
                    this.bladeWaveDist = 1.0D;
                    this.bladeHitVictims.clear();
                    this.playSound(SoundEvents.BREEZE_SHOOT, 1.4F, 1.0F + (3 - this.bladeWavesLeft) * 0.15F);
                } else {
                    this.bladeWaveDist = -1.0D;
                }
            }
        }
    }

    /** 在目标与本体之间放 2-3 个短时旋风区:阻路推开,不造成围困。 */
    private void castCyclones(ServerLevel serverLevel, LivingEntity target) {
        int count = this.isTurbulentPhase() ? 3 : 2;
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        for (int i = 0; i < count; i++) {

            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 2.0 + this.random.nextDouble() * 3.0;
            Vec3 p = target.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            if (p.subtract(anchor).horizontalDistanceSqr() > ARENA_RADIUS * ARENA_RADIUS) {
                continue;
            }
            this.cyclones.add(new Cyclone(p, CYCLONE_LIFETIME));
            MiniBossFx.groundRing(serverLevel, p, this.fxTheme(), CYCLONE_RADIUS);
        }
        this.cycloneCooldown = scaleCooldown(200);
        this.triggerAnim("attack_controller", "cyclone");
        this.playSound(SoundEvents.BREEZE_IDLE_GROUND, 1.8F, 0.6F);
        BossFx.cinematicImpact(serverLevel, target.position().add(0, 0.6, 0),
                22.0D, 0.24F, 4, this.combatImpactColor());
    }

    private void tickCyclones(ServerLevel serverLevel) {
        if (this.cyclones.isEmpty()) {
            return;
        }
        this.cyclones.removeIf(cyclone -> --cyclone.ticks <= 0);
        for (Cyclone cyclone : this.cyclones) {
            if (cyclone.ticks % 4 == 0) {
                // 旋风是持续存在的推开区，环画在 CYCLONE_RADIUS 上标出它的边界。
                MiniBossFx.groundRing(serverLevel, cyclone.pos, this.fxTheme(), CYCLONE_RADIUS);
            }

            if (cyclone.ticks % 5 == 0) {
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        new AABB(cyclone.pos.x - CYCLONE_RADIUS, cyclone.pos.y - 0.5, cyclone.pos.z - CYCLONE_RADIUS,
                                cyclone.pos.x + CYCLONE_RADIUS, cyclone.pos.y + 3.0, cyclone.pos.z + CYCLONE_RADIUS),
                        e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
                    Vec3 away = victim.position().subtract(cyclone.pos).multiply(1, 0, 1);
                    Vec3 dir = away.lengthSqr() > 1.0E-4 ? away.normalize()
                            : new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.7, 0.25, dir.z * 0.7));
                    victim.hurtMarked = true;
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        }
    }

    private void startDive(ServerLevel serverLevel, LivingEntity target) {

        this.diveMark = target.position();
        this.diveTelegraphTicks = DIVE_TELEGRAPH_TICKS;
        this.diveCooldown = scaleCooldown(280);
        this.triggerAnim("attack_controller", "dive");
        this.playSound(SoundEvents.BREEZE_INHALE, 2.0F, 0.5F);
    }

    private void tickDiveTelegraph(ServerLevel serverLevel) {
        if (this.diveMark == null) {
            this.diveTelegraphTicks = 0;
            return;
        }
        // 俯冲预警画在落点，玩家根据它散开。
        MiniBossFx.groundRing(serverLevel, this.diveMark, this.fxTheme(), DIVE_IMPACT_RADIUS);
        // 从天空到落点画一条示意线，让玩家抬头看 Boss 也能意识到它是往哪冲的。
        if (this.tickCount % 3 == 0) {
            MiniBossFx.pierceBeam(serverLevel, this.position(),
                    this.diveMark.subtract(this.position()).normalize(),
                    this.fxTheme(), this.position().distanceTo(this.diveMark));
        }
    }

    private void tickDive(ServerLevel serverLevel) {
        if (this.diveMark == null) {
            this.diving = false;
            return;
        }
        Vec3 toMark = this.diveMark.subtract(this.position());
        if (toMark.length() <= 1.5 || this.onGround()) {
            this.landDive(serverLevel);
            return;
        }
        this.setDeltaMovement(toMark.normalize().scale(1.1));
        this.hurtMarked = true;
    }

    private void landDive(ServerLevel serverLevel) {
        this.diving = false;
        this.setDeltaMovement(Vec3.ZERO);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        // 落地冲击波，环画在结算半径上让玩家读出"散这么开就安全"。
        MiniBossFx.expandingRing(serverLevel, this.position(), this.fxTheme(),
                DIVE_IMPACT_RADIUS, 1.2D);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(DIVE_IMPACT_RADIUS, 1.5, DIVE_IMPACT_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > DIVE_IMPACT_RADIUS * DIVE_IMPACT_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 0.9, 0.4, dir.z * 0.9));
                    victim.hurtMarked = true;
                }
            }
        }
        this.setBroken(true);
        this.brokenTicksRemaining = BROKEN_WINDOW_TICKS;
        this.diveMark = null;

        MiniBossFx.expandingRing(serverLevel, this.position(), this.fxTheme(),
                DIVE_IMPACT_RADIUS * 1.5, 1.4D);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 0.4, 0),
                28.0D, 0.62F, 8, this.combatImpactColor());
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.STORM_WEAVER_SHATTER.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.storm_weaver.broken");
    }

    private void tickBrokenWindow(ServerLevel serverLevel) {
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.4, 0.4, 0.4));

        if (this.tickCount % 5 == 0) {
            MiniBossFx.breachAura(serverLevel, this, this.fxTheme(), serverLevel.getGameTime());
        }
        if (--this.brokenTicksRemaining <= 0 && this.isAlive()) {
            this.setBroken(false);
            this.waypointTicks = 0;
            this.playSound(SoundEvents.BREEZE_CHARGE, 1.8F, 1.2F);
        }
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                candidate -> candidate.distanceToSqr(this) <= radius * radius)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(key), true);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {

        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STORM_WEAVER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STORM_WEAVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STORM_WEAVER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Broken", this.isBroken());
        tag.putInt("BrokenTicks", this.brokenTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.brokenTicksRemaining = BossNbtData.clampTimer(
                tag.getInt("BrokenTicks"), BROKEN_WINDOW_TICKS);
        this.setBroken(tag.getBoolean("Broken") && this.brokenTicksRemaining > 0);
        if (!this.isBroken()) {
            this.brokenTicksRemaining = 0;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isBroken()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? FLY_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("blades", BLADES_ANIM)
                .triggerableAnim("cyclone", CYCLONE_ANIM)
                .triggerableAnim("dive", DIVE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
