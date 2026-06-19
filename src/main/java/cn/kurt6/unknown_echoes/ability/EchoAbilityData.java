package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 玩家个人回响进度数据,通过 NeoForge Data Attachment 持久化。
 * 带 dataVersion,后续新增能力时旧存档缺失字段按未解锁处理。
 */
public class EchoAbilityData {
    public static final int CURRENT_DATA_VERSION = 4;

    public static final Codec<EchoAbilityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            Codec.STRING.listOf().optionalFieldOf("unlocked_abilities", List.of()).forGetter(d -> List.copyOf(d.unlockedAbilities)),
            Codec.STRING.listOf().optionalFieldOf("activated_beacons", List.of()).forGetter(d -> List.copyOf(d.activatedBeacons)),
            Codec.STRING.listOf().optionalFieldOf("defeated_bosses", List.of()).forGetter(d -> List.copyOf(d.defeatedBosses)),
            Codec.STRING.listOf().optionalFieldOf("unlocked_dimensions", List.of()).forGetter(d -> List.copyOf(d.unlockedDimensions)),
            Codec.STRING.listOf().optionalFieldOf("activated_mechanisms", List.of()).forGetter(d -> List.copyOf(d.activatedMechanisms)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("research_points", Map.of()).forGetter(d -> Map.copyOf(d.researchPoints)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("token_serials", Map.of()).forGetter(d -> Map.copyOf(d.tokenSerials))
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
        this.unlockedAbilities.addAll(abilities);
        this.activatedBeacons.addAll(beacons);
        this.defeatedBosses.addAll(bosses);
        this.unlockedDimensions.addAll(dimensions);
        this.activatedMechanisms.addAll(mechanisms);
        this.researchPoints.putAll(researchPoints);
        this.tokenSerials.putAll(tokenSerials);
    }

    public Set<String> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    public Set<String> getActivatedBeacons() {
        return activatedBeacons;
    }

    public Set<String> getDefeatedBosses() {
        return defeatedBosses;
    }

    public Set<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    public Set<String> getActivatedMechanisms() {
        return activatedMechanisms;
    }

    /** 该研究线残页之外的研究点(拓片/回访/守护者来源)。 */
    public int getResearchPoints(EchoResearchLine line) {
        return researchPoints.getOrDefault(line.getStorageKey(), 0);
    }

    /** 累加研究点。存储上限 8(等级封顶之外留少量冗余,防数据无界增长)。 */
    public void addResearchPoints(EchoResearchLine line, int amount) {
        int next = Math.min(8, getResearchPoints(line) + amount);
        researchPoints.put(line.getStorageKey(), next);
    }

    /** 兼容旧调用:能力研究点映射到对应研究线。 */
    public int getResearchPoints(EchoAbilityType type) {
        EchoResearchLine line = EchoResearchLine.byAbility(type);
        return line == null ? researchPoints.getOrDefault(type.getId(), 0) : getResearchPoints(line);
    }

    /** 兼容旧调用:能力研究点映射到对应研究线。 */
    public void addResearchPoints(EchoAbilityType type, int amount) {
        EchoResearchLine line = EchoResearchLine.byAbility(type);
        if (line == null) {
            int next = Math.min(8, getResearchPoints(type) + amount);
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
        int next = getTokenSerial(type) + 1;
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
