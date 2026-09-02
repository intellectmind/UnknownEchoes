package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SimpleEchoFoodItem extends Item {
    private final String tooltipKey;
    private final Item container;
    private final int useDurationTicks;
    private final UseAnim useAnimation;

    public SimpleEchoFoodItem(String tooltipKey, int useDurationTicks, Properties properties) {
        this(tooltipKey, null, useDurationTicks, UseAnim.EAT, properties);
    }

    public SimpleEchoFoodItem(String tooltipKey, Item container, int useDurationTicks,
                              UseAnim useAnimation, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.container = container;
        this.useDurationTicks = useDurationTicks;
        this.useAnimation = useAnimation;
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
        return useDurationTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnimation;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !canBenefit(player)) {
            return stack;
        }
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            switch (tooltipKey) {
                case "moss_honey_stew" ->
                        FoodEffectSupport.applyAbsorption(entity, 400, 0, 4.0F);
                case "dream_nectar_tart" ->
                        FoodEffectSupport.applyAbsorption(entity, 600, 0, 4.0F);
                default -> {
                }
            }
        }
        if (level.isClientSide || container == null
                || !(entity instanceof Player player) || player.getAbilities().instabuild) {
            return result;
        }
        ItemStack remainder = new ItemStack(container);
        if (result.isEmpty()) {
            return remainder;
        }
        if (!player.getInventory().add(remainder)) {
            player.drop(remainder, false);
        }
        return result;
    }

    private boolean canBenefit(Player player) {
        if (player.getFoodData().getFoodLevel() < 20) {
            return true;
        }
        return switch (tooltipKey) {
            case "star_mist_petal" -> needsEffectRefresh(
                    player, MobEffects.SLOW_FALLING, 240, 0);
            case "mirror_lotus_root" -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 600, 0)
                    || needsEffectRefresh(player, MobEffects.NIGHT_VISION, 600, 0);
            case "wind_crisp" -> needsEffectRefresh(
                    player, MobEffects.SLOW_FALLING, 600, 0);
            case "moss_honey_stew" -> needsEffectRefresh(
                    player, MobEffects.REGENERATION, 300, 0)
                    || FoodEffectSupport.needsAbsorptionRefresh(player, 400, 0, 4.0F);
            case "tide_salted_fish" -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 900, 0);
            case "dream_nectar_tart" -> needsEffectRefresh(
                    player, MobEffects.REGENERATION, 240, 0)
                    || FoodEffectSupport.needsAbsorptionRefresh(player, 600, 0, 4.0F);
            case "glow_fruit_jam" -> needsEffectRefresh(
                    player, MobEffects.NIGHT_VISION, 1800, 0);
            case "tide_glass_eel" -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 400, 0);
            case "tide_kelp_wrap" -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 1800, 0)
                    || needsEffectRefresh(player, MobEffects.DOLPHINS_GRACE, 300, 0);
            case "mirror_tail_fish" -> needsEffectRefresh(
                    player, MobEffects.NIGHT_VISION, 600, 0);
            case "echo_cuttlefish_strip" -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 600, 0);
            case "pearlback_fillet" -> needsEffectRefresh(
                    player, MobEffects.DOLPHINS_GRACE, 300, 0);
            default -> true;
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
}
