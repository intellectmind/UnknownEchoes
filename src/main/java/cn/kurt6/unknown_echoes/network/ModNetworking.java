package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.ClientAbilityCache;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncAbilityPayload.TYPE, SyncAbilityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientAbilityCache.update(payload)));

        registrar.playToClient(JournalSyncPayload.TYPE, JournalSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientJournalCache.update(payload)));

        // ---- V0.6D 神器/能量 ----

        registrar.playToClient(ArtifactSyncPayload.TYPE, ArtifactSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientArtifactCache.update(payload)));

        registrar.playToClient(OpenArtifactStationPayload.TYPE, OpenArtifactStationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen
                                .openArtifactStation(payload.stationMode(), payload.pos())));

        registrar.playToServer(ArtifactActionPayload.TYPE, ArtifactActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handleArtifactAction(player, payload);
                    }
                }));

        // ---- V0.6D 迷途旅者交易 ----

        registrar.playToClient(TradeListPayload.TYPE, TradeListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientTradeCache.openFromServer(payload)));

        registrar.playToServer(TradeRequestPayload.TYPE, TradeRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        cn.kurt6.unknown_echoes.trade.EchoTradeManager.handleTradeRequest(
                                player, payload.entityId(), payload.tradeIndex());
                    }
                }));

        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        // V0.6E:能力 + 空中状态 + 能量/冷却全部在 WindGlideTracker 服务端校验
                        cn.kurt6.unknown_echoes.ability.WindGlideTracker.handle(player, payload.mode());
                    }
                }));

        // V0.6E:滑翔请求被拒 → 客户端取消滑翔表现并显示含蓄提示
        registrar.playToClient(GlideDenyPayload.TYPE, GlideDenyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.kurt6.unknown_echoes.client.ClientGameEvents.onGlideDenied(payload.reason())));
    }

    /** 神器页操作(C2S):UI 只发请求,这里校验台座与距离后经 ArtifactManager 落账(5.8 边界)。 */
    private static void handleArtifactAction(ServerPlayer player, ArtifactActionPayload payload) {
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
