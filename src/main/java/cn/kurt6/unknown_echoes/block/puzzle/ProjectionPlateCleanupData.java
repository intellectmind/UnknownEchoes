package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 踏板移除后仍保留未完成的关门，重试不会加载区块或回写原踏板位置。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ProjectionPlateCleanupData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_projection_plate_cleanup";
    private static final SavedData.Factory<ProjectionPlateCleanupData> FACTORY =
            new SavedData.Factory<>(ProjectionPlateCleanupData::new, ProjectionPlateCleanupData::load);
    // 每 10 tick 最多处理四处已移除踏板，失败项移到队尾。
    private static final int MAX_CLEANUPS = 4;
    private final LinkedHashMap<BlockPos, Cleanup> pending = new LinkedHashMap<>();
    private final Set<BlockPos> applying = new HashSet<>();

    static void release(ServerLevel level, BlockPos pos) {
        ProjectionPlateCleanupData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        BlockPos origin = pos.immutable();
        Cleanup request = new Cleanup(level.getGameTime() + ProjectionResonancePlateBlock.RECHECK_TICKS);
        data.pending.remove(origin);
        data.pending.put(origin, request);
        data.setDirty();
        data.apply(level, origin, request);
    }

    private boolean apply(ServerLevel level, BlockPos origin, Cleanup request) {
        if (!applying.add(origin)) {
            return false;
        }
        try {
            boolean complete = ProjectionResonancePlateBlock.finishRemovedPlateCleanup(level, origin);
            // 同一坐标可在回调中再次被移除，旧结果不能清掉新的清理请求。
            if (complete && pending.get(origin) == request) {
                pending.remove(origin);
                setDirty();
            }
            return complete;
        } finally {
            applying.remove(origin);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % ProjectionResonancePlateBlock.RECHECK_TICKS != 0) {
            return;
        }
        ProjectionPlateCleanupData data = level.getDataStorage().get(FACTORY, DATA_NAME);
        if (data != null) {
            data.drain(level);
        }
    }

    private void drain(ServerLevel level) {
        if (pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        var batch = pending.entrySet().stream().limit(MAX_CLEANUPS)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        RuntimeException failure = null;
        for (var entry : batch) {
            BlockPos origin = entry.getKey();
            Cleanup request = entry.getValue();
            long remaining = request.readyAt() - now;
            if (pending.get(origin) != request
                    || remaining > 0 && remaining <= ProjectionResonancePlateBlock.RECHECK_TICKS) {
                continue;
            }
            boolean complete = false;
            try {
                complete = apply(level, origin, request);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            } finally {
                if (!complete && pending.get(origin) == request) {
                    pending.remove(origin);
                    pending.put(origin, new Cleanup(now + ProjectionResonancePlateBlock.RECHECK_TICKS));
                    setDirty();
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static ProjectionPlateCleanupData load(CompoundTag tag, HolderLookup.Provider registries) {
        ProjectionPlateCleanupData data = new ProjectionPlateCleanupData();
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.contains("Origin", Tag.TAG_LONG) && entry.contains("RetryAt", Tag.TAG_LONG)) {
                data.pending.put(BlockPos.of(entry.getLong("Origin")), new Cleanup(entry.getLong("RetryAt")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (var entry : pending.entrySet()) {
            CompoundTag saved = new CompoundTag();
            saved.putLong("Origin", entry.getKey().asLong());
            saved.putLong("RetryAt", entry.getValue().readyAt());
            entries.add(saved);
        }
        tag.put("Entries", entries);
        return tag;
    }

    private record Cleanup(long readyAt) {}
}
