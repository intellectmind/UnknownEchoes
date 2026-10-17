package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.tide.TideRuneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 世界持有符文归属和销毁后的恢复请求，卸载中的新主人仍可阻止旧清理。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class WatcherRuneCleanupData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_watcher_rune_cleanup";
    private static final Factory<WatcherRuneCleanupData> FACTORY =
            new Factory<>(WatcherRuneCleanupData::new, WatcherRuneCleanupData::load);
    private static final int RETRY_TICKS = 10;
    private static final int MAX_CLEANUPS = 4;
    private final Map<UUID, Set<BlockPos>> claims = new LinkedHashMap<>();
    private final Map<BlockPos, Set<UUID>> owners = new HashMap<>();
    private final LinkedHashMap<UUID, Cleanup> pending = new LinkedHashMap<>();
    private final Set<UUID> applying = new HashSet<>();
    private long lastDrainTick = Long.MIN_VALUE;

    static void claim(ServerLevel level, UUID owner, Collection<BlockPos> positions) {
        WatcherRuneCleanupData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        Set<BlockPos> next = new LinkedHashSet<>();
        positions.forEach(pos -> next.add(pos.immutable()));
        if (next.equals(data.claims.getOrDefault(owner, Set.of()))) {
            return;
        }
        data.forget(owner);
        if (!next.isEmpty()) {
            data.claims.put(owner, next);
            next.forEach(pos -> data.owners.computeIfAbsent(pos, ignored -> new HashSet<>()).add(owner));
        }
        data.setDirty();
    }

    static void forget(ServerLevel level, UUID owner) {
        WatcherRuneCleanupData data = level.getDataStorage().get(FACTORY, DATA_NAME);
        if (data != null) {
            data.forget(owner);
        }
    }

    private void forget(UUID owner) {
        Set<BlockPos> previous = claims.remove(owner);
        if (previous == null) {
            return;
        }
        for (BlockPos pos : previous) {
            Set<UUID> current = owners.get(pos);
            if (current != null && current.remove(owner) && current.isEmpty()) {
                owners.remove(pos);
            }
        }
        setDirty();
    }

    static void queue(ServerLevel level, UUID owner, Collection<BlockPos> positions) {
        WatcherRuneCleanupData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        Set<BlockPos> remaining = new LinkedHashSet<>(data.claims.getOrDefault(owner, Set.of()));
        positions.forEach(pos -> remaining.add(pos.immutable()));
        Cleanup previous = data.pending.get(owner);
        if (previous != null) {
            remaining.addAll(previous.positions);
        }
        data.forget(owner);
        if (!remaining.isEmpty() && (previous == null || !previous.positions.equals(remaining))) {
            data.pending.put(owner, new Cleanup(remaining, level.getGameTime() + RETRY_TICKS));
            data.setDirty();
        }
    }

    static void apply(ServerLevel level, UUID owner) {
        WatcherRuneCleanupData data = level.getDataStorage().get(FACTORY, DATA_NAME);
        if (data != null) {
            Cleanup request = data.pending.get(owner);
            if (request != null) {
                data.apply(level, owner, request, BossNbtData.MAX_ARENA_POSITIONS);
            }
        }
    }

    private int apply(ServerLevel level, UUID owner, Cleanup request, int budget) {
        if (request.lastAttemptTick == level.getGameTime() || !applying.add(owner)) {
            return 0;
        }
        request.lastAttemptTick = level.getGameTime();
        int attempted = 0;
        RuntimeException failure = null;
        try {
            for (BlockPos pos : request.positions.stream().limit(budget).toList()) {
                if (pending.get(owner) != request) {
                    break;
                }
                attempted++;
                boolean complete = false;
                try {
                    complete = clearRune(level, pos);
                } catch (RuntimeException exception) {
                    if (failure == null) { failure = exception; }
                    else if (failure != exception) { failure.addSuppressed(exception); }
                } finally {
                    if (pending.get(owner) == request) {
                        request.positions.remove(pos);
                        if (!complete) {
                            request.positions.add(pos);
                        }
                        setDirty();
                    }
                }
            }
        } finally {
            applying.remove(owner);
            if (pending.get(owner) == request) {
                pending.remove(owner);
                if (!request.positions.isEmpty()) {
                    request.readyAt = level.getGameTime() + RETRY_TICKS;
                    pending.put(owner, request);
                }
                setDirty();
            }
        }
        if (failure != null) {
            throw failure;
        }
        return attempted;
    }

    private boolean clearRune(ServerLevel level, BlockPos pos) {
        if (owners.containsKey(pos) || level.isOutsideBuildHeight(pos)) {
            return true;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return false;
        }
        var state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof TideRuneBlock) || !state.getValue(TideRuneBlock.CORRUPTED)) {
            return true;
        }
        var cleared = state.setValue(TideRuneBlock.CORRUPTED, false);
        if (!level.setBlock(pos, cleared, 3)) {
            return false;
        }
        chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos) == cleared;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % RETRY_TICKS != 0) {
            return;
        }
        WatcherRuneCleanupData data = level.getDataStorage().get(FACTORY, DATA_NAME);
        if (data != null) {
            data.drain(level);
        }
    }

    private void drain(ServerLevel level) {
        long now = level.getGameTime();
        if (pending.isEmpty() || lastDrainTick == now) {
            return;
        }
        lastDrainTick = now;
        var batch = pending.entrySet().stream().limit(MAX_CLEANUPS)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        int budget = MAX_CLEANUPS;
        for (var entry : batch) {
            Cleanup request = entry.getValue();
            long remaining = request.readyAt - now;
            if (pending.get(entry.getKey()) != request || remaining > 0 && remaining <= RETRY_TICKS) {
                continue;
            }
            try {
                budget -= apply(level, entry.getKey(), request, 1);
            } catch (RuntimeException failure) {
                budget--;
                UnknownEchoes.LOGGER.error("Watcher rune cleanup failed for {}", entry.getKey(), failure);
            }
            if (budget <= 0) {
                break;
            }
        }
    }

    private static WatcherRuneCleanupData load(CompoundTag tag, HolderLookup.Provider registries) {
        WatcherRuneCleanupData data = new WatcherRuneCleanupData();
        for (String name : new String[]{"Claims", "Pending"}) {
            ListTag entries = tag.getList(name, Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                if (!entry.hasUUID("Owner")) {
                    continue;
                }
                UUID owner = entry.getUUID("Owner");
                Set<BlockPos> positions = new LinkedHashSet<>();
                long[] packed = entry.getLongArray("Positions");
                for (int j = 0; j < packed.length; j++) {
                    BlockPos pos = BlockPos.of(packed[j]);
                    if (Math.abs((long) pos.getX()) <= 30_000_000 && Math.abs((long) pos.getZ()) <= 30_000_000) {
                        positions.add(pos);
                    }
                }
                if (positions.isEmpty()) { continue; }
                if (name.equals("Claims")) {
                    data.claims.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).addAll(positions);
                    positions.forEach(pos -> data.owners.computeIfAbsent(pos, ignored -> new HashSet<>()).add(owner));
                } else {
                    data.pending.computeIfAbsent(owner, ignored -> new Cleanup(Set.of(), entry.getLong("RetryAt")))
                            .positions.addAll(positions);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag active = new ListTag();
        claims.forEach((owner, positions) -> active.add(saveEntry(owner, positions)));
        tag.put("Claims", active);
        ListTag retired = new ListTag();
        pending.forEach((owner, request) -> {
            CompoundTag entry = saveEntry(owner, request.positions);
            entry.putLong("RetryAt", request.readyAt);
            retired.add(entry);
        });
        tag.put("Pending", retired);
        return tag;
    }

    private static CompoundTag saveEntry(UUID owner, Set<BlockPos> positions) {
        CompoundTag entry = new CompoundTag();
        entry.putUUID("Owner", owner);
        entry.putLongArray("Positions", positions.stream().mapToLong(BlockPos::asLong).toArray());
        return entry;
    }

    private static final class Cleanup {
        final LinkedHashSet<BlockPos> positions;
        long readyAt;
        long lastAttemptTick = Long.MIN_VALUE;

        Cleanup(Collection<BlockPos> positions, long readyAt) {
            this.positions = new LinkedHashSet<>(positions);
            this.readyAt = readyAt;
        }
    }
}
