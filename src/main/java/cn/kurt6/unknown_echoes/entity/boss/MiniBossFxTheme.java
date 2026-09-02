package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 每只 Mini Boss 一个演出配色常量，调用点只引用常量。身份由 core/edge 两支染色 dust
 * 承担：原版固定色粒子只有十几种够亮，十只 Mini Boss + 21 把武器 + 6 件神器必然撞车。
 * 任何槽位都不得使用 BossParticleTheme 的 body 粒子：Mini Boss 会被读成对应主线 Boss。
 */
public final class MiniBossFxTheme {

    private MiniBossFxTheme() {
    }

    /** core 描形，edge 收尾。scale 同时放大尺寸与寿命，描形点要比残迹点大一档。 */
    public record Theme(Vector3f core, Vector3f edge,
                        ParticleOptions glint, ParticleOptions residue) {

        /** 形状骨架：由 core 亮到 edge，落点自带一次亮起再收敛的过程。 */
        public DustColorTransitionOptions trace(float scale) {
            return new DustColorTransitionOptions(this.core, this.edge, scale);
        }

        /** 收束残辉：反向渐变，收进这只怪的主色而不是褪成白点。 */
        public DustColorTransitionOptions fade(float scale) {
            return new DustColorTransitionOptions(this.edge, this.core, scale);
        }

        public DustParticleOptions coreDust(float scale) {
            return new DustParticleOptions(this.core, scale);
        }

        public DustParticleOptions edgeDust(float scale) {
            return new DustParticleOptions(this.edge, scale);
        }
    }

    private static Vector3f rgb(int packed) {
        return Vec3.fromRGB24(packed).toVector3f();
    }

    private static Theme theme(int core, int edge, ParticleOptions glint, ParticleOptions residue) {
        return new Theme(rgb(core), rgb(edge), glint, residue);
    }

    /** 灰烬余火的橙红。暖色带只有它与残钟看守，靠 core 的红偏移区分。 */
    public static final Theme ASH_SCRIBE = theme(
            0xFF6929, 0xB3400F, ParticleTypes.SMALL_FLAME, ParticleTypes.ASH);

    /** 铜钟的黄铜。core 必须留在纯橙：偏黄会与晶歌守谱者的黄色高光族撞车。 */
    public static final Theme BROKEN_BELL_KEEPER = theme(
            0xFF9500, 0xA35F00, ParticleTypes.CRIT, ParticleTypes.POOF);

    /** 晶歌的水晶青。六只冷色系里最偏绿的一档，色相 175。 */
    public static final Theme CRYSTAL_SONGKEEPER = theme(
            0x00D6C4, 0x008579, ParticleTypes.NOTE, ParticleTypes.GLOW);

    /** 花冠的粉。十只里唯一的品红，远处只靠色相就能认出来。 */
    public static final Theme DREAM_BLOOM_KEEPER = theme(
            0xFF528B, 0xB30F45, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.CHERRY_LEAVES);

    /** 倒悬重力的紫。edge 压到深紫，与静默镰的紫靠 core 的亮度差区分。 */
    public static final Theme INVERTED_GATEKEEPER = theme(
            0x9E7AFF, 0x3B0FB3, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.GLOW);

    /** 记录墨的青绿。色相 161，位于静默祭司与晶歌守谱者之间。 */
    public static final Theme LOST_RECORDER_CHIEF = theme(
            0x39EFB5, 0x0A7B57, ParticleTypes.ENCHANT, ParticleTypes.SQUID_INK);

    /** 镜尘的淡青。色相 199，比潮汐执灯者更蓝一档。 */
    public static final Theme MIRROR_DUST_BUTLER = theme(
            0x00AEFF, 0x0F7FB3, ParticleTypes.END_ROD, ParticleTypes.ASH);

    /** 沼泽静默的灰绿。色相 148，是冷色带最靠绿的一端。 */
    public static final Theme SILENT_PRIEST = theme(
            0x00FF77, 0x00A34C, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SMOKE);

    /** 风暴的天蓝。色相 215，冷色带最靠蓝的一端。 */
    public static final Theme STORM_WEAVER = theme(
            0x2982FF, 0x0F53B3, ParticleTypes.GUST, ParticleTypes.CLOUD);

    /** 潮汐灯的水青。色相 187，夹在晶歌与镜尘之间。 */
    public static final Theme TIDE_LANTERN_KEEPER = theme(
            0x29E6FF, 0x0090A3, ParticleTypes.NAUTILUS, ParticleTypes.BUBBLE_POP);

    /**
     * 把 core 还原成 0xRRGGBB，供 BossImpactPayload 这类只吃 int 色的通道使用。
     * 屏幕脉冲与粒子必须同色：分两处手填会各自漂移，玩家读成两只怪。
     */
    public static int packedCore(Theme theme) {
        return (Math.round(theme.core().x() * 255.0F) << 16)
                | (Math.round(theme.core().y() * 255.0F) << 8)
                | Math.round(theme.core().z() * 255.0F);
    }

    /**
     * 按 miniboss id 的 path 取主题。新增 Mini Boss 必须在此登记配色，
     * 落到 default 意味着它会与灰烬抄录员同色。
     */
    public static Theme of(String path) {
        return switch (path) {
            case "broken_bell_keeper" -> BROKEN_BELL_KEEPER;
            case "crystal_songkeeper" -> CRYSTAL_SONGKEEPER;
            case "dream_bloom_keeper" -> DREAM_BLOOM_KEEPER;
            case "inverted_gatekeeper" -> INVERTED_GATEKEEPER;
            case "lost_recorder_chief" -> LOST_RECORDER_CHIEF;
            case "mirror_dust_butler" -> MIRROR_DUST_BUTLER;
            case "silent_priest" -> SILENT_PRIEST;
            case "storm_weaver" -> STORM_WEAVER;
            case "tide_lantern_keeper" -> TIDE_LANTERN_KEEPER;
            default -> ASH_SCRIBE;
        };
    }
}
