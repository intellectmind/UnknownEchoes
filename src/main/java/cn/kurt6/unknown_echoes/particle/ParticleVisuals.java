package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class ParticleVisuals {
    public static final int HELD_INTERVAL = 8;
    public static final int MAX_HELD_PLAYERS = 4;
    private static final double TAU = Math.PI * 2;
    private ParticleVisuals() { }

    public record HeldCue(CombatFxPayload payload, boolean followsHand) {
        public CombatFxPayload at(Vec3 emittedCenter, Vec3 center, Vec3 look) {
            if (!followsHand) return payload;
            return new CombatFxPayload(payload.shape(), center.add(payload.origin().subtract(emittedCenter)), look,
                    payload.radius(), payload.length(), payload.span(), payload.core(), payload.edge(),
                    payload.duration(), payload.boss(), payload.entityId());
        }
    }

    public static Vec3 handAnchor(Vec3 position, float eyeHeight, float height, Vec3 look,
                                   Vec3 horizontalLook, float attack, boolean leftHanded, boolean firstPerson) {
        Vec3 side = handSide(horizontalLook, leftHanded);
        double swing = Math.sin(attack * Math.PI);
        return firstPerson ? position.add(0, eyeHeight, 0).add(look.scale(.7)).add(side.scale(.34 - swing * .22)).add(0, -.34, 0)
                : position.add(0, height * .58, 0).add(look.scale(.43)).add(side.scale(.36 - swing * .2));
    }

    public static Vec3 handSide(Vec3 horizontalLook, boolean leftHanded) {
        Vec3 flat = horizontalLook.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1E-6) flat = new Vec3(0, 0, 1);
        return new Vec3(-flat.z, 0, flat.x).normalize().scale(leftHanded ? -1 : 1);
    }

    public static int core(ParticleKind kind) {
        return switch (kind) {
            case FIRE -> 0xFFD08B;
            case FROST -> 0xD5F8FF;
            case VENOM -> 0xC6F482;
            case THUNDER -> 0xFFF3A8;
            case VOID -> 0xC8B9FF;
            case HOLY -> 0xFFF6D9;
            case SHADOW -> 0xE3ABE8;
            case EARTH -> 0xE6C794;
        };
    }

    public static int edge(ParticleKind kind) {
        return switch (kind) {
            case FIRE -> 0xE96B3E;
            case FROST -> 0x53BBD9;
            case VENOM -> 0x609B3B;
            case THUNDER -> 0xD7AA34;
            case VOID -> 0x7264CA;
            case HOLY -> 0xD3B677;
            case SHADOW -> 0x9855A5;
            case EARTH -> 0xA47A45;
        };
    }

    public static ParticleKind dominant(ParticleData data) {
        ParticleKind result = ParticleKind.FIRE;
        for (ParticleKind kind : ParticleKind.values()) if (data.count(kind) > data.count(result)) result = kind;
        return result;
    }

    public static ParticleKind sample(ParticleData data, long index) {
        int total = Math.max(1, data.total());
        int pick = (int) (Math.floorMod(index, total) * 53 % total);
        for (ParticleKind kind : ParticleKind.values()) {
            pick -= data.count(kind);
            if (pick < 0) return kind;
        }
        return ParticleKind.FIRE;
    }

    public static ParticleKind secondary(ParticleData data, ParticleKind primary) {
        ParticleKind result = primary;
        int count = 0;
        for (ParticleKind kind : ParticleKind.values()) if (kind != primary && data.count(kind) > count) {
            result = kind;
            count = data.count(kind);
        }
        return result;
    }

    public static CombatFxPayload cue(Shape shape, Vec3 origin, Vec3 direction, double radius,
                                      double length, double span, ParticleKind kind, int duration) {
        return new CombatFxPayload(shape, origin, direction, (float) radius, (float) length,
                (float) span, core(kind), edge(kind), duration, false, -1);
    }

    public static List<HeldCue> held(ParticleData data, Vec3 center, Vec3 previous, Vec3 look, long tick) {
        int total = data.total();
        int interval = total < 5 ? 16 : HELD_INTERVAL;
        if (total == 0 || Math.floorMod(tick, interval) != 0) return List.of();
        ParticleKind kind = sample(data, Math.floorDiv(tick, interval));
        List<HeldCue> cues = new ArrayList<>(3);
        if (total >= 20) cues.add(new HeldCue(cue(Shape.ORBIT, center, look, total == 50 ? .12 : .09, .1, TAU, kind, HELD_INTERVAL + 2), true));
        else cues.add(new HeldCue(trace(center, center.add(look.scale(total < 5 ? .05 : .09)), kind, interval + 2), true));
        ParticleKind secondary = secondary(data, kind);
        if (total == 50 || secondary != kind) cues.add(new HeldCue(cue(Shape.SIGIL, center.add(0, -.025, 0), look,
                total == 50 ? .14 : total >= 20 ? .07 : .035, 0, 0, secondary, interval + 2), true));
        double distance = previous == null ? 0 : previous.distanceTo(center);
        if (total >= 10 && distance > .025 && distance <= 1.5) cues.add(new HeldCue(trace(previous, center, kind, HELD_INTERVAL), false));
        return List.copyOf(cues);
    }

    public static CombatFxPayload trace(Vec3 from, Vec3 to, ParticleKind kind, int duration) {
        Vec3 delta = to.subtract(from);
        return cue(Shape.TRACE, from, delta.normalize(), .015, Math.min(14, delta.length()), 0, kind, duration);
    }

    public static List<CombatFxPayload> hit(ParticleData data, Vec3 center, Vec3 direction, float width) {
        if (data.total() == 0) return List.of();
        ParticleKind kind = dominant(data);
        double radius = Math.min(16, width * .5 + Math.max(.25, width * .175));
        Shape shape = switch (kind) {
            case FIRE -> Shape.BURST;
            case FROST -> Shape.IMPACT;
            case VENOM -> Shape.SPIRAL;
            case THUNDER -> Shape.BEAM;
            case VOID -> Shape.IMPLOSION;
            case HOLY -> Shape.SIGIL;
            case SHADOW -> Shape.SLASH;
            case EARTH -> Shape.SHOCKWAVE;
        };
        List<CombatFxPayload> cues = new ArrayList<>(3);
        cues.add(cue(shape, center, direction, radius, Math.min(1.5, radius * 1.2),
                kind == ParticleKind.SHADOW ? 1.1 : TAU, kind, 12));
        if (data.total() >= 10) {
            Shape echo = switch (kind) {
                case FIRE, EARTH -> Shape.IMPACT;
                case FROST, HOLY -> Shape.ORBIT;
                case VENOM -> Shape.PETALS;
                case THUNDER -> Shape.BURST;
                case VOID -> Shape.SINGULARITY;
                case SHADOW -> Shape.IMPLOSION;
            };
            cues.add(cue(echo, center, direction, radius * .72, radius * .55, TAU, kind, 16));
        }
        ParticleKind secondary = secondary(data, kind);
        if (secondary != kind && data.count(secondary) >= 5) {
            cues.add(cue(Shape.BURST, center, direction, radius * .65, 0, 0, secondary, 10));
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> operation(Vec3 center, ParticleData data, int action, boolean success) {
        ParticleKind kind = action >= 1 && action <= 24 ? ParticleKind.values()[(action - 1) % 8] : dominant(data);
        if (!success) return List.of(cue(Shape.IMPLOSION, center, Vec3.ZERO, .22, .2, 0, kind, 10));
        if (action >= 17) return List.of(cue(Shape.PETALS, center, Vec3.ZERO, .42, .28, 0, ParticleKind.HOLY, 14));
        return List.of(cue(Shape.SIGIL, center, Vec3.ZERO, .46, 0, 0, kind, 16),
                cue(Shape.SPIRAL, center, Vec3.ZERO, .25, action == 0 ? .8 : .45, TAU, kind, 16));
    }
}
