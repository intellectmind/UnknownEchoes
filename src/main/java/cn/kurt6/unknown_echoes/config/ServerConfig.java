package cn.kurt6.unknown_echoes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BOSS_DYNAMIC_SCALING = BUILDER
            .comment("Scale boss max health by participating players (+150 per extra player)")
            .define("boss_dynamic_scaling", true);

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

    public static final ModConfigSpec.BooleanValue PERSONAL_KEY_REWARDS = BUILDER
            .comment("Key rewards (abilities) are written to player data instead of dropping as items")
            .define("personal_key_rewards", true);

    public static final ModConfigSpec.BooleanValue ABILITY_PERMISSION_REQUIRED = BUILDER
            .comment("Echo mechanisms (wind door, etc.) require the matching echo ability")
            .define("ability_permission_required", true);

    public static final ModConfigSpec.BooleanValue PROTECT_CRITICAL_BLOCKS = BUILDER
            .comment("Prevent survival players from breaking critical mechanism blocks")
            .define("protect_critical_blocks", true);

    // ---- V0.5C 新群系内容参数 ----

    public static final ModConfigSpec.IntValue SILENT_WALKER_SLOWNESS_TICKS = BUILDER
            .comment("Slowness duration (ticks) applied by Silent Walker attacks; 0 disables the effect")
            .defineInRange("silent_walker_slowness_ticks", 60, 0, 1200);

    public static final ModConfigSpec.IntValue CRYSTAL_FEATHER_FEED_COOLDOWN = BUILDER
            .comment("Cooldown ticks before a Crystal Feather Bird can be fed an Echo Flower for another feather")
            .defineInRange("crystal_feather_feed_cooldown", 4800, 200, 72000);

    // ---- V0.6F 环境生物参数 ----

    /** 苔背龟剪取噤声苔的冷却(tick);剪取不伤害生物,冷却中只给含蓄提示。 */
    public static final ModConfigSpec.IntValue MOSS_TURTLE_SHEAR_COOLDOWN = BUILDER
            .comment("Cooldown ticks before a Moss-back Turtle's shell moss regrows and can be sheared again")
            .defineInRange("moss_turtle_shear_cooldown", 6000, 200, 72000);

    /** 回声鹿受击留下回响轨迹(指向最近遗迹的弱线索)的冷却(tick);0 关闭轨迹表现。 */
    public static final ModConfigSpec.IntValue ECHO_DEER_TRAIL_COOLDOWN = BUILDER
            .comment("Cooldown ticks between Echo Deer leaving an echo trail hint when hurt; 0 disables the trail")
            .defineInRange("echo_deer_trail_cooldown", 600, 0, 72000);

    // ---- V0.6A Mini Boss 公共框架 ----

    public static final ModConfigSpec.BooleanValue MINIBOSS_ARENA_REOPEN = BUILDER
            .comment("Mini boss arenas reopen after defeat (repeat kills only grant common rewards)")
            .define("miniboss_arena_reopen", true);

    public static final ModConfigSpec.IntValue MINIBOSS_ARENA_REOPEN_TICKS = BUILDER
            .comment("Ticks before a defeated mini boss arena reopens (default 15 min)")
            .defineInRange("miniboss_arena_reopen_ticks", 18000, 200, 1728000);

    // ---- V0.6D 回响能量(12.2:设定基线 上限 100,自然恢复 1/秒,境域内翻倍) ----

    public static final ModConfigSpec.IntValue ENERGY_MAX = BUILDER
            .comment("Echo energy pool cap per player")
            .defineInRange("energy_max", 100, 1, 10000);

    public static final ModConfigSpec.IntValue ENERGY_REGEN_PER_SECOND = BUILDER
            .comment("Echo energy natural regeneration per second")
            .defineInRange("energy_regen_per_second", 1, 0, 100);

    public static final ModConfigSpec.IntValue ENERGY_REALM_REGEN_MULTIPLIER = BUILDER
            .comment("Energy regeneration multiplier while inside the Echo Realm")
            .defineInRange("energy_realm_regen_multiplier", 2, 1, 10);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_PAGE = BUILDER
            .comment("One-time energy refill when reading a new ancient page")
            .defineInRange("energy_refill_page", 20, 0, 1000);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_PUZZLE = BUILDER
            .comment("One-time energy refill when solving a puzzle")
            .defineInRange("energy_refill_puzzle", 40, 0, 1000);

    public static final ModConfigSpec.IntValue ENERGY_REFILL_STRUCTURE = BUILDER
            .comment("One-time energy refill when discovering a new structure")
            .defineInRange("energy_refill_structure", 20, 0, 1000);

    // ---- V0.6D 神器框架(12.5:每件可单独禁用;能耗与冷却有全局倍率) ----

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
            .comment("Storm Compass base cooldown in seconds; higher levels reduce it by 20s each")
            .defineInRange("storm_compass_cooldown_seconds", 120, 0, 3600);

    public static final ModConfigSpec.IntValue STORM_COMPASS_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Storm Compass flight")
            .defineInRange("storm_compass_energy_cost", 18, 0, 1000);

    public static final ModConfigSpec.IntValue STORM_COMPASS_FLIGHT_SECONDS = BUILDER
            .comment("Storm Compass level 1 flight seconds; higher levels add 4s each")
            .defineInRange("storm_compass_flight_seconds", 10, 1, 600);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_COOLDOWN_SECONDS = BUILDER
            .comment("Tide Lantern active cooldown in seconds")
            .defineInRange("tide_lantern_cooldown_seconds", 75, 0, 3600);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Tide Lantern renewal")
            .defineInRange("tide_lantern_energy_cost", 8, 0, 1000);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_MAX_HEALTH_BONUS = BUILDER
            .comment("Maximum extra health granted by Tide Lantern")
            .defineInRange("tide_lantern_max_health_bonus", 8, 0, 40);

    public static final ModConfigSpec.IntValue TIDE_LANTERN_AUTO_COOLDOWN_SECONDS = BUILDER
            .comment("Low-health Tide Lantern auto-guard cooldown in seconds")
            .defineInRange("tide_lantern_auto_cooldown_seconds", 180, 10, 3600);

    public static final ModConfigSpec.IntValue ECHO_LENS_COOLDOWN_SECONDS = BUILDER
            .comment("Echo Lens active cooldown in seconds")
            .defineInRange("echo_lens_cooldown_seconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue ECHO_LENS_ENERGY_COST = BUILDER
            .comment("Echo energy cost for Echo Lens marking")
            .defineInRange("echo_lens_energy_cost", 12, 0, 1000);

    public static final ModConfigSpec.DoubleValue ECHO_LENS_DAMAGE_BONUS = BUILDER
            .comment("Echo Lens marked-target damage bonus")
            .defineInRange("echo_lens_damage_bonus", 0.20, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue ECHO_LENS_DAMAGE_CAP = BUILDER
            .comment("Echo Lens bonus damage cap per hit")
            .defineInRange("echo_lens_damage_cap", 6.0, 0.0, 1024.0);

    public static final ModConfigSpec.BooleanValue ARTIFACT_PVP_EFFECTS = BUILDER
            .comment("Allow artifact damage marks and PvP effects against players")
            .define("artifact_pvp_effects", false);

    // ---- V0.6D 世界事件框架(22.10:只在有玩家的已加载区块触发,可整体或单事件关闭) ----

    public static final ModConfigSpec.BooleanValue WORLD_EVENTS_ENABLED = BUILDER
            .comment("Master switch for ambient world events (echo fog, mirror anomaly, storm omen)")
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

    // ---- V0.6D 迷途旅者(16.4:营地随时间更换;不出售关键进度物) ----

    public static final ModConfigSpec.BooleanValue LOST_TRAVELER_CAMP_EVENT = BUILDER
            .comment("Lost traveler caravan may appear near players as a low-frequency world event")
            .define("lost_traveler_camp_event", true);

    public static final ModConfigSpec.IntValue LOST_TRAVELER_STAY_TICKS = BUILDER
            .comment("Ticks a lost traveler stays before moving camp (campfire is left extinguished)")
            .defineInRange("lost_traveler_stay_ticks", 36000, 1200, 1728000);

    // ---- V0.6E 风之滑翔能量与风流平台(5.2:消耗+落地冷却走配置;平台只是表现+运动) ----

    public static final ModConfigSpec.IntValue WIND_GLIDE_ENERGY_COST = BUILDER
            .comment("Echo energy consumed when starting a wind glide (0 disables the cost)")
            .defineInRange("wind_glide_energy_cost", 8, 0, 1000);

    public static final ModConfigSpec.IntValue WIND_GLIDE_LANDING_COOLDOWN_TICKS = BUILDER
            .comment("Ticks after landing from a glide before the next glide may start")
            .defineInRange("wind_glide_landing_cooldown_ticks", 60, 0, 1200);

    public static final ModConfigSpec.IntValue WIND_PLATFORM_ACTIVE_TICKS = BUILDER
            .comment("Ticks a wind current platform keeps its updraft column after activation")
            .defineInRange("wind_platform_active_ticks", 600, 40, 24000);

    public static final ModConfigSpec.IntValue WIND_PLATFORM_LIFT_HEIGHT = BUILDER
            .comment("Height (blocks) of the updraft column above an active wind current platform")
            .defineInRange("wind_platform_lift_height", 20, 2, 64);

    // ---- V0.6E 潮汐感应门(5.3:倒影入口稳定时间,研究 3 延长) ----

    public static final ModConfigSpec.IntValue TIDE_DOOR_HOLD_TICKS = BUILDER
            .comment("Ticks a tide sensor door stays open (reflection entry stability time)")
            .defineInRange("tide_door_hold_ticks", 200, 20, 24000);

    public static final ModConfigSpec.IntValue TIDE_DOOR_HOLD_TICKS_RESEARCH3 = BUILDER
            .comment("Ticks a tide sensor door stays open for players with tide research level 3+")
            .defineInRange("tide_door_hold_ticks_research3", 400, 20, 24000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
