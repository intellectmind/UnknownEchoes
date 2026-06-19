package cn.kurt6.unknown_echoes.item.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EchoShearsItem extends ShearsItem {
    public EchoShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.add(tooltip, "echo_shears");
    }
}
