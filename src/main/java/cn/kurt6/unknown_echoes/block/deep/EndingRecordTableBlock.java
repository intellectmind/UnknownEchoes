package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
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

        ItemStack reward = new ItemStack(ModItems.LAST_RECORD_FRAGMENT.get(), 8);
        if (!canFitEntireStack(serverPlayer, reward)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ending_record.need_space"), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        insertEntireStack(serverPlayer, reward);
        EchoAbilityManager.syncToClient(serverPlayer);
        ModAdvancements.award(serverPlayer, ModAdvancements.WORLD_ECHO_COMPLETE);
        getOrCreateTable(serverLevel, pos, state).beginRecording(serverLevel);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                120, 1.2, 1.0, 1.2, 0.12);
        serverLevel.sendParticles(ParticleTypes.GLOW,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                80, 0.8, 0.8, 0.8, 0.06);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.5F, 0.9F);
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

    private static boolean canFitEntireStack(ServerPlayer player, ItemStack reward) {
        int remaining = reward.getCount();
        int max = reward.getMaxStackSize();
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) {
                remaining -= max;
            } else if (ItemStack.isSameItemSameComponents(slot, reward)) {
                remaining -= Math.max(0, max - slot.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static void insertEntireStack(ServerPlayer player, ItemStack reward) {
        int remaining = reward.getCount();
        for (ItemStack slot : player.getInventory().items) {
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, reward)) {
                int moved = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                slot.grow(moved);
                remaining -= moved;
                if (remaining <= 0) {
                    player.getInventory().setChanged();
                    return;
                }
            }
        }
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            if (player.getInventory().items.get(index).isEmpty()) {
                ItemStack inserted = reward.copy();
                int moved = Math.min(remaining, inserted.getMaxStackSize());
                inserted.setCount(moved);
                player.getInventory().items.set(index, inserted);
                remaining -= moved;
                if (remaining <= 0) {
                    player.getInventory().setChanged();
                    return;
                }
            }
        }
    }
}
