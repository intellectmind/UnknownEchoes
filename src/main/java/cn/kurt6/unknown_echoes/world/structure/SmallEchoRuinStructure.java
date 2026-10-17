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
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 主世界小型回响遗迹:回响石砖残垣 + 中央宝箱(回响碎片/低概率回响核心)+ 回响符文砖线索。 */
public class SmallEchoRuinStructure extends TerrainAnchoredStructure {
    public static final MapCodec<SmallEchoRuinStructure> CODEC = simpleCodec(SmallEchoRuinStructure::new);

    private static final int WIDTH = 29;
    private static final int DEPTH = 29;
    private static final int HEIGHT = 15;

    public SmallEchoRuinStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SMALL_ECHO_RUIN.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SMALL_ECHO_RUIN_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SMALL_ECHO_RUIN_PIECE.get(), tag);
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
            BlockState mossy = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int c = WIDTH / 2;

            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1,
                    air, air, false);
            buildSteppedDisk(level, box, c, 0, c, 13, 2, mossy, bricks);
            for (int x = c - 10; x <= c + 10; x++) {
                for (int z = c - 10; z <= c + 10; z++) {
                    int distance = (x - c) * (x - c) + (z - c) * (z - c);
                    if (distance <= 100 && random.nextFloat() < 0.24F) {
                        this.placeBlock(level, cracked, x, 1, z, box);
                    }
                }
            }
            buildRuneCauseway(level, box, c, 1, c, Direction.SOUTH,
                    5, 13, 2, bricks, mossy, rune);

            for (int i = 0; i < 16; i++) {
                if (i == 4 || i == 5 || i == 11) {
                    continue;
                }
                double angle = Math.PI * 2.0D * i / 16.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 10);
                int z = c + (int) Math.round(Math.sin(angle) * 10);
                int height = 3 + random.nextInt(5);
                architecturalPillar(level, box, x, 2, z, height,
                        random.nextFloat() < 0.35F ? cracked : bricks);
                if (height >= 6) {
                    this.placeBlock(level, rune, x, 2 + height, z, box);
                }
            }
            buildCardinalGateway(level, box, c, 2, c, 10, Direction.SOUTH,
                    2, 6, bricks, rune);
            buildCardinalGateway(level, box, c, 2, c, 10, Direction.EAST,
                    2, 4, cracked, rune);
            architecturalRing(level, box, c, 2, c, 7, cracked);
            buildStairCornice(level, box, c, 1, c, Footprint.square(8), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 2, c, Footprint.square(10), 4, ModBlocks.ECHO_STONE_BRICK_WALL.get().defaultBlockState(), cracked);

            buildGabledPavilion(level, box, c, 1, c - 6, Footprint.of(6, 4), 5, 3, mossy, bricks, rune, lamp);
            buildStairCornice(level, box, c, 6, c - 6, Footprint.of(6, 4), ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ECHO_STONE_BRICK_SLAB.get().defaultBlockState(), true);

            for (int[] tower : new int[][]{{c - 8, c - 6, 9}, {c + 8, c - 5, 6}}) {
                architecturalPillar(level, box, tower[0], 1, tower[1], tower[2], bricks);
                architecturalPillar(level, box, tower[0] + 1, 1, tower[1], tower[2] - 2, cracked);

                buildSpireFinial(level, box, tower[0], tower[2] + 1, tower[1], 1, 3,
                        cracked, rune, lamp);
            }

            // 断拱残门：东侧只保留一半拱肋，给小遗迹一个可识别的破损入口。
            for (int y = 1; y <= 6; y++) {
                this.placeBlock(level, y % 2 == 0 ? cracked : bricks, c + 11, y, c + 2, box);
                if (y <= 4) {
                    this.placeBlock(level, bricks, c + 13, y, c + 2, box);
                }
            }
            for (int x = c + 11; x <= c + 13; x++) {
                this.placeBlock(level, x == c + 12 ? rune : cracked, x, 7, c + 2, box);
            }
            this.placeBlock(level, lamp, c + 12, 8, c + 2, box);

            architecturalDisk(level, box, c, 2, c, 4, bricks);
            architecturalRing(level, box, c, 3, c, 3, rune);
            buildAccessibleChest(level, random, box, c, 3, c,
                    ModLootTables.SMALL_ECHO_RUIN_CHEST);
            buildAccessibleChest(level, random, box, c - 7, 2, c + 5,
                    ModLootTables.T1_BASIC);
            buildTrueSightVaultNiche(level, random, box,
                    c + 8, 2, c + 6, Direction.WEST);
            scatterArchitecturalDebris(level, box, "small_echo_ruin", c, 1, c,
                    cracked, mossy);

            placeStructureSpawner(level, random, box, c + 5, 2, c - 9,
                    ModEntities.ECHO_REMNANT.get(), bricks, rune);
            anchorGroundColumns(level, box, 0, mossy);
            dressStructure(level, box, "small_echo_ruin");
            buildGroundEntry(level, box);
        }

        private void buildGroundEntry(WorldGenLevel level, BoundingBox box) {
            BlockState stairs = ModBlocks.ECHO_STONE_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH);
            for (int x = 17; x <= 18; x++) {
                this.placeBlock(level, stairs, x, 1, 2, box);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
