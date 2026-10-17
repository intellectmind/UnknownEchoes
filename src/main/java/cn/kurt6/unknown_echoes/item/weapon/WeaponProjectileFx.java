package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.fx.ProjectileTrailGeometry;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 弹体沿用发射武器的配色，结算仍由弹体持有。 */
public final class WeaponProjectileFx {
    private WeaponProjectileFx() { }

    public static CombatFxPayload tideTrailCue(Vec3 from, Vec3 to) {
        var theme = WeaponFxTheme.TIDE_CROSSBOW;
        return ProjectileTrailGeometry.cue(from, to, CombatFx.color(theme.core()), CombatFx.color(theme.edge()));
    }

    public static void tideImpact(ServerLevel level, Vec3 center, double radius) {
        WeaponFxTheme.Theme theme = WeaponFxTheme.TIDE_CROSSBOW;
        CombatFx.themed(level, Shape.RING, center, Vec3.ZERO, radius, 0, 0,
                theme.core(), theme.edge(), 10);
        WeaponFx.releaseBurst(level, center, theme, 0.75D);
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z,
                8, 0.25D, 0.2D, 0.25D, 0.035D);
    }

    public static void tideTrail(Level level, Vec3 position, Vec3 velocity) {
        if (!level.isClientSide || velocity.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 tail = position.subtract(velocity.normalize().scale(0.3D));
        level.addParticle(WeaponFxTheme.TIDE_CROSSBOW.trace(0.55F),
                tail.x, tail.y, tail.z, 0.0D, 0.0D, 0.0D);
    }
}
