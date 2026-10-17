package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.particle.ParticleItemGeometry;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.particle.ParticleVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClientParticleItemFx {
    private static final Map<SpriteContents, ParticleItemGeometry.Profile> PROFILES = new WeakHashMap<>();
    private static final RenderType MATERIAL = RenderType.entityTranslucentEmissive(
            UnknownEchoes.id("textures/effect/energy_ribbon.png"), false);
    private ClientParticleItemFx() { }

    // 在 ItemRenderer 弹出模型矩阵前绘制，继承实际持握、挥动和物品 display 变换。
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                              MultiBufferSource buffers, BakedModel model) {
        if (!context.firstPerson() || model.isCustomRenderer() || model.isGui3d()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getCameraEntity() != mc.player
                || !mc.options.getCameraType().isFirstPerson() || !ClientConfig.ENABLE_ECHO_PARTICLES.get()
                || !mc.player.isAlive() || mc.player.isInvisible() || mc.player.isSpectator()
                || ClientEquipmentFx.quietWingFlight(mc.player) || ClientShaderCompat.pass() == ClientShaderCompat.Pass.SKIP) return;
        ItemDisplayContext mainHand = mc.player.getMainArm() == HumanoidArm.LEFT
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        if (context != mainHand || !ItemStack.isSameItemSameComponents(stack, mc.player.getMainHandItem())) return;
        var data = ParticleRules.data(stack);
        if (data.total() == 0) return;
        SpriteContents sprite = model.getParticleIcon(ModelData.EMPTY).contents();
        var profile = PROFILES.computeIfAbsent(sprite, value -> ParticleItemGeometry.profile(value.width(), value.height(),
                (x, y) -> !value.isTransparent(0, x, y)));
        if (profile.edges().isEmpty()) return;
        double tick = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        var primary = ParticleVisuals.dominant(data);
        var secondary = ParticleVisuals.secondary(data, primary);
        float alpha = ParticleItemGeometry.opacity(tick) * (data.total() < 5 ? .55F : 1);
        Matrix4f matrix = poses.last().pose();
        // 一个共享 consumer 写完全部光带；不能跨材质交替写入旧 consumer。
        VertexConsumer buffer = buffers.getBuffer(MATERIAL);
        for (var edge : profile.edges()) {
            for (int face = 0; face < 2; face++) {
                double z = face == 0 ? -.036 : .036;
                float flow = ParticleItemGeometry.flow(edge, tick);
                draw(buffer, matrix, edge.from().add(0, 0, z), edge.to().add(0, 0, z),
                        .004 + flow * .003, ParticleVisuals.core(primary), alpha * (.75F + flow * .8F), tick);
            }
        }
        int setting = mc.options.particles().get().getId();
        int glints = setting == 2 ? 0 : setting == 1 ? 3 : data.total() >= 20 ? 8 : 4;
        for (int i = 0; i < glints; i++) {
            var glint = ParticleItemGeometry.glint(profile, i, tick);
            Vec3 axis = glint.axis().scale(.025);
            Vec3 side = new Vec3(-axis.y, axis.x, 0).scale(.6);
            int color = ParticleVisuals.core(i % 3 == 0 ? secondary : primary);
            for (int face = 0; face < 2; face++) {
                Vec3 center = glint.center().add(0, 0, face == 0 ? -.04 : .04);
                draw(buffer, matrix, center.subtract(axis), center.add(axis), .005, color, alpha * glint.alpha() * 1.7F, tick);
                draw(buffer, matrix, center.subtract(side), center.add(side), .004, color, alpha * glint.alpha(), tick);
            }
        }
        if (data.total() >= 20 || secondary != primary) {
            for (int face = 0; face < 2; face++) for (int i = 0; i < 24; i++) {
                double z = face == 0 ? -.038 : .038;
                Vec3 from = ParticleItemGeometry.ring(profile, i * Math.PI / 12 + tick * .012, data.total() == 50).add(0, 0, z);
                Vec3 to = ParticleItemGeometry.ring(profile, (i + 1) * Math.PI / 12 + tick * .012, data.total() == 50).add(0, 0, z);
                draw(buffer, matrix, from, to, .003, ParticleVisuals.core(secondary), alpha * .7F, tick);
            }
        }
    }

    private static void draw(VertexConsumer buffer, Matrix4f matrix, Vec3 from, Vec3 to,
                             double width, int color, float alpha, double tick) {
        Vec3 delta = to.subtract(from);
        Vec3 side = new Vec3(-delta.y, delta.x, 0).normalize();
        float u = (float) (tick * -.008);
        CombatFxVertices.quad(buffer, matrix, Vec3.ZERO, from, to, side, side, width * 2, width * 2,
                u, u + .12F, color, alpha * .3F);
        CombatFxVertices.quad(buffer, matrix, Vec3.ZERO, from, to, side, side, width, width,
                u, u + .12F, color, alpha);
    }
}
