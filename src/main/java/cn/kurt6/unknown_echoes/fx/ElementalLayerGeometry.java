package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import net.minecraft.world.phys.Vec3;

public final class ElementalLayerGeometry {
    private static final double TAU = Math.PI * 2;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private ElementalLayerGeometry() { }

    public static ParticleKind kind(CombatFxPayload p) {
        ParticleKind kind = ElementalFxGeometry.element(p);
        if (kind == null) return null;
        if (p.shape() == Shape.BEAM) return kind == ParticleKind.THUNDER && p.length() >= .3 ? kind : null;
        if (p.radius() < .22) return null;
        return switch (p.shape()) {
            case BURST, IMPACT, RING, SIGIL, ORBIT, SPIRAL, COLUMN, PETALS,
                    IMPLOSION, COLLAPSE, ARRIVAL, SINGULARITY, SHOCKWAVE -> kind;
            case SLASH -> kind == ParticleKind.SHADOW ? kind : null;
            default -> null;
        };
    }

    public static int strands(CombatFxPayload p) {
        ParticleKind kind = kind(p);
        return kind == null ? 0 : kind == ParticleKind.FROST || kind == ParticleKind.EARTH ? 6 : 4;
    }

    public static int facets(CombatFxPayload p) {
        ParticleKind kind = kind(p);
        if (kind == null) return 0;
        return switch (kind) {
            case THUNDER, SHADOW -> 0;
            case FROST, EARTH -> 12;
            default -> 6;
        };
    }

    public static CombatFxGeometry.FacetMaterial material(CombatFxPayload p) {
        ParticleKind kind = kind(p);
        if (kind == null) return null;
        return switch (kind) {
            case FIRE, VENOM, HOLY -> CombatFxGeometry.FacetMaterial.PETAL;
            case FROST, EARTH -> CombatFxGeometry.FacetMaterial.CRYSTAL;
            default -> null;
        };
    }

    private static double progress(CombatFxPayload p, double time) {
        return CombatFxGeometry.transientShape(p) ? Math.clamp(time / p.duration(), 0, 1)
                : .5 + .5 * Math.sin(time * .075);
    }

    public static Vec3 point(CombatFxPayload p, int index, double t, double time) {
        ParticleKind kind = kind(p);
        double r = p.radius(), q = progress(p, time);
        double angle = index * TAU / strands(p);
        Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 side = new Vec3(-radial.z, 0, radial.x);
        return switch (kind) {
            case FIRE -> {
                double curl = t * TAU * .8 + time * .16 + index;
                double radius = r * (.2 + .55 * (1 - t)) * Math.sin(Math.PI * t);
                yield radial.scale(r * .42).add(radial.scale(Math.cos(curl) * radius))
                        .add(side.scale(Math.sin(curl) * radius * .45))
                        .add(0, r * t * (.65 + q * .5), 0);
            }
            case FROST -> {
                double step = t < .5 ? t * 2 : (1 - t) * 2;
                yield radial.scale(r * (.32 + .62 * t))
                        .add(side.scale(r * .055 * Math.sin(t * Math.PI * 4)))
                        .add(0, r * (.04 + step * (.3 + q * .3)), 0);
            }
            case VENOM -> {
                double a = angle + t * TAU * 1.4 - time * .055;
                double radius = r * (.22 + .4 * Math.sin(Math.PI * t));
                yield new Vec3(Math.cos(a) * radius, r * (.08 + .85 * t), Math.sin(a) * radius);
            }
            case THUNDER -> {
                if (p.shape() == Shape.BEAM) {
                    double start = .12 + index * .17;
                    double along = start + t * .23;
                    Vec3 center = ElementalFxGeometry.bolt(p, 0, along, time);
                    Vec3 direction = p.direction().lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : p.direction().normalize();
                    var frame = CombatFxGeometry.billboardFrame(direction);
                    double zigzag = .65 + .35 * Math.abs(Math.sin(t * Math.PI * 4));
                    double reach = Math.sin(Math.PI * t) * Math.min(.48, p.length() * .16) * zigzag;
                    yield center.add(frame.right().scale(Math.cos(angle) * reach))
                            .add(frame.up().scale(Math.sin(angle) * reach));
                }
                yield radial.scale(r * (.14 + t * .78)).add(side.scale(Math.sin(t * Math.PI * 6) * r * .1))
                        .add(0, Math.sin(t * Math.PI * 4 + index) * r * .15, 0);
            }
            case VOID -> {
                double a = angle - t * TAU * 1.25 + time * .12;
                double radius = r * (.08 + .86 * (1 - t)) * Math.pow(1 - q, .7);
                yield new Vec3(Math.cos(a) * radius, Math.sin(a * .5) * radius * .25, Math.sin(a) * radius);
            }
            case HOLY -> {
                double shape = Math.sin(Math.PI * t);
                yield radial.scale(r * (.52 + .07 * shape)).add(side.scale(r * .16 * Math.sin(TAU * t)))
                        .add(0, r * shape * (1 + .22 * Math.sin(time * .13 + index)), 0);
            }
            case SHADOW -> {
                if (p.shape() == Shape.SLASH) {
                    double arc = index * .23 + t * .21;
                    yield CombatFxGeometry.point(p, 0, arc, time).scale(.46 + .38 * t)
                            .add(0, Math.sin(t * Math.PI) * r * .22 * (1 - q), 0);
                }
                double a = angle + t * 1.3 + time * .07;
                double radius = r * (.3 + .55 * t);
                yield new Vec3(Math.cos(a) * radius, Math.sin(t * Math.PI) * r * .28, Math.sin(a) * radius);
            }
            case EARTH -> radial.scale(r * (.15 + .72 * q) * (.65 + .35 * t))
                    .add(0, r * (.9 * q - 1.1 * q * q) + Math.sin(Math.PI * t) * r * .12, 0);
        };
    }

