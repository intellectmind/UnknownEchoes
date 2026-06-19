package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 套装盔甲(11.3):本体走原版 ArmorItem,只追加套装件数效果的 tooltip 说明。
 * 实际效果判定见 ability/EchoArmorSets 与 event/EchoArmorSetEvents(服务端)。
 */
public class EchoSetArmorItem extends ArmorItem {

    /** tooltip 键前缀:tooltip.unknown_echoes.set.<setKey>.2 / .4 */
    private final String setKey;

    public EchoSetArmorItem(Holder<ArmorMaterial> material, Type type, String setKey, Properties properties) {
        super(material, type, properties);
        this.setKey = setKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.unknown_echoes.set." + setKey + ".name")
                .withStyle(ChatFormatting.AQUA));
        addSetLine(tooltip, "2", ChatFormatting.GRAY);
        addSetLine(tooltip, "4", ChatFormatting.GRAY);
        addSetLine(tooltip, "area", ChatFormatting.DARK_AQUA);
    }

    private void addSetLine(List<Component> tooltip, String suffix, ChatFormatting style) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.set." + setKey + "." + suffix)
                .withStyle(style));
    }
}
