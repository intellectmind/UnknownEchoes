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
 * 沉眠花守:沉眠花屋地下温室 Mini Boss(设计文档 10.4 / 重设计 P2)。
 *
 * 机制(低杀伤 + 治疗花苞,设计红线 #4 用机制而非血量做门槛):
 * - 治疗花苞:周期性绽放(bloom)在场地内召唤数枚治疗花苞,花苞存在期间持续回血且受护(未破防)。
 *   玩家走近花苞将其清除;全部清除后进入 12 秒"凋萎"暴露窗口——停止回血、可全额输出,且暂不能再绽放。
 * - 梦雾(mist):正面锥形喷吐,使玩家缓速 + 反胃(低伤、控制为主)。
 * - 花粉爆(nova):贴身花粉爆发,击退 + 虚弱。
 * - 藤臂横扫(swipe):近战命中附带缓速 + 反胃。
 * 结算沿用 MiniBossEntity 个人首杀/重复发放(MiniBossRewardTable)。
 */
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

    private static final int ARENA_RADIUS = 12;
    private static final int EXPOSED_WINDOW = 240; // 凋萎暴露窗口 12s

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
        // 治疗花苞存在期间受护(回血+减伤);凋萎窗口或无花苞时可全额输出
        return !this.healingBuds.isEmpty();
    }

    @Override
    protected String guardedHintKey() {
        return "message.unknown_echoes.dream_bloom_keeper.guarded";
    }

    @Override
    protected void grantSettlement(ServerPlayer player, boolean firstKill) {
        MiniBossRewardTable.grantDreamBloomKeeper(this, player, firstKill);
    }

    // ---- 主循环 ----

    @Override
    public void aiStep() {
        super.aiStep();
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
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        this.getX(), this.getY() + 1.6, this.getZ(), 4, 0.5, 0.6, 0.5, 0.0);
            }
        } else if (!this.healingBuds.isEmpty()) {
            this.tickHealingBuds(serverLevel);
        } else if (this.getTarget() != null && this.bloomCooldown <= 0) {
            this.castBloom(serverLevel);
        }

        this.tickSpecialAttacks(serverLevel);
    }

    /** 治疗花苞:持续回血 + 粒子;玩家走近清除;全清后进入凋萎暴露窗口。 */
    private void tickHealingBuds(ServerLevel serverLevel) {
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.015F * this.healingBuds.size());
        }
        for (Vec3 bud : this.healingBuds) {
            if (this.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        bud.x, bud.y + 0.6, bud.z, 2, 0.25, 0.4, 0.25, 0.0);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        bud.x, bud.y + 0.4, bud.z, 1, 0.2, 0.3, 0.2, 0.0);
            }
            // 花苞到本体的回流光带
            if (this.tickCount % 8 == 0) {
                Vec3 d = this.position().add(0, 1.0, 0).subtract(bud);
                int steps = Math.max(2, (int) d.length());
                for (int i = 0; i <= steps; i += 2) {
                    Vec3 p = bud.add(d.scale(i / (double) steps));
                    serverLevel.sendParticles(ParticleTypes.COMPOSTER, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
        // 清除:任意玩家靠近(2 格内)
        this.healingBuds.removeIf(bud -> serverLevel.players().stream().anyMatch(
                p -> p.isAlive() && p.distanceToSqr(bud.x, bud.y, bud.z) <= 4.0));
        if (this.healingBuds.isEmpty()) {
            this.exposedTicks = EXPOSED_WINDOW;
            this.playSound(ModSounds.DREAM_BLOOM_KEEPER_BLOOM.get(), 1.0F, 1.4F);
            this.broadcastHint(serverLevel, "message.unknown_echoes.dream_bloom_keeper.exposed");
        }
    }

    /** 绽放:召唤治疗花苞 + 小幅回血。 */
    private void castBloom(ServerLevel serverLevel) {
        this.bloomCooldown = 400;
        this.triggerAnim("attack_controller", "bloom");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_BLOOM.get(), 1.2F, 0.7F);
        this.heal(6.0F);
        Vec3 anchor = Vec3.atCenterOf(this.arenaAnchor());
        int count = 3;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + this.random.nextDouble() * 0.6;
            double dist = 4.0 + this.random.nextDouble() * (ARENA_RADIUS - 6);
            Vec3 bud = new Vec3(anchor.x + Math.cos(angle) * dist, anchor.y, anchor.z + Math.sin(angle) * dist);
            this.healingBuds.add(bud);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, bud.x, bud.y + 0.5, bud.z, 12, 0.3, 0.5, 0.3, 0.02);
        }
        this.broadcastHint(serverLevel, "message.unknown_echoes.dream_bloom_keeper.guarded");
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

    /** 孢子弹:远程直击,梦雾孢子弹道,命中缓速 + 反胃(补齐花守的远程手段)。 */
    private void castSporeBolt(ServerLevel serverLevel, LivingEntity target) {
        this.sporeBoltCooldown = 90;
        this.triggerAnim("attack_controller", "mist");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_AMBIENT.get(), 1.1F, 1.2F);
        double ex = this.getX(), ey = this.getEyeY(), ez = this.getZ();
        double tx = target.getX(), ty = target.getY() + target.getBbHeight() * 0.5, tz = target.getZ();
        int steps = 14;
        for (int i = 1; i <= steps; i++) {
            double f = i / (double) steps;
            double px = ex + (tx - ex) * f, py = ey + (ty - ey) * f, pz = ez + (tz - ez) * f;
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, px, py, pz, 2, 0.08, 0.08, 0.08, 0.0);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
        target.hurt(this.damageSources().mobAttack(this), damage);
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0), this);
        }
        BossFx.sparkSpray(serverLevel, this.position().add(0, this.getEyeHeight(), 0),
                target.position().subtract(this.position()), 6.0, ParticleTypes.CHERRY_LEAVES);
    }

    /** 梦雾锥:正面 ~7 格内玩家缓速 + 反胃(低伤控制)。 */
    private void castMist(ServerLevel serverLevel, LivingEntity target) {
        this.mistCooldown = 160;
        this.triggerAnim("attack_controller", "mist");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_AMBIENT.get(), 1.2F, 0.8F);
        Vec3 forward = target.position().subtract(this.position()).multiply(1, 0, 1).normalize();
        for (double d = 1.0; d <= 7.0; d += 1.0) {
            Vec3 p = this.position().add(forward.scale(d)).add(0, 1.2, 0);
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y, p.z, 6, 0.6, 0.5, 0.6, 0.0);
            serverLevel.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 2, 0.5, 0.4, 0.5, 0.0);
        }
        BossFx.sparkSpray(serverLevel, this.position().add(0, 1.2, 0), forward, 7.0, ParticleTypes.SPORE_BLOSSOM_AIR);
        for (Player victim : serverLevel.players()) {
            if (!victim.isAlive() || victim.distanceToSqr(this) > 64.0) {
                continue;
            }
            Vec3 toVictim = victim.position().subtract(this.position()).multiply(1, 0, 1);
            if (toVictim.lengthSqr() > 1.0E-4 && toVictim.normalize().dot(forward) > 0.35) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), this);
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), this);
            }
        }
    }

    /** 花粉爆:贴身 ~4 格 AoE 击退 + 虚弱。 */
    private void castNova(ServerLevel serverLevel) {
        this.novaCooldown = 240;
        this.triggerAnim("attack_controller", "nova");
        this.playSound(ModSounds.DREAM_BLOOM_KEEPER_AMBIENT.get(), 1.4F, 0.6F);
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
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    this.getX(), this.getY() + 1.4, this.getZ(),
                    0, Math.cos(angle) * 0.6, 0.1, Math.sin(angle) * 0.6, 1.0);
        }
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                this.getX(), this.getY() + 1.4, this.getZ(), 20, 1.5, 0.8, 1.5, 0.0);
        BossFx.groundShockwave(serverLevel, this.position().add(0, 0.3, 0), 4.0, ParticleTypes.CHERRY_LEAVES);
        BossFx.impactBurst(serverLevel, this.position().add(0, 1.2, 0), ParticleTypes.CHERRY_LEAVES);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), this);
            this.triggerAnim("attack_controller", "swipe");
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.impactBurst(serverLevel,
                        target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.CHERRY_LEAVES);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.exposedTicks = tag.getInt("ExposedTicks");
        // 花苞为场地坐标,重载后由下一次绽放重建;此处仅恢复暴露窗口与冷却节奏
        if (tag.getInt("BudCount") > 0) {
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
