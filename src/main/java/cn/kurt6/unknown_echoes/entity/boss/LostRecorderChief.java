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

public class LostRecorderChief extends MiniBossEntity {
    public static final ResourceLocation MINIBOSS_ID = UnknownEchoes.id("lost_recorder_chief");

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.lost_recorder_chief.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.lost_recorder_chief.walk");
    private static final RawAnimation INKBOLT =
            RawAnimation.begin().then("animation.lost_recorder_chief.inkbolt", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PAGESTORM =
            RawAnimation.begin().then("animation.lost_recorder_chief.pagestorm", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PULSE =
            RawAnimation.begin().then("animation.lost_recorder_chief.pulse", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SWIPE =
            RawAnimation.begin().then("animation.lost_recorder_chief.swipe", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final int ARENA_RADIUS = 14;
    private static final int EXPOSED_WINDOW = 180;
    private static final int READ_TIMEOUT = 500;
    private static final int PAGE_COUNT = 3;
    private static final double INK_RANGE = 16.0;
    /** 玩家站进这个半径即收回残页。演出环画在这个值上，两处必须同一个常量。 */
    private static final double PAGE_PICKUP_RADIUS = 2.19D;
    /** 墨爆的水平结算半径。 */
    private static final double PULSE_RADIUS = 4.0D;
    /** 墨弹的束半径，与结算侧的 hitRadius 基数一致。 */
    private static final double INK_BEAM_RADIUS = 1.1D;

    /** 散落的残页位置;非空 = 正在审阅且受护。 */
    private final List<Vec3> scatteredPages = new ArrayList<>();
    private int readTicks = 0;
    private int exposedTicks = 0;

    private int pagestormCooldown = 120;
    private int inkboltCooldown = 70;
    private int pulseCooldown = 200;

    public LostRecorderChief(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 290.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 34.0D)
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

        return !this.scatteredPages.isEmpty();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.lost_recorder_chief.guarded";
    }

    @Override
    protected void tickMechanicGuidance(ServerLevel level) {
        Vec3 core = this.position().add(0, 1.4, 0);
        long age = level.getGameTime();
        for (Vec3 page : this.scatteredPages) {
            Vec3 marker = page.add(0, 0.7, 0);
            MiniBossFx.objectiveMarker(level, marker, this.fxTheme(), age);
            MiniBossFx.objectiveLink(level, marker, core, this.fxTheme(), age);
        }
    }

    @Override
    protected void grantSettlement(boolean firstKill) {
        MiniBossRewardTable.grantLostRecorderChief(this, firstKill);
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
        if (this.pagestormCooldown > 0) this.pagestormCooldown--;
        if (this.inkboltCooldown > 0) this.inkboltCooldown--;
        if (this.pulseCooldown > 0) this.pulseCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            if (this.tickCount % 6 == 0) {
                MiniBossFx.breachAura(serverLevel, this, this.fxTheme(),
                        serverLevel.getGameTime());
            }
        } else if (!this.scatteredPages.isEmpty()) {
            this.tickReview(serverLevel);
        } else if (this.getTarget() != null && this.pagestormCooldown <= 0) {
            this.castPagestorm(serverLevel);
        }

        this.tickSpecialAttacks(serverLevel);
    }

    private void tickReview(ServerLevel serverLevel) {
        this.readTicks++;
        for (Vec3 page : this.scatteredPages) {
            if (this.tickCount % 5 == 0) {
                // 环画在"走进去就收回"的那个半径上，玩家能读出要站到哪里。
                MiniBossFx.groundRing(serverLevel, page, this.fxTheme(), PAGE_PICKUP_RADIUS);
            }
        }

        int before = this.scatteredPages.size();
        this.scatteredPages.removeIf(page -> hasNearbyAlivePlayer(serverLevel, page,
                PAGE_PICKUP_RADIUS * PAGE_PICKUP_RADIUS));
        if (this.scatteredPages.size() < before) {
            // 玩家收回一张残页:给出可听的进度反馈,否则整个审阅阶段只有粒子没有声音。
            this.playSound(ModSounds.LOST_RECORDER_CHIEF_REVIEW.get(), 1.1F, 1.0F);
        }

        if (this.scatteredPages.isEmpty()) {

            this.enterExposed(serverLevel, "message.unknown_echoes.lost_recorder_chief.exposed");
        } else if (this.readTicks >= READ_TIMEOUT) {

            this.scatteredPages.clear();
            this.castPulse(serverLevel);
            this.enterExposed(serverLevel, null);
            this.exposedTicks = 60;
        }
    }

    private void enterExposed(ServerLevel serverLevel, String hintKey) {
        this.scatteredPages.clear();
        this.readTicks = 0;
        this.exposedTicks = EXPOSED_WINDOW;
        this.pagestormCooldown = 400;
        if (hintKey != null) {
            this.playSound(ModSounds.LOST_RECORDER_CHIEF_EXPOSED.get(), 1.2F, 1.0F);
            MiniBossFx.breachAura(serverLevel, this, this.fxTheme(), serverLevel.getGameTime());
            BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                    24.0D, 0.46F, 6, this.combatImpactColor());
            this.broadcastHint(serverLevel, hintKey);
        }
    }

    private void castPagestorm(ServerLevel serverLevel) {
        this.triggerAnim("attack_controller", "pagestorm");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_PAGESTORM.get(), 1.4F, 1.0F);
        this.readTicks = 0;
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        for (int i = 0; i < PAGE_COUNT; i++) {
            double angle = Math.PI * 2 * i / PAGE_COUNT + this.random.nextDouble() * 0.5;
            double dist = 5.0 + this.random.nextDouble() * (ARENA_RADIUS - 7);
            Vec3 page = new Vec3(anchor.x + Math.cos(angle) * dist, anchor.y, anchor.z + Math.sin(angle) * dist);
            this.scatteredPages.add(page);
            MiniBossFx.impactZone(serverLevel, page, this.fxTheme(), PAGE_PICKUP_RADIUS, 1.0D);
        }
        BossFx.cinematicImpact(serverLevel, anchor.add(0.0D, 0.8D, 0.0D),
                26.0D, 0.22F, 4, this.combatImpactColor());
        this.broadcastHint(serverLevel, "message.unknown_echoes.lost_recorder_chief.guarded");
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
        if (this.pulseCooldown <= 0 && distSq <= 16.0) {
            this.castPulse(serverLevel);
        } else if (this.inkboltCooldown <= 0 && distSq <= INK_RANGE * INK_RANGE
                && this.hasLineOfSight(target)) {
            this.castInkbolt(serverLevel, target);
        }
    }

