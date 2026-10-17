package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端:打开神器页并携带台座类型、坐标与维度。 */
public record OpenArtifactStationPayload(int stationMode, BlockPos pos, ResourceLocation dimension)
        implements CustomPacketPayload {

    public static final int MODE_RECORD = 0;
    public static final int MODE_TUNING = 1;

    public static final CustomPacketPayload.Type<OpenArtifactStationPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("open_artifact_station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenArtifactStationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenArtifactStationPayload::stationMode,
                    BlockPos.STREAM_CODEC, OpenArtifactStationPayload::pos,
                    ResourceLocation.STREAM_CODEC, OpenArtifactStationPayload::dimension,
                    OpenArtifactStationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
