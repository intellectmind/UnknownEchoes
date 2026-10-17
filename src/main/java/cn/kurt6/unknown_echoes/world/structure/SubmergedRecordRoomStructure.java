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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 淹没记录室：镜湖水下的小型结构。 */
public class SubmergedRecordRoomStructure extends TerrainAnchoredStructure {
    public static final MapCodec<SubmergedRecordRoomStructure> CODEC = simpleCodec(SubmergedRecordRoomStructure::new);

    private static final int SIZE = 27;
    private static final int HEIGHT = 15;

    public SubmergedRecordRoomStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Piece piece = new Piece(context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        int floorY = LakeBedPlacement.findOceanFloor(context,
                footprint.minX(), footprint.minZ(), SIZE, SIZE, 6, 4);
        if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
            return java.util.Optional.empty();
        }
        piece.anchorToLakeFloor(floorY);
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(piece));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SUBMERGED_RECORD_ROOM.get();
    }

    public static class Piece extends EchoStructurePiece {

        /** 已计算出的湖底基准高度;<0 表示尚未计算(NBT 持久化,防跨区块重复下移)。 */
        private int lakeFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SUBMERGED_RECORD_ROOM_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SUBMERGED_RECORD_ROOM_PIECE.get(), tag);
            this.lakeFloorY = tag.contains("LakeFloorY") ? tag.getInt("LakeFloorY") : -1;
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("LakeFloorY", this.lakeFloorY);
            tag.putBoolean("Rejected", this.rejected);
        }

        public void anchorToLakeFloor(int floorY) {
            if (this.lakeFloorY >= 0) {
                return;
            }
            this.lakeFloorY = floorY;
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 5, 0);
        }

        /** 沉进湖床并校验水深:顶板(y4)与湖底齐平,只有 y5 残沿露出湖床一格。 */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.lakeFloorY >= 0) {
                return true;
            }
            int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE);
            if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
                this.rejected = true;
                return false;
            }
            this.lakeFloorY = floorY;

            if (!LakeBedPieceHelper.checkWaterDepthByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 6, 4)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.lakeFloorY - this.boundingBox.minY() - 5, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!adjustToLakeFloor(level, box, generator)) {
                return;
            }
            box = LakeBedPieceHelper.intersection(box, this.boundingBox);
            if (box == null) {
                return;
            }

            BlockState mirror = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState mirrorStone = ModBlocks.MIRROR_DUST_STONE.get().defaultBlockState();
            BlockState cracked = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            int c = SIZE / 2;

            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean wall = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                    this.placeBlock(level, random.nextFloat() < 0.3F ? mirrorStone : mirror, x, 0, z, box);
                    for (int y = 1; y <= 3; y++) {
                        this.placeBlock(level, wall ? (random.nextFloat() < 0.2F ? cracked : mirror) : water,
                                x, y, z, box);
                    }
                    boolean opening = (x == c && z >= c - 1 && z <= c + 2)
                            || (z == 1 && x >= c - 2 && x <= c + 2);
                    this.placeBlock(level, opening ? water
                            : (random.nextFloat() < 0.2F ? cracked : mirror), x, 4, z, box);
                }
            }

            for (int[] rim : new int[][]{{c - 1, c}, {c + 1, c}, {c - 1, c + 1}, {c + 1, c + 1},
                    {c, c - 1}, {c, c + 2}}) {
                if (random.nextFloat() < 0.7F) {
                    this.placeBlock(level, cracked, rim[0], 5, rim[1], box);
                }
            }

            // 必须用围合中庭:倒影回廊已占用镜尘族穹顶剪影。
            buildCourtyardCloister(level, box, c, 1, c, Footprint.square(10), 6, 3, mirrorStone, mirror, cracked, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());

            this.placeBlock(level, mirror, c, 1, c - 1, box);
            buildAccessibleChest(level, random, box, c, 2, c - 1, ModLootTables.SUBMERGED_RECORD_ROOM);
            buildAccessibleChest(level, random, box, c - 4, 2, c + 3, ModLootTables.MIRROR_LAKE_CACHE);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), 1, 1, 1, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), SIZE - 2, 1, SIZE - 2, box);
            this.placeBlock(level, runeBricks, 1, 3, SIZE - 2, box);
            buildTrueSightVaultNiche(level, random, box, SIZE - 4, 2, c + 3, Direction.WEST);

            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 9);
                int z = c + (int) Math.round(Math.sin(angle) * 9);
                architecturalPillar(level, box, x, 1, z, 6, mirror);
                this.placeBlock(level, ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState(),
                        x, 7, z, box);
            }
            architecturalRing(level, box, c, 5, c, 11, cracked);
            architecturalRing(level, box, c, 9, c, 8,
                    Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            BlockState recordStairs = ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState();
            BlockState recordSlab = ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, 6, c, Footprint.square(10), recordStairs, recordSlab, true);
            buildParapet(level, box, c, 7, c, Footprint.square(11), 4, ModBlocks.MIRROR_DUST_BRICK_WALL.get().defaultBlockState(), cracked);

            buildSpireFinial(level, box, c, 8, c, 2, 4,
                    mirror, cracked, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 5, c, 11, direction,
                        2, 5, mirror, ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState());
            }

            // 记录棱脊：四面水下墙体各留一根不同高度的记录柱，避免中庭和穹顶同形。
            for (int[] rib : new int[][]{{c - 9, c - 2, 9}, {c + 9, c + 1, 11},
                    {c - 2, c - 9, 8}, {c + 1, c + 9, 10}}) {
                for (int y = 8; y <= rib[2]; y++) {
                    this.placeBlock(level, y % 2 == 0 ? mirrorStone : mirror,
                            rib[0], y, rib[1], box);
                }
                this.placeBlock(level, runeBricks, rib[0], rib[2] + 1, rib[1], box);
            }

            placeSubmergedStructureSpawner(level, random, box, 5, 1, 5,
                    ModEntities.WATER_SHADOW.get(), mirrorStone, cracked);
            dressStructure(level, box, "submerged_record_room");
        }
    }
}
