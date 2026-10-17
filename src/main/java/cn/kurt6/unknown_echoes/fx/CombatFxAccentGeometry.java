package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class CombatFxAccentGeometry {
    private static final double TAU = Math.PI * 2;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private CombatFxAccentGeometry() { }

    public enum Material {
        FLARE, SPARK, RUNE, WISP, FLAME, ICE, SPORE, LIGHTNING, STAR, CHIP;

        public String texture() {
            return "textures/effect/" + name().toLowerCase(java.util.Locale.ROOT) + "_accent.png";
        }
    }

    // 所有坐标相对演出原点；normal 为零时朝向相机，否则固定在指定平面。
    public record Sprite(Vec3 center, Vec3 axis, Vec3 normal, double halfLength, double halfWidth,
                         float alpha, float tint, Material material) { }

    public static int count(CombatFxPayload p) {
        if (ProjectileTrailGeometry.style(p) != null) return ProjectileTrailGeometry.accents(p);
        if (BossLifecycleGeometry.isLifecycle(p.shape())) return BossLifecycleGeometry.ACCENTS;
        if (RealmDisasterFx.isWind(p)) return 10;
        if (p.shape() == CombatFxPayload.Shape.DEBRIS) return 0;
        if (p.radius() < .2 && p.length() < .3) return 6;
        return switch (p.shape()) {
            case BURST, IMPACT, SHOCKWAVE -> p.boss() ? 96 : 80;
            case SLASH, CONE, BEAM, COLLAPSE, ARRIVAL, IMPLOSION -> p.boss() ? 80 : 64;
            case AEGIS, DOME, PETALS, VORTEX, SINGULARITY -> 48;
            case TRACE, WAKE -> 20;
            default -> 40;
        };
    }

    public static Sprite sprite(CombatFxPayload p, int index, double age, double clock) {
        if (ProjectileTrailGeometry.style(p) != null) return ProjectileTrailGeometry.sprite(p, index, age);
        if (BossLifecycleGeometry.isLifecycle(p.shape())) return BossLifecycleGeometry.sprite(p, index, age);
        double progress = clamp(age / p.duration());
        double seed = random(index, p.core());
        double other = random(index + 79, p.edge());
        double angle = TAU * seed;
        Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double r = p.radius();
        double scale = Math.max(.045, Math.min(1.1, Math.max(r, p.length() * .10)));
        double phase = clock * .035 + seed;
        double cycle = phase - Math.floor(phase);
        double travel = 1 - Math.pow(1 - progress, 3);
        Vec3 dir = p.direction().lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : p.direction().normalize();
        Vec3 center;
        Vec3 axis;
        Material material = index % 4 == 0 ? Material.FLARE : Material.SPARK;
        double size = scale * (.055 + .065 * other);
        float alpha = (float) (.65 + .35 * other);
        Vec3 normal = Vec3.ZERO;
        double width = size * .28;

        switch (p.shape()) {
            case SLASH, CONE -> {
                double t = (index + .5) / count(p);
                double release = clamp((age - t * 1.6) / Math.max(1, p.duration() - 1.6));
                center = CombatFxGeometry.point(p, 0, t, age).scale(.76 + .20 * other);
                axis = new Vec3(-center.z, .2 + other * .5, center.x).normalize();
                double drift = scale * release * (.18 + .32 * other);
                center = center.add(axis.scale(drift)).add(0, -scale * .45 * release * release, 0);
                double reach = Math.hypot(center.x, center.z);
                if (reach > r && reach > 0) center = new Vec3(center.x * r / reach, center.y, center.z * r / reach);
                size *= (index % 4 == 0 ? 4 : 2.4) * (1 - release * .6);
                material = index % 6 == 0 ? Material.STAR : Material.SPARK;
                alpha *= CombatFxGeometry.strokeOpacity(p, t, age) * (float) (1 - release * .55);
            }
            case BEAM, HELIX, TRACE, WAKE -> {
                var frame = CombatFxGeometry.billboardFrame(dir);
                double t = p.shape() == CombatFxPayload.Shape.BEAM ? seed : cycle;
                double spread = Math.sin(Math.PI * t) * Math.min(.42, p.length() * .07);
                Vec3 side = frame.right().scale(Math.cos(angle + clock * .12))
                        .add(frame.up().scale(Math.sin(angle + clock * .12)));
                center = CombatFxGeometry.point(p, 0, t, CombatFxGeometry.motionTime(p, age, clock)).add(side.scale(spread * other));
                axis = dir;
                size = Math.min(size * (index % 3 == 0 ? 4 : 2.6), p.length() * Math.min(t, 1 - t) * .65);
                width = Math.min(width, size * .28);
                material = index % 5 == 0 ? Material.STAR : Material.SPARK;
                alpha *= (float) (.35 + .65 * Math.pow(Math.sin(Math.PI * t), .35));
                if (p.shape() == CombatFxPayload.Shape.BEAM && index == 0) {
                    center = dir.scale(Math.min(.12, p.length() * .5));
                    normal = dir;
                    axis = frame.up();
                    size = Math.min(.8, p.length() * .18);
                    width = size;
                    material = Material.FLARE;
                }
            }
            case BURST, IMPACT -> {
                double delay = index % 3 == 0 ? .16 : .02 * other;
                double release = clamp((progress - delay) / (1 - delay));
                double flight = 1 - Math.pow(1 - release, 3);
                axis = new Vec3(radial.x, .1 + other * .9 - release * .7, radial.z).normalize();
                center = radial.scale(r * (.12 + .78 * flight) * (.5 + .5 * other))
                        .add(0, r * ((.18 + other * .44) * release - .45 * release * release), 0);
                size *= (index % 4 == 0 ? 4.2 : 2.5) * (1 - release * .72);
                material = index % 7 == 0 ? Material.STAR : Material.SPARK;
                alpha *= (float) (Math.min(1, release * 22) * (1 - release * .5));
                if (index == 0) {
                    center = Vec3.ZERO;
                    size = Math.min(1.4, r * .55) * (.6 + travel);
                    width = size;
                    material = Material.FLARE;
                    alpha = (float) Math.pow(1 - progress, 3);
                }
            }
            case RING, SIGIL, SHOCKWAVE -> {
                double distance = p.shape() == CombatFxPayload.Shape.SHOCKWAVE
                        ? r * (.12 + .82 * travel) : r * (.72 + .24 * other);
                center = radial.scale(distance).add(0, .035 + scale * .26 * cycle, 0);
                axis = UP;
                alpha *= (float) Math.sin(Math.PI * cycle);
                if (index < 2) {
                    center = new Vec3(0, .025 + index * .025, 0);
                    double rotation = clock * .012 * (index == 0 ? 1 : -1);
                    axis = new Vec3(Math.cos(rotation), 0, Math.sin(rotation));
                    normal = UP;
                    size = distance * (index == 0 ? .98 : .67);
                    width = size;
                    material = Material.RUNE;
                    alpha = index == 0 ? .48F : .3F;
                } else if (p.shape() != CombatFxPayload.Shape.SHOCKWAVE && index < 10) {
                    double orbit = (index - 2) * TAU / 8 - clock * .024;
                    center = new Vec3(Math.cos(orbit) * r * .71, .055, Math.sin(orbit) * r * .71);
                    axis = new Vec3(Math.cos(orbit), 0, Math.sin(orbit));
                    normal = UP;
                    size = Math.min(.3, r * .13);
                    width = size;
                    material = Material.RUNE;
                    alpha = .58F;
                } else {
                    size *= 1.8;
                    material = index % 4 == 0 ? Material.STAR : Material.SPARK;
                }
            }
            case AEGIS, DOME -> {
                double theta = angle + clock * .025;
                double elevation = other * Math.PI * .45;
                boolean dome = p.shape() == CombatFxPayload.Shape.DOME;
                double distance = r * (dome ? Math.cos(elevation) : 1.02);
                center = new Vec3(Math.cos(theta) * distance,
                        p.length() * (dome ? Math.sin(elevation) : .12 + .76 * cycle), Math.sin(theta) * distance);
                normal = new Vec3(Math.cos(theta), dome ? Math.sin(elevation) : 0, Math.sin(theta)).normalize();
                axis = UP;
                size *= 1.8;
                alpha *= (float) (.4 + .6 * Math.sin(Math.PI * cycle));
                if (index % 4 == 0) {
                    material = Material.RUNE;
                    size = Math.min(.22, r * .18);
                    width = size;
                } else {
                    material = Material.SPARK;
                    width = size * .18;
                }
            }
            case IMPLOSION, COLLAPSE, ARRIVAL, SINGULARITY -> {
                boolean arrival = p.shape() == CombatFxPayload.Shape.ARRIVAL;
                double distance = p.shape() == CombatFxPayload.Shape.SINGULARITY ? .3 + .7 * (1 - cycle)
                        : arrival ? .12 + .88 * travel : 1 - travel;
                double spin = angle + clock * .10;
                Vec3 outward = new Vec3(Math.cos(spin), (other - .5) * 1.4, Math.sin(spin)).normalize();
                center = outward.scale(r * distance);
                axis = new Vec3(-Math.sin(spin), .16, Math.cos(spin)).add(outward.scale(arrival ? .8 : -.8)).normalize();
                size *= 2.8;
                material = index % 3 == 0 ? Material.WISP : Material.SPARK;
                alpha *= (float) Math.min(1, distance * 5);
            }
            case ORBIT -> {
                int lane = index % CombatFxGeometry.baseStrands(p);
                double t = cycle;
                center = CombatFxGeometry.point(p, lane, t, clock);
                axis = center.subtract(CombatFxGeometry.point(p, lane, t - .012, clock)).normalize();
                size *= index % 5 == 0 ? 3.4 : 1.8;
                width = size * .22;
                material = index % 5 == 0 ? Material.STAR : Material.SPARK;
                alpha *= (float) Math.pow(Math.sin(Math.PI * cycle), .6);
            }
            case SPIRAL, COLUMN -> {
                double t = cycle;
                int lane = index % CombatFxGeometry.strands(p);
                center = CombatFxGeometry.point(p, lane, t, clock);
                axis = center.subtract(CombatFxGeometry.point(p, lane, Math.max(0, t - .02), clock)).normalize();
                size *= 2.8;
                width = size * .24;
                material = index % 6 == 0 ? Material.RUNE : Material.SPARK;
                if (material == Material.RUNE) width = size * .65;
                alpha *= (float) Math.sin(Math.PI * cycle);
            }
            default -> {
                int strand = index % Math.max(1, CombatFxGeometry.strands(p));
                double pathAge = CombatFxGeometry.motionTime(p, age, clock);
                center = CombatFxGeometry.point(p, strand, cycle, pathAge);
                axis = center.subtract(CombatFxGeometry.point(p, strand, Math.max(0, cycle - .02), pathAge)).normalize();
                material = index % 3 == 0 ? Material.WISP : Material.FLARE;
                width = size * .65;
                alpha *= (float) Math.sin(Math.PI * cycle);
            }
        }
        Sprite sprite = new Sprite(center, axis, normal, Math.max(0, size), Math.min(Math.max(0, size), width),
                alpha, index % 5 == 0 ? .6F : index % 2 == 0 ? .2F : 0, material);
        sprite = EquipmentWardGeometry.accent(p, sprite, index, clock);
        return ElementalFxGeometry.accent(p, ElementalFxGeometry.element(p), sprite, index, age, clock);
    }

    public static Vec3[] quad(Sprite sprite, Vec3 camera) {
        Vec3 normal = sprite.normal().lengthSqr() < 1E-8
                ? camera.subtract(sprite.center()).normalize() : sprite.normal().normalize();
        var frame = CombatFxGeometry.billboardFrame(normal);
        Vec3 along = sprite.axis().subtract(frame.normal().scale(sprite.axis().dot(frame.normal())));
        along = along.lengthSqr() < 1E-8 ? frame.up() : along.normalize();
        Vec3 side = frame.normal().cross(along).normalize().scale(sprite.halfWidth());
        along = along.scale(sprite.halfLength());
        Vec3 c = sprite.center();
        return new Vec3[]{c.subtract(side).add(along), c.subtract(side).subtract(along),
                c.add(side).subtract(along), c.add(side).add(along)};
    }

    private static double random(int index, int color) {
        int bits = index * 0x45D9F3B ^ color;
        bits = (bits ^ bits >>> 16) * 0x45D9F3B;
        bits ^= bits >>> 16;
        return (bits & 0xFFFFFF) / 16777216.0;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
