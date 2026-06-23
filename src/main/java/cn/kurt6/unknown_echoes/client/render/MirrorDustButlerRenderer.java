package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.MirrorDustButler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 镜尘执事 GeckoLib 渲染器(镜湖神殿隐藏房区域守护者)。
 * 模型:镜面礼服执事轮廓 + 裂镜面具 + 托盘礼杖(64x64 贴图)。
 * 发光层:mirror_dust_butler_glowing.png(裂镜面具缝隙微光)。
 * 注意:真假三像共用同一渲染(同型同贴图)——真身识别只能靠服务端驱动的
 * 粒子/音效线索,渲染层不做任何区分(红线 #9)。
 */
public class MirrorDustButlerRenderer extends GeoEntityRenderer<MirrorDustButler> {

    public MirrorDustButlerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_dust_butler"), true));
        this.shadowRadius = 0.6F;
        this.withScale(1.15F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/mirror_dust_butler_glowing.png")));
    }
}
