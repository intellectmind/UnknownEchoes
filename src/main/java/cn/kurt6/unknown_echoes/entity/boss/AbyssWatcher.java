package cn.kurt6.unknown_echoes.entity.boss;

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
import net.minecraft.world.level.pathfinder.PathType;
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

public class AbyssWatcher extends Monster implements GeoEntity, BossDamageGuard, PhaseGatedBoss,
        BossVisualState,
        RechallengeBoundBoss {
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
    private int cloneRespawnCooldown = 0;

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
        this.becomeClone();
        this.cloneOwnerUuid = owner.getUUID();
        this.homePos = owner.arenaAnchor().immutable();
        this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
    }

    private class ReturnToGuardPostGoal extends net.minecraft.world.entity.ai.goal.Goal {
        ReturnToGuardPostGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return AbyssWatcher.this.isClone() && AbyssWatcher.this.guardPos != null
                    && AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(AbyssWatcher.this.guardPos))
                            > CLONE_LEASH_DIST_SQ;
        }

        @Override
        public boolean canContinueToUse() {
            return AbyssWatcher.this.guardPos != null
                    && AbyssWatcher.this.distanceToSqr(Vec3.atCenterOf(AbyssWatcher.this.guardPos))
                            > CLONE_RETURN_DONE_DIST_SQ
                    && !AbyssWatcher.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 post = Vec3.atCenterOf(AbyssWatcher.this.guardPos);
            AbyssWatcher.this.getNavigation().moveTo(post.x, post.y, post.z, 1.2D);
        }

        @Override
        public void stop() {
            AbyssWatcher.this.getNavigation().stop();
        }
    }

    private void summonClones(ServerLevel serverLevel) {
        if (!BossNbtData.isHorizontalAreaLoaded(
                serverLevel, this.arenaAnchor(), RUNE_SCAN_RADIUS)) {
            return;
        }
        this.cloneIds.clear();

        List<BlockPos> posts = new ArrayList<>();
        for (BlockPos pos : this.corruptedRunes) {
            if (!serverLevel.hasChunkAt(pos)) {
                continue;
            }
            var state = serverLevel.getBlockState(pos);
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
            AbyssWatcher clone = this.getType().create(serverLevel) instanceof AbyssWatcher watcher ? watcher : null;
            if (clone == null) {
                continue;
            }
            BlockPos post = i < posts.size() ? posts.get(i) : null;
            Vec3 spawn = this.findCloneSpawn(serverLevel, clone, post);
            if (spawn == null) {
                clone.discard();
                continue;
            }
            clone.moveTo(spawn.x, spawn.y, spawn.z,
                    this.random.nextFloat() * 360.0F, 0.0F);
            clone.becomeClone(this);
            clone.guardPos = post;
            clone.setTarget(this.getTarget());
            clone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(clone.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            if (!serverLevel.addFreshEntity(clone)) {
                clone.discard();
                continue;
            }
            this.cloneIds.add(clone.getUUID());
            serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                    clone.getX(), clone.getY() + 0.8, clone.getZ(), 16, 0.5, 0.5, 0.5, 0.06);
        }
        if (!this.cloneIds.isEmpty()) {
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2F, 1.4F);
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.clones"));
        }
    }

    public static boolean isRuneGuarded(ServerLevel serverLevel, BlockPos pos) {
        return !serverLevel.getEntitiesOfClass(AbyssWatcher.class,
                new net.minecraft.world.phys.AABB(pos).inflate(CLONE_GUARD_RADIUS),
                w -> w.isClone() && w.isAlive()).isEmpty();
    }

    private boolean anyCloneAlive(ServerLevel serverLevel) {
        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone && clone.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void discardClones(ServerLevel serverLevel) {
        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                        clone.getX(), clone.getY() + 0.6, clone.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                clone.discard();
            }
        }
        this.cloneIds.clear();
    }

    private void cleanupArenaState(ServerLevel serverLevel) {
        this.discardClones(serverLevel);
        for (BlockPos pos : this.corruptedRunes) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                    && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                serverLevel.setBlock(pos,
                        state.setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED,
                                Boolean.FALSE), 3);
            }
        }
        this.corruptedRunes.clear();
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
        if (this.vortexCooldown > 0) this.vortexCooldown--;
        if (this.burstCooldown > 0) this.burstCooldown--;
        if (this.dashCooldown > 0) this.dashCooldown--;
        if (this.bladeCooldown > 0) this.bladeCooldown--;
        this.syncVisualAttackState(serverLevel);

        if (this.pendingDashTicks > 0) {
            this.spawnDashWarning(serverLevel);
            if (--this.pendingDashTicks == 0) {
                this.doTidalDash(serverLevel);
            }
        }
        if (this.pendingBladeTicks > 0) {
            this.spawnBladeChargeBeams(serverLevel);
            if (--this.pendingBladeTicks == 0) {
                this.doWaterBlades(serverLevel);
            }
        }
        if (this.pendingVortexTicks > 0 && --this.pendingVortexTicks == 0) {
            this.doVortexPull(serverLevel);
        }
        if (this.pendingBurstTicks > 0 && --this.pendingBurstTicks == 0) {
            this.doBurstDamage(serverLevel);
        }
        if (!this.isAlive() || this.pendingVortexTicks > 0 || this.pendingBurstTicks > 0
                || this.pendingDashTicks > 0 || this.pendingBladeTicks > 0) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        int cooldownScale = this.getHealth() < this.getMaxHealth() * 0.3F ? 70 : 100;
        double distSq = this.distanceToSqr(target);
        if (this.burstCooldown <= 0 && distSq <= BURST_RANGE * BURST_RANGE) {
            this.pendingBurstTicks = BURST_HIT_DELAY;
            this.burstCooldown = 160 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "burst");
            this.playSound(ModSounds.ABYSS_WATCHER_BURST.get(), 1.4F, 1.0F);
        } else if (this.dashCooldown <= 0 && distSq <= DASH_RANGE * DASH_RANGE
                && distSq > BURST_RANGE * BURST_RANGE && this.hasLineOfSight(target)) {

            this.dashDirection = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(this.position().add(0, this.getBbHeight() * 0.5, 0))
                    .multiply(1, 0.3, 1).normalize();
            this.pendingDashTicks = DASH_WINDUP_TICKS;
            this.dashCooldown = 200 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "dash");
            this.playSound(ModSounds.ABYSS_WATCHER_DASH.get(), 1.6F, 1.0F);
        } else if (this.vortexCooldown <= 0 && distSq <= VORTEX_RANGE * VORTEX_RANGE) {
            this.pendingVortexTicks = VORTEX_HIT_DELAY;
            this.vortexCooldown = 220 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "vortex");
            this.playSound(ModSounds.ABYSS_WATCHER_VORTEX.get(), 1.8F, 1.0F);
        } else if (this.bladeCooldown <= 0 && distSq <= BLADE_RANGE * BLADE_RANGE) {

            this.bladeTargetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            this.pendingBladeTicks = BLADE_WINDUP_TICKS;
            this.bladeCooldown = 240 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "blades");
            this.playSound(ModSounds.ABYSS_WATCHER_BLADES.get(), 1.2F, 1.0F);
        }
    }

    private void spawnDashWarning(ServerLevel serverLevel) {
        Vec3 eye = this.position().add(0, this.getBbHeight() * 0.5, 0);
        BossFx.chargeSpiral(serverLevel, eye, this.tickCount,
                BossFx.waterBubbleOr(serverLevel, eye, BossParticleTheme.ABYSS.body()));
        for (int d = 1; d <= (int) DASH_RANGE; d += 2) {
            Vec3 p = eye.add(this.dashDirection.scale(d));
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 3, 0.4, 0.2, 0.4, 0.0);
            if (d % 4 == 1) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, eye, BossParticleTheme.ABYSS.body()),
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                6, this.getBbWidth() * 0.5, 0.4, this.getBbWidth() * 0.5, 0.05);
    }

    private void doTidalDash(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9F;
        Vec3 start = this.position().add(0, this.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(DASH_RANGE, 4.0, DASH_RANGE),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(start);
            double along = toVictim.dot(this.dashDirection);
            if (along < 0 || along > DASH_RANGE) {
                continue;
            }
            double perpendicular = toVictim.subtract(this.dashDirection.scale(along)).length();
            if (perpendicular > 2.2) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(this.dashDirection.x * 0.9, 0.4, this.dashDirection.z * 0.9));
                victim.hurtMarked = true;
            }
        }

        this.setDeltaMovement(this.dashDirection.scale(2.2));
        this.hurtMarked = true;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.DOLPHIN_JUMP, SoundSource.HOSTILE, 1.8F, 0.5F);
        for (int d = 0; d <= (int) DASH_RANGE; d++) {
            Vec3 p = start.add(this.dashDirection.scale(d));
            serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, p, BossParticleTheme.ABYSS.body()),
                    p.x, p.y, p.z, 6, 0.6, 0.6, 0.6, 0.1);
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.02);
        }
        BossFx.sparkSpray(serverLevel, start, this.dashDirection, DASH_RANGE, BossParticleTheme.ABYSS.spark());
        Vec3 dashCenter = start.add(this.dashDirection.scale(DASH_RANGE * 0.5D));
        BossFx.cinematicImpact(serverLevel, dashCenter, 30.0D, 0.58F, 7, 0x43BFD1);
    }

    private void spawnBladeChargeBeams(ServerLevel serverLevel) {
        if (this.tickCount % 2 != 0) {
            return;
        }
        spawnBeam(serverLevel, this.eyeBeamOrigin(), this.bladeTargetPos);
        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone && clone.isAlive()) {
                spawnBeam(serverLevel, clone.eyeBeamOrigin(), this.bladeTargetPos);
            }
        }
    }

    private Vec3 eyeBeamOrigin() {
        return this.position().add(0, this.getBbHeight() * 0.55, 0);
    }

    private void spawnBeam(ServerLevel serverLevel, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int) (delta.length() * 1.5));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void damageAlongBlade(ServerLevel serverLevel, Vec3 from, float damage, boolean applySlow) {
        Vec3 delta = this.bladeTargetPos.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 dir = delta.normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BLADE_RANGE, 6.0, BLADE_RANGE),
                e -> !e.isSpectator() && !(e instanceof AbyssWatcher)
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(from);
            double along = toVictim.dot(dir);
            if (along < 0 || along > length + 2.0) {
                continue;
            }
            if (toVictim.subtract(dir.scale(along)).length() > 1.6) {
                continue;
            }
            boolean hurt = victim.hurt(this.damageSources().mobAttack(this), damage);
            if (hurt && applySlow) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
            }
        }
    }

    private void doWaterBlades(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        Vec3 from = this.eyeBeamOrigin();
        this.damageAlongBlade(serverLevel, from, damage, true);
        spawnBeam(serverLevel, from, this.bladeTargetPos);

        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone && clone.isAlive()) {
                Vec3 cloneFrom = clone.eyeBeamOrigin();
                this.damageAlongBlade(serverLevel, cloneFrom, (float) CLONE_ATTACK_DAMAGE, false);
                spawnBeam(serverLevel, cloneFrom, this.bladeTargetPos);
            }
        }
        serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                this.bladeTargetPos.x, this.bladeTargetPos.y, this.bladeTargetPos.z,
                14, 0.5, 0.5, 0.5, 0.12);
        serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                this.bladeTargetPos.x, this.bladeTargetPos.y, this.bladeTargetPos.z,
                20, 0.8, 0.8, 0.8, 0.15);
        BossFx.impactBurst(serverLevel, this.bladeTargetPos, BossParticleTheme.ABYSS.spark());
        BossFx.cinematicImpact(serverLevel, this.bladeTargetPos, 26.0D, 0.52F, 7, 0x55C7D5);
        serverLevel.playSound(null, BlockPos.containing(this.bladeTargetPos),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 0.8F);
    }

    private void doVortexPull(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(VORTEX_RANGE, VORTEX_RANGE * 0.6, VORTEX_RANGE),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 horizontal = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (horizontal.lengthSqr() > VORTEX_RANGE * VORTEX_RANGE) {
                continue;
            }
            Vec3 pull = this.position().add(0, this.getBbHeight() * 0.5, 0)
                    .subtract(victim.position());
            if (pull.lengthSqr() < 1.0E-4) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 dir = pull.normalize();
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(dir.x * 1.1, dir.y * 0.6 + 0.1, dir.z * 1.1));
            victim.hurtMarked = true;
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.4F);

        for (int ring = 0; ring < 3; ring++) {
            double r = VORTEX_RANGE - ring * 2.5;
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14 + ring * 0.5;
                Vec3 point = new Vec3(this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r);
                serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, point, BossParticleTheme.ABYSS.body()),
                        point.x, point.y, point.z, 2, 0.1, 0.3, 0.1, 0.02);
            }
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), VORTEX_RANGE * 0.7,
                BossFx.waterBubbleOr(serverLevel, this.position(), BossParticleTheme.ABYSS.body()));
        BossFx.cinematicImpact(serverLevel, this.position(), 34.0D, 0.62F, 8, 0x2B92A6);
    }

    private void doBurstDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BURST_RANGE, 2.0, BURST_RANGE),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > BURST_RANGE * BURST_RANGE) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement()
                            .add(dir.x * 1.6, 0.5, dir.z * 1.6));
                    victim.hurtMarked = true;
                }
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.6F);
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                    this.getX() + Math.cos(angle) * 2.5,
                    this.getY() + this.getBbHeight() * 0.5,
                    this.getZ() + Math.sin(angle) * 2.5, 3, 0.2, 0.3, 0.2, 0.08);
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), BURST_RANGE, BossParticleTheme.ABYSS.ember());
        BossFx.impactBurst(serverLevel, this.position().add(0, this.getBbHeight() * 0.5, 0), BossParticleTheme.ABYSS.spark());
        BossFx.cinematicImpact(serverLevel, this.position(), 30.0D, 0.82F, 10, 0x247F91);
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

        if (!this.isBroken() && this.runeSelectionNeedsRefresh) {
            this.corruptRunes(serverLevel, false);
        }

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

            if (this.tickCount % 10 == 0 && hasNearbyRuneViewer && this.corruptedRunes.isEmpty()) {
                if (this.runeRescanCooldown > 0) {
                    this.runeRescanCooldown -= 10;
                } else {
                    this.corruptRunes(serverLevel);
                    if (this.corruptedRunes.isEmpty()) {
                        this.runeRescanCooldown = 600;
                    }
                }
            }
            if (this.tickCount % 10 == 0 && this.getTarget() != null) {
                if (this.cloneRespawnCooldown > 0) {
                    this.cloneRespawnCooldown -= 10;
                }
                boolean cloneAlive = anyCloneAlive(serverLevel);

                if (this.corruptedRunes.isEmpty() && !cloneAlive
                        && this.cloneRespawnCooldown > 0) {
                    this.triggerBroken(serverLevel);
                }

                if (!cloneAlive && this.cloneRespawnCooldown <= 0) {
                    this.summonClones(serverLevel);
                    this.cloneRespawnCooldown = 200;
                }
            }
        }
        if (guidanceTick && hasNearbyRuneViewer && !this.corruptedRunes.isEmpty()) {
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
     * 拟影的独立生命周期:本体失效、寿命到点或守位符文被净化即自毁。
     * 拟影不打招式、不吃阶段、不参与结算,不要并回 aiStep。
     */
    private void tickCloneLifecycle() {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.cloneOwnerUuid != null && this.tickCount % 20 == 0
                    && BossNbtData.isHorizontalAreaLoaded(serverLevel,
                    this.homePos != null ? this.homePos : this.blockPosition(), RUNE_SCAN_RADIUS + 2)) {
                Entity owner = serverLevel.getEntity(this.cloneOwnerUuid);
                if (!(owner instanceof AbyssWatcher watcher)
                        || watcher.isClone() || !watcher.isAlive()) {
                    serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                            this.getX(), this.getY() + 0.6, this.getZ(),
                            12, 0.4, 0.4, 0.4, 0.02);
                    this.discard();
                    return;
                }
            }

            if (this.isAlive() && this.tickCount % 10 == 0) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        2, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.01);
            }

            if (this.guardPos != null && this.tickCount % 20 == 0
                    && serverLevel.hasChunkAt(this.guardPos)) {
                var state = serverLevel.getBlockState(this.guardPos);
                if (!(state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock)
                        || !state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                    this.guardPos = null;
                }
            }
            if (--this.cloneLifeTicks <= 0) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                        this.getX(), this.getY() + 0.6, this.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                this.discard();
            }
        }
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
        if (!isScanAreaLoaded(serverLevel, center, RUNE_SCAN_RADIUS)) {
            this.runeSelectionNeedsRefresh = true;
            return;
        }
        this.corruptedRunes.clear();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RUNE_SCAN_RADIUS, -RUNE_SCAN_HEIGHT, -RUNE_SCAN_RADIUS),
                center.offset(RUNE_SCAN_RADIUS, RUNE_SCAN_HEIGHT, RUNE_SCAN_RADIUS))) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock) {
                candidates.add(pos.immutable());
            }
        }

        this.corruptedRunes.addAll(selectMechanicRunes(center, candidates));
        // 侵蚀演出只在 announce(玩家在场)时播,静默迁移旧存档时不播。
        if (announce && !this.corruptedRunes.isEmpty()) {
            this.triggerAnim("state_controller", "rune_corrupt");
        }
        Set<BlockPos> selected = new HashSet<>(this.corruptedRunes);
        for (BlockPos pos : candidates) {
            var state = serverLevel.getBlockState(pos);
            boolean shouldBeCorrupted = selected.contains(pos);
            if (state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)
                    != shouldBeCorrupted) {
                serverLevel.setBlock(pos,
                        state.setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED,
                                shouldBeCorrupted), 3);
            }
            if (announce && shouldBeCorrupted) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.04);
            }
        }
        this.runeSelectionNeedsRefresh = false;
        if (announce && !this.corruptedRunes.isEmpty()) {
            serverLevel.playSound(null, center,
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.7F);
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.runes_corrupted"));
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

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return BossNbtData.isHorizontalAreaLoaded(level, center, radius);
    }

    private void tickRuneGuidance(ServerLevel serverLevel) {
        if (this.corruptedRunes.isEmpty()) {
            return;
        }

        List<AbyssWatcher> guards = new ArrayList<>();
        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone
                    && clone.isAlive() && clone.isClone()) {
                guards.add(clone);
            }
        }
        Vec3 core = this.eyeBeamOrigin();
        for (BlockPos pos : this.corruptedRunes) {
            if (!serverLevel.hasChunkAt(pos)) {
                continue;
            }
            var state = serverLevel.getBlockState(pos);
            if (!(state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock)
                    || !state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                continue;
            }
            Vec3 rune = Vec3.atCenterOf(pos).add(0, 0.35, 0);
            AbyssWatcher guard = nearestRuneGuard(guards, rune);
            BossFx.guidanceMarker(serverLevel, rune, BossParticleTheme.ABYSS.ember());
            if (guard != null) {
                BossFx.guidanceFlow(serverLevel, guard.eyeBeamOrigin(), rune,
                        BossParticleTheme.ABYSS.ember(), BossParticleTheme.ABYSS.ember(), this.tickCount);
            } else {
                BossFx.guidanceFlow(serverLevel, core, rune,
                        BossParticleTheme.ABYSS.ember(), BossParticleTheme.ABYSS.ember(), this.tickCount);
            }
        }
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

    private static AbyssWatcher nearestRuneGuard(List<AbyssWatcher> guards, Vec3 rune) {
        AbyssWatcher nearest = null;
        double nearestDistance = CLONE_GUARD_RADIUS * CLONE_GUARD_RADIUS;
        for (AbyssWatcher guard : guards) {
            double distance = guard.distanceToSqr(rune);
            if (distance <= nearestDistance) {
                nearest = guard;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public void onRunePurified(Player purifier) {
        if (this.level().isClientSide || this.isBroken()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BossNbtData.addParticipant(this.participants, purifier.getUUID());
        int remaining = 0;
        for (BlockPos pos : this.corruptedRunes) {
            if (!serverLevel.hasChunkAt(pos)) {
                remaining++;
                continue;
            }
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                    && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                remaining++;
            }
        }
        if (remaining <= 0) {
            this.triggerBroken(serverLevel);
            this.corruptedRunes.clear();
        } else {
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.rune_progress",
                    remaining));
        }
    }

    private void triggerBroken(ServerLevel serverLevel) {
        this.setBroken(true);
        this.triggerAnim("state_controller", "break_veil");
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.70F, 0.35F);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.6F);

        serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), 1, 0, 0, 0, 0);
        Vec3 core = this.eyeBeamOrigin();
        for (BlockPos pos : this.corruptedRunes) {
            Vec3 rune = Vec3.atCenterOf(pos).add(0, 0.35, 0);
            BossFx.guidanceMarker(serverLevel, rune, BossParticleTheme.ABYSS.ember());
            BossFx.guidanceFlow(serverLevel, rune, core,
                    BossParticleTheme.ABYSS.ember(), BossParticleTheme.ABYSS.ember(), this.tickCount);
        }
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 2.0;
            for (int i = 0; i < 12 + ring * 6; i++) {
                double angle = Math.PI * 2 * i / (12 + ring * 6);
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.55,
                        this.getZ() + Math.sin(angle) * r, 1, 0.1, 0.2, 0.1, 0.06);
            }
        }
        broadcastNearby(Component.translatable("message.unknown_echoes.watcher.broken"));
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
    public float phaseDamageFloor() {
        return !this.isClone() && this.isBroken() ? this.brokenWindowHealthFloor : 0.0F;
    }

    private void restoreTideVeil(ServerLevel serverLevel) {
        this.setBroken(false);
        this.triggerAnim("state_controller", "veil_restore");
        this.brokenTicksRemaining = 0;
        this.brokenWindowHealthFloor = 0.0F;
        this.corruptRunes(serverLevel);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
        broadcastNearby(Component.translatable("message.unknown_echoes.watcher.veil_restored"));
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
        if (wasDead || !this.dead || this.isClone()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 沉回湖底的死亡表演,只有本体会走到这里;拟影已在上面 return,走墨雾即时散逸。
        this.triggerAnim("state_controller", "death");
        this.cleanupArenaState(serverLevel);

        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 1.8;
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16 + ring * 0.4;
                serverLevel.sendParticles(BossParticleTheme.ABYSS.spark(),
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, -0.12);
            }
        }
        serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                40, 1.5, 1.0, 1.5, 0.2);

        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        boolean hasEligibleParticipant = false;
        boolean firstKillDrop = false;
        for (ServerPlayer player : this.nearbyPlayers(serverLevel, radius)) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                        EchoPermission.ABYSS_WATCHER_ID);
                hasEligibleParticipant = true;

                // 记录写失败不影响首杀判定,否则记录已满时整队会降级成复战奖励。
                firstKillDrop |= firstKill;
                if (firstKill && !EchoAbilityManager.markBossDefeated(
                        player, EchoPermission.ABYSS_WATCHER_ID)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"));
                }
            }
        }
        if (hasEligibleParticipant) {
            BossMaterialRewards.drop(this, new ItemStack(ModItems.TIDE_CORE.get(), firstKillDrop ? 2 : 1));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.WATCHER_EYE_SHARD.get(),
                    2 + this.random.nextInt(3)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.BLACK_TIDE_THREAD.get(),
                    1 + this.random.nextInt(2)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.MIRROR_LAKE_SHARD.get(),
                    3 + this.random.nextInt(4)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_SHARD.get(),
                    2 + this.random.nextInt(4)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.TIDE_RUNE_FRAGMENT.get(),
                    2 + this.random.nextInt(3)));
        }
        this.finishRechallenge(serverLevel);
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
        BlockPos min = BlockPos.containing(moved.minX, moved.minY, moved.minZ);
        BlockPos max = BlockPos.containing(moved.maxX - 1.0E-7D,
                moved.maxY - 1.0E-7D, moved.maxZ - 1.0E-7D);
        if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)
                || !level.getWorldBorder().isWithinBounds(moved)) {
            return false;
        }
        for (int x : new int[]{min.getX(), max.getX()}) {
            for (int z : new int[]{min.getZ(), max.getZ()}) {
                if (!level.hasChunkAt(new BlockPos(x, min.getY(), z))) {
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
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        this.saveRechallengeBinding(tag);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        ListTag runes = new ListTag();
        for (BlockPos pos : this.corruptedRunes) {
            runes.add(new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("CorruptedRunes", runes);
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
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.loadRechallengeBinding(tag);
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        BossNbtData.loadPositionsNear(tag, "CorruptedRunes", this.corruptedRunes,
                BossNbtData.MAX_ARENA_POSITIONS, arenaCenter, RUNE_SCAN_RADIUS, RUNE_SCAN_HEIGHT);
        this.runeSelectionNeedsRefresh = !this.isClone() && !this.isBroken()
                && this.corruptedRunes.size() > RUNE_MECHANIC_COUNT;
        if (this.homePos != null) {
            if (!this.isClone()) {
                this.restrictTo(this.homePos, RUNE_SCAN_RADIUS);
            }
        }
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
            this.triggerAnim("attack_controller", "attack");
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
        this.setVisualAttackState(mode, age, duration);
        for (UUID id : this.cloneIds) {
            if (level.getEntity(id) instanceof AbyssWatcher clone && clone.isAlive()) {
                clone.setVisualAttackState(mode, age, duration);
            }
        }
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
        if (this.level() instanceof ServerLevel serverLevel) {
            if (!this.isClone() && reason.shouldDestroy()) {
                this.cleanupArenaState(serverLevel);
            } else if (this.isClone() && reason == RemovalReason.KILLED) {
                serverLevel.sendParticles(BossParticleTheme.ABYSS.body(),
                        this.getX(), this.getY() + 0.6, this.getZ(), 16, 0.4, 0.4, 0.4, 0.08);
            }
        }
        super.remove(reason);
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
}
