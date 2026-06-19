package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 潮汐弩矢:复用原版箭模型与贴图(弹道差异在实体逻辑,不需要专属外观)。 */
public class TideBoltRenderer extends ArrowRenderer<TideBoltEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public TideBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TideBoltEntity entity) {
        return TEXTURE;
    }
}
