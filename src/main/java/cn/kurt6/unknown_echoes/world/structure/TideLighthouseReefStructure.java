package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock;
import cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 潮汐灯塔礁：镜湖深水区的环形礁盘与半倒塌灯塔废墟。 */
public class TideLighthouseReefStructure extends SinglePieceStructure {
    public static final MapCodec<TideLighthouseReefStructure> CODEC =
            simpleCodec(TideLighthouseReefStructure::new);

    private static final int SIZE = 41;
    private static final int HEIGHT = 22;

    public TideLighthouseReefStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Piece piece = new Piece(context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        if (!LakeBedPlacement.hasSubmergedFootprint(context,
                footprint.minX(), footprint.minZ(), SIZE, 11, 6)) {
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
        return ModStructures.TIDE_LIGHTHOUSE_REEF.get();
    }

    public static class Piece extends EchoStructurePiece {

        private int oceanFloorY = -1;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.TIDE_LIGHTHOUSE_REEF_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.TIDE_LIGHTHOUSE_REEF_PIECE.get(), tag);
            this.oceanFloorY = tag.getInt("OceanFloorY");
            if (!tag.contains("OceanFloorY")) {
                this.oceanFloorY = -1;
            }
            this.rejected = tag.getBoolean("Rejected");
        }

