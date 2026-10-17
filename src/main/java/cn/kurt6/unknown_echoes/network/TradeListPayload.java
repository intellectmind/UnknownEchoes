package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/** 服务端 → 客户端:下发迷途旅者交易表(已按玩家阶段过滤)并打开总览交易页。 */
public record TradeListPayload(int entityId, UUID sessionId, List<String> trades) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeListPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("trade_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TradeListPayload::entityId,
                    UUIDUtil.STREAM_CODEC, TradeListPayload::sessionId,
                    ByteBufCodecs.stringUtf8(4096).apply(ByteBufCodecs.list(64)), TradeListPayload::trades,
                    TradeListPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
