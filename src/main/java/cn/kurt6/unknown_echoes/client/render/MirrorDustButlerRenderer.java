package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.MirrorDustButler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 镜尘执事 GeckoLib 渲染器(镜湖神殿隐藏房区域守护者)。 */
public class MirrorDustButlerRenderer extends GeoEntityRenderer<MirrorDustButler> {

    public MirrorDustButlerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_dust_butler"), true));
        this.shadowRadius = 1.2F;
        // 几何主体经 1.72 倍缩放后约 2.10 × 4.10 格，碰撞箱按同一包围尺寸登记。
        this.withScale(1.72F);

        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/mirror_dust_butler_glowing.png")));
    }
}
