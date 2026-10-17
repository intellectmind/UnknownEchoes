package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.List;

public final class WorldEventTuning {
    private WorldEventTuning() { }

    public static int weight(WorldEventType type) {
        return switch (type) {
            case ECHO_FOG -> ServerConfig.WORLD_EVENT_WEIGHT_ECHO_FOG.get();
            case MIRROR_ANOMALY -> ServerConfig.WORLD_EVENT_WEIGHT_MIRROR_ANOMALY.get();
            case STORM_OMEN -> ServerConfig.WORLD_EVENT_WEIGHT_STORM_OMEN.get();
            case LOST_CARAVAN -> ServerConfig.WORLD_EVENT_WEIGHT_LOST_CARAVAN.get();
            case METEOR_AFTERGLOW -> ServerConfig.WORLD_EVENT_WEIGHT_METEOR_AFTERGLOW.get();
            case ECHO_STORM -> ServerConfig.WORLD_EVENT_WEIGHT_ECHO_STORM.get();
            case STRATA_AFTERSHOCK -> ServerConfig.WORLD_EVENT_WEIGHT_STRATA_AFTERSHOCK.get();
            case RIFT_STORM -> ServerConfig.WORLD_EVENT_WEIGHT_RIFT_STORM.get();
            case STRATA_RUPTURE -> ServerConfig.WORLD_EVENT_WEIGHT_STRATA_RUPTURE.get();
            case FAR_BELL_PROCESSION -> ServerConfig.WORLD_EVENT_WEIGHT_FAR_BELL_PROCESSION.get();
            case MEMORY_FIREFLY_TIDE -> ServerConfig.WORLD_EVENT_WEIGHT_MEMORY_FIREFLY_TIDE.get();
            case GRAVITY_TIDE -> ServerConfig.WORLD_EVENT_WEIGHT_GRAVITY_TIDE.get();
            case ARCHIVE_DRIFT -> ServerConfig.WORLD_EVENT_WEIGHT_ARCHIVE_DRIFT.get();
            case SILENT_PULSE -> ServerConfig.WORLD_EVENT_WEIGHT_SILENT_PULSE.get();
            case FOLDED_PROCESSION -> ServerConfig.WORLD_EVENT_WEIGHT_FOLDED_PROCESSION.get();
            case CRYSTAL_CHORUS -> ServerConfig.WORLD_EVENT_WEIGHT_CRYSTAL_CHORUS.get();
            case RIFT_AFTERIMAGE -> ServerConfig.WORLD_EVENT_WEIGHT_RIFT_AFTERIMAGE.get();
            case INTERLAYER_RESPITE -> ServerConfig.WORLD_EVENT_WEIGHT_INTERLAYER_RESPITE.get();
        };
    }

    /** 候选已通过维度、群系和开关筛选；全部权重为零时返回 null。 */
    public static WorldEventType select(List<WorldEventType> candidates, RandomSource random) {
        int[] weights = new int[candidates.size()];
        int total = 0;
        for (int i = 0; i < candidates.size(); i++) {
            weights[i] = weight(candidates.get(i));
            total += weights[i];
        }
        if (total == 0) return null;
        // 同次抽取使用同一份权重快照；默认全为 1 时保留原 nextInt 的界限与顺序。
        int roll = random.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights[i];
            if (roll < 0) return candidates.get(i);
        }
        throw new IllegalStateException("World event weight total does not match its snapshot");
    }

    public static double naturalChance(ResourceLocation dimension) {
        double multiplier = 1.0D;
        if (dimension.equals(Level.OVERWORLD.location())) {
            multiplier = ServerConfig.OVERWORLD_WORLD_EVENT_CHANCE_MULTIPLIER.get();
        } else if (dimension.equals(EchoPermission.ECHO_REALM_ID)) {
            multiplier = ServerConfig.ECHO_REALM_WORLD_EVENT_CHANCE_MULTIPLIER.get();
        } else if (dimension.equals(EchoPermission.DEEP_ECHOES_ID)) {
            multiplier = ServerConfig.DEEP_ECHOES_WORLD_EVENT_CHANCE_MULTIPLIER.get();
        }
        double chance = ServerConfig.WORLD_EVENT_CHANCE.get() * multiplier;
        return Double.isFinite(chance) ? Math.clamp(chance, 0.0D, 1.0D) : 0.0D;
    }

    public static int distance(RandomSource random) {
        int first = ServerConfig.WORLD_EVENT_MIN_PLAYER_DISTANCE.get();
        int second = ServerConfig.WORLD_EVENT_MAX_PLAYER_DISTANCE.get();
        int minimum = Math.min(first, second);
        int maximum = Math.max(first, second);
        return minimum + random.nextInt(maximum - minimum + 1);
    }
}
