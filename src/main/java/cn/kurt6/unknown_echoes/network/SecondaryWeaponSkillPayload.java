package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端到服务端：使用独立可改键触发手持武器的副战技。 */
public record SecondaryWeaponSkillPayload(byte hand) implements CustomPacketPayload {
    public static final byte HAND_MAIN = 0;
    public static final byte HAND_OFF = 1;

    public static final SecondaryWeaponSkillPayload MAIN = new SecondaryWeaponSkillPayload(HAND_MAIN);
    public static final SecondaryWeaponSkillPayload OFF = new SecondaryWeaponSkillPayload(HAND_OFF);

    public static final CustomPacketPayload.Type<SecondaryWeaponSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("secondary_weapon_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecondaryWeaponSkillPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE,
                    SecondaryWeaponSkillPayload::hand, SecondaryWeaponSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
