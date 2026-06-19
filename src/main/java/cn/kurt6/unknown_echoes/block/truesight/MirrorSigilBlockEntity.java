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
        if (!sigil.initialized) {
            ensureClusterInitialized(level, pos);
        }
        return sigil.real;
    }

    /** 旧档迁移:收集本簇全部符印,按簇最小角派生的确定性随机重抽真符印。 */
    private static void ensureClusterInitialized(ServerLevel level, BlockPos center) {
        List<BlockPos> cluster = new ArrayList<>();
        BlockPos min = null;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CLUSTER_RANGE_H, -CLUSTER_RANGE_V, -CLUSTER_RANGE_H),
                center.offset(CLUSTER_RANGE_H, CLUSTER_RANGE_V, CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof MirrorSigilBlock) {
                BlockPos immutable = pos.immutable();
                cluster.add(immutable);
                if (min == null || immutable.asLong() < min.asLong()) {
                    min = immutable;
                }
            }
        }
        if (cluster.isEmpty() || min == null) {
            return;
        }
        // 跨调用确定性:同一簇无论从哪枚符印触发,重排结果一致(gotchas #15 同思路)
        RandomSource random = RandomSource.create(min.getX() * 341873128712L ^ min.getZ() * 132897987541L);
        List<BlockPos> shuffled = new ArrayList<>(cluster);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        int realCount = Math.min(REAL_PER_CLUSTER, Math.max(1, shuffled.size() / 3));
        for (int i = 0; i < shuffled.size(); i++) {
            if (level.getBlockEntity(shuffled.get(i)) instanceof MirrorSigilBlockEntity sigil
                    && !sigil.initialized) {
                sigil.setReal(i < realCount);
            }
        }
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
