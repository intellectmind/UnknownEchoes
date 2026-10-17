package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class BossLifecycleIdentity {
    private static final double TAU = Math.PI * 2;
    private static final ProgressionFx.Theme[] ASPECTS = {ProgressionFx.Theme.WIND, ProgressionFx.Theme.TIDE,
            ProgressionFx.Theme.TRUE_SIGHT, ProgressionFx.Theme.GRAVITY, ProgressionFx.Theme.VOID};

    private BossLifecycleIdentity() { }

    public static CombatFxGeometry.Material material(CombatFxPayload p) {
        return switch (BossLifecycleStyle.of(p)) {
            case TIDE, GRAVITY, BLOOM, EMBER -> CombatFxGeometry.Material.FLOW;
            case MIRROR, RIFT -> CombatFxGeometry.Material.BLADE;
            default -> CombatFxGeometry.Material.ETCHED;
        };
    }

    public static CombatFxGeometry.FacetMaterial facetMaterial(CombatFxPayload p) {
        return switch (BossLifecycleStyle.of(p)) {
            case TIDE, GRAVITY -> CombatFxGeometry.FacetMaterial.CRESCENT;
            case BLOOM, EMBER -> CombatFxGeometry.FacetMaterial.PETAL;
            default -> CombatFxGeometry.FacetMaterial.CRYSTAL;
        };
    }

    public static int core(CombatFxPayload p, int index) {
        return BossLifecycleStyle.of(p) == BossLifecycleStyle.WORLD
                ? ASPECTS[Math.floorMod(index, ASPECTS.length)].coreColor() : p.core();
    }

    public static int edge(CombatFxPayload p, int index) {
        return BossLifecycleStyle.of(p) == BossLifecycleStyle.WORLD
                ? ASPECTS[Math.floorMod(index, ASPECTS.length)].edgeColor() : p.edge();
    }

    public static Vec3 point(CombatFxPayload p, int strand, double t, double age, Vec3 base) {
        var style = BossLifecycleStyle.of(p);
        if (style == BossLifecycleStyle.ARCANE || strand >= 24) return base;
        double r = p.radius(), h = p.length(), q = BossLifecycleGeometry.release(p, age);
        double a = TAU * t + age * .055;
        if (strand < 4) {
            return switch (style) {
                case STONE, MIRROR, WORLD -> {
                    int sides = style == BossLifecycleStyle.WORLD ? 5 : style == BossLifecycleStyle.STONE ? 8 : 6;
                    double theta = Math.atan2(base.z, base.x);
                    double edge = TAU / sides;
                    double sector = theta - Math.floor((theta + edge / 2) / edge) * edge;
                    yield base.multiply(Math.cos(Math.PI / sides) / Math.cos(sector), 1,
                            Math.cos(Math.PI / sides) / Math.cos(sector));
                }
                case TIDE -> base.add(0, .08 * r * Math.sin(a * 3 - age * .15) * Math.sin(Math.PI * t), 0);
                case RIFT -> base.scale(1 + .035 * Math.sin(a * 13) * Math.sin(Math.PI * t));
                case BLOOM -> base.multiply(1 + .12 * Math.cos(a * 5), 1, 1 + .12 * Math.cos(a * 5));
                default -> base;
            };
        }
        if (strand < 10) {
            int arm = strand - 4;
            double formation = BossLifecycleGeometry.formation(p, age);
            double y = h * .52 + (t - .52) * h * formation;
            double angle = arm * TAU / 6 + age * .025;
            double radius = r * formation;
            return switch (style) {
                case STONE, SCRIPT -> {
                    double side = Math.sin(t * Math.PI * 6) * .13 * radius;
                    double rune = side + Math.sin(t * Math.PI * 12) * .035 * radius;
                    yield radial(angle, radius * .94, y).add(radial(angle + Math.PI / 2, rune, 0));
                }
                case MIRROR -> {
                    double side = Math.sin(t * Math.PI * 2) * .27 * radius;
                    yield radial(angle, radius * (.86 + .12 * Math.sin(Math.PI * t)), y)
                            .add(radial(angle + Math.PI / 2, side, 0));
                }
                case TIDE -> radial(a * .72 + angle, radius * (.82 + .12 * Math.sin(t * TAU * 2 - age * .12)),
                        h * .46 + (base.y - h * .52) * .72 + Math.sin(a * 2 + arm) * h * .035);
                case GRAVITY -> {
                    double orbit = a + arm * .4;
                    double tilt = .16 + arm * .24;
                    double distance = radius * (.66 + .055 * arm);
                    yield new Vec3(Math.cos(orbit) * distance, h * .52 + Math.sin(orbit) * distance * Math.sin(tilt),
                            Math.sin(orbit) * distance * Math.cos(tilt));
                }
                case RIFT -> {
                    double noise = Math.sin(t * Math.PI * 18 + arm * 2.1) * Math.sin(Math.PI * t) * radius * .13;
                    yield radial(angle, radius * .9 + noise, y)
                            .add(radial(angle + Math.PI / 2, Math.sin(t * Math.PI * 12 + arm) * radius * .055, 0));
                }
                case WORLD -> {
                    double star = arm * TAU / 5 + age * .035;
                    if (arm == 5) yield radial(a, radius * .52, h * .52 + Math.sin(a * 2) * h * .20 * formation);
                    yield radial(star + Math.sin(t * Math.PI) * .65, radius * (.58 + Math.sin(Math.PI * t) * .40), y);
                }
                case BLOOM -> radial(angle + Math.sin(TAU * t) * .28, radius * (.45 + .55 * Math.sin(Math.PI * t)),
                        h * .18 + Math.sin(Math.PI * t) * h * .67 * formation);
                case EMBER -> radial(angle + t * 1.8 - age * .065, radius * (1 - t * .62), y);
                default -> base;
            };
        }
        if (strand < 16) {
            return switch (style) {
                case TIDE -> new Vec3(base.x, h * (.18 + .065 * (strand - 10)) + Math.sin(a * 2) * r * .07, base.z);
                case RIFT -> base.add(radial(strand, Math.sin(t * Math.PI * 20 + strand) * r * .04 * Math.sin(Math.PI * t), 0));
                case STONE -> new Vec3(base.x, .22 + h * (.08 + .035 * (strand - 10)) * q, base.z);
                case BLOOM -> base.add(0, Math.sin(a * 5) * r * .08, 0);
                default -> base;
            };
        }
        return style == BossLifecycleStyle.TIDE ? new Vec3(base.x, h * .25 + (base.y - h * .52) * .36, base.z) : base;
    }

    public static Vec3[] facet(CombatFxPayload p, int index, double age) {
        double q = BossLifecycleGeometry.release(p, age), r = p.radius(), h = p.length();
        var style = BossLifecycleStyle.of(p);
        double angle = index * TAU / BossLifecycleGeometry.FACETS + age * .018;
        double distance = r * (.76 + .60 * Math.sqrt(q));
        double height = h * (.18 + (index % 4) * .21) + Math.sin(index * 2.1) * q * .35;
        double size = Math.min(.85, r * .16) * (1 - q * .6);
        double breadth = .4;
        if (style == BossLifecycleStyle.STONE) {
            height = h * .16 + r * (.15 + (index % 5) * .09) * Math.sin(Math.PI * q);
            breadth = .85;
        } else if (style == BossLifecycleStyle.TIDE) {
            height = h * (.18 + (index % 3) * .075) + Math.sin(angle * 3 + age * .1) * r * .08;
            breadth = 1.6;
            size *= .6;
        } else if (style == BossLifecycleStyle.MIRROR) {
            size *= 1.6;
        } else if (style == BossLifecycleStyle.RIFT) {
            size *= 1.4;
            breadth = .13;
        } else if (style == BossLifecycleStyle.BLOOM) {
            breadth = .9;
        }
        Vec3 center = radial(angle, distance, height);
        Vec3 side = radial(angle + Math.PI / 2 + q, size * breadth, 0);
        Vec3 up = style == BossLifecycleStyle.TIDE ? radial(angle, size, .12 * size)
                : new Vec3(Math.sin(q * Math.PI + index) * size * q * .4, size, 0);
        return new Vec3[]{center.add(up), center.add(side), center.subtract(up), center.subtract(side)};
    }

    public static CombatFxAccentGeometry.Material accent(CombatFxPayload p, int index) {
        if (index % 7 == 0) return CombatFxAccentGeometry.Material.STAR;
        return switch (BossLifecycleStyle.of(p)) {
            case STONE -> CombatFxAccentGeometry.Material.CHIP;
            case TIDE, GRAVITY -> CombatFxAccentGeometry.Material.WISP;
            case MIRROR -> CombatFxAccentGeometry.Material.ICE;
            case RIFT -> CombatFxAccentGeometry.Material.LIGHTNING;
            case WORLD -> CombatFxAccentGeometry.Material.STAR;
            case EMBER -> CombatFxAccentGeometry.Material.FLAME;
            case BLOOM -> CombatFxAccentGeometry.Material.SPORE;
            case SCRIPT -> CombatFxAccentGeometry.Material.RUNE;
            default -> CombatFxAccentGeometry.Material.SPARK;
        };
    }

    private static Vec3 radial(double angle, double radius, double height) {
        return new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }
}
