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
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

/**
 * 中型回响神殿(V0.3 模块化遗迹,主世界生成):
 * 代码版"Jigsaw"——多 Piece 房间池拼装(按 tech-conventions 不用 NBT 模板)。
 * 地表只露一座小入口厅;厅内螺旋梯井下行 8 格,走廊脊、侧房与宝藏房全部埋在地下,
 * 从外部完全看不出房间布局——隐藏房因此真正"隐藏"(走廊里的假墙才是唯一线索)。
 * 布局:入口厅(地表+地下前室)→ 走廊脊 → 三间侧房(战斗房/记录室/隐藏房,槽位随机洗牌)→ 尽头宝藏房。
 * 整体固定朝向(南入口)拼装,避免多 Piece 旋转坐标错位;朝向多样性留待后续版本。
 */
public class EchoTempleStructure extends Structure {
    public static final MapCodec<EchoTempleStructure> CODEC = simpleCodec(EchoTempleStructure::new);

    // 神殿总足迹 23 x 41(x 0..22, z 0..40),所有 Piece 锚点为相对它的局部坐标
    private static final int TOTAL_WIDTH = 23;
    private static final int TOTAL_DEPTH = 41;
    /** 地下部分相对地表的下沉深度。 */
    private static final int SINK_DEPTH = 8;

