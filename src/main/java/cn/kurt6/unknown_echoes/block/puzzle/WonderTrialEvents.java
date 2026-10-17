package cn.kurt6.unknown_echoes.block.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = "unknown_echoes")
public final class WonderTrialEvents {
    private WonderTrialEvents() { }

    // 掉落事件发生在可取消的死亡判定之后；取消死亡不得推进试炼。
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)
                || !event.getEntity().getPersistentData().contains(WonderTrialBlockEntity.MOB_TAG)) return;
        CompoundTag marker = event.getEntity().getPersistentData().getCompound(WonderTrialBlockEntity.MOB_TAG);
        if (marker.hasUUID("Run") && marker.getString("Dimension").equals(server.dimension().location().toString())) {
            WonderTrialData.get(server).defeated(BlockPos.of(marker.getLong("Pos")), marker.getUUID("Run"), event.getEntity().getUUID());
        }
        event.getDrops().clear();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().contains(WonderTrialBlockEntity.MOB_TAG)) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel server)
                || mob.tickCount % 40 != 0 || !mob.isAlive()
                || !mob.getPersistentData().contains(WonderTrialBlockEntity.MOB_TAG)) return;
        CompoundTag marker = mob.getPersistentData().getCompound(WonderTrialBlockEntity.MOB_TAG);
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        WonderTrialProgress progress = WonderTrialData.get(server).find(pos);
        if (!marker.hasUUID("Run") || !marker.getString("Dimension").equals(server.dimension().location().toString())
                || progress == null || !progress.run().equals(marker.getUUID("Run")) || !progress.remaining().contains(mob.getUUID())) {
            mob.discard();
        } else if (mob.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 20 * 20
                && server.hasChunkAt(pos) && server.getBlockEntity(pos) instanceof WonderTrialBlockEntity trial) {
            trial.positionGuard(server, mob);
        }
    }
}
