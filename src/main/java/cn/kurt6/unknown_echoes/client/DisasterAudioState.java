package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.fx.DisasterAftermathGeometry;
import cn.kurt6.unknown_echoes.fx.DisasterAtmosphereProfile;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class DisasterAudioState {
    public static final int MAX_PENDING = 12;
    public static final int MERGE_TICKS = 4;
    private final List<Pending> pending = new ArrayList<>();
    private long clock;
    private long lastCrack = -MERGE_TICKS;
    private long lastRumble = -MERGE_TICKS;

    public record Cue(Vec3 center, boolean deep, boolean rumble, float volume, long eventStart) { }

    private static final class Pending {
        private final RealmDisasterPayload payload;
        private final long started;
        private boolean cracked;
        private boolean rumbled;

        private Pending(RealmDisasterPayload payload, long started) {
            this.payload = payload;
            this.started = started;
        }
    }

    public void accept(RealmDisasterPayload payload) {
        if (payload.valid() && !payload.impact() && payload.strength() == 0) {
            pending.removeIf(sound -> sound.payload.eventStart() == payload.eventStart());
            return;
        }
        if (!payload.valid() || !payload.impact() || payload.strength() <= 0) return;
        if (pending.size() >= MAX_PENDING) pending.removeFirst();
        pending.add(new Pending(payload, clock));
    }

    public List<Cue> tick(Vec3 viewer) {
        pending.removeIf(sound -> clock - sound.started >= DisasterAftermathGeometry.WAVE_LIFETIME);
        Cue crack = null, rumble = null;
        for (Pending sound : pending) {
            var p = sound.payload;
            double distance = viewer.distanceTo(p.center());
            if (!sound.cracked) {
                sound.cracked = true;
                Cue cue = new Cue(p.center(), p.deep(), false, DisasterAtmosphereProfile.impactVolume(distance, p.radius(), p.strength(), false), p.eventStart());
                if (crack == null || cue.volume() > crack.volume()) crack = cue;
            }
            if (!sound.rumbled && clock - sound.started > distance / DisasterAftermathGeometry.speed(p.deep())) {
                sound.rumbled = true;
                Cue cue = new Cue(p.center(), p.deep(), true, DisasterAtmosphereProfile.impactVolume(distance, p.radius(), p.strength(), true), p.eventStart());
                if (rumble == null || cue.volume() > rumble.volume()) rumble = cue;
            }
        }
        pending.removeIf(sound -> sound.cracked && sound.rumbled);
        List<Cue> cues = new ArrayList<>(2);
        if (crack != null && crack.volume() > 0.001F && clock - lastCrack >= MERGE_TICKS) {
            cues.add(crack);
            lastCrack = clock;
        }
        if (rumble != null && rumble.volume() > 0.001F && clock - lastRumble >= MERGE_TICKS) {
            cues.add(rumble);
            lastRumble = clock;
        }
        clock++;
        return cues;
    }

    public int size() { return pending.size(); }
    public long clock() { return clock; }
    public void clear() {
        pending.clear();
        clock = 0;
        lastCrack = lastRumble = -MERGE_TICKS;
    }
}
