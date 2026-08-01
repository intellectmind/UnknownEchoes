package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

/** 结局记录台：将个人最终 Boss 结算资格提交为长期通关记录。 */
public class EndingRecordTableBlock extends Block {
    public static final MapCodec<EndingRecordTableBlock> CODEC = simpleCodec(EndingRecordTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(5, 3, 5, 11, 9, 11),
            Block.box(3, 9, 3, 13, 13, 13));

    public EndingRecordTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.GLOW,
                pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                pos.getY() + 0.85D + random.nextDouble() * 0.35D,
                pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                0.0D, 0.008D, 0.0D);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.dormant"), true);
            return InteractionResult.CONSUME;
        }
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer,
                EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.complete"), true);
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canClaimWorldEchoEnding(serverPlayer)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.45F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.incomplete"), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        ModAdvancements.award(serverPlayer, ModAdvancements.WORLD_ECHO_COMPLETE);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                120, 1.2, 1.0, 1.2, 0.12);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                80, 0.8, 0.8, 0.8, 0.06);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.5F, 0.9F);
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.unknown_echoes.ending_record.recorded"));
        return InteractionResult.CONSUME;
    }
}
