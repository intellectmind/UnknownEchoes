package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 裂隙看守者渲染器：眼部、裂隙与护甲纹路使用独立发光层。 */
public class RiftWardenRenderer extends GeoEntityRenderer<RiftWarden> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = 0.0953D;

    /** 六招层色必须落在 190-235 蓝域(切割最亮、风暴最深),混入粉紫会读成没有固定身份色。 */
    private static final int[] RIFT_COLORS = {
            0,           // NONE
            0x6BE0F2,    // 裂隙切割：最亮的青蓝，对应最快的一招
            0x4FA8E8,    // 锚点砸击：中蓝
            0x3D7FD6,    // 虚空回拉：偏深，向内收束
            0x5FC4EA,    // 护盾破裂：亮蓝爆散
            0x74E8E0,    // 裂隙冲锋：带青的高速拖尾
            0x2F5FC0,    // 裂隙风暴：最深的钴蓝，压场
    };

    public RiftWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("rift_warden"), true));
        this.shadowRadius = 2.0F;
        this.withScale(1.17F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/rift_warden_glowing.png")));
        addRenderLayer(new BossAttackEnergyLayer<>(this,
                UnknownEchoes.id("textures/entity/rift_warden_glowing.png"),
                RIFT_COLORS,
                new int[][]{{}, {14, 18}, {21, 27}, {31}, {28, 35}, {17, 21}, {12, 24, 36, 40, 46}},
                new float[]{1.0F, 1.8F, 0.85F, 0.65F, 1.15F, 2.25F, 1.45F}));
    }

    @Override
    public void preRender(PoseStack poseStack, RiftWarden animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
