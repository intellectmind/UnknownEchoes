package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BossImpactPayload(double x, double y, double z, float radius,
                                float strength, int durationTicks, int color)
        implements CustomPacketPayload {
    public static final Type<BossImpactPayload> TYPE =
            new Type<>(UnknownEchoes.id("boss_impact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossImpactPayload> STREAM_CODEC =
            StreamCodec.of(BossImpactPayload::write, BossImpactPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, BossImpactPayload payload) {
        buffer.writeDouble(payload.x());
        buffer.writeDouble(payload.y());
        buffer.writeDouble(payload.z());
        buffer.writeFloat(payload.radius());
        buffer.writeFloat(payload.strength());
        buffer.writeVarInt(payload.durationTicks());
        buffer.writeInt(payload.color());
    }

    private static BossImpactPayload read(RegistryFriendlyByteBuf buffer) {
        return new BossImpactPayload(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
