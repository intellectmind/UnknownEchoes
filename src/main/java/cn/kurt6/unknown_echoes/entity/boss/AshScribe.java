package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.deep.VoidStabilizerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import java.util.List;

public class AshScribe extends MiniBossEntity implements PhaseGatedBoss {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("ash_scribe");

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.ash_scribe.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.ash_scribe.walk");
    private static final RawAnimation ATTACK =
            RawAnimation.begin().then("animation.ash_scribe.attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TRANSCRIBE =
            RawAnimation.begin().then("animation.ash_scribe.transcribe", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation INKCUT =
            RawAnimation.begin().then("animation.ash_scribe.inkcut", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PAGESTORM =
            RawAnimation.begin().then("animation.ash_scribe.pagestorm", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BURN =
            RawAnimation.begin().then("animation.ash_scribe.burn", Animation.LoopType.PLAY_ONCE);

    private static final int ARENA_RADIUS = 15;
    private static final int BURN_DURATION = 44;
    private static final int REWRITE_DURATION = 40;
    private static final int INKCUT_DURATION = 18;
    private static final int PAGESTORM_DURATION = 32;
    private static final int CATEGORY_COUNT = 4;
    private static final int MAX_SAMPLES = 12;
    /** 净化稳定器相对刷怪器的三角布置;结构旋转由四个方向覆盖。 */
    private static final int[][] EXTINGUISHING_OFFSETS = {{0, -7}, {6, 4}, {-6, 4}};
    private static final int EXTINGUISHING_Y_OFFSET = -2;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final int[] damageSamples = new int[CATEGORY_COUNT];
    private final java.util.Set<BlockPos> extinguishingMechanisms = new java.util.HashSet<>();
    private AttackMode attackMode = AttackMode.NONE;
    private Vec3 attackOrigin = Vec3.ZERO;
    private Vec3 attackTarget = Vec3.ZERO;
    private boolean burnTriggered;
    private boolean burnPending;
    private int burnTicks;
    private int attackTicks;
    private int inkcutCooldown = 65;
    private int pagestormCooldown = 130;
    private int rewriteCooldown = 110;
    private int transcribeCooldown;

    private enum AttackMode {
        NONE, REWRITE, INKCUT, PAGESTORM
    }

    private enum DamageCategory {
        MELEE(0), PROJECTILE(1), MAGIC(2), EXPLOSION(3);

        private final int index;

        DamageCategory(int index) {
            this.index = index;
        }
    }

    public AshScribe(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        this.xpReward = 55;
        this.moveControl = new FlyingMoveControl(this, 14, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 330.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FLYING_SPEED, 0.48D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 42.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.75D));
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
        return this.burnPending || this.burnTicks > 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.ash_scribe.burning";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        if (this.burnPending) {
            Vec3 core = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
            for (BlockPos pos : nearbyExtinguishingMechanisms(level)) {
                Vec3 stabilizer = Vec3.atCenterOf(pos).add(0.0D, 0.55D, 0.0D);
                MiniBossFx.objectiveMarker(level, stabilizer, this.fxTheme(), level.getGameTime());
                MiniBossFx.objectiveLink(level, stabilizer, core, this.fxTheme(), level.getGameTime());
            }
        }
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantAshScribe(this, firstKill);
    }

    @Override
    public float phaseDamageFloor() {
        return this.burnTriggered ? 0.0F : this.getMaxHealth() * 0.25F;
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

        this.setNoGravity(true);
        if (this.inkcutCooldown > 0) this.inkcutCooldown--;
        if (this.pagestormCooldown > 0) this.pagestormCooldown--;
        if (this.rewriteCooldown > 0) this.rewriteCooldown--;
        if (this.transcribeCooldown > 0) this.transcribeCooldown--;

        if (!this.burnTriggered && !this.burnPending
                && this.getHealth() <= this.getMaxHealth() * 0.25F) {
            this.burnPending = true;
            this.extinguishingMechanisms.clear();
            this.broadcastHint(serverLevel, "message.unknown_echoes.ash_scribe.phase_locked");
        }
        if (this.burnPending) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35D));
            return;
        }
        if (this.burnTicks > 0) {
            tickBurn(serverLevel);
            return;
        }
        if (this.attackMode != AttackMode.NONE) {
            tickAttack(serverLevel);
            return;
        }

