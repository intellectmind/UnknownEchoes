package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 每把武器一个战技配色常量,调用点只引用常量。身份由 core/edge 两支染色 dust 承担,
 * glint 与 residue 可跨武器复用,判重看"颜色距离 + 词汇对"的组合。
 * 任何槽位都不得使用 `BossParticleTheme` 的 body 粒子:战技会被读成对应 Boss 的招式。
 */
final class WeaponFxTheme {

    private WeaponFxTheme() {
    }

    /**
     * core 描形,edge 收尾。scale 同时放大尺寸与寿命:`DustParticleBase` 用 scale 乘 lifetime,
     * 所以 scale 越大形状停留越久,描形点要比残迹点大一档才不会先于残迹消失。
     */
    record Theme(Vector3f core, Vector3f edge, ParticleOptions glint, ParticleOptions residue) {

        /** 形状骨架:由 core 亮到 edge,落点自带一次亮起再收敛的过程。 */
        DustColorTransitionOptions trace(float scale) {
            return new DustColorTransitionOptions(this.core, this.edge, scale);
        }

        /** 命中残辉:反向渐变,收进本武器的主色而不是褪成白点。 */
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

    static final Theme ECHO_BROADSWORD = theme(
            0x6FE0D2, 0x1E5E62, ParticleTypes.CRIT, ParticleTypes.GLOW);

    /** 断甲必须留在冷色:暖色带已被余烬弩与残钟锤占满,换成火色会与那两把撞车。 */
    static final Theme ECHO_BATTLEAXE = theme(
            0xB9C6D6, 0x3E4A5A, ParticleTypes.CRIT, ParticleTypes.POOF);

    static final Theme ECHO_LONGBOW = theme(
            0xD2E86B, 0x566E1C, ParticleTypes.CRIT, ParticleTypes.GLOW);

    static final Theme WIND_SPEAR = theme(
            0x9DF2B0, 0x2C7A4A, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CRIT);

    static final Theme TIDE_CROSSBOW = theme(
            0x3FA8E0, 0x10405E, ParticleTypes.NAUTILUS, ParticleTypes.UNDERWATER);

    /** 镜光靠低饱和近白与其余冷色区分,不靠色相:实心 dust 方片的饱和差比色相差更好读。 */
    static final Theme TRUE_SIGHT_BLADE = theme(
            0xE8F4F7, 0x8FA8B4, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    static final Theme SILENCE_SCYTHE = theme(
            0x6B4FA8, 0x311E4F, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SMOKE);

    static final Theme CRYSTAL_SONG_STAFF = theme(
            0xD66BE8, 0x4A1258, ParticleTypes.NOTE, ParticleTypes.GLOW);

    static final Theme BROKEN_BELL_HAMMER = theme(
            0xE0A83C, 0x6B4210, ParticleTypes.CRIT, ParticleTypes.POOF);

    static final Theme DREAM_BLOOM_BOW = theme(
            0xF5A8D2, 0x8A3A66, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.CHERRY_LEAVES);

    static final Theme GRAVITY_INVERSION_BOW = theme(
            0x5A6BE8, 0x16205E, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.GLOW);

    static final Theme ASH_SCRIPT_CROSSBOW = theme(
            0xFF6B2B, 0x7A1F05, ParticleTypes.SMALL_FLAME, ParticleTypes.ASH);

    static final Theme ECHO_OATHBLADE = theme(
            0xF7EBC4, 0xA88A42, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    static final Theme GRAVITY_HAMMER = theme(
            0x7A5AD6, 0x381970, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.POOF);

    static final Theme RIFT_DAGGER = theme(
            0xE85A7A, 0x5E1024, ParticleTypes.CRIT, ParticleTypes.SMOKE);

    static final Theme ANCHOR_BLADE = theme(
            0x4A9E8A, 0x123830, ParticleTypes.SCRAPE, ParticleTypes.POOF);

    static final Theme FOLDED_GLAIVE = theme(
            0xC48A5A, 0x4A2A12, ParticleTypes.CRIT, ParticleTypes.SMOKE);

    static final Theme PALIMPSEST_SABER = theme(
            0xA8C4E8, 0x2E4A78, ParticleTypes.ENCHANT, ParticleTypes.GLOW);

    static final Theme ORRERY_LANCE = theme(
            0xF0C8F7, 0x6B3A78, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    /** 两把世界回响体武器的身份在 edge:core 已与誓约刃、镜隙刃同为近白,改 core 无法区分。 */
    static final Theme WORLD_RIFT_GREATSWORD = theme(
            0xF7F2E0, 0xB85AE8, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    static final Theme FIVEFOLD_STAR_SPEAR = theme(
            0xFFE8A8, 0xE85A2B, ParticleTypes.END_ROD, ParticleTypes.GLOW);

    /**
     * 按 tooltipKey 取主题。多把武器共用一个 Item 类时靠这里分色,
     * 落到 default 意味着新武器没有配色,会与 echo_broadsword 撞车 —— 新增武器必须在此登记。
     */
    static Theme of(String tooltipKey) {
        return switch (tooltipKey) {
            case "echo_battleaxe" -> ECHO_BATTLEAXE;
            case "echo_longbow" -> ECHO_LONGBOW;
            case "wind_spear" -> WIND_SPEAR;
            case "tide_crossbow" -> TIDE_CROSSBOW;
            case "true_sight_blade" -> TRUE_SIGHT_BLADE;
            case "silence_scythe" -> SILENCE_SCYTHE;
            case "crystal_song_staff" -> CRYSTAL_SONG_STAFF;
            case "broken_bell_hammer" -> BROKEN_BELL_HAMMER;
            case "dream_bloom_bow" -> DREAM_BLOOM_BOW;
            case "gravity_inversion_bow" -> GRAVITY_INVERSION_BOW;
            case "ash_script_crossbow" -> ASH_SCRIPT_CROSSBOW;
            case "echo_oathblade" -> ECHO_OATHBLADE;
            case "gravity_hammer" -> GRAVITY_HAMMER;
            case "rift_dagger" -> RIFT_DAGGER;
            case "anchor_blade" -> ANCHOR_BLADE;
            case "folded_glaive" -> FOLDED_GLAIVE;
            case "palimpsest_saber" -> PALIMPSEST_SABER;
            case "orrery_lance" -> ORRERY_LANCE;
            case "world_rift_greatsword" -> WORLD_RIFT_GREATSWORD;
            case "fivefold_star_spear" -> FIVEFOLD_STAR_SPEAR;
            default -> ECHO_BROADSWORD;
        };
    }
}
