package cn.kurt6.unknown_echoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 共餐台与野炊炉的单份支付：只在当前设施确实进入工作状态后提供支援。 */
public final class FacilityActivation {
    private FacilityActivation() {
    }

    public static boolean activate(ServerLevel level, BlockPos pos, BlockState before,
                                   BlockState active, ServerPlayer player, ItemStack input, int activeTicks) {
        // 原版在右键事件之前缓存方块状态，回调可能已激活、移走或替换该设施。
        if (!player.isAlive() || !player.canInteractWithBlock(pos, 1.0D)
                || player.level() != level || input.isEmpty() || level.getBlockState(pos) != before) {
            return false;
        }
        ItemStack payment = player.getAbilities().instabuild ? ItemStack.EMPTY : input.split(1);
        boolean rejected = false;
        try {
            rejected = !level.setBlock(pos, active, Block.UPDATE_ALL);
            return !rejected && level.getBlockState(pos) == active;
        } finally {
            // 写入后的通知可能抛异常；仍在工作态的设施必须登记恢复计时。
            boolean current = level.getBlockState(pos) == active;
            if (current) {
                level.scheduleTick(pos, active.getBlock(), activeTicks);
            }
            // 未提交时退回预留的原栈，保留组件，也不覆盖回调占用的手持槽。
            if ((rejected || !current) && !payment.isEmpty() && !player.getInventory().add(payment)) {
                player.drop(payment, false);
            }
        }
    }
}
