package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

public record StudyActionPayload(UUID session, int revision, int operation, int first, int second) implements CustomPacketPayload {
    public static final Type<StudyActionPayload> TYPE = new Type<>(UnknownEchoes.id("study_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StudyActionPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StudyActionPayload::session,
            ByteBufCodecs.VAR_INT, StudyActionPayload::revision,
            ByteBufCodecs.VAR_INT, StudyActionPayload::operation,
            ByteBufCodecs.VAR_INT, StudyActionPayload::first,
            ByteBufCodecs.VAR_INT, StudyActionPayload::second, StudyActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
