package cn.kurt6.unknown_echoes.client;

import static cn.kurt6.unknown_echoes.client.CombatFxVertices.quad;
import static cn.kurt6.unknown_echoes.client.CombatFxVertices.vertex;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.fx.CombatFxGeometry;
import cn.kurt6.unknown_echoes.fx.BossLifecycleGeometry;
import cn.kurt6.unknown_echoes.fx.BossLifecycleIdentity;
import cn.kurt6.unknown_echoes.fx.CombatFxAccentGeometry;
import cn.kurt6.unknown_echoes.fx.CombatFxBudget;
import cn.kurt6.unknown_echoes.fx.CombatFxOrnamentGeometry;
import cn.kurt6.unknown_echoes.fx.ElementalLayerGeometry;
import cn.kurt6.unknown_echoes.fx.RealmDisasterFx;
import cn.kurt6.unknown_echoes.fx.StormVolumeGeometry;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientCombatFx {
    private static final List<Effect> ACTIVE = new ArrayList<>();
    private static ClientLevel activeLevel;
    private static long admissionTick = Long.MIN_VALUE;
    private static int admissions;
    private static final Materials PARTICLE_MATERIALS = materials(false);
    private static final Materials LATE_MATERIALS = materials(true);

    private record Materials(EnumMap<CombatFxGeometry.Material, RenderType> ribbons,
                             EnumMap<CombatFxGeometry.FacetMaterial, RenderType> facets,
                             EnumMap<CombatFxAccentGeometry.Material, RenderType> accents,
                             RenderType membrane) { }

    private static Materials materials(boolean late) {
        var ribbons = new EnumMap<CombatFxGeometry.Material, RenderType>(CombatFxGeometry.Material.class);
        for (var material : CombatFxGeometry.Material.values()) {
            ribbons.put(material, materialType(material.name().toLowerCase(java.util.Locale.ROOT), material.texture(), true, late));
        }
        var facets = new EnumMap<CombatFxGeometry.FacetMaterial, RenderType>(CombatFxGeometry.FacetMaterial.class);
        for (var material : CombatFxGeometry.FacetMaterial.values()) {
            facets.put(material, materialType(material.name().toLowerCase(java.util.Locale.ROOT),
                    material.texture(), material.additive(), late));
        }
        var accents = new EnumMap<CombatFxAccentGeometry.Material, RenderType>(CombatFxAccentGeometry.Material.class);
        for (var material : CombatFxAccentGeometry.Material.values()) {
            accents.put(material, materialType(material.name().toLowerCase(java.util.Locale.ROOT), material.texture(), true, late));
        }
        return new Materials(ribbons, facets, accents,
                materialType("spatial_membrane", "textures/effect/spatial_membrane.png", false, late));
    }
    // 独占缓冲，不能在写入过程中切换到其他共享 RenderType。
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(262144));

    private ClientCombatFx() { }

    private static RenderType materialType(String name, String texture, boolean additive, boolean late) {
        return RenderType.create("unknown_echoes_fx_" + name + (late ? "_late" : "_particles"),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(UnknownEchoes.id(texture), true, false))
                        .setTransparencyState(additive ? RenderStateShard.LIGHTNING_TRANSPARENCY : RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setOutputState(late ? RenderStateShard.MAIN_TARGET : RenderStateShard.PARTICLES_TARGET)
                        .createCompositeState(false));
    }

    public static void accept(CombatFxPayload payload) {
        acceptFrame(new ClientFxFrame(payload, null), null);
    }

    public static void acceptWhile(CombatFxPayload payload, java.util.function.BooleanSupplier active) {
        acceptFrame(new ClientFxFrame(payload, null, active), null);
    }

    public static void acceptFollowing(UUID owner, CombatFxPayload payload, java.util.function.DoubleFunction<CombatFxPayload> following) {
        acceptFrame(new ClientFxFrame(payload, following), new FollowingKey(owner, payload.shape()));
    }

    private record FollowingKey(UUID owner, CombatFxPayload.Shape shape) { }

    private static void acceptFrame(ClientFxFrame frame, FollowingKey key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != activeLevel) {
            clear();
            activeLevel = mc.level;
        }
        CombatFxPayload payload = frame.at(1);
        if (mc.level == null || mc.player == null || payload == null || !enabled(payload)) {
            return;
        }
        if (payload.shape() == CombatFxPayload.Shape.DEBRIS) {
            ClientTerrainDebris.accept(payload);
            return;
        }
        long now = mc.level.getGameTime();
        if (now != admissionTick) {
            admissionTick = now;
            admissions = 0;
        }
        Vec3 center = origin(payload, 1);
        double extent = Math.max(payload.radius(), payload.length());
        double range = RealmDisasterFx.isLightning(payload) || RealmDisasterFx.isWind(payload)
                ? StormVolumeGeometry.VIEW_DISTANCE : 64;
        if (center == null || center.distanceToSqr(mc.player.position()) > Math.pow(range + extent, 2)) {
            return;
        }
        if (key != null) {
            for (int i = 0; i < ACTIVE.size(); i++) {
                if (key.equals(ACTIVE.get(i).key)) {
                    ACTIVE.remove(i);
                    ACTIVE.add(new Effect(frame, key));
                    return;
                }
            }
        }
        // 持续蓄力可以刷新同一条带，不能每个网络帧再叠一份几何。
        for (int i = 0; i < ACTIVE.size(); i++) {
            Effect effect = ACTIVE.get(i);
            CombatFxPayload old = effect.payload;
            if (frame.refreshable() && effect.frame.refreshable() && !CombatFxGeometry.transientShape(payload)
                    && old.shape() == payload.shape() && old.core() == payload.core() && old.edge() == payload.edge()
                    && old.boss() == payload.boss() && old.entityId() == payload.entityId()
                    && old.origin().distanceToSqr(payload.origin()) < 0.01
                    && old.direction().distanceToSqr(payload.direction()) < 0.01
                    && Math.abs(old.radius() - payload.radius()) < 0.01
                    && Math.abs(old.length() - payload.length()) < 0.01
                    && Math.abs(old.span() - payload.span()) < 0.01 && effect.age < 5) {
                ACTIVE.remove(i);
                ACTIVE.add(new Effect(frame, key));
                return;
            }
        }
        CombatFxBudget budget = CombatFxBudget.forSetting(mc.options.particles().get().getId());
        if (admissions >= budget.admissions()) {
            return;
        }
        if (ACTIVE.size() >= budget.effects()) {
            // 命中和释放不能被持续装饰挤掉；只替换最早的非瞬发演出。
            if (key != null || !CombatFxGeometry.transientShape(payload)) return;
            int replace = -1;
            for (int i = 0; i < ACTIVE.size(); i++) {
                if (!CombatFxGeometry.transientShape(ACTIVE.get(i).payload)) { replace = i; break; }
            }
            if (replace < 0) return;
            ACTIVE.remove(replace);
        }
        admissions++;
        ACTIVE.add(new Effect(frame, key));
    }

    private static boolean enabled(CombatFxPayload p) {
        return p.boss() ? ClientConfig.ENABLE_BOSS_PARTICLES.get()
                && ClientConfig.BOSS_PARTICLE_DENSITY.get() > 0 : ClientConfig.ENABLE_ECHO_PARTICLES.get();
    }

    private static Vec3 origin(CombatFxPayload p, float partialTick) {
        if (p.entityId() < 0) {
            return p.origin();
        }
        Entity entity = activeLevel == null ? null : activeLevel.getEntity(p.entityId());
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return null;
        }
        return entity == null || !entity.isAlive() || entity.isInvisible()
                || entity instanceof Player player && ClientEquipmentFx.quietWingFlight(player) ? null
                : entity.getPosition(partialTick).add(p.origin());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != activeLevel) {
            clear();
            activeLevel = mc.level;
        }
        if (!mc.isPaused() && mc.level != null && mc.level.tickRateManager().runsNormally()) {
            ACTIVE.removeIf(effect -> ++effect.age >= effect.payload.duration() || !enabled(effect.payload) || effect.frame.at(1) == null);
            int limit = CombatFxBudget.forSetting(mc.options.particles().get().getId()).effects();
            if (ACTIVE.size() > limit) ACTIVE.subList(0, ACTIVE.size() - limit).clear();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        ACTIVE.clear();
        activeLevel = null;
        admissionTick = Long.MIN_VALUE;
        admissions = 0;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        boolean late = event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL;
        if ((!late && event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) || ACTIVE.isEmpty()
                || Minecraft.getInstance().level != activeLevel) {
            return;
        }
        var pass = ClientShaderCompat.pass();
        if (pass == ClientShaderCompat.Pass.SKIP || late != (pass == ClientShaderCompat.Pass.LATE)) {
            return;
        }
        var modelView = RenderSystem.getModelViewStack();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        float[] color = RenderSystem.getShaderColor().clone();
        modelView.pushMatrix();
        try {
            // AFTER_LEVEL 已弹出世界矩阵；两条路径都显式设置相机变换，不能再乘一遍。
            modelView.set(event.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), sorting);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (late) {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            }
            renderEffects(event, late ? LATE_MATERIALS : PARTICLE_MATERIALS);
        } finally {
            BUFFERS.endBatch();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        }
    }

    private static void renderEffects(RenderLevelStageEvent event, Materials materials) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f matrix = new Matrix4f();
        List<DrawEffect> visible = new ArrayList<>();
        CombatFxBudget budget = CombatFxBudget.forSetting(Minecraft.getInstance().options.particles().get().getId());
        int remainingSegments = budget.segments();
        for (int index = ACTIVE.size() - 1; index >= 0; index--) {
            Effect effect = ACTIVE.get(index);
            CombatFxPayload p = effect.frame.at(partial);
            if (p == null) continue;
            Vec3 center = origin(p, partial);
            if (center == null || !enabled(p)) {
                continue;
            }
            double extent = Math.max(p.radius() * 1.6, p.length() * 1.1) + 2;
            if (!event.getFrustum().isVisible(new AABB(center, center).inflate(extent))) {
                continue;
            }
            double age = effect.age + partial;
            float alpha = effect.frame.opacity(age, activeLevel.getGameTime() + partial);
            if (p.boss()) {
                alpha *= ClientConfig.BOSS_PARTICLE_DENSITY.get().floatValue();
            }
            if (alpha <= .001F) {
                continue;
            }
            int segments = CombatFxGeometry.segments(p);
            int strands = Math.min(CombatFxGeometry.strands(p), remainingSegments / segments);
            if (strands > 0) {
                remainingSegments -= segments * strands;
                visible.add(new DrawEffect(p, center, age, alpha, strands));
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        renderSurfaces(visible, camera, matrix, materials);
        renderAccents(visible, camera, matrix, materials, budget);
        // 预算按新近演出分配，材质分组不能让排在后面的类型长期饿死。
        for (var entry : materials.ribbons.entrySet()) {
            VertexConsumer buffer = null;
            for (DrawEffect effect : visible) {
                if (CombatFxGeometry.material(effect.payload) != entry.getKey()) {
                    continue;
                }
                if (buffer == null) {
                    buffer = BUFFERS.getBuffer(entry.getValue());
                }
                for (int strand = 0; strand < effect.strands; strand++) {
                    float strandAlpha = effect.alpha * BossLifecycleGeometry.strandOpacity(effect.payload, strand, effect.age)
                            * CombatFxOrnamentGeometry.opacity(effect.payload, strand, effect.age)
                            * ElementalLayerGeometry.opacity(effect.payload, strand, effect.age);
                    if (strandAlpha > .001F) {
                        drawStrand(buffer, matrix, camera, effect.center, effect.payload, strand, effect.age, strandAlpha);
                    }
                }
            }
            if (buffer != null) {
                BUFFERS.endBatch(entry.getValue());
            }
        }
    }

    private record DrawEffect(CombatFxPayload payload, Vec3 center, double age, float alpha, int strands) { }

    private record Surface(Vec3[] vertices, Vec3 center, RenderType material, int color, float alpha,
                           float u0, float u1) { }

    private static void renderSurfaces(List<DrawEffect> effects, Vec3 camera, Matrix4f matrix, Materials materials) {
        int remaining = CombatFxBudget.forSetting(Minecraft.getInstance().options.particles().get().getId()).facets();
        int membranes = 0;
        List<Surface> surfaces = new ArrayList<>();
        for (DrawEffect effect : effects) {
            CombatFxPayload p = effect.payload;
            int count = CombatFxGeometry.facets(p);
            if (count > 0 && count <= remaining) {
                remaining -= count;
                var material = CombatFxGeometry.facetMaterial(p);
                boolean crystal = material == CombatFxGeometry.FacetMaterial.CRYSTAL;
                double time = CombatFxGeometry.motionTime(p, effect.age, activeLevel.getGameTime() + effect.age % 1);
                for (int i = 0; i < count; i++) {
                    Vec3[] vertices = CombatFxGeometry.facet(p, i, time);
                    for (int v = 0; v < vertices.length; v++) {
                        vertices[v] = vertices[v].add(effect.center);
                    }
                    addSurface(surfaces, vertices, materials.facets.get(material),
                            crystal && i % 3 == 0 ? brightCore(BossLifecycleIdentity.core(p, i)) : BossLifecycleIdentity.core(p, i),
                            effect.alpha * CombatFxGeometry.facetOpacity(p, i, effect.age),
                            CombatFxGeometry.facetU(p, i, false), CombatFxGeometry.facetU(p, i, true));
                }
            }
            if (membranes < 12 && (p.shape() == CombatFxPayload.Shape.COLLAPSE
                    || p.shape() == CombatFxPayload.Shape.ARRIVAL || p.shape() == CombatFxPayload.Shape.SINGULARITY)) {
                membranes++;
                boolean singularity = p.shape() == CombatFxPayload.Shape.SINGULARITY;
                var frame = CombatFxGeometry.billboardFrame(camera.subtract(effect.center));
                for (int layer = singularity ? 0 : 2; layer >= 0; layer--) {
                    Vec3 size = CombatFxGeometry.membraneHalfSize(p, effect.age, layer);
                    Vec3 x = frame.right().scale(size.x);
                    Vec3 y = frame.up().scale(size.y);
                    Vec3 offset = effect.center.add(frame.normal().scale(layer * -0.06));
                    addSurface(surfaces, new Vec3[]{offset.subtract(x).add(y), offset.subtract(x).subtract(y),
                                    offset.add(x).subtract(y), offset.add(x).add(y)},
                            materials.membrane, p.core(), effect.alpha * (singularity ? .9F : .52F) / (layer + 1), 0, 1);
                }
            }
        }
        // 不透明度混合依赖顺序，跨材质也按远到近提交；亮芯最后叠加。
        surfaces.sort(Comparator.comparingDouble((Surface surface) -> surface.center.distanceToSqr(camera)).reversed());
        for (Surface surface : surfaces) {
            float alpha = surface.alpha * CombatFxGeometry.detailVisibility(surface.center.distanceTo(camera));
            if (alpha <= .001F) {
                continue;
            }
            VertexConsumer buffer = BUFFERS.getBuffer(surface.material);
            for (int v = 0; v < 4; v++) {
                vertex(buffer, matrix, surface.vertices[v].subtract(camera),
                        v < 2 ? surface.u0 : surface.u1, v == 0 || v == 3 ? 0 : 1, surface.color, alpha);
            }
        }
        BUFFERS.endBatch();
    }

    private static void renderAccents(List<DrawEffect> effects, Vec3 camera, Matrix4f matrix,
                                      Materials materials, CombatFxBudget budget) {
        int remaining = budget.accents();
        var batches = new EnumMap<CombatFxAccentGeometry.Material, List<Surface>>(CombatFxAccentGeometry.Material.class);
        for (DrawEffect effect : effects) {
            CombatFxPayload p = effect.payload;
            double distance = Math.max(0, camera.distanceTo(effect.center) - Math.max(p.radius(), p.length()));
            int total = CombatFxAccentGeometry.count(p);
            int count = Math.min(remaining, budget.accentsFor(total, distance,
                    p.boss() ? ClientConfig.BOSS_PARTICLE_DENSITY.get() : 1));
            remaining -= count;
            for (int i = 0; i < count; i++) {
                int index = i * total / count;
                var sprite = CombatFxAccentGeometry.sprite(p, index, effect.age, activeLevel.getGameTime() + effect.age % 1);
                float alpha = effect.alpha * sprite.alpha()
                        * CombatFxGeometry.detailVisibility(camera.distanceTo(effect.center.add(sprite.center())));
                if (alpha <= .002F || sprite.halfLength() <= .001) continue;
                Vec3[] quad = CombatFxAccentGeometry.quad(sprite, camera.subtract(effect.center));
                for (int v = 0; v < quad.length; v++) quad[v] = quad[v].add(effect.center);
                int color = CombatFxGeometry.mixColor(BossLifecycleIdentity.edge(p, index),
                        brightCore(BossLifecycleIdentity.core(p, index)), sprite.tint() + .35F);
                var surfaces = batches.computeIfAbsent(sprite.material(), ignored -> new ArrayList<>());
                addSurface(surfaces, quad, materials.accents.get(sprite.material()), color, alpha, 0, 1);
            }
            if (remaining == 0) break;
        }
        for (var entry : batches.entrySet()) {
            RenderType type = materials.accents.get(entry.getKey());
            VertexConsumer buffer = BUFFERS.getBuffer(type);
            for (Surface surface : entry.getValue()) {
                for (int v = 0; v < 4; v++) {
                    vertex(buffer, matrix, surface.vertices[v].subtract(camera), v < 2 ? 0 : 1,
                            v == 0 || v == 3 ? 0 : 1, surface.color, surface.alpha);
                }
            }
            BUFFERS.endBatch(type);
        }
    }

    private static void addSurface(List<Surface> surfaces, Vec3[] vertices, RenderType material, int color,
                                   float alpha, float u0, float u1) {
        Vec3 center = vertices[0].add(vertices[1]).add(vertices[2]).add(vertices[3]).scale(.25);
        surfaces.add(new Surface(vertices, center, material, color, alpha, u0, u1));
    }

    private static void drawStrand(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center,
                                   CombatFxPayload p, int strand, double age, float alpha) {
        int segments = CombatFxGeometry.segments(p);
        double clock = activeLevel.getGameTime() + age % 1;
        double motionAge = CombatFxGeometry.motionTime(p, age, clock);
        var samples = CombatFxGeometry.ribbon(p, strand, motionAge, camera.subtract(center));
        int core = BossLifecycleIdentity.core(p, strand);
        int edge = BossLifecycleIdentity.edge(p, strand);
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            float stroke = CombatFxGeometry.strokeOpacity(p, (t0 + t1) * .5, age);
            if (stroke <= 0) {
                continue;
            }
            Vec3 a = center.add(samples[i].point());
            Vec3 b = center.add(samples[i + 1].point());
            Vec3 sideA = samples[i].side();
            Vec3 sideB = samples[i + 1].side();
            double envelope = CombatFxGeometry.widthEnvelope(p, age);
            double w0 = samples[i].width() * envelope;
            double w1 = samples[i + 1].width() * envelope;
            float u0 = (float) (t0 * 3 - clock * 0.025);
            float u1 = (float) (t1 * 3 - clock * 0.025);
            float visibility = CombatFxGeometry.detailVisibility(a.add(b).scale(.5).distanceTo(camera));
            quad(buffer, matrix, camera, a, b, sideA, sideB, w0 * 3.2, w1 * 3.2,
                    u0, u1, haloColor(core, edge),
                    alpha * stroke * .48F * (.2F + .8F * visibility));
            int body = CombatFxGeometry.mixColor(edge, core,
                    (float) (.55 + .45 * Math.sin(Math.PI * t0)));
            quad(buffer, matrix, camera, a, b, sideA, sideB, w0, w1, u0, u1, body,
                    alpha * stroke * .95F * (.6F + .4F * visibility));
            quad(buffer, matrix, camera, a, b, sideA, sideB, w0 * 0.14, w1 * 0.14,
                    u0, u1, brightCore(core), alpha * stroke);
        }
    }

    private static int brightCore(int color) {
        int r = ((color >> 16) & 255), g = ((color >> 8) & 255), b = color & 255;
        return ((r + (255 - r) / 2) << 16) | ((g + (255 - g) / 2) << 8) | (b + (255 - b) / 2);
    }

    private static int haloColor(int core, int edge) {
        return ((core & 0xFEFEFE) >> 1) + ((edge & 0xFEFEFE) >> 1);
    }

    private static final class Effect {
        private final CombatFxPayload payload;
        private final ClientFxFrame frame;
        private final FollowingKey key;
        private int age;

        private Effect(ClientFxFrame frame, FollowingKey key) {
            this.frame = frame;
            this.key = key;
            this.payload = frame.initial();
        }
    }
}
