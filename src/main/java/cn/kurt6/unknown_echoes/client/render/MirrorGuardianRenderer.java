package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 镜像守护者 GeckoLib 渲染器(本体与镜面假身共用——真假辨识靠声音与行为)。 */
public class MirrorGuardianRenderer extends GeoEntityRenderer<MirrorGuardian> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = 0.4358D;

    public MirrorGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("mirror_guardian"), true));
        this.shadowRadius = 2.0F;

        this.withScale(2.49F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/mirror_guardian_glowing.png")));
        addRenderLayer(new BossAttackEnergyLayer<>(this,
                UnknownEchoes.id("textures/entity/mirror_guardian_glowing.png"),
                // 近白冷镜光,与 BossParticleTheme.MIRROR 同族,只靠明度区分;混进暖色会读成别的怪。
                new int[]{0, 0xE9FCFF, 0xC8E8FF, 0xF2FAFF, 0xA8E8FF},
                new int[][]{{}, {8}, {15}, {8, 14, 20}, {11}},
                new float[]{1.0F, 2.15F, 0.72F, 1.75F, 1.3F}));
    }

    @Override
    public void preRender(PoseStack poseStack, MirrorGuardian animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
