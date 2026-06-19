package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 第一维度普通资源物品。不在物品 tooltip 暴露来源/用途,避免干扰玩家自行探索。
 */
public class EchoMaterialItem extends Item {
    private final String tooltipKey;

    public EchoMaterialItem(String tooltipKey, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (this.tooltipKey.contains("core")) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.material.core_boundary")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
