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
import net.minecraft.world.level.block.RotatedPillarBlock;
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
            this.placeBlock(level, rune, c, 2, c - 5, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 3, c - 5, box);
            buildAccessibleChest(level, random, box, c + 6, 1, c + 3, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c - 8, 1, c - 2, ModLootTables.T3_PUZZLE);
            decorateWorldTree(level, box, c);

            // 固定坐标由结构分块包围盒裁切，保证每棵新生成世界树恰好放置一枚仙果。
            this.placeBlock(level, ModBlocks.WORLDTREE_FRUIT.get().defaultBlockState(),
                    c + 12, 11, c - 9, box);
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
                // 干地/空中放置:浮标默认 WATERLOGGED=TRUE,不清掉会往外漏水
                this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock.WATERLOGGED, Boolean.FALSE),
                        c + dx, 26, c + dz, box);
            }
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                    c, 2, c, box);
            this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState()
                    .setValue(cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock.WATERLOGGED, Boolean.FALSE),
                    c + 8, 1, c, box);
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
            // 奇观只提供区域探索材料；终局奖励由个人完成流程结算。
            buildAccessibleChest(level, random, box, c - 6, 7, c - 2, ModLootTables.T4_REGION_CORE);
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
            // 登塔奖励保留，但不越过个人终局结算。
            buildAccessibleChest(level, random, box, c, 84, c + 2, ModLootTables.T4_REGION_CORE);
            buildTrueSightVaultNiche(level, random, box, c + 6, 79, c + 6, Direction.NORTH);
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
