package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 神器凭据共用的操作与主动效果说明；等级和升级条件在神器面板展示。 */
public final class ArtifactTooltip {
    private ArtifactTooltip() {
    }

    public static void add(List<Component> tooltip, ArtifactType type) {
        String prefix = "tooltip.unknown_echoes.artifact." + type.getId();
        tooltip.add(Component.translatable(prefix + ".skill",
                        Component.keybind("key.unknown_echoes.artifact_use"),
                        Component.translatable(prefix + ".active_effect"))
                .withStyle(ChatFormatting.AQUA));
        if (type == ArtifactType.STORM_COMPASS) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.artifact.storm_compass.alternate",
                            Component.keybind("key.unknown_echoes.item_alternate_skill"))
                    .withStyle(ChatFormatting.AQUA));
        } else if (type == ArtifactType.DIMENSION_ANCHOR || type == ArtifactType.RIFT_ANCHOR) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.artifact.anchor.alternate",
                            Component.keybind("key.unknown_echoes.item_alternate_skill"))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
