package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.item.weapon.WeaponEffectGuard;
import cn.kurt6.unknown_echoes.mixin.LivingDamageAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ParticleCombat {
    private record Melee(ParticleAttack attack, int target, long tick) { }
    private record Hit(ParticleAttack attack, ServerPlayer player, DamageSource source, float health,
                       float normal, float voidDamage, float extra, float pierce, boolean backstab, float[] shares, HealthLoss healthLoss) { }
    private static final class HealthLoss {
        private float amount;
    }
    private static final Map<ServerPlayer, Melee> MELEE = new WeakHashMap<>();
    private static final Map<DamageContainer, Hit> HITS = new WeakHashMap<>();
    private static final Map<DamageContainer, Hit> KILLS = new WeakHashMap<>();
    private static final Map<DamageContainer, Float> EXECUTION_CREDIT = new WeakHashMap<>();

    private ParticleCombat() { }

    private static DamageContainer currentDamage(LivingEntity target) {
        // Post 和死亡事件不携带容器；受伤栈顶区分嵌套伤害及复用的 DamageSource。
        var containers = ((LivingDamageAccessor) target).unknownEchoes$getDamageContainers();
        return containers.isEmpty() ? null : containers.peek();
    }

    public static void healthChanged(LivingEntity target, float before, float after) {
        if (target.level().isClientSide) return;
        Hit hit = HITS.get(currentDamage(target));
        if (hit != null && Float.isFinite(before) && Float.isFinite(after))
            hit.healthLoss.amount = Math.max(0, Math.min(before, before - after));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ParticleAttack snapshot = ParticleAttack.capture(player, player.getMainHandItem());
            MELEE.remove(player);
            if (snapshot != null) MELEE.put(player, new Melee(snapshot, event.getTarget().getId(), player.level().getGameTime()));
        }
    }

    @SubscribeEvent public static void spawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
                && event.getEntity() instanceof AbstractArrow arrow) ParticleAttack.launched(arrow);
    }

    // BossPhaseGate 的 LOWEST 处理必须看到已合并的粒子伤害。
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getEntity() instanceof ServerPlayer player) || WeaponEffectGuard.isActive(player)
                || !ParticleEffects.canHit(player, target) || event.getAmount() <= 0) return;
        ParticleAttack attack = null;
        AbstractArrow arrow = source.getDirectEntity() instanceof AbstractArrow a ? a : null;
        if (arrow != null && source.is(DamageTypes.ARROW) && !ParticleAttack.seen(arrow, target.getUUID())) {
            attack = ParticleAttack.projectile(arrow, player);
        } else if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            Melee melee = MELEE.get(player);
            if (melee != null && melee.target == target.getId() && melee.tick == player.level().getGameTime()) {
                attack = melee.attack;
                MELEE.remove(player);
            }
        }
        if (attack == null || attack.data().total() == 0) return;
        ParticleData data = attack.data();
        Vec3 towardSource = arrow == null ? player.position().subtract(target.position()) : arrow.getDeltaMovement().scale(-1);
        boolean backstab = target.getLookAngle().multiply(1, 0, 1).dot(towardSource.multiply(1, 0, 1)) < 0;
        boolean frozen = ParticleEffects.frozen(target);
        int frost = data.count(ParticleKind.FROST), shadow = data.count(ParticleKind.SHADOW), voidCount = data.count(ParticleKind.VOID);
        float frostMultiplier = frozen ? ParticleStats.frostFrozenMultiplier(frost) : 1;
        float ordinary = event.getAmount(), voidDamage = 0, particles = 0;
        for (ParticleKind kind : ParticleKind.values()) {
            int count = data.count(kind);
            if (count == 0 || ParticleEffects.immune(player, target, kind)) continue;
            float amount = ParticleRules.DAMAGE_PER_PARTICLE * count * frostMultiplier;
            if (kind == ParticleKind.SHADOW) amount *= 1 + (backstab ? ParticleStats.shadowBackstab(shadow) : 0) + attack.availableShadow(player);
            particles += amount;
            if (kind == ParticleKind.VOID) voidDamage = amount; else ordinary += amount;
        }
        float extra = frozen && frost >= 25 && !ParticleEffects.immune(player, target, ParticleKind.FROST) ? ParticleStats.frostShatter(frost) : 0;
        if (shadow == 50) extra += particles * 0.3F;
        float pierce = voidCount == 0 || ParticleEffects.immune(player, target, ParticleKind.VOID)
                ? 0 : ParticleStats.voidPierce(voidCount);
        Hit hit = new Hit(attack, player, source, target.getHealth(), ordinary, voidDamage, extra, pierce, backstab,
                new float[]{ordinary, voidDamage, extra}, new HealthLoss());
        HITS.put(event.getContainer(), hit);
        event.setAmount(ordinary + voidDamage + extra);
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> {
            float scale = container.getNewDamage() / Math.max(0.001F, hit.normal + hit.voidDamage + hit.extra);
            float armorable = (hit.normal * (1 - hit.pierce) + hit.extra) * scale;
            float removed = reduction;
            if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && (hit.pierce > 0 || hit.voidDamage > 0)) {
                removed = armorable - CombatRules.getDamageAfterAbsorb(target, armorable, source,
                        target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            }
            float fraction = armorable <= 0 ? 0 : Math.min(1, removed / armorable);
            hit.shares[0] = hit.normal * scale * (1 - (1 - hit.pierce) * fraction);
            hit.shares[1] = hit.voidDamage * scale;
            hit.shares[2] = hit.extra * scale * (1 - fraction);
            return removed;
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        DamageContainer container = currentDamage(target);
        Hit hit = HITS.get(container);
        if (hit == null || hit.source != event.getSource() || WeaponEffectGuard.isActive(hit.player)) return;
        HITS.remove(container);
        Float executionCredit = EXECUTION_CREDIT.remove(container);
        if ((event.getNewDamage() <= 0 || hit.healthLoss.amount <= 0) && event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        if (hit.source.getDirectEntity() instanceof AbstractArrow arrow) ParticleAttack.mark(arrow, target.getUUID());
        hit.attack.claimShadow(hit.player);
        if (hit.healthLoss.amount > 0 && target.getHealth() <= 0) KILLS.put(container, hit);
        float healthLoss = Math.max(0, Math.min(hit.healthLoss.amount, event.getNewDamage()));
        if (executionCredit != null) healthLoss = Math.min(healthLoss, Math.max(0, executionCredit - event.getReduction(DamageContainer.Reduction.ABSORPTION)));
        float directAfterArmor = hit.shares[0] + hit.shares[1];
        float totalAfterArmor = directAfterArmor + hit.shares[2];
        int n = hit.attack.data().count(ParticleKind.VOID);
        float basis = hit.voidDamage <= 0 ? 0 : n >= 20 ? directAfterArmor : hit.shares[1];
        float healing = totalAfterArmor <= 0 ? 0 : healthLoss * basis / totalAfterArmor * ParticleStats.voidSteal(n);
        if (healing > 0) hit.player.heal(Math.min(healthLoss * 0.08F, healing));
        ParticleEffects.hit(hit.player, target, hit.attack, hit.backstab);
        ParticleFx.hit(hit.player, target, hit.attack.data());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void execution(LivingDamageEvent.Pre event) {
        Hit hit = HITS.get(event.getContainer());
        if (hit == null || hit.source != event.getSource() || hit.attack.data().count(ParticleKind.VOID) != 50
                || event.getNewDamage() <= 0 || !(event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)
                || ParticleEffects.immune(hit.player, event.getEntity(), ParticleKind.VOID)
                || ParticleEffects.boss(event.getEntity()) || hit.health > event.getEntity().getMaxHealth() * 0.08F) return;
        EXECUTION_CREDIT.put(event.getContainer(), event.getNewDamage());
        event.setNewDamage(Math.max(event.getNewDamage(), event.getEntity().getHealth() + event.getEntity().getAbsorptionAmount()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        Hit hit = KILLS.remove(currentDamage(event.getEntity()));
        if (hit != null && hit.source == event.getSource()) ParticleEffects.killed(hit.player, event.getEntity(), hit.attack);
        else ParticleEffects.dotDeath(event.getEntity(), event.getSource());
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        MELEE.clear(); HITS.clear(); KILLS.clear(); EXECUTION_CREDIT.clear(); ParticleEffects.clear(); ParticleFx.clear();
    }
}
