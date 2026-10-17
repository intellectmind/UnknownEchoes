package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Boss 唯一物品掉落的世界级失败重试，不加载区块，也不改发给玩家。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
final class BossRewardDropData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_pending_boss_drops";
    private static final SavedData.Factory<BossRewardDropData> FACTORY =
            new SavedData.Factory<>(BossRewardDropData::new, BossRewardDropData::load);
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_RETRIES_PER_PASS = 128;
    private static final int FAILURE_LOG_INTERVAL_TICKS = 1_200;
    private static final int MAX_PERSISTED_STACK_COUNT = 99;
    private static final double MAX_HORIZONTAL_COORDINATE = 30_000_000.0D;
    private static final double MIN_SAVED_Y = -2_048.0D;
    private static final double MAX_SAVED_Y = 2_047.0D;

    private final LinkedHashMap<UUID, PendingDrop> pendingDrops = new LinkedHashMap<>();
    private boolean deliveringDrops;
    private boolean hasLoggedDropFailure;
    private long lastFailureLogTime;

    static void dropOrQueue(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        BossRewardDropData data = get(level);
        List<PendingDrop> batch = data.queueDrops(x, y, z, stack);
        data.deliverDrops(level, batch);
    }

    static void queueForRetry(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (!stack.isEmpty()) {
            get(level).queueDrops(x, y, z, stack);
        }
    }

    private List<PendingDrop> queueDrops(double x, double y, double z, ItemStack stack) {
        List<PendingDrop> batch = new ArrayList<>();
        int maxCount = Math.max(1, Math.min(stack.getMaxStackSize(), MAX_PERSISTED_STACK_COUNT));
        int remaining = stack.getCount();
        while (remaining > 0) {
            int count = Math.min(remaining, maxCount);
            batch.add(new PendingDrop(UUID.randomUUID(), x, y, z, stack.copyWithCount(count)));
            remaining -= count;
        }
        // 实体事件可能抛异常或改写调用方物品，必须先完整登记本次提交的所有堆叠。
        for (PendingDrop pending : batch) {
            this.pendingDrops.put(pending.entityUuid, pending);
        }
        this.setDirty();
        return batch;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && Math.floorMod(level.getGameTime(), RETRY_INTERVAL_TICKS) == 0L) {
            retryLoadedDrops(level);
        }
    }

    private static void retryLoadedDrops(ServerLevel level) {
        BossRewardDropData data = get(level);
        if (data.deliveringDrops || data.pendingDrops.isEmpty()) {
            return;
        }
        int count = Math.min(MAX_RETRIES_PER_PASS, data.pendingDrops.size());
        List<PendingDrop> retryBatch = new ArrayList<>(count);
        var iterator = data.pendingDrops.values().iterator();
        for (int i = 0; i < count; i++) {
            retryBatch.add(iterator.next());
        }
        // 移到队尾而不移除记录，再派发回调；失败或卸载的队首不能挡住后续奖励。
        for (PendingDrop pending : retryBatch) {
            data.pendingDrops.putLast(pending.entityUuid, pending);
        }
        data.deliverDrops(level, retryBatch);
    }

    private void deliverDrops(ServerLevel level, List<PendingDrop> batch) {
        if (this.deliveringDrops) {
            return;
        }
        this.deliveringDrops = true;
        try {
            // 加入实体会同步触发事件；回调追加的待发记录留待下一批，不能重入当前发放。
            for (PendingDrop pending : batch) {
                BlockPos pos = BlockPos.containing(pending.x, pending.y, pending.z);
                // 方块区块就绪时，旧掉落实体仍可能在加载队列中；此时查不到 UUID 不能视作不存在。
                if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                        || !level.areEntitiesLoaded(ChunkPos.asLong(pos))) {
                    continue;
                }

                if (level.getEntity(pending.entityUuid) != null) {
                    this.completeDrop(pending);
                    continue;
                }
                ItemEntity item = null;
                try {
                    item = createItem(level, pending.entityUuid,
                            pending.x, pending.y, pending.z, pending.stack);
                    if (level.addFreshEntity(item)) {
                        this.completeDrop(pending);
                    } else {
                        item.discard();
                    }
                } catch (RuntimeException failure) {
                    // 加入后的回调也可能失败，已登记物品必须先结清，避免拾取后被再次发放。
                    if ((item != null && item.isAddedToLevel()) || level.getEntity(pending.entityUuid) != null) {
                        this.completeDrop(pending);
                    } else if (item != null) {
                        item.discard();
                    }
                    this.logDropFailure(level, pending, failure);
                }
            }
        } finally {
            this.deliveringDrops = false;
        }
    }

    private void logDropFailure(ServerLevel level, PendingDrop pending, RuntimeException failure) {
        long gameTime = level.getGameTime();
        if (this.hasLoggedDropFailure && gameTime >= this.lastFailureLogTime
                && gameTime - this.lastFailureLogTime < FAILURE_LOG_INTERVAL_TICKS) {
            return;
        }
        this.hasLoggedDropFailure = true;
        this.lastFailureLogTime = gameTime;
        UnknownEchoes.LOGGER.error("Boss reward spawn failed in {} at [{}, {}, {}], entity {}",
                level.dimension().location(), pending.x, pending.y, pending.z, pending.entityUuid, failure);
    }

    private void completeDrop(PendingDrop pending) {
        this.pendingDrops.remove(pending.entityUuid);
        // 后续奖励的事件可能抛异常；已完成记录必须独立进入存档更新。
        this.setDirty();
    }

    private static ItemEntity createItem(ServerLevel level, UUID uuid,
                                         double x, double y, double z, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, x, y, z, stack.copy());
        item.setUUID(uuid);
        item.setDefaultPickUpDelay();
        return item;
    }

    private static BossRewardDropData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static BossRewardDropData load(CompoundTag tag, HolderLookup.Provider registries) {
        BossRewardDropData data = new BossRewardDropData();
        ListTag drops = tag.getList("Drops", Tag.TAG_COMPOUND);
        Set<UUID> loadedUuids = new HashSet<>();
        for (int i = 0; i < drops.size(); i++) {
            CompoundTag drop = drops.getCompound(i);
            if (!drop.hasUUID("EntityUuid")) {
                continue;
            }
            UUID entityUuid = drop.getUUID("EntityUuid");
            if (!loadedUuids.add(entityUuid)) {
                continue;
            }
            double x = drop.getDouble("X");
            double y = drop.getDouble("Y");
            double z = drop.getDouble("Z");
            if (!isValidPosition(x, y, z)) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(registries, drop.getCompound("Stack"));
            int maxCount = stack.isEmpty() ? 0
                    : Math.max(1, Math.min(stack.getMaxStackSize(), MAX_PERSISTED_STACK_COUNT));
            if (!stack.isEmpty() && stack.getCount() <= maxCount) {
                data.pendingDrops.put(entityUuid, new PendingDrop(entityUuid, x, y, z, stack));
            }
        }
        return data;
    }

    private static boolean isValidPosition(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= MAX_HORIZONTAL_COORDINATE
                && Math.abs(z) <= MAX_HORIZONTAL_COORDINATE
                && y >= MIN_SAVED_Y && y <= MAX_SAVED_Y;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag drops = new ListTag();
        for (PendingDrop pending : pendingDrops.values()) {
            CompoundTag drop = new CompoundTag();
            drop.putUUID("EntityUuid", pending.entityUuid);
            drop.putDouble("X", pending.x);
            drop.putDouble("Y", pending.y);
            drop.putDouble("Z", pending.z);
            drop.put("Stack", pending.stack.save(registries));
            drops.add(drop);
        }
        tag.put("Drops", drops);
        return tag;
    }

    private record PendingDrop(UUID entityUuid, double x, double y, double z, ItemStack stack) {
    }
}
