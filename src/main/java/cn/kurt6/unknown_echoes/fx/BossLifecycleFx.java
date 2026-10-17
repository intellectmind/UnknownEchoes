package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 只在成功召唤、确认破防和已提交死亡之后调用；坐标固定在事件发生处。 */
public final class BossLifecycleFx {
    private BossLifecycleFx() { }

    public static void summon(ServerLevel level, Vec3 feet, double width, double height, ParticleOptions theme) {
        emit(level, plan(Shape.BOSS_SUMMON, feet, width, height,
                CombatFx.particleColor(theme), CombatFx.particleColor(theme), BossLifecycleStyle.forParticle(theme)));
    }

    public static void death(ServerLevel level, Vec3 feet, double width, double height, ParticleOptions theme) {
        death(level, feet, width, height, theme, BossLifecycleStyle.forParticle(theme));
    }

    public static void death(ServerLevel level, Vec3 feet, double width, double height,
                             ParticleOptions theme, BossLifecycleStyle style) {
        int color = CombatFx.particleColor(theme);
        emit(level, plan(Shape.BOSS_DEATH, feet, width, height, color, color, style));
    }

    public static void death(ServerLevel level, Vec3 feet, double width, double height, int color) {
        emit(level, plan(Shape.BOSS_DEATH, feet, width, height, color, color, BossLifecycleStyle.forMiniColor(color)));
    }

    public static void phase(ServerLevel level, Vec3 feet, double width, double height, ParticleOptions theme) {
        phase(level, feet, width, height, theme, theme);
    }

    public static void phase(ServerLevel level, Vec3 feet, double width, double height,
                             ParticleOptions previous, ParticleOptions next) {
        phase(level, feet, width, height, previous, next, BossLifecycleStyle.forParticle(next));
    }

    public static void phase(ServerLevel level, Vec3 feet, double width, double height,
                             ParticleOptions previous, ParticleOptions next, BossLifecycleStyle style) {
        emit(level, plan(Shape.BOSS_PHASE, feet, width, height,
                CombatFx.particleColor(next), CombatFx.particleColor(previous), style));
    }

    public static CombatFxPayload plan(Shape shape, Vec3 feet, double width, double height, int core, int edge) {
        return plan(shape, feet, width, height, core, edge, BossLifecycleStyle.ARCANE);
    }

    public static CombatFxPayload plan(Shape shape, Vec3 feet, double width, double height,
                                       int core, int edge, BossLifecycleStyle style) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0
                || style == null || !BossLifecycleGeometry.isLifecycle(shape)) return null;
        double radius = Math.min(12, Math.max(1.8, width * .72 + 1.1));
        double rise = Math.min(24, Math.max(2.4, height * 1.12));
        CombatFxPayload cue = new CombatFxPayload(shape, feet, Vec3.ZERO, (float) radius,
                (float) rise, style.ordinal(), core, edge, 30, true, -1);
        return cue.valid() ? cue : null;
    }

    private static void emit(ServerLevel level, CombatFxPayload cue) {
        if (cue == null) return;
        CombatFx.emit(level, cue.shape(), cue.origin(), cue.direction(), cue.radius(), cue.length(),
                cue.span(), cue.core(), cue.edge(), cue.duration(), true);
    }
}
