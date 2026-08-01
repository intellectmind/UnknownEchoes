package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 服务端到客户端的重力/虚空回响冷却展示快照。 */
public record AbilityCooldownSyncPayload(long gravityEnd, int gravityDuration,
                                         long voidEnd, int voidDuration, long gameTime)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilityCooldownSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("sync_ability_cooldowns"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCooldownSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, AbilityCooldownSyncPayload::gravityEnd,
                    ByteBufCodecs.VAR_INT, AbilityCooldownSyncPayload::gravityDuration,
                    ByteBufCodecs.VAR_LONG, AbilityCooldownSyncPayload::voidEnd,
                    ByteBufCodecs.VAR_INT, AbilityCooldownSyncPayload::voidDuration,
                    ByteBufCodecs.VAR_LONG, AbilityCooldownSyncPayload::gameTime,
                    AbilityCooldownSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
