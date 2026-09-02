package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN,
                SoundSource.BLOCKS, 1.2F, 0.72F);
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 1.15F);
        level.sendParticles(ParticleTypes.ENCHANT,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.05D,
                worldPosition.getZ() + 0.5D, 54,
                0.75D, 0.35D, 0.75D, 0.08D);
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
            serverLevel.playSound(null, pos, SoundEvents.VAULT_CLOSE_SHUTTER,
                    SoundSource.BLOCKS, 1.35F, 0.82F);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, 0.62F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5D, pos.getY() + 1.3D, pos.getZ() + 0.5D,
                    64, 0.85D, 0.7D, 0.85D, 0.08D);
        } else if (age == WRITING_TICKS + SEALING_TICKS) {
            serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.BLOCKS, 1.5F, 0.72F);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS, 1.2F, 1.18F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.65D, pos.getZ() + 0.5D,
                    84, 1.05D, 1.0D, 1.05D, 0.1D);
        }

        if ((age & 3) == 0) {
            emitStageParticles(serverLevel, pos, stageForAge(age), age);
        }
    }

    private static void emitStageParticles(ServerLevel level, BlockPos pos,
                                           PerformanceStage stage, int age) {
        double angle = age * 0.19D;
        double radius = stage == PerformanceStage.ARCHIVED ? 1.05D : 0.72D;
        double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
        double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
        double y = pos.getY() + switch (stage) {
            case WRITING -> 1.05D + (age % 24) * 0.025D;
            case SEALING -> 1.2D + Math.sin(angle * 2.0D) * 0.22D;
            case ARCHIVED -> 1.55D + Math.sin(angle) * 0.35D;
            case IDLE -> 1.0D;
        };
        var particle = switch (stage) {
            case WRITING -> ParticleTypes.ENCHANT;
            case SEALING -> ParticleTypes.ELECTRIC_SPARK;
            case ARCHIVED -> ParticleTypes.END_ROD;
            case IDLE -> ParticleTypes.GLOW;
        };
        level.sendParticles(particle, x, y, z, stage == PerformanceStage.ARCHIVED ? 3 : 2,
                0.06D, 0.08D, 0.06D, 0.015D);
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
