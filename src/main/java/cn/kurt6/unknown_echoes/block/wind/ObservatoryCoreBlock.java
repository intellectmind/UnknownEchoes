package cn.kurt6.unknown_echoes.block.wind;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * 天空观测站核心:漂浮群岛的记述派检索装置。
 * 服务端校验风之回响(design-principles #1:其他 Mod 飞上空岛也无法激活);
 * 通过后给出指向湖底深处的弱线索(深渊信标方向,V0.6+ 内容的前置铺垫)并记录日志。
 */
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
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.5F);
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.observatory.locked"), true);
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.03);
        }
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.observatory.hint.title")
                        .withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.observatory.hint.text")
                        .withStyle(ChatFormatting.GRAY), false);
        if (player instanceof ServerPlayer serverPlayer) {
            JournalManager.recordStructure(serverPlayer, UnknownEchoes.id("sky_observatory"), pos);
            // V0.6D 检索残响:为玩家定位一处尚未踏入的回访遗迹,写入个人日志线索
            //(风暴罗盘获得链的"线索"环节;一次性有界定位,罗盘本身不扫描)
            if (cn.kurt6.unknown_echoes.journal.ClueLocator.addObservatoryClue(
                    serverPlayer, serverPlayer.getRandom())) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.observatory.clue_found"));
            }
        }
        return InteractionResult.CONSUME;
    }
}
