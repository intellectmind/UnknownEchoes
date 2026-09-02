package cn.kurt6.unknown_echoes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_ECHO_PARTICLES = BUILDER
            .comment("Show echo particles (double jump, beacon activation)")
            .define("enable_echo_particles", true);

    public static final ModConfigSpec.BooleanValue ENABLE_BOSS_PARTICLES = BUILDER
            .comment("Show optional boss telegraph and energy particles")
            .define("enable_boss_particles", true);

    public static final ModConfigSpec.DoubleValue BOSS_PARTICLE_DENSITY = BUILDER
            .comment("Client-side density for boss energy overlays and impact feedback")
            .defineInRange("boss_particle_density", 1.0D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue PLAY_ECHO_AMBIENT_SOUNDS = BUILDER
            .comment("Play echo ambient sounds")
            .define("play_echo_ambient_sounds", true);

    public static final ModConfigSpec.BooleanValue WIND_THIRD_JUMP_GLIDE = BUILDER
            .comment("Wind Echo: third jump in mid-air triggers a short glide (can be toggled in the ability panel)")
            .define("wind_third_jump_glide", true);

    /** 迁移旧版本的神器快捷键：旧版本默认未绑定，首次启动时补为 U。 */
    public static final ModConfigSpec.BooleanValue ARTIFACT_USE_KEY_MIGRATED = BUILDER
            .comment("Whether the legacy unbound artifact key has been migrated to U")
            .define("artifact_use_key_migrated", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
