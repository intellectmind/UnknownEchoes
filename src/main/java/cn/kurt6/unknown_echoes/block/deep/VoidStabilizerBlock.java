package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.fx.ProgressionFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.AshScribe;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BooleanSupplier;

/** 虚空脉室稳定节点。三个同场节点均按玩家和坐标独立记录。 */
public class VoidStabilizerBlock extends Block {
    public static final MapCodec<VoidStabilizerBlock> CODEC = simpleCodec(VoidStabilizerBlock::new);

    private static final String KEY_PREFIX = "deep_void_stabilizer:";
    private static final int REQUIRED_STABILIZERS = 3;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14),
            Block.box(5, 4, 5, 11, 15, 11));

    public VoidStabilizerBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.wrong_dimension");
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.VOID_ECHO, "locked");
            return InteractionResult.CONSUME;
        }
        // 灰烬抄录员的净化机关复用稳定器，但不要求已拥有重力回响。
        for (AshScribe boss : serverLevel.getEntitiesOfClass(AshScribe.class,
                new AABB(pos).inflate(36.0D), entity -> entity.isAlive()
                        && entity.isAwaitingExtinguishingMechanism())) {
            if (!isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
                return InteractionResult.CONSUME;
            }
            if (!boss.isAlive() || boss.isRemoved() || boss.level() != serverLevel
                    || !boss.isAwaitingExtinguishingMechanism()) {
                continue;
            }
            if (!EchoPermission.canUseEchoMechanism(serverPlayer, EchoAbilityType.VOID_ECHO)) {
                reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.need_void");
                return InteractionResult.CONSUME;
            }
            boss.onExtinguishingMechanismActivated(serverLevel, pos);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.ash_scribe.mechanism"), true);
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canUseGravityMechanism(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.need_gravity");
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.VOID_ECHO,
                    "space_anchor_locked");
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.hasVoidRouteReady(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.need_anchor_array");
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.VOID_ECHO,
                    "void_stabilizer_locked");
            return InteractionResult.CONSUME;
        }

        BooleanSupplier current = () -> isCurrentInteraction(serverLevel, pos, state, serverPlayer)
                && EchoPermission.canUseGravityMechanism(serverPlayer)
                && EchoPermission.hasVoidRouteReady(serverPlayer);
        BlockPos corePos = VoidCoreAltarBlock.findCoreNear(level, pos);
        if (corePos == null) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.locked");
            return InteractionResult.CONSUME;
        }

        if (!current.getAsBoolean()) {
            return InteractionResult.CONSUME;
        }
        String key = mechanismKey(level, pos);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
            notifyActiveBosses(serverLevel, pos, current);
            if (!current.getAsBoolean()) {
                return InteractionResult.CONSUME;
            }
            int activated = countActivatedNearby(serverPlayer, level, pos);
            playGuidance(serverLevel, pos, corePos, activated >= REQUIRED_STABILIZERS, current);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_stabilizer.already",
                    activated, REQUIRED_STABILIZERS), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, key)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        if (!current.getAsBoolean()) {
            return InteractionResult.CONSUME;
        }
        int activated = countActivatedNearby(serverPlayer, level, pos);
        boolean completed = activated >= REQUIRED_STABILIZERS;
        if (completed) {
            level.playSound(null, pos, ModSounds.BLOCK_VOID_STABILIZER_COMPLETE.get(),
                    SoundSource.BLOCKS, 1.1F, 1.0F);
        } else {
            level.playSound(null, pos, ModSounds.BLOCK_VOID_STABILIZER_ACTIVATE.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
        }
        playGuidance(serverLevel, pos, corePos, completed, current);
        notifyActiveBosses(serverLevel, pos, current);
        serverPlayer.displayClientMessage(completed
                        ? Component.translatable("message.unknown_echoes.void_stabilizer.completed")
                        : Component.translatable("message.unknown_echoes.void_stabilizer.activated",
                                activated, REQUIRED_STABILIZERS),
                true);
        return InteractionResult.CONSUME;
    }

    private static boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                                ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D)
                && state.getBlock() instanceof VoidStabilizerBlock
                && DeepArrayBlockAccess.getIfReady(level, pos) == state;
    }

    private static void notifyActiveBosses(ServerLevel level, BlockPos stabilizerPos, BooleanSupplier current) {
        if (!current.getAsBoolean()) {
            return;
        }
        AABB bounds = new AABB(stabilizerPos).inflate(36.0D);
        for (RiftWarden boss : level.getEntitiesOfClass(RiftWarden.class, bounds,
                entity -> entity.isAlive())) {
            if (!current.getAsBoolean()) {
                return;
            }
            if (boss.isAlive() && !boss.isRemoved() && boss.level() == level) {
                boss.onStabilizerActivated(level, stabilizerPos);
            }
        }
        if (!current.getAsBoolean()) {
            return;
        }
        for (AshScribe boss : level.getEntitiesOfClass(AshScribe.class, bounds,
                entity -> entity.isAlive())) {
            if (!current.getAsBoolean()) {
                return;
            }
            if (boss.isAlive() && !boss.isRemoved() && boss.level() == level) {
                boss.onExtinguishingMechanismActivated(level, stabilizerPos);
            }
        }
    }

    private static void playGuidance(ServerLevel level, BlockPos pos, BlockPos corePos,
                                     boolean completed, BooleanSupplier current) {
        if (!current.getAsBoolean()) {
            return;
        }
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0, .65, 0);
        Vec3 origin = Vec3.atCenterOf(pos).add(0, .55, 0);
        MechanismFx.emit(level, ProgressionFx.guidance(origin, altarCenter, ProgressionFx.Theme.VOID));
        if (!current.getAsBoolean() || !completed) {
            return;
        }
        var stabilizers = VoidCoreAltarBlock.findStabilizerPositions(level, corePos);
        if (stabilizers.size() != REQUIRED_STABILIZERS) {
            return;
        }
        for (BlockPos stabilizer : stabilizers) {
            if (stabilizer.equals(pos)) continue;
            if (!current.getAsBoolean()) {
                return;
            }
            BlockState stabilizerState = DeepArrayBlockAccess.getIfReady(level, stabilizer);
            if (stabilizerState == null || !(stabilizerState.getBlock() instanceof VoidStabilizerBlock)) {
                return;
            }
            Vec3 source = Vec3.atCenterOf(stabilizer).add(0, .55, 0);
            MechanismFx.emit(level, ProgressionFx.guidance(source, altarCenter, ProgressionFx.Theme.VOID));
        }
        if (current.getAsBoolean()
                && VoidCoreAltarBlock.findStabilizerPositions(level, corePos).size() == REQUIRED_STABILIZERS) {
            MechanismFx.emit(level, ProgressionFx.convergence(altarCenter, ProgressionFx.Theme.VOID));
        }
    }

    public static boolean isActivated(ServerPlayer player, Level level, BlockPos pos) {
        return EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos));
    }

    public static String mechanismKey(Level level, BlockPos pos) {
        return KEY_PREFIX + level.dimension().location() + ":"
                + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static int countActivatedNearby(ServerPlayer player, Level level, BlockPos center) {
        return VoidCoreAltarBlock.countActivatedStabilizersNear(player, level, center);
    }

    private static void reject(ServerPlayer player, Level level, BlockPos pos, String messageKey) {
        PlayerSoundFeedback.deny(player);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
