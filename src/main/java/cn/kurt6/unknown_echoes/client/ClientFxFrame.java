package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.fx.CombatFxGeometry;
import cn.kurt6.unknown_echoes.particle.ParticleItemGeometry;
import java.util.function.DoubleFunction;
import java.util.function.BooleanSupplier;

public record ClientFxFrame(CombatFxPayload initial, DoubleFunction<CombatFxPayload> following, BooleanSupplier active) {
    private static final BooleanSupplier ALWAYS_ACTIVE = () -> true;

    public ClientFxFrame(CombatFxPayload initial, DoubleFunction<CombatFxPayload> following) {
        this(initial, following, ALWAYS_ACTIVE);
    }

    public boolean refreshable() {
        return following == null && active == ALWAYS_ACTIVE;
    }

    public CombatFxPayload at(double partialTick) {
        if (!active.getAsBoolean()) return null;
        CombatFxPayload frame = following == null ? initial : following.apply(Math.max(0, Math.min(1, partialTick)));
        return frame != null && frame.valid() ? frame : null;
    }

    public float opacity(double age, double tick) {
        return following == null ? CombatFxGeometry.opacity(initial, age) : ParticleItemGeometry.opacity(tick);
    }
}
