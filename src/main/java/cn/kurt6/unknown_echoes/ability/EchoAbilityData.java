package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EchoAbilityData {
    public static final int CURRENT_DATA_VERSION = 4;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_STANDARD_ENTRIES = 1024;
    private static final int MAX_MECHANISM_ENTRIES = 16384;
    private static final int MAX_MECHANISM_LENGTH = 512;
    private static final Codec<String> ID_CODEC = Codec.sizeLimitedString(MAX_ID_LENGTH);
    private static final Codec<String> MECHANISM_CODEC = Codec.sizeLimitedString(MAX_MECHANISM_LENGTH);

    public static final Codec<EchoAbilityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            ID_CODEC.listOf(0, EchoAbilityType.values().length).optionalFieldOf("unlocked_abilities", List.of())
                    .forGetter(d -> List.copyOf(d.unlockedAbilities)),
            ID_CODEC.listOf(0, MAX_STANDARD_ENTRIES).optionalFieldOf("activated_beacons", List.of())
                    .forGetter(d -> List.copyOf(d.activatedBeacons)),
            ID_CODEC.listOf(0, MAX_STANDARD_ENTRIES).optionalFieldOf("defeated_bosses", List.of())
                    .forGetter(d -> List.copyOf(d.defeatedBosses)),
            ID_CODEC.listOf(0, MAX_STANDARD_ENTRIES).optionalFieldOf("unlocked_dimensions", List.of())
                    .forGetter(d -> List.copyOf(d.unlockedDimensions)),
            MECHANISM_CODEC.listOf(0, MAX_MECHANISM_ENTRIES).optionalFieldOf("activated_mechanisms", List.of())
                    .forGetter(d -> List.copyOf(d.activatedMechanisms)),
            Codec.unboundedMap(ID_CODEC, Codec.INT).optionalFieldOf("research_points", Map.of())
                    .forGetter(d -> Map.copyOf(d.researchPoints)),
            Codec.unboundedMap(ID_CODEC, Codec.INT).optionalFieldOf("token_serials", Map.of())
                    .forGetter(d -> Map.copyOf(d.tokenSerials))
    ).apply(instance, EchoAbilityData::new));

    private int dataVersion;
    private final Set<String> unlockedAbilities = new HashSet<>();
    private final Set<String> activatedBeacons = new HashSet<>();
    private final Set<String> defeatedBosses = new HashSet<>();
    private final Set<String> unlockedDimensions = new HashSet<>();
    /** 个人激活过的机关，键格式为“<机关>:<维度>:<坐标>”。 */
    private final Set<String> activatedMechanisms = new HashSet<>();
    /** 残页之外的研究点(拓片阅读/回访点交互/守护者首杀),键=能力 id。 */
    private final Map<String, Integer> researchPoints = new HashMap<>();
    /** 共鸣信物当前有效凭据序号，按能力 id 保存。 */
    private final Map<String, Integer> tokenSerials = new HashMap<>();
    /** 最近一次能力使用失败的提示键，按能力 id 保存；仅用于界面展示。 */
    private final transient Map<String, String> lastFailures = new HashMap<>();

    public EchoAbilityData() {
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public EchoAbilityData(int dataVersion, List<String> abilities, List<String> beacons,
                           List<String> bosses, List<String> dimensions, List<String> mechanisms,
                           Map<String, Integer> researchPoints, Map<String, Integer> tokenSerials) {
        this.dataVersion = CURRENT_DATA_VERSION;
        for (String id : abilities) {
            if (EchoAbilityType.byId(id) != null) {
                this.unlockedAbilities.add(id);
            }
        }
        copyResourceIds(beacons, this.activatedBeacons);
        copyResourceIds(bosses, this.defeatedBosses);
        copyResourceIds(dimensions, this.unlockedDimensions);
        for (String mechanism : mechanisms) {
            if (mechanism != null && !mechanism.isBlank() && mechanism.length() <= MAX_MECHANISM_LENGTH) {
                this.activatedMechanisms.add(mechanism);
                if (this.activatedMechanisms.size() >= MAX_MECHANISM_ENTRIES) {
                    break;
                }
            }
        }

        java.util.Set<String> researchKeys = new java.util.LinkedHashSet<>();
        for (EchoAbilityType type : EchoAbilityType.values()) {
            researchKeys.add(type.getId());
        }
        for (cn.kurt6.unknown_echoes.research.EchoResearchLine line
                : cn.kurt6.unknown_echoes.research.EchoResearchLine.values()) {
            researchKeys.add(line.getStorageKey());
        }
        for (String id : researchKeys) {
            int points = Math.max(0, Math.min(8, researchPoints.getOrDefault(id, 0)));
            if (points > 0) {
                this.researchPoints.put(id, points);
            }
        }
        for (EchoAbilityType type : EchoAbilityType.values()) {
            String id = type.getId();
            int serial = Math.max(0, tokenSerials.getOrDefault(id, 0));
            if (serial > 0) {
                this.tokenSerials.put(id, serial);
            }
        }
    }

    private static void copyResourceIds(List<String> source, Set<String> target) {
        for (String id : source) {
            if (ResourceLocation.tryParse(id) != null) {
                target.add(id);
            }
        }
    }

    public Set<String> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    public Set<String> getActivatedBeacons() {
        return activatedBeacons;
    }

    public boolean activateBeacon(String beaconId) {
        return addResourceId(activatedBeacons, beaconId);
    }

    public Set<String> getDefeatedBosses() {
        return defeatedBosses;
    }

    public boolean markBossDefeated(String bossId) {
        return addResourceId(defeatedBosses, bossId);
    }

    /** Boss 击败与同一次结算产生的机关记录必须同时落盘。 */
    public boolean markBossDefeatedWithMechanisms(String bossId, Collection<String> mechanisms) {
        if (!canStoreResourceId(defeatedBosses, bossId)) {
            return false;
        }
        Set<String> additions = new LinkedHashSet<>();
        if (mechanisms != null) {
            for (String mechanism : mechanisms) {
                if (mechanism == null || mechanism.isBlank()
                        || mechanism.length() > MAX_MECHANISM_LENGTH) {
                    return false;
                }
                if (!activatedMechanisms.contains(mechanism)) {
                    additions.add(mechanism);
                }
            }
        }
        if (activatedMechanisms.size() + additions.size() > MAX_MECHANISM_ENTRIES) {
            return false;
        }
        defeatedBosses.add(bossId);
        activatedMechanisms.addAll(additions);
        return true;
    }

    public Set<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    public boolean unlockDimension(String dimensionId) {
        return addResourceId(unlockedDimensions, dimensionId);
    }

    public Set<String> getActivatedMechanisms() {
        return activatedMechanisms;
    }

    /** 运行时写入必须与 Codec 上限一致，避免附件在下次保存时编码失败。 */
    public boolean activateMechanism(String mechanism) {
        if (mechanism == null || mechanism.isBlank() || mechanism.length() > MAX_MECHANISM_LENGTH) {
            return false;
        }
        if (activatedMechanisms.contains(mechanism)) {
            return true;
        }
        return activatedMechanisms.size() < MAX_MECHANISM_ENTRIES
                && activatedMechanisms.add(mechanism);
    }

    /** 多条关联记录必须同时落盘，避免容量边界留下半完成状态。 */
    public boolean activateMechanisms(Collection<String> mechanisms) {
        if (mechanisms == null || mechanisms.isEmpty()) {
            return false;
        }
        Set<String> additions = new LinkedHashSet<>();
        for (String mechanism : mechanisms) {
            if (mechanism == null || mechanism.isBlank() || mechanism.length() > MAX_MECHANISM_LENGTH) {
                return false;
            }
            if (!activatedMechanisms.contains(mechanism)) {
                additions.add(mechanism);
            }
        }
        if (activatedMechanisms.size() + additions.size() > MAX_MECHANISM_ENTRIES) {
            return false;
        }
        activatedMechanisms.addAll(additions);
        return true;
    }

    // 预留键等量换入完成记录，避免挑战期间其他进度填满附件后无法记录通关。
    public boolean completeReservedMechanisms(Map<String, String> replacements) {
        if (replacements == null || replacements.isEmpty()) return false;
        for (var entry : replacements.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            if (source == null || source.isBlank() || source.length() > MAX_MECHANISM_LENGTH
                    || target == null || target.isBlank() || target.length() > MAX_MECHANISM_LENGTH
                    || (!activatedMechanisms.contains(target) && !activatedMechanisms.contains(source))) return false;
        }
        Set<String> result = new HashSet<>(activatedMechanisms);
        result.removeAll(replacements.keySet());
        result.addAll(replacements.values());
        if (result.size() > MAX_MECHANISM_ENTRIES) return false;
        activatedMechanisms.clear();
        activatedMechanisms.addAll(result);
        return true;
    }

    /** 信标激活与维度权限必须同时成立，可附带同一流程的完成记录。 */
    public boolean activateBeaconRoute(String beaconId, String dimensionId,
                                       Collection<String> mechanisms) {
        if (!canStoreResourceId(activatedBeacons, beaconId)
                || !canStoreResourceId(unlockedDimensions, dimensionId)) {
            return false;
        }
        Set<String> additions = new LinkedHashSet<>();
        if (mechanisms != null) {
            for (String mechanism : mechanisms) {
                if (mechanism == null || mechanism.isBlank()
                        || mechanism.length() > MAX_MECHANISM_LENGTH) {
                    return false;
                }
                if (!activatedMechanisms.contains(mechanism)) {
                    additions.add(mechanism);
                }
            }
        }
        if (activatedMechanisms.size() + additions.size() > MAX_MECHANISM_ENTRIES) {
            return false;
        }
        activatedBeacons.add(beaconId);
        unlockedDimensions.add(dimensionId);
        activatedMechanisms.addAll(additions);
        return true;
    }

    private static boolean addResourceId(Set<String> target, String id) {
        if (!isValidResourceId(id)
                || target.contains(id) || target.size() >= MAX_STANDARD_ENTRIES) {
            return false;
        }
        return target.add(id);
    }

    private static boolean canStoreResourceId(Set<String> target, String id) {
        return isValidResourceId(id)
                && (target.contains(id) || target.size() < MAX_STANDARD_ENTRIES);
    }

    private static boolean isValidResourceId(String id) {
        return id != null && id.length() <= MAX_ID_LENGTH
                && ResourceLocation.tryParse(id) != null;
    }

    public int getResearchPoints(EchoResearchLine line) {
        return researchPoints.getOrDefault(line.getStorageKey(), 0);
    }

    /** 累加研究点。存储上限 8(等级封顶之外留少量冗余,防数据无界增长)。 */
    public void addResearchPoints(EchoResearchLine line, int amount) {
        if (amount <= 0) {
            return;
        }
        int next = (int) Math.min(8L, (long) getResearchPoints(line) + amount);
        researchPoints.put(line.getStorageKey(), next);
    }

    public int getResearchPoints(EchoAbilityType type) {
        EchoResearchLine line = EchoResearchLine.byAbility(type);
        return line == null ? researchPoints.getOrDefault(type.getId(), 0) : getResearchPoints(line);
    }

    public void addResearchPoints(EchoAbilityType type, int amount) {
        if (amount <= 0) {
            return;
        }
        EchoResearchLine line = EchoResearchLine.byAbility(type);
        if (line == null) {
            int next = (int) Math.min(8L, (long) getResearchPoints(type) + amount);
            researchPoints.put(type.getId(), next);
            return;
        }
        addResearchPoints(line, amount);
    }

    public boolean hasAbility(EchoAbilityType type) {
        return unlockedAbilities.contains(type.getId());
    }

    public boolean unlockAbility(EchoAbilityType type) {
        return unlockedAbilities.add(type.getId());
    }

    public int getTokenSerial(EchoAbilityType type) {
        return tokenSerials.getOrDefault(type.getId(), 0);
    }

    /** 发新信物:序号自增并返回,旧序号信物从此不再通过校验。 */
    public int nextTokenSerial(EchoAbilityType type) {
        int current = getTokenSerial(type);
        if (current == Integer.MAX_VALUE) {
            return 0;
        }
        int next = current + 1;
        tokenSerials.put(type.getId(), next);
        return next;
    }

    /** 最近一次失败提示的 lang 键后缀;空串 = 无记录。 */
    public String getLastFailure(EchoAbilityType type) {
        return lastFailures.getOrDefault(type.getId(), "");
    }

    public void setLastFailure(EchoAbilityType type, String failureKey) {
        lastFailures.put(type.getId(), failureKey);
    }
}
