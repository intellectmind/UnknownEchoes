package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/** 按颈部活动空间限制视线转幅；独立视线骨保留造型初始倾角，动作仍由子骨承接。 */
public class LimitedHeadGeoModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {
    private final float maxYaw;
    private final float minPitch;
    private final float maxPitch;

    public LimitedHeadGeoModel(ResourceLocation asset, String lookBone,
                               float maxYaw, float minPitch, float maxPitch) {
        super(asset, lookBone);
        if (!Float.isFinite(maxYaw) || !Float.isFinite(minPitch) || !Float.isFinite(maxPitch)
                || maxYaw <= 0 || minPitch > 0 || maxPitch < 0) {
            throw new IllegalArgumentException("Invalid head look range");
        }
        this.maxYaw = maxYaw;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        GeoBone head = getAnimationProcessor().getBone(this.headBone);
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) {
            return;
        }
        // 每次从烘焙初始值计算，避免暂停、重复绘制或换实体时累加上一次视线。
        head.setRotX(head.getInitialSnapshot().getRotX()
                + Mth.clamp(data.headPitch(), this.minPitch, this.maxPitch) * Mth.DEG_TO_RAD);
        head.setRotY(head.getInitialSnapshot().getRotY()
                + Mth.clamp(data.netHeadYaw(), -this.maxYaw, this.maxYaw) * Mth.DEG_TO_RAD);
    }
}
