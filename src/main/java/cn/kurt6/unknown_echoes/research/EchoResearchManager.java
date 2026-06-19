package cn.kurt6.unknown_echoes.research;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * 研究系统统一入口。研究线与基础能力权限分离,避免把研究误当成权限。
 */
public class EchoResearchManager {
    public static final int MAX_RESEARCH_LEVEL = 4;

    private EchoResearchManager() {
    }

    public static boolean isUnlocked(ServerPlayer player, EchoResearchLine line) {
        EchoAbilityType requiredAbility = line.getRequiredAbility();
        if (requiredAbility != null) {
            return EchoAbilityManager.hasAbility(player, requiredAbility);
        }
        if (line.getRequiredBiomes().isEmpty()) {
            return true;
        }
        var biomes = JournalManager.getData(player).getBiomes();
        for (String biomeId : line.getRequiredBiomes()) {
            if (biomes.contains(biomeId)) {
                return true;
            }
        }
        return false;
    }

    public static int getResearchLevel(ServerPlayer player, EchoResearchLine line) {
        if (!isUnlocked(player, line)) {
            return 0;
        }
        long pages = JournalManager.getData(player).getPages().stream()
                .filter(line.getPageIds()::contains)
                .count();
        int points = EchoAbilityManager.getData(player).getResearchPoints(line);
        return (int) Math.min(MAX_RESEARCH_LEVEL, pages + points);
    }

    public static void addResearchPoints(ServerPlayer player, EchoResearchLine line, int amount) {
        EchoAbilityManager.getData(player).addResearchPoints(line, amount);
        EchoAbilityManager.syncToClient(player);
        ModAdvancements.checkResearchAdvancements(player);
    }

    public static int getResearchLevel(ServerPlayer player, EchoAbilityType ability) {
        EchoResearchLine line = EchoResearchLine.byAbility(ability);
        return line == null ? 0 : getResearchLevel(player, line);
    }
}
