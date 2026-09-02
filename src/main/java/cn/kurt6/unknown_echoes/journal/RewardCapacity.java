package cn.kurt6.unknown_echoes.journal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 发奖前的背包容量试算。一次性奖励必须先确认整批放得下再写进度,否则会停在"进度已记、物品丢了"。
 * 试算与 Inventory.add(-1, stack) 的落点顺序对齐:空副手与盔甲槽不算容量。
 */
public final class RewardCapacity {

    private RewardCapacity() {
    }

    /** 整批奖励是否都能进背包。任一件放不下即返回 false，不做部分发放。 */
    public static boolean canFitAll(ServerPlayer player, List<ItemStack> rewards) {
        List<ItemStack> slots = new ArrayList<>();
        List<ItemStack> offhand = new ArrayList<>();
        for (ItemStack slot : player.getInventory().items) {
            slots.add(slot.copy());
        }
        for (ItemStack slot : player.getInventory().offhand) {
            offhand.add(slot.copy());
        }
        for (ItemStack reward : rewards) {
            if (!simulateInsert(slots, offhand, reward.copy())) {
                return false;
            }
        }
        return true;
    }

    /** 在副本槽位上模拟一次插入；会就地修改 slots/offhand 以累积多件奖励的占用。 */
    private static boolean simulateInsert(List<ItemStack> slots, List<ItemStack> offhand,
                                          ItemStack reward) {
        int remaining = reward.getCount();
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            remaining -= merge(slot, reward, remaining);
        }
        for (ItemStack slot : offhand) {
            if (remaining <= 0) {
                return true;
            }
            remaining -= merge(slot, reward, remaining);
        }
        for (int i = 0; i < slots.size(); i++) {
            if (remaining <= 0) {
                return true;
            }
            if (slots.get(i).isEmpty()) {
                ItemStack placed = reward.copy();
                int move = Math.min(remaining, placed.getMaxStackSize());
                placed.setCount(move);
                slots.set(i, placed);
                remaining -= move;
            }
        }
        return remaining <= 0;
    }

    private static int merge(ItemStack slot, ItemStack reward, int remaining) {
        if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, reward)) {
            return 0;
        }
        int move = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
        if (move <= 0) {
            return 0;
        }
        slot.grow(move);
        return move;
    }
}
