package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

final class DeepArrayBlockAccess {
    private DeepArrayBlockAccess() {
    }

    @Nullable
    static BlockState getIfReady(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            // 区块票据不能证明 FULL 数据就绪；直接读取返回区块，避免世界接口等待加载。
            var chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockState(pos);
        }
        return level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }
}
