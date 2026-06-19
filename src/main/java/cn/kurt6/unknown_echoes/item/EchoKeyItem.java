package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.block.beacon.EchoBeaconBlock;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 回声境域钥匙:用回响碎片与核心合成,消耗后唤醒残响信标。
 * 维度入口状态仍记录在玩家数据中,不是依赖物品携带的通行证。
 */
public class EchoKeyItem extends Item {

    public EchoKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(ModBlocks.RESONANCE_BEACON.get())) {
            return super.useOn(context);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            return EchoBeaconBlock.activateWithKey(
                    level, context.getClickedPos(), serverPlayer, context.getItemInHand());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_key")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
