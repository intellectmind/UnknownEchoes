package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.CrystalChimeSpirit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 晶歌浮灵 GeckoLib 渲染器。
 */
public class CrystalChimeSpiritRenderer extends GeoEntityRenderer<CrystalChimeSpirit> {

    public CrystalChimeSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("crystal_chime_spirit"), true));
        this.shadowRadius = 0.35F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/crystal_chime_spirit_glowing.png")));
    }
}
