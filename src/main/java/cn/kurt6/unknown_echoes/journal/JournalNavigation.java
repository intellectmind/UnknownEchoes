package cn.kurt6.unknown_echoes.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class JournalNavigation {
    public static final int NEARBY_RADIUS = 24;

    public enum Status { TARGET, OTHER_DIMENSION, NEARBY, NONE }

    public record Result(ExplorationClue clue, Status status) { }

    private JournalNavigation() { }

    public static Component structureName(ExplorationClue clue) {
        ResourceLocation id = ResourceLocation.tryParse(clue.structureId());
        return id == null ? Component.translatable("artifact.unknown_echoes.storm_compass.unknown_ruin")
                : Component.translatable("structure." + id.getNamespace() + "." + id.getPath());
    }

    public static Component dimensionName(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            return Component.translatable("overview.unknown_echoes.status.region_unknown");
        }
        String namespace = id.getNamespace().equals("minecraft") ? "unknown_echoes" : id.getNamespace();
        return Component.translatableWithFallback("dimension." + namespace + "." + id.getPath(), id.getPath());
    }

    // 手选地点始终优先；抵达附近或跨维度时不自动换成别处。
    public static Result resolve(List<ExplorationClue> clues, ExplorationClue destination,
                                 String dimension, BlockPos origin, String tuning) {
        if (destination != null) {
            for (ExplorationClue clue : clues) {
                if (destination.sameSite(clue)) {
                    if (!clue.dimension().equals(dimension)) {
                        return new Result(clue, Status.OTHER_DIMENSION);
                    }
                    return new Result(clue, isNearby(clue, origin) ? Status.NEARBY : Status.TARGET);
                }
            }
        }
        ExplorationClue preferred = null;
        ExplorationClue fallback = null;
        double preferredDistance = Double.MAX_VALUE;
        double fallbackDistance = Double.MAX_VALUE;
        boolean preferVisited = "homeward".equals(tuning);
        for (ExplorationClue clue : clues) {
            if (!clue.dimension().equals(dimension) || isNearby(clue, origin)) {
                continue;
            }
            double distance = clue.pos().distSqr(origin);
            if (clue.visited() == preferVisited) {
                if (distance < preferredDistance) {
                    preferred = clue;
                    preferredDistance = distance;
                }
            } else if (distance < fallbackDistance) {
                fallback = clue;
                fallbackDistance = distance;
            }
        }
        ExplorationClue target = preferred != null ? preferred : fallback;
        return new Result(target, target == null ? Status.NONE : Status.TARGET);
    }

    // 主题目标只使用个人已知地点，同维度优先，其次已到访地点；不推断当地机关状态。
    public static ExplorationClue knownSite(List<ExplorationClue> clues, String structureId,
                                            String dimension, BlockPos origin) {
        ExplorationClue best = null;
        int bestRank = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (ExplorationClue clue : clues) {
            if (!clue.structureId().equals(structureId)) continue;
            boolean local = clue.dimension().equals(dimension);
            int rank = (local ? 0 : 2) + (clue.visited() ? 0 : 1);
            double distance = local ? clue.pos().distSqr(origin) : 0;
            if (rank < bestRank || (rank == bestRank && distance < bestDistance)) {
                best = clue;
                bestRank = rank;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean isNearby(ExplorationClue clue, BlockPos origin) {
        double dx = (double) clue.pos().getX() - origin.getX();
        double dz = (double) clue.pos().getZ() - origin.getZ();
        return dx * dx + dz * dz < NEARBY_RADIUS * NEARBY_RADIUS;
    }
}
