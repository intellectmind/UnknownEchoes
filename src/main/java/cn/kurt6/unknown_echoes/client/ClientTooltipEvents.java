package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public class ClientTooltipEvents {
    private static final String MATERIAL_TOOLTIP_PREFIX = "tooltip.unknown_echoes.material.";

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!UnknownEchoes.MODID.equals(itemId.getNamespace())) {
            return;
        }
        event.getToolTip().removeIf(ClientTooltipEvents::isMaterialSourceOrUseLine);
        String clueStructure = event.getItemStack().get(ModDataComponents.CLUE_STRUCTURE.get());
        if (clueStructure != null && !clueStructure.isBlank()) {
            ResourceLocation structureId = ResourceLocation.tryParse(clueStructure);
            String path = structureId == null ? clueStructure : structureId.getPath();
            event.getToolTip().add(Component.translatable("tooltip.unknown_echoes.clue_map.target",
                    Component.translatable("structure.unknown_echoes." + path))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static boolean isMaterialSourceOrUseLine(Component component) {
        if (component.getContents() instanceof TranslatableContents contents
                && isMaterialSourceOrUseKey(contents.getKey())) {
            return true;
        }
        String text = component.getString();
        return isMaterialSourceOrUseKey(text)
                || text.startsWith("来源:")
                || text.startsWith("用途:")
                || text.startsWith("Source:")
                || text.startsWith("Use:");
    }

    private static boolean isMaterialSourceOrUseKey(String key) {
        return key.startsWith(MATERIAL_TOOLTIP_PREFIX)
                && (key.endsWith(".source") || key.endsWith(".use"));
    }
}
