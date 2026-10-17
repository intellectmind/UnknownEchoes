package cn.kurt6.unknown_echoes.entity.ai;

import cn.kurt6.unknown_echoes.fx.AerialAttackFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public final class AerialPulseStrike {
    public static final double RADIUS = 3.0D;
    // 覆盖原版射线向两端外推的范围；该余量只用于区块查询，不扩大伤害球。
    private static final double QUERY_RADIUS = RADIUS + 1.0e-6D;

    private AerialPulseStrike() {
    }

    private static boolean loadedArea(Mob mob, Vec3 center) {
        return center != null && Double.isFinite(center.x) && Double.isFinite(center.y)
                && Double.isFinite(center.z) && center.y - QUERY_RADIUS >= mob.level().getMinBuildHeight()
                && center.y + QUERY_RADIUS < mob.level().getMaxBuildHeight()
                && AerialFlight.hasReadableChunks(mob.level(), BlockPos.containing(center.add(-QUERY_RADIUS, -QUERY_RADIUS, -QUERY_RADIUS)),
                        BlockPos.containing(center.add(QUERY_RADIUS, QUERY_RADIUS, QUERY_RADIUS)));
    }

    public static void particles(Mob mob, Vec3 center, boolean release) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive() || mob.isNoAi()
                || !loadedArea(mob, center)) {
            return;
        }
        emitParticles(level, center, release);
    }

    private static void emitParticles(ServerLevel level, Vec3 center, boolean release) {
        AerialAttackFx.pulse(level, center, RADIUS, release);
        if (!release) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6.0D;
            double a = Math.cos(angle) * RADIUS;
            double b = Math.sin(angle) * RADIUS;
            for (int plane = 0; plane < 2; plane++) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x + a, center.y + (plane == 0 ? 0.0D : b),
                        center.z + (plane == 0 ? b : 0.0D), 2,
                        0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    public static void release(Mob mob, LivingEntity target, Vec3 center) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive() || mob.isNoAi()
                || target == null || !target.isAlive() || target.level() != mob.level()
                || !mob.canAttack(target) || mob.isAlliedTo(target)
                || mob.getTarget() != target || target instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (!loadedArea(mob, center)) {
            return;
        }
        emitParticles(level, center, true);
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        if (center.distanceToSqr(targetCenter) > RADIUS * RADIUS) {
            return;
        }
        Vec3 hit = mob.level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, mob)).getLocation();
        if (hit.distanceToSqr(targetCenter) <= 1.0e-6D) {
            target.hurt(mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }
}
