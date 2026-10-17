package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.entity.boss.BossHealthSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class BossHealthInitializationMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getMaxHealth()F"), require = 1, allow = 1)
    private float unknownEchoes$initializeBossHealth(LivingEntity entity, Operation<Float> original) {
        BossHealthSupport.initialize(entity);
        return original.call(entity);
    }
}
