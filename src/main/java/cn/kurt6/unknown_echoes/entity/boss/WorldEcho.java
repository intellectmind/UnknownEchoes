package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlockEntity;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * 世界回响体最终 Boss。三道生命地板强制完成共鸣转相，高伤害不能跨阶段。
 * 主题攻击分别读取对应能力作为减伤/抗性权限，死亡结算仍按个人参与记录发放。
 */
public class WorldEcho extends Monster implements GeoEntity, PhaseGatedBoss {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.world_echo.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.world_echo.idle");
    private static final RawAnimation WIND =
            RawAnimation.begin().then("animation.world_echo.wind_vortex", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TIDE =
            RawAnimation.begin().then("animation.world_echo.tide_surge", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TRUE_SIGHT =
            RawAnimation.begin().then("animation.world_echo.true_gaze", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GRAVITY =
            RawAnimation.begin().then("animation.world_echo.gravity_crush", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VOID =
            RawAnimation.begin().then("animation.world_echo.void_rift", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation LANCE =
            RawAnimation.begin().then("animation.world_echo.cast", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SLAM =
            RawAnimation.begin().then("animation.world_echo.slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BARRAGE =
            RawAnimation.begin().then("animation.world_echo.echo_barrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation MELEE =
            RawAnimation.begin().then("animation.world_echo.echo_barrage", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BEAM =
            RawAnimation.begin().then("animation.world_echo.true_gaze", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FLAME_IMPACT =
            RawAnimation.begin().then("animation.world_echo.slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation METEOR =
            RawAnimation.begin().then("animation.world_echo.void_rift", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation NOVA =
            RawAnimation.begin().then("animation.world_echo.wind_vortex", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PHASE =
            RawAnimation.begin().then("animation.world_echo.cast", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SPAWN =
            RawAnimation.begin().then("animation.world_echo.spawn", Animation.LoopType.PLAY_ONCE);

    private static final float[] PHASE_FLOORS = {0.75F, 0.50F, 0.25F};
    private static final int SPECIAL_COOLDOWN = 72;
    private static final float BEAM_DAMAGE_MULTIPLIER = 0.70F;
    private static final double BEAM_RANGE = 36.0D;
    private static final double BEAM_WIDTH = 1.05D;
    private static final int BEAM_TRACK_TICKS = 22;
    private static final double FLAME_RADIUS = 13.0D;
    private static final int FLAME_TICKS = 8 * 20;
    private static final double METEOR_RADIUS = 3.4D;
    private static final int[] NOVA_RELEASE_TICKS = {14, 26, 38};
    private static final double[] NOVA_RADII = {6.0D, 12.0D, 18.0D};

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<UUID> participants = new HashSet<>();

    private AttackMode attackMode = AttackMode.NONE;
    private int attackAge;
    private int attackDuration;
    private int attackCooldown = 50;
    private int attackCycle;
    private int phaseGateStage;
    private boolean phaseTransitionPending;
    private boolean phaseTransitionActive;
    private int protectedMessageCooldown;
    private boolean entranceFxPlayed;
    private UUID attackTargetId;
    private Vec3 lockedAttackAim;
    private final List<Vec3> meteorImpacts = new ArrayList<>();
    private UUID altarArenaId;
    private BlockPos altarPos;

    public WorldEcho(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1400.0D)
                .add(Attributes.ATTACK_DAMAGE, 32.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(level);
        }
        if (!this.entranceFxPlayed) {
            this.entranceFxPlayed = true;
            this.playEntranceFx(level);
        }

        this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        this.attackCooldown = decrement(this.attackCooldown);
        this.protectedMessageCooldown = decrement(this.protectedMessageCooldown);
        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            this.registerNearbyParticipants(level);
        }
        if (this.phaseGateStage < PHASE_FLOORS.length
                && !this.phaseTransitionPending && !this.phaseTransitionActive
                && BossPhaseGate.reachedFloor(this, this.currentMechanicFloor())) {
            this.phaseTransitionPending = true;
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            this.tickAttack(level);
            this.attackAge++;
            if (this.attackAge >= this.attackDuration) {
                boolean completedTransition = this.phaseTransitionActive;
                this.finishAttack();
                if (completedTransition) {
                    this.completePhaseTransition(level);
                }
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (this.phaseTransitionPending) {
            if (target == null || !target.isAlive()) {
                target = this.findNearestPlayer(level);
            }
            if (target != null) {
                this.beginAttack(AttackMode.PHASE, 48, "phase", target);
                this.phaseTransitionPending = false;
                this.phaseTransitionActive = true;
                this.broadcastNearby(Component.translatable(
                        "message.unknown_echoes.world_echo.phase_locked"));
            }
            return;
        }
        if (target == null || !target.isAlive() || this.attackCooldown > 0) {
            return;
        }

        AttackMode mode = switch (Math.floorMod(this.attackCycle++, 12)) {
            case 0 -> AttackMode.WIND;
            case 1 -> AttackMode.BEAM;
            case 2 -> AttackMode.TIDE;
            case 3 -> AttackMode.METEOR;
            case 4 -> AttackMode.TRUE_SIGHT;
            case 5 -> AttackMode.FLAME_IMPACT;
            case 6 -> AttackMode.GRAVITY;
            case 7 -> AttackMode.LANCE;
            case 8 -> AttackMode.NOVA;
            case 9 -> AttackMode.SLAM;
            case 10 -> AttackMode.BARRAGE;
            default -> AttackMode.VOID;
        };
        this.beginAttack(mode, duration(mode), trigger(mode), target);
        this.attackCooldown = SPECIAL_COOLDOWN;
    }

    private void beginAttack(AttackMode mode, int duration, String trigger, LivingEntity target) {
        this.attackMode = mode;
        this.attackAge = 0;
        this.attackDuration = duration;
        this.attackTargetId = target.getUUID();
        this.lockedAttackAim = target.getEyePosition();
        this.meteorImpacts.clear();
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        this.faceAttackPosition(target.position());
        this.triggerAnim("attack_controller", trigger);
    }

    private void faceAttackPosition(Vec3 position) {
        Vec3 direction = position.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
    }

    private void tickAttack(ServerLevel level) {
        ParticleOptions chargeParticle = switch (this.attackMode) {
            case WIND -> ParticleTypes.CLOUD;
            case TIDE -> ParticleTypes.SPLASH;
            case TRUE_SIGHT -> ParticleTypes.ENCHANT;
            case GRAVITY -> ParticleTypes.ELECTRIC_SPARK;
            case LANCE, BARRAGE -> ParticleTypes.END_ROD;
            case SLAM -> ParticleTypes.WAX_OFF;
            case BEAM -> ParticleTypes.ELECTRIC_SPARK;
            case FLAME_IMPACT -> ParticleTypes.SOUL_FIRE_FLAME;
            case METEOR -> ParticleTypes.DRAGON_BREATH;
            case NOVA -> ParticleTypes.GLOW;
            case VOID, PHASE -> ParticleTypes.REVERSE_PORTAL;
            case NONE -> ParticleTypes.END_ROD;
        };
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        if (this.attackAge % 4 == 0) {
            level.sendParticles(chargeParticle, core.x, core.y, core.z,
                    12, 1.2D, 1.4D, 1.2D, 0.05D);
        }
        BossFx.chargeSpiral(level, this.position(), this.tickCount, chargeParticle);
        this.tickChargeFx(level, core, chargeParticle);
        switch (this.attackMode) {
            case WIND -> {
                if (this.attackAge == 34) {
                    this.releaseWind(level);
                }
            }
            case TIDE -> {
                if (this.attackAge == 20) {
                    this.releaseTide(level);
                }
            }
            case TRUE_SIGHT -> {
                if (this.attackAge == 22) {
                    this.releaseTrueSight(level);
                }
            }
            case GRAVITY -> {
                if (this.attackAge == 28) {
                    this.releaseGravity(level);
                }
            }
            case VOID -> {
                if (this.attackAge == 24) {
                    this.releaseVoid(level);
                }
            }
            case LANCE -> {
                if (this.attackAge == 14) {
                    this.releaseLance(level);
                }
            }
            case SLAM -> {
                if (this.attackAge == 22) {
                    this.releaseSlam(level);
                }
            }
            case BARRAGE -> {
                if (this.attackAge == 6 || this.attackAge == 18 || this.attackAge == 30) {
                    this.releaseBarrage(level, this.attackAge / 12);
                }
            }
            case BEAM -> this.tickBeam(level);
            case FLAME_IMPACT -> {
                if (this.attackAge == 22) {
                    this.releaseFlameImpact(level);
                }
            }
            case METEOR -> this.tickMeteor(level);
            case NOVA -> this.tickNova(level);
            case PHASE -> {
                if (this.attackAge == 24) {
                    this.releasePhase(level);
                }
            }
            case NONE -> {
            }
        }
    }

    /** 蓄力阶段的招式主题演出:在通用螺旋之上叠加各主题的旋涡/吸聚,让玩家一眼读出即将到来的招式。 */
    private void tickChargeFx(ServerLevel level, Vec3 core, ParticleOptions theme) {
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        switch (this.attackMode) {
            case WIND -> BossFx.vortex(level, ground, 3.4D, 5.5D, this.tickCount, ParticleTypes.CLOUD);
            case TIDE -> {
                if (this.attackAge % 3 == 0) {
                    BossFx.converge(level, ground.add(0.0D, 0.6D, 0.0D), 6.0D, 14, ParticleTypes.SPLASH);
                    // 气泡粒子离水即消失,水雾柱改用任意环境可见的 SPLASH/DOLPHIN。
                    level.sendParticles(ParticleTypes.SPLASH,
                            core.x, core.y - 0.8D, core.z, 6, 1.0D, 0.6D, 1.0D, 0.02D);
                    level.sendParticles(ParticleTypes.DOLPHIN,
                            core.x, core.y - 0.4D, core.z, 4, 0.8D, 0.8D, 0.8D, 0.02D);
                }
            }
            case TRUE_SIGHT -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, core, 5.0D, 12, ParticleTypes.ENCHANT);
                }
            }
            case GRAVITY -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, ground.add(0.0D, 0.4D, 0.0D), 7.0D, 16, ParticleTypes.PORTAL);
                }
            }
            case VOID, PHASE -> {
                if (this.attackAge % 2 == 0) {
                    BossFx.converge(level, core, 6.5D, 14, ParticleTypes.REVERSE_PORTAL);
                }
                if (this.attackAge % 6 == 0) {
                    level.sendParticles(ParticleTypes.SQUID_INK,
                            core.x, core.y + 0.4D, core.z, 4, 1.4D, 1.1D, 1.4D, 0.02D);
                }
            }
            case FLAME_IMPACT -> BossFx.vortex(level, ground, 2.8D, 4.5D, this.tickCount, ParticleTypes.FLAME);
            case BEAM -> {
                if (this.attackAge % 3 == 0) {
                    BossFx.converge(level, this.attackOrigin(), 4.0D, 10, ParticleTypes.ELECTRIC_SPARK);
                }
            }
            case SLAM -> {
                if (this.attackAge % 5 == 0) {
                    BossFx.ring(level, ground, 2.4D + (this.attackAge % 15) * 0.1D, 18, 0.1D, ParticleTypes.CRIT);
                }
            }
            case NOVA -> {
                if (this.attackAge % 4 == 0) {
                    level.sendParticles(ParticleTypes.GLOW,
                            core.x, core.y, core.z, 8, 0.6D, 1.0D, 0.6D, 0.08D);
                }
            }
            case LANCE, BARRAGE, METEOR, NONE -> {
            }
        }
    }

    /** 蓄力光束:前半段追踪、后半段锁定，释放时由方块截断并沿射线单次结算。 */
    private void tickBeam(ServerLevel level) {
        LivingEntity target = this.resolveAttackTarget(level);
        if (this.attackAge <= BEAM_TRACK_TICKS && target != null) {
            this.lockedAttackAim = target.getEyePosition();
            this.getLookControl().setLookAt(target, 40.0F, 40.0F);
            this.faceAttackPosition(target.position());
        }
        if (this.lockedAttackAim == null) {
            return;
        }

        Vec3 origin = this.attackOrigin();
        Vec3 telegraphEnd = this.clipRay(level, origin, this.lockedAttackAim);
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE, 2.4F, 0.65F);
        }
        if (this.attackAge <= 32 && this.attackAge % 2 == 0) {
            BossFx.guidanceFlow(level, origin, telegraphEnd,
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD, this.attackAge);
        }
        if (this.attackAge >= BEAM_TRACK_TICKS && this.attackAge < 34 && this.attackAge % 2 == 0) {
            BossFx.guidanceMarker(level, this.lockedAttackAim, ParticleTypes.ELECTRIC_SPARK);
        }
        if (this.attackAge == 34) {
            this.releaseBeam(level);
        }
    }

    private void releaseBeam(ServerLevel level) {
        Vec3 origin = this.attackOrigin();
        Vec3 aim = this.lockedAttackAim == null ? origin.add(this.getLookAngle()) : this.lockedAttackAim;
        Vec3 direction = aim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize();
        Vec3 beamEnd = this.clipRay(level, origin, origin.add(direction.scale(BEAM_RANGE)));
        double reach = origin.distanceTo(beamEnd);

        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 3.0F, 0.72F);
        BossFx.sparkSpray(level, origin, direction, reach, ParticleTypes.END_ROD);
        BossFx.helix(level, origin, beamEnd, 0.5D, ParticleTypes.ELECTRIC_SPARK);
        int step = 0;
        for (double distance = 0.4D; distance <= reach; distance += 0.5D) {
            Vec3 point = origin.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.GLOW, point.x, point.y, point.z,
                    2, 0.11D, 0.11D, 0.11D, 0.0D);
            if (step++ % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                        2, 0.18D, 0.18D, 0.18D, 0.025D);
            }
        }

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * BEAM_DAMAGE_MULTIPLIER;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(origin, beamEnd).inflate(BEAM_WIDTH), WorldEcho::isCombatPlayer)) {
            Vec3 toPlayer = player.getBoundingBox().getCenter().subtract(origin);
            double along = toPlayer.dot(direction);
            if (along < 0.0D || along > reach) {
                continue;
            }
            double offLine = toPlayer.subtract(direction.scale(along)).length();
            if (offLine > BEAM_WIDTH + player.getBbWidth() * 0.5D) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                BossFx.impactBurst(level, player.position(), ParticleTypes.ELECTRIC_SPARK);
                player.setDeltaMovement(player.getDeltaMovement().add(
                        direction.x * 0.55D, 0.18D, direction.z * 0.55D));
                player.hurtMarked = true;
            }
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                beamEnd.x, beamEnd.y, beamEnd.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        BossFx.impactBurst(level, beamEnd, ParticleTypes.END_ROD);
    }

    /** 烈焰冲击:伤害并点燃附近生物；允许生物破坏时额外留下分散火点。 */
    private void releaseFlameImpact(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 2.6F, 0.55F);
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.75F);
        Vec3 center = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.groundShockwave(level, center, 5.0D, ParticleTypes.FLAME);
        BossFx.groundShockwave(level, center, 9.0D, ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.groundShockwave(level, center, FLAME_RADIUS, ParticleTypes.LAVA);
        BossFx.impactBurst(level, this.attackOrigin(), ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.pillar(level, center, 7.0D, ParticleTypes.SOUL_FIRE_FLAME);
        BossFx.vortex(level, center, 4.0D, 6.0D, this.tickCount, ParticleTypes.FLAME);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.60F;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(FLAME_RADIUS, 5.0D, FLAME_RADIUS), this::isFlameTarget)) {
            Vec3 offset = victim.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (offset.lengthSqr() > FLAME_RADIUS * FLAME_RADIUS) {
                continue;
            }
            victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), FLAME_TICKS));
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = offset.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : offset.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(
                        away.x * 0.75D, 0.35D, away.z * 0.75D));
                victim.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.FLAME,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    18, victim.getBbWidth() * 0.4D, victim.getBbHeight() * 0.3D,
                    victim.getBbWidth() * 0.4D, 0.08D);
        }
        this.igniteGround(level);
    }

    private boolean isFlameTarget(LivingEntity entity) {
        return entity != this && entity.isAlive() && !entity.isSpectator()
                && (!(entity instanceof Player player) || !player.getAbilities().invulnerable);
    }

    private void igniteGround(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        double phase = this.tickCount * 0.19D;
        int[] verticalOffsets = {0, 1, -1, 2, -2};
        for (int index = 0; index < 18; index++) {
            double radius = 3.5D + (index % 3) * 3.5D;
            double angle = phase + index * (Math.PI * 2.0D / 18.0D);
            BlockPos origin = BlockPos.containing(
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.2D,
                    this.getZ() + Math.sin(angle) * radius);
            for (int offset : verticalOffsets) {
                BlockPos firePos = origin.offset(0, offset, 0);
                if (level.isEmptyBlock(firePos)
                        && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
    }

    /** 星陨标记:先冻结落点并持续给出地面预警，再统一落下，重叠区域只伤害一次。 */
    private void tickMeteor(ServerLevel level) {
        if (this.attackAge == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.HOSTILE, 2.2F, 0.55F);
        }
        if (this.attackAge == 2) {
            this.prepareMeteorImpacts(level);
        }
        if (this.attackAge >= 2 && this.attackAge < 32 && this.attackAge % 2 == 0) {
            for (Vec3 impact : this.meteorImpacts) {
                BossFx.ring(level, impact, METEOR_RADIUS, 24, 0.12D, ParticleTypes.DRAGON_BREATH);
                for (int height = 2; height <= 10; height += 2) {
                    level.sendParticles(height % 4 == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW,
                            impact.x, impact.y + height, impact.z,
                            2, 0.18D, 0.18D, 0.18D, 0.01D);
                }
                // 预警后半段:坠星拖着彗尾从高空压向落点,进度与释放帧对齐。
                if (this.attackAge >= 16) {
                    BossFx.comet(level, impact, 14.0D, (this.attackAge - 16) / 16.0D,
                            ParticleTypes.DRAGON_BREATH);
                }
            }
        }
        if (this.attackAge == 32) {
            this.releaseMeteor(level);
        }
    }

    private void prepareMeteorImpacts(ServerLevel level) {
        this.meteorImpacts.clear();
        for (ServerPlayer player : this.targets(level, 32.0D)) {
            this.meteorImpacts.add(player.position().add(0.0D, 0.08D, 0.0D));
            if (this.meteorImpacts.size() >= 4) {
                break;
            }
        }
        if (this.meteorImpacts.isEmpty() && this.lockedAttackAim != null) {
            this.meteorImpacts.add(this.lockedAttackAim);
        }
        if (this.meteorImpacts.size() == 1) {
            Vec3 center = this.meteorImpacts.get(0);
            double angle = Math.toRadians(this.getYRot());
            this.meteorImpacts.add(center.add(Math.cos(angle) * 4.5D, 0.0D, Math.sin(angle) * 4.5D));
            this.meteorImpacts.add(center.add(Math.cos(angle + Math.PI) * 4.5D,
                    0.0D, Math.sin(angle + Math.PI) * 4.5D));
        }
    }

    private void releaseMeteor(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.8F, 0.6F);
        for (Vec3 impact : this.meteorImpacts) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    impact.x, impact.y + 0.2D, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD,
                    impact.x, impact.y + 0.4D, impact.z, 48, 1.5D, 1.2D, 1.5D, 0.22D);
            BossFx.impactBurst(level, impact, ParticleTypes.DRAGON_BREATH);
            BossFx.pillar(level, impact, 6.0D, ParticleTypes.DRAGON_BREATH);
        }

        Set<UUID> hitPlayers = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (ServerPlayer player : this.targets(level, 40.0D)) {
            for (Vec3 impact : this.meteorImpacts) {
                if (player.position().distanceToSqr(impact) > METEOR_RADIUS * METEOR_RADIUS
                        || !hitPlayers.add(player.getUUID())) {
                    continue;
                }
                if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.85D, 0.0D));
                    player.hurtMarked = true;
                }
                break;
            }
        }
    }

    /** 三段共鸣新星:预告固定半径后逐圈扩散，玩家只在波前环带内受击。 */
    private void tickNova(ServerLevel level) {
        for (int wave = 0; wave < NOVA_RELEASE_TICKS.length; wave++) {
            int releaseTick = NOVA_RELEASE_TICKS[wave];
            ParticleOptions theme = novaParticle(wave);
            if (this.attackAge == releaseTick - 6 || this.attackAge == releaseTick - 2) {
                BossFx.ring(level, this.position().add(0.0D, 0.2D, 0.0D),
                        NOVA_RADII[wave], Math.max(32, (int) (NOVA_RADII[wave] * 5.0D)),
                        0.12D, theme);
            }
            if (this.attackAge == releaseTick) {
                this.releaseNova(level, wave, theme);
            }
        }
    }

    private void releaseNova(ServerLevel level, int wave, ParticleOptions theme) {
        double radius = NOVA_RADII[wave];
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.1F, 0.75F + wave * 0.22F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D), radius, theme);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 5.0D + wave * 1.5D, theme);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.28F + wave * 0.06F);
        for (ServerPlayer player : this.targets(level, radius + 3.0D)) {
            Vec3 horizontal = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (Math.abs(horizontal.length() - radius) > 2.25D
                    || Math.abs(player.getY() - this.getY()) > 4.0D) {
                continue;
            }
            if (player.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = horizontal.lengthSqr() < 1.0E-4D
                        ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        away.x * (0.55D + wave * 0.15D), 0.22D, away.z * (0.55D + wave * 0.15D)));
                player.hurtMarked = true;
                BossFx.areaBurst(level, player.position(), theme);
            }
        }
    }

    private static ParticleOptions novaParticle(int wave) {
        return switch (wave) {
            case 0 -> ParticleTypes.ENCHANT;
            case 1 -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.END_ROD;
        };
    }

    private Vec3 attackOrigin() {
        return this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
    }

    private Vec3 clipRay(ServerLevel level, Vec3 origin, Vec3 requestedEnd) {
        BlockHitResult block = level.clip(new ClipContext(origin, requestedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return block.getType() == HitResult.Type.MISS ? requestedEnd : block.getLocation();
    }

    private LivingEntity resolveAttackTarget(ServerLevel level) {
        if (this.attackTargetId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.attackTargetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void finishAttack() {
        this.attackMode = AttackMode.NONE;
        this.attackAge = 0;
        this.attackDuration = 0;
        this.attackTargetId = null;
        this.lockedAttackAim = null;
        this.meteorImpacts.clear();
    }

    /** 记忆之矛:锁定最近目标发射刃光轨迹,命中点迸发。 */
    private void releaseLance(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.HOSTILE, 2.0F, 1.4F);
        Vec3 origin = this.attackOrigin();
        LivingEntity target = this.resolveAttackTarget(level);
        Vec3 aim = target == null ? this.lockedAttackAim : target.getEyePosition();
        if (aim == null) {
            return;
        }
        Vec3 hitPoint = this.clipRay(level, origin, aim);
        BossFx.sparkSpray(level, origin, hitPoint.subtract(origin), origin.distanceTo(hitPoint),
                ParticleTypes.END_ROD);
        BossFx.helix(level, origin, hitPoint, 0.4D, ParticleTypes.GLOW);
        if (target instanceof ServerPlayer player && isCombatPlayer(player)
                && origin.distanceToSqr(aim) <= 24.0D * 24.0D
                && hitPoint.distanceToSqr(aim) < 0.25D
                && player.hurt(this.damageSources().mobAttack(this), 14.0F)) {
            BossFx.impactBurst(level, player.position(), ParticleTypes.WAX_OFF);
        } else {
            BossFx.areaBurst(level, hitPoint, ParticleTypes.END_ROD);
        }
    }

    /** 回响重击:抬升后砸落,双层地面冲击波向外扩散。 */
    private void releaseSlam(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.6F, 0.5F);
        Vec3 ground = this.position().add(0.0D, 0.15D, 0.0D);
        BossFx.groundShockwave(level, ground, 7.0D, ParticleTypes.WAX_OFF);
        BossFx.groundShockwave(level, ground, 13.0D, ParticleTypes.END_ROD);
        BossFx.impactBurst(level, ground, ParticleTypes.ELECTRIC_SPARK);
        BossFx.pillar(level, ground, 8.0D, ParticleTypes.WAX_OFF);
        level.sendParticles(ParticleTypes.CRIT, ground.x, ground.y + 0.4D, ground.z,
                60, 4.5D, 0.4D, 4.5D, 0.35D);
        for (ServerPlayer player : targets(level, 14.0D)) {
            player.hurt(this.damageSources().mobAttack(this), 20.0F);
            Vec3 away = horizontalAway(player);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 1.1D, 0.6D, away.z * 1.1D));
            player.hurtMarked = true;
        }
    }

    /** 回响连击:三段快速脉冲,每段一圈扇形刃光,伤害递增。 */
    private void releaseBarrage(ServerLevel level, int wave) {
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.0F, 1.0F + wave * 0.25F);
        for (ServerPlayer player : targets(level, 12.0D)) {
            Vec3 toPlayer = player.position().subtract(this.position());
            BossFx.sweepArc(level, this.position(), toPlayer, 10.0D, ParticleTypes.END_ROD);
            BossFx.sweepArc(level, this.position().add(0.0D, 1.1D + wave * 0.4D, 0.0D),
                    toPlayer, 9.0D, ParticleTypes.ELECTRIC_SPARK);
            BossFx.areaBurst(level, player.position(), ParticleTypes.WAX_OFF);
            player.hurt(this.damageSources().mobAttack(this), 7.0F + wave * 2.0F);
        }
    }

    private void releaseWind(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.2F, 0.6F);
        ring(level, 5.0D, ParticleTypes.CLOUD);
        ring(level, 10.0D, ParticleTypes.END_ROD);
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.pillar(level, ground, 9.0D, ParticleTypes.CLOUD);
        BossFx.vortex(level, ground, 6.0D, 7.0D, this.tickCount, ParticleTypes.CLOUD);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                ground.x, ground.y + 0.4D, ground.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (ServerPlayer player : targets(level, 14.0D)) {
            boolean attuned = hasAbility(player, EchoAbilityType.WIND_ECHO);
            float damage = attuned ? 9.0F : 15.0F;
            player.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 away = horizontalAway(player);
            double force = attuned ? 0.45D : 1.35D;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * force, attuned ? 0.12D : 0.45D, away.z * force));
            player.hurtMarked = true;
        }
    }

    private void releaseTide(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                SoundSource.HOSTILE, 2.0F, 0.65F);
        ring(level, 7.0D, ParticleTypes.SPLASH);
        ring(level, 13.0D, ParticleTypes.DOLPHIN);
        Vec3 ground = this.position().add(0.0D, 0.2D, 0.0D);
        BossFx.pillar(level, ground, 7.0D, ParticleTypes.SPLASH);
        // 潮涌浪墙:外圈一周向上喷溅的水花柱,读出"浪从 Boss 向外推"的方向感。
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0D * i / 20.0D;
            double px = this.getX() + Math.cos(angle) * 9.5D;
            double pz = this.getZ() + Math.sin(angle) * 9.5D;
            level.sendParticles(ParticleTypes.SPLASH, px, this.getY() + 0.3D, pz,
                    8, 0.25D, 0.9D, 0.25D, 0.12D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.DOLPHIN, px, this.getY() + 0.8D, pz,
                        4, 0.3D, 0.5D, 0.3D, 0.04D);
            }
        }
        for (ServerPlayer player : targets(level, 16.0D)) {
            boolean attuned = hasAbility(player, EchoAbilityType.TIDE_ECHO);
            player.hurt(this.damageSources().mobAttack(this), attuned ? 10.0F : 17.0F);
            if (!attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            }
        }
    }

    private void releaseTrueSight(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 2.2F, 1.6F);
        ring(level, 6.0D, ParticleTypes.ENCHANT);
        level.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + 2.5D, this.getZ(),
                2, 0.0D, 0.0D, 0.0D, 0.0D);
        // 凝视射线:从核心向八方射出的注视之光。
        Vec3 eye = this.attackOrigin();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D + this.tickCount * 0.1D;
            Vec3 dir = new Vec3(Math.cos(angle), -0.12D, Math.sin(angle));
            BossFx.sparkSpray(level, eye, dir, 9.0D, ParticleTypes.ENCHANT);
        }
        for (ServerPlayer player : targets(level, 12.0D)) {
            boolean attuned = hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);
            player.hurt(this.damageSources().mobAttack(this), attuned ? 11.0F : 19.0F);
            if (!attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            }
        }
    }

    private void releaseGravity(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.5F, 0.55F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                12.0D, ParticleTypes.ELECTRIC_SPARK);
        // 重力坍缩:先向心吸入,再垂直压落一道光柱。
        BossFx.converge(level, this.attackOrigin(), 9.0D, 24, ParticleTypes.PORTAL);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 10.0D, ParticleTypes.ELECTRIC_SPARK);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + 0.6D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (ServerPlayer player : targets(level, 15.0D)) {
            boolean attuned = hasAbility(player, EchoAbilityType.GRAVITY_ECHO);
            player.hurt(this.damageSources().mobAttack(this), attuned ? 13.0F : 23.0F);
            Vec3 away = horizontalAway(player);
            double force = attuned ? 0.3D : 0.85D;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * force, attuned ? 0.18D : 0.8D, away.z * force));
            player.hurtMarked = true;
        }
    }

    private void releaseVoid(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.4F, 0.8F);
        for (double radius : new double[]{4.0D, 9.0D, 15.0D, 21.0D}) {
            ring(level, radius, ParticleTypes.REVERSE_PORTAL);
        }
        // 虚空内爆:黑雾聚拢、闪光炸开,再向外铺一层龙息虚蚀环。
        Vec3 core = this.attackOrigin();
        level.sendParticles(ParticleTypes.SQUID_INK, core.x, core.y, core.z,
                40, 2.2D, 1.8D, 2.2D, 0.06D);
        level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z,
                3, 0.0D, 0.0D, 0.0D, 0.0D);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                17.0D, ParticleTypes.DRAGON_BREATH);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 11.0D, ParticleTypes.REVERSE_PORTAL);
        for (ServerPlayer player : targets(level, 22.0D)) {
            boolean attuned = hasAbility(player, EchoAbilityType.VOID_ECHO);
            player.hurt(this.damageSources().mobAttack(this), attuned ? 15.0F : 28.0F);
            if (!attuned) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
        }
    }

    private void releasePhase(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.4F, 0.55F);
        BossFx.groundShockwave(level, this.position().add(0.0D, 0.15D, 0.0D),
                10.0D, ParticleTypes.END_ROD);
        BossFx.pillar(level, this.position().add(0.0D, 0.15D, 0.0D), 12.0D, ParticleTypes.REVERSE_PORTAL);
        BossFx.converge(level, this.attackOrigin(), 8.0D, 20, ParticleTypes.END_ROD);
        for (ServerPlayer player : targets(level, 12.0D)) {
            player.hurt(this.damageSources().mobAttack(this), 12.0F);
            Vec3 away = horizontalAway(player);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    away.x * 0.65D, 0.35D, away.z * 0.65D));
            player.hurtMarked = true;
        }
    }

    private void completePhaseTransition(ServerLevel level) {
        this.phaseTransitionActive = false;
        this.phaseGateStage = Math.min(PHASE_FLOORS.length, this.phaseGateStage + 1);
        this.attackCooldown = 35;
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.2F, 0.7F + this.phaseGateStage * 0.12F);
        this.broadcastNearby(Component.translatable("message.unknown_echoes.world_echo.phase_open"));
    }

    private void ring(ServerLevel level, double radius, ParticleOptions particle) {
        BossFx.ring(level, this.position().add(0.0D, 0.2D, 0.0D),
                radius, Math.max(32, (int) (radius * 5.0D)), 0.12D, particle);
    }

    private List<ServerPlayer> targets(ServerLevel level, double radius) {
        return level.players().stream()
                .filter(player -> isCombatPlayer(player)
                        && player.distanceToSqr(this) <= radius * radius)
                .toList();
    }

    private static boolean isCombatPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().invulnerable;
    }

    private Vec3 horizontalAway(ServerPlayer player) {
        Vec3 away = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        return away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
    }

    private static boolean hasAbility(ServerPlayer player, EchoAbilityType ability) {
        return EchoAbilityManager.hasAbility(player, ability);
    }

    private void registerNearbyParticipants(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.isAlive()
                    && player.distanceToSqr(this) <= radius * radius) {
                this.registerParticipant(player);
            }
        }
    }

    private LivingEntity findNearestPlayer(ServerLevel level) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(this);
            if (isCombatPlayer(player) && distance <= radius * radius
                    && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private float currentMechanicFloor() {
        return this.phaseGateStage < PHASE_FLOORS.length
                ? this.getMaxHealth() * PHASE_FLOORS[this.phaseGateStage]
                : 0.0F;
    }

    @Override
    public float phaseDamageFloor() {
        return this.currentMechanicFloor();
    }

    @Override
    public void onPhaseDamageBlocked(DamageSource source) {
        if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_echo.guard_holds"), true);
            this.protectedMessageCooldown = 40;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            this.registerParticipant(attacker);
            if (this.getTarget() == null) {
                this.setTarget(attacker);
            }
        }
        float floor = this.currentMechanicFloor();
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt && this.phaseGateStage < PHASE_FLOORS.length
                && BossPhaseGate.reachedFloor(this, floor)) {
            this.phaseTransitionPending = true;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.attackMode != AttackMode.NONE) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && this.level() instanceof ServerLevel level) {
            this.attackCooldown = Math.max(this.attackCooldown, 10);
            this.triggerAnim("attack_controller", "melee");
            Vec3 impact = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT,
                    impact.x, impact.y, impact.z, 14, 0.45D, 0.55D, 0.45D, 0.12D);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.HOSTILE, 1.5F, 0.65F);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= radius * radius
                    && EchoPermission.canSummonWorldEcho(player)) {
                if (!EchoAbilityManager.markBossDefeatedWithMechanisms(player,
                        EchoPermission.WORLD_ECHO_ID,
                        EchoPermission.WORLD_ECHO_REWARD_READY_KEY)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"));
                    continue;
                }
                player.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.world_echo.defeated"));
            }
        }
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 5, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD,
                core.x, core.y, core.z, 260, 4.0D, 3.8D, 4.0D, 0.35D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                core.x, core.y, core.z, 320, 4.2D, 4.0D, 4.2D, 0.45D);
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 3.0F, 1.1F);
        this.notifyAltarSettled(level);
    }

    public void registerParticipant(ServerPlayer player) {
        if (!player.isSpectator() && (this.participants.contains(player.getUUID())
                || this.participants.size() < BossNbtData.MAX_PARTICIPANTS)) {
            this.participants.add(player.getUUID());
        }
    }

    public boolean canBindToAltar() {
        return this.altarArenaId == null && this.altarPos == null;
    }

    public boolean bindToAltar(UUID arenaId, BlockPos pos) {
        if (!this.canBindToAltar() && !this.matchesAltar(arenaId, pos)) {
            return false;
        }
        this.altarArenaId = arenaId;
        this.altarPos = pos.immutable();
        return true;
    }

    public boolean matchesAltar(UUID arenaId, BlockPos pos) {
        return arenaId != null && arenaId.equals(this.altarArenaId)
                && pos != null && pos.equals(this.altarPos);
    }

    private void notifyAltarSettled(ServerLevel level) {
        if (this.altarArenaId == null || this.altarPos == null) {
            return;
        }
        if (level.getBlockEntity(this.altarPos) instanceof WorldResonanceAltarBlockEntity altar) {
            altar.onBossSettled(this.altarArenaId, this.getUUID());
        }
    }

    private void broadcastNearby(Component message) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(message, true);
            }
        }
    }

    private void playEntranceFx(ServerLevel level) {
        this.triggerAnim("attack_controller", "spawn");
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.8F, 0.75F);
        for (double radius : new double[]{3.0D, 6.0D, 9.0D}) {
            ring(level, radius, radius == 6.0D ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL);
        }
        level.sendParticles(ParticleTypes.GLOW,
                core.x, core.y, core.z, 120, 2.2D, 2.8D, 2.2D, 0.14D);
    }

    private static int duration(AttackMode mode) {
        return switch (mode) {
            case WIND -> 48;
            case TIDE -> 40;
            case TRUE_SIGHT -> 52;
            case GRAVITY -> 48;
            case VOID -> 56;
            case LANCE -> 36;
            case SLAM -> 44;
            case BARRAGE -> 36;
            case BEAM -> 52;
            case FLAME_IMPACT -> 44;
            case METEOR -> 56;
            case NOVA -> 48;
            case PHASE -> 48;
            case NONE -> 0;
        };
    }

    private static String trigger(AttackMode mode) {
        return switch (mode) {
            case WIND -> "wind";
            case TIDE -> "tide";
            case TRUE_SIGHT -> "true_sight";
            case GRAVITY -> "gravity";
            case VOID -> "void";
            case LANCE -> "lance";
            case SLAM -> "slam";
            case BARRAGE -> "barrage";
            case BEAM -> "beam";
            case FLAME_IMPACT -> "flame";
            case METEOR -> "meteor";
            case NOVA -> "nova";
            case PHASE -> "phase";
            case NONE -> "true_sight";
        };
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
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
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // 最终 Boss 不自然消失。
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhaseGateStage", this.phaseGateStage);
        tag.putBoolean("PhaseTransitionPending",
                this.phaseTransitionPending || this.phaseTransitionActive);
        tag.putInt("AttackCycle", this.attackCycle);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        if (this.altarArenaId != null && this.altarPos != null) {
            tag.putUUID("AltarArenaId", this.altarArenaId);
            tag.putLong("AltarPos", this.altarPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phaseGateStage = Math.max(0, Math.min(PHASE_FLOORS.length,
                tag.getInt("PhaseGateStage")));
        this.phaseTransitionPending = tag.getBoolean("PhaseTransitionPending");
        this.phaseTransitionActive = false;
        this.attackCycle = Math.max(0, tag.getInt("AttackCycle"));
        this.finishAttack();
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        this.altarArenaId = tag.hasUUID("AltarArenaId") ? tag.getUUID("AltarArenaId") : null;
        this.altarPos = null;
        if (this.altarArenaId != null && tag.contains("AltarPos", net.minecraft.nbt.Tag.TAG_LONG)) {
            BlockPos savedPos = BlockPos.of(tag.getLong("AltarPos"));
            BlockPos current = this.blockPosition();
            if (Math.abs((long) savedPos.getX() - current.getX()) <= 128L
                    && Math.abs((long) savedPos.getY() - current.getY()) <= 128L
                    && Math.abs((long) savedPos.getZ() - current.getZ()) <= 128L) {
                this.altarPos = savedPos;
            } else {
                this.altarArenaId = null;
            }
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("wind", WIND)
                .triggerableAnim("tide", TIDE)
                .triggerableAnim("true_sight", TRUE_SIGHT)
                .triggerableAnim("gravity", GRAVITY)
                .triggerableAnim("void", VOID)
                .triggerableAnim("lance", LANCE)
                .triggerableAnim("slam", SLAM)
                .triggerableAnim("barrage", BARRAGE)
                .triggerableAnim("melee", MELEE)
                .triggerableAnim("beam", BEAM)
                .triggerableAnim("flame", FLAME_IMPACT)
                .triggerableAnim("meteor", METEOR)
                .triggerableAnim("nova", NOVA)
                .triggerableAnim("phase", PHASE)
                .triggerableAnim("spawn", SPAWN));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private enum AttackMode {
        NONE,
        WIND,
        TIDE,
        TRUE_SIGHT,
        GRAVITY,
        VOID,
        LANCE,
        SLAM,
        BARRAGE,
        BEAM,
        FLAME_IMPACT,
        METEOR,
        NOVA,
        PHASE
    }
}
