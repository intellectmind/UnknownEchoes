package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/** 配方解锁链: 武器进度只由图纸、材料与合成线承担,能力研究不参与。 */
public final class RecipeUnlocks {

    private record Entry(String unlockKey, List<ResourceLocation> recipeIds,
                         BiPredicate<ServerPlayer, Set<Item>> condition) {}

    /** 任一回响能力已解锁，用作聆听者套装的中期门槛。 */
    private static final BiPredicate<ServerPlayer, Set<Item>> ANY_ABILITY =
            (player, presentItems) -> hasAnyAbility(player);

    private static final List<Entry> ENTRIES = List.of(
            new Entry("echo_basics", List.of(
                    UnknownEchoes.id("echo_dust_from_shard"), UnknownEchoes.id("echo_metal_ingot"),
                    UnknownEchoes.id("echo_string"), UnknownEchoes.id("blank_echo_page"),
                    UnknownEchoes.id("echo_ink_page"), UnknownEchoes.id("blank_echo_page_salvage"),
                    UnknownEchoes.id("echo_ink_page_salvage"),
                    UnknownEchoes.id("echo_stone_bricks"), UnknownEchoes.id("echo_rune_bricks")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_SHARD.get())),
            new Entry("explorer_set", List.of(
                    UnknownEchoes.id("echo_cloth"),
                    UnknownEchoes.id("explorer_helmet"), UnknownEchoes.id("explorer_chestplate"),
                    UnknownEchoes.id("explorer_leggings"), UnknownEchoes.id("explorer_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_FIBER.get())
                            && hasInInventory(presentItems, ModItems.ECHO_SHARD.get())),
            new Entry("wood_echo", List.of(
                    UnknownEchoes.id("echo_planks"), UnknownEchoes.id("echo_planks_sticks")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_LOG.get())),
            new Entry("wood_whispering", List.of(
                    UnknownEchoes.id("whispering_planks"), UnknownEchoes.id("whispering_planks_sticks"),
                    UnknownEchoes.id("whispering_lattice"),
                    UnknownEchoes.id("whispering_timber_frame"),
                    UnknownEchoes.id("whispering_timber_stairs"),
                    UnknownEchoes.id("whispering_timber_slab"),
                    UnknownEchoes.id("whispering_timber_wall")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.WHISPERING_LOG.get())),
            new Entry("wood_tidewood", List.of(
                    UnknownEchoes.id("tidewood_planks"), UnknownEchoes.id("tidewood_planks_sticks")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TIDEWOOD_LOG.get())),
            new Entry("food_glow_fruit", List.of(UnknownEchoes.id("glow_fruit_jam")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.GLOW_FRUIT.get())),
            new Entry("food_wind_crisp", List.of(UnknownEchoes.id("wind_crisp")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.CRYSTAL_FEATHER.get())),
            new Entry("food_tide", List.of(
                    UnknownEchoes.id("tide_kelp_wrap"), UnknownEchoes.id("tide_salted_fish")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TIDE_SALT.get())),
            new Entry("decor_echo_stone", List.of(
                    UnknownEchoes.id("cracked_echo_stone_bricks"), UnknownEchoes.id("echo_mossy_stone"),
                    UnknownEchoes.id("echo_stone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("echo_stone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("echo_stone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_STONE_BRICKS.get())),
            new Entry("decor_mirror_lake", List.of(
                    UnknownEchoes.id("mirror_sand"), UnknownEchoes.id("mirror_stone"),
                    UnknownEchoes.id("mirror_stone_bricks"),
                    UnknownEchoes.id("mirror_stone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("mirror_stone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("mirror_stone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.MIRROR_LAKE_SHARD.get())),
            new Entry("decor_sky_lamp", List.of(UnknownEchoes.id("sky_lamp_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SKY_QUARTZ.get())),
            // 主题灯各自绑定所属群系的材料，玩家到那个群系才解锁对应灯色。
            new Entry("decor_reef_lantern", List.of(UnknownEchoes.id("reef_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.MIRROR_LAKE_SHARD.get())),
            new Entry("decor_bell_lantern", List.of(UnknownEchoes.id("bell_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.BROKEN_BELL_GEAR.get())),
            new Entry("decor_hush_lantern", List.of(UnknownEchoes.id("hush_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SILENT_RESIN.get())),
            new Entry("decor_fold_lantern", List.of(UnknownEchoes.id("fold_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.FOLDED_SLATE.get())),
            new Entry("decor_song_lantern", List.of(UnknownEchoes.id("song_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SONGSTONE.get())),
            new Entry("decor_archive_lantern", List.of(UnknownEchoes.id("archive_lantern_glass")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_STRATA.get())),
            new Entry("decor_pearl_coral", List.of(UnknownEchoes.id("pearl_coral_block")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.PEARL_CLAM.get())),
            new Entry("stone_abyssal", List.of(
                    UnknownEchoes.id("abyssal_stone_bricks"), UnknownEchoes.id("veined_abyssal_stone"),
                    UnknownEchoes.id("abyssal_stone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("abyssal_stone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("abyssal_stone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ABYSSAL_STONE.get())),
            new Entry("stone_ashen_bell", List.of(
                    UnknownEchoes.id("ashen_bellstone_bricks"), UnknownEchoes.id("rusted_bellstone"),
                    UnknownEchoes.id("ashen_bellstone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("ashen_bellstone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("ashen_bellstone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ASHEN_BELLSTONE.get())),
            new Entry("stone_gravity", List.of(
                    UnknownEchoes.id("banded_gravity_shale"), UnknownEchoes.id("gravity_shale_bricks"),
                    UnknownEchoes.id("gravity_shale_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("gravity_shale_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("gravity_shale_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.GRAVITY_SHALE.get())),
            new Entry("stone_echo_strata", List.of(
                    UnknownEchoes.id("chiseled_echo_strata"), UnknownEchoes.id("echo_strata_bricks"),
                    UnknownEchoes.id("archive_mosaic"),
                    UnknownEchoes.id("echo_strata_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("echo_strata_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("echo_strata_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_STRATA.get())),
            new Entry("stone_folded_slate", List.of(
                    UnknownEchoes.id("chiseled_folded_slate"), UnknownEchoes.id("folded_slate_bricks"),
                    UnknownEchoes.id("folded_slate_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("folded_slate_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("folded_slate_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.FOLDED_SLATE.get())),
            new Entry("stone_songstone", List.of(
                    UnknownEchoes.id("chiseled_songstone"), UnknownEchoes.id("songstone_bricks"),
                    UnknownEchoes.id("songstone_tiles"), UnknownEchoes.id("cracked_songstone_bricks"),
                    UnknownEchoes.id("songstone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("songstone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("songstone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SONGSTONE.get())),
            new Entry("stone_mirror_dust", List.of(
                    UnknownEchoes.id("fractured_mirror_tile"), UnknownEchoes.id("mirror_dust_bricks"),
                    UnknownEchoes.id("mirror_dust_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("mirror_dust_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("mirror_dust_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.MIRROR_DUST_STONE.get())),
            new Entry("stone_reef_limestone", List.of(
                    UnknownEchoes.id("reef_limestone_bricks"), UnknownEchoes.id("tideworn_reefstone"),
                    UnknownEchoes.id("reef_limestone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("reef_limestone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("reef_limestone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.REEF_LIMESTONE.get())),
            new Entry("stone_silent_mud", List.of(
                    UnknownEchoes.id("rootbound_mudstone"), UnknownEchoes.id("silent_mudstone_bricks"),
                    UnknownEchoes.id("silent_mudstone_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("silent_mudstone_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("silent_mudstone_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SILENT_MUDSTONE.get())),
            new Entry("stone_tide", List.of(UnknownEchoes.id("tide_smooth_stone")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.MIRROR_STONE.get())),
            new Entry("stone_wind", List.of(UnknownEchoes.id("wind_chiseled_stone"),
                    UnknownEchoes.id("wind_etched_bricks"),
                    UnknownEchoes.id("wind_etched_brick_stairs_from_stonecutting"),
                    UnknownEchoes.id("wind_etched_brick_slab_from_stonecutting"),
                    UnknownEchoes.id("wind_etched_brick_wall_from_stonecutting")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.WIND_ETCHED_STONE.get())),
            new Entry("echo_workshop", List.of(
                    UnknownEchoes.id("echo_key"), UnknownEchoes.id("echo_key_salvage"),
                    UnknownEchoes.id("echo_weapon_frame"),
                    UnknownEchoes.id("echo_armor_plate"), UnknownEchoes.id("echo_broadsword"),
                    UnknownEchoes.id("echo_projector"), UnknownEchoes.id("projection_resonance_plate"),
                    UnknownEchoes.id("echo_pickaxe"),
                    UnknownEchoes.id("echo_shovel"), UnknownEchoes.id("echo_hatchet"),
                    UnknownEchoes.id("echo_hoe"), UnknownEchoes.id("echo_fishing_rod"),
                    UnknownEchoes.id("echo_igniter"), UnknownEchoes.id("echo_longbow"),
                    UnknownEchoes.id("echo_traveler_helmet"), UnknownEchoes.id("echo_traveler_chestplate"),
                    UnknownEchoes.id("echo_traveler_leggings"), UnknownEchoes.id("echo_traveler_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_METAL_INGOT.get())
                            && hasInInventory(presentItems, ModItems.ECHO_CORE.get())),

            new Entry("wind_spear", List.of(UnknownEchoes.id("wind_spear")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.WIND_SPEAR_BLUEPRINT.get())),
            new Entry("tide_crossbow", List.of(UnknownEchoes.id("tide_crossbow")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TIDE_CROSSBOW_BLUEPRINT.get())),
            new Entry("gravity_inversion_bow", List.of(
                    UnknownEchoes.id("gravity_inversion_bow"),
                    UnknownEchoes.id("inverted_bow_limb_salvage")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.INVERTED_BOW_LIMB.get())),
            new Entry("ash_script_crossbow", List.of(
                    UnknownEchoes.id("ash_script_crossbow"),
                    UnknownEchoes.id("ash_script_mechanism_salvage")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ASH_SCRIPT_MECHANISM.get())),
            new Entry("true_sight_blade", List.of(UnknownEchoes.id("true_sight_blade")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TRUE_SIGHT_BLADE_BLUEPRINT.get())),

            new Entry("listener_set", List.of(
                    UnknownEchoes.id("listener_helmet"), UnknownEchoes.id("listener_chestplate"),
                    UnknownEchoes.id("listener_leggings"), UnknownEchoes.id("listener_boots")),
                    ANY_ABILITY),
            new Entry("wind_components", List.of(
                    UnknownEchoes.id("wind_rune_shard"), UnknownEchoes.id("windwoven_thread"),
                    UnknownEchoes.id("echo_shears"), UnknownEchoes.id("wind_enhance_core"),
                    UnknownEchoes.id("wind_chime_bottle")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.WIND_ETCHED_STONE.get(), ModItems.WIND_SAC_MEMBRANE.get())),
            new Entry("tide_components", List.of(
                    UnknownEchoes.id("mirror_tide_alloy_plate"), UnknownEchoes.id("tide_upgrade_core"),
                    UnknownEchoes.id("tide_breath_bottle")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.TIDE_RUNE_FRAGMENT.get(), ModItems.MIRROR_LAKE_SHARD.get(),
                            ModItems.TIDE_SALT.get())),
            new Entry("true_sight_components", List.of(
                    UnknownEchoes.id("polished_mirror_dust"), UnknownEchoes.id("illusion_prism"),
                    UnknownEchoes.id("true_sight_upgrade_core"), UnknownEchoes.id("revealing_powder")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ILLUSION_DUST.get())),
            new Entry("silent_components", List.of(
                    UnknownEchoes.id("silent_resin"), UnknownEchoes.id("silence_scythe"),
                    UnknownEchoes.id("silence_upgrade_core")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.SILENCE_MOSS.get(), ModItems.SILENT_POD.get())),
            new Entry("attunement_components", List.of(
                    UnknownEchoes.id("attuned_echo_gem")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_DUST.get())
                            && hasInInventory(presentItems, ModItems.ECHO_SHARD.get())
                            && hasInInventory(presentItems, ModItems.RECORD_TRACING_PAPER.get())
                            && hasInInventory(presentItems, ModItems.INSCRIPTION_RUBBING_PAPER.get())),
            new Entry("mini_boss_oath_recovery", List.of(
                    UnknownEchoes.id("mini_boss_oath_material_recovery")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.STORM_SPINDLE.get(), ModItems.BROKEN_LANTERN_LENS.get(),
                            ModItems.MIRROR_STAFF_CORE.get(), ModItems.SILENT_CANDLE_WICK.get(),
                            ModItems.SONGKEEPER_FORK.get(), ModItems.BELL_CLAPPER.get(),
                            ModItems.ARCHIVE_BINDING.get())),
            new Entry("region_boss_core_recovery", List.of(
                    UnknownEchoes.id("region_boss_core_recovery")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.COLOSSUS_MEMORY_CORE.get(), ModItems.TIDE_CORE.get(),
                            ModItems.TRUE_SIGHT_CORE.get())),
            new Entry("wind_walker_set", List.of(
                    UnknownEchoes.id("wind_walker_helmet"), UnknownEchoes.id("wind_walker_chestplate"),
                    UnknownEchoes.id("wind_walker_leggings"), UnknownEchoes.id("wind_walker_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.WIND_ENHANCE_CORE.get())),
            new Entry("tide_stalker_set", List.of(
                    UnknownEchoes.id("tide_stalker_helmet"), UnknownEchoes.id("tide_stalker_chestplate"),
                    UnknownEchoes.id("tide_stalker_leggings"), UnknownEchoes.id("tide_stalker_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TIDE_UPGRADE_CORE.get())),
            new Entry("true_sight_shadow_set", List.of(
                    UnknownEchoes.id("true_sight_shadow_helmet"), UnknownEchoes.id("true_sight_shadow_chestplate"),
                    UnknownEchoes.id("true_sight_shadow_leggings"), UnknownEchoes.id("true_sight_shadow_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.TRUE_SIGHT_UPGRADE_CORE.get())),
            new Entry("silent_watch_set", List.of(
                    UnknownEchoes.id("silent_watch_helmet"), UnknownEchoes.id("silent_watch_chestplate"),
                    UnknownEchoes.id("silent_watch_leggings"), UnknownEchoes.id("silent_watch_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SILENCE_UPGRADE_CORE.get())),
            new Entry("crystal_song_gear", List.of(
                    UnknownEchoes.id("crystal_resonance_rod"), UnknownEchoes.id("crystal_song_staff"),
                    UnknownEchoes.id("echo_brush"), UnknownEchoes.id("crystal_song_upgrade_core")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.CRYSTAL_SONG_SHARD.get(), ModItems.RESONANT_CRYSTAL_NOTE.get())),
            new Entry("broken_bell_gear", List.of(
                    UnknownEchoes.id("broken_clockwork_plate"), UnknownEchoes.id("broken_bell_hammer"),
                    UnknownEchoes.id("echo_battleaxe"), UnknownEchoes.id("broken_bell_upgrade_core")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.BROKEN_BELL_GEAR.get(), ModItems.RUSTED_TIME_GEAR.get())),
            new Entry("resource_clockwork_salvage", List.of(
                    UnknownEchoes.id("broken_bell_gear_from_clockwork_scrap")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.CLOCKWORK_SCRAP_BLOCK.get())),
            new Entry("resource_prism_salvage", List.of(
                    UnknownEchoes.id("illusion_dust_from_prism_cluster")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.PRISM_DUST_CLUSTER.get())),
            new Entry("resource_geode_salvage", List.of(
                    UnknownEchoes.id("resonant_crystal_note_from_geode")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.RESONANT_GEODE.get())),
            new Entry("resource_cliff_crystal_salvage", List.of(
                    UnknownEchoes.id("sky_quartz_from_cliff_crystal")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.RESONANT_CLIFF_CRYSTAL.get())),
            new Entry("resource_resonant_slate_rubbing", List.of(
                    UnknownEchoes.id("record_tracing_paper_from_resonant_slate")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.RESONANT_FOLDED_SLATE.get())),
            new Entry("dream_bloom_crafts", List.of(
                    UnknownEchoes.id("moss_honey_stew"), UnknownEchoes.id("dream_nectar_tart"),
                    UnknownEchoes.id("dream_bloom_bow")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DREAM_NECTAR.get())),
            new Entry("wind_research_page", List.of(UnknownEchoes.id("wind_fragment_page")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(presentItems, ModItems.WIND_RESEARCH_RUBBING.get())),
            new Entry("tide_research_page", List.of(UnknownEchoes.id("tide_fragment_page")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(presentItems, ModItems.TIDE_RESEARCH_RUBBING.get())),
            new Entry("true_sight_research_page", List.of(UnknownEchoes.id("true_sight_fragment_page")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(presentItems, ModItems.TRUE_SIGHT_RESEARCH_RUBBING.get())),
            new Entry("gravity_research_page", List.of(UnknownEchoes.id("gravity_fragment_page")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(presentItems, ModItems.GRAVITY_RESEARCH_RUBBING.get())),
            new Entry("void_research_page", List.of(UnknownEchoes.id("void_fragment_page")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(presentItems, ModItems.VOID_RESEARCH_RUBBING.get())),
            new Entry("deep_gravity_equipment", List.of(
                    UnknownEchoes.id("deep_alloy_ingot"),
                    UnknownEchoes.id("abyss_walker_helmet"), UnknownEchoes.id("abyss_walker_chestplate"),
                    UnknownEchoes.id("abyss_walker_leggings"), UnknownEchoes.id("abyss_walker_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DEEP_ORE_SCRAP.get())
                            && hasInInventory(presentItems, ModItems.SPACE_ANCHOR_SHARD.get())
                            && hasInInventory(presentItems, ModItems.FOLDED_CARAPACE.get())),
            new Entry("gravity_hammer", List.of(UnknownEchoes.id("gravity_hammer")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(presentItems, ModItems.GRAVITY_CORE_FRAGMENT.get())),
            new Entry("rift_dagger", List.of(UnknownEchoes.id("rift_dagger")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(presentItems, ModItems.VOID_RIFT_SHARD.get())),
            new Entry("deep_alloy_toolkit", List.of(
                    UnknownEchoes.id("space_anchor_rod"),
                    UnknownEchoes.id("deep_alloy_pickaxe"), UnknownEchoes.id("deep_alloy_shovel"),
                    UnknownEchoes.id("deep_alloy_axe"), UnknownEchoes.id("deep_alloy_hoe"),
                    UnknownEchoes.id("anchor_blade"), UnknownEchoes.id("folded_glaive")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(presentItems, ModItems.SPACE_ANCHOR_SHARD.get())),
            new Entry("deep_weapon_salvage", List.of(
                    UnknownEchoes.id("gravity_hammer_salvage"),
                    UnknownEchoes.id("rift_dagger_salvage"),
                    UnknownEchoes.id("anchor_blade_salvage"),
                    UnknownEchoes.id("folded_glaive_salvage")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.GRAVITY_HAMMER.get(), ModItems.RIFT_DAGGER.get(),
                            ModItems.ANCHOR_BLADE.get(), ModItems.FOLDED_GLAIVE.get())),
            new Entry("world_echo_relic_salvage", List.of(
                    UnknownEchoes.id("world_echo_wings_salvage"),
                    UnknownEchoes.id("riftwing_relay_salvage"),
                    UnknownEchoes.id("perpetual_echo_core_salvage"),
                    UnknownEchoes.id("world_rift_greatsword_salvage"),
                    UnknownEchoes.id("fivefold_star_spear_salvage")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.WORLD_ECHO_WINGS.get(), ModItems.RIFTWING_RELAY.get(),
                            ModItems.PERPETUAL_ECHO_CORE.get(), ModItems.WORLD_RIFT_GREATSWORD.get(),
                            ModItems.FIVEFOLD_STAR_SPEAR.get())),
            new Entry("energy_candy_salvage", List.of(
                    UnknownEchoes.id("faint_echo_candy_salvage"),
                    UnknownEchoes.id("echo_candy_salvage"),
                    UnknownEchoes.id("condensed_echo_candy_salvage"),
                    UnknownEchoes.id("pure_echo_candy_salvage")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.PERPETUAL_ECHO_CORE.get())),
            new Entry("shattered_codex_salvage", List.of(
                    UnknownEchoes.id("palimpsest_saber_salvage"),
                    UnknownEchoes.id("archive_chisel_salvage")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.PALIMPSEST_SABER.get(), ModItems.ARCHIVE_CHISEL.get())),
            new Entry("weightless_orrery_salvage", List.of(
                    UnknownEchoes.id("orrery_lance_salvage"),
                    UnknownEchoes.id("orrery_calibrator_salvage")),
                    (player, presentItems) -> hasAnyInInventory(presentItems,
                            ModItems.ORRERY_LANCE.get(), ModItems.ORRERY_CALIBRATOR.get())),
            new Entry("abyss_walker_boots_salvage", List.of(
                    UnknownEchoes.id("abyss_walker_boots_salvage")),
                    (player, presentItems) -> hasInInventory(
                            presentItems, ModItems.ABYSS_WALKER_BOOTS.get())),
            new Entry("shattered_codex_rewards", List.of(
                    UnknownEchoes.id("palimpsest_saber"), UnknownEchoes.id("archive_chisel"),
                    UnknownEchoes.id("palimpsest_block"),
                    UnknownEchoes.id("palimpsest_leaf_from_block")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.PALIMPSEST_LEAF.get())),
            new Entry("weightless_orrery_rewards", List.of(
                    UnknownEchoes.id("orrery_lance"), UnknownEchoes.id("orrery_calibrator"),
                    UnknownEchoes.id("orrery_prism_block"),
                    UnknownEchoes.id("orrery_prism_core_from_block")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ORRERY_PRISM_CORE.get())),
            new Entry("deep_archive_cuisine", List.of(
                    UnknownEchoes.id("archive_spore_roll")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ARCHIVE_FUNGUS.get())
                            || hasInInventory(presentItems, ModItems.FOLDED_SPOREBLOOM.get())),
            new Entry("deep_weightless_cuisine", List.of(
                    UnknownEchoes.id("weightless_bloom_tea")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.WEIGHTLESS_BLOOM.get())),
            new Entry("deep_void_cuisine", List.of(
                    UnknownEchoes.id("voidcap_stew")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.VOIDCAP.get())),
            new Entry("deep_folded_wild_cuisine", List.of(
                    UnknownEchoes.id("folded_spore_crisp")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.FOLDED_SPOREBLOOM.get())),
            new Entry("deep_gravity_wild_cuisine", List.of(
                    UnknownEchoes.id("gravity_bell_jelly")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.GRAVITY_BELL.get())),
            new Entry("deep_hushlight_cuisine", List.of(
                    UnknownEchoes.id("gravity_bell_jelly_from_hushlight")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.HUSHLIGHT_GEL.get())),
            new Entry("deep_hushed_wild_cuisine", List.of(
                    UnknownEchoes.id("hush_thorn_candy")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.HUSH_THORN.get())),
            new Entry("deep_ashback_cuisine", List.of(
                    UnknownEchoes.id("interlayer_provision_pie_from_ashback")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ASHBACK_MOSS_MEAT.get())),
            new Entry("deep_provision_brazier", List.of(
                    UnknownEchoes.id("deep_provision_brazier")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.DEEP_ALLOY_INGOT.get())
                            && (hasInInventory(presentItems, ModItems.FOLDED_SPOREBLOOM.get())
                            || hasInInventory(presentItems, ModItems.GRAVITY_BELL.get())
                            || hasInInventory(presentItems, ModItems.HUSH_THORN.get()))),
            new Entry("three_realm_cuisine", List.of(
                    UnknownEchoes.id("far_bell_trail_loaf"),
                    UnknownEchoes.id("prismatic_canopy_tart"),
                    UnknownEchoes.id("interlayer_provision_pie")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_SHARD.get())
                            || hasInInventory(presentItems, ModItems.DREAM_NECTAR.get())
                            || hasInInventory(presentItems, ModItems.FOLDED_SPOREBLOOM.get())
                            || hasInInventory(presentItems, ModItems.GRAVITY_BELL.get())
                            || hasInInventory(presentItems, ModItems.HUSH_THORN.get())),
            new Entry("communal_echo_table", List.of(
                    UnknownEchoes.id("communal_echo_table")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_METAL_INGOT.get())),
            new Entry("realm_service_plinth", List.of(
                    UnknownEchoes.id("realm_service_plinth")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_METAL_INGOT.get())),
            new Entry("realm_habitat_chime", List.of(
                    UnknownEchoes.id("realm_habitat_chime")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_METAL_INGOT.get())),
            new Entry("realm_field_lens", List.of(
                    UnknownEchoes.id("realm_field_lens")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_METAL_INGOT.get())),
            new Entry("deep_landmark_utilities", List.of(
                    UnknownEchoes.id("hushed_pilgrimage_bell"),
                    UnknownEchoes.id("fracture_surveyor")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.SILENT_RESIN.get())
                            || hasInInventory(presentItems, ModItems.ILLUSION_PRISM.get())),
            new Entry("resonator_equipment", List.of(
                    UnknownEchoes.id("resonance_alloy_ingot"),
                    UnknownEchoes.id("resonance_alloy_block"),
                    UnknownEchoes.id("resonance_alloy_ingot_from_block"),
                    UnknownEchoes.id("resonator_helmet"), UnknownEchoes.id("resonator_chestplate"),
                    UnknownEchoes.id("resonator_leggings"), UnknownEchoes.id("resonator_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.LAST_RECORD_FRAGMENT.get())),
            new Entry("oath_equipment", List.of(
                    UnknownEchoes.id("oathbound_alloy"), UnknownEchoes.id("echo_oathblade"),
                    UnknownEchoes.id("echo_oath_helmet"), UnknownEchoes.id("echo_oath_chestplate"),
                    UnknownEchoes.id("echo_oath_leggings"), UnknownEchoes.id("echo_oath_boots")),
                    (player, presentItems) -> hasInInventory(presentItems, ModItems.ECHO_REALM_INGOT.get())
                            && hasInInventory(presentItems, ModItems.COLOSSUS_MEMORY_CORE.get())
                            && hasInInventory(presentItems, ModItems.TIDE_CORE.get())
                            && hasInInventory(presentItems, ModItems.MIRROR_GUARDIAN_CORE.get()))
    );

    private RecipeUnlocks() {
    }

    /** 登录、进度变化或数据包重载后全量核对；已解锁组只静默补缺。 */
    public static void checkAndUnlock(ServerPlayer player) {
        checkAndUnlock(player, true);
    }

    /** 常驻 100 tick 路径只检查未解锁组，避免重复查询完整配方书。 */
    public static void checkPendingUnlocks(ServerPlayer player) {
        checkAndUnlock(player, false);
    }

    private static void checkAndUnlock(ServerPlayer player, boolean repairUnlocked) {
        Set<String> activatedMechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        Set<Item> presentItems = null;
        for (Entry entry : ENTRIES) {
            String mechanismKey = "recipe_unlock:" + entry.unlockKey();
            boolean firstUnlock = !activatedMechanisms.contains(mechanismKey)
                    && !coveredByLegacyGroup(activatedMechanisms, entry.unlockKey());
            if (firstUnlock) {
                if (presentItems == null) {
                    presentItems = collectInventoryItems(player);
                }
                if (!entry.condition().test(player, presentItems)) {
                    continue;
                }
            } else if (!repairUnlocked) {
                continue;
            }
            List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders = entry.recipeIds().stream()
                    .flatMap(id -> player.server.getRecipeManager().byKey(id).stream())
                    .collect(java.util.stream.Collectors.toList());

            if (holders.size() != entry.recipeIds().size()) {
                continue;
            }
            List<net.minecraft.world.item.crafting.RecipeHolder<?>> missing = holders.stream()
                    .filter(holder -> !player.getRecipeBook().contains(holder))
                    .collect(java.util.stream.Collectors.toList());
            if (!missing.isEmpty()) {
                player.awardRecipes(missing);
            }
            if (!firstUnlock) {
                continue;
            }
            if (!EchoAbilityManager.activateMechanism(player, mechanismKey)) {
                continue;
            }
        }
    }

    private static Set<Item> collectInventoryItems(ServerPlayer player) {
        Set<Item> presentItems = new HashSet<>();
        collectInventoryItems(presentItems, player.getInventory().items);
        collectInventoryItems(presentItems, player.getInventory().armor);
        collectInventoryItems(presentItems, player.getInventory().offhand);
        return presentItems;
    }

    private static void collectInventoryItems(Set<Item> presentItems, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {

            if (!stack.isEmpty()) {
                presentItems.add(stack.getItem());
            }
        }
    }

    private static boolean hasInInventory(Set<Item> presentItems, Item item) {
        return presentItems.contains(item);
    }

    private static boolean hasAnyInInventory(Set<Item> presentItems, Item... items) {
        for (Item item : items) {
            if (hasInInventory(presentItems, item)) {
                return true;
            }
        }
        return false;
    }

    /** 已完成的配方组只静默补缺，避免重复提示。 */
    private static boolean coveredByLegacyGroup(Set<String> activatedMechanisms, String unlockKey) {
        String legacyKey = switch (unlockKey) {
            case "wood_echo", "wood_whispering", "wood_tidewood" -> "woodworking";
            case "food_glow_fruit", "food_wind_crisp", "food_tide" -> "regional_foods";
            case "decor_echo_stone", "decor_mirror_lake", "decor_sky_lamp", "decor_pearl_coral" ->
                    "decorative_crafts";
            case "stone_abyssal", "stone_ashen_bell", "stone_gravity", "stone_echo_strata",
                 "stone_folded_slate", "stone_songstone", "stone_mirror_dust", "stone_reef_limestone",
                 "stone_silent_mud", "stone_tide", "stone_wind" -> "stonework";
            default -> null;
        };
        return legacyKey != null && activatedMechanisms.contains("recipe_unlock:" + legacyKey);
    }

    private static boolean hasAnyAbility(ServerPlayer player) {
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            if (EchoAbilityManager.hasAbility(player, ability)) {
                return true;
            }
        }
        return false;
    }
}
