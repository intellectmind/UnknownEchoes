package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WorldResonanceAltarRenderer
        implements BlockEntityRenderer<WorldResonanceAltarBlockEntity> {
    public static final ModelResourceLocation[] KINETIC_MODELS = {
            model("wind"),
            model("tide"),
            model("true_sight"),
            model("gravity"),
            model("void"),
            model("core")
    };

    private static final float DEPLOY_TICKS = 18.0F;
    private static final float CORE_DEPLOY_TICKS = 28.0F;
    private static final EchoAbilityType[] ABILITIES = EchoAbilityType.values();

    public WorldResonanceAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WorldResonanceAltarBlockEntity altar, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (altar.getLevel() == null) {
            return;
        }
        float time = altar.getLevel().getGameTime() + partialTick;
        for (int index = 0; index < ABILITIES.length; index++) {
            EchoAbilityType ability = ABILITIES[index];
            if (altar.hasResonance(ability)) {
                renderResonator(altar, ability, index, time, poseStack, bufferSource);
            }
        }
        if (altar.isFullyResonant()) {
            renderCore(altar, time, poseStack, bufferSource);
        }
    }

    private static void renderResonator(WorldResonanceAltarBlockEntity altar,
                                        EchoAbilityType ability, int index,
                                        float time, PoseStack poseStack,
                                        MultiBufferSource bufferSource) {
        float progress = activationProgress(altar, index, time, DEPLOY_TICKS);
        Vec3 pivot = WorldResonanceAltarBlockEntity.localResonatorOffset(ability);
        float idle = Mth.sin(time * (0.075F + index * 0.006F) + index * 1.7F);
        float lift = -0.34F * (1.0F - progress);
        float scaleXZ = 0.68F + progress * 0.32F;
        float scaleY = 0.42F + progress * 0.58F;
        float rotation = switch (index) {
            case 0 -> idle * 3.5F + (1.0F - progress) * 28.0F;
            case 1 -> idle * 2.0F - (1.0F - progress) * 22.5F;
            case 2 -> idle * 2.8F + (1.0F - progress) * 45.0F;
            case 3 -> time * 0.85F + (1.0F - progress) * 35.0F;
            default -> idle * 4.5F - (1.0F - progress) * 45.0F;
        };
        float breathing = switch (index) {
            case 0 -> idle * 0.018F;
            case 1 -> idle * 0.03F;
            case 2 -> idle * 0.012F;
            case 3 -> -Math.abs(idle) * 0.018F;
            default -> idle * 0.022F;
        };

        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.translate(0.0F, lift + breathing, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scaleXZ, scaleY, scaleXZ);
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
        renderModel(KINETIC_MODELS[index], altar, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderCore(WorldResonanceAltarBlockEntity altar, float time,
                                   PoseStack poseStack, MultiBufferSource bufferSource) {
        float progress = activationProgress(altar, altar.getLastResonanceIndex(),
                time, CORE_DEPLOY_TICKS);
        float ritualPulse = altar.isRitualInProgress()
                ? 1.0F + Mth.sin((time - altar.getStateChangedAt()) * 0.22F) * 0.045F
                : 1.0F + Mth.sin(time * 0.07F) * 0.018F;
        float scale = (0.58F + progress * 0.42F) * ritualPulse;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.12F, 0.5F);
        poseStack.translate(0.0F, -0.72F * (1.0F - progress), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.15F));
        poseStack.scale(scale, 0.45F + progress * 0.55F, scale);
        poseStack.translate(-0.5F, -1.12F, -0.5F);
        renderModel(KINETIC_MODELS[5], altar, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static float activationProgress(WorldResonanceAltarBlockEntity altar, int index,
                                            float time, float duration) {
        if (index < 0 || index != altar.getLastResonanceIndex()
                || altar.getLastResonanceAt() < 0L) {
            return 1.0F;
        }
        float linear = Mth.clamp((time - altar.getLastResonanceAt()) / duration, 0.0F, 1.0F);
        float shifted = linear - 1.0F;
        float overshoot = 1.35F;
        return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted
                + overshoot * shifted * shifted;
    }

    private static void renderModel(ModelResourceLocation location,
                                    WorldResonanceAltarBlockEntity altar,
                                    PoseStack poseStack, MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(location);
        if (model == minecraft.getModelManager().getMissingModel()) {
            return;
        }
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), bufferSource.getBuffer(Sheets.solidBlockSheet()),
                altar.getBlockState(), model, 1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public AABB getRenderBoundingBox(WorldResonanceAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).expandTowards(0.0D, 1.5D, 0.0D)
                .inflate(0.75D, 0.0D, 0.75D);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static ModelResourceLocation model(String suffix) {
        return ModelResourceLocation.standalone(
                UnknownEchoes.id("block/world_resonance_altar_kinetic_" + suffix));
    }
}
