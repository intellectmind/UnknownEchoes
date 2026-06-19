package cn.kurt6.unknown_echoes.item.tool;

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
    private static final int CLUE_COOLDOWN_TICKS = 80;

    public EchoBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.add(tooltip, "echo_brush");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level
                && context.getPlayer() instanceof ServerPlayer player
                && isClueSurface(context)) {
            brushClue(level, player, context);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    private static boolean isClueSurface(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return state.is(ModBlocks.RUIN_BLOCKS_TAG) || state.is(ModBlocks.RESOURCE_BLOCKS_TAG);
    }

    private void brushClue(ServerLevel level, ServerPlayer player, UseOnContext context) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        player.getCooldowns().addCooldown(this, CLUE_COOLDOWN_TICKS);
        context.getItemInHand().hurtAndBreak(1, player, player.getSlotForHand(context.getHand()));
        if (level.random.nextFloat() < 0.25F) {
            give(player, new ItemStack(ModItems.RECORD_TRACING_PAPER.get()));
        }
        var pos = context.getClickedPos();
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.05,
                pos.getZ() + 0.5, 5, 0.25, 0.1, 0.25, 0.01);
        level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED,
                SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
