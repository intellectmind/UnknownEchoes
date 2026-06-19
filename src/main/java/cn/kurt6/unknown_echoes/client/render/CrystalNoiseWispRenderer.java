package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.CrystalNoiseWisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 晶噪灵 GeckoLib 渲染器。
 */
public class CrystalNoiseWispRenderer extends GeoEntityRenderer<CrystalNoiseWisp> {

    public CrystalNoiseWispRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("crystal_noise_wisp"), true));
        this.shadowRadius = 0.35F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/crystal_noise_wisp_glowing.png")));
    }
}
