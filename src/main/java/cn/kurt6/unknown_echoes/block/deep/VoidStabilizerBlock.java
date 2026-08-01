package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.wrong_dimension");
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.VOID_ECHO, "locked");
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

        BlockPos corePos = VoidCoreAltarBlock.findCoreNear(level, pos);
        if (corePos == null) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_stabilizer.locked");
            return InteractionResult.CONSUME;
        }

        String key = mechanismKey(level, pos);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
            int activated = countActivatedNearby(serverPlayer, level, pos);
            playGuidance(serverLevel, pos, corePos, activated >= REQUIRED_STABILIZERS);
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
        int activated = countActivatedNearby(serverPlayer, level, pos);
        EchoAbilityManager.syncToClient(serverPlayer);
        boolean completed = activated >= REQUIRED_STABILIZERS;
        serverLevel.sendParticles(completed ? ParticleTypes.SCULK_SOUL : ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                completed ? 72 : 32, 0.5, 0.8, 0.5, 0.08);
        level.playSound(null, pos, completed ? SoundEvents.END_PORTAL_SPAWN
                        : SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, completed ? 1.25F : 0.9F, completed ? 1.1F : 0.7F);
        playGuidance(serverLevel, pos, corePos, completed);
        serverPlayer.displayClientMessage(completed
                        ? Component.translatable("message.unknown_echoes.void_stabilizer.completed")
                        : Component.translatable("message.unknown_echoes.void_stabilizer.activated",
                                activated, REQUIRED_STABILIZERS),
                true);
        return InteractionResult.CONSUME;
    }

    private static void playGuidance(ServerLevel level, BlockPos pos, BlockPos corePos,
                                     boolean completed) {
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0.0D, 0.65D, 0.0D);
        Vec3 current = Vec3.atCenterOf(pos).add(0.0D, 0.55D, 0.0D);
        BossFx.guidanceMarker(level, current, ParticleTypes.SCULK_SOUL);
        BossFx.guidanceFlow(level, current, altarCenter,
                ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, level.getGameTime());
        if (!completed) {
            return;
        }
        for (BlockPos stabilizer : VoidCoreAltarBlock.findStabilizerPositions(level, corePos)) {
            Vec3 source = Vec3.atCenterOf(stabilizer).add(0.0D, 0.55D, 0.0D);
            BossFx.guidanceMarker(level, source, ParticleTypes.SCULK_SOUL);
            BossFx.guidanceFlow(level, source, altarCenter,
                    ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, level.getGameTime());
        }
        BossFx.impactBurst(level, altarCenter, ParticleTypes.REVERSE_PORTAL);
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
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, 0.45F);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
