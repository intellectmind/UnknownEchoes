package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.event.EffectControlSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ControlEffectMixin {
    private static final String ADD = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z";

    @WrapOperation(method = "removeEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectNoUpdate(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"),
            require = 1, allow = 1)
    private MobEffectInstance unknownEchoes$guardControlRemoval(LivingEntity entity, Holder<MobEffect> effect,
                                                            Operation<MobEffectInstance> original) {
        return EffectControlSupport.canRemove(entity, effect) ? original.call(entity, effect) : null;
    }

    @WrapOperation(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;canMobEffectBeApplied(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"),
            require = 1, allow = 1)
    private boolean unknownEchoes$guardControlApplication(LivingEntity entity, MobEffectInstance incoming, Entity source,
                                                       Operation<Boolean> original) {
        return original.call(entity, incoming, source) && EffectControlSupport.canApply(entity, incoming);
    }

    @Inject(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;", shift = At.Shift.AFTER),
            cancellable = true, require = 1, allow = 1)
    private void unknownEchoes$guardControlCommit(MobEffectInstance incoming, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if (!EffectControlSupport.canApply((LivingEntity) (Object) this, incoming)) {
            callback.setReturnValue(false);
        }
    }
}
