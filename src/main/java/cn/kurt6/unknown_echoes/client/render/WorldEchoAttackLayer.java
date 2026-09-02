package cn.kurt6.unknown_echoes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import javax.annotation.Nullable;

/** 招式蓄力能量层：按服务端同步的攻击阶段染色，并在实际释放帧增强脉冲。 */
public class WorldEchoAttackLayer extends GeoRenderLayer<WorldEcho> {
    private final ResourceLocation energyTexture;

    public WorldEchoAttackLayer(GeoRenderer<WorldEcho> renderer, ResourceLocation energyTexture) {
        super(renderer);
        this.energyTexture = energyTexture;
    }

    @Override
    public void render(PoseStack poseStack, WorldEcho worldEcho, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        WorldEcho.AttackMode mode = worldEcho.getSyncedAttackMode();
        int duration = worldEcho.getSyncedAttackDuration();
        if (mode == WorldEcho.AttackMode.NONE || duration <= 0 || worldEcho.isInvisible()) {
            return;
        }

        float age = Math.min(duration, worldEcho.getSyncedAttackAge() + partialTick);
        float progress = age / duration;
        float pulse = releasePulse(mode, age);
        float shimmer = 0.5F + 0.5F * (float) Math.sin(age * 0.62F);
        float speed = flowSpeed(mode);
        float uOffset = (float) Math.sin(age * 0.045F * speed) * 0.012F;
        float vOffset = (float) Math.cos(age * 0.032F * speed) * 0.018F;
        int themeColor = themeColor(mode);
        // energy_swirl 的 alpha 只做 discard；蓄力亮度直接映射到 RGB，避免整层发灰。
        float strength = Math.min(1.0F,
                0.90F + progress * 0.05F + shimmer * 0.05F + pulse * 0.28F);

        renderEnergyPass(poseStack, worldEcho, bakedModel, bufferSource, partialTick,
                packedOverlay, uOffset, vOffset, BossEnergyColor.withIntensity(themeColor, strength));
        if (pulse > 0.08F) {
            int accent = blendTowardWhite(themeColor, 0.72F);
            renderEnergyPass(poseStack, worldEcho, bakedModel, bufferSource, partialTick,
                    packedOverlay, -vOffset * 0.8F, uOffset * 1.1F,
                    BossEnergyColor.withIntensity(accent, pulse));
        }
    }

    private void renderEnergyPass(PoseStack poseStack, WorldEcho worldEcho,
                                  BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                                  float partialTick, int packedOverlay,
                                  float uOffset, float vOffset, int color) {
        RenderType energy = RenderType.energySwirl(this.energyTexture, uOffset, vOffset);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, worldEcho, energy,
                bufferSource.getBuffer(energy), partialTick, LightTexture.FULL_SKY,
                packedOverlay, color);
    }

    private static float releasePulse(WorldEcho.AttackMode mode, float age) {
        return switch (mode) {
            case WIND, BEAM -> pulseAt(age, 34);
            case TIDE -> pulseAt(age, 20);
            case TRUE_SIGHT, SLAM, FLAME_IMPACT -> pulseAt(age, 22);
            case GRAVITY -> pulseAt(age, 28);
            case VOID, PHASE -> pulseAt(age, 24);
            case LANCE -> pulseAt(age, 14);
            case BARRAGE -> Math.max(pulseAt(age, 6),
                    Math.max(pulseAt(age, 18), pulseAt(age, 30)));
            case METEOR, SKYFALL -> pulseAt(age, 32);
            case NOVA -> Math.max(pulseAt(age, 14),
                    Math.max(pulseAt(age, 26), pulseAt(age, 38)));
            case WING_STORM -> Math.max(Math.max(pulseAt(age, 11), pulseAt(age, 17)),
                    Math.max(pulseAt(age, 23), pulseAt(age, 29)));
            case AERIAL_BARRAGE -> Math.max(pulseAt(age, 24),
                    Math.max(pulseAt(age, 34), pulseAt(age, 44)));
            case AERIAL_REAP -> pulseAt(age, 29);
            case NONE -> 0.0F;
        };
    }

    private static float pulseAt(float age, int releaseTick) {
        return Math.max(0.0F, 1.0F - Math.abs(age - releaseTick) / 4.0F);
    }

    private static float flowSpeed(WorldEcho.AttackMode mode) {
        return switch (mode) {
            case BARRAGE, BEAM, SKYFALL, WING_STORM, AERIAL_BARRAGE, AERIAL_REAP -> 1.8F;
            case WIND, GRAVITY, NOVA, PHASE -> 1.35F;
            case TIDE, TRUE_SIGHT, VOID, METEOR -> 0.9F;
            default -> 1.1F;
        };
    }

    private static int themeColor(WorldEcho.AttackMode mode) {
        return switch (mode) {
            case WIND -> 0x86F1FF;
            case TIDE -> 0x45BFEA;
            case TRUE_SIGHT -> 0xD09BFF;
            case GRAVITY -> 0xFFD982;
            case VOID -> 0xA45AFF;
            case LANCE -> 0xE8FCFF;
            case SLAM -> 0xFFC66D;
            case BARRAGE -> 0x9DF4FF;
            case BEAM -> 0xC6FBFF;
            case FLAME_IMPACT -> 0xFF7538;
            case METEOR -> 0xD47EFF;
            case NOVA -> 0xF6E8FF;
            case SKYFALL -> 0xFFF0A8;
            case WING_STORM -> 0x7FE7FF;
            case AERIAL_BARRAGE -> 0xA8F4FF;
            case AERIAL_REAP -> 0xE4B2FF;
            case PHASE -> 0xF2D7FF;
            case NONE -> 0xFFFFFF;
        };
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
