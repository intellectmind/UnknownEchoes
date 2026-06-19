package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.StormWeaver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 风暴编织者 GeckoLib 渲染器(风之强化守护者,天空观测站)。
 * 模型:风鸟形体 + 风暴核心 + 三圈环绕风环(128x128 贴图)。
 * 发光层:storm_weaver_glowing.png(核心/眼/风环自发光)。
 */
public class StormWeaverRenderer extends GeoEntityRenderer<StormWeaver> {

    public StormWeaverRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("storm_weaver"), true));
        this.shadowRadius = 0.9F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/storm_weaver_glowing.png")));
    }
}
