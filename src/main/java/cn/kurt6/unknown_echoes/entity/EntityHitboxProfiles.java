package cn.kurt6.unknown_echoes.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * 主体碰撞箱的水平长宽。EntityDimensions 只有一个 width,所以注册值仍使用
 * max(widthX, depthZ);这里在实体实际 AABB 上补回模型主体需要的长方形 X/Z。
 * Minecraft 的碰撞箱本身不能旋转，实体转身时按 yaw 计算旋转后矩形的轴对齐包围盒。
 */
public final class EntityHitboxProfiles {
    private static final Map<String, Profile> PROFILES = Map.ofEntries(
            Map.entry("spore_crowned_wretch", new Profile(0.45F, 0.40F, -0.01875D, 0.0D)),
            Map.entry("cairn_shell_brute", new Profile(0.90F, 0.85F, -0.1375D, 0.0D)),
            Map.entry("mossplate_hound", new Profile(0.45F, 1.30F, -0.215625D, 0.0D)),
            Map.entry("forgotten_colossus", new Profile(3.15F, 2.70F)),
            Map.entry("void_prophet", new Profile(3.30F, 1.70F)),
            Map.entry("rift_warden", new Profile(2.70F, 2.05F)),
            Map.entry("world_echo", new Profile(3.25F, 2.15F)),
            Map.entry("mirror_guardian", new Profile(2.35F, 1.80F)),
            Map.entry("storm_weaver", new Profile(1.60F, 4.75F)),
            Map.entry("silent_priest", new Profile(1.35F, 1.15F)),
            Map.entry("tide_lantern_keeper", new Profile(1.60F, 1.45F)),
            Map.entry("mirror_dust_butler", new Profile(2.10F, 1.40F)),
            Map.entry("crystal_songkeeper", new Profile(2.20F, 1.25F)),
            Map.entry("broken_bell_keeper", new Profile(1.75F, 1.80F)),
            Map.entry("dream_bloom_keeper", new Profile(2.30F, 1.60F)),
            Map.entry("lost_recorder_chief", new Profile(1.20F, 0.90F)),
            Map.entry("inverted_gatekeeper", new Profile(3.60F, 3.30F)),
            Map.entry("ash_scribe", new Profile(1.45F, 2.15F)),
            Map.entry("wind_drift_jelly", new Profile(0.65F, 0.60F, 0.0D, 0.4375D)),
            Map.entry("reedwing_dragonfly", new Profile(0.35F, 0.40F, -0.07813D, 0.1884D)),
            Map.entry("gale_shrike", new Profile(0.30F, 0.50F, -0.08921D, 0.2D)),
            Map.entry("ash_lantern_bat", new Profile(0.25F, 0.40F, -0.05484D, 0.2D)),
            Map.entry("petalwing_butterfly", new Profile(0.25F, 0.55F, 0.0531D, 0.3877D)),
            Map.entry("mosslight_firefly", new Profile(0.35F, 0.60F, 0.0859D, 0.2311D)),
            Map.entry("bell_needle_wasp", new Profile(0.35F, 0.85F, 0.1234D, 0.3249D)),
            Map.entry("shardwing_skate", new Profile(0.40F, 0.45F, -0.0063D, 0.4688D)),
            Map.entry("sailtail_minnow", new Profile(0.30F, 0.60F, -0.10938D, 0.3969D)),
            Map.entry("moss_sail_snail", new Profile(0.45F, 0.80F, 0.03125D, 0.2812D)),
            Map.entry("crystal_spine_urchin", new Profile(0.35F, 0.40F, -0.0125D, 0.325D)),
            Map.entry("ash_sail_serpent", new Profile(0.30F, 1.15F, 0.1719D, 0.3781D)),
            Map.entry("mossback_hedgehog", new Profile(0.45F, 0.85F, -0.05844D, 0.00000D)),
            Map.entry("emberclaw_scorpion", new Profile(0.65F, 0.55F, -0.00469D, 0.0D)),
            Map.entry("lantern_seahorse", new Profile(0.25F, 0.45F, -0.14063D, 0.2875D)),
            Map.entry("reefjaw_shark", new Profile(0.40F, 0.85F, -0.15156D, 0.2313D)),
            Map.entry("slatehorn_boar", new Profile(0.50F, 0.95F, -0.16563D, 0.00000D)),
            Map.entry("hollow_reed_serpent", new Profile(0.30F, 1.40F, 0.2375D, 0.0D)),
            Map.entry("riftfang_eel", new Profile(0.30F, 1.40F, 0.2D, 0.1875D)),
            Map.entry("needlefin_lionfish", new Profile(0.35F, 0.60F, -0.05156D, 0.2062D)),
            Map.entry("mossfang_toad", new Profile(0.70F, 0.70F, -0.0406D, 0.0D)),
            Map.entry("amberback_beetle", new Profile(0.70F, 0.85F, -0.0219D, 0.0D)),
            Map.entry("lanternjaw_angler", new Profile(0.50F, 0.55F, -0.08281D, 0.125D)),
            Map.entry("sawtooth_prawn", new Profile(0.40F, 1.00F, -0.01406D, 0.11958D)),
            Map.entry("reed_mask_raider", new Profile(0.50F, 0.70F, 0.12878D, -0.21406D, 0.00000D)),
            Map.entry("broken_bell_axeman", new Profile(0.75F, 0.75F, 0.01098D, 0.0D)),
            Map.entry("rift_page_stalker", new Profile(0.45F, 0.55F, -0.03527D, -0.10953D, 0.00000D))
    );

