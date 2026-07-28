package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

final class BossNbtData {
    static final int MAX_PARTICIPANTS = 256;
    static final int MAX_SUMMONS = 64;
    static final int MAX_ARENA_POSITIONS = 256;
    private static final int MAX_HORIZONTAL_COORDINATE = 30_000_000;
    private static final int MIN_PACKED_Y = -2048;
    private static final int MAX_PACKED_Y = 2047;

    private BossNbtData() {
    }

    static void loadUuids(CompoundTag tag, String key, Collection<UUID> target, int maxEntries) {
        target.clear();
        ListTag values = tag.getList(key, Tag.TAG_INT_ARRAY);
        int limit = Math.min(values.size(), maxEntries);
        for (int i = 0; i < limit; i++) {
            if (values.getIntArray(i).length == 4) {
                target.add(NbtUtils.loadUUID(values.get(i)));
            }
        }
    }

    static void addParticipant(Set<UUID> participants, UUID playerId) {
        if (playerId != null && (participants.contains(playerId)
                || participants.size() < MAX_PARTICIPANTS)) {
            participants.add(playerId);
        }
    }

    static void loadPositionsNear(CompoundTag tag, String key, Collection<BlockPos> target, int maxEntries,
                                  BlockPos center, int horizontalRange, int verticalRange) {
        target.clear();
        ListTag values = tag.getList(key, Tag.TAG_INT_ARRAY);
        int limit = Math.min(values.size(), maxEntries);
        for (int i = 0; i < limit; i++) {
            BlockPos pos = decodePosition(values.getIntArray(i));
            if (pos != null && isNear(pos, center, horizontalRange, verticalRange)) {
                target.add(pos);
            }
        }
    }

    static BlockPos loadPositionNear(CompoundTag tag, String key, BlockPos center,
                                     int horizontalRange, int verticalRange) {
        BlockPos pos = decodePosition(tag.getIntArray(key));
        return pos != null && isNear(pos, center, horizontalRange, verticalRange) ? pos : null;
    }

    static void loadVec3sNear(CompoundTag tag, String key, Collection<Vec3> target, int maxEntries,
                              Vec3 center, double horizontalRange, double verticalRange) {
        target.clear();
        ListTag values = tag.getList(key, Tag.TAG_COMPOUND);
        int limit = Math.min(values.size(), maxEntries);
        for (int i = 0; i < limit; i++) {
            Tag entry = values.get(i);
            if (!(entry instanceof CompoundTag value)) {
                continue;
            }
            double x = value.getDouble("X");
            double y = value.getDouble("Y");
            double z = value.getDouble("Z");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Math.abs(x - center.x) <= horizontalRange
                    && Math.abs(y - center.y) <= verticalRange
                    && Math.abs(z - center.z) <= horizontalRange) {
                target.add(new Vec3(x, y, z));
            }
        }
    }

    static int clampTimer(int value, int maximum) {
        return Math.max(0, Math.min(value, Math.max(0, maximum)));
    }

    static int clampParticipantScale(int value) {
        return Math.max(1, Math.min(value, MAX_PARTICIPANTS));
    }

    static float clampHealthFloor(float value, float maximum) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(value, Math.max(0.0F, maximum)));
    }

    private static BlockPos decodePosition(int[] coords) {
        if (coords.length != 3
                || Math.abs((long) coords[0]) > MAX_HORIZONTAL_COORDINATE
                || Math.abs((long) coords[2]) > MAX_HORIZONTAL_COORDINATE
                || coords[1] < MIN_PACKED_Y || coords[1] > MAX_PACKED_Y) {
            return null;
        }
        return new BlockPos(coords[0], coords[1], coords[2]);
    }

    private static boolean isNear(BlockPos pos, BlockPos center, int horizontalRange, int verticalRange) {
        return Math.abs((long) pos.getX() - center.getX()) <= horizontalRange
                && Math.abs((long) pos.getY() - center.getY()) <= verticalRange
                && Math.abs((long) pos.getZ() - center.getZ()) <= horizontalRange;
    }
}
