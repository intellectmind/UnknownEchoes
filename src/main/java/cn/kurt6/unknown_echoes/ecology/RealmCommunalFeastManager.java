package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.block.CommunalEchoTableBlock;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 将三域料理的共享效果传递给附近同域栖鸣台与既有生灵。 */
public final class RealmCommunalFeastManager {
    public static final int CHIME_LINK_RADIUS = 24;
    public static final int CHIME_VERTICAL_RADIUS = 32;
    public static final int MAX_FEAST_RESIDENTS = RealmHabitatChimeBlock.MAX_RESPONDERS;

    private RealmCommunalFeastManager() {
    }

    public static FeastResult shareFeast(ServerLevel level, BlockPos tablePos,
                                         CommunalEchoTableBlock.Mode feastMode) {
        RealmHabitatChimeBlock.Mode expectedMode = modeForFeast(feastMode);
        if (expectedMode == null
                || RealmHabitatChimeBlock.modeForDimension(level) != expectedMode
                || !isCurrentTable(level, tablePos, feastMode)) {
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
        // 栖鸣台按成功响应数限制名额，保留候选供导航失败或资格失效后补选。
        var activation = RealmHabitatChimeBlock.activateWithResidents(
                level, chimePos, expectedMode, residents, resident -> resident.heal(4.0F));
        if (!activation.active() || !isCurrentTable(level, tablePos, feastMode)) {
            return new FeastResult(activation.responders(), false);
        }
        RealmFacilityCircuitManager.arm(level, tablePos, chimePos, feastMode);
        return new FeastResult(activation.responders(), true);
    }

    private static boolean isCurrentTable(ServerLevel level, BlockPos pos, CommunalEchoTableBlock.Mode mode) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof CommunalEchoTableBlock
                && state.getValue(CommunalEchoTableBlock.MODE) == mode;
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
        return RealmChimeLookup.findNearest(level, tablePos, expectedMode, CHIME_LINK_RADIUS, CHIME_VERTICAL_RADIUS);
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
