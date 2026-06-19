package cn.kurt6.unknown_echoes.block.archive;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 回声档案终端 BlockEntity:保留终端自身访问痕迹,完成度与奖励仍以玩家数据为准。
 */
public class EchoArchiveTerminalBlockEntity extends BlockEntity {
    private int accessCount;
    private long lastAccessGameTime;

    public EchoArchiveTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ECHO_ARCHIVE_TERMINAL.get(), pos, state);
    }

    public void recordAccess() {
        this.accessCount++;
        this.lastAccessGameTime = this.level == null ? 0L : this.level.getGameTime();
        this.setChanged();
    }

    public int getAccessCount() {
        return accessCount;
    }

    public long getLastAccessGameTime() {
        return lastAccessGameTime;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AccessCount", this.accessCount);
        tag.putLong("LastAccessGameTime", this.lastAccessGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.accessCount = tag.getInt("AccessCount");
        this.lastAccessGameTime = tag.getLong("LastAccessGameTime");
    }
}
