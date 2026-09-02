package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.WorldEchoFlightStatePayload;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 客户端仅缓存服务端确认的翼装展开状态，不参与飞行权限判定。 */
public final class ClientWorldEchoFlightCache {
    private static final Set<UUID> FLYING_PLAYERS = new HashSet<>();

    private ClientWorldEchoFlightCache() {
    }

    public static void update(WorldEchoFlightStatePayload payload) {
        if (payload.flying()) {
            FLYING_PLAYERS.add(payload.playerId());
        } else {
            FLYING_PLAYERS.remove(payload.playerId());
        }
    }

    public static boolean isFlying(Player player) {
        return FLYING_PLAYERS.contains(player.getUUID());
    }

    public static void clear() {
        FLYING_PLAYERS.clear();
    }
}
