package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 第一维度商人/战利品用轻量消耗品。只提供短时探索支援,不写关键进度。
 */
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
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            applyEffect(entity);
        }
        return result;
    }

    private void applyEffect(LivingEntity entity) {
        switch (kind) {
            case ECHO_BANDAGE -> {
                entity.heal(4.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
            }
            case WIND_CHIME_BOTTLE -> {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0));
            }
            case TIDE_BREATH_BOTTLE -> {
                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 300, 0));
            }
            case REVEALING_POWDER -> {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
                if (entity instanceof ServerPlayer player) {
                    revealNearby(player);
                }
            }
            case MEMORY_ECHO_STONE -> {
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1800, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            }
        }
    }

    private static void revealNearby(ServerPlayer player) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(16.0D),
                target -> target != player && target.isAlive());
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.material." + tooltipKey + ".note")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
