package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.event.EffectControlSupport;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
import cn.kurt6.unknown_echoes.mixin.LivingDamageAccessor;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;

public final class ArtifactEffectManager {
    private static final Set<ServerPlayer> TICKING_PLAYERS =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
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
    private static final Map<UUID, Map<DamageContainer, LensHit>> LENS_HITS = new HashMap<>();
    private static final Map<UUID, Long> LENS_FOCUS_ENDS = new HashMap<>();
    /** 玩家 UUID -> 潮汐灯实际施加过的临时效果及其截止时间。 */
    private static final Map<UUID, Map<Holder<MobEffect>, TrackedEffect>> TIDE_EFFECTS = new HashMap<>();
    private static final Map<LivingEntity, TideCleanup> TIDE_CLEANUPS = new java.util.IdentityHashMap<>();
    private record TideCleanup(Holder<MobEffect> effect, TrackedEffect tracked,
                               CompoundTag snapshot, MobEffectInstance replacement) {}
    private static final String TIDE_EFFECTS_SNAPSHOT_KEY = "ue_tide_effects_snapshot";
    private static final List<Holder<MobEffect>> TIDE_TRACKED_EFFECT_TYPES = List.of(
            MobEffects.REGENERATION, MobEffects.ABSORPTION,
            MobEffects.WATER_BREATHING, MobEffects.DAMAGE_RESISTANCE);
    private static final Map<UUID, PendingAnchorTeleport> PENDING_ANCHOR_TELEPORTS = new HashMap<>();
    /** 同步提交栈的保护由 finally 释放，不能被传送触发的换维度清理提前移除。 */
    private static final Set<UUID> ACTIVE_ANCHOR_TELEPORTS = new HashSet<>();
    private static final Map<UUID, Long> TIME_FREEZE_ENDS = new HashMap<>();
    private static final Map<UUID, Vec3> TIME_FREEZE_MOTION = new HashMap<>();
    /** 冻结目标的维度索引；正常释放时避免为每个目标遍历所有维度，跨维度时仍回退全量查找。 */
    private static final Map<UUID, ResourceKey<Level>> TIME_FREEZE_DIMENSIONS = new HashMap<>();
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
        if (!player.isAlive() || player.hasDisconnected() || player.isSpectator() || !ArtifactManager.isEnabled(type)) {
            return false;
        }
        return ArtifactUseGuard.run(player, type, () -> switch (type) {
            case STORM_COMPASS -> useStormCompass(player, hand);
            case TIDE_LANTERN -> useTideLantern(player, hand);
            case ECHO_LENS -> useEchoLens(player, hand);
            case TIME_HOURGLASS -> useTimeHourglass(player, hand);
            case DIMENSION_ANCHOR -> useDimensionAnchor(player, hand, bindInstead);
            case RIFT_ANCHOR -> useRiftAnchor(player, hand, bindInstead);
        });
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
        if (!player.isAlive() || player.hasDisconnected() || player.isSpectator()) {
            return false;
        }
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
        if (!TICKING_PLAYERS.add(player)) {
            return;
        }
        Level source = player.level();
        try {
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
            if (player.level() == source) {
                tickTideAutoGuard(player, tideLevel);
            }
            if (tickCount % 200 == 0 && player.level() == source) {
                tickTideSustain(player, tideLevel);
            }
        } finally {
            TICKING_PLAYERS.remove(player);
        }
    }

    /** 登出事件先于原版玩家存档，保存可确认归属后再清理内存状态。 */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        saveTideEffects(player);
        clearPlayer(player);
    }

    /** 离线期间效果不倒计时，登录时用已保存的剩余时长重建截止时间。 */
    public static void onPlayerLoggedIn(ServerPlayer player) {
        ListTag saved = player.getPersistentData().getList(TIDE_EFFECTS_SNAPSHOT_KEY, Tag.TAG_COMPOUND);
        player.getPersistentData().remove(TIDE_EFFECTS_SNAPSHOT_KEY);
        long now = ArtifactManager.gameTime(player);
        for (int i = 0; i < Math.min(saved.size(), TIDE_TRACKED_EFFECT_TYPES.size()); i++) {
            CompoundTag encoded = saved.getCompound(i);
            boolean rootOwned = !encoded.contains("ue_tide_root_owned")
                    || encoded.contains("ue_tide_root_owned", Tag.TAG_BYTE) && encoded.getBoolean("ue_tide_root_owned");
            MobEffectInstance snapshot = MobEffectInstance.load(encoded);
            if (snapshot == null || snapshot.getDuration() == 0 || snapshot.getDuration() < -1
                    || rootOwned && snapshot.getDuration() < 0
                    || !TIDE_TRACKED_EFFECT_TYPES.contains(snapshot.getEffect())) {
                continue;
            }
            MobEffectInstance current = player.getEffect(snapshot.getEffect());
            if (current != null && tideEffectSnapshot(current).equals(tideEffectSnapshot(snapshot))) {
                long end = rootOwned ? safeDeadline(now, current.getDuration()) : 0L;
                int amplifier = rootOwned ? current.getAmplifier() : -1;
                TrackedEffect restored = new TrackedEffect(end, amplifier, current, readHiddenTideEnds(encoded, now));
                if (hasTrackedTideLayer(current, restored, now)) {
                    TIDE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                            .put(snapshot.getEffect(), restored);
                }
            }
        }
    }

    /** 效果的 cure 是集合；比较快照时固定其顺序，包括隐藏效果链。 */
    private static CompoundTag tideEffectSnapshot(MobEffectInstance instance) {
        CompoundTag snapshot = (CompoundTag) instance.save();
        sortTideEffectCures(snapshot);
        return snapshot;
    }

    private static void sortTideEffectCures(CompoundTag snapshot) {
        ListTag encodedCures = snapshot.getList("neoforge:cures", Tag.TAG_STRING);
        if (encodedCures.size() > 1) {
            List<String> cures = new ArrayList<>();
            for (int i = 0; i < encodedCures.size(); i++) {
                cures.add(encodedCures.getString(i));
            }
            cures.sort(String::compareTo);
            ListTag sorted = new ListTag();
            for (String cure : cures) {
                sorted.add(StringTag.valueOf(cure));
            }
            snapshot.put("neoforge:cures", sorted);
        }
        if (snapshot.contains("hidden_effect", Tag.TAG_COMPOUND)) {
            sortTideEffectCures(snapshot.getCompound("hidden_effect"));
        }
    }

    /** 正常停服先保存玩家，再断开连接，必须在首次保存前记录归属。 */
    public static void saveTideEffectsBeforeStop(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            saveTideEffects(player);
        }
    }

    private static void saveTideEffects(ServerPlayer player) {
        player.getPersistentData().remove(TIDE_EFFECTS_SNAPSHOT_KEY);
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
        if (effects == null) {
            return;
        }
        ListTag saved = new ListTag();
        long now = ArtifactManager.gameTime(player);
        for (Map.Entry<Holder<MobEffect>, TrackedEffect> entry : effects.entrySet()) {
            MobEffectInstance current = player.getEffect(entry.getKey());
            if (hasTrackedTideLayer(current, entry.getValue(), now)) {
                CompoundTag snapshot = (CompoundTag) current.save();
                boolean rootOwned = matchesTrackedTideEffect(current, entry.getValue(), now);
                snapshot.putBoolean("ue_tide_root_owned", rootOwned);
                Map<Integer, Long> hiddenClaims = tideLayerClaims(entry.getValue());
                if (rootOwned) {
                    hiddenClaims.remove(current.getAmplifier());
                }
                writeHiddenTideEnds(snapshot, hiddenClaims, now);
                saved.add(snapshot);
            }
        }
        if (!saved.isEmpty()) {
            player.getPersistentData().put(TIDE_EFFECTS_SNAPSHOT_KEY, saved);
        }
    }

    /** 末地返回会复制原有效果；死亡重生必须丢弃旧归属。 */
    public static void onPlayerRespawn(ServerPlayer player, boolean endConquered) {
        if (endConquered) {
            rebindCopiedTideEffects(player);
        }
        clearTransientState(player, endConquered);
        if (!endConquered) {
            player.getPersistentData().remove(TIDE_EFFECTS_SNAPSHOT_KEY);
        }
    }

    /** 末地返回使用原版复制构造，只有相符的新实例才能继承旧归属。 */
    private static void rebindCopiedTideEffects(ServerPlayer player) {
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
        if (effects == null) {
            return;
        }
        long now = ArtifactManager.gameTime(player);
        Iterator<Map.Entry<Holder<MobEffect>, TrackedEffect>> entries = effects.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Holder<MobEffect>, TrackedEffect> entry = entries.next();
            TrackedEffect tracked = entry.getValue();
            MobEffectInstance current = player.getEffect(entry.getKey());
            if (current == tracked.instance) {
                continue;
            }
            Long copiedEnd = current == null ? null : current.getAmplifier() == tracked.amplifier
                    ? Long.valueOf(tracked.endTime) : tracked.hiddenTideEnds.get(current.getAmplifier());
            TrackedEffect rebound = copiedEnd == null ? null : new TrackedEffect(
                    copiedEnd, current.getAmplifier(), current, Map.of());
            if (rebound != null && matchesTrackedTideEffect(current, rebound, now)
                    && tideEffectSnapshot(current).equals(tideEffectSnapshot(new MobEffectInstance(tracked.instance)))) {
                entry.setValue(rebound);
            } else {
                entries.remove();
            }
        }
        if (effects.isEmpty()) {
            TIDE_EFFECTS.remove(player.getUUID());
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        clearTransientState(player, false);
    }

    /** 切换维度保留仍随玩家持续的潮汐效果归属。 */
    public static void onPlayerChangedDimension(ServerPlayer player) {
        clearTransientState(player, true);
    }

    private static void clearTransientState(ServerPlayer player, boolean preserveTideEffects) {
        TideEffectApplication.cancel(player);
        stopStormFlight(player, false);
        setStormFlightAccess(player, false);
        UUID id = player.getUUID();
        STORM_SAFE_FALL_ENDS.remove(id);
        TIDE_AUTO_COOLDOWNS.remove(id);
        LENS_MARKS.remove(id);
        LENS_HITS.remove(id);
        LENS_FOCUS_ENDS.remove(id);
        if (!preserveTideEffects) {
            TIDE_EFFECTS.remove(id);
        }
        PENDING_ANCHOR_TELEPORTS.remove(id);
        removeTimeFreezeCaster(player.server, id, ArtifactManager.gameTime(player));
        TIME_FREEZE_ENDS.remove(id);
        TIME_FREEZE_MOTION.remove(id);
        TIME_FREEZE_DIMENSIONS.remove(id);
        TIME_FREEZE_CASTERS.remove(id);
    }

    /** 服务器停止时清理所有瞬态神器状态,避免单机世界切换串状态。 */
    public static void clearAll() {
        TideEffectApplication.cancelAll();
        STORM_FLIGHTS.clear();
        STORM_SAFE_FALL_ENDS.clear();
        TIDE_AUTO_COOLDOWNS.clear();
        LENS_MARKS.clear();
        LENS_HITS.clear();
        LENS_FOCUS_ENDS.clear();
        TIDE_EFFECTS.clear();
        PENDING_ANCHOR_TELEPORTS.clear();
        TIME_FREEZE_ENDS.clear();
        TIME_FREEZE_MOTION.clear();
        TIME_FREEZE_DIMENSIONS.clear();
        TIME_FREEZE_CASTERS.clear();
        timeFreezeReleaseFxTick = Long.MIN_VALUE;
        timeFreezeReleaseFxCount = 0;
        timeFreezeCleanupTick = Long.MIN_VALUE;
    }

    /** 服务器保存世界前恢复所有仍加载的冻结实体，防止零速度写入存档。 */
    public static void restoreFrozenEntities(MinecraftServer server) {
        for (Map.Entry<UUID, Vec3> entry : TIME_FREEZE_MOTION.entrySet()) {
            restoreEntityMotion(server, entry.getKey(), entry.getValue(),
                    TIME_FREEZE_DIMENSIONS.get(entry.getKey()));
        }
        TIME_FREEZE_ENDS.clear();
        TIME_FREEZE_MOTION.clear();
        TIME_FREEZE_DIMENSIONS.clear();
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
        pruneLensHits(player.getUUID());
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
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        Map<DamageContainer, LensHit> hits = LENS_HITS.computeIfAbsent(player.getUUID(), id -> new WeakHashMap<>());
        if (hits.containsKey(event.getContainer())) {
            return;
        }
        int reserved = 0;
        for (LensHit hit : hits.values()) {
            if (hit.mark == mark) reserved++;
        }
        if (reserved >= mark.hits) {
            return;
        }
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.ECHO_LENS);
        float bonusRate = ServerConfig.ECHO_LENS_DAMAGE_BONUS.get().floatValue()
                + ("inscription".equals(tuning) ? 0.10F : 0.0F);
        float bonusCap = ServerConfig.ECHO_LENS_DAMAGE_CAP.get().floatValue()
                + ("inscription".equals(tuning) ? 2.0F : 0.0F);
        float bonus = Math.min(amount * bonusRate, bonusCap);
        if (level(player, ArtifactType.ECHO_LENS) >= 3 && isBrokenBoss(event.getEntity())) {
            bonus += amount * ("archive_door".equals(tuning) ? 0.16F : 0.08F);
        }
        hits.put(event.getContainer(), new LensHit(mark, new WeakReference<>(event.getEntity())));
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
        DamageContainer container = currentDamageContainer(event.getEntity());
        if (container == null || container.getSource() != event.getSource()) {
            return;
        }
        Map<DamageContainer, LensHit> hits = LENS_HITS.get(player.getUUID());
        LensHit hit = hits == null ? null : hits.remove(container);
        if (hit == null || hit.target.get() != event.getEntity()) {
            return;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null) {
            return;
        }
        LensMark mark = marks.get(event.getEntity().getUUID());
        if (mark != hit.mark) {
            return;
        }
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

    private static DamageContainer currentDamageContainer(LivingEntity entity) {
        var containers = ((LivingDamageAccessor) entity).unknownEchoes$getDamageContainers();
        return containers.empty() ? null : containers.peek();
    }

    private static void pruneLensHits(UUID playerId) {
        Map<DamageContainer, LensHit> hits = LENS_HITS.get(playerId);
        if (hits == null) {
            return;
        }
        hits.entrySet().removeIf(entry -> {
            LivingEntity target = entry.getValue().target.get();
            return target == null || !((LivingDamageAccessor) target)
                    .unknownEchoes$getDamageContainers().contains(entry.getKey());
        });
        if (hits.isEmpty()) {
            LENS_HITS.remove(playerId);
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

    private static boolean useStormCompass(ServerPlayer player, InteractionHand hand) {
        if (player.isPassenger()) {
            actionbar(player, "message.unknown_echoes.storm_compass.passenger");
            return false;
        }
        ServerLevel source = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        if (!canContinueStormUse(player, source, stack, hand)) {
            return false;
        }
        ArtifactManager.reserveCooldown(player, ArtifactType.STORM_COMPASS,
                getCooldownSeconds(player, ArtifactType.STORM_COMPASS));
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.STORM_COMPASS))) {
            ArtifactManager.getData(player).clearCooldown(ArtifactType.STORM_COMPASS);
            return false;
        }
        if (!canContinueStormUse(player, source, stack, hand)) {
            return true;
        }
        int level = level(player, ArtifactType.STORM_COMPASS);
        long duration = (long) (ServerConfig.STORM_COMPASS_FLIGHT_SECONDS.get()
                + Math.max(0, level - 1) * 4) * 20L;
        long end = safeDeadline(ArtifactManager.gameTime(player), duration);
        STORM_FLIGHTS.put(player.getUUID(), new StormFlight(end, level));
        setStormFlightAccess(player, true);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        if (!canContinueStormUse(player, source, stack, hand)) {
            return true;
        }
        player.fallDistance = 0.0F;
        spawnStormCompassParticles(player);
        finishUse(player, ArtifactType.STORM_COMPASS);
        return true;
    }

    private static boolean canContinueStormUse(ServerPlayer player, ServerLevel source,
                                               ItemStack stack, InteractionHand hand) {
        return player.level() == source && player.isAlive() && !player.isSpectator()
                && !player.hasDisconnected() && !player.isPassenger()
                && ArtifactManager.isEnabled(ArtifactType.STORM_COMPASS)
                && player.getItemInHand(hand) == stack
                && ArtifactManager.validateCredential(player, ArtifactType.STORM_COMPASS, stack);
    }

    private static boolean canContinueUse(ServerPlayer player, ServerLevel source,
                                           ItemStack stack, InteractionHand hand, ArtifactType type) {
        return player.level() == source && player.isAlive() && !player.isSpectator()
                && !player.hasDisconnected() && ArtifactManager.isEnabled(type) && player.getItemInHand(hand) == stack
                && ArtifactManager.validateCredential(player, type, stack);
    }

    private static boolean payForUse(ServerPlayer player, ArtifactType type) {
        if (!preflightEnergy(player, type)) {
            return false;
        }
        ArtifactManager.reserveCooldown(player, type, getCooldownSeconds(player, type));
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, type))) {
            ArtifactManager.getData(player).clearCooldown(type);
            return false;
        }
        return true;
    }

    private static boolean useTideLantern(ServerPlayer player, InteractionHand hand) {
        ServerLevel source = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        BooleanSupplier canContinue = () -> canContinueUse(player, source, stack, hand, ArtifactType.TIDE_LANTERN);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.TIDE_LANTERN);
        if (!canBenefitFromTideLantern(player, tideLevel, tuning)) {
            return false;
        }
        if (!payForUse(player, ArtifactType.TIDE_LANTERN)) {
            return false;
        }
        try {
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            applyTideRecovery(player, tideLevel, false, canContinue);
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            if (EchoArmorSets.tideStalkerPieces(player) >= 4) {
                applyTideEffect(player, MobEffects.WATER_BREATHING, 200, 0, canContinue);
            }
            if (canContinue.getAsBoolean()) {
                spawnTideLanternParticles(player);
                playUseSound(player, ArtifactType.TIDE_LANTERN);
            }
            return true;
        } finally {
            ArtifactManager.syncToClient(player);
        }
    }

    private static boolean useEchoLens(ServerPlayer player, InteractionHand hand) {
        ServerLevel source = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        BooleanSupplier canContinue = () -> canContinueUse(player, source, stack, hand, ArtifactType.ECHO_LENS);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        int level = level(player, ArtifactType.ECHO_LENS);
        String tuning = ArtifactManager.getData(player).getTuning(ArtifactType.ECHO_LENS);
        int targetCount = (level >= 3 ? 3 : level >= 2 ? 2 : 1)
                + ("cache".equals(tuning) ? 1 : 0);
        if (!preflightEnergy(player, ArtifactType.ECHO_LENS)) {
            return false;
        }
        Vec3 origin = player.position();
        List<LivingEntity> targets = findLensTargets(player, targetCount);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        List<BlockPos> hidden = findAuthorizedHiddenContent(player, source, origin, level, canContinue);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        if (targets.isEmpty() && hidden.isEmpty()) {

            ArtifactManager.startCooldown(player, ArtifactType.ECHO_LENS, 2);
            actionbar(player, "message.unknown_echoes.artifact.echo_lens.no_target");
            return false;
        }
        List<LivingEntity> interruptions = level >= 3
                ? findNearbyOrdinaryEnemies(player, 8.0D, 8) : List.of();
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        if (!payForUse(player, ArtifactType.ECHO_LENS)) {
            return false;
        }
        try {
            if (!canContinue.getAsBoolean()) {
                return true;
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
                if (!canContinue.getAsBoolean()) {
                    return true;
                }
                if (!validLensTarget(player, source, target, origin, lensRange())) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        (int) markDuration + 20, 0, true, false, true));
                if (!canContinue.getAsBoolean()) {
                    return true;
                }
                if (!validLensTarget(player, source, target, origin, lensRange())) {
                    continue;
                }
                marks.put(target.getUUID(), new LensMark(end,
                        2 + level + ("cache".equals(tuning) ? 1 : 0)));
            }
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            if (level >= 3) {
                interruptNearbyEnemies(player, source, origin, interruptions,
                        "archive_door".equals(tuning) ? 80 : 40, canContinue);
            }
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            LENS_FOCUS_ENDS.put(player.getUUID(), safeDeadline(now,
                    "archive_door".equals(tuning) ? 240L : 160L));
            spawnEchoLensParticles(player, source, origin, targets, canContinue);
            revealAuthorizedHiddenContent(player, source, origin, level, hidden, canContinue);
            if (canContinue.getAsBoolean()) {
                playUseSound(player, ArtifactType.ECHO_LENS);
            }
            return true;
        } finally {
            ArtifactManager.syncToClient(player);
        }
    }

    private static boolean useTimeHourglass(ServerPlayer player, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        BooleanSupplier canContinue = () -> canContinueUse(player, level, stack, hand, ArtifactType.TIME_HOURGLASS);
        if (!canContinue.getAsBoolean()) {
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
        Vec3 origin = player.position();
        AABB area = player.getBoundingBox().inflate(radius, 5.0D, radius);
        List<net.minecraft.world.entity.Entity> candidates = level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, area,
                entity -> validFreezeTarget(player, level, entity, origin, area, radius));
        List<net.minecraft.world.entity.Entity> targets = candidates.stream()
                .filter(entity -> freezeDeadline(entity, foldedMemory, baseDuration, duration, now)
                        > TIME_FREEZE_ENDS.getOrDefault(entity.getUUID(), Long.MIN_VALUE))
                .toList();
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        if (targets.isEmpty()) {
            ArtifactManager.startCooldown(player, ArtifactType.TIME_HOURGLASS, 2);
            actionbar(player, "message.unknown_echoes.artifact.time_hourglass.no_target");
            return false;
        }
        if (!payForUse(player, ArtifactType.TIME_HOURGLASS)) {
            return false;
        }
        try {
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            int marked = 0;
            for (net.minecraft.world.entity.Entity entity : targets) {
                if (!canContinue.getAsBoolean()) {
                    return true;
                }
                long end = freezeDeadline(entity, foldedMemory, baseDuration, duration, now);
                if (!validFreezeTarget(player, level, entity, origin, area, radius)
                        || end <= TIME_FREEZE_ENDS.getOrDefault(entity.getUUID(), Long.MIN_VALUE)) {
                    continue;
                }
                freezeEntity(entity, end, player);
                if (!canContinue.getAsBoolean()) {
                    return true;
                }
                // 标记上限比冻结上限低：标记是逐点投放的，点数按目标数乘上去，
                // 而一屏同时标记十几个以上本来就读不清谁是谁。冻结本身不受这个上限影响。
                if (marked++ < MAX_FREEZE_MARKS) {
                    ArtifactFx.targetMark(level, entity, ArtifactFxTheme.TIME_HOURGLASS);
                }
            }
            // 环画在实际作用半径上：玩家靠它判断谁会被冻住，半径与判定不同会导致误判站位。
            ArtifactFx.fieldRing(level, origin, ArtifactFxTheme.TIME_HOURGLASS, radius);
            playUseSound(player, ArtifactType.TIME_HOURGLASS);
            return true;
        } finally {
            ArtifactManager.syncToClient(player);
        }
    }

    private static boolean validFreezeTarget(ServerPlayer player, ServerLevel source,
                                              net.minecraft.world.entity.Entity entity,
                                              Vec3 origin, AABB area, int radius) {
        return entity.level() == source && !(entity instanceof Player) && !entity.isRemoved()
                && (!(entity instanceof LivingEntity living) || living.isAlive() && !living.isAlliedTo(player))
                && entity.getBoundingBox().intersects(area)
                && entity.distanceToSqr(origin.x, entity.getY(), origin.z) <= (double) radius * radius;
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
        if (entity.level() instanceof ServerLevel level) {
            TIME_FREEZE_DIMENSIONS.putIfAbsent(id, level.dimension());
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private static void cleanupTimeFreeze(MinecraftServer server, long now) {
        if (TIME_FREEZE_ENDS.isEmpty() && TIME_FREEZE_MOTION.isEmpty()
                && TIME_FREEZE_CASTERS.isEmpty()) {
            return;
        }
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
                ResourceKey<Level> dimension = TIME_FREEZE_DIMENSIONS.remove(entry.getKey());
                if (motion != null) {
                    restoreEntityMotion(server, entry.getKey(), motion, dimension);
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
            TIME_FREEZE_DIMENSIONS.remove(id);
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

    /** 自动保存不会停止实体追踪；只修正 NBT，保留在线冻结与恢复记录。 */
    public static void saveEntityMotion(net.minecraft.world.entity.Entity entity, CompoundTag compound) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }
        Vec3 motion = TIME_FREEZE_MOTION.get(entity.getUUID());
        if (motion == null) {
            return;
        }
        ListTag savedMotion = new ListTag();
        savedMotion.add(DoubleTag.valueOf(motion.x));
        savedMotion.add(DoubleTag.valueOf(motion.y));
        savedMotion.add(DoubleTag.valueOf(motion.z));
        compound.put("Motion", savedMotion);
    }

    /** 停止追踪时释放冻结；正常区块卸载会在后续保存之前经过此处。 */
    public static void onEntityLeave(net.minecraft.world.entity.Entity entity) {
        UUID id = entity.getUUID();
        TIME_FREEZE_ENDS.remove(id);
        Vec3 motion = TIME_FREEZE_MOTION.remove(id);
        TIME_FREEZE_DIMENSIONS.remove(id);
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
                LENS_HITS.remove(id);
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

    /** 只移除原实例链中仍可确认的潮汐层，保留外部生效层和隐藏层。 */
    private static void removeTrackedTideEffects(ServerPlayer player) {
        TideEffectApplication.cancel(player);
        if (TIDE_CLEANUPS.containsKey(player)) {
            return;
        }
        player.getPersistentData().remove(TIDE_EFFECTS_SNAPSHOT_KEY);
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
        if (effects == null) {
            return;
        }
        long now = ArtifactManager.gameTime(player);
        RuntimeException failure = null;
        for (Map.Entry<Holder<MobEffect>, TrackedEffect> entry : Map.copyOf(effects).entrySet()) {
            if (TIDE_EFFECTS.get(player.getUUID()) != effects || effects.get(entry.getKey()) != entry.getValue()) {
                continue;
            }
            MobEffectInstance current = player.getEffect(entry.getKey());
            try {
                if (!hasTrackedTideLayer(current, entry.getValue(), now)) {
                    continue;
                }
                MobEffectInstance fallback = tideEffectFallback(current, entry.getValue(), now);
                CompoundTag snapshot = tideEffectSnapshot(current);
                if (fallback != null && snapshot.equals(tideEffectSnapshot(fallback))) {
                    continue;
                }
                TIDE_CLEANUPS.put(player, new TideCleanup(entry.getKey(), entry.getValue(), snapshot, fallback));
                if (fallback == null) {
                    player.removeEffect(entry.getKey());
                } else {
                    player.forceAddEffect(fallback, null);
                }
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            } finally {
                TIDE_CLEANUPS.remove(player);
                if (TIDE_EFFECTS.get(player.getUUID()) == effects && effects.get(entry.getKey()) == entry.getValue()
                        && !hasTrackedTideLayer(player.getEffect(entry.getKey()), entry.getValue(), now)) {
                    effects.remove(entry.getKey());
                }
            }
        }
        if (effects.isEmpty()) {
            TIDE_EFFECTS.remove(player.getUUID(), effects);
        }
        if (failure != null) throw failure;
    }

    /** 移除事件返回后复核原归属与完整效果链，避免删除回调续上的效果。 */
    public static boolean canRemoveTideEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (!(entity instanceof ServerPlayer)) return true;
        TideCleanup cleanup = TIDE_CLEANUPS.get(entity);
        return cleanup == null || !cleanup.effect.equals(effect) || isTideCleanupCurrent(entity, cleanup);
    }

    /** 仅约束本次清理的回退实例；回调自己的效果应用仍走原版流程。 */
    public static boolean canReplaceTideEffect(LivingEntity entity, MobEffectInstance incoming) {
        if (!(entity instanceof ServerPlayer)) return true;
        TideCleanup cleanup = TIDE_CLEANUPS.get(entity);
        return cleanup == null || cleanup.replacement != incoming || isTideCleanupCurrent(entity, cleanup);
    }

    private static boolean isTideCleanupCurrent(LivingEntity entity, TideCleanup cleanup) {
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(entity.getUUID());
        return effects != null && effects.get(cleanup.effect) == cleanup.tracked
                && entity.getEffect(cleanup.effect) == cleanup.tracked.instance
                && cleanup.snapshot.equals(tideEffectSnapshot(cleanup.tracked.instance));
    }

    /** 只移除已知潮汐层；保留未到期的外部隐藏层及其剩余时间。 */
    private static MobEffectInstance tideEffectFallback(MobEffectInstance current, TrackedEffect tracked, long now) {
        CompoundTag encoded = (CompoundTag) current.save();
        CompoundTag fallback;
        if (matchesTrackedTideEffect(current, tracked, now)) {
            fallback = encoded.contains("hidden_effect", Tag.TAG_COMPOUND)
                    ? pruneTideLayers(encoded.getCompound("hidden_effect"), tideLayerClaims(tracked), now) : null;
        } else {
            fallback = pruneTideLayers(encoded, tideLayerClaims(tracked), now);
        }
        if (fallback == null) {
            return null;
        }
        fallback.put("id", encoded.get("id").copy());
        return MobEffectInstance.load(fallback);
    }

    private static CompoundTag pruneTideLayers(CompoundTag layer, Map<Integer, Long> owned, long now) {
        CompoundTag next = layer.contains("hidden_effect", Tag.TAG_COMPOUND)
                ? pruneTideLayers(layer.getCompound("hidden_effect"), owned, now) : null;
        int duration = layer.getInt("duration");
        Long end = owned.get(layer.getInt("amplifier"));
        boolean ownedLayer = end != null && end > now && duration >= 0 && duration <= end - now + 2L;
        if (duration != -1 && duration <= 0 || ownedLayer) {
            return next;
        }
        CompoundTag kept = layer.copy();
        kept.remove("hidden_effect");
        if (next != null) {
            kept.put("hidden_effect", next);
        }
        return kept;
    }

    private static Map<Integer, Long> inheritedTideLayers(ServerPlayer player, Holder<MobEffect> type,
                                                          MobEffectInstance before, MobEffectInstance after,
                                                          int amplifier) {
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
        TrackedEffect previous = effects == null ? null : effects.get(type);
        if (previous == null || previous.instance != before || before != after) {
            return Map.of();
        }
        Map<Integer, Long> layers = new HashMap<>(previous.hiddenTideEnds);
        if (previous.amplifier >= 0 && previous.amplifier != amplifier) {
            layers.put(previous.amplifier, previous.endTime);
        }
        layers.remove(amplifier);
        return Map.copyOf(layers);
    }

    private static int hiddenTideDuration(MobEffectInstance current, int amplifier) {
        if (current == null || current.getAmplifier() <= amplifier) {
            return -2;
        }
        CompoundTag layer = (CompoundTag) current.save();
        while (layer.contains("hidden_effect", Tag.TAG_COMPOUND)) {
            layer = layer.getCompound("hidden_effect");
            if (layer.getInt("amplifier") == amplifier) {
                return layer.getInt("duration");
            }
        }
        return -2;
    }

    private static void trackHiddenTideChange(ServerPlayer player, Holder<MobEffect> type,
                                               MobEffectInstance beforeInstance, MobEffectInstance current,
                                               int before, int duration, int amplifier) {
        int after = hiddenTideDuration(current, amplifier);
        if (beforeInstance != current || after < 0 || after > duration || before == -1 || before >= after) {
            return;
        }
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.computeIfAbsent(
                player.getUUID(), ignored -> new HashMap<>());
        TrackedEffect tracked = effects.get(type);
        long end = safeDeadline(ArtifactManager.gameTime(player), after);
        if (tracked == null || tracked.instance != current) {
            effects.put(type, new TrackedEffect(end, amplifier, current, Map.of()));
        } else if (tracked.amplifier == amplifier) {
            effects.put(type, new TrackedEffect(end, amplifier, current, tracked.hiddenTideEnds));
        } else {
            Map<Integer, Long> layers = new HashMap<>(tracked.hiddenTideEnds);
            layers.put(amplifier, end);
            effects.put(type, new TrackedEffect(tracked.endTime, tracked.amplifier, current, Map.copyOf(layers)));
        }
    }

    /** 主记录的层也可能已被更强外部效果压入隐藏链。 */
    private static Map<Integer, Long> tideLayerClaims(TrackedEffect tracked) {
        Map<Integer, Long> claims = new HashMap<>(tracked.hiddenTideEnds);
        if (tracked.amplifier >= 0) {
            claims.merge(tracked.amplifier, tracked.endTime, Math::max);
        }
        return claims;
    }

    private static boolean hasTrackedTideLayer(MobEffectInstance current, TrackedEffect tracked, long now) {
        if (current == null || current != tracked.instance) {
            return false;
        }
        if (matchesTrackedTideEffect(current, tracked, now)) {
            return true;
        }
        for (Map.Entry<Integer, Long> claim : tideLayerClaims(tracked).entrySet()) {
            int duration = hiddenTideDuration(current, claim.getKey());
            if (duration > 0 && claim.getValue() > now && duration <= claim.getValue() - now + 2L) {
                return true;
            }
        }
        return false;
    }

    private static void writeHiddenTideEnds(CompoundTag snapshot, Map<Integer, Long> ends, long now) {
        CompoundTag remaining = new CompoundTag();
        ends.forEach((amplifier, end) -> {
            if (end > now) {
                remaining.putLong(Integer.toString(amplifier), end - now);
            }
        });
        if (!remaining.isEmpty()) {
            snapshot.put("ue_hidden_tide_ticks", remaining);
        }
    }

    private static Map<Integer, Long> readHiddenTideEnds(CompoundTag snapshot, long now) {
        CompoundTag remaining = snapshot.getCompound("ue_hidden_tide_ticks");
        Map<Integer, Long> ends = new HashMap<>();
        for (int amplifier = 0; amplifier < 256; amplifier++) {
            long ticks = remaining.getLong(Integer.toString(amplifier));
            if (ticks > 0 && ticks <= Integer.MAX_VALUE) {
                ends.put(amplifier, safeDeadline(now, (int) ticks));
            }
        }
        return Map.copyOf(ends);
    }

    private static boolean matchesTrackedTideEffect(MobEffectInstance current,
                                                       TrackedEffect tracked, long now) {
        long remaining = Math.max(0L, tracked.endTime - now);
        Long hiddenEnd = current == null ? null : tracked.hiddenTideEnds.get(current.getAmplifier());
        return current != null && current == tracked.instance
                && current.getDuration() >= 0
                && (current.getAmplifier() == tracked.amplifier && current.getDuration() <= remaining + 2L
                    || hiddenEnd != null && hiddenEnd > now && current.getDuration() <= hiddenEnd - now + 2L);
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
                ResourceKey<Level> dimension = TIME_FREEZE_DIMENSIONS.remove(targetId);
                if (motion != null) {
                    restoreEntityMotion(server, targetId, motion, dimension);
                }
            } else {
                TIME_FREEZE_ENDS.put(targetId,
                        casters.values().stream().mapToLong(Long::longValue).max().orElse(now));
            }
        }
    }

    private static void restoreEntityMotion(MinecraftServer server, UUID entityId, Vec3 motion,
                                             ResourceKey<Level> preferredDimension) {
        if (preferredDimension != null) {
            ServerLevel preferredLevel = server.getLevel(preferredDimension);
            if (preferredLevel != null) {
                net.minecraft.world.entity.Entity entity = preferredLevel.getEntity(entityId);
                if (entity != null && !entity.isRemoved()) {
                    entity.setDeltaMovement(motion);
                    entity.hurtMarked = true;
                    return;
                }
            }
        }
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
        ServerLevel source = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        BooleanSupplier canContinue = () -> canStartAnchorUse(player, source, stack, hand,
                ArtifactType.DIMENSION_ANCHOR);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        if (bindInstead) {
            ArtifactAnchorHelper.BindResult result = ArtifactAnchorHelper.bindDimensionAnchor(player);
            if (!canContinue.getAsBoolean()) {
                return result.success();
            }
            if (!result.success()) {
                showAnchorFailure(player, ArtifactType.DIMENSION_ANCHOR, result.failure(), true);
                return false;
            }
            actionbar(player, "message.unknown_echoes.artifact.dimension_anchor.bound");
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            spawnAnchorRing(player.serverLevel(), player.position().add(0.0D, 0.25D, 0.0D), false);
            player.level().playSound(null, player.blockPosition(), ModSounds.ARTIFACT_DIMENSION_ANCHOR_BIND.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
            return true;
        }
        return queueAnchorTeleport(player, ArtifactType.DIMENSION_ANCHOR, hand);
    }

    private static boolean useRiftAnchor(ServerPlayer player, InteractionHand hand,
                                        boolean bindInstead) {
        ServerLevel source = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        BooleanSupplier canContinue = () -> canStartAnchorUse(player, source, stack, hand,
                ArtifactType.RIFT_ANCHOR);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        boolean needsBinding = bindInstead || !ArtifactAnchorHelper.hasRiftBinding(player);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        if (needsBinding) {
            ArtifactAnchorHelper.BindResult result = ArtifactAnchorHelper.bindRiftAnchor(player);
            if (!canContinue.getAsBoolean()) {
                return result.success();
            }
            if (!result.success()) {
                showAnchorFailure(player, ArtifactType.RIFT_ANCHOR, result.failure(), true);
                return false;
            }
            actionbar(player, "message.unknown_echoes.artifact.rift_anchor.bound");
            if (!canContinue.getAsBoolean()) {
                return true;
            }
            spawnAnchorRing(player.serverLevel(), player.position().add(0.0D, 0.25D, 0.0D), true);
            player.level().playSound(null, player.blockPosition(), ModSounds.ARTIFACT_RIFT_ANCHOR_BIND.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
            return true;
        }
        return queueAnchorTeleport(player, ArtifactType.RIFT_ANCHOR, hand);
    }

    private static boolean canStartAnchorUse(ServerPlayer player, ServerLevel source,
                                              ItemStack stack, InteractionHand hand, ArtifactType type) {
        return !ACTIVE_ANCHOR_TELEPORTS.contains(player.getUUID()) && !player.isRemoved()
                && !player.hasDisconnected() && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.server.getLevel(source.dimension()) == source
                && canContinueUse(player, source, stack, hand, type);
    }

    private static boolean queueAnchorTeleport(ServerPlayer player, ArtifactType type, InteractionHand hand) {
        if (ACTIVE_ANCHOR_TELEPORTS.contains(player.getUUID())) {
            return false;
        }
        UUID playerId = player.getUUID();
        if (PENDING_ANCHOR_TELEPORTS.containsKey(playerId)) {
            return true;
        }
        if (!preflightEnergy(player, type)) {
            return false;
        }
        MinecraftServer server = player.server;
        PendingAnchorTeleport pending = new PendingAnchorTeleport(type, hand, player.level().dimension(),
                ArtifactManager.getData(player).getSerial(type), null, player, player.serverLevel());
        PENDING_ANCHOR_TELEPORTS.put(playerId, pending);
        boolean queued = false;
        try {
            server.tell(new TickTask(server.getTickCount() + 1,
                    () -> prepareAnchorTeleport(server, playerId, pending)));
            queued = true;
        } finally {
            if (!queued) {
                clearPendingAnchor(playerId, pending);
            }
        }
        return true;
    }

    private static void prepareAnchorTeleport(MinecraftServer server, UUID playerId,
                                              PendingAnchorTeleport pending) {
        if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending) {
            return;
        }
        PendingAnchorTeleport prepared = null;
        boolean queued = false;
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ArtifactType type = pending.type;
            if (!isAnchorSourceCurrent(player, pending) || ArtifactManager.isOnCooldown(player, type)) {
                return;
            }
            ArtifactAnchorHelper.ResolveResult result = type == ArtifactType.DIMENSION_ANCHOR
                    ? ArtifactAnchorHelper.resolveDimensionAnchor(player)
                    : ArtifactAnchorHelper.resolveRiftAnchor(player);
            if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending
                    || !isAnchorSourceCurrent(player, pending) || ArtifactManager.isOnCooldown(player, type)) {
                return;
            }
            ArtifactAnchorHelper.Destination destination = result.destination();
            if (destination == null) {
                showAnchorFailure(player, type, result.failure(), false);
                return;
            }
            prepared = new PendingAnchorTeleport(type, pending.hand, pending.sourceDimension,
                    pending.credentialSerial, destination, pending.actor, pending.sourceLevel);
            PENDING_ANCHOR_TELEPORTS.put(playerId, prepared);
            PendingAnchorTeleport next = prepared;
            server.tell(new TickTask(server.getTickCount() + 1,
                    () -> finishAnchorTeleport(server, playerId, next)));
            queued = true;
        } finally {
            if (!queued) {
                clearPendingAnchor(playerId, pending);
                if (prepared != null) {
                    clearPendingAnchor(playerId, prepared);
                }
            }
        }
    }

    private static void finishAnchorTeleport(MinecraftServer server, UUID playerId,
                                             PendingAnchorTeleport pending) {
        if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending || !ACTIVE_ANCHOR_TELEPORTS.add(playerId)) {
            return;
        }
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ArtifactType type = pending.type;
            ArtifactAnchorHelper.Destination destination = pending.destination;
            if (destination == null || !isAnchorSourceCurrent(player, pending)
                    || ArtifactManager.isOnCooldown(player, type)) {
                return;
            }
            boolean destinationCurrent = ArtifactAnchorHelper.isDestinationCurrent(player, type, destination);
            if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending || !isAnchorSourceCurrent(player, pending)
                    || ArtifactManager.isOnCooldown(player, type)) {
                return;
            }
            if (!destinationCurrent) {
                actionbar(player, "message.unknown_echoes.artifact." + type.getId() + ".blocked");
                return;
            }
            ArtifactManager.EnergyPayment payment = ArtifactManager.spendEnergyQuietly(player,
                    getEnergyCost(player, type));
            if (payment == null) {
                return;
            }
            boolean teleported = false;
            try {
                ArtifactManager.reserveCooldown(player, type, getCooldownSeconds(player, type));
                ArtifactManager.syncToClient(player);
                if (PENDING_ANCHOR_TELEPORTS.get(playerId) != pending || !isAnchorSourceCurrent(player, pending)
                        || !ArtifactAnchorHelper.isDestinationCurrent(player, type, destination)
                        || PENDING_ANCHOR_TELEPORTS.get(playerId) != pending || !isAnchorSourceCurrent(player, pending)) {
                    return;
                }
                ServerLevel source = player.serverLevel();
                Vec3 departure = player.position();
                try {
                    teleported = ArtifactAnchorHelper.teleport(player, destination);
                } catch (RuntimeException | Error failure) {
                    // 移动已完成后回调抛错仍属于成功支付；进入传送之前的异常必须退款。
                    teleported = hasReachedAnchorDestination(player, destination);
                    throw failure;
                }
                // 换维度清理可以删除 pending；已到达后用会话/凭据/落点确认资格。
                if (!teleported || !isAnchorArrivalCurrent(player, pending)) {
                    return;
                }
                ArtifactAnchorHelper.commitUse(player, type, destination);
                if (!isAnchorArrivalCurrent(player, pending)) {
                    return;
                }
                ArtifactFx.departureWisp(source, ArtifactFxTheme.of(type), departure);
                spawnAnchorRing(destination.level(), destination.target(), type == ArtifactType.RIFT_ANCHOR);
                if (type == ArtifactType.DIMENSION_ANCHOR
                        && "undertow_return".equals(ArtifactManager.getData(player).getTuning(type))) {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
                            1200, 0, true, false, true));
                    if (!isAnchorArrivalCurrent(player, pending)) {
                        return;
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,
                            400, 0, true, false, true));
                    if (!isAnchorArrivalCurrent(player, pending)) {
                        return;
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                            160, 0, true, false, true));
                    if (!isAnchorArrivalCurrent(player, pending)) {
                        return;
                    }
                    destination.level().sendParticles(ParticleTypes.SPLASH, destination.target().x,
                            destination.target().y + 0.6D, destination.target().z,
                            24, 0.7D, 0.45D, 0.7D, 0.12D);
                    ArtifactFx.portalColumn(destination.level(), ArtifactFxTheme.TIDE_LANTERN,
                            destination.target(), 0.7D);
                }
                playUseSound(player, type);
            } finally {
                if (!teleported) {
                    ArtifactManager.refundSpentEnergy(player, payment);
                    ArtifactManager.getData(player).clearCooldown(type);
                    player.getCooldowns().removeCooldown(type.getCredentialItem());
                }
                ArtifactManager.syncToClient(player);
            }
        } finally {
            clearPendingAnchor(playerId, pending);
            ACTIVE_ANCHOR_TELEPORTS.remove(playerId);
        }
    }

    private static boolean hasQueuedCredential(ServerPlayer player, PendingAnchorTeleport pending) {
        return ArtifactManager.getData(player).getSerial(pending.type) == pending.credentialSerial
                && ArtifactManager.validateCredential(player, pending.type,
                player.getItemInHand(pending.hand));
    }

    private static void clearPendingAnchor(UUID playerId, PendingAnchorTeleport pending) {
        if (PENDING_ANCHOR_TELEPORTS.get(playerId) == pending) {
            PENDING_ANCHOR_TELEPORTS.remove(playerId);
        }
    }

    private static boolean isAnchorActorCurrent(ServerPlayer player, PendingAnchorTeleport pending) {
        return player != null && player == pending.actor && !player.isRemoved() && player.isAlive()
                && !player.isSpectator() && !player.hasDisconnected()
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && ArtifactManager.isEnabled(pending.type) && hasQueuedCredential(player, pending);
    }

    private static boolean isAnchorSourceCurrent(ServerPlayer player, PendingAnchorTeleport pending) {
        return isAnchorActorCurrent(player, pending) && player.level() == pending.sourceLevel
                && player.server.getLevel(pending.sourceDimension) == pending.sourceLevel;
    }

    private static boolean hasReachedAnchorDestination(ServerPlayer player,
                                                        ArtifactAnchorHelper.Destination destination) {
        return player.serverLevel() == destination.level()
                && player.position().distanceToSqr(destination.target()) <= 0.01D;
    }

    private static boolean isAnchorArrivalCurrent(ServerPlayer player, PendingAnchorTeleport pending) {
        return isAnchorActorCurrent(player, pending) && hasReachedAnchorDestination(player, pending.destination);
    }

    private static void finishUse(ServerPlayer player, ArtifactType type) {
        playUseSound(player, type);
        int cooldown = getCooldownSeconds(player, type);
        if (cooldown > 0) {
            ArtifactManager.startCooldown(player, type, cooldown);
        }
    }

    private static void playUseSound(ServerPlayer player, ArtifactType type) {
        switch (type) {
            case STORM_COMPASS -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_STORM_COMPASS_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            case TIDE_LANTERN -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_TIDE_LANTERN_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            case ECHO_LENS -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_ECHO_LENS_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            case TIME_HOURGLASS -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_TIME_HOURGLASS_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            case DIMENSION_ANCHOR -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_DIMENSION_ANCHOR_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            case RIFT_ANCHOR -> player.level().playSound(null, player.blockPosition(),
                    ModSounds.ARTIFACT_RIFT_ANCHOR_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
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

    /** 起飞只给脚边短闪，飞行期间不追加环绕和尾迹。 */
    private static void spawnStormCompassParticles(ServerPlayer player) {
        ArtifactFx.liftPulse(player.serverLevel(), player, ArtifactFxTheme.STORM_COMPASS);
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
    private static void spawnEchoLensParticles(ServerPlayer player, ServerLevel source, Vec3 origin,
                                               List<LivingEntity> targets, BooleanSupplier canContinue) {
        if (!canContinue.getAsBoolean()) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        ArtifactFxTheme.Theme theme = ArtifactFxTheme.ECHO_LENS;
        int drawn = 0;
        for (LivingEntity target : targets) {
            // 演出上限独立于标记上限：等级、词条与套装都会抬高目标数，
            // 而每个目标都是一条束加一圈标记，包数按目标数乘上去。
            if (drawn >= MAX_LENS_BEAMS || !canContinue.getAsBoolean()) {
                return;
            }
            if (!validLensTarget(player, source, target, origin, lensRange())) {
                continue;
            }
            drawn++;
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 path = center.subtract(eye);
            ArtifactFx.sightBeam(source, theme, eye, path, path.length());
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (validLensTarget(player, source, target, origin, lensRange())) {
                ArtifactFx.targetMark(source, target, theme);
            }
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
        if (!player.isAlive() || player.hasDisconnected() || player.isPassenger()
                || level(player, ArtifactType.STORM_COMPASS) <= 0) {
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

    /** 读档时只恢复已拥有且启用的潮汐生命容量，不触发其他神器被动。 */
    public static void refreshTideHealth(ServerPlayer player) {
        applyTideHealth(player, level(player, ArtifactType.TIDE_LANTERN));
    }

    private static void applyPassiveAttributes(ServerPlayer player, int tideLevel,
                                               int stormLevel, int lensLevel) {
        applyTideHealth(player, tideLevel);
        boolean sprintBoost = stormLevel > 0 && player.isSprinting();
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), STORM_MOVEMENT_ID,
                sprintBoost ? 0.04D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sprintBoost);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), LENS_ATTACK_SPEED_ID,
                lensLevel >= 3 ? 0.12D : lensLevel >= 2 ? 0.08D : 0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, lensLevel > 0);
    }

    private static void applyTideHealth(ServerPlayer player, int tideLevel) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        for (int i = 0; i < TIDE_HEALTH_IDS.length; i++) {
            applyModifier(health, TIDE_HEALTH_IDS[i], tideHealthBonus(i + 1),
                    AttributeModifier.Operation.ADD_VALUE, tideLevel == i + 1);
        }
    }

    private static void tickTideSustain(ServerPlayer player, int tideLevel) {
        Level source = player.level();
        if (tideLevel <= 0 || !canContinueTideSustain(player, source, 1)) {
            return;
        }
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18 && player.tickCount % 600 == 0) {
            foodData.eat(1, 0.05F);
        }
        if (tideLevel >= 3) {
            BooleanSupplier canContinue = () -> canContinueTideSustain(player, source, 3);
            softenControl(player, MobEffects.MOVEMENT_SLOWDOWN, canContinue);
            softenControl(player, MobEffects.WEAKNESS, canContinue);
            softenControl(player, MobEffects.DIG_SLOWDOWN, canContinue);
        }
    }

    private static boolean canContinueTideSustain(ServerPlayer player, Level source, int minimumLevel) {
        return player.level() == source && player.isAlive() && !player.hasDisconnected()
                && !player.isSpectator() && level(player, ArtifactType.TIDE_LANTERN) >= minimumLevel;
    }

    private static void softenControl(ServerPlayer player, Holder<MobEffect> effect, BooleanSupplier canContinue) {
        if (!canContinue.getAsBoolean()) {
            return;
        }
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.getDuration() <= 80 || instance.getAmplifier() > 1) {
            return;
        }
        int duration = Math.max(80, (int) (instance.getDuration() * 4L / 5L));
        EffectControlSupport.shorten(player, effect, duration, canContinue);
    }

    private static void tickTideAutoGuard(ServerPlayer player, int tideLevel) {
        Level source = player.level();
        if (tideLevel < 3 || !canContinueTideSustain(player, source, 3)) {
            return;
        }
        boolean stableReflection = "stable_reflection".equals(
                ArtifactManager.getData(player).getTuning(ArtifactType.TIDE_LANTERN));
        float triggerRatio = stableReflection ? 0.4F : 0.3F;
        if (player.getHealth() > player.getMaxHealth() * triggerRatio) {
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
        ServerLevel source = player.serverLevel();
        applyTideRecovery(player, level, weak, () -> canContinueTideSustain(player, source, 3));
    }

    private static void applyTideRecovery(ServerPlayer player, int level, boolean weak,
                                          BooleanSupplier canContinue) {
        if (!canContinue.getAsBoolean()) {
            return;
        }
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
        if (!canContinue.getAsBoolean()) {
            return;
        }
        FoodData foodData = player.getFoodData();
        foodData.eat(food, saturation / Math.max(1.0F, food * 2.0F));
        int recoveryDuration = weak ? (stableReflection ? 120 : 80)
                : ("record".equals(tuning) ? 240 : 160);
        int absorptionDuration = weak ? (stableReflection ? 180 : 120)
                : ("record".equals(tuning) ? 240 : 160);
        int absorptionAmplifier = effectiveLevel >= 3 && !weak ? 1 : 0;
        applyTideEffect(player, MobEffects.REGENERATION, recoveryDuration, absorptionAmplifier, canContinue);
        if (!canContinue.getAsBoolean()) {
            return;
        }
        applyTideAbsorption(player, absorptionDuration, absorptionAmplifier,
                (absorptionAmplifier + 1) * 4.0F, canContinue);
        if (!canContinue.getAsBoolean()) {
            return;
        }
        if (stableReflection) {
            applyTideEffect(player, MobEffects.DAMAGE_RESISTANCE, 100, 0, canContinue);
        }
        if (!canContinue.getAsBoolean()) {
            return;
        }
        if (!weak && effectiveLevel >= 2) {
            if ("rune".equals(tuning)) {
                clearAllNegativeEffects(player, canContinue);
            } else {
                clearNegativeEffects(player, 1, canContinue);
            }
        }
    }

    private static void clearAllNegativeEffects(ServerPlayer player, BooleanSupplier canContinue) {
        List<Holder<MobEffect>> harmful = player.getActiveEffects().stream()
                .filter(effect -> !effect.getEffect().value().isBeneficial())
                .map(MobEffectInstance::getEffect)
                .toList();
        for (Holder<MobEffect> effect : harmful) {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            EffectControlSupport.remove(player, effect, canContinue);
        }
    }

    private static void clearNegativeEffects(ServerPlayer player, int limit, BooleanSupplier canContinue) {
        int removed = 0;
        for (Holder<MobEffect> effect : TIDE_CLEANSABLE_NEGATIVE_EFFECTS) {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (removed < limit && removeLowLevelEffect(player, effect, canContinue)) {
                removed++;
            }
        }
    }

    private static boolean removeLowLevelEffect(ServerPlayer player, Holder<MobEffect> effect, BooleanSupplier canContinue) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() <= 1) {
            return EffectControlSupport.remove(player, effect, canContinue);
        }
        return false;
    }

    /** 原版会原地更新已有状态效果，记账比较必须保留应用前的快照。 */
    private static void applyTideEffect(ServerPlayer player, Holder<MobEffect> effect,
                                         int duration, int amplifier, BooleanSupplier canContinue) {
        TideEffectApplication.apply(player, effect, () -> tideEffectClaim(player, effect),
                (current, before, after, absorptionBefore, started) -> {
                    if (!started) trackTideApplication(player, effect, duration, amplifier, current, before, after);
                }, () -> player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true)), canContinue);
    }

    private static TrackedEffect tideEffectClaim(ServerPlayer player, Holder<MobEffect> effect) {
        Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
        return effects == null ? null : effects.get(effect);
    }

    private static void trackTideApplication(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier,
                                             MobEffectInstance current, MobEffectInstance before, MobEffectInstance after) {
        if (after == null) {
            Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
            if (effects != null) {
                effects.remove(effect);
                if (effects.isEmpty()) TIDE_EFFECTS.remove(player.getUUID());
            }
            return;
        }
        if (tideEffectWasChanged(before, after, amplifier)) {
            TIDE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(effect, new TrackedEffect(
                            safeDeadline(ArtifactManager.gameTime(player), duration), amplifier, after,
                            inheritedTideLayers(player, effect, current, after, amplifier)));
        } else {
            trackHiddenTideChange(player, effect, current, after, hiddenTideDuration(before, amplifier), duration, amplifier);
        }
    }

    private static void applyTideAbsorption(ServerPlayer player, int duration, int amplifier,
                                             float absorptionHealth, BooleanSupplier canContinue) {
        TideEffectApplication.apply(player, MobEffects.ABSORPTION, () -> tideEffectClaim(player, MobEffects.ABSORPTION),
                (current, before, after, absorptionBefore, started) -> {
                    if (started) {
                        trackTideAbsorptionRefill(player, duration, amplifier, absorptionHealth,
                                current, before, after, absorptionBefore);
                    } else {
                        trackTideApplication(player, MobEffects.ABSORPTION, duration, amplifier, current, before, after);
                    }
                }, () -> FoodEffectSupport.applyAbsorption(player, duration, amplifier, absorptionHealth), canContinue);
    }

    private static void trackTideAbsorptionRefill(ServerPlayer player, int duration, int amplifier, float absorptionHealth,
                                                 MobEffectInstance current, MobEffectInstance before,
                                                 MobEffectInstance after, float absorptionBefore) {
        boolean depleted = before != null && before.getAmplifier() == amplifier
                && absorptionBefore + 0.01F < absorptionHealth;
        boolean replenished = depleted && after != null && after.getAmplifier() == amplifier
                && player.getAbsorptionAmount() > absorptionBefore;
        if (replenished) {
            long now = ArtifactManager.gameTime(player);
            long end = safeDeadline(now, duration);
            Map<Holder<MobEffect>, TrackedEffect> effects = TIDE_EFFECTS.get(player.getUUID());
            TrackedEffect previous = effects == null ? null : effects.get(MobEffects.ABSORPTION);
            if (previous != null && matchesTrackedTideEffect(after, previous, now)) {
                end = Math.max(end, safeDeadline(now, after.getDuration()));
            }
            TIDE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(MobEffects.ABSORPTION, new TrackedEffect(
                            end, amplifier, after,
                            inheritedTideLayers(player, MobEffects.ABSORPTION, current, after, amplifier)));
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
        ServerLevel source = player.serverLevel();
        if (lensLevel <= 0 || !canContinueLensPassive(player, source, lensLevel)) {
            return;
        }
        Vec3 origin = player.position();
        double range = lensLevel >= 2 ? 20.0D : 16.0D;
        int count = lensLevel >= 2 ? 2 : 1;
        for (LivingEntity target : findNearbyOrdinaryEnemies(player, range, count)) {
            if (!canContinueLensPassive(player, source, lensLevel)) {
                return;
            }
            if (!(target instanceof Enemy) || isBossEntity(target)
                    || !validLensTarget(player, source, target, origin, range)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
        }
    }

    private static boolean canContinueLensPassive(ServerPlayer player, ServerLevel source, int lensLevel) {
        return player.level() == source && player.isAlive() && !player.isSpectator()
                && !player.hasDisconnected() && level(player, ArtifactType.ECHO_LENS) >= lensLevel;
    }

    private static List<LivingEntity> findLensTargets(ServerPlayer player, int count) {
        if (count <= 0) {
            return List.of();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();
        ServerLevel source = player.serverLevel();
        double range = lensRange();
        AABB area = player.getBoundingBox().inflate(range);
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(count,
                Comparator.comparingDouble((LivingEntity entity) -> entity.position().distanceToSqr(origin)).reversed());
        for (LivingEntity candidate : source.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && candidate != player
                        && canLensAffect(player, candidate)
                        && candidate.position().distanceToSqr(origin) <= range * range)) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double forward = to.dot(look);
            if (forward <= 0.0D || forward > range) {
                continue;
            }
            double distanceSq = to.lengthSqr() - forward * forward;
            double allowance = Math.max(1.5D, candidate.getBbWidth() + 0.75D);
            if (distanceSq <= allowance * allowance
                    && (nearest.size() < count || candidate.position().distanceToSqr(origin)
                    < nearest.element().position().distanceToSqr(origin))
                    && hasLensLineOfSight(player, source, candidate)) {
                if (nearest.size() < count) {
                    nearest.add(candidate);
                } else {
                    nearest.remove();
                    nearest.add(candidate);
                }
            }
        }
        List<LivingEntity> result = new ArrayList<>(nearest);
        result.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(origin)));
        return result;
    }

    private static List<LivingEntity> findNearbyOrdinaryEnemies(ServerPlayer player, double range, int count) {
        if (count <= 0) {
            return List.of();
        }
        double rangeSquared = range * range;
        Vec3 origin = player.position();
        ServerLevel source = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(range);
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(count,
                Comparator.comparingDouble((LivingEntity entity) -> entity.position().distanceToSqr(origin)).reversed());
        for (LivingEntity entity : source.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && candidate != player && candidate instanceof Enemy
                        && !candidate.isAlliedTo(player) && !isBossEntity(candidate)
                        && candidate.position().distanceToSqr(origin) <= rangeSquared)) {

            if (nearest.size() < count) {
                if (hasLensLineOfSight(player, source, entity)) {
                    nearest.add(entity);
                }
            } else if (entity.position().distanceToSqr(origin) < nearest.element().position().distanceToSqr(origin)
                    && hasLensLineOfSight(player, source, entity)) {
                nearest.remove();
                nearest.add(entity);
            }
        }
        List<LivingEntity> result = new ArrayList<>(nearest);
        result.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(origin)));
        return result;
    }

    private static boolean hasLensLineOfSight(ServerPlayer player, ServerLevel source, LivingEntity entity) {
        if (player.level() != source || entity.level() != source) {
            return false;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = entity.getEyePosition();
        if (!Double.isFinite(from.x) || !Double.isFinite(from.y) || !Double.isFinite(from.z)
                || !Double.isFinite(to.x) || !Double.isFinite(to.y) || !Double.isFinite(to.z)
                || from.distanceTo(to) > 128.0D) {
            return false;
        }
        Map<Long, Boolean> readable = new HashMap<>();
        boolean ready = BlockGetter.traverseBlocks(from, to, source, (world, pos) -> {
            if (world.isOutsideBuildHeight(pos)) {
                return null;
            }
            int x = pos.getX() >> 4;
            int z = pos.getZ() >> 4;
            boolean available = readable.computeIfAbsent(ChunkPos.asLong(x, z),
                    key -> world.getChunkSource().getChunkNow(x, z) != null);
            return available ? null : Boolean.FALSE;
        }, world -> Boolean.TRUE);
        return ready && player.hasLineOfSight(entity);
    }

    private static int hiddenContentRange(ServerPlayer player, int lensLevel) {
        int range = lensLevel >= 3 ? 16 : lensLevel >= 2 ? 12 : 8;
        if (EchoArmorSets.trueSightShadowPieces(player) >= 4) {
            range += 4;
        }
        return range;
    }

    private static boolean canRevealHiddenContent(ServerPlayer player, ServerLevel source, BooleanSupplier canContinue) {
        return canContinue.getAsBoolean() && player.level() == source && player.isAlive()
                && EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO);
    }

    private static List<BlockPos> findAuthorizedHiddenContent(ServerPlayer player, ServerLevel source, Vec3 origin,
                                                             int lensLevel, BooleanSupplier canContinue) {
        if (!canRevealHiddenContent(player, source, canContinue)) {
            return List.of();
        }
        int range = hiddenContentRange(player, lensLevel);
        BlockPos center = BlockPos.containing(origin);
        int minY = Math.max(center.getY() - 4, source.getMinBuildHeight());
        int maxY = Math.min(center.getY() + 4, source.getMaxBuildHeight() - 1);
        if (minY > maxY) {
            return List.of();
        }
        Map<Long, LevelChunk> loadedChunks = new HashMap<>();
        PriorityQueue<BlockPos> nearest = new PriorityQueue<>(6,
                Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(origin)).reversed());
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - range, minY, center.getZ() - range,
                center.getX() + range, maxY, center.getZ() + range)) {
            if (nearest.size() == 6 && pos.distToCenterSqr(origin) >= nearest.element().distToCenterSqr(origin)) {
                continue;
            }
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            if (!loadedChunks.containsKey(chunkKey)) {
                loadedChunks.put(chunkKey, source.getChunkSource().getChunkNow(chunkX, chunkZ));
            }
            LevelChunk chunk = loadedChunks.get(chunkKey);
            if (chunk == null) {
                continue;
            }
            if (isAuthorizedHiddenBlock(chunk.getBlockState(pos))) {
                BlockPos candidate = pos.immutable();
                if (nearest.size() < 6) {
                    nearest.add(candidate);
                } else if (candidate.distToCenterSqr(origin) < nearest.element().distToCenterSqr(origin)) {
                    nearest.remove();
                    nearest.add(candidate);
                }
            }
        }
        List<BlockPos> found = new ArrayList<>(nearest);
        found.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(origin)));
        return canRevealHiddenContent(player, source, canContinue) ? found : List.of();
    }

    private static boolean isAuthorizedHiddenBlock(BlockState state) {
        return state.is(ModBlocks.HIDDEN_RUNE_BRICKS.get())
                || state.is(ModBlocks.TRUE_SIGHT_STELE.get())
                || state.is(ModBlocks.TRUE_SIGHT_CHEST.get());
    }

    private static void revealAuthorizedHiddenContent(ServerPlayer player, ServerLevel source, Vec3 origin,
                                                      int lensLevel, List<BlockPos> positions, BooleanSupplier canContinue) {
        BlockPos center = BlockPos.containing(origin);
        for (BlockPos pos : positions) {
            if (!canRevealHiddenContent(player, source, canContinue)) {
                return;
            }
            int range = hiddenContentRange(player, lensLevel);
            if (Math.abs((long) pos.getX() - center.getX()) > range
                    || Math.abs((long) pos.getY() - center.getY()) > 4
                    || Math.abs((long) pos.getZ() - center.getZ()) > range) {
                continue;
            }
            BooleanSupplier canReveal = () -> canRevealHiddenContentAt(player, source, pos, canContinue);
            ArtifactFx.revealCross(source, player, ArtifactFxTheme.ECHO_LENS,
                    new Vec3(pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D), canReveal);
        }
    }

    private static boolean canRevealHiddenContentAt(ServerPlayer player, ServerLevel source, BlockPos pos,
                                                    BooleanSupplier canContinue) {
        if (!canRevealHiddenContent(player, source, canContinue) || source.isOutsideBuildHeight(pos)) {
            return false;
        }
        LevelChunk chunk = source.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && isAuthorizedHiddenBlock(chunk.getBlockState(pos));
    }

    private static boolean canLensAffect(ServerPlayer player, LivingEntity entity) {
        return entity != player && !entity.isAlliedTo(player)
                && (!(entity instanceof ServerPlayer targetPlayer) || !targetPlayer.hasDisconnected())
                && (entity instanceof Player target
                ? !target.isSpectator() && ServerConfig.ARTIFACT_PVP_EFFECTS.get()
                : entity instanceof Enemy || isBossEntity(entity));
    }

    private static boolean validLensTarget(ServerPlayer player, ServerLevel source,
                                            LivingEntity entity, Vec3 origin, double range) {
        return entity.level() == source && entity.isAlive() && canLensAffect(player, entity)
                && entity.position().distanceToSqr(origin) <= range * range && hasLensLineOfSight(player, source, entity);
    }

    private static void interruptNearbyEnemies(ServerPlayer player, ServerLevel source,
                                                Vec3 origin, List<LivingEntity> targets,
                                                int durationTicks, BooleanSupplier canContinue) {
        int interrupted = 0;
        for (LivingEntity entity : targets) {
            if (interrupted >= 3 || !canContinue.getAsBoolean()) {
                return;
            }
            if (!(entity instanceof Enemy) || isBossEntity(entity)
                    || !validLensTarget(player, source, entity, origin, 8.0D)) {
                continue;
            }
            interrupted++;
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!validLensTarget(player, source, entity, origin, 8.0D) || isBossEntity(entity)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    durationTicks, 1, true, false, true));
            if (!canContinue.getAsBoolean()) {
                return;
            }
            if (!validLensTarget(player, source, entity, origin, 8.0D) || isBossEntity(entity)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    durationTicks, 0, true, false, true));
        }
    }

    private static void cleanupLensMarks(ServerPlayer player) {
        pruneLensHits(player.getUUID());
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
                                         ArtifactAnchorHelper.Destination destination,
                                         ServerPlayer actor, ServerLevel sourceLevel) {
    }

    private static final class LensMark {
        private long endTime;
        private int hits;

        private LensMark(long endTime, int hits) {
            this.endTime = endTime;
            this.hits = hits;
        }
    }

    private record LensHit(LensMark mark, WeakReference<LivingEntity> target) {}

    /** amplifier 为 -1 表示仅隐藏层有归属，当前生效层属于外部来源。 */
    private record TrackedEffect(long endTime, int amplifier, MobEffectInstance instance,
                                 Map<Integer, Long> hiddenTideEnds) {
    }
}
