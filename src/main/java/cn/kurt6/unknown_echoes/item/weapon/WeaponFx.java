package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 战技演出形状库:扇形技画弧、直线技画束、半径技画环、自身增益画螺旋。
 * 形状必须与判定几何同构,不一致时玩家会照着特效站位,然后被没有特效的那一侧判定命中。
 * 武器演出只改这里,不要改 `BossFx`:那边被全部 Boss 与 Mini Boss 共用。
 */
final class WeaponFx {
    private static final double TAU = Math.PI * 2.0D;

    /** 单次演出的采样上限。粒子是纯表现,预算不能随判定半径或目标数无限增长。 */
    private static final int MAX_ARC_POINTS = 22;
    private static final int MAX_RING_POINTS = 24;
    private static final int MAX_BEAM_POINTS = 28;

    /** 超过这个碰撞箱宽度就按"宽体"处理:中轴与贴面的粒子会被模型遮住。 */
    private static final float WIDE_TARGET_WIDTH = 1.6F;

    /**
     * 宽体重击的冲击柱根数上限。
     *
     * 不要往上调:群体技单次可命中 12 个目标,柱数是按目标数乘上去的,取 4 时最坏一招
     * 就要发近 500 个广播包。两根对位柱已经足够读出"被砸中"。
     */
    private static final int MAX_HEAVY_COLUMNS = 2;

    private WeaponFx() {
    }

    /** 定点投放:count=1 且偏移与速度全 0 时客户端的高斯采样恒为 0,落点即给定坐标。 */
    private static void at(ServerLevel level, ParticleOptions particle, Vec3 p) {
        level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        return look.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
    }

    private static Vec3 waistOf(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
    }

    /**
     * 扇形斩弧:沿玩家朝向画一道有厚度的弧,给锥形判定用。
     *
     * halfAngle 是弧的半张角(弧度),与判定的 minDot 关系是 halfAngle = acos(minDot);
     * 调用点必须由 minDot 反算传入,不要各自填一个看起来差不多的角度。
     */
    static void slashArc(ServerLevel level, ServerPlayer player, WeaponFxTheme.Theme theme,
                         double radius, double halfAngle) {
        // halfAngle 必须钳到正值:minDot=1.0 的锥退化成射线时它是 0,lift 会算出
        // 0/0=NaN 并把 NaN 坐标广播给每个客户端。不能改成提前返回,调用点已经结算了伤害。
        double span = Math.max(1.0E-3D, halfAngle);
        Vec3 look = flatLook(player);
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 pivot = player.position().add(0.0D, player.getBbHeight() * 0.45D, 0.0D);
        CombatFx.themed(level, Shape.SLASH, pivot, look, radius, 0, span,
                theme.core(), theme.edge(), 10);
        int points = Math.min(MAX_ARC_POINTS, 8 + (int) (radius * 2.0D));
        ParticleOptions trace = theme.trace(.9F);
        for (int i = 0; i < points; i++) {
            double a = -span + 2.0D * span * i / (points - 1);
            Vec3 dir = look.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
            // 弧尖上挑:全平的弧从第一人称看是一条横线,读不出挥击方向。
            double lift = 0.35D * (1.0D - Math.abs(a) / span);
            at(level, trace, pivot.add(dir.scale(radius)).add(0.0D, lift, 0.0D));
            if (i % 3 == 0) {
                at(level, theme.edgeDust(.7F), pivot.add(dir.scale(radius * 0.72D)));
            }
            if (i % 5 == 0) {
                at(level, theme.glint(), pivot.add(dir.scale(radius * 0.9D)).add(0.0D, lift, 0.0D));
            }
        }
    }

