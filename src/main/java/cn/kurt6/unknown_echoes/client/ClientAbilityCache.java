package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.network.SyncAbilityPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端能力缓存,仅用于表现层(二段跳手感、滑翔、HUD、能力面板)。
 * 服务端判定永远以玩家数据为准,客户端不能自行声明解锁。
 */
public class ClientAbilityCache {
    private static final Set<String> ABILITIES = new HashSet<>();
    private static final Set<String> BEACONS = new HashSet<>();
    private static final Set<String> DIMENSIONS = new HashSet<>();
    private static final Map<String, Integer> RESEARCH = new HashMap<>();
    /** 已完成核心机关仪式的能力 id(V0.6E 凭据与誓记页;只是布尔事实,不含坐标)。 */
    private static final Set<String> RITUAL_TOKENS = new HashSet<>();
    /** 最近失败提示键(V0.6E 能力面板),键=能力 id。 */
    private static final Map<String, String> FAILURES = new HashMap<>();

    public static void update(SyncAbilityPayload payload) {
        ABILITIES.clear();
        ABILITIES.addAll(payload.abilities());
        BEACONS.clear();
        BEACONS.addAll(payload.beacons());
        DIMENSIONS.clear();
        DIMENSIONS.addAll(payload.dimensions());
        RESEARCH.clear();
        for (String entry : payload.research()) {
            int sep = entry.lastIndexOf(':');
            if (sep > 0) {
                try {
                    RESEARCH.put(entry.substring(0, sep), Integer.parseInt(entry.substring(sep + 1)));
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

    public static void clear() {
        ABILITIES.clear();
        BEACONS.clear();
        DIMENSIONS.clear();
        RESEARCH.clear();
        RITUAL_TOKENS.clear();
        FAILURES.clear();
    }
}
