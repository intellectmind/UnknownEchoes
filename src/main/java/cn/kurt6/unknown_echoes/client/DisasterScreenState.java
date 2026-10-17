package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import cn.kurt6.unknown_echoes.fx.DisasterAftermathGeometry;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class DisasterScreenState {
    public static final int MAX_SOURCES = 16;
    public static final int SOURCE_TTL_TICKS = 25;
    private final Map<Key, Source> sources = new LinkedHashMap<>();
    private final List<Wave> waves = new ArrayList<>();
    private long clock;
    private float strength;
    private float previousStrength;
    private float impact;
    private float previousImpact;
    private Vec3 impactDirection = Vec3.ZERO;
    private Vec3 previousDirection = Vec3.ZERO;
    private boolean deep;

    private record Source(RealmDisasterPayload payload, long expiresAt) { }
    private record Key(Vec3 center, long eventStart) { }
    private record Wave(RealmDisasterPayload payload, long started) { }

    public void accept(RealmDisasterPayload payload, Vec3 viewer) {
        if (!payload.valid()) return;
        Key key = new Key(payload.center(), payload.eventStart());
        if (payload.impact()) {
            if (waves.size() >= DisasterAftermathGeometry.MAX_WAVES) waves.removeFirst();
            waves.add(new Wave(payload, clock));
        } else if (payload.strength() == 0) {
            sources.remove(key);
            waves.removeIf(wave -> wave.payload().eventStart() == payload.eventStart());
        } else {
            if (!sources.containsKey(key) && sources.size() >= MAX_SOURCES) {
                sources.remove(sources.keySet().iterator().next());
            }
            sources.put(key, new Source(payload, clock + SOURCE_TTL_TICKS));
        }
    }

    public void tick(Vec3 viewer) {
        previousStrength = strength;
        previousImpact = impact;
        previousDirection = impactDirection;
        sources.values().removeIf(source -> source.expiresAt() <= clock);
        float target = 0;
        for (Source source : sources.values()) {
            float contribution = proximity(source.payload(), viewer) * source.payload().strength();
            if (contribution > target) {
                target = contribution;
                deep = source.payload().deep();
            }
        }
        strength += (target - strength) * 0.12F;
        if (target == 0 && strength < 0.001F) strength = 0;
        waves.removeIf(wave -> clock - wave.started() >= DisasterAftermathGeometry.WAVE_LIFETIME);
        impact = 0;
        impactDirection = Vec3.ZERO;
        for (Wave wave : waves) {
            var payload = wave.payload();
            double distance = payload.center().distanceTo(viewer);
            double arrival = distance / DisasterAftermathGeometry.speed(payload.deep());
            float received = proximity(payload, viewer) * payload.strength()
                    * DisasterAftermathGeometry.pulse(clock - wave.started() - arrival);
            if (received > impact) {
                impact = received;
                deep = payload.deep();
                impactDirection = viewer.subtract(payload.center()).multiply(1, 0, 1).normalize().scale(received);
            }
        }
        clock++;
    }

    public float strength(float partial) { return interpolate(previousStrength, strength, partial); }
    public float impact(float partial) { return interpolate(previousImpact, impact, partial); }
    public Vec3 impactDirection(float partial) { return previousDirection.lerp(impactDirection, Math.clamp(partial, 0, 1)); }
    public boolean deep() { return deep; }
    public int sourceCount() { return sources.size(); }
    public int waveCount() { return waves.size(); }

    public void clear() {
        sources.clear();
        waves.clear();
        clock = 0;
        strength = previousStrength = impact = previousImpact = 0;
        impactDirection = previousDirection = Vec3.ZERO;
        deep = false;
    }

    private static float interpolate(float before, float after, float partial) {
        return before + (after - before) * Math.clamp(partial, 0.0F, 1.0F);
    }

    private static float proximity(RealmDisasterPayload payload, Vec3 viewer) {
        return (float) Math.clamp(1.0D - viewer.distanceTo(payload.center()) / payload.radius(), 0.0D, 1.0D);
    }
}
