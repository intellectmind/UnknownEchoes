package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.world.RealmTimeCycle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class RealmDimensionEffects extends DimensionSpecialEffects {
    private final boolean deep;

    private RealmDimensionEffects(boolean deep) {
        super(deep ? Float.NaN : 192.0F, true, SkyType.NORMAL, false, false);
        this.deep = deep;
    }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(UnknownEchoes.id("echo_realm"), new RealmDimensionEffects(false));
        event.register(UnknownEchoes.id("deep_echoes"), new RealmDimensionEffects(true));
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        ClientLevel level = Minecraft.getInstance().level;
        double dusk = level == null ? 0.0D : RealmTimeCycle.dusk(level.getDayTime(), deep);
        double glow = (deep ? 0.10D : 0.16D) + dusk * (deep ? 0.08D : 0.13D);
        return fogColor.multiply(glow * 0.82D, glow * 0.90D, glow);
    }

    @Override
    public boolean isFoggyAt(int x, int z) { return false; }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) { return null; }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick,
                                     LightTexture lightTexture, double x, double y, double z) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) { return true; }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken,
                                     float blockFlicker, float skyLight, int block, int sky,
                                     Vector3f colors) {
        float lift = RealmTimeCycle.ambientLift(level.getDayTime(), deep, block);
        colors.add(lift * 0.65F, lift * 0.8F, lift);
    }
}
