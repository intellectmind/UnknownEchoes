package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.AshScribe;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AshScribeRenderer extends GeoEntityRenderer<AshScribe> {
    public AshScribeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("ash_scribe"), true));
        this.shadowRadius = 1.2F;
        this.withScale(1.00F);
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/ash_scribe_glowing.png")));
    }
}
