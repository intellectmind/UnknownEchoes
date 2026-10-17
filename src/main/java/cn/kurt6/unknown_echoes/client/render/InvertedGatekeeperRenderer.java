package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.InvertedGatekeeper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import cn.kurt6.unknown_echoes.client.model.LimitedHeadGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InvertedGatekeeperRenderer extends GeoEntityRenderer<InvertedGatekeeper> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = -0.8513D;
    public InvertedGatekeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new LimitedHeadGeoModel<>(UnknownEchoes.id("inverted_gatekeeper"), "skull_look", 40.0F, -15.0F, 15.0F));
        this.shadowRadius = 1.2F;
        this.withScale(1.33F);
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/inverted_gatekeeper_glowing.png")));
    }

    @Override
    public void preRender(PoseStack poseStack, InvertedGatekeeper animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
