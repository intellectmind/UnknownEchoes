package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.util.RandomSource;

/**
 * 第 10 章群系结构补齐:用单 piece 代码生成小结构、连接结构和主线外场。
 * 关键奖励仍由服务端能力/终端/Boss 逻辑结算,这里仅提供可定位、可探索的结构实体。
 */
public class RegionSiteStructure extends SinglePieceStructure {
    public static final MapCodec<RegionSiteStructure> MEMORY_PILLAR_COURTYARD_CODEC = codec(Kind.MEMORY_PILLAR_COURTYARD);
    public static final MapCodec<RegionSiteStructure> LOST_CAMP_CODEC = codec(Kind.LOST_CAMP);
    public static final MapCodec<RegionSiteStructure> OVERWORLD_LOST_CAMP_CODEC = codec(Kind.OVERWORLD_LOST_CAMP);
    public static final MapCodec<RegionSiteStructure> WIND_ERODED_STEPS_CODEC = codec(Kind.WIND_ERODED_STEPS);
    public static final MapCodec<RegionSiteStructure> WIND_RUNE_STONE_CIRCLE_CODEC = codec(Kind.WIND_RUNE_STONE_CIRCLE);
    public static final MapCodec<RegionSiteStructure> REFLECTION_WELL_CODEC = codec(Kind.REFLECTION_WELL);
    public static final MapCodec<RegionSiteStructure> MIRROR_LAKE_CACHE_CODEC = codec(Kind.MIRROR_LAKE_CACHE);
    public static final MapCodec<RegionSiteStructure> TIDE_BUOY_ARRAY_CODEC = codec(Kind.TIDE_BUOY_ARRAY);
    public static final MapCodec<RegionSiteStructure> REEF_HUT_CODEC = codec(Kind.REEF_HUT);
    public static final MapCodec<RegionSiteStructure> LAKEBED_LIGHTHOUSE_RUINS_CODEC = codec(Kind.LAKEBED_LIGHTHOUSE_RUINS);
    public static final MapCodec<RegionSiteStructure> ILLUSION_WALL_CHAMBER_CODEC = codec(Kind.ILLUSION_WALL_CHAMBER);
    public static final MapCodec<RegionSiteStructure> MOSSBACK_TURTLE_NEST_CODEC = codec(Kind.MOSSBACK_TURTLE_NEST);
    public static final MapCodec<RegionSiteStructure> UNDERGROUND_GREENHOUSE_CODEC = codec(Kind.UNDERGROUND_GREENHOUSE);
    public static final MapCodec<RegionSiteStructure> BROKEN_BELL_HUT_CODEC = codec(Kind.BROKEN_BELL_HUT);
    public static final MapCodec<RegionSiteStructure> BROKEN_BELL_STELE_CODEC = codec(Kind.BROKEN_BELL_STELE);
    public static final MapCodec<RegionSiteStructure> ARCHIVE_REMNANT_HALL_CODEC = codec(Kind.ARCHIVE_REMNANT_HALL);
    public static final MapCodec<RegionSiteStructure> CLIFF_BRIDGE_CODEC = codec(Kind.CLIFF_BRIDGE);
    public static final MapCodec<RegionSiteStructure> ARCHIVE_GATE_CODEC = codec(Kind.ARCHIVE_GATE);

    private final Kind kind;

    private static MapCodec<RegionSiteStructure> codec(Kind kind) {
        return simpleCodec(settings -> new RegionSiteStructure(settings, kind));
    }

