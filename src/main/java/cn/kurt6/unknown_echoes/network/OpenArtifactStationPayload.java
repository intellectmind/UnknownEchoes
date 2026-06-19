package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端 → 客户端:打开回响总览的"神器"页并携带台座上下文(记录台/升级台坐标与类型)。
 * 5.8:神器台/记录台交互统一进入总览壳,不开独立原版界面。
 * stationMode: 0=记录台(领取/复领) 1=升级台(升级/调谐)
 */
public record OpenArtifactStationPayload(int stationMode, BlockPos pos)
        implements CustomPacketPayload {

    public static final int MODE_RECORD = 0;
    public static final int MODE_TUNING = 1;

    public static final CustomPacketPayload.Type<OpenArtifactStationPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("open_artifact_station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenArtifactStationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenArtifactStationPayload::stationMode,
                    BlockPos.STREAM_CODEC, OpenArtifactStationPayload::pos,
                    OpenArtifactStationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
