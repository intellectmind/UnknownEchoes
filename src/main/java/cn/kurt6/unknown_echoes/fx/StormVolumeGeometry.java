package cn.kurt6.unknown_echoes.fx;

import net.minecraft.world.phys.Vec3;

public final class StormVolumeGeometry {
    public static final int MAX_VISIBLE_STORMS = 2;
    public static final int VIEW_DISTANCE = 320;
    public static final int CLOUDS_PER_TICK = 24;
    public static final int ROCKS_PER_STORM = 72;
    public static final int MAX_LIVE_CLOUDS = 960;
    public static final int CLOUD_LIFETIME = 40;
    public static final double HORIZONTAL_SCALE = 4.0D;
    public static final double ROTATION_SPEED_SCALE = 1.5D;

    private StormVolumeGeometry() { }

    public record Cloud(Vec3 position, float size, boolean dust, long born) { }
    public record Rock(Vec3 position, Vec3 rotation, float size) { }

    public static Cloud cloud(Vec3 center, float height, boolean deep, long tick, int index) {
        double time = Math.floorMod(tick, 120000L);
        double layer = fraction(index * 0.6180339887D + time * 0.013D);
        double angle = index * 2.399963D + time * (deep ? -0.032D : 0.075D) * (1.5D - layer) * ROTATION_SPEED_SCALE;
        boolean dust = index % 5 == 0;
        boolean canopy = !deep && index % 5 == 1;
        if (deep && !dust) angle = index % 3 * Math.PI * 2 / 3 + layer * 1.4D - time * 0.018D * ROTATION_SPEED_SCALE;
        double radius = dust ? 6 + 18 * layer : deep ? 6 + layer * 10
                : canopy ? 18 + layer * 12 : 2.5D + Math.pow(layer, 1.6D) * 19;
        double y = dust ? 0.5D + layer * 1.5D : (canopy ? 0.78D + layer * 0.1D : 0.04D + layer * 0.86D) * height;
        double size = dust ? 1.5D : deep ? 1.6D + layer * 2 : canopy ? 5.5D : 2.2D + layer * 3.2D;
        radius *= HORIZONTAL_SCALE;
        size = Math.min(size * 3.0D, Math.max(0.5D, height / 5.0D));
        y = Math.min(height - size - 0.5D, y);
        Vec3 position = center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
                .add(axis(y / height, tick, deep));
        return new Cloud(position, (float) size, dust, tick);
    }

