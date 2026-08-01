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

/**
 * 玩家个人回响进度数据,通过 NeoForge Data Attachment 持久化。
 * 带 dataVersion,后续新增能力时旧存档缺失字段按未解锁处理。
 */
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
    /** 个人激活过的机关(V0.5 起,如潮汐符柱),键格式 "<机关>:<维度>:<坐标>"。旧档缺失按未激活处理。 */
    private final Set<String> activatedMechanisms = new HashSet<>();
    /** 残页之外的研究点(V0.6B:拓片阅读/回访点交互/守护者首杀),键=能力 id。
     *  研究等级 = min(上限, 该能力研究残页数 + 此处研究点);旧档缺失按 0 处理。 */
    private final Map<String, Integer> researchPoints = new HashMap<>();
    /** 共鸣信物当前有效凭据序号(V0.6E 5.1.1,同神器凭据方案),键=能力 id。
     *  复领自增,旧信物/他人信物序号不符即失效;旧档缺失按 0 处理。 */
    private final Map<String, Integer> tokenSerials = new HashMap<>();
    /** 最近一次能力使用失败的含蓄提示键(5.7 面板用),键=能力 id。瞬态:不入存档。 */
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
        // 研究点键 = 研究线存储键(能力线沿用能力 id,非能力线用线 id),不能只按能力 id 白名单过滤
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

    /** 该研究线残页之外的研究点(拓片/回访/守护者来源)。 */
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

    /** 兼容旧调用:能力研究点映射到对应研究线。 */
    public int getResearchPoints(EchoAbilityType type) {
        EchoResearchLine line = EchoResearchLine.byAbility(type);
        return line == null ? researchPoints.getOrDefault(type.getId(), 0) : getResearchPoints(line);
    }

    /** 兼容旧调用:能力研究点映射到对应研究线。 */
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

    // ---- 共鸣信物凭据序号(V0.6E,同 ArtifactData 序号方案) ----

    public int getTokenSerial(EchoAbilityType type) {
        return tokenSerials.getOrDefault(type.getId(), 0);
    }

    /** 发新信物:序号自增并返回,旧序号信物从此不再通过校验。 */
    public int nextTokenSerial(EchoAbilityType type) {
        int current = getTokenSerial(type);
        int next = current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
        tokenSerials.put(type.getId(), next);
        return next;
    }

    // ---- 最近失败提示(瞬态,5.7 能力面板) ----

    /** 最近一次失败提示的 lang 键后缀;空串 = 无记录。 */
    public String getLastFailure(EchoAbilityType type) {
        return lastFailures.getOrDefault(type.getId(), "");
    }

    public void setLastFailure(EchoAbilityType type, String failureKey) {
        lastFailures.put(type.getId(), failureKey);
    }
}
