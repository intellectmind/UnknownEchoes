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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** 深层回响生态食物，只提供探索增益，不写入任何进度。 */
public final class DeepEchoFoodItem extends Item {
    private final Kind kind;
    private final String tooltipKey;

    public DeepEchoFoodItem(Kind kind, String tooltipKey, Properties properties) {
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
            case HUSH_THORN_CANDY -> 16;
            case FOLDED_SPORE_CRISP, GRAVITY_BELL_JELLY -> 20;
            case WEIGHTLESS_BLOOM_TEA -> 24;
            case ARCHIVE_SPORE_ROLL -> 28;
            case VOIDCAP_STEW -> 32;
        };
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.WEIGHTLESS_BLOOM_TEA ? UseAnim.DRINK : UseAnim.EAT;
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
            case ARCHIVE_SPORE_ROLL -> entity.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED, 1200, 1, true, false, true));
            case WEIGHTLESS_BLOOM_TEA -> {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING, 2400, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(
                        MobEffects.JUMP, 900, 1, true, false, true));
                entity.fallDistance = 0.0F;
            }
            case VOIDCAP_STEW -> {
                entity.removeEffect(MobEffects.DARKNESS);
                entity.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, 3600, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 400, 0, true, false, true));
            }
            case FOLDED_SPORE_CRISP -> entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 900, 1, true, false, true));
            case GRAVITY_BELL_JELLY -> {
                entity.fallDistance = 0.0F;
                FoodEffectSupport.applyAbsorption(entity, 900, 1, 8.0F);
            }
            case HUSH_THORN_CANDY -> {
                entity.removeEffect(MobEffects.GLOWING);
                entity.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, 600, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 600, 1, true, false, true));
            }
        }
        return addContainerRemainder(result, entity);
    }

    private boolean canBenefit(Player player) {
        if (player.getFoodData().getFoodLevel() < 20) {
            return true;
        }
        return switch (kind) {
            case ARCHIVE_SPORE_ROLL -> needsEffectRefresh(
                    player, MobEffects.DIG_SPEED, 1200, 1);
            case WEIGHTLESS_BLOOM_TEA -> player.fallDistance > 0.0F
                    || needsEffectRefresh(player, MobEffects.SLOW_FALLING, 2400, 0)
                    || needsEffectRefresh(player, MobEffects.JUMP, 900, 1);
            case VOIDCAP_STEW -> player.hasEffect(MobEffects.DARKNESS)
                    || needsEffectRefresh(player, MobEffects.NIGHT_VISION, 3600, 0)
                    || needsEffectRefresh(player, MobEffects.DAMAGE_RESISTANCE, 400, 0);
            case FOLDED_SPORE_CRISP -> needsEffectRefresh(
                    player, MobEffects.MOVEMENT_SPEED, 900, 1);
            case GRAVITY_BELL_JELLY -> player.fallDistance > 0.0F
                    || FoodEffectSupport.needsAbsorptionRefresh(player, 900, 1, 8.0F);
            case HUSH_THORN_CANDY -> player.hasEffect(MobEffects.GLOWING)
                    || needsEffectRefresh(player, MobEffects.INVISIBILITY, 600, 0)
                    || needsEffectRefresh(player, MobEffects.MOVEMENT_SPEED, 600, 1);
        };
    }

    private static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    private ItemStack addContainerRemainder(ItemStack result, LivingEntity entity) {
        Item container = switch (kind) {
            case ARCHIVE_SPORE_ROLL, FOLDED_SPORE_CRISP,
                    GRAVITY_BELL_JELLY, HUSH_THORN_CANDY -> null;
            case WEIGHTLESS_BLOOM_TEA -> Items.GLASS_BOTTLE;
            case VOIDCAP_STEW -> Items.BOWL;
        };
        if (container == null || !(entity instanceof Player player)
                || player.getAbilities().instabuild) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.unknown_echoes.food." + tooltipKey + ".effect")
                .withStyle(ChatFormatting.AQUA));
    }

    public enum Kind {
        ARCHIVE_SPORE_ROLL,
        WEIGHTLESS_BLOOM_TEA,
        VOIDCAP_STEW,
        FOLDED_SPORE_CRISP,
        GRAVITY_BELL_JELLY,
        HUSH_THORN_CANDY
    }
}
