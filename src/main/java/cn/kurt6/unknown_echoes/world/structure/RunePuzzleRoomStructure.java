package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock;
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
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 符文谜题房(V0.2 第一个谜题房,主世界生成):
 * 前厅八根顺序符文柱(点数乱序分布)+ 北墙高处六块提示柱(= 激活顺序)
 * + 封印石隔墙(嵌谜题核心)+ 内室宝箱。
 * 八根柱里有两根从不出现在提示行上(干扰柱,点错全部重置)。
 * V0.6B 难度与奖励同步上调:六柱五步+1 干扰 → 八柱六步+2 干扰(旧档已生成的房间
 * 序列存在核心 BlockEntity 里,保持原难度不受影响)。
 * 序列只存核心 BlockEntity(服务端),封印石不可破坏,飞行/传送/挖掘都进不了内室。
 */
public class RunePuzzleRoomStructure extends SinglePieceStructure {
    public static final MapCodec<RunePuzzleRoomStructure> CODEC = simpleCodec(RunePuzzleRoomStructure::new);

    private static final int WIDTH = 11;
    private static final int DEPTH = 14;
    private static final int HEIGHT = 8;

    public RunePuzzleRoomStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RUNE_PUZZLE_ROOM.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.RUNE_PUZZLE_ROOM_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.RUNE_PUZZLE_ROOM_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState core = ModBlocks.PUZZLE_CORE.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            // 内室永久外壳用锚定封印石；入口隔墙仍用会随解谜消散的普通封印石。
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    BlockState floor = z >= 8 ? anchored : random.nextFloat() < 0.2F ? cracked : bricks;
                    this.placeBlock(level, floor, x, 0, z, box);
                    this.placeBlock(level, bricks, x, HEIGHT - 1, z, box);
                }
            }
            // 内室底部额外下探两层封印石,封住从洞穴/地下侧向挖入的短路径。
            for (int y = -2; y < 0; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    for (int z = 8; z < DEPTH; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            // 外墙(前厅部分普通砖)
            for (int y = 1; y < HEIGHT - 1; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    this.placeBlock(level, bricks, x, y, 0, box);
                }
                for (int z = 0; z < 8; z++) {
                    this.placeBlock(level, bricks, 0, y, z, box);
                    this.placeBlock(level, bricks, WIDTH - 1, y, z, box);
                }
            }
            // 内室壳(z>=8)整体用封印石:墙、顶,防止挖掘绕过
            for (int y = 1; y < HEIGHT - 1; y++) {
                for (int z = 8; z < DEPTH; z++) {
                    this.placeBlock(level, anchored, 0, y, z, box);
                    this.placeBlock(level, anchored, WIDTH - 1, y, z, box);
                }
                for (int x = 0; x < WIDTH; x++) {
                    this.placeBlock(level, anchored, x, y, DEPTH - 1, box);
                }
            }
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 8; z < DEPTH; z++) {
                    this.placeBlock(level, anchored, x, HEIGHT - 2, z, box);
                }
            }
            // 内部清空(前厅 + 内室)
            this.generateBox(level, box, 1, 1, 1, WIDTH - 2, HEIGHT - 3, 7, air, air, false);
            this.generateBox(level, box, 1, 1, 9, WIDTH - 2, HEIGHT - 3, DEPTH - 2, air, air, false);

            // 入口(南墙正中,两格高门洞)
            this.placeBlock(level, air, WIDTH / 2, 1, 0, box);
            this.placeBlock(level, air, WIDTH / 2, 2, 0, box);
            this.placeBlock(level, rune, WIDTH / 2 - 1, 2, 0, box);
            this.placeBlock(level, rune, WIDTH / 2 + 1, 2, 0, box);

            // 真视测试:前厅两侧墙各藏一块隐纹石砖(外观与回响石砖一致,真视回响可显形)
            BlockState hiddenRune = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            this.placeBlock(level, hiddenRune, 0, 2, 4, box);
            this.placeBlock(level, hiddenRune, WIDTH - 1, 3, 6, box);

            // 封印石隔墙(z=8),中央嵌谜题核心
            for (int y = 1; y < HEIGHT - 1; y++) {
                for (int x = 1; x < WIDTH - 1; x++) {
                    this.placeBlock(level, sealed, x, y, 8, box);
                }
            }
            this.placeBlock(level, core, WIDTH / 2, 2, 8, box);

            // 谜题布局必须跨区块一致:用包围盒坐标派生确定性随机
            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);

            // 八根顺序符文柱:前厅两侧、中部与隔墙前,点数(ORDER 0~7)随机分配到位置
            // (中线 x=WIDTH/2 留空,门口到核心的走道不被柱子挡住)
            int[] orders = shuffled(puzzleRandom, 8);
            int[][] pillarSpots = {{2, 2}, {8, 2}, {2, 5}, {8, 5}, {3, 7}, {7, 7}, {4, 4}, {6, 4}};
            for (int i = 0; i < pillarSpots.length; i++) {
                placeRunePillar(level, box, pillarSpots[i][0], pillarSpots[i][1], orders[i], bricks);
            }

            // 激活序列:八个点数乱序后取前六个;落选的两个是干扰柱
            int[] sequence = java.util.Arrays.copyOf(shuffled(puzzleRandom, 8), 6);
            BlockPos corePos = new BlockPos(this.getWorldX(WIDTH / 2, 8),
                    this.getWorldY(2), this.getWorldZ(WIDTH / 2, 8));
            if (level.getBlockEntity(corePos) instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }

            // 顺序提示行:北侧隔墙上方 y=5,从左到右 = 激活顺序,常亮(不参与判定,重置不影响)
            for (int i = 0; i < sequence.length; i++) {
                BlockState hint = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.TRUE);
                this.placeBlock(level, hint, WIDTH / 2 - 3 + i, 5, 8, box);
            }

            // 内室奖励(发光草垫苔藓,植物不能立在石砖上)
            buildAccessibleChest(level, random, box, WIDTH / 2, 1, DEPTH - 3, ModLootTables.WIND_VAULT_CHEST);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), WIDTH / 2 - 2, 1, DEPTH - 3, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), WIDTH / 2 + 2, 1, DEPTH - 3, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), WIDTH / 2 - 2, 2, DEPTH - 3, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), WIDTH / 2 + 2, 2, DEPTH - 3, box);
        }

        private void placeRunePillar(WorldGenLevel level, BoundingBox box, int x, int z,
                                     int order, BlockState base) {
            this.placeBlock(level, base, x, 1, z, box);
            BlockState runeState = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                    .setValue(SequenceRuneBlock.ORDER, order);
            this.placeBlock(level, runeState, x, 2, z, box);
        }

        /** 0..size-1 的乱序排列(Fisher-Yates)。 */
        private static int[] shuffled(RandomSource random, int size) {
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
    }
}
