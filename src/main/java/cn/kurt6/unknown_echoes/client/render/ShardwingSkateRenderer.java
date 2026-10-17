package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.ShardwingSkate;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public final class ShardwingSkateRenderer extends BodyAlignedEntityRenderer<ShardwingSkate> {
    public ShardwingSkateRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("shardwing_skate"), false));
        this.shadowRadius = 0.25F;
        addRenderLayer(new EchoGlowLayer<>(this,
                UnknownEchoes.id("textures/entity/shardwing_skate_glowing.png")));
    }

}
