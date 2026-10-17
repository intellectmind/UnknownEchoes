package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.entity.ai.CirclePreferredRangeGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MirrorDustButler extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("mirror_dust_butler");

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_butler.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_butler.walk");
    private static final RawAnimation EXPOSED_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_butler.exposed");
    /** 镜尘飞刺:0.8 秒聚尘前摇后齐射,与 BOLT_TELEGRAPH_TICKS 对齐。 */
    private static final RawAnimation VOLLEY_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_butler.volley", Animation.LoopType.PLAY_ONCE);
    /** 换位斩:命中帧 0.5 秒,与 swapStrikeTicks 对齐。 */
    private static final RawAnimation SWAP_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_butler.swap", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VEIL_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_butler.veil", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ORB_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_butler.orb", Animation.LoopType.PLAY_ONCE);


    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 受击窗口,需同步客户端:exposedTicks 只在服务端递减,动画判定必须走这个标记。 */
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED =
            SynchedEntityData.defineId(MirrorDustButler.class, EntityDataSerializers.BOOLEAN);

    private static final int ARENA_RADIUS = 11;
    /** 三个镜尘站位(相对场地锚点),与隐藏房碎镜地台对应。 */
    private static final int[][] STATIONS = {{0, -4}, {4, 3}, {-4, 3}};

    /** 受击窗口:洗牌阶段 3 秒;碎像阶段 4 秒。 */
    private static final int EXPOSED_TICKS = 60;
    private static final int EXPOSED_TICKS_SHATTERED = 80;

    private static final int BOLT_TELEGRAPH_TICKS = 16;
    private static final double BOLT_RANGE = 12.0D;
    private static final int VEIL_DURATION_TICKS = 50;

    /** true = 假像(生命表现 1:任何玩家伤害直接击碎)。 */
    private boolean isImage = false;
    private boolean cleaningImages = false;
    private UUID ownerUuid = null;
    private final List<UUID> imageUuids = new ArrayList<>();

    private int exposedTicks = 0;
    private int shuffleCooldown = 100;
    private int boltCooldown = 80;
    private int boltTelegraphTicks = 0;
    private Vec3 boltDir = null;
    private int veilCooldown = 300;
    private int veilTicks = 0;
    private int swapStrikeTicks = 0;
    private int orbCooldown = 140;
    private Vec3 orbPos = null;
    private Vec3 orbVel = null;
    private int orbTicks = 0;
    private boolean openedFight = false;

    public MirrorDustButler(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EXPOSED, false);
    }

    public boolean isExposed() {
        return this.entityData.get(DATA_EXPOSED);
    }

    /** 服务端唯一写入点：同时维护计时与同步标记，避免两者漂移。 */
    private void setExposedTicks(int ticks) {
        this.exposedTicks = Math.max(0, ticks);
        this.entityData.set(DATA_EXPOSED, this.exposedTicks > 0);
    }

    /**
     * 没有近战 Goal:执事靠瞬移换位而不是走近平砍。
     * 绕圈半径必须超过镜球的下限(距离平方 36),否则玩家贴脸时它只剩弹幕一招。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new CirclePreferredRangeGoal(this, 6.0D, 8.5D, 0.85D,
                this::isMovementLockedByAttack));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5D, 60));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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

        return this.exposedTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.mirror_dust_butler.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        if (this.isImage) {
            return;
        }
        List<MirrorDustButler> bodies = new ArrayList<>();
        bodies.add(this);
        bodies.addAll(this.aliveImages(level));
        for (MirrorDustButler body : bodies) {
            MiniBossFx.objectiveMarker(level, body.position().add(0, 0.9, 0),
                    this.fxTheme(), level.getGameTime());
        }
        this.sendTrueSightHint(level, 2);
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantMirrorDustButler(this, firstKill);
    }

    public boolean isImage() {
        return this.isImage;
    }

    private void markAsImage(MirrorDustButler owner) {
        this.isImage = true;
        this.ownerUuid = owner.getUUID();
        this.homePos = owner.arenaAnchor().immutable();
        this.restrictTo(this.homePos, ARENA_RADIUS);
        this.xpReward = 0;
    }

    private boolean isShatteredPhase(ServerLevel serverLevel) {
        return this.aliveImages(serverLevel).isEmpty();
    }

    private List<MirrorDustButler> aliveImages(ServerLevel serverLevel) {
        List<MirrorDustButler> alive = new ArrayList<>();
        boolean canPrune = BossNbtData.isHorizontalAreaLoaded(
                serverLevel, this.arenaAnchor(), ARENA_RADIUS + 2);
        for (Iterator<UUID> iterator = this.imageUuids.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            if (serverLevel.getEntity(uuid) instanceof MirrorDustButler image
                    && image.isAlive() && image.isImage
                    && this.getUUID().equals(image.ownerUuid)) {
                alive.add(image);
            } else if (canPrune) {
                iterator.remove();
            }
        }
        return alive;
    }

    private void openFight(ServerLevel serverLevel) {
        this.openedFight = true;
        for (int i = 0; i < 2; i++) {
            this.spawnImage(serverLevel);
        }
        this.shuffle(serverLevel);
    }

    private void spawnImage(ServerLevel serverLevel) {
        MirrorDustButler image = ModEntities.MIRROR_DUST_BUTLER.get().create(serverLevel);
        if (image == null) {
            return;
        }
        Vec3 spawn = this.findImageSpawn(serverLevel, image);
        if (spawn == null) {
            image.discard();
            return;
        }
        image.moveTo(spawn.x, spawn.y, spawn.z, this.getYRot(), 0);
        image.markAsImage(this);
        image.setPersistenceRequired();
        image.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        if (!serverLevel.addFreshEntity(image)) {
            image.discard();
            return;
        }
        this.imageUuids.add(image.getUUID());
        serverLevel.sendParticles(this.fxTheme().glint(),
                spawn.x, spawn.y + 1.2, spawn.z, 12, 0.4, 0.6, 0.4, 0.02);
    }

    private Vec3 findImageSpawn(ServerLevel level, MirrorDustButler image) {
        List<int[]> stations = new ArrayList<>(List.of(STATIONS));
        for (int i = stations.size() - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            int[] swap = stations.get(i);
            stations.set(i, stations.get(j));
            stations.set(j, swap);
        }
        BlockPos anchor = this.arenaAnchor();
        for (int[] station : stations) {
            Vec3 destination = new Vec3(anchor.getX() + 0.5D + station[0],
                    anchor.getY() + 1.0D, anchor.getZ() + 0.5D + station[1]);
            AABB moved = image.getBoundingBox().move(destination.subtract(image.position()));
            if (isSafeStation(level, image, destination) && level.noCollision(image, moved)) {
                return destination;
            }
        }
        return null;
    }

    private void shuffle(ServerLevel serverLevel) {
        List<MirrorDustButler> bodies = new ArrayList<>();
        bodies.add(this);
        bodies.addAll(this.aliveImages(serverLevel));

        List<int[]> stations = new ArrayList<>(List.of(STATIONS));
        for (int i = stations.size() - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            int[] tmp = stations.get(i);
            stations.set(i, stations.get(j));
            stations.set(j, tmp);
        }
        var anchor = this.arenaAnchor();
        for (int i = 0; i < bodies.size() && i < stations.size(); i++) {
            MirrorDustButler body = bodies.get(i);
            int[] station = stations.get(i);
            double x = anchor.getX() + 0.5 + station[0];
            double z = anchor.getZ() + 0.5 + station[1];
            Vec3 destination = new Vec3(x, anchor.getY() + 1.0, z);
            if (!isSafeStation(serverLevel, body, destination)) {
                continue;
            }
            serverLevel.sendParticles(this.fxTheme().glint(),
                    body.getX(), body.getY() + 1.0, body.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
            CombatFx.teleport(serverLevel, body.position(), destination, body.getBbWidth() * 0.65 + 0.3,
                    body.getBbHeight(), this.fxTheme().coreDust(1));
            body.teleportTo(destination.x, destination.y, destination.z);
            body.getNavigation().stop();
            serverLevel.sendParticles(this.fxTheme().glint(),
                    destination.x, destination.y + 1.0, destination.z, 8, 0.3, 0.6, 0.3, 0.02);
        }

        boolean lowHealth = this.getHealth() < this.getMaxHealth() * 0.4F;
        serverLevel.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, lowHealth ? 1.6F : 0.7F, 1.4F);
        this.sendTrueSightHint(serverLevel, 16);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) <= 3.5 * 3.5) {
            this.swapStrikeTicks = 10;
            this.triggerAnim("attack_controller", "swap");
        }
        this.shuffleCooldown = this.isShatteredPhase(serverLevel)
                ? (lowHealth ? 120 : 160) : (lowHealth ? 160 : 240);
    }

    private static boolean isSafeStation(ServerLevel level, MirrorDustButler body,
                                         Vec3 destination) {
        AABB moved = body.getBoundingBox().move(destination.subtract(body.position()));
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
        BlockPos floor = BlockPos.containing(destination.x, destination.y - 0.01D, destination.z);
        return level.getWorldBorder().isWithinBounds(moved)
                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.noBlockCollision(body, moved);
    }

    /** 真视微光:只对持真视回响的玩家发送真身位置的短暂粒子(不发同步数据)。 */
    private void sendTrueSightHint(ServerLevel serverLevel, int particles) {
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(32.0D),
                candidate -> candidate.distanceToSqr(this) <= 32.0D * 32.0D
                        && EchoAbilityManager.hasAbility(candidate, EchoAbilityType.TRUE_SIGHT_ECHO))) {
            serverLevel.sendParticles(player, ParticleTypes.END_ROD, false,
                    this.getX(), this.getEyeY() + 0.3, this.getZ(),
                    particles, 0.25, 0.4, 0.25, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return super.hurt(source, amount);
        }

        if (this.isImage) {

            if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                this.discard();
                return true;
            }
            if (source.getEntity() instanceof ServerPlayer attacker) {
                this.shatterImage(serverLevel, attacker);
                return true;
            }
            return false;
        }
        boolean wasGuarded = this.isDamageGuarded();
        boolean hurt = super.hurt(source, amount);

        if (hurt && wasGuarded && source.getEntity() instanceof ServerPlayer) {
            this.setExposedTicks(this.isShatteredPhase(serverLevel)
                    ? EXPOSED_TICKS_SHATTERED : EXPOSED_TICKS);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_HURT.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getEyeY(), this.getZ(), 10, 0.4, 0.5, 0.4, 0.1);
        }
        return hurt;
    }

    private void shatterImage(ServerLevel serverLevel, ServerPlayer attacker) {
        serverLevel.sendParticles(ParticleTypes.ASH,
                this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.5, 0.8, 0.5, 0.08);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        BossFx.impactBurst(serverLevel, this.position().add(0.0D, 1.0D, 0.0D),
                ParticleTypes.END_ROD);
        BossFx.cinematicImpact(serverLevel, this.position().add(0.0D, 1.0D, 0.0D),
                18.0D, 0.18F, 3, 0xC4F2FF);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_SHATTER.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        attacker.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
        attacker.displayClientMessage(
                Component.translatable("message.unknown_echoes.mirror_dust_butler.image"), true);
        this.discard();
    }

    @Override
    public void aiStep() {
        if (this.shouldStopBossAi()) {
            return;
        }
        if (!this.level().isClientSide && this.isImage
                && this.level() instanceof ServerLevel serverLevel
                && !BossNbtData.isHorizontalAreaLoaded(
                serverLevel, this.arenaAnchor(), ARENA_RADIUS + 2)) {
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.aiStep();
        if (this.isSpawnerBindingPaused()) {
            return;
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.isImage) {
            if (this.tickCount % 20 == 0) {
                Entity owner = this.ownerUuid != null ? serverLevel.getEntity(this.ownerUuid) : null;
                if (!(owner instanceof MirrorDustButler real) || !real.isAlive()) {
                    serverLevel.sendParticles(ParticleTypes.ASH,
                            this.getX(), this.getY() + 1.0, this.getZ(), 16, 0.4, 0.6, 0.4, 0.04);
                    this.discard();
                }
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (!this.openedFight && target != null && target.isAlive()) {
            this.openFight(serverLevel);
        }
        if (this.exposedTicks > 0) {

            if (this.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getEyeY(), this.getZ(), 2, 0.3, 0.5, 0.3, 0.02);
            }
            this.setExposedTicks(this.exposedTicks - 1);
            if (this.exposedTicks == 0) {
                this.shuffle(serverLevel);
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }

        this.tickVeil(serverLevel);
        this.tickOrb(serverLevel);
        this.tickSwapStrike(serverLevel);

        if (this.boltTelegraphTicks > 0) {
            this.tickBoltTelegraph(serverLevel);
            return;
        }

        if (this.shuffleCooldown > 0) this.shuffleCooldown--;
        if (this.boltCooldown > 0) this.boltCooldown--;
        if (this.veilCooldown > 0) this.veilCooldown--;
        if (this.orbCooldown > 0) this.orbCooldown--;

        if (this.shuffleCooldown <= 0) {
            this.shuffle(serverLevel);
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.boltCooldown <= 0 && distSq <= BOLT_RANGE * BOLT_RANGE
                && this.hasLineOfSight(target)) {
            this.startBoltVolley(serverLevel, target);
        } else if (this.veilCooldown <= 0
                && BossNbtData.isHorizontalAreaLoaded(
                serverLevel, this.arenaAnchor(), ARENA_RADIUS + 2)
                && this.aliveImages(serverLevel).size() < 2) {
            this.castDustVeil(serverLevel);
        } else if (this.orbCooldown <= 0 && distSq >= 6.0 * 6.0
                && this.hasLineOfSight(target)) {
            this.castMirrorOrb(target);
        }
    }

    private boolean isMovementLockedByAttack() {
        return this.boltTelegraphTicks > 0 || this.veilTicks > 0
                || this.swapStrikeTicks > 0 || this.orbTicks > 0;
    }

    private void startBoltVolley(ServerLevel serverLevel, LivingEntity target) {
        this.boltDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        this.boltTelegraphTicks = BOLT_TELEGRAPH_TICKS;
        this.boltCooldown = 130;
        this.getNavigation().stop();
        serverLevel.playSound(null, this.arenaAnchor(), ModSounds.MIRROR_DUST_BUTLER_ATTACK_VOLLEY_WARN.get(),
                SoundSource.HOSTILE, 1.1F, 1.0F);
        this.triggerAnim("attack_controller", "volley");
        for (MirrorDustButler image : this.aliveImages(serverLevel)) {
            image.triggerAnim("attack_controller", "volley");
        }
    }

    private void tickBoltTelegraph(ServerLevel serverLevel) {
        if (this.boltDir == null) {
            this.boltTelegraphTicks = 0;
            return;
        }

        List<MirrorDustButler> bodies = new ArrayList<>(this.aliveImages(serverLevel));
        bodies.add(this);
        for (MirrorDustButler body : bodies) {
            serverLevel.sendParticles(ParticleTypes.ASH,
                    body.getX(), body.getEyeY(), body.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
            BossFx.chargeSpiral(serverLevel, body.getEyePosition().add(0, -0.3, 0), this.tickCount, ParticleTypes.ASH);
        }
        if (--this.boltTelegraphTicks == 0) {
            this.fireBoltVolley(serverLevel);
        }
    }

    private void fireBoltVolley(ServerLevel serverLevel) {
        if (!this.isAlive() || this.boltDir == null) {
            return;
        }
        LivingEntity target = this.getTarget();

        for (MirrorDustButler image : this.aliveImages(serverLevel)) {
            Vec3 dir = target != null
                    ? target.getEyePosition().subtract(image.getEyePosition()).normalize()
                    : this.boltDir;
            drawBoltTrail(serverLevel, image.getEyePosition(), dir, ParticleTypes.END_ROD);
        }

        drawBoltTrail(serverLevel, this.getEyePosition(), this.boltDir, ParticleTypes.ASH);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_VOLLEY.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 start = this.getEyePosition();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BOLT_RANGE),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive())) {
            Vec3 toVictim = victim.getEyePosition().subtract(start);
            double along = toVictim.dot(this.boltDir);
            if (along < 0 || along > BOLT_RANGE) {
                continue;
            }
            if (toVictim.subtract(this.boltDir.scale(along)).length() > 1.1) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
        }
        BossFx.impactBurst(serverLevel, this.getEyePosition(), ParticleTypes.ENCHANT);
        Vec3 impact = target != null
                ? target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                : this.getEyePosition();
        BossFx.cinematicImpact(serverLevel, impact, 20.0D, 0.28F, 4, 0xC4F2FF);
        this.boltDir = null;
    }

    private static void drawBoltTrail(ServerLevel serverLevel, Vec3 start, Vec3 dir,
                                      net.minecraft.core.particles.SimpleParticleType particle) {
        for (double d = 0.5; d <= BOLT_RANGE; d += 0.6) {
            Vec3 point = start.add(dir.scale(d));
            serverLevel.sendParticles(particle, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        BossFx.sparkSpray(serverLevel, start, dir, BOLT_RANGE, particle);
    }

    private void tickSwapStrike(ServerLevel serverLevel) {
        if (this.swapStrikeTicks <= 0) {
            return;
        }
        Vec3 facing = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        for (double d = 0.5; d <= 2.5; d += 0.5) {
            Vec3 point = this.position().add(facing.scale(d));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y + 0.1, point.z, 1, 0.1, 0.02, 0.1, 0.0);
        }
        if (--this.swapStrikeTicks == 0 && this.isAlive()) {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(2.8, 1.2, 2.8),
                    e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive())) {
                Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
                if (toVictim.lengthSqr() > 2.8D * 2.8D
                        || (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.3)) {
                    continue;
                }
                victim.hurt(this.damageSources().mobAttack(this), damage);
            }
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_ATTACK.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            BossFx.sweepArc(serverLevel, this.position().add(0, 0.7, 0), facing, 2.8, ParticleTypes.ELECTRIC_SPARK);
            BossFx.impactBurst(serverLevel, this.position().add(facing.scale(1.8)).add(0, 1.0, 0), ParticleTypes.ASH);
            BossFx.cinematicImpact(serverLevel, this.position().add(facing.scale(1.8)),
                    20.0D, 0.3F, 4, 0xC4F2FF);
        }
    }

    private void castDustVeil(ServerLevel serverLevel) {
        this.veilCooldown = 320;
        this.veilTicks = VEIL_DURATION_TICKS;
        this.triggerAnim("attack_controller", "veil");
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_VEIL.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        double veilRadius = ARENA_RADIUS + 4.0D;
        for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(veilRadius),
                candidate -> !candidate.isSpectator()
                        && candidate.distanceToSqr(this) <= veilRadius * veilRadius)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, VEIL_DURATION_TICKS, 0));
        }
        int missing = 2 - this.aliveImages(serverLevel).size();
        for (int i = 0; i < missing; i++) {
            this.spawnImage(serverLevel);
        }
        this.shuffle(serverLevel);

        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), ARENA_RADIUS * 0.45, ParticleTypes.ASH);
        this.sendTrueSightHint(serverLevel, 5);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                30.0D, 0.44F, 6, 0xBDEFFF);
    }

    private void tickVeil(ServerLevel serverLevel) {
        if (this.veilTicks <= 0) {
            return;
        }
        this.veilTicks--;
        if (this.tickCount % 2 == 0) {
            var anchor = this.arenaAnchor();
            serverLevel.sendParticles(ParticleTypes.ASH,
                    anchor.getX() + 0.5, anchor.getY() + 1.5, anchor.getZ() + 0.5,
                    12, ARENA_RADIUS * 0.6, 1.5, ARENA_RADIUS * 0.6, 0.02);
        }
    }

    /** 礼杖点镜:慢速镜尘球,命中缓速 + 迷向粒子(降级实现,不劫持移动输入)。 */
    private void castMirrorOrb(LivingEntity target) {
        this.orbCooldown = 220;
        this.orbPos = this.getEyePosition();
        this.orbVel = target.getEyePosition().subtract(this.orbPos).normalize().scale(0.35);
        this.orbTicks = 70;
        this.triggerAnim("attack_controller", "orb");
        this.playSound(ModSounds.MIRROR_DUST_BUTLER_ATTACK_ORB_RELEASE.get(), 1.1F, 1.0F);
    }

    private void tickOrb(ServerLevel serverLevel) {
        if (this.orbTicks <= 0 || this.orbPos == null || this.orbVel == null) {
            return;
        }
        this.orbTicks--;
        this.orbPos = this.orbPos.add(this.orbVel);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.orbPos.x, this.orbPos.y, this.orbPos.z, 2, 0.1, 0.1, 0.1, 0.0);
        serverLevel.sendParticles(ParticleTypes.ASH,
                this.orbPos.x, this.orbPos.y, this.orbPos.z, 1, 0.05, 0.05, 0.05, 0.0);
        if (this.orbTicks % 3 == 0) {
            BossFx.chargeSpiral(serverLevel, this.orbPos, this.tickCount, ParticleTypes.END_ROD);
        }
        var blockPos = net.minecraft.core.BlockPos.containing(this.orbPos);
        if (!serverLevel.hasChunkAt(blockPos)
                || serverLevel.getBlockState(blockPos).isSolidRender(serverLevel, blockPos)) {
            this.orbTicks = 0;
            return;
        }
        for (Player victim : serverLevel.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(this.orbPos, this.orbPos).inflate(1.0),
                p -> !p.isSpectator())) {
            if (victim.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
            serverLevel.sendParticles(ParticleTypes.ASH,
                    victim.getX(), victim.getEyeY(), victim.getZ(), 16, 0.4, 0.4, 0.4, 0.05);
            BossFx.cinematicImpact(serverLevel,
                    victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D),
                    18.0D, 0.22F, 4, 0xC4F2FF);
            serverLevel.playSound(null, blockPos, ModSounds.MIRROR_DUST_BUTLER_ATTACK_ORB_HIT.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            this.orbTicks = 0;
            break;
        }
        if (this.orbTicks <= 0) {
            this.orbPos = null;
            this.orbVel = null;
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        if (this.isImage) {
            return;
        }
        super.startSeenByPlayer(player);
    }

    @Override
    protected boolean discardOnDeath() {
        return this.isImage;
    }

    @Override
    protected void onDeathCommitted(ServerLevel serverLevel, Vec3 deathPos) {
        this.discardImages(serverLevel, true);
        if (!isCurrentDeathPresentation(serverLevel, deathPos)) return;
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                deathPos.x, deathPos.y + 1.2, deathPos.z, 30, 1.0, 1.0, 1.0, 0.06);
        if (isCurrentDeathPresentation(serverLevel, deathPos)) {
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_DEATH.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        }
    }

    private void discardImages(ServerLevel serverLevel, boolean playFx) {
        if (this.cleaningImages) return;
        this.cleaningImages = true;
        try {
            for (UUID id : List.copyOf(this.imageUuids)) {
                runDeathStep("image cleanup", () -> {
                    var entity = serverLevel.getEntity(id);
                    if (entity == null) return;
                    if (!(entity instanceof MirrorDustButler image) || image.level() != serverLevel
                            || !image.isImage || !this.getUUID().equals(image.ownerUuid)) {
                        this.imageUuids.remove(id);
                        return;
                    }
                    Vec3 imagePos = image.position();
                    try {
                        image.discard();
                    } finally {
                        if (image.isRemoved()) this.imageUuids.remove(id);
                    }
                    if (playFx && image.isRemoved()) {
                        serverLevel.sendParticles(ParticleTypes.ASH,
                                imagePos.x, imagePos.y + 1.0, imagePos.z, 24, 0.5, 0.8, 0.5, 0.06);
                    }
                });
            }
        } finally {
            this.cleaningImages = false;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        try {
            ServerLevel cleanupLevel = this.deathCleanupLevel();
            if ((!this.isImage || this.hasCommittedDeath()) && reason.shouldDestroy() && cleanupLevel != null) {
                runDeathStep("image cleanup on removal", () -> this.discardImages(cleanupLevel, false));
            }
        } finally {
            super.remove(reason);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {

        return ModSounds.MIRROR_DUST_BUTLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIRROR_DUST_BUTLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIRROR_DUST_BUTLER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsImage", this.isImage);
        tag.putBoolean("OpenedFight", this.openedFight);
        tag.putInt("ExposedTicks", this.exposedTicks);
        if (this.ownerUuid != null) {
            tag.putUUID("OwnerUuid", this.ownerUuid);
        }
        net.minecraft.nbt.ListTag images = new net.minecraft.nbt.ListTag();
        for (UUID uuid : this.imageUuids) {
            images.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("ImageUuids", images);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.isImage = tag.getBoolean("IsImage");
        this.openedFight = tag.getBoolean("OpenedFight");
        this.setExposedTicks(
                BossNbtData.clampTimer(tag.getInt("ExposedTicks"), EXPOSED_TICKS_SHATTERED));
        this.ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        BossNbtData.loadUuids(tag, "ImageUuids", this.imageUuids, BossNbtData.MAX_SUMMONS);
        if (this.isImage) {
            this.xpReward = 0;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isExposed()) {
                return state.setAndContinue(EXPOSED_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("volley", VOLLEY_ANIM)
                .triggerableAnim("swap", SWAP_ANIM)
                .triggerableAnim("veil", VEIL_ANIM)
                .triggerableAnim("orb", ORB_ANIM));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
    @Override
    public int musicStage() {
        if (this.isImage && this.level() instanceof ServerLevel level && this.ownerUuid != null
                && level.getEntity(this.ownerUuid) instanceof MirrorDustButler owner && !owner.isImage) {
            return owner.musicStage();
        }
        return this.exposedTicks > 0 ? 1 : this.getHealth() < this.getMaxHealth() * 0.4F ? 2 : 0;
    }
}
