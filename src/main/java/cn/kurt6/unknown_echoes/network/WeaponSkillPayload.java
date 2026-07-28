package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端:战技键触发手持武器主动技能。
 * 服务端校验手持物品与冷却后结算,伪造包最多触发一次与右键等价的战技。
 */
public record WeaponSkillPayload(byte hand) implements CustomPacketPayload {

    public static final byte HAND_MAIN = 0;
    public static final byte HAND_OFF = 1;

    public static final WeaponSkillPayload MAIN = new WeaponSkillPayload(HAND_MAIN);
    public static final WeaponSkillPayload OFF = new WeaponSkillPayload(HAND_OFF);

    public static final CustomPacketPayload.Type<WeaponSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(UnknownEchoes.id("weapon_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponSkillPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, WeaponSkillPayload::hand, WeaponSkillPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
