package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.entity.boss.BossVisualState;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import javax.annotation.Nullable;

/** 主线 Boss 施法能量层：使用发光贴图作遮罩，并在真实释放帧叠加一次短促亮闪。 */
public class BossAttackEnergyLayer<T extends LivingEntity & GeoAnimatable & BossVisualState>
        extends GeoRenderLayer<T> {
    private final ResourceLocation energyTexture;
    private final int[] themeColors;
    private final int[][] releaseTicks;
    private final float[] flowSpeeds;

    public BossAttackEnergyLayer(GeoRenderer<T> renderer, ResourceLocation energyTexture,
                                 int[] themeColors, int[][] releaseTicks, float[] flowSpeeds) {
        super(renderer);
        this.energyTexture = energyTexture;
        this.themeColors = themeColors;
        this.releaseTicks = releaseTicks;
        this.flowSpeeds = flowSpeeds;
    }

    @Override
    public void render(PoseStack poseStack, T boss, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        int mode = boss.visualAttackMode();
        int duration = boss.visualAttackDuration();
        if (mode <= 0 || duration <= 0 || boss.isInvisible()
                || !ClientConfig.ENABLE_BOSS_PARTICLES.get()) {
            return;
        }

        float density = ClientConfig.BOSS_PARTICLE_DENSITY.get().floatValue();
        if (density <= 0.001F) {
            return;
        }

        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float distance = (float) Math.sqrt(boss.position().distanceToSqr(cameraPosition));
        float distanceFade = 1.0F - Mth.clamp((distance - 24.0F) / 40.0F, 0.0F, 1.0F);
        density *= distanceFade;
        if (density <= 0.01F) {
            return;
        }

        float age = Math.min(duration, boss.visualAttackAge() + partialTick);
        float progress = age / duration;
        float pulse = releasePulse(mode, age);
        float envelope = attackEnvelope(progress);
        float speed = valueAt(this.flowSpeeds, mode, 1.0F);
        float shimmer = 0.5F + 0.5F * (float) Math.sin(age * (0.48F + speed * 0.12F));
        float uOffset = (float) Math.sin(age * 0.042F * speed) * 0.014F;
        float vOffset = (float) Math.cos(age * 0.031F * speed) * 0.02F;
        int themeColor = valueAt(this.themeColors, mode, 0xFFFFFF);
        // 这组数值不能写进 alpha：energy_swirl 的加法混合不会用 alpha 调 RGB。
        // 现在让攻击包络真正控制 RGB 强度，常态保持接近满亮，远距离才按 density 淡出。
        float strength = Math.min(1.0F,
                (0.88F + shimmer * 0.12F) * envelope + pulse * 0.45F) * density;
        if (strength <= 0.001F) {
            return;
        }

        renderEnergyPass(poseStack, boss, bakedModel, bufferSource, partialTick,
                packedOverlay, uOffset, vOffset, BossEnergyColor.withIntensity(themeColor, strength));
        if (pulse > 0.08F) {
            int accent = blendTowardWhite(themeColor, 0.76F);
            renderEnergyPass(poseStack, boss, bakedModel, bufferSource, partialTick,
                    packedOverlay, -vOffset * 0.75F, uOffset,
                    BossEnergyColor.withIntensity(accent, pulse * density));
        }
    }

    private void renderEnergyPass(PoseStack poseStack, T boss, BakedGeoModel bakedModel,
                                  MultiBufferSource bufferSource, float partialTick,
                                  int packedOverlay, float uOffset, float vOffset, int color) {
        RenderType energy = RenderType.energySwirl(this.energyTexture, uOffset, vOffset);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, boss, energy,
                bufferSource.getBuffer(energy), partialTick, LightTexture.FULL_SKY,
                packedOverlay, color);
    }

    private float releasePulse(int mode, float age) {
        if (mode >= this.releaseTicks.length || this.releaseTicks[mode] == null) {
            return 0.0F;
        }
        float pulse = 0.0F;
        for (int releaseTick : this.releaseTicks[mode]) {
            pulse = Math.max(pulse, Math.max(0.0F, 1.0F - Math.abs(age - releaseTick) / 3.5F));
        }
        return pulse;
    }

    private static float attackEnvelope(float progress) {
        float fadeIn = smoothStep(Mth.clamp(progress / 0.12F, 0.0F, 1.0F));
        float fadeOut = smoothStep(Mth.clamp((1.0F - progress) / 0.16F, 0.0F, 1.0F));
        return fadeIn * fadeOut;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static int valueAt(int[] values, int index, int fallback) {
        return index >= 0 && index < values.length ? values[index] : fallback;
    }

    private static float valueAt(float[] values, int index, float fallback) {
        return index >= 0 && index < values.length ? values[index] : fallback;
    }

    private static int blendTowardWhite(int rgb, float amount) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        red += Math.round((255 - red) * amount);
        green += Math.round((255 - green) * amount);
        blue += Math.round((255 - blue) * amount);
        return (red << 16) | (green << 8) | blue;
    }

}
