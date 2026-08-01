package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 主线 Boss 破防窗口的阶段地板。
 * 高伤害只能把 Boss 推到下一段血线，不能在同一次机制窗口跨多个阶段。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
final class BossPhaseGate {
    private static final float EPSILON = 0.01F;

    private BossPhaseGate() {
    }

    static float nextFloor(float health, float maxHealth, float... phaseFractions) {
        for (float fraction : phaseFractions) {
            float floor = maxHealth * fraction;
            if (health > floor + EPSILON) {
                return floor;
            }
        }
        return 0.0F;
    }

    static float capDamage(float health, float amount, float floor) {
        if (floor <= 0.0F) {
            return amount;
        }
        return Math.min(amount, Math.max(0.0F, health - floor));
    }

    static boolean shouldCapMechanicDamage(DamageSource source) {
        return !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    static boolean reachedFloor(LivingEntity entity, float floor) {
        return floor > 0.0F && entity.isAlive() && entity.getHealth() <= floor + EPSILON;
    }

    /** 在所有同类增伤处理后应用未破防倍率，防止平面增伤绕过机制保护。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuardedBossIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof BossDamageGuard guard)
                || !guard.isMechanicDamageGuarded()
                || !guard.shouldGuardDamage(event.getSource())) {
            return;
        }
        float damage = event.getAmount();
        if (!(damage > 0.0F)) {
            return;
        }
        float multiplier = ServerConfig.BOSS_UNBROKEN_DAMAGE_MULTIPLIER.get().floatValue();
        float cap = ServerConfig.BOSS_UNBROKEN_MAX_DAMAGE.get().floatValue();
        event.setAmount(Math.min(damage * multiplier, cap));
        guard.onMechanicDamageGuarded(event.getSource());
    }

    /** 所有增伤与减伤结算后再钳制，避免其他事件越过单次上限或阶段血线。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalBossDamage(LivingDamageEvent.Pre event) {
        float damage = event.getNewDamage();
        float capped = damage;
        if (event.getEntity() instanceof BossDamageGuard guard
                && guard.isMechanicDamageGuarded()
                && guard.shouldGuardDamage(event.getSource())) {
            capped = Math.min(capped, ServerConfig.BOSS_UNBROKEN_MAX_DAMAGE.get().floatValue());
        }
        PhaseGatedBoss phaseBoss = event.getEntity() instanceof PhaseGatedBoss boss ? boss : null;
        if (phaseBoss != null && shouldCapMechanicDamage(event.getSource())) {
            capped = capDamage(event.getEntity().getHealth(), capped, phaseBoss.phaseDamageFloor());
        }
        event.setNewDamage(capped);
        if (phaseBoss != null && damage > 0.0F && capped <= 0.0F) {
            phaseBoss.onPhaseDamageBlocked(event.getSource());
        }
    }
}

interface PhaseGatedBoss {
    float phaseDamageFloor();

    default void onPhaseDamageBlocked(DamageSource source) {
    }
}

interface BossDamageGuard {
    boolean isMechanicDamageGuarded();

    default boolean shouldGuardDamage(DamageSource source) {
        return BossPhaseGate.shouldCapMechanicDamage(source);
    }

    default void onMechanicDamageGuarded(DamageSource source) {
    }
}
