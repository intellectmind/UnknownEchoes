package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.AbilityCooldownSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** 回响能力冷却的客户端只读表现缓存。 */
public final class ClientAbilityCooldownCache {
    private static long gravityEnd;
    private static int gravityDuration;
    private static long voidEnd;
    private static int voidDuration;
    private static long syncGameTime;
    private static long localGameTimeAtSync;

    private ClientAbilityCooldownCache() {
    }

    public static void update(AbilityCooldownSyncPayload payload) {
        gravityEnd = payload.gravityEnd();
        gravityDuration = payload.gravityDuration();
        voidEnd = payload.voidEnd();
        voidDuration = payload.voidDuration();
        syncGameTime = payload.gameTime();
        var level = Minecraft.getInstance().level;
        localGameTimeAtSync = level == null ? 0L : level.getGameTime();
    }

    public static boolean hasGravityCooldown() {
        return gravityEnd > currentGameTime();
    }

    public static boolean hasVoidCooldown() {
        return voidEnd > currentGameTime();
    }

    public static float getGravityProgress() {
        return recoveryProgress(gravityEnd, gravityDuration);
    }

    public static float getVoidProgress() {
        return recoveryProgress(voidEnd, voidDuration);
    }

    private static float recoveryProgress(long end, int duration) {
        if (duration <= 0) {
            return 1.0F;
        }
        long remaining = Math.max(0L, end - currentGameTime());
        return Mth.clamp(1.0F - (float) remaining / duration, 0.0F, 1.0F);
    }

    private static long currentGameTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return syncGameTime;
        }
        return syncGameTime + Math.max(0L, level.getGameTime() - localGameTimeAtSync);
    }

    public static void clear() {
        gravityEnd = 0L;
        gravityDuration = 0;
        voidEnd = 0L;
        voidDuration = 0;
        syncGameTime = 0L;
        localGameTimeAtSync = 0L;
    }
}
