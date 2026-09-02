package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceLayout;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

/** 世界回响体最终 Boss。四道生命地板强制完成五重共鸣转相，高伤害不能跨阶段。 */
public class WorldEcho extends Monster implements GeoEntity, PhaseGatedBoss, BossVisualState {
    private static final EntityDataAccessor<Integer> DATA_ATTACK_MODE =
            SynchedEntityData.defineId(WorldEcho.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_AGE =
            SynchedEntityData.defineId(WorldEcho.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_DURATION =
            SynchedEntityData.defineId(WorldEcho.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.world_echo.idle");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.world_echo.glide");
    private static final RawAnimation WIND =
            RawAnimation.begin().then("animation.world_echo.wind_vortex", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TIDE =
            RawAnimation.begin().then("animation.world_echo.tide_surge", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TRUE_SIGHT =
            RawAnimation.begin().then("animation.world_echo.true_gaze", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GRAVITY =
            RawAnimation.begin().then("animation.world_echo.gravity_crush", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VOID =
            RawAnimation.begin().then("animation.world_echo.void_rift", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation LANCE =
            RawAnimation.begin().then("animation.world_echo.cast", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SLAM =
            RawAnimation.begin().then("animation.world_echo.slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BARRAGE =
            RawAnimation.begin().then("animation.world_echo.echo_barrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation MELEE =
            RawAnimation.begin().then("animation.world_echo.melee_sweep", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BEAM =
            RawAnimation.begin().then("animation.world_echo.beam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FLAME_IMPACT =
            RawAnimation.begin().then("animation.world_echo.flame_impact", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation METEOR =
            RawAnimation.begin().then("animation.world_echo.meteor", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation NOVA =
            RawAnimation.begin().then("animation.world_echo.nova", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SKYFALL =
            RawAnimation.begin().then("animation.world_echo.skyfall", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WING_STORM =
            RawAnimation.begin().then("animation.world_echo.wing_storm", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AERIAL_BARRAGE =
            RawAnimation.begin().then("animation.world_echo.aerial_barrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AERIAL_REAP =
            RawAnimation.begin().then("animation.world_echo.aerial_reap", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PHASE =
            RawAnimation.begin().then("animation.world_echo.phase", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SPAWN =
            RawAnimation.begin().then("animation.world_echo.spawn", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DEATH =
            RawAnimation.begin().then("animation.world_echo.death", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final int ENTRANCE_DURATION_TICKS = 52;
    private static final int DEATH_CINEMATIC_TICKS = 80;
    private static final double BASE_MAX_HEALTH = 1400.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 350.0D;
    private static final float[] PHASE_FLOORS = {0.80F, 0.60F, 0.40F, 0.20F};
    private static final int[] PHASE_COOLDOWNS = {72, 66, 60, 54, 46};
    private static final AttackMode[][] PHASE_ATTACK_DECKS = {
            {AttackMode.WIND, AttackMode.LANCE, AttackMode.SLAM, AttackMode.WIND},
            {AttackMode.TIDE, AttackMode.AERIAL_BARRAGE, AttackMode.BARRAGE,
                    AttackMode.BEAM, AttackMode.TIDE, AttackMode.AERIAL_BARRAGE},
            {AttackMode.TRUE_SIGHT, AttackMode.METEOR, AttackMode.AERIAL_REAP,
                    AttackMode.LANCE, AttackMode.AERIAL_BARRAGE, AttackMode.WING_STORM,
                    AttackMode.NOVA},
            {AttackMode.GRAVITY, AttackMode.SKYFALL, AttackMode.SLAM,
                    AttackMode.AERIAL_BARRAGE, AttackMode.METEOR, AttackMode.BEAM,
                    AttackMode.AERIAL_REAP, AttackMode.WING_STORM, AttackMode.SKYFALL,
                    AttackMode.NOVA},
            {AttackMode.VOID, AttackMode.AERIAL_BARRAGE, AttackMode.NOVA, AttackMode.WIND,
                    AttackMode.AERIAL_REAP, AttackMode.TIDE, AttackMode.SKYFALL,
                    AttackMode.TRUE_SIGHT, AttackMode.GRAVITY, AttackMode.BEAM,
                    AttackMode.METEOR, AttackMode.AERIAL_BARRAGE, AttackMode.SKYFALL,
                    AttackMode.AERIAL_REAP, AttackMode.WING_STORM, AttackMode.FLAME_IMPACT}
    };
    private static final double WIND_EYE_RADIUS = 3.75D;
    private static final double TIDE_WAVE_RADIUS = 9.5D;
    private static final double TIDE_WAVE_HALF_WIDTH = 2.0D;
    private static final int TRUE_SIGHT_RAY_COUNT = 8;
    private static final double TRUE_SIGHT_RAY_WIDTH = 1.4D;
    private static final double GRAVITY_JUMP_HEIGHT = 0.75D;
    private static final double[] VOID_RING_RADII = {4.0D, 9.0D, 15.0D, 21.0D};
    private static final double VOID_RING_HALF_WIDTH = 1.65D;
    private static final double PHASE_SAFE_LANE_HALF_WIDTH = 3.2D;
    private static final float BEAM_DAMAGE_MULTIPLIER = 0.85F;
    private static final float METEOR_DAMAGE_MULTIPLIER = 0.68F;
    private static final float SKYFALL_DAMAGE_MULTIPLIER = 0.88F;
    private static final float AERIAL_BARRAGE_DAMAGE_MULTIPLIER = 0.30F;
    private static final float AERIAL_REAP_DAMAGE_MULTIPLIER = 0.82F;
    private static final double BEAM_RANGE = 36.0D;
    private static final double BEAM_WIDTH = 1.05D;
    private static final int BEAM_TRACK_TICKS = 22;
    private static final double FLAME_RADIUS = 13.0D;
    private static final int FLAME_TICKS = 8 * 20;
    private static final double METEOR_RADIUS = 3.4D;
    private static final int[] NOVA_RELEASE_TICKS = {14, 26, 38};
    private static final double[] NOVA_RADII = {6.0D, 12.0D, 18.0D};
    private static final int SKYFALL_DIVE_TICK = 16;
    private static final int SKYFALL_IMPACT_TICK = 32;
    private static final double SKYFALL_RADIUS = 11.0D;
    private static final int[] WING_STORM_RELEASE_TICKS = {11, 17, 23, 29};
    private static final double[] WING_STORM_RADII = {7.5D, 9.0D, 10.5D, 12.0D};
    private static final int AERIAL_BARRAGE_TAKEOFF_TICKS = 12;
    private static final int[] AERIAL_BARRAGE_RELEASE_TICKS = {24, 34, 44};
    private static final int AERIAL_BARRAGE_TELEGRAPH_TICKS = 6;
    private static final int AERIAL_BARRAGE_DESCENT_TICK = 50;
    private static final double AERIAL_BARRAGE_RANGE = 42.0D;
    private static final double AERIAL_BARRAGE_WIDTH = 0.82D;
    private static final int AERIAL_REAP_TAKEOFF_TICKS = 12;
    private static final int AERIAL_REAP_LOCK_TICK = 22;
    private static final int AERIAL_REAP_STRIKE_TICK = 29;
    private static final double AERIAL_REAP_RADIUS = 5.5D;
    private static final int ARENA_RESET_GRACE_TICKS = 5 * 20;
    private static final int ARENA_RECOVERY_INTERVAL_TICKS = 20;
    private static final float ARENA_RECOVERY_FRACTION = 0.05F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<UUID> participants = new HashSet<>();

    private AttackMode attackMode = AttackMode.NONE;
    private int attackAge;
    private int attackDuration;
    private int attackCooldown = 50;
    private int attackCycle;
    private int phaseGateStage;
    private final java.util.Set<Integer> activatedPhasePylons = new java.util.HashSet<>();
    private boolean phaseTransitionPending;
    private boolean phaseTransitionActive;
    /** 相位柱连点去抖间隔；同时是该截止时间从存档载入时的钳制上限。 */
    private static final long PHASE_MECHANISM_COOLDOWN_TICKS = 20L;
    private long phaseMechanismCooldownUntil;
    private int protectedMessageCooldown;
    private boolean entranceFxPlayed;
    private int entranceTicksRemaining = ENTRANCE_DURATION_TICKS;
    private int scaledParticipants = 1;
    private int arenaEmptyTicks;
    private boolean encounterDormant;
    private UUID attackTargetId;
    private Vec3 lockedAttackAim;
    private double attackStartY;
    private final List<Vec3> meteorImpacts = new ArrayList<>();
    private UUID altarArenaId;
    private BlockPos altarPos;
    private int combatAudienceCacheTick = Integer.MIN_VALUE;
    private List<ServerPlayer> combatAudienceCache = List.of();

    private enum AltarBindingState {
        UNBOUND,
        UNAVAILABLE,
        INVALID,
        VALID
    }

    public WorldEcho(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
    }

    /** 五相环与翅状演出不计入命中范围，主体底面使用长方形 profile。 */
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
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel level && this.altarPos != null
                && level.hasChunkAt(this.altarPos)
                && level.getBlockEntity(this.altarPos)
                instanceof WorldResonanceAltarBlockEntity altar
                && !altar.registerLoadedBoss(this)) {
            this.discard();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 32.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_MODE, AttackMode.NONE.networkId());
        builder.define(DATA_ATTACK_AGE, 0);
        builder.define(DATA_ATTACK_DURATION, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 20 == 0 && !this.retryAltarRegistration(level)) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(level);
        }
        this.enforceArenaBoundary(level);
        if (this.tickCount % 5 == 0) {
            this.syncBossEventViewers(level);
        }
        if (!this.entranceFxPlayed) {
            this.entranceFxPlayed = true;
            this.playEntranceFx(level);
        }
        if (this.tickCount % 5 == 0 && ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
            this.updateDynamicScaling(level);
        }
        this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        this.protectedMessageCooldown = decrement(this.protectedMessageCooldown);
        if (this.tickEntrance(level)) {
            return;
        }
        if (this.tickEncounterContinuity(level)) {
            return;
        }
        this.tickArenaResonanceFx(level);
        this.attackCooldown = decrement(this.attackCooldown);
        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            this.registerNearbyParticipants(level);
        }
        if (this.phaseGateStage < PHASE_FLOORS.length
                && !this.phaseTransitionPending && !this.phaseTransitionActive
                && BossPhaseGate.reachedFloor(this, this.currentMechanicFloor())) {
            this.phaseTransitionPending = true;
            this.activatedPhasePylons.clear();
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            this.tickAttack(level);
            this.attackAge++;
            this.entityData.set(DATA_ATTACK_AGE, this.attackAge);
            if (this.attackAge >= this.attackDuration) {
                boolean completedTransition = this.phaseTransitionActive;
                this.finishAttack();
                if (completedTransition) {
                    this.completePhaseTransition(level);
                }
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (this.phaseTransitionPending) {
            this.drawPhaseMechanismGuidance(level);
            return;
        }
        if (target == null || !target.isAlive() || this.attackCooldown > 0) {
            return;
        }

        AttackMode mode = this.nextAttackMode();
        this.beginAttack(mode, duration(mode), trigger(mode), target);
        this.attackCooldown = this.specialCooldown();
    }

    /** 实体可能先于祭坛方块实体加入世界，低频重试精确绑定而不扩大祭坛扫描范围。 */
    private boolean retryAltarRegistration(ServerLevel level) {
        if (this.altarPos == null || !level.hasChunkAt(this.altarPos)) {
            return true;
        }
        if (level.getBlockEntity(this.altarPos)
                instanceof WorldResonanceAltarBlockEntity altar) {
            return altar.registerLoadedBoss(this);
        }

        return level.getBlockState(this.altarPos).getBlock() instanceof WorldResonanceAltarBlock;
    }

    private void beginAttack(AttackMode mode, int duration, String trigger, LivingEntity target) {
        this.attackMode = mode;
        this.attackAge = 0;
        this.attackDuration = duration;
        this.entityData.set(DATA_ATTACK_MODE, mode.networkId());
        this.entityData.set(DATA_ATTACK_AGE, 0);
        this.entityData.set(DATA_ATTACK_DURATION, duration);
        this.attackTargetId = target.getUUID();
        this.lockedAttackAim = mode == AttackMode.SKYFALL
                ? this.lockSkyfallImpact(target)
                : target.getEyePosition();
        this.attackStartY = this.getY();
        this.meteorImpacts.clear();
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        this.faceAttackPosition(target.position());
        this.triggerAnim("attack_controller", trigger);
        this.updateBossEventName();
        if (mode != AttackMode.PHASE) {
            this.announceAttack(mode);
        }
    }

    private void faceAttackPosition(Vec3 position) {
        Vec3 direction = position.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
    }

    private void tickAttack(ServerLevel level) {
        // 能力专属招式固定对应一位守护者,通用招式跟随当前阶段。
        // 保留原生粒子的几项表达体量与材质而不是身份色,不要统一成主题色。
        ParticleOptions chargeParticle = switch (this.attackMode) {
            case WIND -> BossParticleTheme.COLOSSUS.body();
            case TIDE -> BossParticleTheme.ABYSS.body();
            case TRUE_SIGHT -> BossParticleTheme.MIRROR.body();
            case GRAVITY -> BossParticleTheme.VOID_PROPHET.body();
            case VOID -> BossParticleTheme.RIFT.body();
            case LANCE, BARRAGE -> this.currentBody();
            case SLAM -> ParticleTypes.WAX_OFF;
            case BEAM -> this.currentBody();
            case FLAME_IMPACT -> ParticleTypes.SOUL_FIRE_FLAME;
            case METEOR -> this.currentSpark();
            case NOVA -> ParticleTypes.GLOW;
            case SKYFALL -> this.currentEmber();
            case WING_STORM -> this.currentBody();
            case AERIAL_BARRAGE -> this.currentBody();
            case AERIAL_REAP -> ParticleTypes.GLOW;
            case PHASE -> aspectParticle(this.nextAspectIndex());
            case NONE -> this.currentBody();
        };
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        if (this.attackAge % 4 == 0) {
            level.sendParticles(chargeParticle, core.x, core.y, core.z,
                    12, 1.2D, 1.4D, 1.2D, 0.05D);
        }
        BossFx.chargeSpiral(level, this.position(), this.tickCount, chargeParticle);
        this.tickChargeFx(level, core, chargeParticle);
        switch (this.attackMode) {
            case WIND -> {
                if (this.attackAge == 34) {
                    this.releaseWind(level);
                }
            }
            case TIDE -> {
                if (this.attackAge == 20) {
                    this.releaseTide(level);
                }
            }
            case TRUE_SIGHT -> {
                if (this.attackAge == 22) {
                    this.releaseTrueSight(level);
                }
            }
            case GRAVITY -> {
                if (this.attackAge == 28) {
                    this.releaseGravity(level);
                }
            }
            case VOID -> {
                if (this.attackAge == 24) {
                    this.releaseVoid(level);
                }
            }
            case LANCE -> {
                if (this.attackAge == 14) {
                    this.releaseLance(level);
                }
            }
            case SLAM -> {
                if (this.attackAge == 22) {
                    this.releaseSlam(level);
                }
            }
            case BARRAGE -> {
                if (this.attackAge == 6 || this.attackAge == 18 || this.attackAge == 30) {
                    this.releaseBarrage(level, this.attackAge / 12);
                }
            }
            case BEAM -> this.tickBeam(level);
            case FLAME_IMPACT -> {
                if (this.attackAge == 22) {
                    this.releaseFlameImpact(level);
                }
            }
            case METEOR -> this.tickMeteor(level);
            case NOVA -> this.tickNova(level);
            case SKYFALL -> this.tickSkyfall(level);
            case WING_STORM -> this.tickWingStorm(level);
            case AERIAL_BARRAGE -> this.tickAerialBarrage(level);
            case AERIAL_REAP -> this.tickAerialReap(level);
            case PHASE -> this.tickPhaseTransition(level);
            case NONE -> {
            }
        }
        this.emitCinematicAttackFeedback(level);
    }

    private void emitCinematicAttackFeedback(ServerLevel level) {
        Vec3 impact = switch (this.attackMode) {
            case METEOR, SKYFALL, AERIAL_REAP -> this.lockedAttackAim != null
                    ? this.lockedAttackAim : this.position();
            default -> this.position();
        };
        switch (this.attackMode) {
            case WIND -> this.cinematicPulseAt(level, impact, 34, 0.58F, 0x86F1FF);
            case TIDE -> this.cinematicPulseAt(level, impact, 20, 0.62F, 0x45BFEA);
            case TRUE_SIGHT -> this.cinematicPulseAt(level, impact, 22, 0.58F, 0xD09BFF);
            case GRAVITY -> this.cinematicPulseAt(level, impact, 28, 0.78F, 0xFFD982);
            case VOID -> this.cinematicPulseAt(level, impact, 24, 0.72F, 0xA45AFF);
            case LANCE -> this.cinematicPulseAt(level, impact, 14, 0.48F, 0xE8FCFF);
            case SLAM -> this.cinematicPulseAt(level, impact, 22, 0.82F, 0xFFC66D);
            case BARRAGE -> {
                if (this.attackAge == 6 || this.attackAge == 18 || this.attackAge == 30) {
                    BossFx.cinematicImpact(level, impact, 30.0D, 0.3F, 4, 0x9DF4FF);
                }
            }
            case BEAM -> this.cinematicPulseAt(level, impact, 34, 0.56F, 0xC6FBFF);
            case FLAME_IMPACT -> this.cinematicPulseAt(level, impact, 22, 0.78F, 0xFF7538);
            case METEOR -> this.cinematicPulseAt(level, impact, 32, 0.72F, 0xD47EFF);
            case NOVA -> {
                for (int index = 0; index < NOVA_RELEASE_TICKS.length; index++) {
                    if (this.attackAge == NOVA_RELEASE_TICKS[index]) {
                        BossFx.cinematicImpact(level, impact, 38.0D,
                                0.42F + index * 0.16F, 5 + index * 2, 0xF6E8FF);
                    }
                }
            }
            case SKYFALL -> this.cinematicPulseAt(level, impact, SKYFALL_IMPACT_TICK, 1.0F, 0xFFF0A8);
            case WING_STORM -> {
                for (int releaseTick : WING_STORM_RELEASE_TICKS) {
                    if (this.attackAge == releaseTick) {
                        BossFx.cinematicImpact(level, impact, 34.0D, 0.34F, 4, 0x7FE7FF);
                    }
                }
            }
            case AERIAL_BARRAGE -> {
                for (int releaseTick : AERIAL_BARRAGE_RELEASE_TICKS) {
                    if (this.attackAge == releaseTick) {
                        BossFx.cinematicImpact(level, impact, 32.0D, 0.38F, 4, 0xA8F4FF);
                    }
                }
            }
            case AERIAL_REAP -> this.cinematicPulseAt(level, impact, AERIAL_REAP_STRIKE_TICK,
                    0.84F, 0xE4B2FF);
            case PHASE -> this.cinematicPulseAt(level, impact, 24, 0.92F, 0xF2D7FF);
            case NONE -> {
            }
        }
    }

    private void cinematicPulseAt(ServerLevel level, Vec3 impact, int releaseTick,
                                  float strength, int color) {
        if (this.attackAge == releaseTick) {
            BossFx.cinematicImpact(level, impact, 38.0D, strength,
                    strength >= 0.8F ? 11 : 7, color);
        }
    }

    private void tickChargeFx(ServerLevel level, Vec3 core, ParticleOptions theme) {
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        switch (this.attackMode) {
            case WIND -> {
                BossFx.vortex(level, ground, 3.4D, 5.5D, this.tickCount, ParticleTypes.CLOUD);
                if (this.attackAge == 12 || this.attackAge == 24 || this.attackAge == 32) {
                    BossFx.ring(level, ground, WIND_EYE_RADIUS, 36, 0.12D, ParticleTypes.GLOW);
                }
            }
            case TIDE -> {
                if (this.attackAge % 3 == 0) {
                    BossFx.converge(level, ground.add(0.0D, 0.6D, 0.0D), 6.0D, 14, BossParticleTheme.ABYSS.body());

                    level.sendParticles(BossParticleTheme.ABYSS.body(),
                            core.x, core.y - 0.8D, core.z, 6, 1.0D, 0.6D, 1.0D, 0.02D);
                    level.sendParticles(ParticleTypes.DOLPHIN,
                            core.x, core.y - 0.4D, core.z, 4, 0.8D, 0.8D, 0.8D, 0.02D);
                }
                if (this.attackAge == 8 || this.attackAge == 14 || this.attackAge == 18) {
                    BossFx.ring(level, ground, TIDE_WAVE_RADIUS, 60, 0.12D, BossParticleTheme.ABYSS.ember());
                }
            }
            case TRUE_SIGHT -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, core, 5.0D, 12, BossParticleTheme.MIRROR.body());
                }
                if (this.attackAge == 10 || this.attackAge == 14
                        || this.attackAge == 18 || this.attackAge == 20) {
                    this.renderTrueSightRayTelegraph(level);
                }
            }
            case GRAVITY -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, ground.add(0.0D, 0.4D, 0.0D), 7.0D, 16, BossParticleTheme.VOID_PROPHET.body());
                }
                if (this.attackAge == 14 || this.attackAge == 22 || this.attackAge == 26) {
                    BossFx.ring(level, ground, 12.0D, 72, 0.12D, BossParticleTheme.VOID_PROPHET.ember());
                }
            }
            case VOID -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, core, 6.5D, 14, BossParticleTheme.RIFT.body());
                }
                if (this.attackAge % 6 == 0) {
                    level.sendParticles(BossParticleTheme.RIFT.ember(),
                            core.x, core.y + 0.4D, core.z, 4, 1.4D, 1.1D, 1.4D, 0.02D);
                }
                if (this.attackAge == 8 || this.attackAge == 16 || this.attackAge == 22) {
                    this.renderVoidRingTelegraph(level, ground);
                }
            }
            case PHASE -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, core, 7.5D, 18, theme);
                }
                if (this.attackAge % 6 == 0) {
                    BossFx.ring(level, ground, 3.0D + this.attackAge * 0.18D,
                            32, 0.12D, theme);
                }
            }
            case FLAME_IMPACT -> BossFx.vortex(level, ground, 2.8D, 4.5D, this.tickCount, ParticleTypes.FLAME);
            case BEAM -> {
                if (this.attackAge % 3 == 0) {
                    BossFx.converge(level, this.attackOrigin(), 4.0D, 10, this.currentBody());
                }
            }
            case SLAM -> {
                if (this.attackAge % 5 == 0) {
                    BossFx.ring(level, ground, 2.4D + (this.attackAge % 15) * 0.1D, 18, 0.1D, ParticleTypes.CRIT);
                }
            }
            case NOVA -> {
                if (this.attackAge % 4 == 0) {
                    level.sendParticles(ParticleTypes.GLOW,
                            core.x, core.y, core.z, 8, 0.6D, 1.0D, 0.6D, 0.08D);
                }
            }
            case SKYFALL -> {
                if (this.attackAge < SKYFALL_DIVE_TICK) {
                    BossFx.vortex(level, this.position(), 3.8D, 6.5D,
                            this.tickCount, ParticleTypes.CLOUD);
                }
                if (this.lockedAttackAim != null && this.attackAge < SKYFALL_IMPACT_TICK
                        && (this.attackAge & 1) == 0) {
                    BossFx.lockSigil(level, this.lockedAttackAim, SKYFALL_RADIUS,
                            this.attackAge / (double) SKYFALL_IMPACT_TICK,
                            this.attackAge, this.currentEmber());
                }
            }
            case WING_STORM -> {
                if (this.attackAge % 4 == 0) {
                    BossFx.vortex(level, this.position(), 4.8D, 4.5D,
                            this.tickCount, this.currentBody());
                }
                for (int wave = 0; wave < WING_STORM_RELEASE_TICKS.length; wave++) {
                    if (this.attackAge == WING_STORM_RELEASE_TICKS[wave] - 3) {
                        BossFx.ring(level, ground, WING_STORM_RADII[wave],
                                48, 0.12D, this.currentEmber());
                    }
                }
            }
            case AERIAL_BARRAGE -> {
                if (this.attackAge < AERIAL_BARRAGE_DESCENT_TICK && (this.attackAge & 1) == 0) {
                    BossFx.vortex(level, this.position(), 4.2D, 4.0D,
                            this.tickCount, this.currentBody());
                }
            }
            case AERIAL_REAP -> {
                if (this.attackAge < AERIAL_REAP_LOCK_TICK && (this.attackAge & 1) == 0) {
                    BossFx.vortex(level, this.position(), 3.6D, 4.8D,
                            this.tickCount, ParticleTypes.CLOUD);
                }
                if (this.lockedAttackAim != null && this.attackAge >= AERIAL_REAP_TAKEOFF_TICKS
                        && this.attackAge < AERIAL_REAP_STRIKE_TICK) {
                    BossFx.guidanceFlow(level, this.attackOrigin(), this.lockedAttackAim,
                            ParticleTypes.GLOW, this.currentEmber(), this.attackAge);
                }
            }
            case LANCE, BARRAGE, METEOR, NONE -> {
            }
        }
    }

    private void tickSkyfall(ServerLevel level) {
        this.setNoGravity(this.attackAge < SKYFALL_IMPACT_TICK);
        this.fallDistance = 0.0F;
        Vec3 impact = this.lockedAttackAim == null ? this.position() : this.lockedAttackAim;

        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), ModSounds.WORLD_ECHO_FLAP.get(),
                    SoundSource.HOSTILE, 2.8F, 1.0F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.attackAge < SKYFALL_DIVE_TICK) {
            Vec3 movement = this.getDeltaMovement();
            double lift = this.attackAge < 10 ? 0.68D : 0.05D;
            this.setDeltaMovement(movement.x * 0.2D, lift, movement.z * 0.2D);
            return;
        }
        if (this.attackAge < SKYFALL_IMPACT_TICK) {
            Vec3 diveTarget = impact.add(0.0D, 0.2D, 0.0D);
            Vec3 velocity = diveTarget.subtract(this.position())
                    .scale(1.0D / Math.max(1, SKYFALL_IMPACT_TICK - this.attackAge));
            double speed = velocity.length();
            if (speed > 2.4D) {
                velocity = velocity.scale(2.4D / speed);
            }
            this.setDeltaMovement(velocity);
            this.faceAttackPosition(impact);
            if ((this.attackAge & 1) == 0) {
                BossFx.flightTrail(level, this.attackOrigin(), this.getDeltaMovement(),
                        1.15D, this.currentBody());
            }
            level.sendParticles(this.currentBody(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.45D, this.getZ(),
                    10, 0.7D, 0.9D, 0.7D, 0.08D);
            if ((this.attackAge & 1) == 0) {
                BossFx.guidanceFlow(level, this.attackOrigin(), diveTarget,
                        ParticleTypes.CLOUD, this.currentEmber(), this.attackAge);
            }
            return;
        }
        if (this.attackAge == SKYFALL_IMPACT_TICK) {
            this.resetFlightState();
            this.setDeltaMovement(0.0D, 0.12D, 0.0D);
            this.releaseSkyfall(level, impact);
        }
    }

    private void releaseSkyfall(ServerLevel level, Vec3 impact) {
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 3.0F, 0.58F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impact.x, impact.y + 0.2D, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                impact.x, impact.y + 0.3D, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        BossFx.groundShockwave(level, impact, SKYFALL_RADIUS, this.currentEmber());
        BossFx.areaBurst(level, impact, this.currentSpark());
        BossFx.pillar(level, impact, 12.0D, this.currentBody());
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * SKYFALL_DAMAGE_MULTIPLIER;
        AABB impactArea = new AABB(impact, impact)
                .inflate(SKYFALL_RADIUS, 5.0D, SKYFALL_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                impactArea, this::isEligibleCombatPlayer)) {
            Vec3 horizontal = player.position().subtract(impact).multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.lengthSqr() > SKYFALL_RADIUS * SKYFALL_RADIUS
                    || Math.abs(player.getY() - impact.y) > 5.0D) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = horizontal.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        away.x * 1.15D, 0.85D, away.z * 1.15D));
                player.hurtMarked = true;
                BossFx.impactBurst(level, player.position(), this.currentSpark());
            }
        }
    }

    private void tickWingStorm(ServerLevel level) {
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.HOSTILE, 2.6F, 1.05F);
        }
        for (int wave = 0; wave < WING_STORM_RELEASE_TICKS.length; wave++) {
            if (this.attackAge == WING_STORM_RELEASE_TICKS[wave]) {
                this.releaseWingStorm(level, wave);
            }
        }
    }