    public static Rock rock(Vec3 center, float height, boolean deep, double time, int index) {
        double phase = index * 0.6180339887D;
        double travel = fraction(phase + time / (deep ? 720.0D : 280.0D));
        boolean falling = deep && index % 4 == 0;
        double fall = fraction(phase + time / 140.0D);
        double descent = Math.clamp((fall - 0.42D) / 0.58D, 0, 1);
        double layer = deep ? falling ? 0.85D * (1 - descent * descent) : 0.2D + fraction(phase) * 0.65D : travel;
        // 角速度只依赖固定碎片序号，不能把累计时间乘上不断变化的升降进度。
        double speed = (deep ? -0.019D : 0.055D) * (0.85D + fraction(index * 0.173D) * 0.3D) * ROTATION_SPEED_SCALE;
        double angle = index * 2.399963D + time * speed + Math.sin(time * 0.013D + index) * 0.22D;
        double radius = deep ? 5 + fraction(index * 0.271D) * 13 : 3 + Math.pow(layer, 1.6D) * 18;
        radius *= HORIZONTAL_SCALE;
        double y = (0.04D + layer * 0.85D) * height;
        if (deep) y += Math.sin(time * 0.04D + index) * 0.6D;
        float size = (float) ((deep ? 0.9D : 0.45D) + fraction(index * 0.417D) * (deep ? 0.9D : 0.65D));
        if (deep && index % 8 == 1) size *= 1.7F;
        if (falling) size *= (float) Math.min(1, Math.sin(fall * Math.PI) * 5);
        if (!deep) size *= (float) Math.min(1, Math.sin(travel * Math.PI) * 4);
        y = Math.clamp(y, size + 0.1D, height - size - 0.1D);
        return new Rock(center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius).add(axis(y / height, time, deep)),
                new Vec3(time * 0.07D * ROTATION_SPEED_SCALE + index, time * 0.04D * ROTATION_SPEED_SCALE,
                        time * 0.05D * ROTATION_SPEED_SCALE + index * 0.3D), size);
    }

    public static Vec3 cloudPosition(Cloud cloud, Vec3 center, float height, boolean deep, double age) {
        Vec3 offset = cloud.position().subtract(center);
        Vec3 radial = offset.subtract(axis(offset.y / height, cloud.born(), deep));
        double angle = age * (deep ? -0.022D : 0.035D) * (1.4D - offset.y / height) * ROTATION_SPEED_SCALE;
        double y = Math.min(height - cloud.size() - 0.5D, offset.y + (cloud.dust() ? 0.012D : 0.05D) * age);
        double inflow = cloud.dust() ? 1 - .48 * Math.clamp(age / CLOUD_LIFETIME, 0, 1) : 1;
        return center.add((radial.x * Math.cos(angle) - radial.z * Math.sin(angle)) * inflow, y,
                (radial.x * Math.sin(angle) + radial.z * Math.cos(angle)) * inflow).add(axis(y / height, cloud.born() + age, deep));
    }

    public static int lightningPeriod(boolean deep) { return deep ? 88 : 64; }

    public static boolean lightningAt(long tick, boolean deep) {
        long phase = Math.floorMod(tick, lightningPeriod(deep));
        return phase == 35 || phase == 39;
    }

    public static float cloudFlash(long tick, boolean deep) {
        double phase = Math.floorMod(tick, lightningPeriod(deep));
        return (float) Math.max(pulse(phase - 35), .65 * pulse(phase - 39));
    }

    public static float cloudIllumination(Vec3 offset, float height, long tick, boolean deep) {
        double reach = 24 + height * .35;
        double inner = Math.exp(-(offset.x * offset.x + offset.z * offset.z) / (reach * reach));
        return cloudFlash(tick, deep) * (float) (.08 + .76 * inner);
    }

    private static double pulse(double age) { return age < 0 || age > 6 ? 0 : Math.pow(1 - age / 6, 2); }

    public static Vec3 inflow(double radius, double height, boolean deep, double orientation,
                              int strand, double t, double age) {
        double progress = Math.clamp(age / 26.0 * .78 + t * .22, 0, 1);
        double distance = radius * (1 - .92 * progress);
        double angle = orientation + strand * Math.PI * 2 / 3 + (deep ? -1 : 1) * progress * 4.8;
        double y = .18 + Math.min(deep ? 1.3 : 6, height * .35) * progress * progress * progress;
        return new Vec3(Math.cos(angle) * distance, y, Math.sin(angle) * distance);
    }

    public static float cloudAlpha(double age) {
        return (float) (0.65D * Math.clamp(age / 6, 0, 1) * Math.clamp((CLOUD_LIFETIME - age) / 12, 0, 1));
    }

    public static float cloudCameraFade(double distance, double size) {
        double t = Math.clamp((distance - .6) / (Math.max(.5, size) * 1.2 + 1.5), 0, 1);
        return (float) (t * t * (3 - 2 * t));
    }

    public static int cloudIndex(long tick, int slot) {
        return (int) (Math.floorMod(tick, 256L) * CLOUDS_PER_TICK + slot);
    }

    private static Vec3 axis(double layer, double time, boolean deep) {
        double bend = layer * layer * (deep ? 1.5D : 4.0D) * HORIZONTAL_SCALE;
        return new Vec3(Math.sin(time * 0.011D + layer * 2.8D) * bend, 0,
                Math.cos(time * 0.008D - layer * 1.9D) * bend * 0.7D);
    }

    private static double fraction(double value) { return value - Math.floor(value); }
}
