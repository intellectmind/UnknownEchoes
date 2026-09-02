package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** 线索地图的生成兜底：逐个生成结构候选区块，避免把未加载目标误报成附近没有。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ClueMapSearch {
    private static final int MAX_CANDIDATES = 512;
    private static final int MAX_CONSECUTIVE_ERRORS = 16;
    private static final int MAX_RING = 128;
    private static final Map<UUID, SearchTask> ACTIVE = new HashMap<>();
    private ClueMapSearch() {
    }

    public enum RequestResult {
        STARTED,
        ALREADY_SEARCHING,
        UNAVAILABLE
    }

    @FunctionalInterface
    public interface Completion {
        void accept(ServerPlayer player, ClueLocator.LocateOutcome outcome);
    }

    public static RequestResult request(ServerPlayer player, List<ResourceLocation> structureIds,
                                        ResourceLocation preferredId, Completion completion) {
        if (!(player.level() instanceof ServerLevel level)) {
            return RequestResult.UNAVAILABLE;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            return RequestResult.ALREADY_SEARCHING;
        }

        Set<String> knownIds = new HashSet<>();
        String dimension = level.dimension().location().toString();
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (clue.dimension().equals(dimension)) {
                knownIds.add(clue.structureId());
            }
        }

        List<ResourceLocation> unknownIds = structureIds.stream()
                .filter(id -> id != null && !knownIds.contains(id.toString()))
                .distinct()
                .toList();
        if (unknownIds.isEmpty()) {
            return RequestResult.UNAVAILABLE;
        }

        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
        List<SearchTarget> targets = new ArrayList<>();
        for (ResourceLocation id : unknownIds) {
            Holder.Reference<Structure> holder = registry.getHolder(
                    ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
            if (holder == null) {
                continue;
            }
            for (var placement : generatorState.getPlacementsForStructure(holder)) {
                if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
                    targets.add(new SearchTarget(id, holder.value(), randomSpread));
                }
            }
        }
        if (targets.isEmpty()) {
            return RequestResult.UNAVAILABLE;
        }

        ResourceLocation preferred = preferredId;
        if (preferred == null || !unknownIds.contains(preferred)) {
            preferred = unknownIds.get(player.getRandom().nextInt(unknownIds.size()));
        }
        SearchTask task = new SearchTask(player.getUUID(), level.dimension(), level.getSeed(),
                player.blockPosition(), generatorState, targets, preferred, completion);
        ACTIVE.put(player.getUUID(), task);
        searchNext(player.server, task);
        return RequestResult.STARTED;
    }

    private static void searchNext(MinecraftServer server, SearchTask task) {
        if (server.isStopped() || ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerLevel level = server.getLevel(task.dimension);
        if (level == null) {
            finish(server, task, new ClueLocator.LocateOutcome(ClueLocator.LocateResult.NOT_FOUND, null));
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId, task);
            return;
        }
        if (task.inFlight || level.getGameTime() < task.nextStepAt) {
            return;
        }
        if (task.searchedCandidates >= MAX_CANDIDATES) {
            finish(server, task, new ClueLocator.LocateOutcome(ClueLocator.LocateResult.NOT_FOUND, null));
            return;
        }

        Candidate candidate = task.cursor.next();
        if (candidate == null) {
            finish(server, task, new ClueLocator.LocateOutcome(ClueLocator.LocateResult.NOT_FOUND, null));
            return;
        }
        task.searchedCandidates++;
        task.inFlight = true;
        loadChunk(level, candidate.pos(), ChunkStatus.STRUCTURE_STARTS)
                .whenComplete((chunk, error) -> server.execute(() ->
                        handleStructureStarts(server, task, level, candidate, chunk, error)));
    }

    private static void handleStructureStarts(MinecraftServer server, SearchTask task, ServerLevel level,
                                              Candidate candidate, ChunkAccess chunk, Throwable error) {
        if (ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId);
            return;
        }
        if (error != null || chunk == null) {
            task.consecutiveErrors++;
            if (task.consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                finish(server, task, new ClueLocator.LocateOutcome(ClueLocator.LocateResult.NOT_FOUND, null));
                return;
            }
            deferNext(level, task);
            return;
        }
        task.consecutiveErrors = 0;
        StructureStart start = chunk.getStartForStructure(candidate.target().structure());
        if (start == null || !start.isValid()) {
            deferNext(level, task);
            return;
        }

        loadChunk(level, candidate.pos(), ChunkStatus.FULL)
                .whenComplete((fullChunk, fullError) -> server.execute(() ->
                        handleFullChunk(server, task, level, candidate, fullChunk, fullError)));
    }

    private static void handleFullChunk(MinecraftServer server, SearchTask task, ServerLevel level,
                                        Candidate candidate, ChunkAccess chunk, Throwable error) {
        if (ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId);
            return;
        }
        if (error != null || chunk == null) {
            deferNext(level, task);
            return;
        }
        StructureStart start = chunk.getStartForStructure(candidate.target().structure());
        if (start == null || !start.isValid()) {
            deferNext(level, task);
            return;
        }

        var position = start.getBoundingBox().getCenter();
        boolean added = JournalManager.addClue(player, new ExplorationClue(
                candidate.target().id().toString(), level.dimension().location().toString(), position, false));
        if (added) {
            JournalManager.syncJournal(player);
        }
        finish(server, task, new ClueLocator.LocateOutcome(
                added ? ClueLocator.LocateResult.ADDED : ClueLocator.LocateResult.ALREADY_KNOWN,
                candidate.target().id()));
    }

    private static void deferNext(ServerLevel level, SearchTask task) {
        task.inFlight = false;
        task.nextStepAt = level.getGameTime() + 1L;
    }

    private static CompletableFuture<ChunkAccess> loadChunk(ServerLevel level, ChunkPos pos, ChunkStatus status) {
        try {
            // 在服务器主线程发起请求,避免后台线程 join 主线程 future。
            return level.getChunkSource().getChunkFuture(pos.x, pos.z, status, true)
                    .thenApply(chunk -> chunk.orElse(null));
        } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static void finish(MinecraftServer server, SearchTask task, ClueLocator.LocateOutcome outcome) {
        if (ACTIVE.remove(task.playerId) != task) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player != null) {
            task.completion.accept(player, outcome);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (SearchTask task : List.copyOf(ACTIVE.values())) {
            searchNext(server, task);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    private record SearchTarget(ResourceLocation id, Structure structure,
                                RandomSpreadStructurePlacement placement) {
    }

    private record Candidate(ChunkPos pos, SearchTarget target, double distanceSquared) {
    }

    private static final class SearchTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final CandidateCursor cursor;
        private final Completion completion;
        private long nextStepAt;
        private boolean inFlight;
        private int searchedCandidates;
        private int consecutiveErrors;

        private SearchTask(UUID playerId, ResourceKey<Level> dimension, long seed,
                           net.minecraft.core.BlockPos origin,
                           ChunkGeneratorStructureState generatorState,
                           List<SearchTarget> targets, ResourceLocation preferredId,
                           Completion completion) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.cursor = new CandidateCursor(origin, seed, generatorState, targets, preferredId);
            this.completion = completion;
        }
    }

    private static final class CandidateCursor {
        private final ChunkPos originChunk;
        private final long seed;
        private final ChunkGeneratorStructureState generatorState;
        private final List<SearchTarget> targets;
        private final ResourceLocation preferredId;
        private final Set<CandidateKey> seen = new HashSet<>();
        private final PriorityQueue<Candidate> pending;
        private int ring;

        private CandidateCursor(net.minecraft.core.BlockPos origin, long seed,
                                ChunkGeneratorStructureState generatorState,
                                List<SearchTarget> targets, ResourceLocation preferredId) {
            this.originChunk = new ChunkPos(origin);
            this.seed = seed;
            this.generatorState = generatorState;
            this.targets = List.copyOf(targets);
            this.preferredId = preferredId;
            this.pending = new PriorityQueue<>(Comparator
                    .comparing((Candidate candidate) -> !candidate.target().id().equals(preferredId))
                    .thenComparingDouble(Candidate::distanceSquared));
        }

    /**
     * 只有队首不远于所有未扫描区域的理论最近点时才出队。按"第一个非空环就出队"会让方形环的
     * 远角候选(可达 700 格)抢在下一环近边候选(可能只 16 格)之前。
     */
        @Nullable
        private Candidate next() {
            while (ring <= MAX_RING) {
                if (pending.isEmpty()) {
                    fillRing(ring++);
                    continue;
                }
                if (pending.peek().distanceSquared() <= minUnsearchedDistanceSquared()) {
                    return pending.remove();
                }
                fillRing(ring++);
            }
            return pending.isEmpty() ? null : pending.remove();
        }

        /** 未扫描区域的理论最近点:环外侧最近的一条区域边界。 */
        private double minUnsearchedDistanceSquared() {
            double minimum = Double.MAX_VALUE;
            for (SearchTarget target : targets) {
                int spacing = target.placement().spacing();
                int regionX = Math.floorDiv(originChunk.x, spacing);
                int regionZ = Math.floorDiv(originChunk.z, spacing);

                int leftChunk = (regionX - ring) * spacing + spacing - 1;
                int rightChunk = (regionX + ring) * spacing;
                int topChunk = (regionZ - ring) * spacing + spacing - 1;
                int bottomChunk = (regionZ + ring) * spacing;

                double dx = Math.min(Math.abs(chunkCenter(leftChunk) - originChunk.getMiddleBlockX()),
                        Math.abs(chunkCenter(rightChunk) - originChunk.getMiddleBlockX()));
                double dz = Math.min(Math.abs(chunkCenter(topChunk) - originChunk.getMiddleBlockZ()),
                        Math.abs(chunkCenter(bottomChunk) - originChunk.getMiddleBlockZ()));
                minimum = Math.min(minimum, Math.min(dx * dx, dz * dz));
            }
            return minimum;
        }

        private static double chunkCenter(int chunkCoordinate) {
            return chunkCoordinate * 16.0D + 8.0D;
        }

        private void fillRing(int radius) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (SearchTarget target : targets) {
                        RandomSpreadStructurePlacement placement = target.placement();
                        ChunkPos candidate = placement.getPotentialStructureChunk(seed,
                                originChunk.x + dx * placement.spacing(),
                                originChunk.z + dz * placement.spacing());
                        if (!placement.isStructureChunk(generatorState, candidate.x, candidate.z)
                                || !seen.add(new CandidateKey(target.id(), candidate.toLong()))) {
                            continue;
                        }
                        double candidateX = candidate.x * 16.0D + 8.0D;
                        double candidateZ = candidate.z * 16.0D + 8.0D;
                        double distanceX = candidateX - originChunk.getMiddleBlockX();
                        double distanceZ = candidateZ - originChunk.getMiddleBlockZ();
                        pending.add(new Candidate(candidate, target,
                                distanceX * distanceX + distanceZ * distanceZ));
                    }
                }
            }
        }
    }

    private record CandidateKey(ResourceLocation structureId, long chunkPos) {
    }
}
