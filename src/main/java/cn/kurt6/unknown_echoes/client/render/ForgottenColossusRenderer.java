package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 遗忘巨像 GeckoLib 渲染器。
 * 模型 v2:双段手臂 + 巨石肩甲 + 发光胸核 + 头冠 + 背部符文石板 + 3 块悬浮符石(128x128 贴图)。
 * 发光层:forgotten_colossus_glowing.png(双眼/胸核/金青裂纹/符石符纹自发光)
 */
public class ForgottenColossusRenderer extends GeoEntityRenderer<ForgottenColossus> {

    public ForgottenColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("forgotten_colossus"), true));
        this.shadowRadius = 2.0F;
        // 模型本体约 4.7 格(冠顶 75px),x1.15 后约 5.4 格,与碰撞箱匹配
        this.withScale(1.15F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/forgotten_colossus_glowing.png")));
    }
}
