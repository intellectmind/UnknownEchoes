package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ParticleStatusRemovalMixin {
    @Inject(method = "onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("HEAD"))
    private void unknownEchoes$clearState(MobEffectInstance effect, CallbackInfo callback) {
        // 移除已被原版确认，护盾余额仍须在撤下吸收上限前清理。
        ParticleEffects.effectRemoved((LivingEntity) (Object) this, effect);
    }
}
