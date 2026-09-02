package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/** 深渊观测者 GeckoLib 渲染器(本体与黑潮拟影共用模型——拟影整体压暗,不伪装本体)。 */
public class AbyssWatcherRenderer extends GeoEntityRenderer<AbyssWatcher> {
    /** 把模型底面抬到碰撞箱底面：碰撞箱以脚底为原点，模型自己的最低面不在 y=0 时会陷进地里或悬空。 */
    private static final double MODEL_Y_OFFSET = 0.8038D;

    /** 拟影色调:暗沉的墨蓝,叠加服务端墨丝粒子,一眼区分本体。 */
    private static final Color CLONE_TINT = Color.ofRGB(95, 115, 140);

    /** 拟影发光层不继承主体暗色,保留 glow 贴图原色与满天光,避免分身看起来没有发光层。 */
    private static final Color CLONE_GLOW_TINT = Color.WHITE;

    public AbyssWatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("abyss_watcher"), true));
        this.shadowRadius = 1.1F;

        // 翼/尾/环与攻击特效不参与命中范围，碰撞箱只按主体宽度登记。
        // 改这个值必须同步 tools/upgrade_abyss_watcher_fx.py 的 RENDER_SCALE:
        // 特效环的覆盖倍数是"服务端格数 / 渲染缩放"推导出来的,缩放变了预警圈就不再等于判定范围。
        this.withScale(1.60F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID, "textures/entity/abyss_watcher_glowing.png"),
                watcher -> watcher.isClone() ? CLONE_GLOW_TINT : Color.WHITE));
        addRenderLayer(new BossAttackEnergyLayer<>(this,
                UnknownEchoes.id("textures/entity/abyss_watcher_glowing.png"),
                new int[]{0, 0x56D7D0, 0x7CE9FF, 0x287F9E, 0xB7F7FF},
                new int[][]{{}, {7}, {13}, {14}, {15}},
                new float[]{1.0F, 0.9F, 2.1F, 0.55F, 1.35F}));
    }

    @Override
    public Color getRenderColor(AbyssWatcher animatable, float partialTick, int packedLight) {
        return animatable.isClone() ? CLONE_TINT
                : super.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, AbyssWatcher animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
