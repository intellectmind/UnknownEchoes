package cn.kurt6.unknown_echoes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class DamageContainerScopeMixin {
    @WrapMethod(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    private boolean unknownEchoes$closeDamageScope(DamageSource source, float amount, Operation<Boolean> original) {
        Stack<DamageContainer> containers = ((LivingDamageAccessor) this).unknownEchoes$getDamageContainers();
        int entryDepth = containers.size();
        try {
            return original.call(source, amount);
        } finally {
            // Cancellation and callback exceptions can skip NeoForge's native pop.
            // Nested attacks must leave the caller's container in place.
            while (containers.size() > entryDepth) containers.pop();
        }
    }
}
