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
 * 天空观测站:漂浮群岛主结构(漂浮空岛,固定高度生成,不贴地)。
 * 倒锥形岛体 + 顶部环形观测台;观测站核心(ObservatoryCoreBlock)服务端校验风之回响——
 * 其他 Mod 飞上来也只能看,不能激活(design-principles #1)。
 * 风暴编织者(强化守护者)按规划拆到 V0.6,此处预留场地。
 */
public class SkyObservatoryStructure extends SinglePieceStructure {
    public static final MapCodec<SkyObservatoryStructure> CODEC = simpleCodec(SkyObservatoryStructure::new);

    private static final int SIZE = 17;
    private static final int HEIGHT = 14;

    public SkyObservatoryStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SKY_OBSERVATORY.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            // 漂浮空岛:固定生成在高空(y 175~190,高于尖峭山峰),不做贴地调整
            super(ModStructures.SKY_OBSERVATORY_PIECE.get(), x, 175 + random.nextInt(16), z,
                    SIZE, HEIGHT, SIZE, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SKY_OBSERVATORY_PIECE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            BlockState windStone = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            BlockState stone = Blocks.STONE.defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState grass = ModBlocks.GLOW_GRASS.get().defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            int c = SIZE / 2; // 8

            // 倒锥形岛体:自下而上半径 2/4/6/7,顶面为可行走平台
            int[] radii = {2, 4, 6, 7};
            for (int y = 0; y < radii.length; y++) {
                int r = radii[y];
                for (int x = 0; x < SIZE; x++) {
                    for (int z = 0; z < SIZE; z++) {
                        int dx = x - c;
                        int dz = z - c;
                        if (dx * dx + dz * dz <= r * r) {
                            boolean surface = y == radii.length - 1;
                            BlockState body = surface
                                    ? (random.nextFloat() < 0.5F ? windStone : moss)
                                    : (random.nextFloat() < 0.35F ? windStone : stone);
                            this.placeBlock(level, body, x, y, z, box);
                        }
                    }
                }
            }
            int top = radii.length; // y=4,平台上一层

            // 平台植被:发光草(下方是 moss/windstone,moss 在 dirt tag,windstone 上的会自检存活)
            for (int i = 0; i < 8; i++) {
                int x = c - 5 + random.nextInt(11);
                int z = c - 5 + random.nextInt(11);
                int dx = x - c;
                int dz = z - c;
                if (dx * dx + dz * dz <= 36
                        && this.getBlock(level, x, top - 1, z, box).is(Blocks.MOSS_BLOCK)) {
                    this.placeBlock(level, grass, x, top, z, box);
                }
            }

            // 中央观测亭:5x5 石砖,高 4,顶部环形檐
            for (int x = c - 2; x <= c + 2; x++) {
                for (int z = c - 2; z <= c + 2; z++) {
                    boolean wall = x == c - 2 || x == c + 2 || z == c - 2 || z == c + 2;
                    for (int y = top; y <= top + 3; y++) {
                        if (wall) {
                            this.placeBlock(level, random.nextFloat() < 0.25F ? cracked : bricks, x, y, z, box);
                        } else {
                            this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                        }
                    }
                    this.placeBlock(level, bricks, x, top + 4, z, box); // 顶
                }
            }

            // 南侧入口:风门封住(服务端校验风之回响)
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c, top, c - 2, box);
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c, top + 1, c - 2, box);

            // 亭内:观测站核心(台座 + 核心)与记录宝箱
            this.placeBlock(level, bricks, c, top, c, box);
            this.placeBlock(level, ModBlocks.OBSERVATORY_CORE.get().defaultBlockState(), c, top + 1, c, box);
            buildAccessibleChest(level, random, box, c + 1, top, c + 1, ModLootTables.SKY_OBSERVATORY_CHEST);
            buildTrueSightVaultNiche(level, random, box, c + 4, top, c - 4, Direction.WEST);

            // V0.6D 神器台座:记录台(领取/复领风暴罗盘)与升级台(升级/调谐),亭内东西两侧
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(),
                    c - 1, top, c + 1, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(),
                    c - 1, top, c - 1, box);

            // 四角风蚀立柱(风暴编织者场地,V0.6A 启用)
            for (int[] corner : new int[][]{{c - 5, c - 5}, {c + 5, c - 5}, {c - 5, c + 5}, {c + 5, c + 5}}) {
                for (int y = top; y <= top + 2; y++) {
                    if (random.nextFloat() < 0.8F) {
                        this.placeBlock(level, windStone, corner[0], y, corner[1], box);
                    }
                }
            }

            // 风流平台(V0.6E,10.5 风流追击):平台环绕观测亭四个正向,
            // 风之权限激活后产生上升气流柱,玩家借气流接近空中的风暴编织者
            BlockState platform = ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState();
            for (int[] spot : new int[][]{{c - 5, c}, {c + 5, c}, {c, c - 5}, {c, c + 5}}) {
                this.placeBlock(level, platform, spot[0], top, spot[1], box);
            }

            // 风暴编织者场地计时器:埋进岛体中心(y=2 处锥体半径 4,中心必在体内)。
            // 首次生成与击败后重开都由它驱动(MiniBossSpawnerBlockEntity,V0.6A 公共框架)。
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 2, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(2), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos) && level.getBlockEntity(spawnerPos)
                    instanceof cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity spawner) {
                // 生成点:观测亭顶(top+4)再高 4 格的开阔空域
                spawner.configure(cn.kurt6.unknown_echoes.UnknownEchoes.id("storm_weaver"), top + 6);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
