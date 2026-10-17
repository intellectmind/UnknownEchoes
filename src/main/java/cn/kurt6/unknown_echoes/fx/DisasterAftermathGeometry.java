package cn.kurt6.unknown_echoes.fx;

import net.minecraft.world.phys.Vec3;

public final class DisasterAftermathGeometry {
    public static final int DUST_PER_IMPACT = 24;
    public static final int MAX_DUST = 144;
    public static final int DUST_LIFETIME = 28;
    public static final int MAX_WAVES = 12;
    public static final int WAVE_LIFETIME = 84;

    private DisasterAftermathGeometry() { }

    public record Dust(Vec3 position, float size, float alpha) { }

    public static double speed(boolean deep) { return deep ? 2.0D : 4.0D; }

    public static float pulse(double sinceArrival) {
        if (sinceArrival <= 0 || sinceArrival >= 18) return 0;
        double attack = Math.min(1, sinceArrival / 2);
        double fade = 1 - sinceArrival / 18;
        return (float) (attack * fade * fade);
    }

    public static Dust dust(Vec3 center, boolean deep, int index, double age) {
        double progress = Math.clamp(age / DUST_LIFETIME, 0, 1);
        double angle = index * Math.PI * 2 / DUST_PER_IMPACT + Math.sin(index * 2.4D) * 0.055D;
        double radius = 13 * (1 - Math.exp(-(0.65D + age * speed(deep)) / 13))
                * (0.85D + 0.15D * Math.sin(index * 1.7D));
        double loft = Math.sin(progress * Math.PI) * (deep ? 0.55D : 0.85D);
        float size = (float) ((0.65D + Math.sin(progress * Math.PI) * (deep ? 2.2D : 1.8D))
                * (0.85D + 0.15D * Math.cos(index * 1.3D)));
        float alpha = (float) (0.55D * Math.min(1, age / 2) * (1 - progress) * (1 - progress));
        return new Dust(center.add(Math.cos(angle) * radius, 0.3D + loft, Math.sin(angle) * radius), size, alpha);
    }
}
