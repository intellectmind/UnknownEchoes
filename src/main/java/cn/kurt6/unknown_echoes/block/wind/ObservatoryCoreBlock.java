package cn.kurt6.unknown_echoes.block.wind;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 天空观测站核心:漂浮群岛的记述派检索装置。 */
public class ObservatoryCoreBlock extends Block {
    public static final MapCodec<ObservatoryCoreBlock> CODEC = simpleCodec(ObservatoryCoreBlock::new);

    public ObservatoryCoreBlock(Properties properties) {
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
        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.WIND_ECHO)) {
            level.playSound(null, pos, ModSounds.BLOCK_OBSERVATORY_LOCKED.get(), SoundSource.BLOCKS, 0.55F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.observatory.locked"), true);
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, ModSounds.BLOCK_OBSERVATORY_READ.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            MechanismFx.emit(serverLevel, ExplorationFx.observatory(Vec3.atBottomCenterOf(pos)));
        }
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.observatory.hint.text")
                        .withStyle(ChatFormatting.GRAY), false);
        if (player instanceof ServerPlayer serverPlayer) {
            JournalManager.recordStructure(serverPlayer, UnknownEchoes.id("sky_observatory"), pos);

            if (cn.kurt6.unknown_echoes.journal.ClueLocator.addObservatoryClue(
                    serverPlayer, serverPlayer.getRandom())) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.observatory.clue_found"));
            }
        }
        return InteractionResult.CONSUME;
    }
}
