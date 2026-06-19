package cn.kurt6.unknown_echoes.block.archive;

import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 回声大档案馆终端:读取第一维度完成度并结算 T7 个人奖励。
 * 交互只读玩家服务端进度,奖励写入个人数据;不依赖掉落物或世界唯一状态。
 */
public class EchoArchiveTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<EchoArchiveTerminalBlock> CODEC = simpleCodec(EchoArchiveTerminalBlock::new);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);

    public EchoArchiveTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoArchiveTerminalBlockEntity(pos, state);
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
        if (level.getBlockEntity(pos) instanceof EchoArchiveTerminalBlockEntity terminal) {
            terminal.recordAccess();
        }
        serverPlayer.sendSystemMessage(Component.translatable("message.unknown_echoes.archive_terminal.awakened"));
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8F, 0.9F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 24, 0.35, 0.45, 0.35, 0.03);
        }
        ModAdvancements.award(serverPlayer, ModAdvancements.GRAND_ARCHIVE);
        EchoCompletionManager.handleArchiveTerminalUse(serverPlayer);
        return InteractionResult.CONSUME;
    }
}
