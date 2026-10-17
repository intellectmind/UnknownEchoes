package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 深层生态实体共享渲染器:每个实体仍使用独立模型、贴图与发光遮罩。 */
public class DeepEcologyRenderer<T extends Mob & GeoAnimatable> extends BodyAlignedEntityRenderer<T> {
    /**
     * 把模型底面抬到碰撞箱底面。碰撞箱以脚底为原点,模型自己的最低面不在 y=0 时会陷进地里或悬空;
     * 本渲染器被多个实体共用,抬升量必须逐实体传入,不能做成共享常量。
     */
    private final double modelYOffset;

    public DeepEcologyRenderer(EntityRendererProvider.Context context, String modelName, float shadowRadius) {
        this(context, modelName, shadowRadius, 0.0D);
    }

    public DeepEcologyRenderer(EntityRendererProvider.Context context, String modelName,
                               float shadowRadius, double modelYOffset) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id(modelName), false));
        this.shadowRadius = shadowRadius;
        this.modelYOffset = modelYOffset;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/" + modelName + "_glowing.png")));
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender && this.modelYOffset != 0.0D) {
            poseStack.translate(0.0D, this.modelYOffset, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
