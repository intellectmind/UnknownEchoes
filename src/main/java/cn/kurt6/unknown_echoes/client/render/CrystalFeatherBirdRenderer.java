package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.CrystalFeatherBird;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 晶羽鸟 GeckoLib 渲染器。
 * 模型:assets/unknown_echoes/geo/entity/crystal_feather_bird.geo.json
 * 贴图:assets/unknown_echoes/textures/entity/crystal_feather_bird.png
 * 发光层:crystal_feather_bird_glowing.png(尾羽晶簇微光)
 */
public class CrystalFeatherBirdRenderer extends GeoEntityRenderer<CrystalFeatherBird> {

    public CrystalFeatherBirdRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("crystal_feather_bird"), true));
        this.shadowRadius = 0.3F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/crystal_feather_bird_glowing.png")));
    }
}
