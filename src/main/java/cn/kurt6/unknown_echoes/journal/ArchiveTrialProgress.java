package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlockEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** 回声大档案馆三翼检查。只在档案终端附近记录个人进度,避免通用机关误算为终局机关。 */
public final class ArchiveTrialProgress {
    private static final int TERMINAL_RANGE = 48;
    private static final String PREFIX = "archive_trial:";

    private ArchiveTrialProgress() {
    }

    public static boolean recordWind(ServerPlayer player, Level level, BlockPos pos) {
        return record(player, level, pos, "wind");
    }

    public static boolean recordTide(ServerPlayer player, Level level, BlockPos pos) {
        return record(player, level, pos, "tide");
    }

    public static boolean recordTrueSight(ServerPlayer player, Level level, BlockPos pos) {
        return record(player, level, pos, "true_sight");
    }

    public static boolean hasCompletedAll(ServerPlayer player) {
        var mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        for (String wind : mechanisms) {
            if (!wind.startsWith(PREFIX + "wind:")) {
                continue;
            }
            String terminalKey = wind.substring((PREFIX + "wind:").length());
            if (mechanisms.contains(PREFIX + "tide:" + terminalKey)
                    && mechanisms.contains(PREFIX + "true_sight:" + terminalKey)) {
                return true;
            }
        }
        return false;
    }

    private static boolean record(ServerPlayer player, Level level, BlockPos pos, String wing) {
        BlockPos terminal = findNearbyArchiveTerminal(level, pos);
        if (terminal == null) {
            return false;
        }
        String key = PREFIX + wing + ":" + level.dimension().location() + ":" + terminal.asLong();
        if (EchoAbilityManager.hasActivatedMechanism(player, key)) {
            return false;
        }
        if (!EchoAbilityManager.activateMechanism(player, key)) {
            return false;
        }
        EchoAbilityManager.syncToClient(player);
        if (hasCompletedAll(player)) {
            ModAdvancements.award(player, ModAdvancements.ARCHIVE_TRIALS);
        }
        return true;
    }

    private static BlockPos findNearbyArchiveTerminal(Level level, BlockPos origin) {
        int minChunkX = (origin.getX() - TERMINAL_RANGE) >> 4;
        int maxChunkX = (origin.getX() + TERMINAL_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - TERMINAL_RANGE) >> 4;
        int maxChunkZ = (origin.getZ() + TERMINAL_RANGE) >> 4;
        BlockPos nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (var blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!(blockEntity instanceof EchoArchiveTerminalBlockEntity)
                            || !blockEntity.getBlockState().is(ModBlocks.ECHO_ARCHIVE_TERMINAL.get())) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    int dx = Math.abs(pos.getX() - origin.getX());
                    int dy = Math.abs(pos.getY() - origin.getY());
                    int dz = Math.abs(pos.getZ() - origin.getZ());
                    if (dx > TERMINAL_RANGE || dy > TERMINAL_RANGE || dz > TERMINAL_RANGE) {
                        continue;
                    }
                    long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (distance < nearestDistance) {
                        nearest = pos.immutable();
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }
}
