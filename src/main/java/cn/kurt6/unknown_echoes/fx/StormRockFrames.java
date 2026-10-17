package cn.kurt6.unknown_echoes.fx;

import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public final class StormRockFrames {
    private StormVolumeGeometry.Rock[] previous = new StormVolumeGeometry.Rock[StormVolumeGeometry.ROCKS_PER_STORM];
    private StormVolumeGeometry.Rock[] current = new StormVolumeGeometry.Rock[StormVolumeGeometry.ROCKS_PER_STORM];
    private Vec3 center;
    private float height;
    private boolean deep;
    private long tick = Long.MIN_VALUE;
    private int count;

    public void advance(Vec3 center, float height, boolean deep, long now, int requestedCount) {
        int nextCount = Math.clamp(requestedCount, 0, current.length);
        boolean sameShape = center.equals(this.center) && height == this.height && deep == this.deep;
        boolean sameTick = sameShape && tick == now;
        if (sameTick && nextCount == count) return;
        boolean continuous = sameShape && tick != Long.MIN_VALUE && now - tick == 1;
        if (!sameTick) {
            var reusable = previous;
            previous = current;
            current = reusable;
        }
        for (int i = sameTick ? count : 0; i < nextCount; i++) {
            current[i] = StormVolumeGeometry.rock(center, height, deep, now, i);
            if (!continuous || i >= count) previous[i] = current[i];
        }
        Arrays.fill(previous, nextCount, previous.length, null);
        Arrays.fill(current, nextCount, current.length, null);
        count = nextCount;
        this.center = center;
        this.height = height;
        this.deep = deep;
        tick = now;
    }

    public StormVolumeGeometry.Rock previous(int index) { return previous[index]; }
    public StormVolumeGeometry.Rock current(int index) { return current[index]; }
    public int size() { return count; }

    public boolean wraps(int index) {
        return Math.abs(previous[index].position().y - current[index].position().y) > height * 0.5D;
    }
}
