package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.journal.JourneyCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record JourneySyncPayload(List<Integer> values, int claimedMask) implements CustomPacketPayload {
    public static final Type<JourneySyncPayload> TYPE = new Type<>(UnknownEchoes.id("sync_journey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JourneySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(JourneyCatalog.Metric.values().length)), JourneySyncPayload::values,
            ByteBufCodecs.VAR_INT, JourneySyncPayload::claimedMask, JourneySyncPayload::new);

    public JourneySyncPayload { values = List.copyOf(values); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
