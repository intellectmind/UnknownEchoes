package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticleEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, UnknownEchoes.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> VENOM = EFFECTS.register("particle_venom", () -> new Marker(MobEffectCategory.HARMFUL, 0x8fe865));
    public static final DeferredHolder<MobEffect, MobEffect> FIRE = EFFECTS.register("particle_fire", () -> new Marker(MobEffectCategory.HARMFUL, 0xff8547));
    public static final DeferredHolder<MobEffect, MobEffect> FROST = EFFECTS.register("particle_frost", () -> new Marker(MobEffectCategory.HARMFUL, 0x9deaff));
    public static final DeferredHolder<MobEffect, MobEffect> SHIELD = EFFECTS.register("particle_shield", () -> new Marker(MobEffectCategory.BENEFICIAL, 0xffefb1));

    private static final class Marker extends MobEffect {
        private Marker(MobEffectCategory category, int color) {
            super(category, color);
            if (category == MobEffectCategory.BENEFICIAL) addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION, UnknownEchoes.id("particle_shield"),
                    8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        }
    }
    private ModParticleEffects() { }
}
