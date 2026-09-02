package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 三域公共设施料理，只提供普通探索增益。 */
public final class RealmCuisineItem extends Item {
    private final Kind kind;
    private final String tooltipKey;

    public RealmCuisineItem(Kind kind, String tooltipKey, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
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
        return switch (kind) {
            case FAR_BELL_TRAIL_LOAF, PRISMATIC_CANOPY_TART -> 28;
            case INTERLAYER_PROVISION_PIE -> 32;
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !canBenefit(player)) {
            return stack;
        }
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide) {
            return result;
        }

        switch (kind) {
            case FAR_BELL_TRAIL_LOAF -> {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 1800, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SPEED, 1200, 0, true, false, true));
            }
            case PRISMATIC_CANOPY_TART -> {
                entity.removeEffect(MobEffects.BLINDNESS);
                entity.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, 3600, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 200, 1, true, false, true));
            }
            case INTERLAYER_PROVISION_PIE -> {
                entity.removeEffect(MobEffects.DARKNESS);
                entity.fallDistance = 0.0F;
                FoodEffectSupport.applyAbsorption(entity, 1800, 1, 8.0F);
                entity.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 600, 0, true, false, true));
            }
        }
        return result;
    }

    private boolean canBenefit(Player player) {
        if (player.getFoodData().getFoodLevel() < 20) {
            return true;
        }
        return switch (kind) {
            case FAR_BELL_TRAIL_LOAF -> needsEffectRefresh(
                    player, MobEffects.MOVEMENT_SPEED, 1800, 0)
                    || needsEffectRefresh(player, MobEffects.DIG_SPEED, 1200, 0);
            case PRISMATIC_CANOPY_TART -> player.hasEffect(MobEffects.BLINDNESS)
                    || needsEffectRefresh(player, MobEffects.NIGHT_VISION, 3600, 0)
                    || needsEffectRefresh(player, MobEffects.REGENERATION, 200, 1);
            case INTERLAYER_PROVISION_PIE -> player.hasEffect(MobEffects.DARKNESS)
                    || player.fallDistance > 0.0F
                    || FoodEffectSupport.needsAbsorptionRefresh(player, 1800, 1, 8.0F)
                    || needsEffectRefresh(player, MobEffects.DAMAGE_RESISTANCE, 600, 0);
        };
    }

    private static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.unknown_echoes.food." + tooltipKey + ".effect")
                .withStyle(ChatFormatting.AQUA));
    }

    public enum Kind {
        FAR_BELL_TRAIL_LOAF,
        PRISMATIC_CANOPY_TART,
        INTERLAYER_PROVISION_PIE
    }
}
