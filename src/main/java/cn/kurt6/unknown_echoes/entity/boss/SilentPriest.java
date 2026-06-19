package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.boss.ResonanceCandleBlock;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 沉默祭司:失语沼泽区域守护者 / Mini Boss(沉默祭坛,设计文档 10.4.2)。
 *
 * 机制:周期性展开"静默领域"——领域内压制提示音与 HUD 线索(服务端不发战斗提示音/actionbar,
 * 客户端压低音乐),迫使玩家观察动作前摇本身;打碎祭坛四角的共鸣烛可缩短领域持续时间。
 * 领域期间受机制保护(未破防双保险)。
 * 阶段:
 * - 低语(>50%):常规近战与范围压制,领域周期较长。
 * - 失声(≤50%):领域更频繁,但每次领域结束后有明确硬直输出窗口(失声硬直)。
 * 攻击:噤声波(无声涟漪预警 → 环形冲击)/ 苔杖横扫(抬杖前摇 + 扇形)/
 *       沉默标记(标记落点 → 延迟生成压制区,迫使移动)。
 * 结算(个人):首杀 = 静默派研究拓片 + 噤声苔 + 沉默荚果 + 回响粉尘;重复 = 普通材料。
 */
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
    private static final int DOMAIN_INTERVAL_WHISPER = 400;
    private static final int DOMAIN_INTERVAL_VOICELESS = 260;
    private static final int STAGGER_TICKS = 70;

    // 噤声波:无声涟漪预警 0.6s → 环形冲击扩散到 7 格
    private static final int WAVE_TELEGRAPH_TICKS = 12;
    private static final double WAVE_MAX_RADIUS = 7.0D;
    // 苔杖横扫:抬杖前摇 0.6s,正面约 120° 扇形 4 格
    private static final int SWEEP_TELEGRAPH_TICKS = 12;
    private static final double SWEEP_RANGE = 4.0D;
    // 沉默标记:标记 1.5s 后生成压制区,持续 4s
    private static final int MARK_DELAY_TICKS = 30;
    private static final int MARK_ZONE_TICKS = 80;
    private static final double MARK_ZONE_RADIUS = 3.0D;

    private int domainCooldown = 200;
    private int domainTicksRemaining = 0;
    private int candlesAtDomainStart = 0;
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

    // ---- MiniBoss 契约 ----

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
        // 静默领域期间受机制保护;领域之外(尤其失声硬直)全额伤害
        return this.isDomainActive();
    }

    @Override
    protected String guardedHintKey() {
        // 静默领域同时压制 HUD 提示——被守护时不给 actionbar 文案,保持"无声"的一致性
        return null;
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantSilentPriest(this, player, firstKill);
    }

    // ---- 阶段与静默领域 ----

    /** 失声阶段:半血以下,领域更频繁,但领域结束后有硬直输出窗口。 */
    private boolean isVoicelessPhase() {
        return this.getHealth() <= this.getMaxHealth() * 0.5F;
    }

    /** 数场地内完好的共鸣烛(打碎 = 缩短领域)。 */
    private int countIntactCandles(ServerLevel serverLevel) {
        BlockPos anchor = this.arenaAnchor();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                anchor.offset(-ARENA_RADIUS, -3, -ARENA_RADIUS),
                anchor.offset(ARENA_RADIUS, 4, ARENA_RADIUS))) {
            if (serverLevel.getBlockState(pos).getBlock() instanceof ResonanceCandleBlock) {
                count++;
            }
        }
        return count;
    }

    private void tickDomain(ServerLevel serverLevel) {
        if (this.domainTicksRemaining > 0) {
            // 领域中打碎共鸣烛立即缩短剩余时间
            int intact = countIntactCandles(serverLevel);
            if (intact < this.candlesAtDomainStart) {
                this.domainTicksRemaining -= (this.candlesAtDomainStart - intact) * DOMAIN_PER_CANDLE_TICKS;
                this.candlesAtDomainStart = intact;
            }
            // 领域边界演出:贴地灰雾环(无声——这正是机制)
            if (this.tickCount % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI * 2 * i / 10 + this.tickCount * 0.05;
                    serverLevel.sendParticles(ParticleTypes.ASH,
                            this.getX() + Math.cos(angle) * DOMAIN_RADIUS * 0.6,
                            this.getY() + 0.3,
                            this.getZ() + Math.sin(angle) * DOMAIN_RADIUS * 0.6,
                            1, 0.2, 0.4, 0.2, 0.0);
                }
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
        this.candlesAtDomainStart = countIntactCandles(serverLevel);
        this.domainTicksRemaining = DOMAIN_BASE_TICKS + this.candlesAtDomainStart * DOMAIN_PER_CANDLE_TICKS;
        this.setDomainActive(true);
        // 开启瞬间是最后一个"有声"提示:之后领域内一切提示音静默
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.SILENT_PRIEST_WAVE.get(), SoundSource.HOSTILE, 1.6F, 0.5F);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                this.getX(), this.getY() + 1.2, this.getZ(), 20, 1.5, 0.8, 1.5, 0.02);
    }

    private void endDomain(ServerLevel serverLevel) {
        this.setDomainActive(false);
        this.domainCooldown = this.isVoicelessPhase() ? DOMAIN_INTERVAL_VOICELESS : DOMAIN_INTERVAL_WHISPER;
        if (this.isVoicelessPhase()) {
            // 失声硬直:领域结束的明确输出窗口(10.4.2)
            this.setStaggered(true);
            this.staggerTicksRemaining = STAGGER_TICKS;
            this.getNavigation().stop();
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 8, 0.6, 0.5, 0.6, 1.0);
        }
        // 领域散去:声音"回来了"
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.SILENT_PRIEST_AMBIENT.get(), SoundSource.HOSTILE, 1.2F, 1.4F);
    }

    /** 领域内不发任何战斗提示音(压制听觉线索);领域外正常。 */
    private void playCue(ServerLevel serverLevel, SoundEvent sound, float volume, float pitch) {
        if (!this.isDomainActive()) {
            serverLevel.playSound(null, this.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        }
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.tickDomain(serverLevel);

        if (this.isStaggered()) {
            this.getNavigation().stop();
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 1.2, this.getZ(), 2, 0.4, 0.6, 0.4, 0.02);
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

        // 沉默标记:延迟生成压制区(与其他动作并行推进)
        if (this.markDelayTicks > 0) {
            this.tickMarkTelegraph(serverLevel);
            if (--this.markDelayTicks == 0) {
                this.markZoneTicks = MARK_ZONE_TICKS;
            }
        } else if (this.markZoneTicks > 0) {
            this.tickMarkZone(serverLevel);
        }

        // 噤声波:预警 → 环形扩散
        if (this.waveTelegraphTicks > 0) {
            this.tickWaveTelegraph(serverLevel);
            if (--this.waveTelegraphTicks == 0) {
                this.waveRadius = 1.0D;
                this.waveHitVictims.clear();
            }
            return;
        }
        if (this.waveRadius >= 0) {
            this.tickWaveExpansion(serverLevel);
            return;
        }
        // 苔杖横扫:前摇 → 扇形命中
        if (this.sweepTelegraphTicks > 0) {
            // 抬杖前摇:杖端苔屑聚集(可见前摇,领域内无声)
            Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getX() + facing.x * 1.2, this.getY() + 2.0, this.getZ() + facing.z * 1.2,
                    2, 0.15, 0.15, 0.15, 0.0);
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
            // 噤声波:近身压制主力
            this.waveTelegraphTicks = WAVE_TELEGRAPH_TICKS;
            this.waveCooldown = 160;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "wave");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_WAVE.get(), 1.4F, 0.8F);
        } else if (this.sweepCooldown <= 0 && distSq <= SWEEP_RANGE * SWEEP_RANGE) {
            this.sweepTelegraphTicks = SWEEP_TELEGRAPH_TICKS;
            this.sweepCooldown = 90;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_AMBIENT.get(), 1.2F, 0.7F);
        } else if (this.markCooldown <= 0 && this.markZoneTicks <= 0 && this.markDelayTicks <= 0
                && distSq <= 14.0 * 14.0) {
            // 沉默标记:标记目标脚下,延迟压制——迫使移动
            this.markPos = target.position();
            this.markDelayTicks = MARK_DELAY_TICKS;
            this.markCooldown = 220;
            this.triggerAnim("attack_controller", "mark");
            this.playCue(serverLevel, ModSounds.SILENT_PRIEST_AMBIENT.get(), 1.2F, 1.3F);
        }
    }

    /** 噤声波预警:脚下浮现"无声涟漪"——刻意只有画面没有声音(机制即主题)。 */
    private void tickWaveTelegraph(ServerLevel serverLevel) {
        double r = 1.5 + (WAVE_TELEGRAPH_TICKS - this.waveTelegraphTicks) * 0.2;
        for (int i = 0; i < 14; i++) {
            double angle = Math.PI * 2 * i / 14;
            serverLevel.sendParticles(ParticleTypes.ASH,
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + 0.1,
                    this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.02, 0.05, 0.0);
        }
        BossFx.chargeSpiral(serverLevel, this.position().add(0, 0.2, 0), this.tickCount, ParticleTypes.ASH);
    }

    /** 噤声波扩散:环形冲击逐格外推,沿环中伤害 + 击退,单次施放每人只命中一次。 */
    private void tickWaveExpansion(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            this.waveRadius = -1.0D;
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        // 冲击环演出
        int points = (int) (this.waveRadius * 8);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    this.getX() + Math.cos(angle) * this.waveRadius,
                    this.getY() + 0.6,
                    this.getZ() + Math.sin(angle) * this.waveRadius, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (this.tickCount % 2 == 0) {
            BossFx.ring(serverLevel, this.position(), this.waveRadius, Math.max(12, points / 2), 0.25, ParticleTypes.ASH);
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(this.waveRadius + 0.8, 1.5, this.waveRadius + 0.8),
                e -> e != this && !(e instanceof Enemy)
                        && !this.waveHitVictims.contains(e.getUUID()))) {
            double dist = victim.position().subtract(this.position()).horizontalDistance();
            if (Math.abs(dist - this.waveRadius) > 1.2) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.8, 0.3, dir.z * 0.8));
                victim.hurtMarked = true;
            }
            this.waveHitVictims.add(victim.getUUID());
        }
        this.waveRadius += 0.8D;
        if (this.waveRadius > WAVE_MAX_RADIUS) {
            this.waveRadius = -1.0D;
        }
    }

    /** 苔杖横扫命中:正面约 120° 扇形。 */
    private void doSweepDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SWEEP_RANGE, 1.5, SWEEP_RANGE),
                e -> e != this && !(e instanceof Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.5) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(facing.x * 0.5, 0.2, facing.z * 0.5));
            victim.hurtMarked = true;
        }
        // 扫击苔屑弧
        for (int i = -3; i <= 3; i++) {
            double angle = Math.atan2(facing.z, facing.x) + i * 0.25;
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getX() + Math.cos(angle) * 2.2,
                    this.getY() + 1.2,
                    this.getZ() + Math.sin(angle) * 2.2, 2, 0.1, 0.1, 0.1, 0.0);
        }
        this.playCue(serverLevel, ModSounds.SILENT_PRIEST_AMBIENT.get(), 1.4F, 0.9F);
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.7, 0), facing, SWEEP_RANGE, ParticleTypes.SPORE_BLOSSOM_AIR);
        BossFx.impactBurst(serverLevel, this.position().add(facing.scale(2.0)).add(0, 1.0, 0), ParticleTypes.SOUL_FIRE_FLAME);
    }

    /** 沉默标记预警:标记点灰纹收束。 */
    private void tickMarkTelegraph(ServerLevel serverLevel) {
        if (this.markPos == null) {
            this.markDelayTicks = 0;
            return;
        }
        double progress = 1.0 - this.markDelayTicks / (double) MARK_DELAY_TICKS;
        double r = MARK_ZONE_RADIUS * (1.2 - progress * 0.5);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + this.tickCount * 0.15;
            serverLevel.sendParticles(ParticleTypes.ASH,
                    this.markPos.x + Math.cos(angle) * r,
                    this.markPos.y + 0.1,
                    this.markPos.z + Math.sin(angle) * r, 1, 0.05, 0.02, 0.05, 0.0);
        }
        if (this.tickCount % 4 == 0) {
            BossFx.ring(serverLevel, this.markPos, MARK_ZONE_RADIUS, 18, 0.12, ParticleTypes.SQUID_INK);
        }
    }

    /** 压制区:站在里面持续缓速 + 虚弱 + 轻微伤害,迫使离开。 */
    private void tickMarkZone(ServerLevel serverLevel) {
        if (this.markPos == null) {
            this.markZoneTicks = 0;
            return;
        }
        this.markZoneTicks--;
        if (this.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.markPos.x, this.markPos.y + 0.5, this.markPos.z,
                    3, MARK_ZONE_RADIUS * 0.5, 0.4, MARK_ZONE_RADIUS * 0.5, 0.0);
        }
        if (this.markZoneTicks % 10 == 0) {
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(this.markPos.x - MARK_ZONE_RADIUS, this.markPos.y - 1.0,
                            this.markPos.z - MARK_ZONE_RADIUS,
                            this.markPos.x + MARK_ZONE_RADIUS, this.markPos.y + 2.5,
                            this.markPos.z + MARK_ZONE_RADIUS),
                    e -> e != this && !(e instanceof Enemy))) {
                victim.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25F);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
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
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.SPORE_BLOSSOM_AIR);
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 死亡演出:领域彻底散去,灰雾向外散开
            this.setDomainActive(false);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 1.0, this.getZ(), 40, 1.5, 1.0, 1.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 12, 1.0, 0.8, 1.0, 1.0);
            double radius = settlementRadius();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    player.sendSystemMessage(
                            Component.translatable("message.unknown_echoes.silent_priest.defeated"));
                }
            }
        }
    }

    // ---- 音效:领域内本体也保持静默 ----

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

    // ---- NBT ----

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
        this.domainTicksRemaining = tag.getInt("DomainTicks");
        this.domainCooldown = Math.max(60, tag.getInt("DomainCooldown"));
        this.staggerTicksRemaining = tag.getInt("StaggerTicks");
        this.setDomainActive(this.domainTicksRemaining > 0);
        this.setStaggered(this.staggerTicksRemaining > 0);
    }

    // ---- GeckoLib ----

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
}
