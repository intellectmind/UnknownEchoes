package cn.kurt6.unknown_echoes.artifact;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 神器演出形状库：自身增益画螺旋或穹顶、指向技画束、半径技画环、传送画光柱。
 * 形状必须与作用几何同构，不一致时玩家会照着特效判断范围，然后在没有特效的一侧被结算。
 * 神器演出只改这里，不要改 `BossFx`：那边被全部 Boss 与 Mini Boss 共用。
 */
final class ArtifactFx {
    private static final double TAU = Math.PI * 2.0D;

    /** 单次演出的采样上限。粒子是纯表现，预算不能随作用半径或目标数无限增长。 */
    private static final int MAX_RING_POINTS = 30;
    private static final int MAX_SPIRAL_STEPS = 26;
    private static final int MAX_BEAM_POINTS = 20;
    private static final int MAX_COLUMN_POINTS = 14;

    /** 超过这个碰撞箱宽度就按“宽体”处理：中轴的粒子会被模型遮住。 */
    private static final float WIDE_TARGET_WIDTH = 1.6F;

    /**
     * 逐目标标记的环采样数。
     *
     * 不要往上调：群体冻结单次可标记 32 个目标，点数是按目标数乘上去的，
     * 取 10 时最坏一次就要发近 400 个广播包。6 点已经足够读出“这个被标记了”。
     */
    private static final int MARK_RING_POINTS = 6;

    private ArtifactFx() {
    }

    /** 定点投放：count=1 且偏移与速度全 0 时客户端的高斯采样恒为 0，落点即给定坐标。 */
    private static void at(ServerLevel level, ParticleOptions particle, Vec3 p) {
        level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** 起飞升柱：双臂螺旋向上。增益技不画环 —— 环会被读成“这里有作用范围”。 */
    static void liftSpiral(ServerLevel level, ServerPlayer player,
                           ArtifactFxTheme.Theme theme, double radius, int turns) {
        int steps = Math.min(MAX_SPIRAL_STEPS, 14 + Math.max(0, turns) * 3);
        int arcs = Math.max(1, turns);
        double height = player.getBbHeight() + 0.9D;
        Vec3 feet = player.position();
        ParticleOptions trace = theme.trace(1.3F);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double a = TAU * arcs * t;
            double y = height * t;
            // 两条臂相位差 π：单臂螺旋绕到背面就整段消失，双臂任何视角都至少有一条可见。
            at(level, trace, feet.add(Math.cos(a) * radius, y, Math.sin(a) * radius));
            at(level, theme.edgeDust(1.0F),
                    feet.add(Math.cos(a + Math.PI) * radius, y, Math.sin(a + Math.PI) * radius));
            if (i % 4 == 0) {
                at(level, theme.glint(), feet.add(0.0D, y + 0.25D, 0.0D));
            }
        }
    }

    /**
     * 治疗穹顶：贴身的半球壳。回复类神器没有作用边界可画，
     * 画贴地环会被读成“站进来就能回血”，而实际只作用于持有者自己。
     */
    static void mendDome(ServerLevel level, ServerPlayer player,
                         ArtifactFxTheme.Theme theme, double radius) {
        Vec3 feet = player.position();
        int rings = 3;
        ParticleOptions trace = theme.trace(1.4F);
        for (int ring = 0; ring < rings; ring++) {
            // 由下到上收窄成壳：等宽的三层环读成三个独立的环而不是一个罩子。
            double t = (ring + 0.5D) / rings;
            double y = player.getBbHeight() * t;
            double r = radius * Math.cos(t * Math.PI * 0.42D);
            int points = Math.min(MAX_RING_POINTS, 8 + (int) (r * 6.0D));
            for (int i = 0; i < points; i++) {
                double a = TAU * i / points + ring * 0.35D;
                at(level, trace, feet.add(Math.cos(a) * r, y, Math.sin(a) * r));
                if (i % 3 == 0) {
                    at(level, theme.residue(), feet.add(Math.cos(a) * r * 0.6D, y, Math.sin(a) * r * 0.6D));
                }
            }
        }
        at(level, theme.glint(), feet.add(0.0D, player.getBbHeight() * 0.6D, 0.0D));
    }

