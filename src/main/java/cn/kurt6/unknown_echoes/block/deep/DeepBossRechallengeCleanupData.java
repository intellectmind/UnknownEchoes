package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 祭坛未加载时暂存深层 Boss 的精确击败与销毁通知，不主动加载区块。 */
final class DeepBossRechallengeCleanupData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_deep_boss_cleanup";
    private static final SavedData.Factory<DeepBossRechallengeCleanupData> FACTORY =
            new SavedData.Factory<>(DeepBossRechallengeCleanupData::new,
                    DeepBossRechallengeCleanupData::load);

    private final Map<Long, Set<UUID>> defeated = new HashMap<>();
    private final Map<Long, Set<UUID>> destroyed = new HashMap<>();
    /** 同一坐标回放期间，新通知只入队，避免回调递归消费尚未确认的记录。 */
    private final Set<Long> applying = new HashSet<>();

    static void queueDefeated(ServerLevel level, BlockPos altarPos, UUID bossUuid) {
        DeepBossRechallengeCleanupData data = get(level);
        data.queue(data.defeated, altarPos, bossUuid);
    }

    static void queueDestroyed(ServerLevel level, BlockPos altarPos, UUID bossUuid) {
        DeepBossRechallengeCleanupData data = get(level);
        data.queue(data.destroyed, altarPos, bossUuid);
    }

    private void queue(Map<Long, Set<UUID>> pending, BlockPos altarPos, UUID bossUuid) {
        if (pending.computeIfAbsent(altarPos.asLong(), ignored -> new HashSet<>()).add(bossUuid)) {
            setDirty();
        }
    }

    static void apply(ServerLevel level, BlockPos altarPos, BossRechallengeAltarBlockEntity altar) {
        DeepBossRechallengeCleanupData data = get(level);
        long key = altarPos.asLong();
        if (!data.applying.add(key)) {
            return;
        }
        try {
            // 击败必须先于销毁；保留记录直到回调返回，异常后仍可存档并重放。
            for (UUID bossUuid : Set.copyOf(data.defeated.getOrDefault(key, Set.of()))) {
                var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
                if (chunk == null || chunk.getBlockEntity(altarPos) != altar) {
                    return;
                }
                altar.startCooldown(level.getGameTime(), bossUuid);
                data.complete(data.defeated, key, bossUuid);
            }
            for (UUID bossUuid : Set.copyOf(data.destroyed.getOrDefault(key, Set.of()))) {
                var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
                if (chunk == null || chunk.getBlockEntity(altarPos) != altar) {
                    return;
                }
                if (data.defeated.getOrDefault(key, Set.of()).contains(bossUuid)) {
                    continue;
                }
                altar.clearBossIfOwned(bossUuid);
                data.complete(data.destroyed, key, bossUuid);
            }
        } finally {
            data.applying.remove(key);
        }
    }

    private void complete(Map<Long, Set<UUID>> pending, long key, UUID bossUuid) {
        Set<UUID> bosses = pending.get(key);
        if (bosses != null && bosses.remove(bossUuid)) {
            if (bosses.isEmpty()) {
                pending.remove(key);
            }
            setDirty();
        }
    }

    private static DeepBossRechallengeCleanupData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static DeepBossRechallengeCleanupData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeepBossRechallengeCleanupData data = new DeepBossRechallengeCleanupData();
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.contains("Altar", Tag.TAG_LONG)) {
                continue;
            }
            long altarPos = entry.getLong("Altar");
            loadBosses(entry, "DefeatedBosses", altarPos, data.defeated);
            loadBosses(entry, "DestroyedBosses", altarPos, data.destroyed);

            loadBosses(entry, "Bosses", altarPos, data.destroyed);
        }
        return data;
    }

    private static void loadBosses(CompoundTag entry, String key, long altarPos,
                                   Map<Long, Set<UUID>> destination) {
        ListTag bosses = entry.getList(key, Tag.TAG_INT_ARRAY);
        Set<UUID> uuids = destination.computeIfAbsent(altarPos, ignored -> new HashSet<>());
        for (int i = 0; i < bosses.size(); i++) {
            try {
                uuids.add(net.minecraft.nbt.NbtUtils.loadUUID(bosses.get(i)));
            } catch (IllegalArgumentException ignored) {

            }
        }
        if (uuids.isEmpty()) {
            destination.remove(altarPos);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        Set<Long> positions = new HashSet<>(defeated.keySet());
        positions.addAll(destroyed.keySet());
        for (long altarPos : positions) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Altar", altarPos);
            entry.put("DefeatedBosses", saveBosses(defeated.get(altarPos)));
            entry.put("DestroyedBosses", saveBosses(destroyed.get(altarPos)));
            entries.add(entry);
        }
        tag.put("Entries", entries);
        return tag;
    }

    private static ListTag saveBosses(Set<UUID> uuids) {
        ListTag bosses = new ListTag();
        if (uuids != null) {
            for (UUID bossUuid : uuids) {
                bosses.add(net.minecraft.nbt.NbtUtils.createUUID(bossUuid));
            }
        }
        return bosses;
    }
}
