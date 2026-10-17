package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.journal.ExplorationJournalData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JournalDestinationPayload(String destination) implements CustomPacketPayload {
    public static final Type<JournalDestinationPayload> TYPE = new Type<>(UnknownEchoes.id("journal_destination"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalDestinationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(ExplorationJournalData.MAX_CLUE_LENGTH),
            JournalDestinationPayload::destination, JournalDestinationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
