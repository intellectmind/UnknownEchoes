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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 第一维度世界奇观首版:用代码结构提供可 /locate、可自然生成的远景地标。
 * 每类至少包含远景剪影、核心交互点、日志/奖励点和隐藏回访点;不承载唯一关键进度。
 */
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
        ECHO_WORLD_TREE("echo_world_tree", 61, 92, 64),
        ETERNAL_ECHO_LIGHTHOUSE("eternal_echo_lighthouse", 41, 78, 64),
        MIRROR_SEA("mirror_sea", 67, 18, 62),
        INVERTED_MOUNTAINS("inverted_mountains", 65, 102, 150),
        SKY_RIFT("sky_rift", 49, 104, 185),
        SILENT_GREAT_BOAT("silent_great_boat", 77, 30, 63),
        BROKEN_BELL_TOWER("broken_bell_tower", 45, 98, 64);

        private final String id;
        private final int size;
        private final int height;
        private final int startY;

        Kind(String id, int size, int height, int startY) {
            this.id = id;
            this.size = size;
            this.height = height;
            this.startY = startY;
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
            switch (this.kind) {
                case ECHO_WORLD_TREE -> buildWorldTree(level, random, box);
                case ETERNAL_ECHO_LIGHTHOUSE -> buildLighthouse(level, random, box);
                case MIRROR_SEA -> buildMirrorSea(level, random, box);
                case INVERTED_MOUNTAINS -> buildInvertedMountains(level, random, box);
                case SKY_RIFT -> buildSkyRift(level, random, box);
                case SILENT_GREAT_BOAT -> buildSilentBoat(level, random, box);
                case BROKEN_BELL_TOWER -> buildBellTower(level, random, box);
            }
        }

        private void buildWorldTree(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState log = ModBlocks.ECHO_LOG.get().defaultBlockState();
            BlockState leaves = ModBlocks.ECHO_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true);
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            disk(level, box, c, 0, c, 22, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            disk(level, box, c, 1, c, 18, Blocks.MOSS_BLOCK.defaultBlockState());
            for (int y = 1; y <= 60; y++) {
                int radius = y < 12 ? 5 : 4;
                disk(level, box, c, y, c, radius, log);
                if (y % 8 == 0) {
                    root(level, box, c, y, c, y / 8);
                }
                if (y % 16 == 0) {
                    hollowDisk(level, box, c, y, c, 8, rune);
                }
            }
            for (int y = 54; y <= 82; y++) {
                int radius = Math.max(9, 20 - Math.abs(68 - y));
                disk(level, box, c, y, c, radius, leaves);
                if (y % 7 == 0) {
                    hollowDisk(level, box, c, y, c, Math.max(6, radius - 5), rune);
                }
            }
            for (int i = 0; i < 8; i++) {
                int angleX = c + (i % 4 < 2 ? -14 : 14);
                int angleZ = c + (i % 2 == 0 ? -14 : 14);
                pillar(level, box, angleX, 2, angleZ, 4 + random.nextInt(3), ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), angleX, 7, angleZ, box);
            }
            this.placeBlock(level, rune, c, 2, c - 5, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 3, c - 5, box);
            buildAccessibleChest(level, random, box, c + 6, 1, c + 3, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c - 8, 1, c - 2, ModLootTables.T3_PUZZLE);
            decorateWorldTree(level, box, random, c);
        }

        private void buildLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            disk(level, box, c, 0, c, 15, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            disk(level, box, c, 1, c, 12, ModBlocks.MIRROR_STONE.get().defaultBlockState());
            for (int y = 1; y <= 58; y++) {
                int radius = y % 10 < 5 ? 5 : 4;
                hollowDisk(level, box, c, y, c, radius, mirror);
                if (y % 10 == 0) {
                    disk(level, box, c, y, c, radius + 2, mirror);
                    hollowDisk(level, box, c, y + 1, c, radius + 3, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
                }
            }
            for (int y = 59; y <= 68; y++) {
                hollowDisk(level, box, c, y, c, 7, Blocks.GLASS.defaultBlockState());
            }
            disk(level, box, c, 58, c, 9, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            disk(level, box, c, 69, c, 6, Blocks.SEA_LANTERN.defaultBlockState());
            pillar(level, box, c, 59, c, 11, Blocks.SEA_LANTERN.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 9;
                int dz = direction.getStepZ() * 9;
                platform(level, box, c + dx - 1, 25, c + dz - 1, 3, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState(), c + dx, 26, c + dz, box);
            }
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c, 2, c, box);
            this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState(), c + 8, 1, c, box);
            buildAccessibleChest(level, random, box, c - 2, 2, c, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c + 10, 1, c + 1, ModLootTables.MIRROR_LAKE_CACHE);
            decorateLighthouse(level, box, c);
        }

        private void buildMirrorSea(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            disk(level, box, c, 0, c, 30, ModBlocks.MIRROR_STONE.get().defaultBlockState());
            disk(level, box, c, 1, c, 27, Blocks.WATER.defaultBlockState());
            hollowDisk(level, box, c, 2, c, 24, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            hollowDisk(level, box, c, 2, c, 15, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState());
            for (int i = 0; i < 44; i++) {
                int x = c - 27 + random.nextInt(55);
                int z = c - 27 + random.nextInt(55);
                if ((x - c) * (x - c) + (z - c) * (z - c) <= 26 * 26) {
                    this.placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), x, 2, z, box);
                }
            }
            for (int i = 0; i < 10; i++) {
                int x = c - 21 + random.nextInt(43);
                int z = c - 21 + random.nextInt(43);
                this.placeBlock(level, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState(), x, 1, z, box);
                this.placeBlock(level, ModBlocks.PEARL_ANEMONE.get().defaultBlockState()
                        .setValue(SeaPickleBlock.WATERLOGGED, Boolean.TRUE), x, 2, z, box);
            }
            platform(level, box, c - 3, 2, c - 3, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 3, c, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c + 3, 3, c, box);
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
            }
            platform(level, box, c - 6, 80, c - 6, 13, cliff);
            hollowDisk(level, box, c, 81, c, 10, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 82, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 3, 82, c, box);
            buildAccessibleChest(level, random, box, c - 3, 82, c + 3, ModLootTables.T4_REGION_CORE);
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
            this.placeBlock(level, ModBlocks.OBSERVATORY_CORE.get().defaultBlockState(), c, 5, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 5, 5, c, box);
            buildAccessibleChest(level, random, box, c - 5, 5, c, ModLootTables.T5_MINIBOSS_REPEAT);
            decorateSkyRift(level, box, c);
        }

        private void buildSilentBoat(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
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
                this.placeBlock(level, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), c, y, c, box);
            }
            platform(level, box, c - 9, 6, c - 4, 7, Blocks.DARK_OAK_SLAB.defaultBlockState());
            platform(level, box, c + 4, 5, c - 5, 8, Blocks.DARK_OAK_SLAB.defaultBlockState());
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 22, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 10, 2, c, box);
            buildAccessibleChest(level, random, box, c - 10, 2, c, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c + 18, 2, c + 3, ModLootTables.SILENT_HUT_CHEST);
            decorateSilentBoat(level, box, c);
        }

        private void buildBellTower(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState brick = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            for (int y = 0; y < 78; y++) {
                int radius = y < 10 ? 10 : 7;
                hollowDisk(level, box, c, y, c, radius, random.nextFloat() < 0.25F
                        ? ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState() : brick);
                if (y % 12 == 0) {
                    disk(level, box, c, y, c, radius + 1, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
                }
            }
            platform(level, box, c - 9, 78, c - 9, 19, ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState());
            for (int y = 79; y < 90; y++) {
                hollowDisk(level, box, c, y, c, 8, Blocks.COPPER_BLOCK.defaultBlockState());
            }
            disk(level, box, c, 83, c, 5, Blocks.GOLD_BLOCK.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 10;
                int dz = direction.getStepZ() * 10;
                pillar(level, box, c + dx, 78, c + dz, 8, Blocks.COPPER_BLOCK.defaultBlockState());
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c + dx, 86, c + dz, box);
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 2, c - 6, box);
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 79, c, box);
            buildAccessibleChest(level, random, box, c + 3, 3, c, ModLootTables.T5_MINIBOSS_REPEAT);
            buildAccessibleChest(level, random, box, c - 5, 79, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            buildTrueSightVaultNiche(level, random, box, c + 6, 79, c + 6, Direction.NORTH);
            decorateBellTower(level, box, c);
        }

        private void decorateWorldTree(WorldGenLevel level, BoundingBox box, RandomSource random, int c) {
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState flower = ModBlocks.ECHO_FLOWER.get().defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI / 12.0;
                int x = c + (int) Math.round(Math.cos(angle) * 20);
                int z = c + (int) Math.round(Math.sin(angle) * 20);
                if (i % 3 == 0) {
                    this.placeBlock(level, lamp, x, 2, z, box);
                } else {
                    this.placeBlock(level, moss, x, 1, z, box);
                    this.placeBlock(level, flower, x, 2, z, box);
                }
            }
            for (int i = 0; i < 18; i++) {
                int x = c - 18 + random.nextInt(37);
                int z = c - 18 + random.nextInt(37);
                this.placeBlock(level, lamp, x, 72 + random.nextInt(9), z, box);
            }
        }

        private void decorateLighthouse(WorldGenLevel level, BoundingBox box, int c) {
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int y = 20; y <= 68; y += 8) {
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
            for (int i = 0; i < 72; i++) {
                int x = c - 29 + random.nextInt(59);
                int z = c - 29 + random.nextInt(59);
                int d2 = (x - c) * (x - c) + (z - c) * (z - c);
                if (d2 <= 28 * 28) {
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
            for (int z = c - 11; z <= c + 11; z += 4) {
                this.placeBlock(level, Blocks.CHAIN.defaultBlockState(), c, 18, z, box);
            }
        }

        private void decorateBellTower(WorldGenLevel level, BoundingBox box, int c) {
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            BlockState copper = Blocks.EXPOSED_COPPER.defaultBlockState();
            hollowDisk(level, box, c, 91, c, 10, copper);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX() * 12;
                int dz = direction.getStepZ() * 12;
                pillar(level, box, c + dx, 88, c + dz, 7, copper);
                this.placeBlock(level, candle, c + dx, 95, c + dz, box);
            }
        }

        private void configureTrueSightVault(WorldGenLevel level, BoundingBox box, RandomSource random,
                                             int x, int y, int z) {
            BlockPos vaultPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y),
                    this.getWorldZ(x, z));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
        }

        private void root(WorldGenLevel level, BoundingBox box, int c, int y, int z, int index) {
            int dx = switch (index % 4) {
                case 0 -> 1;
                case 1 -> -1;
                default -> 0;
            };
            int dz = switch (index % 4) {
                case 2 -> 1;
                case 3 -> -1;
                default -> 0;
            };
            for (int i = 0; i < 13; i++) {
                this.placeBlock(level, ModBlocks.ECHO_LOG.get().defaultBlockState(),
                        c + dx * i, Math.max(1, y - i / 2), z + dz * i, box);
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
