package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/** 后期区域同名结构发行候选版，补齐晶歌、沉眠和档案馆入口结构。 */
public class LateRegionSiteStructure extends SinglePieceStructure {
    public static final MapCodec<LateRegionSiteStructure> CRYSTAL_SONG_SHRINE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.CRYSTAL_SONG_SHRINE));
    public static final MapCodec<LateRegionSiteStructure> CRYSTAL_SONG_HALL_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.CRYSTAL_SONG_HALL));
    public static final MapCodec<LateRegionSiteStructure> DREAM_FLOWER_HOUSE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.DREAM_FLOWER_HOUSE));
    public static final MapCodec<LateRegionSiteStructure> ECHO_GRAND_ARCHIVE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.ECHO_GRAND_ARCHIVE));

    private final Kind kind;

    private LateRegionSiteStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case CRYSTAL_SONG_SHRINE -> ModStructures.CRYSTAL_SONG_SHRINE.get();
            case CRYSTAL_SONG_HALL -> ModStructures.CRYSTAL_SONG_HALL.get();
            case DREAM_FLOWER_HOUSE -> ModStructures.DREAM_FLOWER_HOUSE.get();
            case ECHO_GRAND_ARCHIVE -> ModStructures.ECHO_GRAND_ARCHIVE.get();
        };
    }

    public enum Kind {
        CRYSTAL_SONG_SHRINE("crystal_song_shrine", 25, 18),
        CRYSTAL_SONG_HALL("crystal_song_hall", 49, 34),
        DREAM_FLOWER_HOUSE("dream_flower_house", 65, 40),
        ECHO_GRAND_ARCHIVE("echo_grand_archive", 81, 40);

        private final String id;
        private final int size;
        private final int height;

        Kind(String id, int size, int height) {
            this.id = id;
            this.size = size;
            this.height = height;
        }

        static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return CRYSTAL_SONG_SHRINE;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] CRYSTAL_SHRINE_CLUSTER_NODES = {
                {-3, 4, 2}, {0, 5, 2}, {3, 6, 2}
        };
        private static final int[][] CRYSTAL_HALL_CLUSTER_NODES = {
                {10, 7, 0}, {7, 7, 7}, {0, 7, 10}, {-7, 7, 7},
                {-10, 7, 0}, {-7, 7, -7}, {0, 7, -10}, {7, 7, -7}
        };
        private static final int[][] CRYSTAL_SHRINE_GEODE_NODES = {
                {-5, 1, 0}, {5, 1, 0}, {-4, 1, -3},
                {4, 1, 3}, {-1, 1, 5}, {3, 1, 5}
        };
        private static final int[][] CRYSTAL_HALL_GEODE_NODES = {
                {-12, 1, -8}, {-12, 1, 0}, {-12, 1, 8},
                {12, 1, -4}, {12, 1, 0}, {12, 1, 8},
                {-8, 1, -12}, {0, 1, -12}, {8, 1, -12},
                {-8, 1, 12}, {0, 1, 12}, {8, 1, 12}
        };
        private Kind kind;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.LATE_REGION_SITE_PIECE.get(), x, 64, z,
                    kind.size, kind.height, kind.size,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.LATE_REGION_SITE_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("LateRegionKind"));
        }

        @Override
        protected int siteFootprint() {
            return kind.size;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearPlayableVolume(level, box);
            switch (this.kind) {
                case CRYSTAL_SONG_SHRINE -> buildCrystalShrine(level, random, box);
                case CRYSTAL_SONG_HALL -> buildCrystalHall(level, random, box);
                case DREAM_FLOWER_HOUSE -> buildDreamFlowerHouse(level, random, box);
                case ECHO_GRAND_ARCHIVE -> buildGrandArchive(level, random, box);
            }
            buildLateRegionFrame(level, random, box);
            addThemedEncounter(level, random, box);
            placeBudgetedResourceNodes(level, box);
        }

        /** 晶歌结构闹晶噪浮灵,总档案馆有迷途记录员游荡;梦花屋保持柔和,不加遭遇。 */
        private void addThemedEncounter(WorldGenLevel level, RandomSource random, BoundingBox box) {
            BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
            switch (kind) {
                case CRYSTAL_SONG_SHRINE -> placeStructureSpawner(level, random, box, 6, 1, 18,
                        ModEntities.CRYSTAL_NOISE_WISP.get(), song, chiseled);
                case CRYSTAL_SONG_HALL -> {
                    placeStructureSpawner(level, random, box, 34, 1, 14,
                            ModEntities.CRYSTAL_NOISE_WISP.get(), song, chiseled);

                    buildChimeEchoCourt(level, random, box, 36, 1, 42, Direction.SOUTH,
                            ModBlocks.SONGSTONE_TILES.get().defaultBlockState(),
                            ModBlocks.CRACKED_SONGSTONE_BRICKS.get().defaultBlockState(),
                            chiseled, ModLootTables.T3_PUZZLE);
                }
                case ECHO_GRAND_ARCHIVE -> {
                    placeStructureSpawner(level, random, box, 13, 1, 21,
                            ModEntities.LOST_RECORDER.get(),
                            ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                            ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());

                    buildFoldpagePairRoom(level, random, box, 60, 1, 62, Direction.SOUTH,
                            ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                            ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                            ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                            ModLootTables.T3_PUZZLE);
                }
                default -> { }
            }
        }

        private void buildLateRegionFrame(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case CRYSTAL_SONG_SHRINE -> {
                    BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
                    BlockState cracked = ModBlocks.CRACKED_SONGSTONE_BRICKS.get().defaultBlockState();
                    // 小祠必须用退台祠座:同群系的晶歌大厅已经是穹顶,重复会撞形。
                    buildSteppedZiggurat(level, box, c, 1, c, 8, 3, 3, 0,
                            song, cracked,
                            ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(),
                            ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());
                    buildCardinalGateway(level, box, c, 1, c, 10, Direction.SOUTH,
                            2, 7, song, chiseled);
                    buildStairCornice(level, box, c, 7, c, Footprint.square(8), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(10), 4, ModBlocks.SONGSTONE_BRICK_WALL.get().defaultBlockState(), cracked);
                }
                case CRYSTAL_SONG_HALL -> {
                    BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
                    BlockState cracked = ModBlocks.CRACKED_SONGSTONE_BRICKS.get().defaultBlockState();

                    // 鼓座半径 20 必须大于战斗半径 14,且穹顶不落地柱,否则侵入场地净空。
                    buildDomedRotunda(level, box, c, 1, c, 20, 14,
                            ModBlocks.SONGSTONE_TILES.get().defaultBlockState(),
                            song, cracked,
                            ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, c, 1, c, 20, direction,
                                3, 11, song, chiseled);
                    }
                    architecturalRing(level, box, c, 2, c, 16,
                            ModBlocks.SONGSTONE_TILES.get().defaultBlockState());
                    // 半径 21/22 必须在战斗半径 14 之外,否则侵入场地净空。
                    buildStairCornice(level, box, c, 15, c, Footprint.square(21), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 16, c, Footprint.square(21), song, chiseled);
                    buildParapet(level, box, c, 17, c, Footprint.square(22), 7, ModBlocks.SONGSTONE_BRICK_WALL.get().defaultBlockState(), cracked);
                }
                case DREAM_FLOWER_HOUSE -> {
                    // 柱基/柱身/柱头必须分三种材质,一根到顶的同色柱看不出基-身-头。
                    BlockState log = ModBlocks.WHISPERING_LOG.get().defaultBlockState();
                    BlockState lattice = ModBlocks.WHISPERING_LATTICE.get().defaultBlockState();
                    BlockState timber = ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState();
                    BlockState moss = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
                    BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
                    architecturalRing(level, box, c, 0, c, 29, moss);
                    architecturalRing(level, box, c, 0, c, 27, timber);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 27);
                        int z = c + (int) Math.round(Math.sin(angle) * 27);
                        int height = 9 + i % 3;
                        architecturalPillar(level, box, x, 1, z, 2, timber);
                        architecturalPillar(level, box, x, 3, z, height - 2, log);
                        this.placeBlock(level, lattice, x, height, z, box);
                        this.placeBlock(level, lamp, x, height + 1, z, box);
                    }
                    buildCardinalGateway(level, box, c, 1, c, 29, Direction.SOUTH,
                            3, 9, log, lattice);
                    buildParapet(level, box, c, 1, c, Footprint.square(28), 6, ModBlocks.WHISPERING_TIMBER_WALL.get().defaultBlockState(), moss);
                    buildCornice(level, box, c, 1, c, Footprint.square(26), log, lattice);
                }
                case ECHO_GRAND_ARCHIVE -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();

                    BlockState lamp = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c, Footprint.square(28), 17, 7, ModBlocks.ECHO_STRATA.get().defaultBlockState(), strata, chiseled, lamp);
                    buildTwinTowerFacade(level, box, c, 1, c - 34,
                            24, 5, 31, strata, chiseled, lamp);
                    BlockState archiveStairs = ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState();
                    BlockState archiveSlab = ModBlocks.ECHO_STRATA_BRICK_SLAB.get().defaultBlockState();
                    buildStairCornice(level, box, c, 12, c, Footprint.square(28), archiveStairs, archiveSlab, true);
                    buildCornice(level, box, c, 13, c, Footprint.square(28), chiseled, mosaic);
                    buildStairCornice(level, box, c, 18, c, Footprint.square(28), archiveStairs, archiveSlab, true);
                    buildCornice(level, box, c, 19, c, Footprint.square(28), chiseled, mosaic);
                    buildParapet(level, box, c, 20, c, Footprint.square(28), 7, ModBlocks.ECHO_STRATA_BRICK_WALL.get().defaultBlockState(), chiseled);
                    illuminateInterior(level, box, c, c, 17, 24, 24, 6, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                    for (int towerOffset : new int[]{-24, 24}) {
                        buildStairCornice(level, box, c + towerOffset, 32, c - 34, Footprint.square(4), archiveStairs, archiveSlab, true);
                        // baseY + height 必须 <= 声明高度 - 1:顶灯放在 baseY + height,超出会被足印裁掉。
                        buildSpireFinial(level, box, c + towerOffset, 33, c - 34,
                                4, 6, strata, chiseled, lamp);
                    }
                    for (int portalOffset : new int[]{-5, 0, 5}) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int y = 2; y <= 5; y++) {
                                this.placeBlock(level, Blocks.AIR.defaultBlockState(),
                                        c + portalOffset + dx, y, c - 34, box);
                            }
                        }
                    }
                    this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(),
                            c - 5, 2, c - 34, box);
                    this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED,
                                            Boolean.FALSE),
                            c, 2, c - 34, box);
                    this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(),
                            c + 5, 2, c - 34, box);
                    for (int[] corner : new int[][]{{c - 35, c - 35}, {c + 35, c - 35},
                            {c - 35, c + 35}, {c + 35, c + 35}}) {
                        architecturalPillar(level, box, corner[0], 1, corner[1], 28, strata);
                        architecturalPillar(level, box,
                                corner[0] + Integer.signum(c - corner[0]) * 2, 1,
                                corner[1] + Integer.signum(c - corner[1]) * 2, 20, chiseled);
                        this.placeBlock(level, ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState(),
                                corner[0], 29, corner[1], box);
                    }
                    buildRuneCauseway(level, box, c, 1, c, Direction.SOUTH,
                            32, 39, 3, strata, mosaic,
                            ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
                }
            }
            scatterArchitecturalDebris(level, random, box, c, 1, c,
                    Math.max(6, c - 6), Math.max(7, c - 2), Math.max(12, c),
                    ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
        }

        private void buildCrystalShrine(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState floor = ModBlocks.SONGSTONE_TILES.get().defaultBlockState();
            BlockState pillarState = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState crystal = ModBlocks.CRYSTAL_SONG_CLUSTER.get().defaultBlockState();
            addSiteRewardLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            disk(level, box, c, 0, c, 5, floor);
            for (int[] node : CRYSTAL_SHRINE_CLUSTER_NODES) {
                int x = c + node[0];
                int y = node[1];
                int z = c + node[2];
                pillar(level, box, x, 1, z, y - 1, pillarState);
                this.placeBlock(level, crystal, x, y, z, box);
            }
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), c - 3, 0, c - 2, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), c + 3, 0, c - 1, box);
            this.placeBlock(level, ModBlocks.RESONANT_MUSHROOM.get().defaultBlockState(), c - 3, 1, c - 2, box);
            this.placeBlock(level, ModBlocks.RESONANT_MUSHROOM.get().defaultBlockState(), c + 3, 1, c - 1, box);
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 1, c - 4, ModLootTables.T3_PUZZLE);

            BlockState cracked = ModBlocks.CRACKED_SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
            architecturalRing(level, box, c, 0, c, 5, chiseled);
            buildStairCornice(level, box, c, 0, c, Footprint.square(5), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(6), 4, ModBlocks.SONGSTONE_BRICK_WALL.get().defaultBlockState(), cracked);
            buildCornice(level, box, c, 1, c, Footprint.square(5), chiseled, cracked);

            // 四柱华盖 + 锥顶给小祠一个 12 格级垂直剪影,否则整座建筑集中在 y=0..6,远景是一张贴地圆饼。
            BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            for (int[] post : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                architecturalPillar(level, box, c + post[0], 1, c + post[1], 7, song);
                this.placeBlock(level, chiseled, c + post[0], 8, c + post[1], box);
            }
            // 华盖顶板:只压四角柱之间的边,中央留空让晶簇柱透上来。
            architecturalRing(level, box, c, 9, c, 4, song);
            buildStairCornice(level, box, c, 9, c, Footprint.square(4), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildSpireFinial(level, box, c, 10, c, 2, 4, song, chiseled,
                    ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());
        }

        private void buildCrystalHall(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState brick = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
            BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState tiles = ModBlocks.SONGSTONE_TILES.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState crystal = ModBlocks.CRYSTAL_SONG_CLUSTER.get().defaultBlockState();
            List<BlockPos> crystalPositions = new ArrayList<>();
            addSiteRewardLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);

            buildRibbedVaultHall(level, box, c, 2, c, Footprint.of(17, 21), 13, 6, tiles, song, brick, crystal);
            buildTwinTowerFacade(level, box, c, 1, 3,
                    18, 3, 24, song, brick, crystal);
            buildArcadedWing(level, box, 6, 2, c, Footprint.of(4, 14), 11, 4, tiles, song, cracked, crystal);
            buildArcadedWing(level, box, kind.size - 7, 2, c, Footprint.of(4, 14), 11, 4, tiles, song, cracked, crystal);
            buildBossApse(level, box, c, 2, c + 15,
                    Direction.SOUTH, 7, 12, tiles, song, brick, crystal);
            // 回廊只占外沿 3 格带,中央 14 格战斗半径必须保持开放天井。
            buildCourtyardCloister(level, box, c, 2, c, Footprint.square(21), 12, 3, tiles, song, brick, ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());

            platform(level, box, c - 12, 0, c - 12, 25, tiles);
            // 必须在晶簇柱之前清:晶簇柱在半径 10、y=1..6,正落在清空圆柱内,顺序颠倒会抹掉八根承托柱。
            clearArenaVolume(level, box, c, 1, c, 14, 1, 6);
            int pillarIndex = 0;
            for (int[] node : CRYSTAL_HALL_CLUSTER_NODES) {
                int x = c + node[0];
                int y = node[1];
                int z = c + node[2];
                pillar(level, box, x, 1, z, y - 1, pillarIndex++ % 2 == 0 ? brick : song);
                this.placeBlock(level, ModBlocks.CRYSTAL_SONG_CLUSTER.get().defaultBlockState(), x, y, z, box);
                crystalPositions.add(new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z)));
            }
            buildBossArenaMandala(level, box, c, 0, c, 14,
                    brick, song, cracked);

            buildArenaLightRing(level, box, c, 3, c, 16, 12, 10, song, ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());
            illuminateInterior(level, box, c, c, 20, 15, 19, 6, ModBlocks.SONG_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 17, c, Footprint.of(17, 21), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, c, 18, c, Footprint.of(17, 21), song, brick);
            buildParapet(level, box, c, 19, c, Footprint.of(17, 21), 8, ModBlocks.SONGSTONE_BRICK_WALL.get().defaultBlockState(), brick);
            platform(level, box, c - 4, 1, c - 4, 9, Blocks.CALCITE.defaultBlockState());
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            configureSpawner(level, box, c, 0, c, "crystal_songkeeper", 3,
                    UnknownEchoes.id("crystal_song_cluster"), crystalPositions);
            buildAccessibleChest(level, random, box, c - 6, 1, c, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c + 6, 1, c, ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void buildDreamFlowerHouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState plank = ModBlocks.WHISPERING_PLANKS.get().defaultBlockState();
            BlockState log = ModBlocks.WHISPERING_LOG.get().defaultBlockState();
            BlockState lattice = ModBlocks.WHISPERING_LATTICE.get().defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            BlockState glass = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState path = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            addSiteRewardLoop(level, random, box, c, ModLootTables.T2_RUIN);

            disk(level, box, c, 0, c, 30, moss);
            buildRibbedVaultHall(level, box, c, 1, c + 2, Footprint.of(16, 24), 14, 6, plank, lattice, log, glass);
            buildTwinTowerFacade(level, box, c, 1, c - 25,
                    22, 4, 28, log, lattice, glass);
            buildArcadedWing(level, box, c - 24, 1, c + 1, Footprint.of(7, 17), 12, 4, moss, log, lattice, glass);
            buildArcadedWing(level, box, c + 24, 1, c + 1, Footprint.of(7, 17), 12, 4, moss, log, lattice, glass);
            buildBossApse(level, box, c, 1, c + 23,
                    Direction.SOUTH, 9, 13, moss, log, lattice, glass);
            for (int[] tower : new int[][]{{c - 24, c - 14}, {c + 24, c - 14},
                    {c - 24, c + 17}, {c + 24, c + 17}}) {
                buildTieredTower(level, box, tower[0], 1, tower[1],
                        4, 26, log, lattice, glass);
            }

            buildRuneCauseway(level, box, c, 0, c, Direction.NORTH,
                    17, 30, 3, plank, path, lattice);
            // 肋拱厅的柱列会插进 16 格战斗半径,建筑摆完后统一清出净空。
            clearArenaVolume(level, box, c, 1, c, 16, 1, 6);
            buildBossArenaMandala(level, box, c, 0, c, 16,
                    lattice, plank, path);
            buildArenaLightRing(level, box, c, 3, c, 18, 12, 10, log, glass);
            illuminateInterior(level, box, c, c + 2, 20, 15, 22, 6, glass);
            buildCornice(level, box, c, 16, c + 2, Footprint.of(16, 24), log, lattice);
            for (int[] flower : new int[][]{{-12, -8}, {-9, 10}, {11, -9}, {13, 8},
                    {-23, 5}, {23, 5}, {-20, 16}, {20, 16}}) {
                this.placeBlock(level, moss, c + flower[0], 0, c + flower[1], box);
                this.placeBlock(level, ModBlocks.DREAM_FLOWER.get().defaultBlockState(),
                        c + flower[0], 1, c + flower[1], box);
            }
            this.placeBlock(level, moss, c - 15, 0, c + 18, box);
            this.placeBlock(level, ModBlocks.DREAM_MIST_VINE.get().defaultBlockState(),
                    c - 15, 1, c + 18, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c + 12, box);
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 1, c, box);
            configureSpawner(level, box, c, 1, c, "dream_bloom_keeper", 2, null, null);
            buildAccessibleChest(level, random, box, c - 12, 1, c + 18, ModLootTables.T2_RUIN);
            buildTrueSightVaultNiche(level, random, box, c + 23, 1, c + 18, Direction.WEST);
        }

        private void buildGrandArchive(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState cliff = ModBlocks.ECHO_STRATA.get().defaultBlockState();
            BlockState brick = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
            buildSiteGuidedEntrance(level, box, c, cliff);
            buildArchiveCliffGate(level, box, c, cliff, brick);
            buildArchiveMainHall(level, box, c, cliff, brick);
            buildArchiveAbilityWings(level, random, box, c, cliff, brick);
            buildArchiveTerminalHall(level, random, box, c, cliff, brick);
        }

        private void buildArchiveCliffGate(WorldGenLevel level, BoundingBox box, int c,
                                           BlockState cliff, BlockState brick) {
            for (int y = 1; y <= 31; y++) {
                pillar(level, box, c - 24, y, c - 34, 1, cliff);
                pillar(level, box, c + 24, y, c - 34, 1, cliff);
                if (y >= 24) {
                    for (int x = c - 23; x <= c + 23; x++) {
                        this.placeBlock(level, y % 4 == 0 ? brick : cliff, x, y, c - 34, box);
                    }
                }
            }
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c - 5, 2, c - 34, box);

            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                    c, 2, c - 34, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c + 5, 2, c - 34, box);
        }

        private void buildArchiveMainHall(WorldGenLevel level, BoundingBox box, int c,
                                          BlockState cliff, BlockState brick) {
            platform(level, box, c - 32, 0, c - 32, 65, cliff);
            BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
            for (int offset = -28; offset <= 28; offset++) {
                this.placeBlock(level, mosaic, c + offset, 0, c, box);
                this.placeBlock(level, mosaic, c, 0, c + offset, box);
            }
            for (int y = 1; y <= 18; y++) {
                BlockState wallState = y % 6 == 0 ? mosaic : y % 3 == 0 ? brick : cliff;
                hollowRect(level, box, c - 32, y, c - 32, 65, 65, wallState);
            }
            for (int i = -24; i <= 24; i += 6) {
                pillar(level, box, c + i, 1, c - 26, 9, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                pillar(level, box, c + i, 1, c + 26, 9, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
        }

        private void buildArchiveAbilityWings(WorldGenLevel level, RandomSource random, BoundingBox box, int c,
                                              BlockState cliff, BlockState brick) {
            platform(level, box, c - 33, 1, c - 8, 25, cliff);
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c - 29, 2, c, box);
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c - 32, 2, c, box);
            this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c - 20, 2, c - 5, box);
            buildSiteBranchReward(level, random, box, c - 20, 2, c + 6, ModLootTables.T3_PUZZLE);

            platform(level, box, c + 9, 1, c - 8, 25, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                    c + 15, 2, c, box);
            for (int z = c - 6; z <= c + 6; z += 6) {
                this.placeBlock(level, ModBlocks.TIDE_PILLAR.get().defaultBlockState(), c + 24, 2, z, box);
            }
            this.placeBlock(level, ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState(), c + 18, 2, c, box);
            buildSiteBranchReward(level, random, box, c + 24, 2, c + 8, ModLootTables.T4_REGION_CORE);

            platform(level, box, c - 12, 1, c + 10, 25, brick);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 2, c + 16, box);
            this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), c - 5, 2, c + 28, box);
            this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), c + 5, 2, c + 28, box);
            buildSiteHiddenReward(level, random, box, c, 2, c + 30);
        }

        private void buildArchiveTerminalHall(WorldGenLevel level, RandomSource random, BoundingBox box, int c,
                                              BlockState cliff, BlockState brick) {
            BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
            platform(level, box, c - 12, 1, c - 12, 25, mosaic);
            for (int y = 2; y <= 18; y++) {
                hollowRect(level, box, c - 12, y, c - 12, 25, 25,
                        y % 3 == 0 ? mosaic : y % 2 == 0 ? brick : cliff);
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 2, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c + 4, 2, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(), c - 4, 2, c, box);

            BlockState lamp = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
            buildBossArenaMandala(level, box, c, 1, c + 5, 11,
                    mosaic, brick, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            buildBossArenaCrown(level, box, c, 2, c + 5, 9, 14, cliff, mosaic, lamp);
            illuminateInterior(level, box, c, c, 16, 10, 10, 4, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState());
            // 25x25 的空心方筒必须补挑檐与腰线,否则四壁从 y=2 直上 18 没有水平分段。
            BlockState archiveStairs = ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState();
            BlockState archiveSlab = ModBlocks.ECHO_STRATA_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, 7, c, Footprint.square(12), archiveStairs, archiveSlab, true);
            buildCornice(level, box, c, 8, c, Footprint.square(12), brick, mosaic);
            buildStairCornice(level, box, c, 13, c, Footprint.square(12), archiveStairs, archiveSlab, true);
            buildCornice(level, box, c, 14, c, Footprint.square(12), brick, mosaic);
            buildParapet(level, box, c, 19, c, Footprint.square(12), 6, ModBlocks.ECHO_STRATA_BRICK_WALL.get().defaultBlockState(), mosaic);
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 2, c + 5, box);
            configureSpawner(level, box, c, 2, c + 5, "lost_recorder_chief", 2, null, null);
            buildSiteBranchReward(level, random, box, c - 7, 2, c - 7, ModLootTables.T5_MINIBOSS_REPEAT);
            buildAccessibleChest(level, random, box, c + 7, 2, c + 7, ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void placeBudgetedResourceNodes(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case CRYSTAL_SONG_SHRINE -> {
                    for (int[] node : CRYSTAL_SHRINE_GEODE_NODES) {
                        this.placeBlock(level, ModBlocks.RESONANT_GEODE.get().defaultBlockState(),
                                c + node[0], node[1], c + node[2], box);
                    }
                }
                case CRYSTAL_SONG_HALL -> {
                    for (int[] node : CRYSTAL_HALL_GEODE_NODES) {
                        this.placeBlock(level, ModBlocks.RESONANT_GEODE.get().defaultBlockState(),
                                c + node[0], node[1], c + node[2], box);
                    }
                }
                default -> { }
            }
        }

        private void clearPlayableVolume(WorldGenLevel level, BoundingBox box) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int y = 1; y < kind.height; y++) {
                for (int x = 1; x < kind.size - 1; x++) {
                    for (int z = 1; z < kind.size - 1; z++) {
                        this.placeBlock(level, air, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void buildSiteGuidedEntrance(WorldGenLevel level, BoundingBox box, int c,
                                               BlockState state) {
            int start = Math.max(1, c - 39);
            int end = Math.min(kind.size - 2, c - 33);
            if (start > end) {
                start = Math.min(kind.size - 2, c + 4);
                end = kind.size - 2;
            }
            for (int z = start; z <= end; z++) {
                for (int x = c - 3; x <= c + 3; x++) {
                    this.placeBlock(level, state, x, 1, z, box);
                }
            }
        }

        private void configureSpawner(WorldGenLevel level, BoundingBox box, int x, int y, int z, String entityId,
                                      int spawnYOffset, ResourceLocation resetBlockId,
                                      List<BlockPos> resetPositions) {
            BlockPos spawnerPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id(entityId), spawnYOffset);
                if (resetBlockId != null && resetPositions != null && !resetPositions.isEmpty()) {
                    spawner.setResetBlocks(resetBlockId, resetPositions);
                }
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                            int height, BlockState state) {
            for (int i = 0; i < height; i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0,
                              int size, BlockState state) {
            for (int x = x0; x < x0 + size; x++) {
                for (int z = z0; z < z0 + size; z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz,
                          int radius, BlockState state) {
            int r2 = radius * radius;
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    if (dx * dx + dz * dz <= r2) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private void hollowRect(WorldGenLevel level, BoundingBox box, int x0, int y, int z0,
                                int width, int depth, BlockState state) {
            for (int x = x0; x < x0 + width; x++) {
                for (int z = z0; z < z0 + depth; z++) {
                    boolean wall = x == x0 || z == z0 || x == x0 + width - 1 || z == z0 + depth - 1;
                    if (wall) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("LateRegionKind", this.kind.id);
        }
    }
}
