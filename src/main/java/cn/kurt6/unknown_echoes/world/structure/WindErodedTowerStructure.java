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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 风蚀塔:漂浮群岛垂直谜题(地面结构)。 */
public class WindErodedTowerStructure extends TerrainAnchoredStructure {
    public static final MapCodec<WindErodedTowerStructure> CODEC = simpleCodec(WindErodedTowerStructure::new);

    private static final int SIZE = 25;
    private static final int HEIGHT = 47;

    public WindErodedTowerStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WIND_ERODED_TOWER.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.WIND_ERODED_TOWER_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.WIND_ERODED_TOWER_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState windStone = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int c = SIZE / 2;
            int towerRadius = 7;
            int topY = 33;

            this.generateBox(level, box, 0, 1, 0, SIZE - 1, HEIGHT - 1, SIZE - 1,
                    air, air, false);
            buildSteppedDisk(level, box, c, 0, c, 11, 2, cracked, windStone);
            buildRuneCauseway(level, box, c, 1, c, Direction.NORTH,
                    towerRadius, 11, 2, bricks, windStone, chiseled);

            for (int y = 2; y < topY; y++) {
                int radius = y < 18 ? towerRadius : y < 28 ? towerRadius - 1 : towerRadius - 2;
                for (int x = c - radius; x <= c + radius; x++) {
                    for (int z = c - radius; z <= c + radius; z++) {
                        int distance = (x - c) * (x - c) + (z - c) * (z - c);
                        int innerRadius = radius - 2;
                        boolean shell = distance <= radius * radius && distance >= innerRadius * innerRadius;
                        if (!shell) {
                            continue;
                        }
                        boolean protectedBase = y <= 4;
                        boolean erosionGap = !protectedBase && (x * 7 + z * 11 + y * 3) % 17 == 0;
                        if (!erosionGap) {
                            BlockState body = y % 7 == 0 ? chiseled
                                    : random.nextFloat() < 0.28F ? cracked : windStone;
                            this.placeBlock(level, body, x, y, z, box);
                        }
                    }
                }
                if (y % 8 == 0) {
                    architecturalRing(level, box, c, y, c, radius + 1, bricks);
                }
            }

            this.generateBox(level, box, c - 1, 2, c - towerRadius + 1, c, 4, c - towerRadius + 1,
                    air, air, false);
            buildConnectedDoor(level, box, ModBlocks.WIND_DOOR.get().defaultBlockState(),
                    c, 2, c - towerRadius, Direction.NORTH, 1, 2);

            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(),
                    c, 2, c, box);

            int midY = topY / 2;

            for (int x = c - 5; x <= c - 1; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, windStone, x, midY, z, box);
                }
            }
            buildAccessibleChest(level, random, box, c - 3, midY + 1, c,
                    ModLootTables.SKY_OBSERVATORY_CHEST);
            buildTrueSightVaultNiche(level, random, box, c + 8, midY + 1, c, Direction.WEST);
            this.placeBlock(level, windStone, c, midY, c + 2, box);
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(),
                    c, midY + 1, c + 2, box);
            for (int z = c - 3; z <= c + 3; z++) {
                this.placeBlock(level, air, c - towerRadius, midY + 1, z, box);
                this.placeBlock(level, air, c - towerRadius, midY + 2, z, box);
            }

            architecturalDisk(level, box, c, topY, c, 9, windStone);
            architecturalRing(level, box, c, topY + 1, c, 9, chiseled);
            buildCircularColonnade(level, box, c, topY + 1, c, 7, 8, 4,
                    windStone, lamp, cracked);
            // 收边必须用风蚀岩砖族:借用青色回响石砖会与浅灰风蚀岩撞色。
            BlockState windStairs = ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState();
            BlockState windSlab = ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, topY, c, Footprint.square(8), windStairs, windSlab, true);
            buildParapet(level, box, c, topY + 1, c, Footprint.square(9), 4, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), chiseled);
            buildStairCornice(level, box, c - 3, midY, c, Footprint.of(3, 4), windStairs, windSlab, false);
            this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                    c, topY + 1, c + 1, box);
            buildAccessibleChest(level, random, box, c, topY + 1, c,
                    ModLootTables.WIND_VAULT_CHEST);

            buildCornice(level, box, c, topY + 5, c, Footprint.square(6), chiseled, windStone);

            // 风向翼片：顶环外伸四片不同高度的脊翼，强化风蚀塔的远景剪影。
            for (int[] wing : new int[][]{{-6, -2, 35}, {6, -1, 37}, {-2, 6, 34}, {2, 6, 36}}) {
                int length = wing[2] - topY;
                for (int wingStep = 0; wingStep <= length; wingStep++) {
                    int x = c + (wing[0] == 0 ? 0 : Integer.signum(wing[0]) * Math.min(Math.abs(wing[0]), wingStep));
                    int z = c + (wing[1] == 0 ? 0 : Integer.signum(wing[1]) * Math.min(Math.abs(wing[1]), wingStep));
                    this.placeBlock(level, wingStep % 2 == 0 ? windStone : chiseled, x, topY + wingStep, z, box);
                }
                this.placeBlock(level, lamp, c + wing[0], wing[2], c + wing[1], box);
            }

            int[][] diagonalDirections = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] diagonal : diagonalDirections) {
                for (int distance = 6; distance <= 10; distance++) {
                    int height = Math.max(3, 14 - (distance - 6));
                    architecturalPillar(level, box, c + diagonal[0] * distance, 1,
                            c + diagonal[1] * distance, height,
                            distance % 3 == 0 ? chiseled : windStone);
                }
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Direction side = direction.getClockWise();
                for (int sign : new int[]{-1, 1}) {
                    int x = c + direction.getStepX() * 10 + side.getStepX() * 3 * sign;
                    int z = c + direction.getStepZ() * 10 + side.getStepZ() * 3 * sign;
                    architecturalPillar(level, box, x, 1, z, 14, bricks);
                    this.placeBlock(level, lamp, x, 15, z, box);
                }
            }
            scatterArchitecturalDebris(level, box, "wind_eroded_tower", c, 1, c,
                    cracked, windStone);

            placeStructureSpawner(level, random, box, c + 8, 1, c - 4,
                    ModEntities.WIND_ERODED_SENTINEL.get(), windStone, chiseled);
            buildWalkableSpiral(level, box, c, 1, c, topY, 4, windStone, windStairs);
            for (int x = c - 1; x <= c + 5; x++) {
                for (int z = c - 1; z <= c + 1; z++) {
                    placeCorniceBlock(level, box, windStone, x, midY, z);
                    for (int head = 1; head <= 3; head++) {
                        placeCorniceBlock(level, box, air, x, midY + head, z);
                    }
                }
            }
            anchorGroundColumns(level, box, 0, windStone);
            dressStructure(level, box, "wind_eroded_tower");
        }


        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
