package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.ClientAbilityCache;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class ModNetworking {

    private static final Map<UUID, Long> LAST_UI_ACTION_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_MOBILITY_ACTION_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_SKILL_ACTION_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_ARTIFACT_USE_TICK = new HashMap<>();

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncAbilityPayload.TYPE, SyncAbilityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientAbilityCache.update(payload)));

        registrar.playToClient(AbilityCooldownSyncPayload.TYPE, AbilityCooldownSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientAbilityCooldownCache.update(payload)));

        registrar.playToClient(JournalSyncPayload.TYPE, JournalSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientJournalCache.update(payload)));

        // ---- V0.6D 神器/能量 ----

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

        // ---- V0.6D 迷途旅者交易 ----

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
                        // V0.6E:能力 + 空中状态 + 能量/冷却全部在 WindGlideTracker 服务端校验
                        cn.kurt6.unknown_echoes.ability.WindGlideTracker.handle(player, payload.mode());
                    }
                }));

        registrar.playToServer(ArtifactUsePayload.TYPE, ArtifactUsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowArtifactUse(player)) {
                        handleArtifactUse(player, payload);
                    }
                }));

        registrar.playToServer(EchoAbilityActionPayload.TYPE, EchoAbilityActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && allowMobilityAction(player)) {
                        cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.handle(player, payload.action());
                    }
                }));

        // 战技键 → 手持武器主动技能:冷却与结算全在服务端(物品实现 WeaponSkill)
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

        // V0.6E:滑翔请求被拒 → 客户端取消滑翔表现并显示含蓄提示
        registrar.playToClient(GlideDenyPayload.TYPE, GlideDenyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientGameEvents.onGlideDenied(payload.reason())));
    }

    public static void clearRateLimit(UUID playerId) {
        LAST_UI_ACTION_TICK.remove(playerId);
        LAST_MOBILITY_ACTION_TICK.remove(playerId);
        LAST_SKILL_ACTION_TICK.remove(playerId);
        LAST_ARTIFACT_USE_TICK.remove(playerId);
    }

    public static void clearAllRateLimits() {
        LAST_UI_ACTION_TICK.clear();
        LAST_MOBILITY_ACTION_TICK.clear();
        LAST_SKILL_ACTION_TICK.clear();
        LAST_ARTIFACT_USE_TICK.clear();
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
        long encoded = LAST_MOBILITY_ACTION_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        long tick = encoded >> 3;
        int count = (int) (encoded & 7L);
        if (tick != now) {
            LAST_MOBILITY_ACTION_TICK.put(player.getUUID(), (now << 3) | 1L);
            return true;
        }
        if (count >= 3) {
            return false;
        }
        LAST_MOBILITY_ACTION_TICK.put(player.getUUID(), (now << 3) | (count + 1L));
        return true;
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

    private static boolean allowArtifactUse(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        long now = player.level().getGameTime();
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
                        : net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    /** 神器页操作(C2S):UI 只发请求,这里校验台座与距离后经 ArtifactManager 落账(5.8 边界)。 */
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
            // 台座不在身边/坐标伪造:含蓄拒绝,不报内部错误
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
