package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.event.ArmorHealthLoadSupport;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ArmorHealthLoadMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"), require = 1, allow = 1)
    private void unknownEchoes$restoreArmorHealth(CompoundTag compound, CallbackInfo callback) {
        ArmorHealthLoadSupport.afterLoad((ServerPlayer) (Object) this, compound);
    }

    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"), require = 2, allow = 2)
    private void unknownEchoes$captureCopiedHealth(ServerPlayer player, float health, Operation<Void> original,
            ServerPlayer oldPlayer, boolean keepEverything,
            @Share("copiedHealth") LocalRef<ArmorHealthLoadSupport.CopiedHealth> copied) {
        float maximum = player.getMaxHealth();
        var level = player.level();
        float absorption = oldPlayer.getAbsorptionAmount();
        boolean hadFullSet = EchoArmorSets.resonatorPieces(oldPlayer) >= 4;
        original.call(player, health);
        copied.set(new ArmorHealthLoadSupport.CopiedHealth(health, maximum, player.getHealth(), level,
                absorption, hadFullSet));
    }

    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;onPlayerClone(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/player/Player;Z)V"), require = 1, allow = 1)
    private void unknownEchoes$restoreCopiedHealth(Player player, Player oldPlayer, boolean wasDeath,
            Operation<Void> original, @Share("copiedHealth") LocalRef<ArmorHealthLoadSupport.CopiedHealth> copied) {
        float absorptionBeforeClone = player.getAbsorptionAmount();
        original.call(player, oldPlayer, wasDeath);
        ArmorHealthLoadSupport.afterClone((ServerPlayer) player, copied.get(), wasDeath, absorptionBeforeClone);
    }
}
