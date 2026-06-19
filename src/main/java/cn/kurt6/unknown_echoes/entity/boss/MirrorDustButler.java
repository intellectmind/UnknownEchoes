package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
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
import java.util.UUID;

/**
 * 镜尘执事:镜湖神殿隐藏房区域守护者 / Mini Boss(V0.6C,设计文档 10.4.2)。
 *
 * 机制:开场分裂为三个镜尘像(真身 + 两个假像),只有真身可被真正伤害——
 * - 真假同型同貌(同一实体类型),真假身份只存服务端字段,绝不写同步 DataAccessor(红线 #9);
 * - 假像生命 1:被击碎爆出镜尘并短暂遮挡攻击者视野,不开输出窗口;
 * - 真身被命中 → 3 秒受击窗口(全额伤害),窗口结束强制洗牌换位;
 * - 碎像阶段(假像全碎):换位更频繁,但输出窗口更长;尘幕期间假像重新生成;
 * - 真视回响玩家:洗牌后看到真身短暂微光(逐玩家粒子,不发光轮廓、不同步数据)。
 * 攻击:镜尘飞刺(三像同步弹幕,真身弹道色深 + 碎镜音)/ 换位斩(落位近身斩,碎镜线预警)/
 *       尘幕(遮蔽 + 假像再生)/ 礼杖点镜(慢速镜尘球,命中缓速 + 迷向粒子——V0.6C 降级版,
 *       不劫持客户端输入)。
 * 结算(个人):首杀 = 镜纹拓片 + 幻象尘 + 真视研究点 +1 + 隐藏房线索记录;重复 = 普通材料。
 */
public class MirrorDustButler extends MiniBossEntity {

    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("mirror_dust_butler");

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_butler.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_dust_butler.walk");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().then("animation.mirror_dust_butler.attack", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final int ARENA_RADIUS = 8;
    /** 三个镜尘站位(相对场地锚点),与隐藏房碎镜地台对应。 */
    private static final int[][] STATIONS = {{0, -4}, {4, 3}, {-4, 3}};

    /** 受击窗口:洗牌阶段 3 秒;碎像阶段 4 秒。 */
    private static final int EXPOSED_TICKS = 60;
    private static final int EXPOSED_TICKS_SHATTERED = 80;

    private static final int BOLT_TELEGRAPH_TICKS = 16;
    private static final double BOLT_RANGE = 12.0D;
    private static final int VEIL_DURATION_TICKS = 50;

