package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * 镜像守护者:第三主线 Boss / 真视回响能力守护者(镜湖神殿)。
 *
 * 阶段(按设计文档 10.5):
 * - 幻象布场:进战后生成多个镜面假身(假身会"佯攻"但不造成伤害);未破防时伤害 = min(伤害 x 5%, 上限 4.0)。
 * - 真实辨认:玩家观察"倒影涟漪"并激活神殿全部真镜像符印(MirrorSigilBlock 个人记录,触假会重置)。
 * - 真视破防:激活全部真符印的玩家再触碰任一真符印 → 全部假影向本体收束,真实核心暴露,短输出窗口。
 * - 镜面崩裂:低血量时换位更频繁、假身更少(降低视觉噪声),攻击仍保留完整前摇。
 * 攻击:幻象斩击(真斩低音、假斩高音)/ 镜面换位(与假身换位,留下镜尘残影)/
 *       镜光乱射(三波扇形镜光,附虚弱)/ 镜刺突袭(直线镜刺浪涌,逐段推进)/
 *       镜环爆发(贴身环形冲击,惩罚持续贴脸);假目标爆裂 = 击碎假身时的小范围幻象冲击(反制惩罚)。
 * 死亡:场地内参与玩家个人写入击败记录与真视回响;掉落表只有普通材料。
 */
