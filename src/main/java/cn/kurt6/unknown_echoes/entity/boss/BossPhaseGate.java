package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * 主线 Boss 破防窗口的阶段地板。
 * 高伤害只能把 Boss 推到下一段血线，不能在同一次机制窗口跨多个阶段。
 */
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
}
