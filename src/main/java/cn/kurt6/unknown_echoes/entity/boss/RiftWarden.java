package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.BossLifecycleFx;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.VoidCoreAltarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.ai.CirclePreferredRangeGoal;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RiftWarden extends Monster implements GeoEntity, BossTestControl, PhaseGatedBoss, BossVisualState,
        DeepRechallengeBoundBoss, BossMusicSource {
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_MODE =
            SynchedEntityData.defineId(RiftWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_AGE =
            SynchedEntityData.defineId(RiftWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_DURATION =
            SynchedEntityData.defineId(RiftWarden.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.rift_warden.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.rift_warden.walk");
    private static final RawAnimation RIFT_SLASH =
            RawAnimation.begin().then("animation.rift_warden.rift_slash", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANCHOR_SLAM =
            RawAnimation.begin().then("animation.rift_warden.anchor_slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VOID_PULL =
            RawAnimation.begin().then("animation.rift_warden.void_pull", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SHIELD_BREAK =
            RawAnimation.begin().then("animation.rift_warden.shield_break", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RIFT_CHARGE =
            RawAnimation.begin().then("animation.rift_warden.rift_charge", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RIFT_STORM =
            RawAnimation.begin().then("animation.rift_warden.rift_storm", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AWAKEN =
            RawAnimation.begin().then("animation.rift_warden.awaken", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PHASE_LOCK =
            RawAnimation.begin().then("animation.rift_warden.phase_lock", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PHASE_OPEN =
            RawAnimation.begin().then("animation.rift_warden.phase_open", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STORM_CALL =
            RawAnimation.begin().then("animation.rift_warden.storm_call", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DEATH =
            RawAnimation.begin().then("animation.rift_warden.death", Animation.LoopType.PLAY_ONCE);

    private static final int RIFT_SLASH_DURATION = 32;
    private static final int ANCHOR_SLAM_DURATION = 38;
    private static final int VOID_PULL_DURATION = 44;
    private static final int SHIELD_BREAK_DURATION = 48;
    private static final int RIFT_CHARGE_DURATION = 36;
    private static final int RIFT_STORM_DURATION = 60;
    private static final double BASE_MAX_HEALTH = 800.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 220.0D;
    private static final int MAX_RIFT_SCARS = 64;
    private static final double ARENA_RADIUS = 22.0D;
    private static final float FIRST_PHASE_FLOOR = 0.66F;
    private static final float SECOND_PHASE_FLOOR = 0.33F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final List<RiftScar> riftScars = new ArrayList<>();
    private final List<Vec3> stormMarks = new ArrayList<>();
    private final Set<UUID> attackHits = new HashSet<>();
    private final Set<UUID> participants = new HashSet<>();
    private boolean handlingDeath = false;
    private final Set<BlockPos> phaseMechanismStabilizers = new HashSet<>();

    private AttackMode attackMode = AttackMode.NONE;
    private int attackAge;
    private int attackDuration;
    private Vec3 attackDirection = new Vec3(0.0D, 0.0D, 1.0D);
    private int anchorSlamCooldown = 100;
    private int voidPullCooldown = 160;
    private int shieldBreakCooldown = 260;
    private int riftChargeCooldown = 80;
    private int riftStormCooldown = 240;
    private boolean entranceFxPlayed;
    private int phaseGateStage;
    private int scaledParticipants = 1;
    private boolean phaseTransitionPending;
    private boolean phaseTransitionActive;
    /** 裂隙风暴阶段预告只播一次；客户端表现标记,不参与判定。 */
    private boolean stormPhaseAnnounced;
    private int protectedMessageCooldown;
    private BlockPos arenaCenter;

    public RiftWarden(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 160;
        this.bossEvent.setDarkenScreen(true);
    }

    /** 裂隙特效与外伸构件不参与实体命中，底面使用贴合主体的 profile。 */
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VISUAL_ATTACK_MODE, AttackMode.NONE.ordinal());
        builder.define(DATA_VISUAL_ATTACK_AGE, 0);
        builder.define(DATA_VISUAL_ATTACK_DURATION, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.ATTACK_DAMAGE, 26.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    /**
     * 绕圈半径必须超过虚空牵引的下限(距离平方 36),否则永久贴脸只会循环锚击。
     * 优先级数必须小于近战:MOVE 只能被更小的数抢走,排在后面等于永不触发。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CirclePreferredRangeGoal(this, 6.0D, 11.0D, 0.9D,
                () -> this.attackMode != AttackMode.NONE));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        if (this.handlingDeath || this.dead || !this.isAlive()) {
            return;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.tickDeepRechallengeBinding(serverLevel,
                        () -> !this.handlingDeath && !this.dead)) {
            return;
        }
        super.aiStep();
        if (this.handlingDeath || this.dead || !this.isAlive()) {
            return;
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.isAlive() && this.tickCount % 20 == 0
                && !this.registerDeepRechallengePresence(serverLevel)) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
        }

        if (!this.entranceFxPlayed) {
            this.entranceFxPlayed = true;
            this.playEntranceFx(serverLevel);
            // 裂隙封锁入场演出,不改变判定
            this.triggerAnim("state_controller", "awaken");
        }
        this.enforceArenaBoundary(serverLevel);

        this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        this.anchorSlamCooldown = decrement(this.anchorSlamCooldown);
        this.voidPullCooldown = decrement(this.voidPullCooldown);
        this.shieldBreakCooldown = decrement(this.shieldBreakCooldown);
        this.riftChargeCooldown = decrement(this.riftChargeCooldown);
        this.riftStormCooldown = decrement(this.riftStormCooldown);
        this.protectedMessageCooldown = decrement(this.protectedMessageCooldown);
        this.tickRiftScars(serverLevel);

        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : this.nearbyVictims(serverLevel, radius)) {
                this.registerParticipant(player);
            }
            if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                this.updateDynamicScaling();
            }
        }
        if (this.phaseGateStage < 2 && !this.phaseTransitionPending
                && !this.phaseTransitionActive
                && BossPhaseGate.reachedFloor(this, this.currentMechanicFloor())) {
            this.phaseTransitionPending = true;
            this.phaseMechanismStabilizers.clear();
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.15D, movement.y, movement.z * 0.15D);
            this.tickAttack(serverLevel);
            this.attackAge++;
            this.syncVisualAttackState();
            if (this.attackAge >= this.attackDuration) {
                boolean completedTransition = this.phaseTransitionActive;
                this.attackMode = AttackMode.NONE;
                this.attackAge = 0;
                this.attackDuration = 0;
                this.syncVisualAttackState();
                this.attackHits.clear();
                this.stormMarks.clear();
                if (completedTransition) {
                    this.completePhaseTransition(serverLevel);
                }
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (this.phaseTransitionPending) {
            this.drawStabilizerBreakGuidance(serverLevel,
                    this.position().add(0.0D, this.getBbHeight() * 0.52D, 0.0D));
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }

        // 裂隙风暴预告,与 rift_storm 同一条血量门槛,只播一次;不触碰 attackMode、冷却与选敌。
        if (!this.stormPhaseAnnounced
                && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.stormPhaseAnnounced = true;
            this.triggerAnim("state_controller", "storm_call");
        }

        double distanceSqr = this.distanceToSqr(target);
        if (this.riftStormCooldown <= 0 && distanceSqr <= 625.0D
                && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.beginAttack(AttackMode.RIFT_STORM, RIFT_STORM_DURATION,
                    "rift_storm", target);
            this.riftStormCooldown = 420;
        } else if (this.shieldBreakCooldown <= 0 && distanceSqr <= 196.0D
                && this.getHealth() <= this.getMaxHealth() * 0.6F) {
            this.beginAttack(AttackMode.SHIELD_BREAK, SHIELD_BREAK_DURATION,
                    "shield_break", target);
            this.shieldBreakCooldown = 340;
        } else if (this.riftChargeCooldown <= 0 && distanceSqr > 100.0D && distanceSqr <= 900.0D) {
            this.beginAttack(AttackMode.RIFT_CHARGE, RIFT_CHARGE_DURATION,
                    "rift_charge", target);
            this.riftChargeCooldown = 190;
        } else if (this.anchorSlamCooldown <= 0 && distanceSqr <= 64.0D) {
            this.beginAttack(AttackMode.ANCHOR_SLAM, ANCHOR_SLAM_DURATION,
                    "anchor_slam", target);
            this.anchorSlamCooldown = 180;
        } else if (this.voidPullCooldown <= 0 && distanceSqr > 36.0D && distanceSqr <= 400.0D) {
            this.beginAttack(AttackMode.VOID_PULL, VOID_PULL_DURATION,
                    "void_pull", target);
            this.voidPullCooldown = 220;
        }
    }

    private void beginAttack(AttackMode mode, int duration, String trigger, LivingEntity target) {
        this.attackMode = mode;
        this.attackAge = 0;
        this.attackDuration = duration;
        this.syncVisualAttackState();
        this.attackHits.clear();
        this.stormMarks.clear();
        this.attackDirection = horizontalDirectionTo(target);
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        float yaw = (float) (Math.atan2(this.attackDirection.z, this.attackDirection.x)
                * 180.0D / Math.PI) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.triggerAnim("attack_controller", trigger);
    }

    private void beginPhaseTransition(LivingEntity target) {
        this.phaseTransitionPending = false;
        this.phaseTransitionActive = true;
        if (this.phaseGateStage == 0) {
            this.beginAttack(AttackMode.SHIELD_BREAK, SHIELD_BREAK_DURATION,
                    "shield_break", target);
        } else {
            this.beginAttack(AttackMode.RIFT_STORM, RIFT_STORM_DURATION,
                    "rift_storm", target);
        }
        // 两段锚定破裂提示的第一段：护住核心
        this.triggerAnim("state_controller", "phase_lock");
        this.broadcastNearby(Component.translatable("message.unknown_echoes.rift_warden.phase_locked"));
    }

    private void completePhaseTransition(ServerLevel level) {
        this.phaseTransitionActive = false;
        this.phaseGateStage = Math.min(2, this.phaseGateStage + 1);
        BossLifecycleFx.phase(level, this.position(), this.getBbWidth(), this.getBbHeight(), BossParticleTheme.RIFT.body());
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.0F, 0.55F + this.phaseGateStage * 0.18F);
        // 两段锚定破裂提示的第二段：护盾碎裂成虚空尘、核心暴露
        this.triggerAnim("state_controller", "phase_open");
        this.broadcastNearby(Component.translatable("message.unknown_echoes.rift_warden.phase_open"));
    }

    private LivingEntity findPhaseTarget(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : this.nearbyVictims(level, radius)) {
            double distance = player.distanceToSqr(this);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void updateDynamicScaling() {
        int count = Math.max(1, this.participants.size());
        if (count <= this.scaledParticipants) {
            return;
        }
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double multiplier = ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get();
        int added = count - this.scaledParticipants;
        maxHealth.setBaseValue((BASE_MAX_HEALTH + HEALTH_PER_EXTRA_PLAYER * (count - 1)) * multiplier);
        this.heal((float) (HEALTH_PER_EXTRA_PLAYER * added * multiplier));
        this.scaledParticipants = count;
        this.broadcastNearby(Component.translatable(
                "message.unknown_echoes.rift_warden.scaled", count));
    }

    private float currentMechanicFloor() {
        return switch (this.phaseGateStage) {
            case 0 -> this.getMaxHealth() * FIRST_PHASE_FLOOR;
            case 1 -> this.getMaxHealth() * SECOND_PHASE_FLOOR;
            default -> 0.0F;
        };
    }

    @Override
    public boolean advanceTestPhase() {
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive() || this.isRemoved()
                || this.handlingDeath || this.phaseGateStage >= 2) return false;
        this.setHealth(Math.min(this.getHealth(), this.currentMechanicFloor()));
        this.phaseTransitionPending = false;
        this.phaseMechanismStabilizers.clear();
        this.attackMode = AttackMode.NONE;
        this.attackAge = 0;
        this.attackDuration = 0;
        this.attackHits.clear();
        this.stormMarks.clear();
        this.syncVisualAttackState();
        this.completePhaseTransition(level);
        return true;
    }

    @Override
    public float phaseDamageFloor() {
        return this.currentMechanicFloor();
    }

    @Override
    public void onPhaseDamageBlocked(DamageSource source) {
        if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.rift_warden.guard_holds"), true);
            this.protectedMessageCooldown = 40;
        }
    }

    /** 稳定器由玩家操作后调用；同一场的三座稳定器完成才开始阶段攻击。 */
    public void onStabilizerActivated(ServerLevel level, BlockPos stabilizerPos) {
        if (this.handlingDeath || this.dead || this.isRemoved() || !this.phaseTransitionPending
                || this.arenaCenter == null || !this.isAlive()
                || this.arenaCenter.distManhattan(stabilizerPos) > 48) {
            return;
        }
        List<BlockPos> expected = VoidCoreAltarBlock.findStabilizerPositions(level, this.arenaCenter);
        if (!expected.contains(stabilizerPos)) {
            return;
        }
        this.phaseMechanismStabilizers.add(stabilizerPos.immutable());
        if (this.phaseMechanismStabilizers.size() >= expected.size()
                && expected.size() >= 3) {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) {
                target = this.findPhaseTarget(level);
            }
            if (target != null) {
                this.beginPhaseTransition(target);
                this.phaseMechanismStabilizers.clear();
            }
        }
    }

    private void broadcastNearby(Component message) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : this.nearbyPlayers(level, this.position(), radius, false)) {
            player.displayClientMessage(message, true);
        }
    }

    public void bindArena(BlockPos center) {
        this.arenaCenter = center.immutable();
        this.restrictTo(this.arenaCenter, (int) ARENA_RADIUS);
    }

    @Override
    public BlockPos deepRechallengeAltar() {
        return this.arenaCenter;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel level) {
            if (!this.registerDeepRechallengePresence(level)) {
                this.discard();
            }
        }
    }

    private void enforceArenaBoundary(ServerLevel level) {
        if (this.arenaCenter == null) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && this.horizontalDistanceSqr(target.position()) > square(ARENA_RADIUS + 4.0D)) {
            this.setTarget(null);
        }
        if (this.tickCount % 10 != 0
                || this.horizontalDistanceSqr(this.position()) <= square(ARENA_RADIUS + 3.0D)) {
            return;
        }
        Vec3 returnPoint = this.findArenaReturnPoint(level);
        if (returnPoint == null) {
            return;
        }
        this.teleportTo(returnPoint.x, returnPoint.y, returnPoint.z);
        this.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(BossParticleTheme.RIFT.body(),
                returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                12, 0.9D, 1.4D, 0.9D, 0.05D);
    }

    private Vec3 findArenaReturnPoint(ServerLevel level) {
        BlockPos altarPos = this.arenaCenter;
        if (!this.isAlive() || this.level() != level || altarPos == null) {
            return null;
        }
        var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        if (chunk == null || !(chunk.getBlockState(altarPos).getBlock() instanceof VoidCoreAltarBlock)) {
            return null;
        }
        BlockPos returnPos = VoidCoreAltarBlock.findSpawnPos(level, altarPos);
        if (returnPos == null) {
            return null;
        }
        Vec3 target = Vec3.atBottomCenterOf(returnPos);
        if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, this, target)) {
            return null;
        }
        chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        return this.isAlive() && this.level() == level && altarPos.equals(this.arenaCenter)
                && chunk != null && chunk.getBlockState(altarPos).getBlock() instanceof VoidCoreAltarBlock
                ? target : null;
    }

    private double horizontalDistanceSqr(Vec3 position) {
        double dx = position.x - (this.arenaCenter.getX() + 0.5D);
        double dz = position.z - (this.arenaCenter.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double square(double value) {
        return value * value;
    }

    private void tickAttack(ServerLevel level) {
        switch (this.attackMode) {
            case RIFT_SLASH -> this.tickRiftSlash(level);
            case ANCHOR_SLAM -> this.tickAnchorSlam(level);
            case VOID_PULL -> this.tickVoidPull(level);
            case SHIELD_BREAK -> this.tickShieldBreak(level);
            case RIFT_CHARGE -> this.tickRiftCharge(level);
            case RIFT_STORM -> this.tickRiftStorm(level);
            case NONE -> {
            }
        }
    }

    private void tickRiftSlash(ServerLevel level) {
        Vec3 body = this.position().add(0.0D, this.getBbHeight() * 0.48D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_SLASH_WARN.get(),
                    SoundSource.HOSTILE, 1.4F, 1.0F);
        }
        if (this.attackAge <= 12) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, body, this.tickCount, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 4 == 0) {
                BossFx.sweepArc(level, this.position().add(0.0D, 0.05D, 0.0D),
                        this.attackDirection, 7.0D, BossParticleTheme.RIFT.spark());
            }
        }

        if (this.attackAge == 14) {
            this.releaseRiftSlash(level);
        } else if (this.attackAge == 18) {
            BossFx.sweepArc(level, this.position().add(0.0D, 1.0D, 0.0D),
                    this.attackDirection, 8.0D, BossParticleTheme.RIFT.spark());
        }
    }

    private void releaseRiftSlash(ServerLevel level) {
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.42D, 0.0D);
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_SLASH_RELEASE.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
        BossFx.sweepArc(level, origin, this.attackDirection, 8.0D, BossParticleTheme.RIFT.spark());
        BossFx.sparkSpray(level, origin, this.attackDirection, 8.5D, BossParticleTheme.RIFT.spark());
        Vec3 impact = origin.add(this.attackDirection.scale(4.5D));
        BossFx.impactBurst(level, impact, BossParticleTheme.RIFT.spark());
        level.sendParticles(BossParticleTheme.RIFT.spark(),
                impact.x, impact.y, impact.z, 7, 1.8D, 1.0D, 1.8D, 0.0D);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        this.damageConePlayers(level, 7.5D, 0.12D, damage, 1.0D, 0.22D);
        for (double distance = 1.8D; distance <= 8.4D; distance += 1.1D) {
            this.addRiftScar(this.position().add(this.attackDirection.scale(distance)),
                    66, (int) Math.round(distance * 0.7D));
        }
    }

    private void tickAnchorSlam(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.48D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_SLAM_WARN.get(),
                    SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (this.attackAge <= 19) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, core, this.tickCount, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 4 == 0) {
                double contraction = 6.8D - this.attackAge / 19.0D * 4.8D;
                BossFx.ring(level, center, 7.5D, 56, 0.12D, BossParticleTheme.RIFT.ember());
                BossFx.ring(level, center, contraction, 32, 0.2D, BossParticleTheme.RIFT.ember());
            }
        }

        if (this.attackAge == 21) {
            this.releaseAnchorSlam(level);
        } else if (this.attackAge == 27) {
            BossFx.ring(level, center, 9.5D, 64, 0.18D, BossParticleTheme.RIFT.ember());
        }
    }

    private void releaseAnchorSlam(ServerLevel level) {
        Vec3 center = this.position();
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_SLAM_RELEASE.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
        BossFx.groundShockwave(level, center.add(0.0D, 0.15D, 0.0D),
                7.5D, BossParticleTheme.RIFT.ember());
        BossFx.impactBurst(level, center.add(0.0D, 0.35D, 0.0D), BossParticleTheme.RIFT.spark());
        BossFx.cinematicImpact(level, center, 34.0D, 0.86F, 11, 0x6F55C8);
        level.sendParticles(BossParticleTheme.RIFT.body(),
                center.x, center.y + 0.5D, center.z, 20, 3.1D, 0.9D, 3.1D, 0.04D);
        this.damageAndKnockbackPlayers(level, 7.5D, 22.0F, 1.35D, 0.48D);
        this.addRadialRiftScars(center, 8, 3, 82);
    }

    private void tickVoidPull(ServerLevel level) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_PULL_WARN.get(),
                    SoundSource.HOSTILE, 2.1F, 1.0F);
        }
        if (this.attackAge <= 29) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, core, -this.tickCount, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 2 == 0) {
                double contraction = 18.0D - this.attackAge / 29.0D * 15.0D;
                BossFx.ring(level, core, contraction, 40, 0.0D, BossParticleTheme.RIFT.ember());
            }
            if (this.attackAge % 3 == 0) {
                this.pullPlayers(level, core, 20.0D, 0.13D, 0.05D, 0.0F, true);
            }
        }

        if (this.attackAge == 31) {
            this.releaseVoidPull(level, core);
        } else if (this.attackAge >= 34 && this.attackAge <= 40 && this.attackAge % 2 == 0) {
            level.sendParticles(BossParticleTheme.RIFT.body(),
                    core.x, core.y, core.z, 6, 1.2D, 1.2D, 1.2D, 0.06D);
        }
    }

    private void releaseVoidPull(ServerLevel level, Vec3 core) {
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_RIFT.get(),
                SoundSource.HOSTILE, 1.8F, 1.0F);
        this.pullPlayers(level, core, 20.0D, 1.15D, 0.28D, 11.0F, true);
        BossFx.impactBurst(level, core, BossParticleTheme.RIFT.spark());
        for (double radius : new double[]{4.0D, 8.0D, 12.0D, 18.0D}) {
            BossFx.ring(level, core, radius, Math.max(32, (int) (radius * 5.0D)),
                    0.0D, BossParticleTheme.RIFT.ember());
        }
        level.sendParticles(BossParticleTheme.RIFT.spark(),
                core.x, core.y, core.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(BossParticleTheme.RIFT.body(),
                core.x, core.y, core.z, 24, 2.6D, 2.6D, 2.6D, 0.04D);
    }

    private void tickShieldBreak(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.52D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_BREAK_WARN.get(),
                    SoundSource.HOSTILE, 2.3F, 1.0F);
        }
        if (this.attackAge <= 26) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, core, this.tickCount * 1.35D, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 4 == 0) {
                BossFx.ring(level, center, 4.0D, 28, 0.16D, BossParticleTheme.RIFT.ember());
                BossFx.ring(level, center, 8.0D, 44, 0.2D, BossParticleTheme.RIFT.ember());
                BossFx.ring(level, center, 11.0D, 60, 0.24D, BossParticleTheme.RIFT.ember());
            }
        }
        if (this.phaseTransitionActive && this.attackAge % 4 == 0) {
            this.drawStabilizerBreakGuidance(level, core);
        }

        if (this.attackAge == 28) {
            this.releaseShieldBreak(level);
        } else if (this.attackAge == 35) {
            BossFx.ring(level, center, 14.0D, 72, 0.18D, BossParticleTheme.RIFT.ember());
            level.sendParticles(BossParticleTheme.RIFT.body(),
                    core.x, core.y, core.z, 16, 2.0D, 2.2D, 2.0D, 0.05D);
        }
    }

    private void releaseShieldBreak(ServerLevel level) {
        Vec3 center = this.position().add(0.0D, 0.3D, 0.0D);
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_BREAK_RELEASE.get(),
                SoundSource.HOSTILE, 2.7F, 1.0F);
        BossFx.groundShockwave(level, center, 11.0D, BossParticleTheme.RIFT.ember());
        BossFx.impactBurst(level, center.add(0.0D, 2.0D, 0.0D), BossParticleTheme.RIFT.spark());
        BossFx.cinematicImpact(level, center, 44.0D, 1.0F, 14, 0x8D68D8);
        level.sendParticles(BossParticleTheme.RIFT.spark(),
                center.x, center.y + 2.2D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        List<ServerPlayer> victims = this.damageAndKnockbackPlayers(
                level, 11.0D, 28.0F, 1.75D, 0.68D);
        for (ServerPlayer player : victims) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this);
        }
        this.addRadialRiftScars(this.position(), 12, 2, 96);
    }

    private void tickRiftCharge(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.48D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_CHARGE_WARN.get(),
                    SoundSource.HOSTILE, 1.8F, 1.0F);
        }
        if (this.attackAge <= 10) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, core, this.tickCount * 1.4D, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 4 == 0) {
                this.drawChargeLane(level, center, this.attackDirection, 11.0D);
            }
        }
        if (this.attackAge >= 12 && this.attackAge <= 20) {
            this.setDeltaMovement(this.attackDirection.x * 1.05D,
                    Math.max(this.getDeltaMovement().y, 0.05D),
                    this.attackDirection.z * 1.05D);
            if (this.attackAge % 2 == 0) {
                Vec3 trail = this.position().subtract(this.attackDirection.scale(1.5D))
                        .add(0.0D, 0.35D, 0.0D);
                BossFx.ring(level, trail, 1.8D, 24, 0.0D, BossParticleTheme.RIFT.ember());
                this.addRiftScar(this.position(), 56, 0);
                this.damageChargeVictims(level, 4.5D, 24.0F);
            }
        }

        if (this.attackAge == 17) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_CHARGE_RELEASE.get(),
                    SoundSource.HOSTILE, 1.8F, 1.0F);
            BossFx.impactBurst(level, core.add(this.attackDirection.scale(1.5D)),
                    BossParticleTheme.RIFT.spark());
        } else if (this.attackAge == 21) {
            this.releaseChargeSlash(level);
        }
    }

    private void drawChargeLane(ServerLevel level, Vec3 origin, Vec3 direction, double length) {
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).normalize();
        for (double distance = 1.5D; distance <= length; distance += 1.2D) {
            Vec3 center = origin.add(direction.scale(distance)).add(0.0D, 0.12D, 0.0D);
            for (double offset : new double[]{-2.2D, 0.0D, 2.2D}) {
                Vec3 point = center.add(side.scale(offset));
                level.sendParticles(BossParticleTheme.RIFT.spark(),
                        point.x, point.y, point.z, 1, 0.04D, 0.03D, 0.04D, 0.0D);
            }
        }
    }

    private void damageChargeVictims(ServerLevel level, double radius, float damage) {
        for (ServerPlayer player : this.nearbyVictims(level, radius)) {
            if (!this.attackHits.add(player.getUUID())) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                player.setDeltaMovement(player.getDeltaMovement().add(
                        this.attackDirection.x * 1.2D, 0.35D, this.attackDirection.z * 1.2D));
                player.hurtMarked = true;
            }
            BossFx.impactBurst(level, player.position().add(0.0D, 0.8D, 0.0D),
                    BossParticleTheme.RIFT.spark());
        }
    }

    private void releaseChargeSlash(ServerLevel level) {
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.4D, 0.0D);
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_CHARGE_SLASH.get(),
                SoundSource.HOSTILE, 2.1F, 1.0F);
        BossFx.sweepArc(level, origin, this.attackDirection, 7.0D, BossParticleTheme.RIFT.spark());
        BossFx.sparkSpray(level, origin, this.attackDirection, 8.0D, BossParticleTheme.RIFT.spark());
        this.damageChargeVictims(level, 6.0D, 20.0F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.1D, 0.0D),
                5.5D, BossParticleTheme.RIFT.ember());
    }

    private void tickRiftStorm(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_STORM_WARN.get(),
                    SoundSource.HOSTILE, 2.3F, 1.0F);
        }
        if (this.attackAge == 8) {
            this.prepareStormMarks(level);
        }
        if (this.attackAge <= 37) {
            if (level.getGameTime() % 4 == 0) {
                BossFx.chargeSpiral(level, core, this.tickCount * 1.7D, BossParticleTheme.RIFT.body());
            }
            if (this.attackAge % 4 == 0) {
                double radius = 3.0D + this.attackAge * 0.22D;
                BossFx.ring(level, core, radius, 24, 0.0D, BossParticleTheme.RIFT.ember());
            }
            this.tickStormWarnings(level);
        }
        if (this.phaseTransitionActive && this.attackAge % 4 == 0) {
            this.drawStabilizerBreakGuidance(level, core);
        }

        if (this.attackAge == 40) {
            this.releaseRiftStorm(level, core);
        } else if (this.attackAge == 46) {
            BossFx.groundShockwave(level, center.add(0.0D, 0.12D, 0.0D),
                    15.0D, BossParticleTheme.RIFT.ember());
        }
    }

    private void drawStabilizerBreakGuidance(ServerLevel level, Vec3 bossCore) {
        if (this.arenaCenter == null) {
            return;
        }
        Vec3 altarCore = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 0.65D, 0.0D);
        for (BlockPos stabilizerPos : VoidCoreAltarBlock.findStabilizerPositions(level, this.arenaCenter)) {
            Vec3 stabilizer = Vec3.atCenterOf(stabilizerPos).add(0.0D, 0.55D, 0.0D);
            BossFx.guidanceMarker(level, stabilizer, BossParticleTheme.RIFT.ember());
            BossFx.guidanceFlow(level, stabilizer, altarCore,
                    BossParticleTheme.RIFT.ember(), BossParticleTheme.RIFT.ember(), this.tickCount);
        }
        BossFx.guidanceMarker(level, altarCore, BossParticleTheme.RIFT.ember());
        BossFx.guidanceFlow(level, altarCore, bossCore,
                BossParticleTheme.RIFT.ember(), BossParticleTheme.RIFT.ember(), this.tickCount);
    }

    private void prepareStormMarks(ServerLevel level) {
        this.stormMarks.clear();
        for (ServerPlayer player : this.nearbyVictims(level, 26.0D)) {
            if (this.stormMarks.size() < 4) {
                this.stormMarks.add(player.position());
            }
        }
        int radialMarks = 8;
        double offset = this.random.nextDouble() * Math.PI;
        for (int i = 0; i < radialMarks; i++) {
            double angle = Math.PI * 2.0D * i / radialMarks + offset;
            double radius = i % 2 == 0 ? 6.0D : 10.0D;
            this.stormMarks.add(this.position().add(
                    Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
        }
    }

    private void tickStormWarnings(ServerLevel level) {
        if (this.attackAge < 8 || this.stormMarks.isEmpty()) {
            return;
        }
        boolean pulse = this.attackAge == 12 || this.attackAge == 24 || this.attackAge == 36;
        for (int i = 0; i < this.stormMarks.size(); i++) {
            Vec3 mark = this.stormMarks.get(i);
            if (pulse) {
                BossFx.ring(level, mark, 2.7D, 12, 0.12D, BossParticleTheme.RIFT.ember());
            }
            if (this.attackAge % 4 == 0) {
                level.sendParticles(BossParticleTheme.RIFT.spark(),
                        mark.x, mark.y + 0.25D + (this.attackAge - 8) * 0.08D, mark.z,
                        1, 0.12D, 0.22D, 0.12D, 0.0D);
            }
        }
    }

    private void releaseRiftStorm(ServerLevel level, Vec3 core) {
        level.playSound(null, this.blockPosition(), ModSounds.RIFT_WARDEN_ATTACK_STORM_RELEASE.get(),
                SoundSource.HOSTILE, 2.4F, 1.0F);
        this.attackHits.clear();
        for (Vec3 mark : this.stormMarks) {
            BossFx.areaBurst(level, mark.add(0.0D, 0.2D, 0.0D), BossParticleTheme.RIFT.spark());
            level.sendParticles(BossParticleTheme.RIFT.spark(),
                    mark.x, mark.y + 0.4D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (ServerPlayer player : this.nearbyPlayers(level, mark, 3.0D, true)) {
                if (!this.attackHits.add(player.getUUID())) {
                    continue;
                }
                if (player.hurt(this.damageSources().mobAttack(this), 26.0F)) {
                    Vec3 away = player.position().subtract(mark).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-4D) {
                        Vec3 force = away.normalize().scale(1.15D);
                        player.setDeltaMovement(player.getDeltaMovement().add(force.x, 0.55D, force.z));
                        player.hurtMarked = true;
                    }
                }
            }
            this.addRiftScar(mark, 100, 0);
        }
        BossFx.impactBurst(level, core, BossParticleTheme.RIFT.spark());
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                12.0D, BossParticleTheme.RIFT.ember());
        BossFx.cinematicImpact(level, this.position(), 48.0D, 1.0F, 14, 0x7655C9);
    }

    private void damageConePlayers(ServerLevel level, double radius, double minimumDot,
                                   float damage, double horizontalForce, double verticalForce) {
        for (ServerPlayer player : this.nearbyVictims(level, radius)) {
            Vec3 toVictim = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (toVictim.lengthSqr() > 1.0E-4D
                    && this.attackDirection.dot(toVictim.normalize()) < minimumDot) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                player.setDeltaMovement(player.getDeltaMovement().add(
                        this.attackDirection.x * horizontalForce, verticalForce,
                        this.attackDirection.z * horizontalForce));
                player.hurtMarked = true;
            }
        }
    }

    private List<ServerPlayer> damageAndKnockbackPlayers(ServerLevel level, double radius, float damage,
                                                         double horizontalForce, double verticalForce) {
        List<ServerPlayer> hitPlayers = new ArrayList<>();
        for (ServerPlayer player : this.nearbyVictims(level, radius)) {
            if (!player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            hitPlayers.add(player);
            Vec3 away = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-4D) {
                Vec3 force = away.normalize().scale(horizontalForce);
                player.setDeltaMovement(player.getDeltaMovement().add(force.x, verticalForce, force.z));
                player.hurtMarked = true;
            }
        }
        return hitPlayers;
    }

    private void pullPlayers(ServerLevel level, Vec3 core, double radius,
                             double horizontalForce, double verticalForce,
                             float damage, boolean drawFlow) {
        for (ServerPlayer player : this.nearbyVictims(level, radius)) {
            Vec3 playerCenter = player.position().add(0.0D, 0.8D, 0.0D);
            Vec3 towardCore = core.subtract(playerCenter);
            Vec3 horizontal = towardCore.multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.lengthSqr() > 1.0E-4D) {
                Vec3 force = horizontal.normalize().scale(horizontalForce);
                player.setDeltaMovement(player.getDeltaMovement().add(force.x, verticalForce, force.z));
                player.hurtMarked = true;
            }
            if (damage > 0.0F) {
                player.hurt(this.damageSources().mobAttack(this), damage);
            }
            if (drawFlow) {
                BossFx.guidanceFlow(level, playerCenter, core,
                        BossParticleTheme.RIFT.ember(), BossParticleTheme.RIFT.ember(), this.tickCount);
            }
        }
    }

    private List<ServerPlayer> nearbyVictims(ServerLevel level, double radius) {
        return this.nearbyPlayers(level, this.position(), radius, true);
    }

    private List<ServerPlayer> nearbyPlayers(ServerLevel level, Vec3 center, double radius,
                                             boolean requireAlive) {
        AABB area = new AABB(center, center).inflate(radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(ServerPlayer.class, area,
                player -> !player.isSpectator() && (!requireAlive || player.isAlive())
                        && player.distanceToSqr(center.x, center.y, center.z) <= radiusSqr);
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        Vec3 direction = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        return direction.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isDeepRechallengeCombatBlocked(serverLevel)) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            this.registerParticipant(attacker);
            if (this.getTarget() == null) {
                this.setTarget(attacker);
            }
        }
        float floor = this.currentMechanicFloor();
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt && this.phaseGateStage < 2
                && BossPhaseGate.reachedFloor(this, floor)) {
            this.phaseTransitionPending = true;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level() instanceof ServerLevel serverLevel
                && this.isDeepRechallengeCombatBlocked(serverLevel)) {
            return false;
        }
        if (this.attackMode != AttackMode.NONE || !(target instanceof LivingEntity living)) {
            return false;
        }
        this.beginAttack(AttackMode.RIFT_SLASH, RIFT_SLASH_DURATION,
                "rift_slash", living);
        return false;
    }

    private void addRadialRiftScars(Vec3 center, int arms, int layers, int lifetime) {
        double offset = this.random.nextDouble() * Math.PI;
        for (int arm = 0; arm < arms; arm++) {
            double angle = Math.PI * 2.0D * arm / arms + offset;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            for (int layer = 1; layer <= layers; layer++) {
                double distance = layer * 2.5D;
                this.addRiftScar(center.add(direction.scale(distance)),
                        lifetime, layer * 3);
            }
        }
    }

    private void addRiftScar(Vec3 position, int lifetime, int delay) {
        while (this.riftScars.size() >= MAX_RIFT_SCARS) {
            this.riftScars.remove(0);
        }
        this.riftScars.add(new RiftScar(position, lifetime, delay));
    }

    private void tickRiftScars(ServerLevel level) {
        Iterator<RiftScar> iterator = this.riftScars.iterator();
        while (iterator.hasNext()) {
            RiftScar scar = iterator.next();
            if (scar.delay > 0) {
                scar.delay--;
                continue;
            }
            scar.age++;
            if (scar.age == 1) {
                level.sendParticles(BossParticleTheme.RIFT.body(),
                        scar.position.x, scar.position.y + 0.12D, scar.position.z,
                        3, 0.22D, 0.1D, 0.22D, 0.02D);
            }
            if (scar.age % 6 == 0) {
                level.sendParticles(BossParticleTheme.RIFT.spark(),
                        scar.position.x, scar.position.y + 0.12D, scar.position.z,
                        1, 0.2D, 0.04D, 0.2D, 0.01D);
            }
            if (scar.age >= scar.lifetime) {
                iterator.remove();
            }
        }
    }

    private void playEntranceFx(ServerLevel level) {
        Vec3 center = this.position();
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_EMERGE,
                SoundSource.HOSTILE, 2.2F, 0.62F);
        BossFx.groundShockwave(level, center.add(0.0D, 0.15D, 0.0D),
                6.5D, BossParticleTheme.RIFT.ember());
        for (int ring = 1; ring <= 3; ring++) {
            BossFx.ring(level, center, ring * 2.4D, 24 + ring * 8,
                    0.2D + ring * 0.08D, BossParticleTheme.RIFT.ember());
        }
        level.sendParticles(BossParticleTheme.RIFT.body(),
                center.x, center.y + this.getBbHeight() * 0.5D, center.z,
                20, 1.8D, 2.4D, 1.8D, 0.04D);
    }

    @Override
    public boolean isSettlingDeath() {
        return this.handlingDeath && this.dead;
    }

    @Override
    public void die(DamageSource source) {
        if (this.handlingDeath || this.dead) {
            return;
        }
        this.handlingDeath = true;
        try {
            if (!(this.level() instanceof ServerLevel serverLevel)) {
                super.die(source);
                return;
            }
            Vec3 deathPos = this.position();
            BlockPos deathAltar = this.deepRechallengeAltar();
            UUID deathUuid = this.getUUID();
            Set<UUID> deathParticipants = Set.copyOf(this.participants);
            try {
                super.die(source);
            } finally {
                if (this.dead) {
                    settleDeepBossDeath(serverLevel, deathPos, deathAltar, deathUuid, deathParticipants);
                }
            }
        } finally {
            this.handlingDeath = false;
        }
    }

    private void settleDeepBossDeath(ServerLevel serverLevel, Vec3 deathPos, BlockPos deathAltar,
                                     UUID deathUuid, Set<UUID> deathParticipants) {
        try {
            boolean hasEligibleParticipant = false;
            boolean firstKillDrop = false;
            for (ServerPlayer player : nearbyDeathPlayers(serverLevel, deathPos)) {
                if (deathParticipants.contains(player.getUUID())
                        && isDeathParticipant(serverLevel, deathPos, player)) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                            EchoPermission.RIFT_WARDEN_ID);
                    hasEligibleParticipant = true;
                    firstKillDrop |= firstKill;
                    runDeathStep("personal progress", () -> {
                        if (!EchoAbilityManager.markBossDefeated(player, EchoPermission.RIFT_WARDEN_ID)
                                && isDeathParticipant(serverLevel, deathPos, player)) {
                            player.sendSystemMessage(Component.translatable(
                                    "message.unknown_echoes.progress_record.full"));
                        }
                    });
                }
            }
            if (hasEligibleParticipant) {
                grantDeathMaterials(serverLevel, deathPos, firstKillDrop);
            }
        } finally {
            if (deathAltar != null) {
                runDeathStep("arena completion", () -> BossRechallengeAltarBlockEntity.startCooldown(
                        serverLevel, deathAltar, deathUuid));
            }
        }
        this.riftScars.clear();
        this.stormMarks.clear();
        this.attackHits.clear();
        runDeathStep("death presentation", () -> {
            if (isCurrentDeathPresentation(serverLevel, deathPos)) {
                this.triggerAnim("state_controller", "death");
                playDeathEffects(serverLevel, deathPos);
            }
        });
    }

    private void grantDeathMaterials(ServerLevel serverLevel, Vec3 deathPos, boolean firstKillDrop) {
        runDeathStep("rift shards", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z, new ItemStack(
                ModItems.VOID_RIFT_SHARD.get(), 3 + this.random.nextInt(3))));
        runDeathStep("anchor shards", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z, new ItemStack(
                ModItems.SPACE_ANCHOR_SHARD.get(), firstKillDrop ? 2 : 1)));
        runDeathStep("ore scraps", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z, new ItemStack(
                ModItems.DEEP_ORE_SCRAP.get(), 2 + this.random.nextInt(3))));
        if (firstKillDrop || this.random.nextFloat() < 0.25F) {
            runDeathStep("void rubbing", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z, new ItemStack(
                    ModItems.VOID_RESEARCH_RUBBING.get())));
        }
    }

    private List<ServerPlayer> nearbyDeathPlayers(ServerLevel level, Vec3 deathPos) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(deathPos, deathPos).inflate(radius),
                player -> isDeathParticipant(level, deathPos, player));
    }

    private boolean isDeathParticipant(ServerLevel level, Vec3 deathPos, ServerPlayer player) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.position().distanceToSqr(deathPos) <= radius * radius;
    }

    private void runDeathStep(String step, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            cn.kurt6.unknown_echoes.UnknownEchoes.LOGGER.error("RiftWarden death {} failed for {}", step, this.getUUID(), failure);
        }
    }

    private boolean isCurrentDeathPresentation(ServerLevel level, Vec3 deathPos) {
        return this.dead && !this.isRemoved() && this.level() == level
                && level.getEntity(this.getUUID()) == this
                && this.position().distanceToSqr(deathPos) < 0.0001D;
    }

    private void playDeathEffects(ServerLevel serverLevel, Vec3 deathPos) {
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) return;
        BossLifecycleFx.death(serverLevel, deathPos, this.getBbWidth(), this.getBbHeight(),
                BossParticleTheme.RIFT.body());
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
            return;
        }
        Vec3 core = deathPos.add(0.0D, this.getBbHeight() * 0.52D, 0.0D);
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
            return;
        }
        BossFx.impactBurst(serverLevel, core, BossParticleTheme.RIFT.spark());
        for (int ring = 1; ring <= 5; ring++) {
            if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
                return;
            }
            BossFx.ring(serverLevel, core, ring * 2.2D, 28 + ring * 8,
                    0.0D, ring % 2 == 0 ? BossParticleTheme.RIFT.ember() : BossParticleTheme.RIFT.ember());
        }
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
            return;
        }
        serverLevel.sendParticles(BossParticleTheme.RIFT.body(),
                core.x, core.y, core.z, 48, 3.4D, 3.2D, 3.4D, 0.12D);
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
            return;
        }
        serverLevel.sendParticles(BossParticleTheme.RIFT.body(),
                core.x, core.y, core.z, 16, 2.2D, 2.8D, 2.2D, 0.06D);
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    public void registerParticipant(ServerPlayer player) {
        if (!player.isSpectator() && (this.participants.contains(player.getUUID())
                || this.participants.size() < BossNbtData.MAX_PARTICIPANTS)) {
            this.participants.add(player.getUUID());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {

    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RIFT_WARDEN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RIFT_WARDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RIFT_WARDEN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhaseGateStage", this.phaseGateStage);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        tag.putBoolean("PhaseTransitionPending",
                this.phaseTransitionPending || this.phaseTransitionActive);
        if (this.arenaCenter != null) {
            tag.putIntArray("ArenaCenter", new int[]{
                    this.arenaCenter.getX(), this.arenaCenter.getY(), this.arenaCenter.getZ()});
        }
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phaseGateStage = Math.max(0, Math.min(2, tag.getInt("PhaseGateStage")));
        this.phaseTransitionPending = tag.getBoolean("PhaseTransitionPending");
        this.phaseTransitionActive = false;
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        this.attackMode = AttackMode.NONE;
        this.attackAge = 0;
        this.attackDuration = 0;
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        BlockPos loadedArena = BossNbtData.loadPositionNear(tag, "ArenaCenter", this.blockPosition(), 64, 32);
        if (loadedArena != null) {
            this.bindArena(loadedArena);
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("rift_slash", RIFT_SLASH)
                .triggerableAnim("anchor_slam", ANCHOR_SLAM)
                .triggerableAnim("void_pull", VOID_PULL)
                .triggerableAnim("shield_break", SHIELD_BREAK)
                .triggerableAnim("rift_charge", RIFT_CHARGE)
                .triggerableAnim("rift_storm", RIFT_STORM));
        // 状态演出单独一个 controller：入场、锚定破裂提示、风暴预告与死亡都可能与攻击并发。
        controllers.add(new AnimationController<>(this, "state_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("awaken", AWAKEN)
                .triggerableAnim("phase_lock", PHASE_LOCK)
                .triggerableAnim("phase_open", PHASE_OPEN)
                .triggerableAnim("storm_call", STORM_CALL)
                .triggerableAnim("death", DEATH));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void syncVisualAttackState() {
        this.entityData.set(DATA_VISUAL_ATTACK_MODE, this.attackMode.ordinal());
        this.entityData.set(DATA_VISUAL_ATTACK_AGE, this.attackAge);
        this.entityData.set(DATA_VISUAL_ATTACK_DURATION, this.attackDuration);
    }

    @Override
    public int visualAttackMode() {
        return this.entityData.get(DATA_VISUAL_ATTACK_MODE);
    }

    @Override
    public int visualAttackAge() {
        return this.entityData.get(DATA_VISUAL_ATTACK_AGE);
    }

    @Override
    public int visualAttackDuration() {
        return this.entityData.get(DATA_VISUAL_ATTACK_DURATION);
    }

    private enum AttackMode {
        NONE,
        RIFT_SLASH,
        ANCHOR_SLAM,
        VOID_PULL,
        SHIELD_BREAK,
        RIFT_CHARGE,
        RIFT_STORM
    }

    private static final class RiftScar {
        private final Vec3 position;
        private final int lifetime;
        private int delay;
        private int age;

        private RiftScar(Vec3 position, int lifetime, int delay) {
            this.position = position;
            this.lifetime = lifetime;
            this.delay = delay;
        }
    }
    @Override
    public int musicStage() {
        return Math.max(0, Math.min(2, this.phaseGateStage));
    }
}
