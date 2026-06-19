package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 回声大档案馆三翼检查。只在档案终端附近记录个人进度,避免通用机关误算为终局机关。
 */
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
        EchoAbilityManager.activateMechanism(player, key);
        EchoAbilityManager.syncToClient(player);
        return true;
    }

    private static BlockPos findNearbyArchiveTerminal(Level level, BlockPos origin) {
        BlockPos from = origin.offset(-TERMINAL_RANGE, -TERMINAL_RANGE, -TERMINAL_RANGE);
        BlockPos to = origin.offset(TERMINAL_RANGE, TERMINAL_RANGE, TERMINAL_RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.getBlockState(pos).is(ModBlocks.ECHO_ARCHIVE_TERMINAL.get())) {
                return pos.immutable();
            }
        }
        return null;
    }
}
