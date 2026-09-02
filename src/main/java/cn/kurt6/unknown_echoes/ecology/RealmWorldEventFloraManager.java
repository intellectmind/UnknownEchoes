package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.worldevent.WorldEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 让三域旅行异象唤起附近既有植被的一次性表现共鸣。 */
public final class RealmWorldEventFloraManager {
    public static final int SCAN_RADIUS = 18;
    public static final int VERTICAL_RADIUS = 6;
    public static final int MAX_RESPONDERS = 8;
    public static final int TRACE_STEPS = 8;

    private static final TagKey<Block> WAYFARER_FLORA = floraTag(
            "realm_field_lens_wayfarer_flora");
    private static final TagKey<Block> CONFLUENCE_FLORA = floraTag(
            "realm_field_lens_confluence_flora");
    private static final TagKey<Block> INTERLAYER_FLORA = floraTag(
            "realm_field_lens_interlayer_flora");

    private RealmWorldEventFloraManager() {
    }

    public static boolean supports(WorldEventType type) {
        return modeForEvent(type) != null;
    }

    public static int respond(ServerLevel level, WorldEventType type, BlockPos center) {
        RealmHabitatChimeBlock.Mode mode = modeForEvent(type);
        if (mode == null || RealmHabitatChimeBlock.modeForDimension(level) != mode) {
            return 0;
        }

        FloraCandidate[] responders = findResponders(level, center, mode);
        int count = 0;
        for (FloraCandidate responder : responders) {
            if (responder == null) {
                continue;
            }
            presentResponse(level, responder.pos(), center, mode);
            count++;
        }
        if (count > 0) {
            playResponse(level, center, mode);
        }
        return count;
    }

    /** 每个水平方向扇区只选最近植株，避免密集树冠独占全部表现名额。 */
    private static FloraCandidate[] findResponders(ServerLevel level, BlockPos center,
                                                   RealmHabitatChimeBlock.Mode mode) {
        FloraCandidate[] nearestBySector = new FloraCandidate[MAX_RESPONDERS];
        int horizontalRadiusSqr = SCAN_RADIUS * SCAN_RADIUS;
        TagKey<Block> tag = floraTag(mode);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int yOffset = -VERTICAL_RADIUS; yOffset <= VERTICAL_RADIUS; yOffset++) {
            for (int xOffset = -SCAN_RADIUS; xOffset <= SCAN_RADIUS; xOffset++) {
                for (int zOffset = -SCAN_RADIUS; zOffset <= SCAN_RADIUS; zOffset++) {
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
                    if (!state.is(tag)) {
                        continue;
                    }

                    int sector = sectorFor(xOffset, zOffset);
                    int distanceSqr = horizontalDistanceSqr + yOffset * yOffset;
                    FloraCandidate current = nearestBySector[sector];
                    if (current == null || distanceSqr < current.distanceSqr()) {
                        nearestBySector[sector] = new FloraCandidate(
                                cursor.immutable(), distanceSqr);
                    }
                }
            }
        }
        return nearestBySector;
    }

    private static int sectorFor(int xOffset, int zOffset) {
        double angle = Math.atan2(zOffset, xOffset);
        int sector = (int) Math.floor((angle + Math.PI)
                / (Math.PI * 2.0D) * MAX_RESPONDERS);
        return Math.floorMod(sector, MAX_RESPONDERS);
    }

    private static void presentResponse(ServerLevel level, BlockPos floraPos,
                                        BlockPos center,
                                        RealmHabitatChimeBlock.Mode mode) {
        ParticleOptions primary = switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
        ParticleOptions accent = switch (mode) {
            case WAYFARER -> ParticleTypes.CLOUD;
            case CONFLUENCE -> ParticleTypes.END_ROD;
            case INTERLAYER -> ParticleTypes.REVERSE_PORTAL;
        };

        Vec3 from = Vec3.atCenterOf(floraPos).add(0.0D, 0.35D, 0.0D);
        Vec3 to = Vec3.atCenterOf(center).add(0.0D, 1.1D, 0.0D);
        Vec3 delta = to.subtract(from);
        double arcHeight = switch (mode) {
            case WAYFARER -> 0.45D;
            case CONFLUENCE -> 1.0D;
            case INTERLAYER -> 0.2D;
        };
        for (int step = 0; step <= TRACE_STEPS; step++) {
            double progress = step / (double) TRACE_STEPS;
            Vec3 point = from.add(delta.scale(progress))
                    .add(0.0D, Math.sin(progress * Math.PI) * arcHeight, 0.0D);
            level.sendParticles(primary, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
        level.sendParticles(accent, from.x, from.y, from.z,
                5, 0.22D, 0.18D, 0.22D, 0.012D);
    }

    private static void playResponse(ServerLevel level, BlockPos center,
                                     RealmHabitatChimeBlock.Mode mode) {
        level.playSound(null, center, switch (mode) {
            case WAYFARER -> SoundEvents.BELL_BLOCK;
            case CONFLUENCE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case INTERLAYER -> SoundEvents.SCULK_CLICKING;
        }, SoundSource.AMBIENT, 0.5F, switch (mode) {
            case WAYFARER -> 1.55F;
            case CONFLUENCE -> 1.85F;
            case INTERLAYER -> 0.6F;
        });
    }

    private static TagKey<Block> floraTag(String id) {
        return TagKey.create(Registries.BLOCK, UnknownEchoes.id(id));
    }

    private static TagKey<Block> floraTag(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> WAYFARER_FLORA;
            case CONFLUENCE -> CONFLUENCE_FLORA;
            case INTERLAYER -> INTERLAYER_FLORA;
        };
    }

    private static RealmHabitatChimeBlock.Mode modeForEvent(WorldEventType type) {
        return switch (type) {
            case FAR_BELL_PROCESSION -> RealmHabitatChimeBlock.Mode.WAYFARER;
            case MEMORY_FIREFLY_TIDE -> RealmHabitatChimeBlock.Mode.CONFLUENCE;
            case INTERLAYER_RESPITE -> RealmHabitatChimeBlock.Mode.INTERLAYER;
            default -> null;
        };
    }

    private record FloraCandidate(BlockPos pos, int distanceSqr) {
    }
}
