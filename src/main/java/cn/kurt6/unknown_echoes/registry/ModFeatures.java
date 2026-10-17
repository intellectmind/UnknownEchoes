package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.world.feature.RealmLandformFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, UnknownEchoes.MODID);
    public static final DeferredHolder<Feature<?>, RealmLandformFeature> REALM_LANDFORM =
            FEATURES.register("realm_landform", RealmLandformFeature::new);

    private ModFeatures() {
    }
}
