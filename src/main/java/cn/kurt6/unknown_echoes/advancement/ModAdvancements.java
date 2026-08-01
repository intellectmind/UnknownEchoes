package cn.kurt6.unknown_echoes.advancement;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * 成就授予入口:成就只读关键进度(由能力/Boss/日志事件在服务端触发授予),
 * 绝不反向授予权限或能力(设计文档 V0.5 不可做项)。
 * 进度类成就用 minecraft:impossible 触发器 + 代码授予,避免客户端伪造。
 */
public class ModAdvancements {

    public static final String FIRST_BEACON = "first_beacon";
    public static final String ENTER_ECHO_REALM = "enter_echo_realm";
    public static final String FIRST_PAGE = "first_page";
    public static final String PAGES_10 = "pages_10";
    public static final String ALL_PAGES = "all_pages";
    public static final String FIRST_RUIN = "first_ruin";
    public static final String EXPLORER_5 = "explorer_5";
    public static final String ALL_BIOMES = "all_biomes";
    public static final String GRAND_ARCHIVE = "grand_archive";
    public static final String ECHO_REALM_COMPLETE = "echo_realm_complete";
    public static final String ECHO_REALM_GRADUATION = "echo_realm_graduation";
    public static final String ENTER_DEEP_ECHOES = "enter_deep_echoes";
    public static final String ALL_DEEP_BIOMES = "all_deep_biomes";
    public static final String DEEP_MICRO_SITES = "deep_micro_sites";
    public static final String WORLD_ECHO_COMPLETE = "world_echo_complete";
    private static final String ENTERED_ECHO_REALM_KEY = "advancement:entered_echo_realm_via_beacon";
    private static final String ENTERED_DEEP_ECHOES_KEY = "advancement:entered_deep_echoes_via_beacon";

    private static final Set<String> WORLD_WONDERS = Set.of(
            "unknown_echoes:echo_world_tree",
            "unknown_echoes:eternal_echo_lighthouse",
            "unknown_echoes:mirror_sea",
            "unknown_echoes:inverted_mountains",
            "unknown_echoes:sky_rift",
            "unknown_echoes:silent_great_boat",
            "unknown_echoes:broken_bell_tower");
    private static final Set<String> DEEP_MICRO_SITE_IDS = Set.of(
            "unknown_echoes:folded_waystation",
            "unknown_echoes:gravity_shard_spire",
            "unknown_echoes:hushed_refuge");

