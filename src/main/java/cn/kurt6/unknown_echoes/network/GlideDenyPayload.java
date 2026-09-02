package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GlideDenyPayload(String reason) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GlideDenyPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("glide_deny"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GlideDenyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(16), GlideDenyPayload::reason, GlideDenyPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
