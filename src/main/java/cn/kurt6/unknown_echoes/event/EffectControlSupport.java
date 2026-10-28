package cn.kurt6.unknown_echoes.event;

import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class EffectControlSupport {
    private static final ThreadLocal<Map<LivingEntity, Change>> CHANGES =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private record Change(MobEffectInstance expected, Tag snapshot, MobEffectInstance replacement,
                          BooleanSupplier eligible) {}

    private EffectControlSupport() {}

    public static void shorten(ServerPlayer player, Holder<MobEffect> effect, int maxDuration,
                               BooleanSupplier eligible) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.endsWithin(maxDuration) || instance.getAmplifier() > 1) {
            return;
        }
        change(player, instance, new MobEffectInstance(effect, maxDuration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon()), eligible);
    }

    public static boolean remove(ServerPlayer player, Holder<MobEffect> effect, BooleanSupplier eligible) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null) {
            return change(player, instance, null, eligible);
        }
        return false;
    }

    private static boolean change(ServerPlayer player, MobEffectInstance expected, MobEffectInstance replacement,
                                  BooleanSupplier eligible) {
        Map<LivingEntity, Change> changes = CHANGES.get();
        if (changes.containsKey(player) || !eligible.getAsBoolean()) {
            return false;
        }
        changes.put(player, new Change(expected, expected.save(), replacement, eligible));
        try {
            boolean removed = player.removeEffect(expected.getEffect());
            if (removed && replacement != null
                    && canApply(player, replacement)) {
                player.addEffect(replacement);
            }
            return removed;
        } finally {
            changes.remove(player);
        }
    }

    /** 在移除事件之后复核，避免按类型删除回调中新写入或续期的效果。 */
    public static boolean canRemove(LivingEntity entity, Holder<MobEffect> effect) {
        Change change = CHANGES.get().get(entity);
        return change == null || !change.expected().getEffect().equals(effect)
                || change.eligible().getAsBoolean() && entity.getEffect(effect) == change.expected()
                && change.expected().save().equals(change.snapshot());
    }

    /** 仅约束本次缩短的替代实例，其他来源的效果仍按原生流程处理。 */
    public static boolean canApply(LivingEntity entity, MobEffectInstance incoming) {
        Change change = CHANGES.get().get(entity);
        return change == null || change.replacement() != incoming
                || change.eligible().getAsBoolean() && entity.getEffect(incoming.getEffect()) == null;
    }
}
