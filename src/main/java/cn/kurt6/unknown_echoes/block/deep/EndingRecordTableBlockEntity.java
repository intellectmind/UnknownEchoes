package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 结局记录台的世界共享演出状态；个人结局资格仍保存在玩家进度中。 */
public class EndingRecordTableBlockEntity extends BlockEntity {
    public static final int WRITING_TICKS = 80;
    public static final int SEALING_TICKS = 60;
    public static final int ARCHIVED_TICKS = 80;
    public static final int TOTAL_TICKS = WRITING_TICKS + SEALING_TICKS + ARCHIVED_TICKS;

    private long recordingStartedAt = -1L;

    public EndingRecordTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDING_RECORD_TABLE.get(), pos, state);
    }

    public void beginRecording(ServerLevel level) {
        recordingStartedAt = level.getGameTime();
        setChangedAndSync();
        level.playSound(null, worldPosition, ModSounds.BLOCK_ENDING_RECORD_WRITE.get(),
                SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    public boolean isRecording() {
        return recordingStartedAt >= 0L && level != null
                && animationAge(0.0F) < TOTAL_TICKS;
    }

    public float animationAge(float partialTick) {
        if (recordingStartedAt < 0L || level == null) {
            return -1.0F;
        }
        long now = level.getGameTime();
        if (now < recordingStartedAt) {
            return 0.0F;
        }
        long age = Math.min(Integer.MAX_VALUE, now - recordingStartedAt);
        return age + partialTick;
    }

    public PerformanceStage getStage(float partialTick) {
        return stageForAge(animationAge(partialTick));
    }

    private static PerformanceStage stageForAge(float age) {
        if (age < 0.0F || age >= TOTAL_TICKS) {
            return PerformanceStage.IDLE;
        }
        if (age < WRITING_TICKS) {
            return PerformanceStage.WRITING;
        }
        if (age < WRITING_TICKS + SEALING_TICKS) {
            return PerformanceStage.SEALING;
        }
        return PerformanceStage.ARCHIVED;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  EndingRecordTableBlockEntity table) {
        if (!(level instanceof ServerLevel serverLevel) || table.recordingStartedAt < 0L) {
            return;
        }
        long now = level.getGameTime();
        if (now < table.recordingStartedAt) {
            table.recordingStartedAt = now;
            table.setChangedAndSync();
        }
        int age = (int) Math.min(Integer.MAX_VALUE, now - table.recordingStartedAt);
        if (age >= TOTAL_TICKS) {
            table.recordingStartedAt = -1L;
            table.setChangedAndSync();
            return;
        }

        if (age == WRITING_TICKS) {
            serverLevel.playSound(null, pos, ModSounds.BLOCK_ENDING_RECORD_SEAL.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
            MechanismFx.archiveSeal(serverLevel, Vec3.atBottomCenterOf(pos), false);
        } else if (age == WRITING_TICKS + SEALING_TICKS) {
            serverLevel.playSound(null, pos, ModSounds.BLOCK_ENDING_RECORD_ARCHIVE.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
            MechanismFx.archiveSeal(serverLevel, Vec3.atBottomCenterOf(pos), true);
        }

        if ((age & 3) == 0) {
            emitStageParticles(serverLevel, pos, stageForAge(age), age);
        }
    }

    private static void emitStageParticles(ServerLevel level, BlockPos pos,
                                           PerformanceStage stage, int age) {
        MechanismFx.emit(level, MechanismFx.archiveFrame(
                Vec3.atBottomCenterOf(pos), stage.ordinal(), age));
    }

    private void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (recordingStartedAt >= 0L) {
            tag.putLong("RecordingStartedAt", recordingStartedAt);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recordingStartedAt = tag.contains("RecordingStartedAt", Tag.TAG_LONG)
                ? tag.getLong("RecordingStartedAt") : -1L;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public enum PerformanceStage {
        IDLE,
        WRITING,
        SEALING,
        ARCHIVED
    }
}
