package cn.kurt6.unknown_echoes.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class FoodEffectSupport {
    private FoodEffectSupport() {
    }

    /**
     * 周期性效果是否需要续期:更高等级的外部效果与无限时长(duration < 0)都不打断,
     * 剩余时长掉到本次时长一半以内才续期。夜视阈值更严,走 NightVisionSupport。
     */
    public static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                             int durationTicks, int amplifier) {
        return needsEffectRefresh(entity.getEffect(effect), durationTicks, amplifier);
    }

    /** 已经持有 MobEffectInstance 时的重载,避免重复查询。 */
    public static boolean needsEffectRefresh(MobEffectInstance current,
                                             int durationTicks, int amplifier) {
        if (current == null || current.getAmplifier() < amplifier) {
            return true;
        }
        return current.getDuration() >= 0
                && current.getDuration() <= Math.max(1, durationTicks / 2);
    }

    public static boolean needsAbsorptionRefresh(LivingEntity entity, int durationTicks,
                                                 int amplifier, float absorptionHealth) {
        MobEffectInstance current = entity.getEffect(MobEffects.ABSORPTION);
        if (current == null || current.getAmplifier() < amplifier) {
            return true;
        }
        if (current.getAmplifier() > amplifier) {
            return current.getDuration() >= 0
                    && current.getDuration() <= Math.max(1, durationTicks / 2);
        }
        return entity.getAbsorptionAmount() + 0.01F < absorptionHealth
                || current.getDuration() >= 0
                && current.getDuration() <= Math.max(1, durationTicks / 2);
    }

    /** 同级护盾被打穿时先移除再重加，确保食用确实恢复临时生命。 */
    public static void applyAbsorption(LivingEntity entity, int durationTicks,
                                       int amplifier, float absorptionHealth) {
        MobEffectInstance current = entity.getEffect(MobEffects.ABSORPTION);
        if (current != null && current.getAmplifier() == amplifier
                && entity.getAbsorptionAmount() + 0.01F < absorptionHealth) {
            entity.removeEffect(MobEffects.ABSORPTION);
            current = null;
        }
        boolean higherEffectIsNearExpiry = current != null
                && current.getAmplifier() > amplifier
                && current.getDuration() >= 0
                && current.getDuration() <= Math.max(1, durationTicks / 2);
        if (current == null || current.getAmplifier() <= amplifier || higherEffectIsNearExpiry) {
            if (current != null && current.getAmplifier() < amplifier) {
                entity.removeEffect(MobEffects.ABSORPTION);
            }
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    durationTicks, amplifier, true, false, true));
        }
    }
}
