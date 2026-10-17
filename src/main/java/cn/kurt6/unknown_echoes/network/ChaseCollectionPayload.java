package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChaseCollectionPayload(int earnedMask, int claimedMask) implements CustomPacketPayload {
    public static final Type<ChaseCollectionPayload> TYPE = new Type<>(UnknownEchoes.id("chase_collection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChaseCollectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChaseCollectionPayload::earnedMask,
            ByteBufCodecs.VAR_INT, ChaseCollectionPayload::claimedMask, ChaseCollectionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
