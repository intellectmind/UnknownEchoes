package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class MirrorDustCloisterStructure extends SinglePieceStructure {
    public static final MapCodec<MirrorDustCloisterStructure> CODEC =
            simpleCodec(MirrorDustCloisterStructure::new);

    private static final int WIDTH = 15;
    private static final int DEPTH = 34;
    private static final int HEIGHT = 24;
    /** 地下部分深度:地表在局部 y=GROUND_Y。 */
    private static final int GROUND_Y = 13;

    public MirrorDustCloisterStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MIRROR_DUST_CLOISTER.get();
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] MIRROR_CLOISTER_NODES = {
                {3, 0, 22}, {1, 0, 22}, {11, 0, 22},
                {3, 0, 26}, {13, 0, 22},
                {3, 0, 30}, {7, 0, 30}, {11, 0, 30},
                {1, 2, 26}, {13, 2, 26}
        };

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.MIRROR_DUST_CLOISTER_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.MIRROR_DUST_CLOISTER_PIECE.get(), tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {

            if (!this.updateAverageGroundHeight(level, box, -GROUND_Y)) {
                return;
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState mirrorDecor = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();

            BlockState prism = ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int cx = WIDTH / 2;

            this.generateBox(level, box, cx - 2, 0, 1, cx + 2, GROUND_Y + 1, 5, bricks, bricks, false);
            this.generateBox(level, box, cx - 1, 1, 2, cx + 1, GROUND_Y + 1, 4, air, air, false);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        this.placeBlock(level, bricks, cx + dx, GROUND_Y + 1, 3 + dz, box);
                    }
                }
            }
            for (int[] post : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                this.placeBlock(level, bricks, cx + post[0], GROUND_Y + 2, 3 + post[1], box);
                this.placeBlock(level, runeBricks, cx + post[0], GROUND_Y + 3, 3 + post[1], box);
            }

            this.placeBlock(level, air, cx, GROUND_Y + 1, 1, box);

            int[][] spiral = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
            int y = GROUND_Y;
            int idx = 0;
            while (y >= 2) {
                int[] cell = spiral[idx % spiral.length];
                this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : bricks,
                        cx + cell[0], y, 3 + cell[1], box);
                idx++;
                y--;
            }

            this.placeBlock(level, runeBricks, cx, 0, 3, box);
            this.placeBlock(level, cracked, cx, 0, 4, box);

            this.generateBox(level, box, cx - 1, 1, 5, cx + 1, 3, 6, air, air, false);
            this.placeBlock(level, bricks, cx - 1, 0, 5, box);
            this.placeBlock(level, bricks, cx, 0, 5, box);
            this.placeBlock(level, bricks, cx + 1, 0, 5, box);

            this.generateBox(level, box, cx - 3, 0, 6, cx + 3, 5, 19, bricks, bricks, false);
            this.generateBox(level, box, cx - 2, 1, 6, cx + 2, 4, 19, air, air, false);
            for (int z = 6; z <= 19; z++) {

                for (int dx = -2; dx <= 2; dx++) {
                    BlockState floor = bricks;
                    if (dx == ((z % 4 < 2) ? 0 : (z % 8 < 4 ? 1 : -1))) {
                        floor = cracked;
                    } else if (random.nextFloat() < 0.2F) {
                        floor = mirrorDecor;
                    }
                    this.placeBlock(level, floor, cx + dx, 0, z, box);
                }

                if (random.nextFloat() < 0.35F) {
                    this.placeBlock(level, mirrorDecor, cx - 2, 1 + random.nextInt(3), z, box);
                }
                if (random.nextFloat() < 0.35F) {
                    this.placeBlock(level, mirrorDecor, cx + 2, 1 + random.nextInt(3), z, box);
                }

                if (z % 4 == 2) {
                    this.placeBlock(level, runeBricks, cx, 4, z, box);
                }
                if (z % 4 == 0) {
                    this.placeBlock(level, prism, cx, 4, z, box);
                }
            }

            BlockState cloisterStairs = ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState();
            BlockState westTrim = orientStairs(cloisterStairs, Direction.EAST, Half.TOP);
            BlockState eastTrim = orientStairs(cloisterStairs, Direction.WEST, Half.TOP);
            for (int z = 7; z <= 18; z++) {
                this.placeBlock(level, westTrim, cx - 2, 4, z, box);
                this.placeBlock(level, eastTrim, cx + 2, 4, z, box);
            }

            this.generateBox(level, box, cx - 6, 0, 8, cx - 3, 4, 10, bricks, bricks, false);
            sealHiddenBox(level, box, anchored, cx - 6, cx - 3, 8, 10, 0, 4);
            this.generateBox(level, box, cx - 5, 1, 9, cx - 4, 3, 9, air, air, false);
            for (int yy = 1; yy <= 2; yy++) {
                this.placeBlock(level, hidden, cx - 3, yy, 9, box);
            }
            buildAccessibleChest(level, random, box, cx - 5, 1, 9, ModLootTables.ECHO_TEMPLE_HIDDEN);

            this.generateBox(level, box, cx + 3, 0, 13, cx + 5, 4, 15, bricks, bricks, false);
            this.placeBlock(level, air, cx + 4, 1, 14, box);
            this.placeBlock(level, air, cx + 4, 2, 14, box);
            for (int yy = 1; yy <= 2; yy++) {
                this.placeBlock(level, hidden, cx + 3, yy, 14, box);
            }
            this.placeBlock(level, cracked, cx + 4, 0, 14, box);

            this.placeBlock(level, Blocks.SPAWNER.defaultBlockState(), cx - 3, 1, 17, box);
            BlockPos decoyPos = new BlockPos(this.getWorldX(cx - 3, 17), this.getWorldY(1),
                    this.getWorldZ(cx - 3, 17));
            if (box.isInside(decoyPos) && level.getBlockEntity(decoyPos)
                    instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity decoySpawner) {
                decoySpawner.setEntityId(ModEntities.MIRROR_DUST_DECOY.get(), random);
            }

            for (int dx = -2; dx <= 2; dx++) {
                for (int yy = 1; yy <= 12; yy++) {
                    boolean door = Math.abs(dx) <= 0 && yy <= 2;
                    this.placeBlock(level, door ? hidden : bricks, cx + dx, yy, 19, box);
                }
            }

            this.generateBox(level, box, 0, 0, 20, WIDTH - 1, 15, DEPTH - 1, bricks, bricks, false);
            sealHiddenBox(level, box, anchored, 0, WIDTH - 1, 20, DEPTH - 1, 0, 15);
            this.generateBox(level, box, 1, 1, 21, WIDTH - 2, 14, DEPTH - 2, air, air, false);

            this.placeBlock(level, air, cx, 1, 20, box);
            this.placeBlock(level, air, cx, 2, 20, box);
            int roomCz = 26;

            for (int z = 22; z <= 30; z += 2) {
                for (int yy = 1; yy <= 12; yy++) {
                    this.placeBlock(level, mirrorDecor, 1, yy, z, box);
                    this.placeBlock(level, mirrorDecor, WIDTH - 2, yy, z, box);
                }
            }
            for (int x = 3; x <= WIDTH - 4; x += 2) {
                for (int yy = 1; yy <= 12; yy++) {
                    this.placeBlock(level, mirrorDecor, x, yy, DEPTH - 2, box);
                }
            }

            for (int x = 1; x <= WIDTH - 2; x++) {
                for (int z = 21; z <= DEPTH - 2; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? mirrorDecor : bricks, x, 0, z, box);
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.placeBlock(level, cracked, cx + dx, 0, roomCz + dz, box);
                }
            }

            for (int[] station : new int[][]{{0, -4}, {4, 3}, {-4, 3}}) {
                this.placeBlock(level, cracked, cx + station[0], 0, roomCz + station[1], box);
            }
            buildBossArenaMandala(level, box, cx, 0, roomCz, 5,
                    cracked, mirrorDecor, runeBricks);

            buildBossArenaCrown(level, box, cx, 1, roomCz, 5, 10,
                    mirrorDecor, cracked, prism);

            this.placeBlock(level, runeBricks, cx, 14, roomCz, box);
            this.placeBlock(level, runeBricks, 3, 14, 23, box);
            this.placeBlock(level, runeBricks, WIDTH - 4, 14, 29, box);

            illuminateInterior(level, box, cx, roomCz, 12, 5, 4, 3, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());

            // WIDTH 只有 15(cx=7),细部 half 最多取 6:再大外沿会落到负坐标被足印静默裁掉。
            BlockState mirrorStairs = ModBlocks.MIRROR_DUST_BRICK_STAIRS.get().defaultBlockState();
            BlockState mirrorSlab = ModBlocks.MIRROR_DUST_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, cx, 6, roomCz, Footprint.of(6, 5), mirrorStairs, mirrorSlab, true);
            buildCornice(level, box, cx, 7, roomCz, Footprint.of(6, 5), mirrorDecor, cracked);
            buildStairCornice(level, box, cx, 12, roomCz, Footprint.of(6, 5), mirrorStairs, mirrorSlab, true);

            for (int[] sconce : new int[][]{{1, 23}, {1, 29}, {WIDTH - 2, 23}, {WIDTH - 2, 29}}) {
                this.placeBlock(level, prism, sconce[0], 13, sconce[1], box);
            }

            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(),
                    2, 1, 30, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(2, 30), this.getWorldY(1),
                    this.getWorldZ(2, 30));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(cn.kurt6.unknown_echoes.registry.ModLootTables.TRUE_SIGHT_VAULT,
                        random.nextLong());
            }

            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(),
                    cx, 0, roomCz, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(cx, roomCz), this.getWorldY(0),
                    this.getWorldZ(cx, roomCz));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("mirror_dust_butler"), 1);
                spawner.setChallengeGate(EchoAbilityType.TRUE_SIGHT_ECHO,
                        UnknownEchoes.id("mirror_guardian"),
                        "message.unknown_echoes.mirror_cloister.locked");
            }
            for (int[] node : MIRROR_CLOISTER_NODES) {
                this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                        node[0], node[1], node[2], box);
            }
        }

        /** 隐藏奖励/挑战区的外壳与下方两层封底使用锚定封印石。 */
        private void sealHiddenBox(WorldGenLevel level, BoundingBox box, BlockState anchored,
                                   int x1, int x2, int z1, int z2, int floorY, int topY) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x, floorY, z, box);
                    this.placeBlock(level, anchored, x, topY, z, box);
                    for (int y = floorY - 2; y < floorY; y++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            for (int y = floorY + 1; y < topY; y++) {
                for (int x = x1; x <= x2; x++) {
                    this.placeBlock(level, anchored, x, y, z1, box);
                    this.placeBlock(level, anchored, x, y, z2, box);
                }
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x1, y, z, box);
                    this.placeBlock(level, anchored, x2, y, z, box);
                }
            }
        }
    }
}
