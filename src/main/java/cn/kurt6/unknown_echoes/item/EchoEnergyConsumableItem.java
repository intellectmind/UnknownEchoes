package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class EchoEnergyConsumableItem extends Item {
    private final int restoredEnergy;
    private final int useDurationTicks;

    public EchoEnergyConsumableItem(int restoredEnergy, int useDurationTicks, Properties properties) {
        super(properties);
        this.restoredEnergy = restoredEnergy;
        this.useDurationTicks = useDurationTicks;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useDurationTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean full;
        if (level.isClientSide) {
            int energy = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getEnergy();
            full = energy >= 0 && energy >= cn.kurt6.unknown_echoes.client.ClientArtifactCache.getMaxEnergy();
        } else {
            full = player instanceof ServerPlayer serverPlayer
                    && ArtifactManager.getEnergy(serverPlayer) >= ArtifactManager.getMaxEnergy(serverPlayer);
        }
        return full ? InteractionResultHolder.fail(stack) : super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return ConsumableCompletion.finish(this, stack, entity, () -> {
            if (!level.isClientSide && entity instanceof ServerPlayer player
                    && ArtifactManager.getEnergy(player) >= ArtifactManager.getMaxEnergy(player)) {
                return stack;
            }
            ItemStack result = super.finishUsingItem(stack, level, entity);
            if (!level.isClientSide && entity instanceof ServerPlayer player) {
                ArtifactManager.refillEnergy(player, restoredEnergy);
            }
            return result;
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_candy.energy", restoredEnergy)
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
