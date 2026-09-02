package cn.kurt6.unknown_echoes.item.tool;

import net.minecraft.world.item.ItemStack;

public final class AreaMiningSupport {
    private AreaMiningSupport() {
    }

    public static boolean isSupported(ItemStack stack) {
        return stack.getItem() instanceof EchoPickaxeItem
                || stack.getItem() instanceof EchoShovelItem
                || stack.getItem() instanceof EchoHatchetItem
                || stack.getItem() instanceof EchoHoeItem
                || stack.getItem() instanceof EchoShearsItem
                || stack.getItem() instanceof DeepPickaxeItem
                || stack.getItem() instanceof DeepShovelItem
                || stack.getItem() instanceof DeepAxeItem
                || stack.getItem() instanceof DeepHoeItem
                || stack.getItem() instanceof LandmarkPickaxeItem;
    }

    public static boolean usesToolSkillKey(ItemStack stack) {
        return isSupported(stack) || stack.getItem() instanceof EchoIgniterItem;
    }
}