    private void releaseWingStorm(ServerLevel level, int wave) {
        double radius = WING_STORM_RADII[wave];
        ParticleOptions theme = (wave & 1) == 0
                ? this.currentBody() : this.currentBody();
        double angle = Math.toRadians(this.getYRot() + 90.0F + wave * 45.0F);
        Vec3 forward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.42D, 0.0D);

        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.3F, 0.78F + wave * 0.16F);
        BossFx.sweepArc(level, origin, forward, radius, theme);
        BossFx.sweepArc(level, origin, forward.scale(-1.0D), radius, theme);
        BossFx.sweepArc(level, origin, side, radius, ParticleTypes.GLOW);
        BossFx.sweepArc(level, origin, side.scale(-1.0D), radius, ParticleTypes.GLOW);
        BossFx.ring(level, origin, radius, Math.max(40, (int) (radius * 6.0D)), 0.12D, theme);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.20F;
        AABB waveArea = new AABB(this.position(), this.position())
                .inflate(radius, 5.0D, radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                waveArea, this::isEligibleCombatPlayer)) {
            if (Math.abs(player.getY() - this.getY()) > 5.0D) {
                continue;
            }
            Vec3 horizontal = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.lengthSqr() > radius * radius
                    || !player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            Vec3 away = horizontal.lengthSqr() < 1.0E-4D
                    ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 0.42D, 0.22D, away.z * 0.42D));
            player.hurtMarked = true;
        }
    }

    private void tickAerialBarrage(ServerLevel level) {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        LivingEntity target = this.resolveAttackTarget(level);

        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.HOSTILE, 2.8F, 0.9F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.attackAge < AERIAL_BARRAGE_TAKEOFF_TICKS) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.35D, 0.52D, movement.z * 0.35D);
            this.hasImpulse = true;
            return;
        }
        if (this.attackAge < AERIAL_BARRAGE_DESCENT_TICK) {
            if ((this.attackAge & 1) == 0) {
                BossFx.flightTrail(level, this.attackOrigin(), this.getDeltaMovement(),
                        0.95D, this.currentBody());
            }
            if (target != null) {
                this.getLookControl().setLookAt(target, 55.0F, 55.0F);
                this.faceAttackPosition(target.position());
                double orbit = this.getId() * 0.37D
                        + (this.attackAge - AERIAL_BARRAGE_TAKEOFF_TICKS) * 0.085D;
                Vec3 hover = target.position().add(
                        Math.cos(orbit) * 9.5D, 7.5D, Math.sin(orbit) * 9.5D);
                this.steerFlight(this.clampFlightPoint(hover), 0.18D, 0.82D);
            } else {
                this.steerFlight(new Vec3(this.getX(), this.attackStartY + 6.0D, this.getZ()),
                        0.16D, 0.55D);
            }
            for (int volley = 0; volley < AERIAL_BARRAGE_RELEASE_TICKS.length; volley++) {
                int releaseTick = AERIAL_BARRAGE_RELEASE_TICKS[volley];
                int telegraphTick = releaseTick - AERIAL_BARRAGE_TELEGRAPH_TICKS;
                if (this.attackAge == telegraphTick && target != null) {
                    this.lockedAttackAim = target.getEyePosition().add(
                            target.getDeltaMovement().scale(AERIAL_BARRAGE_TELEGRAPH_TICKS));
                }
                if (this.lockedAttackAim != null
                        && this.attackAge >= telegraphTick && this.attackAge < releaseTick) {
                    Vec3 telegraphEnd = this.clipRay(level, this.attackOrigin(), this.lockedAttackAim);
                    BossFx.guidanceFlow(level, this.attackOrigin(), telegraphEnd,
                            this.currentEmber(), this.currentEmber(), this.attackAge);
                    if ((this.attackAge & 1) == 0) {
                        double progress = (this.attackAge - telegraphTick)
                                / (double) AERIAL_BARRAGE_TELEGRAPH_TICKS;
                        BossFx.lockSigil(level, telegraphEnd, 1.35D, progress,
                                this.attackAge, this.currentEmber());
                    }
                }
                if (this.attackAge == releaseTick) {
                    this.releaseAerialBarrage(level, volley);
                }
            }
            return;
        }

        this.steerFlight(new Vec3(this.getX(), this.attackStartY, this.getZ()), 0.22D, 0.68D);
        if (this.attackAge >= 57) {
            this.resetFlightState();
        }
    }

    private void releaseAerialBarrage(ServerLevel level, int volley) {
        Vec3 origin = this.attackOrigin();
        Vec3 aim = this.lockedAttackAim;
        if (aim == null) {
            return;
        }
        Vec3 direction = aim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize();
        Vec3 end = this.clipRay(level, origin,
                origin.add(direction.scale(AERIAL_BARRAGE_RANGE)));
        double reach = origin.distanceTo(end);
        this.lockedAttackAim = aim;

        level.playSound(null, this.blockPosition(), SoundEvents.SHULKER_SHOOT,
                SoundSource.HOSTILE, 2.2F, 0.78F + volley * 0.18F);
        BossFx.sparkSpray(level, origin, direction, reach, this.currentSpark());
        BossFx.helix(level, origin, end, 0.32D,
                volley == 1 ? ParticleTypes.GLOW : this.currentBody());
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * AERIAL_BARRAGE_DAMAGE_MULTIPLIER;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(origin, end).inflate(AERIAL_BARRAGE_WIDTH), this::isEligibleCombatPlayer)) {
            Vec3 toPlayer = player.getBoundingBox().getCenter().subtract(origin);
            double along = toPlayer.dot(direction);
            if (along < 0.0D || along > reach) {
                continue;
            }
            double offLine = toPlayer.subtract(direction.scale(along)).length();
            if (offLine > AERIAL_BARRAGE_WIDTH + player.getBbWidth() * 0.5D
                    || !player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            BossFx.impactBurst(level, player.position(), this.currentSpark());
            player.setDeltaMovement(player.getDeltaMovement().add(
                    direction.x * 0.32D, 0.14D, direction.z * 0.32D));
            player.hurtMarked = true;
        }
        BossFx.impactBurst(level, end, this.currentSpark());
    }

    private void tickAerialReap(ServerLevel level) {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        LivingEntity target = this.resolveAttackTarget(level);

        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.HOSTILE, 3.0F, 0.68F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.attackAge < AERIAL_REAP_TAKEOFF_TICKS) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.3D, 0.55D, movement.z * 0.3D);
            this.hasImpulse = true;
            return;
        }
        if (this.attackAge < AERIAL_REAP_LOCK_TICK) {
            if (target != null) {
                Vec3 horizontalAway = this.position().subtract(target.position())
                        .multiply(1.0D, 0.0D, 1.0D);
                horizontalAway = horizontalAway.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : horizontalAway.normalize();
                Vec3 staging = target.position().add(horizontalAway.scale(10.0D))
                        .add(0.0D, 6.0D, 0.0D);
                this.steerFlight(this.clampFlightPoint(staging), 0.2D, 0.9D);
                this.lockedAttackAim = target.getBoundingBox().getCenter();
                this.getLookControl().setLookAt(target, 60.0F, 60.0F);
                this.faceAttackPosition(target.position());
            }
            return;
        }
        if (this.attackAge == AERIAL_REAP_LOCK_TICK) {
            if (target != null) {
                this.lockedAttackAim = target.getBoundingBox().getCenter();
            }
            level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(),
                    SoundSource.HOSTILE, 2.8F, 0.72F);
        }
        if (this.attackAge <= AERIAL_REAP_STRIKE_TICK && this.lockedAttackAim != null) {
            this.faceAttackPosition(this.lockedAttackAim);
            this.steerFlight(this.lockedAttackAim, 0.48D, 2.35D);
            if ((this.attackAge & 1) == 0) {
                BossFx.flightTrail(level, this.attackOrigin(), this.getDeltaMovement(),
                        1.25D, ParticleTypes.GLOW);
                double progress = Math.max(0.0D,
                        (this.attackAge - AERIAL_REAP_LOCK_TICK)
                                / (double) (AERIAL_REAP_STRIKE_TICK - AERIAL_REAP_LOCK_TICK));
                BossFx.lockSigil(level, this.lockedAttackAim, AERIAL_REAP_RADIUS,
                        progress, this.attackAge, this.currentEmber());
            }
            level.sendParticles(this.currentBody(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.45D, this.getZ(),
                    12, 0.8D, 0.8D, 0.8D, 0.08D);
        }
        if (this.attackAge == AERIAL_REAP_STRIKE_TICK) {
            this.releaseAerialReap(level);
        }
        if (this.attackAge > AERIAL_REAP_STRIKE_TICK) {
            this.steerFlight(new Vec3(this.getX(), this.attackStartY, this.getZ()),
                    0.24D, 0.78D);
            if (this.attackAge >= 48) {
                this.resetFlightState();
            }
        }
    }

    private void releaseAerialReap(ServerLevel level) {
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.42D, 0.0D);
        Vec3 forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        forward = forward.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 3.0F, 0.58F);
        BossFx.sweepArc(level, origin, forward, AERIAL_REAP_RADIUS, this.currentSpark());
        BossFx.sweepArc(level, origin, side, AERIAL_REAP_RADIUS, this.currentSpark());
        BossFx.impactBurst(level, origin, ParticleTypes.GLOW);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * AERIAL_REAP_DAMAGE_MULTIPLIER;
        AABB strikeArea = this.getBoundingBox().inflate(
                AERIAL_REAP_RADIUS, 3.5D, AERIAL_REAP_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                strikeArea, this::isEligibleCombatPlayer)) {
            if (player.distanceToSqr(this) > square(AERIAL_REAP_RADIUS)
                    || !player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            Vec3 away = horizontalAway(player);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 0.85D, 0.42D, away.z * 0.85D));
            player.hurtMarked = true;
            BossFx.impactBurst(level, player.position(), this.currentSpark());
        }
    }

    private void steerFlight(Vec3 target, double acceleration, double maxSpeed) {
        Vec3 offset = target.subtract(this.position());
        Vec3 desired = offset.lengthSqr() < 1.0E-4D
                ? Vec3.ZERO : offset.normalize().scale(Math.min(maxSpeed, offset.length() * acceleration));
        Vec3 velocity = this.getDeltaMovement().scale(0.55D).add(desired.scale(0.45D));
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
            velocity = velocity.normalize().scale(maxSpeed);
        }
        this.setDeltaMovement(velocity);
        this.hasImpulse = true;
    }

    private Vec3 clampFlightPoint(Vec3 desired) {
        if (this.altarPos == null || this.altarArenaId == null) {
            return desired;
        }
        Vec3 center = new Vec3(this.altarPos.getX() + 0.5D,
                desired.y, this.altarPos.getZ() + 0.5D);
        Vec3 horizontal = desired.subtract(center).multiply(1.0D, 0.0D, 1.0D);
        double limit = Math.max(8.0D, this.arenaRadius() - 6.0D);
        if (horizontal.lengthSqr() <= limit * limit) {
            return desired;
        }
        Vec3 clamped = horizontal.normalize().scale(limit);
        return new Vec3(center.x + clamped.x, desired.y, center.z + clamped.z);
    }

    private void tickBeam(ServerLevel level) {
        LivingEntity target = this.resolveAttackTarget(level);
        if (this.attackAge <= BEAM_TRACK_TICKS && target != null) {
            this.lockedAttackAim = target.getEyePosition();
            this.getLookControl().setLookAt(target, 40.0F, 40.0F);
            this.faceAttackPosition(target.position());
        }
        if (this.lockedAttackAim == null) {
            return;
        }

        Vec3 origin = this.attackOrigin();
        Vec3 telegraphEnd = this.clipRay(level, origin, this.lockedAttackAim);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE, 2.4F, 0.65F);
        }
        if (this.attackAge <= 32 && this.attackAge % 2 == 0) {
            BossFx.guidanceFlow(level, origin, telegraphEnd,
                    this.currentEmber(), this.currentEmber(), this.attackAge);
        }
        if (this.attackAge >= BEAM_TRACK_TICKS && this.attackAge < 34 && this.attackAge % 2 == 0) {
            double progress = (this.attackAge - BEAM_TRACK_TICKS)
                    / (double) (34 - BEAM_TRACK_TICKS);
            BossFx.lockSigil(level, this.lockedAttackAim, 1.6D, progress,
                    this.attackAge, this.currentEmber());
        }
        if (this.attackAge == 34) {
            this.releaseBeam(level);
        }
    }

    private void releaseBeam(ServerLevel level) {
        Vec3 origin = this.attackOrigin();
        Vec3 aim = this.lockedAttackAim == null ? origin.add(this.getLookAngle()) : this.lockedAttackAim;
        Vec3 direction = aim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize();
        Vec3 beamEnd = this.clipRay(level, origin, origin.add(direction.scale(BEAM_RANGE)));
        double reach = origin.distanceTo(beamEnd);

        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 3.0F, 0.72F);
        BossFx.sparkSpray(level, origin, direction, reach, this.currentSpark());
        BossFx.helix(level, origin, beamEnd, 0.5D, this.currentBody());
        int step = 0;
        for (double distance = 0.4D; distance <= reach; distance += 0.5D) {
            Vec3 point = origin.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.GLOW, point.x, point.y, point.z,
                    2, 0.11D, 0.11D, 0.11D, 0.0D);
            if (step++ % 2 == 0) {
                level.sendParticles(this.currentSpark(), point.x, point.y, point.z,
                        2, 0.18D, 0.18D, 0.18D, 0.025D);
            }
        }

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * BEAM_DAMAGE_MULTIPLIER;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(origin, beamEnd).inflate(BEAM_WIDTH), this::isEligibleCombatPlayer)) {
            Vec3 toPlayer = player.getBoundingBox().getCenter().subtract(origin);
            double along = toPlayer.dot(direction);
            if (along < 0.0D || along > reach) {
                continue;
            }
            double offLine = toPlayer.subtract(direction.scale(along)).length();
            if (offLine > BEAM_WIDTH + player.getBbWidth() * 0.5D) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                BossFx.impactBurst(level, player.position(), this.currentSpark());
                player.setDeltaMovement(player.getDeltaMovement().add(
                        direction.x * 0.55D, 0.18D, direction.z * 0.55D));
                player.hurtMarked = true;
            }
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                beamEnd.x, beamEnd.y, beamEnd.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        BossFx.impactBurst(level, beamEnd, this.currentSpark());
    }

    private void releaseFlameImpact(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 2.6F, 0.55F);
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.75F);
        Vec3 center = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.groundShockwave(level, center, 5.0D, ParticleTypes.FLAME);
        BossFx.groundShockwave(level, center, 9.0D, ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.groundShockwave(level, center, FLAME_RADIUS, ParticleTypes.LAVA);
        BossFx.impactBurst(level, this.attackOrigin(), ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.pillar(level, center, 7.0D, ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.vortex(level, center, 4.0D, 6.0D, this.tickCount, ParticleTypes.FLAME);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.60F;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(FLAME_RADIUS, 5.0D, FLAME_RADIUS), this::isFlameTarget)) {
            Vec3 offset = victim.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (offset.lengthSqr() > FLAME_RADIUS * FLAME_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), FLAME_TICKS));
                Vec3 away = offset.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : offset.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(
                        away.x * 0.75D, 0.35D, away.z * 0.75D));
                victim.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.FLAME,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    18, victim.getBbWidth() * 0.4D, victim.getBbHeight() * 0.3D,
                    victim.getBbWidth() * 0.4D, 0.08D);
        }
        this.igniteGround(level);
    }

    private boolean isFlameTarget(LivingEntity entity) {
        return entity != this && entity.isAlive() && !entity.isSpectator()
                && (!(entity instanceof Player player) || !player.getAbilities().invulnerable)
                && this.canAttack(entity);
    }

    private void igniteGround(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        double phase = this.tickCount * 0.19D;
        int[] verticalOffsets = {0, 1, -1, 2, -2};
        for (int index = 0; index < 18; index++) {
            double radius = 3.5D + (index % 3) * 3.5D;
            double angle = phase + index * (Math.PI * 2.0D / 18.0D);
            BlockPos origin = BlockPos.containing(
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.2D,
                    this.getZ() + Math.sin(angle) * radius);
            for (int offset : verticalOffsets) {
                BlockPos firePos = origin.offset(0, offset, 0);
                if (level.isEmptyBlock(firePos)
                        && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
    }

    private void tickMeteor(ServerLevel level) {
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.HOSTILE, 2.2F, 0.55F);
        }
        if (this.attackAge == 2) {
            this.prepareMeteorImpacts(level);
        }
        if (this.attackAge >= 2 && this.attackAge < 32 && this.attackAge % 2 == 0) {
            for (Vec3 impact : this.meteorImpacts) {
                BossFx.lockSigil(level, impact, METEOR_RADIUS,
                        (this.attackAge - 2) / 30.0D,
                        this.attackAge, ParticleTypes.DRAGON_BREATH);
                for (int height = 2; height <= 10; height += 2) {
                    level.sendParticles(height % 4 == 0 ? this.currentSpark() : ParticleTypes.GLOW,
                            impact.x, impact.y + height, impact.z,
                            2, 0.18D, 0.18D, 0.18D, 0.01D);
                }

                if (this.attackAge >= 16) {
                    BossFx.comet(level, impact, 14.0D, (this.attackAge - 16) / 16.0D,
                            ParticleTypes.DRAGON_BREATH);
                }
            }
        }
        if (this.attackAge == 32) {
            this.releaseMeteor(level);
        }
    }

    private void prepareMeteorImpacts(ServerLevel level) {
        this.meteorImpacts.clear();
        for (ServerPlayer player : this.targets(level, 32.0D)) {
            this.meteorImpacts.add(player.position().add(0.0D, 0.08D, 0.0D));
            if (this.meteorImpacts.size() >= 4) {
                break;
            }
        }
        if (this.meteorImpacts.isEmpty() && this.lockedAttackAim != null) {
            this.meteorImpacts.add(this.lockedAttackAim);
        }
        if (this.meteorImpacts.size() == 1) {
            Vec3 center = this.meteorImpacts.get(0);
            double angle = Math.toRadians(this.getYRot());
            this.meteorImpacts.add(center.add(Math.cos(angle) * 4.5D, 0.0D, Math.sin(angle) * 4.5D));
            this.meteorImpacts.add(center.add(Math.cos(angle + Math.PI) * 4.5D,
                    0.0D, Math.sin(angle + Math.PI) * 4.5D));
        }
    }

    private void releaseMeteor(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.8F, 0.6F);
        for (Vec3 impact : this.meteorImpacts) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    impact.x, impact.y + 0.2D, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(this.currentBody(),
                    impact.x, impact.y + 0.4D, impact.z, 48, 1.5D, 1.2D, 1.5D, 0.22D);
            BossFx.impactBurst(level, impact, ParticleTypes.DRAGON_BREATH);
            BossFx.pillar(level, impact, 6.0D, ParticleTypes.DRAGON_BREATH);
        }

        Set<UUID> hitPlayers = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * METEOR_DAMAGE_MULTIPLIER;
        for (ServerPlayer player : this.targets(level, 40.0D)) {
            for (Vec3 impact : this.meteorImpacts) {
                if (player.position().distanceToSqr(impact) > METEOR_RADIUS * METEOR_RADIUS
                        || !hitPlayers.add(player.getUUID())) {
                    continue;
                }
                if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.85D, 0.0D));
                    player.hurtMarked = true;
                }
                break;
            }
        }
    }

    private void tickNova(ServerLevel level) {
        for (int wave = 0; wave < NOVA_RELEASE_TICKS.length; wave++) {
            int releaseTick = NOVA_RELEASE_TICKS[wave];
            ParticleOptions theme = novaParticle(wave);
            if (this.attackAge == releaseTick - 6 || this.attackAge == releaseTick - 2) {
                BossFx.ring(level, this.position().add(0.0D, 0.2D, 0.0D),
                        NOVA_RADII[wave], Math.max(32, (int) (NOVA_RADII[wave] * 5.0D)),
                        0.12D, theme);
            }
            if (this.attackAge == releaseTick) {
                this.releaseNova(level, wave, theme);
            }
        }
    }

    private void releaseNova(ServerLevel level, int wave, ParticleOptions theme) {
        double radius = NOVA_RADII[wave];
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.1F, 0.75F + wave * 0.22F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D), radius, theme);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 5.0D + wave * 1.5D, theme);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.28F + wave * 0.06F);
        for (ServerPlayer player : this.targets(level, radius + 3.0D)) {
            Vec3 horizontal = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (Math.abs(horizontal.length() - radius) > 2.25D
                    || Math.abs(player.getY() - this.getY()) > 4.0D) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = horizontal.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        away.x * (0.55D + wave * 0.15D), 0.22D, away.z * (0.55D + wave * 0.15D)));
                player.hurtMarked = true;
                BossFx.areaBurst(level, player.position(), theme);
            }
        }
    }

    private ParticleOptions novaParticle(int wave) {
        // 三波取当前阶段守护者的三档,由外向内逐波升亮(余烬→主体→火花)。
        // 写死粒子类型会让各阶段的新星长得一样,玩家读不出转相。
        BossParticleTheme.Theme theme = BossParticleTheme.forPhase(this.currentAspectIndex());
        return switch (wave) {
            case 0 -> theme.ember();
            case 1 -> theme.body();
            default -> theme.spark();
        };
    }

    private Vec3 attackOrigin() {
        return this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
    }

    private Vec3 clipRay(ServerLevel level, Vec3 origin, Vec3 requestedEnd) {
        BlockHitResult block = level.clip(new ClipContext(origin, requestedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return block.getType() == HitResult.Type.MISS ? requestedEnd : block.getLocation();
    }

    private Vec3 lockSkyfallImpact(LivingEntity target) {
        Vec3 start = target.position().add(0.0D, Math.max(2.0D, target.getBbHeight()), 0.0D);
        Vec3 fallback = target.position().add(0.0D, 0.05D, 0.0D);
        BlockHitResult ground = this.level().clip(new ClipContext(start, start.add(0.0D, -24.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return ground.getType() == HitResult.Type.BLOCK
                ? ground.getLocation().add(0.0D, 0.05D, 0.0D)
                : fallback;
    }

    private LivingEntity resolveAttackTarget(ServerLevel level) {
        if (this.attackTargetId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.attackTargetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void finishAttack() {
        this.resetFlightState();
        this.attackMode = AttackMode.NONE;
        this.attackAge = 0;
        this.attackDuration = 0;
        this.entityData.set(DATA_ATTACK_MODE, AttackMode.NONE.networkId());
        this.entityData.set(DATA_ATTACK_AGE, 0);
        this.entityData.set(DATA_ATTACK_DURATION, 0);
        this.attackTargetId = null;
        this.lockedAttackAim = null;
        this.attackStartY = 0.0D;
        this.meteorImpacts.clear();
        this.updateBossEventName();
    }

    private void resetFlightState() {
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
    }

    private void updateBossEventName() {
        String key = castTranslationKey(this.attackMode);
        this.bossEvent.setName(key == null
                ? this.getDisplayName()
                : Component.translatable(key, this.getDisplayName()));
    }

    private void releaseLance(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.HOSTILE, 2.0F, 1.4F);
        Vec3 origin = this.attackOrigin();
        LivingEntity target = this.resolveAttackTarget(level);
        Vec3 aim = target == null ? this.lockedAttackAim : target.getEyePosition();
        if (aim == null) {
            return;
        }
        Vec3 hitPoint = this.clipRay(level, origin, aim);
        BossFx.sparkSpray(level, origin, hitPoint.subtract(origin), origin.distanceTo(hitPoint),
                this.currentSpark());
        BossFx.helix(level, origin, hitPoint, 0.4D, ParticleTypes.GLOW);
        if (target instanceof ServerPlayer player && this.isEligibleCombatPlayer(player)
                && origin.distanceToSqr(aim) <= 24.0D * 24.0D
                && hitPoint.distanceToSqr(aim) < 0.25D
                && player.hurt(this.damageSources().mobAttack(this), 14.0F)) {
            BossFx.impactBurst(level, player.position(), ParticleTypes.WAX_OFF);
        } else {
            BossFx.areaBurst(level, hitPoint, this.currentSpark());
        }
    }

    private void releaseSlam(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.6F, 0.5F);
        Vec3 ground = this.position().add(0.0D, 0.15D, 0.0D);
        BossFx.groundShockwave(level, ground, 7.0D, ParticleTypes.WAX_OFF);
        BossFx.groundShockwave(level, ground, 13.0D, this.currentEmber());
        BossFx.impactBurst(level, ground, this.currentSpark());
        BossFx.pillar(level, ground, 8.0D, ParticleTypes.WAX_OFF);
        level.sendParticles(ParticleTypes.CRIT, ground.x, ground.y + 0.4D, ground.z,
                60, 4.5D, 0.4D, 4.5D, 0.35D);
        for (ServerPlayer player : targets(level, 14.0D)) {
            if (!player.hurt(this.damageSources().mobAttack(this), 20.0F)) {
                continue;
            }
            Vec3 away = horizontalAway(player);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 1.1D, 0.6D, away.z * 1.1D));
            player.hurtMarked = true;
        }
    }

    private void releaseBarrage(ServerLevel level, int wave) {
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.0F, 1.0F + wave * 0.25F);
        for (ServerPlayer player : targets(level, 12.0D)) {
            Vec3 toPlayer = player.position().subtract(this.position());
            BossFx.sweepArc(level, this.position(), toPlayer, 10.0D, this.currentSpark());
            BossFx.sweepArc(level, this.position().add(0.0D, 1.1D + wave * 0.4D, 0.0D),
                    toPlayer, 9.0D, this.currentSpark());
            BossFx.areaBurst(level, player.position(), ParticleTypes.WAX_OFF);
            player.hurt(this.damageSources().mobAttack(this), 7.0F + wave * 2.0F);
        }
    }

    private void releaseWind(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.2F, 0.6F);
        ring(level, 5.0D, BossParticleTheme.COLOSSUS.body());
        ring(level, 10.0D, BossParticleTheme.COLOSSUS.body());
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.pillar(level, ground, 9.0D, BossParticleTheme.COLOSSUS.body());
        BossFx.vortex(level, ground, 6.0D, 7.0D, this.tickCount, BossParticleTheme.COLOSSUS.body());
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                ground.x, ground.y + 0.4D, ground.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (ServerPlayer player : targets(level, 14.0D)) {
            double distance = horizontalDistanceFromBoss(player);
            if (distance < WIND_EYE_RADIUS) {
                this.confirmEvade(level, player, BossParticleTheme.COLOSSUS.spark(), false);
                continue;
            }
            boolean attuned = hasAbility(player, EchoAbilityType.WIND_ECHO);
            float damage = attuned ? 9.0F : 15.0F;
            if (!player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            Vec3 away = horizontalAway(player);
            double force = attuned ? 0.45D : 1.35D;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * force, attuned ? 0.12D : 0.45D, away.z * force));
            player.hurtMarked = true;
        }
    }

    private void releaseTide(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                SoundSource.HOSTILE, 2.0F, 0.65F);
        ring(level, 7.0D, BossParticleTheme.ABYSS.body());
        ring(level, 13.0D, ParticleTypes.DOLPHIN);
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.pillar(level, ground, 7.0D, BossParticleTheme.ABYSS.body());

        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0D * i / 20.0D;
            double px = this.getX() + Math.cos(angle) * 9.5D;
            double pz = this.getZ() + Math.sin(angle) * 9.5D;
            level.sendParticles(BossParticleTheme.ABYSS.body(), px, this.getY() + 0.3D, pz,
                    8, 0.25D, 0.9D, 0.25D, 0.12D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.DOLPHIN, px, this.getY() + 0.8D, pz,
                        4, 0.3D, 0.5D, 0.3D, 0.04D);
            }
        }
        for (ServerPlayer player : targets(level, 16.0D)) {
            double distance = horizontalDistanceFromBoss(player);
            if (Math.abs(distance - TIDE_WAVE_RADIUS) > TIDE_WAVE_HALF_WIDTH
                    || Math.abs(player.getY() - this.getY()) > 3.0D) {
                this.confirmEvade(level, player, BossParticleTheme.ABYSS.body(), false);
                continue;
            }
            boolean attuned = hasAbility(player, EchoAbilityType.TIDE_ECHO);
            boolean hurt = player.hurt(this.damageSources().mobAttack(this), attuned ? 10.0F : 17.0F);
            if (hurt && !attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            }
        }
    }

    private void releaseTrueSight(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 2.2F, 1.6F);
        ring(level, 6.0D, BossParticleTheme.MIRROR.body());
        level.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + 2.5D, this.getZ(),
                2, 0.0D, 0.0D, 0.0D, 0.0D);

        Vec3 eye = this.attackOrigin();
        double baseAngle = this.trueSightBaseAngle();
        for (int i = 0; i < TRUE_SIGHT_RAY_COUNT; i++) {
            double angle = baseAngle + Math.PI * 2.0D * i / TRUE_SIGHT_RAY_COUNT;
            Vec3 dir = new Vec3(Math.cos(angle), -0.12D, Math.sin(angle));
            BossFx.sparkSpray(level, eye, dir, 9.0D, BossParticleTheme.MIRROR.spark());
        }
        for (ServerPlayer player : targets(level, 12.0D)) {
            if (!this.isInsideTrueSightRay(player)) {
                this.confirmEvade(level, player, BossParticleTheme.MIRROR.spark(), false);
                continue;
            }
            boolean attuned = hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);
            boolean hurt = player.hurt(this.damageSources().mobAttack(this), attuned ? 11.0F : 19.0F);
            if (hurt && !attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            }
        }
    }

    private void releaseGravity(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.55F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                12.0D, BossParticleTheme.VOID_PROPHET.ember());

        BossFx.converge(level, this.attackOrigin(), 9.0D, 24, BossParticleTheme.VOID_PROPHET.body());
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 10.0D, BossParticleTheme.VOID_PROPHET.body());
        level.sendParticles(BossParticleTheme.VOID_PROPHET.spark(),
                this.getX(), this.getY() + 0.6D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (ServerPlayer player : targets(level, 15.0D)) {
            if (!player.onGround() && player.getY() - this.getY() >= GRAVITY_JUMP_HEIGHT) {
                this.confirmEvade(level, player, BossParticleTheme.VOID_PROPHET.spark(), false);
                continue;
            }
            boolean attuned = hasAbility(player, EchoAbilityType.GRAVITY_ECHO);
            if (!player.hurt(this.damageSources().mobAttack(this), attuned ? 13.0F : 23.0F)) {
                continue;
            }
            Vec3 away = horizontalAway(player);
            double force = attuned ? 0.3D : 0.85D;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * force, attuned ? 0.18D : 0.8D, away.z * force));
            player.hurtMarked = true;
        }
    }

    private void releaseVoid(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.4F, 0.8F);
        for (double radius : VOID_RING_RADII) {
            ring(level, radius, BossParticleTheme.RIFT.body());
        }

        Vec3 core = this.attackOrigin();
        level.sendParticles(BossParticleTheme.RIFT.body(), core.x, core.y, core.z,
                40, 2.2D, 1.8D, 2.2D, 0.06D);
        level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z,
                3, 0.0D, 0.0D, 0.0D, 0.0D);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                17.0D, BossParticleTheme.RIFT.ember());
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 11.0D, BossParticleTheme.RIFT.body());
        for (ServerPlayer player : targets(level, 22.0D)) {
            if (!this.isOnVoidRing(player)) {
                this.confirmEvade(level, player, BossParticleTheme.RIFT.body(), false);
                continue;
            }
            boolean attuned = hasAbility(player, EchoAbilityType.VOID_ECHO);
            boolean hurt = player.hurt(this.damageSources().mobAttack(this), attuned ? 15.0F : 28.0F);
            if (hurt && !attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
        }
    }

    private void renderTrueSightRayTelegraph(ServerLevel level) {
        Vec3 origin = this.attackOrigin();
        double baseAngle = this.trueSightBaseAngle();
        for (int ray = 0; ray < TRUE_SIGHT_RAY_COUNT; ray++) {
            double angle = baseAngle + Math.PI * 2.0D * ray / TRUE_SIGHT_RAY_COUNT;
            for (double distance = 2.0D; distance <= 11.0D; distance += 1.35D) {
                Vec3 point = origin.add(Math.cos(angle) * distance,
                        -0.08D * distance, Math.sin(angle) * distance);
                level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z,
                        1, 0.035D, 0.035D, 0.035D, 0.0D);
            }
        }
    }

    private double trueSightBaseAngle() {
        return Math.toRadians(this.getYRot() + 90.0F);
    }

    private boolean isInsideTrueSightRay(ServerPlayer player) {
        Vec3 offset = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        double distance = offset.length();
        if (distance < 2.0D || distance > 12.0D) {
            return false;
        }
        double baseAngle = this.trueSightBaseAngle();
        for (int ray = 0; ray < TRUE_SIGHT_RAY_COUNT; ray++) {
            double angle = baseAngle + Math.PI * 2.0D * ray / TRUE_SIGHT_RAY_COUNT;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            double along = offset.dot(direction);
            double lateral = Math.abs(offset.x * direction.z - offset.z * direction.x);
            if (along >= 0.0D && lateral <= TRUE_SIGHT_RAY_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private void renderVoidRingTelegraph(ServerLevel level, Vec3 ground) {
        for (double radius : VOID_RING_RADII) {
            BossFx.ring(level, ground, radius,
                    Math.max(28, (int) (radius * 4.0D)), 0.12D, ParticleTypes.REVERSE_PORTAL);
        }
    }

    private boolean isOnVoidRing(ServerPlayer player) {
        if (Math.abs(player.getY() - this.getY()) > 3.0D) {
            return false;
        }
        double distance = horizontalDistanceFromBoss(player);
        for (double radius : VOID_RING_RADII) {
            if (Math.abs(distance - radius) <= VOID_RING_HALF_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private double horizontalDistanceFromBoss(ServerPlayer player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void announceAttack(AttackMode mode) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        String key = switch (mode) {
            case WIND -> "message.unknown_echoes.world_echo.telegraph.wind";
            case TIDE -> "message.unknown_echoes.world_echo.telegraph.tide";
            case TRUE_SIGHT -> "message.unknown_echoes.world_echo.telegraph.true_sight";
            case GRAVITY -> "message.unknown_echoes.world_echo.telegraph.gravity";
            case VOID -> "message.unknown_echoes.world_echo.telegraph.void";
            case SKYFALL -> "message.unknown_echoes.world_echo.telegraph.skyfall";
            case WING_STORM -> "message.unknown_echoes.world_echo.telegraph.wing_storm";
            case AERIAL_BARRAGE -> "message.unknown_echoes.world_echo.telegraph.aerial_barrage";
            case AERIAL_REAP -> "message.unknown_echoes.world_echo.telegraph.aerial_reap";
            case PHASE -> "message.unknown_echoes.world_echo.telegraph.phase";
            default -> null;
        };
        if (key == null) {
            return;
        }
        Component message = mode == AttackMode.PHASE
                ? Component.translatable(key,
                        Component.translatable(aspectTranslationKey(this.nextAspectIndex())))
                : Component.translatable(key);
        float pitch = 0.8F + Math.min(5, mode.ordinal()) * 0.1F;
        for (ServerPlayer player : this.combatAudience(level)) {
            player.displayClientMessage(message, true);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 0.75F, pitch);
        }
    }

    private void confirmEvade(ServerLevel level, ServerPlayer player,
                              ParticleOptions theme, boolean phaseShift) {
        level.sendParticles(theme, player.getX(), player.getY() + 0.8D, player.getZ(),
                phaseShift ? 14 : 8, 0.35D, 0.55D, 0.35D, 0.025D);
        level.sendParticles(ParticleTypes.GLOW,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                phaseShift ? 10 : 5, 0.3D, 0.5D, 0.3D, 0.02D);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, phaseShift ? 0.9F : 0.55F, phaseShift ? 1.85F : 1.65F);
    }

    private void tickPhaseTransition(ServerLevel level) {
        int nextAspect = this.nextAspectIndex();
        ParticleOptions theme = aspectParticle(nextAspect);
        Vec3 pylonTop = this.resonancePylonTop(level, nextAspect);
        if (pylonTop != null && this.attackAge % 3 == 0) {
            BossFx.guidanceFlow(level, pylonTop, this.attackOrigin(),
                    ParticleTypes.GLOW, theme, this.attackAge);
            this.renderPhaseSafeLane(level, pylonTop, theme);
        }
        if (pylonTop != null && (this.attackAge == 6 || this.attackAge == 16
                || this.attackAge == 22)) {
            double radius = 2.2D + this.attackAge * 0.12D;
            BossFx.ring(level, pylonTop.add(0.0D, -0.4D, 0.0D),
                    radius, 36, 0.0D, theme);
            level.playSound(null, BlockPos.containing(pylonTop), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 2.2F, 0.72F + nextAspect * 0.1F);
        }
        if (this.attackAge == 24) {
            this.releasePhase(level);
        }
    }

    private void releasePhase(ServerLevel level) {
        int nextAspect = this.nextAspectIndex();
        ParticleOptions theme = aspectParticle(nextAspect);
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.4F, 0.55F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                10.0D, theme);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 12.0D, theme);
        BossFx.converge(level, this.attackOrigin(), 8.0D, 20, theme);
        Vec3 pylonTop = this.resonancePylonTop(level, nextAspect);
        if (pylonTop != null) {
            BossFx.pillar(level, pylonTop.add(0.0D, -8.0D, 0.0D), 12.0D, theme);
            BossFx.helix(level, pylonTop, this.attackOrigin(), 0.45D, theme);
            if (this.altarPos != null) {
                Vec3 arenaCenter = new Vec3(this.altarPos.getX() + 0.5D,
                        this.altarPos.getY() + 0.15D, this.altarPos.getZ() + 0.5D);
                BossFx.groundShockwave(level, arenaCenter,
                        Math.max(14.0D, this.arenaRadius() - 3.0D), theme);
            }
        }
        List<ServerPlayer> phaseTargets = pylonTop == null
                ? targets(level, 12.0D)
                : this.combatAudience(level);
        float damage = 10.0F + nextAspect * 2.0F;
        for (ServerPlayer player : phaseTargets) {
            if (pylonTop != null && this.isWithinPhaseSafeLane(player, pylonTop)) {
                this.confirmEvade(level, player, theme, true);
                continue;
            }
            if (!player.hurt(this.damageSources().mobAttack(this), damage)) {
                continue;
            }
            Vec3 away = horizontalAway(player);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 0.65D, 0.35D, away.z * 0.65D));
            player.hurtMarked = true;
        }
    }

    private void renderPhaseSafeLane(ServerLevel level, Vec3 pylonTop, ParticleOptions theme) {
        if (this.altarPos == null) {
            return;
        }
        Vec3 start = new Vec3(this.altarPos.getX() + 0.5D, this.altarPos.getY() + 0.15D,
                this.altarPos.getZ() + 0.5D);
        Vec3 delta = new Vec3(pylonTop.x - start.x, 0.0D, pylonTop.z - start.z);
        double length = delta.length();
        if (length < 1.0D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        for (double distance = 3.0D; distance <= length + 2.0D; distance += 1.25D) {
            Vec3 center = start.add(direction.scale(distance));
            Vec3 left = center.add(side.scale(PHASE_SAFE_LANE_HALF_WIDTH));
            Vec3 right = center.add(side.scale(-PHASE_SAFE_LANE_HALF_WIDTH));
            level.sendParticles(theme, center.x, center.y, center.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
            level.sendParticles(ParticleTypes.GLOW, left.x, left.y, left.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
            level.sendParticles(ParticleTypes.GLOW, right.x, right.y, right.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private boolean isWithinPhaseSafeLane(ServerPlayer player, Vec3 pylonTop) {
        if (this.altarPos == null) {
            return false;
        }
        Vec3 start = new Vec3(this.altarPos.getX() + 0.5D, 0.0D,
                this.altarPos.getZ() + 0.5D);
        Vec3 delta = new Vec3(pylonTop.x - start.x, 0.0D, pylonTop.z - start.z);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0D) {
            return false;
        }
        Vec3 offset = new Vec3(player.getX() - start.x, 0.0D, player.getZ() - start.z);
        double along = offset.dot(delta) / lengthSqr;
        if (along < 0.12D || along > 1.12D) {
            return false;
        }
        Vec3 nearest = delta.scale(along);
        return offset.distanceToSqr(nearest)
                <= PHASE_SAFE_LANE_HALF_WIDTH * PHASE_SAFE_LANE_HALF_WIDTH;
    }

    private void completePhaseTransition(ServerLevel level) {
        this.phaseTransitionActive = false;
        this.phaseGateStage = Math.min(PHASE_FLOORS.length, this.phaseGateStage + 1);
        this.attackCycle = 0;
        this.attackCooldown = 30;
        level.playSound(null, this.blockPosition(), ModSounds.WORLD_ECHO_PHASE.get(),
                SoundSource.HOSTILE, 2.2F, 1.0F);
        this.broadcastNearby(Component.translatable("message.unknown_echoes.world_echo.phase_open",
                Component.translatable(aspectTranslationKey(this.currentAspectIndex()))));
    }

    public void onPhasePylonActivated(ServerLevel level, BlockPos pylonPos) {
        if (!this.phaseTransitionPending || !this.isAlive() || this.altarPos == null) {
            return;
        }
        if (!this.altarPos.equals(pylonPos)) {
            return;
        }
        long now = level.getGameTime();
        if (now < this.phaseMechanismCooldownUntil) {
            return;
        }
        int index = this.activatedPhasePylons.size();
        if (index < 5) {
            this.activatedPhasePylons.add(index);
        }
        int required = Math.min(5, PHASE_FLOORS.length + 1);
        if (this.activatedPhasePylons.size() >= required) {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) target = this.findNearestPlayer(level);
            if (target != null) {
                this.beginAttack(AttackMode.PHASE, 48, "phase", target);
                this.phaseTransitionPending = false;
                this.phaseTransitionActive = true;
                this.activatedPhasePylons.clear();
                this.phaseMechanismCooldownUntil = 0L;
                this.broadcastNearby(Component.translatable(
                        "message.unknown_echoes.world_echo.phase_locked"));
                this.announceAttack(AttackMode.PHASE);
            }
        }
        this.phaseMechanismCooldownUntil = now + PHASE_MECHANISM_COOLDOWN_TICKS;
    }

    private void drawPhaseMechanismGuidance(ServerLevel level) {
        if (this.altarPos == null || !(level.getBlockEntity(this.altarPos)
                instanceof WorldResonanceAltarBlockEntity altar)) {
            return;
        }
        Vec3 altarCore = Vec3.atCenterOf(this.altarPos).add(0.0D, 0.65D, 0.0D);
        Vec3 bossCore = this.attackOrigin();
        BossFx.guidanceMarker(level, altarCore, this.currentEmber());
        BossFx.guidanceFlow(level, bossCore, altarCore,
                ParticleTypes.ENCHANT, this.currentEmber(), this.tickCount);
        List<Vec3> pylons = altar.resonancePylonTops();
        for (int index = 0; index < pylons.size(); index++) {
            if (this.activatedPhasePylons.contains(index)) {
                continue;
            }
            Vec3 pylon = pylons.get(index);
            BossFx.guidanceMarker(level, pylon, this.currentEmber());
            BossFx.guidanceFlow(level, altarCore, pylon,
                    ParticleTypes.GLOW, this.currentEmber(), this.tickCount + index * 7L);
        }
    }

    private void ring(ServerLevel level, double radius, ParticleOptions particle) {
        BossFx.ring(level, this.position().add(0.0D, 0.2D, 0.0D),
                radius, Math.max(32, (int) (radius * 5.0D)), 0.12D, particle);
    }

    private List<ServerPlayer> targets(ServerLevel level, double radius) {
        return level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                player -> this.isEligibleCombatPlayer(player)
                        && player.distanceToSqr(this) <= radius * radius);
    }

    private List<ServerPlayer> combatAudience(ServerLevel level) {
        if (this.combatAudienceCacheTick == this.tickCount) {
            return this.combatAudienceCache;
        }
        this.combatAudienceCacheTick = this.tickCount;
        this.combatAudienceCache = level.getEntitiesOfClass(ServerPlayer.class,
                this.combatAudienceBounds(), this::isCombatAudiencePlayer);
        return this.combatAudienceCache;
    }

    private AABB combatAudienceBounds() {
        if (this.altarPos != null && this.altarArenaId != null) {
            double radius = this.arenaRadius() + 2.0D;
            return new AABB(this.altarPos).inflate(radius, 40.0D, radius);
        }
        return this.getBoundingBox().inflate(32.0D);
    }

    private boolean isCombatAudiencePlayer(ServerPlayer player) {
        if (player.level() != this.level() || !this.isEligibleCombatPlayer(player)) {
            return false;
        }
        if (this.altarPos != null && this.altarArenaId != null) {
            return this.isInsideBoundArena(player, 2.0D);
        }
        return player.distanceToSqr(this) <= square(32.0D);
    }

    private void syncBossEventViewers(ServerLevel level) {
        for (ServerPlayer viewer : List.copyOf(this.bossEvent.getPlayers())) {
            if (!this.isCombatAudiencePlayer(viewer)) {
                this.bossEvent.removePlayer(viewer);
            }
        }
        for (ServerPlayer player : this.combatAudience(level)) {
            this.bossEvent.addPlayer(player);
        }
    }

    private boolean tickEncounterContinuity(ServerLevel level) {
        AltarBindingState bindingState = this.altarBindingState(level);
        if (bindingState == AltarBindingState.UNAVAILABLE) {
            this.pauseForUnavailableAltar();
            return true;
        }
        if (bindingState == AltarBindingState.INVALID) {
            this.discard();
            return true;
        }
        if (bindingState != AltarBindingState.VALID) {
            return false;
        }
        List<ServerPlayer> audience = this.combatAudience(level);
        if (!audience.isEmpty()) {
            int recoveredTicks = this.arenaEmptyTicks;
            this.arenaEmptyTicks = 0;
            for (ServerPlayer player : audience) {
                this.registerParticipant(player);
        }
        if (this.encounterDormant) {
            this.wakeDormantEncounter(level, audience);
            return true;
        }
            if (recoveredTicks >= ARENA_RECOVERY_INTERVAL_TICKS) {
                for (ServerPlayer player : audience) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.world_echo.recovered"), true);
                }
            }
            return false;
        }

        this.setTarget(null);
        this.getNavigation().stop();
        if (this.encounterDormant) {
            return true;
        }
        this.arenaEmptyTicks = Math.min(ARENA_RESET_GRACE_TICKS, this.arenaEmptyTicks + 1);
        if (this.phaseTransitionActive) {
            this.phaseTransitionActive = false;
            this.phaseTransitionPending = true;
            this.finishAttack();
        }
        if (this.arenaEmptyTicks < ARENA_RESET_GRACE_TICKS
                && this.arenaEmptyTicks % ARENA_RECOVERY_INTERVAL_TICKS == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * ARENA_RECOVERY_FRACTION);
            BossFx.ring(level, this.attackOrigin(), 4.0D + this.arenaEmptyTicks / 20.0D,
                    44, 0.08D, aspectParticle(this.currentAspectIndex()));
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 1.4F, 0.65F);
        }
        if (this.arenaEmptyTicks >= ARENA_RESET_GRACE_TICKS) {
            this.resetDormantEncounter(level);
            return true;
        }
        return this.attackMode == AttackMode.NONE;
    }

    private void resetDormantEncounter(ServerLevel level) {
        Set<UUID> previousParticipants = Set.copyOf(this.participants);
        Vec3 collapseOrigin = this.attackOrigin();
        for (int aspect = 0; aspect < 5; aspect++) {
            Vec3 pylonTop = this.resonancePylonTop(level, aspect);
            if (pylonTop != null) {
                BossFx.guidanceFlow(level, collapseOrigin, pylonTop,
                        ParticleTypes.REVERSE_PORTAL, aspectParticle(aspect), this.tickCount + aspect * 7);
            }
        }
        BossFx.ring(level, collapseOrigin, Math.min(18.0D, this.arenaRadius() - 2.0D),
                80, 0.16D, ParticleTypes.REVERSE_PORTAL);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                collapseOrigin.x, collapseOrigin.y, collapseOrigin.z,
                180, 2.5D, 3.0D, 2.5D, 0.28D);
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.8F, 0.52F);

        this.phaseGateStage = 0;
        this.phaseTransitionPending = false;
        this.phaseTransitionActive = false;
        this.attackCycle = 0;
        this.attackCooldown = 50;
        this.arenaEmptyTicks = 0;
        this.encounterDormant = true;
        this.entranceTicksRemaining = 0;
        this.entranceFxPlayed = true;
        this.protectedMessageCooldown = 0;
        this.finishAttack();
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setAggressive(false);
        this.removeAllEffects();
        this.participants.clear();
        this.scaledParticipants = 1;
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(BASE_MAX_HEALTH * ServerConfig.WORLD_ECHO_HEALTH_MULTIPLIER.get());
            maxHealth.setBaseValue(maxHealth.getBaseValue()
                    * ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get());
        }
        this.setHealth(this.getMaxHealth());
        this.bossEvent.setProgress(1.0F);

        Vec3 returnPoint = this.findArenaReturnPoint(level);
        if (returnPoint != null) {
            this.teleportTo(returnPoint.x, returnPoint.y, returnPoint.z);
            level.sendParticles(this.currentBody(),
                    returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                    80, 1.4D, 2.0D, 1.4D, 0.12D);
        }
        for (UUID participantId : previousParticipants) {
            var participant = level.getPlayerByUUID(participantId);
            if (participant instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_echo.reset"), true);
            }
        }
    }

    private void wakeDormantEncounter(ServerLevel level, List<ServerPlayer> audience) {
        this.encounterDormant = false;
        this.arenaEmptyTicks = 0;
        this.entranceTicksRemaining = ENTRANCE_DURATION_TICKS;
        this.entranceFxPlayed = true;
        this.attackCooldown = 50;
        this.setTarget(null);
        this.getNavigation().stop();
        this.playEntranceFx(level);
        for (ServerPlayer player : audience) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_echo.reawakened"), true);
        }
    }

    private static boolean isCombatPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().invulnerable;
    }

    private boolean isEligibleCombatPlayer(ServerPlayer player) {
        if (!isCombatPlayer(player)) {
            return false;
        }
        if (this.altarPos == null || this.altarArenaId == null) {
            return true;
        }
        return (EchoPermission.canSummonWorldEcho(player) || this.isAdminTestCombatPlayer())
                && this.isInsideBoundArena(player, 2.0D);
    }

    private boolean isAdminTestCombatPlayer() {
        return this.level() instanceof ServerLevel level
                && this.altarPos != null
                && level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar
                && this.altarArenaId.equals(altar.getArenaId())
                && this.altarPos.equals(altar.getBlockPos())
                && altar.isAdminTestMode();
    }

    private boolean isInsideBoundArena(ServerPlayer player, double padding) {
        return this.altarPos != null
                && this.horizontalDistanceSqr(player.position()) <= square(this.arenaRadius() + padding)
                && Math.abs(player.getY() - this.altarPos.getY()) <= 40.0D;
    }

    private void enforceArenaBoundary(ServerLevel level) {
        if (this.altarPos == null || this.altarArenaId == null) {
            return;
        }
        if (this.getTarget() instanceof ServerPlayer player && !this.isEligibleCombatPlayer(player)) {
            this.setTarget(null);
        }
        double radius = this.arenaRadius();
        if (this.tickCount % 100 == 0) {
            this.restrictTo(this.altarPos, (int) Math.ceil(radius));
        }
        if (this.tickCount % 5 != 0
                || (this.horizontalDistanceSqr(this.position()) <= square(radius + 4.0D)
                && Math.abs(this.getY() - this.altarPos.getY()) <= 24.0D)) {
            return;
        }
        Vec3 returnPoint = this.findArenaReturnPoint(level);
        if (returnPoint == null) {
            return;
        }
        this.teleportTo(returnPoint.x, returnPoint.y, returnPoint.z);
        this.getNavigation().stop();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                90, 2.0D, 2.5D, 2.0D, 0.2D);
    }

    @javax.annotation.Nullable
    private Vec3 findArenaReturnPoint(ServerLevel level) {
        if (this.altarPos == null || this.altarArenaId == null
                || !(level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar)
                || !this.matchesAltar(altar.getArenaId(), this.altarPos)) {
            return null;
        }
        BlockPos returnPos = WorldResonanceAltarBlock.findSpawnPos(
                level, this.altarPos, altar.isGrandArena());
        if (returnPos == null) {
            return null;
        }
        Vec3 target = Vec3.atBottomCenterOf(returnPos);
        AABB movedBounds = this.getBoundingBox().move(target.subtract(this.position()));
        return level.getWorldBorder().isWithinBounds(movedBounds)
                && level.noCollision(this, movedBounds) ? target : null;
    }

    private void tickArenaResonanceFx(ServerLevel level) {
        if (this.attackMode == AttackMode.PHASE || this.tickCount % 5 != 0) {
            return;
        }
        int aspect = this.currentAspectIndex();
        Vec3 pylonTop = this.resonancePylonTop(level, aspect);
        if (pylonTop == null) {
            return;
        }
        ParticleOptions theme = aspectParticle(aspect);
        BossFx.guidanceFlow(level, pylonTop, this.attackOrigin(),
                ParticleTypes.GLOW, theme, this.tickCount);
        if (this.tickCount % 20 == 0) {
            BossFx.ring(level, pylonTop.add(0.0D, -0.35D, 0.0D),
                    2.4D, 28, 0.0D, theme);
        }
    }

    private Vec3 resonancePylonTop(ServerLevel level, int aspect) {
        if (this.altarPos == null || this.altarArenaId == null
                || !(level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar)
                || !this.matchesAltar(altar.getArenaId(), this.altarPos)) {
            return null;
        }
        List<Vec3> pylons = altar.resonancePylonTops();
        return aspect >= 0 && aspect < pylons.size() ? pylons.get(aspect) : null;
    }

    private double arenaRadius() {
        if (this.altarPos != null
                && this.level().getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar
                && this.matchesAltar(altar.getArenaId(), this.altarPos)) {
            return WorldResonanceLayout.arenaRadius(altar.isGrandArena());
        }
        return WorldResonanceLayout.arenaRadius(false);
    }

    private double horizontalDistanceSqr(Vec3 position) {
        if (this.altarPos == null) {
            return 0.0D;
        }
        double dx = position.x - (this.altarPos.getX() + 0.5D);
        double dz = position.z - (this.altarPos.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double square(double value) {
        return value * value;
    }

    private boolean hasValidAltarBinding(ServerLevel level) {
        return this.altarBindingState(level) == AltarBindingState.VALID;
    }

    private AltarBindingState altarBindingState(ServerLevel level) {
        if (this.altarArenaId == null || this.altarPos == null) {
            return AltarBindingState.UNBOUND;
        }
        if (!level.hasChunkAt(this.altarPos)) {
            return AltarBindingState.UNAVAILABLE;
        }
        if (!(level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar)) {
            return level.getBlockState(this.altarPos).getBlock() instanceof WorldResonanceAltarBlock
                    ? AltarBindingState.UNAVAILABLE
                    : AltarBindingState.INVALID;
        }
        return this.matchesAltar(altar.getArenaId(), this.altarPos)
                && altar.ownsActiveBoss(this.altarArenaId, this.getUUID())
                ? AltarBindingState.VALID
                : AltarBindingState.INVALID;
    }

    private boolean isAltarCombatBlocked() {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        AltarBindingState state = this.altarBindingState(level);
        return state != AltarBindingState.UNBOUND && state != AltarBindingState.VALID;
    }

    private void pauseForUnavailableAltar() {
        this.setTarget(null);
        this.getNavigation().stop();
        if (this.phaseTransitionActive) {
            this.phaseTransitionActive = false;
            this.phaseTransitionPending = true;
        }
        if (this.attackMode != AttackMode.NONE) {
            this.finishAttack();
        } else {
            this.resetFlightState();
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.encounterDormant || this.entranceTicksRemaining > 0
                || this.isAltarCombatBlocked()) {
            return false;
        }
        if (target instanceof ServerPlayer player
                && this.altarPos != null && this.altarArenaId != null
                && !this.isEligibleCombatPlayer(player)) {
            return false;
        }
        return super.canAttack(target);
    }

    private Vec3 horizontalAway(ServerPlayer player) {
        Vec3 away = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        return away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
    }

    private static boolean hasAbility(ServerPlayer player, EchoAbilityType ability) {
        return EchoAbilityManager.hasAbility(player, ability);
    }

    private AttackMode nextAttackMode() {
        AttackMode[] deck = PHASE_ATTACK_DECKS[this.currentAspectIndex()];
        return deck[Math.floorMod(this.attackCycle++, deck.length)];
    }

    private int specialCooldown() {
        return PHASE_COOLDOWNS[this.currentAspectIndex()];
    }

    private int currentAspectIndex() {
        return Math.min(PHASE_ATTACK_DECKS.length - 1, Math.max(0, this.phaseGateStage));
    }

    private int nextAspectIndex() {
        return Math.min(PHASE_ATTACK_DECKS.length - 1, this.currentAspectIndex() + 1);
    }

    private static ParticleOptions aspectParticle(int aspect) {
        // 直接取那位守护者的 body 档,不要另定一套:不一致会让玩家看不出这一阶段在模仿谁。
        return BossParticleTheme.forPhase(aspect).body();
    }

    private static ParticleOptions aspectSpark(int aspect) {
        return BossParticleTheme.forPhase(aspect).spark();
    }

    private static ParticleOptions aspectEmber(int aspect) {
        return BossParticleTheme.forPhase(aspect).ember();
    }

    /** 当前阶段的粒子;取别的阶段(如转相预告用下一相)走 aspectParticle 系列。 */
    private ParticleOptions currentBody() {
        return BossParticleTheme.forPhase(this.currentAspectIndex()).body();
    }

    private ParticleOptions currentSpark() {
        return BossParticleTheme.forPhase(this.currentAspectIndex()).spark();
    }

    private ParticleOptions currentEmber() {
        return BossParticleTheme.forPhase(this.currentAspectIndex()).ember();
    }

    private static String aspectTranslationKey(int aspect) {
        return switch (aspect) {
            case 0 -> "gui.unknown_echoes.world_echo.phase.wind";
            case 1 -> "gui.unknown_echoes.world_echo.phase.tide";
            case 2 -> "gui.unknown_echoes.world_echo.phase.true_sight";
            case 3 -> "gui.unknown_echoes.world_echo.phase.gravity";
            default -> "gui.unknown_echoes.world_echo.phase.void";
        };
    }

    private void registerNearbyParticipants(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : this.targets(level, radius)) {
            this.registerParticipant(player);
        }
    }

    private void updateDynamicScaling(ServerLevel level) {
        int count = Math.max(1, this.participants.size());
        if (count <= this.scaledParticipants) {
            return;
        }
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        int addedParticipants = count - this.scaledParticipants;
        // 必须同时吃深层与自身两档倍率(DEEP × WORLD_ECHO),与 CommonEvents 的入场缩放同口径;
        // 只乘一档会让这只 Boss 的血量与入场缩放对不上。
        double multiplier = ServerConfig.WORLD_ECHO_HEALTH_MULTIPLIER.get()
                * ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get();
        maxHealth.setBaseValue((BASE_MAX_HEALTH + HEALTH_PER_EXTRA_PLAYER * (count - 1)) * multiplier);
        this.heal((float) (HEALTH_PER_EXTRA_PLAYER * addedParticipants * multiplier));
        this.scaledParticipants = count;
        Vec3 core = this.attackOrigin();
        BossFx.ring(level, core, Math.min(12.0D, 4.0D + count),
                56, 0.12D, this.currentEmber());
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 1.8F, Math.max(0.55F, 1.05F - count * 0.035F));
        for (ServerPlayer player : this.combatAudience(level)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_echo.scaled", count), true);
        }
    }

    private LivingEntity findNearestPlayer(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : this.targets(level, radius)) {
            double distance = player.distanceToSqr(this);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private float currentMechanicFloor() {
        return this.phaseGateStage < PHASE_FLOORS.length
                ? this.getMaxHealth() * PHASE_FLOORS[this.phaseGateStage]
                : 0.0F;
    }

    @Override
    public float phaseDamageFloor() {
        return this.currentMechanicFloor();
    }

    @Override
    public void onPhaseDamageBlocked(DamageSource source) {
        if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_echo.guard_holds"), true);
            this.protectedMessageCooldown = 40;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (!this.level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isAltarCombatBlocked()) {
            return false;
        }
        if (!this.level().isClientSide && this.encounterDormant
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.protectedMessageCooldown <= 0
                    && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_echo.dormant_guard"), true);
                this.protectedMessageCooldown = 40;
            }
            return false;
        }
        if (!this.level().isClientSide && this.entranceTicksRemaining > 0
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.protectedMessageCooldown <= 0
                    && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_echo.manifesting_guard"), true);
                this.protectedMessageCooldown = 40;
            }
            return false;
        }
        if (!this.level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.altarPos != null && this.altarArenaId != null
                && (!(source.getEntity() instanceof ServerPlayer attacker)
                || !this.isEligibleCombatPlayer(attacker))) {
            if (source.getEntity() instanceof ServerPlayer attacker
                    && this.protectedMessageCooldown <= 0) {
                attacker.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_echo.arena_reject"), true);
                this.protectedMessageCooldown = 40;
            }
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
        if (!this.level().isClientSide && hurt && this.phaseGateStage < PHASE_FLOORS.length
                && BossPhaseGate.reachedFloor(this, floor)) {
            this.phaseTransitionPending = true;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.encounterDormant || this.entranceTicksRemaining > 0
                || this.attackMode != AttackMode.NONE || this.isAltarCombatBlocked()) {
            return false;
        }
        if (target instanceof ServerPlayer player
                && this.altarPos != null && this.altarArenaId != null
                && !this.isEligibleCombatPlayer(player)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && this.level() instanceof ServerLevel level) {
            this.attackCooldown = Math.max(this.attackCooldown, 10);
            this.triggerAnim("attack_controller", "melee");
            Vec3 impact = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT,
                    impact.x, impact.y, impact.z, 14, 0.45D, 0.55D, 0.45D, 0.12D);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.HOSTILE, 1.5F, 0.65F);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (wasDead || !this.dead) {
            return;
        }
        this.resetFlightState();
        this.triggerAnim("attack_controller", "death");
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        boolean validSettlement = this.hasValidAltarBinding(level);
        if (validSettlement) {
            boolean hasEligibleParticipant = false;
            boolean firstKillDrop = false;
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                    this.combatAudienceBounds(), player -> !player.isSpectator())) {
                if (this.participants.contains(player.getUUID())
                        && this.isInsideBoundArena(player, 2.0D)
                        && EchoPermission.canSummonWorldEcho(player)) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(
                            player, EchoPermission.WORLD_ECHO_ID);
                    hasEligibleParticipant = true;
                    if (!EchoAbilityManager.markBossDefeatedWithMechanisms(
                            player, EchoPermission.WORLD_ECHO_ID,
                            EchoPermission.WORLD_ECHO_REWARD_READY_KEY)) {
                        player.sendSystemMessage(Component.translatable(
                                "message.unknown_echoes.progress_record.full"));
                        continue;
                    }
                    firstKillDrop |= firstKill;
                }
            }
            if (hasEligibleParticipant) {
                grantSharedCombatRewards(firstKillDrop);
            }
        }
        this.bossEvent.setProgress(0.0F);
        this.bossEvent.setName(Component.translatable(
                "gui.unknown_echoes.world_echo.cast.silencing", this.getDisplayName()));
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                core.x, core.y, core.z, 64, 1.8D, 2.4D, 1.8D, 0.18D);
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.6F, 0.55F);
        this.notifyAltarSettled(level);
    }

    private void grantSharedCombatRewards(boolean firstKill) {
        ItemStack relic = new ItemStack(switch (this.random.nextInt(5)) {
            case 0 -> ModItems.WORLD_ECHO_WINGS.get();
            case 1 -> ModItems.RIFTWING_RELAY.get();
            case 2 -> ModItems.PERPETUAL_ECHO_CORE.get();
            case 3 -> ModItems.WORLD_RIFT_GREATSWORD.get();
            default -> ModItems.FIVEFOLD_STAR_SPEAR.get();
        });
        ItemStack weapon = new ItemStack(switch (this.random.nextInt(4)) {
            case 0 -> ModItems.GRAVITY_HAMMER.get();
            case 1 -> ModItems.RIFT_DAGGER.get();
            case 2 -> ModItems.ANCHOR_BLADE.get();
            default -> ModItems.FOLDED_GLAIVE.get();
        });
        BossMaterialRewards.drop(this, relic);
        BossMaterialRewards.drop(this, weapon);
        BossMaterialRewards.drop(this, new ItemStack(ModItems.WORLD_ECHO_RESIDUE.get(),
                firstKill ? 4 : 2));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.DEEP_ALLOY_INGOT.get(),
                6 + this.random.nextInt(5)));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.WORLD_ECHO_RESIDUE.get(),
                5 + this.random.nextInt(4)));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.VOID_RIFT_SHARD.get(),
                3 + this.random.nextInt(4)));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.GRAVITY_CORE_FRAGMENT.get(),
                2 + this.random.nextInt(4)));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_ARMOR_PLATE.get(),
                2 + this.random.nextInt(3)));
        BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_WEAPON_FRAME.get(),
                1 + this.random.nextInt(2)));
        BossMaterialRewards.drop(this, new ItemStack(Items.DIAMOND,
                4 + this.random.nextInt(5)));
        if (this.random.nextFloat() < 0.45F) {
            BossMaterialRewards.drop(this, new ItemStack(Items.NETHERITE_INGOT,
                    1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.30F) {
            BossMaterialRewards.drop(this, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (this.random.nextFloat() < 0.35F) {
            BossMaterialRewards.drop(this, new ItemStack(this.random.nextBoolean()
                    ? ModItems.GRAVITY_RESEARCH_RUBBING.get()
                    : ModItems.VOID_RESEARCH_RUBBING.get()));
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!this.level().isClientSide() && !this.isRemoved()
                && this.level() instanceof ServerLevel level) {
            this.tickDeathCinematic(level);
            if (this.deathTime >= DEATH_CINEMATIC_TICKS) {
                this.level().broadcastEntityEvent(this, (byte) 60);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    private void tickDeathCinematic(ServerLevel level) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        if (this.deathTime <= 72 && (this.deathTime & 1) == 0) {
            double progress = this.deathTime / (double) DEATH_CINEMATIC_TICKS;
            double radius = 1.5D + (1.0D - progress) * 9.0D;
            double spin = -this.deathTime * 0.16D;
            for (int aspect = 0; aspect < 5; aspect++) {
                double angle = spin + aspect * Math.PI * 0.4D;
                level.sendParticles(aspectParticle(aspect),
                        core.x + Math.cos(angle) * radius,
                        core.y + Math.sin(angle * 2.0D) * 1.2D,
                        core.z + Math.sin(angle) * radius,
                        2, 0.06D, 0.08D, 0.06D, 0.01D);
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 8, radius * 0.12D, 1.0D,
                    radius * 0.12D, 0.08D);
        }
        if (this.deathTime % 12 == 0 && this.deathTime <= 60) {
            this.releaseDeathAspect(level, this.deathTime / 12 - 1);
        }
        if (this.deathTime == 72) {
            level.sendParticles(ParticleTypes.FLASH,
                    core.x, core.y, core.z, 4, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(this.currentBody(),
                    core.x, core.y, core.z, 240, 4.0D, 4.2D, 4.0D, 0.34D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 320, 4.4D, 4.5D, 4.4D, 0.46D);
            BossFx.ring(level, this.position().add(0.0D, 0.25D, 0.0D),
                    Math.min(24.0D, this.arenaRadius() - 2.0D), 96, 0.18D,
                    this.currentEmber());
            level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                    SoundSource.HOSTILE, 3.2F, 0.92F);
        }
    }

    private void releaseDeathAspect(ServerLevel level, int aspect) {
        Vec3 core = this.attackOrigin();
        Vec3 pylonTop = this.resonancePylonTop(level, aspect);
        ParticleOptions particle = aspectParticle(aspect);
        BossFx.ring(level, core, 3.5D + aspect * 1.8D,
                44 + aspect * 6, 0.1D, particle);
        if (pylonTop != null) {
            BossFx.guidanceFlow(level, core, pylonTop,
                    ParticleTypes.GLOW, particle, this.tickCount + aspect * 9);
            BossFx.ring(level, pylonTop.add(0.0D, -0.35D, 0.0D),
                    2.8D, 32, 0.08D, particle);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 2.2F, 1.15F - aspect * 0.1F);
    }

    public void registerParticipant(ServerPlayer player) {
        if (this.isEligibleCombatPlayer(player) && (this.participants.contains(player.getUUID())
                || this.participants.size() < BossNbtData.MAX_PARTICIPANTS)) {
            this.participants.add(player.getUUID());
        }
    }

    public boolean canBindToAltar() {
        return this.altarArenaId == null && this.altarPos == null;
    }

    public boolean bindToAltar(UUID arenaId, BlockPos pos) {
        if (!this.canBindToAltar() && !this.matchesAltar(arenaId, pos)) {
            return false;
        }
        this.altarArenaId = arenaId;
        this.altarPos = pos.immutable();
        this.restrictTo(this.altarPos, (int) Math.ceil(this.arenaRadius()));
        return true;
    }

    public boolean matchesAltar(UUID arenaId, BlockPos pos) {
        return arenaId != null && arenaId.equals(this.altarArenaId)
                && pos != null && pos.equals(this.altarPos);
    }

    private void notifyAltarSettled(ServerLevel level) {
        if (this.altarArenaId == null || this.altarPos == null) {
            return;
        }
        if (level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar) {
            altar.onBossSettled(this.altarArenaId, this.getUUID());
        }
    }

    private void broadcastNearby(Component message) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        AABB audienceArea = new AABB(this.position(), this.position()).inflate(radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                audienceArea, player -> !player.isSpectator())) {
            if (player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(message, true);
            }
        }
    }

    private void playEntranceFx(ServerLevel level) {
        this.bossEvent.setName(Component.translatable(
                "gui.unknown_echoes.world_echo.cast.manifesting", this.getDisplayName()));
        this.triggerAnim("attack_controller", "spawn");
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.8F, 0.75F);
        for (double radius : new double[]{3.0D, 6.0D, 9.0D}) {
            ring(level, radius, radius == 6.0D ? this.currentBody() : ParticleTypes.REVERSE_PORTAL);
        }
        level.sendParticles(ParticleTypes.GLOW,
                core.x, core.y, core.z, 120, 2.2D, 2.8D, 2.2D, 0.14D);
    }

    private boolean tickEntrance(ServerLevel level) {
        if (this.entranceTicksRemaining <= 0) {
            return false;
        }
        this.getNavigation().stop();
        this.setTarget(null);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, movement.y, 0.0D);
        if (this.tickCount % 5 == 0) {
            this.renderEntranceConvergence(level);
        }
        if (this.entranceTicksRemaining == 40
                || this.entranceTicksRemaining == 24
                || this.entranceTicksRemaining == 8) {
            double radius = 4.0D + (ENTRANCE_DURATION_TICKS - this.entranceTicksRemaining) * 0.18D;
            BossFx.ring(level, this.position().add(0.0D, 0.25D, 0.0D),
                    radius, 56, 0.12D, this.currentEmber());
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 2.4F, 0.65F + (float) radius * 0.025F);
        }
        this.entranceTicksRemaining--;
        if (this.entranceTicksRemaining == 0) {
            this.attackCooldown = Math.max(this.attackCooldown, 20);
            this.updateBossEventName();
            Vec3 core = this.attackOrigin();
            level.sendParticles(ParticleTypes.FLASH,
                    core.x, core.y, core.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(this.currentBody(),
                    core.x, core.y, core.z, 180, 3.0D, 3.6D, 3.0D, 0.24D);
            level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 3.2F, 0.82F);
        }
        return true;
    }

    private void renderEntranceConvergence(ServerLevel level) {
        if (this.altarPos == null || this.altarArenaId == null
                || !(level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar)
                || !this.matchesAltar(altar.getArenaId(), this.altarPos)) {
            return;
        }
        List<Vec3> pylons = altar.resonancePylonTops();
        Vec3 core = this.attackOrigin();
        for (int aspect = 0; aspect < pylons.size(); aspect++) {
            BossFx.guidanceFlow(level, pylons.get(aspect), core,
                    ParticleTypes.GLOW, aspectParticle(aspect), this.tickCount + aspect * 7);
        }
    }

    private static int duration(AttackMode mode) {
        return switch (mode) {
            case WIND -> 48;
            case TIDE -> 40;
            case TRUE_SIGHT -> 52;
            case GRAVITY -> 48;
            case VOID -> 56;
            case LANCE -> 36;
            case SLAM -> 44;
            case BARRAGE -> 36;
            case BEAM -> 52;
            case FLAME_IMPACT -> 44;
            case METEOR -> 56;
            case NOVA -> 48;
            case SKYFALL -> 48;
            case WING_STORM -> 40;
            case AERIAL_BARRAGE -> 60;
            case AERIAL_REAP -> 52;
            case PHASE -> 48;
            case NONE -> 0;
        };
    }

    private static String trigger(AttackMode mode) {
        return switch (mode) {
            case WIND -> "wind";
            case TIDE -> "tide";
            case TRUE_SIGHT -> "true_sight";
            case GRAVITY -> "gravity";
            case VOID -> "void";
            case LANCE -> "lance";
            case SLAM -> "slam";
            case BARRAGE -> "barrage";
            case BEAM -> "beam";
            case FLAME_IMPACT -> "flame";
            case METEOR -> "meteor";
            case NOVA -> "nova";
            case SKYFALL -> "skyfall";
            case WING_STORM -> "wing_storm";
            case AERIAL_BARRAGE -> "aerial_barrage";
            case AERIAL_REAP -> "aerial_reap";
            case PHASE -> "phase";
            case NONE -> "true_sight";
        };
    }

    private static String castTranslationKey(AttackMode mode) {
        return switch (mode) {
            case WIND -> "gui.unknown_echoes.world_echo.cast.wind";
            case TIDE -> "gui.unknown_echoes.world_echo.cast.tide";
            case TRUE_SIGHT -> "gui.unknown_echoes.world_echo.cast.true_sight";
            case GRAVITY -> "gui.unknown_echoes.world_echo.cast.gravity";
            case VOID -> "gui.unknown_echoes.world_echo.cast.void";
            case LANCE -> "gui.unknown_echoes.world_echo.cast.lance";
            case SLAM -> "gui.unknown_echoes.world_echo.cast.slam";
            case BARRAGE -> "gui.unknown_echoes.world_echo.cast.barrage";
            case BEAM -> "gui.unknown_echoes.world_echo.cast.beam";
            case FLAME_IMPACT -> "gui.unknown_echoes.world_echo.cast.flame";
            case METEOR -> "gui.unknown_echoes.world_echo.cast.meteor";
            case NOVA -> "gui.unknown_echoes.world_echo.cast.nova";
            case SKYFALL -> "gui.unknown_echoes.world_echo.cast.skyfall";
            case WING_STORM -> "gui.unknown_echoes.world_echo.cast.wing_storm";
            case AERIAL_BARRAGE -> "gui.unknown_echoes.world_echo.cast.aerial_barrage";
            case AERIAL_REAP -> "gui.unknown_echoes.world_echo.cast.aerial_reap";
            case PHASE -> "gui.unknown_echoes.world_echo.cast.phase";
            case NONE -> null;
        };
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.isCombatAudiencePlayer(player)) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.updateBossEventName();
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
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WORLD_ECHO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WORLD_ECHO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WORLD_ECHO_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhaseGateStage", this.phaseGateStage);
        tag.putBoolean("PhaseTransitionPending",
                this.phaseTransitionPending || this.phaseTransitionActive);
        tag.putLong("PhaseMechanismCooldownUntil", this.phaseMechanismCooldownUntil);
        tag.putInt("AttackCycle", this.attackCycle);
        tag.putInt("EntranceTicksRemaining", this.entranceTicksRemaining);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        tag.putInt("ArenaEmptyTicks", this.arenaEmptyTicks);
        tag.putBoolean("EncounterDormant", this.encounterDormant);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        if (this.altarArenaId != null && this.altarPos != null) {
            tag.putUUID("AltarArenaId", this.altarArenaId);
            tag.putLong("AltarPos", this.altarPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phaseGateStage = Math.max(0, Math.min(PHASE_FLOORS.length,
                tag.getInt("PhaseGateStage")));
        this.phaseTransitionPending = tag.getBoolean("PhaseTransitionPending");
        // 必须钳到"当前时间 + 冷却上限":存档里偏大的未来时间戳会让 activatePhasePylon 永久 return,
        // 玩家点柱子毫无反应也无报错。
        long savedPhaseCooldown = tag.getLong("PhaseMechanismCooldownUntil");
        long phaseCooldownNow = this.level().getGameTime();
        long maxPhaseCooldown = PHASE_MECHANISM_COOLDOWN_TICKS > Long.MAX_VALUE - phaseCooldownNow
                ? Long.MAX_VALUE : phaseCooldownNow + PHASE_MECHANISM_COOLDOWN_TICKS;
        this.phaseMechanismCooldownUntil = savedPhaseCooldown <= 0L
                ? 0L : Math.min(savedPhaseCooldown, maxPhaseCooldown);
        this.phaseTransitionActive = false;
        this.attackCycle = Math.max(0, tag.getInt("AttackCycle"));
        this.entranceTicksRemaining = tag.contains("EntranceTicksRemaining", Tag.TAG_INT)
                ? Math.max(0, Math.min(ENTRANCE_DURATION_TICKS,
                tag.getInt("EntranceTicksRemaining")))
                : 0;
        this.entranceFxPlayed = this.entranceTicksRemaining <= 0;
        this.finishAttack();
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.scaledParticipants = BossNbtData.clampParticipantScale(
                tag.getInt("ScaledParticipants"));
        this.arenaEmptyTicks = BossNbtData.clampTimer(
                tag.getInt("ArenaEmptyTicks"), ARENA_RESET_GRACE_TICKS);
        this.encounterDormant = tag.getBoolean("EncounterDormant");
        this.altarArenaId = tag.hasUUID("AltarArenaId") ? tag.getUUID("AltarArenaId") : null;
        this.altarPos = null;
        if (this.altarArenaId != null && tag.contains("AltarPos", net.minecraft.nbt.Tag.TAG_LONG)) {
            BlockPos savedPos = BlockPos.of(tag.getLong("AltarPos"));
            BlockPos current = this.blockPosition();
            if (Math.abs((long) savedPos.getX() - current.getX()) <= 128L
                    && Math.abs((long) savedPos.getY() - current.getY()) <= 128L
                    && Math.abs((long) savedPos.getZ() - current.getZ()) <= 128L) {
                this.altarPos = savedPos;
                this.restrictTo(this.altarPos, (int) Math.ceil(this.arenaRadius()));
            } else {
                this.altarArenaId = null;
            }
        }
        if (this.entranceTicksRemaining > 0) {
            this.bossEvent.setName(Component.translatable(
                    "gui.unknown_echoes.world_echo.cast.manifesting", this.getDisplayName()));
        } else if (this.encounterDormant) {
            this.arenaEmptyTicks = 0;
            this.updateBossEventName();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? GLIDE : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("wind", WIND)
                .triggerableAnim("tide", TIDE)
                .triggerableAnim("true_sight", TRUE_SIGHT)
                .triggerableAnim("gravity", GRAVITY)
                .triggerableAnim("void", VOID)
                .triggerableAnim("lance", LANCE)
                .triggerableAnim("slam", SLAM)
                .triggerableAnim("barrage", BARRAGE)
                .triggerableAnim("melee", MELEE)
                .triggerableAnim("beam", BEAM)
                .triggerableAnim("flame", FLAME_IMPACT)
                .triggerableAnim("meteor", METEOR)
                .triggerableAnim("nova", NOVA)
                .triggerableAnim("skyfall", SKYFALL)
                .triggerableAnim("wing_storm", WING_STORM)
                .triggerableAnim("aerial_barrage", AERIAL_BARRAGE)
                .triggerableAnim("aerial_reap", AERIAL_REAP)
                .triggerableAnim("phase", PHASE)
                .triggerableAnim("spawn", SPAWN)
                .triggerableAnim("death", DEATH));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    public AttackMode getSyncedAttackMode() {
        return AttackMode.byNetworkId(this.entityData.get(DATA_ATTACK_MODE));
    }

    public int getSyncedAttackAge() {
        return this.entityData.get(DATA_ATTACK_AGE);
    }

    public int getSyncedAttackDuration() {
        return this.entityData.get(DATA_ATTACK_DURATION);
    }

    @Override
    public int visualAttackMode() {
        return this.entityData.get(DATA_ATTACK_MODE);
    }

    @Override
    public int visualAttackAge() {
        return this.entityData.get(DATA_ATTACK_AGE);
    }

    @Override
    public int visualAttackDuration() {
        return this.entityData.get(DATA_ATTACK_DURATION);
    }

    public enum AttackMode {
        NONE(0),
        WIND(1),
        TIDE(2),
        TRUE_SIGHT(3),
        GRAVITY(4),
        VOID(5),
        LANCE(6),
        SLAM(7),
        BARRAGE(8),
        BEAM(9),
        FLAME_IMPACT(10),
        METEOR(11),
        NOVA(12),
        SKYFALL(13),
        WING_STORM(14),
        PHASE(15),
        AERIAL_BARRAGE(16),
        AERIAL_REAP(17);

        private static final AttackMode[] VALUES = values();
        private final int networkId;

        AttackMode(int networkId) {
            this.networkId = networkId;
        }

        public int networkId() {
            return this.networkId;
        }

        public static AttackMode byNetworkId(int networkId) {
            return networkId >= 0 && networkId < VALUES.length ? VALUES[networkId] : NONE;
        }
    }
}
