package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 真视碑文(V0.6E,5.4"读取隐藏碑文"权限落地):照水下符文 TideRuneBlock 模式。
 * - 无真视回响:右键只看到一团乱纹(服务端判定,客户端透视 Mod 也读不出内容);
 * - 有真视回响:读出铭文(按坐标确定 4 组文本之一),首读 +1 真视研究点(回访点,个人去重)。
 * 放置于镜面神殿与幻象遗迹(回响神殿隐藏房)。
 */
public class TrueSightSteleBlock extends Block {
    public static final MapCodec<TrueSightSteleBlock> CODEC = simpleCodec(TrueSightSteleBlock::new);

    /** 铭文文本组数(lang 键 rune.unknown_echoes.true_sight.<0..N-1>)。 */
    public static final int INSCRIPTION_COUNT = 4;

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
        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.truesight_stele.scrambled"), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.5F);
            if (player instanceof ServerPlayer serverPlayer) {
                EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO,
                        "stele_scrambled");
            }
            return InteractionResult.CONSUME;
        }
        // 按坐标稳定取一组铭文,同一块碑每次读到同样的内容
        int index = Math.floorMod(pos.hashCode(), INSCRIPTION_COUNT);
        player.displayClientMessage(Component.translatable("rune.unknown_echoes.true_sight.title")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        player.displayClientMessage(Component.translatable("rune.unknown_echoes.true_sight." + index), false);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.4F);
        // 回访点研究来源(同 TideRuneBlock):每块碑文首次读取 +1 真视研究点(个人绑定,按坐标去重)
        if (player instanceof ServerPlayer serverPlayer) {
            ArchiveTrialProgress.recordTrueSight(serverPlayer, level, pos);
            String key = "research_revisit:truesight_stele:" + level.dimension().location() + ":" + pos.asLong();
            if (!EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
                EchoAbilityManager.activateMechanism(serverPlayer, key);
                EchoAbilityManager.addResearchPoints(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO, 1);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.research.insight"), true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
