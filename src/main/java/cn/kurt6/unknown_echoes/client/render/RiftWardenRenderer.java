package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 裂隙看守者渲染器：眼部、裂隙与护甲纹路使用独立发光层。 */
public class RiftWardenRenderer extends GeoEntityRenderer<RiftWarden> {
    public RiftWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("rift_warden"), true));
        this.shadowRadius = 2.5F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/rift_warden_glowing.png")));
    }
}
