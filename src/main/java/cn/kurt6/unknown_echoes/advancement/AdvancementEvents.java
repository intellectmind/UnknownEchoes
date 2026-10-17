package cn.kurt6.unknown_echoes.advancement;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class AdvancementEvents {
    private AdvancementEvents() { }

    @SubscribeEvent
    public static void pickedUp(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ModAdvancements.checkInventoryMilestones(player);
        }
    }

    @SubscribeEvent
    public static void closedContainer(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModAdvancements.checkInventoryMilestones(player);
        }
    }
}
