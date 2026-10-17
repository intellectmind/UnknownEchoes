package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.ecology.RealmWorldEventEcologyManager;
import cn.kurt6.unknown_echoes.ecology.RealmWorldEventFloraManager;
import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 世界事件框架:统一注册表 + 触发器 + 频率/开关配置 + 个人参与记录。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class WorldEventManager {

    /** 事件增益的补发脉冲间隔(1 秒);夜视时长下限按该值换算。 */
    private static final int WORLD_EVENT_EFFECT_PULSE_TICKS = 20;

    private static int eventRadius() {
        return ServerConfig.WORLD_EVENT_EFFECT_RADIUS.get();
    }

    private static int eventRadiusFor(WorldEventType type) {
        return RealmDisasterManager.supports(type) ? RealmDisasterManager.effectRadius() : eventRadius();
    }

    private static final Map<ResourceKey<Level>, List<ActiveWorldEvent>> ACTIVE = new HashMap<>();
    private static final java.util.Set<ServerLevel> TICKING =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public static boolean startDisaster(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WorldEventType type;
        if (level.dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            type = WorldEventType.RIFT_STORM;
        } else if (level.dimension().location().equals(EchoPermission.DEEP_ECHOES_ID)) {
            type = WorldEventType.STRATA_RUPTURE;
        } else return false;
        if (!ServerConfig.WORLD_EVENTS_ENABLED.get() || isEventDisabled(type)
                || !RealmDisasterManager.safePosition(level, player.blockPosition())) return false;
        List<ActiveWorldEvent> events = ACTIVE.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        if (events.size() >= ServerConfig.WORLD_EVENT_MAX_ACTIVE.get()
                || events.stream().anyMatch(active -> RealmDisasterManager.supports(active.type))) return false;
        long now = level.getGameTime();
        ActiveWorldEvent active = new ActiveWorldEvent(type, player.blockPosition(), now,
                safeDeadline(now, ServerConfig.WORLD_EVENT_DURATION_TICKS.get()));
        events.add(active);
        announce(level, active);
        return true;
    }

    public static int stopDisasters(ServerLevel level) {
        List<ActiveWorldEvent> events = ACTIVE.get(level.dimension());
        if (events == null) return 0;
        return finishEvents(level, events, active -> RealmDisasterManager.supports(active.type));
    }

    /** 先摘除整批事件并停止其后续动作，再发结束通知，允许通知回调开启新事件。 */
    private static int finishEvents(ServerLevel level, List<ActiveWorldEvent> events,
                                    java.util.function.Predicate<ActiveWorldEvent> shouldFinish) {
        List<ActiveWorldEvent> stopped = null;
        for (ActiveWorldEvent active : events) {
            if (!shouldFinish.test(active)) continue;
            if (stopped == null) stopped = new ArrayList<>();
            stopped.add(active);
        }
        if (stopped == null) return 0;
        events.removeAll(stopped);
        for (ActiveWorldEvent active : stopped) active.stop();
        if (events.isEmpty() && ACTIVE.get(level.dimension()) == events) ACTIVE.remove(level.dimension());
        for (ActiveWorldEvent active : stopped) RealmDisasterManager.end(level, active);
        return stopped.size();
    }

    public enum DisasterTestResult { STARTED, DISABLED, BUSY, POSITION, INVALID }

    public static DisasterTestResult testDisaster(ServerPlayer player, WorldEventType type, int seconds) {
        if (!RealmDisasterManager.supports(type) || seconds < 20 || seconds > 300) return DisasterTestResult.INVALID;
        ServerLevel level = player.serverLevel();
        if (!ServerConfig.WORLD_EVENTS_ENABLED.get() || isEventDisabled(type)) return DisasterTestResult.DISABLED;
        if (!player.isAlive() || player.isSpectator() || !player.onGround() || player.isInWaterOrBubble()
                || !level.getWorldBorder().isWithinBounds(player.blockPosition())
                || !RealmDisasterManager.testPositionReady(level, player.blockPosition())) return DisasterTestResult.POSITION;
        List<ActiveWorldEvent> events = ACTIVE.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        if (events.size() >= ServerConfig.WORLD_EVENT_MAX_ACTIVE.get()
                || events.stream().anyMatch(active -> RealmDisasterManager.supports(active.type))) return DisasterTestResult.BUSY;
        long now = level.getGameTime();
        ActiveWorldEvent active = new ActiveWorldEvent(type, player.blockPosition(), now, safeDeadline(now, seconds * 20L), true);
        events.add(active);
        RealmDisasterManager.presentScene(level, active, now);
        return DisasterTestResult.STARTED;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        for (List<ActiveWorldEvent> events : ACTIVE.values()) {
            for (ActiveWorldEvent active : events) active.stop();
        }
        ACTIVE.clear();
    }

    public static WorldEventSignal findNearestTravelEvent(ServerLevel level,
                                                           BlockPos origin,
                                                           int radius) {
        if (!ServerConfig.WORLD_EVENTS_ENABLED.get()) {
            return null;
        }
        long radiusSqr = (long) radius * radius;
        long now = level.getGameTime();
        ActiveWorldEvent nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (ActiveWorldEvent active : ACTIVE.getOrDefault(level.dimension(), List.of())) {
            if (!active.isActiveAt(now)
                    || isEventDisabled(active.type)
                    || !RealmWorldEventEcologyManager.supports(active.type)) {
                continue;
            }
            double distanceSqr = active.center.distSqr(origin);
            if (distanceSqr <= radiusSqr && distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = active;
            }
        }
        return nearest == null ? null : new WorldEventSignal(nearest.type, nearest.center);
    }

    /** 为谐生观测镜提供三域低频环境活动的只读快照，不改变目击状态。 */
    public static WorldEventSignal findNearestAmbientEvent(ServerLevel level,
                                                            BlockPos origin,
                                                            int radius) {
        if (!ServerConfig.WORLD_EVENTS_ENABLED.get()) {
            return null;
        }
        long radiusSqr = (long) radius * radius;
        long now = level.getGameTime();
        ActiveWorldEvent nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (ActiveWorldEvent active : ACTIVE.getOrDefault(level.dimension(), List.of())) {
            if (!active.isActiveAt(now)
                    || isEventDisabled(active.type)
                    || !RealmAmbientEventManager.supports(active.type)) {
                continue;
            }
            double distanceSqr = active.center.distSqr(origin);
            if (distanceSqr <= radiusSqr && distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = active;
            }
        }
        return nearest == null ? null : new WorldEventSignal(nearest.type, nearest.center);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!TICKING.add(level)) return;
        try {
            if (!ServerConfig.WORLD_EVENTS_ENABLED.get()) {
                List<ActiveWorldEvent> stopped = ACTIVE.remove(level.dimension());
                if (stopped != null) finishEvents(level, stopped, active -> true);
                return;
            }
            long now = level.getGameTime();
            List<ActiveWorldEvent> events = ACTIVE.get(level.dimension());
            if (events != null) {
                finishEvents(level, events, active -> active.isExpired(now) || isEventDisabled(active.type));
                events = ACTIVE.get(level.dimension());
            }
            if (events != null && events.isEmpty()) {
                ACTIVE.remove(level.dimension());
                events = null;
            }
            if (level.players().isEmpty()) {
                return;
            }
            if (events != null && now % 10 == 0) {
                for (ActiveWorldEvent active : List.copyOf(events)) {
                    if (!ServerConfig.WORLD_EVENTS_ENABLED.get() || ACTIVE.get(level.dimension()) != events) break;
                    if (active.isStopped() || !events.contains(active) || isEventDisabled(active.type)) continue;
                    tickEvent(level, active, now);
                }
            }

            if (!ServerConfig.WORLD_EVENTS_ENABLED.get()) return;
            events = ACTIVE.get(level.dimension());
            tickDisasterWind(level, events, now);

            long checkInterval = Math.max(1L,
                    (long) ServerConfig.WORLD_EVENT_CHECK_INTERVAL_TICKS.get());
            if (Math.floorMod(now, checkInterval) != 0L
                    || (events != null && events.size() >= ServerConfig.WORLD_EVENT_MAX_ACTIVE.get())) {
                return;
            }
            double chance = WorldEventTuning.naturalChance(level.dimension().location());
            if (chance <= 0.0D || level.random.nextDouble() >= chance) {
                return;
            }
            if (events == null) {
                events = new ArrayList<>();
                ACTIVE.put(level.dimension(), events);
            }
            tryStartEvent(level, events, now);
        } finally {
            TICKING.remove(level);
        }
    }

    private static void tickDisasterWind(ServerLevel level, List<ActiveWorldEvent> events, long now) {
        if (events == null) return;
        java.util.Set<java.util.UUID> affected = new java.util.HashSet<>();
        for (ActiveWorldEvent active : List.copyOf(events)) {
            if (!RealmDisasterManager.supports(active.type)) continue;
            RealmDisasterManager.tickWind(level, active, now, affected,
                    () -> ServerConfig.WORLD_EVENTS_ENABLED.get() && ACTIVE.get(level.dimension()) == events
                            && events.contains(active) && !isEventDisabled(active.type));
        }
    }

    /** 选一名在线玩家,按维度/群系挑可触发事件并寻找安全位置;失败则本次取消。 */
    private static void tryStartEvent(ServerLevel level, List<ActiveWorldEvent> events, long now) {
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator())
                .toList();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        boolean inRealm = level.dimension().location().equals(EchoPermission.ECHO_REALM_ID);
        boolean inDeep = level.dimension().location().equals(EchoPermission.DEEP_ECHOES_ID);
        boolean inOverworld = level.dimension().equals(Level.OVERWORLD);
        var anchorBiome = level.getBiome(anchor.blockPosition()).unwrapKey()
                .map(ResourceKey::location).orElse(null);

        List<WorldEventType> candidates = new ArrayList<>();
        for (WorldEventType type : WorldEventType.values()) {
            if (type.isRealmOnly() != inRealm || type.isDeepOnly() != inDeep
                    || (type.isOverworldOnly() && !inOverworld)
                    || isEventDisabled(type)) {
                continue;
            }
            if (type.getRequiredBiome() != null
                    && !type.getRequiredBiome().equals(anchorBiome)) {
                continue;
            }
            if (type == WorldEventType.LOST_CARAVAN
                    && (!ServerConfig.LOST_TRAVELER_CAMP_EVENT.get()
                        || !level.dimension().equals(Level.OVERWORLD))) {
                continue;
            }
            candidates.add(type);
        }
        if (candidates.isEmpty()) {
            return;
        }
        WorldEventType type = WorldEventTuning.select(candidates, level.random);
        if (type == null) return;

        for (int attempt = 0; attempt < ServerConfig.WORLD_EVENT_PLACEMENT_ATTEMPTS.get(); attempt++) {
            if (!canStartEvent(level, events, type)) return;
            BlockPos pos = pickPosition(level, anchor.blockPosition(), type, level.random);
            if (pos == null) {
                continue;
            }
            long duration = type == WorldEventType.LOST_CARAVAN
                    ? ServerConfig.LOST_TRAVELER_STAY_TICKS.get()
                    : ServerConfig.WORLD_EVENT_DURATION_TICKS.get();
            ActiveWorldEvent active = new ActiveWorldEvent(type, pos, now,
                    safeDeadline(now, duration));
            if (!canStartEvent(level, events, type)) return;
            if (type == WorldEventType.LOST_CARAVAN && !placeCaravan(level, events, active)) {
                continue;
            }
            events.add(active);
            announce(level, active);
            return;
        }

    }

    private static boolean canStartEvent(ServerLevel level, List<ActiveWorldEvent> events,
                                         WorldEventType type) {
        return ServerConfig.WORLD_EVENTS_ENABLED.get()
                && !isEventDisabled(type)
                && ACTIVE.get(level.dimension()) == events
                && events.size() < ServerConfig.WORLD_EVENT_MAX_ACTIVE.get();
    }

    private static boolean isEventDisabled(WorldEventType type) {
        return ServerConfig.DISABLED_WORLD_EVENTS.get().contains(type.getId())
                || RealmDisasterManager.supports(type) && !ServerConfig.REALM_DISASTERS_ENABLED.get()
                || type == WorldEventType.LOST_CARAVAN
                && !ServerConfig.LOST_TRAVELER_CAMP_EVENT.get();
    }

    /** 选点必须已加载、群系匹配、不压结构且地表合法；距离配置不绕过这些检查。 */
    private static BlockPos pickPosition(ServerLevel level, BlockPos origin,
                                         WorldEventType type, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        int dist = WorldEventTuning.distance(random);
        int x = origin.getX() + (int) (Math.cos(angle) * dist);
        int z = origin.getZ() + (int) (Math.sin(angle) * dist);
        if (!level.isLoaded(new BlockPos(x, origin.getY(), z))) {
            return null;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        if (!level.getWorldBorder().isWithinBounds(surface)) {
            return null;
        }

        if (LoadedStructureLookup.mayContainStructureAt(level, surface, structure -> true)) {
            return null;
        }
        if (RealmDisasterManager.supports(type) && !RealmDisasterManager.safePosition(level, surface)) {
            return null;
        }

        if (type.getRequiredBiome() != null) {
            var biomeKey = level.getBiome(surface).unwrapKey();
            if (biomeKey.isEmpty() || !biomeKey.get().location().equals(type.getRequiredBiome())) {
                return null;
            }
        }

        List<ActiveWorldEvent> events = ACTIVE.getOrDefault(level.dimension(), List.of());
        for (ActiveWorldEvent active : events) {
            int spacing = ServerConfig.WORLD_EVENT_MIN_SPACING.get();
            if (active.center.distSqr(surface) < (long) spacing * spacing) {
                return null;
            }
        }

        return switch (type) {
            case MIRROR_ANOMALY -> level.getBlockState(surface.below()).is(Blocks.WATER) ? surface : null;
            case LOST_CARAVAN -> isSafeCaravanSite(level, surface) ? surface : null;
            case METEOR_AFTERGLOW -> level.canSeeSky(surface) && !level.isDay() ? surface : null;
            case FAR_BELL_PROCESSION -> level.canSeeSky(surface) ? surface : null;
            default -> surface;
        };
    }

    /** 只在露天、天然且平整的地表驻留，避免旅者刷进玩家建筑。 */
    private static boolean isSafeCaravanSite(ServerLevel level, BlockPos center) {
        if (!level.canSeeSky(center)) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunkAt(center.offset(dx, 0, dz))) {
                    return false;
                }
            }
        }
        if (!level.noCollision(null,
                new net.minecraft.world.phys.AABB(center).inflate(1.0D, 0.0D, 1.0D))) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                        center.offset(dx, 0, dz));
                if (Math.abs(surface.getY() - center.getY()) > 1
                        || !level.canSeeSky(surface)
                        || !level.getBlockState(surface).isAir()
                        || !level.getBlockState(surface.above()).isAir()
                        || !isNaturalCampGround(level.getBlockState(surface.below()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNaturalCampGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SNOW_BLOCK);
    }

    /** 失落商队：只生成临时旅者；营地氛围由事件粒子与音效表现，不改世界方块。 */
    private static boolean placeCaravan(ServerLevel level, List<ActiveWorldEvent> events,
                                         ActiveWorldEvent active) {
        var traveler = ModEntities.LOST_TRAVELER.get().create(level);
        if (traveler == null) {
            return false;
        }
        boolean placed = false;
        try {
            BlockPos pos = active.center;
            traveler.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, traveler)) return false;
            traveler.startTemporaryStay(pos, active.startedGameTime);
            if (!level.addFreshEntity(traveler)) return false;
            // 加入和追踪回调可移除实体或改变事件容量，原生成功返回值不足以提交营地。
            placed = EntitySpawnSafety.isLoadedAndCollisionFree(level, traveler)
                    && traveler.isAlive()
                    && traveler.level() == level
                    && level.getEntity(traveler.getUUID()) == traveler
                    && traveler.blockPosition().equals(pos)
                    && !active.isExpired(level.getGameTime())
                    && canStartEvent(level, events, active.type);
            return placed;
        } finally {
            if (!placed) traveler.discard();
        }
    }

    private static void announce(ServerLevel level, ActiveWorldEvent active) {
        int radius = eventRadiusFor(active.type);
        for (ServerPlayer player : playersNear(level, active.center, radius * 2, true)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_event." + active.type.getId() + ".start"), true);
        }
    }

    /** 事件表现 + 个人目击记录；同一轮只查询一次附近玩家并传给效果。 */
    private static void tickEvent(ServerLevel level, ActiveWorldEvent active, long now) {
        if (active.isStopped()) return;
        RealmDisasterManager.presentScene(level, active, now);
        int radius = eventRadiusFor(active.type);
        List<ServerPlayer> nearbyPlayers = playersNear(
                level, active.center, radius, false);
        if (active.testMode) {
            RealmDisasterManager.tick(level, active, nearbyPlayers, now);
            return;
        }
        if (nearbyPlayers.isEmpty()) {
            return;
        }
        for (ServerPlayer player : nearbyPlayers) {
            if (active.isStopped()) return;

            if (!active.witnessed.contains(player.getUUID())) {
                String key = "world_event:" + active.type.getId();
                if (EchoAbilityManager.hasActivatedMechanism(player, key)) {
                    active.witnessed.add(player.getUUID());
                } else if (EchoAbilityManager.activateMechanisms(player, () -> {
                    active.witnessed.add(player.getUUID());
                    if (active.type.getEnergyReward() > 0) {
                        ArtifactManager.refillEnergy(player, active.type.getEnergyReward());
                    }
                }, key)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.world_event." + active.type.getId() + ".witness")
                            .withStyle(net.minecraft.ChatFormatting.DARK_AQUA,
                                    net.minecraft.ChatFormatting.ITALIC));
                }
            }
        }
        if (active.isStopped()) return;
        if (!active.ecologyResponded
                && RealmWorldEventEcologyManager.supports(active.type)) {
            active.ecologyResponded = true;
            active.floraResponseGameTime = safeDeadline(now, 40L);
            RealmWorldEventEcologyManager.respond(level, active.type, active.center);
        }
        if (active.isStopped()) return;
        if (!active.floraResponded
                && now >= active.floraResponseGameTime
                && RealmWorldEventFloraManager.supports(active.type)) {
            active.floraResponded = true;
            RealmWorldEventFloraManager.respond(level, active.type, active.center);
        }
        if (active.isStopped()) return;
        presentEvent(level, active, nearbyPlayers, now);
    }

    private static List<ServerPlayer> playersNear(ServerLevel level, BlockPos center,
                                                   int radius, boolean includeSpectators) {
        long radiusSqr = (long) radius * radius;
        return level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(center).inflate(radius),
                player -> (includeSpectators || !player.isSpectator())
                        && player.distanceToSqr(center.getX() + 0.5D,
                        center.getY() + 0.5D, center.getZ() + 0.5D) <= radiusSqr);
    }

    private static boolean withinEventRadius(LivingEntity entity, BlockPos center,
                                             double radiusSquared) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz <= radiusSquared;
    }

    /** 给事件范围内的普通敌对生物施加效果。必须排除本 mod 与其他 mod 的 Boss,并用圆形收口。 */
    private static void applyToNearbyHostiles(ServerLevel level, BlockPos center,
                                              double verticalRange,
                                              net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                              int duration, int amplifier) {
        int radius = eventRadius();
        double radiusSquared = (double) radius * radius;
        AABB area = new AABB(center).inflate(radius, verticalRange, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof Enemy && entity.isAlive()
                        && !entity.getType().is(ModEntityTags.ECHO_BOSSES)
                        && !entity.getType().is(Tags.EntityTypes.BOSSES)
                        && withinEventRadius(entity, center, radiusSquared))) {
            addEffectIfMissing(target, effect, duration, amplifier);
        }
    }

    /** 天灾判定与环境活动各自归属对应处理器，共享附近玩家快照。 */
    private static void presentEvent(ServerLevel level, ActiveWorldEvent active,
                                     List<ServerPlayer> nearbyPlayers, long now) {
        RandomSource random = level.random;
        BlockPos center = active.center;
        boolean effectPulse = Math.floorMod(now, (long) WORLD_EVENT_EFFECT_PULSE_TICKS) == 0L;
        switch (active.type) {
            case RIFT_STORM, STRATA_RUPTURE -> RealmDisasterManager.tick(level, active, nearbyPlayers, now);
            case ECHO_FOG -> presentEchoFog(level, center, random, nearbyPlayers);
            case MIRROR_ANOMALY -> presentMirrorAnomaly(level, center, random, nearbyPlayers);
            case STORM_OMEN -> presentStormOmen(level, center, random, nearbyPlayers);
            case LOST_CARAVAN -> presentLostCaravan(level, center, random);
            case FAR_BELL_PROCESSION -> presentFarBellProcession(level, center, random, effectPulse, nearbyPlayers, now);
            case MEMORY_FIREFLY_TIDE -> presentMemoryFireflyTide(level, center, random, effectPulse, nearbyPlayers);
            case GRAVITY_TIDE -> presentGravityTide(level, center, random, effectPulse, nearbyPlayers);
            case ARCHIVE_DRIFT -> presentArchiveDrift(level, center, random, effectPulse, nearbyPlayers);
            case SILENT_PULSE -> presentSilentPulse(level, center, random, effectPulse, nearbyPlayers);
            case FOLDED_PROCESSION -> presentFoldedProcession(level, center, random, effectPulse, nearbyPlayers, now);
            case CRYSTAL_CHORUS -> presentCrystalChorus(level, center, random, effectPulse, nearbyPlayers);
            case RIFT_AFTERIMAGE -> presentRiftAfterimage(level, center, random, effectPulse, nearbyPlayers);
            case INTERLAYER_RESPITE -> presentInterlayerRespite(level, center, random, effectPulse, nearbyPlayers, now);
            // 这三类的表现归 RealmAmbientEventManager 持有,不要在本类再写一份。
            case METEOR_AFTERGLOW, ECHO_STORM, STRATA_AFTERSHOCK ->
                    RealmAmbientEventManager.present(level, active.type, center, nearbyPlayers);
        }
    }

    private static void presentEchoFog(ServerLevel level, BlockPos center,
                                     RandomSource random, List<ServerPlayer> nearbyPlayers) {
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 6; i++) {
                level.sendParticles(ParticleTypes.WHITE_ASH,
                        center.getX() + random.nextGaussian() * 10,
                        center.getY() + 0.5 + random.nextDouble() * 2,
                        center.getZ() + random.nextGaussian() * 10,
                        1, 0.5, 0.1, 0.5, 0.003);
            }
            level.sendParticles(ParticleTypes.WHITE_ASH,
                    center.getX(), center.getY() + 1.5, center.getZ(),
                    6, 9.0, 0.5, 9.0, 0.005);
        }
        if (Math.floorMod(level.getGameTime(), 160L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_ECHO_FOG_PULSE.get(), 0.5F, 1.0F);
        }
    }

    private static void presentMirrorAnomaly(ServerLevel level, BlockPos center,
                                     RandomSource random, List<ServerPlayer> nearbyPlayers) {
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 4; i++) {
                level.sendParticles(ParticleTypes.END_ROD,
                        center.getX() + random.nextGaussian() * 6,
                        center.getY() + random.nextDouble() * 0.4,
                        center.getZ() + random.nextGaussian() * 6,
                        1, 0.0, 0.12, 0.0, 0.02);
            }
            level.sendParticles(ParticleTypes.GLOW,
                    center.getX(), center.getY() + 0.3, center.getZ(),
                    5, 5.0, 0.2, 5.0, 0.0);
        }
        if (Math.floorMod(level.getGameTime(), 140L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_MIRROR_ANOMALY_PULSE.get(), 0.7F, 1.0F);
        }
    }

    private static void presentStormOmen(ServerLevel level, BlockPos center,
                                     RandomSource random, List<ServerPlayer> nearbyPlayers) {
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 5; i++) {
                level.sendParticles(ParticleTypes.CLOUD,
                        center.getX() + random.nextGaussian() * 8,
                        center.getY() + 6 + random.nextDouble() * 3,
                        center.getZ() + random.nextGaussian() * 8,
                        1, 0.4, 0.05, 0.4, 0.06);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    center.getX(), center.getY() + 4, center.getZ(),
                    4, 6.0, 2.0, 6.0, 0.05);
        }
        if (Math.floorMod(level.getGameTime(), 240L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_STORM_OMEN_PULSE.get(), 0.25F, 1.0F);
        }
    }

    private static void presentLostCaravan(ServerLevel level, BlockPos center,
                                     RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.LAVA,
                    center.getX() + 0.5, center.getY() + 0.4, center.getZ() + 0.5,
                    1, 0.2, 0.1, 0.2, 0.0);
        }
    }

    private static void presentFarBellProcession(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers, long now) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                addEffectIfMissing(player, MobEffects.MOVEMENT_SPEED, 60);
                addEffectIfMissing(player, MobEffects.DIG_SPEED, 40);
            }
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            long phase = now / 10L;
            for (int step = -14; step <= 14; step += 2) {
                double drift = Math.sin((step + phase) * 0.35D) * 3.0D;
                level.sendParticles(step % 4 == 0 ? ParticleTypes.NOTE : ParticleTypes.GLOW,
                        center.getX() + step,
                        center.getY() + 0.6D + Math.floorMod(step, 3) * 0.25D,
                        center.getZ() + drift,
                        1, 0.05D, 0.08D, 0.05D, 0.0D);
            }
        }
        if (Math.floorMod(level.getGameTime(), 200L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_FAR_BELL_PROCESSION_PULSE.get(), 0.45F, 1.0F);
        }
    }

    private static void presentMemoryFireflyTide(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                refreshNightVision(level, player);
            }
            applyToNearbyHostiles(level, center, 8.0D,
                    MobEffects.GLOWING, 40, 0);
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 20; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 3.0D + random.nextDouble() * 12.0D;
                double y = center.getY() + 0.4D + random.nextDouble() * 4.0D;
                level.sendParticles(i % 4 == 0
                                ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.GLOW,
                        center.getX() + Math.cos(angle) * radius, y,
                        center.getZ() + Math.sin(angle) * radius,
                        1, 0.04D, 0.06D, 0.04D, 0.0D);
            }
        }
        if (Math.floorMod(level.getGameTime(), 120L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_MEMORY_FIREFLY_TIDE_PULSE.get(), 0.45F, 1.0F);
        }
    }

    private static void presentGravityTide(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                addEffectIfMissing(player, MobEffects.SLOW_FALLING, 40);
                addEffectIfMissing(player, MobEffects.JUMP, 40);
            }
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 16; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 4.0D + random.nextDouble() * 12.0D;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.getX() + Math.cos(angle) * radius,
                        center.getY() + 0.2D + random.nextDouble() * 1.4D,
                        center.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, 0.06D, 0.0D, 0.0D);
            }
        }
        if (Math.floorMod(level.getGameTime(), 140L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_GRAVITY_TIDE_PULSE.get(), 0.55F, 1.0F);
        }
    }

    private static void presentArchiveDrift(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                refreshNightVision(level, player);
            }
            applyToNearbyHostiles(level, center, 8.0D,
                    MobEffects.GLOWING, 40, 0);
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.getX(), center.getY() + 1.5D, center.getZ(),
                    18, 10.0D, 1.0D, 10.0D, 0.2D);
        }
        if (Math.floorMod(level.getGameTime(), 100L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_ARCHIVE_DRIFT_PULSE.get(), 0.8F, 1.0F);
        }
    }

    private static void presentSilentPulse(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                addEffectIfMissing(player, MobEffects.DAMAGE_RESISTANCE, 40);
            }
            applyToNearbyHostiles(level, center, 6.0D,
                    MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    center.getX(), center.getY() + 0.25D, center.getZ(),
                    22, 11.0D, 0.4D, 11.0D, 0.01D);
        }
        if (Math.floorMod(level.getGameTime(), 120L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_SILENT_PULSE_PULSE.get(), 0.45F, 1.0F);
        }
    }

    private static void presentFoldedProcession(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers, long now) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                addEffectIfMissing(player, MobEffects.MOVEMENT_SPEED, 40);
            }
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = -12; i <= 12; i += 2) {
                boolean turn = Math.floorMod(i + (int) (now / 10L), 8) < 4;
                double x = center.getX() + (turn ? i : i * 0.35D);
                double z = center.getZ() + (turn ? i * 0.35D : i);
                level.sendParticles(ParticleTypes.END_ROD,
                        x, center.getY() + 0.35D + Math.floorMod(i, 3) * 0.25D, z,
                        1, 0.05D, 0.08D, 0.05D, 0.0D);
            }
            level.sendParticles(ParticleTypes.GLOW,
                    center.getX(), center.getY() + 0.55D, center.getZ(),
                    8, 8.0D, 0.5D, 8.0D, 0.0D);
        }
        if (Math.floorMod(level.getGameTime(), 160L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_FOLDED_PROCESSION_PULSE.get(), 0.55F, 1.0F);
        }
    }

    private static void presentCrystalChorus(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                addEffectIfMissing(player, MobEffects.SLOW_FALLING, 40);
                refreshNightVision(level, player);
            }
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int i = 0; i < 18; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 3.0D + random.nextDouble() * 11.0D;
                double y = center.getY() + 0.5D + random.nextDouble() * 6.0D;
                level.sendParticles(i % 3 == 0
                                ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                        center.getX() + Math.cos(angle) * radius, y,
                        center.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, 0.04D, 0.0D, 0.0D);
            }
        }
        if (Math.floorMod(level.getGameTime(), 180L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_CRYSTAL_CHORUS_PULSE.get(), 0.6F, 1.0F);
        }
    }

    private static void presentRiftAfterimage(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers) {
        if (effectPulse) {
            applyToNearbyHostiles(level, center, 7.0D,
                    MobEffects.WEAKNESS, 40, 0);
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    center.getX(), center.getY() + 1.0D, center.getZ(),
                    20, 10.0D, 2.0D, 10.0D, 0.05D);
            level.sendParticles(ParticleTypes.PORTAL,
                    center.getX(), center.getY() + 0.25D, center.getZ(),
                    12, 7.0D, 0.35D, 7.0D, 0.08D);
        }
        if (Math.floorMod(level.getGameTime(), 160L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_RIFT_AFTERIMAGE_PULSE.get(), 0.35F, 1.0F);
        }
    }

    private static void presentInterlayerRespite(ServerLevel level, BlockPos center,
                                     RandomSource random, boolean effectPulse, List<ServerPlayer> nearbyPlayers, long now) {
        if (effectPulse) {
            for (ServerPlayer player : nearbyPlayers) {
                refreshNightVision(level, player);
                addEffectIfMissing(player, MobEffects.ABSORPTION, 80);
            }
        }
        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            for (int ring = 0; ring < 3; ring++) {
                double radius = 5.0D + ring * 4.0D;
                double offset = now * 0.015D * (ring % 2 == 0 ? 1.0D : -1.0D);
                for (int point = 0; point < 8; point++) {
                    double angle = point * Math.PI / 4.0D + offset;
                    level.sendParticles(ring == 1
                                    ? ParticleTypes.SCULK_SOUL : ParticleTypes.END_ROD,
                            center.getX() + Math.cos(angle) * radius,
                            center.getY() + 0.35D + ring * 0.55D,
                            center.getZ() + Math.sin(angle) * radius,
                            1, 0.02D, 0.04D, 0.02D, 0.0D);
                }
            }
        }
        if (Math.floorMod(level.getGameTime(), 200L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_INTERLAYER_RESPITE_PULSE.get(), 0.45F, 1.0F);
        }
    }

    public record WorldEventSignal(WorldEventType type, BlockPos center) {
    }

    /** 周期性事件仅在效果缺失或即将过期时补发；夜视走独立阈值。 */
    private static void addEffectIfMissing(LivingEntity entity,
                                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration) {
        addEffectIfMissing(entity, effect, duration, 0);
    }

    private static void addEffectIfMissing(LivingEntity entity,
                                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration, int amplifier) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current != null && (current.getAmplifier() > amplifier
                || current.getDuration() < 0
                || current.getAmplifier() == amplifier
                && current.getDuration() > Math.max(1, duration / 2))) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    /** 事件夜视按 1 秒脉冲补时长。 */
    private static void refreshNightVision(ServerLevel level, ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && (current.getAmplifier() > 0 || current.isInfiniteDuration())) {

            return;
        }
        int threshold = Math.max(NightVisionSupport.REFRESH_THRESHOLD_TICKS,
                ServerConfig.WORLD_EVENT_NIGHT_VISION_REFRESH_THRESHOLD_TICKS.get());
        int duration = Math.max(ServerConfig.WORLD_EVENT_NIGHT_VISION_DURATION_TICKS.get(),
                threshold + 2 * WORLD_EVENT_EFFECT_PULSE_TICKS);

        NightVisionSupport.sustain(player, NightVisionSupport.Source.AREA_EVENT, duration,
                WORLD_EVENT_EFFECT_PULSE_TICKS, true,
                level.getServer().overworld().getGameTime());
    }

    private static long safeDeadline(long now, long duration) {
        if (now < 0L) {
            now = 0L;
        }
        if (duration <= 0L) {
            return now;
        }
        return duration > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
    }
}
