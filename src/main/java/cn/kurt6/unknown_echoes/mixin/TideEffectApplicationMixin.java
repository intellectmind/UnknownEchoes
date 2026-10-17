package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.artifact.TideEffectApplication;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class TideEffectApplicationMixin {
    private static final String ADD = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z";

    @WrapOperation(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;canMobEffectBeApplied(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"),
            require = 1, allow = 1)
    private boolean unknownEchoes$beginTideApplication(LivingEntity entity, MobEffectInstance incoming, Entity source,
                                                       Operation<Boolean> original) {
        TideEffectApplication.enter(entity, incoming);
        return original.call(entity, incoming, source) && TideEffectApplication.canApply(entity, incoming);
    }

    @Inject(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"),
            require = 1, allow = 1)
    private void unknownEchoes$captureTideApplication(MobEffectInstance incoming, Entity source, CallbackInfoReturnable<Boolean> callback) {
        TideEffectApplication.capture((LivingEntity) (Object) this, incoming);
    }

    @Inject(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;", shift = At.Shift.AFTER),
            cancellable = true, require = 1, allow = 1)
    private void unknownEchoes$checkTideApplication(MobEffectInstance incoming, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if (!TideEffectApplication.canCommit((LivingEntity) (Object) this, incoming)) callback.setReturnValue(false);
    }

    @WrapOperation(method = ADD, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 1, allow = 1)
    private Object unknownEchoes$commitNewTideEffect(Map<?, ?> effects, Object key, Object incoming, Operation<Object> original) {
        Object result = original.call(effects, key, incoming);
        TideEffectApplication.committed((LivingEntity) (Object) this, (MobEffectInstance) incoming);
        return result;
    }

    @WrapOperation(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 1, allow = 1)
    private boolean unknownEchoes$commitTideUpdate(MobEffectInstance current, MobEffectInstance incoming, Operation<Boolean> original) {
        boolean changed = original.call(current, incoming);
        TideEffectApplication.committed((LivingEntity) (Object) this, incoming);
        return changed;
    }

    @WrapOperation(method = ADD, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;onEffectStarted(Lnet/minecraft/world/entity/LivingEntity;)V"), require = 1, allow = 1)
    private void unknownEchoes$startTideEffect(MobEffectInstance incoming, LivingEntity entity, Operation<Void> original) {
        if (TideEffectApplication.canStart(entity, incoming)) {
            original.call(incoming, entity);
            TideEffectApplication.started(entity, incoming);
        }
    }
}
