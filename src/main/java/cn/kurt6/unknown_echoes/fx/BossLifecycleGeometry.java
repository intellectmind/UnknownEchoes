package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.world.phys.Vec3;

/** 坐标相对脚底，半径和高度单位为格；整场演出由一个短寿命载荷驱动。 */
public final class BossLifecycleGeometry {
    public static final int STRANDS = 32;
    public static final int ACCENTS = 128;
    public static final int FACETS = 32;
    private static final double TAU = Math.PI * 2;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private BossLifecycleGeometry() { }

    public static boolean isLifecycle(Shape shape) {
        return shape == Shape.BOSS_SUMMON || shape == Shape.BOSS_DEATH || shape == Shape.BOSS_PHASE;
    }

    public static double release(CombatFxPayload p, double age) {
        double start = p.shape() == Shape.BOSS_SUMMON ? .38 : p.shape() == Shape.BOSS_DEATH ? .30 : .12;
        return Math.max(0, Math.min(1, (age / p.duration() - start) / (1 - start)));
    }

    public static float strandOpacity(CombatFxPayload p, int strand, double age) {
        if (!isLifecycle(p.shape())) return 1;
        double q = release(p, age);
        if (strand >= 24) return (float) (Math.max(0, Math.min(1, (q - .28) * 6)) * (1 - q * .6));
        if (strand < 4) return (float) (.65 + .35 * Math.sin(Math.PI * q));
        if (strand < 10) return (float) (1 - q * .88);
        if (strand < 16) return (float) Math.min(1, q * 9);
        return (float) (Math.min(1, q * 12) * (1 - q * .55));
    }

    public static Vec3 point(CombatFxPayload p, int strand, double t, double age) {
        return BossLifecycleIdentity.point(p, strand, t, age, basePoint(p, strand, t, age));
    }

    public static double formation(CombatFxPayload p, double age) {
        double progress = Math.max(0, Math.min(1, age / p.duration()));
        return p.shape() == Shape.BOSS_DEATH ? 1 - Math.min(1, progress / .3) * .83
                : p.shape() == Shape.BOSS_SUMMON ? .35 + .65 * Math.min(1, progress / .38) : 1;
    }

    private static Vec3 basePoint(CombatFxPayload p, int strand, double t, double age) {
        double q = release(p, age), r = p.radius(), h = p.length();
        double spin = age * .075;
        if (strand >= 24) {
            double late = Math.max(0, (q - .28) / .72);
            double angle = (strand - 24) * TAU / 8 + spin * .65 - t * .32;
            return polar(angle, r * (.52 + late * .82) * (.86 + .14 * t),
                    h * .52 + Math.sin(strand * 2.4) * r * .2 + (1 - t) * r * .20 - late * late * r * .30);
        }
        if (strand < 4) {
            double distance = strand < 2 ? r * (.78 + strand * .18)
                    : r * (.25 + 1.12 * Math.sqrt(q)) * (strand == 2 ? 1 : .78);
            double a = TAU * t + spin * (strand % 2 == 0 ? 1 : -1);
            double teeth = strand == 1 ? 1 + .045 * Math.cos(a * 12) : 1;
            return polar(a, distance * teeth, .08 + strand * .10);
        }
        if (strand < 10) {
            double arm = strand - 4;
            if (p.shape() == Shape.BOSS_PHASE) {
                double angle = TAU * t * .82 + arm * TAU / 6 + spin;
                double tilt = arm * Math.PI / 6;
                double distance = r * (.86 + q * .42);
                return new Vec3(Math.cos(angle) * distance,
                        h * .52 + Math.sin(angle) * Math.sin(tilt) * Math.min(h * .45, distance),
                        Math.sin(angle) * Math.cos(tilt) * distance);
            }
            double a = TAU * (arm / 6 + t * 1.35) - spin * 2;
            double compression = formation(p, age);
            double distance = r * compression * (.82 + .14 * Math.sin(Math.PI * t));
            double y = p.shape() == Shape.BOSS_DEATH ? h * .52 + (t - .52) * h * compression : .12 + h * t;
            return polar(a, distance, y);
        }
        if (strand < 16) {
            double a = TAU * t + spin * (strand % 2 == 0 ? 1 : -1);
            double tilt = (strand - 10) * Math.PI / 6;
            double distance = r * (.55 + .65 * Math.sqrt(q));
            return new Vec3(Math.cos(a) * distance,
                    h * .52 + Math.sin(a) * Math.sin(tilt) * Math.min(h * .45, distance),
                    Math.sin(a) * Math.cos(tilt) * distance);
        }
        double a = (strand - 16) * TAU / 8 + .22;
        double distance = r * (.25 + 1.12 * Math.sqrt(q)) * (.65 + t * .35);
        double lift = p.shape() == Shape.BOSS_SUMMON ? h * (.25 + t * .7) : h * .52 + (t - .5) * h * .55;
        return polar(a + .12 * Math.sin(t * Math.PI), distance, lift);
    }

