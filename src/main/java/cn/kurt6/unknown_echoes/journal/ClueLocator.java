package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 线索定位器；读取线索时查询附近已加载的结构起点并写入玩家日志，罗盘之后只读取已记录线索。 */
public class ClueLocator {

    public enum LocateResult {
        ADDED,
        ALREADY_KNOWN,
        NOT_FOUND;

        public boolean located() {
            return this != NOT_FOUND;
        }
    }

    public record LocateOutcome(LocateResult result, ResourceLocation structureId) {
    }

    /** 已加载区块中的定位半径(区块);查询过程绝不请求区块加载。 */
    private static final int LOADED_SEARCH_RADIUS_CHUNKS = 48;

    /** 观测站检索可命中的回访遗迹池(回声境域内,值得被"检索残响"提到的结构)。 */
    private static final List<ResourceLocation> REALM_CLUE_POOL = List.of(
            UnknownEchoes.id("memory_pillar_courtyard"), UnknownEchoes.id("sunken_temple"),
            UnknownEchoes.id("mirror_temple"), UnknownEchoes.id("echo_grand_archive"),
            UnknownEchoes.id("echo_temple"), UnknownEchoes.id("rune_puzzle_room"),
            UnknownEchoes.id("silent_ring"), UnknownEchoes.id("broken_archive"),
            UnknownEchoes.id("tide_lighthouse_reef"), UnknownEchoes.id("mirror_dust_cloister"),
            UnknownEchoes.id("silent_altar"), UnknownEchoes.id("wind_eroded_tower"),
            UnknownEchoes.id("reflection_vault"));

    public static LocateResult locateAndAddClueResult(ServerPlayer player, ResourceLocation structureId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return LocateResult.NOT_FOUND;
        }
        String dimension = level.dimension().location().toString();
        boolean known = JournalManager.getClues(player).stream()
                .anyMatch(clue -> clue.dimension().equals(dimension)
                        && clue.structureId().equals(structureId.toString()));
        if (known) {
            return LocateResult.ALREADY_KNOWN;
        }
        BlockPos pos = findNearestLoadedStructure(level, structureId, player.blockPosition());
        if (pos == null) {
            return LocateResult.NOT_FOUND;
        }

