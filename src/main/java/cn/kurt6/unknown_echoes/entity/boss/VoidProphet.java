package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.deep.GravityCoreAltarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

/**
 * 虚空先知:第四主线 Boss。由校准大厅核心召唤，死亡时为仍在场地内的参与玩家结算。
 */
public class VoidProphet extends Monster implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.void_prophet.idle");
    private static final RawAnimation SPELL_SWAY_ANIM =
            RawAnimation.begin().then("animation.void_prophet.spell_sway", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SHORT_HOP_ANIM =
            RawAnimation.begin().then("animation.void_prophet.short_hop", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SPATIAL_REND_ANIM =
            RawAnimation.begin().then("animation.void_prophet.spatial_rend", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GRAVITY_RECOIL_ANIM =
            RawAnimation.begin().then("animation.void_prophet.gravity_recoil", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation CORE_BREAK_ANIM =
            RawAnimation.begin().then("animation.void_prophet.core_break", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VOID_LANCE_ANIM =
            RawAnimation.begin().then("animation.void_prophet.void_lance", Animation.LoopType.PLAY_ONCE);

    private static final double BASE_MAX_HEALTH = 680.0D;
    private static final double HEALTH_PER_EXTRA_PLAYER = 220.0D;
    private static final double REND_LENGTH = 22.0D;
    private static final double REND_HALF_WIDTH = 1.35D;
    private static final double GRAVITY_RADIUS = 10.0D;
    private static final double LANCE_BLAST_RADIUS = 2.35D;
    private static final double ARENA_RADIUS = 19.0D;
    private static final float ENRAGE_THRESHOLD = 0.45F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> participants = new HashSet<>();
    private final List<LineRift> rendLines = new ArrayList<>();
    private final List<Vec3> constellationMarks = new ArrayList<>();
    private final List<Vec3> lanceMarks = new ArrayList<>();
    private final List<VoidScar> voidScars = new ArrayList<>();

    private AttackMode attackMode = AttackMode.NONE;
    private int attackTicks = 0;
    private int blinkCooldown = 45;
    private int rendCooldown = 75;
    private int gravityCooldown = 120;
    private int constellationCooldown = 35;
    private int lanceCooldown = 90;
    private int outOfCombatTicks = 0;
    private int scaledParticipants = 1;
    private int protectedMessageCooldown;
    private boolean enraged = false;
    private boolean phaseTransitionTriggered = false;
    private BlockPos arenaCenter;

    public VoidProphet(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 28.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tickCount % 20 == 0) {
            cn.kurt6.unknown_echoes.event.CommonEvents.noteMainBossPresence(serverLevel);
        }

        this.tickBossSystems(serverLevel);
        this.tickVoidScars(serverLevel);
        this.spawnAmbientVoid(serverLevel);

        if (!this.phaseTransitionTriggered && this.getHealth() <= this.getMaxHealth() * ENRAGE_THRESHOLD) {
            this.startCoreBreak(serverLevel);
        }

        if (this.attackMode != AttackMode.NONE) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.45D, 1.0D, 0.45D));
            this.tickActiveAttack(serverLevel);
            return;
        }

        this.tickCooldowns();
        this.tryStartAttack(serverLevel);
    }

    private void tickBossSystems(ServerLevel serverLevel) {
        this.enforceArenaBoundary(serverLevel);
        if (this.protectedMessageCooldown > 0) {
            this.protectedMessageCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.outOfCombatTicks = 0;
            if (this.tickCount % 20 == 0) {
                double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
                for (ServerPlayer player : serverLevel.players()) {
                    if (!player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                        this.registerParticipant(player);
                    }
                }
                if (ServerConfig.BOSS_DYNAMIC_SCALING.get()) {
                    this.updateDynamicScaling();
                }
            }
        } else if (++this.outOfCombatTicks > 120 && this.outOfCombatTicks % 20 == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.015F);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    public void bindArena(BlockPos center) {
        this.arenaCenter = center.immutable();
        this.restrictTo(this.arenaCenter, (int) ARENA_RADIUS);
    }

    private void enforceArenaBoundary(ServerLevel level) {
        if (this.arenaCenter == null) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && this.horizontalDistanceSqr(target.position()) > square(ARENA_RADIUS + 4.0D)) {
            this.setTarget(null);
        }
        if (this.tickCount % 10 != 0
                || this.horizontalDistanceSqr(this.position()) <= square(ARENA_RADIUS + 3.0D)) {
            return;
        }
        Vec3 returnPoint = Vec3.atBottomCenterOf(this.arenaCenter).add(0.0D, 0.0D, 8.0D);
        this.teleportTo(returnPoint.x, returnPoint.y, returnPoint.z);
        this.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                returnPoint.x, returnPoint.y + this.getBbHeight() * 0.5D, returnPoint.z,
                28, 0.8D, 1.2D, 0.8D, 0.1D);
    }

    private double horizontalDistanceSqr(Vec3 position) {
        double dx = position.x - (this.arenaCenter.getX() + 0.5D);
        double dz = position.z - (this.arenaCenter.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double square(double value) {
        return value * value;
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

    private void tickCooldowns() {
        if (this.blinkCooldown > 0) this.blinkCooldown--;
        if (this.rendCooldown > 0) this.rendCooldown--;
        if (this.gravityCooldown > 0) this.gravityCooldown--;
        if (this.constellationCooldown > 0) this.constellationCooldown--;
        if (this.lanceCooldown > 0) this.lanceCooldown--;
    }

    private int phaseCooldown(int normal, int omen) {
        return this.enraged ? omen : normal;
    }

    private void tryStartAttack(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSq = this.distanceToSqr(target);
        if (this.gravityCooldown <= 0 && distanceSq <= GRAVITY_RADIUS * GRAVITY_RADIUS) {
            this.startGravityRecoil(serverLevel);
        } else if (this.lanceCooldown <= 0 && distanceSq <= 32.0D * 32.0D) {
            this.startVoidLance(serverLevel, target);
        } else if (this.rendCooldown <= 0 && distanceSq <= REND_LENGTH * REND_LENGTH) {
            this.startSpatialRend(serverLevel, target);
        } else if (this.constellationCooldown <= 0) {
            this.startConstellation(serverLevel, target);
        } else if (this.blinkCooldown <= 0) {
            this.startShortHop(serverLevel, target);
        }
    }

    private void tickActiveAttack(ServerLevel serverLevel) {
        switch (this.attackMode) {
            case SHORT_HOP -> this.tickShortHop(serverLevel);
            case SPATIAL_REND -> this.tickSpatialRend(serverLevel);
            case GRAVITY_RECOIL -> this.tickGravityRecoil(serverLevel);
            case CONSTELLATION -> this.tickConstellation(serverLevel);
            case VOID_LANCE -> this.tickVoidLance(serverLevel);
            case CORE_BREAK -> this.tickCoreBreak(serverLevel);
            case NONE -> {
            }
        }
        if (--this.attackTicks <= 0) {
            this.attackMode = AttackMode.NONE;
        }
    }

    // ---- 裂隙短跃 + 延迟残影爆发 ----

    private void startShortHop(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.SHORT_HOP;
        this.attackTicks = 18;
        this.blinkCooldown = phaseCooldown(105, 72);
        this.triggerAnim("attack_controller", "short_hop");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PORTAL_AMBIENT,
                SoundSource.HOSTILE, 1.8F, 0.55F);
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
    }

    private void tickShortHop(ServerLevel serverLevel) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        spiral(serverLevel, core, 1.4D, this.attackTicks * 0.6D, ParticleTypes.REVERSE_PORTAL);
        if (this.attackTicks != 10) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 oldPos = this.position();
        Vec3 destination = this.findSafeHop(serverLevel, target);
        this.voidScars.add(new VoidScar(oldPos.add(0.0D, 0.1D, 0.0D), 26));
        this.teleportTo(destination.x, destination.y, destination.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true;

        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                oldPos.x, oldPos.y + this.getBbHeight() * 0.5D, oldPos.z,
                70, 1.0D, 2.2D, 1.0D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + this.getBbHeight() * 0.5D, destination.z,
                90, 1.1D, 2.4D, 1.1D, 0.18D);
        drawLine(serverLevel, oldPos.add(0.0D, 1.0D, 0.0D), destination.add(0.0D, 1.0D, 0.0D),
                0.7D, ParticleTypes.DRAGON_BREATH);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 2.2F, 0.65F);
    }

    private Vec3 findSafeHop(ServerLevel level, LivingEntity target) {
        double baseAngle = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX());
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = baseAngle + Math.PI + (this.random.nextDouble() - 0.5D) * Math.PI * 1.4D;
            double distance = 4.5D + this.random.nextDouble() * 5.0D;
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            for (int dy = 2; dy >= -2; dy--) {
                double y = target.getY() + dy;
                BlockPos floor = BlockPos.containing(x, y - 0.1D, z);
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    continue;
                }
                AABB movedBox = this.getBoundingBox().move(
                        x - this.getX(), y - this.getY(), z - this.getZ());
                if (level.noCollision(this, movedBox)) {
                    return new Vec3(x, y, z);
                }
            }
        }
        return this.position();
    }

    private void tickVoidScars(ServerLevel serverLevel) {
        if (this.voidScars.isEmpty()) {
            return;
        }
        for (int i = this.voidScars.size() - 1; i >= 0; i--) {
            VoidScar scar = this.voidScars.get(i);
            double radius = 0.7D + (26 - scar.ticks) * 0.045D;
            if (scar.ticks % 2 == 0) {
                ring(serverLevel, scar.position, radius, 12, ParticleTypes.PORTAL);
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                        scar.position.x, scar.position.y + 0.5D, scar.position.z,
                        3, radius * 0.4D, 0.5D, radius * 0.4D, 0.01D);
            }
            if (--scar.ticks <= 0) {
                this.damagePlayersNear(serverLevel, scar.position, 3.2D,
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F,
                        0.65D, 0.35D, false);
                serverLevel.sendParticles(ParticleTypes.FLASH,
                        scar.position.x, scar.position.y + 0.6D, scar.position.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        scar.position.x, scar.position.y + 0.6D, scar.position.z,
                        45, 0.9D, 0.7D, 0.9D, 0.25D);
                serverLevel.playSound(null, BlockPos.containing(scar.position), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                        SoundSource.HOSTILE, 1.4F, 1.45F);
                this.voidScars.remove(i);
            }
        }
    }

    // ---- 空间撕裂:锁定位置的线形裂缝,二阶段变为三线交叉 ----

    private void startSpatialRend(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.SPATIAL_REND;
        this.attackTicks = 32;
        this.rendCooldown = phaseCooldown(125, 86);
        this.rendLines.clear();

        Vec3 center = target.position().add(0.0D, 0.1D, 0.0D);
        Vec3 facing = center.subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        if (facing.lengthSqr() < 1.0E-4D) {
            facing = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            facing = facing.normalize();
        }
        int lineCount = this.enraged ? 3 : 1;
        for (int i = 0; i < lineCount; i++) {
            double angle = lineCount == 1 ? 0.0D : (i - 1) * Math.PI / 3.0D;
            Vec3 direction = rotateHorizontal(facing, angle);
            this.rendLines.add(new LineRift(center.subtract(direction.scale(REND_LENGTH * 0.5D)),
                    direction, REND_LENGTH));
        }
        this.triggerAnim("attack_controller", "spatial_rend");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.1F, 1.45F);
    }

    private void tickSpatialRend(ServerLevel serverLevel) {
        if ((this.attackTicks & 1) == 0) {
            for (LineRift line : this.rendLines) {
                for (double d = 0.0D; d <= line.length; d += 1.25D) {
                    Vec3 point = line.origin.add(line.direction.scale(d));
                    serverLevel.sendParticles(this.attackTicks % 3 == 0
                                    ? ParticleTypes.DRAGON_BREATH : ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y, point.z, 1, 0.08D, 0.04D, 0.08D, 0.0D);
                }
            }
        }
        if (this.attackTicks % 5 == 0) {
            Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
            spiral(serverLevel, core, 1.8D, this.attackTicks * 0.45D, ParticleTypes.PORTAL);
        }
        if (this.attackTicks == 5) {
            this.releaseSpatialRend(serverLevel);
        }
    }

    private void releaseSpatialRend(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85F;
        for (LineRift line : this.rendLines) {
            Vec3 end = line.origin.add(line.direction.scale(line.length));
            AABB bounds = new AABB(line.origin, end).inflate(REND_HALF_WIDTH, 3.5D, REND_HALF_WIDTH);
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, bounds,
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                Vec3 relative = player.position().subtract(line.origin).multiply(1.0D, 0.0D, 1.0D);
                double along = Math.max(0.0D, Math.min(line.length, relative.dot(line.direction)));
                Vec3 nearest = line.origin.add(line.direction.scale(along));
                Vec3 away = player.position().subtract(nearest).multiply(1.0D, 0.0D, 1.0D);
                if (away.horizontalDistanceSqr() > REND_HALF_WIDTH * REND_HALF_WIDTH) {
                    continue;
                }
                player.hurt(this.damageSources().mobAttack(this), damage);
                Vec3 knock = away.lengthSqr() > 1.0E-4D
                        ? away.normalize() : new Vec3(-line.direction.z, 0.0D, line.direction.x);
                player.setDeltaMovement(player.getDeltaMovement().add(knock.x * 0.85D, 0.28D, knock.z * 0.85D));
                player.hurtMarked = true;
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
                hit.add(player.getUUID());
            }
            for (double d = 0.0D; d <= line.length; d += 0.55D) {
                Vec3 point = line.origin.add(line.direction.scale(d));
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        point.x, point.y + 0.25D, point.z, 2, 0.18D, 0.25D, 0.18D, 0.08D);
                if (((int) (d * 10.0D)) % 22 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            point.x, point.y + 0.15D, point.z, 1, 0.05D, 0.1D, 0.05D, 0.0D);
                }
            }
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.4F, 1.65F);
    }

    // ---- 重力反冲:收束牵引 -> 内爆 -> 外推冲击 ----

    private void startGravityRecoil(ServerLevel serverLevel) {
        this.attackMode = AttackMode.GRAVITY_RECOIL;
        this.attackTicks = 36;
        this.gravityCooldown = phaseCooldown(185, 132);
        this.triggerAnim("attack_controller", "gravity_recoil");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.HOSTILE, 2.0F, 0.45F);
    }

    private void tickGravityRecoil(ServerLevel serverLevel) {
        Vec3 center = this.position().add(0.0D, 0.35D, 0.0D);
        double warningRadius = 2.0D + GRAVITY_RADIUS * this.attackTicks / 36.0D;
        if ((this.attackTicks & 1) == 0) {
            ring(serverLevel, center, warningRadius, 28, ParticleTypes.REVERSE_PORTAL);
            ring(serverLevel, center, Math.max(1.0D, warningRadius * 0.58D), 18,
                    ParticleTypes.ELECTRIC_SPARK);
            spiral(serverLevel, center.add(0.0D, 2.0D, 0.0D), 2.2D,
                    (36 - this.attackTicks) * 0.65D, ParticleTypes.DRAGON_BREATH);
        }

        if (this.attackTicks > 18 && this.attackTicks % 3 == 0) {
            this.pullPlayers(serverLevel, center, GRAVITY_RADIUS, 0.18D);
        }
        if (this.attackTicks == 18) {
            this.damagePlayersNear(serverLevel, center, 7.0D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F,
                    0.9D, 0.22D, true);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                    center.x, center.y + 1.0D, center.z, 55, 3.5D, 1.2D, 3.5D, 0.18D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.HOSTILE, 1.8F, 1.65F);
        }
        if (this.attackTicks == 3) {
            this.damagePlayersNear(serverLevel, center, 8.5D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F,
                    1.65D, 0.55D, false);
            for (int band = 1; band <= 3; band++) {
                ring(serverLevel, center, band * 3.0D, 32, ParticleTypes.DRAGON_BREATH);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    center.x, center.y + 1.0D, center.z, 4, 1.5D, 1.0D, 1.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y + 1.0D, center.z, 120, 2.0D, 2.2D, 2.0D, 0.35D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 2.5F, 0.55F);
        }
    }

    // ---- 虚空星阵:多点预警,延迟引爆 ----

    private void startConstellation(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.CONSTELLATION;
        this.attackTicks = 36;
        this.constellationCooldown = phaseCooldown(145, 102);
        this.constellationMarks.clear();
        this.constellationMarks.add(target.position().add(0.0D, 0.1D, 0.0D));
        int count = this.enraged ? 7 : 4;
        for (int i = 1; i < count; i++) {
            double angle = Math.PI * 2.0D * i / (count - 1) + this.random.nextDouble() * 0.5D;
            double distance = 2.8D + this.random.nextDouble() * 4.2D;
            this.constellationMarks.add(target.position().add(
                    Math.cos(angle) * distance, 0.1D, Math.sin(angle) * distance));
        }
        this.triggerAnim("attack_controller", "spell_sway");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private void tickConstellation(ServerLevel serverLevel) {
        double radius = 0.9D + (36 - this.attackTicks) * 0.035D;
        if ((this.attackTicks & 1) == 0) {
            for (int index = 0; index < this.constellationMarks.size(); index++) {
                Vec3 mark = this.constellationMarks.get(index);
                ring(serverLevel, mark, radius, 12, index % 2 == 0
                        ? ParticleTypes.PORTAL : ParticleTypes.DRAGON_BREATH);
                if (this.attackTicks % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                            mark.x, mark.y + 0.25D, mark.z,
                            3, radius * 0.5D, 0.05D, radius * 0.5D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            mark.x, mark.y + 2.8D, mark.z, 1, 0.05D, 0.2D, 0.05D, 0.01D);
                }
            }
        }
        if (this.attackTicks == 4) {
            this.detonateConstellation(serverLevel);
        }
    }

    private void detonateConstellation(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F;
        for (Vec3 mark : this.constellationMarks) {
            for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                    new AABB(mark.x - 2.35D, mark.y - 1.0D, mark.z - 2.35D,
                            mark.x + 2.35D, mark.y + 4.0D, mark.z + 2.35D),
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                player.hurt(this.damageSources().mobAttack(this), damage);
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                hit.add(player.getUUID());
            }
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    mark.x, mark.y + 0.7D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    mark.x, mark.y + 0.8D, mark.z, 38, 0.75D, 1.3D, 0.75D, 0.16D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    mark.x, mark.y + 0.7D, mark.z, 24, 0.45D, 1.0D, 0.45D, 0.2D);
        }
        for (int i = 0; i < this.constellationMarks.size(); i++) {
            drawLine(serverLevel, this.constellationMarks.get(i),
                    this.constellationMarks.get((i + 1) % this.constellationMarks.size()),
                    0.45D, ParticleTypes.ELECTRIC_SPARK);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 2.3F, 0.5F);
    }

    // ---- 虚空棱雨:多点锁定后从高空贯穿,狂暴阶段增加落点 ----

    private void startVoidLance(ServerLevel serverLevel, LivingEntity target) {
        this.attackMode = AttackMode.VOID_LANCE;
        this.attackTicks = 34;
        this.lanceCooldown = phaseCooldown(165, 116);
        this.lanceMarks.clear();

        Vec3 targetCenter = target.position();
        this.lanceMarks.add(this.findLanceGround(serverLevel, targetCenter));
        int count = this.enraged ? 6 : 4;
        double phaseOffset = this.random.nextDouble() * Math.PI * 2.0D;
        for (int i = 1; i < count; i++) {
            double angle = phaseOffset + Math.PI * 2.0D * (i - 1) / (count - 1)
                    + (this.random.nextDouble() - 0.5D) * 0.35D;
            double distance = 3.6D + this.random.nextDouble() * 4.8D;
            Vec3 candidate = targetCenter.add(
                    Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            this.lanceMarks.add(this.findLanceGround(serverLevel, candidate));
        }

        this.triggerAnim("attack_controller", "void_lance");
        this.getLookControl().setLookAt(target, 45.0F, 45.0F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.2F, 1.65F);
    }

    private Vec3 findLanceGround(ServerLevel level, Vec3 candidate) {
        BlockPos start = BlockPos.containing(candidate.x, candidate.y + 2.0D, candidate.z);
        for (int offset = 0; offset <= 9; offset++) {
            BlockPos floor = start.below(offset);
            if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return new Vec3(candidate.x, floor.getY() + 1.05D, candidate.z);
            }
        }
        return new Vec3(candidate.x, candidate.y + 0.1D, candidate.z);
    }

    private void tickVoidLance(ServerLevel serverLevel) {
        double closingRadius = 0.75D + this.attackTicks * 0.055D;
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.68D, 0.0D);
        if ((this.attackTicks & 1) == 0) {
            for (int index = 0; index < this.lanceMarks.size(); index++) {
                Vec3 mark = this.lanceMarks.get(index);
                ring(serverLevel, mark, LANCE_BLAST_RADIUS, 18, ParticleTypes.REVERSE_PORTAL);
                ring(serverLevel, mark.add(0.0D, 0.04D, 0.0D), closingRadius, 14,
                        index % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD);

                double lanceHeight = 4.0D + this.attackTicks * 0.18D;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        mark.x, mark.y + lanceHeight, mark.z,
                        2, 0.12D, 0.5D, 0.12D, 0.015D);
                if (this.attackTicks % 4 == 0) {
                    drawLine(serverLevel, mark.add(0.0D, 0.15D, 0.0D),
                            mark.add(0.0D, lanceHeight + 2.0D, 0.0D),
                            1.15D, ParticleTypes.ELECTRIC_SPARK);
                    drawLine(serverLevel, core, mark.add(0.0D, lanceHeight, 0.0D),
                            1.4D, ParticleTypes.DRAGON_BREATH);
                }
            }
        }
        if (this.attackTicks % 3 == 0) {
            spiral(serverLevel, core, 2.4D, (34 - this.attackTicks) * 0.7D,
                    ParticleTypes.REVERSE_PORTAL);
        }
        if (this.attackTicks == 7) {
            this.detonateVoidLances(serverLevel);
        }
    }

    private void detonateVoidLances(ServerLevel serverLevel) {
        Set<UUID> hit = new HashSet<>();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        for (Vec3 mark : this.lanceMarks) {
            AABB area = new AABB(
                    mark.x - LANCE_BLAST_RADIUS, mark.y - 1.0D, mark.z - LANCE_BLAST_RADIUS,
                    mark.x + LANCE_BLAST_RADIUS, mark.y + 10.0D, mark.z + LANCE_BLAST_RADIUS);
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, area,
                    p -> p.isAlive() && !p.isSpectator() && !hit.contains(p.getUUID()))) {
                Vec3 horizontal = player.position().subtract(mark).multiply(1.0D, 0.0D, 1.0D);
                if (horizontal.horizontalDistanceSqr() > LANCE_BLAST_RADIUS * LANCE_BLAST_RADIUS) {
                    continue;
                }
                player.hurt(this.damageSources().mobAttack(this), damage);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.58D, 0.0D));
                player.hurtMarked = true;
                hit.add(player.getUUID());
            }
            this.spawnVoidLanceBurst(serverLevel, mark);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.8F, 1.75F);
    }

    private void spawnVoidLanceBurst(ServerLevel level, Vec3 mark) {
        level.sendParticles(ParticleTypes.FLASH,
                mark.x, mark.y + 1.0D, mark.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                mark.x, mark.y + 4.2D, mark.z, 42, 0.5D, 3.8D, 0.5D, 0.14D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                mark.x, mark.y + 2.0D, mark.z, 62, 0.8D, 2.4D, 0.8D, 0.18D);
        level.sendParticles(ParticleTypes.END_ROD,
                mark.x, mark.y + 5.0D, mark.z, 24, 0.4D, 4.0D, 0.4D, 0.12D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                mark.x, mark.y + 0.25D, mark.z, 46, 1.4D, 0.2D, 1.4D, 0.28D);
        drawLine(level, mark.add(0.0D, 0.1D, 0.0D), mark.add(0.0D, 13.0D, 0.0D),
                0.38D, ParticleTypes.ELECTRIC_SPARK);
        ring(level, mark.add(0.0D, 0.08D, 0.0D), 1.2D, 20, ParticleTypes.END_ROD);
        ring(level, mark.add(0.0D, 0.1D, 0.0D), LANCE_BLAST_RADIUS, 30,
                ParticleTypes.DRAGON_BREATH);
        ring(level, mark.add(0.0D, 0.12D, 0.0D), LANCE_BLAST_RADIUS * 1.55D, 34,
                ParticleTypes.ELECTRIC_SPARK);
    }

    // ---- 核心坍缩:45% 血量强制转入虚空预兆阶段 ----

    private void startCoreBreak(ServerLevel serverLevel) {
        this.phaseTransitionTriggered = true;
        this.enraged = true;
        this.attackMode = AttackMode.CORE_BREAK;
        this.attackTicks = 44;
        this.rendLines.clear();
        this.constellationMarks.clear();
        this.lanceMarks.clear();
        this.triggerAnim("attack_controller", "core_break");
        this.broadcastNearby(serverLevel, Component.translatable("message.unknown_echoes.void_prophet.omen"));
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.4F, 1.7F);
    }

    private void tickCoreBreak(ServerLevel serverLevel) {
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        double collapse = Math.max(0.8D, this.attackTicks * 0.15D);
        if ((this.attackTicks & 1) == 0) {
            ring(serverLevel, core, collapse, 30, ParticleTypes.REVERSE_PORTAL);
            ring(serverLevel, core, collapse * 0.65D, 22, ParticleTypes.DRAGON_BREATH);
            spiral(serverLevel, core, 2.6D, (44 - this.attackTicks) * 0.8D, ParticleTypes.SCULK_SOUL);
        }
        if (this.attackTicks % 3 == 0 && this.attackTicks > 8) {
            this.pullPlayers(serverLevel, core, 12.0D, 0.12D);
        }
        if (this.attackTicks > 6 && this.attackTicks % 4 == 0) {
            this.drawCalibrationBreakGuidance(serverLevel, core);
        }
        if (this.attackTicks == 6) {
            this.damagePlayersNear(serverLevel, core, 10.0D,
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F,
                    1.25D, 0.5D, false);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    core.x, core.y, core.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 180, 2.8D, 3.0D, 2.8D, 0.45D);
            for (int band = 1; band <= 4; band++) {
                ring(serverLevel, this.position().add(0.0D, 0.2D, 0.0D),
                        band * 2.7D, 36, ParticleTypes.SOUL_FIRE_FLAME);
            }
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.HOSTILE, 2.7F, 0.45F);
        }
        if (this.attackTicks == 2) {
            this.blinkCooldown = 18;
            this.rendCooldown = 28;
            this.gravityCooldown = 65;
            this.constellationCooldown = 12;
            this.lanceCooldown = 38;
            this.broadcastNearby(serverLevel, Component.translatable(
                    "message.unknown_echoes.void_prophet.phase_open"));
        }
    }

    private void drawCalibrationBreakGuidance(ServerLevel level, Vec3 bossCore) {
        if (this.arenaCenter == null) {
            return;
        }
        Vec3 altarCore = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 0.65D, 0.0D);
        for (BlockPos nodePos : GravityCoreAltarBlock.findCalibrationNodes(level, this.arenaCenter)) {
            Vec3 node = Vec3.atCenterOf(nodePos).add(0.0D, 0.45D, 0.0D);
            BossFx.guidanceMarker(level, node, ParticleTypes.END_ROD);
            BossFx.guidanceFlow(level, node, altarCore,
                    ParticleTypes.GLOW, ParticleTypes.ENCHANT, this.tickCount);
        }
        BossFx.guidanceMarker(level, altarCore, ParticleTypes.ELECTRIC_SPARK);
        BossFx.guidanceFlow(level, altarCore, bossCore,
                ParticleTypes.ENCHANT, ParticleTypes.ELECTRIC_SPARK, this.tickCount);
    }

    private void spawnAmbientVoid(ServerLevel serverLevel) {
        if (this.tickCount % (this.enraged ? 3 : 5) != 0) {
            return;
        }
        double angle = this.tickCount * 0.22D;
        double radius = 1.5D + Math.sin(this.tickCount * 0.1D) * 0.3D;
        serverLevel.sendParticles(this.enraged ? ParticleTypes.DRAGON_BREATH : ParticleTypes.PORTAL,
                this.getX() + Math.cos(angle) * radius,
                this.getY() + 1.2D + (this.tickCount % 30) * this.getBbHeight() / 30.0D,
                this.getZ() + Math.sin(angle) * radius,
                2, 0.08D, 0.12D, 0.08D, 0.01D);
    }

    private void pullPlayers(ServerLevel level, Vec3 center, double radius, double strength) {
        AABB area = new AABB(center.x - radius, center.y - 5.0D, center.z - radius,
                center.x + radius, center.y + 5.0D, center.z + radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator())) {
            Vec3 toward = center.subtract(player.position()).multiply(1.0D, 0.35D, 1.0D);
            if (toward.lengthSqr() < 1.0E-4D || toward.horizontalDistanceSqr() > radius * radius) {
                continue;
            }
            Vec3 force = toward.normalize().scale(strength);
            player.setDeltaMovement(player.getDeltaMovement().add(force));
            player.hurtMarked = true;
        }
    }

    private void damagePlayersNear(ServerLevel level, Vec3 center, double radius, float damage,
                                   double horizontalForce, double verticalForce, boolean pull) {
        AABB area = new AABB(center.x - radius, center.y - 4.0D, center.z - radius,
                center.x + radius, center.y + 4.0D, center.z + radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator())) {
            Vec3 delta = player.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (delta.horizontalDistanceSqr() > radius * radius) {
                continue;
            }
            player.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 direction = delta.lengthSqr() > 1.0E-4D
                    ? delta.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
            double sign = pull ? -1.0D : 1.0D;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    direction.x * horizontalForce * sign, verticalForce,
                    direction.z * horizontalForce * sign));
            player.hurtMarked = true;
        }
    }

    private void broadcastNearby(ServerLevel level, Component message) {
        double radius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(message, true);
            }
        }
    }

    private static Vec3 rotateHorizontal(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D,
                vector.x * sin + vector.z * cos).normalize();
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int points,
                             net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void spiral(ServerLevel level, Vec3 center, double radius, double spin,
                               net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < 10; i++) {
            double angle = spin + i * Math.PI * 0.4D;
            double localRadius = radius * (0.35D + i / 14.0D);
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * localRadius,
                    center.y - 1.6D + i * 0.34D,
                    center.z + Math.sin(angle) * localRadius,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void drawLine(ServerLevel level, Vec3 from, Vec3 to, double spacing,
                                 net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = from.add(direction.scale(distance));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof Enemy) {
            return false;
        }
        if (!this.level().isClientSide && this.attackMode == AttackMode.CORE_BREAK
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.protectedMessageCooldown <= 0 && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.void_prophet.phase_locked"), true);
                this.protectedMessageCooldown = 40;
            }
            return false;
        }
        if (!this.level().isClientSide && !this.phaseTransitionTriggered
                && BossPhaseGate.shouldCapMechanicDamage(source)) {
            float phaseFloor = this.getMaxHealth() * ENRAGE_THRESHOLD;
            amount = BossPhaseGate.capDamage(this.getHealth(), amount, phaseFloor);
            if (amount <= 0.0F) {
                return false;
            }
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            this.registerParticipant(attacker);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + this.getBbHeight() * 0.58D, this.getZ(),
                    9, 0.65D, 1.2D, 0.65D, 0.08D);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double rewardRadius = ServerConfig.BOSS_REWARD_RADIUS.get();
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isSpectator() && this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= rewardRadius * rewardRadius) {
                EchoAbilityManager.markBossDefeated(player, EchoPermission.VOID_PROPHET_ID);
                player.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.void_prophet.defeated"));
            }
        }
        Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FLASH,
                core.x, core.y, core.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                core.x, core.y, core.z, 240, 3.0D, 3.2D, 3.0D, 0.5D);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                core.x, core.y, core.z, 80, 2.0D, 2.5D, 2.0D, 0.2D);
        for (int band = 1; band <= 5; band++) {
            ring(serverLevel, core, band * 1.8D, 36, ParticleTypes.DRAGON_BREATH);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 2.6F, 1.35F);
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void registerParticipant(ServerPlayer player) {
        if (!player.isSpectator() && (this.participants.contains(player.getUUID())
                || this.participants.size() < BossNbtData.MAX_PARTICIPANTS)) {
            this.participants.add(player.getUUID());
        }
    }

    @Override
    public void checkDespawn() {
        // 主线 Boss 不自然消失。
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
        tag.putBoolean("PhaseTransitionTriggered", this.phaseTransitionTriggered);
        tag.putBoolean("PhaseTransitionPending", this.attackMode == AttackMode.CORE_BREAK);
        tag.putInt("BlinkCooldown", this.blinkCooldown);
        tag.putInt("RendCooldown", this.rendCooldown);
        tag.putInt("GravityCooldown", this.gravityCooldown);
        tag.putInt("ConstellationCooldown", this.constellationCooldown);
        tag.putInt("LanceCooldown", this.lanceCooldown);
        tag.putInt("ScaledParticipants", this.scaledParticipants);
        if (this.arenaCenter != null) {
            tag.putIntArray("ArenaCenter", new int[]{
                    this.arenaCenter.getX(), this.arenaCenter.getY(), this.arenaCenter.getZ()});
        }
        net.minecraft.nbt.ListTag participantsTag = new net.minecraft.nbt.ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        boolean phaseTransitionPending = tag.getBoolean("PhaseTransitionPending");
        this.phaseTransitionTriggered = tag.getBoolean("PhaseTransitionTriggered")
                && !phaseTransitionPending;
        this.blinkCooldown = clampTimer(tag.getInt("BlinkCooldown"), 220);
        this.rendCooldown = clampTimer(tag.getInt("RendCooldown"), 240);
        this.gravityCooldown = clampTimer(tag.getInt("GravityCooldown"), 300);
        this.constellationCooldown = clampTimer(tag.getInt("ConstellationCooldown"), 240);
        this.lanceCooldown = tag.contains("LanceCooldown")
                ? clampTimer(tag.getInt("LanceCooldown"), 260) : 90;
        this.scaledParticipants = BossNbtData.clampParticipantScale(tag.getInt("ScaledParticipants"));
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        BlockPos loadedArena = BossNbtData.loadPositionNear(tag, "ArenaCenter", this.blockPosition(), 64, 32);
        if (loadedArena != null) {
            this.bindArena(loadedArena);
        }
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    private static int clampTimer(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("spell_sway", SPELL_SWAY_ANIM)
                .triggerableAnim("short_hop", SHORT_HOP_ANIM)
                .triggerableAnim("spatial_rend", SPATIAL_REND_ANIM)
                .triggerableAnim("gravity_recoil", GRAVITY_RECOIL_ANIM)
                .triggerableAnim("core_break", CORE_BREAK_ANIM)
                .triggerableAnim("void_lance", VOID_LANCE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private enum AttackMode {
        NONE,
        SHORT_HOP,
        SPATIAL_REND,
        GRAVITY_RECOIL,
        CONSTELLATION,
        VOID_LANCE,
        CORE_BREAK
    }

    private record LineRift(Vec3 origin, Vec3 direction, double length) {
    }

    private static final class VoidScar {
        private final Vec3 position;
        private int ticks;

        private VoidScar(Vec3 position, int ticks) {
            this.position = position;
            this.ticks = ticks;
        }
    }
}
