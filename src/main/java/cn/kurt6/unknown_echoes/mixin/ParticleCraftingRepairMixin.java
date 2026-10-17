package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleRepair;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RepairItemRecipe.class)
public abstract class ParticleCraftingRepairMixin {
    @ModifyReturnValue(method = "canCombine", at = @At("RETURN"))
    private static boolean unknownEchoes$allowRepair(boolean allowed, ItemStack first, ItemStack second) {
        return allowed && !ParticleRepair.conflicts(first, second);
    }

    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack unknownEchoes$preserveParticles(ItemStack result, CraftingInput input, HolderLookup.Provider registries) {
        return ParticleRepair.craftingResult(result, input);
    }
}
