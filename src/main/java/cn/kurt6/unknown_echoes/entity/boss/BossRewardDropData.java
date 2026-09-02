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
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final int MAX_PERSISTED_STACK_COUNT = 99;
    private static final double MAX_HORIZONTAL_COORDINATE = 30_000_000.0D;
    private static final double MIN_SAVED_Y = -2_048.0D;
    private static final double MAX_SAVED_Y = 2_047.0D;

    private final List<PendingDrop> pendingDrops = new ArrayList<>();

    static void dropOrQueue(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int maxCount = Math.max(1, Math.min(stack.getMaxStackSize(), MAX_PERSISTED_STACK_COUNT));
        int remaining = stack.getCount();
        while (remaining > 0) {
            int count = Math.min(remaining, maxCount);
            dropSingleOrQueue(level, x, y, z, stack.copyWithCount(count));
            remaining -= count;
        }
    }

    private static void dropSingleOrQueue(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemEntity item = createItem(level, UUID.randomUUID(), x, y, z, stack);
        if (level.addFreshEntity(item)) {
            return;
        }
        item.discard();
        BossRewardDropData data = get(level);
        data.pendingDrops.add(new PendingDrop(item.getUUID(), x, y, z, stack));
        data.setDirty();
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
        boolean changed = false;
        for (Iterator<PendingDrop> iterator = data.pendingDrops.iterator(); iterator.hasNext(); ) {
            PendingDrop pending = iterator.next();
            BlockPos pos = BlockPos.containing(pending.x, pending.y, pending.z);
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            if (level.getEntity(pending.entityUuid) != null) {
                iterator.remove();
                changed = true;
                continue;
            }
            ItemEntity item = createItem(level, pending.entityUuid,
                    pending.x, pending.y, pending.z, pending.stack);
            if (level.addFreshEntity(item)) {
                iterator.remove();
                changed = true;
            } else {
                item.discard();
            }
        }
        if (changed) {
            data.setDirty();
        }
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
                data.pendingDrops.add(new PendingDrop(entityUuid, x, y, z, stack));
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
        for (PendingDrop pending : pendingDrops) {
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
