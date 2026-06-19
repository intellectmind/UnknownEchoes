package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.ArtifactSyncPayload;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端神器/能量展示缓存(纯表现层,5.8:客户端只展示、服务端是数据源)。
 * 能量按服务端同步的百分比速率 + gameTime 戳本地外推,服务端只在变更时推送。
 */
public class ClientArtifactCache {

    /** 单件神器的展示数据(同步条目 "id:level:tuning:serial:cooldownEnd" 解析结果)。 */
    public record Entry(int level, String tuning, int serial, long cooldownEnd) {}

    private static final Map<String, Entry> ARTIFACTS = new HashMap<>();
    private static int energyTwentieths = -1;
    private static int maxEnergy = 100;
    private static int regenHundredthsPerTick = 100;
    private static long syncGameTime;

    public static void update(ArtifactSyncPayload payload) {
        ARTIFACTS.clear();
        for (String line : payload.artifacts()) {
            String[] parts = line.split(":", 5);
            if (parts.length == 5) {
                try {
                    ARTIFACTS.put(parts[0], new Entry(Integer.parseInt(parts[1]), parts[2],
                            Integer.parseInt(parts[3]), Long.parseLong(parts[4])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        energyTwentieths = payload.energyTwentieths();
        maxEnergy = payload.maxEnergy();
        regenHundredthsPerTick = payload.regenHundredthsPerTick();
        syncGameTime = payload.gameTime();
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

    /** 当前能量(整点,本地按同步速率外推)。尚未同步过返回 -1(UI 显示占位)。 */
    public static int getEnergy() {
        if (energyTwentieths < 0) {
            return -1;
        }
        long now = currentGameTime();
        long elapsed = Math.max(0, now - syncGameTime);
        long value = Math.min((long) maxEnergy * 20,
                energyTwentieths + elapsed * (long) regenHundredthsPerTick / 100L);
        return (int) (value / 20);
    }

    /** 神器剩余冷却秒数(0=就绪)。 */
    public static int getCooldownSeconds(String artifactId) {
        Entry entry = ARTIFACTS.get(artifactId);
        if (entry == null) {
            return 0;
        }
        long remain = entry.cooldownEnd() - currentGameTime();
        return remain <= 0 ? 0 : (int) Math.ceil(remain / 20.0);
    }

    private static long currentGameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? syncGameTime : level.getGameTime();
    }

    public static void clear() {
        ARTIFACTS.clear();
        energyTwentieths = -1;
        regenHundredthsPerTick = 100;
    }
}
