package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/** 世界回响体凝成的终局动力翼甲；飞行状态由 WorldEchoWingsEvents 独立维护。 */
public class WorldEchoWingsItem extends ArmorItem {
    private static final double ARMOR = 18.0D;
    private static final double TOUGHNESS = 8.0D;
    private static final double KNOCKBACK_RESISTANCE = 0.35D;
    private static final double MAX_HEALTH = 8.0D;
    private static final double MOVEMENT_SPEED = 0.15D;

    public WorldEchoWingsItem(Properties properties) {
        super(ModArmorMaterials.RESONATOR, Type.CHESTPLATE, properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        EquipmentSlotGroup slot = EquipmentSlotGroup.CHEST;
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        UnknownEchoes.id("world_echo_wings.armor"), ARMOR,
                        AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                        UnknownEchoes.id("world_echo_wings.toughness"), TOUGHNESS,
                        AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                        UnknownEchoes.id("world_echo_wings.knockback_resistance"),
                        KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.MAX_HEALTH, new AttributeModifier(
                        UnknownEchoes.id("world_echo_wings.max_health"), MAX_HEALTH,
                        AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                        UnknownEchoes.id("world_echo_wings.movement_speed"), MOVEMENT_SPEED,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot)
                .build();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.world_echo_wings.skill",
                        Component.keybind("key.jump"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.world_echo_wings.controls",
                        Component.keybind("key.jump"), Component.keybind("key.sneak"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.world_echo_wings.detail")
                .withStyle(ChatFormatting.GRAY));
    }
}
