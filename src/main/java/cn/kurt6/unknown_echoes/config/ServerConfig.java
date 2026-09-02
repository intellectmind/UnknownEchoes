package cn.kurt6.unknown_echoes.config;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 平衡默认值变更时 +1,加载旧配置文件会按 Migrations 强制覆盖为新默认值。 */
    private static final int CURRENT_CONFIG_VERSION = 5;

    public static final ModConfigSpec.IntValue CONFIG_VERSION = BUILDER
            .comment("Internal balance version, do not edit: outdated files get changed defaults re-applied")
            .defineInRange("config_version", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue BOSS_DYNAMIC_SCALING = BUILDER
            .comment("Scale boss max health by participating players (boss-specific increment)")
            .define("boss_dynamic_scaling", true);

    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Global max health multiplier for mainline bosses (applied when a boss enters the world)")
            .defineInRange("boss_health_multiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Global attack and ability damage multiplier for mainline bosses")
            .defineInRange("boss_damage_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue BOSS_ARMOR_MULTIPLIER = BUILDER
            .comment("Global armor and armor toughness multiplier for mainline bosses")
            .defineInRange("boss_armor_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue BOSS_MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Movement and flying speed multiplier for mainline bosses")
            .defineInRange("boss_movement_speed_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue BOSS_FOLLOW_RANGE_MULTIPLIER = BUILDER
            .comment("Target acquisition range multiplier for mainline bosses")
            .defineInRange("boss_follow_range_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_BOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Independent max health multiplier for bosses in the Deep Echoes")
            .defineInRange("deep_boss_health_multiplier", 1.15D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_BOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Independent attack and ability damage multiplier for Deep Echoes bosses")
            .defineInRange("deep_boss_damage_multiplier", 1.10D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_BOSS_ARMOR_MULTIPLIER = BUILDER
            .comment("Independent armor multiplier for Deep Echoes bosses")
            .defineInRange("deep_boss_armor_multiplier", 1.10D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_BOSS_MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Independent movement speed multiplier for Deep Echoes bosses")
            .defineInRange("deep_boss_movement_speed_multiplier", 1.05D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_BOSS_FOLLOW_RANGE_MULTIPLIER = BUILDER
            .comment("Independent target range multiplier for Deep Echoes bosses")
            .defineInRange("deep_boss_follow_range_multiplier", 1.05D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue MINIBOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Global max health multiplier for mini bosses")
            .defineInRange("miniboss_health_multiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue MINIBOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Global attack and ability damage multiplier for mini bosses")
            .defineInRange("miniboss_damage_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue MINIBOSS_ARMOR_MULTIPLIER = BUILDER
            .comment("Global armor and armor toughness multiplier for mini bosses")
            .defineInRange("miniboss_armor_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue MINIBOSS_MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Movement and flying speed multiplier for mini bosses")
            .defineInRange("miniboss_movement_speed_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue MINIBOSS_FOLLOW_RANGE_MULTIPLIER = BUILDER
            .comment("Target acquisition range multiplier for mini bosses")
            .defineInRange("miniboss_follow_range_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_MINIBOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Independent max health multiplier for mini bosses in the Deep Echoes")
            .defineInRange("deep_miniboss_health_multiplier", 1.10D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_MINIBOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Independent attack and ability damage multiplier for Deep Echoes mini bosses")
            .defineInRange("deep_miniboss_damage_multiplier", 1.08D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_MINIBOSS_ARMOR_MULTIPLIER = BUILDER
            .comment("Independent armor multiplier for Deep Echoes mini bosses")
            .defineInRange("deep_miniboss_armor_multiplier", 1.08D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_MINIBOSS_MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Independent movement speed multiplier for Deep Echoes mini bosses")
            .defineInRange("deep_miniboss_movement_speed_multiplier", 1.04D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue DEEP_MINIBOSS_FOLLOW_RANGE_MULTIPLIER = BUILDER
            .comment("Independent target range multiplier for Deep Echoes mini bosses")
            .defineInRange("deep_miniboss_follow_range_multiplier", 1.04D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue WORLD_ECHO_HEALTH_MULTIPLIER = BUILDER
            .comment("Max health multiplier for the World Echo final boss")
            .defineInRange("world_echo_health_multiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue WORLD_ECHO_DAMAGE_MULTIPLIER = BUILDER
            .comment("Attack and ability damage multiplier for the World Echo final boss")
            .defineInRange("world_echo_damage_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue WORLD_ECHO_ARMOR_MULTIPLIER = BUILDER
            .comment("Armor and armor toughness multiplier for the World Echo final boss")
            .defineInRange("world_echo_armor_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue WORLD_ECHO_MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Movement and flying speed multiplier for the World Echo final boss")
            .defineInRange("world_echo_movement_speed_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue WORLD_ECHO_FOLLOW_RANGE_MULTIPLIER = BUILDER
            .comment("Target acquisition range multiplier for the World Echo final boss")
            .defineInRange("world_echo_follow_range_multiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue BOSS_UNBROKEN_DAMAGE_MULTIPLIER = BUILDER
            .comment("Damage multiplier applied to the Forgotten Colossus while its guard is unbroken")
            .defineInRange("boss_unbroken_damage_multiplier", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BOSS_UNBROKEN_MAX_DAMAGE = BUILDER
            .comment("Hard cap of damage per hit while the boss guard is unbroken (anti one-shot)")
            .defineInRange("boss_unbroken_max_damage", 4.0, 0.0, 1024.0);

    public static final ModConfigSpec.IntValue BOSS_BROKEN_DURATION_TICKS = BUILDER
            .comment("How long (ticks) the boss stays vulnerable after all memory pillars are activated")
            .defineInRange("boss_broken_duration_ticks", 400, 20, 24000);

    public static final ModConfigSpec.IntValue BOSS_REWARD_RADIUS = BUILDER
            .comment("Players within this radius of the boss when it dies receive the Wind Echo")
            .defineInRange("boss_reward_radius", 32, 4, 128);

    public static final ModConfigSpec.IntValue BOSS_RECHALLENGE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks before the five ability-boss altars allow another challenge (default 5 min)")
            .defineInRange("boss_rechallenge_cooldown_ticks", 6000, 0, 1728000);

    public static final ModConfigSpec.IntValue WORLD_ECHO_RECHALLENGE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks before a World Resonance Altar allows another final-boss challenge (default 20 min)")
            .defineInRange("world_echo_rechallenge_cooldown_ticks", 24000, 0, 1728000);

    public static final ModConfigSpec.BooleanValue PERSONAL_KEY_REWARDS = BUILDER
            .comment("Key rewards (abilities) are written to player data instead of dropping as items")
            .define("personal_key_rewards", true);

    public static final ModConfigSpec.BooleanValue ABILITY_PERMISSION_REQUIRED = BUILDER
            .comment("Echo mechanisms (wind door, etc.) require the matching echo ability")
            .define("ability_permission_required", true);

    public static final ModConfigSpec.BooleanValue PROTECT_CRITICAL_BLOCKS = BUILDER
            .comment("Prevent survival players from breaking critical mechanism blocks")
            .define("protect_critical_blocks", true);

    public static final ModConfigSpec.IntValue SILENT_WALKER_SLOWNESS_TICKS = BUILDER
            .comment("Slowness duration (ticks) applied by Silent Walker attacks; 0 disables the effect")
            .defineInRange("silent_walker_slowness_ticks", 60, 0, 1200);

    public static final ModConfigSpec.IntValue CRYSTAL_FEATHER_FEED_COOLDOWN = BUILDER
            .comment("Cooldown ticks before a Crystal Feather Bird can be fed an Echo Flower for another feather")
            .defineInRange("crystal_feather_feed_cooldown", 4800, 200, 72000);

    /** 苔背龟剪取噤声苔的冷却(tick);剪取不伤害生物,冷却中只给含蓄提示。 */
    public static final ModConfigSpec.IntValue MOSS_TURTLE_SHEAR_COOLDOWN = BUILDER
            .comment("Cooldown ticks before a Moss-back Turtle's shell moss regrows and can be sheared again")
            .defineInRange("moss_turtle_shear_cooldown", 6000, 200, 72000);

    /** 回声鹿受击留下回响轨迹(指向最近遗迹的弱线索)的冷却(tick);0 关闭轨迹表现。 */
    public static final ModConfigSpec.IntValue ECHO_DEER_TRAIL_COOLDOWN = BUILDER
            .comment("Cooldown ticks between Echo Deer leaving an echo trail hint when hurt; 0 disables the trail")
            .defineInRange("echo_deer_trail_cooldown", 600, 0, 72000);

    public static final ModConfigSpec.BooleanValue MINIBOSS_ARENA_REOPEN = BUILDER
            .comment("Mini boss arenas reopen after defeat (repeat kills only grant common rewards)")
            .define("miniboss_arena_reopen", true);

    public static final ModConfigSpec.IntValue MINIBOSS_ARENA_REOPEN_TICKS = BUILDER
            .comment("Ticks before a defeated mini boss arena reopens (default 15 min)")
            .defineInRange("miniboss_arena_reopen_ticks", 18000, 200, 1728000);

    public static final ModConfigSpec.IntValue MINIBOSS_OUT_OF_COMBAT_DELAY_TICKS = BUILDER
            .comment("Ticks without a target before a boss starts regenerating (all bosses)")
            .defineInRange("miniboss_out_of_combat_delay_ticks", 100, 0, 2400);

    public static final ModConfigSpec.DoubleValue MINIBOSS_OUT_OF_COMBAT_REGEN_PERCENT = BUILDER
            .comment("Fraction of max health regenerated every second after the out-of-combat delay",
                    "Applies to mainline bosses and mini bosses alike; the miniboss_ key name is",
                    "kept so existing server config files keep working.")
            .defineInRange("miniboss_out_of_combat_regen_percent", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MINIBOSS_GUIDANCE_INTERVAL_TICKS = BUILDER
            .comment("Ticks between mini boss mechanic guidance updates while a guard is active")
            .defineInRange("miniboss_guidance_interval_ticks", 5, 1, 100);

    public static final ModConfigSpec.IntValue MINIBOSS_SETTLEMENT_RADIUS_BONUS = BUILDER
            .comment("Extra blocks beyond a mini boss arena radius used for participant settlement")
            .defineInRange("miniboss_settlement_radius_bonus", 12, 0, 128);

    public static final ModConfigSpec.IntValue ENERGY_MAX = BUILDER
            .comment("Echo energy pool cap per player")
            .defineInRange("energy_max", 100, 1, 10000);

    public static final ModConfigSpec.IntValue ENERGY_REGEN_PER_SECOND = BUILDER
            .comment("Base Echo energy natural regeneration per second before multipliers")
            .defineInRange("energy_regen_per_second", 1, 0, 100);

    public static final ModConfigSpec.IntValue ENERGY_REALM_REGEN_MULTIPLIER = BUILDER
            .comment("Energy regeneration multiplier while inside the Echo Realm")
            .defineInRange("energy_realm_regen_multiplier", 2, 1, 10);

    public static final ModConfigSpec.DoubleValue ENERGY_NATURAL_REGEN_MULTIPLIER = BUILDER
            .comment("Global balance multiplier applied to natural Echo energy regeneration")
            .defineInRange("energy_natural_regen_multiplier", 0.75D, 0.0D, 10.0D);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_PAGE = BUILDER
            .comment("One-time energy refill when reading a new ancient page")
            .defineInRange("energy_refill_page", 20, 0, 1000);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_PUZZLE = BUILDER
            .comment("One-time energy refill when solving a puzzle")
            .defineInRange("energy_refill_puzzle", 40, 0, 1000);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_STRUCTURE = BUILDER
            .comment("One-time energy refill when discovering a new structure")
            .defineInRange("energy_refill_structure", 20, 0, 1000);

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DISABLED_ARTIFACTS = BUILDER
            .comment("Artifact ids disabled on this server (e.g. [\"storm_compass\"])")
            .defineListAllowEmpty("disabled_artifacts", java.util.List.of(),
                    () -> "", o -> o instanceof String);

    public static final ModConfigSpec.DoubleValue ARTIFACT_COOLDOWN_MULTIPLIER = BUILDER
            .comment("Global multiplier applied to all artifact cooldowns")
            .defineInRange("artifact_cooldown_multiplier", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue ARTIFACT_ENERGY_COST_MULTIPLIER = BUILDER
            .comment("Global multiplier applied to all artifact energy costs")
            .defineInRange("artifact_energy_cost_multiplier", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.IntValue STORM_COMPASS_COOLDOWN_SECONDS = BUILDER
            .comment("Storm Compass base cooldown in seconds; higher levels reduce it by 15s each")
            .defineInRange("storm_compass_cooldown_seconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue STORM_COMPASS_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Storm Compass flight")
            .defineInRange("storm_compass_energy_cost", 24, 0, 1000);

    public static final ModConfigSpec.IntValue STORM_COMPASS_FLIGHT_SECONDS = BUILDER
            .comment("Storm Compass level 1 flight seconds; higher levels add 4s each")
            .defineInRange("storm_compass_flight_seconds", 12, 1, 600);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_COOLDOWN_SECONDS = BUILDER
            .comment("Tide Lantern active cooldown in seconds")
            .defineInRange("tide_lantern_cooldown_seconds", 45, 0, 3600);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Tide Lantern renewal")
            .defineInRange("tide_lantern_energy_cost", 18, 0, 1000);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_MAX_HEALTH_BONUS = BUILDER
            .comment("Maximum extra health granted by Tide Lantern")
            .defineInRange("tide_lantern_max_health_bonus", 8, 0, 40);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_AUTO_COOLDOWN_SECONDS = BUILDER
            .comment("Low-health Tide Lantern auto-guard cooldown in seconds")
            .defineInRange("tide_lantern_auto_cooldown_seconds", 120, 10, 3600);

    public static final ModConfigSpec.IntValue ECHO_LENS_COOLDOWN_SECONDS = BUILDER
            .comment("Echo Lens active cooldown in seconds")
            .defineInRange("echo_lens_cooldown_seconds", 30, 0, 3600);

    public static final ModConfigSpec.IntValue ECHO_LENS_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Echo Lens marking")
            .defineInRange("echo_lens_energy_cost", 18, 0, 1000);

    public static final ModConfigSpec.DoubleValue ECHO_LENS_DAMAGE_BONUS = BUILDER
            .comment("Echo Lens marked-target damage bonus")
            .defineInRange("echo_lens_damage_bonus", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue ECHO_LENS_DAMAGE_CAP = BUILDER
            .comment("Echo Lens bonus damage cap per hit")
            .defineInRange("echo_lens_damage_cap", 8.0, 0.0, 1024.0);

    public static final ModConfigSpec.DoubleValue ECHO_LENS_RANGE_BLOCKS = BUILDER
            .comment("Forward targeting range in blocks for Echo Lens marks and focus")
            .defineInRange("echo_lens_range_blocks", 24.0D, 4.0D, 96.0D);

    public static final ModConfigSpec.IntValue ECHO_LENS_MARK_DURATION_TICKS = BUILDER
            .comment("Base duration in ticks of Echo Lens glowing marks before armor and tuning bonuses")
            .defineInRange("echo_lens_mark_duration_ticks", 200, 20, 1200);

    public static final ModConfigSpec.IntValue TIME_HOURGLASS_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown of Time Hourglass entity freeze")
            .defineInRange("time_hourglass_cooldown_seconds", 40, 0, 3600);

    public static final ModConfigSpec.IntValue TIME_HOURGLASS_ENERGY_COST = BUILDER
            .comment("Echo energy cost of Time Hourglass entity freeze")
            .defineInRange("time_hourglass_energy_cost", 32, 0, 1000);

    public static final ModConfigSpec.IntValue TIME_HOURGLASS_FREEZE_TICKS = BUILDER
            .comment("Base duration of Time Hourglass entity freeze in ticks")
            .defineInRange("time_hourglass_freeze_ticks", 100, 20, 1200);

    public static final ModConfigSpec.IntValue TIME_HOURGLASS_RADIUS_BASE_BLOCKS = BUILDER
            .comment("Base horizontal radius in blocks of Time Hourglass before artifact level bonuses")
            .defineInRange("time_hourglass_radius_base_blocks", 8, 1, 64);

    public static final ModConfigSpec.IntValue TIME_HOURGLASS_RADIUS_PER_LEVEL_BLOCKS = BUILDER
            .comment("Additional Time Hourglass radius in blocks granted by each artifact level")
            .defineInRange("time_hourglass_radius_per_level_blocks", 2, 0, 32);

    public static final ModConfigSpec.DoubleValue TIME_HOURGLASS_BOSS_DURATION_MULTIPLIER = BUILDER
            .comment("Duration multiplier for Time Hourglass against bosses with the folded-memory tuning")
            .defineInRange("time_hourglass_boss_duration_multiplier", 1.25D, 0.0D, 4.0D);

    public static final ModConfigSpec.IntValue DIMENSION_ANCHOR_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown of Dimension Anchor return")
            .defineInRange("dimension_anchor_cooldown_seconds", 90, 0, 3600);

    public static final ModConfigSpec.IntValue DIMENSION_ANCHOR_ENERGY_COST = BUILDER
            .comment("Echo energy cost of Dimension Anchor return")
            .defineInRange("dimension_anchor_energy_cost", 36, 0, 1000);

    public static final ModConfigSpec.IntValue RIFT_ANCHOR_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown of Rift Anchor return")
            .defineInRange("rift_anchor_cooldown_seconds", 120, 0, 3600);

    public static final ModConfigSpec.IntValue RIFT_ANCHOR_ENERGY_COST = BUILDER
            .comment("Echo energy cost of Rift Anchor return")
            .defineInRange("rift_anchor_energy_cost", 44, 0, 1000);

    public static final ModConfigSpec.IntValue RIFT_ANCHOR_LIFETIME_TICKS = BUILDER
            .comment("Base lifetime of a bound Rift Anchor point")
            .defineInRange("rift_anchor_lifetime_ticks", 12000, 200, 1728000);

    public static final ModConfigSpec.BooleanValue ARTIFACT_PVP_EFFECTS = BUILDER
            .comment("Allow artifact damage marks and PvP effects against players")
            .define("artifact_pvp_effects", false);

    public static final ModConfigSpec.BooleanValue WORLD_EVENTS_ENABLED = BUILDER
            .comment("Master switch for all ambient world events across the overworld, Echo Realm, and Deep Echoes")
            .define("world_events_enabled", true);

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DISABLED_WORLD_EVENTS = BUILDER
            .comment("World event ids disabled on this server (e.g. [\"echo_fog\"])")
            .defineListAllowEmpty("disabled_world_events", java.util.List.of(),
                    () -> "", o -> o instanceof String);

    public static final ModConfigSpec.IntValue WORLD_EVENT_CHECK_INTERVAL_TICKS = BUILDER
            .comment("Ticks between world event trigger rolls per dimension (default 60s)")
            .defineInRange("world_event_check_interval_ticks", 1200, 200, 1728000);

    public static final ModConfigSpec.DoubleValue WORLD_EVENT_CHANCE = BUILDER
            .comment("Chance per roll that an eligible world event starts near a random player")
            .defineInRange("world_event_chance", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.IntValue WORLD_EVENT_MAX_ACTIVE = BUILDER
            .comment("Maximum simultaneously active world events per dimension")
            .defineInRange("world_event_max_active", 2, 1, 16);

    public static final ModConfigSpec.IntValue WORLD_EVENT_DURATION_TICKS = BUILDER
            .comment("Default world event duration in ticks (default 3 min)")
            .defineInRange("world_event_duration_ticks", 3600, 200, 1728000);

    public static final ModConfigSpec.IntValue WORLD_EVENT_EFFECT_RADIUS = BUILDER
            .comment("Radius in blocks used for world event players, effects, and local interactions")
            .defineInRange("world_event_effect_radius", 32, 4, 128);

    public static final ModConfigSpec.IntValue WORLD_EVENT_MIN_SPACING = BUILDER
            .comment("Minimum horizontal spacing in blocks between active world events")
            .defineInRange("world_event_min_spacing", 96, 0, 512);

    public static final ModConfigSpec.IntValue WORLD_EVENT_PLACEMENT_ATTEMPTS = BUILDER
            .comment("Maximum safe-location attempts made before cancelling one world event roll")
            .defineInRange("world_event_placement_attempts", 8, 1, 32);

    public static final ModConfigSpec.BooleanValue LOST_TRAVELER_CAMP_EVENT = BUILDER
            .comment("Lost traveler caravan may appear near players as a low-frequency world event")
            .define("lost_traveler_camp_event", true);

    public static final ModConfigSpec.IntValue LOST_TRAVELER_STAY_TICKS = BUILDER
            .comment("Ticks a lost traveler stays before leaving (the event places no blocks)")
            .defineInRange("lost_traveler_stay_ticks", 36000, 1200, 1728000);

    public static final ModConfigSpec.IntValue WORLD_EVENT_NIGHT_VISION_DURATION_TICKS = BUILDER
            .comment("Client-visible night vision duration refreshed by eligible world events",
                    "Keep above 600 so the remaining duration never enters vanilla's flicker fade")
            .defineInRange("world_event_night_vision_duration_ticks", 600, 20, 2400);

    public static final ModConfigSpec.IntValue WORLD_EVENT_NIGHT_VISION_REFRESH_THRESHOLD_TICKS = BUILDER
            .comment("Refresh night vision when its remaining duration falls below this many ticks",
                    "Vanilla starts its flickering fade at 200 ticks; keep this above that")
            .defineInRange("world_event_night_vision_refresh_threshold_ticks", 300, 0, 2400);

    public static final ModConfigSpec.IntValue WIND_GLIDE_ENERGY_COST = BUILDER
            .comment("Echo energy consumed when starting a wind glide (0 disables the cost)")
            .defineInRange("wind_glide_energy_cost", 8, 0, 1000);

    public static final ModConfigSpec.IntValue WIND_GLIDE_LANDING_COOLDOWN_TICKS = BUILDER
            .comment("Ticks after landing from a glide before the next glide may start")
            .defineInRange("wind_glide_landing_cooldown_ticks", 60, 0, 1200);

    public static final ModConfigSpec.IntValue GRAVITY_ACTIVE_ENERGY_COST = BUILDER
            .comment("Echo energy consumed by active Gravity Echo attunement (0 by default)")
            .defineInRange("gravity_active_energy_cost", 0, 0, 1000);

    public static final ModConfigSpec.IntValue GRAVITY_ACTIVE_DURATION_TICKS = BUILDER
            .comment("Duration of active Gravity Echo attunement in ticks")
            .defineInRange("gravity_active_duration_ticks", 100, 20, 1200);

    public static final ModConfigSpec.IntValue GRAVITY_ACTIVE_COOLDOWN_TICKS = BUILDER
            .comment("Base cooldown of active Gravity Echo attunement in ticks (100s; 40s at research 4)")
            .defineInRange("gravity_active_cooldown_ticks", 2000, 0, 72000);

    public static final ModConfigSpec.DoubleValue GRAVITY_ACTIVE_RADIUS = BUILDER
            .comment("Base radius of the Gravity Echo suppression field")
            .defineInRange("gravity_active_radius", 5.0D, 1.0D, 32.0D);

    public static final ModConfigSpec.IntValue VOID_STEP_ENERGY_COST = BUILDER
            .comment("Echo energy consumed by Void Echo short step (0 by default)")
            .defineInRange("void_step_energy_cost", 0, 0, 1000);

    public static final ModConfigSpec.DoubleValue VOID_STEP_DISTANCE = BUILDER
            .comment("Maximum collision-checked distance of Void Echo short step")
            .defineInRange("void_step_distance", 6.0D, 1.0D, 12.0D);

    public static final ModConfigSpec.IntValue VOID_STEP_COOLDOWN_TICKS = BUILDER
            .comment("Base cooldown of Void Echo short step in ticks (100s; 40s at research 4)")
            .defineInRange("void_step_cooldown_ticks", 2000, 0, 72000);

    public static final ModConfigSpec.IntValue VOID_WARD_DURATION_TICKS = BUILDER
            .comment("Void-corrosion cleansing window after a successful short step")
            .defineInRange("void_ward_duration_ticks", 60, 20, 1200);

    public static final ModConfigSpec.IntValue WIND_PLATFORM_ACTIVE_TICKS = BUILDER
            .comment("Ticks a wind current platform keeps its updraft column after activation")
            .defineInRange("wind_platform_active_ticks", 600, 40, 24000);

    public static final ModConfigSpec.IntValue WIND_PLATFORM_LIFT_HEIGHT = BUILDER
            .comment("Height (blocks) of the updraft column above an active wind current platform")
            .defineInRange("wind_platform_lift_height", 20, 2, 64);

    public static final ModConfigSpec.IntValue TIDE_DOOR_HOLD_TICKS = BUILDER
            .comment("Ticks a tide sensor door stays open (reflection entry stability time)")
            .defineInRange("tide_door_hold_ticks", 200, 20, 24000);

    public static final ModConfigSpec.IntValue TIDE_DOOR_HOLD_TICKS_RESEARCH3 = BUILDER
            .comment("Ticks a tide sensor door stays open for players with tide research level 3+")
            .defineInRange("tide_door_hold_ticks_research3", 400, 20, 24000);

    public static final ModConfigSpec.DoubleValue TIDE_BOLT_WATER_INERTIA = BUILDER
            .comment("Water inertia of Tide Bolts; 0.6 matches vanilla arrows and 1.0 removes almost all drag")
            .defineInRange("tide_bolt_water_inertia", 0.99D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue TIDE_BOLT_MARK_COOLDOWN_TICKS = BUILDER
            .comment("Per-player cooldown before a Tide Bolt can show another nearby tide rune guidance trail")
            .defineInRange("tide_bolt_mark_cooldown_ticks", 240, 0, 72000);

    public static final ModConfigSpec.IntValue TIDE_BOLT_RUNE_SEARCH_RADIUS = BUILDER
            .comment("Horizontal search radius in blocks for a Tide Bolt's nearby tide rune guidance")
            .defineInRange("tide_bolt_rune_search_radius", 16, 1, 128);

    public static final ModConfigSpec.DoubleValue TIDE_BOLT_BURST_RADIUS = BUILDER
            .comment("Radius in blocks of the Tide Bolt impact burst")
            .defineInRange("tide_bolt_burst_radius", 2.5D, 0.0D, 16.0D);

    public static final ModConfigSpec.DoubleValue TIDE_BOLT_BURST_DAMAGE_FACTOR = BUILDER
            .comment("Impact burst damage as a fraction of Tide Bolt base damage times impact speed")
            .defineInRange("tide_bolt_burst_damage_factor", 0.5D, 0.0D, 4.0D);

    public static final ModConfigSpec.DoubleValue TIDE_BOLT_WET_DAMAGE_MULTIPLIER = BUILDER
            .comment("Direct Tide Bolt damage multiplier against targets standing in water")
            .defineInRange("tide_bolt_wet_damage_multiplier", 1.25D, 0.0D, 4.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** 迁移旧配置中的平衡值；迁移只执行一次，之后服务器仍可自行调整配置。 */
    @EventBusSubscriber(modid = UnknownEchoes.MODID)
    public static final class Migrations {

        @SubscribeEvent
        public static void onLoading(ModConfigEvent.Loading event) {
            migrate(event.getConfig());
        }

        @SubscribeEvent
        public static void onReloading(ModConfigEvent.Reloading event) {
            migrate(event.getConfig());
        }

        private static void migrate(ModConfig config) {
            if (config.getSpec() != SPEC || CONFIG_VERSION.get() >= CURRENT_CONFIG_VERSION) {
                return;
            }
            if (CONFIG_VERSION.get() < 1) {

                ENERGY_NATURAL_REGEN_MULTIPLIER.set(0.1D);
                GRAVITY_ACTIVE_COOLDOWN_TICKS.set(2000);
                VOID_STEP_COOLDOWN_TICKS.set(2000);
            }
            if (CONFIG_VERSION.get() < 2) {

                ENERGY_NATURAL_REGEN_MULTIPLIER.set(0.5D);
                STORM_COMPASS_COOLDOWN_SECONDS.set(60);
                STORM_COMPASS_ENERGY_COST.set(24);
                STORM_COMPASS_FLIGHT_SECONDS.set(12);
                TIDE_LANTERN_COOLDOWN_SECONDS.set(45);
                TIDE_LANTERN_ENERGY_COST.set(18);
                TIDE_LANTERN_AUTO_COOLDOWN_SECONDS.set(120);
                ECHO_LENS_COOLDOWN_SECONDS.set(30);
                ECHO_LENS_ENERGY_COST.set(18);
                ECHO_LENS_DAMAGE_BONUS.set(0.25D);
                ECHO_LENS_DAMAGE_CAP.set(8.0D);
                TIME_HOURGLASS_COOLDOWN_SECONDS.set(40);
                TIME_HOURGLASS_ENERGY_COST.set(32);
                DIMENSION_ANCHOR_COOLDOWN_SECONDS.set(90);
                DIMENSION_ANCHOR_ENERGY_COST.set(36);
                RIFT_ANCHOR_COOLDOWN_SECONDS.set(120);
                RIFT_ANCHOR_ENERGY_COST.set(44);
            }
            if (CONFIG_VERSION.get() < 3) {

                ENERGY_NATURAL_REGEN_MULTIPLIER.set(0.75D);
            }
            if (CONFIG_VERSION.get() < 4) {

                WORLD_EVENT_NIGHT_VISION_DURATION_TICKS.set(600);
                WORLD_EVENT_NIGHT_VISION_REFRESH_THRESHOLD_TICKS.set(300);
            }
            if (CONFIG_VERSION.get() < 5) {
                DEEP_BOSS_HEALTH_MULTIPLIER.set(1.15D);
                DEEP_BOSS_DAMAGE_MULTIPLIER.set(1.10D);
                DEEP_BOSS_ARMOR_MULTIPLIER.set(1.10D);
                DEEP_BOSS_MOVEMENT_SPEED_MULTIPLIER.set(1.05D);
                DEEP_BOSS_FOLLOW_RANGE_MULTIPLIER.set(1.05D);
                DEEP_MINIBOSS_HEALTH_MULTIPLIER.set(1.10D);
                DEEP_MINIBOSS_DAMAGE_MULTIPLIER.set(1.08D);
                DEEP_MINIBOSS_ARMOR_MULTIPLIER.set(1.08D);
                DEEP_MINIBOSS_MOVEMENT_SPEED_MULTIPLIER.set(1.04D);
                DEEP_MINIBOSS_FOLLOW_RANGE_MULTIPLIER.set(1.04D);
            }
            CONFIG_VERSION.set(CURRENT_CONFIG_VERSION);
            SPEC.save();
            UnknownEchoes.LOGGER.info("Unknown Echoes server config migrated to balance version {}",
                    CURRENT_CONFIG_VERSION);
        }
    }
}
