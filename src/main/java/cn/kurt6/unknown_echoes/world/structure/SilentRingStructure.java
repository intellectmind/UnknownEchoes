package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlock;
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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 无声环:失语沼泽环境谜题(低 UI 依赖观察谜题)。
 * 露天石环 + 中央谜题核心 + 六根顺序符文柱(位置与点数全部乱序)。
 * 北侧壁画墙从左到右嵌着五块常亮提示柱——这是激活顺序;六根柱里有一根
 * 从不出现在壁画上(干扰柱,点错会重置全部进度)。
 * 在提示音被压制的沼泽里,玩家只凭壁画上的点数顺序解开机关,打开封印石藏宝室。
 * 完全复用 PuzzleCoreBlockEntity / SequenceRuneBlock,序列只存服务端。
 */
public class SilentRingStructure extends SinglePieceStructure {
    public static final MapCodec<SilentRingStructure> CODEC = simpleCodec(SilentRingStructure::new);

    private static final int SIZE = 15;
    private static final int HEIGHT = 8;

    public SilentRingStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SILENT_RING.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SILENT_RING_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SILENT_RING_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
            BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            int cx = SIZE / 2; // 7
            int cz = SIZE / 2;

            // 圆形石环地面(半径 5),边缘混噤声苔
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= 25) {
                        BlockState floor = distSq >= 18 && random.nextFloat() < 0.45F ? moss
                                : (random.nextFloat() < 0.4F ? mossy : bricks);
                        this.placeBlock(level, floor, x, 0, z, box);
                    }
                }
            }

            // 中央台座 + 谜题核心(y=1,提示柱须在核心 +3 以上才不被重置)
            this.placeBlock(level, chiseled, cx, 0, cz, box);
            this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState()
                    .setValue(PuzzleCoreBlock.ACTIVE, Boolean.FALSE), cx, 1, cz, box);

            // 谜题布局必须跨区块一致:用包围盒坐标派生确定性随机,不用每区块不同的 random
            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);

            // 六根顺序符文柱:环形六点,点数(ORDER 0~5)随机分配到位置
            int[] orders = shuffled(puzzleRandom, 6);
            int[][] runeSpots = {
                    {cx, cz - 4}, {cx + 4, cz - 2}, {cx + 4, cz + 2},
                    {cx, cz + 4}, {cx - 4, cz + 2}, {cx - 4, cz - 2}
            };
            for (int i = 0; i < runeSpots.length; i++) {
                int rx = runeSpots[i][0];
                int rz = runeSpots[i][1];
                this.placeBlock(level, bricks, rx, 1, rz, box);
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, orders[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.FALSE), rx, 2, rz, box);
            }

            // 激活序列:六个点数乱序后取前五个;落选的那个点数是干扰柱
            int[] sequence = java.util.Arrays.copyOf(shuffled(puzzleRandom, 6), 5);
            BlockPos corePos = new BlockPos(this.getWorldX(cx, cz), this.getWorldY(1), this.getWorldZ(cx, cz));
            if (level.getBlockEntity(corePos) instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }

            // 北侧壁画墙:11 宽 4 高,顶排(y=4,核心 +3)从左到右嵌常亮提示柱 = 激活顺序
            for (int x = cx - 5; x <= cx + 5; x++) {
                this.placeBlock(level, bricks, x, 0, 1, box); // 墙基,防止两端悬空
                for (int y = 1; y <= 4; y++) {
                    this.placeBlock(level, random.nextFloat() < 0.35F ? mossy : bricks, x, y, 1, box);
                }
            }
            for (int i = 0; i < sequence.length; i++) {
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.TRUE), cx - 4 + i * 2, 4, 1, box);
            }

            // 南侧藏宝室:永久壳防挖，朝谜题核心的一面用普通封印石作为可消散入口。
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = SIZE - 3; z <= SIZE - 1; z++) {
                    for (int y = 0; y <= 3; y++) {
                        boolean shell = y == 0 || y == 3
                                || x == cx - 1 || x == cx + 1
                                || z == SIZE - 3 || z == SIZE - 1;
                        BlockState shellState = z == SIZE - 3 && y >= 1 && y <= 2
                                ? sealed : anchored;
                        this.placeBlock(level, shell ? shellState : Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }
            // 藏宝室下方额外两层永久封底，防止从沼泽洞穴/地下短路径挖到箱子底部。
            for (int y = -2; y < 0; y++) {
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = SIZE - 3; z <= SIZE - 1; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            buildAccessibleChest(level, random, box, cx, 1, SIZE - 2, ModLootTables.SILENT_RING_CHEST);
            buildCompactTrueSightVaultNiche(level, random, box, cx + 5, 1, cz + 5, Direction.WEST);
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

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
