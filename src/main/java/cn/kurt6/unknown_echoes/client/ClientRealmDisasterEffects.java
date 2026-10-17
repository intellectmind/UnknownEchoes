package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientRealmDisasterEffects {
    private static ClientLevel activeLevel;
    private static final DisasterScreenState STATE = new DisasterScreenState();

    private ClientRealmDisasterEffects() { }

    public static void accept(RealmDisasterPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != activeLevel) clear();
        if (mc.level == null || mc.player == null || !payload.valid()
                || !mc.level.dimension().location().equals(payload.dimension())) return;
        activeLevel = mc.level;
        STATE.accept(payload, mc.player.position());
        ClientRealmStormScene.accept(payload);
        ClientDisasterAftermath.accept(payload);
        ClientDisasterAudio.accept(payload);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != activeLevel || mc.player == null) {
            clear();
            return;
        }
        if (mc.isPaused()) return;
        STATE.tick(mc.player.position());
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!current(mc) || mc.isPaused()) return;
        float partial = (float) event.getPartialTick();
        float pulse = STATE.impact(partial);
        float wind = STATE.strength(partial);
        float scale = ClientConfig.DISASTER_SCREEN_SHAKE.get().floatValue();
        double time = mc.level.getGameTime() + partial;
        float shake = (pulse * 1.1F + wind * 0.08F) * scale;
        var direction = STATE.impactDirection(partial);
        double yaw = Math.toRadians(event.getYaw());
        float forward = (float) (-direction.x * Math.sin(yaw) + direction.z * Math.cos(yaw));
        float side = (float) (direction.x * Math.cos(yaw) + direction.z * Math.sin(yaw));
        event.setPitch(event.getPitch() + (float) Math.sin(time * 1.9D) * shake + forward * 0.55F * scale);
        event.setYaw(event.getYaw() + (float) Math.sin(time * 1.3D) * shake * 0.45F);
        event.setRoll(event.getRoll() + (float) Math.cos(time * 1.6D) * shake * 0.7F + side * 0.7F * scale);
    }

    @SubscribeEvent
    public static void screen(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!current(mc) || mc.options.hideGui || !ClientConfig.DISASTER_SCREEN_EFFECTS.get()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int alpha = Math.min(42, (int) (STATE.strength(partial) * 16 + STATE.impact(partial) * 28));
        if (alpha < 1) return;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int band = Math.max(1, Math.min(width, height) / 100);
        int color = STATE.deep() ? 0x51336D : 0x254555;
        for (int layer = 0; layer < 12; layer++) {
            int inset = band * layer;
            int argb = (alpha * (12 - layer) / 12 << 24) | color;
            event.getGuiGraphics().fill(inset, inset, width - inset, inset + band, argb);
            event.getGuiGraphics().fill(inset, height - inset - band, width - inset, height - inset, argb);
            event.getGuiGraphics().fill(inset, inset + band, inset + band, height - inset - band, argb);
            event.getGuiGraphics().fill(width - inset - band, inset + band, width - inset, height - inset - band, argb);
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        float strength = STATE.strength((float) event.getPartialTick());
        if (!current(mc) || !ClientConfig.DISASTER_SCREEN_EFFECTS.get()
                || event.getType() != FogType.NONE || strength < 0.01F
                || mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.DARKNESS)) return;
        float far = event.getFarPlaneDistance();
        float target = Math.min(far, STATE.deep() ? 56.0F : 72.0F);
        event.setFarPlaneDistance(Mth.lerp(strength * 0.7F, far, target));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), event.getFarPlaneDistance() * 0.6F));
        event.setCanceled(true);
    }

    private static boolean current(Minecraft mc) {
        return mc.level != null && mc.level == activeLevel && mc.player != null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clear() {
        activeLevel = null;
        STATE.clear();
    }
}
