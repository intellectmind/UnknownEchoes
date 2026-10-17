package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 音序核心:观察记忆型顺序谜题。右键播放钟音演示,五根音柱按服务端存储的乱序旋律
 * 逐个亮起并发出不同音高;玩家凭记忆按同样顺序敲击,判定与解封复用谜题核心逻辑。
 */
public class ChimeEchoCoreBlockEntity extends PuzzleCoreBlockEntity {
    /** 演示节拍间隔与单音亮灯时长(tick)。 */
    private static final int BEAT_TICKS = 14;
    private static final int FLASH_TICKS = 9;
    private static final int RUNE_RADIUS = 10;

    /** -1 = 无演示进行中;演示状态不持久化,跨会话重新右键即可。 */
    private int demoIndex = -1;
    private int demoTimer;
    private BlockPos flashPos;
    private int flashTimer;
    private int flashOrder;
    private boolean advancingDemo;
    /** 是否已完整听过一遍演示(NBT 持久化):未听过前音柱不受理敲击,并给出引导。 */
    private boolean demoHeard;

    public ChimeEchoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIME_ECHO_CORE.get(), pos, state);
    }

    /** 右键核心:归零进度、熄灭全部音柱并从头播放一遍旋律演示。 */
    public void startDemo(ServerPlayer player) {
        Level level = this.level;
        if (!isCurrentDemoCore(level) || isDemoPlaying()) {
            return;
        }
        if (isUpdatingInput() || tryFinishPending()) {
            return;
        }
        if (!this.resetRunes(level)) {
            return;
        }
        this.advancingDemo = true;
        try {
            this.demoIndex = 0;
            this.demoTimer = BEAT_TICKS / 2;
            level.playSound(null, this.worldPosition, ModSounds.BLOCK_CHIME_CORE_START.get(),
                    SoundSource.BLOCKS, 1.1F, 1.0F);
            if (isCurrentDemoCore(level) && player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.chime_echo_core.demo"), true);
            }
        } finally {
            this.advancingDemo = false;
        }
    }

    public boolean isDemoPlaying() {
        return this.demoIndex >= 0;
    }

    @Override
    protected boolean isUpdatingInput() {
        return this.advancingDemo || super.isUpdatingInput();
    }

    private boolean isCurrentDemoCore(Level level) {
        return level == this.level && isCurrentInactiveCore(level);
    }

    @Override
    boolean tryFinishPending() {
        if (this.advancingDemo) {
            return true;
        }
        return !isDemoPlaying() && (this.demoHeard || hasPendingRuneReset()) && super.tryFinishPending();
    }

    @Override
    public void tryActivate(int order, BlockPos runePos, ServerPlayer player) {
        if (!isCurrentDemoCore(this.level) || this.advancingDemo) {
            return;
        }
        if (isDemoPlaying()) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.chime_echo_core.listening"), true);
            }
            return;
        }
        if (!this.demoHeard && !hasPendingRuneReset()) {

            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.chime_echo_core.hint"), true);
            }
            if (this.level != null && !this.level.isClientSide) {
                PlayerSoundFeedback.deny(player);
            }
            return;
        }
        super.tryActivate(order, runePos, player);
    }

    private boolean extinguishPendingFlash(Level level) {
        if (this.flashPos == null) {
            return true;
        }
        if (!isCurrentDemoCore(level) || !level.hasChunkAt(this.flashPos)) {
            return false;
        }
        BlockState runeState = level.getBlockState(this.flashPos);
        if (matchesFlashRune(runeState) && runeState.getValue(SequenceRuneBlock.LIT)
                && !level.setBlock(this.flashPos, runeState.setValue(SequenceRuneBlock.LIT, Boolean.FALSE), 3)) {
            return false;
        }
        if (!isCurrentDemoCore(level) || !level.hasChunkAt(this.flashPos)) {
            return false;
        }
        BlockState current = level.getBlockState(this.flashPos);
        if (matchesFlashRune(current) && current.getValue(SequenceRuneBlock.LIT)) {
            return false;
        }
        this.flashPos = null;
        this.flashTimer = 0;
        return true;
    }

    private boolean matchesFlashRune(BlockState state) {
        return state.is(ModBlocks.SEQUENCE_RUNE.get()) && state.getValue(SequenceRuneBlock.ORDER) == this.flashOrder;
    }

    /** 服务端 ticker:推进演示节拍与单音回灭。 */
    public void serverTick(Level level) {
        if (!isCurrentDemoCore(level) || isUpdatingInput()) {
            return;
        }
        this.advancingDemo = true;
        try {
            if (this.flashPos != null) {
                if (this.flashTimer > 0) {
                    this.flashTimer--;
                }
                if (this.flashTimer == 0 && !extinguishPendingFlash(level)) {
                    return;
                }
            }
            if (!isDemoPlaying()) {
                return;
            }
            if (this.demoTimer > 0) {
                this.demoTimer--;
            }
            if (this.demoTimer > 0 || this.flashPos != null) {
                return;
            }
            if (this.demoIndex >= this.sequence.length) {
                this.demoIndex = -1;
                this.demoHeard = true;
                this.setChanged();
                if (isCurrentDemoCore(level)) {
                    level.playSound(null, this.worldPosition, ModSounds.BLOCK_CHIME_CORE_CONFIRM.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return;
            }
            if (!LoadedAreaCheck.horizontalRadiusLoaded(level, this.worldPosition, RUNE_RADIUS)) {
                return;
            }
            if (playNote(level, this.sequence[this.demoIndex])) {
                this.demoIndex++;
                this.demoTimer = BEAT_TICKS;
            }
        } finally {
            this.advancingDemo = false;
        }
    }

    private boolean playNote(Level level, int order) {
        BlockPos runePos = findRuneByOrder(level, order);
        if (runePos != null) {
            BlockState runeState = level.getBlockState(runePos);
            // 点亮通知可能先提交方块再中断，必须提前持有熄灭目标。
            this.flashPos = runePos;
            this.flashOrder = order;
            this.flashTimer = FLASH_TICKS;
            if (!level.setBlock(runePos, runeState.setValue(SequenceRuneBlock.LIT, Boolean.TRUE), 3)) {
                return false;
            }
            if (!isCurrentDemoCore(level) || !level.hasChunkAt(runePos)) {
                return false;
            }
            BlockState current = level.getBlockState(runePos);
            if (!matchesFlashRune(current) || !current.getValue(SequenceRuneBlock.LIT)) {
                return false;
            }
            playNoteSound(level, runePos, order);
            if (!isCurrentDemoCore(level)) {
                return false;
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        runePos.getX() + 0.5D, runePos.getY() + 1.2D, runePos.getZ() + 0.5D,
                        0, order / 8.0D, 0.0D, 0.0D, 1.0D);
            }
        } else {
            playNoteSound(level, this.worldPosition, order);
        }
        return isCurrentDemoCore(level);
    }

    @Override
    protected void playActivationSound(Level level, BlockPos runePos, int order) {
        playNoteSound(level, runePos, order);
    }

    private void playNoteSound(Level level, BlockPos pos, int order) {
        // 示范与敲击都按音柱编号发声，不能用完成进度替代旋律中的音高。
        level.playSound(null, pos, ModSounds.BLOCK_CHIME_CORE_NOTE.get(),
                SoundSource.BLOCKS, 1.5F, 0.55F + 0.14F * Math.clamp(order, 0, 7));
    }

    private BlockPos findRuneByOrder(Level level, int order) {
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-RUNE_RADIUS, -4, -RUNE_RADIUS),
                this.worldPosition.offset(RUNE_RADIUS, 2, RUNE_RADIUS))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.SEQUENCE_RUNE.get())
                    && state.getValue(SequenceRuneBlock.ORDER) == order) {
                return pos.immutable();
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (isDemoPlaying() || this.flashPos != null) {
            tag.putBoolean("RunesResetPending", true);
        }
        tag.putBoolean("DemoHeard", this.demoHeard);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.demoHeard = tag.getBoolean("DemoHeard");
    }
}
