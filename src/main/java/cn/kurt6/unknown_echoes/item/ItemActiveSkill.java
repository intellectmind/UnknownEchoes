package cn.kurt6.unknown_echoes.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface ItemActiveSkill {

    boolean activateItemSkill(ServerLevel level, ServerPlayer player,
                              ItemStack stack, InteractionHand hand);

    /** 是否提供副功能（模式切换、绑定等）；决定客户端是否发送副功能请求。 */
    default boolean hasAlternateItemSkill() {
        return false;
    }

    default boolean activateAlternateItemSkill(ServerLevel level, ServerPlayer player,
                                               ItemStack stack, InteractionHand hand) {
        return false;
    }
}