    public static Vec3[] facet(CombatFxPayload p, int index, double time) {
        ParticleKind kind = kind(p);
        int piece = kind == ParticleKind.FROST || kind == ParticleKind.EARTH ? index / 2 : index;
        double a = piece * TAU / 6 + .19;
        double r = p.radius(), q = progress(p, time);
        Vec3 radial = new Vec3(Math.cos(a), 0, Math.sin(a));
        Vec3 axis = UP;
        Vec3 center = radial.scale(r * .55);
        double size = r * .35, width = r * .13;
        switch (kind) {
            case FIRE -> { center = center.add(0, r * (.18 + q * .5), 0); axis = UP.add(radial.scale(.45)); size *= 1.45; width *= .75; }
            case FROST -> { center = radial.scale(r * (.3 + .55 * q)).add(0, r * .22, 0); axis = radial.add(UP.scale(.9)); }
            case VENOM -> { center = new Vec3(Math.cos(a + time * .06) * r * .54, r * (.2 + .5 * q), Math.sin(a + time * .06) * r * .54); width *= 1.4; }
            case VOID -> { double collapse = Math.pow(1 - q, 1.4); center = radial.scale(r * .8 * collapse); axis = new Vec3(-radial.z, .2, radial.x); size *= collapse; width *= collapse; }
            case HOLY -> { center = center.add(0, r * .55, 0); size *= 1.4; width *= .7; }
            case EARTH -> { center = radial.scale(r * (.25 + q * .65)).add(0, r * (.9 * q - 1.1 * q * q), 0); axis = radial.add(UP.scale(.2)); size *= .55; width *= .8; }
            default -> { }
        }
        axis = axis.normalize();
        var frame = CombatFxGeometry.billboardFrame(axis);
        Vec3 across = index % 2 == 0 ? frame.right() : frame.up();
        Vec3 along = axis.scale(size), acrossSize = across.scale(width);
        return new Vec3[]{center.add(along), center.add(acrossSize), center.subtract(along), center.subtract(acrossSize)};
    }

    public static int firstStrand(CombatFxPayload p) {
        return CombatFxGeometry.baseStrands(p) + CombatFxOrnamentGeometry.strands(p);
    }

    public static double width(CombatFxPayload p, int strand, double t, double base) {
        if (strand < firstStrand(p) || kind(p) == null) return base;
        double size = kind(p) == ParticleKind.THUNDER ? .026 : Math.min(.13, p.radius() * .037);
        return size * (.25 + .75 * Math.sin(Math.PI * t));
    }

    public static float opacity(CombatFxPayload p, int strand, double age) {
        if (strand < firstStrand(p) || kind(p) == null) return 1;
        return (float) (.65 * Math.min(1, (age + .3) / 1.8));
    }

    public static float facetOpacity(double age, int duration) {
        return (float) (.55 * Math.min(1, (age + .2) / 2) * (1 - .6 * Math.clamp(age / duration, 0, 1)));
    }
}
