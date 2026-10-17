package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** 客户端 → 服务端:购买请求，凭据与行号共同指向打开时的列表。 */
public record TradeRequestPayload(int entityId, UUID sessionId, int tradeIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("trade_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TradeRequestPayload::entityId,
                    UUIDUtil.STREAM_CODEC, TradeRequestPayload::sessionId,
                    ByteBufCodecs.VAR_INT, TradeRequestPayload::tradeIndex,
                    TradeRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
