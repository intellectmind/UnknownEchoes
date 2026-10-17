package cn.kurt6.unknown_echoes.artifact;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 每件神器一个演出配色常量，调用点只引用常量。身份由 core/edge 两支染色 dust 承担，
 * 每支 core 取自该神器图标里最亮的饱和像素族，玩家看特效与看物品是同一个颜色印象。
 * 任何槽位都不得使用 `BossParticleTheme` 的 body 粒子：神器会被读成对应 Boss 的招式。
 */
final class ArtifactFxTheme {

    private ArtifactFxTheme() {
    }

    /** core 描形，edge 收尾。scale 同时放大尺寸与寿命，描形点要比残迹点大一档。 */
    record Theme(Vector3f core, Vector3f edge, ParticleOptions glint, ParticleOptions residue) {

        /** 形状骨架：由 core 亮到 edge，落点自带一次亮起再收敛的过程。 */
        DustColorTransitionOptions trace(float scale) {
            return new DustColorTransitionOptions(this.core, this.edge, scale);
        }

        /** 收束残辉：反向渐变，收进本神器的主色而不是褪成白点。 */
        DustColorTransitionOptions fade(float scale) {
            return new DustColorTransitionOptions(this.edge, this.core, scale);
        }

        DustParticleOptions coreDust(float scale) {
            return new DustParticleOptions(this.core, scale);
        }

        DustParticleOptions edgeDust(float scale) {
            return new DustParticleOptions(this.edge, scale);
        }
    }

    private static Vector3f rgb(int packed) {
        return Vec3.fromRGB24(packed).toVector3f();
    }

    private static Theme theme(int core, int edge, ParticleOptions glint, ParticleOptions residue) {
        return new Theme(rgb(core), rgb(edge), glint, residue);
    }

    /** 罗盘的黄铜盘面配青铜刻度：暖 core 在六件神器里独一份，飞行时远处也认得出。 */
    static final Theme STORM_COMPASS = theme(
            0xF2C878, 0x0E6E70, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CRIT);

    /** 薄荷 core 必须配亮青 edge：深青会与回声长剑、镜隙刃挤在同一条冷色带上。 */
    static final Theme TIDE_LANTERN = theme(
            0x87F1D1, 0x0086B8, ParticleTypes.NAUTILUS, ParticleTypes.UNDERWATER);

    static final Theme ECHO_LENS = theme(
            0xDA9FFD, 0x8C10BC, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    /** 沙漏与透镜同属紫系，靠 core 的品红偏移与 edge 的靛蓝分开，不靠形状。 */
    static final Theme TIME_HOURGLASS = theme(
            0xE868F0, 0x5040A0, ParticleTypes.ENCHANT, ParticleTypes.GLOW);

    static final Theme DIMENSION_ANCHOR = theme(
            0x5CF2EC, 0x7C6398, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    /** 裂隙锚的近白 core 配深紫 edge：身份在 edge，改 core 会与维度锚撞车。 */
    static final Theme RIFT_ANCHOR = theme(
            0xC8FBFF, 0x6B21C4, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SMOKE);

    /** 按神器取主题。新增神器必须在此登记配色，否则会与风暴罗盘同色。 */
    static Theme of(ArtifactType type) {
        return switch (type) {
            case TIDE_LANTERN -> TIDE_LANTERN;
            case ECHO_LENS -> ECHO_LENS;
            case TIME_HOURGLASS -> TIME_HOURGLASS;
            case DIMENSION_ANCHOR -> DIMENSION_ANCHOR;
            case RIFT_ANCHOR -> RIFT_ANCHOR;
            case STORM_COMPASS -> STORM_COMPASS;
        };
    }
}
