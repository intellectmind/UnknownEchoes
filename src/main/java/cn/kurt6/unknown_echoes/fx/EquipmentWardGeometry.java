package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class EquipmentWardGeometry {
    public enum Style { LEAF, TIDE, ECHO, CRYSTAL, VOID, SUN }
    private static final Style[] STYLES = Style.values();
    private static final double TAU = Math.PI * 2;

    private EquipmentWardGeometry() { }

    public static float tag(String set) {
        Style style = switch (set) {
            case "explorer", "wind_walker" -> Style.LEAF;
            case "echo_traveler", "tide_stalker" -> Style.TIDE;
            case "listener" -> Style.ECHO;
            case "true_sight_shadow" -> Style.CRYSTAL;
            case "silent_watch", "abyss_walker" -> Style.VOID;
            case "echo_oath", "resonator" -> Style.SUN;
            default -> null;
        };
        return style == null ? 0 : 8 + style.ordinal();
    }

    // 仅绑定实体的护甲触发演出使用 8 至 13 的样式标记，武器护盾沿用普通几何。
    public static Style style(CombatFxPayload p) {
        if (p.boss() || p.entityId() < 0 || p.shape() != CombatFxPayload.Shape.AEGIS
                && p.shape() != CombatFxPayload.Shape.DOME) return null;
        int tag = (int) p.span();
        return p.span() == tag && tag >= 8 && tag < 8 + STYLES.length ? STYLES[tag - 8] : null;
    }

    public static Vec3 point(CombatFxPayload p, int strand, double t, double clock) {
        Style style = style(p);
        double a = t * TAU + clock * .04 * (strand == 0 ? 1 : -1);
        double radius = p.radius() * 1.04;
        double height = .22 + .54 * strand;
        switch (style) {
            case LEAF -> { radius *= .96 + .04 * Math.cos(a * 6); height += Math.sin(a * 3) * .08; }
            case TIDE -> height += Math.sin(a * 2 + clock * .1) * .055;
            case ECHO -> { radius *= .98; height += Math.sin(a * 8) * .018; }
            case CRYSTAL -> {
                double step = t * 8;
                int node = Math.min(7, (int) step);
                double a0 = node * TAU / 8, a1 = (node + 1) * TAU / 8;
                if (p.shape() == CombatFxPayload.Shape.DOME) {
                    radius = p.radius() * Math.cos(height * Math.PI / 2);
                    height = Math.sin(height * Math.PI / 2);
                }
                return new Vec3(Math.cos(a0) * (1 - step + node) + Math.cos(a1) * (step - node), 0,
                        Math.sin(a0) * (1 - step + node) + Math.sin(a1) * (step - node))
                        .scale(radius).add(0, p.length() * height, 0);
            }
            case VOID -> { height = .5 + .28 * Math.sin(a) * (strand == 0 ? 1 : -1); radius *= .98; }
            case SUN -> radius *= .92 + .08 * Math.pow(Math.cos(a * 6), 2);
        }
        if (p.shape() == CombatFxPayload.Shape.DOME) {
            double elevation = height * Math.PI / 2;
            radius = p.radius() * Math.cos(elevation);
            height = Math.sin(elevation);
        }
        return new Vec3(Math.cos(a) * radius, p.length() * height, Math.sin(a) * radius);
    }

    public static CombatFxAccentGeometry.Sprite accent(CombatFxPayload p, CombatFxAccentGeometry.Sprite s,
                                                       int index, double clock) {
        Style style = style(p);
        if (style == null) return s;
        double size = s.halfLength(), width = s.halfWidth();
        Vec3 axis = s.axis();
        Vec3 normal = s.normal();
        if (p.shape() == CombatFxPayload.Shape.DOME && p.radius() > 0 && p.length() > 0) {
            normal = new Vec3(s.center().x / (p.radius() * p.radius()), s.center().y / (p.length() * p.length()),
                    s.center().z / (p.radius() * p.radius())).normalize();
        }
        var material = s.material();
        switch (style) {
            case LEAF -> {
                material = CombatFxAccentGeometry.Material.SPORE;
                size *= .75; width = size * .48;
                axis = new Vec3(Math.sin(clock * .12 + index), .6, Math.cos(clock * .12 + index));
            }
            case TIDE -> { material = index % 4 == 0 ? CombatFxAccentGeometry.Material.RUNE : CombatFxAccentGeometry.Material.WISP; }
            case ECHO -> { material = index % 3 == 0 ? CombatFxAccentGeometry.Material.RUNE : CombatFxAccentGeometry.Material.SPARK; }
            case CRYSTAL -> { material = CombatFxAccentGeometry.Material.ICE; size *= 1.1; width = size * .45; }
            case VOID -> { material = CombatFxAccentGeometry.Material.WISP; size *= 1.4; width = size * .22; }
            case SUN -> { material = index % 3 == 0 ? CombatFxAccentGeometry.Material.RUNE : CombatFxAccentGeometry.Material.STAR; width = size; }
        }
        return new CombatFxAccentGeometry.Sprite(s.center(), axis, normal, size, Math.min(size, width),
                s.alpha(), s.tint(), material);
    }

    public static Vec3 panelPoint(CombatFxPayload p, int panel, double t, double clock) {
        double angle = panel * TAU / 6 + clock * .0108;
        double a = t * TAU;
        double side, height;
        switch (style(p)) {
            case LEAF -> {
                side = Math.sin(a) * Math.pow(Math.abs(Math.sin(a)), .6) * p.radius() * .43;
                height = (.5 + Math.cos(a) * .38) * p.length();
            }
            case TIDE, VOID -> {
                boolean tide = style(p) == Style.TIDE;
                angle += tide ? Math.sin(a + clock * .12) * .18 : t * TAU * .3;
                return new Vec3(Math.cos(angle) * p.radius(), p.length() * (.08 + .84 * t), Math.sin(angle) * p.radius());
            }
            case CRYSTAL -> {
                double step = t * 4;
                int node = Math.min(3, (int) step);
                double a0 = node * Math.PI / 2, a1 = (node + 1) * Math.PI / 2;
                side = (Math.sin(a0) * (1 - step + node) + Math.sin(a1) * (step - node)) * p.radius() * .43;
                height = (.5 + (Math.cos(a0) * (1 - step + node) + Math.cos(a1) * (step - node)) * .38) * p.length();
            }
            case SUN -> {
                double ray = .58 + .42 * Math.pow(Math.cos(a * 2), 6);
                side = Math.cos(a) * ray * p.radius() * .43;
                height = (.5 + Math.sin(a) * ray * .36) * p.length();
            }
            default -> {
                double step = t * 6;
                int node = Math.min(5, (int) step);
                double a0 = node * TAU / 6, a1 = (node + 1) * TAU / 6;
                side = (Math.cos(a0) * (1 - step + node) + Math.cos(a1) * (step - node)) * p.radius() * .43;
                height = (.5 + (Math.sin(a0) * (1 - step + node) + Math.sin(a1) * (step - node)) * .34) * p.length();
            }
        }
        return new Vec3(Math.cos(angle) * p.radius() - Math.sin(angle) * side, height,
                Math.sin(angle) * p.radius() + Math.cos(angle) * side);
    }

    public static Vec3[] crystalFacet(CombatFxPayload p, int panel, double clock) {
        return new Vec3[]{panelPoint(p, panel, 0, clock), panelPoint(p, panel, .25, clock),
                panelPoint(p, panel, .5, clock), panelPoint(p, panel, .75, clock)};
    }
}
