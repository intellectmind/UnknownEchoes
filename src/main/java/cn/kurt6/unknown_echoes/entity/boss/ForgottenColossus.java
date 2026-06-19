package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

/**
 * 遗忘巨像:V0.1 核心 Boss。
 *
 * 阶段一(封锁):受到的伤害 = min(伤害 x 5%, 上限 4.0),防高伤害 Mod 秒杀。
 * 阶段二(破防):4 根记忆柱全部激活后持续 20 秒,受到正常伤害;超时未死重新封锁并重置记忆柱。
 * 阶段三(死亡):场地范围内所有参与玩家写入击败记录;风之回响由祭坛吸收本人信物后解锁。
 *
 * 模型与动画由 GeckoLib 驱动(Blockbench 制作,见 model/forgotten_colossus.bbmodel)。
 */
public class ForgottenColossus extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.forgotten_colossus.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.forgotten_colossus.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.forgotten_colossus.broken");
    private static final RawAnimation SLAM_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STOMP_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.stomp", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BEAM_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.beam", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(ForgottenColossus.class, EntityDataSerializers.BOOLEAN);

    private static final int PILLAR_SCAN_RADIUS = 24;
    private static final int PILLAR_SCAN_HEIGHT = 8;
    private static final int REQUIRED_PILLARS = 4;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int brokenTicksRemaining = 0;
    private float brokenWindowHealthFloor = 0.0F;
    private int protectedMessageCooldown = 0;
    private int outOfCombatTicks = 0;
    private final List<BlockPos> activePillars = new ArrayList<>();
    /** 参与玩家(伤害过 Boss / 激活过记忆柱 / 战斗期间在场),奖励只发给死亡时仍在场地内的参与者。 */
    private final Set<UUID> participants = new HashSet<>();

    // ---- Simon 序列回响:演示一段记忆柱顺序→玩家按序复现→破防(替代旧"点满 4 柱即破防")----
    // 状态为瞬态(不进 NBT):重载后回到 IDLE,记忆柱亮灭由方块状态自然持久化。
    private enum SimonPhase { IDLE, DEMO, INPUT }
    private static final int SIMON_LIT_TICKS = 12;   // 演示单根亮持续
    private static final int SIMON_STEP_TICKS = 20;  // 演示单步总时长(亮 + 间隔)
    private static final double SIMON_HINT_TOP_OFFSET = 0.95D;
    private SimonPhase simonPhase = SimonPhase.IDLE;
    private final List<BlockPos> orderedPillars = new ArrayList<>();
    private int[] simonSequence = new int[0];
    private int simonDemoStep = 0;
    private int simonDemoTimer = 0;
    private int simonInputProgress = 0;

    // ---- 特殊攻击(蓄力挥砸 slam / 重踏震波 stomp / 记忆碎石雨 rockrain / 核心光束 beam):动画先行,伤害延迟到落点帧 ----
    private static final int SLAM_HIT_DELAY = 17;    // 0.85s,对应动画砸地帧
    private static final int STOMP_HIT_DELAY = 11;   // 0.55s,对应动画踏地帧
    private static final int ROCK_RAIN_WARN_TICKS = 24;  // 1.2s,落点尘土预警
    private static final int BEAM_WINDUP_TICKS = 20;     // 1.0s,光束蓄力线
    private int slamCooldown = 0;
    private int stompCooldown = 0;
    private int rockRainCooldown = 0;
    private int beamCooldown = 0;
    private int pendingSlamTicks = 0;
    private int pendingStompTicks = 0;
    private int pendingRockRainTicks = 0;
    private int pendingBeamTicks = 0;
    /** 记忆碎石雨锁定的落点(预警期间持续显示尘土)。 */
    private final List<BlockPos> rockRainTargets = new ArrayList<>();
    /** 核心光束方向(蓄力开始时锁定)。 */
    private net.minecraft.world.phys.Vec3 beamDirection = net.minecraft.world.phys.Vec3.ZERO;
    /** 石臂横扫方向(前摇开始时锁定——预警线指哪打哪,前摇期间不再追踪转身)。 */
    private net.minecraft.world.phys.Vec3 slamDirection = net.minecraft.world.phys.Vec3.ZERO;
    /** 崩解狂怒(低于 30% 血)进入演出只放一次(NBT 持久化防重载重复播报)。 */
    private boolean enrageAnnounced = false;
    /** 沉睡唤醒:首次锁定目标时的苏醒演出只放一次(NBT 持久化)。 */
    private boolean awakened = false;
    /** 已按多少参与者完成血量扩容(只升不降,NBT 持久化防重载重复回血)。 */
    private int scaledParticipants = 1;
    private static final double BASE_MAX_HEALTH = 520.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 160.0D;

    public ForgottenColossus(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 520.0D)
                .add(Attributes.ATTACK_DAMAGE, 19.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 11.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
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
        builder.define(DATA_BROKEN, false);
    }

    public boolean isBroken() {
        return this.entityData.get(DATA_BROKEN);
    }

    private void setBroken(boolean broken) {
        this.entityData.set(DATA_BROKEN, broken);
    }

    /**
     * 记忆柱被点击时由 MemoryPillarBlock 调用(Simon 序列回响的"复现"输入)。
     * 序列只存服务端字段;演示通过记忆柱亮灭表现(本就该被看见),不泄露任何隐藏数据。
     */
    public void onMemoryPillarClicked(BlockPos pos, Player activator) {
        if (this.level().isClientSide || this.isBroken()) {
            return;
        }
        this.participants.add(activator.getUUID());
        // 还没进入"复现"阶段:点柱即开启/等待本轮演示,给含蓄提示
        if (this.simonPhase != SimonPhase.INPUT) {
            if (this.simonPhase == SimonPhase.IDLE) {
                startSimonRound();
            }
            activator.displayClientMessage(
                    Component.translatable("message.unknown_echoes.colossus.simon_listen"), true);
            return;
        }
        int idx = this.orderedPillars.indexOf(pos);
        if (idx < 0 || this.simonInputProgress >= this.simonSequence.length) {
            return;
        }
        // 忽略对"已点亮(已答对)"柱的重复点击,避免误触判错
        BlockState clicked = this.level().getBlockState(pos);
        if (clicked.is(ModBlocks.MEMORY_PILLAR.get()) && clicked.getValue(MemoryPillarBlock.ACTIVATED)) {
            return;
        }
        if (idx == this.simonSequence[this.simonInputProgress]) {
            setPillarLit(pos, true);
            this.simonInputProgress++;
            this.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                    1.2F, 0.8F + 0.12F * this.simonInputProgress);
            if (this.simonInputProgress >= this.simonSequence.length) {
                enterBrokenState();
            }
        } else {
            // 顺序错乱:全部熄灭,重放演示
            allPillarsOff();
            this.simonInputProgress = 0;
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.HOSTILE, 1.0F, 0.6F);
            broadcastNearby(Component.translatable("message.unknown_echoes.colossus.simon_wrong"));
            beginDemo();
        }
    }

    /** 服务端 tick 驱动:场地内有玩家时跑"演示→复现"循环;破防期间暂停。 */
    private void tickSimonTrial() {
        if (this.isBroken()) {
            return;
        }
        // 场地清空一段时间则放弃本轮,熄灭记忆柱回到待机
        if (this.simonPhase != SimonPhase.IDLE && this.tickCount % 40 == 0 && !hasArenaParticipant()) {
            allPillarsOff();
            this.simonPhase = SimonPhase.IDLE;
            this.simonInputProgress = 0;
            return;
        }
        switch (this.simonPhase) {
            case IDLE -> {
                if (this.tickCount % 20 == 0 && hasArenaParticipant()) {
                    startSimonRound();
                }
            }
            case DEMO -> tickSimonDemo();
            case INPUT -> { /* 等待玩家右键记忆柱,由 onMemoryPillarClicked 推进 */ }
        }
    }

    private void startSimonRound() {
        List<BlockPos> pillars = scanAllPillars();
        if (pillars.size() < 2) {
            this.simonPhase = SimonPhase.IDLE;
            return;
        }
        BlockPos center = this.blockPosition();
        // 按绕 Boss 的角度排序,演示顺时针可读
        pillars.sort(java.util.Comparator.comparingDouble(
                p -> Math.atan2(p.getZ() - center.getZ(), p.getX() - center.getX())));
        this.orderedPillars.clear();
        this.orderedPillars.addAll(pillars);
        int n = Math.min(REQUIRED_PILLARS, this.orderedPillars.size());
        this.simonSequence = shuffledIndices(n);
        allPillarsOff();
        broadcastNearby(Component.translatable("message.unknown_echoes.colossus.simon_listen"));
        beginDemo();
    }

    private void beginDemo() {
        this.simonPhase = SimonPhase.DEMO;
        this.simonDemoStep = 0;
        this.simonDemoTimer = 0;
    }

    private void tickSimonDemo() {
        if (this.simonSequence.length == 0 || this.orderedPillars.isEmpty()) {
            this.simonPhase = SimonPhase.IDLE;
            return;
        }
        if (this.simonDemoStep >= this.simonSequence.length) {
            allPillarsOff();
            this.simonPhase = SimonPhase.INPUT;
            this.simonInputProgress = 0;
            broadcastNearby(Component.translatable("message.unknown_echoes.colossus.simon_repeat"));
            return;
        }
        BlockPos pillar = this.orderedPillars.get(this.simonSequence[this.simonDemoStep]);
        if (this.simonDemoTimer == 0) {
            setPillarLit(pillar, true);
            this.level().playSound(null, pillar, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                    1.2F, 0.8F + 0.12F * this.simonDemoStep);
        } else if (this.simonDemoTimer == SIMON_LIT_TICKS) {
            setPillarLit(pillar, false);
        }
        if (this.simonDemoTimer < SIMON_LIT_TICKS) {
            playSimonPillarHint(pillar, this.simonDemoTimer);
        }
        if (++this.simonDemoTimer >= SIMON_STEP_TICKS) {
            this.simonDemoTimer = 0;
            this.simonDemoStep++;
        }
    }

    /** 演示期的可读提示:当前记忆柱柱顶发光,并用短粒子束连向巨像核心。 */
    private void playSimonPillarHint(BlockPos pillar, int timer) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 top = Vec3.atCenterOf(pillar).add(0.0D, SIMON_HINT_TOP_OFFSET, 0.0D);
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                top.x, top.y, top.z, 4, 0.18D, 0.10D, 0.18D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                top.x, top.y + 0.12D, top.z, 2, 0.12D, 0.08D, 0.12D, 0.01D);
        if (timer % 2 == 0) {
            drawSimonHintBeam(serverLevel, top, core);
            double radius = 0.52D + 0.05D * Math.sin((this.tickCount + timer) * 0.45D);
            BossFx.ring(serverLevel, top, radius, 14, 0.02D, ParticleTypes.GLOW);
        }
    }

    private void drawSimonHintBeam(ServerLevel serverLevel, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(8, (int) (delta.length() * 1.4D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    /** Simon 全对后破防:沿用原破防窗口逻辑与演出(文档 10.5)。 */
    private void enterBrokenState() {
        this.activePillars.clear();
        this.activePillars.addAll(this.orderedPillars);
        this.setBroken(true);
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.70F, 0.35F);
        this.simonPhase = SimonPhase.IDLE;
        // 共鸣信物仪式(5.1.1):破防 = 机关完成,对场地内全部参与玩家写仪式记录并发风之信物
        // (红线 #5:多人不强制人人亲手复现)。
        if (this.level() instanceof ServerLevel ritualLevel) {
            double ritualRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer participant : ritualLevel.players()) {
                if (participant.distanceToSqr(this) <= ritualRadius * ritualRadius) {
                    this.participants.add(participant.getUUID());
                    cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                            participant, cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO);
                }
            }
        }
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.phys.Vec3 core = this.position().add(0, this.getBbHeight() * 0.65, 0);
            // 已点亮记忆柱柱顶风纹连线到核心
            for (BlockPos pillar : this.orderedPillars) {
                net.minecraft.world.phys.Vec3 top = net.minecraft.world.phys.Vec3.atCenterOf(pillar).add(0, 0.8, 0);
                net.minecraft.world.phys.Vec3 delta = core.subtract(top);
                int steps = Math.max(6, (int) (delta.length() * 1.2));
                for (int i = 0; i <= steps; i++) {
                    net.minecraft.world.phys.Vec3 p = top.add(delta.scale(i / (double) steps));
                    serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            // 胸口核心裂开,金/青回响裂纹迸发
            serverLevel.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z, 1, 0, 0, 0, 0);
            for (int ring = 1; ring <= 3; ring++) {
                double r = ring * 1.4;
                for (int i = 0; i < 12 + ring * 4; i++) {
                    double angle = Math.PI * 2 * i / (12 + ring * 4);
                    serverLevel.sendParticles(ParticleTypes.GLOW,
                            core.x + Math.cos(angle) * r, core.y, core.z + Math.sin(angle) * r,
                            1, 0.1, 0.2, 0.1, 0.05);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            core.x + Math.cos(angle) * r, core.y - 0.5, core.z + Math.sin(angle) * r,
                            1, 0.1, 0.2, 0.1, 0.08);
                }
            }
        }
        broadcastNearby(Component.translatable("message.unknown_echoes.boss.broken"));
    }

    private boolean hasArenaParticipant() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.isAlive() && player.distanceToSqr(this) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> scanAllPillars() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-PILLAR_SCAN_RADIUS, -PILLAR_SCAN_HEIGHT, -PILLAR_SCAN_RADIUS),
                center.offset(PILLAR_SCAN_RADIUS, PILLAR_SCAN_HEIGHT, PILLAR_SCAN_RADIUS))) {
            if (this.level().getBlockState(pos).is(ModBlocks.MEMORY_PILLAR.get())) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private void setPillarLit(BlockPos pos, boolean lit) {
        BlockState state = this.level().getBlockState(pos);
        if (state.is(ModBlocks.MEMORY_PILLAR.get())
                && state.getValue(MemoryPillarBlock.ACTIVATED) != lit) {
            this.level().setBlock(pos, state.setValue(MemoryPillarBlock.ACTIVATED, lit), 3);
        }
    }

    private void allPillarsOff() {
        for (BlockPos pillar : this.orderedPillars) {
            setPillarLit(pillar, false);
        }
    }

    /** 0..size-1 的乱序(Fisher-Yates);演示会公开顺序,故用实体随机即可。 */
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

    private void resetPillars() {
        for (BlockPos pos : this.activePillars) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(ModBlocks.MEMORY_PILLAR.get())) {
                this.level().setBlock(pos, state.setValue(MemoryPillarBlock.ACTIVATED, Boolean.FALSE), 3);
            }
        }
        this.activePillars.clear();
    }

    private void broadcastNearby(Component message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }

    /** 特殊攻击调度:近身重踏,中距蓄力挥砸,远距碎石雨;崩解狂怒(低血量)时低血光束 + 冷却缩短。 */
    private void tickSpecialAttacks() {
        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.stompCooldown > 0) this.stompCooldown--;
        if (this.rockRainCooldown > 0) this.rockRainCooldown--;
        if (this.beamCooldown > 0) this.beamCooldown--;
        if (this.pendingSlamTicks > 0) {
            this.spawnSlamWarning();
            if (--this.pendingSlamTicks == 0) {
                this.doSlamDamage();
            }
        }
        if (this.pendingStompTicks > 0) {
            this.spawnStompWarning();
            if (--this.pendingStompTicks == 0) {
                this.doStompDamage();
            }
        }
        if (this.pendingRockRainTicks > 0) {
            this.spawnRockRainWarning();
            if (--this.pendingRockRainTicks == 0) {
                this.doRockRainDamage();
            }
        }
        if (this.pendingBeamTicks > 0) {
            this.spawnBeamCharge();
            if (--this.pendingBeamTicks == 0) {
                this.doBeamDamage();
            }
        }
        if (!this.isAlive() || this.pendingSlamTicks > 0 || this.pendingStompTicks > 0
                || this.pendingRockRainTicks > 0 || this.pendingBeamTicks > 0) {
            return;
        }
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // 崩解狂怒:低血量时攻击频率提高(仍保留完整前摇),并解锁核心光束
        boolean enraged = this.getHealth() < this.getMaxHealth() * 0.3F;
        int cooldownScale = enraged ? 65 : 100;
        double distSq = this.distanceToSqr(target);
        if (this.stompCooldown <= 0 && distSq <= 4.5 * 4.5) {
            this.pendingStompTicks = STOMP_HIT_DELAY;
            this.stompCooldown = 160 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "stomp");
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.5F, 0.6F);
        } else if (this.slamCooldown <= 0 && distSq <= 7.0 * 7.0 && this.hasLineOfSight(target)) {
            // 石臂横扫:前摇开始即锁定方向,预警尘土线沿这个方向铺开,前摇期间不再追踪
            this.slamDirection = target.position().subtract(this.position())
                    .multiply(1, 0, 1).normalize();
            this.pendingSlamTicks = SLAM_HIT_DELAY;
            this.slamCooldown = 240 * cooldownScale / 100;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.triggerAnim("attack_controller", "slam");
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.5F, 0.8F);
        } else if (enraged && this.beamCooldown <= 0 && distSq <= 18.0 * 18.0
                && distSq > 6.0 * 6.0 && this.hasLineOfSight(target)) {
            // 核心光束:崩解狂怒下的直线光束,蓄力线提前显形
            this.beamDirection = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(this.beamOrigin()).normalize();
            this.pendingBeamTicks = BEAM_WINDUP_TICKS;
            this.beamCooldown = 280 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "beam");
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.5F);
        } else if (this.rockRainCooldown <= 0 && distSq <= 20.0 * 20.0 && distSq > 5.0 * 5.0) {
            // 记忆碎石雨:锁定目标周边落点并铺尘土预警,迫使玩家移动
            this.lockRockRainTargets(target);
            if (!this.rockRainTargets.isEmpty()) {
                this.pendingRockRainTicks = ROCK_RAIN_WARN_TICKS;
                this.rockRainCooldown = 300 * cooldownScale / 100;
                this.triggerAnim("attack_controller", "slam");
                this.playSound(ModSounds.COLOSSUS_AMBIENT.get(), 1.6F, 0.5F);
            }
        }
    }

    private net.minecraft.world.phys.Vec3 beamOrigin() {
        return this.position().add(0, this.getBbHeight() * 0.65, 0);
    }

    /** 记忆碎石雨:在目标周围锁定 4 个地面落点(吸附地表,玩家跳跃不会把落点锁到空中)。 */
    private void lockRockRainTargets(net.minecraft.world.entity.LivingEntity target) {
        this.rockRainTargets.clear();
        for (int i = 0; i < 4; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = this.random.nextDouble() * 4.0;
            BlockPos ground = this.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    BlockPos.containing(
                            target.getX() + Math.cos(angle) * dist,
                            target.getY(),
                            target.getZ() + Math.sin(angle) * dist));
            this.rockRainTargets.add(ground);
        }
    }

    /** 碎石雨预警:落点中心尘土柱 + 2 格危险圈轮廓(文档:落点出现尘土粒子)。 */
    private void spawnRockRainWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (BlockPos pos : this.rockRainTargets) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.01);
            // 危险圈轮廓:沿半径 2 的圆周转圈点亮,读得出实际判定范围
            double sweep = this.tickCount * 0.7;
            for (int k = 0; k < 3; k++) {
                double angle = sweep + k * Math.PI * 2 / 3;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5 + Math.cos(angle) * 2.0, pos.getY() + 0.15,
                        pos.getZ() + 0.5 + Math.sin(angle) * 2.0, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5, 3, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    /** 碎石雨落地:每个落点 2 格半径重击 + 碎石飞溅。 */
    private void doRockRainDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        BlockState deepslate = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
        for (BlockPos pos : this.rockRainTargets) {
            for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2.0, 2.0, 2.0),
                    e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
            }
            serverLevel.sendParticles(
                    new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, deepslate),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 1.0, 0.5, 1.0, 0.1);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.6F);
        this.rockRainTargets.clear();
    }

    /** 核心光束蓄力:胸口核心亮起,蓄力线由根部逐渐向外延伸——延伸到头即开火,蓄力进度可读。 */
    private void spawnBeamCharge() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 2 != 0) {
            return;
        }
        net.minecraft.world.phys.Vec3 from = this.beamOrigin();
        double progress = 1.0 - this.pendingBeamTicks / (double) BEAM_WINDUP_TICKS;
        int maxD = Math.max(2, (int) (18 * progress));
        for (int d = 1; d <= maxD; d += 2) {
            net.minecraft.world.phys.Vec3 p = from.add(this.beamDirection.scale(d));
            serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                from.x, from.y, from.z, 3, 0.2, 0.2, 0.2, 0.02);
        BossFx.chargeSpiral(serverLevel, from, this.tickCount, ParticleTypes.END_ROD);
    }

    /** 核心光束命中:直线走廊伤害;命中记忆柱即被阻挡(柱后是安全位)。 */
    private void doBeamDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        net.minecraft.world.phys.Vec3 from = this.beamOrigin();
        // 光束被记忆柱阻挡:沿线找第一根记忆柱,截断射程
        double reach = 18.0;
        for (double d = 1.0; d <= 18.0; d += 0.5) {
            BlockPos pos = BlockPos.containing(from.add(this.beamDirection.scale(d)));
            if (this.level().getBlockState(pos).is(ModBlocks.MEMORY_PILLAR.get())) {
                reach = d;
                break;
            }
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(18.0, 4.0, 18.0),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            net.minecraft.world.phys.Vec3 toVictim = victim.position()
                    .add(0, victim.getBbHeight() * 0.5, 0).subtract(from);
            double along = toVictim.dot(this.beamDirection);
            if (along < 0 || along > reach) {
                continue;
            }
            if (toVictim.subtract(this.beamDirection.scale(along)).length() > 1.5) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
        }
        // 光束演出:密集光柱 + 终点迸发
        for (double d = 0.5; d <= reach; d += 0.5) {
            net.minecraft.world.phys.Vec3 p = from.add(this.beamDirection.scale(d));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        BossFx.sparkSpray(serverLevel, from, this.beamDirection, reach, ParticleTypes.END_ROD);
        net.minecraft.world.phys.Vec3 end = from.add(this.beamDirection.scale(reach));
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 16, 0.5, 0.5, 0.5, 0.15);
        BossFx.impactBurst(serverLevel, end, ParticleTypes.END_ROD);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    /** 多人扩容:参与者每多一人,血量上限 +150 并补上同量血(只升不降)。 */
    private void updateDynamicScaling() {
        int count = Math.max(1, this.participants.size());
        if (count <= this.scaledParticipants) {
            return;
        }
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(BASE_MAX_HEALTH + HEALTH_PER_EXTRA_PLAYER * (count - 1));
            this.heal((float) (HEALTH_PER_EXTRA_PLAYER * (count - this.scaledParticipants)));
        }
        this.scaledParticipants = count;
    }

    /** 重踏预警:风压环由外(6 格判定边界)向内收缩——环收到脚下即震地(文档:风压环)。 */
    private void spawnStompWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double r = Math.max(1.0, 6.0 * this.pendingStompTicks / (double) STOMP_HIT_DELAY);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12 + this.tickCount * 0.2;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + Math.cos(angle) * r, this.getY() + 0.15,
                    this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.02, 0.05, 0.0);
        }
    }

    /** 重踏震波:以巨像为中心的 AOE,伤害较低但击退极强。 */
    private void doStompDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(6.0D, 2.0D, 6.0D),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            net.minecraft.world.phys.Vec3 away = victim.position().subtract(this.position())
                    .multiply(1, 0, 1).normalize();
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(away.x * 1.4, 0.6, away.z * 1.4));
            victim.hurtMarked = true;
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.COLOSSUS_STEP.get(), SoundSource.HOSTILE, 2.0F, 0.5F);
        // 震波演出:中心风爆 + 外扩尘浪 + 碎石飞溅("震波"读法,替代原先一圈爆炸)
        serverLevel.sendParticles(ParticleTypes.GUST,
                this.getX(), this.getY() + 0.3, this.getZ(), 1, 0, 0, 0, 0);
        BlockState stompDebris = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + Math.cos(angle) * 3.0, this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * 3.0, 2,
                    Math.cos(angle) * 0.8, 0.05, Math.sin(angle) * 0.8, 0.12);
        }
        serverLevel.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, stompDebris),
                this.getX(), this.getY() + 0.4, this.getZ(), 24, 2.5, 0.3, 2.5, 0.1);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), 6.0, ParticleTypes.CLOUD);
        BossFx.impactBurst(serverLevel, this.position().add(0, 0.3, 0), ParticleTypes.CRIT);
    }

    /** 横扫预警:沿锁定方向铺设地面尘土线 + 扇形两翼边界线(文档:攻击方向提前显示尘土线/地面裂纹)。 */
    private void spawnSlamWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.slamDirection.lengthSqr() < 1.0E-4) {
            return;
        }
        BlockState slamDust = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
        // 中线 + 两翼(±55°,对应命中判定的正面扇形边界)
        for (int wing = -1; wing <= 1; wing++) {
            double rot = wing * Math.toRadians(55);
            double cos = Math.cos(rot), sin = Math.sin(rot);
            double dx = this.slamDirection.x * cos - this.slamDirection.z * sin;
            double dz = this.slamDirection.x * sin + this.slamDirection.z * cos;
            for (int d = 2; d <= 6; d += 2) {
                serverLevel.sendParticles(
                        new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, slamDust),
                        this.getX() + dx * d, this.getY() + 0.15, this.getZ() + dz * d,
                        1, 0.2, 0.05, 0.2, 0.02);
            }
        }
    }

    /** 蓄力挥砸(石臂横扫):锁定方向正面约 110° 扇形、6.5 格内的重击,伤害高。 */
    private void doSlamDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)
                || this.slamDirection.lengthSqr() < 1.0E-4) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
        net.minecraft.world.phys.Vec3 facing = this.slamDirection;
        for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(6.0D, 2.0D, 6.0D),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            net.minecraft.world.phys.Vec3 toVictim = victim.position().subtract(this.position())
                    .multiply(1, 0, 1);
            if (toVictim.length() > 6.5) {
                continue;       // 与预警线长度一致:超出尘土线尽头就是安全的
            }
            if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.5) {
                continue;       // 不在正面扇形内
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(facing.x * 0.8, 0.3, facing.z * 0.8));
            victim.hurtMarked = true;
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.6F, 0.7F);
        // 砸击尘浪:沿锁定方向的地面粒子带
        for (int d = 1; d <= 5; d++) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + facing.x * d, this.getY() + 0.3, this.getZ() + facing.z * d,
                    4, 0.6, 0.1, 0.6, 0.02);
        }
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.4, 0), facing, 6.5, ParticleTypes.CRIT);
        BossFx.impactBurst(serverLevel, this.position().add(facing.scale(3.0)).add(0, 0.4, 0), ParticleTypes.CRIT);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        // 沉睡唤醒:首次锁定目标时的苏醒演出(石尘 + 低频回响波,只放一次)
        if (!this.awakened && this.getTarget() != null
                && this.level() instanceof ServerLevel serverLevel) {
            this.awakened = true;
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.6F);
            BlockState deepslate = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
            for (int ring = 1; ring <= 4; ring++) {
                double r = ring * 1.6;
                for (int i = 0; i < 14; i++) {
                    double angle = Math.PI * 2 * i / 14;
                    serverLevel.sendParticles(
                            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, deepslate),
                            this.getX() + Math.cos(angle) * r, this.getY() + 0.3,
                            this.getZ() + Math.sin(angle) * r, 4, 0.2, 0.3, 0.2, 0.05);
                }
            }
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 1, 0, 0, 0, 0);
            broadcastNearby(Component.translatable("message.unknown_echoes.boss.awakened"));
        }
        if (this.protectedMessageCooldown > 0) {
            this.protectedMessageCooldown--;
        }
        // 崩解狂怒进入演出:低于 30% 血时一次性播报(冷却缩短 + 解锁核心光束的节奏变化要让玩家读到)
        if (this.isAlive() && !this.enrageAnnounced
                && this.getHealth() < this.getMaxHealth() * 0.3F
                && this.level() instanceof ServerLevel serverLevel) {
            this.enrageAnnounced = true;
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.5F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    40, this.getBbWidth() * 0.7, this.getBbHeight() * 0.4, this.getBbWidth() * 0.7, 0.15);
            broadcastNearby(Component.translatable("message.unknown_echoes.boss.enraged"));
        }
        // 状态粒子反馈:封锁时灵魂之火环绕,破防时端烛光迸发;狂怒期体表追加裂纹火花
        if (this.isAlive() && this.tickCount % 10 == 0 && this.level() instanceof ServerLevel serverLevel) {
            if (this.isBroken()) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                        6, this.getBbWidth() * 0.6, this.getBbHeight() * 0.4, this.getBbWidth() * 0.6, 0.02);
            } else {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        4, this.getBbWidth() * 0.7, this.getBbHeight() * 0.45, this.getBbWidth() * 0.7, 0.01);
            }
            if (this.enrageAnnounced) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        3, this.getBbWidth() * 0.5, this.getBbHeight() * 0.35, this.getBbWidth() * 0.5, 0.06);
            }
        }
        if (this.isBroken()) {
            this.brokenTicksRemaining--;
            if (this.brokenTicksRemaining <= 0 && this.isAlive()) {
                restoreMemoryGuard();
            }
        }
        // 战斗期间(有目标时)周期登记场地内玩家为参与者
        if (this.tickCount % 20 == 0 && this.getTarget() != null
                && this.level() instanceof ServerLevel serverLevel) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    this.participants.add(player.getUUID());
                }
            }
            if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                this.updateDynamicScaling();
            }
        }
        this.tickSimonTrial();
        this.tickSpecialAttacks();
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** 脱战回血:无存活目标持续 5 秒后,每秒回复 2% 最大生命(减员消耗战不可行,脱战即重置)。 */
    private void tickOutOfCombatRegen() {
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.outOfCombatTicks = 0;
            return;
        }
        if (++this.outOfCombatTicks > 100 && this.outOfCombatTicks % 20 == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.02F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 巨像只回应玩家:小怪(含其他敌对生物)的攻击无效,也避免误伤后的仇恨混战
        if (!this.level().isClientSide
                && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker) {
            this.participants.add(attacker.getUUID());
        }
        if (!this.level().isClientSide && !this.isBroken()
                && BossPhaseGate.shouldCapMechanicDamage(source)) {
            float multiplier = ServerConfig.BOSS_UNBROKEN_DAMAGE_MULTIPLIER.get().floatValue();
            float cap = ServerConfig.BOSS_UNBROKEN_MAX_DAMAGE.get().floatValue();
            amount = Math.min(amount * multiplier, cap);
            if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.unknown_echoes.boss.protected"), true);
                this.protectedMessageCooldown = 40;
            }
        } else if (!this.level().isClientSide && this.isBroken()
                && BossPhaseGate.shouldCapMechanicDamage(source)) {
            amount = BossPhaseGate.capDamage(this.getHealth(), amount, this.brokenWindowHealthFloor);
            if (amount <= 0.0F) {
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt
                && BossPhaseGate.reachedFloor(this, this.brokenWindowHealthFloor)) {
            restoreMemoryGuard();
        }
        return hurt;
    }

    private void restoreMemoryGuard() {
        this.setBroken(false);
        this.brokenTicksRemaining = 0;
        this.brokenWindowHealthFloor = 0.0F;
        this.resetPillars();
        this.simonPhase = SimonPhase.IDLE;
        this.simonInputProgress = 0;
        allPillarsOff();
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.6F);
        broadcastNearby(Component.translatable("message.unknown_echoes.boss.guard_restored"));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 死亡演出:身体分段石化崩解(自上而下的碎石环,文档 10.5)
            BlockState deepslate = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
            for (int layer = 0; layer < 4; layer++) {
                double y = this.getY() + this.getBbHeight() * (0.9 - layer * 0.25);
                for (int i = 0; i < 12; i++) {
                    double angle = Math.PI * 2 * i / 12 + layer * 0.4;
                    serverLevel.sendParticles(
                            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, deepslate),
                            this.getX() + Math.cos(angle) * 1.4, y,
                            this.getZ() + Math.sin(angle) * 1.4, 6, 0.3, 0.2, 0.3, 0.08);
                }
            }
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    30, 1.0, 1.5, 1.0, 0.1);
            // 设计红线 #4:奖励只发给"死亡时仍在场地范围内"的参与玩家(伤害/机关/在场任一记录过)
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : serverLevel.players()) {
                if (this.participants.contains(player.getUUID())
                        && player.distanceToSqr(this) <= radius * radius) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                            EchoPermission.FORGOTTEN_COLOSSUS_ID);
                    EchoAbilityManager.markBossDefeated(player, EchoPermission.FORGOTTEN_COLOSSUS_ID);
                    if (firstKill) {
                        BossMaterialRewards.givePersonal(player, EchoPermission.FORGOTTEN_COLOSSUS_ID,
                                new ItemStack(ModItems.COLOSSUS_MEMORY_CORE.get()));
                    }
                    BossMaterialRewards.giveOrdinary(player, new ItemStack(ModItems.MEMORY_STONE_SLAB.get(),
                            2 + this.random.nextInt(3)));
                }
            }
        }
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

    @Override
    public void checkDespawn() {
        // Boss 不自然消失
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.COLOSSUS_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.COLOSSUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COLOSSUS_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.COLOSSUS_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Broken", this.isBroken());
        tag.putInt("BrokenTicks", this.brokenTicksRemaining);
        tag.putFloat("BrokenWindowHealthFloor", this.brokenWindowHealthFloor);
        ListTag pillars = new ListTag();
        for (BlockPos pos : this.activePillars) {
            pillars.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("ActivePillars", pillars);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        tag.putBoolean("Awakened", this.awakened);
        tag.putBoolean("EnrageAnnounced", this.enrageAnnounced);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setBroken(tag.getBoolean("Broken"));
        this.brokenTicksRemaining = tag.getInt("BrokenTicks");
        this.brokenWindowHealthFloor = tag.getFloat("BrokenWindowHealthFloor");
        this.activePillars.clear();
        ListTag pillars = tag.getList("ActivePillars", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < pillars.size(); i++) {
            int[] coords = pillars.getIntArray(i);
            if (coords.length == 3) {
                this.activePillars.add(new BlockPos(coords[0], coords[1], coords[2]));
            }
        }
        this.participants.clear();
        ListTag participantsTag = tag.getList("Participants", Tag.TAG_INT_ARRAY);
        for (Tag entry : participantsTag) {
            this.participants.add(net.minecraft.nbt.NbtUtils.loadUUID(entry));
        }
        this.scaledParticipants = Math.max(1, tag.getInt("ScaledParticipants"));
        this.awakened = tag.getBoolean("Awakened");
        this.enrageAnnounced = tag.getBoolean("EnrageAnnounced");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // ---- GeckoLib 动画 ----

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.2F, 1.0F);
            this.triggerAnim("attack_controller", "attack");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.CRIT);
            }
        }
        return hit;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isBroken()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("slam", SLAM_ANIM)
                .triggerableAnim("stomp", STOMP_ANIM)
                .triggerableAnim("beam", BEAM_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
