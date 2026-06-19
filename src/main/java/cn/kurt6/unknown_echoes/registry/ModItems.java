package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.item.AncientPageItem;
import cn.kurt6.unknown_echoes.item.EchoKeyItem;
import cn.kurt6.unknown_echoes.item.EchoMaterialItem;
import cn.kurt6.unknown_echoes.item.EchoProjectorItem;
import cn.kurt6.unknown_echoes.item.EchoSetArmorItem;
import cn.kurt6.unknown_echoes.item.EchoUtilityConsumableItem;
import cn.kurt6.unknown_echoes.item.EquipmentBlueprintItem;
import cn.kurt6.unknown_echoes.item.GenericArtifactItem;
import cn.kurt6.unknown_echoes.item.ResearchRubbingItem;
import cn.kurt6.unknown_echoes.item.SilentPodItem;
import cn.kurt6.unknown_echoes.item.WindEchoMarkItem;
import cn.kurt6.unknown_echoes.item.tool.EchoBrushItem;
import cn.kurt6.unknown_echoes.item.tool.EchoFishingRodItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHatchetItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHoeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoIgniterItem;
import cn.kurt6.unknown_echoes.item.tool.EchoPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShearsItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShovelItem;
import cn.kurt6.unknown_echoes.item.weapon.CrystalSongStaffItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoBowItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoMeleeWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.TideCrossbowItem;
import cn.kurt6.unknown_echoes.item.weapon.TrueSightBladeItem;
import cn.kurt6.unknown_echoes.item.weapon.WindSpearItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UnknownEchoes.MODID);

    public static final DeferredItem<Item> ECHO_SHARD = ITEMS.register("echo_shard",
            () -> new EchoMaterialItem("echo_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ECHO_CORE = ITEMS.register("echo_core",
            () -> new EchoMaterialItem("echo_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> ECHO_CLOTH = ITEMS.register("echo_cloth",
            () -> new EchoMaterialItem("echo_cloth", new Item.Properties()));

    public static final DeferredItem<Item> BLANK_ECHO_PAGE = ITEMS.register("blank_echo_page",
            () -> new EchoMaterialItem("blank_echo_page", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_INK_PAGE = ITEMS.register("echo_ink_page",
            () -> new EchoMaterialItem("echo_ink_page", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_MARK = ITEMS.register("echo_mark",
            () -> new EchoMaterialItem("echo_mark", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TRAVELER_BADGE = ITEMS.register("traveler_badge",
            () -> new EchoMaterialItem("traveler_badge", new Item.Properties().rarity(Rarity.RARE)));

    // ---- 第一维度交易/战利品消耗品(普通物品,不承载关键进度) ----

    public static final DeferredItem<EchoUtilityConsumableItem> ECHO_BANDAGE = ITEMS.register("echo_bandage",
            () -> new EchoUtilityConsumableItem("echo_bandage",
                    EchoUtilityConsumableItem.Kind.ECHO_BANDAGE,
                    new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

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
                    new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                            .nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

    public static final DeferredItem<EchoUtilityConsumableItem> MEMORY_ECHO_STONE =
            ITEMS.register("memory_echo_stone", () -> new EchoUtilityConsumableItem("memory_echo_stone",
                    EchoUtilityConsumableItem.Kind.MEMORY_ECHO_STONE,
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder()
                            .nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

    // ---- V0.5 镜湖材料 ----

    /** 镜湖碎片:镜湖独有资源(挖掘镜石获得),用于镜湖装备与图纸线索。 */
    public static final DeferredItem<Item> MIRROR_LAKE_SHARD = ITEMS.register("mirror_lake_shard",
            () -> new EchoMaterialItem("mirror_lake_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_PEARL = ITEMS.register("tide_pearl",
            () -> new EchoMaterialItem("tide_pearl", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MIRROR_SCALE = ITEMS.register("mirror_scale",
            () -> new EchoMaterialItem("mirror_scale", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 潮汐核心:深渊观测者掉落材料(普通材料可掉落;能力本身只写玩家数据)。 */
    public static final DeferredItem<Item> TIDE_CORE = ITEMS.register("tide_core",
            () -> new EchoMaterialItem("tide_core", new Item.Properties().rarity(Rarity.RARE)));

    /** 真视核心:镜像守护者掉落材料(普通材料可掉落)。 */
    public static final DeferredItem<Item> TRUE_SIGHT_CORE = ITEMS.register("true_sight_core",
            () -> new EchoMaterialItem("true_sight_core", new Item.Properties().rarity(Rarity.RARE)));

    /** 观测者之眼碎片:深渊观测者掉落材料(后续深海信标/神器线索用)。 */
    public static final DeferredItem<Item> WATCHER_EYE_SHARD = ITEMS.register("watcher_eye_shard",
            () -> new EchoMaterialItem("watcher_eye_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.5C 失语沼泽材料 ----

    /** 沉默荚果:失语沼泽独有食材,吃下清除负面状态(SilentPodItem 服务端判定)。 */
    public static final DeferredItem<SilentPodItem> SILENT_POD = ITEMS.register("silent_pod",
            () -> new SilentPodItem(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build())));

    public static final DeferredItem<Item> SILENCE_MOSS = ITEMS.register("silence_moss",
            () -> new EchoMaterialItem("silence_moss", new Item.Properties()));

    public static final DeferredItem<Item> MOSS_SHELL_PLATE = ITEMS.register("moss_shell_plate",
            () -> new EchoMaterialItem("moss_shell_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.5C 漂浮群岛材料 ----

    /** 晶羽:晶羽鸟周期性掉落,风之装备材料。 */
    public static final DeferredItem<Item> CRYSTAL_FEATHER = ITEMS.register("crystal_feather",
            () -> new EchoMaterialItem("crystal_feather", new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.5C 中期装备材料线(武器本体 V0.6 落地) ----

    /** 回响金属:中期装备通用金属(回响碎片熔铸)。 */
    public static final DeferredItem<Item> ECHO_METAL_INGOT = ITEMS.register("echo_metal_ingot",
            () -> new EchoMaterialItem("echo_metal_ingot", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 风纹碎片:风纹石加工产物,风纹长矛材料。 */
    public static final DeferredItem<Item> WIND_RUNE_SHARD = ITEMS.register("wind_rune_shard",
            () -> new EchoMaterialItem("wind_rune_shard", new Item.Properties()));

    /** 水下符文片:沉没圣殿产出,潮汐弩材料。 */
    public static final DeferredItem<Item> TIDE_RUNE_FRAGMENT = ITEMS.register("tide_rune_fragment",
            () -> new EchoMaterialItem("tide_rune_fragment", new Item.Properties()));

    /** 回响弦:潮汐弩弦材(回响碎片缠绕线绳)。 */
    public static final DeferredItem<Item> ECHO_STRING = ITEMS.register("echo_string",
            () -> new EchoMaterialItem("echo_string", new Item.Properties()));

    /** 幻象尘:镜像守护者与镜面神殿产出,真视短刃材料。 */
    public static final DeferredItem<Item> ILLUSION_DUST = ITEMS.register("illusion_dust",
            () -> new EchoMaterialItem("illusion_dust", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> POLISHED_MIRROR_DUST = ITEMS.register("polished_mirror_dust",
            () -> new EchoMaterialItem("polished_mirror_dust", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 假像碎片:镜尘假像低概率掉落的真视路线普通材料。 */
    public static final DeferredItem<Item> DECOY_SHARD = ITEMS.register("decoy_shard",
            () -> new EchoMaterialItem("decoy_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.6A Mini Boss 结算材料 ----

    /** 回响粉尘:守护者结算通用普通材料(重复击败保底,后续交易/合成用)。 */
    public static final DeferredItem<Item> ECHO_DUST = ITEMS.register("echo_dust",
            () -> new EchoMaterialItem("echo_dust", new Item.Properties()));

    /** 风暴罗盘部件:风暴编织者参与结算发放;V0.6D 在天空观测站记录台合部件领取风暴罗盘。 */
    public static final DeferredItem<Item> STORM_COMPASS_PART = ITEMS.register("storm_compass_part",
            () -> new EchoMaterialItem("storm_compass_part", new Item.Properties().rarity(Rarity.RARE)));

    /** 风暴罗盘:首件神器的凭据物品(StormCompassItem)。本体在玩家 ArtifactData,序号不符不可用。 */
    public static final DeferredItem<cn.kurt6.unknown_echoes.item.StormCompassItem> STORM_COMPASS =
            ITEMS.register("storm_compass", () -> new cn.kurt6.unknown_echoes.item.StormCompassItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> TIDE_LANTERN =
            ITEMS.register("tide_lantern", () -> new GenericArtifactItem(ArtifactType.TIDE_LANTERN,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<GenericArtifactItem> ECHO_LENS =
            ITEMS.register("echo_lens", () -> new GenericArtifactItem(ArtifactType.ECHO_LENS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** 线索地图:迷途旅者出售,阅读后做一次有界定位写入个人日志线索(ClueMapItem)。 */
    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ClueMapItem> CLUE_MAP =
            ITEMS.register("clue_map", () -> new cn.kurt6.unknown_echoes.item.ClueMapItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** 记录拓印纸:迷途旅者出售的记述派耗材;V0.6E 共鸣信物/拓印玩法材料预留,当前作交易材料。 */
    public static final DeferredItem<Item> RECORD_TRACING_PAPER = ITEMS.register("record_tracing_paper",
            () -> new EchoMaterialItem("record_tracing_paper", new Item.Properties()));

    /** 风之强化核心:风暴编织者结算强化材料,只强化已获得的风之回响(10.4 分层)。 */
    public static final DeferredItem<Item> WIND_ENHANCE_CORE = ITEMS.register("wind_enhance_core",
            () -> new EchoMaterialItem("wind_enhance_core", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 研究拓片(风之):阅读记入个人记录;V0.6B 接研究等级进度。 */
    public static final DeferredItem<ResearchRubbingItem> WIND_RESEARCH_RUBBING =
            ITEMS.register("wind_research_rubbing", () -> new ResearchRubbingItem("wind",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.6B 强化与装备闭环 ----

    /** 研究拓片(潮汐/真视):阅读写入对应能力研究进度(ResearchRubbingItem)。 */
    public static final DeferredItem<ResearchRubbingItem> TIDE_RESEARCH_RUBBING =
            ITEMS.register("tide_research_rubbing", () -> new ResearchRubbingItem("tide",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TRUE_SIGHT_RESEARCH_RUBBING =
            ITEMS.register("true_sight_research_rubbing", () -> new ResearchRubbingItem("true_sight",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.7A/D 文档规范研究材料 ID ----

    public static final DeferredItem<ResearchRubbingItem> WIND_FRAGMENT_PAGE =
            ITEMS.register("wind_fragment_page", () -> new ResearchRubbingItem("wind",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TIDE_FRAGMENT_PAGE =
            ITEMS.register("tide_fragment_page", () -> new ResearchRubbingItem("tide",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ResearchRubbingItem> TRUE_SIGHT_FRAGMENT_PAGE =
            ITEMS.register("true_sight_fragment_page", () -> new ResearchRubbingItem("true_sight",
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> INSCRIPTION_RUBBING_PAPER =
            ITEMS.register("inscription_rubbing_paper",
                    () -> new EchoMaterialItem("inscription_rubbing_paper", new Item.Properties()));

    public static final DeferredItem<Item> ECHO_INDEX = ITEMS.register("echo_index",
            () -> new EchoMaterialItem("echo_index", new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- V0.7A/D 文档规范强化核心 ID ----

    public static final DeferredItem<Item> TIDE_UPGRADE_CORE = ITEMS.register("tide_upgrade_core",
            () -> new EchoMaterialItem("tide_upgrade_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> TRUE_SIGHT_UPGRADE_CORE = ITEMS.register("true_sight_upgrade_core",
            () -> new EchoMaterialItem("true_sight_upgrade_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SILENCE_UPGRADE_CORE = ITEMS.register("silence_upgrade_core",
            () -> new EchoMaterialItem("silence_upgrade_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CRYSTAL_SONG_UPGRADE_CORE = ITEMS.register("crystal_song_upgrade_core",
            () -> new EchoMaterialItem("crystal_song_upgrade_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> BROKEN_BELL_UPGRADE_CORE = ITEMS.register("broken_bell_upgrade_core",
            () -> new EchoMaterialItem("broken_bell_upgrade_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> ECHO_REALM_INGOT = ITEMS.register("echo_realm_ingot",
            () -> new EchoMaterialItem("echo_realm_ingot", new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> ECHO_REALM_CORE = ITEMS.register("echo_realm_core",
            () -> new EchoMaterialItem("echo_realm_core", new Item.Properties().rarity(Rarity.EPIC)));

    // ---- 第一维度材料体系:主线 Boss / Mini Boss 个人结算材料 ----

    public static final DeferredItem<Item> COLOSSUS_MEMORY_CORE = ITEMS.register("colossus_memory_core",
            () -> new EchoMaterialItem("colossus_memory_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> MEMORY_STONE_SLAB = ITEMS.register("memory_stone_slab",
            () -> new EchoMaterialItem("memory_stone_slab", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BLACK_TIDE_THREAD = ITEMS.register("black_tide_thread",
            () -> new EchoMaterialItem("black_tide_thread", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> BROKEN_REFLECTION = ITEMS.register("broken_reflection",
            () -> new EchoMaterialItem("broken_reflection", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MIRROR_GUARDIAN_CORE = ITEMS.register("mirror_guardian_core",
            () -> new EchoMaterialItem("mirror_guardian_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> STORM_SPINDLE = ITEMS.register("storm_spindle",
            () -> new EchoMaterialItem("storm_spindle", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> BROKEN_LANTERN_LENS = ITEMS.register("broken_lantern_lens",
            () -> new EchoMaterialItem("broken_lantern_lens", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> MIRROR_STAFF_CORE = ITEMS.register("mirror_staff_core",
            () -> new EchoMaterialItem("mirror_staff_core", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SILENT_CANDLE_WICK = ITEMS.register("silent_candle_wick",
            () -> new EchoMaterialItem("silent_candle_wick", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SONGKEEPER_FORK = ITEMS.register("songkeeper_fork",
            () -> new EchoMaterialItem("songkeeper_fork", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> BELL_CLAPPER = ITEMS.register("bell_clapper",
            () -> new EchoMaterialItem("bell_clapper", new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> ARCHIVE_BINDING = ITEMS.register("archive_binding",
            () -> new EchoMaterialItem("archive_binding", new Item.Properties().rarity(Rarity.RARE)));

    // ---- V0.7H 文档规范武器 ID ----

    public static final DeferredItem<EchoMeleeWeaponItem> ECHO_BROADSWORD =
            ITEMS.register("echo_broadsword", () -> new EchoMeleeWeaponItem(11.0, 1.55,
                    "echo_broadsword", new Item.Properties().durability(2200).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<EchoMeleeWeaponItem> ECHO_BATTLEAXE =
            ITEMS.register("echo_battleaxe", () -> new EchoMeleeWeaponItem(15.0, 0.95,
                    "echo_battleaxe", new Item.Properties().durability(2200).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoBowItem> ECHO_LONGBOW =
            ITEMS.register("echo_longbow", () -> new EchoBowItem("echo_longbow",
                    new Item.Properties().durability(980).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<WindSpearItem> WIND_SPEAR =
            ITEMS.register("wind_spear", () -> new WindSpearItem(12.0, 1.40, 140, 1.65,
                    9.9F, new Item.Properties().durability(2100).rarity(Rarity.RARE)));

    public static final DeferredItem<TideCrossbowItem> TIDE_CROSSBOW =
            ITEMS.register("tide_crossbow", () -> new TideCrossbowItem(4.6, 2,
                    new Item.Properties().stacksTo(1).durability(1800).rarity(Rarity.RARE)));

    public static final DeferredItem<TrueSightBladeItem> TRUE_SIGHT_BLADE =
            ITEMS.register("true_sight_blade", () -> new TrueSightBladeItem(10.0, 2.2, 80,
                    6.0F, new Item.Properties().durability(2100).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoMeleeWeaponItem> SILENCE_SCYTHE =
            ITEMS.register("silence_scythe", () -> new EchoMeleeWeaponItem(12.0, 1.25,
                    "silence_scythe", new Item.Properties().durability(2100).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoMeleeWeaponItem> CRYSTAL_SONG_STAFF =
            ITEMS.register("crystal_song_staff", () -> new CrystalSongStaffItem(11.0, 1.10,
                    "crystal_song_staff", new Item.Properties().durability(2000).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoMeleeWeaponItem> BROKEN_BELL_HAMMER =
            ITEMS.register("broken_bell_hammer", () -> new EchoMeleeWeaponItem(16.0, 0.85,
                    "broken_bell_hammer", new Item.Properties().durability(2300).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoBowItem> DREAM_BLOOM_BOW =
            ITEMS.register("dream_bloom_bow", () -> new EchoBowItem("dream_bloom_bow",
                    new Item.Properties().durability(960).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoMeleeWeaponItem> ECHO_OATHBLADE =
            ITEMS.register("echo_oathblade", () -> new EchoMeleeWeaponItem(13.0, 1.6,
                    "echo_oathblade", new Item.Properties().durability(3000).rarity(Rarity.EPIC)));

    // ---- V0.7U 回响基础采集工具:只改善采集体验,不授予进度权限 ----

    public static final DeferredItem<EchoPickaxeItem> ECHO_PICKAXE =
            ITEMS.register("echo_pickaxe", () -> new EchoPickaxeItem(Tiers.DIAMOND,
                    toolProperties(1561, 1.5F, -2.8F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoShovelItem> ECHO_SHOVEL =
            ITEMS.register("echo_shovel", () -> new EchoShovelItem(Tiers.DIAMOND,
                    toolProperties(1561, 1.5F, -3.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoHatchetItem> ECHO_HATCHET =
            ITEMS.register("echo_hatchet", () -> new EchoHatchetItem(Tiers.DIAMOND,
                    toolProperties(1561, 5.5F, -3.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoHoeItem> ECHO_HOE =
            ITEMS.register("echo_hoe", () -> new EchoHoeItem(Tiers.DIAMOND,
                    toolProperties(1561, -1.0F, 0.0F, Rarity.UNCOMMON)));

    public static final DeferredItem<EchoShearsItem> ECHO_SHEARS =
            ITEMS.register("echo_shears", () -> new EchoShearsItem(new Item.Properties()
                    .durability(512).rarity(Rarity.UNCOMMON)
                    .component(DataComponents.TOOL, ShearsItem.createToolProperties())));

    public static final DeferredItem<EchoFishingRodItem> ECHO_FISHING_ROD =
            ITEMS.register("echo_fishing_rod", () -> new EchoFishingRodItem(new Item.Properties()
                    .durability(384).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<EchoBrushItem> ECHO_BRUSH =
            ITEMS.register("echo_brush", () -> new EchoBrushItem(new Item.Properties()
                    .durability(256).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<EchoIgniterItem> ECHO_IGNITER =
            ITEMS.register("echo_igniter", () -> new EchoIgniterItem(new Item.Properties()
                    .durability(256).rarity(Rarity.UNCOMMON)));

    /** 装备图纸:配方线索,非关键奖励(EquipmentBlueprintItem)。 */
    public static final DeferredItem<EquipmentBlueprintItem> WIND_SPEAR_BLUEPRINT =
            ITEMS.register("wind_spear_blueprint", () -> new EquipmentBlueprintItem("wind_spear",
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<EquipmentBlueprintItem> TIDE_CROSSBOW_BLUEPRINT =
            ITEMS.register("tide_crossbow_blueprint", () -> new EquipmentBlueprintItem("tide_crossbow",
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<EquipmentBlueprintItem> TRUE_SIGHT_BLADE_BLUEPRINT =
            ITEMS.register("true_sight_blade_blueprint", () -> new EquipmentBlueprintItem("true_sight_blade",
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoKeyItem> ECHO_KEY = ITEMS.register("echo_key",
            () -> new EchoKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<EchoProjectorItem> ECHO_PROJECTOR = ITEMS.register("echo_projector",
            () -> new EchoProjectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<WindEchoMarkItem> WIND_ECHO_MARK = ITEMS.register("wind_echo_mark",
            () -> new WindEchoMarkItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<AncientPageItem> ANCIENT_PAGE = ITEMS.register("ancient_page",
            () -> new AncientPageItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                    .component(ModDataComponents.PAGE_ID.get(), 1)));

    // ---- V0.6E 共鸣信物(5.1.1 仪式凭据,凭据归属同神器序号方案;丢失可在机关核心处复领) ----

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> WIND_RESONANCE_TOKEN =
            ITEMS.register("wind_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> TIDE_RESONANCE_TOKEN =
            ITEMS.register("tide_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TIDE_ECHO,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<cn.kurt6.unknown_echoes.item.ResonanceTokenItem> TRUE_SIGHT_RESONANCE_TOKEN =
            ITEMS.register("true_sight_resonance_token", () -> new cn.kurt6.unknown_echoes.item.ResonanceTokenItem(
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---- V0.6F 普通环境生物材料(九章 9.1;普通材料允许掉落,用途见 11.2/11.3) ----

    /** 回响纤维:回声鹿掉落,探索者套装与基础合成材料。 */
    public static final DeferredItem<Item> ECHO_FIBER = ITEMS.register("echo_fiber",
            () -> new EchoMaterialItem("echo_fiber", new Item.Properties()));

    /** 荧光皮:荧光兔掉落,装饰与合成材料(微光质感)。 */
    public static final DeferredItem<Item> GLOW_HIDE = ITEMS.register("glow_hide",
            () -> new EchoMaterialItem("glow_hide", new Item.Properties()));

    // ---- V0.7B 后期群系材料 ----

    /** 晶歌碎片:晶歌林地基础材料。 */
    public static final DeferredItem<Item> CRYSTAL_SONG_SHARD = ITEMS.register("crystal_song_shard",
            () -> new EchoMaterialItem("crystal_song_shard", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 共振晶音:晶歌林地高阶晶体材料。 */
    public static final DeferredItem<Item> RESONANT_CRYSTAL_NOTE = ITEMS.register("resonant_crystal_note",
            () -> new EchoMaterialItem("resonant_crystal_note", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 天穹石英:漂浮群岛补充矿物材料,用于轻质装备和风系补给。 */
    public static final DeferredItem<Item> SKY_QUARTZ = ITEMS.register("sky_quartz",
            () -> new EchoMaterialItem("sky_quartz", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 残钟齿轮:残钟荒原基础材料。 */
    public static final DeferredItem<Item> BROKEN_BELL_GEAR = ITEMS.register("broken_bell_gear",
            () -> new EchoMaterialItem("broken_bell_gear", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 锈蚀时轮:残钟荒原高阶升级材料。 */
    public static final DeferredItem<Item> RUSTED_TIME_GEAR = ITEMS.register("rusted_time_gear",
            () -> new EchoMaterialItem("rusted_time_gear", new Item.Properties().rarity(Rarity.RARE)));

    /** 沉眠花蜜:沉眠花海补给与交易材料。 */
    public static final DeferredItem<Item> DREAM_NECTAR = ITEMS.register("dream_nectar",
            () -> new EchoMaterialItem("dream_nectar", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 星雾花瓣:轻量探索食材,来自星雾花。 */
    public static final DeferredItem<Item> STAR_MIST_PETAL = ITEMS.register("star_mist_petal",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0), 0.45F)
                    .build())));

    /** 镜莲根:镜湖岸线食材,偏向水下探索前的短补给。 */
    public static final DeferredItem<Item> MIRROR_LOTUS_ROOT = ITEMS.register("mirror_lotus_root",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3).saturationModifier(0.35F)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0), 0.35F)
                    .build())));

    /** 回响琥珀:温和矿物材料,用于补给食物和中阶交易。 */
    public static final DeferredItem<Item> ECHO_AMBER = ITEMS.register("echo_amber",
            () -> new EchoMaterialItem("echo_amber", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 潮盐:镜湖盐晶,用于食物和潮汐材料配方。 */
    public static final DeferredItem<Item> TIDE_SALT = ITEMS.register("tide_salt",
            () -> new EchoMaterialItem("tide_salt", new Item.Properties()));

    /** 风囊膜:风袋蛾掉落的轻质区域材料。 */
    public static final DeferredItem<Item> WIND_SAC_MEMBRANE = ITEMS.register("wind_sac_membrane",
            () -> new EchoMaterialItem("wind_sac_membrane", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 风织线:风囊膜与回响弦加工件,用于风系弓矛与风纹行者套装。 */
    public static final DeferredItem<Item> WINDWOVEN_THREAD = ITEMS.register("windwoven_thread",
            () -> new EchoMaterialItem("windwoven_thread", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 镜潮合金片:镜湖碎片与潮盐熔合,用于潮汐潜行套装。 */
    public static final DeferredItem<Item> MIRROR_TIDE_ALLOY_PLATE = ITEMS.register("mirror_tide_alloy_plate",
            () -> new EchoMaterialItem("mirror_tide_alloy_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 幻镜棱片:幻象尘与抛光镜尘凝结,用于真视巡影套装与真视武器。 */
    public static final DeferredItem<Item> ILLUSION_PRISM = ITEMS.register("illusion_prism",
            () -> new EchoMaterialItem("illusion_prism", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 静默树脂:噤声苔、沉默荚果与琥珀熬制的装具胶结材料。 */
    public static final DeferredItem<Item> SILENT_RESIN = ITEMS.register("silent_resin",
            () -> new EchoMaterialItem("silent_resin", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 晶歌共鸣棒:晶歌碎片与共振晶音调谐后的法杖芯材。 */
    public static final DeferredItem<Item> CRYSTAL_RESONANCE_ROD = ITEMS.register("crystal_resonance_rod",
            () -> new EchoMaterialItem("crystal_resonance_rod", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 残钟机芯板:残钟齿轮与锈蚀时轮加工件,用于重武器与残钟装备。 */
    public static final DeferredItem<Item> BROKEN_CLOCKWORK_PLATE = ITEMS.register("broken_clockwork_plate",
            () -> new EchoMaterialItem("broken_clockwork_plate", new Item.Properties().rarity(Rarity.RARE)));

    /** 调谐回响晶:把粉尘、残页与碎片压成可控的升级媒介,用于能力/神器强化核心。 */
    public static final DeferredItem<Item> ATTUNED_ECHO_GEM = ITEMS.register("attuned_echo_gem",
            () -> new EchoMaterialItem("attuned_echo_gem", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 回响武器胚:通用武器骨架,避免高阶武器全部直接吃裸锭与裸核心。 */
    public static final DeferredItem<Item> ECHO_WEAPON_FRAME = ITEMS.register("echo_weapon_frame",
            () -> new EchoMaterialItem("echo_weapon_frame", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 回响护甲板:护甲外层加工件,把回响金属、回响布与荧光皮合成稳定装甲片。 */
    public static final DeferredItem<Item> ECHO_ARMOR_PLATE = ITEMS.register("echo_armor_plate",
            () -> new EchoMaterialItem("echo_armor_plate", new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 守誓合金:终局回声锭的加工形态,仍依赖个人闭环奖励材料。 */
    public static final DeferredItem<Item> OATHBOUND_ALLOY = ITEMS.register("oathbound_alloy",
            () -> new EchoMaterialItem("oathbound_alloy", new Item.Properties().rarity(Rarity.EPIC)));

    /** 回响莓:回响森林常见食物,用于早期补给和合成。 */
    public static final DeferredItem<Item> ECHO_BERRY = ITEMS.register("echo_berry",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3).saturationModifier(0.3F).build())));

    /** 风脆饼:漂浮群岛补给,短暂缓降,不作为进度权限。 */
    public static final DeferredItem<Item> WIND_CRISP = ITEMS.register("wind_crisp",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.45F)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0), 1.0F)
                    .build())));

    /** 苔蜜羹:失语沼泽/沉眠花海混合食物,偏向持续探索恢复。 */
    public static final DeferredItem<Item> MOSS_HONEY_STEW = ITEMS.register("moss_honey_stew",
            () -> new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 80, 0), 1.0F)
                    .build())));


    /** 潮盐鱼:镜湖食物,消耗潮盐和镜尾鱼,提供更稳定的水下探索补给。 */
    public static final DeferredItem<Item> TIDE_SALTED_FISH = ITEMS.register("tide_salted_fish",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0), 1.0F)
                    .build())));

    /** 沉眠蜜挞:沉眠花海食物,消耗花蜜和回响琥珀,偏向探索回复。 */
    public static final DeferredItem<Item> DREAM_NECTAR_TART = ITEMS.register("dream_nectar_tart",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.7F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F)
                    .build())));

    public static final DeferredItem<Item> GLOW_FRUIT_JAM = ITEMS.register("glow_fruit_jam",
            () -> new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.65F)
                    .effect(() -> new MobEffectInstance(MobEffects.GLOWING, 120, 0), 0.35F)
                    .build())));

    public static final DeferredItem<Item> TIDE_GLASS_EEL = ITEMS.register("tide_glass_eel",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0), 0.45F)
                    .build())));

    public static final DeferredItem<Item> PEARL_CLAM = ITEMS.register("pearl_clam",
            () -> new EchoMaterialItem("pearl_clam", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> TIDE_KELP_WRAP = ITEMS.register("tide_kelp_wrap",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(7).saturationModifier(0.75F)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0), 1.0F)
                    .build())));

    /** 镜尾鱼:食物,食用后短暂提升水下视野(夜视 10 秒,纯表现)。 */
    public static final DeferredItem<Item> MIRROR_TAIL_FISH = ITEMS.register("mirror_tail_fish",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0), 1.0F)
                    .build())));

    /** 镜尾鱼桶:水桶捕捞镜尾鱼(原版 Bucketable 流程)。 */
    public static final DeferredItem<MobBucketItem> MIRROR_TAIL_FISH_BUCKET = ITEMS.register("mirror_tail_fish_bucket",
            () -> new MobBucketItem(ModEntities.MIRROR_TAIL_FISH.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));

    // ---- V0.6F 套装(11.3):探索者(早期)/聆听者(中期);件数效果见 EchoArmorSets ----

    public static final DeferredItem<EchoSetArmorItem> EXPLORER_HELMET = ITEMS.register("explorer_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.HELMET, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(12))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_CHESTPLATE = ITEMS.register("explorer_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.CHESTPLATE, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(12))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_LEGGINGS = ITEMS.register("explorer_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.LEGGINGS, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(12))));
    public static final DeferredItem<EchoSetArmorItem> EXPLORER_BOOTS = ITEMS.register("explorer_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.EXPLORER, ArmorItem.Type.BOOTS, "explorer",
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(12))));

    public static final DeferredItem<EchoSetArmorItem> LISTENER_HELMET = ITEMS.register("listener_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.HELMET, "listener",
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(18)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_CHESTPLATE = ITEMS.register("listener_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.CHESTPLATE, "listener",
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(18)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_LEGGINGS = ITEMS.register("listener_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.LEGGINGS, "listener",
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(18)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EchoSetArmorItem> LISTENER_BOOTS = ITEMS.register("listener_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.LISTENER, ArmorItem.Type.BOOTS, "listener",
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(18)).rarity(Rarity.UNCOMMON)));

    // ---- V0.7H 六套第一维度装备 ----

    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_HELMET = ITEMS.register("echo_traveler_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.HELMET, "echo_traveler",
                    armorProperties(ArmorItem.Type.HELMET, 36, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_CHESTPLATE = ITEMS.register("echo_traveler_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.CHESTPLATE, "echo_traveler",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 36, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_LEGGINGS = ITEMS.register("echo_traveler_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.LEGGINGS, "echo_traveler",
                    armorProperties(ArmorItem.Type.LEGGINGS, 36, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_TRAVELER_BOOTS = ITEMS.register("echo_traveler_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_TRAVELER, ArmorItem.Type.BOOTS, "echo_traveler",
                    armorProperties(ArmorItem.Type.BOOTS, 36, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_HELMET = ITEMS.register("wind_walker_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.HELMET, "wind_walker",
                    armorProperties(ArmorItem.Type.HELMET, 40, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_CHESTPLATE = ITEMS.register("wind_walker_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.CHESTPLATE, "wind_walker",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 40, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_LEGGINGS = ITEMS.register("wind_walker_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.LEGGINGS, "wind_walker",
                    armorProperties(ArmorItem.Type.LEGGINGS, 40, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> WIND_WALKER_BOOTS = ITEMS.register("wind_walker_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.WIND_WALKER, ArmorItem.Type.BOOTS, "wind_walker",
                    armorProperties(ArmorItem.Type.BOOTS, 40, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_HELMET = ITEMS.register("tide_stalker_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.HELMET, "tide_stalker",
                    armorProperties(ArmorItem.Type.HELMET, 41, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_CHESTPLATE = ITEMS.register("tide_stalker_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.CHESTPLATE, "tide_stalker",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 41, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_LEGGINGS = ITEMS.register("tide_stalker_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.LEGGINGS, "tide_stalker",
                    armorProperties(ArmorItem.Type.LEGGINGS, 41, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TIDE_STALKER_BOOTS = ITEMS.register("tide_stalker_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.TIDE_STALKER, ArmorItem.Type.BOOTS, "tide_stalker",
                    armorProperties(ArmorItem.Type.BOOTS, 41, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_HELMET = ITEMS.register("true_sight_shadow_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.HELMET, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.HELMET, 39, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_CHESTPLATE = ITEMS.register("true_sight_shadow_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.CHESTPLATE, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 39, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_LEGGINGS = ITEMS.register("true_sight_shadow_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.LEGGINGS, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.LEGGINGS, 39, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> TRUE_SIGHT_SHADOW_BOOTS = ITEMS.register("true_sight_shadow_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.TRUE_SIGHT_SHADOW, ArmorItem.Type.BOOTS, "true_sight_shadow",
                    armorProperties(ArmorItem.Type.BOOTS, 39, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_HELMET = ITEMS.register("silent_watch_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.HELMET, "silent_watch",
                    armorProperties(ArmorItem.Type.HELMET, 42, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_CHESTPLATE = ITEMS.register("silent_watch_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.CHESTPLATE, "silent_watch",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 42, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_LEGGINGS = ITEMS.register("silent_watch_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.LEGGINGS, "silent_watch",
                    armorProperties(ArmorItem.Type.LEGGINGS, 42, Rarity.RARE)));
    public static final DeferredItem<EchoSetArmorItem> SILENT_WATCH_BOOTS = ITEMS.register("silent_watch_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.SILENT_WATCH, ArmorItem.Type.BOOTS, "silent_watch",
                    armorProperties(ArmorItem.Type.BOOTS, 42, Rarity.RARE)));

    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_HELMET = ITEMS.register("echo_oath_helmet",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.HELMET, "echo_oath",
                    armorProperties(ArmorItem.Type.HELMET, 47, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_CHESTPLATE = ITEMS.register("echo_oath_chestplate",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.CHESTPLATE, "echo_oath",
                    armorProperties(ArmorItem.Type.CHESTPLATE, 47, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_LEGGINGS = ITEMS.register("echo_oath_leggings",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.LEGGINGS, "echo_oath",
                    armorProperties(ArmorItem.Type.LEGGINGS, 47, Rarity.EPIC)));
    public static final DeferredItem<EchoSetArmorItem> ECHO_OATH_BOOTS = ITEMS.register("echo_oath_boots",
            () -> new EchoSetArmorItem(ModArmorMaterials.ECHO_OATH, ArmorItem.Type.BOOTS, "echo_oath",
                    armorProperties(ArmorItem.Type.BOOTS, 47, Rarity.EPIC)));

    // BlockItems
    public static final DeferredItem<BlockItem> ECHO_STONE_BRICKS = ITEMS.registerSimpleBlockItem("echo_stone_bricks", ModBlocks.ECHO_STONE_BRICKS);
    public static final DeferredItem<BlockItem> CRACKED_ECHO_STONE_BRICKS = ITEMS.registerSimpleBlockItem("cracked_echo_stone_bricks", ModBlocks.CRACKED_ECHO_STONE_BRICKS);
    public static final DeferredItem<BlockItem> ECHO_RUNE_BRICKS = ITEMS.registerSimpleBlockItem("echo_rune_bricks", ModBlocks.ECHO_RUNE_BRICKS);
    public static final DeferredItem<BlockItem> RESONANCE_BEACON = ITEMS.registerSimpleBlockItem("resonance_beacon", ModBlocks.RESONANCE_BEACON);
    public static final DeferredItem<BlockItem> WIND_DOOR = ITEMS.registerSimpleBlockItem("wind_door", ModBlocks.WIND_DOOR);
    public static final DeferredItem<BlockItem> MEMORY_PILLAR = ITEMS.registerSimpleBlockItem("memory_pillar", ModBlocks.MEMORY_PILLAR);
    public static final DeferredItem<BlockItem> REWARD_ALTAR = ITEMS.registerSimpleBlockItem("reward_altar", ModBlocks.REWARD_ALTAR);
    public static final DeferredItem<BlockItem> HIDDEN_RUNE_BRICKS = ITEMS.registerSimpleBlockItem("hidden_rune_bricks", ModBlocks.HIDDEN_RUNE_BRICKS);
    public static final DeferredItem<BlockItem> SEQUENCE_RUNE = ITEMS.registerSimpleBlockItem("sequence_rune", ModBlocks.SEQUENCE_RUNE);
    public static final DeferredItem<BlockItem> PUZZLE_CORE = ITEMS.registerSimpleBlockItem("puzzle_core", ModBlocks.PUZZLE_CORE);
    public static final DeferredItem<BlockItem> SEALED_STONE = ITEMS.registerSimpleBlockItem("sealed_stone", ModBlocks.SEALED_STONE);
    public static final DeferredItem<BlockItem> ANCHORED_SEALED_STONE = ITEMS.registerSimpleBlockItem("anchored_sealed_stone", ModBlocks.ANCHORED_SEALED_STONE);
    public static final DeferredItem<BlockItem> ECHO_LOG = ITEMS.registerSimpleBlockItem("echo_log", ModBlocks.ECHO_LOG);
    public static final DeferredItem<BlockItem> ECHO_PLANKS = ITEMS.registerSimpleBlockItem("echo_planks", ModBlocks.ECHO_PLANKS);
    public static final DeferredItem<BlockItem> ECHO_LEAVES = ITEMS.registerSimpleBlockItem("echo_leaves", ModBlocks.ECHO_LEAVES);
    public static final DeferredItem<BlockItem> WHISPERING_LOG = ITEMS.registerSimpleBlockItem("whispering_log", ModBlocks.WHISPERING_LOG);
    public static final DeferredItem<BlockItem> WHISPERING_PLANKS = ITEMS.registerSimpleBlockItem("whispering_planks", ModBlocks.WHISPERING_PLANKS);
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
    public static final DeferredItem<BlockItem> ECHO_MOSSY_STONE = ITEMS.registerSimpleBlockItem("echo_mossy_stone", ModBlocks.ECHO_MOSSY_STONE);
    public static final DeferredItem<BlockItem> WIND_CHISELED_STONE = ITEMS.registerSimpleBlockItem("wind_chiseled_stone", ModBlocks.WIND_CHISELED_STONE);
    public static final DeferredItem<BlockItem> SKY_LAMP_GLASS = ITEMS.registerSimpleBlockItem("sky_lamp_glass", ModBlocks.SKY_LAMP_GLASS);
    public static final DeferredItem<BlockItem> TIDE_SMOOTH_STONE = ITEMS.registerSimpleBlockItem("tide_smooth_stone", ModBlocks.TIDE_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> MIRROR_SAND = ITEMS.registerSimpleBlockItem("mirror_sand", ModBlocks.MIRROR_SAND);
    public static final DeferredItem<BlockItem> PEARL_CORAL_BLOCK = ITEMS.registerSimpleBlockItem("pearl_coral_block", ModBlocks.PEARL_CORAL_BLOCK);
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

    // Spawn eggs
    public static final DeferredItem<DeferredSpawnEggItem> ECHO_WANDERER_SPAWN_EGG = ITEMS.register("echo_wanderer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ECHO_WANDERER, 0x2E4B5B, 0x7FE3D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FORGOTTEN_COLOSSUS_SPAWN_EGG = ITEMS.register("forgotten_colossus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FORGOTTEN_COLOSSUS, 0x4A4458, 0xB46CFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ABYSS_WATCHER_SPAWN_EGG = ITEMS.register("abyss_watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ABYSS_WATCHER, 0x1E4058, 0x78CDFF, new Item.Properties()));

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

    public static final DeferredItem<DeferredSpawnEggItem> MOSS_BACK_TURTLE_SPAWN_EGG = ITEMS.register("moss_back_turtle_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MOSS_BACK_TURTLE, 0x586448, 0x7C9460, new Item.Properties()));

    // ---- V0.7C 后期群系生态实体刷怪蛋 ----

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

    private static Item.Properties armorProperties(ArmorItem.Type type, int durabilityMultiplier, Rarity rarity) {
        return new Item.Properties().durability(type.getDurability(durabilityMultiplier)).rarity(rarity);
    }

    private static Item.Properties toolProperties(int durability, float attackDamage, float attackSpeed, Rarity rarity) {
        return new Item.Properties()
                .durability(durability)
                .rarity(rarity)
                .attributes(DiggerItem.createAttributes(Tiers.DIAMOND, attackDamage, attackSpeed));
    }
}
