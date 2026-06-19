package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.EchoWanderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 回响游荡者 GeckoLib 渲染器。
 * 模型:assets/unknown_echoes/geo/entity/echo_wanderer.geo.json
 * 动画:assets/unknown_echoes/animations/entity/echo_wanderer.animation.json
 * 贴图:assets/unknown_echoes/textures/entity/echo_wanderer.png
 * 发光层:echo_wanderer_glowing.png(眼睛/胸口符文/手臂光斑自发光)
 */
public class EchoWandererRenderer extends GeoEntityRenderer<EchoWanderer> {

    public EchoWandererRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("echo_wanderer"), true));
        this.shadowRadius = 0.5F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/echo_wanderer_glowing.png")));
    }
}
