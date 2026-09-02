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
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 倒影回廊:镜湖"倒影入口"的落地实现(正典:镜面档案的阅览亭)。 */
public class ReflectionVaultStructure extends SinglePieceStructure {
    public static final MapCodec<ReflectionVaultStructure> CODEC = simpleCodec(ReflectionVaultStructure::new);

    private static final int SIZE = 23;
    private static final int HEIGHT = 40;

    public ReflectionVaultStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Piece piece = new Piece(context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        if (!LakeBedPlacement.hasSubmergedFootprint(context,
                footprint.minX(), footprint.minZ(), SIZE, 7, 5)) {
            return java.util.Optional.empty();
        }
        int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(
                context, footprint.minX(), footprint.minZ(), SIZE, SIZE);
        if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
            return java.util.Optional.empty();
        }
        piece.anchorToLakeFloor(floorY);
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(piece));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.REFLECTION_VAULT.get();
    }

    public static class Piece extends EchoStructurePiece {

        /** 已计算出的湖底基准高度;<0 表示尚未计算(随 NBT 持久化,避免跨区块重复下移)。 */
        private int lakeFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.REFLECTION_VAULT_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.REFLECTION_VAULT_PIECE.get(), tag);
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
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 1, 0);
        }

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
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 7, 5)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.lakeFloorY - this.boundingBox.minY() - 1, 0);
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
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState cracked = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            int c = SIZE / 2;

            for (int x = c - 4; x <= c + 4; x++) {
                for (int z = c - 4; z <= c + 4; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.3F ? mirrorStone : mirror, x, 0, z, box);
                }
            }

            // 侧墙必须整圈砌满,不能只砌四角:本结构 terrain_adaptation 是 none,
            // 角柱之间没写入的 60 格会留着湖床地形,整座密室在实机里读成一坨石头。
            for (int x = c - 3; x <= c + 3; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    boolean wall = x == c - 3 || x == c + 3 || z == c - 3 || z == c + 3;
                    if (!wall) {
                        continue;
                    }
                    boolean corner = (x == c - 3 || x == c + 3) && (z == c - 3 || z == c + 3);
                    for (int y = 1; y <= 3; y++) {
                        this.placeBlock(level,
                                corner ? mirror : (random.nextFloat() < 0.25F ? cracked : mirrorStone),
                                x, y, z, box);
                    }
                }
            }

            for (int x = c - 2; x <= c + 2; x++) {
                for (int z = c - 2; z <= c + 2; z++) {
                    for (int y = 1; y <= 3; y++) {
                        this.placeBlock(level, water, x, y, z, box);
                    }
                }
            }

            for (int x = c - 3; x <= c + 3; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : mirror, x, 4, z, box);
                }
            }

            this.placeBlock(level, mirror, c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 2, c, ModLootTables.MIRROR_LAKE_CACHE);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c, 2, c + 2, box);

            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();

            for (int x = c - 3; x <= c + 3; x++) {
                for (int z = c - 3; z <= c + 3; z++) {
                    this.placeBlock(level, anchored, x, -6, z, box);
                    boolean shaft = x == c && z == c + 2;
                    if (!shaft) {
                        this.placeBlock(level, anchored, x, -1, z, box);
                    }
                    boolean wall = x == c - 3 || x == c + 3 || z == c - 3 || z == c + 3;
                    for (int y = -5; y <= -2; y++) {
                        this.placeBlock(level, wall ? anchored : water, x, y, z, box);
                    }
                }
            }

            this.placeBlock(level, water, c, -1, c + 2, box);
            this.placeBlock(level, ModBlocks.TIDE_SENSOR_DOOR.get().defaultBlockState(), c, 0, c + 2, box);

            this.placeBlock(level, mirror, c, -5, c, box);
            buildAccessibleChest(level, random, box, c, -4, c, ModLootTables.REFLECTION_INNER_VAULT);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c - 2, -5, c - 2, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE.get().defaultBlockState(), c + 2, -5, c - 2, box);
            this.placeBlock(level, runeBricks, c - 2, -2, c + 2, box);
            this.placeBlock(level, runeBricks, c + 2, -2, c + 2, box);

            int seaLevel = level.getSeaLevel();
            int baseWorldY = this.getWorldY(5);
            for (int[] corner : new int[][]{{c - 3, c - 3}, {c + 3, c + 3}}) {
                int topWorldY = seaLevel + 1 + random.nextInt(2);
                for (int worldY = baseWorldY; worldY <= topWorldY; worldY++) {
                    int localY = worldY - this.getWorldY(0);
                    if (localY >= HEIGHT) {
                        break;
                    }
                    if (random.nextFloat() < 0.12F) {
                        continue;
                    }
                    this.placeBlock(level, random.nextFloat() < 0.35F ? cracked : mirror,
                            corner[0], localY, corner[1], box);
                }
            }

            for (int[] corner : new int[][]{{c + 3, c - 3}, {c - 3, c + 3}}) {
                int stubTop = baseWorldY + 2 + random.nextInt(3);
                for (int worldY = baseWorldY; worldY <= stubTop; worldY++) {
                    int localY = worldY - this.getWorldY(0);
                    if (localY < HEIGHT) {
                        this.placeBlock(level, mirror, corner[0], localY, corner[1], box);
                    }
                }
            }

            architecturalRing(level, box, c, 0, c, 10, mirrorStone);
            // 必须用穹顶圆厅:同群系已有五处柱环,再加一处就撞形。
            buildDomedRotunda(level, box, c, 1, c, 9, 7,
                    mirrorStone, mirror, cracked,
                    ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 0, c, Footprint.square(9), ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState(), false);
            buildParapet(level, box, c, 1, c, Footprint.square(10), 4, ModBlocks.MIRROR_DUST_BRICK_WALL.get().defaultBlockState(), cracked);
            buildStairCornice(level, box, c, 9, c, Footprint.square(8), ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState(), true);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 1, c, 10, direction,
                        2, 6, mirror, ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState());
            }
            architecturalRing(level, box, c, 10, c, 8,
                    Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());

            buildStairCornice(level, box, c, 11, c, Footprint.square(7), ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState(), true);
            buildSpireFinial(level, box, c, 12, c, 3, 5,
                    mirror, cracked, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            buildAccessibleChest(level, random, box, c + 7, 2, c - 4,
                    ModLootTables.MIRROR_LAKE_CACHE);

            placeSubmergedStructureSpawner(level, random, box, c - 7, 1, c + 5,
                    ModEntities.WATER_SHADOW.get(), mirrorStone, cracked);
        }
    }
}
