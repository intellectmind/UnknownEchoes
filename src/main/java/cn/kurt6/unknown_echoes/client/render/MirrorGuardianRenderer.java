package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 镜像守护者 GeckoLib 渲染器(本体与镜面假身共用——真假辨识靠声音与行为)。
 * 模型 v2:宽肩甲 + 悬浮镜片 + 右臂镜刃 + 左臂镜盾 + 裙甲 + 头冠晶刺(128x128 贴图)。
 * 发光层:mirror_guardian_glowing.png(眼缝 + 胸核 + 刃缘 + 冠尖 + 镜片棱缘自发光)
 */
public class MirrorGuardianRenderer extends GeoEntityRenderer<MirrorGuardian> {

    public MirrorGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_guardian"), true));
        this.shadowRadius = 0.8F;
        // 模型本体约 3 格高(冠刺到 48px),x1.1 后视觉约 3.3 格,与 3.2 碰撞箱匹配
        this.withScale(1.1F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/mirror_guardian_glowing.png")));
    }
}
