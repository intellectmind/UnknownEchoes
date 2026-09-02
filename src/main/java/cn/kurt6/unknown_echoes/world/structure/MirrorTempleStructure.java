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

public class MirrorTempleStructure extends SinglePieceStructure {
    public static final MapCodec<MirrorTempleStructure> CODEC = simpleCodec(MirrorTempleStructure::new);

    private static final int WIDTH = 49;
    private static final int DEPTH = 49;
    private static final int HEIGHT = 24;
    private static final int BOSS_ARENA_SIZE = 37;
    private static final int BOSS_ARENA_Z = DEPTH - 19;
    /** Boss 出生点与主战区中心重合,复战与首次生成使用同一场地锚点。 */
    private static final int BOSS_SPAWN_Z = BOSS_ARENA_Z;

    public MirrorTempleStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public java.util.Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 定位阶段只做轻量的五点高度图预判；严格水深检查留到 postProcess，避免 /locate 反复采样整列噪声。
        Piece piece = new Piece(context.random(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        BoundingBox footprint = piece.getBoundingBox();
        if (!LakeBedPlacement.hasSubmergedFootprint(context,
                footprint.minX(), footprint.minZ(), WIDTH, 24, 24)) {
            return java.util.Optional.empty();
        }
        int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(
                context, footprint.minX(), footprint.minZ(), WIDTH, DEPTH);
        if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
            return java.util.Optional.empty();
        }
        piece.anchorToLakeFloor(floorY);
        int centerX = context.chunkPos().getMiddleBlockX();
        int centerZ = context.chunkPos().getMiddleBlockZ();
        return java.util.Optional.of(new GenerationStub(
                new BlockPos(centerX, context.chunkGenerator().getSeaLevel(), centerZ),
                builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MIRROR_TEMPLE.get();
    }

    public static class Piece extends EchoStructurePiece {
        private static final int[][] MIRROR_TEMPLE_NODES = {
                {6, 6}, {24, 6}, {42, 6},
                {6, 24}, {42, 24},
                {6, 42}, {24, 42}, {42, 42},
                {15, 15}, {33, 15}, {15, 33}, {33, 33}
        };

        private int oceanFloorY = -1;
        private boolean spawnedBoss = false;
        private boolean bossSpawnChunkVisited = false;
        private boolean altarChunkVisited = false;
        private boolean rejected = false;
        private BoundingBox activeGenerationBox;
        private int activeLocalMinX;
        private int activeLocalMaxX;
        private int activeLocalMinY;
        private int activeLocalMaxY;
        private int activeLocalMinZ;
        private int activeLocalMaxZ;

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
            this.bossSpawnChunkVisited = tag.getBoolean("BossSpawnChunkVisited");
            this.altarChunkVisited = tag.getBoolean("AltarChunkVisited");
            this.rejected = tag.getBoolean("Rejected");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("OceanFloorY", this.oceanFloorY);
            tag.putBoolean("SpawnedBoss", this.spawnedBoss);
            tag.putBoolean("BossSpawnChunkVisited", this.bossSpawnChunkVisited);
            tag.putBoolean("AltarChunkVisited", this.altarChunkVisited);
            tag.putBoolean("Rejected", this.rejected);
        }

        public void anchorToLakeFloor(int floorY) {
            if (this.oceanFloorY >= 0) {
                return;
            }
            this.oceanFloorY = floorY;
            this.boundingBox.move(0, floorY - this.boundingBox.minY() - 1, 0);
        }

        private boolean adjustToLakeFloor(WorldGenLevel level, BoundingBox chunkBox, ChunkGenerator generator) {
            if (this.rejected) {
                return false;
            }
            if (this.oceanFloorY >= 0) {
                return true;
            }
            int floorY = LakeBedPieceHelper.averageOceanFloorByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH);
            if (!LakeBedPieceHelper.isSafeLakeFloor(floorY)) {
                this.rejected = true;
                return false;
            }
            this.oceanFloorY = floorY;

            if (!LakeBedPieceHelper.checkWaterDepthByBaseHeight(level, generator,
                    this.boundingBox.minX(), this.boundingBox.minZ(), WIDTH, DEPTH, 24, 24)) {
                this.rejected = true;
                return false;
            }

            this.boundingBox.move(0, this.oceanFloorY - this.boundingBox.minY() - 1, 0);
            return true;
        }

        /** 计算当前区块对应的局部坐标范围,避免每个区块重复遍历整座神殿。 */
        private void activateGenerationBox(BoundingBox box) {
            this.activeGenerationBox = box;
            Direction orientation = this.getOrientation();
            if (orientation == null) {
                this.activeLocalMinX = Integer.MIN_VALUE;
                this.activeLocalMaxX = Integer.MAX_VALUE;
                this.activeLocalMinY = Integer.MIN_VALUE;
                this.activeLocalMaxY = Integer.MAX_VALUE;
                this.activeLocalMinZ = Integer.MIN_VALUE;
                this.activeLocalMaxZ = Integer.MAX_VALUE;
                return;
            }

            int worldMinX = Math.max(box.minX(), this.boundingBox.minX());
            int worldMaxX = Math.min(box.maxX(), this.boundingBox.maxX());
            int worldMinY = Math.max(box.minY(), this.boundingBox.minY());
            int worldMaxY = Math.min(box.maxY(), this.boundingBox.maxY());
            int worldMinZ = Math.max(box.minZ(), this.boundingBox.minZ());
            int worldMaxZ = Math.min(box.maxZ(), this.boundingBox.maxZ());

            this.activeLocalMinY = worldMinY - this.boundingBox.minY();
            this.activeLocalMaxY = worldMaxY - this.boundingBox.minY();
            if (orientation == Direction.NORTH || orientation == Direction.SOUTH) {
                this.activeLocalMinX = worldMinX - this.boundingBox.minX();
                this.activeLocalMaxX = worldMaxX - this.boundingBox.minX();
            } else {
                this.activeLocalMinX = worldMinZ - this.boundingBox.minZ();
                this.activeLocalMaxX = worldMaxZ - this.boundingBox.minZ();
            }

            switch (orientation) {
                case NORTH -> {
                    this.activeLocalMinZ = this.boundingBox.maxZ() - worldMaxZ;
                    this.activeLocalMaxZ = this.boundingBox.maxZ() - worldMinZ;
                }
                case SOUTH -> {
                    this.activeLocalMinZ = worldMinZ - this.boundingBox.minZ();
                    this.activeLocalMaxZ = worldMaxZ - this.boundingBox.minZ();
                }
                case WEST -> {
                    this.activeLocalMinZ = this.boundingBox.maxX() - worldMaxX;
                    this.activeLocalMaxZ = this.boundingBox.maxX() - worldMinX;
                }
                case EAST -> {
                    this.activeLocalMinZ = worldMinX - this.boundingBox.minX();
                    this.activeLocalMaxZ = worldMaxX - this.boundingBox.minX();
                }
                default -> throw new IllegalStateException("Mirror temple must use a horizontal orientation");
            }
        }

        private boolean isInsideActiveGenerationBox(int x, int y, int z, BoundingBox box) {
            if (box != this.activeGenerationBox) {
                this.activateGenerationBox(box);
            }
            return x >= this.activeLocalMinX && x <= this.activeLocalMaxX
                    && y >= this.activeLocalMinY && y <= this.activeLocalMaxY
                    && z >= this.activeLocalMinZ && z <= this.activeLocalMaxZ;
        }

        @Override
        protected void placeBlock(WorldGenLevel level, BlockState blockState,
                                  int x, int y, int z, BoundingBox box) {
            if (!this.isInsideActiveGenerationBox(x, y, z, box)) {
                return;
            }
            super.placeBlock(level, blockState, x, y, z, box);
        }

        @Override
        protected void generateBox(WorldGenLevel level, BoundingBox box,
                                   int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ,
                                   BlockState boundaryBlockState, BlockState insideBlockState,
                                   boolean existingOnly) {
            if (box != this.activeGenerationBox) {
                this.activateGenerationBox(box);
            }
            int originalMinX = minX;
            int originalMinY = minY;
            int originalMinZ = minZ;
            int originalMaxX = maxX;
            int originalMaxY = maxY;
            int originalMaxZ = maxZ;
            minX = Math.max(minX, this.activeLocalMinX);
            minY = Math.max(minY, this.activeLocalMinY);
            minZ = Math.max(minZ, this.activeLocalMinZ);
            maxX = Math.min(maxX, this.activeLocalMaxX);
            maxY = Math.min(maxY, this.activeLocalMaxY);
            maxZ = Math.min(maxZ, this.activeLocalMaxZ);
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                return;
            }
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (!existingOnly || !this.getBlock(level, x, y, z, box).isAir()) {
                            boolean boundary = y == originalMinY || y == originalMaxY
                                    || x == originalMinX || x == originalMaxX
                                    || z == originalMinZ || z == originalMaxZ;
                            this.placeBlock(level, boundary ? boundaryBlockState : insideBlockState,
                                    x, y, z, box);
                        }
                    }
                }
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.adjustToLakeFloor(level, box, generator)) {
                return;
            }
            box = LakeBedPieceHelper.intersection(box, this.boundingBox);
            if (box == null) {
                return;
            }
            this.activateGenerationBox(box);

            BlockState bricks = ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState();
            BlockState runeBricks = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState altar = ModBlocks.TRUE_SIGHT_ALTAR.get().defaultBlockState();
            BlockState sigil = ModBlocks.MIRROR_SIGIL.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            this.generateBox(level, box, 0, 0, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1,
                    bricks, air, false);

            for (int dx = -1; dx <= 1; dx++) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, air, WIDTH / 2 + dx, y, 0, box);
                }
            }
            buildOuterCloister(level, box, bricks, hidden, air);
            buildSigilRooms(level, box, bricks, sigil, air);
            // 高廊只保留外圈立面,中央战区必须从地板一直通到穹顶。
            buildHighGallery(level, box, bricks, runeBricks);

            for (int[] column : new int[][]{{3, 3}, {WIDTH - 4, 3}, {3, DEPTH - 4}, {WIDTH - 4, DEPTH - 4}}) {
                for (int y = 1; y <= HEIGHT - 3; y++) {
                    this.placeBlock(level, bricks, column[0], y, column[1], box);
                }
                this.placeBlock(level, runeBricks, column[0], 3, column[1], box);
            }

            RandomSource sigilRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);
            int[][] sigilSpots = {
                    {1, 4}, {1, DEPTH / 2}, {1, DEPTH - 5},
                    {WIDTH - 2, 4}, {WIDTH - 2, DEPTH / 2}, {WIDTH - 2, DEPTH - 6},
                    {5, 1}, {8, 1}, {WIDTH - 6, 1},
                    {4, DEPTH - 2}, {9, DEPTH - 2}, {15, DEPTH - 2}
            };

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

                BlockPos sigilPos = new BlockPos(
                        this.getWorldX(sigilSpots[i][0], sigilSpots[i][1]),
                        this.getWorldY(2),
                        this.getWorldZ(sigilSpots[i][0], sigilSpots[i][1]));
                if (box.isInside(sigilPos) && level.getBlockEntity(sigilPos)
                        instanceof cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity sigilEntity) {
                    sigilEntity.setReal(isReal[i]);
                }
            }

            for (int x = WIDTH / 2 - 1; x <= WIDTH / 2 + 1; x++) {
                for (int z = DEPTH - 5; z <= DEPTH - 3; z++) {
                    this.placeBlock(level, bricks, x, 1, z, box);
                }
            }
            this.placeBlock(level, altar, WIDTH / 2, 2, DEPTH - 4, box);

            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(),
                    WIDTH / 2 - 3, 1, DEPTH - 4, box);

            this.generateBox(level, box, WIDTH - 5, 1, DEPTH - 5, WIDTH - 2, 4, DEPTH - 2, air, air, false);

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

            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(),
                    WIDTH - 4, 1, DEPTH - 2, box);
            BlockPos vaultPos = new BlockPos(this.getWorldX(WIDTH - 4, DEPTH - 2),
                    this.getWorldY(1), this.getWorldZ(WIDTH - 4, DEPTH - 2));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                    vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
            buildGuardianArena(level, box, bricks, runeBricks);

            buildAccessibleChest(level, random, box, WIDTH / 2 + 7, 1, DEPTH / 2 - 6,
                    ModLootTables.T5_MINIBOSS_REPEAT);
            buildTrueSightRevisitDoor(level, random, box, hidden, anchored);
            buildMonumentFrame(level, random, box);

            placeStructureSpawner(level, random, box, 6, 1, 25,
                    ModEntities.MIRROR_DUST_DECOY.get(), bricks, runeBricks);
            for (int[] node : MIRROR_TEMPLE_NODES) {
                this.placeBlock(level, ModBlocks.MIRROR_STONE.get().defaultBlockState(),
                        node[0], 0, node[1], box);
            }

            int bossX = this.getWorldX(WIDTH / 2, BOSS_SPAWN_Z);
            int bossZ = this.getWorldZ(WIDTH / 2, BOSS_SPAWN_Z);
            // 地板在局部 y=1; y=3 给 5.4 格高的守护者留下完整净空。
            int bossY = this.getWorldY(3);
            BlockPos bossPos = new BlockPos(bossX, bossY, bossZ);
            BlockPos altarPos = new BlockPos(this.getWorldX(WIDTH / 2, DEPTH - 4),
                    this.getWorldY(2), this.getWorldZ(WIDTH / 2, DEPTH - 4));
            if (box.isInside(bossPos)) {
                this.bossSpawnChunkVisited = true;
            }
            if (box.isInside(altarPos)
                    && level.getBlockState(altarPos).is(ModBlocks.TRUE_SIGHT_ALTAR.get())) {
                this.altarChunkVisited = true;
            }
            if (!this.spawnedBoss && this.bossSpawnChunkVisited && this.altarChunkVisited) {
                MirrorGuardian boss = ModEntities.MIRROR_GUARDIAN.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossX + 0.5D, bossY, bossZ + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    boss.bindRechallengeArena(altarPos, bossPos);
                    if (level.addFreshEntity(boss)) {
                        this.spawnedBoss = true;
                    } else {
                        boss.discard();
                    }
                }
            }
        }

        private void buildMonumentFrame(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = WIDTH / 2;
            BlockState mirror = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState fractured = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState prism = ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState();
            architecturalRing(level, box, c, 1, c, 22, fractured);
            // 八柱外环只做远景轮廓:柱数再加会把 37x37 战区切成柱廊。
            buildCircularColonnade(level, box, c, 2, c, 22, 8, 9,
                    mirror, prism, fractured);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, 2, c, 22, direction,
                        3, 9, mirror, prism);
            }
            architecturalRing(level, box, c, 14, c, 17,
                    Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
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
            // 高位立面只能做在两侧外廊:铺满战区上方会把守护者战区封成上下两层。
            for (int z = 6; z <= DEPTH - 7; z++) {
                if (z % 4 == 2) {
                    this.placeBlock(level, runeBricks, 5, 7, z, box);
                    this.placeBlock(level, runeBricks, WIDTH - 6, 7, z, box);
                } else {
                    this.placeBlock(level, bricks, 5, 7, z, box);
                    this.placeBlock(level, bricks, WIDTH - 6, 7, z, box);
                }
            }
        }

        private void buildGuardianArena(WorldGenLevel level, BoundingBox box, BlockState bricks, BlockState runeBricks) {
            int c = WIDTH / 2;
            int r = BOSS_ARENA_SIZE / 2;
            int arenaZ = BOSS_ARENA_Z;
            for (int x = c - r; x <= c + r; x++) {
                for (int z = arenaZ - r; z <= arenaZ + r; z++) {
                    boolean edge = x == c - r || x == c + r || z == arenaZ - r || z == arenaZ + r;
                    this.placeBlock(level, edge ? runeBricks : bricks, x, edge ? 2 : 1, z, box);
                }
            }

            BlockState fractured = ModBlocks.FRACTURED_MIRROR_TILE.get().defaultBlockState();
            BlockState mirror = ModBlocks.MIRROR_DUST_BRICKS.get().defaultBlockState();
            BlockState prism = ModBlocks.PRISM_DUST_CLUSTER.get().defaultBlockState();
            // 拱厅必须收在北侧:压到 37x37 主战区上会把 Boss 走位区切成狭长通道。
            buildRibbedVaultHall(level, box, c, 2, 6, Footprint.of(15, 5), 12, 6, bricks, bricks, fractured, prism);
            buildTwinTowerFacade(level, box, c, 1, 4,
                    18, 3, 15, bricks, fractured, prism);
            buildArcadedWing(level, box, 7, 2, 6, Footprint.square(4), 8, 4, mirror, bricks, fractured, prism);
            buildArcadedWing(level, box, WIDTH - 8, 2, 6, Footprint.square(4), 8, 4, mirror, bricks, fractured, prism);
            // 后殿必须留在建筑端头,不能伸进 Boss 圆场。
            buildBossApse(level, box, c, 2, 5,
                    Direction.NORTH, 4, 7, mirror, bricks, fractured, prism);

            // 第三方结构改动或旧版布局可能在圆场内留下碰撞体,生成时统一清掉中心净空。
            for (int x = c - r + 2; x <= c + r - 2; x++) {
                for (int z = arenaZ - r + 2; z <= arenaZ + r - 2; z++) {
                    if ((x - c) * (x - c) + (z - arenaZ) * (z - arenaZ) > (r - 2) * (r - 2)) {
                        continue;
                    }
                    for (int y = 2; y <= HEIGHT - 2; y++) {
                        // 祭坛位于战区边缘,必须保留,否则 Boss 生成条件会被净空逻辑抹掉。
                        if (x == WIDTH / 2 && z == DEPTH - 4 && y == 2) {
                            continue;
                        }
                        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }

            buildBossArenaMandala(level, box, c, 1, arenaZ, r - 1,
                    fractured, mirror,
                    ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            buildArenaLightRing(level, box, c, 3, arenaZ, r - 1, 8, 8, mirror, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            illuminateInterior(level, box, c, c, 17, 13, 16, 5, ModBlocks.REEF_LANTERN_GLASS.get().defaultBlockState());
            buildStairCornice(level, box, c, 15, c, Footprint.of(15, 18), ModBlocks.MIRROR_STONE_BRICK_STAIRS.get().defaultBlockState(), ModBlocks.MIRROR_STONE_BRICK_SLAB.get().defaultBlockState(), true);
            buildParapet(level, box, c, 16, c, Footprint.of(15, 18), 7, ModBlocks.MIRROR_STONE_BRICK_WALL.get().defaultBlockState(), fractured);
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
