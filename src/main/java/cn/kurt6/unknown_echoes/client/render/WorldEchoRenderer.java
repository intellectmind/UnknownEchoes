package cn.kurt6.unknown_echoes.client.render;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import cn.kurt6.unknown_echoes.client.model.LimitedHeadGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 世界回响体基础渲染：独立骨架 + 发光层（核心/晶体/符文自发光），战斗粒子区分最终形态。 */
public class WorldEchoRenderer extends GeoEntityRenderer<WorldEcho> {
    private static final float MODEL_SCALE = 1.18F;
    /**
     * 本体最低面（robe_lower 袍摆）在骨架里的静止高度，单位 px。
     * 抬升必须锚在本体而不是 fx_shockwave：锚在 fx 上会让常驻悬空变成 1.06 格而非 0.65 格。
     * fx_shockwave 自己贴地由它在骨架里的相对位置负责，不该反过来决定本体高度。
     */
    private static final double BODY_REST_Y = -25.0528D;
    /** 基础抬升量由 MODEL_SCALE 反解,不能手填:只改 scale 会让袍摆浮空或埋进地里。 */
    private static final double BODY_ANCHOR = -BODY_REST_Y * MODEL_SCALE / 16.0D;
    /** 常驻悬空高度(格):这只 Boss 半飞,脚不踩地。待机时 fx_shockwave 缩成贴身法阵环。 */
    private static final double IDLE_HOVER_BLOCKS = 0.65D;
    private static final double MODEL_Y_OFFSET = BODY_ANCHOR + IDLE_HOVER_BLOCKS;
    /** 出招时在常驻悬空之上再抬的高度：让身下的攻击特效与建模细节露出来。 */
    private static final double ATTACK_HOVER_BLOCKS = 0.55D;
    /** 悬空的淡入淡出时长（tick），避免起手瞬间瞬移。 */
    private static final float HOVER_FADE_TICKS = 6.0F;

    public WorldEchoRenderer(EntityRendererProvider.Context context) {
        super(context, new LimitedHeadGeoModel<>(UnknownEchoes.id("world_echo"), "head", 12.0F, -3.0F, 4.0F));
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 4.64F;
        addRenderLayer(new EchoGlowLayer<>(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/world_echo_glowing.png")));
        addRenderLayer(new WorldEchoAttackLayer(this,
                ResourceLocation.fromNamespaceAndPath(
                        UnknownEchoes.MODID, "textures/entity/world_echo_attack.png")));
    }

    @Override
    public void preRender(PoseStack poseStack, WorldEcho animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {

        if (!isReRender) {
            poseStack.translate(0.0D, MODEL_Y_OFFSET + attackHover(animatable, partialTick), 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    /** 出招期间额外抬升(格)。两端各留 HOVER_FADE_TICKS 淡入淡出,否则起手收招会瞬移。 */
    private static double attackHover(WorldEcho worldEcho, float partialTick) {
        int duration = worldEcho.getSyncedAttackDuration();
        if (worldEcho.getSyncedAttackMode() == WorldEcho.AttackMode.NONE || duration <= 0) {
            return 0.0D;
        }
        float age = Math.min(duration, worldEcho.getSyncedAttackAge() + partialTick);
        float fade = Math.min(HOVER_FADE_TICKS, duration * 0.5F);
        float weight = fade <= 0.0F ? 1.0F
                : Math.min(1.0F, Math.min(age, duration - age) / fade);
        return ATTACK_HOVER_BLOCKS * Math.max(0.0F, weight);
    }
}
