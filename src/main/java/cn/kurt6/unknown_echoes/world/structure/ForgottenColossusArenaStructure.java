package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
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
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 遗忘巨像场地(回声境域):
 * 中央 Boss 区域 + 四角记忆柱 + 奖励祭坛 + 出口风门小室(风门后有宝箱,验证权限闭环)。
 * Boss 在结构生成时直接放入场地,不会自然消失。
 */
public class ForgottenColossusArenaStructure extends SinglePieceStructure {
    public static final MapCodec<ForgottenColossusArenaStructure> CODEC =
            simpleCodec(ForgottenColossusArenaStructure::new);

    private static final int WIDTH = 29;
    private static final int DEPTH = 29;
    private static final int HEIGHT = 12;

    public ForgottenColossusArenaStructure(StructureSettings settings) {
        super(Piece::new, WIDTH, DEPTH, settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FORGOTTEN_COLOSSUS_ARENA.get();
    }

    public static class Piece extends EchoStructurePiece {
        private boolean spawnedBoss = false;

        public Piece(RandomSource random, int x, int z) {
            super(ModStructures.FORGOTTEN_COLOSSUS_ARENA_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                    Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.FORGOTTEN_COLOSSUS_ARENA_PIECE.get(), tag);
            this.spawnedBoss = tag.getBoolean("SpawnedBoss");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("SpawnedBoss", this.spawnedBoss);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
            if (!this.updateAverageGroundHeight(level, box, 0)) {
                return;
            }

            BlockState bricks = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState cracked = ModBlocks.CRACKED_ECHO_STONE_BRICKS.get().defaultBlockState();
            BlockState rune = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
            BlockState pillar = ModBlocks.MEMORY_PILLAR.get().defaultBlockState();
            BlockState altar = ModBlocks.REWARD_ALTAR.get().defaultBlockState();
            BlockState windDoor = ModBlocks.WIND_DOOR.get().defaultBlockState();
            BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            // 场地地板
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? cracked : bricks, x, 0, z, box);
                }
            }
            // 清出场地空间
            this.generateBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1, air, air, false);

            // 外圈围墙(3 高,防止战斗中被击退出场)+ 南墙中央 3 宽门洞作为主入口
            for (int y = 1; y <= 3; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    this.placeBlock(level, bricks, x, y, 0, box);
                    this.placeBlock(level, bricks, x, y, DEPTH - 1, box);
                }
                for (int z = 0; z < DEPTH; z++) {
                    this.placeBlock(level, bricks, 0, y, z, box);
                    this.placeBlock(level, bricks, WIDTH - 1, y, z, box);
                }
            }
            for (int x = WIDTH / 2 - 1; x <= WIDTH / 2 + 1; x++) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, air, x, y, DEPTH - 1, box);
                }
            }
            // 四角符文柱(高出围墙一格,远处可辨认场地)
            for (int[] corner : new int[][]{{0, 0}, {WIDTH - 1, 0}, {0, DEPTH - 1}, {WIDTH - 1, DEPTH - 1}}) {
                this.placeBlock(level, bricks, corner[0], 4, corner[1], box);
                this.placeBlock(level, rune, corner[0], 5, corner[1], box);
            }

            // 四角记忆柱:两层石砖底座 + 记忆柱
            placeMemoryPillar(level, box, 6, 6, bricks, pillar);
            placeMemoryPillar(level, box, 6, DEPTH - 7, bricks, pillar);
            placeMemoryPillar(level, box, WIDTH - 7, 6, bricks, pillar);
            placeMemoryPillar(level, box, WIDTH - 7, DEPTH - 7, bricks, pillar);

            // 奖励祭坛(场地南侧)
            this.placeBlock(level, bricks, WIDTH / 2, 1, DEPTH - 5, box);
            this.placeBlock(level, altar, WIDTH / 2, 2, DEPTH - 5, box);

            // 出口风门小室(场地北侧):风门后的宝箱用于验证权限——没有风之回响就拿不到
            buildWindVault(level, box, random, anchored, rune, windDoor, air);

            // Boss:遗忘巨像
            int bossX = this.getWorldX(WIDTH / 2, DEPTH / 2);
            int bossZ = this.getWorldZ(WIDTH / 2, DEPTH / 2);
            int bossY = this.getWorldY(1);
            BlockPos bossPos = new BlockPos(bossX, bossY, bossZ);
            if (!this.spawnedBoss && box.isInside(bossPos)) {
                ForgottenColossus boss = ModEntities.FORGOTTEN_COLOSSUS.get().create(level.getLevel());
                if (boss != null) {
                    boss.moveTo(bossX + 0.5D, bossY, bossZ + 0.5D, 0.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                    this.spawnedBoss = true;
                }
            }
        }

        private void placeMemoryPillar(WorldGenLevel level, BoundingBox box, int x, int z,
                                       BlockState base, BlockState pillar) {
            this.placeBlock(level, base, x, 1, z, box);
            this.placeBlock(level, base, x, 2, z, box);
            this.placeBlock(level, pillar, x, 3, z, box);
        }

        /**
         * 风门小室外壳整体用锚定封印石(不可破坏):墙、顶、地板全封,
         * 防止挖墙/挖地道绕过风门权限(V0.2 防绕过验收项)。
         */
        private void buildWindVault(WorldGenLevel level, BoundingBox box, RandomSource random,
                                    BlockState anchored, BlockState rune, BlockState windDoor, BlockState air) {
            int x1 = WIDTH / 2 - 3, x2 = WIDTH / 2 + 3;
            int z1 = 0, z2 = 4;
            // 封印地板(整个小室足迹,含墙下,堵地道)
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x, 0, z, box);
                }
            }
            // 额外下探两层,避免从场地外地下斜向挖到风门宝箱底部。
            for (int y = -2; y < 0; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        this.placeBlock(level, anchored, x, y, z, box);
                    }
                }
            }
            // 墙体与屋顶
            for (int x = x1; x <= x2; x++) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, anchored, x, y, z1, box);
                    this.placeBlock(level, anchored, x, y, z2, box);
                }
                for (int z = z1; z <= z2; z++) {
                    this.placeBlock(level, anchored, x, 4, z, box);
                }
            }
            for (int z = z1; z <= z2; z++) {
                for (int y = 1; y <= 3; y++) {
                    this.placeBlock(level, anchored, x1, y, z, box);
                    this.placeBlock(level, anchored, x2, y, z, box);
                }
            }
            // 内部清空
            this.generateBox(level, box, x1 + 1, 1, z1 + 1, x2 - 1, 3, z2 - 1, air, air, false);
            // 风门(两格高)嵌在南墙正中(下块标记上方相连,开启态模型合并为整扇)
            this.placeBlock(level, windDoor.setValue(
                    cn.kurt6.unknown_echoes.block.puzzle.WindDoorBlock.CONN_UP, Boolean.TRUE),
                    WIDTH / 2, 1, z2, box);
            this.placeBlock(level, windDoor, WIDTH / 2, 2, z2, box);
            // 门侧符文提示:贴在墙外侧(场地一侧),只是装饰,挖掉也露不出缺口
            this.placeBlock(level, rune, WIDTH / 2 - 1, 2, z2 + 1, box);
            this.placeBlock(level, rune, WIDTH / 2 + 1, 2, z2 + 1, box);
            // 室内宝箱
            buildAccessibleChest(level, random, box, WIDTH / 2, 1, z1 + 1, ModLootTables.WIND_VAULT_CHEST);
        }
    }
}
