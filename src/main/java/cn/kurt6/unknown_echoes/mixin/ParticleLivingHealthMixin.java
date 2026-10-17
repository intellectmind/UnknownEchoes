package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleCombat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ParticleLivingHealthMixin {
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"), require = 1, allow = 1)
    private void unknownEchoes$measureHealth(LivingEntity target, float health, Operation<Void> original) {
        float before = target.getHealth();
        original.call(target, health);
        ParticleCombat.healthChanged(target, before, target.getHealth());
    }
}
