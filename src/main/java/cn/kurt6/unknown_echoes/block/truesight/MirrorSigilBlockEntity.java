package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 镜像符印 BlockEntity:真假标记的唯一存放处(V0.6A 红线 #9 技术债务迁移)。
 * 此前 REAL 放在方块状态里——方块状态会全量同步客户端,抓包/反编译可直接读出真假。
 * 迁移后真假只存服务端 BlockEntity(本类不实现 getUpdateTag/getUpdatePacket,
 * 永不向客户端同步任何数据),客户端只能靠观察"倒影涟漪"等服务端驱动的表现辨认。
 *
 * 旧档兼容:旧版方块没有 BlockEntity,REAL 属性被丢弃 → 首次被访问时按
 * "全部假符印重排一次"处理:{@link #ensureClusterInitialized} 收集整座神殿的符印,
 * 用位置派生的确定性随机重新抽 4 枚真符印并标记初始化。
 */
public class MirrorSigilBlockEntity extends BlockEntity {

    /** 神殿符印簇的搜索半径(与触假重置范围一致)。必须覆盖整座神殿(23×23):
     *  从任意一枚符印出发都要能扫到对角符印,否则触假重置/旧档重排会漏掉远端(V0.6B 修复,原 14 不够)。 */
    public static final int CLUSTER_RANGE_H = 22;
    public static final int CLUSTER_RANGE_V = 8;
    /** 每簇真符印数量(镜湖神殿:V0.6B 起十二枚中 4 真;旧档十枚重排同样取 4)。 */
    private static final int REAL_PER_CLUSTER = 4;

    private boolean real = false;
    private boolean initialized = false;

    public MirrorSigilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIRROR_SIGIL.get(), pos, state);
    }

    /** 结构生成时直接设定真假(已初始化,不参与旧档重排)。 */
    public void setReal(boolean real) {
        this.real = real;
        this.initialized = true;
        this.setChanged();
    }

    /**
     * 服务端读取真假的唯一入口:未初始化(旧档)时先对整簇做一次确定性重排。
     */
    public static boolean isReal(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MirrorSigilBlockEntity sigil)) {
            return false;
        }
        ensureInitialized(level, pos, sigil);
        return sigil.initialized && sigil.real;
    }

    /** 旧档簇未完整加载时暂不判定,避免把真符印误当成假符印。 */
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

    /** 旧档迁移:收集本簇全部符印,按簇最小角派生的确定性随机重抽真符印。 */
    private static void ensureClusterInitialized(ServerLevel level, BlockPos center) {
        List<BlockPos> cluster = findClusterForMigration(level, center);
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

    private static List<BlockPos> findClusterForMigration(ServerLevel level, BlockPos center) {
        if (!isClusterRangeLoaded(level, center)) {
            return List.of();
        }
        List<BlockPos> cluster = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CLUSTER_RANGE_H, -CLUSTER_RANGE_V, -CLUSTER_RANGE_H),
                center.offset(CLUSTER_RANGE_H, CLUSTER_RANGE_V, CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof MirrorSigilBlock) {
                cluster.add(pos.immutable());
            }
        }
        cluster.sort(Comparator.comparingLong(BlockPos::asLong));
        return cluster;
    }

    /** 只枚举已加载区块;范围未完整加载时返回空,避免漏算符印或触发区块加载。 */
    public static List<BlockPos> findCluster(ServerLevel level, BlockPos center) {
        if (!isClusterRangeLoaded(level, center)) {
            return List.of();
        }
        int minChunkX = (center.getX() - CLUSTER_RANGE_H) >> 4;
        int maxChunkX = (center.getX() + CLUSTER_RANGE_H) >> 4;
        int minChunkZ = (center.getZ() - CLUSTER_RANGE_H) >> 4;
        int maxChunkZ = (center.getZ() + CLUSTER_RANGE_H) >> 4;
        List<BlockPos> cluster = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!(blockEntity instanceof MirrorSigilBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    if (Math.abs((long) pos.getX() - center.getX()) <= CLUSTER_RANGE_H
                            && Math.abs((long) pos.getY() - center.getY()) <= CLUSTER_RANGE_V
                            && Math.abs((long) pos.getZ() - center.getZ()) <= CLUSTER_RANGE_H) {
                        cluster.add(pos.immutable());
                    }
                }
            }
        }
        cluster.sort(Comparator.comparingLong(BlockPos::asLong));
        return cluster;
    }

    private static boolean isClusterRangeLoaded(ServerLevel level, BlockPos center) {
        int minChunkX = (center.getX() - CLUSTER_RANGE_H) >> 4;
        int maxChunkX = (center.getX() + CLUSTER_RANGE_H) >> 4;
        int minChunkZ = (center.getZ() - CLUSTER_RANGE_H) >> 4;
        int maxChunkZ = (center.getZ() + CLUSTER_RANGE_H) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
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

    // 刻意不重写 getUpdateTag / getUpdatePacket:真假信息永不下发客户端(红线 #9)
}
