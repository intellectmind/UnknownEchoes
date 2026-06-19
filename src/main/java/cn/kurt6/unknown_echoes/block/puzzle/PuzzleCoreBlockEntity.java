package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 谜题核心 BlockEntity:记录顺序谜题进度。
 * 每个核心持有自己的激活序列(结构生成时随机打乱写入,旧档默认 0→3 升序兼容):
 * - 点对序列中的下一个序号:柱子点亮,进度 +1;全部点亮 → 激活,清除周围封印石
 * - 点错:全部低处柱子熄灭,进度归零(高处的提示柱不受影响)
 * 进度是世界状态(公共谜题),解开一次即永久开启。序列只存服务端,客户端只能看壁画提示。
 */
public class PuzzleCoreBlockEntity extends BlockEntity {
    /** 旧档/未显式设置时的默认序列长度。 */
    public static final int DEFAULT_SEQUENCE_LENGTH = 4;
    private static final int EFFECT_RADIUS = 10;

    private int progress = 0;
    /** 需要依次点击的 ORDER 序列(默认升序,结构生成时可乱序覆盖)。 */
    private int[] sequence = {0, 1, 2, 3};

    public PuzzleCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUZZLE_CORE.get(), pos, state);
    }

    public int getProgress() {
        return progress;
    }

    public int getSequenceLength() {
        return sequence.length;
    }

    /** 结构生成时写入乱序序列;同时重置进度。 */
    public void setSequence(int[] sequence) {
        if (sequence != null && sequence.length > 0) {
            this.sequence = sequence.clone();
            this.progress = 0;
            this.setChanged();
        }
    }

    public void tryActivate(int order, BlockPos runePos, ServerPlayer player) {
        Level level = this.level;
        if (level == null || level.isClientSide
                || this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE)) {
            return;
        }

        if (order == this.sequence[this.progress]) {
            this.progress++;
            this.setChanged();
            BlockState runeState = level.getBlockState(runePos);
            if (runeState.is(ModBlocks.SEQUENCE_RUNE.get())) {
                level.setBlock(runePos, runeState.setValue(SequenceRuneBlock.LIT, Boolean.TRUE), 3);
            }
            level.playSound(null, runePos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.2F, 0.8F + 0.2F * this.progress);
            if (this.progress >= this.sequence.length) {
                this.solve(level);
            } else if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.puzzle_core.progress", this.progress, this.sequence.length), true);
            }
        } else {
            this.progress = 0;
            this.setChanged();
            this.resetRunes(level);
            level.playSound(null, this.worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 1.0F, 0.6F);
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.puzzle_core.reset"), true);
            }
        }
    }

    private void solve(Level level) {
        level.setBlock(this.worldPosition,
                this.getBlockState().setValue(PuzzleCoreBlock.ACTIVE, Boolean.TRUE), 3);
        level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.5F, 1.2F);
        // 清除封印石,打开内室
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                this.worldPosition.offset(EFFECT_RADIUS, 6, EFFECT_RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.SEALED_STONE.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer nearby : serverLevel.players()) {
                if (nearby.distanceToSqr(this.worldPosition.getX(), this.worldPosition.getY(),
                        this.worldPosition.getZ()) <= 32 * 32) {
                    nearby.sendSystemMessage(
                            Component.translatable("message.unknown_echoes.puzzle_core.opened"));
                    // V0.6D:解开谜题一次性回填回响能量(12.2,场地内参与者各自回填)
                    cn.kurt6.unknown_echoes.artifact.ArtifactManager.refillEnergy(nearby,
                            cn.kurt6.unknown_echoes.config.ServerConfig.ENERGY_REFILL_PUZZLE.get());
                }
            }
        }
    }

    /** 熄灭附近未解谜的顺序柱;高处(核心上方 3 格以上)的提示柱不动。 */
    private void resetRunes(Level level) {
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                this.worldPosition.offset(EFFECT_RADIUS, 2, EFFECT_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.SEQUENCE_RUNE.get()) && state.getValue(SequenceRuneBlock.LIT)) {
                level.setBlock(pos, state.setValue(SequenceRuneBlock.LIT, Boolean.FALSE), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", this.progress);
        tag.putIntArray("Sequence", this.sequence);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("Progress");
        int[] saved = tag.getIntArray("Sequence");
        if (saved.length > 0) {
            this.sequence = saved;
        }
        // 旧档无 Sequence 字段:保留默认 0→3 升序,行为与 V0.2 一致
    }
}
