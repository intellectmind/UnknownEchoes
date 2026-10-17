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

public class BrokenArchiveStructure extends TerrainAnchoredStructure {
    public static final MapCodec<BrokenArchiveStructure> CODEC = simpleCodec(BrokenArchiveStructure::new);

    private static final int SIZE = 65;
    private static final int HEIGHT = 42;

    public BrokenArchiveStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BROKEN_ARCHIVE.get();
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] BROKEN_ARCHIVE_ORE_NODES = {
                {-10, 0}, {-8, -6}, {-5, -9}, {0, -10}, {5, -9}, {8, -6},
                {10, 0}, {8, 6}, {5, 9}, {0, 10}, {-5, 9}, {-8, 6}
        };

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.BROKEN_ARCHIVE_PIECE.get(), x, 64, z,
                    SIZE, HEIGHT, SIZE, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.BROKEN_ARCHIVE_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearStructureSpace(level, box, 0);
            BlockState rusted = ModBlocks.RUSTED_BELLSTONE.get().defaultBlockState();
            BlockState stone = ModBlocks.ASHEN_BELLSTONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CLOCKWORK_SCRAP_BLOCK.get().defaultBlockState();
            BlockState shelf = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            int c = SIZE / 2;

            int[] radii = {26, 28, 30};
            for (int y = 0; y < radii.length; y++) {
                int r = radii[y];
                for (int x = 0; x < SIZE; x++) {
                    for (int z = 0; z < SIZE; z++) {
                        int dx = x - c;
                        int dz = z - c;
                        if (dx * dx + dz * dz <= r * r) {
                            this.placeBlock(level,
                                    random.nextFloat() < 0.3F ? rusted : stone, x, y, z, box);
                        }
                    }
                }
            }
            int top = radii.length;

            buildRibbedVaultHall(level, box, c, top + 1, c, Footprint.of(16, 25), 14, 6, stone, bricks, rusted, rune);
            buildTwinTowerFacade(level, box, c, top + 1, c - 27,
                    22, 4, 26, bricks, rusted, rune);
            buildArcadedWing(level, box, c - 23, top + 1, c, Footprint.of(7, 16), 11, 4, stone, bricks, cracked, rune);
            buildArcadedWing(level, box, c + 23, top + 1, c, Footprint.of(7, 16), 11, 4, stone, bricks, cracked, rune);
            buildBossApse(level, box, c, top + 1, c + 23,
                    Direction.SOUTH, 8, 12, stone, bricks, rusted, rune);
            buildTieredTower(level, box, c + 24, top + 1, c + 17,
                    5, 26, bricks, rusted, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState());

            for (int z = c - 11; z <= c - 4; z++) {
                this.placeBlock(level, rune, c, top, z, box);
                if (z % 3 == 0) {
                    this.placeBlock(level, cracked, c - 1, top, z, box);
                    this.placeBlock(level, cracked, c + 1, top, z, box);
                }
            }

            for (int x = c - 7; x <= c + 7; x++) {
                for (int z = c - 7; z <= c + 7; z++) {
                    boolean wall = x == c - 7 || x == c + 7 || z == c - 7 || z == c + 7;
                    if (!wall) {
                        continue;
                    }
                    int wallHeight = 2 + random.nextInt(3);
                    for (int y = 0; y < wallHeight; y++) {
                        this.placeBlock(level, random.nextFloat() < 0.35F ? cracked : bricks,
                                x, top + y, z, box);
                    }
                }
            }

            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, top, c - 7, box);
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, top + 1, c - 7, box);

            for (int x = c - 5; x <= c + 5; x++) {
                if (random.nextFloat() < 0.7F) {
                    this.placeBlock(level, shelf, x, top, c + 5, box);
                }
            }
            this.placeBlock(level, rune, c, top + 1, c + 6, box);

            buildAccessibleChest(level, random, box, c - 2, top, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            buildAccessibleChest(level, random, box, c + 5, top, c - 2, ModLootTables.T4_REGION_CORE);
            buildTrueSightVaultNiche(level, random, box, c - 6, top, c + 2, Direction.EAST);

            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, top, c + 4, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c + 4), this.getWorldY(top), this.getWorldZ(c, c + 4));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("broken_bell_keeper"), 3);
            }
            // 肋拱厅的柱列会插进 13 格战斗半径,建筑摆完后统一清出净空。
            clearArenaVolume(level, box, c, top, c + 4, 13, 1, 6);
            buildBossArenaMandala(level, box, c, top, c + 4, 13,
                    rusted, cracked, rune);
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            // 批量照明必须用不可破坏的琉璃:共鸣烛可打碎,当唯一光源会让战斗区失明。
            BlockState bellLamp = ModBlocks.BELL_LANTERN_GLASS.get().defaultBlockState();
            buildArenaLightRing(level, box, c, top + 2, c + 4, 11, 12, 10, bricks, bellLamp);
            illuminateInterior(level, box, c, c, top + 18, 14, 22, 6, bellLamp);
            buildStairCornice(level, box, c, top + 15, c, Footprint.of(16, 25), ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.ASHEN_BELLSTONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildCornice(level, box, c, top + 16, c, Footprint.of(16, 25), bricks, rusted);
            buildParapet(level, box, c, top + 17, c, Footprint.of(16, 25), 8, ModBlocks.ASHEN_BELLSTONE_BRICK_WALL.get().defaultBlockState(), rusted);

            // 断裂钟摆：档案馆中央偏轴悬下一根残钟脊柱，打破普通钟塔的对称立面。
            int pendulumX = c + 18;
            int pendulumZ = c - 17;
            for (int y = 20; y <= 34; y++) {
                this.placeBlock(level, y % 3 == 0 ? rune : rusted, pendulumX, y, pendulumZ, box);
            }
            for (int x = pendulumX - 5; x <= pendulumX + 5; x++) {
                if (Math.abs(x - pendulumX) <= 1 || x % 2 == 0) {
                    this.placeBlock(level, bricks, x, 27, pendulumZ, box);
                }
            }
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                    pendulumX, 35, pendulumZ, box);

            for (int i = 1; i <= 8; i++) {
                if (random.nextFloat() < 1.0F - i * 0.2F) {
                    this.placeBlock(level, i < 4 ? bricks : cracked, c + 7 + i, top - 1, c, box);
                }
            }

            architecturalRing(level, box, c, top - 1, c, 17, rusted);

            buildCircularColonnade(level, box, c, top, c, 15, 12, 9,
                    bricks, candle, cracked);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, top, c, 17, direction,
                        3, 8, bricks, ModBlocks.CHISELED_ECHO_STRATA.get().defaultBlockState());
            }
            for (int[] tower : new int[][]{{c - 15, c - 15}, {c + 15, c - 15},
                    {c - 15, c + 15}, {c + 15, c + 15}}) {
                architecturalPillar(level, box, tower[0], top, tower[1], 14, bricks);
                this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(),
                        tower[0], top + 14, tower[1], box);
            }
            buildRuneCauseway(level, box, c, top, c, Direction.SOUTH,
                    8, 19, 2, stone, rusted, rune);
            scatterArchitecturalDebris(level, box, "broken_archive", c, top, c,
                    cracked, rusted);

            placeStructureSpawner(level, random, box, c - 10, top, c + 8,
                    ModEntities.BROKEN_BELL_GUARD.get(), stone, cracked);
            for (int[] node : BROKEN_ARCHIVE_ORE_NODES) {
                this.placeBlock(level, ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState(),
                        c + node[0], top - 1, c + node[1], box);
            }
            anchorGroundColumns(level, box, 0, stone);
            dressStructure(level, box, "broken_archive");
            buildGroundEntry(level, box);
        }

        private void buildGroundEntry(WorldGenLevel level, BoundingBox box) {
            BlockState bricks = ModBlocks.ASHEN_BELLSTONE_BRICKS.get().defaultBlockState();
            BlockState stairs = ModBlocks.ASHEN_BELLSTONE_BRICK_STAIRS.get().defaultBlockState();
            for (int side : new int[]{-1, 1}) {
                for (int distance = 1; distance <= 3; distance++) {
                    int x = 32 + side * distance;
                    int y = 3 - distance;
                    for (int support = 0; support < y; support++) {
                        this.placeBlock(level, bricks, x, support, 63, box);
                    }
                    Direction facing = distance == 1 ? Direction.SOUTH
                            : side < 0 ? Direction.EAST : Direction.WEST;
                    this.placeBlock(level, stairs.setValue(StairBlock.FACING, facing), x, y, 63, box);
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
