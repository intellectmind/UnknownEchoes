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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 世界回响体最终 Boss。三道生命地板强制完成共鸣转相，高伤害不能跨阶段。
 * 五类攻击分别读取对应能力作为减伤/抗性权限，死亡结算仍按个人参与记录发放。
 */
public class WorldEcho extends Monster implements GeoEntity {
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
    private static final RawAnimation PHASE =
            RawAnimation.begin().then("animation.world_echo.cast", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SPAWN =
            RawAnimation.begin().then("animation.world_echo.spawn", Animation.LoopType.PLAY_ONCE);

    private static final float[] PHASE_FLOORS = {0.75F, 0.50F, 0.25F};
    private static final int SPECIAL_COOLDOWN = 72;

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
                this.attackMode = AttackMode.NONE;
                this.attackAge = 0;
                this.attackDuration = 0;
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

        AttackMode mode = switch (this.attackCycle++ % 8) {
            case 0 -> AttackMode.WIND;
            case 1 -> AttackMode.LANCE;
            case 2 -> AttackMode.TIDE;
            case 3 -> AttackMode.TRUE_SIGHT;
            case 4 -> AttackMode.SLAM;
            case 5 -> AttackMode.GRAVITY;
            case 6 -> AttackMode.BARRAGE;
            default -> AttackMode.VOID;
        };
        this.beginAttack(mode, duration(mode), trigger(mode), target);
        this.attackCooldown = SPECIAL_COOLDOWN;
    }

    private void beginAttack(AttackMode mode, int duration, String trigger, LivingEntity target) {
        this.attackMode = mode;
        this.attackAge = 0;
        this.attackDuration = duration;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        Vec3 direction = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
        this.triggerAnim("attack_controller", trigger);
    }

    private void tickAttack(ServerLevel level) {
        ParticleOptions chargeParticle = switch (this.attackMode) {
            case WIND -> ParticleTypes.CLOUD;
            case TIDE -> ParticleTypes.SPLASH;
            case TRUE_SIGHT -> ParticleTypes.ENCHANT;
            case GRAVITY -> ParticleTypes.ELECTRIC_SPARK;
            case LANCE, BARRAGE -> ParticleTypes.END_ROD;
            case SLAM -> ParticleTypes.WAX_OFF;
            case VOID, PHASE -> ParticleTypes.REVERSE_PORTAL;
            case NONE -> ParticleTypes.END_ROD;
        };
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        if (this.attackAge % 4 == 0) {
            level.sendParticles(chargeParticle, core.x, core.y, core.z,
                    12, 1.2D, 1.4D, 1.2D, 0.05D);
        }
        BossFx.chargeSpiral(level, this.position(), this.tickCount, chargeParticle);
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
            case PHASE -> {
                if (this.attackAge == 24) {
                    this.releasePhase(level);
                }
            }
            case NONE -> {
            }
        }
    }

    /** 记忆之矛:锁定最近目标发射刃光轨迹,命中点迸发。 */
    private void releaseLance(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.HOSTILE, 2.0F, 1.4F);
        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.6D, 0.0D);
        for (ServerPlayer player : targets(level, 24.0D)) {
            Vec3 aim = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            BossFx.sparkSpray(level, origin, aim.subtract(origin), origin.distanceTo(aim), ParticleTypes.END_ROD);
            BossFx.impactBurst(level, player.position(), ParticleTypes.WAX_OFF);
            player.hurt(this.damageSources().mobAttack(this), 14.0F);
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
            BossFx.sweepArc(level, this.position(), player.position().subtract(this.position()),
                    10.0D, ParticleTypes.END_ROD);
            BossFx.areaBurst(level, player.position(), ParticleTypes.WAX_OFF);
            player.hurt(this.damageSources().mobAttack(this), 7.0F + wave * 2.0F);
        }
    }

    private void releaseWind(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.2F, 0.6F);
        ring(level, 5.0D, ParticleTypes.CLOUD);
        ring(level, 10.0D, ParticleTypes.END_ROD);
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
        ring(level, 13.0D, ParticleTypes.BUBBLE);
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

    private java.util.List<ServerPlayer> targets(ServerLevel level, double radius) {
        return level.players().stream()
                .filter(player -> !player.isSpectator() && player.isAlive()
                        && player.distanceToSqr(this) <= radius * radius)
                .toList();
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
            if (!player.isSpectator() && player.isAlive() && distance <= radius * radius
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
        if (!this.level().isClientSide && BossPhaseGate.shouldCapMechanicDamage(source)) {
            amount = BossPhaseGate.capDamage(this.getHealth(), amount, floor);
            if (amount <= 0.0F) {
                if (this.protectedMessageCooldown <= 0
                        && source.getEntity() instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.world_echo.guard_holds"), true);
                    this.protectedMessageCooldown = 40;
                }
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (!this.level().isClientSide && hurt && this.phaseGateStage < PHASE_FLOORS.length
                && BossPhaseGate.reachedFloor(this, floor)) {
            this.phaseTransitionPending = true;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.attackMode != AttackMode.NONE || !(target instanceof LivingEntity living)) {
            return false;
        }
        this.beginAttack(AttackMode.TRUE_SIGHT, duration(AttackMode.TRUE_SIGHT),
                trigger(AttackMode.TRUE_SIGHT), living);
        return false;
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
                EchoAbilityManager.markBossDefeated(player, EchoPermission.WORLD_ECHO_ID);
                EchoAbilityManager.activateMechanism(player, EchoPermission.WORLD_ECHO_REWARD_READY_KEY);
                EchoAbilityManager.syncToClient(player);
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
        this.attackMode = AttackMode.NONE;
        this.attackAge = 0;
        this.attackDuration = 0;
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
        PHASE
    }
}
