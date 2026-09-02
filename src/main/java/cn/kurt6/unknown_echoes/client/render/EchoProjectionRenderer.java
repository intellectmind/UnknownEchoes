package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class EchoProjectionRenderer extends GeoEntityRenderer<EchoProjectionEntity> {

    private static final Color HOLOGRAM_TINT = Color.ofRGBA(250, 253, 255, 218);

    public EchoProjectionRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("echo_projection"), true));
        this.shadowRadius = 0.0F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/echo_projection_glowing.png")));
    }

    @Override
    public RenderType getRenderType(EchoProjectionEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public Color getRenderColor(EchoProjectionEntity animatable, float partialTick, int packedLight) {
        return HOLOGRAM_TINT;
    }
}
