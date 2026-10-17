package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.deep.GravityFieldCoreBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
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
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class InvertedGatekeeper extends MiniBossEntity implements PhaseGatedBoss {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("inverted_gatekeeper");
    private static final EntityDataAccessor<Boolean> DATA_INVERTED =
            SynchedEntityData.defineId(InvertedGatekeeper.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.inverted_gatekeeper.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.inverted_gatekeeper.walk");
    private static final RawAnimation INVERTED =
            RawAnimation.begin().thenLoop("animation.inverted_gatekeeper.inverted");
    private static final RawAnimation ATTACK =
            RawAnimation.begin().then("animation.inverted_gatekeeper.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SLAM =
            RawAnimation.begin().then("animation.inverted_gatekeeper.slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation THROW =
            RawAnimation.begin().then("animation.inverted_gatekeeper.throw", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PULSE =
            RawAnimation.begin().then("animation.inverted_gatekeeper.pulse", Animation.LoopType.PLAY_ONCE);

    private static final int ARENA_RADIUS = 18;
    private static final int EXPOSED_WINDOW = 200;
    private static final int SLAM_DURATION = 30;
    private static final int THROW_DURATION = 22;
    private static final int PULSE_DURATION = 26;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private AttackMode attackMode = AttackMode.NONE;
    private Vec3 attackTarget = Vec3.ZERO;
    private boolean inversionTriggered;
    private int exposedTicks;
    private int attackTicks;
    private int slamCooldown = 80;
    private int throwCooldown = 120;
    private int pulseCooldown = 180;
    private BlockPos gravityCorePos;
    private long gravityCoreNextScanGameTime = Long.MIN_VALUE;

    private enum AttackMode {
        NONE, SLAM, THROW, PULSE
    }

    public InvertedGatekeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        this.xpReward = 55;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 380.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.92D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_INVERTED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
        return this.inversionTriggered && this.exposedTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.inverted_gatekeeper.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        BlockPos core = findGravityCore(level, false);
        if (core == null) {
            return;
        }
        Vec3 from = Vec3.atCenterOf(core).add(0.0D, 0.55D, 0.0D);
        Vec3 to = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        MiniBossFx.objectiveMarker(level, from, this.fxTheme(), level.getGameTime());
        MiniBossFx.objectiveLink(level, from, to, this.fxTheme(), level.getGameTime());
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantInvertedGatekeeper(this, firstKill);
    }

    @Override
    public float phaseDamageFloor() {
        return this.inversionTriggered ? 0.0F : this.getMaxHealth() * 0.5F;
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

        if (!this.inversionTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            enterInvertedPhase(serverLevel);
        }
        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.throwCooldown > 0) this.throwCooldown--;
        if (this.pulseCooldown > 0) this.pulseCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            this.setNoGravity(false);
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + 1.5D, this.getZ(),
                        5, 0.8D, 0.8D, 0.8D, 0.02D);
            }
            if (this.exposedTicks == 0 && this.isAlive()) {
                returnToInvertedOrbit(serverLevel);
            }
        } else if (this.inversionTriggered) {
            tickInvertedOrbit(serverLevel);
        }

        if (this.attackMode != AttackMode.NONE) {
            tickAttack(serverLevel);
        } else {
            selectAttack(serverLevel);
        }
    }

    private void enterInvertedPhase(ServerLevel level) {
        this.inversionTriggered = true;
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.entityData.set(DATA_INVERTED, true);
        this.broadcastHint(level, "message.unknown_echoes.inverted_gatekeeper.inverted");
        level.playSound(null, this.blockPosition(), ModSounds.INVERTED_GATEKEEPER_INVERT.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
        BossFx.pillar(level, this.position(), 8.0D, ParticleTypes.REVERSE_PORTAL);
        BossFx.cinematicImpact(level, this.position().add(0, 1.0, 0),
                32.0D, 0.72F, 9, 0x9F75FF);
    }

    private void tickInvertedOrbit(ServerLevel level) {
        this.setNoGravity(true);
        this.getNavigation().stop();
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        double angle = this.tickCount * 0.022D;
        Vec3 desired = anchor.add(Math.cos(angle) * 6.0D, 7.5D,
                Math.sin(angle) * 6.0D);
        Vec3 correction = desired.subtract(this.position()).scale(0.075D);
        Vec3 current = this.getDeltaMovement().scale(0.72D).add(correction);
        this.setDeltaMovement(current.x, Math.max(-0.28D, Math.min(0.28D, current.y)), current.z);
        this.hurtMarked = true;
        if (this.tickCount % 3 == 0) {
            BossFx.flightTrail(level, this.position().add(0.0D, 0.9D, 0.0D),
                    this.getDeltaMovement(), 0.55D, ParticleTypes.ELECTRIC_SPARK);
        }
    }

    private void breakGravityArmor(ServerLevel level, BlockPos corePos) {
        BlockState state = level.getBlockState(corePos);
        if (state.hasProperty(GravityFieldCoreBlock.ACTIVE)) {
            level.setBlock(corePos, state.setValue(GravityFieldCoreBlock.ACTIVE, false), 3);
        }
        this.exposedTicks = EXPOSED_WINDOW;
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        this.setNoGravity(false);
        this.entityData.set(DATA_INVERTED, false);
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        this.teleportTo(anchor.x, anchor.y, anchor.z);
        CombatFx.spatial(level, anchor.add(0, this.getBbHeight() * 0.5, 0), this.getBbWidth() * 0.7 + 0.3,
                this.getBbHeight(), this.fxTheme().coreDust(1), true, 14);
        this.setDeltaMovement(0.0D, -0.65D, 0.0D);
        this.hurtMarked = true;
        this.broadcastHint(level, "message.unknown_echoes.inverted_gatekeeper.exposed");
        level.playSound(null, corePos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 2.2F, 0.65F);
        BossFx.converge(level, this.position().add(0.0D, 1.2D, 0.0D),
                7.0D, 32, ParticleTypes.ELECTRIC_SPARK);
        BossFx.groundShockwave(level, anchor, 7.0D, ParticleTypes.REVERSE_PORTAL);
        BossFx.cinematicImpact(level, anchor.add(0.0D, 0.8D, 0.0D),
                34.0D, 0.82F, 10, 0xB18BFF);
    }

    /** 玩家关闭重力核心后调用，开启一次暴露窗口。 */
    public void onGravityCoreDeactivated(ServerLevel level, BlockPos corePos) {
        if (!this.inversionTriggered || this.exposedTicks > 0 || !this.isAlive()
                || this.arenaAnchor().distManhattan(corePos) > 48
                || !isBoundGravityCore(level, corePos)) {
            return;
        }
        this.breakGravityArmor(level, corePos);
    }

    private boolean isBoundGravityCore(ServerLevel level, BlockPos corePos) {
        BlockPos bound = this.gravityCorePos;
        if (bound == null) {
            bound = findGravityCore(level, false);
        }
        return bound != null
                && bound.equals(corePos)
                && level.hasChunkAt(corePos)
                && level.getBlockState(corePos).is(ModBlocks.GRAVITY_FIELD_CORE.get());
    }

    private void returnToInvertedOrbit(ServerLevel level) {
        this.setNoGravity(true);
        this.entityData.set(DATA_INVERTED, true);
        this.throwCooldown = Math.min(this.throwCooldown, 35);
        this.pulseCooldown = Math.min(this.pulseCooldown, 65);
        level.playSound(null, this.blockPosition(), ModSounds.INVERTED_GATEKEEPER_ORBIT.get(),
                SoundSource.HOSTILE, 1.6F, 1.0F);
        BossFx.chargeSpiral(level, this.position().add(0.0D, 1.2D, 0.0D),
                this.tickCount, ParticleTypes.REVERSE_PORTAL);
    }

    private BlockPos findGravityCore(ServerLevel level, boolean activeOnly) {
        long now = level.getGameTime();
        if (this.gravityCorePos != null) {
            if (!level.hasChunkAt(this.gravityCorePos)) {
                return null;
            }
            BlockState cached = level.getBlockState(this.gravityCorePos);
            if (cached.is(ModBlocks.GRAVITY_FIELD_CORE.get())) {
                if (!activeOnly || cached.getValue(GravityFieldCoreBlock.ACTIVE)) {
                    return this.gravityCorePos;
                }

                this.gravityCoreNextScanGameTime = Math.max(
                        this.gravityCoreNextScanGameTime, now + 20L);
                return null;
            }
            this.gravityCorePos = null;
        }
        if (now < this.gravityCoreNextScanGameTime) {
            return null;
        }
        if (!isScanAreaLoaded(level, this.arenaAnchor(), 14)) {

            this.gravityCoreNextScanGameTime = now + 20L;
            return null;
        }
        BlockPos anchor = this.arenaAnchor();
        for (BlockPos cursor : BlockPos.betweenClosed(anchor.offset(-14, -4, -14),
                anchor.offset(14, 12, 14))) {
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.GRAVITY_FIELD_CORE.get())
                    && (!activeOnly || state.getValue(GravityFieldCoreBlock.ACTIVE))) {
                this.gravityCorePos = cursor.immutable();
                return this.gravityCorePos;
            }
        }

        this.gravityCoreNextScanGameTime = now + 600L;
        return null;
    }

    private static boolean isScanAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, radius);
    }

    private void selectAttack(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distanceSq = this.distanceToSqr(target);
        if (this.inversionTriggered && this.exposedTicks <= 0) {
            if (this.pulseCooldown <= 0 && distanceSq <= 14.0D * 14.0D) {
                startAttack(level, AttackMode.PULSE, PULSE_DURATION, target.position(), "pulse");
            } else if (this.throwCooldown <= 0 && distanceSq <= 24.0D * 24.0D) {
                startAttack(level, AttackMode.THROW, THROW_DURATION, target.position(), "throw");
            }
        } else if (this.slamCooldown <= 0 && distanceSq <= 7.0D * 7.0D) {
            startAttack(level, AttackMode.SLAM, SLAM_DURATION, target.position(), "slam");
        } else if (this.throwCooldown <= 0 && distanceSq <= 20.0D * 20.0D) {
            startAttack(level, AttackMode.THROW, THROW_DURATION, target.position(), "throw");
        }
    }

    private void startAttack(ServerLevel level, AttackMode mode, int duration,
                             Vec3 target, String animation) {
        this.attackMode = mode;
        this.attackTicks = duration;
        this.attackTarget = target;
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", animation);
        switch (mode) {
            case SLAM -> this.playSound(ModSounds.INVERTED_GATEKEEPER_ATTACK_SLAM_WARN.get(), 1.6F, 1.0F);
            case THROW -> this.playSound(ModSounds.INVERTED_GATEKEEPER_ATTACK_THROW_WARN.get(), 1.6F, 1.0F);
            case PULSE -> this.playSound(ModSounds.INVERTED_GATEKEEPER_ATTACK_PULSE_WARN.get(), 1.6F, 1.0F);
            default -> { }
        }
    }

    private void tickAttack(ServerLevel level) {
        this.getNavigation().stop();
        this.attackTicks--;
        switch (this.attackMode) {
            case SLAM -> {
                BossFx.lockSigil(level, this.attackTarget, 3.8D,
                        (SLAM_DURATION - this.attackTicks) / (double) SLAM_DURATION,
                        this.tickCount, ParticleTypes.ELECTRIC_SPARK);
                if (this.attackTicks == 11) resolveSlam(level);
            }
            case THROW -> {
                Vec3 origin = this.position().add(0.0D, 1.5D, 0.0D);
                BossFx.guidanceLink(level, origin, this.attackTarget.add(0.0D, 0.3D, 0.0D),
                        ParticleTypes.REVERSE_PORTAL);
                if (this.attackTicks == 7) resolveThrow(level);
            }
            case PULSE -> {
                BossFx.chargeSpiral(level, this.position().add(0.0D, 1.2D, 0.0D),
                        PULSE_DURATION - this.attackTicks, ParticleTypes.ELECTRIC_SPARK);
                if (this.attackTicks == 9) resolvePulse(level);
            }
            default -> {
            }
        }
        if (this.attackTicks <= 0) {
            this.attackMode = AttackMode.NONE;
        }
    }

    private void resolveSlam(ServerLevel level) {
        this.slamCooldown = this.inversionTriggered ? 105 : 135;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85F;
        damageArea(level, this.attackTarget, 4.1D, 2.2D, damage, 0.85D, 0.38D);
        BossFx.groundShockwave(level, this.attackTarget, 5.5D, ParticleTypes.ELECTRIC_SPARK);
        BossFx.impactBurst(level, this.attackTarget.add(0.0D, 0.5D, 0.0D),
                ParticleTypes.REVERSE_PORTAL);
        BossFx.cinematicImpact(level, this.attackTarget, 26.0D, 0.42F, 5, 0xA98BFF);
        level.playSound(null, BlockPos.containing(this.attackTarget), ModSounds.INVERTED_GATEKEEPER_ATTACK.get(),
                SoundSource.HOSTILE, 2.2F, 1.0F);
    }

    private void resolveThrow(ServerLevel level) {
        this.throwCooldown = this.inversionTriggered ? 85 : 125;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F;
        damageArea(level, this.attackTarget, 2.8D, 3.0D, damage, 0.35D, 0.95D);
        BossFx.comet(level, this.attackTarget, 10.0D, 1.0D, ParticleTypes.REVERSE_PORTAL);
        BossFx.pillar(level, this.attackTarget, 7.0D, ParticleTypes.ELECTRIC_SPARK);
        BossFx.cinematicImpact(level, this.attackTarget, 22.0D, 0.3F, 4, 0xA98BFF);
        level.playSound(null, BlockPos.containing(this.attackTarget), ModSounds.INVERTED_GATEKEEPER_ATTACK_THROW_RELEASE.get(),
                SoundSource.HOSTILE, 1.5F, 1.0F);
    }

    private void resolvePulse(ServerLevel level) {
        this.pulseCooldown = this.inversionTriggered ? 135 : 190;
        Vec3 center = this.position().add(0.0D, 0.6D, 0.0D);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
        damageArea(level, center, 8.0D, 4.5D, damage, 1.15D, 0.25D);
        for (double radius = 2.0D; radius <= 8.0D; radius += 2.0D) {
            BossFx.ring(level, center, radius, 28, 0.15D, ParticleTypes.REVERSE_PORTAL);
        }
        BossFx.areaBurst(level, center, ParticleTypes.ELECTRIC_SPARK);
        BossFx.cinematicImpact(level, center, 30.0D, 0.64F, 8, 0x9F75FF);
        level.playSound(null, this.blockPosition(), ModSounds.INVERTED_GATEKEEPER_ATTACK_PULSE_RELEASE.get(),
                SoundSource.HOSTILE, 1.8F, 1.0F);
    }

    private void damageArea(ServerLevel level, Vec3 center, double horizontalRadius,
                            double verticalRadius, float damage, double push, double lift) {
        AABB bounds = new AABB(center, center).inflate(horizontalRadius, verticalRadius, horizontalRadius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != this && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Enemy))) {
            if (victim.distanceToSqr(center) > horizontalRadius * horizontalRadius) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = victim.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-4D) {
                    Vec3 direction = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            direction.x * push, lift, direction.z * push));
                    victim.hurtMarked = true;
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.triggerAnim("attack_controller", "attack");
            if (!this.level().isClientSide) {
                this.playSound(ModSounds.INVERTED_GATEKEEPER_ATTACK_MELEE.get(), 1.1F, 1.0F);
            }
            if (this.level() instanceof ServerLevel level) {
                BossFx.sweepArc(level, this.position().add(0.0D, 1.2D, 0.0D),
                        this.getLookAngle(), 3.5D, ParticleTypes.ELECTRIC_SPARK);
            }
        }
        return hit;
    }

    private void broadcastHint(ServerLevel level, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                candidate -> !candidate.isSpectator()
                        && candidate.distanceToSqr(this) <= radius * radius)) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.INVERTED_GATEKEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.INVERTED_GATEKEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.INVERTED_GATEKEEPER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("InversionTriggered", this.inversionTriggered);
        tag.putInt("ExposedTicks", this.exposedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.inversionTriggered = tag.getBoolean("InversionTriggered");
        this.exposedTicks = BossNbtData.clampTimer(tag.getInt("ExposedTicks"), EXPOSED_WINDOW);
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        this.setNoGravity(this.inversionTriggered && this.exposedTicks <= 0);
        this.entityData.set(DATA_INVERTED, this.inversionTriggered && this.exposedTicks <= 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.entityData.get(DATA_INVERTED)) {
                return state.setAndContinue(INVERTED);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("slam", SLAM)
                .triggerableAnim("throw", THROW)
                .triggerableAnim("pulse", PULSE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
    @Override
    public int musicStage() {
        return this.exposedTicks > 0 ? 2 : this.inversionTriggered ? 1 : 0;
    }
}