    /**
     * 贯穿光束:沿视线铺一条有侧翼的束,给直线判定用。
     *
     * length 必须是调用点已经用 level.clip 截断过的可达距离,否则光束会穿墙而判定不会。
     */
    static void pierceBeam(ServerLevel level, ServerPlayer player, WeaponFxTheme.Theme theme,
                           Vec3 origin, Vec3 direction, double length) {
        Vec3 dir = direction.lengthSqr() < 1.0E-4D ? flatLook(player) : direction.normalize();
        Vec3 side = Math.abs(dir.y) > 0.92D
                ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-dir.z, 0.0D, dir.x).normalize();
        int steps = Math.min(MAX_BEAM_POINTS, Math.max(3, (int) (length / 0.8D)));
        CombatFx.themed(level, Shape.BEAM, origin, dir, 0, length, 0,
                theme.core(), theme.edge(), 10);
        ParticleOptions trace = theme.trace(.85F);
        for (int i = 0; i < steps; i++) {
            double s = length * (i + 0.5D) / steps;
            Vec3 p = origin.add(dir.scale(s));
            at(level, trace, p);
            // 侧翼随距离收窄:等宽的束读成一根管子,收窄才有"从手里射出去"的方向感。
            double spread = 0.34D * (1.0D - (double) i / steps) + 0.06D;
            if (i % 2 == 0) {
                at(level, theme.edgeDust(.65F), p.add(side.scale(spread)));
                at(level, theme.edgeDust(.65F), p.add(side.scale(-spread)));
            }
            if (i % 4 == 0) {
                at(level, theme.glint(), p);
            }
        }
    }

    /**
     * 地面冲击环:贴地画环,给半径判定用。radius 直接传判定半径,让玩家看到边界在哪。
     *
     * y 取脚底而不是腰高:判定是以玩家为中心的球,但贴地环才能让玩家读出水平边界。
     */
    static void groundRing(ServerLevel level, ServerPlayer player, WeaponFxTheme.Theme theme,
                           double radius) {
        Vec3 feet = player.position();
        int points = Math.min(MAX_RING_POINTS, 10 + (int) (radius * 2.0D));
        CombatFx.themed(level, Shape.RING, feet.add(0, 0.18, 0), Vec3.ZERO, radius, 0, 0,
                theme.core(), theme.edge(), 14);
        ParticleOptions trace = theme.trace(1.6F);
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            at(level, trace, feet.add(cos * radius, 0.18D, sin * radius));
            if (i % 3 == 0) {
                at(level, theme.coreDust(1.1F), feet.add(cos * radius * 0.55D, 0.32D, sin * radius * 0.55D));
            }
            if (i % 4 == 0) {
                at(level, theme.residue(), feet.add(cos * radius * 0.85D, 0.1D, sin * radius * 0.85D));
            }
        }
    }

    /**
     * 自身增益护罩:双螺旋上升柱。与 groundRing 分开是因为增益技没有判定边界可画,
     * 画环会被读成"这里有个攻击范围"。
     */
    static void wardSpiral(ServerLevel level, ServerPlayer player, WeaponFxTheme.Theme theme,
                           double radius, int turns) {
        Vec3 feet = player.position();
        int steps = Math.min(MAX_RING_POINTS, 14 + turns * 2);
        double height = player.getBbHeight() + 0.35D;
        CombatFx.themed(level, Shape.AEGIS, feet, Vec3.ZERO, radius, height, TAU * Math.max(1, turns),
                theme.core(), theme.edge(), 18);
        ParticleOptions trace = theme.trace(1.2F);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double a = TAU * turns * t;
            double y = height * t;
            // 两条臂相位差 π:单臂螺旋绕到背面就整段消失,双臂任何视角都至少有一条可见。
            at(level, trace, feet.add(Math.cos(a) * radius, y, Math.sin(a) * radius));
            at(level, theme.edgeDust(1.0F),
                    feet.add(Math.cos(a + Math.PI) * radius, y, Math.sin(a + Math.PI) * radius));
            if (i % 4 == 0) {
                at(level, theme.glint(), feet.add(0.0D, y, 0.0D));
            }
        }
    }

    static void releaseBurst(ServerLevel level, Vec3 center, WeaponFxTheme.Theme theme, double radius) {
        CombatFx.themed(level, Shape.BURST, center, Vec3.ZERO, Math.min(1.5, Math.max(0, radius)), 0, 0,
                theme.core(), theme.edge(), 8);
    }

    /**
     * 单体命中标记:目标腰高一圈收束残辉。每个命中目标调用一次,所以采样固定 8 点 ——
     * 目标数由调用点的 limit 决定,这里再按目标数放大会让群体技的包数翻倍。
     */
    static void hitMark(ServerLevel level, LivingEntity target, WeaponFxTheme.Theme theme) {
        Vec3 waist = waistOf(target);
        double radius = markRadiusOf(target);
        ParticleOptions fade = theme.fade(1.4F);
        CombatFx.themed(level, Shape.IMPACT, waist, Vec3.ZERO, radius, 0, 0,
                theme.core(), theme.edge(), 8);
        for (int i = 0; i < 8; i++) {
            double a = TAU * i / 8.0D;
            at(level, fade, waist.add(Math.cos(a) * radius, 0.0D, Math.sin(a) * radius));
        }
        // 中心点只给窄目标:宽体身上它在模型内部,发了也看不见。
        if (target.getBbWidth() <= WIDE_TARGET_WIDTH) {
            at(level, theme.glint(), waist);
        }
    }

    /**
     * 命中环的半径。余量必须与碰撞箱宽度成比例,不能改成固定常数:
     * 宽度 4 格以上的目标只留固定 0.25 格时,环会贴在模型表面被整圈遮住。
     */
    private static double markRadiusOf(LivingEntity target) {
        double half = target.getBbWidth() * 0.5D;
        return half + Math.max(0.25D, half * 0.35D);
    }

    /** 重击命中:在 hitMark 之上补一次竖直冲击柱,用来区分"被扫到"和"被砸中"。 */
    static void heavyHitMark(ServerLevel level, LivingEntity target, WeaponFxTheme.Theme theme) {
        hitMark(level, target, theme);
        // 宽体的柱子必须画在环上:target.position() 是碰撞箱中心,整根柱子会埋在模型里。
        double radius = markRadiusOf(target);
        boolean wide = target.getBbWidth() > WIDE_TARGET_WIDTH;
        int columns = Math.min(MAX_HEAVY_COLUMNS, wide ? 2 : 1);
        Vec3 base = target.position();
        for (int c = 0; c < columns; c++) {
            double a = TAU * c / columns;
            Vec3 foot = wide ? base.add(Math.cos(a) * radius, 0.0D, Math.sin(a) * radius) : base;
            CombatFx.themed(level, Shape.COLUMN, foot, Vec3.ZERO, 0.22, 2.1, TAU,
                    theme.core(), theme.edge(), 10);
            for (int i = 0; i < 5; i++) {
                at(level, theme.coreDust(1.5F), foot.add(0.0D, 0.25D + i * 0.42D, 0.0D));
            }
            at(level, theme.residue(), foot.add(0.0D, 0.15D, 0.0D));
        }
    }

    /** 牵引线:从目标指回玩家,让"被拉过来"这件事有可见的因果。 */
    static void pullTether(ServerLevel level, ServerPlayer player,
                           LivingEntity target, WeaponFxTheme.Theme theme) {
        Vec3 from = waistOf(target);
        Vec3 to = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 span = to.subtract(from);
        CombatFx.themed(level, Shape.BEAM, from, span, 0, span.length(), 0,
                theme.core(), theme.edge(), 8);
        // 上限不要放宽:牵引技单次可命中 12 个目标,每个目标一条线,步数按目标数乘上去。
        int steps = Math.min(6, Math.max(3, (int) (span.length() / 0.9D)));
        ParticleOptions trace = theme.trace(1.1F);
        for (int i = 0; i < steps; i++) {
            at(level, trace, from.add(span.scale((i + 0.5D) / steps)));
        }
    }

    /**
     * 位移轨迹:沿实际位移路径铺残迹。调用点在设置速度之前取起点,之后玩家已经不在原地。
     */
    static void dashTrail(ServerLevel level, Vec3 origin, Vec3 direction,
                          double length, WeaponFxTheme.Theme theme) {
        Vec3 dir = direction.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        int steps = Math.min(MAX_BEAM_POINTS, Math.max(3, (int) (length / 0.7D)));
        CombatFx.themed(level, Shape.WAKE, origin, dir, .16, length, 0,
                theme.edge(), theme.core(), 10);
        for (int i = 0; i < steps; i++) {
            Vec3 p = origin.add(dir.scale(length * (i + 0.5D) / steps));
            at(level, theme.fade(1.2F), p);
            if (i % 3 == 0) {
                at(level, theme.residue(), p);
            }
        }
    }

    /** 由锥形判定的 minDot 反算弧的半张角,避免调用点各填一个近似角度。 */
    static double halfAngleOf(double minDot) {
        return Math.acos(Math.max(-1.0D, Math.min(1.0D, minDot)));
    }

    /**
     * 把视线射程截到第一个碰撞方块,供 pierceBeam 使用。
     *
     * 判定侧靠 hasLineOfSight 天然不会穿墙,但光束是逐点投放的,不截断就会画进墙里。
     */
    static double clippedBeamLength(ServerLevel level, ServerPlayer player,
                                    Vec3 origin, Vec3 direction, double range) {
        Vec3 end = origin.add(direction.normalize().scale(range));
        BlockHitResult block = level.clip(new ClipContext(origin, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return block.getType() == HitResult.Type.MISS
                ? range : origin.distanceTo(block.getLocation());
    }
}
