package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.particle.ParticleEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ParticleTeleportMixin {
    @Shadow public ServerPlayer player;

    // 直接传送 API 和位置校正也会进入此处，不依赖用途各异的传送事件。
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void unknownEchoes$particleTeleport(double x, double y, double z, float yaw, float pitch,
                                               Set<RelativeMovement> relative, CallbackInfo callback) {
        ParticleEffects.interruptFall(player);
    }
}
