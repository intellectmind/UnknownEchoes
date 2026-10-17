package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StudyCollectionPayload(int earnedMask, int claimedMask) implements CustomPacketPayload {
    public static final Type<StudyCollectionPayload> TYPE = new Type<>(UnknownEchoes.id("study_collection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StudyCollectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StudyCollectionPayload::earnedMask,
            ByteBufCodecs.VAR_INT, StudyCollectionPayload::claimedMask, StudyCollectionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
