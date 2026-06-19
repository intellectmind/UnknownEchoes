package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Boss / Mini Boss 攻击演出特效工具:把"更炫"的通用粒子组合收敛到一处,
 * 各 Boss 在攻击命中/蓄力/挥出的关键帧调用即可,避免每个攻击各写一套且风格不一。
 *
 * 纯表现层(只发粒子,不做伤害/判定),全部 server 端 {@code sendParticles}。
 * theme 参数传入对应 Boss 的主题粒子(END_ROD / SONIC_BOOM / SOUL_FIRE_FLAME / ...)上色。
 */
public final class BossFx {
    private static final double TAU = Math.PI * 2;

    private BossFx() {
    }

    /** 命中迸发:中心闪光 + 多层扩散光环 + 主题火花上冲。攻击落地/命中时调用。 */
    public static void impactBurst(ServerLevel level, Vec3 c, ParticleOptions theme) {
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y + 0.4, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(theme, c.x, c.y + 0.55, c.z, 20, 0.35, 0.45, 0.35, 0.16);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y + 0.75, c.z, 10, 0.25, 0.35, 0.25, 0.06);
        for (int ring = 1; ring <= 4; ring++) {
            int pts = 12 + ring * 6;
            double r = ring * 0.95;
            for (int i = 0; i < pts; i++) {
                double a = TAU * i / pts + ring * 0.3;
                level.sendParticles(theme, c.x + Math.cos(a) * r, c.y + 0.25, c.z + Math.sin(a) * r,
                        1, 0.0, 0.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            c.x + Math.cos(a) * r * 0.65, c.y + 0.55 + ring * 0.08, c.z + Math.sin(a) * r * 0.65,
                            1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.65, c.z, 28, 0.45, 0.75, 0.45, 0.18);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 12, 0.55, 0.2, 0.55, 0.05);
    }

    /** 蓄力螺旋:绕中心上升的螺旋柱,蓄力/施法每 tick 调用形成聚拢感。t 用 tickCount 传入。 */
    public static void chargeSpiral(ServerLevel level, Vec3 c, double t, ParticleOptions theme) {
        for (int h = 0; h < 8; h++) {
            double y = c.y + 0.25 + h * 0.35;
            double r = Math.max(0.18, 1.65 - h * 0.16);
            for (int arm = 0; arm < 2; arm++) {
                double a = t * (arm == 0 ? 0.42 : -0.34) + h * 0.95 + arm * Math.PI;
                level.sendParticles(theme, c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            if (h % 2 == 0) {
                level.sendParticles(ParticleTypes.GLOW, c.x, y, c.z, 1, 0.18, 0.12, 0.18, 0.02);
            }
        }
    }

    /** 定向火花扇:沿攻击方向铺一条发亮粒子轨迹(挥击/光束的"刃光")。 */
    public static void sparkSpray(ServerLevel level, Vec3 origin, Vec3 dir, double length, ParticleOptions theme) {
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

    /** 扩散冲击环:单层、可指定半径与高度的水平光环(配合 AoE 的边缘提示)。 */
    public static void ring(ServerLevel level, Vec3 c, double radius, int points, double y, ParticleOptions theme) {
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points;
            level.sendParticles(theme, c.x + Math.cos(a) * radius, c.y + y, c.z + Math.sin(a) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.GLOW,
                        c.x + Math.cos(a) * radius * 0.92, c.y + y + 0.08, c.z + Math.sin(a) * radius * 0.92,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    /** 地面冲击波:用于踏地/爆环/落地,提供读得清的外圈边缘与中心尘浪。 */
    public static void groundShockwave(ServerLevel level, Vec3 c, double radius, ParticleOptions theme) {
        ring(level, c, radius, Math.max(16, (int) (radius * 8)), 0.12, theme);
        ring(level, c, radius * 0.65, Math.max(12, (int) (radius * 6)), 0.28, ParticleTypes.CLOUD);
        level.sendParticles(ParticleTypes.GUST, c.x, c.y + 0.25, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(theme, c.x, c.y + 0.35, c.z, 12, radius * 0.18, 0.15, radius * 0.18, 0.08);
    }

    /** 方向性扇形刃光:用于近战横扫,只画表现线,不做判定。 */
    public static void sweepArc(ServerLevel level, Vec3 origin, Vec3 facing, double range, ParticleOptions theme) {
        Vec3 dir = facing.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : facing.normalize();
        double base = Math.atan2(dir.z, dir.x);
        for (int arm = -4; arm <= 4; arm++) {
            double angle = base + arm * 0.18;
            for (double d = 1.2; d <= range; d += 0.9) {
                level.sendParticles(theme,
                        origin.x + Math.cos(angle) * d, origin.y + 0.55 + d * 0.04, origin.z + Math.sin(angle) * d,
                        1, 0.04, 0.04, 0.04, 0.0);
            }
        }
    }
}
