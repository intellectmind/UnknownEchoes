package cn.kurt6.unknown_echoes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_RANDOM_RUINS = BUILDER
            .comment("Reserved switch for ruin generation (structure spacing is data-driven, see structure_set)")
            .define("enable_random_ruins", true);

    public static final ModConfigSpec.DoubleValue OVERWORLD_RUIN_SPAWN_MULTIPLIER = BUILDER
            .comment("Reserved: ruin spawn rate multiplier")
            .defineInRange("overworld_ruin_spawn_multiplier", 1.0, 0.0, 16.0);

    public static final ModConfigSpec.BooleanValue ENABLE_ECHO_REALM = BUILDER
            .comment("Allow players to enter the Echo Realm through resonance beacons")
            .define("enable_echo_realm", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
