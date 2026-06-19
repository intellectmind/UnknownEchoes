package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 沉默荚果:失语沼泽独有食材。吃下后"把杂音一并咽下"——
 * 清除所有负面状态效果(服务端判定)。普通材料,可正常掉落与交易。
 */
public class SilentPodItem extends Item {

    public SilentPodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.material.silent_pod.note")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
