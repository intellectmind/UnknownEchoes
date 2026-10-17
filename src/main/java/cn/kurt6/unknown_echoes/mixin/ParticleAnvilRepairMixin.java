package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleRepair;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AnvilMenu.class)
public abstract class ParticleAnvilRepairMixin {
    @ModifyArg(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"), index = 1)
    private ItemStack unknownEchoes$preserveParticles(ItemStack result) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        return ParticleRepair.preserve(result, menu.getSlot(0).getItem(), menu.getSlot(1).getItem());
    }

    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onAnvilChange(Lnet/minecraft/world/inventory/AnvilMenu;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;Ljava/lang/String;JLnet/minecraft/world/entity/player/Player;)Z"), require = 1, allow = 1)
    private boolean unknownEchoes$repairEvent(AnvilMenu menu, ItemStack left, ItemStack right, Container output,
                                            String name, long cost, Player player, Operation<Boolean> original) {
        if (ParticleRepair.conflicts(left, right)) {
            output.setItem(0, ItemStack.EMPTY);
            menu.setMaximumCost(0);
            menu.repairItemCountCost = 0;
            return false;
        }
        boolean vanilla = original.call(menu, left, right, output, name, cost, player);
        if (!vanilla) output.setItem(0, ParticleRepair.preserve(output.getItem(0), left, right));
        return vanilla;
    }
}