        tickCombatFlight();
        selectAttack(serverLevel);
    }

    private void tickCombatFlight() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.tickCount % 12 != 0) {
            return;
        }
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        double angle = this.tickCount * 0.045D;
        Vec3 desired = target.position().add(Math.cos(angle) * 5.5D, 3.2D,
                Math.sin(angle) * 5.5D);
        Vec3 horizontal = desired.subtract(anchor).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > (ARENA_RADIUS - 2.0D) * (ARENA_RADIUS - 2.0D)) {
            horizontal = horizontal.normalize().scale(ARENA_RADIUS - 2.0D);
            desired = anchor.add(horizontal.x, 3.2D, horizontal.z);
        }
        this.moveControl.setWantedPosition(desired.x, desired.y, desired.z,
                this.burnTriggered ? 1.25D : 0.95D);
    }

    private void startBurn(ServerLevel level) {
        this.burnPending = false;
        this.burnTriggered = true;
        this.burnTicks = BURN_DURATION;
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        clearAdaptations();
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", "burn");
        this.broadcastHint(level, "message.unknown_echoes.ash_scribe.burn");
        level.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.HOSTILE, 2.1F, 0.65F);
        BossFx.cinematicImpact(level, this.position().add(0.0D, 1.0D, 0.0D),
                28.0D, 0.58F, 8, 0xFF9E4D);
    }

    /** 净化机关由玩家操作后调用；三座稳定器完成才解除燃烧护甲。 */
    public void onExtinguishingMechanismActivated(ServerLevel level, BlockPos pos) {
        if (!this.burnPending || !this.isAlive()
                || this.arenaAnchor().distManhattan(pos) > 48
                || !(level.getBlockState(pos).getBlock() instanceof VoidStabilizerBlock)
                || !nearbyExtinguishingMechanisms(level).contains(pos)) {
            return;
        }
        this.extinguishingMechanisms.add(pos.immutable());
        if (this.extinguishingMechanisms.size() >= 3) {
            this.extinguishingMechanisms.clear();
            this.startBurn(level);
        }
    }

    public boolean isAwaitingExtinguishingMechanism() {
        return this.burnPending;
    }

    private java.util.List<BlockPos> nearbyExtinguishingMechanisms(ServerLevel level) {
        BlockPos center = this.arenaAnchor();
        List<BlockPos> best = List.of();
        for (int rotation = 0; rotation < 4; rotation++) {
            List<BlockPos> candidates = new ArrayList<>(EXTINGUISHING_OFFSETS.length);
            for (int[] offset : EXTINGUISHING_OFFSETS) {
                int[] rotated = rotateOffset(offset[0], offset[1], rotation);
                BlockPos pos = center.offset(rotated[0], EXTINGUISHING_Y_OFFSET, rotated[1]);
                if (level.hasChunkAt(pos)
                        && level.getBlockState(pos).getBlock() instanceof VoidStabilizerBlock) {
                    candidates.add(pos.immutable());
                }
            }
            if (candidates.size() > best.size()) {
                best = candidates;
            }
        }
        return best;
    }

    private static int[] rotateOffset(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    private void tickBurn(ServerLevel level) {
        this.burnTicks--;
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.55D).add(0.0D, 0.025D, 0.0D));
        if (this.tickCount % 3 == 0) {
            level.sendParticles(this.fxTheme().glint(),
                    this.getX(), this.getY() + 1.2D, this.getZ(),
                    4, 0.6D, 0.8D, 0.6D, 0.02D);
        }
        if (this.burnTicks == 1) {
            this.inkcutCooldown = 15;
            this.pagestormCooldown = 45;
            level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE, 1.8F, 0.75F);
        }
    }

    private void selectAttack(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distanceSq = this.distanceToSqr(target);
        if (this.rewriteCooldown <= 0 && totalSamples() > 0
                && distanceSq <= 20.0D * 20.0D) {
            startAttack(level, AttackMode.REWRITE, REWRITE_DURATION, target, "transcribe");
        } else if (this.pagestormCooldown <= 0 && distanceSq <= 18.0D * 18.0D) {
            startAttack(level, AttackMode.PAGESTORM, PAGESTORM_DURATION, target, "pagestorm");
        } else if (this.inkcutCooldown <= 0 && distanceSq <= 22.0D * 22.0D
                && this.hasLineOfSight(target)) {
            startAttack(level, AttackMode.INKCUT, INKCUT_DURATION, target, "inkcut");
        }
    }

    private void startAttack(ServerLevel level, AttackMode mode, int duration,
                             LivingEntity target, String animation) {
        this.attackMode = mode;
        this.attackTicks = duration;
        this.attackOrigin = this.position().add(0.0D, this.getBbHeight() * 0.65D, 0.0D);
        this.attackTarget = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", animation);
        level.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.HOSTILE, 1.5F, mode == AttackMode.INKCUT ? 1.25F : 0.7F);
    }

    private void tickAttack(ServerLevel level) {
        this.attackTicks--;
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.65D));
        if (this.attackMode == AttackMode.REWRITE) {
            if (this.tickCount % 4 == 0) {
                MiniBossFx.objectiveLink(level, this.position().add(0.0D, 1.0D, 0.0D),
                        this.attackTarget, this.fxTheme(), level.getGameTime());
            }
            if (this.attackTicks == 12) {
                resolveRewrite(level);
            }
        } else if (this.attackMode == AttackMode.INKCUT) {
            Vec3 direction = this.attackTarget.subtract(this.attackOrigin).normalize();
            double distance = this.attackOrigin.distanceTo(this.attackTarget);
            if (this.tickCount % 3 == 0) {
                MiniBossFx.pierceBeam(level, this.attackOrigin, direction,
                        this.fxTheme(), distance);
            }
            if (this.attackTicks == 5) {
                resolveInkcut(level);
            }
        } else if (this.attackMode == AttackMode.PAGESTORM) {
            double progress = (PAGESTORM_DURATION - this.attackTicks) / (double) PAGESTORM_DURATION;
            if (this.tickCount % 3 == 0) {
                MiniBossFx.impactZone(level, this.attackTarget, this.fxTheme(), 5.0D, progress);
            }
            if (this.attackTicks == 11) {
                resolvePagestorm(level);
            }
        }
        if (this.attackTicks <= 0) {
            this.attackMode = AttackMode.NONE;
        }
    }

    private void resolveRewrite(ServerLevel level) {
        this.rewriteCooldown = this.burnTriggered ? 75 : 125;
        DamageCategory category = mostRecordedCategory();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.52F;
        switch (category) {
            case MELEE -> {
                Vec3 center = this.position().add(0.0D, 0.6D, 0.0D);
                damageRewriteArea(level, center, 4.2D, damage, 0.8D);
                MiniBossFx.groundRing(level, center, this.fxTheme(), 4.8D);
                MiniBossFx.coneSpray(level, center, this.getLookAngle(), this.fxTheme(), 4.5D, Math.PI / 3.0D);
            }
            case PROJECTILE -> {
                Vec3 origin = this.position().add(0.0D, 1.1D, 0.0D);
                Vec3 direction = this.attackTarget.subtract(origin);
                if (direction.lengthSqr() > 1.0E-4D) {
                    direction = direction.normalize();
                    if (this.tickCount % 2 == 0) {
                        MiniBossFx.pierceBeam(level, origin, direction, this.fxTheme(), 14.0D);
                    }
                }
                damageRewriteArea(level, this.attackTarget, 2.2D, damage, 0.25D);
            }
            case MAGIC -> {
                damageRewriteArea(level, this.attackTarget, 3.2D, damage, 0.45D);
                MiniBossFx.impactZone(level, this.attackTarget, this.fxTheme(), 3.2D, 1.0D);
            }
            case EXPLOSION -> {
                damageRewriteArea(level, this.attackTarget, 4.8D, damage * 1.1F, 1.0D);
                MiniBossFx.groundRing(level, this.attackTarget, this.fxTheme(), 4.8D);
            }
        }
        BossFx.cinematicImpact(level, this.attackTarget, 22.0D, 0.3F, 4,
                MiniBossFxTheme.packedCore(this.fxTheme()));
        level.playSound(null, BlockPos.containing(this.attackTarget), ModSounds.ASH_SCRIBE_ATTACK.get(),
                SoundSource.HOSTILE, 1.7F, 1.0F);
    }

    private void damageRewriteArea(ServerLevel level, Vec3 center, double radius,
                                   float damage, double pushStrength) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius, 3.0D, radius),
                entity -> entity != this && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Enemy))) {
            Vec3 offset = victim.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (offset.lengthSqr() > radius * radius) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                MiniBossFx.hitMark(level, victim, this.fxTheme());
                if (offset.lengthSqr() > 1.0E-4D) {
                    Vec3 push = offset.normalize().scale(pushStrength);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.25D, push.z));
                    victim.hurtMarked = true;
                }
            }
        }
    }

    private void resolveInkcut(ServerLevel level) {
        this.inkcutCooldown = this.burnTriggered ? 58 : 88;
        Vec3 direction = this.attackTarget.subtract(this.attackOrigin);
        double length = Math.min(22.0D, direction.length());
        if (length < 0.1D) {
            return;
        }
        direction = direction.normalize();
        Vec3 end = this.attackOrigin.add(direction.scale(length));
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.72F;
        AABB bounds = new AABB(this.attackOrigin, end).inflate(1.15D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != this && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Enemy))) {
            Vec3 center = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
            Vec3 offset = center.subtract(this.attackOrigin);
            double along = offset.dot(direction);
            double radius = 1.15D + victim.getBbWidth() * 0.5D;
            if (along >= 0.0D && along <= length
                    && offset.subtract(direction.scale(along)).lengthSqr() <= radius * radius) {
                if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                    MiniBossFx.hitMark(level, victim, this.fxTheme());
                    if (this.burnTriggered) {
                        victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 60));
                    }
                }
            }
        }
        level.playSound(null, BlockPos.containing(end), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.8F, 0.6F);
    }

    private void resolvePagestorm(ServerLevel level) {
        this.pagestormCooldown = this.burnTriggered ? 105 : 165;
        Vec3 center = this.position().add(0.0D, 0.9D, 0.0D);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.62F;
        int rays = this.burnTriggered ? 12 : 9;
        double range = 10.0D;
        double phase = this.tickCount * 0.15D;
        for (int ray = 0; ray < rays; ray++) {
            double angle = phase + Math.PI * 2.0D * ray / rays;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            if (this.tickCount % 2 == 0) {
                MiniBossFx.pierceBeam(level, center, direction, this.fxTheme(), range);
            }
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(range, 3.5D, range),
                entity -> entity != this && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Enemy))) {
            Vec3 offset = victim.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            double distance = offset.length();
            if (distance > range || distance < 0.2D) {
                continue;
            }
            double victimAngle = Math.atan2(offset.z, offset.x) - phase;
            double sector = Math.PI * 2.0D / rays;
            double nearest = Math.abs(Math.IEEEremainder(victimAngle, sector));
            if (Math.sin(nearest) * distance <= 1.25D + victim.getBbWidth() * 0.5D
                    && victim.hurt(this.damageSources().mobAttack(this), damage)) {
                MiniBossFx.hitMark(level, victim, this.fxTheme());
                Vec3 push = offset.normalize().scale(0.65D);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.3D, push.z));
                victim.hurtMarked = true;
                if (this.burnTriggered) {
                    victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 45));
                }
            }
        }
        MiniBossFx.groundRing(level, center, this.fxTheme(), range);
        BossFx.cinematicImpact(level, center, 30.0D, 0.52F, 7,
                MiniBossFxTheme.packedCore(this.fxTheme()));
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 2.0F, 0.8F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        DamageCategory category = classifyPlayerDamage(source);
        float adjusted = amount;
        if (category != null && !this.burnTriggered) {
            adjusted *= 1.0F - adaptationReduction(category);
        }
        boolean hurt = super.hurt(source, adjusted);
        if (hurt && !this.level().isClientSide && category != null && !this.burnTriggered) {
            recordDamageCategory(category);
        }
        return hurt;
    }

    private DamageCategory classifyPlayerDamage(DamageSource source) {
        if (!(source.getEntity() instanceof Player)) {
            return null;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return DamageCategory.EXPLOSION;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return DamageCategory.PROJECTILE;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return DamageCategory.MAGIC;
        }
        return DamageCategory.MELEE;
    }

    private float adaptationReduction(DamageCategory category) {
        int tier = Math.min(4, this.damageSamples[category.index] / 3);
        return Math.min(0.45F, tier * 0.12F);
    }

    private int totalSamples() {
        int total = 0;
        for (int samples : this.damageSamples) {
            total += samples;
        }
        return total;
    }

    private DamageCategory mostRecordedCategory() {
        DamageCategory result = DamageCategory.MELEE;
        for (DamageCategory category : DamageCategory.values()) {
            if (this.damageSamples[category.index] > this.damageSamples[result.index]) {
                result = category;
            }
        }
        return result;
    }

    private void recordDamageCategory(DamageCategory category) {
        int previousTier = this.damageSamples[category.index] / 3;
        this.damageSamples[category.index] = Math.min(MAX_SAMPLES,
                this.damageSamples[category.index] + 1);
        int newTier = this.damageSamples[category.index] / 3;
        if (newTier > previousTier && this.transcribeCooldown <= 0
                && this.attackMode == AttackMode.NONE && this.burnTicks <= 0) {
            this.transcribeCooldown = 40;
            this.triggerAnim("attack_controller", "transcribe");
            if (this.level() instanceof ServerLevel level) {
                this.broadcastHint(level, "message.unknown_echoes.ash_scribe.transcribe");
                level.playSound(null, this.blockPosition(), ModSounds.ASH_SCRIBE_TRANSCRIBE.get(),
                        SoundSource.HOSTILE, 1.5F, 1.0F);
            }
        }
    }

    private void clearAdaptations() {
        for (int i = 0; i < this.damageSamples.length; i++) {
            this.damageSamples[i] = 0;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.triggerAnim("attack_controller", "attack");
            if (target instanceof LivingEntity living && this.level() instanceof ServerLevel level) {
                MiniBossFx.hitMark(level, living, this.fxTheme());
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
        return ModSounds.ASH_SCRIBE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ASH_SCRIBE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ASH_SCRIBE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BurnTriggered", this.burnTriggered);
        tag.putBoolean("BurnPending", this.burnPending);
        tag.putInt("BurnTicks", this.burnTicks);
        tag.putIntArray("DamageSamples", this.damageSamples);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.burnTriggered = tag.getBoolean("BurnTriggered");
        this.burnPending = tag.getBoolean("BurnPending");
        this.burnTicks = BossNbtData.clampTimer(tag.getInt("BurnTicks"), BURN_DURATION);
        int[] saved = tag.getIntArray("DamageSamples");
        for (int i = 0; i < this.damageSamples.length && i < saved.length; i++) {
            this.damageSamples[i] = Math.max(0, Math.min(MAX_SAMPLES, saved[i]));
        }
        if (this.burnTriggered) {
            clearAdaptations();
        }
        this.attackMode = AttackMode.NONE;
        this.attackTicks = 0;
        this.setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("transcribe", TRANSCRIBE)
                .triggerableAnim("inkcut", INKCUT)
                .triggerableAnim("pagestorm", PAGESTORM)
                .triggerableAnim("burn", BURN));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
