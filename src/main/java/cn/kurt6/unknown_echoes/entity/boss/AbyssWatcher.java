package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
 * 深渊观测者:第二主线 Boss(镜湖底部,潮汐回响守护者)。
 *
 * 阶段(按设计文档 10.5,V1.7 改版:弱化辨真身、强化净化压力战):
 * - 黑潮守卫:侵蚀圣殿水下符文,并分裂出"黑潮拟影"分别守卫——符文 5 格内有存活拟影时无法净化,
 *   玩家需要击杀或引开守卫(拟影脱岗超过 8 格会回防)。拟影不伪装本体:周身缠绕墨丝、躯体暗沉,一眼可辨。
 *   未破防时伤害 = min(伤害 x 5%, 上限 4.0)。
 * - 潮汐破防:玩家净化全部被侵蚀的水下符文 → 本体短暂暴露,正常受伤;超时重新侵蚀并恢复帷幕。
 *   (场地没有符文时退化为"清空拟影破防",保证刷怪蛋/空旷水域也能完整战斗。)
 * 攻击:潮汐冲刺(水纹预警线→突进)/ 齐射水刃(本体高伤、拟影低伤,全部直线光束预警)/
 *       深水牵引(漩涡拖拽)/ 深渊脉冲(近身爆发驱离)。全部动画先行、伤害延迟到命中帧。
 * 死亡:场地内参与玩家个人写入击败记录;潮汐回响由潮汐核心祭坛校验信物后解锁。
 */
public class AbyssWatcher extends Monster implements GeoEntity {
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

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLONE =
            SynchedEntityData.defineId(AbyssWatcher.class, EntityDataSerializers.BOOLEAN);

    private static final int CLONE_COUNT_NO_RUNES = 2;          // 无符文场地的游荡拟影数
    private static final int CLONE_GUARD_CAP = 3;               // 守卫拟影上限(符文更多时留无守卫缺口当突破口)
    private static final int CLONE_LIFETIME_TICKS = 1200;       // 拟影 60 秒后自行消散并重新洗牌
    private static final double CLONE_MAX_HEALTH = 8.0D;
    private static final double CLONE_ATTACK_DAMAGE = 3.0D;
    /** 守卫判定半径:符文这个范围内有存活拟影时无法净化(TideRuneBlock 调用)。 */
    public static final double CLONE_GUARD_RADIUS = 5.0D;
    private static final double CLONE_LEASH_DIST_SQ = 8.0D * 8.0D;   // 脱岗距离:超过即放弃追击回防
    private static final double CLONE_RETURN_DONE_DIST_SQ = 2.5D * 2.5D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int cloneLifeTicks = CLONE_LIFETIME_TICKS;
    /** 拟影的守卫岗位(被侵蚀符文的位置);null = 无岗位游荡拟影。仅分身使用。 */
    private BlockPos guardPos = null;
    private int brokenTicksRemaining = 0;
    private float brokenWindowHealthFloor = 0.0F;
    private int veiledMessageCooldown = 0;
    private int outOfCombatTicks = 0;
    private BlockPos homePos = null;
    private final List<UUID> cloneIds = new ArrayList<>();
    private final Set<UUID> participants = new HashSet<>();

    // ---- 特殊攻击:动画先行,伤害延迟到命中帧 ----
    private static final int VORTEX_HIT_DELAY = 15;   // 深水牵引:0.75s,旋转中段拖拽
    private static final int BURST_HIT_DELAY = 8;     // 深渊脉冲:0.4s,膨胀爆发帧
    private static final int DASH_WINDUP_TICKS = 14;  // 潮汐冲刺:0.7s 水纹预警线
    private static final int BLADE_WINDUP_TICKS = 16; // 齐射水刃:0.8s 蓄力(本体+拟影光束追踪)
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

    // ---- 破防机关:被侵蚀的水下符文(净化全部 → 破防窗口) ----
    private static final int RUNE_SCAN_RADIUS = 30;
    private static final int RUNE_SCAN_HEIGHT = 10;
    private final List<BlockPos> corruptedRunes = new ArrayList<>();
    private int cloneRespawnCooldown = 0;

