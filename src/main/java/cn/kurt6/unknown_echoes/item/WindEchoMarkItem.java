package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 风之回响印记:展示/测试物品。右键将风之回响写入玩家数据(生存模式消耗)。
 * 正式获取途径是击败遗忘巨像;本物品不进入任何掉落表。
 */
public class WindEchoMarkItem extends Item {

    public WindEchoMarkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.mark.already"), true);
            return InteractionResultHolder.consume(stack);
        }
        EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.WIND_ECHO);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_echo_mark")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
