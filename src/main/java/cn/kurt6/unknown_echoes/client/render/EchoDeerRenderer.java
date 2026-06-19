package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.EchoDeer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 回声鹿 GeckoLib 渲染器。
 * 模型:assets/unknown_echoes/geo/entity/echo_deer.geo.json
 * 贴图:assets/unknown_echoes/textures/entity/echo_deer.png
 * 发光层:echo_deer_glowing.png(鹿角回响纹与背部斑点微光)
 */
public class EchoDeerRenderer extends GeoEntityRenderer<EchoDeer> {

    public EchoDeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("echo_deer"), true));
        this.shadowRadius = 0.6F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/echo_deer_glowing.png")));
    }
}
