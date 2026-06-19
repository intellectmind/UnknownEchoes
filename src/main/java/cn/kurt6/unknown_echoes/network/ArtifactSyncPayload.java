package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 服务端 → 客户端:同步神器与回响能量展示数据(12.2)。
 * artifacts 条目格式 "id:level:tuning:serial:cooldownEnd"(纯展示;判定永远在服务端)。
 * 能量以 1/20 点同步,客户端按 regenHundredthsPerTick + gameTime 戳本地外推显示。
 */
public record ArtifactSyncPayload(List<String> artifacts, int energyTwentieths,
                                  int maxEnergy, int regenHundredthsPerTick, long gameTime)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ArtifactSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("sync_artifact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ArtifactSyncPayload::artifacts,
                    ByteBufCodecs.VAR_INT, ArtifactSyncPayload::energyTwentieths,
                    ByteBufCodecs.VAR_INT, ArtifactSyncPayload::maxEnergy,
                    ByteBufCodecs.VAR_INT, ArtifactSyncPayload::regenHundredthsPerTick,
                    ByteBufCodecs.VAR_LONG, ArtifactSyncPayload::gameTime,
                    ArtifactSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
