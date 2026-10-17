package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.journal.ExplorationJournalData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record JournalSyncPayload(List<String> structures, List<String> biomes,
                                 List<String> mobs, List<String> bosses,
                                 List<Integer> pages, List<String> clues,
                                 List<Integer> totals, String destination)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JournalSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("sync_journal"));

    private static final StreamCodec<ByteBuf, List<String>> ID_LIST_CODEC =
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(1024));
    private static final StreamCodec<ByteBuf, List<Integer>> PAGE_LIST_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256));
    private static final StreamCodec<ByteBuf, List<String>> CLUE_LIST_CODEC =
            ByteBufCodecs.stringUtf8(ExplorationJournalData.MAX_CLUE_LENGTH)
                    .apply(ByteBufCodecs.list(ExplorationJournalData.MAX_CLUES));
    private static final StreamCodec<ByteBuf, String> DESTINATION_CODEC =
            ByteBufCodecs.stringUtf8(ExplorationJournalData.MAX_CLUE_LENGTH);
    private static final StreamCodec<ByteBuf, List<Integer>> TOTAL_LIST_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public JournalSyncPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new JournalSyncPayload(
                            ID_LIST_CODEC.decode(buffer), ID_LIST_CODEC.decode(buffer),
                            ID_LIST_CODEC.decode(buffer), ID_LIST_CODEC.decode(buffer),
                            PAGE_LIST_CODEC.decode(buffer), CLUE_LIST_CODEC.decode(buffer),
                            TOTAL_LIST_CODEC.decode(buffer), DESTINATION_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, JournalSyncPayload payload) {
                    ID_LIST_CODEC.encode(buffer, payload.structures());
                    ID_LIST_CODEC.encode(buffer, payload.biomes());
                    ID_LIST_CODEC.encode(buffer, payload.mobs());
                    ID_LIST_CODEC.encode(buffer, payload.bosses());
                    PAGE_LIST_CODEC.encode(buffer, payload.pages());
                    CLUE_LIST_CODEC.encode(buffer, payload.clues());
                    TOTAL_LIST_CODEC.encode(buffer, payload.totals());
                    DESTINATION_CODEC.encode(buffer, payload.destination());
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
