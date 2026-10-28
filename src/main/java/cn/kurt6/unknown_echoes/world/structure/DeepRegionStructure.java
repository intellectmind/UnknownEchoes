package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.DeepCalibrationNodeBlock;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceLayout;
import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock;
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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Arrays;
import java.util.Optional;

public class DeepRegionStructure extends TerrainAnchoredStructure {
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
    public static final MapCodec<DeepRegionStructure> MISFILED_CATALOG_WELL_CODEC =
            codec(Kind.MISFILED_CATALOG_WELL);
    public static final MapCodec<DeepRegionStructure> FOLDED_SCRIPTORIUM_CODEC =
            codec(Kind.FOLDED_SCRIPTORIUM);
    public static final MapCodec<DeepRegionStructure> INVERTED_ARCHIVE_TOWER_CODEC =
            codec(Kind.INVERTED_ARCHIVE_TOWER);
    public static final MapCodec<DeepRegionStructure> SUSPENDED_SURVEY_STATION_CODEC =
            codec(Kind.SUSPENDED_SURVEY_STATION);
    public static final MapCodec<DeepRegionStructure> FRACTURED_ANCHOR_STATION_CODEC =
            codec(Kind.FRACTURED_ANCHOR_STATION);
    public static final MapCodec<DeepRegionStructure> SEVERED_ARCHIVE_VAULT_CODEC =
            codec(Kind.SEVERED_ARCHIVE_VAULT);
    public static final MapCodec<DeepRegionStructure> RESONANCE_FOUNDRY_CODEC =
            codec(Kind.RESONANCE_FOUNDRY);
    public static final MapCodec<DeepRegionStructure> GRAVITY_CROWN_OBSERVATORY_CODEC =
            codec(Kind.GRAVITY_CROWN_OBSERVATORY);
    public static final MapCodec<DeepRegionStructure> VOIDSCAR_BASTION_CODEC =
            codec(Kind.VOIDSCAR_BASTION);
    public static final MapCodec<DeepRegionStructure> SHATTERED_CODEX_VAULT_CODEC =
            codec(Kind.SHATTERED_CODEX_VAULT);
    public static final MapCodec<DeepRegionStructure> WEIGHTLESS_ORRERY_COURT_CODEC =
            codec(Kind.WEIGHTLESS_ORRERY_COURT);
    public static final MapCodec<DeepRegionStructure> HUSHED_CATHEDRAL_CODEC =
            codec(Kind.HUSHED_CATHEDRAL);
    public static final MapCodec<DeepRegionStructure> RIFTGLASS_CITADEL_CODEC =
            codec(Kind.RIFTGLASS_CITADEL);
    public static final MapCodec<DeepRegionStructure> FOLDED_CELESTIAL_ARCHIVE_CODEC =
            codec(Kind.FOLDED_CELESTIAL_ARCHIVE);
    public static final MapCodec<DeepRegionStructure> INVERTED_PENDULUM_PALACE_CODEC =
            codec(Kind.INVERTED_PENDULUM_PALACE);
    public static final MapCodec<DeepRegionStructure> RIFTWORN_MEMORIAL_COURT_CODEC =
            codec(Kind.RIFTWORN_MEMORIAL_COURT);

    private final Kind kind;

    private static MapCodec<DeepRegionStructure> codec(Kind kind) {
        return simpleCodec(settings -> new DeepRegionStructure(settings, kind));
    }

