package cn.kurt6.unknown_echoes.worldevent;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ActiveWorldEvent {
    public final WorldEventType type;
    public final BlockPos center;
    public final long endGameTime;
    private long lastObservedGameTime;
    /** 已记录"目击"的玩家(本次事件内防重复;跨事件的首次目击记录在玩家机关数据里)。 */
    public final Set<UUID> witnessed = new HashSet<>();
    /** 三域旅行异象只执行一次生态响应，避免反复覆盖生灵导航。 */
    public boolean ecologyResponded;
    /** 植被共鸣与生灵响应错峰执行，避免表现和提示相互覆盖。 */
    public long floraResponseGameTime = Long.MAX_VALUE;
    /** 植被共鸣每次事件只执行一次，避免重复覆盖同一片植被。 */
    public boolean floraResponded;

    public ActiveWorldEvent(WorldEventType type, BlockPos center, long startedGameTime,
                            long endGameTime) {
        this.type = type;
        this.center = center;
        this.lastObservedGameTime = startedGameTime;
        this.endGameTime = endGameTime;
    }

    public boolean isExpired(long gameTime) {
        boolean rolledBack = gameTime < this.lastObservedGameTime;
        this.lastObservedGameTime = gameTime;
        return rolledBack || gameTime >= this.endGameTime;
    }
}
