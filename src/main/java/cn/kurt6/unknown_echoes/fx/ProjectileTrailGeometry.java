package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class ProjectileTrailGeometry {
    public enum Style { TIDE, SPORE }

    private ProjectileTrailGeometry() { }

    public static CombatFxPayload cue(Vec3 from, Vec3 to, int core, int edge) {
        return cue(from, to, core, edge, Style.TIDE);
    }

    public static CombatFxPayload cue(Vec3 from, Vec3 to, int core, int edge, Style style) {
        if (from == null || to == null) return null;
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        // 只描实际走过的一段；瞬移、插地和零位移不能留下跨场景连线。
        if (!Double.isFinite(distance) || distance < .02 || distance > 8) return null;
        var cue = new CombatFxPayload(CombatFxPayload.Shape.TRACE, from, delta.normalize(), 0,
                (float) distance, 4 + style.ordinal(), core, edge, 8, false, -1);
        return cue.valid() ? cue : null;
    }

    // TRACE 的 span 为 4 或 5 时表示客户端弹体尾流；普通引导线使用 0。
    public static Style style(CombatFxPayload p) {
        if (p.boss() || p.entityId() >= 0 || p.shape() != CombatFxPayload.Shape.TRACE || p.radius() != 0) return null;
        return p.span() == 4 ? Style.TIDE : p.span() == 5 ? Style.SPORE : null;
    }

    public static Vec3 point(CombatFxPayload p, int strand, double t, double age) {
        Vec3 direction = p.direction().normalize();
        Vec3 center = direction.scale(p.length() * t);
        if (strand == 0) return center;
        var frame = CombatFxGeometry.billboardFrame(direction);
        double progress = Math.max(0, Math.min(1, age / p.duration()));
        double angle = t * Math.PI * 1.5 + strand * Math.PI + progress * .7;
        double width = Math.min(.16, p.length() * .12) * Math.sin(Math.PI * t) * (1 + progress * .35);
        return center.add(frame.right().scale(Math.cos(angle) * width)).add(frame.up().scale(Math.sin(angle) * width));
    }

    public static double width(CombatFxPayload p, double t) {
        double width = .018 + .075 * Math.min(1, p.length()) * Math.pow(t, .7);
        if (style(p) == Style.SPORE) width *= .48;
        return Math.min(width, p.length() * Math.min(t, 1 - t) * .38);
    }

    public static int strands(CombatFxPayload p) {
        return style(p) == Style.SPORE ? 1 : 3;
    }

    public static int accents(CombatFxPayload p) {
        return Math.min(32, 8 + (int) Math.ceil(p.length() * 6));
    }

    public static CombatFxAccentGeometry.Sprite sprite(CombatFxPayload p, int index, double age) {
        double progress = Math.max(0, Math.min(1, age / p.duration()));
        double seed = ((index + 1) * .61803398875) % 1;
        double t = (index + .5) / accents(p);
        Vec3 direction = p.direction().normalize();
        var frame = CombatFxGeometry.billboardFrame(direction);
        double angle = index * 2.39996323 + progress * (style(p) == Style.SPORE ? 1.8 : -.7);
        double spread = Math.min(.25, p.length() * .15) * Math.sin(Math.PI * t) * (.25 + progress * .75);
        Vec3 center = direction.scale(p.length() * t).add(frame.right().scale(Math.cos(angle) * spread))
                .add(frame.up().scale(Math.sin(angle) * spread));
        boolean spore = style(p) == Style.SPORE;
        double size = spore ? (.03 + .035 * seed) * (1 + progress * .7) : (.05 + .09 * seed) * (1 - progress * .5);
        double margin = p.length() * Math.min(t, 1 - t);
        size = Math.min(size, margin * (spore ? .48 : .7));
        double width = size * (spore ? 1 : .22);
        var material = spore ? CombatFxAccentGeometry.Material.SPORE
                : index % 4 == 0 ? CombatFxAccentGeometry.Material.WISP : CombatFxAccentGeometry.Material.SPARK;
        return new CombatFxAccentGeometry.Sprite(center, direction, Vec3.ZERO, size, width,
                (float) ((spore ? .48 : .8) * Math.pow(1 - progress, .7)), spore ? .1F : .5F, material);
    }
}
