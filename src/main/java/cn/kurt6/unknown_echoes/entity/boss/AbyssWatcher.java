package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.BossLifecycleFx;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Path;
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

public class AbyssWatcher extends Monster implements GeoEntity, BossTestControl, BossDamageGuard, PhaseGatedBoss,
        BossVisualState,
        RechallengeBoundBoss, BossMusicSource {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.abyss_watcher.idle");
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.abyss_watcher.swim");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.abyss_watcher.broken");
    private static final RawAnimation VORTEX_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.vortex", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BURST_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.burst", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DASH_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.dash", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BLADES_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.blades", Animation.LoopType.PLAY_ONCE);

    // 状态表演动画。走独立的 state_controller,避免与攻击动画互相打断。
    private static final RawAnimation AWAKEN_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.awaken", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RUNE_CORRUPT_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.rune_corrupt", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BREAK_VEIL_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.break_veil", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VEIL_RESTORE_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.veil_restore", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ENRAGE_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.enrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DEATH_ANIM =
            RawAnimation.begin().then("animation.abyss_watcher.death", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLONE =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_MODE =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_AGE =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_DURATION =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.INT);

    private static final int CLONE_COUNT_NO_RUNES = 2;
    private static final int CLONE_GUARD_CAP = 3;
    private static final int CLONE_LIFETIME_TICKS = 1200;
    private static final double CLONE_MAX_HEALTH = 8.0D;
    private static final double CLONE_ATTACK_DAMAGE = 3.0D;
    /** 守卫判定半径:符文这个范围内有存活拟影时无法净化(TideRuneBlock 调用)。 */
    public static final double CLONE_GUARD_RADIUS = 5.0D;
    private static final double CLONE_LEASH_DIST_SQ = 8.0D * 8.0D;
    private static final double CLONE_RETURN_DONE_DIST_SQ = 2.5D * 2.5D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int cloneLifeTicks = CLONE_LIFETIME_TICKS;
    /** 拟影的守卫岗位(被侵蚀符文的位置);null = 无岗位游荡拟影。仅分身使用。 */
    private BlockPos guardPos = null;
    /** 仅分身使用；用于父 Boss 异常销毁且分身稍后才重载时自清理。 */
    private UUID cloneOwnerUuid = null;
    private int brokenTicksRemaining = 0;
    private float brokenWindowHealthFloor = 0.0F;
    private int veiledMessageCooldown = 0;
    private int outOfCombatTicks = 0;
    private BlockPos homePos = null;
    private final List<UUID> cloneIds = new ArrayList<>();
    /** 无符文场地的重扫退避:避免每 10 tick 全量扫描 6 万+方块。 */
    private int runeRescanCooldown = 0;
    private final Set<UUID> participants = new HashSet<>();
    private BlockPos rechallengeAltar;
    private boolean handlingDeath = false;
    private boolean cleaningArena = false;
    private ServerLevel arenaCleanupLevel = null;

    private static final int VORTEX_HIT_DELAY = 15;
    private static final int BURST_HIT_DELAY = 8;
    private static final int DASH_WINDUP_TICKS = 14;
    private static final int BLADE_WINDUP_TICKS = 16;
    private static final double VORTEX_RANGE = 11.0D;
    private static final double BURST_RANGE = 5.5D;
    private static final double DASH_RANGE = 14.0D;
    private static final double BLADE_RANGE = 16.0D;
    private int vortexCooldown = 0;
    private int burstCooldown = 0;
    private int dashCooldown = 0;
    private int bladeCooldown = 0;
    private int pendingVortexTicks = 0;
    private int pendingBurstTicks = 0;
    private int pendingDashTicks = 0;
    private int pendingBladeTicks = 0;
    /** 冲刺方向(预警开始时锁定,水纹沿这条线铺开)。 */
    private Vec3 dashDirection = Vec3.ZERO;
    /** 水刃落点(蓄力开始时锁定目标,期间光束持续指向)。 */
    private Vec3 bladeTargetPos = Vec3.ZERO;
    private SpecialAttack specialAttack;
    private boolean handlingSpecialAttacks;
    private boolean specialAttackVisuals;
    private int lastSpecialAttackTick = Integer.MIN_VALUE;

    private record SpecialAttack(ServerLevel level, LivingEntity target, boolean broken) {}

    private static final int RUNE_SCAN_RADIUS = 30;
    private static final int RUNE_SCAN_HEIGHT = 10;
    private static final int RUNE_MECHANIC_COUNT = 6;
    private static final int RUNE_PREFERRED_MIN_RADIUS = 10;
    private static final int RUNE_PREFERRED_MAX_RADIUS = 22;
    private static final int RUNE_PREFERRED_MAX_HEIGHT = 4;
    private static final double RUNE_PREFERRED_RADIUS = 14.0D;
    private final List<BlockPos> corruptedRunes = new ArrayList<>();
    /** 旧存档可能记录了整圈装饰符文,加载后只迁移一次。 */
    private boolean runeSelectionNeedsRefresh = false;
    private boolean handlingRuneSelection = false;
    private boolean handlingRuneTransition = false;
    private boolean handlingRuneGuidance = false;
    private boolean handlingCloneLifecycle = false;
    private int cloneRespawnCooldown = 0;
    private boolean cloneWaveActive = false;
    private boolean handlingCloneSummon = false;
    private boolean clonePresenceUnresolved = false;
    private int lastCloneWaveTick = Integer.MIN_VALUE;

    /** 不入 NBT:只控制动画触发,重载后最多多播一次表演。 */
    private boolean awakenPlayed = false;
    private boolean enragePlayed = false;

    private int scaledParticipants = 1;
    private static final double BASE_MAX_HEALTH = 430.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 150.0D;

    public AbyssWatcher(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.6F, 0.5F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 430.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.9D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ReturnToGuardPostGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {

        if (type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public void travel(Vec3 travelVector) {

        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BROKEN, false);
        builder.define(DATA_CLONE, false);
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

    public boolean isClone() {
        return this.entityData.get(DATA_CLONE);
    }

    public boolean isCloneOf(AbyssWatcher owner) {
        return this.isClone() && owner.getUUID().equals(this.cloneOwnerUuid);
    }

    public void becomeClone() {
        this.entityData.set(DATA_CLONE, true);
        this.xpReward = 0;
        this.cloneLifeTicks = CLONE_LIFETIME_TICKS;
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(CLONE_MAX_HEALTH);
        }
        var attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(CLONE_ATTACK_DAMAGE);
        }
        this.setHealth((float) CLONE_MAX_HEALTH);
    }

    private void becomeClone(AbyssWatcher owner) {
        this.cloneOwnerUuid = owner.getUUID();
        this.homePos = owner.arenaAnchor().immutable();
        this.becomeClone();
        this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
    }

    private class ReturnToGuardPostGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private static final int RETRY_TICKS = 20;
        private ServerLevel returnLevel;
        private BlockPos returnPost;
        private UUID returnOwner;
        private PathNavigation returnNavigation;
        private Path returnPath;
        private boolean starting;
        private boolean waiting;
        private int retryAtTick;
        private int revision;

        ReturnToGuardPostGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.starting || this.returnPath != null) {
                return false;
            }
            this.returnLevel = null;
            this.returnPost = null;
            this.returnOwner = null;
            this.returnNavigation = null;
            this.revision++;
            BlockPos post = AbyssWatcher.this.guardPos;
            if (!(AbyssWatcher.this.level() instanceof ServerLevel level)
                    || !isCurrentClone(level, AbyssWatcher.this) || AbyssWatcher.this.isNoAi()
                    || post == null || AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(post)) <= CLONE_LEASH_DIST_SQ) {
                return false;
            }
            int remaining = this.retryAtTick - AbyssWatcher.this.tickCount;
            if (this.waiting && remaining > 0 && remaining <= RETRY_TICKS) {
                return false;
            }
            this.waiting = true;
            this.retryAtTick = AbyssWatcher.this.tickCount + RETRY_TICKS;
            this.returnLevel = level;
            this.returnPost = post.immutable();
            this.returnOwner = AbyssWatcher.this.cloneOwnerUuid;
            this.returnNavigation = AbyssWatcher.this.getNavigation();
            if (!this.isCurrentPost()) {
                this.returnLevel = null;
                this.returnPost = null;
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.starting && this.returnPath != null && this.isCurrentPost()
                    && AbyssWatcher.this.getNavigation() == this.returnNavigation
                    && this.returnNavigation.getPath() == this.returnPath
                    && AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(this.returnPost))
                            > CLONE_RETURN_DONE_DIST_SQ
                    && !this.returnNavigation.isDone();
        }

        @Override
        public void start() {
            if (this.starting) { return; }
            this.starting = true;
            int attempt = this.revision;
            boolean accepted = false;
            PathNavigation navigation = AbyssWatcher.this.getNavigation();
            Path submitted = null;
            try {
                if (!this.isCurrentPost() || navigation != this.returnNavigation
                        || AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(this.returnPost)) <= CLONE_RETURN_DONE_DIST_SQ) { return; }
                Path previous = navigation.getPath();
                Path path = navigation.createPath(this.returnPost, 1);
                if (attempt != this.revision || !this.isCurrentPost()
                        || AbyssWatcher.this.getNavigation() != navigation || navigation.getPath() != previous
                        || AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(this.returnPost)) <= CLONE_RETURN_DONE_DIST_SQ
                        || path != null && !this.returnPost.equals(path.getTarget())) {
                    return;
                }
                // 原生 moveTo 会复用节点相同的旧 Path，不能把回调换入的新路径认作本次提交。
                submitted = path == null ? null : path.sameAs(previous) ? previous : path;
                this.returnNavigation = navigation;
                this.returnPath = submitted;
                accepted = navigation.moveTo(path, 1.2D) && attempt == this.revision && this.isCurrentPost()
                        && AbyssWatcher.this.getNavigation() == navigation && navigation.getPath() == submitted
                        && AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(this.returnPost)) > CLONE_RETURN_DONE_DIST_SQ
                        && !navigation.isDone();
                if (accepted) {
                    this.waiting = false;
                }
            } finally {
                this.starting = false;
                if (!accepted) {
                    int stoppedRevision = this.revision + 1;
                    this.stop();
                    if (this.revision == stoppedRevision && submitted != null && navigation.getPath() == submitted) {
                        navigation.stop();
                    }
                }
            }
        }

        @Override
        public void stop() {
            PathNavigation navigation = this.returnNavigation;
            Path path = this.returnPath;
            if (this.returnPost != null && AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(this.returnPost))
                    > CLONE_RETURN_DONE_DIST_SQ) {
                this.waiting = true;
                this.retryAtTick = AbyssWatcher.this.tickCount + RETRY_TICKS;
            }
            this.revision++;
            this.returnLevel = null;
            this.returnPost = null;
            this.returnOwner = null;
            this.returnNavigation = null;
            this.returnPath = null;
            if (path != null && navigation != null && navigation.getPath() == path) {
                navigation.stop();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!this.canContinueToUse()) {
                this.stop();
            }
        }

        private boolean isCurrentPost() {
            if (this.returnLevel == null || this.returnPost == null
                    || !isCurrentClone(this.returnLevel, AbyssWatcher.this) || AbyssWatcher.this.isNoAi()
                    || !this.returnPost.equals(AbyssWatcher.this.guardPos)
                    || !java.util.Objects.equals(this.returnOwner, AbyssWatcher.this.cloneOwnerUuid)) {
                return false;
            }
            if (this.returnOwner != null) {
                Entity owner = this.returnLevel.getEntity(this.returnOwner);
                if (!(owner instanceof AbyssWatcher watcher) || watcher.level() != this.returnLevel
                        || watcher.isClone() || !watcher.isAlive() || watcher.dead || watcher.handlingDeath) {
                    return false;
                }
            }
            BlockState state = readableRuneState(this.returnLevel, this.returnPost);
            return state != null && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED);
        }
    }

    private void summonClones(ServerLevel serverLevel) {
        BlockPos center = this.arenaAnchor();
        List<BlockPos> runes = List.copyOf(this.corruptedRunes);
        if (this.handlingCloneSummon || this.cloneWaveActive || !this.cloneIds.isEmpty()
                || !isCurrentCloneWaveSource(serverLevel, center, runes)) {
            return;
        }
        this.handlingCloneSummon = true;
        this.cloneRespawnCooldown = 40;
        try {
            if (!areRuneChunksReady(serverLevel, center, RUNE_SCAN_RADIUS)) {
                return;
            }
            List<BlockPos> posts = new ArrayList<>();
            for (BlockPos pos : runes) {
                BlockState state = readableArenaState(serverLevel, pos);
                if (state == null) {
                    return;
                }
                if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                        && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                    posts.add(pos);
                }
                if (posts.size() >= CLONE_GUARD_CAP) {
                    break;
                }
            }
            int count = posts.isEmpty() ? CLONE_COUNT_NO_RUNES : posts.size();
            for (int i = 0; i < count; i++) {
                BlockPos post = i < posts.size() ? posts.get(i) : null;
                if (!isCurrentCloneWaveSource(serverLevel, center, runes) || !isCurrentClonePost(serverLevel, post)) {
                    return;
                }
                AbyssWatcher clone = this.getType().create(serverLevel) instanceof AbyssWatcher watcher ? watcher : null;
                if (clone == null) {
                    continue;
                }
                UUID cloneId = clone.getUUID();
                try {
                    clone.becomeClone(this);
                    if (!isCurrentCloneWaveSource(serverLevel, center, runes) || !isCurrentClonePost(serverLevel, post)) {
                        return;
                    }
                    Vec3 spawn = this.findCloneSpawn(serverLevel, clone, post);
                    if (spawn == null) {
                        continue;
                    }
                    clone.moveTo(spawn.x, spawn.y, spawn.z, this.random.nextFloat() * 360.0F, 0.0F);
                    clone.guardPos = post;
                    LivingEntity target = this.getTarget();
                    clone.setTarget(target != null && target.isAlive() && target.level() == serverLevel
                            && this.canAttack(target) ? target : null);
                    if (!isCurrentCloneCandidate(serverLevel, center, runes, clone, post)) {
                        continue;
                    }
                    clone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(clone.blockPosition()),
                            MobSpawnType.MOB_SUMMONED, null);
                    if (!isCurrentCloneCandidate(serverLevel, center, runes, clone, post)) {
                        continue;
                    }
                    // 加入世界后的回调可能立即结算本体死亡，先保留清理所需的 ID。
                    this.cloneIds.add(cloneId);
                    serverLevel.addFreshEntity(clone);
                } finally {
                    boolean registered = isCurrentOwnedClone(serverLevel, clone);
                    if (registered && isCurrentCloneCandidate(serverLevel, center, runes, clone, post)) {
                        if (!this.cloneIds.contains(cloneId)) {
                            this.cloneIds.add(cloneId);
                        }
                        this.cloneWaveActive = true;
                        this.cloneRespawnCooldown = 200;
                    } else {
                        try {
                            if (clone.level() == serverLevel && clone.isClone()
                                    && this.getUUID().equals(clone.cloneOwnerUuid) && !clone.isRemoved()) {
                                clone.discard();
                            }
                        } finally {
                            if (!isCurrentOwnedClone(serverLevel, clone)) {
                                this.cloneIds.remove(cloneId);
                            } else {
                                // 移除未完成时继续追踪，不能把它当成空波次重复生成。
                                this.cloneWaveActive = true;
                                this.cloneRespawnCooldown = 200;
                            }
                        }
                    }
                }
                if (!isCurrentCloneWaveSource(serverLevel, center, runes)) {
                    return;
                }
                if (isCurrentOwnedClone(serverLevel, clone)) {
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                            clone.getX(), clone.getY() + 0.8, clone.getZ(), 16, 0.5, 0.5, 0.5, 0.06);
                }
            }
            if (this.cloneWaveActive && isCurrentCloneWaveSource(serverLevel, center, runes)) {
                serverLevel.playSound(null, this.blockPosition(),
                        SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2F, 1.4F);
                if (isCurrentCloneWaveSource(serverLevel, center, runes)) {
                    broadcastRunePhase(serverLevel, false, Component.translatable("message.unknown_echoes.watcher.clones"));
                }
            }
        } finally {
            this.handlingCloneSummon = false;
        }
    }

    private boolean isCurrentCloneWaveSource(ServerLevel level, BlockPos center, List<BlockPos> runes) {
        return !this.handlingRuneSelection && !this.handlingRuneTransition && !this.runeSelectionNeedsRefresh
                && isCurrentRuneSelectionSource(level, center) && this.corruptedRunes.equals(runes);
    }

    private static boolean isCurrentClonePost(ServerLevel level, @Nullable BlockPos post) {
        if (post == null) {
            return true;
        }
        BlockState state = readableRuneState(level, post);
        return state != null && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED);
    }

    private boolean isCurrentCloneCandidate(ServerLevel level, BlockPos center, List<BlockPos> runes,
                                            AbyssWatcher clone, @Nullable BlockPos post) {
        LivingEntity target = clone.getTarget();
        return isCurrentCloneWaveSource(level, center, runes) && isCurrentClonePost(level, post)
                && clone.level() == level && clone.isAlive() && clone.isClone()
                && this.getUUID().equals(clone.cloneOwnerUuid) && java.util.Objects.equals(clone.guardPos, post)
                && (target == null || target == this.getTarget() && target.level() == level
                && target.isAlive() && this.canAttack(target))
                && isSafeCloneSpawn(level, clone, clone.position());
    }

    private void tickCloneWave(ServerLevel level) {
        if (this.handlingCloneSummon || this.tickCount % 10 != 0 || this.getTarget() == null
                || this.handlingRuneSelection || this.handlingRuneTransition || this.runeSelectionNeedsRefresh
                || this.lastCloneWaveTick == this.tickCount
                || !isCurrentRuneSelectionSource(level, this.arenaAnchor())) {
            return;
        }
        this.lastCloneWaveTick = this.tickCount;
        boolean cloneAlive = anyCloneAlive(level);
        if (cloneAlive) {
            this.cloneWaveActive = true;
            this.cloneRespawnCooldown = 200;
            return;
        }
        if (this.clonePresenceUnresolved) {
            return;
        }
        this.cloneIds.clear();
        if (this.cloneWaveActive) {
            this.cloneWaveActive = false;
            this.cloneRespawnCooldown = 200;
            if (this.corruptedRunes.isEmpty()) {
                this.triggerBroken(level);
            }
            return;
        }
        if (this.cloneRespawnCooldown > 0) {
            this.cloneRespawnCooldown = Math.max(0, this.cloneRespawnCooldown - 10);
            if (this.cloneRespawnCooldown > 0) {
                return;
            }
        }
        this.summonClones(level);
    }

    public static boolean isRuneGuarded(ServerLevel serverLevel, BlockPos pos) {
        return !serverLevel.getEntitiesOfClass(AbyssWatcher.class,
                new net.minecraft.world.phys.AABB(pos).inflate(CLONE_GUARD_RADIUS),
                w -> w.isClone() && w.isAlive()).isEmpty();
    }

    private boolean anyCloneAlive(ServerLevel serverLevel) {
        this.clonePresenceUnresolved = false;
        boolean unresolved = false;
        for (UUID id : this.cloneIds) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof AbyssWatcher clone && isCurrentOwnedClone(serverLevel, clone)) {
                return true;
            }
            if (entity == null) {
                unresolved = true;
            }
        }
        this.clonePresenceUnresolved = unresolved
                && !areRuneChunksReady(serverLevel, this.arenaAnchor(), RUNE_SCAN_RADIUS + 2);
        return false;
    }

    private void discardClones(ServerLevel serverLevel) {
        for (UUID id : List.copyOf(this.cloneIds)) {
            runDeathStep("clone cleanup", () -> {
                var entity = serverLevel.getEntity(id);
                if (entity == null) {
                    return; // 未加载拟影不强制加载，保留给移除时重试或原有归属自清理。
                }
                if (!(entity instanceof AbyssWatcher clone) || !clone.isClone()
                        || !this.getUUID().equals(clone.cloneOwnerUuid)) {
                    this.cloneIds.remove(id);
                    return;
                }
                if (clone.level() != serverLevel) {
                    return;
                }
                Vec3 clonePos = clone.position();
                clone.discard();
                if (clone.isRemoved()) {
                    this.cloneIds.remove(id);
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                            clonePos.x, clonePos.y + 0.6, clonePos.z, 12, 0.4, 0.4, 0.4, 0.02);
                }
            });
        }
    }

    private void cleanupArenaState(ServerLevel serverLevel) {
        if (this.cleaningArena) {
            return;
        }
        this.cleaningArena = true;
        if (this.arenaCleanupLevel == null) {
            this.arenaCleanupLevel = serverLevel;
        }
        try {
            WatcherRuneCleanupData.queue(serverLevel, this.getUUID(), this.corruptedRunes);
            this.corruptedRunes.clear();
            this.discardClones(serverLevel);
            WatcherRuneCleanupData.apply(serverLevel, this.getUUID());
        } finally {
            this.cleaningArena = false;
        }
    }

    private void broadcastNearby(Component message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : this.nearbyPlayers(serverLevel, radius)) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.handlingSpecialAttacks) { return; }
        this.handlingSpecialAttacks = true;
        boolean failed = false;
        try {
            if (!this.isSpecialAttackSource(serverLevel)
                    || this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) {
                this.cancelSpecialAttack(serverLevel);
                return;
            }
            if (this.lastSpecialAttackTick == this.tickCount) { return; }
            this.lastSpecialAttackTick = this.tickCount;
            if (this.vortexCooldown > 0) this.vortexCooldown--;
            if (this.burstCooldown > 0) this.burstCooldown--;
            if (this.dashCooldown > 0) this.dashCooldown--;
            if (this.bladeCooldown > 0) this.bladeCooldown--;
            this.syncVisualAttackState(serverLevel);
            if (this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }

            if (this.pendingDashTicks > 0) {
                int remaining = --this.pendingDashTicks;
                this.spawnDashWarning(serverLevel);
                if (!this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }
                if (remaining == 0) {
                    this.doTidalDash(serverLevel);
                }
            }
            if (this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }
            if (this.pendingBladeTicks > 0) {
                int remaining = --this.pendingBladeTicks;
                this.spawnBladeChargeBeams(serverLevel);
                if (!this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }
                if (remaining == 0) {
                    this.doWaterBlades(serverLevel);
                }
            }
            if (this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }
            if (this.pendingVortexTicks > 0 && --this.pendingVortexTicks == 0) {
                this.doVortexPull(serverLevel);
            }
            if (this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)) { return; }
            if (this.pendingBurstTicks > 0 && --this.pendingBurstTicks == 0) {
                this.doBurstDamage(serverLevel);
            }
            if (this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack)
                    || this.hasPendingSpecialAttack()) {
                return;
            }
            this.specialAttack = null;
            LivingEntity target = this.getTarget();
            if (!this.isSpecialAttackTarget(serverLevel, target)) {
                return;
            }

            int cooldownScale = this.getHealth() < this.getMaxHealth() * 0.3F ? 70 : 100;
            double distSq = this.distanceToSqr(target);
            if (this.burstCooldown <= 0 && distSq <= BURST_RANGE * BURST_RANGE) {
                this.pendingBurstTicks = BURST_HIT_DELAY;
                this.burstCooldown = 160 * cooldownScale / 100;
                this.beginSpecialAttack(serverLevel, target, "burst", ModSounds.ABYSS_WATCHER_BURST.get(), 1.4F);
            } else if (this.dashCooldown <= 0 && distSq <= DASH_RANGE * DASH_RANGE
                    && distSq > BURST_RANGE * BURST_RANGE && this.hasLineOfSight(target)) {

                this.dashDirection = target.position().add(0, target.getBbHeight() * 0.5, 0)
                        .subtract(this.position().add(0, this.getBbHeight() * 0.5, 0))
                        .multiply(1, 0.3, 1).normalize();
                this.pendingDashTicks = DASH_WINDUP_TICKS;
                this.dashCooldown = 200 * cooldownScale / 100;
                this.beginSpecialAttack(serverLevel, target, "dash", ModSounds.ABYSS_WATCHER_DASH.get(), 1.6F);
            } else if (this.vortexCooldown <= 0 && distSq <= VORTEX_RANGE * VORTEX_RANGE) {
                this.pendingVortexTicks = VORTEX_HIT_DELAY;
                this.vortexCooldown = 220 * cooldownScale / 100;
                this.beginSpecialAttack(serverLevel, target, "vortex", ModSounds.ABYSS_WATCHER_VORTEX.get(), 1.8F);
            } else if (this.bladeCooldown <= 0 && distSq <= BLADE_RANGE * BLADE_RANGE) {

                this.bladeTargetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                this.pendingBladeTicks = BLADE_WINDUP_TICKS;
                this.bladeCooldown = 240 * cooldownScale / 100;
                this.beginSpecialAttack(serverLevel, target, "blades", ModSounds.ABYSS_WATCHER_BLADES.get(), 1.2F);
            }
        } catch (RuntimeException | Error failure) {
            failed = true;
            this.resetSpecialAttack();
            throw failure;
        } finally {
            try {
                if (!failed && (!this.isSpecialAttackSource(serverLevel)
                        || this.specialAttack != null && !this.isCurrentSpecialAttack(serverLevel, this.specialAttack))) {
                    this.cancelSpecialAttack(serverLevel);
                }
                if (!this.hasPendingSpecialAttack()) { this.specialAttack = null; }
            } finally {
                this.handlingSpecialAttacks = false;
            }
        }
    }

    private boolean hasPendingSpecialAttack() {
        return this.pendingVortexTicks > 0 || this.pendingBurstTicks > 0
                || this.pendingDashTicks > 0 || this.pendingBladeTicks > 0;
    }

    private boolean isSpecialAttackSource(ServerLevel level) {
        return this.isCurrentRuneBoss(level) && !this.isNoAi() && !this.handlingRuneSelection
                && !this.handlingRuneTransition && !this.runeSelectionNeedsRefresh;
    }

    private boolean isSpecialAttackTarget(ServerLevel level, @Nullable LivingEntity target) {
        return this.isSpecialAttackSource(level) && target != null && target == this.getTarget()
                && target.level() == level && target.isAlive() && !target.isSpectator()
                && this.canAttack(target) && !this.isAlliedTo(target)
                && (!(target instanceof ServerPlayer player) || !player.hasDisconnected())
                && level.getEntity(target.getUUID()) == target;
    }

    private boolean isCurrentSpecialAttack(ServerLevel level, @Nullable SpecialAttack attack) {
        return attack != null && this.specialAttack == attack && attack.level() == level
                && this.isBroken() == attack.broken() && this.isSpecialAttackTarget(level, attack.target());
    }

    private void beginSpecialAttack(ServerLevel level, LivingEntity target, String animation, SoundEvent sound, float volume) {
        SpecialAttack attack = new SpecialAttack(level, target, this.isBroken());
        this.specialAttack = attack;
        if (!this.isCurrentSpecialAttack(level, attack)) { return; }
        this.getNavigation().stop();
        if (!this.isCurrentSpecialAttack(level, attack)) { return; }
        this.triggerAnim("attack_controller", animation);
        if (!this.isCurrentSpecialAttack(level, attack)) { return; }
        this.playSound(sound, volume, 1.0F);
    }

    private void resetSpecialAttack() {
        this.specialAttack = null;
        this.pendingVortexTicks = 0;
        this.pendingBurstTicks = 0;
        this.pendingDashTicks = 0;
        this.pendingBladeTicks = 0;
        this.dashDirection = Vec3.ZERO;
        this.bladeTargetPos = Vec3.ZERO;
    }

    private void cancelSpecialAttack(ServerLevel level) {
        boolean visible = this.specialAttackVisuals || this.specialAttack != null || this.hasPendingSpecialAttack();
        this.lastSpecialAttackTick = this.tickCount;
        this.resetSpecialAttack();
        if (visible) { this.syncVisualAttackState(level); }
    }

    private void stopSpecialAttacks(ServerLevel level) {
        if (this.handlingSpecialAttacks) { return; }
        this.handlingSpecialAttacks = true;
        try {
            this.cancelSpecialAttack(level);
        } finally {
            this.handlingSpecialAttacks = false;
        }
    }

    private void reconcileSpecialAttack(ServerLevel level) {
        if ((this.specialAttack != null || this.specialAttackVisuals || this.hasPendingSpecialAttack())
                && (!this.isSpecialAttackSource(level)
                || this.specialAttack != null && !this.isCurrentSpecialAttack(level, this.specialAttack))) {
            this.stopSpecialAttacks(level);
        }
    }

    private record AttackRelease(SpecialAttack attack, Vec3 origin) {}
    private record BladeSource(AbyssWatcher entity, Vec3 origin) {}
    private static final int MAX_BLADE_PARTICLE_STEPS = 128;

    @Nullable
    private AttackRelease beginAttackRelease(ServerLevel level) {
        SpecialAttack attack = this.specialAttack;
        return this.isCurrentSpecialAttack(level, attack) ? new AttackRelease(attack, this.position()) : null;
    }

    private boolean isCurrentAttackRelease(ServerLevel level, AttackRelease release) {
        // 已释放的范围攻击继续结算其他人；首个受击者死亡不撤销整次攻击。
        return this.specialAttack == release.attack() && release.attack().level() == level
                && this.isSpecialAttackSource(level) && this.isBroken() == release.attack().broken()
                && this.position().equals(release.origin());
    }

    private boolean canHitSpecialAttack(ServerLevel level, LivingEntity victim, AABB bounds) {
        return victim != this && !(victim instanceof net.minecraft.world.entity.monster.Enemy)
                && victim.level() == level && victim.isAlive() && !victim.isSpectator()
                && this.canAttack(victim) && !this.isAlliedTo(victim)
                && (!(victim instanceof ServerPlayer player) || !player.hasDisconnected())
                && level.getEntity(victim.getUUID()) == victim && victim.getBoundingBox().intersects(bounds);
    }

    private boolean canHitAttackLine(ServerLevel level, LivingEntity victim, AABB bounds,
                                     Vec3 from, Vec3 direction, double length, double width) {
        if (!this.canHitSpecialAttack(level, victim, bounds)) { return false; }
        Vec3 offset = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(from);
        double along = offset.dot(direction);
        return along >= 0 && along <= length && offset.subtract(direction.scale(along)).length() <= width;
    }

    private boolean canHitAttackArea(ServerLevel level, LivingEntity victim, AABB bounds, Vec3 center, double radius) {
        return this.canHitSpecialAttack(level, victim, bounds)
                && victim.position().subtract(center).multiply(1, 0, 1).lengthSqr() <= radius * radius;
    }

    private List<BladeSource> bladeSources(ServerLevel level) {
        List<BladeSource> sources = new ArrayList<>();
        for (UUID id : List.copyOf(this.cloneIds)) {
            if (level.getEntity(id) instanceof AbyssWatcher clone && this.isCurrentOwnedClone(level, clone)
                    && !clone.isNoAi()) {
                sources.add(new BladeSource(clone, clone.eyeBeamOrigin()));
            }
        }
        return sources;
    }

    private boolean isCurrentBladeSource(ServerLevel level, BladeSource source) {
        return this.isCurrentOwnedClone(level, source.entity()) && !source.entity().isNoAi()
                && source.entity().eyeBeamOrigin().equals(source.origin());
    }

    private void spawnDashWarning(ServerLevel serverLevel) {
        SpecialAttack attack = this.specialAttack;
        if (!this.isCurrentSpecialAttack(serverLevel, attack)) { return; }
        Vec3 eye = this.position().add(0, this.getBbHeight() * 0.5, 0);
        BossFx.chargeSpiral(serverLevel, eye, this.tickCount,
                BossFx.waterBubbleOr(serverLevel, eye, BossParticleTheme.ABYSS.body()));
        for (int d = 1; d <= (int) DASH_RANGE; d += 2) {
            if (!this.isCurrentSpecialAttack(serverLevel, attack)) { return; }
            Vec3 p = eye.add(this.dashDirection.scale(d));
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 3, 0.4, 0.2, 0.4, 0.0);
            if (d % 4 == 1) {
                if (!this.isCurrentSpecialAttack(serverLevel, attack)) { return; }
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        if (!this.isCurrentSpecialAttack(serverLevel, attack)) { return; }
        serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, eye, BossParticleTheme.ABYSS.body()),
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                6, this.getBbWidth() * 0.5, 0.4, this.getBbWidth() * 0.5, 0.05);
    }

    private void doTidalDash(ServerLevel serverLevel) {
        AttackRelease release = this.beginAttackRelease(serverLevel);
        if (release == null) { return; }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9F;
        Vec3 start = release.origin().add(0, this.getBbHeight() * 0.5, 0);
        Vec3 direction = this.dashDirection;
        AABB bounds = this.getBoundingBox().inflate(DASH_RANGE, 4.0, DASH_RANGE);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> this.canHitAttackLine(serverLevel, e, bounds, start, direction, DASH_RANGE, 2.2))) {
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (!this.canHitAttackLine(serverLevel, victim, bounds, start, direction, DASH_RANGE, 2.2)) { continue; }
            boolean hurt = victim.hurt(this.damageSources().mobAttack(this), damage);
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (hurt && this.canHitAttackLine(serverLevel, victim, bounds, start, direction, DASH_RANGE, 2.2)) {
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(direction.x * 0.9, 0.4, direction.z * 0.9));
                if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
                if (this.canHitAttackLine(serverLevel, victim, bounds, start, direction, DASH_RANGE, 2.2)) {
                    victim.hurtMarked = true;
                }
            }
        }

        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        this.setDeltaMovement(direction.scale(2.2));
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        this.hurtMarked = true;
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.ABYSS_WATCHER_ATTACK_DASH_RELEASE.get(), SoundSource.HOSTILE, 1.8F, 1.0F);
        for (int d = 0; d <= (int) DASH_RANGE; d++) {
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            Vec3 p = start.add(direction.scale(d));
            serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, p, BossParticleTheme.ABYSS.body()),
                    p.x, p.y, p.z, 6, 0.6, 0.6, 0.6, 0.1);
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.02);
        }
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.sparkSpray(serverLevel, start, direction, DASH_RANGE, BossParticleTheme.ABYSS.spark());
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        Vec3 dashCenter = start.add(direction.scale(DASH_RANGE * 0.5D));
        BossFx.cinematicImpact(serverLevel, dashCenter, 30.0D, 0.58F, 7, 0x43BFD1);
    }

    private void spawnBladeChargeBeams(ServerLevel serverLevel) {
        SpecialAttack attack = this.specialAttack;
        if (this.tickCount % 2 != 0 || !this.isCurrentSpecialAttack(serverLevel, attack)) {
            return;
        }
        Vec3 target = this.bladeTargetPos;
        List<BladeSource> sources = this.bladeSources(serverLevel);
        spawnBeam(serverLevel, this.eyeBeamOrigin(), target, () -> this.isCurrentSpecialAttack(serverLevel, attack));
        for (BladeSource source : sources) {
            if (!this.isCurrentSpecialAttack(serverLevel, attack)) { return; }
            if (this.isCurrentBladeSource(serverLevel, source)) {
                spawnBeam(serverLevel, source.origin(), target, () -> this.isCurrentSpecialAttack(serverLevel, attack)
                        && this.isCurrentBladeSource(serverLevel, source));
            }
        }
    }

    private Vec3 eyeBeamOrigin() {
        return this.position().add(0, this.getBbHeight() * 0.55, 0);
    }

    private void spawnBeam(ServerLevel serverLevel, Vec3 from, Vec3 to, java.util.function.BooleanSupplier current) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (!Double.isFinite(length)) { return; }
        int steps = Math.min(MAX_BLADE_PARTICLE_STEPS, Math.max(4, (int) (length * 1.5)));
        for (int i = 0; i <= steps; i++) {
            if (!current.getAsBoolean()) { return; }
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void damageAlongBlade(ServerLevel serverLevel, Vec3 from, Vec3 target, float damage, boolean applySlow,
                                  java.util.function.BooleanSupplier current) {
        if (!current.getAsBoolean()) { return; }
        Vec3 delta = target.subtract(from);
        double length = delta.length();
        if (!Double.isFinite(length) || length < 1.0E-4) {
            return;
        }
        Vec3 dir = delta.normalize();
        AABB bounds = new AABB(from, target.add(dir.scale(2.0))).inflate(1.6);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> this.canHitAttackLine(serverLevel, e, bounds, from, dir, length + 2.0, 1.6))) {
            if (!current.getAsBoolean()) { return; }
            if (!this.canHitAttackLine(serverLevel, victim, bounds, from, dir, length + 2.0, 1.6)) { continue; }
            boolean hurt = victim.hurt(this.damageSources().mobAttack(this), damage);
            if (!current.getAsBoolean()) { return; }
            if (hurt && applySlow && this.canHitAttackLine(serverLevel, victim, bounds, from, dir, length + 2.0, 1.6)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
            }
        }
    }

    private void doWaterBlades(ServerLevel serverLevel) {
        AttackRelease release = this.beginAttackRelease(serverLevel);
        if (release == null) { return; }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        Vec3 from = this.eyeBeamOrigin();
        Vec3 target = this.bladeTargetPos;
        List<BladeSource> sources = this.bladeSources(serverLevel);
        java.util.function.BooleanSupplier current = () -> this.isCurrentAttackRelease(serverLevel, release);
        this.damageAlongBlade(serverLevel, from, target, damage, true, current);
        spawnBeam(serverLevel, from, target, current);

        for (BladeSource source : sources) {
            if (!current.getAsBoolean()) { return; }
            if (this.isCurrentBladeSource(serverLevel, source)) {
                java.util.function.BooleanSupplier currentClone = () -> current.getAsBoolean()
                        && this.isCurrentBladeSource(serverLevel, source);
                this.damageAlongBlade(serverLevel, source.origin(), target, (float) CLONE_ATTACK_DAMAGE, false, currentClone);
                spawnBeam(serverLevel, source.origin(), target, currentClone);
            }
        }
        if (!current.getAsBoolean()) { return; }
        serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                target.x, target.y, target.z,
                14, 0.5, 0.5, 0.5, 0.12);
        if (!current.getAsBoolean()) { return; }
        serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                target.x, target.y, target.z,
                20, 0.8, 0.8, 0.8, 0.15);
        if (!current.getAsBoolean()) { return; }
        BossFx.terrainImpact(serverLevel, target, BossParticleTheme.ABYSS.spark());
        if (!current.getAsBoolean()) { return; }
        BossFx.cinematicImpact(serverLevel, target, 26.0D, 0.52F, 7, 0x55C7D5);
        if (!current.getAsBoolean()) { return; }
        serverLevel.playSound(null, BlockPos.containing(target),
                ModSounds.ABYSS_WATCHER_ATTACK_BLADES_HIT.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
    }

    private void doVortexPull(ServerLevel serverLevel) {
        AttackRelease release = this.beginAttackRelease(serverLevel);
        if (release == null) { return; }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        Vec3 center = release.origin();
        AABB bounds = this.getBoundingBox().inflate(VORTEX_RANGE, VORTEX_RANGE * 0.6, VORTEX_RANGE);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> this.canHitAttackArea(serverLevel, e, bounds, center, VORTEX_RANGE))) {
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (!this.canHitAttackArea(serverLevel, victim, bounds, center, VORTEX_RANGE)) { continue; }
            Vec3 pull = center.add(0, this.getBbHeight() * 0.5, 0)
                    .subtract(victim.position());
            if (pull.lengthSqr() < 1.0E-4) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (!this.canHitAttackArea(serverLevel, victim, bounds, center, VORTEX_RANGE)) { continue; }
            // 牵引不以 hurt 返回 true 为前提，但不能作用于回调后失效的目标。
            pull = center.add(0, this.getBbHeight() * 0.5, 0).subtract(victim.position());
            if (pull.lengthSqr() < 1.0E-4) { continue; }
            Vec3 dir = pull.normalize();
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(dir.x * 1.1, dir.y * 0.6 + 0.1, dir.z * 1.1));
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (this.canHitAttackArea(serverLevel, victim, bounds, center, VORTEX_RANGE)) { victim.hurtMarked = true; }
        }
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        serverLevel.playSound(null, BlockPos.containing(center),
                ModSounds.ABYSS_WATCHER_ATTACK_VORTEX_PULL.get(), SoundSource.HOSTILE, 2.0F, 1.0F);

        for (int ring = 0; ring < 3; ring++) {
            double r = VORTEX_RANGE - ring * 2.5;
            for (int i = 0; i < 14; i++) {
                if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
                double angle = Math.PI * 2 * i / 14 + ring * 0.5;
                Vec3 point = new Vec3(this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r);
                serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, point, BossParticleTheme.ABYSS.body()),
                        point.x, point.y, point.z, 2, 0.1, 0.3, 0.1, 0.02);
            }
        }
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.groundShockwave(serverLevel, center.add(0, 0.2, 0), VORTEX_RANGE * 0.7,
                BossFx.waterBubbleOr(serverLevel, center, BossParticleTheme.ABYSS.body()));
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.cinematicImpact(serverLevel, center, 34.0D, 0.62F, 8, 0x2B92A6);
    }

    private void doBurstDamage(ServerLevel serverLevel) {
        AttackRelease release = this.beginAttackRelease(serverLevel);
        if (release == null) { return; }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        Vec3 center = release.origin();
        AABB bounds = this.getBoundingBox().inflate(BURST_RANGE, 2.0, BURST_RANGE);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> this.canHitAttackArea(serverLevel, e, bounds, center, BURST_RANGE))) {
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (!this.canHitAttackArea(serverLevel, victim, bounds, center, BURST_RANGE)) { continue; }
            boolean hurt = victim.hurt(this.damageSources().mobAttack(this), damage);
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            if (hurt && this.canHitAttackArea(serverLevel, victim, bounds, center, BURST_RANGE)) {
                Vec3 away = victim.position().subtract(center).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement()
                            .add(dir.x * 1.6, 0.5, dir.z * 1.6));
                    if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
                    if (!this.canHitAttackArea(serverLevel, victim, bounds, center, BURST_RANGE)) { continue; }
                    victim.hurtMarked = true;
                }
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        serverLevel.playSound(null, BlockPos.containing(center),
                ModSounds.ABYSS_WATCHER_ATTACK_BURST_HIT.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        for (int i = 0; i < 20; i++) {
            if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
            double angle = Math.PI * 2 * i / 20;
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                    this.getX() + Math.cos(angle) * 2.5,
                    this.getY() + this.getBbHeight() * 0.5,
                    this.getZ() + Math.sin(angle) * 2.5, 3, 0.2, 0.3, 0.2, 0.08);
        }
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.groundShockwave(serverLevel, center.add(0, 0.2, 0), BURST_RANGE, BossParticleTheme.ABYSS.ember());
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.impactBurst(serverLevel, center.add(0, this.getBbHeight() * 0.5, 0), BossParticleTheme.ABYSS.spark());
        if (!this.isCurrentAttackRelease(serverLevel, release)) { return; }
        BossFx.cinematicImpact(serverLevel, center, 30.0D, 0.82F, 10, 0x247F91);
    }

    @Override
    public void aiStep() {
        if (this.handlingSpecialAttacks) { return; }
        if (this.level() instanceof ServerLevel attackLevel) {
            this.reconcileSpecialAttack(attackLevel);
        }
        if (this.handlingDeath || this.dead || !this.isAlive() || this.handlingRuneSelection || this.handlingRuneTransition
                || this.handlingRuneGuidance || this.handlingCloneLifecycle || this.handlingCloneSummon) {
            return;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.tickRechallengeBinding(serverLevel,
                        () -> !this.handlingDeath && !this.dead)) {
            this.stopSpecialAttacks(serverLevel);
            return;
        }
        super.aiStep();
        if (this.level() instanceof ServerLevel attackLevel) {
            this.reconcileSpecialAttack(attackLevel);
        }
        if (this.handlingDeath || this.dead || !this.isAlive() || this.handlingRuneSelection || this.handlingRuneTransition
                || this.handlingRuneGuidance || this.handlingCloneLifecycle || this.handlingCloneSummon) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }

        if (this.isClone()) {
            tickCloneLifecycle();
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
        }
        if (this.veiledMessageCooldown > 0) {
            this.veiledMessageCooldown--;
        }

        // 沉湖苏醒:第一次锁定目标时演一次,不改 AI 与数值。
        if (!this.awakenPlayed && this.getTarget() != null) {
            this.awakenPlayed = true;
            this.triggerAnim("state_controller", "awaken");
        }
        // 深渊回流:低血阈值必须与 tickSpecialAttacks 的 cooldownScale 用同一个 0.3F,
        // 否则演出与冷却缩短会在不同血量触发。
        if (!this.enragePlayed && this.isAlive()
                && this.getHealth() < this.getMaxHealth() * 0.3F) {
            this.enragePlayed = true;
            this.triggerAnim("state_controller", "enrage");
        }

        this.retryRuneSelection(serverLevel);
        this.tickRunePurification(serverLevel);

        if (this.isAlive() && this.tickCount % 5 == 0) {
            if (this.isBroken()) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        4, this.getBbWidth() * 0.5, this.getBbHeight() * 0.35, this.getBbWidth() * 0.5, 0.04);
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        3, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.08);
            } else {

                double angle = this.tickCount * 0.35;
                double radius = this.getBbWidth() * 0.75;
                double yOffset = (this.tickCount % 40) / 40.0 * this.getBbHeight();
                for (int strand = 0; strand < 2; strand++) {
                    double a = angle + strand * Math.PI;
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                            this.getX() + Math.cos(a) * radius,
                            this.getY() + yOffset,
                            this.getZ() + Math.sin(a) * radius, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }

        boolean guidanceTick = !this.isBroken() && this.tickCount % 5 == 0;
        boolean hasNearbyRuneViewer = guidanceTick && this.hasNearbyRuneViewer(serverLevel);
        if (this.isBroken()) {
            if (--this.brokenTicksRemaining <= 0 && this.isAlive()) {
                restoreTideVeil(serverLevel);
            }
        } else {

            if (this.tickCount % 10 == 0 && hasNearbyRuneViewer && this.corruptedRunes.isEmpty()
                    && !this.runeSelectionNeedsRefresh) {
                if (this.runeRescanCooldown > 0) {
                    this.runeRescanCooldown -= 10;
                } else {
                    this.corruptRunes(serverLevel);
                    if (this.corruptedRunes.isEmpty() && !this.runeSelectionNeedsRefresh) {
                        this.runeRescanCooldown = 600;
                    }
                }
            }
            this.tickCloneWave(serverLevel);
        }
        if (guidanceTick && hasNearbyRuneViewer && !this.corruptedRunes.isEmpty()
                && !this.runeSelectionNeedsRefresh) {
            this.tickRuneGuidance(serverLevel);
        }

        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
        }

        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : this.nearbyPlayers(serverLevel, radius)) {
                if (!player.isSpectator()) {
                    BossNbtData.addParticipant(this.participants, player.getUUID());
                }
            }
            if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                this.updateDynamicScaling();
            }
        }
        this.tickSpecialAttacks(serverLevel);
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /**
     * 拟影的独立生命周期:本体失效或寿命到点即消散，符文失效时解除守位。
     * 拟影不打招式、不吃阶段、不参与结算,不要并回 aiStep。
     */
    private void tickCloneLifecycle() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.handlingCloneLifecycle
                || !isCurrentClone(serverLevel, this)) {
            return;
        }
        this.handlingCloneLifecycle = true;
        try {
            // 寿命先提交，粒子回调异常或重入不能延长拟影寿命。
            if (--this.cloneLifeTicks <= 0) {
                this.expireClone(serverLevel);
                return;
            }
            if (this.cloneOwnerUuid != null && this.tickCount % 20 == 0) {
                Entity owner = serverLevel.getEntity(this.cloneOwnerUuid);
                boolean knownInvalid = owner != null && (!(owner instanceof AbyssWatcher watcher)
                        || watcher.level() != serverLevel || watcher.isClone() || !watcher.isAlive());
                if (knownInvalid || owner == null && areRuneChunksReady(serverLevel,
                        this.homePos != null ? this.homePos : this.blockPosition(), RUNE_SCAN_RADIUS + 2)) {
                    this.expireClone(serverLevel);
                    return;
                }
            }

            if (this.isAlive() && this.tickCount % 10 == 0) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        2, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.01);
            }
            if (!isCurrentClone(serverLevel, this)) {
                return;
            }
            if (this.guardPos != null && this.tickCount % 20 == 0) {
                BlockState state = readableArenaState(serverLevel, this.guardPos);
                if (state != null && (!(state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock)
                        || !state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED))) {
                    this.guardPos = null;
                }
            }
        } finally {
            this.handlingCloneLifecycle = false;
        }
    }

    private void expireClone(ServerLevel level) {
        Vec3 center = this.position();
        this.discard();
        if (this.isRemoved()) {
            level.sendParticles(BossParticleTheme.ABYSS.body(),
                    center.x, center.y + 0.6, center.z, 12, 0.4, 0.4, 0.4, 0.02);
        }
    }

    private static boolean isCurrentClone(ServerLevel level, AbyssWatcher clone) {
        return clone.level() == level && clone.isAlive() && !clone.dead && !clone.handlingDeath
                && clone.isClone() && level.getEntity(clone.getUUID()) == clone;
    }

    private boolean isCurrentOwnedClone(ServerLevel level, AbyssWatcher clone) {
        return isCurrentClone(level, clone) && this.getUUID().equals(clone.cloneOwnerUuid);
    }

    private static boolean areRuneChunksReady(ServerLevel level, BlockPos center, int radius) {
        for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++) {
            for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 脱战回血:判据与配置口径由 OutOfCombatRegen 统一持有。 */
    private void tickOutOfCombatRegen() {
        this.outOfCombatTicks = OutOfCombatRegen.tick(this, this.outOfCombatTicks);
    }


    /** 从竞技场边缘均匀指定机制符文,避免把整圈装饰符文都变成净化目标。 */
    private void corruptRunes(ServerLevel serverLevel) {
        this.corruptRunes(serverLevel, true);
    }

    private void corruptRunes(ServerLevel serverLevel, boolean announce) {
        BlockPos center = arenaAnchor();
        if (this.handlingRuneSelection || !isCurrentRuneSelectionSource(serverLevel, center)) {
            return;
        }
        this.handlingRuneSelection = true;
        this.runeSelectionNeedsRefresh = true;
        this.runeRescanCooldown = 40;
        try {
            List<BlockPos> candidates = scanRuneCandidates(serverLevel, center);
            if (candidates == null) {
                return;
            }
            List<BlockPos> selected = selectMechanicRunes(center, candidates);
            for (BlockPos pos : selected) {
                if (!this.corruptedRunes.contains(pos)) {
                    this.corruptedRunes.add(pos);
                }
            }
            WatcherRuneCleanupData.claim(serverLevel, this.getUUID(), this.corruptedRunes);
            if (announce && !selected.isEmpty()) {
                this.triggerAnim("state_controller", "rune_corrupt");
            }
            Set<BlockPos> chosen = new HashSet<>(selected);
            for (BlockPos pos : candidates) {
                if (!isCurrentRuneSelectionSource(serverLevel, center)) {
                    return;
                }
                BlockState state = readableRuneState(serverLevel, pos);
                if (state == null) {
                    return;
                }
                boolean corrupt = chosen.contains(pos);
                BlockState desired = state.setValue(
                        cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED, corrupt);
                if (state != desired && !serverLevel.setBlock(pos, desired, 3)) {
                    return;
                }
                if (!isCurrentRuneSelectionSource(serverLevel, center)
                        || readableRuneState(serverLevel, pos) != desired) {
                    return;
                }
                if (announce && corrupt) {
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.04);
                }
            }
            if (!isRuneSelectionComplete(serverLevel, center, candidates, chosen)) {
                return;
            }
            if (announce && !selected.isEmpty()) {
                serverLevel.playSound(null, center,
                        SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.7F);
                broadcastRuneSelection(serverLevel, center,
                        Component.translatable("message.unknown_echoes.watcher.runes_corrupted"));
            }
            if (announce && !isRuneSelectionComplete(serverLevel, center, candidates, chosen)) {
                return;
            }
            this.corruptedRunes.clear();
            this.corruptedRunes.addAll(selected);
            WatcherRuneCleanupData.claim(serverLevel, this.getUUID(), this.corruptedRunes);
            this.runeSelectionNeedsRefresh = false;
            this.runeRescanCooldown = 0;
        } finally {
            this.handlingRuneSelection = false;
        }
    }

    private void retryRuneSelection(ServerLevel level) {
        if (!this.isBroken() && this.runeSelectionNeedsRefresh && !this.handlingRuneSelection) {
            if (this.runeRescanCooldown > 0) {
                this.runeRescanCooldown--;
            } else {
                this.corruptRunes(level, false);
            }
        }
    }

    private boolean isRuneSelectionComplete(ServerLevel level, BlockPos center,
                                            List<BlockPos> candidates, Set<BlockPos> chosen) {
        if (!isCurrentRuneSelectionSource(level, center)) {
            return false;
        }
        for (BlockPos pos : candidates) {
            BlockState state = readableRuneState(level, pos);
            if (state == null || state.getValue(
                    cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED) != chosen.contains(pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCurrentRuneSelectionSource(ServerLevel level, BlockPos center) {
        return this.level() == level && this.isAlive() && !this.dead && !this.isRemoved()
                && !this.handlingDeath && !this.cleaningArena && !this.isClone() && !this.isBroken()
                && this.arenaAnchor().equals(center)
                && level.getChunkSource().getChunkNow(this.blockPosition().getX() >> 4,
                        this.blockPosition().getZ() >> 4) != null
                && level.getEntity(this.getUUID()) == this;
    }

    @Nullable
    private static BlockState readableRuneState(ServerLevel level, BlockPos pos) {
        BlockState state = readableArenaState(level, pos);
        return state != null && state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                ? state : null;
    }

    @Nullable
    private static BlockState readableArenaState(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return null;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(pos);
    }

    @Nullable
    private static List<BlockPos> scanRuneCandidates(ServerLevel level, BlockPos center) {
        int minChunkX = (center.getX() - RUNE_SCAN_RADIUS) >> 4;
        int minChunkZ = (center.getZ() - RUNE_SCAN_RADIUS) >> 4;
        int maxChunkX = (center.getX() + RUNE_SCAN_RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + RUNE_SCAN_RADIUS) >> 4;
        LevelChunk[][] chunks = new LevelChunk[maxChunkX - minChunkX + 1][maxChunkZ - minChunkZ + 1];
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    return null;
                }
                chunks[x - minChunkX][z - minChunkZ] = chunk;
            }
        }
        List<BlockPos> candidates = new ArrayList<>();
        int minY = Math.max(center.getY() - RUNE_SCAN_HEIGHT, level.getMinBuildHeight());
        int maxY = Math.min(center.getY() + RUNE_SCAN_HEIGHT, level.getMaxBuildHeight() - 1);
        if (minY > maxY) {
            return candidates;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                new BlockPos(center.getX() - RUNE_SCAN_RADIUS, minY, center.getZ() - RUNE_SCAN_RADIUS),
                new BlockPos(center.getX() + RUNE_SCAN_RADIUS, maxY, center.getZ() + RUNE_SCAN_RADIUS))) {
            BlockState state = chunks[(pos.getX() >> 4) - minChunkX][(pos.getZ() >> 4) - minChunkZ]
                    .getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock) {
                candidates.add(pos.immutable());
            }
        }
        return candidates;
    }

    private void broadcastRuneSelection(ServerLevel level, BlockPos center, Component message) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : nearbyPlayers(level, radius)) {
            if (!isCurrentRuneSelectionSource(level, center)) {
                return;
            }
            if (player.level() == level && player.isAlive() && !player.isSpectator()
                    && !player.isRemoved() && !player.hasDisconnected()
                    && player.distanceToSqr(this) <= radius * radius) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static List<BlockPos> selectMechanicRunes(BlockPos center, List<BlockPos> candidates) {
        int targetCount = Math.min(RUNE_MECHANIC_COUNT, candidates.size());
        if (targetCount == 0) {
            return List.of();
        }

        List<BlockPos> preferred = new ArrayList<>();
        double minRadiusSq = RUNE_PREFERRED_MIN_RADIUS * RUNE_PREFERRED_MIN_RADIUS;
        double maxRadiusSq = RUNE_PREFERRED_MAX_RADIUS * RUNE_PREFERRED_MAX_RADIUS;
        for (BlockPos pos : candidates) {
            double distanceSq = horizontalDistanceSq(center, pos);
            if (Math.abs(pos.getY() - center.getY()) <= RUNE_PREFERRED_MAX_HEIGHT
                    && distanceSq >= minRadiusSq && distanceSq <= maxRadiusSq) {
                preferred.add(pos);
            }
        }
        if (candidates.size() <= targetCount) {
            return new ArrayList<>(candidates);
        }

        Set<BlockPos> preferredSet = new HashSet<>(preferred);
        List<BlockPos> selected = new ArrayList<>(targetCount);
        for (int sector = 0; sector < targetCount; sector++) {
            double targetAngle = Math.PI * 2.0D * sector / targetCount;
            BlockPos best = null;
            double bestAngleDistance = Double.MAX_VALUE;
            double bestRadiusDistance = Double.MAX_VALUE;
            for (BlockPos pos : candidates) {
                if (selected.contains(pos)) {
                    continue;
                }
                double angle = Math.atan2(pos.getZ() - center.getZ(), pos.getX() - center.getX());
                double angleDistance = circularAngleDistance(angle, targetAngle);
                double radiusDistance = Math.abs(Math.sqrt(horizontalDistanceSq(center, pos))
                        - RUNE_PREFERRED_RADIUS);
                boolean isPreferred = preferredSet.contains(pos);
                boolean bestIsPreferred = best != null && preferredSet.contains(best);
                if (best == null
                        || (isPreferred && !bestIsPreferred)
                        || (isPreferred == bestIsPreferred
                        && (angleDistance < bestAngleDistance
                        || (Math.abs(angleDistance - bestAngleDistance) < 1.0E-6
                        && radiusDistance < bestRadiusDistance)))) {
                    best = pos;
                    bestAngleDistance = angleDistance;
                    bestRadiusDistance = radiusDistance;
                }
            }
            if (best != null) {
                selected.add(best);
            }
        }
        return selected;
    }

    private static double horizontalDistanceSq(BlockPos first, BlockPos second) {
        double dx = second.getX() - first.getX();
        double dz = second.getZ() - first.getZ();
        return dx * dx + dz * dz;
    }

    private static double circularAngleDistance(double first, double second) {
        double distance = Math.abs(first - second) % (Math.PI * 2.0D);
        return Math.min(distance, Math.PI * 2.0D - distance);
    }

    private BlockPos arenaAnchor() {
        return this.homePos != null ? this.homePos : this.blockPosition();
    }


    private void tickRuneGuidance(ServerLevel serverLevel) {
        BlockPos center = this.arenaAnchor();
        if (this.handlingRuneGuidance || this.corruptedRunes.isEmpty()
                || !isCurrentGuidanceSource(serverLevel, center)) {
            return;
        }
        this.handlingRuneGuidance = true;
        try {
            List<AbyssWatcher> guards = new ArrayList<>();
            for (UUID id : List.copyOf(this.cloneIds)) {
                if (serverLevel.getEntity(id) instanceof AbyssWatcher clone
                        && isCurrentOwnedClone(serverLevel, clone)) {
                    guards.add(clone);
                }
            }
            Vec3 core = this.eyeBeamOrigin();
            for (BlockPos pos : List.copyOf(this.corruptedRunes)) {
                if (!isCurrentGuidanceSource(serverLevel, center)) {
                    return;
                }
                if (!isCurrentGuidanceRune(serverLevel, pos)) {
                    continue;
                }
                Vec3 rune = Vec3.atCenterOf(pos).add(0, 0.35, 0);
                BossFx.guidanceMarker(serverLevel, rune, BossParticleTheme.ABYSS.ember());
                if (!isCurrentGuidanceSource(serverLevel, center)) {
                    return;
                }
                if (!isCurrentGuidanceRune(serverLevel, pos)) {
                    continue;
                }
                AbyssWatcher guard = nearestRuneGuard(serverLevel, guards, rune);
                Vec3 origin = guard != null ? guard.eyeBeamOrigin() : core;
                BossFx.guidanceFlow(serverLevel, origin, rune,
                        BossParticleTheme.ABYSS.ember(), BossParticleTheme.ABYSS.ember(), this.tickCount);
            }
        } finally {
            this.handlingRuneGuidance = false;
        }
    }

    private boolean isCurrentGuidanceSource(ServerLevel level, BlockPos center) {
        return !this.handlingRuneSelection && !this.handlingRuneTransition && !this.runeSelectionNeedsRefresh
                && isCurrentRuneSelectionSource(level, center);
    }

    private boolean isCurrentGuidanceRune(ServerLevel level, BlockPos pos) {
        if (!this.corruptedRunes.contains(pos)) {
            return false;
        }
        BlockState state = readableRuneState(level, pos);
        return state != null && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED);
    }

    private boolean hasNearbyRuneViewer(ServerLevel level) {
        double radius = RUNE_SCAN_RADIUS + 8.0D;
        for (ServerPlayer player : this.nearbyPlayers(level, radius)) {
            if (!player.isSpectator() && player.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private List<ServerPlayer> nearbyPlayers(ServerLevel level, double radius) {
        AABB bounds = this.getBoundingBox().inflate(radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(ServerPlayer.class, bounds,
                player -> player.distanceToSqr(this) <= radiusSqr);
    }

    private AbyssWatcher nearestRuneGuard(ServerLevel level, List<AbyssWatcher> guards, Vec3 rune) {
        AbyssWatcher nearest = null;
        double nearestDistance = CLONE_GUARD_RADIUS * CLONE_GUARD_RADIUS;
        for (AbyssWatcher guard : guards) {
            if (!isCurrentOwnedClone(level, guard)) {
                continue;
            }
            double distance = guard.distanceToSqr(rune);
            if (distance <= nearestDistance) {
                nearest = guard;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public void onRunePurified(BlockPos sourcePos, Player purifier) {
        if (this.handlingDeath || this.cleaningArena || this.dead || this.isRemoved() || !this.isAlive()
                || this.isClone() || this.level().isClientSide || this.isBroken()
                || this.handlingRuneSelection || this.runeSelectionNeedsRefresh
                || this.handlingRuneTransition || !(this.level() instanceof ServerLevel serverLevel)
                || !isCurrentRuneBoss(serverLevel) || !this.corruptedRunes.contains(sourcePos)
                || !(purifier instanceof ServerPlayer player) || player.level() != serverLevel
                || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || !player.canInteractWithBlock(sourcePos, 1.0D)) {
            return;
        }
        BlockState source = readableRuneState(serverLevel, sourcePos);
        if (source == null || source.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
            return;
        }
        BossNbtData.addParticipant(this.participants, purifier.getUUID());
        int remaining = remainingCorruptedRunes(serverLevel);
        if (remaining == 0) {
            this.triggerBroken(serverLevel);
        } else {
            broadcastRunePhase(serverLevel, false,
                    Component.translatable("message.unknown_echoes.watcher.rune_progress", remaining));
        }
    }

    private void tickRunePurification(ServerLevel level) {
        if (this.tickCount % 10 == 0 && !this.corruptedRunes.isEmpty()
                && !this.isBroken() && !this.handlingRuneSelection && !this.runeSelectionNeedsRefresh
                && !this.handlingRuneTransition && isCurrentRuneBoss(level)
                && remainingCorruptedRunes(level) == 0) {
            this.triggerBroken(level);
        }
    }

    private int remainingCorruptedRunes(ServerLevel level) {
        int remaining = 0;
        java.util.Map<Long, LevelChunk> chunks = new java.util.HashMap<>();
        for (BlockPos pos : this.corruptedRunes) {
            long key = net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (!chunks.containsKey(key)) {
                chunks.put(key, level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4));
            }
            LevelChunk chunk = chunks.get(key);
            if (chunk == null) {
                remaining++;
                continue;
            }
            BlockState state = chunk.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                    && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                remaining++;
            }
        }
        return remaining;
    }

    private boolean isCurrentRuneBoss(ServerLevel level) {
        return this.level() == level && this.isAlive() && !this.dead && !this.isRemoved()
                && !this.handlingDeath && !this.cleaningArena && !this.isClone()
                && level.getChunkSource().getChunkNow(this.blockPosition().getX() >> 4,
                        this.blockPosition().getZ() >> 4) != null
                && level.getEntity(this.getUUID()) == this;
    }

    private boolean isCurrentRunePhase(ServerLevel level, boolean broken) {
        return isCurrentRuneBoss(level) && this.isBroken() == broken && !this.runeSelectionNeedsRefresh;
    }

    private void broadcastRunePhase(ServerLevel level, boolean broken, Component message) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : nearbyPlayers(level, radius)) {
            if (!isCurrentRunePhase(level, broken)) {
                return;
            }
            if (player.level() == level && player.isAlive() && !player.isSpectator()
                    && !player.hasDisconnected() && player.distanceToSqr(this) <= radius * radius) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void triggerBroken(ServerLevel serverLevel) {
        if (this.handlingRuneTransition || this.handlingRuneSelection || this.runeSelectionNeedsRefresh
                || this.isBroken() || !isCurrentRuneBoss(serverLevel)) {
            return;
        }
        this.handlingRuneTransition = true;
        try {
            List<BlockPos> runes = List.copyOf(this.corruptedRunes);
            this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
            this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                    0.70F, 0.35F);
            this.corruptedRunes.clear();
            WatcherRuneCleanupData.forget(serverLevel, this.getUUID());
            this.setBroken(true);
            if (!isCurrentRunePhase(serverLevel, true)) { return; }
            BossLifecycleFx.phase(serverLevel, this.position(), this.getBbWidth(), this.getBbHeight(), BossParticleTheme.ABYSS.body());
            this.triggerAnim("state_controller", "break_veil");
            if (!isCurrentRunePhase(serverLevel, true)) { return; }
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            if (!isCurrentRunePhase(serverLevel, true)) { return; }
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.6F);

            if (!isCurrentRunePhase(serverLevel, true)) { return; }

            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), 1, 0, 0, 0, 0);
            Vec3 core = this.eyeBeamOrigin();
            for (BlockPos pos : runes) {
                Vec3 rune = Vec3.atCenterOf(pos).add(0, 0.35, 0);
                if (!isCurrentRunePhase(serverLevel, true)) { return; }
                BossFx.guidanceMarker(serverLevel, rune, BossParticleTheme.ABYSS.ember());
                if (!isCurrentRunePhase(serverLevel, true)) { return; }
                BossFx.guidanceFlow(serverLevel, rune, core,
                        BossParticleTheme.ABYSS.ember(), BossParticleTheme.ABYSS.ember(), this.tickCount);
            }
            for (int ring = 1; ring <= 3; ring++) {
                double r = ring * 2.0;
                for (int i = 0; i < 12 + ring * 6; i++) {
                    double angle = Math.PI * 2 * i / (12 + ring * 6);
                    if (!isCurrentRunePhase(serverLevel, true)) { return; }
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                            this.getX() + Math.cos(angle) * r,
                            this.getY() + this.getBbHeight() * 0.55,
                            this.getZ() + Math.sin(angle) * r, 1, 0.1, 0.2, 0.1, 0.06);
                }
            }
            broadcastRunePhase(serverLevel, true, Component.translatable("message.unknown_echoes.watcher.broken"));
        } finally {
            this.handlingRuneTransition = false;
        }
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
        if (this.isClone()) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            BossNbtData.addParticipant(this.participants, attacker.getUUID());
        }
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt
                && BossPhaseGate.reachedFloor(this, this.brokenWindowHealthFloor)
                && this.level() instanceof ServerLevel serverLevel) {
            restoreTideVeil(serverLevel);
        }
        return hurt;
    }

    @Override
    public boolean isMechanicDamageGuarded() {
        return !this.isClone() && !this.isBroken();
    }

    @Override
    public void onMechanicDamageGuarded(DamageSource source) {
        if (this.veiledMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.watcher.veiled"), true);
            this.veiledMessageCooldown = 40;
        }
    }

    @Override
    public boolean openTestWindow() {
        if (!(this.level() instanceof ServerLevel level) || !isCurrentRuneBoss(level)
                || this.isClone() || this.isBroken()) return false;
        this.triggerBroken(level);
        return this.isBroken();
    }

    @Override
    public boolean acceptsTestControl() { return !this.isClone(); }

    @Override
    public boolean advanceTestPhase() {
        if (!(this.level() instanceof ServerLevel) || !this.isAlive() || this.isRemoved()
                || !this.acceptsTestControl()) return false;
        boolean wasBroken = this.isBroken();
        if (!wasBroken && !this.openTestWindow()) return false;
        if (!this.isAlive() || this.isRemoved() || !this.isBroken()) return false;
        if (this.brokenWindowHealthFloor > 0.0F) {
            if (this.getHealth() <= this.brokenWindowHealthFloor + 0.01F) return false;
            this.setHealth(this.brokenWindowHealthFloor);
            // 下一 tick 由原有恢复流程收回机关和破防状态。
            this.brokenTicksRemaining = 1;
            return true;
        }
        return !wasBroken;
    }

    @Override
    public float phaseDamageFloor() {
        return !this.isClone() && this.isBroken() ? this.brokenWindowHealthFloor : 0.0F;
    }

    private void restoreTideVeil(ServerLevel serverLevel) {
        if (!this.isBroken() || !isCurrentRuneBoss(serverLevel)) {
            return;
        }
        // 破防表现回调内到达血量门槛时，保留下一 tick 的恢复请求。
        this.brokenTicksRemaining = 1;
        if (this.handlingRuneTransition || this.handlingRuneSelection) {
            return;
        }
        this.handlingRuneTransition = true;
        try {
            this.brokenTicksRemaining = 0;
            this.brokenWindowHealthFloor = 0.0F;
            this.runeSelectionNeedsRefresh = true;
            this.runeRescanCooldown = 0;
            this.setBroken(false);
            if (!isCurrentRuneBoss(serverLevel) || this.isBroken()) { return; }
            this.triggerAnim("state_controller", "veil_restore");
            if (!isCurrentRuneBoss(serverLevel) || this.isBroken()) { return; }
            this.corruptRunes(serverLevel);
            if (!isCurrentRunePhase(serverLevel, false)) { return; }
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
            broadcastRunePhase(serverLevel, false,
                    Component.translatable("message.unknown_echoes.watcher.veil_restored"));
        } finally {
            this.handlingRuneTransition = false;
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (!this.registerRechallengePresence(serverLevel)) {
                this.discard();
            } else if (!this.isClone() && this.isAlive() && !this.dead && !this.isRemoved()
                    && !this.handlingDeath && serverLevel.getEntity(this.getUUID()) == this) {
                WatcherRuneCleanupData.claim(serverLevel, this.getUUID(), this.corruptedRunes);
            }
        }
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
            if (!(this.level() instanceof ServerLevel serverLevel) || this.isClone()) {
                super.die(source);
                return;
            }
            Vec3 deathPos = this.position();
            BlockPos deathAltar = this.rechallengeAltar();
            UUID deathUuid = this.getUUID();
            Set<UUID> deathParticipants = Set.copyOf(this.participants);
            try {
                super.die(source);
            } finally {
                // 原版在掉落/演出通知前提交 dead；后续通知异常不能吞掉本模组结算。
                if (this.dead) {
                    this.arenaCleanupLevel = serverLevel;
                    settleWatcherDeath(serverLevel, deathPos, deathAltar, deathUuid, deathParticipants);
                }
            }
        } finally {
            this.handlingDeath = false;
        }
    }

    private void settleWatcherDeath(ServerLevel serverLevel, Vec3 deathPos, BlockPos deathAltar,
                                    UUID deathUuid, Set<UUID> deathParticipants) {
        try {
            boolean hasEligibleParticipant = false;
            boolean firstKillDrop = false;
            for (ServerPlayer player : nearbyDeathPlayers(serverLevel, deathPos)) {
                if (deathParticipants.contains(player.getUUID())
                        && isDeathParticipant(serverLevel, deathPos, player)) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                            EchoPermission.ABYSS_WATCHER_ID);
                    hasEligibleParticipant = true;
                    // 容量失败或通知异常不能把整场首杀包降为复战包。
                    firstKillDrop |= firstKill;
                    if (firstKill) {
                        runDeathStep("personal progress", () -> {
                            if (!EchoAbilityManager.markBossDefeated(player, EchoPermission.ABYSS_WATCHER_ID)
                                    && isDeathParticipant(serverLevel, deathPos, player)) {
                                player.sendSystemMessage(Component.translatable(
                                        "message.unknown_echoes.progress_record.full"));
                            }
                        });
                    }
                }
            }
            if (hasEligibleParticipant) {
                grantDeathMaterials(serverLevel, deathPos, firstKillDrop);
            }
        } finally {
            if (deathAltar != null) {
                runDeathStep("arena completion", () -> cn.kurt6.unknown_echoes.world.MainBossRechallengeData.onBossDefeated(
                        serverLevel, deathAltar, this.rechallengeKind(), deathUuid));
            }
        }
        runDeathStep("arena cleanup", () -> { this.cleanupArenaState(serverLevel); });
        runDeathStep("death effects", () -> {
            if (isCurrentDeathPresentation(serverLevel, deathPos)) {
                this.triggerAnim("state_controller", "death");
                playDeathEffects(serverLevel, deathPos);
            }
        });
    }

    private void grantDeathMaterials(ServerLevel serverLevel, Vec3 deathPos, boolean firstKillDrop) {
        if (!firstKillDrop) {
            runDeathStep("void particles", () -> BossMaterialRewards.drop(serverLevel,
                    deathPos.x, deathPos.y + 0.5D, deathPos.z,
                    new ItemStack(ModItems.VOID_PARTICLE_CRYSTAL.get(), 1 + this.random.nextInt(2))));
        }
        runDeathStep("tide core", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.TIDE_CORE.get(), firstKillDrop ? 2 : 1)));
        runDeathStep("watcher eye shards", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.WATCHER_EYE_SHARD.get(), 2 + this.random.nextInt(3))));
        runDeathStep("black tide thread", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.BLACK_TIDE_THREAD.get(), 1 + this.random.nextInt(2))));
        runDeathStep("mirror lake shards", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.MIRROR_LAKE_SHARD.get(), 3 + this.random.nextInt(4))));
        runDeathStep("echo shards", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.ECHO_SHARD.get(), 2 + this.random.nextInt(4))));
        runDeathStep("tide rune fragments", () -> BossMaterialRewards.drop(serverLevel,
                deathPos.x, deathPos.y + 0.5D, deathPos.z,
                new ItemStack(ModItems.TIDE_RUNE_FRAGMENT.get(), 2 + this.random.nextInt(3))));
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
            cn.kurt6.unknown_echoes.UnknownEchoes.LOGGER.error("Watcher death {} failed for {}", step, this.getUUID(), failure);
        }
    }

    private boolean isCurrentDeathPresentation(ServerLevel level, Vec3 deathPos) {
        return this.dead && !this.isClone() && !this.isRemoved() && this.level() == level
                && level.getEntity(this.getUUID()) == this
                && this.position().distanceToSqr(deathPos) < 0.0001D;
    }

    private void playDeathEffects(ServerLevel serverLevel, Vec3 deathPos) {
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) return;
        BossLifecycleFx.death(serverLevel, deathPos, this.getBbWidth(), this.getBbHeight(),
                BossParticleTheme.ABYSS.body());
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 1.8;
            for (int i = 0; i < 16; i++) {
                if (!isCurrentDeathPresentation(serverLevel, deathPos)) {
                    return;
                }
                double angle = Math.PI * 2 * i / 16 + ring * 0.4;
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        deathPos.x + Math.cos(angle) * r,
                        deathPos.y + this.getBbHeight() * 0.5,
                        deathPos.z + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, -0.12);
            }
        }
        if (isCurrentDeathPresentation(serverLevel, deathPos)) {
            serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                    deathPos.x, deathPos.y + this.getBbHeight() * 0.5, deathPos.z,
                    40, 1.5, 1.0, 1.5, 0.2);
        }
    }

    @Override
    public cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind rechallengeKind() {
        return cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind.ABYSS_WATCHER;
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
        if (!this.isClone()) {
            this.homePos = arenaCenter.immutable();
            this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
        }
    }

    private Vec3 findCloneSpawn(ServerLevel level, AbyssWatcher clone, BlockPos post) {
        if (post != null) {
            for (int distance = 2; distance <= 4; distance++) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    for (int dy : new int[]{0, 1, -1}) {
                        Vec3 candidate = Vec3.atCenterOf(post.relative(direction, distance).offset(0, dy, 0));
                        if (isSafeCloneSpawn(level, clone, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
            return null;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + this.random.nextDouble() * 3.0D;
            Vec3 candidate = new Vec3(this.getX() + Math.cos(angle) * distance,
                    this.getY() + (this.random.nextDouble() - 0.5D) * 2.0D,
                    this.getZ() + Math.sin(angle) * distance);
            if (isSafeCloneSpawn(level, clone, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSafeCloneSpawn(ServerLevel level, AbyssWatcher clone, Vec3 destination) {
        AABB moved = clone.getBoundingBox().move(destination.subtract(clone.position()));
        if (!Double.isFinite(moved.minX) || !Double.isFinite(moved.minY) || !Double.isFinite(moved.minZ)
                || !Double.isFinite(moved.maxX) || !Double.isFinite(moved.maxY) || !Double.isFinite(moved.maxZ)) {
            return false;
        }
        BlockPos min = BlockPos.containing(moved.minX, moved.minY, moved.minZ);
        BlockPos max = BlockPos.containing(moved.maxX - 1.0E-7D,
                moved.maxY - 1.0E-7D, moved.maxZ - 1.0E-7D);
        if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)
                || !level.getWorldBorder().isWithinBounds(moved)) {
            return false;
        }
        int minChunkX = (Mth.floor(moved.minX - 1.0e-7D) - 1) >> 4;
        int maxChunkX = (Mth.floor(moved.maxX + 1.0e-7D) + 1) >> 4;
        int minChunkZ = (Mth.floor(moved.minZ - 1.0e-7D) - 1) >> 4;
        int maxChunkZ = (Mth.floor(moved.maxZ + 1.0e-7D) + 1) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return level.noCollision(clone, moved);
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.isClone() && super.shouldDropLoot();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone()) {
            this.bossEvent.addPlayer(player);
        }
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
        return this.isInWater() ? ModSounds.ABYSS_WATCHER_AMBIENT.get() : ModSounds.ABYSS_WATCHER_AMBIENT_LAND.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? ModSounds.ABYSS_WATCHER_HURT.get() : ModSounds.ABYSS_WATCHER_HURT_LAND.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? ModSounds.ABYSS_WATCHER_DEATH.get() : ModSounds.ABYSS_WATCHER_DEATH_LAND.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsClone", this.isClone());
        tag.putInt("CloneLife", this.cloneLifeTicks);
        if (this.guardPos != null) {
            tag.put("GuardPos", new net.minecraft.nbt.IntArrayTag(
                    new int[]{this.guardPos.getX(), this.guardPos.getY(), this.guardPos.getZ()}));
        }
        if (this.cloneOwnerUuid != null) {
            tag.putUUID("CloneOwnerUuid", this.cloneOwnerUuid);
        }
        tag.putBoolean("Broken", this.isBroken());
        tag.putInt("BrokenTicks", this.brokenTicksRemaining);
        tag.putFloat("BrokenWindowHealthFloor", this.brokenWindowHealthFloor);
        ListTag clones = new ListTag();
        for (UUID id : this.cloneIds) {
            clones.add(NbtUtils.createUUID(id));
        }
        tag.put("CloneIds", clones);
        this.writeCloneWave(tag);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        this.saveRechallengeBinding(tag);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        this.writeRuneSelection(tag);
        if (this.homePos != null) {
            tag.putIntArray("HomePos", new int[]{this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BlockPos entityPos = this.blockPosition();
        this.homePos = BossNbtData.loadPositionNear(tag, "HomePos", entityPos,
                RUNE_SCAN_RADIUS * 2, RUNE_SCAN_HEIGHT * 2);
        BlockPos arenaCenter = this.homePos != null ? this.homePos : entityPos;
        if (tag.getBoolean("IsClone")) {
            float loadedHealth = this.getHealth();
            this.becomeClone();
            this.setHealth(BossNbtData.clampHealthFloor(loadedHealth, (float) CLONE_MAX_HEALTH));
            this.cloneLifeTicks = BossNbtData.clampTimer(tag.getInt("CloneLife"), CLONE_LIFETIME_TICKS);
            this.cloneOwnerUuid = tag.hasUUID("CloneOwnerUuid")
                    ? tag.getUUID("CloneOwnerUuid") : null;
            this.guardPos = BossNbtData.loadPositionNear(tag, "GuardPos", arenaCenter,
                    RUNE_SCAN_RADIUS, RUNE_SCAN_HEIGHT);
        }
        this.setBroken(tag.getBoolean("Broken"));
        this.brokenTicksRemaining = BossNbtData.clampTimer(tag.getInt("BrokenTicks"),
                ServerConfig.BOSS_BROKEN_DURATION_TICKS.get());
        this.brokenWindowHealthFloor = BossNbtData.clampHealthFloor(
                tag.getFloat("BrokenWindowHealthFloor"), this.getMaxHealth());
        BossNbtData.loadUuids(tag, "CloneIds", this.cloneIds, BossNbtData.MAX_SUMMONS);
        this.readCloneWave(tag);
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.loadRechallengeBinding(tag);
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        this.readRuneSelection(tag, arenaCenter);
        if (this.homePos != null) {
            if (!this.isClone()) {
                this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
            }
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    private void writeCloneWave(CompoundTag tag) {
        tag.putBoolean("CloneWaveActive", this.cloneWaveActive);
        tag.putInt("CloneRespawnCooldown", this.cloneRespawnCooldown);
    }

    private void readCloneWave(CompoundTag tag) {
        this.cloneWaveActive = !this.isClone() && (tag.contains("CloneWaveActive", net.minecraft.nbt.Tag.TAG_BYTE)
                ? tag.getBoolean("CloneWaveActive") : !this.cloneIds.isEmpty());
        this.cloneRespawnCooldown = BossNbtData.clampTimer(tag.getInt("CloneRespawnCooldown"), 200);
    }

    private void writeRuneSelection(CompoundTag tag) {
        ListTag runes = new ListTag();
        for (BlockPos pos : this.corruptedRunes) {
            runes.add(new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("CorruptedRunes", runes);
        tag.putBoolean("RuneSelectionNeedsRefresh", this.runeSelectionNeedsRefresh);
    }

    private void readRuneSelection(CompoundTag tag, BlockPos arenaCenter) {
        BossNbtData.loadPositionsNear(tag, "CorruptedRunes", this.corruptedRunes,
                BossNbtData.MAX_ARENA_POSITIONS, arenaCenter, RUNE_SCAN_RADIUS, RUNE_SCAN_HEIGHT);
        this.runeSelectionNeedsRefresh = !this.isClone() && !this.isBroken()
                && (tag.getBoolean("RuneSelectionNeedsRefresh")
                || this.corruptedRunes.size() > RUNE_MECHANIC_COUNT);
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
            this.triggerAnim("attack_controller", "attack");
            this.playSound(ModSounds.ABYSS_WATCHER_ATTACK_MELEE.get(), 1.1F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), BossParticleTheme.ABYSS.spark());
            }
        }
        return hit;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (!this.isClone() && this.isBroken()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            boolean swimming = this.isInWaterOrBubble()
                    ? state.isMoving() || this.getDeltaMovement().lengthSqr() > 1.0E-4
                    : state.isMoving();
            return state.setAndContinue(swimming ? SWIM_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("dash", DASH_ANIM)
                .triggerableAnim("blades", BLADES_ANIM)
                .triggerableAnim("vortex", VORTEX_ANIM)
                .triggerableAnim("burst", BURST_ANIM));
        controllers.add(new AnimationController<>(this, "state_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("awaken", AWAKEN_ANIM)
                .triggerableAnim("rune_corrupt", RUNE_CORRUPT_ANIM)
                .triggerableAnim("break_veil", BREAK_VEIL_ANIM)
                .triggerableAnim("veil_restore", VEIL_RESTORE_ANIM)
                .triggerableAnim("enrage", ENRAGE_ANIM)
                .triggerableAnim("death", DEATH_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void syncVisualAttackState(ServerLevel level) {
        int mode;
        int remaining;
        int duration;
        if (this.pendingBurstTicks > 0) {
            mode = 1;
            remaining = this.pendingBurstTicks;
            duration = BURST_HIT_DELAY;
        } else if (this.pendingDashTicks > 0) {
            mode = 2;
            remaining = this.pendingDashTicks;
            duration = DASH_WINDUP_TICKS;
        } else if (this.pendingVortexTicks > 0) {
            mode = 3;
            remaining = this.pendingVortexTicks;
            duration = VORTEX_HIT_DELAY;
        } else if (this.pendingBladeTicks > 0) {
            mode = 4;
            remaining = this.pendingBladeTicks;
            duration = BLADE_WINDUP_TICKS;
        } else {
            mode = 0;
            remaining = 0;
            duration = 0;
        }
        int age = Math.max(0, duration - remaining);
        SpecialAttack expected = this.specialAttack;
        this.specialAttackVisuals = true;
        this.setVisualAttackState(mode, age, duration);
        for (UUID id : List.copyOf(this.cloneIds)) {
            if (mode != 0 && !this.isCurrentSpecialAttack(level, expected)) { return; }
            if (level.getEntity(id) instanceof AbyssWatcher clone && this.isCurrentOwnedClone(level, clone)) {
                clone.setVisualAttackState(mode, age, duration);
            }
        }
        this.specialAttackVisuals = mode != 0;
    }

    private void setVisualAttackState(int mode, int age, int duration) {
        this.entityData.set(DATA_VISUAL_ATTACK_MODE, mode);
        this.entityData.set(DATA_VISUAL_ATTACK_AGE, age);
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

    @Override
    public void remove(RemovalReason reason) {
        try {
            if (this.level() instanceof ServerLevel currentLevel) {
                if (!this.isClone() && reason.shouldDestroy()) {
                    ServerLevel serverLevel = this.arenaCleanupLevel == null ? currentLevel : this.arenaCleanupLevel;
                    runDeathStep("removal cleanup", () -> { this.cleanupArenaState(serverLevel); });
                } else if (this.isClone() && reason == RemovalReason.KILLED) {
                    runDeathStep("clone removal effects", () -> currentLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                            this.getX(), this.getY() + 0.6, this.getZ(), 16, 0.4, 0.4, 0.4, 0.08));
                }
            }
        } finally {
            super.remove(reason);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isInWater();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.isInWater() && this.onGround()) {

            if (this.tickCount % 40 == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(
                        (this.random.nextFloat() * 2.0F - 1.0F) * 0.1F, 0.3,
                        (this.random.nextFloat() * 2.0F - 1.0F) * 0.1F));
            }
        }
    }
    @Override
    public int musicStage() {
        if (this.isClone() && this.level() instanceof ServerLevel level && this.cloneOwnerUuid != null
                && level.getEntity(this.cloneOwnerUuid) instanceof AbyssWatcher owner && !owner.isClone()) {
            return owner.musicStage();
        }
        return this.getHealth() < this.getMaxHealth() * 0.3F ? 2 : this.isBroken() ? 1 : 0;
    }
}
