package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
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

/** 回声峭壁中的折痕祭坛。结构可多处生成，个人校准互不影响。 */
public class AbyssBeaconStructure extends SinglePieceStructure {
    public static final MapCodec<AbyssBeaconStructure> CODEC = simpleCodec(AbyssBeaconStructure::new);

    private static final int WIDTH = 11;
    private static final int DEPTH = 11;

    public AbyssBeaconStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ABYSS_BEACON_STRUCTURE.get();
    }

    public static class Piece extends ScatteredFeaturePiece {
        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.ABYSS_BEACON_PIECE.get(), x, 64, z, WIDTH, 7, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.ABYSS_BEACON_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState stone = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, 6, DEPTH - 1, air, air, false);
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    int edge = Math.max(Math.abs(x - WIDTH / 2), Math.abs(z - DEPTH / 2));
                    BlockState floor = edge >= 4 ? stone : random.nextFloat() < 0.24F ? cracked : bricks;
                    this.placeBlock(level, floor, x, 0, z, box);
                }
            }

            // 四向断裂环与能力符柱，中央信标高于祭坛一步。
            for (int offset = -3; offset <= 3; offset++) {
                if (Math.abs(offset) == 1) {
                    continue;
                }
                this.placeBlock(level, rune, WIDTH / 2 + offset, 1, 1, box);
                this.placeBlock(level, rune, WIDTH / 2 + offset, 1, DEPTH - 2, box);
                this.placeBlock(level, rune, 1, 1, DEPTH / 2 + offset, box);
                this.placeBlock(level, rune, WIDTH - 2, 1, DEPTH / 2 + offset, box);
            }
            int[][] pillars = {{1, 1}, {WIDTH - 2, 1}, {1, DEPTH - 2}, {WIDTH - 2, DEPTH - 2}};
            for (int[] pillar : pillars) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, y == 3 ? rune : stone, pillar[0], y, pillar[1], box);
                }
            }
            this.placeBlock(level, bricks, WIDTH / 2, 1, DEPTH / 2, box);
            this.placeBlock(level, ModBlocks.ABYSS_BEACON.get().defaultBlockState(),
                    WIDTH / 2, 2, DEPTH / 2, box);
        }
    }
}
