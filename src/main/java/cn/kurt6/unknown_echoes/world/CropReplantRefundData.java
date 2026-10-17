package cn.kurt6.unknown_echoes.world;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** 补种失败的种子按玩家 UUID 保存在主世界，离线和满背包时保留待归还数量。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class CropReplantRefundData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_crop_replant_refunds";
    private static final SavedData.Factory<CropReplantRefundData> FACTORY =
            new SavedData.Factory<>(CropReplantRefundData::new, CropReplantRefundData::load);
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_SAVED_STACK_COUNT = 99;
    private final Map<UUID, List<ItemStack>> refunds = new HashMap<>();

    public static void refund(MinecraftServer server, UUID playerId, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CropReplantRefundData data = get(server);
        data.addRefund(playerId, stack);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            data.deliver(playerId, player.getInventory()::add);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            get(player.server).deliver(player.getUUID(), player.getInventory()::add);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (Math.floorMod(server.getTickCount(), RETRY_INTERVAL_TICKS) != 0) {
            return;
        }
        CropReplantRefundData data = get(server);
        if (data.refunds.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            data.deliver(player.getUUID(), player.getInventory()::add);
        }
    }

    void addRefund(UUID playerId, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        List<ItemStack> pending = refunds.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        int remaining = stack.getCount();
        int maxCount = Math.max(1, Math.min(stack.getMaxStackSize(), MAX_SAVED_STACK_COUNT));
        for (ItemStack existing : pending) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int moved = Math.min(remaining, Math.max(0, maxCount - existing.getCount()));
                existing.grow(moved);
                remaining -= moved;
                if (remaining == 0) {
                    break;
                }
            }
        }
        while (remaining > 0) {
            int count = Math.min(remaining, maxCount);
            pending.add(stack.copyWithCount(count));
            remaining -= count;
        }
        setDirty();
    }

    // 插入器只修改传入的副本；账本按实际剩余数量扣减，部分入包也不会重发。
    void deliver(UUID playerId, Consumer<ItemStack> insert) {
        List<ItemStack> pending = refunds.get(playerId);
        if (pending == null) {
            return;
        }
        for (Iterator<ItemStack> iterator = pending.iterator(); iterator.hasNext();) {
            ItemStack stored = iterator.next();
            ItemStack remainder = stored.copy();
            insert.accept(remainder);
            if (remainder.getCount() == stored.getCount()) {
                continue;
            }
            if (remainder.isEmpty()) {
                iterator.remove();
            } else {
                stored.setCount(remainder.getCount());
            }
            setDirty();
        }
        if (pending.isEmpty()) {
            refunds.remove(playerId);
        }
    }

    private static CropReplantRefundData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    static CropReplantRefundData load(CompoundTag tag, HolderLookup.Provider registries) {
        CropReplantRefundData data = new CropReplantRefundData();
        ListTag entries = tag.getList("Refunds", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID("Player")) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
            if (!stack.isEmpty()) {
                data.addRefund(entry.getUUID("Player"), stack);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        refunds.forEach((playerId, stacks) -> {
            for (ItemStack stack : stacks) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Player", playerId);
                entry.put("Stack", stack.save(registries));
                entries.add(entry);
            }
        });
        tag.put("Refunds", entries);
        return tag;
    }
}
