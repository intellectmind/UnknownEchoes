package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 回响索引:阅读型查询物品。右键展示第一维度闭环进度并在条件齐备时领取 T7 个人奖励。 */
public class EchoIndexItem extends EchoMaterialItem {

    public EchoIndexItem(Properties properties) {
        super("echo_index", properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            EchoCompletionManager.handleIndexBookUse(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_index.hint",
                        Component.keybind("key.use"))
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
