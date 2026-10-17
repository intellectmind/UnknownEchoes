package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.BossMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ClientBossMusic {
    // 短于 6 tick 的往返不换曲，避免机制边界抖动不断重播曲头。
    private static final int STABLE_CUE_TICKS = 6;
    private static BossMusicPayload cue;
    private static int leaseTicks;
    private static ResourceLocation dimension;
    private static BossMusicSound current;
    private static BossMusicSound outgoing;
    private static SoundEvent playing;
    private static SoundEvent outgoingTrack;
    private static SoundEvent candidate;
    private static int stableTicks;
    private static int transitionTicks;

    private ClientBossMusic() {}

    public static void accept(BossMusicPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(payload.dimension())) return;
        if (dimension != null && !dimension.equals(payload.dimension())) clear(mc);
        dimension = payload.dimension();
        cue = payload;
        leaseTicks = 50;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            clear(mc);
            return;
        }
        ResourceLocation here = mc.level.dimension().location();
        if (dimension != null && !dimension.equals(here)) clear(mc);
        dimension = here;
        if (leaseTicks > 0) leaseTicks--;
        boolean muted = cue != null && leaseTicks > 0 && cue.muted();
        if (muted || !ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS.get()) {
            if (muted) mc.getMusicManager().stopPlaying();
            stopTracks(mc);
            return;
        }
        SoundEvent wanted = cue != null && leaseTicks > 0 && !cue.muted() && mc.player.isAlive()
                ? BossMusicTracks.forCue(cue.boss(), cue.stage()) : null;
        if (wanted != candidate) {
            candidate = wanted;
            stableTicks = 0;
        } else stableTicks = Math.min(STABLE_CUE_TICKS, stableTicks + 1);
        if (current != null || outgoing != null) mc.getMusicManager().stopPlaying();
        if (transitionTicks > 0) transitionTicks--;
        if (outgoing != null && (outgoing.isStopped() || transitionTicks == 0)) {
            mc.getSoundManager().stop(outgoing);
            outgoing = null;
            outgoingTrack = null;
        }
        if (wanted == null) {
            if (current != null) {
                if (outgoing != null) mc.getSoundManager().stop(outgoing);
                outgoing = current;
                outgoingTrack = playing;
                outgoing.fadeOut();
                current = null;
                playing = null;
                transitionTicks = BossMusicSound.FADE_TICKS;
            }
            return;
        }
        if (current != null && transitionTicks == 0 && !mc.getSoundManager().isActive(current)) {
            mc.getSoundManager().stop(current);
            current = null;
            playing = null;
        }
        if (wanted.equals(playing)) return;
        if (current != null && stableTicks < STABLE_CUE_TICKS) return;
        if (outgoing != null && wanted.equals(outgoingTrack)) {
            BossMusicSound previous = current;
            SoundEvent previousTrack = playing;
            current = outgoing;
            playing = outgoingTrack;
            current.fadeIn();
            outgoing = previous;
            outgoingTrack = previousTrack;
            if (outgoing != null) outgoing.fadeOut();
            transitionTicks = BossMusicSound.FADE_TICKS;
            return;
        }
        // 当前过渡结束后再接最新 Boss，任何时刻最多保留两条流式音轨。
        if (outgoing != null) return;
        outgoing = current;
        outgoingTrack = playing;
        if (outgoing != null) outgoing.fadeOut();
        current = new BossMusicSound(wanted);
        playing = wanted;
        transitionTicks = BossMusicSound.FADE_TICKS;
        mc.getMusicManager().stopPlaying();
        mc.getSoundManager().play(current);
    }

    private static void stopTracks(Minecraft mc) {
        if (current != null) mc.getSoundManager().stop(current);
        if (outgoing != null) mc.getSoundManager().stop(outgoing);
        current = null;
        outgoing = null;
        playing = null;
        outgoingTrack = null;
        candidate = null;
        stableTicks = 0;
        transitionTicks = 0;
    }

    public static void clear(Minecraft mc) {
        stopTracks(mc);
        cue = null;
        dimension = null;
        leaseTicks = 0;
    }
}
