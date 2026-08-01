package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public final class ArtifactEffectManager {
    private static final ResourceLocation[] TIDE_HEALTH_IDS = {
            UnknownEchoes.id("tide_lantern_health_1"),
            UnknownEchoes.id("tide_lantern_health_2"),
            UnknownEchoes.id("tide_lantern_health_3")
    };
    private static final ResourceLocation STORM_MOVEMENT_ID = UnknownEchoes.id("storm_compass_movement");
    private static final ResourceLocation LENS_ATTACK_SPEED_ID = UnknownEchoes.id("echo_lens_attack_speed");
    private static final double LENS_RANGE = 24.0D;

    private static final Map<UUID, StormFlight> STORM_FLIGHTS = new HashMap<>();
    private static final Map<UUID, Long> STORM_SAFE_FALL_ENDS = new HashMap<>();
    private static final Map<UUID, Long> TIDE_AUTO_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<UUID, LensMark>> LENS_MARKS = new HashMap<>();
    private static final Map<UUID, Long> LENS_FOCUS_ENDS = new HashMap<>();
    private static final Map<UUID, PendingAnchorTeleport> PENDING_ANCHOR_TELEPORTS = new HashMap<>();
    private static final Map<UUID, Long> TIME_FREEZE_ENDS = new HashMap<>();
    private static final Map<UUID, Vec3> TIME_FREEZE_MOTION = new HashMap<>();
    private static long timeFreezeReleaseFxTick = Long.MIN_VALUE;
    private static int timeFreezeReleaseFxCount;
    private static long timeFreezeCleanupTick = Long.MIN_VALUE;

    private ArtifactEffectManager() {
    }

    public static boolean use(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        if (!ArtifactManager.isEnabled(type)) {
            return false;
        }
        return switch (type) {
            case STORM_COMPASS -> useStormCompass(player);
            case TIDE_LANTERN -> useTideLantern(player);
            case ECHO_LENS -> useEchoLens(player);
            case TIME_HOURGLASS -> useTimeHourglass(player);
            case DIMENSION_ANCHOR -> useDimensionAnchor(player, hand);
            case RIFT_ANCHOR -> useRiftAnchor(player, hand);
        };
    }

    /** 快捷键与物品右键共用的服务端入口；凭据校验由调用方完成。 */
    public static boolean useFromKey(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        if (ArtifactManager.isOnCooldown(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.generic_cooldown");
            return false;
        }
        return use(player, type, hand);
    }

    public static int getEnergyCost(ServerPlayer player, ArtifactType type) {
        int cost = switch (type) {
            case STORM_COMPASS -> ServerConfig.STORM_COMPASS_ENERGY_COST.get();
            case TIDE_LANTERN -> ServerConfig.TIDE_LANTERN_ENERGY_COST.get();
            case ECHO_LENS -> ServerConfig.ECHO_LENS_ENERGY_COST.get();
            case TIME_HOURGLASS -> ServerConfig.TIME_HOURGLASS_ENERGY_COST.get();
            case DIMENSION_ANCHOR -> ServerConfig.DIMENSION_ANCHOR_ENERGY_COST.get();
            case RIFT_ANCHOR -> ServerConfig.RIFT_ANCHOR_ENERGY_COST.get();
        };
        if (type == ArtifactType.STORM_COMPASS && EchoArmorSets.windWalkerPieces(player) >= 4) {
            cost = Math.round(cost * 0.90F);
        }
        String tuning = ArtifactManager.getData(player).getTuning(type);
        if (type == ArtifactType.TIME_HOURGLASS && "shorthand".equals(tuning)
                || type == ArtifactType.DIMENSION_ANCHOR && "thrift".equals(tuning)) {
            cost = Math.round(cost * 0.75F);
        }
        return cost;
    }

    public static int getCooldownSeconds(ServerPlayer player, ArtifactType type) {
        int level = level(player, type);
        return switch (type) {
            case STORM_COMPASS -> Math.max(0,
                    ServerConfig.STORM_COMPASS_COOLDOWN_SECONDS.get() - Math.max(0, level - 1) * 20);
            case TIDE_LANTERN -> ServerConfig.TIDE_LANTERN_COOLDOWN_SECONDS.get();
            case ECHO_LENS -> ServerConfig.ECHO_LENS_COOLDOWN_SECONDS.get();
            case TIME_HOURGLASS -> ServerConfig.TIME_HOURGLASS_COOLDOWN_SECONDS.get();
            case DIMENSION_ANCHOR -> ServerConfig.DIMENSION_ANCHOR_COOLDOWN_SECONDS.get();
            case RIFT_ANCHOR -> {
                int base = ServerConfig.RIFT_ANCHOR_COOLDOWN_SECONDS.get();
                yield "silent_fold".equals(ArtifactManager.getData(player).getTuning(type))
                        ? Math.max(1, Math.round(base * 0.75F)) : base;
            }
        };
    }

    public static void tick(ServerPlayer player) {
        tickStormFlight(player);
        ArtifactAnchorHelper.tickRiftAnchor(player);
        if (player.tickCount % 40 == 0) {
            applyPassiveAttributes(player);
            tickEchoLensPassive(player);
            cleanupLensMarks(player);
            cleanupTimeFreeze(ArtifactManager.gameTime(player));
        }
        if (player.tickCount % 20 == 0) {
            tickTideAutoGuard(player);
        }
        if (player.tickCount % 200 == 0) {
            tickTideSustain(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        stopStormFlight(player, false);
        UUID id = player.getUUID();
        STORM_SAFE_FALL_ENDS.remove(id);
        TIDE_AUTO_COOLDOWNS.remove(id);
        LENS_MARKS.remove(id);
        LENS_FOCUS_ENDS.remove(id);
        PENDING_ANCHOR_TELEPORTS.remove(id);
        TIME_FREEZE_ENDS.remove(id);
        TIME_FREEZE_MOTION.remove(id);
    }

    /** 服务器停止时清理所有瞬态神器状态,避免单机世界切换串状态。 */
    public static void clearAll() {
        STORM_FLIGHTS.clear();
        STORM_SAFE_FALL_ENDS.clear();
        TIDE_AUTO_COOLDOWNS.clear();
        LENS_MARKS.clear();
        LENS_FOCUS_ENDS.clear();
        PENDING_ANCHOR_TELEPORTS.clear();
        TIME_FREEZE_ENDS.clear();
        TIME_FREEZE_MOTION.clear();
        timeFreezeReleaseFxTick = Long.MIN_VALUE;
        timeFreezeReleaseFxCount = 0;
        timeFreezeCleanupTick = Long.MIN_VALUE;
    }

    /** 服务器保存世界前恢复所有仍加载的冻结实体，防止零速度写入存档。 */
    public static void restoreFrozenEntities(MinecraftServer server) {
        for (Map.Entry<UUID, Vec3> entry : TIME_FREEZE_MOTION.entrySet()) {
            for (ServerLevel level : server.getAllLevels()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                if (entity != null) {
                    entity.setDeltaMovement(entry.getValue());
                    entity.hurtMarked = true;
                    break;
                }
            }
        }
        TIME_FREEZE_ENDS.clear();
        TIME_FREEZE_MOTION.clear();
    }

    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasLensFocus(player)) {
            return;
        }
        var item = event.getItem().getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            event.setDuration(Math.max(0, event.getDuration() - 1));
        }
    }

    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        Long safeEnd = STORM_SAFE_FALL_ENDS.get(player.getUUID());
        if (safeEnd != null && ArtifactManager.isEnabled(ArtifactType.STORM_COMPASS) && now <= safeEnd) {
            event.setDamageMultiplier(0.0F);
            return;
        }
        if (safeEnd != null) {
            STORM_SAFE_FALL_ENDS.remove(player.getUUID());
        }
        if (level(player, ArtifactType.STORM_COMPASS) > 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.7F);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || level(player, ArtifactType.ECHO_LENS) <= 0
                || (event.getEntity() instanceof Player && !ServerConfig.ARTIFACT_PVP_EFFECTS.get())) {
            return;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null) {
            return;
        }
        LensMark mark = marks.get(event.getEntity().getUUID());
        long now = player.level().getGameTime();
        if (mark == null || mark.endTime < now || mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
            return;
        }
        float amount = event.getAmount();
        float bonus = Math.min(amount * ServerConfig.ECHO_LENS_DAMAGE_BONUS.get().floatValue(),
                ServerConfig.ECHO_LENS_DAMAGE_CAP.get().floatValue());
        if (level(player, ArtifactType.ECHO_LENS) >= 3 && isBrokenBoss(event.getEntity())) {
            bonus += amount * 0.08F;
        }
        event.setAmount(amount + bonus);
        mark.hits--;
        if (mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
        }
    }

    private static boolean useStormCompass(ServerPlayer player) {
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.STORM_COMPASS))) {
            return false;
        }
        int level = level(player, ArtifactType.STORM_COMPASS);
        long end = player.level().getGameTime()
                + (long) (ServerConfig.STORM_COMPASS_FLIGHT_SECONDS.get() + Math.max(0, level - 1) * 4) * 20L;
        StormFlight activeFlight = STORM_FLIGHTS.get(player.getUUID());
        boolean hadMayfly = activeFlight != null ? activeFlight.hadMayfly : player.getAbilities().mayfly;
        STORM_FLIGHTS.put(player.getUUID(), new StormFlight(end, level, hadMayfly));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        spawnStormCompassParticles(player);
        finishUse(player, ArtifactType.STORM_COMPASS, SoundEvents.ELYTRA_FLYING);
        return true;
    }

    private static boolean useTideLantern(ServerPlayer player) {
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.TIDE_LANTERN))) {
            return false;
        }
        applyTideRecovery(player, level(player, ArtifactType.TIDE_LANTERN), false);
        if (EchoArmorSets.tideStalkerPieces(player) >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0,
                    true, false, true));
        }
        spawnTideLanternParticles(player);
        finishUse(player, ArtifactType.TIDE_LANTERN, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean useEchoLens(ServerPlayer player) {
        int level = level(player, ArtifactType.ECHO_LENS);
        List<LivingEntity> targets = findLensTargets(player, level >= 2 ? 2 : 1);
        List<BlockPos> hidden = findAuthorizedHiddenContent(player, level);
        if (targets.isEmpty() && hidden.isEmpty()) {
            // 失败也进短冷却:扫描本身有成本,不给宏连点白嫖(风暴罗盘同款处理)
            ArtifactManager.startCooldown(player, ArtifactType.ECHO_LENS, 2);
            actionbar(player, "message.unknown_echoes.artifact.echo_lens.no_target");
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.ECHO_LENS))) {
            return false;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        boolean trueSightSet = EchoArmorSets.trueSightShadowPieces(player) >= 4;
        long end = player.level().getGameTime() + (trueSightSet ? 260L : 200L);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    trueSightSet ? 280 : 220, 0, true, false, true));
            marks.put(target.getUUID(), new LensMark(end, 3));
        }
        if (level >= 3) {
            interruptNearbyEnemies(player);
        }
        LENS_FOCUS_ENDS.put(player.getUUID(), player.level().getGameTime() + 160L);
        spawnEchoLensParticles(player, targets);
        revealAuthorizedHiddenContent(player, hidden);
        finishUse(player, ArtifactType.ECHO_LENS, SoundEvents.AMETHYST_BLOCK_RESONATE,
                targets.isEmpty()
                        ? "message.unknown_echoes.artifact.use.echo_lens.reveal"
                        : "message.unknown_echoes.artifact.use.echo_lens");
        return true;
    }

    private static boolean useTimeHourglass(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int artifactLevel = level(player, ArtifactType.TIME_HOURGLASS);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.TIME_HOURGLASS);
        int radius = 8 + artifactLevel * 2 + ("breadth".equals(tuning) ? 4 : 0);
        long duration = ServerConfig.TIME_HOURGLASS_FREEZE_TICKS.get()
                + Math.max(0, artifactLevel - 1) * 20L;
        if ("folded_memory".equals(tuning)) {
            duration = duration * 5L / 4L;
        }
        AABB area = player.getBoundingBox().inflate(radius, 5.0D, radius);
        List<net.minecraft.world.entity.Entity> targets = level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, area,
                entity -> !(entity instanceof ServerPlayer) && !entity.isRemoved());
        if (targets.isEmpty()) {
            ArtifactManager.startCooldown(player, ArtifactType.TIME_HOURGLASS, 2);
            actionbar(player, "message.unknown_echoes.artifact.time_hourglass.no_target");
            return false;
        }
        if (!ArtifactManager.spendEnergy(player,
                 getEnergyCost(player, ArtifactType.TIME_HOURGLASS))) {
            return false;
        }
        int marked = 0;
        for (net.minecraft.world.entity.Entity entity : targets) {
            freezeEntity(entity, ArtifactManager.gameTime(player) + duration);
            if (marked++ < 32) {
                level.sendParticles(ParticleTypes.END_ROD, entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        2, 0.18D, 0.25D, 0.18D, 0.01D);
            }
        }
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                42, 1.2D, 0.8D, 1.2D, 0.08D);
        spawnHorizontalRing(level, ParticleTypes.ENCHANT,
                player.position().add(0.0D, 0.12D, 0.0D), radius, Math.min(48, radius * 4), 0.02D);
        finishUse(player, ArtifactType.TIME_HOURGLASS, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        return true;
    }

    private static void freezeEntity(net.minecraft.world.entity.Entity entity, long endTime) {
        UUID id = entity.getUUID();
        TIME_FREEZE_ENDS.merge(id, endTime, Math::max);
        TIME_FREEZE_MOTION.putIfAbsent(id, entity.getDeltaMovement());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private static void cleanupTimeFreeze(long now) {
        if (timeFreezeCleanupTick != Long.MIN_VALUE && now - timeFreezeCleanupTick < 40L) {
            return;
        }
        timeFreezeCleanupTick = now;
        TIME_FREEZE_ENDS.entrySet().removeIf(entry -> {
            // 实体自身 tick 负责先恢复速度；这里仅清理离开加载区后遗留的极端陈旧记录。
            if (entry.getValue() + 200L <= now) {
                TIME_FREEZE_MOTION.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /** 在实体自身 tick 入口拦截冻结，避免按玩家重复扫描或因玩家走远而永久冻结。 */
    public static boolean onEntityTick(net.minecraft.world.entity.Entity entity) {
        if (TIME_FREEZE_ENDS.isEmpty() || entity instanceof ServerPlayer
                || !(entity.level() instanceof ServerLevel)) {
            return false;
        }
        UUID id = entity.getUUID();
        Long end = TIME_FREEZE_ENDS.get(id);
        if (end == null) {
            return false;
        }
        long now = ((ServerLevel) entity.level()).getServer().overworld().getGameTime();
        if (entity.isRemoved() || now >= end) {
            TIME_FREEZE_ENDS.remove(id);
            Vec3 motion = TIME_FREEZE_MOTION.remove(id);
            if (motion != null && !entity.isRemoved()) {
                entity.setDeltaMovement(motion);
                entity.hurtMarked = true;
                spawnFreezeReleaseFx((ServerLevel) entity.level(), entity, now);
            }
            return false;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        return true;
    }

    /** 实体卸载前恢复原速度，避免冻结状态随 NBT 保存成永久静止。 */
    public static void onEntityLeave(net.minecraft.world.entity.Entity entity) {
        UUID id = entity.getUUID();
        TIME_FREEZE_ENDS.remove(id);
        Vec3 motion = TIME_FREEZE_MOTION.remove(id);
        if (motion != null) {
            entity.setDeltaMovement(motion);
            entity.hurtMarked = true;
        }
    }

    private static void spawnFreezeReleaseFx(ServerLevel level, net.minecraft.world.entity.Entity entity, long now) {
        if (timeFreezeReleaseFxTick != now) {
            timeFreezeReleaseFxTick = now;
            timeFreezeReleaseFxCount = 0;
        }
        if (timeFreezeReleaseFxCount++ >= 32) {
            return;
        }
        level.sendParticles(ParticleTypes.POOF, entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                2, 0.15D, 0.2D, 0.15D, 0.02D);
    }

    private static boolean useDimensionAnchor(ServerPlayer player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            ArtifactAnchorHelper.BindResult result = ArtifactAnchorHelper.bindDimensionAnchor(player);
            if (!result.success()) {
                showAnchorFailure(player, ArtifactType.DIMENSION_ANCHOR, result.failure(), true);
                return false;
            }
            actionbar(player, "message.unknown_echoes.artifact.dimension_anchor.bound");
            spawnAnchorRing(player.serverLevel(), player.position().add(0.0D, 0.25D, 0.0D), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK,
                    SoundSource.PLAYERS, 0.9F, 1.1F);
            return true;
        }
        return queueAnchorTeleport(player, ArtifactType.DIMENSION_ANCHOR, hand);
    }

    private static boolean useRiftAnchor(ServerPlayer player, InteractionHand hand) {
        if (player.isShiftKeyDown() || !ArtifactAnchorHelper.hasRiftBinding(player)) {
            int artifactLevel = level(player, ArtifactType.RIFT_ANCHOR);
            String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.RIFT_ANCHOR);
            long duration = ServerConfig.RIFT_ANCHOR_LIFETIME_TICKS.get()
                    + Math.max(0, artifactLevel - 1) * 2400L;
            if ("lingering".equals(tuning)) {
                duration = duration * 3 / 2;
            }
            ArtifactAnchorHelper.BindResult result = ArtifactAnchorHelper.bindRiftAnchor(player,
                    ArtifactManager.gameTime(player) + duration);
            if (!result.success()) {
                showAnchorFailure(player, ArtifactType.RIFT_ANCHOR, result.failure(), true);
                return false;
            }
            actionbar(player, "message.unknown_echoes.artifact.rift_anchor.bound");
            spawnAnchorRing(player.serverLevel(), player.position().add(0.0D, 0.25D, 0.0D), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.PLAYERS, 0.9F, 0.7F);
            return true;
        }
        return queueAnchorTeleport(player, ArtifactType.RIFT_ANCHOR, hand);
    }

    private static boolean queueAnchorTeleport(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        UUID playerId = player.getUUID();
        if (PENDING_ANCHOR_TELEPORTS.containsKey(playerId)) {
            return true;
        }
        MinecraftServer server = player.server;
        PendingAnchorTeleport pending = new PendingAnchorTeleport(type, hand, player.level().dimension(),
                ArtifactManager.getData(player).getSerial(type), null);
        PENDING_ANCHOR_TELEPORTS.put(playerId, pending);
        server.tell(new TickTask(server.getTickCount() + 1,
                () -> prepareAnchorTeleport(server, playerId, pending)));
        return true;
    }

    private static void prepareAnchorTeleport(MinecraftServer server, UUID playerId,
                                              PendingAnchorTeleport pending) {
        if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        ArtifactType type = pending.type;
        if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()
                || !player.level().dimension().equals(pending.sourceDimension)
                || !ArtifactManager.isEnabled(type)
                || !hasQueuedCredential(player, pending)
                || ArtifactManager.isOnCooldown(player, type)) {
            PENDING_ANCHOR_TELEPORTS.remove(playerId);
            return;
        }
        ArtifactAnchorHelper.ResolveResult result = type == ArtifactType.DIMENSION_ANCHOR
                ? ArtifactAnchorHelper.resolveDimensionAnchor(player)
                : ArtifactAnchorHelper.resolveRiftAnchor(player);
        ArtifactAnchorHelper.Destination destination = result.destination();
        if (destination == null) {
            PENDING_ANCHOR_TELEPORTS.remove(playerId);
            showAnchorFailure(player, type, result.failure(), false);
            return;
        }
        PendingAnchorTeleport prepared = new PendingAnchorTeleport(type, pending.hand, pending.sourceDimension,
                pending.credentialSerial, destination);
        PENDING_ANCHOR_TELEPORTS.put(playerId, prepared);
        server.tell(new TickTask(server.getTickCount() + 1,
                () -> finishAnchorTeleport(server, playerId, prepared)));
    }

    private static void finishAnchorTeleport(MinecraftServer server, UUID playerId,
                                             PendingAnchorTeleport pending) {
        if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending) {
            return;
        }
        PENDING_ANCHOR_TELEPORTS.remove(playerId);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        ArtifactType type = pending.type;
        ArtifactAnchorHelper.Destination destination = pending.destination;
        if (player == null || destination == null || player.isRemoved() || !player.isAlive()
                || player.isSpectator() || !player.level().dimension().equals(pending.sourceDimension)
                || !ArtifactManager.isEnabled(type) || !hasQueuedCredential(player, pending)
                || ArtifactManager.isOnCooldown(player, type)
                || !ArtifactAnchorHelper.isDestinationSafe(player, destination)) {
            if (player != null && destination != null && player.isAlive()
                    && !player.isSpectator() && ArtifactManager.isEnabled(type)
                    && hasQueuedCredential(player, pending)
                    && !ArtifactManager.isOnCooldown(player, type)) {
                actionbar(player, "message.unknown_echoes.artifact." + type.getId() + ".blocked");
            }
            return;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, type))) {
            return;
        }
        ServerLevel source = player.serverLevel();
        double sourceX = player.getX();
        double sourceY = player.getY() + 0.8D;
        double sourceZ = player.getZ();
        source.sendParticles(type == ArtifactType.RIFT_ANCHOR
                        ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                sourceX, sourceY, sourceZ,
                18, 0.45D, 0.7D, 0.45D, 0.05D);
        ArtifactAnchorHelper.teleport(player, destination);
        ArtifactAnchorHelper.commitUse(player, type, destination);
        destination.level().sendParticles(ParticleTypes.PORTAL, destination.target().x,
                destination.target().y + 0.8D, destination.target().z, 26,
                0.6D, 0.8D, 0.6D, 0.08D);
        spawnAnchorRing(destination.level(), destination.target(), type == ArtifactType.RIFT_ANCHOR);
        if (type == ArtifactType.DIMENSION_ANCHOR
                && "undertow_return".equals(ArtifactManager.getData(player).getTuning(type))) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
                    300, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    60, 0, true, false, true));
            destination.level().sendParticles(ParticleTypes.SPLASH, destination.target().x,
                    destination.target().y + 0.6D, destination.target().z,
                    24, 0.7D, 0.45D, 0.7D, 0.12D);
        }
        finishUse(player, type, type == ArtifactType.DIMENSION_ANCHOR
                ? SoundEvents.PORTAL_TRAVEL : SoundEvents.ENDERMAN_TELEPORT);
    }

    private static boolean hasQueuedCredential(ServerPlayer player, PendingAnchorTeleport pending) {
        return ArtifactManager.getData(player).getSerial(pending.type) == pending.credentialSerial
                && ArtifactManager.validateCredential(player, pending.type,
                player.getItemInHand(pending.hand));
    }

    private static void finishUse(ServerPlayer player, ArtifactType type, net.minecraft.sounds.SoundEvent sound) {
        finishUse(player, type, sound, "message.unknown_echoes.artifact.use." + type.getId());
    }

    private static void finishUse(ServerPlayer player, ArtifactType type,
                                  net.minecraft.sounds.SoundEvent sound, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), false);
        player.level().playSound(null, player.blockPosition(), sound,
                SoundSource.PLAYERS, 0.9F, 1.2F);
        int cooldown = getCooldownSeconds(player, type);
        if (cooldown > 0) {
            ArtifactManager.startCooldown(player, type, cooldown);
        }
    }

    private static void showAnchorFailure(ServerPlayer player, ArtifactType type,
                                          ArtifactAnchorHelper.Failure failure, boolean binding) {
        String key = switch (failure) {
            case NO_ANCHOR -> type == ArtifactType.DIMENSION_ANCHOR
                    ? "message.unknown_echoes.artifact.dimension_anchor.no_beacon"
                    : "message.unknown_echoes.artifact.rift_anchor.invalid";
            case UNSUPPORTED_DIMENSION -> "message.unknown_echoes.artifact.rift_anchor.unsupported_dimension";
            case DIMENSION_LOCKED -> "message.unknown_echoes.artifact.anchor.dimension_locked";
            case BOSS_NEARBY -> "message.unknown_echoes.artifact.anchor.boss_nearby";
            case UNSAFE_POSITION -> binding
                    ? "message.unknown_echoes.artifact.rift_anchor.unsafe_record"
                    : "message.unknown_echoes.artifact." + type.getId() + ".blocked";
            case EXPIRED -> "message.unknown_echoes.artifact.rift_anchor.expired";
            case INVALID_ANCHOR -> "message.unknown_echoes.artifact." + type.getId() + ".invalid";
            case NONE -> null;
        };
        if (key != null) {
            actionbar(player, key);
        }
    }

    private static void spawnStormCompassParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.18D, 0.0D);
        spawnHorizontalRing(level, ParticleTypes.CLOUD, center, 1.15D, 28, 0.035D);
        spawnHorizontalRing(level, ParticleTypes.END_ROD, center.add(0.0D, 1.0D, 0.0D),
                0.7D, 16, 0.015D);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.8D, player.getZ(),
                18, 0.55D, 0.7D, 0.55D, 0.08D);
    }

    private static void spawnTideLanternParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.12D, 0.0D);
        spawnHorizontalRing(level, ParticleTypes.SPLASH, center, 1.35D, 28, 0.04D);
        spawnHorizontalRing(level, ParticleTypes.NAUTILUS, center.add(0.0D, 0.8D, 0.0D),
                0.75D, 18, 0.015D);
        level.sendParticles(ParticleTypes.NAUTILUS, player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.45D, 0.7D, 0.45D, 0.04D);
    }

    private static void spawnEchoLensParticles(ServerPlayer player, List<LivingEntity> targets) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        level.sendParticles(ParticleTypes.ENCHANT, eye.x, eye.y, eye.z,
                18, 0.3D, 0.25D, 0.3D, 0.035D);
        for (LivingEntity target : targets) {
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 path = center.subtract(eye);
            for (int step = 1; step <= 12; step++) {
                Vec3 point = eye.add(path.scale(step / 12.0D));
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                        1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
            spawnHorizontalRing(level, ParticleTypes.ELECTRIC_SPARK, center,
                    Math.max(0.65D, target.getBbWidth() * 0.75D), 18, 0.025D);
            level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                    16, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.4D,
                    target.getBbWidth() * 0.35D, 0.03D);
        }
    }

    private static void spawnHorizontalRing(ServerLevel level, ParticleOptions particle, Vec3 center,
                                            double radius, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.025D, 0.0D, speed);
        }
    }

    private static void spawnAnchorRing(ServerLevel level, Vec3 center, boolean rift) {
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            double radius = rift ? 1.35D : 1.05D;
            level.sendParticles(rift ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                    center.x + Math.cos(angle) * radius, center.y + 0.08D,
                    center.z + Math.sin(angle) * radius, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    private static void tickStormFlight(ServerPlayer player) {
        StormFlight flight = STORM_FLIGHTS.get(player.getUUID());
        if (flight == null) {
            return;
        }
        if (!ArtifactManager.isEnabled(ArtifactType.STORM_COMPASS)) {
            stopStormFlight(player, false);
            return;
        }
        long now = player.level().getGameTime();
        if (now >= flight.endTime || player.isSpectator()) {
            stopStormFlight(player, true);
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (player.isSprinting() && player.tickCount % 12 == 0) {
            Vec3 look = player.getLookAngle();
            double dash = flight.level >= 2 ? 0.18D : 0.14D;
            player.setDeltaMovement(player.getDeltaMovement()
                    .add(look.x * dash, Math.max(0.0D, look.y) * dash * 0.35D, look.z * dash));
            player.hurtMarked = true;
        }
        if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.2D, player.getZ(),
                    4, 0.25D, 0.08D, 0.25D, 0.01D);
        }
    }

    private static void stopStormFlight(ServerPlayer player, boolean grantSafeFall) {
        StormFlight flight = STORM_FLIGHTS.remove(player.getUUID());
        if (flight == null) {
            return;
        }
        if (!flight.hadMayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (grantSafeFall) {
            int windPieces = EchoArmorSets.windWalkerPieces(player);
            long duration = flight.level >= 3 ? 120L : windPieces >= 4 ? 100L : windPieces >= 2 ? 60L : 0L;
            if (duration > 0L) {
                STORM_SAFE_FALL_ENDS.put(player.getUUID(), player.level().getGameTime() + duration);
            }
        }
    }

    private static void applyPassiveAttributes(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        for (int i = 0; i < TIDE_HEALTH_IDS.length; i++) {
            applyModifier(health, TIDE_HEALTH_IDS[i], tideHealthBonus(i + 1),
                    AttributeModifier.Operation.ADD_VALUE, tideLevel == i + 1);
        }
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), STORM_MOVEMENT_ID,
                level(player, ArtifactType.STORM_COMPASS) > 0 && player.isSprinting() ? 0.04D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                level(player, ArtifactType.STORM_COMPASS) > 0 && player.isSprinting());
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), LENS_ATTACK_SPEED_ID,
                level(player, ArtifactType.ECHO_LENS) > 0 ? 0.05D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                level(player, ArtifactType.ECHO_LENS) > 0);
    }

    private static void tickTideSustain(ServerPlayer player) {
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        if (tideLevel <= 0) {
            return;
        }
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18 && player.tickCount % 600 == 0) {
            foodData.eat(1, 0.05F);
        }
        if (tideLevel >= 3) {
            softenControl(player, MobEffects.MOVEMENT_SLOWDOWN);
            softenControl(player, MobEffects.WEAKNESS);
            softenControl(player, MobEffects.DIG_SLOWDOWN);
        }
    }

    private static void softenControl(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.getDuration() <= 80 || instance.getAmplifier() > 1) {
            return;
        }
        int duration = Math.max(80, instance.getDuration() * 4 / 5);
        player.removeEffect(effect);
        player.addEffect(new MobEffectInstance(effect, duration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon()));
    }

    private static void tickTideAutoGuard(ServerPlayer player) {
        int level = level(player, ArtifactType.TIDE_LANTERN);
        if (level < 3 || player.getHealth() > player.getMaxHealth() * 0.3F) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < TIDE_AUTO_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) {
            return;
        }
        TIDE_AUTO_COOLDOWNS.put(player.getUUID(),
                now + ServerConfig.TIDE_LANTERN_AUTO_COOLDOWN_SECONDS.get() * 20L);
        applyTideRecovery(player, 1, true);
        actionbar(player, "message.unknown_echoes.artifact.tide_lantern.auto");
    }

    private static void applyTideRecovery(ServerPlayer player, int level, boolean weak) {
        int effectiveLevel = Math.max(1, level);
        float heal = weak ? 4.0F : switch (effectiveLevel) {
            case 1 -> 6.0F;
            case 2 -> 8.0F;
            default -> 10.0F;
        };
        int food = weak ? 2 : 2 + effectiveLevel * 2;
        float saturation = weak ? 2.0F : 2.0F + effectiveLevel * 2.0F;
        player.heal(heal);
        FoodData foodData = player.getFoodData();
        foodData.eat(food, saturation / Math.max(1.0F, food * 2.0F));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, weak ? 80 : 160,
                effectiveLevel >= 3 && !weak ? 1 : 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, weak ? 120 : 160,
                effectiveLevel >= 3 && !weak ? 1 : 0, true, false, true));
        if (!weak && effectiveLevel >= 2) {
            clearOneNegativeEffect(player);
        }
    }

    private static void clearOneNegativeEffect(ServerPlayer player) {
        if (removeLowLevelEffect(player, MobEffects.POISON)
                || removeLowLevelEffect(player, MobEffects.WITHER)
                || removeLowLevelEffect(player, MobEffects.MOVEMENT_SLOWDOWN)
                || removeLowLevelEffect(player, MobEffects.WEAKNESS)
                || removeLowLevelEffect(player, MobEffects.DIG_SLOWDOWN)
                || removeLowLevelEffect(player, MobEffects.BLINDNESS)) {
            actionbar(player, "message.unknown_echoes.artifact.tide_lantern.cleaned");
        }
    }

    private static boolean removeLowLevelEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() <= 1) {
            player.removeEffect(effect);
            return true;
        }
        return false;
    }

    private static void tickEchoLensPassive(ServerPlayer player) {
        int level = level(player, ArtifactType.ECHO_LENS);
        if (level <= 0) {
            return;
        }
        int count = level >= 2 ? 2 : 1;
        for (LivingEntity target : findNearbyOrdinaryEnemies(player, level >= 2 ? 20.0D : 16.0D, count)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
        }
    }

    private static List<LivingEntity> findLensTargets(ServerPlayer player, int count) {
        if (count <= 0) {
            return List.of();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().inflate(LENS_RANGE);
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(count,
                Comparator.comparingDouble((LivingEntity entity) -> player.distanceToSqr(entity)).reversed());
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && canLensAffect(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double forward = to.dot(look);
            if (forward <= 0.0D || forward > LENS_RANGE || !player.hasLineOfSight(entity)) {
                continue;
            }
            double distanceSq = to.lengthSqr() - forward * forward;
            double allowance = Math.max(1.5D, entity.getBbWidth() + 0.75D);
            if (distanceSq <= allowance * allowance) {
                if (nearest.size() < count) {
                    nearest.add(entity);
                } else if (player.distanceToSqr(entity) < player.distanceToSqr(nearest.element())) {
                    nearest.remove();
                    nearest.add(entity);
                }
            }
        }
        List<LivingEntity> result = new ArrayList<>(nearest);
        result.sort(Comparator.comparingDouble(player::distanceToSqr));
        return result;
    }

    private static List<LivingEntity> findNearbyOrdinaryEnemies(ServerPlayer player, double range, int count) {
        if (count <= 0) {
            return List.of();
        }
        AABB area = player.getBoundingBox().inflate(range);
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(count,
                Comparator.comparingDouble((LivingEntity entity) -> player.distanceToSqr(entity)).reversed());
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && candidate instanceof Enemy && !isBossEntity(candidate))) {
            // 视线射线检测最贵,只对可能进前 N 的候选做
            if (nearest.size() < count) {
                if (player.hasLineOfSight(entity)) {
                    nearest.add(entity);
                }
            } else if (player.distanceToSqr(entity) < player.distanceToSqr(nearest.element())
                    && player.hasLineOfSight(entity)) {
                nearest.remove();
                nearest.add(entity);
            }
        }
        List<LivingEntity> result = new ArrayList<>(nearest);
        result.sort(Comparator.comparingDouble(player::distanceToSqr));
        return result;
    }

    private static List<BlockPos> findAuthorizedHiddenContent(ServerPlayer player, int lensLevel) {
        int range = lensLevel >= 3 ? 16 : lensLevel >= 2 ? 12 : 8;
        if (EchoArmorSets.trueSightShadowPieces(player) >= 4) {
            range += 4;
        }
        BlockPos center = player.blockPosition();
        PriorityQueue<BlockPos> nearest = new PriorityQueue<>(6,
                Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())).reversed());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -4, -range),
                center.offset(range, 4, range))) {
            if (isAuthorizedHiddenBlock(player, pos)) {
                BlockPos candidate = pos.immutable();
                if (nearest.size() < 6) {
                    nearest.add(candidate);
                } else if (candidate.distToCenterSqr(player.position())
                        < nearest.element().distToCenterSqr(player.position())) {
                    nearest.remove();
                    nearest.add(candidate);
                }
            }
        }
        List<BlockPos> found = new ArrayList<>(nearest);
        found.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())));
        return found;
    }

    private static boolean isAuthorizedHiddenBlock(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        return state.is(ModBlocks.HIDDEN_RUNE_BRICKS.get())
                || state.is(ModBlocks.TRUE_SIGHT_STELE.get())
                || state.is(ModBlocks.TRUE_SIGHT_CHEST.get());
    }

    private static void revealAuthorizedHiddenContent(ServerPlayer player, List<BlockPos> positions) {
        if (positions.isEmpty() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : positions) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    5, 0.28D, 0.25D, 0.28D, 0.01D);
        }
        actionbar(player, "message.unknown_echoes.artifact.echo_lens.hidden_found");
    }

    private static boolean canLensAffect(ServerPlayer player, LivingEntity entity) {
        return !(entity instanceof Player) || ServerConfig.ARTIFACT_PVP_EFFECTS.get();
    }

    private static void interruptNearbyEnemies(ServerPlayer player) {
        int interrupted = 0;
        for (LivingEntity entity : findNearbyOrdinaryEnemies(player, 8.0D, 8)) {
            if (interrupted >= 3) {
                return;
            }
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            interrupted++;
        }
    }

    private static void cleanupLensMarks(ServerPlayer player) {
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null || marks.isEmpty()) {
            LENS_FOCUS_ENDS.computeIfPresent(player.getUUID(),
                    (id, end) -> end < player.level().getGameTime() ? null : end);
            return;
        }
        long now = player.level().getGameTime();
        for (Iterator<LensMark> it = marks.values().iterator(); it.hasNext(); ) {
            LensMark mark = it.next();
            if (mark.endTime < now || mark.hits <= 0) {
                it.remove();
            }
        }
        if (marks.isEmpty()) {
            LENS_MARKS.remove(player.getUUID());
        }
        LENS_FOCUS_ENDS.computeIfPresent(player.getUUID(), (id, end) -> end < now ? null : end);
    }

    private static boolean hasLensFocus(ServerPlayer player) {
        if (!ArtifactManager.isEnabled(ArtifactType.ECHO_LENS)) {
            LENS_FOCUS_ENDS.remove(player.getUUID());
            return false;
        }
        Long end = LENS_FOCUS_ENDS.get(player.getUUID());
        if (end == null) {
            return false;
        }
        if (end < player.level().getGameTime()) {
            LENS_FOCUS_ENDS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active && amount > 0.0D && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else if (!active) {
            attribute.removeModifier(id);
        }
    }

    private static double tideHealthBonus(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return Math.min(ServerConfig.TIDE_LANTERN_MAX_HEALTH_BONUS.get(), switch (level) {
            case 1 -> 4.0D;
            case 2 -> 6.0D;
            default -> 8.0D;
        });
    }

    private static int level(ServerPlayer player, ArtifactType type) {
        return ArtifactManager.isEnabled(type) ? ArtifactManager.getData(player).getLevel(type) : 0;
    }

    private static boolean isBossEntity(LivingEntity entity) {
        if (entity instanceof AbyssWatcher watcher && watcher.isClone()) {
            return false;
        }
        if (entity instanceof MirrorGuardian guardian && guardian.isIllusion()) {
            return false;
        }
        return entity.getType().is(ModEntityTags.ECHO_BOSSES);
    }

    private static boolean isBrokenBoss(LivingEntity entity) {
        return entity instanceof ForgottenColossus colossus && colossus.isBroken()
                || entity instanceof AbyssWatcher watcher && !watcher.isClone() && watcher.isBroken()
                || entity instanceof MirrorGuardian guardian && !guardian.isIllusion() && guardian.isBroken();
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record StormFlight(long endTime, int level, boolean hadMayfly) {
    }

    private record PendingAnchorTeleport(ArtifactType type, InteractionHand hand,
                                         ResourceKey<Level> sourceDimension,
                                         int credentialSerial,
                                         ArtifactAnchorHelper.Destination destination) {
    }

    private static final class LensMark {
        private final long endTime;
        private int hits;

        private LensMark(long endTime, int hits) {
            this.endTime = endTime;
            this.hits = hits;
        }
    }
}
