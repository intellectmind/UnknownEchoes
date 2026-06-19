package cn.kurt6.unknown_echoes.advancement;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * 成就授予入口:成就只读关键进度(由能力/Boss/日志事件在服务端触发授予),
 * 绝不反向授予权限或能力(设计文档 V0.5 不可做项)。
 * 进度类成就用 minecraft:impossible 触发器 + 代码授予,避免客户端伪造。
 */
public class ModAdvancements {

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

    private static final Set<String> WORLD_WONDERS = Set.of(
            "unknown_echoes:echo_world_tree",
            "unknown_echoes:eternal_echo_lighthouse",
            "unknown_echoes:mirror_sea",
            "unknown_echoes:inverted_mountains",
            "unknown_echoes:sky_rift",
            "unknown_echoes:silent_great_boat",
            "unknown_echoes:broken_bell_tower");

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

    public static void awardStructureDiscovery(ServerPlayer player, String structureId, Set<String> discovered) {
        switch (structureId) {
            case "unknown_echoes:echo_world_tree" -> award(player, "world_tree");
            case "unknown_echoes:mirror_sea" -> award(player, "mirror_sea");
            case "unknown_echoes:sky_rift" -> award(player, "sky_rift");
            case "unknown_echoes:silent_great_boat" -> award(player, "silent_ark");
            case "unknown_echoes:broken_bell_tower" -> award(player, "broken_bell_tower");
            case "unknown_echoes:broken_archive" -> award(player, GRAND_ARCHIVE);
            default -> {
            }
        }
        long foundWonders = WORLD_WONDERS.stream().filter(discovered::contains).count();
        if (foundWonders >= 5) {
            award(player, "all_wonders");
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
