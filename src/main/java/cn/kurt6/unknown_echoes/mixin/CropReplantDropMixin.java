package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.event.CommonEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 在所有掉落监听器结束后、物品实体生成前结算补种与范围采掘。 */
@Mixin(value = CommonHooks.class, remap = false)
public abstract class CropReplantDropMixin {
    @WrapOperation(
            method = "handleBlockDrops",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/level/BlockDropsEvent;isCanceled()Z"),
            remap = false, require = 1, allow = 1)
    private static boolean unknownEchoes$resolveCropReplant(BlockDropsEvent event,
                                                           Operation<Boolean> original) {
        boolean canceled = original.call(event);
        if (!canceled) {
            CommonEvents.onBlockDropsResolved(event);
        }
        return canceled;
    }
}
