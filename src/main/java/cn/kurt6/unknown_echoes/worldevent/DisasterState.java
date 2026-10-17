package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class DisasterState {
    public static final int WARNING_TICKS = 200;
    public static final int STRIKE_WARNING_TICKS = 40;
    public static final int STRIKE_INTERVAL_TICKS = 140;
    public static final int MAX_MARKS = 3;
    public static final int MAX_BROKEN_BLOCKS = 24;
    public static final int FADE_TICKS = 60;
    public static final int PRESENTATION_INTERVAL_TICKS = 10;
    public static final double STORM_RADIUS = 3.75D;
    public static final double RUPTURE_RADIUS = 4.5D;

    public static int effectRadius(int worldEventRadius) {
        return Math.clamp((long) worldEventRadius * 4, 1, 128);
    }

    private final List<Mark> marks = new ArrayList<>();
    private long lastWave = Long.MIN_VALUE;
    private int brokenBlocks;
    private long lastWindTick = Long.MIN_VALUE;
    private long sceneSampleTick = Long.MIN_VALUE;
    private int sceneHeight;

    public void sampleSceneHeight(int height, long now) {
        sceneHeight = Math.clamp(height, 0, 96);
        sceneSampleTick = now;
    }

    public int windHeight(long now) {
        return sceneSampleTick != Long.MIN_VALUE && now >= sceneSampleTick
                && now - sceneSampleTick <= PRESENTATION_INTERVAL_TICKS ? sceneHeight : 0;
    }

    public boolean beginWindTick(long now) {
        if (lastWindTick != Long.MIN_VALUE && now <= lastWindTick) return false;
        lastWindTick = now;
        return true;
    }

    public static final class Mark {
        private final Vec3 center;
        private final long impactTime;
        private long lastWarningTime;

        private Mark(Vec3 center, long now) {
            this.center = center;
            impactTime = now + STRIKE_WARNING_TICKS;
            lastWarningTime = now;
        }

        public Vec3 center() { return center; }
        public long impactTime() { return impactTime; }

        public boolean warningIsContinuous(long now) {
            return now >= lastWarningTime && now - lastWarningTime <= PRESENTATION_INTERVAL_TICKS;
        }

        public void presented(long now) { lastWarningTime = now; }

        public float remaining(long now) {
            return (float) Math.clamp((impactTime - now) / (double) STRIKE_WARNING_TICKS, 0.0D, 1.0D);
        }
    }

    public List<Mark> marks() { return List.copyOf(marks); }

    public boolean beginWave(long now, long start, long end) {
        if (now < start || now - start < WARNING_TICKS || end - now <= STRIKE_WARNING_TICKS + FADE_TICKS
                || !marks.isEmpty() || lastWave != Long.MIN_VALUE
                && (now < lastWave || now - lastWave < STRIKE_INTERVAL_TICKS)) return false;
        lastWave = now;
        return true;
    }

    public boolean add(Vec3 center, long now) {
        if (marks.size() >= MAX_MARKS || marks.stream().anyMatch(mark ->
                mark.center().distanceToSqr(center) < 144.0D)) return false;
        marks.add(new Mark(center, now));
        return true;
    }

    public void remove(Mark mark) { marks.remove(mark); }

    public void clearMarks() { marks.clear(); }

    public static float intensity(long now, long start, long end) {
        if (now < start || now >= end) return 0;
        double rise = Math.clamp((now - start) / (double) WARNING_TICKS, 0.0D, 1.0D);
        double fall = Math.clamp((end - now) / (double) FADE_TICKS, 0.0D, 1.0D);
        return (float) (rise * rise * (3 - 2 * rise) * fall * fall * (3 - 2 * fall));
    }

    public boolean canBreakBlock() {
        return brokenBlocks < Math.min(MAX_BROKEN_BLOCKS, ServerConfig.DISASTER_MAX_BROKEN_BLOCKS.get());
    }

    public void recordBrokenBlock() { brokenBlocks++; }

    public static boolean hits(Vec3 center, Vec3 feet, boolean deep) {
        double dx = feet.x - center.x;
        double dz = feet.z - center.z;
        double radius = deep ? RUPTURE_RADIUS : STORM_RADIUS;
        double dy = feet.y - center.y;
        return dx * dx + dz * dz <= radius * radius
                && dy >= -0.5D && dy <= (deep ? 0.65D : 3.0D);
    }
}
