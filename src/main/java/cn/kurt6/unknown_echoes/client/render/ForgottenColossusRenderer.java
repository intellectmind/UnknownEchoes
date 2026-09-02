package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ForgottenColossusRenderer extends GeoEntityRenderer<ForgottenColossus> {
    private static final float MODEL_SCALE = 1.34F;
    /** 抬高量:脚底最低面在模型 Y = −2px,缩放后下沉约 0.167 格,抬回并留 0.02 格避免 z-fighting。 */
    private static final double MODEL_Y_OFFSET = 2.0D * MODEL_SCALE / 16.0D + 0.02D;

    public ForgottenColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("forgotten_colossus"), true));
        this.shadowRadius = 2.0F;

        this.withScale(MODEL_SCALE);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/forgotten_colossus_glowing.png")));
        // 释放帧必须与服务端常数逐项一致(索引 = syncVisualAttackState 的 mode),差一 tick
        // 亮闪就与判定错开。层色取 BossParticleTheme.COLOSSUS 同族。
        addRenderLayer(new BossAttackEnergyLayer<>(this,
                UnknownEchoes.id("textures/entity/forgotten_colossus_glowing.png"),
                new int[]{0, 0xC58A49, 0xF2C86B, 0x36B8AA, 0xD7A84C},
                new int[][]{{}, {11}, {15}, {20}, {24}},
                new float[]{1.0F, 0.55F, 0.7F, 1.45F, 0.45F}));
    }

    @Override
    public void preRender(PoseStack poseStack, ForgottenColossus animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
