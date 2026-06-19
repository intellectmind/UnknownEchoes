package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 晶歌守谱者:晶歌大乐厅 Mini Boss(晶歌林地,设计文档 10.5 / 7.4 节奏谜题)。
 *
 * 机制(音柱节奏调音破防,设计红线 #4):
 * - 失衡破防:默认受护(未破防,伤害被压);只有完成"按序调音"进入失衡窗口才吃全额伤害。
 * - 节奏演示(Simon):守谱者按顺序逐根点亮场地音柱(CRYSTAL_SONG_CLUSTER,光束 + 升调编钟);
 *   玩家照演示顺序"调音"(右键音柱 / 谱杖音波命中音柱)复现;全对 → 失衡 8 秒 + 打断大音波;错 → 重演。
 *   顺序只存服务端字段,演示靠粒子表现(本就该被看见),不泄露隐藏数据。
 * - 音波环(soundwave):贴身扩散环 AoE,击退 + 低伤。
 * - 共振充能 → 大音波(charge → bigwave):长读条蓄力,完成调音(失衡)可打断;
 *   未被打断则轰出覆盖全场的大音波(重伤 + 减速)。
 * - 谱杖近战(attack):命中触发挥击。
 * 结算沿用 MiniBossEntity 个人首杀/重复发放(MiniBossRewardTable)。
 */
public class CrystalSongkeeper extends MiniBossEntity {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("crystal_songkeeper");
    private static final int ARENA_RADIUS = 14;
    private static final int CHARGE_TICKS = 70;
    /** 完成调音后的失衡窗口(tick,= 文档 7.4 的"失衡 8 秒")。 */
    private static final int STAGGER_TICKS = 160;
    /** 调音音柱搜索范围(覆盖整座大乐厅,半径 14)。 */
    private static final int PILLAR_SCAN_RADIUS = 14;
    private static final int PILLAR_SCAN_HEIGHT = 8;
    /** 节奏演示:单根亮持续 / 单步总时长(亮 + 间隔)。 */
    private static final int DEMO_LIT_TICKS = 14;
    private static final int DEMO_STEP_TICKS = 22;
    /** 序列长度:从场地音柱里取这么多步(不足则用实际数量)。 */
    private static final int SEQUENCE_LENGTH = 4;
    /** 调音生效范围:玩家右键/谱杖命中的音柱必须在守谱者这个范围内。 */
    public static final double TUNE_RANGE = 18.0D;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crystal_songkeeper.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crystal_songkeeper.walk");
    private static final RawAnimation ATTACK =
            RawAnimation.begin().then("animation.crystal_songkeeper.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SOUNDWAVE =
            RawAnimation.begin().then("animation.crystal_songkeeper.soundwave", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CHARGE =
            RawAnimation.begin().then("animation.crystal_songkeeper.charge", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BIGWAVE =
            RawAnimation.begin().then("animation.crystal_songkeeper.bigwave", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // 节奏谜题(Simon)状态:演示→复现→失衡。staggerTicks 入档,Simon 进度瞬态不入档。
    private enum TunePhase { IDLE, DEMO, INPUT }
    private TunePhase tunePhase = TunePhase.IDLE;
    private final java.util.List<BlockPos> orderedPillars = new java.util.ArrayList<>();
    private int[] tuneSequence = new int[0];
    private int demoStep = 0;
    private int demoTimer = 0;
    private int inputProgress = 0;
    private int staggerTicks = 0;           // >0 = 失衡窗口(可被全额伤害)

    private int soundwaveCooldown = 100;
    private int chargeCooldown = 200;
    private int noteBoltCooldown = 50;
    private int chargeTicks = 0;            // >0 = 正在蓄力大音波

    public CrystalSongkeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        this.xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
        return this.staggerTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.crystal_songkeeper.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantCrystalSongkeeper(this, player, firstKill);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 失衡窗口倒计时:结束后重新受护
        if (this.staggerTicks > 0) {
            this.staggerTicks--;
            if (this.staggerTicks == 0) {
                this.broadcastHint(serverLevel, "message.unknown_echoes.crystal_songkeeper.guarded");
            }
        }
        if (this.soundwaveCooldown > 0) this.soundwaveCooldown--;
        if (this.chargeCooldown > 0) this.chargeCooldown--;
        if (this.noteBoltCooldown > 0) this.noteBoltCooldown--;

        // 节奏谜题:未失衡时驱动"演示→复现"循环(失衡期间暂停)
        if (this.staggerTicks <= 0) {
            this.tickTuneTrial(serverLevel);
        }

        if (this.chargeTicks > 0) {
            this.tickCharge(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // 大音波只在未失衡时开始(失衡时把节奏让给玩家输出)
        if (this.chargeCooldown <= 0 && this.staggerTicks <= 0) {
            this.startCharge(serverLevel);
        } else if (this.soundwaveCooldown <= 0 && this.distanceToSqr(target) <= 100.0) {
            this.castSoundwave(serverLevel);
        } else if (this.noteBoltCooldown <= 0 && this.hasLineOfSight(target)) {
            this.castNoteBolt(serverLevel, target);
        }
    }

    /** 音符弹:远程直击,沿弹道铺音符轨迹,命中造成远程伤害 + 缓速(补齐守谱者的远程手段)。 */
    private void castNoteBolt(ServerLevel serverLevel, LivingEntity target) {
        this.noteBoltCooldown = 80;
        this.triggerAnim("attack_controller", "attack");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_ATTACK.get(), 1.0F, 1.5F);
        double ex = this.getX(), ey = this.getEyeY(), ez = this.getZ();
        double tx = target.getX(), ty = target.getY() + target.getBbHeight() * 0.5, tz = target.getZ();
        int steps = 14;
        for (int i = 1; i <= steps; i++) {
            double f = i / (double) steps;
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    ex + (tx - ex) * f, ey + (ty - ey) * f, ez + (tz - ez) * f, 1, 0.05, 0.05, 0.05, 0.8);
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        target.hurt(this.damageSources().mobAttack(this), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
        BossFx.sparkSpray(serverLevel, this.position().add(0, this.getEyeHeight(), 0),
                target.position().subtract(this.position()), 6.0, ParticleTypes.END_ROD);
    }

    // ---- 节奏谜题(Simon)----

    /** 服务端 tick:场地内有玩家时跑"演示→复现"循环。 */
    private void tickTuneTrial(ServerLevel level) {
        if (this.tunePhase != TunePhase.IDLE && this.tickCount % 40 == 0 && !hasArenaPlayer(level)) {
            this.tunePhase = TunePhase.IDLE;
            this.inputProgress = 0;
            return;
        }
        switch (this.tunePhase) {
            case IDLE -> {
                if (this.tickCount % 20 == 0 && this.getTarget() != null && hasArenaPlayer(level)) {
                    startTuneRound(level);
                }
            }
            case DEMO -> tickDemo(level);
            case INPUT -> markTunedPillars(level); // 已调对的音柱保持微光,提示进度
        }
    }

    private void startTuneRound(ServerLevel level) {
        java.util.List<BlockPos> pillars = scanPillars(level);
        if (pillars.size() < 2) {
            this.tunePhase = TunePhase.IDLE;
            return;
        }
        BlockPos center = this.blockPosition();
        // 按绕守谱者的角度排序,演示顺时针可读
        pillars.sort(java.util.Comparator.comparingDouble(
                p -> Math.atan2(p.getZ() - center.getZ(), p.getX() - center.getX())));
        this.orderedPillars.clear();
        this.orderedPillars.addAll(pillars);
        int n = Math.min(SEQUENCE_LENGTH, this.orderedPillars.size());
        this.tuneSequence = shuffledIndices(n);
        this.demoStep = 0;
        this.demoTimer = 0;
        this.inputProgress = 0;
        this.tunePhase = TunePhase.DEMO;
        broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.tune_listen");
    }

    private void tickDemo(ServerLevel level) {
        if (this.tuneSequence.length == 0 || this.orderedPillars.isEmpty()) {
            this.tunePhase = TunePhase.IDLE;
            return;
        }
        if (this.demoStep >= this.tuneSequence.length) {
            this.tunePhase = TunePhase.INPUT;
            this.inputProgress = 0;
            broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.tune_repeat");
            return;
        }
        BlockPos pillar = this.orderedPillars.get(this.tuneSequence[this.demoStep]);
        if (this.demoTimer == 0) {
            litBeam(level, pillar, ParticleTypes.END_ROD, 28);
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_AMBIENT.get(), 1.0F, 0.7F + 0.12F * this.demoStep);
        } else if (this.demoTimer == DEMO_LIT_TICKS) {
            level.sendParticles(ParticleTypes.GLOW,
                    pillar.getX() + 0.5, pillar.getY() + 1.0, pillar.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
        }
        if (++this.demoTimer >= DEMO_STEP_TICKS) {
            this.demoTimer = 0;
            this.demoStep++;
        }
    }

    /** 玩家调音(右键音柱 / 谱杖音波命中音柱)时调用。 */
    public void onTonePillarClicked(BlockPos pos, ServerPlayer player) {
        if (this.level().isClientSide || this.staggerTicks > 0
                || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.participants.add(player.getUUID());
        if (this.tunePhase != TunePhase.INPUT) {
            // 还没到复现阶段:点柱即提示"先听演示"(IDLE + 有目标时顺便开一轮)
            if (this.tunePhase == TunePhase.IDLE && this.getTarget() != null) {
                startTuneRound(level);
            }
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.crystal_songkeeper.tune_listen"), true);
            return;
        }
        int idx = this.orderedPillars.indexOf(pos);
        if (idx < 0 || this.inputProgress >= this.tuneSequence.length) {
            return;
        }
        if (idx == this.tuneSequence[this.inputProgress]) {
            this.inputProgress++;
            litBeam(level, pos, ParticleTypes.NOTE, 16);
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_AMBIENT.get(),
                    1.1F, 0.8F + 0.12F * this.inputProgress);
            if (this.inputProgress >= this.tuneSequence.length) {
                enterStagger(level);
            }
        } else {
            // 调错:重演
            this.inputProgress = 0;
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_HURT.get(), 1.0F, 0.6F);
            broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.tune_wrong");
            this.demoStep = 0;
            this.demoTimer = 0;
            this.tunePhase = TunePhase.DEMO;
        }
    }

    /** 完成调音 → 失衡窗口(全额伤害)+ 打断大音波。 */
    private void enterStagger(ServerLevel level) {
        this.staggerTicks = STAGGER_TICKS;
        this.tunePhase = TunePhase.IDLE;
        this.inputProgress = 0;
        this.chargeTicks = 0; // 打断大音波
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_HURT.get(), 1.4F, 1.3F);
        BossFx.impactBurst(level, this.position().add(0, 1.2, 0), ParticleTypes.END_ROD);
        broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.staggered");
    }

    /** 复现阶段:已调对的音柱每 10 tick 冒一颗音符,提示当前进度。 */
    private void markTunedPillars(ServerLevel level) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        for (int i = 0; i < this.inputProgress && i < this.tuneSequence.length; i++) {
            BlockPos pillar = this.orderedPillars.get(this.tuneSequence[i]);
            level.sendParticles(ParticleTypes.NOTE,
                    pillar.getX() + 0.5, pillar.getY() + 1.2, pillar.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.6);
        }
    }

    /** 音柱发光:从柱顶向上打一道粒子光束,远处可读。 */
    private void litBeam(ServerLevel level, BlockPos pillar,
                         net.minecraft.core.particles.SimpleParticleType particle, int count) {
        for (int i = 0; i < count; i++) {
            level.sendParticles(particle,
                    pillar.getX() + 0.5, pillar.getY() + 1.0 + i * 0.18, pillar.getZ() + 0.5,
                    1, 0.06, 0.0, 0.06, 0.0);
        }
        level.sendParticles(ParticleTypes.GLOW,
                pillar.getX() + 0.5, pillar.getY() + 1.0, pillar.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
    }

    private boolean hasArenaPlayer(ServerLevel level) {
        double radius = settlementRadius();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && p.distanceToSqr(this) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<BlockPos> scanPillars(ServerLevel level) {
        java.util.List<BlockPos> result = new java.util.ArrayList<>();
        BlockPos anchor = this.arenaAnchor();
        for (BlockPos pos : BlockPos.betweenClosed(
                anchor.offset(-PILLAR_SCAN_RADIUS, -4, -PILLAR_SCAN_RADIUS),
                anchor.offset(PILLAR_SCAN_RADIUS, PILLAR_SCAN_HEIGHT, PILLAR_SCAN_RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    /** 0..size-1 的乱序;演示会公开顺序,用实体随机即可。 */
    private int[] shuffledIndices(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        for (int i = size - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return arr;
    }

    /** 音波环:贴身扩散环 AoE,击退 + 低伤。 */
    private void castSoundwave(ServerLevel serverLevel) {
        this.soundwaveCooldown = 140;
        this.triggerAnim("attack_controller", "soundwave");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_ATTACK.get(), 1.3F, 0.9F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(5.0, 2.0, 5.0),
                e -> e != this && !(e instanceof Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            victim.knockback(0.6D, -dx, -dz);
        }
        for (int ring = 1; ring <= 5; ring++) {
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        this.getX() + Math.cos(a) * ring, this.getY() + 1.0, this.getZ() + Math.sin(a) * ring,
                        1, 0.0, 0.0, 0.0, 1.0);
            }
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.8, 0), 5.0, ParticleTypes.NOTE);
    }

    /** 共振充能:开始蓄力大音波(可被敲音柱打断)。 */
    private void startCharge(ServerLevel serverLevel) {
        this.chargeTicks = CHARGE_TICKS;
        this.chargeCooldown = 360;
        this.triggerAnim("attack_controller", "charge");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_AMBIENT.get(), 1.6F, 0.5F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.crystal_songkeeper.charging");
    }

    private void tickCharge(ServerLevel serverLevel) {
        // 充能演出:头顶共振音环聚拢
        BossFx.chargeSpiral(serverLevel, this.position().add(0, 0.8, 0), this.tickCount, ParticleTypes.INSTANT_EFFECT);
        double spin = this.chargeTicks * 0.3;
        for (int i = 0; i < 8; i++) {
            double a = spin + Math.PI * 2 * i / 8;
            double r = 0.5 + 2.5 * this.chargeTicks / CHARGE_TICKS;
            serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT,
                    this.getX() + Math.cos(a) * r, this.getY() + 2.2, this.getZ() + Math.sin(a) * r,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        // 完成调音(失衡)会直接清零 chargeTicks 打断;此处只推进读条
        if (--this.chargeTicks <= 0) {
            this.castBigWave(serverLevel);
        }
    }

    /** 大音波:覆盖全场的重伤波 + 减速。 */
    private void castBigWave(ServerLevel serverLevel) {
        this.triggerAnim("attack_controller", "bigwave");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_ATTACK.get(), 2.0F, 0.6F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(ARENA_RADIUS, 4.0, ARENA_RADIUS),
                e -> e != this && !(e instanceof Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
        }
        for (int ring = 1; ring <= ARENA_RADIUS; ring += 1) {
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                        this.getX() + Math.cos(a) * ring, this.getY() + 1.0, this.getZ() + Math.sin(a) * ring,
                        0, 0.0, 0.0, 0.0, 0.0);
            }
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.8, 0), 8.0, ParticleTypes.SONIC_BOOM);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.2, 0), ParticleTypes.END_ROD);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_ATTACK.get(), 1.1F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.NOTE);
            }
        }
        return hit;
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRYSTAL_SONGKEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRYSTAL_SONGKEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRYSTAL_SONGKEEPER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChargeTicks", this.chargeTicks);
        tag.putInt("StaggerTicks", this.staggerTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.chargeTicks = tag.getInt("ChargeTicks");
        this.staggerTicks = tag.getInt("StaggerTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("soundwave", SOUNDWAVE)
                .triggerableAnim("charge", CHARGE)
                .triggerableAnim("bigwave", BIGWAVE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
