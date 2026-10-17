package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public final class CombatFx {
    private CombatFx() { }

    public static int color(Vector3f v) {
        return ((int) (v.x * 255) << 16) | ((int) (v.y * 255) << 8) | (int) (v.z * 255);
    }

    public static void emit(ServerLevel level, Shape shape, Vec3 origin, Vec3 direction,
                            double radius, double length, double span, int core, int edge,
                            int duration, boolean boss) {
        CombatFxPayload payload = new CombatFxPayload(shape, origin, direction, (float) radius,
                (float) length, (float) span, core, edge, duration, boss, -1);
        if (!payload.valid()) {
            return;
        }
        // 一个形状一个包；接收范围包含整条光束，站在远端的玩家也能看到。
        double extent = Math.max(radius, length);
        PacketDistributor.sendToPlayersNear(level, null, origin.x, origin.y, origin.z,
                48 + extent, payload);
    }

    public static void themed(ServerLevel level, Shape shape, Vec3 origin, Vec3 direction,
                              double radius, double length, double span,
                              Vector3f core, Vector3f edge, int duration) {
        emit(level, shape, origin, direction, radius, length, span,
                color(core), color(edge), duration, false);
    }

    public static void boss(ServerLevel level, Shape shape, Vec3 origin, Vec3 direction,
                            double radius, double length, double span, ParticleOptions theme, int duration) {
        int core = particleColor(theme);
        int edge = ((core & 0xFEFEFE) >> 1);
        emit(level, shape, origin, direction, radius, length, span, core, edge, duration, true);
    }

    public static void debris(ServerLevel level, Vec3 origin, double radius, double strength) {
        emit(level, Shape.DEBRIS, origin, Vec3.ZERO, Math.min(10, radius), strength, 0,
                0, 0, 30, true);
    }

    public static void spatial(ServerLevel level, Vec3 center, double radius, double height,
                               ParticleOptions theme, boolean arrival, int duration) {
        int core = particleColor(theme);
        emit(level, arrival ? Shape.ARRIVAL : Shape.COLLAPSE, center, Vec3.ZERO,
                radius, height, 0, core, (core & 0xFEFEFE) >> 1, duration, true);
    }

    public static void teleport(ServerLevel level, Vec3 from, Vec3 to, double radius,
                                double height, ParticleOptions theme) {
        spatial(level, from.add(0, height * 0.5, 0), radius, height, theme, false, 10);
        spatial(level, to.add(0, height * 0.5, 0), radius, height, theme, true, 16);
    }

    static int particleColor(ParticleOptions particle) {
        if (particle instanceof DustParticleOptions dust) {
            return color(dust.getColor());
        }
        if (particle instanceof DustColorTransitionOptions dust) {
            return color(dust.getFromColor());
        }
        return switch (BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()).getPath()) {
            case "campfire_cosy_smoke", "crit", "ash", "smoke", "poof" -> 0xE8B975;
            case "white_ash", "end_rod", "snowflake", "enchant" -> 0xDAF6FF;
            case "splash", "bubble_pop", "squid_ink", "bubble_column_up", "nautilus", "underwater" -> 0x40DCC9;
            case "reverse_portal", "dragon_breath", "portal", "witch" -> 0xD574FA;
            case "sculk_soul", "sonic_boom", "sculk_charge_pop", "soul_fire_flame" -> 0x468FF5;
            case "flame", "small_flame", "lava" -> 0xFF9A42;
            case "cherry_leaves", "spore_blossom_air", "heart" -> 0xF29BCD;
            case "note" -> 0xCB8DFF;
            case "gust", "cloud" -> 0xBAE9D5;
            default -> 0xA5E9FA;
        };
    }
}
