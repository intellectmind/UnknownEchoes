package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 迷失记录者首席:破损档案室 Mini Boss(回声峭壁,T7 前区域首席)。
 *
 * 机制(残页召回 + 审阅,设计红线 #4 用机制做门槛):
 * - 残页审阅:周期性展开账册(pagestorm)散落 3 张残页并进入"审阅"——审阅期间受护(未破防)。
 *   玩家走近归还全部残页即可打断审阅,首席进入 9 秒暴露窗口(可全额输出);若放任不管,审阅在
 *   读条结束时自行完成并轰出一次墨爆,随后仅短暂暴露——拖延有代价,归页更划算。
 * - 墨迹弹(inkbolt):沿锁定方向喷出一道墨线,命中致盲 + 低伤。
 * - 墨爆(pulse):贴身墨花爆发,击退 + 虚弱。
 * - 笔锋横扫(swipe):近战命中附带致盲 + 虚弱。
 * 结算沿用 MiniBossEntity 个人首杀/重复发放(MiniBossRewardTable)。
 */
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
    private static final int EXPOSED_WINDOW = 180;   // 打断审阅后的暴露窗口 9s
    private static final int READ_TIMEOUT = 500;     // 审阅读条上限(放任不管自行完成)
    private static final double INK_RANGE = 16.0;

    /** 散落的残页位置;非空 = 正在审阅且受护。 */
    private final List<Vec3> scatteredPages = new ArrayList<>();
    private int readTicks = 0;       // 审阅已持续 tick
    private int exposedTicks = 0;    // 暴露窗口剩余 tick

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
        // 审阅期间受护;残页全部归还(或读条结束)后进入暴露窗口可全额输出
        return !this.scatteredPages.isEmpty();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.lost_recorder_chief.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantLostRecorderChief(this, player, firstKill);
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.pagestormCooldown > 0) this.pagestormCooldown--;
        if (this.inkboltCooldown > 0) this.inkboltCooldown--;
        if (this.pulseCooldown > 0) this.pulseCooldown--;

        if (this.exposedTicks > 0) {
            this.exposedTicks--;
            if (this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX(), this.getY() + 1.6, this.getZ(), 3, 0.5, 0.6, 0.5, 0.0);
            }
        } else if (!this.scatteredPages.isEmpty()) {
            this.tickReview(serverLevel);
        } else if (this.getTarget() != null && this.pagestormCooldown <= 0) {
            this.castPagestorm(serverLevel);
        }

        this.tickSpecialAttacks(serverLevel);
    }

    /** 审阅:维持护盾 + 残页演出;玩家归还残页或读条超时则结束审阅进入暴露。 */
    private void tickReview(ServerLevel serverLevel) {
        this.readTicks++;
        for (Vec3 page : this.scatteredPages) {
            if (this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        page.x, page.y + 1.0, page.z, 3, 0.3, 0.6, 0.3, 0.0);
            }
            // 残页向首席回流的审阅光带
            if (this.tickCount % 10 == 0) {
                Vec3 d = this.position().add(0, 1.4, 0).subtract(page);
                int steps = Math.max(2, (int) d.length());
                for (int i = 0; i <= steps; i += 2) {
                    Vec3 p = page.add(d.scale(i / (double) steps));
                    serverLevel.sendParticles(ParticleTypes.SCRAPE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
        // 归还:玩家靠近(2.2 格内)残页即归位
        this.scatteredPages.removeIf(page -> serverLevel.players().stream().anyMatch(
                p -> p.isAlive() && p.distanceToSqr(page.x, page.y, page.z) <= 4.8));

        if (this.scatteredPages.isEmpty()) {
            // 被打断:进入暴露窗口
            this.enterExposed(serverLevel, "message.unknown_echoes.lost_recorder_chief.exposed");
        } else if (this.readTicks >= READ_TIMEOUT) {
            // 读条完成:轰出墨爆,清空残页,短暂暴露(拖延的代价)
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
            this.playSound(ModSounds.LOST_RECORDER_CHIEF_REVIEW.get(), 1.2F, 1.3F);
            this.broadcastHint(serverLevel, hintKey);
        }
    }

    /** 展开账册:散落 3 张残页并进入审阅(护盾)。 */
    private void castPagestorm(ServerLevel serverLevel) {
        this.triggerAnim("attack_controller", "pagestorm");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_REVIEW.get(), 1.4F, 0.6F);
        this.readTicks = 0;
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        int count = 3;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + this.random.nextDouble() * 0.5;
            double dist = 5.0 + this.random.nextDouble() * (ARENA_RADIUS - 7);
            Vec3 page = new Vec3(anchor.x + Math.cos(angle) * dist, anchor.y, anchor.z + Math.sin(angle) * dist);
            this.scatteredPages.add(page);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, page.x, page.y + 1.0, page.z, 16, 0.4, 0.8, 0.4, 0.1);
        }
        this.broadcastHint(serverLevel, "message.unknown_echoes.lost_recorder_chief.guarded");
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

    /** 墨迹弹:沿锁定方向喷出墨线,命中致盲 + 低伤。 */
    private void castInkbolt(ServerLevel serverLevel, LivingEntity target) {
        this.inkboltCooldown = 110;
        this.triggerAnim("attack_controller", "inkbolt");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_AMBIENT.get(), 1.2F, 1.1F);
        Vec3 origin = this.position().add(0, this.getBbHeight() * 0.7, 0);
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        Set<UUID> hit = new HashSet<>();
        for (double d = 1.0; d <= INK_RANGE; d += 1.0) {
            Vec3 p = origin.add(dir.scale(d));
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 2, 0.12, 0.12, 0.12, 0.0);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(p.x - 1.1, p.y - 1.1, p.z - 1.1, p.x + 1.1, p.y + 1.1, p.z + 1.1),
                    e -> e != this && !(e instanceof Enemy) && !hit.contains(e.getUUID()))) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
                hit.add(victim.getUUID());
            }
        }
        BossFx.sparkSpray(serverLevel, origin, dir, INK_RANGE, ParticleTypes.SQUID_INK);
    }

    /** 墨爆:贴身 ~4 格 AoE 击退 + 虚弱。 */
    private void castPulse(ServerLevel serverLevel) {
        this.pulseCooldown = 240;
        this.triggerAnim("attack_controller", "pulse");
        this.playSound(ModSounds.LOST_RECORDER_CHIEF_AMBIENT.get(), 1.4F, 0.6F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.0, 2.0, 4.0),
                e -> e != this && !(e instanceof Enemy))) {
            victim.hurt(this.damageSources().mobAttack(this), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), this);
            Vec3 away = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.6, 0.35, dir.z * 0.6));
                victim.hurtMarked = true;
            }
        }
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2 * i / 28;
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 1.4, this.getZ(),
                    0, Math.cos(angle) * 0.6, 0.1, Math.sin(angle) * 0.6, 1.0);
        }
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                this.getX(), this.getY() + 1.4, this.getZ(), 24, 1.5, 0.8, 1.5, 0.0);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.3, 0), 4.0, ParticleTypes.SQUID_INK);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.2, 0), ParticleTypes.ENCHANT);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0), this);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
            this.triggerAnim("attack_controller", "swipe");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.SQUID_INK);
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
        this.exposedTicks = tag.getInt("ExposedTicks");
        this.readTicks = tag.getInt("ReadTicks");
        this.scatteredPages.clear();
        for (Tag entry : tag.getList("ScatteredPages", Tag.TAG_COMPOUND)) {
            if (entry instanceof CompoundTag pageTag) {
                this.scatteredPages.add(new Vec3(
                        pageTag.getDouble("X"), pageTag.getDouble("Y"), pageTag.getDouble("Z")));
            }
        }
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
