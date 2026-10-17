package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.fx.StormVolumeGeometry;
import cn.kurt6.unknown_echoes.fx.DisasterAtmosphereProfile;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StormSceneState {
    public static final int MAX_SOURCES = 16;
    public static final int TTL = 30;
    private final Map<Key, Scene> scenes = new LinkedHashMap<>();
    private long clock;

    public record Key(Vec3 center, long eventStart) { }

    public static final class Scene {
        private RealmDisasterPayload payload;
        private long expiresAt;
        private float strength;
        private float previous;

        public RealmDisasterPayload payload() { return payload; }
        public float strength(float partial) { return previous + (strength - previous) * Math.clamp(partial, 0, 1); }
        public float visualStrength(float partial, Vec3 viewer) {
            return strength(partial) * DisasterAtmosphereProfile.visibility(payload.center().distanceTo(viewer));
        }
        public Key key() { return new Key(payload.center(), payload.eventStart()); }
    }

    public void accept(RealmDisasterPayload payload) {
        if (!payload.valid() || payload.impact()) return;
        Key key = new Key(payload.center(), payload.eventStart());
        if (payload.strength() == 0 || payload.sceneHeight() < 8) {
            Scene existing = scenes.get(key);
            if (existing != null) existing.expiresAt = clock;
            return;
        }
        if (!scenes.containsKey(key) && scenes.size() >= MAX_SOURCES) scenes.remove(scenes.keySet().iterator().next());
        Scene scene = scenes.computeIfAbsent(key, ignored -> new Scene());
        scene.payload = payload;
        scene.expiresAt = clock + TTL;
    }

    public void tick() {
        clock++;
        scenes.values().removeIf(scene -> {
            scene.previous = scene.strength;
            float target = clock < scene.expiresAt ? scene.payload.strength() : 0;
            scene.strength += (target - scene.strength) * 0.12F;
            return target == 0 && scene.strength < 0.001F;
        });
    }

    public List<Scene> visible(Vec3 viewer) {
        return scenes.values().stream()
                .filter(scene -> scene.payload.center().distanceToSqr(viewer) < StormVolumeGeometry.VIEW_DISTANCE * StormVolumeGeometry.VIEW_DISTANCE)
                .sorted(Comparator.comparingDouble(scene -> scene.payload.center().distanceToSqr(viewer)))
                .limit(StormVolumeGeometry.MAX_VISIBLE_STORMS).toList();
    }

    public long clock() { return clock; }
    public int size() { return scenes.size(); }
    public void clear() { scenes.clear(); clock = 0; }
}
