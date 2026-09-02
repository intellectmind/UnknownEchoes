package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端神器快捷键请求；服务端按当前手持凭据重新判定神器类型。 */
public record ArtifactUsePayload(boolean offhand, boolean bind) implements CustomPacketPayload {
    public static final ArtifactUsePayload MAIN_HAND = new ArtifactUsePayload(false, false);
    public static final ArtifactUsePayload OFF_HAND = new ArtifactUsePayload(true, false);
    public static final ArtifactUsePayload MAIN_HAND_BIND = new ArtifactUsePayload(false, true);
    public static final ArtifactUsePayload OFF_HAND_BIND = new ArtifactUsePayload(true, true);
    public static final Type<ArtifactUsePayload> TYPE = new Type<>(UnknownEchoes.id("artifact_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactUsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ArtifactUsePayload::offhand,
                    ByteBufCodecs.BOOL, ArtifactUsePayload::bind,
                    ArtifactUsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
