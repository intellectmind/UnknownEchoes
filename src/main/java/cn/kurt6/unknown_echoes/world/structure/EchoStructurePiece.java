package cn.kurt6.unknown_echoes.world.structure;

import cn.kurt6.unknown_echoes.block.puzzle.FoldpagePairCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModLootTables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

abstract class EchoStructurePiece extends ScatteredFeaturePiece {

    protected EchoStructurePiece(StructurePieceType type, int x, int y, int z,
                                 int width, int height, int depth, Direction orientation) {
        super(type, x, y, z, width, height, depth, orientation);
    }

    protected EchoStructurePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
    }

    /** 结构装饰不得覆盖世界最低层基岩;水下结构下沉时也保留自然底层。 */
    @Override
    protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z,
                               BoundingBox box) {
        BlockPos worldPos = this.getWorldPos(x, y, z);
        if (!box.isInside(worldPos)
                || worldPos.getY() <= level.getMinBuildHeight()
                || level.getBlockState(worldPos).is(Blocks.BEDROCK)) {
            return;
        }
        super.placeBlock(level, state, x, y, z, box);
    }

    /**
     * 断言 small <= big,传反了立刻抛错而不是静默生成空建筑。
     * 只用于循环边界;比例性的顺序(如 wallHeight 与 baySpacing)传反只是比例奇怪,不要用它。
     */
    private static void requireAscending(String helper, String smallName, int small,
                                         String bigName, int big) {
        if (small > big) {
            throw new IllegalArgumentException(helper + ": " + smallName + " (" + small
                    + ") must not exceed " + bigName + " (" + big
                    + "); the generating loop would place nothing at all");
        }
    }

    /** 本片区的方形足印边长(局部坐标)。壁龛靠它判断朝内开门,取错会把门开在外墙上。 */
    protected int siteFootprint() {
        return Math.max(this.boundingBox.getXSpan(), this.boundingBox.getZSpan());
    }

    /** 分支奖励台(明奖)。引用掉落表,子类不要各自复制一份,否则奖励口径分叉。 */
    protected void buildSiteBranchReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                         int x, int y, int z,
                                         ResourceKey<LootTable> loot) {
        BlockState floor = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
        BlockState accent = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
        int floorY = Math.max(0, y - 1);
        sitePlatform(level, box, x - 2, floorY, z - 2, 5, floor);
        clearChestPocket(level, box, x, y, z);
        sitePillar(level, box, x - 2, y, z - 2, 2, accent);
        sitePillar(level, box, x + 2, y, z - 2, 2, accent);
        sitePillar(level, box, x - 2, y, z + 2, 2, accent);
        sitePillar(level, box, x + 2, y, z + 2, 2, accent);
        this.placeBlock(level, accent, x, y, z - 1, box);
        buildAccessibleChest(level, random, box, x, y, z, loot);
    }

    /** 暗奖:真视宝库壁龛,开门方向按壁龛在足印中的位置朝内。 */
    protected void buildSiteHiddenReward(WorldGenLevel level, RandomSource random, BoundingBox box,
                                         int x, int y, int z) {
        buildTrueSightVaultNiche(level, random, box, x, y, z, siteVaultEntrance(x, z));
    }

    /** 壁龛朝内开门:贴哪条边就朝相反方向开。 */
    protected Direction siteVaultEntrance(int x, int z) {
        int size = siteFootprint();
        if (x >= size - 5) {
            return Direction.WEST;
        }
        if (x <= 4) {
            return Direction.EAST;
        }
        if (z >= size - 5) {
            return Direction.NORTH;
        }
        return Direction.SOUTH;
    }

    /** 完整探索环:引导入口 → 明奖 → 暗奖,坐标按足印夹取。不得省略入口,否则玩家找不到奖励分支。 */
    protected void addSiteRewardLoop(WorldGenLevel level, RandomSource random, BoundingBox box,
                                     int center, ResourceKey<LootTable> loot) {
        int size = siteFootprint();
        buildSiteGuidedEntrance(level, box, center,
                ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState());
        buildSiteBranchReward(level, random, box, Math.max(2, center - 4), 1,
                Math.min(size - 3, center + 3), loot);
        buildSiteHiddenReward(level, random, box, Math.min(size - 3, center + 4), 1,
                Math.max(2, center - 4));
    }

    /** 从结构边缘向核心铺引导路。基类只定契约不给几何:两类遗址是 3 格宽全程与 7 格宽引桥两种设计。 */
    protected void buildSiteGuidedEntrance(WorldGenLevel level, BoundingBox box,
                                           int center, BlockState state) {
    }

    private void sitePlatform(WorldGenLevel level, BoundingBox box, int x0, int y, int z0,
                              int size, BlockState state) {
        for (int x = x0; x < x0 + size; x++) {
            for (int z = z0; z < z0 + size; z++) {
                this.placeBlock(level, state, x, y, z, box);
            }
        }
    }

    private void sitePillar(WorldGenLevel level, BoundingBox box, int x, int y, int z,
                            int height, BlockState state) {
        for (int i = 0; i < height; i++) {
            this.placeBlock(level, state, x, y + i, z, box);
        }
    }

    protected void buildTrueSightVaultNiche(WorldGenLevel level, RandomSource random, BoundingBox box,
                                            int x, int y, int z, Direction entranceSide) {
        buildHiddenVaultNiche(level, box, x, y, z, horizontal(entranceSide));
        this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), x, y, z, box);
        configureTrueSightVault(level, box, random, x, y, z);
    }

    protected void buildCompactTrueSightVaultNiche(WorldGenLevel level, RandomSource random, BoundingBox box,
                                                   int x, int y, int z, Direction entranceSide) {
        Direction side = horizontal(entranceSide);
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState hidden = ModBlocks.HIDDEN_RUNE_BRICKS.get().defaultBlockState();
        sealHiddenNiche(level, box, anchored, x - 1, x + 1, z - 1, z + 1, y - 1, y + 3);
        placeHiddenDoor(level, box, hidden, side, x, y, z, x - 1, x + 1, z - 1, z + 1);
        clearCompactVaultApproach(level, box, side, x, y, z);
        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y + 1, z, box);
        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y + 2, z, box);
        this.placeBlock(level, ModBlocks.TRUE_SIGHT_CHEST.get().defaultBlockState(), x, y, z, box);
        configureTrueSightVault(level, box, random, x, y, z);
    }

    protected void buildAccessibleChest(WorldGenLevel level, RandomSource random, BoundingBox box,
                                        int x, int y, int z,
                                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        BlockState floor = ModBlocks.ECHO_STONE_BRICKS.get().defaultBlockState();
        BlockState accent = ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState();
        int floorY = y - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                this.placeBlock(level, floor, x + dx, floorY, z + dz, box);
            }
        }
        this.placeBlock(level, accent, x, floorY, z - 2, box);
        clearChestPocket(level, box, x, y, z);
        this.createChest(level, box, random, x, y, z, loot);
    }

    protected void buildSteppedDisk(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    int radius, int levels,
                                    BlockState foundation, BlockState surface) {
        for (int layer = 0; layer < levels; layer++) {
            int layerRadius = Math.max(1, radius - layer * 2);
            architecturalDisk(level, box, centerX, baseY + layer, centerZ, layerRadius,
                    layer == levels - 1 ? surface : foundation);
        }
    }

    protected void buildCircularColonnade(WorldGenLevel level, BoundingBox box,
                                           int centerX, int baseY, int centerZ,
                                           int radius, int columns, int height,
                                           BlockState pillar, BlockState capital,
                                           BlockState ring) {
        for (int i = 0; i < columns; i++) {
            double angle = Math.PI * 2.0D * i / columns;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            architecturalPillar(level, box, x, baseY, z, height, pillar);
            this.placeBlock(level, capital, x, baseY + height, z, box);
        }
        architecturalRing(level, box, centerX, baseY + height, centerZ, radius, ring);
    }

    protected void buildCardinalGateway(WorldGenLevel level, BoundingBox box,
                                         int centerX, int baseY, int centerZ,
                                         int radius, Direction direction,
                                         int halfWidth, int height,
                                         BlockState pillar, BlockState accent) {
        Direction side = direction.getClockWise();
        int gateX = centerX + direction.getStepX() * radius;
        int gateZ = centerZ + direction.getStepZ() * radius;
        for (int sign : new int[]{-1, 1}) {
            int x = gateX + side.getStepX() * halfWidth * sign;
            int z = gateZ + side.getStepZ() * halfWidth * sign;
            architecturalPillar(level, box, x, baseY, z, height, pillar);
            this.placeBlock(level, accent, x, baseY + height, z, box);
        }
        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            double normalized = lateral / (double) Math.max(1, halfWidth);
            int archRise = (int) Math.round(2.0D * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
            int x = gateX + side.getStepX() * lateral;
            int z = gateZ + side.getStepZ() * lateral;
            this.placeBlock(level, Math.abs(lateral) <= 1 ? accent : pillar,
                    x, baseY + height + archRise, z, box);
        }
    }

    /** 符文引道。startRadius 必须 <= endRadius,否则整条引道不生成。 */
    protected void buildRuneCauseway(WorldGenLevel level, BoundingBox box,
                                      int centerX, int y, int centerZ,
                                      Direction outward, int startRadius, int endRadius, int halfWidth,
                                      BlockState floor, BlockState edge, BlockState rune) {
        requireAscending("buildRuneCauseway", "startRadius", startRadius,
                "endRadius", endRadius);
        Direction side = outward.getClockWise();
        for (int distance = startRadius; distance <= endRadius; distance++) {
            int pathX = centerX + outward.getStepX() * distance;
            int pathZ = centerZ + outward.getStepZ() * distance;
            for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                BlockState state = Math.abs(lateral) == halfWidth ? edge : floor;
                if (lateral == 0 && distance % 4 == 0) {
                    state = rune;
                }
                this.placeBlock(level, state,
                        pathX + side.getStepX() * lateral, y,
                        pathZ + side.getStepZ() * lateral, box);
            }
        }
    }

    /** 放射扶壁。innerRadius 必须 <= outerRadius,否则整圈扶壁不生成。 */
    protected void buildRadialButtresses(WorldGenLevel level, BoundingBox box,
                                          int centerX, int baseY, int centerZ,
                                          int innerRadius, int outerRadius, int height,
                                          BlockState body, BlockState accent) {
        requireAscending("buildRadialButtresses", "innerRadius", innerRadius,
                "outerRadius", outerRadius);
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] direction : directions) {
            boolean diagonal = direction[0] != 0 && direction[1] != 0;
            int actualOuter = diagonal ? Math.max(innerRadius, outerRadius - 2) : outerRadius;
            for (int distance = innerRadius; distance <= actualOuter; distance++) {
                int taper = Math.max(1, height - (distance - innerRadius) / 2);
                int x = centerX + direction[0] * distance;
                int z = centerZ + direction[1] * distance;
                architecturalPillar(level, box, x, baseY, z, taper,
                        distance == actualOuter ? accent : body);
            }
        }
    }

    protected void buildRibbedVaultHall(WorldGenLevel level, BoundingBox box,
                                        int centerX, int baseY, int centerZ,
                                        Footprint footprint,
                                        int wallHeight, int baySpacing,
                                        BlockState floor, BlockState wall,
                                        BlockState rib, BlockState light) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        int aisleX = Math.max(3, halfWidth - 4);
        int vaultRise = Math.max(3, halfWidth / 2);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockState state = (x == centerX || Math.floorMod(z - minZ, baySpacing) == 0)
                        ? rib : floor;
                this.placeBlock(level, state, x, baseY - 1, z, box);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            int bay = Math.floorMod(z - minZ, baySpacing);
            boolean buttress = bay == 0 || bay == 1 || z == maxZ;
            for (int side : new int[]{-1, 1}) {
                int wallX = centerX + side * halfWidth;
                for (int y = 0; y <= wallHeight; y++) {
                    boolean plinth = y <= 2;
                    boolean clerestory = y >= wallHeight - 1;
                    boolean arcadeOpening = !buttress && bay >= 2 && bay <= baySpacing - 2 && y <= 4;
                    if (!arcadeOpening && (buttress || plinth || clerestory)) {
                        this.placeBlock(level, buttress && y % 4 == 0 ? rib : wall,
                                wallX, baseY + y, z, box);
                    }
                }
                if (buttress) {
                    architecturalPillar(level, box,
                            centerX + side * aisleX, baseY, z, wallHeight - 1, wall);
                    this.placeBlock(level, light,
                            centerX + side * aisleX, baseY + wallHeight, z, box);
                }
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            boolean ribBay = Math.floorMod(z - minZ, baySpacing) == 0 || z == maxZ;
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                double normalized = dx / (double) Math.max(1, halfWidth);
                int rise = (int) Math.round(vaultRise
                        * Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized)));
                BlockState state = ribBay
                        ? dx == 0 || Math.abs(dx) == aisleX ? light : rib
                        : Math.abs(dx) <= 2 ? light : wall;
                this.placeBlock(level, state,
                        centerX + dx, baseY + wallHeight + rise, z, box);
            }
        }
        int ridgeY = baseY + wallHeight + vaultRise;
        for (int z = minZ; z <= maxZ; z++) {
            this.placeBlock(level, Math.floorMod(z - minZ, baySpacing) == 0 ? light : rib,
                    centerX, ridgeY, z, box);
        }

        for (int endZ : new int[]{minZ, maxZ}) {
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                int gableTop = wallHeight + Math.max(0,
                        (int) Math.round(vaultRise * (1.0D - Math.abs(dx) / (double) halfWidth)));
                for (int y = 0; y <= gableTop; y++) {
                    boolean doorway = Math.abs(dx) <= 2 && y <= 4;
                    boolean roseWindow = Math.abs(dx) <= 2
                            && y >= wallHeight - 2 && y <= wallHeight + 1;
                    if (!doorway) {
                        this.placeBlock(level, roseWindow ? light : y % 5 == 0 ? rib : wall,
                                centerX + dx, baseY + y, endZ, box);
                    }
                }
            }
        }
    }

    /** 双塔正面。towerRadius 必须 <= towerOffset,否则两塔之间的正墙整块不生成,只剩两根孤塔。 */
    protected void buildTwinTowerFacade(WorldGenLevel level, BoundingBox box,
                                        int centerX, int baseY, int facadeZ,
                                        int towerOffset, int towerRadius, int towerHeight,
                                        BlockState body, BlockState trim, BlockState light) {
        requireAscending("buildTwinTowerFacade", "towerRadius", towerRadius,
                "towerOffset", towerOffset);
        for (int side : new int[]{-1, 1}) {
            int towerX = centerX + side * towerOffset;
            for (int y = 0; y <= towerHeight; y++) {
                int radius = y >= towerHeight * 2 / 3 ? Math.max(2, towerRadius - 1) : towerRadius;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                            BlockState state = y % 5 == 0 ? trim : body;
                            this.placeBlock(level, state, towerX + dx, baseY + y, facadeZ + dz, box);
                        }
                    }
                }
            }
            int crownY = baseY + towerHeight + 1;
            for (int dx = -towerRadius; dx <= towerRadius; dx++) {
                for (int dz = -towerRadius; dz <= towerRadius; dz++) {
                    if (Math.abs(dx) == towerRadius || Math.abs(dz) == towerRadius) {
                        this.placeBlock(level, (dx + dz) % 2 == 0 ? trim : body,
                                towerX + dx, crownY, facadeZ + dz, box);
                    }
                }
            }
            this.placeBlock(level, light, towerX, crownY + 1, facadeZ, box);
        }

        int facadeHeight = Math.max(8, towerHeight * 2 / 3);
        for (int dx = -towerOffset + towerRadius; dx <= towerOffset - towerRadius; dx++) {
            int gable = Math.max(0, (towerOffset - Math.abs(dx)) / 3);
            for (int y = 0; y <= facadeHeight + gable; y++) {
                boolean mainDoor = Math.abs(dx) <= 3 && y <= 5;
                boolean roseWindow = Math.abs(dx) <= 2
                        && y >= facadeHeight - 2 && y <= facadeHeight + 1;
                if (!mainDoor) {
                    this.placeBlock(level, roseWindow ? light : y % 5 == 0 ? trim : body,
                            centerX + dx, baseY + y, facadeZ, box);
                }
            }
        }
        buildCardinalGateway(level, box, centerX, baseY, facadeZ, 0, Direction.NORTH,
                3, 6, body, trim);
    }

    /** 开放式侧翼/耳堂，四周连续拱廊提供建筑边界而不切碎 Boss 走位。 */
    protected void buildArcadedWing(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    Footprint footprint, int height, int spacing,
                                    BlockState floor, BlockState pillar,
                                    BlockState beam, BlockState light) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                this.placeBlock(level, (x == centerX || z == centerZ) ? beam : floor,
                        x, baseY - 1, z, box);
            }
        }
        for (int x = minX; x <= maxX; x += spacing) {
            architecturalPillar(level, box, x, baseY, minZ, height, pillar);
            architecturalPillar(level, box, x, baseY, maxZ, height, pillar);
            this.placeBlock(level, light, x, baseY + height, minZ, box);
            this.placeBlock(level, light, x, baseY + height, maxZ, box);
        }
        for (int z = minZ; z <= maxZ; z += spacing) {
            architecturalPillar(level, box, minX, baseY, z, height, pillar);
            architecturalPillar(level, box, maxX, baseY, z, height, pillar);
            this.placeBlock(level, light, minX, baseY + height, z, box);
            this.placeBlock(level, light, maxX, baseY + height, z, box);
        }
        for (int x = minX; x <= maxX; x++) {
            this.placeBlock(level, beam, x, baseY + height, minZ, box);
            this.placeBlock(level, beam, x, baseY + height, maxZ, box);
        }
        for (int z = minZ; z <= maxZ; z++) {
            this.placeBlock(level, beam, minX, baseY + height, z, box);
            this.placeBlock(level, beam, maxX, baseY + height, z, box);
        }
    }

    protected void buildTieredTower(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    int baseRadius, int height,
                                    BlockState body, BlockState band, BlockState light) {
        for (int y = 0; y <= height; y++) {
            int tier = y * 3 / Math.max(1, height);
            int radius = Math.max(2, baseRadius - tier);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        this.placeBlock(level, y % 5 == 0 ? band : body,
                                centerX + dx, baseY + y, centerZ + dz, box);
                    }
                }
            }
        }
        architecturalDisk(level, box, centerX, baseY + height + 1, centerZ,
                Math.max(2, baseRadius - 2), band);
        this.placeBlock(level, light, centerX, baseY + height + 2, centerZ, box);
    }

    /**
     * 有完整山墙、开放侧廊和分段屋架的可进入亭殿。中央保持净空，可包覆水井、祭坛、苗圃等既有玩法点；
     * 南北两端均留门，避免外壳把玩家封死。
     */
    protected void buildGabledPavilion(WorldGenLevel level, BoundingBox box,
                                       int centerX, int baseY, int centerZ,
                                       Footprint footprint, int wallHeight,
                                       int baySpacing, BlockState floor, BlockState wall,
                                       BlockState roof, BlockState light) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                this.placeBlock(level, (x == centerX || z == centerZ) ? roof : floor,
                        x, baseY - 1, z, box);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            boolean frameBay = z == minZ || z == maxZ
                    || Math.floorMod(z - minZ, Math.max(2, baySpacing)) == 0;
            for (int side : new int[]{-1, 1}) {
                int x = centerX + side * halfWidth;
                for (int y = 0; y <= wallHeight; y++) {
                    boolean solid = frameBay || y <= 1 || y == wallHeight;
                    if (solid) {
                        this.placeBlock(level, frameBay && y % 3 == 0 ? roof : wall,
                                x, baseY + y, z, box);
                    }
                }
                if (frameBay) {
                    this.placeBlock(level, light, x, baseY + wallHeight - 1, z, box);
                }
            }
        }

        int roofRise = Math.max(2, halfWidth / 2);
        for (int endZ : new int[]{minZ, maxZ}) {
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                int gable = wallHeight + Math.max(0,
                        (int) Math.round(roofRise * (1.0D - Math.abs(dx) / (double) halfWidth)));
                for (int y = 0; y <= gable; y++) {
                    boolean doorway = Math.abs(dx) <= 1 && y <= 3;
                    boolean window = Math.abs(dx) <= 1 && y >= wallHeight - 1;
                    if (!doorway) {
                        this.placeBlock(level, window ? light : y % 4 == 0 ? roof : wall,
                                centerX + dx, baseY + y, endZ, box);
                    }
                }
            }
        }

        for (int dx = -halfWidth - 1; dx <= halfWidth + 1; dx++) {
            int clamped = Math.min(halfWidth, Math.abs(dx));
            int rise = (halfWidth - clamped) * roofRise / Math.max(1, halfWidth);
            int roofY = baseY + wallHeight + 1 + rise;
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                BlockState state = dx == 0 && Math.floorMod(z - minZ, baySpacing) == 0
                        ? light : roof;
                this.placeBlock(level, state, centerX + dx, roofY, z, box);
            }
        }
    }

    /**
     * 带桥塔、连续护墙和下部桥墩的纪念性长桥。
     * 桥面方向由 forward 决定，两端中央保持三格宽通道，可直接连接门楼或山道。
     */
    protected void buildMonumentalBridge(WorldGenLevel level, BoundingBox box,
                                         int centerX, int deckY, int centerZ,
                                         Direction forward, int halfLength, int halfWidth,
                                         int towerHeight, BlockState deck, BlockState wall,
                                         BlockState trim, BlockState light) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        for (int distance = -halfLength; distance <= halfLength; distance++) {
            for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                int x = localX(centerX, axis, side, distance, lateral);
                int z = localZ(centerZ, axis, side, distance, lateral);
                this.placeBlock(level,
                        lateral == 0 && Math.floorMod(distance, 5) == 0 ? trim : deck,
                        x, deckY, z, box);
                if (Math.abs(lateral) == halfWidth) {
                    this.placeBlock(level, wall, x, deckY - 1, z, box);
                    if (Math.floorMod(distance, 3) != 1) {
                        this.placeBlock(level, Math.floorMod(distance, 5) == 0 ? trim : wall,
                                x, deckY + 1, z, box);
                    }
                }
            }
        }

        int[] pierDistances = {-halfLength + 3, -halfLength / 3, halfLength / 3, halfLength - 3};
        for (int distance : pierDistances) {
            for (int lateral : new int[]{-halfWidth, halfWidth}) {
                int x = localX(centerX, axis, side, distance, lateral);
                int z = localZ(centerZ, axis, side, distance, lateral);
                architecturalPillar(level, box, x, 0, z, deckY + 1, wall);
                this.placeBlock(level, trim, x, deckY + 1, z, box);
            }
        }

        for (int distance : new int[]{-halfLength + 1, halfLength - 1}) {
            for (int lateral : new int[]{-halfWidth - 1, halfWidth + 1}) {
                int x = localX(centerX, axis, side, distance, lateral);
                int z = localZ(centerZ, axis, side, distance, lateral);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                            architecturalPillar(level, box, x + dx, deckY, z + dz,
                                    towerHeight, wall);
                        }
                    }
                }
                architecturalDisk(level, box, x, deckY + towerHeight, z, 2, trim);
                this.placeBlock(level, light, x, deckY + towerHeight + 1, z, box);
            }
            int gateX = localX(centerX, axis, side, distance, 0);
            int gateZ = localZ(centerZ, axis, side, distance, 0);
            buildCardinalGateway(level, box, gateX, deckY + 1, gateZ, 0,
                    axis, halfWidth, Math.max(4, towerHeight - 2), wall, trim);
        }
    }

    protected void buildBossApse(WorldGenLevel level, BoundingBox box,
                                 int centerX, int baseY, int centerZ,
                                 Direction outward, int radius, int height,
                                 BlockState floor, BlockState wall,
                                 BlockState rib, BlockState light) {
        Direction side = outward.getClockWise();
        for (int forward = 0; forward <= radius; forward++) {
            int halfSpan = (int) Math.floor(Math.sqrt(radius * radius - forward * forward));
            for (int lateral = -halfSpan; lateral <= halfSpan; lateral++) {
                int x = centerX + outward.getStepX() * forward + side.getStepX() * lateral;
                int z = centerZ + outward.getStepZ() * forward + side.getStepZ() * lateral;
                this.placeBlock(level, lateral == 0 ? rib : floor, x, baseY - 1, z, box);
                if (Math.abs(lateral) == halfSpan) {
                    for (int y = 0; y <= height; y++) {
                        this.placeBlock(level, y == height ? rib : wall, x, baseY + y, z, box);
                    }
                }
            }
        }
        for (int lateral = -radius; lateral <= radius; lateral += Math.max(3, radius / 2)) {
            int x = centerX + side.getStepX() * lateral;
            int z = centerZ + side.getStepZ() * lateral;
            architecturalPillar(level, box, x, baseY, z, height, wall);
            this.placeBlock(level, light, x, baseY + height, z, box);
        }
    }

    /**
     * 必须在建筑摆完之后调用:矩形厅 helper 的柱列一定会插进圆形走位区。
     * radius 单位格,fromY/toY 相对地板(1 = 地板上方第一格);机关、封印、宝箱与祭坛让行,地板与流体不动。
     */
    protected void clearArenaVolume(WorldGenLevel level, BoundingBox box,
                                    int centerX, int floorY, int centerZ,
                                    int radius, int fromY, int toY) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                for (int y = floorY + fromY; y <= floorY + toY; y++) {
                    BlockState existing = this.getBlock(level, x, y, z, box);
                    if (existing.isAir() || isArenaFixture(existing)
                            || !existing.getFluidState().isEmpty()) {
                        continue;
                    }
                    this.placeBlock(level, air, x, y, z, box);
                }
            }
        }
    }

    /** 场地净空必须让行的方块:机关、封印、奖励与生成锚点。 */
    private boolean isArenaFixture(BlockState state) {
        return state.is(ModBlocks.MINIBOSS_SPAWNER.get())
                || state.is(ModBlocks.SEALED_STONE.get())
                || state.is(ModBlocks.ANCHORED_SEALED_STONE.get())
                || state.is(ModBlocks.HIDDEN_RUNE_BRICKS.get())
                || state.is(ModBlocks.PUZZLE_CORE.get())
                || state.is(ModBlocks.CHIME_ECHO_CORE.get())
                || state.is(ModBlocks.FOLDPAGE_PAIR_CORE.get())
                || state.is(ModBlocks.FOLDPAGE_TILE.get())
                || state.is(ModBlocks.SEQUENCE_RUNE.get())
                || state.is(ModBlocks.STEP_RUNE.get())
                || state.is(ModBlocks.REWARD_ALTAR.get())
                || state.is(ModBlocks.TRUE_SIGHT_ALTAR.get())
                || state.is(ModBlocks.TRUE_SIGHT_CHEST.get())
                || state.is(ModBlocks.TRUE_SIGHT_STELE.get())
                || state.is(ModBlocks.ARTIFACT_RECORD_TABLE.get())
                || state.is(ModBlocks.GRAVITY_CORE_ALTAR.get())
                || state.is(Blocks.CHEST)
                || state.is(Blocks.SPAWNER);
    }

    /**
     * Boss 场地地面纹章。
     * 只改写已有实体地板，并保留中央 5x5 机关区与水体，因此可安全复用于埋有刷怪器、祭坛或水下机关的场地。
     */
    protected void buildBossArenaMandala(WorldGenLevel level, BoundingBox box,
                                          int centerX, int floorY, int centerZ, int radius,
                                          BlockState outerBand, BlockState paths,
                                          BlockState focus) {
        int outerSquared = radius * radius;
        int innerRadius = Math.max(3, radius / 3);
        int middleRadius = Math.max(innerRadius + 2, radius * 2 / 3);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > outerSquared || distanceSquared <= 4) {
                    continue;
                }
                int distance = (int) Math.round(Math.sqrt(distanceSquared));
                boolean outerRing = Math.abs(distance - radius) <= 1;
                boolean middleRing = Math.abs(distance - middleRadius) == 0;
                boolean innerRing = Math.abs(distance - innerRadius) == 0;
                boolean cardinalPath = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)
                        && distance >= innerRadius;
                boolean diagonalPath = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1
                        && distance >= middleRadius;
                if (!outerRing && !middleRing && !innerRing && !cardinalPath && !diagonalPath) {
                    continue;
                }

                BlockState existing = this.getBlock(level, x, floorY, z, box);
                if (existing.isAir() || !existing.getFluidState().isEmpty()
                        || existing.is(ModBlocks.MINIBOSS_SPAWNER.get())) {
                    continue;
                }
                BlockState state = innerRing ? focus
                        : outerRing || diagonalPath ? outerBand : paths;
                this.placeBlock(level, state, x, floorY, z, box);
            }
        }
    }

    /** 断冠式 Boss 外圈。八座错位高柱避开四条主轴，分段悬环只建立远景轮廓，不会在场地中央制造碰撞障碍或封闭入口。 */
    protected void buildBossArenaCrown(WorldGenLevel level, BoundingBox box,
                                        int centerX, int baseY, int centerZ,
                                        int radius, int height,
                                        BlockState body, BlockState band,
                                        BlockState light) {
        for (int index = 0; index < 8; index++) {
            double angle = Math.toRadians(22.5D + index * 45.0D);
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            architecturalDisk(level, box, x, baseY - 1, z, 1, body);
            architecturalPillar(level, box, x, baseY, z, height, body);
            this.placeBlock(level, band, x, baseY + Math.max(2, height / 2), z, box);
            this.placeBlock(level, light, x, baseY + height, z, box);
        }

        int samples = Math.max(32, radius * 4);
        for (int sample = 0; sample < samples; sample++) {
            int segment = Math.floorMod(sample * 8 / samples, 8);
            int withinSegment = Math.floorMod(sample * 8, samples);
            if (withinSegment < samples / 4 || withinSegment > samples * 3 / 4) {
                continue;
            }
            double angle = Math.PI * 2.0D * sample / samples;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            BlockState state = (sample + segment) % 5 == 0 ? light : band;
            this.placeBlock(level, state, x, baseY + height, z, box);
        }
    }

    /**
     * 半球穹顶圆厅:中央整跨净空、没有落地柱,适合 Boss 场地(肋拱厅的柱列会侵入走位区)。
     * 末位参数是灯位(顶眼环),必须 lightLevel >= 6。
     */
    protected void buildDomedRotunda(WorldGenLevel level, BoundingBox box,
                                     int centerX, int baseY, int centerZ,
                                     int radius, int drumHeight,
                                     BlockState floor, BlockState drum,
                                     BlockState shell, BlockState light) {
        int rise = Math.max(3, radius * 2 / 3);
        architecturalDisk(level, box, centerX, baseY - 1, centerZ, radius, floor);
        architecturalRing(level, box, centerX, baseY - 1, centerZ, radius, drum);

        for (int y = 0; y <= drumHeight; y++) {
            for (int angleStep = 0; angleStep < 360; angleStep++) {
                double angle = Math.toRadians(angleStep);
                int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
                int dx = x - centerX;
                int dz = z - centerZ;
                boolean doorway = (Math.abs(dx) <= 2 || Math.abs(dz) <= 2) && y <= 4;
                if (doorway) {
                    continue;
                }
                this.placeBlock(level, y % 4 == 0 ? drum : shell, x, baseY + y, z, box);
            }
        }

        int eyeRadius = Math.max(1, radius / 6);
        for (int layer = 0; layer <= rise; layer++) {
            double ratio = layer / (double) rise;
            int layerRadius = (int) Math.round(radius * Math.cos(ratio * Math.PI / 2.0D));
            if (layerRadius <= eyeRadius) {
                break;
            }
            int y = baseY + drumHeight + layer;
            boolean ribLayer = layer % 3 == 0;
            for (int angleStep = 0; angleStep < 360; angleStep++) {
                double angle = Math.toRadians(angleStep);
                int x = centerX + (int) Math.round(Math.cos(angle) * layerRadius);
                int z = centerZ + (int) Math.round(Math.sin(angle) * layerRadius);
                this.placeBlock(level, ribLayer ? drum : shell, x, y, z, box);
            }
        }

        // 灯环悬在穹顶顶眼正中:破坏后只留空气,不会开出通路。
        int eyeY = baseY + drumHeight + rise;
        architecturalRing(level, box, centerX, eyeY, centerZ, eyeRadius + 1, light);
    }

    /**
     * 逐层退台,台体不封顶:封顶会在场地上方铺出实心顶板,而 clearArenaVolume 清不到那个高度。
     * coreRadius 是不可侵入的中央半径(套 Boss 场地须 >= 战斗半径,纯地标传 0);末位是灯位,需 lightLevel >= 6。
     */
    protected void buildSteppedZiggurat(WorldGenLevel level, BoundingBox box,
                                        int centerX, int baseY, int centerZ,
                                        int baseRadius, int tiers, int tierHeight,
                                        int coreRadius,
                                        BlockState body, BlockState edge,
                                        BlockState stair, BlockState light) {
        int levels = Math.max(2, tiers);
        int step = Math.max(2, tierHeight);
        int inset = Math.max(2, baseRadius / levels);
        // 退台平台带宽度:比收分量小一格,保证相邻两层之间有可站立的平台而不连成实心。
        int platformWidth = Math.max(1, inset - 1);
        int floorRadius = Math.max(2, coreRadius);
        int topTier = levels;
        for (int tier = 0; tier < levels; tier++) {
            int tierRadius = baseRadius - tier * inset;
            if (tierRadius <= floorRadius) {
                // 已经收到中央保护半径,停止继续向内收分。
                topTier = tier;
                break;
            }
            int tierBase = baseY + tier * step;
            for (int y = 0; y < step; y++) {
                boolean top = y == step - 1;
                for (int x = centerX - tierRadius; x <= centerX + tierRadius; x++) {
                    for (int z = centerZ - tierRadius; z <= centerZ + tierRadius; z++) {
                        int ring = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
                        boolean wall = ring == tierRadius;
                        // 只在台壁内侧铺一条平台带,中央留空。
                        boolean platform = top && ring < tierRadius
                                && ring >= Math.max(floorRadius, tierRadius - platformWidth);
                        if (!wall && !platform) {
                            continue;
                        }
                        this.placeBlock(level, platform ? edge : body, x, tierBase + y, z, box);
                    }
                }
            }
            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    int x = centerX + sx * tierRadius;
                    int z = centerZ + sz * tierRadius;
                    BlockState oriented = orientStairs(stair,
                            sx > 0 ? Direction.WEST : Direction.EAST, Half.BOTTOM);
                    placeCorniceBlock(level, box, oriented, x, tierBase + step - 1, z);
                }
            }
            topTier = tier + 1;
        }
        int topY = baseY + topTier * step;
        int topRadius = Math.max(floorRadius, baseRadius - (topTier - 1) * inset);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                this.placeBlock(level, light,
                        centerX + sx * topRadius, topY, centerZ + sz * topRadius, box);
            }
        }
    }

    /**
     * 四面围合中庭:外圈实墙 + 内圈回廊柱 + 中央开天井,中央保持天光与完整净空。
     * 末位参数是灯位(四角回廊柱头),必须 lightLevel >= 6。
     */
    protected void buildCourtyardCloister(WorldGenLevel level, BoundingBox box,
                                          int centerX, int baseY, int centerZ,
                                          Footprint footprint,
                                          int wallHeight, int arcadeInset,
                                          BlockState floor, BlockState wall,
                                          BlockState beam, BlockState light) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        int inset = Math.max(2, arcadeInset);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean walkway = Math.abs(x - centerX) >= halfWidth - inset
                        || Math.abs(z - centerZ) >= halfDepth - inset;
                this.placeBlock(level, walkway ? beam : floor, x, baseY - 1, z, box);
            }
        }

        for (int y = 0; y <= wallHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean door = Math.abs(x - centerX) <= 1 && y <= 3;
                if (!door) {
                    BlockState state = y == wallHeight || y % 4 == 0 ? beam : wall;
                    this.placeBlock(level, state, x, baseY + y, minZ, box);
                    this.placeBlock(level, state, x, baseY + y, maxZ, box);
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                boolean door = Math.abs(z - centerZ) <= 1 && y <= 3;
                if (!door) {
                    BlockState state = y == wallHeight || y % 4 == 0 ? beam : wall;
                    this.placeBlock(level, state, minX, baseY + y, z, box);
                    this.placeBlock(level, state, maxX, baseY + y, z, box);
                }
            }
        }

        // 下限 1:inset >= halfWidth 时 innerX 会变成 0 或负数,回廊柱与顶板翻折到中庭中央。
        int innerX = Math.max(1, halfWidth - inset);
        int innerZ = Math.max(1, halfDepth - inset);
        int arcadeHeight = Math.max(3, wallHeight - 2);
        for (int x = centerX - innerX; x <= centerX + innerX; x += 3) {
            for (int side : new int[]{-1, 1}) {
                int z = centerZ + side * innerZ;
                architecturalPillar(level, box, x, baseY, z, arcadeHeight, wall);
                this.placeBlock(level, beam, x, baseY + arcadeHeight, z, box);
            }
        }
        for (int z = centerZ - innerZ; z <= centerZ + innerZ; z += 3) {
            for (int side : new int[]{-1, 1}) {
                int x = centerX + side * innerX;
                architecturalPillar(level, box, x, baseY, z, arcadeHeight, wall);
                this.placeBlock(level, beam, x, baseY + arcadeHeight, z, box);
            }
        }
        // 顶板只盖 inset 带,天井不盖。
        for (int x = centerX - innerX; x <= centerX + innerX; x++) {
            for (int side : new int[]{-1, 1}) {
                for (int offset = 0; offset < inset; offset++) {
                    int z = centerZ + side * (innerZ + offset);
                    this.placeBlock(level, beam, x, baseY + arcadeHeight, z, box);
                }
            }
        }
        for (int z = centerZ - innerZ; z <= centerZ + innerZ; z++) {
            for (int side : new int[]{-1, 1}) {
                for (int offset = 0; offset < inset; offset++) {
                    int x = centerX + side * (innerX + offset);
                    this.placeBlock(level, beam, x, baseY + arcadeHeight, z, box);
                }
            }
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                this.placeBlock(level, light, centerX + sx * innerX,
                        baseY + arcadeHeight - 1, centerZ + sz * innerZ, box);
            }
        }
    }

    /**
     * 悬挑层叠平台:每层向一侧出挑并配斜撑,层间错开。
     * 末位参数是灯位(每层挑头),必须 lightLevel >= 6。
     */
    protected void buildCantileverTerraces(WorldGenLevel level, BoundingBox box,
                                           int centerX, int baseY, int centerZ,
                                           Direction outward, int terraces,
                                           int terraceSpan, int terraceHeight,
                                           BlockState deck, BlockState brace,
                                           BlockState edge, BlockState light) {
        Direction axis = horizontal(outward);
        Direction side = axis.getClockWise();
        int levels = Math.max(2, terraces);
        int span = Math.max(3, terraceSpan);
        int step = Math.max(2, terraceHeight);

        for (int tier = 0; tier < levels; tier++) {
            int y = baseY + tier * step;
            // 出挑方向必须交替,同向会叠成一根柱子。
            int direction = tier % 2 == 0 ? 1 : -1;
            int reach = span - tier;
            if (reach < 2) {
                reach = 2;
            }
            int halfWidth = Math.max(2, span - tier / 2);
            for (int distance = 0; distance <= reach; distance++) {
                for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                    int x = localX(centerX, axis, side, direction * distance, lateral);
                    int z = localZ(centerZ, axis, side, direction * distance, lateral);
                    boolean rim = distance == reach || Math.abs(lateral) == halfWidth;
                    this.placeBlock(level, rim ? edge : deck, x, y, z, box);
                }
            }
            // 斜撑只能在第一层之上:最底层向下伸的斜撑会穿到 baseY 以下,落进场地净空。
            if (tier > 0) {
                for (int lateral : new int[]{-halfWidth, halfWidth}) {
                    for (int back = 1; back <= Math.min(reach, step); back++) {
                        int x = localX(centerX, axis, side, direction * (reach - back), lateral);
                        int z = localZ(centerZ, axis, side, direction * (reach - back), lateral);
                        this.placeBlock(level, brace, x, y - back, z, box);
                    }
                }
            }
            this.placeBlock(level, light,
                    localX(centerX, axis, side, direction * reach, 0), y + 1,
                    localZ(centerZ, axis, side, direction * reach, 0), box);
        }
    }

    /**
     * 半塌残壳。缺口是"墙缺了一段"而不是门,不能拿它当封印室外壳。
     * 末位参数是灯位(残墙顶残留灯),必须 lightLevel >= 6。
     */
    protected void buildRuinedShell(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    int halfWidth, int halfDepth, int wallHeight,
                                    BlockState wall, BlockState rubble,
                                    BlockState accent, BlockState light) {
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        // 跨区块一致:高度轮廓必须由包围盒坐标派生,不能用 postProcess 传入的 random。
        RandomSource shape = RandomSource.create(
                this.boundingBox.minX() * 341873128712L
                        ^ this.boundingBox.minZ() * 132897987541L
                        ^ centerX * 92821L ^ centerZ * 68917L);

        int[] profile = new int[(halfWidth + halfDepth) * 2 + 8];
        for (int i = 0; i < profile.length; i++) {
            profile[i] = shape.nextInt(wallHeight + 1);
        }

        int index = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int side : new int[]{-1, 1}) {
                int z = side < 0 ? minZ : maxZ;
                int top = profile[Math.floorMod(index++, profile.length)];
                for (int y = 0; y <= top; y++) {
                    this.placeBlock(level, y == top ? accent : wall, x, baseY + y, z, box);
                }
                if (top >= wallHeight - 1) {
                    this.placeBlock(level, light, x, baseY + top + 1, z, box);
                }
            }
        }
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            for (int side : new int[]{-1, 1}) {
                int x = side < 0 ? minX : maxX;
                int top = profile[Math.floorMod(index++, profile.length)];
                for (int y = 0; y <= top; y++) {
                    this.placeBlock(level, y == top ? accent : wall, x, baseY + y, z, box);
                }
            }
        }

        // 倒塌堆积只落在墙内侧一格,不侵占中央。
        for (int x = minX + 1; x <= maxX - 1; x++) {
            if (shape.nextFloat() < 0.35F) {
                this.placeBlock(level, rubble, x, baseY, minZ + 1, box);
            }
            if (shape.nextFloat() < 0.35F) {
                this.placeBlock(level, rubble, x, baseY, maxZ - 1, box);
            }
        }
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            if (shape.nextFloat() < 0.35F) {
                this.placeBlock(level, rubble, minX + 1, baseY, z, box);
            }
            if (shape.nextFloat() < 0.35F) {
                this.placeBlock(level, rubble, maxX - 1, baseY, z, box);
            }
        }
    }

    /** 室内网格吊灯。只填空气，因此不会把不可破坏的锚定封印石换成可挖的灯具而开出盗洞。 */
    protected void illuminateInterior(WorldGenLevel level, BoundingBox box,
                                      int centerX, int centerZ, int lampY,
                                      int halfWidth, int halfDepth, int spacing,
                                      BlockState lamp) {
        int step = Math.max(2, spacing);
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfDepth; dz <= halfDepth; dz++) {

                if (Math.floorMod(dx, step) != 0 || Math.floorMod(dz, step) != 0) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                BlockState existing = this.getBlock(level, x, lampY, z, box);
                if (!existing.isAir()) {
                    continue;
                }
                this.placeBlock(level, lamp, x, lampY, z, box);
            }
        }
    }

    protected void buildSealedManor(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    int halfWidth, int halfDepth,
                                    int storeys, int storeyHeight,
                                    BlockState wall, BlockState trim,
                                    BlockState floorState, BlockState window,
                                    BlockState lamp) {
        int levels = Math.max(1, storeys);
        int height = Math.max(3, storeyHeight);
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        BlockState air = Blocks.AIR.defaultBlockState();
        int topY = baseY + levels * height;

        this.generateBox(level, box, minX + 1, baseY, minZ + 1,
                maxX - 1, topY - 1, maxZ - 1, air, air, false);

        for (int storey = 0; storey <= levels; storey++) {
            int y = baseY + storey * height - (storey == 0 ? 1 : 0);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean beam = x == centerX || z == centerZ
                            || Math.floorMod(x - minX, 6) == 0
                            || Math.floorMod(z - minZ, 6) == 0;
                    this.placeBlock(level, beam ? trim : floorState, x, y, z, box);
                }
            }
        }

        for (int storey = 0; storey < levels; storey++) {
            int floorY = baseY + storey * height;
            for (int y = floorY; y < floorY + height; y++) {
                boolean windowBand = (y - floorY) == height / 2;
                for (int x = minX; x <= maxX; x++) {
                    boolean windowSlot = windowBand && Math.floorMod(x - minX, 4) == 2;
                    BlockState state = windowSlot ? window
                            : Math.floorMod(x - minX, 6) == 0 ? trim : wall;
                    this.placeBlock(level, state, x, y, minZ, box);
                    this.placeBlock(level, state, x, y, maxZ, box);
                }
                for (int z = minZ; z <= maxZ; z++) {
                    boolean windowSlot = windowBand && Math.floorMod(z - minZ, 4) == 2;
                    BlockState state = windowSlot ? window
                            : Math.floorMod(z - minZ, 6) == 0 ? trim : wall;
                    this.placeBlock(level, state, minX, y, z, box);
                    this.placeBlock(level, state, maxX, y, z, box);
                }
            }
            buildManorStoreyInterior(level, box, centerX, floorY, centerZ,
                    halfWidth, halfDepth, height, wall, trim, lamp);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                this.placeBlock(level, air, centerX + dx, baseY + dy, maxZ, box);
            }
            this.placeBlock(level, trim, centerX + dx, baseY + 2, maxZ, box);
        }

        int roofRise = Math.max(2, Math.min(halfWidth, halfDepth) / 2);
        for (int layer = 0; layer <= roofRise; layer++) {
            int inset = layer;
            int y = topY + layer;
            for (int x = minX + inset; x <= maxX - inset; x++) {
                for (int z = minZ + inset; z <= maxZ - inset; z++) {
                    boolean edge = x == minX + inset || x == maxX - inset
                            || z == minZ + inset || z == maxZ - inset;
                    if (!edge && layer != roofRise) {
                        continue;
                    }
                    this.placeBlock(level, layer == roofRise ? trim : wall, x, y, z, box);
                }
            }
        }
    }

    private void buildManorStoreyInterior(WorldGenLevel level, BoundingBox box,
                                          int centerX, int floorY, int centerZ,
                                          int halfWidth, int halfDepth, int height,
                                          BlockState wall, BlockState trim, BlockState lamp) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        int ceiling = floorY + height - 1;

        int doorInnerX = Math.max(1, halfWidth - 2);
        int doorOuterX = Math.max(doorInnerX, halfWidth - 1);
        int doorInnerZ = Math.max(1, halfDepth - 2);
        int doorOuterZ = Math.max(doorInnerZ, halfDepth - 1);
        for (int y = floorY; y < ceiling; y++) {
            for (int x = minX + 1; x <= maxX - 1; x++) {
                int distance = Math.abs(x - centerX);
                boolean doorway = distance >= doorInnerX && distance <= doorOuterX
                        && (y - floorY) <= 1;
                this.placeBlock(level, doorway ? air
                        : (y - floorY) == height - 2 ? trim : wall, x, y, centerZ, box);
            }
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                if (z == centerZ) {
                    continue;
                }
                int distance = Math.abs(z - centerZ);
                boolean doorway = distance >= doorInnerZ && distance <= doorOuterZ
                        && (y - floorY) <= 1;
                this.placeBlock(level, doorway ? air
                        : (y - floorY) == height - 2 ? trim : wall, centerX, y, z, box);
            }
        }

        int stairX = minX + 2;
        int stairZ = minZ + 2;
        for (int step = 0; step < height; step++) {
            this.placeBlock(level, trim, stairX + step % 2, floorY + step, stairZ, box);
            this.placeBlock(level, air, stairX + step % 2, floorY + step + 1, stairZ, box);
            this.placeBlock(level, air, stairX + step % 2, floorY + step + 2, stairZ, box);
        }

        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int x = centerX + sx * Math.max(2, halfWidth / 2);
                int z = centerZ + sz * Math.max(2, halfDepth / 2);
                if (this.getBlock(level, x, ceiling - 1, z, box).isAir()) {
                    this.placeBlock(level, lamp, x, ceiling - 1, z, box);
                }
            }
        }
    }

    /** Boss 场地边缘灯柱环。灯柱只落在场地外沿并避开四条正向入口，战斗走位区与中央机关区保持净空。 */
    protected void buildArenaLightRing(WorldGenLevel level, BoundingBox box,
                                       int centerX, int baseY, int centerZ,
                                       int radius, int count, int height,
                                       BlockState pillar, BlockState lamp) {
        int columns = Math.max(4, count);
        for (int index = 0; index < columns; index++) {
            double angle = Math.PI * 2.0D * (index + 0.5D) / columns;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            int dx = x - centerX;
            int dz = z - centerZ;

            if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
                continue;
            }
            architecturalPillar(level, box, x, baseY, z, height, pillar);
            this.placeBlock(level, lamp, x, baseY + height, z, box);
        }
    }

    /**
     * 整砖腰线。需要落进空气才能出挑,因此靠 placeCorniceBlock 跳过机关与封印方块,
     * 并在水下继承 WATERLOGGED——直接 placeBlock 会挤出空气口袋。
     */
    protected void buildCornice(WorldGenLevel level, BoundingBox box,
                               int centerX, int y, int centerZ,
                               Footprint footprint,
                               BlockState body, BlockState accent) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth - 1;
        int maxX = centerX + halfWidth + 1;
        int minZ = centerZ - halfDepth - 1;
        int maxZ = centerZ + halfDepth + 1;
        for (int x = minX; x <= maxX; x++) {
            BlockState state = Math.floorMod(x - minX, 4) == 0 ? accent : body;
            placeCorniceBlock(level, box, state, x, y, minZ);
            placeCorniceBlock(level, box, state, x, y, maxZ);
        }
        for (int z = minZ; z <= maxZ; z++) {
            BlockState state = Math.floorMod(z - minZ, 4) == 0 ? accent : body;
            placeCorniceBlock(level, box, state, minX, y, z);
            placeCorniceBlock(level, box, state, maxX, y, z);
        }
    }

    /** 倒置楼梯挑檐。朝向与上下半在这里设置；四角必须用台阶,楼梯在转角会互相咬合。 */
    protected void buildStairCornice(WorldGenLevel level, BoundingBox box,
                                     int centerX, int y, int centerZ,
                                     Footprint footprint,
                                     BlockState stairs, BlockState cornerSlab,
                                     boolean inverted) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth - 1;
        int maxX = centerX + halfWidth + 1;
        int minZ = centerZ - halfDepth - 1;
        int maxZ = centerZ + halfDepth + 1;
        Half half = inverted ? Half.TOP : Half.BOTTOM;
        BlockState north = orientStairs(stairs, Direction.SOUTH, half);
        BlockState south = orientStairs(stairs, Direction.NORTH, half);
        BlockState west = orientStairs(stairs, Direction.EAST, half);
        BlockState east = orientStairs(stairs, Direction.WEST, half);
        for (int x = minX + 1; x <= maxX - 1; x++) {
            placeCorniceBlock(level, box, north, x, y, minZ);
            placeCorniceBlock(level, box, south, x, y, maxZ);
        }
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            placeCorniceBlock(level, box, west, minX, y, z);
            placeCorniceBlock(level, box, east, maxX, y, z);
        }

        BlockState corner = orientSlab(cornerSlab, inverted);
        placeCorniceBlock(level, box, corner, minX, y, minZ);
        placeCorniceBlock(level, box, corner, maxX, y, minZ);
        placeCorniceBlock(level, box, corner, minX, y, maxZ);
        placeCorniceBlock(level, box, corner, maxX, y, maxZ);
    }

    /**
     * 垛口矮墙护栏。
     * placeBlock 用 setBlock(...,2) 不触发邻块更新，矮墙不会自己连边，因此必须按矩形几何显式设置连接方向，
     * 否则渲染成一串孤立小柱。
     */
    protected void buildParapet(WorldGenLevel level, BoundingBox box,
                                int centerX, int y, int centerZ,
                                Footprint footprint, int postSpacing,
                                BlockState wall, BlockState post) {
        int halfWidth = footprint.halfWidth();
        int halfDepth = footprint.halfDepth();
        int minX = centerX - halfWidth;
        int maxX = centerX + halfWidth;
        int minZ = centerZ - halfDepth;
        int maxZ = centerZ + halfDepth;
        int spacing = Math.max(2, postSpacing);

        for (int x = minX + 1; x <= maxX - 1; x++) {
            boolean pillar = Math.floorMod(x - minX, spacing) == 0;
            BlockState state = pillar ? post
                    : connectWall(wall, false, true, false, true, false);
            placeCorniceBlock(level, box, state, x, y, minZ);
            placeCorniceBlock(level, box, state, x, y, maxZ);
        }

        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            boolean pillar = Math.floorMod(z - minZ, spacing) == 0;
            BlockState state = pillar ? post
                    : connectWall(wall, true, false, true, false, false);
            placeCorniceBlock(level, box, state, minX, y, z);
            placeCorniceBlock(level, box, state, maxX, y, z);
        }

        placeCorniceBlock(level, box, connectWall(wall, false, true, true, false, true),
                minX, y, minZ);
        placeCorniceBlock(level, box, connectWall(wall, false, false, true, true, true),
                maxX, y, minZ);
        placeCorniceBlock(level, box, connectWall(wall, true, true, false, false, true),
                minX, y, maxZ);
        placeCorniceBlock(level, box, connectWall(wall, true, false, false, true, true),
                maxX, y, maxZ);
    }

    /** 显式设置矮墙的连接方向。传入非矮墙方块（例如整砖立柱）时原样返回，因此调用方可以混用矮墙与实心柱而不必分支。 */
    protected BlockState connectWall(BlockState wall, boolean north, boolean east,
                                     boolean south, boolean west, boolean up) {
        if (!wall.hasProperty(BlockStateProperties.NORTH_WALL)) {
            return wall;
        }
        return wall
                .setValue(BlockStateProperties.NORTH_WALL, north ? WallSide.LOW : WallSide.NONE)
                .setValue(BlockStateProperties.EAST_WALL, east ? WallSide.LOW : WallSide.NONE)
                .setValue(BlockStateProperties.SOUTH_WALL, south ? WallSide.LOW : WallSide.NONE)
                .setValue(BlockStateProperties.WEST_WALL, west ? WallSide.LOW : WallSide.NONE)
                .setValue(BlockStateProperties.UP, up);
    }

    protected BlockState orientStairs(BlockState stairs, Direction facing, Half half) {
        BlockState state = stairs;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, horizontal(facing));
        }
        if (state.hasProperty(BlockStateProperties.HALF)) {
            state = state.setValue(BlockStateProperties.HALF, half);
        }
        return state;
    }

    protected BlockState orientSlab(BlockState slab, boolean top) {
        if (slab.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return slab.setValue(BlockStateProperties.SLAB_TYPE,
                    top ? SlabType.TOP : SlabType.BOTTOM);
        }
        return slab;
    }

    /** 装饰构件单块放置。让行清单必须委托 isArenaFixture,另写一份会覆盖机关、真视箱与奖励祭坛。 */
    private void placeCorniceBlock(WorldGenLevel level, BoundingBox box,
                                   BlockState state, int x, int y, int z) {
        BlockState existing = this.getBlock(level, x, y, z, box);
        if (isArenaFixture(existing)) {
            return;
        }
        this.placeBlock(level, inheritWaterlogging(state, existing), x, y, z, box);
    }

    protected BlockState inheritWaterlogging(BlockState state, BlockState existing) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state;
        }
        boolean submerged = existing.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER);
        return state.setValue(BlockStateProperties.WATERLOGGED, submerged);
    }

    /** 尖顶收头。逐层收分到单点并在顶端点灯，让塔楼、尖塔与冠冕有明确的垂直终点而不是被平切。 */
    protected void buildSpireFinial(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ,
                                    int baseRadius, int height,
                                    BlockState body, BlockState accent, BlockState lamp) {
        int levels = Math.max(1, height);
        for (int layer = 0; layer < levels; layer++) {

            int radius = Math.max(0, baseRadius - baseRadius * layer / levels);
            int y = baseY + layer;
            if (radius == 0) {
                placeCorniceBlock(level, box, layer % 2 == 0 ? accent : body,
                        centerX, y, centerZ);
                continue;
            }
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    placeCorniceBlock(level, box, layer % 2 == 0 ? accent : body,
                            centerX + dx, y, centerZ + dz);
                }
            }
        }
        placeCorniceBlock(level, box, lamp, centerX, baseY + levels, centerZ);
    }

    protected void scatterArchitecturalDebris(WorldGenLevel level, RandomSource random, BoundingBox box,
                                               int centerX, int y, int centerZ,
                                               int minRadius, int maxRadius, int count,
                                               BlockState primary, BlockState secondary) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius + 1));
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);

            if (Math.abs(x - centerX) <= 2 || Math.abs(z - centerZ) <= 2
                    || !this.getBlock(level, x, y, z, box).isAir()) {
                continue;
            }
            int height = 1 + random.nextInt(3);
            architecturalPillar(level, box, x, y, z, height,
                    random.nextBoolean() ? primary : secondary);
        }
    }

    /**
     * 四踏板双闸门投影接力廊。
     * 外侧、中央和内侧均有踏板，保证玩家进入和离开时 都能通过重新投射残影控制对应闸门，
     * 不会因 30 秒寿命耗尽而永久受困。
     */
    protected void buildProjectionRelayGallery(WorldGenLevel level, RandomSource random, BoundingBox box,
                                                int centerX, int baseY, int centerZ, Direction forward,
                                                BlockState floor, BlockState wall, BlockState accent,
                                                ResourceKey<LootTable> loot) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        int halfLength = 15;
        int halfWidth = 4;
        int ceilingY = baseY + 6;

        for (int distance = -halfLength; distance <= halfLength; distance++) {
            for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                int x = localX(centerX, axis, side, distance, lateral);
                int z = localZ(centerZ, axis, side, distance, lateral);

                boolean vault = Math.abs(distance) <= 7;
                this.placeBlock(level, vault ? anchored
                                : Math.abs(lateral) == halfWidth || Math.floorMod(distance, 4) == 0
                                        ? accent : floor,
                        x, baseY, z, box);
                this.placeBlock(level, vault ? anchored
                                : distance % 4 == 0 && Math.abs(lateral) <= 1 ? accent : wall,
                        x, ceilingY, z, box);
                if (vault) {

                    this.placeBlock(level, anchored, x, baseY - 1, z, box);
                }
                for (int y = baseY + 1; y < ceilingY; y++) {
                    this.placeBlock(level, air, x, y, z, box);
                }
                if (Math.abs(lateral) == halfWidth) {
                    for (int y = baseY + 1; y < ceilingY; y++) {
                        this.placeBlock(level, vault ? anchored
                                        : distance % 4 == 0 || y == ceilingY - 1 ? accent : wall,
                                x, y, z, box);
                    }
                }
            }
        }

        for (int gateDistance : new int[]{-7, 7}) {
            buildRelayGate(level, box, centerX, baseY, centerZ, axis, side, gateDistance);
        }
        for (int plateDistance : new int[]{-11, -3, 3, 11}) {
            int x = localX(centerX, axis, side, plateDistance, 0);
            int z = localZ(centerZ, axis, side, plateDistance, 0);
            this.placeBlock(level, ModBlocks.PROJECTION_RESONANCE_PLATE.get().defaultBlockState(),
                    x, baseY + 1, z, box);
            for (int lateral : new int[]{-3, 3}) {
                int lampX = localX(centerX, axis, side, plateDistance, lateral);
                int lampZ = localZ(centerZ, axis, side, plateDistance, lateral);
                this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                        lampX, baseY + 3, lampZ, box);
            }
        }

        int chestX = localX(centerX, axis, side, 0, 0);
        int chestZ = localZ(centerZ, axis, side, 0, 0);
        this.placeBlock(level, air, chestX, baseY + 1, chestZ, box);
        this.placeBlock(level, air, chestX, baseY + 2, chestZ, box);
        this.createChest(level, box, random, chestX, baseY + 1, chestZ, loot);

        for (int entrance : new int[]{-halfLength, halfLength}) {
            int gateX = localX(centerX, axis, side, entrance, 0);
            int gateZ = localZ(centerZ, axis, side, entrance, 0);
            buildCardinalGateway(level, box, gateX, baseY + 1, gateZ, 0,
                    axis, 3, 4, wall, accent);
        }
    }

    /** 建造一个由四枚顺序符文控制的封印收藏室，答案只写入服务端 BlockEntity。 */
    protected void buildSequenceSeal(WorldGenLevel level, RandomSource random, BoundingBox box,
                                     int coreX, int baseY, int coreZ, Direction forward,
                                     BlockState floor, BlockState wall, BlockState accent,
                                     ResourceKey<LootTable> loot) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();

        for (int distance = -4; distance <= 9; distance++) {
            for (int lateral = -4; lateral <= 4; lateral++) {
                int x = localX(coreX, axis, side, distance, lateral);
                int z = localZ(coreZ, axis, side, distance, lateral);
                boolean vault = distance >= 5;

                this.placeBlock(level, vault ? anchored
                                : Math.abs(lateral) == 4 ? accent : floor,
                        x, baseY, z, box);

                if (vault) {
                    this.placeBlock(level, anchored, x, baseY - 1, z, box);
                }
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    this.placeBlock(level, air, x, y, z, box);
                }
                if (Math.abs(lateral) == 4) {

                    if (distance >= 4) {
                        for (int y = baseY + 1; y <= baseY + 4; y++) {
                            this.placeBlock(level, vault ? anchored
                                            : y == baseY + 4 ? accent : wall,
                                    x, y, z, box);
                        }
                    }
                }
                if (distance >= 4) {
                    this.placeBlock(level, vault ? anchored : wall, x, baseY + 5, z, box);
                }
            }
        }

        for (int lateral = -4; lateral <= 4; lateral++) {
            for (int y = baseY + 1; y <= baseY + 4; y++) {
                int gateX = localX(coreX, axis, side, 5, lateral);
                int gateZ = localZ(coreZ, axis, side, 5, lateral);
                boolean opening = Math.abs(lateral) <= 1 && y <= baseY + 3;
                this.placeBlock(level, opening ? sealed : anchored, gateX, y, gateZ, box);

                int backX = localX(coreX, axis, side, 9, lateral);
                int backZ = localZ(coreZ, axis, side, 9, lateral);
                this.placeBlock(level, anchored, backX, y, backZ, box);
            }
        }

        this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState(),
                coreX, baseY + 1, coreZ, box);
        int[] sequence = shuffledSequence(coreX, coreZ);
        BlockPos corePos = new BlockPos(this.getWorldX(coreX, coreZ),
                this.getWorldY(baseY + 1), this.getWorldZ(coreX, coreZ));
        if (box.isInside(corePos) && level.getBlockEntity(corePos) instanceof PuzzleCoreBlockEntity core) {
            core.setSequence(sequence);
        }

        int[][] runeOffsets = {{-3, -3}, {3, -3}, {-3, 2}, {3, 2}};
        for (int order = 0; order < runeOffsets.length; order++) {
            int distance = runeOffsets[order][1];
            int lateral = runeOffsets[order][0];
            int x = localX(coreX, axis, side, distance, lateral);
            int z = localZ(coreZ, axis, side, distance, lateral);
            architecturalPillar(level, box, x, baseY + 1, z, 2,
                    order % 2 == 0 ? wall : accent);
            this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                            .setValue(SequenceRuneBlock.ORDER, order),
                    x, baseY + 3, z, box);
        }
        for (int index = 0; index < sequence.length; index++) {
            int x = localX(coreX, axis, side, 5, index - 2);
            int z = localZ(coreZ, axis, side, 5, index - 2);
            this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                            .setValue(SequenceRuneBlock.ORDER, sequence[index])
                            .setValue(SequenceRuneBlock.LIT, Boolean.TRUE),
                    x, baseY + 4, z, box);
        }

        int chestX = localX(coreX, axis, side, 8, 0);
        int chestZ = localZ(coreZ, axis, side, 8, 0);
        this.placeBlock(level, air, chestX, baseY + 1, chestZ, box);
        this.placeBlock(level, air, chestX, baseY + 2, chestZ, box);
        this.createChest(level, box, random, chestX, baseY + 1, chestZ, loot);
    }

    protected void placeStructureSpawner(WorldGenLevel level, RandomSource random, BoundingBox box,
                                         int x, int y, int z, EntityType<?> entityType,
                                         BlockState floor, BlockState accent) {
        architecturalDisk(level, box, x, y - 1, z, 3, floor);
        architecturalRing(level, box, x, y, z, 3, accent);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    this.placeBlock(level, Blocks.AIR.defaultBlockState(), x + dx, y + dy, z + dz, box);
                }
            }
        }
        this.placeBlock(level, Blocks.SPAWNER.defaultBlockState(), x, y, z, box);
        BlockPos spawnerPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
        if (box.isInside(spawnerPos)
                && level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entityType, random);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int lampX = x + direction.getStepX() * 3;
            int lampZ = z + direction.getStepZ() * 3;
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                    lampX, y + 2, lampZ, box);
        }
    }

    /**
     * 潮池刷怪笼:水影等实体的生成规则要求脚下与头顶各一格水, 因此把刷怪笼沉入一座 5x5 围壁潮池,
     * 围壁同时防止水漫出结构。
     */
    protected void placeFloodedStructureSpawner(WorldGenLevel level, RandomSource random, BoundingBox box,
                                                int x, int y, int z, EntityType<?> entityType,
                                                BlockState basin, BlockState rim) {
        architecturalDisk(level, box, x, y - 1, z, 3, basin);
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int dy = 0; dy <= 1; dy++) {
                    this.placeBlock(level, edge ? rim : water, x + dx, y + dy, z + dz, box);
                }
                this.placeBlock(level, Blocks.AIR.defaultBlockState(), x + dx, y + 2, z + dz, box);
                this.placeBlock(level, Blocks.AIR.defaultBlockState(), x + dx, y + 3, z + dz, box);
            }
        }
        this.placeBlock(level, Blocks.SPAWNER.defaultBlockState(), x, y, z, box);
        BlockPos spawnerPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
        if (box.isInside(spawnerPos)
                && level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entityType, random);
        }
    }

    /**
     * 外圈城郭:一圈带垛口的矮墙 + 八座错开正门的卫塔,四个正向留门洞, 与神道/接力廊对齐,
     * 为地标补出连续的外沿步行路线。
     */
    protected void buildOuterBailey(WorldGenLevel level, BoundingBox box,
                                    int centerX, int baseY, int centerZ, int radius,
                                    BlockState wall, BlockState accent, BlockState lamp) {
        int outer = radius * radius;
        int innerRadius = radius - 1;
        int inner = innerRadius * innerRadius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int distance = dx * dx + dz * dz;
                if (distance > outer || distance < inner) {
                    continue;
                }

                if (Math.abs(dx) <= 4 || Math.abs(dz) <= 4) {
                    continue;
                }
                for (int dy = -1; dy <= 2; dy++) {
                    this.placeBlock(level, wall, x, baseY + dy, z, box);
                }
                if (Math.floorMod(x + z, 3) == 0) {
                    this.placeBlock(level, accent, x, baseY + 3, z, box);
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 8.0D + i * Math.PI / 4.0D;
            int towerX = centerX + (int) Math.round(Math.cos(angle) * radius);
            int towerZ = centerZ + (int) Math.round(Math.sin(angle) * radius);
            for (int dy = -1; dy <= 6; dy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        boolean corner = ox != 0 && oz != 0;
                        this.placeBlock(level, corner && dy >= 5 ? accent : wall,
                                towerX + ox, baseY + dy, towerZ + oz, box);
                    }
                }
            }
            this.placeBlock(level, lamp, towerX, baseY + 7, towerZ, box);
        }
    }

    /**
     * 紧凑投影密龛:7x7 锚定石小室,单侧投影共鸣门由踏板控制,门外门内各一枚踏板保证进出都能重新投射,
     * 适合中小体量结构。
     */
    protected void buildProjectionVaultNiche(WorldGenLevel level, RandomSource random, BoundingBox box,
                                             int doorX, int baseY, int doorZ, Direction inward,
                                             BlockState shellAccent, ResourceKey<LootTable> loot) {
        Direction axis = horizontal(inward);
        Direction side = axis.getClockWise();
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int distance = 0; distance <= 6; distance++) {
            for (int lateral = -3; lateral <= 3; lateral++) {
                int x = localX(doorX, axis, side, distance, lateral);
                int z = localZ(doorZ, axis, side, distance, lateral);
                this.placeBlock(level, anchored, x, baseY - 1, z, box);
                this.placeBlock(level, anchored, x, baseY + 3, z, box);
                boolean shell = distance == 0 || distance == 6 || Math.abs(lateral) == 3;
                for (int dy = 0; dy <= 2; dy++) {
                    if (distance == 0 && lateral == 0 && dy <= 1) {
                        continue;
                    }
                    this.placeBlock(level, shell ? anchored : air, x, baseY + dy, z, box);
                }
            }
        }
        BlockState door = ModBlocks.PROJECTION_RESONANCE_DOOR.get().defaultBlockState();
        this.placeBlock(level, door, doorX, baseY, doorZ, box);
        this.placeBlock(level, door, doorX, baseY + 1, doorZ, box);
        for (int plateDistance : new int[]{-2, 2}) {
            int x = localX(doorX, axis, side, plateDistance, 0);
            int z = localZ(doorZ, axis, side, plateDistance, 0);

            this.placeBlock(level, plateDistance < 0 ? shellAccent : anchored,
                    x, baseY - 1, z, box);
            if (plateDistance < 0) {
                this.placeBlock(level, air, x, baseY, z, box);
                this.placeBlock(level, air, x, baseY + 1, z, box);
            }
            this.placeBlock(level, ModBlocks.PROJECTION_RESONANCE_PLATE.get().defaultBlockState(),
                    x, baseY, z, box);
        }

        for (int lampDistance : new int[]{2, 4}) {
            this.placeBlock(level, ModBlocks.SKY_LAMP_GLASS.get().defaultBlockState(),
                    localX(doorX, axis, side, lampDistance, 0), baseY + 2,
                    localZ(doorZ, axis, side, lampDistance, 0), box);
        }
        for (int studDistance : new int[]{1, 3, 5}) {
            for (int lateral : new int[]{-2, 2}) {
                this.placeBlock(level, shellAccent,
                        localX(doorX, axis, side, studDistance, lateral), baseY + 2,
                        localZ(doorZ, axis, side, studDistance, lateral), box);
            }
        }
        int chestX = localX(doorX, axis, side, 5, 0);
        int chestZ = localZ(doorZ, axis, side, 5, 0);
        this.createChest(level, box, random, chestX, baseY, chestZ, loot);
    }

    /** 水下刷怪笼:不清空水体,只放刷怪笼与基座环,供内部本就浸水的结构使用。 */
    protected void placeSubmergedStructureSpawner(WorldGenLevel level, RandomSource random, BoundingBox box,
                                                  int x, int y, int z, EntityType<?> entityType,
                                                  BlockState floor, BlockState accent) {
        architecturalDisk(level, box, x, y - 1, z, 3, floor);
        architecturalRing(level, box, x, y - 1, z, 3, accent);
        this.placeBlock(level, Blocks.SPAWNER.defaultBlockState(), x, y, z, box);
        BlockPos spawnerPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
        if (box.isInside(spawnerPos)
                && level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entityType, random);
        }
    }

    /**
     * 音序共鸣庭:中央音序核心右键播放一遍钟音演示(五根音柱按服务端旋律逐个亮起), 玩家凭记忆按序敲击音柱;
     * 解开后核心前方的封印龛消散。观察记忆型谜题。
     */
    protected void buildChimeEchoCourt(WorldGenLevel level, RandomSource random, BoundingBox box,
                                       int coreX, int baseY, int coreZ, Direction forward,
                                       BlockState floor, BlockState wall, BlockState accent,
                                       ResourceKey<LootTable> loot) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();
        architecturalDisk(level, box, coreX, baseY, coreZ, 6, floor);
        architecturalRing(level, box, coreX, baseY, coreZ, 6, accent);
        for (int distance = -5; distance <= 5; distance++) {
            for (int lateral = -5; lateral <= 5; lateral++) {
                if (distance * distance + lateral * lateral > 34) {
                    continue;
                }
                int x = localX(coreX, axis, side, distance, lateral);
                int z = localZ(coreZ, axis, side, distance, lateral);
                for (int dy = 1; dy <= 4; dy++) {
                    this.placeBlock(level, air, x, baseY + dy, z, box);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            int lateral = (i - 2) * 2;
            int x = localX(coreX, axis, side, -3, lateral);
            int z = localZ(coreZ, axis, side, -3, lateral);
            this.placeBlock(level, wall, x, baseY + 1, z, box);
            this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                    .setValue(SequenceRuneBlock.ORDER, i), x, baseY + 2, z, box);
        }
        this.placeBlock(level, ModBlocks.CHIME_ECHO_CORE.get().defaultBlockState(),
                coreX, baseY + 1, coreZ, box);
        BlockPos corePos = new BlockPos(this.getWorldX(coreX, coreZ),
                this.getWorldY(baseY + 1), this.getWorldZ(coreX, coreZ));
        if (box.isInside(corePos)
                && level.getBlockEntity(corePos) instanceof PuzzleCoreBlockEntity core) {
            core.setSequence(shuffledOrders(coreX, coreZ, 5));
        }
        buildSealedVaultPocket(level, random, box, coreX, baseY, coreZ, axis, side, 3, loot);
    }

    /**
     * 对页归档室:八块对页书板两两同源,翻开时以尘色短暂显形; 全部配对归位后后墙封印龛消散。
     * 翻牌记忆型谜题,配对关系只存服务端。
     */
    protected void buildFoldpagePairRoom(WorldGenLevel level, RandomSource random, BoundingBox box,
                                         int coreX, int baseY, int coreZ, Direction forward,
                                         BlockState floor, BlockState wall, BlockState accent,
                                         ResourceKey<LootTable> loot) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int distance = -5; distance <= 3; distance++) {
            for (int lateral = -4; lateral <= 4; lateral++) {
                int x = localX(coreX, axis, side, distance, lateral);
                int z = localZ(coreZ, axis, side, distance, lateral);
                this.placeBlock(level, Math.abs(lateral) == 4 ? accent : floor, x, baseY, z, box);
                this.placeBlock(level, wall, x, baseY + 5, z, box);

                boolean shell = Math.abs(lateral) == 4 || distance == -5
                        || (distance == 3 && Math.abs(lateral) >= 3);
                boolean doorway = distance == -5 && Math.abs(lateral) <= 1;
                for (int dy = 1; dy <= 4; dy++) {
                    BlockState body = shell && !(doorway && dy <= 2)
                            ? (dy == 4 ? accent : wall) : air;
                    this.placeBlock(level, body, x, baseY + dy, z, box);
                }
            }
        }
        java.util.List<BlockPos> tilePositions = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int lateral = i < 4 ? -2 : 2;
            int distance = -4 + (i % 4) * 2;
            int x = localX(coreX, axis, side, distance, lateral);
            int z = localZ(coreZ, axis, side, distance, lateral);
            this.placeBlock(level, ModBlocks.FOLDPAGE_TILE.get().defaultBlockState(),
                    x, baseY + 1, z, box);
            tilePositions.add(new BlockPos(this.getWorldX(x, z),
                    this.getWorldY(baseY + 1), this.getWorldZ(x, z)));
        }
        this.placeBlock(level, ModBlocks.FOLDPAGE_PAIR_CORE.get().defaultBlockState(),
                coreX, baseY + 1, coreZ, box);
        BlockPos corePos = new BlockPos(this.getWorldX(coreX, coreZ),
                this.getWorldY(baseY + 1), this.getWorldZ(coreX, coreZ));
        if (box.isInside(corePos)
                && level.getBlockEntity(corePos) instanceof FoldpagePairCoreBlockEntity core) {
            core.configure(tilePositions, this.boundingBox.minX() * 341873128712L
                    ^ this.boundingBox.minZ() * 132897987541L ^ coreX * 92821L ^ coreZ * 68917L);
        }
        buildSealedVaultPocket(level, random, box, coreX, baseY, coreZ, axis, side, 4, loot);
    }

    /**
     * 步径圣所:开放式步庭内散布五枚步纹符文(点数固定),尽头壁画从左到右 给出踩踏顺序;
     * 按序走完全程,壁画下方的封印门消散。走位观察型谜题。
     */
    protected void buildStepPathSanctum(WorldGenLevel level, RandomSource random, BoundingBox box,
                                        int coreX, int baseY, int coreZ, Direction forward,
                                        BlockState floor, BlockState wall, BlockState accent,
                                        ResourceKey<LootTable> loot) {
        Direction axis = horizontal(forward);
        Direction side = axis.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int distance = -8; distance <= 0; distance++) {
            for (int lateral = -4; lateral <= 4; lateral++) {
                int x = localX(coreX, axis, side, distance, lateral);
                int z = localZ(coreZ, axis, side, distance, lateral);
                this.placeBlock(level, Math.abs(lateral) == 4 || distance == -8 ? accent : floor,
                        x, baseY, z, box);
                for (int dy = 1; dy <= 4; dy++) {
                    this.placeBlock(level, air, x, baseY + dy, z, box);
                }
            }
        }

        int[][] stepSpots = {{-3, -7}, {2, -6}, {-1, -4}, {3, -3}, {0, -2}};
        for (int i = 0; i < stepSpots.length; i++) {
            int x = localX(coreX, axis, side, stepSpots[i][1], stepSpots[i][0]);
            int z = localZ(coreZ, axis, side, stepSpots[i][1], stepSpots[i][0]);
            this.placeBlock(level, ModBlocks.STEP_RUNE.get().defaultBlockState()
                    .setValue(SequenceRuneBlock.ORDER, i), x, baseY + 1, z, box);
        }
        this.placeBlock(level, accent, coreX, baseY, coreZ, box);
        this.placeBlock(level, ModBlocks.PUZZLE_CORE.get().defaultBlockState(),
                coreX, baseY + 1, coreZ, box);
        int[] sequence = shuffledOrders(coreX, coreZ, 5);
        BlockPos corePos = new BlockPos(this.getWorldX(coreX, coreZ),
                this.getWorldY(baseY + 1), this.getWorldZ(coreX, coreZ));
        if (box.isInside(corePos)
                && level.getBlockEntity(corePos) instanceof PuzzleCoreBlockEntity core) {
            core.setSequence(sequence);
        }

        buildSealedVaultPocket(level, random, box, coreX, baseY, coreZ, axis, side, 1, loot);

        for (int i = 0; i < sequence.length; i++) {
            int x = localX(coreX, axis, side, 1, i - 2);
            int z = localZ(coreZ, axis, side, 1, i - 2);
            this.placeBlock(level, ModBlocks.SEQUENCE_RUNE.get().defaultBlockState()
                            .setValue(SequenceRuneBlock.ORDER, sequence[i])
                            .setValue(SequenceRuneBlock.LIT, Boolean.TRUE),
                    x, baseY + 4, z, box);
        }
    }

    /** 谜题核心前方的封印奖励龛:锚定石外壳,普通封印石门,由解谜清除。 */
    private void buildSealedVaultPocket(WorldGenLevel level, RandomSource random, BoundingBox box,
                                        int coreX, int baseY, int coreZ, Direction axis, Direction side,
                                        int doorDistance, ResourceKey<LootTable> loot) {
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        BlockState sealed = ModBlocks.SEALED_STONE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int distance = doorDistance; distance <= doorDistance + 3; distance++) {
            for (int lateral = -2; lateral <= 2; lateral++) {
                int x = localX(coreX, axis, side, distance, lateral);
                int z = localZ(coreZ, axis, side, distance, lateral);
                this.placeBlock(level, anchored, x, baseY, z, box);
                this.placeBlock(level, anchored, x, baseY + 4, z, box);
                boolean shell = distance == doorDistance || distance == doorDistance + 3
                        || Math.abs(lateral) == 2;
                boolean doorway = distance == doorDistance && Math.abs(lateral) <= 1;
                for (int dy = 1; dy <= 3; dy++) {
                    BlockState body = doorway && dy <= 2 ? sealed : shell ? anchored : air;
                    this.placeBlock(level, body, x, baseY + dy, z, box);
                }
            }
        }
        int chestX = localX(coreX, axis, side, doorDistance + 2, 0);
        int chestZ = localZ(coreZ, axis, side, doorDistance + 2, 0);
        this.createChest(level, box, random, chestX, baseY + 1, chestZ, loot);
    }

    private void buildRelayGate(WorldGenLevel level, BoundingBox box,
                                int centerX, int baseY, int centerZ,
                                Direction axis, Direction side, int distance) {
        BlockState anchored = ModBlocks.ANCHORED_SEALED_STONE.get().defaultBlockState();
        for (int lateral = -4; lateral <= 4; lateral++) {
            int x = localX(centerX, axis, side, distance, lateral);
            int z = localZ(centerZ, axis, side, distance, lateral);
            for (int y = baseY + 1; y <= baseY + 5; y++) {
                boolean doorOpening = (lateral == -1 || lateral == 0) && y <= baseY + 2;
                if (!doorOpening) {

                    this.placeBlock(level, anchored, x, y, z, box);
                }
            }
        }
        for (int lateral : new int[]{-1, 0}) {
            int x = localX(centerX, axis, side, distance, lateral);
            int z = localZ(centerZ, axis, side, distance, lateral);
            BlockState door = ModBlocks.PROJECTION_RESONANCE_DOOR.get().defaultBlockState();
            this.placeBlock(level, door, x, baseY + 1, z, box);
            this.placeBlock(level, door, x, baseY + 2, z, box);
        }
    }

    private int[] shuffledSequence(int localX, int localZ) {
        return shuffledOrders(localX, localZ, 4);
    }

    /** 由包围盒与局部坐标派生的确定性乱序排列(跨区块一致)。 */
    protected int[] shuffledOrders(int localX, int localZ, int count) {
        long seed = this.boundingBox.minX() * 341873128712L
                ^ this.boundingBox.minZ() * 132897987541L
                ^ localX * 42317861L ^ localZ * 374761393L;
        RandomSource random = RandomSource.create(seed);
        int[] sequence = new int[count];
        for (int i = 0; i < count; i++) {
            sequence[i] = i;
        }
        for (int i = sequence.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int value = sequence[i];
            sequence[i] = sequence[j];
            sequence[j] = value;
        }
        return sequence;
    }

    private static int localX(int centerX, Direction axis, Direction side,
                              int distance, int lateral) {
        return centerX + axis.getStepX() * distance + side.getStepX() * lateral;
    }

    private static int localZ(int centerZ, Direction axis, Direction side,
                              int distance, int lateral) {
        return centerZ + axis.getStepZ() * distance + side.getStepZ() * lateral;
    }

    protected void architecturalDisk(WorldGenLevel level, BoundingBox box,
                                      int centerX, int y, int centerZ, int radius, BlockState state) {
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz <= radiusSquared) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }
    }

    protected void architecturalRing(WorldGenLevel level, BoundingBox box,
                                      int centerX, int y, int centerZ, int radius, BlockState state) {
        int outer = radius * radius;
        int innerRadius = Math.max(0, radius - 1);
        int inner = innerRadius * innerRadius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int distance = dx * dx + dz * dz;
                if (distance <= outer && distance >= inner) {
                    this.placeBlock(level, state, x, y, z, box);
                }
            }
        }
    }

    protected void architecturalPillar(WorldGenLevel level, BoundingBox box,
                                        int x, int y, int z, int height, BlockState state) {
        for (int offset = 0; offset < height; offset++) {
            this.placeBlock(level, state, x, y + offset, z, box);
        }
    }

    /** 给已放置的真视宝箱播种战利品表。自己摆 TRUE_SIGHT_CHEST 的子类也必须调这里,否则掉落表引用漏改。 */
    protected void configureTrueSightVault(WorldGenLevel level, BoundingBox box, RandomSource random,
                                           int x, int y, int z) {
        BlockPos vaultPos = new BlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
        if (box.isInside(vaultPos) && level.getBlockEntity(vaultPos)
                instanceof cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity vault) {
            vault.setLootTable(ModLootTables.TRUE_SIGHT_VAULT, random.nextLong());
        }
    }

    protected void clearChestPocket(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    this.placeBlock(level, air, x + dx, y, z + dz, box);
                }
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            this.placeBlock(level, air, x, y + dy, z, box);
        }
    }

    private void buildHiddenVaultNiche(WorldGenLevel level, BoundingBox box,
                                       int x, int y, int z, Direction entranceSide) {
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
        placeHiddenDoor(level, box, hidden, entranceSide, x, y, z, minX, maxX, minZ, maxZ);
        clearVaultApproach(level, box, entranceSide, x, y, z);
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
        for (int yy = floorY + 1; yy < roofY; yy++) {
            for (int xx = minX; xx <= maxX; xx++) {
                this.placeBlock(level, anchored, xx, yy, minZ, box);
                this.placeBlock(level, anchored, xx, yy, maxZ, box);
            }
            for (int zz = minZ; zz <= maxZ; zz++) {
                this.placeBlock(level, anchored, minX, yy, zz, box);
                this.placeBlock(level, anchored, maxX, yy, zz, box);
            }
        }
    }

    private void placeHiddenDoor(WorldGenLevel level, BoundingBox box, BlockState hidden, Direction side,
                                 int x, int y, int z, int minX, int maxX, int minZ, int maxZ) {
        for (int yy = y; yy <= y + 2; yy++) {
            for (int offset = -1; offset <= 1; offset++) {
                switch (side) {
                    case NORTH -> this.placeBlock(level, hidden, x + offset, yy, minZ, box);
                    case SOUTH -> this.placeBlock(level, hidden, x + offset, yy, maxZ, box);
                    case WEST -> this.placeBlock(level, hidden, minX, yy, z + offset, box);
                    case EAST -> this.placeBlock(level, hidden, maxX, yy, z + offset, box);
                    default -> { }
                }
            }
        }
    }

    private void clearVaultApproach(WorldGenLevel level, BoundingBox box, Direction side, int x, int y, int z) {
        clearVaultApproach(level, box, side, x, y, z, 3, 4);
    }

    private void clearCompactVaultApproach(WorldGenLevel level, BoundingBox box, Direction side, int x, int y, int z) {
        clearVaultApproach(level, box, side, x, y, z, 2, 3);
    }

    private void clearVaultApproach(WorldGenLevel level, BoundingBox box, Direction side,
                                    int x, int y, int z, int firstStep, int lastStep) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int perpX = side.getStepZ();
        int perpZ = -side.getStepX();
        for (int step = firstStep; step <= lastStep; step++) {
            int baseX = x + side.getStepX() * step;
            int baseZ = z + side.getStepZ() * step;
            for (int offset = -1; offset <= 1; offset++) {
                for (int dy = 0; dy <= 2; dy++) {
                    this.placeBlock(level, air, baseX + perpX * offset, y + dy,
                            baseZ + perpZ * offset, box);
                }
            }
        }
        this.placeBlock(level, ModBlocks.ECHO_RUNE_BRICKS.get().defaultBlockState(),
                x + side.getStepX() * firstStep, Math.max(0, y - 1), z + side.getStepZ() * firstStep, box);
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.SOUTH;
    }
}
