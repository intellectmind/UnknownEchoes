package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class BossHealthSupport {
    private static final double VANILLA_HEALTH_LIMIT = 1024.0D;

    private BossHealthSupport() { }

    public static void initialize(LivingEntity entity) {
        if (!(entity instanceof BossDamageGuard) && !(entity instanceof PhaseGatedBoss)) return;
        var attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            ((BossHealthAttribute) attribute).unknownEchoes$enableBossHealth();
        }
    }

    /** 只解除正向上界截断；保留下界、无效数值与第三方额外修正。 */
    public static double expandUpperBound(Attribute attribute, double requested, double sanitized) {
        if (!(attribute instanceof RangedAttribute range) || !Double.isFinite(requested)
                || range.getMaxValue() < VANILLA_HEALTH_LIMIT
                || requested <= range.getMaxValue() || sanitized != range.getMaxValue()) {
            return sanitized;
        }
        return Math.max(sanitized, Math.min(requested, Float.MAX_VALUE));
    }
}
