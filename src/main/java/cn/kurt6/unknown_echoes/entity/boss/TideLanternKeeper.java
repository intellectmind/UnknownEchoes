package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock;
import cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

public class TideLanternKeeper extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("tide_lantern_keeper");

    @Override
    public boolean requiresWaterSpawn() {
        return true;
    }

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.tide_lantern_keeper.idle");
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.tide_lantern_keeper.swim");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.tide_lantern_keeper.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.tide_lantern_keeper.broken");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 潮汐灯亮(同步客户端:灯罩发光/光束表现)。 */
    private static final EntityDataAccessor<Boolean> DATA_LANTERN_LIT =
            SynchedEntityData.defineId(TideLanternKeeper.class, EntityDataSerializers.BOOLEAN);
    /** 硬直(破灯/致盲后的输出窗口表现)。 */
    private static final EntityDataAccessor<Boolean> DATA_STAGGERED =
            SynchedEntityData.defineId(TideLanternKeeper.class, EntityDataSerializers.BOOLEAN);

    private static final int ARENA_RADIUS = 13;
    private static final int MECHANIC_SCAN_RADIUS = 16;
    /** 灯塔战场的机关位于 Boss 下方约 14 至 17 格,扫描必须覆盖整座竖井。 */
    private static final int MECHANIC_SCAN_HEIGHT = 20;
    private static final int MECHANIC_ANCHOR_CAP = 8;
    private static final int GUIDANCE_ANCHOR_BATCH = 3;
    /** 竖井区块未加载时的重扫退避:引导每 5 tick 触发一次,不退避会持续重入扫描门槛。 */
    private static final int GUIDANCE_SCAN_RETRY_TICKS = 600;

    /** 灯熄时长(浮标破灯):7 秒。 */
    private static final int LANTERN_OUT_TICKS = 140;
    /** 符文致盲:4 秒。 */
    private static final int BLIND_TICKS = 80;
    /** 破灯硬直：基础 2.5 秒，低血潮涌期额外增加 1 秒。 */
    private static final int STAGGER_TICKS = 50;
    private static final int STAGGER_BONUS_TICKS = 20;

    private static final int BEAM_TELEGRAPH_TICKS = 24;
    private static final double BEAM_RANGE = 14.0D;

    private static final int DASH_TELEGRAPH_TICKS = 15;
    private static final int DASH_MAX_TICKS = 14;

    private static final double TAIL_RANGE = 2.8D;
    private static final int SHADE_CAP = 2;
    private boolean fadingShades = false;
    private static final double SHADE_OWNER_SEARCH_RADIUS =
            TideWaterShade.OWNER_TETHER_RANGE + 2.0D;

    private int lanternOutTicks = 0;
    private int blindTicks = 0;
    private int staggerTicks = 0;

    private int beamCooldown = 60;
    private int beamTelegraphTicks = 0;
    private Vec3 beamDir = null;

    private int dashCooldown = 120;
    private int dashTelegraphTicks = 0;
    private int dashTicks = 0;
    private Vec3 dashDir = null;
    private final Set<java.util.UUID> dashHitVictims = new HashSet<>();

    private int tailCooldown = 0;
    private int surgeCooldown = 0;
    private final List<BlockPos> guidanceBuoys = new ArrayList<>();
    private final List<BlockPos> guidanceRuneSeats = new ArrayList<>();
    private boolean guidanceAnchorsScanned = false;
    private long guidanceScanRetryAt = Long.MIN_VALUE;

    public TideLanternKeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        this.xpReward = 25;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.55F, 0.4F, true);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.ARMOR, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.6D, 40));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LANTERN_LIT, true);
        builder.define(DATA_STAGGERED, false);
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
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
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

        return this.isLanternLit() && this.blindTicks <= 0 && this.staggerTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.tide_lantern_keeper.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        if (!this.guidanceAnchorsScanned && level.getGameTime() >= this.guidanceScanRetryAt) {
            this.scanGuidanceAnchors(level);
        }
        Vec3 core = this.position().add(0, this.getBbHeight() * 0.55, 0);
        int buoyStart = guidanceBatchStart(this.guidanceBuoys.size());
        int guidedBuoys = 0;
        for (int i = 0; i < this.guidanceBuoys.size() && guidedBuoys < GUIDANCE_ANCHOR_BATCH; i++) {
            BlockPos pos = this.guidanceBuoys.get((buoyStart + i) % this.guidanceBuoys.size());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TideBuoyBlock && state.getValue(TideBuoyBlock.LIT)) {
                Vec3 buoy = Vec3.atCenterOf(pos).add(0, 0.45, 0);
                MiniBossFx.objectiveMarker(level, buoy, this.fxTheme(), level.getGameTime());
                MiniBossFx.objectiveLink(level, buoy, core, this.fxTheme(), level.getGameTime());
                guidedBuoys++;
            }
        }
        int runeStart = guidanceBatchStart(this.guidanceRuneSeats.size());
        int guidedRunes = 0;
        for (int i = 0; i < this.guidanceRuneSeats.size() && guidedRunes < GUIDANCE_ANCHOR_BATCH; i++) {
            BlockPos pos = this.guidanceRuneSeats.get((runeStart + i) % this.guidanceRuneSeats.size());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TideRuneSeatBlock && state.getValue(TideRuneSeatBlock.LIT)) {
                Vec3 rune = Vec3.atCenterOf(pos).add(0, 0.35, 0);
                MiniBossFx.objectiveMarker(level, rune, this.fxTheme(), level.getGameTime());
                MiniBossFx.objectiveLink(level, rune, core, this.fxTheme(), level.getGameTime());
                guidedRunes++;
            }
        }
    }

    private int guidanceBatchStart(int size) {
        if (size <= 0) {
            return 0;
        }
        return Math.floorMod((this.tickCount / 5) * GUIDANCE_ANCHOR_BATCH, size);
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantTideLanternKeeper(this, firstKill);
    }

    public boolean isLanternLit() {
        return this.entityData.get(DATA_LANTERN_LIT);
    }

    private void setLanternLit(boolean lit) {
        this.entityData.set(DATA_LANTERN_LIT, lit);
    }

    public boolean isStaggered() {
        return this.entityData.get(DATA_STAGGERED);
    }

    private boolean isSurgePhase() {
        return this.getHealth() < this.getMaxHealth() * 0.4F;
    }

    public void applyRuneBlind(Player trigger) {
        if (!this.isAlive() || this.level().isClientSide) {
            return;
        }
        this.blindTicks = BLIND_TICKS;
        this.startStagger(30);
        this.cancelWindups();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getEyeY(), this.getZ(), 16, 0.4, 0.3, 0.4, 0.03);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_HURT.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
            BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                    22.0D, 0.34F, 5, 0x54D9E8);
        }
    }

    private void extinguishLantern(ServerLevel serverLevel) {
        this.lanternOutTicks = LANTERN_OUT_TICKS;
        this.setLanternLit(false);
        this.startStagger(STAGGER_TICKS + (this.isSurgePhase() ? STAGGER_BONUS_TICKS : 0));
        this.cancelWindups();
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_SHATTER.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.WAX_OFF,
                this.getX(), this.getEyeY(), this.getZ(), 24, 0.6, 0.6, 0.6, 0.05);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                28.0D, 0.58F, 8, 0x54D9E8);
    }

    private void startStagger(int ticks) {
        this.staggerTicks = Math.max(this.staggerTicks, ticks);
        this.entityData.set(DATA_STAGGERED, true);
        this.getNavigation().stop();
    }

    private void cancelWindups() {
        this.beamTelegraphTicks = 0;
        this.beamDir = null;
        this.dashTelegraphTicks = 0;
        this.dashTicks = 0;
        this.dashDir = null;
        this.dashHitVictims.clear();
    }

    private void scanGuidanceAnchors(ServerLevel level) {
        BlockPos anchor = this.arenaAnchor();
        if (!isScanAreaLoaded(level, anchor, MECHANIC_SCAN_RADIUS)) {
            this.guidanceScanRetryAt = level.getGameTime() + GUIDANCE_SCAN_RETRY_TICKS;
            return;
        }
        this.guidanceBuoys.clear();
        this.guidanceRuneSeats.clear();
        for (BlockPos pos : BlockPos.betweenClosed(
                anchor.offset(-MECHANIC_SCAN_RADIUS, -MECHANIC_SCAN_HEIGHT, -MECHANIC_SCAN_RADIUS),
                anchor.offset(MECHANIC_SCAN_RADIUS, MECHANIC_SCAN_HEIGHT, MECHANIC_SCAN_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TideBuoyBlock && this.guidanceBuoys.size() < MECHANIC_ANCHOR_CAP) {
                this.guidanceBuoys.add(pos.immutable());
            } else if (state.getBlock() instanceof TideRuneSeatBlock
                    && this.guidanceRuneSeats.size() < MECHANIC_ANCHOR_CAP) {
                this.guidanceRuneSeats.add(pos.immutable());
            }
            if (this.guidanceBuoys.size() >= MECHANIC_ANCHOR_CAP
                    && this.guidanceRuneSeats.size() >= MECHANIC_ANCHOR_CAP) {
                break;
            }
        }
        this.guidanceAnchorsScanned = true;
    }

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, radius);
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

        if (this.blindTicks > 0) {
            this.blindTicks--;
        }
        if (this.lanternOutTicks > 0 && --this.lanternOutTicks == 0) {
            this.setLanternLit(true);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_RELIGHT.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getEyeY(), this.getZ(), 12, 0.5, 0.5, 0.5, 0.03);
        }
        if (this.staggerTicks > 0) {
            this.getNavigation().stop();
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                        this.getX(), this.getEyeY(), this.getZ(), 4, 0.4, 0.5, 0.4, 0.02);
            }
            if (--this.staggerTicks == 0) {
                this.entityData.set(DATA_STAGGERED, false);
            }
            return;
        }

        if (this.isLanternLit() && this.tickCount % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getEyeY() + 0.4, this.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
        }

        this.tickSurge(serverLevel);
        this.tickAttacks(serverLevel);
    }

    /** 低血潮涌:服务端速度脉冲表现"水流方向周期反转",不改水方块(技术边界)。 */
    private void tickSurge(ServerLevel serverLevel) {
        if (!this.isSurgePhase() || this.getTarget() == null) {
            return;
        }
        if (--this.surgeCooldown > 0) {
            return;
        }
        this.surgeCooldown = 160;

        boolean clockwise = (this.tickCount / 160) % 2 == 0;
        double surgeRadius = ARENA_RADIUS + 4.0D;
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(surgeRadius, 6, surgeRadius),
                p -> !p.isSpectator())) {
            if (!player.isInWater()) {
                continue;
            }
            Vec3 toPlayer = player.position().subtract(this.position()).multiply(1, 0, 1);
            if (toPlayer.lengthSqr() < 1.0E-4
                    || toPlayer.lengthSqr() > surgeRadius * surgeRadius) {
                continue;
            }
            Vec3 dir = toPlayer.normalize();
            Vec3 tangent = clockwise ? new Vec3(-dir.z, 0, dir.x) : new Vec3(dir.z, 0, -dir.x);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    tangent.x * 0.7, 0.1, tangent.z * 0.7));
            player.hurtMarked = true;
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            Vec3 point = new Vec3(this.getX() + Math.cos(angle) * 6,
                    this.getY() + 1.0, this.getZ() + Math.sin(angle) * 6);
            serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, point, ParticleTypes.SPLASH),
                    point.x, point.y, point.z, 2, 0.3, 0.8, 0.3, 0.02);
        }
    }

    private void tickAttacks(ServerLevel serverLevel) {
        if (this.beamCooldown > 0) this.beamCooldown--;
        if (this.dashCooldown > 0) this.dashCooldown--;
        if (this.tailCooldown > 0) this.tailCooldown--;

        if (this.dashTicks > 0) {
            this.tickDash(serverLevel);
            return;
        }
        if (this.dashTelegraphTicks > 0) {
            this.tickDashTelegraph(serverLevel);
            return;
        }

        if (this.beamTelegraphTicks > 0) {
            this.tickBeamTelegraph(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.blindTicks > 0) {
            return;
        }

        double distSq = this.distanceToSqr(target);

        if (this.tailCooldown <= 0 && distSq <= TAIL_RANGE * TAIL_RANGE) {
            this.doTailSweep(serverLevel);
            return;
        }
        if (!this.isLanternLit()) {
            return;
        }
        int beamInterval = this.isSurgePhase() ? 100 : 140;
        if (this.beamCooldown <= 0 && distSq <= BEAM_RANGE * BEAM_RANGE && this.hasLineOfSight(target)) {
            this.beamDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
            this.beamTelegraphTicks = BEAM_TELEGRAPH_TICKS;
            this.beamCooldown = beamInterval;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_BEAM_WARN.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        } else if (this.dashCooldown <= 0 && distSq >= 4.0 * 4.0) {

            Vec3 lockPos = target.position().add(0, target.getBbHeight() * 0.4, 0);
            this.dashDir = lockPos.subtract(this.position()).normalize();
            this.dashTelegraphTicks = DASH_TELEGRAPH_TICKS;
            this.dashCooldown = 200;
            this.dashHitVictims.clear();
            this.playSound(ModSounds.TIDE_LANTERN_KEEPER_ATTACK_DASH_WARN.get(), 1.2F, 1.0F);
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
        }
    }

    private void tickBeamTelegraph(ServerLevel serverLevel) {
        if (this.beamDir == null) {
            this.beamTelegraphTicks = 0;
            return;
        }
        Vec3 start = this.getEyePosition();
        BossFx.chargeSpiral(serverLevel, start, this.tickCount, ParticleTypes.GLOW);
        for (double d = 1.0; d <= BEAM_RANGE; d += 1.5) {
            Vec3 point = start.add(this.beamDir.scale(d));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (--this.beamTelegraphTicks == 0) {
            this.fireBeam(serverLevel);
        }
    }

    private void fireBeam(ServerLevel serverLevel) {
        if (!this.isAlive() || this.beamDir == null) {
            return;
        }
        Vec3 start = this.getEyePosition();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        double beamReach = BEAM_RANGE;
        for (double d = 0.5; d <= BEAM_RANGE; d += 0.5) {
            Vec3 point = start.add(this.beamDir.scale(d));
            BlockPos pointPos = BlockPos.containing(point);
            if (!serverLevel.hasChunkAt(pointPos)) {
                beamReach = Math.max(0.0D, d - 0.5D);
                break;
            }
            BlockState pointState = serverLevel.getBlockState(pointPos);
            if (!pointState.isAir() && pointState.isSolidRender(serverLevel, pointPos)) {
                beamReach = Math.max(0.0D, d - 0.5D);
                break;
            }
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.0);
        }
        BossFx.sparkSpray(serverLevel, start, this.beamDir, beamReach, ParticleTypes.GLOW);
        Vec3 end = start.add(this.beamDir.scale(beamReach));
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(1.3D),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive());
        for (LivingEntity victim : victims) {
            Vec3 toVictim = victim.getEyePosition().subtract(start);
            double along = toVictim.dot(this.beamDir);
            if (along < 0 || along > beamReach) {
                continue;
            }
            double offLine = toVictim.subtract(this.beamDir.scale(along)).length();
            if (offLine > 1.3) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 60, 0));
                this.summonShade(serverLevel, victim);
            }
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_BEAM.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        BossFx.cinematicImpact(serverLevel, end, 24.0D, 0.32F, 4, 0x7FEFFF);
        this.beamDir = null;
    }

    private void tickDashTelegraph(ServerLevel serverLevel) {
        if (this.dashDir == null) {
            this.dashTelegraphTicks = 0;
            return;
        }
        this.getNavigation().stop();
        for (double d = 1.0; d <= 6.0; d += 1.0) {
            Vec3 point = this.position().add(this.dashDir.scale(d)).add(0, 1.0, 0);
            serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, point, ParticleTypes.SPLASH),
                    point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        if (--this.dashTelegraphTicks == 0) {
            this.dashTicks = DASH_MAX_TICKS;
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_DASH.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        }
    }

    private void tickDash(ServerLevel serverLevel) {
        if (this.dashDir == null) {
            this.dashTicks = 0;
            this.dashHitVictims.clear();
            return;
        }
        this.dashTicks--;
        this.setDeltaMovement(this.dashDir.scale(0.85));
        this.hurtMarked = true;
        serverLevel.sendParticles(
                BossFx.waterBubbleOr(serverLevel, this.position(), ParticleTypes.SPLASH),
                this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getY() + 1.1, this.getZ(), 2, 0.25, 0.25, 0.25, 0.02);

        BlockPos min = BlockPos.containing(this.getBoundingBox().inflate(1.0).getMinPosition());
        BlockPos max = BlockPos.containing(this.getBoundingBox().inflate(1.0).getMaxPosition());
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof TideBuoyBlock
                    && TideBuoyBlock.tryExtinguish(serverLevel, pos.immutable(), state)) {
                this.dashTicks = 0;
                this.dashDir = null;
                this.extinguishLantern(serverLevel);
                return;
            }
        }

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(0.6),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive()
                        && !this.dashHitVictims.contains(e.getUUID()))) {
            boolean hurt = victim.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            this.dashHitVictims.add(victim.getUUID());
            if (hurt) {
                Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 0.9, 0.3, dir.z * 0.9));
                    victim.hurtMarked = true;
                }
                BossFx.cinematicImpact(serverLevel,
                        victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D),
                        18.0D, 0.2F, 3, 0x54D9E8);
            }
        }

        if (this.horizontalCollision || this.verticalCollision) {
            this.dashTicks = 0;
        }
        if (this.dashTicks <= 0) {
            this.dashDir = null;
            this.dashHitVictims.clear();
        }
    }

    private void doTailSweep(ServerLevel serverLevel) {
        this.tailCooldown = 100;
        this.triggerAnim("attack_controller", "attack");
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(TAIL_RANGE, 1.5, TAIL_RANGE),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive())) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > TAIL_RANGE * TAIL_RANGE) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 1.1, 0.35, dir.z * 1.1));
                    victim.hurtMarked = true;
                }
            }
        }
        serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                this.getX(), this.getY() + 0.8, this.getZ(), 20, 1.2, 0.5, 1.2, 0.06);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_TAIL_SWEEP.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), TAIL_RANGE, ParticleTypes.BUBBLE_POP);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.0, 0), ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 0.8, 0),
                24.0D, 0.38F, 5, 0x54D9E8);
    }

    private void summonShade(ServerLevel serverLevel, LivingEntity target) {
        long alive = serverLevel.getEntitiesOfClass(TideWaterShade.class,
                this.getBoundingBox().inflate(SHADE_OWNER_SEARCH_RADIUS),
                shade -> shade.isAlive() && shade.isOwnedBy(this)).size();
        if (alive >= SHADE_CAP) {
            return;
        }
        TideWaterShade shade = ModEntities.TIDE_WATER_SHADE.get().create(serverLevel);
        if (shade == null) {
            return;
        }
        Vec3 spawn = this.findShadeSpawn(serverLevel, shade);
        if (spawn == null) {
            shade.discard();
            return;
        }
        shade.moveTo(spawn.x, spawn.y, spawn.z, this.getYRot(), 0);
        shade.bindOwner(this);
        shade.setTarget(target);
        shade.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        if (!serverLevel.addFreshEntity(shade)) {
            shade.discard();
            return;
        }
        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                spawn.x, spawn.y + 0.8, spawn.z, 10, 0.3, 0.4, 0.3, 0.02);
    }

    private Vec3 findShadeSpawn(ServerLevel level, TideWaterShade shade) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + this.random.nextDouble() * 2.5D;
            Vec3 destination = new Vec3(this.getX() + Math.cos(angle) * distance,
                    this.getY() + (this.random.nextDouble() - 0.5D) * 2.0D,
                    this.getZ() + Math.sin(angle) * distance);
            AABB moved = shade.getBoundingBox().move(destination.subtract(shade.position()));
            BlockPos min = BlockPos.containing(moved.minX, moved.minY, moved.minZ);
            BlockPos max = BlockPos.containing(moved.maxX - 1.0E-7D,
                    moved.maxY - 1.0E-7D, moved.maxZ - 1.0E-7D);
            if (!level.isInWorldBounds(min) || !level.isInWorldBounds(max)
                    || !level.getWorldBorder().isWithinBounds(moved)) {
                continue;
            }
            boolean loaded = true;
            for (int x : new int[]{min.getX(), max.getX()}) {
                for (int z : new int[]{min.getZ(), max.getZ()}) {
                    if (!level.hasChunkAt(new BlockPos(x, min.getY(), z))) {
                        loaded = false;
                    }
                }
            }
            if (loaded && level.noCollision(shade, moved)) {
                return destination;
            }
        }
        return null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            this.playSound(ModSounds.TIDE_LANTERN_KEEPER_ATTACK_MELEE.get(), 1.1F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.GLOW);
            }
        }
        return hit;
    }

    @Override
    protected void onDeathCommitted(ServerLevel serverLevel, Vec3 deathPos) {
        this.fadeOwnedShades(serverLevel);
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) return;
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getEyeY(), this.getZ(), 40, 1.2, 1.0, 1.2, 0.05);
        if (isCurrentDeathPresentation(serverLevel, deathPos)) {
            serverLevel.sendParticles(BossFx.waterBubbleOr(serverLevel, deathPos, ParticleTypes.SPLASH),
                    deathPos.x, deathPos.y, deathPos.z, 30, 1.0, 1.2, 1.0, 0.04);
        }
    }

    private void fadeOwnedShades(ServerLevel serverLevel) {
        if (this.fadingShades) return;
        this.fadingShades = true;
        try {
            AABB bounds = this.deathCleanupBounds().inflate(SHADE_OWNER_SEARCH_RADIUS);
            for (TideWaterShade shade : serverLevel.getEntitiesOfClass(TideWaterShade.class,
                    bounds, shade -> shade.isOwnedBy(this))) {
                runDeathStep("shade fade", () -> {
                    if (shade.level() == serverLevel && shade.isAlive() && !shade.isRemoved()
                            && shade.isOwnedBy(this) && shade.getBoundingBox().intersects(bounds)) {
                        shade.beginFade();
                    }
                });
            }
        } finally {
            this.fadingShades = false;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        try {
            ServerLevel cleanupLevel = this.deathCleanupLevel();
            if (reason.shouldDestroy() && cleanupLevel != null) {
                runDeathStep("shade fade on removal", () -> this.fadeOwnedShades(cleanupLevel));
            }
        } finally {
            super.remove(reason);
        }
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {

        return level.isUnobstructed(this);
    }

    /** 出生即在水中:水深判定走流体而不是 onGround。 */
    public boolean isInArenaWater() {
        return this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TIDE_LANTERN_KEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TIDE_LANTERN_KEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TIDE_LANTERN_KEEPER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LanternOutTicks", this.lanternOutTicks);
        tag.putInt("BlindTicks", this.blindTicks);
        tag.putInt("StaggerTicks", this.staggerTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lanternOutTicks = BossNbtData.clampTimer(tag.getInt("LanternOutTicks"), LANTERN_OUT_TICKS);
        this.blindTicks = BossNbtData.clampTimer(tag.getInt("BlindTicks"), BLIND_TICKS);
        this.staggerTicks = BossNbtData.clampTimer(
                tag.getInt("StaggerTicks"), STAGGER_TICKS + STAGGER_BONUS_TICKS);
        this.setLanternLit(this.lanternOutTicks <= 0);
        this.entityData.set(DATA_STAGGERED, this.staggerTicks > 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isStaggered()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? SWIM_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
    @Override
    public int musicStage() {
        return this.isStaggered() ? 1 : this.isSurgePhase() ? 2 : 0;
    }
}
