package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock;
import cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock;
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
 * 潮汐灯塔礁:镜湖深水区的环形礁盘 + 半倒塌灯塔废墟(V0.6C 镜湖区域变体,设计文档 4.3.2)。
 * 承载潮汐执灯者 Mini Boss:三枚水中浮标(破灯)+ 三座水下符文座(致盲)分布在外环与
 * 不同高度,中央灯塔残柱作为掩体。普通探索给水下符文片/镜湖碎片/残页线索(宝箱)。
 * 挑战门槛:MiniBossSpawnerBlock 服务端校验 TIDE_ECHO 或深渊观测者击败记录(红线 #1)。
 */
public class TideLighthouseReefStructure extends SinglePieceStructure {
    public static final MapCodec<TideLighthouseReefStructure> CODEC =
            simpleCodec(TideLighthouseReefStructure::new);

    private static final int SIZE = 25;
    private static final int HEIGHT = 12;

    public TideLighthouseReefStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 深水礁盘:只生成在镜湖深水(灯塔高 9 + 浮标层,要求中心水深 ≥ 11)。
        // 不调用 super——SinglePieceStructure 默认拒绝水下放置(gotchas #16)。
        //
        // CRITICAL: 不在此处做水深校验——/locate 会对每个候选区块调用此方法,
        // 累积的噪声柱采样会拖服务端主线程超看门狗限制。水深校验推迟到 postProcess。
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(new Piece(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TIDE_LIGHTHOUSE_REEF.get();
    }

    public static class Piece extends EchoStructurePiece {

        private int oceanFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.TIDE_LIGHTHOUSE_REEF_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.TIDE_LIGHTHOUSE_REEF_PIECE.get(), tag);
            this.oceanFloorY = tag.getInt("OceanFloorY");
            if (!tag.contains("OceanFloorY")) {
                this.oceanFloorY = -1;
            }
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("Rejected", this.rejected);
        }

        /** 对齐湖底并校验水深,首次成功后锁定。 */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.oceanFloorY >= 0) {
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
            this.oceanFloorY = total / count;

            // 水深校验:灯塔高 9 + 浮标层,要求中心 ≥11,四角 ≥6
            if (!LakeBedPieceHelper.checkWaterDepth(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 11, 6)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.oceanFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.adjustToLakeFloor(level, box, generator)) {
                return;
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState stone = ModBlocks.MIRROR_STONE.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState deepslate = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            BlockState buoy = ModBlocks.TIDE_BUOY.get().defaultBlockState()
                    .setValue(TideBuoyBlock.LIT, Boolean.TRUE)
                    .setValue(TideBuoyBlock.WATERLOGGED, Boolean.TRUE);
            BlockState seat = ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(TideRuneSeatBlock.LIT, Boolean.TRUE)
                    .setValue(TideRuneSeatBlock.WATERLOGGED, Boolean.TRUE);
            BlockState chain = Blocks.CHAIN.defaultBlockState();
            BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
            int c = SIZE / 2; // 12

            // 环形礁盘:半径 8-11 的抬升环,深板岩混镜石,留出蚀空缺口
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    int distSq = dx * dx + dz * dz;
                    if (distSq >= 64 && distSq <= 121 && random.nextFloat() < 0.85F) {
                        BlockState reef = random.nextFloat() < 0.5F ? deepslate
                                : (random.nextFloat() < 0.5F ? stone : bricks);
                        this.placeBlock(level, reef, x, 1, z, box);
                        if (random.nextFloat() < 0.3F) {
                            this.placeBlock(level, deepslate, x, 2, z, box);
                        }
                    }
                    // 礁盘内圈地坪:镜石散铺(战场地面,不满铺保留湖底)
                    if (distSq < 64 && random.nextFloat() < 0.4F) {
                        this.placeBlock(level, random.nextFloat() < 0.4F ? bricks : stone, x, 1, z, box);
                    }
                }
            }

            // 中央灯塔基座:5×5 环墙,半倒塌(越高越残缺),内部留空
            for (int y = 1; y <= 8; y++) {
                float keep = 1.0F - (y - 2) * 0.13F;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        if (!wall) {
                            continue;
                        }
                        // 东面 y1-2 开门洞
                        if (dx == 2 && Math.abs(dz) <= 0 && y <= 2) {
                            continue;
                        }
                        if (random.nextFloat() < keep) {
                            this.placeBlock(level, y % 3 == 0 && random.nextFloat() < 0.3F
                                    ? runeBricks : bricks, c + dx, y, c + dz, box);
                        }
                    }
                }
            }
            // 破碎灯室:残存的灯——湖面远望可见的微光源
            this.placeBlock(level, lantern, c, 8, c, box);
            this.placeBlock(level, bricks, c, 7, c, box);

            // 三枚水中浮标:外环三角分布,锚链垂到礁盘
            int[][] buoyOffsets = {{8, 0}, {-4, 7}, {-4, -7}};
            for (int[] offset : buoyOffsets) {
                int bx = c + offset[0];
                int bz = c + offset[1];
                this.placeBlock(level, deepslate, bx, 1, bz, box);
                for (int y = 2; y <= 4; y++) {
                    this.placeBlock(level, chain, bx, y, bz, box);
                }
                this.placeBlock(level, buoy, bx, 5, bz, box);
            }

            // 三座水下符文座:不同高度(礁盘 / 塔侧台 / 礁岩柱),分散站位
            this.placeBlock(level, seat, c - 8, 2, c, box);          // 礁盘西缘
            this.placeBlock(level, bricks, c + 3, 1, c + 3, box);
            this.placeBlock(level, bricks, c + 3, 2, c + 3, box);
            this.placeBlock(level, bricks, c + 3, 3, c + 3, box);
            this.placeBlock(level, seat, c + 3, 4, c + 3, box);      // 塔侧高台
            this.placeBlock(level, deepslate, c + 5, 1, c - 6, box);
            this.placeBlock(level, deepslate, c + 5, 2, c - 6, box);
            this.placeBlock(level, seat, c + 5, 3, c - 6, box);      // 礁岩柱

            // 探索奖励:灯塔基座内的回访宝箱(水下符文片/镜湖碎片/残页线索)
            buildAccessibleChest(level, random, box, c - 1, 1, c - 1,
                    ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);

            // 守护者场地计时器:埋在塔基,服务端校验 TIDE_ECHO 或深渊观测者记录后才生成
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(0), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("tide_lantern_keeper"), 10);
                spawner.setChallengeGate(EchoAbilityType.TIDE_ECHO,
                        UnknownEchoes.id("abyss_watcher"),
                        "message.unknown_echoes.tide_lighthouse.locked");
            }
        }
    }
}
