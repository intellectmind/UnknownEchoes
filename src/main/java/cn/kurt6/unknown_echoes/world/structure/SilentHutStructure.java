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
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class SilentHutStructure extends SinglePieceStructure {
    public static final MapCodec<SilentHutStructure> CODEC = simpleCodec(SilentHutStructure::new);

    private static final int WIDTH = 29;
    private static final int DEPTH = 29;
    private static final int HEIGHT = 16;

    public SilentHutStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SILENT_HUT.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SILENT_HUT_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SILENT_HUT_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {

            if (!this.updateAverageGroundHeight(level, box, -2)) {
                return;
            }

            BlockState mossy = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.SILENT_MUDSTONE.get().defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    float roll = random.nextFloat();
                    BlockState floor = roll < 0.4F ? moss : (roll < 0.7F ? mossy : bricks);
                    this.placeBlock(level, floor, x, 0, z, box);
                }
            }
            for (int z = 0; z <= DEPTH / 2; z++) {
                this.placeBlock(level, moss, WIDTH / 2, 1, z, box);
            }

            this.generateBox(level, box, 1, 1, 1, WIDTH - 2, 4, DEPTH - 2, air, air, false);

            for (int y = 1; y <= 4; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    placeWall(level, box, random, x, y, 0, y);
                    placeWall(level, box, random, x, y, DEPTH - 1, y);
                }
                for (int z = 1; z < DEPTH - 1; z++) {
                    placeWall(level, box, random, 0, y, z, y);
                    placeWall(level, box, random, WIDTH - 1, y, z, y);
                }
            }

            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    boolean edge = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                    if (edge || random.nextFloat() < 0.3F) {
                        this.placeBlock(level, random.nextFloat() < 0.5F ? mossy : cracked, x, 5, z, box);
                    }
                }
            }

            this.placeBlock(level, air, WIDTH / 2, 1, 0, box);
            this.placeBlock(level, air, WIDTH / 2, 2, 0, box);
            this.placeBlock(level, air, WIDTH / 2, 3, 0, box);

            this.placeBlock(level, rune, WIDTH / 2, 2, DEPTH - 1, box);

            this.placeBlock(level, Blocks.COBWEB.defaultBlockState(), 1, 3, DEPTH - 2, box);
            this.placeBlock(level, moss, WIDTH - 2, 1, DEPTH - 2, box);
            this.placeBlock(level, rune, WIDTH / 2 - 4, 1, DEPTH / 2, box);
            this.placeBlock(level, rune, WIDTH / 2 + 4, 1, DEPTH / 2, box);

            int c = WIDTH / 2;
            // 必须用半塌残壳:项目里已有九座亭殿,再加一座就撞形。
            buildRuinedShell(level, box, c, 1, c, 9, 9, 6,
                    ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState(),
                    ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState(),
                    moss, ModBlocks.BOGLIGHT_STONE.get().defaultBlockState());
            buildStairCornice(level, box, c, 6, c, Footprint.square(9), ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SILENT_MUDSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 1, c, Footprint.square(12), 5, ModBlocks.SILENT_MUDSTONE_BRICK_WALL.get().defaultBlockState(), ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState());

            buildAccessibleChest(level, random, box, WIDTH / 2, 1, DEPTH - 2,
                    ModLootTables.SILENT_HUT_CHEST);
            buildAccessibleChest(level, random, box, 3, 1, DEPTH / 2,
                    ModLootTables.T2_RUIN);
            buildTrueSightVaultNiche(level, random, box,
                    WIDTH - 4, 1, DEPTH / 2 + 2, Direction.WEST);

            for (int z = 4; z <= DEPTH - 5; z++) {
                if (Math.abs(z - c) > 2) {
                    architecturalPillar(level, box, c - 5, 1, z, 4, bricks);
                    architecturalPillar(level, box, c + 5, 1, z, 4, mossy);
                }
            }
            architecturalRing(level, box, c, 6, c, 8,
                    ModBlocks.BOGLIGHT_STONE.get().defaultBlockState());
            for (int[] tower : new int[][]{{3, 3}, {WIDTH - 4, 3},
                    {3, DEPTH - 4}, {WIDTH - 4, DEPTH - 4}}) {
                architecturalPillar(level, box, tower[0], 1, tower[1], 9, bricks);

                buildSpireFinial(level, box, tower[0], 10, tower[1], 1, 3,
                        cracked, mossy,
                        ModBlocks.BOGLIGHT_STONE.get().defaultBlockState());
            }
            buildRuneCauseway(level, box, c, 0, c, Direction.SOUTH,
                    8, 14, 2, moss, mossy, rune);
            scatterArchitecturalDebris(level, random, box, c, 1, c,
                    10, 14, 24, cracked, mossy);

            placeStructureSpawner(level, random, box, c + 7, 1, c - 6,
                    ModEntities.SILENT_WALKER.get(), mossy,
                    ModBlocks.BOGLIGHT_STONE.get().defaultBlockState());
        }

        /** 墙体方块:下层更苔化,带随机坍塌缺口。 */
        private void placeWall(WorldGenLevel level, BoundingBox box, RandomSource random,
                               int x, int y, int z, int height) {
            if (random.nextFloat() < 0.08F && height >= 3) {
                return;
            }
            float mossChance = 0.7F - 0.15F * height;
            BlockState state;
            if (random.nextFloat() < mossChance) {
                state = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
            } else {
                state = random.nextFloat() < 0.25F
                        ? ModBlocks.SILENT_MUDSTONE.get().defaultBlockState()
                        : ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
            }
            this.placeBlock(level, state, x, y, z, box);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
