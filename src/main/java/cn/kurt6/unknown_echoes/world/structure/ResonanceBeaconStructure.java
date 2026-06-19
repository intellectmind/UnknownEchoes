package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 残响信标结构:主世界中玩家第一次进入回声境域的入口。
 * 小型祭坛平台,中央是不可破坏的残响信标,四角符文柱。
 */
public class ResonanceBeaconStructure extends SinglePieceStructure {
    public static final MapCodec<ResonanceBeaconStructure> CODEC = simpleCodec(ResonanceBeaconStructure::new);

    private static final int WIDTH = 7;
    private static final int DEPTH = 7;

    public ResonanceBeaconStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RESONANCE_BEACON_STRUCTURE.get();
    }

    public static class Piece extends ScatteredFeaturePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.RESONANCE_BEACON_PIECE.get(), x, 64, z, WIDTH, 5, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.RESONANCE_BEACON_PIECE.get(), tag);
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
            BlockState beacon = ModBlocks.RESONANCE_BEACON.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            // 平台
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : bricks, x, 0, z, box);
                }
            }
            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, 4, DEPTH - 1, air, air, false);

            // 四角符文柱
            for (int y = 1; y <= 2; y++) {
                this.placeBlock(level, y == 2 ? rune : bricks, 0, y, 0, box);
                this.placeBlock(level, y == 2 ? rune : bricks, WIDTH - 1, y, 0, box);
                this.placeBlock(level, y == 2 ? rune : bricks, 0, y, DEPTH - 1, box);
                this.placeBlock(level, y == 2 ? rune : bricks, WIDTH - 1, y, DEPTH - 1, box);
            }

            // 中央基座与信标
            this.placeBlock(level, bricks, WIDTH / 2, 1, DEPTH / 2, box);
            this.placeBlock(level, beacon, WIDTH / 2, 2, DEPTH / 2, box);
        }
    }
}
