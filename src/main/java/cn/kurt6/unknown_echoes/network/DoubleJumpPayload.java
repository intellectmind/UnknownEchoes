package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端:风之回响空中动作(二段跳 / 滑翔开始 / 滑翔保持)。
 * 服务端校验能力后重置摔落距离并按 mode 播放对应音效;
 * mode 只区分表现,能力判定不变,伪造包最多白听一声风。
 */
public record DoubleJumpPayload(byte mode) implements CustomPacketPayload {

    public static final byte MODE_JUMP = 0;
    public static final byte MODE_GLIDE_START = 1;
    public static final byte MODE_GLIDE_HOLD = 2;

    public static final DoubleJumpPayload JUMP = new DoubleJumpPayload(MODE_JUMP);
    public static final DoubleJumpPayload GLIDE_START = new DoubleJumpPayload(MODE_GLIDE_START);
    public static final DoubleJumpPayload GLIDE_HOLD = new DoubleJumpPayload(MODE_GLIDE_HOLD);

    public static final CustomPacketPayload.Type<DoubleJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("double_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, DoubleJumpPayload::mode, DoubleJumpPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
