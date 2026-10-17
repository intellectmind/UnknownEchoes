package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.JourneyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class PuzzleJourneyRecorder {
    private PuzzleJourneyRecorder() { }

    private static String kind(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var state = level.getBlockState(pos);
        if (!state.hasProperty(PuzzleCoreBlock.ACTIVE) || !state.getValue(PuzzleCoreBlock.ACTIVE)) return null;
        var core = level.getBlockEntity(pos);
        if (core instanceof ChimeEchoCoreBlockEntity) return "chime";
        if (core instanceof FoldpagePairCoreBlockEntity) return "pairs";
        if (core instanceof PuzzleCoreBlockEntity puzzle) return puzzle.isLightsOutPuzzle() ? "ring" : "sequence";
        return null;
    }

    public static void completed(ServerLevel level, BlockPos pos) {
        String kind = kind(level, pos);
        if (kind == null) return;
        var core = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(12))) {
            if (!level.hasChunkAt(pos) || core.isRemoved() || core.getLevel() != level
                    || level.getBlockEntity(pos) != core || level.getBlockState(pos) != state) return;
            if (player.level() == level && !player.isRemoved() && EchoPermission.canUseEchoMechanism(player)) {
                JourneyManager.recordPuzzle(player, pos, kind);
            }
        }
    }

    @SubscribeEvent
    public static void inspect(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !EchoPermission.canUseEchoMechanism(player)) return;
        String kind = kind(player.serverLevel(), event.getPos());
        if (kind != null) JourneyManager.recordPuzzle(player, event.getPos(), kind);
    }
}
