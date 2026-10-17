package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlockEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

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
        pos = pos.immutable();
        if (!(level instanceof ServerLevel serverLevel) || !isCurrentPlayer(player, serverLevel, pos)) {
            return false;
        }
        BlockState source = readableState(serverLevel, pos);
        if (source == null) {
            return false;
        }
        EchoArchiveTerminalBlockEntity terminal = findNearbyArchiveTerminal(level, pos);
        if (terminal == null || !isCurrentRecord(player, serverLevel, pos, source, terminal)) {
            return false;
        }
        String key = PREFIX + wing + ":" + level.dimension().location() + ":" + terminal.getBlockPos().asLong();
        if (EchoAbilityManager.hasActivatedMechanism(player, key)) {
            return false;
        }
        if (!EchoAbilityManager.activateMechanism(player, key)) {
            return false;
        }
        if (!isCurrentRecord(player, serverLevel, pos, source, terminal)) {
            return true;
        }
        EchoAbilityManager.syncToClient(player);
        if (isCurrentRecord(player, serverLevel, pos, source, terminal) && hasCompletedAll(player)) {
            ModAdvancements.award(player, ModAdvancements.ARCHIVE_TRIALS);
        }
        return true;
    }

    private static boolean isCurrentPlayer(ServerPlayer player, ServerLevel level, BlockPos origin) {
        return player.level() == level && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !player.hasDisconnected()
                && player.canInteractWithBlock(origin, 1.0D);
    }

    private static BlockState readableState(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return null;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    private static boolean isCurrentRecord(ServerPlayer player, ServerLevel level, BlockPos origin,
                                            BlockState source, EchoArchiveTerminalBlockEntity terminal) {
        return isCurrentPlayer(player, level, origin) && readableState(level, origin) == source
                && isCurrentTerminal(level, terminal);
    }

    private static boolean isCurrentTerminal(ServerLevel level, EchoArchiveTerminalBlockEntity terminal) {
        if (terminal.isRemoved() || terminal.getLevel() != level) {
            return false;
        }
        BlockPos pos = terminal.getBlockPos();
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        BlockState state = terminal.getBlockState();
        return chunk != null && chunk.getBlockEntities().get(pos) == terminal
                && state.is(ModBlocks.ECHO_ARCHIVE_TERMINAL.get()) && chunk.getBlockState(pos) == state;
    }

    private static EchoArchiveTerminalBlockEntity findNearbyArchiveTerminal(Level level, BlockPos origin) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        int minChunkX = (origin.getX() - TERMINAL_RANGE) >> 4;
        int maxChunkX = (origin.getX() + TERMINAL_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - TERMINAL_RANGE) >> 4;
        int maxChunkZ = (origin.getZ() + TERMINAL_RANGE) >> 4;
        EchoArchiveTerminalBlockEntity nearest = null;
        BlockPos nearestPos = null;
        long nearestDistance = Long.MAX_VALUE;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof EchoArchiveTerminalBlockEntity terminal)) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    long dx = Math.abs((long) pos.getX() - origin.getX());
                    long dy = Math.abs((long) pos.getY() - origin.getY());
                    long dz = Math.abs((long) pos.getZ() - origin.getZ());
                    if (dx > TERMINAL_RANGE || dy > TERMINAL_RANGE || dz > TERMINAL_RANGE) {
                        continue;
                    }
                    if (!isCurrentTerminal(serverLevel, terminal)) {
                        continue;
                    }
                    long distance = dx * dx + dy * dy + dz * dz;
                    if (distance < nearestDistance || distance == nearestDistance && earlierTerminal(pos, nearestPos)) {
                        nearest = terminal;
                        nearestPos = pos;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    private static boolean earlierTerminal(BlockPos pos, BlockPos other) {
        return other == null || pos.getZ() < other.getZ()
                || pos.getZ() == other.getZ() && (pos.getY() < other.getY()
                || pos.getY() == other.getY() && pos.getX() < other.getX());
    }
}
