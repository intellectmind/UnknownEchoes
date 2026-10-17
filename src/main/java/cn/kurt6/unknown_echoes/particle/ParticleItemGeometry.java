package cn.kurt6.unknown_echoes.particle;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public final class ParticleItemGeometry {
    public static final int MAX_EDGES = 192;
    public record Edge(Vec3 from, Vec3 to) { }
    public record Profile(Vec3 center, List<Edge> edges) { }
    private ParticleItemGeometry() { }

    // generated/handheld 的模型坐标为 0–1；PNG 的 y 轴朝下，模型的 y 轴朝上。
    public static Profile profile(int width, int height, BiPredicate<Integer, Integer> opaque) {
        if (width < 1 || height < 1 || width > 64 || height > 64) return new Profile(Vec3.ZERO, List.of());
        Vec3 grip = new Vec3(.18, .18, .5), tip = grip;
        boolean[][] pixels = new boolean[height][width];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            pixels[y][x] = opaque.test(x, y);
            Vec3 point = pixel(x + .5, y + .5, width, height);
            if (pixels[y][x] && point.distanceToSqr(grip) > tip.distanceToSqr(grip)) tip = point;
        }
        double length = tip.distanceTo(grip);
        Vec3 axis = tip.subtract(grip).normalize(), target = grip.add(axis.scale(length * .65));
        Vec3 center = target;
        double nearest = Double.MAX_VALUE;
        List<Edge> edges = new ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (!pixels[y][x]) continue;
            Vec3 point = pixel(x + .5, y + .5, width, height);
            if (point.subtract(grip).dot(axis) < length * .32) continue;
            if (point.distanceToSqr(target) < nearest) { nearest = point.distanceToSqr(target); center = point; }
            if (y == 0 || !pixels[y - 1][x]) edges.add(edge(x, y, x + 1, y, width, height));
            if (y == height - 1 || !pixels[y + 1][x]) edges.add(edge(x + 1, y + 1, x, y + 1, width, height));
            if (x == 0 || !pixels[y][x - 1]) edges.add(edge(x, y + 1, x, y, width, height));
            if (x == width - 1 || !pixels[y][x + 1]) edges.add(edge(x + 1, y, x + 1, y + 1, width, height));
        }
        if (edges.size() > MAX_EDGES) {
            List<Edge> bounded = new ArrayList<>(MAX_EDGES);
            for (int i = 0; i < MAX_EDGES; i++) bounded.add(edges.get(i * edges.size() / MAX_EDGES));
            edges = bounded;
        }
        return new Profile(center, List.copyOf(edges));
    }

    public static float opacity(double tick) {
        return (float) (.36 + .035 * Math.sin(tick * .035));
    }

    public static Vec3 ring(Profile profile, double angle, boolean full) {
        double radius = full ? .075 : .05;
        return profile.center.add(Math.cos(angle) * radius, Math.sin(angle) * radius, 0);
    }

    public static float flow(Edge edge, double tick) {
        Vec3 center = edge.from.add(edge.to).scale(.5);
        return (float) Math.pow(.5 + .5 * Math.sin((center.x + center.y) * 12 - tick * .22), 6);
    }

    public record Glint(Vec3 center, Vec3 axis, float alpha) { }

    public static Glint glint(Profile profile, int index, double tick) {
        if (profile.edges.isEmpty()) return new Glint(profile.center, Vec3.ZERO, 0);
        double clock = tick / 18 + index * .61803398875;
        long cycle = (long) Math.floor(clock);
        double phase = clock - cycle;
        int slot = (int) Math.floorMod(cycle * 31 + index * 47, profile.edges.size());
        Edge edge = profile.edges.get(slot);
        Vec3 axis = edge.to.subtract(edge.from).normalize();
        return new Glint(edge.from.lerp(edge.to, phase), axis, (float) Math.pow(Math.sin(Math.PI * phase), 2));
    }

    private static Edge edge(int x0, int y0, int x1, int y1, int width, int height) {
        return new Edge(pixel(x0, y0, width, height), pixel(x1, y1, width, height));
    }

    private static Vec3 pixel(double x, double y, int width, int height) {
        return new Vec3(x / width, 1 - y / height, .5);
    }
}
