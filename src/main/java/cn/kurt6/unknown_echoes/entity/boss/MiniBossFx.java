package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mini Boss 演出形状库：半径技画环、直线技画束、落点技画落区、单体命中画标记。
 * 形状必须与判定几何同构，不一致时玩家会照着特效站位，然后被没有特效的那一侧判定命中。
 * Mini Boss 演出只改这里，不要改 BossFx：那边被六只主线 Boss 与十只 Mini Boss 共用。
 */
public final class MiniBossFx {
    private static final double TAU = Math.PI * 2.0D;

    /** 单次演出的采样上限。粒子是纯表现，预算不能随判定半径或目标数无限增长。 */
    private static final int MAX_RING_POINTS = 28;
    private static final int MAX_BEAM_POINTS = 26;
    private static final int MAX_ZONE_POINTS = 18;
    /** 引导线的步数上限。多目标机制会对每个目标各画一条，步数按目标数乘上去。 */
    private static final int MAX_LINK_POINTS = 8;
    /** 锥形喷射的分臂数上限。总采样是 arms x steps，两者都要钳。 */
    private static final int MAX_CONE_ARMS = 5;

    /** 超过这个碰撞箱宽度就按"宽体"处理：中轴与贴面的粒子会被模型遮住。 */
    private static final float WIDE_TARGET_WIDTH = 1.6F;

    /** 单体标记的固定采样数。不要改成按目标数放大：群体技的包数会翻倍。 */
    private static final int MARK_POINTS = 8;

    private MiniBossFx() {
    }

    /** 定点投放：count=1 且偏移与速度全 0 时客户端的高斯采样恒为 0，落点即给定坐标。 */
    private static void at(ServerLevel level, ParticleOptions particle, Vec3 point) {
        level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * 地面冲击环：贴地画环，给半径判定用。radius 直接传判定半径，让玩家看到边界在哪。
     *
     * y 取脚底而不是腰高：判定是以实体为中心的盒，但贴地环才能让玩家读出水平边界。
     */
    public static void groundRing(ServerLevel level, Vec3 center,
                                  MiniBossFxTheme.Theme theme, double radius) {
        CombatFx.boss(level, Shape.RING, center.add(0, 0.18, 0), Vec3.ZERO, radius, 0, 0,
                theme.coreDust(1), 12);
        int points = Math.min(MAX_RING_POINTS, 12 + (int) (radius * 2.0D));
        ParticleOptions trace = theme.trace(1.6F);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            at(level, trace, center.add(cos * radius, 0.18D, sin * radius));
            if (i % 3 == 0) {
                at(level, theme.coreDust(1.1F),
                        center.add(cos * radius * 0.55D, 0.32D, sin * radius * 0.55D));
            }
            if (i % 4 == 0) {
                at(level, theme.residue(),
                        center.add(cos * radius * 0.85D, 0.1D, sin * radius * 0.85D));
            }
        }
    }

    /** 瞬发范围技的释放波。固定外环保留结算边界，内侧双波只负责扩散余势。 */
    public static void releasePulse(ServerLevel level, Vec3 center,
                                    MiniBossFxTheme.Theme theme, double radius) {
        groundRing(level, center, theme, radius);
        CombatFx.emit(level, Shape.SHOCKWAVE, center.add(0, 0.2, 0), Vec3.ZERO, radius, 0, 0,
                CombatFx.color(theme.core()), CombatFx.color(theme.edge()), 12, true);
    }

    /** 真实重击落地时掀起客户端材质碎块；蓄力、预警和机制目标不得调用。 */
    public static void groundImpact(ServerLevel level, Vec3 center,
                                    MiniBossFxTheme.Theme theme, double radius, double strength) {
        releasePulse(level, center, theme, radius);
        CombatFx.debris(level, center, radius, strength);
    }

    /** 持续法阵刚激活时的一次收束，不推进领域半径或重复扮演落点预警。 */
    public static void sealRelease(ServerLevel level, Vec3 center,
                                   MiniBossFxTheme.Theme theme, double radius) {
        CombatFx.emit(level, Shape.SIGIL, center.add(0, .16, 0), Vec3.ZERO, radius, 0, 0,
                CombatFx.color(theme.core()), CombatFx.color(theme.edge()), 14, true);
        CombatFx.emit(level, Shape.IMPLOSION, center.add(0, 0.6, 0), Vec3.ZERO,
                radius, 1.8, 0, CombatFx.color(theme.core()), CombatFx.color(theme.edge()), 12, true);
    }

