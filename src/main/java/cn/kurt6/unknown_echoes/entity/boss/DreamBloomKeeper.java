package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

public class DreamBloomKeeper extends MiniBossEntity {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("dream_bloom_keeper");

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.dream_bloom_keeper.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.dream_bloom_keeper.walk");
    private static final RawAnimation MIST =
            RawAnimation.begin().then("animation.dream_bloom_keeper.mist", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BLOOM =
            RawAnimation.begin().then("animation.dream_bloom_keeper.bloom", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation NOVA =
            RawAnimation.begin().then("animation.dream_bloom_keeper.nova", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SWIPE =
            RawAnimation.begin().then("animation.dream_bloom_keeper.swipe", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final int ARENA_RADIUS = 14;
    private static final int EXPOSED_WINDOW = 240;
    private static final int BUD_COUNT = 3;
    /** 玩家站进这个半径即掐灭花苞。演出环画在这个值上，两处必须同一个常量。 */
    private static final double BUD_DOUSE_RADIUS = 2.0D;
    /** 花冠新星的水平结算半径。 */
    private static final double NOVA_RADIUS = 4.0D;
    /** 梦雾的锥形射程与锥半角(弧度)，由结算侧的 dot 阈值 0.35 反算。 */
    private static final double MIST_RANGE = 7.0D;
    private static final double MIST_MIN_DOT = 0.35D;

    /** 当前在场治疗花苞位置;非空 = 正在回血且受护。 */
    private final List<Vec3> healingBuds = new ArrayList<>();
    /** 凋萎暴露剩余 tick;>0 = 可全额输出、停止回血、暂不能绽放。 */
    private int exposedTicks = 0;

    private int bloomCooldown = 80;
    private int mistCooldown = 120;
    private int novaCooldown = 200;
    private int sporeBoltCooldown = 60;

    public DreamBloomKeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PINK);
        this.xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
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

        return !this.healingBuds.isEmpty();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.dream_bloom_keeper.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        Vec3 core = this.position().add(0, 1.0, 0);
        long age = level.getGameTime();
        for (Vec3 bud : this.healingBuds) {
            Vec3 marker = bud.add(0, 0.4, 0);
            MiniBossFx.objectiveMarker(level, marker, this.fxTheme(), age);
            MiniBossFx.objectiveLink(level, marker, core, this.fxTheme(), age);
        }
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantDreamBloomKeeper(this, firstKill);
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
        if (this.bloomCooldown > 0) this.bloomCooldown--;
        if (this.mistCooldown > 0) this.mistCooldown--;
        if (this.novaCooldown > 0) this.novaCooldown--;
        if (this.sporeBoltCooldown > 0) this.sporeBoltCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            if (this.tickCount % 6 == 0) {
                MiniBossFx.breachAura(serverLevel, this, this.fxTheme(),
                        serverLevel.getGameTime());
            }
        } else if (!this.healingBuds.isEmpty()) {
            this.tickHealingBuds(serverLevel);
        } else if (this.getTarget() != null && this.bloomCooldown <= 0) {
            this.castBloom(serverLevel);
        }

        this.tickSpecialAttacks(serverLevel);
    }

    private void tickHealingBuds(ServerLevel serverLevel) {
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.015F * this.healingBuds.size());
        }
        for (Vec3 bud : this.healingBuds) {
            if (this.tickCount % 4 == 0) {
                // 环画在"踩进去就熄灭"的那个半径上，玩家能读出要站到哪里。
                MiniBossFx.groundRing(serverLevel, bud, this.fxTheme(), BUD_DOUSE_RADIUS);
            }
        }

