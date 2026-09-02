package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.ai.CirclePreferredRangeGoal;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
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

public class ForgottenColossus extends Monster implements GeoEntity, BossDamageGuard, PhaseGatedBoss,
        BossVisualState,
        RechallengeBoundBoss {
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
    private static final RawAnimation ROCK_RAIN_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.rock_rain", Animation.LoopType.PLAY_ONCE);

    // 状态演出必须走独立的 state_controller,与攻击动画同 controller 会互相打断。
    private static final RawAnimation AWAKEN_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.awaken", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BREAK_CORE_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.break_core", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ENRAGE_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.enrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RESTORE_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.restore", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation COMMAND_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.command", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation COLLAPSE_ANIM =
            RawAnimation.begin().then("animation.forgotten_colossus.collapse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(ForgottenColossus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_MODE =
            SynchedEntityData.defineId(ForgottenColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_AGE =
            SynchedEntityData.defineId(ForgottenColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_DURATION =
            SynchedEntityData.defineId(ForgottenColossus.class, EntityDataSerializers.INT);

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
    /** 参与玩家(伤害过 Boss / 激活过记忆柱 / 战斗期间在场),用于逐人进度与单份物品结算。 */
    private final Set<UUID> participants = new HashSet<>();
    private BlockPos rechallengeAltar;
    private BlockPos homePos;

    private enum SimonPhase { IDLE, DEMO, INPUT }
    private static final int SIMON_LIT_TICKS = 12;
    private static final int SIMON_STEP_TICKS = 20;
    private static final double SIMON_HINT_TOP_OFFSET = 0.95D;
    private SimonPhase simonPhase = SimonPhase.IDLE;
    private final List<BlockPos> orderedPillars = new ArrayList<>();
    private int[] simonSequence = new int[0];
    private int simonDemoStep = 0;
    private int simonDemoTimer = 0;
    private int simonInputProgress = 0;
    /** 记忆柱不足时的重扫退避(tick)。 */
    private int pillarRescanCooldown = 0;
    /** Simon 阶段遇到区块卸载后,等待相关区域重新加载再熄灭旧柱。 */
    private boolean pillarCleanupPending = false;

    private static final int SLAM_HIT_DELAY = 15;
    private static final int STOMP_HIT_DELAY = 11;
    private static final int ROCK_RAIN_WARN_TICKS = 24;
    private static final int BEAM_WINDUP_TICKS = 20;
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
    /** 石臂横扫方向(前摇开始时锁定——预警线指哪打哪,前摇期间不跟随转身)。 */
    private net.minecraft.world.phys.Vec3 slamDirection = net.minecraft.world.phys.Vec3.ZERO;
    /** 崩解狂怒(低于 30% 血)进入演出只放一次(NBT 持久化防重载重复播报)。 */
    private boolean enrageAnnounced = false;
    /** 沉睡唤醒:首次锁定目标时的苏醒演出只放一次(NBT 持久化)。 */
    private boolean awakened = false;
    private int scaledParticipants = 1;
    private static final double BASE_MAX_HEALTH = 520.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 160.0D;

    public ForgottenColossus(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    /** 石臂与演出附肢不计入命中范围，主体底面使用 profile 的长方形尺寸。 */
    @Override
    protected AABB makeBoundingBox() {
        return EntityHitboxProfiles.make(this);
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        EntityHitboxProfiles.refresh(this);
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

    /**
     * 绕圈半径必须同时落在光束(距离平方 >6²)与碎石雨(>5²)的射程区间内,
     * 否则永久贴脸只会循环踏击与横扫。优先级数必须小于近战,MOVE 只能被更小的数抢走。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CirclePreferredRangeGoal(this, 6.0D, 9.5D, 0.85D,
                this::isMovementLockedByAttack));
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
        builder.define(DATA_VISUAL_ATTACK_MODE, 0);
        builder.define(DATA_VISUAL_ATTACK_AGE, 0);
        builder.define(DATA_VISUAL_ATTACK_DURATION, 0);
    }

    public boolean isBroken() {
        return this.entityData.get(DATA_BROKEN);
    }

    private void setBroken(boolean broken) {
        this.entityData.set(DATA_BROKEN, broken);
    }

    public void onMemoryPillarClicked(BlockPos pos, Player activator) {
        if (this.level().isClientSide || this.isBroken()) {
            return;
        }
        BossNbtData.addParticipant(this.participants, activator.getUUID());

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

            allPillarsOff();
            this.simonInputProgress = 0;
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.HOSTILE, 1.0F, 0.6F);
            broadcastNearby(Component.translatable("message.unknown_echoes.colossus.simon_wrong"));
            beginDemo();
        }
    }

    private void tickSimonTrial(List<ServerPlayer> arenaPlayers) {
        if (this.isBroken()) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel
                && this.simonPhase != SimonPhase.IDLE
                && !isScanAreaLoaded(serverLevel, this.blockPosition(), PILLAR_SCAN_RADIUS)) {
            this.simonPhase = SimonPhase.IDLE;
            this.simonInputProgress = 0;
            this.pillarCleanupPending = true;
            this.pillarRescanCooldown = Math.max(this.pillarRescanCooldown, 40);
            return;
        }

        if (this.simonPhase != SimonPhase.IDLE && this.tickCount % 40 == 0
                && !hasArenaParticipant(arenaPlayers)) {
            allPillarsOff();
            this.simonPhase = SimonPhase.IDLE;
            this.simonInputProgress = 0;
            return;
        }
        switch (this.simonPhase) {
            case IDLE -> {
                if (this.pillarCleanupPending
                        && this.level() instanceof ServerLevel serverLevel
                        && isScanAreaLoaded(serverLevel, this.blockPosition(), PILLAR_SCAN_RADIUS)) {
                    allPillarsOff();
                    this.pillarCleanupPending = false;
                }
                if (this.pillarRescanCooldown > 0) {
                    this.pillarRescanCooldown--;
                } else if (this.tickCount % 20 == 0 && hasArenaParticipant(arenaPlayers)) {
                    startSimonRound();
                }
            }
            case DEMO -> tickSimonDemo();
            case INPUT -> {  }
        }
    }

    private void startSimonRound() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !isScanAreaLoaded(serverLevel, this.blockPosition(), PILLAR_SCAN_RADIUS)) {
            return;
        }
        List<BlockPos> pillars = scanAllPillars();
        if (pillars.size() < 2) {

            this.simonPhase = SimonPhase.IDLE;
            this.pillarRescanCooldown = 600;
            return;
        }
        BlockPos center = this.blockPosition();

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
            // 演示每一步抬臂指一次，否则玩家只看到柱子在亮。
            this.triggerAnim("state_controller", "command");
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

    private void playSimonPillarHint(BlockPos pillar, int timer) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 top = Vec3.atCenterOf(pillar).add(0.0D, SIMON_HINT_TOP_OFFSET, 0.0D);
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
        serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                top.x, top.y, top.z, 4, 0.18D, 0.10D, 0.18D, 0.02D);
        serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                top.x, top.y + 0.12D, top.z, 2, 0.12D, 0.08D, 0.12D, 0.01D);
        if (timer % 2 == 0) {
            drawSimonHintBeam(serverLevel, top, core);
            double radius = 0.52D + 0.05D * Math.sin((this.tickCount + timer) * 0.45D);
            BossFx.ring(serverLevel, top, radius, 14, 0.02D, BossParticleTheme.COLOSSUS.ember());
        }
    }

    private void drawSimonHintBeam(ServerLevel serverLevel, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(8, (int) (delta.length() * 1.4D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            if (i % 4 == 0) {
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                        point.x, point.y, point.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    private void enterBrokenState() {
        this.activePillars.clear();
        this.activePillars.addAll(this.orderedPillars);
        this.setBroken(true);
        this.triggerAnim("state_controller", "break_core");
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.70F, 0.35F);
        this.simonPhase = SimonPhase.IDLE;
        List<ServerPlayer> ritualParticipants = List.of();

        if (this.level() instanceof ServerLevel ritualLevel) {
            double ritualRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
            ritualParticipants = nearbyPlayers(ritualLevel, ritualRadius);
            for (ServerPlayer participant : ritualParticipants) {
                BossNbtData.addParticipant(this.participants, participant.getUUID());
                cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                        participant, cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO);
            }
        }
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.phys.Vec3 core = this.position().add(0, this.getBbHeight() * 0.65, 0);

            for (BlockPos pillar : this.orderedPillars) {
                net.minecraft.world.phys.Vec3 top = net.minecraft.world.phys.Vec3.atCenterOf(pillar).add(0, 0.8, 0);
                net.minecraft.world.phys.Vec3 delta = core.subtract(top);
                int steps = Math.max(6, (int) (delta.length() * 1.2));
                for (int i = 0; i <= steps; i++) {
                    net.minecraft.world.phys.Vec3 p = top.add(delta.scale(i / (double) steps));
                    serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            serverLevel.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z, 1, 0, 0, 0, 0);
            for (int ring = 1; ring <= 3; ring++) {
                double r = ring * 1.4;
                for (int i = 0; i < 12 + ring * 4; i++) {
                    double angle = Math.PI * 2 * i / (12 + ring * 4);
                    serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                            core.x + Math.cos(angle) * r, core.y, core.z + Math.sin(angle) * r,
                            1, 0.1, 0.2, 0.1, 0.05);
                    serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                            core.x + Math.cos(angle) * r, core.y - 0.5, core.z + Math.sin(angle) * r,
                            1, 0.1, 0.2, 0.1, 0.08);
                }
            }
        }
        Component brokenMessage = Component.translatable("message.unknown_echoes.boss.broken");
        for (ServerPlayer participant : ritualParticipants) {
            participant.sendSystemMessage(brokenMessage);
        }
    }

    private List<ServerPlayer> nearbyPlayers(ServerLevel level, double radius) {
        double radiusSqr = radius * radius;
        AABB bounds = this.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(ServerPlayer.class, bounds,
                player -> !player.isSpectator()
                        && player.distanceToSqr(this) <= radiusSqr);
    }

    private boolean hasArenaParticipant(List<ServerPlayer> arenaPlayers) {
        for (ServerPlayer player : arenaPlayers) {
            if (player.isAlive()) {
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

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, radius);
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

    private void cleanupArenaState(ServerLevel serverLevel) {
        Set<BlockPos> pillars = new HashSet<>(this.orderedPillars);
        pillars.addAll(this.activePillars);
        for (BlockPos pos : pillars) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(ModBlocks.MEMORY_PILLAR.get())
                    && state.getValue(MemoryPillarBlock.ACTIVATED)) {
                serverLevel.setBlock(pos,
                        state.setValue(MemoryPillarBlock.ACTIVATED, Boolean.FALSE), 3);
            }
        }
        this.activePillars.clear();
        this.orderedPillars.clear();
        this.simonSequence = new int[0];
        this.simonPhase = SimonPhase.IDLE;
        this.simonInputProgress = 0;
        this.pillarCleanupPending = false;
    }

    private void broadcastNearby(Component message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : nearbyPlayers(serverLevel, radius)) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * 抬手期锁死移动:预警粒子与伤害判定都锚在实时位置上,此时走动会让地上的圈跟着人跑。
     * 抬手起始的 getNavigation().stop() 只发一次,拦不住后续 Goal 重新 moveTo。
     */
    private boolean isMovementLockedByAttack() {
        return this.pendingSlamTicks > 0 || this.pendingStompTicks > 0
                || this.pendingRockRainTicks > 0 || this.pendingBeamTicks > 0;
    }

    private void tickSpecialAttacks() {
        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.stompCooldown > 0) this.stompCooldown--;
        if (this.rockRainCooldown > 0) this.rockRainCooldown--;
        if (this.beamCooldown > 0) this.beamCooldown--;
        this.syncVisualAttackState();
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

        boolean enraged = this.getHealth() < this.getMaxHealth() * 0.3F;
        int cooldownScale = enraged ? 65 : 100;
        double distSq = this.distanceToSqr(target);
        if (this.stompCooldown <= 0 && distSq <= 4.5 * 4.5) {
            this.pendingStompTicks = STOMP_HIT_DELAY;
            this.stompCooldown = 160 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "stomp");
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.5F, 1.0F);
        } else if (this.slamCooldown <= 0 && distSq <= 7.0 * 7.0 && this.hasLineOfSight(target)) {

            this.slamDirection = target.position().subtract(this.position())
                    .multiply(1, 0, 1).normalize();
            this.pendingSlamTicks = SLAM_HIT_DELAY;
            this.slamCooldown = 240 * cooldownScale / 100;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.triggerAnim("attack_controller", "slam");
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.5F, 1.0F);
        } else if (enraged && this.beamCooldown <= 0 && distSq <= 18.0 * 18.0
                && distSq > 6.0 * 6.0 && this.hasLineOfSight(target)) {

            this.beamDirection = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(this.beamOrigin()).normalize();
            this.pendingBeamTicks = BEAM_WINDUP_TICKS;
            this.beamCooldown = 280 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "beam");
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.5F);
        } else if (this.rockRainCooldown <= 0 && distSq <= 20.0 * 20.0 && distSq > 5.0 * 5.0) {

            this.lockRockRainTargets(target);
            if (!this.rockRainTargets.isEmpty()) {
                this.pendingRockRainTicks = ROCK_RAIN_WARN_TICKS;
                this.rockRainCooldown = 300 * cooldownScale / 100;
                this.triggerAnim("attack_controller", "rock_rain");
                this.playSound(ModSounds.COLOSSUS_ROCK_RAIN.get(), 1.6F, 1.0F);
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

    private void spawnRockRainWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (BlockPos pos : this.rockRainTargets) {
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.01);

            double sweep = this.tickCount * 0.7;
            for (int k = 0; k < 3; k++) {
                double angle = sweep + k * Math.PI * 2 / 3;
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                        pos.getX() + 0.5 + Math.cos(angle) * 2.0, pos.getY() + 0.15,
                        pos.getZ() + 0.5 + Math.sin(angle) * 2.0, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                        pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5, 3, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    private void doRockRainDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        BlockState deepslate = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
        for (BlockPos pos : this.rockRainTargets) {
            net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
            for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2.0, 2.0, 2.0),
                    e -> e != this && !e.isSpectator()
                            && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
                net.minecraft.world.phys.Vec3 offset = victim.position().subtract(center).multiply(1, 0, 1);
                if (offset.lengthSqr() > 2.0D * 2.0D) {
                    continue;
                }
                victim.hurt(this.damageSources().mobAttack(this), damage);
            }
            serverLevel.sendParticles(
                    new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, deepslate),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 1.0, 0.5, 1.0, 0.1);
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.6F);
        if (!this.rockRainTargets.isEmpty()) {
            double centerX = this.rockRainTargets.stream().mapToDouble(pos -> pos.getX() + 0.5D).average().orElse(this.getX());
            double centerY = this.rockRainTargets.stream().mapToDouble(pos -> pos.getY() + 0.3D).average().orElse(this.getY());
            double centerZ = this.rockRainTargets.stream().mapToDouble(pos -> pos.getZ() + 0.5D).average().orElse(this.getZ());
            BossFx.cinematicImpact(serverLevel, new net.minecraft.world.phys.Vec3(centerX, centerY, centerZ),
                    30.0D, 0.64F, 8, 0xB98A55);
        }
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
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                from.x, from.y, from.z, 3, 0.2, 0.2, 0.2, 0.02);
        BossFx.chargeSpiral(serverLevel, from, this.tickCount, BossParticleTheme.COLOSSUS.body());
    }

    private void doBeamDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        net.minecraft.world.phys.Vec3 from = this.beamOrigin();

        double reach = 18.0;
        for (double d = 1.0; d <= 18.0; d += 0.5) {
            BlockPos pos = BlockPos.containing(from.add(this.beamDirection.scale(d)));
            if (!serverLevel.hasChunkAt(pos)) {
                reach = Math.max(0.0D, d - 0.5D);
                break;
            }
            if (this.level().getBlockState(pos).is(ModBlocks.MEMORY_PILLAR.get())) {
                reach = d;
                break;
            }
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(18.0, 4.0, 18.0),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
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

        for (double d = 0.5; d <= reach; d += 0.5) {
            net.minecraft.world.phys.Vec3 p = from.add(this.beamDirection.scale(d));
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        BossFx.sparkSpray(serverLevel, from, this.beamDirection, reach, BossParticleTheme.COLOSSUS.spark());
        net.minecraft.world.phys.Vec3 end = from.add(this.beamDirection.scale(reach));
        serverLevel.sendParticles(BossParticleTheme.COLOSSUS.body(), end.x, end.y, end.z, 16, 0.5, 0.5, 0.5, 0.15);
        BossFx.impactBurst(serverLevel, end, BossParticleTheme.COLOSSUS.spark());
        BossFx.cinematicImpact(serverLevel, end, 30.0D, 0.5F, 7, 0x6DD5C5);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private void updateDynamicScaling() {
        int count = Math.max(1, this.participants.size());
        if (count <= this.scaledParticipants) {
            return;
        }
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double multiplier = ServerConfig.BOSS_HEALTH_MULTIPLIER.get();
            maxHealth.setBaseValue((BASE_MAX_HEALTH + HEALTH_PER_EXTRA_PLAYER * (count - 1)) * multiplier);
            this.heal((float) (HEALTH_PER_EXTRA_PLAYER * (count - this.scaledParticipants) * multiplier));
        }
        this.scaledParticipants = count;
    }

    private void spawnStompWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double r = Math.max(1.0, 6.0 * this.pendingStompTicks / (double) STOMP_HIT_DELAY);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12 + this.tickCount * 0.2;
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    this.getX() + Math.cos(angle) * r, this.getY() + 0.15,
                    this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.02, 0.05, 0.0);
        }
    }

    private void doStompDamage() {
        if (!this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (net.minecraft.world.entity.LivingEntity victim : serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(6.0D, 2.0D, 6.0D),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            net.minecraft.world.phys.Vec3 away = victim.position().subtract(this.position())
                    .multiply(1, 0, 1);
            if (away.lengthSqr() > 6.0D * 6.0D) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                away = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(away.x * 1.4, 0.6, away.z * 1.4));
                victim.hurtMarked = true;
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.COLOSSUS_STOMP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);

        serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                this.getX(), this.getY() + 0.3, this.getZ(), 1, 0, 0, 0, 0);
        BlockState stompDebris = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    this.getX() + Math.cos(angle) * 3.0, this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * 3.0, 2,
                    Math.cos(angle) * 0.8, 0.05, Math.sin(angle) * 0.8, 0.12);
        }
        serverLevel.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, stompDebris),
                this.getX(), this.getY() + 0.4, this.getZ(), 24, 2.5, 0.3, 2.5, 0.1);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), 6.0, BossParticleTheme.COLOSSUS.ember());
        BossFx.impactBurst(serverLevel, this.position().add(0, 0.3, 0), BossParticleTheme.COLOSSUS.spark());
        BossFx.cinematicImpact(serverLevel, this.position(), 36.0D, 0.92F, 12, 0xC58A49);
    }

    private void spawnSlamWarning() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.slamDirection.lengthSqr() < 1.0E-4) {
            return;
        }
        BlockState slamDust = net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState();

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
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            net.minecraft.world.phys.Vec3 toVictim = victim.position().subtract(this.position())
                    .multiply(1, 0, 1);
            if (toVictim.length() > 6.5) {
                continue;
            }
            if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.5) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(facing.x * 0.8, 0.3, facing.z * 0.8));
                victim.hurtMarked = true;
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.6F, 0.7F);

        for (int d = 1; d <= 5; d++) {
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    this.getX() + facing.x * d, this.getY() + 0.3, this.getZ() + facing.z * d,
                    4, 0.6, 0.1, 0.6, 0.02);
        }
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.4, 0), facing, 6.5, BossParticleTheme.COLOSSUS.spark());
        BossFx.impactBurst(serverLevel, this.position().add(facing.scale(3.0)).add(0, 0.4, 0), BossParticleTheme.COLOSSUS.spark());
        BossFx.cinematicImpact(serverLevel, this.position().add(facing.scale(3.0)),
                34.0D, 0.8F, 10, 0xD0A05B);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.tickRechallengeBinding(serverLevel)) {
            return;
        }
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (!this.awakened && this.getTarget() != null
                && this.level() instanceof ServerLevel serverLevel) {
            this.awakened = true;
            this.triggerAnim("state_controller", "awaken");
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
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 1, 0, 0, 0, 0);
        }
        if (this.protectedMessageCooldown > 0) {
            this.protectedMessageCooldown--;
        }

        if (this.isAlive() && !this.enrageAnnounced
                && this.getHealth() < this.getMaxHealth() * 0.3F
                && this.level() instanceof ServerLevel serverLevel) {
            this.enrageAnnounced = true;
            this.triggerAnim("state_controller", "enrage");
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.5F);
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.body(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    40, this.getBbWidth() * 0.7, this.getBbHeight() * 0.4, this.getBbWidth() * 0.7, 0.15);
            broadcastNearby(Component.translatable("message.unknown_echoes.boss.enraged"));
        }

        if (this.isAlive() && this.tickCount % 10 == 0 && this.level() instanceof ServerLevel serverLevel) {
            if (this.isBroken()) {
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                        6, this.getBbWidth() * 0.6, this.getBbHeight() * 0.4, this.getBbWidth() * 0.6, 0.02);
            } else {
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        4, this.getBbWidth() * 0.7, this.getBbHeight() * 0.45, this.getBbWidth() * 0.7, 0.01);
            }
            if (this.enrageAnnounced) {
                serverLevel.sendParticles(BossParticleTheme.COLOSSUS.spark(),
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
        List<ServerPlayer> arenaPlayers = List.of();

        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
            if (this.getTarget() != null || !this.isBroken()) {
                double arenaRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
                arenaPlayers = this.nearbyPlayers(serverLevel, arenaRadius);
            }

            if (this.getTarget() != null) {
                for (ServerPlayer player : arenaPlayers) {
                    BossNbtData.addParticipant(this.participants, player.getUUID());
                }
                if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                    this.updateDynamicScaling();
                }
            }
        }
        this.tickSimonTrial(arenaPlayers);
        this.tickSpecialAttacks();
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** 脱战回血:判据与配置口径由 OutOfCombatRegen 统一持有。 */
    private void tickOutOfCombatRegen() {
        this.outOfCombatTicks = OutOfCombatRegen.tick(this, this.outOfCombatTicks);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isRechallengeCombatBlocked(serverLevel)) {
            return false;
        }

        if (!this.level().isClientSide
                && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            BossNbtData.addParticipant(this.participants, attacker.getUUID());
        }
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt
                && BossPhaseGate.reachedFloor(this, this.brokenWindowHealthFloor)) {
            restoreMemoryGuard();
        }
        return hurt;
    }

    @Override
    public boolean isMechanicDamageGuarded() {
        return !this.isBroken();
    }

    @Override
    public void onMechanicDamageGuarded(DamageSource source) {
        if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.boss.protected"), true);
            this.protectedMessageCooldown = 40;
        }
    }

    @Override
    public float phaseDamageFloor() {
        return this.isBroken() ? this.brokenWindowHealthFloor : 0.0F;
    }

    private void restoreMemoryGuard() {
        if (this.level() instanceof ServerLevel serverLevel
                && (!arePillarChunksLoaded(serverLevel, this.activePillars)
                || !arePillarChunksLoaded(serverLevel, this.orderedPillars))) {

            this.brokenTicksRemaining = 1;
            return;
        }
        this.setBroken(false);
        this.triggerAnim("state_controller", "restore");
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

    private static boolean arePillarChunksLoaded(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (!this.registerRechallengePresence(serverLevel)) {
                this.discard();
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (wasDead || !this.dead) {
            return;
        }
        this.triggerAnim("state_controller", "collapse");
        if (this.level() instanceof ServerLevel serverLevel) {
            this.cleanupArenaState(serverLevel);

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
            serverLevel.sendParticles(BossParticleTheme.COLOSSUS.body(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    30, 1.0, 1.5, 1.0, 0.1);

            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            boolean hasEligibleParticipant = false;
            for (ServerPlayer player : nearbyPlayers(serverLevel, radius)) {
                if (this.participants.contains(player.getUUID())) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                            EchoPermission.FORGOTTEN_COLOSSUS_ID);
                    hasEligibleParticipant = true;
                    if (firstKill && !EchoAbilityManager.markBossDefeated(
                            player, EchoPermission.FORGOTTEN_COLOSSUS_ID)) {
                        player.sendSystemMessage(Component.translatable(
                                "message.unknown_echoes.progress_record.full"));
                    }
                }
            }
            if (hasEligibleParticipant) {
                BossMaterialRewards.drop(this, new ItemStack(ModItems.COLOSSUS_MEMORY_CORE.get()));
                BossMaterialRewards.drop(this, new ItemStack(ModItems.MEMORY_STONE_SLAB.get(),
                        2 + this.random.nextInt(3)));
                BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_SHARD.get(),
                        8 + this.random.nextInt(9)));
                BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_CORE.get(),
                        1 + this.random.nextInt(2)));
            }
            this.finishRechallenge(serverLevel);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel serverLevel) {
            this.cleanupArenaState(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    public cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind rechallengeKind() {
        return cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind.FORGOTTEN_COLOSSUS;
    }

    @Override
    public BlockPos rechallengeAltar() {
        return this.rechallengeAltar;
    }

    @Override
    public void bindRechallengeAltar(BlockPos altarPos) {
        this.rechallengeAltar = altarPos.immutable();
    }

    @Override
    public void bindRechallengeArena(BlockPos altarPos, BlockPos arenaCenter) {
        this.rechallengeAltar = altarPos.immutable();
        this.homePos = arenaCenter.immutable();
        this.restrictTo(this.homePos, PILLAR_SCAN_RADIUS);
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
        if (this.homePos != null) {
            tag.putIntArray("HomePos", new int[]{this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
        }
        this.saveRechallengeBinding(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.brokenTicksRemaining = BossNbtData.clampTimer(tag.getInt("BrokenTicks"),
                ServerConfig.BOSS_BROKEN_DURATION_TICKS.get());
        this.setBroken(tag.getBoolean("Broken") && this.brokenTicksRemaining > 0);
        this.brokenWindowHealthFloor = BossNbtData.clampHealthFloor(
                tag.getFloat("BrokenWindowHealthFloor"), this.getMaxHealth());
        if (!this.isBroken()) {
            this.brokenTicksRemaining = 0;
            this.brokenWindowHealthFloor = 0.0F;
        }
        BossNbtData.loadPositionsNear(tag, "ActivePillars", this.activePillars,
                BossNbtData.MAX_ARENA_POSITIONS, this.blockPosition(),
                PILLAR_SCAN_RADIUS * 2, PILLAR_SCAN_HEIGHT * 2);
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        this.awakened = tag.getBoolean("Awakened");
        this.enrageAnnounced = tag.getBoolean("EnrageAnnounced");
        this.homePos = BossNbtData.loadPositionNear(tag, "HomePos", this.blockPosition(),
                PILLAR_SCAN_RADIUS * 2, PILLAR_SCAN_HEIGHT * 2);
        if (this.homePos != null) {
            this.restrictTo(this.homePos, PILLAR_SCAN_RADIUS);
        }
        this.loadRechallengeBinding(tag);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level() instanceof ServerLevel serverLevel
                && this.isRechallengeCombatBlocked(serverLevel)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.playSound(ModSounds.COLOSSUS_ATTACK.get(), 1.2F, 1.0F);
            this.triggerAnim("attack_controller", "attack");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), BossParticleTheme.COLOSSUS.spark());
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
                .triggerableAnim("beam", BEAM_ANIM)
                .triggerableAnim("rock_rain", ROCK_RAIN_ANIM));
        // 状态演出单独一个 controller：苏醒/破防/狂怒会与攻击并发,同 controller 会互相截断。
        controllers.add(new AnimationController<>(this, "state_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("awaken", AWAKEN_ANIM)
                .triggerableAnim("break_core", BREAK_CORE_ANIM)
                .triggerableAnim("enrage", ENRAGE_ANIM)
                .triggerableAnim("restore", RESTORE_ANIM)
                .triggerableAnim("command", COMMAND_ANIM)
                .triggerableAnim("collapse", COLLAPSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void syncVisualAttackState() {
        int mode;
        int remaining;
        int duration;
        if (this.pendingStompTicks > 0) {
            mode = 1;
            remaining = this.pendingStompTicks;
            duration = STOMP_HIT_DELAY;
        } else if (this.pendingSlamTicks > 0) {
            mode = 2;
            remaining = this.pendingSlamTicks;
            duration = SLAM_HIT_DELAY;
        } else if (this.pendingBeamTicks > 0) {
            mode = 3;
            remaining = this.pendingBeamTicks;
            duration = BEAM_WINDUP_TICKS;
        } else if (this.pendingRockRainTicks > 0) {
            mode = 4;
            remaining = this.pendingRockRainTicks;
            duration = ROCK_RAIN_WARN_TICKS;
        } else {
            mode = 0;
            remaining = 0;
            duration = 0;
        }
        this.entityData.set(DATA_VISUAL_ATTACK_MODE, mode);
        this.entityData.set(DATA_VISUAL_ATTACK_AGE, Math.max(0, duration - remaining));
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
}
