package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端只提交深层回响动作意图，能力、能量、冷却和落点均由服务端复核。 */
public record EchoAbilityActionPayload(byte action) implements CustomPacketPayload {
    public static final byte ACTION_GRAVITY_ATTUNE = 0;
    public static final byte ACTION_VOID_STEP = 1;

    public static final EchoAbilityActionPayload GRAVITY_ATTUNE =
            new EchoAbilityActionPayload(ACTION_GRAVITY_ATTUNE);
    public static final EchoAbilityActionPayload VOID_STEP =
            new EchoAbilityActionPayload(ACTION_VOID_STEP);

    public static final Type<EchoAbilityActionPayload> TYPE =
            new Type<>(UnknownEchoes.id("echo_ability_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoAbilityActionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, EchoAbilityActionPayload::action,
                    EchoAbilityActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
