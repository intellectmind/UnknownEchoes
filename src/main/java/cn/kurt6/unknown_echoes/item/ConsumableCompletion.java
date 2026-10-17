package cn.kurt6.unknown_echoes.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** 食物和轻量补给共用的内部完成结算边界。 */
final class ConsumableCompletion {
    private static final ThreadLocal<Set<UUID>> FINISHING_USERS = ThreadLocal.withInitial(HashSet::new);

    private ConsumableCompletion() {
    }

    static ItemStack finish(Item item, ItemStack stack, LivingEntity entity,
                            Supplier<ItemStack> consumption) {
        if (!stack.is(item)) return stack;
        Set<UUID> finishing = FINISHING_USERS.get();
        if (!finishing.add(entity.getUUID())) return stack;
        try {
            InteractionHand hand = entity.isUsingItem() && entity.getUseItem() == stack
                    ? entity.getUsedItemHand() : null;
            ItemStack result = consumption.get();
            return finishResult(stack, result, entity, hand);
        } finally {
            finishing.remove(entity.getUUID());
            if (finishing.isEmpty()) FINISHING_USERS.remove();
        }
    }

    private static ItemStack finishResult(ItemStack original, ItemStack result,
                                         LivingEntity entity, InteractionHand hand) {
        if (hand == null || entity.getItemInHand(hand) == original) return result;
        // 回调可能换掉手持物；容器另行交付，让原版收尾保留当前手持栈。
        if (result != original && !result.isEmpty() && entity instanceof Player player) {
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        return entity.getItemInHand(hand);
    }
}
