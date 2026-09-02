package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.block.CommunalEchoTableBlock;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/** 将三域料理的共享效果传递给附近同域栖鸣台与既有生灵。 */
public final class RealmCommunalFeastManager {
    public static final int CHIME_LINK_RADIUS = 24;
    public static final int CHIME_VERTICAL_RADIUS = 32;
    public static final int MAX_FEAST_RESIDENTS = 6;

    private RealmCommunalFeastManager() {
    }

    public static FeastResult shareFeast(ServerLevel level, BlockPos tablePos,
                                         CommunalEchoTableBlock.Mode feastMode) {
        RealmHabitatChimeBlock.Mode expectedMode = modeForFeast(feastMode);
        if (expectedMode == null
                || RealmHabitatChimeBlock.modeForDimension(level) != expectedMode) {
            return FeastResult.NONE;
        }

        BlockPos chimePos = findNearestChime(level, tablePos, expectedMode);
        if (chimePos == null) {
            return FeastResult.NONE;
        }

        List<PathfinderMob> residents = level.getEntitiesOfClass(PathfinderMob.class,
                new AABB(chimePos).inflate(RealmHabitatChimeBlock.CALL_RADIUS),
                resident -> resident.isAlive() && resident.getTarget() == null
                        && !resident.isLeashed()
                        && !resident.isPassenger() && !resident.isVehicle()
                        && resident.distanceToSqr(chimePos.getX() + 0.5D,
                                chimePos.getY() + 0.5D, chimePos.getZ() + 0.5D)
                                <= RealmHabitatChimeBlock.CALL_RADIUS
                                * RealmHabitatChimeBlock.CALL_RADIUS
                        && RealmHabitatChimeBlock.matchesMode(resident, expectedMode));
        residents.sort(Comparator.comparingDouble(resident -> resident.distanceToSqr(
                chimePos.getX() + 0.5D, chimePos.getY() + 0.5D,
                chimePos.getZ() + 0.5D)));
        if (residents.size() > MAX_FEAST_RESIDENTS) {
            residents = residents.subList(0, MAX_FEAST_RESIDENTS);
        }

        int responders = RealmHabitatChimeBlock.activateFromFacility(
                level, chimePos, expectedMode, residents, resident -> resident.heal(4.0F));
        RealmFacilityCircuitManager.arm(level, tablePos, chimePos, feastMode);
        return new FeastResult(responders, true);
    }

    private static RealmHabitatChimeBlock.Mode modeForFeast(
            CommunalEchoTableBlock.Mode feastMode) {
        return switch (feastMode) {
            case FAR_BELL -> RealmHabitatChimeBlock.Mode.WAYFARER;
            case CANOPY -> RealmHabitatChimeBlock.Mode.CONFLUENCE;
            case INTERLAYER -> RealmHabitatChimeBlock.Mode.INTERLAYER;
            case IDLE -> null;
        };
    }

    private static BlockPos findNearestChime(ServerLevel level, BlockPos tablePos,
                                             RealmHabitatChimeBlock.Mode expectedMode) {
        BlockPos builtInChimePos = builtInChimePos(tablePos, expectedMode);
        if (isAvailableChime(level, builtInChimePos, expectedMode)) {
            return builtInChimePos;
        }

        BlockPos nearest = null;
        int nearestDistanceSqr = Integer.MAX_VALUE;
        int horizontalRadiusSqr = CHIME_LINK_RADIUS * CHIME_LINK_RADIUS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int yOffset = -CHIME_VERTICAL_RADIUS;
             yOffset <= CHIME_VERTICAL_RADIUS; yOffset++) {
            for (int xOffset = -CHIME_LINK_RADIUS;
                 xOffset <= CHIME_LINK_RADIUS; xOffset++) {
                for (int zOffset = -CHIME_LINK_RADIUS;
                     zOffset <= CHIME_LINK_RADIUS; zOffset++) {
                    int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
                    if (horizontalDistanceSqr > horizontalRadiusSqr) {
                        continue;
                    }
                    cursor.set(tablePos.getX() + xOffset, tablePos.getY() + yOffset,
                            tablePos.getZ() + zOffset);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!isAvailableChime(state, expectedMode)) {
                        continue;
                    }
                    int distanceSqr = horizontalDistanceSqr + yOffset * yOffset;
                    if (distanceSqr < nearestDistanceSqr) {
                        nearestDistanceSqr = distanceSqr;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static BlockPos builtInChimePos(BlockPos tablePos,
                                            RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> tablePos.offset(9, -4, -15);
            case CONFLUENCE -> tablePos.offset(-8, 23, -8);
            case INTERLAYER -> tablePos.offset(-9, 0, -9);
        };
    }

    private static boolean isAvailableChime(ServerLevel level, BlockPos pos,
                                            RealmHabitatChimeBlock.Mode expectedMode) {
        return level.hasChunkAt(pos)
                && isAvailableChime(level.getBlockState(pos), expectedMode);
    }

    private static boolean isAvailableChime(BlockState state,
                                            RealmHabitatChimeBlock.Mode expectedMode) {
        return state.getBlock() instanceof RealmHabitatChimeBlock
                && state.getValue(RealmHabitatChimeBlock.MODE) == expectedMode
                && !state.getValue(RealmHabitatChimeBlock.ACTIVE);
    }

    public record FeastResult(int residents, boolean chimeActivated) {
        public static final FeastResult NONE = new FeastResult(0, false);
    }
}
