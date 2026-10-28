package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.item.weapon.WeaponEffectGuard;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModParticleEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ParticleEffects {
    private static final ResourceLocation SLOW = UnknownEchoes.id("particle_slow");
    private static final ResourceLocation SHADOW_SPEED = UnknownEchoes.id("particle_shadow_speed");
    private static final String SHIELD_REMAINDER = "ue_particle_shield_remaining";
    private static final Map<LivingEntity, List<Dot>> DOTS = new WeakHashMap<>();
    private static final Map<LivingEntity, Control> CONTROLS = new WeakHashMap<>();
    private static final Map<LivingEntity, Shield> SHIELDS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Map<ParticleKind, Field>> FIELDS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Fall> FALLS = new WeakHashMap<>();
    private static final ThreadLocal<DotKill> DOT_KILL = new ThreadLocal<>();
    private static final ThreadLocal<Set<LivingEntity>> HEALING = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final ThreadLocal<StatusApplication> APPLICATION = new ThreadLocal<>();
    private record Control(long until, long frozenUntil, float slow) { }
    private record Shield(long until, float remaining, int count) { }
    private record Fall(ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 position, long tick, double descended, boolean valid) { }
    private record DotKill(LivingEntity target, DamageSource source, ServerPlayer player, ParticleAttack attack, ServerLevel level) { }
    private static final class StatusApplication {
        private final LivingEntity target;
        private final MobEffectInstance effect;
        private final StatusApplication previous;
        private boolean accepted;
        private StatusApplication(LivingEntity target, MobEffectInstance effect, StatusApplication previous) {
            this.target = target; this.effect = effect; this.previous = previous;
        }
    }
    private static final class Dot {
        private final ParticleKind kind;
        private final ParticleAttack attack;
        private final int interval;
        private long until, next;
        private Dot(ParticleKind kind, ParticleAttack attack, long now, int duration) {
            this.kind = kind; this.attack = attack; until = now + duration;
            interval = kind == ParticleKind.FIRE ? 20 : Math.max(1, 25 >> ((int) ParticleStats.venomLevel(attack.data().count(kind)) - 1));
            next = now + interval;
        }
    }
    private static final class Field {
        private final ServerLevel level;
        private Vec3 center;
        private double radius;
        private int durationTicks;
        private long until, next;
        private Field(ServerLevel level, Vec3 center, double radius, long now, int duration) {
            this.level = level; this.center = center; this.radius = radius; durationTicks = duration; until = now + duration; next = now + 20;
        }
    }

    private ParticleEffects() { }
    private static long now(LivingEntity entity) { return entity.getServer().overworld().getGameTime(); }
    public static boolean boss(LivingEntity entity) { return entity.getType().is(ModEntityTags.ECHO_BOSSES) || entity.getType().is(Tags.EntityTypes.BOSSES); }
    public static boolean canHit(ServerPlayer player, LivingEntity target) {
        return target != player && !target.isRemoved() && !target.isAlliedTo(player) && !player.isAlliedTo(target)
                && (!(target instanceof Player other) || !other.isSpectator() && player.canHarmPlayer(other));
    }
    public static boolean isImpactCurrent(ServerPlayer player, LivingEntity target, ServerLevel level) {
        return activeOwner(player, level) && target.level() == level && !target.isRemoved() && canHit(player, target);
    }
    private static List<LivingEntity> targets(ServerPlayer player, Vec3 center, double radius) {
        double r = Math.min(7, radius);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                        e -> e.isAlive() && canHit(player, e) && (e instanceof Enemy || e instanceof Player)
                                && e.distanceToSqr(center) <= r * r && player.hasLineOfSight(e))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();
    }
    private static DamageSource source(ServerPlayer player, ParticleKind kind) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, UnknownEchoes.id("particle_" + kind.id()));
        return new DamageSource(player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), player);
    }
    public static boolean immune(ServerPlayer player, LivingEntity target, ParticleKind kind) {
        return target.isInvulnerableTo(source(player, kind)) || kind == ParticleKind.FIRE
                && (target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE))
                || kind == ParticleKind.VENOM && !target.canBeAffected(new MobEffectInstance(MobEffects.POISON, 20));
    }
    public static boolean frozen(LivingEntity target) {
        Control control = CONTROLS.get(target);
        return control != null && control.frozenUntil > now(target) && target.hasEffect(ModParticleEffects.FROST);
    }
    public static float consumeShadow(ServerPlayer player, int n) {
        long ready = player.getPersistentData().getLong("ue_particle_shadow_ready");
        player.getPersistentData().remove("ue_particle_shadow_ready");
        if (n >= 35 && player.isInvisible()) return 0.6F;
        return n >= 30 && ready > now(player) ? 0.15F : 0;
    }

    public static void hit(ServerPlayer player, LivingEntity target, ParticleAttack attack, boolean backstab) {
        ServerLevel level = player.serverLevel();
        if (!isImpactCurrent(player, target, level)) return;
        Vec3 center = target.position();
        ParticleData data = attack.data();
        int fire = data.count(ParticleKind.FIRE), frost = data.count(ParticleKind.FROST), venom = data.count(ParticleKind.VENOM);
        int thunder = data.count(ParticleKind.THUNDER), holy = data.count(ParticleKind.HOLY), earth = data.count(ParticleKind.EARTH);
        boolean lightning = thunder > 0 && player.getRandom().nextFloat() < ParticleStats.thunderChance(thunder);
        if (target.isAlive()) {
            if (fire > 0) {
                int radius = ParticleStats.fireSpreadRadius(fire);
                if (applyDot(player, target, attack, ParticleKind.FIRE) && fire >= 7)
                    for (LivingEntity other : targets(player, center, radius)) {
                        if (!isImpactCurrent(player, target, level)) return;
                        if (other != target && other.distanceToSqr(center) <= radius * radius)
                            applyDot(player, other, attack, ParticleKind.FIRE);
                    }
            }
            if (!isImpactCurrent(player, target, level)) return;
            if (venom > 0 && applyDot(player, target, attack, ParticleKind.VENOM) && venom >= 5) {
                int radius = ParticleStats.venomSpreadRadius(venom);
                for (LivingEntity other : targets(player, center, radius)) {
                    if (!isImpactCurrent(player, target, level)) return;
                    if (other != target && other.distanceToSqr(center) <= radius * radius)
                        applyDot(player, other, attack, ParticleKind.VENOM);
                }
            }
            if (!isImpactCurrent(player, target, level)) return;
            if (target.isAlive()) {
                boolean freeze = frost >= 3 && player.getRandom().nextFloat() < ParticleStats.frostChance(frost);
                boolean knock = earth > 0 && player.getRandom().nextFloat() < ParticleStats.earthChance(earth);
                int freezeTicks = freeze ? ParticleStats.frostDurationTicks(frost) : 0;
                float thunderSlow = lightning && thunder >= 20 ? 0.3F : 0;
                int slowTicks = ParticleStats.thunderSlowTicks(thunder);
                boolean froze = control(player, target, ParticleStats.frostSlow(frost), freezeTicks, thunderSlow,
                        knock ? ParticleStats.earthDistance(earth) : 0, earth >= 45, player.position(), slowTicks);
                if (!isImpactCurrent(player, target, level)) return;
                int frostRadius = froze && frost >= 20 ? 3 : 0;
                int earthRadius = (knock && earth >= 30 || earth >= 45) ? ParticleStats.earthAreaRadius(earth) : 0;
                if (frostRadius > 0 || earthRadius > 0) {
                    for (LivingEntity other : targets(player, center, Math.max(frostRadius, earthRadius))) {
                        if (!isImpactCurrent(player, target, level)) return;
                        if (other == target) continue;
                        double distance = other.distanceToSqr(center);
                        boolean freezeOther = frostRadius > 0 && distance <= frostRadius * frostRadius;
                        boolean push = earthRadius > 0 && distance <= earthRadius * earthRadius;
                        control(player, other, freezeOther ? ParticleStats.frostSlow(frost) : 0, freezeOther ? freezeTicks : 0, 0,
                                push ? ParticleStats.earthDistance(earth) : 0, push && earth >= 45, center, 60);
                    }
                }
                if (!isImpactCurrent(player, target, level)) return;
                if (froze && frost >= 35) field(player, ParticleKind.FROST, center, ParticleStats.frostFieldRadius(frost), 80);
            }
        }
        if (fire >= 20) field(player, ParticleKind.FIRE, center, ParticleStats.fireFieldRadius(fire), ParticleStats.fireFieldTicks(fire));
        if (lightning) thunder(player, target, thunder);
        if (!isImpactCurrent(player, target, level)) return;
        if (holy > 0) holy(player, holy);
        if (!isImpactCurrent(player, target, level)) return;
        if (backstab && data.count(ParticleKind.SHADOW) >= 30) player.getPersistentData().putLong("ue_particle_shadow_ready", now(player) + 100);
        if (earth == 50) areaDamage(player, center, 7, 10, ParticleKind.EARTH, null);
    }

    private static boolean applyDot(ServerPlayer player, LivingEntity target, ParticleAttack attack, ParticleKind kind) {
        ServerLevel level = player.serverLevel();
        if (!canApplyDot(player, target, level, kind)) return false;
        int n = attack.data().count(kind);
        var effect = kind == ParticleKind.FIRE ? ModParticleEffects.FIRE : ModParticleEffects.VENOM;
        if (kind == ParticleKind.VENOM && !target.canBeAffected(new MobEffectInstance(MobEffects.POISON, 20))) return false;
        if (!target.hasEffect(effect)) clearEffect(target, effect);
        int duration = (int) ((kind == ParticleKind.FIRE ? ParticleStats.fireSeconds(n) : ParticleStats.venomSeconds(n)) * 20);
        if (!applyStatus(target, new MobEffectInstance(effect, duration, 0, false, true, true))) return false;
        if (!canApplyDot(player, target, level, kind)) return false;
        if (kind == ParticleKind.FIRE) target.igniteForSeconds(duration / 20F);
        List<Dot> dots = DOTS.computeIfAbsent(target, ignored -> new ArrayList<>());
        dots.removeIf(dot -> dot.until <= now(player));
        int maximum = kind == ParticleKind.FIRE ? ParticleStats.fireLayers(n) : ParticleStats.venomLayers(n);
        int layers = 0;
        for (Dot dot : dots) if (dot.kind == kind && dot.attack.owner().equals(attack.owner()) && dot.attack.weapon().equals(attack.weapon())) {
            dot.until = now(player) + duration;
            layers++;
        }
        if (layers < maximum) dots.add(new Dot(kind, attack, now(player), duration));
        return true;
    }

    private static boolean canApplyDot(ServerPlayer player, LivingEntity target, ServerLevel level, ParticleKind kind) {
        return activeOwner(player, level) && target.level() == level && target.isAlive()
                && canHit(player, target) && !immune(player, target, kind);
    }

    private static boolean control(ServerPlayer player, LivingEntity target, float frostSlow, int freezeTicks, float thunderSlow,
                                   float distance, boolean launch, Vec3 origin, int duration) {
        return controlWhen(player, target, frostSlow, freezeTicks, thunderSlow, distance, launch, origin, duration, () -> true);
    }

    private static boolean controlWhen(ServerPlayer player, LivingEntity target, float frostSlow, int freezeTicks, float thunderSlow,
                                       float distance, boolean launch, Vec3 origin, int duration, BooleanSupplier current) {
        ServerLevel level = player.serverLevel();
        if (!current.getAsBoolean()) return false;
        if (!canControlTarget(player, target, level)) return false;
        if ((frostSlow > 0 || freezeTicks > 0) && immune(player, target, ParticleKind.FROST)) { frostSlow = 0; freezeTicks = 0; }
        if (thunderSlow > 0 && immune(player, target, ParticleKind.THUNDER)) thunderSlow = 0;
        if ((distance > 0 || launch) && immune(player, target, ParticleKind.EARTH)) { distance = 0; launch = false; }
        float slow = Math.max(frostSlow, thunderSlow);
        int activeTicks = frostSlow > 0 || freezeTicks > 0 ? 60 : Math.min(60, duration);
        if (boss(target) || slow <= 0 && freezeTicks <= 0 && distance <= 0 && !launch) return false;
        long time = now(player);
        if (target.getPersistentData().getLong("ue_particle_control_ready") > time) return false;
        boolean freeze = freezeTicks > 0 && !(target instanceof Player);
        if (!applyStatus(target, new MobEffectInstance(ModParticleEffects.FROST, activeTicks, 0, false, false, true))) return false;
        if (!current.getAsBoolean()) return false;
        if (!canControlTarget(player, target, level)) return false;
        if ((frostSlow > 0 || freezeTicks > 0) && immune(player, target, ParticleKind.FROST)) { frostSlow = 0; freezeTicks = 0; }
        if (thunderSlow > 0 && immune(player, target, ParticleKind.THUNDER)) thunderSlow = 0;
        if ((distance > 0 || launch) && immune(player, target, ParticleKind.EARTH)) { distance = 0; launch = false; }
        slow = Math.max(frostSlow, thunderSlow);
        freeze = freezeTicks > 0 && !(target instanceof Player);
        activeTicks = frostSlow > 0 || freezeTicks > 0 ? 60 : Math.min(60, duration);
        if (slow <= 0 && !freeze && distance <= 0 && !launch) return false;
        target.getPersistentData().putLong("ue_particle_control_ready", time + 60);
        Control control = new Control(time + activeTicks, freeze ? time + Math.min(60, freezeTicks) : 0, slow);
        CONTROLS.put(target, control);
        if (distance > 0) {
            Vec3 direction = target.position().subtract(origin).multiply(1, 0, 1).normalize();
            target.knockback(distance * 0.25, -direction.x, -direction.z);
        }
        if (!current.getAsBoolean()) return false;
        if (!canControlTarget(player, target, level) || CONTROLS.get(target) != control
                || !target.hasEffect(ModParticleEffects.FROST)) return false;
        if (launch && !immune(player, target, ParticleKind.EARTH)) { target.push(0, 0.5, 0); target.hurtMarked = true; }
        return freeze;
    }

    private static boolean canControlTarget(ServerPlayer player, LivingEntity target, ServerLevel level) {
        return activeOwner(player, level) && target.level() == level && target.isAlive()
                && canHit(player, target) && !boss(target);
    }

    private static boolean controlAreaTarget(ServerPlayer player, LivingEntity target, ServerLevel level, Vec3 center, double radius) {
        return activeOwner(player, level) && target.level() == level && target.distanceToSqr(center) <= radius * radius
                && player.hasLineOfSight(target);
    }

    private static void thunder(ServerPlayer player, LivingEntity target, int n) {
        ServerLevel level = player.serverLevel();
        if (!activeOwner(player, level) || target.level() != level || target.isRemoved()) return;
        ParticleFx.ring(level, target.position().add(0, 0.2, 0), 0.7, ParticleKind.THUNDER);
        int maximum = Math.min(4, (int) ParticleStats.thunderTargets(n));
        if (maximum == 0) return;
        float multiplier = ParticleStats.thunderMultiplier(n);
        if (n >= 30 && player.getRandom().nextFloat() < ParticleStats.thunderCriticalChance(n)) multiplier *= 2;
        List<LivingEntity> remaining = new ArrayList<>(targets(player, target.position(), 7));
        remaining.removeIf(e -> e.getUUID().equals(target.getUUID()));
        LivingEntity previous = target;
        for (int jump = 0; jump < maximum; jump++) {
            if (!activeOwner(player, level) || target.level() != level || target.isRemoved()) return;
            LivingEntity from = n >= 35 ? target : previous;
            if (from.level() != level || from.isRemoved()) break;
            LivingEntity other = remaining.stream()
                    .filter(e -> e.level() == level && e.isAlive() && canHit(player, e) && !immune(player, e, ParticleKind.THUNDER)
                            && e.distanceToSqr(target.position()) <= 49 && from.hasLineOfSight(e))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(from.position()))).orElse(null);
            if (other == null) break;
            remaining.removeIf(e -> e.getUUID().equals(other.getUUID()));
            Vec3 start = from.getEyePosition(), end = other.getEyePosition();
            damage(player, other, ParticleRules.DAMAGE_PER_PARTICLE * n * 0.6F * multiplier, ParticleKind.THUNDER, null);
            if (n >= 20 && other.isAlive() && activeOwner(player, level) && other.level() == level && canHit(player, other))
                control(player, other, 0, 0, 0.3F, 0, false, from.position(), ParticleStats.thunderSlowTicks(n));
            ParticleFx.line(level, start, end, ParticleKind.THUNDER);
            previous = other;
        }
        if (n == 50 && activeOwner(player, level) && target.level() == level && !target.isRemoved())
            areaDamage(player, target.position(), 7, 10 * multiplier, ParticleKind.THUNDER, null);
    }

    private static void holy(ServerPlayer player, int n) {
        ServerLevel level = player.serverLevel();
        if (!activeOwner(player, level)) return;
        float heal = ParticleStats.holyHeal(n);
        if (player.getRandom().nextFloat() < ParticleStats.holyChance(n)) {
            if (applyStatus(player, new MobEffectInstance(ModParticleEffects.SHIELD, 80, 0, false, true, true))) {
                if (!activeOwner(player, level)) return;
                Shield previous = SHIELDS.get(player);
                float previousValue = previous == null ? 0 : Math.min(previous.remaining, player.getAbsorptionAmount());
                float amount = Math.min(8, Math.max(previousValue, ParticleStats.holyShield(n)));
                player.setAbsorptionAmount(player.getAbsorptionAmount() + amount - previousValue);
                rememberShield(player, new Shield(now(player) + 80, amount, n));
                if (n >= 10) heal += 2;
            }
        }
        if (!activeOwner(player, level)) return;
        heal(player, heal);
        if (n >= 30 && activeOwner(player, level)) field(player, ParticleKind.HOLY, player.position(), ParticleStats.holyFieldRadius(n), 80);
    }

    private static void heal(LivingEntity target, float amount) {
        if (!target.isAlive() || target.getHealth() >= target.getMaxHealth()) return;
        long time = now(target);
        if (target.getPersistentData().getLong("ue_particle_holy_ready") > time) return;
        Set<LivingEntity> healing = HEALING.get();
        // 治疗事件在生命值与冷却更新前触发，必须先阻止同一目标重入。
        if (!healing.add(target)) return;
        try {
            target.heal(Math.min(8, amount));
            target.getPersistentData().putLong("ue_particle_holy_ready", time + 60);
        } finally {
            healing.remove(target);
            if (healing.isEmpty()) HEALING.remove();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shieldDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        Shield shield = SHIELDS.get(target);
        if (shield == null || shield.until <= now(target) || !target.hasEffect(ModParticleEffects.SHIELD)) return;
        float used = event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION);
        if (used <= 0) return;
        float remaining = Math.max(0, Math.min(target.getAbsorptionAmount(), shield.remaining - used));
        if (remaining > 0) { rememberShield(target, new Shield(shield.until, remaining, shield.count)); return; }
        var level = target.level();
        Vec3 center = target.position();
        SHIELDS.remove(target);
        target.getPersistentData().remove(SHIELD_REMAINDER);
        target.removeEffect(ModParticleEffects.SHIELD);
        if (!isShieldRecipient(target, level)) return;
        int n = shield.count;
        float personal = ParticleStats.holyBreakHeal(n);
        if (personal > 0) heal(target, personal);
        if (n >= 20 && target instanceof ServerPlayer player && isShieldRecipient(target, level))
            healAllies(player, center, ParticleStats.holyBreakRadius(n), ParticleStats.holyBreakAllyHeal(n), false);
    }

    private static boolean isShieldRecipient(LivingEntity target, net.minecraft.world.level.Level level) {
        return !level.isClientSide && target.level() == level && target.isAlive() && !target.isRemoved()
                && (!(target instanceof ServerPlayer player) || activeOwner(player, level));
    }

    private static void healAllies(ServerPlayer player, Vec3 center, double radius, float amount, boolean self) {
        healAlliesWhen(player, center, radius, amount, self, () -> true);
    }

    private static void healAlliesWhen(ServerPlayer player, Vec3 center, double radius, float amount, boolean self, BooleanSupplier current) {
        ServerLevel level = player.serverLevel();
        if (!current.getAsBoolean()) return;
        if (!activeOwner(player, level)) return;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            if (!current.getAsBoolean()) break;
            if (!activeOwner(player, level)) break;
            if (entity.level() == level && !entity.isRemoved() && entity.distanceToSqr(center) <= radius * radius && entity.isAlive()
                    && (self && entity == player || entity != player && player.isAlliedTo(entity))) heal(entity, amount);
        }
    }

    private static void field(ServerPlayer player, ParticleKind kind, Vec3 center, double radius, int duration) {
        Map<ParticleKind, Field> fields = FIELDS.computeIfAbsent(player, ignored -> new HashMap<>());
        Field existing = fields.get(kind);
        long time = now(player);
        if (existing != null && existing.level == player.serverLevel() && existing.until > time) {
            existing.center = center; existing.radius = Math.max(existing.radius, radius);
            existing.durationTicks = Math.max(existing.durationTicks, duration); existing.until = time + existing.durationTicks;
        } else fields.put(kind, new Field(player.serverLevel(), center, radius, time, duration));
    }

    public static void killed(ServerPlayer player, LivingEntity target, ParticleAttack attack) {
        ServerLevel level = player.serverLevel();
        if (!isImpactCurrent(player, target, level)) return;
        Vec3 center = target.position();
        int fire = attack.data().count(ParticleKind.FIRE), venom = attack.data().count(ParticleKind.VENOM);
        int v = attack.data().count(ParticleKind.VOID), shadow = attack.data().count(ParticleKind.SHADOW);
        if (fire >= 10) areaDamage(player, center, ParticleStats.fireExplosionRadius(fire), ParticleStats.fireExplosionDamage(fire), ParticleKind.FIRE, target);
        if (!isImpactCurrent(player, target, level)) return;
        if (venom >= 20) field(player, ParticleKind.VENOM, center, ParticleStats.venomFieldRadius(venom), ParticleStats.venomFieldTicks(venom));
        if (v >= 10 && !(target instanceof Player)) player.heal(player.getMaxHealth() * ParticleStats.voidKillHeal(v));
        if (!isImpactCurrent(player, target, level)) return;
        if (shadow >= 10) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ParticleStats.shadowInvisibleTicks(shadow), 0, false, false, true));
        }
    }

    private static void damage(ServerPlayer player, LivingEntity target, float amount, ParticleKind kind, ParticleAttack dot) {
        ServerLevel level = player.serverLevel();
        if (!target.isAlive() || !isImpactCurrent(player, target, level) || immune(player, target, kind) || amount <= 0) return;
        DamageSource source = source(player, kind);
        WeaponEffectGuard.run(player, () -> {
            DotKill previous = DOT_KILL.get();
            if (dot != null) DOT_KILL.set(new DotKill(target, source, player, dot, level)); else DOT_KILL.remove();
            try { target.hurt(source, amount); }
            finally { if (previous == null) DOT_KILL.remove(); else DOT_KILL.set(previous); }
        });
    }
    private static void areaDamage(ServerPlayer player, Vec3 center, double radius, float amount, ParticleKind kind, LivingEntity excluded) {
        areaDamageWhen(player, center, radius, amount, kind, excluded, () -> true);
    }

    private static void areaDamageWhen(ServerPlayer player, Vec3 center, double radius, float amount, ParticleKind kind,
                                       LivingEntity excluded, BooleanSupplier current) {
        ServerLevel level = player.serverLevel();
        if (!current.getAsBoolean()) return;
        if (!activeOwner(player, level)) return;
        double r = Math.min(7, radius);
        for (LivingEntity other : targets(player, center, r)) {
            if (!current.getAsBoolean()) break;
            if (!activeOwner(player, level)) break;
            // 伤害回调可能传送后续目标；候选列表不能替代当前世界、距离与视线校验。
            if (other != excluded && other.level() == level && other.distanceToSqr(center) <= r * r && player.hasLineOfSight(other))
                damage(player, other, amount, kind, null);
        }
        if (current.getAsBoolean()) ParticleFx.ring(level, center.add(0, 0.15, 0), radius, kind);
    }
    public static void dotDeath(LivingEntity target, DamageSource source) {
        DotKill kill = DOT_KILL.get();
        if (kill != null && kill.target == target && kill.source == source) {
            DOT_KILL.remove();
            if (isImpactCurrent(kill.player, target, kill.level)) killed(kill.player, target, kill.attack);
        }
    }

    private static void shadowSpeed(ServerPlayer player) {
        player.getPersistentData().remove("ue_particle_shadow_speed");
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        int bonus = player.isAlive() && !player.isSpectator() && player.isInvisible()
                ? ParticleStats.shadowSpeedPercent(ParticleRules.data(player.getMainHandItem()).count(ParticleKind.SHADOW)) : 0;
        var current = speed.getModifier(SHADOW_SPEED);
        if (bonus == 0) {
            if (current != null) speed.removeModifier(SHADOW_SPEED);
        } else if (current == null || current.amount() != bonus / 100.0
                || current.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            if (current != null) speed.removeModifier(SHADOW_SPEED);
            speed.addTransientModifier(new AttributeModifier(SHADOW_SPEED, bonus / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fireTick(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.ON_FIRE) && event.getEntity().hasEffect(ModParticleEffects.FIRE)
                && hasDot(event.getEntity(), ParticleKind.FIRE)) event.setCanceled(true);
    }

    private static boolean activeDot(LivingEntity target, Dot dot, long time) {
        return dot.until >= time && target.isAlive() && !target.isRemoved()
                && target.hasEffect(dot.kind == ParticleKind.FIRE ? ModParticleEffects.FIRE : ModParticleEffects.VENOM)
                && (dot.kind != ParticleKind.FIRE || target.isOnFire() && !target.isInWaterRainOrBubble());
    }

    @SubscribeEvent public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide) return;
        List<Dot> dots = DOTS.get(target);
        if (!(target instanceof ServerPlayer) && dots == null
                && !CONTROLS.containsKey(target) && !SHIELDS.containsKey(target)) return;
        long time = now(target);
        if (dots != null) {
            dots.removeIf(dot -> !activeDot(target, dot, time));
            for (Dot dot : List.copyOf(dots)) {
                // 伤害回调可以净化并重施状态，快照中的旧层不能继续结算。
                if (DOTS.get(target) != dots || !dots.contains(dot)) continue;
                if (!activeDot(target, dot, time)) { dots.remove(dot); continue; }
                ServerPlayer player = target.getServer().getPlayerList().getPlayer(dot.attack.owner());
                if (!activeOwner(player, target.level()) || immune(player, target, dot.kind)) { dots.remove(dot); continue; }
                if (dot.next > time) continue;
                dot.next = time + dot.interval;
                float amount = dot.kind == ParticleKind.VENOM ? Math.min(1, target.getHealth() - 1) : 1;
                damage(player, target, amount, dot.kind, dot.attack);
                if (!activeDot(target, dot, time)) dots.remove(dot);
            }
            if (dots.isEmpty() && DOTS.get(target) == dots) DOTS.remove(target);
            if (!hasDot(target, ParticleKind.FIRE)) target.removeEffect(ModParticleEffects.FIRE);
            if (!hasDot(target, ParticleKind.VENOM)) target.removeEffect(ModParticleEffects.VENOM);
        }
        Control control = CONTROLS.get(target);
        if (control != null) {
            var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (control.until <= time || !target.hasEffect(ModParticleEffects.FROST) || !target.isAlive()) {
                CONTROLS.remove(target); if (speed != null) speed.removeModifier(SLOW);
            } else {
                boolean frozen = control.frozenUntil > time;
                if (speed != null) {
                    double amount = frozen ? -1 : -control.slow;
                    var current = speed.getModifier(SLOW);
                    if (amount == 0) {
                        if (current != null) speed.removeModifier(SLOW);
                    } else if (current == null || current.amount() != amount
                            || current.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                        if (current != null) speed.removeModifier(SLOW);
                        speed.addTransientModifier(new AttributeModifier(SLOW, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                }
                if (frozen) target.setDeltaMovement(target.getDeltaMovement().multiply(0, 1, 0));
            }
        }
        Shield shield = SHIELDS.get(target);
        if (shield != null && (shield.until <= time || !target.hasEffect(ModParticleEffects.SHIELD))) {
            clearShield(target);
            target.removeEffect(ModParticleEffects.SHIELD);
        }
        if (target instanceof ServerPlayer player) {
            shadowSpeed(player);
            if (player.tickCount % 8 == 0) ParticleFx.held(player);
        }
    }

    @SubscribeEvent public static void playerTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) trackFall(player);
    }

    public static void effectRemoved(LivingEntity target, MobEffectInstance effect) {
        clearEffect(target, effect.getEffect());
    }

    @SubscribeEvent
    public static void effectAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        StatusApplication attempt = APPLICATION.get();
        if (attempt != null && attempt.target == event.getEntity() && attempt.effect == event.getEffectInstance()) attempt.accepted = true;
    }

    private static boolean applyStatus(LivingEntity target, MobEffectInstance effect) {
        StatusApplication previous = APPLICATION.get();
        for (StatusApplication pending = previous; pending != null; pending = pending.previous)
            if (pending.target == target && pending.effect.getEffect().equals(effect.getEffect())) return false;
        StatusApplication attempt = new StatusApplication(target, effect, previous);
        APPLICATION.set(attempt);
        try {
            // addEffect 返回 false 也可能只是标记无需更新；Added 才表示本次施加被允许。
            target.addEffect(effect);
            return attempt.accepted && target.hasEffect(effect.getEffect());
        } finally {
            if (previous == null) APPLICATION.remove(); else APPLICATION.set(previous);
        }
    }

    private static void rememberShield(LivingEntity target, Shield shield) {
        SHIELDS.put(target, shield);
        // 只用于重载时移除本系统的剩余吸收值，不恢复跨会话护盾。
        target.getPersistentData().putFloat(SHIELD_REMAINDER, shield.remaining);
    }

    private static void clearShield(LivingEntity target) {
        if (target.level().isClientSide) return;
        Shield shield = SHIELDS.remove(target);
        float remaining = shield == null ? target.getPersistentData().getFloat(SHIELD_REMAINDER) : shield.remaining;
        target.getPersistentData().remove(SHIELD_REMAINDER);
        if (Float.isFinite(remaining) && remaining > 0) target.setAbsorptionAmount(Math.max(0, target.getAbsorptionAmount() - Math.min(8, remaining)));
    }

    private static boolean hasDot(LivingEntity target, ParticleKind kind) {
        List<Dot> dots = DOTS.get(target);
        return dots != null && dots.stream().anyMatch(dot -> dot.kind == kind);
    }

    private static boolean activeOwner(ServerPlayer player, net.minecraft.world.level.Level level) {
        return player != null && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && !player.isSpectator() && player.level() == level;
    }

    private static void clearEffect(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (target.level().isClientSide) return;
        if (effect.equals(ModParticleEffects.SHIELD)) clearShield(target);
        if (effect.equals(ModParticleEffects.FROST)) {
            CONTROLS.remove(target);
            var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(SLOW);
        }
        ParticleKind kind = effect.equals(ModParticleEffects.FIRE) ? ParticleKind.FIRE
                : effect.equals(ModParticleEffects.VENOM) ? ParticleKind.VENOM : null;
        List<Dot> dots = DOTS.get(target);
        if (kind != null && dots != null) {
            dots.removeIf(dot -> dot.kind == kind);
            if (dots.isEmpty()) DOTS.remove(target);
        }
    }

    @SubscribeEvent public static void loaded(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity target)) return;
        if (!hasDot(target, ParticleKind.FIRE)) target.removeEffect(ModParticleEffects.FIRE);
        if (!hasDot(target, ParticleKind.VENOM)) target.removeEffect(ModParticleEffects.VENOM);
        if (!CONTROLS.containsKey(target)) {
            clearEffect(target, ModParticleEffects.FROST);
            target.removeEffect(ModParticleEffects.FROST);
        }
        if (!SHIELDS.containsKey(target)) {
            clearShield(target);
            target.removeEffect(ModParticleEffects.SHIELD);
        }
    }

    @SubscribeEvent public static void fields(ServerTickEvent.Post event) {
        for (var entry : Map.copyOf(FIELDS).entrySet()) {
            ServerPlayer player = entry.getKey();
            long time = now(player);
            Map<ParticleKind, Field> active = entry.getValue();
            active.entrySet().removeIf(e -> !activeOwner(player, e.getValue().level) || e.getValue().until < time);
            for (var part : Map.copyOf(active).entrySet()) {
                Field field = part.getValue();
                if (FIELDS.get(player) != active) break;
                if (active.get(part.getKey()) != field) continue;
                if (!activeOwner(player, field.level) || field.until < time) { active.remove(part.getKey(), field); continue; }
                if (field.next > time) continue;
                field.next += 20;
                if (!field.level.hasChunkAt(net.minecraft.core.BlockPos.containing(field.center))) continue;
                switch (part.getKey()) {
                    case FIRE, VENOM, HOLY -> fieldPulse(player, part.getKey(), field, active);
                    case FROST -> frostField(player, field, active);
                    default -> { }
                }
            }
            if (active.isEmpty() && FIELDS.get(player) == active) FIELDS.remove(player);
        }
    }

    private static void frostField(ServerPlayer player, Field field, Map<ParticleKind, Field> active) {
        Vec3 center = field.center;
        double radius = Math.min(7, field.radius);
        BooleanSupplier current = () -> currentField(player, ParticleKind.FROST, field, active, center, radius);
        for (LivingEntity target : targets(player, center, radius)) {
            if (!current.getAsBoolean()) return;
            controlWhen(player, target, 0.5F, 0, 0, 0, false, center, 60,
                    () -> current.getAsBoolean() && controlAreaTarget(player, target, field.level, center, radius));
        }
        if (current.getAsBoolean()) ParticleFx.ring(field.level, center, radius, ParticleKind.FROST);
    }

    private static boolean currentField(ServerPlayer player, ParticleKind kind, Field field, Map<ParticleKind, Field> active,
                                        Vec3 center, double radius) {
        return activeOwner(player, field.level) && FIELDS.get(player) == active && active.get(kind) == field
                && field.center.equals(center) && Math.min(7, field.radius) == radius;
    }

    private static void fieldPulse(ServerPlayer player, ParticleKind kind, Field field, Map<ParticleKind, Field> active) {
        Vec3 center = field.center;
        double radius = Math.min(7, field.radius);
        BooleanSupplier current = () -> currentField(player, kind, field, active, center, radius);
        if (kind == ParticleKind.HOLY) {
            healAlliesWhen(player, center, radius, 1, true, current);
            if (current.getAsBoolean()) ParticleFx.ring(field.level, center, radius, kind);
        } else areaDamageWhen(player, center, radius, 1.25F, kind, null, current);
    }

    private static void trackFall(ServerPlayer player) {
        Fall previous = FALLS.get(player);
        boolean continuous = continuousFall(previous, player);
        boolean valid = fallAllowed(player) && (player.onGround() || continuous && previous.valid);
        double descended = valid && continuous && previous.valid && previous.position.y >= player.getY()
                ? previous.descended + previous.position.y - player.getY() : 0;
        if (player.onGround()) descended = 0;
        FALLS.put(player, new Fall(player.level().dimension(), player.position(), now(player), descended, valid));
    }

    private static boolean fallAllowed(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.hasDisconnected() && !player.isSpectator()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isPassenger()
                && !player.isInWaterOrBubble() && !player.onClimbable();
    }

    private static boolean continuousFall(Fall previous, ServerPlayer player) {
        long time = now(player);
        return previous != null && previous.dimension.equals(player.level().dimension())
                && time >= previous.tick && time - previous.tick <= 2 && previous.position.distanceToSqr(player.position()) < 64;
    }

    public static void interruptFall(ServerPlayer player) { FALLS.remove(player); }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void landed(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Fall fall = FALLS.remove(player);
        if (event.isCanceled() || !Float.isFinite(event.getDistance()) || event.getDistance() < 3
                || !fallAllowed(player) || !player.onGround() || !continuousFall(fall, player) || !fall.valid
                || fall.descended + Math.max(0, fall.position.y - player.getY()) < 3) return;
        ParticleData data = ParticleRules.data(player.getMainHandItem());
        int n = data.count(ParticleKind.EARTH);
        if (n < 10 || player.getPersistentData().getLong("ue_particle_earth_ready") > now(player)) return;
        player.getPersistentData().putLong("ue_particle_earth_ready", now(player) + 60);
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        double radius = ParticleStats.earthLandingRadius(n);
        areaDamage(player, center, radius, ParticleStats.earthLandingDamage(n), ParticleKind.EARTH, null);
        if (n >= 35 && activeOwner(player, level)) for (LivingEntity target : targets(player, center, radius)) {
            if (!activeOwner(player, level)) break;
            controlWhen(player, target, 0, 0, 0, ParticleStats.earthDistance(n), n >= 45, center, 60,
                    () -> controlAreaTarget(player, target, level, center, radius));
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        FALLS.remove(player); FIELDS.remove(player);
        for (var entry : List.copyOf(DOTS.entrySet())) {
            entry.getValue().removeIf(dot -> dot.attack.owner().equals(player.getUUID()));
            LivingEntity target = entry.getKey();
            if (entry.getValue().isEmpty()) DOTS.remove(target);
            if (!hasDot(target, ParticleKind.FIRE)) target.removeEffect(ModParticleEffects.FIRE);
            if (!hasDot(target, ParticleKind.VENOM)) target.removeEffect(ModParticleEffects.VENOM);
        }
        clearEntity(player);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SHADOW_SPEED);
        player.getPersistentData().remove("ue_particle_shadow_speed");
        player.getPersistentData().remove("ue_particle_shadow_ready");
    }

    private static void clearEntity(LivingEntity target) {
        for (var effect : List.of(ModParticleEffects.FIRE, ModParticleEffects.VENOM, ModParticleEffects.FROST, ModParticleEffects.SHIELD)) {
            clearEffect(target, effect);
            target.removeEffect(effect);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player);
        else clearEntity(event.getEntity());
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player); }
    @SubscribeEvent public static void teleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent event) { if (event.getEntity() instanceof ServerPlayer player) interruptFall(player); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player); }
    public static void clear() { DOTS.clear(); CONTROLS.clear(); SHIELDS.clear(); FIELDS.clear(); FALLS.clear(); }
}
