package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.CrystalFeatherBird;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrystalFeatherBirdRenderer extends GeoEntityRenderer<CrystalFeatherBird> {

    public CrystalFeatherBirdRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("crystal_feather_bird"), true));
        this.shadowRadius = 0.3F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/crystal_feather_bird_glowing.png")));
    }
}
