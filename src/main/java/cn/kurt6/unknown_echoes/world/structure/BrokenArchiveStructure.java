package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
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
 * 破钟档案馆:残钟荒原中型遗迹。
 * 半塌的书架室 + 断桥 + 残钟看守场地,普通 loot 与个人 Boss 结算分离。
 */
public class BrokenArchiveStructure extends SinglePieceStructure {
    public static final MapCodec<BrokenArchiveStructure> CODEC = simpleCodec(BrokenArchiveStructure::new);

    private static final int SIZE = 25;
    private static final int HEIGHT = 14;

    public BrokenArchiveStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BROKEN_ARCHIVE.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.BROKEN_ARCHIVE_PIECE.get(), x, 64, z,
                    SIZE, HEIGHT, SIZE, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.BROKEN_ARCHIVE_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            BlockState bellOre = ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState();
            BlockState stone = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState shelf = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            int c = SIZE / 2; // 12

            // 残钟荒原地基:半径 6/9/11 三层碎石台,支撑中型档案馆与战斗场。
            int[] radii = {6, 9, 11};
            for (int y = 0; y < radii.length; y++) {
                int r = radii[y];
                for (int x = 0; x < SIZE; x++) {
                    for (int z = 0; z < SIZE; z++) {
                        int dx = x - c;
                        int dz = z - c;
                        if (dx * dx + dz * dz <= r * r) {
                            this.placeBlock(level,
                                    random.nextFloat() < 0.3F ? bellOre : stone, x, y, z, box);
                        }
                    }
                }
            }
            int top = radii.length; // y=3

            // 入口碎路和左右侧室,让玩家能读到"档案室 -> 断桥 -> 看守场"的路线。
            for (int z = c - 11; z <= c - 4; z++) {
                this.placeBlock(level, rune, c, top, z, box);
                if (z % 3 == 0) {
                    this.placeBlock(level, cracked, c - 1, top, z, box);
                    this.placeBlock(level, cracked, c + 1, top, z, box);
                }
            }

            // 档案室:15x15 残墙(2~4 高的断壁)
            for (int x = c - 7; x <= c + 7; x++) {
                for (int z = c - 7; z <= c + 7; z++) {
                    boolean wall = x == c - 7 || x == c + 7 || z == c - 7 || z == c + 7;
                    if (!wall) {
                        continue;
                    }
                    int wallHeight = 2 + random.nextInt(3);
                    for (int y = 0; y < wallHeight; y++) {
                        this.placeBlock(level, random.nextFloat() < 0.35F ? cracked : bricks,
                                x, top + y, z, box);
                    }
                }
            }
            // 南墙留门洞
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, top, c - 7, box);
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, top + 1, c - 7, box);

            // 室内:残存书架一排 + 北墙符文砖标记
            for (int x = c - 5; x <= c + 5; x++) {
                if (random.nextFloat() < 0.7F) {
                    this.placeBlock(level, shelf, x, top, c + 5, box);
                }
            }
            this.placeBlock(level, rune, c, top + 1, c + 6, box);

            // 残页宝箱、侧室补给和真视回访奖励分离。
            buildAccessibleChest(level, random, box, c - 2, top, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            buildAccessibleChest(level, random, box, c + 5, top, c - 2, ModLootTables.T4_REGION_CORE);
            buildTrueSightVaultNiche(level, random, box, c - 6, top, c + 2, Direction.EAST);

            // 残钟看守:玩家接近时由服务端计时器生成,击败后走个人 Mini Boss 结算。
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, top, c + 4, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c + 4), this.getWorldY(top), this.getWorldZ(c, c + 4));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("broken_bell_keeper"), 3);
            }

            // 断桥:从东墙向外延伸到战斗边界,越远越破碎。
            for (int i = 1; i <= 8; i++) {
                if (random.nextFloat() < 1.0F - i * 0.2F) {
                    this.placeBlock(level, i < 4 ? bricks : cracked, c + 7 + i, top - 1, c, box);
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
