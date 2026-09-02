package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.resources.ResourceLocation;

/** 世界事件注册表:全部事件共享统一触发器，按主世界/回声境域/深层回响限定范围。 */
public enum WorldEventType {

    ECHO_FOG("echo_fog", true, null),

    MIRROR_ANOMALY("mirror_anomaly", true, UnknownEchoes.id("mirror_lake")),

    STORM_OMEN("storm_omen", true, UnknownEchoes.id("floating_isles")),

    /** 失落商队:主世界安全位置出现迷途旅者；只用实体和粒子表现临时营地。 */
    LOST_CARAVAN("lost_caravan", false, null),

    METEOR_AFTERGLOW("meteor_afterglow", false, null,
            false, 2, true),

    ECHO_STORM("echo_storm", true, null,
            false, 4, false),

    STRATA_AFTERSHOCK("strata_aftershock", false, null,
            true, 3, false),

    FAR_BELL_PROCESSION("far_bell_procession", false, null,
            false, 2, true),

    MEMORY_FIREFLY_TIDE("memory_firefly_tide", true, null,
            false, 3, false),

    GRAVITY_TIDE("gravity_tide", false, UnknownEchoes.id("inverted_chasm"), true, 6),

    ARCHIVE_DRIFT("archive_drift", false, UnknownEchoes.id("shattered_archive"), true, 4),

    SILENT_PULSE("silent_pulse", false, UnknownEchoes.id("silent_abyss"), true, 5),

    FOLDED_PROCESSION("folded_procession", false,
            UnknownEchoes.id("folded_corridors"), true, 4),

    CRYSTAL_CHORUS("crystal_chorus", false,
            UnknownEchoes.id("weightless_grotto"), true, 5),

    RIFT_AFTERIMAGE("rift_afterimage", false,
            UnknownEchoes.id("voidscar_hollows"), true, 5),

    INTERLAYER_RESPITE("interlayer_respite", false, null,
            true, 4, false);

    private final String id;
    /** true=只在回声境域触发；false 时继续由 deepOnly/overworldOnly 限定。 */
    private final boolean realmOnly;
    /** true=只在深层回响触发。与 realmOnly 互斥。 */
    private final boolean deepOnly;
    /** true=只在主世界触发，防止普通维度事件进入下界或末地。 */
    private final boolean overworldOnly;
    private final ResourceLocation requiredBiome;
    /** 首次个人目击时回填的普通回响能量；0 表示不回填。 */
    private final int energyReward;

    WorldEventType(String id, boolean realmOnly, ResourceLocation requiredBiome) {
        this(id, realmOnly, requiredBiome, false, 0);
    }

    WorldEventType(String id, boolean realmOnly, ResourceLocation requiredBiome,
                   boolean deepOnly, int energyReward) {
        this(id, realmOnly, requiredBiome, deepOnly, energyReward, false);
    }

    WorldEventType(String id, boolean realmOnly, ResourceLocation requiredBiome,
                   boolean deepOnly, int energyReward, boolean overworldOnly) {
        this.id = id;
        this.realmOnly = realmOnly;
        this.deepOnly = deepOnly;
        this.overworldOnly = overworldOnly;
        this.requiredBiome = requiredBiome;
        this.energyReward = energyReward;
    }

    public String getId() {
        return id;
    }

    public boolean isRealmOnly() {
        return realmOnly;
    }

    public boolean isDeepOnly() {
        return deepOnly;
    }

    public boolean isOverworldOnly() {
        return overworldOnly;
    }

    public ResourceLocation getRequiredBiome() {
        return requiredBiome;
    }

    public int getEnergyReward() {
        return energyReward;
    }

    public static WorldEventType byId(String id) {
        for (WorldEventType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