    private void castInkbolt(ServerLevel serverLevel, LivingEntity target) {
        this.inkboltCooldown = 110;
        this.triggerAnim("attack_controller", "inkbolt");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_INKBOLT.get(), 1.2F, 1.0F);
        Vec3 origin = this.position().add(0, this.getBbHeight() * 0.7, 0);
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        MiniBossFx.pierceBeam(serverLevel, origin, dir, this.fxTheme(), INK_RANGE);
        Vec3 end = origin.add(dir.scale(INK_RANGE));
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, end).inflate(INK_BEAM_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy) && e.isAlive())) {
            Vec3 victimCenter = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
            Vec3 toVictim = victimCenter.subtract(origin);
            double along = toVictim.dot(dir);
            double hitRadius = INK_BEAM_RADIUS + victim.getBbWidth() * 0.5D;
            if (along < 0.0D || along > INK_RANGE
                    || toVictim.subtract(dir.scale(along)).lengthSqr() > hitRadius * hitRadius) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
        }
        BossFx.cinematicImpact(serverLevel,
                target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                18.0D, 0.2F, 4, this.combatImpactColor());
    }

    private void castPulse(ServerLevel serverLevel) {
        this.pulseCooldown = 240;
        this.triggerAnim("attack_controller", "pulse");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_PULSE.get(), 1.4F, 1.0F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(PULSE_RADIUS, 2.0, PULSE_RADIUS),
                e -> e != this && !e.isSpectator() && !(e instanceof Enemy))) {
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > PULSE_RADIUS * PULSE_RADIUS) {
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
        MiniBossFx.groundRing(serverLevel, this.position(), this.fxTheme(), PULSE_RADIUS);
        BossFx.cinematicImpact(serverLevel, this.position().add(0, 1.0, 0),
                26.0D, 0.52F, 7, this.combatImpactColor());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0), this);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
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
        return ModSounds.LOST_RECORDER_CHIEF_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LOST_RECORDER_CHIEF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LOST_RECORDER_CHIEF_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ExposedTicks", this.exposedTicks);
        tag.putInt("ReadTicks", this.readTicks);
        tag.putInt("PageCount", this.scatteredPages.size());
        ListTag pages = new ListTag();
        for (Vec3 page : this.scatteredPages) {
            CompoundTag pageTag = new CompoundTag();
            pageTag.putDouble("X", page.x);
            pageTag.putDouble("Y", page.y);
            pageTag.putDouble("Z", page.z);
            pages.add(pageTag);
        }
        tag.put("ScatteredPages", pages);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.exposedTicks = BossNbtData.clampTimer(tag.getInt("ExposedTicks"), EXPOSED_WINDOW);
        this.readTicks = BossNbtData.clampTimer(tag.getInt("ReadTicks"), READ_TIMEOUT);
        BossNbtData.loadVec3sNear(tag, "ScatteredPages", this.scatteredPages, PAGE_COUNT,
                Vec3.atCenterOf(this.arenaAnchor()), ARENA_RADIUS, ARENA_RADIUS);
        if (!this.scatteredPages.isEmpty()) {
            this.exposedTicks = 0;
        } else if (tag.getInt("PageCount") > 0) {
            this.pagestormCooldown = 60;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("inkbolt", INKBOLT)
                .triggerableAnim("pagestorm", PAGESTORM)
                .triggerableAnim("pulse", PULSE)
                .triggerableAnim("swipe", SWIPE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
