package cn.kurt6.unknown_echoes.item;

import net.minecraft.world.item.Item;

/** 第一维度普通资源物品。不在物品 tooltip 暴露来源/用途,避免干扰玩家自行探索。 */
public class EchoMaterialItem extends Item {
    public EchoMaterialItem(String tooltipKey, Properties properties) {
        super(properties);
    }
}
