package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class NightVisionCleanupMixin {
    @WrapOperation(method = "removeEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectNoUpdate(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"),
            require = 1, allow = 1)
    private MobEffectInstance unknownEchoes$guardNightVisionCleanup(LivingEntity entity, Holder<MobEffect> effect,
                                                                  Operation<MobEffectInstance> original) {
        return NightVisionSupport.canRemoveDuringCleanup(entity, effect) ? original.call(entity, effect) : null;
    }
}
