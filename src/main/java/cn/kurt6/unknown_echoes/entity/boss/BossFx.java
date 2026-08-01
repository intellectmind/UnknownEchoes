package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
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

    /**
     * 水主题粒子选择:BUBBLE/BUBBLE_COLUMN_UP 离水当 tick 即自毁,岸上/水面以上完全不可见。
     * 位置在水中返回气泡(水下观感最好),否则回退到任意环境可渲染的水花。
     */
    public static ParticleOptions waterBubbleOr(ServerLevel level, Vec3 pos, ParticleOptions airFallback) {
        return level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)
                ? ParticleTypes.BUBBLE_COLUMN_UP : airFallback;
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

    /** 多落点攻击使用的紧凑爆发，避免为每个落点重复发送完整命中演出的粒子包。 */
    public static void areaBurst(ServerLevel level, Vec3 c, ParticleOptions theme) {
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y + 0.35D, c.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(theme, c.x, c.y + 0.3D, c.z,
                24, 0.65D, 0.45D, 0.65D, 0.14D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.4D, c.z,
                10, 0.55D, 0.35D, 0.55D, 0.08D);
        ring(level, c, 2.7D, 12, 0.12D, theme);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.12D, c.z,
                8, 0.45D, 0.12D, 0.45D, 0.04D);
    }

    /**
     * 蓄力螺旋:绕中心上升的双臂螺旋柱。调用方可每 tick 调用，内部交错采样高度层，
     * 在保持 20 TPS 动态轮廓的同时把逐点粒子广播包减半。t 用 tickCount 传入。
     */
    public static void chargeSpiral(ServerLevel level, Vec3 c, double t, ParticleOptions theme) {
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

    public static void guidanceLink(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions theme) {
        guidanceFlow(level, from, to, ParticleTypes.GLOW, theme, level.getGameTime());
    }

    /**
     * 流动引导线:亮色骨架保证光影下可读,脉冲沿起点向终点移动以明确方向。
     * 适合每 5 tick 调用;age 传实体 tickCount 或世界时间,避免额外保存动画状态。
     */
    public static void guidanceFlow(ServerLevel level, Vec3 from, Vec3 to,
                                    ParticleOptions trail, ParticleOptions pulse, long age) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, Math.min(10, (int) Math.ceil(delta.length() / 2.6D)));
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

    /**
     * 吸聚环:圆周上的粒子带着朝中心的初速飞入(sendParticles count=0 时偏移量作为速度向量),
     * 表现"能量被拽向 Boss"的蓄力阶段。适合每 2-4 tick 调用一次。
     */
    public static void converge(ServerLevel level, Vec3 c, double radius, int points, ParticleOptions theme) {
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

    /** 双螺旋:沿 from→to 缠绕两条主题粒子链,骨架加 GLOW,用于光束/长矛的"能量缠绕"。 */
    public static void helix(ServerLevel level, Vec3 from, Vec3 to, double radius, ParticleOptions theme) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 d = delta.scale(1.0 / length);
        Vec3 side = Math.abs(d.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(-d.z, 0, d.x).normalize();
        Vec3 up = d.cross(side);
        for (double s = 0.0; s <= length; s += 0.55) {
            double a = s * 1.35;
            Vec3 axis = from.add(d.scale(s));
            for (int arm = 0; arm < 2; arm++) {
                double phase = a + arm * Math.PI;
                Vec3 p = axis.add(side.scale(Math.cos(phase) * radius)).add(up.scale(Math.sin(phase) * radius));
                level.sendParticles(theme, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (((int) (s / 0.55)) % 3 == 0) {
                level.sendParticles(ParticleTypes.GLOW, axis.x, axis.y, axis.z, 1, 0.04, 0.04, 0.04, 0.0);
            }
        }
    }

    /** 冲天光柱:落点/释放点向上喷发的能量柱,顶端加闪光,用于砸击、相变与新星。 */
    public static void pillar(ServerLevel level, Vec3 c, double height, ParticleOptions theme) {
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
     * 坠星轨迹:从高空指向落点的倾斜彗尾,progress ∈ [0,1] 表示当前坠落进度,
     * 预警阶段按帧推进 progress 即可获得"正在坠落"的读法。
     */
    public static void comet(ServerLevel level, Vec3 impact, double height, double progress, ParticleOptions theme) {
        Vec3 start = impact.add(height * 0.45, height, height * 0.3);
        Vec3 head = start.add(impact.subtract(start).scale(Math.min(1.0, Math.max(0.0, progress))));
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
