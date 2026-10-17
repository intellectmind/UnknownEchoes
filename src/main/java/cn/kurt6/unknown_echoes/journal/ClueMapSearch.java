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
        void accept(ServerPlayer player, @Nullable ExplorationClue clue);
    }

    public static boolean isSearching(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static RequestResult request(ServerPlayer player, List<ResourceLocation> structureIds,
                                        ResourceLocation preferredId, Completion completion) {
        if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) {
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
            finish(server, task, null);
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.isAlive() || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId, task);
            return;
        }
        if (task.inFlight || level.getGameTime() < task.nextStepAt) {
            return;
        }
        if (task.searchedCandidates >= MAX_CANDIDATES) {
            finish(server, task, null);
            return;
        }

        Candidate candidate = task.cursor.next();
        if (candidate == null) {
            if (task.cursor.isExhausted()) finish(server, task, null);
            else deferNext(level, task);
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
        if (server.isStopped() || ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.isAlive() || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId, task);
            return;
        }
        if (error != null || chunk == null) {
            task.consecutiveErrors++;
            if (task.consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                finish(server, task, null);
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
        if (server.isStopped() || ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player == null || !player.isAlive() || !player.level().dimension().equals(task.dimension)) {
            ACTIVE.remove(task.playerId, task);
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
        finish(server, task, new ExplorationClue(
                candidate.target().id().toString(), level.dimension().location().toString(), position, false));
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

    private static void finish(MinecraftServer server, SearchTask task, @Nullable ExplorationClue clue) {
        if (!ACTIVE.remove(task.playerId, task)) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (!server.isStopped() && player != null && player.isAlive()
                && player.level().dimension().equals(task.dimension)) {
            task.completion.accept(player, clue);
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
        if (ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (ACTIVE.size() == 1) {
            searchNext(server, ACTIVE.values().iterator().next());
            return;
        }
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
        private final StructureSearchRings rings;

        private CandidateCursor(net.minecraft.core.BlockPos origin, long seed,
                                ChunkGeneratorStructureState generatorState,
                                List<SearchTarget> targets, ResourceLocation preferredId) {
            this.originChunk = new ChunkPos(origin);
            this.seed = seed;
            this.generatorState = generatorState;
            this.targets = List.copyOf(targets);
            this.rings = new StructureSearchRings(targets.size());
            this.preferredId = preferredId;
            this.pending = new PriorityQueue<>(Comparator
                    .comparing((Candidate candidate) -> !candidate.target().id().equals(preferredId))
                    .thenComparingDouble(Candidate::distanceSquared));
        }

        /**
         * 完整扫描当前环后才按未扫描区域的距离下界出队；null 须配合 isExhausted 区分暂停与结束。
         */
        @Nullable
        private Candidate next() {
            int remaining = StructureSearchRings.CHECKS_PER_STEP;
            while (rings.ring() <= MAX_RING) {
                if (rings.atRingStart() && !pending.isEmpty()
                        && pending.peek().distanceSquared() <= minUnsearchedDistanceSquared()) {
                    return pending.remove();
                }
                if (remaining-- == 0) return null;
                scanPlacement();
            }
            return pending.isEmpty() ? null : pending.remove();
        }

        private boolean isExhausted() {
            return rings.ring() > MAX_RING && pending.isEmpty();
        }

        /** 未扫描区域的理论最近点:环外侧最近的一条区域边界。 */
        private double minUnsearchedDistanceSquared() {
            int ring = rings.ring();
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

        private void scanPlacement() {
            SearchTarget target = targets.get(rings.targetIndex());
            RandomSpreadStructurePlacement placement = target.placement();
            ChunkPos candidate = placement.getPotentialStructureChunk(seed,
                    originChunk.x + rings.offsetX() * placement.spacing(),
                    originChunk.z + rings.offsetZ() * placement.spacing());
            rings.advance();
            if (!placement.isStructureChunk(generatorState, candidate.x, candidate.z)
                    || !seen.add(new CandidateKey(target.id(), candidate.toLong()))) {
                return;
            }
            double distanceX = chunkCenter(candidate.x) - originChunk.getMiddleBlockX();
            double distanceZ = chunkCenter(candidate.z) - originChunk.getMiddleBlockZ();
            pending.add(new Candidate(candidate, target,
                    distanceX * distanceX + distanceZ * distanceZ));
        }
    }

    private record CandidateKey(ResourceLocation structureId, long chunkPos) {
    }
}
