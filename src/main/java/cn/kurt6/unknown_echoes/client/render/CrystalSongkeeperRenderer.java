package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 晶歌守谱者 GeckoLib 渲染器。 */
public class CrystalSongkeeperRenderer extends GeoEntityRenderer<CrystalSongkeeper> {
    public CrystalSongkeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("crystal_songkeeper"), true));
        this.shadowRadius = 0.65F;
        this.withScale(1.15F);
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(UnknownEchoes.MODID,
                        "textures/entity/crystal_songkeeper_glowing.png")));
    }
}
