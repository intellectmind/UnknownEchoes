package cn.kurt6.unknown_echoes.artifact;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** 将潮汐归属提交放在原生效果写入之后、效果附加回调之前。 */
public final class TideEffectApplication {
    private static final Map<LivingEntity, Frame> ACTIVE = new IdentityHashMap<>();

    @FunctionalInterface
    public interface Tracker {
        void track(MobEffectInstance original, MobEffectInstance before, MobEffectInstance after,
                   float absorptionBefore, boolean started);
    }

    private static final class Frame {
        final ServerPlayer player;
        final Holder<MobEffect> effect;
        final Level level;
        final Frame previous;
        final Supplier<?> ownership;
        final Tracker tracker;
        final BooleanSupplier canContinue;
        boolean canceled;
        MobEffectInstance incoming;
        MobEffectInstance original;
        MobEffectInstance before;
        MobEffectInstance committed;
        CompoundTag snapshot;
        Object claim;
        float absorptionBefore;

        Frame(ServerPlayer player, Holder<MobEffect> effect, Supplier<?> ownership, Tracker tracker, BooleanSupplier canContinue) {
            this.player = player;
            this.effect = effect;
            this.level = player.level();
            this.previous = ACTIVE.get(player);
            this.ownership = ownership;
            this.tracker = tracker;
            this.canContinue = canContinue;
            this.claim = ownership.get();
        }

        boolean valid() {
            return !canceled && player.level() == level && player.isAlive() && !player.isSpectator() && canContinue.getAsBoolean();
        }
    }

    private TideEffectApplication() {}

    public static void apply(ServerPlayer player, Holder<MobEffect> effect, Supplier<?> ownership,
                             Tracker tracker, Runnable operation, BooleanSupplier canContinue) {
        Frame frame = new Frame(player, effect, ownership, tracker, canContinue);
        ACTIVE.put(player, frame);
        try {
            if (frame.valid()) operation.run();
        } finally {
            try {
                if (player.getEffect(effect) == null && frame.claim == ownership.get()) {
                    tracker.track(frame.original, frame.before, null, frame.absorptionBefore, false);
                }
            } finally {
                if (frame.previous == null) ACTIVE.remove(player);
                else ACTIVE.put(player, frame.previous);
            }
        }
    }

    public static void cancel(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer)) return;
        for (Frame frame = ACTIVE.get(entity); frame != null; frame = frame.previous) {
            frame.canceled = true;
        }
    }

    public static void cancelAll() {
        for (Frame frame : ACTIVE.values()) {
            for (Frame current = frame; current != null; current = current.previous) current.canceled = true;
        }
    }

    private static Frame frame(LivingEntity entity, MobEffectInstance incoming) {
        if (!(entity instanceof ServerPlayer)) return null;
        Frame frame = ACTIVE.get(entity);
        return frame != null && frame.incoming == incoming ? frame : null;
    }

    public static void enter(LivingEntity entity, MobEffectInstance incoming) {
        if (!(entity instanceof ServerPlayer)) return;
        Frame frame = ACTIVE.get(entity);
        if (frame != null && frame.incoming == null && frame.effect.equals(incoming.getEffect())) {
            frame.incoming = incoming;
        }
    }

    public static boolean canApply(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        return frame == null || frame.valid();
    }

    public static void capture(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        if (frame == null) return;
        MobEffectInstance original = entity.getEffect(frame.effect);
        frame.original = original;
        frame.snapshot = original == null ? null : (CompoundTag) original.save();
        frame.before = frame.snapshot == null ? null : MobEffectInstance.load(frame.snapshot);
        frame.absorptionBefore = incoming.is(MobEffects.ABSORPTION) ? entity.getAbsorptionAmount() : 0.0F;
        frame.claim = frame.ownership.get();
    }

    public static boolean canCommit(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        return frame == null || frame.valid() && frame.original == entity.getEffect(frame.effect)
                && frame.claim == frame.ownership.get()
                && (frame.original == null || frame.snapshot.equals(frame.original.save()));
    }

    public static void committed(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        if (frame == null) return;
        frame.committed = entity.getEffect(frame.effect);
        frame.tracker.track(frame.original, frame.before, frame.committed, frame.absorptionBefore, false);
        frame.claim = frame.ownership.get();
    }

    public static boolean canStart(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        return frame == null || frame.valid() && frame.committed == entity.getEffect(frame.effect)
                && frame.claim == frame.ownership.get();
    }

    public static void started(LivingEntity entity, MobEffectInstance incoming) {
        Frame frame = frame(entity, incoming);
        if (frame == null || !canStart(entity, incoming)) return;
        frame.tracker.track(frame.original, frame.before, frame.committed, frame.absorptionBefore, true);
        frame.claim = frame.ownership.get();
    }
}
