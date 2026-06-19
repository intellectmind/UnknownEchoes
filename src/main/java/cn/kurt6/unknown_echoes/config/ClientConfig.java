package cn.kurt6.unknown_echoes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_ABILITY_UNLOCK_MESSAGE = BUILDER
            .comment("Show a chat message when an echo ability is unlocked")
            .define("show_ability_unlock_message", true);

    public static final ModConfigSpec.BooleanValue ENABLE_ECHO_PARTICLES = BUILDER
            .comment("Show echo particles (double jump, beacon activation)")
            .define("enable_echo_particles", true);

    public static final ModConfigSpec.BooleanValue PLAY_ECHO_AMBIENT_SOUNDS = BUILDER
            .comment("Play echo ambient sounds")
            .define("play_echo_ambient_sounds", true);

    public static final ModConfigSpec.BooleanValue WIND_THIRD_JUMP_GLIDE = BUILDER
            .comment("Wind Echo: third jump in mid-air triggers a short glide (can be toggled in the ability panel)")
            .define("wind_third_jump_glide", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
