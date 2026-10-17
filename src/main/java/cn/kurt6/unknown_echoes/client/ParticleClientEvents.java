package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.gui.ParticleEnchantingScreen;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ParticleClientEvents {
    private ParticleClientEvents() { }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(ModMenus.PARTICLE_ENCHANTING.get(), ParticleEnchantingScreen::new); }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var data = ParticleRules.data(event.getItemStack());
        if (data.tier() == 0) return;
        event.getToolTip().add(Component.translatable("particle.unknown_echoes.capacity", data.tier(), data.total(), data.capacity()).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("particle.unknown_echoes.damage", String.format(java.util.Locale.ROOT, "%.1f", data.total() * ParticleRules.DAMAGE_PER_PARTICLE)).withStyle(ChatFormatting.GRAY));
        for (ParticleKind kind : ParticleKind.values()) if (data.count(kind) > 0)
            event.getToolTip().add(Component.translatable("particle.unknown_echoes.tooltip", Component.translatable(kind.key()), data.count(kind), ParticleRules.resonance(data.count(kind))).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("particle.unknown_echoes.repair").withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("particle.unknown_echoes.repair_pair").withStyle(ChatFormatting.DARK_GRAY));
    }
}
