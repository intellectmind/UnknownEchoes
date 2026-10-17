package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JourneyClaimPayload(int chapterIndex) implements CustomPacketPayload {
    public static final Type<JourneyClaimPayload> TYPE = new Type<>(UnknownEchoes.id("claim_journey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JourneyClaimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JourneyClaimPayload::chapterIndex, JourneyClaimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
