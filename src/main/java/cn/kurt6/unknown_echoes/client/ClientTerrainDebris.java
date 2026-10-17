package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.fx.TerrainDebrisPhysics;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientTerrainDebris {
    private static final List<Fragment> FRAGMENTS = new ArrayList<>();
    private static final List<RecentBurst> RECENT = new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(131072));
    private static ClientLevel level;
    private static long lastAdmissionTick = Long.MIN_VALUE;
    private static int admissions;

    private ClientTerrainDebris() { }

    public static void accept(CombatFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level == null || mc.player == null || !p.valid() || !enabled(p.boss())
                || p.origin().distanceToSqr(mc.player.position()) > 48 * 48) {
            return;
        }
        long now = level.getGameTime();
        RECENT.removeIf(burst -> now - burst.tick > 4);
        if (RECENT.stream().anyMatch(burst -> burst.origin.distanceToSqr(p.origin()) < 2.25)) {
            return;
        }
        if (lastAdmissionTick != now) {
            lastAdmissionTick = now;
            admissions = 0;
        }
        if (++admissions > 4) {
            return;
        }
        RECENT.add(new RecentBurst(p.origin(), now));
        int particleSetting = mc.options.particles().get() == ParticleStatus.MINIMAL ? 2
                : mc.options.particles().get() == ParticleStatus.DECREASED ? 1 : 0;
        double density = p.boss() ? ClientConfig.BOSS_PARTICLE_DENSITY.get() : 1;
        int budget = TerrainDebrisPhysics.budget(particleSetting, density);
        int count = Math.min(TerrainDebrisPhysics.MAX_PER_BURST, 10 + (int) (p.radius() * 2));
        count = Math.min((int) Math.ceil(count * density / (particleSetting + 1)), budget - FRAGMENTS.size());
        long seed = BlockPos.containing(p.origin()).asLong() ^ now * 31;
        for (int i = 0; i < count; i++) {
            var launch = TerrainDebrisPhysics.launch(seed, i, p.radius(), p.length());
            Vec3 sample = p.origin().add(launch.offset());
            BlockPos ground = surface(sample);
            if (ground == null) {
                continue;
            }
            BlockState state = level.getBlockState(ground);
            double half = launch.size() * 0.5;
            Vec3 start = new Vec3(sample.x, ground.getY() + 1 + half + 0.025, sample.z);
            AABB bounds = AABB.ofSize(start, launch.size(), launch.size(), launch.size());
            if (!loaded(bounds) || !level.noCollision(bounds)) {
                continue;
            }
            FRAGMENTS.add(new Fragment(start, launch, state, LevelRenderer.getLightColor(level, ground.above()),
                    p.duration(), p.boss()));
        }
    }

    private static BlockPos surface(Vec3 point) {
        BlockPos top = BlockPos.containing(point.x, point.y + 0.5, point.z);
        if (!level.hasChunkAt(top)) {
            return null;
        }
        // 只读落点附近三格；高空爆炸不能从远处地板凭空掀起石块。
        for (int depth = 0; depth <= 3; depth++) {
            BlockPos pos = top.below(depth);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || (state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty())) {
                continue;
            }
            return state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity()
                    && state.canOcclude() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, pos)
                    && state.isFaceSturdy(level, pos, Direction.UP) ? pos : null;
        }
        return null;
    }

    private static boolean loaded(AABB bounds) {
        return level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    private static boolean enabled(boolean boss) {
        return boss ? ClientConfig.ENABLE_BOSS_PARTICLES.get() && ClientConfig.BOSS_PARTICLE_DENSITY.get() > 0
                : ClientConfig.ENABLE_ECHO_PARTICLES.get();
    }

    private static void ensureLevel(ClientLevel current) {
        if (level != current) {
            FRAGMENTS.clear();
            RECENT.clear();
            admissions = 0;
            lastAdmissionTick = Long.MIN_VALUE;
            level = current;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ensureLevel(null);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level == null || mc.isPaused() || !level.tickRateManager().runsNormally()) {
            return;
        }
        FRAGMENTS.removeIf(fragment -> !enabled(fragment.boss) || fragment.advance());
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || level == null || level != Minecraft.getInstance().level || FRAGMENTS.isEmpty()
                || ClientShaderCompat.pass() == ClientShaderCompat.Pass.SKIP) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Fragment fragment : FRAGMENTS) {
            if (fragment.age <= fragment.launch.delay() || !enabled(fragment.boss)) {
                continue;
            }
            Vec3 position = fragment.previous.lerp(fragment.position, partial);
            if (!event.getFrustum().isVisible(AABB.ofSize(position, 2, 2, 2))) {
                continue;
            }
            float size = fragment.launch.size() * TerrainDebrisPhysics.scale(fragment.age, partial, fragment.lifetime);
            double time = fragment.age - fragment.launch.delay() + partial;
            Vec3 spin = fragment.launch.spin();
            pose.pushPose();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            pose.mulPose(Axis.XP.rotation((float) (spin.x * time)));
            pose.mulPose(Axis.YP.rotation((float) (spin.y * time)));
            pose.mulPose(Axis.ZP.rotation((float) (spin.z * time)));
            pose.scale(size, size * 0.72F, size * 0.86F);
            pose.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(fragment.state, pose, BUFFERS,
                    fragment.light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        BUFFERS.endBatch();
    }

    private record RecentBurst(Vec3 origin, long tick) { }

    private static final class Fragment {
        private final TerrainDebrisPhysics.Launch launch;
        private final BlockState state;
        private final int light;
        private final int lifetime;
        private final boolean boss;
        private Vec3 position;
        private Vec3 previous;
        private Vec3 velocity;
        private int age;
        private int bounces;

        private Fragment(Vec3 start, TerrainDebrisPhysics.Launch launch, BlockState state,
                          int light, int lifetime, boolean boss) {
            this.position = this.previous = start;
            this.launch = launch;
            this.velocity = launch.velocity();
            this.state = state;
            this.light = light;
            this.lifetime = lifetime;
            this.boss = boss;
        }

        private boolean advance() {
            previous = position;
            if (++age >= lifetime || !level.hasChunkAt(BlockPos.containing(position))) {
                return true;
            }
            if (age <= launch.delay()) {
                return false;
            }
            AABB bounds = AABB.ofSize(position, launch.size(), launch.size(), launch.size());
            if (!loaded(bounds.expandTowards(velocity))) {
                return true;
            }
            Vec3 move = Entity.collideBoundingBox(null, velocity, bounds, level, List.of());
            boolean landed = velocity.y < 0 && Math.abs(move.y - velocity.y) > 1.0E-6;
            position = position.add(move);
            velocity = TerrainDebrisPhysics.afterMove(velocity, move, bounces);
            if (landed && bounces++ == 0) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        position.x, position.y - launch.size() * 0.4, position.z, 0, 0.035, 0);
            }
            return false;
        }
    }
}
