package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * 深渊观测者 GeckoLib 渲染器(本体与黑潮拟影共用模型——拟影整体压暗,不伪装本体)。
 * 模型 v2:装甲背甲+背棘 + 眼窝环+追踪巨瞳+副眼 + 双段触须 x4 + 三鳍 + 3 颗倒影泪滴(128x128 贴图)。
 * 发光层:abyss_watcher_glowing.png(瞳/副眼/泪滴/触须尖/背甲脊线自发光)
 */
public class AbyssWatcherRenderer extends GeoEntityRenderer<AbyssWatcher> {

    /** 拟影色调:暗沉的墨蓝,叠加服务端墨丝粒子,一眼区分本体。 */
    private static final Color CLONE_TINT = Color.ofRGB(95, 115, 140);

    public AbyssWatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("abyss_watcher"), true));
        this.shadowRadius = 1.3F;
        // Boss 级体型:模型本体约 1.7 格,x2.0 后约 3.4 格(碰撞箱 2.6x2.4)
        this.withScale(2.0F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/abyss_watcher_glowing.png")));
    }

    @Override
    public Color getRenderColor(AbyssWatcher animatable, float partialTick, int packedLight) {
        return animatable.isClone() ? CLONE_TINT
                : super.getRenderColor(animatable, partialTick, packedLight);
    }
}
