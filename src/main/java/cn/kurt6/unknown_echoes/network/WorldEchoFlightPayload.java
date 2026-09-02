package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端只提交展开或收拢翼装的意图，装备与玩家状态由服务端复核。 */
public record WorldEchoFlightPayload(boolean flying) implements CustomPacketPayload {
    public static final WorldEchoFlightPayload START = new WorldEchoFlightPayload(true);
    public static final WorldEchoFlightPayload STOP = new WorldEchoFlightPayload(false);

    public static final Type<WorldEchoFlightPayload> TYPE =
            new Type<>(UnknownEchoes.id("world_echo_flight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldEchoFlightPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, WorldEchoFlightPayload::flying,
                    WorldEchoFlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
