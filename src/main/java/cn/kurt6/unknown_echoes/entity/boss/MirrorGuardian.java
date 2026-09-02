package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlock;
import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.ai.CirclePreferredRangeGoal;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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

public class MirrorGuardian extends Monster implements GeoEntity, BossDamageGuard, PhaseGatedBoss,
        BossVisualState,
        RechallengeBoundBoss {
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
    private static final RawAnimation NOVA_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.nova", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SWAP_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.swap", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation THRUST_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.thrust", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SUMMON_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.summon", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SHATTER_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.shatter", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RESTORE_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.restore", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FRACTURE_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.fracture", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation COLLAPSE_ANIM =
            RawAnimation.begin().then("animation.mirror_guardian.collapse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_MODE =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_AGE =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ATTACK_DURATION =
            SynchedEntityData.defineId(MirrorGuardian.class, EntityDataSerializers.INT);
    /** 真假身份只存服务端:不入 SynchedEntityData,抓包/实体数据读不出真身。 */
    private boolean illusion = false;

    private static final int ILLUSION_LIFETIME_TICKS = 1200;
    private static final double ILLUSION_MAX_HEALTH = 10.0D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    private int illusionLifeTicks = ILLUSION_LIFETIME_TICKS;
    /** 仅假身使用；让跨区块重载后的孤立假身能验证并清理父 Boss。 */
    private UUID illusionOwnerUuid = null;
    private int brokenTicksRemaining = 0;
    private float brokenWindowHealthFloor = 0.0F;
    private int veiledMessageCooldown = 0;
    /** 崩裂阶段是否已宣告。必须持久化,否则重载后会再放一次演出;不参与伤害与冷却判定。 */
    private boolean fractureAnnounced = false;
    private int illusionRespawnCooldown = 0;
    private int outOfCombatTicks = 0;
    private final List<UUID> illusionIds = new ArrayList<>();
    private final Set<UUID> participants = new HashSet<>();
    private BlockPos rechallengeAltar;

    private static final int SLASH_HIT_DELAY = 9;
    private static final int BARRAGE_WAVE_DELAY = 6;
    private static final int NOVA_CHARGE_TICKS = 16;
    private static final int SPIKE_TELEGRAPH_TICKS = 12;
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

    private int scaledParticipants = 1;
    private static final double BASE_MAX_HEALTH = 475.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 160.0D;

    /** 场地锚点:出生点即神殿中心;假身落点、镜面换位与游荡都被锁在这个范围内,防止打到场外。 */
    private static final int ARENA_RADIUS = 14;
    private BlockPos homePos = null;

    public MirrorGuardian(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    /** 镜像守卫者的剑臂与装甲是外伸附肢，碰撞箱只覆盖主体并保留长方形底面。 */
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
                .add(Attributes.MAX_HEALTH, 475.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    /**
     * 绕圈半径必须落在弹幕(>NOVA_RANGE)与地刺(>SLASH_RANGE)的射程区间内,
     * 否则永久贴脸只会循环斩击与新星。优先级数必须小于近战,MOVE 只能被更小的数抢走。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CirclePreferredRangeGoal(this, SLASH_RANGE, 7.5D, 0.9D,
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

    public boolean isIllusion() {
        return this.illusion;
    }

    public void becomeIllusion() {
        this.illusion = true;
        this.xpReward = 0;
        this.illusionLifeTicks = ILLUSION_LIFETIME_TICKS;
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(ILLUSION_MAX_HEALTH);
        }
        this.setHealth((float) ILLUSION_MAX_HEALTH);
    }

    private void becomeIllusion(MirrorGuardian owner, BlockPos arenaCenter) {
        this.becomeIllusion();
        this.illusionOwnerUuid = owner.getUUID();
        this.homePos = arenaCenter.immutable();
        this.restrictTo(this.homePos, ARENA_RADIUS);
    }

    /** 镜面崩裂阶段(低血量)假身更少,降低视觉噪声。 */
    private int illusionCount() {
        return this.getHealth() < this.getMaxHealth() * 0.3F ? 2 : 3;
    }

    private void summonIllusions(ServerLevel serverLevel) {
        BlockPos anchor = this.homePos != null ? this.homePos : this.blockPosition();
        if (!BossNbtData.isHorizontalAreaLoaded(serverLevel, anchor, ARENA_RADIUS + 2)) {
            return;
        }
        this.illusionIds.clear();
        for (int i = 0; i < this.illusionCount(); i++) {
            MirrorGuardian illusion =
                    this.getType().create(serverLevel) instanceof MirrorGuardian guardian ? guardian : null;
            if (illusion == null) {
                continue;
            }
            Vec3 spot = this.findArenaSpot(serverLevel, anchor, illusion);
            if (spot == null) {
                illusion.discard();
                continue;
            }
            illusion.moveTo(spot.x, spot.y, spot.z, this.random.nextFloat() * 360.0F, 0.0F);
            illusion.becomeIllusion(this, anchor);
            illusion.setTarget(this.getTarget());
            illusion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(illusion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            if (!serverLevel.addFreshEntity(illusion)) {
                illusion.discard();
                continue;
            }
            this.illusionIds.add(illusion.getUUID());

            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                    illusion.getX(), illusion.getY() + 1.5, illusion.getZ(), 24, 0.5, 1.0, 0.5, 0.05);
            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                    illusion.getX(), illusion.getY() + 1.5, illusion.getZ(), 8, 0.3, 0.8, 0.3, 0.02);
        }
        if (!this.illusionIds.isEmpty()) {
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_GUARDIAN_AMBIENT.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
            broadcastNearby(Component.translatable("message.unknown_echoes.guardian.illusions"));
        }
    }

    /** 在场地锚点范围内找一个能容纳完整幻象碰撞箱的空位。 */
    @Nullable
    private Vec3 findArenaSpot(ServerLevel serverLevel, BlockPos anchor, MirrorGuardian illusion) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 2.0 + this.random.nextDouble() * (ARENA_RADIUS - 3);
            BlockPos candidate = BlockPos.containing(
                    anchor.getX() + 0.5 + Math.cos(angle) * dist,
                    anchor.getY(),
                    anchor.getZ() + 0.5 + Math.sin(angle) * dist);

            for (int dy = 0; dy <= 3; dy++) {
                BlockPos feet = candidate.below(dy);
                if (!serverLevel.hasChunkAt(feet)) {
                    break;
                }
                if (!serverLevel.getBlockState(feet).isAir()) {
                    break;
                }
                if (serverLevel.getBlockState(feet.below()).isSolidRender(serverLevel, feet.below())
                        && isSafeDestination(serverLevel, illusion, Vec3.atBottomCenterOf(feet))) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        return null;
    }

    private static boolean isSafeDestination(ServerLevel level, MirrorGuardian illusion,
                                             Vec3 destination) {
        AABB moved = illusion.getBoundingBox().move(destination.subtract(illusion.position()));
        BlockPos min = BlockPos.containing(moved.minX, moved.minY, moved.minZ);
        BlockPos max = BlockPos.containing(moved.maxX - 1.0E-7D,
                moved.maxY - 1.0E-7D, moved.maxZ - 1.0E-7D);
        if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)) {
            return false;
        }
        for (int x : new int[]{min.getX(), max.getX()}) {
            for (int z : new int[]{min.getZ(), max.getZ()}) {
                BlockPos corner = new BlockPos(x, min.getY(), z);
                if (!level.hasChunkAt(corner)) {
                    return false;
                }
            }
        }
        return level.getWorldBorder().isWithinBounds(moved) && level.noCollision(illusion, moved);
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

                    spawnConvergence(serverLevel, illusion.position().add(0, 1.5, 0));
                }
                serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                        illusion.getX(), illusion.getY() + 1.2, illusion.getZ(), 16, 0.4, 0.8, 0.4, 0.04);
                illusion.discard();
            }
        }
        this.illusionIds.clear();
    }

    private void spawnConvergence(ServerLevel serverLevel, Vec3 from) {
        Vec3 to = this.position().add(0, this.getBbHeight() * 0.6, 0);
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int) (delta.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void broadcastNearby(Component message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(radius),
                    candidate -> candidate.distanceToSqr(this) <= radius * radius)) {
                player.sendSystemMessage(message);
            }
        }
    }

    public void onSigilActivated(ServerPlayer activator, BlockPos activatedSigil) {
        if (this.level().isClientSide || this.isBroken() || this.isIllusion()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BossNbtData.addParticipant(this.participants, activator.getUUID());
        List<BlockPos> realSigils = findNearbyRealSigils(serverLevel, activatedSigil);
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

    private List<BlockPos> findNearbyRealSigils(ServerLevel serverLevel, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : MirrorSigilBlockEntity.findCluster(serverLevel, center)) {
            if (MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
                result.add(pos);
            }
        }
        return result;
    }

    private void tickSigilGuidance(ServerLevel serverLevel) {
        BlockPos anchor = this.homePos != null ? this.homePos : this.blockPosition();
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        for (BlockPos sigilPos : findNearbyRealSigils(serverLevel, anchor)) {
            if (!serverLevel.hasChunkAt(sigilPos)) {
                continue;
            }
            Vec3 sigil = Vec3.atCenterOf(sigilPos).add(0.0D, 0.45D, 0.0D);
            BossFx.guidanceMarker(serverLevel, sigil, BossParticleTheme.MIRROR.ember());
            BossFx.guidanceFlow(serverLevel, core, sigil,
                    BossParticleTheme.MIRROR.ember(), BossParticleTheme.MIRROR.ember(), this.tickCount);
        }
    }

    private void triggerBroken(ServerLevel serverLevel) {
        this.triggerAnim("state_controller", "shatter");
        this.discardIllusions(serverLevel, true);
        this.setBroken(true);
        this.brokenTicksRemaining = ServerConfig.BOSS_BROKEN_DURATION_TICKS.get();
        this.brokenWindowHealthFloor = BossPhaseGate.nextFloor(this.getHealth(), this.getMaxHealth(),
                0.75F, 0.40F);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_SHATTER.get(), SoundSource.HOSTILE, 2.0F, 1.0F);

        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 1.6;
            for (int i = 0; i < 10 + ring * 6; i++) {
                double angle = Math.PI * 2 * i / (10 + ring * 6);
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.6,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.2, 0.05, -0.08);
            }
        }
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        BossFx.converge(serverLevel, core, 5.2D, 20, BossParticleTheme.MIRROR.body());
        BossFx.pillar(serverLevel, this.position().add(0.0D, 0.1D, 0.0D),
                6.5D, BossParticleTheme.MIRROR.body());
        BossFx.cinematicImpact(serverLevel, core, 32.0D, 0.82F, 10, 0xC9F7FF);
        broadcastNearby(Component.translatable("message.unknown_echoes.guardian.broken"));
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.slashCooldown > 0) this.slashCooldown--;
        if (this.swapCooldown > 0) this.swapCooldown--;
        if (this.barrageCooldown > 0) this.barrageCooldown--;
        if (this.novaCooldown > 0) this.novaCooldown--;
        if (this.spikeCooldown > 0) this.spikeCooldown--;
        this.syncVisualAttackState(serverLevel);

        if (this.pendingSlashTicks > 0) {

            Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                    this.getX() + facing.x * 1.2, this.getY() + 1.6, this.getZ() + facing.z * 1.2,
                    2, 0.15, 0.15, 0.15, 0.02);
            BossFx.chargeSpiral(serverLevel,
                    this.position().add(facing.scale(1.2)).add(0, 1.0, 0), this.tickCount, BossParticleTheme.MIRROR.body());
            if ((this.pendingSlashTicks & 1) == 0) {
                BossFx.converge(serverLevel,
                        this.position().add(facing.scale(1.5D)).add(0.0D, 1.35D, 0.0D),
                        1.8D, 8, BossParticleTheme.MIRROR.body());
            }
            if (--this.pendingSlashTicks == 0) {
                this.doSlashDamage(serverLevel);
            }
        }
        if (this.pendingBarrageWaves > 0 && --this.barrageWaveTimer <= 0) {
            this.doBarrageWave(serverLevel);
            this.pendingBarrageWaves--;
            this.barrageWaveTimer = BARRAGE_WAVE_DELAY;
        }
        if (this.pendingBarrageWaves > 0) {
            BossFx.chargeSpiral(serverLevel,
                    this.position().add(0.0D, this.getBbHeight() * 0.38D, 0.0D),
                    this.tickCount, BossParticleTheme.MIRROR.body());
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

        boolean shattering = this.getHealth() < this.getMaxHealth() * 0.3F;
        // 崩裂阶段的一次性宣告,与 shattering 同一条判据;换位频率与冷却仍由下方逻辑决定。
        if (shattering && !this.fractureAnnounced && !this.isIllusion()) {
            this.fractureAnnounced = true;
            this.triggerAnim("state_controller", "fracture");
        }
        double distSq = this.distanceToSqr(target);
        if (this.slashCooldown <= 0 && distSq <= SLASH_RANGE * SLASH_RANGE) {

            this.pendingSlashTicks = SLASH_HIT_DELAY;
            this.slashCooldown = 100;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.5F);
            for (MirrorGuardian illusion : aliveIllusions(serverLevel)) {
                illusion.triggerAnim("attack_controller", "attack");
                illusion.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.5F);
            }
        } else if (this.novaCooldown <= 0 && distSq <= NOVA_RANGE * NOVA_RANGE) {

            this.pendingNovaTicks = NOVA_CHARGE_TICKS;
            this.novaCooldown = 220;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "nova");
            this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.35F);
        } else if (this.barrageCooldown <= 0 && distSq <= BARRAGE_RANGE * BARRAGE_RANGE
                && distSq > NOVA_RANGE * NOVA_RANGE && this.hasLineOfSight(target)) {

            this.pendingBarrageWaves = 3;
            this.barrageWaveTimer = SLASH_HIT_DELAY;
            this.barrageCooldown = 200;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "barrage");
            this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.8F);
        } else if (this.spikeCooldown <= 0 && distSq <= SPIKE_RANGE * SPIKE_RANGE
                && distSq > SLASH_RANGE * SLASH_RANGE) {
            this.startSpikeSurge(target);
        } else if (this.swapCooldown <= 0 && !this.isBroken()) {
            List<MirrorGuardian> illusions = aliveIllusions(serverLevel);

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

    private void doSlashDamage(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SLASH_RANGE, 2.0, SLASH_RANGE),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > SLASH_RANGE * SLASH_RANGE
                    || (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.5)) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.setDeltaMovement(victim.getDeltaMovement()
                        .add(facing.x * 0.6, 0.25, facing.z * 0.6));
                victim.hurtMarked = true;
            }
        }

        for (int i = -3; i <= 3; i++) {
            double angle = Math.atan2(facing.z, facing.x) + i * 0.22;
            for (double d = 1.5; d <= SLASH_RANGE - 1; d += 1.2) {
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX() + Math.cos(angle) * d,
                        this.getY() + 1.4,
                        this.getZ() + Math.sin(angle) * d, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_ATTACK.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.8, 0), facing, SLASH_RANGE, BossParticleTheme.MIRROR.spark());
        BossFx.sparkSpray(serverLevel,
                this.position().add(facing.scale(0.9D)).add(0.0D, 1.35D, 0.0D),
                facing, SLASH_RANGE - 0.4D, BossParticleTheme.MIRROR.spark());
        BossFx.impactBurst(serverLevel, this.position().add(facing.scale(2.4)).add(0, 1.2, 0), BossParticleTheme.MIRROR.spark());
        BossFx.cinematicImpact(serverLevel, this.position().add(facing.scale(2.4)),
                22.0D, 0.38F, 5, 0xA9E8F2);
    }

    private void doMirrorSwap(ServerLevel serverLevel, MirrorGuardian illusion) {
        Vec3 myPos = this.position();
        Vec3 illusionPos = illusion.position();
        this.triggerAnim("attack_controller", "swap");
        illusion.triggerAnim("attack_controller", "swap");

        for (Vec3 p : List.of(myPos, illusionPos)) {
            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(), p.x, p.y + 1.5, p.z, 30, 0.4, 1.2, 0.4, 0.06);
            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(), p.x, p.y + 1.5, p.z, 10, 0.3, 1.0, 0.3, 0.03);
            BossFx.chargeSpiral(serverLevel, p.add(0, 0.3, 0), this.tickCount, BossParticleTheme.MIRROR.body());
        }
        this.teleportTo(illusionPos.x, illusionPos.y, illusionPos.z);
        illusion.teleportTo(myPos.x, myPos.y, myPos.z);
        BossFx.helix(serverLevel, myPos.add(0.0D, 1.2D, 0.0D),
                illusionPos.add(0.0D, 1.2D, 0.0D), 0.28D, BossParticleTheme.MIRROR.body());
        BossFx.cinematicImpact(serverLevel, illusionPos, 18.0D, 0.22F, 4, 0xB8EAF2);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_SWAP.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
    }

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
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > BARRAGE_RANGE * BARRAGE_RANGE
                    || (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.64)) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }
        }

        serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                this.getX(), this.getY() + 1.5, this.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        for (int i = -4; i <= 4; i++) {
            double angle = Math.atan2(facing.z, facing.x) + i * 0.14;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);

            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                    this.getX() + vx * 1.0, this.getY() + 1.5, this.getZ() + vz * 1.0,
                    0, vx * 0.8, 0.0, vz * 0.8, 1.0);
            for (double d = 2.0; d <= BARRAGE_RANGE; d += 2.0) {
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX() + vx * d, this.getY() + 1.5, this.getZ() + vz * d,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        float wavePitch = 1.0F + (3 - this.pendingBarrageWaves) * 0.15F;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.6F, wavePitch);
        if (this.pendingBarrageWaves == 1) {
            BossFx.cinematicImpact(serverLevel, this.position().add(facing.scale(4.0)),
                    26.0D, 0.46F, 6, 0x9FDCE9);
        }
    }

    private void tickNovaCharge(ServerLevel serverLevel) {
        double r = NOVA_BURST_RADIUS * this.pendingNovaTicks / (double) NOVA_CHARGE_TICKS + 0.5;
        double y = this.getY() + this.getBbHeight() * 0.55;
        BossFx.chargeSpiral(serverLevel, this.position().add(0, this.getBbHeight() * 0.25, 0),
                this.tickCount, BossParticleTheme.MIRROR.body());
        BossFx.lockSigil(serverLevel, this.position().add(0.0D, 0.1D, 0.0D),
                r, 1.0D - this.pendingNovaTicks / (double) NOVA_CHARGE_TICKS,
                this.tickCount, BossParticleTheme.MIRROR.ember());
        if ((this.pendingNovaTicks & 1) == 0) {
            BossFx.converge(serverLevel, this.position().add(0.0D, 0.5D, 0.0D),
                    Math.max(1.0D, r), 12, BossParticleTheme.MIRROR.body());
        }
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2 * i / 10 + this.tickCount * 0.25;
            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                    this.getX() + Math.cos(angle) * r, y, this.getZ() + Math.sin(angle) * r,
                    0, -Math.cos(angle) * 0.25, 0.0, -Math.sin(angle) * 0.25, 1.0);
        }
    }

    private void doNovaBurst(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(NOVA_BURST_RADIUS, 2.0, NOVA_BURST_RADIUS),
                e -> e != this && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > NOVA_BURST_RADIUS * NOVA_BURST_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 1.1, 0.45, dir.z * 1.1));
                    victim.hurtMarked = true;
                }
            }
        }

        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 0; ring < 3; ring++) {
            int points = 18 + ring * 6;
            double speed = 0.35 + ring * 0.15;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2 * i / points + ring * 0.2;
                serverLevel.sendParticles(
                        ring == 1 ? BossParticleTheme.MIRROR.spark() : BossParticleTheme.MIRROR.spark(),
                        this.getX(), this.getY() + 0.9 + ring * 0.4, this.getZ(),
                        0, Math.cos(angle) * speed, 0.05, Math.sin(angle) * speed, 1.0);
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_GUARDIAN_NOVA.get(), SoundSource.HOSTILE, 1.8F, 1.0F);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), NOVA_BURST_RADIUS, BossParticleTheme.MIRROR.ember());
        BossFx.pillar(serverLevel, this.position().add(0.0D, 0.1D, 0.0D),
                5.0D, BossParticleTheme.MIRROR.body());
        BossFx.impactBurst(serverLevel, this.position().add(0, this.getBbHeight() * 0.55, 0), BossParticleTheme.MIRROR.spark());
        BossFx.cinematicImpact(serverLevel, this.position(), 28.0D, 0.78F, 9, 0xBDEFF4);
    }

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

    /**
     * 抬手期锁死移动:斩击/新星/弹幕的判定锚在实时位置上,此时走动会让预警与真实落点分离。
     * 抬手起始的 getNavigation().stop() 只发一次,拦不住后续 Goal 重新 moveTo。
     */
    private boolean isMovementLockedByAttack() {
        return this.pendingSlashTicks > 0 || this.pendingNovaTicks > 0
                || this.pendingBarrageWaves > 0 || this.spikeTelegraphTicks > 0
                || this.spikeWaveDist >= 0.0D;
    }

    private void tickSpikeTelegraph(ServerLevel serverLevel) {
        for (double d = 1.5; d <= SPIKE_MAX_DIST; d += 1.0) {
            Vec3 p = this.spikeOrigin.add(this.spikeDir.scale(d));
            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                    p.x, this.spikeOrigin.y + 0.1, p.z, 1, 0.1, 0.02, 0.1, 0.01);
        }
        if ((this.spikeTelegraphTicks & 1) == 0) {
            BossFx.guidanceFlow(serverLevel, this.spikeOrigin.add(0.0D, 0.16D, 0.0D),
                    this.spikeOrigin.add(this.spikeDir.scale(SPIKE_MAX_DIST)).add(0.0D, 0.16D, 0.0D),
                    BossParticleTheme.MIRROR.ember(), BossParticleTheme.MIRROR.ember(), this.tickCount);
            BossFx.lockSigil(serverLevel,
                    this.spikeOrigin.add(this.spikeDir.scale(SPIKE_MAX_DIST)).add(0.0D, 0.1D, 0.0D),
                    1.35D, 1.0D - this.spikeTelegraphTicks / (double) SPIKE_TELEGRAPH_TICKS,
                    this.tickCount, BossParticleTheme.MIRROR.ember());
        }
    }

    private void tickSpikeWave(ServerLevel serverLevel) {
        if (!this.isAlive()) {
            this.spikeWaveDist = -1.0D;
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        for (int seg = 0; seg < 2 && this.spikeWaveDist >= 0; seg++) {
            Vec3 p = this.spikeOrigin.add(this.spikeDir.scale(this.spikeWaveDist));

            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                    p.x, this.spikeOrigin.y + 0.2, p.z, 10, 0.2, 0.05, 0.2, 0.3);
            serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                    p.x, this.spikeOrigin.y + 0.6, p.z, 6, 0.25, 0.5, 0.25, 0.1);
            BossFx.groundShockwave(serverLevel, new Vec3(p.x, this.spikeOrigin.y + 0.1, p.z),
                    1.8, BossParticleTheme.MIRROR.ember());
            BossFx.sparkSpray(serverLevel, p.add(0.0D, 0.35D, 0.0D),
                    this.spikeDir, 1.3D, BossParticleTheme.MIRROR.spark());
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                            p.x - 1.2, this.spikeOrigin.y - 1.0, p.z - 1.2,
                            p.x + 1.2, this.spikeOrigin.y + 2.5, p.z + 1.2),
                    e -> e != this && !e.isSpectator()
                            && !(e instanceof net.minecraft.world.entity.monster.Enemy)
                            && !this.spikeHitVictims.contains(e.getUUID()))) {
                if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.4, 0.0));
                    victim.hurtMarked = true;
                }
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
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.tickRechallengeBinding(serverLevel)) {
            return;
        }
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (this.isIllusion()) {
            if (this.level() instanceof ServerLevel serverLevel
                    && this.illusionOwnerUuid != null && this.tickCount % 20 == 0
                    && BossNbtData.isHorizontalAreaLoaded(serverLevel,
                    this.homePos != null ? this.homePos : this.blockPosition(), ARENA_RADIUS + 2)) {
                Entity owner = serverLevel.getEntity(this.illusionOwnerUuid);
                if (!(owner instanceof MirrorGuardian guardian)
                        || guardian.isIllusion() || !guardian.isAlive()) {
                    serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                            this.getX(), this.getY() + 1.2, this.getZ(),
                            16, 0.4, 0.8, 0.4, 0.04);
                    this.discard();
                    return;
                }
            }
            if (--this.illusionLifeTicks <= 0) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                            this.getX(), this.getY() + 1.2, this.getZ(), 16, 0.4, 0.8, 0.4, 0.04);
                }
                this.discard();
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, ARENA_RADIUS);
        }
        if (this.veiledMessageCooldown > 0) {
            this.veiledMessageCooldown--;
        }

        if (this.isAlive() && this.tickCount % 70 == 0) {
            serverLevel.playSound(null, this.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.9F, 0.45F);
        }

        if (!this.isBroken() && this.getTarget() != null && this.tickCount % 5 == 0) {
            this.tickSigilGuidance(serverLevel);
        }

        if (this.isAlive() && this.tickCount % 5 == 0) {
            if (this.isBroken()) {
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        4, 0.5, 0.8, 0.5, 0.05);
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
                        2, 0.4, 0.6, 0.4, 0.08);
            } else {
                double angle = this.tickCount * 0.3;
                double radius = this.getBbWidth() * 1.1;
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
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

            if (this.illusionRespawnCooldown > 0) {
                this.illusionRespawnCooldown -= 10;
            }
            if (aliveIllusions(serverLevel).isEmpty() && this.illusionRespawnCooldown <= 0) {
                this.summonIllusions(serverLevel);
                this.illusionRespawnCooldown = 160;
            }
        }

        if (this.tickCount % 20 == 0 && !this.isIllusion()) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
        }

        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(radius),
                    candidate -> !candidate.isSpectator()
                            && candidate.distanceToSqr(this) <= radius * radius)) {
                BossNbtData.addParticipant(this.participants, player.getUUID());
            }
            if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                this.updateDynamicScaling();
            }
        }
        this.tickSpecialAttacks(serverLevel);
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** 脱战回血:判据与配置口径由 OutOfCombatRegen 统一持有。 */
    private void tickOutOfCombatRegen() {
        this.outOfCombatTicks = OutOfCombatRegen.tick(this, this.outOfCombatTicks);
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
        if (this.isIllusion()) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            BossNbtData.addParticipant(this.participants, attacker.getUUID());
        }

        if (!this.level().isClientSide && amount > 0
                && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
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

    @Override
    public boolean isMechanicDamageGuarded() {
        return !this.isIllusion() && !this.isBroken();
    }

    @Override
    public void onMechanicDamageGuarded(DamageSource source) {
        if (this.veiledMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.guardian.veiled"), true);
            this.veiledMessageCooldown = 40;
        }
    }

    @Override
    public float phaseDamageFloor() {
        return !this.isIllusion() && this.isBroken() ? this.brokenWindowHealthFloor : 0.0F;
    }

    private void restoreMirrorVeil(ServerLevel serverLevel) {
        this.triggerAnim("state_controller", "restore");
        this.setBroken(false);
        this.brokenTicksRemaining = 0;
        this.brokenWindowHealthFloor = 0.0F;
        serverLevel.playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
        broadcastNearby(Component.translatable("message.unknown_echoes.guardian.veil_restored"));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (!this.registerRechallengePresence(serverLevel)) {
                this.discard();
                return;
            }
            // 幻象布场演出只给真身:假身再放一次会读成场上有两个本体。
            if (!this.isIllusion()) {
                this.triggerAnim("state_controller", "summon");
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (wasDead || !this.dead || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 分段崩解只属于真身;假身被命中即碎,有自己的碎镜表现(见下方分支)。
        if (!this.isIllusion()) {
            this.triggerAnim("state_controller", "collapse");
        }

        if (this.isIllusion()) {
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(3.0, 1.5, 3.0),
                    e -> e != this && !e.isSpectator()
                            && !(e instanceof net.minecraft.world.entity.monster.Enemy))) {
                if (victim.hurt(this.damageSources().mobAttack(this), 3.0F)) {
                    Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1.0E-4) {
                        Vec3 dir = away.normalize();
                        victim.setDeltaMovement(victim.getDeltaMovement().add(
                                dir.x * 0.8, 0.3, dir.z * 0.8));
                        victim.hurtMarked = true;
                    }
                }
            }
            serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                    this.getX(), this.getY() + 1.2, this.getZ(), 30, 0.8, 1.0, 0.8, 0.1);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_GUARDIAN_COLLAPSE.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
            return;
        }
        this.discardIllusions(serverLevel, true);

        serverLevel.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 1.5;
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14 + ring * 0.3;
                serverLevel.sendParticles(BossParticleTheme.MIRROR.spark(),
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ() + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, -0.1);
            }
        }

        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        boolean hasEligibleParticipant = false;
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius), player -> !player.isSpectator())) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= radius * radius) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player,
                        EchoPermission.MIRROR_GUARDIAN_ID);
                hasEligibleParticipant = true;
                if (firstKill && !EchoAbilityManager.markBossDefeated(
                        player, EchoPermission.MIRROR_GUARDIAN_ID)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"));
                }
            }
        }
        if (hasEligibleParticipant) {
            BossMaterialRewards.drop(this, new ItemStack(ModItems.TRUE_SIGHT_CORE.get()));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.MIRROR_GUARDIAN_CORE.get()));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.BROKEN_REFLECTION.get(),
                    2 + this.random.nextInt(3)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.MIRROR_LAKE_SHARD.get(),
                    3 + this.random.nextInt(4)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.ECHO_SHARD.get(),
                    2 + this.random.nextInt(4)));
            BossMaterialRewards.drop(this, new ItemStack(ModItems.ILLUSION_DUST.get(),
                    2 + this.random.nextInt(3)));
        }
        this.finishRechallenge(serverLevel);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.isIllusion() && reason.shouldDestroy()
                && this.level() instanceof ServerLevel serverLevel) {
            this.discardIllusions(serverLevel, false);
        }
        super.remove(reason);
    }

    @Override
    public cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind rechallengeKind() {
        return cn.kurt6.unknown_echoes.world.MainBossRechallengeData.BossKind.MIRROR_GUARDIAN;
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
        if (!this.isIllusion()) {
            this.homePos = arenaCenter.immutable();
            this.restrictTo(this.homePos, ARENA_RADIUS);
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

    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {

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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsIllusion", this.isIllusion());
        tag.putInt("IllusionLife", this.illusionLifeTicks);
        if (this.illusionOwnerUuid != null) {
            tag.putUUID("IllusionOwnerUuid", this.illusionOwnerUuid);
        }
        tag.putBoolean("Broken", this.isBroken());
        tag.putBoolean("FractureAnnounced", this.fractureAnnounced);
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
        this.saveRechallengeBinding(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsIllusion")) {
            float loadedHealth = this.getHealth();
            this.becomeIllusion();
            this.setHealth(BossNbtData.clampHealthFloor(loadedHealth, (float) ILLUSION_MAX_HEALTH));
            this.illusionLifeTicks = BossNbtData.clampTimer(
                    tag.getInt("IllusionLife"), ILLUSION_LIFETIME_TICKS);
            this.illusionOwnerUuid = tag.hasUUID("IllusionOwnerUuid")
                    ? tag.getUUID("IllusionOwnerUuid") : null;
        }
        this.setBroken(tag.getBoolean("Broken"));
        this.fractureAnnounced = tag.getBoolean("FractureAnnounced");
        this.brokenTicksRemaining = BossNbtData.clampTimer(tag.getInt("BrokenTicks"),
                ServerConfig.BOSS_BROKEN_DURATION_TICKS.get());
        this.brokenWindowHealthFloor = BossNbtData.clampHealthFloor(
                tag.getFloat("BrokenWindowHealthFloor"), this.getMaxHealth());
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        BossNbtData.loadUuids(tag, "IllusionIds", this.illusionIds, BossNbtData.MAX_SUMMONS);
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.homePos = BossNbtData.loadPositionNear(tag, "HomePos", this.blockPosition(),
                ARENA_RADIUS * 3, ARENA_RADIUS * 2);
        if (this.homePos != null) {
            this.restrictTo(this.homePos, ARENA_RADIUS);
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

            this.triggerAnim("attack_controller", this.random.nextBoolean() ? "attack" : "thrust");
            this.spawnSwingDust();
        }
        return hit;
    }

    /** 挥击镜尘:真假身共用的近战挥臂粒子,必须一致——辨识只靠音高与伤害。 */
    private void spawnSwingDust() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        serverLevel.sendParticles(BossParticleTheme.MIRROR.body(),
                this.getX() + facing.x * 1.4, this.getY() + 1.4, this.getZ() + facing.z * 1.4,
                10, 0.5, 0.4, 0.5, 0.06);
        BossFx.sweepArc(serverLevel, this.position().add(0, 0.7, 0), facing, SLASH_RANGE * 0.75, BossParticleTheme.MIRROR.spark());
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
                .triggerableAnim("barrage", BARRAGE_ANIM)
                .triggerableAnim("nova", NOVA_ANIM));
        // 状态动画走独立控制器:破防/恢复/崩裂/死亡不能打断正在播的攻击动作,否则判定仍在但视觉断了。
        controllers.add(new AnimationController<>(this, "state_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("summon", SUMMON_ANIM)
                .triggerableAnim("shatter", SHATTER_ANIM)
                .triggerableAnim("restore", RESTORE_ANIM)
                .triggerableAnim("fracture", FRACTURE_ANIM)
                .triggerableAnim("collapse", COLLAPSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void syncVisualAttackState(ServerLevel level) {
        int mode;
        int age;
        int duration;
        if (this.pendingSlashTicks > 0) {
            mode = 1;
            duration = SLASH_HIT_DELAY;
            age = duration - this.pendingSlashTicks;
        } else if (this.pendingNovaTicks > 0) {
            mode = 2;
            duration = NOVA_CHARGE_TICKS;
            age = duration - this.pendingNovaTicks;
        } else if (this.pendingBarrageWaves > 0) {
            mode = 3;
            duration = SLASH_HIT_DELAY + BARRAGE_WAVE_DELAY * 2;
            int remaining = this.barrageWaveTimer
                    + Math.max(0, this.pendingBarrageWaves - 1) * BARRAGE_WAVE_DELAY;
            age = duration - remaining;
        } else if (this.spikeTelegraphTicks > 0) {
            mode = 4;
            duration = SPIKE_TELEGRAPH_TICKS;
            age = duration - this.spikeTelegraphTicks;
        } else {
            mode = 0;
            age = 0;
            duration = 0;
        }
        this.setVisualAttackState(mode, Math.max(0, age), duration);
        for (MirrorGuardian illusion : this.aliveIllusions(level)) {
            illusion.setVisualAttackState(mode, Math.max(0, age), duration);
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
}