    /**
     * 扩张环：给逐帧长大的半径判定用（静默领域的声波、潮涌）。
     * progress 取 [0,1]，越接近 1 环越淡——判定已经扫过的位置不该继续亮着。
     */
    public static void expandingRing(ServerLevel level, Vec3 center,
                                     MiniBossFxTheme.Theme theme,
                                     double radius, double progress) {
        CombatFx.boss(level, Shape.RING, center.add(0, 0.2, 0), Vec3.ZERO, radius, 0, 0,
                theme.coreDust(1), 4);
        double phase = Math.max(0.0D, Math.min(1.0D, progress));
        int points = Math.min(MAX_RING_POINTS, 12 + (int) (radius * 2.0D));
        ParticleOptions edge = phase > 0.6D ? theme.fade(1.2F) : theme.trace(1.5F);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points;
            at(level, edge, center.add(Math.cos(angle) * radius, 0.2D,
                    Math.sin(angle) * radius));
        }
    }

    /**
     * 贯穿光束：沿方向铺一条有侧翼的束，给直线判定用。
     *
     * length 必须是调用点已经截断过的可达距离，否则光束会穿墙而判定不会。
     */
    public static void pierceBeam(ServerLevel level, Vec3 origin, Vec3 direction,
                                  MiniBossFxTheme.Theme theme, double length) {
        Vec3 dir = direction.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        Vec3 side = Math.abs(dir.y) > 0.92D
                ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-dir.z, 0.0D, dir.x).normalize();
        int steps = Math.min(MAX_BEAM_POINTS, Math.max(3, (int) (length / 0.8D)));
        CombatFx.boss(level, Shape.BEAM, origin, dir, 0, length, 0, theme.coreDust(1), 10);
        ParticleOptions trace = theme.trace(1.3F);
        for (int i = 0; i < steps; i++) {
            Vec3 point = origin.add(dir.scale(length * (i + 0.5D) / steps));
            at(level, trace, point);
            // 侧翼随距离收窄：等宽的束读成一根管子，收窄才有"从这里射出去"的方向感。
            double spread = 0.34D * (1.0D - (double) i / steps) + 0.06D;
            if (i % 2 == 0) {
                at(level, theme.edgeDust(0.9F), point.add(side.scale(spread)));
                at(level, theme.edgeDust(0.9F), point.add(side.scale(-spread)));
            }
            if (i % 4 == 0) {
                at(level, theme.glint(), point);
            }
        }
    }

    /**
     * 落点预警区：画出即将结算的水平半径，给 AABB 落点判定用（花苞、纸页、齿轮弹）。
     *
     * progress 取 [0,1] 表示预警推进；环随进度收紧，读成"还有多久落下"。
     * radius 必须传结算用的那个半径，不要传一个"看起来差不多"的值。
     */
    public static void impactZone(ServerLevel level, Vec3 center,
                                  MiniBossFxTheme.Theme theme,
                                  double radius, double progress) {
        CombatFx.boss(level, Shape.RING, center.add(0, 0.14, 0), Vec3.ZERO, radius, 0, 0,
                theme.coreDust(1), 6);
        double phase = Math.max(0.0D, Math.min(1.0D, progress));
        int points = Math.min(MAX_ZONE_POINTS, 8 + (int) (radius * 3.0D));
        ParticleOptions trace = theme.trace(1.4F);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points + phase * 1.6D;
            at(level, trace, center.add(Math.cos(angle) * radius, 0.14D,
                    Math.sin(angle) * radius));
        }
        // 中心竖柱随进度长高：只有环时玩家读不出"这里要落东西"还是"这里有个圈"。
        int column = 1 + (int) (phase * 3.0D);
        for (int i = 0; i < column; i++) {
            at(level, theme.coreDust(1.2F), center.add(0.0D, 0.25D + i * 0.4D, 0.0D));
        }
    }

    /**
     * 锥形喷射。halfAngle 必须等于 acos(判定层 minDot)。
     * @param halfAngle acos(判定层 minDot)，禁止"看起来差不多"
     */
    public static void coneSpray(ServerLevel level, Vec3 origin, Vec3 facing,
                                 MiniBossFxTheme.Theme theme,
                                 double range, double halfAngle) {
        Vec3 dir = facing.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, 1.0D) : facing.normalize();
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        // halfAngle 钳到正值：minDot=1.0 的锥退化成射线时它是 0，除法会得到 NaN 坐标。
        double span = Math.max(1.0E-3D, halfAngle);
        CombatFx.boss(level, Shape.CONE, origin, dir, range, 0, span, theme.coreDust(1), 10);
        int arms = Math.min(MAX_CONE_ARMS, 5);
        int steps = Math.min(MAX_BEAM_POINTS / arms, Math.max(2, (int) (range / 1.2D)));
        ParticleOptions trace = theme.trace(1.3F);
        for (int arm = 0; arm < arms; arm++) {
            double angle = -span + 2.0D * span * arm / (arms - 1);
            Vec3 lane = dir.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            for (int i = 0; i < steps; i++) {
                double distance = range * (i + 1) / (double) steps;
                Vec3 point = origin.add(lane.scale(distance));
                at(level, arm == arms / 2 ? trace : theme.edgeDust(1.0F), point);
            }
        }
        at(level, theme.glint(), origin.add(dir.scale(0.8D)));
    }

    /**
     * 单体命中标记：目标腰高一圈收束残辉。每个命中目标调用一次，采样固定 MARK_POINTS ——
     * 目标数由调用点的判定决定，这里再按目标数放大会让群体技的包数翻倍。
     */
    public static void hitMark(ServerLevel level, LivingEntity target,
                               MiniBossFxTheme.Theme theme) {
        Vec3 waist = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        double radius = markRadiusOf(target);
        ParticleOptions fade = theme.fade(1.4F);
        CombatFx.boss(level, Shape.IMPACT, waist, Vec3.ZERO, radius, 0, 0, theme.coreDust(1), 8);
        for (int i = 0; i < MARK_POINTS; i++) {
            double angle = TAU * i / MARK_POINTS;
            at(level, fade, waist.add(Math.cos(angle) * radius, 0.0D,
                    Math.sin(angle) * radius));
        }
        // 中心点只给窄目标：宽体身上它在模型内部，发了也看不见。
        if (target.getBbWidth() <= WIDE_TARGET_WIDTH) {
            at(level, theme.glint(), waist);
        }
    }

    /**
     * 命中环的半径。余量必须与碰撞箱宽度成比例，不能改成固定常数：
     * 宽度 4 格以上的目标只留固定 0.25 格时，环会贴在模型表面被整圈遮住。
     */
    private static double markRadiusOf(LivingEntity target) {
        double half = target.getBbWidth() * 0.5D;
        return half + Math.max(0.25D, half * 0.35D);
    }

    /**
     * 蓄力汇聚：粒子从环上带着朝心速度飞入，表现"能量正被拽向这里"。
     * 与 groundRing 分开是因为蓄力没有判定边界可画，画实心环会被读成攻击范围。
     */
    public static void chargeConverge(ServerLevel level, Vec3 center,
                                      MiniBossFxTheme.Theme theme,
                                      double radius, double progress) {
        if (level.getGameTime() % 4 == 0) {
            CombatFx.boss(level, Shape.IMPLOSION, center.add(0, 0.4, 0), Vec3.ZERO, radius, radius * 2, 0,
                    theme.coreDust(1), 8);
        }
        double phase = Math.max(0.0D, Math.min(1.0D, progress));
        int points = Math.min(MAX_ZONE_POINTS, 10 + (int) (radius * 2.0D));
        ParticleOptions trace = theme.trace(1.2F);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points + phase * 2.4D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            // count=0 时偏移量当速度向量用，粒子自己朝中心飞。
            level.sendParticles(trace, center.x + cos * radius, center.y + 0.4D,
                    center.z + sin * radius, 0, -cos, 0.04D, -sin, 0.22D);
        }
        at(level, theme.glint(), center.add(0.0D, 0.6D + phase * 0.6D, 0.0D));
    }

    /**
     * 破防窗口标记：绕实体一圈上升的残辉，告诉玩家"现在能打进去"。
     * 只在未破防保护解除时调用；与 chargeConverge 方向相反（向外散而不是向内聚）。
     */
    public static void breachAura(ServerLevel level, LivingEntity self,
                                 MiniBossFxTheme.Theme theme, long age) {
        double radius = self.getBbWidth() * 0.6D + 0.35D;
        double height = self.getBbHeight();
        CombatFx.boss(level, Shape.SPIRAL, self.position(), Vec3.ZERO, radius, height, TAU,
                theme.coreDust(1), 8);
        ParticleOptions fade = theme.fade(1.3F);
        for (int arm = 0; arm < 2; arm++) {
            double angle = age * 0.28D + arm * Math.PI;
            double rise = (age % 20L) / 20.0D * height;
            // 两条臂相位差 π：单臂绕到背面就整段消失，双臂任何视角都至少有一条可见。
            at(level, fade, self.position().add(Math.cos(angle) * radius, rise,
                    Math.sin(angle) * radius));
        }
    }

    /**
     * 机制目标标记：给玩家要交互的那个方块（音柱、浮标、符文座、烛台）打一圈可读的框。
     * 与 impactZone 分开是因为它标的是"你要去这里"而不是"这里要落东西"，
     * 用收束残辉而不是描形渐变，避免读成攻击预警。
     */
    public static void objectiveMarker(ServerLevel level, Vec3 center,
                                      MiniBossFxTheme.Theme theme, long age) {
        double radius = 0.62D;
        ParticleOptions fade = theme.fade(1.2F);
        for (int i = 0; i < 4; i++) {
            double angle = TAU * i / 4.0D + age * 0.06D;
            at(level, fade, center.add(Math.cos(angle) * radius, 0.12D,
                    Math.sin(angle) * radius));
        }
        at(level, theme.glint(), center.add(0.0D, 0.55D, 0.0D));
    }

    /**
     * 机制引导线：从目标方块流向 Mini Boss，脉冲沿这个方向移动以明确因果。
     * 采样上限必须钳住：引导线会同时对多个目标画，步数是按目标数乘上去的。
     */
    public static void objectiveLink(ServerLevel level, Vec3 from, Vec3 to,
                                     MiniBossFxTheme.Theme theme, long age) {
        Vec3 span = to.subtract(from);
        int steps = Math.max(3, Math.min(MAX_LINK_POINTS,
                (int) Math.ceil(span.length() / 2.4D)));
        ParticleOptions trace = theme.trace(1.0F);
        for (int i = 0; i <= steps; i++) {
            at(level, trace, from.add(span.scale(i / (double) steps)));
        }
        double phase = Math.floorMod(age * 6L, 100L) / 100.0D;
        at(level, theme.coreDust(1.3F), from.add(span.scale(phase)));
    }

    /**
     * 竖直光柱：从方块顶面往上一条柱，用于"这一根现在亮了"的节奏演示与输入回馈。
     *
     * height 是格数；采样步距固定 0.18 格，超过 MAX_BEAM_POINTS 就截断——
     * 柱高由调用点的演示语义决定，不该让它随场地尺寸无限增长。
     */
    public static void pillarBeam(ServerLevel level, Vec3 base,
                                  MiniBossFxTheme.Theme theme, double height) {
        int steps = Math.min(MAX_BEAM_POINTS, Math.max(2, (int) (height / 0.18D)));
        CombatFx.boss(level, Shape.BEAM, base, new Vec3(0, 1, 0), 0, steps * 0.18, 0,
                theme.coreDust(1), 12);
        ParticleOptions trace = theme.trace(1.4F);
        for (int i = 0; i < steps; i++) {
            at(level, trace, base.add(0.0D, i * 0.18D, 0.0D));
        }
        at(level, theme.glint(), base.add(0.0D, steps * 0.18D, 0.0D));
    }

    /**
     * 已完成标记：比 pillarBeam 轻一档的常驻残辉，表示"这一步已经答对了"。
     * 用 fade 而不是 trace：还没答的步骤才该用描形渐变，两者混用玩家分不出进度。
     */
    public static void solvedMark(ServerLevel level, Vec3 center,
                                  MiniBossFxTheme.Theme theme) {
        at(level, theme.fade(1.1F), center);
        at(level, theme.edgeDust(0.9F), center.add(0.0D, 0.22D, 0.0D));
    }
}
