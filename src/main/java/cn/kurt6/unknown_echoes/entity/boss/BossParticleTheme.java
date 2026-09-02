package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 六只主线 Boss 的粒子主题：任意两只的调色板都不得撞车，否则 30 米外读成同一只怪。
 * 调用点按三档语义取常量，不要新增 sendParticles 调用。
 */
public final class BossParticleTheme {

    private BossParticleTheme() {
    }

    /**
     * body 铺量，必须是三档里最显眼的：暗色放进 body 会让预警在暗场地里消失。
     * 暗色归 ember，spark 只作命中与刃光的高亮点。
     */
    public record Theme(ParticleOptions body, ParticleOptions spark, ParticleOptions ember) {
    }

    /** 石屑扬尘：body 要上升慢、体积大，发光尖点会读成火花；ember 不用白余烬,浅色石头上看不见。 */
    public static final Theme COLOSSUS = new Theme(
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            ParticleTypes.CRIT,
            ParticleTypes.ASH);

    /** 镜屑：WHITE_ASH 必须留在 body,玩家靠它认怪；END_ROD 只能作 spark,进 body 会撞车。 */
    public static final Theme MIRROR = new Theme(
            ParticleTypes.WHITE_ASH,
            ParticleTypes.END_ROD,
            ParticleTypes.SNOWFLAKE);

    /** SQUID_INK 只能作 ember：它在非水方块里当 tick 自毁,而这只 Boss 有陆上形态。 */
    public static final Theme ABYSS = new Theme(
            ParticleTypes.SPLASH,
            ParticleTypes.BUBBLE_POP,
            ParticleTypes.SQUID_INK);

    /** REVERSE_PORTAL 归先知独占：RIFT 共用会让两只深层 Boss 撞车。 */
    public static final Theme VOID_PROPHET = new Theme(
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.DRAGON_BREATH,
            ParticleTypes.PORTAL);

    /** 蓝黑折线。三档都不得引入 REVERSE_PORTAL：那是 VOID_PROPHET 的 body。 */
    public static final Theme RIFT = new Theme(
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.SONIC_BOOM,
            ParticleTypes.SCULK_CHARGE_POP);

    /** 顺序必须与 WorldEcho.aspectTranslationKey 的阶段逐项对齐,错位会让玩家读不出在模拟谁。 */
    public static final Theme[] WORLD_ECHO_CYCLE = {
            COLOSSUS,      // aspect 0 = wind        （风之回响 / 遗忘巨像）
            ABYSS,         // aspect 1 = tide        （潮汐回响 / 深渊观测者）
            MIRROR,        // aspect 2 = true_sight  （真视回响 / 镜像守护者）
            VOID_PROPHET,  // aspect 3 = gravity     （重力回响 / 虚空先知）
            RIFT,          // aspect 4 = void        （虚空回响 / 裂隙看守者）
    };

    /** 越界钳到两端而不是抛异常：粒子只是表现,不该让阶段计算的边界把战斗打崩。 */
    public static Theme forPhase(int phase) {
        if (phase < 0) {
            return WORLD_ECHO_CYCLE[0];
        }
        return WORLD_ECHO_CYCLE[Math.min(phase, WORLD_ECHO_CYCLE.length - 1)];
    }
}
