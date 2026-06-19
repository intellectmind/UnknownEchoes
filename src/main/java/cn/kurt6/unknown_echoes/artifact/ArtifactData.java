package cn.kurt6.unknown_echoes.artifact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家个人神器数据 + 回响能量池(NeoForge Data Attachment,与 EchoAbilityData 同模式)。
 * 服务端权威:等级、调谐词条、凭据序号、能量全部存这里;物品只是凭据(12.2)。
 * 能量用 1/20 点定点数存储(energyTwentieths),小数恢复余量用 1/100 个 twentieth 记录。
 * 按 gameTime 差值惰性恢复,无每 tick 开销。
 */
public class ArtifactData {
    public static final int CURRENT_DATA_VERSION = 3;

    public static final Codec<ArtifactData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(d -> Map.copyOf(d.levels)),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("tunings", Map.of()).forGetter(d -> Map.copyOf(d.tunings)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("serials", Map.of()).forGetter(d -> Map.copyOf(d.serials)),
            Codec.INT.optionalFieldOf("energy_twentieths", -1).forGetter(d -> d.energyTwentieths),
            Codec.INT.optionalFieldOf("energy_remainder_hundredths", 0).forGetter(d -> d.energyRemainderHundredths),
            Codec.LONG.optionalFieldOf("energy_game_time", 0L).forGetter(d -> d.energyGameTime)
    ).apply(instance, ArtifactData::new));

    private int dataVersion;
    /** 神器 id → 当前等级(1-3)。存在即视为已领取资格落账。 */
    private final Map<String, Integer> levels = new HashMap<>();
    /** 神器 id → 当前调谐词条(空串=未调谐)。 */
    private final Map<String, String> tunings = new HashMap<>();
    /** 神器 id → 当前有效凭据序号。复领时自增,旧凭据自动作废(12.2 防冒用)。 */
    private final Map<String, Integer> serials = new HashMap<>();
    /** 回响能量,单位 1/20 点(-1 = 尚未初始化,首次读取按满池处理)。 */
    private int energyTwentieths;
    /** 能量恢复小数余量,单位 1/100 个 energyTwentieths。 */
    private int energyRemainderHundredths;
    /** 上次能量结算时的 gameTime,用于惰性恢复。 */
    private long energyGameTime;

    /** 冷却(神器 id → 冷却结束 gameTime)。瞬态:不入存档,死亡/重登即清,体验向数据。 */
    private final transient Map<String, Long> cooldownEnds = new HashMap<>();

    public ArtifactData() {
        this.dataVersion = CURRENT_DATA_VERSION;
        this.energyTwentieths = -1;
        this.energyRemainderHundredths = 0;
        this.energyGameTime = 0L;
    }

    public ArtifactData(int dataVersion, Map<String, Integer> levels, Map<String, String> tunings,
                        Map<String, Integer> serials, int energyTwentieths,
                        int energyRemainderHundredths, long energyGameTime) {
        this.dataVersion = CURRENT_DATA_VERSION;
        if (dataVersion >= CURRENT_DATA_VERSION) {
            copyCurrentArtifacts(levels, tunings, serials);
        }
        this.energyTwentieths = energyTwentieths;
        this.energyRemainderHundredths = Math.max(0, Math.min(99, energyRemainderHundredths));
        this.energyGameTime = energyGameTime;
    }

    private void copyCurrentArtifacts(Map<String, Integer> levels,
                                      Map<String, String> tunings,
                                      Map<String, Integer> serials) {
        for (ArtifactType type : ArtifactType.values()) {
            String id = type.getId();
            if (levels.containsKey(id)) {
                this.levels.put(id, Math.max(1, Math.min(ArtifactType.MAX_LEVEL, levels.get(id))));
            }
            if (tunings.containsKey(id)) {
                this.tunings.put(id, tunings.get(id));
            }
            if (serials.containsKey(id)) {
                this.serials.put(id, Math.max(0, serials.get(id)));
            }
        }
    }

    public boolean isClaimed(ArtifactType type) {
        return levels.containsKey(type.getId());
    }

    public int getLevel(ArtifactType type) {
        return levels.getOrDefault(type.getId(), 0);
    }

    public void setLevel(ArtifactType type, int level) {
        levels.put(type.getId(), Math.max(1, Math.min(ArtifactType.MAX_LEVEL, level)));
    }

    /** 当前调谐词条;空串表示未调谐(1 级或玩家尚未选择)。 */
    public String getTuning(ArtifactType type) {
        return tunings.getOrDefault(type.getId(), "");
    }

    /** 管理员撤销:清除等级与调谐;序号刻意保留,使流通中的旧凭据全部失效。 */
    public void removeArtifact(ArtifactType type) {
        levels.remove(type.getId());
        tunings.remove(type.getId());
    }

    public void setTuning(ArtifactType type, String word) {
        tunings.put(type.getId(), word);
    }

    public int getSerial(ArtifactType type) {
        return serials.getOrDefault(type.getId(), 0);
    }

    /** 发新凭据:序号自增并返回。旧序号凭据从此不再通过校验。 */
    public int nextSerial(ArtifactType type) {
        int next = getSerial(type) + 1;
        serials.put(type.getId(), next);
        return next;
    }

    public int getEnergyTwentieths() {
        return energyTwentieths;
    }

    public void setEnergyTwentieths(int value) {
        this.energyTwentieths = value;
    }

    public int getEnergyRemainderHundredths() {
        return energyRemainderHundredths;
    }

    public void setEnergyRemainderHundredths(int value) {
        this.energyRemainderHundredths = Math.max(0, Math.min(99, value));
    }

    public long getEnergyGameTime() {
        return energyGameTime;
    }

    public void setEnergyGameTime(long gameTime) {
        this.energyGameTime = gameTime;
    }

    public long getCooldownEnd(ArtifactType type) {
        return cooldownEnds.getOrDefault(type.getId(), 0L);
    }

    public void setCooldownEnd(ArtifactType type, long gameTime) {
        cooldownEnds.put(type.getId(), gameTime);
    }
}
