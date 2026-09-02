package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
    /** 是否已完整听过一遍演示(NBT 持久化):未听过前音柱不受理敲击,并给出引导。 */
    private boolean demoHeard;

    public ChimeEchoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIME_ECHO_CORE.get(), pos, state);
    }

    /** 右键核心:归零进度、熄灭全部音柱并从头播放一遍旋律演示。 */
    public void startDemo(ServerPlayer player) {
        Level level = this.level;
        if (level == null || level.isClientSide || isDemoPlaying()) {
            return;
        }
        this.progress = 0;
        this.setChanged();
        this.resetRunes(level);
        this.demoIndex = 0;
        this.demoTimer = BEAT_TICKS / 2;
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 1.1F, 0.7F);
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.chime_echo_core.demo"), true);
        }
    }

    public boolean isDemoPlaying() {
        return this.demoIndex >= 0;
    }

    @Override
    public void tryActivate(int order, BlockPos runePos, ServerPlayer player) {
        if (isDemoPlaying()) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.chime_echo_core.listening"), true);
            }
            return;
        }
        if (!this.demoHeard) {

            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.chime_echo_core.hint"), true);
            }
            if (this.level != null) {
                this.level.playSound(null, runePos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.BLOCKS, 0.7F, 0.5F);
            }
            return;
        }
        super.tryActivate(order, runePos, player);
    }

    /** 熄灭当前正在闪亮的演示音柱,并清空待回灭状态。 */
    private void extinguishPendingFlash() {
        Level level = this.level;
        if (this.flashPos == null || level == null) {
            this.flashPos = null;
            this.flashTimer = 0;
            return;
        }
        if (level.hasChunkAt(this.flashPos)) {
            BlockState runeState = level.getBlockState(this.flashPos);
            if (isSequenceInput(runeState) && runeState.getValue(SequenceRuneBlock.LIT)) {
                level.setBlock(this.flashPos,
                        runeState.setValue(SequenceRuneBlock.LIT, Boolean.FALSE), 3);
            }
        }
        this.flashPos = null;
        this.flashTimer = 0;
    }

    /** 服务端 ticker:推进演示节拍与单音回灭。 */
    public void serverTick(Level level) {
        if (this.flashTimer > 0 && --this.flashTimer == 0) {
            this.extinguishPendingFlash();
        }
        if (!isDemoPlaying() || --this.demoTimer > 0) {
            return;
        }
        if (this.demoIndex >= this.sequence.length) {
            this.demoIndex = -1;
            this.demoHeard = true;
            this.setChanged();
            level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, 1.6F);
            return;
        }
        playNote(level, this.sequence[this.demoIndex]);
        this.demoIndex++;
        this.demoTimer = BEAT_TICKS;
    }

    private void playNote(Level level, int order) {
        BlockPos runePos = findRuneByOrder(level, order);
        float pitch = 0.55F + 0.14F * order;
        if (runePos != null) {
            BlockState runeState = level.getBlockState(runePos);
            level.setBlock(runePos, runeState.setValue(SequenceRuneBlock.LIT, Boolean.TRUE), 3);
            this.flashPos = runePos;
            this.flashTimer = FLASH_TICKS;
            level.playSound(null, runePos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.3F, pitch);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        runePos.getX() + 0.5D, runePos.getY() + 1.2D, runePos.getZ() + 0.5D,
                        0, order / 8.0D, 0.0D, 0.0D, 1.0D);
            }
        } else {
            level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, pitch);
        }
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

        this.extinguishPendingFlash();
        tag.putBoolean("DemoHeard", this.demoHeard);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.demoHeard = tag.getBoolean("DemoHeard");
    }
}
