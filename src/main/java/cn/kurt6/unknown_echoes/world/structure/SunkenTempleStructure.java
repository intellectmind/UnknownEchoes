package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 沉没圣殿:镜湖湖底的水下遗迹(V0.5 潮汐回响获取点)。
 * 镜石砖残殿,内部保留湖水不排空:三根潮汐符柱(个人激活)+ 中央潮汐核心祭坛 +
 * 四面水下符文 + 一只镜湖宝箱。所有机关判定都在服务端(设计红线 #1/#6)。
 */
public class SunkenTempleStructure extends SinglePieceStructure {
    public static final MapCodec<SunkenTempleStructure> CODEC = simpleCodec(SunkenTempleStructure::new);

    private static final int WIDTH = 65;
    private static final int DEPTH = 65;
    private static final int HEIGHT = 18;
    private static final int BOSS_ARENA_SIZE = 29;
    private static final int CENTER_X = WIDTH / 2;
    private static final int CENTER_Z = DEPTH / 2;
    private static final int PILLAR_OFFSET = 9;

    public SunkenTempleStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 镜湖群系存在旱地区段:只有整片场地确实沉在湖水里才生成。
        // 不调用 super——SinglePieceStructure 默认"最低地表 < 海平面就不放"(防沙漠神殿落海),
        // 对水下结构正好相反,必须绕开并改锚湖底(OCEAN_FLOOR_WG)。
        //
        // CRITICAL: 不在此处做连续水方块校验(getBaseColumn)——/locate 会对每个候选区块调用此方法。
        // 这里只用五点高度图预判过滤空现场;严格水深校验推迟到 postProcess 首次生成时做。
        if (!LakeBedPlacement.hasSubmergedFootprint(context, WIDTH, 14, 7)) {
            return java.util.Optional.empty();
        }
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(new Piece(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SUNKEN_TEMPLE.get();
    }

    public static class Piece extends EchoStructurePiece {

        /** 已计算出的湖底基准高度;<0 表示尚未计算(随 NBT 持久化,避免跨区块重复下移)。 */
        private int oceanFloorY = -1;
        /** 深渊观测者只生成一次(随 NBT 持久化)。 */
        private boolean spawnedBoss = false;
        /** 水深校验已失败,整座结构取消生成(随 NBT 持久化,避免跨区块重复校验)。 */
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SUNKEN_TEMPLE_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SUNKEN_TEMPLE_PIECE.get(), tag);
            this.oceanFloorY = tag.getInt("OceanFloorY");
            if (!tag.contains("OceanFloorY")) {
                this.oceanFloorY = -1;
            }
            this.spawnedBoss = tag.getBoolean("SpawnedBoss");
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("SpawnedBoss", this.spawnedBoss);
            tag.putBoolean("Rejected", this.rejected);
        }

        /**
         * 把结构底部对齐到湖底(OCEAN_FLOOR_WG 忽略水面),并做水深校验。
         * 仿照 updateAverageGroundHeight:只采样当前区块内的列,首次成功后锁定。
         * 校验失败时标记 rejected,后续 postProcess 调用直接跳过不再生成任何方块。
         */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;  // 已被拒绝,不再生成
            }
            if (this.oceanFloorY >= 0) {
                return true;  // 已校验通过并锁定高度
            }
            // 首次处理:先对齐湖底,再校验水深
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
                return false;  // 当前区块内无采样点,等下次 postProcess
            }
            this.oceanFloorY = total / count;

            // 水深校验:大型足印跨多个区块,用噪声柱避免读取未就绪边角区块导致空现场。
            if (!LakeBedPieceHelper.checkWaterDepthByNoise(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH, 14, 7)) {
                this.rejected = true;
                return false;  // 落旱地,取消整座结构生成
            }

            this.boundingBox.move(0, this.oceanFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.adjustToLakeFloor(level, box, generator)) {
                return;  // 水深校验失败或等待更多区块生成
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState stone = ModBlocks.MIRROR_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.TIDE_RUNE.get().defaultBlockState();
            BlockState pillar = ModBlocks.TIDE_PILLAR.get().defaultBlockState();
            BlockState altar = ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState();

            buildTempleShell(level, random, box, bricks, stone, rune);
            buildSurfaceTower(level, box, bricks, rune);
            buildBubbleCorridor(level, box, bricks);
            buildTidePillarCourt(level, box, bricks, pillar, altar);
            buildWatcherArena(level, box, random, bricks, rune);
            buildRevisitReservoir(level, box, random, bricks);
            spawnWatcher(level, box);
        }

        private void buildTempleShell(WorldGenLevel level, RandomSource random, BoundingBox box,
                                      BlockState bricks, BlockState stone, BlockState rune) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.3F ? stone : bricks, x, 0, z, box);
                }
            }
            for (int y = 1; y <= 12; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (!isDoorway(x, WIDTH) || y > 5) {
                        maybeWall(level, box, random, bricks, x, y, 0);
                        maybeWall(level, box, random, bricks, x, y, DEPTH - 1);
                    }
                }
                for (int z = 1; z < DEPTH - 1; z++) {
                    if (!isDoorway(z, DEPTH) || y > 5) {
                        maybeWall(level, box, random, bricks, 0, y, z);
                        maybeWall(level, box, random, bricks, WIDTH - 1, y, z);
                    }
                }
            }
            this.placeBlock(level, rune, CENTER_X, 7, 0, box);
            this.placeBlock(level, rune, CENTER_X, 7, DEPTH - 1, box);
            this.placeBlock(level, rune, 0, 7, CENTER_Z, box);
            this.placeBlock(level, rune, WIDTH - 1, 7, CENTER_Z, box);
        }

        private void buildSurfaceTower(WorldGenLevel level, BoundingBox box, BlockState bricks, BlockState rune) {
            int towerX = CENTER_X;
            int towerZ = 7;
            for (int y = 1; y <= HEIGHT - 1; y++) {
                int radius = y > 12 ? 3 : 5;
                for (int x = towerX - radius; x <= towerX + radius; x++) {
                    for (int z = towerZ - radius; z <= towerZ + radius; z++) {
                        boolean shell = x == towerX - radius || x == towerX + radius
                                || z == towerZ - radius || z == towerZ + radius;
                        if (shell) {
                            this.placeBlock(level, y % 4 == 0 ? rune : bricks, x, y, z, box);
                        }
                    }
                }
            }
        }

        private void buildBubbleCorridor(WorldGenLevel level, BoundingBox box, BlockState bricks) {
            for (int z = 10; z <= CENTER_Z - 11; z++) {
                for (int x = CENTER_X - 2; x <= CENTER_X + 2; x++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
                if (z % 6 == 0) {
                    this.placeBlock(level, net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN.defaultBlockState(),
                            CENTER_X, 2, z, box);
                    this.placeBlock(level, net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(),
                            CENTER_X - 2, 2, z, box);
                }
            }
            buildAccessibleChest(level, RandomSource.create(0), box, CENTER_X + 5, 1, 17, ModLootTables.MIRROR_LAKE_CACHE);
        }

        private void buildTidePillarCourt(WorldGenLevel level, BoundingBox box,
                                          BlockState bricks, BlockState pillar, BlockState altar) {
            placePillar(level, box, bricks, pillar, CENTER_X - PILLAR_OFFSET, CENTER_Z - PILLAR_OFFSET);
            placePillar(level, box, bricks, pillar, CENTER_X + PILLAR_OFFSET, CENTER_Z - PILLAR_OFFSET);
            placePillar(level, box, bricks, pillar, CENTER_X, CENTER_Z + PILLAR_OFFSET);
            for (int x = CENTER_X - 2; x <= CENTER_X + 2; x++) {
                for (int z = CENTER_Z - 2; z <= CENTER_Z + 2; z++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
            }
            this.placeBlock(level, altar, CENTER_X, 2, CENTER_Z, box);
        }

        private void buildWatcherArena(WorldGenLevel level, BoundingBox box, RandomSource random,
                                       BlockState bricks, BlockState rune) {
            int r = BOSS_ARENA_SIZE / 2;
            for (int x = CENTER_X - r; x <= CENTER_X + r; x++) {
                for (int z = CENTER_Z + 16 - r; z <= CENTER_Z + 16 + r; z++) {
                    boolean edge = x == CENTER_X - r || x == CENTER_X + r
                            || z == CENTER_Z + 16 - r || z == CENTER_Z + 16 + r;
                    this.placeBlock(level, edge ? rune : bricks, x, edge ? 2 : 1, z, box);
                }
            }
            for (int dx : new int[]{-10, 10}) {
                this.placeBlock(level, net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN.defaultBlockState(),
                        CENTER_X + dx, 2, CENTER_Z + 16, box);
            }
            buildAccessibleChest(level, random, box, CENTER_X - 7, 2, CENTER_Z + 22, ModLootTables.T4_REGION_CORE);
        }

        private void buildRevisitReservoir(WorldGenLevel level, BoundingBox box, RandomSource random, BlockState bricks) {
            for (int x = 5; x <= 15; x++) {
                for (int z = DEPTH - 16; z <= DEPTH - 6; z++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
            }
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), 10, 2, DEPTH - 11, box);
            buildTrueSightVaultNiche(level, random, box, 10, 2, DEPTH - 8, Direction.NORTH);
        }

        private void spawnWatcher(WorldGenLevel level, BoundingBox box) {
            int bossX = this.getWorldX(CENTER_X, CENTER_Z + 16);
            int bossZ = this.getWorldZ(CENTER_X, CENTER_Z + 16);
            int bossY = this.getWorldY(4);
            BlockPos bossPos = new BlockPos(bossX, bossY, bossZ);
            if (!this.spawnedBoss && box.isInside(bossPos)) {
                AbyssWatcher boss = ModEntities.ABYSS_WATCHER.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossX + 0.5D, bossY, bossZ + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                    this.spawnedBoss = true;
                }
            }
        }

        /** 每面墙正中 3 格为拱门口。 */
        private static boolean isDoorway(int coord, int span) {
            int mid = span / 2;
            return coord >= mid - 1 && coord <= mid + 1;
        }

        /** 残缺墙体:大部分保留,留出蚀空感。 */
        private void maybeWall(WorldGenLevel level, BoundingBox box, RandomSource random,
                               BlockState bricks, int x, int y, int z) {
            if (random.nextFloat() < 0.85F) {
                this.placeBlock(level, bricks, x, y, z, box);
            }
        }

        private void placePillar(WorldGenLevel level, BoundingBox box,
                                 BlockState base, BlockState pillar, int x, int z) {
            this.placeBlock(level, base, x, 1, z, box);
            this.placeBlock(level, pillar, x, 2, z, box);
        }
    }
}
