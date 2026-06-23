package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 镜湖神殿:镜湖湖底的封闭大殿(V0.5B 真视回响获取点)。
 * 殿内被"幻象斥开湖水"——实心镜石砖墙体 + 内部空气(海底神殿式气穴),一道拱门入口。
 * 内容:镜像符印阵(十枚混排在四面墙,4 真 6 假,真假同貌)、真视核心祭坛、
 * 隐纹石砖暗室(真视回访宝箱)、镜像守护者(只生成一次)。所有机关判定都在服务端。
 */
public class MirrorTempleStructure extends SinglePieceStructure {
    public static final MapCodec<MirrorTempleStructure> CODEC = simpleCodec(MirrorTempleStructure::new);

    private static final int WIDTH = 49;
    private static final int DEPTH = 49;
    private static final int HEIGHT = 16;
    private static final int BOSS_ARENA_SIZE = 31;

    public MirrorTempleStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 镜湖群系存在旱地区段:湖底气穴大殿必须整体沉在水里(殿高 12,要求中心水深 ≥ 13)。
        // 不调用 super——SinglePieceStructure 默认拒绝"最低地表 < 海平面"的位置,水下结构必须绕开。
        //
        // CRITICAL: 不在此处做连续水方块校验——/locate 会对每个候选区块调用此方法,
        // 累积的噪声柱采样会拖服务端主线程超看门狗限制。这里用五点高度图预判,严格校验推迟到 postProcess。
        if (!LakeBedPlacement.hasSubmergedFootprint(context, WIDTH, 17, 9)) {
            return java.util.Optional.empty();
        }
        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG,
                builder -> builder.addPiece(new Piece(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MIRROR_TEMPLE.get();
    }

    public static class Piece extends EchoStructurePiece {

        private int oceanFloorY = -1;
        private boolean spawnedBoss = false;
        private boolean rejected = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.MIRROR_TEMPLE_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.MIRROR_TEMPLE_PIECE.get(), tag);
            this.oceanFloorY = tag.getInt("OceanFloorY");
            if (!tag.contains("OceanFloorY")) {
                this.oceanFloorY = -1;
            }
            this.spawnedBoss = tag.getBoolean("SpawnedBoss");
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("SpawnedBoss", this.spawnedBoss);
            tag.putBoolean("Rejected", this.rejected);
        }

