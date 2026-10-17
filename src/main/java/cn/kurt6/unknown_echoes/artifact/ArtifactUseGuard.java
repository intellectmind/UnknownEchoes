package cn.kurt6.unknown_echoes.artifact;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** 服务端同步施放期间，同一玩家的同一神器只能进入一次，包括零冷却副功能。 */
public final class ArtifactUseGuard {
    private static final Set<UseKey> ACTIVE = new HashSet<>();

    private ArtifactUseGuard() {
    }

    public static boolean run(ServerPlayer player, ArtifactType type, BooleanSupplier action) {
        UseKey key = new UseKey(player.getUUID(), type);
        if (!ACTIVE.add(key)) {
            return false;
        }
        try {
            return action.getAsBoolean();
        } finally {
            ACTIVE.remove(key);
        }
    }

    private record UseKey(UUID playerId, ArtifactType type) {
    }
}
