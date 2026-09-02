package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.BossImpactPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** 距离衰减的 Boss 镜头冲击与边缘脉冲，不修改玩家实际朝向。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientBossImpactEffects {
    private static int remainingTicks;
    private static int totalTicks;
    private static float strength;
    private static int color;
    private static double phase;

    private ClientBossImpactEffects() {
    }

    public static void trigger(BossImpactPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientConfig.ENABLE_BOSS_PARTICLES.get()
                || payload.radius() <= 0.0F) {
            return;
        }

        Vec3 center = new Vec3(payload.x(), payload.y(), payload.z());
        double distance = minecraft.player.getEyePosition().distanceTo(center);
        if (distance >= payload.radius()) {
            return;
        }

        float proximity = 1.0F - (float) (distance / payload.radius());
        proximity = proximity * proximity * (3.0F - 2.0F * proximity);
        float density = ClientConfig.BOSS_PARTICLE_DENSITY.get().floatValue();
        float receivedStrength = Mth.clamp(payload.strength() * proximity * density, 0.0F, 1.0F);
        if (receivedStrength < 0.035F) {
            return;
        }

        int receivedDuration = Mth.clamp(payload.durationTicks(), 2, 16);
        float currentEnvelope = currentEnvelope();
        if (remainingTicks > 0 && receivedStrength < currentEnvelope) {
            return;
        }

        color = payload.color() & 0xFFFFFF;
        phase = payload.x() * 0.173D + payload.y() * 0.119D + payload.z() * 0.197D;
        strength = receivedStrength;
        totalTicks = receivedDuration;
        remainingTicks = receivedDuration;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear();
            return;
        }
        if (!minecraft.isPaused() && remainingTicks > 0 && --remainingTicks == 0) {
            clear();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0 || totalTicks <= 0 || !ClientConfig.ENABLE_BOSS_PARTICLES.get()
                || Minecraft.getInstance().isPaused()) {
            return;
        }

        double left = Math.max(0.0D, remainingTicks - event.getPartialTick());
        double envelope = left / totalTicks;
        envelope *= envelope;
        double age = totalTicks - left;
        float amplitude = (float) (1.35D * strength * envelope);
        event.setPitch(event.getPitch() + (float) Math.sin(age * 2.35D + phase) * amplitude);
        event.setYaw(event.getYaw() + (float) Math.sin(age * 1.83D + phase * 1.7D) * amplitude * 0.58F);
        event.setRoll(event.getRoll() + (float) Math.sin(age * 1.31D + phase * 0.7D) * amplitude * 0.34F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (remainingTicks <= 0 || totalTicks <= 0 || !ClientConfig.ENABLE_BOSS_PARTICLES.get()
                || minecraft.options.hideGui || minecraft.isPaused()) {
            return;
        }

        float envelope = remainingTicks / (float) totalTicks;
        envelope *= envelope;
        int alpha = Mth.clamp((int) (58.0F * strength * envelope), 0, 58);
        if (alpha <= 0) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int band = Math.max(2, Math.min(width, height) / 54);
        event.getGuiGraphics().fill(0, 0, width, height,
                ((Math.max(1, alpha / 7)) << 24) | color);
        for (int layer = 0; layer < 3; layer++) {
            int offset = layer * band;
            int layerAlpha = alpha * (3 - layer) / 3;
            int argb = (layerAlpha << 24) | color;
            event.getGuiGraphics().fill(offset, offset, width - offset, offset + band, argb);
            event.getGuiGraphics().fill(offset, height - offset - band, width - offset, height - offset, argb);
            event.getGuiGraphics().fill(offset, offset + band, offset + band, height - offset - band, argb);
            event.getGuiGraphics().fill(width - offset - band, offset + band, width - offset,
                    height - offset - band, argb);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        remainingTicks = 0;
        totalTicks = 0;
        strength = 0.0F;
        color = 0;
        phase = 0.0D;
    }

    private static float currentEnvelope() {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return 0.0F;
        }
        float envelope = remainingTicks / (float) totalTicks;
        return strength * envelope * envelope;
    }
}
