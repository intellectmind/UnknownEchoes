package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 潮汐符柱:沉没圣殿核心机关的组成部分。 */
public class TidePillarBlock extends Block {
    public static final MapCodec<TidePillarBlock> CODEC = simpleCodec(TidePillarBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(4, 3, 4, 12, 13, 12),
            Block.box(3, 13, 3, 13, 16, 13));

    public TidePillarBlock(Properties properties) {
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

    /** 个人激活键:绑定维度与坐标,同一圣殿的符柱对每位玩家独立计数。 */
    public static String mechanismKey(Level level, BlockPos pos) {
        return "tide_pillar:" + level.dimension().location() + ":" + pos.asLong();
    }

    /** 符柱链完成判定的搜索范围:从任意符柱出发要能覆盖整座圣殿的其余符柱。 */
    private static final int CLUSTER_RANGE_H = 20;
    private static final int CLUSTER_RANGE_V = 8;
    private static final int REQUIRED_PILLARS = 3;

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        String key = mechanismKey(level, pos);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {

            if (clusterComplete(serverPlayer, level, pos)) {
                if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasRitualRecord(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {

                    cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                            serverPlayer, EchoAbilityType.TIDE_ECHO);
                    return InteractionResult.CONSUME;
                }
                if (cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.reissueIfMissing(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                    return InteractionResult.CONSUME;
                }
            }
            return InteractionResult.CONSUME;
        }
        boolean completesRitual = wouldCompleteCluster(serverPlayer, level, pos);
        boolean recorded = completesRitual
                ? cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                        serverPlayer, EchoAbilityType.TIDE_ECHO, key)
                : EchoAbilityManager.activateMechanism(serverPlayer, key);
        if (!recorded) {
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.3, 0.4, 0.3, 0.05);
        }

        return InteractionResult.CONSUME;
    }

    /** 机关完成判定:本圣殿(以该符柱为中心的簇)所有符柱都被该玩家亲自激活过。 */
    private boolean clusterComplete(ServerPlayer player, Level level, BlockPos center) {
        if (!isClusterRangeLoaded(level, center)) {
            return false;
        }
        int pillarCount = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CLUSTER_RANGE_H, -CLUSTER_RANGE_V, -CLUSTER_RANGE_H),
                center.offset(CLUSTER_RANGE_H, CLUSTER_RANGE_V, CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof TidePillarBlock) {
                pillarCount++;
                if (!EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                    return false;
                }
            }
        }
        return pillarCount >= REQUIRED_PILLARS;
    }

    private boolean wouldCompleteCluster(ServerPlayer player, Level level, BlockPos current) {
        if (!isClusterRangeLoaded(level, current)) {
            return false;
        }
        int pillarCount = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                current.offset(-CLUSTER_RANGE_H, -CLUSTER_RANGE_V, -CLUSTER_RANGE_H),
                current.offset(CLUSTER_RANGE_H, CLUSTER_RANGE_V, CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof TidePillarBlock) {
                pillarCount++;
                if (!pos.equals(current)
                        && !EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                    return false;
                }
            }
        }
        return pillarCount >= REQUIRED_PILLARS;
    }

    private static boolean isClusterRangeLoaded(Level level, BlockPos center) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, center, CLUSTER_RANGE_H);
    }
}
