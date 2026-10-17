package cn.kurt6.unknown_echoes.worldevent;

import net.minecraft.world.phys.Vec3;

/** 风场只追加有限冲量，保留玩家与其他能力已有的动量。 */
public final class DisasterWindField {
    private DisasterWindField() { }

    public static double radius(double effectRadius, boolean deep) {
        return Math.max(0, Math.min(deep ? 28 : 48, effectRadius));
    }

    public static double strength(long now, long start, long end) {
        if (now < start || now >= end) return 0;
        double rise = smooth((now - start - DisasterState.WARNING_TICKS) / 60.0);
        return rise * smooth((end - now) / (double) DisasterState.FADE_TICKS);
    }

    public static Vec3 impulse(Vec3 offset, Vec3 velocity, double effectRadius,
                               double height, boolean deep, double strength) {
        if (!finite(offset) || !finite(velocity) || !Double.isFinite(height)
                || !Double.isFinite(effectRadius) || !Double.isFinite(strength) || height < 8 || strength <= 0) return Vec3.ZERO;
        double radius = radius(effectRadius, deep);
        double distance = Math.hypot(offset.x, offset.z);
        double ceiling = Math.min(deep ? 6 : 12, height - 2);
        if (radius <= 0 || distance >= radius || offset.y < -3 || offset.y >= ceiling) return Vec3.ZERO;
        double radial = smooth(1 - distance / radius);
        double vertical = smooth((ceiling - offset.y) / 3.0) * smooth((offset.y + 3) / 2.0);
        double force = Math.clamp(strength, 0, 1) * radial * vertical;
        Vec3 inward = distance < 1.0E-6 ? Vec3.ZERO : new Vec3(-offset.x / distance, 0, -offset.z / distance);
        Vec3 tangent = new Vec3(inward.z, 0, -inward.x).scale(deep ? -1 : 1);
        double core = smooth(distance / 2.5);
        Vec3 horizontal = inward.scale((deep ? .065 : .105) * force * core)
                .add(tangent.scale((deep ? .026 : .045) * force * core));
        double amount = horizontal.length();
        if (amount > 1.0E-8) {
            Vec3 direction = horizontal.scale(1 / amount);
            horizontal = direction.scale(Math.max(0, Math.min(amount, (deep ? .32 : .52) - velocity.dot(direction))));
        }
        double lift = deep ? 0 : .115 * force * smooth(1 - distance / (radius * .32))
                * smooth((Math.min(7, height - 3) - offset.y) / 3.0);
        lift = Math.max(0, Math.min(lift, .24 - velocity.y));
        return horizontal.add(0, lift, 0);
    }

    private static double smooth(double value) {
        double t = Math.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static boolean finite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
