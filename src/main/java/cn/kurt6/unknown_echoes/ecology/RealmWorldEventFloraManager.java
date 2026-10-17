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
        java.util.function.Predicate<BlockState> matches = state -> state.is(tag);
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - VERTICAL_RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + VERTICAL_RADIUS);
        if (minY > maxY) return nearestBySector;
        int minX = center.getX() - SCAN_RADIUS, maxX = center.getX() + SCAN_RADIUS;
        int minZ = center.getZ() - SCAN_RADIUS, maxZ = center.getZ() + SCAN_RADIUS;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                int fromX = Math.max(minX, chunkX << 4), toX = Math.min(maxX, (chunkX << 4) + 15);
                int fromZ = Math.max(minZ, chunkZ << 4), toZ = Math.min(maxZ, (chunkZ << 4) + 15);
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    var section = chunk.getSection(chunk.getSectionIndex(sectionY << 4));
                    if (!section.maybeHas(matches)) continue;
                    int fromY = Math.max(minY, sectionY << 4), toY = Math.min(maxY, (sectionY << 4) + 15);
                    for (int y = fromY; y <= toY; y++) {
                        int dy = y - center.getY();
                        for (int x = fromX; x <= toX; x++) {
                            int dx = x - center.getX();
                            for (int z = fromZ; z <= toZ; z++) {
                                int dz = z - center.getZ();
                                int horizontalDistanceSqr = dx * dx + dz * dz;
                                if (horizontalDistanceSqr > horizontalRadiusSqr
                                        || !matches.test(section.getBlockState(x & 15, y & 15, z & 15))) continue;
                                int sector = sectorFor(dx, dz);
                                int distanceSqr = horizontalDistanceSqr + dy * dy;
                                FloraCandidate current = nearestBySector[sector];
                                if (current == null || distanceSqr < current.distanceSqr()
                                        || distanceSqr == current.distanceSqr() && earlier(y, x, z, current.pos())) {
                                    nearestBySector[sector] = new FloraCandidate(new BlockPos(x, y, z), distanceSqr);
                                }
                            }
                        }
                    }
                }
            }
        }
        return nearestBySector;
    }

    /** 分区遍历不决定同距优先级；同一扇区仍按 Y、X、Z 选择。 */
    private static boolean earlier(int y, int x, int z, BlockPos current) {
        return y < current.getY()
                || y == current.getY() && (x < current.getX() || x == current.getX() && z < current.getZ());
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
