package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** 可由战技键触发的主动武器技能;实现方自查冷却并负责全部服务端结算。 */
public interface WeaponSkill {

    boolean activateSkill(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand);
}
