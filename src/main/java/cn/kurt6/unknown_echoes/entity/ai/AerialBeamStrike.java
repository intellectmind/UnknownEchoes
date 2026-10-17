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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AerialBeamStrike {
    private AerialBeamStrike() {
    }

    private static Vec3 endpoint(Mob mob, Vec3 start, Vec3 end) {
        if (start == null || end == null || !Double.isFinite(start.x) || !Double.isFinite(start.y)
                || !Double.isFinite(start.z) || !Double.isFinite(end.x) || !Double.isFinite(end.y)
                || !Double.isFinite(end.z)) {
            return null;
        }
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (!Double.isFinite(length) || length > 12.01D || length < 0.001D) {
            return null;
        }
        // BlockGetter.traverseBlocks 会将两端各外推 1e-7，区块检查必须覆盖实际遍历范围。
        Vec3 from = start.lerp(end, -1.0e-7D);
        Vec3 to = end.lerp(start, -1.0e-7D);
        BlockPos low = BlockPos.containing(Math.min(from.x, to.x), Math.min(from.y, to.y),
                Math.min(from.z, to.z));
        BlockPos high = BlockPos.containing(Math.max(from.x, to.x), Math.max(from.y, to.y),
                Math.max(from.z, to.z));
        // 连续包络覆盖擦过区块角落的短段；12 格射线最多跨 2×2 个水平区块。
        if (low.getY() < mob.level().getMinBuildHeight() || high.getY() >= mob.level().getMaxBuildHeight()
                || !AerialFlight.hasReadableChunks(mob.level(), low, high)) {
            return null;
        }
        return mob.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, mob)).getLocation();
    }

    public static void particles(Mob mob, Vec3 start, Vec3 end, boolean release) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive() || mob.isNoAi()) {
            return;
        }
        Vec3 clipped = endpoint(mob, start, end);
        if (clipped == null) {
            return;
        }
        emitParticles(level, start, clipped, release);
    }

    private static void emitParticles(ServerLevel level, Vec3 start, Vec3 clipped, boolean release) {
        AerialAttackFx.beam(level, start, clipped, release);
        if (!release) {
            return;
        }
        Vec3 delta = clipped.subtract(start);
        int count = Math.max(1, (int) Math.ceil(delta.length() / 0.75D));
        for (int i = 0; i <= count; i++) {
            Vec3 pos = start.add(delta.scale((double) i / count));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 2, 0.015D, 0.015D, 0.015D, 0.0D);
        }
    }

    public static void release(Mob mob, LivingEntity target, Vec3 start, Vec3 end) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive() || mob.isNoAi() || target == null
                || !target.isAlive() || target.level() != mob.level() || mob.getTarget() != target
                || !mob.canAttack(target) || mob.isAlliedTo(target)
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        Vec3 clipped = endpoint(mob, start, end);
        if (clipped == null) {
            return;
        }
        if (clipped.subtract(start).length() >= 0.001D) {
            emitParticles(level, start, clipped, true);
        }
        AABB hitbox = target.getBoundingBox();
        if (hitbox.contains(start) || hitbox.clip(start, clipped).isPresent()) {
            target.hurt(mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }
}
