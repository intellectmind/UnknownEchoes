package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.BrokenBellKeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 残钟看守 GeckoLib 渲染器。 */
public class BrokenBellKeeperRenderer extends GeoEntityRenderer<BrokenBellKeeper> {
    public BrokenBellKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("broken_bell_keeper"), true));
        this.shadowRadius = 0.75F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/broken_bell_keeper_glowing.png")));
    }
}
