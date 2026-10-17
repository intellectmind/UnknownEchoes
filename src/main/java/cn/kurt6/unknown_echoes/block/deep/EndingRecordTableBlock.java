package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.fx.ProgressionFx;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.journal.RewardCapacity;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** 结局记录台：将个人最终 Boss 结算资格提交为长期通关记录。 */
public class EndingRecordTableBlock extends BaseEntityBlock {
    public static final MapCodec<EndingRecordTableBlock> CODEC = simpleCodec(EndingRecordTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(5, 3, 5, 11, 9, 11),
            Block.box(3, 9, 3, 13, 13, 13));

    public EndingRecordTableBlock(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndingRecordTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ENDING_RECORD_TABLE.get(),
                EndingRecordTableBlockEntity::serverTick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean recording = level.getBlockEntity(pos) instanceof EndingRecordTableBlockEntity table
                && table.isRecording();
        if (!recording && random.nextInt(3) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = recording ? 0.52D + random.nextDouble() * 0.55D : random.nextDouble() * 0.35D;
        level.addParticle(recording ? ParticleTypes.ENCHANT : ParticleTypes.GLOW,
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.85D + random.nextDouble() * (recording ? 1.05D : 0.35D),
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                -Math.cos(angle) * 0.015D, 0.008D, -Math.sin(angle) * 0.015D);
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
        // 原版右键事件会缓存 state；事件回调可能已换块或传送玩家。
        if (serverPlayer.level() != serverLevel || serverLevel.getBlockState(pos) != state) {
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
            PlayerSoundFeedback.deny(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.incomplete"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack reward = new ItemStack(ModItems.LAST_RECORD_FRAGMENT.get(), 8);
        if (!RewardCapacity.canFitAll(serverPlayer, List.of(reward))) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.need_space"), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanisms(serverPlayer, () -> {
            serverPlayer.getInventory().add(reward);
        }, EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        serverPlayer.getInventory().setChanged();
        EchoAbilityManager.syncToClient(serverPlayer);
        ModAdvancements.award(serverPlayer, ModAdvancements.WORLD_ECHO_COMPLETE);
        // 记录与奖励已提交；成就回调改变现场后，不再创建旧台子的实体或播放演出。
        if (serverPlayer.level() == serverLevel && serverLevel.getBlockState(pos) == state) {
            getOrCreateTable(serverLevel, pos, state).beginRecording(serverLevel);
            MechanismFx.emit(serverLevel, ProgressionFx.recordReceipt(Vec3.atBottomCenterOf(pos)));
        }
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.unknown_echoes.ending_record.complete"));
        return InteractionResult.CONSUME;
    }

    private static EndingRecordTableBlockEntity getOrCreateTable(ServerLevel level, BlockPos pos,
                                                                  BlockState state) {
        if (level.getBlockEntity(pos) instanceof EndingRecordTableBlockEntity table) {
            return table;
        }

        EndingRecordTableBlockEntity table = new EndingRecordTableBlockEntity(pos, state);
        level.setBlockEntity(table);
        return table;
    }
}
