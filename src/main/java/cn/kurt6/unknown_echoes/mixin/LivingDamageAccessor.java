package cn.kurt6.unknown_echoes.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Stack;

@Mixin(LivingEntity.class)
public interface LivingDamageAccessor {
    @Accessor(value = "damageContainers", remap = false)
    Stack<DamageContainer> unknownEchoes$getDamageContainers();
}
