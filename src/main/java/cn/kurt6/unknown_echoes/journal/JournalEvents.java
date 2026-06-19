package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 探索日志自动记录钩子:
 * - 每 2 秒检测玩家脚下是否处于本 Mod 结构/群系内(读区块结构引用,开销可忽略)
 * - 击杀本 Mod 生物时记录图鉴
 * 残页阅读记录在 AncientPageItem.use,Boss/能力进度复用 EchoAbilityData。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class JournalEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 40 != 0
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = player.blockPosition();
        level.structureManager().getAllStructuresAt(pos).keySet().forEach(structure -> {
            ResourceLocation id = level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .getKey(structure);
            if (id != null && id.getNamespace().equals(UnknownEchoes.MODID)) {
                JournalManager.recordStructure(player, id);
            }
        });
        level.getBiome(pos).unwrapKey().ifPresent(biomeKey -> {
            if (biomeKey.location().getNamespace().equals(UnknownEchoes.MODID)) {
                JournalManager.recordBiome(player, biomeKey.location());
            }
        });
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(event.getEntity().getType());
            if (typeId.getNamespace().equals(UnknownEchoes.MODID)) {
                JournalManager.recordMob(player, typeId);
            }
        }
    }
}
