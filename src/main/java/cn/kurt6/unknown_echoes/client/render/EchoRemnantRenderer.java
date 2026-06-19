package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.EchoRemnant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EchoRemnantRenderer extends GeoEntityRenderer<EchoRemnant> {
    public EchoRemnantRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("echo_remnant"), true));
        this.shadowRadius = 0.42F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/echo_remnant_glowing.png")));
    }
}
