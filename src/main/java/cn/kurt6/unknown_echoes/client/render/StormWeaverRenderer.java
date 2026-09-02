package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.StormWeaver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 风暴编织者 GeckoLib 渲染器(风之强化守护者,天空观测站)。 */
public class StormWeaverRenderer extends GeoEntityRenderer<StormWeaver> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = -1.4213D;

    public StormWeaverRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("storm_weaver"), true));
        this.shadowRadius = 1.2F;
        this.withScale(2.47F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/storm_weaver_glowing.png")));
    }

    @Override
    public void preRender(PoseStack poseStack, StormWeaver animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
