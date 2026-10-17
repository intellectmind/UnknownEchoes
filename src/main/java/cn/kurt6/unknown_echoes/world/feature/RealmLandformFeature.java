package cn.kurt6.unknown_echoes.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RealmLandformFeature extends Feature<RealmLandformFeature.Configuration> {
    private static final TagKey<Block> NATURAL_GROUND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("unknown_echoes", "landform_ground"));

    public enum Shape implements StringRepresentable {
        ARCH("arch"), SPIRES("spires"), SHELVES("shelves"), FLOATING("floating"), CANOPY("canopy"),
        FINS("fins"), CRYSTAL_FAN("crystal_fan"), INVERTED("inverted");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Configuration(Shape shape, BlockState rock, BlockState accent, int height,
                                int radius, boolean submerged, int drape, boolean waterline) implements FeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StringRepresentable.fromEnum(Shape::values).fieldOf("shape").forGetter(Configuration::shape),
                BlockState.CODEC.fieldOf("rock").forGetter(Configuration::rock),
                BlockState.CODEC.fieldOf("accent").forGetter(Configuration::accent),
                Codec.intRange(6, 28).fieldOf("height").forGetter(Configuration::height),
                Codec.intRange(3, 10).fieldOf("radius").forGetter(Configuration::radius),
                Codec.BOOL.optionalFieldOf("submerged", false).forGetter(Configuration::submerged),
                Codec.intRange(0, 6).optionalFieldOf("drape", 0).forGetter(Configuration::drape),
                Codec.BOOL.optionalFieldOf("waterline", false).forGetter(Configuration::waterline)
        ).apply(instance, Configuration::new));
    }

    public RealmLandformFeature() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Configuration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Configuration config = context.config();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(origin.getX(), origin.getY() - 1, origin.getZ());
        if (level.isOutsideBuildHeight(origin) || level.isOutsideBuildHeight(cursor)
                || !level.ensureCanWrite(origin)) return false;
        if (!level.getBlockState(cursor).is(NATURAL_GROUND)) return false;
        BlockState atOrigin = level.getBlockState(origin);
        if (!openSpace(atOrigin, config) || config.submerged() && !atOrigin.is(Blocks.WATER)) return false;
        int margin = config.radius() + 2;
        // 仅访问装饰阶段可写的邻区块；结构引用同时保护空着的门庭和战斗净空。
        for (int x = (origin.getX() - margin) >> 4; x <= (origin.getX() + margin) >> 4; x++) {
            for (int z = (origin.getZ() - margin) >> 4; z <= (origin.getZ() + margin) >> 4; z++) {
                cursor.set(x << 4, origin.getY(), z << 4);
                if (!level.ensureCanWrite(cursor)) return false;
                var chunk = level.getChunk(x, z);
                for (var references : chunk.getAllReferences().values()) {
                    if (!references.isEmpty()) return false;
                }
                for (var start : chunk.getAllStarts().values()) {
                    if (start.isValid()) return false;
                }
            }
        }
        var blocks = buildLayout(config, context.random());
        var footings = new LongArrayList();
        for (var iterator = blocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
            var entry = iterator.next();
            long pos = entry.getLongKey();
            if (BlockPos.getY(pos) == -4) {
                footings.add(pos);
            }
        }
        if (!fitFootings(level, origin, config, blocks, footings)) return false;
        for (var iterator = blocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
            long local = iterator.next().getLongKey();
            int x = BlockPos.getX(local), y = BlockPos.getY(local), z = BlockPos.getZ(local);
            cursor.setWithOffset(origin, x, y, z);
            if (level.isOutsideBuildHeight(cursor) || !level.ensureCanWrite(cursor)) return false;
            BlockState existing = level.getBlockState(cursor);
            boolean empty = openSpace(existing, config);
            if (!empty && !(y <= 3 && blocks.containsKey(BlockPos.asLong(x, -4, z))
                    && existing.is(NATURAL_GROUND))) return false;
        }
        for (var iterator = blocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
            var entry = iterator.next();
            long local = entry.getLongKey();
            cursor.setWithOffset(origin, BlockPos.getX(local), BlockPos.getY(local), BlockPos.getZ(local));
            level.setBlock(cursor, entry.getValue(), 2);
        }
        return true;
    }

    private static boolean openSpace(BlockState state, Configuration config) {
        if (config.waterline()) return state.isAir() || state.is(Blocks.WATER);
        return config.submerged() ? state.is(Blocks.WATER) : state.isAir();
    }

    private static boolean fitFootings(WorldGenLevel level, BlockPos origin, Configuration config,
                                       Long2ObjectLinkedOpenHashMap<BlockState> blocks, LongArrayList footings) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // 只调整承重点的 -6..3 格高差；超出范围整处跳过，不削平山坡。
        // 承点取自布局的 -4 层；补脚只添加更低的方块，不能改变这层的列成员与材质。
        for (int i = 0; i < footings.size(); i++) {
            long footing = footings.getLong(i);
            int localX = BlockPos.getX(footing);
            int localZ = BlockPos.getZ(footing);
            BlockState material = blocks.get(footing);
            int x = origin.getX() + localX, z = origin.getZ() + localZ;
            cursor.set(x, origin.getY() + 4, z);
            if (level.isOutsideBuildHeight(cursor) || !level.ensureCanWrite(cursor)) return false;
            if (!openSpace(level.getBlockState(cursor), config)) return false;
            boolean supported = false;
            for (int y = 3; y >= -6; y--) {
                cursor.set(x, origin.getY() + y, z);
                if (level.isOutsideBuildHeight(cursor.getY() - 1) || !level.ensureCanWrite(cursor)) return false;
                BlockState state = level.getBlockState(cursor);
                if (state.is(NATURAL_GROUND)) {
                    cursor.move(Direction.DOWN);
                    if (!level.getBlockState(cursor).is(NATURAL_GROUND)) return false;
                    for (int fillY = y; fillY < -4; fillY++) {
                        blocks.put(BlockPos.asLong(localX, fillY, localZ), material);
                    }
                    supported = true;
                    break;
                }
                if (!openSpace(state, config)) return false;
            }
            if (!supported) return false;
        }
        return true;
    }

    public static Map<BlockPos, BlockState> layout(Configuration config, RandomSource random) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        for (var entry : buildLayout(config, random).long2ObjectEntrySet()) {
            result.put(BlockPos.of(entry.getLongKey()), entry.getValue());
        }
        return result;
    }

    private static Long2ObjectLinkedOpenHashMap<BlockState> buildLayout(Configuration config, RandomSource random) {
        var blocks = new Long2ObjectLinkedOpenHashMap<BlockState>(128);
        int height = config.height() + random.nextInt(5) - 2;
        int radius = config.radius();
        int turn = random.nextInt(4);
        switch (config.shape()) {
            case ARCH -> arch(blocks, turn, height, radius, config, random);
            case SPIRES -> spires(blocks, turn, height, radius, config, random);
            case SHELVES -> shelves(blocks, turn, height, radius, config, random);
            case FLOATING -> floating(blocks, turn, height, radius, config, random);
            case CANOPY -> canopy(blocks, turn, height, radius, config, random);
            case FINS -> fins(blocks, turn, height, radius, config, random);
            case CRYSTAL_FAN -> crystalFan(blocks, turn, height, radius, config, random);
            case INVERTED -> inverted(blocks, turn, height, radius, config, random);
        }
        return blocks;
    }

    private static void arch(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                             Configuration config, RandomSource random) {
        int[] tops = new int[radius * 2 + 1];
        double lean = random.nextDouble() * 0.32 - 0.16;
        double curvePower = 0.52 + random.nextDouble() * 0.2;
        int vein = random.nextInt(11);
        for (int x = -radius; x <= radius; x++) {
            double curve = Math.pow(Math.max(0, 1.0 - (double) x * x / (radius * radius)), curvePower);
            tops[x + radius] = (int) Math.round(height * curve * (1 + lean * x / radius));
        }
        for (int x = -radius; x <= radius; x++) {
            int top = tops[x + radius];
            int neighborTop = Math.min(tops[Math.max(0, x + radius - 1)], tops[Math.min(radius * 2, x + radius + 1)]);
            int bottom = Math.abs(x) >= radius - 1 ? -4 : Math.max(0, Math.min(top - 3, neighborTop - 1));
            int width = Math.abs(x) >= radius - 2 ? 2 : 1;
            int bend = (int) Math.round(1.5 * Math.sin(x * Math.PI / radius));
            for (int y = bottom; y <= top; y++) for (int z = -width; z <= width; z++) {
                if (y > 4 && Math.abs(x) >= radius - 2 && Math.abs(z) == 2) continue;
                if (Math.abs(x) < radius - 1 && Math.abs(z) == width && (y == top || y == bottom)) continue;
                boolean seam = y > 2 && top - y == 1 && Math.floorMod(x + vein, 6) < 3;
                put(blocks, turn, x, y, z + bend, seam ? config.accent() : config.rock());
            }
        }
    }

    private static void spires(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                               Configuration config, RandomSource random) {
        taperedSpire(blocks, turn, 0, 0, height, Math.max(2, radius / 3),
                random.nextInt(5) - 2, random.nextInt(3) - 1, config);
        double phase = random.nextDouble() * Math.PI;
        for (int i = 0; i < 3; i++) {
            double angle = phase + i * Math.PI * 2 / 3;
            int x = (int) Math.round(Math.cos(angle) * (radius - 3));
            int z = (int) Math.round(Math.sin(angle) * (radius - 3));
            int peak = Math.max(4, height * (4 + random.nextInt(4)) / 10);
            taperedSpire(blocks, turn, x, z, peak, 2, Integer.signum(x), Integer.signum(z), config);
        }
    }

    private static void taperedSpire(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int centerX, int centerZ,
                                     int height, int radius, int leanX, int leanZ, Configuration config) {
        for (int y = -4; y <= height; y++) {
            double t = Math.max(0, y) / (double) height;
            int driftX = (int) Math.round(leanX * t);
            int driftZ = (int) Math.round(leanZ * t);
            double width = Math.max(0.65, radius * (1 - t) + 0.35 * Math.sin(t * Math.PI * 3));
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) + Math.abs(z) > width) continue;
                put(blocks, turn, centerX + driftX + x, y, centerZ + driftZ + z,
                        y >= height - 1 ? config.accent() : config.rock());
            }
            // 倾斜尖端仍须面相连，不能只靠对角相邻悬接。
            if (y > 0) {
                int previousX = (int) Math.round(leanX * (y - 1.0) / height);
                int previousZ = (int) Math.round(leanZ * (y - 1.0) / height);
                line(blocks, turn, centerX + previousX, y, centerZ + previousZ,
                        centerX + driftX, y, centerZ + driftZ, y >= height - 1 ? config.accent() : config.rock());
            }
        }
    }

    private static void shelves(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                                Configuration config, RandomSource random) {
        for (int y = -4; y <= height; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            put(blocks, turn, x, y, z, config.rock());
        }
        for (int tier = 0; tier < 4; tier++) {
            int y = Math.max(5, height * (tier + 1) / 4);
            int size = Math.max(2, radius - tier / 2);
            int shiftX = random.nextInt(5) - 2;
            int shiftZ = random.nextInt(3) - 1;
            for (int x = -size; x <= size; x++) for (int z = -size; z <= size; z++) {
                if (Math.abs(x) + Math.abs(z) > size * 1.45 || x * x + z * z > size * size) continue;
                boolean rim = x * x + z * z > (size - 1) * (size - 1);
                int lift = Math.floorDiv(x + z + tier, Math.max(3, size));
                put(blocks, turn, x + shiftX, y + lift - 1, z + shiftZ, config.rock());
                put(blocks, turn, x + shiftX, y + lift, z + shiftZ, rim ? config.rock() : config.accent());
            }
        }
    }

    private static void floating(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                                 Configuration config, RandomSource random) {
        double phase = random.nextDouble() * Math.PI * 2;
        for (int shard = 0; shard < 3; shard++) {
            double angle = phase + shard * Math.PI * 2 / 3;
            int centerX = (int) Math.round(Math.cos(angle) * (radius - 3));
            int centerZ = (int) Math.round(Math.sin(angle) * (radius - 3));
            int centerY = Math.max(10, height - shard * 4 + random.nextInt(3) - 1);
            int size = shard == 0 ? 3 : 2;
            int tail = 5 + random.nextInt(3);
            int lean = random.nextBoolean() ? 1 : -1;
            for (int y = -tail; y <= 3; y++) {
                double width = size * (1.0 - Math.abs(y) / (y < 0 ? tail + 1.0 : 4.0));
                int bend = y < -2 ? lean : 0;
                for (int x = -size; x <= size; x++) for (int z = -size; z <= size; z++) {
                    if (Math.abs(x) + Math.abs(z) > width) continue;
                    if (y > 0 && (x != 0 || z != 0) && y > (x + z > 0 ? 1 : 2)) continue;
                    put(blocks, turn, centerX + x + bend, centerY + y, centerZ + z,
                            y == 1 && Math.abs(x) + Math.abs(z) < size ? config.accent() : config.rock());
                }
                if (y == -3) put(blocks, turn, centerX, centerY + y, centerZ, config.rock());
            }
        }
    }

    private static void fins(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                             Configuration config, RandomSource random) {
        for (int fin = 0; fin < 3; fin++) {
            int centerX = (1 - fin) * 2;
            int centerZ = (fin - 1) * 4;
            int peak = fin == 1 ? height : height * (6 + random.nextInt(3)) / 10;
            int lean = 2 + random.nextInt(2);
            for (int y = -4; y <= peak; y++) {
                double t = Math.max(0, y) / (double) peak;
                int drift = y <= 3 ? 0 : (int) Math.round(lean * t);
                double sail = y <= 3 ? 1 : Math.max(0, Math.sin(Math.PI * t)) * (radius - 3) / 2.0 + 0.6;
                int span = (int) Math.floor(sail);
                int width = t < 0.7 ? 1 : 0;
                for (int x = -span; x <= span; x++) for (int z = -width; z <= width; z++) {
                    if (y > 4 && Math.abs(x) == span && z != 0 && (y + fin) % 4 == 0) continue;
                    boolean seam = y > 3 && Math.floorMod(y + x / 2 + fin * 3, 9) == 0;
                    put(blocks, turn, centerX + drift + x, y, centerZ + z, seam ? config.accent() : config.rock());
                }
                if (y > 0) {
                    int previous = y <= 4 ? 0 : (int) Math.round(lean * (y - 1.0) / peak);
                    line(blocks, turn, centerX + previous, y, centerZ, centerX + drift, y, centerZ, config.rock());
                }
            }
        }
    }

    private static void crystalFan(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                                   Configuration config, RandomSource random) {
        taperedSpire(blocks, turn, 0, 0, height, 2, 0, 0, config);
        double phase = random.nextDouble() * Math.PI * 2;
        for (int branch = 0; branch < 5; branch++) {
            double angle = phase + branch * Math.PI * 2 / 5;
            int endX = (int) Math.round(Math.cos(angle) * (radius - 1));
            int endZ = (int) Math.round(Math.sin(angle) * (radius - 1));
            int startY = Math.min(3 + branch % 3, Math.max(0, height - 3));
            int endY = Math.max(startY + 2, height * (6 + random.nextInt(3)) / 10);
            int previousX = 0, previousZ = 0;
            for (int y = startY; y <= endY; y++) {
                double t = (y - startY) / (double) (endY - startY);
                int x = (int) Math.round(endX * t), z = (int) Math.round(endZ * t);
                int width = t < 0.5 ? 2 : t < 0.88 ? 1 : 0;
                BlockState material = t > 0.65 ? config.accent() : config.rock();
                for (int dx = -width; dx <= width; dx++) for (int dz = -width; dz <= width; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= width) put(blocks, turn, x + dx, y, z + dz, material);
                }
                line(blocks, turn, previousX, y, previousZ, x, y, z, material);
                previousX = x; previousZ = z;
            }
        }
    }

    private static void inverted(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                                 Configuration config, RandomSource random) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            double edge = x * x / (double) (radius * radius) + z * z / (double) ((radius - 1) * (radius - 1));
            edge += 0.09 * Math.sin(x * 0.9 + z * 0.5) + 0.05 * Math.cos(z * 1.4);
            if (edge > 1) continue;
            int crown = (int) Math.round(3 * (1 - edge));
            for (int y = -2; y <= crown; y++) {
                put(blocks, turn, x, height + y, z, y == 0 && Math.floorMod(x + z, 7) < 3 ? config.accent() : config.rock());
            }
        }
        double phase = random.nextDouble() * Math.PI * 2;
        for (int point = 0; point < 5; point++) {
            double angle = phase + point * Math.PI / 2;
            int x = point == 4 ? 0 : (int) Math.round(Math.cos(angle) * (radius - 4));
            int z = point == 4 ? 0 : (int) Math.round(Math.sin(angle) * (radius - 4));
            int length = Math.max(3, height * (point == 4 ? 3 : 2) / 5 + random.nextInt(3));
            int size = point == 4 ? 3 : 2;
            for (int down = 2; down <= length; down++) {
                double width = size * (1 - (down - 2.0) / (length - 1));
                for (int dx = -size; dx <= size; dx++) for (int dz = -size; dz <= size; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > width) continue;
                    put(blocks, turn, x + dx, height - down, z + dz, down >= length - 1 ? config.accent() : config.rock());
                }
            }
        }
    }

    private static void canopy(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int height, int radius,
                               Configuration config, RandomSource random) {
        for (int y = -4; y < height - 1; y++) {
            int width = y < 2 ? 2 : 1;
            for (int x = -width; x <= width; x++) for (int z = -width; z <= width; z++) {
                if (Math.abs(x) + Math.abs(z) <= width) put(blocks, turn, x, y, z, config.rock());
            }
        }
        int rootLength = Math.min(4, radius - 1);
        for (int branch = 0; branch < 4; branch++) {
            for (int step = 2; step <= rootLength; step++) {
                int x = branch < 2 ? (branch == 0 ? step : -step) : 0;
                int z = branch >= 2 ? (branch == 2 ? step : -step) : 0;
                for (int y = -4; y <= rootLength - step; y++) put(blocks, turn, x, y, z, config.rock());
            }
        }
        double phase = random.nextDouble() * Math.PI * 2;
        for (int branch = 0; branch < 5; branch++) {
            double angle = phase + branch * Math.PI * 2 / 5;
            int reach = Math.max(1, radius - 4);
            int centerX = branch == 0 ? 0 : (int) Math.round(Math.cos(angle) * reach);
            int centerZ = branch == 0 ? 0 : (int) Math.round(Math.sin(angle) * reach);
            int centerY = branch == 0 ? height : Math.max(3, height - 3 - random.nextInt(4));
            int crown = Math.max(2, radius / 2 + (branch == 0 ? 1 : 0));
            for (int x = -crown; x <= crown; x++) for (int z = -crown; z <= crown; z++) {
                for (int y = -2; y <= 3; y++) {
                    double edge = x * x / (double) (crown * crown) + z * z / (double) (crown * crown) + y * y / 10.0;
                    if (edge > 1 || edge > 0.8 && Math.floorMod(x + z + branch, 3) == 0) continue;
                    put(blocks, turn, centerX + x, centerY + y, centerZ + z, config.accent());
                }
            }
            line(blocks, turn, 0, centerY - 2, 0, centerX, centerY - 2, centerZ, config.rock());
            line(blocks, turn, centerX, centerY - 2, centerZ, centerX, centerY, centerZ, config.rock());
        }
        if (config.drape() > 0) {
            var drapeCandidates = new LongArrayList();
            for (var iterator = blocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
                var entry = iterator.next();
                long leaf = entry.getLongKey();
                if (entry.getValue().equals(config.accent())
                        && !blocks.containsKey(BlockPos.offset(leaf, 0, -1, 0))) drapeCandidates.add(leaf);
            }
            // 补垂叶会修改布局；只截取初始叶底，仍按顺序复查下方空位。
            for (int candidate = 0; candidate < drapeCandidates.size(); candidate++) {
                long leaf = drapeCandidates.getLong(candidate);
                if (blocks.containsKey(BlockPos.offset(leaf, 0, -1, 0)) || random.nextInt(7) != 0) continue;
                int length = 1 + random.nextInt(config.drape());
                for (int i = 1; i <= length && i < BlockPos.getY(leaf); i++) {
                    long hanging = BlockPos.offset(leaf, 0, -i, 0);
                    if (blocks.containsKey(hanging)) break;
                    blocks.put(hanging, config.accent());
                }
            }
        }
    }

    private static void line(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int x, int y, int z,
                             int endX, int endY, int endZ, BlockState state) {
        put(blocks, turn, x, y, z, state);
        while (x != endX || y != endY || z != endZ) {
            if (x != endX) {
                x += Integer.signum(endX - x);
                put(blocks, turn, x, y, z, axis(state, Direction.Axis.X));
            }
            if (z != endZ) {
                z += Integer.signum(endZ - z);
                put(blocks, turn, x, y, z, axis(state, Direction.Axis.Z));
            }
            if (y != endY) {
                y += Integer.signum(endY - y);
                put(blocks, turn, x, y, z, axis(state, Direction.Axis.Y));
            }
        }
    }

    private static BlockState axis(BlockState state, Direction.Axis axis) {
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
    }

    private static void put(Long2ObjectLinkedOpenHashMap<BlockState> blocks, int turn, int x, int y, int z, BlockState state) {
        int rotatedX = switch (turn) { case 1 -> -z; case 2 -> -x; case 3 -> z; default -> x; };
        int rotatedZ = switch (turn) { case 1 -> x; case 2 -> -z; case 3 -> -x; default -> z; };
        if (turn % 2 != 0 && state.hasProperty(RotatedPillarBlock.AXIS)) {
            Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
            if (axis != Direction.Axis.Y) state = axis(state, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        blocks.put(BlockPos.asLong(rotatedX, y, rotatedZ), state);
    }
}
