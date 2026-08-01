package cn.kurt6.unknown_echoes.journal;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 线索定位器(V0.6D):在"读取线索"的那一刻查询附近已加载的结构起点,
 * 把结果写入玩家个人日志线索;此后罗盘只读线索,不再扫描(12.2:不做全图结构扫描)。
 * 绝不为交互生成新区块,避免结构定位把噪声地形计算放到服务器主线程。
 */
public class ClueLocator {

    /** 已加载区块中的定位半径(区块);查询过程绝不请求区块加载。 */
    private static final int LOADED_SEARCH_RADIUS_CHUNKS = 48;

    /** 观测站检索可命中的回访遗迹池(回声境域内,值得被"检索残响"提到的结构)。 */
    private static final List<String> REALM_CLUE_POOL = List.of(
            "memory_pillar_courtyard", "sunken_temple", "mirror_temple", "echo_grand_archive",
            "echo_temple", "rune_puzzle_room",
            "silent_ring", "broken_archive", "tide_lighthouse_reef", "mirror_dust_cloister",
            "silent_altar", "wind_eroded_tower", "reflection_vault");

    /**
     * 定位指定结构并写入未踏入线索。返回 true 表示新线索落账(或附近已有同结构线索)。
     * 找不到附近已加载的同类结构返回 false,调用方给含蓄提示。
     */
    public static boolean locateAndAddClue(ServerPlayer player, ResourceLocation structureId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder =
                registry.getHolder(ResourceKey.create(Registries.STRUCTURE, structureId));
        if (holder.isEmpty()) {
            return false;
        }
        BlockPos pos = findNearestLoadedStructure(level, holder.get().value(), player.blockPosition());
        if (pos == null) {
            return false;
        }
        // 已经踏入过的位置不再作为"未踏入"线索写入(addClue 内部会合并)
        JournalManager.addClue(player, new ExplorationClue(structureId.toString(),
                level.dimension().location().toString(), pos, false));
        JournalManager.syncJournal(player);
        return true;
    }

    /**
     * 只读取已完整加载区块中的结构起点。不要改用 StructureManager 的便利查询:
     * 它会为结构引用回退请求 STRUCTURE_STARTS 状态,从而重新进入区块生成路径。
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

                BlockPos candidate = start.getBoundingBox().getCenter();
                double distance = origin.distSqr(candidate);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    /**
     * 从已加载区块中查找指定结构标签的最近有效起点。
     * 供实体等高频服务端逻辑使用，严禁以生成式结构定位替代。
     */
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
                    BlockPos candidate = entry.getValue().getBoundingBox().getCenter();
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
        List<String> candidates = new ArrayList<>(REALM_CLUE_POOL);
        // 排除已有未踏入线索的结构,避免检索重复
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (!clue.visited()) {
                ResourceLocation id = ResourceLocation.tryParse(clue.structureId());
                if (id != null) {
                    candidates.remove(id.getPath());
                }
            }
        }
        while (!candidates.isEmpty()) {
            String pick = candidates.remove(random.nextInt(candidates.size()));
            if (locateAndAddClue(player, cn.kurt6.unknown_echoes.UnknownEchoes.id(pick))) {
                return true;
            }
        }
        return false;
    }
}
