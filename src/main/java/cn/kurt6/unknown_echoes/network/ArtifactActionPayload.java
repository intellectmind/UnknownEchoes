package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端 → 服务端:神器页操作请求(领取/复领、升级、调谐)。 */
public record ArtifactActionPayload(int action, String artifactId, String word, BlockPos stationPos)
        implements CustomPacketPayload {

    public static final int ACTION_CLAIM = 0;
    public static final int ACTION_UPGRADE = 1;
    public static final int ACTION_TUNE = 2;
    private static final int MAX_ARTIFACT_ID_LENGTH = 32;
    private static final int MAX_TUNING_WORD_LENGTH = 32;

    public static final CustomPacketPayload.Type<ArtifactActionPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("artifact_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ArtifactActionPayload::action,
                    ByteBufCodecs.stringUtf8(MAX_ARTIFACT_ID_LENGTH), ArtifactActionPayload::artifactId,
                    ByteBufCodecs.stringUtf8(MAX_TUNING_WORD_LENGTH), ArtifactActionPayload::word,
                    BlockPos.STREAM_CODEC, ArtifactActionPayload::stationPos,
                    ArtifactActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
