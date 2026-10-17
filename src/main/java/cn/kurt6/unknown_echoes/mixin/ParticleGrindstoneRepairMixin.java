package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleRepair;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GrindstoneMenu.class)
public abstract class ParticleGrindstoneRepairMixin {
    @ModifyReturnValue(method = "computeResult", at = @At("RETURN"))
    private ItemStack unknownEchoes$preserveParticles(ItemStack result, ItemStack top, ItemStack bottom) {
        return ParticleRepair.preserve(result, top, bottom);
    }

    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onGrindstoneChange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;I)I"), require = 1, allow = 1)
    private int unknownEchoes$repairEvent(ItemStack top, ItemStack bottom, Container output, int xp, Operation<Integer> original) {
        if (ParticleRepair.conflicts(top, bottom)) {
            output.setItem(0, ItemStack.EMPTY);
            return -1;
        }
        int result = original.call(top, bottom, output, xp);
        if (result != Integer.MIN_VALUE) output.setItem(0, ParticleRepair.preserve(output.getItem(0), top, bottom));
        return result;
    }
}
