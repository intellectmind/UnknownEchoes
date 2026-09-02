package cn.kurt6.unknown_echoes.item.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class DeepTooltips {
    private DeepTooltips() {
    }

    static void add(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + key + ".detail",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.AQUA));
    }

    static void addHoe(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_hoe.detail")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_hoe.trigger",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.GRAY));
    }

    static void addShovel(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_shovel.detail")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_shovel.trigger",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.GRAY));
    }

    static void addAxe(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_axe.detail")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_axe.trigger",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.GRAY));
    }

    static void addPickaxe(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_pickaxe.detail")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.deep_alloy_pickaxe.trigger",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.GRAY));
    }
}