    /** 多人扩容:参与者每多一人,血量上限 +150(只升不降,NBT 持久化防重载重复回血)。 */
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
        // 水栖:水中不会溺水(NeoForge 中 canBreatheUnderwater 为 final,走 FluidType 扩展)
        if (type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public void travel(Vec3 travelVector) {
        // 仿守卫者:水中自体推进,离水退回默认重力行走
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

    /** 把这只实例标记为黑潮拟影:低血量、弱攻击、无掉落无经验、限时消散。 */
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

    // ---- 黑潮拟影:守卫被侵蚀符文,阻碍净化 ----

    /** 脱岗回防:拟影离守卫岗位太远(被引开/追击过头)时放弃当前行动游回符文。 */
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
        this.cloneIds.clear();
        // 一岗一哨:给仍处于侵蚀状态的符文各派一只拟影(上限 CLONE_GUARD_CAP,
        // 符文更多时留出无守卫缺口当玩家的突破口);没有符文的场地退化为游荡拟影。
        List<BlockPos> posts = new ArrayList<>();
        for (BlockPos pos : this.corruptedRunes) {
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
            if (post != null) {
                // 出生在岗位旁,而不是本体旁——玩家一眼看清"谁守着哪块符文"
                clone.moveTo(post.getX() + 0.5, post.getY() + 0.5, post.getZ() + 0.5,
                        this.random.nextFloat() * 360.0F, 0.0F);
            } else {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double dist = 3.0 + this.random.nextDouble() * 3.0;
                clone.moveTo(this.getX() + Math.cos(angle) * dist, this.getY(),
                        this.getZ() + Math.sin(angle) * dist, this.random.nextFloat() * 360.0F, 0.0F);
            }
            clone.becomeClone();
            clone.guardPos = post;
            clone.setTarget(this.getTarget());
            clone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(clone.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(clone);
            this.cloneIds.add(clone.getUUID());
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    clone.getX(), clone.getY() + 0.8, clone.getZ(), 16, 0.5, 0.5, 0.5, 0.06);
        }
        if (!this.cloneIds.isEmpty()) {
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2F, 1.4F);
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.clones"));
        }
    }

