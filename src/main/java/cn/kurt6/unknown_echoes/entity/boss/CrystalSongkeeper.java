package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CrystalSongkeeper extends MiniBossEntity {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("crystal_songkeeper");
    private static final int ARENA_RADIUS = 14;
    private static final int CHARGE_TICKS = 70;
    /** 完成调音后的失衡窗口（tick）。 */
    private static final int STAGGER_TICKS = 160;
    /** 声波近身判定半径。演出环画在这个值上，改了要同步 castSoundwave 的两处引用。 */
    private static final double SOUNDWAVE_RADIUS = 5.0D;
    /** 蓄力汇聚环的起始半径。纯表现，与任何判定无关。 */
    private static final double CHARGE_CONVERGE_RADIUS = 3.0D;
    /** 调音音柱搜索范围(覆盖整座大乐厅,半径 14)。 */
    private static final int PILLAR_SCAN_RADIUS = 14;
    private static final int PILLAR_SCAN_HEIGHT = 8;
    /** 节奏演示:单根亮持续 / 单步总时长(亮 + 间隔)。 */
    private static final int DEMO_LIT_TICKS = 14;
    private static final int DEMO_STEP_TICKS = 22;
    /** 演示与输入回馈的光柱高度(格)。演示柱更高，让"听"和"答"能分辨。 */
    private static final double DEMO_BEAM_HEIGHT = 5.0D;
    private static final double INPUT_BEAM_HEIGHT = 2.9D;
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

    private enum TunePhase { IDLE, DEMO, INPUT }
    private TunePhase tunePhase = TunePhase.IDLE;
    private final java.util.List<BlockPos> orderedPillars = new java.util.ArrayList<>();
    private int[] tuneSequence = new int[0];
    private int demoStep = 0;
    private int demoTimer = 0;
    private int inputProgress = 0;
    /** 音柱不足时的重扫退避(tick)。 */
    private int pillarRescanCooldown = 0;
    private int staggerTicks = 0;

    private int soundwaveCooldown = 100;
    private int chargeCooldown = 200;
    private int noteBoltCooldown = 50;
    private int chargeTicks = 0;

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
    protected void tickMechanicGuidance(ServerLevel level) {
        if (this.tunePhase != TunePhase.INPUT) {
            return;
        }
        Vec3 core = this.position().add(0, 1.2, 0);
        long age = level.getGameTime();
        for (int index : this.tuneSequence) {
            if (index < 0 || index >= this.orderedPillars.size()) {
                continue;
            }
            Vec3 pillar = Vec3.atCenterOf(this.orderedPillars.get(index)).add(0, 0.7, 0);
            MiniBossFx.objectiveMarker(level, pillar, this.fxTheme(), age);
            MiniBossFx.objectiveLink(level, pillar, core, this.fxTheme(), age);
        }
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantCrystalSongkeeper(this, firstKill);
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

        if (this.staggerTicks > 0) {
            this.staggerTicks--;
            if (this.staggerTicks == 0) {
                this.broadcastHint(serverLevel, "message.unknown_echoes.crystal_songkeeper.guarded");
            }
        }
        if (this.soundwaveCooldown > 0) this.soundwaveCooldown--;
        if (this.chargeCooldown > 0) this.chargeCooldown--;
        if (this.noteBoltCooldown > 0) this.noteBoltCooldown--;

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

        if (this.chargeCooldown <= 0 && this.staggerTicks <= 0) {
            this.startCharge(serverLevel);
        } else if (this.soundwaveCooldown <= 0 && this.distanceToSqr(target) <= 100.0) {
            this.castSoundwave(serverLevel);
        } else if (this.noteBoltCooldown <= 0 && this.hasLineOfSight(target)) {
            this.castNoteBolt(serverLevel, target);
        }
    }

    private void castNoteBolt(ServerLevel serverLevel, LivingEntity target) {
        this.noteBoltCooldown = 80;
        this.triggerAnim("attack_controller", "attack");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_NOTE_BOLT.get(), 1.0F, 1.0F);
        Vec3 origin = this.position().add(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 waist = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 delta = waist.subtract(origin);
        MiniBossFx.pierceBeam(serverLevel, origin, delta, this.fxTheme(), delta.length());
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        if (target.hurt(this.damageSources().mobAttack(this), damage)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
            MiniBossFx.hitMark(serverLevel, target, this.fxTheme());
        }
        BossFx.cinematicImpact(serverLevel, waist, 18.0D, 0.22F, 4,
                this.combatImpactColor());
    }

    private void tickTuneTrial(ServerLevel level) {
        if (this.tunePhase != TunePhase.IDLE && this.tickCount % 40 == 0 && !hasArenaPlayer(level)) {
            this.tunePhase = TunePhase.IDLE;
            this.inputProgress = 0;
            return;
        }
        switch (this.tunePhase) {
            case IDLE -> {
                if (this.pillarRescanCooldown > 0) {
                    this.pillarRescanCooldown--;
                } else if (this.tickCount % 20 == 0 && this.getTarget() != null && hasArenaPlayer(level)) {
                    startTuneRound(level);
                }
            }
            case DEMO -> tickDemo(level);
            case INPUT -> markTunedPillars(level);
        }
    }

    private void startTuneRound(ServerLevel level) {
        if (!isScanAreaLoaded(level, this.arenaAnchor(), PILLAR_SCAN_RADIUS)) {
            return;
        }
        java.util.List<BlockPos> pillars = scanPillars(level);
        if (pillars.size() < 2) {

            this.tunePhase = TunePhase.IDLE;
            this.pillarRescanCooldown = 600;
            return;
        }
        BlockPos center = this.blockPosition();

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
            MiniBossFx.pillarBeam(level, pillarTop(pillar), this.fxTheme(), DEMO_BEAM_HEIGHT);
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_TUNE_DEMO.get(), 1.0F, 0.7F + 0.12F * this.demoStep);
        } else if (this.demoTimer == DEMO_LIT_TICKS) {
            MiniBossFx.solvedMark(level, pillarTop(pillar), this.fxTheme());
        }
        if (++this.demoTimer >= DEMO_STEP_TICKS) {
            this.demoTimer = 0;
            this.demoStep++;
        }
    }

    public void onTonePillarClicked(BlockPos pos, ServerPlayer player) {
        if (this.level().isClientSide || this.staggerTicks > 0
                || !(this.level() instanceof ServerLevel level)) {
            return;
        }
            BossNbtData.addParticipant(this.participants, player.getUUID());
        if (this.tunePhase != TunePhase.INPUT) {

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
            MiniBossFx.pillarBeam(level, pillarTop(pos), this.fxTheme(), INPUT_BEAM_HEIGHT);
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_TUNE_INPUT.get(),
                    1.1F, 0.8F + 0.12F * this.inputProgress);
            if (this.inputProgress >= this.tuneSequence.length) {
                enterStagger(level);
            }
        } else {

            this.inputProgress = 0;
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_TUNE_FAIL.get(), 1.0F, 1.0F);
            broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.tune_wrong");
            this.demoStep = 0;
            this.demoTimer = 0;
            this.tunePhase = TunePhase.DEMO;
        }
    }

    private void enterStagger(ServerLevel level) {
        this.staggerTicks = STAGGER_TICKS;
        this.tunePhase = TunePhase.IDLE;
        this.inputProgress = 0;
        this.chargeTicks = 0;
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_STAGGER.get(), 1.4F, 1.0F);
        MiniBossFx.groundRing(level, this.position(), this.fxTheme(), SOUNDWAVE_RADIUS);
        BossFx.cinematicImpact(level, this.position().add(0, 1.0, 0),
                28.0D, 0.6F, 8, this.combatImpactColor());
        broadcastHint(level, "message.unknown_echoes.crystal_songkeeper.staggered");
    }

    private void markTunedPillars(ServerLevel level) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        for (int i = 0; i < this.inputProgress && i < this.tuneSequence.length; i++) {
            BlockPos pillar = this.orderedPillars.get(this.tuneSequence[i]);
            MiniBossFx.solvedMark(level, pillarTop(pillar), this.fxTheme());
        }
    }

    /** 音柱顶面中心。演出一律从这里起算，不要各处自己拼 +0.5/+1.0。 */
    private static Vec3 pillarTop(BlockPos pillar) {
        return new Vec3(pillar.getX() + 0.5D, pillar.getY() + 1.0D, pillar.getZ() + 0.5D);
    }

    private boolean hasArenaPlayer(ServerLevel level) {
        double radius = settlementRadius();
        return !level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                player -> !player.isSpectator() && player.isAlive()
                        && player.distanceToSqr(this) <= radius * radius).isEmpty();
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

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, radius);
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

    private void castSoundwave(ServerLevel serverLevel) {
        this.soundwaveCooldown = 140;
        this.triggerAnim("attack_controller", "soundwave");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_WAVE.get(), 1.3F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SOUNDWAVE_RADIUS, 2.0, SOUNDWAVE_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > SOUNDWAVE_RADIUS * SOUNDWAVE_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                double dx = victim.getX() - this.getX();
                double dz = victim.getZ() - this.getZ();
                victim.knockback(0.6D, -dx, -dz);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), SOUNDWAVE_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                24.0D, 0.42F, 6, this.combatImpactColor());
    }

    private void startCharge(ServerLevel serverLevel) {
        this.chargeTicks = CHARGE_TICKS;
        this.chargeCooldown = 360;
        this.triggerAnim("attack_controller", "charge");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_CHARGE.get(), 1.6F, 1.0F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.crystal_songkeeper.charging");
    }

    private void tickCharge(ServerLevel serverLevel) {
        double progress = 1.0D - this.chargeTicks / (double) CHARGE_TICKS;
        MiniBossFx.chargeConverge(serverLevel, this.position(), this.fxTheme(),
                CHARGE_CONVERGE_RADIUS, progress);
        if (--this.chargeTicks <= 0) {
            this.castBigWave(serverLevel);
        }
    }

    private void castBigWave(ServerLevel serverLevel) {
        this.triggerAnim("attack_controller", "bigwave");
        this.playSound(ModSounds.CRYSTAL_SONGKEEPER_BIG_WAVE.get(), 2.0F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(ARENA_RADIUS, 4.0, ARENA_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > ARENA_RADIUS * ARENA_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
        // 环画在结算半径上：画小了玩家会站在环外仍然吃满伤害。
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), ARENA_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                36.0D, 0.86F, 11, this.combatImpactColor());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            this.playSound(ModSounds.CRYSTAL_SONGKEEPER_ATTACK.get(), 1.1F, 1.0F);
        }
        return hit;
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                candidate -> candidate.distanceToSqr(this) <= radius * radius)) {
            player.displayClientMessage(Component.translatable(key), true);
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
        this.chargeTicks = BossNbtData.clampTimer(tag.getInt("ChargeTicks"), CHARGE_TICKS);
        this.staggerTicks = BossNbtData.clampTimer(tag.getInt("StaggerTicks"), STAGGER_TICKS);
        if (this.staggerTicks > 0) {
            this.chargeTicks = 0;
        }
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
