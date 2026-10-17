package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class AerialAttackFx {
    private AerialAttackFx() {
    }

    // 光束必须使用命中判定的已裁剪终点，不能在表现层延长到墙后。
    public static void beam(ServerLevel level, Vec3 start, Vec3 end, boolean release) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0e-6D) {
            return;
        }
        CombatFx.emit(level, release ? Shape.BEAM : Shape.TRACE, start, delta.normalize(),
                0, delta.length(), 0, release ? 0xB6DBEE : 0x58788A,
                0x415766, release ? 8 : 4, false);
    }

    // 三个大圆标示同一个三维球面；不能用地面圆环替代垂直范围。
    public static void pulse(ServerLevel level, Vec3 center, double radius, boolean release) {
        CombatFx.emit(level, Shape.ORBIT, center, Vec3.ZERO, radius, 0, 0,
                release ? 0x92E1BA : 0x456958, 0x28483B, release ? 9 : 6, false);
    }
}
