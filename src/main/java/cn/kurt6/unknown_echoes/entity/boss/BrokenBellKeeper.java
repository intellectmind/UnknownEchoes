package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.ai.CirclePreferredRangeGoal;
import cn.kurt6.unknown_echoes.registry.ModSounds;
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
    private static final int EXPOSED_WINDOW = 140;
    private static final int WINDUP_TICKS = 50;
    private static final int GEAR_FUSE = 26;
    /** 鸣钟的水平结算半径。预警环与命中环都画在这个值上，改了两处会一起跟随。 */
    private static final double TOLL_RADIUS = 6.0D;
    /** 齿轮弹的水平结算半径。预警区画在这个值上，画小了玩家会站在圈外仍被炸。 */
    private static final double GEAR_BLAST_RADIUS = 2.5D;
    /** 铁锤溅射半径，以被击者为心。 */
    private static final double HAMMER_SPLASH_RADIUS = 2.0D;

    private int exposedTicks = 0;
    private int windupTicks = 0;
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

    /**
     * 绕圈半径必须落在齿爆的射程区间(距离平方 16 到 18²)内,否则永久贴脸会让齿爆一次都放不出来。
     * 优先级数必须小于近战:MOVE 只能被更小的数抢走,排在后面等于永不触发。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CirclePreferredRangeGoal(this, 5.0D, 8.0D, 0.9D,
                () -> this.windupTicks > 0));
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

        return this.exposedTicks <= 0;
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.broken_bell_keeper.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        if (this.windupTicks <= 0) {
            return;
        }
        // 鸣钟的破防窗口靠"打断蓄力"取得，所以引导画的是钟体本身而不是某个方块。
        MiniBossFx.objectiveMarker(level, this.position().add(0, 1.3, 0),
                this.fxTheme(), level.getGameTime());
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantBrokenBellKeeper(this, firstKill);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide && this.windupTicks > 0 && this.exposedTicks <= 0
                && source.getEntity() instanceof ServerPlayer
                && this.level() instanceof ServerLevel serverLevel) {
            this.windupTicks = 0;
            this.triggerAnim("attack_controller", "stagger");
            this.enterExposed(serverLevel);
        }
        return result;
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
        if (this.tollCooldown > 0) this.tollCooldown--;
        if (this.hammerCooldown > 0) this.hammerCooldown--;
        if (this.gearburstCooldown > 0) this.gearburstCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            if (this.tickCount % 5 == 0) {
                MiniBossFx.breachAura(serverLevel, this, this.fxTheme(),
                        serverLevel.getGameTime());
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

    private void tickWindup(ServerLevel serverLevel) {
        // 预警环画在鸣钟的结算半径上，让玩家知道要退到哪里；同时汇聚表达"正在蓄力"。
        double progress = 1.0D - this.windupTicks / (double) WINDUP_TICKS;
        MiniBossFx.chargeConverge(serverLevel, this.position(), this.fxTheme(),
                TOLL_RADIUS, progress);
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

    private void castToll(ServerLevel serverLevel) {
        this.tollCooldown = 220;
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_TOLL.get(),
                SoundSource.HOSTILE, 1.6F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(TOLL_RADIUS, 2.0, TOLL_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > TOLL_RADIUS * TOLL_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), TOLL_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 0.6, 0),
                30.0D, 0.72F, 9, this.combatImpactColor());
    }

    private void enterExposed(ServerLevel serverLevel) {
        this.exposedTicks = EXPOSED_WINDOW;
        this.tollCooldown = 240;
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_EXPOSED.get(),
                SoundSource.HOSTILE, 1.4F, 1.0F);
        MiniBossFx.breachAura(serverLevel, this, this.fxTheme(), serverLevel.getGameTime());
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                24.0D, 0.42F, 6, this.combatImpactColor());
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

    private void castGearburst(ServerLevel serverLevel, LivingEntity target) {
        this.gearburstCooldown = 200;
        this.triggerAnim("attack_controller", "gearburst");
        serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_GEARBURST.get(),
                SoundSource.HOSTILE, 1.2F, 1.0F);
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
                    // 预警区画在结算半径上，随引信推进收紧；画小了玩家会站在圈外仍被炸。
                    MiniBossFx.impactZone(serverLevel, bomb.pos, this.fxTheme(), GEAR_BLAST_RADIUS,
                            1.0D - bomb.fuse / (double) GEAR_FUSE);
                }
                return false;
            }

            MiniBossFx.groundRing(serverLevel, bomb.pos, this.fxTheme(), GEAR_BLAST_RADIUS);
            BossFx.cinematicImpact(serverLevel, bomb.pos.add(0, 0.45, 0),
                    20.0D, 0.34F, 4, this.combatImpactColor());
            serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(bomb.pos),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.2F);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(bomb.pos.x - GEAR_BLAST_RADIUS, bomb.pos.y - 1.5,
                            bomb.pos.z - GEAR_BLAST_RADIUS,
                            bomb.pos.x + GEAR_BLAST_RADIUS, bomb.pos.y + 2.0,
                            bomb.pos.z + GEAR_BLAST_RADIUS),
                    e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
                if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                    MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                    Vec3 away = victim.position().subtract(bomb.pos).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1.0E-4) {
                        Vec3 dir = away.normalize();
                        victim.setDeltaMovement(victim.getDeltaMovement().add(
                                dir.x * 0.5, 0.35, dir.z * 0.5));
                        victim.hurtMarked = true;
                    }
                }
            }
            return true;
        });
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.triggerAnim("attack_controller", "hammer");
            serverLevel.playSound(null, this.blockPosition(), ModSounds.BROKEN_BELL_KEEPER_ATTACK.get(),
                    SoundSource.HOSTILE, 1.2F, 1.0F);
            if (this.hammerCooldown <= 0 && target instanceof LivingEntity primary) {
                this.hammerCooldown = 30;
                float splash = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        primary.getBoundingBox().inflate(HAMMER_SPLASH_RADIUS),
                        e -> e != this && e != primary && !e.isSpectator() && !(e instanceof Enemy))) {
                    if (victim.hurt(this.damageSources().mobAttack(this), splash)) {
                        MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                    }
                }
                // 溅射是以被击者为心的球，所以环画在它脚下而不是看守脚下。
                MiniBossFx.groundRing(serverLevel, primary.position(), this.fxTheme(),
                        HAMMER_SPLASH_RADIUS);
            }
        }
        return hit;
    }

    private void broadcastHint(ServerLevel serverLevel, String key) {
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(radius),
                candidate -> candidate.distanceToSqr(this) <= radius * radius)) {
            player.displayClientMessage(Component.translatable(key), true);
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
        this.exposedTicks = BossNbtData.clampTimer(tag.getInt("ExposedTicks"), EXPOSED_WINDOW);
        this.windupTicks = BossNbtData.clampTimer(tag.getInt("WindupTicks"), WINDUP_TICKS);
        if (this.exposedTicks > 0) {
            this.windupTicks = 0;
        }
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
