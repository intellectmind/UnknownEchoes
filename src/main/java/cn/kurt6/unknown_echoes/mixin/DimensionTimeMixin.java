package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.world.RealmTimeCycle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public abstract class DimensionTimeMixin {
    @Shadow
    public abstract ResourceLocation effectsLocation();

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void unknownEchoes$darkCycle(long dayTime, CallbackInfoReturnable<Float> callback) {
        ResourceLocation effects = effectsLocation();
        if (!effects.getNamespace().equals("unknown_echoes")) return;
        if (effects.getPath().equals("echo_realm")) {
            callback.setReturnValue(RealmTimeCycle.angle(dayTime, false));
        } else if (effects.getPath().equals("deep_echoes")) {
            callback.setReturnValue(RealmTimeCycle.angle(dayTime, true));
        }
    }
}
