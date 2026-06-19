package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.GlowRabbit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 荧光兔 GeckoLib 渲染器。
 * 发光层:glow_rabbit_glowing.png(皮毛荧斑微光)。
 */
public class GlowRabbitRenderer extends GeoEntityRenderer<GlowRabbit> {

    public GlowRabbitRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("glow_rabbit"), true));
        this.shadowRadius = 0.25F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/glow_rabbit_glowing.png")));
    }
}
