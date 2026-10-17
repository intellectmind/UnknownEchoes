package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.entity.boss.BossParticleTheme;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossFxTheme;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;

public enum BossLifecycleStyle {
    ARCANE, STONE, TIDE, MIRROR, GRAVITY, RIFT, WORLD, EMBER, BLOOM, SCRIPT;

    private static final BossLifecycleStyle[] WIRE_VALUES = values();

    // 生命周期形状的 span 保存离散风格编号；0 保留为通用演出。
    public static BossLifecycleStyle of(CombatFxPayload cue) {
        if (!BossLifecycleGeometry.isLifecycle(cue.shape())) return ARCANE;
        int value = (int) cue.span();
        return value >= 0 && value < WIRE_VALUES.length && value == cue.span() ? WIRE_VALUES[value] : ARCANE;
    }

    public static BossLifecycleStyle forParticle(ParticleOptions theme) {
        if (theme instanceof DustColorTransitionOptions dust) return forMiniColor(CombatFx.color(dust.getFromColor()));
        if (theme instanceof DustParticleOptions dust) return forMiniColor(CombatFx.color(dust.getColor()));
        if (theme == BossParticleTheme.COLOSSUS.body()) return STONE;
        if (theme == BossParticleTheme.ABYSS.body()) return TIDE;
        if (theme == BossParticleTheme.MIRROR.body()) return MIRROR;
        if (theme == BossParticleTheme.VOID_PROPHET.body()) return GRAVITY;
        if (theme == BossParticleTheme.RIFT.body()) return RIFT;
        return ARCANE;
    }

    public static BossLifecycleStyle forMiniColor(int color) {
        if (matches(color, MiniBossFxTheme.ASH_SCRIBE)) return EMBER;
        if (matches(color, MiniBossFxTheme.BROKEN_BELL_KEEPER)) return STONE;
        if (matches(color, MiniBossFxTheme.CRYSTAL_SONGKEEPER) || matches(color, MiniBossFxTheme.MIRROR_DUST_BUTLER)) return MIRROR;
        if (matches(color, MiniBossFxTheme.DREAM_BLOOM_KEEPER)) return BLOOM;
        if (matches(color, MiniBossFxTheme.INVERTED_GATEKEEPER)) return GRAVITY;
        if (matches(color, MiniBossFxTheme.LOST_RECORDER_CHIEF) || matches(color, MiniBossFxTheme.SILENT_PRIEST)) return SCRIPT;
        if (matches(color, MiniBossFxTheme.STORM_WEAVER)) return RIFT;
        if (matches(color, MiniBossFxTheme.TIDE_LANTERN_KEEPER)) return TIDE;
        return ARCANE;
    }

    public static BossLifecycleStyle forProgression(ProgressionFx.Theme theme) {
        return switch (theme) {
            case WIND -> STONE;
            case TIDE -> TIDE;
            case TRUE_SIGHT -> MIRROR;
            case GRAVITY -> GRAVITY;
            case VOID -> RIFT;
            case WORLD -> WORLD;
        };
    }

    private static boolean matches(int color, MiniBossFxTheme.Theme theme) {
        return color == MiniBossFxTheme.packedCore(theme) || color == CombatFx.color(theme.core());
    }
}
