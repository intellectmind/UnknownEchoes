package cn.kurt6.unknown_echoes.artifact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public class ArtifactData {
    public static final int CURRENT_DATA_VERSION = 4;
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_TUNING_LENGTH = 64;
    private static final Codec<String> ID_CODEC = Codec.sizeLimitedString(MAX_ID_LENGTH);

    public static final Codec<ArtifactData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            Codec.unboundedMap(ID_CODEC, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(d -> Map.copyOf(d.levels)),
            Codec.unboundedMap(ID_CODEC, Codec.sizeLimitedString(MAX_TUNING_LENGTH)).optionalFieldOf("tunings", Map.of())
                    .forGetter(d -> Map.copyOf(d.tunings)),
            Codec.unboundedMap(ID_CODEC, Codec.INT).optionalFieldOf("serials", Map.of()).forGetter(d -> Map.copyOf(d.serials)),
            Codec.unboundedMap(ID_CODEC, Codec.sizeLimitedString(512)).optionalFieldOf("bindings", Map.of())
                    .forGetter(d -> Map.copyOf(d.bindings)),
            Codec.INT.optionalFieldOf("energy_twentieths", -1).forGetter(d -> d.energyTwentieths),
            Codec.INT.optionalFieldOf("energy_remainder_hundredths", 0).forGetter(d -> d.energyRemainderHundredths),
            Codec.LONG.optionalFieldOf("energy_game_time", 0L).forGetter(d -> d.energyGameTime)
    ).apply(instance, ArtifactData::new));

    private int dataVersion;
    private final Map<String, Integer> levels = new HashMap<>();
    private final Map<String, String> tunings = new HashMap<>();
    /** 神器 id → 当前有效凭据序号；复领时自增，旧凭据自动作废。 */
    private final Map<String, Integer> serials = new HashMap<>();
    /** 神器私有绑定数据。格式由对应效果处理器校验，物品凭据不保存坐标。 */
    private final Map<String, String> bindings = new HashMap<>();
    /** 回响能量,单位 1/20 点(-1 = 尚未初始化,首次读取按满池处理)。 */
    private int energyTwentieths;
    /** 能量恢复小数余量,单位 1/100 个 energyTwentieths。 */
    private int energyRemainderHundredths;
    /** 上次能量结算时的 gameTime,用于惰性恢复。 */
    private long energyGameTime;

    /** 冷却(神器 id → 冷却结束 gameTime)。瞬态:不入存档,死亡/重登即清,体验向数据。 */
    private final transient Map<String, Long> cooldownEnds = new HashMap<>();
    /** 冷却原始时长,用于时间回拨后按实际时长钳制截止值。 */
    private final transient Map<String, Long> cooldownDurations = new HashMap<>();

    public ArtifactData() {
        this.dataVersion = CURRENT_DATA_VERSION;
        this.energyTwentieths = -1;
        this.energyRemainderHundredths = 0;
        this.energyGameTime = 0L;
    }

    public ArtifactData(int dataVersion, Map<String, Integer> levels, Map<String, String> tunings,
                        Map<String, Integer> serials, Map<String, String> bindings, int energyTwentieths,
                        int energyRemainderHundredths, long energyGameTime) {
        this.dataVersion = CURRENT_DATA_VERSION;

        copyCurrentArtifacts(levels, tunings, serials, bindings);
        this.energyTwentieths = energyTwentieths;
        this.energyRemainderHundredths = Math.max(0, Math.min(99, energyRemainderHundredths));
        this.energyGameTime = Math.max(0L, energyGameTime);
    }

    private void copyCurrentArtifacts(Map<String, Integer> levels,
                                      Map<String, String> tunings,
                                      Map<String, Integer> serials,
                                      Map<String, String> bindings) {
        for (ArtifactType type : ArtifactType.values()) {
            String id = type.getId();
            if (levels.containsKey(id)) {
                this.levels.put(id, Math.max(1, Math.min(ArtifactType.MAX_LEVEL, levels.get(id))));
            }
            if (tunings.containsKey(id)) {
                String tuning = tunings.get(id);
                if (isValidTuning(type, tuning)) {
                    this.tunings.put(id, tuning);
                }
            }
            if (serials.containsKey(id)) {
                this.serials.put(id, Math.max(0, serials.get(id)));
            }
            String binding = bindings.get(id);
            if (binding != null && !binding.isBlank() && binding.length() <= 512) {
                this.bindings.put(id, binding);
            }
        }
    }

    private static boolean isValidTuning(ArtifactType type, String tuning) {
        return tuning != null && (tuning.isEmpty()
                || type.getTuningWords().contains(tuning)
                || type.getHiddenWord().equals(tuning));
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

    public String getTuning(ArtifactType type) {
        return tunings.getOrDefault(type.getId(), "");
    }

    /** 管理员撤销:清除等级、调谐和私有绑定;序号保留,使流通中的旧凭据全部失效。 */
    public void removeArtifact(ArtifactType type) {
        levels.remove(type.getId());
        tunings.remove(type.getId());
        bindings.remove(type.getId());
    }

    public void setTuning(ArtifactType type, String word) {
        if (isValidTuning(type, word)) {
            tunings.put(type.getId(), word);
        }
    }

    public int getSerial(ArtifactType type) {
        return serials.getOrDefault(type.getId(), 0);
    }

    /** 发新凭据:序号自增并返回。旧序号凭据从此不再通过校验。 */
    public int nextSerial(ArtifactType type) {
        int current = getSerial(type);
        if (current == Integer.MAX_VALUE) {
            return 0;
        }
        int next = current + 1;
        serials.put(type.getId(), next);
        return next;
    }

    public String getBinding(ArtifactType type) {
        return bindings.getOrDefault(type.getId(), "");
    }

    public void setBinding(ArtifactType type, String binding) {
        if (binding == null || binding.isBlank()) {
            bindings.remove(type.getId());
        } else if (binding.length() <= 512) {
            bindings.put(type.getId(), binding);
        }
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

    public long getCooldownDuration(ArtifactType type) {
        return cooldownDurations.getOrDefault(type.getId(), 0L);
    }

    public void setCooldown(ArtifactType type, long gameTime, long duration) {
        cooldownEnds.put(type.getId(), gameTime);
        cooldownDurations.put(type.getId(), Math.max(0L, duration));
    }

    public void clearCooldown(ArtifactType type) {
        cooldownEnds.remove(type.getId());
        cooldownDurations.remove(type.getId());
    }
}
