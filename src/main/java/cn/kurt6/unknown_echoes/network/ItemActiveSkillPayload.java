package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ItemActiveSkillPayload(byte hand, boolean alternate) implements CustomPacketPayload {
    public static final byte HAND_MAIN = 0;
    public static final byte HAND_OFF = 1;

    public static final ItemActiveSkillPayload MAIN = new ItemActiveSkillPayload(HAND_MAIN, false);
    public static final ItemActiveSkillPayload OFF = new ItemActiveSkillPayload(HAND_OFF, false);
    public static final ItemActiveSkillPayload MAIN_ALTERNATE =
            new ItemActiveSkillPayload(HAND_MAIN, true);
    public static final ItemActiveSkillPayload OFF_ALTERNATE =
            new ItemActiveSkillPayload(HAND_OFF, true);

    public static final CustomPacketPayload.Type<ItemActiveSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("item_active_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemActiveSkillPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE, ItemActiveSkillPayload::hand,
                    ByteBufCodecs.BOOL, ItemActiveSkillPayload::alternate,
                    ItemActiveSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
