package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/**
 * 风之核心祭坛(场地内奖励祭坛)。V0.6E 共鸣信物仪式(5.1.1):
 * 服务端双记录校验——已击败遗忘巨像(个人 Boss 记录)+ 已完成记忆柱机关(个人仪式记录,
 * 破防瞬间对参与者写入)。双记录齐全后,持风之信物右键被吸收(消耗 + 演出)才写入能力;
 * 无信物时含蓄指引回记忆柱处免费复领。修正 V0.5 偏差:旧版只查 Boss 记录,
 * 服务器调高未破防伤害硬杀巨像即可跳过机关——现在机关记录是硬条件。
 * 散件摆放(周围无记忆柱)时只查 Boss 击败,保证管理员/数据包布景可用。
 */
public class RewardAltarBlock extends Block {
    public static final MapCodec<RewardAltarBlock> CODEC = simpleCodec(RewardAltarBlock::new);

    /** 祭坛搜索本场地记忆柱的范围(竞技场 29×29,祭坛在南侧,对角记忆柱距离约 19)。 */
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.altar.claimed"), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.FORGOTTEN_COLOSSUS_ID)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.altar.locked"), true);
            return InteractionResult.CONSUME;
        }

        // 机关条件:场地内存在记忆柱时必须有个人仪式记录(散件摆放只查 Boss,兼容布景)
        boolean hasPillars = hasNearbyMemoryPillars(level, pos);
        if (hasPillars && !ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.need_ritual"), true);
            return InteractionResult.CONSUME;
        }

        // 共鸣信物仪式:双记录齐全后还需把信物交给祭坛(消耗 + 演出);无信物指引回机关复领
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

    /** 信物吸收演出:螺旋上升的回响光点 + 共鸣音(所有祭坛共用,5.1.1 仪式必经感)。 */
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
