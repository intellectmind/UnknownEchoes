package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class DisasterBuildProtection {
    private DisasterBuildProtection() { }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getLevel() instanceof ServerLevel level)) return;
        var dimension = level.dimension().location();
        if (!dimension.equals(EchoPermission.ECHO_REALM_ID) && !dimension.equals(EchoPermission.DEEP_ECHOES_ID)) return;
        protect(level, event.getPos());
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) protect(level, snapshot.getPos());
        }
    }

    private static void protect(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null && !chunk.getData(ModAttachments.DISASTER_BUILD_PROTECTION)) {
            // 布尔值随区块保存，拆掉建筑后也不恢复天灾破坏资格。
            chunk.setData(ModAttachments.DISASTER_BUILD_PROTECTION, true);
            chunk.setUnsaved(true);
        }
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null || chunk.getData(ModAttachments.DISASTER_BUILD_PROTECTION);
    }
}
