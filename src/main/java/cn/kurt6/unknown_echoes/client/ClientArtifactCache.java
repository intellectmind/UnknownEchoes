package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.ArtifactSyncPayload;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** 客户端神器与能量展示缓存；服务端是唯一数据源。 */
public class ClientArtifactCache {

    /** 单件神器的展示数据；耗能单位为 1/20 点。 */
    public record Entry(int level, String tuning, int serial, long cooldownEnd,
                        int energyCostTwentieths) {}

    private static final Map<String, Entry> ARTIFACTS = new HashMap<>();
    private static int energyTwentieths = -1;
    private static int maxEnergy = 100;
    private static int regenHundredthsPerTick = 100;
    private static long syncGameTime;
    private static long localGameTimeAtSync;

    public static void update(ArtifactSyncPayload payload) {
        ARTIFACTS.clear();
        for (String line : payload.artifacts()) {
            String[] parts = line.split(":", 6);
            if (parts.length == 6) {
                try {
                    ARTIFACTS.put(parts[0], new Entry(Integer.parseInt(parts[1]), parts[2],
                            Integer.parseInt(parts[3]), Long.parseLong(parts[4]),
                            Integer.parseInt(parts[5])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        energyTwentieths = payload.energyTwentieths();
        maxEnergy = payload.maxEnergy();
        regenHundredthsPerTick = payload.regenHundredthsPerTick();
        syncGameTime = payload.gameTime();
        var level = Minecraft.getInstance().level;
        localGameTimeAtSync = level == null ? 0L : level.getGameTime();
    }

    public static boolean isClaimed(String artifactId) {
        return ARTIFACTS.containsKey(artifactId);
    }

    public static Entry get(String artifactId) {
        return ARTIFACTS.get(artifactId);
    }

    public static int getMaxEnergy() {
        return maxEnergy;
    }

    public static boolean isEnergyRegenerating() {
        return energyTwentieths >= 0 && regenHundredthsPerTick > 0 && getEnergy() < maxEnergy;
    }

    /** 当前能量(整点,本地按同步速率外推)。尚未同步过返回 -1(UI 显示占位)。 */
    public static int getEnergy() {
        if (energyTwentieths < 0) {
            return -1;
        }
        long now = currentGameTime();
        long elapsed = syncGameTime >= 0L && syncGameTime <= now ? now - syncGameTime : 0L;
        long capacity = Math.max(0L, (long) maxEnergy * 20L);
        long regenerated = saturatedMultiply(elapsed, Math.max(0L, regenHundredthsPerTick)) / 100L;
        long baseEnergy = Math.max(0L, energyTwentieths);
        long value = Math.min(capacity,
                baseEnergy > Long.MAX_VALUE - regenerated
                        ? Long.MAX_VALUE : baseEnergy + regenerated);
        return (int) (value / 20);
    }

    public static int getCooldownSeconds(String artifactId) {
        Entry entry = ARTIFACTS.get(artifactId);
        if (entry == null) {
            return 0;
        }
        long now = currentGameTime();
        long remain = entry.cooldownEnd() > now ? entry.cooldownEnd() - now : 0L;
        long seconds = remain / 20L + (remain % 20L == 0L ? 0L : 1L);
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }

    private static long currentGameTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return syncGameTime;
        }
        long base = Math.max(0L, syncGameTime);
        long localNow = level.getGameTime();
        long elapsed = localNow >= localGameTimeAtSync ? localNow - localGameTimeAtSync : 0L;
        return base > Long.MAX_VALUE - elapsed ? Long.MAX_VALUE : base + elapsed;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    public static void clear() {
        ARTIFACTS.clear();
        energyTwentieths = -1;
        maxEnergy = 100;
        regenHundredthsPerTick = 100;
        syncGameTime = 0L;
        localGameTimeAtSync = 0L;
    }
}
