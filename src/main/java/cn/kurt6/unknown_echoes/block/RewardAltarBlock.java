package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** 风之核心祭坛：服务端确认 Boss 击败记录和机关完成记录后，吸收绑定信物并解锁能力。 */
public class RewardAltarBlock extends Block {
    public static final MapCodec<RewardAltarBlock> CODEC = simpleCodec(RewardAltarBlock::new);

    /** 搜索本场地记忆柱的范围(格)。祭坛偏在场地一侧,水平半径要够到对角的记忆柱。 */
    private static final int PILLAR_RANGE_H = 20;
    private static final int PILLAR_RANGE_V = 6;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public RewardAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            MainBossRechallengeData.trySummon(serverPlayer, pos,
                    MainBossRechallengeData.BossKind.FORGOTTEN_COLOSSUS);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.FORGOTTEN_COLOSSUS_ID)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.altar.locked"), true);
            return InteractionResult.CONSUME;
        }

        if (!isRangeLoaded(level, pos, PILLAR_RANGE_H)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.area_loading"), true);
            return InteractionResult.CONSUME;
        }

        boolean hasPillars = hasNearbyMemoryPillars(level, pos);
        if (hasPillars && !ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.need_ritual"), true);
            return InteractionResult.CONSUME;
        }

        if (hasPillars) {
            if (!ResonanceTokenManager.hasValidToken(serverPlayer, EchoAbilityType.WIND_ECHO)) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                return InteractionResult.CONSUME;
            }
            ResonanceTokenManager.consumeToken(serverPlayer, EchoAbilityType.WIND_ECHO);
            playAbsorbCeremony(level, pos);
        }

        EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.WIND_ECHO);
        return InteractionResult.CONSUME;
    }

    private boolean hasNearbyMemoryPillars(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-PILLAR_RANGE_H, -PILLAR_RANGE_V, -PILLAR_RANGE_H),
                center.offset(PILLAR_RANGE_H, PILLAR_RANGE_V, PILLAR_RANGE_H))) {
            if (level.getBlockState(pos).getBlock()
                    instanceof cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRangeLoaded(Level level, BlockPos center, int horizontalRange) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, horizontalRange);
    }

    public static void playAbsorbCeremony(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.3F, 0.7F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            double cx = pos.getX() + 0.5;
            double cz = pos.getZ() + 0.5;
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 12;
                double r = 0.9 - i * 0.03;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        cx + Math.cos(angle) * r, pos.getY() + 0.6 + i * 0.09, cz + Math.sin(angle) * r,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    cx, pos.getY() + 1.2, cz, 20, 0.4, 0.5, 0.4, 0.1);
        }
    }
}
