package cn.kurt6.unknown_echoes.block.truesight;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
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
 * 真视宝箱(V0.6E,5.4"打开真视宝箱"权限落地):交互前置真视权限校验的包装容器。
 * 服务端判定——无真视回响时只看到一只"轮廓模糊的箱影",打不开(透视/自由视角 Mod 绕不过,
 * 因为校验的是玩家能力数据);内容为强化材料与研究拓片(true_sight_vault 战利品表)。
 * 藏于隐藏暗室与倒影井;方块本体不可破坏(critical_blocks),箱内物品照常取出。
 */
public class TrueSightChestBlock extends BaseEntityBlock {
    public static final MapCodec<TrueSightChestBlock> CODEC = simpleCodec(TrueSightChestBlock::new);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public TrueSightChestBlock(Properties properties) {
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
        return new TrueSightChestBlockEntity(pos, state);
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
        if (!EchoPermission.canUseEchoMechanism(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ASH,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.02);
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.truesight_chest.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO, "chest_locked");
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof TrueSightChestBlockEntity chest) {
            EchoAbilityManager.activateMechanism(serverPlayer, mechanismKey(level, pos));
            serverPlayer.openMenu(chest);
        }
        return InteractionResult.CONSUME;
    }

    public static String mechanismKey(Level level, BlockPos pos) {
        return "truesight_chest:" + level.dimension().location() + ":" + pos.asLong();
    }
}
