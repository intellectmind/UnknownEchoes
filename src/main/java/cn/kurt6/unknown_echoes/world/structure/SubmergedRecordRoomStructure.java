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
 * 淹没记录室(V0.6E,5.3 强化方向落地):镜湖水下小型结构。
 * 沉在湖床里的记述派档案残间——湖底只露出一圈残破砖沿与顶口,
 * 室内:档案宝箱(残页+潮汐研究拓片)+ 水下符文回访点。
 * 不承载权限门槛,只服务潮汐研究与回访(探索日志提示见 JournalManager)。
 */
public class SubmergedRecordRoomStructure extends SinglePieceStructure {
    public static final MapCodec<SubmergedRecordRoomStructure> CODEC = simpleCodec(SubmergedRecordRoomStructure::new);

    private static final int SIZE = 15;
    private static final int HEIGHT = 9;

    public SubmergedRecordRoomStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 与倒影回廊一致:必须真正沉在水里(镜湖存在旱地区段),水下结构绕开
        // SinglePieceStructure 的"最低地表 < 海平面即拒绝"默认逻辑。
        //
        // CRITICAL: 不在此处做水深校验——/locate 会对每个候选区块调用此方法,
        // 累积的噪声柱采样会拖服务端主线程超看门狗限制。水深校验推迟到 postProcess。
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(new Piece(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SUBMERGED_RECORD_ROOM.get();
    }

    public static class Piece extends EchoStructurePiece {

        /** 已计算出的湖底基准高度;<0 表示尚未计算(NBT 持久化,防跨区块重复下移)。 */
        private int lakeFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SUBMERGED_RECORD_ROOM_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SUBMERGED_RECORD_ROOM_PIECE.get(), tag);
            this.lakeFloorY = tag.contains("LakeFloorY") ? tag.getInt("LakeFloorY") : -1;
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("LakeFloorY", this.lakeFloorY);
            tag.putBoolean("Rejected", this.rejected);
        }

        /** 沉进湖床并校验水深:顶板(y4)与湖底齐平,只有 y5 残沿露出湖床一格。 */
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

            // 水深校验:扩展为 15x15 水下记录室后,仍保持轻量生成阶段校验。
            if (!LakeBedPieceHelper.checkWaterDepth(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 6, 4)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.lakeFloorY - this.boundingBox.minY() - 5, 0);
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
            BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            int c = SIZE / 2; // 7

            // 外壳:地板 y0 / 四壁 y1-3 / 顶板 y4(留顶口和短走廊);室内充水(淹没档案)
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean wall = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                    this.placeBlock(level, random.nextFloat() < 0.3F ? mirrorStone : mirror, x, 0, z, box);
                    for (int y = 1; y <= 3; y++) {
                        this.placeBlock(level, wall ? (random.nextFloat() < 0.2F ? cracked : mirror) : water,
                                x, y, z, box);
                    }
                    boolean opening = (x == c && z >= c - 1 && z <= c + 2)
                            || (z == 1 && x >= c - 2 && x <= c + 2);
                    this.placeBlock(level, opening ? water
                            : (random.nextFloat() < 0.2F ? cracked : mirror), x, 4, z, box);
                }
            }
            // 湖床上的残沿:顶口四周残破砖块(远看是一圈"沉下去的屋脊")
            for (int[] rim : new int[][]{{c - 1, c}, {c + 1, c}, {c - 1, c + 1}, {c + 1, c + 1},
                    {c, c - 1}, {c, c + 2}}) {
                if (random.nextFloat() < 0.7F) {
                    this.placeBlock(level, cracked, rim[0], 5, rim[1], box);
                }
            }

            // 室内:档案宝箱(残页+潮汐拓片)+ 水下符文回访点 + 符文砖微光
            this.placeBlock(level, mirror, c, 1, c - 1, box);
            buildAccessibleChest(level, random, box, c, 2, c - 1, ModLootTables.SUBMERGED_RECORD_ROOM);
            buildAccessibleChest(level, random, box, c - 4, 2, c + 3, ModLootTables.MIRROR_LAKE_CACHE);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), 1, 1, 1, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), SIZE - 2, 1, SIZE - 2, box);
            this.placeBlock(level, runeBricks, 1, 3, SIZE - 2, box);
            buildTrueSightVaultNiche(level, random, box, SIZE - 4, 2, c + 3, Direction.WEST);
        }
    }
}
