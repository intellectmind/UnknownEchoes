package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.resources.ResourceLocation;

/**
 * 世界事件注册表(V0.6D 第一版,22.10):首批三个低频环境事件 + 失落商队。
 * 全部挂在 WorldEventManager 的统一触发器/计时器上,不允许各写一套定时器(二十三章)。
 * 事件只做表现层与个人参与记录,不发放关键奖励,不替代主线(16 章边界)。
 */
public enum WorldEventType {

    /** 回响雾:回声境域任意群系,局部雾气与脚步回声(纯表现)。 */
    ECHO_FOG("echo_fog", true, null),

    /** 镜湖倒影异常:镜湖水面上方,倒影粒子逆流上升(真视氛围线索)。 */
    MIRROR_ANOMALY("mirror_anomaly", true, UnknownEchoes.id("mirror_lake")),

    /** 风暴前兆:漂浮群岛,远雷与上升气流(风暴编织者场地氛围铺垫)。 */
    STORM_OMEN("storm_omen", true, UnknownEchoes.id("floating_isles")),

    /** 失落商队(16.4):主世界安全位置出现迷途旅者临时营地;结束后留下熄灭的篝火。 */
    LOST_CARAVAN("lost_caravan", false, null);

    private final String id;
    /** true=只在回声境域触发;false=只在主世界触发。 */
    private final boolean realmOnly;
    /** 限定群系(null=不限)。 */
    private final ResourceLocation requiredBiome;

    WorldEventType(String id, boolean realmOnly, ResourceLocation requiredBiome) {
        this.id = id;
        this.realmOnly = realmOnly;
        this.requiredBiome = requiredBiome;
    }

    public String getId() {
        return id;
    }

    public boolean isRealmOnly() {
        return realmOnly;
    }

    public ResourceLocation getRequiredBiome() {
        return requiredBiome;
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
