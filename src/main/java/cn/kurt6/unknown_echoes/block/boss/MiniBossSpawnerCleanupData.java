package cn.kurt6.unknown_echoes.block.boss;

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

/** 刷怪器未加载时暂存 Mini Boss 的精确击败与销毁通知，不主动加载区块。 */
final class MiniBossSpawnerCleanupData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_miniboss_spawner_cleanup";
    private static final SavedData.Factory<MiniBossSpawnerCleanupData> FACTORY =
            new SavedData.Factory<>(MiniBossSpawnerCleanupData::new,
                    MiniBossSpawnerCleanupData::load);

    private final Map<Long, Set<UUID>> defeated = new HashMap<>();
    private final Map<Long, Set<UUID>> destroyed = new HashMap<>();

    static void queueDefeated(ServerLevel level, BlockPos spawnerPos, UUID bossUuid) {
        MiniBossSpawnerCleanupData data = get(level);
        data.queue(data.defeated, spawnerPos, bossUuid);
    }

    static void queueDestroyed(ServerLevel level, BlockPos spawnerPos, UUID bossUuid) {
        MiniBossSpawnerCleanupData data = get(level);
        data.queue(data.destroyed, spawnerPos, bossUuid);
    }

    private void queue(Map<Long, Set<UUID>> pending, BlockPos spawnerPos, UUID bossUuid) {
        if (pending.computeIfAbsent(spawnerPos.asLong(), ignored -> new HashSet<>()).add(bossUuid)) {
            setDirty();
        }
    }

    static void apply(ServerLevel level, BlockPos spawnerPos, MiniBossSpawnerBlockEntity spawner) {
        MiniBossSpawnerCleanupData data = get(level);
        long key = spawnerPos.asLong();
        Set<UUID> defeatedBosses = data.defeated.remove(key);
        Set<UUID> destroyedBosses = data.destroyed.remove(key);
        if (defeatedBosses != null) {
            for (UUID bossUuid : defeatedBosses) {
                spawner.onBossDefeated(bossUuid);
            }
        }
        if (destroyedBosses != null) {
            for (UUID bossUuid : destroyedBosses) {
                spawner.onBossDestroyed(bossUuid);
            }
        }
        if (defeatedBosses != null || destroyedBosses != null) {
            data.setDirty();
        }
    }

    private static MiniBossSpawnerCleanupData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static MiniBossSpawnerCleanupData load(CompoundTag tag, HolderLookup.Provider registries) {
        MiniBossSpawnerCleanupData data = new MiniBossSpawnerCleanupData();
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.contains("Spawner", Tag.TAG_LONG)) {
                continue;
            }
            long spawnerPos = entry.getLong("Spawner");
            loadBosses(entry, "DefeatedBosses", spawnerPos, data.defeated);
            loadBosses(entry, "DestroyedBosses", spawnerPos, data.destroyed);
        }
        return data;
    }

    /** 必须合并而不是覆盖:同一目的地有第二个来源键时,put 会丢掉先加载的那批 UUID。 */
    private static void loadBosses(CompoundTag entry, String key, long spawnerPos,
                                   Map<Long, Set<UUID>> destination) {
        ListTag bosses = entry.getList(key, Tag.TAG_INT_ARRAY);
        Set<UUID> uuids = destination.computeIfAbsent(spawnerPos, ignored -> new HashSet<>());
        for (int i = 0; i < bosses.size(); i++) {
            try {
                uuids.add(net.minecraft.nbt.NbtUtils.loadUUID(bosses.get(i)));
            } catch (IllegalArgumentException ignored) {

            }
        }
        if (uuids.isEmpty()) {
            destination.remove(spawnerPos);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        Set<Long> positions = new HashSet<>(defeated.keySet());
        positions.addAll(destroyed.keySet());
        for (long spawnerPos : positions) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Spawner", spawnerPos);
            entry.put("DefeatedBosses", saveBosses(defeated.get(spawnerPos)));
            entry.put("DestroyedBosses", saveBosses(destroyed.get(spawnerPos)));
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
