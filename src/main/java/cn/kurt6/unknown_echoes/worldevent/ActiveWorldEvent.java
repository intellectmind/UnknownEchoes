package cn.kurt6.unknown_echoes.worldevent;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ActiveWorldEvent {
    public final WorldEventType type;
    public final BlockPos center;
    public final long endGameTime;
    public final long startedGameTime;
    public final boolean testMode;
    public final DisasterState disaster = new DisasterState();
    private long lastObservedGameTime;
    private boolean stopped;
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
        this(type, center, startedGameTime, endGameTime, false);
    }

    public ActiveWorldEvent(WorldEventType type, BlockPos center, long startedGameTime,
                            long endGameTime, boolean testMode) {
        this.type = type;
        this.center = center;
        this.startedGameTime = startedGameTime;
        this.lastObservedGameTime = startedGameTime;
        this.endGameTime = endGameTime;
        this.testMode = testMode;
    }

    public boolean isExpired(long gameTime) {
        boolean rolledBack = gameTime < this.lastObservedGameTime;
        this.lastObservedGameTime = gameTime;
        return this.stopped || rolledBack || gameTime >= this.endGameTime;
    }

    public boolean isStopped() { return this.stopped; }

    /** 观测查询不推进生命周期时钟，避免吞掉调度器尚未处理的时间回退。 */
    public boolean isActiveAt(long gameTime) {
        return !this.stopped && gameTime >= this.lastObservedGameTime && gameTime < this.endGameTime;
    }

    public void stop() {
        this.stopped = true;
        this.disaster.clearMarks();
    }
}
