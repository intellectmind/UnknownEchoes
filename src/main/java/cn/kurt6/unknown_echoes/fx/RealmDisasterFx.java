package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.worldevent.DisasterState;
import cn.kurt6.unknown_echoes.worldevent.DisasterWindField;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RealmDisasterFx {
    private RealmDisasterFx() { }

    public static List<CombatFxPayload> skyArc(Vec3 center, float height, boolean deep, long tick) {
        if (height < 8) return List.of();
        double turn = Math.floorMod(tick, 720L) * 0.017D * StormVolumeGeometry.ROTATION_SPEED_SCALE;
        Vec3 axis = new Vec3(Math.cos(turn), 0, Math.sin(turn)).scale(StormVolumeGeometry.HORIZONTAL_SCALE);
        Vec3 top = center.add(0, height * 0.84D, 0).add(axis.scale(deep ? 3 : 12));
        Vec3 bend = center.add(0, height * 0.57D, 0).add(axis.scale(deep ? -2 : 4));
        Vec3 lower = center.add(0, height * 0.38D, 0).add(axis.scale(deep ? 2 : -7));
        Vec3 end = center.add(0, height * 0.18D, 0).add(axis.scale(deep ? -1 : -3));
        Vec3 fork = bend.add(-axis.z * 2.6, -height * .18, axis.x * 2.6);
        Vec3 branch = lower.add(axis.z * 2.1, height * .1, -axis.x * 2.1);
        return List.of(arcBeam(top, bend, deep), arcBeam(bend, lower, deep), arcBeam(lower, end, deep),
                arcBeam(bend, fork, deep), arcBeam(lower, branch, deep));
    }

    public static boolean isLightning(CombatFxPayload p) {
        return !p.boss() && p.shape() == Shape.BEAM && (p.edge() == 0x294D68
                && (p.core() == 0x82C6DB || p.core() == 0x78BDCE) || p.core() == 0xAD83D6 && p.edge() == 0x4A335F);
    }

    public static boolean isWind(CombatFxPayload p) {
        return !p.boss() && p.shape() == Shape.VORTEX && (p.core() == 0xA8DFED && p.edge() == 0x375A73
                || p.core() == 0xC2A1ED && p.edge() == 0x624A83);
    }

    public static CombatFxPayload wind(Vec3 center, float radius, float height, boolean deep, long tick) {
        return windStream(center, radius, height, deep, tick, 0);
    }

    public static List<CombatFxPayload> windStreams(Vec3 center, float radius, float height, boolean deep, long tick) {
        return List.of(windStream(center, radius, height, deep, tick, 0),
                windStream(center, radius, height, deep, tick, 1), windStream(center, radius, height, deep, tick, 2));
    }

    private static CombatFxPayload windStream(Vec3 center, float radius, float height, boolean deep, long tick, int stream) {
        double angle = Math.floorMod(tick, 120000L) * .19;
        angle += stream * Math.PI * 2 / 3;
        return new CombatFxPayload(Shape.VORTEX, center.add(0, -.5, 0), new Vec3(Math.cos(angle), 0, Math.sin(angle)),
                (float) DisasterWindField.radius(radius, deep), height, 0,
                deep ? 0xC2A1ED : 0xA8DFED, deep ? 0x624A83 : 0x375A73, 26, false, -1);
    }

    private static CombatFxPayload arcBeam(Vec3 start, Vec3 end, boolean deep) {
        Vec3 delta = end.subtract(start);
        return new CombatFxPayload(Shape.BEAM, start, delta.normalize(), 0.18F, (float) delta.length(), 0,
                deep ? 0xAD83D6 : 0x82C6DB, deep ? 0x4A335F : 0x294D68, 8, false, -1);
    }

    public static List<CombatFxPayload> warning(Vec3 center, boolean deep, float remaining) {
        double radius = deep ? DisasterState.RUPTURE_RADIUS : DisasterState.STORM_RADIUS;
        double countdown = radius * (0.1D + 0.75D * Math.clamp(remaining, 0.0F, 1.0F));
        return List.of(cue(Shape.RING, center.add(0, 0.08D, 0), radius, 0, deep, 14),
                cue(Shape.RING, center.add(0, 0.12D, 0), countdown, 0, deep, 14));
    }

    public static List<CombatFxPayload> strike(Vec3 center, boolean deep, int clearance) {
        CombatFxPayload debris = new CombatFxPayload(Shape.DEBRIS, center, Vec3.ZERO,
                (float) (deep ? DisasterState.RUPTURE_RADIUS : DisasterState.STORM_RADIUS),
                deep ? 1.2F : 0.9F, 0, 0, 0, 30, false, -1);
        if (deep) {
            CombatFxPayload shock = cue(Shape.SHOCKWAVE, center.add(0, 0.1, 0), DisasterState.RUPTURE_RADIUS, 0, true, 16);
            return clearance < 2 ? List.of(shock) : List.of(shock,
                    cue(Shape.COLUMN, center, 0.7D, Math.min(5, clearance - 1), true, 14), debris);
        }
        if (clearance < 2) return List.of(cue(Shape.BURST, center.add(0, 0.3, 0), DisasterState.STORM_RADIUS, 1, false, 12));
        double height = Math.min(18, clearance - 0.5D);
        Vec3 top = center.add(0, height, 0);
        Vec3 bend = center.add(1.4D, height * 0.5D, 0.6D);
        return List.of(beam(top, bend), beam(bend, center),
                cue(Shape.BURST, center.add(0, 0.3, 0), DisasterState.STORM_RADIUS, 3, false, 12), debris);
    }

    private static CombatFxPayload beam(Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        return new CombatFxPayload(Shape.BEAM, start, delta.normalize(), 0.3F, (float) delta.length(), 0,
                0x78BDCE, 0x294D68, 10, false, -1);
    }

    private static CombatFxPayload cue(Shape shape, Vec3 center, double radius, double length,
                                       boolean deep, int duration) {
        return new CombatFxPayload(shape, center, new Vec3(0, 1, 0), (float) radius,
                (float) length, (float) (Math.PI * 2), deep ? 0xAE81D8 : 0x78BDCE,
                deep ? 0x45385E : 0x294D68, duration, false, -1);
    }
}
