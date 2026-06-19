package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 武器属性工具:按"面板总值"直接声明攻击伤害/攻速(设计文档 V0.6B 数值为设定基线,
 * 原版换算:总伤害 = 玩家基础 1.0 + 修饰值,总攻速 = 基础 4.0 + 修饰值)。
 */
final class WeaponAttributes {

    private WeaponAttributes() {
    }

    /** totalDamage/totalSpeed 为物品面板上显示的最终值。 */
    static ItemAttributeModifiers melee(double totalDamage, double totalSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, totalDamage - 1.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, totalSpeed - 4.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