        /** 对齐湖底并校验水深,首次成功后锁定。 */
        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.oceanFloorY >= 0) {
                return true;
            }
            int total = 0;
            int count = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
                for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
                    cursor.set(x, 64, z);
                    if (chunkBox.isInside(cursor)) {
                        total += level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        count++;
                    }
                }
            }
            if (count == 0) {
                return false;
            }
            this.oceanFloorY = total / count;

            // 水深校验:外廊 + Boss 场扩大后仍推迟到 postProcess,避免 /locate 采样过重。
            if (!LakeBedPieceHelper.checkWaterDepth(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH, 17, 9)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.oceanFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.adjustToLakeFloor(level, box, generator)) {
                return;
            }

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState stone = ModBlocks.MIRROR_STONE.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState altar = ModBlocks.TRUE_SIGHT_ALTAR.get().defaultBlockState();
            BlockState sigil = ModBlocks.MIRROR_SIGIL.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            // 实心外壳(墙厚 1,顶底封死)——海底气穴的前提
            this.generateBox(level, box, 0, 0, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1, bricks, bricks, false);
            // 内部排空:幻象斥水的干燥大殿
            this.generateBox(level, box, 1, 1, 1, WIDTH - 2, HEIGHT - 2, DEPTH - 2, air, air, false);

            // 地板:镜石砖混镜石
            for (int x = 1; x < WIDTH - 1; x++) {
                for (int z = 1; z < DEPTH - 1; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.25F ? stone : bricks, x, 0, z, box);
                }
            }

            // 入口拱门:南面正中 3 宽 3 高(进出会放水,但气穴主体保留——与海底遗迹一致的体验)
            for (int dx = -1; dx <= 1; dx++) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, air, WIDTH / 2 + dx, y, 0, box);
                }
            }
            buildOuterCloister(level, box, bricks, hidden, air);
            buildSigilRooms(level, box, bricks, sigil, air);
            buildHighGallery(level, box, bricks, runeBricks);

            // 殿内列柱与符文砖照明(四根角柱 + 柱顶符文砖)
            for (int[] column : new int[][]{{3, 3}, {WIDTH - 4, 3}, {3, DEPTH - 4}, {WIDTH - 4, DEPTH - 4}}) {
                for (int y = 1; y <= HEIGHT - 3; y++) {
                    this.placeBlock(level, bricks, column[0], y, column[1], box);
                }
                this.placeBlock(level, runeBricks, column[0], 3, column[1], box);
            }

            // 镜像符印阵:十二枚符印混排在四面墙上(4 真 8 假,真假同貌,跨区块确定性乱序)。
            // 不再"真假各占一面墙"——只能靠观察倒影涟漪/鱼群行为辨认。
            // V0.6B 难度与奖励同步上调:十枚 4 真 6 假 → 十二枚 4 真 8 假(每面墙 3 枚)。
            RandomSource sigilRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);
            int[][] sigilSpots = {
                    {1, 4}, {1, DEPTH / 2}, {1, DEPTH - 5},                   // 西墙
                    {WIDTH - 2, 4}, {WIDTH - 2, DEPTH / 2}, {WIDTH - 2, DEPTH - 6}, // 东墙(避开暗室)
                    {5, 1}, {8, 1}, {WIDTH - 6, 1},                           // 南墙(避开门洞)
                    {4, DEPTH - 2}, {9, DEPTH - 2}, {15, DEPTH - 2}           // 北墙(避开祭坛与暗室)
            };
            // 抽 4 个位置放真符印
            int[] perm = new int[sigilSpots.length];
            for (int i = 0; i < perm.length; i++) {
                perm[i] = i;
            }
            for (int i = perm.length - 1; i > 0; i--) {
                int j = sigilRandom.nextInt(i + 1);
                int tmp = perm[i];
                perm[i] = perm[j];
                perm[j] = tmp;
            }
            boolean[] isReal = new boolean[sigilSpots.length];
            for (int i = 0; i < 4; i++) {
                isReal[perm[i]] = true;
            }
            for (int i = 0; i < sigilSpots.length; i++) {
                this.placeBlock(level, sigil, sigilSpots[i][0], 2, sigilSpots[i][1], box);
                // 真假写入服务端 BlockEntity(V0.6A 迁移,红线 #9:方块状态会同步客户端)
                BlockPos sigilPos = new BlockPos(
                        this.getWorldX(sigilSpots[i][0], sigilSpots[i][1]),
                        this.getWorldY(2),
                        this.getWorldZ(sigilSpots[i][0], sigilSpots[i][1]));
                if (box.isInside(sigilPos) && level.getBlockEntity(sigilPos)
                        instanceof cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity sigilEntity) {
                    sigilEntity.setReal(isReal[i]);
                }
            }

            // 真视核心祭坛:北端高台
            for (int x = WIDTH / 2 - 1; x <= WIDTH / 2 + 1; x++) {
                for (int z = DEPTH - 5; z <= DEPTH - 3; z++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
            }
            this.placeBlock(level, altar, WIDTH / 2, 2, DEPTH - 4, box);

            // V0.6E 真视碑文:祭坛高台西侧——无真视只见乱纹,持真视读出铭文并计研究回访点
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(),
                    WIDTH / 2 - 3, 1, DEPTH - 4, box);

            // 隐藏暗室:东北角 3x3,整面隐纹石砖幻象墙封住(真视回响回访点)
            this.generateBox(level, box, WIDTH - 5, 1, DEPTH - 5, WIDTH - 2, 4, DEPTH - 2, air, air, false);
            // 暗室贴外墙,外侧/底部必须用永久封壳,避免从湖床或神殿外壳挖入。
            for (int x = WIDTH - 5; x <= WIDTH - 1; x++) {
                for (int z = DEPTH - 5; z <= DEPTH - 1; z++) {
                    this.placeBlock(level, anchored, x, 0, z, box);
                    this.placeBlock(level, anchored, x, 5, z, box);
                    for (int y = -2; y < 0; y++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            for (int y = 1; y <= 4; y++) {
                for (int z = DEPTH - 5; z <= DEPTH - 1; z++) {
                    this.placeBlock(level, anchored, WIDTH - 1, y, z, box);
                }
                for (int x = WIDTH - 5; x <= WIDTH - 1; x++) {
                    this.placeBlock(level, anchored, x, y, DEPTH - 1, box);
                }
            }
            for (int y = 1; y <= 3; y++) {
                for (int z = DEPTH - 5; z <= DEPTH - 2; z++) {
                    this.placeBlock(level, hidden, WIDTH - 5, y, z, box);
                }
                for (int x = WIDTH - 5; x <= WIDTH - 2; x++) {
                    this.placeBlock(level, hidden, x, y, DEPTH - 5, box);
                }
            }
            buildAccessibleChest(level, random, box, WIDTH - 3, 1, DEPTH - 3,
                    ModLootTables.ECHO_TEMPLE_HIDDEN);
            // V0.6E 真视宝箱:暗室内,开箱前置真视权限(强化材料+研究拓片)
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(),
                    WIDTH - 4, 1, DEPTH - 2, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(WIDTH - 4, DEPTH - 2),
                    this.getWorldY(1), this.getWorldZ(WIDTH - 4, DEPTH - 2));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                    vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
            buildGuardianArena(level, box, bricks, runeBricks);
            buildTrueSightRevisitDoor(level, random, box, hidden, anchored);

            // 镜像守护者:大殿中央(只生成一次)
            int bossX = this.getWorldX(WIDTH / 2, DEPTH / 2);
            int bossZ = this.getWorldZ(WIDTH / 2, DEPTH / 2);
            int bossY = this.getWorldY(1);
            BlockPos bossPos = new BlockPos(bossX, bossY, bossZ);
            if (!this.spawnedBoss && box.isInside(bossPos)) {
                MirrorGuardian boss = ModEntities.MIRROR_GUARDIAN.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossX + 0.5D, bossY, bossZ + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                    this.spawnedBoss = true;
                }
            }
        }

        private void buildOuterCloister(WorldGenLevel level, BoundingBox box,
                                        BlockState bricks, BlockState hidden, BlockState air) {
            for (int i = 4; i <= WIDTH - 5; i++) {
                this.placeBlock(level, ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState(), i, 1, 4, box);
                this.placeBlock(level, ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState(), i, 1, DEPTH - 5, box);
                this.placeBlock(level, ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState(), 4, 1, i, box);
                this.placeBlock(level, ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState(), WIDTH - 5, 1, i, box);
            }
            this.generateBox(level, box, 7, 1, 7, 12, 3, 7, hidden, hidden, false);
            this.generateBox(level, box, WIDTH - 13, 1, 9, WIDTH - 8, 3, 9, hidden, hidden, false);
            this.placeBlock(level, air, WIDTH / 2, 1, 4, box);
            this.placeBlock(level, bricks, WIDTH / 2, 1, 8, box);
        }

        private void buildSigilRooms(WorldGenLevel level, BoundingBox box,
                                     BlockState bricks, BlockState sigil, BlockState air) {
            int[][] rooms = {{10, 11}, {WIDTH / 2 - 4, 11}, {WIDTH - 18, 11}};
            for (int[] room : rooms) {
                this.generateBox(level, box, room[0], 1, room[1], room[0] + 8, 6, room[1] + 8, bricks, bricks, false);
                this.generateBox(level, box, room[0] + 1, 2, room[1] + 1, room[0] + 7, 5, room[1] + 7, air, air, false);
                this.placeBlock(level, air, room[0] + 4, 2, room[1] + 8, box);
                this.placeBlock(level, sigil, room[0] + 2, 3, room[1] + 1, box);
                this.placeBlock(level, sigil, room[0] + 4, 3, room[1] + 1, box);
                this.placeBlock(level, sigil, room[0] + 6, 3, room[1] + 1, box);
            }
        }

        private void buildHighGallery(WorldGenLevel level, BoundingBox box, BlockState bricks, BlockState runeBricks) {
            for (int z = 22; z <= 30; z++) {
                for (int x = 8; x <= WIDTH - 9; x++) {
                    if ((x + z) % 5 != 0) {
                        this.placeBlock(level, bricks, x, 7, z, box);
                    }
                }
                if (z % 4 == 2) {
                    this.placeBlock(level, runeBricks, WIDTH / 2, 8, z, box);
                }
            }
        }

        private void buildGuardianArena(WorldGenLevel level, BoundingBox box, BlockState bricks, BlockState runeBricks) {
            int c = WIDTH / 2;
            int r = BOSS_ARENA_SIZE / 2;
            for (int x = c - r; x <= c + r; x++) {
                for (int z = DEPTH - 17 - r; z <= DEPTH - 17 + r; z++) {
                    boolean edge = x == c - r || x == c + r || z == DEPTH - 17 - r || z == DEPTH - 17 + r;
                    this.placeBlock(level, edge ? runeBricks : bricks, x, edge ? 2 : 1, z, box);
                }
            }
        }

        private void buildTrueSightRevisitDoor(WorldGenLevel level, RandomSource random, BoundingBox box,
                                               BlockState hidden, BlockState anchored) {
            int x0 = 6;
            int z0 = DEPTH - 13;
            this.generateBox(level, box, x0, 1, z0, x0 + 8, 5, z0 + 8, anchored, anchored, false);
            this.generateBox(level, box, x0 + 1, 2, z0 + 1, x0 + 7, 4, z0 + 7,
                    Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(level, hidden, x0 + 4, 2, z0, box);
            this.placeBlock(level, hidden, x0 + 4, 3, z0, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), x0 + 2, 2, z0 + 3, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), x0 + 6, 2, z0 + 6, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(x0 + 6, z0 + 6),
                    this.getWorldY(2), this.getWorldZ(x0 + 6, z0 + 6));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
        }
    }
}
