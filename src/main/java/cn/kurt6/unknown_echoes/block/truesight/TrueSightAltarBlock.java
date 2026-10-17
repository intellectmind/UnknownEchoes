package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
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
import java.util.Optional;

/** 真视核心祭坛：真视回响的发放入口，只写入玩家数据。 */
public class TrueSightAltarBlock extends Block {
    public static final MapCodec<TrueSightAltarBlock> CODEC = simpleCodec(TrueSightAltarBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public TrueSightAltarBlock(Properties properties) {
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

        // 右键事件缓存的状态可能已因换块或传送失效。
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            MainBossRechallengeData.trySummon(serverPlayer, pos,
                    MainBossRechallengeData.BossKind.MIRROR_GUARDIAN);
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.MIRROR_GUARDIAN_ID);
        if (!bossDefeated) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.truesight_altar.locked"), true);
            return InteractionResult.CONSUME;
        }
        if (!isSigilRangeLoaded(level, pos)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.area_loading"), true);
            return InteractionResult.CONSUME;
        }
        Optional<List<BlockPos>> realSigilResult = findRealSigils(level, pos);
        if (realSigilResult.isEmpty()) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.truesight_altar.need_sigils"), true);
            return InteractionResult.CONSUME;
        }
        List<BlockPos> realSigils = realSigilResult.get();
        boolean mechanismDone = realSigils.isEmpty()
                || realSigils.size() >= MirrorSigilBlock.REQUIRED_REAL_SIGILS
                && allSigilsActivated(serverPlayer, level, realSigils);
        if (mechanismDone) {

            if (!realSigils.isEmpty()) {
                if (!ResonanceTokenManager.consumeToken(
                        serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                    return InteractionResult.CONSUME;
                }
            }
            // 先写入能力，再进入吸收音效的外部回调，避免已扣信物却未获得能力。
            if (EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)
                    && !realSigils.isEmpty() && RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
                RewardAltarBlock.playAbsorbCeremony(level, pos);
            }
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
        serverPlayer.displayClientMessage(
                Component.translatable("message.unknown_echoes.truesight_altar.need_sigils"), true);
        return InteractionResult.CONSUME;
    }

    /** 机关条件:神殿内所有真符印都被该玩家亲自激活过;假符印不计数。 */
    private boolean allSigilsActivated(ServerPlayer player, Level level, List<BlockPos> realSigils) {
        for (BlockPos sigil : realSigils) {
            if (!EchoAbilityManager.hasActivatedMechanism(player, MirrorSigilBlock.mechanismKey(level, sigil))) {
                return false;
            }
        }
        return true;
    }

    private Optional<List<BlockPos>> findRealSigils(Level level, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return Optional.empty();
        }
        if (!MirrorSigilBlockEntity.isClusterLoaded(serverLevel, center)) {
            return Optional.empty();
        }
        for (BlockPos pos : MirrorSigilBlockEntity.findCluster(serverLevel, center)) {
            if (!MirrorSigilBlockEntity.ensureInitialized(serverLevel, pos)) {
                return Optional.empty();
            }
            if (MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
                result.add(pos.immutable());
            }
        }
        return Optional.of(result);
    }

    private static boolean isSigilRangeLoaded(Level level, BlockPos center) {
        return level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && MirrorSigilBlockEntity.isClusterLoaded(serverLevel, center);
    }
}