    public static Vec3[] facet(CombatFxPayload p, int index, double age) {
        return BossLifecycleIdentity.facet(p, index, age);
    }

    public static CombatFxAccentGeometry.Sprite sprite(CombatFxPayload p, int index, double age) {
        double q = release(p, age), r = p.radius(), h = p.length();
        double seed = (index * .61803398875) % 1;
        double angle = TAU * seed + age * .055;
        double size = Math.min(.65, r * .10);
        Vec3 center, axis = UP, normal = Vec3.ZERO;
        double halfLength = size, halfWidth = size * .28;
        float alpha = .85F;
        float tint = (float) (.20 + .65 * q);
        var material = CombatFxAccentGeometry.Material.SPARK;
        if (index < 3) {
            center = new Vec3(0, .045 + index * .025, 0);
            axis = polar(age * .028 * (index % 2 == 0 ? 1 : -1), 1, 0);
            normal = UP;
            halfLength = halfWidth = r * (1 - index * .23);
            material = CombatFxAccentGeometry.Material.RUNE;
            alpha = .38F;
        } else if (index == 3) {
            center = new Vec3(0, h * .52, 0);
            halfLength = halfWidth = Math.min(2.2, r * .48) * (.45 + Math.sin(Math.PI * q));
            material = CombatFxAccentGeometry.Material.FLARE;
            alpha = (float) (Math.max(0, 1 - Math.abs(q - .10) * 6) * .85);
            tint = 0;
        } else if (index < 28) {
            double ring = index - 4;
            angle = ring * TAU / 24 - age * .025;
            center = polar(angle, r * (.88 + q * .28), h * (.18 + .56 * ((index % 3) / 2.0)));
            normal = polar(angle, 1, 0);
            halfLength = halfWidth = size * .85;
            material = CombatFxAccentGeometry.Material.RUNE;
            alpha = (float) (.65 * (1 - q));
        } else {
            double band = (index % 7) / 6.0;
            double start = p.shape() == Shape.BOSS_SUMMON ? .38 : p.shape() == Shape.BOSS_DEATH ? .30 : .12;
            double gather = Math.min(1, Math.max(0, age / p.duration() / start));
            gather = gather * gather * (3 - 2 * gather);
            double distance = r * (q > 0 ? .24 + 1.12 * Math.sqrt(q) : .92 - .68 * gather);
            double spread = q > 0 ? .22 + .78 * Math.sqrt(q) : 1 - .78 * gather;
            center = polar(angle, distance * (.58 + seed * .42),
                    h * .52 + h * (-.36 + .72 * band) * spread + q * (.5 - seed) * h * .26);
            axis = polar(angle + .35, 1, .25 + band).normalize();
            halfLength *= 1.8 - q * .7;
            material = BossLifecycleIdentity.accent(p, index);
            halfWidth = halfLength * (material == CombatFxAccentGeometry.Material.STAR ? .55 : .32);
            alpha *= (float) (.30 + .70 * Math.min(1, q * 12));
            if (index >= 96) {
                alpha *= (float) Math.max(0, Math.min(1, (q - .28) * 6));
                center = center.add(0, -r * q * q * .22, 0);
            }
        }
        return new CombatFxAccentGeometry.Sprite(center, axis, normal, halfLength, halfWidth, alpha, tint, material);
    }

    private static Vec3 polar(double angle, double radius, double y) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }
}
