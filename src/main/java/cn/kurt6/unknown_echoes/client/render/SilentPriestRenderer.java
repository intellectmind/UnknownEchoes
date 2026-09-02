package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.SilentPriest;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 沉默祭司 GeckoLib 渲染器(失语沼泽区域守护者,沉默祭坛)。 */
public class SilentPriestRenderer extends GeoEntityRenderer<SilentPriest> {

    public SilentPriestRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("silent_priest"), true));

        this.shadowRadius = 1.2F;
        this.withScale(1.48F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/silent_priest_glowing.png")));
    }
}
