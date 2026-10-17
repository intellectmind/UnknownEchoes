package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** 第一维度商人/战利品用轻量消耗品。只提供短时探索支援,不写关键进度。 */
public class EchoUtilityConsumableItem extends Item {
    public enum Kind {
        ECHO_BANDAGE,
        WIND_CHIME_BOTTLE,
        TIDE_BREATH_BOTTLE,
        REVEALING_POWDER,
        MEMORY_ECHO_STONE
    }

    private final String tooltipKey;
    private final Kind kind;

    public EchoUtilityConsumableItem(String tooltipKey, Kind kind, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.kind = kind;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return switch (kind) {
            case ECHO_BANDAGE, REVEALING_POWDER -> 16;
            case WIND_CHIME_BOTTLE, TIDE_BREATH_BOTTLE -> 24;
            case MEMORY_ECHO_STONE -> 32;
        };
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (kind) {
            case WIND_CHIME_BOTTLE, TIDE_BREATH_BOTTLE -> UseAnim.DRINK;

            case ECHO_BANDAGE, REVEALING_POWDER -> UseAnim.BRUSH;
            case MEMORY_ECHO_STONE -> UseAnim.NONE;
        };
    }

    /** 三种非食物消耗品没有 food 组件,必须自己开启使用动作。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!canBenefit(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (usesFoodChannel()) {
            return super.use(level, player, hand);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    private boolean usesFoodChannel() {
        return kind == Kind.WIND_CHIME_BOTTLE || kind == Kind.TIDE_BREATH_BOTTLE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return ConsumableCompletion.finish(this, stack, entity, () -> {
            if (entity instanceof Player player && !canBenefit(player)) {
                return stack;
            }
            ItemStack result;
            if (!usesFoodChannel()) {
                result = finishNonFoodUse(stack, level, entity);
            } else {
                result = super.finishUsingItem(stack, level, entity);
                if (!level.isClientSide) {
                    applyEffect(entity);
                    result = addContainerRemainder(result, entity);
                }
            }
            return result;
        });
    }

    /** 无 food 组件时 super.finishUsingItem 原样返回:既不扣数量也不出声,两者都要自己补。 */
    private ItemStack finishNonFoodUse(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        playFinishSound(level, entity);
        if (!level.isClientSide) {
            applyEffect(entity);
        }
        return stack;
    }

    private boolean canBenefit(Player player) {
        return switch (kind) {
            case ECHO_BANDAGE -> player.getHealth() < player.getMaxHealth()
                    || needsEffectRefresh(player, MobEffects.REGENERATION, 120, 0);
            case WIND_CHIME_BOTTLE -> needsEffectRefresh(
                    player, MobEffects.SLOW_FALLING, 900, 0)
                    || needsEffectRefresh(player, MobEffects.MOVEMENT_SPEED, 400, 1);
            case TIDE_BREATH_BOTTLE -> needsEffectRefresh(
                    player, MobEffects.WATER_BREATHING, 1800, 0)
                    || needsEffectRefresh(player, MobEffects.DOLPHINS_GRACE, 600, 0);
            case REVEALING_POWDER -> needsEffectRefresh(
                    player, MobEffects.NIGHT_VISION, 900, 0)
                    || hasUnrevealedTarget(player);
            case MEMORY_ECHO_STONE -> FoodEffectSupport.needsAbsorptionRefresh(
                    player, 1800, 1, 8.0F)
                    || needsEffectRefresh(player, MobEffects.REGENERATION, 120, 1);
        };
    }

    private static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    private void playFinishSound(Level level, LivingEntity entity) {
        switch (kind) {
            case REVEALING_POWDER -> playFinishSound(level, entity,
                    ModSounds.ITEM_REVEALING_POWDER_USE.get(), 0.9F, 1.0F);
            case MEMORY_ECHO_STONE -> playFinishSound(level, entity,
                    ModSounds.ITEM_MEMORY_ECHO_STONE_USE.get(), 0.9F, 1.0F);
            default -> playFinishSound(level, entity,
                    ModSounds.ITEM_ECHO_BANDAGE_USE.get(), 0.9F, 1.0F);
        }
    }

    private static void playFinishSound(Level level, LivingEntity entity, SoundEvent sound,
                                        float volume, float pitch) {
        level.playSound(entity instanceof Player player ? player : null,
                entity.getX(), entity.getY(), entity.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void applyEffect(LivingEntity entity) {
        switch (kind) {
            case ECHO_BANDAGE -> {
                entity.heal(6.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
            }
            case WIND_CHIME_BOTTLE -> {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 900, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
            }
            case TIDE_BREATH_BOTTLE -> {
                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1800, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
            }
            case REVEALING_POWDER -> {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 900, 0));
                if (entity instanceof ServerPlayer player) {
                    revealNearby(player);
                }
            }
            case MEMORY_ECHO_STONE -> {
                FoodEffectSupport.applyAbsorption(entity, 1800, 1, 8.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            }
        }
    }

    private static boolean hasUnrevealedTarget(Player player) {
        return revealTargets(player).stream().anyMatch(target ->
                needsEffectRefresh(target, MobEffects.GLOWING, 300, 0));
    }

    private static List<LivingEntity> revealTargets(Player player) {
        double range = 24.0D;
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> target != player && target.isAlive()
                        && target.distanceToSqr(player) <= range * range);
    }

    private static void revealNearby(ServerPlayer player) {
        for (LivingEntity target : revealTargets(player)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        }
    }

    private ItemStack addContainerRemainder(ItemStack result, LivingEntity entity) {
        if (kind != Kind.WIND_CHIME_BOTTLE && kind != Kind.TIDE_BREATH_BOTTLE
                || !(entity instanceof Player player) || player.getAbilities().instabuild) {
            return result;
        }
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (result.isEmpty()) {
            return bottle;
        }
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.material." + tooltipKey + ".effect")
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
