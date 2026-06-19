package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 风暴编织者:风之回响强化守护者 / Mini Boss(天空观测站四角风蚀柱区,设计文档 10.5)。
 *
 * 阶段:
 * - 风流追击:长期悬空绕场,保持与玩家距离;空中受机制保护(未破防双保险)。
 * - 坠风破防:周期性标记一处落点俯冲穿刺,落地后短暂停留——唯一完整输出窗口。
 * - 乱流收束:低血量(30%)后全技能冷却 x0.7,旋风更频繁,但俯冲窗口不变短。
 * 攻击:风刃连射(预警线 + 三段风刃浪涌)/ 旋风束缚(场内短时旋风区,推开不困死)/
 *       俯冲穿刺(标记落点 → 风压轨迹 → 俯冲 AoE → 落地硬直)。
 * 结算(个人):首杀 = 罗盘部件 + 风之强化核心 + 风之研究拓片 + 回响粉尘;
 *             重复 = 回响粉尘 + 概率拓片(普通奖励与研究进度,10.4.1)。
 */
public class StormWeaver extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("storm_weaver");

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.fly");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BLADES_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.blades", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CYCLONE_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.cyclone", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DIVE_ANIM =
            RawAnimation.begin().then("animation.storm_weaver.dive", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BROKEN_ANIM =
            RawAnimation.begin().thenLoop("animation.storm_weaver.broken");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_BROKEN =
            SynchedEntityData.defineId(StormWeaver.class, EntityDataSerializers.BOOLEAN);

    private static final int ARENA_RADIUS = 14;
    /** 悬空高度区间(相对场地锚点)。 */
    private static final int HOVER_MIN = 5;
    private static final int HOVER_MAX = 9;

    // 风刃连射:预警 0.8s → 三段风刃沿锁定方向推进
    private static final int BLADE_TELEGRAPH_TICKS = 16;
    private static final double BLADE_MAX_DIST = 16.0D;
    // 俯冲穿刺:风压轨迹预警 1.2s → 俯冲 → 落地硬直(输出窗口)
    private static final int DIVE_TELEGRAPH_TICKS = 24;
    private static final int BROKEN_WINDOW_TICKS = 120;
    // 旋风束缚:旋风区持续 5s
    private static final int CYCLONE_LIFETIME = 100;
    private static final double CYCLONE_RADIUS = 2.2D;

    private int bladeCooldown = 60;
    private int cycloneCooldown = 140;
    private int diveCooldown = 200;

    private int bladeTelegraphTicks = 0;
    private Vec3 bladeDir = Vec3.ZERO;
    private Vec3 bladeOrigin = Vec3.ZERO;
    /** 风刃浪涌推进距离,-1 = 未激活。 */
    private double bladeWaveDist = -1.0D;
    private int bladeWavesLeft = 0;
    private final Set<UUID> bladeHitVictims = new HashSet<>();

    private int diveTelegraphTicks = 0;
    private Vec3 diveMark = null;
    private boolean diving = false;
    private int brokenTicksRemaining = 0;

    /** 在场旋风:位置 + 剩余 tick。 */
    private final List<Cyclone> cyclones = new ArrayList<>();

    private Vec3 hoverWaypoint = null;
    private int waypointTicks = 0;

    private static class Cyclone {
        final Vec3 pos;
        int ticks;

        Cyclone(Vec3 pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    public StormWeaver(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        this.xpReward = 30;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
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
        // 风流追击:空中受风护;只有俯冲落地的硬直窗口可全额输出
        return !this.isBroken();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.storm_weaver.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantStormWeaver(this, player, firstKill);
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.isBroken()) {
            this.tickBrokenWindow(serverLevel);
            return;
        }

        this.tickHover(serverLevel);
        this.tickCyclones(serverLevel);
        this.tickSpecialAttacks(serverLevel);

        // 翼下风纹:盘旋时周期性风屑(5 tick 节流)
        if (this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() - 0.3, this.getZ(), 2, 0.8, 0.1, 0.8, 0.01);
        }
    }

    /** 风流追击:围绕场地锚点高空盘旋,周期性换悬空航点,刻意与目标拉开距离。 */
    private void tickHover(ServerLevel serverLevel) {
        if (--this.waypointTicks > 0 && this.hoverWaypoint != null
                && this.distanceToSqr(this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z) > 4.0) {
            this.getMoveControl().setWantedPosition(
                    this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z, 1.0);
            return;
        }
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        LivingEntity target = this.getTarget();
        // 航点偏向目标对侧:保持空中距离,逼玩家用风之机动接近
        double baseAngle = target != null
                ? Math.atan2(anchor.z - target.getZ(), anchor.x - target.getX())
                : this.random.nextDouble() * Math.PI * 2;
        double angle = baseAngle + (this.random.nextDouble() - 0.5) * Math.PI * 0.8;
        double dist = 4.0 + this.random.nextDouble() * (ARENA_RADIUS - 6);
        double height = HOVER_MIN + this.random.nextDouble() * (HOVER_MAX - HOVER_MIN);
        this.hoverWaypoint = new Vec3(
                anchor.x + Math.cos(angle) * dist,
                anchor.y + height,
                anchor.z + Math.sin(angle) * dist);
        this.waypointTicks = 40 + this.random.nextInt(30);
        this.getMoveControl().setWantedPosition(
                this.hoverWaypoint.x, this.hoverWaypoint.y, this.hoverWaypoint.z, 1.0);
    }

    /** 乱流收束(低血量):冷却整体提速。 */
    private boolean isTurbulentPhase() {
        return this.getHealth() < this.getMaxHealth() * 0.3F;
    }

    private int scaleCooldown(int base) {
        return this.isTurbulentPhase() ? (int) (base * 0.7F) : base;
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (this.bladeCooldown > 0) this.bladeCooldown--;
        if (this.cycloneCooldown > 0) this.cycloneCooldown--;
        if (this.diveCooldown > 0) this.diveCooldown--;

        // 风刃预警 → 浪涌推进
        if (this.bladeTelegraphTicks > 0) {
            this.tickBladeTelegraph(serverLevel);
            if (--this.bladeTelegraphTicks == 0) {
                this.bladeWaveDist = 1.0D;
                this.bladeHitVictims.clear();
            }
            return;
        }
        if (this.bladeWaveDist >= 0) {
            this.tickBladeWave(serverLevel);
            return;
        }
        // 俯冲:预警 → 俯冲位移(在 tickDive 处理)
        if (this.diveTelegraphTicks > 0) {
            this.tickDiveTelegraph(serverLevel);
            if (--this.diveTelegraphTicks == 0) {
                this.diving = true;
                this.playSound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.7F);
            }
            return;
        }
        if (this.diving) {
            this.tickDive(serverLevel);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.diveCooldown <= 0) {
            this.startDive(serverLevel, target);
        } else if (this.bladeCooldown <= 0 && distSq <= BLADE_MAX_DIST * BLADE_MAX_DIST
                && this.hasLineOfSight(target)) {
            this.startBladeVolley(target);
        } else if (this.cycloneCooldown <= 0) {
            this.castCyclones(serverLevel, target);
        }
    }

    // ---- 风刃连射 ----

    private void startBladeVolley(LivingEntity target) {
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position()).normalize();
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        this.bladeDir = dir;
        this.bladeOrigin = this.position().add(0, this.getBbHeight() * 0.5, 0);
        this.bladeTelegraphTicks = BLADE_TELEGRAPH_TICKS;
        this.bladeWavesLeft = 3;
        this.bladeCooldown = scaleCooldown(110);
        this.triggerAnim("attack_controller", "blades");
        this.playSound(SoundEvents.BREEZE_CHARGE, 1.6F, 0.8F);
    }

    /** 风刃预警:沿攻击路径铺一条风压线。 */
    private void tickBladeTelegraph(ServerLevel serverLevel) {
        BossFx.chargeSpiral(serverLevel, this.bladeOrigin, this.tickCount, ParticleTypes.ELECTRIC_SPARK);
        for (double d = 1.5; d <= BLADE_MAX_DIST; d += 1.5) {
            Vec3 p = this.bladeOrigin.add(this.bladeDir.scale(d));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /** 风刃浪涌:沿线推进(每 tick 两段),单波每人只命中一次,共三波。 */
    private void tickBladeWave(ServerLevel serverLevel) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (int seg = 0; seg < 2 && this.bladeWaveDist >= 0; seg++) {
            Vec3 p = this.bladeOrigin.add(this.bladeDir.scale(this.bladeWaveDist));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.05);
            BossFx.sparkSpray(serverLevel, p, this.bladeDir, 2.2, ParticleTypes.ELECTRIC_SPARK);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(p.x - 1.2, p.y - 1.2, p.z - 1.2, p.x + 1.2, p.y + 1.2, p.z + 1.2),
                    e -> e != this && !(e instanceof Enemy)
                            && !this.bladeHitVictims.contains(e.getUUID()))) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
                this.bladeHitVictims.add(victim.getUUID());
            }
            this.bladeWaveDist += 1.6D;
            if (this.bladeWaveDist > BLADE_MAX_DIST) {
                // 一波结束:还有剩余波次则向目标重新锁向再射
                if (--this.bladeWavesLeft > 0) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive()) {
                        this.bladeDir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                                .subtract(this.position()).normalize();
                        this.bladeOrigin = this.position().add(0, this.getBbHeight() * 0.5, 0);
                    }
                    this.bladeWaveDist = 1.0D;
                    this.bladeHitVictims.clear();
                    this.playSound(SoundEvents.BREEZE_SHOOT, 1.4F, 1.0F + (3 - this.bladeWavesLeft) * 0.15F);
                } else {
                    this.bladeWaveDist = -1.0D;
                }
            }
        }
    }

    // ---- 旋风束缚 ----

    /** 在目标与本体之间放 2-3 个短时旋风区:阻路推开,不造成围困。 */
    private void castCyclones(ServerLevel serverLevel, LivingEntity target) {
        int count = this.isTurbulentPhase() ? 3 : 2;
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        for (int i = 0; i < count; i++) {
            // 落点:目标周围 2~5 格的地表附近,但不直接压在目标头上(留可读路线)
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 2.0 + this.random.nextDouble() * 3.0;
            Vec3 p = target.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            // 锁场地:超出场地半径的旋风丢弃
            if (p.subtract(anchor).horizontalDistanceSqr() > ARENA_RADIUS * ARENA_RADIUS) {
                continue;
            }
            this.cyclones.add(new Cyclone(p, CYCLONE_LIFETIME));
            BossFx.groundShockwave(serverLevel, p.add(0, 0.1, 0), CYCLONE_RADIUS, ParticleTypes.CLOUD);
        }
        this.cycloneCooldown = scaleCooldown(200);
        this.triggerAnim("attack_controller", "cyclone");
        this.playSound(SoundEvents.BREEZE_IDLE_GROUND, 1.8F, 0.6F);
    }

    private void tickCyclones(ServerLevel serverLevel) {
        if (this.cyclones.isEmpty()) {
            return;
        }
        this.cyclones.removeIf(cyclone -> --cyclone.ticks <= 0);
        for (Cyclone cyclone : this.cyclones) {
            // 螺旋风柱演出
            double spin = (CYCLONE_LIFETIME - cyclone.ticks) * 0.5;
            for (int h = 0; h < 3; h++) {
                double r = 0.6 + h * 0.5;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        cyclone.pos.x + Math.cos(spin + h * 2.1) * r,
                        cyclone.pos.y + 0.3 + h * 0.9,
                        cyclone.pos.z + Math.sin(spin + h * 2.1) * r,
                        1, 0.05, 0.05, 0.05, 0.0);
            }
            if (cyclone.ticks % 4 == 0) {
                BossFx.chargeSpiral(serverLevel, cyclone.pos.add(0, 0.1, 0), this.tickCount, ParticleTypes.CLOUD);
            }
            // 推离:站进旋风会被推开 + 短暂缓速,阻路但不困死
            if (cyclone.ticks % 5 == 0) {
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        new AABB(cyclone.pos.x - CYCLONE_RADIUS, cyclone.pos.y - 0.5, cyclone.pos.z - CYCLONE_RADIUS,
                                cyclone.pos.x + CYCLONE_RADIUS, cyclone.pos.y + 3.0, cyclone.pos.z + CYCLONE_RADIUS),
                        e -> e != this && !(e instanceof Enemy))) {
                    Vec3 away = victim.position().subtract(cyclone.pos).multiply(1, 0, 1);
                    Vec3 dir = away.lengthSqr() > 1.0E-4 ? away.normalize()
                            : new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.7, 0.25, dir.z * 0.7));
                    victim.hurtMarked = true;
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        }
    }

    // ---- 俯冲穿刺 → 坠风破防 ----

    private void startDive(ServerLevel serverLevel, LivingEntity target) {
        // 标记落点:目标当前所站位置(俯冲一定落到标记点,落地即输出窗口)
        this.diveMark = target.position();
        this.diveTelegraphTicks = DIVE_TELEGRAPH_TICKS;
        this.diveCooldown = scaleCooldown(280);
        this.triggerAnim("attack_controller", "dive");
        this.playSound(SoundEvents.BREEZE_INHALE, 2.0F, 0.5F);
    }

    /** 俯冲预警:标记点风压圈 + 本体到落点的长条风压轨迹(10.5 粒子演出)。 */
    private void tickDiveTelegraph(ServerLevel serverLevel) {
        if (this.diveMark == null) {
            this.diveTelegraphTicks = 0;
            return;
        }
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + this.tickCount * 0.2;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.diveMark.x + Math.cos(angle) * 1.8,
                    this.diveMark.y + 0.1,
                    this.diveMark.z + Math.sin(angle) * 1.8, 1, 0.05, 0.02, 0.05, 0.0);
        }
        if (this.tickCount % 4 == 0) {
            BossFx.ring(serverLevel, this.diveMark, 2.4, 20, 0.12, ParticleTypes.CLOUD);
        }
        Vec3 from = this.position();
        Vec3 delta = this.diveMark.subtract(from);
        int steps = Math.max(4, (int) (delta.length()));
        for (int i = 0; i <= steps; i += 2) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void tickDive(ServerLevel serverLevel) {
        if (this.diveMark == null) {
            this.diving = false;
            return;
        }
        Vec3 toMark = this.diveMark.subtract(this.position());
        if (toMark.length() <= 1.5 || this.onGround()) {
            this.landDive(serverLevel);
            return;
        }
        this.setDeltaMovement(toMark.normalize().scale(1.1));
        this.hurtMarked = true;
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
    }

    /** 落地:穿刺 AoE → 坠风破防(硬直输出窗口,唯一全额伤害期)。 */
    private void landDive(ServerLevel serverLevel) {
        this.diving = false;
        this.setDeltaMovement(Vec3.ZERO);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(3.0, 1.5, 3.0),
                e -> e != this && !(e instanceof Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.9, 0.4, dir.z * 0.9));
                victim.hurtMarked = true;
            }
        }
        this.setBroken(true);
        this.brokenTicksRemaining = BROKEN_WINDOW_TICKS;
        this.diveMark = null;
        // 破防演出:羽状风纹围绕核心散开(10.5)
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.5, this.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    0, Math.cos(angle) * 0.5, 0.1, Math.sin(angle) * 0.5, 1.0);
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.1, 0), 3.5, ParticleTypes.CLOUD);
        BossFx.impactBurst(serverLevel, this.position().add(0, 0.5, 0), ParticleTypes.CLOUD);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.STORM_WEAVER_SHATTER.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.storm_weaver.broken");
    }

    /** 坠风硬直:停在原地,窗口结束后重新升空。 */
    private void tickBrokenWindow(ServerLevel serverLevel) {
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.4, 0.4, 0.4));
        // 核心风纹外泄(节流)
        if (this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.5, 0.6, 0.5, 0.03);
        }
        if (--this.brokenTicksRemaining <= 0 && this.isAlive()) {
            this.setBroken(false);
            this.waypointTicks = 0;   // 立刻取新航点升空
            this.playSound(SoundEvents.BREEZE_CHARGE, 1.8F, 1.2F);
        }
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(key), true);
            }
        }
    }

    // ---- 体征 ----

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        // 俯冲是机制位移,不吃摔落伤害
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STORM_WEAVER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STORM_WEAVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STORM_WEAVER_DEATH.get();
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Broken", this.isBroken());
        tag.putInt("BrokenTicks", this.brokenTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setBroken(tag.getBoolean("Broken"));
        this.brokenTicksRemaining = tag.getInt("BrokenTicks");
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isBroken()) {
                return state.setAndContinue(BROKEN_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? FLY_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("blades", BLADES_ANIM)
                .triggerableAnim("cyclone", CYCLONE_ANIM)
                .triggerableAnim("dive", DIVE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
