package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PuzzleCoreBlockEntity extends BlockEntity {
    public static final int DEFAULT_SEQUENCE_LENGTH = 4;
    private static final int[] DEFAULT_SEQUENCE = {0, 1, 2, 3};
    private static final int EFFECT_RADIUS = PuzzleSolveEffects.EFFECT_RADIUS;
    private static final int MODE_SEQUENCE = 0;
    private static final int MODE_LIGHTS_OUT = 1;
    private static final int LIGHTS_OUT_SIZE = 8;

    protected int progress = 0;
    /** 需要依次点击的 ORDER 序列(默认升序,结构生成时可乱序覆盖)。 */
    protected int[] sequence = DEFAULT_SEQUENCE.clone();
    private int puzzleMode = MODE_SEQUENCE;
    private long[] lightsOutTiles = new long[0];
    private int lightsOutMask;
    private boolean updatingLightsOut;
    private BlockPos pendingSequenceRune;
    private boolean updatingSequence;
    private boolean runesResetPending;

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

    boolean isLightsOutPuzzle() {
        return this.puzzleMode == MODE_LIGHTS_OUT;
    }

    boolean acceptsLitRuneInput() {
        return this.puzzleMode == MODE_LIGHTS_OUT || this.pendingSequenceRune != null || this.runesResetPending
                || this.progress == this.sequence.length;
    }

    /** 结构生成时写入乱序序列;同时重置进度。 */
    public void setSequence(int[] sequence) {
        if (!isUpdatingInput() && isValidSequence(sequence)) {
            this.sequence = sequence.clone();
            this.progress = 0;
            this.puzzleMode = MODE_SEQUENCE;
            this.lightsOutTiles = new long[0];
            this.lightsOutMask = 0;
            this.pendingSequenceRune = null;
            this.runesResetPending = false;
            this.setChanged();
        }
    }

    /** 结构生成时安装八点回响环;每次按键会翻转自身与环上两侧符文。 */
    public void setLightsOutPattern(List<BlockPos> tiles, int initialMask) {
        if (isUpdatingInput() || !isValidLightsOutTiles(tiles)) {
            return;
        }
        this.puzzleMode = MODE_LIGHTS_OUT;
        this.lightsOutTiles = tiles.stream().mapToLong(BlockPos::asLong).toArray();
        this.lightsOutMask = initialMask & ((1 << LIGHTS_OUT_SIZE) - 1);
        this.progress = Integer.bitCount(this.lightsOutMask);
        this.pendingSequenceRune = null;
        this.runesResetPending = false;
        this.updatingLightsOut = true;
        try {
            this.setChanged();
            if (isCurrentInactiveCore(this.level)) {
                applyLightsOutState(this.level);
            }
        } finally {
            this.updatingLightsOut = false;
        }
    }

    public void tryActivate(int order, BlockPos runePos, ServerPlayer player) {
        Level level = this.level;
        if (!isCurrentInactiveCore(level)) {
            return;
        }
        if (tryFinishPending()) {
            return;
        }

        if (this.puzzleMode == MODE_LIGHTS_OUT) {
            tryLightsOut(level, order, runePos, player);
            return;
        }

        if (!isSequenceRune(level, runePos, order)) {
            return;
        }
        this.updatingSequence = true;
        try {
            if (this.progress < 0 || this.progress >= this.sequence.length) {
                this.resetRunes(level);
                return;
            }
            if (order == this.sequence[this.progress]) {
                this.progress++;
                this.pendingSequenceRune = runePos.immutable();
                this.setChanged();
                if (!applyPendingSequenceRune(level)) {
                    return;
                }
                this.playActivationSound(level, runePos, order);
                if (finishPendingSequenceRune(level) && this.progress >= this.sequence.length) {
                    this.solve(level);
                }
            } else {
                if (this.resetRunes(level) && isCurrentInactiveCore(level)) {
                    level.playSound(null, this.worldPosition, ModSounds.BLOCK_PUZZLE_REJECT.get(),
                            SoundSource.BLOCKS, 0.12F, 1.0F);
                }
            }
        } finally {
            this.updatingSequence = false;
        }
    }

    protected boolean isUpdatingInput() {
        return this.updatingSequence || this.updatingLightsOut;
    }

    private boolean isWithinInputRange(BlockPos pos) {
        return pos != null
                && Math.abs((long) pos.getX() - this.worldPosition.getX()) <= SequenceRuneBlock.CORE_SEARCH_RADIUS
                && Math.abs((long) pos.getY() - this.worldPosition.getY()) <= 4
                && Math.abs((long) pos.getZ() - this.worldPosition.getZ()) <= SequenceRuneBlock.CORE_SEARCH_RADIUS;
    }

    private boolean isSequenceRune(Level level, BlockPos pos, int order) {
        if (!isCurrentInactiveCore(level) || !isWithinInputRange(pos) || !level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return isSequenceInput(state) && state.getValue(SequenceRuneBlock.ORDER) == order;
    }

    private boolean applyPendingSequenceRune(Level level) {
        BlockPos pos = this.pendingSequenceRune;
        if (this.progress <= 0 || this.progress > this.sequence.length
                || !isSequenceRune(level, pos, this.sequence[this.progress - 1])) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.getValue(SequenceRuneBlock.LIT)
                && !level.setBlock(pos, state.setValue(SequenceRuneBlock.LIT, Boolean.TRUE), 3)) {
            return false;
        }
        return isPendingSequenceRuneLit(level);
    }

    private boolean isPendingSequenceRuneLit(Level level) {
        return this.progress > 0 && this.progress <= this.sequence.length
                && isSequenceRune(level, this.pendingSequenceRune, this.sequence[this.progress - 1])
                && level.getBlockState(this.pendingSequenceRune).getValue(SequenceRuneBlock.LIT);
    }

    private boolean finishPendingSequenceRune(Level level) {
        if (!isPendingSequenceRuneLit(level)) {
            return false;
        }
        // 保存通知内仍可中断；保留待写位置到通知返回，重载后重复确认点亮是幂等的。
        this.setChanged();
        if (!isPendingSequenceRuneLit(level)) {
            return false;
        }
        this.pendingSequenceRune = null;
        return true;
    }

    protected void playActivationSound(Level level, BlockPos runePos, int order) {
        level.playSound(null, runePos, ModSounds.BLOCK_PUZZLE_INPUT.get(),
                SoundSource.BLOCKS, 1.2F, 0.8F + 0.14F * Math.clamp(this.progress, 0, 8));
    }

    private void tryLightsOut(Level level, int order, BlockPos runePos, ServerPlayer player) {
        int index = -1;
        for (int i = 0; i < this.lightsOutTiles.length; i++) {
            if (BlockPos.of(this.lightsOutTiles[i]).equals(runePos)) {
                index = i;
                break;
            }
        }
        if (index < 0 || index >= LIGHTS_OUT_SIZE || order != index) {
            return;
        }
        int left = Math.floorMod(index - 1, LIGHTS_OUT_SIZE);
        int right = (index + 1) % LIGHTS_OUT_SIZE;
        this.updatingLightsOut = true;
        try {
            this.lightsOutMask ^= (1 << index) | (1 << left) | (1 << right);
            this.progress = Integer.bitCount(this.lightsOutMask);
            this.setChanged();
            if (!this.applyLightsOutState(level)) {
                return;
            }
            level.playSound(null, runePos, ModSounds.BLOCK_LIGHTS_OUT_INPUT.get(),
                    SoundSource.BLOCKS, 1.1F, 0.75F + 0.08F * Math.clamp(index, 0, 7));
            if (!isLightsOutStateCurrent(level)) {
                return;
            }
            if (this.lightsOutMask == 0) {
                if (player != null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.unknown_echoes.lights_out.solved"), true);
                }
                this.solve(level);
            } else if (player != null) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.unknown_echoes.lights_out.progress",
                                Integer.bitCount(this.lightsOutMask)), true);
            }
        } finally {
            this.updatingLightsOut = false;
        }
    }

    private boolean applyLightsOutState(Level level) {
        boolean complete = true;
        for (int i = 0; i < this.lightsOutTiles.length; i++) {
            if (!isCurrentInactiveCore(level)) {
                return false;
            }
            BlockPos pos = BlockPos.of(this.lightsOutTiles[i]);
            if (!level.hasChunkAt(pos)) {
                complete = false;
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isSequenceInput(state) || state.getValue(SequenceRuneBlock.ORDER) != i) {
                complete = false;
                continue;
            }
            boolean lit = (this.lightsOutMask & (1 << i)) != 0;
            if (state.getValue(SequenceRuneBlock.LIT) != lit
                    && !level.setBlock(pos, state.setValue(SequenceRuneBlock.LIT, lit), 3)) {
                complete = false;
            }
        }
        return complete && isLightsOutStateCurrent(level);
    }

    private boolean isLightsOutStateCurrent(Level level) {
        if (!isCurrentInactiveCore(level)) {
            return false;
        }
        for (int i = 0; i < this.lightsOutTiles.length; i++) {
            BlockPos pos = BlockPos.of(this.lightsOutTiles[i]);
            if (!level.hasChunkAt(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (!isSequenceInput(state) || state.getValue(SequenceRuneBlock.ORDER) != i
                    || state.getValue(SequenceRuneBlock.LIT) != ((this.lightsOutMask & (1 << i)) != 0)) {
                return false;
            }
        }
        return isCurrentInactiveCore(level);
    }

    private boolean recoverLightsOut(Level level) {
        if (this.lightsOutMask != 0 && isLightsOutStateCurrent(level)) {
            return false;
        }
        // 修复显示的这次输入不能同时翻转灯态，否则玩家会操作尚未显示的棋盘。
        this.updatingLightsOut = true;
        try {
            if (applyLightsOutState(level) && this.lightsOutMask == 0) {
                solve(level);
            }
        } finally {
            this.updatingLightsOut = false;
        }
        return true;
    }

    private void solve(Level level) {
        if (!isCurrentInactiveCore(level)) {
            return;
        }
        PuzzleSolveEffects.settle(level, this.worldPosition,
                this.getBlockState().setValue(PuzzleCoreBlock.ACTIVE, Boolean.TRUE));
    }

    // 返回已受理；写入被拒绝时也不能让调用方清空完成进度。
    boolean tryFinishPending() {
        if (!isCurrentInactiveCore(this.level)) {
            return false;
        }
        if (this.updatingLightsOut) {
            return true;
        }
        if (this.updatingSequence) {
            return true;
        }
        if (this.puzzleMode == MODE_LIGHTS_OUT) {
            return recoverLightsOut(this.level);
        }
        if (this.runesResetPending) {
            resetRunes(this.level);
            return true;
        }
        if (this.pendingSequenceRune != null) {
            this.updatingSequence = true;
            try {
                if (applyPendingSequenceRune(this.level) && finishPendingSequenceRune(this.level)
                        && this.progress == this.sequence.length) {
                    solve(this.level);
                }
            } finally {
                this.updatingSequence = false;
            }
            return true;
        }
        if (this.progress != this.sequence.length) {
            return false;
        }
        solve(this.level);
        return true;
    }

    protected boolean isCurrentInactiveCore(Level level) {
        return level != null && !level.isClientSide && !this.isRemoved()
                && level.hasChunkAt(this.worldPosition)
                && level.getBlockEntity(this.worldPosition) == this
                && level.getBlockState(this.worldPosition) == this.getBlockState()
                && !this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE);
    }


    /** 熄灭附近未解谜的顺序柱;高处(核心上方 3 格以上)的提示柱不动。 */
    protected boolean resetRunes(Level level) {
        if (!isCurrentInactiveCore(level) || this.puzzleMode != MODE_SEQUENCE) {
            return false;
        }
        boolean wasUpdatingSequence = this.updatingSequence;
        this.updatingSequence = true;
        try {
            if (!this.runesResetPending) {
                this.progress = 0;
                this.pendingSequenceRune = null;
                this.runesResetPending = true;
                this.setChanged();
            }
            boolean complete = true;
            for (BlockPos pos : sequenceResetPositions()) {
                if (!isCurrentInactiveCore(level)) {
                    return false;
                }
                if (!level.hasChunkAt(pos)) {
                    complete = false;
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (isSequenceInput(state) && state.getValue(SequenceRuneBlock.LIT)
                        && !level.setBlock(pos, state.setValue(SequenceRuneBlock.LIT, Boolean.FALSE), 3)) {
                    complete = false;
                }
            }
            if (!complete || !areSequenceRunesReset(level)) {
                return false;
            }
            this.setChanged();
            if (!areSequenceRunesReset(level)) {
                return false;
            }
            this.runesResetPending = false;
            return true;
        } finally {
            this.updatingSequence = wasUpdatingSequence;
        }
    }

    protected boolean hasPendingRuneReset() {
        return this.runesResetPending;
    }

    private Iterable<BlockPos> sequenceResetPositions() {
        return BlockPos.betweenClosed(
                this.worldPosition.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                this.worldPosition.offset(EFFECT_RADIUS, 2, EFFECT_RADIUS));
    }

    private boolean areSequenceRunesReset(Level level) {
        if (!isCurrentInactiveCore(level)) {
            return false;
        }
        for (BlockPos pos : sequenceResetPositions()) {
            if (!level.hasChunkAt(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (isSequenceInput(state) && state.getValue(SequenceRuneBlock.LIT)) {
                return false;
            }
        }
        return isCurrentInactiveCore(level);
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
        tag.putInt("PuzzleMode", this.puzzleMode);
        tag.putLongArray("LightsOutTiles", this.lightsOutTiles);
        tag.putInt("LightsOutMask", this.lightsOutMask);
        if (this.pendingSequenceRune != null) {
            tag.putLong("PendingSequenceRune", this.pendingSequenceRune.asLong());
        }
        if (this.runesResetPending) {
            tag.putBoolean("RunesResetPending", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.sequence = DEFAULT_SEQUENCE.clone();
        int[] saved = tag.getIntArray("Sequence");
        boolean validSavedSequence = isValidSequence(saved);
        if (validSavedSequence) {

            this.sequence = saved.clone();
        }
        // 完整进度可能先于核心点亮保存，越界进度仍不能作为完成凭据。
        int maxProgress = this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE)
                || validSavedSequence && tag.getInt("Progress") == this.sequence.length
                ? this.sequence.length : Math.max(0, this.sequence.length - 1);
        this.progress = Math.max(0, Math.min(tag.getInt("Progress"), maxProgress));

        this.puzzleMode = tag.getInt("PuzzleMode");
        long[] savedTiles = tag.getLongArray("LightsOutTiles");
        if (this.puzzleMode == MODE_LIGHTS_OUT && savedTiles.length == LIGHTS_OUT_SIZE) {
            List<BlockPos> positions = new ArrayList<>(LIGHTS_OUT_SIZE);
            for (long packed : savedTiles) {
                positions.add(BlockPos.of(packed));
            }
            if (isValidLightsOutTiles(positions)) {
                this.lightsOutTiles = savedTiles.clone();
                this.lightsOutMask = tag.getInt("LightsOutMask")
                        & ((1 << LIGHTS_OUT_SIZE) - 1);
                this.progress = Integer.bitCount(this.lightsOutMask);
            } else {
                this.puzzleMode = MODE_SEQUENCE;
                this.lightsOutTiles = new long[0];
                this.lightsOutMask = 0;
            }
        } else {
            this.puzzleMode = MODE_SEQUENCE;
            this.lightsOutTiles = new long[0];
            this.lightsOutMask = 0;
        }
        this.pendingSequenceRune = null;
        if (this.puzzleMode == MODE_SEQUENCE && tag.contains("PendingSequenceRune")) {
            BlockPos savedRune = BlockPos.of(tag.getLong("PendingSequenceRune"));
            if (tag.contains("PendingSequenceRune", Tag.TAG_LONG) && validSavedSequence
                    && this.progress > 0 && this.progress == tag.getInt("Progress")
                    && isWithinInputRange(savedRune)) {
                this.pendingSequenceRune = savedRune;
            } else {
                this.progress = 0;
            }
        }
        this.runesResetPending = this.puzzleMode == MODE_SEQUENCE
                && !this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE)
                && tag.getBoolean("RunesResetPending");
        if (this.runesResetPending) {
            this.progress = 0;
            this.pendingSequenceRune = null;
        }
    }

    private boolean isValidLightsOutTiles(List<BlockPos> candidate) {
        if (candidate == null || candidate.size() != LIGHTS_OUT_SIZE) {
            return false;
        }
        Set<BlockPos> unique = new HashSet<>();
        for (BlockPos pos : candidate) {
            long dx = (long) pos.getX() - this.worldPosition.getX();
            long dy = (long) pos.getY() - this.worldPosition.getY();
            long dz = (long) pos.getZ() - this.worldPosition.getZ();
            if (Math.abs(dx) > SequenceRuneBlock.CORE_SEARCH_RADIUS
                    || Math.abs(dy) > 4
                    || Math.abs(dz) > SequenceRuneBlock.CORE_SEARCH_RADIUS
                    || !unique.add(pos)) {
                return false;
            }
        }
        return true;
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
