package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PuzzleCoreBlockEntity extends BlockEntity {
    public static final int DEFAULT_SEQUENCE_LENGTH = 4;
    private static final int[] DEFAULT_SEQUENCE = {0, 1, 2, 3};
    private static final int EFFECT_RADIUS = PuzzleSolveEffects.EFFECT_RADIUS;

    protected int progress = 0;
    /** 需要依次点击的 ORDER 序列(默认升序,结构生成时可乱序覆盖)。 */
    protected int[] sequence = DEFAULT_SEQUENCE.clone();

    public PuzzleCoreBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PUZZLE_CORE.get(), pos, state);
    }

    protected PuzzleCoreBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                    BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getProgress() {
        return progress;
    }

    public int getSequenceLength() {
        return sequence.length;
    }

    /** 结构生成时写入乱序序列;同时重置进度。 */
    public void setSequence(int[] sequence) {
        if (isValidSequence(sequence)) {
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

        if (this.progress < 0 || this.progress >= this.sequence.length) {
            this.progress = 0;
            this.setChanged();
        }
        if (order == this.sequence[this.progress]) {
            this.progress++;
            this.setChanged();
            BlockState runeState = level.getBlockState(runePos);
            if (isSequenceInput(runeState)) {
                level.setBlock(runePos, runeState.setValue(SequenceRuneBlock.LIT, Boolean.TRUE), 3);
            }
            level.playSound(null, runePos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.2F, 0.8F + 0.2F * this.progress);
            if (this.progress >= this.sequence.length) {
                this.solve(level);
            }
        } else {
            this.progress = 0;
            this.setChanged();
            this.resetRunes(level);
            level.playSound(null, this.worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    private void solve(Level level) {
        PuzzleSolveEffects.settle(level, this.worldPosition,
                this.getBlockState().setValue(PuzzleCoreBlock.ACTIVE, Boolean.TRUE));
    }


    /** 熄灭附近未解谜的顺序柱;高处(核心上方 3 格以上)的提示柱不动。 */
    protected void resetRunes(Level level) {
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                this.worldPosition.offset(EFFECT_RADIUS, 2, EFFECT_RADIUS))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isSequenceInput(state) && state.getValue(SequenceRuneBlock.LIT)) {
                level.setBlock(pos, state.setValue(SequenceRuneBlock.LIT, Boolean.FALSE), 3);
            }
        }
    }

    /** 顺序符文柱与步纹符文共用 LIT/ORDER 属性,核心统一点亮与熄灭两类输入方块。 */
    protected static boolean isSequenceInput(BlockState state) {
        return state.is(ModBlocks.SEQUENCE_RUNE.get()) || state.is(ModBlocks.STEP_RUNE.get());
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
        this.sequence = DEFAULT_SEQUENCE.clone();
        int[] saved = tag.getIntArray("Sequence");
        if (isValidSequence(saved)) {

            this.sequence = saved.clone();
        }
        int maxProgress = this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE)
                ? this.sequence.length : Math.max(0, this.sequence.length - 1);
        this.progress = Math.max(0, Math.min(tag.getInt("Progress"), maxProgress));

    }

    private static boolean isValidSequence(int[] candidate) {
        if (candidate == null || candidate.length == 0 || candidate.length > 8) {
            return false;
        }
        boolean[] seen = new boolean[8];
        for (int value : candidate) {
            if (value < 0 || value >= seen.length || seen[value]) {
                return false;
            }
            seen[value] = true;
        }
        return true;
    }
}
