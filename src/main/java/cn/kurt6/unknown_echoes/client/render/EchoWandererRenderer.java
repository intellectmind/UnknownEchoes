package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.EchoWanderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class EchoWandererRenderer extends GeoEntityRenderer<EchoWanderer> {

    public EchoWandererRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("echo_wanderer"), true));
        this.shadowRadius = 0.5F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/echo_wanderer_glowing.png")));
    }
}
