package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class CombatFxOrnamentGeometry {
    private static final double TAU = Math.PI * 2;

    private CombatFxOrnamentGeometry() { }

    public static int strands(CombatFxPayload p) {
        if (BossLifecycleGeometry.isLifecycle(p.shape()) || RealmDisasterFx.isWind(p)
                || RealmDisasterFx.isLightning(p) || p.radius() < .35 && p.length() < 1) return 0;
        return switch (p.shape()) {
            case RING, SLASH, CONE, AEGIS, DOME, PETALS, SHOCKWAVE, SPIRAL, COLUMN,
                    BEAM, HELIX, ORBIT, VORTEX, SINGULARITY -> 2;
            case SIGIL, BURST, IMPACT -> 4;
            default -> 0;
        };
    }

    // 装饰曲线仅补充内层，首条边界与服务端给定的起终点保持原样。
    public static Vec3 point(CombatFxPayload p, int index, double t, double age) {
        if (EquipmentWardGeometry.style(p) != null) return EquipmentWardGeometry.point(p, index, t, age);
        double r = p.radius(), a = TAU * t;
        double spin = age * .045 * (index % 2 == 0 ? 1 : -1);
        double q = Math.max(0, Math.min(1, age / p.duration()));
        Vec3 dir = p.direction().lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : p.direction().normalize();
        return switch (p.shape()) {
            case RING, SIGIL -> {
                double angle = a + spin;
                double radius = r * (index == 0 ? .86 : index == 1 ? .56
                        : .38 + .08 * Math.cos(a * 6));
                if (index == 3) angle = -a - spin;
                yield new Vec3(Math.cos(angle) * radius, .018 + .018 * index, Math.sin(angle) * radius);
            }
            case SLASH, CONE -> CombatFxGeometry.point(p, 0, t, age)
                    .scale(1 - (.12 + index * .14) * Math.sin(Math.PI * t))
                    .add(0, -.045 * (index + 1) * Math.sin(Math.PI * t), 0);
            case AEGIS, DOME -> {
                double elevation = p.shape() == CombatFxPayload.Shape.DOME ? .28 + index * .3 : 0;
                double radius = r * (p.shape() == CombatFxPayload.Shape.DOME ? Math.cos(elevation * Math.PI / 2) : 1.04);
                double y = p.length() * (p.shape() == CombatFxPayload.Shape.DOME
                        ? Math.sin(elevation * Math.PI / 2) : .22 + .54 * index);
                yield new Vec3(Math.cos(a + spin) * radius, y, Math.sin(a + spin) * radius);
            }
            case BEAM, HELIX -> {
                var frame = CombatFxGeometry.billboardFrame(dir);
                double distance = Math.sin(Math.PI * t) * Math.min(.32, p.length() * .065);
                double angle = a * 3 - age * .22 + index * Math.PI;
                yield dir.scale(p.length() * t).add(frame.right().scale(Math.cos(angle) * distance))
                        .add(frame.up().scale(Math.sin(angle) * distance));
            }
            case SHOCKWAVE, IMPACT -> {
                double delay = .06 + index * .075;
                double travel = Math.max(0, Math.min(1, (q - delay) / (1 - delay)));
                double distance = r * (.08 + .85 * (1 - Math.pow(1 - travel, 2)));
                yield new Vec3(Math.cos(a) * distance, .025 + index * .035, Math.sin(a) * distance);
            }
            case BURST -> {
                double angle = index * TAU / 4 + .35;
                double travel = Math.max(0, q - index * .035);
                double distance = r * (.15 + .75 * (1 - Math.pow(1 - travel, 3))) * (.65 + .35 * t);
                yield new Vec3(Math.cos(angle) * distance,
                        r * (.16 + .17 * (index % 2)) * Math.sin(travel * Math.PI) + t * r * .12,
                        Math.sin(angle) * distance);
            }
            case SPIRAL, COLUMN -> {
                double angle = -a * 2 - spin * 2 + index * Math.PI;
                double radius = r * (.45 + .12 * Math.sin(Math.PI * t));
                yield new Vec3(Math.cos(angle) * radius, p.length() * t, Math.sin(angle) * radius);
            }
            case ORBIT -> {
                double angle = a - spin * 2;
                double tilt = .35 + index * 1.3;
                double radius = r * (.52 + index * .16);
                yield new Vec3(Math.cos(angle) * radius, Math.sin(angle) * Math.sin(tilt) * radius,
                        Math.sin(angle) * Math.cos(tilt) * radius);
            }
            case PETALS -> {
                double angle = a + spin;
                double radius = r * (.36 + .1 * Math.cos(a * 6)) * (1 - index * .35);
                yield new Vec3(Math.cos(angle) * radius, p.length() * .18 + index * .025, Math.sin(angle) * radius);
            }
            case VORTEX -> {
                double angle = a * 3 - age * .18 + index * Math.PI;
                double radius = r * (.12 + .42 * t);
                yield new Vec3(Math.cos(angle) * radius, p.length() * t, Math.sin(angle) * radius);
            }
            case SINGULARITY -> {
                double angle = a * 2 + spin * 3 + index * Math.PI;
                double radius = r * (.18 + .48 * (1 - t));
                yield new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius * .18, Math.sin(angle) * radius);
            }
            default -> Vec3.ZERO;
        };
    }

    public static double width(CombatFxPayload p, int strand, double base) {
        int index = strand - CombatFxGeometry.baseStrands(p);
        return index < 0 || index >= strands(p) ? base : base * .38;
    }

    public static float opacity(CombatFxPayload p, int strand, double age) {
        int index = strand - CombatFxGeometry.baseStrands(p);
        if (index < 0 || index >= strands(p)) return 1;
        double q = Math.max(0, Math.min(1, age / p.duration()));
        return switch (p.shape()) {
            case SLASH, CONE -> (float) (.68 * Math.min(1, Math.max(0, age - index * .65) / 1.4));
            case IMPACT, SHOCKWAVE -> (float) (.56 * Math.min(1, Math.max(0, q - .06 - index * .075) * 16));
            default -> .58F;
        };
    }
}
