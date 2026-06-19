package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/**
 * 残钟看守:残钟废塔 Mini Boss(残钟荒原,设计文档 10.5)。
 *
 * 机制(计时窗破甲,设计红线 #4——窗外攻击不破甲):
 * - 看守平时披甲受护(未破防)。周期性举起钟摆"鸣钟蓄力"(toll 读条)——这就是计时窗:
 *   窗内被玩家击中 → 踉跄破甲(stagger),护甲脱落 7 秒(可全额输出);放任读条结束 →
 *   鸣钟轰出冲击波(减速 + 伤害),回到披甲。
 * - 钟锤过顶砸(hammer):近战重击带溅射 + 击退。
 * - 延迟齿轮爆(gearburst):向目标周围埋 2-3 枚齿轮雷,引信到点爆裂(范围伤害)。
 * 结算沿用 MiniBossEntity 个人首杀/重复发放(MiniBossRewardTable)。
 */
public class BrokenBellKeeper extends MiniBossEntity {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("broken_bell_keeper");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.broken_bell_keeper.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.broken_bell_keeper.walk");
    private static final RawAnimation HAMMER =
            RawAnimation.begin().then("animation.broken_bell_keeper.hammer", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation TOLL =
            RawAnimation.begin().then("animation.broken_bell_keeper.toll", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GEARBURST =
            RawAnimation.begin().then("animation.broken_bell_keeper.gearburst", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STAGGER =
            RawAnimation.begin().then("animation.broken_bell_keeper.stagger", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final int ARENA_RADIUS = 14;
    private static final int EXPOSED_WINDOW = 140;  // 破甲暴露 7s
    private static final int WINDUP_TICKS = 50;     // 鸣钟蓄力(计时窗)2.5s
    private static final int GEAR_FUSE = 26;

    private int exposedTicks = 0;     // >0 = 护甲脱落,可全额输出
    private int windupTicks = 0;      // >0 = 鸣钟计时窗(可被打断破甲)
    private int tollCooldown = 120;
    private int hammerCooldown = 40;
    private int gearburstCooldown = 160;

    private final List<GearBomb> gearBombs = new ArrayList<>();

    private static final class GearBomb {
        final Vec3 pos;
        int fuse;
        GearBomb(Vec3 pos, int fuse) {
            this.pos = pos;
            this.fuse = fuse;
        }
    }

    public BrokenBellKeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        this.xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 290.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.ARMOR, 11.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
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
        // 仅在破甲暴露窗口内可全额输出;其余(含鸣钟计时窗)都披甲受护
        return this.exposedTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.broken_bell_keeper.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantBrokenBellKeeper(this, player, firstKill);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        // 计时窗内被玩家击中 → 踉跄破甲
        if (result && !this.level().isClientSide && this.windupTicks > 0 && this.exposedTicks <= 0
                && source.getEntity() instanceof ServerPlayer
                && this.level() instanceof ServerLevel serverLevel) {
            this.windupTicks = 0;
            this.triggerAnim("attack_controller", "stagger");
            this.enterExposed(serverLevel);
        }
        return result;
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tollCooldown > 0) this.tollCooldown--;
        if (this.hammerCooldown > 0) this.hammerCooldown--;
        if (this.gearburstCooldown > 0) this.gearburstCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.SCRAPE,
                        this.getX(), this.getY() + 1.4, this.getZ(), 4, 0.7, 0.8, 0.7, 0.0);
            }
        } else if (this.windupTicks > 0) {
            this.tickWindup(serverLevel);
        } else if (this.getTarget() != null) {
            if (this.tollCooldown <= 0) {
                this.startWindup(serverLevel);
            } else {
                this.tickSpecialAttacks(serverLevel);
            }
        }

