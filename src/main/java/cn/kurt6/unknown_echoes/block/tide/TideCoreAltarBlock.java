package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import cn.kurt6.unknown_echoes.world.AltarPillarScan;
import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

/** 潮汐核心祭坛：潮汐回响的发放入口，能力只写入玩家数据。 */
public class TideCoreAltarBlock extends Block {
    public static final MapCodec<TideCoreAltarBlock> CODEC = simpleCodec(TideCoreAltarBlock::new);

    /** 祭坛搜索本圣殿符柱的范围(水平/垂直)。 */
    private static final int PILLAR_RANGE_H = 12;
    private static final int PILLAR_RANGE_V = 6;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public TideCoreAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return interact(state, level, pos, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        interact(state, level, pos, player, hitResult);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                       Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        pos = pos.immutable();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        // 右键事件缓存的状态可能已因换块或传送失效。
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TIDE_ECHO)) {
            ArchiveTrialProgress.recordTide(serverPlayer, level, pos);
            if (RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)
                    && EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                MainBossRechallengeData.trySummon(serverPlayer, pos,
                        MainBossRechallengeData.BossKind.ABYSS_WATCHER);
            }
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.ABYSS_WATCHER_ID);
        if (!bossDefeated) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_altar.locked"), true);
            return InteractionResult.CONSUME;
        }
        Optional<List<BlockPos>> scan = findNearbyPillars(level, pos);
        if (scan.isEmpty()) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.area_loading"), true);
            return InteractionResult.CONSUME;
        }
        List<BlockPos> pillars = scan.get();
        boolean mechanismDone = pillars.isEmpty() || allPillarsAwakened(serverPlayer, level, pillars);
        if (mechanismDone) {

            if (!pillars.isEmpty()) {
                if (!ResonanceTokenManager.consumeToken(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                    return InteractionResult.CONSUME;
                }
            }
            // 先写入能力，再进入吸收音效的外部回调，避免已扣信物却未获得能力。
            if (EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.TIDE_ECHO)
                    && !pillars.isEmpty() && RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
                RewardAltarBlock.playAbsorbCeremony(level, pos);
            }
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
        serverPlayer.displayClientMessage(
                Component.translatable("message.unknown_echoes.tide_altar.need_pillars"), true);
        return InteractionResult.CONSUME;
    }

    /** 机关条件:祭坛周围的所有潮汐符柱都被该玩家亲自激活过。 */
    private boolean allPillarsAwakened(ServerPlayer player, Level level, List<BlockPos> pillars) {
        for (BlockPos pillar : pillars) {
            if (!EchoAbilityManager.hasActivatedMechanism(player, TidePillarBlock.mechanismKey(level, pillar))) {
                return false;
            }
        }
        return true;
    }

    private Optional<List<BlockPos>> findNearbyPillars(Level level, BlockPos center) {
        return AltarPillarScan.find(level, center, PILLAR_RANGE_H, PILLAR_RANGE_V,
                state -> state.getBlock() instanceof TidePillarBlock, false);
    }
}