public class MirrorGuardian extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_guardian.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_guardian.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_guardian.broken");
    private static final RawAnimation BARRAGE_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.barrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SWAP_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.swap", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation THRUST_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.thrust", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ILLUSION =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.BOOLEAN);

    private static final int ILLUSION_LIFETIME_TICKS = 1200;
    private static final double ILLUSION_MAX_HEALTH = 10.0D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    private int illusionLifeTicks = ILLUSION_LIFETIME_TICKS;
    private int brokenTicksRemaining = 0;
    private float brokenWindowHealthFloor = 0.0F;
    private int veiledMessageCooldown = 0;
    private int illusionRespawnCooldown = 0;
    private int outOfCombatTicks = 0;
    private final List<UUID> illusionIds = new ArrayList<>();
    private final Set<UUID> participants = new HashSet<>();

    // ---- 特殊攻击:动画先行,伤害延迟到命中帧 ----
    private static final int SLASH_HIT_DELAY = 9;      // 幻象斩击:0.45s 抬臂,对应动画落刃帧
    private static final int BARRAGE_WAVE_DELAY = 6;   // 镜光乱射:三波,每波间隔 0.3s
    private static final int NOVA_CHARGE_TICKS = 16;   // 镜环爆发:0.8s 收束前摇
    private static final int SPIKE_TELEGRAPH_TICKS = 12; // 镜刺突袭:0.6s 地面光线预警
    private static final double SLASH_RANGE = 5.0D;
    private static final double BARRAGE_RANGE = 9.0D;
    private static final double NOVA_RANGE = 3.5D;
    private static final double NOVA_BURST_RADIUS = 4.5D;
    private static final double SPIKE_RANGE = 14.0D;
    private static final double SPIKE_MAX_DIST = 12.0D;
    private int slashCooldown = 0;
    private int swapCooldown = 0;
    private int barrageCooldown = 0;
    private int novaCooldown = 0;
    private int spikeCooldown = 0;
    private int pendingSlashTicks = 0;
    private int pendingBarrageWaves = 0;
    private int barrageWaveTimer = 0;
    private int pendingNovaTicks = 0;
    private int spikeTelegraphTicks = 0;
    /** 镜刺浪涌当前推进距离,-1 = 未激活。 */
    private double spikeWaveDist = -1.0D;
    private Vec3 spikeDir = Vec3.ZERO;
    private Vec3 spikeOrigin = Vec3.ZERO;
    private final Set<UUID> spikeHitVictims = new HashSet<>();

    /** 多人扩容:参与者每多一人,血量上限 +160(只升不降,NBT 持久化)。 */
    private int scaledParticipants = 1;
    private static final double BASE_MAX_HEALTH = 475.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 160.0D;

    /** 场地锚点:出生点即神殿中心;假身落点、镜面换位与游荡都被锁在这个范围内,防止打到场外。 */
    private static final int ARENA_RADIUS = 10;
    private BlockPos homePos = null;

    public MirrorGuardian(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 475.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
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
        builder.define(DATA_BROKEN, false);
        builder.define(DATA_ILLUSION, false);
    }

    public boolean isBroken() {
        return this.entityData.get(DATA_BROKEN);
    }

    private void setBroken(boolean broken) {
        this.entityData.set(DATA_BROKEN, broken);
    }

    public boolean isIllusion() {
        return this.entityData.get(DATA_ILLUSION);
    }

    /** 把这只实例标记为镜面假身:低血量、佯攻不造成伤害、无掉落无经验、限时碎裂。 */
    public void becomeIllusion() {
        this.entityData.set(DATA_ILLUSION, true);
        this.xpReward = 0;
        this.illusionLifeTicks = ILLUSION_LIFETIME_TICKS;
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(ILLUSION_MAX_HEALTH);
        }
        this.setHealth((float) ILLUSION_MAX_HEALTH);
    }

    // ---- 幻象布场 ----

    /** 镜面崩裂阶段(低血量)假身更少,降低视觉噪声。 */
    private int illusionCount() {
        return this.getHealth() < this.getMaxHealth() * 0.3F ? 2 : 3;
    }

    private void summonIllusions(ServerLevel serverLevel) {
        this.illusionIds.clear();
        BlockPos anchor = this.homePos != null ? this.homePos : this.blockPosition();
        for (int i = 0; i < this.illusionCount(); i++) {
            MirrorGuardian illusion =
                    this.getType().create(serverLevel) instanceof MirrorGuardian guardian ? guardian : null;
            if (illusion == null) {
                continue;
            }
            Vec3 spot = this.findArenaSpot(serverLevel, anchor);
            illusion.moveTo(spot.x, spot.y, spot.z, this.random.nextFloat() * 360.0F, 0.0F);
            illusion.becomeIllusion();
            illusion.restrictTo(anchor, ARENA_RADIUS);   // 假身游荡同样锁场地
            illusion.setTarget(this.getTarget());
            illusion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(illusion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(illusion);
            this.illusionIds.add(illusion.getUUID());
            // 半透明镜尘:假身出场演出
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    illusion.getX(), illusion.getY() + 1.5, illusion.getZ(), 24, 0.5, 1.0, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    illusion.getX(), illusion.getY() + 1.5, illusion.getZ(), 8, 0.3, 0.8, 0.3, 0.02);
        }
        if (!this.illusionIds.isEmpty()) {
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_GUARDIAN_AMBIENT.get(), SoundSource.HOSTILE, 1.6F, 0.5F);
            broadcastNearby(Component.translatable("message.unknown_echoes.guardian.illusions"));
        }
    }

    /** 在场地锚点范围内找一个可站立的空位(头脚两格为空、脚下有地);找不到就退回锚点旁。 */
    private Vec3 findArenaSpot(ServerLevel serverLevel, BlockPos anchor) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 2.0 + this.random.nextDouble() * (ARENA_RADIUS - 3);
            BlockPos candidate = BlockPos.containing(
                    anchor.getX() + 0.5 + Math.cos(angle) * dist,
                    anchor.getY(),
                    anchor.getZ() + 0.5 + Math.sin(angle) * dist);
            // 落点贴地修正:最多向下找 3 格地面
            for (int dy = 0; dy <= 3; dy++) {
                BlockPos feet = candidate.below(dy);
                if (!serverLevel.getBlockState(feet).isAir()) {
                    break;
                }
                if (serverLevel.getBlockState(feet.below()).isSolidRender(serverLevel, feet.below())
                        && serverLevel.getBlockState(feet.above()).isAir()
                        && serverLevel.getBlockState(feet.above(2)).isAir()) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        return Vec3.atBottomCenterOf(anchor.east(2));
    }

    private List<MirrorGuardian> aliveIllusions(ServerLevel serverLevel) {
        List<MirrorGuardian> result = new ArrayList<>();
        for (UUID id : this.illusionIds) {
            if (serverLevel.getEntity(id) instanceof MirrorGuardian illusion && illusion.isAlive()) {
                result.add(illusion);
            }
        }
        return result;
    }

    private void discardIllusions(ServerLevel serverLevel, boolean converge) {
        for (UUID id : this.illusionIds) {
            if (serverLevel.getEntity(id) instanceof MirrorGuardian illusion) {
                if (converge) {
                    // 破防演出:假影化作镜尘流向真实核心
                    spawnConvergence(serverLevel, illusion.position().add(0, 1.5, 0));
                }
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                        illusion.getX(), illusion.getY() + 1.2, illusion.getZ(), 16, 0.4, 0.8, 0.4, 0.04);
                illusion.discard();
            }
        }
        this.illusionIds.clear();
    }

    /** 从某点向本体核心铺一条收束粒子线。 */
    private void spawnConvergence(ServerLevel serverLevel, Vec3 from) {
        Vec3 to = this.position().add(0, this.getBbHeight() * 0.6, 0);
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int) (delta.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
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

    // ---- 真实辨认 → 真视破防:由 MirrorSigilBlock 在真符印被激活时调用 ----

    /** 该玩家已亲自激活本神殿全部真符印 → 假影收束,真实核心暴露。 */
    public void onSigilActivated(ServerPlayer activator) {
        if (this.level().isClientSide || this.isBroken() || this.isIllusion()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.participants.add(activator.getUUID());
        List<BlockPos> realSigils = findNearbyRealSigils(serverLevel);
        if (realSigils.isEmpty()) {
            return;
        }
        int activated = 0;
        for (BlockPos sigil : realSigils) {
            if (EchoAbilityManager.hasActivatedMechanism(activator,
                    MirrorSigilBlock.mechanismKey(serverLevel, sigil))) {
                activated++;
            }
        }
        if (activated >= realSigils.size()) {
            this.triggerBroken(serverLevel);
        } else {
            broadcastNearby(Component.translatable("message.unknown_echoes.guardian.sigil_progress",
                    activated, realSigils.size()));
        }
    }

    private List<BlockPos> findNearbyRealSigils(ServerLevel serverLevel) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-20, -8, -20), center.offset(20, 8, 20))) {
            var state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof MirrorSigilBlock
                    && cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    /** 真视破防:所有假影向真实核心收束,守护者跪地暴露。 */
    private void triggerBroken(ServerLevel serverLevel) {
        this.discardIllusions(serverLevel, true);
        this.setBroken(true);
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.75F, 0.40F);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_SHATTER.get(), SoundSource.HOSTILE, 2.0F, 0.9F);
        // 破防演出:核心白光迸发 + 镜尘螺旋坍缩
        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 1.6;
            for (int i = 0; i < 10 + ring * 6; i++) {
                double angle = Math.PI * 2 * i / (10 + ring * 6);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.6,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.2, 0.05, -0.08);
            }
        }
        broadcastNearby(Component.translatable("message.unknown_echoes.guardian.broken"));
    }

    // ---- 特殊攻击 ----

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.slashCooldown > 0) this.slashCooldown--;
        if (this.swapCooldown > 0) this.swapCooldown--;
        if (this.barrageCooldown > 0) this.barrageCooldown--;
        if (this.novaCooldown > 0) this.novaCooldown--;
        if (this.spikeCooldown > 0) this.spikeCooldown--;

        if (this.pendingSlashTicks > 0) {
            // 斩击前摇:刃前镜光聚集,给玩家可读的躲避窗口
            Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + facing.x * 1.2, this.getY() + 1.6, this.getZ() + facing.z * 1.2,
                    2, 0.15, 0.15, 0.15, 0.02);
            BossFx.chargeSpiral(serverLevel,
                    this.position().add(facing.scale(1.2)).add(0, 1.0, 0), this.tickCount, ParticleTypes.END_ROD);
            if (--this.pendingSlashTicks == 0) {
                this.doSlashDamage(serverLevel);
            }
        }
        if (this.pendingBarrageWaves > 0 && --this.barrageWaveTimer <= 0) {
            this.doBarrageWave(serverLevel);
            this.pendingBarrageWaves--;
            this.barrageWaveTimer = BARRAGE_WAVE_DELAY;
        }
        if (this.pendingNovaTicks > 0) {
            this.tickNovaCharge(serverLevel);
            if (--this.pendingNovaTicks == 0) {
                this.doNovaBurst(serverLevel);
            }
        }
        if (this.spikeTelegraphTicks > 0) {
            this.tickSpikeTelegraph(serverLevel);
            if (--this.spikeTelegraphTicks == 0) {
                this.spikeWaveDist = 1.5D;
                this.spikeHitVictims.clear();
            }
        } else if (this.spikeWaveDist >= 0) {
            this.tickSpikeWave(serverLevel);
        }
        if (!this.isAlive() || this.pendingSlashTicks > 0 || this.pendingBarrageWaves > 0
                || this.pendingNovaTicks > 0 || this.spikeTelegraphTicks > 0 || this.spikeWaveDist >= 0) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // 镜面崩裂阶段(低血量):换位明显提速
        boolean shattering = this.getHealth() < this.getMaxHealth() * 0.3F;
        double distSq = this.distanceToSqr(target);
        if (this.slashCooldown <= 0 && distSq <= SLASH_RANGE * SLASH_RANGE) {
            // 幻象斩击:真斩低音前摇;全部假身同步佯攻(高音),考验听觉辨识
            this.pendingSlashTicks = SLASH_HIT_DELAY;
            this.slashCooldown = 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.5F);   // 真:低音
            for (MirrorGuardian illusion : aliveIllusions(serverLevel)) {
                illusion.triggerAnim("attack_controller", "attack");
                illusion.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.5F); // 假:高音
            }
        } else if (this.novaCooldown <= 0 && distSq <= NOVA_RANGE * NOVA_RANGE) {
            // 镜环爆发:斩击不可用时仍被贴脸 → 收束镜环把玩家炸退
            this.pendingNovaTicks = NOVA_CHARGE_TICKS;
            this.novaCooldown = 220;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "barrage");
            this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.35F);
        } else if (this.barrageCooldown <= 0 && distSq <= BARRAGE_RANGE * BARRAGE_RANGE
                && distSq > NOVA_RANGE * NOVA_RANGE && this.hasLineOfSight(target)) {
            // 镜光乱射:中距离主力。必须排在镜面换位之前——换位只看冷却不看距离,
            // 排在前面会吃掉几乎全部出手窗口(此前乱射在实战中基本放不出来就是这个原因)
            this.pendingBarrageWaves = 3;
            this.barrageWaveTimer = SLASH_HIT_DELAY;     // 第一波也有前摇
            this.barrageCooldown = 200;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "barrage");
            this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.8F);
        } else if (this.spikeCooldown <= 0 && distSq <= SPIKE_RANGE * SPIKE_RANGE
                && distSq > SLASH_RANGE * SLASH_RANGE) {
            this.startSpikeSurge(target);
        } else if (this.swapCooldown <= 0 && !this.isBroken()) {
            List<MirrorGuardian> illusions = aliveIllusions(serverLevel);
            // 换位锁场地:被引出场地的假身不参与换位,防止真身被换到场外
            if (this.homePos != null) {
                illusions.removeIf(illusion ->
                        !illusion.blockPosition().closerThan(this.homePos, ARENA_RADIUS + 2));
            }
            if (!illusions.isEmpty()) {
                this.doMirrorSwap(serverLevel, illusions.get(this.random.nextInt(illusions.size())));
                this.swapCooldown = shattering ? 100 : 220;
            }
        }
    }

    /** 幻象斩击命中:正面约 120° 扇形;只有真身造成伤害。 */
    private void doSlashDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SLASH_RANGE, 2.0, SLASH_RANGE),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.5) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.setDeltaMovement(victim.getDeltaMovement()
                    .add(facing.x * 0.6, 0.25, facing.z * 0.6));
            victim.hurtMarked = true;
        }
        // 斩击弧光:沿面向方向扫出一道镜光弧
        for (int i = -3; i <= 3; i++) {
            double angle = Math.atan2(facing.z, facing.x) + i * 0.22;
            for (double d = 1.5; d <= SLASH_RANGE - 1; d += 1.2) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * d,
                        this.getY() + 1.4,
                        this.getZ() + Math.sin(angle) * d, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_ATTACK.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.8, 0), facing, SLASH_RANGE, ParticleTypes.END_ROD);
        BossFx.impactBurst(serverLevel, this.position().add(facing.scale(2.4)).add(0, 1.2, 0), ParticleTypes.END_ROD);
    }

    /** 镜面换位:与一个假身交换位置,旋身消隐 + 旧位置留下镜尘残影。 */
    private void doMirrorSwap(ServerLevel serverLevel, MirrorGuardian illusion) {
        Vec3 myPos = this.position();
        Vec3 illusionPos = illusion.position();
        this.triggerAnim("attack_controller", "swap");
        illusion.triggerAnim("attack_controller", "swap");
        // 换位双向演出:两个位置各起一柱镜尘
        for (Vec3 p : List.of(myPos, illusionPos)) {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, p.x, p.y + 1.5, p.z, 30, 0.4, 1.2, 0.4, 0.06);
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.5, p.z, 10, 0.3, 1.0, 0.3, 0.03);
            BossFx.chargeSpiral(serverLevel, p.add(0, 0.3, 0), this.tickCount, ParticleTypes.WHITE_ASH);
        }
        this.teleportTo(illusionPos.x, illusionPos.y, illusionPos.z);
        illusion.teleportTo(myPos.x, myPos.y, myPos.z);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_SWAP.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
    }

    /** 镜光乱射:正面约 100° 扇形一波镜光,低伤害 + 虚弱,三波连发。 */
    private void doBarrageWave(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
        Vec3 facing = Vec3.directionFromRotation(0, this.yHeadRot).normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BARRAGE_RANGE, 3.0, BARRAGE_RANGE),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.64) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        // 镜光扇:胸口辉光 + 九道放射镜光弹道 + 沿途光屑
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getY() + 1.5, this.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        for (int i = -4; i <= 4; i++) {
            double angle = Math.atan2(facing.z, facing.x) + i * 0.14;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);
            // count=0 时 delta 作为粒子速度:END_ROD 沿射线飞出,形成可见弹道
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + vx * 1.0, this.getY() + 1.5, this.getZ() + vz * 1.0,
                    0, vx * 0.8, 0.0, vz * 0.8, 1.0);
            for (double d = 2.0; d <= BARRAGE_RANGE; d += 2.0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + vx * d, this.getY() + 1.5, this.getZ() + vz * d,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
        // 三波音高递增,听觉上形成"连射节奏"
        float wavePitch = 1.0F + (3 - this.pendingBarrageWaves) * 0.15F;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.6F, wavePitch);
    }

    /** 镜环爆发前摇:外圈镜光向核心收束,半径随倒计时缩小——离它远点。 */
    private void tickNovaCharge(ServerLevel serverLevel) {
        double r = NOVA_BURST_RADIUS * this.pendingNovaTicks / (double) NOVA_CHARGE_TICKS + 0.5;
        double y = this.getY() + this.getBbHeight() * 0.55;
        BossFx.chargeSpiral(serverLevel, this.position().add(0, this.getBbHeight() * 0.25, 0),
                this.tickCount, ParticleTypes.END_ROD);
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2 * i / 10 + this.tickCount * 0.25;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + Math.cos(angle) * r, y, this.getZ() + Math.sin(angle) * r,
                    0, -Math.cos(angle) * 0.25, 0.0, -Math.sin(angle) * 0.25, 1.0);
        }
    }

    /** 镜环爆发:贴身环形冲击,中伤害 + 强击退,惩罚无脑贴脸。 */
    private void doNovaBurst(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(NOVA_BURST_RADIUS, 2.0, NOVA_BURST_RADIUS),
                e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 1.1, 0.45, dir.z * 1.1));
                victim.hurtMarked = true;
            }
        }
        // 演出:核心白闪 + 三层高度递进的镜屑放射环
        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 0; ring < 3; ring++) {
            int points = 18 + ring * 6;
            double speed = 0.35 + ring * 0.15;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2 * i / points + ring * 0.2;
                serverLevel.sendParticles(
                        ring == 1 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 0.9 + ring * 0.4, this.getZ(),
                        0, Math.cos(angle) * speed, 0.05, Math.sin(angle) * speed, 1.0);
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_SHATTER.get(), SoundSource.HOSTILE, 1.8F, 0.7F);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), NOVA_BURST_RADIUS, ParticleTypes.END_ROD);
        BossFx.impactBurst(serverLevel, this.position().add(0, this.getBbHeight() * 0.55, 0), ParticleTypes.END_ROD);
    }

    /** 镜刺突袭起手:锁定目标方向,进入地面预警。 */
    private void startSpikeSurge(LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        this.spikeDir = dir.normalize();
        this.spikeOrigin = this.position();
        this.spikeTelegraphTicks = SPIKE_TELEGRAPH_TICKS;
        this.spikeCooldown = 240;
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", "thrust");
        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.7F);
    }

    /** 镜刺预警:沿攻击路径在地面铺一条闪烁光线。 */
    private void tickSpikeTelegraph(ServerLevel serverLevel) {
        for (double d = 1.5; d <= SPIKE_MAX_DIST; d += 1.0) {
            Vec3 p = this.spikeOrigin.add(this.spikeDir.scale(d));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.x, this.spikeOrigin.y + 0.1, p.z, 1, 0.1, 0.02, 0.1, 0.01);
        }
    }

    /** 镜刺浪涌:镜刺逐段破地推进(每 tick 两段),沿线中伤害 + 轻挑空,单次施放每人只命中一次。 */
    private void tickSpikeWave(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            this.spikeWaveDist = -1.0D;
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        for (int seg = 0; seg < 2 && this.spikeWaveDist >= 0; seg++) {
            Vec3 p = this.spikeOrigin.add(this.spikeDir.scale(this.spikeWaveDist));
            // 镜刺破地:竖直迸发的镜屑柱
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    p.x, this.spikeOrigin.y + 0.2, p.z, 10, 0.2, 0.05, 0.2, 0.3);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.x, this.spikeOrigin.y + 0.6, p.z, 6, 0.25, 0.5, 0.25, 0.1);
            BossFx.groundShockwave(serverLevel, new Vec3(p.x, this.spikeOrigin.y + 0.1, p.z),
                    1.8, ParticleTypes.END_ROD);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                            p.x - 1.2, this.spikeOrigin.y - 1.0, p.z - 1.2,
                            p.x + 1.2, this.spikeOrigin.y + 2.5, p.z + 1.2),
                    e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy)
                            && !this.spikeHitVictims.contains(e.getUUID()))) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.4, 0.0));
                victim.hurtMarked = true;
                this.spikeHitVictims.add(victim.getUUID());
            }
            this.spikeWaveDist += 1.5D;
            if (this.spikeWaveDist > SPIKE_MAX_DIST) {
                this.spikeWaveDist = -1.0D;
            }
        }
        if (this.tickCount % 2 == 0 && this.spikeWaveDist >= 0) {
            Vec3 head = this.spikeOrigin.add(this.spikeDir.scale(this.spikeWaveDist));
            serverLevel.playSound(null, BlockPos.containing(head),
                    SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 1.5F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        // 假身:限时碎裂;佯攻由 doHurtTarget 处理
        if (this.isIllusion()) {
            if (--this.illusionLifeTicks <= 0) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                            this.getX(), this.getY() + 1.2, this.getZ(), 16, 0.4, 0.8, 0.4, 0.04);
                }
                this.discard();
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 场地锚点:出生位置即神殿中心,游荡/换位/假身全部锁在范围内
        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, ARENA_RADIUS);
        }
        if (this.veiledMessageCooldown > 0) {
            this.veiledMessageCooldown--;
        }

        // 真身辨识线索:本体周期发出低沉镜鸣(假身静默)
        if (this.isAlive() && this.tickCount % 70 == 0) {
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.9F, 0.45F);
        }

        // 状态演出:帷幕期镜尘环绕;破防期核心白光迸发(5 tick 节流)
        if (this.isAlive() && this.tickCount % 5 == 0) {
            if (this.isBroken()) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        4, 0.5, 0.8, 0.5, 0.05);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        2, 0.4, 0.6, 0.4, 0.08);
            } else {
                double angle = this.tickCount * 0.3;
                double radius = this.getBbWidth() * 1.1;
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                        this.getX() + Math.cos(angle) * radius,
                        this.getY() + 0.4 + (this.tickCount % 50) / 50.0 * 2.2,
                        this.getZ() + Math.sin(angle) * radius, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }

        if (this.isBroken()) {
            if (--this.brokenTicksRemaining <= 0 && this.isAlive()) {
                restoreMirrorVeil(serverLevel);
            }
        } else if (this.tickCount % 10 == 0 && this.getTarget() != null) {
            // 幻象布场:保持假身在场
            if (this.illusionRespawnCooldown > 0) {
                this.illusionRespawnCooldown -= 10;
            }
            if (aliveIllusions(serverLevel).isEmpty() && this.illusionRespawnCooldown <= 0) {
                this.summonIllusions(serverLevel);
                this.illusionRespawnCooldown = 160;
            }
        }

        // 参与者登记 + 多人扩容
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
        if (this.isIllusion()) {
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
                player.displayClientMessage(Component.translatable("message.unknown_echoes.guardian.veiled"), true);
                this.veiledMessageCooldown = 40;
            }
        } else if (!this.level().isClientSide && this.isBroken()
                && BossPhaseGate.shouldCapMechanicDamage(source)) {
            amount = BossPhaseGate.capDamage(this.getHealth(), amount, this.brokenWindowHealthFloor);
            if (amount <= 0.0F) {
                return false;
            }
        }
        // 真身受击:迸出少量镜屑(假身保持原版反馈,避免又多一条辨识捷径)
        if (!this.level().isClientSide && amount > 0
                && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    8, 0.3, 0.5, 0.3, 0.05);
        }
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt
                && BossPhaseGate.reachedFloor(this, this.brokenWindowHealthFloor)
                && this.level() instanceof ServerLevel serverLevel) {
            restoreMirrorVeil(serverLevel);
        }
        return hurt;
    }

    private void restoreMirrorVeil(ServerLevel serverLevel) {
        this.setBroken(false);
        this.brokenTicksRemaining = 0;
        this.brokenWindowHealthFloor = 0.0F;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
        broadcastNearby(Component.translatable("message.unknown_echoes.guardian.veil_restored"));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 假目标爆裂:假身被击碎时小范围幻象冲击(低伤害 + 击退,不是惩罚性卡关)
        if (this.isIllusion()) {
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(3.0, 1.5, 3.0),
                    e -> e != this && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
                victim.hurt(this.damageSources().mobAttack(this), 3.0F);
                Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.8, 0.3, dir.z * 0.8));
                    victim.hurtMarked = true;
                }
            }
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    this.getX(), this.getY() + 1.2, this.getZ(), 30, 0.8, 1.0, 0.8, 0.1);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_GUARDIAN_SHATTER.get(), SoundSource.HOSTILE, 1.4F, 1.6F);
            return;
        }
        this.discardIllusions(serverLevel, true);
        // 死亡演出:镜面崩解——白光收束 + 四圈镜尘坍缩
        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 1.5;
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14 + ring * 0.3;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, -0.1);
            }
        }
        // 设计红线 #4:奖励只发给死亡时仍在场地范围内的参与玩家
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : serverLevel.players()) {
            if (this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= radius * radius) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                        EchoPermission.MIRROR_GUARDIAN_ID);
                EchoAbilityManager.markBossDefeated(player, EchoPermission.MIRROR_GUARDIAN_ID);
                if (firstKill) {
                    BossMaterialRewards.givePersonal(player, EchoPermission.MIRROR_GUARDIAN_ID,
                            new ItemStack(ModItems.TRUE_SIGHT_CORE.get()));
                }
                BossMaterialRewards.giveOrdinary(player, new ItemStack(ModItems.BROKEN_REFLECTION.get(),
                        2 + this.random.nextInt(3)));
                BossMaterialRewards.giveOrdinary(player, new ItemStack(ModItems.MIRROR_GUARDIAN_CORE.get()));
                if (ServerConfig.PERSONAL_KEY_REWARDS.get()) {
                    EchoAbilityManager.unlockAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);
                }
            }
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.isIllusion() && super.shouldDropLoot();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isIllusion()) {
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
        // 本体不自然消失;假身由计时器碎裂
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ---- 音效(镜晶系,专属事件在 sounds.json 混合原版音源) ----

    @Override
    protected SoundEvent getAmbientSound() {
        // 假身静默:不发任何环境音,与本体周期镜鸣共同构成听觉辨识线索
        return this.isIllusion() ? null : ModSounds.MIRROR_GUARDIAN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIRROR_GUARDIAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIRROR_GUARDIAN_DEATH.get();
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsIllusion", this.isIllusion());
        tag.putInt("IllusionLife", this.illusionLifeTicks);
        tag.putBoolean("Broken", this.isBroken());
        tag.putInt("BrokenTicks", this.brokenTicksRemaining);
        tag.putFloat("BrokenWindowHealthFloor", this.brokenWindowHealthFloor);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        ListTag illusions = new ListTag();
        for (UUID id : this.illusionIds) {
            illusions.add(NbtUtils.createUUID(id));
        }
        tag.put("IllusionIds", illusions);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        if (this.homePos != null) {
            tag.putIntArray("HomePos", new int[]{this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsIllusion")) {
            this.becomeIllusion();
            this.illusionLifeTicks = tag.getInt("IllusionLife");
        }
        this.setBroken(tag.getBoolean("Broken"));
        this.brokenTicksRemaining = tag.getInt("BrokenTicks");
        this.brokenWindowHealthFloor = tag.getFloat("BrokenWindowHealthFloor");
        this.scaledParticipants = Math.max(1, tag.getInt("ScaledParticipants"));
        this.illusionIds.clear();
        for (Tag entry : tag.getList("IllusionIds", Tag.TAG_INT_ARRAY)) {
            this.illusionIds.add(NbtUtils.loadUUID(entry));
        }
        this.participants.clear();
        for (Tag entry : tag.getList("Participants", Tag.TAG_INT_ARRAY)) {
            this.participants.add(NbtUtils.loadUUID(entry));
        }
        int[] home = tag.getIntArray("HomePos");
        if (home.length == 3) {
            this.homePos = new BlockPos(home[0], home[1], home[2]);
            this.restrictTo(this.homePos, ARENA_RADIUS);
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
        // 假身只佯攻:挥臂 + 高音脆响,不造成伤害(真假斩击靠音高与伤害区分)
        if (this.isIllusion()) {
            if (!this.level().isClientSide) {
                this.triggerAnim("attack_controller", this.random.nextBoolean() ? "attack" : "thrust");
                this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.5F);
                this.spawnSwingDust();
            }
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            // 普通近战随机斩/刺两式,避免动作重复
            this.triggerAnim("attack_controller", this.random.nextBoolean() ? "attack" : "thrust");
            this.spawnSwingDust();
        }
        return hit;
    }

    /** 挥击镜尘:真假共用的近战挥臂粒子(刻意一致,辨识只靠音高与伤害)。 */
    private void spawnSwingDust() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                this.getX() + facing.x * 1.4, this.getY() + 1.4, this.getZ() + facing.z * 1.4,
                10, 0.5, 0.4, 0.5, 0.06);
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.7, 0), facing, SLASH_RANGE * 0.75, ParticleTypes.WHITE_ASH);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (!this.isIllusion() && this.isBroken()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("thrust", THRUST_ANIM)
                .triggerableAnim("swap", SWAP_ANIM)
                .triggerableAnim("barrage", BARRAGE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
