package cn.kurt6.unknown_echoes.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 背包支付原语。清点与扣除必须遍历同一批槽位(items + offhand,盔甲槽不参与):
 * 数得到扣不掉是白拿,扣得掉数不到是玩家亏损。
 */
public final class InventoryPayment {

    private InventoryPayment() {
    }

    /** 背包或副手是否还有空槽放绑定物。必须与 placeBound 同口径,否则归还时物品会消失。 */
    public static boolean hasBoundItemSpace(ServerPlayer player) {
        if (player.getInventory().getFreeSlot() >= 0) {
            return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把绑定物放回背包空槽,放不下返回 false(调用方须保留待归还标记)。
     * 整槽写入而不用 Inventory.add:绑定物带序列号组件,并栈会把两件合成一件。
     */
    public static boolean placeBound(ServerPlayer player, ItemStack stack) {
        int slot = player.getInventory().getFreeSlot();
        if (slot >= 0) {
            player.getInventory().items.set(slot, stack);
            player.getInventory().setChanged();
            return true;
        }
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            if (player.getInventory().offhand.get(i).isEmpty()) {
                player.getInventory().offhand.set(i, stack);
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    /** 玩家可用于支付的该物品总数(主背包 + 副手)。 */
    public static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 扣除最多 amount 个该物品,返回实际扣掉的数量。调用方必须先用 count 确认够付:
     * 扣除不保证原子性也不回滚。
     */
    public static int consume(ServerPlayer player, Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int remaining = amount;
        remaining -= takeFrom(player.getInventory().items, item, remaining);
        remaining -= takeFrom(player.getInventory().offhand, item, remaining);
        int consumed = amount - remaining;
        if (consumed > 0) {
            player.getInventory().setChanged();
        }
        return consumed;
    }

    private static int takeFrom(Iterable<ItemStack> slots, Item item, int wanted) {
        int taken = 0;
        for (ItemStack stack : slots) {
            if (taken >= wanted) {
                break;
            }
            if (!stack.is(item)) {
                continue;
            }
            int take = Math.min(wanted - taken, stack.getCount());
            stack.shrink(take);
            taken += take;
        }
        return taken;
    }
}
