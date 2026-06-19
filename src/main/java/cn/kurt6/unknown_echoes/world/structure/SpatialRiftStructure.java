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
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 主世界空间裂隙(静态点,文档 4.2.1):细小的悬空裂纹,与 16.2 事件裂隙共用紫水晶/末地烛表现。
 * 只提供日志线索(残页)与少量材料,不是遗迹、不含关键进度(设计红线 #2)。
 * 「靠近时环境音轻微失真」的逐 tick 音频随 V0.9 事件裂隙系统一并做;此处用紫水晶质感 + 粒子近似。
 */
public class SpatialRiftStructure extends SinglePieceStructure {
    public static final MapCodec<SpatialRiftStructure> CODEC = simpleCodec(SpatialRiftStructure::new);

    private static final int WIDTH = 9;
    private static final int DEPTH = 9;
    private static final int HEIGHT = 14;

    public SpatialRiftStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SPATIAL_RIFT.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.SPATIAL_RIFT_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SPATIAL_RIFT_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            int c = WIDTH / 2;
            BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
            BlockState budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
            BlockState calcite = Blocks.CALCITE.defaultBlockState();
            BlockState glow = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            BlockState rod = Blocks.END_ROD.defaultBlockState();

            // 地面残痕:晶洞自然以方解石包裹紫水晶,散落一圈作为坠落锚点
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz <= 4 && random.nextFloat() < 0.55F) {
                        this.placeBlock(level, random.nextFloat() < 0.3F ? amethyst : calcite,
                                c + dx, 0, c + dz, box);
                    }
                }
            }

            // 悬空裂纹:一道窄薄的紫水晶竖切片,随高度轻微漂移(sin),发光玻璃透出裂隙光
            for (int y = 4; y < HEIGHT - 1; y++) {
                int drift = (int) Math.round(Math.sin(y / 2.0) * 1.6);
                this.placeBlock(level, amethyst, c + drift, y, c, box);
                if (y % 3 == 0) {
                    this.placeBlock(level, glow, c + drift, y, c, box);
                    this.placeBlock(level, rod, c + drift, y, c + 1, box);
                    this.placeBlock(level, rod, c + drift, y, c - 1, box);
                }
                if (y == 6 || y == 9) {
                    this.placeBlock(level, budding, c + drift, y, c, box);
                }
            }

            // 地面小宝箱:日志线索残页 + 少量材料(刻意稀薄)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.placeBlock(level, calcite, c + 2 + dx, 0, c + 2 + dz, box);
                }
            }
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c + 2, 1, c + 2, box);
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c + 2, 2, c + 2, box);
            this.createChest(level, box, random, c + 2, 1, c + 2, ModLootTables.SPATIAL_RIFT_CHEST);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
