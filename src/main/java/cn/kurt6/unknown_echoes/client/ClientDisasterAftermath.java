package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.fx.DisasterAftermathGeometry;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientDisasterAftermath {
    private static final List<Dust> PARTICLES = new ArrayList<>();
    private static ClientLevel level;
    private static long clock;

    private ClientDisasterAftermath() { }

    public static void accept(RealmDisasterPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level != null && payload.valid() && level.dimension().location().equals(payload.dimension())
                && !payload.impact() && payload.strength() == 0) {
            PARTICLES.removeIf(dust -> {
                if (dust.eventStart != payload.eventStart()) return false;
                dust.remove();
                return true;
            });
            return;
        }
        if (level == null || mc.player == null || !payload.valid() || !payload.impact()
                || !level.dimension().location().equals(payload.dimension())
                || mc.player.position().distanceToSqr(payload.center()) > 48 * 48) return;
        double density = ClientRealmStormScene.density(mc);
        int count = Math.min((int) Math.ceil(DisasterAftermathGeometry.DUST_PER_IMPACT * density),
                DisasterAftermathGeometry.MAX_DUST - PARTICLES.size());
        if (count == 0) return;
        var texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if (!(texture instanceof TextureAtlas atlas)) return;
        for (int i = 0; i < count; i++) {
            int index = i * DisasterAftermathGeometry.DUST_PER_IMPACT / count;
            Dust dust = new Dust(payload, index, atlas);
            if (!dust.followGround(0)) continue;
            PARTICLES.add(dust);
            mc.particleEngine.add(dust);
        }
    }

    private static void ensureLevel(ClientLevel current) {
        if (level == current) return;
        PARTICLES.forEach(Dust::remove);
        PARTICLES.clear();
        clock = 0;
        level = current;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ensureLevel(null); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level == null || mc.isPaused()) return;
        clock++;
        boolean disabled = ClientRealmStormScene.density(mc) == 0;
        PARTICLES.removeIf(dust -> {
            if (!disabled && dust.isAlive() && clock - dust.born < DisasterAftermathGeometry.DUST_LIFETIME) return false;
            dust.remove();
            return true;
        });
    }

    private static final class Dust extends TextureSheetParticle {
        private final Vec3 center;
        private final boolean deep;
        private final int index;
        private final long born;
        private final float strength;
        private final long eventStart;

        private Dust(RealmDisasterPayload payload, int index, TextureAtlas atlas) {
            super(ClientDisasterAftermath.level, payload.center().x, payload.center().y + 0.3, payload.center().z);
            this.center = payload.center();
            this.deep = payload.deep();
            this.index = index;
            this.born = clock;
            this.strength = payload.strength();
            this.eventStart = payload.eventStart();
            lifetime = DisasterAftermathGeometry.DUST_LIFETIME;
            hasPhysics = false;
            setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_" + (index % 2 + 6))));
            setColor(deep ? 0.44F : 0.4F, deep ? 0.36F : 0.44F, deep ? 0.48F : 0.47F);
            alpha = 0;
        }

        private boolean followGround(int age) {
            var sample = DisasterAftermathGeometry.dust(center, deep, index, age);
            Vec3 point = sample.position();
            BlockPos top = BlockPos.containing(point.x, center.y + 1, point.z);
            if (!level.hasChunkAt(top)) return false;
            for (int depth = 0; depth <= 3; depth++) {
                BlockPos floor = top.below(depth);
                var state = level.getBlockState(floor);
                if (!state.isSolidRender(level, floor) || !level.getBlockState(floor.above()).isAir()) continue;
                Vec3 target = new Vec3(point.x, floor.getY() + 1 + point.y - center.y, point.z);
                Vec3 previous = new Vec3(x, y, z);
                if (age > 0 && (!level.hasChunksAt(BlockPos.containing(Math.min(x, target.x), Math.min(y, target.y), Math.min(z, target.z)),
                        BlockPos.containing(Math.max(x, target.x), Math.max(y, target.y), Math.max(z, target.z)))
                        || level.clip(new ClipContext(previous, target, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, CollisionContext.empty())).getType() != HitResult.Type.MISS)) return false;
                setPos(target.x, target.y, target.z);
                quadSize = sample.size();
                alpha = sample.alpha() * strength;
                if (age == 0) { xo = x; yo = y; zo = z; }
                return true;
            }
            return false;
        }

        @Override
        public void tick() {
            xo = x; yo = y; zo = z; oRoll = roll;
            if (++age >= lifetime || this.level != ClientDisasterAftermath.level || !followGround(age)) { remove(); return; }
            roll += (index % 2 == 0 ? 1 : -1) * 0.08F;
        }

        @Override
        public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    }
}
