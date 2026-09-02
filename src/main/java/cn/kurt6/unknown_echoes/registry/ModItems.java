package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.item.AncientPageItem;
import cn.kurt6.unknown_echoes.item.DeepEchoFoodItem;
import cn.kurt6.unknown_echoes.item.EchoEnergyConsumableItem;
import cn.kurt6.unknown_echoes.item.EchoKeyItem;
import cn.kurt6.unknown_echoes.item.EchoMaterialItem;
import cn.kurt6.unknown_echoes.item.EchoProjectorItem;
import cn.kurt6.unknown_echoes.item.EchoRealmCoreItem;
import cn.kurt6.unknown_echoes.item.EchoSetArmorItem;
import cn.kurt6.unknown_echoes.item.EchoUtilityConsumableItem;
import cn.kurt6.unknown_echoes.item.EquipmentBlueprintItem;
import cn.kurt6.unknown_echoes.item.FractureSurveyorItem;
import cn.kurt6.unknown_echoes.item.GenericArtifactItem;
import cn.kurt6.unknown_echoes.item.HushedPilgrimageBellItem;
import cn.kurt6.unknown_echoes.item.PerpetualEchoCoreItem;
import cn.kurt6.unknown_echoes.item.ResearchRubbingItem;
import cn.kurt6.unknown_echoes.item.RealmCuisineItem;
import cn.kurt6.unknown_echoes.item.RealmFieldLensItem;
import cn.kurt6.unknown_echoes.item.RiftwingRelayItem;
import cn.kurt6.unknown_echoes.item.SilentPodItem;
import cn.kurt6.unknown_echoes.item.SimpleEchoFoodItem;
import cn.kurt6.unknown_echoes.item.WorldEchoWingsItem;
import cn.kurt6.unknown_echoes.item.WorldtreeFruitItem;
import cn.kurt6.unknown_echoes.item.tool.EchoBrushItem;
import cn.kurt6.unknown_echoes.item.tool.DeepAlloyTier;
import cn.kurt6.unknown_echoes.item.tool.DeepAxeItem;
import cn.kurt6.unknown_echoes.item.tool.DeepHoeItem;
import cn.kurt6.unknown_echoes.item.tool.DeepPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.DeepShovelItem;
import cn.kurt6.unknown_echoes.item.tool.LandmarkPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoFishingRodItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHatchetItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHoeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoIgniterItem;
import cn.kurt6.unknown_echoes.item.tool.EchoPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShearsItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShovelItem;
import cn.kurt6.unknown_echoes.item.weapon.CrystalSongStaffItem;
import cn.kurt6.unknown_echoes.item.weapon.AshScriptCrossbowItem;
import cn.kurt6.unknown_echoes.item.weapon.DeepMeleeWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.LandmarkWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoBowItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoMeleeWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoMetalTier;
import cn.kurt6.unknown_echoes.item.weapon.EchoOathbladeItem;
import cn.kurt6.unknown_echoes.item.weapon.InvertedBowItem;
import cn.kurt6.unknown_echoes.item.weapon.TideCrossbowItem;
import cn.kurt6.unknown_echoes.item.weapon.TrueSightBladeItem;
import cn.kurt6.unknown_echoes.item.weapon.WindSpearItem;
import cn.kurt6.unknown_echoes.item.weapon.WorldEchoWeaponItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UnknownEchoes.MODID);

    public static final DeferredItem<Item> ECHO_SHARD = ITEMS.register("echo_shard",
            () -> new EchoMaterialItem("echo_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_CORE = ITEMS.register("echo_core",
            () -> new EchoMaterialItem("echo_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> ECHO_CLOTH = ITEMS.register("echo_cloth",
            () -> new EchoMaterialItem("echo_cloth", new Item.Properties()));

    public static final DeferredItem<Item> BLANK_ECHO_PAGE = ITEMS.register("blank_echo_page",
            () -> new EchoMaterialItem("blank_echo_page", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_INK_PAGE = ITEMS.register("echo_ink_page",
            () -> new EchoMaterialItem("echo_ink_page", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_MARK = ITEMS.register("echo_mark",
            () -> new EchoMaterialItem("echo_mark", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TRAVELER_BADGE = ITEMS.register("traveler_badge",
            () -> new EchoMaterialItem("traveler_badge", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<EchoUtilityConsumableItem> ECHO_BANDAGE = ITEMS.register("echo_bandage",
            () -> new EchoUtilityConsumableItem("echo_bandage",
                    EchoUtilityConsumableItem.Kind.ECHO_BANDAGE,
                    new Item.Properties().stacksTo(16)));

    public static final DeferredItem<EchoUtilityConsumableItem> WIND_CHIME_BOTTLE =
            ITEMS.register("wind_chime_bottle", () -> new EchoUtilityConsumableItem("wind_chime_bottle",
                    EchoUtilityConsumableItem.Kind.WIND_CHIME_BOTTLE,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

    public static final DeferredItem<EchoUtilityConsumableItem> TIDE_BREATH_BOTTLE =
            ITEMS.register("tide_breath_bottle", () -> new EchoUtilityConsumableItem("tide_breath_bottle",
                    EchoUtilityConsumableItem.Kind.TIDE_BREATH_BOTTLE,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

    public static final DeferredItem<EchoUtilityConsumableItem> REVEALING_POWDER =
            ITEMS.register("revealing_powder", () -> new EchoUtilityConsumableItem("revealing_powder",
                    EchoUtilityConsumableItem.Kind.REVEALING_POWDER,
                    new Item.Properties().stacksTo(16)));

    public static final DeferredItem<EchoUtilityConsumableItem> MEMORY_ECHO_STONE =
            ITEMS.register("memory_echo_stone", () -> new EchoUtilityConsumableItem("memory_echo_stone",
                    EchoUtilityConsumableItem.Kind.MEMORY_ECHO_STONE,
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<EchoEnergyConsumableItem> FAINT_ECHO_CANDY =
            ITEMS.register("faint_echo_candy", () -> new EchoEnergyConsumableItem(6, 8,
                    energyFoodProperties(16, Rarity.COMMON)));
    public static final DeferredItem<EchoEnergyConsumableItem> ECHO_CANDY =
            ITEMS.register("echo_candy", () -> new EchoEnergyConsumableItem(12, 12,
                    energyFoodProperties(8, Rarity.UNCOMMON)));
    public static final DeferredItem<EchoEnergyConsumableItem> CONDENSED_ECHO_CANDY =
            ITEMS.register("condensed_echo_candy", () -> new EchoEnergyConsumableItem(24, 20,
                    energyFoodProperties(4, Rarity.RARE)));
    public static final DeferredItem<EchoEnergyConsumableItem> PURE_ECHO_CANDY =
            ITEMS.register("pure_echo_candy", () -> new EchoEnergyConsumableItem(48, 32,
                    energyFoodProperties(2, Rarity.EPIC)));

    public static final DeferredItem<DeepEchoFoodItem> ARCHIVE_SPORE_ROLL =
            ITEMS.register("archive_spore_roll", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.ARCHIVE_SPORE_ROLL, "archive_spore_roll",
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(7).saturationModifier(0.75F).alwaysEdible().build())));

    public static final DeferredItem<DeepEchoFoodItem> WEIGHTLESS_BLOOM_TEA =
            ITEMS.register("weightless_bloom_tea", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.WEIGHTLESS_BLOOM_TEA, "weightless_bloom_tea",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(4)
                                    .saturationModifier(0.45F).alwaysEdible().build())));

    public static final DeferredItem<DeepEchoFoodItem> VOIDCAP_STEW =
            ITEMS.register("voidcap_stew", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.VOIDCAP_STEW, "voidcap_stew",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(8)
                                    .saturationModifier(0.9F).alwaysEdible().build())));

    public static final DeferredItem<DeepEchoFoodItem> FOLDED_SPORE_CRISP =
            ITEMS.register("folded_spore_crisp", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.FOLDED_SPORE_CRISP, "folded_spore_crisp",
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(4).saturationModifier(0.45F).alwaysEdible().build())));

    public static final DeferredItem<DeepEchoFoodItem> GRAVITY_BELL_JELLY =
            ITEMS.register("gravity_bell_jelly", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.GRAVITY_BELL_JELLY, "gravity_bell_jelly",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(3)
                                    .saturationModifier(0.25F).alwaysEdible().build())));

    public static final DeferredItem<DeepEchoFoodItem> HUSH_THORN_CANDY =
            ITEMS.register("hush_thorn_candy", () -> new DeepEchoFoodItem(
                    DeepEchoFoodItem.Kind.HUSH_THORN_CANDY, "hush_thorn_candy",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(2)
                                    .saturationModifier(0.15F).alwaysEdible().build())));

    public static final DeferredItem<RealmCuisineItem> FAR_BELL_TRAIL_LOAF =
            ITEMS.register("far_bell_trail_loaf", () -> new RealmCuisineItem(
                    RealmCuisineItem.Kind.FAR_BELL_TRAIL_LOAF, "far_bell_trail_loaf",
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(8).saturationModifier(0.8F).alwaysEdible().build())));

    public static final DeferredItem<RealmCuisineItem> PRISMATIC_CANOPY_TART =
            ITEMS.register("prismatic_canopy_tart", () -> new RealmCuisineItem(
                    RealmCuisineItem.Kind.PRISMATIC_CANOPY_TART, "prismatic_canopy_tart",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(6)
                                    .saturationModifier(0.6F).alwaysEdible().build())));

    public static final DeferredItem<RealmCuisineItem> INTERLAYER_PROVISION_PIE =
            ITEMS.register("interlayer_provision_pie", () -> new RealmCuisineItem(
                    RealmCuisineItem.Kind.INTERLAYER_PROVISION_PIE, "interlayer_provision_pie",
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                            .food(new FoodProperties.Builder().nutrition(10)
                                    .saturationModifier(1.0F).alwaysEdible().build())));

    public static final DeferredItem<HushedPilgrimageBellItem> HUSHED_PILGRIMAGE_BELL =
            ITEMS.register("hushed_pilgrimage_bell", () -> new HushedPilgrimageBellItem(
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<FractureSurveyorItem> FRACTURE_SURVEYOR =
            ITEMS.register("fracture_surveyor", () -> new FractureSurveyorItem(
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<RealmFieldLensItem> REALM_FIELD_LENS =
            ITEMS.register("realm_field_lens", () -> new RealmFieldLensItem(
                    new Item.Properties().durability(320).rarity(Rarity.UNCOMMON)
                            .fireResistant()));

    public static final DeferredItem<Item> MIRROR_LAKE_SHARD = ITEMS.register("mirror_lake_shard",
            () -> new EchoMaterialItem("mirror_lake_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_PEARL = ITEMS.register("tide_pearl",
            () -> new EchoMaterialItem("tide_pearl", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MIRROR_SCALE = ITEMS.register("mirror_scale",
            () -> new EchoMaterialItem("mirror_scale", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_CORE = ITEMS.register("tide_core",
            () -> new EchoMaterialItem("tide_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> TRUE_SIGHT_CORE = ITEMS.register("true_sight_core",
            () -> new EchoMaterialItem("true_sight_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> WATCHER_EYE_SHARD = ITEMS.register("watcher_eye_shard",
            () -> new EchoMaterialItem("watcher_eye_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<SilentPodItem> SILENT_POD = ITEMS.register("silent_pod",
            () -> new SilentPodItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F)
                            .alwaysEdible().build())));

    public static final DeferredItem<Item> SILENCE_MOSS = ITEMS.register("silence_moss",
            () -> new EchoMaterialItem("silence_moss", new Item.Properties()));

    public static final DeferredItem<Item> MOSS_SHELL_PLATE = ITEMS.register("moss_shell_plate",
            () -> new EchoMaterialItem("moss_shell_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CRYSTAL_FEATHER = ITEMS.register("crystal_feather",
            () -> new EchoMaterialItem("crystal_feather", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_METAL_INGOT = ITEMS.register("echo_metal_ingot",
            () -> new EchoMaterialItem("echo_metal_ingot", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> WIND_RUNE_SHARD = ITEMS.register("wind_rune_shard",
            () -> new EchoMaterialItem("wind_rune_shard", new Item.Properties()));

    public static final DeferredItem<Item> TIDE_RUNE_FRAGMENT = ITEMS.register("tide_rune_fragment",
            () -> new EchoMaterialItem("tide_rune_fragment", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_STRING = ITEMS.register("echo_string",
            () -> new EchoMaterialItem("echo_string", new Item.Properties()));

    public static final DeferredItem<Item> ILLUSION_DUST = ITEMS.register("illusion_dust",
            () -> new EchoMaterialItem("illusion_dust", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> POLISHED_MIRROR_DUST = ITEMS.register("polished_mirror_dust",
            () -> new EchoMaterialItem("polished_mirror_dust", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> DECOY_SHARD = ITEMS.register("decoy_shard",
            () -> new EchoMaterialItem("decoy_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_DUST = ITEMS.register("echo_dust",
            () -> new EchoMaterialItem("echo_dust", new Item.Properties()));

    public static final DeferredItem<Item> STORM_COMPASS_PART = ITEMS.register("storm_compass_part",
            () -> new EchoMaterialItem("storm_compass_part", valuableProperties(Rarity.RARE)));

    /** 只是凭据物品:神器本体在玩家 ArtifactData,序号不符时这件物品不可用。 */
    public static final DeferredItem<cn.kurt6.unknown_echoes.item.StormCompassItem> STORM_COMPASS =
            ITEMS.register("storm_compass", () -> new cn.kurt6.unknown_echoes.item.StormCompassItem(
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> TIDE_LANTERN =
            ITEMS.register("tide_lantern", () -> new GenericArtifactItem(ArtifactType.TIDE_LANTERN,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> ECHO_LENS =
            ITEMS.register("echo_lens", () -> new GenericArtifactItem(ArtifactType.ECHO_LENS,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> TIME_HOURGLASS =
            ITEMS.register("time_hourglass", () -> new GenericArtifactItem(ArtifactType.TIME_HOURGLASS,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> DIMENSION_ANCHOR =
            ITEMS.register("dimension_anchor", () -> new GenericArtifactItem(ArtifactType.DIMENSION_ANCHOR,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> RIFT_ANCHOR =
            ITEMS.register("rift_anchor", () -> new GenericArtifactItem(ArtifactType.RIFT_ANCHOR,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ClueMapItem> CLUE_MAP =
            ITEMS.register("clue_map", () -> new cn.kurt6.unknown_echoes.item.ClueMapItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> RECORD_TRACING_PAPER = ITEMS.register("record_tracing_paper",
            () -> new EchoMaterialItem("record_tracing_paper", new Item.Properties()));

    public static final DeferredItem<Item> WIND_ENHANCE_CORE = ITEMS.register("wind_enhance_core",
            () -> new EchoMaterialItem("wind_enhance_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<ResearchRubbingItem> WIND_RESEARCH_RUBBING =
            ITEMS.register("wind_research_rubbing", () -> new ResearchRubbingItem("wind",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TIDE_RESEARCH_RUBBING =
            ITEMS.register("tide_research_rubbing", () -> new ResearchRubbingItem("tide",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TRUE_SIGHT_RESEARCH_RUBBING =
            ITEMS.register("true_sight_research_rubbing", () -> new ResearchRubbingItem("true_sight",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> GRAVITY_RESEARCH_RUBBING =
            ITEMS.register("gravity_research_rubbing", () -> new ResearchRubbingItem("gravity",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> VOID_RESEARCH_RUBBING =
            ITEMS.register("void_research_rubbing", () -> new ResearchRubbingItem("void",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> WIND_FRAGMENT_PAGE =
            ITEMS.register("wind_fragment_page", () -> new ResearchRubbingItem("wind", 2,
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TIDE_FRAGMENT_PAGE =
            ITEMS.register("tide_fragment_page", () -> new ResearchRubbingItem("tide", 2,
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TRUE_SIGHT_FRAGMENT_PAGE =
            ITEMS.register("true_sight_fragment_page", () -> new ResearchRubbingItem("true_sight", 2,
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> GRAVITY_FRAGMENT_PAGE =
            ITEMS.register("gravity_fragment_page", () -> new ResearchRubbingItem("gravity", 2,
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> VOID_FRAGMENT_PAGE =
            ITEMS.register("void_fragment_page", () -> new ResearchRubbingItem("void", 2,
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> INSCRIPTION_RUBBING_PAPER =
            ITEMS.register("inscription_rubbing_paper",
                    () -> new EchoMaterialItem("inscription_rubbing_paper", new Item.Properties()));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.EchoIndexItem> ECHO_INDEX =
            ITEMS.register("echo_index", () -> new cn.kurt6.unknown_echoes.item.EchoIndexItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_UPGRADE_CORE = ITEMS.register("tide_upgrade_core",
            () -> new EchoMaterialItem("tide_upgrade_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> TRUE_SIGHT_UPGRADE_CORE = ITEMS.register("true_sight_upgrade_core",
            () -> new EchoMaterialItem("true_sight_upgrade_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> SILENCE_UPGRADE_CORE = ITEMS.register("silence_upgrade_core",
            () -> new EchoMaterialItem("silence_upgrade_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> CRYSTAL_SONG_UPGRADE_CORE = ITEMS.register("crystal_song_upgrade_core",
            () -> new EchoMaterialItem("crystal_song_upgrade_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> BROKEN_BELL_UPGRADE_CORE = ITEMS.register("broken_bell_upgrade_core",
            () -> new EchoMaterialItem("broken_bell_upgrade_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> ECHO_REALM_INGOT = ITEMS.register("echo_realm_ingot",
            () -> new EchoMaterialItem("echo_realm_ingot", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> ECHO_REALM_CORE = ITEMS.register("echo_realm_core",
            () -> new EchoRealmCoreItem(valuableProperties(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> COLOSSUS_MEMORY_CORE = ITEMS.register("colossus_memory_core",
            () -> new EchoMaterialItem("colossus_memory_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> MEMORY_STONE_SLAB = ITEMS.register("memory_stone_slab",
            () -> new EchoMaterialItem("memory_stone_slab", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BLACK_TIDE_THREAD = ITEMS.register("black_tide_thread",
            () -> new EchoMaterialItem("black_tide_thread", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BROKEN_REFLECTION = ITEMS.register("broken_reflection",
            () -> new EchoMaterialItem("broken_reflection", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MIRROR_GUARDIAN_CORE = ITEMS.register("mirror_guardian_core",
            () -> new EchoMaterialItem("mirror_guardian_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> STORM_SPINDLE = ITEMS.register("storm_spindle",
            () -> new EchoMaterialItem("storm_spindle", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> BROKEN_LANTERN_LENS = ITEMS.register("broken_lantern_lens",
            () -> new EchoMaterialItem("broken_lantern_lens", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> MIRROR_STAFF_CORE = ITEMS.register("mirror_staff_core",
            () -> new EchoMaterialItem("mirror_staff_core", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> SILENT_CANDLE_WICK = ITEMS.register("silent_candle_wick",
            () -> new EchoMaterialItem("silent_candle_wick", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> SONGKEEPER_FORK = ITEMS.register("songkeeper_fork",
            () -> new EchoMaterialItem("songkeeper_fork", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> BELL_CLAPPER = ITEMS.register("bell_clapper",
            () -> new EchoMaterialItem("bell_clapper", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> ARCHIVE_BINDING = ITEMS.register("archive_binding",
            () -> new EchoMaterialItem("archive_binding", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> INVERTED_BOW_LIMB = ITEMS.register("inverted_bow_limb",
            () -> new EchoMaterialItem("inverted_bow_limb", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> ASH_SCRIPT_MECHANISM = ITEMS.register("ash_script_mechanism",
            () -> new EchoMaterialItem("ash_script_mechanism", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<EchoMeleeWeaponItem> ECHO_BROADSWORD =
            ITEMS.register("echo_broadsword", () -> new EchoMeleeWeaponItem(12.5, 1.55,
                    "echo_broadsword", new Item.Properties().durability(3000).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final DeferredItem<EchoMeleeWeaponItem> ECHO_BATTLEAXE =
            ITEMS.register("echo_battleaxe", () -> new EchoMeleeWeaponItem(17.0, 1.0,
                    "echo_battleaxe", new Item.Properties().durability(3100).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<EchoBowItem> ECHO_LONGBOW =
            ITEMS.register("echo_longbow", () -> new EchoBowItem("echo_longbow",
                    new Item.Properties().durability(1225).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final DeferredItem<WindSpearItem> WIND_SPEAR =
            ITEMS.register("wind_spear", () -> new WindSpearItem(13.5, 1.45, 140, 1.65,
                    16.2F, new Item.Properties().durability(3000).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<TideCrossbowItem> TIDE_CROSSBOW =
            ITEMS.register("tide_crossbow", () -> new TideCrossbowItem(4.8, 5,
                    new Item.Properties().stacksTo(1).durability(2600).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<TrueSightBladeItem> TRUE_SIGHT_BLADE =
            ITEMS.register("true_sight_blade", () -> new TrueSightBladeItem(11.0, 2.2, 80,
                    6.5F, new Item.Properties().durability(2950).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<EchoMeleeWeaponItem> SILENCE_SCYTHE =
            ITEMS.register("silence_scythe", () -> new EchoMeleeWeaponItem(14.0, 1.35,
                    "silence_scythe", new Item.Properties().durability(3050).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<EchoMeleeWeaponItem> CRYSTAL_SONG_STAFF =
            ITEMS.register("crystal_song_staff", () -> new CrystalSongStaffItem(13.5, 1.25,
                    "crystal_song_staff", new Item.Properties().durability(2900).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<EchoMeleeWeaponItem> BROKEN_BELL_HAMMER =
            ITEMS.register("broken_bell_hammer", () -> new EchoMeleeWeaponItem(18.5, 0.95,
                    "broken_bell_hammer", new Item.Properties().durability(3200).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<EchoBowItem> DREAM_BLOOM_BOW =
            ITEMS.register("dream_bloom_bow", () -> new EchoBowItem("dream_bloom_bow",
                    new Item.Properties().durability(1200).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<InvertedBowItem> GRAVITY_INVERSION_BOW =
            ITEMS.register("gravity_inversion_bow", () -> new InvertedBowItem(
                    new Item.Properties().durability(3600).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<AshScriptCrossbowItem> ASH_SCRIPT_CROSSBOW =
            ITEMS.register("ash_script_crossbow", () -> new AshScriptCrossbowItem(
                    new Item.Properties().stacksTo(1).durability(3800)
                            .rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<EchoOathbladeItem> ECHO_OATHBLADE =
            ITEMS.register("echo_oathblade", () -> new EchoOathbladeItem(20.0, 1.70,
                    new Item.Properties().durability(4800).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<DeepMeleeWeaponItem> GRAVITY_HAMMER =
            ITEMS.register("gravity_hammer", () -> new DeepMeleeWeaponItem(
                    DeepMeleeWeaponItem.Kind.GRAVITY_HAMMER, 23.0, 0.95, "gravity_hammer",
                    new Item.Properties().durability(4200).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<DeepMeleeWeaponItem> RIFT_DAGGER =
            ITEMS.register("rift_dagger", () -> new DeepMeleeWeaponItem(
                    DeepMeleeWeaponItem.Kind.RIFT_DAGGER, 18.0, 2.20, "rift_dagger",
                    new Item.Properties().durability(4400).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<DeepMeleeWeaponItem> ANCHOR_BLADE =
            ITEMS.register("anchor_blade", () -> new DeepMeleeWeaponItem(
                    DeepMeleeWeaponItem.Kind.ANCHOR_BLADE, 21.0, 1.45, "anchor_blade",
                    new Item.Properties().durability(4400).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<DeepMeleeWeaponItem> FOLDED_GLAIVE =
            ITEMS.register("folded_glaive", () -> new DeepMeleeWeaponItem(
                    DeepMeleeWeaponItem.Kind.FOLDED_GLAIVE, 20.0, 1.55, "folded_glaive",
                    new Item.Properties().durability(4300).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<LandmarkWeaponItem> PALIMPSEST_SABER =
            ITEMS.register("palimpsest_saber", () -> new LandmarkWeaponItem(
                    LandmarkWeaponItem.Kind.PALIMPSEST_SABER,
                    24.0D, 1.55D, "palimpsest_saber",
                    new Item.Properties().durability(5000).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<LandmarkWeaponItem> ORRERY_LANCE =
            ITEMS.register("orrery_lance", () -> new LandmarkWeaponItem(
                    LandmarkWeaponItem.Kind.ORRERY_LANCE,
                    23.0D, 1.70D, "orrery_lance",
                    new Item.Properties().durability(4900).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<WorldEchoWingsItem> WORLD_ECHO_WINGS =
            ITEMS.register("world_echo_wings", () -> new WorldEchoWingsItem(
                    unbreakableEndgameArmorProperties(ArmorItem.Type.CHESTPLATE)
                            .attributes(WorldEchoWingsItem.createAttributes())));

    public static final DeferredItem<RiftwingRelayItem> RIFTWING_RELAY =
            ITEMS.register("riftwing_relay", () -> new RiftwingRelayItem(
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<PerpetualEchoCoreItem> PERPETUAL_ECHO_CORE =
            ITEMS.register("perpetual_echo_core", () -> new PerpetualEchoCoreItem(
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<WorldEchoWeaponItem> WORLD_RIFT_GREATSWORD =
            ITEMS.register("world_rift_greatsword", () -> new WorldEchoWeaponItem(
                    WorldEchoWeaponItem.Kind.WORLD_RIFT_GREATSWORD,
                    30.0D, 1.15D, "world_rift_greatsword",
                    unbreakableEndgameProperties()));

    public static final DeferredItem<WorldEchoWeaponItem> FIVEFOLD_STAR_SPEAR =
            ITEMS.register("fivefold_star_spear", () -> new WorldEchoWeaponItem(
                    WorldEchoWeaponItem.Kind.FIVEFOLD_STAR_SPEAR,
                    27.0D, 1.65D, "fivefold_star_spear",
                    unbreakableEndgameProperties()));

    public static final DeferredItem<EchoPickaxeItem> ECHO_PICKAXE =
            ITEMS.register("echo_pickaxe", () -> new EchoPickaxeItem(EchoMetalTier.INSTANCE,
                    toolProperties(3250, 1.5F, -2.8F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoShovelItem> ECHO_SHOVEL =
            ITEMS.register("echo_shovel", () -> new EchoShovelItem(EchoMetalTier.INSTANCE,
                    toolProperties(3250, 1.5F, -3.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoHatchetItem> ECHO_HATCHET =
            ITEMS.register("echo_hatchet", () -> new EchoHatchetItem(EchoMetalTier.INSTANCE,
                    toolProperties(3250, 5.5F, -3.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoHoeItem> ECHO_HOE =
            ITEMS.register("echo_hoe", () -> new EchoHoeItem(EchoMetalTier.INSTANCE,
                    toolProperties(3250, -1.0F, 0.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoShearsItem> ECHO_SHEARS =
            ITEMS.register("echo_shears", () -> new EchoShearsItem(new Item.Properties()
                    .durability(960).rarity(Rarity.UNCOMMON).fireResistant()
                    .component(DataComponents.TOOL, ShearsItem.createToolProperties())));

    public static final DeferredItem<EchoFishingRodItem> ECHO_FISHING_ROD =
            ITEMS.register("echo_fishing_rod", () -> new EchoFishingRodItem(new Item.Properties()
                    .durability(720).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final DeferredItem<EchoBrushItem> ECHO_BRUSH =
            ITEMS.register("echo_brush", () -> new EchoBrushItem(new Item.Properties()
                    .durability(480).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final DeferredItem<EchoIgniterItem> ECHO_IGNITER =
            ITEMS.register("echo_igniter", () -> new EchoIgniterItem(new Item.Properties()
                    .rarity(Rarity.UNCOMMON).fireResistant()
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    public static final DeferredItem<DeepPickaxeItem> DEEP_ALLOY_PICKAXE =
            ITEMS.register("deep_alloy_pickaxe", () -> new DeepPickaxeItem(DeepAlloyTier.INSTANCE,
                    deepToolProperties(1.5F, -2.8F)));
    public static final DeferredItem<DeepShovelItem> DEEP_ALLOY_SHOVEL =
            ITEMS.register("deep_alloy_shovel", () -> new DeepShovelItem(DeepAlloyTier.INSTANCE,
                    deepToolProperties(1.5F, -3.0F)));
    public static final DeferredItem<DeepAxeItem> DEEP_ALLOY_AXE =
            ITEMS.register("deep_alloy_axe", () -> new DeepAxeItem(DeepAlloyTier.INSTANCE,
                    deepToolProperties(5.5F, -3.0F)));
    public static final DeferredItem<DeepHoeItem> DEEP_ALLOY_HOE =
            ITEMS.register("deep_alloy_hoe", () -> new DeepHoeItem(DeepAlloyTier.INSTANCE,
                    deepToolProperties(-1.0F, 0.0F)));

    public static final DeferredItem<LandmarkPickaxeItem> ARCHIVE_CHISEL =
            ITEMS.register("archive_chisel", () -> new LandmarkPickaxeItem(
                    LandmarkPickaxeItem.Kind.ARCHIVE_CHISEL, "archive_chisel",
                    DeepAlloyTier.INSTANCE, landmarkToolProperties(2.0F, -2.7F)));

    public static final DeferredItem<LandmarkPickaxeItem> ORRERY_CALIBRATOR =
            ITEMS.register("orrery_calibrator", () -> new LandmarkPickaxeItem(
                    LandmarkPickaxeItem.Kind.ORRERY_CALIBRATOR, "orrery_calibrator",
                    DeepAlloyTier.INSTANCE, landmarkToolProperties(2.0F, -2.7F)));

    public static final DeferredItem<EquipmentBlueprintItem> WIND_SPEAR_BLUEPRINT =
            ITEMS.register("wind_spear_blueprint", () -> new EquipmentBlueprintItem("wind_spear",
                    valuableProperties(16, Rarity.RARE)));

    public static final DeferredItem<EquipmentBlueprintItem> TIDE_CROSSBOW_BLUEPRINT =
            ITEMS.register("tide_crossbow_blueprint", () -> new EquipmentBlueprintItem("tide_crossbow",
                    valuableProperties(16, Rarity.RARE)));

    public static final DeferredItem<EquipmentBlueprintItem> TRUE_SIGHT_BLADE_BLUEPRINT =
            ITEMS.register("true_sight_blade_blueprint", () -> new EquipmentBlueprintItem("true_sight_blade",
                    valuableProperties(16, Rarity.RARE)));

    public static final DeferredItem<EchoKeyItem> ECHO_KEY = ITEMS.register("echo_key",
            () -> new EchoKeyItem(valuableProperties(16, Rarity.RARE)));

    public static final DeferredItem<EchoProjectorItem> ECHO_PROJECTOR = ITEMS.register("echo_projector",
            () -> new EchoProjectorItem(valuableProperties(1, Rarity.RARE)));

    public static final DeferredItem<AncientPageItem> ANCIENT_PAGE = ITEMS.register("ancient_page",
            () -> new AncientPageItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                    .component(ModDataComponents.PAGE_ID.get(), 1)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> WIND_RESONANCE_TOKEN =
            ITEMS.register("wind_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO,
                    valuableProperties(1, Rarity.RARE)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> TIDE_RESONANCE_TOKEN =
            ITEMS.register("tide_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TIDE_ECHO,
                    valuableProperties(1, Rarity.RARE)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> TRUE_SIGHT_RESONANCE_TOKEN =
            ITEMS.register("true_sight_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO,
                    valuableProperties(1, Rarity.RARE)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> GRAVITY_RESONANCE_TOKEN =
            ITEMS.register("gravity_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.GRAVITY_ECHO,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> VOID_RESONANCE_TOKEN =
            ITEMS.register("void_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.VOID_ECHO,
                    valuableProperties(1, Rarity.EPIC)));

    public static final DeferredItem<Item> ECHO_FIBER = ITEMS.register("echo_fiber",
            () -> new EchoMaterialItem("echo_fiber", new Item.Properties()));

    public static final DeferredItem<Item> GLOW_HIDE = ITEMS.register("glow_hide",
            () -> new EchoMaterialItem("glow_hide", new Item.Properties()));

    public static final DeferredItem<Item> FOLDED_CARAPACE = ITEMS.register("folded_carapace",
            () -> new EchoMaterialItem("folded_carapace", new Item.Properties()));

    public static final DeferredItem<Item> SPACE_ANCHOR_SHARD = ITEMS.register("space_anchor_shard",
            () -> new EchoMaterialItem("space_anchor_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> DEEP_ORE_SCRAP = ITEMS.register("deep_ore_scrap",
            () -> new EchoMaterialItem("deep_ore_scrap", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ASHBACK_MOSS_MEAT = ITEMS.register("ashback_moss_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.45F).build())));

    public static final DeferredItem<Item> HUSHLIGHT_GEL = ITEMS.register("hushlight_gel",
            () -> new EchoMaterialItem("hushlight_gel", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> VOID_RIFT_SHARD = ITEMS.register("void_rift_shard",
            () -> new EchoMaterialItem("void_rift_shard", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> DEEP_ALLOY_INGOT = ITEMS.register("deep_alloy_ingot",
            () -> new EchoMaterialItem("deep_alloy_ingot", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> PALIMPSEST_LEAF = ITEMS.register("palimpsest_leaf",
            () -> new EchoMaterialItem("palimpsest_leaf",
                    valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> ORRERY_PRISM_CORE = ITEMS.register("orrery_prism_core",
            () -> new EchoMaterialItem("orrery_prism_core",
                    valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> GRAVITY_CORE_FRAGMENT = ITEMS.register("gravity_core_fragment",
            () -> new EchoMaterialItem("gravity_core_fragment", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> SPACE_ANCHOR_ROD = ITEMS.register("space_anchor_rod",
            () -> new EchoMaterialItem("space_anchor_rod", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> WORLD_ECHO_RESIDUE = ITEMS.register("world_echo_residue",
            () -> new EchoMaterialItem("world_echo_residue", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> RESONANCE_ALLOY_INGOT = ITEMS.register("resonance_alloy_ingot",
            () -> new EchoMaterialItem("resonance_alloy_ingot", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> LAST_RECORD_FRAGMENT = ITEMS.register("last_record_fragment",
            () -> new EchoMaterialItem("last_record_fragment",
                    valuableProperties(16, Rarity.EPIC)));

    public static final DeferredItem<Item> CRYSTAL_SONG_SHARD = ITEMS.register("crystal_song_shard",
            () -> new EchoMaterialItem("crystal_song_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> RESONANT_CRYSTAL_NOTE = ITEMS.register("resonant_crystal_note",
            () -> new EchoMaterialItem("resonant_crystal_note", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> SKY_QUARTZ = ITEMS.register("sky_quartz",
            () -> new EchoMaterialItem("sky_quartz", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BROKEN_BELL_GEAR = ITEMS.register("broken_bell_gear",
            () -> new EchoMaterialItem("broken_bell_gear", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> RUSTED_TIME_GEAR = ITEMS.register("rusted_time_gear",
            () -> new EchoMaterialItem("rusted_time_gear", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> DREAM_NECTAR = ITEMS.register("dream_nectar",
            () -> new EchoMaterialItem("dream_nectar", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<SimpleEchoFoodItem> STAR_MIST_PETAL = ITEMS.register("star_mist_petal",
            () -> new SimpleEchoFoodItem("star_mist_petal", 16,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> MIRROR_LOTUS_ROOT = ITEMS.register("mirror_lotus_root",
            () -> new SimpleEchoFoodItem("mirror_lotus_root", 20,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(3).saturationModifier(0.35F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<Item> ECHO_AMBER = ITEMS.register("echo_amber",
            () -> new EchoMaterialItem("echo_amber", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_SALT = ITEMS.register("tide_salt",
            () -> new EchoMaterialItem("tide_salt", new Item.Properties()));

    public static final DeferredItem<Item> WIND_SAC_MEMBRANE = ITEMS.register("wind_sac_membrane",
            () -> new EchoMaterialItem("wind_sac_membrane", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> WINDWOVEN_THREAD = ITEMS.register("windwoven_thread",
            () -> new EchoMaterialItem("windwoven_thread", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MIRROR_TIDE_ALLOY_PLATE = ITEMS.register("mirror_tide_alloy_plate",
            () -> new EchoMaterialItem("mirror_tide_alloy_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ILLUSION_PRISM = ITEMS.register("illusion_prism",
            () -> new EchoMaterialItem("illusion_prism", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> SILENT_RESIN = ITEMS.register("silent_resin",
            () -> new EchoMaterialItem("silent_resin", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CRYSTAL_RESONANCE_ROD = ITEMS.register("crystal_resonance_rod",
            () -> new EchoMaterialItem("crystal_resonance_rod", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BROKEN_CLOCKWORK_PLATE = ITEMS.register("broken_clockwork_plate",
            () -> new EchoMaterialItem("broken_clockwork_plate", valuableProperties(Rarity.RARE)));

    public static final DeferredItem<Item> ATTUNED_ECHO_GEM = ITEMS.register("attuned_echo_gem",
            () -> new EchoMaterialItem("attuned_echo_gem", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_WEAPON_FRAME = ITEMS.register("echo_weapon_frame",
            () -> new EchoMaterialItem("echo_weapon_frame", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_ARMOR_PLATE = ITEMS.register("echo_armor_plate",
            () -> new EchoMaterialItem("echo_armor_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> OATHBOUND_ALLOY = ITEMS.register("oathbound_alloy",
            () -> new EchoMaterialItem("oathbound_alloy", valuableProperties(Rarity.EPIC)));

    public static final DeferredItem<Item> ECHO_BERRY = ITEMS.register("echo_berry",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3).saturationModifier(0.3F).build())));

    public static final DeferredItem<Item> GLOW_FRUIT = ITEMS.register("glow_fruit",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.45F).build())));

    public static final DeferredItem<WorldtreeFruitItem> WORLDTREE_FRUIT =
            ITEMS.register("worldtree_fruit",
                    () -> new WorldtreeFruitItem(new Item.Properties().stacksTo(16)
                            .rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder()
                                    .nutrition(4).saturationModifier(0.6F).alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> WIND_CRISP = ITEMS.register("wind_crisp",
            () -> new SimpleEchoFoodItem("wind_crisp", 20,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.45F)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> MOSS_HONEY_STEW = ITEMS.register("moss_honey_stew",
            () -> new SimpleEchoFoodItem("moss_honey_stew", Items.BOWL, 32, UseAnim.EAT,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 300, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 400, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> TIDE_SALTED_FISH = ITEMS.register("tide_salted_fish",
            () -> new SimpleEchoFoodItem("tide_salted_fish", 24,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 900, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> DREAM_NECTAR_TART = ITEMS.register("dream_nectar_tart",
            () -> new SimpleEchoFoodItem("dream_nectar_tart", 28,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.7F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 240, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 600, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> GLOW_FRUIT_JAM = ITEMS.register("glow_fruit_jam",
            () -> new SimpleEchoFoodItem("glow_fruit_jam", Items.GLASS_BOTTLE, 24, UseAnim.DRINK,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.65F)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> TIDE_GLASS_EEL = ITEMS.register("tide_glass_eel",
            () -> new SimpleEchoFoodItem("tide_glass_eel", 20,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<Item> PEARL_CLAM = ITEMS.register("pearl_clam",
            () -> new EchoMaterialItem("pearl_clam", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<SimpleEchoFoodItem> TIDE_KELP_WRAP = ITEMS.register("tide_kelp_wrap",
            () -> new SimpleEchoFoodItem("tide_kelp_wrap", 28,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(7).saturationModifier(0.75F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 1800, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 300, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> MIRROR_TAIL_FISH = ITEMS.register("mirror_tail_fish",
            () -> new SimpleEchoFoodItem("mirror_tail_fish", 20,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> ECHO_CUTTLEFISH_STRIP = ITEMS.register("echo_cuttlefish_strip",
            () -> new SimpleEchoFoodItem("echo_cuttlefish_strip", 20,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.45F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<SimpleEchoFoodItem> PEARLBACK_FILLET = ITEMS.register("pearlback_fillet",
            () -> new SimpleEchoFoodItem("pearlback_fillet", 24,
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 300, 0), 1.0F)
                    .alwaysEdible().build())));

    public static final DeferredItem<MobBucketItem> MIRROR_TAIL_FISH_BUCKET = ITEMS.register("mirror_tail_fish_bucket",
            () -> new MobBucketItem(ModEntities.MIRROR_TAIL_FISH.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<MobBucketItem> ECHO_CUTTLEFISH_BUCKET = ITEMS.register("echo_cuttlefish_bucket",
            () -> new MobBucketItem(ModEntities.ECHO_CUTTLEFISH.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<MobBucketItem> PEARLBACK_RAY_BUCKET = ITEMS.register("pearlback_ray_bucket",
            () -> new MobBucketItem(ModEntities.PEARLBACK_RAY.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<EchoSetArmorItem> EXPLORER_HELMET = ITEMS.register("explorer_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.HELMET, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_CHESTPLATE = ITEMS.register("explorer_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.CHESTPLATE, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_LEGGINGS = ITEMS.register("explorer_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.LEGGINGS, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_BOOTS = ITEMS.register("explorer_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.BOOTS, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));

    public static final DeferredItem<EchoSetArmorItem> LISTENER_HELMET = ITEMS.register("listener_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.HELMET, "listener",
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(23)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_CHESTPLATE = ITEMS.register("listener_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.CHESTPLATE, "listener",
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(23)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_LEGGINGS = ITEMS.register("listener_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.LEGGINGS, "listener",
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(23)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_BOOTS = ITEMS.register("listener_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.BOOTS, "listener",
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(23)).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_HELMET = ITEMS.register("echo_traveler_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.HELMET, "echo_traveler",
                    armorProperties(ArmorItem.Type.HELMET, 45, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_CHESTPLATE = ITEMS.register("echo_traveler_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.CHESTPLATE, "echo_traveler",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 45, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_LEGGINGS = ITEMS.register("echo_traveler_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.LEGGINGS, "echo_traveler",
                    armorProperties(ArmorItem.Type.LEGGINGS, 45, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_BOOTS = ITEMS.register("echo_traveler_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.BOOTS, "echo_traveler",
                    armorProperties(ArmorItem.Type.BOOTS, 45, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_HELMET = ITEMS.register("wind_walker_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.HELMET, "wind_walker",
                    armorProperties(ArmorItem.Type.HELMET, 50, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_CHESTPLATE = ITEMS.register("wind_walker_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.CHESTPLATE, "wind_walker",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 50, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_LEGGINGS = ITEMS.register("wind_walker_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.LEGGINGS, "wind_walker",
                    armorProperties(ArmorItem.Type.LEGGINGS, 50, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_BOOTS = ITEMS.register("wind_walker_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.BOOTS, "wind_walker",
                    armorProperties(ArmorItem.Type.BOOTS, 50, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_HELMET = ITEMS.register("tide_stalker_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.HELMET, "tide_stalker",
                    armorProperties(ArmorItem.Type.HELMET, 51, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_CHESTPLATE = ITEMS.register("tide_stalker_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.CHESTPLATE, "tide_stalker",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 51, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_LEGGINGS = ITEMS.register("tide_stalker_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.LEGGINGS, "tide_stalker",
                    armorProperties(ArmorItem.Type.LEGGINGS, 51, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_BOOTS = ITEMS.register("tide_stalker_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.BOOTS, "tide_stalker",
                    armorProperties(ArmorItem.Type.BOOTS, 51, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_HELMET = ITEMS.register("true_sight_shadow_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.HELMET, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.HELMET, 49, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_CHESTPLATE = ITEMS.register("true_sight_shadow_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.CHESTPLATE, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 49, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_LEGGINGS = ITEMS.register("true_sight_shadow_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.LEGGINGS, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.LEGGINGS, 49, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_BOOTS = ITEMS.register("true_sight_shadow_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.BOOTS, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.BOOTS, 49, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_HELMET = ITEMS.register("silent_watch_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.HELMET, "silent_watch",
                    armorProperties(ArmorItem.Type.HELMET, 53, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_CHESTPLATE = ITEMS.register("silent_watch_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.CHESTPLATE, "silent_watch",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 53, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_LEGGINGS = ITEMS.register("silent_watch_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.LEGGINGS, "silent_watch",
                    armorProperties(ArmorItem.Type.LEGGINGS, 53, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_BOOTS = ITEMS.register("silent_watch_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.BOOTS, "silent_watch",
                    armorProperties(ArmorItem.Type.BOOTS, 53, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_HELMET = ITEMS.register("echo_oath_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.HELMET, "echo_oath",
                    armorProperties(ArmorItem.Type.HELMET, 64, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_CHESTPLATE = ITEMS.register("echo_oath_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.CHESTPLATE, "echo_oath",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 64, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_LEGGINGS = ITEMS.register("echo_oath_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.LEGGINGS, "echo_oath",
                    armorProperties(ArmorItem.Type.LEGGINGS, 64, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_BOOTS = ITEMS.register("echo_oath_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.BOOTS, "echo_oath",
                    armorProperties(ArmorItem.Type.BOOTS, 64, Rarity.EPIC)));

    public static final DeferredItem<EchoSetArmorItem> ABYSS_WALKER_HELMET = ITEMS.register("abyss_walker_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.ABYSS_WALKER, ArmorItem.Type.HELMET, "abyss_walker",
                    armorProperties(ArmorItem.Type.HELMET, 72, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ABYSS_WALKER_CHESTPLATE = ITEMS.register("abyss_walker_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.ABYSS_WALKER, ArmorItem.Type.CHESTPLATE, "abyss_walker",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 72, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ABYSS_WALKER_LEGGINGS = ITEMS.register("abyss_walker_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.ABYSS_WALKER, ArmorItem.Type.LEGGINGS, "abyss_walker",
                    armorProperties(ArmorItem.Type.LEGGINGS, 72, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ABYSS_WALKER_BOOTS = ITEMS.register("abyss_walker_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.ABYSS_WALKER, ArmorItem.Type.BOOTS, "abyss_walker",
                    armorProperties(ArmorItem.Type.BOOTS, 72, Rarity.EPIC)));

    public static final DeferredItem<EchoSetArmorItem> RESONATOR_HELMET = ITEMS.register("resonator_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.RESONATOR, ArmorItem.Type.HELMET, "resonator",
                    unbreakableEndgameArmorProperties(ArmorItem.Type.HELMET)));
    public static final DeferredItem<EchoSetArmorItem> RESONATOR_CHESTPLATE = ITEMS.register("resonator_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.RESONATOR, ArmorItem.Type.CHESTPLATE, "resonator",
                    unbreakableEndgameArmorProperties(ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<EchoSetArmorItem> RESONATOR_LEGGINGS = ITEMS.register("resonator_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.RESONATOR, ArmorItem.Type.LEGGINGS, "resonator",
                    unbreakableEndgameArmorProperties(ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<EchoSetArmorItem> RESONATOR_BOOTS = ITEMS.register("resonator_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.RESONATOR, ArmorItem.Type.BOOTS, "resonator",
                    unbreakableEndgameArmorProperties(ArmorItem.Type.BOOTS)));

    public static final DeferredItem<BlockItem> ECHO_STONE_BRICKS = ITEMS.registerSimpleBlockItem("echo_stone_bricks", ModBlocks.ECHO_STONE_BRICKS);
    public static final DeferredItem<BlockItem> CRACKED_ECHO_STONE_BRICKS = ITEMS.registerSimpleBlockItem("cracked_echo_stone_bricks", ModBlocks.CRACKED_ECHO_STONE_BRICKS);
    public static final DeferredItem<BlockItem> ECHO_RUNE_BRICKS = ITEMS.registerSimpleBlockItem("echo_rune_bricks", ModBlocks.ECHO_RUNE_BRICKS);
    public static final DeferredItem<BlockItem> RESONANCE_BEACON = ITEMS.registerSimpleBlockItem("resonance_beacon", ModBlocks.RESONANCE_BEACON);
    public static final DeferredItem<BlockItem> ABYSS_BEACON = ITEMS.registerSimpleBlockItem("abyss_beacon", ModBlocks.ABYSS_BEACON);
    public static final DeferredItem<BlockItem> DEEP_ROUTE_NODE = ITEMS.registerSimpleBlockItem("deep_route_node", ModBlocks.DEEP_ROUTE_NODE);
    public static final DeferredItem<BlockItem> DEEP_CALIBRATION_NODE = ITEMS.registerSimpleBlockItem(
            "deep_calibration_node", ModBlocks.DEEP_CALIBRATION_NODE);
    public static final DeferredItem<BlockItem> GRAVITY_CORE_ALTAR = ITEMS.registerSimpleBlockItem(
            "gravity_core_altar", ModBlocks.GRAVITY_CORE_ALTAR);
    public static final DeferredItem<BlockItem> GRAVITY_FOLD_DOOR = ITEMS.registerSimpleBlockItem(
            "gravity_fold_door", ModBlocks.GRAVITY_FOLD_DOOR);
    public static final DeferredItem<BlockItem> GRAVITY_FIELD_CORE = ITEMS.registerSimpleBlockItem(
            "gravity_field_core", ModBlocks.GRAVITY_FIELD_CORE);
    public static final DeferredItem<BlockItem> SPACE_ANCHOR = ITEMS.registerSimpleBlockItem(
            "space_anchor", ModBlocks.SPACE_ANCHOR);
    public static final DeferredItem<BlockItem> VOID_STABILIZER = ITEMS.registerSimpleBlockItem(
            "void_stabilizer", ModBlocks.VOID_STABILIZER);
    public static final DeferredItem<BlockItem> VOID_CORE_ALTAR = ITEMS.registerSimpleBlockItem(
            "void_core_altar", ModBlocks.VOID_CORE_ALTAR);
    public static final DeferredItem<BlockItem> WORLD_RESONANCE_ALTAR = ITEMS.registerSimpleBlockItem(
            "world_resonance_altar", ModBlocks.WORLD_RESONANCE_ALTAR);
    public static final DeferredItem<BlockItem> ENDING_RECORD_TABLE = ITEMS.registerSimpleBlockItem(
            "ending_record_table", ModBlocks.ENDING_RECORD_TABLE);
    public static final DeferredItem<BlockItem> DEEP_PROVISION_BRAZIER = ITEMS.registerSimpleBlockItem(
            "deep_provision_brazier", ModBlocks.DEEP_PROVISION_BRAZIER);
    public static final DeferredItem<BlockItem> COMMUNAL_ECHO_TABLE = ITEMS.registerSimpleBlockItem(
            "communal_echo_table", ModBlocks.COMMUNAL_ECHO_TABLE);
    public static final DeferredItem<BlockItem> REALM_SERVICE_PLINTH = ITEMS.registerSimpleBlockItem(
            "realm_service_plinth", ModBlocks.REALM_SERVICE_PLINTH);
    public static final DeferredItem<BlockItem> REALM_HABITAT_CHIME = ITEMS.registerSimpleBlockItem(
            "realm_habitat_chime", ModBlocks.REALM_HABITAT_CHIME);
    public static final DeferredItem<BlockItem> PROJECTION_RESONANCE_PLATE = ITEMS.registerSimpleBlockItem(
            "projection_resonance_plate", ModBlocks.PROJECTION_RESONANCE_PLATE);
    public static final DeferredItem<BlockItem> WIND_DOOR = ITEMS.registerSimpleBlockItem("wind_door", ModBlocks.WIND_DOOR);
    public static final DeferredItem<BlockItem> MEMORY_PILLAR = ITEMS.registerSimpleBlockItem("memory_pillar", ModBlocks.MEMORY_PILLAR);
    public static final DeferredItem<BlockItem> REWARD_ALTAR = ITEMS.registerSimpleBlockItem("reward_altar", ModBlocks.REWARD_ALTAR);
    public static final DeferredItem<BlockItem> HIDDEN_RUNE_BRICKS = ITEMS.registerSimpleBlockItem("hidden_rune_bricks", ModBlocks.HIDDEN_RUNE_BRICKS);
    public static final DeferredItem<BlockItem> SEQUENCE_RUNE = ITEMS.registerSimpleBlockItem("sequence_rune", ModBlocks.SEQUENCE_RUNE);
    public static final DeferredItem<BlockItem> PUZZLE_CORE = ITEMS.registerSimpleBlockItem("puzzle_core", ModBlocks.PUZZLE_CORE);
    public static final DeferredItem<BlockItem> CHIME_ECHO_CORE = ITEMS.registerSimpleBlockItem("chime_echo_core", ModBlocks.CHIME_ECHO_CORE);
    public static final DeferredItem<BlockItem> FOLDPAGE_PAIR_CORE = ITEMS.registerSimpleBlockItem("foldpage_pair_core", ModBlocks.FOLDPAGE_PAIR_CORE);
    public static final DeferredItem<BlockItem> FOLDPAGE_TILE = ITEMS.registerSimpleBlockItem("foldpage_tile", ModBlocks.FOLDPAGE_TILE);
    public static final DeferredItem<BlockItem> STEP_RUNE = ITEMS.registerSimpleBlockItem("step_rune", ModBlocks.STEP_RUNE);
    public static final DeferredItem<BlockItem> SEALED_STONE = ITEMS.registerSimpleBlockItem("sealed_stone", ModBlocks.SEALED_STONE);
    public static final DeferredItem<BlockItem> ANCHORED_SEALED_STONE = ITEMS.registerSimpleBlockItem("anchored_sealed_stone", ModBlocks.ANCHORED_SEALED_STONE);
    public static final DeferredItem<BlockItem> ECHO_LOG = ITEMS.registerSimpleBlockItem("echo_log", ModBlocks.ECHO_LOG);
    public static final DeferredItem<BlockItem> ECHO_PLANKS = ITEMS.registerSimpleBlockItem("echo_planks", ModBlocks.ECHO_PLANKS);
    public static final DeferredItem<BlockItem> ECHO_LEAVES = ITEMS.registerSimpleBlockItem("echo_leaves", ModBlocks.ECHO_LEAVES);
    public static final DeferredItem<BlockItem> WHISPERING_LOG = ITEMS.registerSimpleBlockItem("whispering_log", ModBlocks.WHISPERING_LOG);
    public static final DeferredItem<BlockItem> WHISPERING_PLANKS = ITEMS.registerSimpleBlockItem("whispering_planks", ModBlocks.WHISPERING_PLANKS);
    public static final DeferredItem<BlockItem> WHISPERING_LATTICE = ITEMS.registerSimpleBlockItem("whispering_lattice", ModBlocks.WHISPERING_LATTICE);
    public static final DeferredItem<BlockItem> WHISPERING_LEAVES = ITEMS.registerSimpleBlockItem("whispering_leaves", ModBlocks.WHISPERING_LEAVES);
    public static final DeferredItem<BlockItem> TIDEWOOD_LOG = ITEMS.registerSimpleBlockItem("tidewood_log", ModBlocks.TIDEWOOD_LOG);
    public static final DeferredItem<BlockItem> TIDEWOOD_PLANKS = ITEMS.registerSimpleBlockItem("tidewood_planks", ModBlocks.TIDEWOOD_PLANKS);
    public static final DeferredItem<BlockItem> TIDEWOOD_LEAVES = ITEMS.registerSimpleBlockItem("tidewood_leaves", ModBlocks.TIDEWOOD_LEAVES);
    public static final DeferredItem<BlockItem> GLOW_GRASS = ITEMS.registerSimpleBlockItem("glow_grass", ModBlocks.GLOW_GRASS);
    public static final DeferredItem<BlockItem> ECHO_FLOWER = ITEMS.registerSimpleBlockItem("echo_flower", ModBlocks.ECHO_FLOWER);
    public static final DeferredItem<BlockItem> GLOW_FERN = ITEMS.registerSimpleBlockItem("glow_fern", ModBlocks.GLOW_FERN);
    public static final DeferredItem<BlockItem> MIRROR_STONE = ITEMS.registerSimpleBlockItem("mirror_stone", ModBlocks.MIRROR_STONE);
    public static final DeferredItem<BlockItem> MIRROR_STONE_BRICKS = ITEMS.registerSimpleBlockItem("mirror_stone_bricks", ModBlocks.MIRROR_STONE_BRICKS);
    public static final DeferredItem<BlockItem> TIDE_RUNE = ITEMS.registerSimpleBlockItem("tide_rune", ModBlocks.TIDE_RUNE);
    public static final DeferredItem<BlockItem> TIDE_PILLAR = ITEMS.registerSimpleBlockItem("tide_pillar", ModBlocks.TIDE_PILLAR);
    public static final DeferredItem<BlockItem> TIDE_CORE_ALTAR = ITEMS.registerSimpleBlockItem("tide_core_altar", ModBlocks.TIDE_CORE_ALTAR);
    public static final DeferredItem<BlockItem> MIRROR_SIGIL = ITEMS.registerSimpleBlockItem("mirror_sigil", ModBlocks.MIRROR_SIGIL);
    public static final DeferredItem<BlockItem> TRUE_SIGHT_ALTAR = ITEMS.registerSimpleBlockItem("true_sight_altar", ModBlocks.TRUE_SIGHT_ALTAR);
    public static final DeferredItem<BlockItem> MUFFLE_MOSS = ITEMS.registerSimpleBlockItem("muffle_moss", ModBlocks.MUFFLE_MOSS);
    public static final DeferredItem<BlockItem> SILENT_POD_BUSH = ITEMS.registerSimpleBlockItem("silent_pod_bush", ModBlocks.SILENT_POD_BUSH);
    public static final DeferredItem<BlockItem> WIND_ETCHED_STONE = ITEMS.registerSimpleBlockItem("wind_etched_stone", ModBlocks.WIND_ETCHED_STONE);
    public static final DeferredItem<BlockItem> OBSERVATORY_CORE = ITEMS.registerSimpleBlockItem("observatory_core", ModBlocks.OBSERVATORY_CORE);
    public static final DeferredItem<BlockItem> RESONANCE_CANDLE = ITEMS.registerSimpleBlockItem("resonance_candle", ModBlocks.RESONANCE_CANDLE);
    public static final DeferredItem<BlockItem> TIDE_BUOY = ITEMS.registerSimpleBlockItem("tide_buoy", ModBlocks.TIDE_BUOY);
    public static final DeferredItem<BlockItem> TIDE_RUNE_SEAT = ITEMS.registerSimpleBlockItem("tide_rune_seat", ModBlocks.TIDE_RUNE_SEAT);
    public static final DeferredItem<BlockItem> CRACKED_MIRROR_BRICKS = ITEMS.registerSimpleBlockItem("cracked_mirror_bricks", ModBlocks.CRACKED_MIRROR_BRICKS);
    public static final DeferredItem<BlockItem> ARTIFACT_RECORD_TABLE = ITEMS.registerSimpleBlockItem("artifact_record_table", ModBlocks.ARTIFACT_RECORD_TABLE);
    public static final DeferredItem<BlockItem> ARTIFACT_TUNING_TABLE = ITEMS.registerSimpleBlockItem("artifact_tuning_table", ModBlocks.ARTIFACT_TUNING_TABLE);
    public static final DeferredItem<BlockItem> WIND_CURRENT_PLATFORM = ITEMS.registerSimpleBlockItem("wind_current_platform", ModBlocks.WIND_CURRENT_PLATFORM);
    public static final DeferredItem<BlockItem> TIDE_SENSOR_DOOR = ITEMS.registerSimpleBlockItem("tide_sensor_door", ModBlocks.TIDE_SENSOR_DOOR);
    public static final DeferredItem<BlockItem> TRUE_SIGHT_STELE = ITEMS.registerSimpleBlockItem("true_sight_stele", ModBlocks.TRUE_SIGHT_STELE);
    public static final DeferredItem<BlockItem> TRUE_SIGHT_CHEST = ITEMS.registerSimpleBlockItem("true_sight_chest", ModBlocks.TRUE_SIGHT_CHEST);
    public static final DeferredItem<BlockItem> WIND_ERODED_RUBBLE = ITEMS.registerSimpleBlockItem("wind_eroded_rubble", ModBlocks.WIND_ERODED_RUBBLE);
    public static final DeferredItem<BlockItem> MIRROR_REED = ITEMS.registerSimpleBlockItem("mirror_reed", ModBlocks.MIRROR_REED);
    public static final DeferredItem<BlockItem> TIDE_GRASS = ITEMS.registerSimpleBlockItem("tide_grass", ModBlocks.TIDE_GRASS);
    public static final DeferredItem<BlockItem> PEARL_ANEMONE = ITEMS.registerSimpleBlockItem("pearl_anemone", ModBlocks.PEARL_ANEMONE);
    public static final DeferredItem<BlockItem> STAR_MIST_BLOOM = ITEMS.registerSimpleBlockItem("star_mist_bloom", ModBlocks.STAR_MIST_BLOOM);
    public static final DeferredItem<BlockItem> MIRROR_LOTUS = ITEMS.registerSimpleBlockItem("mirror_lotus", ModBlocks.MIRROR_LOTUS);
    public static final DeferredItem<BlockItem> ASH_CHIME_GRASS = ITEMS.registerSimpleBlockItem("ash_chime_grass", ModBlocks.ASH_CHIME_GRASS);
    public static final DeferredItem<BlockItem> FOLDED_SPOREBLOOM = ITEMS.registerSimpleBlockItem(
            "folded_sporebloom", ModBlocks.FOLDED_SPOREBLOOM);
    public static final DeferredItem<BlockItem> GRAVITY_BELL = ITEMS.registerSimpleBlockItem(
            "gravity_bell", ModBlocks.GRAVITY_BELL);
    public static final DeferredItem<BlockItem> HUSH_THORN = ITEMS.registerSimpleBlockItem(
            "hush_thorn", ModBlocks.HUSH_THORN);
    public static final DeferredItem<BlockItem> MIRROR_DUST_STONE = ITEMS.registerSimpleBlockItem("mirror_dust_stone", ModBlocks.MIRROR_DUST_STONE);
    public static final DeferredItem<BlockItem> MIRROR_DUST_BRICKS = ITEMS.registerSimpleBlockItem("mirror_dust_bricks", ModBlocks.MIRROR_DUST_BRICKS);
    public static final DeferredItem<BlockItem> FRACTURED_MIRROR_TILE = ITEMS.registerSimpleBlockItem("fractured_mirror_tile", ModBlocks.FRACTURED_MIRROR_TILE);
    public static final DeferredItem<BlockItem> PRISM_DUST_CLUSTER = ITEMS.registerSimpleBlockItem("prism_dust_cluster", ModBlocks.PRISM_DUST_CLUSTER);
    public static final DeferredItem<BlockItem> GLASSVEIL_GRASS = ITEMS.registerSimpleBlockItem("glassveil_grass", ModBlocks.GLASSVEIL_GRASS);
    public static final DeferredItem<BlockItem> REFLECTION_ORCHID = ITEMS.registerSimpleBlockItem("reflection_orchid", ModBlocks.REFLECTION_ORCHID);
    public static final DeferredItem<BlockItem> ECHO_STRATA = ITEMS.registerSimpleBlockItem("echo_strata", ModBlocks.ECHO_STRATA);
    public static final DeferredItem<BlockItem> ECHO_STRATA_BRICKS = ITEMS.registerSimpleBlockItem("echo_strata_bricks", ModBlocks.ECHO_STRATA_BRICKS);
    public static final DeferredItem<BlockItem> CHISELED_ECHO_STRATA = ITEMS.registerSimpleBlockItem("chiseled_echo_strata", ModBlocks.CHISELED_ECHO_STRATA);
    public static final DeferredItem<BlockItem> ARCHIVE_MOSAIC = ITEMS.registerSimpleBlockItem("archive_mosaic", ModBlocks.ARCHIVE_MOSAIC);
    public static final DeferredItem<BlockItem> RESONANT_CLIFF_CRYSTAL = ITEMS.registerSimpleBlockItem("resonant_cliff_crystal", ModBlocks.RESONANT_CLIFF_CRYSTAL);
    public static final DeferredItem<BlockItem> CLIFF_HEATHER = ITEMS.registerSimpleBlockItem("cliff_heather", ModBlocks.CLIFF_HEATHER);
    public static final DeferredItem<BlockItem> ARCHIVE_LICHEN = ITEMS.registerSimpleBlockItem("archive_lichen", ModBlocks.ARCHIVE_LICHEN);
    public static final DeferredItem<BlockItem> SONGSTONE = ITEMS.registerSimpleBlockItem("songstone", ModBlocks.SONGSTONE);
    public static final DeferredItem<BlockItem> SONGSTONE_BRICKS = ITEMS.registerSimpleBlockItem("songstone_bricks", ModBlocks.SONGSTONE_BRICKS);
    public static final DeferredItem<BlockItem> CHISELED_SONGSTONE = ITEMS.registerSimpleBlockItem("chiseled_songstone", ModBlocks.CHISELED_SONGSTONE);
    public static final DeferredItem<BlockItem> SONGSTONE_TILES = ITEMS.registerSimpleBlockItem("songstone_tiles", ModBlocks.SONGSTONE_TILES);
    public static final DeferredItem<BlockItem> CRACKED_SONGSTONE_BRICKS = ITEMS.registerSimpleBlockItem(
            "cracked_songstone_bricks", ModBlocks.CRACKED_SONGSTONE_BRICKS);
    public static final DeferredItem<BlockItem> RESONANT_GEODE = ITEMS.registerSimpleBlockItem("resonant_geode", ModBlocks.RESONANT_GEODE);
    public static final DeferredItem<BlockItem> TUNING_GRASS = ITEMS.registerSimpleBlockItem("tuning_grass", ModBlocks.TUNING_GRASS);
    public static final DeferredItem<BlockItem> CRYSTAL_BELLFLOWER = ITEMS.registerSimpleBlockItem("crystal_bellflower", ModBlocks.CRYSTAL_BELLFLOWER);
    public static final DeferredItem<BlockItem> ASHEN_BELLSTONE = ITEMS.registerSimpleBlockItem("ashen_bellstone", ModBlocks.ASHEN_BELLSTONE);
    public static final DeferredItem<BlockItem> ASHEN_BELLSTONE_BRICKS = ITEMS.registerSimpleBlockItem("ashen_bellstone_bricks", ModBlocks.ASHEN_BELLSTONE_BRICKS);
    public static final DeferredItem<BlockItem> RUSTED_BELLSTONE = ITEMS.registerSimpleBlockItem("rusted_bellstone", ModBlocks.RUSTED_BELLSTONE);
    public static final DeferredItem<BlockItem> CLOCKWORK_SCRAP_BLOCK = ITEMS.registerSimpleBlockItem("clockwork_scrap_block", ModBlocks.CLOCKWORK_SCRAP_BLOCK);
    public static final DeferredItem<BlockItem> CINDER_REED = ITEMS.registerSimpleBlockItem("cinder_reed", ModBlocks.CINDER_REED);
    public static final DeferredItem<BlockItem> CLOCKWORK_THISTLE = ITEMS.registerSimpleBlockItem("clockwork_thistle", ModBlocks.CLOCKWORK_THISTLE);
    public static final DeferredItem<BlockItem> REEF_LIMESTONE = ITEMS.registerSimpleBlockItem("reef_limestone", ModBlocks.REEF_LIMESTONE);
    public static final DeferredItem<BlockItem> REEF_LIMESTONE_BRICKS = ITEMS.registerSimpleBlockItem("reef_limestone_bricks", ModBlocks.REEF_LIMESTONE_BRICKS);
    public static final DeferredItem<BlockItem> TIDEWORN_REEFSTONE = ITEMS.registerSimpleBlockItem("tideworn_reefstone", ModBlocks.TIDEWORN_REEFSTONE);
    public static final DeferredItem<BlockItem> TIDELIGHT_CORAL_BLOCK = ITEMS.registerSimpleBlockItem("tidelight_coral_block", ModBlocks.TIDELIGHT_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> LANTERN_SEAFAN = ITEMS.registerSimpleBlockItem("lantern_seafan", ModBlocks.LANTERN_SEAFAN);
    public static final DeferredItem<BlockItem> SHELL_BLOOM = ITEMS.registerSimpleBlockItem("shell_bloom", ModBlocks.SHELL_BLOOM);
    public static final DeferredItem<BlockItem> SILENT_MUDSTONE = ITEMS.registerSimpleBlockItem("silent_mudstone", ModBlocks.SILENT_MUDSTONE);
    public static final DeferredItem<BlockItem> SILENT_MUDSTONE_BRICKS = ITEMS.registerSimpleBlockItem("silent_mudstone_bricks", ModBlocks.SILENT_MUDSTONE_BRICKS);
    public static final DeferredItem<BlockItem> ROOTBOUND_MUDSTONE = ITEMS.registerSimpleBlockItem("rootbound_mudstone", ModBlocks.ROOTBOUND_MUDSTONE);
    public static final DeferredItem<BlockItem> BOGLIGHT_STONE = ITEMS.registerSimpleBlockItem("boglight_stone", ModBlocks.BOGLIGHT_STONE);
    public static final DeferredItem<BlockItem> MUTE_REED = ITEMS.registerSimpleBlockItem("mute_reed", ModBlocks.MUTE_REED);
    public static final DeferredItem<BlockItem> MIRE_BELL = ITEMS.registerSimpleBlockItem("mire_bell", ModBlocks.MIRE_BELL);
    public static final DeferredItem<BlockItem> FOLDED_SLATE = ITEMS.registerSimpleBlockItem("folded_slate", ModBlocks.FOLDED_SLATE);
    public static final DeferredItem<BlockItem> FOLDED_SLATE_BRICKS = ITEMS.registerSimpleBlockItem("folded_slate_bricks", ModBlocks.FOLDED_SLATE_BRICKS);
    public static final DeferredItem<BlockItem> CHISELED_FOLDED_SLATE = ITEMS.registerSimpleBlockItem("chiseled_folded_slate", ModBlocks.CHISELED_FOLDED_SLATE);
    public static final DeferredItem<BlockItem> RESONANT_FOLDED_SLATE = ITEMS.registerSimpleBlockItem("resonant_folded_slate", ModBlocks.RESONANT_FOLDED_SLATE);
    public static final DeferredItem<BlockItem> SEAM_GRASS = ITEMS.registerSimpleBlockItem("seam_grass", ModBlocks.SEAM_GRASS);
    public static final DeferredItem<BlockItem> ARCHIVE_FUNGUS = ITEMS.registerSimpleBlockItem("archive_fungus", ModBlocks.ARCHIVE_FUNGUS);
    public static final DeferredItem<BlockItem> GRAVITY_SHALE = ITEMS.registerSimpleBlockItem("gravity_shale", ModBlocks.GRAVITY_SHALE);
    public static final DeferredItem<BlockItem> GRAVITY_SHALE_BRICKS = ITEMS.registerSimpleBlockItem("gravity_shale_bricks", ModBlocks.GRAVITY_SHALE_BRICKS);
    public static final DeferredItem<BlockItem> BANDED_GRAVITY_SHALE = ITEMS.registerSimpleBlockItem("banded_gravity_shale", ModBlocks.BANDED_GRAVITY_SHALE);
    public static final DeferredItem<BlockItem> SUSPENDED_CRYSTAL = ITEMS.registerSimpleBlockItem("suspended_crystal", ModBlocks.SUSPENDED_CRYSTAL);
    public static final DeferredItem<BlockItem> INVERSION_REED = ITEMS.registerSimpleBlockItem("inversion_reed", ModBlocks.INVERSION_REED);
    public static final DeferredItem<BlockItem> WEIGHTLESS_BLOOM = ITEMS.registerSimpleBlockItem("weightless_bloom", ModBlocks.WEIGHTLESS_BLOOM);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE = ITEMS.registerSimpleBlockItem("abyssal_stone", ModBlocks.ABYSSAL_STONE);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE_BRICKS = ITEMS.registerSimpleBlockItem("abyssal_stone_bricks", ModBlocks.ABYSSAL_STONE_BRICKS);
    public static final DeferredItem<BlockItem> VEINED_ABYSSAL_STONE = ITEMS.registerSimpleBlockItem("veined_abyssal_stone", ModBlocks.VEINED_ABYSSAL_STONE);
    public static final DeferredItem<BlockItem> MUTE_CRYSTAL = ITEMS.registerSimpleBlockItem("mute_crystal", ModBlocks.MUTE_CRYSTAL);
    public static final DeferredItem<BlockItem> RESONANCE_ALLOY_BLOCK = ITEMS.registerSimpleBlockItem(
            "resonance_alloy_block", ModBlocks.RESONANCE_ALLOY_BLOCK, valuableProperties(Rarity.EPIC));
    public static final DeferredItem<BlockItem> PALIMPSEST_BLOCK = ITEMS.registerSimpleBlockItem(
            "palimpsest_block", ModBlocks.PALIMPSEST_BLOCK, valuableProperties(Rarity.EPIC));
    public static final DeferredItem<BlockItem> ORRERY_PRISM_BLOCK = ITEMS.registerSimpleBlockItem(
            "orrery_prism_block", ModBlocks.ORRERY_PRISM_BLOCK, valuableProperties(Rarity.EPIC));
    public static final DeferredItem<BlockItem> HUSH_GRASS = ITEMS.registerSimpleBlockItem("hush_grass", ModBlocks.HUSH_GRASS);
    public static final DeferredItem<BlockItem> VOIDCAP = ITEMS.registerSimpleBlockItem("voidcap", ModBlocks.VOIDCAP);
    public static final DeferredItem<BlockItem> ECHO_MOSSY_STONE = ITEMS.registerSimpleBlockItem("echo_mossy_stone", ModBlocks.ECHO_MOSSY_STONE);
    public static final DeferredItem<BlockItem> WIND_CHISELED_STONE = ITEMS.registerSimpleBlockItem("wind_chiseled_stone", ModBlocks.WIND_CHISELED_STONE);
    public static final DeferredItem<BlockItem> SKY_LAMP_GLASS = ITEMS.registerSimpleBlockItem("sky_lamp_glass", ModBlocks.SKY_LAMP_GLASS);
    public static final DeferredItem<BlockItem> TIDE_SMOOTH_STONE = ITEMS.registerSimpleBlockItem("tide_smooth_stone", ModBlocks.TIDE_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> MIRROR_SAND = ITEMS.registerSimpleBlockItem("mirror_sand", ModBlocks.MIRROR_SAND);
    public static final DeferredItem<BlockItem> PEARL_CORAL_BLOCK = ITEMS.registerSimpleBlockItem("pearl_coral_block", ModBlocks.PEARL_CORAL_BLOCK);

    public static final DeferredItem<BlockItem> ECHO_STONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("echo_stone_brick_stairs", ModBlocks.ECHO_STONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> ECHO_STONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("echo_stone_brick_slab", ModBlocks.ECHO_STONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> ECHO_STONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("echo_stone_brick_wall", ModBlocks.ECHO_STONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> ECHO_STRATA_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("echo_strata_brick_stairs", ModBlocks.ECHO_STRATA_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> ECHO_STRATA_BRICK_SLAB = ITEMS.registerSimpleBlockItem("echo_strata_brick_slab", ModBlocks.ECHO_STRATA_BRICK_SLAB);
    public static final DeferredItem<BlockItem> ECHO_STRATA_BRICK_WALL = ITEMS.registerSimpleBlockItem("echo_strata_brick_wall", ModBlocks.ECHO_STRATA_BRICK_WALL);
    public static final DeferredItem<BlockItem> SONGSTONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("songstone_brick_stairs", ModBlocks.SONGSTONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> SONGSTONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("songstone_brick_slab", ModBlocks.SONGSTONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> SONGSTONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("songstone_brick_wall", ModBlocks.SONGSTONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> ASHEN_BELLSTONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("ashen_bellstone_brick_stairs", ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> ASHEN_BELLSTONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("ashen_bellstone_brick_slab", ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> ASHEN_BELLSTONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("ashen_bellstone_brick_wall", ModBlocks.ASHEN_BELLSTONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> REEF_LIMESTONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("reef_limestone_brick_stairs", ModBlocks.REEF_LIMESTONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> REEF_LIMESTONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("reef_limestone_brick_slab", ModBlocks.REEF_LIMESTONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> REEF_LIMESTONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("reef_limestone_brick_wall", ModBlocks.REEF_LIMESTONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> SILENT_MUDSTONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("silent_mudstone_brick_stairs", ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> SILENT_MUDSTONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("silent_mudstone_brick_slab", ModBlocks.SILENT_MUDSTONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> SILENT_MUDSTONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("silent_mudstone_brick_wall", ModBlocks.SILENT_MUDSTONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> MIRROR_STONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("mirror_stone_brick_stairs", ModBlocks.MIRROR_STONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> MIRROR_STONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("mirror_stone_brick_slab", ModBlocks.MIRROR_STONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> MIRROR_STONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("mirror_stone_brick_wall", ModBlocks.MIRROR_STONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> MIRROR_DUST_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("mirror_dust_brick_stairs", ModBlocks.MIRROR_DUST_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> MIRROR_DUST_BRICK_SLAB = ITEMS.registerSimpleBlockItem("mirror_dust_brick_slab", ModBlocks.MIRROR_DUST_BRICK_SLAB);
    public static final DeferredItem<BlockItem> MIRROR_DUST_BRICK_WALL = ITEMS.registerSimpleBlockItem("mirror_dust_brick_wall", ModBlocks.MIRROR_DUST_BRICK_WALL);
    public static final DeferredItem<BlockItem> FOLDED_SLATE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("folded_slate_brick_stairs", ModBlocks.FOLDED_SLATE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> FOLDED_SLATE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("folded_slate_brick_slab", ModBlocks.FOLDED_SLATE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> FOLDED_SLATE_BRICK_WALL = ITEMS.registerSimpleBlockItem("folded_slate_brick_wall", ModBlocks.FOLDED_SLATE_BRICK_WALL);
    public static final DeferredItem<BlockItem> GRAVITY_SHALE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("gravity_shale_brick_stairs", ModBlocks.GRAVITY_SHALE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> GRAVITY_SHALE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("gravity_shale_brick_slab", ModBlocks.GRAVITY_SHALE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> GRAVITY_SHALE_BRICK_WALL = ITEMS.registerSimpleBlockItem("gravity_shale_brick_wall", ModBlocks.GRAVITY_SHALE_BRICK_WALL);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("abyssal_stone_brick_stairs", ModBlocks.ABYSSAL_STONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem("abyssal_stone_brick_slab", ModBlocks.ABYSSAL_STONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE_BRICK_WALL = ITEMS.registerSimpleBlockItem("abyssal_stone_brick_wall", ModBlocks.ABYSSAL_STONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> WIND_ETCHED_BRICKS = ITEMS.registerSimpleBlockItem("wind_etched_bricks", ModBlocks.WIND_ETCHED_BRICKS);
    public static final DeferredItem<BlockItem> WIND_ETCHED_BRICK_STAIRS = ITEMS.registerSimpleBlockItem("wind_etched_brick_stairs", ModBlocks.WIND_ETCHED_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> WIND_ETCHED_BRICK_SLAB = ITEMS.registerSimpleBlockItem("wind_etched_brick_slab", ModBlocks.WIND_ETCHED_BRICK_SLAB);
    public static final DeferredItem<BlockItem> WIND_ETCHED_BRICK_WALL = ITEMS.registerSimpleBlockItem("wind_etched_brick_wall", ModBlocks.WIND_ETCHED_BRICK_WALL);
    public static final DeferredItem<BlockItem> WHISPERING_TIMBER_FRAME = ITEMS.registerSimpleBlockItem("whispering_timber_frame", ModBlocks.WHISPERING_TIMBER_FRAME);
    public static final DeferredItem<BlockItem> WHISPERING_TIMBER_STAIRS = ITEMS.registerSimpleBlockItem("whispering_timber_stairs", ModBlocks.WHISPERING_TIMBER_STAIRS);
    public static final DeferredItem<BlockItem> WHISPERING_TIMBER_SLAB = ITEMS.registerSimpleBlockItem("whispering_timber_slab", ModBlocks.WHISPERING_TIMBER_SLAB);
    public static final DeferredItem<BlockItem> WHISPERING_TIMBER_WALL = ITEMS.registerSimpleBlockItem("whispering_timber_wall", ModBlocks.WHISPERING_TIMBER_WALL);
    public static final DeferredItem<BlockItem> REEF_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("reef_lantern_glass", ModBlocks.REEF_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> BELL_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("bell_lantern_glass", ModBlocks.BELL_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> HUSH_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("hush_lantern_glass", ModBlocks.HUSH_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> FOLD_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("fold_lantern_glass", ModBlocks.FOLD_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> SONG_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("song_lantern_glass", ModBlocks.SONG_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> ARCHIVE_LANTERN_GLASS = ITEMS.registerSimpleBlockItem("archive_lantern_glass", ModBlocks.ARCHIVE_LANTERN_GLASS);
    public static final DeferredItem<BlockItem> CRYSTAL_SONG_CLUSTER = ITEMS.registerSimpleBlockItem("crystal_song_cluster", ModBlocks.CRYSTAL_SONG_CLUSTER);
    public static final DeferredItem<BlockItem> RESONANT_MUSHROOM = ITEMS.registerSimpleBlockItem("resonant_mushroom", ModBlocks.RESONANT_MUSHROOM);
    public static final DeferredItem<BlockItem> DREAM_FLOWER = ITEMS.registerSimpleBlockItem("dream_flower", ModBlocks.DREAM_FLOWER);
    public static final DeferredItem<BlockItem> ECHO_AMBER_ORE = ITEMS.registerSimpleBlockItem("echo_amber_ore", ModBlocks.ECHO_AMBER_ORE);
    public static final DeferredItem<BlockItem> DREAM_MIST_VINE = ITEMS.registerSimpleBlockItem("dream_mist_vine", ModBlocks.DREAM_MIST_VINE);
    public static final DeferredItem<BlockItem> BROKEN_BELL_ORE = ITEMS.registerSimpleBlockItem("broken_bell_ore", ModBlocks.BROKEN_BELL_ORE);
    public static final DeferredItem<BlockItem> TIDE_SALT_ORE = ITEMS.registerSimpleBlockItem("tide_salt_ore", ModBlocks.TIDE_SALT_ORE);
    public static final DeferredItem<BlockItem> BROKEN_BELL_THORN = ITEMS.registerSimpleBlockItem("broken_bell_thorn", ModBlocks.BROKEN_BELL_THORN);
    public static final DeferredItem<BlockItem> SKY_QUARTZ_ORE = ITEMS.registerSimpleBlockItem("sky_quartz_ore", ModBlocks.SKY_QUARTZ_ORE);
    public static final DeferredItem<BlockItem> ECHO_CLIFF_STONE = ITEMS.registerSimpleBlockItem("echo_cliff_stone", ModBlocks.ECHO_CLIFF_STONE);
    public static final DeferredItem<BlockItem> ECHO_ARCHIVE_TERMINAL = ITEMS.registerSimpleBlockItem("echo_archive_terminal", ModBlocks.ECHO_ARCHIVE_TERMINAL);

    public static final DeferredItem<DeferredSpawnEggItem> ECHO_WANDERER_SPAWN_EGG = ITEMS.register("echo_wanderer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ECHO_WANDERER, 0x2E4B5B, 0x7FE3D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FORGOTTEN_COLOSSUS_SPAWN_EGG = ITEMS.register("forgotten_colossus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FORGOTTEN_COLOSSUS, 0x4A4458, 0xB46CFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ABYSS_WATCHER_SPAWN_EGG = ITEMS.register("abyss_watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ABYSS_WATCHER, 0x1E4058, 0x78CDFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> VOID_PROPHET_SPAWN_EGG = ITEMS.register("void_prophet_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VOID_PROPHET, 0x140D24, 0xD94CFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> RIFT_WARDEN_SPAWN_EGG = ITEMS.register("rift_warden_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RIFT_WARDEN, 0x171722, 0x7842E8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WORLD_ECHO_SPAWN_EGG = ITEMS.register("world_echo_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WORLD_ECHO, 0x0C0C14, 0xF0B84E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MIRROR_GUARDIAN_SPAWN_EGG = ITEMS.register("mirror_guardian_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIRROR_GUARDIAN, 0xBCD4DE, 0x2C3E4E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SILENT_WALKER_SPAWN_EGG = ITEMS.register("silent_walker_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SILENT_WALKER, 0x4A5246, 0x76806C, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CRYSTAL_FEATHER_BIRD_SPAWN_EGG = ITEMS.register("crystal_feather_bird_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRYSTAL_FEATHER_BIRD, 0xC8DCE8, 0x8FE8E0, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> STORM_WEAVER_SPAWN_EGG = ITEMS.register("storm_weaver_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STORM_WEAVER, 0x9CB8C8, 0x3FD9C4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SILENT_PRIEST_SPAWN_EGG = ITEMS.register("silent_priest_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SILENT_PRIEST, 0x3C4438, 0x9CB48A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> TIDE_LANTERN_KEEPER_SPAWN_EGG = ITEMS.register("tide_lantern_keeper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TIDE_LANTERN_KEEPER, 0x1E3A4C, 0x6FE0D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MIRROR_DUST_BUTLER_SPAWN_EGG = ITEMS.register("mirror_dust_butler_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIRROR_DUST_BUTLER, 0x5A5E6E, 0xC8CFE8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> TIDE_WATER_SHADE_SPAWN_EGG = ITEMS.register("tide_water_shade_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TIDE_WATER_SHADE, 0x16242E, 0x2E4C66, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LOST_TRAVELER_SPAWN_EGG = ITEMS.register("lost_traveler_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LOST_TRAVELER, 0x4E4438, 0xC8A85A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ECHO_DEER_SPAWN_EGG = ITEMS.register("echo_deer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ECHO_DEER, 0x6E5A48, 0x9FE8D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> GLOW_RABBIT_SPAWN_EGG = ITEMS.register("glow_rabbit_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GLOW_RABBIT, 0xE8E0D0, 0xA8F0C8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MIRROR_TAIL_FISH_SPAWN_EGG = ITEMS.register("mirror_tail_fish_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIRROR_TAIL_FISH, 0x6E8EA0, 0xDCEEF4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ECHO_CUTTLEFISH_SPAWN_EGG = ITEMS.register("echo_cuttlefish_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ECHO_CUTTLEFISH, 0x263B4B, 0x65E8D6, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> PEARLBACK_RAY_SPAWN_EGG = ITEMS.register("pearlback_ray_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PEARLBACK_RAY, 0x4D5964, 0xE9D7B1, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MOSS_BACK_TURTLE_SPAWN_EGG = ITEMS.register("moss_back_turtle_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MOSS_BACK_TURTLE, 0x586448, 0x7C9460, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> DREAMING_DEER_SPAWN_EGG = ITEMS.register("dreaming_deer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DREAMING_DEER, 0xB7A178, 0xE9C7D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CRYSTAL_CHIME_SPIRIT_SPAWN_EGG = ITEMS.register("crystal_chime_spirit_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRYSTAL_CHIME_SPIRIT, 0x7DA8B8, 0xD8F7FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BROKEN_BELL_GUARD_SPAWN_EGG = ITEMS.register("broken_bell_guard_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BROKEN_BELL_GUARD, 0x4D4637, 0xB79050, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CRYSTAL_NOISE_WISP_SPAWN_EGG = ITEMS.register("crystal_noise_wisp_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRYSTAL_NOISE_WISP, 0x39455A, 0xE2B7FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BROKEN_BELL_CROW_SPAWN_EGG = ITEMS.register("broken_bell_crow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BROKEN_BELL_CROW, 0x2C2A28, 0xB48342, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LOST_RECORDER_SPAWN_EGG = ITEMS.register("lost_recorder_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LOST_RECORDER, 0x3B4050, 0xD6C8A4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WIND_SAC_MOTH_SPAWN_EGG = ITEMS.register("wind_sac_moth_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WIND_SAC_MOTH, 0xB9D6D3, 0xF3E6AA, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WIND_ERODED_SENTINEL_SPAWN_EGG = ITEMS.register("wind_eroded_sentinel_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WIND_ERODED_SENTINEL, 0x8D927E, 0xCDEFE7, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ECHO_REMNANT_SPAWN_EGG = ITEMS.register("echo_remnant_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ECHO_REMNANT, 0x24364A, 0x7FE3D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WATER_SHADOW_SPAWN_EGG = ITEMS.register("water_shadow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WATER_SHADOW, 0x142839, 0x6ECAD6, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MIRROR_DUST_DECOY_SPAWN_EGG = ITEMS.register("mirror_dust_decoy_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIRROR_DUST_DECOY, 0x3A4055, 0xD7E5FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CRYSTAL_SONGKEEPER_SPAWN_EGG =
            ITEMS.register("crystal_songkeeper_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.CRYSTAL_SONGKEEPER,
                            0x6E4A9C, 0xE7C8FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BROKEN_BELL_KEEPER_SPAWN_EGG =
            ITEMS.register("broken_bell_keeper_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.BROKEN_BELL_KEEPER,
                            0x5B4630, 0xD4A04E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> DREAM_BLOOM_KEEPER_SPAWN_EGG =
            ITEMS.register("dream_bloom_keeper_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.DREAM_BLOOM_KEEPER,
                            0xDDA7C8, 0xF7E6A4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LOST_RECORDER_CHIEF_SPAWN_EGG =
            ITEMS.register("lost_recorder_chief_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.LOST_RECORDER_CHIEF,
                            0x263044, 0xD6C8A4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> INVERTED_GATEKEEPER_SPAWN_EGG =
            ITEMS.register("inverted_gatekeeper_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.INVERTED_GATEKEEPER,
                            0x173D3D, 0xB98945, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ASH_SCRIBE_SPAWN_EGG =
            ITEMS.register("ash_scribe_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ASH_SCRIBE,
                            0x123647, 0xC7472D, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FOLDPAGE_BUG_SPAWN_EGG =
            ITEMS.register("foldpage_bug_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.FOLDPAGE_BUG,
                            0x4C544B, 0x9E8651, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ANCHOR_SHELL_BEAST_SPAWN_EGG =
            ITEMS.register("anchor_shell_beast_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ANCHOR_SHELL_BEAST,
                            0x343A38, 0x9B713A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ABYSS_LURKER_SPAWN_EGG =
            ITEMS.register("abyss_lurker_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ABYSS_LURKER,
                            0x221D2A, 0x649A91, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> VOID_WANDERER_SPAWN_EGG =
            ITEMS.register("void_wanderer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.VOID_WANDERER,
                            0x181923, 0x6FB8AE, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> RIFT_GLIDER_SPAWN_EGG =
            ITEMS.register("rift_glider_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.RIFT_GLIDER,
                            0x263D49, 0x66EBE0, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ASHBACK_GRAZER_SPAWN_EGG =
            ITEMS.register("ashback_grazer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ASHBACK_GRAZER,
                            0x5C5646, 0xD2843E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> HUSHLIGHT_JELLY_SPAWN_EGG =
            ITEMS.register("hushlight_jelly_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.HUSHLIGHT_JELLY,
                            0x3F3A58, 0xAEF7DD, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FOLDED_MANTIS_SPAWN_EGG =
            ITEMS.register("folded_mantis_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.FOLDED_MANTIS,
                            0x605C46, 0xC44A34, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> GRAVITY_RAM_SPAWN_EGG =
            ITEMS.register("gravity_ram_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.GRAVITY_RAM,
                            0x46445B, 0x70DEF2, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ASHVEIL_STALKER_SPAWN_EGG =
            ITEMS.register("ashveil_stalker_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ASHVEIL_STALKER,
                            0x524B48, 0xFF8B3E, new Item.Properties()));

    private static Item.Properties armorProperties(ArmorItem.Type type, int durabilityMultiplier, Rarity rarity) {
        return new Item.Properties().durability(type.getDurability(durabilityMultiplier))
                .rarity(rarity).fireResistant();
    }

    private static Item.Properties unbreakableEndgameProperties() {
        return valuableProperties(1, Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    }

    private static Item.Properties unbreakableEndgameArmorProperties(ArmorItem.Type type) {
        return unbreakableEndgameProperties()
                .durability(type.getDurability(96))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    }

    private static Item.Properties energyFoodProperties(int stackSize, Rarity rarity) {
        Item.Properties properties = new Item.Properties().stacksTo(stackSize).rarity(rarity);
        if (rarity == Rarity.RARE || rarity == Rarity.EPIC) {
            properties.fireResistant();
        }
        return properties.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F)
                .alwaysEdible().build());
    }

    private static Item.Properties valuableProperties(Rarity rarity) {
        return new Item.Properties().rarity(rarity).fireResistant();
    }

    private static Item.Properties valuableProperties(int stackSize, Rarity rarity) {
        return new Item.Properties().stacksTo(stackSize).rarity(rarity).fireResistant();
    }

    private static Item.Properties toolProperties(int durability, float attackDamage, float attackSpeed, Rarity rarity) {
        return new Item.Properties()
                .durability(durability)
                .rarity(rarity)
                .fireResistant()
                .attributes(DiggerItem.createAttributes(EchoMetalTier.INSTANCE, attackDamage, attackSpeed));
    }

    private static Item.Properties deepToolProperties(float attackDamage, float attackSpeed) {
        return new Item.Properties().durability(DeepAlloyTier.INSTANCE.getUses()).rarity(Rarity.RARE)
                .fireResistant()
                .attributes(DiggerItem.createAttributes(DeepAlloyTier.INSTANCE, attackDamage, attackSpeed));
    }

    private static Item.Properties landmarkToolProperties(float attackDamage, float attackSpeed) {
        return valuableProperties(1, Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .attributes(DiggerItem.createAttributes(DeepAlloyTier.INSTANCE, attackDamage, attackSpeed));
    }
}
