package cn.kurt6.unknown_echoes.item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Collections;

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
        TIDE,
        TRUE_SIGHT,
        /** 世界事件与境域环境事件(按半径供给,玩家走出半径没有回调)。 */
        AREA_EVENT
    }

    private static final Map<UUID, Map<Source, Long>> CLAIMS = new HashMap<>();
    private static final Set<UUID> PENDING_CLEANUP = new HashSet<>();
    private static final Set<LivingEntity> REFRESHING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<LivingEntity, MobEffectInstance> REMOVING = new IdentityHashMap<>();
    private static long nextClaimCheck = Long.MIN_VALUE;
    private static long lastClaimCheck = Long.MIN_VALUE;
    private static long latestClaimTime = Long.MIN_VALUE;
    private static boolean cleaningClaims;

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
        if (!REFRESHING.add(entity)) return;
        try {
            int duration = Math.max(durationTicks, sustainedDuration(pollIntervalTicks));
            entity.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, duration, 0, true, false, showIcon));
        } finally {
            REFRESHING.remove(entity);
        }
    }

    public static void sustain(ServerPlayer player, Source source, int durationTicks,
                               int pollIntervalTicks, boolean showIcon, long gameTime) {
        CLAIMS.computeIfAbsent(player.getUUID(), key -> new EnumMap<>(Source.class))
                .put(source, gameTime);
        PENDING_CLEANUP.remove(player.getUUID());
        nextClaimCheck = Math.min(nextClaimCheck, nextCheckAfter(gameTime));
        latestClaimTime = Math.max(latestClaimTime, gameTime);
        try {
            refresh(player, durationTicks, pollIntervalTicks, showIcon);
        } finally {
            if (!CLAIMS.containsKey(player.getUUID())) clearReleased(player);
        }
    }

    public static void release(ServerPlayer player, Source source) {
        Map<Source, Long> sources = CLAIMS.get(player.getUUID());
        if (sources == null) {
            return;
        }
        sources.remove(source);
        if (sources.isEmpty()) {
            CLAIMS.remove(player.getUUID());
            clearReleased(player);
        }
    }

    /** 逐 tick 检查时钟，到期后先摘除整批租约，再触发可重入的效果移除。 */
    public static void tickClaims(MinecraftServer server, long gameTime) {
        if (cleaningClaims) return;
        boolean scanClaims = gameTime < lastClaimCheck || gameTime >= nextClaimCheck || gameTime < latestClaimTime;
        lastClaimCheck = gameTime;
        if (!scanClaims && PENDING_CLEANUP.isEmpty()) return;
        cleaningClaims = true;
        try {
            if (scanClaims) {
                nextClaimCheck = Long.MAX_VALUE;
                latestClaimTime = Long.MIN_VALUE;
                Iterator<Map.Entry<UUID, Map<Source, Long>>> players = CLAIMS.entrySet().iterator();
                while (players.hasNext()) {
                    Map.Entry<UUID, Map<Source, Long>> entry = players.next();
                    Iterator<Long> sources = entry.getValue().values().iterator();
                    while (sources.hasNext()) {
                        long claimed = sources.next();
                        long elapsed = gameTime - claimed;
                        if (claimed > gameTime || elapsed < 0L || elapsed > CLAIM_GRACE_TICKS) {
                            sources.remove();
                        } else {
                            nextClaimCheck = Math.min(nextClaimCheck, nextCheckAfter(claimed));
                            latestClaimTime = Math.max(latestClaimTime, claimed);
                        }
                    }
                    if (entry.getValue().isEmpty()) {
                        players.remove();
                        PENDING_CLEANUP.add(entry.getKey());
                    }
                }
            }
            RuntimeException failure = null;
            for (UUID playerId : java.util.List.copyOf(PENDING_CLEANUP)) {
                if (!PENDING_CLEANUP.contains(playerId)) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    PENDING_CLEANUP.remove(playerId);
                    continue;
                }
                try {
                    clearReleased(player);
                } catch (RuntimeException exception) {
                    if (failure == null) failure = exception;
                    else if (failure != exception) failure.addSuppressed(exception);
                }
            }
            if (failure != null) throw failure;
        } finally {
            cleaningClaims = false;
        }
    }

    private static long nextCheckAfter(long claimed) {
        return claimed > Long.MAX_VALUE - CLAIM_GRACE_TICKS - 1L
                ? Long.MAX_VALUE : claimed + CLAIM_GRACE_TICKS + 1L;
    }

    private static void clearReleased(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (CLAIMS.containsKey(playerId)) {
            PENDING_CLEANUP.remove(playerId);
            return;
        }
        PENDING_CLEANUP.add(playerId);
        if (REMOVING.containsKey(player)) return;
        clearSustained(player);
        PENDING_CLEANUP.remove(playerId);
    }

    /** 供给中断时清除本 Mod 施加的持续型夜视,不留残余时长闪烁。 */
    public static void clearSustained(LivingEntity entity) {
        if (REMOVING.containsKey(entity)) return;
        if (entity instanceof ServerPlayer player && CLAIMS.containsKey(player.getUUID())) return;
        MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
        if (isSustained(current)) {
            REMOVING.put(entity, current);
            try {
                entity.removeEffect(MobEffects.NIGHT_VISION);
            } finally {
                REMOVING.remove(entity);
            }
        }
    }

    /** 原生移除事件返回后复核租约和效果身份；清理范围外的移除保持原生行为。 */
    public static boolean canRemoveDuringCleanup(LivingEntity entity, Holder<MobEffect> effect) {
        MobEffectInstance expected = REMOVING.get(entity);
        if (expected == null || !effect.equals(MobEffects.NIGHT_VISION)) return true;
        return entity.getEffect(effect) == expected && isSustained(expected)
                && (!(entity instanceof ServerPlayer player) || !CLAIMS.containsKey(player.getUUID()));
    }

    public static boolean isSustained(MobEffectInstance instance) {
        return instance != null
                && instance.getAmplifier() == 0
                && instance.isAmbient()
                && !instance.isVisible();
    }

    public static void clearClaims(UUID playerId) {
        CLAIMS.remove(playerId);
        PENDING_CLEANUP.remove(playerId);
    }

    public static void clearAllClaims() {
        CLAIMS.clear();
        PENDING_CLEANUP.clear();
        nextClaimCheck = Long.MIN_VALUE;
        lastClaimCheck = Long.MIN_VALUE;
        latestClaimTime = Long.MIN_VALUE;
    }
}
