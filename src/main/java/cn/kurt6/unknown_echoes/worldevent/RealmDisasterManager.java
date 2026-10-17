package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.fx.StormVolumeGeometry;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.fx.RealmDisasterFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.RealmDisasterPayload;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RealmDisasterManager {
    private static final TagKey<Block> BREAKABLE = TagKey.create(
            Registries.BLOCK, UnknownEchoes.id("disaster_breakable"));
    private static final int TERRAIN_CHECK_RADIUS = 6;
    private static final int BLOCKS_PER_IMPACT = 3;

    private RealmDisasterManager() { }

    public static int effectRadius() {
        return DisasterState.effectRadius(ServerConfig.WORLD_EVENT_EFFECT_RADIUS.get());
    }

    public static boolean supports(WorldEventType type) {
        return type == WorldEventType.RIFT_STORM || type == WorldEventType.STRATA_RUPTURE;
    }

    public static boolean safePosition(ServerLevel level, BlockPos pos) {
        // 两个入口都位于原点附近；用水平保护柱覆盖旧档门厅的不同高度。
        return (long) pos.getX() * pos.getX() + (long) pos.getZ() * pos.getZ() > 64L * 64L
                && level.getWorldBorder().isWithinBounds(pos)
                && level.isLoaded(pos)
                && !LoadedStructureLookup.mayContainStructureAt(level, pos, structure -> true);
    }

    public static void presentScene(ServerLevel level, ActiveWorldEvent active, long now) {
        if (active.isStopped() || !supports(active.type) || !level.isLoaded(active.center)) return;
        boolean deep = active.type == WorldEventType.STRATA_RUPTURE;
        if (!active.testMode && !matchesDimension(level, deep)) return;
        Vec3 center = Vec3.atCenterOf(active.center);
        List<ServerPlayer> viewers = level.players().stream().filter(player ->
                player.distanceToSqr(center) <= StormVolumeGeometry.VIEW_DISTANCE * StormVolumeGeometry.VIEW_DISTANCE).toList();
        if (viewers.isEmpty()) return;
        int height = clearHeight(level, active.center, deep ? 56 : 96);
        active.disaster.sampleSceneHeight(height, now);
        float strength = DisasterState.intensity(now, active.startedGameTime, active.endGameTime);
        var payload = new RealmDisasterPayload(level.dimension().location(), center,
                effectRadius(), strength, deep, false, height, active.startedGameTime);
        // 远景只发形态与阶段，云团和碎片由客户端续演。
        for (ServerPlayer player : viewers) PacketDistributor.sendToPlayer(player, payload);
    }

    public static boolean testPositionReady(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && LoadedAreaCheck.horizontalRadiusLoaded(level, pos, TERRAIN_CHECK_RADIUS)
                && clearHeight(level, pos, 8) >= 8;
    }

    public static void tick(ServerLevel level, ActiveWorldEvent active,
                             List<ServerPlayer> nearbyPlayers, long now) {
        if (active.isStopped()) return;
        boolean deep = active.type == WorldEventType.STRATA_RUPTURE;
        if (!active.testMode && !matchesDimension(level, deep)) return;
        DisasterState state = active.disaster;
        List<ServerPlayer> exposed = nearbyPlayers.stream()
                .filter(player -> player.isAlive() && !player.isSpectator()
                        && (active.testMode || safePosition(level, player.blockPosition())))
                .toList();
        if (exposed.isEmpty()) {
            state.clearMarks();
            return;
        }
        long age = now - active.startedGameTime;
        if (Math.floorMod(now, 20L) == 0L) {
            if (!active.testMode && age >= DisasterState.WARNING_TICKS) empowerMonsters(level, active.center);
        }

        Set<UUID> hitThisPulse = new HashSet<>();
        for (DisasterState.Mark mark : state.marks()) {
            if (active.isStopped()) return;
            if (!mark.warningIsContinuous(now) || !strikePositionReady(level, mark.center(), deep, active.testMode)) {
                state.remove(mark);
                continue;
            }
            if (now >= mark.impactTime()) {
                state.remove(mark);
                // 长时间无人或区块卸载后直接丢弃过期落击，不补打错过的预警。
                if (now - mark.impactTime() < 10L) {
                    impact(level, active, mark, exposed, hitThisPulse, deep);
                }
            } else {
                telegraph(level, mark.center(), deep, mark.remaining(now), exposed);
                mark.presented(now);
            }
        }
        if (active.isStopped()) return;
        if (!state.beginWave(now, active.startedGameTime, active.endGameTime)) return;
        int startIndex = level.random.nextInt(exposed.size());
        for (int i = 0; i < exposed.size() && state.marks().size() < DisasterState.MAX_MARKS; i++) {
            if (active.isStopped()) return;
            ServerPlayer player = exposed.get((startIndex + i) % exposed.size());
            BlockPos feet = player.blockPosition();
            if ((!active.testMode && player.isCreative()) || !player.onGround() || player.isInWaterOrBubble()
                    || (!active.testMode && (!safePosition(level, feet) || !deep && !level.canSeeSky(feet)))) continue;
            Vec3 mark = new Vec3(player.getX(), player.getY(), player.getZ());
            if (strikePositionReady(level, mark, deep, active.testMode) && state.add(mark, now)) {
                telegraph(level, mark, deep, 1.0F, exposed);
                player.displayClientMessage(Component.translatable("message.unknown_echoes.disaster."
                        + (deep ? "jump" : "dodge")), true);
            }
        }
    }

    private static boolean matchesDimension(ServerLevel level, boolean deep) {
        return level.dimension().location().equals(deep ? EchoPermission.DEEP_ECHOES_ID : EchoPermission.ECHO_REALM_ID);
    }

    public static void tickWind(ServerLevel level, ActiveWorldEvent active, long now,
                                Set<UUID> affected, java.util.function.BooleanSupplier current) {
        if (!current.getAsBoolean() || !active.isActiveAt(now) || !supports(active.type)
                || level.getDifficulty() == Difficulty.PEACEFUL || !active.disaster.beginWindTick(now)) return;
        boolean deep = active.type == WorldEventType.STRATA_RUPTURE;
        double strength = DisasterWindField.strength(now, active.startedGameTime, active.endGameTime);
        int height = active.disaster.windHeight(now);
        if (strength <= 0 || height < 8 || !safePosition(level, active.center)
                || !active.testMode && !matchesDimension(level, deep)) return;
        Vec3 center = Vec3.atBottomCenterOf(active.center);
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (!current.getAsBoolean() || !active.isActiveAt(now)) return;
            if (!windPlayerReady(level, player) || affected.contains(player.getUUID())) continue;
            Vec3 position = player.position();
            Vec3 velocity = player.getDeltaMovement();
            Vec3 impulse = DisasterWindField.impulse(position.subtract(center), velocity,
                    effectRadius(), height, deep, strength);
            if (impulse.lengthSqr() < 1.0E-10 || !safePosition(level, player.blockPosition())
                    || !deep && !level.canSeeSky(player.blockPosition())) continue;
            Vec3 eye = player.getEyePosition();
            Vec3 source = center.add(0, Math.clamp(eye.y - center.y, 1, height - 1), 0);
            if (!level.hasChunksAt(BlockPos.containing(Math.min(eye.x, source.x), Math.min(eye.y, source.y), Math.min(eye.z, source.z)),
                    BlockPos.containing(Math.max(eye.x, source.x), Math.max(eye.y, source.y), Math.max(eye.z, source.z)))
                    || level.clip(new ClipContext(eye, source, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            if (!current.getAsBoolean() || !active.isActiveAt(now)) return;
            if (!windPlayerReady(level, player) || level.getDifficulty() == Difficulty.PEACEFUL
                    || !position.equals(player.position()) || !velocity.equals(player.getDeltaMovement())) continue;
            affected.add(player.getUUID());
            player.setDeltaMovement(player.getDeltaMovement().add(impulse));
            player.hasImpulse = true;
            player.hurtMarked = true;
        }
    }

    private static boolean windPlayerReady(ServerLevel level, ServerPlayer player) {
        return player.serverLevel() == level && player.isAlive() && !player.isRemoved()
                && !player.hasDisconnected() && !player.isCreative() && !player.isSpectator() && !player.isPassenger();
    }

    private static boolean strikePositionReady(ServerLevel level, Vec3 center, boolean deep, boolean testMode) {
        BlockPos pos = BlockPos.containing(center);
        return (testMode ? level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos) : safePosition(level, pos))
                && LoadedAreaCheck.horizontalRadiusLoaded(level, pos, TERRAIN_CHECK_RADIUS)
                && (testMode || deep || level.canSeeSky(pos));
    }

    public static void end(ServerLevel level, ActiveWorldEvent active) {
        active.stop();
        if (!supports(active.type)) return;
        var payload = new RealmDisasterPayload(level.dimension().location(), Vec3.atCenterOf(active.center),
                effectRadius(), 0, active.type == WorldEventType.STRATA_RUPTURE,
                false, 0, active.startedGameTime);
        for (ServerPlayer player : level.players()) PacketDistributor.sendToPlayer(player, payload);
    }

    private static void telegraph(ServerLevel level, Vec3 center, boolean deep, float remaining,
                                    List<ServerPlayer> players) {
        double radius = deep ? DisasterState.RUPTURE_RADIUS : DisasterState.STORM_RADIUS;
        for (CombatFxPayload cue : RealmDisasterFx.warning(center, deep, remaining)) emitCue(level, cue);
        // 原版粒子保留预警边界，关闭可选特效时仍能判断落击范围。
        for (int i = 0; i < 18; i++) {
            double angle = i * Math.PI / 9.0D;
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(center) <= 32.0D * 32.0D) {
                    level.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, true,
                            center.x + Math.cos(angle) * radius, center.y + 0.15D,
                            center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private static void impact(ServerLevel level, ActiveWorldEvent active, DisasterState.Mark mark,
                                List<ServerPlayer> players, Set<UUID> hitThisPulse, boolean deep) {
        if (active.isStopped()) return;
        Vec3 center = mark.center();
        BlockPos pos = BlockPos.containing(center);
        if (!active.testMode && (!safePosition(level, pos) || !deep && !level.canSeeSky(pos))) return;
        if (!LoadedAreaCheck.horizontalRadiusLoaded(level, pos, TERRAIN_CHECK_RADIUS)) return;
        int clearance = clearHeight(level, pos, deep ? 6 : 18);
        for (CombatFxPayload cue : RealmDisasterFx.strike(center, deep, clearance)) emitCue(level, cue);
        for (ServerPlayer player : players) {
            if (active.isStopped()) return;
            PacketDistributor.sendToPlayer(player, new RealmDisasterPayload(level.dimension().location(),
                    center, 28.0F, 1.0F, deep, true, 0, active.startedGameTime));
            if (player.isCreative() || player.isSpectator() || !player.isAlive()
                    || hitThisPulse.contains(player.getUUID()) || level.getDifficulty() == Difficulty.PEACEFUL
                    || !DisasterState.hits(center, player.position(), deep)
                    || !safePosition(level, player.blockPosition())
                    || !deep && !level.canSeeSky(player.blockPosition())) continue;
            if (deep && (!player.onGround() || player.isInWaterOrBubble())) continue;
            if (level.clip(new ClipContext(center.add(0, 0.5D, 0), player.position().add(0, 0.5D, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            hitThisPulse.add(player.getUUID());
            float damage = ServerConfig.DISASTER_DAMAGE.get().floatValue();
            if (damage > 0.0F) {
                var type = ResourceKey.create(Registries.DAMAGE_TYPE,
                        UnknownEchoes.id(deep ? "strata_rupture" : "rift_storm"));
                player.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(type)), damage);
            }
        }
        if (!active.isStopped()) breakTerrain(level, active, center);
    }

    private static void empowerMonsters(ServerLevel level, BlockPos center) {
        if (!ServerConfig.DISASTER_EMPOWER_MONSTERS.get()) return;
        int radius = effectRadius();
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center).inflate(radius), entity -> entity instanceof Enemy && entity.isAlive()
                        && !entity.getType().is(ModEntityTags.ECHO_BOSSES)
                        && !entity.getType().is(Tags.EntityTypes.BOSSES)
                        && entity.distanceToSqr(Vec3.atCenterOf(center)) <= (double) radius * radius)) {
            if (!safePosition(level, mob.blockPosition())) continue;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
        }
    }

    private static void breakTerrain(ServerLevel level, ActiveWorldEvent active, Vec3 center) {
        if (active.isStopped() || !ServerConfig.DISASTER_BLOCK_DAMAGE.get()
                || ServerConfig.DISASTER_BLOCKS_PER_IMPACT.get() <= 0
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !active.disaster.canBreakBlock()) return;
        BlockPos base = BlockPos.containing(center).below();
        if (!LoadedAreaCheck.horizontalRadiusLoaded(level, base, TERRAIN_CHECK_RADIUS)
                || !naturalSurroundings(level, base)) return;
        List<BlockPos> candidates = List.of(base, base.north(), base.south(), base.east(), base.west());
        int broken = 0;
        for (BlockPos pos : candidates) {
            if (active.isStopped() || broken >= Math.min(BLOCKS_PER_IMPACT, ServerConfig.DISASTER_BLOCKS_PER_IMPACT.get())
                    || !active.disaster.canBreakBlock()) break;
            BlockState block = level.getBlockState(pos);
            if (!block.is(BREAKABLE) || block.hasBlockEntity() || block.is(Tags.Blocks.ORES)
                    || DisasterBuildProtection.isProtected(level, pos)
                    || block.getDestroySpeed(level, pos) < 0.0F || !safePosition(level, pos)
                    || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                    || !level.getBlockState(pos.below(2)).isSolidRender(level, pos.below(2))) continue;
            if (level.destroyBlock(pos, false)) {
                active.disaster.recordBrokenBlock();
                broken++;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block),
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 24, 0.6, 0.5, 0.6, 0.16);
            }
        }
    }

    private static boolean naturalSurroundings(ServerLevel level, BlockPos center) {
        for (int x = (center.getX() - 4) >> 4; x <= (center.getX() + 4) >> 4; x++) {
            for (int z = (center.getZ() - 4) >> 4; z <= (center.getZ() + 4) >> 4; z++) {
                if (DisasterBuildProtection.isProtected(level, new BlockPos(x << 4, center.getY(), z << 4))) return false;
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -1, -4), center.offset(4, 3, 4))) {
            BlockState block = level.getBlockState(pos);
            if (block.hasBlockEntity()) return false;
            if (block.isAir() || !block.getFluidState().isEmpty() || block.is(BREAKABLE)
                    || block.is(BlockTags.BASE_STONE_OVERWORLD) || block.is(BlockTags.DIRT)
                    || block.is(BlockTags.LOGS) || block.is(BlockTags.LEAVES) || block.is(Tags.Blocks.ORES)
                    || block.canBeReplaced()) continue;
            return false;
        }
        return true;
    }

    private static void emitCue(ServerLevel level, CombatFxPayload cue) {
        CombatFx.emit(level, cue.shape(), cue.origin(), cue.direction(), cue.radius(), cue.length(),
                cue.span(), cue.core(), cue.edge(), cue.duration(), false);
    }

    private static int clearHeight(ServerLevel level, BlockPos center, int maximum) {
        int height = 0;
        for (int y = 1; y <= maximum && center.getY() + y < level.getMaxBuildHeight(); y++) {
            if (!level.getBlockState(center.above(y)).isAir()) break;
            height = y;
        }
        return height;
    }
}
