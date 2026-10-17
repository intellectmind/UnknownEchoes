package cn.kurt6.unknown_echoes.block.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class WonderTrialData extends SavedData {
    private static final Factory<WonderTrialData> FACTORY = new Factory<>(WonderTrialData::new, WonderTrialData::load);
    private final Map<Long, WonderTrialProgress> trials = new HashMap<>();

    public static WonderTrialData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "unknown_echoes_wonder_trials");
    }

    public WonderTrialProgress find(BlockPos pos) { return trials.get(pos.asLong()); }

    public WonderTrialProgress at(BlockPos pos) {
        return trials.computeIfAbsent(pos.asLong(), key -> {
            setDirty();
            return new WonderTrialProgress();
        });
    }

    public boolean complete(BlockPos pos) {
        WonderTrialProgress state = find(pos);
        return state != null && state.complete();
    }

    // 击败记录独立于方块区块；未加载或被清理的实体不能被当成击杀。
    public void defeated(BlockPos pos, UUID run, UUID id) {
        WonderTrialProgress state = find(pos);
        if (state != null && state.defeated(run, id)) setDirty();
    }

    private static WonderTrialData load(CompoundTag tag, HolderLookup.Provider registries) {
        WonderTrialData data = new WonderTrialData();
        ListTag list = tag.getList("Trials", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("Pos", Tag.TAG_LONG) || !entry.hasUUID("Run")) continue;
            HashSet<UUID> remaining = new HashSet<>();
            ListTag ids = entry.getList("Remaining", Tag.TAG_INT_ARRAY);
            boolean valid = ids.size() <= WonderTrialProgress.MAX_ALIVE;
            for (int j = 0; j < ids.size() && valid; j++) {
                try { valid = remaining.add(NbtUtils.loadUUID(ids.get(j))); }
                catch (IllegalArgumentException ignored) { valid = false; }
            }
            if (valid) data.trials.put(entry.getLong("Pos"), WonderTrialProgress.restore(entry.getUUID("Run"),
                    entry.getInt("Wave"), entry.getInt("Spawned"), entry.getBoolean("Active"), remaining));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        trials.forEach((pos, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos);
            entry.putUUID("Run", state.run());
            entry.putInt("Wave", state.wave());
            entry.putInt("Spawned", state.spawned());
            entry.putBoolean("Active", state.active());
            ListTag ids = new ListTag();
            state.remaining().forEach(id -> ids.add(NbtUtils.createUUID(id)));
            entry.put("Remaining", ids);
            list.add(entry);
        });
        tag.put("Trials", list);
        return tag;
    }
}
