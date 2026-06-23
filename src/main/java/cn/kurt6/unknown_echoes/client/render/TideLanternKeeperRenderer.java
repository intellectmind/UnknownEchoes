package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 潮汐执灯者 GeckoLib 渲染器(镜湖湖底灯塔废墟区域守护者)。
 * 模型:湖灯执事轮廓——罩袍水栖身形 + 执灯臂 + 尾鳍(64x64 贴图)。
 * 发光层:tide_lantern_keeper_glowing.png(青蓝潮灯灯罩 + 双眼自发光)。
 */
public class TideLanternKeeperRenderer extends GeoEntityRenderer<TideLanternKeeper> {

    public TideLanternKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("tide_lantern_keeper"), true));
        this.shadowRadius = 0.8F;
        this.withScale(1.15F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/tide_lantern_keeper_glowing.png")));
    }
}
