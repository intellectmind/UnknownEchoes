package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 潮汐执灯者:镜湖湖底灯塔废墟区域守护者 / Mini Boss(V0.6C,设计文档 10.4.2)。
 *
 * 机制:持一盏潮汐灯,灯亮时受未破防双保险保护;两条破灯路线——
 * - 引导其"执灯冲刺"撞上亮灯浮标:灯火熄灭 7 秒(浮标 20 秒冷却,非一次性);
 * - 持潮汐回响交互水下符文座:4 秒致盲 + 短硬直(符文座 20 秒冷却)。
 * 灯亮时远程压制(灯光灼射直线光束,1.2 秒光路预警;锁定召唤水影,场上限 2);
 * 灯熄/致盲期间近战化,全额伤害输出窗口。
 * 低血(<40%)"潮涌":周期性水流速度脉冲推搡玩家(只做速度+粒子,不改水方块),
 * 灯光节奏加快,但每次破灯硬直 +1 秒作为补偿。
 * 结算(个人):首杀 = 潮痕拓片 + 水下符文片 + 镜湖碎片 + 潮汐研究点 +1 + 灯塔线索记录;
 * 重复 = 普通材料 + 低概率拓片。不发放任何基础能力(红线 #2)。
 */
public class TideLanternKeeper extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("tide_lantern_keeper");

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

    private static final int ARENA_RADIUS = 10;

    /** 灯熄时长(浮标破灯):7 秒。 */
    private static final int LANTERN_OUT_TICKS = 140;
    /** 符文致盲:4 秒。 */
    private static final int BLIND_TICKS = 80;
    /** 破灯硬直:2.5 秒;低血潮涌期 +1 秒补偿。 */
    private static final int STAGGER_TICKS = 50;
    private static final int STAGGER_BONUS_TICKS = 20;

    // 灯光灼射:1.2 秒光路预警 → 直线光束
    private static final int BEAM_TELEGRAPH_TICKS = 24;
    private static final double BEAM_RANGE = 14.0D;
    // 执灯冲刺:0.75 秒收束预警 → 直线冲刺(可被引导撞浮标)
    private static final int DASH_TELEGRAPH_TICKS = 15;
    private static final int DASH_MAX_TICKS = 14;
    // 潮汐摆尾:近身范围击退
    private static final double TAIL_RANGE = 2.8D;
    /** 水影同场上限。 */
    private static final int SHADE_CAP = 2;

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

    private int tailCooldown = 0;
    private int surgeCooldown = 0;

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

    // ---- 水栖体征 ----

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        // NeoForge 将 canBreatheUnderwater 改为 final(gotchas #6),水栖防溺水走 FluidType
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

    // ---- MiniBoss 契约 ----

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
        // 灯亮且未被致盲/硬直时受机制保护;破灯与致盲都是输出窗口
        return this.isLanternLit() && this.blindTicks <= 0 && this.staggerTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.tide_lantern_keeper.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantTideLanternKeeper(this, player, firstKill);
    }

    // ---- 机制状态 ----

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

    /** 水下符文座触发:4 秒致盲 + 短硬直(TideRuneSeatBlock 调用,服务端权威)。 */
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
                    ModSounds.TIDE_LANTERN_KEEPER_HURT.get(), SoundSource.HOSTILE, 1.4F, 0.6F);
        }
    }

    /** 浮标破灯:灯火熄灭 7 秒 + 硬直(低血潮涌期硬直 +1 秒补偿)。 */
    private void extinguishLantern(ServerLevel serverLevel) {
        this.lanternOutTicks = LANTERN_OUT_TICKS;
        this.setLanternLit(false);
        this.startStagger(STAGGER_TICKS + (this.isSurgePhase() ? STAGGER_BONUS_TICKS : 0));
        this.cancelWindups();
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_SHATTER.get(), SoundSource.HOSTILE, 1.6F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.WAX_OFF,
                this.getX(), this.getEyeY(), this.getZ(), 24, 0.6, 0.6, 0.6, 0.05);
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
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.blindTicks > 0) {
            this.blindTicks--;
        }
        if (this.lanternOutTicks > 0 && --this.lanternOutTicks == 0) {
            this.setLanternLit(true);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_AMBIENT.get(), SoundSource.HOSTILE, 1.4F, 1.3F);
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

        // 灯亮巡游时的灯雾演出
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
        // 以本体为中心的旋向脉冲:把水中玩家往切线方向推一把
        boolean clockwise = (this.tickCount / 160) % 2 == 0;
        for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(ARENA_RADIUS + 4, 6, ARENA_RADIUS + 4))) {
            if (!player.isInWater()) {
                continue;
            }
            Vec3 toPlayer = player.position().subtract(this.position()).multiply(1, 0, 1);
            if (toPlayer.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 dir = toPlayer.normalize();
            Vec3 tangent = clockwise ? new Vec3(-dir.z, 0, dir.x) : new Vec3(dir.z, 0, -dir.x);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    tangent.x * 0.7, 0.1, tangent.z * 0.7));
            player.hurtMarked = true;
            ((ServerPlayer) player).displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_lantern_keeper.surge"), true);
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    this.getX() + Math.cos(angle) * 6, this.getY() + 1.0,
                    this.getZ() + Math.sin(angle) * 6, 2, 0.3, 0.8, 0.3, 0.02);
        }
    }

    private void tickAttacks(ServerLevel serverLevel) {
        if (this.beamCooldown > 0) this.beamCooldown--;
        if (this.dashCooldown > 0) this.dashCooldown--;
        if (this.tailCooldown > 0) this.tailCooldown--;

        // 冲刺推进:直线突进,命中亮灯浮标 → 破灯;命中玩家 → 伤害 + 击退
        if (this.dashTicks > 0) {
            this.tickDash(serverLevel);
            return;
        }
        if (this.dashTelegraphTicks > 0) {
            this.tickDashTelegraph(serverLevel);
            return;
        }
        // 光束预警与释放
        if (this.beamTelegraphTicks > 0) {
            this.tickBeamTelegraph(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.blindTicks > 0) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        // 潮汐摆尾:近身把玩家推离符文座(灯亮灯熄都可用)
        if (this.tailCooldown <= 0 && distSq <= TAIL_RANGE * TAIL_RANGE) {
            this.doTailSweep(serverLevel);
            return;
        }
        if (!this.isLanternLit()) {
            return; // 灯熄期:只保留近战与摆尾
        }
        int beamInterval = this.isSurgePhase() ? 100 : 140;
        if (this.beamCooldown <= 0 && distSq <= BEAM_RANGE * BEAM_RANGE && this.hasLineOfSight(target)) {
            this.beamDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
            this.beamTelegraphTicks = BEAM_TELEGRAPH_TICKS;
            this.beamCooldown = beamInterval;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_AMBIENT.get(), SoundSource.HOSTILE, 1.4F, 1.6F);
        } else if (this.dashCooldown <= 0 && distSq >= 4.0 * 4.0) {
            // 锁定玩家当前位置直线冲刺——站在浮标后即可引导撞灯(机制核心)
            Vec3 lockPos = target.position().add(0, target.getBbHeight() * 0.4, 0);
            this.dashDir = lockPos.subtract(this.position()).normalize();
            this.dashTelegraphTicks = DASH_TELEGRAPH_TICKS;
            this.dashCooldown = 200;
            this.getNavigation().stop();
            this.triggerAnim("attack_controller", "attack");
        }
    }

    /** 光路预警:1.2 秒灯光收束铺出直线光路(10.4.2:水中有光路预警)。 */
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

    /** 灯光灼射:直线光束,命中伤害 + 发光标记(方便水影追击)。 */
    private void fireBeam(ServerLevel serverLevel) {
        if (!this.isAlive() || this.beamDir == null) {
            return;
        }
        Vec3 start = this.getEyePosition();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        for (double d = 0.5; d <= BEAM_RANGE; d += 0.5) {
            Vec3 point = start.add(this.beamDir.scale(d));
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.0);
            if (!serverLevel.getBlockState(BlockPos.containing(point)).isAir()
                    && serverLevel.getBlockState(BlockPos.containing(point)).isSolidRender(
                    serverLevel, BlockPos.containing(point))) {
                break;
            }
        }
        BossFx.sparkSpray(serverLevel, start, this.beamDir, BEAM_RANGE, ParticleTypes.GLOW);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BEAM_RANGE),
                e -> e != this && !(e instanceof Enemy) && e.isAlive());
        for (LivingEntity victim : victims) {
            Vec3 toVictim = victim.getEyePosition().subtract(start);
            double along = toVictim.dot(this.beamDir);
            if (along < 0 || along > BEAM_RANGE) {
                continue;
            }
            double offLine = toVictim.subtract(this.beamDir.scale(along)).length();
            if (offLine > 1.3) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 60, 0));
            this.summonShade(serverLevel, victim);
        }
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_BEAM.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        this.beamDir = null;
    }

    /** 冲刺预警:灯光收束到冲刺路径(10.4.2)。 */
    private void tickDashTelegraph(ServerLevel serverLevel) {
        if (this.dashDir == null) {
            this.dashTelegraphTicks = 0;
            return;
        }
        this.getNavigation().stop();
        for (double d = 1.0; d <= 6.0; d += 1.0) {
            Vec3 point = this.position().add(this.dashDir.scale(d)).add(0, 1.0, 0);
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        if (--this.dashTelegraphTicks == 0) {
            this.dashTicks = DASH_MAX_TICKS;
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.TIDE_LANTERN_KEEPER_DASH.get(), SoundSource.HOSTILE, 1.4F, 0.9F);
        }
    }

    /** 冲刺推进:每 tick 检查路径上的浮标与玩家。 */
    private void tickDash(ServerLevel serverLevel) {
        if (this.dashDir == null) {
            this.dashTicks = 0;
            return;
        }
        this.dashTicks--;
        this.setDeltaMovement(this.dashDir.scale(0.85));
        this.hurtMarked = true;
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getY() + 1.1, this.getZ(), 2, 0.25, 0.25, 0.25, 0.02);

        // 浮标判定:包围盒外扩 1 格内的亮灯浮标被撞灭 → 破灯
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
        // 玩家判定:冲刺撞人 = 伤害 + 击退
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(0.6),
                e -> e != this && !(e instanceof Enemy) && e.isAlive())) {
            victim.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.9, 0.3, dir.z * 0.9));
                victim.hurtMarked = true;
            }
        }
        // 撞墙提前结束
        if (this.horizontalCollision || this.verticalCollision) {
            this.dashTicks = 0;
        }
        if (this.dashTicks <= 0) {
            this.dashDir = null;
        }
    }

    /** 潮汐摆尾:近身范围击退,把玩家推离符文座。 */
    private void doTailSweep(ServerLevel serverLevel) {
        this.tailCooldown = 100;
        this.triggerAnim("attack_controller", "attack");
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(TAIL_RANGE, 1.5, TAIL_RANGE),
                e -> e != this && !(e instanceof Enemy) && e.isAlive())) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 1.1, 0.35, dir.z * 1.1));
                victim.hurtMarked = true;
            }
        }
        serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                this.getX(), this.getY() + 0.8, this.getZ(), 20, 1.2, 0.5, 1.2, 0.06);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.TIDE_LANTERN_KEEPER_AMBIENT.get(), SoundSource.HOSTILE, 1.4F, 0.7F);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), TAIL_RANGE, ParticleTypes.BUBBLE_POP);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.0, 0), ParticleTypes.SOUL_FIRE_FLAME);
    }

    /** 水影召唤:灯光锁定的追击添加物,同场上限 2,随灯/本体消亡。 */
    private void summonShade(ServerLevel serverLevel, LivingEntity target) {
        long alive = serverLevel.getEntitiesOfClass(TideWaterShade.class,
                this.getBoundingBox().inflate(ARENA_RADIUS + 8), TideWaterShade::isAlive).size();
        if (alive >= SHADE_CAP) {
            return;
        }
        TideWaterShade shade = ModEntities.TIDE_WATER_SHADE.get().create(serverLevel);
        if (shade == null) {
            return;
        }
        Vec3 spawn = this.position().add(
                (this.random.nextDouble() - 0.5) * 3, 0.5, (this.random.nextDouble() - 0.5) * 3);
        shade.moveTo(spawn.x, spawn.y, spawn.z, this.getYRot(), 0);
        shade.bindOwner(this);
        shade.setTarget(target);
        shade.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntity(shade);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                spawn.x, spawn.y + 0.8, spawn.z, 10, 0.3, 0.4, 0.3, 0.02);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            this.triggerAnim("attack_controller", "attack");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.GLOW);
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 灯火彻底熄灭:残灯余光散入湖水;水影随之消散
            for (TideWaterShade shade : serverLevel.getEntitiesOfClass(TideWaterShade.class,
                    this.getBoundingBox().inflate(ARENA_RADIUS + 12))) {
                shade.beginFade();
            }
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getEyeY(), this.getZ(), 40, 1.2, 1.0, 1.2, 0.05);
            serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    this.getX(), this.getY(), this.getZ(), 30, 1.0, 1.2, 1.0, 0.04);
            double radius = settlementRadius();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.tide_lantern_keeper.defeated"));
                }
            }
        }
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        // 出生在水中的灯塔废墟中央(MiniBossSpawnerBlock 上方),不要求露天
        return level.isUnobstructed(this);
    }

    /** 出生即在水中:水深判定走流体而不是 onGround。 */
    public boolean isInArenaWater() {
        return this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER);
    }

    // ---- 音效 ----

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

    // ---- NBT ----

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
        this.lanternOutTicks = tag.getInt("LanternOutTicks");
        this.blindTicks = tag.getInt("BlindTicks");
        this.staggerTicks = tag.getInt("StaggerTicks");
        this.setLanternLit(this.lanternOutTicks <= 0);
        this.entityData.set(DATA_STAGGERED, this.staggerTicks > 0);
    }

    // ---- GeckoLib ----

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
}
