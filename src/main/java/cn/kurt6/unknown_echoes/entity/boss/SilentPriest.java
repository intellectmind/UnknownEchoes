package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.boss.ResonanceCandleBlock;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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

public class SilentPriest extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("silent_priest");

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.silent_priest.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.silent_priest.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.silent_priest.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WAVE_ANIM =
            RawAnimation.begin().then("animation.silent_priest.wave", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation MARK_ANIM =
            RawAnimation.begin().then("animation.silent_priest.mark", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.silent_priest.broken");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 静默领域开启(同步客户端:压低音乐等表现;领域范围 = DOMAIN_RADIUS)。 */
    private static final EntityDataAccessor<Boolean> DATA_DOMAIN =
            SynchedEntityData.defineId(SilentPriest.class, EntityDataSerializers.BOOLEAN);
    /** 失声硬直(领域结束后的输出窗口,失声阶段专属)。 */
    private static final EntityDataAccessor<Boolean> DATA_STAGGERED =
            SynchedEntityData.defineId(SilentPriest.class, EntityDataSerializers.BOOLEAN);

    private static final int ARENA_RADIUS = 12;
    public static final double DOMAIN_RADIUS = 16.0D;

    /** 领域时长:基础 + 每根完好共鸣烛延长(4 根全在 = 110 tick,全碎 = 50 tick)。 */
    private static final int DOMAIN_BASE_TICKS = 50;
    private static final int DOMAIN_PER_CANDLE_TICKS = 15;
    private static final int MAX_DOMAIN_CANDLES = 16;
    private static final int GUIDANCE_CANDLE_BATCH = 4;
    private static final int DOMAIN_INTERVAL_WHISPER = 400;
    private static final int DOMAIN_INTERVAL_VOICELESS = 260;
    private static final int STAGGER_TICKS = 70;

    private static final int WAVE_TELEGRAPH_TICKS = 12;
    private static final double WAVE_MAX_RADIUS = 7.0D;

    private static final int SWEEP_TELEGRAPH_TICKS = 12;
    private static final double SWEEP_RANGE = 4.0D;
    /** 横扫的锥形 dot 阈值。演出锥半角由它反算，两处必须同一个常量。 */
    private static final double SWEEP_MIN_DOT = 0.5D;
    /** 噤声波的命中带半宽：只有距波前这个距离内才结算。 */
    private static final double WAVE_BAND_HALF_WIDTH = 1.2D;

    private static final int MARK_DELAY_TICKS = 30;
    private static final int MARK_ZONE_TICKS = 80;
    private static final double MARK_ZONE_RADIUS = 3.0D;

    private int domainCooldown = 200;
    private int domainTicksRemaining = 0;
    private int candlesAtDomainStart = 0;
    private final List<BlockPos> domainCandlePositions = new ArrayList<>();
    private boolean domainCandlesInitialized = false;
    private int staggerTicksRemaining = 0;

    private int waveCooldown = 0;
    private int sweepCooldown = 0;
    private int markCooldown = 0;
    private int waveTelegraphTicks = 0;
    /** 噤声波当前扩散半径,-1 = 未激活。 */
    private double waveRadius = -1.0D;
    private final Set<UUID> waveHitVictims = new HashSet<>();
    private int sweepTelegraphTicks = 0;
    private Vec3 markPos = null;
    private int markDelayTicks = 0;
    private int markZoneTicks = 0;

    public SilentPriest(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 225.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DOMAIN, false);
        builder.define(DATA_STAGGERED, false);
    }

    public boolean isDomainActive() {
        return this.entityData.get(DATA_DOMAIN);
    }

    private void setDomainActive(boolean active) {
        this.entityData.set(DATA_DOMAIN, active);
    }

    public boolean isStaggered() {
        return this.entityData.get(DATA_STAGGERED);
    }

    private void setStaggered(boolean staggered) {
        this.entityData.set(DATA_STAGGERED, staggered);
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

        return this.isDomainActive();
    }

    @Override
    protected String guardedHintKey() {

        return null;
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        Vec3 core = this.position().add(0, 1.2, 0);
        int size = this.domainCandlePositions.size();
        if (size == 0) {
            return;
        }
        int start = Math.floorMod((this.tickCount / 5) * GUIDANCE_CANDLE_BATCH, size);
        int guided = 0;
        for (int i = 0; i < size && guided < GUIDANCE_CANDLE_BATCH; i++) {
            BlockPos pos = this.domainCandlePositions.get((start + i) % size);
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockState(pos).getBlock() instanceof ResonanceCandleBlock)) {
                continue;
            }
            Vec3 candle = Vec3.atCenterOf(pos).add(0, 0.35, 0);
            MiniBossFx.objectiveMarker(level, candle, this.fxTheme(), level.getGameTime());
            MiniBossFx.objectiveLink(level, candle, core, this.fxTheme(), level.getGameTime());
            guided++;
        }
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantSilentPriest(this, firstKill);
    }

    private boolean isVoicelessPhase() {
        return this.getHealth() <= this.getMaxHealth() * 0.5F;
    }

    private boolean initializeDomainCandles(ServerLevel serverLevel) {
        BlockPos anchor = this.arenaAnchor();
        if (!isScanAreaLoaded(serverLevel, anchor, ARENA_RADIUS)) {
            return false;
        }
        this.domainCandlePositions.clear();
        for (BlockPos pos : BlockPos.betweenClosed(
                anchor.offset(-ARENA_RADIUS, -3, -ARENA_RADIUS),
                anchor.offset(ARENA_RADIUS, 4, ARENA_RADIUS))) {
            if (serverLevel.getBlockState(pos).getBlock() instanceof ResonanceCandleBlock) {
                this.domainCandlePositions.add(pos.immutable());
                if (this.domainCandlePositions.size() >= MAX_DOMAIN_CANDLES) {
                    break;
                }
            }
        }
        this.candlesAtDomainStart = this.domainCandlePositions.size();
        this.domainCandlesInitialized = true;
        return true;
    }

    private int countIntactCandles(ServerLevel serverLevel) {
        int count = 0;
        for (BlockPos pos : this.domainCandlePositions) {
            if (!serverLevel.hasChunkAt(pos)
                    || serverLevel.getBlockState(pos).getBlock() instanceof ResonanceCandleBlock) {
                count++;
            }
        }
        return count;
    }

    private void tickDomain(ServerLevel serverLevel) {
        if (this.domainTicksRemaining > 0) {
            if (!this.domainCandlesInitialized && !initializeDomainCandles(serverLevel)) {
                return;
            }
            int intact = countIntactCandles(serverLevel);
            if (intact < this.candlesAtDomainStart) {
                this.domainTicksRemaining -= (this.candlesAtDomainStart - intact) * DOMAIN_PER_CANDLE_TICKS;
                this.candlesAtDomainStart = intact;
            }

            if (this.tickCount % 4 == 0) {
                // 领域环画在 DOMAIN_RADIUS 上：这是玩家"进入静默"的实际边界。
                MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(),
                        DOMAIN_RADIUS);
            }
            if (--this.domainTicksRemaining <= 0) {
                this.endDomain(serverLevel);
            }
            return;
        }
        if (this.getTarget() == null || !this.isAlive() || this.isStaggered()) {
            return;
        }
        if (--this.domainCooldown <= 0) {
            this.startDomain(serverLevel);
        }
    }

    private void startDomain(ServerLevel serverLevel) {
        if (!initializeDomainCandles(serverLevel)) {
            return;
        }
        this.domainTicksRemaining = DOMAIN_BASE_TICKS + this.candlesAtDomainStart * DOMAIN_PER_CANDLE_TICKS;
        this.setDomainActive(true);

        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.SILENT_PRIEST_DOMAIN.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), DOMAIN_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                30.0D, 0.5F, 7, this.combatImpactColor());
    }

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, radius);
    }

    private void endDomain(ServerLevel serverLevel) {
        this.setDomainActive(false);
        this.domainCandlePositions.clear();
        this.domainCandlesInitialized = false;
        this.domainCooldown = this.isVoicelessPhase() ? DOMAIN_INTERVAL_VOICELESS : DOMAIN_INTERVAL_WHISPER;
        if (this.isVoicelessPhase()) {

            this.setStaggered(true);
            this.staggerTicksRemaining = STAGGER_TICKS;
            this.getNavigation().stop();
            MiniBossFx.breachAura(serverLevel, this, this.fxTheme(), serverLevel.getGameTime());
        }

        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.SILENT_PRIEST_AMBIENT.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
    }

    /** 领域内不发任何战斗提示音(压制听觉线索);领域外正常。 */
    private void playCue(ServerLevel serverLevel, SoundEvent sound, float volume, float pitch) {
        if (!this.isDomainActive()) {
            serverLevel.playSound(null, this.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        }
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

        this.tickDomain(serverLevel);

        if (this.isStaggered()) {
            this.getNavigation().stop();
            if (this.tickCount % 5 == 0) {
                MiniBossFx.breachAura(serverLevel, this, this.fxTheme(),
                        serverLevel.getGameTime());
            }
            if (--this.staggerTicksRemaining <= 0) {
                this.setStaggered(false);
            }
            return;
        }

        this.tickSpecialAttacks(serverLevel);
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.waveCooldown > 0) this.waveCooldown--;
        if (this.sweepCooldown > 0) this.sweepCooldown--;
        if (this.markCooldown > 0) this.markCooldown--;

        if (this.markDelayTicks > 0) {
            this.tickMarkTelegraph(serverLevel);
            if (--this.markDelayTicks == 0) {
                this.markZoneTicks = MARK_ZONE_TICKS;
                if (this.markPos != null) {
                    this.playCue(serverLevel, ModSounds.SILENT_PRIEST_ATTACK_MARK_RELEASE.get(), 1.1F, 1.0F);
                    BossFx.cinematicImpact(serverLevel, this.markPos,
                            24.0D, 0.34F, 5, this.combatImpactColor());
                }
            }
        } else if (this.markZoneTicks > 0) {
            this.tickMarkZone(serverLevel);
        }

        if (this.waveTelegraphTicks > 0) {
            this.tickWaveTelegraph(serverLevel);
            if (--this.waveTelegraphTicks == 0) {
                this.waveRadius = 1.0D;
                this.playCue(serverLevel, ModSounds.SILENT_PRIEST_ATTACK_WAVE_RELEASE.get(), 1.2F, 1.0F);
                this.waveHitVictims.clear();
            }
            return;
        }
        if (this.waveRadius >= 0) {
            this.tickWaveExpansion(serverLevel);
            return;
        }

        if (this.sweepTelegraphTicks > 0) {

            Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
            // 前摇就画出横扫的扇形，玩家才有侧移的依据；锥半角由结算侧的 dot 反算。
            MiniBossFx.coneSpray(serverLevel, this.position().add(0, 0.7, 0), facing,
                    this.fxTheme(), SWEEP_RANGE, Math.acos(SWEEP_MIN_DOT));
            if (--this.sweepTelegraphTicks == 0) {
                this.doSweepDamage(serverLevel);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.waveCooldown <= 0 && distSq <= 5.0 * 5.0) {

            this.waveTelegraphTicks = WAVE_TELEGRAPH_TICKS;
            this.waveCooldown = 160;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "wave");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_WAVE.get(), 1.4F, 1.0F);
        } else if (this.sweepCooldown <= 0 && distSq <= SWEEP_RANGE * SWEEP_RANGE) {
            this.sweepTelegraphTicks = SWEEP_TELEGRAPH_TICKS;
            this.sweepCooldown = 90;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_ATTACK_SWEEP_WARN.get(), 1.2F, 1.0F);
        } else if (this.markCooldown <= 0 && this.markZoneTicks <= 0 && this.markDelayTicks <= 0
                && distSq <= 14.0 * 14.0) {

            this.markPos = target.position();
            this.markDelayTicks = MARK_DELAY_TICKS;
            this.markCooldown = 220;
            this.triggerAnim("attack_controller", "mark");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_MARK.get(), 1.2F, 1.0F);
        }
    }

    /** 噤声波预警:脚下浮现无声涟漪,只有画面没有声音。 */
    private void tickWaveTelegraph(ServerLevel serverLevel) {
        double progress = 1.0D - this.waveTelegraphTicks / (double) WAVE_TELEGRAPH_TICKS;
        MiniBossFx.chargeConverge(serverLevel, this.position(), this.fxTheme(),
                WAVE_MAX_RADIUS, progress);
    }

    private void tickWaveExpansion(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            this.waveRadius = -1.0D;
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        // 环画在当前 waveRadius 上：这道波是逐帧长大的判定，画固定半径玩家读不出它到哪了。
        MiniBossFx.expandingRing(serverLevel, this.position(), this.fxTheme(),
                this.waveRadius, this.waveRadius / WAVE_MAX_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(this.waveRadius + 0.8, 1.5, this.waveRadius + 0.8),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy)
                        && !this.waveHitVictims.contains(e.getUUID()))) {
            double dist = victim.position().subtract(this.position()).horizontalDistance();
            if (Math.abs(dist - this.waveRadius) > WAVE_BAND_HALF_WIDTH) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 0.8, 0.3, dir.z * 0.8));
                    victim.hurtMarked = true;
                }
            }
            this.waveHitVictims.add(victim.getUUID());
        }
        this.waveRadius += 0.8D;
        if (this.waveRadius > WAVE_MAX_RADIUS) {
            this.waveRadius = -1.0D;
        }
    }

    private void doSweepDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SWEEP_RANGE, 1.5, SWEEP_RANGE),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > SWEEP_RANGE * SWEEP_RANGE
                    || (toVictim.lengthSqr() > 1.0E-4
                        && facing.dot(toVictim.normalize()) < SWEEP_MIN_DOT)) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(facing.x * 0.5, 0.2, facing.z * 0.5));
                victim.hurtMarked = true;
            }
        }
        this.playCue(serverLevel, ModSounds.SILENT_PRIEST_SWEEP.get(), 1.4F, 1.0F);
        MiniBossFx.coneSpray(serverLevel, this.position().add(0, 0.7, 0), facing,
                this.fxTheme(), SWEEP_RANGE, Math.acos(SWEEP_MIN_DOT));
        BossFx.cinematicImpact(serverLevel, this.position().add(facing.scale(2.0)),
                22.0D, 0.28F, 4, this.combatImpactColor());
    }

    private void tickMarkTelegraph(ServerLevel serverLevel) {
        if (this.markPos == null) {
            this.markDelayTicks = 0;
            return;
        }
        // 预警区画在实际结算半径上，随进度收紧；画大或画小都会让玩家站错位置。
        MiniBossFx.impactZone(serverLevel, this.markPos, this.fxTheme(), MARK_ZONE_RADIUS,
                1.0D - this.markDelayTicks / (double) MARK_DELAY_TICKS);
    }

    private void tickMarkZone(ServerLevel serverLevel) {
        if (this.markPos == null) {
            this.markZoneTicks = 0;
            return;
        }
        if (this.markZoneTicks == MARK_ZONE_TICKS) {
            MiniBossFx.sealRelease(serverLevel, this.markPos, this.fxTheme(), MARK_ZONE_RADIUS);
        }
        this.markZoneTicks--;
        if (this.tickCount % 3 == 0) {
            MiniBossFx.groundRing(serverLevel, this.markPos, this.fxTheme(), MARK_ZONE_RADIUS);
        }
        if (this.markZoneTicks % 10 == 0) {
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(this.markPos.x - MARK_ZONE_RADIUS, this.markPos.y - 1.0,
                            this.markPos.z - MARK_ZONE_RADIUS,
                            this.markPos.x + MARK_ZONE_RADIUS, this.markPos.y + 2.5,
                            this.markPos.z + MARK_ZONE_RADIUS),
                    e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
                Vec3 horizontal = victim.position().subtract(this.markPos).multiply(1, 0, 1);
                if (horizontal.lengthSqr() > MARK_ZONE_RADIUS * MARK_ZONE_RADIUS) {
                    continue;
                }
                victim.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25F);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
        if (this.markZoneTicks <= 0) {
            this.markPos = null;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            if (this.level() instanceof ServerLevel serverLevel) {
                this.playCue(serverLevel, ModSounds.SILENT_PRIEST_ATTACK_MELEE.get(), 1.1F, 1.0F);
            }
        }
        return hit;
    }

    @Override
    protected void onDeathCommitted(ServerLevel serverLevel, Vec3 deathPos) {
        this.setDomainActive(false);
        if (isCurrentDeathPresentation(serverLevel, deathPos)) {
            MiniBossFx.expandingRing(serverLevel, deathPos, this.fxTheme(), DOMAIN_RADIUS, 1.0D);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isDomainActive() ? null : ModSounds.SILENT_PRIEST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isDomainActive() ? null : ModSounds.SILENT_PRIEST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SILENT_PRIEST_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DomainTicks", this.domainTicksRemaining);
        tag.putInt("DomainCooldown", this.domainCooldown);
        tag.putInt("StaggerTicks", this.staggerTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.domainTicksRemaining = Math.max(0, Math.min(
                DOMAIN_BASE_TICKS + MAX_DOMAIN_CANDLES * DOMAIN_PER_CANDLE_TICKS,
                tag.getInt("DomainTicks")));
        this.domainCooldown = Math.max(60, Math.min(DOMAIN_INTERVAL_WHISPER,
                tag.getInt("DomainCooldown")));
        this.staggerTicksRemaining = Math.max(0, Math.min(STAGGER_TICKS, tag.getInt("StaggerTicks")));
        if (this.domainTicksRemaining > 0) {
            this.staggerTicksRemaining = 0;
        }
        this.domainCandlePositions.clear();
        this.domainCandlesInitialized = false;
        this.setDomainActive(this.domainTicksRemaining > 0);
        this.setStaggered(this.staggerTicksRemaining > 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isStaggered()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("wave", WAVE_ANIM)
                .triggerableAnim("mark", MARK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
    @Override
    public int musicStage() {
        return this.isStaggered() ? 1 : this.isVoicelessPhase() ? 2 : 0;
    }
}
