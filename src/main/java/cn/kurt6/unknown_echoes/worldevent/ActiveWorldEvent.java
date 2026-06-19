package cn.kurt6.unknown_echoes.worldevent;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 一个进行中的世界事件实例(22.10 事件字段的最小子集)。
 * 只存内存:环境事件是低频氛围内容,服务器重启即消散(个人"目击"记录另存玩家数据)。
 */
public class ActiveWorldEvent {
    public final WorldEventType type;
    public final BlockPos center;
    public final long endGameTime;
    /** 已记录"目击"的玩家(本次事件内防重复;跨事件的首次目击记录在玩家机关数据里)。 */
    public final Set<UUID> witnessed = new HashSet<>();

    public ActiveWorldEvent(WorldEventType type, BlockPos center, long endGameTime) {
        this.type = type;
        this.center = center;
        this.endGameTime = endGameTime;
    }
}
