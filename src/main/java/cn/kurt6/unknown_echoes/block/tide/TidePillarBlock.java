package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

/**
 * 潮汐符柱:沉没圣殿核心机关的组成部分。
 * 激活进度按玩家个人记录(设计红线 #3),所以方块没有"已激活"外观状态——
 * 每位玩家都要亲自唤醒圣殿里的全部符柱,潮汐核心祭坛才会回应他。
 * 激活键 = "tide_pillar:<维度>:<坐标 long>",写入 EchoAbilityData.mechanisms。
 */
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
            // 共鸣信物(V0.6E):符柱链已完成时,机关核心处补记录/免费复领(5.1.1 不卡进度)
            if (clusterComplete(serverPlayer, level, pos)) {
                if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasRitualRecord(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                    // 旧档兼容:V0.6E 前已完成符柱链的玩家,补写仪式记录并发信物
                    cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                            serverPlayer, EchoAbilityType.TIDE_ECHO);
                    return InteractionResult.CONSUME;
                }
                if (cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.reissueIfMissing(
                        serverPlayer, EchoAbilityType.TIDE_ECHO)) {
                    return InteractionResult.CONSUME;
                }
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_pillar.already"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.activateMechanism(serverPlayer, key);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.3, 0.4, 0.3, 0.05);
        }
        serverPlayer.displayClientMessage(
                Component.translatable("message.unknown_echoes.tide_pillar.activated"), true);
        // 共鸣信物(V0.6E,5.1.1):最后一根符柱唤醒 = 机关完成,写仪式记录并发放潮汐信物
        if (clusterComplete(serverPlayer, level, pos)) {
            cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                    serverPlayer, EchoAbilityType.TIDE_ECHO);
        }
        return InteractionResult.CONSUME;
    }

    /** 机关完成判定:本圣殿(以该符柱为中心的簇)所有符柱都被该玩家亲自激活过。 */
    private boolean clusterComplete(ServerPlayer player, Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CLUSTER_RANGE_H, -CLUSTER_RANGE_V, -CLUSTER_RANGE_H),
                center.offset(CLUSTER_RANGE_H, CLUSTER_RANGE_V, CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof TidePillarBlock
                    && !EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                return false;
            }
        }
        return true;
    }
}