    private DeepRegionStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.width, kind.depth, settings);
        this.kind = kind;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var candidate = super.findGenerationPoint(context);
        if (kind != Kind.FOLDED_WAYSTATION || candidate.isEmpty()) {
            return candidate;
        }
        var bounds = candidate.get().getPiecesBuilder().build().pieces().getFirst().getBoundingBox();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        // 驿站没有跨层桥，足印内高差超过建筑高度时不能把它嵌在两层地形之间。
        for (int ix = 0; ix < 3; ix++) {
            for (int iz = 0; iz < 3; iz++) {
                int y = context.chunkGenerator().getBaseHeight(
                        bounds.minX() + (bounds.getXSpan() - 1) * ix / 2,
                        bounds.minZ() + (bounds.getZSpan() - 1) * iz / 2,
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);
                if (highest - lowest > bounds.getYSpan()) {
                    return Optional.empty();
                }
            }
        }
        return candidate;
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
            case MISFILED_CATALOG_WELL -> ModStructures.MISFILED_CATALOG_WELL.get();
            case FOLDED_SCRIPTORIUM -> ModStructures.FOLDED_SCRIPTORIUM.get();
            case INVERTED_ARCHIVE_TOWER -> ModStructures.INVERTED_ARCHIVE_TOWER.get();
            case SUSPENDED_SURVEY_STATION -> ModStructures.SUSPENDED_SURVEY_STATION.get();
            case FRACTURED_ANCHOR_STATION -> ModStructures.FRACTURED_ANCHOR_STATION.get();
            case SEVERED_ARCHIVE_VAULT -> ModStructures.SEVERED_ARCHIVE_VAULT.get();
            case RESONANCE_FOUNDRY -> ModStructures.RESONANCE_FOUNDRY.get();
            case GRAVITY_CROWN_OBSERVATORY -> ModStructures.GRAVITY_CROWN_OBSERVATORY.get();
            case VOIDSCAR_BASTION -> ModStructures.VOIDSCAR_BASTION.get();
            case SHATTERED_CODEX_VAULT -> ModStructures.SHATTERED_CODEX_VAULT.get();
            case WEIGHTLESS_ORRERY_COURT -> ModStructures.WEIGHTLESS_ORRERY_COURT.get();
            case HUSHED_CATHEDRAL -> ModStructures.HUSHED_CATHEDRAL.get();
            case RIFTGLASS_CITADEL -> ModStructures.RIFTGLASS_CITADEL.get();
            case FOLDED_CELESTIAL_ARCHIVE -> ModStructures.FOLDED_CELESTIAL_ARCHIVE.get();
            case INVERTED_PENDULUM_PALACE -> ModStructures.INVERTED_PENDULUM_PALACE.get();
            case RIFTWORN_MEMORIAL_COURT -> ModStructures.RIFTWORN_MEMORIAL_COURT.get();
        };
    }

    public enum Kind {
        FOLDED_ARCHIVE("folded_archive", 31, 18, 31),
        INVERTED_CHASM_BRIDGE("inverted_chasm_bridge", 37, 24, 25),
        SILENT_ABYSS_ANCHOR("silent_abyss_anchor", 31, 18, 31),
        CALIBRATION_HALL("calibration_hall", 45, 28, 45),
        VOID_VEIN_CHAMBER("void_vein_chamber", 49, 30, 49),
        WORLD_RESONANCE_HALL("world_resonance_hall", WorldResonanceLayout.GRAND_SIZE,
                WorldResonanceLayout.GRAND_HEIGHT, WorldResonanceLayout.GRAND_SIZE),
        FOLDED_WAYSTATION("folded_waystation", 25, 18, 25),
        GRAVITY_SHARD_SPIRE("gravity_shard_spire", 27, 32, 27),
        HUSHED_REFUGE("hushed_refuge", 29, 18, 29),
        MISFILED_CATALOG_WELL("misfiled_catalog_well", 35, 24, 35),
        FOLDED_SCRIPTORIUM("folded_scriptorium", 39, 20, 33),
        INVERTED_ARCHIVE_TOWER("inverted_archive_tower", 35, 40, 35),
        SUSPENDED_SURVEY_STATION("suspended_survey_station", 41, 24, 37),
        FRACTURED_ANCHOR_STATION("fractured_anchor_station", 39, 24, 39),
        SEVERED_ARCHIVE_VAULT("severed_archive_vault", 43, 30, 37),
        RESONANCE_FOUNDRY("resonance_foundry", 61, 25, 61),
        GRAVITY_CROWN_OBSERVATORY("gravity_crown_observatory", 57, 32, 57),
        VOIDSCAR_BASTION("voidscar_bastion", 65, 28, 65),
        SHATTERED_CODEX_VAULT("shattered_codex_vault", 67, 27, 67),
        WEIGHTLESS_ORRERY_COURT("weightless_orrery_court", 63, 36, 63),
        HUSHED_CATHEDRAL("hushed_cathedral", 69, 36, 69),
        RIFTGLASS_CITADEL("riftglass_citadel", 73, 38, 73),
        FOLDED_CELESTIAL_ARCHIVE("folded_celestial_archive", 71, 34, 71),
        INVERTED_PENDULUM_PALACE("inverted_pendulum_palace", 75, 42, 75),
        RIFTWORN_MEMORIAL_COURT("riftworn_memorial_court", 79, 40, 79);

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
        protected int sampleGroundHeight(net.minecraft.world.level.levelgen.structure.Structure.GenerationContext context) {
            BlockPos entry = switch (kind) {
                case FOLDED_ARCHIVE -> new BlockPos(16, 0, -1);
                case INVERTED_CHASM_BRIDGE -> new BlockPos(-1, 0, 11);
                case SILENT_ABYSS_ANCHOR -> new BlockPos(31, 0, 14);
                case CALIBRATION_HALL -> new BlockPos(21, 0, -1);
                case VOID_VEIN_CHAMBER -> new BlockPos(24, 0, -1);
                case WORLD_RESONANCE_HALL -> new BlockPos(-1, 0, 40);
                case FOLDED_WAYSTATION -> new BlockPos(11, 0, -1);
                case GRAVITY_SHARD_SPIRE -> new BlockPos(12, 0, -1);
                case HUSHED_REFUGE -> new BlockPos(14, 0, -1);
                case INVERTED_ARCHIVE_TOWER -> new BlockPos(-1, 0, 17);
                case SUSPENDED_SURVEY_STATION -> new BlockPos(14, 0, -1);
                case FRACTURED_ANCHOR_STATION -> new BlockPos(39, 0, 10);
                case SEVERED_ARCHIVE_VAULT -> new BlockPos(21, 0, -1);
                case RESONANCE_FOUNDRY -> new BlockPos(30, 0, -1);
                case GRAVITY_CROWN_OBSERVATORY -> new BlockPos(28, 0, -1);
                case VOIDSCAR_BASTION -> new BlockPos(32, 0, -1);
                case SHATTERED_CODEX_VAULT -> new BlockPos(33, 0, -1);
                case WEIGHTLESS_ORRERY_COURT -> new BlockPos(31, 0, -1);
                case RIFTGLASS_CITADEL -> new BlockPos(36, 0, -1);
                case HUSHED_CATHEDRAL -> new BlockPos(34, 0, -1);
                case FOLDED_CELESTIAL_ARCHIVE -> new BlockPos(35, 0, -1);
                case INVERTED_PENDULUM_PALACE -> new BlockPos(37, 0, -1);
                case RIFTWORN_MEMORIAL_COURT -> new BlockPos(39, 0, -1);
                default -> null;
            };
            if (entry == null) {
                return super.sampleGroundHeight(context);
            }
            // 浮岛上下层的平均高度可能落在空气中，按入口外的固定地表列定高。
            return context.chunkGenerator().getBaseHeight(getWorldX(entry.getX(), entry.getZ()), getWorldZ(entry.getX(), entry.getZ()),
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
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
                case MISFILED_CATALOG_WELL -> buildMisfiledCatalogWell(level, box);
                case FOLDED_SCRIPTORIUM -> buildFoldedScriptorium(level, box);
                case INVERTED_ARCHIVE_TOWER -> buildInvertedArchiveTower(level, box);
                case SUSPENDED_SURVEY_STATION -> buildSuspendedSurveyStation(level, box);
                case FRACTURED_ANCHOR_STATION -> buildFracturedAnchorStation(level, box);
                case SEVERED_ARCHIVE_VAULT -> buildSeveredArchiveVault(level, box);
                case RESONANCE_FOUNDRY -> buildResonanceFoundry(level, box);
                case GRAVITY_CROWN_OBSERVATORY -> buildGravityCrownObservatory(level, box);
                case VOIDSCAR_BASTION -> buildVoidscarBastion(level, box);
                case SHATTERED_CODEX_VAULT -> buildShatteredCodexVault(level, box);
                case WEIGHTLESS_ORRERY_COURT -> buildWeightlessOrreryCourt(level, box);
                case HUSHED_CATHEDRAL -> buildHushedCathedral(level, box);
                case RIFTGLASS_CITADEL -> buildRiftglassCitadel(level, box);
                case FOLDED_CELESTIAL_ARCHIVE -> buildFoldedCelestialArchive(level, box);
                case INVERTED_PENDULUM_PALACE -> buildInvertedPendulumPalace(level, box);
                case RIFTWORN_MEMORIAL_COURT -> buildRiftwornMemorialCourt(level, box);
            }
            buildArchitecturalEnvelope(level, box);
            buildDeepSignature(level, box);
            placeExplorationLoot(level, random, box);
            placeThemedEncounter(level, random, box);
            BlockState foundation = switch (kind) {
                case INVERTED_CHASM_BRIDGE, GRAVITY_SHARD_SPIRE, GRAVITY_CROWN_OBSERVATORY,
                        WEIGHTLESS_ORRERY_COURT, INVERTED_PENDULUM_PALACE ->
                        ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                case SILENT_ABYSS_ANCHOR, HUSHED_REFUGE, HUSHED_CATHEDRAL ->
                        ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
                default -> ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            };
            anchorGroundColumns(level, box, 0, foundation);
            dressStructure(level, box, kind.id);
            if (kind == Kind.FOLDED_CELESTIAL_ARCHIVE) {
                placeBlock(level, ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState(), 47, 7, 39, box);
            }
            buildGroundEntry(level, box);
        }

        private void buildGroundEntry(WorldGenLevel level, BoundingBox box) {
            switch (kind) {
                case GRAVITY_SHARD_SPIRE -> {
                    BlockState stairs = ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.NORTH);
                    placeBlock(level, ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState(), 12, 0, 1, box);
                    placeBlock(level, stairs, 12, 1, 1, box);
                    placeBlock(level, stairs, 12, 0, 0, box);
                    BlockState wall = ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState()
                            .setValue(BlockStateProperties.NORTH_WALL, WallSide.TALL)
                            .setValue(BlockStateProperties.EAST_WALL, WallSide.TALL);
                    placeBlock(level, wall, 13, 1, 1, box);
                }
                case FOLDED_ARCHIVE -> placeBlock(level,
                        ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState()
                                .setValue(StairBlock.FACING, Direction.NORTH), 16, 0, 0, box);
                case SILENT_ABYSS_ANCHOR -> {
                    BlockState stairs = ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.WEST);
                    placeBlock(level, stairs, 28, 1, 14, box);
                    placeBlock(level, stairs, 29, 0, 14, box);
                    BlockState wall = ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState();
                    placeBlock(level, wall.setValue(BlockStateProperties.SOUTH_WALL, WallSide.TALL),
                            28, 1, 13, box);
                    placeBlock(level, wall.setValue(BlockStateProperties.NORTH_WALL, WallSide.TALL)
                                    .setValue(BlockStateProperties.EAST_WALL, WallSide.TALL)
                                    .setValue(BlockStateProperties.WEST_WALL, WallSide.TALL),
                            28, 1, 15, box);
                }
                case FRACTURED_ANCHOR_STATION -> {
                    BlockState bricks = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    for (int x = 35; x <= 37; x++) {
                        placeBlock(level, bricks, x, 0, 10, box);
                    }
                    placeBlock(level, bricks, 36, 1, 10, box);
                    placeBlock(level, Blocks.AIR.defaultBlockState(), 35, 2, 10, box);
                    placeBlock(level, Blocks.AIR.defaultBlockState(), 35, 3, 10, box);
                    BlockState stairs = ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.WEST);
                    placeBlock(level, stairs, 37, 1, 10, box);
                    placeBlock(level, stairs, 38, 0, 10, box);
                    BlockState wall = ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState();
                    placeBlock(level, wall.setValue(BlockStateProperties.SOUTH_WALL, WallSide.LOW),
                            37, 1, 9, box);
                    placeBlock(level, wall.setValue(BlockStateProperties.NORTH_WALL, WallSide.LOW),
                            37, 1, 11, box);
                }
                default -> {
                }
            }
        }

        /** 深层结构的专属构件:折叠、重力与虚痕各自拥有不同的远景节奏。 */
        private void buildDeepSignature(WorldGenLevel level, BoundingBox box) {
            int c = kind.width / 2;
            switch (kind) {
                case FOLDED_ARCHIVE -> {
                    BlockState slate = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int z = c - 10; z <= c + 10; z += 5) {
                            placeCorniceBlock(level, box, slate, c + side * 12, 11, z);
                            placeCorniceBlock(level, box, rune, c + side * 12, 12, z);
                        }
                    }
                }
                case INVERTED_CHASM_BRIDGE -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (int x = 7; x <= 29; x += 4) {
                        architecturalPillar(level, box, x, 14, c, 7, gravity);
                        placeCorniceBlock(level, box, band, x, 21, c);
                    }
                }
                case SILENT_ABYSS_ANCHOR -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        architecturalPillar(level, box, c + direction.getStepX() * 11,
                                10, c + direction.getStepZ() * 11, 7, abyss);
                        placeCorniceBlock(level, box, vein, c + direction.getStepX() * 11,
                                17, c + direction.getStepZ() * 11);
                    }
                }
                case CALIBRATION_HALL -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (int radius : new int[]{20, 22}) {
                        architecturalRing(level, box, c, 16 + radius % 3, c, radius,
                                radius == 20 ? gravity : band);
                    }
                }
                case VOID_VEIN_CHAMBER -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int y = 10; y <= 24; y += 3) {
                            placeCorniceBlock(level, box, y % 2 == 0 ? abyss : vein,
                                    c + side * 23, y, c + side * 7);
                        }
                    }
                }
                case WORLD_RESONANCE_HALL -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    for (int layer = 0; layer < 3; layer++) {
                        architecturalRing(level, box, c, 17 + layer * 5, c,
                                25 + layer * 5, layer == 1 ? gravity : folded);
                    }
                }
                case FOLDED_WAYSTATION -> {
                    BlockState slate = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 8, 10, c - 8, 5, slate);
                        placeCorniceBlock(level, box, rune, c + side * 8, 15, c - 8);
                    }
                }
                case GRAVITY_SHARD_SPIRE -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (int y = 12; y <= 26; y += 2) {
                        int radius = 8 - (y - 12) / 5;
                        architecturalRing(level, box, c, y, c, Math.max(3, radius),
                                y % 4 == 0 ? band : gravity);
                    }
                }
                case HUSHED_REFUGE -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
                    architecturalRing(level, box, c, 11, c, 10, abyss);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        placeCorniceBlock(level, box, moss, c + direction.getStepX() * 10,
                                12, c + direction.getStepZ() * 10);
                    }
                }
                case MISFILED_CATALOG_WELL -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    for (int radius : new int[]{9, 12, 15}) {
                        architecturalRing(level, box, c, 12 + radius % 4, c, radius,
                                radius == 12 ? trim : folded);
                    }
                }
                case FOLDED_SCRIPTORIUM -> {
                    BlockState slate = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    for (int x = c - 13; x <= c + 13; x += 5) {
                        architecturalPillar(level, box, x, 11, c - 14, 6, slate);
                        placeCorniceBlock(level, box, trim, x, 17, c - 14);
                    }
                }
                case INVERTED_ARCHIVE_TOWER -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    for (int y = 20; y <= 34; y += 3) {
                        int radius = Math.max(3, 9 - (y - 20) / 4);
                        architecturalRing(level, box, c, y, c, radius,
                                y % 2 == 0 ? gravity : folded);
                    }
                }
                case SUSPENDED_SURVEY_STATION -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int z = c - 12; z <= c + 12; z += 6) {
                            placeCorniceBlock(level, box, gravity, c + side * 14, 14, z);
                            placeCorniceBlock(level, box, band, c + side * 14, 15, z);
                        }
                    }
                }
                case FRACTURED_ANCHOR_STATION -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 15);
                        int z = c + (int) Math.round(Math.sin(angle) * 15);
                        architecturalPillar(level, box, x, 12, z, 6, abyss);
                        placeCorniceBlock(level, box, vein, x, 18, z);
                    }
                }
                case SEVERED_ARCHIVE_VAULT -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 17, 12, c - 12, 7, abyss);
                        placeCorniceBlock(level, box, trim, c + side * 17, 19, c - 12);
                    }
                }
                case RESONANCE_FOUNDRY -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    for (int radius : new int[]{20, 26}) {
                        architecturalRing(level, box, c, 14 + radius % 3, c, radius,
                                radius == 20 ? gravity : folded);
                    }
                }
                case GRAVITY_CROWN_OBSERVATORY -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        architecturalPillar(level, box, c + direction.getStepX() * 18,
                                16, c + direction.getStepZ() * 18, 9, gravity);
                        placeCorniceBlock(level, box, band, c + direction.getStepX() * 18,
                                25, c + direction.getStepZ() * 18);
                    }
                }
                case VOIDSCAR_BASTION -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int z = c - 23; z <= c + 23; z += 8) {
                            architecturalPillar(level, box, c + side * 24, 15, z, 8, abyss);
                            placeCorniceBlock(level, box, vein, c + side * 24, 23, z);
                        }
                    }
                }
                case SHATTERED_CODEX_VAULT -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
                    for (int radius : new int[]{18, 24, 30}) {
                        architecturalRing(level, box, c, 14 + radius % 4, c, radius,
                                radius == 24 ? rune : folded);
                    }
                }
                case WEIGHTLESS_ORRERY_COURT -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState prism = ModBlocks.ORRERY_PRISM_BLOCK.get().defaultBlockState();
                    for (int i = 0; i < 10; i++) {
                        double angle = i * Math.PI / 5.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 22);
                        int z = c + (int) Math.round(Math.sin(angle) * 22);
                        architecturalPillar(level, box, x, 15, z, 8, gravity);
                        placeCorniceBlock(level, box, prism, x, 23, z);
                    }
                }
                case HUSHED_CATHEDRAL -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int x = c - 25; x <= c + 25; x += 10) {
                            architecturalPillar(level, box, x, 17, c + side * 26, 9, abyss);
                            placeCorniceBlock(level, box, moss, x, 26, c + side * 26);
                        }
                    }
                }
                case RIFTGLASS_CITADEL -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState glass = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
                    for (int radius : new int[]{24, 30, 34}) {
                        architecturalRing(level, box, c, 17 + radius % 4, c, radius,
                                radius == 30 ? glass : folded);
                    }
                }
                case FOLDED_CELESTIAL_ARCHIVE -> {
                    BlockState slate = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int z = c - 24; z <= c + 24; z += 8) {
                            architecturalPillar(level, box, c + side * 25, 16, z, 8, slate);
                            placeCorniceBlock(level, box, trim, c + side * 25, 24, z);
                        }
                    }
                }
                case INVERTED_PENDULUM_PALACE -> {
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
                    for (int y = 17; y <= 35; y += 3) {
                        int radius = 20 + (y % 6);
                        architecturalRing(level, box, c, y, c, radius,
                                y % 2 == 0 ? gravity : band);
                    }
                }
                case RIFTWORN_MEMORIAL_COURT -> {
                    BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
                    for (int i = 0; i < 12; i++) {
                        double angle = i * Math.PI / 6.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 29);
                        int z = c + (int) Math.round(Math.sin(angle) * 29);
                        architecturalPillar(level, box, x, 16, z, 10, abyss);
                        placeCorniceBlock(level, box, vein, x, 26, z);
                    }
                }
            }
        }

        /**
         * 为前期深层探索点补足可进入的建筑外壳。
         * 机关与奖励随后生成，因此这里仅负责 门面、屋顶、连桥和外廊，不改动既有解谜坐标与结算路径。
         */
        private void buildArchitecturalEnvelope(WorldGenLevel level, BoundingBox box) {
            BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState foldedTrim = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState foldedRune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState gravityBand = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState abyssVein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState suspendedLight = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            BlockState muteLight = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            switch (kind) {
                case FOLDED_ARCHIVE -> {
                    buildArcadedWing(level, box, 15, 1, 15, Footprint.square(12), 8, 4, ModBlocks.FOLDED_SLATE.get().defaultBlockState(), folded, foldedTrim, suspendedLight);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, 15, 1, 15, 14, direction,
                                2, 9, folded, foldedRune);
                    }
                    buildStairCornice(level, box, 15, 9, 15, Footprint.square(12), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 15, 1, 15, Footprint.square(14), 5, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), foldedRune);
                }
                case INVERTED_CHASM_BRIDGE -> {
                    // 桥面与桥墩必须用不同砖:同砖会读成一条无厚度的灰板。
                    BlockState deck = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    BlockState pier = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
                    buildMonumentalBridge(level, box,
                            18, 4, 12, Direction.EAST, 8, 2, 7,
                            deck, pier, gravityBand, suspendedLight);
                    buildParapet(level, box, 18, 5, 12, Footprint.of(8, 2), 4, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), gravityBand);
                    buildStairCornice(level, box, 18, 4, 12, Footprint.of(8, 2), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
                    for (int towerX : new int[]{10, 26}) {
                        buildSpireFinial(level, box, towerX, 12, 12, 2, 4,
                                gravity, gravityBand, suspendedLight);
                    }
                }
                case SILENT_ABYSS_ANCHOR -> {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, 15, 1, 15, 13, direction,
                                2, 9, abyss, abyssVein);
                    }
                    buildRadialButtresses(level, box, 15, 1, 15,
                            9, 14, 6, abyss, muteLight);
                    buildStairCornice(level, box, 15, 7, 15, Footprint.square(11), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 15, 1, 15, Footprint.square(13), 5, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), abyssVein);
                    buildCornice(level, box, 15, 8, 15, Footprint.square(10), ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState(), ModBlocks.MUFFLE_MOSS.get().defaultBlockState());
                    buildSpireFinial(level, box, 15, 9, 15, 3, 5,
                            abyss, abyssVein, muteLight);
                }
                case FOLDED_WAYSTATION -> {
                    // 折痕驿站必须用悬挑栈台而不是亭殿:同群系已有折叠板岩亭殿,会撞形。
                    buildCantileverTerraces(level, box, 12, 1, 12,
                            Direction.EAST, 3, 7, 3,
                            ModBlocks.FOLDED_SLATE.get().defaultBlockState(),
                            folded, foldedTrim, suspendedLight);
                    buildStairCornice(level, box, 12, 7, 12, Footprint.square(9), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 12, 8, 12, Footprint.square(10), 4, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), foldedTrim);
                    buildSpireFinial(level, box, 12, 9, 12, 2, 4,
                            folded, foldedTrim,
                            ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
                }
                case GRAVITY_SHARD_SPIRE -> {
                    // 扶壁与末端必须传不同材质,同材质整座碎塔只有一种灰。
                    buildRadialButtresses(level, box, 13, 1, 13,
                            7, 12, 7, gravity,
                            ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState());
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, 13, 1, 13, 12, direction,
                                2, 8, gravity, gravityBand);
                    }
                    buildStairCornice(level, box, 13, 8, 13, Footprint.square(9), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 13, 1, 13, Footprint.square(12), 5, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), gravityBand);
                    buildCornice(level, box, 13, 9, 13, Footprint.square(7), folded, gravityBand);
                    buildSpireFinial(level, box, 13, 10, 13, 3, 6,
                            ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState(),
                            folded, suspendedLight);
                }
                case HUSHED_REFUGE -> {
                    // 避难所必须用半塌残壳而不是完好院落:残墙是"被放弃过"的唯一可读信号。
                    buildRuinedShell(level, box, 14, 1, 14, 10, 10, 8,
                            abyss, ModBlocks.MUFFLE_MOSS.get().defaultBlockState(),
                            abyssVein, muteLight);
                    buildStairCornice(level, box, 14, 8, 14, Footprint.square(9), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 14, 9, 14, Footprint.square(11), 4, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), abyssVein);
                    buildCornice(level, box, 14, 2, 14, Footprint.square(10), abyss, abyssVein);
                }
                case MISFILED_CATALOG_WELL -> {
                    // 编目井必须用围合中庭:折痕驿站已占用同群系的折叠板岩亭殿剪影。
                    buildCourtyardCloister(level, box, 17, 1, 17, Footprint.square(13), 9, 3, ModBlocks.FOLDED_SLATE.get().defaultBlockState(), folded, foldedTrim, suspendedLight);
                    buildStairCornice(level, box, 17, 9, 17, Footprint.square(12), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 17, 1, 17, Footprint.square(13), 5, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), foldedTrim);
                    buildSpireFinial(level, box, 17, 10, 17, 3, 5,
                            folded, foldedTrim,
                            ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
                }
                case FOLDED_SCRIPTORIUM -> {
                    buildRibbedVaultHall(level, box, 19, 1, 16, Footprint.of(15, 13), 8, 5, ModBlocks.FOLDED_SLATE.get().defaultBlockState(), folded, foldedTrim, suspendedLight);
                    buildStairCornice(level, box, 19, 10, 16, Footprint.of(15, 13), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 19, 11, 16, Footprint.of(15, 13), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), foldedTrim);
                    buildCornice(level, box, 19, 6, 16, Footprint.of(15, 13), folded, foldedTrim);
                }
                case INVERTED_ARCHIVE_TOWER -> {
                    buildTwinTowerFacade(level, box,
                            17, 1, 2, 9, 2, 16,
                            ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState(),
                            gravityBand, suspendedLight);
                    buildCornice(level, box, 17, 16, 3, Footprint.of(9, 2), gravity, folded);

                    buildStairCornice(level, box, 17, 17, 3, Footprint.of(9, 2), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
                    for (int towerX : new int[]{8, 26}) {
                        buildSpireFinial(level, box, towerX, 19, 2, 2, 4,
                                gravity, gravityBand, suspendedLight);
                    }
                }
                case SUSPENDED_SURVEY_STATION -> {
                    // 桥面与桥墩必须传不同材质:同材质整条桥读成一块无厚度的灰板。
                    buildMonumentalBridge(level, box, 20, 5, 18,
                            Direction.EAST, 16, 2, 6,
                            ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState(),
                            gravity, gravityBand, suspendedLight);
                    buildParapet(level, box, 20, 6, 18, Footprint.of(16, 2), 4, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), gravityBand);
                    buildStairCornice(level, box, 20, 5, 18, Footprint.of(16, 2), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
                    // 必须叠悬挑观测层:倒悬断桥已占用同群系的重力页岩长桥剪影。
                    buildCantileverTerraces(level, box, 20, 9, 18,
                            Direction.NORTH, 3, 6, 3,
                            gravity, gravityBand, ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState(),
                            suspendedLight);
                    buildCornice(level, box, 20, 4, 18, Footprint.of(16, 2), gravity, gravityBand);
                }
                case FRACTURED_ANCHOR_STATION -> {
                    buildRadialButtresses(level, box, 19, 1, 19,
                            12, 18, 8, abyss, abyssVein);
                    // 必须叠半塌残壳:静默锚阵已占用同群系的深渊石飞扶壁剪影。
                    buildRuinedShell(level, box, 19, 1, 19, 16, 16, 8,
                            abyss, abyssVein,
                            ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState(), muteLight);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, 19, 1, 19, 18, direction,
                                3, 9, abyss, abyssVein);
                    }
                    buildStairCornice(level, box, 19, 9, 19, Footprint.square(15), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 19, 1, 19, Footprint.square(18), 6, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), abyssVein);
                    buildCornice(level, box, 19, 10, 19, Footprint.square(14), ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState(), ModBlocks.MUFFLE_MOSS.get().defaultBlockState());
                }
                case SEVERED_ARCHIVE_VAULT -> {
                    BlockState vaultBrick = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
                    buildTwinTowerFacade(level, box,
                            21, 1, 2, 13, 2, 18,
                            vaultBrick, abyssVein, muteLight);

                    buildStairCornice(level, box, 21, 19, 3, Footprint.of(13, 2), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, 21, 20, 3, Footprint.of(13, 2), 5, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), folded);
                    for (int towerX : new int[]{8, 34}) {
                        buildSpireFinial(level, box, towerX, 21, 2, 2, 4,
                                abyss, abyssVein, muteLight);
                    }
                }
                default -> {
                }
            }
        }

        private void placeExplorationLoot(WorldGenLevel level, RandomSource random, BoundingBox box) {
            switch (kind) {
                case FOLDED_ARCHIVE -> {
                    buildAccessibleChest(level, random, box, 6, 1, 15, ModLootTables.FOLDED_ARCHIVE_CHEST);
                    buildAccessibleChest(level, random, box, 24, 1, 15, ModLootTables.FOLDED_ARCHIVE_CHEST);
                    buildAccessibleChest(level, random, box, 15, 8, 15, ModLootTables.FOLDED_ARCHIVE_CHEST);
                }
                case INVERTED_CHASM_BRIDGE -> {
                    buildAccessibleChest(level, random, box, 5, 1, 12, ModLootTables.INVERTED_CHASM_CHEST);
                    buildAccessibleChest(level, random, box, 31, 1, 12, ModLootTables.INVERTED_CHASM_CHEST);
                }
                case SILENT_ABYSS_ANCHOR -> {
                    buildAccessibleChest(level, random, box, 15, 1, 5, ModLootTables.SILENT_ABYSS_CHEST);
                    buildAccessibleChest(level, random, box, 15, 1, 25, ModLootTables.SILENT_ABYSS_CHEST);
                }
                case CALIBRATION_HALL -> {
                    buildAccessibleChest(level, random, box, 13, 1, 14, ModLootTables.CALIBRATION_HALL_CHEST);
                    buildAccessibleChest(level, random, box, 31, 1, 30, ModLootTables.CALIBRATION_HALL_CHEST);
                }
                case VOID_VEIN_CHAMBER -> {
                    buildAccessibleChest(level, random, box, 12, 1, 12, ModLootTables.VOID_VEIN_CHEST);
                    buildAccessibleChest(level, random, box, 36, 1, 36, ModLootTables.VOID_VEIN_CHEST);
                }
                case WORLD_RESONANCE_HALL -> {
                    buildAccessibleChest(level, random, box, 14, 1, 16,
                            ModLootTables.WORLD_RESONANCE_HALL_CHEST);
                    buildAccessibleChest(level, random, box, 66, 1, 16,
                            ModLootTables.WORLD_RESONANCE_HALL_CHEST);
                }
                case FOLDED_WAYSTATION -> {
                    buildAccessibleChest(level, random, box, 4, 1, 8,
                            ModLootTables.FOLDED_WAYSTATION_CHEST);
                    buildAccessibleChest(level, random, box, 20, 1, 16,
                            ModLootTables.FOLDED_WAYSTATION_CHEST);
                }
                case GRAVITY_SHARD_SPIRE -> {
                    buildAccessibleChest(level, random, box, 13, 1, 4,
                            ModLootTables.GRAVITY_SHARD_SPIRE_CHEST);
                    buildAccessibleChest(level, random, box, 13, 1, 22,
                            ModLootTables.GRAVITY_SHARD_SPIRE_CHEST);
                }
                case HUSHED_REFUGE -> {
                    buildAccessibleChest(level, random, box, 14, 2, 14,
                            ModLootTables.HUSHED_REFUGE_CHEST);
                    buildAccessibleChest(level, random, box, 14, 1, 22,
                            ModLootTables.HUSHED_REFUGE_CHEST);
                }
                case MISFILED_CATALOG_WELL -> {
                    buildAccessibleChest(level, random, box, 17, 1, 17,
                            ModLootTables.PUZZLE_MISFILED_CATALOG_WELL_CHEST);
                    buildAccessibleChest(level, random, box, 5, 1, 17,
                            ModLootTables.MISFILED_CATALOG_WELL_CHEST);
                }
                case FOLDED_SCRIPTORIUM -> {
                    buildAccessibleChest(level, random, box, 32, 1, 25,
                            ModLootTables.FOLDED_SCRIPTORIUM_CHEST);
                    buildAccessibleChest(level, random, box, 6, 8, 25,
                            ModLootTables.FOLDED_SCRIPTORIUM_CHEST);
                }
                case INVERTED_ARCHIVE_TOWER -> {
                    buildAccessibleChest(level, random, box, 17, 17, 17,
                            ModLootTables.INVERTED_ARCHIVE_TOWER_CHEST);
                    buildAccessibleChest(level, random, box, 17, 33, 17,
                            ModLootTables.INVERTED_ARCHIVE_TOWER_CHEST);
                }
                case SUSPENDED_SURVEY_STATION -> {
                    buildAccessibleChest(level, random, box, 20, 1, 6,
                            ModLootTables.PUZZLE_SUSPENDED_SURVEY_STATION_CHEST);
                    buildAccessibleChest(level, random, box, 20, 10, 18,
                            ModLootTables.SUSPENDED_SURVEY_STATION_CHEST);
                }
                case FRACTURED_ANCHOR_STATION -> {
                    buildAccessibleChest(level, random, box, 19, 1, 19,
                            ModLootTables.FRACTURED_ANCHOR_STATION_CHEST);
                    buildAccessibleChest(level, random, box, 19, 1, 33,
                            ModLootTables.FRACTURED_ANCHOR_STATION_CHEST);
                }
                case SEVERED_ARCHIVE_VAULT -> {
                    buildAccessibleChest(level, random, box, 21, 1, 18,
                            ModLootTables.PUZZLE_SEVERED_ARCHIVE_VAULT_CHEST);
                    buildAccessibleChest(level, random, box, 8, 1, 18,
                            ModLootTables.SEVERED_ARCHIVE_VAULT_CHEST, Direction.EAST);
                }
                case RESONANCE_FOUNDRY -> {
                    for (int[] point : new int[][]{{8, 1, 30}, {52, 1, 30},
                            {30, 1, 8}, {30, 1, 52}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.RESONANCE_FOUNDRY_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 30, 9, 30,
                            ModLootTables.RESONANCE_FOUNDRY_VAULT);
                }
                case GRAVITY_CROWN_OBSERVATORY -> {
                    for (int[] point : new int[][]{{28, 5, 8}, {28, 5, 53},
                            {8, 5, 33}, {48, 5, 23}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.GRAVITY_CROWN_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 28, 9, 28,
                            ModLootTables.GRAVITY_CROWN_VAULT);
                }
                case VOIDSCAR_BASTION -> {
                    for (int[] point : new int[][]{{12, 1, 12}, {52, 1, 12},
                            {12, 1, 52}, {52, 1, 52}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.VOIDSCAR_BASTION_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 32, 1, 32,
                            ModLootTables.VOIDSCAR_BASTION_VAULT);
                }
                case SHATTERED_CODEX_VAULT -> {
                    buildAccessibleChest(level, random, box, 33, 1, 9,
                            ModLootTables.PUZZLE_SHATTERED_CODEX_CACHE);
                    for (int[] point : new int[][]{{33, 1, 57},
                            {9, 1, 33}, {57, 1, 33}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.SHATTERED_CODEX_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 33, 8, 33,
                            ModLootTables.SHATTERED_CODEX_VAULT);
                }
                case WEIGHTLESS_ORRERY_COURT -> {
                    for (int[] point : new int[][]{{36, 5, 7}, {26, 5, 55},
                            {7, 5, 31}, {55, 5, 36}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.WEIGHTLESS_ORRERY_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 31, 11, 35,
                            ModLootTables.WEIGHTLESS_ORRERY_VAULT);
                }
                case HUSHED_CATHEDRAL -> {
                    buildAccessibleChest(level, random, box, 34, 2, 10,
                            ModLootTables.PUZZLE_HUSHED_CATHEDRAL_CACHE);
                    for (int[] point : new int[][]{{34, 2, 58},
                            {10, 2, 34}, {58, 2, 34}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.HUSHED_CATHEDRAL_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 34, 9, 34,
                            ModLootTables.HUSHED_CATHEDRAL_VAULT);
                }
                case RIFTGLASS_CITADEL -> {
                    for (int[] point : new int[][]{{36, 2, 8}, {36, 2, 64},
                            {8, 2, 36}, {64, 2, 36}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.RIFTGLASS_CITADEL_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 36, 9, 36,
                            ModLootTables.RIFTGLASS_CITADEL_VAULT);
                }
                case FOLDED_CELESTIAL_ARCHIVE -> {
                    buildAccessibleChest(level, random, box, 9, 2, 35,
                            ModLootTables.PUZZLE_FOLDED_CELESTIAL_ARCHIVE_CACHE);
                    for (int[] point : new int[][]{{35, 2, 9}, {35, 2, 61}, {61, 2, 35}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.FOLDED_CELESTIAL_ARCHIVE_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 35, 10, 35,
                            ModLootTables.FOLDED_CELESTIAL_ARCHIVE_VAULT);
                }
                case INVERTED_PENDULUM_PALACE -> {
                    for (int[] point : new int[][]{{37, 2, 9}, {37, 2, 65},
                            {9, 2, 37}, {65, 2, 37}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.INVERTED_PENDULUM_PALACE_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 37, 11, 37,
                            ModLootTables.INVERTED_PENDULUM_PALACE_VAULT);
                }
                case RIFTWORN_MEMORIAL_COURT -> {
                    for (int[] point : new int[][]{{39, 3, 10}, {39, 3, 68},
                            {10, 3, 39}, {68, 3, 39}}) {
                        buildAccessibleChest(level, random, box, point[0], point[1], point[2],
                                ModLootTables.RIFTWORN_MEMORIAL_COURT_CACHE);
                    }
                    buildAccessibleChest(level, random, box, 39, 9, 39,
                            ModLootTables.RIFTWORN_MEMORIAL_COURT_VAULT);
                }
            }
        }

        private void buildFoldedArchive(WorldGenLevel level, BoundingBox box) {
            int center = 15;
            BlockState floor = ModBlocks.FOLDED_SLATE.get().defaultBlockState();
            BlockState wall = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState carved = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 29, 29, floor);
            for (int cursor = 2; cursor <= 28; cursor++) {
                boolean gate = Math.abs(cursor - center) <= 2;
                for (int y = 1; y <= 7; y++) {
                    if (!gate || y > 4) {
                        BlockState state = y == 4 || y == 7 || cursor % 6 == 0 ? rune : wall;
                        placeBlock(level, state, cursor, y, 2, box);
                        placeBlock(level, state, cursor, y, 28, box);
                        placeBlock(level, state, 2, y, cursor, box);
                        placeBlock(level, state, 28, y, cursor, box);
                    }
                }
            }
            for (int[] tower : new int[][]{{4, 4}, {26, 4}, {4, 26}, {26, 26}}) {
                pillar(level, box, tower[0], 1, tower[1], 13, carved);
                for (int y = 4; y <= 12; y += 4) {
                    placeBlock(level, rune, tower[0], y, tower[1], box);
                }
                placeBlock(level, light, tower[0], 14, tower[1], box);
            }

            for (int offset = -2; offset <= 2; offset++) {
                for (int cursor = 3; cursor <= 27; cursor++) {
                    BlockState path = Math.abs(offset) == 2 || cursor % 6 == 0 ? rune : floor;
                    placeBlock(level, path, center + offset, 0, cursor, box);
                    placeBlock(level, path, cursor, 0, center + offset, box);
                }
            }
            for (int z : new int[]{6, 11, 19, 24}) {
                for (int x = 4; x <= 26; x++) {
                    if (Math.abs(x - center) <= 2) {
                        continue;
                    }
                    if (x % 4 != 0) {
                        pillar(level, box, x, 1, z, 4,
                                Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                    }
                    placeBlock(level, x % 5 == 0 ? light : carved, x, 7, z, box);
                }
            }

            disk(level, box, center, 1, center, 9, wall);
            annulus(level, box, center, 1, center, 7, 9, rune);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 1; step <= 7; step++) {
                    int distance = 12 - step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step,
                                Math.abs(lateral) == 2 ? rune : floor);
                    }
                }
            }
            disk(level, box, center, 7, center, 5, floor);
            annulus(level, box, center, 8, center, 4, 5, carved);
            for (int[] pylon : new int[][]{{10, 10}, {20, 10}, {10, 20}, {20, 20}}) {
                pillar(level, box, pylon[0], 2, pylon[1], 11, wall);
                placeBlock(level, rune, pylon[0], 8, pylon[1], box);
                placeBlock(level, light, pylon[0], 13, pylon[1], box);
            }
            circularRing(level, box, center, 12, center, 6, rune, 4);
            circularRing(level, box, center, 15, center, 4, light, 11);
        }

        private void buildInvertedBridge(WorldGenLevel level, BoundingBox box) {
            int centerX = 18;
            int centerZ = 12;
            BlockState cliff = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState bridge = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 2, 11, 21, cliff);
            platform(level, box, 25, 0, 2, 11, 21, cliff);
            for (int[] tower : new int[][]{{3, 4}, {9, 20}, {27, 20}, {33, 4}}) {
                pillar(level, box, tower[0], 1, tower[1], 15, bridge);
                for (int y = 5; y <= 13; y += 4) {
                    placeBlock(level, rune, tower[0], y, tower[1], box);
                }
                placeBlock(level, light, tower[0], 16, tower[1], box);
            }
            for (int step = 1; step <= 4; step++) {
                for (int x : new int[]{7 + step, 29 - step}) {
                    for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                        pillar(level, box, x, 1, z, step,
                                Math.abs(z - centerZ) == 2 ? rune : bridge);
                    }
                }
            }
            for (int x = 11; x <= 25; x++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    BlockState state = Math.abs(z - centerZ) == 2 || x % 5 == 0 ? rune : bridge;
                    placeBlock(level, state, x, 4, z, box);
                }
                if (x % 4 == 0) {
                    pillar(level, box, x, 1, centerZ - 3, 4, band);
                    pillar(level, box, x, 1, centerZ + 3, 4, band);
                    placeBlock(level, light, x, 5, centerZ - 3, box);
                    placeBlock(level, light, x, 5, centerZ + 3, box);
                }
            }
            for (int x = 9; x <= 27; x++) {
                if (x % 6 != 0) {
                    placeBlock(level, band, x, 9, 6, box);
                    placeBlock(level, x % 4 == 0 ? light : rune, x, 10, 6, box);
                }
            }
            for (int x = 3; x <= 33; x += 5) {
                int depth = 7 + Math.abs(centerX - x) / 3;
                for (int y = 22; y >= depth; y--) {
                    placeBlock(level, y == depth ? light : cliff, x, y, centerZ, box);
                }
            }
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    centerX, 5, centerZ, box);
            placeBlock(level, ModBlocks.DEEP_PROVISION_BRAZIER.get().defaultBlockState(),
                    6, 1, 6, box);
            buildConnectedDoor(level, box, ModBlocks.GRAVITY_FOLD_DOOR.get().defaultBlockState(),
                    25, 5, centerZ - 1, Direction.EAST, 3, 4);
        }

        private void buildSilentAnchor(WorldGenLevel level, BoundingBox box) {
            int center = 15;
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState sealed = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState light = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();

            disk(level, box, center, 0, center, 14, moss);
            annulus(level, box, center, 0, center, 10, 14, sealed);
            brokenCircularWall(level, box, center, center, 13, 1, 5, wall, 8, true);
            for (int index = 0; index < 8; index++) {
                double angle = index * Math.PI / 4.0D;
                int x = center + (int) Math.round(Math.cos(angle) * 12.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 12.0D);
                int height = 9 + index % 3 * 2;
                pillar(level, box, x, 1, z, height, index % 2 == 0 ? sealed : wall);
                placeBlock(level, rune, x, height, z, box);
                placeBlock(level, light, x, height + 1, z, box);
            }
            annulus(level, box, center, 1, center, 5, 8, rune);
            disk(level, box, center, 1, center, 4, sealed);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 2, center, box);

            int[][] anchors = {{center, 10}, {8, 22}, {22, 22}};
            for (int[] anchor : anchors) {
                disk(level, box, anchor[0], 1, anchor[1], 3, sealed);
                annulus(level, box, anchor[0], 2, anchor[1], 2, 3, rune);
                placeBlock(level, sealed, anchor[0], 2, anchor[1], box);
                placeBlock(level, ModBlocks.SPACE_ANCHOR.get().defaultBlockState(),
                        anchor[0], 3, anchor[1], box);
                for (int side : new int[]{-3, 3}) {
                    pillar(level, box, anchor[0] + side, 2, anchor[1], 8, wall);
                    placeBlock(level, light, anchor[0] + side, 10, anchor[1], box);
                }
                for (int offset = -3; offset <= 3; offset++) {
                    int y = 11 + (3 - Math.abs(offset)) / 2;
                    placeBlock(level, offset == 0 ? light : rune,
                            anchor[0] + offset, y, anchor[1], box);
                }
            }
            circularRing(level, box, center, 13, center, 9, rune, 3);
            circularRing(level, box, center, 15, center, 6, light, 10);
        }

        private void buildCalibrationHall(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState lamp = ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState();
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
                        int wallHeight = 12 + Math.floorMod(x + z, 3);
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

            buildRibbedVaultHall(level, box, center, 1, center, Footprint.of(14, 19), 13, 5, floor, wall, rune, lamp);
            buildTwinTowerFacade(level, box, center, 1, 2,
                    17, 3, 18, wall, rune, lamp);
            buildArcadedWing(level, box, 5, 1, center, Footprint.of(3, 13), 11, 4, floor, wall, rune, lamp);
            buildArcadedWing(level, box, 39, 1, center, Footprint.of(3, 13), 11, 4, floor, wall, rune, lamp);
            buildBossApse(level, box, center, 1, center + 14,
                    Direction.SOUTH, 6, 12, floor, wall, rune, lamp);
            buildCalibrationArches(level, box, center, 18, 10, wall, rune, lamp);
            buildMonumentGates(level, box, center, 20, 11, wall, rune, lamp);
            // 鼓座半径 20 必须大于战斗半径 18,且穹顶不落地柱,否则占走位区。
            buildDomedRotunda(level, box, center, 1, center,
                    20, 12, floor, wall, rune, lamp);

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
            // 肋拱厅柱列、侧廊与后殿都会插进 18 格战斗半径,建筑摆完后统一清出净空。
            clearArenaVolume(level, box, center, 0, center, 18, 1, 8);
            buildBossArenaMandala(level, box, center, 0, center, 18,
                    rune, wall, ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState());
            buildArenaLightRing(level, box, center, 3, center, 20, 12, 10, wall, lamp);
            illuminateInterior(level, box, center, center, 18, 13, 18, 6, lamp);
            buildStairCornice(level, box, center, 15, center, Footprint.of(14, 19), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 16, center, Footprint.of(14, 19), wall, rune);
            buildParapet(level, box, center, 17, center, Footprint.of(14, 19), 8, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), rune);
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
                        int wallHeight = 12 + Math.floorMod(x * 17 + z * 7, 5);
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

            buildRibbedVaultHall(level, box, center, 1, center, Footprint.of(16, 21), 14, 6, floor, wall, vein, crystal);
            buildTwinTowerFacade(level, box, center, 1, 2,
                    18, 4, 20, wall, vein, crystal);
            buildArcadedWing(level, box, 6, 1, center + 3, Footprint.of(4, 14), 11, 4, floor, wall, vein, crystal);
            buildArcadedWing(level, box, 42, 1, center + 3, Footprint.of(4, 14), 11, 4, floor, wall, vein, crystal);
            buildBossApse(level, box, center, 1, center + 16,
                    Direction.SOUTH, 7, 12, floor, wall, vein, crystal);
            buildMonumentGates(level, box, center, 22, 12, wall, vein, crystal);

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
            BlockState hushLamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
            // coreRadius=21 > 场地战斗半径 20:退台只在场地外围收分。
            buildSteppedZiggurat(level, box, center, 1, center,
                    23, 3, 4, 21, wall, vein,
                    ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), hushLamp);
            // 悬挑锚点必须偏离场地中心:挂在正上方会盖出顶板压住场地核心。
            // 偏移上限受 49 格足印约束,+24 会被裁掉一半。
            buildCantileverTerraces(level, box, center + 8, 12, center,
                    Direction.EAST, 3, 8, 4, floor, wall, vein, hushLamp);
            // 肋拱厅的柱列会插进 20 格战斗半径,建筑摆完后统一清出净空。
            clearArenaVolume(level, box, center, 0, center, 20, 1, 8);
            buildBossArenaMandala(level, box, center, 0, center, 20,
                    vein, floor, crystal);

            BlockState lamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
            buildArenaLightRing(level, box, center, 3, center, 22, 12, 11, wall, lamp);
            illuminateInterior(level, box, center, center, 19, 15, 20, 6, lamp);
            buildStairCornice(level, box, center, 17, center, Footprint.of(16, 21), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 18, center, Footprint.of(16, 21), wall, vein);
            buildParapet(level, box, center, 19, center, Footprint.of(16, 21), 7, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), vein);
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), center, 1, 11, box);
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), 13, 1, 31, box);
            placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(), 35, 1, 31, box);
            placeBlock(level, ModBlocks.VOID_CORE_ALTAR.get().defaultBlockState(), center, 1, center, box);
        }

        private void buildWorldResonanceHall(WorldGenLevel level, BoundingBox box) {
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState floor = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState[] themes = {
                    ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState(),
                    ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState(),
                    ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState(),
                    ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState()
            };
            int center = WorldResonanceLayout.GRAND_CENTER;
            int outerRadius = 39;

            for (int x = 1; x < WorldResonanceLayout.GRAND_SIZE - 1; x++) {
                for (int z = 1; z < WorldResonanceLayout.GRAND_SIZE - 1; z++) {
                    int dx = x - center;
                    int dz = z - center;
                    double distance = Math.sqrt(square(dx) + square(dz));
                    if (distance <= outerRadius) {
                        BlockState state = distance >= 35.0D ? anchored
                                : isGrandFloorRing(distance) ? rune : floor;
                        placeBlock(level, state, x, 0, z, box);
                    }
                    if (distance >= 37.2D && distance <= outerRadius
                            && !isArenaEntrance(dx, dz, 36, 5)) {

                        int wallHeight = 22 + (Math.floorMod(x * 7 + z * 3, 11) == 0 ? 3 : 0);
                        for (int y = 1; y <= wallHeight; y++) {
                            BlockState state = y == 4 || y == 8 || y == 14 || y == 19
                                    || y == wallHeight
                                    || Math.floorMod(x + z + y, 17) == 0 ? rune : wall;
                            placeBlock(level, state, x, y, z, box);
                        }
                    }
                }
            }

            int[][] pylons = {
                    {center, center - 27},
                    {center + 26, center - 8},
                    {center + 16, center + 22},
                    {center - 16, center + 22},
                    {center - 26, center - 8}
            };
            for (int i = 0; i < pylons.length; i++) {
                buildGrandResonancePath(level, box, center, center,
                        pylons[i][0], pylons[i][1], themes[i], rune);
                buildGrandEchoPylon(level, box, pylons[i][0], pylons[i][1],
                        themes[i], wall, rune, lamp);
                buildConvergenceArch(level, box, center, center,
                        pylons[i][0], pylons[i][1], rune, lamp, i);
            }

            for (int index = 0; index < 12; index++) {
                double angle = Math.toRadians(15.0D + index * 30.0D);
                int x = center + (int) Math.round(Math.cos(angle) * 37.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 37.0D);
                buildResonanceBoundaryTower(level, box, x, z, anchored, wall, rune, lamp, index);
            }
            for (Direction direction : new Direction[]{
                    Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                buildResonanceGate(level, box, center, direction, wall, rune, lamp);
            }

            circularRing(level, box, center, 26, center, 20, rune, 0);
            circularRing(level, box, center, 27, center, 20, lamp, 7);
            circularRing(level, box, center, 31, center, 28, rune, 3);
            circularRing(level, box, center, 32, center, 28, lamp, 11);
            annulus(level, box, center, 0, center, 5, 8, rune);
            disk(level, box, center, 0, center, 4, anchored);
            buildBossArenaMandala(level, box, center, 0, center, 34,
                    rune, wall, anchored);
            buildArenaLightRing(level, box, center, 1, center, 33, 16, 6, wall, lamp);

            circularRing(level, box, center, 24, center, 26, lamp, 5);

            BlockState grandStairs = ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState();
            BlockState grandSlab = ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState();
            buildCircularPlinth(level, box, center, 0, center, 37, wall, grandStairs);

            buildCircularCornice(level, box, center, 23, center, 38, grandStairs, grandSlab, true);
            buildCircularParapet(level, box, center, 24, center, 38, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState());

            buildCircularCornice(level, box, center, 12, center, 38, grandStairs, grandSlab, true);
            for (int index = 0; index < 12; index++) {
                double angle = Math.toRadians(15.0D + index * 30.0D);
                int towerX = center + (int) Math.round(Math.cos(angle) * 37.0D);
                int towerZ = center + (int) Math.round(Math.sin(angle) * 37.0D);

                buildSpireFinial(level, box, towerX, 28, towerZ, 2, 4, wall, rune, lamp);
            }
            buildEndingRecordSanctuary(level, box, center, wall, rune, lamp);

            BlockState altar = ModBlocks.WORLD_RESONANCE_ALTAR.get().defaultBlockState()
                    .setValue(WorldResonanceAltarBlock.FACING, Direction.NORTH)
                    .setValue(WorldResonanceAltarBlock.GRAND, true);
            placeBlock(level, altar, center, 1, center, box);
            placeBlock(level, ModBlocks.ENDING_RECORD_TABLE.get().defaultBlockState(),
                    center, 1, center + 30, box);
        }

        private void buildFoldedWaystation(WorldGenLevel level, BoundingBox box) {
            int center = 12;
            BlockState floor = ModBlocks.FOLDED_SLATE.get().defaultBlockState();
            BlockState wall = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState carved = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 23, 23, floor);
            for (int frameIndex = 0; frameIndex < 3; frameIndex++) {
                int z = 4 + frameIndex * 8;
                int height = frameIndex == 1 ? 13 : 10;
                pillar(level, box, 3, 1, z, height, wall);
                pillar(level, box, 21, 1, z, height, wall);
                for (int x = 3; x <= 21; x++) {
                    int archY = height + (int) Math.round(3.0D
                            * Math.sqrt(Math.max(0.0D, 1.0D - square(x - center) / 81.0D)));
                    placeBlock(level, x == center ? light : x % 4 == 0 ? rune : carved,
                            x, archY, z, box);
                }
            }
            for (int z = 3; z <= 21; z++) {
                for (int lateral = -2; lateral <= 2; lateral++) {
                    BlockState path = Math.abs(lateral) == 2 || z % 6 == 0 ? rune : floor;
                    placeBlock(level, path, center + lateral, 0, z, box);
                }
            }
            for (int offset = -8; offset <= 8; offset++) {
                if (Math.abs(offset) > 2) {
                    placeBlock(level, offset % 3 == 0 ? light : rune,
                            center + offset, 6, center + offset, box);
                    placeBlock(level, offset % 4 == 0 ? carved : wall,
                            center + offset, 7, center - offset, box);
                }
            }
            platform(level, box, 8, 2, 8, 9, 9, carved);
            annulus(level, box, center, 3, center, 3, 5, rune);
            for (int[] pillar : new int[][]{{8, 8}, {16, 8}, {8, 16}, {16, 16}}) {
                pillar(level, box, pillar[0], 3, pillar[1], 8, wall);
                placeBlock(level, light, pillar[0], 11, pillar[1], box);
            }
            for (int[] shelf : new int[][]{{10, 10}, {14, 10}, {10, 14}, {14, 14}}) {
                pillar(level, box, shelf[0], 3, shelf[1], 5,
                        Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                placeBlock(level, rune, shelf[0], 8, shelf[1], box);
            }
            circularRing(level, box, center, 12, center, 6, rune, 6);
            circularRing(level, box, center, 14, center, 4, light, 13);
            placeBlock(level, ModBlocks.DEEP_PROVISION_BRAZIER.get().defaultBlockState(),
                    center, 3, center, box);
        }

        private void buildGravityShardSpire(WorldGenLevel level, BoundingBox box) {
            int center = 13;
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState frame = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            disk(level, box, center, 0, center, 12, floor);
            annulus(level, box, center, 0, center, 9, 12, frame);
            brokenCircularWall(level, box, center, center, 12, 1, 4, band, 5, true);
            for (int index = 0; index < 8; index++) {
                double angle = Math.PI / 8.0D + index * Math.PI / 4.0D;
                int x = center + (int) Math.round(Math.cos(angle) * 10.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 10.0D);
                int height = 10 + index % 4 * 2;
                pillar(level, box, x, 1, z, height, index % 2 == 0 ? frame : band);
                placeBlock(level, rune, x, height, z, box);
                placeBlock(level, light, x, height + 1, z, box);
            }
            disk(level, box, center, 1, center, 5, band);
            annulus(level, box, center, 2, center, 3, 5, rune);
            for (int x = center - 1; x <= center + 1; x++) {
                for (int z = center - 1; z <= center + 1; z++) {
                    if (x == center && z == center) {
                        continue;
                    }
                    pillar(level, box, x, 3, z, 25, (x + z) % 2 == 0 ? frame : rune);
                }
            }
            pillar(level, box, center, 3, center, 26, light);
            circularRing(level, box, center, 8, center, 10, rune, 2);
            circularRing(level, box, center, 15, center, 7, light, 9);
            circularRing(level, box, center, 22, center, 4, band, 15);
            orbitalRing(level, box, center, center, 6, 25, 3, rune, Math.PI / 3.0D);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 2, center, box);
            placeBlock(level, light, center, 30, center, box);
        }

        private void buildHushedRefuge(WorldGenLevel level, BoundingBox box) {
            int center = 14;
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState sealed = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState light = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 27, 27, moss);
            for (int edge = 2; edge <= 26; edge++) {
                boolean opening = Math.abs(edge - center) <= 2;
                for (int y = 1; y <= 6; y++) {
                    if ((!opening || y > 4) && (edge + y) % 11 != 0) {
                        BlockState state = y == 3 || y == 6 ? rune : wall;
                        placeBlock(level, state, edge, y, 2, box);
                        placeBlock(level, state, edge, y, 26, box);
                        placeBlock(level, state, 2, y, edge, box);
                        placeBlock(level, state, 26, y, edge, box);
                    }
                }
            }
            for (int[] tower : new int[][]{{4, 4}, {24, 4}, {4, 24}, {24, 24}}) {
                pillar(level, box, tower[0], 1, tower[1], 11, sealed);
                placeBlock(level, rune, tower[0], 7, tower[1], box);
                placeBlock(level, light, tower[0], 12, tower[1], box);
            }
            for (int offset = -2; offset <= 2; offset++) {
                for (int cursor = 3; cursor <= 25; cursor++) {
                    BlockState path = Math.abs(offset) == 2 || cursor % 7 == 0 ? rune : moss;
                    placeBlock(level, path, center + offset, 0, cursor, box);
                    placeBlock(level, path, cursor, 0, center + offset, box);
                }
            }
            disk(level, box, center, 1, center, 7, sealed);
            annulus(level, box, center, 1, center, 5, 7, rune);
            for (int[] point : new int[][]{{9, 9}, {19, 9}, {9, 19}, {19, 19}}) {
                pillar(level, box, point[0], 2, point[1], 10, wall);
                placeBlock(level, rune, point[0], 7, point[1], box);
                placeBlock(level, light, point[0], 12, point[1], box);
            }
            for (int x = 8; x <= 20; x++) {
                placeBlock(level, x % 4 == 0 ? light : rune, x, 12, 9, box);
                placeBlock(level, x % 4 == 0 ? light : rune, x, 12, 19, box);
            }
            for (int z = 9; z <= 19; z++) {
                placeBlock(level, z % 4 == 0 ? light : rune, 9, 12, z, box);
                placeBlock(level, z % 4 == 0 ? light : rune, 19, 12, z, box);
            }
            circularRing(level, box, center, 14, center, 7, rune, 4);
            circularRing(level, box, center, 16, center, 4, light, 12);
            placeBlock(level, ModBlocks.DEEP_PROVISION_BRAZIER.get().defaultBlockState(),
                    center, 2, center - 3, box);
        }

        private void buildMisfiledCatalogWell(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.FOLDED_SLATE.get().defaultBlockState();
            BlockState wall = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState resonant = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            int center = 17;

            disk(level, box, center, 0, center, 16, floor);
            annulus(level, box, center, 0, center, 12, 16, chiseled);
            brokenCircularWall(level, box, center, center, 16, 1, 7, wall, 3, true);
            for (int[] point : new int[][]{{5, 5}, {29, 5}, {5, 29}, {29, 29}}) {
                pillar(level, box, point[0], 1, point[1], 14, chiseled);
                for (int y = 4; y <= 12; y += 4) {
                    placeBlock(level, resonant, point[0], y, point[1], box);
                }
                placeBlock(level, light, point[0], 15, point[1], box);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 8; step <= 15; step++) {
                    int x = center + dx * step;
                    int z = center + dz * step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int pathX = x + dz * lateral;
                        int pathZ = z - dx * lateral;
                        placeBlock(level, Math.abs(lateral) == 2 ? resonant : floor,
                                pathX, 0, pathZ, box);
                    }
                }
            }

            buildVaultShell(level, box, 12, 22, 12, 22, 0, 10,
                    Direction.SOUTH, anchored, ModBlocks.SEALED_STONE.get().defaultBlockState());
            circularRing(level, box, center, 7, center, 11, resonant, 1);
            circularRing(level, box, center, 12, center, 14, chiseled, 8);
            orbitalRing(level, box, center, center, 9, 17, 4, light, Math.PI / 4.0D);
            for (int index = 0; index < 8; index++) {
                double angle = index * Math.PI / 4.0D;
                int x = center + (int) Math.round(Math.cos(angle) * 11.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 11.0D);
                int height = 8 + index % 3 * 2;
                pillar(level, box, x, 1, z, height, index % 2 == 0 ? wall : chiseled);
                placeBlock(level, resonant, x, height, z, box);
            }

            int[][] runes = {
                    {10, 2, 28}, {13, 2, 31}, {17, 2, 32},
                    {21, 2, 31}, {24, 2, 28}, {8, 2, 24}, {26, 2, 24}
            };
            int[][] hints = {
                    {14, 9, 24}, {15, 10, 24}, {16, 9, 24},
                    {17, 11, 24}, {18, 9, 24}, {19, 10, 24}, {20, 9, 24}
            };
            installSequencePuzzle(level, box, 17, 2, 26,
                    runes, hints, 6, chiseled, 0x29A34D31L);
        }

        private void buildFoldedScriptorium(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.FOLDED_SLATE.get().defaultBlockState();
            BlockState wall = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState resonant = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            int centerX = 19;
            int centerZ = 16;

            platform(level, box, 1, 0, 1, 37, 31, floor);
            for (int x = 2; x <= 36; x++) {
                boolean gate = Math.abs(x - centerX) <= 2;
                for (int y = 1; y <= 7; y++) {
                    if (!gate || y > 4) {
                        BlockState state = y == 4 || y == 7 || x % 7 == 0 ? resonant : wall;
                        placeBlock(level, state, x, y, 2, box);
                        placeBlock(level, state, x, y, 30, box);
                    }
                }
            }
            for (int z = 2; z <= 30; z++) {
                boolean gate = Math.abs(z - centerZ) <= 2;
                for (int y = 1; y <= 7; y++) {
                    if (!gate || y > 4) {
                        BlockState state = y == 4 || y == 7 || z % 7 == 0 ? resonant : wall;
                        placeBlock(level, state, 2, y, z, box);
                        placeBlock(level, state, 36, y, z, box);
                    }
                }
            }
            for (int[] tower : new int[][]{{4, 4}, {34, 4}, {4, 28}, {34, 28}}) {
                pillar(level, box, tower[0], 1, tower[1], 15, chiseled);
                placeBlock(level, resonant, tower[0], 8, tower[1], box);
                placeBlock(level, light, tower[0], 16, tower[1], box);
            }

            for (int z : new int[]{6, 12, 20, 26}) {
                for (int x = 4; x <= 34; x++) {
                    if (Math.abs(x - centerX) <= 2) {
                        continue;
                    }
                    if (x % 5 != 0) {
                        pillar(level, box, x, 1, z, 5,
                                Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                    }
                    placeBlock(level, x % 4 == 0 ? light : chiseled, x, 8, z, box);
                }
            }
            for (int offset = -2; offset <= 2; offset++) {
                for (int cursor = 3; cursor <= 29; cursor++) {
                    placeBlock(level, Math.abs(offset) == 2 || cursor % 6 == 0 ? resonant : floor,
                            centerX + offset, 0, cursor, box);
                }
            }

            platform(level, box, 3, 7, 21, 11, 8, chiseled);
            annulus(level, box, 8, 8, 25, 3, 6, resonant);
            for (int step = 1; step <= 7; step++) {
                int z = 14 + step;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    pillar(level, box, 10 + lateral, 1, z, step,
                            Math.abs(lateral) == 2 ? resonant : floor);
                }
            }
            for (int[] pylon : new int[][]{{4, 21}, {13, 21}, {4, 29}, {13, 29}}) {
                pillar(level, box, pylon[0], 8, pylon[1], 8, wall);
                placeBlock(level, light, pylon[0], 16, pylon[1], box);
            }

            buildVaultShell(level, box, 28, 36, 20, 30, 0, 11,
                    Direction.WEST, ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState(),
                    ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState());
            pillar(level, box, centerX, 1, centerZ, 12,
                    Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            for (int[] brace : new int[][]{{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
                pillar(level, box, centerX + brace[0], 1, centerZ + brace[1], 10, chiseled);
                placeBlock(level, light, centerX + brace[0], 11, centerZ + brace[1], box);
            }
            circularRing(level, box, centerX, 13, centerZ, 7, resonant, 5);
            orbitalRing(level, box, centerX, centerZ, 5, 16, 2, light, Math.PI / 2.0D);
        }

        private void buildInvertedArchiveTower(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState wall = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState banded = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            int center = 17;

            disk(level, box, center, 0, center, 16, floor);
            annulus(level, box, center, 0, center, 12, 16, wall);
            brokenCircularWall(level, box, center, center, 16, 1, 6, banded, 9, true);
            for (int index = 0; index < 8; index++) {
                double angle = Math.PI / 8.0D + index * Math.PI / 4.0D;
                int x = center + (int) Math.round(Math.cos(angle) * 14.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 14.0D);
                pillar(level, box, x, 1, z, 34, index % 2 == 0 ? wall : banded);
                for (int y = 6; y <= 32; y += 6) {
                    placeBlock(level, index % 3 == 0 ? crystal : rune, x, y, z, box);
                }
                placeBlock(level, crystal, x, 35, z, box);
            }
            int[] levels = {8, 16, 24, 32};
            int[] radii = {13, 11, 9, 7};
            for (int index = 0; index < levels.length; index++) {
                int levelY = levels[index];
                int radius = radii[index];
                disk(level, box, center, levelY, center, radius,
                        index % 2 == 0 ? wall : banded);
                annulus(level, box, center, levelY + 1, center,
                        Math.max(2, radius - 2), radius, rune);
                circularRing(level, box, center, levelY + 5, center,
                        Math.max(3, radius - 2), index % 2 == 0 ? crystal : banded, index * 5);
            }
            buildTowerRamp(level, box, center, 0, 16, Direction.NORTH, floor, rune);
            buildTowerRamp(level, box, center, 8, 14, Direction.EAST, wall, rune);
            buildTowerRamp(level, box, center, 16, 12, Direction.SOUTH, banded, rune);
            buildTowerRamp(level, box, center, 24, 10, Direction.WEST, wall, rune);
            for (int[] spine : new int[][]{{15, 15}, {19, 15}, {15, 19}, {19, 19}}) {
                pillar(level, box, spine[0], 1, spine[1], 37, wall);
                for (int y = 7; y <= 35; y += 7) {
                    placeBlock(level, crystal, spine[0], y, spine[1], box);
                }
            }
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 1, center - 4, box);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center + 4, 25, center, box);
            orbitalRing(level, box, center, center, 10, 27, 6, crystal, Math.PI / 3.0D);
            placeBlock(level, crystal, center, 38, center, box);
        }

        private void buildTowerRamp(WorldGenLevel level, BoundingBox box,
                                    int center, int baseY, int outerDistance,
                                    Direction direction, BlockState floor, BlockState rune) {
            int dx = direction.getStepX();
            int dz = direction.getStepZ();
            for (int step = 1; step <= 8; step++) {
                int distance = outerDistance - step;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    int x = center + dx * distance + dz * lateral;
                    int z = center + dz * distance - dx * lateral;
                    pillar(level, box, x, 1, z, baseY + step,
                            Math.abs(lateral) == 2 ? rune : floor);
                }
            }
        }

        private void buildSuspendedSurveyStation(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState wall = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState banded = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            int centerX = 20;
            int centerZ = 18;

            disk(level, box, centerX, 0, centerZ, 14, floor);
            platform(level, box, 2, 0, 16, 37, 5, floor);
            platform(level, box, 18, 0, 2, 5, 33, floor);
            for (int[] satellite : new int[][]{{7, 7}, {33, 7}, {7, 29}, {33, 29}}) {
                buildSurveySatellite(level, box, satellite[0], satellite[1], wall, banded, rune, crystal);
                int dx = Integer.compare(satellite[0], centerX);
                int dz = Integer.compare(satellite[1], centerZ);
                for (int step = 6; step <= 13; step++) {
                    int x = centerX + dx * step;
                    int z = centerZ + dz * step;
                    placeBlock(level, step % 3 == 0 ? crystal : banded, x, 5, z, box);
                }
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 1; step <= 9; step++) {
                    int distance = 18 - step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int x = centerX + dx * distance + dz * lateral;
                        int z = centerZ + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step,
                                Math.abs(lateral) == 2 ? rune : floor);
                    }
                }
            }
            disk(level, box, centerX, 9, centerZ, 8, wall);
            annulus(level, box, centerX, 10, centerZ, 6, 8, rune);
            for (int[] pylon : new int[][]{{13, 11}, {27, 11}, {13, 25}, {27, 25}}) {
                pillar(level, box, pylon[0], 1, pylon[1], 17, wall);
                placeBlock(level, rune, pylon[0], 10, pylon[1], box);
                placeBlock(level, crystal, pylon[0], 18, pylon[1], box);
            }
            circularRing(level, box, centerX, 15, centerZ, 11, rune, 3);
            orbitalRing(level, box, centerX, centerZ, 8, 18, 4, crystal, Math.PI / 2.0D);

            buildVaultShell(level, box, 15, 25, 2, 10, 0, 9,
                    Direction.SOUTH, anchored, ModBlocks.SEALED_STONE.get().defaultBlockState());

            int[][] runes = {
                    {12, 2, 13}, {15, 2, 15}, {20, 2, 16},
                    {25, 2, 15}, {28, 2, 13}, {16, 2, 11}, {24, 2, 11}
            };
            int[][] hints = {
                    {17, 10, 13}, {18, 10, 13}, {19, 10, 13},
                    {20, 10, 13}, {21, 10, 13}, {22, 10, 13}
            };
            installSequencePuzzle(level, box, 20, 2, 12,
                    runes, hints, 6, banded, 0x41D2C6A7L);
        }

        private void buildSurveySatellite(WorldGenLevel level, BoundingBox box,
                                          int centerX, int centerZ, BlockState wall,
                                          BlockState banded, BlockState rune, BlockState crystal) {
            disk(level, box, centerX, 0, centerZ, 6, wall);
            annulus(level, box, centerX, 1, centerZ, 4, 6, rune);
            for (int[] corner : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                pillar(level, box, centerX + corner[0], 1, centerZ + corner[1], 14, banded);
                placeBlock(level, crystal, centerX + corner[0], 15, centerZ + corner[1], box);
            }
            disk(level, box, centerX, 8, centerZ, 4, banded);
            circularRing(level, box, centerX, 12, centerZ, 5, crystal, centerX + centerZ);
            pillar(level, box, centerX, 1, centerZ, 13, rune);
        }

        private void buildFracturedAnchorStation(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState veined = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState crystal = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            int center = 19;

            disk(level, box, center, 0, center, 18, floor);
            annulus(level, box, center, 0, center, 14, 18, anchored);
            for (int radius : new int[]{7, 12, 18}) {
                brokenCircularWall(level, box, center, center, radius, 1,
                        radius == 18 ? 7 : radius == 12 ? 5 : 3,
                        radius == 12 ? crystal : radius == 7 ? veined : wall,
                        radius + 3, true);
            }
            for (int index = 0; index < 10; index++) {
                double angle = Math.PI / 10.0D + index * Math.PI / 5.0D;
                int x = center + (int) Math.round(Math.cos(angle) * 15.0D);
                int z = center + (int) Math.round(Math.sin(angle) * 15.0D);
                int height = 10 + index % 5 * 2;
                pillar(level, box, x, 1, z, height, index % 2 == 0 ? wall : veined);
                for (int y = 5; y < height; y += 5) {
                    placeBlock(level, anchored, x, y, z, box);
                }
                placeBlock(level, crystal, x, height, z, box);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 7; step <= 17; step++) {
                    int x = center + dx * step;
                    int z = center + dz * step;
                    for (int lateral = -1; lateral <= 1; lateral++) {
                        placeBlock(level, step % 3 == 0 ? crystal : veined,
                                x + dz * lateral, 0, z - dx * lateral, box);
                    }
                }
            }
            buildVaultShell(level, box, 13, 25, 13, 25, 0, 10,
                    Direction.SOUTH, ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState(),
                    ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState());
            circularRing(level, box, center, 11, center, 10, veined, 1);
            orbitalRing(level, box, center, center, 14, 15, 5, crystal, Math.PI / 5.0D);
            orbitalRing(level, box, center, center, 9, 19, 3, anchored, Math.PI / 2.0D);
            for (int[] shard : new int[][]{{7, 10}, {31, 8}, {9, 31}, {30, 29},
                    {4, 21}, {34, 19}}) {
                int height = 7 + Math.floorMod(shard[0] + shard[1], 7);
                pillar(level, box, shard[0], height, shard[1], 5, crystal);
            }
        }

        private void buildSeveredArchiveVault(WorldGenLevel level, BoundingBox box) {
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState veined = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState crystal = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();

            int centerX = 21;
            int centerZ = 18;
            platform(level, box, 1, 0, 1, 41, 35, floor);
            for (int x = 2; x <= 40; x++) {
                boolean gate = Math.abs(x - centerX) <= 2;
                for (int y = 1; y <= 8; y++) {
                    if ((!gate || y > 5) && Math.floorMod(x + y, 13) != 0) {
                        BlockState state = y == 4 || y == 8 ? veined : wall;
                        placeBlock(level, state, x, y, 2, box);
                        placeBlock(level, state, x, y, 34, box);
                    }
                }
            }
            for (int z = 2; z <= 34; z++) {
                boolean gate = Math.abs(z - centerZ) <= 2;
                for (int y = 1; y <= 8; y++) {
                    if ((!gate || y > 5) && Math.floorMod(z + y, 13) != 0) {
                        BlockState state = y == 4 || y == 8 ? veined : wall;
                        placeBlock(level, state, 2, y, z, box);
                        placeBlock(level, state, 40, y, z, box);
                    }
                }
            }
            for (int[] tower : new int[][]{{4, 4}, {38, 4}, {4, 32}, {38, 32}}) {
                pillar(level, box, tower[0], 1, tower[1], 17, anchored);
                for (int y = 5; y <= 15; y += 5) {
                    placeBlock(level, veined, tower[0], y, tower[1], box);
                }
                placeBlock(level, crystal, tower[0], 18, tower[1], box);
            }

            buildVaultShell(level, box, 7, 35, 5, 31, 0, 12,
                    Direction.SOUTH, anchored, ModBlocks.SEALED_STONE.get().defaultBlockState());
            buildVaultShell(level, box, 14, 28, 10, 26, 0, 15,
                    Direction.SOUTH, anchored, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState());
            // 晶石挂在内室顶板下方，不能嵌入封印边界的地板或侧墙。
            for (int z = 11; z <= 25; z += 2) {
                for (int x = 17; x <= 25; x += 2) {
                    placeBlock(level, crystal, x, 14, z, box);
                }
            }
            for (int[] point : new int[][]{{10, 9}, {32, 9}, {10, 27}, {32, 27}}) {
                pillar(level, box, point[0], 1, point[1], 16, wall);
                for (int y = 4; y <= 14; y += 5) {
                    placeBlock(level, veined, point[0], y, point[1], box);
                }
                placeBlock(level, crystal, point[0], 17, point[1], box);
            }
            for (int z = 5; z <= 31; z += 4) {
                for (int x = 7; x <= 35; x++) {
                    if (Math.floorMod(x + z, 5) != 0) {
                        placeBlock(level, Math.floorMod(x, 4) == 0 ? crystal : veined,
                                x, 18, z, box);
                    }
                }
            }
            for (int[] brace : new int[][]{{12, 18}, {30, 18}, {21, 8}, {21, 28}}) {
                pillar(level, box, brace[0], 1, brace[1], 19, anchored);
                placeBlock(level, crystal, brace[0], 20, brace[1], box);
            }
            circularRing(level, box, centerX, 17, centerZ, 11, crystal, 7);

            int[][] runes = {
                    {13, 2, 32}, {16, 2, 34}, {19, 2, 33}, {23, 2, 33},
                    {26, 2, 34}, {29, 2, 32}, {11, 2, 32}, {31, 2, 32}
            };
            int[][] hints = {
                    {17, 6, 32}, {18, 6, 32}, {19, 6, 32}, {20, 6, 32},
                    {21, 6, 32}, {22, 6, 32}, {23, 6, 32}
            };
            installSequencePuzzle(level, box, centerX, 2, 32,
                    runes, hints, 7, veined, 0x6B81F253L, Direction.SOUTH);
        }

        private void buildShatteredCodexVault(WorldGenLevel level, BoundingBox box) {
            int center = 33;
            BlockState floor = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState carved = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 65, 65, floor);
            annulus(level, box, center, 0, center, 24, 32, carved);
            annulus(level, box, center, 0, center, 18, 23, wall);
            brokenCircularWall(level, box, center, center, 31,
                    1, 6, wall, 14, true);
            buildMonumentGates(level, box, center, 31, 13, wall, rune, light);

            for (int offset = -2; offset <= 2; offset++) {
                for (int cursor = 2; cursor <= 64; cursor++) {
                    BlockState path = Math.abs(offset) == 2 || cursor % 8 == 0 ? rune : floor;
                    placeBlock(level, path, center + offset, 0, cursor, box);
                    placeBlock(level, path, cursor, 0, center + offset, box);
                }
            }

            buildCodexWing(level, box, center, 9, false, wall, carved, rune, light);
            buildCodexWing(level, box, center, 57, false, wall, carved, rune, light);
            buildCodexWing(level, box, 9, center, true, wall, carved, rune, light);
            buildCodexWing(level, box, 57, center, true, wall, carved, rune, light);
            for (int[] stack : new int[][]{{9, 9}, {57, 9}, {9, 57}, {57, 57}}) {
                buildCodexStack(level, box, stack[0], stack[1], wall, carved, rune, light);
            }

            disk(level, box, center, 1, center, 14, wall);
            annulus(level, box, center, 1, center, 10, 14, rune);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 1; step <= 7; step++) {
                    int distance = 15 - step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step,
                                Math.abs(lateral) == 2 ? rune : floor);
                    }
                }
            }
            disk(level, box, center, 7, center, 8, floor);
            annulus(level, box, center, 8, center, 6, 8, carved);
            for (int[] pylon : new int[][]{{center - 6, center - 6}, {center + 6, center - 6},
                    {center - 6, center + 6}, {center + 6, center + 6}}) {
                pillar(level, box, pylon[0], 8, pylon[1], 14, wall);
                for (int y = 10; y <= 20; y += 4) {
                    placeBlock(level, rune, pylon[0], y, pylon[1], box);
                }
                placeBlock(level, light, pylon[0], 22, pylon[1], box);
            }
            circularRing(level, box, center, 13, center, 7, rune, 2);
            circularRing(level, box, center, 17, center, 6, light, 9);
            circularRing(level, box, center, 21, center, 4, carved, 15);
            buildStairCornice(level, box, center, 7, center, Footprint.square(31), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 8, center, Footprint.square(31), wall, carved);
            buildParapet(level, box, center, 9, center, Footprint.square(31), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), carved);
            for (int[] stack : new int[][]{{9, 9}, {57, 9}, {9, 57}, {57, 57}}) {
                buildSpireFinial(level, box, stack[0], 17, stack[1], 3, 6, wall, rune, light);
            }
            illuminateInterior(level, box, center, center, 12, 8, 8, 4, ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
            buildProjectionRelayGallery(level, box, center, 1, 13, Direction.NORTH,
                    floor, wall, rune, light);
            buildSealedManor(level, box, 51, 1, 46, 5, 5, 3, 5,
                    wall, carved, floor,
                    Blocks.TINTED_GLASS.defaultBlockState(), light);
        }

        private void buildCodexWing(WorldGenLevel level, BoundingBox box,
                                    int centerX, int centerZ, boolean alongX,
                                    BlockState wall, BlockState carved,
                                    BlockState rune, BlockState light) {
            int halfX = alongX ? 8 : 6;
            int halfZ = alongX ? 6 : 8;
            for (int sx : new int[]{-halfX, halfX}) {
                for (int sz : new int[]{-halfZ, halfZ}) {
                    pillar(level, box, centerX + sx, 1, centerZ + sz, 11, wall);
                    placeBlock(level, light, centerX + sx, 12, centerZ + sz, box);
                }
            }
            for (int side : new int[]{-4, 4}) {
                for (int cursor = -5; cursor <= 5; cursor++) {
                    int x = alongX ? centerX + cursor : centerX + side;
                    int z = alongX ? centerZ + side : centerZ + cursor;
                    if (Math.abs(cursor) > 1) {
                        pillar(level, box, x, 1, z, 4, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                    }
                    placeBlock(level, cursor % 3 == 0 ? rune : carved, x, 7, z, box);
                }
            }
            for (int offset = -halfX + 1; offset < halfX; offset++) {
                int x = alongX ? centerX + offset : centerX;
                int z = alongX ? centerZ : centerZ + offset;
                if (offset != 0 && offset % 2 == 0) {
                    pillar(level, box, x, 1, z, 3, rune);
                    placeBlock(level, light, x, 4, z, box);
                }
            }
            if (alongX) {
                for (int x = centerX - halfX; x <= centerX + halfX; x++) {
                    placeBlock(level, x % 4 == 0 ? light : carved, x, 10, centerZ - halfZ, box);
                    placeBlock(level, x % 4 == 0 ? light : carved, x, 10, centerZ + halfZ, box);
                }
            } else {
                for (int z = centerZ - halfZ; z <= centerZ + halfZ; z++) {
                    placeBlock(level, z % 4 == 0 ? light : carved, centerX - halfX, 10, z, box);
                    placeBlock(level, z % 4 == 0 ? light : carved, centerX + halfX, 10, z, box);
                }
            }
        }

        private void buildCodexStack(WorldGenLevel level, BoundingBox box,
                                     int centerX, int centerZ, BlockState wall,
                                     BlockState carved, BlockState rune, BlockState light) {
            disk(level, box, centerX, 0, centerZ, 7, carved);
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 1, centerZ + corner[1], 18, wall);
                placeBlock(level, light, centerX + corner[0], 19, centerZ + corner[1], box);
            }
            for (int levelY : new int[]{5, 10, 15}) {
                disk(level, box, centerX, levelY, centerZ, 5, carved);
                annulus(level, box, centerX, levelY + 1, centerZ, 4, 5, rune);
            }
            pillar(level, box, centerX, 1, centerZ, 20,
                    Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            placeBlock(level, light, centerX, 21, centerZ, box);
        }

        private void buildWeightlessOrreryCourt(WorldGenLevel level, BoundingBox box) {
            int center = 31;
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState frame = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            disk(level, box, center, 0, center, 30, floor);
            annulus(level, box, center, 0, center, 24, 30, frame);
            brokenCircularWall(level, box, center, center, 29,
                    1, 5, band, 19, true);
            buildMonumentGates(level, box, center, 29, 14, band, rune, crystal);
            circularRing(level, box, center, 1, center, 22, rune, 5);

            int[][] satellites = {{center, 7}, {center, 55}, {7, center}, {55, center}};
            for (int index = 0; index < satellites.length; index++) {
                int[] satellite = satellites[index];
                buildOrrerySatellite(level, box, satellite[0], satellite[1],
                        frame, band, rune, crystal, index);
                int dx = Integer.compare(satellite[0], center);
                int dz = Integer.compare(satellite[1], center);
                for (int step = 9; step <= 24; step++) {
                    int x = center + dx * step;
                    int z = center + dz * step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int pathX = dz == 0 ? x : x + lateral;
                        int pathZ = dx == 0 ? z : z + lateral;
                        placeBlock(level, Math.abs(lateral) == 2 ? rune : frame,
                                pathX, 4, pathZ, box);
                        if (step % 4 == 0 && Math.abs(lateral) == 2) {
                            pillar(level, box, pathX, 1, pathZ, 3, band);
                        }
                    }
                }
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 1; step <= 10; step++) {
                    int distance = 18 - step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step,
                                Math.abs(lateral) == 2 ? rune : floor);
                    }
                }
            }
            disk(level, box, center, 10, center, 8, frame);
            annulus(level, box, center, 11, center, 6, 8, band);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center, 11, center, box);
            pillar(level, box, center, 12, center, 18, crystal);

            orbitalRing(level, box, center, center, 12, 18, 5, rune, 0.0D);
            orbitalRing(level, box, center, center, 9, 24, 4, crystal, Math.PI / 2.0D);
            orbitalRing(level, box, center, center, 6, 29, 3, band, Math.PI / 4.0D);
            for (int[] pylon : new int[][]{{18, 18}, {44, 18}, {18, 44}, {44, 44}}) {
                int height = 17 + Math.floorMod(pylon[0] + pylon[1], 5);
                pillar(level, box, pylon[0], 1, pylon[1], height, frame);
                pillar(level, box, pylon[0], height + 1, pylon[1], 4, crystal);
                for (int arm = 1; arm <= 4; arm++) {
                    placeBlock(level, rune,
                            pylon[0] + Integer.compare(center, pylon[0]) * arm,
                            height - arm / 2, pylon[1], box);
                }
            }
            for (int[] needle : new int[][]{{5, 19}, {5, 43}, {19, 5}, {43, 5},
                    {57, 19}, {57, 43}, {19, 57}, {43, 57}}) {
                int height = 11 + Math.floorMod(needle[0] * 3 + needle[1], 7);
                pillar(level, box, needle[0], 1, needle[1], height, band);
                placeBlock(level, crystal, needle[0], height + 1, needle[1], box);
                buildSpireFinial(level, box, needle[0], height + 2, needle[1],
                        1, 3, band, rune, crystal);
            }
            buildStairCornice(level, box, center, 6, center, Footprint.square(29), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 7, center, Footprint.square(29), band, rune);
            buildParapet(level, box, center, 8, center, Footprint.square(29), 6, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), rune);
        }

        private void buildOrrerySatellite(WorldGenLevel level, BoundingBox box,
                                           int centerX, int centerZ, BlockState frame,
                                           BlockState band, BlockState rune, BlockState crystal,
                                           int variant) {
            disk(level, box, centerX, 4, centerZ, 7, frame);
            annulus(level, box, centerX, 5, centerZ, 5, 7, rune);
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 1, centerZ + corner[1], 10, band);
                placeBlock(level, crystal, centerX + corner[0], 11, centerZ + corner[1], box);
            }
            circularRing(level, box, centerX, 12, centerZ, 5, crystal, centerX - centerZ);
            switch (variant) {
                case 0, 1 -> buildAlternatingGravityGallery(level, box, centerX, centerZ,
                        false, frame, band, rune, crystal);
                case 2 -> buildVerticalSuspensionWell(level, box, centerX, centerZ,
                        frame, band, rune, crystal);
                default -> buildOrbitalLeapLaboratory(level, box, centerX, centerZ,
                        frame, band, rune, crystal);
            }
        }

        private void buildAlternatingGravityGallery(WorldGenLevel level, BoundingBox box,
                                                     int centerX, int centerZ, boolean alongX,
                                                     BlockState frame, BlockState band,
                                                     BlockState rune, BlockState crystal) {
            for (int forward = -6; forward <= 6; forward++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    int x = centerX + (alongX ? forward : lateral);
                    int z = centerZ + (alongX ? lateral : forward);
                    placeBlock(level, Math.abs(lateral) == 3 ? rune : frame, x, 4, z, box);
                    placeBlock(level, (forward + lateral) % 2 == 0 ? band : rune,
                            x, 12, z, box);
                    if (Math.abs(lateral) == 3 && Math.floorMod(forward, 3) != 1) {
                        placeBlock(level, band, x, 5, z, box);
                    }
                }
            }
            for (int forward = -6; forward <= 6; forward += 3) {
                for (int lateral : new int[]{-4, 4}) {
                    int x = centerX + (alongX ? forward : lateral);
                    int z = centerZ + (alongX ? lateral : forward);
                    pillar(level, box, x, 5, z, 8, band);
                    placeBlock(level, crystal, x, 8, z, box);
                }
                for (int lateral = -4; lateral <= 4; lateral++) {
                    int x = centerX + (alongX ? forward : lateral);
                    int z = centerZ + (alongX ? lateral : forward);
                    placeBlock(level, Math.floorMod(forward, 6) == 0 ? rune : frame,
                            x, 12, z, box);
                }
            }
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    centerX, 5, centerZ, box);
        }

        private void buildVerticalSuspensionWell(WorldGenLevel level, BoundingBox box,
                                                  int centerX, int centerZ,
                                                  BlockState frame, BlockState band,
                                                  BlockState rune, BlockState crystal) {
            annulus(level, box, centerX, 10, centerZ, 4, 6, frame);
            disk(level, box, centerX, 10, centerZ, 2, band);
            annulus(level, box, centerX, 16, centerZ, 3, 6, rune);

            for (int step = 0; step <= 5; step++) {
                int lowerZ = centerZ + 6 - step;
                int upperZ = centerZ - 1 - step;
                for (int lateral = -1; lateral <= 1; lateral++) {
                    placeBlock(level, step % 2 == 0 ? frame : band,
                            centerX + lateral, 4 + step, lowerZ, box);
                    placeBlock(level, step % 2 == 0 ? rune : frame,
                            centerX + lateral, 11 + step, upperZ, box);
                }
            }
            for (int[] brace : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + brace[0], 5, centerZ + brace[1], 15, band);
                placeBlock(level, crystal, centerX + brace[0], 20, centerZ + brace[1], box);
            }
            circularRing(level, box, centerX, 21, centerZ, 5, crystal, centerX + centerZ);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    centerX, 11, centerZ, box);
        }

        private void buildOrbitalLeapLaboratory(WorldGenLevel level, BoundingBox box,
                                                 int centerX, int centerZ,
                                                 BlockState frame, BlockState band,
                                                 BlockState rune, BlockState crystal) {
            annulus(level, box, centerX, 5, centerZ, 5, 6, frame);
            int[][] pods = {{0, -4, 7}, {4, 0, 9}, {0, 4, 11}, {-4, 0, 9}};
            for (int index = 0; index < pods.length; index++) {
                int[] pod = pods[index];
                platform(level, box, centerX + pod[0] - 1, pod[2],
                        centerZ + pod[1] - 1, 3, 3, index % 2 == 0 ? rune : band);
                pillar(level, box, centerX + pod[0], 5, centerZ + pod[1],
                        pod[2] - 5, crystal);
            }
            for (int[] brace : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + brace[0], 5, centerZ + brace[1], 11, band);
                placeBlock(level, crystal, centerX + brace[0], 16, centerZ + brace[1], box);
            }
            orbitalRing(level, box, centerX, centerZ, 5, 14, 2, rune, 0.0D);
            orbitalRing(level, box, centerX, centerZ, 7, 18, 2, crystal, Math.PI / 2.0D);
            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    centerX, 5, centerZ, box);
        }

        private void orbitalRing(WorldGenLevel level, BoundingBox box,
                                 int centerX, int centerZ, int radius,
                                 int baseY, int amplitude, BlockState state, double phase) {
            for (int step = 0; step < 96; step++) {
                double angle = step * Math.PI / 48.0D;
                int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
                int y = baseY + (int) Math.round(Math.sin(angle + phase) * amplitude);
                placeBlock(level, state, x, y, z, box);
            }
        }

        private void buildHushedCathedral(WorldGenLevel level, BoundingBox box) {
            int center = 34;
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState rib = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState hush = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            BlockState light = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 67, 67, floor);
            annulus(level, box, center, 0, center, 26, 33, wall);
            brokenCircularWall(level, box, center, center, 32,
                    1, 7, rib, 23, true);
            buildMonumentGates(level, box, center, 32, 16, wall, rib, hush);

            for (int cursor = 2; cursor <= 66; cursor++) {
                for (int lateral = -4; lateral <= 4; lateral++) {
                    BlockState path = Math.abs(lateral) == 4 || cursor % 8 == 0 ? rib : floor;
                    placeBlock(level, path, center + lateral, 1, cursor, box);
                    placeBlock(level, path, cursor, 1, center + lateral, box);
                }
                if (cursor % 6 == 0 && Math.abs(cursor - center) > 12) {
                    pillar(level, box, center - 6, 2, cursor, 6, wall);
                    pillar(level, box, center + 6, 2, cursor, 6, wall);
                    pillar(level, box, cursor, 2, center - 6, 6, wall);
                    pillar(level, box, cursor, 2, center + 6, 6, wall);
                    placeBlock(level, light, center - 6, 8, cursor, box);
                    placeBlock(level, light, center + 6, 8, cursor, box);
                    placeBlock(level, light, cursor, 8, center - 6, box);
                    placeBlock(level, light, cursor, 8, center + 6, box);
                }
            }

            buildCathedralChapel(level, box, center, 10, false, wall, rib, hush, light);
            buildCathedralChapel(level, box, center, 58, false, wall, rib, hush, light);
            buildCathedralChapel(level, box, 10, center, true, wall, rib, hush, light);
            buildCathedralChapel(level, box, 58, center, true, wall, rib, hush, light);

            for (int[] tower : new int[][]{{10, 10}, {58, 10}, {10, 58}, {58, 58}}) {
                buildCathedralTower(level, box, tower[0], tower[1], wall, rib, hush, light);
                for (int step = 0; step <= 12; step++) {
                    int x = tower[0] + (center - tower[0]) * step / 12;
                    int z = tower[1] + (center - tower[1]) * step / 12;
                    int y = 22 + (int) Math.round(Math.sin(step * Math.PI / 12.0D) * 7.0D);
                    placeBlock(level, step % 4 == 0 ? hush : rib, x, y, z, box);
                }
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 0; step <= 7; step++) {
                    int distance = 20 - step;
                    for (int lateral = -3; lateral <= 3; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 2, z, step + 1,
                                Math.abs(lateral) == 3 ? rib : wall);
                    }
                }
            }
            disk(level, box, center, 9, center, 13, wall);
            annulus(level, box, center, 10, center, 9, 13, rib);
            annulus(level, box, center, 10, center, 5, 8, floor);
            for (int[] pylon : new int[][]{{27, 27}, {41, 27}, {27, 41}, {41, 41}}) {
                pillar(level, box, pylon[0], 10, pylon[1], 22, wall);
                for (int y = 13; y <= 29; y += 4) {
                    placeBlock(level, hush, pylon[0], y, pylon[1], box);
                }
                placeBlock(level, light, pylon[0], 32, pylon[1], box);
            }
            circularRing(level, box, center, 16, center, 11, rib, 3);
            circularRing(level, box, center, 22, center, 8, hush, 9);
            circularRing(level, box, center, 28, center, 5, light, 15);
            pillar(level, box, center, 10, center, 24, hush);
            placeBlock(level, light, center, 34, center, box);
            buildStairCornice(level, box, center, 8, center, Footprint.square(32), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 9, center, Footprint.square(32), wall, rib);
            buildParapet(level, box, center, 10, center, Footprint.square(32), 6, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), rib);
            for (int[] tower : new int[][]{{10, 10}, {58, 10}, {10, 58}, {58, 58}}) {
                buildSpireFinial(level, box, tower[0], 24, tower[1], 3, 7, wall, rib, light);
            }
            illuminateInterior(level, box, center, center, 14, 12, 12, 4, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
            buildProjectionRelayGallery(level, box, center, 2, 14, Direction.NORTH,
                    floor, wall, rib, light);
            buildSealedManor(level, box, 55, 1, 44, 5, 5, 3, 5,
                    wall, rib, floor,
                    Blocks.TINTED_GLASS.defaultBlockState(), light);
        }

        private void buildCathedralChapel(WorldGenLevel level, BoundingBox box,
                                          int centerX, int centerZ, boolean alongX,
                                          BlockState wall, BlockState rib,
                                          BlockState hush, BlockState light) {
            int halfX = alongX ? 9 : 7;
            int halfZ = alongX ? 7 : 9;
            platform(level, box, centerX - halfX, 1, centerZ - halfZ,
                    halfX * 2 + 1, halfZ * 2 + 1, wall);
            for (int[] corner : new int[][]{{-halfX, -halfZ}, {halfX, -halfZ},
                    {-halfX, halfZ}, {halfX, halfZ}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], 15, rib);
                placeBlock(level, light, centerX + corner[0], 17, centerZ + corner[1], box);
            }
            int span = alongX ? halfX : halfZ;
            for (int cursor = -span; cursor <= span; cursor++) {
                int x = alongX ? centerX + cursor : centerX;
                int z = alongX ? centerZ : centerZ + cursor;
                double normalized = cursor / (double) span;
                int y = 13 + (int) Math.round(5.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                placeBlock(level, Math.floorMod(cursor, 4) == 0 ? hush : rib, x, y, z, box);
            }
            for (int side : new int[]{-4, 4}) {
                int x = alongX ? centerX + side : centerX;
                int z = alongX ? centerZ : centerZ + side;
                pillar(level, box, x, 2, z, 12, hush);
            }
        }

        private void buildCathedralTower(WorldGenLevel level, BoundingBox box,
                                         int centerX, int centerZ, BlockState wall,
                                         BlockState rib, BlockState hush, BlockState light) {
            platform(level, box, centerX - 4, 1, centerZ - 4, 9, 9, wall);
            for (int[] corner : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], 25, rib);
                placeBlock(level, light, centerX + corner[0], 27, centerZ + corner[1], box);
            }
            platform(level, box, centerX - 3, 9, centerZ - 3, 7, 7, hush);
            platform(level, box, centerX - 4, 17, centerZ - 4, 9, 9, wall);
            circularRing(level, box, centerX, 23, centerZ, 5, rib, centerX + centerZ);
            pillar(level, box, centerX, 18, centerZ, 13, hush);
            placeBlock(level, light, centerX, 31, centerZ, box);
        }

        /** 内外投影踏板共用同组封门，内侧踏板留在宝箱清理范围之外。 */
        private void buildProjectionRelayGallery(WorldGenLevel level, BoundingBox box,
                                                  int gateX, int baseY, int gateZ,
                                                  Direction towardCache, BlockState floor,
                                                  BlockState wall, BlockState accent,
                                                  BlockState light) {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            for (int forward = -4; forward <= 5; forward++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    int x = gateX + towardCache.getStepX() * forward
                            + towardCache.getStepZ() * lateral;
                    int z = gateZ + towardCache.getStepZ() * forward
                            - towardCache.getStepX() * lateral;
                    for (int y = baseY; y <= baseY + 5; y++) {
                        placeBlock(level, air, x, y, z, box);
                    }
                }
            }

            for (int forward = -5; forward <= 6; forward++) {
                for (int lateral = -4; lateral <= 4; lateral++) {
                    int x = gateX + towardCache.getStepX() * forward
                            + towardCache.getStepZ() * lateral;
                    int z = gateZ + towardCache.getStepZ() * forward
                            - towardCache.getStepX() * lateral;
                    boolean vault = forward >= 0;
                    placeBlock(level, vault ? anchored : Math.abs(lateral) == 4 ? accent : floor,
                            x, baseY - 1, z, box);
                    if (vault && baseY >= 2) {
                        placeBlock(level, anchored, x, baseY - 2, z, box);
                    }
                    placeBlock(level, vault ? anchored : Math.floorMod(forward + lateral, 4) == 0 ? accent : wall,
                            x, baseY + 6, z, box);
                    if (Math.abs(lateral) == 4 || forward == 6) {
                        for (int y = baseY; y <= baseY + 5; y++) {
                            placeBlock(level, vault ? anchored : (y == baseY + 3 || forward % 3 == 0)
                                            ? accent : wall,
                                    x, y, z, box);
                        }
                    }
                }
            }

            for (int lateral = -4; lateral <= 4; lateral++) {
                int x = gateX + towardCache.getStepZ() * lateral;
                int z = gateZ - towardCache.getStepX() * lateral;
                for (int y = baseY; y <= baseY + 5; y++) {
                    if (lateral != 0 || y >= baseY + 2) {
                        placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }

            BlockState door = ModBlocks.PROJECTION_RESONANCE_DOOR.get().defaultBlockState();
            placeBlock(level, door, gateX, baseY, gateZ, box);
            placeBlock(level, door, gateX, baseY + 1, gateZ, box);

            int relayX = gateX - towardCache.getStepX() * 3;
            int relayZ = gateZ - towardCache.getStepZ() * 3;
            placeBlock(level, ModBlocks.PROJECTION_RESONANCE_PLATE.get().defaultBlockState(),
                    relayX, baseY, relayZ, box);
            placeBlock(level, ModBlocks.PROJECTION_RESONANCE_PLATE.get().defaultBlockState(),
                    gateX + towardCache.getStepX() * 3 + towardCache.getStepZ() * 2, baseY,
                    gateZ + towardCache.getStepZ() * 3 - towardCache.getStepX() * 2, box);

            for (int lateral : new int[]{-3, 3}) {
                int outerX = gateX - towardCache.getStepX() * 2
                        + towardCache.getStepZ() * lateral;
                int outerZ = gateZ - towardCache.getStepZ() * 2
                        - towardCache.getStepX() * lateral;
                int innerX = gateX + towardCache.getStepX() * 4
                        + towardCache.getStepZ() * lateral;
                int innerZ = gateZ + towardCache.getStepZ() * 4
                        - towardCache.getStepX() * lateral;
                placeBlock(level, light, outerX, baseY + 3, outerZ, box);
                placeBlock(level, light, innerX, baseY + 3, innerZ, box);
            }
        }

        private void buildRiftglassCitadel(WorldGenLevel level, BoundingBox box) {
            int center = 36;
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState frame = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState rift = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState prism = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();
            BlockState anchor = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 71, 71, floor);
            annulus(level, box, center, 0, center, 28, 35, frame);
            brokenCircularWall(level, box, center, center, 34,
                    1, 8, wall, 31, true);
            buildMonumentGates(level, box, center, 34, 17, wall, rift, prism);

            for (int cursor = 2; cursor <= 70; cursor++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    BlockState road = Math.abs(lateral) == 3 || cursor % 9 == 0 ? rift : floor;
                    placeBlock(level, road, center + lateral, 1, cursor, box);
                    placeBlock(level, road, cursor, 1, center + lateral, box);
                }
            }
            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    for (int step = 3; step <= 31; step++) {
                        int x = center + sx * step;
                        int z = center + sz * step;
                        for (int lateral = -1; lateral <= 1; lateral++) {
                            placeBlock(level, lateral == 0 ? anchor : frame,
                                    x + lateral * sz, 2, z - lateral * sx, box);
                        }
                        if (step % 5 == 0) {
                            pillar(level, box, x + 2 * sz, 1, z - 2 * sx, 6, rift);
                            placeBlock(level, prism, x + 2 * sz, 7, z - 2 * sx, box);
                        }
                    }
                }
            }

            int[][] bastions = {{36, 8}, {36, 64}, {8, 36}, {64, 36},
                    {16, 16}, {56, 16}, {16, 56}, {56, 56}};
            for (int index = 0; index < bastions.length; index++) {
                buildRiftglassBastion(level, box, bastions[index][0], bastions[index][1],
                        index >= 4, wall, frame, rift, prism, anchor);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 0; step <= 7; step++) {
                    int distance = 22 - step;
                    for (int lateral = -3; lateral <= 3; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 2, z, step + 1,
                                Math.abs(lateral) == 3 ? rift : anchor);
                    }
                }
            }
            disk(level, box, center, 9, center, 15, wall);
            annulus(level, box, center, 10, center, 10, 15, frame);
            annulus(level, box, center, 10, center, 5, 9, anchor);
            for (int[] pylon : new int[][]{{27, 27}, {45, 27}, {27, 45}, {45, 45}}) {
                pillar(level, box, pylon[0], 10, pylon[1], 24, wall);
                for (int y = 13; y <= 31; y += 3) {
                    placeBlock(level, y % 2 == 0 ? prism : rift, pylon[0], y, pylon[1], box);
                }
                placeBlock(level, prism, pylon[0], 34, pylon[1], box);
            }
            squareRiftRing(level, box, center, 16, 12, true, rift, 2);
            squareRiftRing(level, box, center, 22, 16, false, prism, 7);
            squareRiftRing(level, box, center, 28, 10, true, frame, 13);
            pillar(level, box, center, 10, center, 26, prism);
            placeBlock(level, rift, center, 36, center, box);
            buildStairCornice(level, box, center, 9, center, Footprint.square(34), ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 10, center, Footprint.square(34), wall, frame);
            buildParapet(level, box, center, 11, center, Footprint.square(34), 6, ModBlocks.MIRROR_DUST_BRICK_WALL.get().defaultBlockState(), frame);
            for (int[] bastion : new int[][]{{36, 8}, {36, 64}, {8, 36}, {64, 36},
                    {16, 16}, {56, 16}, {16, 56}, {56, 56}}) {
                buildSpireFinial(level, box, bastion[0], 16, bastion[1], 2, 5, wall, rift, prism);
            }
            illuminateInterior(level, box, center, center, 14, 14, 14, 4, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
        }

        private void buildRiftglassBastion(WorldGenLevel level, BoundingBox box,
                                           int centerX, int centerZ, boolean diagonal,
                                           BlockState wall, BlockState frame, BlockState rift,
                                           BlockState prism, BlockState anchor) {
            platform(level, box, centerX - 5, 1, centerZ - 5, 11, 11, anchor);
            int height = diagonal ? 23 : 19;
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], height, wall);
                placeBlock(level, prism, centerX + corner[0], height + 2, centerZ + corner[1], box);
            }
            platform(level, box, centerX - 4, 8, centerZ - 4, 9, 9, frame);
            platform(level, box, centerX - 3, 14, centerZ - 3, 7, 7, rift);
            circularRing(level, box, centerX, height - 2, centerZ, 5, prism,
                    centerX * 3 + centerZ);
            for (int[] brace : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
                pillar(level, box, centerX + brace[0], 15, centerZ + brace[1],
                        Math.max(4, height - 13), frame);
            }
        }

        private void squareRiftRing(WorldGenLevel level, BoundingBox box,
                                    int center, int y, int radius, boolean diamond,
                                    BlockState state, int phase) {
            if (diamond) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int dz = radius - Math.abs(dx);
                    if (Math.floorMod(dx + phase, 9) != 0) {
                        placeBlock(level, state, center + dx, y, center + dz, box);
                        placeBlock(level, state, center + dx, y, center - dz, box);
                    }
                }
                return;
            }
            for (int offset = -radius; offset <= radius; offset++) {
                if (Math.floorMod(offset + phase, 11) == 0) {
                    continue;
                }
                placeBlock(level, state, center + offset, y, center - radius, box);
                placeBlock(level, state, center + offset, y, center + radius, box);
                placeBlock(level, state, center - radius, y, center + offset, box);
                placeBlock(level, state, center + radius, y, center + offset, box);
            }
        }

        private void buildResonanceFoundry(WorldGenLevel level, BoundingBox box) {
            int center = 30;
            BlockState floor = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState frame = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState accent = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 59, 59, floor);
            annulus(level, box, center, 0, center, 22, 29, frame);
            annulus(level, box, center, 0, center, 18, 21, accent);
            brokenCircularWall(level, box, center, center, 28,
                    1, 5, wall, 6, true);
            buildMonumentGates(level, box, center, 28, 13, wall, accent, crystal);

            for (int offset = -2; offset <= 2; offset++) {
                for (int cursor = 1; cursor < 60; cursor++) {
                    BlockState road = Math.abs(offset) == 2 || cursor % 7 == 0 ? accent : floor;
                    placeBlock(level, road, center + offset, 0, cursor, box);
                    placeBlock(level, road, cursor, 0, center + offset, box);
                }
            }

            for (int[] tower : new int[][]{{7, 7}, {53, 7}, {7, 53}, {53, 53}}) {
                buildFoundryTower(level, box, tower[0], tower[1], frame, wall, accent, crystal);
                buildSpireFinial(level, box, tower[0], 20, tower[1], 3, 6, frame, accent, crystal);
            }
            buildStairCornice(level, box, center, 6, center, Footprint.square(28), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 7, center, Footprint.square(28), wall, accent);
            buildParapet(level, box, center, 8, center, Footprint.square(28), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), accent);
            illuminateInterior(level, box, center, center, 11, 12, 12, 4, ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
            buildFoundryWing(level, box, center, 9, false, floor, frame, accent, crystal);
            buildFoundryWing(level, box, center, 51, false, floor, frame, accent, crystal);
            buildFoundryWing(level, box, 9, center, true, floor, frame, accent, crystal);
            buildFoundryWing(level, box, 51, center, true, floor, frame, accent, crystal);

            disk(level, box, center, 1, center, 13, frame);
            annulus(level, box, center, 2, center, 9, 12, accent);
            for (int step = 0; step <= 7; step++) {
                int height = step + 1;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    for (int y = 1; y <= height; y++) {
                        BlockState stair = Math.abs(lateral) == 2 ? accent : floor;
                        placeBlock(level, stair, center + lateral, y, 18 + step, box);
                        placeBlock(level, stair, center + lateral, y, 42 - step, box);
                        placeBlock(level, stair, 18 + step, y, center + lateral, box);
                        placeBlock(level, stair, 42 - step, y, center + lateral, box);
                    }
                }
            }
            disk(level, box, center, 8, center, 7, floor);
            annulus(level, box, center, 9, center, 4, 7, accent);
            circularRing(level, box, center, 12, center, 5, crystal, 2);
            circularRing(level, box, center, 16, center, 8, frame, 7);
            for (int[] pylon : new int[][]{{center - 9, center}, {center + 9, center},
                    {center, center - 9}, {center, center + 9}}) {
                pillar(level, box, pylon[0], 1, pylon[1], 16, wall);
                pillar(level, box, pylon[0], 17, pylon[1], 4, crystal);
            }
            for (int[] arch : new int[][]{{center - 4, center - 13, center + 4, center - 13},
                    {center - 4, center + 13, center + 4, center + 13},
                    {center - 13, center - 4, center - 13, center + 4},
                    {center + 13, center - 4, center + 13, center + 4}}) {
                pillar(level, box, arch[0], 1, arch[1], 10, frame);
                pillar(level, box, arch[2], 1, arch[3], 10, frame);
                if (arch[1] == arch[3]) {
                    for (int x = Math.min(arch[0], arch[2]); x <= Math.max(arch[0], arch[2]); x++) {
                        placeBlock(level, accent, x, 10, arch[1], box);
                    }
                } else {
                    for (int z = Math.min(arch[1], arch[3]); z <= Math.max(arch[1], arch[3]); z++) {
                        placeBlock(level, accent, arch[0], 10, z, box);
                    }
                }
            }
            buildProjectionRelayGallery(level, box, 12, 1, center, Direction.WEST,
                    floor, wall, accent, crystal);
            buildSealedManor(level, box, 49, 1, 39, 4, 4, 2, 5,
                    wall, accent, floor,
                    Blocks.TINTED_GLASS.defaultBlockState(), crystal);
            // 必须叠退台炉体:碎典藏库已占用同群系的折叠板岩封闭大宅剪影。
            buildSteppedZiggurat(level, box, center, 1, center,
                    14, 3, 3, 0, wall, accent,
                    ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), crystal);
        }

        private void buildFoundryWing(WorldGenLevel level, BoundingBox box,
                                      int centerX, int centerZ, boolean alongX,
                                      BlockState floor, BlockState frame,
                                      BlockState accent, BlockState crystal) {
            int halfX = alongX ? 7 : 5;
            int halfZ = alongX ? 5 : 7;
            platform(level, box, centerX - halfX, 1, centerZ - halfZ,
                    halfX * 2 + 1, halfZ * 2 + 1, floor);
            for (int sx : new int[]{-halfX, halfX}) {
                for (int sz : new int[]{-halfZ, halfZ}) {
                    pillar(level, box, centerX + sx, 2, centerZ + sz, 8, frame);
                    placeBlock(level, crystal, centerX + sx, 10, centerZ + sz, box);
                }
            }
            for (int offset = -3; offset <= 3; offset++) {
                if ((offset & 1) == 0) {
                    int x = alongX ? centerX + offset : centerX;
                    int z = alongX ? centerZ : centerZ + offset;
                    pillar(level, box, x, 2, z, 4, accent);
                    placeBlock(level, crystal, x, 6, z, box);
                }
            }
            if (alongX) {
                for (int x = centerX - halfX; x <= centerX + halfX; x++) {
                    placeBlock(level, x % 3 == 0 ? accent : frame, x, 9, centerZ - halfZ, box);
                    placeBlock(level, x % 3 == 0 ? accent : frame, x, 9, centerZ + halfZ, box);
                }
            } else {
                for (int z = centerZ - halfZ; z <= centerZ + halfZ; z++) {
                    placeBlock(level, z % 3 == 0 ? accent : frame, centerX - halfX, 9, z, box);
                    placeBlock(level, z % 3 == 0 ? accent : frame, centerX + halfX, 9, z, box);
                }
            }
        }

        private void buildFoundryTower(WorldGenLevel level, BoundingBox box,
                                       int centerX, int centerZ, BlockState frame,
                                       BlockState wall, BlockState accent, BlockState crystal) {
            platform(level, box, centerX - 4, 1, centerZ - 4, 9, 9, frame);
            for (int[] corner : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], 16, wall);
                placeBlock(level, crystal, centerX + corner[0], 18, centerZ + corner[1], box);
            }
            platform(level, box, centerX - 3, 7, centerZ - 3, 7, 7, accent);
            platform(level, box, centerX - 4, 13, centerZ - 4, 9, 9, frame);
            for (int offset = -3; offset <= 3; offset += 2) {
                pillar(level, box, centerX + offset, 14, centerZ - 4, 5, accent);
                pillar(level, box, centerX + offset, 14, centerZ + 4, 5, accent);
            }
            pillar(level, box, centerX, 14, centerZ, 9, crystal);
        }

        private void buildGravityCrownObservatory(WorldGenLevel level, BoundingBox box) {
            int center = 28;
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState frame = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState accent = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            disk(level, box, center, 0, center, 27, floor);
            annulus(level, box, center, 0, center, 20, 27, frame);
            brokenCircularWall(level, box, center, center, 26,
                    1, 4, band, 11, true);
            buildMonumentGates(level, box, center, 26, 14, band, accent, crystal);
            for (int levelY = 1; levelY <= 8; levelY++) {
                disk(level, box, center, levelY, center, 15 - levelY, levelY % 2 == 0 ? band : frame);
            }

            int[][] satellites = {{center, 8}, {center, 48}, {8, center}, {48, center}};
            for (int index = 0; index < satellites.length; index++) {
                int[] satellite = satellites[index];
                buildObservatorySatellite(level, box, satellite[0], satellite[1],
                        frame, band, accent, crystal, index);
                int dx = Integer.compare(satellite[0], center);
                int dz = Integer.compare(satellite[1], center);
                for (int step = 9; step <= 20; step++) {
                    int x = center + dx * step;
                    int z = center + dz * step;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        int px = dz == 0 ? x : x + lateral;
                        int pz = dx == 0 ? z : z + lateral;
                        placeBlock(level, Math.abs(lateral) == 2 ? accent : floor, px, 4, pz, box);
                        pillar(level, box, px, 1, pz, 3, band);
                    }
                }
            }

            circularRing(level, box, center, 13, center, 11, accent, 1);
            circularRing(level, box, center, 17, center, 9, crystal, 7);
            circularRing(level, box, center, 21, center, 6, band, 12);
            for (int[] pylon : new int[][]{{center - 11, center}, {center + 11, center},
                    {center, center - 11}, {center, center + 11}}) {
                pillar(level, box, pylon[0], 5, pylon[1], 17, frame);
                placeBlock(level, crystal, pylon[0], 22, pylon[1], box);
            }
            pillar(level, box, center, 12, center, 17, crystal);

            int[][] crown = {{4, 18}, {4, 38}, {18, 4}, {38, 4},
                    {52, 18}, {52, 38}, {18, 52}, {38, 52}};
            for (int index = 0; index < crown.length; index++) {
                int x = crown[index][0];
                int z = crown[index][1];
                int height = 12 + index % 4 * 2;
                pillar(level, box, x, 1, z, height, index % 2 == 0 ? band : frame);
                pillar(level, box, x, height + 1, z, 4, crystal);
                for (int arm = 1; arm <= 3; arm++) {
                    placeBlock(level, accent, x + Integer.compare(center, x) * arm,
                            height - arm / 2, z, box);
                    placeBlock(level, accent, x, height - arm / 2,
                            z + Integer.compare(center, z) * arm, box);
                }
                buildSpireFinial(level, box, x, height + 5, z, 1, 3, band, accent, crystal);
            }
            buildStairCornice(level, box, center, 5, center, Footprint.square(26), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 6, center, Footprint.square(26), band, accent);
            buildParapet(level, box, center, 7, center, Footprint.square(26), 6, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), accent);
        }

        private void buildObservatorySatellite(WorldGenLevel level, BoundingBox box,
                                                int centerX, int centerZ, BlockState frame,
                                                BlockState band, BlockState accent, BlockState crystal,
                                                int variant) {
            disk(level, box, centerX, 4, centerZ, 7, frame);
            annulus(level, box, centerX, 5, centerZ, 5, 7, accent);
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 1, centerZ + corner[1], 10, band);
                placeBlock(level, crystal, centerX + corner[0], 11, centerZ + corner[1], box);
            }
            circularRing(level, box, centerX, 10, centerZ, 5, crystal, centerX + centerZ);
            switch (variant) {
                case 0 -> buildVerticalSuspensionWell(level, box, centerX, centerZ,
                        frame, band, accent, crystal);
                case 1 -> buildOrbitalLeapLaboratory(level, box, centerX, centerZ,
                        frame, band, accent, crystal);
                default -> buildAlternatingGravityGallery(level, box, centerX, centerZ,
                        true, frame, band, accent, crystal);
            }
        }

        private void buildVoidscarBastion(WorldGenLevel level, BoundingBox box) {
            int center = 32;
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState veined = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState accent = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            BlockState anchor = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 63, 63, floor);
            for (int y = 1; y <= 7; y++) {
                for (int cursor = 3; cursor <= 61; cursor++) {
                    boolean gate = Math.abs(cursor - center) <= 2 && y <= 4;
                    if (!gate && (cursor + y) % 11 != 0) {
                        placeBlock(level, wall, cursor, y, 3, box);
                        placeBlock(level, wall, cursor, y, 61, box);
                        placeBlock(level, wall, 3, y, cursor, box);
                        placeBlock(level, wall, 61, y, cursor, box);
                    }
                }
            }
            for (int cursor = 3; cursor <= 61; cursor += 4) {
                pillar(level, box, cursor, 8, 3, 3, veined);
                pillar(level, box, cursor, 8, 61, 3, veined);
                pillar(level, box, 3, 8, cursor, 3, veined);
                pillar(level, box, 61, 8, cursor, 3, veined);
            }
            buildMonumentGates(level, box, center, 29, 14, wall, veined, accent);

            for (int[] tower : new int[][]{{11, 11}, {53, 11}, {11, 53}, {53, 53}}) {
                buildBastionTower(level, box, tower[0], tower[1], wall, veined, anchor, accent);
            }

            for (int cursor = 7; cursor <= 57; cursor++) {
                if (cursor < 27 || cursor > 37) {
                    placeBlock(level, cursor % 3 == 0 ? accent : veined, center - 6, 0, cursor, box);
                    placeBlock(level, cursor % 4 == 0 ? accent : veined, cursor, 0, center + 7, box);
                }
            }
            for (int step = 0; step <= 26; step++) {
                int x = 6 + step;
                int z = 58 - step;
                placeBlock(level, step % 3 == 0 ? accent : veined, x, 0, z, box);
                placeBlock(level, step % 5 == 0 ? accent : veined, x + 1, 0, z, box);
            }

            int min = 20;
            int max = 44;
            for (int y = 1; y <= 13; y++) {
                for (int cursor = min; cursor <= max; cursor++) {
                    boolean gate = Math.abs(cursor - center) <= 2 && y <= 5;
                    if (!gate) {
                        BlockState state = (cursor + y) % 5 == 0 ? veined : wall;
                        placeBlock(level, state, cursor, y, min, box);
                        placeBlock(level, state, cursor, y, max, box);
                        placeBlock(level, state, min, y, cursor, box);
                        placeBlock(level, state, max, y, cursor, box);
                    }
                }
            }
            annulus(level, box, center, 0, center, 7, 12, anchor);
            for (int[] pylon : new int[][]{{23, 23}, {41, 23}, {23, 41}, {41, 41}}) {
                pillar(level, box, pylon[0], 1, pylon[1], 20, wall);
                pillar(level, box, pylon[0], 21, pylon[1], 5, accent);
            }
            for (int step = 0; step <= 8; step++) {
                int height = step + 1;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    for (int y = 1; y <= height; y++) {
                        placeBlock(level, Math.abs(lateral) == 2 ? veined : anchor,
                                center + lateral, y, 11 + step, box);
                    }
                }
            }
            platform(level, box, 27, 9, 20, 11, 5, anchor);
            circularRing(level, box, center, 16, center, 10, accent, 5);
            circularRing(level, box, center, 20, center, 7, veined, 13);

            BlockState lamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
            BlockState abyssStairs = ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState();
            BlockState abyssSlab = ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState();
            illuminateInterior(level, box, center, center, 13, 10, 10, 4, lamp);
            buildStairCornice(level, box, center, 8, center, Footprint.square(29), abyssStairs, abyssSlab, true);
            buildParapet(level, box, center, 9, center, Footprint.square(29), 6, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), veined);
            buildStairCornice(level, box, center, 14, center, Footprint.square(12), abyssStairs, abyssSlab, true);
            buildCornice(level, box, center, 15, center, Footprint.square(12), wall, accent);
            for (int[] tower : new int[][]{{11, 11}, {53, 11}, {11, 53}, {53, 53}}) {
                buildSpireFinial(level, box, tower[0], 19, tower[1], 3, 6, wall, veined, lamp);
            }
            for (int[] pylon : new int[][]{{23, 23}, {41, 23}, {23, 41}, {41, 41}}) {
                placeBlock(level, lamp, pylon[0], 26, pylon[1], box);
            }
        }

        private void buildBastionTower(WorldGenLevel level, BoundingBox box,
                                       int centerX, int centerZ, BlockState wall,
                                       BlockState veined, BlockState anchor, BlockState accent) {
            platform(level, box, centerX - 5, 1, centerZ - 5, 11, 11, anchor);
            for (int y = 2; y <= 15; y++) {
                for (int offset = -5; offset <= 5; offset++) {
                    if ((offset + y) % 7 != 0) {
                        placeBlock(level, wall, centerX + offset, y, centerZ - 5, box);
                        placeBlock(level, wall, centerX + offset, y, centerZ + 5, box);
                        placeBlock(level, wall, centerX - 5, y, centerZ + offset, box);
                        placeBlock(level, wall, centerX + 5, y, centerZ + offset, box);
                    }
                }
            }
            platform(level, box, centerX - 4, 8, centerZ - 4, 9, 9, veined);
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 16, centerZ + corner[1], 5, accent);
            }
            circularRing(level, box, centerX, 18, centerZ, 5, veined, centerX);
        }

        private void buildFoldedCelestialArchive(WorldGenLevel level, BoundingBox box) {
            int center = 35;
            BlockState floor = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState carved = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState light = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            platform(level, box, 1, 0, 1, 69, 69, floor);
            annulus(level, box, center, 0, center, 27, 34, carved);
            annulus(level, box, center, 0, center, 19, 26, wall);
            brokenCircularWall(level, box, center, center, 33,
                    1, 7, wall, 31, true);
            buildMonumentGates(level, box, center, 33, 18, wall, rune, light);

            for (int cursor = 2; cursor <= 68; cursor++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    BlockState path = Math.abs(lateral) == 3 || cursor % 7 == 0 ? rune : floor;
                    placeBlock(level, path, center + lateral, 1, cursor, box);
                    placeBlock(level, path, cursor, 1, center + lateral, box);
                }
            }

            buildFoldedReadingHall(level, box, center, 9, true, wall, carved, rune, light);
            buildFoldedReadingHall(level, box, center, 61, true, wall, carved, rune, light);
            buildFoldedReadingHall(level, box, 9, center, false, wall, carved, rune, light);
            buildFoldedReadingHall(level, box, 61, center, false, wall, carved, rune, light);

            int[][] towers = {{11, 11}, {59, 11}, {11, 59}, {59, 59}};
            for (int index = 0; index < towers.length; index++) {
                int[] tower = towers[index];
                buildFoldedArchiveCrownTower(level, box, tower[0], tower[1],
                        wall, carved, rune, light, index);
                buildFoldedSkyRibbon(level, box, tower[0], tower[1], center, center,
                        rune, light, index);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 0; step <= 8; step++) {
                    int distance = 20 - step;
                    for (int lateral = -3; lateral <= 3; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step + 1,
                                Math.abs(lateral) == 3 ? rune : floor);
                    }
                }
            }
            disk(level, box, center, 9, center, 12, wall);
            annulus(level, box, center, 10, center, 8, 12, carved);
            annulus(level, box, center, 10, center, 4, 7, rune);
            buildCircularColonnade(level, box, center, 10, center,
                    10, 12, 18, wall, light, rune);
            buildBossArenaMandala(level, box, 61, 1, center, 5,
                    rune, carved, floor);
            buildBossArenaCrown(level, box, 61, 2, center, 6, 12,
                    wall, rune, light);
            buildArenaLightRing(level, box, 61, 2, center, 7, 10, 4, wall, ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
            circularRing(level, box, center, 24, center, 8, light, 3);
            circularRing(level, box, center, 28, center, 6, rune, 9);
            pillar(level, box, center, 10, center, 22, carved);
            placeBlock(level, light, center, 32, center, box);
            for (int[] tower : new int[][]{{11, 11}, {59, 11}, {11, 59}, {59, 59}}) {
                buildSpireFinial(level, box, tower[0], 18, tower[1], 3, 6, wall, carved, light);
            }
            buildStairCornice(level, box, center, 10, center, Footprint.square(12), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 11, center, Footprint.square(12), carved, rune);
            buildParapet(level, box, center, 12, center, Footprint.square(12), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), rune);

            // 灰烬抄录员的焚稿阶段：三座可操作稳定器，避免自动破防。
            for (int[] stabilizer : new int[][]{{0, -7}, {6, 4}, {-6, 4}}) {
                placeBlock(level, ModBlocks.VOID_STABILIZER.get().defaultBlockState(),
                        61 + stabilizer[0], 1, center + stabilizer[1], box);
            }

            placeMiniBossSpawner(level, box, 61, 0, center, "ash_scribe", 3);
            buildSealedManor(level, box, 56, 1, 45, 5, 5, 3, 5,
                    wall, carved, floor,
                    Blocks.TINTED_GLASS.defaultBlockState(), light);
        }

        private void buildFoldedReadingHall(WorldGenLevel level, BoundingBox box,
                                            int centerX, int centerZ, boolean alongX,
                                            BlockState wall, BlockState carved,
                                            BlockState rune, BlockState light) {
            int halfX = alongX ? 9 : 6;
            int halfZ = alongX ? 6 : 9;
            platform(level, box, centerX - halfX, 1, centerZ - halfZ,
                    halfX * 2 + 1, halfZ * 2 + 1, carved);
            for (int[] corner : new int[][]{{-halfX, -halfZ}, {halfX, -halfZ},
                    {-halfX, halfZ}, {halfX, halfZ}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], 15, wall);
                placeBlock(level, light, centerX + corner[0], 17, centerZ + corner[1], box);
            }
            int span = alongX ? halfX : halfZ;
            for (int cursor = -span; cursor <= span; cursor++) {
                int x = alongX ? centerX + cursor : centerX;
                int z = alongX ? centerZ : centerZ + cursor;
                double normalized = cursor / (double) span;
                int roofY = 14 + (int) Math.round(4.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                placeBlock(level, Math.floorMod(cursor, 4) == 0 ? light : rune,
                        x, roofY, z, box);
            }
            for (int shelf = -4; shelf <= 4; shelf += 2) {
                int x = alongX ? centerX + shelf : centerX - 3;
                int z = alongX ? centerZ - 3 : centerZ + shelf;
                pillar(level, box, x, 2, z, 4, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
        }

        private void buildFoldedArchiveCrownTower(WorldGenLevel level, BoundingBox box,
                                                   int centerX, int centerZ,
                                                   BlockState wall, BlockState carved,
                                                   BlockState rune, BlockState light, int phase) {
            disk(level, box, centerX, 1, centerZ, 7, carved);
            for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                pillar(level, box, centerX + corner[0], 2, centerZ + corner[1], 24, wall);
                for (int y = 6; y <= 22; y += 5) {
                    placeBlock(level, rune, centerX + corner[0], y, centerZ + corner[1], box);
                }
                placeBlock(level, light, centerX + corner[0], 26, centerZ + corner[1], box);
            }
            disk(level, box, centerX, 8, centerZ, 5, wall);
            annulus(level, box, centerX, 9, centerZ, 4, 6, rune);
            disk(level, box, centerX, 16, centerZ, 4, carved);
            circularRing(level, box, centerX, 23, centerZ, 6, rune, phase * 3);
            circularRing(level, box, centerX, 28, centerZ, 4, light, phase * 5);
            pillar(level, box, centerX, 17, centerZ, 13, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            placeBlock(level, light, centerX, 30, centerZ, box);
        }

        private void buildFoldedSkyRibbon(WorldGenLevel level, BoundingBox box,
                                          int startX, int startZ, int centerX, int centerZ,
                                          BlockState rune, BlockState light, int phase) {
            double endX = centerX + Integer.compare(startX, centerX) * 8.0D;
            double endZ = centerZ + Integer.compare(startZ, centerZ) * 8.0D;
            for (int step = 0; step <= 24; step++) {
                double progress = step / 24.0D;
                int x = (int) Math.round(startX + (endX - startX) * progress);
                int z = (int) Math.round(startZ + (endZ - startZ) * progress);
                int y = 23 + (int) Math.round(Math.sin(progress * Math.PI) * 8.0D);
                BlockState state = (step + phase) % 5 == 0 ? light : rune;
                placeBlock(level, state, x, y, z, box);
                placeBlock(level, state, x + Integer.compare(centerZ, startZ), y, z, box);
            }
        }

        private void buildInvertedPendulumPalace(WorldGenLevel level, BoundingBox box) {
            int center = 37;
            BlockState floor = ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
            BlockState frame = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState band = ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState();
            BlockState rune = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            disk(level, box, center, 0, center, 36, floor);
            annulus(level, box, center, 0, center, 29, 36, frame);
            brokenCircularWall(level, box, center, center, 35,
                    1, 7, band, 37, true);
            buildMonumentGates(level, box, center, 35, 20, band, rune, crystal);

            for (int cursor = 2; cursor <= 72; cursor++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    BlockState path = Math.abs(lateral) == 3 || cursor % 8 == 0 ? rune : frame;
                    placeBlock(level, path, center + lateral, 1, cursor, box);
                    placeBlock(level, path, cursor, 1, center + lateral, box);
                }
            }

            int[][] halls = {{15, 15}, {59, 15}, {15, 59}, {59, 59}};
            for (int index = 0; index < halls.length; index++) {
                int[] hall = halls[index];
                buildDiagonalProcession(level, box, center, center, hall[0], hall[1],
                        frame, rune);
                buildSuspendedBalanceHall(level, box, hall[0], hall[1],
                        frame, band, rune, crystal, index);
            }
            for (int[] spire : new int[][]{{8, 8}, {66, 8}, {8, 66}, {66, 66}}) {
                pillar(level, box, spire[0], 1, spire[1], 29, band);
                for (int y = 6; y <= 26; y += 5) {
                    placeBlock(level, rune, spire[0], y, spire[1], box);
                }
                placeBlock(level, crystal, spire[0], 30, spire[1], box);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 0; step <= 9; step++) {
                    int distance = 22 - step;
                    for (int lateral = -3; lateral <= 3; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 1, z, step + 1,
                                Math.abs(lateral) == 3 ? rune : floor);
                    }
                }
            }
            disk(level, box, center, 10, center, 12, frame);
            annulus(level, box, center, 11, center, 8, 12, band);
            annulus(level, box, center, 11, center, 4, 7, rune);
            buildBossArenaMandala(level, box, center, 10, center, 10,
                    band, frame, crystal);
            buildArenaLightRing(level, box, center, 11, center, 12, 12, 5, frame, ModBlocks.FOLD_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, center, 11, center, Footprint.square(12), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 12, center, Footprint.square(12), band, rune);
            buildParapet(level, box, center, 13, center, Footprint.square(12), 6, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), rune);
            for (int[] spire : new int[][]{{8, 8}, {66, 8}, {8, 66}, {66, 66}}) {
                buildSpireFinial(level, box, spire[0], 31, spire[1], 2, 5, band, rune, crystal);
            }

            for (int[] pier : new int[][]{{28, 28}, {46, 28}, {28, 46}, {46, 46}}) {
                pillar(level, box, pier[0], 11, pier[1], 25, frame);
                for (int y = 15; y <= 33; y += 6) {
                    placeBlock(level, rune, pier[0], y, pier[1], box);
                }
                placeBlock(level, crystal, pier[0], 36, pier[1], box);
            }
            buildPendulumSpan(level, box, center, center, true, band, rune, crystal);
            buildPendulumSpan(level, box, center, center, false, band, rune, crystal);
            circularRing(level, box, center, 28, center, 13, rune, 5);
            circularRing(level, box, center, 35, center, 10, crystal, 11);

            pillar(level, box, center, 26, center, 12, Blocks.CHAIN.defaultBlockState());
            disk(level, box, center, 25, center, 2, crystal);
            disk(level, box, center, 24, center, 3, band);
            disk(level, box, center, 23, center, 2, rune);
            pillar(level, box, center, 18, center, 5, crystal);

            placeBlock(level, ModBlocks.GRAVITY_FIELD_CORE.get().defaultBlockState(),
                    center + 8, 11, center, box);
            placeMiniBossSpawner(level, box, center, 9, center,
                    "inverted_gatekeeper", 3);
        }

        private void placeMiniBossSpawner(WorldGenLevel level, BoundingBox box,
                                          int x, int y, int z, String entityId,
                                          int spawnYOffset) {
            placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), x, y, z, box);
            BlockPos pos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
            if (box.isInside(pos)
                    && level.getBlockEntity(pos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id(entityId), spawnYOffset);
                if ("inverted_gatekeeper".equals(entityId)) {
                    spawner.setChallengeGate(EchoAbilityType.GRAVITY_ECHO, null,
                            "message.unknown_echoes.inverted_gatekeeper.locked");
                } else if ("ash_scribe".equals(entityId)) {
                    spawner.setChallengeGate(EchoAbilityType.VOID_ECHO, null,
                            "message.unknown_echoes.ash_scribe.locked");
                }
            }
        }

        private void buildDiagonalProcession(WorldGenLevel level, BoundingBox box,
                                              int startX, int startZ, int endX, int endZ,
                                              BlockState floor, BlockState edge) {
            int steps = Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ));
            for (int step = 0; step <= steps; step++) {
                double progress = step / (double) steps;
                int x = (int) Math.round(startX + (endX - startX) * progress);
                int z = (int) Math.round(startZ + (endZ - startZ) * progress);
                int y = 1 + Math.min(5, step * 5 / Math.max(1, steps - 4));
                for (int lateral = -1; lateral <= 1; lateral++) {
                    pillar(level, box, x + lateral, 1, z, y,
                            Math.abs(lateral) == 1 ? edge : floor);
                    pillar(level, box, x, 1, z + lateral, y,
                            Math.abs(lateral) == 1 ? edge : floor);
                }
            }
        }

        private void buildSuspendedBalanceHall(WorldGenLevel level, BoundingBox box,
                                               int centerX, int centerZ,
                                               BlockState frame, BlockState band,
                                               BlockState rune, BlockState crystal, int phase) {
            disk(level, box, centerX, 6, centerZ, 8, frame);
            annulus(level, box, centerX, 7, centerZ, 6, 8, rune);
            for (int[] corner : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
                pillar(level, box, centerX + corner[0], 1, centerZ + corner[1], 23, band);
                placeBlock(level, crystal, centerX + corner[0], 24, centerZ + corner[1], box);
            }
            disk(level, box, centerX, 14, centerZ, 5, band);
            annulus(level, box, centerX, 15, centerZ, 4, 6, rune);
            circularRing(level, box, centerX, 21, centerZ, 7, crystal, phase * 4);
            circularRing(level, box, centerX, 26, centerZ, 5, rune, phase * 7);
            pillar(level, box, centerX, 15, centerZ, 13, crystal);
            placeBlock(level, band, centerX, 28, centerZ, box);
        }

        private void buildPendulumSpan(WorldGenLevel level, BoundingBox box,
                                       int centerX, int centerZ, boolean alongX,
                                       BlockState frame, BlockState rune, BlockState crystal) {
            for (int cursor = -30; cursor <= 30; cursor++) {
                double normalized = cursor / 30.0D;
                int y = 22 + (int) Math.round(15.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                int x = alongX ? centerX + cursor : centerX;
                int z = alongX ? centerZ : centerZ + cursor;
                placeBlock(level, Math.floorMod(cursor, 6) == 0 ? crystal : rune, x, y, z, box);
            }
            for (int side : new int[]{-30, 30}) {
                int x = alongX ? centerX + side : centerX;
                int z = alongX ? centerZ : centerZ + side;
                pillar(level, box, x, 2, z, 20, frame);
            }
        }

        private void buildRiftwornMemorialCourt(WorldGenLevel level, BoundingBox box) {
            int center = 39;
            BlockState floor = ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
            BlockState wall = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState vein = ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState();
            BlockState hush = ModBlocks.MUTE_CRYSTAL.get().defaultBlockState();
            BlockState light = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();

            disk(level, box, center, 0, center, 38, floor);
            annulus(level, box, center, 0, center, 31, 38, wall);
            brokenCircularWall(level, box, center, center, 37,
                    1, 8, vein, 43, true);
            buildMonumentGates(level, box, center, 35, 19, wall, vein, hush);

            for (int cursor = 3; cursor <= 75; cursor++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    BlockState path = Math.abs(lateral) == 3 || cursor % 9 == 0 ? hush : floor;
                    placeBlock(level, path, center + lateral, 1, cursor, box);
                    placeBlock(level, path, cursor, 1, center + lateral, box);
                }
            }
            buildMemorialCloister(level, box, center, 10, true, wall, vein, hush, light);
            buildMemorialCloister(level, box, center, 68, true, wall, vein, hush, light);
            buildMemorialCloister(level, box, 10, center, false, wall, vein, hush, light);
            buildMemorialCloister(level, box, 68, center, false, wall, vein, hush, light);

            int[][] gardens = {{19, 19}, {59, 19}, {19, 59}, {59, 59}};
            for (int[] garden : gardens) {
                disk(level, box, garden[0], 2, garden[1], 8, wall);
                annulus(level, box, garden[0], 3, garden[1], 5, 8, vein);
                annulus(level, box, garden[0], 3, garden[1], 2, 4, hush);
                pillar(level, box, garden[0], 3, garden[1], 10, light);
                circularRing(level, box, garden[0], 12, garden[1], 5, hush,
                        garden[0] + garden[1]);
            }

            int[][] spires = {{39, 8}, {61, 17}, {70, 39}, {61, 61},
                    {39, 70}, {17, 61}, {8, 39}, {17, 17}};
            for (int index = 0; index < spires.length; index++) {
                int[] spire = spires[index];
                int height = 20 + Math.floorMod(index * 5, 9);
                buildMemorialSpire(level, box, spire[0], spire[1], height,
                        wall, vein, hush, light, index);
                buildMemoryCanopyRib(level, box, spire[0], spire[1], center, center,
                        vein, hush, index);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                for (int step = 0; step <= 7; step++) {
                    int distance = 21 - step;
                    for (int lateral = -3; lateral <= 3; lateral++) {
                        int x = center + dx * distance + dz * lateral;
                        int z = center + dz * distance - dx * lateral;
                        pillar(level, box, x, 2, z, step + 1,
                                Math.abs(lateral) == 3 ? hush : wall);
                    }
                }
            }
            disk(level, box, center, 8, center, 13, wall);
            annulus(level, box, center, 9, center, 9, 13, vein);
            annulus(level, box, center, 9, center, 4, 8, floor);
            buildCircularColonnade(level, box, center, 9, center,
                    11, 16, 21, wall, light, hush);
            circularRing(level, box, center, 24, center, 9, vein, 4);
            circularRing(level, box, center, 30, center, 7, hush, 10);
            pillar(level, box, center, 9, center, 27, hush);
            for (int arm = 3; arm <= 9; arm++) {
                BlockState state = arm % 3 == 0 ? light : vein;
                placeBlock(level, state, center + arm, 29 - arm / 3, center, box);
                placeBlock(level, state, center - arm, 29 - arm / 3, center, box);
                placeBlock(level, state, center, 29 - arm / 3, center + arm, box);
                placeBlock(level, state, center, 29 - arm / 3, center - arm, box);
            }
            placeBlock(level, light, center, 36, center, box);

            buildStairCornice(level, box, center, 9, center, Footprint.square(37), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, center, 10, center, Footprint.square(37), wall, vein);
            buildParapet(level, box, center, 11, center, Footprint.square(37), 6, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), vein);
            for (int index = 0; index < spires.length; index++) {
                int height = 20 + Math.floorMod(index * 5, 9);
                buildSpireFinial(level, box, spires[index][0], height + 2, spires[index][1],
                        2, 5, wall, vein, light);
            }
            illuminateInterior(level, box, center, center, 13, 12, 12, 4, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
        }

        private void buildMemorialCloister(WorldGenLevel level, BoundingBox box,
                                           int centerX, int centerZ, boolean alongX,
                                           BlockState wall, BlockState vein,
                                           BlockState hush, BlockState light) {
            int halfX = alongX ? 9 : 6;
            int halfZ = alongX ? 6 : 9;
            platform(level, box, centerX - halfX, 2, centerZ - halfZ,
                    halfX * 2 + 1, halfZ * 2 + 1, wall);
            for (int[] corner : new int[][]{{-halfX, -halfZ}, {halfX, -halfZ},
                    {-halfX, halfZ}, {halfX, halfZ}}) {
                pillar(level, box, centerX + corner[0], 3, centerZ + corner[1], 15, vein);
                placeBlock(level, light, centerX + corner[0], 18, centerZ + corner[1], box);
            }
            int span = alongX ? halfX : halfZ;
            for (int cursor = -span; cursor <= span; cursor++) {
                int x = alongX ? centerX + cursor : centerX;
                int z = alongX ? centerZ : centerZ + cursor;
                double normalized = cursor / (double) span;
                int y = 15 + (int) Math.round(4.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                placeBlock(level, Math.floorMod(cursor, 5) == 0 ? light : hush, x, y, z, box);
            }
            for (int offset = -4; offset <= 4; offset += 2) {
                int x = alongX ? centerX + offset : centerX - 3;
                int z = alongX ? centerZ - 3 : centerZ + offset;
                pillar(level, box, x, 3, z, 6, hush);
            }
        }

        private void buildMemorialSpire(WorldGenLevel level, BoundingBox box,
                                        int x, int z, int height,
                                        BlockState wall, BlockState vein,
                                        BlockState hush, BlockState light, int phase) {
            disk(level, box, x, 1, z, 4, wall);
            for (int y = 2; y <= height; y++) {
                BlockState state = y % 6 == 0 ? hush : y % 4 == 0 ? vein : wall;
                placeBlock(level, state, x, y, z, box);
                if (y < height - 4 && y % 5 == 0) {
                    placeBlock(level, vein, x + 1, y, z, box);
                    placeBlock(level, vein, x - 1, y, z, box);
                    placeBlock(level, vein, x, y, z + 1, box);
                    placeBlock(level, vein, x, y, z - 1, box);
                }
            }
            circularRing(level, box, x, height - 2, z, 4, hush, phase * 3);
            placeBlock(level, light, x, height + 1, z, box);
        }

        private void buildMemoryCanopyRib(WorldGenLevel level, BoundingBox box,
                                          int startX, int startZ, int centerX, int centerZ,
                                          BlockState vein, BlockState hush, int phase) {
            double endX = centerX + Integer.compare(startX, centerX) * 10.0D;
            double endZ = centerZ + Integer.compare(startZ, centerZ) * 10.0D;
            for (int step = 0; step <= 24; step++) {
                double progress = step / 24.0D;
                int x = (int) Math.round(startX + (endX - startX) * progress);
                int z = (int) Math.round(startZ + (endZ - startZ) * progress);
                int y = 20 + (int) Math.round(Math.sin(progress * Math.PI) * 10.0D);
                placeBlock(level, (step + phase) % 5 == 0 ? hush : vein, x, y, z, box);
            }
        }

        private void buildGrandResonancePath(WorldGenLevel level, BoundingBox box,
                                             int centerX, int centerZ, int targetX, int targetZ,
                                             BlockState theme, BlockState rune) {
            double dx = targetX - centerX;
            double dz = targetZ - centerZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            double perpendicularX = -dz / length;
            double perpendicularZ = dx / length;
            int steps = (int) Math.round(length);
            for (int step = 5; step <= steps - 4; step++) {
                double progress = step / length;
                double pathX = centerX + dx * progress;
                double pathZ = centerZ + dz * progress;
                for (int lateral = -2; lateral <= 2; lateral++) {
                    int x = (int) Math.round(pathX + perpendicularX * lateral);
                    int z = (int) Math.round(pathZ + perpendicularZ * lateral);
                    BlockState state = Math.abs(lateral) == 2 || step % 6 == 0 ? rune : theme;
                    placeBlock(level, state, x, 0, z, box);
                }
            }
        }

        private void buildGrandEchoPylon(WorldGenLevel level, BoundingBox box, int x, int z,
                                         BlockState theme, BlockState wall,
                                         BlockState rune, BlockState lamp) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int distanceSquared = square(dx) + square(dz);
                    if (distanceSquared <= 16) {
                        placeBlock(level, distanceSquared >= 11 ? rune : theme,
                                x + dx, 0, z + dz, box);
                    }
                    if (distanceSquared <= 8 && distanceSquared >= 3) {
                        placeBlock(level, distanceSquared >= 6 ? rune : wall,
                                x + dx, 1, z + dz, box);
                    }
                }
            }

            for (int y = 2; y <= 13; y++) {
                int radius = y <= 4 ? 2 : y <= 10 ? 1 : 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        boolean band = y == 5 || y == 10 || y == 13;
                        placeBlock(level, band ? rune : theme, x + dx, y, z + dz, box);
                    }
                }
            }
            for (int offset = -3; offset <= 3; offset++) {
                BlockState crown = Math.abs(offset) == 3 ? lamp : rune;
                placeBlock(level, crown, x + offset, 14, z, box);
                placeBlock(level, crown, x, 14, z + offset, box);
            }
            for (int y = 15; y <= 17; y++) {
                placeBlock(level, y == 16 ? lamp : rune, x, y, z, box);
            }
            placeBlock(level, lamp, x, 18, z, box);
            circularRing(level, box, x, 12, z, 4, rune, 3);
            circularRing(level, box, x, 13, z, 4, lamp, 11);
        }

        private void buildConvergenceArch(WorldGenLevel level, BoundingBox box,
                                          int centerX, int centerZ, int pylonX, int pylonZ,
                                          BlockState rune, BlockState lamp, int phase) {
            double dx = pylonX - centerX;
            double dz = pylonZ - centerZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            double endX = centerX + dx / length * 12.0D;
            double endZ = centerZ + dz / length * 12.0D;
            for (int step = 0; step <= 28; step++) {
                double progress = step / 28.0D;
                int x = (int) Math.round(pylonX + (endX - pylonX) * progress);
                int z = (int) Math.round(pylonZ + (endZ - pylonZ) * progress);
                int y = 18 + (int) Math.round(Math.sin(progress * Math.PI) * 10.0D);
                BlockState state = (step + phase) % 5 == 0 ? lamp : rune;
                placeBlock(level, state, x, y, z, box);
            }
        }

        private void buildResonanceBoundaryTower(WorldGenLevel level, BoundingBox box,
                                                 int x, int z, BlockState base,
                                                 BlockState wall, BlockState rune,
                                                 BlockState lamp, int phase) {
            disk(level, box, x, 0, z, 3, base);
            for (int y = 1; y <= 24; y++) {
                int radius = y <= 4 ? 2 : y <= 18 ? 1 : 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        boolean band = y == 5 || y == 12 || y == 19 || y == 24;
                        BlockState state = band || Math.floorMod(y + phase, 13) == 0 ? rune : wall;
                        placeBlock(level, state, x + dx, y, z + dz, box);
                    }
                }
            }
            for (int offset = -2; offset <= 2; offset++) {
                placeBlock(level, rune, x + offset, 24, z, box);
                placeBlock(level, rune, x, 24, z + offset, box);
            }
            placeBlock(level, lamp, x, 25, z, box);
            placeBlock(level, rune, x, 26, z, box);
            placeBlock(level, lamp, x, 27, z, box);
        }

        private void buildResonanceGate(WorldGenLevel level, BoundingBox box, int center,
                                        Direction direction, BlockState wall,
                                        BlockState rune, BlockState lamp) {
            int stepX = direction.getStepX();
            int stepZ = direction.getStepZ();
            int perpendicularX = stepZ;
            int perpendicularZ = -stepX;
            for (int side : new int[]{-6, 6}) {
                for (int depth = -1; depth <= 1; depth++) {
                    int x = center + stepX * (38 + depth) + perpendicularX * side;
                    int z = center + stepZ * (38 + depth) + perpendicularZ * side;
                    for (int y = 1; y <= 18; y++) {
                        BlockState state = y == 6 || y == 12 || y == 18 ? rune : wall;
                        placeBlock(level, state, x, y, z, box);
                    }
                }
            }
            for (int lateral = -6; lateral <= 6; lateral++) {
                double normalized = lateral / 6.0D;
                int y = 19 + (int) Math.round(5.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                for (int depth = -1; depth <= 1; depth++) {
                    int x = center + stepX * (38 + depth) + perpendicularX * lateral;
                    int z = center + stepZ * (38 + depth) + perpendicularZ * lateral;
                    placeBlock(level, lateral == 0 || Math.floorMod(lateral, 4) == 0 ? lamp : rune,
                            x, y, z, box);
                }
            }
        }

        private void buildEndingRecordSanctuary(WorldGenLevel level, BoundingBox box, int center,
                                                BlockState wall, BlockState rune, BlockState lamp) {
            int tableZ = center + 30;
            annulus(level, box, center, 0, tableZ, 3, 6, rune);
            disk(level, box, center, 0, tableZ, 3, ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState());
            for (int side : new int[]{-5, 5}) {
                pillar(level, box, center + side, 1, tableZ, 9, wall);
                placeBlock(level, rune, center + side, 5, tableZ, box);
                placeBlock(level, lamp, center + side, 10, tableZ, box);
            }
            for (int lateral = -5; lateral <= 5; lateral++) {
                double normalized = lateral / 5.0D;
                int y = 10 + (int) Math.round(3.0D
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                placeBlock(level, Math.abs(lateral) <= 1 ? lamp : rune,
                        center + lateral, y, tableZ, box);
            }
            for (int offset = -3; offset <= 3; offset++) {
                placeBlock(level, rune, center + offset, 0, tableZ - 6, box);
            }
        }

        private static boolean isGrandFloorRing(double distance) {
            return Math.abs(distance - 8.0D) < 0.46D
                    || Math.abs(distance - 15.0D) < 0.46D
                    || Math.abs(distance - 23.0D) < 0.46D
                    || Math.abs(distance - 31.0D) < 0.46D;
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

        private void installSequencePuzzle(WorldGenLevel level, BoundingBox box,
                                           int coreX, int coreY, int coreZ,
                                           int[][] runeSpots, int[][] hintSpots,
                                           int sequenceLength, BlockState pedestal,
                                           long salt) {
            installSequencePuzzle(level, box, coreX, coreY, coreZ, runeSpots, hintSpots,
                    sequenceLength, pedestal, salt, Direction.UP);
        }

        private void installSequencePuzzle(WorldGenLevel level, BoundingBox box,
                                           int coreX, int coreY, int coreZ,
                                           int[][] runeSpots, int[][] hintSpots,
                                           int sequenceLength, BlockState pedestal,
                                           long salt, Direction hintFacing) {
            placeBlock(level, pedestal, coreX, coreY - 1, coreZ, box);
            placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState(),
                    coreX, coreY, coreZ, box);
            RandomSource puzzleRandom = RandomSource.create(
                    boundingBox.minX() * 341873128712L
                            ^ boundingBox.minZ() * 132897987541L ^ salt);
            int[] orders = shuffled(puzzleRandom, runeSpots.length);
            for (int i = 0; i < runeSpots.length; i++) {
                int[] spot = runeSpots[i];
                placeBlock(level, pedestal, spot[0], spot[1] - 1, spot[2], box);
                BlockState rune = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, orders[i]);
                placeBlock(level, rune, spot[0], spot[1], spot[2], box);
            }

            int[] sequence = Arrays.copyOf(shuffled(puzzleRandom, runeSpots.length), sequenceLength);
            for (int i = 0; i < sequence.length; i++) {
                int[] spot = hintSpots[i];
                BlockState hint = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.TRUE)
                        .setValue(BlockStateProperties.FACING, localFurnitureFacing(hintFacing));
                placeBlock(level, hint, spot[0], spot[1], spot[2], box);
            }

            BlockPos corePos = new BlockPos(getWorldX(coreX, coreZ),
                    getWorldY(coreY), getWorldZ(coreX, coreZ));
            if (box.isInside(corePos) && level.getBlockEntity(corePos) instanceof PuzzleCoreBlockEntity core) {
                core.setSequence(sequence);
            }
        }

        private void buildVaultShell(WorldGenLevel level, BoundingBox box,
                                     int minX, int maxX, int minZ, int maxZ,
                                     int floorY, int roofY, Direction entrance,
                                     BlockState shell, BlockState door) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // 地板纹饰与后续大厅铺装可覆盖表层，下垫层必须保持封闭。
                    placeBlock(level, shell, x, floorY - 1, z, box);
                    placeBlock(level, shell, x, floorY, z, box);
                    placeBlock(level, shell, x, roofY, z, box);
                }
            }
            for (int y = floorY + 1; y < roofY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    placeBlock(level, shell, x, y, minZ, box);
                    placeBlock(level, shell, x, y, maxZ, box);
                }
                for (int z = minZ; z <= maxZ; z++) {
                    placeBlock(level, shell, minX, y, z, box);
                    placeBlock(level, shell, maxX, y, z, box);
                }
            }
            for (int x = minX + 1; x < maxX; x++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    for (int y = floorY + 1; y < roofY; y++) {
                        placeBlock(level, air, x, y, z, box);
                    }
                }
            }

            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            for (int y = floorY + 1; y <= floorY + 3; y++) {
                for (int offset = -1; offset <= 1; offset++) {
                    switch (entrance) {
                        case NORTH -> placeBlock(level, door, centerX + offset, y, minZ, box);
                        case SOUTH -> placeBlock(level, door, centerX + offset, y, maxZ, box);
                        case WEST -> placeBlock(level, door, minX, y, centerZ + offset, box);
                        case EAST -> placeBlock(level, door, maxX, y, centerZ + offset, box);
                        default -> { }
                    }
                }
            }
        }

        private void buildMonumentGates(WorldGenLevel level, BoundingBox box,
                                        int center, int radius, int pillarHeight,
                                        BlockState wall, BlockState rune, BlockState light) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                int perpendicularX = dz;
                int perpendicularZ = -dx;
                for (int side : new int[]{-5, 5}) {
                    int x = center + dx * radius + perpendicularX * side;
                    int z = center + dz * radius + perpendicularZ * side;
                    pillar(level, box, x, 1, z, pillarHeight, wall);
                    placeBlock(level, rune, x, 5, z, box);
                    placeBlock(level, light, x, pillarHeight + 1, z, box);

                    int braceX = center + dx * (radius - 3) + perpendicularX * (side * 6 / 5);
                    int braceZ = center + dz * (radius - 3) + perpendicularZ * (side * 6 / 5);
                    pillar(level, box, braceX, 1, braceZ,
                            Math.max(6, pillarHeight - 4), rune);
                }
                for (int lateral = -5; lateral <= 5; lateral++) {
                    double normalized = lateral / 5.0D;
                    int y = pillarHeight + (int) Math.round(4.0D
                            * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                    int x = center + dx * radius + perpendicularX * lateral;
                    int z = center + dz * radius + perpendicularZ * lateral;
                    placeBlock(level, Math.abs(lateral) <= 1 ? light : rune, x, y, z, box);
                }
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box,
                          int centerX, int y, int centerZ, int radius, BlockState state) {
            int radiusSquared = radius * radius;
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    int dx = x - centerX;
                    int dz = z - centerZ;
                    if (dx * dx + dz * dz <= radiusSquared) {
                        placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private void annulus(WorldGenLevel level, BoundingBox box,
                             int centerX, int y, int centerZ,
                             int innerRadius, int outerRadius, BlockState state) {
            int innerSquared = innerRadius * innerRadius;
            int outerSquared = outerRadius * outerRadius;
            for (int x = centerX - outerRadius; x <= centerX + outerRadius; x++) {
                for (int z = centerZ - outerRadius; z <= centerZ + outerRadius; z++) {
                    int dx = x - centerX;
                    int dz = z - centerZ;
                    int distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared >= innerSquared && distanceSquared <= outerSquared) {
                        placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private void circularRing(WorldGenLevel level, BoundingBox box,
                                  int centerX, int y, int centerZ,
                                  int radius, BlockState state, int phase) {
            for (int step = 0; step < 64; step++) {
                if ((step + phase) % 17 == 0) {
                    continue;
                }
                double angle = step * Math.PI / 32.0D;
                int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
                placeBlock(level, state, x, y, z, box);
            }
        }

        private void brokenCircularWall(WorldGenLevel level, BoundingBox box,
                                        int centerX, int centerZ, int radius,
                                        int startY, int height, BlockState state,
                                        int phase, boolean cardinalOpenings) {
            for (int step = 0; step < 64; step++) {
                double angle = step * Math.PI / 32.0D;
                int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
                int dx = x - centerX;
                int dz = z - centerZ;
                boolean entrance = cardinalOpenings
                        && ((Math.abs(dx) <= 1 && Math.abs(dz) >= radius - 1)
                        || (Math.abs(dz) <= 1 && Math.abs(dx) >= radius - 1));
                if (entrance || (step + phase) % 19 <= 1) {
                    continue;
                }
                for (int y = startY; y < startY + height; y++) {
                    if ((step + y + phase) % 23 != 0) {
                        placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private static int[] shuffled(RandomSource random, int size) {
            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                values[i] = i;
            }
            for (int i = size - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int current = values[i];
                values[i] = values[other];
                values[other] = current;
            }
            return values;
        }

        /**
         * 每种深层结构配一座主题刷怪笼(空地坐标避开既有机关、宝箱与柱网): 折页虫守档案系,锚壳兽守重力系,
         * 虚空行者守裂痕与噤声系。
         */
        private void placeThemedEncounter(WorldGenLevel level, RandomSource random, BoundingBox box) {
            BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            switch (kind) {
                case FOLDED_ARCHIVE -> placeStructureSpawner(level, random, box, 23, 1, 22,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case INVERTED_CHASM_BRIDGE -> placeStructureSpawner(level, random, box, 32, 1, 18,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case SILENT_ABYSS_ANCHOR -> placeStructureSpawner(level, random, box, 24, 1, 9,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case CALIBRATION_HALL -> placeStructureSpawner(level, random, box, 27, 1, 28,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case VOID_VEIN_CHAMBER -> placeStructureSpawner(level, random, box, 31, 1, 11,
                        ModEntities.VOID_WANDERER.get(), abyss, chiseled);
                case WORLD_RESONANCE_HALL -> placeStructureSpawner(level, random, box, 53, 1, 23,
                        ModEntities.VOID_WANDERER.get(), abyss, chiseled);
                case FOLDED_WAYSTATION -> placeStructureSpawner(level, random, box, 5, 1, 19,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case GRAVITY_SHARD_SPIRE -> placeStructureSpawner(level, random, box, 6, 1, 13,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case HUSHED_REFUGE -> placeStructureSpawner(level, random, box, 4, 1, 14,
                        ModEntities.VOID_WANDERER.get(), abyss, chiseled);
                case MISFILED_CATALOG_WELL -> placeStructureSpawner(level, random, box, 6, 1, 14,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case FOLDED_SCRIPTORIUM -> placeStructureSpawner(level, random, box, 8, 1, 25,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case INVERTED_ARCHIVE_TOWER -> placeStructureSpawner(level, random, box, 9, 1, 9,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case SUSPENDED_SURVEY_STATION -> placeStructureSpawner(level, random, box, 29, 1, 17,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case FRACTURED_ANCHOR_STATION -> placeStructureSpawner(level, random, box, 6, 1, 27,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case SEVERED_ARCHIVE_VAULT -> placeStructureSpawner(level, random, box, 11, 1, 8,
                        ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                case RESONANCE_FOUNDRY -> placeStructureSpawner(level, random, box, 19, 1, 19,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case GRAVITY_CROWN_OBSERVATORY -> placeStructureSpawner(level, random, box, 15, 1, 15,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case VOIDSCAR_BASTION -> placeStructureSpawner(level, random, box, 28, 1, 36,
                        ModEntities.VOID_WANDERER.get(), abyss, chiseled);
                case SHATTERED_CODEX_VAULT -> {
                    placeStructureSpawner(level, random, box, 21, 1, 21,
                            ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);

                    buildFoldpagePairRoom(level, random, box, 45, 1, 45, Direction.EAST,
                            folded, folded, chiseled, ModLootTables.T3_PUZZLE);
                }
                case WEIGHTLESS_ORRERY_COURT -> placeStructureSpawner(level, random, box, 25, 1, 25,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case HUSHED_CATHEDRAL -> {
                    placeStructureSpawner(level, random, box, 21, 1, 21,
                            ModEntities.VOID_WANDERER.get(), abyss, chiseled);

                    buildStepPathSanctum(level, random, box, 47, 1, 47, Direction.EAST,
                            abyss, gravity, chiseled, ModLootTables.T3_PUZZLE);
                }
                case RIFTGLASS_CITADEL -> placeStructureSpawner(level, random, box, 17, 1, 29,
                        ModEntities.VOID_WANDERER.get(), abyss, chiseled);
                case FOLDED_CELESTIAL_ARCHIVE -> {
                    placeStructureSpawner(level, random, box, 22, 1, 22,
                            ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);

                    buildFoldpagePairRoom(level, random, box, 49, 1, 22, Direction.EAST,
                            folded, folded, chiseled, ModLootTables.T3_PUZZLE);
                }
                case INVERTED_PENDULUM_PALACE -> placeStructureSpawner(level, random, box, 27, 1, 15,
                        ModEntities.ANCHOR_SHELL_BEAST.get(), gravity, chiseled);
                case RIFTWORN_MEMORIAL_COURT -> {
                    placeStructureSpawner(level, random, box, 31, 1, 29,
                            ModEntities.VOID_WANDERER.get(), abyss, chiseled);

                    buildStepPathSanctum(level, random, box, 50, 1, 48, Direction.EAST,
                            folded, gravity, chiseled, ModLootTables.T3_PUZZLE);
                }
            }
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
                placeCorniceBlock(level, box, state, x, yy, z);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("DeepRegionKind", kind.id);
        }
    }
}
