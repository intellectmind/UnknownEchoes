package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BossMusicPayload(ResourceLocation dimension, ResourceLocation boss, int stage, boolean muted)
        implements CustomPacketPayload {
    public static final Type<BossMusicPayload> TYPE = new Type<>(UnknownEchoes.id("boss_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BossMusicPayload> STREAM_CODEC =
            StreamCodec.of(BossMusicPayload::write, BossMusicPayload::read);

    public BossMusicPayload {
        if (stage < 0 || stage > 4) throw new IllegalArgumentException("Invalid music stage");
    }

    private static void write(RegistryFriendlyByteBuf buffer, BossMusicPayload payload) {
        buffer.writeResourceLocation(payload.dimension());
        buffer.writeResourceLocation(payload.boss());
        buffer.writeVarInt(payload.stage());
        buffer.writeBoolean(payload.muted());
    }

    private static BossMusicPayload read(RegistryFriendlyByteBuf buffer) {
        return new BossMusicPayload(buffer.readResourceLocation(), buffer.readResourceLocation(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
