package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.damagesource.DamageSource;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
    private static final ResourceLocation STORM_FLIGHT_ID = UnknownEchoes.id("storm_compass_flight");
    private static final ResourceLocation LENS_ATTACK_SPEED_ID = UnknownEchoes.id("echo_lens_attack_speed");
    private static final long MAX_STORM_SAFE_FALL_TICKS = 120L;
    /** 单次冻结的逐目标标记上限。冻结不受此限，只有演出受限。 */
    private static final int MAX_FREEZE_MARKS = 12;
    /** 单次透镜的视线束上限。标记次数不受此限，只有演出受限。 */
    private static final int MAX_LENS_BEAMS = 4;
    private static final long MAX_LENS_FOCUS_TICKS = 240L;
    private static final List<Holder<MobEffect>> TIDE_CLEANSABLE_NEGATIVE_EFFECTS = List.of(
            MobEffects.POISON, MobEffects.WITHER, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.BLINDNESS);

    private static double lensRange() {
        return ServerConfig.ECHO_LENS_RANGE_BLOCKS.get();
    }

    private static long maxLensMarkTicks() {
        return Math.max(390L, ServerConfig.ECHO_LENS_MARK_DURATION_TICKS.get() * 3L);
    }

    private static final Map<UUID, StormFlight> STORM_FLIGHTS = new HashMap<>();
    private static final Map<UUID, Long> STORM_SAFE_FALL_ENDS = new HashMap<>();
    private static final Map<UUID, Long> TIDE_AUTO_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<UUID, LensMark>> LENS_MARKS = new HashMap<>();
    private static final Map<UUID, Long> LENS_FOCUS_ENDS = new HashMap<>();
    /** 玩家 UUID -> 潮汐灯实际施加过的临时效果及其截止时间。 */
    private static final Map<UUID, Map<Holder<MobEffect>, TrackedEffect>> TIDE_EFFECTS = new HashMap<>();
    private static final Map<UUID, PendingAnchorTeleport> PENDING_ANCHOR_TELEPORTS = new HashMap<>();
    private static final Map<UUID, Long> TIME_FREEZE_ENDS = new HashMap<>();
    private static final Map<UUID, Vec3> TIME_FREEZE_MOTION = new HashMap<>();
    /** 目标实体 UUID -> 施法者 UUID/该施法截止时间，支持多人同时冻结且可单独撤销。 */
    private static final Map<UUID, Map<UUID, Long>> TIME_FREEZE_CASTERS = new HashMap<>();
    private static long timeFreezeReleaseFxTick = Long.MIN_VALUE;
    private static int timeFreezeReleaseFxCount;
    private static long timeFreezeCleanupTick = Long.MIN_VALUE;

    private ArtifactEffectManager() {
    }

    public static boolean use(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        return use(player, type, hand, false);
    }

    /** bindInstead 由「物品副功能」键传入，不读取潜行状态。 */
    public static boolean use(ServerPlayer player, ArtifactType type, InteractionHand hand,
                              boolean bindInstead) {
        if (!ArtifactManager.isEnabled(type)) {
            return false;
        }
        return switch (type) {
            case STORM_COMPASS -> useStormCompass(player);
            case TIDE_LANTERN -> useTideLantern(player);
            case ECHO_LENS -> useEchoLens(player);
            case TIME_HOURGLASS -> useTimeHourglass(player);
            case DIMENSION_ANCHOR -> useDimensionAnchor(player, hand, bindInstead);
            case RIFT_ANCHOR -> useRiftAnchor(player, hand, bindInstead);
        };
    }

    public static boolean hasBindMode(ArtifactType type) {
        return type == ArtifactType.DIMENSION_ANCHOR || type == ArtifactType.RIFT_ANCHOR;
    }

    /** 各按键入口共用的服务端入口；凭据校验由调用方完成。 */
    public static boolean useFromKey(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        return useFromKey(player, type, hand, false);
    }

    public static boolean useFromKey(ServerPlayer player, ArtifactType type, InteractionHand hand,
                                     boolean bindInstead) {
        if (ArtifactManager.isOnCooldown(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.generic_cooldown");
            return false;
        }
        return use(player, type, hand, bindInstead);
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
                    ServerConfig.STORM_COMPASS_COOLDOWN_SECONDS.get() - Math.max(0, level - 1) * 15);
            case TIDE_LANTERN -> ServerConfig.TIDE_LANTERN_COOLDOWN_SECONDS.get();
            case ECHO_LENS -> ServerConfig.ECHO_LENS_COOLDOWN_SECONDS.get();
            case TIME_HOURGLASS -> ServerConfig.TIME_HOURGLASS_COOLDOWN_SECONDS.get();
            case DIMENSION_ANCHOR -> Math.max(0,
                    ServerConfig.DIMENSION_ANCHOR_COOLDOWN_SECONDS.get() - Math.max(0, level - 1) * 15);
            case RIFT_ANCHOR -> {
                int base = Math.max(0,
                        ServerConfig.RIFT_ANCHOR_COOLDOWN_SECONDS.get() - Math.max(0, level - 1) * 15);
                yield "silent_fold".equals(ArtifactManager.getData(player).getTuning(type))
                        ? Math.max(1, Math.round(base * 0.75F)) : base;
            }
        };
    }

    public static void tick(ServerPlayer player) {
        tickStormFlight(player);
        int tickCount = player.tickCount;
        if (tickCount % 20 != 0) {
            return;
        }

        ArtifactData data = ArtifactManager.getData(player);
        int tideLevel = level(data, ArtifactType.TIDE_LANTERN);
        if (tickCount % 40 == 0) {
            ArtifactAnchorHelper.tickRiftAnchor(player, data);
            int stormLevel = level(data, ArtifactType.STORM_COMPASS);
            int lensLevel = level(data, ArtifactType.ECHO_LENS);
            applyPassiveAttributes(player, tideLevel, stormLevel, lensLevel);
            tickEchoLensPassive(player, lensLevel);
            cleanupLensMarks(player);
            cleanupTimeFreeze(player.server, ArtifactManager.gameTime(player));
        }
        tickTideAutoGuard(player, tideLevel);
        if (tickCount % 200 == 0) {
            tickTideSustain(player, tideLevel);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        stopStormFlight(player, false);
        setStormFlightAccess(player, false);
        UUID id = player.getUUID();
        STORM_SAFE_FALL_ENDS.remove(id);
        TIDE_AUTO_COOLDOWNS.remove(id);
        LENS_MARKS.remove(id);
        LENS_FOCUS_ENDS.remove(id);
        TIDE_EFFECTS.remove(id);
        PENDING_ANCHOR_TELEPORTS.remove(id);
        removeTimeFreezeCaster(player.server, id, ArtifactManager.gameTime(player));
        TIME_FREEZE_ENDS.remove(id);
        TIME_FREEZE_MOTION.remove(id);
        TIME_FREEZE_CASTERS.remove(id);
    }

    /** 服务器停止时清理所有瞬态神器状态,避免单机世界切换串状态。 */
    public static void clearAll() {
        STORM_FLIGHTS.clear();
        STORM_SAFE_FALL_ENDS.clear();
        TIDE_AUTO_COOLDOWNS.clear();
        LENS_MARKS.clear();
        LENS_FOCUS_ENDS.clear();
        TIDE_EFFECTS.clear();
        PENDING_ANCHOR_TELEPORTS.clear();
        TIME_FREEZE_ENDS.clear();
        TIME_FREEZE_MOTION.clear();
        TIME_FREEZE_CASTERS.clear();
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
        TIME_FREEZE_CASTERS.clear();
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
        long now = ArtifactManager.gameTime(player);
        Long safeEnd = normalizeDeadline(STORM_SAFE_FALL_ENDS, player.getUUID(), now,
                MAX_STORM_SAFE_FALL_TICKS);
        if (safeEnd != null && level(player, ArtifactType.STORM_COMPASS) > 0 && now <= safeEnd) {
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
        ServerPlayer player = responsiblePlayer(event.getSource());
        if (player == null || level(player, ArtifactType.ECHO_LENS) <= 0
                || event.getEntity().isAlliedTo(player)
                || (event.getEntity() instanceof Player && !ServerConfig.ARTIFACT_PVP_EFFECTS.get())) {
            return;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null) {
            return;
        }
        LensMark mark = marks.get(event.getEntity().getUUID());
        long now = ArtifactManager.gameTime(player);
        if (mark == null || normalizeLensMark(mark, now) || mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
            return;
        }
        float amount = event.getAmount();
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.ECHO_LENS);
        float bonusRate = ServerConfig.ECHO_LENS_DAMAGE_BONUS.get().floatValue()
                + ("inscription".equals(tuning) ? 0.10F : 0.0F);
        float bonusCap = ServerConfig.ECHO_LENS_DAMAGE_CAP.get().floatValue()
                + ("inscription".equals(tuning) ? 2.0F : 0.0F);
        float bonus = Math.min(amount * bonusRate, bonusCap);
        if (level(player, ArtifactType.ECHO_LENS) >= 3 && isBrokenBoss(event.getEntity())) {
            bonus += amount * ("archive_door".equals(tuning) ? 0.16F : 0.08F);
        }
        event.setAmount(amount + bonus);
    }

    /** 透镜次数只在伤害管线确认成功后消耗，取消事件和完全盾挡不会吃掉标记。 */
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        ServerPlayer player = responsiblePlayer(event.getSource());
        if (player == null || level(player, ArtifactType.ECHO_LENS) <= 0
                || event.getEntity().isAlliedTo(player)
                || (event.getEntity() instanceof Player && !ServerConfig.ARTIFACT_PVP_EFFECTS.get())) {
            return;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null) {
            return;
        }
        LensMark mark = marks.get(event.getEntity().getUUID());
        long now = ArtifactManager.gameTime(player);
        if (mark == null || normalizeLensMark(mark, now) || mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
            return;
        }
        mark.hits--;
        if (mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
        }
    }

    /** 兼容投射物或特殊伤害源只在 directEntity 中保留玩家施法者的情况。 */
    private static ServerPlayer responsiblePlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean useStormCompass(ServerPlayer player) {
        if (player.isPassenger()) {
            actionbar(player, "message.unknown_echoes.storm_compass.passenger");
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.STORM_COMPASS))) {
            return false;
        }
        int level = level(player, ArtifactType.STORM_COMPASS);
        long duration = (long) (ServerConfig.STORM_COMPASS_FLIGHT_SECONDS.get()
                + Math.max(0, level - 1) * 4) * 20L;
        long end = safeDeadline(ArtifactManager.gameTime(player), duration);
        STORM_FLIGHTS.put(player.getUUID(), new StormFlight(end, level));
        setStormFlightAccess(player, true);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        spawnStormCompassParticles(player);
        finishUse(player, ArtifactType.STORM_COMPASS, SoundEvents.ELYTRA_FLYING);
        return true;
    }

    private static boolean useTideLantern(ServerPlayer player) {
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.TIDE_LANTERN);
        if (!canBenefitFromTideLantern(player, tideLevel, tuning)) {
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.TIDE_LANTERN))) {
            return false;
        }
        applyTideRecovery(player, tideLevel, false);
        if (EchoArmorSets.tideStalkerPieces(player) >= 4) {
            applyTideEffect(player, MobEffects.WATER_BREATHING, 200, 0);
        }
        spawnTideLanternParticles(player);
        finishUse(player, ArtifactType.TIDE_LANTERN, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean useEchoLens(ServerPlayer player) {
        int level = level(player, ArtifactType.ECHO_LENS);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.ECHO_LENS);
        int targetCount = (level >= 3 ? 3 : level >= 2 ? 2 : 1)
                + ("cache".equals(tuning) ? 1 : 0);
        if (!preflightEnergy(player, ArtifactType.ECHO_LENS)) {
            return false;
        }
        List<LivingEntity> targets = findLensTargets(player, targetCount);
        List<BlockPos> hidden = findAuthorizedHiddenContent(player, level);
        if (targets.isEmpty() && hidden.isEmpty()) {

            ArtifactManager.startCooldown(player, ArtifactType.ECHO_LENS, 2);
            actionbar(player, "message.unknown_echoes.artifact.echo_lens.no_target");
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.ECHO_LENS))) {
            return false;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        boolean trueSightSet = EchoArmorSets.trueSightShadowPieces(player) >= 4;
        long markDuration = trueSightSet
                ? Math.round(ServerConfig.ECHO_LENS_MARK_DURATION_TICKS.get() * 1.3D)
                : ServerConfig.ECHO_LENS_MARK_DURATION_TICKS.get();
        if ("archive_door".equals(tuning)) {
            markDuration = markDuration * 3L / 2L;
        }
        long now = ArtifactManager.gameTime(player);
        long end = safeDeadline(now, markDuration);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    (int) markDuration + 20, 0, true, false, true));
            marks.put(target.getUUID(), new LensMark(end,
                    2 + level + ("cache".equals(tuning) ? 1 : 0)));
        }
        if (level >= 3) {
            interruptNearbyEnemies(player, "archive_door".equals(tuning) ? 80 : 40);
        }
        LENS_FOCUS_ENDS.put(player.getUUID(), safeDeadline(now,
                "archive_door".equals(tuning) ? 240L : 160L));
        spawnEchoLensParticles(player, targets);
        revealAuthorizedHiddenContent(player, hidden);
        finishUse(player, ArtifactType.ECHO_LENS, SoundEvents.AMETHYST_BLOCK_RESONATE);
        return true;
    }

    private static boolean useTimeHourglass(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!preflightEnergy(player, ArtifactType.TIME_HOURGLASS)) {
            return false;
        }
        int artifactLevel = level(player, ArtifactType.TIME_HOURGLASS);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.TIME_HOURGLASS);
        int radius = ServerConfig.TIME_HOURGLASS_RADIUS_BASE_BLOCKS.get()
                + artifactLevel * ServerConfig.TIME_HOURGLASS_RADIUS_PER_LEVEL_BLOCKS.get()
                + ("breadth".equals(tuning) ? 4 : 0);
        long baseDuration = ServerConfig.TIME_HOURGLASS_FREEZE_TICKS.get()
                + Math.max(0, artifactLevel - 1) * 20L;
        boolean foldedMemory = "folded_memory".equals(tuning);
        long duration = foldedMemory ? foldedMemoryDuration(baseDuration) : baseDuration;
        long now = ArtifactManager.gameTime(player);
        AABB area = player.getBoundingBox().inflate(radius, 5.0D, radius);
        List<net.minecraft.world.entity.Entity> candidates = level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, area,
                entity -> !(entity instanceof ServerPlayer) && !entity.isRemoved()
                        && (!(entity instanceof LivingEntity living)
                        || !living.isAlliedTo(player))

                         && entity.distanceToSqr(player.getX(), entity.getY(), player.getZ())
                         <= (double) radius * radius);
        List<net.minecraft.world.entity.Entity> targets = candidates.stream()
                .filter(entity -> freezeDeadline(entity, foldedMemory, baseDuration, duration, now)
                        > TIME_FREEZE_ENDS.getOrDefault(entity.getUUID(), Long.MIN_VALUE))
                .toList();
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
            freezeEntity(entity, freezeDeadline(entity, foldedMemory, baseDuration, duration, now), player);
            // 标记上限比冻结上限低：标记是逐点投放的，点数按目标数乘上去，
            // 而一屏同时标记十几个以上本来就读不清谁是谁。冻结本身不受这个上限影响。
            if (marked++ < MAX_FREEZE_MARKS) {
                ArtifactFx.targetMark(level, entity, ArtifactFxTheme.TIME_HOURGLASS);
            }
        }
        // 环画在实际作用半径上：玩家靠它判断谁会被冻住，半径与判定不同会导致误判站位。
        ArtifactFx.fieldRing(level, player, ArtifactFxTheme.TIME_HOURGLASS, radius);
        finishUse(player, ArtifactType.TIME_HOURGLASS, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        return true;
    }

    private static long freezeDeadline(net.minecraft.world.entity.Entity entity,
                                       boolean foldedMemory, long baseDuration,
                                       long defaultDuration, long now) {
        long entityDuration = foldedMemory && entity instanceof LivingEntity living
                && isBossEntity(living)
                ? Math.round(baseDuration * ServerConfig.TIME_HOURGLASS_BOSS_DURATION_MULTIPLIER.get())
                : defaultDuration;
        return safeDeadline(now, entityDuration);
    }

    private static void freezeEntity(net.minecraft.world.entity.Entity entity, long endTime,
                                     ServerPlayer caster) {
        UUID id = entity.getUUID();
        Map<UUID, Long> casters = TIME_FREEZE_CASTERS.computeIfAbsent(id, ignored -> new HashMap<>());
        casters.put(caster.getUUID(), endTime);
        TIME_FREEZE_ENDS.put(id, casters.values().stream().mapToLong(Long::longValue).max().orElse(endTime));
        TIME_FREEZE_MOTION.putIfAbsent(id, entity.getDeltaMovement());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private static void cleanupTimeFreeze(MinecraftServer server, long now) {
        boolean clockRolledBack = timeFreezeCleanupTick != Long.MIN_VALUE
                && now < timeFreezeCleanupTick;
        if (!clockRolledBack && timeFreezeCleanupTick != Long.MIN_VALUE
                && timeFreezeCleanupTick <= now
                && now - timeFreezeCleanupTick < 40L) {
            return;
        }
        timeFreezeCleanupTick = now;
        normalizeTimeFreezeDeadlines(now);
        TIME_FREEZE_ENDS.entrySet().removeIf(entry -> {
            long end = entry.getValue();
            if (end < 0L || end <= now && now - end >= 200L) {
                Vec3 motion = TIME_FREEZE_MOTION.remove(entry.getKey());
                TIME_FREEZE_CASTERS.remove(entry.getKey());
                if (motion != null) {
                    restoreEntityMotion(server, entry.getKey(), motion);
                }
                return true;
            }
            return false;
        });
        TIME_FREEZE_CASTERS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /** 游戏时间回退时收敛瞬态截止时间，避免冻结效果被旧时间戳无限延长。 */
    private static void normalizeTimeFreezeDeadlines(long now) {
        long maximumEnd = safeDeadline(now, maximumTimeFreezeDurationTicks());
        for (Map.Entry<UUID, Map<UUID, Long>> entry : TIME_FREEZE_CASTERS.entrySet()) {
            Map<UUID, Long> casters = entry.getValue();
            casters.replaceAll((casterId, end) -> Math.min(end, maximumEnd));
            if (!casters.isEmpty()) {
                TIME_FREEZE_ENDS.put(entry.getKey(), casters.values().stream()
                        .mapToLong(Long::longValue).max().orElse(maximumEnd));
            }
        }
        TIME_FREEZE_ENDS.replaceAll((targetId, end) -> Math.min(end, maximumEnd));
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
        if (entity.isRemoved()
                || !ArtifactManager.isEnabled(ArtifactType.TIME_HOURGLASS) || now >= end
                || TIME_FREEZE_CASTERS.get(id) == null
                || end > safeDeadline(now, maximumTimeFreezeDurationTicks())) {
            TIME_FREEZE_ENDS.remove(id);
            TIME_FREEZE_CASTERS.remove(id);
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
        TIME_FREEZE_CASTERS.remove(id);
        if (motion != null) {
            entity.setDeltaMovement(motion);
            entity.hurtMarked = true;
        }
    }

    /** 管理员撤销神器时立即撤销该施法者留下的瞬态状态。 */
    public static void onArtifactRevoked(ServerPlayer player, ArtifactType type) {
        UUID id = player.getUUID();
        switch (type) {
            case STORM_COMPASS -> {
                stopStormFlight(player, false);
                STORM_SAFE_FALL_ENDS.remove(id);
                setStormFlightAccess(player, false);
                refreshPassiveAttributesAfterRevocation(player);
            }
            case TIDE_LANTERN -> {
                TIDE_AUTO_COOLDOWNS.remove(id);
                removeTrackedTideEffects(player);
                refreshPassiveAttributesAfterRevocation(player);
            }
            case ECHO_LENS -> {
                LENS_MARKS.remove(id);
                LENS_FOCUS_ENDS.remove(id);
                refreshPassiveAttributesAfterRevocation(player);
            }
            case TIME_HOURGLASS -> removeTimeFreezeCaster(player.server, id,
                    ArtifactManager.gameTime(player));
            case DIMENSION_ANCHOR, RIFT_ANCHOR -> PENDING_ANCHOR_TELEPORTS.remove(id);
        }
    }

    private static void refreshPassiveAttributesAfterRevocation(ServerPlayer player) {
        ArtifactData data = ArtifactManager.getData(player);
        applyPassiveAttributes(player, level(data, ArtifactType.TIDE_LANTERN),
                level(data, ArtifactType.STORM_COMPASS), level(data, ArtifactType.ECHO_LENS));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** 只撤销仍能确认由潮汐灯施加的同级临时效果；更长或更高等级的外部效果保留。 */
    private static void removeTrackedTideEffects(ServerPlayer player) {
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.remove(player.getUUID());
        if (effects == null) {
            return;
        }
        long now = ArtifactManager.gameTime(player);
        for (Map.Entry<Holder<MobEffect>, TrackedEffect> entry : effects.entrySet()) {
            MobEffectInstance current = player.getEffect(entry.getKey());
            TrackedEffect tracked = entry.getValue();
            long remaining = Math.max(0L, tracked.endTime - now);
            if (current != null && current.getAmplifier() == tracked.amplifier
                    && current.getDuration() >= 0
                    && current.getDuration() <= remaining + 2L) {
                player.removeEffect(entry.getKey());
            }
        }
    }

    private static void removeTimeFreezeCaster(MinecraftServer server, UUID casterId, long now) {
        Iterator<Map.Entry<UUID, Map<UUID, Long>>> targets = TIME_FREEZE_CASTERS.entrySet().iterator();
        while (targets.hasNext()) {
            Map.Entry<UUID, Map<UUID, Long>> targetEntry = targets.next();
            Map<UUID, Long> casters = targetEntry.getValue();
            if (casters.remove(casterId) == null) {
                continue;
            }
            UUID targetId = targetEntry.getKey();
            if (casters.isEmpty()) {
                targets.remove();
                TIME_FREEZE_ENDS.remove(targetId);
                Vec3 motion = TIME_FREEZE_MOTION.remove(targetId);
                if (motion != null) {
                    restoreEntityMotion(server, targetId, motion);
                }
            } else {
                TIME_FREEZE_ENDS.put(targetId,
                        casters.values().stream().mapToLong(Long::longValue).max().orElse(now));
            }
        }
    }

    private static void restoreEntityMotion(MinecraftServer server, UUID entityId, Vec3 motion) {
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
            if (entity != null && !entity.isRemoved()) {
                entity.setDeltaMovement(motion);
                entity.hurtMarked = true;
                return;
            }
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
        ArtifactFx.releaseMark(level, entity, ArtifactFxTheme.TIME_HOURGLASS);
    }

    private static boolean useDimensionAnchor(ServerPlayer player, InteractionHand hand,
                                             boolean bindInstead) {
        if (bindInstead) {
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

    private static boolean useRiftAnchor(ServerPlayer player, InteractionHand hand,
                                        boolean bindInstead) {
        if (bindInstead || !ArtifactAnchorHelper.hasRiftBinding(player)) {
            ArtifactAnchorHelper.BindResult result = ArtifactAnchorHelper.bindRiftAnchor(player);
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
        if (!preflightEnergy(player, type)) {
            return false;
        }
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
                || !ArtifactAnchorHelper.isDestinationCurrent(player, type, destination)) {
            if (player != null && destination != null && player.isAlive()
                    && !player.isSpectator() && ArtifactManager.isEnabled(type)
                    && hasQueuedCredential(player, pending)
                    && !ArtifactManager.isOnCooldown(player, type)) {
                actionbar(player, "message.unknown_echoes.artifact." + type.getId() + ".blocked");
            }
            return;
        }
        int energyCost = getEnergyCost(player, type);
        boolean usedPerpetualCore = ArtifactManager.hasPerpetualEchoCore(player);
        if (!ArtifactManager.spendEnergy(player, energyCost)) {
            return;
        }
        ServerLevel source = player.serverLevel();
        Vec3 departure = player.position();
        if (!ArtifactAnchorHelper.teleport(player, destination)) {
            if (!usedPerpetualCore) {
                ArtifactManager.refundSpentEnergy(player, energyCost);
            }
            return;
        }
        // 出发演出必须在 teleport 成功之后：画在前面时失败退款的那条路径已经发出了粒子，
        // 玩家会看到传送特效却没被传送。坐标只能用传送前抓的 departure，
        // 不能读 player.position()——此刻它已经是目的地。
        ArtifactFx.departureWisp(source, ArtifactFxTheme.of(type), departure);
        ArtifactAnchorHelper.commitUse(player, type, destination);
        spawnAnchorRing(destination.level(), destination.target(), type == ArtifactType.RIFT_ANCHOR);
        if (type == ArtifactType.DIMENSION_ANCHOR
                && "undertow_return".equals(ArtifactManager.getData(player).getTuning(type))) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
                    1200, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,
                    400, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    160, 0, true, false, true));
            // 回潮词条额外加一层潮汐灯配色的环，让玩家读出多拿到的是水下增益。
            destination.level().sendParticles(ParticleTypes.SPLASH, destination.target().x,
                    destination.target().y + 0.6D, destination.target().z,
                    24, 0.7D, 0.45D, 0.7D, 0.12D);
            ArtifactFx.portalColumn(destination.level(), ArtifactFxTheme.TIDE_LANTERN,
                    destination.target(), 0.7D);
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
        player.level().playSound(null, player.blockPosition(), sound,
                SoundSource.PLAYERS, 0.9F, 1.2F);
        int cooldown = getCooldownSeconds(player, type);
        if (cooldown > 0) {
            ArtifactManager.startCooldown(player, type, cooldown);
        }
    }

    /** 能量不足时在昂贵的实体/方块扫描或区块准备前结束请求,并沿用统一提示。 */
    private static boolean preflightEnergy(ServerPlayer player, ArtifactType type) {
        int cost = getEnergyCost(player, type);
        if (ArtifactManager.hasEnoughEnergy(player, cost)) {
            return true;
        }
        ArtifactManager.spendEnergy(player, cost);
        return false;
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

    /**
     * 罗盘指向线索的演出。配色表是包内可见的，跨包调用点走这里而不是各自拼粒子，
     * 否则罗盘的两种用法会是两个颜色。
     */
    public static void spawnCompassPointer(ServerLevel level, Vec3 origin,
                                           Vec3 direction, double length) {
        ArtifactFx.sightBeam(level, ArtifactFxTheme.STORM_COMPASS, origin, direction, length);
    }

    /** 起飞是自身增益，画螺旋而不是环：环会被读成“这里有作用范围”。 */
    private static void spawnStormCompassParticles(ServerPlayer player) {
        ArtifactFx.liftSpiral(player.serverLevel(), player,
                ArtifactFxTheme.STORM_COMPASS, 0.85D, 3);
    }

    /** 回复只作用于持有者，所以画贴身穹顶而不是作用域环。 */
    private static void spawnTideLanternParticles(ServerPlayer player) {
        ArtifactFx.mendDome(player.serverLevel(), player,
                ArtifactFxTheme.TIDE_LANTERN, 1.15D);
    }

    /**
     * 透镜是视线锁定，所以画从眼睛指向目标的束加逐目标标记。
     * 束长取到目标实际距离，不要取配置射程：目标更近时多出来的一段会插进目标身后。
     */
    private static void spawnEchoLensParticles(ServerPlayer player, List<LivingEntity> targets) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        ArtifactFxTheme.Theme theme = ArtifactFxTheme.ECHO_LENS;
        int drawn = 0;
        for (LivingEntity target : targets) {
            // 演出上限独立于标记上限：等级、词条与套装都会抬高目标数，
            // 而每个目标都是一条束加一圈标记，包数按目标数乘上去。
            if (drawn++ >= MAX_LENS_BEAMS) {
                return;
            }
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 path = center.subtract(eye);
            ArtifactFx.sightBeam(level, theme, eye, path, path.length());
            ArtifactFx.targetMark(level, target, theme);
        }
    }

    private static void spawnAnchorRing(ServerLevel level, Vec3 center, boolean rift) {
        ArtifactFx.portalColumn(level, rift
                        ? ArtifactFxTheme.RIFT_ANCHOR : ArtifactFxTheme.DIMENSION_ANCHOR,
                center, rift ? 1.35D : 1.05D);
    }

    private static void tickStormFlight(ServerPlayer player) {
        StormFlight flight = STORM_FLIGHTS.get(player.getUUID());
        if (flight == null) {
            return;
        }
        if (level(player, ArtifactType.STORM_COMPASS) <= 0) {
            stopStormFlight(player, false);
            return;
        }
        long now = ArtifactManager.gameTime(player);
        long maximumEnd = safeDeadline(now, stormFlightDurationTicks(flight.level));
        if (flight.endTime > maximumEnd) {
            flight = new StormFlight(maximumEnd, flight.level);
            STORM_FLIGHTS.put(player.getUUID(), flight);
        }
        if (now >= flight.endTime || player.isSpectator()) {
            stopStormFlight(player, true);
            return;
        }
        setStormFlightAccess(player, true);
        player.fallDistance = 0.0F;
        if (player.isSprinting() && player.tickCount % 12 == 0) {
            Vec3 look = player.getLookAngle();
            double dash = flight.level >= 2 ? 0.18D : 0.14D;
            player.setDeltaMovement(player.getDeltaMovement()
                    .add(look.x * dash, Math.max(0.0D, look.y) * dash * 0.35D, look.z * dash));
            player.hurtMarked = true;
        }
        // 飞行尾迹是每 10 tick 的持续演出，只投 2 点染色：形状留给起飞时的螺旋。
        if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
            Vec3 trail = player.position().add(0.0D, 0.2D, 0.0D);
            level.sendParticles(ArtifactFxTheme.STORM_COMPASS.coreDust(1.1F),
                    trail.x, trail.y, trail.z, 2, 0.22D, 0.06D, 0.22D, 0.0D);
            level.sendParticles(ParticleTypes.CLOUD,
                    trail.x, trail.y, trail.z, 3, 0.25D, 0.08D, 0.25D, 0.01D);
        }
    }

    private static void stopStormFlight(ServerPlayer player, boolean grantSafeFall) {
        StormFlight flight = STORM_FLIGHTS.remove(player.getUUID());
        if (flight == null) {
            return;
        }
        setStormFlightAccess(player, false);
        if (!player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (grantSafeFall) {
            int windPieces = EchoArmorSets.windWalkerPieces(player);
            long duration = flight.level >= 3 ? 120L : windPieces >= 4 ? 100L : windPieces >= 2 ? 60L : 0L;
            if (duration > 0L) {
                STORM_SAFE_FALL_ENDS.put(player.getUUID(), safeDeadline(
                        ArtifactManager.gameTime(player), duration));
            }
        }
    }

    private static void setStormFlightAccess(ServerPlayer player, boolean enabled) {
        AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (attribute == null) {
            return;
        }
        if (enabled) {
            if (attribute.getModifier(STORM_FLIGHT_ID) == null) {
                attribute.addTransientModifier(new AttributeModifier(STORM_FLIGHT_ID,
                        1.0D, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            attribute.removeModifier(STORM_FLIGHT_ID);
        }
    }

    private static void applyPassiveAttributes(ServerPlayer player, int tideLevel,
                                               int stormLevel, int lensLevel) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        for (int i = 0; i < TIDE_HEALTH_IDS.length; i++) {
            applyModifier(health, TIDE_HEALTH_IDS[i], tideHealthBonus(i + 1),
                    AttributeModifier.Operation.ADD_VALUE, tideLevel == i + 1);
        }
        boolean sprintBoost = stormLevel > 0 && player.isSprinting();
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), STORM_MOVEMENT_ID,
                sprintBoost ? 0.04D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sprintBoost);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), LENS_ATTACK_SPEED_ID,
                lensLevel >= 3 ? 0.12D : lensLevel >= 2 ? 0.08D : 0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, lensLevel > 0);
    }

    private static void tickTideSustain(ServerPlayer player, int tideLevel) {
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

    private static void tickTideAutoGuard(ServerPlayer player, int tideLevel) {
        boolean stableReflection = "stable_reflection".equals(
                ArtifactManager.getData(player).getTuning(ArtifactType.TIDE_LANTERN));
        float triggerRatio = stableReflection ? 0.4F : 0.3F;
        if (tideLevel < 3 || player.getHealth() > player.getMaxHealth() * triggerRatio) {
            return;
        }
        long now = ArtifactManager.gameTime(player);
        long cooldownTicks = tideAutoCooldownTicks(player, stableReflection);
        Long cooldownEnd = normalizeDeadline(TIDE_AUTO_COOLDOWNS, player.getUUID(), now,
                cooldownTicks);
        if (cooldownEnd != null && now < cooldownEnd) {
            return;
        }
        TIDE_AUTO_COOLDOWNS.put(player.getUUID(), safeDeadline(now, cooldownTicks));
        applyTideRecovery(player, 1, true);
    }

    private static long safeDeadline(long now, long duration) {
        long boundedDuration = Math.max(0L, duration);
        return now > Long.MAX_VALUE - boundedDuration
                ? Long.MAX_VALUE : now + boundedDuration;
    }

    private static Long normalizeDeadline(Map<UUID, Long> deadlines, UUID id,
                                          long now, long maximumDuration) {
        Long end = deadlines.get(id);
        if (end == null) {
            return null;
        }
        long maximumEnd = safeDeadline(now, maximumDuration);
        if (end > maximumEnd) {
            end = maximumEnd;
            deadlines.put(id, end);
        }
        return end;
    }

    private static boolean normalizeLensMark(LensMark mark, long now) {
        long maximumEnd = safeDeadline(now, maxLensMarkTicks());
        if (mark.endTime > maximumEnd) {
            mark.endTime = maximumEnd;
        }
        return mark.endTime < now;
    }

    private static long stormFlightDurationTicks(int level) {
        return (long) (ServerConfig.STORM_COMPASS_FLIGHT_SECONDS.get()
                + Math.max(0, level - 1) * 4) * 20L;
    }

    private static long tideAutoCooldownTicks(ServerPlayer player, boolean stableReflection) {
        long ticks = (long) ServerConfig.TIDE_LANTERN_AUTO_COOLDOWN_SECONDS.get() * 20L;
        return stableReflection ? ticks * 3L / 4L : ticks;
    }

    private static long maximumTimeFreezeDurationTicks() {
        long ticks = ServerConfig.TIME_HOURGLASS_FREEZE_TICKS.get()
                + (long) (ArtifactType.MAX_LEVEL - 1) * 20L;
        long regularDuration = foldedMemoryDuration(ticks);
        long bossDuration = Math.round(ticks
                * ServerConfig.TIME_HOURGLASS_BOSS_DURATION_MULTIPLIER.get());
        return Math.max(regularDuration, bossDuration);
    }

    private static long foldedMemoryDuration(long ticks) {
        return ticks * 3L / 2L;
    }

    private static boolean canBenefitFromTideLantern(ServerPlayer player, int level,
                                                     String tuning) {
        int effectiveLevel = Math.max(1, level);
        int recoveryDuration = "record".equals(tuning) ? 240 : 160;
        int absorptionDuration = recoveryDuration;
        int amplifier = effectiveLevel >= 3 ? 1 : 0;
        boolean stableReflection = "stable_reflection".equals(tuning);
        FoodData foodData = player.getFoodData();
        return player.getHealth() < player.getMaxHealth()
                || foodData.getFoodLevel() < 20
                || foodData.getSaturationLevel() + 0.01F < foodData.getFoodLevel()
                || needsEffectRefresh(player, MobEffects.REGENERATION,
                recoveryDuration, amplifier)
                || FoodEffectSupport.needsAbsorptionRefresh(player, absorptionDuration,
                amplifier, (amplifier + 1) * 4.0F)
                || stableReflection && needsEffectRefresh(
                player, MobEffects.DAMAGE_RESISTANCE, 100, 0)
                || EchoArmorSets.tideStalkerPieces(player) >= 4 && needsEffectRefresh(
                player, MobEffects.WATER_BREATHING, 200, 0)
                || effectiveLevel >= 2 && hasClearableNegativeEffect(player, tuning);
    }

    private static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    private static boolean hasClearableNegativeEffect(ServerPlayer player, String tuning) {
        if ("rune".equals(tuning)) {
            return player.getActiveEffects().stream()
                    .anyMatch(effect -> !effect.getEffect().value().isBeneficial());
        }
        for (Holder<MobEffect> effect : TIDE_CLEANSABLE_NEGATIVE_EFFECTS) {
            MobEffectInstance instance = player.getEffect(effect);
            if (instance != null && instance.getAmplifier() <= 1) {
                return true;
            }
        }
        return false;
    }

    private static void applyTideRecovery(ServerPlayer player, int level, boolean weak) {
        int effectiveLevel = Math.max(1, level);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.TIDE_LANTERN);
        boolean stableReflection = "stable_reflection".equals(tuning);
        float heal = weak ? 4.0F : switch (effectiveLevel) {
            case 1 -> 6.0F;
            case 2 -> 8.0F;
            default -> 10.0F;
        };
        if (!weak && "record".equals(tuning)) {
            heal += 2.0F;
        }
        int food = weak ? 2 : 2 + effectiveLevel * 2;
        float saturation = weak ? 2.0F : 2.0F + effectiveLevel * 2.0F;
        player.heal(heal);
        FoodData foodData = player.getFoodData();
        foodData.eat(food, saturation / Math.max(1.0F, food * 2.0F));
        int recoveryDuration = weak ? (stableReflection ? 120 : 80)
                : ("record".equals(tuning) ? 240 : 160);
        int absorptionDuration = weak ? (stableReflection ? 180 : 120)
                : ("record".equals(tuning) ? 240 : 160);
        int absorptionAmplifier = effectiveLevel >= 3 && !weak ? 1 : 0;
        applyTideEffect(player, MobEffects.REGENERATION, recoveryDuration, absorptionAmplifier);
        applyTideAbsorption(player, absorptionDuration, absorptionAmplifier,
                (absorptionAmplifier + 1) * 4.0F);
        if (stableReflection) {
            applyTideEffect(player, MobEffects.DAMAGE_RESISTANCE, 100, 0);
        }
        if (!weak && effectiveLevel >= 2) {
            if ("rune".equals(tuning)) {
                clearAllNegativeEffects(player);
            } else {
                clearNegativeEffects(player, 1);
            }
        }
    }

    private static void clearAllNegativeEffects(ServerPlayer player) {
        List<Holder<MobEffect>> harmful = player.getActiveEffects().stream()
                .filter(effect -> !effect.getEffect().value().isBeneficial())
                .map(MobEffectInstance::getEffect)
                .toList();
        harmful.forEach(player::removeEffect);
    }

    private static void clearNegativeEffects(ServerPlayer player, int limit) {
        int removed = 0;
        for (Holder<MobEffect> effect : TIDE_CLEANSABLE_NEGATIVE_EFFECTS) {
            if (removed < limit && removeLowLevelEffect(player, effect)) {
                removed++;
            }
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

    private static void applyTideEffect(ServerPlayer player, Holder<MobEffect> effect,
                                         int duration, int amplifier) {
        MobEffectInstance before = player.getEffect(effect);
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        MobEffectInstance after = player.getEffect(effect);
        if (tideEffectWasChanged(before, after, amplifier)) {
            TIDE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(effect, new TrackedEffect(
                            safeDeadline(ArtifactManager.gameTime(player), duration), amplifier));
        }
    }

    private static void applyTideAbsorption(ServerPlayer player, int duration, int amplifier,
                                             float absorptionHealth) {
        MobEffectInstance before = player.getEffect(MobEffects.ABSORPTION);
        boolean depleted = before != null && before.getAmplifier() == amplifier
                && player.getAbsorptionAmount() + 0.01F < absorptionHealth;
        FoodEffectSupport.applyAbsorption(player, duration, amplifier, absorptionHealth);
        MobEffectInstance after = player.getEffect(MobEffects.ABSORPTION);
        if (depleted || tideEffectWasChanged(before, after, amplifier)) {
            TIDE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(MobEffects.ABSORPTION, new TrackedEffect(
                            safeDeadline(ArtifactManager.gameTime(player), duration), amplifier));
        }
    }

    /** 只记录潮汐灯实际改变的效果，避免撤销时误删外部的更高或更长效果。 */
    private static boolean tideEffectWasChanged(MobEffectInstance before,
                                                MobEffectInstance after, int amplifier) {
        if (after == null || after.getAmplifier() != amplifier) {
            return false;
        }
        if (before == null || before.getAmplifier() != after.getAmplifier()) {
            return true;
        }
        return before.getDuration() < 0
                ? false
                : after.getDuration() > before.getDuration();
    }

    private static void tickEchoLensPassive(ServerPlayer player, int lensLevel) {
        if (lensLevel <= 0) {
            return;
        }
        int count = lensLevel >= 2 ? 2 : 1;
        for (LivingEntity target : findNearbyOrdinaryEnemies(player,
                lensLevel >= 2 ? 20.0D : 16.0D, count)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
        }
    }

    private static List<LivingEntity> findLensTargets(ServerPlayer player, int count) {
        if (count <= 0) {
            return List.of();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double range = lensRange();
        AABB area = player.getBoundingBox().inflate(range);
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(count,
                Comparator.comparingDouble((LivingEntity entity) -> player.distanceToSqr(entity)).reversed());
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && canLensAffect(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double forward = to.dot(look);
            if (forward <= 0.0D || forward > range) {
                continue;
            }
            double distanceSq = to.lengthSqr() - forward * forward;
            double allowance = Math.max(1.5D, entity.getBbWidth() + 0.75D);
            if (distanceSq <= allowance * allowance && player.hasLineOfSight(entity)) {
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
                candidate -> candidate.isAlive() && candidate instanceof Enemy
                        && !candidate.isAlliedTo(player) && !isBossEntity(candidate)
                        && candidate.distanceToSqr(player) <= range * range)) {

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
            if (!player.level().hasChunkAt(pos)) {
                continue;
            }
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
            ArtifactFx.revealCross(level, ArtifactFxTheme.ECHO_LENS,
                    new Vec3(pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D));
        }
    }

    private static boolean canLensAffect(ServerPlayer player, LivingEntity entity) {
        boolean playerTarget = entity instanceof Player;
        return !entity.isAlliedTo(player)
                && (entity instanceof Enemy || isBossEntity(entity)
                || playerTarget && ServerConfig.ARTIFACT_PVP_EFFECTS.get());
    }

    private static void interruptNearbyEnemies(ServerPlayer player, int durationTicks) {
        int interrupted = 0;
        for (LivingEntity entity : findNearbyOrdinaryEnemies(player, 8.0D, 8)) {
            if (interrupted >= 3) {
                return;
            }
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    durationTicks, 1, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    durationTicks, 0, true, false, true));
            interrupted++;
        }
    }

    private static void cleanupLensMarks(ServerPlayer player) {
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null || marks.isEmpty()) {
            long now = ArtifactManager.gameTime(player);
            Long end = normalizeDeadline(LENS_FOCUS_ENDS, player.getUUID(), now,
                    MAX_LENS_FOCUS_TICKS);
            if (end != null && end < now) {
                LENS_FOCUS_ENDS.remove(player.getUUID());
            }
            return;
        }
        long now = ArtifactManager.gameTime(player);
        for (Iterator<LensMark> it = marks.values().iterator(); it.hasNext(); ) {
            LensMark mark = it.next();
            if (normalizeLensMark(mark, now) || mark.hits <= 0) {
                it.remove();
            }
        }
        if (marks.isEmpty()) {
            LENS_MARKS.remove(player.getUUID());
        }
        Long focusEnd = normalizeDeadline(LENS_FOCUS_ENDS, player.getUUID(), now,
                MAX_LENS_FOCUS_TICKS);
        if (focusEnd != null && focusEnd < now) {
            LENS_FOCUS_ENDS.remove(player.getUUID());
        }
    }

    private static boolean hasLensFocus(ServerPlayer player) {
        if (level(player, ArtifactType.ECHO_LENS) <= 0) {
            LENS_FOCUS_ENDS.remove(player.getUUID());
            return false;
        }
        long now = ArtifactManager.gameTime(player);
        Long end = normalizeDeadline(LENS_FOCUS_ENDS, player.getUUID(), now,
                MAX_LENS_FOCUS_TICKS);
        if (end == null) {
            return false;
        }
        if (end < now) {
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
        AttributeModifier existing = attribute.getModifier(id);
        if (active && amount > 0.0D) {
            if (existing == null || Double.compare(existing.amount(), amount) != 0
                    || existing.operation() != operation) {
                attribute.removeModifier(id);
                attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else {
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
        return level(ArtifactManager.getData(player), type);
    }

    private static int level(ArtifactData data, ArtifactType type) {
        return ArtifactManager.isEnabled(type) ? data.getLevel(type) : 0;
    }

    private static boolean isBossEntity(LivingEntity entity) {
        if (entity instanceof AbyssWatcher watcher && watcher.isClone()) {
            return false;
        }
        if (entity instanceof MirrorGuardian guardian && guardian.isIllusion()) {
            return false;
        }
        return entity.getType().is(ModEntityTags.ECHO_BOSSES)
                || entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean isBrokenBoss(LivingEntity entity) {
        return entity instanceof ForgottenColossus colossus && colossus.isBroken()
                || entity instanceof AbyssWatcher watcher && !watcher.isClone() && watcher.isBroken()
                || entity instanceof MirrorGuardian guardian && !guardian.isIllusion() && guardian.isBroken();
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record StormFlight(long endTime, int level) {
    }

    private record PendingAnchorTeleport(ArtifactType type, InteractionHand hand,
                                         ResourceKey<Level> sourceDimension,
                                         int credentialSerial,
                                         ArtifactAnchorHelper.Destination destination) {
    }

    private static final class LensMark {
        private long endTime;
        private int hits;

        private LensMark(long endTime, int hits) {
            this.endTime = endTime;
            this.hits = hits;
        }
    }

    private record TrackedEffect(long endTime, int amplifier) {
    }
}
