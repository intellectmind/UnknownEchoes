package cn.kurt6.unknown_echoes.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
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
            Map.entry("forgotten_colossus", new Profile(3.15F, 2.70F)),
            Map.entry("void_prophet", new Profile(3.30F, 1.70F)),
            Map.entry("rift_warden", new Profile(2.70F, 2.05F)),
            Map.entry("world_echo", new Profile(3.25F, 2.15F)),
            Map.entry("mirror_guardian", new Profile(2.35F, 1.80F)),
            Map.entry("storm_weaver", new Profile(1.55F, 4.75F)),
            Map.entry("silent_priest", new Profile(1.35F, 1.15F)),
            Map.entry("tide_lantern_keeper", new Profile(1.60F, 1.85F)),
            Map.entry("mirror_dust_butler", new Profile(2.10F, 2.10F)),
            Map.entry("crystal_songkeeper", new Profile(2.10F, 1.20F)),
            Map.entry("broken_bell_keeper", new Profile(1.65F, 1.80F)),
            Map.entry("dream_bloom_keeper", new Profile(2.20F, 1.60F)),
            Map.entry("lost_recorder_chief", new Profile(1.20F, 0.90F)),
            Map.entry("inverted_gatekeeper", new Profile(3.55F, 3.10F)),
            Map.entry("ash_scribe", new Profile(1.35F, 1.85F))
    );

    private EntityHitboxProfiles() {
    }

    /** 使用主体长宽与实体当前高度生成底面中心锚定的实际碰撞箱。 */
    public static AABB make(Entity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        Profile profile = PROFILES.get(id);
        double widthX = profile == null ? entity.getBbWidth() : profile.widthX();
        double depthZ = profile == null ? entity.getBbWidth() : profile.depthZ();
        if (profile != null) {
            double yaw = Math.toRadians(entity.getYRot());
            double cos = Math.abs(Math.cos(yaw));
            double sin = Math.abs(Math.sin(yaw));
            double rotatedWidthX = cos * widthX + sin * depthZ;
            double rotatedDepthZ = sin * widthX + cos * depthZ;
            widthX = rotatedWidthX;
            depthZ = rotatedDepthZ;
        }
        Vec3 position = entity.position();
        return new AABB(
                position.x - widthX * 0.5D,
                position.y,
                position.z - depthZ * 0.5D,
                position.x + widthX * 0.5D,
                position.y + entity.getBbHeight(),
                position.z + depthZ * 0.5D);
    }

    /** 在实体 yaw 改变后立即重建 profile AABB；普通实体仍保持注册的正方形尺寸。 */
    public static void refresh(Entity entity) {
        entity.setBoundingBox(make(entity));
    }

    public record Profile(float widthX, float depthZ) {
    }
}
