package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 潮汐执灯者 GeckoLib 渲染器(镜湖湖底灯塔废墟区域守护者)。 */
public class TideLanternKeeperRenderer extends GeoEntityRenderer<TideLanternKeeper> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = 0.2354D;

    public TideLanternKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("tide_lantern_keeper"), true));
        // 水下战斗空间更紧凑，视觉体型与碰撞箱同步收窄；技能范围保持原设计。
        this.shadowRadius = 1.0F;
        this.withScale(1.55F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/tide_lantern_keeper_glowing.png")));
    }

    @Override
    public void preRender(PoseStack poseStack, TideLanternKeeper animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
