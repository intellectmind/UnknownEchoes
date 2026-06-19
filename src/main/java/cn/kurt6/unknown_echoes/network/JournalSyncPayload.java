package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 服务端 → 客户端:同步玩家个人探索日志(书本 UI 数据源)。
 * totals 依次为:结构总数 / 群系总数 / 生物总数(含 Boss)/ Boss 总数 / 残页池总数。
 * 纯展示数据,客户端不可反向写回。
 */
public record JournalSyncPayload(List<String> structures, List<String> biomes,
                                 List<String> mobs, List<String> bosses,
                                 List<Integer> pages, List<Integer> totals)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JournalSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("sync_journal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), JournalSyncPayload::structures,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), JournalSyncPayload::biomes,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), JournalSyncPayload::mobs,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), JournalSyncPayload::bosses,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), JournalSyncPayload::pages,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), JournalSyncPayload::totals,
                    JournalSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
