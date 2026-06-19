package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 倒影回廊:镜湖"倒影入口"的落地实现(正典:镜面档案的阅览亭)。
 * 技术边界(设计文档 4.3.2):不做实时反射——
 * 湖底是预构建的"倒置阅览亭"(檐座在下、地板朝上,可进入,这才是档案本体);
 * 湖面上方只有残缺的原像立柱(装饰,不完整)。
 * 亭内:镜湖宝箱 + 可读水下符文(潮汐回响回访点)。
 */
public class ReflectionVaultStructure extends SinglePieceStructure {
    public static final MapCodec<ReflectionVaultStructure> CODEC = simpleCodec(ReflectionVaultStructure::new);

    private static final int SIZE = 11;
    private static final int HEIGHT = 40; // 包含伸出水面的原像柱

    public ReflectionVaultStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 镜湖群系存在旱地区段:倒置阅览亭(顶在湖底 +4)必须沉在水里,原像柱才有"水面"可出。
        // 不调用 super——SinglePieceStructure 默认拒绝"最低地表 < 海平面"的位置,水下结构必须绕开。
        //
        // CRITICAL: 不在此处做水深校验——/locate 会对每个候选区块调用此方法,
        // 累积的噪声柱采样会拖服务端主线程超看门狗限制。水深校验推迟到 postProcess。
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(new Piece(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.REFLECTION_VAULT.get();
    }

    public static class Piece extends EchoStructurePiece {

        /** 已计算出的湖底基准高度;<0 表示尚未计算(随 NBT 持久化,避免跨区块重复下移)。 */
        private int lakeFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.REFLECTION_VAULT_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.REFLECTION_VAULT_PIECE.get(), tag);
            this.lakeFloorY = tag.contains("LakeFloorY") ? tag.getInt("LakeFloorY") : -1;
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("LakeFloorY", this.lakeFloorY);
            tag.putBoolean("Rejected", this.rejected);
        }

        /** 对齐湖底并校验水深,首次成功后锁定。 */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.lakeFloorY >= 0) {
                return true;
            }
            int total = 0;
            int count = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
                for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
                    cursor.set(x, 64, z);
                    if (chunkBox.isInside(cursor)) {
                        total += level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        count++;
                    }
                }
            }
            if (count == 0) {
                return false;
            }
            this.lakeFloorY = total / count;

            // 水深校验:倒置亭顶在湖底 +4,要求中心 ≥7,四角 ≥5
            if (!LakeBedPieceHelper.checkWaterDepth(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 7, 5)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.lakeFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!adjustToLakeFloor(level, box, generator)) {
                return;
            }

            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState mirrorStone = ModBlocks.MIRROR_STONE.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            int c = SIZE / 2; // 5

            // ---- 湖底:倒置阅览亭 ----
            // 檐座(原本的亭顶,如今朝下埋在湖底):9x9 大于地板,读作"倒过来"
            for (int x = c - 4; x <= c + 4; x++) {
                for (int z = c - 4; z <= c + 4; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.3F ? mirrorStone : mirror, x, 0, z, box);
                }
            }
            // 亭身:四角柱(y1~3),四面敞开——任意一面都是"倒影入口"
            for (int[] corner : new int[][]{{c - 3, c - 3}, {c + 3, c - 3}, {c - 3, c + 3}, {c + 3, c + 3}}) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, mirror, corner[0], y, corner[1], box);
                }
            }
            // 内部充水排空:倒置亭内是水下气室?——正典是水下档案,保留水体更合理;只清掉实体方块
            for (int x = c - 2; x <= c + 2; x++) {
                for (int z = c - 2; z <= c + 2; z++) {
                    for (int y = 1; y <= 3; y++) {
                        this.placeBlock(level, water, x, y, z, box);
                    }
                }
            }
            // "地板"(原本的亭底,如今朝上做顶):7x7
            for (int x = c - 3; x <= c + 3; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : mirror, x, 4, z, box);
                }
            }

            // 亭内:档案宝箱 + 可读水下符文(潮汐回响回访点)
            this.placeBlock(level, mirror, c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 2, c, ModLootTables.MIRROR_LAKE_CACHE);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c, 2, c + 2, box);

            // ---- 深层内室(V0.6E,5.3"进入镜湖倒影层"):埋在湖床之下的镜面档案库 ----
            // 入口在檐座层(y0)的潮汐感应门:服务端校验 TIDE_ECHO 的自动门,
            // 开启保持时长 = "倒影入口稳定时间"(研究 3 延长)。
            // 局部 y 为负:内室刻意不进结构包围盒,旧档已生成的回廊跨区块续生成时布局不错位
            // (placeBlock 只按区块盒裁剪,负向局部坐标在区块全高内有效)。
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            // 内室外壳使用永久封印石，确保潮汐感应门是唯一入口。
            for (int x = c - 3; x <= c + 3; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, anchored, x, -6, z, box);
                    boolean shaft = x == c && z == c + 2;
                    if (!shaft) {
                        this.placeBlock(level, anchored, x, -1, z, box);
                    }
                    boolean wall = x == c - 3 || x == c + 3 || z == c - 3 || z == c + 3;
                    for (int y = -5; y <= -2; y++) {
                        this.placeBlock(level, wall ? anchored : water, x, y, z, box);
                    }
                }
            }
            // 竖井与感应门:门替换檐座一格(与湖床齐平),井下一格通顶板开口
            this.placeBlock(level, water, c, -1, c + 2, box);
            this.placeBlock(level, ModBlocks.TIDE_SENSOR_DOOR.get().defaultBlockState(), c, 0, c + 2, box);
            // 内室陈设:中央台座宝箱 + 两枚水下符文(回访点)+ 顶角符文砖微光
            this.placeBlock(level, mirror, c, -5, c, box);
            buildAccessibleChest(level, random, box, c, -4, c, ModLootTables.REFLECTION_INNER_VAULT);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c - 2, -5, c - 2, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c + 2, -5, c - 2, box);
            this.placeBlock(level, runeBricks, c - 2, -2, c + 2, box);
            this.placeBlock(level, runeBricks, c + 2, -2, c + 2, box);

            // ---- 湖面:残缺原像柱(装饰,不完整;伸出水面 1~2 格) ----
            int seaLevel = level.getSeaLevel();
            int baseWorldY = this.getWorldY(5);
            for (int[] corner : new int[][]{{c - 3, c - 3}, {c + 3, c + 3}}) {
                int topWorldY = seaLevel + 1 + random.nextInt(2);
                for (int worldY = baseWorldY; worldY <= topWorldY; worldY++) {
                    int localY = worldY - this.getWorldY(0);
                    if (localY >= HEIGHT) {
                        break;
                    }
                    if (random.nextFloat() < 0.12F) {
                        continue; // 残缺
                    }
                    this.placeBlock(level, random.nextFloat() < 0.35F ? cracked : mirror,
                            corner[0], localY, corner[1], box);
                }
            }
            // 另外两角只剩半截(原像"不完整版本")
            for (int[] corner : new int[][]{{c + 3, c - 3}, {c - 3, c + 3}}) {
                int stubTop = baseWorldY + 2 + random.nextInt(3);
                for (int worldY = baseWorldY; worldY <= stubTop; worldY++) {
                    int localY = worldY - this.getWorldY(0);
                    if (localY < HEIGHT) {
                        this.placeBlock(level, mirror, corner[0], localY, corner[1], box);
                    }
                }
            }
        }
    }
}
