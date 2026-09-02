package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class EchoIgniterItem extends FlintAndSteelItem {
    private static final int RESONANT_LIGHT_RADIUS = 4;
    private static final int RESONANT_LIGHT_VERTICAL_RADIUS = 2;

    public EchoIgniterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EchoTooltips.addWithSkillKeyOnly(tooltip, "echo_igniter");
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

            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer
                && cn.kurt6.unknown_echoes.network.ModNetworking
                        .isResonantLightKeyActive(serverPlayer)
                && context.getLevel() instanceof ServerLevel serverLevel) {
            return lightNearby(serverLevel, serverPlayer, context.getClickedPos()) > 0
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    private static int lightNearby(ServerLevel level, ServerPlayer player, BlockPos origin) {
        int litCount = 0;
        BlockPos from = origin.offset(-RESONANT_LIGHT_RADIUS, -RESONANT_LIGHT_VERTICAL_RADIUS,
                -RESONANT_LIGHT_RADIUS);
        BlockPos to = origin.offset(RESONANT_LIGHT_RADIUS, RESONANT_LIGHT_VERTICAL_RADIUS,
                RESONANT_LIGHT_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.CRITICAL_BLOCKS_TAG) || !canResonantlyLight(state)
                    || !mayModify(level, player, pos)) {
                continue;
            }            if (level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true),
                    Block.UPDATE_ALL)) {
                litCount++;
            }
        }
        if (litCount > 0) {
            level.playSound(null, origin, SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS, 1.0F, 1.05F);
        }
        return litCount;
    }

    /** 逐格权限：出生保护、世界边界与该玩家当前游戏模式的建造限制。 */
    private static boolean mayModify(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return level.mayInteract(player, pos)
                && !player.blockActionRestricted(level, pos,
                        player.gameMode.getGameModeForPlayer());
    }

    private static boolean canResonantlyLight(BlockState state) {
        boolean supported = state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.CANDLES)
                || state.is(BlockTags.CANDLE_CAKES);
        return supported
                && state.hasProperty(BlockStateProperties.LIT)
                && !state.getValue(BlockStateProperties.LIT)
                && (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                || !state.getValue(BlockStateProperties.WATERLOGGED));
    }
}
