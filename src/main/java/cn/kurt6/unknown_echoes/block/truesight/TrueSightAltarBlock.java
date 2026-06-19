package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 真视核心祭坛:真视回响的发放入口(设计红线 #2,只写玩家数据)。
 * 服务端判定,双条件(设计文档 5.4:击败镜像守护者 + 完成神殿核心机关):
 *  1. 已击败镜像守护者(个人进度);
 *  2. 已亲自激活本神殿全部真镜像符印(个人进度,见 MirrorSigilBlock)。
 * 散件摆放(周围无符印)时只查 Boss 击败。
 */
public class TrueSightAltarBlock extends Block {
    public static final MapCodec<TrueSightAltarBlock> CODEC = simpleCodec(TrueSightAltarBlock::new);

    /** 符印扫描半径:必须从祭坛(北端高台)覆盖整座 23×23 神殿,含南墙符印
     *  (V0.6B 修复:原 12 扫不到南墙,真符印落在南墙时祭坛校验会漏掉它)。 */
    private static final int SIGIL_RANGE_H = 22;
    private static final int SIGIL_RANGE_V = 6;

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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.claimed"), true);
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.MIRROR_GUARDIAN_ID);
        List<BlockPos> realSigils = findRealSigils(level, pos);
        boolean mechanismDone = realSigils.isEmpty() || allSigilsActivated(serverPlayer, level, realSigils);
        if (bossDefeated && mechanismDone) {
            // 共鸣信物仪式(V0.6E,5.1.1):双记录齐全后还需持信物领取(消耗 + 吸收演出);
            // 散件摆放(无符印)保持直接发放,兼容管理员/数据包布景
            if (!realSigils.isEmpty()) {
                if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasValidToken(
                        serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                    return InteractionResult.CONSUME;
                }
                cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.consumeToken(
                        serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO);
                cn.kurt6.unknown_echoes.block.RewardAltarBlock.playAbsorbCeremony(level, pos);
            }
            EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO);
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
        serverPlayer.displayClientMessage(Component.translatable(
                bossDefeated ? "message.unknown_echoes.truesight_altar.need_sigils"
                             : "message.unknown_echoes.truesight_altar.locked"), true);
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

    private List<BlockPos> findRealSigils(Level level, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return result;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SIGIL_RANGE_H, -SIGIL_RANGE_V, -SIGIL_RANGE_H),
                center.offset(SIGIL_RANGE_H, SIGIL_RANGE_V, SIGIL_RANGE_H))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MirrorSigilBlock
                    && MirrorSigilBlockEntity.isReal(serverLevel, pos)) {
                result.add(pos.immutable());
            }
        }
        return result;
    }
}
