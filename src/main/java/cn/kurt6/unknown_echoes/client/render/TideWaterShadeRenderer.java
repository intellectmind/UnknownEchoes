package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.model.LimitedHeadGeoModel;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class TideWaterShadeRenderer extends GeoEntityRenderer<TideWaterShade> {

    public TideWaterShadeRenderer(EntityRendererProvider.Context context) {
        super(context, new LimitedHeadGeoModel<>(UnknownEchoes.id("tide_water_shade"),
                "head", 24.0F, -9.0F, 11.0F));
        this.shadowRadius = 0.3F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/tide_water_shade_glowing.png")));
    }

    @Override
    public Color getRenderColor(TideWaterShade animatable, float partialTick, int packedLight) {
        return Color.ofRGBA(1.0F, 1.0F, 1.0F, animatable.fadeVisibility(partialTick));
    }

    @Override
    public RenderType getRenderType(TideWaterShade animatable, ResourceLocation texture,
                                    net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {

        return RenderType.entityTranslucent(texture);
    }
}
