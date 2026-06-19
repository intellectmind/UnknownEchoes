package cn.kurt6.unknown_echoes.research;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 基础能力研究线。研究只强化玩法表现与数值,不授予 EchoPermission。
 */
public enum EchoResearchLine {
    WIND("wind", "wind_echo", EchoAbilityType.WIND_ECHO, Set.of(17, 18, 19, 20, 21, 22, 23, 24), Set.of()),
    TIDE("tide", "tide_echo", EchoAbilityType.TIDE_ECHO, Set.of(42, 43, 44, 45, 46, 47, 48, 49, 50, 51), Set.of()),
    TRUE_SIGHT("true_sight", "true_sight_echo", EchoAbilityType.TRUE_SIGHT_ECHO,
            Set.of(56, 57, 58, 59, 60, 61, 62, 63, 64, 65), Set.of());

    private final String id;
    private final String legacyResearchKey;
    @Nullable
    private final EchoAbilityType requiredAbility;
    private final Set<Integer> pageIds;
    private final Set<String> requiredBiomes;

    EchoResearchLine(String id, String legacyResearchKey, @Nullable EchoAbilityType requiredAbility,
                     Set<Integer> pageIds, Set<String> requiredBiomes) {
        this.id = id;
        this.legacyResearchKey = legacyResearchKey;
        this.requiredAbility = requiredAbility;
        this.pageIds = pageIds;
        this.requiredBiomes = requiredBiomes;
    }

    public String getId() {
        return id;
    }

    /** 旧档 research_points 使用能力 id;前三条继续沿用,避免迁移破坏旧数据。 */
    public String getStorageKey() {
        return requiredAbility == null ? id : requiredAbility.getId();
    }

    public String getLegacyResearchKey() {
        return legacyResearchKey;
    }

    @Nullable
    public EchoAbilityType getRequiredAbility() {
        return requiredAbility;
    }

    public Set<Integer> getPageIds() {
        return pageIds;
    }

    public Set<String> getRequiredBiomes() {
        return requiredBiomes;
    }

    public boolean isAbilityGated() {
        return requiredAbility != null;
    }

    @Nullable
    public static EchoResearchLine byId(String id) {
        for (EchoResearchLine line : values()) {
            if (line.id.equals(id) || line.legacyResearchKey.equals(id)) {
                return line;
            }
        }
        return null;
    }

    @Nullable
    public static EchoResearchLine byAbility(EchoAbilityType ability) {
        for (EchoResearchLine line : values()) {
            if (line.requiredAbility == ability) {
                return line;
            }
        }
        return null;
    }
}
