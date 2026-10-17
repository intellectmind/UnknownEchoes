package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.network.BossImpactPayload;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Boss 与 Mini Boss 的攻击演出特效工具:通用粒子组合收敛在这里,
 * 各 Boss 只在命中、蓄力与挥出的关键帧调用,避免每个攻击各写一套导致风格不一。
 */
public final class BossFx {
    private static final double TAU = Math.PI * 2;
    /** 粒子环每帧最多发送的独立广播包：采样数不随半径无限增长。 */
    private static final int MAX_RING_POINTS = 32;
    private static final int MAX_IMPACT_RING_POINTS = 18;

    private BossFx() {
    }

    /** 水主题粒子选择:BUBBLE/BUBBLE_COLUMN_UP 离水当 tick 即自毁,岸上/水面以上完全不可见。 */
    public static ParticleOptions waterBubbleOr(ServerLevel level, Vec3 pos, ParticleOptions airFallback) {
        BlockPos blockPos = BlockPos.containing(pos);
        if (level.isOutsideBuildHeight(blockPos)) {
            return airFallback;
        }
        // Cosmetic sampling must not load the next chunk crossed by a warning or wave.
        var chunk = level.getChunkSource().getChunkNow(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        return chunk != null && chunk.getFluidState(blockPos).is(FluidTags.WATER)
                ? ParticleTypes.BUBBLE_COLUMN_UP : airFallback;
    }

    public static void impactBurst(ServerLevel level, Vec3 c, ParticleOptions theme) {
        CombatFx.boss(level, Shape.BURST, c.add(0, 0.55, 0), Vec3.ZERO, 3.8, 0, 0, theme, 12);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y + 0.4, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(theme, c.x, c.y + 0.55, c.z, 8, 0.35, 0.45, 0.35, 0.08);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y + 0.75, c.z, 3, 0.25, 0.35, 0.25, 0.03);
        for (int ring = 1; ring <= 2; ring++) {
            int pts = Math.min(12 + ring * 6, MAX_IMPACT_RING_POINTS);
            double r = ring * 0.95;
            for (int i = 0; i < pts; i++) {
                double a = TAU * i / pts + ring * 0.3;
                level.sendParticles(theme, c.x + Math.cos(a) * r, c.y + 0.25, c.z + Math.sin(a) * r,
                        1, 0.0, 0.0, 0.0, 0.0);
                if (i % 6 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            c.x + Math.cos(a) * r * 0.65, c.y + 0.55 + ring * 0.08, c.z + Math.sin(a) * r * 0.65,
                            1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.65, c.z, 4, 0.45, 0.75, 0.45, 0.07);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 3, 0.55, 0.2, 0.55, 0.03);
    }

    /** 多落点攻击使用的紧凑爆发，避免为每个落点重复发送完整命中演出的粒子包。 */
    public static void areaBurst(ServerLevel level, Vec3 c, ParticleOptions theme) {
        CombatFx.boss(level, Shape.BURST, c.add(0, 0.35, 0), Vec3.ZERO, 2.7, 0, 0, theme, 10);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y + 0.35D, c.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(theme, c.x, c.y + 0.3D, c.z,
                10, 0.65D, 0.45D, 0.65D, 0.07D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.4D, c.z,
                4, 0.55D, 0.35D, 0.55D, 0.04D);
        ring(level, c, 2.7D, 12, 0.12D, theme);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.12D, c.z,
                3, 0.45D, 0.12D, 0.45D, 0.02D);
    }

    /**
     * 蓄力螺旋:绕中心上升的双臂螺旋柱,t 用 tickCount 传入。
     * 可以每 tick 调用:内部交错采样高度层,在保持 20 TPS 轮廓的同时把逐点粒子广播包减半。
     */
    public static void chargeSpiral(ServerLevel level, Vec3 c, double t, ParticleOptions theme) {
        if (level.getGameTime() % 4 == 0) {
            CombatFx.boss(level, Shape.SPIRAL, c, Vec3.ZERO, 1.35, 2.8, TAU * 2, theme, 10);
        }
        int heightParity = (int) (level.getGameTime() & 1L);
        for (int h = heightParity; h < 8; h += 2) {
            double y = c.y + 0.25 + h * 0.35;
            double r = Math.max(0.18, 1.65 - h * 0.16);
            for (int arm = 0; arm < 2; arm++) {
                double a = t * (arm == 0 ? 0.42 : -0.34) + h * 0.95 + arm * Math.PI;
                level.sendParticles(theme, c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            if ((h & 3) == heightParity) {
                level.sendParticles(ParticleTypes.GLOW, c.x, y, c.z, 1, 0.18, 0.12, 0.18, 0.02);
            }
        }
    }

    public static void sparkSpray(ServerLevel level, Vec3 origin, Vec3 dir, double length, ParticleOptions theme) {
        CombatFx.boss(level, Shape.BEAM, origin, dir, 0, length, 0, theme, 8);
        Vec3 d = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 side = Math.abs(d.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(-d.z, 0, d.x).normalize();
        int step = 0;
        for (double s = 0.6; s <= length; s += 0.7) {
            Vec3 p = origin.add(d.scale(s));
            level.sendParticles(theme, p.x, p.y, p.z, 2, 0.12, 0.12, 0.12, 0.01);
            level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.0);
            if (step % 2 == 0) {
                Vec3 left = p.add(side.scale(0.32));
                Vec3 right = p.add(side.scale(-0.32));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, left.x, left.y, left.z, 1, 0.04, 0.04, 0.04, 0.01);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, right.x, right.y, right.z, 1, 0.04, 0.04, 0.04, 0.01);
            }
            step++;
        }
    }

