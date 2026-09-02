package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 无声环:失语沼泽环境谜题(低 UI 依赖观察谜题)。 */
public class SilentRingStructure extends SinglePieceStructure {
    public static final MapCodec<SilentRingStructure> CODEC = simpleCodec(SilentRingStructure::new);

    private static final int SIZE = 29;
    private static final int HEIGHT = 16;

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

            BlockState mossy = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            int cx = SIZE / 2;
            int cz = SIZE / 2;

            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= 121) {
                        BlockState floor = distSq >= 90 && random.nextFloat() < 0.45F ? moss
                                : (random.nextFloat() < 0.4F ? mossy : bricks);
                        this.placeBlock(level, floor, x, 0, z, box);
                    }
                }
            }

            this.placeBlock(level, chiseled, cx, 0, cz, box);
            this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState()
                    .setValue(PuzzleCoreBlock.ACTIVE, Boolean.FALSE), cx, 1, cz, box);

            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);

            int[] orders = shuffled(puzzleRandom, 6);
            int[][] runeSpots = {
                    {cx, cz - 8}, {cx + 7, cz - 4}, {cx + 7, cz + 4},
                    {cx, cz + 8}, {cx - 7, cz + 4}, {cx - 7, cz - 4}
            };
            for (int i = 0; i < runeSpots.length; i++) {
                int rx = runeSpots[i][0];
                int rz = runeSpots[i][1];
                this.placeBlock(level, bricks, rx, 1, rz, box);
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, orders[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.FALSE), rx, 2, rz, box);
            }

            int[] sequence = java.util.Arrays.copyOf(shuffled(puzzleRandom, 6), 5);
            BlockPos corePos = new BlockPos(this.getWorldX(cx, cz), this.getWorldY(1), this.getWorldZ(cx, cz));
            if (box.isInside(corePos) && level.getBlockEntity(corePos)
                    instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }

            int muralZ = cz - 10;
            for (int x = cx - 7; x <= cx + 7; x++) {
                this.placeBlock(level, bricks, x, 0, muralZ, box);
                for (int y = 1; y <= 4; y++) {
                    this.placeBlock(level, random.nextFloat() < 0.35F ? mossy : bricks, x, y, muralZ, box);
                }
            }
            for (int i = 0; i < sequence.length; i++) {
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.TRUE), cx - 4 + i * 2, 4, muralZ, box);
            }

            int vaultMinZ = cz + 7;
            int vaultMaxZ = cz + 9;
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = vaultMinZ; z <= vaultMaxZ; z++) {
                    for (int y = 0; y <= 3; y++) {
                        boolean shell = y == 0 || y == 3
                                || x == cx - 1 || x == cx + 1
                                || z == vaultMinZ || z == vaultMaxZ;
                        BlockState shellState = z == vaultMinZ && y >= 1 && y <= 2
                                ? sealed : anchored;
                        this.placeBlock(level, shell ? shellState : Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }

            for (int y = -2; y < 0; y++) {
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = vaultMinZ; z <= vaultMaxZ; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            buildAccessibleChest(level, random, box, cx, 1, cz + 8, ModLootTables.SILENT_RING_CHEST);
            buildCompactTrueSightVaultNiche(level, random, box, cx + 10, 1, cz + 7, Direction.WEST);

            architecturalRing(level, box, cx, 1, cz, 12,
                    ModBlocks.SILENT_MUDSTONE.get().defaultBlockState());
            buildCircularColonnade(level, box, cx, 1, cz, 11, 12, 7,
                    bricks, chiseled, mossy);
            buildCardinalGateway(level, box, cx, 1, cz, 12, Direction.SOUTH,
                    2, 6, bricks, chiseled);
            buildParapet(level, box, cx, 1, cz, Footprint.square(13), 5, ModBlocks.SILENT_MUDSTONE_BRICK_WALL.get().defaultBlockState(), mossy);
            buildStairCornice(level, box, cx, 8, cz, Footprint.square(11), ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SILENT_MUDSTONE_BRICK_SLAB.get().defaultBlockState(), true);

            buildStairCornice(level, box, cx, 5, muralZ, Footprint.of(7, 0), ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SILENT_MUDSTONE_BRICK_SLAB.get().defaultBlockState(), true);

            for (int[] diagonal : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                int towerX = cx + diagonal[0] * 9;
                int towerZ = cz + diagonal[1] * 9;
                architecturalPillar(level, box, towerX, 1, towerZ, 7, bricks);
                buildSpireFinial(level, box, towerX, 8, towerZ, 1, 3,
                        ModBlocks.SILENT_MUDSTONE.get().defaultBlockState(), mossy, chiseled);
            }
            buildRuneCauseway(level, box, cx, 0, cz, Direction.SOUTH,
                    9, 13, 1, moss, mossy, chiseled);

            placeStructureSpawner(level, random, box, cx - 7, 1, cz + 6,
                    ModEntities.SILENT_WALKER.get(), mossy, chiseled);
        }

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
