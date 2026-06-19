package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.LostTraveler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 迷途旅者 GeckoLib 渲染器。
 * 模型:assets/unknown_echoes/geo/entity/lost_traveler.geo.json(手写 box 几何)
 * 动画:assets/unknown_echoes/animations/entity/lost_traveler.animation.json
 * 贴图:assets/unknown_echoes/textures/entity/lost_traveler.png(tools/gen_v06d_textures.py)
 */
public class LostTravelerRenderer extends GeoEntityRenderer<LostTraveler> {

    public LostTravelerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("lost_traveler"), true));
        this.shadowRadius = 0.45F;
    }
}
