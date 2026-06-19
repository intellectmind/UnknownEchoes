package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端:购买请求(交易序号指向服务端按同一规则过滤的可见列表)。
 * 服务端重建列表并校验距离/库存/货款(5.8:UI 只发请求)。
 */
public record TradeRequestPayload(int entityId, int tradeIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("trade_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TradeRequestPayload::entityId,
                    ByteBufCodecs.VAR_INT, TradeRequestPayload::tradeIndex,
                    TradeRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
