package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 调用方先确认个人进度；这里只描述可见节点、已允许的指引与成功召唤。 */
public final class ProgressionFx {
    public enum Theme {
        WIND(0xBDE8BA, 0x527454), TIDE(0x66C5D8, 0x305B77),
        TRUE_SIGHT(0xE6DBC2, 0x8B7C5F), GRAVITY(0xB6A0ED, 0x624A8B),
        VOID(0x8DABDC, 0x374F80), WORLD(0xE4C38D, 0x84613C);

        private final int core;
        private final int edge;

        Theme(int core, int edge) {
            this.core = core;
            this.edge = edge;
        }

        public int coreColor() { return core; }

        public int edgeColor() { return edge; }
    }

    private ProgressionFx() { }

    public static List<CombatFxPayload> node(Vec3 center, Theme theme, boolean complete) {
        List<CombatFxPayload> cues = new ArrayList<>(3);
        sigil(cues, center, complete ? .5 : .34, theme, 20);
        if (complete) {
            for (int side : new int[]{-1, 1}) {
                Vec3 start = center.add(side * .38, .1, 0);
                trace(cues, start, center.add(0, .32, 0), .07, theme, 16);
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> guidance(Vec3 source, Vec3 target, Theme theme) {
        List<CombatFxPayload> cues = new ArrayList<>(2);
        sigil(cues, source, .28, theme, 20);
        trace(cues, source.add(0, .05, 0), target.add(0, .05, 0), .2, theme, 20);
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> convergence(Vec3 center, Theme theme) {
        List<CombatFxPayload> cues = new ArrayList<>(3);
        sigil(cues, center, .7, theme, 24);
        for (int side : new int[]{-1, 1}) {
            Vec3 start = center.add(side * .55, .1, 0);
            trace(cues, start, center.add(0, .5, 0), .1, theme, 18);
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> summon(Vec3 feet, double width, double height, Theme theme) {
        CombatFxPayload cue = BossLifecycleFx.plan(Shape.BOSS_SUMMON, feet, width, height, theme.core, theme.edge,
                BossLifecycleStyle.forProgression(theme));
        return cue == null ? List.of() : List.of(cue);
    }

    public static List<CombatFxPayload> entrancePulse(Vec3 feet, double width, boolean ready) {
        List<CombatFxPayload> cues = new ArrayList<>(3);
        if (ready) {
            cues.addAll(convergence(feet.add(0, .12, 0), Theme.WORLD));
        } else if (Double.isFinite(width) && width > 0) {
            sigil(cues, feet.add(0, .12, 0), Math.min(2.25, width * .65), Theme.WORLD, 12);
        }
        return bossCues(cues);
    }

    public static List<CombatFxPayload> entranceLinks(List<Vec3> pylons, Vec3 target) {
        List<CombatFxPayload> cues = new ArrayList<>(5);
        Theme[] aspects = {Theme.WIND, Theme.TIDE, Theme.TRUE_SIGHT, Theme.GRAVITY, Theme.VOID};
        for (int i = 0; i < Math.min(aspects.length, pylons.size()); i++) {
            trace(cues, pylons.get(i), target, .3, aspects[i], 10);
        }
        return bossCues(cues);
    }

    public static List<CombatFxPayload> recordReceipt(Vec3 table) {
        List<CombatFxPayload> cues = new ArrayList<>(4);
        Vec3[] corners = {table.add(-.3, 1.16, -.2), table.add(.3, 1.16, -.2),
                table.add(.3, 1.16, .2), table.add(-.3, 1.16, .2)};
        for (int i = 0; i < corners.length; i++) {
            trace(cues, corners[i], corners[(i + 1) % corners.length], 0, Theme.WORLD, 12);
        }
        return List.copyOf(cues);
    }

    private static void sigil(List<CombatFxPayload> cues, Vec3 center, double radius, Theme theme, int duration) {
        add(cues, Shape.SIGIL, center, Vec3.ZERO, radius, 0, theme, duration);
    }

    private static List<CombatFxPayload> bossCues(List<CombatFxPayload> cues) {
        return cues.stream().map(cue -> new CombatFxPayload(cue.shape(), cue.origin(), cue.direction(),
                cue.radius(), cue.length(), cue.span(), cue.core(), cue.edge(), cue.duration(), true, -1)).toList();
    }

    private static void trace(List<CombatFxPayload> cues, Vec3 start, Vec3 end, double arch,
                              Theme theme, int duration) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (!Double.isFinite(length) || length <= 1E-5) return;
        add(cues, Shape.TRACE, start, delta.scale(1 / length), arch, length, theme, duration);
    }

    private static void add(List<CombatFxPayload> cues, Shape shape, Vec3 origin, Vec3 direction,
                            double radius, double length, Theme theme, int duration) {
        CombatFxPayload cue = new CombatFxPayload(shape, origin, direction, (float) radius,
                (float) length, 0, theme.core, theme.edge, duration, false, -1);
        if (cue.valid()) cues.add(cue);
    }
}
