package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 沉默荚果:失语沼泽独有食材,吃下后清除所有负面状态效果(服务端判定)。 */
public class SilentPodItem extends Item {

    public SilentPodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!canBenefit(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 16;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !canBenefit(player)) {
            return stack;
        }
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            List<MobEffectInstance> harmful = entity.getActiveEffects().stream()
                    .filter(effect -> !effect.getEffect().value().isBeneficial())
                    .toList();
            for (MobEffectInstance effect : harmful) {
                entity.removeEffect(effect.getEffect());
            }
        }
        return result;
    }

    private static boolean canBenefit(Player player) {
        return player.getFoodData().getFoodLevel() < 20 || hasHarmfulEffect(player);
    }

    private static boolean hasHarmfulEffect(LivingEntity entity) {
        return entity.getActiveEffects().stream()
                .anyMatch(effect -> !effect.getEffect().value().isBeneficial());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.material.silent_pod.effect")
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
