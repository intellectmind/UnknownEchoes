package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 虚空先知渲染器:核心、冠晶与裂隙纹使用独立发光层。 */
public class VoidProphetRenderer extends GeoEntityRenderer<VoidProphet> {
    public VoidProphetRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("void_prophet"), true));
        this.shadowRadius = 2.2F;
        this.withScale(1.22F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/void_prophet_glowing.png")));
    }
}
