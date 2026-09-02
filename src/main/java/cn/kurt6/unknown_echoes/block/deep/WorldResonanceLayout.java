package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public final class WorldResonanceLayout {
    public static final int GRAND_SIZE = 81;
    public static final int GRAND_HEIGHT = 36;
    public static final int GRAND_CENTER = GRAND_SIZE / 2;
    public static final int RITUAL_DURATION_TICKS = 180;

    private static final double GRAND_ARENA_RADIUS = 32.0D;
    private static final double LEGACY_ARENA_RADIUS = 19.0D;
    private static final List<BlockPos> GRAND_PYLON_OFFSETS = List.of(
            new BlockPos(0, 0, -27),
            new BlockPos(26, 0, -8),
            new BlockPos(16, 0, 22),
            new BlockPos(-16, 0, 22),
            new BlockPos(-26, 0, -8));
    private static final List<BlockPos> LEGACY_PYLON_OFFSETS = List.of(
            new BlockPos(0, 0, -17),
            new BlockPos(16, 0, -5),
            new BlockPos(10, 0, 14),
            new BlockPos(-10, 0, 14),
            new BlockPos(-16, 0, -5));

    private WorldResonanceLayout() {
    }

    public static double arenaRadius(boolean grand) {
        return grand ? GRAND_ARENA_RADIUS : LEGACY_ARENA_RADIUS;
    }

    public static int pylonTopOffset(boolean grand) {
        return grand ? 17 : 9;
    }

    public static List<BlockPos> pylonPositions(BlockPos altarPos, Direction facing, boolean grand) {
        List<BlockPos> offsets = grand ? GRAND_PYLON_OFFSETS : LEGACY_PYLON_OFFSETS;
        return offsets.stream()
                .map(offset -> altarPos.offset(rotate(offset, facing)))
                .toList();
    }

    private static BlockPos rotate(BlockPos offset, Direction facing) {
        int x = offset.getX();
        int z = offset.getZ();
        return switch (facing) {
            case EAST -> new BlockPos(-z, offset.getY(), x);
            case SOUTH -> new BlockPos(-x, offset.getY(), -z);
            case WEST -> new BlockPos(z, offset.getY(), -x);
            default -> offset;
        };
    }
}
