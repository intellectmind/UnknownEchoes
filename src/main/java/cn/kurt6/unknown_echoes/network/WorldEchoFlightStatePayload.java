package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record WorldEchoFlightStatePayload(UUID playerId, boolean flying)
        implements CustomPacketPayload {
    public static final Type<WorldEchoFlightStatePayload> TYPE =
            new Type<>(UnknownEchoes.id("world_echo_flight_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldEchoFlightStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, WorldEchoFlightStatePayload::playerId,
                    ByteBufCodecs.BOOL, WorldEchoFlightStatePayload::flying,
                    WorldEchoFlightStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
