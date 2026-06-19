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

import java.util.ArrayList;
import java.util.List;

/**
 * 沉默祭坛:失语沼泽区域守护者场地(V0.6A,设计文档 10.4.2 沉默祭司)。
 * 圆形苔石平台 + 中央祭坛高台 + 四角共鸣烛柱 + 残缺石环。
 * 共鸣烛是机制方块(战斗中打碎缩短静默领域),刻意可破坏;
 * 场地重开时由 MiniBossSpawnerBlockEntity 按登记位置补回。
 */
public class SilentAltarStructure extends SinglePieceStructure {
    public static final MapCodec<SilentAltarStructure> CODEC = simpleCodec(SilentAltarStructure::new);

    private static final int SIZE = 19;
    private static final int HEIGHT = 8;

    public SilentAltarStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SILENT_ALTAR.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SILENT_ALTAR_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SILENT_ALTAR_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
            BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            BlockState moss = ModBlocks.MUFFLE_MOSS.get().defaultBlockState();
            BlockState candle = ModBlocks.RESONANCE_CANDLE.get().defaultBlockState();
            int c = SIZE / 2; // 9

            // 圆形平台(半径 8):苔石混杂,边缘大片噤声苔——沼泽里的一块"无声空地"
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    int dx = x - c;
                    int dz = z - c;
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= 64) {
                        BlockState floor = distSq >= 44 && random.nextFloat() < 0.5F ? moss
                                : (random.nextFloat() < 0.4F ? mossy : bricks);
                        this.placeBlock(level, floor, x, 0, z, box);
                    }
                }
            }

            // 中央祭坛高台:3x3 抬升一层 + 中心凿制祭坛(守护者出生于其上)
            for (int x = c - 1; x <= c + 1; x++) {
                for (int z = c - 1; z <= c + 1; z++) {
                    this.placeBlock(level, x == c && z == c ? chiseled : mossy, x, 1, z, box);
                }
            }

            // 四角共鸣烛柱:双层石柱 + 顶端共鸣烛(打碎缩短静默领域,可破坏不进保护)
            List<BlockPos> candlePositions = new ArrayList<>();
            for (int[] corner : new int[][]{{c - 5, c - 5}, {c + 5, c - 5}, {c - 5, c + 5}, {c + 5, c + 5}}) {
                this.placeBlock(level, bricks, corner[0], 1, corner[1], box);
                this.placeBlock(level, chiseled, corner[0], 2, corner[1], box);
                this.placeBlock(level, candle, corner[0], 3, corner[1], box);
                candlePositions.add(new BlockPos(
                        this.getWorldX(corner[0], corner[1]),
                        this.getWorldY(3),
                        this.getWorldZ(corner[0], corner[1])));
            }

            // 残缺石环:外圈零散立柱与断拱,给走位提供掩体但不封死场地
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                int x = c + (int) Math.round(Math.cos(angle) * 7.5);
                int z = c + (int) Math.round(Math.sin(angle) * 7.5);
                if (Math.abs(x - c) == 5 && Math.abs(z - c) == 5) {
                    continue; // 不挤占烛柱位
                }
                if (random.nextFloat() < 0.6F) {
                    int height = 1 + random.nextInt(3);
                    for (int y = 1; y <= height; y++) {
                        this.placeBlock(level, random.nextFloat() < 0.5F ? mossy : bricks, x, y, z, box);
                    }
                }
            }

            // 守护者场地计时器:埋在祭坛正下方;首次生成与击败后重开都由它驱动
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(0), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("silent_priest"), 2);
                spawner.setResetBlocks(UnknownEchoes.id("resonance_candle"), candlePositions);
            }

            // 重试入口与回访奖励:公共箱只放材料/补给,真视箱提供非主线回访价值。
            buildAccessibleChest(level, random, box, c, 1, c - 7, ModLootTables.T5_MINIBOSS_REPEAT);
            buildTrueSightVaultNiche(level, random, box, c + 6, 1, c + 2, Direction.WEST);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
