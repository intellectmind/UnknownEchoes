package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 服务端 → 客户端:下发迷途旅者交易表(已按玩家阶段过滤)并打开总览交易页。
 * 行格式 "costItem,costCount,resultItem,resultCount,usesLeft"(纯展示;成交校验在服务端)。
 */
public record TradeListPayload(int entityId, List<String> trades) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeListPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("trade_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TradeListPayload::entityId,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TradeListPayload::trades,
                    TradeListPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
