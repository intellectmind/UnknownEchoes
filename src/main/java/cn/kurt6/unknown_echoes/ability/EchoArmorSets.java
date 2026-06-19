package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.registry.ModArmorMaterials;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * 套装件数判定(11.3):客户端可读自己的装备做提示/手感,实际效果全部由服务端事件层施加。
 * 实现约定(11.3 套装实现建议):穿戴件数触发;1 件=部位小属性(盔甲值自带),
 * 2/4 件=探索便利,4/4 件=主题强化;不做每 tick 高成本全局扫描——
 * 调用方一律在低频检查点(40 tick 周期 / 事件回调 / 滑翔包入口)取件数。
 */
public final class EchoArmorSets {

    private EchoArmorSets() {
    }

    /** 探索者套装件数(0-4)。 */
    public static int explorerPieces(Player player) {
        return countPieces(player, ModArmorMaterials.EXPLORER);
    }

    /** 聆听者套装件数(0-4)。 */
    public static int listenerPieces(Player player) {
        return countPieces(player, ModArmorMaterials.LISTENER);
    }

    /** 聆听者 2 件:线索表现(真视暴露发光、潮汐符文轨迹)显示时间延长。 */
    public static boolean hasListenerClueBonus(Player player) {
        return listenerPieces(player) >= 2;
    }

    /** 聆听者 4 件:回响能力冷却小幅降低、持续时间小幅延长。 */
    public static boolean hasListenerAbilityBonus(Player player) {
        return listenerPieces(player) >= 4;
    }

    public static int echoTravelerPieces(Player player) {
        return countPieces(player, ModArmorMaterials.ECHO_TRAVELER);
    }

    public static int windWalkerPieces(Player player) {
        return countPieces(player, ModArmorMaterials.WIND_WALKER);
    }

    public static int tideStalkerPieces(Player player) {
        return countPieces(player, ModArmorMaterials.TIDE_STALKER);
    }

    public static int trueSightShadowPieces(Player player) {
        return countPieces(player, ModArmorMaterials.TRUE_SIGHT_SHADOW);
    }

    public static int silentWatchPieces(Player player) {
        return countPieces(player, ModArmorMaterials.SILENT_WATCH);
    }

    public static int echoOathPieces(Player player) {
        return countPieces(player, ModArmorMaterials.ECHO_OATH);
    }

    private static int countPieces(Player player, Holder<ArmorMaterial> material) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial().is(material.getKey())) {
                count++;
            }
        }
        return count;
    }
}
