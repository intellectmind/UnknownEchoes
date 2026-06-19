package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.server.level.ServerPlayer;

public final class WeaponEffectGuard {
    private static final String KEY = "ue_weapon_effect_guard";

    private WeaponEffectGuard() {
    }

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    public static void run(ServerPlayer player, Runnable action) {
        var data = player.getPersistentData();
        boolean previous = data.getBoolean(KEY);
        data.putBoolean(KEY, true);
        try {
            action.run();
        } finally {
            data.putBoolean(KEY, previous);
        }
    }
}
