package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.MirrorTailFish;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 镜尾鱼 GeckoLib 渲染器(鱼类不做头部跟随)。
 * 发光层:mirror_tail_fish_glowing.png(镜面尾鳍反光)。
 */
public class MirrorTailFishRenderer extends GeoEntityRenderer<MirrorTailFish> {

    public MirrorTailFishRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_tail_fish"), false));
        this.shadowRadius = 0.2F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/mirror_tail_fish_glowing.png")));
    }
}
