package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 限制深层核心祭坛的大范围方块校验频率，避免连续交互放大扫描成本。 */
public final class DeepAltarScanLimiter {
    private static final long INTERVAL_TICKS = 10L;
    private static final Map<UUID, Long> LAST_SCAN_TICKS = new HashMap<>();

    private DeepAltarScanLimiter() {
    }

    public static boolean allow(ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        Long previous = LAST_SCAN_TICKS.get(player.getUUID());
        if (previous != null && previous <= now && now - previous < INTERVAL_TICKS) {
            return false;
        }
        LAST_SCAN_TICKS.put(player.getUUID(), now);
        return true;
    }

    public static void clear(UUID playerId) {
        LAST_SCAN_TICKS.remove(playerId);
    }

    public static void clearAll() {
        LAST_SCAN_TICKS.clear();
    }
}
