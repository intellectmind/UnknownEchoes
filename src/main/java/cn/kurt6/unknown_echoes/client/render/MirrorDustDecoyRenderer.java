package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.MirrorDustDecoy;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 镜尘假像渲染器:半透明本体 + 独立发光层。
 */
public class MirrorDustDecoyRenderer extends GeoEntityRenderer<MirrorDustDecoy> {
    public MirrorDustDecoyRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_dust_decoy"), true));
        this.shadowRadius = 0.25F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/mirror_dust_decoy_glowing.png")));
    }

    @Override
    public RenderType getRenderType(MirrorDustDecoy animatable, ResourceLocation texture,
                                    net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
