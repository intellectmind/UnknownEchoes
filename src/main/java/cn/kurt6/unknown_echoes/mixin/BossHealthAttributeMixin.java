package cn.kurt6.unknown_echoes.mixin;

import cn.kurt6.unknown_echoes.entity.boss.BossHealthAttribute;
import cn.kurt6.unknown_echoes.entity.boss.BossHealthSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttributeInstance.class)
public abstract class BossHealthAttributeMixin implements BossHealthAttribute {
    @Unique private boolean unknownEchoes$bossHealth;

    @Shadow public abstract Holder<Attribute> getAttribute();
    @Shadow protected abstract void setDirty();

    @Override
    public void unknownEchoes$enableBossHealth() {
        if (!this.unknownEchoes$bossHealth && this.getAttribute().equals(Attributes.MAX_HEALTH)) {
            this.unknownEchoes$bossHealth = true;
            this.setDirty();
        }
    }

    @WrapOperation(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"), require = 1, allow = 1)
    private double unknownEchoes$preserveBossHealth(Attribute attribute, double value, Operation<Double> original) {
        double sanitized = original.call(attribute, value);
        return this.unknownEchoes$bossHealth
                ? BossHealthSupport.expandUpperBound(attribute, value, sanitized) : sanitized;
    }
}