        boolean added = JournalManager.addClue(player, new ExplorationClue(structureId.toString(),
                dimension, pos, false));
        if (added) {
            JournalManager.syncJournal(player);
            return LocateResult.ADDED;
        }
        return LocateResult.ALREADY_KNOWN;
    }

    /** 兼容只关心"附近是否有目标"的旧调用方(主线提示仍沿用原有布尔语义)。 */
    public static boolean locateAndAddClue(ServerPlayer player, ResourceLocation structureId) {
        return locateAndAddClueResult(player, structureId).located();
    }

    /** 在一次已加载区块扫描中,从候选池优先挑一处玩家尚未记录的结构。 */
    public static LocateOutcome locateAndAddAnyClue(ServerPlayer player,
                                                      List<ResourceLocation> structureIds,
                                                      RandomSource random) {
        if (!(player.level() instanceof ServerLevel level) || structureIds.isEmpty()) {
            return new LocateOutcome(LocateResult.NOT_FOUND, null);
        }
        String dimension = level.dimension().location().toString();
        Set<String> knownIds = new HashSet<>();
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (clue.dimension().equals(dimension)) {
                knownIds.add(clue.structureId());
            }
        }
        List<ResourceLocation> unknownIds = structureIds.stream()
                .filter(id -> !knownIds.contains(id.toString()))
                .distinct()
                .toList();
        if (unknownIds.isEmpty()) {
            return new LocateOutcome(LocateResult.ALREADY_KNOWN, null);
        }
        ResourceLocation preferred = unknownIds.get(random.nextInt(unknownIds.size()));
        LoadedStructureTarget target = findNearestLoadedStructure(level, unknownIds, preferred,
                player.blockPosition());
        if (target == null) {
            return new LocateOutcome(LocateResult.NOT_FOUND, null);
        }
        boolean added = JournalManager.addClue(player, new ExplorationClue(target.id().toString(),
                dimension, target.pos(), false));
        if (added) {
            JournalManager.syncJournal(player);
            return new LocateOutcome(LocateResult.ADDED, target.id());
        }
        return new LocateOutcome(LocateResult.ALREADY_KNOWN, target.id());
    }

    public static BlockPos findNearestLoadedStructure(ServerLevel level,
                                                       ResourceLocation structureId,
                                                       BlockPos origin) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder =
                registry.getHolder(ResourceKey.create(Registries.STRUCTURE, structureId));
        return holder.map(reference -> findNearestLoadedStructure(
                level, reference.value(), origin)).orElse(null);
    }

    /**
     * 一次已加载区块遍历同时查询多个结构。优先返回 preferredId 的最近实例；
     * 首选结构不存在时返回候选池中离 origin 最近的实例。
     */
    public static LoadedStructureTarget findNearestLoadedStructure(
            ServerLevel level, List<ResourceLocation> structureIds,
            ResourceLocation preferredId, BlockPos origin) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<Structure, ResourceLocation> targetIds = new HashMap<>();
        for (ResourceLocation id : structureIds) {
            registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id))
                    .ifPresent(holder -> targetIds.put(holder.value(), id));
        }
        if (targetIds.isEmpty()) {
            return null;
        }

        ChunkPos originChunk = new ChunkPos(origin);
        LoadedStructureTarget preferred = null;
        LoadedStructureTarget nearest = null;
        double preferredDistance = Double.MAX_VALUE;
        double nearestDistance = Double.MAX_VALUE;
        for (int chunkZ = originChunk.z - LOADED_SEARCH_RADIUS_CHUNKS;
             chunkZ <= originChunk.z + LOADED_SEARCH_RADIUS_CHUNKS; chunkZ++) {
            for (int chunkX = originChunk.x - LOADED_SEARCH_RADIUS_CHUNKS;
                 chunkX <= originChunk.x + LOADED_SEARCH_RADIUS_CHUNKS; chunkX++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    ResourceLocation id = targetIds.get(entry.getKey());
                    StructureStart start = entry.getValue();
                    if (id == null || start == null || !start.isValid()) {
                        continue;
                    }
                    BlockPos candidate = LoadedStructureLookup.currentBounds(start).getCenter();
                    double distance = origin.distSqr(candidate);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = new LoadedStructureTarget(id, candidate);
                    }
                    if (id.equals(preferredId) && distance < preferredDistance) {
                        preferredDistance = distance;
                        preferred = new LoadedStructureTarget(id, candidate);
                    }
                }
            }
        }
        return preferred != null ? preferred : nearest;
    }

    public record LoadedStructureTarget(ResourceLocation id, BlockPos pos) {
    }

    /**
     * 只读取已完整加载区块中的结构起点。
     * 不要改用 StructureManager 的便利查询: 它会为结构引用回退请求 STRUCTURE_STARTS 状态,
     * 从而重新进入区块生成路径。
     */
    private static BlockPos findNearestLoadedStructure(ServerLevel level, Structure structure, BlockPos origin) {
        ChunkPos originChunk = new ChunkPos(origin);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int chunkZ = originChunk.z - LOADED_SEARCH_RADIUS_CHUNKS;
             chunkZ <= originChunk.z + LOADED_SEARCH_RADIUS_CHUNKS; chunkZ++) {
            for (int chunkX = originChunk.x - LOADED_SEARCH_RADIUS_CHUNKS;
                 chunkX <= originChunk.x + LOADED_SEARCH_RADIUS_CHUNKS; chunkX++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                StructureStart start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) {
                    continue;
                }

                BlockPos candidate = LoadedStructureLookup.currentBounds(start).getCenter();
                double distance = origin.distSqr(candidate);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    public static BlockPos findNearestLoadedStructureInTag(ServerLevel level, TagKey<Structure> tag,
                                                           BlockPos origin, int searchRadiusChunks) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> targets = registry.getTag(tag);
        if (targets.isEmpty()) {
            return null;
        }

        int radius = Math.max(0, searchRadiusChunks);
        ChunkPos originChunk = new ChunkPos(origin);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int chunkZ = originChunk.z - radius; chunkZ <= originChunk.z + radius; chunkZ++) {
            for (int chunkX = originChunk.x - radius; chunkX <= originChunk.x + radius; chunkX++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    if (!targets.get().contains(registry.wrapAsHolder(entry.getKey()))
                            || !entry.getValue().isValid()) {
                        continue;
                    }
                    BlockPos candidate = LoadedStructureLookup.currentBounds(entry.getValue()).getCenter();
                    double distance = origin.distSqr(candidate);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    /** 观测站"检索残响":从回访池里随机挑一个玩家尚无未踏入线索的结构定位。 */
    public static boolean addObservatoryClue(ServerPlayer player, RandomSource random) {
        return locateAndAddAnyClue(player, REALM_CLUE_POOL, random).result() == LocateResult.ADDED;
    }
}
