package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class WorldtreeFruitItem extends Item {
    public static final int MAX_USES = 3;
    public static final double HEALTH_PER_USE = 5.0D;
    public static final float SURPLUS_HEAL = 12.0F;
    public static final int SURPLUS_REGEN_TICKS = 200;
    public static final int SURPLUS_ABSORPTION_TICKS = 1200;

    private static final String USES_KEY = "ue_worldtree_fruit_uses";
    private static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER_ID =
            UnknownEchoes.id("worldtree_fruit_health");

    public WorldtreeFruitItem(Properties properties) {
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
        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !canBenefit(player)) {
            return stack;
        }
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            int currentUses = getUses(player);
            if (currentUses < MAX_USES) {
                int uses = currentUses + 1;
                player.getPersistentData().putInt(USES_KEY, uses);
                applyHealthBonus(player);
                player.heal((float) HEALTH_PER_USE);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.worldtree_fruit.used", uses, MAX_USES), true);
            } else {
                player.heal(SURPLUS_HEAL);
                player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, SURPLUS_REGEN_TICKS, 1, true, false, true));
                FoodEffectSupport.applyAbsorption(
                        player, SURPLUS_ABSORPTION_TICKS, 1, 8.0F);
            }
            playConsumptionEffects(player);
        }
        return result;
    }

    private static boolean canBenefit(Player player) {
        if (getUses(player) < MAX_USES) {
            return true;
        }
        return player.getFoodData().getFoodLevel() < 20
                || player.getHealth() < player.getMaxHealth()
                || needsEffectRefresh(player, MobEffects.REGENERATION, SURPLUS_REGEN_TICKS, 1)
                || FoodEffectSupport.needsAbsorptionRefresh(
                player, SURPLUS_ABSORPTION_TICKS, 1, 8.0F);
    }

    private static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.worldtree_fruit.effect")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.worldtree_fruit.surplus")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static void copyPersistentData(Player original, Player replacement) {
        replacement.getPersistentData().putInt(USES_KEY, getUses(original));
        applyHealthBonus(replacement);
    }

    public static void applyHealthBonus(Player player) {
        int uses = getUses(player);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (uses <= 0) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
            return;
        }
        maxHealth.addOrReplacePermanentModifier(new AttributeModifier(
                HEALTH_MODIFIER_ID,
                uses * HEALTH_PER_USE,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static int getUses(Player player) {
        int uses = Math.clamp(player.getPersistentData().getInt(USES_KEY), 0, MAX_USES);
        if (player.getPersistentData().getInt(USES_KEY) != uses) {
            player.getPersistentData().putInt(USES_KEY, uses);
        }
        return uses;
    }

    private static void playConsumptionEffects(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                28, 0.65D, 0.9D, 0.65D, 0.035D);
        level.sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.1D, player.getZ(),
                8, 0.5D, 0.65D, 0.5D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, 0.85F);
    }
}
