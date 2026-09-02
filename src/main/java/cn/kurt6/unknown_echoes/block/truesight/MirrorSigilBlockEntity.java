package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 镜像符印 BlockEntity:真假标记的唯一存放处。 */
public class MirrorSigilBlockEntity extends BlockEntity {

    /** 无结构引用时的兼容扫描半径；正式神殿优先使用结构包围盒。 */
    public static final int CLUSTER_RANGE_H = 48;
    public static final int CLUSTER_RANGE_V = 8;
    private static final int REAL_PER_CLUSTER = 4;

    private boolean real = false;
    private boolean initialized = false;

    public MirrorSigilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIRROR_SIGIL.get(), pos, state);
    }

    public void setReal(boolean real) {
        this.real = real;
        this.initialized = true;
        this.setChanged();
    }

    /** 服务端读取真假的唯一入口:未初始化时先对整簇做一次确定性重排。 */
    public static boolean isReal(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MirrorSigilBlockEntity sigil)) {
            return false;
        }
        ensureInitialized(level, pos, sigil);
        return sigil.initialized && sigil.real;
    }

    /** 符印簇未完整加载时暂不判定，避免误判真符印。 */
    public static boolean ensureInitialized(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MirrorSigilBlockEntity sigil)) {
            return false;
        }
        return ensureInitialized(level, pos, sigil);
    }

    private static boolean ensureInitialized(ServerLevel level, BlockPos pos,
                                             MirrorSigilBlockEntity sigil) {
        if (!sigil.initialized) {
            ensureClusterInitialized(level, pos);
        }
        return sigil.initialized;
    }

    private static void ensureClusterInitialized(ServerLevel level, BlockPos center) {
        List<BlockPos> cluster = findCluster(level, center);
        if (cluster.isEmpty()) {
            return;
        }
        BlockPos min = cluster.getFirst();
        RandomSource random = RandomSource.create(min.getX() * 341873128712L ^ min.getZ() * 132897987541L);
        List<BlockPos> shuffled = new ArrayList<>(cluster);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        int realCount = shuffled.size() >= 10
                ? REAL_PER_CLUSTER
                : Math.max(1, shuffled.size() / 3);
        for (int i = 0; i < shuffled.size(); i++) {
            if (level.getBlockEntity(shuffled.get(i)) instanceof MirrorSigilBlockEntity sigil
                    && !sigil.initialized) {
                sigil.setReal(i < realCount);
            }
        }
    }

    /** 返回所属镜面神殿内的全部符印；没有结构引用时使用旧档兼容范围。 */
    public static List<BlockPos> findCluster(ServerLevel level, BlockPos center) {
        BoundingBox bounds = findClusterBounds(level, center);
        if (!isBoundsLoaded(level, bounds)) {
            return List.of();
        }
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;
        List<BlockPos> cluster = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!(blockEntity instanceof MirrorSigilBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    if (bounds.isInside(pos)) {
                        cluster.add(pos.immutable());
                    }
                }
            }
        }
        cluster.sort(Comparator.comparingLong(BlockPos::asLong));
        return cluster;
    }

    public static boolean isClusterLoaded(ServerLevel level, BlockPos center) {
        return isBoundsLoaded(level, findClusterBounds(level, center));
    }

    private static BoundingBox findClusterBounds(ServerLevel level, BlockPos center) {
        BoundingBox structureBounds = LoadedStructureLookup.loadedStructureBoundsAt(level, center,
                structure -> structure.type() == ModStructures.MIRROR_TEMPLE.get());
        if (structureBounds != null) {
            return structureBounds;
        }
        return new BoundingBox(
                center.getX() - CLUSTER_RANGE_H, center.getY() - CLUSTER_RANGE_V,
                center.getZ() - CLUSTER_RANGE_H,
                center.getX() + CLUSTER_RANGE_H, center.getY() + CLUSTER_RANGE_V,
                center.getZ() + CLUSTER_RANGE_H);
    }

    private static boolean isBoundsLoaded(ServerLevel level, BoundingBox bounds) {
        return LoadedAreaCheck.boundsLoaded(level, bounds);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Real", this.real);
        tag.putBoolean("Initialized", this.initialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.real = tag.getBoolean("Real");
        this.initialized = tag.getBoolean("Initialized");
    }

}
