package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 机关只传已确认的表现状态，演出规划不读取资格、奖励或隐藏答案。 */
public final class MechanismFx {
    private static final int[] RESONANCE_CORE = {0xBDE8BA, 0x66C5D8, 0xE6DBC2, 0xB6A0ED, 0x8DABDC};
    private static final int[] RESONANCE_EDGE = {0x527454, 0x305B77, 0x8B7C5F, 0x624A8B, 0x374F80};
    private static final int INK = 0xA8CCDE;
    private static final int INK_EDGE = 0x456E83;
    private static final int GOLD = 0xE4C38D;
    private static final int GOLD_EDGE = 0x84613C;

    private MechanismFx() { }

    public static void emit(ServerLevel level, List<CombatFxPayload> cues) {
        for (CombatFxPayload cue : cues) {
            CombatFx.emit(level, cue.shape(), cue.origin(), cue.direction(), cue.radius(), cue.length(),
                    cue.span(), cue.core(), cue.edge(), cue.duration(), cue.boss());
        }
    }

    public static void resonanceNode(ServerLevel level, Vec3 center, int index) {
        if (index < 0 || index >= RESONANCE_CORE.length) return;
        CombatFx.emit(level, Shape.BURST, center, Vec3.ZERO, .32, 0, 0,
                RESONANCE_CORE[index], RESONANCE_EDGE[index], 8, false);
    }

    public static List<CombatFxPayload> ritualFrame(Vec3 altar, List<Vec3> pylons, int age, int duration) {
        if (age < 0 || age >= duration || age % 8 != 0) return List.of();
        List<CombatFxPayload> cues = new ArrayList<>(7);
        Vec3 focus = altar.add(0, 4.5 + 2.5 * smooth((age - 90) / 40.0), 0);
        for (int i = 0; i < Math.min(5, pylons.size()); i++) {
            double reach = smooth((age - i * 6) / 64.0);
            if (reach <= 0) continue;
            Vec3 source = pylons.get(i);
            trace(cues, source, source.lerp(focus, reach), .8 * reach,
                    RESONANCE_CORE[i], RESONANCE_EDGE[i], 10);
        }
        if (age >= 45 && age % 16 == 0) {
            double radius = age < 135 ? 1.2 + .8 * smooth((age - 45) / 70.0)
                    : 2.0 - 1.25 * smooth((age - 135) / 45.0);
            add(cues, Shape.SIGIL, focus, Vec3.ZERO, radius, 0, GOLD, GOLD_EDGE, 18);
        }
        if (age >= 150 && age % 16 == 0) {
            add(cues, Shape.IMPLOSION, focus, Vec3.ZERO, 1.25, 1.4, INK, INK_EDGE, 12);
        }
        return List.copyOf(cues);
    }

    public static void ritualOpening(ServerLevel level, Vec3 altar) {
        CombatFx.emit(level, Shape.SIGIL, altar.add(0, 1.1, 0), Vec3.ZERO,
                .9, 0, 0, GOLD, GOLD_EDGE, 20, false);
    }

    public static void ritualCollapse(ServerLevel level, Vec3 altar) {
        CombatFx.emit(level, Shape.IMPLOSION, altar.add(0, 3, 0), Vec3.ZERO,
                1.4, 1.5, 0, INK, INK_EDGE, 18, false);
    }

    public static List<CombatFxPayload> archiveFrame(Vec3 table, int stage, int age) {
        if (age < 0 || age % 8 != 0 || stage < 1 || stage > 3) return List.of();
        List<CombatFxPayload> cues = new ArrayList<>(2);
        if (stage == 1) {
            int row = (age / 16) % 3;
            double reach = .25 + (age % 16) / 16.0;
            Vec3 start = table.add(-.24, 1.14, -.17 + row * .17);
            trace(cues, start, start.add(.48 * reach, 0, 0), .018, INK, INK_EDGE, 10);
        } else if (stage == 2 && age % 16 == 0) {
            add(cues, Shape.SIGIL, table.add(0, 1.18, 0), Vec3.ZERO, .48, 0, GOLD, GOLD_EDGE, 16);
        } else if (stage == 3) {
            double lift = .04 * Math.sin(age * .1);
            for (int side : new int[]{-1, 1}) {
                Vec3 start = table.add(side * .12, 1.55 + lift, -.1);
                trace(cues, start, start.add(side * .34, .12, .23), .12, GOLD, GOLD_EDGE, 10);
            }
        }
        return List.copyOf(cues);
    }

    public static void archiveSeal(ServerLevel level, Vec3 table, boolean archived) {
        CombatFx.emit(level, archived ? Shape.BURST : Shape.IMPACT, table.add(0, 1.22, 0), Vec3.ZERO,
                archived ? .55 : .42, 0, 0, GOLD, GOLD_EDGE, 12, false);
    }

    public static void gravityBoundary(ServerLevel level, Vec3 center, double radius, boolean inverted) {
        CombatFx.emit(level, Shape.RING, center, Vec3.ZERO, radius, 0, 0,
                inverted ? 0xBCA4ED : GOLD, inverted ? 0x614487 : GOLD_EDGE, 20, false);
    }

    public static List<CombatFxPayload> gravityFrame(Vec3 center, double radius, double height,
                                                    boolean inverted, long time) {
        if (Math.floorMod(time, 8L) != 0L || radius <= 0 || height < 1.8) return List.of();
        List<CombatFxPayload> cues = new ArrayList<>(9);
        double phase = Math.floorMod(time, 40L) / 40.0;
        double y = .65 + (inverted ? phase : 1 - phase) * (height - 1.8);
        double sign = inverted ? 1 : -1;
        int core = inverted ? 0xBCA4ED : GOLD;
        int edge = inverted ? 0x614487 : GOLD_EDGE;
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI * 2 / 3;
            Vec3 start = center.add(Math.cos(angle) * radius * .7, y, Math.sin(angle) * radius * .7);
            Vec3 tip = start.add(0, sign * .55, 0);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(.18);
            trace(cues, start, tip, 0, core, edge, 8);
            trace(cues, tip.add(tangent).add(0, -sign * .2, 0), tip, 0, core, edge, 8);
            trace(cues, tip.subtract(tangent).add(0, -sign * .2, 0), tip, 0, core, edge, 8);
        }
        return List.copyOf(cues);
    }

    private static void trace(List<CombatFxPayload> cues, Vec3 start, Vec3 end, double arch,
                              int core, int edge, int duration) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1E-5 || !Double.isFinite(length)) return;
        add(cues, Shape.TRACE, start, delta.scale(1 / length), arch, length, core, edge, duration);
    }

    private static void add(List<CombatFxPayload> cues, Shape shape, Vec3 origin, Vec3 direction,
                            double radius, double length, int core, int edge, int duration) {
        CombatFxPayload cue = new CombatFxPayload(shape, origin, direction, (float) radius, (float) length,
                0, core, edge, duration, false, -1);
        if (cue.valid()) cues.add(cue);
    }

    private static double smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }
}
