package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.BrokenBellGuard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 残钟守卫 GeckoLib 渲染器。
 */
public class BrokenBellGuardRenderer extends GeoEntityRenderer<BrokenBellGuard> {

    public BrokenBellGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(UnknownEchoes.id("broken_bell_guard"), true));
        this.shadowRadius = 0.55F;
    }
}
