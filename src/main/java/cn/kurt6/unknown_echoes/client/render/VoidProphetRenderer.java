package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VoidProphetRenderer extends GeoEntityRenderer<VoidProphet> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = 0.0836D;
    public VoidProphetRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("void_prophet"), true));
        this.shadowRadius = 2.0F;
        this.withScale(1.04F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/void_prophet_glowing.png")));
        // 模型单位数最大,所以 scale 最小,渲染后仍约 7 格,不要顺手调大。
        // releaseTicks 每项 = duration − 服务端命中 tick,与 AttackMode 逐项对齐。
        addRenderLayer(new BossAttackEnergyLayer<>(this,
                UnknownEchoes.id("textures/entity/void_prophet_glowing.png"),
                // 必须留在 258-275 紫域:先知与裂隙看守者靠"紫 vs 蓝"区分,借色就撞车。
                new int[]{0, 0x7651C9, 0xA15BE8, 0x55327D, 0x8B67BC, 0x9A6ED8, 0xC07AFF},
                new int[][]{{}, {8}, {27}, {18, 33}, {32}, {27}, {38, 42}},
                new float[]{1.0F, 1.8F, 1.1F, 0.55F, 0.75F, 1.45F, 0.48F}));
    }

    @Override
    public void preRender(PoseStack poseStack, VoidProphet animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
