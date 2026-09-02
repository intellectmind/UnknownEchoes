package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EchoFishingRodItem extends FishingRodItem {
    public EchoFishingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ECHO_STRING.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.addTriggerOnly(tooltip, "echo_fishing_rod");
    }
}
