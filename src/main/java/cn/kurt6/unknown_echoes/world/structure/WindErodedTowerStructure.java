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
 * 风蚀塔:漂浮群岛垂直谜题(地面结构)。
 * 底层风门(服务端校验风之回响)封住入口;塔内整砖螺旋阶梯
 * (gotchas #13:不用梯子等方向性附着方块),塔身随机风蚀孔洞。
 * 塔顶:风纹石平台 + 宝箱。进度门槛是风门权限,不是跳跃能力(design-principles #1)。
 */
public class WindErodedTowerStructure extends SinglePieceStructure {
    public static final MapCodec<WindErodedTowerStructure> CODEC = simpleCodec(WindErodedTowerStructure::new);

    private static final int SIZE = 9;
    private static final int HEIGHT = 20;

    public WindErodedTowerStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WIND_ERODED_TOWER.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.WIND_ERODED_TOWER_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.WIND_ERODED_TOWER_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState windStone = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int topY = HEIGHT - 3;

            // 地基与塔身:9x9 外壳,内部中空;塔身越高风蚀孔越多
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    this.placeBlock(level, bricks, x, 0, z, box);
                }
            }
            for (int y = 1; y < topY; y++) {
                for (int x = 0; x < SIZE; x++) {
                    for (int z = 0; z < SIZE; z++) {
                        boolean wall = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                        if (!wall) {
                            this.placeBlock(level, air, x, y, z, box);
                            continue;
                        }
                        // 风蚀孔:高度越高越破,但 1~2 层保持完整(风门所在)
                        float erodeChance = y <= 2 ? 0.0F : Math.min(0.3F, 0.02F * y);
                        if (random.nextFloat() < erodeChance) {
                            continue;
                        }
                        BlockState body = random.nextFloat() < 0.3F ? windStone
                                : (random.nextFloat() < 0.2F ? cracked : bricks);
                        this.placeBlock(level, body, x, y, z, box);
                    }
                }
            }

            // 入口:南面正中,风门封住(权限门槛)
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), SIZE / 2, 1, 0, box);
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), SIZE / 2, 2, 0, box);

            // 风流平台(V0.6E):塔心地面一座,激活后气流直送中段休息平台——
            // 螺旋阶梯仍是无能力时的完整路线,气流只是风之玩家的容错与爽感(红线 #1)
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(),
                    SIZE / 2, 1, SIZE / 2, box);

            // 整砖螺旋阶梯:沿内壁逆时针,每步抬高 1 格;部分台阶蒙着蛛网(攀爬阻滞),
            // 中段嵌一圈休息平台(次级宝箱 + 残破窗洞,垂直探索的节奏点)
            int[][] ring = ringPath();
            int step = 0;
            int midY = topY / 2;
            for (int y = 1; y < topY - 1; y++) {
                int[] spot = ring[step % ring.length];
                this.placeBlock(level, bricks, spot[0], y, spot[1], box);
                if (y != midY && random.nextFloat() < 0.15F) {
                    this.placeBlock(level, Blocks.COBWEB.defaultBlockState(), spot[0], y + 1, spot[1], box);
                }
                step++;
            }

            // 中段休息平台:3x3,靠西墙;放次级宝箱,平台外侧风蚀出一排大窗
            for (int x = 1; x <= 3; x++) {
                for (int z = 3; z <= 5; z++) {
                    this.placeBlock(level, windStone, x, midY, z, box);
                }
            }
            buildAccessibleChest(level, random, box, 2, midY + 1, 4, ModLootTables.SKY_OBSERVATORY_CHEST);
            buildTrueSightVaultNiche(level, random, box, 6, midY + 1, 4, Direction.WEST);
            for (int z = 3; z <= 5; z++) {
                this.placeBlock(level, air, 0, midY + 1, z, box);
                this.placeBlock(level, air, 0, midY + 2, z, box);
            }

            // 塔顶平台:风纹石,中央宝箱 + 符文砖
            for (int x = 1; x < SIZE - 1; x++) {
                for (int z = 1; z < SIZE - 1; z++) {
                    this.placeBlock(level, windStone, x, topY, z, box);
                }
            }
            // 顶层矮护栏(残缺)
            for (int x = 0; x < SIZE; x++) {
                if (random.nextFloat() < 0.6F) {
                    this.placeBlock(level, cracked, x, topY + 1, 0, box);
                }
                if (random.nextFloat() < 0.6F) {
                    this.placeBlock(level, cracked, x, topY + 1, SIZE - 1, box);
                }
            }
            this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                    SIZE / 2, topY + 1, SIZE / 2 + 1, box);
            buildAccessibleChest(level, random, box, SIZE / 2, topY + 1, SIZE / 2,
                    ModLootTables.WIND_VAULT_CHEST);
        }

        /** 内壁一圈的踏步点位(7x7 内圈周长路径)。 */
        private static int[][] ringPath() {
            return new int[][]{
                    {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {7, 1},
                    {7, 2}, {7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7},
                    {6, 7}, {5, 7}, {4, 7}, {3, 7}, {2, 7}, {1, 7},
                    {1, 6}, {1, 5}, {1, 4}, {1, 3}, {1, 2}
            };
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