        this.healingBuds.removeIf(bud ->
                hasNearbyAlivePlayer(serverLevel, bud, BUD_DOUSE_RADIUS * BUD_DOUSE_RADIUS));
        if (this.healingBuds.isEmpty()) {
            this.exposedTicks = EXPOSED_WINDOW;
            this.playSound(ModSounds.DREAM_BLOOM_KEEPER_BUDS.get(), 1.0F, 1.0F);
            this.broadcastHint(serverLevel, "message.unknown_echoes.dream_bloom_keeper.exposed");
        }
    }

    private void castBloom(ServerLevel serverLevel) {
        this.bloomCooldown = 400;
        this.triggerAnim("attack_controller", "bloom");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_BLOOM.get(), 1.2F, 1.0F);
        this.heal(6.0F);
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        for (int i = 0; i < BUD_COUNT; i++) {
            double angle = Math.PI * 2 * i / BUD_COUNT + this.random.nextDouble() * 0.6;
            double dist = 4.0 + this.random.nextDouble() * (ARENA_RADIUS - 6);
            Vec3 bud = new Vec3(anchor.x + Math.cos(angle) * dist, anchor.y, anchor.z + Math.sin(angle) * dist);
            this.healingBuds.add(bud);
            MiniBossFx.impactZone(serverLevel, bud, this.fxTheme(), BUD_DOUSE_RADIUS, 1.0D);
        }
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                28.0D, 0.34F, 5, this.combatImpactColor());
        this.broadcastHint(serverLevel, "message.unknown_echoes.dream_bloom_keeper.guarded");
    }

    private static boolean hasNearbyAlivePlayer(ServerLevel level, Vec3 pos, double rangeSqr) {
        double range = Math.sqrt(rangeSqr);
        return !level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(pos, pos).inflate(range),
                player -> !player.isSpectator() && player.isAlive()
                        && player.distanceToSqr(pos) <= rangeSqr).isEmpty();
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);
        if (this.novaCooldown <= 0 && distSq <= 25.0) {
            this.castNova(serverLevel);
        } else if (this.mistCooldown <= 0 && distSq <= 64.0 && this.hasLineOfSight(target)) {
            this.castMist(serverLevel, target);
        } else if (this.sporeBoltCooldown <= 0 && distSq <= 196.0 && this.hasLineOfSight(target)) {
            this.castSporeBolt(serverLevel, target);
        }
    }

    private void castSporeBolt(ServerLevel serverLevel, LivingEntity target) {
        this.sporeBoltCooldown = 90;
        this.triggerAnim("attack_controller", "mist");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_SPORE_BOLT.get(), 1.1F, 1.0F);
        Vec3 origin = this.position().add(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 waist = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 delta = waist.subtract(origin);
        MiniBossFx.pierceBeam(serverLevel, origin, delta, this.fxTheme(), delta.length());
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
        if (target.hurt(this.damageSources().mobAttack(this), damage) && target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0), this);
            MiniBossFx.hitMark(serverLevel, target, this.fxTheme());
        }
        BossFx.cinematicImpact(serverLevel, waist, 18.0D, 0.2F, 4,
                this.combatImpactColor());
    }

    private void castMist(ServerLevel serverLevel, LivingEntity target) {
        this.mistCooldown = 160;
        this.triggerAnim("attack_controller", "mist");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_MIST.get(), 1.2F, 1.0F);
        Vec3 forward = target.position().subtract(this.position()).multiply(1, 0, 1).normalize();
        // 锥半角由结算侧的 dot 阈值反算，画出来的扇形与实际吃效果的区域同构。
        MiniBossFx.coneSpray(serverLevel, this.position().add(0, 1.2, 0), forward,
                this.fxTheme(), MIST_RANGE, Math.acos(MIST_MIN_DOT));
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                24.0D, 0.28F, 4, this.combatImpactColor());
        for (ServerPlayer victim : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(MIST_RANGE + 1.0D),
                candidate -> !candidate.isSpectator() && candidate.isAlive()
                        && candidate.distanceToSqr(this) <= MIST_RANGE * MIST_RANGE)) {
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > 1.0E-4
                    && toVictim.normalize().dot(forward) > MIST_MIN_DOT) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), this);
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), this);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
    }

    private void castNova(ServerLevel serverLevel) {
        this.novaCooldown = 240;
        this.triggerAnim("attack_controller", "nova");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_NOVA.get(), 1.4F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(NOVA_RADIUS, 2.0, NOVA_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > NOVA_RADIUS * NOVA_RADIUS) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), this);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 dir = away.normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dir.x * 0.6, 0.35, dir.z * 0.6));
                    victim.hurtMarked = true;
                }
            }
        }
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), NOVA_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                26.0D, 0.54F, 7, this.combatImpactColor());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), this);
            this.triggerAnim("attack_controller", "swipe");
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
        return ModSounds.DREAM_BLOOM_KEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DREAM_BLOOM_KEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DREAM_BLOOM_KEEPER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ExposedTicks", this.exposedTicks);
        tag.putInt("BudCount", this.healingBuds.size());
        ListTag buds = new ListTag();
        for (Vec3 bud : this.healingBuds) {
            CompoundTag budTag = new CompoundTag();
            budTag.putDouble("X", bud.x);
            budTag.putDouble("Y", bud.y);
            budTag.putDouble("Z", bud.z);
            buds.add(budTag);
        }
        tag.put("HealingBuds", buds);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.exposedTicks = BossNbtData.clampTimer(tag.getInt("ExposedTicks"), EXPOSED_WINDOW);
        BossNbtData.loadVec3sNear(tag, "HealingBuds", this.healingBuds, BUD_COUNT,
                Vec3.atCenterOf(this.arenaAnchor()), ARENA_RADIUS, ARENA_RADIUS);
        if (!this.healingBuds.isEmpty()) {
            this.exposedTicks = 0;
        } else if (tag.getInt("BudCount") > 0) {
            this.bloomCooldown = 60;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("mist", MIST)
                .triggerableAnim("bloom", BLOOM)
                .triggerableAnim("nova", NOVA)
                .triggerableAnim("swipe", SWIPE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
