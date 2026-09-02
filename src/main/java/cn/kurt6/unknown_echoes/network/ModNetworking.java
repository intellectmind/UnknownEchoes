package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.ClientAbilityCache;
import cn.kurt6.unknown_echoes.item.tool.AreaMiningSupport;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class ModNetworking {

    private static final Map<UUID, Long> LAST_UI_ACTION_TICK = new HashMap<>();
    private static final Map<UUID, MobilityRateLimit> MOBILITY_ACTION_LIMITS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SKILL_ACTION_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_ARTIFACT_USE_TICK = new HashMap<>();
    private static final Set<UUID> ACTIVE_TOOL_SKILL_KEYS = new HashSet<>();

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToClient(SyncAbilityPayload.TYPE, SyncAbilityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientAbilityCache.update(payload)));

        registrar.playToClient(AbilityCooldownSyncPayload.TYPE, AbilityCooldownSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientAbilityCooldownCache.update(payload)));

        registrar.playToClient(BossImpactPayload.TYPE, BossImpactPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientBossImpactEffects.trigger(payload)));

        registrar.playToClient(JournalSyncPayload.TYPE, JournalSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    cn.kurt6.unknown_echoes.client.ClientJournalCache.update(payload);
                    cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.onJournalSync();
                }));

        registrar.playToClient(ArtifactSyncPayload.TYPE, ArtifactSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    cn.kurt6.unknown_echoes.client.ClientArtifactCache.update(payload);
                    cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.onArtifactSync();
                }));

        registrar.playToClient(OpenArtifactStationPayload.TYPE, OpenArtifactStationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen
                                .openArtifactStation(payload.stationMode(), payload.pos())));

        registrar.playToServer(ArtifactActionPayload.TYPE, ArtifactActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowUiAction(player)) {
                        handleArtifactAction(player, payload);
                    }
                }));

        registrar.playToClient(TradeListPayload.TYPE, TradeListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientTradeCache.openFromServer(payload)));

        registrar.playToServer(TradeRequestPayload.TYPE, TradeRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowUiAction(player)) {
                        cn.kurt6.unknown_echoes.trade.EchoTradeManager.handleTradeRequest(
                                player, payload.entityId(), payload.tradeIndex());
                    }
                }));

        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowMobilityAction(player)) {

                        cn.kurt6.unknown_echoes.ability.WindGlideTracker.handle(player, payload.mode());
                    }
                }));

        registrar.playToServer(ArtifactUsePayload.TYPE, ArtifactUsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowArtifactUse(player)) {
                        handleArtifactUse(player, payload);
                    }
                }));

        registrar.playToServer(WorldEchoFlightPayload.TYPE, WorldEchoFlightPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && (!payload.flying() || allowMobilityAction(player))) {
                        cn.kurt6.unknown_echoes.event.WorldEchoWingsEvents.handleFlightRequest(
                                player, payload.flying());
                    }
                }));

        registrar.playToServer(EchoAbilityActionPayload.TYPE, EchoAbilityActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowMobilityAction(player)) {
                        cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.handle(player, payload.action());
                    }
                }));

        registrar.playToServer(WeaponSkillPayload.TYPE, WeaponSkillPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowSkillAction(player)) {
                        handleWeaponSkill(player, payload);
                    }
                }));

        registrar.playToServer(SecondaryWeaponSkillPayload.TYPE, SecondaryWeaponSkillPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowSkillAction(player)) {
                        handleSecondaryWeaponSkill(player, payload);
                    }
                }));

        registrar.playToServer(ItemActiveSkillPayload.TYPE, ItemActiveSkillPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowSkillAction(player)) {
                        handleItemActiveSkill(player, payload);
                    }
                }));

        registrar.playToServer(ToolSkillStatePayload.TYPE, ToolSkillStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handleToolSkillState(player, payload.active());
                    }
                }));

        registrar.playToClient(GlideDenyPayload.TYPE, GlideDenyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientGameEvents.onGlideDenied(payload.reason())));

        registrar.playToClient(WorldEchoFlightStatePayload.TYPE, WorldEchoFlightStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    cn.kurt6.unknown_echoes.client.ClientWorldEchoFlightCache.update(payload);
                    cn.kurt6.unknown_echoes.client.ClientGameEvents.onWorldEchoFlightState(
                            payload.playerId(), payload.flying());
                }));
    }

    public static void clearRateLimit(UUID playerId) {
        LAST_UI_ACTION_TICK.remove(playerId);
        MOBILITY_ACTION_LIMITS.remove(playerId);
        LAST_SKILL_ACTION_TICK.remove(playerId);
        LAST_ARTIFACT_USE_TICK.remove(playerId);
        ACTIVE_TOOL_SKILL_KEYS.remove(playerId);
    }

    public static void clearAllRateLimits() {
        LAST_UI_ACTION_TICK.clear();
        MOBILITY_ACTION_LIMITS.clear();
        LAST_SKILL_ACTION_TICK.clear();
        LAST_ARTIFACT_USE_TICK.clear();
        ACTIVE_TOOL_SKILL_KEYS.clear();
    }

    /** 范围采集仍由方块掉落事件触发；这里仅提供经过主手校验的按键状态。 */
    public static boolean isToolSkillKeyActive(ServerPlayer player) {
        return ACTIVE_TOOL_SKILL_KEYS.contains(player.getUUID())
                && AreaMiningSupport.isSupported(player.getMainHandItem());
    }

    /** 点火器的共鸣批量点亮共用工具技能修饰键，不占用潜行。 */
    public static boolean isResonantLightKeyActive(ServerPlayer player) {
        return ACTIVE_TOOL_SKILL_KEYS.contains(player.getUUID())
                && player.getMainHandItem().getItem()
                        instanceof cn.kurt6.unknown_echoes.item.tool.EchoIgniterItem;
    }

    private static void handleToolSkillState(ServerPlayer player, boolean active) {
        UUID id = player.getUUID();
        if (!active || !player.isAlive() || player.isSpectator()
                || !AreaMiningSupport.usesToolSkillKey(player.getMainHandItem())) {
            ACTIVE_TOOL_SKILL_KEYS.remove(id);
            return;
        }
        ACTIVE_TOOL_SKILL_KEYS.add(id);
    }

    private static boolean allowUiAction(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        long now = player.level().getGameTime();
        Long previous = LAST_UI_ACTION_TICK.put(player.getUUID(), now);
        return previous == null || previous != now;
    }

    /** 移动类包限频:每 tick 允许 3 个(跳+滑翔开始+保持可能挤进同一服务器 tick,单包限频会静默吞包)。 */
    private static boolean allowMobilityAction(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        long now = player.level().getGameTime();
        MobilityRateLimit limit = MOBILITY_ACTION_LIMITS.get(player.getUUID());
        if (limit == null || limit.tick() != now) {
            MOBILITY_ACTION_LIMITS.put(player.getUUID(), new MobilityRateLimit(now, 1));
            return true;
        }
        if (limit.count() >= 3) {
            return false;
        }
        MOBILITY_ACTION_LIMITS.put(player.getUUID(),
                new MobilityRateLimit(now, limit.count() + 1));
        return true;
    }

    private record MobilityRateLimit(long tick, int count) {
    }

    private static boolean allowSkillAction(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        long now = player.level().getGameTime();
        Long previous = LAST_SKILL_ACTION_TICK.put(player.getUUID(), now);
        return previous == null || previous != now;
    }

    private static void handleWeaponSkill(ServerPlayer player, WeaponSkillPayload payload) {
        if (!player.isAlive()
                || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (payload.hand() != WeaponSkillPayload.HAND_MAIN
                && payload.hand() != WeaponSkillPayload.HAND_OFF) {
            return;
        }
        net.minecraft.world.InteractionHand hand = payload.hand() == WeaponSkillPayload.HAND_OFF
                ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND;
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill skill) {
            skill.activateSkill(level, player, stack, hand);
        }
    }

    private static void handleSecondaryWeaponSkill(ServerPlayer player, SecondaryWeaponSkillPayload payload) {
        if (!player.isAlive()
                || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (payload.hand() != SecondaryWeaponSkillPayload.HAND_MAIN
                && payload.hand() != SecondaryWeaponSkillPayload.HAND_OFF) {
            return;
        }
        net.minecraft.world.InteractionHand hand = payload.hand() == SecondaryWeaponSkillPayload.HAND_OFF
                ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND;
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill skill
                && skill.hasSecondarySkill()) {
            skill.activateSecondarySkill(level, player, stack, hand);
        }
    }

    private static void handleItemActiveSkill(ServerPlayer player, ItemActiveSkillPayload payload) {
        if (!player.isAlive()
                || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (payload.hand() != ItemActiveSkillPayload.HAND_MAIN
                && payload.hand() != ItemActiveSkillPayload.HAND_OFF) {
            return;
        }
        net.minecraft.world.InteractionHand hand = payload.hand() == ItemActiveSkillPayload.HAND_OFF
                ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND;
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof cn.kurt6.unknown_echoes.item.ItemActiveSkill skill)) {
            return;
        }
        if (payload.alternate()) {
            if (skill.hasAlternateItemSkill()) {
                skill.activateAlternateItemSkill(level, player, stack, hand);
            }
            return;
        }
        skill.activateItemSkill(level, player, stack, hand);
    }

    private static boolean allowArtifactUse(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        long now = player.server.overworld().getGameTime();
        Long previous = LAST_ARTIFACT_USE_TICK.put(player.getUUID(), now);
        return previous == null || previous != now;
    }

    private static void handleArtifactUse(ServerPlayer player, ArtifactUsePayload payload) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        var stack = player.getItemInHand(payload.offhand()
                ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND);
        var type = cn.kurt6.unknown_echoes.artifact.ArtifactType.byId(
                stack.get(cn.kurt6.unknown_echoes.registry.ModDataComponents.ARTIFACT_ID.get()));
        if (type == null || !cn.kurt6.unknown_echoes.artifact.ArtifactManager.validateCredential(player, type, stack)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.unknown_echoes.artifact.not_yours"), true);
            return;
        }
        cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager.useFromKey(player, type,
                payload.offhand() ? net.minecraft.world.InteractionHand.OFF_HAND
                        : net.minecraft.world.InteractionHand.MAIN_HAND,
                payload.bind());
    }

    private static void handleArtifactAction(ServerPlayer player, ArtifactActionPayload payload) {
        if (payload.action() < ArtifactActionPayload.ACTION_CLAIM
                || payload.action() > ArtifactActionPayload.ACTION_TUNE) {
            return;
        }
        var type = cn.kurt6.unknown_echoes.artifact.ArtifactType.byId(payload.artifactId());
        if (type == null) {
            return;
        }
        var stationMode = payload.action() == ArtifactActionPayload.ACTION_CLAIM
                ? cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.RECORD
                : cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.TUNING;
        if (!cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.validateStation(
                player, payload.stationPos(), stationMode)) {

            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.unknown_echoes.artifact.no_station"), true);
            return;
        }
        switch (payload.action()) {
            case ArtifactActionPayload.ACTION_CLAIM ->
                    cn.kurt6.unknown_echoes.artifact.ArtifactManager.claimOrReissue(player, type);
            case ArtifactActionPayload.ACTION_UPGRADE ->
                    cn.kurt6.unknown_echoes.artifact.ArtifactManager.upgrade(player, type);
            case ArtifactActionPayload.ACTION_TUNE ->
                    cn.kurt6.unknown_echoes.artifact.ArtifactManager.tune(player, type, payload.word());
            default -> { }
        }
    }
}
