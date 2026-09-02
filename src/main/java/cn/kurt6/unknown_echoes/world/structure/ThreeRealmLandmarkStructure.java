package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.RealmServicePlinthBlock;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 三个维度的非主线综合巨构。每座结构都提供完整徒步路线、功能设施和可重复普通奖励。 */
public class ThreeRealmLandmarkStructure extends SinglePieceStructure {
    public static final MapCodec<ThreeRealmLandmarkStructure> FAR_ECHO_CARTOGRAPHY_COURT_CODEC =
            codec(Kind.FAR_ECHO_CARTOGRAPHY_COURT);
    public static final MapCodec<ThreeRealmLandmarkStructure> CHORUS_CONFLUENCE_COURT_CODEC =
            codec(Kind.CHORUS_CONFLUENCE_COURT);
    public static final MapCodec<ThreeRealmLandmarkStructure> PALIMPSEST_ECOLOGY_DOME_CODEC =
            codec(Kind.PALIMPSEST_ECOLOGY_DOME);
    public static final MapCodec<ThreeRealmLandmarkStructure> FAR_BELL_WAYFARER_HARBOR_CODEC =
            codec(Kind.FAR_BELL_WAYFARER_HARBOR);
    public static final MapCodec<ThreeRealmLandmarkStructure> PRISMATIC_CANOPY_CONSERVATORY_CODEC =
            codec(Kind.PRISMATIC_CANOPY_CONSERVATORY);
    public static final MapCodec<ThreeRealmLandmarkStructure> INTERLAYER_EXPEDITION_RING_CODEC =
            codec(Kind.INTERLAYER_EXPEDITION_RING);

    private final Kind kind;

    private static MapCodec<ThreeRealmLandmarkStructure> codec(Kind kind) {
        return simpleCodec(settings -> new ThreeRealmLandmarkStructure(settings, kind));
    }

    private ThreeRealmLandmarkStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case FAR_ECHO_CARTOGRAPHY_COURT -> ModStructures.FAR_ECHO_CARTOGRAPHY_COURT.get();
            case CHORUS_CONFLUENCE_COURT -> ModStructures.CHORUS_CONFLUENCE_COURT.get();
            case PALIMPSEST_ECOLOGY_DOME -> ModStructures.PALIMPSEST_ECOLOGY_DOME.get();
            case FAR_BELL_WAYFARER_HARBOR -> ModStructures.FAR_BELL_WAYFARER_HARBOR.get();
            case PRISMATIC_CANOPY_CONSERVATORY -> ModStructures.PRISMATIC_CANOPY_CONSERVATORY.get();
            case INTERLAYER_EXPEDITION_RING -> ModStructures.INTERLAYER_EXPEDITION_RING.get();
        };
    }

    public enum Kind {
        FAR_ECHO_CARTOGRAPHY_COURT("far_echo_cartography_court", 69, 38),
        CHORUS_CONFLUENCE_COURT("chorus_confluence_court", 77, 44),
        PALIMPSEST_ECOLOGY_DOME("palimpsest_ecology_dome", 85, 50),
        FAR_BELL_WAYFARER_HARBOR("far_bell_wayfarer_harbor", 75, 42),
        PRISMATIC_CANOPY_CONSERVATORY("prismatic_canopy_conservatory", 83, 48),
        INTERLAYER_EXPEDITION_RING("interlayer_expedition_ring", 91, 54);

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
            return FAR_ECHO_CARTOGRAPHY_COURT;
        }
    }

    public static class Piece extends EchoStructurePiece {
        /**
         * 点亮的原版铜灯。
         * 默认 lit=false 的铜灯光照为 0，直接用 defaultBlockState 当柱头灯会得到一座完全不发光的"灯柱"。
         */
        private static final BlockState LIT_COPPER_BULB = Blocks.COPPER_BULB.defaultBlockState()
                .setValue(BlockStateProperties.LIT, Boolean.TRUE);

        private Kind kind;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.THREE_REALM_LANDMARK_PIECE.get(), x, 64, z,
                    kind.size, kind.height, kind.size,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.THREE_REALM_LANDMARK_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("ThreeRealmLandmarkKind"));
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearPlayableVolume(level, box);
            switch (kind) {
                case FAR_ECHO_CARTOGRAPHY_COURT -> buildCartographyCourt(level, random, box);
                case CHORUS_CONFLUENCE_COURT -> buildConfluenceCourt(level, random, box);
                case PALIMPSEST_ECOLOGY_DOME -> buildEcologyDome(level, random, box);
                case FAR_BELL_WAYFARER_HARBOR -> buildWayfarerHarbor(level, random, box);
                case PRISMATIC_CANOPY_CONSERVATORY -> buildPrismaticConservatory(level, random, box);
                case INTERLAYER_EXPEDITION_RING -> buildExpeditionRing(level, random, box);
            }
            addLandmarkGameplay(level, random, box);
        }

        /**
         * 地标可玩层:外圈城郭补全轮廓与步行路线,南向投影接力廊守住一份区域奖励, 主题刷怪笼提供普通遭遇,
         * 三座地标叠加顺序封印室;机关可重复、服务端判定, 不承载唯一关键进度。
         */
        private void addLandmarkGameplay(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case FAR_ECHO_CARTOGRAPHY_COURT -> {
                    BlockState floor = Blocks.POLISHED_TUFF.defaultBlockState();
                    BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
                    BlockState trim = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 32,
                            Blocks.TUFF_BRICKS.defaultBlockState(), trim,
                            Blocks.SEA_LANTERN.defaultBlockState());
                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            floor, wall, trim, ModLootTables.FAR_ECHO_CARTOGRAPHY_CACHE);
                    placeStructureSpawner(level, random, box, c + 18, 1, c - 7,
                            ModEntities.LOST_RECORDER.get(), floor, trim);
                }
                case CHORUS_CONFLUENCE_COURT -> {
                    BlockState echo = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    BlockState bell = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 36, echo, rune,
                            ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());

                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            bell, echo, rune, ModLootTables.CHORUS_CONFLUENCE_CACHE);
                    buildSequenceSeal(level, random, box, c - 18, 1, c + 14, Direction.EAST,
                            echo, bell, rune, ModLootTables.T3_PUZZLE);

                    buildChimeEchoCourt(level, random, box, c + 14, 1, c + 14, Direction.EAST,
                            bell, echo, rune, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c + 24, 1, c - 10,
                            ModEntities.ECHO_WANDERER.get(), echo, rune);
                }
                case PALIMPSEST_ECOLOGY_DOME -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 40, folded, chiseled,
                            ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState());
                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            folded, gravity, chiseled, ModLootTables.PALIMPSEST_ECOLOGY_CACHE);
                    placeStructureSpawner(level, random, box, c + 26, 1, c - 10,
                            ModEntities.FOLDPAGE_BUG.get(), folded, chiseled);
                }
                case FAR_BELL_WAYFARER_HARBOR -> {
                    BlockState floor = Blocks.POLISHED_TUFF.defaultBlockState();
                    BlockState wall = Blocks.TUFF_BRICKS.defaultBlockState();
                    BlockState copper = Blocks.WEATHERED_COPPER.defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 35, wall,
                            Blocks.CHISELED_TUFF_BRICKS.defaultBlockState(),
                            Blocks.SEA_LANTERN.defaultBlockState());
                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            floor, wall, copper, ModLootTables.FAR_BELL_WAYFARER_CACHE);

                    buildSequenceSeal(level, random, box, c + 14, 1, c, Direction.EAST,
                            floor, wall, copper, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c + 24, 1, c - 8,
                            ModEntities.ECHO_REMNANT.get(), floor, copper);
                }
                case PRISMATIC_CANOPY_CONSERVATORY -> {
                    BlockState echo = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    BlockState glass = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 39, echo, rune, glass);
                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            echo, echo, glass, ModLootTables.PRISMATIC_CANOPY_CACHE);
                    placeStructureSpawner(level, random, box, c + 26, 1, c - 10,
                            ModEntities.CRYSTAL_NOISE_WISP.get(), echo, glass);
                }
                case INTERLAYER_EXPEDITION_RING -> {
                    BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
                    BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
                    buildOuterBailey(level, box, c, 1, c, 43, folded, chiseled,
                            ModBlocks.MUTE_CRYSTAL.get().defaultBlockState());
                    buildProjectionRelayGallery(level, random, box, c, 1, c + 19, Direction.SOUTH,
                            folded, gravity, chiseled, ModLootTables.INTERLAYER_EXPEDITION_CACHE);

                    buildSequenceSeal(level, random, box, c - 16, 1, c, Direction.WEST,
                            folded, gravity, chiseled, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c + 28, 1, c - 11,
                            ModEntities.VOID_WANDERER.get(), folded, chiseled);
                }
            }
        }

        private void buildCartographyCourt(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState foundation = Blocks.TUFF_BRICKS.defaultBlockState();
            BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
            BlockState trim = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
            BlockState copper = Blocks.WEATHERED_COPPER.defaultBlockState();
            BlockState roof = Blocks.CUT_COPPER.defaultBlockState();

            architecturalDisk(level, box, c, 0, c, 28, foundation);
            architecturalRing(level, box, c, 1, c, 27, trim);
            architecturalRing(level, box, c, 1, c, 22, Blocks.POLISHED_TUFF.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 27, direction, 3, 9, wall, copper);
                buildRuneCauseway(level, box, c, 1, c, direction, 9, 27, 2,
                        Blocks.SMOOTH_STONE.defaultBlockState(), trim,
                        ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            }
            buildCircularColonnade(level, box, c, 1, c, 24, 16, 10, wall,
                    LIT_COPPER_BULB, trim);
            buildStairCornice(level, box, c, 1, c, Footprint.square(26), Blocks.TUFF_BRICK_STAIRS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(27), 6, Blocks.TUFF_BRICK_WALL.defaultBlockState(), copper);

            buildSteppedDisk(level, box, c, 1, c, 12, 4, foundation, Blocks.SMOOTH_STONE.defaultBlockState());
            buildSurveyRotunda(level, box, c, wall, copper, roof);

            int[][] pavilions = {{-18, -18}, {18, -18}, {-18, 18}, {18, 18}};
            for (int i = 0; i < pavilions.length; i++) {
                int x = c + pavilions[i][0];
                int z = c + pavilions[i][1];
                buildMapPavilion(level, box, x, z, wall, trim, roof, i);
                placeRewardChest(level, random, box, x, 2, z,
                        ModLootTables.FAR_ECHO_CARTOGRAPHY_CACHE, foundation, copper);
            }
            placeRewardChest(level, random, box, c, 6, c,
                    ModLootTables.FAR_ECHO_CARTOGRAPHY_VAULT, foundation, copper);

            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), c - 3, 5, c, box);
            this.placeBlock(level, Blocks.LECTERN.defaultBlockState(), c + 3, 5, c, box);

            this.placeBlock(level, Blocks.SMITHING_TABLE.defaultBlockState(), c + 8, 2, c + 15, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c + 15, 2, c, box);
            this.placeBlock(level, Blocks.FURNACE.defaultBlockState(), c - 15, 2, c, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.WAYFARER),
                    c, 2, c - 15, box);
            decorateSurveyGardens(level, random, box, c);
            buildSurveyRookery(level, box, c);
        }

        private void buildSurveyRotunda(WorldGenLevel level, BoundingBox box, int c,
                                        BlockState wall, BlockState copper, BlockState roof) {
            architecturalDisk(level, box, c, 5, c, 8, Blocks.SMOOTH_STONE.defaultBlockState());
            buildCircularColonnade(level, box, c, 5, c, 8, 12, 12, wall, LIT_COPPER_BULB, roof);
            architecturalRing(level, box, c, 12, c, 7, copper);
            architecturalRing(level, box, c, 16, c, 5, roof);
            architecturalRing(level, box, c, 20, c, 3, copper);
            architecturalPillar(level, box, c, 5, c, 18, Blocks.COPPER_BLOCK.defaultBlockState());
            this.placeBlock(level, Blocks.LIGHTNING_ROD.defaultBlockState(), c, 23, c, box);
            buildStairCornice(level, box, c, 5, c, Footprint.square(8), Blocks.TUFF_BRICK_STAIRS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), true);
            buildParapet(level, box, c, 6, c, Footprint.square(8), 5, Blocks.TUFF_BRICK_WALL.defaultBlockState(), copper);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = c + direction.getStepX() * 9;
                int z = c + direction.getStepZ() * 9;
                buildBlockRamp(level, box, x, 1, z,
                        -direction.getStepX(), -direction.getStepZ(), 10, 3,
                        Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }

        private void buildMapPavilion(WorldGenLevel level, BoundingBox box, int x, int z,
                                      BlockState wall, BlockState trim, BlockState roof, int phase) {
            architecturalDisk(level, box, x, 1, z, 6, Blocks.POLISHED_TUFF.defaultBlockState());
            buildCircularColonnade(level, box, x, 2, z, 5, 8, 7 + phase % 2,
                    wall, LIT_COPPER_BULB, roof);
            architecturalRing(level, box, x, 10 + phase % 2, z, 4, roof);
            buildCornice(level, box, x, 9 + phase % 2, z, Footprint.square(4), roof, trim);
            this.placeBlock(level, Blocks.SEA_LANTERN.defaultBlockState(), x, 11 + phase % 2, z, box);
        }

        private void decorateSurveyGardens(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            int[][] beds = {{0, -18}, {18, 0}, {0, 18}, {-18, 0}};
            for (int[] bed : beds) {
                int cx = c + bed[0];
                int cz = c + bed[1];
                architecturalDisk(level, box, cx, 1, cz, 5, Blocks.MOSS_BLOCK.defaultBlockState());
                for (int i = 0; i < 10; i++) {
                    int x = cx - 3 + random.nextInt(7);
                    int z = cz - 3 + random.nextInt(7);
                    BlockState plant = random.nextBoolean()
                            ? Blocks.AZURE_BLUET.defaultBlockState()
                            : Blocks.CORNFLOWER.defaultBlockState();
                    this.placeBlock(level, plant, x, 2, z, box);
                }
                this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), cx, 2, cz, box);
            }
        }

        private void buildSurveyRookery(WorldGenLevel level, BoundingBox box, int c) {
            int x = c + 11;
            int z = c - 11;
            architecturalDisk(level, box, x, 1, z, 5, Blocks.POLISHED_TUFF.defaultBlockState());
            architecturalRing(level, box, x, 2, z, 5, Blocks.WEATHERED_COPPER.defaultBlockState());
            architecturalDisk(level, box, x, 2, z, 3, Blocks.MOSS_BLOCK.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int px = x + direction.getStepX() * 4;
                int pz = z + direction.getStepZ() * 4;
                architecturalPillar(level, box, px, 2, pz, 4,
                        Blocks.STRIPPED_OAK_LOG.defaultBlockState());
                this.placeBlock(level, Blocks.LANTERN.defaultBlockState(), px, 6, pz, box);
            }
            this.placeBlock(level, Blocks.NOTE_BLOCK.defaultBlockState(), x - 2, 3, z + 1, box);
            this.placeBlock(level, Blocks.HAY_BLOCK.defaultBlockState(), x + 2, 3, z + 1, box);
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.WAYFARER),
                    x, 3, z, box);
        }

        private void buildConfluenceCourt(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState echo = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState song = ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState();
            BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();

            architecturalDisk(level, box, c, 0, c, 32, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            architecturalRing(level, box, c, 1, c, 31, rune);
            buildCircularColonnade(level, box, c, 1, c, 29, 24, 13,
                    echo, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), rune);
            buildStairCornice(level, box, c, 1, c, Footprint.square(30), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(31), 6, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 31, direction, 4, 12, echo, rune);
                buildRuneCauseway(level, box, c, 1, c, direction, 8, 31, 3,
                        echo, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(), rune);
            }

            BlockState[] palettes = {mirror, song, reef};
            Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.WEST};
            for (int i = 0; i < directions.length; i++) {
                Direction direction = directions[i];
                int x = c + direction.getStepX() * 19;
                int z = c + direction.getStepZ() * 19;
                buildThemedWing(level, box, x, z, palettes[i], rune, i);
                placeRewardChest(level, random, box, x, 3, z,
                        ModLootTables.CHORUS_CONFLUENCE_CACHE, palettes[i], rune);
            }

            buildConfluenceStage(level, box, c, echo, rune);
            placeRewardChest(level, random, box, c, 7, c,
                    ModLootTables.CHORUS_CONFLUENCE_VAULT, echo, rune);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c - 4, 6, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(), c + 4, 6, c, box);
            this.placeBlock(level, Blocks.LECTERN.defaultBlockState(), c + 8, 2, c + 12, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.CONFLUENCE),
                    c, 2, c - 12, box);
            decorateConfluenceBeds(level, random, box, c);

            buildSealedManor(level, box, c + 18, 1, c + 8, 4, 4, 2, 5,
                    ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ECHO_PLANKS.get().defaultBlockState(),
                    ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                    ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            buildConfluenceListeningSanctuary(level, box, c);
        }

        private void buildThemedWing(WorldGenLevel level, BoundingBox box, int x, int z,
                                     BlockState material, BlockState rune, int phase) {
            architecturalDisk(level, box, x, 2, z, 8, material);
            buildCircularColonnade(level, box, x, 3, z, 7, 12, 9 + phase,
                    material, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), rune);
            architecturalRing(level, box, x, 13 + phase, z, 6, material);
            architecturalRing(level, box, x, 15 + phase, z, 4, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), x, 16 + phase, z, box);
        }

        private void buildConfluenceStage(WorldGenLevel level, BoundingBox box, int c,
                                          BlockState echo, BlockState rune) {
            buildSteppedDisk(level, box, c, 1, c, 12, 5, echo, rune);
            buildCircularColonnade(level, box, c, 6, c, 10, 12, 12,
                    echo, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), rune);
            for (int y = 9; y <= 28; y += 4) {
                int radius = Math.max(3, 11 - (y - 9) / 3);
                architecturalRing(level, box, c, y, c, radius,
                        y % 8 == 1 ? rune : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            }
            architecturalPillar(level, box, c, 6, c, 24, ModBlocks.ECHO_LOG.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c, 30, c, box);
            buildSpireFinial(level, box, c, 31, c, 2, 5, echo, rune,
                    ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 6, c, Footprint.square(10), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 7, c, Footprint.square(10), 5, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
        }

        private void decorateConfluenceBeds(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            int[][] centers = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}};
            for (int i = 0; i < centers.length; i++) {
                int cx = c + centers[i][0];
                int cz = c + centers[i][1];
                architecturalDisk(level, box, cx, 1, cz, 5, Blocks.MOSS_BLOCK.defaultBlockState());
                for (int n = 0; n < 12; n++) {
                    int x = cx - 3 + random.nextInt(7);
                    int z = cz - 3 + random.nextInt(7);
                    BlockState plant = switch ((n + i) % 4) {
                        case 0 -> ModBlocks.ECHO_FLOWER.get().defaultBlockState();
                        case 1 -> ModBlocks.DREAM_FLOWER.get().defaultBlockState();
                        case 2 -> ModBlocks.STAR_MIST_BLOOM.get().defaultBlockState();
                        default -> ModBlocks.GLOW_FERN.get().defaultBlockState();
                    };
                    this.placeBlock(level, plant, x, 2, z, box);
                }
            }
        }

        private void buildConfluenceListeningSanctuary(WorldGenLevel level, BoundingBox box, int c) {
            int x = c - 12;
            int z = c - 12;
            BlockState echo = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            architecturalDisk(level, box, x, 1, z, 5, echo);
            architecturalRing(level, box, x, 2, z, 5, rune);
            architecturalDisk(level, box, x, 2, z, 3,
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            int[][] pillars = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
            for (int[] pillar : pillars) {
                architecturalPillar(level, box, x + pillar[0], 2, z + pillar[1], 5,
                        ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                        x + pillar[0], 7, z + pillar[1], box);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                        x + direction.getStepX() * 3, 3,
                        z + direction.getStepZ() * 3, box);
            }
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.CONFLUENCE),
                    x, 3, z, box);
        }

        private void buildEcologyDome(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState abyss = ModBlocks.ABYSSAL_STONE_BRICKS.get().defaultBlockState();
            BlockState glass = Blocks.TINTED_GLASS.defaultBlockState();

            architecturalDisk(level, box, c, 0, c, 36, ModBlocks.FOLDED_SLATE.get().defaultBlockState());
            architecturalRing(level, box, c, 1, c, 35, ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState());
            buildCircularColonnade(level, box, c, 1, c, 33, 24, 16,
                    folded, ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState(), chiseled);
            buildStairCornice(level, box, c, 1, c, Footprint.square(34), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(35), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), chiseled);
            buildRadialButtresses(level, box, c, 1, c, 25, 34, 12,
                    gravity, ModBlocks.MUTE_CRYSTAL.get().defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 35, direction, 4, 14, folded, chiseled);
                buildRuneCauseway(level, box, c, 1, c, direction, 9, 35, 3,
                        folded, gravity, ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState());
            }

            int[][] conservatories = {{-21, -21}, {21, -21}, {-21, 21}, {21, 21}};
            for (int i = 0; i < conservatories.length; i++) {
                int x = c + conservatories[i][0];
                int z = c + conservatories[i][1];
                buildDeepConservatory(level, random, box, x, z, i, folded, glass);
                placeRewardChest(level, random, box, x, 3, z,
                        ModLootTables.PALIMPSEST_ECOLOGY_CACHE, folded, chiseled);
                this.placeBlock(level, ModBlocks.DEEP_PROVISION_BRAZIER.get().defaultBlockState(),
                        x + (i % 2 == 0 ? 4 : -4), 3, z, box);
            }

            buildEcologySpire(level, box, c, abyss, glass);
            buildHelicalWalkway(level, box, c, 9, 5, 31, gravity,
                    ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState());
            buildRuneCauseway(level, box, c, 31, c, Direction.SOUTH, 2, 9, 1,
                    gravity, chiseled, ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState());
            placeRewardChest(level, random, box, c, 32, c,
                    ModLootTables.PALIMPSEST_ECOLOGY_VAULT, abyss,
                    ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState());
            this.placeBlock(level, Blocks.LECTERN.defaultBlockState(), c + 8, 2, c + 14, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.INTERLAYER),
                    c, 2, c - 14, box);
            buildPalimpsestObservationHabitat(level, box, c);

            buildSealedManor(level, box, c + 20, 1, c + 8, 4, 4, 3, 5,
                    ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState(),
                    ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState(),
                    ModBlocks.FOLDED_SLATE.get().defaultBlockState(),
                    Blocks.TINTED_GLASS.defaultBlockState(),
                    ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
        }

        private void buildDeepConservatory(WorldGenLevel level, RandomSource random, BoundingBox box,
                                           int cx, int cz, int phase, BlockState folded, BlockState glass) {
            architecturalDisk(level, box, cx, 1, cz, 9, folded);
            buildCircularColonnade(level, box, cx, 2, cz, 8, 12, 10,
                    folded, ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState(), glass);
            for (int y = 8; y <= 18; y += 2) {
                int radius = Math.max(2, 9 - (y - 8) / 2);
                architecturalRing(level, box, cx, y, cz, radius,
                        y % 4 == 0 ? glass : ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState());
            }

            BlockState soil = switch (phase) {
                case 0 -> ModBlocks.FOLDED_SLATE.get().defaultBlockState();
                case 1 -> ModBlocks.GRAVITY_SHALE.get().defaultBlockState();
                case 2 -> ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
                default -> ModBlocks.SILENT_MUDSTONE.get().defaultBlockState();
            };
            architecturalDisk(level, box, cx, 2, cz, 5, soil);
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8.0D;
                int x = cx + (int) Math.round(Math.cos(angle) * (2 + i % 4));
                int z = cz + (int) Math.round(Math.sin(angle) * (2 + i % 4));
                BlockState plant = switch (phase) {
                    case 0 -> i % 3 == 0 ? ModBlocks.ARCHIVE_FUNGUS.get().defaultBlockState()
                            : ModBlocks.SEAM_GRASS.get().defaultBlockState();
                    case 1 -> i % 3 == 0 ? ModBlocks.WEIGHTLESS_BLOOM.get().defaultBlockState()
                            : ModBlocks.INVERSION_REED.get().defaultBlockState();
                    case 2 -> i % 3 == 0 ? ModBlocks.VOIDCAP.get().defaultBlockState()
                            : ModBlocks.HUSH_GRASS.get().defaultBlockState();
                    default -> i % 3 == 0 ? ModBlocks.MIRE_BELL.get().defaultBlockState()
                            : ModBlocks.MUTE_REED.get().defaultBlockState();
                };
                this.placeBlock(level, plant, x, 3, z, box);
            }
        }

        private void buildPalimpsestObservationHabitat(WorldGenLevel level, BoundingBox box, int c) {
            int x = c - 12;
            int z = c + 12;
            BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState glass = Blocks.TINTED_GLASS.defaultBlockState();
            architecturalDisk(level, box, x, 1, z, 5, folded);
            architecturalRing(level, box, x, 2, z, 5,
                    ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState());
            architecturalDisk(level, box, x, 2, z, 3,
                    ModBlocks.ABYSSAL_STONE.get().defaultBlockState());
            int[][] corners = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
            for (int[] corner : corners) {
                architecturalPillar(level, box, x + corner[0], 2, z + corner[1], 6, glass);
                this.placeBlock(level, ModBlocks.MUTE_CRYSTAL.get().defaultBlockState(),
                        x + corner[0], 8, z + corner[1], box);
            }
            this.placeBlock(level, ModBlocks.DEEP_ROUTE_NODE.get().defaultBlockState(),
                    x - 3, 3, z, box);
            this.placeBlock(level, ModBlocks.DEEP_ROUTE_NODE.get().defaultBlockState(),
                    x + 3, 3, z, box);
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.INTERLAYER),
                    x, 3, z, box);
        }

        private void buildEcologySpire(WorldGenLevel level, BoundingBox box, int c,
                                       BlockState abyss, BlockState glass) {
            buildSteppedDisk(level, box, c, 1, c, 13, 5, abyss,
                    ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState());
            buildCircularColonnade(level, box, c, 6, c, 11, 16, 24,
                    abyss, ModBlocks.MUTE_CRYSTAL.get().defaultBlockState(), glass);
            for (int y = 10; y <= 38; y += 4) {
                int radius = Math.max(3, 12 - (y - 10) / 4);
                architecturalRing(level, box, c, y, c, radius,
                        y % 8 == 2 ? ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState() : glass);
            }
            architecturalPillar(level, box, c, 6, c, 34,
                    ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState(), c, 40, c, box);
            buildSpireFinial(level, box, c, 41, c, 2, 5, abyss,
                    ModBlocks.VEINED_ABYSSAL_STONE.get().defaultBlockState(),
                    ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState());
            buildStairCornice(level, box, c, 6, c, Footprint.square(11), ModBlocks.ABYSSAL_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ABYSSAL_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 7, c, Footprint.square(11), 5, ModBlocks.ABYSSAL_STONE_BRICK_WALL.get().defaultBlockState(), glass);
        }

        private void buildHelicalWalkway(WorldGenLevel level, BoundingBox box, int c,
                                         int radius, int startY, int endY,
                                         BlockState walkway, BlockState marker) {
            int steps = (endY - startY) * 4;
            for (int step = 0; step <= steps; step++) {
                double angle = step * Math.PI / 16.0D;
                int x = c + (int) Math.round(Math.cos(angle) * radius);
                int z = c + (int) Math.round(Math.sin(angle) * radius);
                int y = startY + step / 4;
                architecturalDisk(level, box, x, y, z, 1, walkway);
                if (step % 12 == 0) {
                    this.placeBlock(level, marker, x, y + 1, z, box);
                    architecturalPillar(level, box, x, 1, z, Math.max(1, y - 1),
                            ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState());
                }
            }
        }

        private void buildWayfarerHarbor(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState foundation = Blocks.TUFF_BRICKS.defaultBlockState();
            BlockState floor = Blocks.POLISHED_TUFF.defaultBlockState();
            BlockState trim = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
            BlockState copper = Blocks.WEATHERED_COPPER.defaultBlockState();
            BlockState roof = Blocks.CUT_COPPER.defaultBlockState();

            BlockState bulb = LIT_COPPER_BULB;

            architecturalDisk(level, box, c, 0, c, 31, foundation);
            architecturalRing(level, box, c, 1, c, 30, trim);
            architecturalRing(level, box, c, 1, c, 24, copper);
            buildCircularColonnade(level, box, c, 1, c, 29, 24, 11,
                    foundation, bulb, roof);
            // 必须叠悬挑栈台:万声合奏庭已占用回响族柱环+阶台+封闭厅剪影。
            buildCantileverTerraces(level, box, c, 2, c,
                    Direction.SOUTH, 3, 9, 4, floor, foundation, copper, bulb);
            buildStairCornice(level, box, c, 1, c, Footprint.square(30), Blocks.TUFF_BRICK_STAIRS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(30), 6, Blocks.TUFF_BRICK_WALL.defaultBlockState(), copper);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 30, direction, 4, 11, foundation, copper);
                buildRuneCauseway(level, box, c, 1, c, direction, 11, 30, 3,
                        floor, trim, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
                int rampX = c + direction.getStepX() * 18;
                int rampZ = c + direction.getStepZ() * 18;
                buildBlockRamp(level, box, rampX, 1, rampZ,
                        -direction.getStepX(), -direction.getStepZ(), 11, 3, floor);
            }

            int[][] halls = {{-19, -19}, {19, -19}, {-19, 19}, {19, 19}};
            for (int i = 0; i < halls.length; i++) {
                int x = c + halls[i][0];
                int z = c + halls[i][1];
                buildWayfarerServiceHall(level, box, x, z, i, foundation, trim, copper, roof);
                placeRewardChest(level, random, box, x, 3, z,
                        ModLootTables.FAR_BELL_WAYFARER_CACHE, floor, copper);
            }

            buildSteppedDisk(level, box, c, 1, c, 13, 5, foundation, floor);
            buildCircularColonnade(level, box, c, 6, c, 11, 16, 10,
                    foundation, bulb, copper);
            buildHarborSignalTower(level, box, c, foundation, copper, roof);
            buildRuneCauseway(level, box, c, 5, c, Direction.EAST, 5, 8, 1,
                    floor, copper, trim);
            buildHelicalWalkway(level, box, c, 8, 6, 27, floor, bulb);
            placeRewardChest(level, random, box, c, 28, c + 5,
                    ModLootTables.FAR_BELL_WAYFARER_VAULT, floor, copper);

            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), c - 5, 6, c, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c + 5, 6, c, box);
            this.placeBlock(level, Blocks.SMITHING_TABLE.defaultBlockState(), c, 6, c - 5, box);
            this.placeBlock(level, Blocks.ENDER_CHEST.defaultBlockState(), c - 4, 6, c + 4, box);
            this.placeBlock(level, ModBlocks.COMMUNAL_ECHO_TABLE.get().defaultBlockState(),
                    c + 3, 7, c + 3, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.WAYFARER),
                    c - 3, 7, c + 3, box);
            decorateWayfarerYards(level, random, box, c);

            buildSealedManor(level, box, c + 16, 1, c + 7, 3, 3, 3, 5,
                    Blocks.TUFF_BRICKS.defaultBlockState(),
                    Blocks.CHISELED_TUFF_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_TUFF.defaultBlockState(),
                    Blocks.GLASS.defaultBlockState(),
                    LIT_COPPER_BULB);
            buildWayfarerRestGarden(level, box, c);
        }

        private void buildWayfarerServiceHall(WorldGenLevel level, BoundingBox box,
                                               int x, int z, int phase,
                                               BlockState wall, BlockState trim,
                                               BlockState copper, BlockState roof) {
            architecturalDisk(level, box, x, 1, z, 8, Blocks.POLISHED_TUFF.defaultBlockState());
            buildCircularColonnade(level, box, x, 2, z, 7, 10, 8,
                    wall, LIT_COPPER_BULB, copper);
            architecturalRing(level, box, x, 11, z, 7, roof);
            architecturalRing(level, box, x, 13, z, 5, trim);
            this.placeBlock(level, Blocks.LANTERN.defaultBlockState(), x, 14, z, box);
            BlockState workstation = switch (phase) {
                case 0 -> Blocks.BLAST_FURNACE.defaultBlockState();
                case 1 -> Blocks.SMOKER.defaultBlockState();
                case 2 -> Blocks.LOOM.defaultBlockState();
                default -> Blocks.STONECUTTER.defaultBlockState();
            };
            this.placeBlock(level, workstation, x + 3, 3, z, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), x - 3, 3, z, box);
        }

        private void buildHarborSignalTower(WorldGenLevel level, BoundingBox box, int c,
                                            BlockState wall, BlockState copper, BlockState roof) {
            architecturalDisk(level, box, c, 6, c, 7, Blocks.SMOOTH_STONE.defaultBlockState());
            buildCircularColonnade(level, box, c, 7, c, 6, 12, 20,
                    wall, LIT_COPPER_BULB, copper);
            architecturalRing(level, box, c, 15, c, 6, roof);
            architecturalRing(level, box, c, 21, c, 5, copper);
            architecturalDisk(level, box, c, 27, c, 9, roof);
            architecturalRing(level, box, c, 27, c, 9, Blocks.CHISELED_TUFF_BRICKS.defaultBlockState());
            architecturalPillar(level, box, c, 7, c, 25, Blocks.COPPER_BLOCK.defaultBlockState());
            buildSpireFinial(level, box, c, 28, c, 3, 4, roof, copper, LIT_COPPER_BULB);
            buildStairCornice(level, box, c, 6, c, Footprint.square(7), Blocks.TUFF_BRICK_STAIRS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), true);
            buildParapet(level, box, c, 7, c, Footprint.square(7), 5, Blocks.TUFF_BRICK_WALL.defaultBlockState(), copper);
            this.placeBlock(level, Blocks.LIGHTNING_ROD.defaultBlockState(), c, 33, c, box);
        }

        private void decorateWayfarerYards(WorldGenLevel level, RandomSource random,
                                           BoundingBox box, int c) {
            int[][] yards = {{0, -20}, {20, 0}, {0, 20}, {-20, 0}};
            for (int i = 0; i < yards.length; i++) {
                int x = c + yards[i][0];
                int z = c + yards[i][1];
                architecturalDisk(level, box, x, 1, z, 5, Blocks.MOSS_BLOCK.defaultBlockState());
                this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), x, 2, z, box);
                for (int n = 0; n < 8; n++) {
                    int px = x - 3 + random.nextInt(7);
                    int pz = z - 3 + random.nextInt(7);
                    this.placeBlock(level, n % 2 == 0
                            ? Blocks.CORNFLOWER.defaultBlockState()
                            : Blocks.AZURE_BLUET.defaultBlockState(), px, 2, pz, box);
                }
            }
        }

        private void buildWayfarerRestGarden(WorldGenLevel level, BoundingBox box, int c) {
            int x = c + 12;
            int z = c - 12;
            architecturalDisk(level, box, x, 1, z, 6, Blocks.POLISHED_TUFF.defaultBlockState());
            architecturalRing(level, box, x, 2, z, 5, Blocks.CUT_COPPER.defaultBlockState());
            architecturalDisk(level, box, x, 2, z, 3, Blocks.MOSS_BLOCK.defaultBlockState());
            int[][] canopyPosts = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
            for (int[] post : canopyPosts) {
                architecturalPillar(level, box, x + post[0], 2, z + post[1], 5,
                        Blocks.WEATHERED_COPPER.defaultBlockState());
                this.placeBlock(level, Blocks.LANTERN.defaultBlockState(),
                        x + post[0], 7, z + post[1], box);
            }
            this.placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState(), x - 3, 3, z, box);
            this.placeBlock(level, Blocks.HAY_BLOCK.defaultBlockState(), x + 3, 3, z - 1, box);
            this.placeBlock(level, Blocks.HAY_BLOCK.defaultBlockState(), x + 3, 3, z + 1, box);
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.WAYFARER),
                    x, 3, z, box);
        }

        private void buildPrismaticConservatory(WorldGenLevel level, RandomSource random,
                                                 BoundingBox box) {
            int c = kind.size / 2;
            BlockState echo = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState glass = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState moss = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();

            architecturalDisk(level, box, c, 0, c, 35, moss);
            architecturalRing(level, box, c, 1, c, 34, rune);
            architecturalRing(level, box, c, 1, c, 27, echo);
            buildCircularColonnade(level, box, c, 1, c, 32, 24, 14,
                    echo, glass, rune);
            // 必须叠围合中庭:远鸣测绘院已占用回响石柱环+阶台剪影。
            buildCourtyardCloister(level, box, c, 1, c, Footprint.square(24), 11, 4, moss, echo, rune, glass);
            buildStairCornice(level, box, c, 1, c, Footprint.square(33), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(34), 6, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 34, direction, 4, 13, echo, rune);
                buildRuneCauseway(level, box, c, 1, c, direction, 10, 34, 3,
                        echo, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(), rune);
            }

            BlockState[] palettes = {
                    ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.SONGSTONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState()
            };
            int[][] galleries = {{-23, -23}, {23, -23}, {-23, 23}, {23, 23}};
            for (int i = 0; i < galleries.length; i++) {
                int x = c + galleries[i][0];
                int z = c + galleries[i][1];
                buildPrismaticGallery(level, random, box, x, z, i, palettes[i], rune, glass);
                placeRewardChest(level, random, box, x, 3, z,
                        ModLootTables.PRISMATIC_CANOPY_CACHE, palettes[i], rune);
            }

            buildSteppedDisk(level, box, c, 1, c, 14, 6, echo, moss);
            buildCanopyArboretum(level, box, c, echo, rune, glass);
            buildHelicalWalkway(level, box, c, 10, 7, 29, echo, glass);
            architecturalDisk(level, box, c, 29, c, 11, echo);
            architecturalRing(level, box, c, 29, c, 11, rune);
            placeRewardChest(level, random, box, c, 30, c + 7,
                    ModLootTables.PRISMATIC_CANOPY_VAULT, echo, glass);

            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c - 5, 7, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(), c + 5, 7, c, box);
            this.placeBlock(level, Blocks.COMPOSTER.defaultBlockState(), c, 7, c - 5, box);
            this.placeBlock(level, Blocks.SMOKER.defaultBlockState(), c + 5, 7, c - 5, box);
            this.placeBlock(level, ModBlocks.COMMUNAL_ECHO_TABLE.get().defaultBlockState(),
                    c + 3, 7, c + 3, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.CONFLUENCE),
                    c - 3, 7, c + 3, box);
            buildCanopyPollinatorCrown(level, box, c);
        }

        private void buildPrismaticGallery(WorldGenLevel level, RandomSource random, BoundingBox box,
                                           int x, int z, int phase, BlockState material,
                                           BlockState rune, BlockState glass) {
            architecturalDisk(level, box, x, 1, z, 9, material);
            architecturalDisk(level, box, x, 2, z, 5, Blocks.MOSS_BLOCK.defaultBlockState());
            buildCircularColonnade(level, box, x, 2, z, 8, 12, 10,
                    material, glass, rune);
            for (int y = 12; y <= 20; y += 2) {
                architecturalRing(level, box, x, y, z, Math.max(3, 8 - (y - 12) / 2),
                        y % 4 == 0 ? glass : material);
            }
            for (int n = 0; n < 16; n++) {
                int px = x - 3 + random.nextInt(7);
                int pz = z - 3 + random.nextInt(7);
                BlockState plant = switch ((n + phase) % 4) {
                    case 0 -> ModBlocks.ECHO_FLOWER.get().defaultBlockState();
                    case 1 -> ModBlocks.DREAM_FLOWER.get().defaultBlockState();
                    case 2 -> ModBlocks.STAR_MIST_BLOOM.get().defaultBlockState();
                    default -> ModBlocks.GLOW_FERN.get().defaultBlockState();
                };
                this.placeBlock(level, plant, px, 3, pz, box);
            }
            this.placeBlock(level, phase % 2 == 0
                    ? Blocks.BREWING_STAND.defaultBlockState()
                    : Blocks.CAULDRON.defaultBlockState(), x + 4, 3, z, box);
        }

        private void buildCanopyPollinatorCrown(WorldGenLevel level, BoundingBox box, int c) {
            int x = c - 5;
            int z = c - 5;
            architecturalDisk(level, box, x, 29, z, 4,
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            int[][] perches = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
            for (int[] perch : perches) {
                architecturalPillar(level, box, x + perch[0], 30, z + perch[1], 4,
                        ModBlocks.ECHO_LOG.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                        x + perch[0], 34, z + perch[1], box);
            }

            BlockState plantSoil = Blocks.MOSS_BLOCK.defaultBlockState();
            this.placeBlock(level, plantSoil, x - 2, 29, z, box);
            this.placeBlock(level, plantSoil, x + 2, 29, z, box);
            this.placeBlock(level, plantSoil, x, 29, z - 2, box);
            this.placeBlock(level, plantSoil, x, 29, z + 2, box);
            this.placeBlock(level, ModBlocks.ECHO_FLOWER.get().defaultBlockState(), x - 2, 30, z, box);
            this.placeBlock(level, ModBlocks.DREAM_FLOWER.get().defaultBlockState(), x + 2, 30, z, box);
            this.placeBlock(level, ModBlocks.STAR_MIST_BLOOM.get().defaultBlockState(), x, 30, z - 2, box);
            this.placeBlock(level, ModBlocks.GLOW_FERN.get().defaultBlockState(), x, 30, z + 2, box);
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.CONFLUENCE),
                    x, 30, z, box);
        }

        private void buildCanopyArboretum(WorldGenLevel level, BoundingBox box, int c,
                                          BlockState echo, BlockState rune, BlockState glass) {
            buildCircularColonnade(level, box, c, 7, c, 13, 16, 24, echo, glass, rune);
            for (int y = 13; y <= 35; y += 4) {
                int radius = Math.max(4, 15 - (y - 13) / 2);
                architecturalRing(level, box, c, y, c, radius, y % 8 == 1 ? rune : glass);
            }
            architecturalPillar(level, box, c, 7, c, 29,
                    ModBlocks.ECHO_LOG.get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            BlockState persistentEchoLeaves = ModBlocks.ECHO_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, Boolean.TRUE);
            BlockState persistentWhisperingLeaves = ModBlocks.WHISPERING_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, Boolean.TRUE);
            architecturalDisk(level, box, c, 24, c, 8, persistentEchoLeaves);
            architecturalDisk(level, box, c, 28, c, 6, persistentWhisperingLeaves);
            architecturalDisk(level, box, c, 32, c, 4, persistentEchoLeaves);
            this.placeBlock(level, glass, c, 37, c, box);
            buildSpireFinial(level, box, c, 38, c, 2, 4, echo, rune, glass);
            buildStairCornice(level, box, c, 7, c, Footprint.square(13), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 8, c, Footprint.square(13), 5, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
        }

        private void buildExpeditionRing(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState folded = ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState();
            BlockState resonant = ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_FOLDED_SLATE.get().defaultBlockState();
            BlockState gravity = ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState();
            BlockState crystal = ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState();

            architecturalDisk(level, box, c, 0, c, 39, ModBlocks.FOLDED_SLATE.get().defaultBlockState());
            architecturalRing(level, box, c, 1, c, 38, resonant);
            architecturalRing(level, box, c, 1, c, 30, gravity);
            buildCircularColonnade(level, box, c, 1, c, 36, 32, 16,
                    folded, crystal, chiseled);
            buildStairCornice(level, box, c, 1, c, Footprint.square(37), ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.FOLDED_SLATE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(38), 6, ModBlocks.FOLDED_SLATE_BRICK_WALL.get().defaultBlockState(), chiseled);
            buildRadialButtresses(level, box, c, 1, c, 29, 38, 14,
                    gravity, ModBlocks.MUTE_CRYSTAL.get().defaultBlockState());
            // 必须叠退台营垒:残卷生态穹顶已占用折叠板岩柱环+扶壁+阶台剪影。
            buildSteppedZiggurat(level, box, c, 1, c, 26, 3, 4, 0,
                    folded, resonant,
                    ModBlocks.FOLDED_SLATE_BRICK_STAIRS.get().defaultBlockState(), crystal);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 38, direction, 5, 15, folded, chiseled);
                buildRuneCauseway(level, box, c, 1, c, direction, 12, 38, 4,
                        folded, gravity, resonant);
            }

            int[][] bastions = {{-25, -25}, {25, -25}, {-25, 25}, {25, 25}};
            for (int i = 0; i < bastions.length; i++) {
                int x = c + bastions[i][0];
                int z = c + bastions[i][1];
                buildExpeditionBastion(level, box, x, z, i, folded, chiseled, gravity, crystal);
                placeRewardChest(level, random, box, x, 3, z + 5,
                        ModLootTables.INTERLAYER_EXPEDITION_CACHE, folded, chiseled);
                this.placeBlock(level, ModBlocks.DEEP_PROVISION_BRAZIER.get().defaultBlockState(),
                        x + (i % 2 == 0 ? 5 : -5), 3, z, box);
            }

            buildSteppedDisk(level, box, c, 1, c, 16, 6, folded, gravity);
            buildExpeditionSpire(level, box, c, folded, chiseled, gravity, crystal);
            buildHelicalWalkway(level, box, c, 11, 7, 36, gravity, crystal);
            architecturalDisk(level, box, c, 36, c, 12, folded);
            architecturalRing(level, box, c, 36, c, 12, resonant);
            placeRewardChest(level, random, box, c + 8, 37, c,
                    ModLootTables.INTERLAYER_EXPEDITION_VAULT, folded, crystal);

            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c - 6, 7, c, box);
            this.placeBlock(level, Blocks.ENDER_CHEST.defaultBlockState(), c + 6, 7, c, box);
            this.placeBlock(level, Blocks.ANVIL.defaultBlockState(), c, 7, c - 6, box);
            this.placeBlock(level, Blocks.GRINDSTONE.defaultBlockState(), c + 5, 7, c + 5, box);
            this.placeBlock(level, ModBlocks.COMMUNAL_ECHO_TABLE.get().defaultBlockState(),
                    c + 4, 8, c + 4, box);
            this.placeBlock(level, ModBlocks.REALM_SERVICE_PLINTH.get().defaultBlockState()
                    .setValue(RealmServicePlinthBlock.MODE, RealmServicePlinthBlock.Mode.INTERLAYER),
                    c - 4, 8, c + 4, box);
            buildInterlayerShelterHabitat(level, box, c);

            buildSealedManor(level, box, c + 23, 1, c + 10, 5, 5, 3, 5,
                    ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState(),
                    ModBlocks.BANDED_GRAVITY_SHALE.get().defaultBlockState(),
                    ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState(),
                    Blocks.TINTED_GLASS.defaultBlockState(),
                    ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
        }

        private void buildExpeditionBastion(WorldGenLevel level, BoundingBox box,
                                             int x, int z, int phase,
                                             BlockState folded, BlockState chiseled,
                                             BlockState gravity, BlockState crystal) {
            architecturalDisk(level, box, x, 1, z, 10, folded);
            architecturalRing(level, box, x, 2, z, 9, gravity);
            buildCircularColonnade(level, box, x, 2, z, 8, 12, 11,
                    folded, crystal, chiseled);
            architecturalRing(level, box, x, 14, z, 8, gravity);
            architecturalRing(level, box, x, 17, z, 6, chiseled);
            architecturalPillar(level, box, x, 2, z, 17, phase % 2 == 0 ? gravity : folded);
            this.placeBlock(level, crystal, x, 20, z, box);
            BlockState station = switch (phase) {
                case 0 -> Blocks.BLAST_FURNACE.defaultBlockState();
                case 1 -> Blocks.SMOKER.defaultBlockState();
                case 2 -> Blocks.STONECUTTER.defaultBlockState();
                default -> Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
            };
            this.placeBlock(level, station, x - 4, 3, z, box);
        }

        private void buildInterlayerShelterHabitat(WorldGenLevel level, BoundingBox box, int c) {
            int x = c - 5;
            int z = c - 5;
            architecturalDisk(level, box, x, 7, z, 4,
                    ModBlocks.FOLDED_SLATE_BRICKS.get().defaultBlockState());
            int[][] pylons = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
            for (int[] pylon : pylons) {
                architecturalPillar(level, box, x + pylon[0], 8, z + pylon[1], 4,
                        ModBlocks.GRAVITY_SHALE_BRICKS.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.SUSPENDED_CRYSTAL.get().defaultBlockState(),
                        x + pylon[0], 12, z + pylon[1], box);
            }
            this.placeBlock(level, ModBlocks.DEEP_ROUTE_NODE.get().defaultBlockState(),
                    x - 3, 8, z, box);
            this.placeBlock(level, ModBlocks.SPACE_ANCHOR.get().defaultBlockState(),
                    x + 3, 8, z, box);
            this.placeBlock(level, ModBlocks.REALM_HABITAT_CHIME.get().defaultBlockState()
                    .setValue(RealmHabitatChimeBlock.MODE, RealmHabitatChimeBlock.Mode.INTERLAYER),
                    x, 8, z, box);
        }

        private void buildExpeditionSpire(WorldGenLevel level, BoundingBox box, int c,
                                           BlockState folded, BlockState chiseled,
                                           BlockState gravity, BlockState crystal) {
            architecturalDisk(level, box, c, 7, c, 13, gravity);
            buildCircularColonnade(level, box, c, 7, c, 12, 16, 30,
                    folded, crystal, chiseled);
            for (int y = 14; y <= 41; y += 5) {
                int radius = Math.max(4, 13 - (y - 14) / 3);
                architecturalRing(level, box, c, y, c, radius, y % 10 == 4 ? crystal : gravity);
            }
            architecturalPillar(level, box, c, 7, c, 36,
                    ModBlocks.RESONANT_FOLDED_SLATE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.MUTE_CRYSTAL.get().defaultBlockState(), c, 44, c, box);
            buildSpireFinial(level, box, c, 45, c, 2, 5, folded, chiseled, crystal);
            buildStairCornice(level, box, c, 7, c, Footprint.square(12), ModBlocks.GRAVITY_SHALE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.GRAVITY_SHALE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 8, c, Footprint.square(12), 5, ModBlocks.GRAVITY_SHALE_BRICK_WALL.get().defaultBlockState(), chiseled);
        }

        private void buildBlockRamp(WorldGenLevel level, BoundingBox box,
                                    int startX, int startY, int startZ,
                                    int stepX, int stepZ, int length, int width,
                                    BlockState state) {
            int sideX = stepZ;
            int sideZ = -stepX;
            for (int step = 0; step < length; step++) {
                int y = startY + step / 3;
                for (int lateral = -width / 2; lateral <= width / 2; lateral++) {
                    this.placeBlock(level, state,
                            startX + stepX * step + sideX * lateral, y,
                            startZ + stepZ * step + sideZ * lateral, box);
                }
            }
        }

        private void placeRewardChest(WorldGenLevel level, RandomSource random, BoundingBox box,
                                      int x, int y, int z,
                                      net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot,
                                      BlockState floor, BlockState accent) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    this.placeBlock(level, Math.abs(dx) == 2 || Math.abs(dz) == 2 ? accent : floor,
                            x + dx, y - 1, z + dz, box);
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x + dx, y + dy, z + dz, box);
                    }
                }
            }
            this.createChest(level, box, random, x, y, z, loot); // audit-ok: themed helper clears a 3x3x3 pocket
        }

        private void clearPlayableVolume(WorldGenLevel level, BoundingBox box) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int x = 0; x < kind.size; x++) {
                for (int z = 0; z < kind.size; z++) {
                    int worldX = this.getWorldX(x, z);
                    int worldZ = this.getWorldZ(x, z);
                    if (worldX < box.minX() || worldX > box.maxX()
                            || worldZ < box.minZ() || worldZ > box.maxZ()) {
                        continue;
                    }
                    for (int y = 1; y < kind.height; y++) {
                        this.placeBlock(level, air, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("ThreeRealmLandmarkKind", kind.id);
        }
    }
}
