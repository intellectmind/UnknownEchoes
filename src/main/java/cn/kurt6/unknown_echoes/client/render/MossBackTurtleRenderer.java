package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.MossBackTurtle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 苔背龟 GeckoLib 渲染器(无发光层,沼泽生物保持低调)。
 */
public class MossBackTurtleRenderer extends GeoEntityRenderer<MossBackTurtle> {

    public MossBackTurtleRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("moss_back_turtle"), true));
        this.shadowRadius = 0.5F;
    }
}
