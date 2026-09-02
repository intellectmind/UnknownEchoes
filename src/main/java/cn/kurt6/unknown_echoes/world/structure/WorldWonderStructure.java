package cn.kurt6.unknown_echoes.world.structure;

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
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 第一维度世界奇观:用代码结构提供可 /locate、可自然生成的远景地标。 */
public class WorldWonderStructure extends SinglePieceStructure {
    public static final MapCodec<WorldWonderStructure> ECHO_WORLD_TREE_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.ECHO_WORLD_TREE));
    public static final MapCodec<WorldWonderStructure> ETERNAL_ECHO_LIGHTHOUSE_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.ETERNAL_ECHO_LIGHTHOUSE));
    public static final MapCodec<WorldWonderStructure> MIRROR_SEA_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.MIRROR_SEA));
    public static final MapCodec<WorldWonderStructure> INVERTED_MOUNTAINS_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.INVERTED_MOUNTAINS));
    public static final MapCodec<WorldWonderStructure> SKY_RIFT_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.SKY_RIFT));
    public static final MapCodec<WorldWonderStructure> SILENT_GREAT_BOAT_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.SILENT_GREAT_BOAT));
    public static final MapCodec<WorldWonderStructure> BROKEN_BELL_TOWER_CODEC =
            simpleCodec(settings -> new WorldWonderStructure(settings, Kind.BROKEN_BELL_TOWER));

    private final Kind kind;

    private WorldWonderStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case ECHO_WORLD_TREE -> ModStructures.ECHO_WORLD_TREE.get();
            case ETERNAL_ECHO_LIGHTHOUSE -> ModStructures.ETERNAL_ECHO_LIGHTHOUSE.get();
            case MIRROR_SEA -> ModStructures.MIRROR_SEA.get();
            case INVERTED_MOUNTAINS -> ModStructures.INVERTED_MOUNTAINS.get();
            case SKY_RIFT -> ModStructures.SKY_RIFT.get();
            case SILENT_GREAT_BOAT -> ModStructures.SILENT_GREAT_BOAT.get();
            case BROKEN_BELL_TOWER -> ModStructures.BROKEN_BELL_TOWER.get();
        };
    }

    public enum Kind {
        // clearRadius 必须 >= 该奇观在 clearHeight 内写入的最大 |x-c| / |z-c|,否则残留原地形;
        // 0 = 悬空奇观,周围本来就是空气。判据见 tools/audit_wonder_clearance.py。
        ECHO_WORLD_TREE("echo_world_tree", 69, 100, 64, 34, 99),
        ETERNAL_ECHO_LIGHTHOUSE("eternal_echo_lighthouse", 53, 88, 64, 22, 87),
        MIRROR_SEA("mirror_sea", 75, 22, 62, 37, 21),
        INVERTED_MOUNTAINS("inverted_mountains", 73, 104, 150, 0, 0),
        SKY_RIFT("sky_rift", 61, 108, 185, 0, 0),
        SILENT_GREAT_BOAT("silent_great_boat", 85, 38, 63, 37, 37),
        BROKEN_BELL_TOWER("broken_bell_tower", 57, 108, 64, 28, 107);

        private final String id;
        private final int size;
        private final int height;
        private final int startY;
        private final int clearRadius;
        private final int clearHeight;

        Kind(String id, int size, int height, int startY, int clearRadius, int clearHeight) {
            this.id = id;
            this.size = size;
            this.height = height;
            this.startY = startY;
            this.clearRadius = clearRadius;
            this.clearHeight = clearHeight;
        }

        static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return ECHO_WORLD_TREE;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] LIGHTHOUSE_MIRROR_NODES = {
                {-8, 0}, {8, 0}, {-6, -6}, {6, -6},
                {-6, 6}, {6, 6}, {-7, -3}, {7, 3}
        };
        private static final int[][] MIRROR_SEA_MIRROR_NODES = {
                {-24, 0}, {24, 0}, {0, -24}, {0, 24},
                {-17, -17}, {17, -17}, {-17, 17}, {17, 17},
                {-12, -5}, {12, -5}, {-12, 5}, {12, 5},
                {-5, -12}, {5, -12}, {-5, 12}, {5, 12}
        };
        private static final int[][] BELL_TOWER_GOLD_NODES = {
                {0, 0}, {-3, 0}, {3, 0}, {0, -3}, {0, 3}
        };
        private Kind kind;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.WORLD_WONDER_PIECE.get(), x, kind.startY, z,
                    kind.size, kind.height, kind.size, Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.WORLD_WONDER_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("WonderKind"));
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (this.kind.startY < 120 && !this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearWonderVolume(level, box);
            switch (this.kind) {
                case ECHO_WORLD_TREE -> buildWorldTree(level, random, box);
                case ETERNAL_ECHO_LIGHTHOUSE -> buildLighthouse(level, random, box);
                case MIRROR_SEA -> buildMirrorSea(level, random, box);
                case INVERTED_MOUNTAINS -> buildInvertedMountains(level, random, box);
                case SKY_RIFT -> buildSkyRift(level, random, box);
                case SILENT_GREAT_BOAT -> buildSilentBoat(level, random, box);
                case BROKEN_BELL_TOWER -> buildBellTower(level, random, box);
            }
            buildWonderFrame(level, random, box);
            addWonderGameplay(level, random, box);
        }

        /**
         * 每座奇观的可玩层:投影接力廊守住一份奖励,主题刷怪笼提供普通遭遇, 部分奇观叠加顺序封印室;
         * 机关全部可重复、服务端判定,不承载唯一关键进度。
         */
        private void addWonderGameplay(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case ECHO_WORLD_TREE -> {
                    BlockState floor = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
                    BlockState wall = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 0, c + 19, Direction.SOUTH,
                            floor, wall, rune, ModLootTables.T4_REGION_CORE);
                    buildSequenceSeal(level, random, box, c - 24, 0, c - 8, Direction.EAST,
                            floor, wall, rune, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c + 18, 1, c - 14,
                            ModEntities.ECHO_WANDERER.get(), floor, rune);
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState tide = ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 1, c, Direction.SOUTH,
                            mirror, reef, tide, ModLootTables.T4_REGION_CORE);
                    this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                            .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                            c + 2, 2, c, box);
                    placeFloodedStructureSpawner(level, random, box, c - 11, 1, c + 9,
                            ModEntities.WATER_SHADOW.get(), reef, mirror);
                    for (int[] node : LIGHTHOUSE_MIRROR_NODES) {
                        this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                                c + node[0], 1, c + node[1], box);
                    }
                }
                case MIRROR_SEA -> {
                    BlockState floor = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState wall = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 1, c - 27, Direction.EAST,
                            floor, wall, trim, ModLootTables.MIRROR_LAKE_CACHE);
                    placeStructureSpawner(level, random, box, c + 20, 2, c + 10,
                            ModEntities.MIRROR_DUST_DECOY.get(), trim, wall);
                    for (int[] node : MIRROR_SEA_MIRROR_NODES) {
                        this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                                c + node[0], 0, c + node[1], box);
                    }
                }
                case INVERTED_MOUNTAINS -> {
                    // 铺地/砌墙/腰线必须换三种不同岩,只用粗岩会读成一条素坑道。
                    BlockState floor = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                    BlockState wall = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
                    BlockState trim = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 0, c, Direction.EAST,
                            floor, wall, trim, ModLootTables.T4_REGION_CORE);
                    buildStairCornice(level, box, c, 7, c, Footprint.square(8), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
                    placeStructureSpawner(level, random, box, c + 16, 1, c - 18,
                            ModEntities.WIND_ERODED_SENTINEL.get(), floor, trim);
                }
                case SKY_RIFT -> {
                    BlockState floor = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState wall = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState glow = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 4, c + 9, Direction.EAST,
                            floor, wall, glow, ModLootTables.T5_MINIBOSS_REPEAT);
                    placeStructureSpawner(level, random, box, c - 16, 4, c - 12,
                            ModEntities.CRYSTAL_NOISE_WISP.get(), floor, glow);
                    buildAccessibleChest(level, random, box, c, 104, c, ModLootTables.WIND_VAULT_CHEST);
                }
                case SILENT_GREAT_BOAT -> {
                    BlockState planks = ModBlocks.WHISPERING_PLANKS.get().defaultBlockState();
                    BlockState log = ModBlocks.WHISPERING_LOG.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 1, c + 7, Direction.EAST,
                            planks, planks, log, ModLootTables.SILENT_HUT_CHEST);
                    buildSequenceSeal(level, random, box, c + 24, 1, c - 4, Direction.EAST,
                            planks, planks, log, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c - 14, 2, c - 8,
                            ModEntities.SILENT_WALKER.get(), planks, log);
                }
                case BROKEN_BELL_TOWER -> {
                    BlockState ashen = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    BlockState strata = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 0, c + 13, Direction.SOUTH,
                            ashen, ashen, rusted, ModLootTables.BROKEN_ARCHIVE_CHEST);
                    buildSequenceSeal(level, random, box, c + 16, 0, c + 6, Direction.EAST,
                            ashen, rusted, strata, ModLootTables.T3_PUZZLE);

                    buildChimeEchoCourt(level, random, box, c + 14, 0, c - 10, Direction.EAST,
                            ashen, ashen, rusted, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c - 14, 1, c - 14,
                            ModEntities.BROKEN_BELL_GUARD.get(), ashen, rusted);
                    for (int[] node : BELL_TOWER_GOLD_NODES) {
                        this.placeBlock(level, Blocks.GOLD_BLOCK.defaultBlockState(),
                                c + node[0], 83, c + node[1], box);
                    }
                }
            }
        }

        /**
         * 必须在建造之前调用:奇观 terrain_adaptation 是 none,不掏空会把原地形留在构件之间。
         * 腔体必须是方形,圆形会把长条奇观的窄轴两侧多掏 20 格山体;列先判 box 再进 y 循环,
         * 否则 postProcess 每个相交区块都重扫整个腔体。
         */
        private void clearWonderVolume(WorldGenLevel level, BoundingBox box) {
            if (kind.clearRadius <= 0) {
                return;
            }
            BlockState air = Blocks.AIR.defaultBlockState();
            int c = kind.size / 2;
            int top = Math.min(kind.height - 1, kind.clearHeight);
            for (int x = c - kind.clearRadius; x <= c + kind.clearRadius; x++) {
                for (int z = c - kind.clearRadius; z <= c + kind.clearRadius; z++) {
                    int worldX = this.getWorldX(x, z);
                    int worldZ = this.getWorldZ(x, z);
                    if (worldX < box.minX() || worldX > box.maxX()
                            || worldZ < box.minZ() || worldZ > box.maxZ()) {
                        continue;
                    }
                    for (int y = 1; y <= top; y++) {
                        if (this.getBlock(level, x, y, z, box).isAir()) {
                            continue;
                        }
                        this.placeBlock(level, air, x, y, z, box);
                    }
                }
            }
        }

        private void buildWonderFrame(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case ECHO_WORLD_TREE -> {
                    // 柱基/柱身/柱头必须分三种材质,一根到顶的同色柱看不出基-身-头。
                    BlockState mossy = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
                    BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                    BlockState shaft = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
                    BlockState timber = ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState();
                    for (int i = 0; i < 12; i++) {
                        double angle = i * Math.PI / 6.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 27);
                        int z = c + (int) Math.round(Math.sin(angle) * 27);
                        int height = 7 + i % 4;
                        architecturalPillar(level, box, x, 1, z, 2, mossy);
                        architecturalPillar(level, box, x, 3, z, height - 2, shaft);
                        this.placeBlock(level, rune, x, height + 1, z, box);
                    }

                    buildRuneCauseway(level, box, c, 1, c, Direction.NORTH,
                            18, 29, 3, Blocks.MOSS_BLOCK.defaultBlockState(), timber, rune);
                    buildCornice(level, box, c, 1, c, Footprint.square(28), shaft, mossy);
                    buildParapet(level, box, c, 2, c, Footprint.square(29), 7, ModBlocks.WHISPERING_TIMBER_WALL.get().defaultBlockState(), mossy);
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    architecturalRing(level, box, c, 1, c, 18, ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState());
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        int x = c + direction.getStepX() * 17;
                        int z = c + direction.getStepZ() * 17;
                        architecturalPillar(level, box, x, 1, z, 18, reef);
                        architecturalRing(level, box, x, 19, z, 3, mirror);
                        this.placeBlock(level, Blocks.SEA_LANTERN.defaultBlockState(), x, 20, z, box);
                    }
                    buildRuneCauseway(level, box, c, 1, c, Direction.SOUTH,
                            12, 19, 2, mirror, reef,
                            ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
                    // 两座附属屋必须一坡一穹:同款亭殿会让灯塔左右两侧撞形。
                    BlockState reefBrick = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState reefLamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    buildGabledPavilion(level, box, c - 14, 1, c + 2, Footprint.of(6, 7), 6, 4, mirror, reef, reefBrick, Blocks.SEA_LANTERN.defaultBlockState());
                    buildDomedRotunda(level, box, c + 14, 1, c + 2,
                            6, 5, mirror, reefBrick, reef, reefLamp);
                    buildStairCornice(level, box, c - 14, 7, c + 2, Footprint.of(6, 7), ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c - 14, 2, c + 2, Footprint.of(7, 8), 4, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), reef);
                    buildParapet(level, box, c + 14, 2, c + 2, Footprint.square(6), 4, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), reef);
                }
                case MIRROR_SEA -> {
                    BlockState mirror = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
                    BlockState prism = ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState();
                    for (int i = 0; i < 16; i++) {
                        double angle = i * Math.PI / 8.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 29);
                        int z = c + (int) Math.round(Math.sin(angle) * 29);
                        int height = 5 + (i * 3) % 9;
                        architecturalPillar(level, box, x, 2, z, height, mirror);
                        this.placeBlock(level, prism, x, height + 2, z, box);
                    }
                    architecturalRing(level, box, c, 12, c, 28,
                            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
                    // 两岸镜廊必须一坡一穹,同款亭殿会让两岸撞形。
                    BlockState mirrorStone = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState fractured = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
                    BlockState reefLamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    buildGabledPavilion(level, box, c - 23, 2, c, Footprint.of(5, 7), 5, 3, mirror, mirrorStone, fractured, Blocks.SEA_LANTERN.defaultBlockState());
                    buildDomedRotunda(level, box, c + 23, 2, c,
                            5, 5, mirror, mirrorStone, fractured, reefLamp);
                    buildStairCornice(level, box, c - 23, 8, c, Footprint.of(5, 7), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c - 23, 2, c, Footprint.of(6, 8), 4, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), fractured);
                    buildCornice(level, box, c + 23, 8, c, Footprint.square(5), mirrorStone, fractured);
                    buildSpireFinial(level, box, c + 23, 9, c, 2, 4,
                            mirrorStone, fractured, reefLamp);
                }
                case INVERTED_MOUNTAINS -> {
                    // 峰体/峰腰/峰基必须分三种岩,单一石材远景是一团灰。
                    BlockState wind = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState band = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState base = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
                    BlockState crystal = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        int x = c + direction.getStepX() * 25;
                        int z = c + direction.getStepZ() * 25;
                        for (int y = 68; y >= 38; y--) {
                            int radius = Math.max(1, (68 - y) / 7);
                            BlockState body = y >= 62 ? base : (y % 6 == 0 ? band : wind);
                            architecturalDisk(level, box, x, y, z, radius, body);
                        }
                        this.placeBlock(level, crystal, x, 37, z, box);
                        buildStairCornice(level, box, x, 68, z, Footprint.square(4), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), false);
                        buildCornice(level, box, x, 67, z, Footprint.square(4), band, base);
                    }
                    architecturalRing(level, box, c, 78, c, 26, wind);
                    architecturalRing(level, box, c, 77, c, 26, band);
                    buildParapet(level, box, c, 79, c, Footprint.square(26), 7, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), band);
                }
                case SKY_RIFT -> {
                    // 浮台底面与台面必须分两种材质,单一石材会读成一张光板圆盘。
                    BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState windBrick = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                    BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState glow = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
                    BlockState windStairs = ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState();
                    BlockState windSlab = ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState();
                    for (int i = 0; i < 12; i++) {
                        double angle = i * Math.PI / 6.0D;
                        int x = c + (int) Math.round(Math.cos(angle) * 18);
                        int z = c + (int) Math.round(Math.sin(angle) * 18);
                        int y = 20 + (i % 4) * 16;
                        int radius = 2 + i % 3;
                        // 底面与台面各一层:单层浮台在侧视里没有厚度。
                        architecturalDisk(level, box, x, y - 1, z, radius, wind);
                        architecturalDisk(level, box, x, y, z, radius, windBrick);
                        architecturalRing(level, box, x, y, z, radius, chiseled);
                        buildStairCornice(level, box, x, y, z, Footprint.square(radius), windStairs, windSlab, true);
                        this.placeBlock(level, glow, x, y + 2 + i % 3, z, box);
                    }
                    for (int radius : new int[]{8, 14, 20}) {
                        architecturalRing(level, box, c, 12 + radius * 2, c, radius,
                                radius == 14 ? glow : Blocks.AMETHYST_BLOCK.defaultBlockState());
                        buildParapet(level, box, c, 13 + radius * 2, c, Footprint.square(radius), 6, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), chiseled);
                        buildCornice(level, box, c, 11 + radius * 2, c, Footprint.square(radius), windBrick, chiseled);
                    }
                }
                case SILENT_GREAT_BOAT -> {
                    buildGreatBoatRigging(level, box, c);
                    BlockState planks = ModBlocks.WHISPERING_PLANKS.get().defaultBlockState();
                    BlockState log = ModBlocks.WHISPERING_LOG.get().defaultBlockState();
                    BlockState lattice = ModBlocks.WHISPERING_LATTICE.get().defaultBlockState();
                    BlockState timber = ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState();
                    BlockState bog = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();
                    // 艏楼双坡、艉舱围合:两端用不同体量才能从远处分辨船首船尾。
                    buildGabledPavilion(level, box, c - 20, 2, c, Footprint.of(7, 8), 6, 4, planks, log, lattice, bog);
                    buildCourtyardCloister(level, box, c + 20, 2, c, Footprint.of(7, 8), 6, 2, planks, timber, log, bog);
                    buildStairCornice(level, box, c - 20, 8, c, Footprint.of(7, 8), ModBlocks.WHISPERING_TIMBER_STAIRS.get().defaultBlockState(), ModBlocks.WHISPERING_TIMBER_SLAB.get().defaultBlockState(), true);
                    buildParapet(level, box, c + 20, 8, c, Footprint.of(7, 8), 4, ModBlocks.WHISPERING_TIMBER_WALL.get().defaultBlockState(), lattice);
                    buildCornice(level, box, c - 20, 5, c, Footprint.of(7, 8), planks, log);
                    buildCornice(level, box, c + 20, 5, c, Footprint.of(7, 8), planks, log);
                }
                case BROKEN_BELL_TOWER -> {
                    BlockState ashen = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    for (Direction direction : Direction.Plane.HORIZONTAL) {

                        if (direction == Direction.SOUTH) {
                            continue;
                        }
                        int dx = direction.getStepX();
                        int dz = direction.getStepZ();
                        for (int distance = 10; distance <= 18; distance++) {
                            int height = Math.max(6, 30 - distance);
                            architecturalPillar(level, box, c + dx * distance, 1,
                                    c + dz * distance, height, distance % 3 == 0 ? rusted : ashen);
                        }
                        int x = c + dx * 18;
                        int z = c + dz * 18;

                        this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                                x, 13, z, box);
                    }
                    architecturalRing(level, box, c, 26, c, 16, rusted);
                    BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c + 13, Footprint.of(17, 8), 8, 5, rusted, ashen, rusted, candle);
                    buildStairCornice(level, box, c, 9, c + 13, Footprint.of(17, 8), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 10, c + 13, Footprint.of(17, 8), ashen, ModBlocks.CLOCKWORK_SCRAP_BLOCK.get().defaultBlockState());
                }
            }
            scatterArchitecturalDebris(level, random, box, c, 1, c,
                    Math.max(8, c - 8), Math.max(9, c - 3), Math.max(18, c),
                    ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
        }

        private void buildGreatBoatRigging(WorldGenLevel level, BoundingBox box, int c) {
            BlockState mast = ModBlocks.WHISPERING_LOG.get().defaultBlockState();
            BlockState planks = ModBlocks.WHISPERING_PLANKS.get().defaultBlockState();
            for (int mastX : new int[]{c - 18, c, c + 18}) {
                architecturalPillar(level, box, mastX, 2, c, 30, mast);
                for (int y = 9; y <= 22; y++) {
                    int halfWidth = Math.max(2, 10 - Math.abs(15 - y));
                    for (int z = c - halfWidth; z <= c + halfWidth; z++) {
                        if ((y + z + mastX) % 5 != 0) {
                            this.placeBlock(level,
                                    y % 3 == 0 ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                                            : Blocks.WHITE_WOOL.defaultBlockState(),
                                    mastX, y, z, box);
                        }
                    }
                }

                architecturalRing(level, box, mastX, 28, c, 2, planks);
                this.placeBlock(level, ModBlocks.BOGLIGHT_STONE.get().defaultBlockState(),
                        mastX, 32, c, box);
            }

            platform(level, box, 8, 2, c - 5, 11, planks);
            for (int[] corner : new int[][]{{9, c - 4}, {9, c + 4}, {17, c - 4}, {17, c + 4}}) {
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                        corner[0], 3, corner[1], box);
            }
            for (int x = 8; x < kind.size - 8; x += 4) {
                this.placeBlock(level, ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState(),
                        x, 2, c - 13, box);
                this.placeBlock(level, ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState(),
                        x, 2, c + 13, box);
            }
        }

        private void buildWorldTree(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState log = ModBlocks.ECHO_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaves = ModBlocks.ECHO_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true);
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            disk(level, box, c, 0, c, 22, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            disk(level, box, c, 1, c, 18, Blocks.MOSS_BLOCK.defaultBlockState());

            int[][] rootDirections = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            };
            for (int[] direction : rootDirections) {
                buildButtressRoot(level, box, c, c, direction[0], direction[1], log);
            }

            for (int y = 1; y <= 72; y++) {
                int radius = y < 14 ? 6 : y < 34 ? 5 : y < 52 ? 4 : y < 65 ? 3 : 2;
                disk(level, box, c, y, c, radius, log);
            }
            // 腰线必须用砖而不是原木:同色收分在游戏里看不出高度刻度。
            for (int bandY : new int[]{14, 34, 52, 65}) {
                int radius = bandY < 34 ? 5 : bandY < 52 ? 4 : 3;
                buildCornice(level, box, c, bandY, c, Footprint.square(radius), ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), rune);
            }

            int[][] crowns = {
                    {-17, 61, -8, 10, 6, 8}, {16, 63, -11, 9, 6, 8},
                    {-17, 64, 11, 9, 6, 8}, {17, 66, 10, 10, 6, 8},
                    {0, 68, -17, 9, 6, 8}, {0, 70, 17, 9, 6, 8},
                    {-9, 75, -4, 9, 7, 8}, {9, 76, 5, 9, 7, 8},
                    {0, 83, 0, 9, 8, 9}
            };
            for (int[] crown : crowns) {
                ellipsoid(level, box, c + crown[0], crown[1], c + crown[2],
                        crown[3], crown[4], crown[5], leaves);
            }

            int[][] branches = {
                    {c, 45, c, c - 18, 59, c - 8},
                    {c, 47, c, c + 17, 61, c - 11},
                    {c, 49, c, c - 18, 62, c + 11},
                    {c, 51, c, c + 18, 64, c + 10},
                    {c, 54, c, c, 66, c - 18},
                    {c, 56, c, c, 68, c + 18},
                    {c, 61, c, c - 10, 74, c - 4},
                    {c, 64, c, c + 10, 75, c + 5}
            };
            for (int[] branch : branches) {
                buildWorldTreeBranch(level, box,
                        branch[0], branch[1], branch[2], branch[3], branch[4], branch[5]);
            }

            ellipsoid(level, box, c + 12, 15, c - 9, 4, 3, 4, leaves);
            buildWorldTreeBranch(level, box, c + 3, 20, c - 2, c + 12, 15, c - 9);

            decorativeRing(level, box, c, 29, c, 9, 1);
            decorativeRing(level, box, c, 45, c, 12, 4);
            decorativeRing(level, box, c, 61, c, 15, 7);

            pillar(level, box, c, 83, c, 6, rune);
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c, 89, c, box);

            pillar(level, box, c, 90, c, 6, rune);
            ellipsoid(level, box, c, 92, c, 7, 5, 7, leaves);
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c, 96, c, box);
            buildStairCornice(level, box, c, 1, c, Footprint.square(17), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(21), 6, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), rune);
            this.placeBlock(level, rune, c, 2, c - 5, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 3, c - 5, box);
            buildAccessibleChest(level, random, box, c + 6, 1, c + 3, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c - 8, 1, c - 2, ModLootTables.T3_PUZZLE);
            decorateWorldTree(level, box, c);

            // 仙果正上方必须是叶子或实心面(见 HangingFruitBlock.canSurvive),否则上方变空时它会变成空气。
            // 这里挂在第二座叶冠底面下一格。
            this.placeBlock(level, ModBlocks.WORLDTREE_FRUIT.get().defaultBlockState(),
                    c + 16, 56, c - 11, box);
        }

        private void buildLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            disk(level, box, c, 0, c, 15, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            disk(level, box, c, 1, c, 12, mirror);
            for (int y = 1; y <= 70; y++) {
                int radius = y % 10 < 5 ? 5 : 4;
                hollowDisk(level, box, c, y, c, radius, mirror);
                if (y % 10 == 0) {
                    disk(level, box, c, y, c, radius + 2, mirror);
                    hollowDisk(level, box, c, y + 1, c, radius + 3, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                }

                if (y % 5 == 0) {
                    for (Direction wall : Direction.Plane.HORIZONTAL) {
                        this.placeBlock(level, lamp,
                                c + wall.getStepX() * (radius - 1), y,
                                c + wall.getStepZ() * (radius - 1), box);
                    }
                }
            }
            for (int y = 71; y <= 80; y++) {
                hollowDisk(level, box, c, y, c, 7, Blocks.GLASS.defaultBlockState());
            }
            disk(level, box, c, 70, c, 9, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            disk(level, box, c, 81, c, 6, Blocks.SEA_LANTERN.defaultBlockState());
            pillar(level, box, c, 71, c, 11, Blocks.SEA_LANTERN.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 9;
                int dz = direction.getStepZ() * 9;
                platform(level, box, c + dx - 1, 25, c + dz - 1, 3, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());

                this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock.WATERLOGGED, Boolean.FALSE),
                        c + dx, 26, c + dz, box);
            }

            this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock.WATERLOGGED, Boolean.FALSE),
                    c + 8, 1, c, box);
            buildAccessibleChest(level, random, box, c + 10, 1, c + 1, ModLootTables.MIRROR_LAKE_CACHE);
            buildSpireFinial(level, box, c, 82, c, 5, 6, mirror,
                    ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState(),
                    Blocks.SEA_LANTERN.defaultBlockState());
            buildStairCornice(level, box, c, 70, c, Footprint.square(7), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 71, c, Footprint.square(8), 4, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            decorateLighthouse(level, box, c);
        }

        private void buildMirrorSea(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            disk(level, box, c, 0, c, 34, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            disk(level, box, c, 1, c, 31, Blocks.WATER.defaultBlockState());
            hollowDisk(level, box, c, 2, c, 28, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            hollowDisk(level, box, c, 2, c, 19, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState());
            for (int i = 0; i < 52; i++) {
                int x = c - 31 + random.nextInt(63);
                int z = c - 31 + random.nextInt(63);
                if ((x - c) * (x - c) + (z - c) * (z - c) <= 30 * 30) {
                    this.placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), x, 2, z, box);
                }
            }
            for (int i = 0; i < 12; i++) {
                int x = c - 25 + random.nextInt(51);
                int z = c - 25 + random.nextInt(51);
                this.placeBlock(level, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState(), x, 1, z, box);
                this.placeBlock(level, ModBlocks.PEARL_ANEMONE.get().defaultBlockState()
                        .setValue(SeaPickleBlock.WATERLOGGED, Boolean.TRUE), x, 2, z, box);
            }

            int[][] prisms = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
            for (int i = 0; i < prisms.length; i++) {
                int x = c + prisms[i][0];
                int z = c + prisms[i][1];
                pillar(level, box, x, 1, z, 9 + i % 3, ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState(),
                        x, 10 + i % 3, z, box);
            }
            hollowDisk(level, box, c, 16, c, 22, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());

            BlockState seaStairs = ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState();
            BlockState seaSlab = ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, 1, c, Footprint.square(32), seaStairs, seaSlab, false);
            buildParapet(level, box, c, 1, c, Footprint.square(34), 6, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState());
            for (int i = 0; i < prisms.length; i++) {
                buildSpireFinial(level, box, c + prisms[i][0], 11 + i % 3, c + prisms[i][1],
                        1, 3, ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState(),
                        ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState(),
                        ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            }
            platform(level, box, c - 3, 2, c - 3, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 3, c, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                    c + 3, 3, c, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), c - 3, 3, c + 3, box);
            configureTrueSightVault(level, box, random, c - 3, 3, c + 3);
            buildAccessibleChest(level, random, box, c + 5, 2, c - 5, ModLootTables.T4_REGION_CORE);
            decorateMirrorSea(level, box, random, c);
        }

        private void buildInvertedMountains(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState cliff = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            for (int i = 0; i < 7; i++) {
                int ox = c - 21 + i * 7;
                int oz = c + (i % 2 == 0 ? -8 : 8);
                for (int y = 84; y >= 10; y--) {
                    int r = Math.max(1, (84 - y) / 8);
                    disk(level, box, ox, y, oz, r, random.nextFloat() < 0.25F
                            ? ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState() : cliff);
                    if (y % 13 == 0) {
                        hollowDisk(level, box, ox, y, oz, r + 1, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
                    }
                }
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), ox, 9, oz, box);

                this.placeBlock(level, ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState(), ox, 87, oz, box);
                for (int y = 88; y <= 96; y++) {
                    int r = 1 + (y - 88) / 3;
                    disk(level, box, ox, y, oz, r, (y + i) % 4 == 0
                            ? ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState() : cliff);
                }
                buildSpireFinial(level, box, ox, 85, oz, 1, 3, cliff,
                        ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState(),
                        ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState());
            }
            platform(level, box, c - 6, 80, c - 6, 13, cliff);
            hollowDisk(level, box, c, 81, c, 10, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            buildCornice(level, box, c, 80, c, Footprint.square(6), cliff, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 82, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 3, 82, c, box);
            buildAccessibleChest(level, random, box, c - 3, 82, c + 3, ModLootTables.T4_REGION_CORE);
            buildParapet(level, box, c, 82, c, Footprint.square(9), 5, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            decorateInvertedMountains(level, box, c);
        }

        private void buildSkyRift(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            for (int y = 5; y < 94; y++) {
                int drift = (int) Math.round(Math.sin(y / 6.0) * 5);
                this.placeBlock(level, Blocks.AMETHYST_BLOCK.defaultBlockState(), c + drift, y, c, box);
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c + drift + 1, y, c, box);
                if (y % 4 == 0) {
                    this.placeBlock(level, Blocks.END_ROD.defaultBlockState(), c + drift - 1, y, c, box);
                    this.placeBlock(level, Blocks.END_ROD.defaultBlockState(), c - drift + 1, y, c, box);
                }
                if (y % 8 == 0) {
                    this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, y, c + 3, box);
                    this.placeBlock(level, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState(), c + drift, y, c + 2, box);
                }
            }
            platform(level, box, c - 7, 4, c - 7, 15, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
            hollowDisk(level, box, c, 5, c, 11, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            buildStairCornice(level, box, c, 4, c, Footprint.square(7), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 5, c, Footprint.square(8), 4, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.OBSERVATORY_CORE.get().defaultBlockState(), c, 5, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 5, 5, c, box);
            buildAccessibleChest(level, random, box, c - 5, 5, c, ModLootTables.T5_MINIBOSS_REPEAT);

            architecturalRing(level, box, c, 97, c, 7, Blocks.AMETHYST_BLOCK.defaultBlockState());
            architecturalRing(level, box, c, 100, c, 10, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            architecturalRing(level, box, c, 103, c, 7, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                this.placeBlock(level, Blocks.END_ROD.defaultBlockState(),
                        c + direction.getStepX() * 7, 104, c + direction.getStepZ() * 7, box);
            }

            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0D;
                int px = c + (int) Math.round(Math.cos(angle) * 24);
                int pz = c + (int) Math.round(Math.sin(angle) * 24);
                int py = 10 + (i % 4) * 14;
                disk(level, box, px, py, pz, 2, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
                if (i % 2 == 0) {
                    this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(),
                            px, py + 1, pz, box);
                } else {
                    this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                            px, py + 1, pz, box);
                }
            }
            buildStairCornice(level, box, c, 5, c, Footprint.square(12), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, c, 6, c, Footprint.square(12), ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState(), ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            decorateSkyRift(level, box, c);
        }

        private void buildSilentBoat(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState hull = ModBlocks.WHISPERING_PLANKS.get().defaultBlockState();
            for (int x = 6; x < kind.size - 6; x++) {
                int half = Math.min(13, Math.max(3, Math.min(x - 4, kind.size - 5 - x) / 2));
                for (int z = c - half; z <= c + half; z++) {
                    this.placeBlock(level, hull, x, 1, z, box);
                    if (Math.abs(z - c) == half) {
                        pillar(level, box, x, 2, z, 5, hull);
                    }
                }
                if (x % 8 == 0) {
                    this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), x, 3, c - half + 1, box);
                    this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), x, 3, c + half - 1, box);
                }
            }
            for (int y = 2; y < 22; y++) {
                this.placeBlock(level, ModBlocks.WHISPERING_LOG.get().defaultBlockState(), c, y, c, box);
            }
            platform(level, box, c - 9, 6, c - 4, 7, Blocks.DARK_OAK_SLAB.defaultBlockState());
            platform(level, box, c + 4, 5, c - 5, 8, Blocks.DARK_OAK_SLAB.defaultBlockState());
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 22, c, box);

            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            // 批量照明必须用不可破坏的琉璃:共鸣烛可打碎,当唯一光源会让整片舱室失明。
            BlockState hushLamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
            illuminateInterior(level, box, c - 6, c - 1, 5, 3, 3, 3, hushLamp);
            illuminateInterior(level, box, c + 7, c - 2, 4, 3, 3, 3, hushLamp);
            buildSpireFinial(level, box, c, 23, c, 1, 3,
                    ModBlocks.WHISPERING_LOG.get().defaultBlockState(),
                    ModBlocks.WHISPERING_LATTICE.get().defaultBlockState(), candle);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 10, 2, c, box);
            buildAccessibleChest(level, random, box, c - 10, 2, c, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c + 18, 2, c + 3, ModLootTables.SILENT_HUT_CHEST);

            buildAccessibleChest(level, random, box, c - 6, 7, c - 2, ModLootTables.T4_REGION_CORE);

            buildSealedManor(level, box, 11, 1, 11, 9, 9, 3, 5,
                    ModBlocks.WHISPERING_PLANKS.get().defaultBlockState(),
                    ModBlocks.WHISPERING_LOG.get().defaultBlockState(),
                    ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.WHISPERING_LATTICE.get().defaultBlockState(),
                    ModBlocks.BOGLIGHT_STONE.get().defaultBlockState());
            buildCornice(level, box, c, 4, c, Footprint.of(14, 30), ModBlocks.WHISPERING_PLANKS.get().defaultBlockState(), ModBlocks.WHISPERING_LATTICE.get().defaultBlockState());
            decorateSilentBoat(level, box, c);
        }

        private void buildBellTower(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState brick = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            for (int y = 0; y < 78; y++) {
                int radius = y < 10 ? 10 : 7;
                hollowDisk(level, box, c, y, c, radius, random.nextFloat() < 0.25F
                        ? ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState() : brick);
                if (y % 12 == 0) {
                    disk(level, box, c, y, c, radius + 1, ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState());
                }

                if (y > 2 && y % 6 == 0) {
                    // 塔身约 50 盏壁灯必须用不可破坏的琉璃:换成共鸣烛会让打碎后整塔失明。
                    for (Direction wall : Direction.Plane.HORIZONTAL) {
                        this.placeBlock(level,
                                ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState(),
                                c + wall.getStepX() * (radius - 1), y,
                                c + wall.getStepZ() * (radius - 1), box);
                    }
                }
            }
            platform(level, box, c - 9, 78, c - 9, 19, ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState());
            for (int y = 79; y < 90; y++) {
                hollowDisk(level, box, c, y, c, 8, Blocks.COPPER_BLOCK.defaultBlockState());
            }
            disk(level, box, c, 83, c, 5, ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 10;
                int dz = direction.getStepZ() * 10;
                pillar(level, box, c + dx, 78, c + dz, 8, Blocks.COPPER_BLOCK.defaultBlockState());
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c + dx, 86, c + dz, box);
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 2, c - 6, box);
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 79, c, box);

            buildAccessibleChest(level, random, box, c - 6, 1, c - 2, ModLootTables.T5_MINIBOSS_REPEAT);
            buildAccessibleChest(level, random, box, c - 5, 79, c, ModLootTables.BROKEN_ARCHIVE_CHEST);

            buildAccessibleChest(level, random, box, c, 84, c + 2, ModLootTables.T4_REGION_CORE);
            buildTrueSightVaultNiche(level, random, box, c + 6, 79, c + 6, Direction.NORTH);
            for (Direction spire : Direction.Plane.HORIZONTAL) {
                buildSpireFinial(level, box,
                        c + spire.getStepX() * 6, 90, c + spire.getStepZ() * 6,
                        1, 4, Blocks.COPPER_BLOCK.defaultBlockState(),
                        ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState(), candle);
            }
            buildStairCornice(level, box, c, 78, c, Footprint.square(8), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 79, c, Footprint.square(9), 5, ModBlocks.ASHEN_BELLSTONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState());
            decorateBellTower(level, box, c);
        }

        private void decorateWorldTree(WorldGenLevel level, BoundingBox box, int c) {
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState flower = ModBlocks.ECHO_FLOWER.get().defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            for (int i = 0; i < 32; i++) {
                double angle = i * Math.PI / 16.0;
                int x = c + (int) Math.round(Math.cos(angle) * 22);
                int z = c + (int) Math.round(Math.sin(angle) * 22);
                this.placeBlock(level, moss, x, 1, z, box);
                if (i % 4 == 0) {
                    this.placeBlock(level, lamp, x, 2, z, box);
                } else {
                    this.placeBlock(level, flower, x, 2, z, box);
                }
            }

            int[][] canopyLights = {
                    {-20, 61, -8}, {-14, 65, -12}, {18, 63, -12}, {15, 68, -6},
                    {-19, 64, 12}, {-13, 68, 15}, {20, 66, 10}, {14, 71, 14},
                    {-6, 77, -8}, {8, 79, 9}, {-2, 86, -5}, {4, 84, 7}
            };
            for (int[] light : canopyLights) {
                this.placeBlock(level, lamp, c + light[0], light[1], c + light[2], box);
            }
        }

        private void decorateLighthouse(WorldGenLevel level, BoundingBox box, int c) {
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int y = 20; y <= 80; y += 8) {
                hollowDisk(level, box, c, y, c, 10, lamp);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 13;
                int dz = direction.getStepZ() * 13;
                pillar(level, box, c + dx, 2, c + dz, 5, Blocks.SEA_LANTERN.defaultBlockState());
            }
        }

        private void decorateMirrorSea(WorldGenLevel level, BoundingBox box, RandomSource random, int c) {
            BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            BlockState coral = ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 88; i++) {
                int x = c - 33 + random.nextInt(67);
                int z = c - 33 + random.nextInt(67);
                int d2 = (x - c) * (x - c) + (z - c) * (z - c);
                if (d2 <= 32 * 32) {
                    this.placeBlock(level, random.nextFloat() < 0.25F ? coral : glass, x, 2, z, box);
                }
            }
        }

        private void decorateInvertedMountains(WorldGenLevel level, BoundingBox box, int c) {
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int i = -2; i <= 2; i++) {
                int x = c + i * 9;
                pillar(level, box, x, 86, c - 13, 3, lamp);
                pillar(level, box, x, 86, c + 13, 3, lamp);
            }
            hollowDisk(level, box, c, 84, c, 15, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
        }

        private void decorateSkyRift(WorldGenLevel level, BoundingBox box, int c) {
            BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int y = 12; y < 98; y += 6) {
                int radius = 5 + y % 11;
                int x = c + (int) Math.round(Math.sin(y / 5.0) * radius);
                int z = c + (int) Math.round(Math.cos(y / 5.0) * radius);
                this.placeBlock(level, y % 12 == 0 ? lamp : amethyst, x, y, z, box);
                this.placeBlock(level, Blocks.END_ROD.defaultBlockState(), c - (x - c), y, c - (z - c), box);
            }
        }

        private void decorateSilentBoat(WorldGenLevel level, BoundingBox box, int c) {
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            for (int x = 10; x < kind.size - 10; x += 6) {
                this.placeBlock(level, candle, x, 4, c - 14, box);
                this.placeBlock(level, candle, x, 4, c + 14, box);
            }
            // 缆索挂在桅间(x=c±9),桅杆列 x=c 已被帆布覆盖。CHAIN 上方要有支撑,靠随后铺的 y=19 横桁承托。
            for (int mastGapX : new int[]{c - 9, c + 9}) {
                for (int z = c - 2; z <= c + 2; z++) {
                    this.placeBlock(level, Blocks.CHAIN.defaultBlockState(),
                            mastGapX, 18, z, box);
                }
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, ModBlocks.WHISPERING_LOG.get().defaultBlockState(),
                            mastGapX, 19, z, box);
                }
            }
        }

        private void decorateBellTower(WorldGenLevel level, BoundingBox box, int c) {
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            BlockState copper = Blocks.EXPOSED_COPPER.defaultBlockState();
            BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
            hollowDisk(level, box, c, 91, c, 10, copper);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 12;
                int dz = direction.getStepZ() * 12;
                pillar(level, box, c + dx, 88, c + dz, 7, copper);
                this.placeBlock(level, candle, c + dx, 95, c + dz, box);
            }

            int[][] shards = {{5, 0, 13}, {0, 5, 8}, {-5, 0, 11}, {0, -5, 5}};
            for (int[] shard : shards) {
                pillar(level, box, c + shard[0], 92, c + shard[1], shard[2], rusted);
                this.placeBlock(level, candle, c + shard[0], 92 + shard[2], c + shard[1], box);
            }
        }

        private void buildButtressRoot(WorldGenLevel level, BoundingBox box, int cx, int cz,
                                       int dx, int dz, BlockState verticalLog) {
            Direction.Axis axis = Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            BlockState rootLog = verticalLog.setValue(RotatedPillarBlock.AXIS, axis);
            for (int i = 0; i <= 22; i++) {
                int x = cx + dx * i;
                int z = cz + dz * i;
                int y = Math.max(1, 7 - i / 4);
                int width = Math.max(0, 3 - i / 7);
                for (int offset = -width; offset <= width; offset++) {
                    int px = axis == Direction.Axis.X ? x : x + offset;
                    int pz = axis == Direction.Axis.Z ? z : z + offset;
                    this.placeBlock(level, rootLog, px, y, pz, box);
                    if (i < 10 && Math.abs(offset) < width) {
                        this.placeBlock(level, verticalLog, px, y + 1, pz, box);
                    }
                }
            }
        }

        private void buildWorldTreeBranch(WorldGenLevel level, BoundingBox box,
                                          int x0, int y0, int z0, int x1, int y1, int z1) {
            int steps = Math.max(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)), Math.abs(z1 - z0));
            int previousX = x0;
            int previousZ = z0;
            for (int i = 0; i <= steps; i++) {
                double progress = steps == 0 ? 0.0D : (double) i / steps;
                int x = (int) Math.round(x0 + (x1 - x0) * progress);
                int y = (int) Math.round(y0 + (y1 - y0) * progress);
                int z = (int) Math.round(z0 + (z1 - z0) * progress);
                Direction.Axis axis = Math.abs(x - previousX) >= Math.abs(z - previousZ)
                        ? Direction.Axis.X : Direction.Axis.Z;
                BlockState branchLog = ModBlocks.ECHO_LOG.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, axis);
                this.placeBlock(level, branchLog, x, y, z, box);
                if (progress < 0.55D) {
                    this.placeBlock(level, branchLog, x, y - 1, z, box);
                    if (axis == Direction.Axis.X) {
                        this.placeBlock(level, branchLog, x, y, z + 1, box);
                    } else {
                        this.placeBlock(level, branchLog, x + 1, y, z, box);
                    }
                }
                previousX = x;
                previousZ = z;
            }
        }

        private void ellipsoid(WorldGenLevel level, BoundingBox box, int cx, int cy, int cz,
                               int radiusX, int radiusY, int radiusZ, BlockState state) {
            for (int x = cx - radiusX; x <= cx + radiusX; x++) {
                for (int y = cy - radiusY; y <= cy + radiusY; y++) {
                    for (int z = cz - radiusZ; z <= cz + radiusZ; z++) {
                        double dx = (double) (x - cx) / radiusX;
                        double dy = (double) (y - cy) / radiusY;
                        double dz = (double) (z - cz) / radiusZ;
                        if (dx * dx + dy * dy + dz * dz <= 1.0D) {
                            placeIfAir(level, box, x, y, z, state);
                        }
                    }
                }
            }
        }

        private void decorativeRing(WorldGenLevel level, BoundingBox box,
                                    int cx, int y, int cz, int radius, int phase) {
            for (int i = 0; i < 48; i++) {
                if ((i + phase) % 11 == 0 || (i + phase) % 11 == 1) {
                    continue;
                }
                double angle = i * Math.PI / 24.0D;
                int x = cx + (int) Math.round(Math.cos(angle) * radius);
                int z = cz + (int) Math.round(Math.sin(angle) * radius);
                BlockState state = i % 8 == 0
                        ? ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState()
                        : ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
                this.placeBlock(level, state, x, y, z, box);
            }
        }

        private void placeIfAir(WorldGenLevel level, BoundingBox box,
                                int x, int y, int z, BlockState state) {
            BlockPos worldPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
            if (box.isInside(worldPos) && level.getBlockState(worldPos).isAir()) {
                this.placeBlock(level, state, x, y, z, box);
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z, int height, BlockState state) {
            for (int i = 0; i < height; i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0, int size, BlockState state) {
            for (int x = x0; x < x0 + size; x++) {
                for (int z = z0; z < z0 + size; z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
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

        private void hollowDisk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            int outer = radius * radius;
            int inner = Math.max(0, radius - 2) * Math.max(0, radius - 2);
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
            tag.putString("WonderKind", this.kind.id);
        }
    }
}
