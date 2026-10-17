package cn.kurt6.unknown_echoes.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CombatFxVertices {
    private CombatFxVertices() { }

    public static void quad(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 a, Vec3 b,
                            Vec3 sideA, Vec3 sideB, double wa, double wb,
                            float u0, float u1, int color, float alpha) {
        offsetVertex(out, matrix, camera, a, sideA, wa, u0, 0, color, alpha);
        offsetVertex(out, matrix, camera, a, sideA, -wa, u0, 1, color, alpha);
        offsetVertex(out, matrix, camera, b, sideB, -wb, u1, 1, color, alpha);
        offsetVertex(out, matrix, camera, b, sideB, wb, u1, 0, color, alpha);
    }

    private static void offsetVertex(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                                     Vec3 point, Vec3 side, double width,
                                     float u, float v, int color, float alpha) {
        // 保留先偏移再减相机的 double 运算顺序，避免远坐标处的舍入差异。
        vertex(out, matrix, (point.x + side.x * width) - camera.x,
                (point.y + side.y * width) - camera.y, (point.z + side.z * width) - camera.z,
                u, v, color, alpha);
    }

    public static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 point,
                              float u, float v, int color, float alpha) {
        vertex(out, matrix, point.x, point.y, point.z, u, v, color, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                               float u, float v, int color, float alpha) {
        out.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (alpha * 255))
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
    }
}
