package cn.kurt6.unknown_echoes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import cn.kurt6.unknown_echoes.entity.mob.AerialHunter;
import cn.kurt6.unknown_echoes.entity.mob.HumanoidRaider;
import cn.kurt6.unknown_echoes.fx.MobAttackFx;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import javax.annotation.Nullable;
import java.util.function.Function;

/** 自发光渲染层:用原版 RenderType.eyes(蜘蛛眼同款)整体重绘一遍发光贴图。 */
public class EchoGlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final ResourceLocation glowTexture;
    @Nullable
    private final Function<T, Color> glowTint;

    public EchoGlowLayer(GeoRenderer<T> renderer, ResourceLocation glowTexture) {
        this(renderer, glowTexture, null);
    }

    /** glowTint 为空时沿用渲染器的整体着色;本体被整体压暗的实体(拟影等)必须单独给发光层提亮,否则辉光随本体一起压到不可见。 */
    public EchoGlowLayer(GeoRenderer<T> renderer, ResourceLocation glowTexture,
                         @Nullable Function<T, Color> glowTint) {
        super(renderer);
        this.glowTexture = glowTexture;
        this.glowTint = glowTint;
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
        Color tint = this.glowTint != null
                ? this.glowTint.apply(animatable)
                : getRenderer().getRenderColor(animatable, partialTick, packedLight);
        int phase = animatable instanceof AerialHunter hunter ? hunter.attackPresentation()
                : animatable instanceof HumanoidRaider raider ? raider.attackPresentation() : 0;
        if (phase != 0 && animatable instanceof Entity entity && entity.isAlive()) {
            float gain = MobAttackFx.glow(phase, entity.tickCount + partialTick);
            tint = Color.ofRGBA(tint.getRedFloat() * gain, tint.getGreenFloat() * gain,
                    tint.getBlueFloat() * gain, tint.getAlphaFloat());
        }
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_SKY, packedOverlay,
                tint.argbInt());
    }
}
