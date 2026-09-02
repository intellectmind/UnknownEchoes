package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
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

import java.util.ArrayList;
import java.util.List;

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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TIDE_ECHO)) {
            ArchiveTrialProgress.recordTide(serverPlayer, level, pos);
            MainBossRechallengeData.trySummon(serverPlayer, pos,
                    MainBossRechallengeData.BossKind.ABYSS_WATCHER);
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.ABYSS_WATCHER_ID);
        if (!bossDefeated) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_altar.locked"), true);
            return InteractionResult.CONSUME;
        }
        if (!isPillarRangeLoaded(level, pos)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.area_loading"), true);
            return InteractionResult.CONSUME;
        }
        List<BlockPos> pillars = findNearbyPillars(level, pos);
        boolean mechanismDone = pillars.isEmpty() || allPillarsAwakened(serverPlayer, level, pillars);
        if (mechanismDone) {

            if (!pillars.isEmpty()) {
                if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasValidToken(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                    return InteractionResult.CONSUME;
                }
                cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.consumeToken(
                        serverPlayer, EchoAbilityType.TIDE_ECHO);
                cn.kurt6.unknown_echoes.block.RewardAltarBlock.playAbsorbCeremony(level, pos);
            }
            EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.TIDE_ECHO);
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

    private List<BlockPos> findNearbyPillars(Level level, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos from = center.offset(-PILLAR_RANGE_H, -PILLAR_RANGE_V, -PILLAR_RANGE_H);
        BlockPos to = center.offset(PILLAR_RANGE_H, PILLAR_RANGE_V, PILLAR_RANGE_H);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.getBlockState(pos).getBlock() instanceof TidePillarBlock) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private static boolean isPillarRangeLoaded(Level level, BlockPos center) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, PILLAR_RANGE_H);
    }
}
