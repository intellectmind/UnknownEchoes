package cn.kurt6.unknown_echoes.fx;

import net.minecraft.world.phys.Vec3;

import java.util.SplittableRandom;

public final class TerrainDebrisPhysics {
    public static final int MAX_FRAGMENTS = 96;
    public static final int MAX_PER_BURST = 28;
    public static final double GRAVITY = 0.045;

    private TerrainDebrisPhysics() { }

    public record Launch(Vec3 offset, Vec3 velocity, Vec3 spin, float size, int delay) { }

    public static Launch launch(long seed, int index, double radius, double strength) {
        SplittableRandom random = new SplittableRandom(seed + index * 7919L);
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(random.nextDouble()) * Math.min(10, Math.max(0, radius)) * 0.8;
        double force = Math.min(1.5, Math.max(0.25, strength));
        double speed = (0.11 + random.nextDouble() * 0.15) * force;
        float size = (float) (0.18 + random.nextDouble() * 0.46);
        return new Launch(new Vec3(Math.cos(angle) * distance, 0, Math.sin(angle) * distance),
                new Vec3(Math.cos(angle) * speed, (0.26 + random.nextDouble() * 0.24) * force, Math.sin(angle) * speed),
                new Vec3(random.nextDouble(-0.18, 0.18), random.nextDouble(-0.24, 0.24), random.nextDouble(-0.18, 0.18)),
                size, Math.min(4, (int) (distance * 0.65)));
    }

    public static Vec3 afterMove(Vec3 velocity, Vec3 actualMove, int bounces) {
        boolean floor = velocity.y < 0 && Math.abs(actualMove.y - velocity.y) > 1.0E-6;
        double x = Math.abs(actualMove.x - velocity.x) > 1.0E-6 ? -velocity.x * 0.22 : velocity.x * 0.96;
        double z = Math.abs(actualMove.z - velocity.z) > 1.0E-6 ? -velocity.z * 0.22 : velocity.z * 0.96;
        double y = floor ? (bounces == 0 ? -velocity.y * 0.3 : 0) : velocity.y - GRAVITY;
        if (!floor && Math.abs(actualMove.y - velocity.y) > 1.0E-6) {
            y = -GRAVITY;
        }
        return new Vec3(floor ? x * 0.6 : x, y, floor ? z * 0.6 : z);
    }

    public static float scale(int age, float partial, int lifetime) {
        double remaining = lifetime - age - partial;
        return (float) Math.max(0, Math.min(1, remaining / 7));
    }

    public static int budget(int particleSetting, double density) {
        int base = particleSetting == 2 ? 20 : particleSetting == 1 ? 48 : MAX_FRAGMENTS;
        return (int) Math.floor(base * Math.max(0, Math.min(1, density)));
    }
}
