package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.util.RandomSource;

/** 群系结构补齐:用单 piece 代码生成小结构、连接结构和主线外场。 */
public class RegionSiteStructure extends TerrainAnchoredStructure {
    public static final MapCodec<RegionSiteStructure> MEMORY_PILLAR_COURTYARD_CODEC = codec(Kind.MEMORY_PILLAR_COURTYARD);
    public static final MapCodec<RegionSiteStructure> LOST_CAMP_CODEC = codec(Kind.LOST_CAMP);
    public static final MapCodec<RegionSiteStructure> OVERWORLD_LOST_CAMP_CODEC = codec(Kind.OVERWORLD_LOST_CAMP);
    public static final MapCodec<RegionSiteStructure> WIND_ERODED_STEPS_CODEC = codec(Kind.WIND_ERODED_STEPS);
    public static final MapCodec<RegionSiteStructure> WIND_RUNE_STONE_CIRCLE_CODEC = codec(Kind.WIND_RUNE_STONE_CIRCLE);
    public static final MapCodec<RegionSiteStructure> REFLECTION_WELL_CODEC = codec(Kind.REFLECTION_WELL);
    public static final MapCodec<RegionSiteStructure> MIRROR_LAKE_CACHE_CODEC = codec(Kind.MIRROR_LAKE_CACHE);
    public static final MapCodec<RegionSiteStructure> TIDE_BUOY_ARRAY_CODEC = codec(Kind.TIDE_BUOY_ARRAY);
    public static final MapCodec<RegionSiteStructure> REEF_HUT_CODEC = codec(Kind.REEF_HUT);
    public static final MapCodec<RegionSiteStructure> LAKEBED_LIGHTHOUSE_RUINS_CODEC = codec(Kind.LAKEBED_LIGHTHOUSE_RUINS);
    public static final MapCodec<RegionSiteStructure> ILLUSION_WALL_CHAMBER_CODEC = codec(Kind.ILLUSION_WALL_CHAMBER);
    public static final MapCodec<RegionSiteStructure> MOSSBACK_TURTLE_NEST_CODEC = codec(Kind.MOSSBACK_TURTLE_NEST);
    public static final MapCodec<RegionSiteStructure> UNDERGROUND_GREENHOUSE_CODEC = codec(Kind.UNDERGROUND_GREENHOUSE);
    public static final MapCodec<RegionSiteStructure> BROKEN_BELL_HUT_CODEC = codec(Kind.BROKEN_BELL_HUT);
    public static final MapCodec<RegionSiteStructure> BROKEN_BELL_STELE_CODEC = codec(Kind.BROKEN_BELL_STELE);
    public static final MapCodec<RegionSiteStructure> ARCHIVE_REMNANT_HALL_CODEC = codec(Kind.ARCHIVE_REMNANT_HALL);
    public static final MapCodec<RegionSiteStructure> CLIFF_BRIDGE_CODEC = codec(Kind.CLIFF_BRIDGE);
    public static final MapCodec<RegionSiteStructure> ARCHIVE_GATE_CODEC = codec(Kind.ARCHIVE_GATE);

    private final Kind kind;

    private static MapCodec<RegionSiteStructure> codec(Kind kind) {
        return simpleCodec(settings -> new RegionSiteStructure(settings, kind));
    }

