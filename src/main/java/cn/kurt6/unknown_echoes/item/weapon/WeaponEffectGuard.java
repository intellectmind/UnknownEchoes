package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 战技追加伤害的同步调用栈守卫，防止追加伤害再次触发武器被动。 */
public final class WeaponEffectGuard {
    private static final ThreadLocal<Map<UUID, Integer>> ACTIVE_DEPTH =
            ThreadLocal.withInitial(HashMap::new);

    private WeaponEffectGuard() {
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE_DEPTH.get().getOrDefault(player.getUUID(), 0) > 0;
    }

    public static void run(ServerPlayer player, Runnable action) {
        Map<UUID, Integer> depths = ACTIVE_DEPTH.get();
        UUID playerId = player.getUUID();
        depths.merge(playerId, 1, Integer::sum);
        try {
            action.run();
        } finally {
            int remaining = depths.getOrDefault(playerId, 1) - 1;
            if (remaining <= 0) {
                depths.remove(playerId);
            } else {
                depths.put(playerId, remaining);
            }
            if (depths.isEmpty()) {
                ACTIVE_DEPTH.remove();
            }
        }
    }
}
