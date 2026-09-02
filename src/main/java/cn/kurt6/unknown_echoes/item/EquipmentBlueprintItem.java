package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 装备图纸：提供第一维度武器的配方线索。 */
public class EquipmentBlueprintItem extends Item {
    /** 对应武器的 lang 子键(wind_spear / tide_crossbow / true_sight_blade)。 */
    private final String weaponKey;

    public EquipmentBlueprintItem(String weaponKey, Properties properties) {
        super(properties);
        this.weaponKey = weaponKey;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.blueprint." + weaponKey + ".materials")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.blueprint.keeps")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
