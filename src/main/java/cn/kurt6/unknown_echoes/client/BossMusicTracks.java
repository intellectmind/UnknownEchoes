package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/** 每个 Boss 只使用一条循环 BGM；战斗阶段不再切换曲目。 */
final class BossMusicTracks {
    private BossMusicTracks() {}

    @Nullable
    static SoundEvent forCue(ResourceLocation boss, int stage) {
        if (!"unknown_echoes".equals(boss.getNamespace())) return null;
        return switch (boss.getPath()) {
            case "forgotten_colossus" -> ModSounds.COLOSSUS_MUSIC.get();
            case "abyss_watcher" -> ModSounds.ABYSS_WATCHER_MUSIC.get();
            case "mirror_guardian" -> ModSounds.MIRROR_GUARDIAN_MUSIC.get();
            case "void_prophet" -> ModSounds.VOID_PROPHET_MUSIC.get();
            case "rift_warden" -> ModSounds.RIFT_WARDEN_MUSIC.get();
            case "world_echo" -> ModSounds.WORLD_ECHO_MUSIC.get();
            case "storm_weaver", "silent_priest", "tide_lantern_keeper",
                 "mirror_dust_butler", "crystal_songkeeper", "broken_bell_keeper",
                 "dream_bloom_keeper", "lost_recorder_chief", "inverted_gatekeeper",
                 "ash_scribe" -> ModSounds.MINIBOSS_MUSIC.get();
            default -> null;
        };
    }
}
