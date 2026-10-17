package cn.kurt6.unknown_echoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** 共餐台与野炊炉的即时支援资格，在效果回调后重新确认。 */
public final class FacilitySupport {
    private FacilitySupport() {
    }

    public static boolean isEligible(ServerLevel level, BlockPos pos, double radius, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= radius * radius;
    }
}
