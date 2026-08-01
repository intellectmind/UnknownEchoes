package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 可食用的回响能量补给。能量结算只在服务端进行。 */
public final class EchoEnergyConsumableItem extends Item {
    private final int restoredEnergy;

    public EchoEnergyConsumableItem(int restoredEnergy, Properties properties) {
        super(properties);
        this.restoredEnergy = restoredEnergy;
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
                    && ArtifactManager.getEnergy(serverPlayer) >= ArtifactManager.getMaxEnergy();
        }
        return full ? InteractionResultHolder.fail(stack) : super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player
                && ArtifactManager.getEnergy(player) >= ArtifactManager.getMaxEnergy()) {
            return stack;
        }
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            ArtifactManager.refillEnergy(player, restoredEnergy);
        }
        return result;
    }
}
