package cn.kurt6.unknown_echoes.item.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class EchoTooltips {
    private EchoTooltips() {
    }

    static void add(List<Component> tooltip, String key) {
        add(tooltip, key, new Object[0]);
    }

    static void addWithSkillKeyOnly(List<Component> tooltip, String key) {
        add(tooltip, key, Component.keybind("key.unknown_echoes.echo_tool_skill"));
    }

    static void addWithSkillKey(List<Component> tooltip, String key) {
        add(tooltip, key, Component.keybind("key.unknown_echoes.echo_tool_skill"));
    }

    static void addRoleOnly(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + key + ".role")
                .withStyle(ChatFormatting.AQUA));
    }

    static void addTriggerOnly(List<Component> tooltip, String key) {
        addTrigger(tooltip, key);
    }

    private static void add(List<Component> tooltip, String key, Object... triggerArgs) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + key + ".role")
                .withStyle(ChatFormatting.AQUA));
        addTrigger(tooltip, key, triggerArgs);
    }

    private static void addTrigger(List<Component> tooltip, String key, Object... triggerArgs) {
        tooltip.add(Component.translatable(
                        "tooltip.unknown_echoes.tool." + key + ".trigger", triggerArgs)
                .withStyle(ChatFormatting.GRAY));
    }
}
