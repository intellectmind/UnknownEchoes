package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.deep.DeepCalibrationNodeBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 深层回响三群系地标与校准大厅的代码生成结构。 */
public class DeepRegionStructure extends SinglePieceStructure {
    public static final MapCodec<DeepRegionStructure> FOLDED_ARCHIVE_CODEC = codec(Kind.FOLDED_ARCHIVE);
    public static final MapCodec<DeepRegionStructure> INVERTED_CHASM_BRIDGE_CODEC =
            codec(Kind.INVERTED_CHASM_BRIDGE);
    public static final MapCodec<DeepRegionStructure> SILENT_ABYSS_ANCHOR_CODEC =
            codec(Kind.SILENT_ABYSS_ANCHOR);
    public static final MapCodec<DeepRegionStructure> CALIBRATION_HALL_CODEC =
            codec(Kind.CALIBRATION_HALL);
    public static final MapCodec<DeepRegionStructure> VOID_VEIN_CHAMBER_CODEC =
            codec(Kind.VOID_VEIN_CHAMBER);
    public static final MapCodec<DeepRegionStructure> WORLD_RESONANCE_HALL_CODEC =
            codec(Kind.WORLD_RESONANCE_HALL);
    public static final MapCodec<DeepRegionStructure> FOLDED_WAYSTATION_CODEC =
            codec(Kind.FOLDED_WAYSTATION);
    public static final MapCodec<DeepRegionStructure> GRAVITY_SHARD_SPIRE_CODEC =
            codec(Kind.GRAVITY_SHARD_SPIRE);
    public static final MapCodec<DeepRegionStructure> HUSHED_REFUGE_CODEC =
            codec(Kind.HUSHED_REFUGE);

    private final Kind kind;

    private static MapCodec<DeepRegionStructure> codec(Kind kind) {
        return simpleCodec(settings -> new DeepRegionStructure(settings, kind));
    }

