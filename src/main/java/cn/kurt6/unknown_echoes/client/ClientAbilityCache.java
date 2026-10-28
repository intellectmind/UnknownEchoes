package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.network.SyncAbilityPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 客户端能力缓存,仅用于表现层(二段跳手感、滑翔、HUD、能力面板)。 */
public class ClientAbilityCache {
    private static final Set<String> ABILITIES = new HashSet<>();
    private static final Set<String> BEACONS = new HashSet<>();
    private static final Set<String> DIMENSIONS = new HashSet<>();
    private static final Map<String, Integer> RESEARCH = new HashMap<>();
    private static final Set<String> RITUAL_TOKENS = new HashSet<>();
    private static final Map<String, String> FAILURES = new HashMap<>();
    private static int puzzleScore;
    private static int archiveProgress;

    public static void update(SyncAbilityPayload payload) {
        ABILITIES.clear();
        ABILITIES.addAll(payload.abilities());
        BEACONS.clear();
        BEACONS.addAll(payload.beacons());
        DIMENSIONS.clear();
        DIMENSIONS.addAll(payload.dimensions());
        RESEARCH.clear();
        puzzleScore = 0;
        archiveProgress = 0;
        for (String entry : payload.research()) {
            int sep = entry.lastIndexOf(':');
            if (sep > 0) {
                try {
                    int value = Integer.parseInt(entry.substring(sep + 1));
                    if (entry.startsWith("@puzzle_score:")) {
                        puzzleScore = Math.max(0, value);
                    } else if (entry.startsWith("@archive_progress:")) {
                        archiveProgress = Math.max(0, value) & 3;
                    } else {
                        RESEARCH.put(entry.substring(0, sep), value);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        RITUAL_TOKENS.clear();
        RITUAL_TOKENS.addAll(payload.ritualTokens());
        FAILURES.clear();
        for (String entry : payload.failures()) {
            int sep = entry.indexOf(':');
            if (sep > 0) {
                FAILURES.put(entry.substring(0, sep), entry.substring(sep + 1));
            }
        }
    }

    public static boolean hasAbility(EchoAbilityType type) {
        return ABILITIES.contains(type.getId());
    }

    /** 研究等级(由残页推导,服务端同步;仅表现层使用)。 */
    public static int getResearchLevel(EchoAbilityType type) {
        return RESEARCH.getOrDefault(type.getId(), 0);
    }

    /** 六研究线等级。前三条同步时仍使用 ability id,这里兼容映射。 */
    public static int getResearchLevel(EchoResearchLine line) {
        return RESEARCH.getOrDefault(line.getLegacyResearchKey(),
                RESEARCH.getOrDefault(line.getId(), 0));
    }

    /** 该能力的核心机关仪式是否已完成(凭据与誓记页展示用)。 */
    public static boolean hasRitualRecord(EchoAbilityType type) {
        return RITUAL_TOKENS.contains(type.getId());
    }

    /** 最近失败提示键(空串 = 无);面板显示 overview.unknown_echoes.failure.<key>。 */
    public static String getLastFailure(EchoAbilityType type) {
        return FAILURES.getOrDefault(type.getId(), "");
    }

    public static int getPuzzleScore() {
        return puzzleScore;
    }

    public static boolean hasArchiveTerminalRecord() {
        return (archiveProgress & 1) != 0;
    }

    public static boolean hasCompletedArchiveTrials() {
        return (archiveProgress & 2) != 0;
    }

    public static void clear() {
        ABILITIES.clear();
        BEACONS.clear();
        DIMENSIONS.clear();
        RESEARCH.clear();
        RITUAL_TOKENS.clear();
        FAILURES.clear();
        puzzleScore = 0;
        archiveProgress = 0;
    }
}
