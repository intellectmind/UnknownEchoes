package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.fx.StormWindMoteGeometry;
import cn.kurt6.unknown_echoes.worldevent.DisasterWindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

public final class ClientStormWindParticles {
    private static final List<Mote> MOTES = new ArrayList<>();
    private static List<StormSceneState.Scene> scenes = List.of();
    private static ClientLevel level;
    private static double credit;
    private static long lastEmissionTick = Long.MIN_VALUE;

    private ClientStormWindParticles() { }

    public static void clear() {
        MOTES.forEach(Mote::remove);
        MOTES.clear();
        scenes = List.of();
        credit = 0;
        lastEmissionTick = Long.MIN_VALUE;
        level = null;
    }

    public static void tick(Minecraft mc, List<StormSceneState.Scene> visible, double density) {
        if (mc.level != level) { clear(); level = mc.level; }
        if (level == null || mc.player == null || density <= 0 || visible.isEmpty()
                || !level.hasChunkAt(mc.player.blockPosition())) { clear(); return; }
        if (mc.isPaused() || !level.tickRateManager().runsNormally()) return;
        scenes = List.copyOf(visible);
        MOTES.removeIf(mote -> {
            if (mote.isAlive() && scenes.contains(mote.scene)) return false;
            mote.remove();
            return true;
        });
        if (lastEmissionTick == level.getGameTime()) return;
        lastEmissionTick = level.getGameTime();
        Vec3 viewer = mc.player.position();
        StormSceneState.Scene selected = null;
        double strongest = 0;
        for (var scene : scenes) {
            var p = scene.payload();
            double strength = strength(scene);
            Vec3 force = DisasterWindField.impulse(viewer.subtract(p.center().add(0, -.5, 0)), Vec3.ZERO,
                    p.radius(), p.sceneHeight(), p.deep(), strength);
            double influence = force.lengthSqr();
            if (influence > strongest && (p.deep() || level.canSeeSky(mc.player.blockPosition()))
                    && clearPath(mc.player.getEyePosition(), new Vec3(p.center().x, mc.player.getEyePosition().y, p.center().z))) {
                strongest = influence;
                selected = scene;
            }
        }
        if (selected == null) { credit = 0; return; }
        if (level != mc.level) { clear(); return; }
        credit = Math.min(StormWindMoteGeometry.PER_TICK, credit + StormWindMoteGeometry.PER_TICK
                * Math.clamp(density, 0, 1) * strength(selected));
        int count = Math.min((int) credit, StormWindMoteGeometry.MAX_LIVE - MOTES.size());
        credit -= count;
        if (count <= 0) return;
        var texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if (!(texture instanceof TextureAtlas atlas)) return;
        for (int i = 0; i < count; i++) {
            Vec3 point = StormWindMoteGeometry.spawn(viewer, level.getGameTime(), i);
            BlockPos pos = BlockPos.containing(point);
            var p = selected.payload();
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()
                    || !p.deep() && !level.canSeeSky(pos) || !clearPath(mc.player.getEyePosition(), point)
                    || StormWindMoteGeometry.velocity(point.subtract(p.center().add(0, -.5, 0)), p.radius(), p.sceneHeight(),
                    p.deep(), strength(selected), i).lengthSqr() < 1E-10) continue;
            if (level != mc.level) { clear(); return; }
            Mote mote = new Mote(selected, point, i + (int) Math.floorMod(level.getGameTime(), 1024), atlas);
            MOTES.add(mote);
            mc.particleEngine.add(mote);
        }
    }

    private static double strength(StormSceneState.Scene scene) {
        return StormWindMoteGeometry.strength(level.getGameTime(), scene.payload().eventStart(), scene.strength(1));
    }

    private static boolean clearPath(Vec3 from, Vec3 to) {
        return level.hasChunksAt(BlockPos.containing(Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z)),
                BlockPos.containing(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z)))
                && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private static final class Mote extends TextureSheetParticle {
        private final StormSceneState.Scene scene;
        private final int index;
        private final boolean spark;

        private Mote(StormSceneState.Scene scene, Vec3 point, int index, TextureAtlas atlas) {
            super(ClientStormWindParticles.level, point.x, point.y, point.z);
            this.scene = scene;
            this.index = index;
            spark = index % 3 == 0;
            setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace(spark ? "spark_" + index % 8 : "generic_" + (index % 4 + 3))));
            boolean deep = scene.payload().deep();
            setColor(deep ? .76F : .60F, deep ? .63F : .85F, .96F);
            quadSize = spark ? .055F : .10F + index % 4 * .015F;
            lifetime = StormWindMoteGeometry.LIFETIME;
            hasPhysics = false;
            alpha = 0;
            xo = x; yo = y; zo = z;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            xo = x; yo = y; zo = z; oRoll = roll;
            if (++age >= lifetime || this.level != ClientStormWindParticles.level || this.level != mc.level
                    || mc.player == null || !scenes.contains(scene) || strength(scene) < .01) { remove(); return; }
            var p = scene.payload();
            Vec3 point = new Vec3(x, y, z);
            Vec3 velocity = StormWindMoteGeometry.velocity(point.subtract(p.center().add(0, -.5, 0)), p.radius(), p.sceneHeight(),
                    p.deep(), strength(scene), index + age);
            if (velocity.lengthSqr() < 1E-10 || !clearPath(point, point.add(velocity))) { remove(); return; }
            Vec3 next = point.add(velocity);
            setPos(next.x, next.y, next.z);
            roll += p.deep() ? -.09F : .12F;
            alpha = StormWindMoteGeometry.alpha(age, next.distanceTo(mc.gameRenderer.getMainCamera().getPosition()), strength(scene));
        }

        @Override
        protected int getLightColor(float partialTick) { return spark ? 0xF000F0 : super.getLightColor(partialTick); }

        @Override
        public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    }
}
