package cn.kurt6.unknown_echoes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import javax.annotation.Nullable;

/**
 * 自发光渲染层:用原版 RenderType.eyes(蜘蛛眼同款)整体重绘一遍发光贴图。
 * 发光贴图为透明底,只绘制需要发光的像素(由纹理脚本与底图同步输出)。
 *
 * 不用 GeckoLib 的 AutoGlowingGeoLayer:它会动态改写并重新上传基础贴图,
 * 在 Sodium/光影环境下有把整个实体渲染成黑色的已知风险(本项目实际发生过)。
 */
public class EchoGlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final ResourceLocation glowTexture;

    public EchoGlowLayer(GeoRenderer<T> renderer, ResourceLocation glowTexture) {
        super(renderer);
        this.glowTexture = glowTexture;
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (animatable instanceof Entity entity && entity.isInvisible()) {
            return;
        }
        RenderType glow = RenderType.eyes(this.glowTexture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_SKY, packedOverlay,
                getRenderer().getRenderColor(animatable, partialTick, packedLight).argbInt());
    }
}
