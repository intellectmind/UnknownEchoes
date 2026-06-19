package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
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
 * 镜尘回廊:镜湖岸边倒影层残廊(V0.6C 镜湖区域变体,设计文档 4.3.2)。
 * 三段一体:
 * - 倒影井(地表小型探索点 / 唯一入口):镜石砖井口,井内螺旋阶梯沉入倒影层;
 * - 镜尘回廊(地下浅廊):错位镜砖墙体 + 地面碎镜纹路暗示洗牌路径 + 幻象墙暗格(回访);
 * - 隐藏房(镜面长厅):三面镜墙 + 三个镜尘站位 + 中央碎镜地台,镜尘执事战斗区。
 * 隐藏房入口藏在回廊尽头的幻象墙后(真视发现);挑战门槛由 MiniBossSpawnerBlock
 * 服务端校验 TRUE_SIGHT_ECHO 或镜像守护者击败记录(红线 #1,真视只提供发现与入口)。
 * 生成边界:镜湖群系旱地/岸段(默认地表放置正好拒绝水下),绑定倒影井,不独立刷大片区域。
 */
public class MirrorDustCloisterStructure extends SinglePieceStructure {
    public static final MapCodec<MirrorDustCloisterStructure> CODEC =
            simpleCodec(MirrorDustCloisterStructure::new);

    private static final int WIDTH = 15;
    private static final int DEPTH = 34;
    private static final int HEIGHT = 17;
    /** 地下部分深度:地表在局部 y=GROUND_Y。 */
    private static final int GROUND_Y = 13;

    public MirrorDustCloisterStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MIRROR_DUST_CLOISTER.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.MIRROR_DUST_CLOISTER_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.MIRROR_DUST_CLOISTER_PIECE.get(), tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            // 整体下沉:局部 GROUND_Y 对齐地表,井口露头、回廊与隐藏房埋入倒影层
            if (!this.updateAverageGroundHeight(level, box, -GROUND_Y)) {
                return;
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState stone = ModBlocks.MIRROR_STONE.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int cx = WIDTH / 2; // 7

            // ---- 倒影井(z 0..6,地表):5×5 井口 + 井内螺旋阶梯 ----
            // 井筒外壳(y1..GROUND_Y+1),内部 3×3 排空
            this.generateBox(level, box, cx - 2, 0, 1, cx + 2, GROUND_Y + 1, 5, bricks, bricks, false);
            this.generateBox(level, box, cx - 1, 1, 2, cx + 1, GROUND_Y + 1, 4, air, air, false);
            // 井口沿与角柱(地表可见的"小型倒置井室")
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        this.placeBlock(level, bricks, cx + dx, GROUND_Y + 1, 3 + dz, box);
                    }
                }
            }
            for (int[] post : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                this.placeBlock(level, bricks, cx + post[0], GROUND_Y + 2, 3 + post[1], box);
                this.placeBlock(level, runeBricks, cx + post[0], GROUND_Y + 3, 3 + post[1], box);
            }
            // 井口南侧开缺口供进入(翻过井沿)
            this.placeBlock(level, air, cx, GROUND_Y + 1, 1, box);
            // 螺旋阶梯:沿井筒内壁 8 格一圈逐级下沉,从井口走到倒影层
            int[][] spiral = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
            int y = GROUND_Y;
            int idx = 0;
            while (y >= 2) {
                int[] cell = spiral[idx % spiral.length];
                this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : bricks,
                        cx + cell[0], y, 3 + cell[1], box);
                idx++;
                y--;
            }
            // 井底:微光与碎镜纹,指向回廊
            this.placeBlock(level, runeBricks, cx, 0, 3, box);
            this.placeBlock(level, cracked, cx, 0, 4, box);
            // 井底通往回廊的门洞
            this.generateBox(level, box, cx - 1, 1, 5, cx + 1, 3, 6, air, air, false);
            this.placeBlock(level, bricks, cx - 1, 0, 5, box);
            this.placeBlock(level, bricks, cx, 0, 5, box);
            this.placeBlock(level, bricks, cx + 1, 0, 5, box);

            // ---- 镜尘回廊(z 6..19):5 宽浅廊,错位镜砖 + 碎镜纹路 + 幻象墙暗格 ----
            this.generateBox(level, box, cx - 3, 0, 6, cx + 3, 5, 19, bricks, bricks, false);
            this.generateBox(level, box, cx - 2, 1, 6, cx + 2, 4, 19, air, air, false);
            for (int z = 6; z <= 19; z++) {
                // 地板:碎镜纹路沿中线蜿蜒,暗示洗牌路径(4.3.2)
                for (int dx = -2; dx <= 2; dx++) {
                    BlockState floor = bricks;
                    if (dx == ((z % 4 < 2) ? 0 : (z % 8 < 4 ? 1 : -1))) {
                        floor = cracked;
                    } else if (random.nextFloat() < 0.2F) {
                        floor = stone;
                    }
                    this.placeBlock(level, floor, cx + dx, 0, z, box);
                }
                // 错位镜砖墙:随机外凸/内嵌的镜石,远看像普通残廊
                if (random.nextFloat() < 0.35F) {
                    this.placeBlock(level, stone, cx - 2, 1 + random.nextInt(3), z, box);
                }
                if (random.nextFloat() < 0.35F) {
                    this.placeBlock(level, stone, cx + 2, 1 + random.nextInt(3), z, box);
                }
                // 顶部符文砖微光照明
                if (z % 4 == 2) {
                    this.placeBlock(level, runeBricks, cx, 4, z, box);
                }
            }
            // 幻象墙暗格 1(西侧 z9):真视可见的暗门,内藏回访宝箱(复用镜面神殿暗室池)
            this.generateBox(level, box, cx - 6, 0, 8, cx - 3, 4, 10, bricks, bricks, false);
            sealHiddenBox(level, box, anchored, cx - 6, cx - 3, 8, 10, 0, 4);
            this.generateBox(level, box, cx - 5, 1, 9, cx - 4, 3, 9, air, air, false);
            for (int yy = 1; yy <= 2; yy++) {
                this.placeBlock(level, hidden, cx - 3, yy, 9, box);
            }
            buildAccessibleChest(level, random, box, cx - 5, 1, 9, ModLootTables.ECHO_TEMPLE_HIDDEN);
            // 幻象墙暗格 2(东侧 z14):假门——真视玩家也要判断哪扇门有意义
            this.generateBox(level, box, cx + 3, 0, 13, cx + 5, 4, 15, bricks, bricks, false);
            this.placeBlock(level, air, cx + 4, 1, 14, box);
            this.placeBlock(level, air, cx + 4, 2, 14, box);
            for (int yy = 1; yy <= 2; yy++) {
                this.placeBlock(level, hidden, cx + 3, yy, 14, box);
            }
            this.placeBlock(level, cracked, cx + 4, 0, 14, box);

            // 回廊尽头:整面幻象墙封住隐藏房入口(真视发现,2×2 暗门)
            for (int dx = -2; dx <= 2; dx++) {
                for (int yy = 1; yy <= 4; yy++) {
                    boolean door = Math.abs(dx) <= 0 && yy <= 2;
                    this.placeBlock(level, door ? hidden : bricks, cx + dx, yy, 19, box);
                }
            }

            // ---- 隐藏房(z 20..32):13×13 镜面长厅,镜尘执事战斗区 ----
            this.generateBox(level, box, 0, 0, 20, WIDTH - 1, 7, DEPTH - 1, bricks, bricks, false);
            sealHiddenBox(level, box, anchored, 0, WIDTH - 1, 20, DEPTH - 1, 0, 7);
            this.generateBox(level, box, 1, 1, 21, WIDTH - 2, 6, DEPTH - 2, air, air, false);
            // 进门通洞(与幻象暗门相接)
            this.placeBlock(level, air, cx, 1, 20, box);
            this.placeBlock(level, air, cx, 2, 20, box);
            int roomCz = 26; // 房间中心 z
            // 三面镜墙:东西北三面竖直镜石板(站位参考,4.3.2 表现与遮挡用,不封锁)
            for (int z = 22; z <= 30; z += 2) {
                for (int yy = 1; yy <= 4; yy++) {
                    this.placeBlock(level, stone, 1, yy, z, box);
                    this.placeBlock(level, stone, WIDTH - 2, yy, z, box);
                }
            }
            for (int x = 3; x <= WIDTH - 4; x += 2) {
                for (int yy = 1; yy <= 4; yy++) {
                    this.placeBlock(level, stone, x, yy, DEPTH - 2, box);
                }
            }
            // 地板:镜石砖混镜石,中央 3×3 碎镜地台
            for (int x = 1; x <= WIDTH - 2; x++) {
                for (int z = 21; z <= DEPTH - 2; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? stone : bricks, x, 0, z, box);
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.placeBlock(level, cracked, cx + dx, 0, roomCz + dz, box);
                }
            }
            // 三个镜尘站位:与 MirrorDustButler.STATIONS 对应(锚点 = 地台中心上方出生点)
            for (int[] station : new int[][]{{0, -4}, {4, 3}, {-4, 3}}) {
                this.placeBlock(level, cracked, cx + station[0], 0, roomCz + station[1], box);
            }
            // 顶部符文砖微光
            this.placeBlock(level, runeBricks, cx, 6, roomCz, box);
            this.placeBlock(level, runeBricks, 3, 6, 23, box);
            this.placeBlock(level, runeBricks, WIDTH - 4, 6, 29, box);

            // V0.6E 真视宝箱(倒影井隐藏房):开箱前置真视权限,内容强化材料+研究拓片
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(),
                    2, 1, 30, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(2, 30), this.getWorldY(1),
                    this.getWorldZ(2, 30));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(cn.kurt6.unknown_echoes.registry.ModLootTables.TRUE_SIGHT_VAULT,
                        random.nextLong());
            }

            // 守护者场地计时器:埋在地台正下方,服务端校验真视回响/镜像守护者记录
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(),
                    cx, 0, roomCz, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(cx, roomCz), this.getWorldY(0),
                    this.getWorldZ(cx, roomCz));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id("mirror_dust_butler"), 1);
                spawner.setChallengeGate(EchoAbilityType.TRUE_SIGHT_ECHO,
                        UnknownEchoes.id("mirror_guardian"),
                        "message.unknown_echoes.mirror_cloister.locked");
            }
        }

        /**
         * 隐藏奖励/挑战区的外壳与下方两层封底使用锚定封印石。
         * 入口幻象墙仍由调用点覆盖,保证真视路线可通、挖地道不可通。
         */
        private void sealHiddenBox(WorldGenLevel level, BoundingBox box, BlockState anchored,
                                   int x1, int x2, int z1, int z2, int floorY, int topY) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x, floorY, z, box);
                    this.placeBlock(level, anchored, x, topY, z, box);
                    for (int y = floorY - 2; y < floorY; y++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            for (int y = floorY + 1; y < topY; y++) {
                for (int x = x1; x <= x2; x++) {
                    this.placeBlock(level, anchored, x, y, z1, box);
                    this.placeBlock(level, anchored, x, y, z2, box);
                }
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x1, y, z, box);
                    this.placeBlock(level, anchored, x2, y, z, box);
                }
            }
        }
    }
}
