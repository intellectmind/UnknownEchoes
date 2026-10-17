package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.world.phys.Vec3;

final class EscapePosition {
    private EscapePosition() {
    }

    // 原版随机选点只约束初始水平方向，垂直偏移和活动范围修正仍可能靠近威胁。
    static boolean isNotCloser(Vec3 origin, Vec3 threat, Vec3 destination) {
        if (destination == null) {
            return false;
        }
        double distanceSqr = destination.distanceToSqr(threat);
        return Double.isFinite(distanceSqr) && distanceSqr >= origin.distanceToSqr(threat);
    }
}
