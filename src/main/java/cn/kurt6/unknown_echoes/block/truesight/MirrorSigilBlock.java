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

/**
 * 镜像符印:镜湖神殿核心机关(V0.5 镜像谜题)。真假符印外观与亮度完全一致,
 * 真假标记只存服务端 BlockEntity(V0.6A 自方块状态迁移,红线 #9:方块状态会同步
 * 客户端,抓包/调试可读出真假;BlockEntity 不实现同步即客户端不可见)。辨识手段:
 * - 真符印每隔几秒泛起一次"倒影涟漪"粒子(服务端发,所有人可见,需要观察);
 * - 远古残页与水下符文铭文给出文字线索。
 * 右键真符印 → 按玩家个人记录激活(同潮汐符柱,设计红线 #3);
 * 右键假符印 → 幻象碎裂提示并清空该玩家在本神殿的全部符印进度(轻量惩罚,机关链感)。
 */
public class MirrorSigilBlock extends BaseEntityBlock {
    public static final MapCodec<MirrorSigilBlock> CODEC = simpleCodec(MirrorSigilBlock::new);

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
        if (MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
            String key = mechanismKey(level, pos);
            if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
                // 共鸣信物(V0.6E):符印阵已点亮时,机关核心处补记录/免费复领(5.1.1 不卡进度)
                if (clusterComplete(serverPlayer, serverLevel, pos)) {
                    if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasRitualRecord(
                            serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO)) {
                        // 旧档兼容:V0.6E 前已点亮全部真符印的玩家,补写仪式记录并发信物
                        cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                                serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO);
                        return InteractionResult.CONSUME;
                    }
                    if (cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.reissueIfMissing(
                            serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO)) {
                        return InteractionResult.CONSUME;
                    }
                }
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.mirror_sigil.already"), true);
                return InteractionResult.CONSUME;
            }
            EchoAbilityManager.activateMechanism(serverPlayer, key);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.3F, 1.2F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.03);
            // 通知附近的镜像守护者重新检查破防条件(真实辨认 → 真视破防)
            for (var guardian : serverLevel.getEntitiesOfClass(
                    cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(24.0))) {
                if (!guardian.isIllusion()) {
                    guardian.onSigilActivated(serverPlayer);
                }
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.mirror_sigil.activated"), true);
            // 共鸣信物(V0.6E,5.1.1):最后一枚真符印点亮 = 机关完成,写仪式记录并发放真视信物
            if (clusterComplete(serverPlayer, serverLevel, pos)) {
                cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(
                        serverPlayer, cn.kurt6.unknown_echoes.ability.EchoAbilityType.TRUE_SIGHT_ECHO);
            }
        } else {
            // 幻象碎裂:清空该玩家在本神殿的符印进度,但不造成伤害(观察谜题,不是陷阱房)
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
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-MirrorSigilBlockEntity.CLUSTER_RANGE_H,
                        -MirrorSigilBlockEntity.CLUSTER_RANGE_V,
                        -MirrorSigilBlockEntity.CLUSTER_RANGE_H),
                center.offset(MirrorSigilBlockEntity.CLUSTER_RANGE_H,
                        MirrorSigilBlockEntity.CLUSTER_RANGE_V,
                        MirrorSigilBlockEntity.CLUSTER_RANGE_H))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MirrorSigilBlock
                    && MirrorSigilBlockEntity.isReal(level, pos)) {
                mechanisms.remove(mechanismKey(level, pos));
            }
        }
    }

    /** 机关完成判定(V0.6E 信物仪式):本神殿全部真符印都被该玩家点亮。 */
    private boolean clusterComplete(ServerPlayer player, ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-MirrorSigilBlockEntity.CLUSTER_RANGE_H,
                        -MirrorSigilBlockEntity.CLUSTER_RANGE_V,
                        -MirrorSigilBlockEntity.CLUSTER_RANGE_H),
                center.offset(MirrorSigilBlockEntity.CLUSTER_RANGE_H,
                        MirrorSigilBlockEntity.CLUSTER_RANGE_V,
                        MirrorSigilBlockEntity.CLUSTER_RANGE_H))) {
            if (level.getBlockState(pos).getBlock() instanceof MirrorSigilBlock
                    && MirrorSigilBlockEntity.isReal(level, pos)
                    && !EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos))) {
                return false;
            }
        }
        return true;
    }
}
