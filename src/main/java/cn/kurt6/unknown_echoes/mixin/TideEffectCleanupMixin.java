package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class TideEffectCleanupMixin {
    @WrapOperation(method = "removeEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectNoUpdate(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"),
            require = 1, allow = 1)
    private MobEffectInstance unknownEchoes$guardTideRemoval(LivingEntity entity, Holder<MobEffect> effect,
                                                            Operation<MobEffectInstance> original) {
        return ArtifactEffectManager.canRemoveTideEffect(entity, effect) ? original.call(entity, effect) : null;
    }

    @WrapOperation(method = "forceAddEffect", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;canMobEffectBeApplied(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"),
            require = 1, allow = 1)
    private boolean unknownEchoes$guardTideFallback(LivingEntity entity, MobEffectInstance incoming, Entity source,
                                                   Operation<Boolean> original) {
        return original.call(entity, incoming, source) && ArtifactEffectManager.canReplaceTideEffect(entity, incoming);
    }
}
