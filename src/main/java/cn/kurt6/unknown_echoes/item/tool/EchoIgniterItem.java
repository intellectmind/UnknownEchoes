package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EchoIgniterItem extends FlintAndSteelItem {
    public EchoIgniterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool.echo_igniter.role")
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        boolean bypassProtection = player != null && (player.isCreative() || player.hasPermissions(2));
        if (!bypassProtection
                && context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.CRITICAL_BLOCKS_TAG)) {
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.igniter.protected"), true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
}