    private DeepRegionStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.width, kind.depth, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case FOLDED_ARCHIVE -> ModStructures.FOLDED_ARCHIVE.get();
            case INVERTED_CHASM_BRIDGE -> ModStructures.INVERTED_CHASM_BRIDGE.get();
            case SILENT_ABYSS_ANCHOR -> ModStructures.SILENT_ABYSS_ANCHOR.get();
            case CALIBRATION_HALL -> ModStructures.CALIBRATION_HALL.get();
            case VOID_VEIN_CHAMBER -> ModStructures.VOID_VEIN_CHAMBER.get();
            case WORLD_RESONANCE_HALL -> ModStructures.WORLD_RESONANCE_HALL.get();
            case FOLDED_WAYSTATION -> ModStructures.FOLDED_WAYSTATION.get();
            case GRAVITY_SHARD_SPIRE -> ModStructures.GRAVITY_SHARD_SPIRE.get();
            case HUSHED_REFUGE -> ModStructures.HUSHED_REFUGE.get();
        };
    }

    public enum Kind {
        FOLDED_ARCHIVE("folded_archive", 19, 12, 19),
        INVERTED_CHASM_BRIDGE("inverted_chasm_bridge", 23, 18, 15),
        SILENT_ABYSS_ANCHOR("silent_abyss_anchor", 17, 10, 17),
        CALIBRATION_HALL("calibration_hall", 45, 18, 45),
        VOID_VEIN_CHAMBER("void_vein_chamber", 49, 20, 49),
        WORLD_RESONANCE_HALL("world_resonance_hall", 49, 23, 49),
        FOLDED_WAYSTATION("folded_waystation", 15, 10, 15),
        GRAVITY_SHARD_SPIRE("gravity_shard_spire", 15, 22, 15),
        HUSHED_REFUGE("hushed_refuge", 17, 11, 17);

        private final String id;
        private final int width;
        private final int height;
        private final int depth;

        Kind(String id, int width, int height, int depth) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        private static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return FOLDED_ARCHIVE;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private Kind kind;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.DEEP_REGION_SITE_PIECE.get(), x, 64, z,
                    kind.width, kind.height, kind.depth,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.DEEP_REGION_SITE_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("DeepRegionKind"));
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearVolume(level, box);
            switch (kind) {
                case FOLDED_ARCHIVE -> buildFoldedArchive(level, box);
                case INVERTED_CHASM_BRIDGE -> buildInvertedBridge(level, box);
                case SILENT_ABYSS_ANCHOR -> buildSilentAnchor(level, box);
                case CALIBRATION_HALL -> buildCalibrationHall(level, box);
                case VOID_VEIN_CHAMBER -> buildVoidVeinChamber(level, box);
                case WORLD_RESONANCE_HALL -> buildWorldResonanceHall(level, box);
                case FOLDED_WAYSTATION -> buildFoldedWaystation(level, box);
                case GRAVITY_SHARD_SPIRE -> buildGravityShardSpire(level, box);
                case HUSHED_REFUGE -> buildHushedRefuge(level, box);
            }
            placeExplorationLoot(level, random, box);
        }

        private void placeExplorationLoot(WorldGenLevel level, RandomSource random, BoundingBox box) {
            switch (kind) {
                case FOLDED_ARCHIVE -> {
                    buildAccessibleChest(level, random, box, 4, 1, 6, ModLootTables.FOLDED_ARCHIVE_CHEST);
                    buildAccessibleChest(level, random, box, 14, 1, 12, ModLootTables.FOLDED_ARCHIVE_CHEST);
                }
                case INVERTED_CHASM_BRIDGE -> {
                    buildAccessibleChest(level, random, box, 3, 1, 7, ModLootTables.INVERTED_CHASM_CHEST);
                    buildAccessibleChest(level, random, box, 19, 1, 7, ModLootTables.INVERTED_CHASM_CHEST);
                }
                case SILENT_ABYSS_ANCHOR ->
                        buildAccessibleChest(level, random, box, 8, 1, 3, ModLootTables.SILENT_ABYSS_CHEST);
                case CALIBRATION_HALL -> {
                    buildAccessibleChest(level, random, box, 13, 1, 14, ModLootTables.CALIBRATION_HALL_CHEST);
                    buildAccessibleChest(level, random, box, 31, 1, 30, ModLootTables.CALIBRATION_HALL_CHEST);
                }
                case VOID_VEIN_CHAMBER -> {
                    buildAccessibleChest(level, random, box, 12, 1, 12, ModLootTables.VOID_VEIN_CHEST);
                    buildAccessibleChest(level, random, box, 36, 1, 36, ModLootTables.VOID_VEIN_CHEST);
                }
                case WORLD_RESONANCE_HALL -> {
                    buildAccessibleChest(level, random, box, 10, 1, 10,
                            ModLootTables.WORLD_RESONANCE_HALL_CHEST);
                    buildAccessibleChest(level, random, box, 38, 1, 10,
                            ModLootTables.WORLD_RESONANCE_HALL_CHEST);
                }
                case FOLDED_WAYSTATION ->
                        buildAccessibleChest(level, random, box, 3, 1, 7,
                                ModLootTables.FOLDED_WAYSTATION_CHEST);
                case GRAVITY_SHARD_SPIRE ->
                        buildAccessibleChest(level, random, box, 3, 1, 3,
                                ModLootTables.GRAVITY_SHARD_SPIRE_CHEST);
                case HUSHED_REFUGE ->
                        buildAccessibleChest(level, random, box, 8, 1, 8,
                                ModLootTables.HUSHED_REFUGE_CHEST);
            }
        }

        private void buildFoldedArchive(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState accent = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            platform(level, box, 1, 0, 1, 17, 17, floor);
            for (int z : new int[]{3, 9, 15}) {
                for (int x = 2; x <= 16; x++) {
                    if (x < 8 || x > 10) {
                        placeBlock(level, wall, x, 1, z, box);
                        placeBlock(level, wall, x, 2, z, box);
                    }
                    placeBlock(level, x % 3 == 0 ? accent : wall, x, 5, z, box);
                }
                pillar(level, box, 2, 1, z, 5, accent);
                pillar(level, box, 16, 1, z, 5, accent);
            }
            for (int z = 2; z <= 16; z++) {
                placeBlock(level, z % 4 == 0 ? accent : wall, 9, 0, z, box);
            }
            pillar(level, box, 9, 1, 9, 7, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        }

        private void buildInvertedBridge(WorldGenLevel level, BoundingBox box) {
            BlockState chasm = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState bridge = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState light = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            platform(level, box, 1, 0, 3, 7, 9, chasm);
            platform(level, box, 15, 0, 3, 7, 9, chasm);
            for (int x = 7; x <= 15; x++) {
                if (x != 10 && x != 13) {
                    for (int z = 6; z <= 8; z++) {
                        placeBlock(level, bridge, x, 2, z, box);
                    }
                }
            }
            for (int x : new int[]{3, 7, 11, 15, 19}) {
                for (int y = 15; y >= 8 - Math.abs(11 - x) / 2; y--) {
                    placeBlock(level, y == 8 ? light : chasm, x, y, 7, box);
                }
            }
            for (int y = 1; y <= 4; y++) {
                placeBlock(level, bridge, 6, y, 7, box);
                placeBlock(level, bridge, 16, y, 7, box);
            }
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(), 11, 3, 7, box);
            for (int y = 3; y <= 5; y++) {
                for (int z = 6; z <= 8; z++) {
                    placeBlock(level, ModBlocks.GRAVITY_FOLD_DOOR.get().defaultBlockState(),
                            15, y, z, box);
                }
            }
        }

        private void buildSilentAnchor(WorldGenLevel level, BoundingBox box) {
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState sealed = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            platform(level, box, 1, 0, 1, 15, 15, moss);
            int center = 8;
            for (int x = 2; x <= 14; x++) {
                for (int z = 2; z <= 14; z++) {
                    int distance = Math.max(Math.abs(x - center), Math.abs(z - center));
                    if (distance == 5) {
                        placeBlock(level, (x + z) % 3 == 0 ? rune : sealed, x, 1, z, box);
                    }
                }
            }
            for (int[] point : new int[][]{{3, 3}, {13, 3}, {3, 13}, {13, 13}}) {
                pillar(level, box, point[0], 1, point[1], 5, sealed);
                placeBlock(level, rune, point[0], 6, point[1], box);
            }
            placeBlock(level, sealed, center, 1, center, box);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 2, center, box);
            for (int[] anchor : new int[][]{{center, 3}, {3, 12}, {13, 12}}) {
                placeBlock(level, ModBlocks.SPACE_ANCHOR.get().defaultBlockState(),
                        anchor[0], 2, anchor[1], box);
            }
        }

        private void buildCalibrationHall(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int center = 22;

            for (int x = 1; x <= 43; x++) {
                for (int z = 1; z <= 43; z++) {
                    int dx = x - center;
                    int dz = z - center;
                    double distance = Math.sqrt(square(dx) + square(dz));
                    if (distance <= 21.0D) {
                        BlockState state = isArenaRing(distance, 5.0D, 11.0D, 17.0D)
                                || Math.abs(dx) <= 1 || Math.abs(dz) <= 1
                                || Math.abs(Math.abs(dx) - Math.abs(dz)) == 0 ? rune : floor;
                        placeBlock(level, state, x, 0, z, box);
                    }
                    if (distance >= 19.9D && distance <= 21.0D
                            && !isArenaEntrance(dx, dz, 20, 3)) {
                        int wallHeight = 7 + Math.floorMod(x + z, 3);
                        for (int y = 1; y <= wallHeight; y++) {
                            BlockState state = y == 4 || y == wallHeight
                                    || Math.floorMod(x + z + y, 11) == 0
                                    ? rune : wall;
                            placeBlock(level, state, x, y, z, box);
                        }
                    }
                }
            }

            for (int[] point : new int[][]{
                    {22, 5}, {34, 10}, {39, 22}, {34, 34},
                    {22, 39}, {10, 34}, {5, 22}, {10, 10}}) {
                buildCalibrationPylon(level, box, point[0], point[1], 11, wall, rune, lamp);
            }
            buildCalibrationArches(level, box, center, 18, 10, wall, rune, lamp);

            buildCalibrationWing(level, box, center, Direction.NORTH,
                    ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState(), wall, rune, lamp);
            buildCalibrationWing(level, box, center, Direction.WEST,
                    ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState(), wall, rune, lamp);
            buildCalibrationWing(level, box, center, Direction.EAST,
                    ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(), wall, rune, lamp);

            for (int x = center - 2; x <= center + 2; x++) {
                for (int z = center - 2; z <= center + 2; z++) {
                    placeBlock(level, rune, x, 0, z, box);
                }
            }
            placeCalibrationNode(level, box, center, 1, 7,
                    DeepCalibrationNodeBlock.Calibration.WIND);
            placeCalibrationNode(level, box, 7, 1, center,
                    DeepCalibrationNodeBlock.Calibration.TIDE);
            placeCalibrationNode(level, box, 37, 1, center,
                    DeepCalibrationNodeBlock.Calibration.TRUE_SIGHT);
            placeBlock(level, ModBlocks.GRAVITY_CORE_ALTAR.get().defaultBlockState(),
                    center, 1, center, box);
        }

        private void buildVoidVeinChamber(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState crystal = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            int center = 24;

            for (int x = 1; x <= 47; x++) {
                for (int z = 1; z <= 47; z++) {
                    int dx = x - center;
                    int dz = z - center;
                    if (isVoidArenaFloor(dx, dz)) {
                        BlockState state = isVoidFissure(dx, dz)
                                ? (Math.floorMod(x * 11 + z * 7, 5) == 0 ? crystal : vein)
                                : floor;
                        placeBlock(level, state, x, 0, z, box);
                    }
                    if (isVoidArenaBoundary(dx, dz)
                            && !isArenaEntrance(dx, dz, 22, 4)) {
                        int wallHeight = 6 + Math.floorMod(x * 17 + z * 7, 5);
                        for (int y = 1; y <= wallHeight; y++) {
                            BlockState state = y == wallHeight || y == 3
                                    || Math.floorMod(x * 3 + z + y, 13) == 0
                                    ? vein : wall;
                            placeBlock(level, state, x, y, z, box);
                        }
                    }
                }
            }

            for (int[] spine : new int[][]{
                    {24, 2, 16}, {37, 5, 12}, {46, 17, 15}, {45, 33, 10},
                    {35, 44, 14}, {18, 46, 12}, {5, 38, 15}, {3, 21, 11}, {12, 6, 13}}) {
                buildRiftSpine(level, box, spine[0], spine[1], spine[2], center,
                        wall, vein, crystal);
            }

            buildStabilizerCradle(level, box, center, 11, 3, 0, center,
                    floor, vein, crystal);
            buildStabilizerCradle(level, box, 13, 31, 2, 2, center,
                    floor, vein, crystal);
            buildStabilizerCradle(level, box, 35, 31, 2, -2, center,
                    floor, vein, crystal);

            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 4) {
                        BlockState state = Math.abs(dx) + Math.abs(dz) == 4
                                || dx == 0 || dz == 0 ? crystal : vein;
                        placeBlock(level, state, center + dx, 0, center + dz, box);
                    }
                }
            }
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), center, 1, 11, box);
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), 13, 1, 31, box);
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), 35, 1, 31, box);
            placeBlock(level, ModBlocks.VOID_CORE_ALTAR.get().defaultBlockState(), center, 1, center, box);
        }

        private void buildWorldResonanceHall(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState innerFloor = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int center = 24;

            for (int x = 1; x <= 47; x++) {
                for (int z = 1; z <= 47; z++) {
                    double distance = Math.sqrt(square(x - center) + square(z - center));
                    if (distance <= 23.0D) {
                        BlockState state = isFloorRing(distance) ? rune : innerFloor;
                        placeBlock(level, state, x, 0, z, box);
                    }
                    if (distance >= 21.7D && distance <= 23.0D
                            && !isCardinalEntrance(x - center, z - center)) {
                        for (int y = 1; y <= 8; y++) {
                            BlockState state = y == 4 || y == 8 || (x + z + y) % 13 == 0
                                    ? rune : wall;
                            placeBlock(level, state, x, y, z, box);
                        }
                    }
                }
            }

            for (int[] point : new int[][]{
                    {24, 3}, {39, 9}, {45, 24}, {39, 39},
                    {24, 45}, {9, 39}, {3, 24}, {9, 9}}) {
                pillar(level, box, point[0], 1, point[1], 16, floor);
                placeBlock(level, rune, point[0], 17, point[1], box);
                placeBlock(level, lamp, point[0], 18, point[1], box);
            }

            for (int offset = -16; offset <= 16; offset++) {
                double normalized = offset / 16.0D;
                int archY = 14 + (int) Math.round(5.0D * Math.sqrt(1.0D - normalized * normalized));
                BlockState arch = Math.floorMod(offset, 4) == 0 ? lamp : rune;
                placeBlock(level, arch, center + offset, archY, center, box);
                placeBlock(level, arch, center, archY, center + offset, box);
            }

            trace(level, box, rune, center, center, 0.0D);
            trace(level, box, rune, center, center, 72.0D);
            trace(level, box, rune, center, center, 144.0D);
            trace(level, box, rune, center, center, 216.0D);
            trace(level, box, rune, center, center, 288.0D);

            buildEchoPylon(level, box, 24, 7,
                    ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState(), rune, lamp);
            buildEchoPylon(level, box, 40, 19,
                    ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState(), rune, lamp);
            buildEchoPylon(level, box, 34, 38,
                    ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(), rune, lamp);
            buildEchoPylon(level, box, 14, 38, floor, rune, lamp);
            buildEchoPylon(level, box, 8, 19,
                    ModBlocks.SEALED_STONE.get().defaultBlockState(), rune, lamp);

            for (int x = center - 2; x <= center + 2; x++) {
                for (int z = center - 2; z <= center + 2; z++) {
                    placeBlock(level, rune, x, 0, z, box);
                }
            }
            placeBlock(level, ModBlocks.WORLD_RESONANCE_ALTAR.get().defaultBlockState(),
                    center, 1, center, box);
            placeBlock(level, ModBlocks.ENDING_RECORD_TABLE.get().defaultBlockState(),
                    center, 1, center + 16, box);
        }

        private void buildFoldedWaystation(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            platform(level, box, 1, 0, 1, 13, 13, floor);

            for (int z : new int[]{2, 7, 12}) {
                pillar(level, box, 2, 1, z, 6, wall);
                pillar(level, box, 12, 1, z, 6, wall);
                for (int x = 2; x <= 12; x++) {
                    placeBlock(level, x % 3 == 0 ? lamp : rune, x, 6, z, box);
                }
            }
            for (int offset = -4; offset <= 4; offset++) {
                if (Math.abs(offset) == 1) {
                    continue;
                }
                placeBlock(level, rune, 7 + offset, 1, 7 + offset, box);
                placeBlock(level, wall, 7 + offset, 2, 7 - offset, box);
            }
            for (int[] point : new int[][]{{3, 3}, {11, 3}, {3, 11}, {11, 11}}) {
                pillar(level, box, point[0], 1, point[1], 4, wall);
                placeBlock(level, lamp, point[0], 5, point[1], box);
            }
            pillar(level, box, 7, 1, 7, 4, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            placeBlock(level, lamp, 7, 5, 7, box);
        }

        private void buildGravityShardSpire(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState frame = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int center = 7;
            platform(level, box, 1, 0, 1, 13, 13, floor);

            for (int[] point : new int[][]{{2, 2}, {12, 2}, {2, 12}, {12, 12}}) {
                pillar(level, box, point[0], 1, point[1], 7, frame);
                placeBlock(level, rune, point[0], 8, point[1], box);
                placeBlock(level, lamp, point[0], 9, point[1], box);
            }
            for (int y = 4; y <= 18; y++) {
                placeBlock(level, y % 4 == 0 ? lamp : rune, center, y, center, box);
            }
            for (int[] ring : new int[][]{{4, 6}, {3, 11}, {2, 15}}) {
                int radius = ring[0];
                int y = ring[1];
                for (int offset = -radius; offset <= radius; offset++) {
                    if (Math.abs(offset) == radius || offset % 2 == 0) {
                        placeBlock(level, rune, center + offset, y, center - radius, box);
                        placeBlock(level, rune, center + offset, y, center + radius, box);
                        placeBlock(level, rune, center - radius, y, center + offset, box);
                        placeBlock(level, rune, center + radius, y, center + offset, box);
                    }
                }
            }
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 1, center, box);
            placeBlock(level, lamp, center, 19, center, box);
        }

        private void buildHushedRefuge(WorldGenLevel level, BoundingBox box) {
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState sealed = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            platform(level, box, 1, 0, 1, 15, 15, moss);

            for (int edge = 1; edge <= 15; edge++) {
                boolean opening = Math.abs(edge - 8) <= 1;
                for (int y = 1; y <= 4; y++) {
                    if (!opening && (edge + y) % 7 != 0) {
                        placeBlock(level, y == 4 ? rune : wall, edge, y, 1, box);
                        placeBlock(level, y == 4 ? rune : wall, edge, y, 15, box);
                        placeBlock(level, y == 4 ? rune : wall, 1, y, edge, box);
                        placeBlock(level, y == 4 ? rune : wall, 15, y, edge, box);
                    }
                }
            }
            for (int[] point : new int[][]{{4, 4}, {12, 4}, {4, 12}, {12, 12}}) {
                pillar(level, box, point[0], 1, point[1], 5, sealed);
                placeBlock(level, lamp, point[0], 6, point[1], box);
            }
            for (int offset = 3; offset <= 13; offset++) {
                if (offset % 3 != 0) {
                    placeBlock(level, rune, offset, 6, 8, box);
                    placeBlock(level, rune, 8, 6, offset, box);
                }
            }
            for (int x = 6; x <= 10; x++) {
                for (int z = 6; z <= 10; z++) {
                    if (x == 6 || x == 10 || z == 6 || z == 10) {
                        placeBlock(level, sealed, x, 1, z, box);
                    }
                }
            }
            placeBlock(level, rune, 8, 1, 8, box);
        }

        private void buildEchoPylon(WorldGenLevel level, BoundingBox box, int x, int z,
                                    BlockState body, BlockState rune, BlockState lamp) {
            for (int xx = x - 1; xx <= x + 1; xx++) {
                for (int zz = z - 1; zz <= z + 1; zz++) {
                    placeBlock(level, rune, xx, 1, zz, box);
                }
            }
            pillar(level, box, x, 2, z, 7, body);
            placeBlock(level, rune, x, 9, z, box);
            placeBlock(level, lamp, x, 10, z, box);
        }

        private void trace(WorldGenLevel level, BoundingBox box, BlockState state,
                           int centerX, int centerZ, double angleDegrees) {
            double radians = Math.toRadians(angleDegrees);
            for (int radius = 4; radius <= 19; radius++) {
                int x = centerX + (int) Math.round(Math.cos(radians) * radius);
                int z = centerZ + (int) Math.round(Math.sin(radians) * radius);
                placeBlock(level, state, x, 0, z, box);
            }
        }

        private static boolean isFloorRing(double distance) {
            return Math.abs(distance - 6.0D) < 0.42D
                    || Math.abs(distance - 12.0D) < 0.42D
                    || Math.abs(distance - 18.0D) < 0.42D;
        }

        private static boolean isCardinalEntrance(int dx, int dz) {
            return (Math.abs(dx) <= 3 && Math.abs(dz) >= 21)
                    || (Math.abs(dz) <= 3 && Math.abs(dx) >= 21);
        }

        private static boolean isArenaRing(double distance, double... radii) {
            for (double radius : radii) {
                if (Math.abs(distance - radius) < 0.46D) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isArenaEntrance(int dx, int dz, int edge, int halfWidth) {
            return Math.abs(dx) <= halfWidth && Math.abs(dz) >= edge
                    || Math.abs(dz) <= halfWidth && Math.abs(dx) >= edge;
        }

        private static boolean isVoidArenaFloor(int dx, int dz) {
            return Math.abs(dx) <= 23 && Math.abs(dz) <= 23
                    && Math.abs(dx) + Math.abs(dz) <= 34;
        }

        private static boolean isVoidArenaBoundary(int dx, int dz) {
            if (!isVoidArenaFloor(dx, dz)) {
                return false;
            }
            return !isVoidArenaFloor(dx + 1, dz) || !isVoidArenaFloor(dx - 1, dz)
                    || !isVoidArenaFloor(dx, dz + 1) || !isVoidArenaFloor(dx, dz - 1);
        }

        private static boolean isVoidFissure(int dx, int dz) {
            return isNearSegment(dx, dz, 0, 0, 0, -22, 0.7D)
                    || isNearSegment(dx, dz, 0, 0, -18, 11, 0.7D)
                    || isNearSegment(dx, dz, 0, 0, 18, 11, 0.7D)
                    || isNearSegment(dx, dz, 0, -8, -8, -15, 0.55D)
                    || isNearSegment(dx, dz, -7, 4, -17, -1, 0.55D)
                    || isNearSegment(dx, dz, 7, 4, 17, -1, 0.55D);
        }

        private static boolean isNearSegment(int x, int z, int startX, int startZ,
                                             int endX, int endZ, double radius) {
            double segmentX = endX - startX;
            double segmentZ = endZ - startZ;
            double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
            double projection = ((x - startX) * segmentX + (z - startZ) * segmentZ)
                    / lengthSquared;
            projection = Math.max(0.0D, Math.min(1.0D, projection));
            double nearestX = startX + projection * segmentX;
            double nearestZ = startZ + projection * segmentZ;
            double offsetX = x - nearestX;
            double offsetZ = z - nearestZ;
            return offsetX * offsetX + offsetZ * offsetZ <= radius * radius;
        }

        private static int square(int value) {
            return value * value;
        }

        private void placeCalibrationNode(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                                          DeepCalibrationNodeBlock.Calibration calibration) {
            BlockState state = ModBlocks.DEEP_CALIBRATION_NODE.get().defaultBlockState()
                    .setValue(DeepCalibrationNodeBlock.CALIBRATION, calibration);
            placeBlock(level, state, x, y, z, box);
        }

        private void buildCalibrationWing(WorldGenLevel level, BoundingBox box, int center,
                                          Direction direction, BlockState theme,
                                          BlockState wall, BlockState rune, BlockState lamp) {
            int stepX = direction.getStepX();
            int stepZ = direction.getStepZ();
            int perpendicularX = stepZ;
            int perpendicularZ = -stepX;
            int nodeX = center + stepX * 15;
            int nodeZ = center + stepZ * 15;

            for (int step = 4; step <= 15; step++) {
                int pathX = center + stepX * step;
                int pathZ = center + stepZ * step;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    BlockState state = Math.abs(lateral) == 2 || step % 5 == 0 ? rune : theme;
                    placeBlock(level, state,
                            pathX + perpendicularX * lateral, 0,
                            pathZ + perpendicularZ * lateral, box);
                }
            }

            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int distanceSquared = square(dx) + square(dz);
                    if (distanceSquared <= 10) {
                        placeBlock(level, distanceSquared >= 8 ? rune : theme,
                                nodeX + dx, 0, nodeZ + dz, box);
                    }
                }
            }

            for (int side : new int[]{-3, 3}) {
                int supportX = nodeX + perpendicularX * side;
                int supportZ = nodeZ + perpendicularZ * side;
                pillar(level, box, supportX, 1, supportZ, 7, wall);
                placeBlock(level, rune, supportX, 4, supportZ, box);
                placeBlock(level, lamp, supportX, 8, supportZ, box);
            }
            for (int lateral = -3; lateral <= 3; lateral++) {
                int archY = 8 + (3 - Math.abs(lateral)) / 2;
                BlockState state = lateral == 0 ? lamp : theme;
                placeBlock(level, state,
                        nodeX + perpendicularX * lateral, archY,
                        nodeZ + perpendicularZ * lateral, box);
            }

            int rearX = nodeX + stepX * 3;
            int rearZ = nodeZ + stepZ * 3;
            pillar(level, box, rearX, 1, rearZ, 9, wall);
            placeBlock(level, rune, rearX, 5, rearZ, box);
            placeBlock(level, lamp, rearX, 10, rearZ, box);
        }

        private void buildCalibrationPylon(WorldGenLevel level, BoundingBox box,
                                            int x, int z, int height,
                                            BlockState body, BlockState rune, BlockState lamp) {
            for (int xx = x - 1; xx <= x + 1; xx++) {
                for (int zz = z - 1; zz <= z + 1; zz++) {
                    placeBlock(level, Math.abs(xx - x) + Math.abs(zz - z) <= 1 ? rune : body,
                            xx, 0, zz, box);
                }
            }
            pillar(level, box, x, 1, z, height, body);
            for (int y = 3; y < height; y += 3) {
                placeBlock(level, rune, x, y, z, box);
                placeBlock(level, rune, x + 1, y, z, box);
                placeBlock(level, rune, x - 1, y, z, box);
                placeBlock(level, rune, x, y, z + 1, box);
                placeBlock(level, rune, x, y, z - 1, box);
            }
            placeBlock(level, lamp, x, height + 1, z, box);
        }

        private void buildCalibrationArches(WorldGenLevel level, BoundingBox box, int center,
                                            int radius, int height, BlockState body,
                                            BlockState rune, BlockState lamp) {
            for (int offset = -3; offset <= 3; offset++) {
                int archY = height + (3 - Math.abs(offset));
                BlockState cap = offset == 0 ? lamp : rune;
                placeBlock(level, cap, center + offset, archY, center - radius, box);
                placeBlock(level, cap, center + offset, archY, center + radius, box);
                placeBlock(level, cap, center - radius, archY, center + offset, box);
                placeBlock(level, cap, center + radius, archY, center + offset, box);
            }
            for (int y = 1; y <= height; y++) {
                for (int side : new int[]{-4, 4}) {
                    placeBlock(level, y % 4 == 0 ? rune : body,
                            center + side, y, center - radius, box);
                    placeBlock(level, y % 4 == 0 ? rune : body,
                            center + side, y, center + radius, box);
                    placeBlock(level, y % 4 == 0 ? rune : body,
                            center - radius, y, center + side, box);
                    placeBlock(level, y % 4 == 0 ? rune : body,
                            center + radius, y, center + side, box);
                }
            }
        }

        private void buildRiftSpine(WorldGenLevel level, BoundingBox box,
                                    int x, int z, int height, int center,
                                    BlockState body, BlockState vein, BlockState crystal) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 1) {
                        placeBlock(level, dx == 0 && dz == 0 ? vein : body,
                                x + dx, 0, z + dz, box);
                    }
                }
            }

            int inwardX = Integer.compare(center, x);
            int inwardZ = Integer.compare(center, z);
            if (Math.abs(x - center) >= Math.abs(z - center)) {
                inwardZ = 0;
            } else {
                inwardX = 0;
            }
            int topX = x;
            int topZ = z;
            for (int y = 1; y <= height; y++) {
                int bend = Math.max(0, y - height / 2) / 4;
                topX = x + inwardX * bend;
                topZ = z + inwardZ * bend;
                placeBlock(level, y % 3 == 0 ? vein : body, topX, y, topZ, box);
                if (y >= height / 3 && y % 4 == 0) {
                    placeBlock(level, vein, topX + inwardZ, y, topZ - inwardX, box);
                }
            }
            placeBlock(level, crystal, topX, height + 1, topZ, box);
        }

        private void buildStabilizerCradle(WorldGenLevel level, BoundingBox box,
                                           int nodeX, int nodeZ, int supportDx, int supportDz,
                                           int center, BlockState floor,
                                           BlockState vein, BlockState crystal) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int distanceSquared = square(dx) + square(dz);
                    if (distanceSquared <= 10) {
                        placeBlock(level, distanceSquared >= 8 ? crystal : floor,
                                nodeX + dx, 0, nodeZ + dz, box);
                    }
                }
            }

            for (int sign : new int[]{-1, 1}) {
                int supportX = nodeX + supportDx * sign;
                int supportZ = nodeZ + supportDz * sign;
                for (int y = 1; y <= 8; y++) {
                    placeBlock(level, y == 3 || y == 6 ? crystal : vein,
                            supportX, y, supportZ, box);
                }
            }
            for (int step = -3; step <= 3; step++) {
                int archX = nodeX + (int) Math.round(supportDx * step / 3.0D);
                int archZ = nodeZ + (int) Math.round(supportDz * step / 3.0D);
                int archY = 9 + (3 - Math.abs(step)) / 2;
                placeBlock(level, step == 0 ? crystal : vein, archX, archY, archZ, box);
            }

            int radialX = Integer.compare(nodeX, center);
            int radialZ = Integer.compare(nodeZ, center);
            placeBlock(level, crystal, nodeX + radialX * 3, 1, nodeZ + radialZ * 3, box);
        }

        private void clearVolume(WorldGenLevel level, BoundingBox box) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int x = 1; x < kind.width - 1; x++) {
                for (int z = 1; z < kind.depth - 1; z++) {
                    int worldX = getWorldX(x, z);
                    int worldZ = getWorldZ(x, z);
                    if (worldX < box.minX() || worldX > box.maxX()
                            || worldZ < box.minZ() || worldZ > box.maxZ()) {
                        continue;
                    }
                    for (int y = 1; y < kind.height; y++) {
                        placeBlock(level, air, x, y, z, box);
                    }
                }
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                              int width, int depth, BlockState state) {
            for (int xx = x; xx < x + width; xx++) {
                for (int zz = z; zz < z + depth; zz++) {
                    placeBlock(level, state, xx, y, zz, box);
                }
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                            int height, BlockState state) {
            for (int yy = y; yy < y + height; yy++) {
                placeBlock(level, state, x, yy, z, box);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("DeepRegionKind", kind.id);
        }
    }
}
