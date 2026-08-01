package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.VoidCoreAltarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * 裂隙看守者主线 Boss。
 *
 * 六种攻击按 Blockbench 动画关键帧分段执行。66% 与 33% 血线会强制执行解锚阶段，
 * 高伤害不能跨越阶段；死亡时只给仍在场地内的有效参与者写个人击败记录。
 */
public class RiftWarden extends Monster implements GeoEntity, PhaseGatedBoss {
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

    private static final int RIFT_SLASH_DURATION = 32;
    private static final int ANCHOR_SLAM_DURATION = 38;
    private static final int VOID_PULL_DURATION = 44;
    private static final int SHIELD_BREAK_DURATION = 48;
    private static final int RIFT_CHARGE_DURATION = 36;
    private static final int RIFT_STORM_DURATION = 60;
    private static final int MAX_RIFT_SCARS = 64;
    private static final double ARENA_RADIUS = 21.0D;
    private static final float FIRST_PHASE_FLOOR = 0.66F;
    private static final float SECOND_PHASE_FLOOR = 0.33F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final List<RiftScar> riftScars = new ArrayList<>();
    private final List<Vec3> stormMarks = new ArrayList<>();
    private final Set<UUID> attackHits = new HashSet<>();
    private final Set<UUID> participants = new HashSet<>();

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
    private boolean phaseTransitionPending;
    private boolean phaseTransitionActive;
    private int protectedMessageCooldown;
    private BlockPos arenaCenter;

    public RiftWarden(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 160;
        this.bossEvent.setDarkenScreen(true);
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

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
        }

        if (!this.entranceFxPlayed) {
            this.entranceFxPlayed = true;
            this.playEntranceFx(serverLevel);
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
            for (ServerPlayer player : serverLevel.players()) {
                if (!player.isSpectator() && player.isAlive()
                        && player.distanceToSqr(this) <= radius * radius) {
                    this.registerParticipant(player);
                }
            }
        }
        if (this.phaseGateStage < 2 && !this.phaseTransitionPending
                && !this.phaseTransitionActive
                && BossPhaseGate.reachedFloor(this, this.currentMechanicFloor())) {
            this.phaseTransitionPending = true;
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.15D, movement.y, movement.z * 0.15D);
            this.tickAttack(serverLevel);
            this.attackAge++;
            if (this.attackAge >= this.attackDuration) {
                boolean completedTransition = this.phaseTransitionActive;
                this.attackMode = AttackMode.NONE;
                this.attackAge = 0;
                this.attackDuration = 0;
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
            if (target == null || !target.isAlive()) {
                target = this.findPhaseTarget(serverLevel);
            }
            if (target != null) {
                this.beginPhaseTransition(target);
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
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
        this.broadcastNearby(Component.translatable("message.unknown_echoes.rift_warden.phase_locked"));
    }

    private void completePhaseTransition(ServerLevel level) {
        this.phaseTransitionActive = false;
        this.phaseGateStage = Math.min(2, this.phaseGateStage + 1);
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.0F, 0.55F + this.phaseGateStage * 0.18F);
        this.broadcastNearby(Component.translatable("message.unknown_echoes.rift_warden.phase_open"));
    }

    private LivingEntity findPhaseTarget(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(this);
            if (!player.isSpectator() && player.isAlive() && distance <= radius * radius
                    && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private float currentMechanicFloor() {
        return switch (this.phaseGateStage) {
            case 0 -> this.getMaxHealth() * FIRST_PHASE_FLOOR;
            case 1 -> this.getMaxHealth() * SECOND_PHASE_FLOOR;
            default -> 0.0F;
        };
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

    private void broadcastNearby(Component message) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(message, true);
            }
        }
    }