    public static void ring(ServerLevel level, Vec3 c, double radius, int points, double y, ParticleOptions theme) {
        int samples = Math.min(points, MAX_RING_POINTS);
        if (samples <= 0) {
            return;
        }
        CombatFx.boss(level, Shape.RING, c.add(0, y, 0), Vec3.ZERO, radius, 0, 0, theme, 8);
        for (int i = 0; i < samples; i++) {
            double a = TAU * i / samples;
            level.sendParticles(theme, c.x + Math.cos(a) * radius, c.y + y, c.z + Math.sin(a) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.GLOW,
                        c.x + Math.cos(a) * radius * 0.92, c.y + y + 0.08, c.z + Math.sin(a) * radius * 0.92,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    public static void guidanceLink(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions theme) {
        guidanceFlow(level, from, to, ParticleTypes.GLOW, theme, level.getGameTime());
    }

    /** 流动引导线:亮色骨架保证光影下可读,脉冲沿起点向终点移动以明确方向。 */
    public static void guidanceFlow(ServerLevel level, Vec3 from, Vec3 to,
                                    ParticleOptions trail, ParticleOptions pulse, long age) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, Math.min(8, (int) Math.ceil(delta.length() / 3.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double) steps));
            level.sendParticles(trail, point.x, point.y, point.z,
                    1, 0.035, 0.035, 0.035, 0.0);
        }
        for (int i = 0; i < 2; i++) {
            double progress = Math.floorMod(age * 8L + i * 50L, 100L) / 100.0D;
            Vec3 point = from.add(delta.scale(progress));
            level.sendParticles(pulse, point.x, point.y, point.z,
                    2, 0.055, 0.055, 0.055, 0.015);
        }
    }

    public static void guidanceMarker(ServerLevel level, Vec3 center, ParticleOptions theme) {
        for (int i = 0; i < 4; i++) {
            double angle = TAU * i / 4;
            level.sendParticles(theme,
                    center.x + Math.cos(angle) * 0.65, center.y + 0.12,
                    center.z + Math.sin(angle) * 0.65, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(theme, center.x, center.y + 0.7, center.z, 3, 0.03, 0.35, 0.03, 0.0);
        if (theme != ParticleTypes.GLOW) {
            level.sendParticles(ParticleTypes.GLOW, center.x, center.y + 0.45, center.z,
                    2, 0.12, 0.24, 0.12, 0.0);
        }
        if (theme != ParticleTypes.END_ROD) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.72, center.z,
                    1, 0.04, 0.16, 0.04, 0.0);
        }
    }

