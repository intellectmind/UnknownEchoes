package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

abstract class EchoStructurePiece extends ScatteredFeaturePiece {

    protected EchoStructurePiece(StructurePieceType type, int x, int y, int z,
                                 int width, int height, int depth, Direction orientation) {
        super(type, x, y, z, width, height, depth, orientation);
    }

    protected EchoStructurePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
    }

    protected void buildTrueSightVaultNiche(WorldGenLevel level, RandomSource random, BoundingBox box,
                                            int x, int y, int z, Direction entranceSide) {
        buildHiddenVaultNiche(level, box, x, y, z, horizontal(entranceSide));
        this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), x, y, z, box);
        configureTrueSightVault(level, box, random, x, y, z);
    }

    protected void buildCompactTrueSightVaultNiche(WorldGenLevel level, RandomSource random, BoundingBox box,
                                                   int x, int y, int z, Direction entranceSide) {
        Direction side = horizontal(entranceSide);
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
        sealHiddenNiche(level, box, anchored, x - 1, x + 1, z - 1, z + 1, y - 1, y + 3);
        placeHiddenDoor(level, box, hidden, side, x, y, z, x - 1, x + 1, z - 1, z + 1);
        clearCompactVaultApproach(level, box, side, x, y, z);
        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y + 1, z, box);
        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y + 2, z, box);
        this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), x, y, z, box);
        configureTrueSightVault(level, box, random, x, y, z);
    }

    protected void buildAccessibleChest(WorldGenLevel level, RandomSource random, BoundingBox box,
                                        int x, int y, int z,
                                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        BlockState floor = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
        BlockState accent = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
        int floorY = y - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                this.placeBlock(level, floor, x + dx, floorY, z + dz, box);
            }
        }
        this.placeBlock(level, accent, x, floorY, z - 2, box);
        clearChestPocket(level, box, x, y, z);
        this.createChest(level, box, random, x, y, z, loot);
    }

    private void configureTrueSightVault(WorldGenLevel level, BoundingBox box, RandomSource random,
                                         int x, int y, int z) {
        BlockPos vaultPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
        if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
            vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
        }
    }

    private void clearChestPocket(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    this.placeBlock(level, air, x + dx, y, z + dz, box);
                }
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            this.placeBlock(level, air, x, y + dy, z, box);
        }
    }

    private void buildHiddenVaultNiche(WorldGenLevel level, BoundingBox box,
                                       int x, int y, int z, Direction entranceSide) {
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int minX = x - 2;
        int maxX = x + 2;
        int minZ = z - 2;
        int maxZ = z + 2;
        int roofY = y + 3;
        sealHiddenNiche(level, box, anchored, minX, maxX, minZ, maxZ, y - 1, roofY);
        this.generateBox(level, box, x - 1, y, z - 1, x + 1, y + 2, z + 1, air, air, false);
        placeHiddenDoor(level, box, hidden, entranceSide, x, y, z, minX, maxX, minZ, maxZ);
        clearVaultApproach(level, box, entranceSide, x, y, z);
        clearChestPocket(level, box, x, y, z);
    }

    private void sealHiddenNiche(WorldGenLevel level, BoundingBox box, BlockState anchored,
                                 int minX, int maxX, int minZ, int maxZ, int floorY, int roofY) {
        for (int xx = minX; xx <= maxX; xx++) {
            for (int zz = minZ; zz <= maxZ; zz++) {
                this.placeBlock(level, anchored, xx, floorY, zz, box);
                this.placeBlock(level, anchored, xx, roofY, zz, box);
                for (int yy = floorY - 2; yy < floorY; yy++) {
                    this.placeBlock(level, anchored, xx, yy, zz, box);
                }
            }
        }
        for (int yy = floorY + 1; yy < roofY; yy++) {
            for (int xx = minX; xx <= maxX; xx++) {
                this.placeBlock(level, anchored, xx, yy, minZ, box);
                this.placeBlock(level, anchored, xx, yy, maxZ, box);
            }
            for (int zz = minZ; zz <= maxZ; zz++) {
                this.placeBlock(level, anchored, minX, yy, zz, box);
                this.placeBlock(level, anchored, maxX, yy, zz, box);
            }
        }
    }

    private void placeHiddenDoor(WorldGenLevel level, BoundingBox box, BlockState hidden, Direction side,
                                 int x, int y, int z, int minX, int maxX, int minZ, int maxZ) {
        for (int yy = y; yy <= y + 2; yy++) {
            for (int offset = -1; offset <= 1; offset++) {
                switch (side) {
                    case NORTH -> this.placeBlock(level, hidden, x + offset, yy, minZ, box);
                    case SOUTH -> this.placeBlock(level, hidden, x + offset, yy, maxZ, box);
                    case WEST -> this.placeBlock(level, hidden, minX, yy, z + offset, box);
                    case EAST -> this.placeBlock(level, hidden, maxX, yy, z + offset, box);
                    default -> { }
                }
            }
        }
    }

    private void clearVaultApproach(WorldGenLevel level, BoundingBox box, Direction side, int x, int y, int z) {
        clearVaultApproach(level, box, side, x, y, z, 3, 4);
    }

    private void clearCompactVaultApproach(WorldGenLevel level, BoundingBox box, Direction side, int x, int y, int z) {
        clearVaultApproach(level, box, side, x, y, z, 2, 3);
    }

    private void clearVaultApproach(WorldGenLevel level, BoundingBox box, Direction side,
                                    int x, int y, int z, int firstStep, int lastStep) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int perpX = side.getStepZ();
        int perpZ = -side.getStepX();
        for (int step = firstStep; step <= lastStep; step++) {
            int baseX = x + side.getStepX() * step;
            int baseZ = z + side.getStepZ() * step;
            for (int offset = -1; offset <= 1; offset++) {
                for (int dy = 0; dy <= 2; dy++) {
                    this.placeBlock(level, air, baseX + perpX * offset, y + dy,
                            baseZ + perpZ * offset, box);
                }
            }
        }
        this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                x + side.getStepX() * firstStep, Math.max(0, y - 1), z + side.getStepZ() * firstStep, box);
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.SOUTH;
    }
}
