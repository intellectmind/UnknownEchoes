package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 镜像符印：真假状态仅存于服务端，客户端通过粒子表现提供辨识线索。 */
public class MirrorSigilBlock extends BaseEntityBlock {
    public static final MapCodec<MirrorSigilBlock> CODEC = simpleCodec(MirrorSigilBlock::new);
    public static final int REQUIRED_REAL_SIGILS = 4;

    public MirrorSigilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MirrorSigilBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static String mechanismKey(Level level, BlockPos pos) {
        return "mirror_sigil:" + level.dimension().location() + ":" + pos.asLong();
    }

    /** 真符印的"倒影涟漪":随机 tick 偶发微粒,耐心观察可辨真假(注册时需开 randomTicks)。 */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos,
                              net.minecraft.util.RandomSource random) {
        if (MirrorSigilBlockEntity.isReal(level, pos)) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.5F, 1.6F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }
        if (!MirrorSigilBlockEntity.ensureInitialized(serverLevel, pos)) {
            return InteractionResult.CONSUME;
        }
        if (MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
            String key = mechanismKey(level, pos);
            if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {

                if (clusterComplete(serverPlayer, serverLevel, pos)) {
                    if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasRitualRecord(
                            serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO)) {

                        cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                                serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO);
                        return InteractionResult.CONSUME;
                    }
                    if (cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.reissueIfMissing(
                            serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO)) {
                        return InteractionResult.CONSUME;
                    }
                }
                return InteractionResult.CONSUME;
            }
            boolean completesRitual = wouldCompleteCluster(serverPlayer, serverLevel, pos);
            boolean recorded = completesRitual
                    ? cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                            serverPlayer,
                            cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO, key)
                    : EchoAbilityManager.activateMechanism(serverPlayer, key);
            if (!recorded) {
                return InteractionResult.CONSUME;
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.3F, 1.2F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.03);

            double guardianRange = MirrorSigilBlockEntity.CLUSTER_RANGE_H;
            for (var guardian : serverLevel.getEntitiesOfClass(
                    cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(guardianRange),
                    guardian -> !guardian.isIllusion()
                            && guardian.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D) <= guardianRange * guardianRange)) {
                guardian.onSigilActivated(serverPlayer, pos);
            }

        } else {

            resetPlayerSigils(serverPlayer, serverLevel, pos);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.2F, 1.4F);
            serverLevel.sendParticles(ParticleTypes.ASH,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.05);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.mirror_sigil.false"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** 触假即重置:周围所有真符印的个人激活记录被抹掉,需要重新观察。 */
    private void resetPlayerSigils(ServerPlayer player, ServerLevel level, BlockPos center) {
        var mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        for (BlockPos pos : MirrorSigilBlockEntity.findCluster(level, center)) {
            if (MirrorSigilBlockEntity.isReal(level, pos)) {
                mechanisms.remove(mechanismKey(level, pos));
            }
        }
    }

    /** 机关完成判定：玩家点亮本神殿全部真符印。 */
    private boolean clusterComplete(ServerPlayer player, ServerLevel level, BlockPos center) {
        int realCount = 0;
        for (BlockPos pos : MirrorSigilBlockEntity.findCluster(level, center)) {
            if (!MirrorSigilBlockEntity.isReal(level, pos)) {
                continue;
            }
            realCount++;
            if (!EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                return false;
            }
        }
        return realCount >= REQUIRED_REAL_SIGILS;
    }

    private boolean wouldCompleteCluster(ServerPlayer player, ServerLevel level, BlockPos current) {
        int realCount = 0;
        for (BlockPos pos : MirrorSigilBlockEntity.findCluster(level, current)) {
            if (!MirrorSigilBlockEntity.isReal(level, pos)) {
                continue;
            }
            realCount++;
            if (!pos.equals(current)
                    && !EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                return false;
            }
        }
        return realCount >= REQUIRED_REAL_SIGILS;
    }
}
