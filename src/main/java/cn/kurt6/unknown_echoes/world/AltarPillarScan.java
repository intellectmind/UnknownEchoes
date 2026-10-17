package cn.kurt6.unknown_echoes.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** 祭坛机关体积查询；未完成的范围不能作为旧存档“无机关”回退的依据。 */
public final class AltarPillarScan {
    private AltarPillarScan() {
    }

    /** 空 Optional 表示范围不可读；空列表才表示已确认没有机关。仅在本次同步调用内保留区块。 */
    public static Optional<List<BlockPos>> find(Level level, BlockPos center, int horizontal, int vertical,
                                                Predicate<BlockState> matches, boolean firstOnly) {
        if (!(level instanceof ServerLevel server)) return Optional.empty();
        int minX = center.getX() - horizontal, maxX = center.getX() + horizontal;
        int minZ = center.getZ() - horizontal, maxZ = center.getZ() + horizontal;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - vertical);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + vertical);
        if (minY > maxY) return Optional.of(List.of());
        int minChunkX = minX >> 4, maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4, maxChunkZ = maxZ >> 4;
        LevelChunk[][] chunks = new LevelChunk[maxChunkX - minChunkX + 1][maxChunkZ - minChunkZ + 1];
        // 先确认整片场地可读；首个命中也不能掩盖另一端尚未就绪的机关。
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) return Optional.empty();
                chunks[chunkX - minChunkX][chunkZ - minChunkZ] = chunk;
            }
        }
        List<BlockPos> result = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    LevelChunk chunk = chunks[chunkX - minChunkX][chunkZ - minChunkZ];
                    var section = chunk.getSection(chunk.getSectionIndex(sectionY << 4));
                    if (!section.maybeHas(matches)) continue;
                    for (int z = Math.max(minZ, chunkZ << 4); z <= Math.min(maxZ, (chunkZ << 4) + 15); z++) {
                        for (int y = Math.max(minY, sectionY << 4); y <= Math.min(maxY, (sectionY << 4) + 15); y++) {
                            for (int x = Math.max(minX, chunkX << 4); x <= Math.min(maxX, (chunkX << 4) + 15); x++) {
                                if (!matches.test(section.getBlockState(x & 15, y & 15, z & 15))) continue;
                                result.add(cursor.set(x, y, z).immutable());
                                if (firstOnly) return Optional.of(result);
                            }
                        }
                    }
                }
            }
        }
        // 分区段过滤后恢复 betweenClosed 的 z/y/x 顺序，保持个人记录的检查顺序。
        result.sort(Comparator.<BlockPos>comparingInt(BlockPos::getZ)
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX));
        return Optional.of(result);
    }
}
