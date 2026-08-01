package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class WorldtreeFruitItem extends Item {
    public static final int MAX_USES = 3;
    public static final double HEALTH_PER_USE = 5.0D;

    private static final String USES_KEY = "ue_worldtree_fruit_uses";
    private static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER_ID =
            UnknownEchoes.id("worldtree_fruit_health");

    public WorldtreeFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hasReachedUseLimit(level, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.worldtree_fruit.limit"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            int uses = Math.min(MAX_USES, getUses(player) + 1);
            player.getPersistentData().putInt(USES_KEY, uses);
            applyHealthBonus(player);
            player.heal((float) HEALTH_PER_USE);
            playConsumptionEffects(player);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.worldtree_fruit.used", uses, MAX_USES), true);
        }
        return result;
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

    private static boolean hasReachedUseLimit(Level level, Player player) {
        if (!level.isClientSide) {
            return getUses(player) >= MAX_USES;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = maxHealth == null ? null : maxHealth.getModifier(HEALTH_MODIFIER_ID);
        return modifier != null && modifier.amount() >= MAX_USES * HEALTH_PER_USE;
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