        public void anchorToLakeFloor(int floorY) {
            if (this.oceanFloorY >= 0) {
                return;
            }
            this.oceanFloorY = floorY;
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 1, 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("Rejected", this.rejected);
        }

        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.oceanFloorY >= 0) {
                return true;
            }
            int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE);
            if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
                this.rejected = true;
                return false;
            }
            this.oceanFloorY = floorY;

            if (!LakeBedPieceHelper.checkWaterDepthByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), SIZE, SIZE, 11, 6)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.oceanFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.adjustToLakeFloor(level, box, generator)) {
                return;
            }
            box = LakeBedPieceHelper.intersection(box, this.boundingBox);
            if (box == null) {
                return;
            }

            BlockState bricks = ModBlocks.REEF_LIMESTONE_BRICKS.get().defaultBlockState();
            BlockState stone = ModBlocks.REEF_LIMESTONE.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState deepslate = ModBlocks.TIDEWORN_REEFSTONE.get().defaultBlockState();
            BlockState buoy = ModBlocks.TIDE_BUOY.get().defaultBlockState()
                    .setValue(TideBuoyBlock.LIT, Boolean.TRUE)
                    .setValue(TideBuoyBlock.WATERLOGGED, Boolean.TRUE);
            BlockState seat = ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState()
                    .setValue(TideRuneSeatBlock.LIT, Boolean.TRUE)
                    .setValue(TideRuneSeatBlock.WATERLOGGED, Boolean.TRUE);

            BlockState chain = Blocks.CHAIN.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, Boolean.TRUE);
            BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
            int c = SIZE / 2;

            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    int distSq = dx * dx + dz * dz;
                    if (distSq >= 64 && distSq <= 121 && random.nextFloat() < 0.85F) {
                        BlockState reef = random.nextFloat() < 0.5F ? deepslate
                                : (random.nextFloat() < 0.5F ? stone : bricks);
                        this.placeBlock(level, reef, x, 1, z, box);
                        if (random.nextFloat() < 0.3F) {
                            this.placeBlock(level, deepslate, x, 2, z, box);
                        }
                    }

                    if (distSq < 64 && random.nextFloat() < 0.4F) {
                        this.placeBlock(level, random.nextFloat() < 0.4F ? bricks : stone, x, 1, z, box);
                    }
                }
            }

            for (int y = 1; y <= 16; y++) {
                float keep = Math.max(0.48F, 1.0F - (y - 2) * 0.045F);
                int radius = y <= 10 ? 4 : 3;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        boolean wall = Math.abs(dx) == radius || Math.abs(dz) == radius;
                        if (!wall) {
                            continue;
                        }

                        if (dx == radius && Math.abs(dz) <= 1 && y <= 3) {
                            continue;
                        }
                        if (random.nextFloat() < keep) {
                            this.placeBlock(level, y % 3 == 0 && random.nextFloat() < 0.3F
                                    ? runeBricks : bricks, c + dx, y, c + dz, box);
                        }
                    }
                }
            }

            this.placeBlock(level, lantern, c, 17, c, box);
            this.placeBlock(level, bricks, c, 16, c, box);

            BlockState reefStairs = ModBlocks.REEF_LIMESTONE_BRICK_STAIRS.get().defaultBlockState();
            BlockState reefSlab = ModBlocks.REEF_LIMESTONE_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, 10, c, Footprint.square(4), reefStairs, reefSlab, true);
            buildStairCornice(level, box, c, 15, c, Footprint.square(3), reefStairs, reefSlab, true);
            buildParapet(level, box, c, 16, c, Footprint.square(4), 3, ModBlocks.REEF_LIMESTONE_BRICK_WALL.get().defaultBlockState(), runeBricks);

            int[][] buoyOffsets = {{8, 0}, {-4, 7}, {-4, -7}};
            for (int[] offset : buoyOffsets) {
                int bx = c + offset[0];
                int bz = c + offset[1];
                this.placeBlock(level, deepslate, bx, 1, bz, box);
                for (int y = 2; y <= 4; y++) {
                    this.placeBlock(level, chain, bx, y, bz, box);
                }
                this.placeBlock(level, buoy, bx, 5, bz, box);
            }

            this.placeBlock(level, seat, c - 8, 2, c, box);
            this.placeBlock(level, bricks, c + 3, 1, c + 3, box);
            this.placeBlock(level, bricks, c + 3, 2, c + 3, box);
            this.placeBlock(level, bricks, c + 3, 3, c + 3, box);
            this.placeBlock(level, seat, c + 3, 4, c + 3, box);
            this.placeBlock(level, deepslate, c + 5, 1, c - 6, box);
            this.placeBlock(level, deepslate, c + 5, 2, c - 6, box);
            this.placeBlock(level, seat, c + 5, 3, c - 6, box);

            buildAccessibleChest(level, random, box, c - 1, 1, c - 1,
                    ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);

            architecturalRing(level, box, c, 1, c, 18, deepslate);
            architecturalRing(level, box, c, 2, c, 15, bricks);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6.0D;
                int x = c + (int) Math.round(Math.cos(angle) * 17);
                int z = c + (int) Math.round(Math.sin(angle) * 17);
                int height = 5 + i % 4;
                architecturalPillar(level, box, x, 1, z, height, bricks);
                this.placeBlock(level, ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState(),
                        x, height + 1, z, box);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildRuneCauseway(level, box, c, 1, c, direction,
                        10, 19, 2, stone, deepslate, runeBricks);
                int x = c + direction.getStepX() * 18;
                int z = c + direction.getStepZ() * 18;
                architecturalPillar(level, box, x, 1, z, 11, bricks);
                this.placeBlock(level, lantern, x, 12, z, box);
            }
            buildAccessibleChest(level, random, box, c + 13, 2, c + 4,
                    ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);

            placeSubmergedStructureSpawner(level, random, box, c - 12, 2, c - 9,
                    ModEntities.WATER_SHADOW.get(), deepslate, bricks);
            buildBossArenaMandala(level, box, c, 1, c, 14,
                    runeBricks, bricks, ModBlocks.TIDELIGHT_CORAL_BLOCK.get().defaultBlockState());
            buildArenaLightRing(level, box, c, 2, c, 12, 12, 4, bricks,
                    ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());

            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(0), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {

                // 刷怪器埋在局部 y=0,主体从抬高后的灯塔冠部上方出生。
                spawner.configure(UnknownEchoes.id("tide_lantern_keeper"), 19);
                spawner.setChallengeGate(EchoAbilityType.TIDE_ECHO,
                        UnknownEchoes.id("abyss_watcher"),
                        "message.unknown_echoes.tide_lighthouse.locked");
            }
        }
    }
}
