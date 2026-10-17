package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientDisasterAudio {
    private static final DisasterAudioState STATE = new DisasterAudioState();
    private static final int MAX_VOICES = 4;
    private static final List<Voice> VOICES = new ArrayList<>();
    private static ClientLevel level;

    private ClientDisasterAudio() { }

    private record Voice(SoundInstance sound, long started, long eventStart) { }

    public static void accept(RealmDisasterPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (!payload.valid() || level == null || !payload.dimension().equals(level.dimension().location())) return;
        if (ClientConfig.DISASTER_SOUND_VOLUME.get() == 0) { silence(); return; }
        STATE.accept(payload);
        if (!payload.impact() && payload.strength() == 0) VOICES.removeIf(voice -> {
            if (voice.eventStart() != payload.eventStart()) return false;
            mc.getSoundManager().stop(voice.sound());
            return true;
        });
    }

    private static void silence() {
        var manager = Minecraft.getInstance().getSoundManager();
        VOICES.forEach(voice -> manager.stop(voice.sound()));
        VOICES.clear();
        STATE.clear();
    }

    private static void ensureLevel(ClientLevel current) {
        if (level == current) return;
        silence();
        level = current;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ensureLevel(null); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level == null || mc.player == null || mc.isPaused()) return;
        double volume = ClientConfig.DISASTER_SOUND_VOLUME.get();
        if (volume == 0) { silence(); return; }
        var manager = mc.getSoundManager();
        VOICES.removeIf(voice -> {
            if (STATE.clock() - voice.started() <= 2 || manager.isActive(voice.sound()) && STATE.clock() - voice.started() < 120) return false;
            manager.stop(voice.sound());
            return true;
        });
        for (DisasterAudioState.Cue cue : STATE.tick(mc.player.position())) {
            SoundEvent sound = cue.rumble()
                    ? cue.deep() ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.LIGHTNING_BOLT_THUNDER
                    : cue.deep() ? SoundEvents.DEEPSLATE_BREAK : SoundEvents.LIGHTNING_BOLT_IMPACT;
            float pitch = cue.rumble() ? cue.deep() ? 0.62F : 0.72F : cue.deep() ? 0.92F : 1.1F;
            var point = cue.center();
            var instance = new SimpleSoundInstance(sound.getLocation(), SoundSource.WEATHER, (float) (cue.volume() * volume), pitch,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, point.x, point.y, point.z, false);
            if (VOICES.size() >= MAX_VOICES) manager.stop(VOICES.removeFirst().sound());
            manager.play(instance);
            VOICES.add(new Voice(instance, STATE.clock(), cue.eventStart()));
        }
    }
}
