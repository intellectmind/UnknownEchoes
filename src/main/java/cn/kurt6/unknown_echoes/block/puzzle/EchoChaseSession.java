package cn.kurt6.unknown_echoes.block.puzzle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EchoChaseSession {
    public static final int WAVES = 3;
    public static final int MAX_ALIVE = 12;
    public static final int SPAWN_INTERVAL_TICKS = 10;
    public static final int LIMIT_TICKS = 240 * 20;
    public static final int REST_TICKS = 5 * 20;
    public static final int WARNING_TICKS = 30;
    public static final int PULSE_TICKS = 8 * 20;
    public static final double RADIUS = 2.0;
    private final UUID id = UUID.randomUUID();
    private final long started;
    private final Set<UUID> remaining = new HashSet<>();
    private long readyAt;
    private long spawnAt;
    private long pulseAt;
    private long finishedAt = -1;
    private int wave;
    private int spawned;
    private Strike strike;

    public record Strike(double x, double y, double z, long due) {
        // 判定使用双脚位置；环的上下各 1.5 格有效，不贯穿楼层。
        public boolean contains(double px, double py, double pz) {
            return Math.abs(py - y) <= 1.5 && (px - x) * (px - x) + (pz - z) * (pz - z) <= RADIUS * RADIUS;
        }
    }

    public EchoChaseSession(long now) { started = now; readyAt = now; spawnAt = now; pulseAt = now + REST_TICKS; }
    public UUID id() { return id; }
    public int wave() { return wave; }
    public int spawned() { return spawned; }
    public int quota() { return 18 + wave * 6; }
    public int left() { return complete() ? 0 : quota() - spawned + remaining.size(); }
    public Set<UUID> remaining() { return Set.copyOf(remaining); }
    public boolean complete() { return finishedAt >= 0; }
    public boolean expired(long now) { return !complete() && now - started >= LIMIT_TICKS; }
    public long elapsed(long now) { return Math.max(0, (complete() ? finishedAt : now) - started); }
    public int secondsLeft(long now) { return (int) Math.max(0, (LIMIT_TICKS - elapsed(now) + 19) / 20); }
    public boolean resting(long now) { return now < readyAt; }
    public double health() { return 36 + wave * 12; }
    public double attack() { return 8 + wave; }
    public Strike strike() { return strike; }

    public boolean canSpawn(long now) {
        return !complete() && !expired(now) && now >= readyAt && now >= spawnAt
                && spawned < quota() && remaining.size() < MAX_ALIVE;
    }
    public boolean reserve(UUID guard, long now) {
        if (!canSpawn(now) || !remaining.add(guard)) return false;
        spawned++;
        spawnAt = now + SPAWN_INTERVAL_TICKS;
        return true;
    }
    public boolean cancel(UUID run, UUID guard) {
        if (!id.equals(run) || !remaining.remove(guard)) return false;
        spawned--;
        return true;
    }
    public boolean defeated(UUID run, UUID guard, long now) {
        if (!id.equals(run) || complete() || expired(now) || !remaining.remove(guard)) return false;
        if (spawned == quota() && remaining.isEmpty()) {
            wave++;
            strike = null;
            if (wave == WAVES) finishedAt = now;
            else {
                spawned = 0;
                readyAt = now + REST_TICKS;
                pulseAt = readyAt + REST_TICKS;
            }
        }
        return true;
    }
    public Strike warn(long now, double x, double y, double z) {
        if (complete() || expired(now) || resting(now) || strike != null || now < pulseAt) return null;
        strike = new Strike(x, y, z, now + WARNING_TICKS);
        pulseAt = now + PULSE_TICKS;
        return strike;
    }
    public Strike resolve(long now) {
        if (strike == null || now < strike.due()) return null;
        Strike result = strike;
        strike = null;
        return result;
    }
}
