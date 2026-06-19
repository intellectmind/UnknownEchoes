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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 半沉的静默派石屋:失语沼泽残页点。
 * 屋体下沉两格,半埋进沼泽——进屋要先往下走;屋内宝箱放静默派残页组。
 * 纯环境叙事结构,无机关、无权限判定。
 */
public class SilentHutStructure extends SinglePieceStructure {
    public static final MapCodec<SilentHutStructure> CODEC = simpleCodec(SilentHutStructure::new);

    private static final int WIDTH = 15;
    private static final int DEPTH = 15;
    private static final int HEIGHT = 8;

    public SilentHutStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SILENT_HUT.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SILENT_HUT_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SILENT_HUT_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            // 下沉两格:地板低于沼泽地表,呈"半埋"状
            if (!this.updateAverageGroundHeight(level, box, -2)) {
                return;
            }

            BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
            BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            // 地板:噤声苔混石砖(沼泽渗进来了)
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    float roll = random.nextFloat();
                    BlockState floor = roll < 0.4F ? moss : (roll < 0.7F ? mossy : bricks);
                    this.placeBlock(level, floor, x, 0, z, box);
                }
            }
            for (int z = 0; z <= DEPTH / 2; z++) {
                this.placeBlock(level, moss, WIDTH / 2, 1, z, box);
            }
            // 清出内部
            this.generateBox(level, box, 1, 1, 1, WIDTH - 2, 4, DEPTH - 2, air, air, false);

            // 四面墙(高 4),石砖为主,越往下越苔化;随机缺口表现坍塌
            for (int y = 1; y <= 4; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    placeWall(level, box, random, x, y, 0, y);
                    placeWall(level, box, random, x, y, DEPTH - 1, y);
                }
                for (int z = 1; z < DEPTH - 1; z++) {
                    placeWall(level, box, random, 0, y, z, y);
                    placeWall(level, box, random, WIDTH - 1, y, z, y);
                }
            }

            // 半塌的屋顶:只剩边缘一圈 + 随机残板
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    boolean edge = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                    if (edge || random.nextFloat() < 0.3F) {
                        this.placeBlock(level, random.nextFloat() < 0.5F ? mossy : cracked, x, 5, z, box);
                    }
                }
            }

            // 门洞:南面正中,2 高;门外一格台阶方便下来
            this.placeBlock(level, air, WIDTH / 2, 1, 0, box);
            this.placeBlock(level, air, WIDTH / 2, 2, 0, box);
            this.placeBlock(level, air, WIDTH / 2, 3, 0, box);

            // 北墙嵌一块回响符文砖:静默派的标记(线索,指向沼泽里的无声环)
            this.placeBlock(level, rune, WIDTH / 2, 2, DEPTH - 1, box);

            // 屋内角落:蛛网 + 苔块堆,废弃感
            this.placeBlock(level, Blocks.COBWEB.defaultBlockState(), 1, 3, DEPTH - 2, box);
            this.placeBlock(level, moss, WIDTH - 2, 1, DEPTH - 2, box);
            this.placeBlock(level, rune, WIDTH / 2 - 4, 1, DEPTH / 2, box);
            this.placeBlock(level, rune, WIDTH / 2 + 4, 1, DEPTH / 2, box);

            // 残页宝箱 + 侧室补给 + 真视回访点
            buildAccessibleChest(level, random, box, WIDTH / 2, 1, DEPTH - 2,
                    ModLootTables.SILENT_HUT_CHEST);
            buildAccessibleChest(level, random, box, 3, 1, DEPTH / 2,
                    ModLootTables.T2_RUIN);
            buildTrueSightVaultNiche(level, random, box,
                    WIDTH - 4, 1, DEPTH / 2 + 2, Direction.WEST);
        }

        /** 墙体方块:下层更苔化,带随机坍塌缺口。 */
        private void placeWall(WorldGenLevel level, BoundingBox box, RandomSource random,
                               int x, int y, int z, int height) {
            if (random.nextFloat() < 0.08F && height >= 3) {
                return; // 高处偶有缺口
            }
            float mossChance = 0.7F - 0.15F * height;
            BlockState state;
            if (random.nextFloat() < mossChance) {
                state = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else {
                state = random.nextFloat() < 0.25F
                        ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState();
            }
            this.placeBlock(level, state, x, y, z, box);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
