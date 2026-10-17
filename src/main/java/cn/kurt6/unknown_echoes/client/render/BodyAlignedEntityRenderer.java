package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 将生态模型的主体对齐物理箱；在转向和原版体型缩放后使用局部坐标。 */
public class BodyAlignedEntityRenderer<T extends Mob & GeoAnimatable> extends GeoEntityRenderer<T> {
    protected BodyAlignedEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        EntityHitboxProfiles.Profile profile = EntityHitboxProfiles.profile(animatable);
        if (profile != null && profile.alignModel()) {
            float ageScale = animatable.getAgeScale();
            poseStack.scale(ageScale, ageScale, ageScale);
            poseStack.translate(-profile.centerX(), -profile.floorY(), -profile.centerZ());
        }
    }
}
