package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.ModifyRegistriesEvent;

/** 旧生物桶名称按普通水桶读取，不恢复捕捉或释放生物的能力。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ModRegistryAliases {
    private ModRegistryAliases() {}

    @SubscribeEvent
    public static void onModifyRegistries(ModifyRegistriesEvent event) {
        var items = event.getRegistry(Registries.ITEM);
        for (String name : new String[]{"mirror_tail_fish_bucket", "echo_cuttlefish_bucket",
                "pearlback_ray_bucket", "lantern_seahorse_bucket"}) {
            items.addAlias(UnknownEchoes.id(name), ResourceLocation.withDefaultNamespace("water_bucket"));
        }
    }
}
