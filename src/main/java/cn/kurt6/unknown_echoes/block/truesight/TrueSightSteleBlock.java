package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

import java.util.IdentityHashMap;
import java.util.Set;

/** 真视碑文，交互逻辑沿用 TideRuneBlock 的权限模式。 */
public class TrueSightSteleBlock extends Block {
    public static final MapCodec<TrueSightSteleBlock> CODEC = simpleCodec(TrueSightSteleBlock::new);

    /** 铭文文本组数(lang 键 rune.unknown_echoes.true_sight.<0..N-1>)。 */
    public static final int INSCRIPTION_COUNT = 4;
    private final Set<ServerPlayer> readingPlayers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public TrueSightSteleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        pos = pos.immutable();
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        readInscription(serverLevel, pos, state, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private void readInscription(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (!this.readingPlayers.add(player)) {
            return;
        }
        try {
            if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.truesight_stele.scrambled"), true);
                if (!canContinueReading(level, pos, state, player, false)) {
                    return;
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.5F);
                if (canContinueReading(level, pos, state, player, false)) {
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.TRUE_SIGHT_ECHO, "stele_scrambled");
                }
                return;
            }

            int index = Math.floorMod(pos.hashCode(), INSCRIPTION_COUNT);
            player.displayClientMessage(Component.translatable("rune.unknown_echoes.true_sight." + index), false);
            if (!canContinueReading(level, pos, state, player, true)) {
                return;
            }
            level.playSound(null, pos, ModSounds.BLOCK_TRUE_SIGHT_STELE_CONFIRM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!canContinueReading(level, pos, state, player, true)) {
                return;
            }
            ArchiveTrialProgress.recordTrueSight(player, level, pos);
            if (!canContinueReading(level, pos, state, player, true)) {
                return;
            }
            String key = "research_revisit:truesight_stele:" + level.dimension().location() + ":" + pos.asLong();
            if (EchoResearchManager.recordRevisit(player, EchoResearchLine.TRUE_SIGHT, key)
                    && canContinueReading(level, pos, state, player, true)) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.research.insight"), true);
            }
        } finally {
            this.readingPlayers.remove(player);
        }
    }

    private boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (player.level() != level || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || !player.canInteractWithBlock(pos, 1.0D) || !state.is(this) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos) == state;
    }

    private boolean canContinueReading(ServerLevel level, BlockPos pos, BlockState state,
                                       ServerPlayer player, boolean allowed) {
        return isCurrentInteraction(level, pos, state, player)
                && EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO) == allowed;
    }
}
