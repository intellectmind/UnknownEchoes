package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** 只解析已完整加载区块中的结构引用，供主线程事件和周期逻辑使用。 */
public final class LoadedStructureLookup {

    private LoadedStructureLookup() {
    }

    public record Site(Structure structure, BoundingBox bounds) { }

    // 同一起点的本地区块和引用只记一次，返回实际几何快照而非玩家脚下坐标。
    public static List<Site> sitesAt(ServerLevel level, BlockPos pos, Predicate<Structure> filter) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return List.of();
        Set<StructureStart> seen = new HashSet<>();
        List<Site> sites = new ArrayList<>();
        for (var entry : chunk.getAllStarts().entrySet()) {
            if (filter.test(entry.getKey())) {
                addSite(sites, seen, entry.getKey(), entry.getValue(), pos);
            }
        }
        for (var entry : chunk.getAllReferences().entrySet()) {
            if (!filter.test(entry.getKey())) continue;
            for (long reference : entry.getValue()) {
                addSite(sites, seen, entry.getKey(), getLoadedStart(level, entry.getKey(), reference), pos);
            }
        }
        return List.copyOf(sites);
    }

    private static void addSite(List<Site> sites, Set<StructureStart> seen, Structure structure,
                                StructureStart start, BlockPos pos) {
        if (start != null && seen.add(start) && containsPiece(start, pos)) {
            sites.add(new Site(structure, currentBounds(start)));
        }
    }

    /** 返回包围盒实际包含目标位置的结构类型，不因查询加载新的起点区块。 */
    public static Set<Structure> structuresAt(ServerLevel level, BlockPos pos,
                                              Predicate<Structure> filter) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return Set.of();
        }

        var references = chunk.getAllReferences();
        if (references.isEmpty() && chunk.getAllStarts().isEmpty()) {
            return Set.of();
        }
        Set<Structure> structures = new HashSet<>();
        for (var entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (filter.test(entry.getKey()) && start != null && containsPiece(start, pos)) {
                structures.add(entry.getKey());
            }
        }
        for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                : references.entrySet()) {
            Structure structure = entry.getKey();
            if (structures.contains(structure) || !filter.test(structure)) {
                continue;
            }
            for (long reference : entry.getValue()) {
                StructureStart start = getLoadedStart(level, structure, reference);
                if (start != null && containsPiece(start, pos)) {
                    structures.add(structure);
                    break;
                }
            }
        }
        return structures;
    }

    /** 返回包围盒包含目标位置的已加载结构起点；不会为解析引用而加载起点区块。 */
    public static StructureStart loadedStructureStartAt(ServerLevel level, BlockPos pos,
                                                         Predicate<Structure> filter) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return null;
        }
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (filter.test(entry.getKey()) && start != null && containsPiece(start, pos)) {
                return start;
            }
        }
        var references = chunk.getAllReferences();
        if (references.isEmpty()) {
            return null;
        }
        for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                : references.entrySet()) {
            if (!filter.test(entry.getKey())) {
                continue;
            }
            for (long reference : entry.getValue()) {
                StructureStart start = getLoadedStart(level, entry.getKey(), reference);
                if (start != null && containsPiece(start, pos)) {
                    return start;
                }
            }
        }
        return null;
    }

    /** 返回结构实际 Piece 的合并包围盒,避免使用生成前可能已缓存的 StructureStart 包围盒。 */
    public static BoundingBox loadedStructureBoundsAt(ServerLevel level, BlockPos pos,
                                                       Predicate<Structure> filter) {
        StructureStart start = loadedStructureStartAt(level, pos, filter);
        if (start == null) {
            return null;
        }
        return currentBounds(start);
    }

    /**
     * 保守判断目标位置是否可能位于结构内。
     * 任何尚未加载、因而无法解析的引用都按命中处理，适合禁止在结构内放置内容的选点逻辑。
     */
    public static boolean mayContainStructureAt(ServerLevel level, BlockPos pos,
                                                Predicate<Structure> filter) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return true;
        }

        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            if (filter.test(entry.getKey()) && containsCurrentBounds(entry.getValue(), pos)) {
                return true;
            }
        }

        var references = chunk.getAllReferences();
        if (references.isEmpty()) {
            return false;
        }
        for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                : references.entrySet()) {
            Structure structure = entry.getKey();
            if (!filter.test(structure)) {
                continue;
            }
            for (long reference : entry.getValue()) {
                ChunkPos startPos = new ChunkPos(reference);
                LevelChunk startChunk = level.getChunkSource().getChunkNow(startPos.x, startPos.z);
                if (startChunk == null) {
                    return true;
                }
                StructureStart start = startChunk.getStartForStructure(structure);
                if (containsCurrentBounds(start, pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsCurrentBounds(StructureStart start, BlockPos pos) {
        if (start == null || !start.isValid()) {
            return false;
        }
        BoundingBox bounds = currentBounds(start);
        return bounds != null && bounds.isInside(pos);
    }

    public static BoundingBox currentBounds(StructureStart start) {
        BoundingBox bounds = null;
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox next = piece.getBoundingBox();
            if (bounds == null) {
                bounds = new BoundingBox(next.minX(), next.minY(), next.minZ(),
                        next.maxX(), next.maxY(), next.maxZ());
            } else {
                bounds.encapsulate(next);
            }
        }
        return bounds;
    }

    /** 等价于 StructureManager#getStructureWithPieceAt 的只读已加载区块版本。 */
    public static boolean hasStructurePieceAt(ServerLevel level, BlockPos pos,
                                               TagKey<Structure> tag) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return loadedStructureStartAt(level, pos, structure -> registry.wrapAsHolder(structure).is(tag)) != null;
    }

    private static StructureStart getLoadedStart(ServerLevel level, Structure structure, long reference) {
        ChunkPos startPos = new ChunkPos(reference);
        LevelChunk startChunk = level.getChunkSource().getChunkNow(startPos.x, startPos.z);
        if (startChunk == null) {
            return null;
        }
        StructureStart start = startChunk.getStartForStructure(structure);
        return start != null && start.isValid() ? start : null;
    }

    /** 结构 Piece 可能在生成阶段下沉或上移,不能只依赖起点缓存的原始包围盒。 */
    private static boolean containsPiece(StructureStart start, BlockPos pos) {
        return start.isValid() && start.getPieces().stream()
                .anyMatch(piece -> piece.getBoundingBox().isInside(pos));
    }
}
