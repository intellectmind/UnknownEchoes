package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 水影 GeckoLib 渲染器(潮汐执灯者召唤物)。
 * 模型:细瘦人形剪影(32x32 贴图);半透明深蓝——贴图带透明像素,走半透渲染层。
 */
public class TideWaterShadeRenderer extends GeoEntityRenderer<TideWaterShade> {

    public TideWaterShadeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("tide_water_shade"), true));
        this.shadowRadius = 0.3F;
    }

    @Override
    public RenderType getRenderType(TideWaterShade animatable, ResourceLocation texture,
                                    net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {
        // 半透明深蓝剪影(10.4.2 贴图需求):贴图 alpha 直接生效
        return RenderType.entityTranslucent(texture);
    }
}
