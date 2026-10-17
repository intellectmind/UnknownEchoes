package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

public class SilentAltarStructure extends TerrainAnchoredStructure {
    public static final MapCodec<SilentAltarStructure> CODEC = simpleCodec(SilentAltarStructure::new);

    private static final int SIZE = 65;
    private static final int HEIGHT = 40;

    public SilentAltarStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SILENT_ALTAR.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SILENT_ALTAR_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SILENT_ALTAR_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            clearStructureSpace(level, box, 0);
            BlockState mossy = ModBlocks.ROOTBOUND_MUDSTONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.SILENT_MUDSTONE_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.BOGLIGHT_STONE.get().defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            int c = SIZE / 2;

            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= 900) {
                        BlockState floor = distSq >= 784 && random.nextFloat() < 0.5F ? moss
                                : (random.nextFloat() < 0.4F ? mossy : bricks);
                        this.placeBlock(level, floor, x, 0, z, box);
                    }
                }
            }

            buildRibbedVaultHall(level, box, c, 1, c + 3, Footprint.of(16, 25), 13, 6, bricks, mossy, chiseled, chiseled);
            buildTwinTowerFacade(level, box, c, 1, c - 27,
                    22, 4, 28, bricks, chiseled, chiseled);
            buildArcadedWing(level, box, c - 23, 1, c + 2, Footprint.of(7, 17), 11, 4, moss, bricks, mossy, chiseled);
            buildArcadedWing(level, box, c + 23, 1, c + 2, Footprint.of(7, 17), 11, 4, moss, bricks, mossy, chiseled);
            buildBossApse(level, box, c, 1, c + 24,
                    Direction.SOUTH, 8, 12, bricks, mossy, chiseled, chiseled);
            buildTieredTower(level, box, c - 24, 1, c + 18,
                    5, 27, bricks, chiseled, chiseled);
            // coreRadius=16 必须大于场地战斗半径 15,否则退台会收进走位区。
            buildSteppedZiggurat(level, box, c, 1, c, 27, 3, 3, 16,
                    bricks, mossy,
                    ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(),
                    ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());

            // 肋拱厅的柱列会插进 15 格战斗半径,建筑摆完后统一清出净空。
            clearArenaVolume(level, box, c, 1, c, 15, 1, 5);
            buildBossArenaMandala(level, box, c, 0, c, 15,
                    bricks, mossy, chiseled);

            buildArenaLightRing(level, box, c, 3, c, 13, 12, 10, bricks, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
            illuminateInterior(level, box, c, c + 3, 18, 14, 22, 6, ModBlocks.HUSH_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 15, c + 3, Footprint.of(16, 25), ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.SILENT_MUDSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 16, c + 3, Footprint.of(16, 25), 8, ModBlocks.SILENT_MUDSTONE_BRICK_WALL.get().defaultBlockState(), mossy);

            for (int x = c - 1; x <= c + 1; x++) {
                for (int z = c - 1; z <= c + 1; z++) {
                    this.placeBlock(level, x == c && z == c ? chiseled : mossy, x, 1, z, box);
                }
            }

            List<BlockPos> candlePositions = new ArrayList<>();
            for (int[] corner : new int[][]{{c - 5, c - 5}, {c + 5, c - 5}, {c - 5, c + 5}, {c + 5, c + 5}}) {
                this.placeBlock(level, bricks, corner[0], 1, corner[1], box);
                this.placeBlock(level, chiseled, corner[0], 2, corner[1], box);
                this.placeBlock(level, candle, corner[0], 3, corner[1], box);
                candlePositions.add(new BlockPos(
                        this.getWorldX(corner[0], corner[1]),
                        this.getWorldY(3),
                        this.getWorldZ(corner[0], corner[1])));
            }

            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                int x = c + (int) Math.round(Math.cos(angle) * 7.5);
                int z = c + (int) Math.round(Math.sin(angle) * 7.5);
                if (Math.abs(x - c) == 5 && Math.abs(z - c) == 5) {
                    continue;
                }
                if (random.nextFloat() < 0.6F) {
                    int height = 1 + random.nextInt(3);
                    for (int y = 1; y <= height; y++) {
                        this.placeBlock(level, random.nextFloat() < 0.5F ? mossy : bricks, x, y, z, box);
                    }
                }
            }

            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(0), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("silent_priest"), 2);
                spawner.setResetBlocks(UnknownEchoes.id("resonance_candle"), candlePositions);
            }

            buildAccessibleChest(level, random, box, c, 1, c - 7, ModLootTables.T5_MINIBOSS_REPEAT);
            buildTrueSightVaultNiche(level, random, box, c + 6, 1, c + 2, Direction.WEST);

            // 悬冠回音环：高于 Boss 净空的错位冠环保留静默祭坛的垂直焦点。
            architecturalRing(level, box, c, 22, c, 12, chiseled);
            for (int[] blade : new int[][]{{-10, 0, 5}, {10, 0, 4}, {0, -10, 6}, {0, 10, 3}}) {
                for (int y = 17; y < 17 + blade[2]; y++) {
                    this.placeBlock(level, y % 2 == 0 ? bricks : mossy,
                            c + blade[0], y, c + blade[1], box);
                }
                this.placeBlock(level, candle, c + blade[0], 17 + blade[2], c + blade[1], box);
            }

            architecturalRing(level, box, c, 1, c, 29,
                    ModBlocks.SILENT_MUDSTONE.get().defaultBlockState());
            buildCircularColonnade(level, box, c, 1, c, 17, 16, 9,
                    bricks, chiseled, mossy);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 29, direction,
                        3, 8, bricks, chiseled);
            }
            buildRuneCauseway(level, box, c, 0, c, Direction.NORTH,
                    18, 31, 3, moss, mossy, chiseled);
            scatterArchitecturalDebris(level, box, "silent_altar", c, 1, c,
                    mossy, ModBlocks.SILENT_MUDSTONE.get().defaultBlockState());

            placeStructureSpawner(level, random, box, c - 10, 1, c - 9,
                    ModEntities.SILENT_WALKER.get(), mossy, chiseled);

            buildStepPathSanctum(level, random, box, c - 12, 1, c + 2, Direction.SOUTH,
                    bricks, mossy, chiseled, ModLootTables.T3_PUZZLE);
            anchorGroundColumns(level, box, 0, mossy);
            dressStructure(level, box, "silent_altar");
            buildGroundEntry(level, box);
        }

        private void buildGroundEntry(WorldGenLevel level, BoundingBox box) {
            BlockState stairs = ModBlocks.SILENT_MUDSTONE_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.SOUTH);
            for (int x = 30; x <= 32; x++) {
                for (int z = 60; z <= 61; z++) {
                    this.placeBlock(level, stairs, x, 62 - z, z, box);
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
