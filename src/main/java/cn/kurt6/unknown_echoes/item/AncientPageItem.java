package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 远古记录残页:环境叙事物品(普通物品,可正常掉落/交易)。 */
public class AncientPageItem extends Item {
    private static final int DUPLICATE_RECYCLE_COUNT = 2;

    public AncientPageItem(Properties properties) {
        super(properties);
    }

    private static int pageId(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PAGE_ID.get(), 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        int id = pageId(stack);
        tooltip.add(Component.translatable("page.unknown_echoes." + id + ".title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.ancient_page.read",
                        Component.keybind("key.use"))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {

            cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.openAncientPage(pageId(stack));
            level.playSound(player, player.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int id = pageId(stack);
            if (!AncientPageCatalog.isReleasePage(id)) {
                JournalManager.recordPage(serverPlayer, id);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (JournalManager.getData(serverPlayer).getPages().contains(id)) {
                JournalManager.retryAllPagesReward(serverPlayer);
                if (!serverPlayer.getAbilities().instabuild) {
                    recycleDuplicate(serverPlayer, stack);
                }
            } else {
                JournalManager.recordPage(serverPlayer, id);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void recycleDuplicate(ServerPlayer player, ItemStack pageStack) {
        ItemStack reward = new ItemStack(
                ModItems.RECORD_TRACING_PAPER.get(), DUPLICATE_RECYCLE_COUNT);
        pageStack.shrink(1);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.ancient_page.recycled",
                new ItemStack(ModItems.RECORD_TRACING_PAPER.get()).getHoverName(),
                DUPLICATE_RECYCLE_COUNT), true);
    }
}
