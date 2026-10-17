package cn.kurt6.unknown_echoes.world.structure;

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
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 主世界空间裂隙：细小的悬空裂纹，与世界事件裂隙共用紫水晶和末地烛表现。 */
public class SpatialRiftStructure extends TerrainAnchoredStructure {
    public static final MapCodec<SpatialRiftStructure> CODEC = simpleCodec(SpatialRiftStructure::new);

    private static final int WIDTH = 25;
    private static final int DEPTH = 25;
    private static final int HEIGHT = 32;

    public SpatialRiftStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SPATIAL_RIFT.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SPATIAL_RIFT_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SPATIAL_RIFT_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            int c = WIDTH / 2;
            BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
            BlockState budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
            BlockState calcite = Blocks.CALCITE.defaultBlockState();
            BlockState glow = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState rod = Blocks.END_ROD.defaultBlockState();
            BlockState obsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1,
                    air, air, false);

            architecturalDisk(level, box, c, 0, c, 11, calcite);
            architecturalDisk(level, box, c, 1, c, 8, Blocks.SMOOTH_BASALT.defaultBlockState());
            architecturalRing(level, box, c, 2, c, 7, obsidian);
            architecturalRing(level, box, c, 2, c, 4, amethyst);
            buildRuneCauseway(level, box, c, 1, c, Direction.SOUTH,
                    7, 11, 1, calcite, Blocks.SMOOTH_BASALT.defaultBlockState(), amethyst);

            buildStairCornice(level, box, c, 1, c, Footprint.square(10), Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState(), Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(11), 5, Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), obsidian);

            int[][] pylons = {{c - 8, c - 7, 11}, {c + 8, c - 7, 8},
                    {c - 8, c + 7, 7}, {c + 8, c + 7, 13}};
            for (int[] pylon : pylons) {
                architecturalPillar(level, box, pylon[0], 1, pylon[1], pylon[2],
                        Blocks.SMOOTH_BASALT.defaultBlockState());
                architecturalPillar(level, box, pylon[0] + Integer.signum(c - pylon[0]), 2,
                        pylon[1] + Integer.signum(c - pylon[1]), pylon[2] - 2, obsidian);

                buildSpireFinial(level, box, pylon[0], pylon[2] + 1, pylon[1], 1, 3,
                        obsidian, Blocks.SMOOTH_BASALT.defaultBlockState(), glow);
            }

            for (int y = 4; y < HEIGHT - 2; y++) {
                int drift = (int) Math.round(Math.sin(y / 3.2D) * 2.5D);
                BlockState spine = y % 5 == 0 ? budding : y % 3 == 0 ? glow : amethyst;
                this.placeBlock(level, spine, c + drift, y, c, box);
                if (y >= 8 && y <= 24 && y % 2 == 0) {
                    this.placeBlock(level, amethyst, c - 3 - drift / 2, y - 2, c + 1, box);
                }
                if (y >= 11 && y <= 27 && y % 3 != 0) {
                    this.placeBlock(level, glow, c + 4 + drift / 2, y - 3, c - 1, box);
                }
                if (y % 4 == 0) {
                    this.placeBlock(level, rod, c + drift, y, c + 2, box);
                    this.placeBlock(level, rod, c + drift, y, c - 2, box);
                }
            }
            architecturalRing(level, box, c, 10, c, 5, amethyst);
            architecturalRing(level, box, c, 19, c, 7, glow);
            architecturalRing(level, box, c, 27, c, 4, obsidian);

            // 断裂悬桥：碎片沿不同高度错位，强调裂隙正在把建筑拉开。
            for (int i = 0; i < 6; i++) {
                int x = c - 10 + i * 3;
                int z = c + 7 - (i % 2) * 2;
                int y = 12 + i * 2;
                this.placeBlock(level, i % 3 == 0 ? budding : calcite, x, y, z, box);
                this.placeBlock(level, obsidian, x + 1, y, z, box);
                if (i % 2 == 0) {
                    this.placeBlock(level, rod, x, y + 1, z, box);
                }
            }
            for (int i = 0; i < 4; i++) {
                this.placeBlock(level, glow, c + 6, 15 + i * 3, c - 8 + i * 2, box);
            }

            buildAccessibleChest(level, random, box, c + 7, 2, c + 6,
                    ModLootTables.SPATIAL_RIFT_CHEST);
            scatterArchitecturalDebris(level, box, "spatial_rift", c, 2, c,
                    calcite, amethyst);

            buildProjectionVaultNiche(level, random, box, c, 1, c - 6, Direction.NORTH,
                    amethyst, ModLootTables.PUZZLE_SPATIAL_RIFT_CHEST);
            anchorGroundColumns(level, box, 0, calcite);
            dressStructure(level, box, "spatial_rift");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
