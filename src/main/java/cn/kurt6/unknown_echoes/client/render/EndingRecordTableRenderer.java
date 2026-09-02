package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.deep.EndingRecordTableBlockEntity;
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

/** 以三套附加几何表现誊写、封印与归档，阶段之间做交叠变形。 */
public class EndingRecordTableRenderer
        implements BlockEntityRenderer<EndingRecordTableBlockEntity> {
    public static final ModelResourceLocation[] KINETIC_MODELS = {
            model("writing"),
            model("sealing"),
            model("archived")
    };

    public EndingRecordTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EndingRecordTableBlockEntity table, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        float age = table.animationAge(partialTick);
        if (age < 0.0F || age >= EndingRecordTableBlockEntity.TOTAL_TICKS) {
            return;
        }
        if (age < EndingRecordTableBlockEntity.WRITING_TICKS + 16.0F) {
            renderWriting(table, age, poseStack, bufferSource);
        }
        if (age >= EndingRecordTableBlockEntity.WRITING_TICKS - 10.0F
                && age < EndingRecordTableBlockEntity.WRITING_TICKS
                + EndingRecordTableBlockEntity.SEALING_TICKS + 18.0F) {
            renderSealing(table, age, poseStack, bufferSource);
        }
        if (age >= EndingRecordTableBlockEntity.WRITING_TICKS
                + EndingRecordTableBlockEntity.SEALING_TICKS - 12.0F) {
            renderArchived(table, age, poseStack, bufferSource);
        }
    }

    private static void renderWriting(EndingRecordTableBlockEntity table, float age,
                                      PoseStack poseStack, MultiBufferSource bufferSource) {
        float deploy = smooth(Mth.clamp(age / 18.0F, 0.0F, 1.0F));
        float retire = smooth(Mth.clamp((EndingRecordTableBlockEntity.WRITING_TICKS + 16.0F - age)
                / 16.0F, 0.0F, 1.0F));
        float visibility = Math.min(deploy, retire);
        float bob = Mth.sin(age * 0.16F) * 0.035F;
        float scale = 0.62F + visibility * 0.38F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.92F, 0.5F);
        poseStack.translate(0.0F, bob - (1.0F - visibility) * 0.42F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(age * 0.055F) * 5.5F));
        poseStack.scale(scale, 0.45F + visibility * 0.55F, scale);
        poseStack.translate(-0.5F, -0.92F, -0.5F);
        renderModel(KINETIC_MODELS[0], table, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderSealing(EndingRecordTableBlockEntity table, float age,
                                      PoseStack poseStack, MultiBufferSource bufferSource) {
        float local = age - (EndingRecordTableBlockEntity.WRITING_TICKS - 10.0F);
        float deploy = smooth(Mth.clamp(local / 20.0F, 0.0F, 1.0F));
        float retireStart = EndingRecordTableBlockEntity.WRITING_TICKS
                + EndingRecordTableBlockEntity.SEALING_TICKS;
        float retire = smooth(Mth.clamp((retireStart + 18.0F - age) / 18.0F, 0.0F, 1.0F));
        float visibility = Math.min(deploy, retire);
        float close = 1.42F - deploy * 0.42F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.02F, 0.5F);
        poseStack.translate(0.0F, (1.0F - visibility) * 0.3F, 0.0F);
        poseStack.scale(close, 0.55F + visibility * 0.45F, 0.88F + visibility * 0.12F);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(local * 0.11F) * 2.0F));
        poseStack.translate(-0.5F, -1.02F, -0.5F);
        renderModel(KINETIC_MODELS[1], table, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderArchived(EndingRecordTableBlockEntity table, float age,
                                       PoseStack poseStack, MultiBufferSource bufferSource) {
        float start = EndingRecordTableBlockEntity.WRITING_TICKS
                + EndingRecordTableBlockEntity.SEALING_TICKS - 12.0F;
        float local = age - start;
        float deploy = backOut(Mth.clamp(local / 28.0F, 0.0F, 1.0F));
        float pulse = 1.0F + Mth.sin(local * 0.13F) * 0.018F * deploy;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.2F, 0.5F);
        poseStack.translate(0.0F, -(1.0F - deploy) * 0.85F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((1.0F - deploy) * 35.0F
                + Mth.sin(local * 0.04F) * 2.5F));
        poseStack.scale((0.55F + deploy * 0.45F) * pulse,
                0.38F + deploy * 0.62F,
                (0.55F + deploy * 0.45F) * pulse);
        poseStack.translate(-0.5F, -1.2F, -0.5F);
        renderModel(KINETIC_MODELS[2], table, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float backOut(float value) {
        float shifted = value - 1.0F;
        float overshoot = 1.28F;
        return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted
                + overshoot * shifted * shifted;
    }

    private static void renderModel(ModelResourceLocation location,
                                    EndingRecordTableBlockEntity table,
                                    PoseStack poseStack, MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(location);
        if (model == minecraft.getModelManager().getMissingModel()) {
            return;
        }
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), bufferSource.getBuffer(Sheets.cutoutBlockSheet()),
                table.getBlockState(), model, 1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public AABB getRenderBoundingBox(EndingRecordTableBlockEntity table) {
        return new AABB(table.getBlockPos()).expandTowards(0.0D, 1.5D, 0.0D)
                .inflate(0.75D, 0.0D, 0.75D);
    }

    @Override
    public int getViewDistance() {
        return 80;
    }

    private static ModelResourceLocation model(String suffix) {
        return ModelResourceLocation.standalone(
                UnknownEchoes.id("block/ending_record_table_kinetic_" + suffix));
    }
}
