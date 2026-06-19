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
import net.minecraft.resources.ResourceLocation;
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
 * 后期区域同名结构发行候选版:补齐文档中的晶歌/沉眠/档案馆入口结构。
 * 这些结构只提供可定位场地、区域材料与普通奖励;关键进度仍由服务端能力/Boss 数据结算。
 */
public class LateRegionSiteStructure extends SinglePieceStructure {
    public static final MapCodec<LateRegionSiteStructure> CRYSTAL_SONG_SHRINE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.CRYSTAL_SONG_SHRINE));
    public static final MapCodec<LateRegionSiteStructure> CRYSTAL_SONG_HALL_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.CRYSTAL_SONG_HALL));
    public static final MapCodec<LateRegionSiteStructure> DREAM_FLOWER_HOUSE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.DREAM_FLOWER_HOUSE));
    public static final MapCodec<LateRegionSiteStructure> ECHO_GRAND_ARCHIVE_CODEC =
            simpleCodec(settings -> new LateRegionSiteStructure(settings, Kind.ECHO_GRAND_ARCHIVE));

    private final Kind kind;

    private LateRegionSiteStructure(StructureSettings settings, Kind kind) {
        super((random, x, z) -> new Piece(kind, random, x, z), kind.size, kind.size, settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case CRYSTAL_SONG_SHRINE -> ModStructures.CRYSTAL_SONG_SHRINE.get();
            case CRYSTAL_SONG_HALL -> ModStructures.CRYSTAL_SONG_HALL.get();
            case DREAM_FLOWER_HOUSE -> ModStructures.DREAM_FLOWER_HOUSE.get();
            case ECHO_GRAND_ARCHIVE -> ModStructures.ECHO_GRAND_ARCHIVE.get();
        };
    }

    public enum Kind {
        CRYSTAL_SONG_SHRINE("crystal_song_shrine", 13, 10),
        CRYSTAL_SONG_HALL("crystal_song_hall", 35, 18),
        DREAM_FLOWER_HOUSE("dream_flower_house", 15, 9),
        ECHO_GRAND_ARCHIVE("echo_grand_archive", 81, 32);

        private final String id;
        private final int size;
        private final int height;

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
            return CRYSTAL_SONG_SHRINE;
        }
    }

    public static class Piece extends EchoStructurePiece {
        private Kind kind;

        public Piece(Kind kind, RandomSource random, int x, int z) {
            super(ModStructures.LATE_REGION_SITE_PIECE.get(), x, 64, z,
                    kind.size, kind.height, kind.size,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
            this.kind = kind;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.LATE_REGION_SITE_PIECE.get(), tag);
            this.kind = Kind.byId(tag.getString("LateRegionKind"));
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }
            clearPlayableVolume(level, box);
            switch (this.kind) {
                case CRYSTAL_SONG_SHRINE -> buildCrystalShrine(level, random, box);
                case CRYSTAL_SONG_HALL -> buildCrystalHall(level, random, box);
                case DREAM_FLOWER_HOUSE -> buildDreamFlowerHouse(level, random, box);
                case ECHO_GRAND_ARCHIVE -> buildGrandArchive(level, random, box);
            }
        }

        private void buildCrystalShrine(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState floor = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState crystal = ModBlocks.CRYSTAL_SONG_CLUSTER.get().defaultBlockState();
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T3_PUZZLE);
            disk(level, box, c, 0, c, 5, floor);
            for (int i = 0; i < 3; i++) {
                int x = c + (i - 1) * 3;
                pillar(level, box, x, 1, c + 2, 3 + i, Blocks.AMETHYST_BLOCK.defaultBlockState());
                this.placeBlock(level, crystal, x, 4 + i, c + 2, box);
            }
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), c - 3, 0, c - 2, box);
            this.placeBlock(level, Blocks.MOSS_BLOCK.defaultBlockState(), c + 3, 0, c - 1, box);
            this.placeBlock(level, ModBlocks.RESONANT_MUSHROOM.get().defaultBlockState(), c - 3, 1, c - 2, box);
            this.placeBlock(level, ModBlocks.RESONANT_MUSHROOM.get().defaultBlockState(), c + 3, 1, c - 1, box);
            this.placeBlock(level, ModBlocks.RESONANCE_CANDLE.get().defaultBlockState(), c, 1, c, box);
            buildAccessibleChest(level, random, box, c, 1, c - 4, ModLootTables.T3_PUZZLE);
        }

        private void buildCrystalHall(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState brick = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState crystal = Blocks.AMETHYST_BLOCK.defaultBlockState();
            List<BlockPos> crystalPositions = new ArrayList<>();
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T4_REGION_CORE);
            platform(level, box, c - 12, 0, c - 12, 25, ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState());
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                int x = c + (int) Math.round(Math.cos(angle) * 10);
                int z = c + (int) Math.round(Math.sin(angle) * 10);
                pillar(level, box, x, 1, z, 6, i % 2 == 0 ? brick : crystal);
                this.placeBlock(level, ModBlocks.CRYSTAL_SONG_CLUSTER.get().defaultBlockState(), x, 7, z, box);
                crystalPositions.add(new BlockPos(this.getWorldX(x, z), this.getWorldY(7), this.getWorldZ(x, z)));
            }
            platform(level, box, c - 4, 1, c - 4, 9, Blocks.CALCITE.defaultBlockState());
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 0, c, box);
            configureSpawner(level, box, c, 0, c, "crystal_songkeeper", 3,
                    UnknownEchoes.id("crystal_song_cluster"), crystalPositions);
            buildAccessibleChest(level, random, box, c - 6, 1, c, ModLootTables.T4_REGION_CORE);
            buildAccessibleChest(level, random, box, c + 6, 1, c, ModLootTables.T5_MINIBOSS_REPEAT);
        }

        private void buildDreamFlowerHouse(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState plank = Blocks.BIRCH_PLANKS.defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            addSiteExplorationLoop(level, random, box, c, ModLootTables.T2_RUIN);
            platform(level, box, 1, 0, 1, kind.size - 2, moss);
            for (int x = 3; x <= 11; x++) {
                for (int z = 3; z <= 11; z++) {
                    boolean wall = x == 3 || x == 11 || z == 3 || z == 11;
                    if (wall) {
                        pillar(level, box, x, 1, z, 4, plank);
                    }
                }
            }
            platform(level, box, 2, 5, 2, 11, Blocks.PINK_TERRACOTTA.defaultBlockState());
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, 1, 3, box);
            this.placeBlock(level, Blocks.AIR.defaultBlockState(), c, 2, 3, box);
            this.placeBlock(level, ModBlocks.DREAM_FLOWER.get().defaultBlockState(), c - 4, 1, c - 5, box);
            this.placeBlock(level, ModBlocks.DREAM_FLOWER.get().defaultBlockState(), c + 4, 1, c - 4, box);
            this.placeBlock(level, ModBlocks.DREAM_MIST_VINE.get().defaultBlockState(), c - 3, 1, c + 4, box);
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 1, c + 3, box);
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 1, c, box);
            configureSpawner(level, box, c, 1, c, "dream_bloom_keeper", 2, null, null);
            buildAccessibleChest(level, random, box, c - 3, 1, c + 2, ModLootTables.T2_RUIN);
        }

        private void buildGrandArchive(WorldGenLevel level, RandomSource random, BoundingBox box) {
            int c = kind.size / 2;
            BlockState cliff = ModBlocks.ECHO_CLIFF_STONE.get().defaultBlockState();
            BlockState brick = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            buildGuidedEntrance(level, box, c, cliff);
            buildArchiveCliffGate(level, box, c, cliff, brick);
            buildArchiveMainHall(level, box, c, cliff, brick);
            buildArchiveAbilityWings(level, random, box, c, cliff, brick);
            buildArchiveTerminalHall(level, random, box, c, cliff, brick);
        }

        private void buildArchiveCliffGate(WorldGenLevel level, BoundingBox box, int c,
                                           BlockState cliff, BlockState brick) {
            for (int y = 1; y <= 31; y++) {
                pillar(level, box, c - 24, y, c - 34, 1, cliff);
                pillar(level, box, c + 24, y, c - 34, 1, cliff);
                if (y >= 24) {
                    for (int x = c - 23; x <= c + 23; x++) {
                        this.placeBlock(level, y % 4 == 0 ? brick : cliff, x, y, c - 34, box);
                    }
                }
            }
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c - 5, 2, c - 34, box);
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c, 2, c - 34, box);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c + 5, 2, c - 34, box);
        }

        private void buildArchiveMainHall(WorldGenLevel level, BoundingBox box, int c,
                                          BlockState cliff, BlockState brick) {
            platform(level, box, c - 32, 0, c - 32, 65, cliff);
            for (int y = 1; y <= 18; y++) {
                hollowRect(level, box, c - 32, y, c - 32, 65, 65, y % 6 == 0 ? brick : cliff);
            }
            for (int i = -24; i <= 24; i += 6) {
                pillar(level, box, c + i, 1, c - 26, 9, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
                pillar(level, box, c + i, 1, c + 26, 9, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
        }

        private void buildArchiveAbilityWings(WorldGenLevel level, RandomSource random, BoundingBox box, int c,
                                              BlockState cliff, BlockState brick) {
            platform(level, box, c - 33, 1, c - 8, 25, cliff);
            this.placeBlock(level, ModBlocks.WIND_DOOR.get().defaultBlockState(), c - 29, 2, c, box);
            this.placeBlock(level, ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState(), c - 32, 2, c, box);
            this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(), c - 20, 2, c - 5, box);
            buildBranchReward(level, random, box, c - 20, 2, c + 6, ModLootTables.T3_PUZZLE);

            platform(level, box, c + 9, 1, c - 8, 25, ModBlocks.TIDE_SMOOTH_STONE.get().defaultBlockState());
            this.placeBlock(level, ModBlocks.TIDE_RUNE_SEAT.get().defaultBlockState(), c + 15, 2, c, box);
            for (int z = c - 6; z <= c + 6; z += 6) {
                this.placeBlock(level, ModBlocks.TIDE_PILLAR.get().defaultBlockState(), c + 24, 2, z, box);
            }
            this.placeBlock(level, ModBlocks.TIDE_CORE_ALTAR.get().defaultBlockState(), c + 18, 2, c, box);
            buildBranchReward(level, random, box, c + 24, 2, c + 8, ModLootTables.T4_REGION_CORE);

            platform(level, box, c - 12, 1, c + 10, 25, brick);
            this.placeBlock(level, ModBlocks.TRUE_SIGHT_STELE.get().defaultBlockState(), c, 2, c + 16, box);
            this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), c - 5, 2, c + 28, box);
            this.placeBlock(level, ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState(), c + 5, 2, c + 28, box);
            buildHiddenReward(level, random, box, c, 2, c + 30, ModLootTables.TRUE_SIGHT_VAULT);
        }

        private void buildArchiveTerminalHall(WorldGenLevel level, RandomSource random, BoundingBox box, int c,
                                              BlockState cliff, BlockState brick) {
            platform(level, box, c - 12, 1, c - 12, 25, brick);
            for (int y = 2; y <= 10; y++) {
                hollowRect(level, box, c - 12, y, c - 12, 25, 25, y % 3 == 0 ? brick : cliff);
            }
            this.placeBlock(level, ModBlocks.ECHO_ARCHIVE_TERMINAL.get().defaultBlockState(), c, 2, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(), c + 4, 2, c, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(), c - 4, 2, c, box);
            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 2, c + 5, box);
            configureSpawner(level, box, c, 2, c + 5, "lost_recorder_chief", 2, null, null);
            buildBranchReward(level, random, box, c - 7, 2, c - 7, ModLootTables.T5_MINIBOSS_REPEAT);
            buildAccessibleChest(level, random, box, c + 7, 2, c + 7, ModLootTables.T5_MINIBOSS_REPEAT);
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

        private void buildGuidedEntrance(WorldGenLevel level, BoundingBox box, int c, BlockState state) {
            int start = Math.max(1, c - 39);
            int end = Math.min(kind.size - 2, c - 33);
            if (start > end) {
                start = Math.min(kind.size - 2, c + 4);
                end = kind.size - 2;
            }
            for (int z = start; z <= end; z++) {
                for (int x = c - 3; x <= c + 3; x++) {
                    this.placeBlock(level, state, x, 1, z, box);
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

        private void configureSpawner(WorldGenLevel level, BoundingBox box, int x, int y, int z, String entityId,
                                      int spawnYOffset, ResourceLocation resetBlockId,
                                      List<BlockPos> resetPositions) {
            BlockPos spawnerPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
            if (box.isInside(spawnerPos)
                    && level.getBlockEntity(spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
                spawner.configure(UnknownEchoes.id(entityId), spawnYOffset);
                if (resetBlockId != null && resetPositions != null && !resetPositions.isEmpty()) {
                    spawner.setResetBlocks(resetBlockId, resetPositions);
                }
            }
        }

        private void pillar(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                            int height, BlockState state) {
            for (int i = 0; i < height; i++) {
                this.placeBlock(level, state, x, y + i, z, box);
            }
        }

        private void platform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0,
                              int size, BlockState state) {
            for (int x = x0; x < x0 + size; x++) {
                for (int z = z0; z < z0 + size; z++) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }

        private void disk(WorldGenLevel level, BoundingBox box, int cx, int y, int cz,
                          int radius, BlockState state) {
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

        private void hollowRect(WorldGenLevel level, BoundingBox box, int x0, int y, int z0,
                                int width, int depth, BlockState state) {
            for (int x = x0; x < x0 + width; x++) {
                for (int z = z0; z < z0 + depth; z++) {
                    boolean wall = x == x0 || z == z0 || x == x0 + width - 1 || z == z0 + depth - 1;
                    if (wall) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("LateRegionKind", this.kind.id);
        }
    }
}