    /**
     * 作用域环：贴地画环，给半径作用用。radius 直接传作用半径，让玩家看到边界在哪。
     *
     * y 取脚底而不是腰高：作用范围是以玩家为中心的柱体，贴地环才能读出水平边界。
     */
    static void fieldRing(ServerLevel level, ServerPlayer player,
                          ArtifactFxTheme.Theme theme, double radius) {
        Vec3 feet = player.position();
        int points = Math.min(MAX_RING_POINTS, 12 + (int) (radius * 1.6D));
        ParticleOptions trace = theme.trace(1.7F);
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            at(level, trace, feet.add(cos * radius, 0.18D, sin * radius));
            if (i % 3 == 0) {
                at(level, theme.coreDust(1.2F), feet.add(cos * radius * 0.55D, 0.34D, sin * radius * 0.55D));
            }
            if (i % 4 == 0) {
                at(level, theme.residue(), feet.add(cos * radius * 0.85D, 0.1D, sin * radius * 0.85D));
            }
        }
    }

    /**
     * 指向束：沿给定方向铺一条收窄的束，给指路与标记视线用。
     *
     * length 必须是调用点已经截断过的可达距离，否则束会画进墙里而作用不会。
     */
    static void sightBeam(ServerLevel level, ArtifactFxTheme.Theme theme,
                          Vec3 origin, Vec3 direction, double length) {
        Vec3 dir = direction.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        Vec3 side = Math.abs(dir.y) > 0.92D
                ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-dir.z, 0.0D, dir.x).normalize();
        int steps = Math.min(MAX_BEAM_POINTS, Math.max(3, (int) (length / 0.9D)));
        ParticleOptions trace = theme.trace(1.2F);
        for (int i = 0; i < steps; i++) {
            Vec3 p = origin.add(dir.scale(length * (i + 0.5D) / steps));
            at(level, trace, p);
            // 侧翼随距离收窄：等宽的束读成一根管子，收窄才有“从手里指出去”的方向感。
            double spread = 0.3D * (1.0D - (double) i / steps) + 0.05D;
            if (i % 3 == 0) {
                at(level, theme.edgeDust(0.9F), p.add(side.scale(spread)));
                at(level, theme.edgeDust(0.9F), p.add(side.scale(-spread)));
            }
            if (i % 5 == 0) {
                at(level, theme.glint(), p);
            }
        }
    }

    /**
     * 逐目标标记：目标腰高一圈收束残辉。每个目标调用一次，所以采样固定 10 点 ——
     * 目标数由调用点的上限决定，这里再按目标数放大会让群体技的包数翻倍。
     */
    static void targetMark(ServerLevel level, Entity target, ArtifactFxTheme.Theme theme) {
        Vec3 waist = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        double radius = markRadiusOf(target);
        ParticleOptions fade = theme.fade(1.5F);
        for (int i = 0; i < MARK_RING_POINTS; i++) {
            double a = TAU * i / MARK_RING_POINTS;
            at(level, fade, waist.add(Math.cos(a) * radius, 0.0D, Math.sin(a) * radius));
        }
        // 中心点只给窄目标：宽体身上它在模型内部，发了也看不见。
        if (target.getBbWidth() <= WIDE_TARGET_WIDTH) {
            at(level, theme.glint(), waist);
        }
    }

    /**
     * 标记环的半径。余量必须与碰撞箱宽度成比例，不能改成固定常数：
     * 宽度 4 格以上的目标只留固定 0.25 格时，环会贴在模型表面被整圈遮住。
     */
    private static double markRadiusOf(Entity target) {
        double half = target.getBbWidth() * 0.5D;
        return half + Math.max(0.25D, half * 0.35D);
    }

    /**
     * 传送门柱：落点一圈环加一根中轴光柱。环给“落在哪”，柱给“从这里出现”。
     *
     * 传送是瞬间事件，只画贴地环时玩家在空中或水下读不出落点。
     */
    static void portalColumn(ServerLevel level, ArtifactFxTheme.Theme theme,
                             Vec3 center, double radius) {
        int points = Math.min(MAX_RING_POINTS, 14 + (int) (radius * 6.0D));
        ParticleOptions trace = theme.trace(1.5F);
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points;
            at(level, trace, center.add(Math.cos(a) * radius, 0.1D, Math.sin(a) * radius));
            if (i % 4 == 0) {
                at(level, theme.residue(), center.add(Math.cos(a) * radius * 0.7D, 0.05D,
                        Math.sin(a) * radius * 0.7D));
            }
        }
        for (int i = 0; i < MAX_COLUMN_POINTS; i++) {
            double y = 0.15D + i * 0.22D;
            // 光柱随高度旋进：直上直下的一列点从任何角度都读成一条直线，旋进才有体积。
            double a = TAU * i / 7.0D;
            double r = radius * 0.28D;
            at(level, theme.coreDust(1.3F), center.add(Math.cos(a) * r, y, Math.sin(a) * r));
            if (i % 4 == 0) {
                at(level, theme.glint(), center.add(0.0D, y, 0.0D));
            }
        }
    }

    /**
     * 解除标记：向外散开的一团，与 targetMark 的向内收束相反，读作“放开了”。
     * 必须用带偏移的散点而不是逐点画环：解除会在同一 tick 对整批实体触发，
     * 逐点画环时每个实体都乘一份广播包，而“放开了”不需要精确形状。
     */
    static void releaseMark(ServerLevel level, Entity target, ArtifactFxTheme.Theme theme) {
        Vec3 waist = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        double spread = markRadiusOf(target) * 0.8D;
        level.sendParticles(theme.edgeDust(1.1F), waist.x, waist.y, waist.z,
                5, spread, 0.2D, spread, 0.02D);
        level.sendParticles(theme.residue(), waist.x, waist.y, waist.z,
                2, spread * 0.5D, 0.15D, spread * 0.5D, 0.01D);
    }

    /**
     * 离场余烬：在传送起点留一小团收束残辉。
     *
     * 用散点而不是 portalColumn：起点只需要读出“他从这里走了”，
     * 落点柱留给目的地；两端都画柱时一次传送就是九十多个广播包。
     */
    static void departureWisp(ServerLevel level, ArtifactFxTheme.Theme theme, Vec3 origin) {
        level.sendParticles(theme.fade(1.4F), origin.x, origin.y + 0.9D, origin.z,
                8, 0.3D, 0.6D, 0.3D, 0.03D);
        level.sendParticles(theme.residue(), origin.x, origin.y + 0.5D, origin.z,
                3, 0.25D, 0.3D, 0.25D, 0.01D);
    }

    /** 隐纹显形：方块面上的小十字。方块是格子对齐的，散点会读成周围有雾。 */
    static void revealCross(ServerLevel level, ArtifactFxTheme.Theme theme, Vec3 center) {
        ParticleOptions trace = theme.trace(1.1F);
        at(level, trace, center);
        for (int axis = 0; axis < 4; axis++) {
            double a = TAU * axis / 4.0D;
            at(level, trace, center.add(Math.cos(a) * 0.3D, 0.0D, Math.sin(a) * 0.3D));
        }
        at(level, theme.glint(), center.add(0.0D, 0.2D, 0.0D));
    }
}