    /** 符文 pos 周围是否有存活的守卫拟影(净化拦截判定,TideRuneBlock 调用)。 */
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
                serverLevel.sendParticles(ParticleTypes.POOF,
                        clone.getX(), clone.getY() + 0.6, clone.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                clone.discard();
            }
        }
        this.cloneIds.clear();
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

    // ---- 特殊攻击调度:远距水刃齐射,中距冲刺/漩涡,近身脉冲驱离 ----

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.vortexCooldown > 0) this.vortexCooldown--;
        if (this.burstCooldown > 0) this.burstCooldown--;
        if (this.dashCooldown > 0) this.dashCooldown--;
        if (this.bladeCooldown > 0) this.bladeCooldown--;

        // 预警/蓄力演出:每 tick 持续铺设
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
        // 低血量"深渊回流":冷却缩短,但所有攻击仍保留完整前摇与预警
        int cooldownScale = this.getHealth() < this.getMaxHealth() * 0.3F ? 70 : 100;
        double distSq = this.distanceToSqr(target);
        if (this.burstCooldown <= 0 && distSq <= BURST_RANGE * BURST_RANGE) {
            this.pendingBurstTicks = BURST_HIT_DELAY;
            this.burstCooldown = 160 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "burst");
            this.playSound(SoundEvents.GUARDIAN_ATTACK, 1.4F, 0.6F);
        } else if (this.dashCooldown <= 0 && distSq <= DASH_RANGE * DASH_RANGE
                && distSq > BURST_RANGE * BURST_RANGE && this.hasLineOfSight(target)) {
            // 潮汐冲刺:锁定方向并铺水纹预警线
            this.dashDirection = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(this.position().add(0, this.getBbHeight() * 0.5, 0))
                    .multiply(1, 0.3, 1).normalize();
            this.pendingDashTicks = DASH_WINDUP_TICKS;
            this.dashCooldown = 200 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "dash");
            this.playSound(SoundEvents.DOLPHIN_SPLASH, 1.6F, 0.5F);
        } else if (this.vortexCooldown <= 0 && distSq <= VORTEX_RANGE * VORTEX_RANGE) {
            this.pendingVortexTicks = VORTEX_HIT_DELAY;
            this.vortexCooldown = 220 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "vortex");
            this.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.8F, 0.5F);
        } else if (this.bladeCooldown <= 0 && distSq <= BLADE_RANGE * BLADE_RANGE) {
            // 齐射水刃:锁定落点,本体与全部拟影一起亮起蓄力光束
            this.bladeTargetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            this.pendingBladeTicks = BLADE_WINDUP_TICKS;
            this.bladeCooldown = 240 * cooldownScale / 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "blades");
            this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F, 1.6F);
        }
    }

    /** 潮汐冲刺预警:沿锁定方向铺设水纹线 + 自身环绕加速水花。 */
    private void spawnDashWarning(ServerLevel serverLevel) {
        Vec3 eye = this.position().add(0, this.getBbHeight() * 0.5, 0);
        BossFx.chargeSpiral(serverLevel, eye, this.tickCount, ParticleTypes.BUBBLE_COLUMN_UP);
        for (int d = 1; d <= (int) DASH_RANGE; d += 2) {
            Vec3 p = eye.add(this.dashDirection.scale(d));
            serverLevel.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 3, 0.4, 0.2, 0.4, 0.0);
            if (d % 4 == 1) {
                serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                6, this.getBbWidth() * 0.5, 0.4, this.getBbWidth() * 0.5, 0.05);
    }

    /** 潮汐冲刺:沿预警线高速突进,走廊内的玩家受击退与伤害。 */
    private void doTidalDash(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9F;
        Vec3 start = this.position().add(0, this.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(DASH_RANGE, 4.0, DASH_RANGE),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(start);
            double along = toVictim.dot(this.dashDirection);
            if (along < 0 || along > DASH_RANGE) {
                continue;
            }
            double perpendicular = toVictim.subtract(this.dashDirection.scale(along)).length();
            if (perpendicular > 2.2) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(this.dashDirection.x * 0.9, 0.4, this.dashDirection.z * 0.9));
            victim.hurtMarked = true;
        }
        // 突进本体:一次强位移 + 尾迹水花
        this.setDeltaMovement(this.dashDirection.scale(2.2));
        this.hurtMarked = true;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.DOLPHIN_JUMP, SoundSource.HOSTILE, 1.8F, 0.5F);
        for (int d = 0; d <= (int) DASH_RANGE; d++) {
            Vec3 p = start.add(this.dashDirection.scale(d));
            serverLevel.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 6, 0.6, 0.6, 0.6, 0.1);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.02);
        }
        BossFx.sparkSpray(serverLevel, start, this.dashDirection, DASH_RANGE, ParticleTypes.BUBBLE_POP);
    }

    /** 齐射水刃蓄力:本体与拟影各自亮起一条指向落点的直线光束(全部都是真的,提示走位)。 */
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

    /** 沿 from→to 铺一条 GLOW 粒子光束。 */
    private void spawnBeam(ServerLevel serverLevel, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int) (delta.length() * 1.5));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 沿 from→落点 的直线走廊结算一次水刃伤害(本体与拟影共用)。 */
    private void damageAlongBlade(ServerLevel serverLevel, Vec3 from, float damage, boolean applySlow) {
        Vec3 delta = this.bladeTargetPos.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 dir = delta.normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BLADE_RANGE, 6.0, BLADE_RANGE),
                e -> !(e instanceof AbyssWatcher) && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(from);
            double along = toVictim.dot(dir);
            if (along < 0 || along > length + 2.0) {
                continue;
            }
            if (toVictim.subtract(dir.scale(along)).length() > 1.6) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            if (applySlow) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
            }
        }
    }

    /** 齐射水刃命中:本体水刃高伤附缓慢,拟影水刃低伤;落点水压炸开。 */
    private void doWaterBlades(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        Vec3 from = this.eyeBeamOrigin();
        this.damageAlongBlade(serverLevel, from, damage, true);
        spawnBeam(serverLevel, from, this.bladeTargetPos);
        // 拟影齐射:每条走廊低伤结算——躲开本体光束不等于安全,逼玩家看全场光束
        for (UUID id : this.cloneIds) {
            if (serverLevel.getEntity(id) instanceof AbyssWatcher clone && clone.isAlive()) {
                Vec3 cloneFrom = clone.eyeBeamOrigin();
                this.damageAlongBlade(serverLevel, cloneFrom, (float) CLONE_ATTACK_DAMAGE, false);
                spawnBeam(serverLevel, cloneFrom, this.bladeTargetPos);
            }
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.bladeTargetPos.x, this.bladeTargetPos.y, this.bladeTargetPos.z,
                14, 0.5, 0.5, 0.5, 0.12);
        serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                this.bladeTargetPos.x, this.bladeTargetPos.y, this.bladeTargetPos.z,
                20, 0.8, 0.8, 0.8, 0.15);
        BossFx.impactBurst(serverLevel, this.bladeTargetPos, ParticleTypes.BUBBLE_POP);
        serverLevel.playSound(null, BlockPos.containing(this.bladeTargetPos),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 0.8F);
    }

    /** 深渊漩涡:把范围内玩家向本体拖拽并造成中等伤害——水下暗流,打断走位与远程风筝。 */
    private void doVortexPull(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(VORTEX_RANGE, VORTEX_RANGE * 0.6, VORTEX_RANGE),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
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
        // 漩涡环粒子:由外向内三圈
        for (int ring = 0; ring < 3; ring++) {
            double r = VORTEX_RANGE - ring * 2.5;
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14 + ring * 0.5;
                serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r, 2, 0.1, 0.3, 0.1, 0.02);
            }
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), VORTEX_RANGE * 0.7, ParticleTypes.BUBBLE_COLUMN_UP);
    }

    /** 深渊脉冲:近身 AOE 爆发,高击退 + 短暂缓慢——惩罚贴脸硬打,逼玩家利用破防窗口。 */
    private void doBurstDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BURST_RANGE, 2.0, BURST_RANGE),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(dir.x * 1.6, 0.5, dir.z * 1.6));
                victim.hurtMarked = true;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.6F);
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    this.getX() + Math.cos(angle) * 2.5,
                    this.getY() + this.getBbHeight() * 0.5,
                    this.getZ() + Math.sin(angle) * 2.5, 3, 0.2, 0.3, 0.2, 0.08);
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), BURST_RANGE, ParticleTypes.BUBBLE_POP);
        BossFx.impactBurst(serverLevel, this.position().add(0, this.getBbHeight() * 0.5, 0), ParticleTypes.SCULK_SOUL);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        // 拟影:限时消散,不参与任何 Boss 逻辑
        if (this.isClone()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                // 黑潮墨丝:拟影不伪装本体,周身持续缠绕墨色雾丝,一眼可辨
                if (this.isAlive() && this.tickCount % 10 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                            2, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.01);
                }
                // 守卫的符文已被净化 → 撤岗,转为游荡骚扰
                if (this.guardPos != null && this.tickCount % 20 == 0) {
                    var state = serverLevel.getBlockState(this.guardPos);
                    if (!(state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock)
                            || !state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                        this.guardPos = null;
                    }
                }
                if (--this.cloneLifeTicks <= 0) {
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            this.getX(), this.getY() + 0.6, this.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                    this.discard();
                }
            }
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

        // 状态演出:帷幕期辉光螺旋环绕;破防期裂纹火花迸发(按 5 tick 节流,不逐 tick 广播)
        if (this.isAlive() && this.tickCount % 5 == 0) {
            if (this.isBroken()) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        4, this.getBbWidth() * 0.5, this.getBbHeight() * 0.35, this.getBbWidth() * 0.5, 0.04);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        3, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.08);
            } else {
                // 双股辉光螺旋:沿身体盘旋上升的"帷幕"
                double angle = this.tickCount * 0.35;
                double radius = this.getBbWidth() * 0.75;
                double yOffset = (this.tickCount % 40) / 40.0 * this.getBbHeight();
                for (int strand = 0; strand < 2; strand++) {
                    double a = angle + strand * Math.PI;
                    serverLevel.sendParticles(ParticleTypes.GLOW,
                            this.getX() + Math.cos(a) * radius,
                            this.getY() + yOffset,
                            this.getZ() + Math.sin(a) * radius, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }

        if (this.isBroken()) {
            if (--this.brokenTicksRemaining <= 0 && this.isAlive()) {
                restoreTideVeil(serverLevel);
            }
        } else if (this.tickCount % 10 == 0 && this.getTarget() != null) {
            if (this.cloneRespawnCooldown > 0) {
                this.cloneRespawnCooldown -= 10;
            }
            // 破防机关:先确保场地符文处于侵蚀状态(拟影按"仍被侵蚀的符文"分派岗位,顺序不能反)
            if (this.corruptedRunes.isEmpty()) {
                this.corruptRunes(serverLevel);
                // 场地没有符文时退化为"清空拟影破防"
                if (this.corruptedRunes.isEmpty() && !anyCloneAlive(serverLevel)
                        && this.cloneRespawnCooldown > 0) {
                    this.triggerBroken(serverLevel);
                }
            }
            // 黑潮守卫:保持拟影在岗(被清掉后隔一段时间重新分裂)
            if (!anyCloneAlive(serverLevel) && this.cloneRespawnCooldown <= 0) {
                this.summonClones(serverLevel);
                this.cloneRespawnCooldown = 200;
            }
        }

        // 战斗期间周期登记场地内玩家为参与者 + 多人血量扩容
        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
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
        this.tickSpecialAttacks(serverLevel);
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** 脱战回血:无存活目标持续 5 秒后,每秒回复 2% 最大生命(减员消耗战不可行,脱战即重置)。 */
    private void tickOutOfCombatRegen() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.outOfCombatTicks = 0;
            return;
        }
        if (++this.outOfCombatTicks > 100 && this.outOfCombatTicks % 20 == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.02F);
        }
    }

    // ---- 破防机关:侵蚀/净化水下符文 ----

    /** 侵蚀场地内全部水下符文(黑潮漫上符文,等待玩家净化)。 */
    private void corruptRunes(ServerLevel serverLevel) {
        this.corruptedRunes.clear();
        BlockPos center = arenaAnchor();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RUNE_SCAN_RADIUS, -RUNE_SCAN_HEIGHT, -RUNE_SCAN_RADIUS),
                center.offset(RUNE_SCAN_RADIUS, RUNE_SCAN_HEIGHT, RUNE_SCAN_RADIUS))) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock) {
                serverLevel.setBlock(pos,
                        state.setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED, Boolean.TRUE), 3);
                this.corruptedRunes.add(pos.immutable());
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.04);
            }
        }
        if (!this.corruptedRunes.isEmpty()) {
            serverLevel.playSound(null, center,
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.7F);
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.runes_corrupted"));
        }
    }

    private BlockPos arenaAnchor() {
        return this.homePos != null ? this.homePos : this.blockPosition();
    }

    /** 玩家净化一块符文时由 TideRuneBlock 调用:全部净化 → 破防窗口。 */
    public void onRunePurified(Player purifier) {
        if (this.level().isClientSide || this.isBroken()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.participants.add(purifier.getUUID());
        int remaining = 0;
        for (BlockPos pos : this.corruptedRunes) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                    && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                remaining++;
            }
        }
        if (remaining <= 0) {
            this.corruptedRunes.clear();
            this.triggerBroken(serverLevel);
        } else {
            broadcastNearby(Component.translatable("message.unknown_echoes.watcher.rune_progress",
                    remaining));
        }
    }

    /** 潮汐破防:水面投影裂开,真身短暂暴露。 */
    private void triggerBroken(ServerLevel serverLevel) {
        this.setBroken(true);
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.70F, 0.35F);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.6F);
        // 破防演出:中心音爆 + 三圈火花涟漪向外扩散("倒影裂开")
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 2.0;
            for (int i = 0; i < 12 + ring * 6; i++) {
                double angle = Math.PI * 2 * i / (12 + ring * 6);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.55,
                        this.getZ() + Math.sin(angle) * r, 1, 0.1, 0.2, 0.1, 0.06);
            }
        }
        broadcastNearby(Component.translatable("message.unknown_echoes.watcher.broken"));
    }

    /** 多人扩容:参与者每多一人,血量上限 +100 并补上同量血(只升不降)。 */
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide
                && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (this.isClone()) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker) {
            this.participants.add(attacker.getUUID());
        }
        if (!this.level().isClientSide && !this.isBroken()
                && BossPhaseGate.shouldCapMechanicDamage(source)) {
            float multiplier = ServerConfig.BOSS_UNBROKEN_DAMAGE_MULTIPLIER.get().floatValue();
            float cap = ServerConfig.BOSS_UNBROKEN_MAX_DAMAGE.get().floatValue();
            amount = Math.min(amount * multiplier, cap);
            if (this.veiledMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.unknown_echoes.watcher.veiled"), true);
                this.veiledMessageCooldown = 40;
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
                && BossPhaseGate.reachedFloor(this, this.brokenWindowHealthFloor)
                && this.level() instanceof ServerLevel serverLevel) {
            restoreTideVeil(serverLevel);
        }
        return hurt;
    }

    private void restoreTideVeil(ServerLevel serverLevel) {
        this.setBroken(false);
        this.brokenTicksRemaining = 0;
        this.brokenWindowHealthFloor = 0.0F;
        this.corruptRunes(serverLevel);
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
        broadcastNearby(Component.translatable("message.unknown_echoes.watcher.veil_restored"));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.isClone() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.discardClones(serverLevel);
        // 死亡演出:全场粒子向核心收束 + 外扩水环(设计文档:死亡以粒子收束为主,无关键掉落)
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 1.8;
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16 + ring * 0.4;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, -0.12);
            }
        }
        serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                40, 1.5, 1.0, 1.5, 0.2);
        // 清理战斗状态:残余侵蚀符文恢复平静
        for (BlockPos pos : this.corruptedRunes) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof cn.kurt6.unknown_echoes.block.tide.TideRuneBlock
                    && state.getValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED)) {
                serverLevel.setBlock(pos,
                        state.setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneBlock.CORRUPTED, Boolean.FALSE), 3);
            }
        }
        this.corruptedRunes.clear();
        // 设计红线 #4:奖励只发给死亡时仍在场地范围内的参与玩家
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : serverLevel.players()) {
            if (this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= radius * radius) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                        EchoPermission.ABYSS_WATCHER_ID);
                EchoAbilityManager.markBossDefeated(player, EchoPermission.ABYSS_WATCHER_ID);
                if (firstKill) {
                    BossMaterialRewards.givePersonal(player, EchoPermission.ABYSS_WATCHER_ID,
                            new ItemStack(ModItems.TIDE_CORE.get()));
                }
                BossMaterialRewards.giveOrdinary(player, new ItemStack(ModItems.WATCHER_EYE_SHARD.get(),
                        2 + this.random.nextInt(3)));
                BossMaterialRewards.giveOrdinary(player, new ItemStack(ModItems.BLACK_TIDE_THREAD.get(),
                        1 + this.random.nextInt(2)));
            }
        }
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
        // 本体不自然消失;分身由计时器消散
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ---- 音效(守卫者系,水下氛围) ----

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? SoundEvents.GUARDIAN_HURT : SoundEvents.GUARDIAN_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? SoundEvents.GUARDIAN_DEATH : SoundEvents.GUARDIAN_DEATH_LAND;
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsClone", this.isClone());
        tag.putInt("CloneLife", this.cloneLifeTicks);
        if (this.guardPos != null) {
            tag.put("GuardPos", new net.minecraft.nbt.IntArrayTag(
                    new int[]{this.guardPos.getX(), this.guardPos.getY(), this.guardPos.getZ()}));
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
        if (tag.getBoolean("IsClone")) {
            this.becomeClone();
            this.cloneLifeTicks = tag.getInt("CloneLife");
            if (tag.contains("GuardPos", Tag.TAG_INT_ARRAY)) {
                int[] coords = tag.getIntArray("GuardPos");
                if (coords.length == 3) {
                    this.guardPos = new BlockPos(coords[0], coords[1], coords[2]);
                }
            }
        }
        this.setBroken(tag.getBoolean("Broken"));
        this.brokenTicksRemaining = tag.getInt("BrokenTicks");
        this.brokenWindowHealthFloor = tag.getFloat("BrokenWindowHealthFloor");
        this.cloneIds.clear();
        for (Tag entry : tag.getList("CloneIds", Tag.TAG_INT_ARRAY)) {
            this.cloneIds.add(NbtUtils.loadUUID(entry));
        }
        this.participants.clear();
        for (Tag entry : tag.getList("Participants", Tag.TAG_INT_ARRAY)) {
            this.participants.add(NbtUtils.loadUUID(entry));
        }
        this.scaledParticipants = Math.max(1, tag.getInt("ScaledParticipants"));
        this.corruptedRunes.clear();
        ListTag runes = tag.getList("CorruptedRunes", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < runes.size(); i++) {
            int[] coords = runes.getIntArray(i);
            if (coords.length == 3) {
                this.corruptedRunes.add(new BlockPos(coords[0], coords[1], coords[2]));
            }
        }
        int[] home = tag.getIntArray("HomePos");
        if (home.length == 3) {
            this.homePos = new BlockPos(home[0], home[1], home[2]);
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

    // ---- GeckoLib 动画 ----

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.SQUID_INK);
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /** 拟影被击杀时用墨雾散逸替代死亡表现。 */
    @Override
    public void remove(RemovalReason reason) {
        if (this.isClone() && reason == RemovalReason.KILLED
                && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.6, this.getZ(), 16, 0.4, 0.4, 0.4, 0.08);
        }
        super.remove(reason);
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isInWater();
    }

    /** 水下移动考验:本体在水里不受溺水/下沉惩罚,但离水后行动迟缓。 */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.isInWater() && this.onGround()) {
            // 离水搁浅:缓慢扑腾(降低威胁,鼓励玩家在水下正面应对)
            if (this.tickCount % 40 == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(
                        (this.random.nextFloat() * 2.0F - 1.0F) * 0.1F, 0.3,
                        (this.random.nextFloat() * 2.0F - 1.0F) * 0.1F));
            }
        }
    }
}