        this.tickGearBombs(serverLevel);
    }

    /** 鸣钟蓄力:计时窗内绕身金色火花环(可被击中打断破甲);窗满则轰出冲击波。 */
    private void tickWindup(ServerLevel serverLevel) {
        double spin = this.windupTicks * 0.4;
        BossFx.chargeSpiral(serverLevel, this.position().add(0, 0.7, 0), this.tickCount, ParticleTypes.CRIT);
        for (int i = 0; i < 6; i++) {
            double a = spin + Math.PI * 2 * i / 6;
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX() + Math.cos(a) * 2.2, this.getY() + 1.2, this.getZ() + Math.sin(a) * 2.2,
                    1, 0.02, 0.05, 0.02, 0.0);
        }
        if (--this.windupTicks <= 0) {
            this.castToll(serverLevel);
        }
    }

    private void startWindup(ServerLevel serverLevel) {
        this.windupTicks = WINDUP_TICKS;
        this.triggerAnim("attack_controller", "toll");
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0F, 0.5F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.broken_bell_keeper.windup");
    }

    /** 鸣钟冲击波:未被打断时,环形 AoE 伤害 + 减速;之后回到披甲。 */
    private void castToll(ServerLevel serverLevel) {
        this.tollCooldown = 220;
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_ATTACK.get(),
                SoundSource.HOSTILE, 1.6F, 0.6F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(6.0, 2.0, 6.0),
                e -> e != this && !(e instanceof Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
        }
        for (int ring = 1; ring <= 6; ring++) {
            for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18;
                serverLevel.sendParticles(ParticleTypes.POOF,
                        this.getX() + Math.cos(a) * ring, this.getY() + 0.4, this.getZ() + Math.sin(a) * ring,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.1, 0), 6.0, ParticleTypes.END_ROD);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.0, 0), ParticleTypes.END_ROD);
    }

    private void enterExposed(ServerLevel serverLevel) {
        this.exposedTicks = EXPOSED_WINDOW;
        this.tollCooldown = 240;
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_HURT.get(),
                SoundSource.HOSTILE, 1.4F, 0.7F);
        this.broadcastHint(serverLevel, "message.unknown_echoes.broken_bell_keeper.exposed");
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.gearburstCooldown <= 0 && distSq > 16.0 && distSq <= 18.0 * 18.0) {
            this.castGearburst(serverLevel, target);
        }
    }

    /** 延迟齿轮爆:目标周围埋 2-3 枚齿轮雷,引信到点爆裂。 */
    private void castGearburst(ServerLevel serverLevel, LivingEntity target) {
        this.gearburstCooldown = 200;
        this.triggerAnim("attack_controller", "gearburst");
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_ATTACK.get(),
                SoundSource.HOSTILE, 1.2F, 1.1F);
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        int count = 2 + this.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = this.random.nextDouble() * 3.0;
            Vec3 p = target.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            if (p.subtract(anchor).horizontalDistanceSqr() <= ARENA_RADIUS * ARENA_RADIUS) {
                this.gearBombs.add(new GearBomb(p, GEAR_FUSE));
            }
        }
    }

    private void tickGearBombs(ServerLevel serverLevel) {
        if (this.gearBombs.isEmpty()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F;
        this.gearBombs.removeIf(bomb -> {
            bomb.fuse--;
            if (bomb.fuse > 0) {
                if (bomb.fuse % 3 == 0) {
                    double r = 1.6 * bomb.fuse / GEAR_FUSE + 0.3;
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                bomb.pos.x + Math.cos(a) * r, bomb.pos.y + 0.1, bomb.pos.z + Math.sin(a) * r,
                                1, 0.0, 0.0, 0.0, 0.0);
                    }
                    serverLevel.sendParticles(ParticleTypes.CRIT, bomb.pos.x, bomb.pos.y + 0.3, bomb.pos.z,
                            2, 0.1, 0.1, 0.1, 0.0);
                }
                return false;
            }
            // 爆裂
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, bomb.pos.x, bomb.pos.y + 0.5, bomb.pos.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT, bomb.pos.x, bomb.pos.y + 0.5, bomb.pos.z, 14, 0.5, 0.4, 0.5, 0.1);
            BossFx.impactBurst(serverLevel, bomb.pos.add(0, 0.45, 0), ParticleTypes.CRIT);
            serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(bomb.pos),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.2F);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(bomb.pos.x - 2.5, bomb.pos.y - 1.5, bomb.pos.z - 2.5,
                            bomb.pos.x + 2.5, bomb.pos.y + 2.0, bomb.pos.z + 2.5),
                    e -> e != this && !(e instanceof Enemy))) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
                Vec3 away = victim.position().subtract(bomb.pos).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.5, 0.35, dir.z * 0.5));
                    victim.hurtMarked = true;
                }
            }
            return true;
        });
    }

    /** 近战 = 钟锤过顶砸:命中带溅射 + 击退。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.triggerAnim("attack_controller", "hammer");
            serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_ATTACK.get(),
                    SoundSource.HOSTILE, 1.2F, 0.8F);
            if (this.hammerCooldown <= 0 && target instanceof LivingEntity primary) {
                this.hammerCooldown = 30;
                float splash = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        primary.getBoundingBox().inflate(2.0),
                        e -> e != this && e != primary && !(e instanceof Enemy))) {
                    victim.hurt(this.damageSources().mobAttack(this), splash);
                }
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.4, 0.3, 0.4, 0.1);
                BossFx.sweepArc(serverLevel, this.position().add(0, 0.8, 0),
                        target.position().subtract(this.position()), 3.2, ParticleTypes.CRIT);
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.CRIT);
            }
        }
        return hit;
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= radius * radius) {
                player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BROKEN_BELL_KEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BROKEN_BELL_KEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BROKEN_BELL_KEEPER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ExposedTicks", this.exposedTicks);
        tag.putInt("WindupTicks", this.windupTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.exposedTicks = tag.getInt("ExposedTicks");
        this.windupTicks = tag.getInt("WindupTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("hammer", HAMMER)
                .triggerableAnim("toll", TOLL)
                .triggerableAnim("gearburst", GEARBURST)
                .triggerableAnim("stagger", STAGGER));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
