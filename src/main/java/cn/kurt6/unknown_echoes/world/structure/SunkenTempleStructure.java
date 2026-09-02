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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 沉没圣殿:镜湖湖底的水下遗迹( 潮汐回响获取点)。 */
public class SunkenTempleStructure extends SinglePieceStructure {
    public static final MapCodec<SunkenTempleStructure> CODEC = simpleCodec(SunkenTempleStructure::new);

    private static final int WIDTH = 65;
    private static final int DEPTH = 65;
    private static final int HEIGHT = 30;
    private static final int BOSS_ARENA_SIZE = 29;
    private static final int CENTER_X = WIDTH / 2;
    private static final int CENTER_Z = DEPTH / 2;
    private static final int PILLAR_OFFSET = 9;

    public SunkenTempleStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Piece piece = new Piece(context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        if (!LakeBedPlacement.hasSubmergedFootprint(context,
                footprint.minX(), footprint.minZ(), WIDTH, 25, 21)) {
            return java.util.Optional.empty();
        }
        int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(
                context, footprint.minX(), footprint.minZ(), WIDTH, DEPTH);
        if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
            return java.util.Optional.empty();
        }
        piece.anchorToLakeFloor(floorY);
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(piece));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SUNKEN_TEMPLE.get();
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] SUNKEN_TEMPLE_MIRROR_NODES = {
                {8, 8}, {32, 8}, {56, 8},
                {8, 32}, {56, 32},
                {8, 56}, {32, 56}, {56, 56},
                {16, 16}, {32, 16}, {48, 16},
                {16, 32}, {48, 32},
                {16, 48}, {32, 48}, {48, 48}
        };

        /** 已计算出的湖底基准高度;<0 表示尚未计算(随 NBT 持久化,避免跨区块重复下移)。 */
        private int oceanFloorY = -1;
        /** 深渊观测者只生成一次(随 NBT 持久化)。 */
        private boolean spawnedBoss = false;
        private boolean bossSpawnChunkVisited = false;
        private boolean altarChunkVisited = false;
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
            this.bossSpawnChunkVisited = tag.getBoolean("BossSpawnChunkVisited");
            this.altarChunkVisited = tag.getBoolean("AltarChunkVisited");
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("SpawnedBoss", this.spawnedBoss);
            tag.putBoolean("BossSpawnChunkVisited", this.bossSpawnChunkVisited);
            tag.putBoolean("AltarChunkVisited", this.altarChunkVisited);
            tag.putBoolean("Rejected", this.rejected);
        }

        public void anchorToLakeFloor(int floorY) {
            if (this.oceanFloorY >= 0) {
                return;
            }
            this.oceanFloorY = floorY;
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 1, 0);
        }

        /** 把结构底部对齐到湖底(OCEAN_FLOOR_WG 忽略水面),并做水深校验。 */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.oceanFloorY >= 0) {
                return true;
            }

            int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH);
            if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
                this.rejected = true;
                return false;
            }
            this.oceanFloorY = floorY;

            if (!LakeBedPieceHelper.checkWaterDepthByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH, 25, 21)) {
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
            box = LakeBedPieceHelper.intersection(box, this.boundingBox);
            if (box == null) {
                return;
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.TIDE_RUNE.get().defaultBlockState();
            BlockState pillar = ModBlocks.TIDE_PILLAR.get().defaultBlockState();
            BlockState altar = ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();

            // 先清掉足印内的自然地形并回填水，避免湖床山丘穿进 Boss 场地。
            this.generateBox(level, box, 1, 1, 1, WIDTH - 2, HEIGHT - 2, DEPTH - 2,
                    water, water, false);

            buildTempleShell(level, random, box, bricks, rune);
            buildSurfaceTower(level, box, bricks, rune);
            buildBubbleCorridor(level, random, box, bricks);
            buildTidePillarCourt(level, box, bricks, pillar, altar);
            buildWatcherArena(level, box, random, bricks, rune);
            buildRevisitReservoir(level, box, random, bricks);
            buildMonumentFrame(level, random, box);

            placeSubmergedStructureSpawner(level, random, box, 10, 1, 40,
                    ModEntities.WATER_SHADOW.get(), bricks, bricks);
            for (int[] node : SUNKEN_TEMPLE_MIRROR_NODES) {
                this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                        node[0], 0, node[1], box);
            }
            spawnWatcher(level, box);
        }

        private void buildMonumentFrame(WorldGenLevel level, RandomSource random, BoundingBox box) {
            BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
            BlockState worn = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
            BlockState light = ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState();
            architecturalRing(level, box, CENTER_X, 1, CENTER_Z, 30, worn);
            buildCircularColonnade(level, box, CENTER_X, 2, CENTER_Z, 28, 20, 12,
                    reef, light, worn);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, CENTER_X, 2, CENTER_Z, 30, direction,
                        4, 10, reef, light);
            }
            for (int radius : new int[]{18, 24}) {
                architecturalRing(level, box, CENTER_X, 3 + radius / 6, CENTER_Z,
                        radius, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            }
            scatterArchitecturalDebris(level, random, box, CENTER_X, 2, CENTER_Z,
                    25, 30, 46, worn, reef);
        }

        private void buildTempleShell(WorldGenLevel level, RandomSource random, BoundingBox box,
                                      BlockState bricks, BlockState rune) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, bricks, x, 0, z, box);
                }
            }
            for (int y = 1; y <= 20; y++) {
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

        private void buildBubbleCorridor(WorldGenLevel level, RandomSource random,
                                         BoundingBox box, BlockState bricks) {
            for (int z = 10; z <= CENTER_Z - 11; z++) {
                for (int x = CENTER_X - 2; x <= CENTER_X + 2; x++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
                if (z % 6 == 0) {

                    this.placeBlock(level, net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState(),
                            CENTER_X, 1, z, box);
                    this.placeBlock(level, net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN.defaultBlockState(),
                            CENTER_X, 2, z, box);
                    this.placeBlock(level, net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(),
                            CENTER_X - 2, 2, z, box);
                }
            }
            buildAccessibleChest(level, random, box, CENTER_X + 5, 1, 17, ModLootTables.MIRROR_LAKE_CACHE);
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
            int arenaZ = CENTER_Z + 16;

            BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
            BlockState worn = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
            BlockState light = ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState();
            buildRibbedVaultHall(level, box, CENTER_X, 2, CENTER_Z + 6, Footprint.of(16, 26), 14, 6, bricks, reef, worn, light);
            buildTwinTowerFacade(level, box, CENTER_X, 2, 6,
                    21, 4, 18, reef, worn, light);
            buildArcadedWing(level, box, 10, 2, CENTER_Z + 5, Footprint.of(6, 14), 11, 4, bricks, reef, worn, light);
            buildArcadedWing(level, box, WIDTH - 11, 2, CENTER_Z + 5, Footprint.of(6, 14), 11, 4, bricks, reef, worn, light);
            buildBossApse(level, box, CENTER_X, 2, CENTER_Z + 23,
                    Direction.SOUTH, 7, 12, bricks, reef, worn, light);
            // 必须加穹顶:镜湖神殿也用拱厅+镜塔配方,穹顶是唯一区分点。
            buildDomedRotunda(level, box, CENTER_X, 2, CENTER_Z + 6,
                    18, 14, bricks, reef, worn,
                    ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());

            buildBossArenaMandala(level, box, CENTER_X, 1, arenaZ, r - 1,
                    ModBlocks.TIDE_RUNE.get().defaultBlockState(),
                    ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState(),
                    ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState());
            for (int dx : new int[]{-10, 10}) {
                this.placeBlock(level, net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState(),
                        CENTER_X + dx, 1, arenaZ, box);
                this.placeBlock(level, net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN.defaultBlockState(),
                        CENTER_X + dx, 2, arenaZ, box);
            }
            buildArenaLightRing(level, box, CENTER_X, 3, arenaZ, r - 2, 12, 11, reef, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, CENTER_X, 17, CENTER_Z + 6, Footprint.of(16, 26), ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, CENTER_X, 18, CENTER_Z + 6, Footprint.of(16, 26), 7, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), worn);
            buildAccessibleChest(level, random, box, CENTER_X - 7, 2, CENTER_Z + 22,
                    ModLootTables.T5_MINIBOSS_REPEAT);
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
            BlockPos altarPos = new BlockPos(this.getWorldX(CENTER_X, CENTER_Z),
                    this.getWorldY(2), this.getWorldZ(CENTER_X, CENTER_Z));
            if (box.isInside(bossPos)) {
                this.bossSpawnChunkVisited = true;
            }
            if (box.isInside(altarPos)
                    && level.getBlockState(altarPos).is(ModBlocks.TIDE_CORE_ALTAR.get())) {
                this.altarChunkVisited = true;
            }
            if (!this.spawnedBoss && this.bossSpawnChunkVisited && this.altarChunkVisited) {
                AbyssWatcher boss = ModEntities.ABYSS_WATCHER.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossX + 0.5D, bossY, bossZ + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    boss.bindRechallengeArena(altarPos, bossPos);
                    if (level.addFreshEntity(boss)) {
                        this.spawnedBoss = true;
                    } else {
                        boss.discard();
                    }
                }
            }
        }

        private static boolean isDoorway(int coord, int span) {
            int mid = span / 2;
            return coord >= mid - 1 && coord <= mid + 1;
        }

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
