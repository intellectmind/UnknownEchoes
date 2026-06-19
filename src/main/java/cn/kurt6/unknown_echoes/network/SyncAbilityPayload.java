package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 服务端 → 客户端:同步玩家已解锁能力、已激活信标、已解锁维度摘要、能力研究等级、
 * 核心机关完成标记(V0.6E 凭据与誓记页)与最近失败提示(V0.6E 能力面板)。
 * 研究等级条目格式 "ability_id:level";失败条目格式 "ability_id:failure_key"。
 * 全部为表现层数据,客户端不可反向声明。
 */
public record SyncAbilityPayload(List<String> abilities, List<String> beacons,
                                 List<String> dimensions, List<String> research,
                                 List<String> ritualTokens, List<String> failures)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("sync_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::abilities,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::beacons,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::dimensions,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::research,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::ritualTokens,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncAbilityPayload::failures,
                    SyncAbilityPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
