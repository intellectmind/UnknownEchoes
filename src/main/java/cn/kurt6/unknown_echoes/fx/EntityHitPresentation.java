package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import net.minecraft.world.phys.Vec3;

public final class EntityHitPresentation {
    private EntityHitPresentation() { }

    public static int color(String id) {
        if (id.contains("ash") || id.contains("bell") || id.contains("slate")) return 0xF3BD75;
        if (id.contains("rift") || id.contains("void") || id.contains("fold") || id.contains("gravity")) return 0xB99AF2;
        if (id.contains("crystal") || id.contains("mirror")) return 0xB9E8FF;
        if (id.contains("tide") || id.contains("water") || id.contains("abyss") || id.contains("fish")
                || id.contains("minnow") || id.contains("ray")) return 0x65DDE2;
        if (id.contains("wind") || id.contains("gale") || id.contains("reed")) return 0xB7ECD2;
        if (id.contains("dream") || id.contains("petal")) return 0xEEADDB;
        if (id.contains("silent") || id.contains("hollow") || id.contains("lost")) return 0xBFB4DB;
        return 0xBCE4A3;
    }

    public static CombatFxPayload cue(String id, Vec3 center, Vec3 camera, float width, float height) {
        Vec3 towardCamera = camera.subtract(center).normalize();
        if (towardCamera.lengthSqr() < 1E-8) towardCamera = new Vec3(0, 0, 1);
        // 取视线穿出碰撞箱的位置，宽体和高个体的受击闪光不能埋在模型中。
        double extent = Math.min(width * .5 / Math.max(1E-8, Math.abs(towardCamera.x)),
                Math.min(height * .5 / Math.max(1E-8, Math.abs(towardCamera.y)),
                        width * .5 / Math.max(1E-8, Math.abs(towardCamera.z))));
        Vec3 position = center.add(towardCamera.scale(extent + .12));
        int color = color(id);
        return new CombatFxPayload(CombatFxPayload.Shape.BURST, position, towardCamera,
                Math.max(.18F, Math.min(.85F, width * .38F)), 0, 0,
                color, (color & 0xFEFEFE) >> 1, 10, false, -1);
    }
}