    /** 关键释放帧的客户端反馈：范围内玩家自行按距离衰减镜头和屏幕边缘脉冲。 */
    public static void cinematicImpact(ServerLevel level, Vec3 center, double radius,
                                       float strength, int durationTicks, int color) {
        double safeRadius = Mth.clamp(radius, 4.0D, 64.0D);
        float safeStrength = Mth.clamp(strength, 0.05F, 1.0F);
        int safeDuration = Mth.clamp(durationTicks, 2, 16);
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, safeRadius,
                new BossImpactPayload(center.x, center.y, center.z, (float) safeRadius,
                        safeStrength, safeDuration, color & 0xFFFFFF));
    }

    public static void lockSigil(ServerLevel level, Vec3 center, double radius,
                                 double progress, long age, ParticleOptions theme) {
        double charge = Math.max(0.0D, Math.min(1.0D, progress));
        double outer = radius * (1.08D - charge * 0.18D);
        CombatFx.boss(level, Shape.RING, center.add(0, 0.16, 0), Vec3.ZERO, outer, 0, 0, theme, 8);
        double inner = radius * (0.32D + charge * 0.16D);
        double phase = age * 0.16D;
        int points = Math.max(16, Math.min(28, (int) Math.ceil(radius * 3.0D)));
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points + phase;
            double y = center.y + 0.12D + ((i & 1) == 0 ? 0.04D : 0.0D);
            level.sendParticles(theme,
                    center.x + Math.cos(angle) * outer, y,
                    center.z + Math.sin(angle) * outer,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (int arm = 0; arm < 4; arm++) {
            double angle = phase * -0.45D + arm * (TAU / 4.0D);
            for (int step = 0; step < 3; step++) {
                double distance = inner + (outer - inner) * step / 3.0D;
                Vec3 point = center.add(Math.cos(angle) * distance, 0.18D,
                        Math.sin(angle) * distance);
                level.sendParticles(step == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW,
                        point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
        level.sendParticles(theme, center.x, center.y + 0.25D, center.z,
                3, 0.04D, 0.42D + charge * 0.35D, 0.04D, 0.0D);
    }

    /** 三股短尾迹标明 Boss 的实际飞行方向，不沿整条历史路径持续堆积粒子。 */
    public static void flightTrail(ServerLevel level, Vec3 center, Vec3 velocity,
                                   double spread, ParticleOptions theme) {
        if (velocity.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 backward = velocity.normalize().scale(-1.0D);
        CombatFx.boss(level, Shape.BEAM, center, backward, 0, 2.88, 0, theme, 8);
        Vec3 side = Math.abs(backward.y) > 0.92D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(-backward.z, 0.0D, backward.x).normalize();
        for (int lane = -1; lane <= 1; lane++) {
            for (int step = 1; step <= 4; step++) {
                double distance = step * 0.72D;
                double laneWidth = lane * spread * (1.0D - step * 0.1D);
                Vec3 point = center.add(backward.scale(distance)).add(side.scale(laneWidth));
                level.sendParticles(theme, point.x, point.y, point.z,
                        1, 0.04D, 0.04D, 0.04D, 0.0D);
                if (lane == 0) {
                    level.sendParticles(ParticleTypes.GLOW, point.x, point.y, point.z,
                            1, 0.03D, 0.03D, 0.03D, 0.0D);
                }
            }
        }
    }

    /** 有实体冲击的命中，碎块采样局限在附近地面；普通法术闪光不带起地板。 */
    public static void terrainImpact(ServerLevel level, Vec3 c, ParticleOptions theme) {
        CombatFx.debris(level, c, 3.4, 0.95);
        impactBurst(level, c, theme);
    }

    public static void groundShockwave(ServerLevel level, Vec3 c, double radius, ParticleOptions theme) {
        CombatFx.debris(level, c, radius, 1.15);
        CombatFx.boss(level, Shape.SHOCKWAVE, c.add(0, 0.16, 0), Vec3.ZERO,
                radius, 0, 0, theme, 12);
        ring(level, c, radius, Math.max(16, (int) (radius * 8)), 0.12, theme);
        level.sendParticles(theme, c.x, c.y + 0.35, c.z, 12, radius * 0.18, 0.15, radius * 0.18, 0.08);
    }

    /** 方向性扇形刃光:用于近战横扫,只画表现线,不做判定。 */
    public static void sweepArc(ServerLevel level, Vec3 origin, Vec3 facing, double range, ParticleOptions theme) {
        CombatFx.boss(level, Shape.SLASH, origin.add(0, 0.55, 0), facing, range, 0, 0.72, theme, 10);
        Vec3 dir = facing.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : facing.normalize();
        double base = Math.atan2(dir.z, dir.x);
        for (int arm = -4; arm <= 4; arm++) {
            double angle = base + arm * 0.18;
            level.sendParticles(theme,
                    origin.x + Math.cos(angle) * range, origin.y + 0.55 + range * 0.04,
                    origin.z + Math.sin(angle) * range, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    /**
     * 吸聚环:圆周上的粒子带着朝中心的初速飞入,表现能量被拽向 Boss 的蓄力阶段。
     * 靠 sendParticles count=0 时偏移量当速度向量实现;适合每 2-4 tick 调用一次。
     */
    public static void converge(ServerLevel level, Vec3 c, double radius, int points, ParticleOptions theme) {
        if (level.getGameTime() % 4 == 0) {
            CombatFx.boss(level, Shape.IMPLOSION, c, Vec3.ZERO, radius, radius * 2, 0, theme, 10);
        }
        points = Math.min(points, 24);
        if (points <= 0) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points + radius * 0.7;
            double px = c.x + Math.cos(a) * radius;
            double pz = c.z + Math.sin(a) * radius;
            level.sendParticles(theme, px, c.y, pz,
                    0, -Math.cos(a), 0.02, -Math.sin(a), 0.22);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.GLOW, px, c.y + 0.35, pz,
                        0, -Math.cos(a), 0.08, -Math.sin(a), 0.14);
            }
        }
    }

    public static void helix(ServerLevel level, Vec3 from, Vec3 to, double radius, ParticleOptions theme) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 d = delta.scale(1.0 / length);
        CombatFx.boss(level, Shape.HELIX, from, d, radius, length, 0, theme, 10);
        Vec3 side = Math.abs(d.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(-d.z, 0, d.x).normalize();
        Vec3 up = d.cross(side);
        int samples = Math.max(4, Math.min(28, (int) Math.ceil(length / 0.7D)));
        for (int sample = 0; sample <= samples; sample++) {
            double s = length * sample / samples;
            double a = s * 1.35;
            Vec3 axis = from.add(d.scale(s));
            for (int arm = 0; arm < 2; arm++) {
                double phase = a + arm * Math.PI;
                Vec3 p = axis.add(side.scale(Math.cos(phase) * radius)).add(up.scale(Math.sin(phase) * radius));
                level.sendParticles(theme, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (sample % 3 == 0) {
                level.sendParticles(ParticleTypes.GLOW, axis.x, axis.y, axis.z, 1, 0.04, 0.04, 0.04, 0.0);
            }
        }
    }

    public static void pillar(ServerLevel level, Vec3 c, double height, ParticleOptions theme) {
        CombatFx.boss(level, Shape.COLUMN, c, Vec3.ZERO, 0.55, height, TAU * 2, theme, 12);
        for (double h = 0.2; h <= height; h += 0.6) {
            double r = 0.55 * (1.0 - h / (height * 1.6));
            level.sendParticles(theme, c.x, c.y + h, c.z, 2, r, 0.1, r, 0.02);
            if (((int) (h / 0.6)) % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + h, c.z, 1, 0.12, 0.05, 0.12, 0.01);
            }
        }
        level.sendParticles(theme, c.x, c.y + 0.3, c.z, 0, 0.0, 1.0, 0.0, 0.55);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y + height * 0.7, c.z, 1, 0, 0, 0, 0);
    }

    /**
     * 坠星轨迹:从高空指向落点的倾斜彗尾。progress 取 [0,1] 表示坠落进度,
     * 预警阶段按帧推进它就能读成"正在坠落"。
     */
    public static void comet(ServerLevel level, Vec3 impact, double height, double progress, ParticleOptions theme) {
        Vec3 start = impact.add(height * 0.45, height, height * 0.3);
        Vec3 head = start.add(impact.subtract(start).scale(Math.min(1.0, Math.max(0.0, progress))));
        Vec3 tail = start.subtract(head);
        CombatFx.boss(level, Shape.BEAM, head, tail, 0, Math.min(3.2, tail.length()), 0, theme, 6);
        level.sendParticles(theme, head.x, head.y, head.z, 4, 0.22, 0.22, 0.22, 0.04);
        level.sendParticles(ParticleTypes.FLAME, head.x, head.y, head.z, 2, 0.14, 0.14, 0.14, 0.02);
        Vec3 tailDir = start.subtract(head);
        double tailLength = Math.min(3.2, tailDir.length());
        if (tailLength > 0.2) {
            Vec3 t = tailDir.normalize();
            for (double s = 0.5; s <= tailLength; s += 0.6) {
                Vec3 p = head.add(t.scale(s));
                level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.0);
            }
        }
    }

    /** 环绕旋涡:围绕中心旋转上升的粒子筒(风暴/火旋),t 传 tickCount 驱动旋转。 */
    public static void vortex(ServerLevel level, Vec3 c, double radius, double height, double t, ParticleOptions theme) {
        if (level.getGameTime() % 4 == 0) {
            CombatFx.boss(level, Shape.VORTEX, c, Vec3.ZERO, radius, height, TAU * 2, theme, 10);
        }
        for (int layer = 0; layer < 5; layer++) {
            double y = c.y + 0.3 + layer * (height / 5.0);
            double r = radius * (0.7 + 0.3 * Math.sin(t * 0.2 + layer));
            for (int arm = 0; arm < 3; arm++) {
                double a = t * 0.38 + layer * 0.8 + arm * (TAU / 3.0);
                double px = c.x + Math.cos(a) * r;
                double pz = c.z + Math.sin(a) * r;
                level.sendParticles(theme, px, y, pz,
                        0, -Math.sin(a), 0.06, Math.cos(a), 0.18);
            }
        }
    }
}
