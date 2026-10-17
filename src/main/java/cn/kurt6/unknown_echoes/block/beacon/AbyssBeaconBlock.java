package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** 第一维度毕业入口。每座信标的三段校准按玩家和坐标独立保存。 */
public class AbyssBeaconBlock extends Block {
    public static final MapCodec<AbyssBeaconBlock> CODEC = simpleCodec(AbyssBeaconBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 8, 12),
            Block.box(6, 8, 6, 10, 16, 10));
    private static final String CALIBRATION_PREFIX = "abyss_beacon:";
    private static final int STAGE_COOLDOWN_TICKS = 24;
    private static final int ACTIVATION_COOLDOWN_TICKS = 62;
    private static final Set<ServerPlayer> INTERACTING = Collections.newSetFromMap(new WeakHashMap<>());

    public AbyssBeaconBlock(Properties properties) {
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
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }
        BlockPos source = pos.immutable();
        if (!isInteractionCurrent(serverPlayer, serverLevel, source) || !INTERACTING.add(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        try {
            return useBeacon(serverLevel, source, serverPlayer);
        } finally {
            INTERACTING.remove(serverPlayer);
        }
    }

    private static InteractionResult useBeacon(ServerLevel level, BlockPos pos, ServerPlayer serverPlayer) {
        if (level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            DeepEchoTeleporter.returnToEchoRealm(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(EchoBeaconBlock.ECHO_REALM)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.wrong_realm"), true);
            return InteractionResult.CONSUME;
        }

        EchoCompletionManager.refreshMainlineCompletion(serverPlayer);
        if (!isInteractionCurrent(serverPlayer, level, pos)) {
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canCalibrateAbyssBeacon(serverPlayer)) {
            fail(level, pos, serverPlayer, "message.unknown_echoes.abyss_beacon.mainline_required");
            return InteractionResult.CONSUME;
        }

        String baseKey = calibrationKey(level, pos);
        if (serverPlayer.getCooldowns().isOnCooldown(ModItems.ABYSS_BEACON.get())) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.calibration_settling"), true);
            return InteractionResult.CONSUME;
        }
        if (isStageComplete(serverPlayer, baseKey, "complete")) {
            if (EchoPermission.canEnterDeepEchoes(serverPlayer)) {
                DeepEchoTeleporter.enter(serverPlayer, pos);
            }
            return InteractionResult.CONSUME;
        }

        if (!isStageComplete(serverPlayer, baseKey, "wind")) {
            calibrateStage(level, pos, serverPlayer, baseKey, "wind", EchoAbilityType.WIND_ECHO,
                    ParticleTypes.CLOUD, SoundEvents.AMETHYST_BLOCK_RESONATE);
        } else if (!isStageComplete(serverPlayer, baseKey, "tide")) {
            calibrateStage(level, pos, serverPlayer, baseKey, "tide", EchoAbilityType.TIDE_ECHO,
                    ParticleTypes.SPLASH, SoundEvents.CONDUIT_ACTIVATE);
        } else {
            if (!EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                fail(level, pos, serverPlayer, "message.unknown_echoes.abyss_beacon.need_true_sight");
                return InteractionResult.CONSUME;
            }
            if (!EchoAbilityManager.activateBeaconRoute(serverPlayer,
                    EchoPermission.ABYSS_BEACON_ID, EchoPermission.DEEP_ECHOES_ID,
                    baseKey + ":true_sight", baseKey + ":complete")) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.progress_record.full"), true);
                return InteractionResult.CONSUME;
            }
            BooleanSupplier canContinue = () -> isCalibrationCurrent(serverPlayer, level, pos,
                    baseKey, "complete", EchoAbilityType.TRUE_SIGHT_ECHO)
                    && isStageComplete(serverPlayer, baseKey, "true_sight")
                    && EchoPermission.canEnterDeepEchoes(serverPlayer);
            if (!playStageFeedback(level, pos, serverPlayer,
                    EchoAbilityType.TRUE_SIGHT_ECHO, ParticleTypes.ENCHANT,
                    SoundEvents.BEACON_ACTIVATE, ACTIVATION_COOLDOWN_TICKS, canContinue)) {
                return InteractionResult.CONSUME;
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.calibrated_true_sight"), true);
            if (canContinue.getAsBoolean()) {
                AbyssBeaconEffects.playActivation(level, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    private static boolean calibrateStage(ServerLevel level, BlockPos pos, ServerPlayer player, String baseKey,
                                          String stage, EchoAbilityType ability,
                                          net.minecraft.core.particles.ParticleOptions particle,
                                          net.minecraft.sounds.SoundEvent sound) {
        if (!EchoAbilityManager.hasAbility(player, ability)) {
            fail(level, pos, player, "message.unknown_echoes.abyss_beacon.need_" + stage);
            return false;
        }
        if (!EchoAbilityManager.activateMechanism(player, baseKey + ":" + stage)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return false;
        }
        BooleanSupplier canContinue = () -> isCalibrationCurrent(player, level, pos, baseKey, stage, ability);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        EchoAbilityManager.syncToClient(player);
        return playStageFeedback(level, pos, player, ability, particle, sound,
                STAGE_COOLDOWN_TICKS, canContinue);
    }

    private static boolean playStageFeedback(ServerLevel level, BlockPos pos, ServerPlayer player,
                                          EchoAbilityType ability,
                                          net.minecraft.core.particles.ParticleOptions particle,
                                          net.minecraft.sounds.SoundEvent sound, int cooldownTicks,
                                          BooleanSupplier canContinue) {
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        player.getCooldowns().addCooldown(ModItems.ABYSS_BEACON.get(), cooldownTicks);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.9F, 1.0F);
        if (!canContinue.getAsBoolean()) {
            return false;
        }
        AbyssBeaconEffects.playCalibration(level, pos, ability, particle);
        return canContinue.getAsBoolean();
    }

    private static void fail(ServerLevel level, BlockPos pos, ServerPlayer player, String messageKey) {
        PlayerSoundFeedback.deny(player);
        if (isInteractionCurrent(player, level, pos)) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static boolean isCalibrationCurrent(ServerPlayer player, ServerLevel level, BlockPos pos,
                                                 String baseKey, String stage, EchoAbilityType ability) {
        return isInteractionCurrent(player, level, pos)
                && EchoPermission.canCalibrateAbyssBeacon(player)
                && EchoAbilityManager.hasAbility(player, ability)
                && isStageComplete(player, baseKey, stage);
    }

    private static boolean isInteractionCurrent(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return !player.isRemoved() && player.isAlive() && !player.isSpectator() && !player.hasDisconnected()
                && player.level() == level
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.server.getLevel(level.dimension()) == level
                && player.canInteractWithBlock(pos, 1.0D)
                && isBeaconPresent(level, pos);
    }

    private static boolean isStageComplete(ServerPlayer player, String baseKey, String stage) {
        return EchoAbilityManager.hasActivatedMechanism(player, baseKey + ":" + stage);
    }

    public static boolean isCalibratedFor(ServerPlayer player, Level level, BlockPos pos) {
        return level.dimension().equals(EchoBeaconBlock.ECHO_REALM)
                && isBeaconPresent(level, pos)
                && isStageComplete(player, calibrationKey(level, pos), "complete");
    }

    static boolean isBeaconPresent(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos).is(ModBlocks.ABYSS_BEACON.get());
    }

    /** 结构中心与信标中心共享 X/Z；忽略地表自适应产生的 Y 差异。 */
    public static boolean hasCompletedAtStructureCenter(ServerPlayer player, Level level,
                                                        BlockPos structureCenter) {
        String prefix = CALIBRATION_PREFIX + level.dimension().location() + ":"
                + structureCenter.getX() + ",";
        String suffix = "," + structureCenter.getZ() + ":complete";
        return EchoAbilityManager.getData(player).getActivatedMechanisms().stream()
                .anyMatch(key -> key.startsWith(prefix) && key.endsWith(suffix));
    }

    private static String calibrationKey(Level level, BlockPos pos) {
        return CALIBRATION_PREFIX + level.dimension().location() + ":"
                + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
