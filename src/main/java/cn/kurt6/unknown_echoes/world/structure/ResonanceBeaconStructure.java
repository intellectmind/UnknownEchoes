package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class ResonanceBeaconStructure extends TerrainAnchoredStructure {
    public static final MapCodec<ResonanceBeaconStructure> CODEC = simpleCodec(ResonanceBeaconStructure::new);

    private static final int WIDTH = 31;
    private static final int DEPTH = 31;
    private static final int HEIGHT = 18;

    public ResonanceBeaconStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RESONANCE_BEACON_STRUCTURE.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.RESONANCE_BEACON_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.RESONANCE_BEACON_PIECE.get(), tag);
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
            BlockState beacon = ModBlocks.RESONANCE_BEACON.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState mossy = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int c = WIDTH / 2;

            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1,
                    air, air, false);

            buildSteppedDisk(level, box, c, 0, c, 14, 3, mossy, bricks);
            buildRuneCauseway(level, box, c, 2, c, Direction.SOUTH,
                    6, 14, 2, bricks, cracked, rune);
            architecturalRing(level, box, c, 3, c, 10, rune);
            buildStairCornice(level, box, c, 2, c, Footprint.square(11), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 3, c, Footprint.square(13), 5, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), cracked);
            architecturalRing(level, box, c, 3, c, 6, cracked);

            int[][] diagonalDirections = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] diagonal : diagonalDirections) {
                for (int distance = 6; distance <= 12; distance++) {
                    int height = Math.max(2, 8 - (distance - 6) / 2);
                    architecturalPillar(level, box, c + diagonal[0] * distance, 1,
                            c + diagonal[1] * distance, height,
                            distance % 3 == 0 ? rune : bricks);
                }
            }
            buildCircularColonnade(level, box, c, 3, c, 9, 12, 7,
                    bricks, rune, cracked);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 3, c, 10, direction,
                        2, 6, bricks, rune);
            }

            for (int pavilionX : new int[]{c - 8, c + 8}) {
                buildGabledPavilion(level, box, pavilionX, 1, c - 7, Footprint.of(4, 5), 6, 3, mossy, bricks, rune, lamp);
                buildStairCornice(level, box, pavilionX, 7, c - 7, Footprint.of(4, 5), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                buildSpireFinial(level, box, pavilionX, 8, c - 7, 2, 4,
                        bricks, rune, lamp);
            }

            architecturalDisk(level, box, c, 3, c, 4, bricks);
            architecturalRing(level, box, c, 4, c, 3, rune);
            this.placeBlock(level, lamp, c, 3, c, box);
            this.placeBlock(level, beacon, c, 4, c, box);
            for (int[] point : new int[][]{{c - 6, c - 6}, {c + 6, c - 6},
                    {c - 6, c + 6}, {c + 6, c + 6}}) {
                architecturalPillar(level, box, point[0], 3, point[1], 5, bricks);
                this.placeBlock(level, lamp, point[0], 8, point[1], box);
            }
            // 共鸣叉架：中央信标之外再给四条频率不同的立向支架，形成不对称高点。
            for (int[] fork : new int[][]{{c - 5, c + 2, 8}, {c + 5, c + 1, 9},
                    {c - 2, c - 5, 8}, {c + 3, c - 4, 7}}) {
                architecturalPillar(level, box, fork[0], 5, fork[1], fork[2],
                        fork[2] % 2 == 0 ? cracked : bricks);
                this.placeBlock(level, rune, fork[0], 5 + fork[2], fork[1], box);
                this.placeBlock(level, lamp, fork[0], 6 + fork[2], fork[1], box);
            }
            for (int offset = -3; offset <= 3; offset++) {
                this.placeBlock(level, offset % 2 == 0 ? rune : bricks,
                        c + offset, 10, c + 1, box);
            }
            scatterArchitecturalDebris(level, box, "resonance_beacon_structure", c, 1, c,
                    cracked, mossy);

            placeStructureSpawner(level, random, box, c + 10, 2, c - 5,
                    ModEntities.ECHO_WANDERER.get(), bricks, rune);
            anchorGroundColumns(level, box, 0, mossy);
            dressStructure(level, box, "resonance_beacon_structure");
        }
    }
}
