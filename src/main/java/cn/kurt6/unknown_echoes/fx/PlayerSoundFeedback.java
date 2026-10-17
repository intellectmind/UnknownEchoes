package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

public final class PlayerSoundFeedback {
    private static final int DENIAL_INTERVAL_TICKS = 10;
    private static final Map<ServerPlayer, Long> LAST_DENIAL = new WeakHashMap<>();

    private PlayerSoundFeedback() { }

    public static void deny(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        long now = serverPlayer.server.overworld().getGameTime();
        Long last = LAST_DENIAL.get(serverPlayer);
        if (last != null && now >= last && now - last < DENIAL_INTERVAL_TICKS) return;
        LAST_DENIAL.put(serverPlayer, now);
        serverPlayer.playNotifySound(ModSounds.FEEDBACK_DENIED.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
    }
}
