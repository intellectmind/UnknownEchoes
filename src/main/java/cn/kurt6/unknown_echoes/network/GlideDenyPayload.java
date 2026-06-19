package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端 → 客户端:滑翔请求被拒(V0.6E 服务端状态/能量/冷却校验)。
 * 客户端收到后立即取消滑翔表现,并按 reason 显示含蓄提示
 * (lang 键 message.unknown_echoes.glide.deny.<reason>;reason ∈ state/cooldown/energy)。
 */
public record GlideDenyPayload(String reason) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GlideDenyPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("glide_deny"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GlideDenyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, GlideDenyPayload::reason, GlideDenyPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