    private EntityHitboxProfiles() {
    }

    /** 使用主体长宽与实体当前高度生成底面中心锚定的实际碰撞箱。 */
    public static AABB make(Entity entity) {
        return make(entity, null);
    }

    private static AABB make(Entity entity, AABB existing) {
        Profile profile = profile(entity);
        double widthX = profile == null ? entity.getBbWidth() : profile.widthX();
        double depthZ = profile == null ? entity.getBbWidth() : profile.depthZ();
        if (profile != null) {
            double scale = profile.alignModel() ? entity.getBbWidth() / entity.getType().getWidth() : 1.0D;
            widthX *= scale;
            depthZ *= scale;
            float bodyYaw = profile.alignModel() && entity instanceof LivingEntity living
                    ? living.yBodyRot : entity.getYRot();
            double yaw = Math.toRadians(bodyYaw);
            double cos = Math.abs(Math.cos(yaw));
            double sin = Math.abs(Math.sin(yaw));
            double rotatedWidthX = cos * widthX + sin * depthZ;
            double rotatedDepthZ = sin * widthX + cos * depthZ;
            widthX = rotatedWidthX;
            depthZ = rotatedDepthZ;
        }
        Vec3 position = entity.position();
        double minX = position.x - widthX * 0.5D;
        double minY = position.y;
        double minZ = position.z - depthZ * 0.5D;
        double maxX = position.x + widthX * 0.5D;
        double maxY = position.y + entity.getBbHeight();
        double maxZ = position.z + depthZ * 0.5D;
        if (existing != null && existing.minX == minX && existing.minY == minY && existing.minZ == minZ
                && existing.maxX == maxX && existing.maxY == maxY && existing.maxZ == maxZ) {
            return existing;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 边界相同时复用原箱体；位置、体型或朝向变化仍立即更新。 */
    public static void refresh(Entity entity) {
        AABB updated = make(entity, entity.getBoundingBox());
        if (updated != entity.getBoundingBox()) {
            entity.setBoundingBox(updated);
        }
    }

    public static Profile profile(Entity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return PROFILES.get(id);
    }

    /** centerX/centerZ/floorY 是模型主体在格单位下的锚点；渲染时归中，不移动实体物理坐标。 */
    public record Profile(float widthX, float depthZ, double centerX, double centerZ, double floorY,
                          boolean alignModel) {
        public Profile(float widthX, float depthZ) {
            this(widthX, depthZ, 0.0D, 0.0D, 0.0D, false);
        }

        public Profile(float widthX, float depthZ, double centerZ, double floorY) {
            this(widthX, depthZ, 0.0D, centerZ, floorY, true);
        }

        public Profile(float widthX, float depthZ, double centerX, double centerZ, double floorY) {
            this(widthX, depthZ, centerX, centerZ, floorY, true);
        }
    }
}
