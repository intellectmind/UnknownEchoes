package cn.kurt6.unknown_echoes.item.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class EchoTooltips {
    private EchoTooltips() {
    }

    static void add(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + key + ".role")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + key + ".trigger")
                .withStyle(ChatFormatting.GRAY));
    }
}
