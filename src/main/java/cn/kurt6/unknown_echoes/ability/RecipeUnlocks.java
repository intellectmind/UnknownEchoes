package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * 配方解锁链(V0.6B,11.7 既定规则落地):
 * 能力研究不再驱动武器配方;武器进度只由图纸、材料与合成线承担。
 * 配方书只负责显示；深层装备/武器按材料链解锁，不把能力等级当作合成权限。
 * 当前实现基线:配方书可见性解锁;硬性合成封锁仅在 doLimitedCrafting 规则开启时由原版生效。
 */
public final class RecipeUnlocks {

    private record Entry(String unlockKey, List<ResourceLocation> recipeIds,
                         java.util.function.Predicate<ServerPlayer> condition) {}

    /** 任一回响能力已解锁(聆听者套装的中期门槛,11.3 材料线 V0.5-V0.6)。 */
    private static final java.util.function.Predicate<ServerPlayer> ANY_ABILITY =
            RecipeUnlocks::hasAnyAbility;

    private static final List<Entry> ENTRIES = List.of(
            new Entry("echo_basics", List.of(
                    UnknownEchoes.id("echo_dust_from_shard"), UnknownEchoes.id("echo_metal_ingot"),
                    UnknownEchoes.id("echo_string"), UnknownEchoes.id("blank_echo_page"),
                    UnknownEchoes.id("echo_ink_page"),
                    UnknownEchoes.id("echo_stone_bricks"), UnknownEchoes.id("echo_rune_bricks")),
                    player -> hasInInventory(player, ModItems.ECHO_SHARD.get())),
            new Entry("explorer_set", List.of(
                    UnknownEchoes.id("echo_cloth"),
                    UnknownEchoes.id("explorer_helmet"), UnknownEchoes.id("explorer_chestplate"),
                    UnknownEchoes.id("explorer_leggings"), UnknownEchoes.id("explorer_boots")),
                    player -> hasInInventory(player, ModItems.ECHO_FIBER.get())
                            && hasInInventory(player, ModItems.ECHO_SHARD.get())),
            new Entry("wood_echo", List.of(
                    UnknownEchoes.id("echo_planks"), UnknownEchoes.id("echo_planks_sticks")),
                    player -> hasInInventory(player, ModItems.ECHO_LOG.get())),
            new Entry("wood_whispering", List.of(
                    UnknownEchoes.id("whispering_planks"), UnknownEchoes.id("whispering_planks_sticks")),
                    player -> hasInInventory(player, ModItems.WHISPERING_LOG.get())),
            new Entry("wood_tidewood", List.of(
                    UnknownEchoes.id("tidewood_planks"), UnknownEchoes.id("tidewood_planks_sticks")),
                    player -> hasInInventory(player, ModItems.TIDEWOOD_LOG.get())),
            new Entry("food_glow_fruit", List.of(UnknownEchoes.id("glow_fruit_jam")),
                    player -> hasInInventory(player, ModItems.GLOW_FRUIT.get())),
            new Entry("food_wind_crisp", List.of(UnknownEchoes.id("wind_crisp")),
                    player -> hasInInventory(player, ModItems.CRYSTAL_FEATHER.get())),
            new Entry("food_tide", List.of(
                    UnknownEchoes.id("tide_kelp_wrap"), UnknownEchoes.id("tide_salted_fish")),
                    player -> hasInInventory(player, ModItems.TIDE_SALT.get())),
            new Entry("decor_echo_stone", List.of(
                    UnknownEchoes.id("cracked_echo_stone_bricks"), UnknownEchoes.id("echo_mossy_stone")),
                    player -> hasInInventory(player, ModItems.ECHO_STONE_BRICKS.get())),
            new Entry("decor_mirror_lake", List.of(
                    UnknownEchoes.id("mirror_sand"), UnknownEchoes.id("mirror_stone"),
                    UnknownEchoes.id("mirror_stone_bricks")),
                    player -> hasInInventory(player, ModItems.MIRROR_LAKE_SHARD.get())),
            new Entry("decor_sky_lamp", List.of(UnknownEchoes.id("sky_lamp_glass")),
                    player -> hasInInventory(player, ModItems.SKY_QUARTZ.get())),
            new Entry("decor_pearl_coral", List.of(UnknownEchoes.id("pearl_coral_block")),
                    player -> hasInInventory(player, ModItems.PEARL_CLAM.get())),
            new Entry("stone_abyssal", List.of(
                    UnknownEchoes.id("abyssal_stone_bricks"), UnknownEchoes.id("veined_abyssal_stone")),
                    player -> hasInInventory(player, ModItems.ABYSSAL_STONE.get())),
            new Entry("stone_ashen_bell", List.of(
                    UnknownEchoes.id("ashen_bellstone_bricks"), UnknownEchoes.id("rusted_bellstone")),
                    player -> hasInInventory(player, ModItems.ASHEN_BELLSTONE.get())),
            new Entry("stone_gravity", List.of(
                    UnknownEchoes.id("banded_gravity_shale"), UnknownEchoes.id("gravity_shale_bricks")),
                    player -> hasInInventory(player, ModItems.GRAVITY_SHALE.get())),
            new Entry("stone_echo_strata", List.of(
                    UnknownEchoes.id("chiseled_echo_strata"), UnknownEchoes.id("echo_strata_bricks")),
                    player -> hasInInventory(player, ModItems.ECHO_STRATA.get())),
            new Entry("stone_folded_slate", List.of(
                    UnknownEchoes.id("chiseled_folded_slate"), UnknownEchoes.id("folded_slate_bricks")),
                    player -> hasInInventory(player, ModItems.FOLDED_SLATE.get())),
            new Entry("stone_songstone", List.of(
                    UnknownEchoes.id("chiseled_songstone"), UnknownEchoes.id("songstone_bricks")),
                    player -> hasInInventory(player, ModItems.SONGSTONE.get())),
            new Entry("stone_mirror_dust", List.of(
                    UnknownEchoes.id("fractured_mirror_tile"), UnknownEchoes.id("mirror_dust_bricks")),
                    player -> hasInInventory(player, ModItems.MIRROR_DUST_STONE.get())),
            new Entry("stone_reef_limestone", List.of(
                    UnknownEchoes.id("reef_limestone_bricks"), UnknownEchoes.id("tideworn_reefstone")),
                    player -> hasInInventory(player, ModItems.REEF_LIMESTONE.get())),
            new Entry("stone_silent_mud", List.of(
                    UnknownEchoes.id("rootbound_mudstone"), UnknownEchoes.id("silent_mudstone_bricks")),
                    player -> hasInInventory(player, ModItems.SILENT_MUDSTONE.get())),
            new Entry("stone_tide", List.of(UnknownEchoes.id("tide_smooth_stone")),
                    player -> hasInInventory(player, ModItems.MIRROR_STONE.get())),
            new Entry("stone_wind", List.of(UnknownEchoes.id("wind_chiseled_stone")),
                    player -> hasInInventory(player, ModItems.WIND_ETCHED_STONE.get())),
            new Entry("echo_workshop", List.of(
                    UnknownEchoes.id("echo_key"), UnknownEchoes.id("echo_weapon_frame"),
                    UnknownEchoes.id("echo_armor_plate"), UnknownEchoes.id("echo_broadsword"),
                    UnknownEchoes.id("echo_projector"), UnknownEchoes.id("echo_pickaxe"),
                    UnknownEchoes.id("echo_shovel"), UnknownEchoes.id("echo_hatchet"),
                    UnknownEchoes.id("echo_hoe"), UnknownEchoes.id("echo_fishing_rod"),
                    UnknownEchoes.id("echo_igniter"), UnknownEchoes.id("echo_longbow"),
                    UnknownEchoes.id("echo_traveler_helmet"), UnknownEchoes.id("echo_traveler_chestplate"),
                    UnknownEchoes.id("echo_traveler_leggings"), UnknownEchoes.id("echo_traveler_boots")),
                    player -> hasInInventory(player, ModItems.ECHO_METAL_INGOT.get())
                            && hasInInventory(player, ModItems.ECHO_CORE.get())),
            // 图纸既是配方线索也是可重复使用的合成媒介；拿到图纸后立即写入配方书。
            new Entry("wind_spear", List.of(UnknownEchoes.id("wind_spear")),
                    player -> hasInInventory(player, ModItems.WIND_SPEAR_BLUEPRINT.get())),
            new Entry("tide_crossbow", List.of(UnknownEchoes.id("tide_crossbow")),
                    player -> hasInInventory(player, ModItems.TIDE_CROSSBOW_BLUEPRINT.get())),
            new Entry("true_sight_blade", List.of(UnknownEchoes.id("true_sight_blade")),
                    player -> hasInInventory(player, ModItems.TRUE_SIGHT_BLADE_BLUEPRINT.get())),
            // 聆听者套装(V0.6F,11.3):四件随任一能力解锁,一次性提示
            new Entry("listener_set", List.of(
                    UnknownEchoes.id("listener_helmet"), UnknownEchoes.id("listener_chestplate"),
                    UnknownEchoes.id("listener_leggings"), UnknownEchoes.id("listener_boots")),
                    ANY_ABILITY),
            new Entry("wind_components", List.of(
                    UnknownEchoes.id("wind_rune_shard"), UnknownEchoes.id("windwoven_thread"),
                    UnknownEchoes.id("echo_shears"), UnknownEchoes.id("wind_enhance_core")),
                    player -> hasAnyInInventory(player,
                            ModItems.WIND_ETCHED_STONE.get(), ModItems.WIND_SAC_MEMBRANE.get())),
            new Entry("tide_components", List.of(
                    UnknownEchoes.id("mirror_tide_alloy_plate"), UnknownEchoes.id("tide_upgrade_core")),
                    player -> hasAnyInInventory(player,
                            ModItems.TIDE_RUNE_FRAGMENT.get(), ModItems.MIRROR_LAKE_SHARD.get())),
            new Entry("true_sight_components", List.of(
                    UnknownEchoes.id("polished_mirror_dust"), UnknownEchoes.id("illusion_prism"),
                    UnknownEchoes.id("true_sight_upgrade_core")),
                    player -> hasInInventory(player, ModItems.ILLUSION_DUST.get())),
            new Entry("silent_components", List.of(
                    UnknownEchoes.id("silent_resin"), UnknownEchoes.id("silence_scythe"),
                    UnknownEchoes.id("silence_upgrade_core")),
                    player -> hasAnyInInventory(player,
                            ModItems.SILENCE_MOSS.get(), ModItems.SILENT_POD.get())),
            new Entry("attunement_components", List.of(
                    UnknownEchoes.id("attuned_echo_gem")),
                    player -> hasInInventory(player, ModItems.ECHO_DUST.get())
                            && hasInInventory(player, ModItems.ECHO_SHARD.get())
                            && hasInInventory(player, ModItems.RECORD_TRACING_PAPER.get())
                            && hasInInventory(player, ModItems.INSCRIPTION_RUBBING_PAPER.get())),
            new Entry("wind_walker_set", List.of(
                    UnknownEchoes.id("wind_walker_helmet"), UnknownEchoes.id("wind_walker_chestplate"),
                    UnknownEchoes.id("wind_walker_leggings"), UnknownEchoes.id("wind_walker_boots")),
                    player -> hasInInventory(player, ModItems.WIND_ENHANCE_CORE.get())),
            new Entry("tide_stalker_set", List.of(
                    UnknownEchoes.id("tide_stalker_helmet"), UnknownEchoes.id("tide_stalker_chestplate"),
                    UnknownEchoes.id("tide_stalker_leggings"), UnknownEchoes.id("tide_stalker_boots")),
                    player -> hasInInventory(player, ModItems.TIDE_UPGRADE_CORE.get())),
            new Entry("true_sight_shadow_set", List.of(
                    UnknownEchoes.id("true_sight_shadow_helmet"), UnknownEchoes.id("true_sight_shadow_chestplate"),
                    UnknownEchoes.id("true_sight_shadow_leggings"), UnknownEchoes.id("true_sight_shadow_boots")),
                    player -> hasInInventory(player, ModItems.TRUE_SIGHT_UPGRADE_CORE.get())),
            new Entry("silent_watch_set", List.of(
                    UnknownEchoes.id("silent_watch_helmet"), UnknownEchoes.id("silent_watch_chestplate"),
                    UnknownEchoes.id("silent_watch_leggings"), UnknownEchoes.id("silent_watch_boots")),
                    player -> hasInInventory(player, ModItems.SILENCE_UPGRADE_CORE.get())),
            new Entry("crystal_song_gear", List.of(
                    UnknownEchoes.id("crystal_resonance_rod"), UnknownEchoes.id("crystal_song_staff"),
                    UnknownEchoes.id("echo_brush"), UnknownEchoes.id("crystal_song_upgrade_core")),
                    player -> hasAnyInInventory(player,
                            ModItems.CRYSTAL_SONG_SHARD.get(), ModItems.RESONANT_CRYSTAL_NOTE.get())),
            new Entry("broken_bell_gear", List.of(
                    UnknownEchoes.id("broken_clockwork_plate"), UnknownEchoes.id("broken_bell_hammer"),
                    UnknownEchoes.id("echo_battleaxe"), UnknownEchoes.id("broken_bell_upgrade_core")),
                    player -> hasAnyInInventory(player,
                            ModItems.BROKEN_BELL_GEAR.get(), ModItems.RUSTED_TIME_GEAR.get())),
            new Entry("dream_bloom_crafts", List.of(
                    UnknownEchoes.id("moss_honey_stew"), UnknownEchoes.id("dream_nectar_tart"),
                    UnknownEchoes.id("dream_bloom_bow")),
                    player -> hasInInventory(player, ModItems.DREAM_NECTAR.get())),
            new Entry("wind_research_page", List.of(UnknownEchoes.id("wind_fragment_page")),
                    player -> hasInInventory(player, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(player, ModItems.WIND_RESEARCH_RUBBING.get())),
            new Entry("tide_research_page", List.of(UnknownEchoes.id("tide_fragment_page")),
                    player -> hasInInventory(player, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(player, ModItems.TIDE_RESEARCH_RUBBING.get())),
            new Entry("true_sight_research_page", List.of(UnknownEchoes.id("true_sight_fragment_page")),
                    player -> hasInInventory(player, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(player, ModItems.TRUE_SIGHT_RESEARCH_RUBBING.get())),
            new Entry("gravity_research_page", List.of(UnknownEchoes.id("gravity_fragment_page")),
                    player -> hasInInventory(player, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(player, ModItems.GRAVITY_RESEARCH_RUBBING.get())),
            new Entry("void_research_page", List.of(UnknownEchoes.id("void_fragment_page")),
                    player -> hasInInventory(player, ModItems.ECHO_INK_PAGE.get())
                            && hasInInventory(player, ModItems.VOID_RESEARCH_RUBBING.get())),
            new Entry("deep_gravity_equipment", List.of(
                    UnknownEchoes.id("deep_alloy_ingot"),
                    UnknownEchoes.id("abyss_walker_helmet"), UnknownEchoes.id("abyss_walker_chestplate"),
                    UnknownEchoes.id("abyss_walker_leggings"), UnknownEchoes.id("abyss_walker_boots")),
                    player -> hasInInventory(player, ModItems.DEEP_ORE_SCRAP.get())
                            && hasInInventory(player, ModItems.SPACE_ANCHOR_SHARD.get())
                            && hasInInventory(player, ModItems.FOLDED_CARAPACE.get())),
            new Entry("gravity_hammer", List.of(UnknownEchoes.id("gravity_hammer")),
                    player -> hasInInventory(player, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(player, ModItems.GRAVITY_CORE_FRAGMENT.get())),
            new Entry("rift_dagger", List.of(UnknownEchoes.id("rift_dagger")),
                    player -> hasInInventory(player, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(player, ModItems.VOID_RIFT_SHARD.get())),
            new Entry("oath_equipment", List.of(
                    UnknownEchoes.id("oathbound_alloy"), UnknownEchoes.id("echo_oathblade"),
                    UnknownEchoes.id("echo_oath_helmet"), UnknownEchoes.id("echo_oath_chestplate"),
                    UnknownEchoes.id("echo_oath_leggings"), UnknownEchoes.id("echo_oath_boots")),
                    player -> hasInInventory(player, ModItems.ECHO_REALM_INGOT.get())
                            && hasInInventory(player, ModItems.COLOSSUS_MEMORY_CORE.get())
                            && hasInInventory(player, ModItems.TIDE_CORE.get())
                            && hasInInventory(player, ModItems.MIRROR_GUARDIAN_CORE.get()))
    );

    private RecipeUnlocks() {
    }

    /** 能力/研究进度变化后核对一次;条件满足即解锁,已解锁的只静默补缺。 */
    public static void checkAndUnlock(ServerPlayer player) {
        for (Entry entry : ENTRIES) {
            String mechanismKey = "recipe_unlock:" + entry.unlockKey();
            boolean firstUnlock = !EchoAbilityManager.hasActivatedMechanism(player, mechanismKey)
                    && !coveredByLegacyGroup(player, entry.unlockKey());
            if (firstUnlock && !entry.condition().test(player)) {
                continue;
            }
            List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders = entry.recipeIds().stream()
                    .flatMap(id -> player.server.getRecipeManager().byKey(id).stream())
                    .collect(java.util.stream.Collectors.toList());
            // 数据包重载期间可能只加载到组内一部分配方；此时不能提前写整组标记。
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
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_IN, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.recipe_unlocked." + entry.unlockKey()));
        }
    }

    private static boolean hasInInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().contains(item.getDefaultInstance());
    }

    private static boolean hasAnyInInventory(ServerPlayer player, net.minecraft.world.item.Item... items) {
        for (net.minecraft.world.item.Item item : items) {
            if (hasInInventory(player, item)) {
                return true;
            }
        }
        return false;
    }

    /** 旧版四个大组已经一次性授予全部配方；细分后不向旧存档重复弹提示。 */
    private static boolean coveredByLegacyGroup(ServerPlayer player, String unlockKey) {
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
        return legacyKey != null && EchoAbilityManager.hasActivatedMechanism(
                player, "recipe_unlock:" + legacyKey);
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