    public void bindArena(BlockPos center) {
        this.arenaCenter = center.immutable();
        this.restrictTo(this.arenaCenter, (int) ARENA_RADIUS);
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
        Vec3 returnPoint = Vec3.atBottomCenterOf(this.arenaCenter).add(0.0D, 0.0D, 8.0D);
        this.teleportTo(returnPoint.x, returnPoint.y, returnPoint.z);
        this.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                34, 0.9D, 1.4D, 0.9D, 0.12D);
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
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.4F, 0.55F);
        }
        if (this.attackAge <= 12) {
            BossFx.chargeSpiral(level, body, this.tickCount, ParticleTypes.REVERSE_PORTAL);
            if (this.attackAge % 3 == 0) {
                BossFx.sweepArc(level, this.position().add(0.0D, 0.05D, 0.0D),
                        this.attackDirection, 7.0D, ParticleTypes.ELECTRIC_SPARK);
            }
        }
        // 动画 0.7 秒处是挥刃峰值。
        if (this.attackAge == 14) {
            this.releaseRiftSlash(level);
        } else if (this.attackAge == 18) {
            BossFx.sweepArc(level, this.position().add(0.0D, 1.0D, 0.0D),
                    this.attackDirection, 8.0D, ParticleTypes.PORTAL);
        }
    }

    private void releaseRiftSlash(ServerLevel level) {
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.42D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 2.0F, 0.62F);
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.6F, 0.45F);
        BossFx.sweepArc(level, origin, this.attackDirection, 8.0D, ParticleTypes.END_ROD);
        BossFx.sparkSpray(level, origin, this.attackDirection, 8.5D, ParticleTypes.REVERSE_PORTAL);
        Vec3 impact = origin.add(this.attackDirection.scale(4.5D));
        BossFx.impactBurst(level, impact, ParticleTypes.REVERSE_PORTAL);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
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
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 2.0F, 0.48F);
        }
        if (this.attackAge <= 19) {
            BossFx.chargeSpiral(level, core, this.tickCount, ParticleTypes.SCULK_SOUL);
            if (this.attackAge % 4 == 0) {
                double contraction = 6.8D - this.attackAge / 19.0D * 4.8D;
                BossFx.ring(level, center, 7.5D, 56, 0.12D, ParticleTypes.REVERSE_PORTAL);
                BossFx.ring(level, center, contraction, 32, 0.2D, ParticleTypes.ELECTRIC_SPARK);
            }
        }
        // 动画 1.05 秒处是锚刃落地帧。
        if (this.attackAge == 21) {
            this.releaseAnchorSlam(level);
        } else if (this.attackAge == 27) {
            BossFx.ring(level, center, 9.5D, 64, 0.18D, ParticleTypes.PORTAL);
        }
    }

    private void releaseAnchorSlam(ServerLevel level) {
        Vec3 center = this.position();
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.52F);
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK,
                SoundSource.HOSTILE, 2.0F, 0.58F);
        BossFx.groundShockwave(level, center.add(0.0D, 0.15D, 0.0D),
                7.5D, ParticleTypes.REVERSE_PORTAL);
        BossFx.impactBurst(level, center.add(0.0D, 0.35D, 0.0D), ParticleTypes.SCULK_SOUL);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                center.x, center.y + 0.5D, center.z, 64, 3.1D, 0.9D, 3.1D, 0.1D);
        this.damageAndKnockbackPlayers(level, 7.5D, 22.0F, 1.35D, 0.48D);
        this.addRadialRiftScars(center, 8, 3, 82);
    }

    private void tickVoidPull(ServerLevel level) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE, 2.1F, 0.58F);
        }
        if (this.attackAge <= 29) {
            BossFx.chargeSpiral(level, core, -this.tickCount, ParticleTypes.DRAGON_BREATH);
            if (this.attackAge % 2 == 0) {
                double contraction = 18.0D - this.attackAge / 29.0D * 15.0D;
                BossFx.ring(level, core, contraction, 40, 0.0D, ParticleTypes.PORTAL);
            }
            if (this.attackAge % 3 == 0) {
                this.pullPlayers(level, core, 20.0D, 0.13D, 0.05D, 0.0F, true);
            }
        }
        // 动画 1.55 秒处核心合拢，牵引在这里达到峰值。
        if (this.attackAge == 31) {
            this.releaseVoidPull(level, core);
        } else if (this.attackAge >= 34 && this.attackAge <= 40 && this.attackAge % 2 == 0) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 20, 1.2D, 1.2D, 1.2D, 0.22D);
        }
    }

    private void releaseVoidPull(ServerLevel level, Vec3 core) {
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.3F, 0.72F);
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 1.8F, 1.35F);
        this.pullPlayers(level, core, 20.0D, 1.15D, 0.28D, 11.0F, true);
        BossFx.impactBurst(level, core, ParticleTypes.DRAGON_BREATH);
        for (double radius : new double[]{4.0D, 8.0D, 12.0D, 18.0D}) {
            BossFx.ring(level, core, radius, Math.max(32, (int) (radius * 5.0D)),
                    0.0D, ParticleTypes.REVERSE_PORTAL);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                core.x, core.y, core.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                core.x, core.y, core.z, 80, 2.6D, 2.6D, 2.6D, 0.08D);
    }

    private void tickShieldBreak(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.52D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 2.3F, 0.38F);
            level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.HOSTILE, 1.8F, 0.62F);
        }
        if (this.attackAge <= 26) {
            BossFx.chargeSpiral(level, core, this.tickCount * 1.35D, ParticleTypes.ELECTRIC_SPARK);
            if (this.attackAge % 4 == 0) {
                BossFx.ring(level, center, 4.0D, 28, 0.16D, ParticleTypes.END_ROD);
                BossFx.ring(level, center, 8.0D, 44, 0.2D, ParticleTypes.REVERSE_PORTAL);
                BossFx.ring(level, center, 11.0D, 60, 0.24D, ParticleTypes.ELECTRIC_SPARK);
            }
        }
        if (this.phaseTransitionActive && this.attackAge % 4 == 0) {
            this.drawStabilizerBreakGuidance(level, core);
        }
        // 精修后的双臂外爆峰值位于 1.4 秒。
        if (this.attackAge == 28) {
            this.releaseShieldBreak(level);
        } else if (this.attackAge == 35) {
            BossFx.ring(level, center, 14.0D, 72, 0.18D, ParticleTypes.PORTAL);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 48, 2.0D, 2.2D, 2.0D, 0.16D);
        }
    }

    private void releaseShieldBreak(ServerLevel level) {
        Vec3 center = this.position().add(0.0D, 0.3D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 2.7F, 0.55F);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.2F, 0.48F);
        BossFx.groundShockwave(level, center, 11.0D, ParticleTypes.ELECTRIC_SPARK);
        BossFx.impactBurst(level, center.add(0.0D, 2.0D, 0.0D), ParticleTypes.END_ROD);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                center.x, center.y + 2.2D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        this.damageAndKnockbackPlayers(level, 11.0D, 28.0F, 1.75D, 0.68D);
        for (ServerPlayer player : level.players()) {
            if (this.isValidVictim(player, 11.0D)) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this);
            }
        }
        this.addRadialRiftScars(this.position(), 12, 2, 96);
    }

    private void tickRiftCharge(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.48D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.8F, 0.42F);
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 1.5F, 1.25F);
        }
        if (this.attackAge <= 10) {
            BossFx.chargeSpiral(level, core, this.tickCount * 1.4D, ParticleTypes.ELECTRIC_SPARK);
            if (this.attackAge % 2 == 0) {
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
                BossFx.ring(level, trail, 1.8D, 24, 0.0D, ParticleTypes.REVERSE_PORTAL);
                this.addRiftScar(this.position(), 56, 0);
                this.damageChargeVictims(level, 4.5D, 24.0F);
            }
        }
        // 动画 0.85 秒处达到最大前倾，随后在 1.05 秒挥出终结横斩。
        if (this.attackAge == 17) {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.HOSTILE, 1.8F, 1.35F);
            BossFx.impactBurst(level, core.add(this.attackDirection.scale(1.5D)),
                    ParticleTypes.ELECTRIC_SPARK);
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
                level.sendParticles(offset == 0.0D ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.PORTAL,
                        point.x, point.y, point.z, 1, 0.04D, 0.03D, 0.04D, 0.0D);
            }
        }
    }

    private void damageChargeVictims(ServerLevel level, double radius, float damage) {
        for (ServerPlayer player : level.players()) {
            if (!this.isValidVictim(player, radius) || !this.attackHits.add(player.getUUID())) {
                continue;
            }
            player.hurt(this.damageSources().mobAttack(this), damage);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    this.attackDirection.x * 1.2D, 0.35D, this.attackDirection.z * 1.2D));
            player.hurtMarked = true;
            BossFx.impactBurst(level, player.position().add(0.0D, 0.8D, 0.0D),
                    ParticleTypes.REVERSE_PORTAL);
        }
    }

    private void releaseChargeSlash(ServerLevel level) {
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.4D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 2.1F, 0.82F);
        BossFx.sweepArc(level, origin, this.attackDirection, 7.0D, ParticleTypes.END_ROD);
        BossFx.sparkSpray(level, origin, this.attackDirection, 8.0D, ParticleTypes.ELECTRIC_SPARK);
        this.damageChargeVictims(level, 6.0D, 20.0F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.1D, 0.0D),
                5.5D, ParticleTypes.REVERSE_PORTAL);
    }

    private void tickRiftStorm(ServerLevel level) {
        Vec3 center = this.position();
        Vec3 core = center.add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 2.1F, 1.45F);
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE, 2.3F, 0.42F);
        }
        if (this.attackAge == 8) {
            this.prepareStormMarks(level);
        }
        if (this.attackAge <= 37) {
            if (this.attackAge % 2 == 0) {
                BossFx.chargeSpiral(level, core, this.tickCount * 1.7D, ParticleTypes.REVERSE_PORTAL);
            }
            if (this.attackAge % 4 == 0) {
                double radius = 3.0D + this.attackAge * 0.22D;
                BossFx.ring(level, core, radius, 24, 0.0D, ParticleTypes.ELECTRIC_SPARK);
            }
            this.tickStormWarnings(level);
        }
        if (this.phaseTransitionActive && this.attackAge % 4 == 0) {
            this.drawStabilizerBreakGuidance(level, core);
        }
        // 动画 2.0 秒处双臂下压，全部裂隙同步爆发。
        if (this.attackAge == 40) {
            this.releaseRiftStorm(level, core);
        } else if (this.attackAge == 46) {
            BossFx.groundShockwave(level, center.add(0.0D, 0.12D, 0.0D),
                    15.0D, ParticleTypes.PORTAL);
        }
    }

    private void drawStabilizerBreakGuidance(ServerLevel level, Vec3 bossCore) {
        if (this.arenaCenter == null) {
            return;
        }
        Vec3 altarCore = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 0.65D, 0.0D);
        for (BlockPos stabilizerPos : VoidCoreAltarBlock.findStabilizerPositions(level, this.arenaCenter)) {
            Vec3 stabilizer = Vec3.atCenterOf(stabilizerPos).add(0.0D, 0.55D, 0.0D);
            BossFx.guidanceMarker(level, stabilizer, ParticleTypes.SCULK_SOUL);
            BossFx.guidanceFlow(level, stabilizer, altarCore,
                    ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, this.tickCount);
        }
        BossFx.guidanceMarker(level, altarCore, ParticleTypes.END_ROD);
        BossFx.guidanceFlow(level, altarCore, bossCore,
                ParticleTypes.REVERSE_PORTAL, ParticleTypes.END_ROD, this.tickCount);
    }

    private void prepareStormMarks(ServerLevel level) {
        this.stormMarks.clear();
        for (ServerPlayer player : level.players()) {
            if (this.isValidVictim(player, 26.0D) && this.stormMarks.size() < 4) {
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
                BossFx.ring(level, mark, 2.7D, 12, 0.12D, ParticleTypes.REVERSE_PORTAL);
            }
            if (this.attackAge % 2 == 0) {
                level.sendParticles(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                        mark.x, mark.y + 0.25D + (this.attackAge - 8) * 0.08D, mark.z,
                        1, 0.12D, 0.22D, 0.12D, 0.0D);
            }
        }
    }

    private void releaseRiftStorm(ServerLevel level, Vec3 core) {
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.4F, 0.72F);
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.82F);
        this.attackHits.clear();
        for (Vec3 mark : this.stormMarks) {
            BossFx.areaBurst(level, mark.add(0.0D, 0.2D, 0.0D), ParticleTypes.REVERSE_PORTAL);
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    mark.x, mark.y + 0.4D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || !player.isAlive()
                        || player.distanceToSqr(mark) > 9.0D
                        || !this.attackHits.add(player.getUUID())) {
                    continue;
                }
                player.hurt(this.damageSources().mobAttack(this), 26.0F);
                Vec3 away = player.position().subtract(mark).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-4D) {
                    Vec3 force = away.normalize().scale(1.15D);
                    player.setDeltaMovement(player.getDeltaMovement().add(force.x, 0.55D, force.z));
                    player.hurtMarked = true;
                }
            }
            this.addRiftScar(mark, 100, 0);
        }
        BossFx.impactBurst(level, core, ParticleTypes.ELECTRIC_SPARK);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                12.0D, ParticleTypes.REVERSE_PORTAL);
    }

    private void damageConePlayers(ServerLevel level, double radius, double minimumDot,
                                   float damage, double horizontalForce, double verticalForce) {
        for (ServerPlayer player : level.players()) {
            if (!this.isValidVictim(player, radius)) {
                continue;
            }
            Vec3 toVictim = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (toVictim.lengthSqr() > 1.0E-4D
                    && this.attackDirection.dot(toVictim.normalize()) < minimumDot) {
                continue;
            }
            player.hurt(this.damageSources().mobAttack(this), damage);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    this.attackDirection.x * horizontalForce, verticalForce,
                    this.attackDirection.z * horizontalForce));
            player.hurtMarked = true;
        }
    }

    private void damageAndKnockbackPlayers(ServerLevel level, double radius, float damage,
                                           double horizontalForce, double verticalForce) {
        for (ServerPlayer player : level.players()) {
            if (!this.isValidVictim(player, radius)) {
                continue;
            }
            player.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-4D) {
                Vec3 force = away.normalize().scale(horizontalForce);
                player.setDeltaMovement(player.getDeltaMovement().add(force.x, verticalForce, force.z));
                player.hurtMarked = true;
            }
        }
    }

    private void pullPlayers(ServerLevel level, Vec3 core, double radius,
                             double horizontalForce, double verticalForce,
                             float damage, boolean drawFlow) {
        for (ServerPlayer player : level.players()) {
            if (!this.isValidVictim(player, radius)) {
                continue;
            }
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
                        ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, this.tickCount);
            }
        }
    }

    private boolean isValidVictim(ServerPlayer player, double radius) {
        return !player.isSpectator() && player.isAlive() && player.distanceToSqr(this) <= radius * radius;
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
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        scar.position.x, scar.position.y + 0.12D, scar.position.z,
                        8, 0.22D, 0.1D, 0.22D, 0.08D);
            }
            if (scar.age % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                        scar.position.x, scar.position.y + 0.12D, scar.position.z,
                        2, 0.2D, 0.04D, 0.2D, 0.03D);
            }
            if (scar.age % 7 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        scar.position.x, scar.position.y + 0.2D, scar.position.z,
                        1, 0.12D, 0.08D, 0.12D, 0.01D);
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
                6.5D, ParticleTypes.REVERSE_PORTAL);
        for (int ring = 1; ring <= 3; ring++) {
            BossFx.ring(level, center, ring * 2.4D, 24 + ring * 8,
                    0.2D + ring * 0.08D, ParticleTypes.ELECTRIC_SPARK);
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                center.x, center.y + this.getBbHeight() * 0.5D, center.z,
                56, 1.8D, 2.4D, 1.8D, 0.08D);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.riftScars.clear();
        this.stormMarks.clear();
        this.attackHits.clear();
        double rewardRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= rewardRadius * rewardRadius) {
                if (!EchoAbilityManager.markBossDefeated(player, EchoPermission.RIFT_WARDEN_ID)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"));
                    continue;
                }
                player.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.rift_warden.defeated"));
            }
        }
        if (this.arenaCenter != null) {
            BossRechallengeAltarBlockEntity.startCooldown(level, this.arenaCenter);
        }
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.52D, 0.0D);
        BossFx.impactBurst(level, core, ParticleTypes.REVERSE_PORTAL);
        for (int ring = 1; ring <= 5; ring++) {
            BossFx.ring(level, core, ring * 2.2D, 28 + ring * 8,
                    0.0D, ring % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK);
        }
        level.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                core.x, core.y, core.z, 220, 3.4D, 3.2D, 3.4D, 0.42D);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                core.x, core.y, core.z, 80, 2.2D, 2.8D, 2.2D, 0.18D);
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 2.5F, 0.72F);
        level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 2.2F, 0.45F);
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
        // 主线 Boss 不自然消失。
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhaseGateStage", this.phaseGateStage);
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
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
}
