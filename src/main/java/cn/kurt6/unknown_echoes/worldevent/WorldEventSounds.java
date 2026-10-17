package cn.kurt6.unknown_echoes.worldevent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.List;

final class WorldEventSounds {
    private WorldEventSounds() {
    }

    static void pulse(List<ServerPlayer> nearbyPlayers, SoundEvent sound, float volume, float pitch) {
        // 环境声覆盖同一批事件参与者；低音量的中心广播只有 16 格，覆盖不到事件外圈。
        for (ServerPlayer player : nearbyPlayers) {
            player.playNotifySound(sound, SoundSource.AMBIENT, volume, pitch);
        }
    }
}
