package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class CombatFxGeometry {
    public static final int MAX_SEGMENTS = 64;
    private static final double TAU = Math.PI * 2;
    private static final double[][] GLYPH = {{0, .62}, {.12, .75}, {0, .92}, {-.12, .75}, {0, .62}};

    private CombatFxGeometry() { }

    public enum Material {
        FILAMENT("energy_ribbon"), BLADE("blade_ribbon"), FLOW("flow_ribbon"), ETCHED("etched_ribbon");

        private final String texture;

        Material(String texture) {
            this.texture = texture;
        }

        public String texture() {
            return "textures/effect/" + texture + ".png";
        }
    }

    public enum FacetMaterial {
        SHIELD("shield_membrane", false), CRYSTAL("crystal_facet", false),
        CRESCENT("crescent_membrane", true), PETAL("petal_membrane", true);

        private final String texture;
        private final boolean additive;

        FacetMaterial(String texture, boolean additive) {
            this.texture = texture;
            this.additive = additive;
        }

        public String texture() {
            return "textures/effect/" + texture + ".png";
        }

        public boolean additive() {
            return additive;
        }
    }

    public static FacetMaterial facetMaterial(CombatFxPayload p) {
        var elemental = ElementalLayerGeometry.material(p);
        if (elemental != null) return elemental;
        if (EquipmentWardGeometry.style(p) == EquipmentWardGeometry.Style.CRYSTAL) return FacetMaterial.CRYSTAL;
        if (EquipmentWardGeometry.style(p) == EquipmentWardGeometry.Style.LEAF) return FacetMaterial.PETAL;
        return switch (p.shape()) {
            case SLASH, CONE -> FacetMaterial.CRESCENT;
            case PETALS -> FacetMaterial.PETAL;
            case BURST, IMPACT -> FacetMaterial.CRYSTAL;
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> BossLifecycleIdentity.facetMaterial(p);
            default -> FacetMaterial.SHIELD;
        };
    }

    public static Material material(CombatFxPayload p) {
        if (ProjectileTrailGeometry.style(p) != null) return Material.FLOW;
        return switch (p.shape()) {
            case SLASH, CONE, BURST, IMPACT -> Material.BLADE;
            case BEAM, HELIX, WAKE, PETALS, VORTEX, SINGULARITY -> Material.FLOW;
            case RING, SHOCKWAVE, SIGIL, AEGIS, DOME -> Material.ETCHED;
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> BossLifecycleIdentity.material(p);
            default -> Material.FILAMENT;
        };
    }

    public static int strands(CombatFxPayload p) {
        return baseStrands(p) + CombatFxOrnamentGeometry.strands(p) + ElementalLayerGeometry.strands(p);
    }

    public static int baseStrands(CombatFxPayload p) {
        if (ProjectileTrailGeometry.style(p) != null) return ProjectileTrailGeometry.strands(p);
        if (RealmDisasterFx.isWind(p)) return 1;
        return switch (p.shape()) {
            case DEBRIS -> 0;
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> BossLifecycleGeometry.STRANDS;
            case SPIRAL, COLUMN, HELIX, SHOCKWAVE, WAKE -> 2;
            case PETALS, AEGIS -> 6;
            case SIGIL -> 7;
            case DOME, ORBIT, BEAM -> 3;
            case VORTEX -> 3;
            case SINGULARITY -> 4;
            case IMPLOSION, COLLAPSE, ARRIVAL -> 5;
            case BURST -> 7;
            default -> 1;
        };
    }

    public static int segments(CombatFxPayload p) {
        if (ProjectileTrailGeometry.style(p) != null) return 24;
        if (EquipmentWardGeometry.style(p) != null) return 48;
        return switch (p.shape()) {
            case TRACE -> 16;
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> 48;
            case BURST, AEGIS -> 12;
            case PETALS, WAKE, SIGIL, BEAM -> 32;
            case SLASH, CONE -> 40;
            default -> MAX_SEGMENTS;
        };
    }

    // 坐标相对演出原点；边界环与斩弧不随动画扩张，始终对应服务端判定。
    public static Vec3 point(CombatFxPayload p, int strand, double t, double age) {
        if (ProjectileTrailGeometry.style(p) != null) return ProjectileTrailGeometry.point(p, strand, t, age);
        int baseCount = baseStrands(p);
        int elemental = ElementalLayerGeometry.firstStrand(p);
        if (strand >= elemental && strand < elemental + ElementalLayerGeometry.strands(p))
            return ElementalLayerGeometry.point(p, strand - elemental, t, age);
        if (strand >= baseCount && strand < baseCount + CombatFxOrnamentGeometry.strands(p))
            return CombatFxOrnamentGeometry.point(p, strand - baseCount, t, age);
        if (EquipmentWardGeometry.style(p) != null && p.shape() == CombatFxPayload.Shape.AEGIS)
            return EquipmentWardGeometry.panelPoint(p, strand, t, age);
        Vec3 dir = p.direction().lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : p.direction().normalize();
        double r = p.radius();
        double phase = age * 0.09;
        double a = TAU * t;
        return switch (p.shape()) {
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> BossLifecycleGeometry.point(p, strand, t, age);
            case TRACE -> dir.scale(p.length() * t).add(0, Math.sin(Math.PI * t) * r, 0);
            case RING, IMPACT -> new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r);
            case SIGIL -> {
                if (strand == 0) {
                    yield new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r);
                }
                double angle = (strand - 1) * TAU / 6 + phase * 0.12;
                double step = t * 4;
                int k = Math.min(3, (int) step);
                double x = (GLYPH[k][0] + (GLYPH[k+1][0] - GLYPH[k][0]) * (step-k)) * r;
                double z = (GLYPH[k][1] + (GLYPH[k+1][1] - GLYPH[k][1]) * (step-k)) * r;
                yield new Vec3(Math.cos(angle) * z - Math.sin(angle) * x, .02,
                        Math.sin(angle) * z + Math.cos(angle) * x);
            }
            case PETALS -> {
                double angle = strand * TAU / 6 + Math.sin(phase) * .08;
                double distance = r * Math.sin(Math.PI * t);
                double spread = Math.sin(a) * r * .22;
                yield new Vec3(Math.cos(angle) * distance - Math.sin(angle) * spread,
                        p.length() * Math.pow(Math.sin(Math.PI * t), 2),
                        Math.sin(angle) * distance + Math.cos(angle) * spread);
            }
            case AEGIS -> {
                double angle = strand * TAU / 6 + phase * .12;
                double step = t * 6;
                int k = Math.min(5, (int) step);
                double a0 = k * TAU / 6, a1 = (k+1) * TAU / 6;
                double side = (Math.cos(a0) + (Math.cos(a1)-Math.cos(a0)) * (step-k)) * r * .43;
                double y = (.5 + (Math.sin(a0) + (Math.sin(a1)-Math.sin(a0)) * (step-k)) * .34) * p.length();
                yield new Vec3(Math.cos(angle) * r - Math.sin(angle) * side, y,
                        Math.sin(angle) * r + Math.cos(angle) * side);
            }
            case WAKE -> {
                Vec3 side = billboardFrame(dir).right();
                double wave = Math.sin(Math.PI * t) * Math.sin(a - phase * 2 + strand * Math.PI) * r;
                yield dir.scale(p.length() * t).add(side.scale(wave));
            }
            case VORTEX -> {
                if (RealmDisasterFx.isWind(p)) {
                    yield StormVolumeGeometry.inflow(r, p.length(), p.core() == 0xC2A1ED,
                            Math.atan2(dir.z, dir.x), strand, t, age);
                }
                double angle = a * 2.5 - phase * 2 + strand * TAU / 3;
                double distance = r * (.22 + .78 * t);
                yield new Vec3(Math.cos(angle) * distance, p.length() * t, Math.sin(angle) * distance);
            }
            case SINGULARITY -> {
                double angle = a + phase * (strand < 2 ? .5 : -1.4) + strand * Math.PI;
                double distance = r * (strand < 2 ? .72 + strand * .18 : .26 + .74 * t);
                if (strand >= 2) {
                    angle += a;
                }
                yield new Vec3(Math.cos(angle) * distance, Math.sin(angle) * distance * .22,
                        Math.sin(angle) * distance);
            }
            case SLASH -> {
                double base = Math.atan2(dir.z, dir.x);
                double angle = base - p.span() + 2 * p.span() * t;
                yield new Vec3(Math.cos(angle) * r, 0.35 * Math.sin(Math.PI * t), Math.sin(angle) * r);
            }
            case BEAM -> {
                if (RealmDisasterFx.isLightning(p)
                        || ElementalFxGeometry.element(p) == cn.kurt6.unknown_echoes.particle.ParticleKind.THUNDER) {
                    yield ElementalFxGeometry.bolt(p, strand, t, age);
                }
                Vec3 axis = dir.scale(p.length() * t);
                if (strand == 0) {
                    yield axis;
                }
                var frame = billboardFrame(dir);
                double angle = a * 1.5 - phase * 2 + strand * Math.PI;
                double spread = Math.min(.22, p.length() * .08) * Math.sin(Math.PI * t);
                yield axis.add(frame.right().scale(Math.cos(angle) * spread))
                        .add(frame.up().scale(Math.sin(angle) * spread));
            }
            case DEBRIS -> Vec3.ZERO;
            case SHOCKWAVE -> {
                double progress = Math.min(1, Math.max(0, age / p.duration()));
                double distance = r * Math.min(1, 0.12 + Math.sqrt(progress) * (strand == 0 ? 1 : 0.72));
                yield new Vec3(Math.cos(a) * distance, strand * 0.16, Math.sin(a) * distance);
            }
            case HELIX -> {
                Vec3 side = billboardFrame(dir).right();
                Vec3 up = dir.cross(side);
                double angle = a * 2 - phase * 2 + strand * Math.PI;
                double taper = Math.sin(Math.PI * t) * 0.7 + 0.3;
                yield dir.scale(p.length() * t).add(side.scale(Math.cos(angle) * r * taper))
                        .add(up.scale(Math.sin(angle) * r * taper));
            }
            case IMPLOSION, COLLAPSE, ARRIVAL -> {
                double compression = spatialScale(p, age);
                double angle = a + age * 0.16 * (strand % 2 == 0 ? 1 : -1);
                double tilt = 0.45 + strand * Math.PI / 5;
                double stretch = 1 + (1 - compression) * 0.7;
                double x = Math.cos(angle) * r * compression;
                double y = Math.sin(angle) * Math.sin(tilt) * Math.max(r, p.length() * 0.5) * stretch;
                double z = Math.sin(angle) * Math.cos(tilt) * r * compression;
                // 最后四分之一连竖轴一起收拢，离场残像必须最终消失到同一个点。
                if (p.shape() != CombatFxPayload.Shape.ARRIVAL) {
                    y *= Math.min(1, compression * 4);
                }
                yield new Vec3(x, y, z);
            }
            case CONE -> {
                double angle = -p.span() + 2 * p.span() * t;
                yield dir.scale(Math.cos(angle) * r)
                        .add(new Vec3(-dir.z, 0, dir.x).scale(Math.sin(angle) * r));
            }
            case SPIRAL, COLUMN -> {
                double angle = a * Math.max(1, p.span() / TAU) + phase + strand * Math.PI;
                double taper = p.shape() == CombatFxPayload.Shape.COLUMN ? 0.65 + 0.35 * (1 - t) : 1;
                yield new Vec3(Math.cos(angle) * r * taper, p.length() * t, Math.sin(angle) * r * taper);
            }
            case DOME -> {
                double tilt = strand * Math.PI / 3;
                double arc = Math.PI * t;
                yield new Vec3(Math.cos(arc) * Math.cos(tilt) * r,
                        Math.sin(arc) * p.length(), Math.cos(arc) * Math.sin(tilt) * r);
            }
            case ORBIT -> {
                double angle = a + phase * (strand % 2 == 0 ? 1 : -1);
                double tilt = 0.55 + strand * 0.85;
                yield new Vec3(Math.cos(angle) * r, Math.sin(angle) * Math.sin(tilt) * r,
                        Math.sin(angle) * Math.cos(tilt) * r);
            }
            case BURST -> {
                double angle = strand * TAU / 7;
                double progress = Math.sqrt(Math.min(1, Math.max(0, age / p.duration())));
                double distance = r * (.18 + .82 * progress) * (.55 + .45 * t);
                yield new Vec3(Math.cos(angle) * distance, Math.sin(strand * 2.4) * distance * 0.6,
                        Math.sin(angle) * distance);
            }
        };
    }

    public static double width(CombatFxPayload p, double t) {
        if (ProjectileTrailGeometry.style(p) != null) return ProjectileTrailGeometry.width(p, t);
        if (EquipmentWardGeometry.style(p) != null) return .025 + Math.min(.025, p.radius() * .016);
        return switch (p.shape()) {
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> .04 + Math.min(.15, p.radius() * .022) * Math.sin(Math.PI * t);
            case TRACE -> .025 + Math.min(.04, p.length() * .0015);
            case ORBIT, SPIRAL -> Math.max(.012, Math.min(.12, p.radius() * .10));
            case SLASH, CONE -> 0.055 + Math.sin(Math.PI * t) * Math.min(.20, p.radius() * .045);
            case BEAM -> RealmDisasterFx.isLightning(p) ? .18 + .26 * (1 - t) : 0.04 + 0.08 * (1 - t);
            case WAKE -> 0.025 + .20 * Math.sin(Math.PI * t);
            case VORTEX -> (.06 + Math.min(.24, p.radius() * .055)) * (.35 + .65 * Math.sin(Math.PI * t));
            case SINGULARITY -> .06 + Math.min(.18, p.radius() * .045);
            case SIGIL -> Math.min(Math.min(0.25, 0.07 + p.radius() * 0.018), p.radius() * .065);
            case RING, IMPACT -> Math.min(0.25, 0.07 + p.radius() * 0.018);
            case AEGIS, PETALS -> .045 + .018 * p.radius();
            case SHOCKWAVE -> Math.max(.012, Math.min(Math.min(0.6, 0.18 + p.radius() * 0.04), p.radius() * .16));
            case IMPLOSION, COLLAPSE, ARRIVAL -> 0.08 + p.radius() * 0.025;
            case BURST -> 0.16 * (1 - t) + 0.012;
            default -> 0.12;
        };
    }

    public static float opacity(double age, int duration) {
        double attack = Math.min(1, (age + 0.5) / 2);
        double fade = Math.max(0, 1 - age / duration);
        return (float) (attack * fade * fade);
    }

    public static float opacity(CombatFxPayload p, double age) {
        double t = Math.max(0, Math.min(1, age / p.duration()));
        double fade = 1 - smoothstep(.32, 1, t);
        return switch (p.shape()) {
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> (float) (Math.min(1, (age + .5) / 3) * (1 - smoothstep(.68, 1, t)));
            case BURST, IMPACT, SLASH, CONE, BEAM, SHOCKWAVE -> (float) ((.85 + .15 * Math.sin(Math.PI * t)) * fade);
            case COLLAPSE, ARRIVAL, IMPLOSION -> (float) (Math.min(1, (age + .5) / 1.5) * (1 - smoothstep(.65, 1, t)));
            case AEGIS, DOME -> (float) (Math.min(1, (age + 1) / 1.5) * (1 - smoothstep(.4, 1, t)));
            default -> (float) (Math.min(1, (age + .5) / 2) * fade);
        };
    }

    public static double widthEnvelope(CombatFxPayload p, double age) {
        double t = Math.max(0, Math.min(1, age / p.duration()));
        return switch (p.shape()) {
            case SLASH, CONE, BURST, IMPACT -> .65 + .65 * Math.sin(Math.PI * Math.pow(t, .6));
            case BEAM -> .85 + .20 * Math.sin(Math.PI * t);
            case SHOCKWAVE -> 1.25 - t * .5;
            default -> 1;
        };
    }

    public static int mixColor(int a, int b, float fraction) {
        float t = Math.max(0, Math.min(1, fraction));
        int r = Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int blue = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | blue;
    }

    private static double smoothstep(double from, double to, double value) {
        double t = Math.max(0, Math.min(1, (value - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }

    public record BillboardFrame(Vec3 normal, Vec3 right, Vec3 up) { }

    public static BillboardFrame billboardFrame(Vec3 towardCamera) {
        Vec3 normal = towardCamera.lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : towardCamera.normalize();
        Vec3 right = normal.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1E-8 ? new Vec3(1, 0, 0) : right.normalize();
        return new BillboardFrame(normal, right, right.cross(normal).normalize());
    }

    public static Vec3 membraneHalfSize(CombatFxPayload p, double age, int layer) {
        if (p.shape() == CombatFxPayload.Shape.SINGULARITY) {
            return new Vec3(p.radius() * .72, p.radius() * .72, 0);
        }
        double compression = spatialScale(p, age);
        double echo = 1 + layer * 0.14;
        double height = Math.max(p.radius(), p.length() * 0.5) * (1 + (1 - compression) * 0.7);
        if (p.shape() == CombatFxPayload.Shape.COLLAPSE) {
            height *= Math.min(1, compression * 4);
        }
        return new Vec3(p.radius() * compression * echo, height * echo, 0);
    }

    public static int facets(CombatFxPayload p) {
        return baseFacets(p) + ElementalLayerGeometry.facets(p);
    }

    public static int baseFacets(CombatFxPayload p) {
        return switch (p.shape()) {
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> BossLifecycleGeometry.FACETS;
            case DOME, SLASH, CONE -> 24;
            case PETALS -> 48;
            case AEGIS -> 6;
            case IMPACT, BURST -> 18;
            default -> 0;
        };
    }

    public static Vec3[] facet(CombatFxPayload p, int index, double age) {
        int base = baseFacets(p);
        if (index >= base && index < base + ElementalLayerGeometry.facets(p))
            return ElementalLayerGeometry.facet(p, index - base, age);
        if (BossLifecycleGeometry.isLifecycle(p.shape())) return BossLifecycleGeometry.facet(p, index, age);
        if (EquipmentWardGeometry.style(p) == EquipmentWardGeometry.Style.CRYSTAL && p.shape() == CombatFxPayload.Shape.AEGIS)
            return EquipmentWardGeometry.crystalFacet(p, index, age);
        if (p.shape() == CombatFxPayload.Shape.SLASH || p.shape() == CombatFxPayload.Shape.CONE) {
            double t0 = index / 24.0, t1 = (index + 1) / 24.0;
            return new Vec3[]{bladeInner(p, t0, age), point(p, 0, t0, age),
                    point(p, 0, t1, age), bladeInner(p, t1, age)};
        }
        if (p.shape() == CombatFxPayload.Shape.PETALS) {
            int petal = index / 8;
            double t0 = (index % 8) / 16.0, t1 = (index % 8 + 1) / 16.0;
            return new Vec3[]{point(p, petal, t0, age), point(p, petal, 1 - t0, age),
                    point(p, petal, 1 - t1, age), point(p, petal, t1, age)};
        }
        if (p.shape() == CombatFxPayload.Shape.DOME) {
            int band = index / 12;
            double a0 = index % 12 * TAU / 12, a1 = a0 + TAU / 12;
            double y0 = band * Math.PI / 4, y1 = y0 + Math.PI / 4;
            return new Vec3[]{domePoint(p, a0, y0), domePoint(p, a1, y0),
                    domePoint(p, a1, y1), domePoint(p, a0, y1)};
        }
        if (p.shape() == CombatFxPayload.Shape.AEGIS) {
            double angle = index * TAU / 6 + age * .09 * .12;
            Vec3 center = new Vec3(Math.cos(angle) * p.radius(), p.length() * .5, Math.sin(angle) * p.radius());
            Vec3 x = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(p.radius() * .43);
            Vec3 y = new Vec3(0, p.length() * .34, 0);
            return new Vec3[]{center.subtract(x).add(y), center.subtract(x).subtract(y),
                    center.add(x).subtract(y), center.add(x).add(y)};
        }
        if (p.shape() == CombatFxPayload.Shape.IMPACT || p.shape() == CombatFxPayload.Shape.BURST) {
            int shard = index / 3;
            double angle = shard * TAU / 6 + .27;
            double motion = Math.min(1, age / 6);
            Vec3 axis = new Vec3(Math.cos(angle), .22 + .12 * (shard % 3), Math.sin(angle)).normalize();
            var frame = billboardFrame(axis);
            double size = p.radius() * (.12 + .03 * (shard % 2));
            double distance = p.shape() == CombatFxPayload.Shape.IMPACT ? 1 + .12 * motion
                    : .25 + .70 * Math.sqrt(Math.min(1, Math.max(0, age / p.duration())));
            Vec3 center = axis.scale(p.radius() * distance);
            Vec3 tip = center.add(axis.scale(size));
            Vec3 tail = center.subtract(axis.scale(size));
            double a0 = (index % 3) * TAU / 3 + age * .16;
            double a1 = a0 + TAU / 3;
            Vec3 v0 = center.add(frame.right().scale(Math.cos(a0) * size * .36))
                    .add(frame.up().scale(Math.sin(a0) * size * .36));
            Vec3 v1 = center.add(frame.right().scale(Math.cos(a1) * size * .36))
                    .add(frame.up().scale(Math.sin(a1) * size * .36));
            return new Vec3[]{tip, v0, tail, v1};
        }
        return new Vec3[0];
    }

    // 刃面只向判定边界内侧延伸，不能用加宽外缘冒充更大的攻击范围。
    private static Vec3 bladeInner(CombatFxPayload p, double t, double age) {
        double thickness = Math.min(1.8, p.radius() * .40) * Math.pow(Math.sin(Math.PI * t), .7);
        return point(p, 0, t, age).scale(p.radius() > 0 ? 1 - thickness / p.radius() : 0);
    }

    public static float facetU(CombatFxPayload p, int index, boolean end) {
        if (index >= baseFacets(p) && ElementalLayerGeometry.facets(p) > 0) return end ? 1 : 0;
        int step = index + (end ? 1 : 0);
        return switch (p.shape()) {
            case SLASH, CONE -> step / 24F;
            case PETALS -> (index % 8 + (end ? 1 : 0)) / 8F;
            default -> end ? 1 : 0;
        };
    }

    public static float facetOpacity(CombatFxPayload p, int index, double age) {
        if (index >= baseFacets(p) && ElementalLayerGeometry.facets(p) > 0)
            return ElementalLayerGeometry.facetOpacity(age, p.duration());
        var ward = EquipmentWardGeometry.style(p);
        if (ward != null) return ward == EquipmentWardGeometry.Style.CRYSTAL ? .5F
                : ward == EquipmentWardGeometry.Style.LEAF ? .24F : .15F;
        return switch (p.shape()) {
            case BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> (float) (.7 * Math.min(1, BossLifecycleGeometry.release(p, age) * 10));
            case SLASH, CONE -> .85F * strokeOpacity(p, (index + .5) / 24.0, age);
            case PETALS -> .34F;
            case BURST, IMPACT -> .88F;
            case AEGIS, DOME -> (float) (.30 + .28 * Math.pow(.5 + .5 * Math.sin(age * .7 - index * .8), 3));
            default -> .38F;
        };
    }

    public static float strokeOpacity(CombatFxPayload p, double t, double age) {
        if (p.shape() == CombatFxPayload.Shape.SLASH || p.shape() == CombatFxPayload.Shape.CONE) {
            double head = Math.min(1.12, (age + .5) / 2);
            return (float) Math.max(0, Math.min(1, (head - t) / .12));
        }
        return 1;
    }

    // 近镜头只淡出面片和外晕，范围边界与亮芯保持可读；距离单位为格。
    public static float detailVisibility(double distance) {
        double t = Math.max(0, Math.min(1, (distance - .65) / 1.85));
        return (float) (t * t * (3 - 2 * t));
    }

    private static Vec3 domePoint(CombatFxPayload p, double angle, double elevation) {
        return new Vec3(Math.cos(angle) * Math.cos(elevation) * p.radius(),
                Math.sin(elevation) * p.length(), Math.sin(angle) * Math.cos(elevation) * p.radius());
    }

    public static boolean transientShape(CombatFxPayload p) {
        if (ProjectileTrailGeometry.style(p) != null || RealmDisasterFx.isWind(p) || RealmDisasterFx.isLightning(p)) return true;
        return switch (p.shape()) {
            case DEBRIS, SHOCKWAVE, IMPLOSION, COLLAPSE, ARRIVAL, BURST, IMPACT, SLASH, CONE,
                    BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE -> true;
            default -> false;
        };
    }

    public static double motionTime(CombatFxPayload p, double age, double clock) {
        return transientShape(p) ? age : clock;
    }

    public static double spatialScale(CombatFxPayload p, double age) {
        double t = Math.max(0, Math.min(1, age / p.duration()));
        if (p.shape() == CombatFxPayload.Shape.ARRIVAL) {
            return Math.sin(Math.min(1, t * 2.5) * Math.PI / 2);
        }
        return Math.pow(1 - t, 1.65);
    }

    public static Vec3 ribbonSide(Vec3 a, Vec3 b, Vec3 camera) {
        Vec3 tangent = b.subtract(a);
        Vec3 side = tangent.cross(camera.subtract(a.add(b).scale(0.5)));
        if (side.lengthSqr() < 1.0E-8) {
            side = tangent.cross(new Vec3(0, 1, 0));
        }
        return side.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : side.normalize();
    }

    public record RibbonSample(Vec3 point, Vec3 side, double width) { }

    // 镜头与采样点均相对演出原点；相邻四边形必须共用接头，避免宽光带漏缝或叠亮。
    public static RibbonSample[] ribbon(CombatFxPayload p, int strand, double age, Vec3 camera) {
        int count = segments(p);
        Vec3[] points = new Vec3[count + 1];
        for (int i = 0; i <= count; i++) {
            points[i] = point(p, strand, i / (double) count, age);
        }
        boolean closed = points[0].distanceToSqr(points[count]) < 1.0E-12;
        RibbonSample[] samples = new RibbonSample[count + 1];
        for (int i = 0; i <= count; i++) {
            Vec3 previous = points[i == 0 ? (closed ? count - 1 : 0) : i - 1];
            Vec3 next = points[i == count ? (closed ? 1 : count) : i + 1];
            Vec3 tangent = next.subtract(previous);
            Vec3 side = ribbonSide(points[i].subtract(tangent.scale(.5)),
                    points[i].add(tangent.scale(.5)), camera);
            double width = width(p, i / (double) count);
            if (ProjectileTrailGeometry.style(p) != null && strand > 0) width *= .42;
            if (p.shape() == CombatFxPayload.Shape.BEAM && strand > 0) {
                width *= .35;
            }
            width = CombatFxOrnamentGeometry.width(p, strand, width);
            samples[i] = new RibbonSample(points[i], side, ElementalLayerGeometry.width(p, strand, i / (double) count, width));
        }
        return samples;
    }
}
