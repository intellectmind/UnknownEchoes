package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.item.InventoryPayment;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EchoBrushItem extends BrushItem {
    private static final int CLUE_COOLDOWN_TICKS = 60;

    public EchoBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ECHO_FIBER.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.add(tooltip, "echo_brush");
    }

    /** 线索刷取作为原版刷子的附加结算，交互本身始终交回 `super.useOn`。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level
                && context.getPlayer() instanceof ServerPlayer player
                && isClueSurface(context)) {
            brushClue(level, player, context);
        }
        return super.useOn(context);
    }

    private static boolean isClueSurface(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return state.is(ModBlocks.RUIN_BLOCKS_TAG) || state.is(ModBlocks.RESOURCE_BLOCKS_TAG);
    }

    private boolean brushClue(ServerLevel level, ServerPlayer player, UseOnContext context) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        if (!hasEchoDust(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.echo_brush.echo_dust_required"), true);
            return false;
        }
        player.getCooldowns().addCooldown(this, CLUE_COOLDOWN_TICKS);
        context.getItemInHand().hurtAndBreak(1, player, player.getSlotForHand(context.getHand()));
        // 耐久回调可能改变背包，发奖必须以本次实际扣料成功为准。
        if (level.random.nextFloat() < 0.35F && consumeEchoDust(player)) {
            give(player, new ItemStack(ModItems.RECORD_TRACING_PAPER.get()));
        }
        var pos = context.getClickedPos();
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.05,
                pos.getZ() + 0.5, 5, 0.25, 0.1, 0.25, 0.01);
        level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED,
                SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    private static boolean hasEchoDust(ServerPlayer player) {
        return player.getAbilities().instabuild
                || InventoryPayment.count(player, ModItems.ECHO_DUST.get()) > 0;
    }

    private static boolean consumeEchoDust(ServerPlayer player) {
        return player.getAbilities().instabuild
                || InventoryPayment.consume(player, ModItems.ECHO_DUST.get(), 1) == 1;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
