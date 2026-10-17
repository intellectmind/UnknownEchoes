package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Iterator;
import java.util.Set;

/** Boss 物品统一在死亡位置生成，每场战斗只由 Boss 结算一次。 */
public final class BossMaterialRewards {
    private static final String LEGACY_PENDING_REWARD_PREFIX = "pending_boss_material|";
    private static final int MAX_LEGACY_REWARD_COUNT = 4096;

    private BossMaterialRewards() {
    }

    public static void drop(LivingEntity boss, ItemStack stack) {
        if (stack.isEmpty() || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BossRewardDropData.dropOrQueue(level, boss.getX(), boss.getY() + 0.5D, boss.getZ(), stack);
    }

    /** 死亡回调可能传送或移除 Boss；已提交物品仍交付到原死亡现场。 */
    public static void drop(ServerLevel level, double x, double y, double z, ItemStack stack) {
        BossRewardDropData.dropOrQueue(level, x, y, z, stack);
    }

    /** 将旧版个人待领记录转交世界队列，在登录位置按重试节拍生成。 */
    public static void loadPendingPersonalRewards(ServerPlayer player) {
        Set<String> mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        Iterator<String> iterator = mechanisms.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.startsWith(LEGACY_PENDING_REWARD_PREFIX)) {
                continue;
            }
            PendingReward reward = parseLegacyReward(key);
            if (reward != null) {
                BossRewardDropData.queueForRetry(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                        new ItemStack(reward.item(), reward.count()));
            }
            iterator.remove();
        }
    }

    private static PendingReward parseLegacyReward(String key) {
        String[] parts = key.substring(LEGACY_PENDING_REWARD_PREFIX.length()).split("\\|", 4);
        if (parts.length < 3) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(parts[1]);
        int count;
        try {
            count = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (itemId == null || count <= 0 || count > MAX_LEGACY_REWARD_COUNT) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        return item == null || item == Items.AIR ? null : new PendingReward(item, count);
    }

    private record PendingReward(Item item, int count) {
    }
}
