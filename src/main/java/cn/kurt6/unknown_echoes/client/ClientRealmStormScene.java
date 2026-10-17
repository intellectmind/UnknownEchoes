package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.fx.StormVolumeGeometry;
import cn.kurt6.unknown_echoes.fx.StormRockFrames;
import cn.kurt6.unknown_echoes.fx.RealmDisasterFx;
import cn.kurt6.unknown_echoes.fx.CombatFxGeometry;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.fx.DisasterAtmosphereProfile;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientRealmStormScene {
    private static final StormSceneState STATE = new StormSceneState();
    private static final List<StormCloud> CLOUDS = new ArrayList<>();
    private static final Map<StormSceneState.Key, Visual> VISUALS = new HashMap<>();
    private static final Map<StormSceneState.Key, StormSound> SOUNDS = new HashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(131072));
    private static ClientLevel level;
    private static List<StormSceneState.Scene> visible = List.of();
    private static double cloudCredit;

    private ClientRealmStormScene() { }

    public static void accept(RealmDisasterPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level != null && level.dimension().location().equals(payload.dimension())) STATE.accept(payload);
    }

    private static void ensureLevel(ClientLevel current) {
        if (level == current) return;
        CLOUDS.forEach(StormCloud::remove);
        CLOUDS.clear();
        ClientStormWindParticles.clear();
        SOUNDS.values().forEach(StormSound::stopNow);
        SOUNDS.clear();
        VISUALS.clear();
        STATE.clear();
        visible = List.of();
        cloudCredit = 0;
        level = current;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ensureLevel(null); }

    static double density(Minecraft mc) {
        if (!ClientConfig.ENABLE_ECHO_PARTICLES.get()) return 0;
        double setting = mc.options.particles().get() == ParticleStatus.MINIMAL ? 0.25
                : mc.options.particles().get() == ParticleStatus.DECREASED ? 0.55 : 1;
        return setting * ClientConfig.DISASTER_VISUAL_DENSITY.get();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level == null || mc.player == null || mc.isPaused() || !level.tickRateManager().runsNormally()) return;
        STATE.tick();
        visible = STATE.visible(mc.player.position());
        List<StormSceneState.Key> keys = visible.stream().map(StormSceneState.Scene::key).toList();
        VISUALS.keySet().removeIf(key -> !keys.contains(key));
        SOUNDS.entrySet().removeIf(entry -> {
            boolean playing = STATE.clock() % 20 != 0 || mc.getSoundManager().isActive(entry.getValue());
            if (keys.contains(entry.getKey()) && ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS.get()
                    && ClientConfig.DISASTER_SOUND_VOLUME.get() > 0 && playing) return false;
            entry.getValue().stopNow();
            return true;
        });
        CLOUDS.removeIf(cloud -> {
            if (cloud.isAlive() && STATE.clock() - cloud.born < StormVolumeGeometry.CLOUD_LIFETIME) return false;
            cloud.remove();
            return true;
        });
        double density = density(mc);
        ClientStormWindParticles.tick(mc, visible, density);
        if (density == 0) {
            CLOUDS.forEach(StormCloud::remove);
            CLOUDS.clear();
        }
        double strength = visible.stream().mapToDouble(scene -> scene.visualStrength(1, mc.player.position())).max().orElse(0);
        cloudCredit = Math.min(StormVolumeGeometry.CLOUDS_PER_TICK, cloudCredit + StormVolumeGeometry.CLOUDS_PER_TICK * density * strength);
        int spawn = Math.min((int) cloudCredit, StormVolumeGeometry.MAX_LIVE_CLOUDS - CLOUDS.size());
        cloudCredit -= spawn;
        for (StormSceneState.Scene scene : visible) {
            if (density > 0) VISUALS.computeIfAbsent(scene.key(), ignored -> new Visual(material(scene.payload())))
                    .advance(scene.payload(), density);
            if (density > 0 && scene.visualStrength(1, mc.player.position()) > 0.5F
                    && StormVolumeGeometry.lightningAt(STATE.clock(), scene.payload().deep())) {
                var p = scene.payload();
                for (var arc : RealmDisasterFx.skyArc(p.center(), p.sceneHeight(), p.deep(), STATE.clock())) {
                    if (clearCue(arc)) acceptSceneCue(scene, arc);
                }
            }
            int windInterval = density >= .75 ? 12 : density >= .4 ? 20 : 30;
            if (density > 0 && scene.visualStrength(1, mc.player.position()) > .15F
                    && scene.payload().center().distanceToSqr(mc.player.position()) < 128 * 128
                    && STATE.clock() % windInterval == 0) {
                var p = scene.payload();
                for (var wind : RealmDisasterFx.windStreams(p.center(), p.radius(), p.sceneHeight(), p.deep(), STATE.clock())) {
                    if (clearCue(wind)) acceptSceneCue(scene, wind);
                }
            }
            if (ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS.get() && ClientConfig.DISASTER_SOUND_VOLUME.get() > 0) {
                StormSound sound = SOUNDS.computeIfAbsent(scene.key(), ignored -> {
                    StormSound created = new StormSound(scene);
                    mc.getSoundManager().play(created);
                    return created;
                });
                sound.scene = scene;
            }
        }
        if (visible.isEmpty() || spawn == 0) return;
        var texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if (!(texture instanceof TextureAtlas atlas)) return;
        for (int i = 0; i < spawn; i++) {
            StormSceneState.Scene scene = visible.get((i + (int) (STATE.clock() % visible.size())) % visible.size());
            if (scene.strength(1) < 0.01F) continue;
            StormCloud cloud = new StormCloud(scene, STATE.clock(), StormVolumeGeometry.cloudIndex(STATE.clock(), i), atlas);
            if (!airAt(cloud.position(), 0.5)) continue;
            CLOUDS.add(cloud);
            mc.particleEngine.add(cloud);
        }
    }

    private static boolean airAt(Vec3 point, double half) {
        BlockPos pos = BlockPos.containing(point);
        return level != null && level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                && !level.getBlockCollisions(null, AABB.ofSize(point, half * 2, half * 2, half * 2)).iterator().hasNext();
    }

    private static void acceptSceneCue(StormSceneState.Scene scene, CombatFxPayload cue) {
        ClientLevel owner = level;
        ClientCombatFx.acceptWhile(cue, () -> level == owner && Minecraft.getInstance().level == owner
                && density(Minecraft.getInstance()) > 0 && visible.contains(scene) && scene.strength(1) > .01F);
    }

    private static boolean clearCue(CombatFxPayload cue) {
        // 检查折线自身及其后续相位，不能只检查闪电两端之间的直线。
        int stride = RealmDisasterFx.isWind(cue) ? 5 : 3;
        for (int age = 0; age <= cue.duration(); age += stride) {
            for (int strand = 0; strand < CombatFxGeometry.strands(cue); strand++) {
                Vec3 previous = cue.origin().add(CombatFxGeometry.point(cue, strand, 0, age));
                for (int step = 1; step <= 32; step++) {
                    Vec3 point = cue.origin().add(CombatFxGeometry.point(cue, strand, step / 32.0, age));
                    if (!level.hasChunksAt(BlockPos.containing(Math.min(previous.x, point.x), Math.min(previous.y, point.y), Math.min(previous.z, point.z)),
                            BlockPos.containing(Math.max(previous.x, point.x), Math.max(previous.y, point.y), Math.max(previous.z, point.z)))
                            || level.clip(new ClipContext(previous, point, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, CollisionContext.empty())).getType() != HitResult.Type.MISS) return false;
                    previous = point;
                }
            }
        }
        return true;
    }

    private record Material(BlockState state, int light) { }

    private static final class Visual {
        private final Material material;
        private final StormRockFrames frames = new StormRockFrames();
        private final boolean[] exposed = new boolean[StormVolumeGeometry.ROCKS_PER_STORM];

        private Visual(Material material) { this.material = material; }

        private void advance(RealmDisasterPayload p, double density) {
            int count = (int) Math.ceil(StormVolumeGeometry.ROCKS_PER_STORM * density);
            frames.advance(p.center(), p.sceneHeight(), p.deep(), STATE.clock(), count);
            for (int i = 0; i < frames.size(); i++) {
                BlockPos pos = BlockPos.containing(frames.current(i).position());
                exposed[i] = level.hasChunkAt(pos) && level.getBlockState(pos).isAir();
            }
        }
    }

    private static Material material(RealmDisasterPayload payload) {
        BlockPos ground = BlockPos.containing(payload.center()).below();
        if (level.hasChunkAt(ground)) {
            BlockState state = level.getBlockState(ground);
            if (state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity()
                    && state.is(TagKey.create(Registries.BLOCK, UnknownEchoes.id("disaster_breakable")))
                    && state.isSolidRender(level, ground) && state.getFluidState().isEmpty()) {
                return new Material(state, LevelRenderer.getLightColor(level, ground.above()));
            }
        }
        return new Material((payload.deep() ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState(), 0x600060);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || level != mc.level
                || visible.isEmpty() || density(mc) == 0 || ClientShaderCompat.pass() == ClientShaderCompat.Pass.SKIP) return;
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        for (StormSceneState.Scene scene : visible) {
            RealmDisasterPayload p = scene.payload();
            Visual visual = VISUALS.get(scene.key());
            float strength = scene.visualStrength(partial, camera);
            if (visual == null || strength < 0.01F) continue;
            for (int i = 0; i < visual.frames.size(); i++) {
                if (!visual.exposed[i] || visual.frames.wraps(i)) continue;
                var before = visual.frames.previous(i);
                var rock = visual.frames.current(i);
                float size = Mth.lerp(partial, before.size(), rock.size()) * strength;
                double x = Mth.lerp(partial, before.position().x, rock.position().x);
                double y = Mth.lerp(partial, before.position().y, rock.position().y);
                double z = Mth.lerp(partial, before.position().z, rock.position().z);
                if (size < 0.025F || !event.getFrustum().isVisible(new AABB(x - 3, y - 3, z - 3, x + 3, y + 3, z + 3))) continue;
                pose.pushPose();
                pose.translate(x - camera.x, y - camera.y, z - camera.z);
                pose.mulPose(Axis.XP.rotation((float) Mth.lerp(partial, before.rotation().x, rock.rotation().x)));
                pose.mulPose(Axis.YP.rotation((float) Mth.lerp(partial, before.rotation().y, rock.rotation().y)));
                pose.mulPose(Axis.ZP.rotation((float) Mth.lerp(partial, before.rotation().z, rock.rotation().z)));
                pose.scale(size, size * (p.deep() ? 0.3F : 0.7F), size * 0.85F);
                pose.translate(-0.5, -0.5, -0.5);
                try {
                    mc.getBlockRenderer().renderSingleBlock(visual.material.state(), pose, BUFFERS, visual.material.light(), OverlayTexture.NO_OVERLAY);
                } finally {
                    pose.popPose();
                }
            }
        }
        BUFFERS.endBatch();
    }

    private static final class StormCloud extends TextureSheetParticle {
        private final StormSceneState.Scene scene;
        private final StormVolumeGeometry.Cloud cloud;
        private final Vec3 center;
        private final float height;
        private final boolean deep;
        private final long born;

        private StormCloud(StormSceneState.Scene scene, long tick, int index, TextureAtlas atlas) {
            super(ClientRealmStormScene.level, scene.payload().center().x, scene.payload().center().y, scene.payload().center().z);
            this.scene = scene;
            this.born = tick;
            this.center = scene.payload().center();
            this.height = scene.payload().sceneHeight();
            this.deep = scene.payload().deep();
            this.cloud = StormVolumeGeometry.cloud(center, height, deep, tick, index);
            setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_" + (index % 4 + 3))));
            setPos(cloud.position().x, cloud.position().y, cloud.position().z);
            xo = x; yo = y; zo = z;
            lifetime = StormVolumeGeometry.CLOUD_LIFETIME;
            quadSize = cloud.size();
            setSize(quadSize * 2, quadSize * 2);
            hasPhysics = false;
            setColor(deep ? 0.39F : 0.34F, deep ? 0.34F : 0.41F, deep ? 0.46F : 0.47F);
            alpha = 0;
        }

        private Vec3 position() { return new Vec3(x, y, z); }

        @Override
        public void tick() {
            xo = x; yo = y; zo = z; oRoll = roll;
            if (++age >= lifetime || ClientRealmStormScene.level != this.level) { remove(); return; }
            Vec3 point = StormVolumeGeometry.cloudPosition(cloud, center, height, deep, age);
            if (!airAt(point, 0.3)) { remove(); return; }
            setPos(point.x, point.y, point.z);
            roll += (deep ? -0.013F : 0.024F) * StormVolumeGeometry.ROTATION_SPEED_SCALE;
            float flash = StormVolumeGeometry.cloudIllumination(point.subtract(center), height, STATE.clock(), deep);
            float rim = (float) Math.clamp((point.y - center.y) / Math.max(8, height), 0, 1);
            setColor((deep ? .39F : .34F) + flash * .42F + rim * .08F,
                    (deep ? .34F : .41F) + flash * .43F + rim * .09F,
                    (deep ? .46F : .47F) + flash * .48F + rim * .05F);
            var player = Minecraft.getInstance().player;
            double cameraDistance = point.distanceTo(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
            alpha = player == null ? 0 : StormVolumeGeometry.cloudAlpha(age) * scene.visualStrength(1, player.position())
                    * DisasterAtmosphereProfile.gust(STATE.clock(), deep) * (deep ? 0.72F : 1)
                    * StormVolumeGeometry.cloudCameraFade(cameraDistance, cloud.size());
        }

        @Override
        public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

        @Override
        protected int getLightColor(float partialTick) {
            int light = super.getLightColor(partialTick);
            int glow = (int) (96 + 144 * StormVolumeGeometry.cloudIllumination(position().subtract(center), height, STATE.clock(), deep));
            return (light & 0xFFFF0000) | Math.max(light & 0xFFFF, glow);
        }
    }

    private static final class StormSound extends AbstractTickableSoundInstance {
        private StormSceneState.Scene scene;
        private boolean ending;
        private float shelter = 1;
        private float shelterTarget = 1;

        private StormSound(StormSceneState.Scene scene) {
            super(scene.payload().deep() ? SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value() : SoundEvents.BREEZE_WHIRL,
                    SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.scene = scene;
            looping = true;
            delay = 0;
            relative = false;
            attenuation = Attenuation.NONE;
            volume = 0.001F;
            x = scene.payload().center().x;
            y = scene.payload().center().y + Math.min(8, scene.payload().sceneHeight() * 0.25);
            z = scene.payload().center().z;
        }

        private void stopNow() { stop(); }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != level || mc.player == null) ending = true;
            double distance = mc.player == null ? StormVolumeGeometry.VIEW_DISTANCE : mc.player.position().distanceTo(scene.payload().center());
            float near = (float) Math.clamp(1 - distance / StormVolumeGeometry.VIEW_DISTANCE, 0, 1);
            if (!ending && STATE.clock() % 10 == 0) {
                Vec3 eye = mc.player.getEyePosition();
                Vec3 source = new Vec3(x, y, z);
                boolean loaded = level.hasChunksAt(BlockPos.containing(Math.min(x, eye.x), Math.min(y, eye.y), Math.min(z, eye.z)),
                        BlockPos.containing(Math.max(x, eye.x), Math.max(y, eye.y), Math.max(z, eye.z)));
                shelterTarget = loaded && level.clip(new ClipContext(eye, source, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS ? 1 : 0.3F;
            }
            shelter += (shelterTarget - shelter) * 0.12F;
            float gust = DisasterAtmosphereProfile.gust(STATE.clock(), scene.payload().deep());
            float target = ending ? 0 : DisasterAtmosphereProfile.windVolume(distance, scene.strength(1), shelter, gust,
                    ClientConfig.DISASTER_SOUND_VOLUME.get());
            volume += (target - volume) * 0.08F;
            pitch = (scene.payload().deep() ? 0.65F : 0.55F) + near * 0.25F + (gust - 0.88F) * 0.3F - (1 - shelter) * 0.07F;
            if (ending && volume < 0.001F) stop();
        }
    }
}
