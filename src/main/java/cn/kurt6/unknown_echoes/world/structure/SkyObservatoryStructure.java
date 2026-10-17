package cn.kurt6.unknown_echoes.world.structure;

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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 天空观测站:漂浮群岛主结构(漂浮空岛,固定高度生成,不贴地)。 */
public class SkyObservatoryStructure extends TerrainAnchoredStructure {
    public static final MapCodec<SkyObservatoryStructure> CODEC = simpleCodec(SkyObservatoryStructure::new);

    private static final int SIZE = 41;
    private static final int HEIGHT = 32;

    public SkyObservatoryStructure(StructureSettings settings) {
        super(Piece::new, SIZE, SIZE, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SKY_OBSERVATORY.get();
    }

    public static class Piece extends EchoStructurePiece {

        public Piece(RandomSource random, int x, int z) {

            super(ModStructures.SKY_OBSERVATORY_PIECE.get(), x, 175 + random.nextInt(16), z,
                    SIZE, HEIGHT, SIZE, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SKY_OBSERVATORY_PIECE.get(), tag);
        }

        @Override
        protected boolean usesSurfaceAnchor() {
            return false;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            clearStructureSpace(level, box, 7);
            BlockState windStone = ModBlocks.WIND_ETCHED_STONE.get().defaultBlockState();
            BlockState stone = Blocks.STONE.defaultBlockState();
            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState grass = ModBlocks.GLOW_GRASS.get().defaultBlockState();
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            BlockState chiseled = ModBlocks.WIND_CHISELED_STONE.get().defaultBlockState();
            BlockState lamp = ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState();
            int c = SIZE / 2;

            int[] radii = {3, 5, 8, 11, 14, 16, 18, 19};
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
            int top = radii.length;

            for (int i = 0; i < 28; i++) {
                int x = c - 15 + random.nextInt(31);
                int z = c - 15 + random.nextInt(31);
                int dx = x - c;
                int dz = z - c;
                if (dx * dx + dz * dz <= 256
                        && this.getBlock(level, x, top - 1, z, box).is(Blocks.MOSS_BLOCK)) {
                    this.placeBlock(level, grass, x, top, z, box);
                }
            }

            buildSteppedDisk(level, box, c, top - 1, c, 13, 3, windStone, bricks);
            // 悬挑锚点必须高于净空上界 top+6(地板 top+1 加 miniboss 所需 5 格),否则观测层铺进走位区。
            // 朝北是为了避开风门与四向气流柱。
            buildCantileverTerraces(level, box, c, top + 10, c,
                    Direction.NORTH, 3, 7, 4,
                    windStone, bricks, chiseled, lamp);
            buildBossArenaMandala(level, box, c, top + 1, c, 11,
                    chiseled, windStone, lamp);
            buildArenaLightRing(level, box, c, top + 2, c, 12, 12, 5, bricks, lamp);
            architecturalRing(level, box, c, top + 2, c, 11, chiseled);
            buildCircularColonnade(level, box, c, top + 2, c, 10, 16, 10,
                    bricks, lamp, chiseled);
            // 收边必须用风蚀岩砖族:借用青色回响石砖会与浅灰风蚀岩撞色。
            BlockState windStairs = ModBlocks.WIND_ETCHED_BRICK_STAIRS.get().defaultBlockState();
            BlockState windSlab = ModBlocks.WIND_ETCHED_BRICK_SLAB.get().defaultBlockState();
            buildStairCornice(level, box, c, top, c, Footprint.square(12), windStairs, windSlab, false);
            buildParapet(level, box, c, top + 2, c, Footprint.square(13), 5, ModBlocks.WIND_ETCHED_BRICK_WALL.get().defaultBlockState(), chiseled);

            buildCircularCornice(level, box, c, top + 12, c, 11, windStairs, windSlab, true);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildCardinalGateway(level, box, c, top + 2, c, 11, direction,
                        3, 8, windStone, chiseled);
            }

            buildConnectedDoor(level, box, ModBlocks.WIND_DOOR.get().defaultBlockState(),
                    c, top + 2, c - 10, Direction.NORTH, 1, 2);

            architecturalDisk(level, box, c, top + 2, c, 4, chiseled);
            this.placeBlock(level, bricks, c, top + 3, c, box);
            this.placeBlock(level, ModBlocks.OBSERVATORY_CORE.get().defaultBlockState(), c, top + 4, c, box);
            buildAccessibleChest(level, random, box, c + 6, top + 3, c + 3,
                    ModLootTables.SKY_OBSERVATORY_CHEST);
            buildTrueSightVaultNiche(level, random, box, c + 10, top + 3, c - 6, Direction.WEST);

            this.placeBlock(level, chiseled, c - 3, top + 2, c + 3, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_RECORD_TABLE.get().defaultBlockState(),
                    c - 3, top + 3, c + 3, box);
            this.placeBlock(level, chiseled, c - 3, top + 2, c - 3, box);
            this.placeBlock(level, ModBlocks.ARTIFACT_TUNING_TABLE.get().defaultBlockState(),
                    c - 3, top + 3, c - 3, box);

            for (int[] corner : new int[][]{{c - 14, c - 14}, {c + 14, c - 14},
                    {c - 14, c + 14}, {c + 14, c + 14}}) {
                architecturalDisk(level, box, corner[0], top + 1, corner[1], 4, windStone);
                architecturalPillar(level, box, corner[0], top - 3, corner[1], 10, chiseled);
                this.placeBlock(level, lamp, corner[0], top + 7, corner[1], box);
            }

            BlockState platform = ModBlocks.WIND_CURRENT_PLATFORM.get().defaultBlockState();
            for (int[] spot : new int[][]{{c - 15, c}, {c + 15, c}, {c, c - 15}, {c, c + 15}}) {
                architecturalDisk(level, box, spot[0], top + 1, spot[1], 3, windStone);
                this.placeBlock(level, platform, spot[0], top + 2, spot[1], box);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                buildRuneCauseway(level, box, c, top + 1, c, direction,
                        10, 16, 1, bricks, windStone, chiseled);
            }
            architecturalRing(level, box, c, top + 20, c, 14, lamp);

            // 星轨冠：观测站顶部保留一根轴心与三段偏心轨道，远景不会读成普通圆台。
            for (int y = top + 16; y <= top + 22; y++) {
                this.placeBlock(level, y % 2 == 0 ? chiseled : windStone, c, y, c, box);
            }
            for (int[] arm : new int[][]{{-8, 0}, {8, 0}, {0, 8}}) {
                for (int step = 0; step <= 4; step++) {
                    int x = c + (arm[0] == 0 ? 0 : Integer.signum(arm[0]) * step * 2);
                    int z = c + (arm[1] == 0 ? 0 : Integer.signum(arm[1]) * step * 2);
                    this.placeBlock(level, step % 2 == 0 ? windStone : chiseled,
                            x, top + 19, z, box);
                }
            }
            this.placeBlock(level, lamp, c, top + 23, c, box);

            placeStructureSpawner(level, random, box, c + 13, top + 2, c - 5,
                    ModEntities.WIND_ERODED_SENTINEL.get(), windStone, chiseled);

            this.placeBlock(level, ModBlocks.MINIBOSS_SPAWNER.get().defaultBlockState(), c, 4, c, box);
            BlockPos spawnerPos = new BlockPos(this.getWorldX(c, c), this.getWorldY(4), this.getWorldZ(c, c));
            if (box.isInside(spawnerPos) && level.getBlockEntity(spawnerPos)
                    instanceof cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity spawner) {

                spawner.configure(cn.kurt6.unknown_echoes.UnknownEchoes.id("storm_weaver"), top + 13);
            }
            dressStructure(level, box, "sky_observatory");
            restorePlantBed(level, box, c, top - 1, c, 15, moss,
                    state -> state.is(ModBlocks.GLOW_GRASS.get()));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
        }
    }
}
