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
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 主世界小型回响遗迹:回响石砖残垣 + 中央宝箱(回响碎片/低概率回响核心)+ 回响符文砖线索。
 */
public class SmallEchoRuinStructure extends SinglePieceStructure {
    public static final MapCodec<SmallEchoRuinStructure> CODEC = simpleCodec(SmallEchoRuinStructure::new);

    private static final int WIDTH = 17;
    private static final int DEPTH = 17;

    public SmallEchoRuinStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SMALL_ECHO_RUIN.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SMALL_ECHO_RUIN_PIECE.get(), x, 64, z, WIDTH, 8, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SMALL_ECHO_RUIN_PIECE.get(), tag);
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
            BlockState air = Blocks.AIR.defaultBlockState();

            // 地板 + 入口引导:小遗迹不再是 9x9 单箱子,玩家能从南侧符文路读到中心。
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.25F ? cracked : bricks, x, 0, z, box);
                }
            }
            for (int z = DEPTH / 2; z < DEPTH; z++) {
                this.placeBlock(level, rune, WIDTH / 2, 1, z, box);
            }
            // 清出内部空间
            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, 4, DEPTH - 1, air, air, false);

            // 残破的四周矮墙(随机缺口表现遗迹感)
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 1; y <= 2; y++) {
                    if (random.nextFloat() < 0.75F) {
                        this.placeBlock(level, random.nextFloat() < 0.3F ? cracked : bricks, x, y, 0, box);
                    }
                    if (random.nextFloat() < 0.75F) {
                        this.placeBlock(level, random.nextFloat() < 0.3F ? cracked : bricks, x, y, DEPTH - 1, box);
                    }
                }
            }
            for (int z = 1; z < DEPTH - 1; z++) {
                for (int y = 1; y <= 2; y++) {
                    if (random.nextFloat() < 0.75F) {
                        this.placeBlock(level, random.nextFloat() < 0.3F ? cracked : bricks, 0, y, z, box);
                    }
                    if (random.nextFloat() < 0.75F) {
                        this.placeBlock(level, random.nextFloat() < 0.3F ? cracked : bricks, WIDTH - 1, y, z, box);
                    }
                }
            }

            // 四角立柱
            for (int y = 1; y <= 3; y++) {
                this.placeBlock(level, bricks, 0, y, 0, box);
                this.placeBlock(level, bricks, WIDTH - 1, y, 0, box);
                this.placeBlock(level, bricks, 0, y, DEPTH - 1, box);
                this.placeBlock(level, bricks, WIDTH - 1, y, DEPTH - 1, box);
            }

            // 回响符文砖:神秘文字线索,指向残响信标
            this.placeBlock(level, rune, WIDTH / 2, 1, 0, box);
            this.placeBlock(level, rune, WIDTH / 2, 2, 0, box);
            this.placeBlock(level, rune, WIDTH / 2 - 4, 1, DEPTH / 2, box);
            this.placeBlock(level, rune, WIDTH / 2 + 4, 1, DEPTH / 2, box);

            // 中央宝箱 + 侧室分支 + 真视回访点
            buildAccessibleChest(level, random, box, WIDTH / 2, 1, DEPTH / 2,
                    ModLootTables.SMALL_ECHO_RUIN_CHEST);
            buildAccessibleChest(level, random, box, WIDTH / 2 - 5, 1, DEPTH / 2 + 4,
                    ModLootTables.T2_RUIN);
            buildTrueSightVaultNiche(level, random, box,
                    WIDTH / 2 + 6, 1, DEPTH / 2 + 5, Direction.WEST);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
