package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.GravityCoreAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepCalibrationNodeBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VoidProphet extends Monster implements GeoEntity, PhaseGatedBoss, BossVisualState,
        DeepRechallengeBoundBoss {
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_MODE =
            SynchedEntityData.defineId(VoidProphet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_AGE =
            SynchedEntityData.defineId(VoidProphet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_DURATION =
            SynchedEntityData.defineId(VoidProphet.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.void_prophet.idle");
    private static final RawAnimation SPELL_SWAY_ANIM =
            RawAnimation.begin().then("animation.void_prophet.spell_sway", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SHORT_HOP_ANIM =
            RawAnimation.begin().then("animation.void_prophet.short_hop", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SPATIAL_REND_ANIM =
            RawAnimation.begin().then("animation.void_prophet.spatial_rend", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GRAVITY_RECOIL_ANIM =
            RawAnimation.begin().then("animation.void_prophet.gravity_recoil", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CORE_BREAK_ANIM =
            RawAnimation.begin().then("animation.void_prophet.core_break", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VOID_LANCE_ANIM =
            RawAnimation.begin().then("animation.void_prophet.void_lance", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CONSTELLATION_ANIM =
            RawAnimation.begin().then("animation.void_prophet.constellation", Animation.LoopType.PLAY_ONCE);

    // 状态演出走独立的 state_controller,避免与攻击动画互相打断。
    private static final RawAnimation AWAKEN_ANIM =
            RawAnimation.begin().then("animation.void_prophet.awaken", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation OMEN_ANIM =
            RawAnimation.begin().then("animation.void_prophet.omen", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CALIBRATE_ANIM =
            RawAnimation.begin().then("animation.void_prophet.calibrate", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation COLLAPSE_ANIM =
            RawAnimation.begin().then("animation.void_prophet.collapse", Animation.LoopType.PLAY_ONCE);

    private static final double BASE_MAX_HEALTH = 680.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 220.0D;
    private static final double REND_LENGTH = 22.0D;
    private static final double REND_HALF_WIDTH = 1.35D;
    private static final double GRAVITY_RADIUS = 10.0D;
    private static final double LANCE_BLAST_RADIUS = 2.35D;
    private static final double ARENA_RADIUS = 20.0D;
    private static final float ENRAGE_THRESHOLD = 0.45F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> participants = new HashSet<>();
    private final List<LineRift> rendLines = new ArrayList<>();
    private final List<Vec3> constellationMarks = new ArrayList<>();
    private final List<Vec3> lanceMarks = new ArrayList<>();
    private final List<VoidScar> voidScars = new ArrayList<>();
    private final Set<BlockPos> phaseMechanismNodes = new HashSet<>();

    private AttackMode attackMode = AttackMode.NONE;
    private int attackTicks = 0;
    private int blinkCooldown = 45;
    private int rendCooldown = 75;
    private int gravityCooldown = 120;
    private int constellationCooldown = 35;
    private int lanceCooldown = 90;
    private int outOfCombatTicks = 0;
    private int scaledParticipants = 1;
    private int protectedMessageCooldown;
    private boolean enraged = false;
    private boolean phaseTransitionTriggered = false;
    private boolean phaseTransitionPending = false;
    /** 空间错位显形只播一次。客户端表现用,不参与判定、不写 NBT。 */
    private boolean awakenPlayed = false;
    private BlockPos arenaCenter;

    public VoidProphet(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.bossEvent.setDarkenScreen(true);
    }

    /** 法袍/法杖等外伸部分不计入主体碰撞箱，底面按 profile 保留 X/Z 长方形。 */
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
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 28.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.tickDeepRechallengeBinding(serverLevel)) {
            return;
        }
        super.aiStep();
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

        this.tickBossSystems(serverLevel);
        this.tickVoidScars(serverLevel);
        this.spawnAmbientVoid(serverLevel);

        // 空间错位显形：只播一次动画,不停导航、不改冷却、不加无敌。
        if (!this.awakenPlayed && this.getTarget() != null) {
            this.awakenPlayed = true;
            this.triggerAnim("state_controller", "awaken");
        }

        if (!this.phaseTransitionTriggered && !this.phaseTransitionPending
                && this.getHealth() <= this.getMaxHealth() * ENRAGE_THRESHOLD) {
            this.phaseTransitionPending = true;
            this.phaseMechanismNodes.clear();
            // 虚空预兆宣告：与 phase_locked 提示同帧。
            this.triggerAnim("state_controller", "omen");
            this.broadcastNearby(serverLevel, Component.translatable(
                    "message.unknown_echoes.void_prophet.phase_locked"));
        }

        if (this.phaseTransitionPending && this.attackMode == AttackMode.NONE) {
            this.getNavigation().stop();
            this.drawCalibrationBreakGuidance(serverLevel,
                    this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D));
            return;
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.45D, 1.0D, 0.45D));
            this.syncVisualAttackState();
            this.tickActiveAttack(serverLevel);
            return;
        }

        this.tickCooldowns();
        this.tryStartAttack(serverLevel);
        this.syncVisualAttackState();
    }

    private void tickBossSystems(ServerLevel serverLevel) {
        this.enforceArenaBoundary(serverLevel);
        if (this.protectedMessageCooldown > 0) {
            this.protectedMessageCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.outOfCombatTicks = 0;
            if (this.tickCount % 20 == 0) {
                double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
                for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(radius),
                        candidate -> !candidate.isSpectator()
                                && candidate.distanceToSqr(this) <= radius * radius)) {
                    this.registerParticipant(player);
                }
                if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                    this.updateDynamicScaling();
                }
            }
        } else if (++this.outOfCombatTicks > 120 && this.outOfCombatTicks % 20 == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.015F);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
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
        level.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                28, 0.8D, 1.2D, 0.8D, 0.1D);
    }

    private Vec3 findArenaReturnPoint(ServerLevel level) {
        if (this.arenaCenter == null || !level.hasChunkAt(this.arenaCenter)
                || !(level.getBlockState(this.arenaCenter).getBlock() instanceof GravityCoreAltarBlock)) {
            return null;
        }
        BlockPos returnPos = GravityCoreAltarBlock.findSpawnPos(level, this.arenaCenter);
        if (returnPos == null) {
            return null;
        }
        Vec3 target = Vec3.atBottomCenterOf(returnPos);
        AABB movedBounds = this.getBoundingBox().move(target.subtract(this.position()));
        return level.getWorldBorder().isWithinBounds(movedBounds)
                && level.noCollision(this, movedBounds) ? target : null;
    }

    private double horizontalDistanceSqr(Vec3 position) {
        double dx = position.x - (this.arenaCenter.getX() + 0.5D);
        double dz = position.z - (this.arenaCenter.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double square(double value) {
        return value * value;
    }

    private void updateDynamicScaling() {
        int count = Math.max(1, this.participants.size());
        if (count <= this.scaledParticipants) {
            return;
        }
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double multiplier = ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get();
            maxHealth.setBaseValue((BASE_MAX_HEALTH + HEALTH_PER_EXTRA_PLAYER * (count - 1)) * multiplier);
            this.heal((float) (HEALTH_PER_EXTRA_PLAYER * (count - this.scaledParticipants) * multiplier));
        }
        this.scaledParticipants = count;
    }

    private void tickCooldowns() {
        if (this.blinkCooldown > 0) this.blinkCooldown--;
        if (this.rendCooldown > 0) this.rendCooldown--;
        if (this.gravityCooldown > 0) this.gravityCooldown--;
        if (this.constellationCooldown > 0) this.constellationCooldown--;
        if (this.lanceCooldown > 0) this.lanceCooldown--;
    }

    private int phaseCooldown(int normal, int omen) {
        return this.enraged ? omen : normal;
    }

    private void tryStartAttack(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSq = this.distanceToSqr(target);
        if (this.gravityCooldown <= 0 && distanceSq <= GRAVITY_RADIUS * GRAVITY_RADIUS) {
            this.startGravityRecoil(serverLevel);
        } else if (this.lanceCooldown <= 0 && distanceSq <= 32.0D * 32.0D) {
            this.startVoidLance(serverLevel, target);
        } else if (this.rendCooldown <= 0 && distanceSq <= REND_LENGTH * REND_LENGTH) {
            this.startSpatialRend(serverLevel, target);
        } else if (this.constellationCooldown <= 0) {
            this.startConstellation(serverLevel, target);
        } else if (this.blinkCooldown <= 0) {
            this.startShortHop(serverLevel, target);
        }
    }

    private void tickActiveAttack(ServerLevel serverLevel) {
        switch (this.attackMode) {
            case SHORT_HOP -> this.tickShortHop(serverLevel);
            case SPATIAL_REND -> this.tickSpatialRend(serverLevel);
            case GRAVITY_RECOIL -> this.tickGravityRecoil(serverLevel);
            case CONSTELLATION -> this.tickConstellation(serverLevel);
            case VOID_LANCE -> this.tickVoidLance(serverLevel);
            case CORE_BREAK -> this.tickCoreBreak(serverLevel);
            case NONE -> {
            }
        }
        if (--this.attackTicks <= 0) {
            this.attackMode = AttackMode.NONE;
        }
    }

    private void startShortHop(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.SHORT_HOP;
        this.attackTicks = 18;
        this.blinkCooldown = phaseCooldown(105, 72);
        this.triggerAnim("attack_controller", "short_hop");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PORTAL_AMBIENT,
                SoundSource.HOSTILE, 1.8F, 0.55F);
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
    }

    private void tickShortHop(ServerLevel serverLevel) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        spiral(serverLevel, core, 1.4D, this.attackTicks * 0.6D, BossParticleTheme.VOID_PROPHET.body());
        if ((this.attackTicks & 1) == 0) {
            BossFx.converge(serverLevel, core, 1.4D + this.attackTicks * 0.04D,
                    10, BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks != 10) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 oldPos = this.position();
        Vec3 destination = this.findSafeHop(serverLevel, target);
        this.voidScars.add(new VoidScar(oldPos.add(0.0D, 0.1D, 0.0D), 26));
        this.teleportTo(destination.x, destination.y, destination.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true;

        serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                oldPos.x, oldPos.y + this.getBbHeight() * 0.5D, oldPos.z,
                70, 1.0D, 2.2D, 1.0D, 0.12D);
        serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                destination.x, destination.y + this.getBbHeight() * 0.5D, destination.z,
                90, 1.1D, 2.4D, 1.1D, 0.18D);
        drawLine(serverLevel, oldPos.add(0.0D, 1.0D, 0.0D), destination.add(0.0D, 1.0D, 0.0D),
                0.7D, BossParticleTheme.VOID_PROPHET.spark());
        BossFx.helix(serverLevel, oldPos.add(0.0D, 1.2D, 0.0D),
                destination.add(0.0D, 1.2D, 0.0D), 0.32D, BossParticleTheme.VOID_PROPHET.body());
        BossFx.cinematicImpact(serverLevel, destination, 20.0D, 0.24F, 4, 0x5E3A94);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 2.2F, 0.65F);
    }

    private Vec3 findSafeHop(ServerLevel level, LivingEntity target) {
        double baseAngle = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX());
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = baseAngle + Math.PI + (this.random.nextDouble() - 0.5D) * Math.PI * 1.4D;
            double distance = 4.5D + this.random.nextDouble() * 5.0D;
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            for (int dy = 2; dy >= -2; dy--) {
                double y = target.getY() + dy;
                BlockPos floor = BlockPos.containing(x, y - 0.1D, z);
                if (!level.hasChunkAt(floor)
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
        return this.position();
    }

    private void tickVoidScars(ServerLevel serverLevel) {
        if (this.voidScars.isEmpty()) {
            return;
        }
        for (int i = this.voidScars.size() - 1; i >= 0; i--) {
            VoidScar scar = this.voidScars.get(i);
            double radius = 0.7D + (26 - scar.ticks) * 0.045D;
            if (scar.ticks % 2 == 0) {
                ring(serverLevel, scar.position, radius, 12, BossParticleTheme.VOID_PROPHET.body());
                serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                        scar.position.x, scar.position.y + 0.5D, scar.position.z,
                        3, radius * 0.4D, 0.5D, radius * 0.4D, 0.01D);
            }
            if (scar.ticks % 4 == 0) {
                BossFx.lockSigil(serverLevel, scar.position, 3.2D,
                        1.0D - scar.ticks / 26.0D, this.tickCount, BossParticleTheme.VOID_PROPHET.ember());
            }
            if (--scar.ticks <= 0) {
                this.damagePlayersNear(serverLevel, scar.position, 3.2D,
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F,
                        0.65D, 0.35D, false);
                serverLevel.sendParticles(ParticleTypes.FLASH,
                        scar.position.x, scar.position.y + 0.6D, scar.position.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                        scar.position.x, scar.position.y + 0.6D, scar.position.z,
                        45, 0.9D, 0.7D, 0.9D, 0.25D);
                serverLevel.playSound(null, BlockPos.containing(scar.position), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                        SoundSource.HOSTILE, 1.4F, 1.45F);
                this.voidScars.remove(i);
            }
        }
    }

    private void startSpatialRend(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.SPATIAL_REND;
        this.attackTicks = 32;
        this.rendCooldown = phaseCooldown(125, 86);
        this.rendLines.clear();

        Vec3 center = target.position().add(0.0D, 0.1D, 0.0D);
        Vec3 facing = center.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (facing.lengthSqr() < 1.0E-4D) {
            facing = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            facing = facing.normalize();
        }
        int lineCount = this.enraged ? 3 : 1;
        for (int i = 0; i < lineCount; i++) {
            double angle = lineCount == 1 ? 0.0D : (i - 1) * Math.PI / 3.0D;
            Vec3 direction = rotateHorizontal(facing, angle);
            this.rendLines.add(new LineRift(center.subtract(direction.scale(REND_LENGTH * 0.5D)),
                    direction, REND_LENGTH));
        }
        this.triggerAnim("attack_controller", "spatial_rend");
        serverLevel.playSound(null, this.blockPosition(), ModSounds.VOID_PROPHET_SONIC.get(),
                SoundSource.HOSTILE, 2.1F, 1.0F);
    }

    private void tickSpatialRend(ServerLevel serverLevel) {
        if ((this.attackTicks & 1) == 0) {
            for (LineRift line : this.rendLines) {
                for (double d = 0.0D; d <= line.length; d += 1.25D) {
                    Vec3 point = line.origin.add(line.direction.scale(d));
                    serverLevel.sendParticles(this.attackTicks % 3 == 0
                                    ? BossParticleTheme.VOID_PROPHET.spark() : BossParticleTheme.VOID_PROPHET.spark(),
                            point.x, point.y, point.z, 1, 0.08D, 0.04D, 0.08D, 0.0D);
                }
                if (this.attackTicks % 4 == 0) {
                    BossFx.guidanceFlow(serverLevel, line.origin.add(0.0D, 0.16D, 0.0D),
                            line.origin.add(line.direction.scale(line.length)).add(0.0D, 0.16D, 0.0D),
                            BossParticleTheme.VOID_PROPHET.ember(), BossParticleTheme.VOID_PROPHET.ember(), this.tickCount);
                }
            }
        }
        if (this.attackTicks % 5 == 0) {
            Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
            spiral(serverLevel, core, 1.8D, this.attackTicks * 0.45D, BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks == 5) {
            this.releaseSpatialRend(serverLevel);
        }
    }

    private void releaseSpatialRend(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85F;
        for (LineRift line : this.rendLines) {
            Vec3 end = line.origin.add(line.direction.scale(line.length));
            AABB bounds = new AABB(line.origin, end).inflate(REND_HALF_WIDTH, 3.5D, REND_HALF_WIDTH);
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, bounds,
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                Vec3 relative = player.position().subtract(line.origin).multiply(1.0D, 0.0D, 1.0D);
                double along = Math.max(0.0D, Math.min(line.length, relative.dot(line.direction)));
                Vec3 nearest = line.origin.add(line.direction.scale(along));
                Vec3 away = player.position().subtract(nearest).multiply(1.0D, 0.0D, 1.0D);
                if (away.horizontalDistanceSqr() > REND_HALF_WIDTH * REND_HALF_WIDTH) {
                    continue;
                }
                if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                    Vec3 knock = away.lengthSqr() > 1.0E-4D
                            ? away.normalize() : new Vec3(-line.direction.z, 0.0D, line.direction.x);
                    player.setDeltaMovement(player.getDeltaMovement().add(
                            knock.x * 0.85D, 0.28D, knock.z * 0.85D));
                    player.hurtMarked = true;
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
                }
                hit.add(player.getUUID());
            }
            for (double d = 0.0D; d <= line.length; d += 0.55D) {
                Vec3 point = line.origin.add(line.direction.scale(d));
                serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                        point.x, point.y + 0.25D, point.z, 2, 0.18D, 0.25D, 0.18D, 0.08D);
                if (((int) (d * 10.0D)) % 22 == 0) {
                    serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                            point.x, point.y + 0.15D, point.z, 1, 0.05D, 0.1D, 0.05D, 0.0D);
                }
            }
            BossFx.helix(serverLevel, line.origin.add(0.0D, 0.35D, 0.0D),
                    end.add(0.0D, 0.35D, 0.0D), 0.42D, BossParticleTheme.VOID_PROPHET.body());
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.4F, 1.65F);
        BossFx.cinematicImpact(serverLevel, this.position(), 34.0D, 0.66F, 8, 0x7651C9);
    }

    private void startGravityRecoil(ServerLevel serverLevel) {
        this.attackMode = AttackMode.GRAVITY_RECOIL;
        this.attackTicks = 36;
        this.gravityCooldown = phaseCooldown(185, 132);
        this.triggerAnim("attack_controller", "gravity_recoil");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.HOSTILE, 2.0F, 0.45F);
    }

    private void tickGravityRecoil(ServerLevel serverLevel) {
        Vec3 center = this.position().add(0.0D, 0.35D, 0.0D);
        double warningRadius = 2.0D + GRAVITY_RADIUS * this.attackTicks / 36.0D;
        if ((this.attackTicks & 1) == 0) {
            ring(serverLevel, center, warningRadius, 28, BossParticleTheme.VOID_PROPHET.body());
            ring(serverLevel, center, Math.max(1.0D, warningRadius * 0.58D), 18,
                    BossParticleTheme.VOID_PROPHET.body());
            spiral(serverLevel, center.add(0.0D, 2.0D, 0.0D), 2.2D,
                    (36 - this.attackTicks) * 0.65D, BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks > 3 && this.attackTicks % 3 == 0) {
            BossFx.converge(serverLevel, center.add(0.0D, 1.0D, 0.0D),
                    Math.max(2.0D, warningRadius), 14, BossParticleTheme.VOID_PROPHET.body());
        }

        if (this.attackTicks > 18 && this.attackTicks % 3 == 0) {
            this.pullPlayers(serverLevel, center, GRAVITY_RADIUS, 0.18D);
        }
        if (this.attackTicks == 18) {
            this.damagePlayersNear(serverLevel, center, 7.0D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F,
                    0.9D, 0.22D, true);
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    center.x, center.y + 1.0D, center.z, 55, 3.5D, 1.2D, 3.5D, 0.18D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.HOSTILE, 1.8F, 1.65F);
            BossFx.cinematicImpact(serverLevel, center, 28.0D, 0.42F, 5, 0x4F367A);
        }
        if (this.attackTicks == 3) {
            this.damagePlayersNear(serverLevel, center, 8.5D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F,
                    1.65D, 0.55D, false);
            for (int band = 1; band <= 3; band++) {
                ring(serverLevel, center, band * 3.0D, 32, BossParticleTheme.VOID_PROPHET.body());
            }
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                    center.x, center.y + 1.0D, center.z, 4, 1.5D, 1.0D, 1.5D, 0.0D);
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    center.x, center.y + 1.0D, center.z, 120, 2.0D, 2.2D, 2.0D, 0.35D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 2.5F, 0.55F);
            BossFx.cinematicImpact(serverLevel, center, 34.0D, 0.9F, 11, 0x7C4DC3);
        }
    }

    private void startConstellation(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.CONSTELLATION;
        this.attackTicks = 36;
        this.constellationCooldown = phaseCooldown(145, 102);
        this.constellationMarks.clear();
        this.constellationMarks.add(target.position().add(0.0D, 0.1D, 0.0D));
        int count = this.enraged ? 7 : 4;
        for (int i = 1; i < count; i++) {
            double angle = Math.PI * 2.0D * i / (count - 1) + this.random.nextDouble() * 0.5D;
            double distance = 2.8D + this.random.nextDouble() * 4.2D;
            this.constellationMarks.add(target.position().add(
                    Math.cos(angle) * distance, 0.1D, Math.sin(angle) * distance));
        }
        this.triggerAnim("attack_controller", "constellation");
        serverLevel.playSound(null, this.blockPosition(), ModSounds.VOID_PROPHET_SUMMON.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    private void tickConstellation(ServerLevel serverLevel) {
        double radius = 0.9D + (36 - this.attackTicks) * 0.035D;
        if ((this.attackTicks & 1) == 0) {
            for (int index = 0; index < this.constellationMarks.size(); index++) {
                Vec3 mark = this.constellationMarks.get(index);
                ring(serverLevel, mark, radius, 12, index % 2 == 0
                        ? BossParticleTheme.VOID_PROPHET.body() : BossParticleTheme.VOID_PROPHET.body());
                if (this.attackTicks % 4 == 0) {
                    serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                            mark.x, mark.y + 0.25D, mark.z,
                            3, radius * 0.5D, 0.05D, radius * 0.5D, 0.0D);
                    serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                            mark.x, mark.y + 2.8D, mark.z, 1, 0.05D, 0.2D, 0.05D, 0.01D);
                }
            }
        }
        if (this.attackTicks % 4 == 0) {
            for (int index = 0; index < this.constellationMarks.size(); index++) {
                Vec3 from = this.constellationMarks.get(index);
                Vec3 to = this.constellationMarks.get((index + 1) % this.constellationMarks.size());
                BossFx.guidanceFlow(serverLevel, from.add(0.0D, 0.2D, 0.0D),
                        to.add(0.0D, 0.2D, 0.0D), BossParticleTheme.VOID_PROPHET.ember(),
                        BossParticleTheme.VOID_PROPHET.ember(), this.tickCount + index * 3L);
                BossFx.lockSigil(serverLevel, from, 2.35D,
                        1.0D - this.attackTicks / 36.0D, this.tickCount + index, BossParticleTheme.VOID_PROPHET.ember());
            }
        }
        if (this.attackTicks == 4) {
            this.detonateConstellation(serverLevel);
        }
    }

    private void detonateConstellation(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F;
        for (Vec3 mark : this.constellationMarks) {
            for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                    new AABB(mark.x - 2.35D, mark.y - 1.0D, mark.z - 2.35D,
                            mark.x + 2.35D, mark.y + 4.0D, mark.z + 2.35D),
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                }
                hit.add(player.getUUID());
            }
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    mark.x, mark.y + 0.7D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    mark.x, mark.y + 0.8D, mark.z, 38, 0.75D, 1.3D, 0.75D, 0.16D);
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    mark.x, mark.y + 0.7D, mark.z, 24, 0.45D, 1.0D, 0.45D, 0.2D);
        }
        for (int i = 0; i < this.constellationMarks.size(); i++) {
            drawLine(serverLevel, this.constellationMarks.get(i),
                    this.constellationMarks.get((i + 1) % this.constellationMarks.size()),
                    0.45D, BossParticleTheme.VOID_PROPHET.spark());
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 2.3F, 0.5F);
        BossFx.cinematicImpact(serverLevel, this.constellationMarks.getFirst(),
                34.0D, 0.62F, 7, 0x8253B5);
    }

    private void startVoidLance(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.VOID_LANCE;
        this.attackTicks = 34;
        this.lanceCooldown = phaseCooldown(165, 116);
        this.lanceMarks.clear();

        Vec3 targetCenter = target.position();
        this.lanceMarks.add(this.findLanceGround(serverLevel, targetCenter));
        int count = this.enraged ? 6 : 4;
        double phaseOffset = this.random.nextDouble() * Math.PI * 2.0D;
        for (int i = 1; i < count; i++) {
            double angle = phaseOffset + Math.PI * 2.0D * (i - 1) / (count - 1)
                    + (this.random.nextDouble() - 0.5D) * 0.35D;
            double distance = 3.6D + this.random.nextDouble() * 4.8D;
            Vec3 candidate = targetCenter.add(
                    Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            this.lanceMarks.add(this.findLanceGround(serverLevel, candidate));
        }

        this.triggerAnim("attack_controller", "void_lance");
        this.getLookControl().setLookAt(target, 45.0F, 45.0F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.2F, 1.65F);
    }

    private Vec3 findLanceGround(ServerLevel level, Vec3 candidate) {
        BlockPos start = BlockPos.containing(candidate.x, candidate.y + 2.0D, candidate.z);
        for (int offset = 0; offset <= 9; offset++) {
            BlockPos floor = start.below(offset);
            if (level.hasChunkAt(floor)
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return new Vec3(candidate.x, floor.getY() + 1.05D, candidate.z);
            }
        }
        return new Vec3(candidate.x, candidate.y + 0.1D, candidate.z);
    }

    private void tickVoidLance(ServerLevel serverLevel) {
        double closingRadius = 0.75D + this.attackTicks * 0.055D;
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.68D, 0.0D);
        if ((this.attackTicks & 1) == 0) {
            for (int index = 0; index < this.lanceMarks.size(); index++) {
                Vec3 mark = this.lanceMarks.get(index);
                ring(serverLevel, mark, LANCE_BLAST_RADIUS, 18, BossParticleTheme.VOID_PROPHET.body());
                ring(serverLevel, mark.add(0.0D, 0.04D, 0.0D), closingRadius, 14,
                        index % 2 == 0 ? BossParticleTheme.VOID_PROPHET.body() : BossParticleTheme.VOID_PROPHET.body());

                double lanceHeight = 4.0D + this.attackTicks * 0.18D;
                serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                        mark.x, mark.y + lanceHeight, mark.z,
                        2, 0.12D, 0.5D, 0.12D, 0.015D);
                if (this.attackTicks % 4 == 0) {
                    drawLine(serverLevel, mark.add(0.0D, 0.15D, 0.0D),
                            mark.add(0.0D, lanceHeight + 2.0D, 0.0D),
                            1.15D, BossParticleTheme.VOID_PROPHET.spark());
                    drawLine(serverLevel, core, mark.add(0.0D, lanceHeight, 0.0D),
                            1.4D, BossParticleTheme.VOID_PROPHET.body());
                }
            }
        }
        if (this.attackTicks % 4 == 0) {
            double progress = 1.0D - this.attackTicks / 34.0D;
            for (int index = 0; index < this.lanceMarks.size(); index++) {
                Vec3 mark = this.lanceMarks.get(index);
                BossFx.lockSigil(serverLevel, mark, LANCE_BLAST_RADIUS,
                        progress, this.tickCount + index, BossParticleTheme.VOID_PROPHET.ember());
                BossFx.comet(serverLevel, mark, 11.0D, progress, BossParticleTheme.VOID_PROPHET.body());
            }
        }
        if (this.attackTicks % 3 == 0) {
            spiral(serverLevel, core, 2.4D, (34 - this.attackTicks) * 0.7D,
                    BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks == 7) {
            this.detonateVoidLances(serverLevel);
        }
    }

    private void detonateVoidLances(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (Vec3 mark : this.lanceMarks) {
            AABB area = new AABB(
                    mark.x - LANCE_BLAST_RADIUS, mark.y - 1.0D, mark.z - LANCE_BLAST_RADIUS,
                    mark.x + LANCE_BLAST_RADIUS, mark.y + 10.0D, mark.z + LANCE_BLAST_RADIUS);
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, area,
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                Vec3 horizontal = player.position().subtract(mark).multiply(1.0D, 0.0D, 1.0D);
                if (horizontal.horizontalDistanceSqr() > LANCE_BLAST_RADIUS * LANCE_BLAST_RADIUS) {
                    continue;
                }
                if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.58D, 0.0D));
                    player.hurtMarked = true;
                }
                hit.add(player.getUUID());
            }
            this.spawnVoidLanceBurst(serverLevel, mark);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.8F, 1.75F);
        BossFx.cinematicImpact(serverLevel, this.position(), 38.0D, 0.72F, 9, 0x8A55D1);
    }

    private void spawnVoidLanceBurst(ServerLevel level, Vec3 mark) {
        level.sendParticles(ParticleTypes.FLASH,
                mark.x, mark.y + 1.0D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                mark.x, mark.y + 4.2D, mark.z, 42, 0.5D, 3.8D, 0.5D, 0.14D);
        level.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                mark.x, mark.y + 2.0D, mark.z, 62, 0.8D, 2.4D, 0.8D, 0.18D);
        level.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                mark.x, mark.y + 5.0D, mark.z, 24, 0.4D, 4.0D, 0.4D, 0.12D);
        level.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                mark.x, mark.y + 0.25D, mark.z, 46, 1.4D, 0.2D, 1.4D, 0.28D);
        drawLine(level, mark.add(0.0D, 0.1D, 0.0D), mark.add(0.0D, 13.0D, 0.0D),
                0.38D, BossParticleTheme.VOID_PROPHET.body());
        ring(level, mark.add(0.0D, 0.08D, 0.0D), 1.2D, 20, BossParticleTheme.VOID_PROPHET.body());
        ring(level, mark.add(0.0D, 0.1D, 0.0D), LANCE_BLAST_RADIUS, 30,
                BossParticleTheme.VOID_PROPHET.body());
        ring(level, mark.add(0.0D, 0.12D, 0.0D), LANCE_BLAST_RADIUS * 1.55D, 34,
                BossParticleTheme.VOID_PROPHET.body());
    }

    private void startCoreBreak(ServerLevel serverLevel) {
        this.phaseTransitionTriggered = true;
        this.enraged = true;
        this.attackMode = AttackMode.CORE_BREAK;
        this.attackTicks = 44;
        this.rendLines.clear();
        this.constellationMarks.clear();
        this.lanceMarks.clear();
        this.triggerAnim("attack_controller", "core_break");
        this.broadcastNearby(serverLevel, Component.translatable("message.unknown_echoes.void_prophet.omen"));
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.4F, 1.7F);
    }

    /** 校准节点由玩家操作后调用；三座节点完成才开始破防演出。 */
    public void onCalibrationNodeActivated(ServerLevel level, BlockPos nodePos) {
        if (!this.phaseTransitionPending || this.arenaCenter == null
                || !this.isAlive() || this.arenaCenter.distManhattan(nodePos) > 48) {
            return;
        }
        boolean knownNode = false;
        for (BlockPos candidate : GravityCoreAltarBlock.findCalibrationNodes(level, this.arenaCenter)) {
            if (candidate.equals(nodePos)) {
                knownNode = true;
                break;
            }
        }
        if (!knownNode) {
            return;
        }
        boolean newNode = this.phaseMechanismNodes.add(nodePos.immutable());
        if (newNode) {
            // 三声校准反应,每座新节点播一次;重复交互不刷动画。
            this.triggerAnim("state_controller", "calibrate");
        }
        if (this.phaseMechanismNodes.size() >= DeepCalibrationNodeBlock.Calibration.values().length) {
            this.phaseTransitionPending = false;
            this.phaseMechanismNodes.clear();
            this.startCoreBreak(level);
        }
    }

    private void tickCoreBreak(ServerLevel serverLevel) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        double collapse = Math.max(0.8D, this.attackTicks * 0.15D);
        if ((this.attackTicks & 1) == 0) {
            ring(serverLevel, core, collapse, 30, BossParticleTheme.VOID_PROPHET.body());
            ring(serverLevel, core, collapse * 0.65D, 22, BossParticleTheme.VOID_PROPHET.body());
            spiral(serverLevel, core, 2.6D, (44 - this.attackTicks) * 0.8D, BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks > 6 && this.attackTicks % 3 == 0) {
            BossFx.converge(serverLevel, core, collapse, 16, BossParticleTheme.VOID_PROPHET.body());
        }
        if (this.attackTicks % 3 == 0 && this.attackTicks > 8) {
            this.pullPlayers(serverLevel, core, 12.0D, 0.12D);
        }
        if (this.attackTicks > 6 && this.attackTicks % 4 == 0) {
            this.drawCalibrationBreakGuidance(serverLevel, core);
        }
        if (this.attackTicks == 6) {
            this.damagePlayersNear(serverLevel, core, 10.0D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F,
                    1.25D, 0.5D, false);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    core.x, core.y, core.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    core.x, core.y, core.z, 180, 2.8D, 3.0D, 2.8D, 0.45D);
            for (int band = 1; band <= 4; band++) {
                ring(serverLevel, this.position().add(0.0D, 0.2D, 0.0D),
                        band * 2.7D, 36, BossParticleTheme.VOID_PROPHET.spark());
            }
            BossFx.pillar(serverLevel, this.position().add(0.0D, 0.1D, 0.0D),
                    9.0D, BossParticleTheme.VOID_PROPHET.body());
            BossFx.groundShockwave(serverLevel, this.position().add(0.0D, 0.1D, 0.0D),
                    10.0D, BossParticleTheme.VOID_PROPHET.ember());
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.HOSTILE, 2.7F, 0.45F);
            BossFx.cinematicImpact(serverLevel, core, 42.0D, 1.0F, 14, 0x6C37A6);
        }
        if (this.attackTicks == 2) {
            this.blinkCooldown = 18;
            this.rendCooldown = 28;
            this.gravityCooldown = 65;
            this.constellationCooldown = 12;
            this.lanceCooldown = 38;
            this.broadcastNearby(serverLevel, Component.translatable(
                    "message.unknown_echoes.void_prophet.phase_open"));
        }
    }

    private void drawCalibrationBreakGuidance(ServerLevel level, Vec3 bossCore) {
        if (this.arenaCenter == null) {
            return;
        }
        Vec3 altarCore = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 0.65D, 0.0D);
        for (BlockPos nodePos : GravityCoreAltarBlock.findCalibrationNodes(level, this.arenaCenter)) {
            Vec3 node = Vec3.atCenterOf(nodePos).add(0.0D, 0.45D, 0.0D);
            BossFx.guidanceMarker(level, node, BossParticleTheme.VOID_PROPHET.ember());
            BossFx.guidanceFlow(level, node, altarCore,
                    BossParticleTheme.VOID_PROPHET.ember(), BossParticleTheme.VOID_PROPHET.ember(), this.tickCount);
        }
        BossFx.guidanceMarker(level, altarCore, BossParticleTheme.VOID_PROPHET.ember());
        BossFx.guidanceFlow(level, altarCore, bossCore,
                BossParticleTheme.VOID_PROPHET.ember(), BossParticleTheme.VOID_PROPHET.ember(), this.tickCount);
    }

    private void spawnAmbientVoid(ServerLevel serverLevel) {
        if (this.tickCount % (this.enraged ? 3 : 5) != 0) {
            return;
        }
        double angle = this.tickCount * 0.22D;
        double radius = 1.5D + Math.sin(this.tickCount * 0.1D) * 0.3D;
        serverLevel.sendParticles(this.enraged ? BossParticleTheme.VOID_PROPHET.spark() : BossParticleTheme.VOID_PROPHET.spark(),
                this.getX() + Math.cos(angle) * radius,
                this.getY() + 1.2D + (this.tickCount % 30) * this.getBbHeight() / 30.0D,
                this.getZ() + Math.sin(angle) * radius,
                2, 0.08D, 0.12D, 0.08D, 0.01D);
    }

    private void pullPlayers(ServerLevel level, Vec3 center, double radius, double strength) {
        AABB area = new AABB(center.x - radius, center.y - 5.0D, center.z - radius,
                center.x + radius, center.y + 5.0D, center.z + radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator())) {
            Vec3 toward = center.subtract(player.position()).multiply(1.0D, 0.35D, 1.0D);
            if (toward.lengthSqr() < 1.0E-4D || toward.horizontalDistanceSqr() > radius * radius) {
                continue;
            }
            Vec3 force = toward.normalize().scale(strength);
            player.setDeltaMovement(player.getDeltaMovement().add(force));
            player.hurtMarked = true;
        }
    }

    private void damagePlayersNear(ServerLevel level, Vec3 center, double radius, float damage,
                                   double horizontalForce, double verticalForce, boolean pull) {
        AABB area = new AABB(center.x - radius, center.y - 4.0D, center.z - radius,
                center.x + radius, center.y + 4.0D, center.z + radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator())) {
            Vec3 delta = player.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (delta.horizontalDistanceSqr() > radius * radius) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 direction = delta.lengthSqr() > 1.0E-4D
                        ? delta.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
                double sign = pull ? -1.0D : 1.0D;
                player.setDeltaMovement(player.getDeltaMovement().add(
                        direction.x * horizontalForce * sign, verticalForce,
                        direction.z * horizontalForce * sign));
                player.hurtMarked = true;
            }
        }
    }

    private void broadcastNearby(ServerLevel level, Component message) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                candidate -> !candidate.isSpectator()
                        && candidate.distanceToSqr(this) <= radius * radius)) {
            player.displayClientMessage(message, true);
        }
    }

    private static Vec3 rotateHorizontal(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D,
                vector.x * sin + vector.z * cos).normalize();
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int points,
                             net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void spiral(ServerLevel level, Vec3 center, double radius, double spin,
                               net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < 10; i++) {
            double angle = spin + i * Math.PI * 0.4D;
            double localRadius = radius * (0.35D + i / 14.0D);
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * localRadius,
                    center.y - 1.6D + i * 0.34D,
                    center.z + Math.sin(angle) * localRadius,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void drawLine(ServerLevel level, Vec3 from, Vec3 to, double spacing,
                                 net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = from.add(direction.scale(distance));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isDeepRechallengeCombatBlocked(serverLevel)) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof Enemy) {
            return false;
        }
        if (!this.level().isClientSide && this.attackMode == AttackMode.CORE_BREAK
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.void_prophet.phase_locked"), true);
                this.protectedMessageCooldown = 40;
            }
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            this.registerParticipant(attacker);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.58D, this.getZ(),
                    9, 0.65D, 1.2D, 0.65D, 0.08D);
        }
        return hurt;
    }

    @Override
    public float phaseDamageFloor() {
        return this.phaseTransitionTriggered ? 0.0F : this.getMaxHealth() * ENRAGE_THRESHOLD;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (wasDead || !this.dead || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 死亡只有视觉:getDeathSound 返回 null 是既定演出(见 audit_entity_sounds),不要补音效。
        this.triggerAnim("state_controller", "collapse");
        double rewardRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
        boolean hasEligibleParticipant = false;
        boolean firstKillDrop = false;
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(rewardRadius), player -> !player.isSpectator())) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= rewardRadius * rewardRadius) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                        EchoPermission.VOID_PROPHET_ID);
                hasEligibleParticipant = true;
                if (!EchoAbilityManager.markBossDefeated(player, EchoPermission.VOID_PROPHET_ID)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"));
                    continue;
                }
                firstKillDrop |= firstKill;
            }
        }
        if (hasEligibleParticipant) {
            BossMaterialRewards.drop(this, new net.minecraft.world.item.ItemStack(
                    ModItems.DEEP_ORE_SCRAP.get(), 3 + this.random.nextInt(3)));
            BossMaterialRewards.drop(this, new net.minecraft.world.item.ItemStack(
                    ModItems.GRAVITY_CORE_FRAGMENT.get(), firstKillDrop ? 2 : 1));
            BossMaterialRewards.drop(this, new net.minecraft.world.item.ItemStack(
                    ModItems.SPACE_ANCHOR_SHARD.get(), 1 + this.random.nextInt(2)));
            if (firstKillDrop || this.random.nextFloat() < 0.25F) {
                BossMaterialRewards.drop(this, new net.minecraft.world.item.ItemStack(
                        ModItems.GRAVITY_RESEARCH_RUBBING.get()));
            }
        }
        if (this.arenaCenter != null) {
            this.finishDeepRechallenge(serverLevel);
        }
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                core.x, core.y, core.z, 240, 3.0D, 3.2D, 3.0D, 0.5D);
        serverLevel.sendParticles(BossParticleTheme.VOID_PROPHET.body(),
                core.x, core.y, core.z, 80, 2.0D, 2.5D, 2.0D, 0.2D);
        for (int band = 1; band <= 5; band++) {
            ring(serverLevel, core, band * 1.8D, 36, BossParticleTheme.VOID_PROPHET.spark());
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 2.6F, 1.35F);
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

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void registerParticipant(ServerPlayer player) {
        if (!player.isSpectator() && (this.participants.contains(player.getUUID())
                || this.participants.size() < BossNbtData.MAX_PARTICIPANTS)) {
            this.participants.add(player.getUUID());
        }
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
        return ModSounds.VOID_PROPHET_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VOID_PROPHET_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
        tag.putBoolean("PhaseTransitionTriggered", this.phaseTransitionTriggered);
        tag.putBoolean("PhaseTransitionPending", this.phaseTransitionPending
                || this.attackMode == AttackMode.CORE_BREAK);
        tag.putInt("BlinkCooldown", this.blinkCooldown);
        tag.putInt("RendCooldown", this.rendCooldown);
        tag.putInt("GravityCooldown", this.gravityCooldown);
        tag.putInt("ConstellationCooldown", this.constellationCooldown);
        tag.putInt("LanceCooldown", this.lanceCooldown);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        if (this.arenaCenter != null) {
            tag.putIntArray("ArenaCenter", new int[]{
                    this.arenaCenter.getX(), this.arenaCenter.getY(), this.arenaCenter.getZ()});
        }
        net.minecraft.nbt.ListTag participantsTag = new net.minecraft.nbt.ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        boolean phaseTransitionPending = tag.getBoolean("PhaseTransitionPending");
        this.phaseTransitionPending = phaseTransitionPending;
        this.phaseTransitionTriggered = tag.getBoolean("PhaseTransitionTriggered")
                && !phaseTransitionPending;
        this.blinkCooldown = clampTimer(tag.getInt("BlinkCooldown"), 220);
        this.rendCooldown = clampTimer(tag.getInt("RendCooldown"), 240);
        this.gravityCooldown = clampTimer(tag.getInt("GravityCooldown"), 300);
        this.constellationCooldown = clampTimer(tag.getInt("ConstellationCooldown"), 240);
        this.lanceCooldown = tag.contains("LanceCooldown")
                ? clampTimer(tag.getInt("LanceCooldown"), 260) : 90;
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        BlockPos loadedArena = BossNbtData.loadPositionNear(tag, "ArenaCenter", this.blockPosition(), 64, 32);
        if (loadedArena != null) {
            this.bindArena(loadedArena);
        }
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    private static int clampTimer(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("spell_sway", SPELL_SWAY_ANIM)
                .triggerableAnim("short_hop", SHORT_HOP_ANIM)
                .triggerableAnim("spatial_rend", SPATIAL_REND_ANIM)
                .triggerableAnim("gravity_recoil", GRAVITY_RECOIL_ANIM)
                .triggerableAnim("core_break", CORE_BREAK_ANIM)
                .triggerableAnim("void_lance", VOID_LANCE_ANIM)
                .triggerableAnim("constellation", CONSTELLATION_ANIM));
        // 状态演出单独一个 controller：显形/预兆/校准/死亡都可能与攻击并发,同 controller 会互相截断。
        controllers.add(new AnimationController<>(this, "state_controller", 0,
                state -> PlayState.STOP)
                .triggerableAnim("awaken", AWAKEN_ANIM)
                .triggerableAnim("omen", OMEN_ANIM)
                .triggerableAnim("calibrate", CALIBRATE_ANIM)
                .triggerableAnim("collapse", COLLAPSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void syncVisualAttackState() {
        int duration = switch (this.attackMode) {
            case SHORT_HOP -> 18;
            case SPATIAL_REND -> 32;
            case GRAVITY_RECOIL, CONSTELLATION -> 36;
            case VOID_LANCE -> 34;
            case CORE_BREAK -> 44;
            case NONE -> 0;
        };
        this.entityData.set(DATA_VISUAL_ATTACK_MODE, this.attackMode.ordinal());
        this.entityData.set(DATA_VISUAL_ATTACK_AGE, Math.max(0, duration - this.attackTicks));
        this.entityData.set(DATA_VISUAL_ATTACK_DURATION, duration);
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
        SHORT_HOP,
        SPATIAL_REND,
        GRAVITY_RECOIL,
        CONSTELLATION,
        VOID_LANCE,
        CORE_BREAK
    }

    private record LineRift(Vec3 origin, Vec3 direction, double length) {
    }

    private static final class VoidScar {
        private final Vec3 position;
        private int ticks;

        private VoidScar(Vec3 position, int ticks) {
            this.position = position;
            this.ticks = ticks;
        }
    }
}
