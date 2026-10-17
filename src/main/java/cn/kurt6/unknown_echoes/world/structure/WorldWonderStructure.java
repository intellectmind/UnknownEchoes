package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.WonderTrialBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleDialBlock;
import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleLogic;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 第一维度世界奇观:用代码结构提供可 /locate、可自然生成的远景地标。 */
public class WorldWonderStructure extends TerrainAnchoredStructure {
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
        ECHO_WORLD_TREE("echo_world_tree", 129, 128, 64, 64, 127),
        ETERNAL_ECHO_LIGHTHOUSE("eternal_echo_lighthouse", 129, 128, 64, 64, 127),
        MIRROR_SEA("mirror_sea", 129, 128, 62, 64, 127),
        INVERTED_MOUNTAINS("inverted_mountains", 129, 128, 150, 0, 0),
        SKY_RIFT("sky_rift", 129, 128, 185, 0, 0),
        SILENT_GREAT_BOAT("silent_great_boat", 129, 128, 63, 64, 127),
        BROKEN_BELL_TOWER("broken_bell_tower", 129, 128, 64, 64, 127);

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
                {-8, 0}, {7, 0}, {-6, -6}, {6, -6},
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
        protected boolean usesSurfaceAnchor() {
            return kind.startY < 120;
        }

        @Override
        protected boolean createChest(WorldGenLevel level, BoundingBox box, RandomSource random,
                                      int x, int y, int z,
                                      net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            // 邻区块可能尚未清出箱子周围的空位，默认朝向不能依赖生成中的邻格。
            return super.createChest(level, box, random, this.getWorldPos(x, y, z), loot,
                    Blocks.CHEST.defaultBlockState());
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
            buildWonderInteriors(level, box);
            addWonderGameplay(level, random, box);
            addWonderExpedition(level, random, box);
            addWonderLoot(level, random, box);
            if (this.kind == Kind.INVERTED_MOUNTAINS) {
                this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), kind.size / 2, 107, kind.size / 2, box);
            }
            if (kind.startY < 120) {
                BlockState foundation = switch (kind) {
                    case ECHO_WORLD_TREE -> ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
                    case ETERNAL_ECHO_LIGHTHOUSE, MIRROR_SEA -> ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
                    case SILENT_GREAT_BOAT -> ModBlocks.SHIP_TIMBER.get().defaultBlockState();
                    default -> ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                };
                anchorGroundColumns(level, box, 0, foundation);
            }
        }

        private void buildCanopyCity(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState log = ModBlocks.ECHO_LOG.get().defaultBlockState();
            BlockState wood = ModBlocks.ECHO_PLANKS.get().defaultBlockState();
            BlockState trim = ModBlocks.WHISPERING_TIMBER_FRAME.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                for (int y = 2; y <= 66; y++) {
                    double r = 10 + 31 * Math.pow(1.0 - (y - 2) / 64.0, 2);
                    int x = c + (int) Math.round(Math.cos(a) * r);
                    int z = c + (int) Math.round(Math.sin(a) * r);
                    detailBeam(level, box, x, y, z, x, y + 1, z, y < 20 ? 2 : 1, log);
                }
            }
            for (int i = 0; i < 4; i++) {
                Direction d = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[i];
                int y = i % 2 == 0 ? 52 : 76;
                int x = c + d.getStepX() * 26;
                int z = c + d.getStepZ() * 26;
                detailBeam(level, box, c, y - 23, c, x, y - 2, z, 2, log);
                detailGallery(level, box, x, y, z, 9, 0, wood, trim);
                for (int p = 0; p < 8; p++) {
                    double a = p * Math.PI / 4;
                    int px = x + (int) Math.round(Math.cos(a) * 7);
                    int pz = z + (int) Math.round(Math.sin(a) * 7);
                    detailBeam(level, box, px, y + 1, pz, px, y + 7, pz, 0, log);
                    detailBeam(level, box, px, y + 8, pz, x, y + 14, z, 0, trim);
                    this.placeBlock(level, lamp, px, y + 6, pz, box);
                }
                for (int roof = 0; roof <= 6; roof++) {
                    hollowDisk(level, box, x, y + 8 + roof, z, Math.max(1, 9 - roof), trim);
                }
                for (int distance = 3; distance <= 26; distance++) {
                    for (int w = -2; w <= 2; w++) {
                        this.placeBlock(level, wood, c + d.getStepX() * distance + d.getStepZ() * w,
                                y, c + d.getStepZ() * distance + d.getStepX() * w, box);
                    }
                }
            }
        }

        private void buildLighthouseCrown(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
            BlockState ivory = ModBlocks.IVORY_REEF_ASHLAR.get().defaultBlockState();
            BlockState glass = Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
            BlockState lamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 8 + i * Math.PI / 4;
                for (int y = 3; y <= 86; y++) {
                    double r = 17 + 28 * Math.pow(1.0 - (y - 3) / 83.0, 2);
                    int x = c + (int) Math.round(Math.cos(a) * r);
                    int z = c + (int) Math.round(Math.sin(a) * r);
                    detailBeam(level, box, x, y, z, x, y + 1, z, 1, y % 14 < 2 ? ivory : reef);
                }
            }
            for (int y : new int[]{16, 46, 76}) {
                int r = y == 16 ? 25 : y == 46 ? 22 : 19;
                detailGallery(level, box, c, y, c, r, r - 4, ivory, reef);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    detailLancet(level, box, c, y + 3, c, d, r - 5, 3, 10, reef, glass);
                }
            }
            for (int y = 99; y <= 124; y++) {
                int r = y < 116 ? 17 : Math.max(2, (int) Math.round(18 * Math.sqrt((125 - y) / 10.0)));
                BlockState roof = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
                hollowDisk(level, box, c, y, c, r, y >= 116 ? roof : y == 99 ? ivory : glass);
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    int x = c + (int) Math.round(Math.cos(a) * r);
                    int z = c + (int) Math.round(Math.sin(a) * r);
                    this.placeBlock(level, y >= 116 ? ivory : reef, x, y, z, box);
                }
            }
            for (int y = 104; y <= 113; y++) {
                hollowDisk(level, box, c, y, c, 6 + Math.abs(108 - y) / 2, lamp);
            }
            detailGallery(level, box, c, 101, c, 23, 14, ivory, reef);
            detailBeam(level, box, c, 115, c, c, 126, c, 1, ivory);
        }

        private void buildMirrorVaults(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState quartz = ModBlocks.IVORY_REEF_ASHLAR.get().defaultBlockState();
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState tile = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            BlockState lamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int px = c + (int) Math.round(Math.cos(a) * 40);
                int pz = c + (int) Math.round(Math.sin(a) * 40);
                for (int t = 0; t < 90; t++) {
                    double b = t * Math.PI / 180;
                    double next = (t + 1) * Math.PI / 180;
                    detailBeam(level, box,
                            c + (int) Math.round(Math.cos(a) * 40 * Math.cos(b)), 12 + (int) Math.round(64 * Math.sin(b)),
                            c + (int) Math.round(Math.sin(a) * 40 * Math.cos(b)),
                            c + (int) Math.round(Math.cos(a) * 40 * Math.cos(next)), 12 + (int) Math.round(64 * Math.sin(next)),
                            c + (int) Math.round(Math.sin(a) * 40 * Math.cos(next)), 1, quartz);
                }
                detailBeam(level, box, px, 2, pz, px, 12, pz, 1, tile);
                this.placeBlock(level, lamp, px, 11, pz, box);
            }
            detailGallery(level, box, c, 60, c, 29, 24, mirror, tile);
            for (int y = 68; y <= 98; y++) {
                double t = (y - 68) / 30.0;
                int r = Math.max(2, (int) Math.round(28 * Math.sqrt(1 - t * t)));
                for (int segment = 0; segment < 360; segment++) {
                    double a = segment * Math.PI / 180;
                    int phase = segment % 30;
                    boolean rib = phase <= 1 || phase >= 29;
                    if (!rib && (phase < 6 || phase > 24 || y < 74)) continue;
                    int x = c + (int) Math.round(Math.cos(a) * r);
                    int z = c + (int) Math.round(Math.sin(a) * r);
                    this.placeBlock(level, rib ? quartz : glass, x, y, z, box);
                }
            }
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int x = c + (int) Math.round(Math.cos(a) * 28);
                int z = c + (int) Math.round(Math.sin(a) * 28);
                detailBeam(level, box, x, 60, z, x, 68, z, 0, quartz);
                this.placeBlock(level, lamp, x, 67, z, box);
            }
        }

        private void buildHangingStrata(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState rock = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState cut = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState wind = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int[][] peaks = {{-36, -22}, {0, -42}, {36, -22}, {-36, 22}, {0, 42}, {36, 22}};
            for (int i = 0; i < peaks.length; i++) {
                int x = c + peaks[i][0];
                int z = c + peaks[i][1];
                for (int rib = 0; rib < 7; rib++) {
                    double a = rib * Math.PI * 2 / 7 + i * 0.3;
                    for (int y = 22 + rib % 3 * 5; y <= 98 - i % 3 * 5; y++) {
                        int radius = Math.min(18, 2 + (y - 12) / 5) + (y / 7 + rib) % 2;
                        int px = x + (int) Math.round(Math.cos(a) * radius);
                        int pz = z + (int) Math.round(Math.sin(a) * radius);
                        detailBeam(level, box, px, y, pz, px, y + 1, pz, y > 62 ? 1 : 0,
                                y % 17 < 3 ? cut : rock);
                    }
                }
                for (int y : new int[]{64, 88}) {
                    int radius = (y - 8) / 5 + 2;
                    for (int a = 25 + i * 30; a <= 215 + i * 30; a++) {
                        double angle = a * Math.PI / 180;
                        for (int r = radius - 3; r <= radius; r++) {
                            int px = x + (int) Math.round(Math.cos(angle) * r);
                            int pz = z + (int) Math.round(Math.sin(angle) * r);
                            this.placeBlock(level, r == radius ? cut : wind, px, y, pz, box);
                        }
                    }
                    for (int rib = 0; rib < 6; rib++) {
                        double a = rib * Math.PI / 3;
                        int px = x + (int) Math.round(Math.cos(a) * radius);
                        int pz = z + (int) Math.round(Math.sin(a) * radius);
                        detailBeam(level, box, px, y - 7, pz, px, y - 1, pz, 0, cut);
                        this.placeBlock(level, lamp, px, y - 6, pz, box);
                    }
                }
            }
        }

        private void buildRiftArmillary(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState dark = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState rim = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState light = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            for (int ring = 0; ring < 3; ring++) {
                double yaw = Math.PI / 4 + ring * Math.PI / 3;
                double tilt = ring == 2 ? Math.PI / 8 : (ring == 0 ? 1 : -1) * Math.PI / 3;
                int r = 49 - ring * 3;
                for (int t = 0; t < 360; t++) {
                    double a = t * Math.PI / 180;
                    double next = (t + 1) * Math.PI / 180;
                    int x = c + (int) Math.round(r * (Math.cos(a) * Math.cos(yaw) - Math.sin(a) * Math.cos(tilt) * Math.sin(yaw)));
                    int z = c + (int) Math.round(r * (Math.cos(a) * Math.sin(yaw) + Math.sin(a) * Math.cos(tilt) * Math.cos(yaw)));
                    int y = 63 + (int) Math.round(Math.sin(a) * r * Math.sin(tilt));
                    int nx = c + (int) Math.round(r * (Math.cos(next) * Math.cos(yaw) - Math.sin(next) * Math.cos(tilt) * Math.sin(yaw)));
                    int nz = c + (int) Math.round(r * (Math.cos(next) * Math.sin(yaw) + Math.sin(next) * Math.cos(tilt) * Math.cos(yaw)));
                    int ny = 63 + (int) Math.round(Math.sin(next) * r * Math.sin(tilt));
                    detailBeam(level, box, x, y, z, nx, ny, nz, 1,
                            t % 30 < 3 ? light : Blocks.PURPUR_BLOCK.defaultBlockState());
                    if (t % 30 == 0) detailBeam(level, box, x, y - 2, z, x, y + 2, z, 0, rim);
                }
            }
            for (int y = 30; y <= 94; y++) {
                int drift = (int) Math.round(3 * Math.sin(y / 13.0));
                int half = Math.max(1, 5 - Math.abs(62 - y) / 7);
                for (int x = -half; x <= half; x++) {
                    this.placeBlock(level, Math.abs(x) == half ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                            : Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), c + drift + x, y, c, box);
                }
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int x = c + d.getStepX() * 34;
                int z = c + d.getStepZ() * 34;
                for (int y : new int[]{18, 50, 84}) {
                    detailLancet(level, box, x, y + 2, z, d, 9, 2, 10, rim, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
                }
                for (int s : new int[]{-1, 1}) {
                    int px = x + d.getStepZ() * s * 8;
                    int pz = z + d.getStepX() * s * 8;
                    detailBeam(level, box, px, 8, pz, px, 112, pz, 0, rim);
                }
            }
        }

        private void buildShipwrightDetails(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState wood = ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState();
            BlockState frame = ModBlocks.SHIP_TIMBER.get().defaultBlockState();
            BlockState copper = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            BlockState lamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
            for (int mast = 0; mast < 3; mast++) {
                int mastX = mast == 0 ? 34 : mast == 1 ? 64 : 94;
                int top = mast == 0 ? 98 : mast == 1 ? 123 : 108;
                detailBeam(level, box, mastX, 13, c, mastX, top, c, 1, frame);
                int tiers = mast == 1 ? 3 : 2;
                for (int tier = 0; tier < tiers; tier++) {
                    int base = (mast == 0 ? 47 : mast == 1 ? 54 : 57) + tier * 22;
                    int half = (mast == 1 ? 20 : 17) - tier * 4;
                    detailBeam(level, box, mastX, base + 17, c - half - 2, mastX, base + 17, c + half + 2, 0, frame);
                    for (int h = 0; h <= 16; h++) {
                        int spread = half - (int) Math.round((16 - h) * 0.22);
                        for (int w = -spread; w <= spread; w++) {
                            int foot = (int) Math.round(2 * Math.cos(w * Math.PI / (2 * half + 2)));
                            if (h < foot) continue;
                            int billow = 1 + (int) Math.round(6 * Math.sin(Math.PI * h / 17)
                                    * Math.cos(w * Math.PI / (2 * half + 2)));
                            boolean emblem = mast == 1 && tier == 0 && Math.abs(w) + Math.abs(h - 9) <= 5
                                    && Math.abs(w) + Math.abs(h - 9) >= 3;
                            BlockState cloth = Math.abs(w) == spread ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                                    : emblem ? Blocks.BLUE_WOOL.defaultBlockState()
                                    : ModBlocks.WEATHERED_SAILCLOTH.get().defaultBlockState();
                            this.placeBlock(level, cloth, mastX + billow, base + h, c + w, box);
                        }
                    }
                    for (int side : new int[]{-1, 1}) {
                        detailBeam(level, box, mastX + 1, base + 1, c + side * (half - 4),
                                mastX - 3, base - 6, c + side * (half - 1), 0, frame);
                    }
                }
                for (int side : new int[]{-1, 1}) {
                    detailBeam(level, box, mastX, top - 8, c, mastX + side * 10, 29, c + side * 20, 0, frame);
                }
                detailGallery(level, box, mastX, top - 9, c, 4, 2, wood, frame);
                for (int flag = 1; flag <= 9; flag++) {
                    for (int h = 0; h <= 3 - flag / 3; h++) {
                        this.placeBlock(level, Blocks.BLUE_WOOL.defaultBlockState(), mastX - flag, top - 1 - h, c, box);
                    }
                }
            }
            for (int x = 94; x <= 113; x++) {
                for (int z = c - 9; z <= c + 9; z++) {
                    for (int y = 29; y <= 41; y++) {
                        if (y == 35 || y == 41 || x == 94 || x == 113 || Math.abs(z - c) == 9) {
                            boolean window = y >= 31 && y <= 33 || y >= 37 && y <= 39;
                            this.placeBlock(level, window && (x % 4 != 0) ? Blocks.GRAY_STAINED_GLASS.defaultBlockState()
                                    : y == 35 || y == 41 ? copper : frame, x, y, z, box);
                        }
                    }
                }
            }
            for (int y = 42; y <= 47; y++) {
                int half = 10 - (y - 42);
                for (int x = 92; x <= 114; x++) {
                    for (int z = c - half; z <= c + half; z++) {
                        if (Math.abs(z - c) >= half - 1) this.placeBlock(level,
                                ModBlocks.TARRED_SHIP_PLANKS.get().defaultBlockState(), x, y, z, box);
                    }
                }
            }
            buildStairCornice(level, box, 103, 41, c, Footprint.of(10, 9),
                    Blocks.DARK_OAK_STAIRS.defaultBlockState(), Blocks.DARK_OAK_SLAB.defaultBlockState(), true);
            for (int x = 15; x <= 30; x++) {
                int half = Math.min(8, shipHalfWidth(x, 28));
                for (int z = c - half; z <= c + half; z++) {
                    this.placeBlock(level, wood, x, 35, z, box);
                    if (Math.abs(z - c) == half) detailBeam(level, box, x, 29, z, x, 35, z, 0, frame);
                }
            }
            detailBeam(level, box, 18, 28, c, 2, 44, c, 0, frame);
            detailBeam(level, box, 2, 44, c, 34, 89, c, 0, frame);
            detailBeam(level, box, 94, 101, c, 120, 30, c, 0, frame);
            for (int x : new int[]{18, 28, 96, 112}) {
                for (int side : new int[]{-1, 1}) {
                    detailBeam(level, box, x, 29, c + side * 8, x, 34, c + side * 8, 0, frame);
                    this.placeBlock(level, lamp, x, 33, c + side * 8, box);
                }
            }
            for (int y = 30; y <= 33; y++) for (int z = c - 2; z <= c + 2; z++) {
                this.placeBlock(level, Blocks.AIR.defaultBlockState(), 94, y, z, box);
            }
            for (int y : new int[]{35, 41}) {
                for (int x = 99; x <= 114; x++) {
                    for (int side : new int[]{-1, 1}) {
                        this.placeBlock(level, wood, x, y, c + side * 11, box);
                        this.placeBlock(level, copper, x, y - 1, c + side * 11, box);
                        this.placeBlock(level, x % 3 == 0 ? frame : Blocks.DARK_OAK_SLAB.defaultBlockState(),
                                x, y + 1, c + side * 12, box);
                    }
                }
            }
            for (int step = 0; step <= 6; step++) {
                for (int width = -1; width <= 1; width++) {
                    this.placeBlock(level, Blocks.SPRUCE_STAIRS.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                            36 - step, 29 + step, c + width, box);
                }
            }
            for (int side : new int[]{-1, 1}) {
                for (int x = 8; x <= 31; x++) {
                    int z = c + side * shipHalfWidth(x, 28);
                    int lift = (int) Math.round(5 * Math.pow((31 - x) / 23.0, 2));
                    for (int y = 29; y <= 30 + lift; y++) {
                        this.placeBlock(level, y == 30 + lift ? copper : frame, x, y, z, box);
                    }
                }
                detailBeam(level, box, 10, 33, c + side * 4, 5, 40, c + side * 2, 0, copper);
                detailBeam(level, box, 5, 40, c + side * 2, 9, 43, c + side * 2, 0, copper);
            }
            for (int x = 65; x <= 74; x++) {
                for (int z = c - 5; z <= c + 5; z++) {
                    this.placeBlock(level, x % 3 == 0 || Math.abs(z - c) == 5 ? frame
                            : ModBlocks.TARRED_SHIP_PLANKS.get().defaultBlockState(), x, 29, z, box);
                }
            }
            detailBeam(level, box, 83, 29, c, 83, 32, c, 1, copper);
            detailBeam(level, box, 79, 32, c, 87, 32, c, 0, frame);
            for (int[] sail : new int[][]{{34, 89, 64, 120, 74}, {64, 99, 94, 105, 65}}) {
                int span = sail[2] - sail[0];
                for (int x = sail[0] + 1; x < sail[2]; x++) {
                    double t = (double) (x - sail[0]) / span;
                    int top = (int) Math.round(sail[1] + (sail[3] - sail[1]) * t);
                    int bottom = (int) Math.round(sail[1] + (sail[4] - sail[1]) * t);
                    for (int y = bottom; y <= top; y++) {
                        int billow = (int) Math.round(3 * Math.sin(Math.PI * t)
                                * Math.sin(Math.PI * (y - bottom) / Math.max(1, top - bottom)));
                        placeIfAir(level, box, x, y, c + billow,
                                Math.abs(y - top) < 2 || Math.abs(y - bottom) < 2
                                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                                        : ModBlocks.WEATHERED_SAILCLOTH.get().defaultBlockState());
                    }
                }
                detailBeam(level, box, sail[0], sail[1], c, sail[2], sail[3], c, 0, frame);
                detailBeam(level, box, sail[0], sail[1], c, sail[2], sail[4], c, 0, frame);
            }
        }

        private void buildClockworkBelfry(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState stone = ModBlocks.CARVED_BELLSTONE.get().defaultBlockState();
            BlockState trim = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
            BlockState brass = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            BlockState dark = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int x = c + d.getStepX() * 23 + d.getStepZ() * 9;
                int z = c + d.getStepZ() * 23 + d.getStepX() * 9;
                for (int y = 8; y <= 95; y++) {
                    int r = y < 74 ? 3 : y < 88 ? 4 : Math.max(1, 4 - (y - 88) / 2);
                    hollowDisk(level, box, x, y, z, r, y % 14 < 2 ? trim : stone);
                }
                this.placeBlock(level, lamp, x, 96, z, box);
                for (int u = -10; u <= 10; u++) {
                    for (int v = -10; v <= 10; v++) {
                        int r2 = u * u + v * v;
                        if (r2 > 100) continue;
                        BlockState face = r2 >= 72 ? brass : Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                        if ((u == 0 && v >= -1 && v <= 7) || (v == -u && u >= 0 && u <= 5)) face = lamp;
                        this.placeBlock(level, face, c + d.getStepX() * 17 + d.getStepZ() * u,
                                73 + v, c + d.getStepZ() * 17 + d.getStepX() * u, box);
                    }
                }
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    int u = (int) Math.round(Math.cos(a) * 8);
                    int v = (int) Math.round(Math.sin(a) * 8);
                    this.placeBlock(level, trim, c + d.getStepX() * 18 + d.getStepZ() * u,
                            73 + v, c + d.getStepZ() * 18 + d.getStepX() * u, box);
                }
                detailLancet(level, box, c, 88, c, d, 12, 4, 13, trim, Blocks.AIR.defaultBlockState());
                detailBeam(level, box, x, 38, z, c + d.getStepX() * 12, 58, c + d.getStepZ() * 12, 1, dark);
            }
            for (int y = 99; y <= 115; y++) {
                int r = y < 103 ? 9 : Math.max(4, 9 - (y - 102) / 2);
                for (int a = 0; a < 360; a++) {
                    if (a >= 34 && a <= 42 + (115 - y) * 2) continue;
                    double angle = a * Math.PI / 180;
                    int x = c + (int) Math.round(Math.cos(angle) * r);
                    int z = c + (int) Math.round(Math.sin(angle) * r);
                    this.placeBlock(level, y < 103 || y == 112
                            ? ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState() : brass, x, y, z, box);
                }
            }
            detailBeam(level, box, c, 97, c, c, 120, c, 0, dark);
            disk(level, box, c, 97, c, 2, ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState());
            detailBeam(level, box, c - 12, 120, c, c + 12, 120, c, 1, trim);
        }

        private int shipHalfWidth(int x, int y) {
            double length = (x - 5) / 118.0;
            double outline = Math.pow(Math.max(0, Math.sin(length * Math.PI)), 0.65) * 26;
            double section = y <= 12 ? Math.sqrt((y + 2) / 14.0) : 1.0 - (y - 12) * 0.004;
            return (int) Math.round(outline * section);
        }

        private void buildCragLayer(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                                    int radius, int phase, BlockState rock) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!box.isInside(getWorldX(x + dx, z + dz), getWorldY(y), getWorldZ(x + dx, z + dz))) continue;
                    double angle = Math.atan2(dz, dx);
                    double edge = Math.max(1, radius - 1.5 + Math.sin(angle * 5 + phase) * 1.1
                            + Math.sin(angle * 3 - y / 13.0) * 0.7);
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance <= edge && distance >= Math.max(0, edge - 3)) {
                        this.placeBlock(level, rock, x + dx, y, z + dz, box);
                    }
                }
            }
        }

        private void detailBeam(WorldGenLevel level, BoundingBox box,
                                int x0, int y0, int z0, int x1, int y1, int z1, int thickness, BlockState material) {
            int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0))));
            if (material.hasProperty(RotatedPillarBlock.AXIS)) {
                Direction.Axis axis = Math.abs(y1 - y0) >= Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0))
                        ? Direction.Axis.Y : Math.abs(x1 - x0) >= Math.abs(z1 - z0) ? Direction.Axis.X : Direction.Axis.Z;
                material = material.setValue(RotatedPillarBlock.AXIS, axis);
            }
            for (int step = 0; step <= steps; step++) {
                double t = (double) step / steps;
                int x = (int) Math.round(x0 + (x1 - x0) * t);
                int y = (int) Math.round(y0 + (y1 - y0) * t);
                int z = (int) Math.round(z0 + (z1 - z0) * t);
                for (int dx = -thickness; dx <= thickness; dx++) {
                    for (int dz = -thickness; dz <= thickness; dz++) {
                        if (dx * dx + dz * dz <= thickness * thickness + 1) {
                            placeCorniceBlock(level, box, material, x + dx, y, z + dz);
                        }
                    }
                }
            }
        }

        private void detailGallery(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                                   int outer, int inner, BlockState floor, BlockState rim) {
            for (int dx = -outer; dx <= outer; dx++) {
                for (int dz = -outer; dz <= outer; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > outer * outer || d2 < inner * inner) continue;
                    placeCorniceBlock(level, box, floor, x + dx, y, z + dz);
                    if (d2 >= (outer - 1) * (outer - 1)) {
                        placeCorniceBlock(level, box, rim, x + dx, y - 1, z + dz);
                        placeCorniceBlock(level, box, rim, x + dx, y + 1, z + dz);
                    }
                }
            }
        }

        private void detailLancet(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                                  Direction outward, int distance, int halfWidth, int height,
                                  BlockState frame, BlockState pane) {
            for (int h = 0; h <= height; h++) {
                int half = h < height - halfWidth ? halfWidth : height - h;
                for (int w = -half; w <= half; w++) {
                    for (int depth = distance - 2; depth <= distance + 1; depth++) {
                        boolean edge = Math.abs(w) == half || h == 0;
                        placeCorniceBlock(level, box, edge ? frame : pane,
                                x + outward.getStepX() * depth + outward.getStepZ() * w, y + h,
                                z + outward.getStepZ() * depth + outward.getStepX() * w);
                    }
                }
            }
        }

        private void buildSilentBoatCargo(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            // 下层船壳向龙骨收窄；货架须落在舱内甲板，并让开旋梯和中央通道。
            int[][] racks = {{27, -6}, {27, -4}, {42, -7}, {49, -7},
                    {42, -5}, {49, -5}, {74, -7}, {74, -5}};
            for (int[] rack : racks) {
                for (int width = 0; width < 7; width++) {
                    BlockState furnishing = width == 2 ? Blocks.CRAFTING_TABLE.defaultBlockState()
                            : Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,
                                    localFurnitureFacing(Direction.NORTH));
                    this.placeBlock(level, furnishing, rack[0] + width, 1, c + rack[1], box);
                }
            }
        }

        private void buildWonderInteriors(WorldGenLevel level, BoundingBox box) {
            int c = kind.size / 2;
            BlockState air = Blocks.AIR.defaultBlockState();
            switch (kind) {
                case ECHO_WORLD_TREE -> {
                    BlockState wood = ModBlocks.ECHO_PLANKS.get().defaultBlockState();
                    BlockState beam = ModBlocks.ECHO_LOG.get().defaultBlockState();
                    for (int y : new int[]{4, 16, 28, 40, 52, 64, 76, 88}) {
                        int radius = y < 18 ? 11 : y < 38 ? 9 : y < 58 ? 7 : y < 76 ? 5 : 4;
                        disk(level, box, c, y, c, radius, wood);
                        if (radius >= 7) {
                            for (int side : new int[]{-1, 1}) {
                                for (int x = c - 4; x <= c + 4; x++) {
                                    architecturalPillar(level, box, x, y + 1, c + side * (radius - 2),
                                            x % 3 == 0 ? 4 : 2, x % 3 == 0 ? beam : Blocks.BOOKSHELF.defaultBlockState());
                                }
                                this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                                        .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(side < 0 ? Direction.EAST : Direction.WEST)),
                                        c + side * 5, y + 1, c, box);
                            }
                        }
                        this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c, y + 5, c, box);
                        architecturalPillar(level, box, c, y + 6, c, 6, beam);
                    }
                    for (int y = 2; y <= 7; y++) {
                        for (int z = c + 4; z <= c + 15; z++) {
                            for (int x = c - 1; x <= c + 1; x++) {
                                this.placeBlock(level, y == 2 ? wood : air, x, y, z, box);
                            }
                        }
                    }
                    disk(level, box, c, 96, c, 4, wood);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        for (int distance = 0; distance <= 14; distance++) {
                            for (int width = -1; width <= 1; width++) {
                                int x = c + direction.getStepX() * distance + direction.getStepZ() * width;
                                int z = c + direction.getStepZ() * distance + direction.getStepX() * width;
                                this.placeBlock(level, wood, x, 28, z, box);
                                for (int h = 1; h <= 4; h++) {
                                    this.placeBlock(level, air, x, 28 + h, z, box);
                                }
                            }
                        }
                    }
                    this.placeBlock(level, beam, c + 1, 99, c + 1, box);
                    this.placeBlock(level, air, c + 1, 97, c + 1, box);
                    this.placeBlock(level, ModBlocks.WORLDTREE_FRUIT.get().defaultBlockState(),
                            c + 1, 98, c + 1, box);
                    for (int[] room : new int[][]{{4,7,2},{16,-7,5},{28,6,0},{40,-5,0},{52,5,3}}) {
                        wonderFurnishing(level, box, c + (room[0] == 4 ? 7 : 0), room[0], c + (room[0] == 4 ? 0 : room[1]), 0, room[2]);
                        if (room[0] == 40) {
                            // 椅背与主旋梯错开一格，防止原版邻块更新将座椅合成楼梯外角。
                            this.placeBlock(level, air, c - 2, room[0] + 1, c + room[1] + 1, box);
                            this.placeBlock(level, orientStairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.SOUTH, Half.BOTTOM),
                                    c - 3, room[0] + 1, c + room[1] + 1, box);
                        }
                    }
                    for (int y : new int[]{64,76,88,96}) {
                        wonderLanding(level, box, c, y, c, 0, y / 12);
                    }
                    buildWalkableSpiral(level, box, c, 1, c, 96, 2, wood,
                            ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState());
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    BlockState floor = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState rib = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState lamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                    for (int y : new int[]{2, 16, 31, 46, 61, 76, 91, 101}) {
                        int radius = y < 18 ? 21 : y < 44 ? 18 : y < 72 ? 15 : 12;
                        disk(level, box, c, y, c, radius, floor);
                        for (int side : new int[]{-1, 1}) {
                            for (int z = c - 6; z <= c + 6; z++) {
                                this.placeBlock(level, rib, c + side * 7, y + 5, z, box);
                                if (Math.abs(z - c) == 6) {
                                    architecturalPillar(level, box, c + side * 7, y + 1, z, 4, rib);
                                }
                            }
                            for (int z = c - 3; z <= c + 3; z += 3) {
                                this.placeBlock(level, y % 2 == 0 ? Blocks.BOOKSHELF.defaultBlockState()
                                        : Blocks.CUT_COPPER.defaultBlockState(), c + side * (radius - 2), y + 1, z, box);
                            }
                            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), c + side * 8, y + 1, c + 3, box);
                            this.placeBlock(level, lamp, c + side * 7, y + 4, c, box);
                        }
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                            for (int h = 2; h <= 4; h++) {
                                for (int depth = radius; depth <= radius + 3; depth++) {
                                    this.placeBlock(level, Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                                            c + direction.getStepX() * depth, y + h,
                                            c + direction.getStepZ() * depth, box);
                                }
                            }
                        }
                    }
                    for (int[] room : new int[][]{{2,-10,-10,0},{2,-5,12,2},{2,-12,0,4},
                            {16,0,10,1},{16,-10,1,0},{31,9,9,3},{31,-10,7,5},
                            {46,-10,-3,0},{46,0,-10,4},{61,5,9,4},{61,-10,1,0},
                            {76,0,8,5},{76,-7,4,0},{91,0,7,3},{91,6,0,0},{101,0,7,4},{101,6,0,3}}) {
                        wonderFurnishing(level, box, c + room[1], room[0], c + room[2], 1, room[3]);
                    }
                    buildWalkableSpiral(level, box, c - 4, 1, c - 4, 103, 3, floor,
                            ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState());
                    for (int floorY : new int[]{2, 31}) {
                        for (int z = c + 3; z <= c + 26; z++) {
                            for (int x = c - 2; x <= c + 2; x++) {
                                this.placeBlock(level, floor, x, floorY, z, box);
                                for (int h = 1; h <= 4; h++) {
                                    this.placeBlock(level, air, x, floorY + h, z, box);
                                }
                            }
                        }
                    }
                }
                case MIRROR_SEA -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState tile = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
                    ellipsoid(level, box, c, 65, c, 28, 8, 28, air);
                    disk(level, box, c, 60, c, 25, mirror);
                    for (int side : new int[]{-1, 1}) {
                        for (int z = c - 8; z <= c + 8; z += 8) {
                            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), c + side * 14, 61, z, box);
                            this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(side < 0 ? Direction.EAST : Direction.WEST)),
                                    c + side * 17, 61, z, box);
                            this.placeBlock(level, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState(), c + side * 14, 66, z, box);
                        }
                    }
                    for (int side : new int[]{-1, 1}) {
                        int z = c + side * 18;
                        for (int x = c - 25; x <= c + 25; x++) {
                            for (int width = -3; width <= 3; width++) {
                                this.placeBlock(level, width == 0 ? tile : mirror, x, 12, z + width, box);
                                if (Math.abs(width) == 3) {
                                    this.placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), x, 13, z + width, box);
                                }
                            }
                            if (Math.floorMod(x - c, 8) == 0) {
                                architecturalPillar(level, box, x, 2, z, 10, mirror);
                                this.placeBlock(level, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState(), x, 11, z, box);
                            }
                        }
                        for (int x = c - 18; x <= c + 18; x += 12) {
                            platform(level, box, x - 3, 13, z - 2, 5, tile);
                            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), x, 14, z + side, box);
                        }
                    }
                    for (int y : new int[]{12, 28, 44, 60}) {
                        disk(level, box, c, y, c, 8, mirror);
                        for (int side : new int[]{-1, 1}) {
                            architecturalPillar(level, box, c + side * 7, y, c, 16, tile);
                            this.placeBlock(level, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState(), c + side * 6, y + 4, c, box);
                        }
                    }
                    for (int z = c - 20; z <= c + 20; z++) {
                        for (int x = c - 2; x <= c + 2; x++) {
                            this.placeBlock(level, mirror, x, 12, z, box);
                        }
                    }
                    for (int y : new int[]{12,28,44}) wonderLanding(level, box, c, y, c, 2, y / 12);
                    for (int[] room : new int[][]{{-10,12,5},{0,-15,3},{12,-12,0},{0,16,5},{-16,3,0}}) {
                        wonderFurnishing(level, box, c + room[0], 60, c + room[1], 2, room[2]);
                    }
                    buildWalkableSpiral(level, box, c, 2, c, 61, 3, mirror,
                            ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState());
                }
                case INVERTED_MOUNTAINS -> {
                    BlockState wind = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                    BlockState cliff = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
                    for (int y : new int[]{16, 40, 64, 88, 106}) {
                        disk(level, box, c, y, c, 8, wind);
                        for (int side : new int[]{-1, 1}) {
                            for (int z = c; z != c + side * 43; z += side) {
                                for (int width = -2; width <= 2; width++) {
                                    this.placeBlock(level, wind, c + width, y, z, box);
                                    for (int h = 1; h <= 3; h++) {
                                        this.placeBlock(level, air, c + width, y + h, z, box);
                                    }
                                }
                            }
                            int radius = Math.max(2, Math.min(19, (y - 8) / 5 - 1));
                            disk(level, box, c, y, c + side * 42, radius, wind);
                            this.placeBlock(level, Blocks.STONECUTTER.defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.NORTH)), c, y + 1, c + side * 43, box);
                            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), c, y + 4, c + side * 42, box);
                            architecturalPillar(level, box, c + side * 7, y, c, 18, cliff);
                        }
                    }
                    for (int y : new int[]{16,40,64,88,106}) wonderLanding(level, box, c, y, c, 3, y / 16);
                    for (int[] room : new int[][]{{40,42,4},{64,-42,1},{88,42,3},{106,-42,5},{106,42,0}}) {
                        wonderFurnishing(level, box, c + (room[0] == 40 ? 0 : 6), room[0], c + room[1], 3, room[2]);
                    }
                    buildWalkableSpiral(level, box, c, 0, c, 120, 4, wind,
                            ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState());
                }
                case SKY_RIFT -> {
                    BlockState wind = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    for (int[] tower : new int[][]{{-34, 0}, {34, 0}, {0, -34}, {0, 34}}) {
                        int x = c + tower[0];
                        int z = c + tower[1];
                        for (int y : new int[]{6, 18, 34, 50, 68, 84, 102}) {
                            platform(level, box, x - 5, y, z - 5, 11, wind);
                            this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), x, y + 1, z + 4, box);
                            this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.SOUTH)),
                                    x, y + 1, z - (y == 34 && tower[0] != 0 ? 5 : 4), box);
                            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), x, y + 5, z, box);
                            for (int side : new int[]{-1, 1}) {
                                architecturalPillar(level, box, x + side * 5, y + 1, z + 5, 4, trim);
                            }
                        }
                        for (int y : new int[]{6, 34, 68, 102}) {
                            for (int distance = -11; distance <= 11; distance++) {
                                for (int width = -1; width <= 1; width++) {
                                    int px = tower[0] == 0 ? x + width : x + distance;
                                    int pz = tower[0] == 0 ? z + distance : z + width;
                                    this.placeBlock(level, wind, px, y, pz, box);
                                    for (int h = 1; h <= 3; h++) {
                                        this.placeBlock(level, air, px, y + h, pz, box);
                                    }
                                }
                            }
                        }
                        for (int y : new int[]{6,18,34,50,68,84,102}) {
                            wonderLanding(level, box, x, y, z, 4, y / 16 + (tower[0] == 0 ? 1 : 0));
                        }
                        buildWalkableSpiral(level, box, x, 6, z, 108, 3, wind,
                                ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState());
                    }
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        for (int distance = 0; distance <= 35; distance++) {
                            for (int width = -2; width <= 2; width++) {
                                this.placeBlock(level, wind, c + direction.getStepX() * distance
                                        + direction.getStepZ() * width, 6,
                                        c + direction.getStepZ() * distance + direction.getStepX() * width, box);
                            }
                        }
                    }
                    this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 6, c, box);
                }
                case SILENT_GREAT_BOAT -> {
                    BlockState plank = ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState();
                    BlockState log = ModBlocks.SHIP_TIMBER.get().defaultBlockState();
                    BlockState lamp = ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
                    for (int y : new int[]{1, 13}) {
                        for (int x : new int[]{40, 56, 72, 88}) {
                            for (int z = c - 14; z <= c + 14; z++) {
                                if (Math.abs(z - c) > 3) {
                                    architecturalPillar(level, box, x, y, z, 7, z % 5 == 0 ? log : plank);
                                }
                                this.placeBlock(level, log, x, y + 7, z, box);
                            }
                            for (int side : new int[]{-1, 1}) {
                                if (y == 13) {
                                    for (int width = 0; width < 7; width++) {
                                        this.placeBlock(level, Blocks.DARK_OAK_SLAB.defaultBlockState()
                                                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), x + 3 + width, y, c + side * 12, box);
                                        this.placeBlock(level, Blocks.BLUE_CARPET.defaultBlockState(), x + 3 + width, y + 1, c + side * 12, box);
                                    }
                                    this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), x + 5, y, c + side * 12, box);
                                }
                                this.placeBlock(level, lamp, x + 7, y + 5, c + side * 10, box);
                            }
                        }
                    }
                    for (int side : new int[]{-1, 1}) {
                        for (int y = 1; y <= 44; y++) {
                            this.placeBlock(level, log, c + side * 20, y, c - 15, box);
                        }
                    }
                    for (int[] room : new int[][]{{48,0,8,4},{64,0,-8,2},{80,0,8,4},{96,0,-8,2},
                            {48,12,8,1},{64,12,-8,1},{64,12,8,0},{96,12,8,3},
                            {58,28,9,3},{77,28,-9,2},{22,35,0,3},{108,35,-3,1},
                            {98,35,4,0},{108,41,0,3},{98,41,4,4}}) {
                        wonderFurnishing(level, box, room[0], room[1], c + room[2], 5, room[3]);
                    }
                    buildWalkableSpiral(level, box, c - 17, 0, c - 6, 44, 3, plank,
                            Blocks.SPRUCE_STAIRS.defaultBlockState());
                    buildWalkableSpiral(level, box, 27, 28, c, 35, 2, plank,
                            Blocks.SPRUCE_STAIRS.defaultBlockState());
                    buildWalkableSpiral(level, box, 101, 28, c - 2, 41, 2, plank,
                            Blocks.SPRUCE_STAIRS.defaultBlockState());
                    for (int step = 0; step <= 24; step++) {
                        for (int width = -2; width <= 2; width++) {
                            int z = c - 49 + step;
                            int y = step / 2;
                            placeCorniceBlock(level, box, step % 2 == 0
                                            ? orientStairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.SOUTH, Half.BOTTOM) : plank,
                                    c + width, y, z);
                            for (int h = 1; h <= 4; h++) {
                                this.placeBlock(level, air, c + width, y + h, z, box);
                            }
                        }
                    }
                    buildSilentBoatCargo(level, box);
                }
                case BROKEN_BELL_TOWER -> {
                    BlockState stone = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState gear = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    BlockState lamp = ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();
                    for (int y : new int[]{6, 20, 34, 48, 62, 76, 90}) {
                        int radius = y < 38 ? 18 : y < 62 ? 16 : y < 84 ? 13 : 10;
                        for (int x = c - radius; x <= c + radius; x++) {
                            for (int z = c - radius; z <= c + radius; z++) {
                                int distance = (x - c) * (x - c) + (z - c) * (z - c);
                                if (distance <= radius * radius && distance >= 25) {
                                    this.placeBlock(level, stone, x, y, z, box);
                                }
                            }
                        }
                        for (int side : new int[]{-1, 1}) {
                            for (int x = c - 4; x <= c + 4; x++) {
                                this.placeBlock(level, gear, x, y + 4, c + side * 7, box);
                                this.placeBlock(level, x % 3 == 0 ? Blocks.NOTE_BLOCK.defaultBlockState()
                                        : orientStairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(),
                                                side < 0 ? Direction.NORTH : Direction.SOUTH, Half.BOTTOM),
                                        x, y + 1, c + side * (radius - 2), box);
                            }
                            architecturalPillar(level, box, c + side * 7, y, c + 6, 12, gear);
                            this.placeBlock(level, lamp, c + side * 7, y + 3, c + 6, box);
                        }
                    }
                    for (int[] room : new int[][]{{6,-11,0},{20,11,4},{34,-11,1},{48,-10,4},{62,9,3},{76,-9,0},{90,-7,4}}) {
                        wonderFurnishing(level, box, c, room[0], c + room[1], 6, room[2]);
                    }
                    for (int[] room : new int[][]{{6,-10,-4,4},{20,10,-4,0},{34,10,-4,3},
                            {48,-10,-4,0},{62,9,-1,4},{76,9,0,4},{90,6,3,3}}) {
                        wonderFurnishing(level, box, c + room[1], room[0], c + room[2], 6, room[3]);
                    }
                    buildWalkableSpiral(level, box, c, 1, c, 108, 5, stone,
                            ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState());
                    for (int y : new int[]{1, 34}) {
                        for (int z = c + 7; z <= c + 25; z++) {
                            for (int x = c - 2; x <= c + 2; x++) {
                                this.placeBlock(level, stone, x, y, z, box);
                                for (int h = 1; h <= 4; h++) {
                                    this.placeBlock(level, air, x, y + h, z, box);
                                }
                            }
                        }
                    }
                }
            }
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
                            floor, wall, rune, ModLootTables.PUZZLE_T4_REGION_CORE);
                    buildSequenceSeal(level, random, box, c - 24, 0, c - 8, Direction.EAST,
                            floor, wall, rune, ModLootTables.T3_PUZZLE);
                    buildLightsOutConstellation(level, random, box, c - 38, 0, c + 38,
                            Direction.NORTH, floor, wall, rune, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c + 18, 1, c - 14,
                            ModEntities.ECHO_WANDERER.get(), floor, rune);
                    this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(),
                            c + 2, 5, c - 3, box);
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState reef = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
                    BlockState tide = ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c + 12, 1, c, Direction.SOUTH,
                            mirror, reef, tide, ModLootTables.PUZZLE_T4_REGION_CORE);
                    this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                            .setValue(cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock.WATERLOGGED, Boolean.FALSE),
                            c + 2, 3, c, box);
                    placeFloodedStructureSpawner(level, random, box, c - 11, 1, c + 9,
                            ModEntities.WATER_SHADOW.get(), reef, mirror);
                    for (int[] node : LIGHTHOUSE_MIRROR_NODES) {
                        this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                                c + node[0], 2, c + node[1], box);
                    }
                }
                case MIRROR_SEA -> {
                    BlockState floor = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                    BlockState wall = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
                    BlockState trim = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 1, c - 27, Direction.EAST,
                            floor, wall, trim, ModLootTables.PUZZLE_MIRROR_LAKE_CACHE);
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

                    buildProjectionRelayGallery(level, random, box, c, 16, c + 18, Direction.SOUTH,
                            floor, wall, trim, ModLootTables.PUZZLE_T4_REGION_CORE);
                    buildStairCornice(level, box, c, 23, c + 25, Footprint.of(5, 8), ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), true);
                    placeStructureSpawner(level, random, box, c + 16, 1, c - 18,
                            ModEntities.WIND_ERODED_SENTINEL.get(), floor, trim);
                }
                case SKY_RIFT -> {
                    BlockState floor = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
                    BlockState wall = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                    BlockState glow = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 4, c + 9, Direction.EAST,
                            floor, wall, glow, ModLootTables.PUZZLE_T5_MINIBOSS_REPEAT);
                    placeStructureSpawner(level, random, box, c - 16, 4, c - 12,
                            ModEntities.CRYSTAL_NOISE_WISP.get(), floor, glow);
                }
                case SILENT_GREAT_BOAT -> {
                    BlockState planks = ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState();
                    BlockState log = ModBlocks.SHIP_TIMBER.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 1, c + 7, Direction.EAST,
                            planks, planks, log, ModLootTables.PUZZLE_SILENT_HUT_CHEST);
                    buildSequenceSeal(level, random, box, c + 24, 1, c - 4, Direction.EAST,
                            planks, planks, log, ModLootTables.T3_PUZZLE);
                    placeStructureSpawner(level, random, box, c - 8, 2, c - 16,
                            ModEntities.SILENT_WALKER.get(), planks, log);
                }
                case BROKEN_BELL_TOWER -> {
                    BlockState ashen = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
                    BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
                    BlockState strata = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();

                    buildProjectionRelayGallery(level, random, box, c, 0, c + 13, Direction.SOUTH,
                            ashen, ashen, rusted, ModLootTables.PUZZLE_BROKEN_ARCHIVE_CHEST);
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
                    this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(),
                            c, 7, c - 7, box);
                }
            }
        }

        // 奖励必须在楼板、旋梯与机关之后放置，避免被后续生成覆盖；落点须连到对应楼层。
        private void addWonderLoot(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            switch (kind) {
                case ECHO_WORLD_TREE -> {
                    buildWonderWorkshop(level, random, box, c + 26, 52, c, 0, ModLootTables.WONDER_T4_REWARD);
                    buildAccessibleChest(level, random, box, c - 8, 5, c - 4, ModLootTables.T3_PUZZLE);
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    buildWonderWorkshop(level, random, box, c + 9, 46, c + 1, 1, ModLootTables.WONDER_LIGHTHOUSE_REWARD);
                }
                case MIRROR_SEA -> {
                    buildWonderWorkshop(level, random, box, c + 13, 60, c - 2, 2, ModLootTables.WONDER_T4_REWARD);
                    buildTrueSightVaultNiche(level, random, box, c - 14, 61, c + 8, Direction.EAST);
                }
                case INVERTED_MOUNTAINS -> {
                    buildWonderWorkshop(level, random, box, c, 88, c - 42, 3, ModLootTables.WONDER_T4_REWARD);
                }
                case SKY_RIFT -> {
                    buildAccessibleChest(level, random, box, c - 34, 35, c + 3, ModLootTables.T5_MINIBOSS_REPEAT);
                    buildWonderWorkshop(level, random, box, c + 34, 68, c + 14, 4, ModLootTables.WONDER_SKY_RIFT_REWARD);
                }
                case SILENT_GREAT_BOAT -> {
                    buildAccessibleChest(level, random, box, c - 12, 1, c, ModLootTables.T4_REGION_CORE);
                    buildAccessibleChest(level, random, box, c + 42, 29, c + 4, ModLootTables.SILENT_HUT_CHEST);
                    buildWonderWorkshop(level, random, box, c + 18, 12, c, 5, ModLootTables.WONDER_T4_REWARD);
                }
                case BROKEN_BELL_TOWER -> {
                    buildAccessibleChest(level, random, box, c - 8, 7, c - 3, ModLootTables.T5_MINIBOSS_REPEAT);
                    buildAccessibleChest(level, random, box, c + 9, 91, c, ModLootTables.T4_REGION_CORE);
                    buildWonderWorkshop(level, random, box, c + 14, 48, c, 6, ModLootTables.WONDER_T4_REWARD);
                    buildTrueSightVaultNiche(level, random, box, c + 9, 77, c - 6, Direction.WEST);
                }
            }
            dressStructure(level, box, kind.id);
        }

        private void buildWonderWorkshop(WorldGenLevel level, RandomSource random, BoundingBox box,
                                          int x, int y, int z, int theme,
                                          net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            BlockState floor = switch (theme) {
                case 0 -> ModBlocks.ECHO_PLANKS.get().defaultBlockState();
                case 1 -> ModBlocks.IVORY_REEF_ASHLAR.get().defaultBlockState();
                case 2 -> ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
                case 3, 4 -> ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
                case 5 -> ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState();
                default -> ModBlocks.CARVED_BELLSTONE.get().defaultBlockState();
            };
            BlockState trim = switch (theme) {
                case 0 -> ModBlocks.ECHO_LOG.get().defaultBlockState();
                case 2 -> ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
                case 3 -> ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
                case 4 -> Blocks.PURPUR_BLOCK.defaultBlockState();
                case 5 -> ModBlocks.SHIP_TIMBER.get().defaultBlockState();
                default -> ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            };
            BlockState lamp = theme == 1 || theme == 2 ? ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState()
                    : ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 9; dz++) {
                    this.placeBlock(level, floor, x + dx, y, z + dz, box);
                    boolean edge = Math.abs(dx) == 6 || dz == -6 || dz == 9;
                    boolean entry = (Math.abs(dx) == 6 && Math.abs(dz) <= 1)
                            || (dz == -6 && Math.abs(dx) <= 1) || (dz == 9 && Math.abs(dx) >= 3 && Math.abs(dx) <= 4);
                    for (int h = 1; h <= 5; h++) {
                        BlockState material = edge && !entry ? (h == 1 || h == 5 ? trim
                                : Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState()) : air;
                        this.placeBlock(level, material, x + dx, y + h, z + dz, box);
                    }
                    this.placeBlock(level, Math.abs(dx) % 3 == 0 ? trim : floor, x + dx, y + 6, z + dz, box);
                }
            }
            for (int side : new int[]{-1, 1}) {
                for (int dz : new int[]{-5, 3, 8}) {
                    this.placeBlock(level, lamp, x + side * 5, y + 4, z + dz, box);
                    this.placeBlock(level, trim, x + side * 5, y + 5, z + dz, box);
                }
                this.placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), x + side * 5, y + 1, z - 4, box);
                this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), x + side * 4, y + 1, z + 4, box);
                this.placeBlock(level, Blocks.SPRUCE_STAIRS.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, side < 0 ? Direction.WEST : Direction.EAST),
                        x + side * 5, y + 1, z + 4, box);
                this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), x + side * 5, y + 1, z + 7, box);
            }
            java.util.List<BlockPos> inputs = new java.util.ArrayList<>();
            int mode = WonderPuzzleLogic.mode(theme);
            int[] initial = WonderPuzzleLogic.initial(theme);
            if (mode == WonderPuzzleLogic.WATER) {
                for (int dx : new int[]{-3, 0, 3}) {
                    for (int h = 1; h <= 3; h++) {
                        this.placeBlock(level, h == 3 ? trim : Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                                x + dx, y + h, z - 5, box);
                    }
                    this.placeBlock(level, lamp, x + dx, y + 4, z - 5, box);
                }
                for (int dx = -4; dx <= 4; dx++) this.placeBlock(level, trim, x + dx, y + 5, z - 5, box);
            } else if (mode == WonderPuzzleLogic.BALANCE) {
                for (int side : new int[]{-1, 1}) {
                    for (int h = 1; h <= 3; h++) {
                        this.placeBlock(level, theme == 5 ? Blocks.BARREL.defaultBlockState()
                                .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)) : floor,
                                x + side * 3, y + h, z - 5, box);
                        this.placeBlock(level, trim, x + side * 4, y + h, z - 5, box);
                    }
                    this.placeBlock(level, Blocks.STONECUTTER.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.NORTH)), x + side * 4, y + 1, z + 6, box);
                }
            } else if (mode == WonderPuzzleLogic.GEARS) {
                for (int side : new int[]{-1, 1}) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int h = 2; h <= 4; h++) {
                            this.placeBlock(level, dx == 0 && h == 3 ? lamp : trim,
                                    x + side * 3 + dx, y + h, z - 5, box);
                        }
                    }
                }
            }
            for (int i = 0; i < initial.length; i++) {
                int dx = mode == WonderPuzzleLogic.OPTICS ? (i % 3 - 1) * 2
                        : mode == WonderPuzzleLogic.WATER ? (i - 1) * 3 : i * 2 - 3;
                int dz = mode == WonderPuzzleLogic.OPTICS ? (i / 3 - 1) * 2 - 1 : -2;
                this.placeBlock(level, trim, x + dx, y, z + dz, box);
                this.placeBlock(level, ModBlocks.WONDER_PUZZLE_DIAL.get().defaultBlockState()
                        .setValue(WonderPuzzleDialBlock.FORM, mode).setValue(WonderPuzzleDialBlock.VALUE, initial[i]),
                        x + dx, y + 1, z + dz, box);
                inputs.add(new BlockPos(this.getWorldX(x + dx, z + dz), this.getWorldY(y + 1), this.getWorldZ(x + dx, z + dz)));
            }
            if (mode == WonderPuzzleLogic.OPTICS) {
                int[] targets = WonderPuzzleLogic.target(theme);
                for (int ray = 0; ray < 2; ray++) {
                    BlockState marker = ray == 0 ? Blocks.ORANGE_STAINED_GLASS.defaultBlockState() : Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                    this.placeBlock(level, marker, x - 4, y + 1, z - 3 + ray * 4, box);
                    int edge = targets[ray] / 3;
                    int offset = (targets[ray] % 3 - 1) * 2;
                    int dx = edge == 0 ? 4 : edge == 2 ? -4 : offset;
                    int dz = edge == 1 ? 3 : edge == 3 ? -5 : offset - 1;
                    this.placeBlock(level, lamp, x + dx, y, z + dz, box);
                    this.placeBlock(level, marker, x + dx, y + 1, z + dz, box);
                }
            }
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            java.util.List<BlockPos> gates = new java.util.ArrayList<>();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = 6; dz <= 9; dz++) {
                    for (int h = 0; h <= 4; h++) {
                        boolean shell = Math.abs(dx) == 2 || dz == 6 || dz == 9 || h == 0 || h == 4;
                        this.placeBlock(level, shell ? anchored : air, x + dx, y + h, z + dz, box);
                        if (dz == 6 && Math.abs(dx) <= 1 && (h == 1 || h == 2)) {
                            gates.add(new BlockPos(this.getWorldX(x + dx, z + dz), this.getWorldY(y + h), this.getWorldZ(x + dx, z + dz)));
                        }
                    }
                }
            }
            this.createChest(level, box, random, x, y + 1, z + 8, loot); // audit-ok: 工坊封印室的入口由谜题与试炼共同开启，不能挖穿外壳。
            this.placeBlock(level, ModBlocks.WONDER_PUZZLE_CORE.get().defaultBlockState(), x, y + 1, z + 4, box);
            configureWonderWorkshop(level, box, x, y, z, theme, inputs, gates);
        }

        private void configureWonderWorkshop(WorldGenLevel level, BoundingBox box, int x, int y, int z, int theme,
                                              java.util.List<BlockPos> inputs, java.util.List<BlockPos> gates) {
            BlockPos corePos = new BlockPos(this.getWorldX(x, z + 4), this.getWorldY(y + 1), this.getWorldZ(x, z + 4));
            if (box.isInside(corePos) && level.getBlockEntity(corePos) instanceof WonderPuzzleCoreBlockEntity core) {
                core.configure(theme, inputs, gates);
                int[][] route = wonderTrialRoute();
                core.bindTrials(java.util.List.of(wonderRoutePos(route[0]), wonderRoutePos(route[1])));
            }
        }

        private BlockState wonderRoomLamp(int theme) {
            return switch (theme) {
                case 1, 2 -> ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();
                case 5 -> ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState();
                case 6 -> ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();
                default -> ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            };
        }

        private void wonderLanding(WorldGenLevel level, BoundingBox box, int x, int y, int z, int theme, int room) {
            BlockState furnishing = switch (Math.floorMod(room, 4)) {
                case 0 -> Blocks.BOOKSHELF.defaultBlockState();
                case 1 -> Blocks.BARREL.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH));
                case 2 -> Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
                default -> Blocks.CRAFTING_TABLE.defaultBlockState();
            };
            int width = theme == 0 ? 1 : 2;
            int depth = theme == 0 ? 3 : theme == 2 || theme == 3 ? 6 : 4;
            for (int side : new int[]{-1, 1}) {
                // 旋梯南侧在这些楼层保留三格头部净空，家具列放在相邻楼板上。
                int furnitureZ = z + (kind == Kind.ECHO_WORLD_TREE && side == 1 && (y == 64 || y == 88) ? 1 : depth);
                if (kind == Kind.ECHO_WORLD_TREE && side == -1 && (y == 64 || y == 88 || y == 96)) {
                    furnitureZ = z + 1;
                }
                if (kind == Kind.SKY_RIFT && side == 1 && y == 50) {
                    furnitureZ = z + 5;
                }
                this.placeBlock(level, furnishing, x + side * width, y + 1, furnitureZ, box);
                this.placeBlock(level, wonderRoomLamp(theme), x + side * width, y + 3, furnitureZ, box);
                this.placeBlock(level, Blocks.POTTED_FERN.defaultBlockState(), x + side * width, y + 2, furnitureZ, box);
            }
        }

        private void wonderFurnishing(WorldGenLevel level, BoundingBox box, int x, int y, int z, int theme, int room) {
            if (theme == 3) {
                wonderPassage(level, box, kind.size / 2, y, z, x, z, ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState());
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int h = 1; h <= 4; h++) {
                    this.placeBlock(level, Blocks.AIR.defaultBlockState(), x + dx, y + h, z + dz, box);
                }
            }
            BlockState timber = theme == 5 ? ModBlocks.SHIP_TIMBER.get().defaultBlockState()
                    : ModBlocks.ECHO_LOG.get().defaultBlockState();
            BlockState seat = orientStairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.SOUTH, Half.BOTTOM);
            for (int side : new int[]{-1, 1}) {
                this.placeBlock(level, timber, x + side * 2, y + 1, z - 2, box);
                this.placeBlock(level, timber, x + side * 2, y + 2, z - 2, box);
                this.placeBlock(level, wonderRoomLamp(theme), x + side * 2, y + 3, z - 2, box);
            }
            switch (room) {
                case 0 -> {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int h = 1; h <= 2; h++) this.placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), x + dx, y + h, z - 2, box);
                    }
                    this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.SOUTH)), x, y + 1, z, box);
                    this.placeBlock(level, seat, x - 2, y + 1, z + 1, box);
                    this.placeBlock(level, seat, x + 2, y + 1, z + 1, box);
                }
                case 1 -> {
                    for (int side : new int[]{-1, 1}) {
                        for (int dz = -1; dz <= 1; dz++) {
                            this.placeBlock(level, dz == -1 ? Blocks.WHITE_WOOL.defaultBlockState() : Blocks.BLUE_WOOL.defaultBlockState(), x + side * 2, y + 1, z + dz, box);
                        }
                        this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                                .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), x + side * 2, y + 1, z + 2, box);
                    }
                }
                case 2 -> {
                    this.placeBlock(level, Blocks.SMOKER.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.SOUTH)), x - 1, y + 1, z - 2, box);
                    this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), x, y + 1, z - 2, box);
                    this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                            .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), x + 1, y + 1, z - 2, box);
                    for (int dx = -1; dx <= 1; dx++) {
                        this.placeBlock(level, ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState(), x + dx, y + 1, z + 1, box);
                        this.placeBlock(level, seat, x + dx, y + 1, z + 2, box);
                    }
                }
                case 3 -> {
                    this.placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), x, y + 1, z, box);
                    this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.EAST)), x - 2, y + 1, z + 1, box);
                    this.placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), x + 1, y + 1, z - 2, box);
                    this.placeBlock(level, Blocks.CYAN_STAINED_GLASS.defaultBlockState(), x, y + 2, z - 2, box);
                    this.placeBlock(level, seat, x, y + 1, z + 2, box);
                }
                case 4 -> {
                    for (int dx = -1; dx <= 1; dx++) {
                        this.placeBlock(level, ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState(), x + dx, y + 1, z - 2, box);
                    }
                    this.placeBlock(level, Blocks.STONECUTTER.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.SOUTH)), x - 1, y + 2, z - 2, box);
                    this.placeBlock(level, Blocks.NOTE_BLOCK.defaultBlockState(), x + 1, y + 2, z - 2, box);
                    this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), x - 2, y + 1, z + 1, box);
                    this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                            .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), x + 2, y + 1, z + 1, box);
                }
                default -> {
                    for (int dx = -1; dx <= 1; dx++) {
                        this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), x + dx, y + 1, z - 2, box);
                        this.placeBlock(level, Blocks.POTTED_FERN.defaultBlockState(), x + dx, y + 2, z - 2, box);
                    }
                    this.placeBlock(level, seat, x - 2, y + 1, z + 1, box);
                    this.placeBlock(level, seat, x + 2, y + 1, z + 1, box);
                    this.placeBlock(level, Blocks.LECTERN.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, localFurnitureFacing(Direction.NORTH)), x, y + 1, z + 2, box);
                }
            }
        }

        private int[][] wonderTrialRoute() {
            return switch (kind) {
                case ECHO_WORLD_TREE -> new int[][]{{39,53,64},{65,77,90},{90,53,68}};
                case ETERNAL_ECHO_LIGHTHOUSE -> new int[][]{{75,17,58},{71,77,65},{73,47,69}};
                case MIRROR_SEA -> new int[][]{{65,29,78},{51,61,56},{77,61,66}};
                case INVERTED_MOUNTAINS -> new int[][]{{65,41,22},{65,65,106},{64,89,26}};
                case SKY_RIFT -> new int[][]{{79,35,30},{79,85,98},{98,69,82}};
                case SILENT_GREAT_BOAT -> new int[][]{{35,1,69},{95,29,60},{82,13,68}};
                case BROKEN_BELL_TOWER -> new int[][]{{54,21,65},{54,63,64},{78,49,68}};
            };
        }

        private BlockPos wonderRoutePos(int[] point) {
            return new BlockPos(this.getWorldX(point[0], point[2]), this.getWorldY(point[1]), this.getWorldZ(point[0], point[2]));
        }

        private void configureWonderTrial(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
            BlockPos pos = wonderRoutePos(new int[]{x, y, z});
            if (!box.isInside(pos) || !(level.getBlockEntity(pos) instanceof WonderTrialBlockEntity trial)) return;
            int[][] route = wonderTrialRoute();
            boolean first = x == route[0][0] && y == route[0][1] && z == route[0][2];
            trial.configure(kind.ordinal(), first ? null : wonderRoutePos(route[0]), wonderRoutePos(route[first ? 1 : 2]));
        }

        private void addWonderExpedition(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState floor = kind == Kind.SILENT_GREAT_BOAT ? ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState()
                    : kind == Kind.ECHO_WORLD_TREE ? ModBlocks.ECHO_PLANKS.get().defaultBlockState()
                    : ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            switch (kind) {
                case ECHO_WORLD_TREE -> {
                    wonderPassage(level, box, c + 5, 52, c, c + 26, c, floor);
                    wonderPassage(level, box, c - 5, 52, c, c - 26, c, floor);
                    wonderPassage(level, box, c, 76, c + 3, c, c + 26, floor);
                    wonderGuardroom(level, random, box, c - 26, 52, c, ModEntities.ECHO_WANDERER.get(), floor);
                    wonderGuardroom(level, random, box, c, 76, c + 26, ModEntities.ECHO_WANDERER.get(), floor);
                }
                case ETERNAL_ECHO_LIGHTHOUSE -> {
                    wonderGuardroom(level, random, box, c + 10, 16, c - 6, ModEntities.MIRROR_DUST_DECOY.get(), floor);
                    wonderGuardroom(level, random, box, c + 6, 76, c + 1, ModEntities.MIRROR_DUST_DECOY.get(), floor);
                }
                case MIRROR_SEA -> {
                    wonderPassage(level, box, c, 28, c + 5, c, c + 14, floor);
                    wonderGuardroom(level, random, box, c, 28, c + 14, ModEntities.MIRROR_DUST_DECOY.get(), floor);
                    wonderGuardroom(level, random, box, c - 14, 60, c - 8, ModEntities.MIRROR_DUST_DECOY.get(), floor);
                }
                case INVERTED_MOUNTAINS -> {
                    wonderGuardroom(level, random, box, c, 40, c - 42, ModEntities.WIND_ERODED_SENTINEL.get(), floor);
                    wonderGuardroom(level, random, box, c, 64, c + 42, ModEntities.WIND_ERODED_SENTINEL.get(), floor);
                }
                case SKY_RIFT -> {
                    wonderPassage(level, box, c + 34, 68, c + 5, c + 34, c + 14, floor);
                    wonderPassage(level, box, c + 5, 34, c - 34, c + 14, c - 34, floor);
                    wonderPassage(level, box, c + 5, 84, c + 34, c + 14, c + 34, floor);
                    wonderGuardroom(level, random, box, c + 14, 34, c - 34, ModEntities.CRYSTAL_NOISE_WISP.get(), floor);
                    wonderGuardroom(level, random, box, c + 14, 84, c + 34, ModEntities.CRYSTAL_NOISE_WISP.get(), floor);
                }
                case SILENT_GREAT_BOAT -> {
                    wonderGuardroom(level, random, box, c - 30, 0, c + 5, ModEntities.SILENT_WALKER.get(), floor);
                    wonderGuardroom(level, random, box, c + 30, 28, c - 4, ModEntities.SILENT_WALKER.get(), floor);
                }
                case BROKEN_BELL_TOWER -> {
                    wonderGuardroom(level, random, box, c - 11, 20, c + 1, ModEntities.BROKEN_BELL_GUARD.get(), floor);
                    wonderGuardroom(level, random, box, c - 11, 62, c, ModEntities.BROKEN_BELL_GUARD.get(), floor);
                }
            }
        }

        private void wonderPassage(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                                    int endX, int endZ, BlockState floor) {
            int length = Math.max(Math.abs(endX - x), Math.abs(endZ - z));
            int dx = Integer.signum(endX - x);
            int dz = Integer.signum(endZ - z);
            for (int i = 0; i <= length; i++) {
                for (int width = -2; width <= 2; width++) {
                    int px = x + i * dx + width * dz;
                    int pz = z + i * dz + width * dx;
                    this.placeBlock(level, floor, px, y, pz, box);
                    for (int h = 1; h <= 4; h++) {
                        this.placeBlock(level, Math.abs(width) == 2 && h == 1 ? floor : Blocks.AIR.defaultBlockState(), px, y + h, pz, box);
                    }
                    if (Math.abs(width) == 2 && i % 6 == 0) {
                        this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), px, y + 2, pz, box);
                    }
                }
            }
        }

        private void wonderGuardroom(WorldGenLevel level, RandomSource random, BoundingBox box,
                                      int x, int y, int z, net.minecraft.world.entity.EntityType<?> entity, BlockState floor) {
            BlockState trim = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    this.placeBlock(level, floor, x + dx, y, z + dz, box);
                    for (int h = 1; h <= 4; h++) {
                        boolean rim = (Math.abs(dx) == 5 || Math.abs(dz) == 5) && Math.abs(dx) > 1 && Math.abs(dz) > 1;
                        this.placeBlock(level, rim && h == 1 ? trim : Blocks.AIR.defaultBlockState(), x + dx, y + h, z + dz, box);
                    }
                }
            }
            this.placeBlock(level, ModBlocks.WONDER_TRIAL.get().defaultBlockState(), x + 1, y + 1, z, box);
            configureWonderTrial(level, box, x + 1, y + 1, z);
            for (int side : new int[]{-1, 1}) {
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), x + side * 4, y + 2, z - 4, box);
                this.placeBlock(level, trim, x + side * 4, y + 1, z - 4, box);
            }
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), x - 4, y + 1, z - 3, box);
            this.placeBlock(level, Blocks.BARREL.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, localFurnitureFacing(Direction.NORTH)), x - 4, y + 1, z - 2, box);
            this.placeBlock(level, orientStairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.SOUTH, Half.BOTTOM),
                    x - 4, y + 1, z + 3, box);
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(), x - 4, y + 2, z + 4, box);
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
                            int radius = Math.max(1, (y - 38) / 7);
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
                    if (kind.size >= 100) break;
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
                    BlockState lamp = ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();
                    buildGabledPavilion(level, box, c, 1, c + 13, Footprint.of(17, 8), 8, 5, rusted, ashen, rusted, lamp);
                    buildStairCornice(level, box, c, 9, c + 13, Footprint.of(17, 8), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
                    buildCornice(level, box, c, 10, c + 13, Footprint.of(17, 8), ashen, ModBlocks.CLOCKWORK_SCRAP_BLOCK.get().defaultBlockState());
                }
            }
            scatterArchitecturalDebris(level, box, kind.id, c, 1, c,
                    ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState(), ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
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

        /** 129x129x128 版本的世界树:根系、树冠和环廊共同占满巨构包络。 */
        private void buildMegaWorldTree(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState root = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            BlockState trunk = ModBlocks.ECHO_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState leaf = ModBlocks.ECHO_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, Boolean.TRUE);
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

            disk(level, box, c, 0, c, 62, root);
            disk(level, box, c, 1, c, 58, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState());
            architecturalRing(level, box, c, 2, c, 61, brick);

            int[][] directions = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            };
            for (int[] direction : directions) {
                buildButtressRoot(level, box, c, c, direction[0], direction[1], trunk);
            }
            for (int[] direction : directions) {
                int axisLength = direction[0] != 0 && direction[1] != 0 ? 51 : 57;
                for (int distance = 3; distance <= axisLength; distance++) {
                    int x = c + direction[0] * distance;
                    int z = c + direction[1] * distance;
                    int width = distance < 18 ? 4 : distance < 38 ? 3 : 2;
                    int y = Math.max(2, 10 - distance / 8);
                    for (int lateral = -width; lateral <= width; lateral++) {
                        int px = direction[0] == 0 ? x + lateral : x;
                        int pz = direction[1] == 0 ? z + lateral : z;
                        this.placeBlock(level, lateral == 0 ? trunk : root, px, y, pz, box);
                        if (distance < 18 && Math.abs(lateral) < width) {
                            this.placeBlock(level, trunk, px, y + 1, pz, box);
                        }
                    }
                }
            }

            for (int y = 2; y <= 98; y++) {
                int radius = y < 18 ? 13 : y < 38 ? 11 : y < 58 ? 9
                        : y < 76 ? 7 : y < 91 ? 5 : 4;
                // 树心从第四层起掏空,让根厅、环廊和高层树冠成为真正可攀登的内部城市。
                if (y <= 3) {
                    disk(level, box, c, y, c, radius, trunk);
                } else {
                    hollowDisk(level, box, c, y, c, radius, trunk);
                }
                if (y % 14 == 0) {
                    architecturalRing(level, box, c, y, c, radius + 2, brick);
                }
            }
            for (int y : new int[]{18, 38, 58, 76, 91}) {
                int radius = y < 38 ? 11 : y < 58 ? 9 : y < 76 ? 7 : 5;
                buildCornice(level, box, c, y, c, Footprint.square(radius), brick, rune);
            }

            int[][] crowns = {
                    {-47, 91, -23, 12, 8, 11}, {46, 94, -20, 13, 9, 12},
                    {-43, 96, 25, 13, 9, 11}, {45, 99, 25, 12, 9, 12},
                    {-25, 103, -42, 14, 9, 11}, {24, 105, 43, 14, 9, 11},
                    {-6, 108, -49, 13, 8, 10}, {7, 110, 49, 13, 8, 10},
                    {-30, 112, 4, 15, 9, 13}, {30, 114, -3, 15, 9, 13},
                    {0, 117, 0, 17, 10, 15}
            };
            for (int[] crown : crowns) {
                BlockState youngLeaf = ModBlocks.WHISPERING_LEAVES.get().defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, true);
                for (int dx = -crown[3]; dx <= crown[3]; dx++) {
                    for (int dz = -crown[5]; dz <= crown[5]; dz++) {
                        double a = Math.atan2(dz, dx);
                        double radius = Math.sqrt((double) dx * dx / (crown[3] * crown[3])
                                + (double) dz * dz / (crown[5] * crown[5]));
                        double fringe = 0.85 + 0.10 * Math.sin(a * 5 + crown[1]) + 0.05 * Math.sin(a * 9);
                        if (radius > fringe) continue;
                        double fullness = Math.sqrt(Math.max(0, 1 - radius * radius / (fringe * fringe)));
                        int crest = (int) Math.round(fullness * (crown[4] + 2 * Math.sin(dx * 0.55) * Math.cos(dz * 0.43)));
                        int underside = -(int) Math.round(fullness * (5 + 2 * Math.cos(a * 3 + crown[1])));
                        for (int dy = underside; dy <= crest; dy++) {
                            int y = Math.min(126, crown[1] + dy);
                            boolean young = dy >= crest - 2 && Math.sin(dx * 0.25 + dz * 0.17 + crown[1]) > 0.8;
                            this.placeBlock(level, young ? youngLeaf : leaf, c + crown[0] + dx, y, c + crown[2] + dz, box);
                        }
                    }
                }
                this.placeBlock(level, lamp, c + crown[0], crown[1] + crown[4],
                        c + crown[2], box);
            }

            int[][] branches = {
                    {c, 47, c, c - 47, 91, c - 23},
                    {c, 50, c, c + 46, 94, c - 20},
                    {c, 54, c, c - 43, 96, c + 25},
                    {c, 58, c, c + 45, 99, c + 25},
                    {c, 63, c, c - 25, 103, c - 42},
                    {c, 67, c, c + 24, 105, c + 43},
                    {c, 73, c, c - 30, 112, c + 4},
                    {c, 78, c, c + 30, 114, c - 3}
            };
            for (int[] branch : branches) {
                buildWorldTreeBranch(level, box, branch[0], branch[1], branch[2],
                        branch[3], branch[4], branch[5]);
            }

            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 55);
                int z = c + (int) Math.round(Math.sin(angle) * 55);
                architecturalPillar(level, box, x, 3, z, 10 + i % 3, brick);
                buildSpireFinial(level, box, x, 13 + i % 3, z, 2, 5, brick, rune, lamp);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildMonumentalBridge(level, box, c, 28, c, direction, 49, 4, 10,
                        brick, root, rune, lamp);
            }

            for (int y = 3; y <= 6; y++) {
                for (int x = c - 2; x <= c + 2; x++) {
                    for (int z = c + 8; z <= c + 14; z++) {
                        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }
            for (int side : new int[]{-1, 1}) {
                architecturalPillar(level, box, c + side * 3, 3, c + 12, 6, root);
                this.placeBlock(level, rune, c + side * 3, 9, c + 12, box);
            }
            buildSpireFinial(level, box, c, 116, c, 4, 10, trunk, rune, lamp);
            buildCanopyCity(level, box);
            int[][] canopyLayers = {
                    {-24, 98, -24, 22, 10, 20}, {24, 100, -24, 22, 10, 20},
                    {-24, 101, 24, 22, 10, 20}, {24, 103, 24, 22, 10, 20},
                    {0, 106, -32, 25, 11, 20}, {0, 108, 32, 25, 11, 20},
                    {-33, 109, 0, 22, 11, 25}, {33, 111, 0, 22, 11, 25},
                    {0, 117, 0, 28, 9, 28}
            };
            for (int[] crown : canopyLayers) {
                ellipsoid(level, box, c + crown[0], crown[1], c + crown[2],
                        crown[3], crown[4], crown[5], leaf);
            }
        }

        /** 灯塔巨构由海上环城、四向灯桥和顶部透光穹冠组成,主体不复用旧灯塔筒身。 */
        private void buildMegaLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState reef = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
            BlockState mirror = ModBlocks.IVORY_REEF_ASHLAR.get().defaultBlockState();
            BlockState tide = ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState();

            disk(level, box, c, 0, c, 62, tide);
            architecturalRing(level, box, c, 1, c, 61, reef);
            detailGallery(level, box, c, 1, c, 62, 58, tide, reef);
            for (int y = 4; y <= 98; y++) {
                int radius = y < 18 ? 23 : y < 44 ? 20 : y < 72 ? 17 : 14;
                hollowDisk(level, box, c, y, c, radius, y < 16 || y % 24 < 3 ? reef : mirror);
                if (y % 12 == 4) {
                    architecturalRing(level, box, c, y, c, radius + 2, tide);
                }
                if (y % 8 == 4) {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        this.placeBlock(level, lamp, c + direction.getStepX() * (radius - 1), y,
                                c + direction.getStepZ() * (radius - 1), box);
                    }
                }
            }
            for (int y : new int[]{20, 44, 68, 92}) {
                int radius = y < 44 ? 21 : y < 68 ? 18 : y < 92 ? 15 : 12;
                buildCornice(level, box, c, y, c, Footprint.square(radius), mirror, tide);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildMonumentalBridge(level, box, c, 31, c, direction, 48, 4, 11,
                        mirror, reef, tide, lamp);
            }

            int[][] towers = {{0, -46}, {46, 0}, {0, 46}, {-46, 0},
                    {32, 32}, {-32, 32}, {32, -32}, {-32, -32}};
            for (int[] tower : towers) {
                int x = c + tower[0];
                int z = c + tower[1];
                if (tower[0] == 0 || tower[1] == 0) {
                    buildTieredTower(level, box, x, 4, z, 5, 26, mirror, reef, lamp);
                    buildSpireFinial(level, box, x, 32, z, 3, 9, reef, mirror, lamp);
                } else {
                    buildGabledPavilion(level, box, x, 3, z, Footprint.of(7, 9), 9, 6,
                            mirror, reef, ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState(), lamp);
                }
            }
            buildDomedRotunda(level, box, c - 30, 5, c + 26, 10, 6,
                    tide, reef, mirror, lamp);
            buildDomedRotunda(level, box, c + 30, 5, c + 26, 10, 6,
                    tide, mirror, reef, lamp);

            buildSpireFinial(level, box, c, 116, c, 7, 10, mirror, tide, lamp);
            buildLighthouseCrown(level, box);
        }

        /** 镜面海巨构以水镜盆地、八座折射塔和玻璃穹庭形成水平展开的奇观城市。 */
        private void buildMegaMirrorSea(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState mirror = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState dust = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState prismBrick = Blocks.PRISMARINE_BRICKS.defaultBlockState();
            BlockState quartz = Blocks.QUARTZ_PILLAR.defaultBlockState();

            disk(level, box, c, 0, c, 62, mirror);
            disk(level, box, c, 1, c, 54, Blocks.WATER.defaultBlockState());
            architecturalRing(level, box, c, 2, c, 60, dust);
            architecturalRing(level, box, c, 3, c, 51, prismBrick);
            for (int y = 5; y <= 20; y += 5) {
                hollowDisk(level, box, c, y, c, 47 - y / 2, y % 10 == 5 ? glass : mirror);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildMonumentalBridge(level, box, c, 12, c, direction, 48, 4, 10,
                        mirror, dust, prismBrick, lamp);
            }

            int[][] towers = {{0, -44}, {44, 0}, {0, 44}, {-44, 0},
                    {31, 31}, {-31, 31}, {31, -31}, {-31, -31}};
            for (int i = 0; i < towers.length; i++) {
                int x = c + towers[i][0];
                int z = c + towers[i][1];
                int high = i < 4 ? 31 : 23;
                for (int y = 4; y <= high; y++) {
                    int r = y < 9 ? 6 : y < high - 6 ? 4 : Math.max(1, (high - y) / 2 + 1);
                    hollowDisk(level, box, x, y, z, r, y % 13 < 2 ? mirror : dust);
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        this.placeBlock(level, y > 10 && y < high - 5 ? glass : quartz,
                                x + side.getStepX() * r, y, z + side.getStepZ() * r, box);
                    }
                }
                buildSpireFinial(level, box, x, high, z, 2, 8, mirror, prismBrick, lamp);
            }
            buildDomedRotunda(level, box, c - 25, 8, c, 13, 7,
                    mirror, dust, glass, lamp);
            buildDomedRotunda(level, box, c + 25, 8, c, 13, 7,
                    mirror, prismBrick, dust, lamp);

            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 54);
                int z = c + (int) Math.round(Math.sin(angle) * 54);
                architecturalPillar(level, box, x, 4, z, 12 + i % 4, dust);
                placeCorniceBlock(level, box, lamp, x, 17 + i % 4, z);
            }
            buildSpireFinial(level, box, c, 99, c, 3, 12, mirror, prismBrick, lamp);
            buildMirrorVaults(level, box);
        }

        /** 倒悬山脉巨构由六座逐层收分的浮空山锥和中央风桥构成,保留大面积天空净空。 */
        private void buildMegaInvertedMountains(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState cliff = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState wind = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState brick = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

            int[][] peaks = {{-36, -22}, {0, -42}, {36, -22}, {-36, 22}, {0, 42}, {36, 22}};
            for (int i = 0; i < peaks.length; i++) {
                int x = c + peaks[i][0];
                int z = c + peaks[i][1];
                int summit = new int[]{116, 109, 119, 104, 117, 111}[i];
                int tip = 7 + i % 3 * 8;
                for (int y = tip; y <= summit; y++) {
                    double t = (double) (y - tip) / (summit - tip);
                    int radius = Math.max(1, (int) Math.round(19 * Math.pow(t, 0.78) + 2 * Math.sin(t * 9 + i)));
                    int driftX = (int) Math.round(Math.sin(t * 3 + i) * 2);
                    int driftZ = (int) Math.round(Math.cos(t * 4 + i) * 2);
                    BlockState rock = y % 19 < 3 ? ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState()
                            : y % 11 < 2 ? ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState()
                            : Blocks.TUFF.defaultBlockState();
                    buildCragLayer(level, box, x + driftX, y, z + driftZ, radius, i, rock);
                }
                for (int dx = -18; dx <= 18; dx++) {
                    for (int dz = -18; dz <= 18; dz++) {
                        double r = Math.sqrt(dx * dx + dz * dz);
                        if (r > 17 + 1.5 * Math.sin(Math.atan2(dz, dx) * 5 + i)) continue;
                        int rise = (int) Math.round((1 - r / 20) * 5 + 2 * Math.sin(dx * 0.5 + i) * Math.cos(dz * 0.4));
                        for (int y = summit - 2; y <= Math.min(126, summit + rise); y++) {
                            this.placeBlock(level, y == summit + rise ? Blocks.TUFF.defaultBlockState() : chiseled, x + dx, y, z + dz, box);
                        }
                    }
                }
                for (int y = tip - 4; y <= tip + 3; y++) {
                    int r = Math.max(0, 2 - Math.abs(y - tip) / 2);
                    disk(level, box, x, y, z, r, lamp);
                }
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildMonumentalBridge(level, box, c, 106, c, direction, 42, 3, 9,
                        wind, cliff, chiseled, lamp);
            }
            for (int[] corner : new int[][]{{-42, -42}, {42, -42}, {-42, 42}, {42, 42}}) {
                buildTieredTower(level, box, c + corner[0], 111, c + corner[1],
                        6, 10, chiseled, wind, lamp);
                buildSpireFinial(level, box, c + corner[0], 123, c + corner[1],
                        3, 4, wind, chiseled, lamp);
            }
            architecturalRing(level, box, c, 108, c, 49, wind);
            architecturalRing(level, box, c, 114, c, 42, chiseled);
            buildDomedRotunda(level, box, c, 104, c, 12, 5,
                    wind, chiseled, brick, lamp);
            buildSpireFinial(level, box, c, 116, c, 5, 9, cliff, wind, lamp);
            buildHangingStrata(level, box);
            detailGallery(level, box, c, 40, c, 8, 6, wind, chiseled);
        }

        /** 天空裂隙巨构采用四座裂门塔、跨空星桥和中央断裂轴,形成可从远处辨认的垂直门廊。 */
        private void buildMegaSkyRift(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState wind = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState brick = ModBlocks.WIND_ETCHED_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();

            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 6, c, box);
            architecturalRing(level, box, c, 4, c, 58, chiseled);
            architecturalRing(level, box, c, 5, c, 49, wind);
            int[][] towers = {{-34, 0}, {34, 0}, {0, -34}, {0, 34}};
            for (int i = 0; i < towers.length; i++) {
                int x = c + towers[i][0];
                int z = c + towers[i][1];
                for (int y = 6; y <= 112; y++) {
                    int r = y < 18 ? 10 : y < 84 ? 8 : 7;
                    hollowDisk(level, box, x, y, z, r, y % 34 < 3 ? chiseled : brick);
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        int px = x + face.getStepX() * r;
                        int pz = z + face.getStepZ() * r;
                        this.placeBlock(level, y % 16 < 12 ? Blocks.GRAY_STAINED_GLASS.defaultBlockState() : lamp,
                                px, y, pz, box);
                    }
                }
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    int endY = 118 + (i + face.ordinal()) % 3 * 4;
                    detailBeam(level, box, x + face.getStepX() * 10, 8, z + face.getStepZ() * 10,
                            x + face.getStepX() * 8, endY, z + face.getStepZ() * 8, 1, chiseled);
                    detailBeam(level, box, x + face.getStepX() * 8, endY, z + face.getStepZ() * 8,
                            x + face.getStepX() * 3, endY - 9, z + face.getStepZ() * 3, 0, amethyst);
                    this.placeBlock(level, lamp, x + face.getStepX() * 8, endY, z + face.getStepZ() * 8, box);
                }
            }
            for (int[] tower : towers) {
                for (int floorY : new int[]{34, 68, 102}) {
                    buildStairCornice(level, box, c + tower[0], floorY, c + tower[1], Footprint.square(8),
                            ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState(),
                            ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState(), false);
                }
            }
            for (int y = 10; y <= 112; y++) {
                int drift = (int) Math.round(Math.sin(y / 9.0D) * 4);
                this.placeBlock(level, amethyst, c - 15 + drift, y, c, box);
                this.placeBlock(level, amethyst, c + 15 - drift, y, c, box);
                if (y % 5 == 0) {
                    this.placeBlock(level, lamp, c - 16 + drift, y, c, box);
                    this.placeBlock(level, lamp, c + 16 - drift, y, c, box);
                }
            }
            for (int y : new int[]{34, 68, 102}) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    buildMonumentalBridge(level, box, c, y, c, direction, 48, 3, 8,
                            brick, wind, chiseled, lamp);
                }
                architecturalRing(level, box, c, y + 2, c, 23, amethyst);
            }
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 51);
                int z = c + (int) Math.round(Math.sin(angle) * 51);
                int y = 16 + i % 4 * 24;
                disk(level, box, x, y, z, 4, wind);
                architecturalRing(level, box, x, y + 1, z, 4, chiseled);
                placeCorniceBlock(level, box, lamp, x, y + 2, z);
            }
            for (int i = 0; i < 360; i++) {
                if (i % 90 > 65) continue;
                double a = i * Math.PI / 180;
                int x = c + (int) Math.round(Math.cos(a) * 50);
                int z = c + (int) Math.round(Math.sin(a) * 50);
                this.placeBlock(level, i % 15 == 0 ? lamp : amethyst, x, 115, z, box);
            }
            buildRiftArmillary(level, box);
        }

        /** 静默巨舟巨构扩展为三层船身、三桅风帆和高空观星冠,长轴保持船形而非方盒。 */
        private void buildMegaSilentBoat(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState plank = ModBlocks.SHIP_DECK_PLANKS.get().defaultBlockState();
            BlockState hull = ModBlocks.TARRED_SHIP_PLANKS.get().defaultBlockState();
            BlockState rib = ModBlocks.SHIP_TIMBER.get().defaultBlockState();
            BlockState copper = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            for (int x = 5; x <= 123; x++) {
                for (int y = 0; y <= 28; y++) {
                    int half = shipHalfWidth(x, y);
                    if (half < 1) continue;
                    for (int z = c - half; z <= c + half; z++) {
                        int edge = Math.abs(z - c);
                        if (y == 0 || y == 12 || y == 28) {
                            this.placeBlock(level, x % 9 == 0 ? rib : plank, x, y, z, box);
                        } else if (edge >= half - 1) {
                            boolean stripe = y == 10 || y == 24;
                            boolean port = x % 9 >= 3 && x % 9 <= 5 && (y >= 5 && y <= 7 || y >= 17 && y <= 20);
                            this.placeBlock(level, stripe ? copper : port ? Blocks.GRAY_STAINED_GLASS.defaultBlockState()
                                    : x % 9 == 0 ? rib : hull, x, y, z, box);
                        }
                    }
                }
                int half = shipHalfWidth(x, 28);
                if (half > 0) {
                    for (int side : new int[]{-1, 1}) {
                        this.placeBlock(level, rib, x, 29, c + side * half, box);
                        this.placeBlock(level, x % 6 == 0 ? copper : Blocks.DARK_OAK_SLAB.defaultBlockState(),
                                x, 30, c + side * half, box);
                    }
                }
            }
            buildShipwrightDetails(level, box);
        }

        private void buildWorldTree(WorldGenLevel level, RandomSource random, BoundingBox box) {
            if (kind.size >= 100) {
                buildMegaWorldTree(level, random, box);
                return;
            }
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

        }

        private void buildLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            if (kind.size >= 100) {
                buildMegaLighthouse(level, random, box);
                return;
            }
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
            if (kind.size >= 100) {
                buildMegaMirrorSea(level, random, box);
                return;
            }
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
            if (kind.size >= 100) {
                buildMegaInvertedMountains(level, random, box);
                return;
            }
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
            if (kind.size >= 100) {
                buildMegaSkyRift(level, random, box);
                return;
            }
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
            if (kind.size >= 100) {
                buildMegaSilentBoat(level, random, box);
                return;
            }
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

        /** 129x129x128 版本的残钟巨塔:中心钟室、四向长桥和外圈冠塔组成完整城市式剪影。 */
        private void buildMegaBellTower(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState body = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
            BlockState rust = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
            BlockState strata = ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState();
            BlockState copper = ModBlocks.RIVETED_BRONZE_CASING.get().defaultBlockState();
            BlockState lamp = ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();

            disk(level, box, c, 0, c, 62, body);
            architecturalRing(level, box, c, 1, c, 61, strata);
            detailGallery(level, box, c, 1, c, 62, 58, body, strata);

            for (int y = 4; y <= 104; y++) {
                int radius = y < 16 ? 22 : y < 38 ? 20 : y < 62 ? 18 : y < 84 ? 15 : 12;
                hollowDisk(level, box, c, y, c, radius, y % 12 == 0 ? strata : body);
                if (y % 12 == 0) {
                    architecturalRing(level, box, c, y, c, radius + 2, rust);
                }
                if (y % 8 == 0) {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        this.placeBlock(level, lamp, c + direction.getStepX() * (radius - 1), y,
                                c + direction.getStepZ() * (radius - 1), box);
                    }
                }
            }

            for (int y : new int[]{20, 42, 64, 86}) {
                int radius = y < 42 ? 20 : y < 64 ? 18 : y < 86 ? 15 : 12;
                buildCornice(level, box, c, y, c, Footprint.square(radius), rust, strata);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildMonumentalBridge(level, box, c, 34, c, direction, 49, 5, 12,
                        rust, body, strata, lamp);
            }

            int[][] towers = {
                    {0, -52}, {52, 0}, {0, 52}, {-52, 0},
                    {37, 37}, {-37, 37}, {37, -37}, {-37, -37}
            };
            for (int i = 0; i < towers.length; i++) {
                int x = c + towers[i][0];
                int z = c + towers[i][1];
                int high = i < 4 ? 32 + i * 3 : 14 + i % 3 * 4;
                for (int y = 3; y <= high; y++) {
                    int r = y < 10 ? 7 : 5;
                    hollowDisk(level, box, x, y, z, r, y % 12 < 2 ? rust : body);
                }
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    detailLancet(level, box, x, 11, z, face, 5, 2, Math.max(3, high - 16),
                            strata, Blocks.AIR.defaultBlockState());
                    int lip = high + (i + face.ordinal()) % 4;
                    detailBeam(level, box, x + face.getStepX() * 5, high - 1, z + face.getStepZ() * 5,
                            x + face.getStepX() * 5, lip, z + face.getStepZ() * 5, 0, rust);
                    this.placeBlock(level, lamp, x + face.getStepX() * 5, lip, z + face.getStepZ() * 5, box);
                }
            }

            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    int x = c + sx * 12;
                    int z = c + sz * 12;
                    int top = sx == sz ? 125 : 118;
                    detailBeam(level, box, x, 91, z, x, top, z, 1, strata);
                    buildSpireFinial(level, box, x, top - 2, z, 2, 3, rust, copper, lamp);
                }
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                for (int u = -12; u <= 12; u++) {
                    int y = 116 + (int) Math.round(7 * Math.sqrt(1 - u * u / 144.0));
                    if (side == Direction.SOUTH && u > 2 && u < 8) continue;
                    detailBeam(level, box, c + side.getStepX() * 12 + side.getStepZ() * u, y,
                            c + side.getStepZ() * 12 + side.getStepX() * u,
                            c + side.getStepX() * 12 + side.getStepZ() * u, y + 1,
                            c + side.getStepZ() * 12 + side.getStepX() * u, 0, copper);
                }
            }
            decorateBellTower(level, box, c);
            buildClockworkBelfry(level, box);
        }

        private void buildBellTower(WorldGenLevel level, RandomSource random, BoundingBox box) {
            if (kind.size >= 100) {
                buildMegaBellTower(level, random, box);
                return;
            }
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
            BoundingBox local = localGenerationBounds(box);
            for (int x = Math.max(cx - radiusX, local.minX()); x <= Math.min(cx + radiusX, local.maxX()); x++) {
                double dx = (double) (x - cx) / radiusX;
                double dxSquared = dx * dx;
                for (int y = Math.max(cy - radiusY, local.minY()); y <= Math.min(cy + radiusY, local.maxY()); y++) {
                    double dy = (double) (y - cy) / radiusY;
                    double crossSection = dxSquared + dy * dy;
                    if (crossSection > 1.0D) {
                        continue;
                    }
                    for (int z = Math.max(cz - radiusZ, local.minZ()); z <= Math.min(cz + radiusZ, local.maxZ()); z++) {
                        double dz = (double) (z - cz) / radiusZ;
                        if (crossSection + dz * dz <= 1.0D) {
                            if (state.isAir()) {
                                placeCorniceBlock(level, box, state, x, y, z);
                            } else {
                                placeIfAir(level, box, x, y, z, state);
                            }
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
            int worldX = this.getWorldX(x, z);
            int worldY = this.getWorldY(y);
            int worldZ = this.getWorldZ(x, z);
            if (!box.isInside(worldX, worldY, worldZ)) {
                return;
            }
            BlockPos worldPos = new BlockPos(worldX, worldY, worldZ);
            if (level.getBlockState(worldPos).isAir()) {
                this.placeBlock(level, state, x, y, z, box);
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z, int height, BlockState state) {
            BoundingBox local = localGenerationBounds(box);
            if (x < local.minX() || x > local.maxX() || z < local.minZ() || z > local.maxZ()) {
                return;
            }
            for (int i = Math.max(0, local.minY() - y); i < Math.min(height, local.maxY() - y + 1); i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0, int size, BlockState state) {
            BoundingBox local = localGenerationBounds(box);
            if (y < local.minY() || y > local.maxY()) {
                return;
            }
            for (int x = Math.max(x0, local.minX()); x < Math.min(x0 + size, local.maxX() + 1); x++) {
                for (int z = Math.max(z0, local.minZ()); z < Math.min(z0 + size, local.maxZ() + 1); z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            BoundingBox local = localGenerationBounds(box);
            if (y < local.minY() || y > local.maxY()) {
                return;
            }
            int r2 = radius * radius;
            for (int x = Math.max(cx - radius, local.minX()); x <= Math.min(cx + radius, local.maxX()); x++) {
                for (int z = Math.max(cz - radius, local.minZ()); z <= Math.min(cz + radius, local.maxZ()); z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    if (dx * dx + dz * dz <= r2) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private void hollowDisk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            BoundingBox local = localGenerationBounds(box);
            if (y < local.minY() || y > local.maxY()) {
                return;
            }
            int outer = radius * radius;
            int inner = Math.max(0, radius - 2) * Math.max(0, radius - 2);
            for (int x = Math.max(cx - radius, local.minX()); x <= Math.min(cx + radius, local.maxX()); x++) {
                for (int z = Math.max(cz - radius, local.minZ()); z <= Math.min(cz + radius, local.maxZ()); z++) {
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
