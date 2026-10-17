package cn.kurt6.unknown_echoes.block.puzzle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WonderTrialProgress {
    public static final int WAVES = 3;
    public static final int MAX_ALIVE = 10;
    private UUID run = UUID.randomUUID();
    private int wave;
    private int spawned;
    private boolean active;
    private final Set<UUID> remaining = new HashSet<>();

    public UUID run() { return run; }
    public int wave() { return wave; }
    public int spawned() { return spawned; }
    public boolean active() { return active; }
    public boolean complete() { return wave == WAVES; }
    public int quota() { return complete() ? 0 : waveQuota(wave); }
    private static int waveQuota(int wave) { return 12 + wave * 6; }
    public Set<UUID> remaining() { return Set.copyOf(remaining); }
    public boolean canSpawn() { return active && spawned < quota() && remaining.size() < MAX_ALIVE; }

    public boolean begin() {
        if (active || complete()) return false;
        active = true;
        return true;
    }

    public boolean add(UUID id) {
        if (!canSpawn() || !remaining.add(id)) return false;
        spawned++;
        return true;
    }

    public boolean cancelSpawn(UUID epoch, UUID id) {
        if (!active || !run.equals(epoch) || !remaining.remove(id)) return false;
        spawned--;
        return true;
    }

    public boolean defeated(UUID epoch, UUID id) {
        if (!active || !run.equals(epoch) || !remaining.remove(id)) return false;
        if (spawned == quota() && remaining.isEmpty()) {
            wave++;
            spawned = 0;
            active = false;
        }
        return true;
    }

    public boolean reset() {
        if (complete()) return false;
        run = UUID.randomUUID();
        wave = 0;
        spawned = 0;
        active = false;
        remaining.clear();
        return true;
    }

    public static WonderTrialProgress restore(UUID run, int wave, int spawned, boolean active, Set<UUID> remaining) {
        WonderTrialProgress state = new WonderTrialProgress();
        if (run == null || wave < 0 || wave > WAVES || spawned < 0 || spawned > waveQuota(wave)
                || remaining.size() > Math.min(spawned, MAX_ALIVE)
                || (!active && (spawned != 0 || !remaining.isEmpty()))
                || (wave == WAVES && active)
                || (active && spawned == waveQuota(wave) && remaining.isEmpty())) return state;
        state.run = run;
        state.wave = wave;
        state.spawned = spawned;
        state.active = active;
        state.remaining.addAll(remaining);
        return state;
    }
}
