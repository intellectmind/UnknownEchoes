package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.worldevent.DisasterState;
import cn.kurt6.unknown_echoes.worldevent.DisasterWindField;
import net.minecraft.world.phys.Vec3;

public final class StormWindMoteGeometry {
    public static final int PER_TICK = 6;
    public static final int LIFETIME = 24;
    public static final int MAX_LIVE = PER_TICK * LIFETIME;

    private StormWindMoteGeometry() { }

    public static double strength(long now, long eventStart, double sceneStrength) {
        if (now < eventStart || !Double.isFinite(sceneStrength)) return 0;
        double rise = Math.clamp((now - eventStart - DisasterState.WARNING_TICKS) / 60.0, 0, 1);
        return rise * rise * (3 - 2 * rise) * Math.clamp(sceneStrength, 0, 1);
    }

    public static Vec3 spawn(Vec3 viewer, long tick, int slot) {
        double phase = Math.floorMod(tick, 4096L) * PER_TICK + slot;
        double angle = phase * 2.399963229728653;
        double radius = 2.5 + fraction(phase * .754877666) * 6;
        return viewer.add(Math.cos(angle) * radius, .3 + fraction(phase * .569840291) * 2.6, Math.sin(angle) * radius);
    }

    public static Vec3 velocity(Vec3 offset, float radius, float height, boolean deep, double strength, int index) {
        Vec3 force = DisasterWindField.impulse(offset, Vec3.ZERO, radius, height, deep, strength);
        if (force.lengthSqr() < 1E-10) return Vec3.ZERO;
        return force.scale(3.4).add(0, .014 + .01 * Math.sin(index * .73), 0);
    }

    public static float alpha(int age, double cameraDistance, double strength) {
        double life = Math.clamp(age / 4.0, 0, 1) * Math.clamp((LIFETIME - age) / 8.0, 0, 1);
        double camera = Math.clamp((cameraDistance - .8) / 1.4, 0, 1);
        return (float) (.7 * life * camera * Math.clamp(strength, 0, 1));
    }

    private static double fraction(double value) { return value - Math.floor(value); }
}
