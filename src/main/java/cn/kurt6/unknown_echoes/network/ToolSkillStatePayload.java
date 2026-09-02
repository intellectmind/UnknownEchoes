package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ToolSkillStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<ToolSkillStatePayload> TYPE =
            new Type<>(UnknownEchoes.id("tool_skill_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSkillStatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ToolSkillStatePayload::active,
                    ToolSkillStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