    private RegionSiteStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case MEMORY_PILLAR_COURTYARD -> ModStructures.MEMORY_PILLAR_COURTYARD.get();
            case LOST_CAMP -> ModStructures.LOST_CAMP.get();
            case OVERWORLD_LOST_CAMP -> ModStructures.OVERWORLD_LOST_CAMP.get();
            case WIND_ERODED_STEPS -> ModStructures.WIND_ERODED_STEPS.get();
            case WIND_RUNE_STONE_CIRCLE -> ModStructures.WIND_RUNE_STONE_CIRCLE.get();
            case REFLECTION_WELL -> ModStructures.REFLECTION_WELL.get();
            case MIRROR_LAKE_CACHE -> ModStructures.MIRROR_LAKE_CACHE.get();
            case TIDE_BUOY_ARRAY -> ModStructures.TIDE_BUOY_ARRAY.get();
            case REEF_HUT -> ModStructures.REEF_HUT.get();
            case LAKEBED_LIGHTHOUSE_RUINS -> ModStructures.LAKEBED_LIGHTHOUSE_RUINS.get();
            case ILLUSION_WALL_CHAMBER -> ModStructures.ILLUSION_WALL_CHAMBER.get();
            case MOSSBACK_TURTLE_NEST -> ModStructures.MOSSBACK_TURTLE_NEST.get();
            case UNDERGROUND_GREENHOUSE -> ModStructures.UNDERGROUND_GREENHOUSE.get();
            case BROKEN_BELL_HUT -> ModStructures.BROKEN_BELL_HUT.get();
            case BROKEN_BELL_STELE -> ModStructures.BROKEN_BELL_STELE.get();
            case ARCHIVE_REMNANT_HALL -> ModStructures.ARCHIVE_REMNANT_HALL.get();
            case CLIFF_BRIDGE -> ModStructures.CLIFF_BRIDGE.get();
            case ARCHIVE_GATE -> ModStructures.ARCHIVE_GATE.get();
        };
    }

    public enum Kind {
        MEMORY_PILLAR_COURTYARD("memory_pillar_courtyard", 61, 16),
        LOST_CAMP("lost_camp", 19, 8),
        OVERWORLD_LOST_CAMP("overworld_lost_camp", 19, 8),
        WIND_ERODED_STEPS("wind_eroded_steps", 25, 16),
        WIND_RUNE_STONE_CIRCLE("wind_rune_stone_circle", 21, 10),
        REFLECTION_WELL("reflection_well", 17, 10),
        MIRROR_LAKE_CACHE("mirror_lake_cache", 15, 8),
        TIDE_BUOY_ARRAY("tide_buoy_array", 21, 8),
        REEF_HUT("reef_hut", 17, 9),
        LAKEBED_LIGHTHOUSE_RUINS("lakebed_lighthouse_ruins", 23, 14),
        ILLUSION_WALL_CHAMBER("illusion_wall_chamber", 21, 9),
        MOSSBACK_TURTLE_NEST("mossback_turtle_nest", 17, 7),
        UNDERGROUND_GREENHOUSE("underground_greenhouse", 27, 12),
        BROKEN_BELL_HUT("broken_bell_hut", 17, 9),
        BROKEN_BELL_STELE("broken_bell_stele", 13, 12),
        ARCHIVE_REMNANT_HALL("archive_remnant_hall", 23, 11),
        CLIFF_BRIDGE("cliff_bridge", 31, 9),
        ARCHIVE_GATE("archive_gate", 25, 18);

        final String id;
        final int size;
        final int height;

        Kind(String id, int size, int height) {
            this.id = id;
            this.size = size;
            this.height = height;
        }

        static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return LOST_CAMP;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private Kind kind;
        private boolean spawnedMainBoss = false;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.REGION_SITE_PIECE.get(), x, 64, z,
                    kind.size, kind.height, kind.size, Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.REGION_SITE_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("RegionSiteKind"));
            this.spawnedMainBoss = tag.getBoolean("SpawnedMainBoss");
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearPlayableVolume(level, box);
            int c = kind.size / 2;
            switch (kind) {
                case MEMORY_PILLAR_COURTYARD -> memoryCourtyard(level, random, box, c);
                case LOST_CAMP -> lostCamp(level, random, box, c);
                case OVERWORLD_LOST_CAMP -> overworldLostCamp(level, random, box, c);
                case WIND_ERODED_STEPS -> windSteps(level, random, box, c);
                case WIND_RUNE_STONE_CIRCLE -> windCircle(level, random, box, c);
                case REFLECTION_WELL -> reflectionWell(level, random, box, c);
                case MIRROR_LAKE_CACHE -> mirrorCache(level, random, box, c);
                case TIDE_BUOY_ARRAY -> tideBuoys(level, random, box, c);
                case REEF_HUT -> reefHut(level, random, box, c);
                case LAKEBED_LIGHTHOUSE_RUINS -> lakebedLighthouse(level, random, box, c);
                case ILLUSION_WALL_CHAMBER -> illusionChamber(level, random, box, c);
                case MOSSBACK_TURTLE_NEST -> turtleNest(level, random, box, c);
                case UNDERGROUND_GREENHOUSE -> greenhouse(level, random, box, c);
                case BROKEN_BELL_HUT -> brokenHut(level, random, box, c);
                case BROKEN_BELL_STELE -> brokenStele(level, random, box, c);
                case ARCHIVE_REMNANT_HALL -> archiveHall(level, random, box, c);
                case CLIFF_BRIDGE -> cliffBridge(level, random, box, c);
                case ARCHIVE_GATE -> archiveGate(level, random, box, c);
            }
        }

        private void memoryCourtyard(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState moss = ModBlocks.ECHO_MOSSY_STONE.get().defaultBlockState();
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            disk(level, box, c, 0, c, 30, moss);
            buildGuidedEntrance(level, box, c, brick);
            buildMemoryOuterSteles(level, random, box, c);
            buildMemoryBossArena(level, random, box, c);
            buildMemoryRetryShortcut(level, box, c);
            buildMemoryRevisitVault(level, random, box, c);
            BlockPos bossPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(2), this.getWorldZ(c, c));
            if (!this.spawnedMainBoss && box.isInside(bossPos)) {
                var boss = ModEntities.FORGOTTEN_COLOSSUS.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossPos.getX() + 0.5D, bossPos.getY(), bossPos.getZ() + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                    this.spawnedMainBoss = true;
                }
            }
        }

        private void buildMemoryOuterSteles(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            int[][] steles = {{c - 18, c - 23}, {c, c - 25}, {c + 18, c - 23}};
            for (int i = 0; i < steles.length; i++) {
                pillar(level, box, steles[i][0], 1, steles[i][1], 2 + i, cracked);
                this.placeBlock(level, rune, steles[i][0], 3 + i, steles[i][1], box);
            }
            buildAccessibleChest(level, random, box, c - 22, 1, c - 21, ModLootTables.T2_RUIN);
        }

        private void buildMemoryBossArena(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            ring(level, box, c, 1, c, 24, brick);
            ring(level, box, c, 2, c, 13, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
            for (int d : new int[]{-14, 14}) {
                memoryPillar(level, box, c + d, c + d);
                memoryPillar(level, box, c + d, c - d);
            }
            disk(level, box, c, 1, c, 12, rune);
            pillar(level, box, c - 2, 2, c, 4, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
            pillar(level, box, c + 2, 2, c, 3, ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.REWARD_ALTAR.get().defaultBlockState(), c, 2, c - 8, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c, 2, c + 8, box);
            buildAccessibleChest(level, random, box, c - 6, 2, c - 7, ModLootTables.T4_REGION_CORE);
        }

        private void buildMemoryRetryShortcut(WorldGenLevel level, BoundingBox box, int c) {
            BlockState wind = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            for (int z = c + 25; z <= c + 30; z++) {
                for (int x = c - 2; x <= c + 2; x++) {
                    this.placeBlock(level, wind, x, 1, z, box);
                }
            }
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c, 2, c + 24, box);
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, 2, c + 28, box);
        }

        private void buildMemoryRevisitVault(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c + 18, 1, c + 16, 9, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c + 20, 2, c + 18, box);
            buildHiddenReward(level, random, box, c + 22, 2, c + 22, ModLootTables.TRUE_SIGHT_VAULT);
        }

        private void lostCamp(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c - 6, 0, c - 5, 13, Blocks.COARSE_DIRT.defaultBlockState());
            buildGuidedEntrance(level, box, c, Blocks.COARSE_DIRT.defaultBlockState());
            for (int i = -5; i <= 5; i += 5) {
                pillar(level, box, c + i, 1, c - 4, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                platform(level, box, c + i - 2, 4, c - 5, 5, Blocks.BROWN_WOOL.defaultBlockState());
            }
            this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), c, 1, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c + 4, 1, c + 3, box);
            buildBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.T2_RUIN);
            buildHiddenReward(level, random, box, c + 5, 1, c + 5, ModLootTables.TRUE_SIGHT_VAULT);
        }

        /**
         * 主世界失落营地(文档 4.2.1):先行探索者残留的营地——篝火、行囊(木桶+宝箱)、
         * 普通残页与偶见的潦草地图(由 OVERWORLD_LOST_CAMP_CHEST 战利品提供)。
         * 与回声境域的 lostCamp 不同:不放 ECHO_ARCHIVE_TERMINAL(那是境域科技),只留先行者风味。
         */
        private void overworldLostCamp(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            platform(level, box, c - 6, 0, c - 5, 13, Blocks.COARSE_DIRT.defaultBlockState());
            buildGuidedEntrance(level, box, c, Blocks.COARSE_DIRT.defaultBlockState());
            // 简易帐篷:云杉立柱 + 棕色羊毛顶
            for (int i = -5; i <= 5; i += 5) {
                pillar(level, box, c + i, 1, c - 4, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                platform(level, box, c + i - 2, 4, c - 5, 5, Blocks.BROWN_WOOL.defaultBlockState());
            }
            // 篝火 + 床铺(羊毛)+ 行囊木桶,先行者落脚痕迹
            this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), c, 1, c, box);
            this.placeBlock(level, Blocks.RED_WOOL.defaultBlockState(), c - 3, 1, c - 3, box);
            this.placeBlock(level, Blocks.WHITE_WOOL.defaultBlockState(), c - 3, 1, c - 2, box);
            this.placeBlock(level, Blocks.BARREL.defaultBlockState(), c + 3, 1, c - 3, box);
            this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), c + 4, 1, c + 2, box);
            // 行囊宝箱:普通材料 + 普通残页 + 偶见潦草地图(指向最近遗迹)
            buildBranchReward(level, random, box, c - 4, 1, c + 3, ModLootTables.OVERWORLD_LOST_CAMP_CHEST);
        }


        private void windSteps(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.SKY_OBSERVATORY_CHEST);
            for (int i = 0; i < 10; i++) {
                platform(level, box, c - 4 + i, i, c - 8 + i, 8, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
                if (i % 3 == 0) {
                    this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c, i + 1, c - 6 + i, box);
                }
            }
            buildAccessibleChest(level, random, box, c + 5, 10, c + 5, ModLootTables.SKY_OBSERVATORY_CHEST);
        }

        private void windCircle(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            disk(level, box, c, 0, c, 9, ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState());
            // 谜题布局跨区块一致:从包围盒坐标派生确定性随机(gotchas #15)
            RandomSource puzzleRandom = RandomSource.create(
                    this.boundingBox.minX() * 341873128712L ^ this.boundingBox.minZ() * 132897987541L);
            // 八根环形柱,柱顶嵌顺序符文(点数 orders[i]):右键把点数报给中心谜题核心
            int[] orders = shuffledOrders(puzzleRandom, 8);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                int px = c + (int) Math.round(Math.cos(a) * 7);
                int pz = c + (int) Math.round(Math.sin(a) * 7);
                pillar(level, box, px, 1, pz, 3, ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState());
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.ORDER, orders[i]),
                        px, 4, pz, box);
            }
            // 中心谜题核心 + 激活序列(八点数乱序取前五,落选两根为干扰柱)
            this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState(), c, 1, c, box);
            int[] sequence = java.util.Arrays.copyOf(shuffledOrders(puzzleRandom, 8), 5);
            BlockPos corePos = new BlockPos(this.getWorldX(c, c), this.getWorldY(1), this.getWorldZ(c, c));
            if (level.getBlockEntity(corePos)
                    instanceof cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity coreEntity) {
                coreEntity.setSequence(sequence);
            }
            // 北侧提示墙:常亮顺序符文 = 激活顺序(不参与判定,触错重置不灭)
            BlockState wall = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            for (int x = c - 3; x <= c + 3; x++) {
                for (int y = 1; y <= 4; y++) {
                    this.placeBlock(level, wall, x, y, c - 8, box);
                }
            }
            for (int i = 0; i < sequence.length; i++) {
                this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.ORDER, sequence[i])
                        .setValue(cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock.LIT, Boolean.TRUE),
                        c - 2 + i, 4, c - 8, box);
            }
            // 解谜奖励:封印石室,解开后由谜题核心清除半径内封印石(PuzzleCore EFFECT_RADIUS=10)
            buildSealedReward(level, random, box, c + 5, 1, c, ModLootTables.T3_PUZZLE);
        }

        /** 封印奖励:奖励箱四面 + 顶封印石(不可破坏),谜题核心解开后清除封印露出宝箱。 */
        private void buildSealedReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                       int x, int y, int z,
                                       net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();
            this.placeBlock(level, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), x, y - 1, z, box);
            this.createChest(level, box, random, x, y, z, loot); // audit-ok: puzzle core clears sealed stones before chest access
            this.placeBlock(level, sealed, x, y + 1, z, box);
            this.placeBlock(level, sealed, x + 1, y, z, box);
            this.placeBlock(level, sealed, x - 1, y, z, box);
            this.placeBlock(level, sealed, x, y, z + 1, box);
            this.placeBlock(level, sealed, x, y, z - 1, box);
        }

        /** 0..size-1 的乱序排列(Fisher-Yates);用确定性 random 保证跨区块布局一致。 */
        private static int[] shuffledOrders(RandomSource random, int size) {
            int[] arr = new int[size];
            for (int i = 0; i < size; i++) {
                arr[i] = i;
            }
            for (int i = size - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
            }
            return arr;
        }

        private void reflectionWell(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.MIRROR_LAKE_CACHE);
            disk(level, box, c, 0, c, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            for (int y = 1; y <= 5; y++) {
                ring(level, box, c, y, c, 4, y % 2 == 0 ? Blocks.GLASS.defaultBlockState() : ModBlocks.MIRROR_STONE.get().defaultBlockState());
            }
            disk(level, box, c, 1, c, 3, Blocks.WATER.defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 2, c - 5, box);
            buildAccessibleChest(level, random, box, c + 4, 1, c, ModLootTables.MIRROR_LAKE_CACHE);
        }

        private void mirrorCache(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.MIRROR_LAKE_CACHE);
            platform(level, box, c - 5, 0, c - 5, 11, ModBlocks.MIRROR_SAND.get().defaultBlockState());
            platform(level, box, c - 3, 1, c - 3, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), c, 2, c, box);
            configureTrueSightVault(level, box, random, c, 2, c);
            buildAccessibleChest(level, random, box, c - 3, 2, c + 3, ModLootTables.MIRROR_LAKE_CACHE);
        }

        private void tideBuoys(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
            disk(level, box, c, 0, c, 9, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0;
                this.placeBlock(level, ModBlocks.TIDE_BUOY.get().defaultBlockState(),
                        c + (int) Math.round(Math.cos(a) * 6), 1, c + (int) Math.round(Math.sin(a) * 6), box);
            }
            buildAccessibleChest(level, random, box, c, 1, c, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
        }

        private void reefHut(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            disk(level, box, c, 0, c, 7, ModBlocks.PEARL_CORAL_BLOCK.get().defaultBlockState());
            hut(level, box, c, ModBlocks.TIDEWOOD_PLANKS.get().defaultBlockState(), ModBlocks.TIDEWOOD_LOG.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c + 3, 1, c, box);
            buildAccessibleChest(level, random, box, c - 3, 1, c + 2, ModLootTables.T4_REGION_CORE);
        }

        private void lakebedLighthouse(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
            disk(level, box, c, 0, c, 10, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            for (int y = 1; y <= 10; y++) {
                ring(level, box, c, y, c, Math.max(3, 6 - y / 3), y % 3 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            }
            this.placeBlock(level, ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c + 5, 1, c, ModLootTables.TIDE_LIGHTHOUSE_REEF_CHEST);
        }

        private void illusionChamber(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            platform(level, box, c - 8, 0, c - 8, 17, ModBlocks.CRACKED_MIRROR_BRICKS.get().defaultBlockState());
            wall(level, box, c - 8, 1, c - 8, 17, 7, ModBlocks.MIRROR_STONE_BRICKS.get().defaultBlockState());
            for (int x = c - 5; x <= c + 5; x += 5) {
                this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), x, 1, c, box);
                this.placeBlock(level, ModBlocks.MIRROR_SIGIL.get().defaultBlockState(), x, 1, c + 4, box);
            }
            buildHiddenReward(level, random, box, c, 1, c - 5, ModLootTables.TRUE_SIGHT_VAULT);
        }

        private void turtleNest(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.SILENT_HUT_CHEST);
            disk(level, box, c, 0, c, 7, ModBlocks.MUFFLE_MOSS.get().defaultBlockState());
            for (int i = 0; i < 5; i++) {
                disk(level, box, c - 4 + i * 2, 1, c + random.nextInt(7) - 3, 2, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            }
            buildAccessibleChest(level, random, box, c + 3, 1, c - 3, ModLootTables.SILENT_HUT_CHEST);
        }

        private void greenhouse(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            platform(level, box, c - 10, 0, c - 10, 21, Blocks.MOSS_BLOCK.defaultBlockState());
            wall(level, box, c - 10, 1, c - 10, 21, 6, Blocks.GLASS.defaultBlockState());
            for (int x = c - 7; x <= c + 7; x += 2) {
                this.placeBlock(level, ModBlocks.DREAM_FLOWER.get().defaultBlockState(), x, 1, c - 3, box);
                this.placeBlock(level, ModBlocks.DREAM_MIST_VINE.get().defaultBlockState(), x, 1, c + 3, box);
            }
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 1, c + 6, ModLootTables.T4_REGION_CORE);
        }

        private void brokenHut(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            platform(level, box, c - 6, 0, c - 6, 13, Blocks.MUD_BRICKS.defaultBlockState());
            hut(level, box, c, Blocks.COPPER_BLOCK.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState());
            this.placeBlock(level, Blocks.COARSE_DIRT.defaultBlockState(), c + 4, 0, c - 3, box);
            this.placeBlock(level, ModBlocks.BROKEN_BELL_THORN.get().defaultBlockState(), c + 4, 1, c - 3, box);
            buildAccessibleChest(level, random, box, c - 3, 1, c + 2, ModLootTables.BROKEN_ARCHIVE_CHEST);
        }

        private void brokenStele(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.BROKEN_ARCHIVE_CHEST);
            disk(level, box, c, 0, c, 5, ModBlocks.BROKEN_BELL_ORE.get().defaultBlockState());
            pillar(level, box, c, 1, c, 9, Blocks.COPPER_BLOCK.defaultBlockState());
            this.placeBlock(level, Blocks.GOLD_BLOCK.defaultBlockState(), c, 10, c, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c - 4, box);
        }

        private void archiveHall(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T5_MINIBOSS_REPEAT);
            platform(level, box, c - 8, 0, c - 8, 17, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState());
            for (int x = c - 7; x <= c + 7; x += 3) {
                pillar(level, box, x, 1, c - 6, 6, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c + 5, 1, c + 3, ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void cliffBridge(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            for (int z = c - 14; z <= c + 14; z++) {
                for (int x = c - 2; x <= c + 2; x++) {
                    this.placeBlock(level, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState(), x, 2, z, box);
                }
                if ((z - c) % 5 == 0) {
                    pillar(level, box, c - 3, 0, z, 5, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                    pillar(level, box, c + 3, 0, z, 5, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                }
            }
            buildAccessibleChest(level, random, box, c, 3, c + 10, ModLootTables.T4_REGION_CORE);
        }

        private void archiveGate(WorldGenLevel level, RandomSource random, BoundingBox box, int c) {
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            platform(level, box, c - 10, 0, c - 4, 21, ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState());
            for (int y = 1; y <= 15; y++) {
                pillar(level, box, c - 8, y, c, 1, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                pillar(level, box, c + 8, y, c, 1, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
                if (y > 11) {
                    for (int x = c - 7; x <= c + 7; x++) {
                        this.placeBlock(level, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState(), x, y, c, box);
                    }
                }
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c - 2, box);
            buildAccessibleChest(level, random, box, c, 1, c + 3, ModLootTables.T4_REGION_CORE);
        }

        private void clearPlayableVolume(WorldGenLevel level, BoundingBox box) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int y = 1; y < kind.height; y++) {
                for (int x = 1; x < kind.size - 1; x++) {
                    for (int z = 1; z < kind.size - 1; z++) {
                        this.placeBlock(level, air, x, y, z, box);
                    }
                }
            }
        }

        private void memoryPillar(WorldGenLevel level, BoundingBox box, int x, int z) {
            pillar(level, box, x, 1, z, 3, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.MEMORY_PILLAR.get().defaultBlockState(), x, 4, z, box);
        }

        private void buildGuidedEntrance(WorldGenLevel level, BoundingBox box, int c, BlockState state) {
            for (int z = c + 4; z <= kind.size - 2; z++) {
                for (int x = c - 1; x <= c + 1; x++) {
                    this.placeBlock(level, state, x, 1, z, box);
                }
                if ((z - c) % 4 == 0) {
                    this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c - 2, 1, z, box);
                    this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c + 2, 1, z, box);
                }
            }
        }

        private void buildBranchReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                       int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            BlockState floor = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState accent = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            int floorY = Math.max(0, y - 1);
            platform(level, box, x - 2, floorY, z - 2, 5, floor);
            clearChestPocket(level, box, x, y, z);
            pillar(level, box, x - 2, y, z - 2, 2, accent);
            pillar(level, box, x + 2, y, z - 2, 2, accent);
            pillar(level, box, x - 2, y, z + 2, 2, accent);
            pillar(level, box, x + 2, y, z + 2, 2, accent);
            this.placeBlock(level, accent, x, y, z - 1, box);
            buildAccessibleChest(level, random, box, x, y, z, loot);
        }

        private void buildHiddenReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                       int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            buildTrueSightVaultNiche(level, random, box, x, y, z, vaultEntrance(x, z));
        }

        private Direction vaultEntrance(int x, int z) {
            if (x >= kind.size - 5) {
                return Direction.WEST;
            }
            if (x <= 4) {
                return Direction.EAST;
            }
            if (z >= kind.size - 5) {
                return Direction.NORTH;
            }
            return Direction.SOUTH;
        }

        private void configureTrueSightVault(WorldGenLevel level, BoundingBox box, RandomSource random,
                                             int x, int y, int z) {
            BlockPos vaultPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
            if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                    instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
                vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
            }
        }

        private void addSiteExplorationLoop(WorldGenLevel level, RandomSource random, BoundingBox box,
                                            int c, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
            buildGuidedEntrance(level, box, c, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
            buildBranchReward(level, random, box, Math.max(2, c - 4), 1, Math.min(kind.size - 3, c + 3), loot);
            buildHiddenReward(level, random, box, Math.min(kind.size - 3, c + 4), 1,
                    Math.max(2, c - 4), ModLootTables.TRUE_SIGHT_VAULT);
        }

        private void clearChestPocket(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    this.placeBlock(level, air, x + dx, y, z + dz, box);
                }
            }
            for (int dy = 1; dy <= 2; dy++) {
                this.placeBlock(level, air, x, y + dy, z, box);
            }
        }

        private void buildHiddenVaultNiche(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int minX = x - 2;
            int maxX = x + 2;
            int minZ = z - 2;
            int maxZ = z + 2;
            int roofY = y + 3;
            sealHiddenNiche(level, box, anchored, minX, maxX, minZ, maxZ, y - 1, roofY);
            this.generateBox(level, box, x - 1, y, z - 1, x + 1, y + 2, z + 1, air, air, false);
            boolean openWest = x >= kind.size - 5;
            boolean openEast = x <= 4;
            boolean openNorth = z >= kind.size - 5;
            boolean openSouth = !openWest && !openEast && !openNorth;
            for (int yy = y; yy <= y + 2; yy++) {
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    this.placeBlock(level, openNorth ? hidden : anchored, xx, yy, minZ, box);
                    this.placeBlock(level, openSouth ? hidden : anchored, xx, yy, maxZ, box);
                }
                for (int zz = z - 1; zz <= z + 1; zz++) {
                    this.placeBlock(level, openWest ? hidden : anchored, minX, yy, zz, box);
                    this.placeBlock(level, openEast ? hidden : anchored, maxX, yy, zz, box);
                }
            }
            clearChestPocket(level, box, x, y, z);
        }

        private void sealHiddenNiche(WorldGenLevel level, BoundingBox box, BlockState anchored,
                                     int minX, int maxX, int minZ, int maxZ, int floorY, int roofY) {
            for (int xx = minX; xx <= maxX; xx++) {
                for (int zz = minZ; zz <= maxZ; zz++) {
                    this.placeBlock(level, anchored, xx, floorY, zz, box);
                    this.placeBlock(level, anchored, xx, roofY, zz, box);
                    for (int yy = floorY - 2; yy < floorY; yy++) {
                        this.placeBlock(level, anchored, xx, yy, zz, box);
                    }
                }
            }
        }

        private void hut(WorldGenLevel level, BoundingBox box, int c, BlockState wall, BlockState post) {
            platform(level, box, c - 4, 1, c - 4, 9, wall);
            for (int x = c - 4; x <= c + 4; x += 8) {
                for (int z = c - 4; z <= c + 4; z += 8) {
                    pillar(level, box, x, 2, z, 4, post);
                }
            }
            platform(level, box, c - 5, 5, c - 5, 11, Blocks.SPRUCE_PLANKS.defaultBlockState());
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, 2, c - 4, box);
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, 3, c - 4, box);
        }

        private void wall(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int size, int height, BlockState state) {
            for (int y = y0; y < y0 + height; y++) {
                ring(level, box, x0 + size / 2, y, z0 + size / 2, size / 2, state);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0, int size, BlockState state) {
            for (int x = x0; x < x0 + size; x++) {
                for (int z = z0; z < z0 + size; z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z, int height, BlockState state) {
            for (int i = 0; i < height; i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            int r2 = radius * radius;
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    if (dx * dx + dz * dz <= r2) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        private void ring(WorldGenLevel level, BoundingBox box, int cx, int y, int cz, int radius, BlockState state) {
            int outer = radius * radius;
            int inner = Math.max(0, radius - 1) * Math.max(0, radius - 1);
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                    if (d2 <= outer && d2 >= inner) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("RegionSiteKind", this.kind.id);
            tag.putBoolean("SpawnedMainBoss", this.spawnedMainBoss);
        }
    }
}
