package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Iterator;
import java.util.Set;

/**
 * 主线 Boss 材料结算。首杀个人材料不可因背包满丢失,重复普通材料允许落地补发。
 */
public final class BossMaterialRewards {
    private static final String PENDING_REWARD_PREFIX = "pending_boss_material|";

    private BossMaterialRewards() {
    }

    public static void givePersonal(ServerPlayer player, ResourceLocation bossId, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (canFitEntireStack(player, copy) && player.getInventory().add(copy)) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        EchoAbilityManager.getData(player).getActivatedMechanisms().add(
                PENDING_REWARD_PREFIX + bossId + "|" + itemId + "|" + stack.getCount());
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.miniboss.reward_pending", stack.getHoverName()), true);
    }

    public static void giveOrdinary(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void retryPendingPersonalRewards(ServerPlayer player) {
        Set<String> mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        Iterator<String> iterator = mechanisms.iterator();
        while (iterator.hasNext()) {
            PendingReward reward = parsePendingReward(iterator.next());
            if (reward == null) {
                continue;
            }
            ItemStack stack = new ItemStack(reward.item(), reward.count());
            if (canFitEntireStack(player, stack) && player.getInventory().add(stack)) {
                iterator.remove();
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.miniboss.reward_claimed", stack.getHoverName()), true);
            }
        }
    }

    private static PendingReward parsePendingReward(String key) {
        if (!key.startsWith(PENDING_REWARD_PREFIX)) {
            return null;
        }
        String[] parts = key.substring(PENDING_REWARD_PREFIX.length()).split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(parts[1]);
        int count = parseCount(parts[2]);
        if (itemId == null || count <= 0) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? null : new PendingReward(item, count);
    }

    private static int parseCount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean canFitEntireStack(ServerPlayer player, ItemStack stack) {
        int remaining = stack.getCount();
        int max = stack.getMaxStackSize();
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) {
                remaining -= max;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                remaining -= Math.max(0, max - slot.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private record PendingReward(Item item, int count) {
    }
}
