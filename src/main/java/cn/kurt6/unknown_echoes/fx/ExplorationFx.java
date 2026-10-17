package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** 坐标以方块底面中心为原点；表现不读取权限或改变机关状态。 */
public final class ExplorationFx {
    private static final int WIND = 0xBDE8BA;
    private static final int WIND_EDGE = 0x527454;
    private static final int TIDE = 0x66C5D8;
    private static final int TIDE_EDGE = 0x305B77;
    private static final int GOLD = 0xE4C38D;
    private static final int GOLD_EDGE = 0x84613C;

    public enum Provision {
        FOLDED(0xB7D7A0, 0x536D48), GRAVITY(0xB6A0ED, 0x624A8B), HUSHED(0x83BCC4, 0x3B5D69);

        private final int core;
        private final int edge;
        private final DustParticleOptions ember;

        Provision(int core, int edge) {
            this.core = core;
            this.edge = edge;
            this.ember = new DustParticleOptions(new Vector3f(
                    ((core >> 16) & 255) / 255F, ((core >> 8) & 255) / 255F, (core & 255) / 255F), .3F);
        }

        public DustParticleOptions ember() {
            return ember;
        }
    }

    private ExplorationFx() { }

    public static List<CombatFxPayload> windFrame(Vec3 base, double height, long time, long salt) {
        if (!Double.isFinite(height) || height < 2 || height > 64
                || Math.floorMod(Math.floorMod(time, 12) + Math.floorMod(salt, 12), 12) != 0) {
            return List.of();
        }
        List<CombatFxPayload> cues = new ArrayList<>(4);
        int steps = Math.max(2, (int) Math.ceil((height - .9) / .55));
        long step = Math.floorMod(Math.floorDiv(time, 12), steps);
        for (int side : new int[]{-1, 1}) {
            double phase = Math.floorMod(step + Math.floorMod(salt, steps)
                    + (side > 0 ? steps / 2 : 0), steps) / (double) steps;
            Vec3 tip = base.add(side * 1.12, .83 + phase * (height - .9), -side * 1.12);
            for (int arm : new int[]{-1, 1}) {
                line(cues, tip.add(arm * .10, -.28, 0), tip,
                        WIND, WIND_EDGE, 10, false);
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> tideDoor(Vec3 base, boolean axisZ, boolean open) {
        List<CombatFxPayload> cues = new ArrayList<>(8);
        for (int face : new int[]{-1, 1}) {
            for (int side : new int[]{-1, 1}) {
                Vec3 tip = doorPoint(base, axisZ, side * .43, open ? .69 : .31, face * .54);
                for (int arm : new int[]{-1, 1}) {
                    Vec3 start = doorPoint(base, axisZ, side * .43 + arm * .07,
                            open ? .49 : .51, face * .54);
                    line(cues, start, tip, open ? TIDE : 0x7FA8BD, TIDE_EDGE, 12, false);
                }
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> tideLocked(Vec3 base, boolean axisZ) {
        List<CombatFxPayload> cues = new ArrayList<>(2);
        for (int face : new int[]{-1, 1}) {
            line(cues, doorPoint(base, axisZ, -.10, .32, face * .54),
                    doorPoint(base, axisZ, .10, .32, face * .54), 0x7FA8BD, TIDE_EDGE, 10, false);
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> buoy(Vec3 base, boolean lit) {
        List<CombatFxPayload> cues = new ArrayList<>(4);
        for (int x : new int[]{-1, 1}) {
            for (int z : new int[]{-1, 1}) {
                Vec3 low = base.add(x * .56, .64, z * .56);
                Vec3 high = lit ? low.add(0, .35, 0) : base.add(x * .42, 1.16, z * .42);
                line(cues, lit ? low : high, lit ? high : low,
                        lit ? TIDE : GOLD, lit ? TIDE_EDGE : GOLD_EDGE, lit ? 14 : 10, true);
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> observatory(Vec3 base) {
        List<CombatFxPayload> cues = new ArrayList<>(6);
        Vec3[] points = {base.add(0, 1.4, -.36), base.add(.36, 1.4, 0),
                base.add(0, 1.4, .36), base.add(-.36, 1.4, 0)};
        for (int i = 0; i < points.length; i++) {
            line(cues, points[i], points[(i + 1) % points.length], GOLD, GOLD_EDGE, 16, false);
        }
        line(cues, base.add(-.12, 1.4, 0), base.add(.12, 1.4, 0), WIND, WIND_EDGE, 12, false);
        line(cues, base.add(0, 1.4, -.12), base.add(0, 1.4, .12), WIND, WIND_EDGE, 12, false);
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> provision(Vec3 base, Provision theme, boolean cooling) {
        if (theme == null) return List.of();
        List<CombatFxPayload> cues = new ArrayList<>(5);
        if (cooling) {
            for (int side : new int[]{-1, 1}) {
                line(cues, base.add(side * .1, 1.12, -.12), base.add(side * .1, 1.12, .12),
                        theme.core, theme.edge, 10, false);
            }
        } else if (theme == Provision.FOLDED) {
            for (int side : new int[]{-1, 1}) {
                for (int end : new int[]{-1, 1}) {
                    line(cues, base.add(side * .3, 1.14, end * .18), base.add(0, 1.26, end * .18),
                            theme.core, theme.edge, 16, false);
                }
            }
        } else if (theme == Provision.GRAVITY) {
            diamond(cues, base.add(0, 1.18, 0), .27, theme.core, theme.edge, 16, false);
            line(cues, base.add(0, 1.14, 0), base.add(0, 1.38, 0), theme.core, theme.edge, 16, false);
        } else {
            for (int row = -1; row <= 1; row++) {
                double half = row == 0 ? .27 : .18;
                line(cues, base.add(-half, 1.16, row * .16), base.add(half, 1.16, row * .16),
                        theme.core, theme.edge, 16, false);
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> tideRune(Vec3 base, boolean purified) {
        List<CombatFxPayload> cues = new ArrayList<>(6);
        if (purified) {
            diamond(cues, base.add(0, 1.05, 0), .28, TIDE, TIDE_EDGE, 16, true);
        } else {
            for (int side : new int[]{-1, 1}) {
                line(cues, base.add(side * .28, 1.05, -.16), base.add(side * .28, 1.05, .16),
                        0xAA9BCC, 0x574965, 10, true);
                for (int end : new int[]{-1, 1}) {
                    line(cues, base.add(side * .28, 1.05, end * .16), base.add(side * .14, 1.05, end * .16),
                            0xAA9BCC, 0x574965, 10, true);
                }
            }
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> tidePillar(Vec3 base) {
        List<CombatFxPayload> cues = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2 * i / 3;
            Vec3 start = base.add(Math.cos(angle) * .24, 1.34, Math.sin(angle) * .24);
            line(cues, start, start.add(0, .2, 0), TIDE, TIDE_EDGE, 16, false);
        }
        return List.copyOf(cues);
    }

    public static List<CombatFxPayload> tideSeat(Vec3 base, boolean ready) {
        List<CombatFxPayload> cues = new ArrayList<>(4);
        if (ready) {
            for (int side : new int[]{-1, 1}) {
                line(cues, base.add(side * .16, .46, -.2), base.add(side * .16, .46, 0),
                        TIDE, TIDE_EDGE, 12, true);
            }
        } else {
            diamond(cues, base.add(0, .47, -.14), .24, TIDE, TIDE_EDGE, 10, true);
        }
        return List.copyOf(cues);
    }

    private static void diamond(List<CombatFxPayload> cues, Vec3 center, double radius,
                                int core, int edge, int duration, boolean boss) {
        Vec3[] points = {center.add(-radius, 0, 0), center.add(0, 0, -radius),
                center.add(radius, 0, 0), center.add(0, 0, radius)};
        for (int i = 0; i < points.length; i++) {
            line(cues, points[i], points[(i + 1) % points.length], core, edge, duration, boss);
        }
    }

    private static Vec3 doorPoint(Vec3 base, boolean axisZ, double x, double y, double z) {
        return base.add(axisZ ? x : z, y, axisZ ? z : x);
    }

    private static void line(List<CombatFxPayload> cues, Vec3 from, Vec3 to,
                             int core, int edge, int duration, boolean boss) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (!Double.isFinite(length) || length <= 1E-5) return;
        CombatFxPayload cue = new CombatFxPayload(Shape.TRACE, from, delta.scale(1 / length),
                0, (float) length, 0, core, edge, duration, boss, -1);
        if (cue.valid()) cues.add(cue);
    }
}