    private RegionSiteStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!kind.underwater) {
            return super.findGenerationPoint(context);
        }

        int centerDepth = kind == Kind.LAKEBED_LIGHTHOUSE_RUINS ? 4 : kind == Kind.REEF_HUT ? 3 : 2;
        int edgeDepth = kind == Kind.LAKEBED_LIGHTHOUSE_RUINS ? 2 : 1;
        Piece piece = new Piece(kind, context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        int floorY = LakeBedPlacement.findOceanFloor(context,
                footprint.minX(), footprint.minZ(), kind.size, kind.size, centerDepth, edgeDepth);
        if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
            return java.util.Optional.empty();
        }
        piece.anchorToLakeFloor(floorY);
        return onTopOfChunkCenter(context, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(piece));
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case MEMORY_PILLAR_COURTYARD -> ModStructures.MEMORY_PILLAR_COURTYARD.get();
            case LOST_CAMP -> ModStructures.LOST_CAMP.get();
            case OVERWORLD_LOST_CAMP -> ModStructures.OVERWORLD_LOST_CAMP.get();
            case WIND_ERODED_STEPS -> ModStructures.WIND_ERODED_STEPS.get();
            case WIND_RUNE_STONE_CIRCLE -> ModStructures.WIND_RUNE_STONE_CIRCLE.get();
            case REFLECTION_WELL -> ModStructures.REFLECTION_WELL.get();
            case MIRROR_LAKE_CACHE -> ModStructures.MIRROR_LAKE_CACHE.get();
            case TIDE_BUOY_ARRAY -> ModStructures.TIDE_BUOY_ARRAY.get();
            case REEF_HUT -> ModStructures.REEF_HUT.get();
            case LAKEBED_LIGHTHOUSE_RUINS -> ModStructures.LAKEBED_LIGHTHOUSE_RUINS.get();
            case ILLUSION_WALL_CHAMBER -> ModStructures.ILLUSION_WALL_CHAMBER.get();
            case MOSSBACK_TURTLE_NEST -> ModStructures.MOSSBACK_TURTLE_NEST.get();
            case UNDERGROUND_GREENHOUSE -> ModStructures.UNDERGROUND_GREENHOUSE.get();
            case BROKEN_BELL_HUT -> ModStructures.BROKEN_BELL_HUT.get();
            case BROKEN_BELL_STELE -> ModStructures.BROKEN_BELL_STELE.get();
            case ARCHIVE_REMNANT_HALL -> ModStructures.ARCHIVE_REMNANT_HALL.get();
            case CLIFF_BRIDGE -> ModStructures.CLIFF_BRIDGE.get();
            case ARCHIVE_GATE -> ModStructures.ARCHIVE_GATE.get();
        };
    }

    public enum Kind {
        MEMORY_PILLAR_COURTYARD("memory_pillar_courtyard", 73, 28),
        LOST_CAMP("lost_camp", 31, 14),
        OVERWORLD_LOST_CAMP("overworld_lost_camp", 31, 14),
        WIND_ERODED_STEPS("wind_eroded_steps", 35, 22),
        WIND_RUNE_STONE_CIRCLE("wind_rune_stone_circle", 33, 16),
        REFLECTION_WELL("reflection_well", 29, 18),
        MIRROR_LAKE_CACHE("mirror_lake_cache", 27, 14),
        TIDE_BUOY_ARRAY("tide_buoy_array", 29, 12, true),
        REEF_HUT("reef_hut", 25, 14, true),
        LAKEBED_LIGHTHOUSE_RUINS("lakebed_lighthouse_ruins", 31, 20, true),
        ILLUSION_WALL_CHAMBER("illusion_wall_chamber", 35, 16),
        MOSSBACK_TURTLE_NEST("mossback_turtle_nest", 29, 12),
        UNDERGROUND_GREENHOUSE("underground_greenhouse", 39, 20),
        BROKEN_BELL_HUT("broken_bell_hut", 31, 16),
        BROKEN_BELL_STELE("broken_bell_stele", 25, 24),
        ARCHIVE_REMNANT_HALL("archive_remnant_hall", 37, 20),
        CLIFF_BRIDGE("cliff_bridge", 45, 18),
        ARCHIVE_GATE("archive_gate", 41, 28);

        final String id;
        final int size;
        final int height;
        /** 湖底生成:findGenerationPoint 锚湖床,postProcess 不清空水体。 */
        final boolean underwater;

        Kind(String id, int size, int height) {
            this(id, size, height, false);
        }

        Kind(String id, int size, int height, boolean underwater) {
            this.id = id;
            this.size = size;
            this.height = height;
            this.underwater = underwater;
        }

        static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return LOST_CAMP;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] REFLECTION_WELL_MIRROR_NODES = {
                {-3, -3}, {3, -3}, {-3, 3}, {3, 3}
        };
        private static final int[][] BROKEN_STELE_ORE_NODES = {
                {0, 0}, {-3, 0}, {3, 0}, {0, -3}, {0, 3}
        };
        private static final int[][] GREENHOUSE_GEODE_NODES = {
                {-9, -1}, {-9, 6}, {-3, -9}, {3, -9},
                {9, -6}, {9, 1}, {-4, 9}, {4, 9}
        };
        private Kind kind;
        private boolean spawnedMainBoss = false;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.REGION_SITE_PIECE.get(), x, 64, z,
                    kind.size, kind.height, kind.size, Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.REGION_SITE_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("RegionSiteKind"));
            this.spawnedMainBoss = tag.getBoolean("SpawnedMainBoss");
        }

        public void anchorToLakeFloor(int floorY) {
            if (this.heightPosition >= 0) {
                return;
            }
            this.heightPosition = floorY;
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 1, 0);
        }

        @Override
        protected int siteFootprint() {
            return kind.size;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            boolean anchored = kind.underwater
                    ? this.updateLakeFloorHeight(level, box, generator)
                    : this.updateAverageGroundHeight(level, box, 0);
            if (!anchored) {
                return;
            }
            if (kind.underwater) {
                box = LakeBedPieceHelper.intersection(box, this.boundingBox);
                if (box == null) {
                    return;
                }
            }
            if (!kind.underwater) {

                clearPlayableVolume(level, box);
            }
            int c = kind.size / 2;
            switch (kind) {
                case MEMORY_PILLAR_COURTYARD -> memoryCourtyard(level, random, box, c);
                case LOST_CAMP -> lostCamp(level, random, box, c);
                case OVERWORLD_LOST_CAMP -> overworldLostCamp(level, random, box, c);
                case WIND_ERODED_STEPS -> windSteps(level, random, box, c);
                case WIND_RUNE_STONE_CIRCLE -> windCircle(level, random, box, c);
                case REFLECTION_WELL -> reflectionWell(level, random, box, c);
                case MIRROR_LAKE_CACHE -> mirrorCache(level, random, box, c);
                case TIDE_BUOY_ARRAY -> tideBuoys(level, random, box, c);
                case REEF_HUT -> reefHut(level, random, box, c);
                case LAKEBED_LIGHTHOUSE_RUINS -> lakebedLighthouse(level, random, box, c);
                case ILLUSION_WALL_CHAMBER -> illusionChamber(level, random, box, c);
                case MOSSBACK_TURTLE_NEST -> turtleNest(level, box, c);
                case UNDERGROUND_GREENHOUSE -> greenhouse(level, random, box, c);
                case BROKEN_BELL_HUT -> brokenHut(level, random, box, c);
                case BROKEN_BELL_STELE -> brokenStele(level, random, box, c);
                case ARCHIVE_REMNANT_HALL -> archiveHall(level, random, box, c);
                case CLIFF_BRIDGE -> cliffBridge(level, random, box, c);
                case ARCHIVE_GATE -> archiveGate(level, random, box, c);
            }
            buildRegionalFrame(level, random, box, c);
            buildRegionalSignature(level, box, c);
            if (kind == Kind.TIDE_BUOY_ARRAY) {
                // 明奖落在底层甲板，北向壁龛避开层间通路，浮标位于各层暴露顶面。
                buildSiteBranchReward(level, random, box, c - 4, 2, c + 3, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
                buildAccessibleChest(level, random, box, c, 2, c + 1, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
                buildTrueSightVaultNiche(level, random, box, c + 4, 1, c - 4, Direction.NORTH);
                placeTideBuoyMarkers(level, box, c);
            }
            if (kind == Kind.WIND_ERODED_STEPS) {
                buildTrueSightVaultNiche(level, random, box, c - 8, 1, c - 3, Direction.EAST);
            }
            if (kind == Kind.LAKEBED_LIGHTHOUSE_RUINS) {
                buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
                buildTrueSightVaultNiche(level, random, box, c + 9, 1, c - 3, Direction.NORTH);
            }
            // 西侧壁龛与屋墙共用入口面，须在主体砌筑后封壳。
            if (kind == Kind.REEF_HUT) {
                buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.T4_REGION_CORE);
                buildTrueSightVaultNiche(level, random, box, c - 10, 1, c - 4, Direction.EAST);
            }
            if (kind == Kind.BROKEN_BELL_HUT) {
                buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.BROKEN_ARCHIVE_CHEST);
                buildTrueSightVaultNiche(level, random, box, c - 10, 1, c - 4, Direction.EAST);
            }
            if (kind == Kind.MOSSBACK_TURTLE_NEST) {
                this.placeBlock(level, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), c + 1, 1, c - 1, box);
                buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.SILENT_HUT_CHEST);
                buildSiteHiddenReward(level, random, box, c + 4, 1, c - 4);
                buildAccessibleChest(level, random, box, c + 3, 1, c - 3, ModLootTables.SILENT_HUT_CHEST);
            }
            if (kind == Kind.CLIFF_BRIDGE) {
                buildSiteHiddenReward(level, random, box, c + 4, 1, c - 4);
                placeCorniceBlock(level, box, ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), c + 3, 1, c - 1);
            }
            addThemedEncounter(level, random, box);
            placeBudgetedResourceNodes(level, box, c);
            if (!kind.underwater) {
                BlockState foundation = switch (kind) {
                    case WIND_ERODED_STEPS, WIND_RUNE_STONE_CIRCLE, CLIFF_BRIDGE ->
                            ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                    case BROKEN_BELL_HUT, BROKEN_BELL_STELE ->
                            ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    case REEF_HUT, REFLECTION_WELL -> ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
                    case LOST_CAMP, OVERWORLD_LOST_CAMP, MOSSBACK_TURTLE_NEST, UNDERGROUND_GREENHOUSE ->
                            ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
                    default -> ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                };
                anchorGroundColumns(level, box, 0, foundation);
            }
            dressStructure(level, box, kind.id);
            if (kind == Kind.CLIFF_BRIDGE) {
                buildGroundEntry(level, box);
            }
            if (kind == Kind.BROKEN_BELL_HUT) {
                restorePlantBed(level, box, c + 4, 0, c - 3, 0, Blocks.MOSS_BLOCK.defaultBlockState(),
                        state -> state.is(ModBlocks.BROKEN_BELL_THORN.get()));
            } else if (kind == Kind.OVERWORLD_LOST_CAMP) {
                for (int dx : new int[]{-2, 2}) {
                    for (int dz : new int[]{-2, 2}) {
                        int x = c - 7 + dx;
                        int z = c + 6 + dz;
                        if (getBlock(level, x, 4, z, box).is(Blocks.LANTERN)) {
                            placeBlock(level, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), x, 5, z, box);
                            placeBlock(level, Blocks.LANTERN.defaultBlockState()
                                    .setValue(BlockStateProperties.HANGING, true), x, 4, z, box);
                        }
                    }
                }
            }
        }

        /** 区域结构各自的识别构件,用于把小型据点从同一套房屋模板中拉开。 */
        private void buildRegionalSignature(WorldGenLevel level, BoundingBox box, int c) {
            switch (kind) {
                case MEMORY_PILLAR_COURTYARD -> {
                    BlockState stone = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int x = c - 27; x <= c + 27; x += 9) {
                            architecturalPillar(level, box, x, 13, c + side * 28, 9, stone);
                            placeCorniceBlock(level, box, rune, x, 22, c + side * 28);
                        }
                    }
                }
                case LOST_CAMP -> {
                    BlockState log = ModBlocks.ECHO_LOG.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 10, 6, c - 10, 5, log);
                        placeCorniceBlock(level, box, rune, c + side * 10, 11, c - 10);
                    }
                }
                case OVERWORLD_LOST_CAMP -> {
                    BlockState timber = ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState();
                    BlockState plank = ModBlocks.ECHO_PLANKS.get().defaultBlockState();
                    for (int x = c - 10; x <= c + 10; x += 5) {
                        placeCorniceBlock(level, box, timber, x, 8, c + 9);
                        placeCorniceBlock(level, box, plank, x, 9, c + 9);
                    }
                }
                case WIND_ERODED_STEPS -> {
                    BlockState wind = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int y = 8; y <= 18; y += 2) {
                            placeCorniceBlock(level, box, wind, c + side * (y - 2), y, c + 9);
                        }
                        placeCorniceBlock(level, box, lamp, c + side * 10, 19, c + 9);
                    }
                }
                case WIND_RUNE_STONE_CIRCLE -> {
                    BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState rune = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 12);
                        int z = c + (int) Math.round(Math.sin(angle) * 12);
                        architecturalPillar(level, box, x, 8, z, 6 + i % 2, wind);
                        placeCorniceBlock(level, box, rune, x, 14 + i % 2, z);
                    }
                }
                case REFLECTION_WELL -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState dust = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int y = 8; y <= 14; y++) {
                            placeCorniceBlock(level, box, y % 2 == 0 ? mirror : dust,
                                    c + side * (y - 4), y, c - 9);
                        }
                    }
                }
                case MIRROR_LAKE_CACHE -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState prism = ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState();
                    architecturalRing(level, box, c, 10, c, 10, mirror);
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 10, 8, c, 5, mirror);
                        placeCorniceBlock(level, box, prism, c + side * 10, 13, c);
                    }
                }
                case TIDE_BUOY_ARRAY -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState coral = ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int x = c - 9; x <= c + 9; x += 6) {
                            placeCorniceBlock(level, box, reef, x, 7, c + side * 10);
                            placeCorniceBlock(level, box, coral, x, 8, c + side * 10);
                        }
                    }
                }
                case REEF_HUT -> {
                    BlockState wood = ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState();
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    for (int x = c - 8; x <= c + 8; x += 4) {
                        placeCorniceBlock(level, box, wood, x, 9, c + 8);
                        placeCorniceBlock(level, box, reef, x, 10, c + 8);
                    }
                }
                case LAKEBED_LIGHTHOUSE_RUINS -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState coral = ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState();
                    for (int i = 0; i < 6; i++) {
                        double angle = i * Math.PI / 3.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 11);
                        int z = c + (int) Math.round(Math.sin(angle) * 11);
                        architecturalPillar(level, box, x, 10, z, 6, reef);
                        placeCorniceBlock(level, box, coral, x, 16, z);
                    }
                }
                case ILLUSION_WALL_CHAMBER -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState glass = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    for (int x = c - 12; x <= c + 12; x += 4) {
                        placeCorniceBlock(level, box, mirror, x, 11, c - 12);
                        placeCorniceBlock(level, box, glass, x, 12, c - 12);
                    }
                }
                case MOSSBACK_TURTLE_NEST -> {
                    BlockState mud = ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
                    BlockState root = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
                    architecturalRing(level, box, c, 8, c, 10, root);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        architecturalPillar(level, box, c + direction.getStepX() * 10,
                                8, c + direction.getStepZ() * 10, 3, mud);
                    }
                }
                case UNDERGROUND_GREENHOUSE -> {
                    BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
                    BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                    for (int layer = 0; layer < 3; layer++) {
                        architecturalRing(level, box, c, 11 + layer * 2, c,
                                12 + layer * 3, layer == 1 ? glass : song);
                    }
                }
                case BROKEN_BELL_HUT -> {
                    BlockState bell = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rust = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 10, 8, c - 9, 6, bell);
                        placeCorniceBlock(level, box, rust, c + side * 10, 14, c - 9);
                    }
                }
                case BROKEN_BELL_STELE -> {
                    BlockState bell = ModBlocks.ASHEN_BELLSTONE.get().defaultBlockState();
                    BlockState rust = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    for (int y = 10; y <= 20; y++) {
                        int offset = (y - 10) / 2;
                        placeCorniceBlock(level, box, y % 3 == 0 ? rust : bell,
                                c - offset, y, c + offset);
                    }
                }
                case ARCHIVE_REMNANT_HALL -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
                    for (int x = c - 12; x <= c + 12; x += 4) {
                        architecturalPillar(level, box, x, 13, c - 13, 5, strata);
                        placeCorniceBlock(level, box, mosaic, x, 18, c - 13);
                    }
                }
                case CLIFF_BRIDGE -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState crystal = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        for (int distance = -14; distance <= 14; distance += 7) {
                            placeCorniceBlock(level, box, strata, c + side * 4,
                                    8, c + distance);
                            placeCorniceBlock(level, box, crystal, c + side * 4,
                                    9, c + distance);
                        }
                    }
                }
                case ARCHIVE_GATE -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
                    for (int side : new int[]{-1, 1}) {
                        architecturalPillar(level, box, c + side * 14, 16, c, 8, strata);
                        placeCorniceBlock(level, box, mosaic, c + side * 14, 24, c);
                    }
                }
            }
        }

        /**
         * 按群系主题给据点补普通遭遇(小型点位、谜题环与龟巢保持安静): 迷雾营地闹回响流浪者,风蚀阶闹哨兵,
         * 湖底废墟潜水影,残钟屋守卫巡逻。
         */
        private void addThemedEncounter(WorldGenLevel level, RandomSource random, BoundingBox box) {
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState reef = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState worn = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
            switch (kind) {
                case MEMORY_PILLAR_COURTYARD -> placeStructureSpawner(level, random, box, 17, 1, 43,
                        ModEntities.MOSSPLATE_HOUND.get(), bricks, rune);
                case LOST_CAMP -> placeStructureSpawner(level, random, box, 7, 1, 17,
                        ModEntities.REED_MASK_RAIDER.get(), bricks, rune);
                case OVERWORLD_LOST_CAMP -> placeStructureSpawner(level, random, box, 21, 1, 21,
                        ModEntities.ECHO_REMNANT.get(), bricks, rune);
                case WIND_ERODED_STEPS -> placeStructureSpawner(level, random, box, 6, 1, 6,
                        ModEntities.WIND_ERODED_SENTINEL.get(), bricks, rune);
                case TIDE_BUOY_ARRAY -> placeSubmergedStructureSpawner(level, random, box, 14, 1, 7,
                        ModEntities.WATER_SHADOW.get(), reef, worn);
                case LAKEBED_LIGHTHOUSE_RUINS -> placeSubmergedStructureSpawner(level, random, box, 22, 1, 20,
                        ModEntities.WATER_SHADOW.get(), reef, worn);
                case ILLUSION_WALL_CHAMBER -> placeStructureSpawner(level, random, box, 27, 1, 25,
                        ModEntities.MIRROR_DUST_DECOY.get(), reef, rune);
                case UNDERGROUND_GREENHOUSE -> placeStructureSpawner(level, random, box, 13, 1, 13,
                        ModEntities.SPORE_CROWNED_WRETCH.get(), bricks, rune);
                case BROKEN_BELL_HUT -> placeStructureSpawner(level, random, box, 5, 1, 20,
                        ModEntities.BROKEN_BELL_AXEMAN.get(), bricks, rune);
                case ARCHIVE_REMNANT_HALL -> placeStructureSpawner(level, random, box, 23, 1, 27,
                        ModEntities.RIFT_PAGE_STALKER.get(), bricks, rune);
                case CLIFF_BRIDGE -> placeStructureSpawner(level, random, box, 10, 1, 22,
                        ModEntities.CAIRN_SHELL_BRUTE.get(), bricks, rune);
                case ARCHIVE_GATE -> placeStructureSpawner(level, random, box, 13, 1, 31,
                        ModEntities.ECHO_REMNANT.get(), bricks, rune);
                default -> { }
            }
        }

        private void buildRegionalFrame(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            switch (kind) {
                case MEMORY_PILLAR_COURTYARD -> {
                    BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    buildCircularColonnade(level, box, c, 1, c, 32, 16, 9,
                            brick, rune, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, c, 1, c, 32, direction,
                                3, 8, brick, rune);
                    }
                    buildRuneCauseway(level, box, c, 1, c, Direction.SOUTH,
                            24, 35, 2, brick, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(), rune);
                    buildStairCornice(level, box, c, 10, c, Footprint.square(32), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(33), 6, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
                }
                case LOST_CAMP -> {
                    // 与主世界失落营地共用 helper,必须靠材质分层区分,否则读成同一处。
                    buildCampPerimeter(level, random, box, c, true);
                    buildRuinedShell(level, box, c, 1, c, 7, 7, 5,
                            ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                            ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(),
                            ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(),
                            ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                    buildParapet(level, box, c, 1, c, Footprint.square(7), 4, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
                    buildStairCornice(level, box, c, 6, c, Footprint.square(7), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 5, c, Footprint.square(7), ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
                }
                case OVERWORLD_LOST_CAMP -> {
                    buildCampPerimeter(level, random, box, c, false);
                    buildCourtyardCloister(level, box, c, 1, c, Footprint.square(8), 5, 2, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(), ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState(), ModBlocks.ECHO_PLANKS.get().defaultBlockState(), ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                    buildParapet(level, box, c, 1, c, Footprint.square(7), 4, ModBlocks.WHISPERING_TIMBER_WALL.get().defaultBlockState(), ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
                    // 木构收边:回声境域营地用砖石收边,两处必须区分。
                    buildStairCornice(level, box, c, 6, c, Footprint.square(8), ModBlocks.WHISPERING_TIMBER_STAIRS.get().defaultBlockState(), ModBlocks.WHISPERING_TIMBER_SLAB.get().defaultBlockState(), true);
                    buildSpireFinial(level, box, c, 7, c, 2, 4,
                            ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState(),
                            ModBlocks.ECHO_PLANKS.get().defaultBlockState(),
                            ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                }
                case WIND_ERODED_STEPS -> {
                    BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState light = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
                    buildTwinTowerFacade(level, box, c, 1, c - 11,
                            8, 2, 12, wind, chiseled, light);
                    buildGabledPavilion(level, box, c + 5, 10, c + 5, Footprint.of(5, 4), 5, 3, wind, wind, chiseled, light);
                    buildRuneCauseway(level, box, c, 1, c, Direction.NORTH,
                            8, 15, 2, wind, chiseled, light);
                    // 收边必须用风蚀岩砖族:借用青色回响石砖会与浅灰风蚀岩撞色。
                    buildStairCornice(level, box, c, 13, c - 11, Footprint.of(8, 2), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
                    buildStairCornice(level, box, c + 5, 15, c + 5, Footprint.of(5, 4), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c + 5, 16, c + 5, Footprint.of(6, 5), 4, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), chiseled);
                    for (int towerX : new int[]{c - 8, c + 8}) {
                        buildSpireFinial(level, box, towerX, 14, c - 11, 2, 4,
                                wind, chiseled, light);
                    }
                }
                case WIND_RUNE_STONE_CIRCLE -> {
                    BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState light = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

                    buildCircularColonnade(level, box, c, 1, c, 11, 12, 7,
                            wind, light, chiseled);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        buildCardinalGateway(level, box, c, 1, c, 12, direction,
                                2, 7, wind, chiseled);
                    }
                    buildTieredTower(level, box, c - 11, 1, c - 11,
                            2, 9, wind, chiseled, light);
                    buildTieredTower(level, box, c + 11, 1, c - 11,
                            2, 9, wind, chiseled, light);
                    buildStairCornice(level, box, c, 8, c, Footprint.square(11), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(13), 5, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), chiseled);
                    for (int towerX : new int[]{c - 11, c + 11}) {
                        buildSpireFinial(level, box, towerX, 11, c - 11,
                                1, 3, wind, chiseled, light);
                    }
                }
                case REFLECTION_WELL -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState fractured = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
                    BlockState light = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    // 倒影井必须用围合中庭:永恒灯塔已占用镜石族亭殿剪影。
                    buildCourtyardCloister(level, box, c, 1, c, Footprint.square(9), 7, 3, fractured, mirror, fractured, light);
                    buildCardinalGateway(level, box, c, 1, c, 9, Direction.SOUTH,
                            3, 8, mirror, ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState());
                    buildStairCornice(level, box, c, 7, c, Footprint.square(7), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(9), 4, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), fractured);
                }
                case MIRROR_LAKE_CACHE -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState dust = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
                    BlockState light = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c, Footprint.of(6, 5), 5, 3, dust, mirror, dust, light);
                    buildTieredTower(level, box, c - 8, 1, c,
                            2, 9, mirror, dust, light);
                    buildTieredTower(level, box, c + 8, 1, c,
                            2, 9, mirror, dust, light);
                    buildStairCornice(level, box, c, 6, c, Footprint.of(6, 5), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    for (int towerX : new int[]{c - 8, c + 8}) {
                        buildSpireFinial(level, box, towerX, 11, c, 2, 4, mirror, dust, light);
                    }
                    buildCornice(level, box, c, 5, c, Footprint.of(6, 5), mirror, dust);
                    buildParapet(level, box, c, 7, c, Footprint.of(7, 6), 4, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), dust);
                }
                case ILLUSION_WALL_CHAMBER -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState fractured = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
                    BlockState light = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c, Footprint.square(9), 8, 4, fractured, mirror, fractured, light);
                    buildTwinTowerFacade(level, box, c, 1, c - 10,
                            8, 2, 12, mirror, fractured, light);
                    buildStairCornice(level, box, c, 9, c, Footprint.square(9), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildStairCornice(level, box, c, 13, c - 10, Footprint.of(8, 2), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 10, c, Footprint.square(10), 5, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), fractured);
                    buildCornice(level, box, c, 9, c, Footprint.square(9), mirror, fractured);
                    for (int towerX : new int[]{c - 8, c + 8}) {
                        buildSpireFinial(level, box, towerX, 14, c - 10, 2, 4,
                                mirror, fractured, light);
                    }
                }
                case TIDE_BUOY_ARRAY -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState worn = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
                    buildCantileverTerraces(level, box, c, 1, c,
                            Direction.SOUTH, 3, 7, 3, worn, reef, worn,
                            ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState());
                    buildRuneCauseway(level, box, c, 0, c, Direction.SOUTH,
                            8, 13, 2, reef, worn,
                            ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState());
                    buildStairCornice(level, box, c, 6, c, Footprint.square(8), ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(10), 5, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), worn);
                    buildCornice(level, box, c, 2, c, Footprint.square(9), ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState(), ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState());
                }
                case REEF_HUT -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState wood = ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState();
                    BlockState light = ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c, Footprint.of(8, 7), 6, 4, reef, wood, reef, light);
                    buildArcadedWing(level, box, c, 1, c + 7, Footprint.of(8, 3), 4, 4, reef, wood, reef, light);
                    buildStairCornice(level, box, c, 7, c, Footprint.of(8, 7), ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c + 7, Footprint.of(8, 3), 4, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), reef);
                    buildCornice(level, box, c, 6, c, Footprint.of(8, 7), reef, wood);
                }
                case LAKEBED_LIGHTHOUSE_RUINS -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState worn = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
                    BlockState light = ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState();
                    buildRadialButtresses(level, box, c, 1, c,
                            6, 11, 8, reef, worn);
                    buildArcadedWing(level, box, c, 1, c + 8, Footprint.of(10, 3), 5, 5, worn, reef, worn, light);
                    buildCardinalGateway(level, box, c, 1, c, 12, Direction.SOUTH,
                            3, 7, reef, worn);
                    buildStairCornice(level, box, c, 9, c, Footprint.square(11), ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(12), 5, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), worn);
                    buildCornice(level, box, c, 2, c, Footprint.square(11), ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(), ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState());
                }
                case MOSSBACK_TURTLE_NEST -> {
                    BlockState mud = ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
                    BlockState root = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
                    BlockState light = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();
                    buildArcadedWing(level, box, c, 1, c, Footprint.of(9, 7), 5, 3, root, mud, root, light);
                    buildRadialButtresses(level, box, c, 1, c,
                            8, 12, 5, mud, light);
                    buildStairCornice(level, box, c, 6, c, Footprint.of(9, 7), ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SILENT_MUDSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c, 1, c, Footprint.square(12), 5, ModBlocks.SILENT_MUDSTONE_BRICK_WALL.get().defaultBlockState(), root);
                    buildCornice(level, box, c, 2, c, Footprint.square(11), mud, root);
                }
                case UNDERGROUND_GREENHOUSE -> {
                    BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
                    BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_SONGSTONE.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 0, c, Footprint.of(12, 14), 8, 4, song, chiseled, glass, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
                    buildTieredTower(level, box, c - 14, 1, c,
                            3, 13, song, chiseled, glass);
                    buildTieredTower(level, box, c + 14, 1, c,
                            3, 13, song, chiseled, glass);
                    buildStairCornice(level, box, c, 9, c, Footprint.of(12, 14), ModBlocks.SONGSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SONGSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    for (int towerX : new int[]{c - 14, c + 14}) {
                        buildSpireFinial(level, box, towerX, 15, c, 3, 5, song, chiseled,
                                ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
                    }
                    buildParapet(level, box, c, 1, c, Footprint.square(16), 6, ModBlocks.SONGSTONE_BRICK_WALL.get().defaultBlockState(), chiseled);
                    buildCornice(level, box, c, 12, c, Footprint.square(15), song, chiseled);
                }
                case BROKEN_BELL_HUT -> {
                    BlockState ashen = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    BlockState scrap = ModBlocks.CLOCKWORK_SCRAP_BLOCK.get().defaultBlockState();
                    BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c, Footprint.of(8, 7), 6, 4, rusted, ashen, rusted,
                            ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState());
                    buildTieredTower(level, box, c + 10, 1, c - 7,
                            2, 12, ashen, rusted, candle);
                    buildStairCornice(level, box, c, 7, c, Footprint.of(8, 7), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 8, c, Footprint.of(8, 7), ashen, scrap);
                    buildParapet(level, box, c, 9, c, Footprint.of(9, 8), 4, ModBlocks.ASHEN_BELLSTONE_BRICK_WALL.get().defaultBlockState(), rusted);
                }
                case BROKEN_BELL_STELE -> {
                    BlockState ashen = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    BlockState scrap = ModBlocks.CLOCKWORK_SCRAP_BLOCK.get().defaultBlockState();

                    // 残钟碑必须用退台碑座:本文件已有五处柱环,再加一处就撞形。
                    buildSteppedZiggurat(level, box, c, 1, c, 9, 3, 3, 0,
                            ashen, rusted,
                            ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(),
                            ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState());
                    buildCardinalGateway(level, box, c, 1, c, 10, Direction.SOUTH,
                            2, 9, ashen, rusted);
                    buildCornice(level, box, c, 1, c, Footprint.square(9), ashen, scrap);
                    buildStairCornice(level, box, c, 10, c, Footprint.square(6), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                }
                case ARCHIVE_REMNANT_HALL -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();

                    BlockState lamp = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
                    buildRibbedVaultHall(level, box, c, 1, c, Footprint.of(9, 13), 8, 4, strata, strata, chiseled, lamp);
                    buildTwinTowerFacade(level, box, c, 1, c - 13,
                            7, 2, 12, strata, chiseled, lamp);
                    buildStairCornice(level, box, c, 10, c, Footprint.of(9, 13), ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STRATA_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 11, c, Footprint.of(9, 13), chiseled, mosaic);
                    buildParapet(level, box, c, 12, c, Footprint.of(9, 13), 5, ModBlocks.ECHO_STRATA_BRICK_WALL.get().defaultBlockState(), chiseled);
                    illuminateInterior(level, box, c, c, 8, 8, 12, 5, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState());
                }
                case CLIFF_BRIDGE -> {
                    // 桥面/桥墩/腰线必须分三种材质,同材质长桥在侧视里是一条等厚板。
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
                    buildMonumentalBridge(level, box, c, 2, c, Direction.SOUTH,
                            16, 3, 7, strata,
                            ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState(),
                            chiseled, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                    buildParapet(level, box, c, 3, c, Footprint.of(16, 3), 5, ModBlocks.ECHO_STRATA_BRICK_WALL.get().defaultBlockState(), chiseled);
                    buildStairCornice(level, box, c, 2, c, Footprint.of(16, 3), ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STRATA_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 1, c, Footprint.of(16, 3), chiseled, mosaic);
                }
                case ARCHIVE_GATE -> {
                    BlockState strata = ModBlocks.ECHO_STRATA_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
                    BlockState mosaic = ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState();
                    BlockState lamp = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
                    buildTwinTowerFacade(level, box, c, 1, c,
                            9, 3, 19, strata, chiseled, lamp);
                    buildArcadedWing(level, box, c, 1, c + 9, Footprint.of(12, 5), 7, 4, strata, strata, chiseled, lamp);
                    buildRuneCauseway(level, box, c, 0, c, Direction.SOUTH,
                            5, 18, 2, strata, chiseled, mosaic);
                    for (int towerOffset : new int[]{-9, 9}) {
                        buildStairCornice(level, box, c + towerOffset, 20, c, Footprint.square(3), ModBlocks.ECHO_STRATA_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STRATA_BRICK_SLAB.get().defaultBlockState(), true);
                        buildSpireFinial(level, box, c + towerOffset, 21, c,
                                3, 5, strata, chiseled, lamp);
                    }
                    buildCornice(level, box, c, 3, c, Footprint.square(14), ModBlocks.ECHO_STRATA.get().defaultBlockState(), ModBlocks.ARCHIVE_MOSAIC.get().defaultBlockState());
                }
            }
        }

        private void buildCampPerimeter(WorldGenLevel level, RandomSource random, BoundingBox box,
                                        int c, boolean echoRealm) {
            BlockState wall = echoRealm
                    ? ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState()
                    : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            BlockState post = echoRealm
                    ? ModBlocks.ECHO_LOG.get().defaultBlockState()
                    : Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
            BlockState accent = echoRealm
                    ? ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState()
                    : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            architecturalRing(level, box, c, 0, c, 13, wall);
            int[][] towers = {{c - 11, c - 9}, {c + 11, c - 9}, {c - 11, c + 9}, {c + 11, c + 9}};
            for (int[] tower : towers) {
                int height = 5 + random.nextInt(3);
                architecturalPillar(level, box, tower[0], 1, tower[1], height, post);
                this.placeBlock(level, accent, tower[0], height + 1, tower[1], box);
            }
            buildRuneCauseway(level, box, c, 0, c, Direction.SOUTH,
                    6, 14, 1, Blocks.COARSE_DIRT.defaultBlockState(), wall, accent);
            scatterArchitecturalDebris(level, box, kind.id, c, 1, c,
                    wall, accent);
        }

        /** 水下场点锚湖床(OCEAN_FLOOR):沿用父类 heightPosition 的一次性调整协议。 */
        private boolean updateLakeFloorHeight(WorldGenLevel level, BoundingBox bounds,
                                              ChunkGenerator generator) {
            if (this.heightPosition >= 0) {
                return true;
            }
            int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), kind.size, kind.size);
            if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
                return false;
            }
            this.heightPosition = floorY;
            this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() - 1, 0);
            return true;
        }

        private void memoryCourtyard(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState moss = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            disk(level, box, c, 0, c, 30, moss);
            buildSiteGuidedEntrance(level, box, c, brick);
            buildMemoryOuterSteles(level, random, box, c);
            buildMemoryBossArena(level, random, box, c);
            buildMemoryRetryShortcut(level, box, c);
            buildMemoryRevisitVault(level, random, box, c);
            BlockPos bossPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(2), this.getWorldZ(c, c));
            BlockPos altarPos = new BlockPos(this.getWorldX(c, c - 8), this.getWorldY(2), this.getWorldZ(c, c - 8));
            if (!this.spawnedMainBoss && box.isInside(bossPos)) {
                var boss = ModEntities.FORGOTTEN_COLOSSUS.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossPos.getX() + 0.5D, bossPos.getY(), bossPos.getZ() + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    boss.bindRechallengeArena(altarPos, bossPos);
                    if (level.addFreshEntity(boss)) {
                        this.spawnedMainBoss = true;
                    } else {
                        boss.discard();
                    }
                }
            }
        }

        private void buildMemoryOuterSteles(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            int[][] steles = {{c - 18, c - 23}, {c, c - 25}, {c + 18, c - 23}};
            for (int i = 0; i < steles.length; i++) {
                pillar(level, box, steles[i][0], 1, steles[i][1], 2 + i, cracked);
                this.placeBlock(level, rune, steles[i][0], 3 + i, steles[i][1], box);
            }
            buildAccessibleChest(level, random, box, c - 22, 1, c - 21, ModLootTables.T2_RUIN);
        }

        private void buildMemoryBossArena(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
            ring(level, box, c, 1, c, 24, brick);
            ring(level, box, c, 2, c, 13, cracked);
            for (int d : new int[]{-14, 14}) {
                memoryPillar(level, box, c + d, c + d);
                memoryPillar(level, box, c + d, c - d);
            }

            buildRibbedVaultHall(level, box, c, 2, c, Footprint.of(17, 27), 13, 6, brick, brick, cracked, rune);
            buildTwinTowerFacade(level, box, c, 2, c - 28,
                    22, 4, 20, brick, cracked, rune);
            buildArcadedWing(level, box, c - 24, 2, c, Footprint.of(6, 17), 10, 4, brick, cracked, chiseled, rune);
            buildArcadedWing(level, box, c + 24, 2, c, Footprint.of(6, 17), 10, 4, brick, cracked, chiseled, rune);
            buildBossApse(level, box, c, 2, c + 25, Direction.SOUTH,
                    8, 11, brick, brick, cracked, rune);
            // 必须补半塌残壳外墙:另外三座场地也用肋拱厅配方,废墟感是唯一区分点。
            buildRuinedShell(level, box, c, 2, c, 30, 30, 9,
                    cracked, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(),
                    brick, rune);

            disk(level, box, c, 1, c, 12, rune);
            buildBossArenaMandala(level, box, c, 1, c, 12,
                    cracked, brick, chiseled);
            BlockState lamp = ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState();
            buildArenaLightRing(level, box, c, 3, c, 14, 12, 9, brick, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState());
            illuminateInterior(level, box, c, c, 18, 15, 24, 6, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 15, c, Footprint.of(17, 27), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, c, 16, c, Footprint.of(17, 27), brick, cracked);
            buildParapet(level, box, c, 17, c, Footprint.of(17, 27), 8, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), cracked);
            pillar(level, box, c - 2, 2, c, 4, cracked);
            pillar(level, box, c + 2, 2, c, 3, cracked);
            this.placeBlock(level, ModBlocks.REWARD_ALTAR.get().defaultBlockState(), c, 2, c - 8, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c, 2, c + 8, box);
            buildAccessibleChest(level, random, box, c - 6, 2, c - 7,
                    ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void buildMemoryRetryShortcut(WorldGenLevel level, BoundingBox box, int c) {
            BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            for (int z = c + 25; z <= c + 30; z++) {
                for (int x = c - 2; x <= c + 2; x++) {
                    this.placeBlock(level, wind, x, 1, z, box);
                }
            }
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c, 2, c + 24, box);
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 2, c + 28, box);
        }

        private void buildMemoryRevisitVault(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c + 18, 1, c + 16, 9, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c + 20, 2, c + 18, box);
            buildTrueSightVaultNiche(level, random, box, c + 22, 2, c + 22, Direction.NORTH);
        }

        private void lostCamp(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c - 6, 0, c - 5, 13, Blocks.COARSE_DIRT.defaultBlockState());
            buildSiteGuidedEntrance(level, box, c, Blocks.COARSE_DIRT.defaultBlockState());
            for (int i = -5; i <= 5; i += 5) {
                pillar(level, box, c + i, 1, c - 4, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                platform(level, box, c + i - 2, 4, c - 5, 5, Blocks.BROWN_WOOL.defaultBlockState());
            }
            this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), c, 1, c, box);

            buildSealedManor(level, box, c - 7, 1, c + 6, 4, 4, 1, 5,
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(),
                    ModBlocks.ECHO_LOG.get().defaultBlockState(),
                    ModBlocks.ECHO_PLANKS.get().defaultBlockState(),
                    ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState(),
                    ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 4, 1, c + 3, box);
            buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.T2_RUIN);
            buildTrueSightVaultNiche(level, random, box, c + 5, 1, c + 5, Direction.NORTH);
        }

        /** 主世界失落营地：包含篝火、行囊、普通残页和可能出现的潦草地图。 */
        private void overworldLostCamp(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c - 6, 0, c - 5, 13, Blocks.COARSE_DIRT.defaultBlockState());
            buildSiteGuidedEntrance(level, box, c, Blocks.COARSE_DIRT.defaultBlockState());

            for (int i = -5; i <= 5; i += 5) {
                pillar(level, box, c + i, 1, c - 4, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                platform(level, box, c + i - 2, 4, c - 5, 5, Blocks.BROWN_WOOL.defaultBlockState());
            }

            this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), c, 1, c, box);
            this.placeBlock(level, Blocks.RED_WOOL.defaultBlockState(), c - 3, 1, c - 3, box);
            this.placeBlock(level, Blocks.WHITE_WOOL.defaultBlockState(), c - 3, 1, c - 2, box);
            this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), c + 3, 1, c - 3, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c + 4, 1, c + 2, box);

            buildSealedManor(level, box, c - 7, 1, c + 6, 4, 4, 1, 5,
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                    Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.GLASS.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState());

            buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.OVERWORLD_LOST_CAMP_CHEST);
        }

        private void windSteps(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.SKY_OBSERVATORY_CHEST);
            for (int i = 0; i < 10; i++) {
                platform(level, box, c - 4 + i, i, c - 8 + i, 8, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
                if (i % 3 == 0) {
                    // 台阶向东南叠高，西北角须留作有承托的风流起点。
                    this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c - 4 + i, i + 1, c - 8 + i, box);
                }
            }
            buildAccessibleChest(level, random, box, c + 5, 10, c + 5, ModLootTables.SKY_OBSERVATORY_CHEST);
        }

        private void windCircle(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            disk(level, box, c, 0, c, 9, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());

            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);

            int[] orders = shuffledOrders(puzzleRandom, 8);
            // 输入须在核心上方两格的重置范围内，东北柱须避开真视库房。
            int[][] runeOffsets = {{7, 0}, {5, 5}, {0, 7}, {-5, 5},
                    {-7, 0}, {-5, -5}, {0, -7}, {7, -5}};
            for (int i = 0; i < 8; i++) {
                int px = c + runeOffsets[i][0];
                int pz = c + runeOffsets[i][1];
                pillar(level, box, px, 1, pz, 2, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.ORDER, orders[i]),
                        px, 3, pz, box);
            }

            this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState(), c, 1, c, box);
            int[] sequence = java.util.Arrays.copyOf(shuffledOrders(puzzleRandom, 8), 5);
            BlockPos corePos = new BlockPos(this.getWorldX(c, c), this.getWorldY(1), this.getWorldZ(c, c));
            if (box.isInside(corePos) && level.getBlockEntity(corePos)
                    instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }

            BlockState wall = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            for (int x = c - 3; x <= c + 3; x++) {
                for (int y = 1; y <= 4; y++) {
                    this.placeBlock(level, wall, x, y, c - 8, box);
                }
            }
            for (int i = 0; i < sequence.length; i++) {
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.LIT, Boolean.TRUE)
                        .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.SOUTH)),
                        c - 2 + i, 4, c - 8, box);
            }

            buildSealedReward(level, random, box, c + 5, 1, c, ModLootTables.T3_PUZZLE);
        }

        /** 封印奖励:奖励箱四面 + 顶封印石(不可破坏),谜题核心解开后清除封印露出宝箱。 */
        private void buildSealedReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                       int x, int y, int z,
                                       net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();

            this.placeBlock(level, ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState(), x, y - 1, z, box);
            this.createChest(level, box, random, x, y, z, loot, null); // audit-ok: puzzle core clears sealed stones before chest access
            this.placeBlock(level, sealed, x, y + 1, z, box);
            this.placeBlock(level, sealed, x + 1, y, z, box);
            this.placeBlock(level, sealed, x - 1, y, z, box);
            this.placeBlock(level, sealed, x, y, z + 1, box);
            this.placeBlock(level, sealed, x, y, z - 1, box);
        }

        private static int[] shuffledOrders(RandomSource random, int size) {
            int[] arr = new int[size];
            for (int i = 0; i < size; i++) {
                arr[i] = i;
            }
            for (int i = size - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
            }
            return arr;
        }

        private void reflectionWell(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.MIRROR_LAKE_CACHE);
            disk(level, box, c, 0, c, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            for (int y = 1; y <= 5; y++) {
                ring(level, box, c, y, c, 4, y % 2 == 0
                        ? Blocks.GLASS.defaultBlockState()
                        : ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState());
            }
            disk(level, box, c, 1, c, 3, Blocks.WATER.defaultBlockState());
            this.placeBlock(level, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(), c, 1, c - 5, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 2, c - 5, box);
            buildAccessibleChest(level, random, box, c + 4, 1, c, ModLootTables.MIRROR_LAKE_CACHE);
            for (int[] node : REFLECTION_WELL_MIRROR_NODES) {
                this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                        c + node[0], 1, c + node[1], box);
            }
        }

        private void mirrorCache(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.MIRROR_LAKE_CACHE);
            platform(level, box, c - 5, 0, c - 5, 11, ModBlocks.MIRROR_SAND.get().defaultBlockState());
            platform(level, box, c - 3, 1, c - 3, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), c, 2, c, box);
            configureTrueSightVault(level, box, random, c, 2, c);
            buildAccessibleChest(level, random, box, c - 3, 2, c + 3, ModLootTables.MIRROR_LAKE_CACHE);
        }

        private void tideBuoys(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            disk(level, box, c, 0, c, 9, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
        }

        private void placeTideBuoyMarkers(WorldGenLevel level, BoundingBox box, int c) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0;
                int z = c + (int) Math.round(Math.sin(a) * 6);
                this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState(),
                        c + (int) Math.round(Math.cos(a) * 6), z < c ? 5 : 8, z, box);
            }
        }

        private void reefHut(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            disk(level, box, c, 0, c, 7, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState());
            hut(level, box, c, ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState(), ModBlocks.TIDEWOOD_LOG.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c + 2, 2, c + 2, box);
            buildAccessibleChest(level, random, box, c - 3, 1, c + 2, ModLootTables.T4_REGION_CORE);
        }

        private void lakebedLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            disk(level, box, c, 0, c, 10, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            for (int y = 1; y <= 10; y++) {
                ring(level, box, c, y, c, Math.max(3, 6 - y / 3), y % 3 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            }

            this.placeBlock(level, ModBlocks.TIDE_PILLAR.get().defaultBlockState(), c - 4, 1, c - 4, box);
            this.placeBlock(level, ModBlocks.TIDE_PILLAR.get().defaultBlockState(), c + 4, 1, c - 4, box);
            this.placeBlock(level, ModBlocks.TIDE_PILLAR.get().defaultBlockState(), c, 1, c + 3, box);
            this.placeBlock(level, ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c + 5, 1, c, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
            buildAccessibleChest(level, random, box, c - 5, 1, c, ModLootTables.T4_REGION_CORE);
        }

        private void illusionChamber(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            platform(level, box, c - 10, 0, c - 10, 21,
                    ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState());
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            for (int lane = -1; lane <= 1; lane++) {
                int screenX = c + lane * 6;
                int openingZ = c + lane * 4;
                for (int z = c - 7; z <= c + 7; z++) {
                    boolean opening = Math.abs(z - openingZ) <= 1;
                    for (int y = 1; y <= 5; y++) {
                        if (!opening || y >= 4) {
                            this.placeBlock(level, y == 4 && Math.floorMod(z - c, 3) == 0
                                            ? hidden : mirror,
                                    screenX, y, z, box);
                        }
                    }
                }
            }
            for (int x = c - 5; x <= c + 5; x += 5) {
                this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), x, 1, c, box);
                this.placeBlock(level, ModBlocks.MIRROR_SIGIL.get().defaultBlockState(), x, 1, c + 4, box);
            }
            buildSiteHiddenReward(level, random, box, c, 1, c - 5);
        }

        private void turtleNest(WorldGenLevel level, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            disk(level, box, c, 0, c, 7, ModBlocks.MUFFLE_MOSS.get().defaultBlockState());
            RandomSource nestRandom = RandomSource.create(this.boundingBox.minX() * 341873128712L
                    ^ this.boundingBox.minZ() * 132897987541L);
            for (int i = 0; i < 5; i++) {
                disk(level, box, c - 4 + i * 2, 1, c + nestRandom.nextInt(7) - 3, 2, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            }
        }

        private void greenhouse(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            BlockState frame = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            for (int x = c - 12; x <= c + 12; x++) {
                for (int z = c - 14; z <= c + 14; z++) {
                    boolean border = Math.abs(x - c) == 12 || Math.abs(z - c) == 14;
                    boolean channel = Math.abs(x - c) == 2 && Math.abs(z - c) < 11;
                    this.placeBlock(level, border ? frame : channel
                                    ? Blocks.WATER.defaultBlockState() : moss,
                            x, 0, z, box);
                }
            }
            for (int z = c - 10; z <= c + 10; z += 4) {
                for (int x = c - 9; x <= c + 9; x += 3) {
                    BlockState plant = Math.floorMod(x + z, 2) == 0
                            ? ModBlocks.DREAM_FLOWER.get().defaultBlockState()
                            : ModBlocks.DREAM_MIST_VINE.get().defaultBlockState();
                    this.placeBlock(level, plant, x, 1, z, box);
                }
            }
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 1, c + 6, ModLootTables.T4_REGION_CORE);
        }

        private void placeBudgetedResourceNodes(WorldGenLevel level, BoundingBox box, int c) {
            if (kind != Kind.UNDERGROUND_GREENHOUSE) {
                return;
            }
            for (int[] node : GREENHOUSE_GEODE_NODES) {
                this.placeBlock(level, ModBlocks.RESONANT_GEODE.get().defaultBlockState(),
                        c + node[0], 1, c + node[1], box);
            }
        }

        private void brokenHut(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            platform(level, box, c - 6, 0, c - 6, 13, Blocks.MUD_BRICKS.defaultBlockState());
            hut(level, box, c, Blocks.COPPER_BLOCK.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState());
            this.placeBlock(level, Blocks.COARSE_DIRT.defaultBlockState(), c + 4, 0, c - 3, box);
            this.placeBlock(level, ModBlocks.BROKEN_BELL_THORN.get().defaultBlockState(), c + 4, 1, c - 3, box);
            buildAccessibleChest(level, random, box, c - 3, 1, c + 2, ModLootTables.BROKEN_ARCHIVE_CHEST);
        }

        private void brokenStele(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            disk(level, box, c, 0, c, 5, ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState());
            for (int[] node : BROKEN_STELE_ORE_NODES) {
                this.placeBlock(level, ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState(),
                        c + node[0], 0, c + node[1], box);
            }
            pillar(level, box, c, 1, c, 9, Blocks.COPPER_BLOCK.defaultBlockState());
            this.placeBlock(level, Blocks.GOLD_BLOCK.defaultBlockState(), c, 10, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c - 4, box);
        }

        private void archiveHall(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.T5_MINIBOSS_REPEAT);
            platform(level, box, c - 8, 0, c - 8, 17, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState());
            for (int x = c - 7; x <= c + 7; x += 3) {
                pillar(level, box, x, 1, c - 6, 6, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c + 5, 1, c + 3, ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void buildGroundEntry(WorldGenLevel level, BoundingBox box) {
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState stairs = ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH);
            for (int x = 20; x <= 22; x++) {
                this.placeBlock(level, bricks, x, 0, 5, box);
                this.placeBlock(level, stairs, x, 1, 5, box);
                this.placeBlock(level, stairs, x, 0, 4, box);
            }
        }

        private void cliffBridge(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            buildSiteGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            buildSiteBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.T4_REGION_CORE);
            for (int z = c - 14; z <= c + 14; z++) {
                for (int x = c - 2; x <= c + 2; x++) {
                    this.placeBlock(level, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState(), x, 2, z, box);
                }
                if ((z - c) % 5 == 0) {
                    pillar(level, box, c - 3, 0, z, 5, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                    pillar(level, box, c + 3, 0, z, 5, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                }
            }
            buildAccessibleChest(level, random, box, c, 3, c + 10, ModLootTables.T4_REGION_CORE);
        }

        private void archiveGate(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteRewardLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            platform(level, box, c - 10, 0, c - 4, 21, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState());
            for (int y = 1; y <= 15; y++) {
                pillar(level, box, c - 8, y, c, 1, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                pillar(level, box, c + 8, y, c, 1, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                if (y > 11) {
                    for (int x = c - 7; x <= c + 7; x++) {
                        this.placeBlock(level, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), x, y, c, box);
                    }
                }
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c - 2, box);
            buildAccessibleChest(level, random, box, c, 1, c + 3, ModLootTables.T4_REGION_CORE);
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

        private void memoryPillar(WorldGenLevel level, BoundingBox box, int x, int z) {
            pillar(level, box, x, 1, z, 3, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.MEMORY_PILLAR.get().defaultBlockState(), x, 4, z, box);
        }

        @Override
        protected void buildSiteGuidedEntrance(WorldGenLevel level, BoundingBox box, int c,
                                               BlockState state) {
            for (int z = c + 4; z <= kind.size - 2; z++) {
                for (int x = c - 1; x <= c + 1; x++) {
                    this.placeBlock(level, state, x, 1, z, box);
                }
                if ((z - c) % 4 == 0) {
                    this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c - 2, 1, z, box);
                    this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c + 2, 1, z, box);
                }
            }
        }

        private void hut(WorldGenLevel level, BoundingBox box, int c, BlockState wall, BlockState post) {
            BlockState air = Blocks.AIR.defaultBlockState();
            platform(level, box, c - 4, 1, c - 4, 9, wall);

            this.generateBox(level, box, c - 3, 2, c - 3, c + 3, 4, c + 3, air, air, false);

            for (int y = 2; y <= 4; y++) {
                for (int offset = -4; offset <= 4; offset++) {
                    boolean windowBand = y == 3 && Math.floorMod(offset + 4, 3) == 1;
                    BlockState north = windowBand ? ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState() : wall;

                    this.placeBlock(level, Math.abs(offset) == 4 ? post : north,
                            c + offset, y, c - 4, box);
                    this.placeBlock(level, Math.abs(offset) == 4 ? post : north,
                            c + offset, y, c + 4, box);
                    this.placeBlock(level, Math.abs(offset) == 4 ? post : north,
                            c - 4, y, c + offset, box);
                    this.placeBlock(level, Math.abs(offset) == 4 ? post : north,
                            c + 4, y, c + offset, box);
                }
            }

            for (int x = c - 3; x <= c + 3; x++) {
                for (int y = 2; y <= 4; y++) {
                    boolean doorway = Math.abs(x - c) <= 1 && y <= 3;
                    this.placeBlock(level, doorway ? air : (y == 4 ? post : wall), x, y, c + 1, box);
                }
            }

            platform(level, box, c - 5, 5, c - 5, 11, Blocks.SPRUCE_PLANKS.defaultBlockState());

            this.placeBlock(level, air, c, 2, c - 4, box);
            this.placeBlock(level, air, c, 3, c - 4, box);

            this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), c - 3, 2, c - 3, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c + 3, 2, c - 3, box);
            this.placeBlock(level, Blocks.LIGHT_GRAY_WOOL.defaultBlockState(), c - 3, 2, c + 3, box);
            this.placeBlock(level, Blocks.WHITE_WOOL.defaultBlockState(), c - 2, 2, c + 3, box);
            this.placeBlock(level, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState(), c, 4, c - 2, box);
            this.placeBlock(level, ModBlocks.ARCHIVE_LANTERN_GLASS.get().defaultBlockState(), c, 4, c + 3, box);
        }

        private void wall(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int size, int height, BlockState state) {
            for (int y = y0; y < y0 + height; y++) {
                ring(level, box, x0 + size / 2, y, z0 + size / 2, size / 2, state);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0, int size, BlockState state) {
            for (int x = x0; x < x0 + size; x++) {
                for (int z = z0; z < z0 + size; z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z, int height, BlockState state) {
            for (int i = 0; i < height; i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
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

        private void ring(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            int outer = radius * radius;
            int inner = Math.max(0, radius - 1) * Math.max(0, radius - 1);
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                    if (d2 <= outer && d2 >= inner) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("RegionSiteKind", this.kind.id);
            tag.putBoolean("SpawnedMainBoss", this.spawnedMainBoss);
        }
    }
}
