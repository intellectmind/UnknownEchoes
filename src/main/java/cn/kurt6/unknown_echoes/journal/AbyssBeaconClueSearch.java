package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.beacon.AbyssBeaconBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

/** 档案终端的深渊信标补录任务。每次只推进一个候选起点，避免同步 /locate 卡住服务端。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class AbyssBeaconClueSearch {
    public enum RequestResult {
        ADDED,
        ALREADY_KNOWN,
        SEARCHING,
        UNAVAILABLE
    }

    /**
     * 搜索预算的硬上限。候选是"要生成的区块"而不是纯算术,缺上限时有两条失控路径:
     * 附近信标全已完成会一路向外生成区块;placement 全为 frequency 0.0 时主线程空转卡死。
     */
    private static final int MAX_RING = 128;
    private static final int MAX_CANDIDATES = 512;
    private static final int MAX_CONSECUTIVE_ERRORS = 16;

    private static final Map<UUID, SearchTask> ACTIVE = new HashMap<>();

    private AbyssBeaconClueSearch() {
    }

    public static RequestResult request(ServerPlayer player, ResourceLocation structureId) {
        if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) {
            return RequestResult.UNAVAILABLE;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            return RequestResult.SEARCHING;
        }

        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder.Reference<Structure> holder = registry.getHolder(
                ResourceKey.create(Registries.STRUCTURE, structureId)).orElse(null);
        if (holder == null) {
            return RequestResult.UNAVAILABLE;
        }
        ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
        List<RandomSpreadStructurePlacement> placements = generatorState.getPlacementsForStructure(holder).stream()
                .filter(RandomSpreadStructurePlacement.class::isInstance)
                .map(RandomSpreadStructurePlacement.class::cast)
                .toList();
        if (placements.isEmpty()) {
            return RequestResult.UNAVAILABLE;
        }

        SearchTask task = new SearchTask(player.getUUID(), level.dimension(), structureId,
                holder.value(), player.blockPosition(), level.getSeed(), generatorState, placements);
        ACTIVE.put(player.getUUID(), task);
        searchNext(player.server, task);
        return RequestResult.SEARCHING;
    }

    private static RequestResult record(ServerPlayer player, ServerLevel level,
                                        ResourceLocation structureId, BlockPos pos) {
        boolean added = JournalManager.addClue(player, new ExplorationClue(structureId.toString(),
                level.dimension().location().toString(), pos, false));
        if (added) {
            JournalManager.syncJournal(player);
            return RequestResult.ADDED;
        }
        return RequestResult.ALREADY_KNOWN;
    }

    private static void searchNext(MinecraftServer server, SearchTask task) {
        if (server.isStopped() || ACTIVE.get(task.playerId) != task) {
            return;
        }
        ServerLevel level = server.getLevel(task.dimension);
        if (level == null) {
            finishUnavailable(server, task);
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
            finishUnavailable(server, task);
            return;
        }
        ChunkPos candidate = task.cursor.next();
        if (candidate == null) {
            if (task.cursor.isExhausted()) finishUnavailable(server, task);
            else deferNext(level, task);
            return;
        }
        task.searchedCandidates++;
        task.inFlight = true;
        loadChunk(level, candidate, ChunkStatus.STRUCTURE_STARTS)
                .whenComplete((chunk, error) -> server.execute(() ->
                        handleCandidate(server, task, level, chunk, error)));
    }

    private static CompletableFuture<ChunkAccess> loadChunk(ServerLevel level, ChunkPos candidate,
                                                              ChunkStatus status) {
        try {
            // 在服务器主线程发起请求,避免后台线程 join 主线程 future。
            return level.getChunkSource().getChunkFuture(candidate.x, candidate.z, status, true)
                    .thenApply(chunk -> chunk.orElse(null));
        } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static void handleCandidate(MinecraftServer server, SearchTask task, ServerLevel level,
                                        ChunkAccess chunk, Throwable error) {
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
                UnknownEchoes.LOGGER.error("Abyss beacon clue search stopped after repeated chunk errors", error);
                finishUnavailable(server, task);
                return;
            }
            deferNext(level, task);
            return;
        }
        task.consecutiveErrors = 0;
        StructureStart start = chunk.getStartForStructure(task.target);
        if (start == null || !start.isValid()) {
            deferNext(level, task);
            return;
        }
        BlockPos target = start.getBoundingBox().getCenter();
        if (AbyssBeaconBlock.hasCompletedAtStructureCenter(player, level, target)) {
            deferNext(level, task);
            return;
        }

        RequestResult result = record(player, level, task.structureId, target);
        ACTIVE.remove(task.playerId, task);
        player.displayClientMessage(Component.translatable(
                result == RequestResult.ADDED
                        ? "message.unknown_echoes.completion.abyss_clue_added"
                        : "message.unknown_echoes.completion.abyss_clue_known"), true);
    }

    private static void deferNext(ServerLevel level, SearchTask task) {
        task.inFlight = false;
        task.nextStepAt = level.getGameTime() + 1L;
    }

    private static void finishUnavailable(MinecraftServer server, SearchTask task) {
        if (!ACTIVE.remove(task.playerId, task)) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (!server.isStopped() && player != null && player.isAlive()
                && player.level().dimension().equals(task.dimension)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.completion.abyss_clue_unavailable"), true);
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

    private static final class SearchTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final ResourceLocation structureId;
        private final Structure target;
        private final CandidateCursor cursor;
        private long nextStepAt;
        private boolean inFlight;
        private int searchedCandidates;
        private int consecutiveErrors;

        private SearchTask(UUID playerId, ResourceKey<Level> dimension,
                           ResourceLocation structureId, Structure target,
                           BlockPos origin, long seed,
                           ChunkGeneratorStructureState generatorState,
                           List<RandomSpreadStructurePlacement> placements) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.structureId = structureId;
            this.target = target;
            this.cursor = new CandidateCursor(origin, seed, generatorState, placements);
        }
    }

    private static final class CandidateCursor {
        private final BlockPos origin;
        private final ChunkPos originChunk;
        private final long seed;
        private final ChunkGeneratorStructureState generatorState;
        private final List<RandomSpreadStructurePlacement> placements;
        private final Set<Long> seen = new HashSet<>();
        private final PriorityQueue<Candidate> pending = new PriorityQueue<>(
                Comparator.comparingDouble(Candidate::distanceSquared));
        private final StructureSearchRings rings;

        private CandidateCursor(BlockPos origin, long seed,
                                ChunkGeneratorStructureState generatorState,
                                List<RandomSpreadStructurePlacement> placements) {
            this.origin = origin.immutable();
            this.originChunk = new ChunkPos(origin);
            this.seed = seed;
            this.generatorState = generatorState;
            this.placements = List.copyOf(placements);
            this.rings = new StructureSearchRings(placements.size());
        }

        /** null 可能表示单次检查预算耗尽；须配合 isExhausted 判断是否已搜完。 */
        @Nullable
        private ChunkPos next() {
            int remaining = StructureSearchRings.CHECKS_PER_STEP;
            while (rings.ring() <= MAX_RING) {
                if (rings.atRingStart() && !pending.isEmpty()
                        && pending.peek().distanceSquared() <= minUnsearchedDistanceSquared()) {
                    return pending.remove().pos();
                }
                if (remaining-- == 0) return null;
                scanPlacement();
            }
            return pending.isEmpty() ? null : pending.remove().pos();
        }

        private boolean isExhausted() {
            return rings.ring() > MAX_RING && pending.isEmpty();
        }

        private void scanPlacement() {
            RandomSpreadStructurePlacement placement = placements.get(rings.targetIndex());
            ChunkPos candidate = placement.getPotentialStructureChunk(seed,
                    originChunk.x + rings.offsetX() * placement.spacing(),
                    originChunk.z + rings.offsetZ() * placement.spacing());
            rings.advance();
            if (!placement.isStructureChunk(generatorState, candidate.x, candidate.z)
                    || !seen.add(candidate.toLong())) {
                return;
            }
            pending.add(new Candidate(candidate, distanceSquared(candidate)));
        }

        /** 下一候选只有不远于所有未扫描区域的理论最近点时才可出队，避免方形环边角候选抢在下一环的近边候选之前。 */
        private double minUnsearchedDistanceSquared() {
            int ring = rings.ring();
            double minimum = Double.MAX_VALUE;
            for (RandomSpreadStructurePlacement placement : placements) {
                int spacing = placement.spacing();
                int regionX = Math.floorDiv(originChunk.x, spacing);
                int regionZ = Math.floorDiv(originChunk.z, spacing);

                int leftChunk = (regionX - ring) * spacing + spacing - 1;
                int rightChunk = (regionX + ring) * spacing;
                int topChunk = (regionZ - ring) * spacing + spacing - 1;
                int bottomChunk = (regionZ + ring) * spacing;

                double dx = Math.min(Math.abs(chunkCenter(leftChunk) - origin.getX()),
                        Math.abs(chunkCenter(rightChunk) - origin.getX()));
                double dz = Math.min(Math.abs(chunkCenter(topChunk) - origin.getZ()),
                        Math.abs(chunkCenter(bottomChunk) - origin.getZ()));
                minimum = Math.min(minimum, Math.min(dx * dx, dz * dz));
            }
            return minimum;
        }

        private double distanceSquared(ChunkPos candidate) {
            double x = chunkCenter(candidate.x) - origin.getX();
            double z = chunkCenter(candidate.z) - origin.getZ();
            return x * x + z * z;
        }

        private static double chunkCenter(int chunkCoordinate) {
            return chunkCoordinate * 16.0D + 8.0D;
        }

        private record Candidate(ChunkPos pos, double distanceSquared) {
        }
    }
}