    public EchoTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ECHO_TEMPLE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
                builder -> generatePieces(builder, context));
    }

    private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        WorldgenRandom random = context.random();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int baseX = centerX - TOTAL_WIDTH / 2;
        int baseZ = centerZ - TOTAL_DEPTH / 2;
        // 全足迹 9 点采样取最低:地下层顶必须低于全场最低地表 2 格,防止房间裸露
        int minAll = Integer.MAX_VALUE;
        int[][] fullSamples = {{0, 0}, {11, 0}, {22, 0}, {0, 20}, {11, 20}, {22, 20}, {0, 40}, {11, 40}, {22, 40}};
        for (int[] s : fullSamples) {
            minAll = Math.min(minAll, sampleSurface(context, baseX + s[0], baseZ + s[1]));
        }
        // 门前区域单独采样:大厅地板贴门口地表,门廊(Piece 南扩 2 格)负责清掉门前地形
        int doorMin = Integer.MAX_VALUE;
        int[][] doorSamples = {{6, -2}, {11, -2}, {16, -2}, {6, 2}, {16, 2}};
        for (int[] s : doorSamples) {
            doorMin = Math.min(doorMin, sampleSurface(context, baseX + s[0], baseZ + s[1]));
        }
        int undergroundY = minAll - SINK_DEPTH;
        // 大厅地板的 Piece 局部高度(= 竖井深度),随门口地势加长,最少 SINK_DEPTH
        int hallFloor = Math.max(SINK_DEPTH, doorMin - undergroundY);

        // 三个侧房槽位(西 z10、东 z14、西 z22)洗牌分配房型
        TemplePiece.Kind[] sideKinds = {TemplePiece.Kind.COMBAT, TemplePiece.Kind.ARCHIVE, TemplePiece.Kind.HIDDEN};
        for (int i = sideKinds.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            TemplePiece.Kind tmp = sideKinds[i];
            sideKinds[i] = sideKinds[j];
            sideKinds[j] = tmp;
        }

        // 入口 Piece 含门廊(南扩 2 格)并跨地下前室到地表大厅;其余全部在地下层
        builder.addPiece(new TemplePiece(TemplePiece.Kind.ENTRANCE, baseX + 6, undergroundY, baseZ - 2, hallFloor + 7));
        builder.addPiece(new TemplePiece(TemplePiece.Kind.CORRIDOR, baseX + 9, undergroundY, baseZ + 8));
        builder.addPiece(new TemplePiece(sideKinds[0].asWest(), baseX, undergroundY, baseZ + 10));
        builder.addPiece(new TemplePiece(sideKinds[1].asEast(), baseX + 13, undergroundY, baseZ + 14));
        builder.addPiece(new TemplePiece(sideKinds[2].asWest(), baseX, undergroundY, baseZ + 22));
        builder.addPiece(new TemplePiece(TemplePiece.Kind.TREASURE, baseX + 6, undergroundY, baseZ + 33));
    }

    private static int sampleSurface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z,
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    public static class TemplePiece extends EchoStructurePiece {

        /** 房型 + 朝向变体:WEST/EAST 表示侧房在走廊哪一侧(门开在对应墙上)。 */
        public enum Kind {
            ENTRANCE(11, 15, 10),
            CORRIDOR(5, 7, 25),
            COMBAT(10, 7, 11), COMBAT_E(10, 7, 11),
            ARCHIVE(10, 7, 11), ARCHIVE_E(10, 7, 11),
            HIDDEN(10, 7, 11), HIDDEN_E(10, 7, 11),
            TREASURE(11, 7, 8);

            final int width, height, depth;

            Kind(int width, int height, int depth) {
                this.width = width;
                this.height = height;
                this.depth = depth;
            }

            Kind asWest() {
                return this;
            }

            Kind asEast() {
                return switch (this) {
                    case COMBAT -> COMBAT_E;
                    case ARCHIVE -> ARCHIVE_E;
                    case HIDDEN -> HIDDEN_E;
                    default -> this;
                };
            }

            boolean isEastSide() {
                return this == COMBAT_E || this == ARCHIVE_E || this == HIDDEN_E;
            }
        }

        private final Kind kind;

        public TemplePiece(Kind kind, int x, int y, int z) {
            this(kind, x, y, z, kind.height);
        }

        /** heightOverride:入口 Piece 的竖井深度随门口地势变化,高度由 generatePieces 计算。 */
        public TemplePiece(Kind kind, int x, int y, int z, int heightOverride) {
            super(ModStructures.ECHO_TEMPLE_PIECE.get(), x, y, z,
                    kind.width, heightOverride, kind.depth, Direction.SOUTH);
            this.kind = kind;
        }

        public TemplePiece(CompoundTag tag) {
            super(ModStructures.ECHO_TEMPLE_PIECE.get(), tag);
            this.kind = Kind.valueOf(tag.getString("Kind"));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("Kind", this.kind.name());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            switch (this.kind) {
                case ENTRANCE -> buildEntrance(level, box, random);
                case CORRIDOR -> buildCorridor(level, box, random);
                case TREASURE -> buildTreasure(level, box, random);
                case COMBAT, COMBAT_E -> buildCombat(level, box, random);
                case ARCHIVE, ARCHIVE_E -> buildArchive(level, box, random);
                case HIDDEN, HIDDEN_E -> buildHidden(level, box, random);
            }
        }

        private BlockState bricks(RandomSource random) {
            return random.nextFloat() < 0.25F
                    ? ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState()
                    : ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
        }

        /** 通用房壳:地板 + 顶板 + 四面墙(墙体留洞由各房型自己开)。 */
        private void buildShell(WorldGenLevel level, BoundingBox box, RandomSource random) {
            int w = this.kind.width, h = this.kind.height, d = this.kind.depth;
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, bricks(random), x, 0, z, box);
                    this.placeBlock(level, bricks(random), x, h - 1, z, box);
                }
            }
            for (int y = 1; y < h - 1; y++) {
                for (int x = 0; x < w; x++) {
                    this.placeBlock(level, bricks(random), x, y, 0, box);
                    this.placeBlock(level, bricks(random), x, y, d - 1, box);
                }
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, bricks(random), 0, y, z, box);
                    this.placeBlock(level, bricks(random), w - 1, y, z, box);
                }
            }
            BlockState air = Blocks.AIR.defaultBlockState();
            this.generateBox(level, box, 1, 1, 1, w - 2, h - 2, d - 2, air, air, false);
        }

        /**
         * 入口 Piece(跨层,南扩 2 格门廊,高度随门口地势变化):
         * 下层 y0..3 地下前室(北门通走廊)| 实心夹层 | hallFloor 大厅地板 | 上方地表大厅 + 厅顶。
         * 局部 z:0..1 门廊(清掉门前地形)| 2 南墙(大门)| 3..8 大厅 | 9 北墙。
         * 大厅东北角 2x2 梯井(整砖螺旋步)贯穿夹层连通上下。
         */
        private void buildEntrance(WorldGenLevel level, BoundingBox box, RandomSource random) {
            int w = this.kind.width, d = this.kind.depth;
            // 大厅地板局部高度 = 包围盒总高 - 大厅净空 7(墙 5 + 顶 1 + 地板层)
            int hallFloor = this.boundingBox.getYSpan() - 7;
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();

            // 地下前室:地板 + 四壁(南墙在 z1,z0 门廊正下方保持实心)+ 内部清空
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, bricks(random), x, 0, z, box);
                }
            }
            for (int y = 1; y <= 3; y++) {
                for (int x = 0; x < w; x++) {
                    this.placeBlock(level, bricks(random), x, y, 1, box);
                    this.placeBlock(level, bricks(random), x, y, d - 1, box);
                }
                for (int z = 1; z < d; z++) {
                    this.placeBlock(level, bricks(random), 0, y, z, box);
                    this.placeBlock(level, bricks(random), w - 1, y, z, box);
                }
            }
            this.generateBox(level, box, 1, 1, 2, w - 2, 3, d - 2, air, air, false);
            // 前室北门通走廊 + 门侧微光指引
            for (int x = 4; x <= 6; x++) {
                this.placeBlock(level, air, x, 1, d - 1, box);
                this.placeBlock(level, air, x, 2, d - 1, box);
            }
            this.placeBlock(level, rune, 3, 3, d - 1, box);
            this.placeBlock(level, rune, 7, 3, d - 1, box);
            // 实心夹层直到大厅地板(含门廊地面,顺带垫平门口)
            for (int y = 4; y <= hallFloor; y++) {
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) {
                        this.placeBlock(level, bricks(random), x, y, z, box);
                    }
                }
            }
            // 地表大厅:四壁(z2 南墙、z9 北墙)+ 厅顶,内部清空
            for (int y = hallFloor + 1; y <= hallFloor + 5; y++) {
                for (int x = 0; x < w; x++) {
                    this.placeBlock(level, bricks(random), x, y, 2, box);
                    this.placeBlock(level, bricks(random), x, y, d - 1, box);
                }
                for (int z = 2; z < d; z++) {
                    this.placeBlock(level, bricks(random), 0, y, z, box);
                    this.placeBlock(level, bricks(random), w - 1, y, z, box);
                }
            }
            for (int x = 0; x < w; x++) {
                for (int z = 2; z < d; z++) {
                    this.placeBlock(level, bricks(random), x, hallFloor + 6, z, box);
                }
            }
            this.generateBox(level, box, 1, hallFloor + 1, 3, w - 2, hallFloor + 5, d - 2, air, air, false);
            // 大厅南门 + 符文门楣
            for (int x = 4; x <= 6; x++) {
                this.placeBlock(level, air, x, hallFloor + 1, 2, box);
                this.placeBlock(level, air, x, hallFloor + 2, 2, box);
            }
            this.placeBlock(level, rune, 5, hallFloor + 3, 2, box);
            // 门廊(z0..1):清掉门前地形,保证大门无遮挡
            this.generateBox(level, box, 2, hallFloor + 1, 0, w - 3, hallFloor + 5, 1, air, air, false);
            // 梯井:2x2(x2..3, z7..8)贯穿夹层,整砖螺旋步下行
            for (int y = 1; y <= hallFloor; y++) {
                for (int x = 2; x <= 3; x++) {
                    for (int z = 7; z <= 8; z++) {
                        this.placeBlock(level, air, x, y, z, box);
                    }
                }
            }
            int[][] spiral = {{2, 7}, {3, 7}, {3, 8}, {2, 8}};
            for (int y = hallFloor - 1; y >= 1; y--) {
                int[] step = spiral[y % 4];
                this.placeBlock(level, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(),
                        step[0], y, step[1], box);
            }
            // 井口符文标记 + 厅内微光植被(垫苔藓)
            this.placeBlock(level, rune, 4, hallFloor + 1, 7, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), 8, hallFloor, 4, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), 8, hallFloor, 6, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), 8, hallFloor + 1, 4, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), 8, hallFloor + 1, 6, box);
        }

        private void buildCorridor(WorldGenLevel level, BoundingBox box, RandomSource random) {
            int w = this.kind.width, h = this.kind.height, d = this.kind.depth;
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            // 地板/顶板/侧墙(z 两端开放,由入口厅和宝藏房的墙封)
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, bricks(random), x, 0, z, box);
                    this.placeBlock(level, bricks(random), x, h - 1, z, box);
                }
            }
            for (int y = 1; y < h - 1; y++) {
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, bricks(random), 0, y, z, box);
                    this.placeBlock(level, bricks(random), w - 1, y, z, box);
                }
            }
            this.generateBox(level, box, 1, 1, 0, w - 2, h - 2, d - 1, air, air, false);
            // 每隔 6 格一对符文壁灯
            for (int z = 3; z < d; z += 6) {
                this.placeBlock(level, rune, 0, 3, z, box);
                this.placeBlock(level, rune, w - 1, 3, z, box);
            }
        }

        private void buildTreasure(WorldGenLevel level, BoundingBox box, RandomSource random) {
            buildShell(level, box, random);
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            // 南门通走廊(走廊内部 x10..12 → 本片局部 x4..6)
            for (int x = 4; x <= 6; x++) {
                this.placeBlock(level, air, x, 1, 0, box);
                this.placeBlock(level, air, x, 2, 0, box);
            }
            // 符文饰墙 + 双宝箱
            this.placeBlock(level, rune, 5, 1, 6, box);
            this.placeBlock(level, rune, 5, 2, 6, box);
            buildAccessibleChest(level, random, box, 3, 1, 5, ModLootTables.ECHO_TEMPLE_TREASURE);
            buildAccessibleChest(level, random, box, 7, 1, 5, ModLootTables.ECHO_TEMPLE_TREASURE);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), 2, 0, 6, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), 8, 0, 6, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), 2, 1, 6, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), 8, 1, 6, box);
        }

        /** 侧房通走廊的门:西侧房门在东墙(x=w-1),东侧房门在西墙(x=0),z 居中。 */
        private void carveSideDoor(WorldGenLevel level, BoundingBox box, BlockState fill) {
            int doorX = this.kind.isEastSide() ? 0 : this.kind.width - 1;
            int doorZ = this.kind.depth / 2;
            this.placeBlock(level, fill, doorX, 1, doorZ, box);
            this.placeBlock(level, fill, doorX, 2, doorZ, box);
        }

        private void buildCombat(WorldGenLevel level, BoundingBox box, RandomSource random) {
            buildShell(level, box, random);
            carveSideDoor(level, box, Blocks.AIR.defaultBlockState());
            // 中央刷怪笼(回响游荡者)+ 角落补给箱
            int cx = this.kind.width / 2, cz = this.kind.depth / 2;
            this.placeBlock(level, Blocks.SPAWNER.defaultBlockState(), cx, 1, cz, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(cx, cz), this.getWorldY(1), this.getWorldZ(cx, cz));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(ModEntities.ECHO_WANDERER.get(), random);
            }
            buildAccessibleChest(level, random, box, 2, 1, 2, ModLootTables.SMALL_ECHO_RUIN_CHEST);
        }

        private void buildArchive(WorldGenLevel level, BoundingBox box, RandomSource random) {
            buildShell(level, box, random);
            carveSideDoor(level, box, Blocks.AIR.defaultBlockState());
            // 记录室:沿北南墙书架,中央讲台,残页宝箱
            BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();
            for (int x = 2; x <= this.kind.width - 3; x++) {
                for (int y = 1; y <= 2; y++) {
                    this.placeBlock(level, shelf, x, y, 1, box);
                    this.placeBlock(level, shelf, x, y, this.kind.depth - 2, box);
                }
            }
            this.placeBlock(level, Blocks.LECTERN.defaultBlockState(),
                    this.kind.width / 2, 1, this.kind.depth / 2, box);
            buildAccessibleChest(level, random, box, this.kind.isEastSide() ? this.kind.width - 3 : 2,
                    1, this.kind.depth / 2, ModLootTables.ECHO_TEMPLE_ARCHIVE);
        }

        /**
         * 隐藏房:外壳整体锚定封印石(不可破坏,挖周围绕不进),
         * 朝走廊整面墙是幻象墙(隐纹石砖,外观同普通砖)——真视右键整面消散。
         */
        private void buildHidden(WorldGenLevel level, BoundingBox box, RandomSource random) {
            int w = this.kind.width, h = this.kind.height, d = this.kind.depth;
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState illusion = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int illusionX = this.kind.isEastSide() ? 0 : w - 1;
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, anchored, x, 0, z, box);
                    this.placeBlock(level, anchored, x, h - 1, z, box);
                }
            }
            // 苔藓/花会替换局部地面;下方二次封底保证不能从地下挖入隐藏房。
            for (int y = -2; y < 0; y++) {
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            for (int y = 1; y < h - 1; y++) {
                for (int x = 0; x < w; x++) {
                    this.placeBlock(level, anchored, x, y, 0, box);
                    this.placeBlock(level, anchored, x, y, d - 1, box);
                }
                for (int z = 0; z < d; z++) {
                    this.placeBlock(level, illusionX == 0 ? illusion : anchored, 0, y, z, box);
                    this.placeBlock(level, illusionX == w - 1 ? illusion : anchored, w - 1, y, z, box);
                }
            }
            this.generateBox(level, box, 1, 1, 1, w - 2, h - 2, d - 2, air, air, false);
            buildAccessibleChest(level, random, box, w / 2, 1, 2, ModLootTables.ECHO_TEMPLE_HIDDEN);
            // 荧光花立在苔藓上(植物不能种在石砖上)
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), w / 2 - 2, 0, 3, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), w / 2 + 2, 0, 3, box);
            this.placeBlock(level, ModBlocks.ECHO_FLOWER.get().defaultBlockState(), w / 2 - 2, 1, 3, box);
            this.placeBlock(level, ModBlocks.ECHO_FLOWER.get().defaultBlockState(), w / 2 + 2, 1, 3, box);
            // V0.6E 真视回访点(幻象遗迹=回响神殿隐藏房):隐藏碑文 + 真视宝箱
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(),
                    w / 2, 1, d - 3, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(),
                    w / 2 + 2, 1, d - 3, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(w / 2 + 2, d - 3),
                    this.getWorldY(1), this.getWorldZ(w / 2 + 2, d - 3));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
        }
    }
}
