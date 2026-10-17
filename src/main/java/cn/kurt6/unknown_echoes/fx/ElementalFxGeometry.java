package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleVisuals;
import net.minecraft.world.phys.Vec3;

public final class ElementalFxGeometry {
    private static final ParticleKind[] ELEMENTS = ParticleKind.values();
    private ElementalFxGeometry() { }

    // 配色对来自同一权威表；不能仅凭色相把普通武器或 Boss 误判成附魔元素。
    public static ParticleKind element(CombatFxPayload p) {
        if (p.boss()) return null;
        for (ParticleKind kind : ELEMENTS) {
            if (p.core() == ParticleVisuals.core(kind) && p.edge() == ParticleVisuals.edge(kind)) return kind;
        }
        return null;
    }

    public static Vec3 bolt(CombatFxPayload p, int strand, double t, double age) {
        Vec3 dir = p.direction().lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : p.direction().normalize();
        var frame = CombatFxGeometry.billboardFrame(dir);
        double step = t * 12;
        int node = Math.min(11, (int) step);
        double blend = step - node;
        int phase = (int) Math.floor(age / 3);
        double x = jitter(node, strand, phase) * (1 - blend) + jitter(node + 1, strand, phase) * blend;
        double y = jitter(node, strand + 7, phase) * (1 - blend) + jitter(node + 1, strand + 7, phase) * blend;
        double spread = Math.sin(Math.PI * t) * Math.min(RealmDisasterFx.isLightning(p) ? 1.35 : .28,
                p.length() * .14) * (strand == 0 ? 1 : .65);
        return dir.scale(p.length() * t).add(frame.right().scale(x * spread)).add(frame.up().scale(y * spread));
    }

    private static double jitter(int node, int strand, int phase) {
        int bits = node * 73428767 ^ strand * 912931 ^ phase * 438289;
        bits ^= bits >>> 13;
        bits *= 1274126177;
        return (bits & 65535) / 32767.5 - 1;
    }

    public static CombatFxAccentGeometry.Sprite accent(CombatFxPayload p, ParticleKind element,
            CombatFxAccentGeometry.Sprite original, int index, double age, double clock) {
        if (element == null || original.material() == CombatFxAccentGeometry.Material.RUNE
                || index == 0 && original.material() == CombatFxAccentGeometry.Material.FLARE) return original;
        var shape = p.shape();
        // 线状演出的端点归调用方裁剪；元素变化只改贴图，不偏移端点外的几何。
        boolean line = shape == CombatFxPayload.Shape.BEAM || shape == CombatFxPayload.Shape.TRACE
                || shape == CombatFxPayload.Shape.HELIX || shape == CombatFxPayload.Shape.WAKE;
        double progress = Math.max(0, Math.min(1, age / p.duration()));
        double seed = (index * .61803398875) % 1;
        double angle = index * 2.39996323;
        double radius = Math.min(2.5, p.radius());
        double size = original.halfLength();
        double width = original.halfWidth();
        Vec3 center = original.center(), axis = original.axis();
        float alpha = original.alpha(), tint = original.tint();
        var material = switch (element) {
            case FIRE -> CombatFxAccentGeometry.Material.FLAME;
            case FROST -> CombatFxAccentGeometry.Material.ICE;
            case VENOM -> CombatFxAccentGeometry.Material.SPORE;
            case THUNDER -> CombatFxAccentGeometry.Material.LIGHTNING;
            case HOLY -> CombatFxAccentGeometry.Material.STAR;
            case EARTH -> CombatFxAccentGeometry.Material.CHIP;
            case VOID, SHADOW -> CombatFxAccentGeometry.Material.WISP;
        };
        if (!line) {
            switch (element) {
                case FIRE -> {
                    double rise = radius * progress * (.25 + seed * .7);
                    center = center.add(Math.sin(angle + progress * 6) * rise * .18, rise, Math.cos(angle + progress * 6) * rise * .18);
                    axis = new Vec3(Math.sin(angle + progress * 4) * .25, 1, 0);
                    size *= 1.7;
                    width = size * .48;
                    alpha *= (float) (1 - progress * .45);
                }
                case FROST -> {
                    center = center.add(0, -.3 * radius * progress * progress, 0);
                    axis = new Vec3(Math.sin(angle + progress), .7, Math.cos(angle + progress));
                    size *= 1.35;
                    width = size * .46;
                    tint = .7F;
                }
                case VENOM -> {
                    center = center.add(Math.sin(clock * .09 + angle) * radius * .06, progress * radius * .55, 0);
                    size *= 1.5 + progress;
                    width = size;
                    alpha *= .55F;
                }
                case THUNDER -> { size *= 2.0; width = size * .55; tint = .7F; }
                case HOLY -> {
                    center = center.add(0, radius * progress * .32, 0);
                    size *= 1.4;
                    width = size;
                    alpha *= (float) (.6 + .4 * Math.pow(Math.sin(clock * .25 + angle), 2));
                }
                case EARTH -> {
                    center = center.add(0, radius * (.8 * progress - 1.1 * progress * progress), 0);
                    axis = new Vec3(Math.cos(angle + age * .15), Math.sin(angle + age * .15), .3);
                    size *= 1.45;
                    width = size * .72;
                    tint = .1F;
                }
                case VOID -> { size *= 1.9; width = size * .4; alpha *= .8F; }
                case SHADOW -> {
                    center = center.add(Math.sin(angle) * radius * progress * .12, 0, Math.cos(angle) * radius * progress * .12);
                    size *= 2.2;
                    width = size * .25;
                    alpha *= .75F;
                }
            }
        }
        return new CombatFxAccentGeometry.Sprite(center, axis, original.normal(), size,
                Math.min(size, width), alpha, tint, material);
    }
}
