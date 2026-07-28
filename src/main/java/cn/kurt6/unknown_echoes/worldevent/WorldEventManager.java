package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界事件框架第一版(V0.6D,22.10):统一注册表 + 触发器 + 频率/开关配置 + 个人参与记录。
 * 核心边界:只在有玩家的已加载区块附近触发;无人维度零开销;找不到安全位置直接取消;
 * 事件只做表现层与普通氛围,不生成结构、不发关键奖励、不替代主线(16 章)。
 * 首批实例:回响雾 / 镜湖倒影异常 / 风暴前兆(回声境域)+ 失落商队(主世界,16.4)。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class WorldEventManager {

    /** 事件表现/目击半径。 */
    private static final int EVENT_RADIUS = 32;
    /** 新事件与既有事件的最小间距。 */
    private static final int MIN_EVENT_SPACING = 96;
    /** 选点尝试次数(全部失败则本次取消,22.10 流程 4)。 */
    private static final int PLACEMENT_ATTEMPTS = 8;

    private static final Map<ResourceKey<Level>, List<ActiveWorldEvent>> ACTIVE = new HashMap<>();

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !ServerConfig.WORLD_EVENTS_ENABLED.get()
                || level.players().isEmpty()) {
            return; // 无人维度零 tick 行为
        }
        List<ActiveWorldEvent> events = ACTIVE.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        long now = level.getGameTime();

        // 事件记录到期后移除；失落商队不放置世界方块，旅者实体自行离场。
        events.removeIf(active -> now >= active.endGameTime);

        // 表现与目击:每 10 tick 一轮,只对附近玩家生效
        if (now % 10 == 0) {
            for (ActiveWorldEvent active : events) {
                tickEvent(level, active);
            }
        }

        // 触发滚动
        if (now % ServerConfig.WORLD_EVENT_CHECK_INTERVAL_TICKS.get() != 0
                || events.size() >= ServerConfig.WORLD_EVENT_MAX_ACTIVE.get()
                || level.random.nextDouble() >= ServerConfig.WORLD_EVENT_CHANCE.get()) {
            return;
        }
        tryStartEvent(level, events);
    }

    /** 选一名在线玩家,按维度/群系挑可触发事件并寻找安全位置;失败则本次取消。 */
    private static void tryStartEvent(ServerLevel level, List<ActiveWorldEvent> events) {
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator())
                .toList();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        boolean inRealm = level.dimension().location().equals(EchoPermission.ECHO_REALM_ID);

        List<WorldEventType> candidates = new ArrayList<>();
        for (WorldEventType type : WorldEventType.values()) {
            if (type.isRealmOnly() != inRealm
                    || ServerConfig.DISABLED_WORLD_EVENTS.get().contains(type.getId())) {
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
        WorldEventType type = candidates.get(level.random.nextInt(candidates.size()));

        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            BlockPos pos = pickPosition(level, anchor.blockPosition(), type, level.random);
            if (pos == null) {
                continue;
            }
            long duration = type == WorldEventType.LOST_CARAVAN
                    ? ServerConfig.LOST_TRAVELER_STAY_TICKS.get()
                    : ServerConfig.WORLD_EVENT_DURATION_TICKS.get();
            ActiveWorldEvent active = new ActiveWorldEvent(type, pos,
                    level.getGameTime() + duration);
            if (type == WorldEventType.LOST_CARAVAN && !placeCaravan(level, pos)) {
                continue;
            }
            events.add(active);
            announce(level, active);
            return;
        }
        // 全部尝试失败:取消本次事件,不强行生成(22.10)
    }

    /** 选点:玩家附近 40-60 格,已加载、群系匹配、不压结构、地表合法。失败返回 null。 */
    private static BlockPos pickPosition(ServerLevel level, BlockPos origin,
                                         WorldEventType type, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        int dist = 40 + random.nextInt(21);
        int x = origin.getX() + (int) (Math.cos(angle) * dist);
        int z = origin.getZ() + (int) (Math.sin(angle) * dist);
        if (!level.isLoaded(new BlockPos(x, origin.getY(), z))) {
            return null;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        // 结构黑名单:不在任何已注册结构内触发(村庄/遗迹/Boss 场地一并避开,22.10)
        if (!level.structureManager().getAllStructuresAt(surface).isEmpty()) {
            return null;
        }
        // 群系匹配
        if (type.getRequiredBiome() != null) {
            var biomeKey = level.getBiome(surface).unwrapKey();
            if (biomeKey.isEmpty() || !biomeKey.get().location().equals(type.getRequiredBiome())) {
                return null;
            }
        }
        // 事件间距
        List<ActiveWorldEvent> events = ACTIVE.getOrDefault(level.dimension(), List.of());
        for (ActiveWorldEvent active : events) {
            if (active.center.distSqr(surface) < (long) MIN_EVENT_SPACING * MIN_EVENT_SPACING) {
                return null;
            }
        }
        // 事件专属落点要求
        return switch (type) {
            case MIRROR_ANOMALY -> level.getBlockState(surface.below()).is(Blocks.WATER) ? surface : null;
            case LOST_CARAVAN -> isSafeCaravanSite(level, surface) ? surface : null;
            default -> surface;
        };
    }

    /** 只在露天、天然且平整的地表驻留，避免旅者刷进玩家建筑。 */
    private static boolean isSafeCaravanSite(ServerLevel level, BlockPos center) {
        if (!level.canSeeSky(center) || !level.noCollision(null,
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
    private static boolean placeCaravan(ServerLevel level, BlockPos pos) {
        var traveler = ModEntities.LOST_TRAVELER.get().create(level);
        if (traveler == null) {
            return false;
        }
        traveler.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(traveler)) {
            return false;
        }
        traveler.startTemporaryStay(pos, level.getGameTime());
        return level.addFreshEntity(traveler);
    }

    /** 事件开始:给附近玩家一条含蓄氛围文案(红线 #7)。 */
    private static void announce(ServerLevel level, ActiveWorldEvent active) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(active.center) <= (long) (EVENT_RADIUS * 2) * (EVENT_RADIUS * 2)) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_event." + active.type.getId() + ".start"), true);
            }
        }
    }

    /** 事件表现 + 个人目击记录(只对附近玩家;无人靠近时只做极轻量距离判断)。 */
    private static void tickEvent(ServerLevel level, ActiveWorldEvent active) {
        boolean anyoneNear = false;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            double distSqr = player.blockPosition().distSqr(active.center);
            if (distSqr > (long) EVENT_RADIUS * EVENT_RADIUS) {
                continue;
            }
            anyoneNear = true;
            // 个人参与记录:首次目击写入个人机关数据(持久),并给一次含蓄提示
            if (active.witnessed.add(player.getUUID())) {
                String key = "world_event:" + active.type.getId();
                if (!EchoAbilityManager.hasActivatedMechanism(player, key)) {
                    EchoAbilityManager.activateMechanism(player, key);
                    player.sendSystemMessage(Component.translatable(
                            "message.unknown_echoes.world_event." + active.type.getId() + ".witness")
                            .withStyle(net.minecraft.ChatFormatting.DARK_AQUA,
                                    net.minecraft.ChatFormatting.ITALIC));
                }
            }
        }
        if (!anyoneNear) {
            return; // 无人区域不做任何表现
        }
        presentEvent(level, active);
    }

    /** 表现层:粒子与偶发音效(只改氛围,不改方块、不改实体行为)。 */
    private static void presentEvent(ServerLevel level, ActiveWorldEvent active) {
        RandomSource random = level.random;
        BlockPos center = active.center;
        switch (active.type) {
            case ECHO_FOG -> {
                for (int i = 0; i < 6; i++) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            center.getX() + random.nextGaussian() * 10,
                            center.getY() + 0.5 + random.nextDouble() * 2,
                            center.getZ() + random.nextGaussian() * 10,
                            1, 0.5, 0.1, 0.5, 0.003);
                }
                level.sendParticles(ParticleTypes.WHITE_ASH,
                        center.getX(), center.getY() + 1.5, center.getZ(),
                        12, 9.0, 1.5, 9.0, 0.01);
                if (random.nextInt(20) == 0) {
                    level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.AMBIENT, 0.5F, 0.6F);
                }
            }
            case MIRROR_ANOMALY -> {
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
                if (random.nextInt(24) == 0) {
                    level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.AMBIENT, 0.7F, 0.5F);
                }
            }
            case STORM_OMEN -> {
                for (int i = 0; i < 5; i++) {
                    level.sendParticles(ParticleTypes.CLOUD,
                            center.getX() + random.nextGaussian() * 8,
                            center.getY() + 2 + random.nextDouble() * 4,
                            center.getZ() + random.nextGaussian() * 8,
                            1, 0.4, 0.05, 0.4, 0.06);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.getX(), center.getY() + 4, center.getZ(),
                        4, 6.0, 2.0, 6.0, 0.05);
                if (random.nextInt(30) == 0) {
                    level.playSound(null, center, SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.AMBIENT, 0.25F, 0.7F);
                }
            }
            case LOST_CARAVAN -> {
                // 不放置篝火；用低频暖色火星标记旅者的临时停留点。
                if (random.nextInt(4) == 0) {
                    level.sendParticles(ParticleTypes.LAVA,
                            center.getX() + 0.5, center.getY() + 0.4, center.getZ() + 0.5,
                            1, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }
}
