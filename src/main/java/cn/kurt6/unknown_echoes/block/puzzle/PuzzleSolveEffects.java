package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 谜题解开时的统一结算:开核心、清封印石、给在场玩家回能。
 * 两类谜题核心都必须调这里,各自复制会让回能额度分叉。
 */
final class PuzzleSolveEffects {
    /** 解谜时清除封印石与判定在场玩家的水平半径。 */
    static final int EFFECT_RADIUS = 10;
    /** 在场回能半径(格)。 */
    private static final double REFILL_RADIUS = 32.0D;
    private static final int MAX_SEAL_SPARKS = 12;
    private record SealClearKey(Level level, BlockPos pos) {}
    // 方块通知与粒子派发可同步回调核心，同一批次不能递归扫描或叠加表现预算。
    private static final Set<SealClearKey> CLEARING_SEALS = new HashSet<>();

    private PuzzleSolveEffects() {
    }

    /** 点亮核心、清掉周围封印石、给半径内每位非旁观玩家回能。 */
    static void settle(Level level, BlockPos corePos, BlockState litCoreState) {
        if (level.isClientSide || !isActiveCore(litCoreState) || !level.hasChunkAt(corePos)) {
            return;
        }
        BlockEntity controller = level.getBlockEntity(corePos);
        BlockState unlitCoreState = litCoreState.setValue(PuzzleCoreBlock.ACTIVE, false);
        if (!isCurrentCore(level, corePos, unlitCoreState, controller)
                || !level.setBlock(corePos, litCoreState, 3)
                || !isCurrentCore(level, corePos, litCoreState, controller)) {
            return;
        }
        level.playSound(null, corePos, ModSounds.BLOCK_PUZZLE_SOLVE.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        clearSealedStone(level, corePos, litCoreState, controller);
        if (!isCurrentCore(level, corePos, litCoreState, controller)) {
            return;
        }
        refillNearby(level, corePos, litCoreState, controller);
        if (isCurrentCore(level, corePos, litCoreState, controller)
                && level instanceof ServerLevel serverLevel) {
            PuzzleJourneyRecorder.completed(serverLevel, corePos);
            if (!isCurrentCore(level, corePos, litCoreState, controller)) {
                return;
            }
            Vec3 center = Vec3.atBottomCenterOf(corePos).add(0.0D, 1.02D, 0.0D);
            CombatFx.emit(serverLevel, Shape.SIGIL, center, Vec3.ZERO,
                    1.05D, 0.0D, 0.0D, 0xBCE5CE, 0x518B85, 18, false);
        }
    }

    static void recoverSeals(Level level, BlockPos corePos, BlockState coreState) {
        if (level.isClientSide || !isActiveCore(coreState) || !level.hasChunkAt(corePos)) {
            return;
        }
        // 已激活的旧存档只能补解封，不能据此重放回能或完成奖励。
        clearSealedStone(level, corePos, coreState, level.getBlockEntity(corePos));
    }

    private static boolean isActiveCore(BlockState state) {
        return (state.is(ModBlocks.PUZZLE_CORE.get())
                || state.is(ModBlocks.CHIME_ECHO_CORE.get())
                || state.is(ModBlocks.FOLDPAGE_PAIR_CORE.get()))
                && state.getValue(PuzzleCoreBlock.ACTIVE);
    }

    private static boolean isCurrentCore(Level level, BlockPos corePos,
                                         BlockState coreState, BlockEntity controller) {
        return (controller instanceof PuzzleCoreBlockEntity || controller instanceof FoldpagePairCoreBlockEntity)
                && !controller.isRemoved() && controller.getLevel() == level
                && level.hasChunkAt(corePos)
                && level.getBlockEntity(corePos) == controller
                && level.getBlockState(corePos) == coreState;
    }

    private static void clearSealedStone(Level level, BlockPos corePos,
                                          BlockState coreState, BlockEntity controller) {
        SealClearKey key = new SealClearKey(level, corePos.immutable());
        if (!CLEARING_SEALS.add(key)) {
            return;
        }
        try {
            clearSealedStoneBatch(level, corePos, coreState, controller);
        } finally {
            CLEARING_SEALS.remove(key);
        }
    }

    private static void clearSealedStoneBatch(Level level, BlockPos corePos,
                                               BlockState coreState, BlockEntity controller) {
        List<BlockPos> sparks = new ArrayList<>(MAX_SEAL_SPARKS);
        RandomSource visualRandom = RandomSource.create(corePos.asLong() ^ level.getGameTime());
        int cleared = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                corePos.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                corePos.offset(EFFECT_RADIUS, 6, EFFECT_RADIUS))) {
            if (!isCurrentCore(level, corePos, coreState, controller)) {
                return;
            }
            if (!level.hasChunkAt(pos)
                    || !level.getBlockState(pos).is(ModBlocks.SEALED_STONE.get())) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()) {
                continue;
            }
            // 蓄水池抽样覆盖整片封印，表现预算不截断清除流程。
            int sample = visualRandom.nextInt(++cleared);
            if (sparks.size() < MAX_SEAL_SPARKS) {
                sparks.add(pos.immutable());
            } else if (sample < MAX_SEAL_SPARKS) {
                sparks.set(sample, pos.immutable());
            }
        }
        if (isCurrentCore(level, corePos, coreState, controller)
                && level instanceof ServerLevel serverLevel) {
            for (BlockPos pos : sparks) {
                if (!isCurrentCore(level, corePos, coreState, controller)) {
                    return;
                }
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        2, 0.18D, 0.18D, 0.18D, 0.015D);
            }
        }
    }

    private static void refillNearby(Level level, BlockPos corePos,
                                     BlockState coreState, BlockEntity controller) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                new AABB(corePos).inflate(REFILL_RADIUS),
                player -> canRefill(serverLevel, player, corePos))) {
            if (!isCurrentCore(level, corePos, coreState, controller)) {
                return;
            }
            if (canRefill(serverLevel, nearby, corePos)) {
                ArtifactManager.refillEnergy(nearby, ServerConfig.ENERGY_REFILL_PUZZLE.get());
            }
        }
    }

    private static boolean canRefill(ServerLevel level, ServerPlayer player, BlockPos corePos) {
        return player.level() == level && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && player.distanceToSqr(corePos.getX() + 0.5D, corePos.getY() + 0.5D,
                corePos.getZ() + 0.5D) <= REFILL_RADIUS * REFILL_RADIUS;
    }
}
