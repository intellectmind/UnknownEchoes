package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.WindErodedSentinel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WindErodedSentinelRenderer extends GeoEntityRenderer<WindErodedSentinel> {
    public WindErodedSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("wind_eroded_sentinel"), true));
        this.shadowRadius = 0.45F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/wind_eroded_sentinel_glowing.png")));
    }
}