    /** 按路径授予成就(advancement JSON 的 criteria 全部补完)。 */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements()
                .get(UnknownEchoes.id(path));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }

    public static void awardAbility(ServerPlayer player, String abilityId) {
        award(player, "ability_" + abilityId);
    }

    public static void awardBossDefeat(ServerPlayer player, String bossPath) {
        award(player, "defeat_" + bossPath);
    }

    /** 登录、修档和数据同步时按服务端事实补发主线成就，不写入任何玩法进度。 */
    public static void checkMainlineAdvancements(ServerPlayer player) {
        if (EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID)) {
            award(player, FIRST_BEACON);
        }
        if (EchoAbilityManager.hasActivatedMechanism(player, ENTERED_ECHO_REALM_KEY)) {
            award(player, ENTER_ECHO_REALM);
        }
        awardBossIfDefeated(player, EchoPermission.FORGOTTEN_COLOSSUS_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.WIND_ECHO);
        awardBossIfDefeated(player, EchoPermission.ABYSS_WATCHER_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.TIDE_ECHO);
        awardBossIfDefeated(player, EchoPermission.MIRROR_GUARDIAN_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.TRUE_SIGHT_ECHO);
        if (EchoCompletionManager.hasArchiveTerminalRecord(player)) {
            award(player, GRAND_ARCHIVE);
        }
        if (EchoCompletionManager.hasMainlineCompletionRecord(player)) {
            award(player, ECHO_REALM_GRADUATION);
        }
        if (EchoAbilityManager.hasActivatedMechanism(player, ENTERED_DEEP_ECHOES_KEY)) {
            award(player, ENTER_DEEP_ECHOES);
        }
        awardBossIfDefeated(player, EchoPermission.VOID_PROPHET_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.GRAVITY_ECHO);
        awardBossIfDefeated(player, EchoPermission.RIFT_WARDEN_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.VOID_ECHO);
        awardBossIfDefeated(player, EchoPermission.WORLD_ECHO_ID);
        if (EchoAbilityManager.hasActivatedMechanism(player,
                EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            award(player, WORLD_ECHO_COMPLETE);
        }
    }

    private static void awardBossIfDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (EchoAbilityManager.hasDefeatedBoss(player, bossId)) {
            awardBossDefeat(player, bossId.getPath());
        }
    }

    private static void awardAbilityIfUnlocked(ServerPlayer player, EchoAbilityType ability) {
        if (EchoAbilityManager.hasAbility(player, ability)) {
            awardAbility(player, ability.getId());
        }
    }

    /** 只由残响信标在传送成功后调用，其他 Mod 或管理员传送不计入。 */
    public static void recordEchoRealmEntry(ServerPlayer player) {
        if (player.isSpectator()
                || !player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)
                || !EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID)
                || !EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.ECHO_REALM_ID)) {
            return;
        }
        if (EchoAbilityManager.activateMechanism(player, ENTERED_ECHO_REALM_KEY)) {
            award(player, ENTER_ECHO_REALM);
        }
    }

    /** 只由校准完成的深渊信标在传送成功后调用，测试传送不计入。 */
    public static void recordDeepEchoesEntry(ServerPlayer player) {
        if (player.isSpectator()
                || !player.level().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID)
                || !EchoPermission.canEnterDeepEchoes(player)
                || !EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.ABYSS_BEACON_ID)
                || !EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.DEEP_ECHOES_ID)) {
            return;
        }
        if (EchoAbilityManager.activateMechanism(player, ENTERED_DEEP_ECHOES_KEY)) {
            award(player, ENTER_DEEP_ECHOES);
        }
    }

    /** 登录修档时只读既有日志事实补发成就，不写入探索、能力或权限进度。 */
    public static void checkJournalAdvancements(ServerPlayer player) {
        var journal = JournalManager.getData(player);
        int pageCount = AncientPageCatalog.countReleasePages(journal.getPages());
        if (pageCount >= 1) {
            award(player, FIRST_PAGE);
        }
        if (pageCount >= 10) {
            award(player, PAGES_10);
        }
        if (pageCount >= JournalManager.KNOWN_PAGES) {
            award(player, ALL_PAGES);
        }

        Set<String> structures = journal.getStructures();
        if (!structures.isEmpty()) {
            award(player, FIRST_RUIN);
        }
        if (structures.size() >= 5) {
            award(player, EXPLORER_5);
        }
        for (String structureId : structures) {
            awardStructureDiscovery(player, structureId, structures);
        }
        if (EchoRealmBiomeCatalog.hasVisitedAll(journal.getBiomes())) {
            award(player, ALL_BIOMES);
        }
        checkDeepExplorationAdvancements(player, structures, journal.getBiomes());
        checkResearchAdvancements(player);
        if (EchoCompletionManager.hasClaimedT7Reward(player)) {
            award(player, ECHO_REALM_COMPLETE);
        }
    }

    public static void awardStructureDiscovery(ServerPlayer player, String structureId, Set<String> discovered) {
        switch (structureId) {
            case "unknown_echoes:echo_world_tree" -> award(player, "world_tree");
            case "unknown_echoes:mirror_sea" -> award(player, "mirror_sea");
            case "unknown_echoes:sky_rift" -> award(player, "sky_rift");
            case "unknown_echoes:silent_great_boat" -> award(player, "silent_ark");
            case "unknown_echoes:broken_bell_tower" -> award(player, "broken_bell_tower");
            default -> {
            }
        }
        long foundWonders = WORLD_WONDERS.stream().filter(discovered::contains).count();
        if (foundWonders >= 5) {
            award(player, "all_wonders");
        }
    }

    /** 根据服务端日志补发深层探索成就，不改变任何玩法权限。 */
    public static void checkDeepExplorationAdvancements(ServerPlayer player,
                                                        Set<String> discoveredStructures,
                                                        Set<String> visitedBiomes) {
        if (!EchoPermission.canEnterDeepEchoes(player)) {
            return;
        }
        if (DeepEchoesBiomeCatalog.hasVisitedAll(visitedBiomes)) {
            award(player, ALL_DEEP_BIOMES);
            if (discoveredStructures.containsAll(DEEP_MICRO_SITE_IDS)) {
                award(player, DEEP_MICRO_SITES);
            }
        }
    }

    public static void checkResearchAdvancements(ServerPlayer player) {
        boolean anyResearch = false;
        boolean anyLevelFour = false;
        boolean allBase = true;
        boolean allLines = true;
        for (EchoResearchLine line : EchoResearchLine.values()) {
            int level = EchoResearchManager.getResearchLevel(player, line);
            anyResearch |= level >= 1;
            anyLevelFour |= level >= EchoResearchManager.MAX_RESEARCH_LEVEL;
            allLines &= level >= EchoResearchManager.MAX_RESEARCH_LEVEL;
            if (line == EchoResearchLine.WIND
                    || line == EchoResearchLine.TIDE
                    || line == EchoResearchLine.TRUE_SIGHT) {
                allBase &= level >= EchoResearchManager.MAX_RESEARCH_LEVEL;
            }
        }
        if (anyResearch) {
            award(player, "first_research");
        }
        if (anyLevelFour) {
            award(player, "research_level_4");
        }
        if (allBase) {
            award(player, "all_base_research");
        }
        if (allLines) {
            award(player, "all_research");
        }
    }
}
