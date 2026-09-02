package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 携带时将共享回响能量保持满值，不承担任何能力或进度权限。 */
public class PerpetualEchoCoreItem extends Item {
    public PerpetualEchoCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.perpetual_echo_core.skill")
                .withStyle(ChatFormatting.AQUA));
    }
}