    // ---- 服务端权威状态(刻意不进 SynchedEntityData,红线 #9) ----
    /** true = 假像(生命表现 1:任何玩家伤害直接击碎)。 */
    private boolean isImage = false;
    /** 假像所属的真身。 */
    private UUID ownerUuid = null;
    /** 真身的两个假像。 */
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
    protected void registerGoals() {
        // 刻意不给近战追击:真假三像都只在站位附近游走,近身威胁来自换位斩
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5D, 60));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
        // 受击窗口外受机制保护;窗口内全额伤害
        return this.exposedTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.mirror_dust_butler.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantMirrorDustButler(this, player, firstKill);
    }

    // ---- 真假像管理(服务端权威) ----

    public boolean isImage() {
        return this.isImage;
    }

    /** 把本体标记为假像(由真身分裂时调用,只在服务端发生)。 */
    private void markAsImage(MirrorDustButler owner) {
        this.isImage = true;
        this.ownerUuid = owner.getUUID();
        this.xpReward = 0;
    }

    private boolean isShatteredPhase(ServerLevel serverLevel) {
        return this.aliveImages(serverLevel).isEmpty();
    }

    private List<MirrorDustButler> aliveImages(ServerLevel serverLevel) {
        List<MirrorDustButler> alive = new ArrayList<>();
        for (UUID uuid : this.imageUuids) {
            if (serverLevel.getEntity(uuid) instanceof MirrorDustButler image && image.isAlive()) {
                alive.add(image);
            }
        }
        return alive;
    }

    /** 开场分裂:真身入场即生成两个假像并洗牌一次。 */
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
        Vec3 spawn = this.position().add(
                (this.random.nextDouble() - 0.5) * 2, 0, (this.random.nextDouble() - 0.5) * 2);
        image.moveTo(spawn.x, spawn.y, spawn.z, this.getYRot(), 0);
        image.markAsImage(this);
        image.setPersistenceRequired();
        image.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntity(image);
        this.imageUuids.add(image.getUUID());
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                spawn.x, spawn.y + 1.2, spawn.z, 12, 0.4, 0.6, 0.4, 0.02);
    }

    /**
     * 洗牌:真身与假像在三个站位间随机换位。
     * 站位分配只在服务端掷随机;落位瞬间真身脚下有碎镜声(低血时更明显,观察补偿),
     * 真视玩家额外看到真身短暂微光(逐玩家粒子)。
     */
    private void shuffle(ServerLevel serverLevel) {
        List<MirrorDustButler> bodies = new ArrayList<>();
        bodies.add(this);
        bodies.addAll(this.aliveImages(serverLevel));
        // 服务端洗牌站位
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
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    body.getX(), body.getY() + 1.0, body.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
            body.teleportTo(x, anchor.getY() + 1.0, z);
            body.getNavigation().stop();
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    x, anchor.getY() + 2.0, z, 8, 0.3, 0.6, 0.3, 0.02);
        }
        // 真身落位补偿线索:碎镜声(低血更响)
        boolean lowHealth = this.getHealth() < this.getMaxHealth() * 0.4F;
        serverLevel.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, lowHealth ? 1.6F : 0.7F, 1.4F);
        this.sendTrueSightHint(serverLevel, 16);
        // 落位近身威胁:换位斩预警
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) <= 3.5 * 3.5) {
            this.swapStrikeTicks = 10;
            this.triggerAnim("attack_controller", "attack");
        }
        this.shuffleCooldown = this.isShatteredPhase(serverLevel)
                ? (lowHealth ? 120 : 160) : (lowHealth ? 160 : 240);
    }

    /** 真视微光:只对持真视回响的玩家发送真身位置的短暂粒子(红线 #9:不发同步数据)。 */
    private void sendTrueSightHint(ServerLevel serverLevel, int particles) {
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= 32 * 32
                    && EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                serverLevel.sendParticles(player, ParticleTypes.END_ROD, false,
                        this.getX(), this.getEyeY() + 0.3, this.getZ(),
                        particles, 0.25, 0.4, 0.25, 0.0);
            }
        }
    }

    // ---- 伤害入口:真假分流 ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return super.hurt(source, amount);
        }
        // 假像:任何玩家伤害直接击碎——爆镜尘 + 攻击者短暂视野遮挡,不开输出窗口
        if (this.isImage) {
            if (source.getEntity() instanceof ServerPlayer attacker) {
                this.shatterImage(serverLevel, attacker);
                return true;
            }
            return false;
        }
        boolean wasGuarded = this.isDamageGuarded();
        boolean hurt = super.hurt(source, amount);
        // 真身被玩家命中:打开受击窗口(窗口结束强制洗牌)
        if (hurt && wasGuarded && source.getEntity() instanceof ServerPlayer) {
            this.exposedTicks = this.isShatteredPhase(serverLevel)
                    ? EXPOSED_TICKS_SHATTERED : EXPOSED_TICKS;
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_HURT.get(), SoundSource.HOSTILE, 1.4F, 1.2F);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getEyeY(), this.getZ(), 10, 0.4, 0.5, 0.4, 0.1);
        }
        return hurt;
    }

    /** 假像被击碎:爆出镜尘,攻击者短暂黑暗,本体消散(不走 die,不触发结算)。 */
    private void shatterImage(ServerLevel serverLevel, ServerPlayer attacker) {
        serverLevel.sendParticles(ParticleTypes.ASH,
                this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.5, 0.8, 0.5, 0.08);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_SHATTER.get(), SoundSource.HOSTILE, 1.2F, 1.1F);
        attacker.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
        attacker.displayClientMessage(
                Component.translatable("message.unknown_echoes.mirror_dust_butler.image"), true);
        this.discard();
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 假像:无机制循环,只在本体消失时自清理
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
            // 受击窗口:裂镜微光外露
            if (this.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getEyeY(), this.getZ(), 2, 0.3, 0.5, 0.3, 0.02);
            }
            if (--this.exposedTicks == 0) {
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
                && this.aliveImages(serverLevel).size() < 2) {
            this.castDustVeil(serverLevel);
        } else if (this.orbCooldown <= 0 && distSq >= 6.0 * 6.0
                && this.hasLineOfSight(target)) {
            this.castMirrorOrb(target);
        }
    }

    /** 镜尘飞刺:三像同步前摇(0.8 秒),真身弹道色深 + 碎镜音(观察线索)。 */
    private void startBoltVolley(ServerLevel serverLevel, LivingEntity target) {
        this.boltDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        this.boltTelegraphTicks = BOLT_TELEGRAPH_TICKS;
        this.boltCooldown = 130;
        this.getNavigation().stop();
        this.triggerAnim("attack_controller", "attack");
        for (MirrorDustButler image : this.aliveImages(serverLevel)) {
            image.triggerAnim("attack_controller", "attack");
        }
    }

    private void tickBoltTelegraph(ServerLevel serverLevel) {
        if (this.boltDir == null) {
            this.boltTelegraphTicks = 0;
            return;
        }
        // 三像同步聚尘前摇:真假外观一致,只有弹道释放瞬间有色差与音差
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

    /** 弹幕释放:真身弹道伤害 + 深色粒子 + 碎镜音;假像弹道纯演出。 */
    private void fireBoltVolley(ServerLevel serverLevel) {
        if (!this.isAlive() || this.boltDir == null) {
            return;
        }
        LivingEntity target = this.getTarget();
        // 假像弹道:浅色,无伤害
        for (MirrorDustButler image : this.aliveImages(serverLevel)) {
            Vec3 dir = target != null
                    ? target.getEyePosition().subtract(image.getEyePosition()).normalize()
                    : this.boltDir;
            drawBoltTrail(serverLevel, image.getEyePosition(), dir, ParticleTypes.END_ROD);
        }
        // 真身弹道:深色 + 碎镜音 + 真实伤害(观察色差与音源即可锁定真身)
        drawBoltTrail(serverLevel, this.getEyePosition(), this.boltDir, ParticleTypes.ASH);
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_SHATTER.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 start = this.getEyePosition();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BOLT_RANGE),
                e -> e != this && !(e instanceof Enemy) && e.isAlive())) {
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

    /** 换位斩:洗牌落位的近身斩击,地面碎镜线预警 10 tick 后命中。 */
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
                    e -> e != this && !(e instanceof Enemy) && e.isAlive())) {
                Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
                if (toVictim.lengthSqr() > 1.0E-4 && facing.dot(toVictim.normalize()) < 0.3) {
                    continue;
                }
                victim.hurt(this.damageSources().mobAttack(this), damage);
            }
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_ATTACK.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            BossFx.sweepArc(serverLevel, this.position().add(0, 0.7, 0), facing, 2.8, ParticleTypes.ELECTRIC_SPARK);
            BossFx.impactBurst(serverLevel, this.position().add(facing.scale(1.8)).add(0, 1.0, 0), ParticleTypes.ASH);
        }
    }

    /** 尘幕:短暂遮蔽视野,假像趁机重新生成;真视玩家看到更短的微光残影。 */
    private void castDustVeil(ServerLevel serverLevel) {
        this.veilCooldown = 320;
        this.veilTicks = VEIL_DURATION_TICKS;
        this.triggerAnim("attack_controller", "attack");
        serverLevel.playSound(null, this.blockPosition(),
                ModSounds.MIRROR_DUST_BUTLER_AMBIENT.get(), SoundSource.HOSTILE, 1.4F, 0.7F);
        for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(ARENA_RADIUS + 4))) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, VEIL_DURATION_TICKS, 0));
        }
        int missing = 2 - this.aliveImages(serverLevel).size();
        for (int i = 0; i < missing; i++) {
            this.spawnImage(serverLevel);
        }
        this.shuffle(serverLevel);
        // 尘幕中的微光残影:比洗牌提示更短促
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.2, 0), ARENA_RADIUS * 0.45, ParticleTypes.ASH);
        this.sendTrueSightHint(serverLevel, 5);
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
        this.triggerAnim("attack_controller", "attack");
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
        if (serverLevel.getBlockState(blockPos).isSolidRender(serverLevel, blockPos)) {
            this.orbTicks = 0;
            return;
        }
        for (Player victim : serverLevel.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(this.orbPos, this.orbPos).inflate(1.0))) {
            victim.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            serverLevel.sendParticles(ParticleTypes.ASH,
                    victim.getX(), victim.getEyeY(), victim.getZ(), 16, 0.4, 0.4, 0.4, 0.05);
            this.orbTicks = 0;
            break;
        }
        if (this.orbTicks <= 0) {
            this.orbPos = null;
            this.orbVel = null;
        }
    }

    // ---- 体征:假像不进 Boss 血条、不结算、不重开 ----

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        if (this.isImage) {
            return; // 假像不显示 Boss 血条(否则血条数量直接暴露真身)
        }
        super.startSeenByPlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        // 假像理论上只会被 shatterImage 移除;兜底:die 不触发结算/重开
        if (this.isImage) {
            this.discard();
            return;
        }
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 死亡演出:三像同碎,镜尘散场
            for (MirrorDustButler image : this.aliveImages(serverLevel)) {
                serverLevel.sendParticles(ParticleTypes.ASH,
                        image.getX(), image.getY() + 1.0, image.getZ(), 24, 0.5, 0.8, 0.5, 0.06);
                image.discard();
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.2, this.getZ(), 30, 1.0, 1.0, 1.0, 0.06);
            serverLevel.playSound(null, this.blockPosition(),
                    ModSounds.MIRROR_DUST_BUTLER_DEATH.get(), SoundSource.HOSTILE, 1.6F, 0.9F);
            double radius = settlementRadius();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.mirror_dust_butler.defeated"));
                }
            }
        }
    }

    // ---- 音效 ----

    @Override
    protected SoundEvent getAmbientSound() {
        // 真假像环境音一致,音效差异只出现在真身弹道(碎镜音)
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

    // ---- NBT ----

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
        this.exposedTicks = tag.getInt("ExposedTicks");
        this.ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        this.imageUuids.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("ImageUuids",
                net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            this.imageUuids.add(net.minecraft.nbt.NbtUtils.loadUUID(entry));
        }
        if (this.isImage) {
            this.xpReward = 0;
        }
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
