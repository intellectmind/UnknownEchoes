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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** 回声峭壁中的纪念性折痕祭坛。结构可多处生成，个人校准互不影响。 */
public class AbyssBeaconStructure extends SinglePieceStructure {
    public static final MapCodec<AbyssBeaconStructure> CODEC = simpleCodec(AbyssBeaconStructure::new);

    private static final int WIDTH = 49;
    private static final int DEPTH = 49;
    private static final int HEIGHT = 26;

    public AbyssBeaconStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ABYSS_BEACON_STRUCTURE.get();
    }

    public static class Piece extends EchoStructurePiece {
        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.ABYSS_BEACON_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.ABYSS_BEACON_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState stone = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState crystal = ModBlocks.RESONANT_CLIFF_CRYSTAL.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int c = WIDTH / 2;

            // 掏空穹顶。不要换成 clearArenaVolume:它让流体让行,而基座可能泡在水里(sea_level 63)。
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    if (dx * dx + dz * dz <= 23 * 23) {
                        for (int y = 1; y < HEIGHT; y++) {
                            if (this.getBlock(level, x, y, z, box).isAir()) {
                                continue;
                            }
                            this.placeBlock(level, air, x, y, z, box);
                        }
                    }
                }
            }

            architecturalDisk(level, box, c, 0, c, 20, stone);
            architecturalDisk(level, box, c, 1, c, 17, cracked);
            architecturalDisk(level, box, c, 2, c, 14, bricks);
            architecturalDisk(level, box, c, 3, c, 11, bricks);
            architecturalRing(level, box, c, 3, c, 9, rune);
            buildStairCornice(level, box, c, 2, c, Footprint.square(10), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 3, c, Footprint.square(12), 5, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Direction side = direction.getClockWise();
                for (int distance = 9; distance <= 22; distance++) {
                    int pathY = distance <= 11 ? 3 : distance <= 14 ? 2 : distance <= 18 ? 1 : 0;
                    int pathX = c + direction.getStepX() * distance;
                    int pathZ = c + direction.getStepZ() * distance;
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        BlockState path = Math.abs(lateral) == 2 ? stone
                                : lateral == 0 && distance % 4 == 0 ? rune : bricks;
                        this.placeBlock(level, path,
                                pathX + side.getStepX() * lateral, pathY,
                                pathZ + side.getStepZ() * lateral, box);
                    }
                }
                buildCardinalGateway(level, box, c, 1, c, 21, direction,
                        3, 10, stone, rune);
            }

            for (int i = 0; i < 12; i++) {
                double angle = (i + 0.5D) * Math.PI * 2.0D / 12.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 16);
                int z = c + (int) Math.round(Math.sin(angle) * 16);
                architecturalPillar(level, box, x, 1, z, 10 + i % 3, i % 2 == 0 ? stone : bricks);
                this.placeBlock(level, crystal, x, 12 + i % 3, z, box);
            }
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    int distance = dx * dx + dz * dz;
                    if (distance >= 15 * 15 && distance <= 16 * 16
                            && Math.floorMod(x * 31 + z * 17, 7) != 0) {
                        this.placeBlock(level, rune, x, 14, z, box);
                    }
                }
            }

            int[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] diagonal : diagonals) {
                for (int distance = 10; distance <= 19; distance++) {
                    int height = Math.max(2, 12 - (distance - 10));
                    int x = c + diagonal[0] * distance;
                    int z = c + diagonal[1] * distance;
                    architecturalPillar(level, box, x, 1, z, height,
                            distance % 4 == 0 ? rune : stone);
                }
            }

            // 侧亭必须用退台台座与主线折痕祭坛的三级圆台同族,双坡亭殿是通用模板。
            for (int pavilionX : new int[]{c - 14, c + 14}) {
                buildSteppedZiggurat(level, box, pavilionX, 1, c - 7,
                        5, 3, 3, 0, bricks, rune,
                        ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), crystal);
                buildStairCornice(level, box, pavilionX, 8, c - 7, Footprint.square(5), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);
                buildSpireFinial(level, box, pavilionX, 9, c - 7, 3, 5,
                        bricks, rune, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState());
            }

            buildCornice(level, box, c, 16, c, Footprint.square(7), bricks, rune);
            buildStairCornice(level, box, c, 21, c, Footprint.square(5), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);

            for (int[] diagonal : diagonals) {
                int x = c + diagonal[0] * 5;
                int z = c + diagonal[1] * 5;
                architecturalPillar(level, box, x, 3, z, 15, bricks);
                this.placeBlock(level, crystal, x, 18, z, box);
            }
            architecturalRing(level, box, c, 17, c, 7, rune);
            architecturalRing(level, box, c, 20, c, 5, cracked);
            this.placeBlock(level, bricks, c, 4, c, box);
            this.placeBlock(level, ModBlocks.ABYSS_BEACON.get().defaultBlockState(),
                    c, 5, c, box);

            scatterArchitecturalDebris(level, random, box, c, 1, c,
                    20, 23, 36, stone, cracked);

            placeStructureSpawner(level, random, box, c + 13, 3, c + 5,
                    ModEntities.CRYSTAL_NOISE_WISP.get(), bricks, rune);
        }
    }
}
