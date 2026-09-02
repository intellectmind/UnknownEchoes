package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.ClientGameEvents;
import cn.kurt6.unknown_echoes.client.ClientWorldEchoFlightCache;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** 左右翼独立绕背部根节点运动；地面收拢，飞行时动态展开，不复用原版鞘翅几何。 */
public final class WorldEchoWingsLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation SPREAD_TEXTURE =
            UnknownEchoes.id("textures/entity/world_echo_wings_spread.png");
    private static final ResourceLocation FOLDED_TEXTURE =
            UnknownEchoes.id("textures/entity/world_echo_wings_folded.png");
    private static final ResourceLocation SPREAD_EMISSIVE_TEXTURE =
            UnknownEchoes.id("textures/entity/world_echo_wings_spread_emissive.png");
    private static final ResourceLocation FOLDED_EMISSIVE_TEXTURE =
            UnknownEchoes.id("textures/entity/world_echo_wings_folded_emissive.png");

    private static final float CANVAS_WIDTH = 128.0F;
    private static final float CANVAS_HEIGHT = 64.0F;

    private static final WingPose SPREAD = new WingPose(
            SPREAD_TEXTURE, SPREAD_EMISSIVE_TEXTURE,
            1.55F, 1.60F, 0.14F, -0.18F, 0.155F, 0.11F,
            118.0F / CANVAS_WIDTH, 61.0F / CANVAS_HEIGHT);
    private static final WingPose FOLDED = new WingPose(
            FOLDED_TEXTURE, FOLDED_EMISSIVE_TEXTURE,
            0.72F, 0.80F, 0.12F, 0.02F, 0.165F, 0.045F,
            88.0F / CANVAS_WIDTH, 49.0F / CANVAS_HEIGHT);

    private static final int WING_SEGMENTS = 4;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    private final Map<AbstractClientPlayer, WingVisualState> visualStates = new WeakHashMap<>();

    public WorldEchoWingsLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!stack.is(ModItems.WORLD_ECHO_WINGS.get())) {
            return;
        }

        boolean flying = ClientWorldEchoFlightCache.isFlying(player)
                || player.getAbilities().flying
                || player.isLocalPlayer() && ClientGameEvents.isWorldEchoFlying();
        WingVisual visual = resolveVisual(player, flying, ageInTicks);
        WingPose pose = visual.pose();
        WingMotion motion = calculateMotion(player, visual.openAmount(), ageInTicks);
        poseStack.pushPose();
        poseStack.translate(0.0F, pose.top(), pose.depth());
        if (!visual.open() && player.isCrouching()) {
            poseStack.translate(0.0F, 0.16F, 0.015F);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
        }

        VertexConsumer base = ItemRenderer.getArmorFoilBuffer(buffers,
                RenderType.armorCutoutNoCull(pose.texture()), stack.hasFoil());
        renderWingPair(poseStack, base, packedLight, pose, motion);

        VertexConsumer emissive = buffers.getBuffer(RenderType.eyes(pose.emissiveTexture()));
        renderWingPair(poseStack, emissive, LightTexture.FULL_BRIGHT, pose, motion);
        poseStack.popPose();
    }

    private static void renderWingPair(PoseStack poseStack, VertexConsumer consumer,
                                       int packedLight, WingPose pose, WingMotion motion) {
        renderWingHalf(poseStack, consumer, packedLight, pose, motion, -1.0F);
        renderWingHalf(poseStack, consumer, packedLight, pose, motion, 1.0F);
    }

    private WingMotion calculateMotion(AbstractClientPlayer player,
                                       float openAmount, float ageInTicks) {
        WingVisualState state = visualStates.get(player);
        Vec3 velocity = player.getDeltaMovement();
        float targetTravel = Mth.clamp((float) Math.sqrt(
                velocity.x * velocity.x + velocity.z * velocity.z) / 0.9F, 0.0F, 1.0F);
        float targetClimb = Mth.clamp((float) velocity.y * 18.0F, -7.0F, 7.0F);
        float elapsed = Mth.clamp(ageInTicks - state.motionAge(), 0.0F, 1.5F);
        float motionSmoothing = 1.0F - (float) Math.pow(0.72D, elapsed);
        state.setTravel(Mth.lerp(motionSmoothing, state.travel(), targetTravel));
        state.setClimb(Mth.lerp(motionSmoothing, state.climb(), targetClimb));
        state.setMotionAge(ageInTicks);

        float phaseSpeed = Mth.lerp(state.travel(), 0.21F, 0.15F);
        float nextPhase = state.phase() + elapsed * phaseSpeed * openAmount;
        if (nextPhase >= TWO_PI) {
            nextPhase -= TWO_PI;
        }
        state.setPhase(nextPhase);
        float phase = state.phase();
        float beat = Mth.sin(phase);
        float beatEase = beat * (1.0F - 0.18F * Math.abs(beat));

        float idle = Mth.sin(ageInTicks * 0.09F + state.phaseOffset());
        float idleRoll = 5.0F + idle * 1.5F;
        float idleSweep = 22.0F + idle * 2.0F;
        float idlePitch = 3.0F + idle;

        float flightRoll = Mth.lerp(state.travel(), 2.0F, -2.0F)
                + beatEase * Mth.lerp(state.travel(), 10.0F, 6.5F);
        float flightSweep = Mth.lerp(state.travel(), 10.0F, 16.0F)
                + Mth.sin(phase + Mth.HALF_PI) * Mth.lerp(state.travel(), 2.5F, 1.8F);
        float flightPitch = 4.5F
                + Mth.sin(phase - 0.7F) * Mth.lerp(state.travel(), 2.2F, 1.2F)
                - state.climb();
        float blend = openAmount * openAmount * (3.0F - 2.0F * openAmount);
        return new WingMotion(
                Mth.lerp(blend, idleRoll, flightRoll),
                Mth.lerp(blend, idleSweep, flightSweep),
                Mth.lerp(blend, idlePitch, flightPitch));
    }

    private WingVisual resolveVisual(AbstractClientPlayer player, boolean flying,
                                     float ageInTicks) {
        WingVisualState state = visualStates.get(player);
        if (state == null) {
            state = new WingVisualState(flying ? 1.0F : 0.0F, flying, ageInTicks,
                    (player.getId() & 15) * 0.37F);
            visualStates.put(player, state);
        } else {
            float elapsed = Mth.clamp(ageInTicks - state.lastAge(), 0.0F, 1.5F);
            float smoothing = 1.0F - (float) Math.pow(0.78D, elapsed);
            float target = flying ? 1.0F : 0.0F;
            state.setOpenAmount(Mth.lerp(smoothing, state.openAmount(), target));
            state.setLastAge(ageInTicks);

            if (flying && state.openAmount() >= 0.75F) {
                state.setTextureOpen(true);
            } else if (!flying && state.openAmount() <= 0.25F) {
                state.setTextureOpen(false);
            }
        }

        float amount = state.openAmount();
        WingPose texturePose = state.textureOpen() ? SPREAD : FOLDED;
        WingPose pose = new WingPose(
                texturePose.texture(), texturePose.emissiveTexture(),
                Mth.lerp(amount, FOLDED.wingWidth(), SPREAD.wingWidth()),
                Mth.lerp(amount, FOLDED.height(), SPREAD.height()),
                Mth.lerp(amount, FOLDED.rootOffset(), SPREAD.rootOffset()),
                Mth.lerp(amount, FOLDED.top(), SPREAD.top()),
                Mth.lerp(amount, FOLDED.depth(), SPREAD.depth()),
                Mth.lerp(amount, FOLDED.tipCurl(), SPREAD.tipCurl()),
                texturePose.uMax(), texturePose.vMax());
        return new WingVisual(pose, state.textureOpen(), amount);
    }

    private static void renderWingHalf(PoseStack poseStack, VertexConsumer consumer,
                                       int packedLight, WingPose pose, WingMotion motion,
                                       float side) {
        poseStack.pushPose();
        poseStack.translate(side * pose.rootOffset(), 0.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side * motion.sweepDegrees()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-side * motion.rollDegrees()));
        poseStack.mulPose(Axis.XP.rotationDegrees(motion.pitchDegrees()));
        renderHalfPlane(poseStack, consumer, packedLight, pose, side);
        poseStack.popPose();
    }

    private static void renderHalfPlane(PoseStack poseStack, VertexConsumer consumer,
                                        int packedLight, WingPose pose, float side) {
        PoseStack.Pose last = poseStack.last();
        float height = pose.height();
        float tip = side * pose.wingWidth();
        float rootU = pose.uMax() * 0.5F;
        float tipU = side < 0.0F ? 0.0F : pose.uMax();
        for (int segment = 0; segment < WING_SEGMENTS; segment++) {
            float start = (float) segment / WING_SEGMENTS;
            float end = (float) (segment + 1) / WING_SEGMENTS;
            float x0 = tip * start;
            float x1 = tip * end;
            float z0 = pose.tipCurl() * start * start;
            float z1 = pose.tipCurl() * end * end;
            float u0 = Mth.lerp(start, rootU, tipU);
            float u1 = Mth.lerp(end, rootU, tipU);
            addVertex(consumer, last, x0, 0.0F, z0, u0, 0.0F, packedLight);
            addVertex(consumer, last, x0, height, z0, u0, pose.vMax(), packedLight);
            addVertex(consumer, last, x1, height, z1, u1, pose.vMax(), packedLight);
            addVertex(consumer, last, x1, 0.0F, z1, u1, 0.0F, packedLight);
        }
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                  float x, float y, float z, float u, float v,
                                  int packedLight) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    private record WingPose(ResourceLocation texture, ResourceLocation emissiveTexture,
                            float wingWidth, float height, float rootOffset,
                            float top, float depth, float tipCurl,
                            float uMax, float vMax) {
    }

    private record WingMotion(float rollDegrees, float sweepDegrees, float pitchDegrees) {
    }

    private record WingVisual(WingPose pose, boolean open, float openAmount) {
    }

    private static final class WingVisualState {
        private float openAmount;
        private boolean textureOpen;
        private float lastAge;
        private final float phaseOffset;
        private float phase;
        private float motionAge;
        private float travel;
        private float climb;

        private WingVisualState(float openAmount, boolean textureOpen, float lastAge,
                                float phaseOffset) {
            this.openAmount = openAmount;
            this.textureOpen = textureOpen;
            this.lastAge = lastAge;
            this.phaseOffset = phaseOffset;
            this.phase = phaseOffset;
            this.motionAge = lastAge;
        }

        private float openAmount() {
            return openAmount;
        }

        private void setOpenAmount(float openAmount) {
            this.openAmount = openAmount;
        }

        private boolean textureOpen() {
            return textureOpen;
        }

        private void setTextureOpen(boolean textureOpen) {
            this.textureOpen = textureOpen;
        }

        private float lastAge() {
            return lastAge;
        }

        private void setLastAge(float lastAge) {
            this.lastAge = lastAge;
        }

        private float phaseOffset() {
            return phaseOffset;
        }

        private float phase() {
            return phase;
        }

        private void setPhase(float phase) {
            this.phase = phase;
        }

        private float motionAge() {
            return motionAge;
        }

        private void setMotionAge(float motionAge) {
            this.motionAge = motionAge;
        }

        private float travel() {
            return travel;
        }

        private void setTravel(float travel) {
            this.travel = travel;
        }

        private float climb() {
            return climb;
        }

        private void setClimb(float climb) {
            this.climb = climb;
        }
    }
}
