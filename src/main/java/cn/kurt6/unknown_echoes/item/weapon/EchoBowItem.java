package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 第一维度弓类武器的轻量实现,保留原版弹道并补充玩法说明。 */
public class EchoBowItem extends BowItem {
    private final String tooltipKey;

    public EchoBowItem(String tooltipKey, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".skill")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
