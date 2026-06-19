package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.SilentWalker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 沉默行者 GeckoLib 渲染器。
 * 模型:assets/unknown_echoes/geo/entity/silent_walker.geo.json
 * 动画:assets/unknown_echoes/animations/entity/silent_walker.animation.json
 * 贴图:assets/unknown_echoes/textures/entity/silent_walker.png
 * 刻意没有发光层:沉默行者不发光,在昏暗沼泽里靠轮廓辨认。
 */
public class SilentWalkerRenderer extends GeoEntityRenderer<SilentWalker> {

    public SilentWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("silent_walker"), true));
        this.shadowRadius = 0.5F;
    }
}
