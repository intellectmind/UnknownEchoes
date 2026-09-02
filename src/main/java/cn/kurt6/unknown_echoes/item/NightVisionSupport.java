package cn.kurt6.unknown_echoes.item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class NightVisionSupport {
    /** 原版开始做闪烁淡出的剩余时长(10 秒)。持续型夜视的剩余时长不得低于该值。 */
    public static final int VANILLA_FLICKER_TICKS = 200;
    /** 提前补时长的阈值(15 秒):比闪烁阈值高 5 秒,留出轮询与网络同步余量。 */
    public static final int REFRESH_THRESHOLD_TICKS = 300;
    /** 持续型夜视的最小时长(30 秒),同时覆盖刷新阈值与常见轮询周期。 */
    public static final int MIN_SUSTAINED_DURATION_TICKS = 600;

    /** 超过该 tick 数没有再续期,视为该来源已停止供给。 */
    private static final int CLAIM_GRACE_TICKS = 60;

    public enum Source {
        ARMOR_SET,
        RESONATOR,
        TRUE_SIGHT,
        /** 世界事件与境域环境事件(按半径供给,玩家走出半径没有回调)。 */
        AREA_EVENT
    }

    private static final Map<UUID, Map<Source, Long>> CLAIMS = new HashMap<>();

    private NightVisionSupport() {
    }

    public static int sustainedDuration(int pollIntervalTicks) {
        int safeInterval = Math.max(1, pollIntervalTicks);
        return Math.max(MIN_SUSTAINED_DURATION_TICKS, REFRESH_THRESHOLD_TICKS + 2 * safeInterval);
    }

    /** 持续型夜视是否需要补时长。更高等级或无限时长的外部夜视不打断。 */
    public static boolean needsRefresh(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
        if (current == null) {
            return true;
        }
        if (current.getAmplifier() > 0 || current.isInfiniteDuration()) {
            return false;
        }
        return current.getDuration() <= REFRESH_THRESHOLD_TICKS;
    }

    public static void refresh(LivingEntity entity, int durationTicks, int pollIntervalTicks,
                              boolean showIcon) {
        if (!needsRefresh(entity)) {
            return;
        }
        int duration = Math.max(durationTicks, sustainedDuration(pollIntervalTicks));
        entity.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, duration, 0, true, false, showIcon));
    }

    public static void sustain(ServerPlayer player, Source source, int durationTicks,
                               int pollIntervalTicks, boolean showIcon, long gameTime) {
        CLAIMS.computeIfAbsent(player.getUUID(), key -> new EnumMap<>(Source.class))
                .put(source, gameTime);
        refresh(player, durationTicks, pollIntervalTicks, showIcon);
    }

    public static void release(ServerPlayer player, Source source) {
        Map<Source, Long> sources = CLAIMS.get(player.getUUID());
        if (sources == null) {
            return;
        }
        sources.remove(source);
        if (sources.isEmpty()) {
            CLAIMS.remove(player.getUUID());
            clearSustained(player);
        }
    }

    /** 每 tick 回收过期 claim：区域事件等没有"离开"回调的来源靠它兜底。 */
    public static void tickClaims(MinecraftServer server, long gameTime) {
        if (CLAIMS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Map<Source, Long>>> players = CLAIMS.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Map<Source, Long>> entry = players.next();

            entry.getValue().values().removeIf(claimed -> {
                long elapsed = gameTime - claimed;
                return elapsed < 0L || elapsed > CLAIM_GRACE_TICKS;
            });
            if (!entry.getValue().isEmpty()) {
                continue;
            }
            players.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                clearSustained(player);
            }
        }
    }

    /** 供给中断时清除本 Mod 施加的持续型夜视,不留残余时长闪烁。 */
    public static void clearSustained(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
        if (isSustained(current)) {
            entity.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    public static boolean isSustained(MobEffectInstance instance) {
        return instance != null
                && instance.getAmplifier() == 0
                && instance.isAmbient()
                && !instance.isVisible();
    }

    public static void clearClaims(UUID playerId) {
        CLAIMS.remove(playerId);
    }

    public static void clearAllClaims() {
        CLAIMS.clear();
    }
}
