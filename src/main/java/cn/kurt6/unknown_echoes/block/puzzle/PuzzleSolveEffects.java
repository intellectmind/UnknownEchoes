package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 谜题解开时的统一结算:开核心、清封印石、给在场玩家回能。
 * 两类谜题核心都必须调这里,各自复制会让回能额度分叉。
 */
final class PuzzleSolveEffects {
    /** 解谜时清除封印石与判定在场玩家的水平半径。 */
    static final int EFFECT_RADIUS = 10;
    /** 在场回能半径(格)。 */
    private static final double REFILL_RADIUS = 32.0D;

    private PuzzleSolveEffects() {
    }

    /** 点亮核心、清掉周围封印石、给半径内每位非旁观玩家回能。 */
    static void settle(Level level, BlockPos corePos, BlockState litCoreState) {
        level.setBlock(corePos, litCoreState, 3);
        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.5F, 1.2F);
        clearSealedStone(level, corePos);
        refillNearby(level, corePos);
    }

    private static void clearSealedStone(Level level, BlockPos corePos) {
        for (BlockPos pos : BlockPos.betweenClosed(
                corePos.offset(-EFFECT_RADIUS, -4, -EFFECT_RADIUS),
                corePos.offset(EFFECT_RADIUS, 6, EFFECT_RADIUS))) {
            if (!level.hasChunkAt(pos)
                    || !level.getBlockState(pos).is(ModBlocks.SEALED_STONE.get())) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        6, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
    }

    private static void refillNearby(Level level, BlockPos corePos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double radiusSqr = REFILL_RADIUS * REFILL_RADIUS;
        for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                new AABB(corePos).inflate(REFILL_RADIUS),
                player -> !player.isSpectator()
                        && player.distanceToSqr(corePos.getX() + 0.5D,
                        corePos.getY() + 0.5D, corePos.getZ() + 0.5D) <= radiusSqr)) {
            ArtifactManager.refillEnergy(nearby, ServerConfig.ENERGY_REFILL_PUZZLE.get());
        }
    }
}
