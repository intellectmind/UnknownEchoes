package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;
import java.util.UUID;

public record StudySyncPayload(UUID session, int revision, List<Integer> board, List<Integer> details,
                               int earnedMask, int claimedMask) implements CustomPacketPayload {
    public static final Type<StudySyncPayload> TYPE = new Type<>(UnknownEchoes.id("study_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StudySyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StudySyncPayload::session,
            ByteBufCodecs.VAR_INT, StudySyncPayload::revision,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(9)), StudySyncPayload::board,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(5)), StudySyncPayload::details,
            ByteBufCodecs.VAR_INT, StudySyncPayload::earnedMask,
            ByteBufCodecs.VAR_INT, StudySyncPayload::claimedMask, StudySyncPayload::new);
    public StudySyncPayload { board = List.copyOf(board); details = List.copyOf(details); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
