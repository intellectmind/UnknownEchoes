package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class TimeFreezeSaveMixin {
    @Inject(method = "saveWithoutId", at = @At("RETURN"), require = 1, allow = 1)
    private void unknownEchoes$saveFrozenMotion(CompoundTag compound, CallbackInfoReturnable<CompoundTag> callback) {
        ArtifactEffectManager.saveEntityMotion((Entity) (Object) this, callback.getReturnValue());
    }
}
