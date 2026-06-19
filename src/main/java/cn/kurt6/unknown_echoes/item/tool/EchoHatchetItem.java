package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EchoHatchetItem extends AxeItem {
    private static final int RESIN_COOLDOWN_TICKS = 200;

    public EchoHatchetItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.add(tooltip, "echo_hatchet");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level
                && context.getPlayer() instanceof ServerPlayer player
                && isEchoLog(context)) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                give(player, new ItemStack(ModItems.SILENT_RESIN.get()));
                player.getCooldowns().addCooldown(this, RESIN_COOLDOWN_TICKS);
                context.getItemInHand().hurtAndBreak(1, player, player.getSlotForHand(context.getHand()));
                level.playSound(null, context.getClickedPos(), SoundEvents.HONEY_BLOCK_BREAK,
                        SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    private static boolean isEchoLog(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return state.is(ModBlocks.ECHO_LOG.get())
                || state.is(ModBlocks.WHISPERING_LOG.get())
                || state.is(ModBlocks.TIDEWOOD_LOG.get());
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
