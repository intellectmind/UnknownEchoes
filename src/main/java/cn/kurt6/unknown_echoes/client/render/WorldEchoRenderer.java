package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 世界回响体基础渲染：独立骨架 + 发光层（核心/晶体/符文自发光），战斗粒子区分最终形态。 */
public class WorldEchoRenderer extends GeoEntityRenderer<WorldEcho> {
    public WorldEchoRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("world_echo"), true));
        this.withScale(2.0F);
        this.shadowRadius = 6.4F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/world_echo_glowing.png")));
    }
}
