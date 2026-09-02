package cn.kurt6.unknown_echoes.world.structure;

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

/**
 * 符文谜题房:主世界生成的第一个谜题房。
 * 前厅八根顺序符文柱(点数乱序分布)、北墙高处六块提示柱(给出激活顺序)、
 * 嵌谜题核心的封印石隔墙,内室放宝箱。
 */
public class RunePuzzleRoomStructure extends SinglePieceStructure {
    public static final MapCodec<RunePuzzleRoomStructure> CODEC = simpleCodec(RunePuzzleRoomStructure::new);

    private static final int WIDTH = 25;
    private static final int DEPTH = 31;
    private static final int HEIGHT = 15;

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
            int centerX = WIDTH / 2;
            int centerZ = DEPTH / 2;
            int roomMinX = 4;
            int roomMaxX = WIDTH - 5;
            int frontZ = 6;
            int barrierZ = 18;
            int backZ = 26;
            int roofY = 10;

            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1,
                    air, air, false);
            buildSteppedDisk(level, box, centerX, 0, centerZ, 12, 2,
                    ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(), bricks);

            for (int x = roomMinX; x <= roomMaxX; x++) {
                for (int z = frontZ; z <= backZ; z++) {
                    BlockState floor = z >= barrierZ ? anchored
                            : random.nextFloat() < 0.22F ? cracked : bricks;
                    this.placeBlock(level, floor, x, 1, z, box);
                    this.placeBlock(level, z >= barrierZ ? anchored : bricks, x, roofY, z, box);
                }
            }
            for (int y = 2; y < roofY; y++) {
                for (int x = roomMinX; x <= roomMaxX; x++) {
                    this.placeBlock(level, bricks, x, y, frontZ, box);
                    this.placeBlock(level, anchored, x, y, backZ, box);
                }
                for (int z = frontZ; z <= backZ; z++) {
                    BlockState side = z >= barrierZ ? anchored : bricks;
                    this.placeBlock(level, side, roomMinX, y, z, box);
                    this.placeBlock(level, side, roomMaxX, y, z, box);
                }
            }
            for (int y = -2; y <= 0; y++) {
                for (int x = roomMinX; x <= roomMaxX; x++) {
                    for (int z = barrierZ; z <= backZ; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            this.generateBox(level, box, roomMinX + 1, 2, frontZ + 1,
                    roomMaxX - 1, roofY - 1, barrierZ - 1, air, air, false);
            this.generateBox(level, box, roomMinX + 1, 2, barrierZ + 1,
                    roomMaxX - 1, roofY - 1, backZ - 1, air, air, false);

            this.placeBlock(level, air, centerX, 2, frontZ, box);
            this.placeBlock(level, air, centerX, 3, frontZ, box);
            this.placeBlock(level, air, centerX, 4, frontZ, box);
            this.placeBlock(level, rune, centerX - 2, 4, frontZ, box);
            this.placeBlock(level, rune, centerX + 2, 4, frontZ, box);

            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int frontCenterZ = (frontZ + barrierZ) / 2;
            int sealCenterZ = (barrierZ + backZ) / 2;
            illuminateInterior(level, box, centerX, frontCenterZ, roofY - 1,
                    (roomMaxX - roomMinX) / 2 - 1, (barrierZ - frontZ) / 2 - 1, 4, lamp);
            illuminateInterior(level, box, centerX, sealCenterZ, roofY - 1,
                    (roomMaxX - roomMinX) / 2 - 1, (backZ - barrierZ) / 2 - 1, 3, lamp);

            BlockState hiddenRune = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            this.placeBlock(level, hiddenRune, roomMinX, 4, centerZ - 3, box);
            this.placeBlock(level, hiddenRune, roomMaxX, 5, centerZ + 1, box);

            for (int y = 2; y < roofY; y++) {
                for (int x = roomMinX + 1; x < roomMaxX; x++) {
                    this.placeBlock(level, sealed, x, y, barrierZ, box);
                }
            }
            this.placeBlock(level, core, centerX, 3, barrierZ, box);

            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);

            int[] orders = shuffled(puzzleRandom, 8);
            int[][] pillarSpots = {
                    {centerX - 6, frontZ + 3}, {centerX + 6, frontZ + 3},
                    {centerX - 6, frontZ + 7}, {centerX + 6, frontZ + 7},
                    {centerX - 4, barrierZ - 2}, {centerX + 4, barrierZ - 2},
                    {centerX - 2, frontZ + 5}, {centerX + 2, frontZ + 5}
            };
            for (int i = 0; i < pillarSpots.length; i++) {
                placeRunePillar(level, box, pillarSpots[i][0], pillarSpots[i][1], orders[i], bricks);
            }

            int[] sequence = java.util.Arrays.copyOf(shuffled(puzzleRandom, 8), 6);
            BlockPos corePos = new BlockPos(this.getWorldX(centerX, barrierZ),
                    this.getWorldY(3), this.getWorldZ(centerX, barrierZ));
            if (box.isInside(corePos) && level.getBlockEntity(corePos)
                    instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }

            for (int i = 0; i < sequence.length; i++) {
                BlockState hint = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(SequenceRuneBlock.LIT, Boolean.TRUE);
                this.placeBlock(level, hint, centerX - 3 + i, 7, barrierZ, box);
            }

            buildAccessibleChest(level, random, box, centerX, 2, backZ - 3,
                    ModLootTables.WIND_VAULT_CHEST);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), centerX - 3, 1, backZ - 3, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), centerX + 3, 1, backZ - 3, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), centerX - 3, 2, backZ - 3, box);
            this.placeBlock(level, ModBlocks.GLOW_GRASS.get().defaultBlockState(), centerX + 3, 2, backZ - 3, box);

            architecturalRing(level, box, centerX, 2, centerZ, 11, cracked);
            buildStairCornice(level, box, centerX, 1, centerZ, Footprint.square(11), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, centerX, 2, centerZ, Footprint.square(12), 5, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), cracked);
            buildCircularColonnade(level, box, centerX, 2, centerZ, 10, 12, 8,
                    bricks, rune, cracked);

            buildTwinTowerFacade(level, box, centerX, 1, frontZ - 1,
                    7, 2, 9, bricks, rune, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            buildCardinalGateway(level, box, centerX, 2, centerZ, 11, Direction.NORTH,
                    3, 7, bricks, rune);
            buildRuneCauseway(level, box, centerX, 1, centerZ, Direction.NORTH,
                    8, 14, 2, bricks, ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState(), rune);
            buildTrueSightVaultNiche(level, random, box,
                    centerX + 9, 2, centerZ + 8, Direction.WEST);

            placeStructureSpawner(level, random, box, centerX - 9, 2, centerZ - 9,
                    ModEntities.ECHO_REMNANT.get(), bricks, rune);
        }

        private void placeRunePillar(WorldGenLevel level, BoundingBox box, int x, int z,
                                     int order, BlockState base) {
            this.placeBlock(level, base, x, 1, z, box);
            BlockState runeState = ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                    .setValue(SequenceRuneBlock.ORDER, order);
            this.placeBlock(level, runeState, x, 2, z, box);
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
    }
}
