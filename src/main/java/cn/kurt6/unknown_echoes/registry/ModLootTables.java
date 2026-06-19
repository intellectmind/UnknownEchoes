package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModLootTables {
    public static final ResourceKey<LootTable> T1_BASIC =
            chest("common/t1_basic");
    public static final ResourceKey<LootTable> T2_RUIN =
            chest("common/t2_ruin");
    public static final ResourceKey<LootTable> T3_PUZZLE =
            chest("common/t3_puzzle");
    public static final ResourceKey<LootTable> T4_REGION_CORE =
            chest("common/t4_region_core");
    public static final ResourceKey<LootTable> T5_MINIBOSS_REPEAT =
            chest("common/t5_miniboss_repeat");
    public static final ResourceKey<LootTable> T6_MAIN_BOSS =
            chest("common/t6_main_boss");
    public static final ResourceKey<LootTable> T7_ECHO_REALM_ENDGAME =
            chest("common/t7_echo_realm_endgame");

    public static final ResourceKey<LootTable> SMALL_ECHO_RUIN_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/small_echo_ruin"));

    public static final ResourceKey<LootTable> WIND_VAULT_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/wind_vault"));

    public static final ResourceKey<LootTable> ECHO_TEMPLE_TREASURE =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/echo_temple_treasure"));

    public static final ResourceKey<LootTable> ECHO_TEMPLE_ARCHIVE =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/echo_temple_archive"));

    public static final ResourceKey<LootTable> ECHO_TEMPLE_HIDDEN =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/echo_temple_hidden"));

    public static final ResourceKey<LootTable> MIRROR_LAKE_CACHE =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/mirror_lake_cache"));

    public static final ResourceKey<LootTable> SILENT_HUT_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/silent_hut"));

    public static final ResourceKey<LootTable> SILENT_RING_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/silent_ring"));

    public static final ResourceKey<LootTable> SKY_OBSERVATORY_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/sky_observatory"));

    public static final ResourceKey<LootTable> BROKEN_ARCHIVE_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/broken_archive"));

    public static final ResourceKey<LootTable> TIDE_LIGHTHOUSE_REEF_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/tide_lighthouse_reef"));

    // ---- V0.6E ----

    /** 倒影回廊深层内室(潮汐感应门之后)。 */
    public static final ResourceKey<LootTable> REFLECTION_INNER_VAULT =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/reflection_inner_vault"));

    /** 淹没记录室(镜湖水下残页+研究拓片点)。 */
    public static final ResourceKey<LootTable> SUBMERGED_RECORD_ROOM =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/submerged_record_room"));

    /** 真视宝箱(强化材料+研究拓片;交互前置真视权限)。 */
    public static final ResourceKey<LootTable> TRUE_SIGHT_VAULT =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/true_sight_vault"));

    // ---- V0.7 主世界探索点补齐 ----

    /** 主世界失落营地行囊:普通材料+残页+偶见指向最近遗迹的潦草地图。 */
    public static final ResourceKey<LootTable> OVERWORLD_LOST_CAMP_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/overworld_lost_camp"));

    /** 空间裂隙静态点:日志线索残页+少量材料(刻意稀薄)。 */
    public static final ResourceKey<LootTable> SPATIAL_RIFT_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/spatial_rift"));

    private static ResourceKey<LootTable> chest(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, UnknownEchoes.id("chests/" + path));
    }
}
