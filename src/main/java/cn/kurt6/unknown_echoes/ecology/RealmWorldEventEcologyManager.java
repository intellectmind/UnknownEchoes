package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.worldevent.WorldEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public final class RealmWorldEventEcologyManager {
    public static final double RESPONSE_RADIUS = 24.0D;
    public static final int CHIME_SEARCH_RADIUS = 16;
    public static final int CHIME_VERTICAL_RADIUS = 6;
    public static final int MAX_RESPONDERS = 6;

    private RealmWorldEventEcologyManager() {
    }

    public static boolean supports(WorldEventType type) {
        return modeForEvent(type) != null;
    }

    public static int respond(ServerLevel level, WorldEventType type, BlockPos center) {
        RealmHabitatChimeBlock.Mode mode = modeForEvent(type);
        if (mode == null || RealmHabitatChimeBlock.modeForDimension(level) != mode) {
            return 0;
        }

        List<PathfinderMob> residents = level.getEntitiesOfClass(PathfinderMob.class,
                new AABB(center).inflate(RESPONSE_RADIUS, 8.0D, RESPONSE_RADIUS),
                resident -> resident.isAlive() && resident.getTarget() == null
                        && !resident.isLeashed()
                        && !resident.isPassenger() && !resident.isVehicle()
                        && !RealmLivingTrailManager.hasActiveTrail(resident)
                        && resident.distanceToSqr(center.getX() + 0.5D,
                                resident.getY(), center.getZ() + 0.5D)
                                <= RESPONSE_RADIUS * RESPONSE_RADIUS
                        && RealmHabitatChimeBlock.matchesMode(resident, mode));
        residents.sort(Comparator.comparingDouble(
                resident -> resident.distanceToSqr(center.getX() + 0.5D,
                        center.getY() + 0.5D, center.getZ() + 0.5D)));

        BlockPos chimePos = findNearestChime(level, center, mode);
        if (chimePos != null) {
            int chimeResponders = RealmHabitatChimeBlock.activateFromFacility(
                    level, chimePos, mode, residents,
                    resident -> presentResidentResponse(level, resident, mode));
            if (chimeResponders > 0) {
                return chimeResponders;
            }
        }
        return guideToEvent(level, center, mode, residents);
    }

    private static int guideToEvent(ServerLevel level, BlockPos center,
                                    RealmHabitatChimeBlock.Mode mode,
                                    List<PathfinderMob> residents) {
        int moved = 0;
        for (PathfinderMob resident : residents) {
            if (moved >= MAX_RESPONDERS) {
                break;
            }
            double angle = moved * Math.PI * 2.0D / MAX_RESPONDERS;
            double radius = responseRingRadius(mode);
            double targetX = center.getX() + 0.5D + Math.cos(angle) * radius;
            double targetZ = center.getZ() + 0.5D + Math.sin(angle) * radius;
            BlockPos target = BlockPos.containing(targetX, center.getY(), targetZ);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            resident.getLookControl().setLookAt(center.getX() + 0.5D,
                    center.getY() + 0.8D, center.getZ() + 0.5D);
            if (resident.getNavigation().moveTo(targetX, center.getY(), targetZ,
                    responseSpeed(mode))) {
                presentResidentResponse(level, resident, mode);
                moved++;
            }
        }
        if (moved > 0) {
            playResponse(level, center, mode);
        }
        return moved;
    }

    private static BlockPos findNearestChime(ServerLevel level, BlockPos center,
                                             RealmHabitatChimeBlock.Mode mode) {
        BlockPos nearest = null;
        int nearestDistanceSqr = Integer.MAX_VALUE;
        int horizontalRadiusSqr = CHIME_SEARCH_RADIUS * CHIME_SEARCH_RADIUS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int yOffset = -CHIME_VERTICAL_RADIUS;
             yOffset <= CHIME_VERTICAL_RADIUS; yOffset++) {
            for (int xOffset = -CHIME_SEARCH_RADIUS;
                 xOffset <= CHIME_SEARCH_RADIUS; xOffset++) {
                for (int zOffset = -CHIME_SEARCH_RADIUS;
                     zOffset <= CHIME_SEARCH_RADIUS; zOffset++) {
                    int horizontalDistanceSqr = xOffset * xOffset + zOffset * zOffset;
                    if (horizontalDistanceSqr > horizontalRadiusSqr) {
                        continue;
                    }
                    cursor.set(center.getX() + xOffset, center.getY() + yOffset,
                            center.getZ() + zOffset);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof RealmHabitatChimeBlock)
                            || state.getValue(RealmHabitatChimeBlock.MODE) != mode
                            || state.getValue(RealmHabitatChimeBlock.ACTIVE)) {
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

    private static RealmHabitatChimeBlock.Mode modeForEvent(WorldEventType type) {
        return switch (type) {
            case FAR_BELL_PROCESSION -> RealmHabitatChimeBlock.Mode.WAYFARER;
            case MEMORY_FIREFLY_TIDE -> RealmHabitatChimeBlock.Mode.CONFLUENCE;
            case INTERLAYER_RESPITE -> RealmHabitatChimeBlock.Mode.INTERLAYER;
            default -> null;
        };
    }

    private static double responseRingRadius(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> 10.0D;
            case CONFLUENCE -> 8.0D;
            case INTERLAYER -> 6.0D;
        };
    }

    private static double responseSpeed(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> 1.05D;
            case CONFLUENCE -> 0.95D;
            case INTERLAYER -> 0.85D;
        };
    }

    private static void presentResidentResponse(ServerLevel level, PathfinderMob resident,
                                                RealmHabitatChimeBlock.Mode mode) {
        ParticleOptions particle = switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
        level.sendParticles(particle, resident.getX(),
                resident.getY() + resident.getBbHeight() * 0.6D, resident.getZ(),
                10, 0.3D, 0.2D, 0.3D, 0.02D);
    }

    private static void playResponse(ServerLevel level, BlockPos center,
                                     RealmHabitatChimeBlock.Mode mode) {
        level.playSound(null, center, switch (mode) {
            case WAYFARER -> SoundEvents.BELL_BLOCK;
            case CONFLUENCE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case INTERLAYER -> SoundEvents.SCULK_CLICKING;
        }, SoundSource.AMBIENT, 0.55F, switch (mode) {
            case WAYFARER -> 1.3F;
            case CONFLUENCE -> 1.6F;
            case INTERLAYER -> 0.7F;
        });
    }
}
